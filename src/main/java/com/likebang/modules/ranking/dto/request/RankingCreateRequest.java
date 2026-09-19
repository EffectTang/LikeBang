package com.likebang.modules.ranking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建榜单请求（发布到社区）
 */
@Data
public class RankingCreateRequest {

    @NotBlank(message = "榜单标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100")
    private String title;

    @Size(max = 1000, message = "描述长度不能超过1000")
    private String description;

    /**
     * 分类ID，可不选
     */
    private Long categoryId;

    @NotNull(message = "排名项数量不能为空")
    @Min(value = 3, message = "至少选择3个排名项")
    @Max(value = 50, message = "排名项数量不能超过50")
    private Integer itemLimit;

    /**
     * 可见性：0私有，1公开，2仅链接可见；默认公开
     */
    private Integer visibility = 1;

    @Valid
    @NotEmpty(message = "排名项不能为空")
    @Size(min = 2, max = 50, message = "排名项数量需在2~50之间")
    private List<Item> items;

    /**
     * 榜单内单个排名项（按数组顺序作为初始排名）
     */
    @Data
    public static class Item {

        @NotBlank(message = "排名项名称不能为空")
        @Size(max = 200, message = "排名项名称长度不能超过200")
        private String name;

        @Size(max = 1000, message = "排名项描述长度不能超过1000")
        private String description;

        /**
         * 创建者对该项的推荐理由
         */
        @Size(max = 1000, message = "推荐理由长度不能超过1000")
        private String reason;
    }
}
