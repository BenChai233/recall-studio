package com.recallstudio.service;

import com.recallstudio.domain.Review;
import com.recallstudio.repo.ReviewRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InsightsService {
    private static final String UNLABELED = "未标记";

    private final ReviewRepository reviewRepository;

    public InsightsService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public ReasonDistribution getReasonDistribution(Integer days) {
        List<Review> reviews = reviewRepository.findAll();
        if (days != null && days > 0) {
            OffsetDateTime since = OffsetDateTime.now().minusDays(days);
            reviews = reviews.stream()
                    .filter(r -> r.getReviewedAt() != null && !r.getReviewedAt().isBefore(since))
                    .toList();
        }

        Map<String, Integer> counter = new HashMap<>();
        int wrongReviewCount = 0;
        int totalTagCount = 0;
        for (Review review : reviews) {
            if (review.getScore() != 0) {
                continue;
            }
            wrongReviewCount++;
            List<String> tags = review.getReasonTags();
            if (tags == null || tags.isEmpty()) {
                counter.merge(UNLABELED, 1, Integer::sum);
                totalTagCount++;
                continue;
            }
            for (String tag : tags) {
                String normalized = (tag == null || tag.isBlank()) ? UNLABELED : tag.trim();
                counter.merge(normalized, 1, Integer::sum);
                totalTagCount++;
            }
        }

        List<ReasonStat> stats = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counter.entrySet()) {
            stats.add(new ReasonStat(entry.getKey(), entry.getValue()));
        }
        stats.sort(Comparator.comparing(ReasonStat::count).reversed().thenComparing(ReasonStat::tag));

        return new ReasonDistribution(wrongReviewCount, totalTagCount, stats);
    }

    public record ReasonStat(String tag, int count) {}

    public record ReasonDistribution(int wrongReviewCount, int totalTagCount, List<ReasonStat> stats) {}
}
