package com.recallstudio.api;

import com.recallstudio.domain.Settings;
import com.recallstudio.service.SettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ApiResponse<Settings> get() {
        return ApiResponse.ok(settingsService.get());
    }

    @PutMapping
    public ApiResponse<Settings> update(@RequestBody Settings settings) {
        return ApiResponse.ok(settingsService.update(settings));
    }
}
