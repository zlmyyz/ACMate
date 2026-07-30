package com.itnoduck.acmate.training.dto;

import java.time.LocalDateTime;

public class PlanMemberResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private LocalDateTime joinTime;
    private boolean creator;
    private int completedCount;
    private int totalCount;
    private int requiredCompletedCount;
    private int requiredTotal;
    private LocalDateTime currentLastAcceptedTime;
    private LocalDateTime deadlineLastAcceptedTime;
    private LocalDateTime currentCompletedAt;
    private LocalDateTime deadlineCompletedAt;
    private Long rank;
    private Integer completionOrder;
    private int deadlineCompletedCount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public LocalDateTime getJoinTime() { return joinTime; }
    public void setJoinTime(LocalDateTime joinTime) { this.joinTime = joinTime; }

    public boolean isCreator() { return creator; }
    public void setCreator(boolean creator) { this.creator = creator; }

    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int v) { completedCount = v; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int v) { totalCount = v; }
    public int getRequiredCompletedCount() { return requiredCompletedCount; }
    public void setRequiredCompletedCount(int v) { requiredCompletedCount = v; }
    public int getRequiredTotal() { return requiredTotal; }
    public void setRequiredTotal(int v) { requiredTotal = v; }
    public LocalDateTime getCurrentLastAcceptedTime() { return currentLastAcceptedTime; }
    public void setCurrentLastAcceptedTime(LocalDateTime v) { currentLastAcceptedTime = v; }
    public LocalDateTime getDeadlineLastAcceptedTime() { return deadlineLastAcceptedTime; }
    public void setDeadlineLastAcceptedTime(LocalDateTime v) { deadlineLastAcceptedTime = v; }
    public LocalDateTime getCurrentCompletedAt() { return currentCompletedAt; }
    public void setCurrentCompletedAt(LocalDateTime v) { currentCompletedAt = v; }
    public LocalDateTime getDeadlineCompletedAt() { return deadlineCompletedAt; }
    public void setDeadlineCompletedAt(LocalDateTime v) { deadlineCompletedAt = v; }
    public Long getRank() { return rank; }
    public void setRank(Long v) { rank = v; }
    public Integer getCompletionOrder() { return completionOrder; }
    public void setCompletionOrder(Integer v) { completionOrder = v; }
    public int getDeadlineCompletedCount() { return deadlineCompletedCount; }
    public void setDeadlineCompletedCount(int v) { deadlineCompletedCount = v; }
}
