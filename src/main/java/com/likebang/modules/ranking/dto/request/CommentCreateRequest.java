package com.likebang.modules.ranking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增理由评论
 */
@Data
public class CommentCreateRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容长度不能超过500")
    private String content;
}
