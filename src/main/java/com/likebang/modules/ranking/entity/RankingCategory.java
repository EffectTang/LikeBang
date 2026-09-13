package com.likebang.modules.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 排名分类表 实体类
 */
@Data
@TableName("ranking_category")
public class RankingCategory implements Serializable {

    /**
     * 分类ID，Snowflake生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 分类图标
     */
    private String iconUrl;

    /**
     * 排序值，越小越靠前
     */
    private Integer sort;

    /**
     * 状态：0禁用，1正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
