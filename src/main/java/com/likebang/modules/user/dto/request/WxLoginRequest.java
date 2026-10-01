package com.likebang.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 微信小程序登录请求
 * <p>
 * 前端调用 wx.login 拿到临时凭证 code，随可选的昵称/头像一并提交；
 * 服务端凭 code 调用 jscode2session 换取 openid，完成登录或静默注册。
 */
@Data
public class WxLoginRequest {

    /**
     * wx.login 返回的临时登录凭证，服务端据此换取 openid（一次性、5分钟有效）
     */
    @NotBlank(message = "code不能为空")
    private String code;

    /**
     * 可选：微信昵称，仅在首次登录需静默注册新用户时用于初始化展示名
     */
    @Size(max = 32, message = "昵称长度不能超过32")
    private String nickname;

    /**
     * 可选：微信头像URL，仅在首次登录需静默注册新用户时用于初始化头像
     */
    @Size(max = 512, message = "头像地址长度不能超过512")
    private String avatarUrl;
}
