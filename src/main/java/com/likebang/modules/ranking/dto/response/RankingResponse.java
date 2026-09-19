package com.likebang.modules.ranking.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 榜单列表项响应（社区浏览用）
 */
@Data
public class RankingResponse {

    private Long id;

    private String title;

    private String description;

    private String coverUrl;

    private Long categoryId;

    private String categoryName;

    private Integer itemLimit;

    private Integer itemCount;

    private Long participantCount;

    private Long agreeCount;

    private Long viewCount;

    private Integer status;

    private Integer visibility;

    private Long creatorId;

    private String creatorNickname;

    private LocalDateTime createdAt;
}
