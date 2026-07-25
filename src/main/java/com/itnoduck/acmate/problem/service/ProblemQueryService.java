package com.itnoduck.acmate.problem.service;

import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.dto.ProblemSummaryResponse;

/**
 * 题目查询服务。
 */
public interface ProblemQueryService {

    /**
     * 根据 ID 查询题目详情，仅返回 status=1 的题目。
     *
     * @param id 题目 ID，必须大于 0
     * @return 题目详情
     * @throws com.itnoduck.acmate.common.exception.BusinessException 题目不存在或已禁用时抛出 404
     */
    ProblemDetailResponse getProblem(long id);

    /**
     * 分页查询题目列表，固定过滤 status=1，支持平台、难度和关键词筛选。
     */
    PageResponse<ProblemSummaryResponse> listProblems(ProblemQueryRequest request);
}
