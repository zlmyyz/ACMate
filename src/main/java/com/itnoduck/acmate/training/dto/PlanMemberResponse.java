package com.itnoduck.acmate.training.dto;

import java.time.LocalDateTime;

public class PlanMemberResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private LocalDateTime joinTime;
    private boolean creator;

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
}
