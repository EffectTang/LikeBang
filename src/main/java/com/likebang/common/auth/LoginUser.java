package com.likebang.common.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录主体：由 AuthInterceptor 解析 Token + 查库后构造，存入 UserContext。
 * <p>
 * 角色实时来自数据库（非 Token 快照），保证权限变更即时生效。
 */
@Data
@AllArgsConstructor
public class LoginUser {

    private Long userId;

    private String username;

    private UserRole role;

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
