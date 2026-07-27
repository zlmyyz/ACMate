package com.itnoduck.acmate.training.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("training_plan")
public class TrainingPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String planType;

    private Integer isActive;

    private String deactivationSource;

    private String deactivationReason;

    private Long deactivatedBy;

    private LocalDateTime deactivationTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long creatorUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

    public String getDeactivationSource() { return deactivationSource; }
    public void setDeactivationSource(String v) { this.deactivationSource = v; }

    public String getDeactivationReason() { return deactivationReason; }
    public void setDeactivationReason(String v) { this.deactivationReason = v; }

    public Long getDeactivatedBy() { return deactivatedBy; }
    public void setDeactivatedBy(Long v) { this.deactivatedBy = v; }

    public LocalDateTime getDeactivationTime() { return deactivationTime; }
    public void setDeactivationTime(LocalDateTime v) { this.deactivationTime = v; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Long getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(Long creatorUserId) { this.creatorUserId = creatorUserId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
