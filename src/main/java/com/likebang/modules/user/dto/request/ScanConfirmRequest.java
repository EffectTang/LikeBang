package com.likebang.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 扫码登录确认请求：小程序端（已登录）拿到 Web 二维码中的 scene 后回传，
 * 以当前登录用户身份为该 sceneId 签发 Web 登录态。
 */
@Data
public class ScanConfirmRequest {

    @NotBlank(message = "sceneId 不能为空")
    private String sceneId;
}
