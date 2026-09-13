package com.likebang.modules.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 排名项表 实体类
 */
@Data
@TableName("ranking_item")
public class RankingItem implements Serializable {

    /**
     * 排名项ID，Snowflake生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 排名ID
     */
    private Long rankingId;

    /**
     * 排名项创建者ID
     */
    private Long creatorId;

    /**
     * 排名项名称
     */
    private String name;

    /**
     * 排名项描述
     */
    private String description;

    /**
     * 排名项图片
     */
    private String imageUrl;

    /**
     * 当前排名
     */
    private Integer currentRank;

    /**
     * 当前综合得分
     */
    private BigDecimal score;

    /**
     * 认同数
     */
    private Long agreeCount;

    /**
     * 反对数
     */
    private Long opposeCount;

    /**
     * 参与人数
     */
    private Long participantCount;

    /**
     * 认同率
     */
    private BigDecimal agreeRate;

    /**
     * 理由数量
     */
    private Integer reasonCount;

    /**
     * 状态：0删除，1正常，2隐藏
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
