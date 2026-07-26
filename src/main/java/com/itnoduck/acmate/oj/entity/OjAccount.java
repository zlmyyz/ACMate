package com.itnoduck.acmate.oj.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("oj_account")
public class OjAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String platform;
    private String externalUserId;
    private String displayName;
    private Integer verifyStatus;
    private Integer syncEnabled;
    private String lastSyncCursor;
    private LocalDateTime lastSyncTime;
    private Integer lastSyncSuccess;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getPlatform() { return platform; }
    public void setPlatform(String v) { platform = v; }
    public String getExternalUserId() { return externalUserId; }
    public void setExternalUserId(String v) { externalUserId = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { displayName = v; }
    public Integer getVerifyStatus() { return verifyStatus; }
    public void setVerifyStatus(Integer v) { verifyStatus = v; }
    public Integer getSyncEnabled() { return syncEnabled; }
    public void setSyncEnabled(Integer v) { syncEnabled = v; }
    public String getLastSyncCursor() { return lastSyncCursor; }
    public void setLastSyncCursor(String v) { lastSyncCursor = v; }
    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(LocalDateTime v) { lastSyncTime = v; }
    public Integer getLastSyncSuccess() { return lastSyncSuccess; }
    public void setLastSyncSuccess(Integer v) { lastSyncSuccess = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime v) { updateTime = v; }
}
