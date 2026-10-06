package com.likebang.modules.user.scanlogin;

import com.likebang.modules.user.dto.response.UserInfoResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * 单次扫码登录的内存会话。仅存活于 {@link ScanLoginService} 的进程内缓存中，
 * 用于在"Web 发起 → 小程序确认 → Web 领取"三方之间传递一次性登录态。
 * <p>
 * 跨字段读写的可见性由 volatile 保证；对同一 sceneId 的写入仅发生在小程序确认这一路径，
 * 领取走 ConcurrentHashMap.remove 的原子摘除，故无需额外加锁。
 */
@Getter
public class ScanSession {

    private final String sceneId;
    private final long createdAt;

    private volatile ScanStatus status;
    private volatile long updatedAt;

    /** CONFIRMED 后就绪，供 Web 轮询一次性领取；领取后即随会话销毁 */
    @Setter
    private volatile String token;
    @Setter
    private volatile UserInfoResponse userInfo;

    public ScanSession(String sceneId) {
        this.sceneId = sceneId;
        this.status = ScanStatus.PENDING;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markConfirmed() {
        this.status = ScanStatus.CONFIRMED;
        this.updatedAt = System.currentTimeMillis();
    }
}
