package com.likebang.modules.user.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功响应：JWT + 用户信息
 */
@Data
@NoArgsConstructor
public class LoginResponse {

    private String token;

    private UserInfoResponse userInfo;

    /**
     * 仅微信登录场景：本次是否为新注册用户。前端据此只在“新用户首次登录”弹一次
     * “完善资料”引导（可跳过），老用户一律不弹。账号密码/扫码登录恒为 false。
     */
    private boolean newUser;

    public LoginResponse(String token, UserInfoResponse userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }
}
