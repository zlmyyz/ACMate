package com.itnoduck.acmate.oj.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeforcesApiResponse<T> {

    private String status;
    private String comment;

    private List<T> result;

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getComment() { return comment; }
    public void setComment(String v) { this.comment = v; }

    @JsonProperty("result")
    public List<T> getResult() { return result; }
    @JsonProperty("result")
    public void setResult(List<T> v) { this.result = v; }
}
