package com.itnoduck.acmate.auditlog.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String reason;
    private String beforeState;
    private String afterState;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long v) { operatorId = v; }
    public String getAction() { return action; }
    public void setAction(String v) { action = v; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String v) { resourceType = v; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long v) { resourceId = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { reason = v; }
    public String getBeforeState() { return beforeState; }
    public void setBeforeState(String v) { beforeState = v; }
    public String getAfterState() { return afterState; }
    public void setAfterState(String v) { afterState = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { createTime = v; }
}
