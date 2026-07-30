package com.itnoduck.acmate.training.dto;

public class ProgressSummaryResponse {

    private int requiredCompletedCount;
    private int requiredTotal;
    private int optionalCompletedCount;
    private int optionalTotal;

    public int getRequiredCompletedCount() { return requiredCompletedCount; }
    public void setRequiredCompletedCount(int v) { requiredCompletedCount = v; }
    public int getRequiredTotal() { return requiredTotal; }
    public void setRequiredTotal(int v) { requiredTotal = v; }
    public int getOptionalCompletedCount() { return optionalCompletedCount; }
    public void setOptionalCompletedCount(int v) { optionalCompletedCount = v; }
    public int getOptionalTotal() { return optionalTotal; }
    public void setOptionalTotal(int v) { optionalTotal = v; }
}
