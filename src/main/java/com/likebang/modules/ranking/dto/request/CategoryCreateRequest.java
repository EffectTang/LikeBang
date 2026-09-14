package com.likebang.modules.ranking.dto.request;

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
}
