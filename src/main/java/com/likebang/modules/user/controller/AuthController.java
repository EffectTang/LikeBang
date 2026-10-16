package com.likebang.modules.user.controller;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.likebang.common.result.Result;
import com.likebang.common.auth.RequireRole;
import com.likebang.common.auth.UserRole;
import com.likebang.common.utils.UserContext;
import com.likebang.modules.user.dto.request.LoginRequest;
import com.likebang.modules.user.dto.request.PasswordResetCompleteRequest;
import com.likebang.modules.user.dto.request.PasswordResetRequest;
import com.likebang.modules.user.dto.request.ProfileUpdateRequest;
import com.likebang.modules.user.dto.request.RegisterRequest;
import com.likebang.modules.user.dto.request.WxLoginRequest;
import com.likebang.modules.user.dto.response.LoginResponse;
import com.likebang.modules.user.dto.response.UserInfoResponse;
import com.likebang.modules.user.service.PasswordResetService;
import com.likebang.modules.user.service.UserService;
import com.likebang.modules.user.service.WxAccessTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证 接口
 */
@RestController
@RequestMapping("/user/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final WxAccessTokenService wxAccessTokenService;
    private final PasswordResetService passwordResetService;

    /**
     * 注册（成功后直接返回登录态）
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    /**
     * 找回密码（免登录）：向登记邮箱发送含一次性重置链接的邮件。
     * 无论邮箱是否存在均返回成功（防账号探测）；来源 IP 取自代理头，供服务层频控与审计
     */
    @PostMapping("/password-reset/request")
    public Result<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request,
                                             HttpServletRequest servletRequest) {
        passwordResetService.requestReset(request, JakartaServletUtil.getClientIP(servletRequest));
        return Result.success(null);
    }

    /**
     * 完成密码重置（免登录）：凭邮件链接中的一次性令牌设置新密码，成功后旧链接即刻失效
     */
    @PostMapping("/password-reset/complete")
    public Result<Void> completePasswordReset(@Valid @RequestBody PasswordResetCompleteRequest request) {
        passwordResetService.completeReset(request);
        return Result.success(null);
    }

    /**
     * 微信小程序登录（成功后返回与账号密码一致的登录态）
     */
    @PostMapping("/wx-login")
    public Result<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return Result.success(userService.wxLogin(request));
    }

    /**
     * 当前登录用户信息（需携带 Token）
     */
    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        return Result.success(userService.currentUser(UserContext.requireUserId()));
    }

    /**
     * 自助修改个人资料（昵称/头像/自我介绍），返回最新信息供前端刷新缓存。
     * 仅能修改本人：userId 取自登录态，忽略任何前端传入的身份标识
     */
    @PutMapping("/profile")
    public Result<UserInfoResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.success(userService.updateProfile(UserContext.requireUserId(), request));
    }

    /**
     * 【临时自测入口·扫码登录第 1 步】校验微信 access_token 能否获取（验证 IP 白名单/appid/secret）。
     * 仅管理员可调；只返回 token 长度+前缀预览，不回显完整值；失败则直接抛出微信 errcode/errmsg 便于排障。
     * 全链路联调稳定后可移除此入口。
     */
    @RequireRole(UserRole.ADMIN)
    @GetMapping("/wx-token-check")
    public Result<String> wxTokenCheck() {
        return Result.success(wxAccessTokenService.maskedPreview());
    }
}
