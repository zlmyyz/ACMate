package com.itnoduck.acmate.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("user_problem_status")
public class UserProblemStatus {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long problemId;
    private Integer status;
    private Integer attemptCount;
    private LocalDateTime firstSubmitTime;
    private LocalDateTime firstAcTime;
    private LocalDateTime lastSubmitTime;
    private String solveSource;
    private Long acceptedSubmissionId;
    private String performanceNote;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public Long getProblemId() { return problemId; }
    public void setProblemId(Long v) { problemId = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { status = v; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer v) { attemptCount = v; }
    public LocalDateTime getFirstSubmitTime() { return firstSubmitTime; }
    public void setFirstSubmitTime(LocalDateTime v) { firstSubmitTime = v; }
    public LocalDateTime getFirstAcTime() { return firstAcTime; }
    public void setFirstAcTime(LocalDateTime v) { firstAcTime = v; }
    public LocalDateTime getLastSubmitTime() { return lastSubmitTime; }
    public void setLastSubmitTime(LocalDateTime v) { lastSubmitTime = v; }
    public String getSolveSource() { return solveSource; }
    public void setSolveSource(String v) { solveSource = v; }
    public Long getAcceptedSubmissionId() { return acceptedSubmissionId; }
    public void setAcceptedSubmissionId(Long v) { acceptedSubmissionId = v; }
    public String getPerformanceNote() { return performanceNote; }
    public void setPerformanceNote(String v) { performanceNote = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime v) { updateTime = v; }
}
