package com.itnoduck.acmate.discussion.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("post")
public class Post {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long authorUserId;
    private Long problemId;
    private Long trainingPlanId;
    private String postType;
    private String title;
    private String contentMd;
    private Integer status;
    private Integer isPinned;
    private Long acceptedCommentId;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(Long v) { this.authorUserId = v; }
    public Long getProblemId() { return problemId; }
    public void setProblemId(Long v) { this.problemId = v; }
    public Long getTrainingPlanId() { return trainingPlanId; }
    public void setTrainingPlanId(Long v) { this.trainingPlanId = v; }
    public String getPostType() { return postType; }
    public void setPostType(String v) { this.postType = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContentMd() { return contentMd; }
    public void setContentMd(String v) { this.contentMd = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public Integer getIsPinned() { return isPinned; }
    public void setIsPinned(Integer v) { this.isPinned = v; }
    public Long getAcceptedCommentId() { return acceptedCommentId; }
    public void setAcceptedCommentId(Long v) { this.acceptedCommentId = v; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer v) { this.viewCount = v; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer v) { this.likeCount = v; }
    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer v) { this.commentCount = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
