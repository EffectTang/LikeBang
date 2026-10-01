package com.likebang.modules.ranking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.auth.LoginUser;
import com.likebang.common.dto.PageParam;
import com.likebang.modules.ranking.dto.request.RankingCreateRequest;
import com.likebang.modules.ranking.dto.request.ItemCreateRequest;
import com.likebang.modules.ranking.dto.request.ItemUpdateRequest;
import com.likebang.modules.ranking.dto.request.RankingUpdateRequest;
import com.likebang.modules.ranking.dto.request.ReasonCreateRequest;
import com.likebang.modules.ranking.dto.request.ReasonUpdateRequest;
import com.likebang.modules.ranking.dto.response.RankingDetailResponse;
import com.likebang.modules.ranking.dto.response.RankingResponse;

import java.util.List;
import java.util.Map;

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
     * 分页浏览某用户的公开榜单（他人主页用）：仅 status=已发布 且 visibility=公开
     */
    IPage<RankingResponse> pageByAuthor(PageParam pageParam, Long authorId);

    /**
     * 分页浏览我创建的榜单（排除已删除，可按状态筛选），返回含状态供前端展示
     */
    IPage<RankingResponse> pageMine(PageParam pageParam, Integer status, LoginUser operator);

    /**
     * 编辑榜单元数据（仅创建者本人或管理员）；null 字段不修改
     */
    void update(Long id, RankingUpdateRequest request, LoginUser operator);

    /**
     * 榜单详情（含排名项与每个项按认同数降序的理由 Top10），浏览量+1
     */
    RankingDetailResponse detail(Long id);

    /**
     * 指定排名项详情（排名项详情页主体，不含理由，理由走分页接口）
     */
    RankingDetailResponse.RankingItemResponse getItem(Long rankingId, Long itemId);

    /**
     * 指定排名项的全量理由（分页，按认同数降序，含当前用户投票态）
     */
    IPage<RankingDetailResponse.RankingReasonResponse> pageItemReasons(
            Long rankingId, Long itemId, PageParam pageParam);

    /**
     * 修改排名项配图（仅排名项创建者本人或管理员）；imageUrl 为空表示清空
     */
    void updateItemImage(Long rankingId, Long itemId, String imageUrl, LoginUser operator);

    /**
     * 新增排名项（仅榜单创建者本人或管理员），返回新项ID；受 itemLimit 与榜内名称唯一约束
     */
    Long addItem(Long rankingId, ItemCreateRequest request, LoginUser operator);

    /**
     * 删除排名项（仅榜单创建者本人或管理员）：软删该项及其下理由，重排剩余项名次，item_count 原子回退
     */
    void deleteItem(Long rankingId, Long itemId, LoginUser operator);

    /**
     * 编辑排名项（仅榜单创建者本人或管理员）：全量替换名称/描述/配图/来源字段，不动 currentRank
     */
    void updateItem(Long rankingId, Long itemId, ItemUpdateRequest request, LoginUser operator);

    /**
     * 来源类型字典：key -> 展示名（前端下拉选项单一事实源，需登录）
     */
    Map<String, String> sourceTypes();

    /**
     * 站内历史来源作品名去重列表（创建表单自动补全用，关键词可选，需登录）
     */
    List<String> sourceNames(String keyword);

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
