package com.recallstudio.api;

import com.recallstudio.service.InsightsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {
    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/reasons")
    public InsightsService.ReasonDistribution reasons(@RequestParam(name = "days", required = false) Integer days) {
        return insightsService.getReasonDistribution(days);
    }
}
