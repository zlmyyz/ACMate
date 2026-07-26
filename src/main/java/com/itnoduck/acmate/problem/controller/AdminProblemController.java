package com.itnoduck.acmate.problem.controller;

import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.problem.dto.AdminProblemSummaryResponse;
import com.itnoduck.acmate.problem.dto.MineProblemStatusFilter;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.service.AdminProblemQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员题目管理接口。
 *
 * <p>所有接口仅管理员可访问，由 SecurityConfig 的 {@code hasRole("ADMIN")} 控制。</p>
 * <p>Controller 不注入 Mapper，查询逻辑全部交由 Service 层处理。</p>
 */
@RestController
@RequestMapping("/api/admin/problems")
public class AdminProblemController {

    private final AdminProblemQueryService adminProblemQueryService;

    public AdminProblemController(AdminProblemQueryService adminProblemQueryService) {
        this.adminProblemQueryService = adminProblemQueryService;
    }

    /**
     * 管理员查询全部题库，支持状态、创建者、平台、难度和关键词组合筛选。
     *
     * <p>接口：{@code GET /api/admin/problems}</p>
     * <p>权限：仅管理员可访问；GET 请求不需要 CSRF Token。</p>
     * <p>查询参数：{@code page}（页码，最小 1）、{@code size}（每页数量，1-100）、
     * {@code status}（ALL / ACTIVE / INACTIVE，默认 ALL）、
     * {@code creatorUserId}（创建者 ID，可空）、
     * {@code platform}（平台）、{@code difficulty}（难度）、
     * {@code keyword}（关键词，匹配 title 或 externalProblemKey）。</p>
     * <p>成功响应：{@code 200 OK}。</p>
     * <p>错误响应：参数非法返回 {@code 400}；未登录返回 {@code 401}；
     * 非管理员返回 {@code 403}。</p>
     *
     * @param request 分页和筛选参数
     * @param status  状态筛选，默认 ALL
     * @return 全部题目列表，含创建者用户名和昵称
     */
    @GetMapping
    public PageResponse<AdminProblemSummaryResponse> listProblems(
            @Valid ProblemQueryRequest request,
            @RequestParam(defaultValue = "ALL") MineProblemStatusFilter status) {
        return adminProblemQueryService.listProblems(request, status);
    }
}
