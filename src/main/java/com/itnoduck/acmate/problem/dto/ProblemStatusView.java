package com.itnoduck.acmate.problem.dto;

/**
 * 向前端暴露的题目状态视图。
 * 不直接返回数据库数字，避免前端依赖内部编码。
 */
public enum ProblemStatusView {
    ACTIVE,
    INACTIVE;

    /**
     * 根据数据库 status 字段（1=正常, 0=停用）转换为视图枚举。
     */
    public static ProblemStatusView fromStatus(Integer status) {
        return status != null && status == 1 ? ACTIVE : INACTIVE;
    }
}
