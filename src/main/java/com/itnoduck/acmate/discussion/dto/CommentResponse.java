package com.itnoduck.acmate.discussion.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CommentResponse {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Long replyToUserId;
    private String replyToUsername;
    private String content;
    private boolean active;
    private List<CommentResponse> replies;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String v) { this.avatarUrl = v; }
    public Long getReplyToUserId() { return replyToUserId; }
    public void setReplyToUserId(Long v) { this.replyToUserId = v; }
    public String getReplyToUsername() { return replyToUsername; }
    public void setReplyToUsername(String v) { this.replyToUsername = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public List<CommentResponse> getReplies() { return replies; }
    public void setReplies(List<CommentResponse> v) { this.replies = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
