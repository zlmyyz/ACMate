package com.itnoduck.acmate.problem.dto;

/**
 * "我的题目"列表的状态筛选维度。
 * 不暴露数据库数字状态，向前端提供语义化选项。
 */
public enum MineProblemStatusFilter {
    ALL,
    ACTIVE,
    INACTIVE
}
