package com.itnoduck.acmate.practice;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.common.cache.RedisJsonCache;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import tools.jackson.core.type.TypeReference;
import java.time.Duration;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.problem.service.NowcoderProblemImportService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/practice")
public class PracticeController {
    private final PracticeRunner runner;
    private final ProblemMapper problems;
    private final NowcoderProblemImportService importer;
    private final RedisJsonCache cache;
    private final PracticeRateLimit rateLimit;
    private final PracticeAccess access;
    public PracticeController(PracticeRunner runner,ProblemMapper problems,NowcoderProblemImportService importer,RedisJsonCache cache,PracticeRateLimit rateLimit,PracticeAccess access){this.runner=runner;this.problems=problems;this.importer=importer;this.cache=cache;this.rateLimit=rateLimit;this.access=access;}
    public record RunRequest(@NotNull @Positive Long problemId, @Min(0) @Max(9) Integer sampleIndex, @NotBlank @Pattern(regexp="CPP17|C17|JAVA17|PYTHON3|CSHARP|TYPESCRIPT|JAVASCRIPT|GO|RUST") String language,@NotBlank @Size(max=65536) String sourceCode,@Pattern(regexp="PUBLIC_SAMPLE|CUSTOM") String inputMode,@Size(max=65536) String input){}
    @GetMapping("/status") public Map<String,Object> status(HttpServletRequest request){return Map.of("available",access.allowed(request)&&runner.available(),"languages",Arrays.stream(PracticeLanguage.values()).map(Enum::name).toList());}
    @PostMapping("/run") public PracticeRunner.Result run(@Valid @RequestBody RunRequest request,@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest http){
        access.require(http);
        rateLimit.check(user.getId(),"run",30);
        requireProblem(request.problemId());
        String input;
        if("CUSTOM".equals(request.inputMode())) {
            if(request.input()==null)throw new BusinessException(400,"请提供自测输入，可为空字符串");
            input=request.input();
        }else {
            var samples=loadSamples(request.problemId());
            if(request.sampleIndex()==null||request.sampleIndex()>=samples.size())throw new BusinessException(400,"该公开样例不存在，请刷新题目重试");
            input=samples.get(request.sampleIndex()).input();
        }
        return runner.run(request.language(),request.sourceCode(),input);
    }
    private void requireProblem(long id){
        var p=problems.selectById(id);
        if(p==null||!Integer.valueOf(1).equals(p.getStatus())||!"NOWCODER".equals(p.getPlatform()))throw new BusinessException(404,"题目不存在或已停用");
    }
    @GetMapping("/problems/{id}/samples") public List<NowcoderProblemImportService.Sample> samples(@PathVariable long id,@AuthenticationPrincipal AuthenticatedUser user){
        rateLimit.check(user.getId(),"samples",60);
        return loadSamples(id);
    }
    private List<NowcoderProblemImportService.Sample> loadSamples(long id){
        var p=problems.selectById(id);
        if(p==null||!Integer.valueOf(1).equals(p.getStatus())||!"NOWCODER".equals(p.getPlatform())||p.getSourceUrl()==null)throw new BusinessException(404,"没有可读取的牛客公开样例");
        return cache.getOrLoad(RedisJsonCache.keyFor("acmate:cache:samples:v1",p.getSourceUrl()),new TypeReference<List<NowcoderProblemImportService.Sample>>() {},Duration.ofMinutes(10),() -> importer.samples(p.getSourceUrl()));
    }
}
