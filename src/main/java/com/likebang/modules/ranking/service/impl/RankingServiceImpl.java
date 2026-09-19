package com.likebang.modules.ranking.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.likebang.common.dto.PageParam;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.common.utils.DateTimeUtils;
import com.likebang.modules.ranking.dto.request.RankingCreateRequest;
import com.likebang.modules.ranking.dto.response.RankingDetailResponse;
import com.likebang.modules.ranking.dto.response.RankingResponse;
import com.likebang.modules.ranking.entity.Ranking;
import com.likebang.modules.ranking.entity.RankingCategory;
import com.likebang.modules.ranking.entity.RankingItem;
import com.likebang.modules.ranking.entity.RankingReason;
import com.likebang.modules.ranking.mapper.RankingCategoryMapper;
import com.likebang.modules.ranking.mapper.RankingItemMapper;
import com.likebang.modules.ranking.mapper.RankingMapper;
import com.likebang.modules.ranking.mapper.RankingReasonMapper;
import com.likebang.modules.ranking.service.RankingService;
import com.likebang.modules.user.entity.SysUser;
import com.likebang.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 榜单服务实现：创建发布 / 社区浏览 / 详情
 */
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    /** 状态：正常（已发布） */
    private static final int STATUS_PUBLISHED = 1;
    /** 可见性：公开 */
    private static final int VISIBILITY_PUBLIC = 1;
    /** 详情理由列表最多展示条数 */
    private static final int REASON_LIMIT = 10;

    private final RankingMapper rankingMapper;
    private final RankingItemMapper rankingItemMapper;
    private final RankingReasonMapper rankingReasonMapper;
    private final RankingCategoryMapper rankingCategoryMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RankingCreateRequest request, Long creatorId) {
        List<RankingCreateRequest.Item> items = request.getItems();
        if (items.size() > request.getItemLimit()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "排名项数量不能超过最大排名项数量");
        }
        // 同一榜单内名称查重（忽略大小写）
        Set<String> nameSet = new HashSet<>();
        for (RankingCreateRequest.Item item : items) {
            if (!nameSet.add(item.getName().toLowerCase())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "存在同名的排名项：" + item.getName());
            }
        }

        Ranking ranking = new Ranking();
        ranking.setCreatorId(creatorId);
        ranking.setCategoryId(request.getCategoryId());
        ranking.setTitle(request.getTitle());
        ranking.setDescription(request.getDescription());
        ranking.setItemLimit(request.getItemLimit());
        ranking.setItemCount(items.size());
        ranking.setVisibility(request.getVisibility() == null
                ? VISIBILITY_PUBLIC : request.getVisibility());
        ranking.setStatus(STATUS_PUBLISHED);
        ranking.setCreatedAt(DateTimeUtils.now());
        ranking.setUpdatedAt(DateTimeUtils.now());
        rankingMapper.insert(ranking);

        int rank = 1;
        for (RankingCreateRequest.Item item : items) {
            RankingItem entity = new RankingItem();
            entity.setRankingId(ranking.getId());
            entity.setCreatorId(creatorId);
            entity.setName(item.getName());
            entity.setDescription(StrUtil.blankToDefault(item.getDescription(), null));
            entity.setCurrentRank(rank++);
            entity.setStatus(1);
            entity.setCreatedAt(DateTimeUtils.now());
            entity.setUpdatedAt(DateTimeUtils.now());
            rankingItemMapper.insert(entity);

            // 创建者的推荐理由
            if (StrUtil.isNotBlank(item.getReason())) {
                RankingReason reason = new RankingReason();
                reason.setRankingId(ranking.getId());
                reason.setItemId(entity.getId());
                reason.setCreatorId(creatorId);
                reason.setContent(item.getReason());
                reason.setCurrentRank(1);
                reason.setStatus(1);
                reason.setCreatedAt(DateTimeUtils.now());
                reason.setUpdatedAt(DateTimeUtils.now());
                rankingReasonMapper.insert(reason);

                RankingItem update = new RankingItem();
                update.setId(entity.getId());
                update.setReasonCount(1);
                rankingItemMapper.updateById(update);
            }
        }

        return ranking.getId();
    }

    @Override
    public IPage<RankingResponse> pagePublic(PageParam pageParam, Long categoryId) {
        LambdaQueryWrapper<Ranking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ranking::getStatus, STATUS_PUBLISHED)
                .eq(Ranking::getVisibility, VISIBILITY_PUBLIC);
        if (categoryId != null) {
            wrapper.eq(Ranking::getCategoryId, categoryId);
        }
        if (StrUtil.isNotBlank(pageParam.getKeyword())) {
            wrapper.and(w -> w.like(Ranking::getTitle, pageParam.getKeyword())
                    .or().like(Ranking::getDescription, pageParam.getKeyword()));
        }
        wrapper.orderByDesc(Ranking::getCreatedAt);

        Page<Ranking> page = rankingMapper.selectPage(
                new Page<>(pageParam.getCurrent(), pageParam.getSize()), wrapper);

        Map<Long, String> nicknames = nicknameMap(
                page.getRecords().stream().map(Ranking::getCreatorId).collect(Collectors.toSet()));
        Map<Long, String> categoryNames = categoryNameMap(
                page.getRecords().stream().map(Ranking::getCategoryId)
                        .filter(java.util.Objects::nonNull).collect(Collectors.toSet()));

        return page.convert(r -> {
            RankingResponse response = new RankingResponse();
            BeanUtils.copyProperties(r, response);
            response.setCreatorNickname(nicknames.get(r.getCreatorId()));
            if (r.getCategoryId() != null) {
                response.setCategoryName(categoryNames.get(r.getCategoryId()));
            }
            return response;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RankingDetailResponse detail(Long id) {
        Ranking ranking = rankingMapper.selectById(id);
        if (ranking == null || ranking.getStatus() == 3) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "榜单不存在");
        }
        if (ranking.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "榜单未发布");
        }

        // 浏览量 +1（DB 层原子自增，避免并发覆盖）
        rankingMapper.incrementViewCount(id);

        RankingDetailResponse response = new RankingDetailResponse();
        BeanUtils.copyProperties(ranking, response);
        response.setCreatorNickname(nicknameMap(Collections.singleton(ranking.getCreatorId()))
                .get(ranking.getCreatorId()));
        if (ranking.getCategoryId() != null) {
            response.setCategoryName(categoryNameMap(Collections.singleton(ranking.getCategoryId()))
                    .get(ranking.getCategoryId()));
        }

        List<RankingItem> items = rankingItemMapper.selectList(
                new LambdaQueryWrapper<RankingItem>()
                        .eq(RankingItem::getRankingId, id)
                        .eq(RankingItem::getStatus, 1)
                        .orderByAsc(RankingItem::getCurrentRank));
        if (items.isEmpty()) {
            response.setItems(Collections.emptyList());
            return response;
        }

        List<RankingReason> reasons = rankingReasonMapper.selectList(
                new LambdaQueryWrapper<RankingReason>()
                        .eq(RankingReason::getRankingId, id)
                        .eq(RankingReason::getStatus, 1)
                        .orderByAsc(RankingReason::getCurrentRank));

        // 批量补齐昵称（榜单创建者 + 项创建者 + 理由创建者）
        Set<Long> userIds = new HashSet<>();
        userIds.add(ranking.getCreatorId());
        items.forEach(i -> userIds.add(i.getCreatorId()));
        reasons.forEach(r -> userIds.add(r.getCreatorId()));
        Map<Long, String> nicknames = nicknameMap(userIds);

        Map<Long, List<RankingReason>> reasonsByItem = reasons.stream()
                .collect(Collectors.groupingBy(RankingReason::getItemId));

        response.setItems(items.stream().map(item -> {
            RankingDetailResponse.RankingItemResponse itemResponse =
                    new RankingDetailResponse.RankingItemResponse();
            BeanUtils.copyProperties(item, itemResponse);
            itemResponse.setCreatorNickname(nicknames.get(item.getCreatorId()));
            itemResponse.setReasons(reasonsByItem.getOrDefault(item.getId(), List.of())
                    .stream().limit(REASON_LIMIT).map(reason -> {
                        RankingDetailResponse.RankingReasonResponse reasonResponse =
                                new RankingDetailResponse.RankingReasonResponse();
                        BeanUtils.copyProperties(reason, reasonResponse);
                        reasonResponse.setCreatorNickname(
                                nicknames.get(reason.getCreatorId()));
                        return reasonResponse;
                    }).collect(Collectors.toList()));
            return itemResponse;
        }).collect(Collectors.toList()));

        return response;
    }

    private Map<Long, String> nicknameMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds));
        return users.stream().collect(
                Collectors.toMap(SysUser::getId, SysUser::getNickname, (a, b) -> a));
    }

    private Map<Long, String> categoryNameMap(Set<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        List<RankingCategory> categories = rankingCategoryMapper.selectList(
                new LambdaQueryWrapper<RankingCategory>().in(RankingCategory::getId, categoryIds));
        return categories.stream().collect(
                Collectors.toMap(RankingCategory::getId, RankingCategory::getName, (a, b) -> a));
    }
}
