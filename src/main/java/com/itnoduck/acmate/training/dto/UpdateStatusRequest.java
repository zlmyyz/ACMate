package com.itnoduck.acmate.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateStatusRequest(
        @NotBlank @Pattern(regexp = "^(NOT_STARTED|CHALLENGING)$") String status
) {}
