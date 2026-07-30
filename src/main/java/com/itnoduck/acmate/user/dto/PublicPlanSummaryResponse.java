package com.itnoduck.acmate.user.dto;

import java.time.LocalDateTime;

public class PublicPlanSummaryResponse {
    private Long id;
    private String title;
    private String timeStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int problemCount;
    private int memberCount;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getTimeStatus() { return timeStatus; }
    public void setTimeStatus(String v) { timeStatus = v; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime v) { startTime = v; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime v) { endTime = v; }
    public int getProblemCount() { return problemCount; }
    public void setProblemCount(int v) { problemCount = v; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int v) { memberCount = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { createTime = v; }
}
