package com.itnoduck.acmate.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目实体，映射 problem 表。
 *
 * <p>tags 当前存储为逗号分隔字符串（VARCHAR(255)），暂不拆分为关联表或 JSON 字段。</p>
 */
@Data
@TableName("problem")
public class Problem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String platform;
    private String externalProblemKey;
    private String title;
    private String sourceUrl;
    private String difficulty;
    private String tags;
    private String contentMd;
    private Long creatorUserId;
    private Integer status;
    private String deactivationSource;
    private String deactivationReason;
    private Long deactivatedBy;
    private LocalDateTime deactivationTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
