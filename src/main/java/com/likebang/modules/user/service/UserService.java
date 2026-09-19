package com.likebang.modules.user.service;

import com.likebang.modules.user.dto.request.LoginRequest;
import com.likebang.modules.user.dto.request.RegisterRequest;
import com.likebang.modules.user.dto.response.LoginResponse;
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
     * 根据ID查询用户，不存在抛业务异常
     */
    SysUser getExistingById(Long id);

    /**
     * 查询当前登录用户信息
     */
    UserInfoResponse currentUser(Long userId);
}
