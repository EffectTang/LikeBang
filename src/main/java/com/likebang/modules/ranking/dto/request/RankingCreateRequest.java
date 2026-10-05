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
     * 封面图地址（前端先走 /files/image 上传拿到相对路径，可选）；
     * 历史单图字段，与 coverUrls 二选一，同传时以 coverUrls 为准
     */
    @Size(max = 512, message = "封面地址长度不能超过512")
    private String coverUrl;

    /**
     * 封面图地址列表（可选，按数组顺序即详情页轮播顺序）；
     * 数量上限由系统配置 ranking.detail.cover_limit 在 Service 层校验，至少 1 张
     */
    @Size(max = 10, message = "封面图数量不能超过10张")
    private List<String> coverUrls;

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
         * 排名项图片地址（可选，同封面图走上传接口获取）
         */
        @Size(max = 512, message = "排名项图片地址长度不能超过512")
        private String imageUrl;

        /**
         * 来源类型（可选，仅分类开启来源能力时生效）：取值见 GET /rankings/source-types，服务端白名单校验
         */
        @Size(max = 32, message = "来源类型长度不能超过32")
        private String sourceType;

        /**
         * 来源作品名称（可选）：台词/歌词/书摘等摘录型元素的出处，如“让子弹飞”
         */
        @Size(max = 100, message = "来源作品名称长度不能超过100")
        private String sourceName;

        /**
         * 来源补充说明（可选）：如台词出现的场景/章节
         */
        @Size(max = 500, message = "来源补充说明长度不能超过500")
        private String sourceDesc;

        /**
         * 创建者对该项的推荐理由
         */
        @Size(max = 1000, message = "推荐理由长度不能超过1000")
        private String reason;
    }
}
