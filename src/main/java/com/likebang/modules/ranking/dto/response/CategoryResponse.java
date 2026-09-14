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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
