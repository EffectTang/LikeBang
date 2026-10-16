package com.likebang.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 完成密码重置请求（免登录）：凭邮件链接中的一次性令牌设置新密码
 */
@Data
public class PasswordResetCompleteRequest {

    @NotBlank(message = "重置凭证不能为空")
    @Size(max = 64, message = "重置凭证格式不正确")
    private String token;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在6~32之间")
    private String newPassword;
}
