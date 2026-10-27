package com.itnoduck.acmate.oj.controller;
import com.itnoduck.acmate.oj.service.NowcoderTrainingService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/nowcoder")
public class NowcoderTrainingController {
    private final NowcoderTrainingService service;
    public NowcoderTrainingController(NowcoderTrainingService service){this.service=service;}
    public record Binding(@NotBlank @Pattern(regexp="[1-9][0-9]{0,11}") String uid){}
    @GetMapping("/account") public NowcoderTrainingService.Account account(@AuthenticationPrincipal AuthenticatedUser user){return service.account(user.getId());}
    @PostMapping("/account") public void bind(@Valid @RequestBody Binding body,@AuthenticationPrincipal AuthenticatedUser user){service.bind(user.getId(),body.uid());}
    @DeleteMapping("/account") public void unbind(@AuthenticationPrincipal AuthenticatedUser user){service.unbind(user.getId());}
    @GetMapping("/problems/{id}") public NowcoderTrainingService.Training info(@PathVariable long id){return service.training(id);}
    @GetMapping("/problems/{id}/submissions") public List<Map<String,Object>> records(@PathVariable long id,@AuthenticationPrincipal AuthenticatedUser user){return service.records(id,user.getId());}
    @PostMapping("/problems/{id}/sync") public NowcoderTrainingService.Sync sync(@PathVariable long id,@AuthenticationPrincipal AuthenticatedUser user){return service.sync(id,user.getId());}
}
