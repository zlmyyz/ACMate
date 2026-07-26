package com.itnoduck.acmate.discussion.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PostDetailResponse {
    private Long id;
    private String title;
    private String contentMd;
    private String postType;
    private Long authorUserId;
    private String authorUsername;
    private String authorNickname;
    private String authorAvatarUrl;
    private Long problemId;
    private String problemTitle;
    private Long trainingPlanId;
    private boolean active;
    private boolean pinned;
    private int likeCount;
    private int commentCount;
    private int viewCount;
    private boolean likedByMe;
    private List<CommentResponse> comments;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContentMd() { return contentMd; }
    public void setContentMd(String v) { this.contentMd = v; }
    public String getPostType() { return postType; }
    public void setPostType(String v) { this.postType = v; }
    public Long getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(Long v) { this.authorUserId = v; }
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String v) { this.authorUsername = v; }
    public String getAuthorNickname() { return authorNickname; }
    public void setAuthorNickname(String v) { this.authorNickname = v; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public void setAuthorAvatarUrl(String v) { this.authorAvatarUrl = v; }
    public Long getProblemId() { return problemId; }
    public void setProblemId(Long v) { this.problemId = v; }
    public String getProblemTitle() { return problemTitle; }
    public void setProblemTitle(String v) { this.problemTitle = v; }
    public Long getTrainingPlanId() { return trainingPlanId; }
    public void setTrainingPlanId(Long v) { this.trainingPlanId = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean v) { this.pinned = v; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int v) { this.likeCount = v; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int v) { this.commentCount = v; }
    public int getViewCount() { return viewCount; }
    public void setViewCount(int v) { this.viewCount = v; }
    public boolean isLikedByMe() { return likedByMe; }
    public void setLikedByMe(boolean v) { this.likedByMe = v; }
    public List<CommentResponse> getComments() { return comments; }
    public void setComments(List<CommentResponse> v) { this.comments = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
