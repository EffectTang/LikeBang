package com.likebang.modules.ranking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.dto.PageParam;
import com.likebang.common.result.Result;
import com.likebang.common.utils.UserContext;
import com.likebang.modules.ranking.dto.request.RankingCreateRequest;
import com.likebang.modules.ranking.dto.response.RankingDetailResponse;
import com.likebang.modules.ranking.dto.response.RankingResponse;
import com.likebang.modules.ranking.service.RankingService;
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
     * 榜单详情
     */
    @GetMapping("/{id}")
    public Result<RankingDetailResponse> detail(@PathVariable Long id) {
        return Result.success(rankingService.detail(id));
    }
}
