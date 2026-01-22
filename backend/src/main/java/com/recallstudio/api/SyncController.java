package com.recallstudio.api;

import com.recallstudio.service.SyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {
    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/status")
    public SyncService.SyncStatus status() {
        return syncService.getStatus();
    }

    @PostMapping("/snapshot")
    public SyncService.SyncStatus snapshot() {
        return syncService.markSnapshot();
    }
}
