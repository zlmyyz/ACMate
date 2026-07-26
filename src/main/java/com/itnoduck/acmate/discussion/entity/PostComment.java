package com.itnoduck.acmate.discussion.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("post_comment")
public class PostComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long userId;
    private Long parentId;
    private Long replyToUserId;
    private String content;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private String deactivationSource;
    private String deactivationReason;
    private Long deactivatedBy;
    private LocalDateTime deactivationTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public String getDeactivationSource() { return deactivationSource; }
    public void setDeactivationSource(String v) { this.deactivationSource = v; }
    public String getDeactivationReason() { return deactivationReason; }
    public void setDeactivationReason(String v) { this.deactivationReason = v; }
    public Long getDeactivatedBy() { return deactivatedBy; }
    public void setDeactivatedBy(Long v) { this.deactivatedBy = v; }
    public LocalDateTime getDeactivationTime() { return deactivationTime; }
    public void setDeactivationTime(LocalDateTime v) { this.deactivationTime = v; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long v) { this.postId = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long v) { this.parentId = v; }
    public Long getReplyToUserId() { return replyToUserId; }
    public void setReplyToUserId(Long v) { this.replyToUserId = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
