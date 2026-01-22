package com.recallstudio.api;

import com.recallstudio.service.ReviewService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/today")
    public ApiResponse<ReviewService.TodayStats> today() {
        return ApiResponse.ok(reviewService.getTodayStats());
    }

    @PostMapping("/sessions")
    public ApiResponse<ReviewService.SessionResponse> createSession(@RequestBody ReviewService.SessionRequest request) {
        return ApiResponse.ok(reviewService.createSession(request));
    }

    @PostMapping
    public ApiResponse<ReviewService.ReviewResult> submit(@RequestBody ReviewService.ReviewSubmit request) {
        return ApiResponse.ok(reviewService.submitReview(request));
    }

    @PostMapping("/sessions/{sessionId}/undo")
    public ApiResponse<ReviewService.UndoResult> undo(@PathVariable("sessionId") String sessionId) {
        return ApiResponse.ok(reviewService.undoLastReview(sessionId));
    }

    @PostMapping("/items/{itemId}/undo")
    public ApiResponse<ReviewService.UndoResult> undoByItem(@PathVariable("itemId") String itemId) {
        return ApiResponse.ok(reviewService.undoLastReviewByItem(itemId));
    }

    @GetMapping("/sessions/{sessionId}/summary")
    public ApiResponse<ReviewService.SessionSummary> summary(@PathVariable("sessionId") String sessionId) {
        return ApiResponse.ok(reviewService.getSessionSummary(sessionId));
    }

    @GetMapping("/items/{itemId}/latest")
    public ApiResponse<ReviewService.ReviewAnswer> latestAnswer(@PathVariable("itemId") String itemId) {
        return ApiResponse.ok(reviewService.getLatestAnswer(itemId));
    }
}
