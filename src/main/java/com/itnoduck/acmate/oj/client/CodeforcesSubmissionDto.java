package com.itnoduck.acmate.oj.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeforcesSubmissionDto {

    private Long id;

    @JsonProperty("creationTimeSeconds")
    private Long creationTimeSeconds;

    private String verdict;

    @JsonProperty("programmingLanguage")
    private String programmingLanguage;

    @JsonProperty("passedTestCount")
    private Integer passedTestCount;

    @JsonProperty("timeConsumedMillis")
    private Integer timeConsumedMillis;

    @JsonProperty("memoryConsumedBytes")
    private Long memoryConsumedBytes;

    private CodeforcesProblemDto problem;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCreationTimeSeconds() { return creationTimeSeconds; }
    public void setCreationTimeSeconds(Long v) { this.creationTimeSeconds = v; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String v) { this.verdict = v; }
    public String getProgrammingLanguage() { return programmingLanguage; }
    public void setProgrammingLanguage(String v) { this.programmingLanguage = v; }
    public Integer getPassedTestCount() { return passedTestCount; }
    public void setPassedTestCount(Integer v) { this.passedTestCount = v; }
    public Integer getTimeConsumedMillis() { return timeConsumedMillis; }
    public void setTimeConsumedMillis(Integer v) { this.timeConsumedMillis = v; }
    public Long getMemoryConsumedBytes() { return memoryConsumedBytes; }
    public void setMemoryConsumedBytes(Long v) { this.memoryConsumedBytes = v; }
    public CodeforcesProblemDto getProblem() { return problem; }
    public void setProblem(CodeforcesProblemDto v) { this.problem = v; }
}
