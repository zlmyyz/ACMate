package com.itnoduck.acmate.problem.controller;

import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.problem.dto.CreateProblemRequest;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.dto.ProblemSummaryResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目模块的 HTTP 表现层。
 *
 * <p>当前包含题目详情查询、分页查询和创建接口。
 * 三个接口均要求用户已登录，POST 创建接口还要求携带有效 CSRF Token。</p>
 * <p>{@code creatorUserId} 从 {@link AuthenticatedUser} 认证主体获取，
 * 不由请求体指定。</p>
 * <p>Controller 只负责参数绑定和调用 Service，不直接操作 Mapper，
 * 不在 Controller 中实现业务规则。</p>
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
     * <p>成功响应：{@code 200 OK}，返回题目的完整信息。</p>
     * <p>错误响应：未登录返回 {@code 401}；
     * 题目不存在或已停用（{@code status != 1}）返回 {@code 404}。</p>
     *
     * @param id 路径参数中的题目 ID
     * @return 题目详情
     */
    @GetMapping("/{id}")
    public ProblemDetailResponse getProblem(@PathVariable long id) {
        return problemQueryService.getProblem(id);
    }

    /**
     * 分页查询正常状态的题目。
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
     * @param user 当前认证用户，用于确定题目创建者
     * @return 创建后的题目详情
     */
    @PostMapping
    public ResponseEntity<ProblemDetailResponse> createProblem(@Valid @RequestBody CreateProblemRequest request,
                                                                @AuthenticationPrincipal AuthenticatedUser user) {
        ProblemDetailResponse response = problemCommandService.createProblem(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
