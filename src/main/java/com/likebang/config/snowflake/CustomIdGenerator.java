package com.likebang.config.snowflake;

import com.baomidou.mybatisplus.core.toolkit.Sequence;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 自定义雪花ID生成器
 * <p>
 * 用配置文件中显式指定的 workerId 和 datacenterId 创建 Sequence，
 * 替换 MyBatis-Plus 默认基于主机名 hash 取 ID 的策略。
 * <p>
 * 使用方式：
 * 实体类主键上 @TableId(type = IdType.ASSIGN_ID) 保持不变，
 * MyBatis-Plus 会自动从 Spring 容器中找到这个自定义 IdentifierGenerator 来生成 ID。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomIdGenerator implements IdentifierGenerator {

    private final SnowflakeProperties properties;

    private Sequence sequence;

    @PostConstruct
    public void init() {
        long workerId = properties.getWorkerId();
        long datacenterId = properties.getDatacenterId();

        if (workerId < 0 || workerId > 31) {
            throw new IllegalArgumentException(
                    "likebang.snowflake.worker-id 必须在 0~31 之间，当前值: " + workerId);
        }
        if (datacenterId < 0 || datacenterId > 31) {
            throw new IllegalArgumentException(
                    "likebang.snowflake.datacenter-id 必须在 0~31 之间，当前值: " + datacenterId);
        }

        this.sequence = new Sequence(workerId, datacenterId);

        log.info("✅ 雪花ID生成器初始化完成: workerId={}, datacenterId={}", workerId, datacenterId);
    }

    @Override
    public Number nextId(Object entity) {
        return sequence.nextId();
    }
}
