package com.likebang.modules.user.scanlogin;

import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.modules.user.dto.response.LoginResponse;
import com.likebang.modules.user.dto.response.ScanQrResponse;
import com.likebang.modules.user.dto.response.ScanStatusResponse;
import com.likebang.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web 扫码登录（路线 2：复用已有小程序）的后端编排核心。
 * <p>
 * 流程：{@link #createLoginQr()} 生成 sceneId + 小程序码 → 手机扫码进小程序确认页 →
 * {@link #confirm(String, Long)}（需小程序登录态）以当前用户签发登录态并置 CONFIRMED →
 * Web {@link #pollStatus(String)} 领取一次性 token。
 * <p>
 * 会话存于进程内 {@link ConcurrentHashMap}，单实例够用；一旦扩容为多实例或需重启保活，
 * 必须替换为 Redis 或落库（Web 轮询与小程序确认可能打到不同节点，内存态会失效）。
 * 过期采用"读时判定 + 新建时顺带清扫"，未引入定时任务，保持自包含。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanLoginService {

    /** PENDING 存活上限：生成后 120s 内未被扫码确认即失效 */
    private static final long PENDING_TTL_MS = 120_000L;
    /** CONFIRMED 领取宽限：确认后 60s 内 Web 仍未轮询领取即清理（token 一次性，宁可失效不发） */
    private static final long CONFIRMED_GRACE_MS = 60_000L;
    private static final long QR_EXPIRES_SECONDS = PENDING_TTL_MS / 1000;

    private final WxQrcodeService wxQrcodeService;
    private final UserService userService;

    private final Map<String, ScanSession> sessions = new ConcurrentHashMap<>();

    /**
     * 生成一张登录二维码：先分配一次性 sceneId，再向微信换取小程序码；二维码生成失败则不留下悬空会话。
     */
    public ScanQrResponse createLoginQr() {
        purgeExpired();
        // UUID 去横线 = 32 位十六进制，恰好落在微信 scene ≤32 可见字符限制内
        String sceneId = UUID.randomUUID().toString().replace("-", "");
        String qrBase64 = wxQrcodeService.buildLoginQrDataUrl(sceneId);
        sessions.put(sceneId, new ScanSession(sceneId));
        return new ScanQrResponse(sceneId, qrBase64, QR_EXPIRES_SECONDS);
    }

    /**
     * Web 轮询扫码状态。CONFIRMED 为一次性领取：命中即摘除会话，杜绝同一 token 被重复领取。
     */
    public ScanStatusResponse pollStatus(String sceneId) {
        if (sceneId == null || sceneId.isBlank()) {
            return ScanStatusResponse.expired();
        }
        ScanSession session = sessions.get(sceneId);
        if (session == null) {
            // 不存在 / 已被领取 / 已被清扫，统一按失效返回，避免探测出有效 sceneId
            return ScanStatusResponse.expired();
        }
        if (session.getStatus() == ScanStatus.CONFIRMED) {
            sessions.remove(sceneId);
            return ScanStatusResponse.confirmed(session.getToken(), session.getUserInfo());
        }
        if (isPendingExpired(session)) {
            sessions.remove(sceneId);
            return ScanStatusResponse.expired();
        }
        return ScanStatusResponse.of(session.getStatus());
    }

    /**
     * 小程序端（已登录）确认：以当前登录用户身份为该 sceneId 签发 Web 登录态。
     * 会话不存在/超时/已确认一律拒绝，防止对失效或已消费的二维码重复授权。
     */
    public void confirm(String sceneId, Long userId) {
        ScanSession session = sceneId == null ? null : sessions.get(sceneId);
        if (session == null || session.getStatus() == ScanStatus.CONFIRMED || isPendingExpired(session)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "登录二维码已失效，请在电脑上刷新后重新扫码");
        }
        LoginResponse login = userService.issueLoginToken(userId);
        session.setToken(login.getToken());
        session.setUserInfo(login.getUserInfo());
        session.markConfirmed();
        log.info("扫码登录已确认: sceneId={}, userId={}", sceneId, userId);
    }

    private boolean isPendingExpired(ScanSession session) {
        return System.currentTimeMillis() - session.getCreatedAt() > PENDING_TTL_MS;
    }

    /**
     * 清扫：未确认且超 PENDING_TTL 的、已确认但超领取宽限未被取走的，一并移除。
     * 在每次新建二维码时触发，规模小、代价低，可避免用户中途关页造成的内存堆积。
     */
    private void purgeExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> {
            ScanSession s = e.getValue();
            if (s.getStatus() == ScanStatus.CONFIRMED) {
                return now - s.getUpdatedAt() > CONFIRMED_GRACE_MS;
            }
            return now - s.getCreatedAt() > PENDING_TTL_MS;
        });
    }
}
