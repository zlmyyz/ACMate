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
 * 题目查询接口。
 *
 * <p>所有接口由全局 anyRequest().authenticated() 保护，
 * 需登录后携带 JSESSIONID Cookie 访问。</p>
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

    @GetMapping("/{id}")
    public ProblemDetailResponse getProblem(@PathVariable long id) {
        return problemQueryService.getProblem(id);
    }

    @GetMapping
    public PageResponse<ProblemSummaryResponse> listProblems(@Valid ProblemQueryRequest request) {
        return problemQueryService.listProblems(request);
    }

    /**
     * 管理员创建题目。
     *
     * <p>creatorUserId 从当前认证用户获取，不由请求体指定。
     * 管理员权限由 SecurityFilterChain 的 hasRole("ADMIN") 控制。</p>
     */
    @PostMapping
    public ResponseEntity<ProblemDetailResponse> createProblem(@Valid @RequestBody CreateProblemRequest request,
                                                                @AuthenticationPrincipal AuthenticatedUser user) {
        ProblemDetailResponse response = problemCommandService.createProblem(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
