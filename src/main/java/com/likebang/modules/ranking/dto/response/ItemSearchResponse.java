package com.likebang.modules.ranking.dto.response;

import lombok.Data;

/**
 * 排名项搜索结果响应（跨榜单内容维度搜索）。
 * <p>
 * 只投影"定位一个排名项并跳转到其详情页"所需的最小字段：项本身 + 所属榜单元信息 + 项创建者昵称。
 * 有意<b>不含</b>投票计数、理由、图片二进制等重字段，避免搜索结果列表被拖大；
 * 需要详情时前端凭 {@code rankingId + itemId} 走既有项详情接口。
 */
@Data
public class ItemSearchResponse {

    private Long itemId;

    private String name;

    private String description;

    private String imageUrl;

    /**
     * 来源作品名（分类开启来源能力时才有值）
     */
    private String sourceName;

    private Long rankingId;

    private String rankingTitle;

    private String coverUrl;

    private Long creatorId;

    private String creatorNickname;
}
