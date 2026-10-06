package com.likebang.modules.user.scanlogin;

/**
 * 扫码登录会话状态机。
 * <p>
 * 迁移：PENDING →（小程序确认）CONFIRMED →（Web 一次性领取后销毁）；任一状态超时→EXPIRED。
 * SCANNED 为预留态（小程序确认页加载时可回填"已被扫码，等待用户点确认"），本步暂不使用。
 */
public enum ScanStatus {
    /** 已生成，等待手机扫码 */
    PENDING,
    /** 已被扫码，等待用户在小程序端点确认（预留） */
    SCANNED,
    /** 已确认，登录态就绪，待 Web 轮询一次性领取 */
    CONFIRMED,
    /** 已失效/已被消费 */
    EXPIRED
}
