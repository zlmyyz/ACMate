package com.itnoduck.acmate.training.dto;

import java.time.LocalDateTime;

public class PlanSummaryResponse {

    private Long id;
    private String title;
    private String planType;
    private boolean active;
    private String creatorUsername;
    private String creatorNickname;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** Computed: NOT_STARTED / ONGOING / ENDED */
    private String timeStatus;
    private int problemCount;
    private int memberCount;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatorUsername() { return creatorUsername; }
    public void setCreatorUsername(String creatorUsername) { this.creatorUsername = creatorUsername; }

    public String getCreatorNickname() { return creatorNickname; }
    public void setCreatorNickname(String creatorNickname) { this.creatorNickname = creatorNickname; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getTimeStatus() { return timeStatus; }
    public void setTimeStatus(String timeStatus) { this.timeStatus = timeStatus; }

    public int getProblemCount() { return problemCount; }
    public void setProblemCount(int problemCount) { this.problemCount = problemCount; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
