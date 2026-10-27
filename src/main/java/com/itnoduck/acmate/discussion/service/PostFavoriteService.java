package com.itnoduck.acmate.discussion.service;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
@Service
public class PostFavoriteService {
    private final JdbcTemplate jdbc;
    public PostFavoriteService(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public Map<String,Object> state(long post,long user) {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM post WHERE id=? AND status=1",Long.class,post)==0) throw new BusinessException(404,"帖子不存在或已停用");
        return Map.of("favorited",jdbc.queryForObject("SELECT COUNT(*) FROM post_favorite WHERE user_id=? AND post_id=?",Long.class,user,post)>0,
                "count",jdbc.queryForObject("SELECT COUNT(*) FROM post_favorite WHERE post_id=?",Long.class,post));
    }
    @Transactional public Map<String,Object> set(long post,long user,boolean favorite) {
        var rows=jdbc.queryForList("SELECT status FROM post WHERE id=? FOR UPDATE",post);
        if(rows.isEmpty()||favorite&&((Number)rows.get(0).get("status")).intValue()!=1) throw new BusinessException(404,"帖子不存在或已停用");
        if(favorite) jdbc.update("INSERT IGNORE INTO post_favorite(user_id,post_id) VALUES(?,?)",user,post);
        else jdbc.update("DELETE FROM post_favorite WHERE user_id=? AND post_id=?",user,post);
        return Map.of("favorited",favorite,"count",jdbc.queryForObject("SELECT COUNT(*) FROM post_favorite WHERE post_id=?",Long.class,post));
    }
}
