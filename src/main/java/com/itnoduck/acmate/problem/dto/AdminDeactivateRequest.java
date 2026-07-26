package com.itnoduck.acmate.problem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class AdminDeactivateRequest {

    @NotBlank(message = "停用原因不能为空")
    @Size(max = 500, message = "停用原因不能超过 500 字")
    private String reason;
}
