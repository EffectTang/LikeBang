package com.likebang.modules.ranking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.dto.PageParam;
import com.likebang.modules.ranking.dto.request.RankingCreateRequest;
import com.likebang.modules.ranking.dto.response.RankingDetailResponse;
import com.likebang.modules.ranking.dto.response.RankingResponse;

/**
 * 榜单服务接口
 */
public interface RankingService {

    /**
     * 创建榜单（发布），返回榜单ID
     */
    Long create(RankingCreateRequest request, Long creatorId);

    /**
     * 分页浏览公开榜单
     */
    IPage<RankingResponse> pagePublic(PageParam pageParam, Long categoryId);

    /**
     * 榜单详情（含排名项与理由），浏览量+1
     */
    RankingDetailResponse detail(Long id);
}
