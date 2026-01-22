package com.recallstudio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recallstudio.config.AppProperties;
import com.recallstudio.domain.Review;
import com.recallstudio.repo.DataDirService;
import com.recallstudio.repo.JsonFileStore;
import com.recallstudio.repo.ReviewRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InsightsServiceTest {
    @Test
    void reasonDistributionCountsWrongTags() throws Exception {
        Path dataDir = Files.createTempDirectory("recall-insights-");
        Files.createDirectories(dataDir.resolve(".git"));

        AppProperties properties = new AppProperties();
        properties.setDataDir(dataDir.toString());
        DataDirService dataDirService = new DataDirService(properties);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonFileStore store = new JsonFileStore(mapper);
        ReviewRepository reviewRepository = new ReviewRepository(dataDirService, store);
        InsightsService insightsService = new InsightsService(reviewRepository);

        OffsetDateTime now = OffsetDateTime.now();

        Review review1 = new Review();
        review1.setReviewId("r1");
        review1.setSessionId("s1");
        review1.setItemId("i1");
        review1.setDeckId("d1");
        review1.setReviewedAt(now.minusDays(1));
        review1.setScore(0);
        review1.setReasonTags(List.of("概念", "机制"));

        Review review2 = new Review();
        review2.setReviewId("r2");
        review2.setSessionId("s1");
        review2.setItemId("i2");
        review2.setDeckId("d1");
        review2.setReviewedAt(now.minusDays(1));
        review2.setScore(0);
        review2.setReasonTags(List.of("概念"));

        Review review3 = new Review();
        review3.setReviewId("r3");
        review3.setSessionId("s2");
        review3.setItemId("i3");
        review3.setDeckId("d1");
        review3.setReviewedAt(now.minusDays(1));
        review3.setScore(1);
        review3.setReasonTags(List.of("概念"));

        Review review4 = new Review();
        review4.setReviewId("r4");
        review4.setSessionId("s2");
        review4.setItemId("i4");
        review4.setDeckId("d1");
        review4.setReviewedAt(now.minusDays(2));
        review4.setScore(0);
        review4.setReasonTags(List.of());

        reviewRepository.append(review1);
        reviewRepository.append(review2);
        reviewRepository.append(review3);
        reviewRepository.append(review4);

        InsightsService.ReasonDistribution distribution = insightsService.getReasonDistribution(null);

        assertThat(distribution.wrongReviewCount()).isEqualTo(3);
        assertThat(distribution.totalTagCount()).isEqualTo(4);
        assertThat(distribution.stats())
                .extracting(InsightsService.ReasonStat::tag, InsightsService.ReasonStat::count)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("概念", 2),
                        org.assertj.core.groups.Tuple.tuple("机制", 1),
                        org.assertj.core.groups.Tuple.tuple("未标记", 1)
                );
    }
}
