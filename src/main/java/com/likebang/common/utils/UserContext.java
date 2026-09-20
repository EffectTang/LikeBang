package com.likebang.common.utils;

import com.likebang.common.auth.LoginUser;

/**
 * 当前登录用户上下文
 * <p>
 * 由 AuthInterceptor 在请求进入时写入，请求结束时清理。
 * Service / 切面通过 {@link #getLoginUser()} 获取当前操作人身份与角色。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        CURRENT.set(loginUser);
    }

    public static LoginUser getLoginUser() {
        return CURRENT.get();
    }

    /**
     * 当前登录用户ID，未登录返回 null
     */
    public static Long getUserId() {
        LoginUser user = CURRENT.get();
        return user == null ? null : user.getUserId();
    }

    /**
     * 当前登录用户ID，无上下文直接抛错（仅在拦截器放行后的受保护接口中使用）
     */
    public static Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new IllegalStateException("当前请求无登录用户上下文");
        }
        return userId;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
