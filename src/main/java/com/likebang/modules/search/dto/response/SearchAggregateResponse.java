package com.likebang.modules.search.dto.response;

import com.likebang.modules.ranking.dto.response.ItemSearchResponse;
import com.likebang.modules.ranking.dto.response.RankingResponse;
import com.likebang.modules.user.dto.response.UserSearchItemResponse;
import lombok.Data;

import java.util.List;

/**
 * 全站多维搜索聚合响应（"全部"模式，供搜索框联想面板分组导航用）。
 * <p>
 * 每一维只取前 N 条（N 由配置中心 {@code search.aggregate.size} 驱动），固定小容量、不分页；
 * 需要某维全量时前端切到对应 type 走单维分页搜索接口。各维独立降级：某维查询失败返回空列表，
 * 不拖垮整体响应。
 */
@Data
public class SearchAggregateResponse {

    private List<RankingResponse> rankings = List.of();

    private List<UserSearchItemResponse> users = List.of();

    private List<ItemSearchResponse> items = List.of();

    /**
     * 去重来源作品名（复用创建表单补全接口口径，固定上限）
     */
    private List<String> works = List.of();
}
