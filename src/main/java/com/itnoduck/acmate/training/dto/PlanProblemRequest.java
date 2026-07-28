package com.itnoduck.acmate.training.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PlanProblemRequest {

    @NotNull(message = "题目ID不能为空")
    private Long problemId;

    @Min(0)
    private Integer sortOrder;

    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
