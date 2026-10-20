package com.likebang.config;

import com.likebang.modules.file.service.FileStorageService;
import com.likebang.modules.user.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
     * 图片存储唯一事实源：静态映射的目录/前缀直接复用本地存储实现已归一化的解析结果，
     * 保证“落盘写到哪”与“对外从哪读”永远同一目录（根治相对路径 ./uploads 被两套 API 解析分叉）。
     * <p>
     * 用 {@link ObjectProvider} 延迟按需获取：云存储模式（storage-mode=cloudbase）下本地 Bean
     * 不存在，此时无需本地静态资源映射，跳过即可（新图走云存储 CDN 域名直读）。
     */
    private final ObjectProvider<FileStorageService> fileStorageServiceProvider;

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
                        // 找回密码：申请重置邮件与完成重置均为免登录接口（频控与令牌校验在服务层）
                        "/user/auth/password-reset/**",
                        // Web 扫码登录：生成二维码与轮询状态为公开接口（扫码确认 scan-confirm 需小程序登录态，不放行）
                        "/user/auth/scan-qr",
                        "/user/auth/scan-status",
                        // 上传图片走 <img> 标签访问，浏览器不会携带 Authorization 头，整体放行（仅只读静态资源）
                        "/uploads/**",
                        "/error");
    }

    /**
     * 上传目录静态资源映射（仅本地存储模式）：/uploads/** → FileStorageService 已解析的绝对存储根目录。
     * 云存储模式下无本地 Bean，跳过映射。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        FileStorageService fileStorageService = fileStorageServiceProvider.getIfAvailable();
        if (fileStorageService == null) {
            return;
        }
        String location = fileStorageService.getStorageRoot().toUri().toString();
        // 目录尚未创建时 toUri() 不会带尾部斜杠，会被当成文件基准导致相对解析错位
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(fileStorageService.getAccessPrefix() + "/**")
                .addResourceLocations(location);
    }
}
