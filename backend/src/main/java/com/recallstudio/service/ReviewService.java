package com.recallstudio.service;

import com.recallstudio.domain.DueType;
import com.recallstudio.domain.Item;
import com.recallstudio.domain.Review;
import com.recallstudio.domain.Settings;
import com.recallstudio.domain.SrsState;
import com.recallstudio.exception.AppException;
import com.recallstudio.exception.NotFoundException;
import com.recallstudio.repo.ItemRepository;
import com.recallstudio.repo.ReviewRepository;
import com.recallstudio.repo.SettingsRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    private final ItemRepository itemRepository;
    private final ReviewRepository reviewRepository;
    private final SettingsRepository settingsRepository;
    private final SrsService srsService;

    public ReviewService(ItemRepository itemRepository,
                         ReviewRepository reviewRepository,
                         SettingsRepository settingsRepository,
                         SrsService srsService) {
        this.itemRepository = itemRepository;
        this.reviewRepository = reviewRepository;
        this.settingsRepository = settingsRepository;
        this.srsService = srsService;
    }

    public TodayStats getTodayStats() {
        Settings settings = settingsRepository.load();
        OffsetDateTime now = OffsetDateTime.now();
        int dueCount = 0;
        int wrongCount = 0;
        int newCount = 0;
        for (Item item : itemRepository.findAll()) {
            if (item.isArchived()) {
                continue;
            }
            if (item.getSrs() == null) {
                newCount++;
                continue;
            }
            SrsState srs = item.getSrs();
            if (srs.getWrongDue() != null && !srs.getWrongDue().isAfter(now)) {
                wrongCount++;
            }
            if (srs.getDue() != null && !srs.getDue().isAfter(now)) {
                dueCount++;
            }
        }
        int dailyLimit = settings.getDailyLimit() > 0 ? settings.getDailyLimit() : 50;
        int total = Math.min(dailyLimit, dueCount + wrongCount + newCount);
        return new TodayStats(dueCount, wrongCount, newCount, total);
    }

    public SessionResponse createSession(SessionRequest request) {
        Settings settings = settingsRepository.load();
        OffsetDateTime now = OffsetDateTime.now();
        int limit = request.limit() != null && request.limit() > 0 ? request.limit()
                : settings.getDailyLimit();
        String deckId = request.deckId();
        boolean onlyWrong = request.onlyWrong() != null && request.onlyWrong();

        List<Item> items = itemRepository.findAll().stream()
                .filter(item -> !item.isArchived())
                .filter(item -> deckId == null || deckId.isBlank() || deckId.equals(item.getDeckId()))
                .collect(Collectors.toList());

        List<Item> dueItems = items.stream()
                .filter(item -> item.getSrs() != null && item.getSrs().getDue() != null
                        && !item.getSrs().getDue().isAfter(now))
                .sorted(Comparator.comparing(i -> i.getSrs().getDue()))
                .collect(Collectors.toList());

        List<Item> wrongItems = items.stream()
                .filter(item -> item.getSrs() != null && item.getSrs().getWrongDue() != null
                        && !item.getSrs().getWrongDue().isAfter(now))
                .sorted(Comparator.comparing(i -> i.getSrs().getWrongDue()))
                .collect(Collectors.toList());

        List<Item> newItems = items.stream()
                .filter(item -> item.getSrs() == null)
                .collect(Collectors.toList());

        LinkedHashSet<Item> selected = new LinkedHashSet<>();
        if (onlyWrong) {
            selected.addAll(wrongItems);
        } else {
            selected.addAll(dueItems);
            selected.addAll(wrongItems);
            selected.addAll(newItems);
        }

        List<SessionItem> sessionItems = new ArrayList<>();
        for (Item item : selected) {
            if (sessionItems.size() >= limit) {
                break;
            }
            sessionItems.add(new SessionItem(item.getItemId(), item.getPrompt(), item.getType(), resolveDueType(item, now)));
        }

        return new SessionResponse(UUID.randomUUID().toString(), sessionItems);
    }

    public ReviewResult submitReview(ReviewSubmit request) {
        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> new NotFoundException("item not found"));
        OffsetDateTime now = request.reviewedAt() != null ? request.reviewedAt() : OffsetDateTime.now();

        SrsState prevSrs = copySrs(item.getSrs());
        SrsState updated = srsService.update(item.getSrs(), request.score(), now);
        applyWrongSchedule(updated, request.score(), settingsRepository.load(), now);
        item.setSrs(updated);
        itemRepository.save(item);

        Review review = new Review();
        review.setReviewId(UUID.randomUUID().toString());
        review.setItemId(item.getItemId());
        review.setDeckId(item.getDeckId());
        review.setSessionId(request.sessionId());
        review.setReviewedAt(now);
        review.setScore(request.score());
        review.setAnswer(request.answer());
        review.setReasonTags(request.reasonTags());
        review.setPrevSrs(prevSrs);
        review.setPrevSrsPresent(true);
        reviewRepository.append(review);

        return new ReviewResult(updated.getDue(), updated);
    }

    public UndoResult undoLastReview(String sessionId) {
        Review review = reviewRepository.findLastBySession(sessionId);
        if (review == null || review.getReviewId() == null) {
            throw new NotFoundException("no review to undo");
        }
        SrsState targetSrs = resolvePrevSrs(review);
        boolean removed = reviewRepository.removeByReviewId(sessionId, review.getReviewId(), review.getReviewedAt());
        if (!removed) {
            throw new AppException("UNDO_FAILED", "failed to remove review", 500);
        }
        Item item = itemRepository.findById(review.getItemId())
                .orElseThrow(() -> new NotFoundException("item not found"));
        item.setSrs(targetSrs);
        itemRepository.save(item);
        return new UndoResult(review.getItemId(), review.getScore(), review.getAnswer(), review.getReasonTags());
    }

    public UndoResult undoLastReviewByItem(String itemId) {
        Review review = reviewRepository.findLastByItem(itemId);
        if (review == null || review.getReviewId() == null) {
            throw new NotFoundException("no review to undo");
        }
        SrsState targetSrs = resolvePrevSrs(review);
        boolean removed = reviewRepository.removeByReviewId(review.getSessionId(), review.getReviewId(), review.getReviewedAt());
        if (!removed) {
            throw new AppException("UNDO_FAILED", "failed to remove review", 500);
        }
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("item not found"));
        item.setSrs(targetSrs);
        itemRepository.save(item);
        return new UndoResult(review.getItemId(), review.getScore(), review.getAnswer(), review.getReasonTags());
    }

    public SessionSummary getSessionSummary(String sessionId) {
        List<Review> reviews = reviewRepository.findBySession(sessionId);
        if (reviews.isEmpty()) {
            return new SessionSummary(0.0, Map.of("0", 0, "1", 0, "2", 0), List.of(), List.of());
        }
        double avg = reviews.stream().mapToInt(Review::getScore).average().orElse(0);
        Map<String, Integer> scoreCount = new HashMap<>();
        scoreCount.put("0", 0);
        scoreCount.put("1", 0);
        scoreCount.put("2", 0);
        for (Review review : reviews) {
            String key = String.valueOf(review.getScore());
            scoreCount.put(key, scoreCount.getOrDefault(key, 0) + 1);
        }
        List<SummaryItem> wrongItems = reviews.stream()
                .filter(r -> r.getScore() == 0)
                .map(r -> {
                    Item item = itemRepository.findById(r.getItemId()).orElse(null);
                    String prompt = item != null ? item.getPrompt() : "";
                    return new SummaryItem(r.getItemId(), prompt);
                })
                .collect(Collectors.toList());
        List<String> nextQuestions = wrongItems.stream()
                .map(SummaryItem::prompt)
                .filter(p -> p != null && !p.isBlank())
                .limit(5)
                .collect(Collectors.toList());
        return new SessionSummary(avg, scoreCount, wrongItems, nextQuestions);
    }

    public ReviewAnswer getLatestAnswer(String itemId) {
        Review review = reviewRepository.findLastByItem(itemId);
        if (review == null) {
            return null;
        }
        return new ReviewAnswer(review.getItemId(), review.getAnswer(), review.getScore(),
                review.getReviewedAt(), review.getReasonTags());
    }

    private DueType resolveDueType(Item item, OffsetDateTime now) {
        if (item.getSrs() == null) {
            return DueType.NEW;
        }
        if (item.getSrs().getWrongDue() != null && !item.getSrs().getWrongDue().isAfter(now)) {
            return DueType.WRONG;
        }
        if (item.getSrs().getDue() != null && !item.getSrs().getDue().isAfter(now)) {
            return DueType.DUE;
        }
        return DueType.DUE;
    }

    private void applyWrongSchedule(SrsState srs, int score, Settings settings, OffsetDateTime now) {
        List<Integer> schedule = settings.getWrongReviewSchedule() == null || settings.getWrongReviewSchedule().isEmpty()
                ? List.of(1, 3, 7)
                : settings.getWrongReviewSchedule();
        if (score == 0) {
            int nextStep = srs.getWrongStep() == null ? 0 : Math.min(srs.getWrongStep() + 1, schedule.size() - 1);
            srs.setWrongStep(nextStep);
            int days = schedule.get(nextStep);
            srs.setWrongDue(now.plusDays(days));
        } else {
            srs.setWrongStep(null);
            srs.setWrongDue(null);
        }
    }

    private SrsState copySrs(SrsState source) {
        if (source == null) {
            return null;
        }
        SrsState copy = new SrsState();
        copy.setEasiness(source.getEasiness());
        copy.setInterval(source.getInterval());
        copy.setDue(source.getDue());
        copy.setReps(source.getReps());
        copy.setLastScore(source.getLastScore());
        copy.setLastReviewedAt(source.getLastReviewedAt());
        copy.setWrongStep(source.getWrongStep());
        copy.setWrongDue(source.getWrongDue());
        return copy;
    }

    private SrsState resolvePrevSrs(Review review) {
        if (review.isPrevSrsPresent()) {
            return copySrs(review.getPrevSrs());
        }
        return rebuildSrsBefore(review.getItemId(), review.getReviewId());
    }

    private SrsState rebuildSrsBefore(String itemId, String excludedReviewId) {
        List<Review> reviews = reviewRepository.findByItem(itemId);
        if (reviews.isEmpty()) {
            return null;
        }
        List<Review> ordered = reviews.stream()
                .filter(r -> r.getReviewId() != null && !r.getReviewId().equals(excludedReviewId))
                .filter(r -> r.getReviewedAt() != null)
                .sorted(Comparator.comparing(Review::getReviewedAt))
                .collect(Collectors.toList());
        if (ordered.isEmpty()) {
            return null;
        }
        Settings settings = settingsRepository.load();
        SrsState srs = null;
        for (Review review : ordered) {
            OffsetDateTime when = review.getReviewedAt();
            srs = srsService.update(srs, review.getScore(), when);
            applyWrongSchedule(srs, review.getScore(), settings, when);
        }
        return srs;
    }

    public record TodayStats(int dueCount, int wrongCount, int newCount, int totalPlanned) {}

    public record SessionRequest(String deckId, Boolean onlyWrong, Integer limit) {}

    public record SessionItem(String itemId, String prompt, String type, DueType dueType) {}

    public record SessionResponse(String sessionId, List<SessionItem> items) {}

    public record ReviewSubmit(String sessionId, String itemId, int score, String answer, List<String> reasonTags,
                               OffsetDateTime reviewedAt) {}

    public record ReviewResult(OffsetDateTime nextDue, SrsState srs) {}

    public record UndoResult(String itemId, int score, String answer, List<String> reasonTags) {}

    public record ReviewAnswer(String itemId, String answer, int score, OffsetDateTime reviewedAt,
                               List<String> reasonTags) {}

    public record SummaryItem(String itemId, String prompt) {}

    public record SessionSummary(double avgScore, Map<String, Integer> scoreCount,
                                 List<SummaryItem> wrongItems, List<String> nextQuestions) {}
}
