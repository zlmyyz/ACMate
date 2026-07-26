package com.itnoduck.acmate.training.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("training_plan_member")
public class TrainingPlanMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getJoinTime() { return joinTime; }
    public void setJoinTime(LocalDateTime joinTime) { this.joinTime = joinTime; }
}
