package com.itnoduck.acmate.training.dto;

import jakarta.validation.Valid;
import java.util.List;

public class UpdateProblemsRequest {

    @Valid
    private List<PlanProblemRequest> problems;

    public List<PlanProblemRequest> getProblems() { return problems; }
    public void setProblems(List<PlanProblemRequest> problems) { this.problems = problems; }
}
