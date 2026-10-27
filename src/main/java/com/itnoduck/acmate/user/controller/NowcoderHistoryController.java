package com.itnoduck.acmate.user.controller;
import com.itnoduck.acmate.user.service.NowcoderHistoryService;
import com.itnoduck.acmate.user.service.UserActivityService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/users/{id}/nowcoder-history")
public class NowcoderHistoryController {
 private final NowcoderHistoryService history;
 public NowcoderHistoryController(NowcoderHistoryService history){this.history=history;}
 private boolean self(long id,AuthenticatedUser viewer){return viewer!=null && viewer.getId().longValue()==id;}
 private void requireVisible(long id,String uid,AuthenticatedUser viewer){if(!self(id,viewer)&&!uid.equals(history.currentUid(id)))throw new BusinessException(404,"牛客账号不存在");}
 @GetMapping public List<NowcoderHistoryService.Account> accounts(@PathVariable long id,@AuthenticationPrincipal AuthenticatedUser viewer){
  if(self(id,viewer))return history.accounts(id);
  String current=history.currentUid(id);return current==null?List.of():history.accounts(id).stream().filter(a->a.uid().equals(current)).toList();
 }
 @GetMapping("/{uid}/activity") public UserActivityService.Activity activity(@PathVariable long id,@PathVariable String uid,@AuthenticationPrincipal AuthenticatedUser viewer){requireVisible(id,uid,viewer);return history.activity(id,uid);}
 @GetMapping("/{uid}/recent") public List<UserActivityService.Submission> recent(@PathVariable long id,@PathVariable String uid,@AuthenticationPrincipal AuthenticatedUser viewer){requireVisible(id,uid,viewer);return history.recent(id,uid);}
 @GetMapping("/{uid}/overview") public UserActivityService.Overview overview(@PathVariable long id,@PathVariable String uid,@AuthenticationPrincipal AuthenticatedUser viewer){requireVisible(id,uid,viewer);return history.overview(id,uid);}
}
