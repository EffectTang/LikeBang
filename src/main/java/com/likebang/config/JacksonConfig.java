package com.likebang.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 序列化配置
 * <p>
 * 主键采用 Snowflake，为 64 位长整型，超过 JavaScript Number 的安全整数范围（2^53-1）。
 * 若以 JSON 数字下发，前端解析会丢失末位精度（榜单/分类等 ID 被改写，导致按 ID 查询报“不存在”）。
 * 这里统一将 Long 序列化为字符串，前端全程按字符串处理，请求时 Spring 再自动转回 Long。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
