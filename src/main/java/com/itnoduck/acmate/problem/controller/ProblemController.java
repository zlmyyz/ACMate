package com.itnoduck.acmate.problem.controller;

import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.problem.dto.CreateProblemRequest;
import com.itnoduck.acmate.problem.dto.MineProblemStatusFilter;
import com.itnoduck.acmate.problem.dto.MyProblemSummaryResponse;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.dto.ProblemSummaryResponse;
import com.itnoduck.acmate.problem.dto.UpdateProblemRequest;
import com.itnoduck.acmate.problem.service.ProblemCommandService;
import com.itnoduck.acmate.problem.service.ProblemQueryService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目模块的 HTTP 表现层。
 *
 * <p>当前包含题目详情查询、公共分页查询、我的题目查询、创建和完整修改。</p>
 * <p>GET 接口只要求登录；POST 和 PUT 接口要求登录并通过 CSRF 校验。</p>
 * <p>资源级权限（创建者或管理员）由 Service 在查询资源后判断。</p>
 * <p>Controller 只负责 HTTP 参数绑定、认证主体提取和调用 Service，
 * 不直接访问 Mapper，不在 Controller 中实现业务规则。</p>
 */
@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemQueryService problemQueryService;
    private final ProblemCommandService problemCommandService;

    public ProblemController(ProblemQueryService problemQueryService,
                             ProblemCommandService problemCommandService) {
        this.problemQueryService = problemQueryService;
        this.problemCommandService = problemCommandService;
    }

    /**
     * 查询指定题目的完整信息。
     *
     * <p>接口：{@code GET /api/problems/{id}}</p>
     * <p>权限：所有已登录用户；GET 请求不需要 CSRF Token。</p>
     * <p>status=1 的题目所有登录用户可见；status=0 的题目仅创建者和管理员可见，
     * 其他普通用户返回 {@code 404}（不暴露停用题目存在性）。</p>
     * <p>成功响应：{@code 200 OK}，返回题目的完整信息。</p>
     * <p>错误响应：未登录返回 {@code 401}；
     * 题目不存在或无查看权限返回 {@code 404}。</p>
     *
     * @param id          路径参数中的题目 ID
     * @param currentUser 当前认证用户，用于停用题目可见性判断
     * @return 题目详情
     */
    @GetMapping("/{id}")
    public ProblemDetailResponse getProblem(@PathVariable long id,
                                            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return problemQueryService.getProblem(id, currentUser.getId(), currentUser.isAdmin());
    }

    /**
     * 分页查询正常状态的题目（公共题库）。
     *
     * <p>接口：{@code GET /api/problems}</p>
     * <p>权限：所有已登录用户；GET 请求不需要 CSRF Token。</p>
     * <p>查询参数：{@code page}（页码，最小 1）、{@code size}（每页数量，1-100）、
     * {@code platform}（平台筛选）、{@code difficulty}（难度精确匹配）、
     * {@code keyword}（关键词，匹配 {@code title} 或 {@code externalProblemKey}）。</p>
     * <p>{@code status=0} 的题目不会出现在列表中。</p>
     * <p>成功响应：{@code 200 OK}。</p>
     * <p>错误响应：未登录返回 {@code 401}；查询参数非法返回 {@code 400}。</p>
     *
     * @param request 分页查询参数
     * @return 分页题目列表
     */
    @GetMapping
    public PageResponse<ProblemSummaryResponse> listProblems(@Valid ProblemQueryRequest request) {
        return problemQueryService.listProblems(request);
    }

    /**
     * 查询当前登录用户创建的题目，支持按状态筛选。
     *
     * <p>接口：{@code GET /api/problems/mine}</p>
     * <p>权限：所有已登录用户；GET 请求不需要 CSRF Token。</p>
     * <p>查询参数：{@code page}（页码，最小 1）、{@code size}（每页数量，1-100）、
     * {@code status}（状态筛选：ALL / ACTIVE / INACTIVE，默认 ALL）、
     * {@code platform}（平台筛选）、{@code difficulty}（难度精确匹配）、
     * {@code keyword}（关键词，匹配 {@code title} 或 {@code externalProblemKey}）。</p>
     * <p>creatorUserId 固定为当前认证用户，不接受客户端传入其他用户 ID——
     * 每个用户只能查看自己创建的题目。</p>
     * <p>列表中包含 {@code status} 字段（ACTIVE/INACTIVE），
     * 公共列表不返回此字段，避免向其他用户暴露题目管理状态。</p>
     * <p>成功响应：{@code 200 OK}。</p>
     * <p>错误响应：未登录返回 {@code 401}；查询参数非法返回 {@code 400}。</p>
     *
     * @param request     分页和筛选参数
     * @param status      状态筛选：ALL / ACTIVE / INACTIVE
     * @param currentUser 当前认证用户，用于确定题目创建者
     * @return 当前用户的题目列表
     */
    @GetMapping("/mine")
    public PageResponse<MyProblemSummaryResponse> listMyProblems(
            @Valid ProblemQueryRequest request,
            @RequestParam(defaultValue = "ALL") MineProblemStatusFilter status,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return problemQueryService.listMyProblems(request, status, currentUser.getId());
    }

    /**
     * 由当前登录用户创建一道题目。
     *
     * <p>接口：{@code POST /api/problems}</p>
     * <p>权限：所有已登录用户，包括普通用户和管理员。</p>
     * <p>安全：该接口会修改服务器状态，必须携带有效 CSRF Token。</p>
     * <p>所有权：创建者 ID 从当前认证主体获取，请求体不能指定
     * {@code creatorUserId}；题目状态由服务端固定为正常状态。</p>
     * <p>成功响应：{@code 201 Created}，返回创建后的题目详情。</p>
     * <p>错误响应：请求参数非法返回 {@code 400}；未登录返回 {@code 401}；
     * CSRF 校验失败返回 {@code 403}；平台题目标识冲突返回 {@code 409}。</p>
     *
     * @param request 题目创建请求，只包含客户端允许填写的题目字段
     * @param user    当前认证用户，用于确定题目创建者
     * @return 创建后的题目详情
     */
    @PostMapping
    public ResponseEntity<ProblemDetailResponse> createProblem(@Valid @RequestBody CreateProblemRequest request,
                                                                @AuthenticationPrincipal AuthenticatedUser user) {
        ProblemDetailResponse response = problemCommandService.createProblem(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 完整修改指定题目的可编辑信息。
     *
     * <p>接口：{@code PUT /api/problems/{id}}</p>
     * <p>权限：题目创建者可以修改自己的正常或停用题目；
     * 管理员可以修改任意正常或停用的题目；
     * 其他普通用户对正常题目返回 {@code 403 Forbidden}，
     * 对停用题目返回 {@code 404}（不暴露存在性）。</p>
     * <p>编辑停用题目仅修改内容字段，不会自动恢复 status。</p>
     * <p>安全：该接口会修改服务器状态，必须携带有效 CSRF Token。</p>
     * <p>参数来源：题目 ID 来自路径参数；可编辑字段来自请求体；
     * 当前操作者 ID 和管理员状态来自认证主体，客户端不能指定。</p>
     * <p>成功响应：{@code 200 OK}，返回数据库中的最新题目详情。</p>
     * <p>错误响应：请求参数非法返回 {@code 400}；未登录返回 {@code 401}；
     * 无资源管理权限或 CSRF 校验失败返回 {@code 403}；
     * 题目不存在或停用题目无权限返回 {@code 404}；
     * 平台题目标识冲突返回 {@code 409}。</p>
     *
     * @param id          路径参数中的题目 ID
     * @param request     题目完整更新请求，只包含允许客户端编辑的字段
     * @param currentUser 当前认证用户，用于判断题目所有权和管理员权限
     * @return 更新后的题目详情
     */
    @PutMapping("/{id}")
    public ProblemDetailResponse updateProblem(@PathVariable long id,
                                                @Valid @RequestBody UpdateProblemRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return problemCommandService.updateProblem(id, request, currentUser.getId(), currentUser.isAdmin());
    }

    /**
     * 停用指定题目（ACTIVE → INACTIVE）。
     *
     * <p>接口：{@code POST /api/problems/{id}/deactivate}</p>
     * <p>权限：题目创建者和管理员可以停用；
     * 其他普通用户对正常题目返回 {@code 403}，对停用题目返回 {@code 404}。</p>
     * <p>安全：该接口会修改服务器状态，必须携带有效 CSRF Token。</p>
     * <p>重复停用直接返回 {@code 204}，不视为错误。</p>
     * <p>停用是逻辑操作，不会物理删除记录；
     * 停用的题目仍占用 platform+externalProblemKey 唯一性。</p>
     * <p>成功响应：{@code 204 No Content}。</p>
     * <p>错误响应：参数非法返回 {@code 400}；未登录返回 {@code 401}；
     * 无权限或 CSRF 校验失败返回 {@code 403}；
     * 题目不存在或停用题目无权限返回 {@code 404}。</p>
     *
     * @param id          路径参数中的题目 ID
     * @param currentUser 当前认证用户，用于判断题目所有权和管理员权限
     * @return 204 No Content
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateProblem(@PathVariable long id,
                                                   @AuthenticationPrincipal AuthenticatedUser currentUser) {
        problemCommandService.deactivateProblem(id, currentUser.getId(), currentUser.isAdmin());
        return ResponseEntity.noContent().build();
    }

    /**
     * 恢复指定题目（INACTIVE → ACTIVE）。
     *
     * <p>接口：{@code POST /api/problems/{id}/restore}</p>
     * <p>权限：题目创建者和管理员可以恢复；
     * 其他普通用户对正常题目返回 {@code 403}，对停用题目返回 {@code 404}。</p>
     * <p>安全：该接口会修改服务器状态，必须携带有效 CSRF Token。</p>
     * <p>重复恢复直接返回 {@code 204}，不视为错误。</p>
     * <p>成功响应：{@code 204 No Content}。</p>
     * <p>错误响应：参数非法返回 {@code 400}；未登录返回 {@code 401}；
     * 无权限或 CSRF 校验失败返回 {@code 403}；
     * 题目不存在返回 {@code 404}。</p>
     *
     * @param id          路径参数中的题目 ID
     * @param currentUser 当前认证用户，用于判断题目所有权和管理员权限
     * @return 204 No Content
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restoreProblem(@PathVariable long id,
                                                @AuthenticationPrincipal AuthenticatedUser currentUser) {
        problemCommandService.restoreProblem(id, currentUser.getId(), currentUser.isAdmin());
        return ResponseEntity.noContent().build();
    }
}
