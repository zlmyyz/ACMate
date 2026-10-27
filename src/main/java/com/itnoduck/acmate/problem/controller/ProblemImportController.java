package com.itnoduck.acmate.problem.controller;

import com.itnoduck.acmate.problem.dto.ImportedProblemResponse;
import com.itnoduck.acmate.problem.service.NowcoderProblemImportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problems/import")
public class ProblemImportController {
    private final NowcoderProblemImportService importer;
    public ProblemImportController(NowcoderProblemImportService importer) { this.importer = importer; }
    public record ImportRequest(@NotBlank @Size(max=1024) String url) {}
    @PostMapping("/nowcoder")
    public ImportedProblemResponse preview(@Valid @RequestBody ImportRequest request) {
        return importer.preview(request.url());
    }
}
