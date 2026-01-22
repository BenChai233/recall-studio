package com.recallstudio.repo;

import com.recallstudio.domain.Review;
import org.springframework.stereotype.Repository;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
