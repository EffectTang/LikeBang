package com.likebang.modules.system.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置项 响应 DTO
 */
@Data
public class SysConfigResponse implements Serializable {

    private Long id;

    private String configKey;

    private String configValue;

    private String configName;

    private String configGroup;

    private String valueType;

    private String description;

    /**
     * 是否允许管理员修改：0只读，1可改
     */
    private Integer editable;

    private String minValue;

    private String maxValue;

    private Integer sortOrder;

    private LocalDateTime updatedAt;
}
