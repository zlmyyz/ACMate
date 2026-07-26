package com.itnoduck.acmate.oj.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("oj_submission")
public class OjSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ojAccountId;
    private Long userId;
    private String platform;
    private String remoteSubmissionId;
    private Long problemId;
    private String externalProblemKey;
    private String verdict;
    private String language;
    private LocalDateTime submittedTime;
    private Integer isFirstAc;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getOjAccountId() { return ojAccountId; }
    public void setOjAccountId(Long v) { ojAccountId = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getPlatform() { return platform; }
    public void setPlatform(String v) { platform = v; }
    public String getRemoteSubmissionId() { return remoteSubmissionId; }
    public void setRemoteSubmissionId(String v) { remoteSubmissionId = v; }
    public Long getProblemId() { return problemId; }
    public void setProblemId(Long v) { problemId = v; }
    public String getExternalProblemKey() { return externalProblemKey; }
    public void setExternalProblemKey(String v) { externalProblemKey = v; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String v) { verdict = v; }
    public String getLanguage() { return language; }
    public void setLanguage(String v) { language = v; }
    public LocalDateTime getSubmittedTime() { return submittedTime; }
    public void setSubmittedTime(LocalDateTime v) { submittedTime = v; }
    public Integer getIsFirstAc() { return isFirstAc; }
    public void setIsFirstAc(Integer v) { isFirstAc = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { createTime = v; }
}
