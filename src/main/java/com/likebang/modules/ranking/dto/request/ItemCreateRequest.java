package com.likebang.modules.ranking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增排名项（榜单创建者本人或管理员）
 * <p>
 * 从"排名项结构编辑"中切出的最小子集：仅追加一项，来源(source)/初始理由走各自已有接口后续补。
 */
@Data
public class ItemCreateRequest {

    @NotBlank(message = "排名项名称不能为空")
    @Size(max = 200, message = "排名项名称长度不能超过200")
    private String name;

    @Size(max = 1000, message = "排名项描述长度不能超过1000")
    private String description;

    /**
     * 排名项图片地址（可选，先走 /files/image 上传拿相对路径）
     */
    @Size(max = 512, message = "排名项图片地址长度不能超过512")
    private String imageUrl;
}
