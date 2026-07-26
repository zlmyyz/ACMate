package com.itnoduck.acmate.export.controller;

import com.itnoduck.acmate.export.service.DataExportService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/exports")
public class DataExportController {

    private final DataExportService exportService;

    public DataExportController(DataExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/problems")
    public ResponseEntity<String> exportProblems(@AuthenticationPrincipal AuthenticatedUser user) {
        String csv = exportService.exportProblems(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=problems.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<String> exportLeaderboard(@RequestParam(defaultValue = "total") String period,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
        String csv = exportService.exportLeaderboard(period, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leaderboard_" + period + ".csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
