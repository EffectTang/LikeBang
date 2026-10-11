package com.likebang.modules.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 榜单收藏表 实体类
 * <p>
 * 「用户 × 榜单」二元关系，一人对同一榜单至多一条（uk_ranking_user 唯一键保证）。
 */
@Data
@TableName("ranking_bookmark")
public class RankingBookmark implements Serializable {

    /**
     * 收藏ID，Snowflake生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 排名ID
     */
    private Long rankingId;

    /**
     * 收藏者ID
     */
    private Long userId;

    /**
     * 创建时间（即收藏时间，「我的收藏」按其倒序）
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
