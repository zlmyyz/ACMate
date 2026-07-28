package com.itnoduck.acmate.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itnoduck.acmate.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("SELECT COUNT(*) FROM notification WHERE recipient_user_id = #{userId} AND is_read = 0")
    long countUnread(@Param("userId") Long userId);

    @Update("UPDATE notification SET is_read = 1, read_time = NOW() WHERE recipient_user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Long userId);
}
