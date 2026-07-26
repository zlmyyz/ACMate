package com.itnoduck.acmate.problem.service;

import com.itnoduck.acmate.problem.dto.CreateProblemRequest;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;

/**
 * 题目命令服务（创建、修改、删除）。
 */
public interface ProblemCommandService {

    /**
     * 管理员创建题目。
     *
     * @param request       请求 DTO（不含 creatorUserId 和 status）
     * @param creatorUserId 由 Controller 从当前认证用户获取，客户端不能指定
     * @return 创建后的题目详情
     */
    ProblemDetailResponse createProblem(CreateProblemRequest request, long creatorUserId);
}
