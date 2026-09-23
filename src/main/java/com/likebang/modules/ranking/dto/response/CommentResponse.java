package com.likebang.modules.ranking.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 理由评论
 */
@Data
public class CommentResponse {

    private Long id;

    private Long reasonId;

    private String content;

    /**
     * 点赞数（二期评论点赞上线前恒为 0）
     */
    private Long likeCount;

    private Long creatorId;

    private String creatorNickname;

    private LocalDateTime createdAt;
}
