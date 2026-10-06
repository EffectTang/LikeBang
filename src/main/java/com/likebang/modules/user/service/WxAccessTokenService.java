package com.likebang.modules.user.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.config.auth.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 微信小程序服务端 access_token 管理。
 * <p>
 * access_token 是调用 {@code wxacode.getUnlimited}（生成小程序码）等 cgi-bin 接口的全局凭据：
 * <ul>
 *   <li>微信对 {@code cgi-bin/token} 及业务接口有<b>每日调用配额</b>，且同一 appid 的 token 全局唯一
 *       （重复拉取会顶掉旧值），故必须<b>缓存复用</b>，绝不能每次登录都重新拉取；</li>
 *   <li>有效期 7200s，这里<b>提前 200s</b> 触发刷新，避开边界过期；</li>
 *   <li>用 {@code synchronized} 做 single-flight，并发到期时只有一个线程真正去微信拉取。</li>
 * </ul>
 * 依赖前置：调用服务器公网出口 IP 必须加进小程序后台「IP 白名单」，否则微信返回 40164 拿不到 token。
 * <p>
 * 注意：access_token 是<b>服务端机密</b>，只用于后端调微信，绝不回传前端/写日志；URL 含 appSecret，同样不可入日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxAccessTokenService {

    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={}&secret={}";

    /**
     * 提前刷新余量（秒）：距过期不足该值即视为需要刷新，规避网络延迟导致的临界过期。
     */
    private static final long REFRESH_AHEAD_SECONDS = 200L;

    private final AuthProperties authProperties;

    private final Object lock = new Object();

    /**
     * 缓存的 access_token；volatile 保证快路径读到的最新值对其它线程立即可见。
     */
    private volatile String cachedToken;

    /**
     * 缓存 token 的过期时间戳（毫秒，epoch），已含提前刷新余量。
     */
    private volatile long expireAtMillis;

    /**
     * 获取当前有效的 access_token：命中缓存直接返回，过期则加锁刷新（double-check 防并发重复拉取）。
     *
     * @return 非空 access_token
     * @throws BusinessException 未配置微信凭据，或调用微信获取 token 失败
     */
    public String getToken() {
        // 快路径：未过期直接返回，无锁
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < expireAtMillis) {
            return cachedToken;
        }
        synchronized (lock) {
            // double-check：等锁期间可能已被其它线程刷新
            now = System.currentTimeMillis();
            if (cachedToken != null && now < expireAtMillis) {
                return cachedToken;
            }
            return refresh();
        }
    }

    /**
     * 强制刷新并写回缓存。仅在持锁时调用。
     */
    private String refresh() {
        AuthProperties.Wx wx = authProperties.getWx();
        if (wx == null || StrUtil.isBlank(wx.getAppId()) || StrUtil.isBlank(wx.getAppSecret())) {
            throw new BusinessException(ResultCode.WX_CONFIG_MISSING);
        }

        String url = StrUtil.format(TOKEN_URL, wx.getAppId(), wx.getAppSecret());
        String body;
        try {
            body = HttpUtil.get(url, 5000);
        } catch (Exception e) {
            // 仅记录异常信息，URL 含 appSecret 不可入日志
            log.warn("调用微信 cgi-bin/token 网络异常: {}", e.getMessage());
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED.getCode(), "获取微信access_token失败，请重试");
        }

        JSONObject json = JSONUtil.parseObj(body);
        int errcode = json.getInt("errcode", 0);
        String token = json.getStr("access_token");
        if (errcode != 0 || StrUtil.isBlank(token)) {
            // 40164=IP 不在白名单；40013=appid 无效；40002=secret 无效；41002=缺参数
            log.warn("获取微信 access_token 失败: errcode={}, errmsg={}", errcode, json.getStr("errmsg"));
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED.getCode(),
                    "获取微信access_token失败：" + json.getStr("errmsg"));
        }

        long expiresIn = json.getLong("expires_in", 7200L);
        this.cachedToken = token;
        this.expireAtMillis = System.currentTimeMillis()
                + Math.max(60L, expiresIn - REFRESH_AHEAD_SECONDS) * 1000L;
        log.info("微信 access_token 刷新成功，有效期 {}s（提前 {}s 复用刷新）", expiresIn, REFRESH_AHEAD_SECONDS);
        return token;
    }

    /**
     * 供自测入口使用：返回 token 的安全预览（长度 + 前 6 位），绝不回显完整值。
     * 若获取失败会抛出 BusinessException，由调用方转成错误信息展示。
     */
    public String maskedPreview() {
        String token = getToken();
        int len = token.length();
        String prefix = StrUtil.subPre(token, 6);
        return "len=" + len + ", prefix=" + prefix + "***";
    }
}
