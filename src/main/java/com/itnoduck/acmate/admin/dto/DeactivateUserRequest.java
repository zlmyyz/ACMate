package com.itnoduck.acmate.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DeactivateUserRequest {
    @NotBlank(message = "停用原因不能为空")
    @Size(max = 500, message = "停用原因不能超过500字符")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v != null ? v.strip() : null; }
}
