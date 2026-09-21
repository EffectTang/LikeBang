package com.likebang.modules.ranking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑榜单请求：仅元数据（标题/描述/分类/数量上限/可见性）。
 * <p>
 * 排名项与理由是社区共同积累的数据（含投票与他人理由），本期不开放结构编辑；
 * 字段为 null 表示不修改，与分类/理由更新接口语义一致。
 */
@Data
public class RankingUpdateRequest {

    @Size(max = 100, message = "标题长度不能超过100")
    private String title;

    /**
     * 榜单说明；传空字符串可清空
     */
    @Size(max = 1000, message = "描述长度不能超过1000")
    private String description;

    /**
     * 分类ID，仅支持换为其他分类（null=不修改，本期不支持清空分类）
     */
    private Long categoryId;

    /**
     * 最大排名项数量，Service 校验不得小于当前排名项数量
     */
    @Min(value = 3, message = "名次数量至少为3")
    @Max(value = 50, message = "名次数量不能超过50")
    private Integer itemLimit;

    /**
     * 可见性：0私有，1公开，2仅链接可见
     */
    @Min(value = 0, message = "可见性取值不合法")
    @Max(value = 2, message = "可见性取值不合法")
    private Integer visibility;
}
