package com.itnoduck.acmate.discussion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCommentRequest {
    @NotBlank @Size(max = 5000)
    private String content;
    private Long parentId;
    private Long replyToUserId;

    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long v) { this.parentId = v; }
    public Long getReplyToUserId() { return replyToUserId; }
    public void setReplyToUserId(Long v) { this.replyToUserId = v; }
}
