package com.likebang.common.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页查询参数
 * <p>
 * 所有分页列表查询接口入参都继承或接收该对象，
 * 统一分页参数命名，避免每个 Controller 各写各的。
 */
@Data
public class PageParam implements Serializable {

    /**
     * 每页数量上限，防止恶意超大 size 拖库
     */
    private static final long MAX_SIZE = 100L;

    /**
     * 页码，从1开始
     */
    private Long current = 1L;

    /**
     * 每页数量
     */
    private Long size = 10L;

    /**
     * 搜索关键字（可选）
     */
    private String keyword;

    /**
     * 转成 MyBatis-Plus 的 Page 对象：各分页接口统一经此构建，
     * 集中做非法值兜底与 size 上限钳制
     */
    public <T> Page<T> toPage() {
        long c = current == null || current < 1 ? 1L : current;
        long s = size == null || size < 1 ? 10L : Math.min(size, MAX_SIZE);
        return new Page<>(c, s);
    }
}
