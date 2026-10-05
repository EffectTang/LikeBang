package com.likebang.modules.search.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.dto.PageParam;
import com.likebang.modules.ranking.dto.response.ItemSearchResponse;
import com.likebang.modules.ranking.dto.response.RankingResponse;
import com.likebang.modules.search.dto.response.SearchAggregateResponse;
import com.likebang.modules.user.dto.response.UserSearchItemResponse;

import java.util.List;

/**
 * 全站搜索编排服务：跨域聚合 + 单维分页 delegator。
 * <p>
 * 只做编排不含业务事实源，各维度实际查询委托 ranking/user 域。
 */
public interface SearchService {

    /**
     * "全部"模式聚合导航：榜单/用户/排名项各取前 N 条 + 去重作品名，各维独立降级
     */
    SearchAggregateResponse aggregate(String keyword);

    /**
     * 单维分页：榜单（标题/描述/发起人昵称）
     */
    IPage<RankingResponse> pageRankings(PageParam pageParam);

    /**
     * 单维分页：用户（昵称）
     */
    IPage<UserSearchItemResponse> pageUsers(PageParam pageParam);

    /**
     * 单维分页：排名项（名称/描述/来源作品名）
     */
    IPage<ItemSearchResponse> pageItems(PageParam pageParam);

    /**
     * 单维：去重来源作品名（固定上限，供回搜补全）
     */
    List<String> listWorks(String keyword);
}
