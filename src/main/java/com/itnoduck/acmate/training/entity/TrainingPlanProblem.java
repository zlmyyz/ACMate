package com.itnoduck.acmate.training.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("training_plan_problem")
public class TrainingPlanProblem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long problemId;

    private Integer sortOrder;

    private Integer requiredFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getRequiredFlag() { return requiredFlag; }
    public void setRequiredFlag(Integer requiredFlag) { this.requiredFlag = requiredFlag; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
