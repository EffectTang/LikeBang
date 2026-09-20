package com.likebang.common.auth;

import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.common.utils.UserContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * {@link RequireRole} 授权切面：在受保护方法执行前校验当前登录用户角色。
 * <p>
 * 依赖 AuthInterceptor 已完成登录校验并写入 UserContext；未登录（无上下文）按 401 处理，
 * 角色不匹配按 403 处理。
 */
@Aspect
@Component
public class RequireRoleAspect {

    @Before("@annotation(com.likebang.common.auth.RequireRole) "
            + "|| @within(com.likebang.common.auth.RequireRole)")
    public void checkRole(JoinPoint joinPoint) {
        RequireRole requireRole = resolveAnnotation(joinPoint);
        if (requireRole == null) {
            return;
        }

        LoginUser loginUser = UserContext.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        boolean allowed = Arrays.stream(requireRole.value())
                .anyMatch(role -> role == loginUser.getRole());
        if (!allowed) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权限访问该功能");
        }
    }

    /**
     * 方法级注解优先，其次取类级注解
     */
    private RequireRole resolveAnnotation(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequireRole annotation = AnnotatedElementUtils.findMergedAnnotation(method, RequireRole.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils
                    .findMergedAnnotation(joinPoint.getTarget().getClass(), RequireRole.class);
        }
        return annotation;
    }
}
