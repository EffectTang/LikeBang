package com.likebang.modules.user.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.likebang.common.dto.PageParam;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.auth.UserRole;
import com.likebang.common.result.ResultCode;
import com.likebang.config.auth.AuthProperties;
import com.likebang.modules.user.dto.request.LoginRequest;
import com.likebang.modules.user.dto.request.ProfileUpdateRequest;
import com.likebang.modules.user.dto.request.RegisterRequest;
import com.likebang.modules.user.dto.request.WxLoginRequest;
import com.likebang.modules.user.dto.response.LoginResponse;
import com.likebang.modules.user.dto.response.PublicProfileResponse;
import com.likebang.modules.user.dto.response.UserSearchItemResponse;
import com.likebang.modules.user.dto.response.UserInfoResponse;
import com.likebang.modules.user.entity.SysUser;
import com.likebang.modules.user.mapper.SysUserMapper;
import com.likebang.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
        // 注册入口一律为普通用户，管理员只能由内置超管/后台提升产生
        user.setRole(UserRole.USER.getCode());
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

    /**
     * 微信小程序登录。
     * <p>
     * 刻意不加 @Transactional：换取 openid 需外部 HTTP 调用，避免长时间占用数据库连接；
     * 落库仅为单条 insert，依赖 openid 唯一键 + 冲突回查保证并发首登幂等。
     */
    @Override
    public LoginResponse wxLogin(WxLoginRequest request) {
        AuthProperties.Wx wx = authProperties.getWx();
        if (wx == null || StrUtil.isBlank(wx.getAppId()) || StrUtil.isBlank(wx.getAppSecret())) {
            throw new BusinessException(ResultCode.WX_CONFIG_MISSING);
        }
        String openid = fetchOpenid(request.getCode(), wx.getAppId(), wx.getAppSecret());

        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getOpenid, openid));
        if (user == null) {
            user = registerByWechat(openid, request);
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

    /**
     * 为已通过身份校验的用户签发登录态：供扫码确认等内部流程复用，
     * 与账号密码/微信登录一致地校验有效并更新最后登录时间。
     */
    @Override
    public LoginResponse issueLoginToken(Long userId) {
        SysUser user = getExistingById(userId);
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateById(update);
        user.setLastLoginAt(update.getLastLoginAt());
        return buildLoginResponse(user);
    }

    /**
     * 调用微信 jscode2session 用 code 换取 openid（失败即抛业务异常，绝不泄露 appSecret）
     */
    private String fetchOpenid(String code, String appId, String appSecret) {
        String url = StrUtil.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid={}&secret={}&js_code={}&grant_type=authorization_code",
                appId, appSecret, code);
        String body;
        try {
            body = HttpUtil.get(url, 5000);
        } catch (Exception e) {
            // 仅记录异常信息，URL 含 appSecret 不可入日志
            log.warn("调用微信 jscode2session 网络异常: {}", e.getMessage());
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED);
        }
        JSONObject json = JSONUtil.parseObj(body);
        int errcode = json.getInt("errcode", 0);
        if (errcode != 0) {
            log.warn("微信 jscode2session 返回错误: errcode={}, errmsg={}", errcode, json.getStr("errmsg"));
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED.getCode(),
                    "微信登录失败：" + json.getStr("errmsg"));
        }
        String openid = json.getStr("openid");
        if (StrUtil.isBlank(openid)) {
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED);
        }
        return openid;
    }

    /**
     * 微信首登静默注册：username 由 openid 派生（保证唯一），email 留空，
     * password_hash 置随机值以禁用密码登录（此类账号仅能走微信登录）
     */
    private SysUser registerByWechat(String openid, WxLoginRequest request) {
        SysUser user = new SysUser();
        user.setOpenid(openid);
        user.setUsername("wx_" + StrUtil.subPre(DigestUtil.md5Hex(openid), 24));
        user.setNickname(StrUtil.isBlank(request.getNickname())
                ? "微信用户" : StrUtil.subPre(request.getNickname().trim(), 32));
        user.setAvatarUrl(blankToNull(request.getAvatarUrl()));
        user.setPasswordHash(BCrypt.hashpw(UUID.randomUUID().toString()));
        user.setStatus(1);
        // 微信注册入口一律为普通用户，管理员只能由内置超管/后台提升产生
        user.setRole(UserRole.USER.getCode());
        try {
            sysUserMapper.insert(user);
            log.info("微信新用户静默注册成功: id={}, openid={}", user.getId(), openid);
        } catch (DuplicateKeyException e) {
            // 并发首登：另一请求已插入同 openid 用户，回查复用，避免唯一键冲突上抛
            SysUser existing = sysUserMapper.selectOne(
                    Wrappers.<SysUser>lambdaQuery().eq(SysUser::getOpenid, openid));
            if (existing == null) {
                throw e;
            }
            return existing;
        }
        return user;
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
     * 他人公开资料（脱敏投影）：只取可展示字段，禁用/删除账号一律按"不存在"处理，
     * 避免经主页接口探测账号状态或泄露 email/phone/role 等敏感信息
     */
    @Override
    public PublicProfileResponse publicProfile(Long targetUserId) {
        SysUser user = getExistingById(targetUserId);
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        PublicProfileResponse response = new PublicProfileResponse();
        response.setId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setIntro(user.getIntro());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    /**
     * 按昵称分页搜索用户：只取脱敏投影字段，仅命中有效账号（@TableLogic 自动排除软删 + status=1 排除禁用）。
     * 空关键词直接返回空页，不“无关键词=全量用户”，避免脱敏列表被批量拖取。
     */
    @Override
    public IPage<UserSearchItemResponse> pageSearchPublic(PageParam pageParam) {
        String keyword = StrUtil.trimToNull(pageParam.getKeyword());
        if (keyword == null) {
            return pageParam.toPage();
        }
        Page<SysUser> page = sysUserMapper.selectPage(pageParam.toPage(),
                Wrappers.<SysUser>lambdaQuery()
                        .select(SysUser::getId, SysUser::getNickname,
                                SysUser::getAvatarUrl, SysUser::getIntro)
                        .eq(SysUser::getStatus, 1)
                        .like(SysUser::getNickname, keyword)
                        .orderByDesc(SysUser::getId));
        return page.convert(u -> {
            UserSearchItemResponse item = new UserSearchItemResponse();
            item.setId(u.getId());
            item.setNickname(u.getNickname());
            item.setAvatarUrl(u.getAvatarUrl());
            item.setIntro(u.getIntro());
            return item;
        });
    }

    /**
     * 自助修改个人资料。
     * <p>
     * 局部更新用 lambdaUpdate().set 显式写回目标列（updateById 会忽略 null 导致"清空"不生效）：
     * 头像/简介传空即落 null 完成清空；昵称 NOT NULL，空白按"不修改"处理。
     * 无实际变更时短路返回，不发多余 UPDATE。
     */
    @Override
    public UserInfoResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        SysUser current = getExistingById(userId);

        // 昵称不可清空：null/空白 = 不修改
        String nickname = StrUtil.isNotBlank(request.getNickname())
                ? StrUtil.subPre(request.getNickname().trim(), 32) : null;
        boolean touchNickname = nickname != null && !nickname.equals(current.getNickname());
        // 头像/简介：null = 不修改；非 null 时按空/非空决定清空或写入
        boolean touchAvatar = request.getAvatarUrl() != null;
        boolean touchIntro = request.getIntro() != null;
        String avatarUrl = touchAvatar ? normalizeAvatarUrl(request.getAvatarUrl()) : null;
        String intro = touchIntro ? blankToNull(request.getIntro()) : null;

        if (!touchNickname && !touchAvatar && !touchIntro) {
            return UserInfoResponse.from(current);
        }

        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, userId)
                .set(touchNickname, SysUser::getNickname, nickname)
                .set(touchAvatar, SysUser::getAvatarUrl, avatarUrl)
                .set(touchIntro, SysUser::getIntro, intro));

        log.info("用户更新个人资料: userId={}, nickname={}, avatar={}, intro={}",
                userId, touchNickname, touchAvatar, touchIntro);
        return UserInfoResponse.from(getExistingById(userId));
    }

    /**
     * 头像地址校验：空=清空(null)；仅接受站内 /uploads/ 相对路径或 http(s) 外链，
     * 拒绝 javascript:/data: 等危险协议，防经 <img src> 触发存储型 XSS
     */
    private String normalizeAvatarUrl(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String value = raw.trim();
        if (value.startsWith("/uploads/") || value.startsWith("https://") || value.startsWith("http://")) {
            return value;
        }
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "头像地址格式不合法");
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
