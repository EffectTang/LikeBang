package com.likebang.modules.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功响应：JWT + 用户信息
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private UserInfoResponse userInfo;
}
