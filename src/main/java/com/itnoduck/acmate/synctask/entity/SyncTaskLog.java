package com.itnoduck.acmate.synctask.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("sync_task_log")
public class SyncTaskLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ojAccountId;
    private String platform;
    private String triggerType;
    private String taskStatus;
    private String cursorBefore;
    private String cursorAfter;
    private Integer fetchedCount;
    private Integer insertedCount;
    private Integer firstAcCount;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getOjAccountId() { return ojAccountId; }
    public void setOjAccountId(Long v) { ojAccountId = v; }
    public String getPlatform() { return platform; }
    public void setPlatform(String v) { platform = v; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String v) { triggerType = v; }
    public String getTaskStatus() { return taskStatus; }
    public void setTaskStatus(String v) { taskStatus = v; }
    public String getCursorBefore() { return cursorBefore; }
    public void setCursorBefore(String v) { cursorBefore = v; }
    public String getCursorAfter() { return cursorAfter; }
    public void setCursorAfter(String v) { cursorAfter = v; }
    public Integer getFetchedCount() { return fetchedCount; }
    public void setFetchedCount(Integer v) { fetchedCount = v; }
    public Integer getInsertedCount() { return insertedCount; }
    public void setInsertedCount(Integer v) { insertedCount = v; }
    public Integer getFirstAcCount() { return firstAcCount; }
    public void setFirstAcCount(Integer v) { firstAcCount = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { errorMessage = v; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime v) { startTime = v; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime v) { endTime = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { createTime = v; }
}
