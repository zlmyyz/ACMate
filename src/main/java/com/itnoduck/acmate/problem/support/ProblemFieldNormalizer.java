package com.itnoduck.acmate.problem.support;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 题目输入字段规范化工具。
 *
 * <p>统一处理 {@code CreateProblemRequest} 和 {@code UpdateProblemRequest} 的
 * 字段规范化规则，确保创建和修改接口的输入行为一致。</p>
 * <p>纯函数，无状态，不访问数据库。</p>
 */
public final class ProblemFieldNormalizer {

    private ProblemFieldNormalizer() {}

    /**
     * strip 后 blank 转 null。
     */
    public static String normalizeOptionalString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    /**
     * 规范化 tags：按逗号切分 → 每项 strip → 去空 → 去重（保持首次出现顺序）→ 逗号连接。
     * 最终空字符串转为 null。
     */
    public static String normalizeTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return null;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String part : tags.split(",")) {
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) {
                seen.add(trimmed);
            }
        }
        String result = String.join(",", seen);
        return result.isEmpty() ? null : result;
    }
}
