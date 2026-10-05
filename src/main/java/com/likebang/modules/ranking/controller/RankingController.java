package com.likebang.modules.ranking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.dto.PageParam;
import com.likebang.common.result.Result;
import com.likebang.common.utils.UserContext;
import com.likebang.modules.ranking.dto.request.CommentCreateRequest;
import com.likebang.modules.ranking.dto.request.ItemImageUpdateRequest;
import com.likebang.modules.ranking.dto.request.ItemCreateRequest;
import com.likebang.modules.ranking.dto.request.ItemUpdateRequest;
import com.likebang.modules.ranking.dto.request.RankingCreateRequest;
import com.likebang.modules.ranking.dto.request.RankingUpdateRequest;
import com.likebang.modules.ranking.dto.request.ReasonCreateRequest;
import com.likebang.modules.ranking.dto.request.ReasonUpdateRequest;
import com.likebang.modules.ranking.dto.request.VoteRequest;
import com.likebang.modules.ranking.dto.response.CommentResponse;
import com.likebang.modules.ranking.dto.response.ItemSearchResponse;
import com.likebang.modules.ranking.dto.response.RankingDetailResponse;
import com.likebang.modules.ranking.dto.response.RankingResponse;
import com.likebang.modules.ranking.dto.response.SpaceStatsResponse;
import com.likebang.modules.ranking.dto.response.VoteResponse;
import com.likebang.modules.ranking.service.RankingService;
import com.likebang.modules.ranking.service.ReasonCommentService;
import com.likebang.modules.ranking.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 榜单 接口
 */
@RestController
@RequestMapping("/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;
    private final VoteService voteService;
    private final ReasonCommentService reasonCommentService;

    /**
     * 创建榜单（需登录）
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RankingCreateRequest request) {
        return Result.success(rankingService.create(request, UserContext.requireUserId()));
    }

    /**
     * 分页浏览公开榜单，creatorNickname 为可选二级过滤（Phase2 交叉搜索）
     */
    @GetMapping("/public")
    public Result<IPage<RankingResponse>> pagePublic(
            @ModelAttribute PageParam pageParam,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String creatorNickname) {
        return Result.success(rankingService.pagePublic(pageParam, categoryId, creatorNickname));
    }

    /**
     * 某用户的公开榜单（分页，他人主页用；需登录，仅返回已发布+公开）
     */
    @GetMapping("/by-user/{userId}")
    public Result<IPage<RankingResponse>> pageByUser(
            @PathVariable Long userId,
            @ModelAttribute PageParam pageParam) {
        return Result.success(rankingService.pageByAuthor(pageParam, userId));
    }

    /**
     * 我创建的榜单（分页，排除已删除，需登录）
     */
    @GetMapping("/mine")
    public Result<IPage<RankingResponse>> pageMine(
            @ModelAttribute PageParam pageParam,
            @RequestParam(required = false) Integer status) {
        return Result.success(rankingService.pageMine(pageParam, status, UserContext.getLoginUser()));
    }

    /**
     * 我参与投票的榜单（分页，需登录，仅返回未删除榜单，供「我的空间-动态」用）
     */
    @GetMapping("/voted-by-me")
    public Result<IPage<RankingResponse>> pageVotedByMe(@ModelAttribute PageParam pageParam) {
        return Result.success(rankingService.pageVotedByMe(pageParam, UserContext.requireUserId()));
    }

    /**
     * 「我的空间」数据概览（需登录，仅本人：我发布榜单数 / 收到票数 / 参与投票榜单数）
     */
    @GetMapping("/space-stats")
    public Result<SpaceStatsResponse> spaceStats() {
        return Result.success(rankingService.spaceStats(UserContext.requireUserId()));
    }

    /**
     * 编辑榜单元数据（需登录）：仅创建者本人或管理员，null 字段不修改
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody RankingUpdateRequest request) {
        rankingService.update(id, request, UserContext.getLoginUser());
        return Result.success();
    }

    /**
     * 来源类型字典：key -> 展示名（需登录，创建表单下拉选项单一事实源）
     */
    @GetMapping("/source-types")
    public Result<Map<String, String>> sourceTypes() {
        return Result.success(rankingService.sourceTypes());
    }

    /**
     * 榜单封面最大数量上限（需登录，上传表单单一事实源；
     * 管理员在系统设置中改 ranking.detail.cover_limit 后即时生效）
     */
    @GetMapping("/cover-limit")
    public Result<Integer> coverLimit() {
        return Result.success(rankingService.coverLimit());
    }

    /**
     * 站内历史来源作品名去重列表（需登录，创建表单自动补全，keyword 可选）
     */
    @GetMapping("/source-names")
    public Result<List<String>> sourceNames(@RequestParam(required = false) String keyword) {
        return Result.success(rankingService.sourceNames(keyword));
    }

    /**
     * 跟榜单排名项内容分页搜索（需登录）：命中项名称/描述/来源作品名，仅返回公开已发布榜单下的有效项。
     * sourceName 为可选二级过滤（Phase2 交叉搜索）。
     * 字面量路径 /items/search 优先于 /{id} 匹配，与 /cover-limit、/source-names 同范式。
     */
    @GetMapping("/items/search")
    public Result<IPage<ItemSearchResponse>> searchItems(
            @ModelAttribute PageParam pageParam,
            @RequestParam(required = false) String sourceName) {
        return Result.success(rankingService.pageItemsByKeyword(pageParam, sourceName));
    }

    /**
     * 榜单详情（每个排名项附认同数降序的理由 Top10）
     */
    @GetMapping("/{id}")
    public Result<RankingDetailResponse> detail(@PathVariable Long id) {
        return Result.success(rankingService.detail(id));
    }

    /**
     * 某排名项详情（排名项页面主体，需登录）
     */
    @GetMapping("/{id}/items/{itemId}")
    public Result<RankingDetailResponse.RankingItemResponse> getItem(
            @PathVariable Long id, @PathVariable Long itemId) {
        return Result.success(rankingService.getItem(id, itemId));
    }

    /**
     * 某排名项的全量理由（分页，认同数降序，需登录）
     */
    @GetMapping("/{id}/items/{itemId}/reasons")
    public Result<IPage<RankingDetailResponse.RankingReasonResponse>> pageItemReasons(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @ModelAttribute PageParam pageParam) {
        return Result.success(rankingService.pageItemReasons(id, itemId, pageParam));
    }

    /**
     * 修改排名项配图（需登录）：仅排名项创建者本人或管理员；imageUrl 传空表示清空
     */
    @PutMapping("/{id}/items/{itemId}/image")
    public Result<Void> updateItemImage(@PathVariable Long id,
                                        @PathVariable Long itemId,
                                        @Valid @RequestBody ItemImageUpdateRequest request) {
        rankingService.updateItemImage(id, itemId, request.getImageUrl(), UserContext.getLoginUser());
        return Result.success();
    }

    /**
     * 新增排名项（需登录）：仅榜单创建者本人或管理员；返回新项ID
     */
    @PostMapping("/{id}/items")
    public Result<Long> addItem(@PathVariable Long id,
                                @Valid @RequestBody ItemCreateRequest request) {
        return Result.success(rankingService.addItem(id, request, UserContext.getLoginUser()));
    }

    /**
     * 删除排名项（需登录）：仅榜单创建者本人或管理员；软删该项及其下理由
     */
    @DeleteMapping("/{id}/items/{itemId}")
    public Result<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        rankingService.deleteItem(id, itemId, UserContext.getLoginUser());
        return Result.success();
    }

    /**
     * 编辑排名项（需登录）：仅榜单创建者本人或管理员；全量替换名称/描述/配图/来源，不动名次
     */
    @PutMapping("/{id}/items/{itemId}")
    public Result<Void> updateItem(@PathVariable Long id,
                                   @PathVariable Long itemId,
                                   @Valid @RequestBody ItemUpdateRequest request) {
        rankingService.updateItem(id, itemId, request, UserContext.getLoginUser());
        return Result.success();
    }

    /**
     * 删除榜单（需登录）：仅创建者本人或管理员可删
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        rankingService.delete(id, UserContext.getLoginUser());
        return Result.success();
    }

    /**
     * 为榜单中某排名项新增推荐理由（需登录，任何已登录用户均可）
     */
    @PostMapping("/{id}/items/{itemId}/reasons")
    public Result<Long> addReason(@PathVariable Long id,
                                  @PathVariable Long itemId,
                                  @Valid @RequestBody ReasonCreateRequest request) {
        return Result.success(rankingService.addReason(id, itemId, request, UserContext.getLoginUser()));
    }

    /**
     * 修改推荐理由（仅本人或管理员）
     */
    @PutMapping("/reasons/{reasonId}")
    public Result<Void> updateReason(@PathVariable Long reasonId,
                                     @Valid @RequestBody ReasonUpdateRequest request) {
        rankingService.updateReason(reasonId, request, UserContext.getLoginUser());
        return Result.success();
    }

    /**
     * 删除推荐理由（仅本人或管理员）
     */
    @DeleteMapping("/reasons/{reasonId}")
    public Result<Void> deleteReason(@PathVariable Long reasonId) {
        rankingService.deleteReason(reasonId, UserContext.getLoginUser());
        return Result.success();
    }

    /**
     * 对排名项投/换 认同或反对票（需登录，重复投同类型幂等）
     */
    @PostMapping("/{id}/items/{itemId}/vote")
    public Result<VoteResponse> voteItem(@PathVariable Long id,
                                         @PathVariable Long itemId,
                                         @Valid @RequestBody VoteRequest request) {
        return Result.success(voteService.voteItem(id, itemId, request, UserContext.getLoginUser()));
    }

    /**
     * 取消对排名项的投票（需登录，无票时幂等成功）
     */
    @DeleteMapping("/{id}/items/{itemId}/vote")
    public Result<VoteResponse> cancelItemVote(@PathVariable Long id,
                                               @PathVariable Long itemId) {
        return Result.success(voteService.cancelItemVote(id, itemId, UserContext.getLoginUser()));
    }

    /**
     * 对理由投/换 认同或反对票（需登录，榜单归属由理由记录反查）
     */
    @PostMapping("/reasons/{reasonId}/vote")
    public Result<VoteResponse> voteReason(@PathVariable Long reasonId,
                                           @Valid @RequestBody VoteRequest request) {
        return Result.success(voteService.voteReason(reasonId, request, UserContext.getLoginUser()));
    }

    /**
     * 取消对理由的投票（需登录，无票时幂等成功）
     */
    @DeleteMapping("/reasons/{reasonId}/vote")
    public Result<VoteResponse> cancelReasonVote(@PathVariable Long reasonId) {
        return Result.success(voteService.cancelReasonVote(reasonId, UserContext.getLoginUser()));
    }

    /**
     * 发布理由评论（需登录，仅登录可见可发：不在免登录白名单）
     */
    @PostMapping("/reasons/{reasonId}/comments")
    public Result<CommentResponse> addComment(@PathVariable Long reasonId,
                                              @Valid @RequestBody CommentCreateRequest request) {
        return Result.success(reasonCommentService.add(reasonId, request, UserContext.getLoginUser()));
    }

    /**
     * 某理由的评论分页（时间正序，需登录）
     */
    @GetMapping("/reasons/{reasonId}/comments")
    public Result<IPage<CommentResponse>> pageReasonComments(
            @PathVariable Long reasonId,
            @ModelAttribute PageParam pageParam) {
        return Result.success(reasonCommentService.pageByReason(reasonId, pageParam));
    }

    /**
     * 删除理由评论（仅评论者本人或管理员）
     */
    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        reasonCommentService.delete(commentId, UserContext.getLoginUser());
        return Result.success();
    }
}
