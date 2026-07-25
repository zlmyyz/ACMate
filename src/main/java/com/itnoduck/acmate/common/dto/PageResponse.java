package com.itnoduck.acmate.common.dto;

import java.util.List;

/**
 * 通用分页响应。
 */
public record PageResponse<T>(
        long page,
        long size,
        long total,
        long pages,
        List<T> records
) {}
