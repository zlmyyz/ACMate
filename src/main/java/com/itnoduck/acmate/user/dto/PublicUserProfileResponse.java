package com.itnoduck.acmate.user.dto;

import java.time.LocalDateTime;

public class PublicUserProfileResponse {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private boolean admin;
    /** ACTIVE / DISABLED */
    private String accountStatus;
    private long createdProblemCount;
    private String codeforcesHandle;
    private OjStatsResponse ojStats;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { username = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { nickname = v; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String v) { avatarUrl = v; }
    public String getBio() { return bio; }
    public void setBio(String v) { bio = v; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean v) { admin = v; }
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String v) { accountStatus = v; }
    public long getCreatedProblemCount() { return createdProblemCount; }
    public void setCreatedProblemCount(long v) { createdProblemCount = v; }
    public String getCodeforcesHandle() { return codeforcesHandle; }
    public void setCodeforcesHandle(String v) { codeforcesHandle = v; }
    public OjStatsResponse getOjStats() { return ojStats; }
    public void setOjStats(OjStatsResponse v) { ojStats = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { createTime = v; }
}
