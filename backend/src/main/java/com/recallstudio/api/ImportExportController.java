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
    public ImportExportService.ImportResult importData(@RequestBody ImportExportService.ImportRequest request) {
        return importExportService.importData(request);
    }

    @GetMapping("/export")
    public ImportExportService.ExportResponse exportData(
            @RequestParam(name = "includeReviews", defaultValue = "false") boolean includeReviews) {
        return importExportService.exportData(includeReviews);
    }
}
