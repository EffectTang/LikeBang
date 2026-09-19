package com.likebang.modules.ranking.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 榜单详情响应：榜单信息 + 排名项 + 理由
 */
@Data
public class RankingDetailResponse {

    private Long id;

    private String title;

    private String description;

    private Long categoryId;

    private String categoryName;

    private Integer itemLimit;

    private Integer itemCount;

    private Long participantCount;

    private Long viewCount;

    private Integer status;

    private Integer visibility;

    private Long creatorId;

    private String creatorNickname;

    private LocalDateTime createdAt;

    private List<RankingItemResponse> items;

    @Data
    public static class RankingItemResponse {

        private Long id;

        private String name;

        private String description;

        private String imageUrl;

        private Integer currentRank;

        private BigDecimal score;

        private Long agreeCount;

        private Long opposeCount;

        private Long participantCount;

        private BigDecimal agreeRate;

        private Integer reasonCount;

        private Long creatorId;

        private String creatorNickname;

        private LocalDateTime createdAt;

        private List<RankingReasonResponse> reasons;
    }

    @Data
    public static class RankingReasonResponse {

        private Long id;

        private String content;

        private Long agreeCount;

        private Long opposeCount;

        private BigDecimal agreeRate;

        private Integer currentRank;

        private Long creatorId;

        private String creatorNickname;

        private LocalDateTime createdAt;
    }
}
