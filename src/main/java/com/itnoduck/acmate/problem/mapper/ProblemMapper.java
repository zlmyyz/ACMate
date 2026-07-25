package com.itnoduck.acmate.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itnoduck.acmate.problem.entity.Problem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProblemMapper extends BaseMapper<Problem> {
}
