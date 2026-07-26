package com.itnoduck.acmate.problem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public class ProblemQueryRequest {

    @Min(1)
    private long page = 1;
    @Min(1) @Max(100)
    private long size = 20;
    @Pattern(regexp = "CUSTOM|CODEFORCES|NOWCODER|OTHER", message = "不支持的平台")
    private String platform;
    private String difficulty;
    @Size(max = 100)
    private String keyword;
    @Min(1)
    private Long creatorUserId;

    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) {
        this.platform = platform == null || platform.isBlank() ? null : platform.strip().toUpperCase(Locale.ROOT);
    }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty == null || difficulty.isBlank() ? null : difficulty.strip();
    }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) {
        this.keyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
    }
    public Long getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(Long creatorUserId) { this.creatorUserId = creatorUserId; }
    @Override
    public String toString() {
        return "ProblemQueryRequest{page=" + page + ", size=" + size + "}";
    }
}
