package com.likebang.common.auth;

/**
 * 用户角色枚举（与 sys_user.role 取值对应）
 * <p>
 * 角色能力为显式规则而非数值大小比较：
 * USER 无管理能力的普通用户；ADMIN 全量后台能力含授予/撤销管理员；
 * STAFF 由管理员授权产生的运营管理员，仅能查看与维护普通用户基础信息/状态，
 * 不可变更任何角色。后续出现更多角色时按本枚举扩展并迁移 RBAC 三表。
 */
public enum UserRole {

    /** 普通用户 */
    USER(0),
    /** 管理员 */
    ADMIN(1),
    /** 运营管理员（管理员授权） */
    STAFF(2);

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
