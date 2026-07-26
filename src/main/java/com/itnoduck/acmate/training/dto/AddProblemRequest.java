package com.itnoduck.acmate.training.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AddProblemRequest {

    @NotNull(message = "题目ID不能为空")
    private Long problemId;

    private int sortOrder;

    @Min(0)
    private int requiredFlag = 1;

    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public int getRequiredFlag() { return requiredFlag; }
    public void setRequiredFlag(int requiredFlag) { this.requiredFlag = requiredFlag; }
}
