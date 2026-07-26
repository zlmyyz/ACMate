package com.itnoduck.acmate.discussion.dto;

import jakarta.validation.constraints.Size;

public class UpdatePostRequest {
    @Size(max = 255)
    private String title;
    private String contentMd;

    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContentMd() { return contentMd; }
    public void setContentMd(String v) { this.contentMd = v; }
}
