package com.likebang.modules.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理后台编辑用户请求
 * <p>
 * null = 不修改（局部更新语义，与分类/榜单编辑一致）。
 * 有意不包含 role 字段：角色变更必须走独立的 PATCH /users/{id}/role 接口，
 * 使"运营管理员（STAFF）可编辑用户但不能提权"成为结构性约束而非运行时判断。
 */
@Data
public class UserUpdateRequest {

    @Size(max = 32, message = "昵称长度不能超过32个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;

    /**
     * 状态：0禁用，1正常
     */
    @Min(value = 0, message = "状态取值非法")
    @Max(value = 1, message = "状态取值非法")
    private Integer status;
}
