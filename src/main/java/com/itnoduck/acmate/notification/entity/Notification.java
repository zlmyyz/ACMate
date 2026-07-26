package com.itnoduck.acmate.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String resourceType;
    private Long resourceId;
    private Integer isRead;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getType() { return type; }
    public void setType(String v) { type = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getContent() { return content; }
    public void setContent(String v) { content = v; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String v) { resourceType = v; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long v) { resourceId = v; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer v) { isRead = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { createTime = v; }
}
