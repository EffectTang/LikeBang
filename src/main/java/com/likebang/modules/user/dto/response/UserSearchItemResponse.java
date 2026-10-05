package com.likebang.modules.user.dto.response;

import lombok.Data;

/**
 * 用户搜索结果是响应（脱敏投影）。
 * <p>
 * 面向全站"按昵称找人"场景，只投影可公开展示的字段：
 * 有意<b>不含</b> email / phone / username / role / status / openid / passwordHash——
 * 敏感信息在此 DTO 中不存在（结构性脱敏），杜绝经搜索接口批量泄露用户隐私。
 * 只按昵称匹配、只返回有效账号（未删除、未禁用），口径与 {@link PublicProfileResponse} 一致。
 */
@Data
public class UserSearchItemResponse {

    private Long id;

    private String nickname;

    private String avatarUrl;

    /**
     * 自我介绍（可空）
     */
    private String intro;
}
