package com.itnoduck.acmate.discussion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatePostRequest {
    @NotBlank @Size(max = 255)
    private String title;
    @NotBlank
    private String contentMd;
    @NotBlank
    private String postType;
    private Long problemId;
    private Long trainingPlanId;
    private Boolean broadcast;

    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContentMd() { return contentMd; }
    public void setContentMd(String v) { this.contentMd = v; }
    public String getPostType() { return postType; }
    public void setPostType(String v) { this.postType = v; }
    public Long getProblemId() { return problemId; }
    public void setProblemId(Long v) { this.problemId = v; }
    public Long getTrainingPlanId() { return trainingPlanId; }
    public void setTrainingPlanId(Long v) { this.trainingPlanId = v; }
    public Boolean getBroadcast() { return broadcast; }
    public void setBroadcast(Boolean v) { this.broadcast = v; }
}
