package com.likebang.modules.ranking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 为排名项新增推荐理由
 */
@Data
public class ReasonCreateRequest {

    @NotBlank(message = "理由内容不能为空")
    @Size(max = 1000, message = "理由内容长度不能超过1000")
    private String content;
}
