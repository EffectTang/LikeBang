package com.likebang.modules.ranking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增分类请求 DTO
 */
@Data
public class CategoryCreateRequest implements Serializable {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 32, message = "分类名称最长32个字符")
    private String name;

    @Size(max = 255, message = "分类描述最长255个字符")
    private String description;

    @Size(max = 512, message = "图标地址最长512个字符")
    private String iconUrl;

    private Integer sort = 0;

    private Integer status = 1;

    /**
     * 是否开启元素来源填写：0关闭，1开启
     */
    @Min(value = 0, message = "来源开关取值0/1")
    @Max(value = 1, message = "来源开关取值0/1")
    private Integer sourceEnabled = 0;
}
