package com.likebang.common.auth;

/**
 * 用户角色枚举（与 sys_user.role 取值对应）
 */
public enum UserRole {

    /** 普通用户 */
    USER(0),
    /** 管理员 */
    ADMIN(1);

    private final int code;

    UserRole(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 按数据库取值解析角色，未知/空一律降级为普通用户（最小权限原则）
     */
    public static UserRole of(Integer code) {
        if (code != null) {
            for (UserRole role : values()) {
                if (role.code == code) {
                    return role;
                }
            }
        }
        return USER;
    }
}
