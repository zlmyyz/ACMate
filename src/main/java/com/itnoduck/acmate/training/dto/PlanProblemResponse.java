package com.itnoduck.acmate.training.dto;

public class PlanProblemResponse {

    private Long id;
    private Long problemId;
    private String problemTitle;
    private String platform;
    private String difficulty;
    private boolean problemActive;
    private int sortOrder;
    private boolean required;
    private String myStatus;
    private String performanceNote;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }

    public String getProblemTitle() { return problemTitle; }
    public void setProblemTitle(String problemTitle) { this.problemTitle = problemTitle; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public boolean isProblemActive() { return problemActive; }
    public void setProblemActive(boolean problemActive) { this.problemActive = problemActive; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String getMyStatus() { return myStatus; }
    public void setMyStatus(String myStatus) { this.myStatus = myStatus; }

    public String getPerformanceNote() { return performanceNote; }
    public void setPerformanceNote(String performanceNote) { this.performanceNote = performanceNote; }
}
