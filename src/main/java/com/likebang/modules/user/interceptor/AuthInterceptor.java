package com.likebang.modules.user.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likebang.common.result.Result;
import com.likebang.common.result.ResultCode;
import com.likebang.common.utils.UserContext;
import com.likebang.modules.user.entity.SysUser;
import com.likebang.modules.user.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录认证拦截器
 * <p>
 * 校验 Authorization: Bearer &lt;token&gt;，通过后写入 UserContext。
 * 白名单（登录/注册接口）在 WebConfig 中排除。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserServiceImpl userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String auth = request.getHeader(HEADER);
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            return reject(response);
        }

        Long userId = userService.parseToken(auth.substring(BEARER_PREFIX.length()));
        if (userId == null) {
            return reject(response);
        }

        // Token 有效但账号可能已被禁用/删除
        SysUser user;
        try {
            user = userService.getExistingById(userId);
        } catch (Exception e) {
            return reject(response);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            return reject(response);
        }

        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean reject(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(Result.error(ResultCode.UNAUTHORIZED)));
        return false;
    }
}
