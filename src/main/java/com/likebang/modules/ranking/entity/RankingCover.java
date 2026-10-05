package com.likebang.modules.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 榜单封面多图表 实体类
 * <p>
 * 延续“专表 + 冗余”原则：不在 ranking.cover_url 单列上塞多图列表；
 * cover_url 仍保留且恒等于第 1 张封面（列表卡片单图视角不变），本表存全量供详情页轮播。
 */
@Data
@TableName("ranking_cover")
public class RankingCover implements Serializable {

    /**
     * 封面ID，Snowflake生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 排名ID
     */
    private Long rankingId;

    /**
     * 封面图地址（/uploads/ 相对路径）
     */
    private String imageUrl;

    /**
     * 展示顺序，越小越靠前；第 1 张同步写回 ranking.cover_url
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
