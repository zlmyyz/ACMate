package com.itnoduck.acmate.training.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MemberProgressResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private LocalDateTime joinTime;
    private boolean creator;
    private int completedCount;
    private int totalCount;
    private LocalDateTime lastAcceptedTime;
    private Long rank;
    private Integer completionOrder;
    private List<ProblemProgressItem> problems;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { username = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { nickname = v; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String v) { avatarUrl = v; }
    public LocalDateTime getJoinTime() { return joinTime; }
    public void setJoinTime(LocalDateTime v) { joinTime = v; }
    public boolean isCreator() { return creator; }
    public void setCreator(boolean v) { creator = v; }
    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int v) { completedCount = v; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int v) { totalCount = v; }
    public LocalDateTime getLastAcceptedTime() { return lastAcceptedTime; }
    public void setLastAcceptedTime(LocalDateTime v) { lastAcceptedTime = v; }
    public Long getRank() { return rank; }
    public void setRank(Long v) { rank = v; }
    public Integer getCompletionOrder() { return completionOrder; }
    public void setCompletionOrder(Integer v) { completionOrder = v; }
    public List<ProblemProgressItem> getProblems() { return problems; }
    public void setProblems(List<ProblemProgressItem> v) { problems = v; }

    public static class ProblemProgressItem {
        private Long problemId;
        private String problemTitle;
        private String platform;
        private String difficulty;
        private boolean problemActive;
        private int sortOrder;
        private boolean required;
        private String myStatus;
        private String performanceNote;

        public Long getProblemId() { return problemId; }
        public void setProblemId(Long v) { problemId = v; }
        public String getProblemTitle() { return problemTitle; }
        public void setProblemTitle(String v) { problemTitle = v; }
        public String getPlatform() { return platform; }
        public void setPlatform(String v) { platform = v; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String v) { difficulty = v; }
        public boolean isProblemActive() { return problemActive; }
        public void setProblemActive(boolean v) { problemActive = v; }
        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int v) { sortOrder = v; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean v) { required = v; }
        public String getMyStatus() { return myStatus; }
        public void setMyStatus(String v) { myStatus = v; }
        public String getPerformanceNote() { return performanceNote; }
        public void setPerformanceNote(String v) { performanceNote = v; }
    }
}
