package com.itnoduck.acmate.problem.controller;

import com.itnoduck.acmate.common.cache.RedisJsonCache;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.dto.UpdateProblemRequest;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.problem.service.ProblemCommandService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/problem-reviews")
public class ProblemReviewController {
    private final JdbcTemplate jdbc;
    private final ProblemMapper problems;
    private final ProblemCommandService command;
    private final ObjectMapper json;
    public ProblemReviewController(JdbcTemplate jdbc,ProblemMapper problems,ProblemCommandService command,ObjectMapper json){this.jdbc=jdbc;this.problems=problems;this.command=command;this.json=json;}
    public record Decision(@Size(max=500) String note){}
    private void admin(AuthenticatedUser user){if(!user.isAdmin())throw new BusinessException(403,"仅管理员可以审核题目");}
    private String fingerprint(Problem p){return RedisJsonCache.keyFor("problem",json.writeValueAsString(java.util.Arrays.asList(p.getPlatform(),p.getExternalProblemKey(),p.getTitle(),p.getSourceUrl(),p.getDifficulty(),p.getTags(),p.getContentMd()))).substring("problem:".length());}
    private String snapshot(Problem p){
        var original=new UpdateProblemRequest();original.setPlatform(p.getPlatform());original.setExternalProblemKey(p.getExternalProblemKey());original.setTitle(p.getTitle());original.setSourceUrl(p.getSourceUrl());original.setDifficulty(p.getDifficulty());original.setTags(p.getTags());original.setContentMd(p.getContentMd());return json.writeValueAsString(original);
    }
    @PostMapping("/{problemId}") @Transactional
    public Map<String,Object> propose(@PathVariable long problemId,@Valid @RequestBody UpdateProblemRequest request,@AuthenticationPrincipal AuthenticatedUser user){
        if(jdbc.queryForList("SELECT id FROM app_user WHERE id=? AND status=1 FOR UPDATE",Long.class,user.getId()).isEmpty())throw new BusinessException(401,"账号不可用");
        Long pending=jdbc.queryForObject("SELECT COUNT(*) FROM problem_change_request WHERE author_user_id=? AND status='PENDING'",Long.class,user.getId());
        if(pending != null && pending >= 5)throw new BusinessException(429,"待审核建议最多保留 5 条，请等待审核");
        var locks=jdbc.queryForList("SELECT id FROM problem WHERE id=? AND status=1 FOR UPDATE",Long.class,problemId);
        if(locks.isEmpty())throw new BusinessException(404,"题目不存在或已停用");
        Problem p=problems.selectById(problemId);
        if("CUSTOM".equals(p.getPlatform()))throw new BusinessException(400,"原创题目由创建者或管理员直接维护");
        if(!p.getPlatform().equals(request.getPlatform()) || !java.util.Objects.equals(p.getExternalProblemKey(),request.getExternalProblemKey()))throw new BusinessException(400,"修改建议不能更改平台和题号");
        jdbc.update("INSERT INTO problem_change_request(problem_id,author_user_id,base_update_time,base_fingerprint,original_content,proposed_content) VALUES(?,?,?,?,?,?)",problemId,user.getId(),p.getUpdateTime(),fingerprint(p),snapshot(p),json.writeValueAsString(request));
        return Map.of("message","修改建议已提交，等待管理员审核");
    }
    private com.itnoduck.acmate.common.dto.PageResponse<Map<String,Object>> page(String where,Object[] args,int page,int size){
        page=Math.max(1,page);size=Math.max(1,Math.min(size,20));
        Long count=jdbc.queryForObject("SELECT COUNT(*) FROM problem_change_request WHERE "+where,Long.class,args);
        var params=new java.util.ArrayList<Object>(java.util.Arrays.asList(args));params.add(size);params.add((long)(page-1)*size);
        var rows=jdbc.queryForList("SELECT id,problem_id,author_user_id,status,reviewer_user_id,review_time,review_note,create_time FROM problem_change_request WHERE "+where+" ORDER BY id DESC LIMIT ? OFFSET ?",params.toArray());
        long total=count==null?0:count;return new com.itnoduck.acmate.common.dto.PageResponse<>(page,size,total,(total+size-1)/size,rows);
    }
    @GetMapping("/problem/{problemId}")
    public com.itnoduck.acmate.common.dto.PageResponse<Map<String,Object>> history(@PathVariable long problemId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@AuthenticationPrincipal AuthenticatedUser user){
        return user.isAdmin()?page("problem_id=?",new Object[]{problemId},page,size):page("problem_id=? AND author_user_id=?",new Object[]{problemId,user.getId()},page,size);
    }
    @GetMapping("/pending")
    public com.itnoduck.acmate.common.dto.PageResponse<Map<String,Object>> pending(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@AuthenticationPrincipal AuthenticatedUser user){admin(user);return page("status='PENDING'",new Object[]{},page,size);}
    @GetMapping("/{id}")
    public Map<String,Object> detail(@PathVariable long id,@AuthenticationPrincipal AuthenticatedUser user){
        var rows=user.isAdmin()?jdbc.queryForList("SELECT id,original_content,proposed_content FROM problem_change_request WHERE id=?",id):jdbc.queryForList("SELECT id,original_content,proposed_content FROM problem_change_request WHERE id=? AND author_user_id=?",id,user.getId());
        if(rows.isEmpty())throw new BusinessException(404,"修改建议不存在");return rows.get(0);
    }
    @PostMapping("/{id}/approve") @Transactional
    public void approve(@PathVariable long id,@Valid @RequestBody Decision decision,@AuthenticationPrincipal AuthenticatedUser user){
        admin(user);
        var rows=jdbc.queryForList("SELECT problem_id,base_fingerprint,proposed_content,status FROM problem_change_request WHERE id=? FOR UPDATE",id);
        if(rows.isEmpty())throw new BusinessException(404,"修改建议不存在");
        var row=rows.get(0); if(!"PENDING".equals(row.get("status")))throw new BusinessException(409,"该建议已审核");
        long problemId=((Number)row.get("problem_id")).longValue();
        var locks=jdbc.queryForList("SELECT id FROM problem WHERE id=? AND status=1 FOR UPDATE",Long.class,problemId);
        if(locks.isEmpty())throw new BusinessException(409,"题目已停用或不存在");
        Problem p=problems.selectById(problemId);
        if(!fingerprint(p).equals(row.get("base_fingerprint")))throw new BusinessException(409,"题目已更新，请重新提交修改建议");
        command.updateProblem(problemId,json.readValue((String)row.get("proposed_content"),UpdateProblemRequest.class),user.getId(),true);
        jdbc.update("UPDATE problem_change_request SET status='APPROVED',reviewer_user_id=?,review_time=NOW(),review_note=? WHERE id=?",user.getId(),decision.note(),id);
    }
    @PostMapping("/{id}/reject") @Transactional
    public void reject(@PathVariable long id,@Valid @RequestBody Decision decision,@AuthenticationPrincipal AuthenticatedUser user){
        admin(user);
        if(jdbc.update("UPDATE problem_change_request SET status='REJECTED',reviewer_user_id=?,review_time=NOW(),review_note=? WHERE id=? AND status='PENDING'",user.getId(),decision.note(),id)!=1)throw new BusinessException(409,"修改建议不存在或已审核");
    }
}
