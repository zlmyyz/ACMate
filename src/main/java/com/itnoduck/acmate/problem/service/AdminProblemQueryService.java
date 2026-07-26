package com.itnoduck.acmate.problem.service;

import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.problem.dto.AdminProblemSummaryResponse;
import com.itnoduck.acmate.problem.dto.MineProblemStatusFilter;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;

/**
 * 管理员题目查询服务。
 * 负责全站题库查询，支持组合状态、创建者、平台等筛选及创建者信息批量加载。
 */
public interface AdminProblemQueryService {

    /**
     * 管理员分页查询全站题目，支持 ALL/ACTIVE/INACTIVE 状态筛选、
     * 按创建者筛选、以及平台/难度/关键词组合。
     *
     * @param request      分页与公共筛选参数
     * @param statusFilter 状态筛选：ALL / ACTIVE / INACTIVE
     * @return 管理员全部题目列表，含创建者信息
     */
    PageResponse<AdminProblemSummaryResponse> listProblems(ProblemQueryRequest request,
                                                            MineProblemStatusFilter statusFilter);
}
