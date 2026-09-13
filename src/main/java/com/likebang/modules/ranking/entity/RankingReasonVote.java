package com.likebang.modules.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 理由投票记录表 实体类
 */
@Data
@TableName("ranking_reason_vote")
public class RankingReasonVote implements Serializable {

    /**
     * 投票记录ID，Snowflake生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 排名ID
     */
    private Long rankingId;

    /**
     * 排名项ID
     */
    private Long itemId;

    /**
     * 理由ID
     */
    private Long reasonId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 投票类型：1认同，-1反对
     */
    private Integer voteType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
