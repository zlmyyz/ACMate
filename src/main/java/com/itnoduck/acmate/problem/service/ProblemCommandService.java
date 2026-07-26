package com.itnoduck.acmate.problem.service;

import com.itnoduck.acmate.problem.dto.CreateProblemRequest;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.UpdateProblemRequest;

/**
 * 题目命令服务。
 *
 * <p>当前包含题目创建和完整更新。停用、恢复和删除尚未实现。</p>
 */
public interface ProblemCommandService {

    /**
     * 由当前登录用户创建题目。
     *
     * <p>普通用户和管理员均可创建。{@code creatorUserId} 来自认证主体，
     * 不由请求体指定；{@code status} 由服务端固定为 {@code 1}。</p>
     * <p>Service 不负责区分普通用户和管理员——双方走同一创建流程。</p>
     *
     * @param request       请求 DTO（不含 creatorUserId 和 status）
     * @param creatorUserId 当前认证用户 ID
     * @return 创建后的题目详情
     */
    ProblemDetailResponse createProblem(CreateProblemRequest request, long creatorUserId);

    /**
     * 完整修改指定题目的可编辑信息。
     *
     * <p>权限规则：</p>
     * <ul>
     *   <li>题目创建者可以修改自己的题目；</li>
     *   <li>管理员可以修改任意正常状态（{@code status=1}）的题目；</li>
     *   <li>其他已登录用户返回 {@code 403 Forbidden}。</li>
     * </ul>
     * <p>修改不会变更 {@code creatorUserId}、{@code status} 和 {@code createTime}。
     * 可空字段提交空白可清空为 {@code null}。</p>
     *
     * @param problemId      目标题目 ID
     * @param request        完整更新请求，只包含允许客户端编辑的字段
     * @param operatorUserId 当前认证用户 ID
     * @param operatorAdmin  当前认证用户是否为管理员
     * @return 更新后重新查询的题目详情
     */
    ProblemDetailResponse updateProblem(long problemId, UpdateProblemRequest request,
                                        long operatorUserId, boolean operatorAdmin);
}
