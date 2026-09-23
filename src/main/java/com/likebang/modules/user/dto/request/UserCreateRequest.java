package com.likebang.modules.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员后台新增用户请求 DTO
 * <p>
 * 与 RegisterRequest 保持同口径口令策略（密码 6~32、用户名 4~32 字符合法字符），
 * 确保管理员代建账号不会弱化注册安全基线。
 * 额外支持可选 role/status：入口已由 @RequireRole(ADMIN) 收窄，STAFF 无法访问本接口，
 * 因此不构成提权路径；未指定时默认普通用户 + 正常状态。
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$",
            message = "用户名只能包含字母、数字、下划线，长度4~32")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称长度不能超过32个字符")
    private String nickname;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在6~32之间")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 初始角色：0普通用户（默认），1管理员，2运营管理员；null = 普通用户
     */
    @Min(value = 0, message = "角色取值非法")
    @Max(value = 2, message = "角色取值非法")
    private Integer role;

    /**
     * 初始状态：0禁用，1正常（默认）；null = 正常
     */
    @Min(value = 0, message = "状态取值非法")
    @Max(value = 1, message = "状态取值非法")
    private Integer status;
}
