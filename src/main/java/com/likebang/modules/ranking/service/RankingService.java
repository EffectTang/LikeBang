package com.likebang.modules.ranking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.auth.LoginUser;
import com.likebang.common.dto.PageParam;
import com.likebang.modules.ranking.dto.request.RankingCreateRequest;
import com.likebang.modules.ranking.dto.request.ReasonCreateRequest;
import com.likebang.modules.ranking.dto.request.ReasonUpdateRequest;
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

    /**
     * 删除榜单（软删除）：仅创建者本人或管理员可删
     */
    void delete(Long id, LoginUser operator);

    /**
     * 为指定榜单下的排名项新增推荐理由（需登录，返回新理由ID）
     */
    Long addReason(Long rankingId, Long itemId, ReasonCreateRequest request, LoginUser operator);

    /**
     * 修改推荐理由（仅本人或管理员）
     */
    void updateReason(Long reasonId, ReasonUpdateRequest request, LoginUser operator);

    /**
     * 删除推荐理由（仅本人或管理员，软删除，同步回退项上 reason_count）
     */
    void deleteReason(Long reasonId, LoginUser operator);
}
