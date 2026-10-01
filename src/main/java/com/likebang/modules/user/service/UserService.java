package com.likebang.modules.user.service;

import com.likebang.modules.user.dto.request.LoginRequest;
import com.likebang.modules.user.dto.request.ProfileUpdateRequest;
import com.likebang.modules.user.dto.request.RegisterRequest;
import com.likebang.modules.user.dto.request.WxLoginRequest;
import com.likebang.modules.user.dto.response.LoginResponse;
import com.likebang.modules.user.dto.response.PublicProfileResponse;
import com.likebang.modules.user.dto.response.UserInfoResponse;
import com.likebang.modules.user.entity.SysUser;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 注册并直接返回登录态
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 账号密码登录，签发 JWT
     */
    LoginResponse login(LoginRequest request);

    /**
     * 微信小程序登录：凭 wx.login 的 code 换取 openid，命中则登录、未命中则静默注册，
     * 签发与账号密码一致的 JWT
     */
    LoginResponse wxLogin(WxLoginRequest request);

    /**
     * 根据ID查询用户，不存在抛业务异常
     */
    SysUser getExistingById(Long id);

    /**
     * 查询当前登录用户信息
     */
    UserInfoResponse currentUser(Long userId);

    /**
     * 自助修改个人资料（昵称/头像/自我介绍），返回最新用户信息供前端刷新缓存
     */
    UserInfoResponse updateProfile(Long userId, ProfileUpdateRequest request);

    /**
     * 查询他人公开资料（脱敏投影，用于他人主页）：目标不存在/已删除/已禁用一律按"不存在"处理
     */
    PublicProfileResponse publicProfile(Long targetUserId);
}
