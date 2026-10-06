package com.likebang.modules.user.controller;

import com.likebang.common.result.Result;
import com.likebang.common.utils.UserContext;
import com.likebang.modules.user.dto.request.ScanConfirmRequest;
import com.likebang.modules.user.dto.response.ScanQrResponse;
import com.likebang.modules.user.dto.response.ScanStatusResponse;
import com.likebang.modules.user.scanlogin.ScanLoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web 扫码登录（路线 2：复用已有小程序）接口。
 * <p>
 * 权限：{@code scan-qr}/{@code scan-status} 为公开（已在 {@code WebConfig} 白名单放行）；
 * {@code scan-confirm} 必须由<b>已登录的小程序用户</b>调用（走拦截器、不在白名单），
 * 这是整个信任链的锚点——只有持有效小程序 token 的请求才能把某个 sceneId 绑定到具体账号。
 */
@RestController
@RequestMapping("/user/auth")
@RequiredArgsConstructor
public class ScanLoginController {

    private final ScanLoginService scanLoginService;

    /**
     * 生成登录二维码：返回 sceneId + base64 小程序码 + 有效期，供 Web 展示并据此轮询。
     */
    @GetMapping("/scan-qr")
    public Result<ScanQrResponse> scanQr() {
        return Result.success(scanLoginService.createLoginQr());
    }

    /**
     * Web 轮询扫码状态；CONFIRMED 时一次性返回 token/userInfo 并即刻销毁会话。
     */
    @GetMapping("/scan-status")
    public Result<ScanStatusResponse> scanStatus(@RequestParam String sceneId) {
        return Result.success(scanLoginService.pollStatus(sceneId));
    }

    /**
     * 小程序端确认授权：以当前登录用户身份为该 sceneId 签发 Web 登录态。userId 取自登录态，忽略入参身份。
     */
    @PostMapping("/scan-confirm")
    public Result<Void> scanConfirm(@Valid @RequestBody ScanConfirmRequest request) {
        scanLoginService.confirm(request.getSceneId(), UserContext.requireUserId());
        return Result.success();
    }
}
