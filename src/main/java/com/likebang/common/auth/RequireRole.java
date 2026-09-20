package com.likebang.common.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色授权注解：标注在 Controller 方法或类上，声明访问所需的角色。
 * <p>
 * 由 {@link RequireRoleAspect} 统一拦截校验，命中当前登录用户角色不匹配时抛 403。
 * 默认要求管理员，例如：{@code @RequireRole(UserRole.ADMIN)}。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 允许访问的角色，满足其中之一即可（默认仅管理员）
     */
    UserRole[] value() default {UserRole.ADMIN};
}
