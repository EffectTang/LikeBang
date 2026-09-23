package com.likebang.modules.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 变更用户角色请求（仅管理员可调用，独立接口便于审计与权限分层）
 */
@Data
public class UserRoleUpdateRequest {

    /**
     * 目标角色：0普通用户，1管理员，2运营管理员
     */
    @NotNull(message = "角色不能为空")
    @Min(value = 0, message = "角色取值非法")
    @Max(value = 2, message = "角色取值非法")
    private Integer role;
}
