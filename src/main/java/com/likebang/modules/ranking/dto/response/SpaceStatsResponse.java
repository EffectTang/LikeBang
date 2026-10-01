package com.likebang.modules.ranking.dto.response;

import lombok.Data;

/**
 * 「我的空间」数据概览响应（仅本人可见的活动统计）
 * <p>
 * 三项均为只读聚合，来自既有表：ranking / ranking_item / ranking_reason / ranking_item_vote。
 * 注意：字段为 Long，经全局 Long→String 序列化后前端拿的是字符串，渲染/运算前须 Number() 归一化。
 */
@Data
public class SpaceStatsResponse {

    /** 我发布的榜单数（排除已删除） */
    private Long rankingCount;

    /** 我创建的排名项/理由收到的他人票数 */
    private Long receivedVoteCount;

    /** 我参与投票的榜单数（按 ranking_item_vote 去重 ranking_id） */
    private Long joinedRankingCount;
}
