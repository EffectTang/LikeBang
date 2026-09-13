package com.likebang.config.snowflake;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 雪花算法配置属性
 * <p>
 * 通过配置文件手动指定 workerId 和 datacenterId，
 * 解决 MyBatis-Plus 默认基于主机名 hash 取 ID 在多实例/容器化下冲突的问题。
 * <p>
 * 5 bit workerId:     0 ~ 31
 * 5 bit datacenterId: 0 ~ 31
 */
@Data
@Component
@ConfigurationProperties(prefix = "likebang.snowflake")
public class SnowflakeProperties {

    /**
     * 机器ID（0~31），每个 JVM 实例必须不一样
     * 支持通过环境变量 WORKER_ID 注入（如 K8s Pod env）
     */
    private long workerId = 1L;

    /**
     * 数据中心ID（0~31），跨机房/跨区域部署时用来区分
     * 支持通过环境变量 DC_ID 注入
     */
    private long datacenterId = 1L;
}
