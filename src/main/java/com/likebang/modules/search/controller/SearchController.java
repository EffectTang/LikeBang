package com.likebang.modules.search.controller;

import com.likebang.common.dto.PageParam;
import com.likebang.common.result.Result;
import com.likebang.modules.search.constant.SearchType;
import com.likebang.modules.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全站多维搜索 接口。
 * <p>
 * 统一入口 {@code GET /search?keyword=&type=}：
 * <ul>
 *   <li>{@code type=ALL}（默认）：返回聚合导航 {@link com.likebang.modules.search.dto.response.SearchAggregateResponse}，
 *       榜单/用户/排名项各取前 N 条 + 去重作品名，纯展示、不分页；</li>
 *   <li>{@code type=RANKING|USER|ITEM}：委托对应域单维分页搜索，返回标准 IPage；</li>
 *   <li>{@code type=SOURCE}：去重来源作品名列表。</li>
 * </ul>
 * 所有维度均需登录（不在游客白名单），关键词全在既有域接口内做 trim/空处理与 size 钳制。
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public Result<Object> search(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String type,
                                 PageParam pageParam) {
        SearchType searchType = SearchType.parse(type);
        pageParam.setKeyword(keyword);
        Object data = switch (searchType) {
            case ALL -> searchService.aggregate(keyword);
            case RANKING -> searchService.pageRankings(pageParam);
            case USER -> searchService.pageUsers(pageParam);
            case ITEM -> searchService.pageItems(pageParam);
            case SOURCE -> searchService.listWorks(keyword);
        };
        return Result.success(data);
    }
}
