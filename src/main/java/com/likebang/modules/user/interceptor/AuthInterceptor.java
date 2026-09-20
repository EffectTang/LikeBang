package com.likebang.modules.user.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likebang.common.auth.LoginUser;
import com.likebang.common.auth.UserRole;
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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 登录认证拦截器
 * <p>
 * 校验 Authorization: Bearer &lt;token&gt;，通过后写入 UserContext。
 * 登录/注册接口在 WebConfig 中整体放行；社区浏览类接口按“只读 GET”放行（见 {@link #PUBLIC_READS}），
 * 其写操作仍需登录。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 允许游客访问的只读接口（仅 GET 方法命中时放行，写操作仍需登录）
     */
    private static final List<String> PUBLIC_READS = List.of(
            "/rankings/public",
            "/categories"
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final UserServiceImpl userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 游客可浏览的只读接口：GET 命中白名单直接放行，不要求 Token
        if (isPublicRead(request)) {
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

        UserContext.set(new LoginUser(userId, user.getUsername(), UserRole.of(user.getRole())));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean isPublicRead(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }
        final String lookupPath = path;
        return PUBLIC_READS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, lookupPath));
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
