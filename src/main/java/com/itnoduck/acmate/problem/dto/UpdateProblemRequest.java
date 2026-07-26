package com.itnoduck.acmate.problem.dto;

import com.itnoduck.acmate.problem.support.ProblemFieldNormalizer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.Locale;

/**
 * 题目完整更新请求，对应 {@code PUT /api/problems/{id}}。
 *
 * <p>这是完整更新，所有可编辑字段的当前值都必须包含在请求体中。
 * 如需清空可选字段，提交该字段为空白即可。</p>
 * <p>不包含 {@code creatorUserId} 和 {@code status}——所有权和状态由服务端管理。</p>
 * <p>不包含 {@code createTime} 和 {@code updateTime}——由数据库自动维护。</p>
 */
@Getter
public class UpdateProblemRequest {

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

    @Size(max = 2097152)
    private String contentMd;

    public void setPlatform(String platform) {
        this.platform = platform == null || platform.isBlank() ? null : platform.strip().toUpperCase(Locale.ROOT);
    }

    public void setExternalProblemKey(String externalProblemKey) {
        this.externalProblemKey = ProblemFieldNormalizer.normalizeOptionalString(externalProblemKey);
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.strip();
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = ProblemFieldNormalizer.normalizeOptionalString(sourceUrl);
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = ProblemFieldNormalizer.normalizeOptionalString(difficulty);
    }

    public String getTags() { return tags; }

    public void setTags(String tags) {
        this.tags = ProblemFieldNormalizer.normalizeTags(tags);
    }

    public void setContentMd(String contentMd) {
        this.contentMd = contentMd;
    }

    @Override
    public String toString() {
        return "UpdateProblemRequest{platform='" + platform + "', title='" + title + "'}";
    }
}
