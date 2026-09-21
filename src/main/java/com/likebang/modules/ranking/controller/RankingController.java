package com.likebang.modules.ranking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.dto.PageParam;
import com.likebang.common.result.Result;
import com.likebang.common.utils.UserContext;
import com.likebang.modules.ranking.dto.request.RankingCreateRequest;
import com.likebang.modules.ranking.dto.request.RankingUpdateRequest;
import com.likebang.modules.ranking.dto.request.ReasonCreateRequest;
import com.likebang.modules.ranking.dto.request.ReasonUpdateRequest;
import com.likebang.modules.ranking.dto.request.VoteRequest;
import com.likebang.modules.ranking.dto.response.RankingDetailResponse;
import com.likebang.modules.ranking.dto.response.RankingResponse;
import com.likebang.modules.ranking.dto.response.VoteResponse;
import com.likebang.modules.ranking.service.RankingService;
import com.likebang.modules.ranking.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 榜单 接口
 */
@RestController
@RequestMapping("/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;
    private final VoteService voteService;

    /**
     * 创建榜单（需登录）
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RankingCreateRequest request) {
        return Result.success(rankingService.create(request, UserContext.requireUserId()));
    }

    /**
     * 分页浏览公开榜单
     */
    @GetMapping("/public")
    public Result<IPage<RankingResponse>> pagePublic(
            @ModelAttribute PageParam pageParam,
            @RequestParam(required = false) Long categoryId) {
        return Result.success(rankingService.pagePublic(pageParam, categoryId));
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
     * 编辑榜单元数据（需登录）：仅创建者本人或管理员，null 字段不修改
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody RankingUpdateRequest request) {
        rankingService.update(id, request, UserContext.getLoginUser());
        return Result.success();
    }

    /**
     * 榜单详情
     */
    @GetMapping("/{id}")
    public Result<RankingDetailResponse> detail(@PathVariable Long id) {
        return Result.success(rankingService.detail(id));
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
}
