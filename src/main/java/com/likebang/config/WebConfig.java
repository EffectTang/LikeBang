package com.likebang.config;

import com.likebang.modules.user.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 登录拦截：除认证接口白名单外，其它请求默认需携带 Token。
     * <p>
     * 社区浏览类接口（如 /rankings/public、/categories）的“只读 GET”放行由
     * {@link AuthInterceptor} 按 HTTP 方法精确判断，因此此处不再按路径整体放行，
     * 以免其新增/修改/删除等写操作被无登录访问。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/auth/login",
                        "/user/auth/register",
                        "/error");
    }
}
