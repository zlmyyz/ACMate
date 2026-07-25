package com.itnoduck.acmate.problem.controller;

import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.dto.ProblemSummaryResponse;
import com.itnoduck.acmate.problem.service.ProblemQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public ProblemController(ProblemQueryService problemQueryService) {
        this.problemQueryService = problemQueryService;
    }

    @GetMapping("/{id}")
    public ProblemDetailResponse getProblem(@PathVariable long id) {
        return problemQueryService.getProblem(id);
    }

    @GetMapping
    public PageResponse<ProblemSummaryResponse> listProblems(@Valid ProblemQueryRequest request) {
        return problemQueryService.listProblems(request);
    }
}
