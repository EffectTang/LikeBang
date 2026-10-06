package com.likebang.modules.user.dto.response;

import com.likebang.modules.user.scanlogin.ScanStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 扫码状态轮询响应。仅 CONFIRMED 时携带一次性登录态（token/userInfo），
 * Web 领取后后端立即销毁会话，故同一 sceneId 第二次轮询会返回 EXPIRED。
 */
@Data
@AllArgsConstructor
public class ScanStatusResponse {

    /** PENDING / SCANNED / CONFIRMED / EXPIRED */
    private String status;

    /** 仅 CONFIRMED 非空：Web 端直接以此完成登录 */
    private String token;

    /** 仅 CONFIRMED 非空 */
    private UserInfoResponse userInfo;

    public static ScanStatusResponse of(ScanStatus status) {
        return new ScanStatusResponse(status.name(), null, null);
    }

    public static ScanStatusResponse expired() {
        return new ScanStatusResponse(ScanStatus.EXPIRED.name(), null, null);
    }

    public static ScanStatusResponse confirmed(String token, UserInfoResponse userInfo) {
        return new ScanStatusResponse(ScanStatus.CONFIRMED.name(), token, userInfo);
    }
}
