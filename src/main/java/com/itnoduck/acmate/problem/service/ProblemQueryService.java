package com.itnoduck.acmate.problem.service;

import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.problem.dto.MineProblemStatusFilter;
import com.itnoduck.acmate.problem.dto.MyProblemSummaryResponse;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.dto.ProblemSummaryResponse;

/**
 * 题目查询服务。
 */
public interface ProblemQueryService {

    /**
     * 根据 ID 查询题目详情。
     * status=1 的题目所有登录用户可见；status=0 仅创建者和管理员可见，
     * 其他用户返回 404（不暴露停用题目存在性）。
     *
     * @param id            题目 ID，必须大于 0
     * @param viewerUserId  当前查看者 ID
     * @param viewerAdmin   当前查看者是否为管理员
     * @return 题目详情
     * @throws com.itnoduck.acmate.common.exception.BusinessException 题目不存在或无权限时抛出 404
     */
    ProblemDetailResponse getProblem(long id, long viewerUserId, boolean viewerAdmin);

    /**
     * 分页查询公共题目列表，固定过滤 status=1，支持平台、难度和关键词筛选。
     */
    PageResponse<ProblemSummaryResponse> listProblems(ProblemQueryRequest request);

    /**
     * 分页查询当前用户的题目（含正常和停用）。
     * creatorUserId 必须来自服务端（currentUserId），不接受客户端传入。
     *
     * @param request       分页与筛选参数（page、size、platform、difficulty、keyword）
     * @param statusFilter  状态筛选：ALL / ACTIVE / INACTIVE
     * @param currentUserId 当前认证用户 ID
     * @return 当前用户的题目列表
     */
    PageResponse<MyProblemSummaryResponse> listMyProblems(ProblemQueryRequest request,
                                                          MineProblemStatusFilter statusFilter,
                                                          long currentUserId);
}
