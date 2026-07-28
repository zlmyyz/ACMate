package com.itnoduck.acmate.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recipientUserId;
    private String notificationType;
    private Long actorUserId;
    private String resourceType;
    private Long resourceId;
    private String payloadJson;
    private Integer isRead;
    private LocalDateTime readTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(Long v) { recipientUserId = v; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String v) { notificationType = v; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long v) { actorUserId = v; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String v) { resourceType = v; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long v) { resourceId = v; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String v) { payloadJson = v; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer v) { isRead = v; }
    public LocalDateTime getReadTime() { return readTime; }
    public void setReadTime(LocalDateTime v) { readTime = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { createTime = v; }
}
