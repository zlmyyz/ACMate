package com.itnoduck.acmate.problem.service;

import com.itnoduck.acmate.problem.dto.CreateProblemRequest;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.UpdateProblemRequest;

/**
 * 题目命令服务。
 */
public interface ProblemCommandService {

    /**
     * 由当前登录用户创建题目。
     *
     * <p>普通用户和管理员均可创建。{@code creatorUserId} 来自认证主体，
     * 不由请求体指定；{@code status} 由服务端固定为 {@code 1}。</p>
     *
     * @param request       请求 DTO（不含 creatorUserId 和 status）
     * @param creatorUserId 当前认证用户 ID
     * @return 创建后的题目详情
     */
    ProblemDetailResponse createProblem(CreateProblemRequest request, long creatorUserId);

    /**
     * 完整修改指定题目的可编辑信息。
     *
     * <p>权限规则：创建者或管理员可以管理题目；非创建者非管理员对正常题目返回 403，
     * 对停用题目返回 404。</p>
     * <p>修改不会变更 {@code creatorUserId}、{@code status} 和 {@code createTime}。
     * 编辑停用题目不会自动恢复。</p>
     *
     * @param problemId      目标题目 ID
     * @param request        完整更新请求
     * @param operatorUserId 当前认证用户 ID
     * @param operatorAdmin  当前认证用户是否为管理员
     * @return 更新后重新查询的题目详情
     */
    ProblemDetailResponse updateProblem(long problemId, UpdateProblemRequest request,
                                        long operatorUserId, boolean operatorAdmin);

    /**
     * 停用题目（ACTIVE → INACTIVE）。
     *
     * <p>需要先查询资源再判断权限——资源所有权无法在 URL 层确定。</p>
     * <p>创建者和管理员可以停用；停用后题目仍占用 platform+externalProblemKey；
     * 停用题目对无关普通用户返回 404，不暴露私有停用题目存在性。</p>
     * <p>重复停用直接返回成功，不视为错误。</p>
     * <p>{@code operatorUserId} 和 {@code operatorAdmin} 来自认证主体，不由请求指定。</p>
     *
     * @param problemId      目标题目 ID
     * @param operatorUserId 当前认证用户 ID
     * @param operatorAdmin  当前认证用户是否为管理员
     */
    void deactivateProblem(long problemId, long operatorUserId, boolean operatorAdmin);

    /**
     * 恢复题目（INACTIVE → ACTIVE）。
     *
     * <p>创建者和管理员可以恢复停用的题目。重复恢复直接返回成功。</p>
     *
     * @param problemId      目标题目 ID
     * @param operatorUserId 当前认证用户 ID
     * @param operatorAdmin  当前认证用户是否为管理员
     */
    void restoreProblem(long problemId, long operatorUserId, boolean operatorAdmin);
}
