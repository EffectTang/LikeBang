package com.likebang.modules.search.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.dto.PageParam;
import com.likebang.modules.ranking.dto.response.ItemSearchResponse;
import com.likebang.modules.ranking.dto.response.RankingResponse;
import com.likebang.modules.ranking.service.RankingService;
import com.likebang.modules.search.dto.response.SearchAggregateResponse;
import com.likebang.modules.search.service.SearchService;
import com.likebang.modules.system.constant.ConfigKeys;
import com.likebang.modules.system.service.SysConfigService;
import com.likebang.modules.user.dto.response.UserSearchItemResponse;
import com.likebang.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 全站搜索编排实现：纯只读，不加 @Transactional。
 * <p>
 * "全部"聚合各维度独立 try-catch 降级——某维失败仅记日志、返回空列表，不拖垮整体响应。
 * 聚合容量由配置中心 {@code search.aggregate.size} 驱动，兜底默认值。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final RankingService rankingService;
    private final UserService userService;
    private final SysConfigService sysConfigService;

    @Override
    public SearchAggregateResponse aggregate(String keyword) {
        SearchAggregateResponse response = new SearchAggregateResponse();
        if (StrUtil.isBlank(keyword)) {
            return response;
        }
        int size = sysConfigService.getInt(
                ConfigKeys.SEARCH_AGGREGATE_SIZE, ConfigKeys.DEFAULT_SEARCH_AGGREGATE_SIZE);

        try {
            response.setRankings(rankingService.pagePublic(aggregateParam(keyword, size), null, null)
                    .getRecords());
        } catch (Exception e) {
            log.warn("聚合搜索-榜单维度失败: keyword={}, err={}", keyword, e.getMessage());
        }
        try {
            response.setUsers(userService.pageSearchPublic(aggregateParam(keyword, size))
                    .getRecords());
        } catch (Exception e) {
            log.warn("聚合搜索-用户维度失败: keyword={}, err={}", keyword, e.getMessage());
        }
        try {
            response.setItems(rankingService.pageItemsByKeyword(aggregateParam(keyword, size), null)
                    .getRecords());
        } catch (Exception e) {
            log.warn("聚合搜索-排名项维度失败: keyword={}, err={}", keyword, e.getMessage());
        }
        try {
            response.setWorks(rankingService.sourceNames(keyword));
        } catch (Exception e) {
            log.warn("聚合搜索-作品名维度失败: keyword={}, err={}", keyword, e.getMessage());
        }
        return response;
    }

    @Override
    public IPage<RankingResponse> pageRankings(PageParam pageParam) {
        return rankingService.pagePublic(pageParam, null, null);
    }

    @Override
    public IPage<UserSearchItemResponse> pageUsers(PageParam pageParam) {
        return userService.pageSearchPublic(pageParam);
    }

    @Override
    public IPage<ItemSearchResponse> pageItems(PageParam pageParam) {
        return rankingService.pageItemsByKeyword(pageParam, null);
    }

    @Override
    public List<String> listWorks(String keyword) {
        return rankingService.sourceNames(keyword);
    }

    /**
     * 聚合区单维分页参数：固定第 1 页、size 由配置钳制（≥1），关键词统一注入
     */
    private PageParam aggregateParam(String keyword, int size) {
        PageParam param = new PageParam();
        param.setKeyword(keyword);
        param.setCurrent(1L);
        param.setSize((long) Math.max(1, size));
        return param;
    }
}
