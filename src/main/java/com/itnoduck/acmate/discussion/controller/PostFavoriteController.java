package com.itnoduck.acmate.discussion.controller;
import com.itnoduck.acmate.discussion.service.PostFavoriteService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/posts/{id}/favorite")
public class PostFavoriteController {
    private final PostFavoriteService favorites;
    public PostFavoriteController(PostFavoriteService favorites) {this.favorites=favorites;}
    @GetMapping public Map<String,Object> state(@PathVariable long id,@AuthenticationPrincipal AuthenticatedUser user) {return favorites.state(id,user.getId());}
    @PutMapping public Map<String,Object> favorite(@PathVariable long id,@AuthenticationPrincipal AuthenticatedUser user) {return favorites.set(id,user.getId(),true);}
    @DeleteMapping public Map<String,Object> unfavorite(@PathVariable long id,@AuthenticationPrincipal AuthenticatedUser user) {return favorites.set(id,user.getId(),false);}
}
