package com.itnoduck.acmate.oj.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("oj_first_ac")
public class FirstAc {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String platform;
    private String externalProblemKey;
    private Long submissionId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getPlatform() { return platform; }
    public void setPlatform(String v) { platform = v; }
    public String getExternalProblemKey() { return externalProblemKey; }
    public void setExternalProblemKey(String v) { externalProblemKey = v; }
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long v) { submissionId = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { createTime = v; }
}
