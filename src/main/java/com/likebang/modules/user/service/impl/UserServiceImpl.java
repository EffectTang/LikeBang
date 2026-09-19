package com.likebang.modules.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.config.auth.AuthProperties;
import com.likebang.modules.user.dto.request.LoginRequest;
import com.likebang.modules.user.dto.request.RegisterRequest;
import com.likebang.modules.user.dto.response.LoginResponse;
import com.likebang.modules.user.dto.response.UserInfoResponse;
import com.likebang.modules.user.entity.SysUser;
import com.likebang.modules.user.mapper.SysUserMapper;
import com.likebang.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现：注册 / 登录 / JWT 签发与校验
 * <p>
 * 密码使用 BCrypt 加盐哈希（自带随机盐），Token 使用 Hutool JWT（HS256）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String CLAIM_USER_ID = "userId";

    private final SysUserMapper sysUserMapper;
    private final AuthProperties authProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        // 用户名唯一性校验（逻辑删除记录也纳入，避免账号身份混乱）
        Long exists = sysUserMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }
        String email = request.getEmail().toLowerCase();
        Long emailExists = sysUserMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getEmail, email));
        if (emailExists != null && emailExists > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS.getCode(), "该邮箱已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setPasswordHash(BCrypt.hashpw(request.getPassword()));
        // 邮箱必填且已校验唯一，统一小写归一化存储，避免大小写重复账号
        user.setEmail(email);
        user.setPhone(blankToNull(request.getPhone()));
        user.setStatus(1);
        sysUserMapper.insert(user);

        log.info("新用户注册成功: id={}, username={}", user.getId(), user.getUsername());
        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, request.getUsername()));

        // 用户不存在与密码错误返回同一提示，避免用户名探测
        if (user == null || user.getPasswordHash() == null
                || !BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR.getCode(), "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 更新最后登录时间
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateById(update);
        user.setLastLoginAt(update.getLastLoginAt());

        return buildLoginResponse(user);
    }

    @Override
    public SysUser getExistingById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public UserInfoResponse currentUser(Long userId) {
        return UserInfoResponse.from(getExistingById(userId));
    }

    /**
     * 签发 JWT：payload 携带 userId，有效期由配置决定
     */
    private String createToken(Long userId) {
        long now = System.currentTimeMillis();
        Map<String, Object> payload = new HashMap<>();
        payload.put(CLAIM_USER_ID, userId);
        payload.put(JWT.ISSUED_AT, new Date(now));
        payload.put(JWT.EXPIRES_AT, new Date(now + authProperties.getJwtTimeout()));
        return JWTUtil.createToken(payload, secretKey());
    }

    /**
     * 解析并校验 Token，返回用户ID；非法/过期返回 null（由拦截器统一转 401）
     */
    public Long parseToken(String token) {
        try {
            if (!JWTUtil.verify(token, secretKey())) {
                return null;
            }
            JWTValidator.of(token).validateDate();
            Object userId = JWTUtil.parseToken(token).getPayload(CLAIM_USER_ID);
            return userId == null ? null : Long.valueOf(userId.toString());
        } catch (Exception e) {
            log.debug("Token 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private LoginResponse buildLoginResponse(SysUser user) {
        return new LoginResponse(createToken(user.getId()), UserInfoResponse.from(user));
    }

    private byte[] secretKey() {
        String secret = authProperties.getJwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("likebang.auth.jwt-secret 未配置或长度不足32字节");
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
