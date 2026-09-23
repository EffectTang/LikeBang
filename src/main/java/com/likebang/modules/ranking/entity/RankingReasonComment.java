package com.likebang.modules.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 理由评论表 实体类
 * 评论挂在理由之下（楼中楼），ranking_id/item_id 全链路冗余，风格同 ranking_reason_vote
 */
@Data
@TableName("ranking_reason_comment")
public class RankingReasonComment implements Serializable {

    /**
     * 评论ID，Snowflake生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 排名ID（冗余，鉴权/级联自包含）
     */
    private Long rankingId;

    /**
     * 排名项ID（冗余）
     */
    private Long itemId;

    /**
     * 所属理由ID
     */
    private Long reasonId;

    /**
     * 评论者ID
     */
    private Long creatorId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 点赞数（预留列，评论点赞二期随 vote 表一并实现）
     */
    private Long likeCount;

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
