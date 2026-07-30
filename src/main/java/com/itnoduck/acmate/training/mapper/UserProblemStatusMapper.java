package com.itnoduck.acmate.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itnoduck.acmate.training.entity.UserProblemStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserProblemStatusMapper extends BaseMapper<UserProblemStatus> {

    void upsertFirstAc(@Param("userId") Long userId,
                       @Param("problemId") Long problemId,
                       @Param("firstAcTime") LocalDateTime firstAcTime,
                       @Param("submissionId") Long submissionId,
                       @Param("solveSource") String solveSource);

    int updateStatusAtomic(@Param("userId") Long userId,
                           @Param("problemId") Long problemId,
                           @Param("newStatus") Integer newStatus);

    void upsertNote(@Param("userId") Long userId,
                    @Param("problemId") Long problemId,
                    @Param("note") String note);

    void batchBackfillFromFirstAc(@Param("platform") String platform,
                                  @Param("externalProblemKey") String externalProblemKey,
                                  @Param("problemId") Long problemId);
}
