package com.likebang.modules.ranking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改推荐理由（仅本人或管理员）
 */
@Data
public class ReasonUpdateRequest {

    @NotBlank(message = "理由内容不能为空")
    @Size(max = 1000, message = "理由内容长度不能超过1000")
    private String content;

    /**
     * 理由配图：null=不修改，传空字符串=清空配图（仅改图不重填正文时前端回传原图路径即可）
     */
    @Size(max = 512, message = "图片地址长度不能超过512")
    private String imageUrl;
}
