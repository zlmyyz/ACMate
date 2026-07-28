package com.itnoduck.acmate.oj.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeforcesProblemDto {

    @JsonProperty("contestId")
    private Long contestId;

    @JsonProperty("problemsetName")
    private String problemsetName;

    private String index;

    private String name;

    public Long getContestId() { return contestId; }
    public void setContestId(Long v) { this.contestId = v; }
    public String getProblemsetName() { return problemsetName; }
    public void setProblemsetName(String v) { this.problemsetName = v; }
    public String getIndex() { return index; }
    public void setIndex(String v) { this.index = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
}
