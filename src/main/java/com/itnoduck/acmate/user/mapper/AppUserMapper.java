package com.itnoduck.acmate.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itnoduck.acmate.user.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
    @org.apache.ibatis.annotations.Select("SELECT CONCAT('https://avatars.githubusercontent.com/u/', identifier, '?v=4') FROM user_login_identity WHERE provider='GITHUB' AND user_id=#{userId}")
    String githubAvatarUrl(long userId);
    @org.apache.ibatis.annotations.Select("SELECT login_name FROM user_login_identity WHERE provider='GITHUB' AND user_id=#{userId}")
    String githubLogin(long userId);
    @org.apache.ibatis.annotations.Select("SELECT display_name FROM user_login_identity WHERE provider='GITHUB' AND user_id=#{userId}")
    String githubName(long userId);
    @org.apache.ibatis.annotations.Select("SELECT u.* FROM app_user u JOIN user_login_identity i ON i.user_id=u.id WHERE i.provider='EMAIL' AND i.identifier=#{email}")
    AppUser findByLoginEmail(String email);
    @org.apache.ibatis.annotations.Insert("INSERT INTO user_login_identity(user_id,provider,identifier) VALUES(#{userId},'EMAIL',#{email})")
    int insertLoginEmail(long userId, String email);
    @org.apache.ibatis.annotations.Update("UPDATE user_login_identity SET identifier=#{email} WHERE user_id=#{userId} AND provider='EMAIL'")
    int updateLoginEmail(long userId, String email);
    default void bindLoginEmail(long userId, String email) {
        if (updateLoginEmail(userId, email) == 0) insertLoginEmail(userId, email);
    }
}

