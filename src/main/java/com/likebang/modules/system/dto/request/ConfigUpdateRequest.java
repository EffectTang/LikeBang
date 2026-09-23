package com.likebang.modules.system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改系统配置值 请求 DTO
 * <p>
 * 一期后台只开放"改值"，不开放改键名/类型/分组等元数据（这些属结构定义，改之易失配）。
 */
@Data
public class ConfigUpdateRequest implements Serializable {

    /**
     * 新配置值（统一以字符串提交，服务端按 value_type 校验）
     */
    @NotNull(message = "配置值不能为空")
    @Size(max = 512, message = "配置值最长512个字符")
    private String configValue;
}
