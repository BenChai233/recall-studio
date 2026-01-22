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
    public ReviewService.TodayStats today() {
        return reviewService.getTodayStats();
    }

    @PostMapping("/sessions")
    public ReviewService.SessionResponse createSession(@RequestBody ReviewService.SessionRequest request) {
        return reviewService.createSession(request);
    }

    @PostMapping
    public ReviewService.ReviewResult submit(@RequestBody ReviewService.ReviewSubmit request) {
        return reviewService.submitReview(request);
    }

    @GetMapping("/sessions/{sessionId}/summary")
    public ReviewService.SessionSummary summary(@PathVariable("sessionId") String sessionId) {
        return reviewService.getSessionSummary(sessionId);
    }
}
