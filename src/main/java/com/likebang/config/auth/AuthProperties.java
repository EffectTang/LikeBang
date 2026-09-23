package com.likebang.config.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录认证配置属性
 * <p>
 * JWT 签名密钥与过期时间，生产环境密钥务必通过环境变量注入，禁止使用默认值。
 */
@Data
@Component
@ConfigurationProperties(prefix = "likebang.auth")
public class AuthProperties {

    /**
     * JWT 签名密钥（HS256 要求至少 32 字节）
     * 支持通过环境变量 JWT_SECRET 注入
     */
    private String jwtSecret;

    /**
     * Token 有效期（毫秒），默认 7 天
     */
    private long jwtTimeout = 7 * 24 * 60 * 60 * 1000L;

    /**
     * 内置超级管理员账号名（schema.sql 播种的 admin）：
     * 该账号不可被任何人禁用/删除/变更角色，防止权限体系被锁死或夺舍
     */
    private String superAdminUsername = "admin";
}
