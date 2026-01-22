package com.recallstudio.api;

import com.recallstudio.service.ImportExportService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ImportExportController {
    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @PostMapping("/import")
    public ApiResponse<ImportExportService.ImportResult> importData(@RequestBody ImportExportService.ImportRequest request) {
        return ApiResponse.ok(importExportService.importData(request));
    }

    @GetMapping("/export")
    public ApiResponse<ImportExportService.ExportResponse> exportData(
            @RequestParam(name = "includeReviews", defaultValue = "false") boolean includeReviews) {
        return ApiResponse.ok(importExportService.exportData(includeReviews));
    }
}
