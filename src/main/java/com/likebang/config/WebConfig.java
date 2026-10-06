package com.likebang.config;

import com.likebang.modules.file.service.FileStorageService;
import com.likebang.modules.user.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * 图片存储唯一事实源：静态映射的目录/前缀直接复用 FileStorageService 已归一化的解析结果，
     * 保证“落盘写到哪”与“对外从哪读”永远同一目录（根治相对路径 ./uploads 被两套 API 解析分叉）。
     */
    private final FileStorageService fileStorageService;

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
                        "/user/auth/wx-login",
                        // Web 扫码登录：生成二维码与轮询状态为公开接口（扫码确认 scan-confirm 需小程序登录态，不放行）
                        "/user/auth/scan-qr",
                        "/user/auth/scan-status",
                        // 上传图片走 <img> 标签访问，浏览器不会携带 Authorization 头，整体放行（仅只读静态资源）
                        "/uploads/**",
                        "/error");
    }

    /**
     * 上传目录静态资源映射：/uploads/** → FileStorageService 已解析的绝对存储根目录
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = fileStorageService.getStorageRoot().toUri().toString();
        // 目录尚未创建时 toUri() 不会带尾部斜杠，会被当成文件基准导致相对解析错位
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(fileStorageService.getAccessPrefix() + "/**")
                .addResourceLocations(location);
    }
}
