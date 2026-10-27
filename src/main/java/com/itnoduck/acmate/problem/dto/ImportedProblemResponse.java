package com.itnoduck.acmate.problem.dto;

import java.util.List;

public record ImportedProblemResponse(String platform, String externalProblemKey, String title,
        String sourceUrl, String difficulty, String tags, String contentMd, List<String> warnings) {}
