package com.likebang.modules.ranking.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类详情响应 DTO
 */
@Data
public class CategoryResponse implements Serializable {

    private Long id;

    private String name;

    private String description;

    private String iconUrl;

    private Integer sort;

    private Integer status;

    /**
     * 是否开启元素来源填写：0关闭，1开启
     */
    private Integer sourceEnabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
