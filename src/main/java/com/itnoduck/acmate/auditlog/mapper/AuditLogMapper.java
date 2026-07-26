package com.itnoduck.acmate.auditlog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itnoduck.acmate.auditlog.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
