package com.itnoduck.acmate.user.service;

import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class UserSocialService {
    private final JdbcTemplate jdbc;
    public record Contributions(long views,long yesterdayViews,long likes,long yesterdayLikes,long favorites,long yesterdayFavorites) {}
    public record Summary(long followingCount,long followerCount,boolean following,String ipRegion,Contributions contributions) {}
    public record Member(long id,String username,String nickname,String avatarUrl,boolean following) {}
    public UserSocialService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public Summary summary(long userId,Long viewer) {
        requireUser(userId);
        String region=viewer != null && viewer == userId ? jdbc.queryForObject("SELECT last_login_region FROM app_user WHERE id=?",String.class,userId) : null;
        var yesterday=LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).minusDays(1);
        var today=yesterday.plusDays(1);
        return new Summary(count("SELECT COUNT(*) FROM user_follow WHERE follower_id=?",userId),
                count("SELECT COUNT(*) FROM user_follow WHERE followed_id=?",userId),
                viewer!=null&&count("SELECT COUNT(*) FROM user_follow WHERE follower_id=? AND followed_id=?",viewer,userId)>0,
                region,new Contributions(count("SELECT COALESCE(SUM(view_count),0) FROM post WHERE author_user_id=? AND status=1",userId),
                count("SELECT COALESCE(SUM(v.view_count),0) FROM post_daily_views v JOIN post p ON p.id=v.post_id WHERE p.author_user_id=? AND p.status=1 AND v.stat_date=?",userId,yesterday),
                count("SELECT COALESCE(SUM(like_count),0) FROM post WHERE author_user_id=? AND status=1",userId),
                count("SELECT COUNT(*) FROM post_like l JOIN post p ON p.id=l.post_id WHERE p.author_user_id=? AND p.status=1 AND l.create_time>=? AND l.create_time<?",userId,yesterday,today),
                count("SELECT COUNT(*) FROM post_favorite f JOIN post p ON p.id=f.post_id WHERE p.author_user_id=? AND p.status=1",userId),
                count("SELECT COUNT(*) FROM post_favorite f JOIN post p ON p.id=f.post_id WHERE p.author_user_id=? AND p.status=1 AND f.create_time>=? AND f.create_time<?",userId,yesterday,today)));
    }

    public PageResponse<Member> follows(long userId,boolean followers,Long viewer,int page,int size) {
        requireUser(userId);
        page=Math.max(1,page);size=Math.max(1,Math.min(size,50));
        String owner=followers?"followed_id":"follower_id",other=followers?"follower_id":"followed_id";
        long total=count("SELECT COUNT(*) FROM user_follow WHERE "+owner+"=?",userId);
        var rows=jdbc.query("SELECT u.id,u.username,u.nickname,COALESCE(NULLIF(u.avatar_url,''),CONCAT('https://avatars.githubusercontent.com/u/',g.identifier,'?v=4')) AS avatar_url,EXISTS(SELECT 1 FROM user_follow vf WHERE vf.follower_id=? AND vf.followed_id=u.id) AS following FROM user_follow f JOIN app_user u ON u.id=f."+other+" LEFT JOIN user_login_identity g ON g.user_id=u.id AND g.provider='GITHUB' WHERE f."+owner+"=? ORDER BY f.create_time DESC,u.id DESC LIMIT ? OFFSET ?",
                (rs,n)->new Member(rs.getLong("id"),rs.getString("username"),rs.getString("nickname"),rs.getString("avatar_url"),rs.getBoolean("following")),viewer==null?0L:viewer,userId,size,(long)(page-1)*size);
        return new PageResponse<>(page,size,total,(total+size-1)/size,rows);
    }

    @Transactional
    public void follow(long actor,long target,boolean follow) {
        if(actor==target) throw new BusinessException(400,"不能关注自己");
        var users=jdbc.queryForList("SELECT id,status FROM app_user WHERE id IN (?,?) ORDER BY id FOR UPDATE",actor,target);
        if(users.size()!=2) throw new BusinessException(404,"用户不存在");
        if(follow&&users.stream().anyMatch(u->((Number)u.get("status")).intValue()!=1)) throw new BusinessException(400,"账号已停用，无法关注");
        if(follow) jdbc.update("INSERT IGNORE INTO user_follow(follower_id,followed_id) VALUES(?,?)",actor,target);
        else jdbc.update("DELETE FROM user_follow WHERE follower_id=? AND followed_id=?",actor,target);
    }

    private void requireUser(long id) { if(count("SELECT COUNT(*) FROM app_user WHERE id=?",id)==0) throw new BusinessException(404,"用户不存在"); }
    private long count(String sql,Object... args) { return jdbc.queryForObject(sql,Long.class,args); }
}
