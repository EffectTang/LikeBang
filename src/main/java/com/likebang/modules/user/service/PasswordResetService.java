package com.likebang.modules.user.service;

import com.likebang.modules.user.dto.request.PasswordResetCompleteRequest;
import com.likebang.modules.user.dto.request.PasswordResetRequest;

/**
 * 找回密码服务接口：邮件重置链接（一次性令牌），全程免登录
 */
public interface PasswordResetService {

    /**
     * 申请重置：向目标邮箱发送含重置链接的邮件。
     * 无论邮箱是否存在均正常返回（防账号探测），仅受频控约束。
     */
    void requestReset(PasswordResetRequest request, String clientIp);

    /**
     * 完成重置：校验令牌有效性与新密码规则，修改密码并作废该账号全部重置凭证
     */
    void completeReset(PasswordResetCompleteRequest request);
}
