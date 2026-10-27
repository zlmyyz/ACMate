package com.itnoduck.acmate.oj.controller;
import com.itnoduck.acmate.oj.service.NowcoderProfileSyncService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nowcoder/profile-sync")
public class NowcoderProfileController {
    private final NowcoderProfileSyncService service;
    public NowcoderProfileController(NowcoderProfileSyncService service){this.service=service;}
    @GetMapping public NowcoderProfileSyncService.State state(@AuthenticationPrincipal AuthenticatedUser user){return service.state(user.getId());}
    @PostMapping public ResponseEntity<NowcoderProfileSyncService.State> sync(@AuthenticationPrincipal AuthenticatedUser user){return ResponseEntity.accepted().body(service.enqueue(user.getId()));}
    @PostMapping("/retry-imports") public void retry(@AuthenticationPrincipal AuthenticatedUser user){service.retryImports(user.getId());}
}
