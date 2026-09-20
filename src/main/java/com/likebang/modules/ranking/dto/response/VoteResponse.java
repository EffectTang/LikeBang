package com.likebang.modules.ranking.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 投票操作响应：返回操作后的最新计数，前端据此局部更新，免整页刷新
 */
@Data
public class VoteResponse {

    /**
     * 当前用户投票态：1认同，-1反对，null未投（取消成功后为 null）
     */
    private Integer myVoteType;

    /**
     * 认同数
     */
    private Long agreeCount;

    /**
     * 反对数
     */
    private Long opposeCount;

    /**
     * 参与人数（对该对象投过票的 distinct 用户数）
     */
    private Long participantCount;

    /**
     * 认同率：认同数/参与人数，取值0~1
     */
    private BigDecimal agreeRate;
}
