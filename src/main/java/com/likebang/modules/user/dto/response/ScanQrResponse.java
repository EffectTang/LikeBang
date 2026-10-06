package com.likebang.modules.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 扫码登录二维码响应：Web 拿到后展示二维码，并用 sceneId 轮询状态。
 */
@Data
@AllArgsConstructor
public class ScanQrResponse {

    /** 一次性会话标识，前端此后轮询/回传用 */
    private String sceneId;

    /** 小程序码图片，data:image/png;base64,... 可直接放进 img src */
    private String qrBase64;

    /** 二维码有效期（秒），到期前端应重新获取 */
    private long expiresIn;
}
