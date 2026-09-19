package com.likebang.common.utils;

/**
 * 当前登录用户上下文
 * <p>
 * 由 AuthInterceptor 在请求进入时写入，请求结束时清理。
 * Service 层通过 {@link #getUserId()} 获取当前操作人。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    /**
     * 获取当前登录用户ID，未登录返回 null
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 获取当前登录用户ID，未登录直接抛空指针（仅在拦截器放行后的受保护接口中使用）
     */
    public static Long requireUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new IllegalStateException("当前请求无登录用户上下文");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
