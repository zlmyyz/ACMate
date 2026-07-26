package com.itnoduck.acmate.problem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 创建题目请求。
 *
 * <p>creatorUserId 和 status 不在 DTO 中——由服务端根据当前认证用户和业务规则确定。</p>
 *
 * <p>tags 当前存储为逗号分隔字符串（VARCHAR(255)），暂不拆分为关联表。</p>
 */
@Getter
public class CreateProblemRequest {

    @NotBlank
    @Pattern(regexp = "CUSTOM|CODEFORCES|NOWCODER|OTHER", message = "不支持的平台")
    private String platform;

    @Size(max = 64)
    private String externalProblemKey;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 1024)
    private String sourceUrl;

    @Size(max = 32)
    private String difficulty;

    @Size(max = 255)
    private String tags;

    @Size(max = 2097152) // MEDIUMTEXT practical limit
    private String contentMd;

    public void setPlatform(String platform) {
        this.platform = platform == null || platform.isBlank() ? null : platform.strip().toUpperCase(Locale.ROOT);
    }

    public void setExternalProblemKey(String externalProblemKey) {
        this.externalProblemKey = externalProblemKey == null || externalProblemKey.isBlank() ? null : externalProblemKey.strip();
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.strip();
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl == null || sourceUrl.isBlank() ? null : sourceUrl.strip();
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty == null || difficulty.isBlank() ? null : difficulty.strip();
    }

    public String getTags() { return tags; }

    /**
     * 规范化 tags：按逗号切分 → 每项 strip → 去空 → 去重（保持顺序）→ 逗号连接。
     * 最终空字符串转为 null。
     */
    public void setTags(String tags) {
        if (tags == null || tags.isBlank()) {
            this.tags = null;
            return;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String part : tags.split(",")) {
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) {
                seen.add(trimmed);
            }
        }
        String result = String.join(",", seen);
        this.tags = result.isEmpty() ? null : result;
    }

    public void setContentMd(String contentMd) {
        this.contentMd = contentMd;
    }

    @Override
    public String toString() {
        return "CreateProblemRequest{platform='" + platform + "', title='" + title + "'}";
    }
}
