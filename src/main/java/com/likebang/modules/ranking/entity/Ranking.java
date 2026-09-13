package com.likebang.modules.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 排名表 实体类
 */
@Data
@TableName("ranking")
public class Ranking implements Serializable {

    /**
     * 排名ID，Snowflake生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 排名标题
     */
    private String title;

    /**
     * 排名描述
     */
    private String description;

    /**
     * 封面地址
     */
    private String coverUrl;

    /**
     * 最大排名项数量
     */
    private Integer itemLimit;

    /**
     * 当前排名项数量
     */
    private Integer itemCount;

    /**
     * 参与人数
     */
    private Long participantCount;

    /**
     * 累计认同数
     */
    private Long agreeCount;

    /**
     * 浏览次数
     */
    private Long viewCount;

    /**
     * 状态：0草稿，1正常，2下架，3删除
     */
    private Integer status;

    /**
     * 可见性：0私有，1公开，2仅链接可见
     */
    private Integer visibility;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
