package com.likebang.modules.ranking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑排名项请求（榜单创建者本人或管理员）
 * <p>
 * 全量替换可编辑字段（前端编辑弹窗一次提交当前全部值）：空白一律落 null，因此"清空描述/图片/来源"
 * 通过提交空值实现。不含 currentRank 调整（改名/改内容不影响名次，重排仍归 P0 得分迭代）。
 */
@Data
public class ItemUpdateRequest {

    @NotBlank(message = "排名项名称不能为空")
    @Size(max = 200, message = "排名项名称长度不能超过200")
    private String name;

    @Size(max = 1000, message = "排名项描述长度不能超过1000")
    private String description;

    /**
     * 排名项图片地址；空/空白=清空配图
     */
    @Size(max = 512, message = "排名项图片地址长度不能超过512")
    private String imageUrl;

    /**
     * 来源类型；取值见 GET /rankings/source-types，服务端白名单校验；仅分类开启来源能力时生效
     */
    @Size(max = 32, message = "来源类型长度不能超过32")
    private String sourceType;

    /**
     * 来源作品名称；空/空白=清空
     */
    @Size(max = 100, message = "来源作品名称长度不能超过100")
    private String sourceName;

    /**
     * 来源补充说明；空/空白=清空
     */
    @Size(max = 500, message = "来源补充说明长度不能超过500")
    private String sourceDesc;
}
