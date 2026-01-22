package com.recallstudio.repo;

import com.recallstudio.domain.Review;
import org.springframework.stereotype.Repository;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class ReviewRepository {
    private final DataDirService dataDirService;
    private final JsonFileStore store;

    public ReviewRepository(DataDirService dataDirService, JsonFileStore store) {
        this.dataDirService = dataDirService;
        this.store = store;
    }

    public void append(Review review) {
        LocalDate date = review.getReviewedAt().toLocalDate();
        String dateDir = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        Path dir = dataDirService.reviewsDir().resolve(dateDir);
        dataDirService.ensureDir(dir);
        Path path = dir.resolve(review.getSessionId() + ".jsonl");
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            writer.write(store.toJson(review));
            writer.newLine();
        } catch (Exception ex) {
            throw new RuntimeException("failed to append review", ex);
        }
    }

    public List<Review> findBySession(String sessionId) {
        Path reviewsDir = dataDirService.reviewsDir();
        if (!Files.exists(reviewsDir)) {
            return List.of();
        }
        List<Review> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(reviewsDir, 2)) {
            stream.filter(p -> p.getFileName().toString().equals(sessionId + ".jsonl"))
                    .forEach(p -> result.addAll(readJsonl(p)));
        } catch (Exception ex) {
            throw new RuntimeException("failed to read reviews", ex);
        }
        return result;
    }

    public Review findLastBySession(String sessionId) {
        List<Review> reviews = findBySession(sessionId);
        if (reviews.isEmpty()) {
            return null;
        }
        return reviews.stream()
                .filter(r -> r.getReviewedAt() != null)
                .max(Comparator.comparing(Review::getReviewedAt))
                .orElse(reviews.getLast());
    }

    public Review findLastByItem(String itemId) {
        List<Review> reviews = findByItem(itemId);
        if (reviews.isEmpty()) {
            return null;
        }
        return reviews.stream()
                .filter(r -> r.getReviewedAt() != null)
                .max(Comparator.comparing(Review::getReviewedAt))
                .orElse(reviews.getFirst());
    }

    public List<Review> findByItem(String itemId) {
        return findAll().stream()
                .filter(r -> itemId.equals(r.getItemId()))
                .collect(Collectors.toList());
    }

    public List<Review> findAll() {
        Path reviewsDir = dataDirService.reviewsDir();
        if (!Files.exists(reviewsDir)) {
            return List.of();
        }
        List<Review> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(reviewsDir, 3)) {
            stream.filter(p -> p.toString().endsWith(".jsonl")).forEach(p -> result.addAll(readJsonl(p)));
        } catch (Exception ex) {
            throw new RuntimeException("failed to read reviews", ex);
        }
        return result;
    }

    public boolean removeByReviewId(String sessionId, String reviewId, OffsetDateTime reviewedAt) {
        List<Path> candidates = new ArrayList<>();
        if (reviewedAt != null) {
            LocalDate date = reviewedAt.toLocalDate();
            String dateDir = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            candidates.add(dataDirService.reviewsDir().resolve(dateDir).resolve(sessionId + ".jsonl"));
        }
        candidates.addAll(listSessionFiles(sessionId));
        LinkedHashSet<Path> unique = new LinkedHashSet<>(candidates);
        for (Path path : unique) {
            if (!Files.exists(path)) {
                continue;
            }
            List<String> lines;
            try {
                lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                throw new RuntimeException("failed to read reviews", ex);
            }
            boolean removed = false;
            List<String> keep = new ArrayList<>();
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                Review review = store.fromJson(line, Review.class);
                if (!removed && reviewId.equals(review.getReviewId())) {
                    removed = true;
                    continue;
                }
                keep.add(line);
            }
            if (removed) {
                try {
                    if (keep.isEmpty()) {
                        Files.deleteIfExists(path);
                    } else {
                        Files.write(path, keep, StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("failed to update reviews", ex);
                }
                return true;
            }
        }
        return false;
    }

    private List<Path> listSessionFiles(String sessionId) {
        Path reviewsDir = dataDirService.reviewsDir();
        if (!Files.exists(reviewsDir)) {
            return List.of();
        }
        List<Path> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(reviewsDir, 2)) {
            stream.filter(p -> p.getFileName().toString().equals(sessionId + ".jsonl"))
                    .forEach(result::add);
        } catch (Exception ex) {
            throw new RuntimeException("failed to list review files", ex);
        }
        return result;
    }

    private List<Review> readJsonl(Path path) {
        List<Review> result = new ArrayList<>();
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            lines.filter(l -> !l.isBlank()).forEach(line -> result.add(store.fromJson(line, Review.class)));
        } catch (Exception ex) {
            throw new RuntimeException("failed to parse jsonl: " + path, ex);
        }
        return result;
    }
}
