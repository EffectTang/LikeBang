package com.likebang.modules.ranking.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改排名项配图（仅本人或管理员）
 * <p>
 * 专用于"单独换图/清图"，不触碰排名项其他结构，避免与"排名项结构编辑"迭代耦合。
 * imageUrl 传空串/空白表示清空配图。
 */
@Data
public class ItemImageUpdateRequest {

    /**
     * 排名项配图相对路径（如 /uploads/202609/xxx.png）；空或空白=清空配图
     */
    @Size(max = 512, message = "图片地址长度不能超过512")
    private String imageUrl;
}
