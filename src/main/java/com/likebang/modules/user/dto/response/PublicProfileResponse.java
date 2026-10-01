package com.likebang.modules.user.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 他人主页公开资料响应（脱敏投影）
 * <p>
 * 面向社区"他人主页"场景，只投影可公开展示的字段：
 * 有意<b>不含</b> email / phone / username / role / status / openid / passwordHash——
 * 敏感信息在此 DTO 中不存在（结构性脱敏，而非运行时置空），杜绝经他人主页接口泄露隐私或权限信息。
 * 前端"TA的公开榜单"数量复用榜单分页返回的 total，无需在此冗余统计。
 */
@Data
public class PublicProfileResponse {

    private Long id;

    private String nickname;

    private String avatarUrl;

    /**
     * 自我介绍（可空）
     */
    private String intro;

    /**
     * 加入时间
     */
    private LocalDateTime createdAt;
}
