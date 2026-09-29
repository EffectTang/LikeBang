package com.likebang.modules.ranking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改分类请求 DTO
 */
@Data
public class CategoryUpdateRequest implements Serializable {

    @Size(max = 32, message = "分类名称最长32个字符")
    private String name;

    @Size(max = 255, message = "分类描述最长255个字符")
    private String description;

    @Size(max = 512, message = "图标地址最长512个字符")
    private String iconUrl;

    private Integer sort;

    /**
     * 状态：0禁用，1正常
     */
    private Integer status;

    /**
     * 是否开启元素来源填写：0关闭，1开启；null 不修改
     */
    @Min(value = 0, message = "来源开关取值0/1")
    @Max(value = 1, message = "来源开关取值0/1")
    private Integer sourceEnabled;
}
