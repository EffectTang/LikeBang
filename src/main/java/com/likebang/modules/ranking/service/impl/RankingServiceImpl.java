package com.likebang.modules.ranking.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.likebang.common.dto.PageParam;
import com.likebang.common.auth.LoginUser;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.common.utils.DateTimeUtils;
import com.likebang.common.utils.UserContext;
import com.likebang.modules.ranking.dto.request.RankingCreateRequest;
import com.likebang.modules.ranking.dto.request.RankingUpdateRequest;
import com.likebang.modules.ranking.dto.request.ReasonCreateRequest;
import com.likebang.modules.ranking.dto.request.ReasonUpdateRequest;
import com.likebang.modules.ranking.dto.response.RankingDetailResponse;
import com.likebang.modules.ranking.dto.response.RankingResponse;
import com.likebang.modules.ranking.entity.Ranking;
import com.likebang.modules.ranking.entity.RankingCategory;
import com.likebang.modules.ranking.entity.RankingItem;
import com.likebang.modules.ranking.entity.RankingItemVote;
import com.likebang.modules.ranking.entity.RankingReason;
import com.likebang.modules.ranking.entity.RankingReasonVote;
import com.likebang.modules.ranking.mapper.RankingCategoryMapper;
import com.likebang.modules.ranking.mapper.RankingItemMapper;
import com.likebang.modules.ranking.mapper.RankingItemVoteMapper;
import com.likebang.modules.ranking.mapper.RankingMapper;
import com.likebang.modules.ranking.mapper.RankingReasonMapper;
import com.likebang.modules.ranking.mapper.RankingReasonVoteMapper;
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
import java.util.Objects;
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
    /** 状态：已删除 */
    private static final int STATUS_DELETED = 3;
    /** 项目/理由启用状态 */
    private static final int ITEM_REASON_NORMAL = 1;
    /** 理由已删除 */
    private static final int REASON_DELETED = 0;
    /** 可见性：公开 */
    private static final int VISIBILITY_PUBLIC = 1;
    /** 详情理由列表最多展示条数 */
    private static final int REASON_LIMIT = 10;

    private final RankingMapper rankingMapper;
    private final RankingItemMapper rankingItemMapper;
    private final RankingReasonMapper rankingReasonMapper;
    private final RankingCategoryMapper rankingCategoryMapper;
    private final RankingItemVoteMapper rankingItemVoteMapper;
    private final RankingReasonVoteMapper rankingReasonVoteMapper;
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
        applyKeyword(wrapper, pageParam.getKeyword());
        wrapper.orderByDesc(Ranking::getCreatedAt);
        return toResponsePage(pageParam, wrapper);
    }

    @Override
    public IPage<RankingResponse> pageMine(PageParam pageParam, Integer status, LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        LambdaQueryWrapper<Ranking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ranking::getCreatorId, operator.getUserId())
                .ne(Ranking::getStatus, STATUS_DELETED);
        if (status != null) {
            wrapper.eq(Ranking::getStatus, status);
        }
        applyKeyword(wrapper, pageParam.getKeyword());
        wrapper.orderByDesc(Ranking::getCreatedAt);
        return toResponsePage(pageParam, wrapper);
    }

    private void applyKeyword(LambdaQueryWrapper<Ranking> wrapper, String keyword) {
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Ranking::getTitle, keyword)
                    .or().like(Ranking::getDescription, keyword));
        }
    }

    /**
     * 榜单分页查询公共尾部：执行分页并补齐创建者昵称与分类名
     */
    private IPage<RankingResponse> toResponsePage(PageParam pageParam, LambdaQueryWrapper<Ranking> wrapper) {
        // 统一经 PageParam.toPage() 构建：非法页码兜底 + size 上限钳制
        Page<Ranking> page = rankingMapper.selectPage(pageParam.toPage(), wrapper);

        Map<Long, String> nicknames = nicknameMap(
                page.getRecords().stream().map(Ranking::getCreatorId).collect(Collectors.toSet()));
        Map<Long, String> categoryNames = categoryNameMap(
                page.getRecords().stream().map(Ranking::getCategoryId)
                        .filter(Objects::nonNull).collect(Collectors.toSet()));

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
        if (ranking == null || ranking.getStatus() == STATUS_DELETED) {
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

        // 回填当前用户投票态（每表一条批量查询，无 N+1）；无登录上下文时全部为 null
        Long currentUserId = UserContext.getUserId();
        final Map<Long, Integer> myItemVotes = currentUserId == null ? Map.of()
                : rankingItemVoteMapper.selectList(Wrappers.<RankingItemVote>lambdaQuery()
                        .eq(RankingItemVote::getRankingId, id)
                        .eq(RankingItemVote::getUserId, currentUserId)).stream()
                .collect(Collectors.toMap(RankingItemVote::getItemId,
                        RankingItemVote::getVoteType, (a, b) -> a));
        final Map<Long, Integer> myReasonVotes = currentUserId == null ? Map.of()
                : rankingReasonVoteMapper.selectList(Wrappers.<RankingReasonVote>lambdaQuery()
                        .eq(RankingReasonVote::getRankingId, id)
                        .eq(RankingReasonVote::getUserId, currentUserId)).stream()
                .collect(Collectors.toMap(RankingReasonVote::getReasonId,
                        RankingReasonVote::getVoteType, (a, b) -> a));

        response.setItems(items.stream().map(item -> {
            RankingDetailResponse.RankingItemResponse itemResponse =
                    new RankingDetailResponse.RankingItemResponse();
            BeanUtils.copyProperties(item, itemResponse);
            itemResponse.setCreatorNickname(nicknames.get(item.getCreatorId()));
            itemResponse.setMyVoteType(myItemVotes.get(item.getId()));
            itemResponse.setReasons(reasonsByItem.getOrDefault(item.getId(), List.of())
                    .stream().limit(REASON_LIMIT).map(reason -> {
                        RankingDetailResponse.RankingReasonResponse reasonResponse =
                                new RankingDetailResponse.RankingReasonResponse();
                        BeanUtils.copyProperties(reason, reasonResponse);
                        reasonResponse.setCreatorNickname(
                                nicknames.get(reason.getCreatorId()));
                        reasonResponse.setMyVoteType(myReasonVotes.get(reason.getId()));
                        return reasonResponse;
                    }).collect(Collectors.toList()));
            return itemResponse;
        }).collect(Collectors.toList()));

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Ranking ranking = rankingMapper.selectById(id);
        if (ranking == null || ranking.getStatus() == STATUS_DELETED) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "榜单不存在");
        }
        boolean isOwner = ranking.getCreatorId() != null
                && ranking.getCreatorId().equals(operator.getUserId());
        if (!isOwner && !operator.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权删除该榜单");
        }

        Ranking update = new Ranking();
        update.setId(id);
        update.setStatus(STATUS_DELETED);
        update.setUpdatedAt(DateTimeUtils.now());
        rankingMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RankingUpdateRequest request, LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Ranking ranking = rankingMapper.selectById(id);
        if (ranking == null || ranking.getStatus() == STATUS_DELETED) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "榜单不存在");
        }
        boolean isOwner = ranking.getCreatorId() != null
                && ranking.getCreatorId().equals(operator.getUserId());
        if (!isOwner && !operator.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权修改该榜单");
        }

        // 局部更新：只写实际变更的列，避免整档回写覆盖并发修改（与 CategoryServiceImpl 同一写法）
        Ranking update = new Ranking();
        update.setId(id);
        boolean changed = false;
        if (request.getTitle() != null) {
            if (StrUtil.isBlank(request.getTitle())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "榜单标题不能为空");
            }
            update.setTitle(request.getTitle().trim());
            changed = true;
        }
        if (request.getDescription() != null) {
            update.setDescription(request.getDescription());
            changed = true;
        }
        if (request.getCategoryId() != null) {
            update.setCategoryId(request.getCategoryId());
            changed = true;
        }
        if (request.getItemLimit() != null) {
            int itemCount = ranking.getItemCount() == null ? 0 : ranking.getItemCount();
            if (request.getItemLimit() < itemCount) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "最大排名项数量不能小于当前排名项数量（" + itemCount + "）");
            }
            update.setItemLimit(request.getItemLimit());
            changed = true;
        }
        if (request.getVisibility() != null) {
            update.setVisibility(request.getVisibility());
            changed = true;
        }
        if (!changed) {
            return;
        }

        update.setUpdatedAt(DateTimeUtils.now());
        rankingMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addReason(Long rankingId, Long itemId, ReasonCreateRequest request, LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Ranking ranking = rankingMapper.selectById(rankingId);
        if (ranking == null || ranking.getStatus() == STATUS_DELETED) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "榜单不存在");
        }
        if (ranking.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "榜单未发布");
        }
        RankingItem item = rankingItemMapper.selectById(itemId);
        if (item == null
                || !Objects.equals(item.getRankingId(), rankingId)
                || !Objects.equals(item.getStatus(), ITEM_REASON_NORMAL)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "排名项不存在");
        }

        int nextRank = (item.getReasonCount() == null ? 0 : item.getReasonCount()) + 1;
        RankingReason reason = new RankingReason();
        reason.setRankingId(rankingId);
        reason.setItemId(itemId);
        reason.setCreatorId(operator.getUserId());
        reason.setContent(request.getContent());
        reason.setCurrentRank(nextRank);
        reason.setStatus(ITEM_REASON_NORMAL);
        reason.setCreatedAt(DateTimeUtils.now());
        reason.setUpdatedAt(DateTimeUtils.now());
        rankingReasonMapper.insert(reason);

        // 原子自增项上的 reason_count，避免读改写并发覆盖
        rankingItemMapper.update(null, Wrappers.<RankingItem>lambdaUpdate()
                .eq(RankingItem::getId, itemId)
                .setSql("reason_count = reason_count + 1"));

        return reason.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReason(Long reasonId, ReasonUpdateRequest request, LoginUser operator) {
        RankingReason reason = loadEditableReason(reasonId, operator);
        reason.setContent(request.getContent());
        reason.setUpdatedAt(DateTimeUtils.now());
        rankingReasonMapper.updateById(reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReason(Long reasonId, LoginUser operator) {
        RankingReason reason = loadEditableReason(reasonId, operator);

        RankingReason update = new RankingReason();
        update.setId(reason.getId());
        update.setStatus(REASON_DELETED);
        update.setUpdatedAt(DateTimeUtils.now());
        rankingReasonMapper.updateById(update);

        // 原子回退项上的 reason_count，下限 0
        rankingItemMapper.update(null, Wrappers.<RankingItem>lambdaUpdate()
                .eq(RankingItem::getId, reason.getItemId())
                .setSql("reason_count = GREATEST(reason_count - 1, 0)"));
    }

    /**
     * 加载可编辑的理由：存在、未删，且当前用户为创建者本人或管理员
     */
    private RankingReason loadEditableReason(Long reasonId, LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        RankingReason reason = rankingReasonMapper.selectById(reasonId);
        if (reason == null || !Objects.equals(reason.getStatus(), ITEM_REASON_NORMAL)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "理由不存在");
        }
        boolean isOwner = reason.getCreatorId() != null
                && reason.getCreatorId().equals(operator.getUserId());
        if (!isOwner && !operator.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作该理由");
        }
        return reason;
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
