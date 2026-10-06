package com.likebang.modules.user.scanlogin;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.config.auth.AuthProperties;
import com.likebang.modules.user.service.WxAccessTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 生成"小程序码"（{@code wxacode.getUnlimited}）：Web 扫码登录的二维码载体。
 * <p>
 * 与 jscode2session 不同，该接口需带服务端 access_token（由 {@link WxAccessTokenService} 统一缓存提供）；
 * scene 传入一次性 sceneId，用户扫码后小程序在 {@code page} 指定页面的 onLoad 中以 {@code options.scene} 取回。
 * <p>
 * 注意：access_token 属服务端机密、拼在 URL 上，均不可入日志；接口成功返回图片二进制、失败返回 JSON，
 * 二者以响应 Content-Type 区分。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxQrcodeService {

    /** 扫码确认页路径：小程序侧须存在此页面（功能第 3 步实现）。 */
    private static final String SCAN_LOGIN_PAGE = "pages/scan-login/scan-login";
    private static final int QR_WIDTH = 280;

    private final AuthProperties authProperties;
    private final WxAccessTokenService wxAccessTokenService;

    /**
     * 生成登录小程序码，返回可直接放进 {@code <img src>} 的 base64 data URL。
     *
     * @param sceneId 一次性会话标识（≤32 可见字符），扫码后由小程序回传给确认接口
     * @throws BusinessException access_token 获取失败，或微信生成二维码失败
     */
    public String buildLoginQrDataUrl(String sceneId) {
        String accessToken = wxAccessTokenService.getToken();
        String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;

        // check_path=false：不因目标页面尚未发布而拦截，便于开发/体验期联调
        JSONObject body = JSONUtil.createObj()
                .set("scene", sceneId)
                .set("page", SCAN_LOGIN_PAGE)
                .set("check_path", false)
                .set("env_version", authProperties.getWx().getEnvVersion())
                .set("width", QR_WIDTH);

        HttpResponse resp;
        try {
            // 微信硬约束：getUnlimited 的 POST 参数必须是原始 JSON，不能以表单提交；
            // 显式声明 Content-Type，否则缺省按表单发出会读不到参数而报 40097 invalid args
            resp = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(8000)
                    .execute();
        } catch (Exception e) {
            log.warn("调用微信 getwxacodeunlimit 网络异常: {}", e.getMessage());
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED.getCode(), "生成登录二维码失败，请重试");
        }

        byte[] bytes = resp.bodyBytes();
        String contentType = resp.header("Content-Type");
        if (contentType != null && contentType.contains("application/json")) {
            JSONObject json = JSONUtil.parseObj(new String(bytes, StandardCharsets.UTF_8));
            // 常见错误：40066 invalid page/scene；41002 缺参；45009 接口调用超频率限制；48001 api 未授权
            log.warn("生成登录二维码失败: errcode={}, errmsg={}", json.getInt("errcode"), json.getStr("errmsg"));
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED.getCode(),
                    "生成登录二维码失败：" + json.getStr("errmsg"));
        }
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED.getCode(), "生成登录二维码失败：返回内容为空");
        }
        return "data:image/png;base64," + Base64.encode(bytes);
    }
}
