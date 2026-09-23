package com.likebang.modules.ranking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.likebang.common.auth.LoginUser;
import com.likebang.common.dto.PageParam;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.common.utils.DateTimeUtils;
import com.likebang.modules.ranking.dto.request.CommentCreateRequest;
import com.likebang.modules.ranking.dto.response.CommentResponse;
import com.likebang.modules.ranking.entity.Ranking;
import com.likebang.modules.ranking.entity.RankingReason;
import com.likebang.modules.ranking.entity.RankingReasonComment;
import com.likebang.modules.ranking.mapper.RankingMapper;
import com.likebang.modules.ranking.mapper.RankingReasonCommentMapper;
import com.likebang.modules.ranking.mapper.RankingReasonMapper;
import com.likebang.modules.ranking.service.ReasonCommentService;
import com.likebang.modules.user.entity.SysUser;
import com.likebang.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 理由评论服务实现：校验链、软删、原子计数、局部更新均沿用理由模块既有范式
 */
@Service
@RequiredArgsConstructor
public class ReasonCommentServiceImpl implements ReasonCommentService {

    /** 榜单状态：正常（已发布） */
    private static final int RANKING_PUBLISHED = 1;
    /** 榜单状态：已删除 */
    private static final int RANKING_DELETED = 3;
    /** 理由/评论启用状态 */
    private static final int STATUS_NORMAL = 1;
    /** 评论已删除 */
    private static final int COMMENT_DELETED = 0;

    private final RankingReasonCommentMapper commentMapper;
    private final RankingReasonMapper rankingReasonMapper;
    private final RankingMapper rankingMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentResponse add(Long reasonId, CommentCreateRequest request, LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        RankingReason reason = requirePublishedReason(reasonId);

        RankingReasonComment comment = new RankingReasonComment();
        comment.setRankingId(reason.getRankingId());
        comment.setItemId(reason.getItemId());
        comment.setReasonId(reasonId);
        comment.setCreatorId(operator.getUserId());
        comment.setContent(request.getContent());
        comment.setStatus(STATUS_NORMAL);
        comment.setCreatedAt(DateTimeUtils.now());
        comment.setUpdatedAt(DateTimeUtils.now());
        commentMapper.insert(comment);

        // 原子自增理由上的 comment_count，避免并发读改写覆盖计数（同 reason_count 范式）
        rankingReasonMapper.update(null, Wrappers.<RankingReason>lambdaUpdate()
                .eq(RankingReason::getId, reasonId)
                .setSql("comment_count = comment_count + 1"));

        CommentResponse response = toResponse(comment);
        // LoginUser 不携带昵称（避免动鉴权链路构造点），主键单查补齐后返回完整评论体
        SysUser creator = sysUserMapper.selectById(operator.getUserId());
        response.setCreatorNickname(creator == null ? null : creator.getNickname());
        return response;
    }

    @Override
    public IPage<CommentResponse> pageByReason(Long reasonId, PageParam pageParam) {
        requirePublishedReason(reasonId);

        // 时间正序=贴吧楼层顺序；同秒用 id 兜底保证分页稳定不重不漏
        Page<RankingReasonComment> page = commentMapper.selectPage(pageParam.toPage(),
                new LambdaQueryWrapper<RankingReasonComment>()
                        .eq(RankingReasonComment::getReasonId, reasonId)
                        .eq(RankingReasonComment::getStatus, STATUS_NORMAL)
                        .orderByAsc(RankingReasonComment::getCreatedAt)
                        .orderByAsc(RankingReasonComment::getId));

        Map<Long, String> nicknames = nicknameMap(page.getRecords().stream()
                .map(RankingReasonComment::getCreatorId).collect(Collectors.toSet()));

        return page.convert(c -> {
            CommentResponse response = toResponse(c);
            response.setCreatorNickname(nicknames.get(c.getCreatorId()));
            return response;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long commentId, LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        RankingReasonComment comment = commentMapper.selectById(commentId);
        if (comment == null || !Objects.equals(comment.getStatus(), STATUS_NORMAL)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "评论不存在");
        }
        boolean isOwner = comment.getCreatorId() != null
                && comment.getCreatorId().equals(operator.getUserId());
        if (!isOwner && !operator.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权删除该评论");
        }

        RankingReasonComment update = new RankingReasonComment();
        update.setId(commentId);
        update.setStatus(COMMENT_DELETED);
        update.setUpdatedAt(DateTimeUtils.now());
        commentMapper.updateById(update);

        // 原子回退理由上的 comment_count，下限 0
        rankingReasonMapper.update(null, Wrappers.<RankingReason>lambdaUpdate()
                .eq(RankingReason::getId, comment.getReasonId())
                .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
    }

    /**
     * 校验理由存在、正常且所属榜单已发布（评论链路自包含，无需前端传榜单 ID）
     */
    private RankingReason requirePublishedReason(Long reasonId) {
        RankingReason reason = rankingReasonMapper.selectById(reasonId);
        if (reason == null || !Objects.equals(reason.getStatus(), STATUS_NORMAL)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "理由不存在");
        }
        Ranking ranking = rankingMapper.selectById(reason.getRankingId());
        if (ranking == null || ranking.getStatus() == RANKING_DELETED) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "榜单不存在");
        }
        if (ranking.getStatus() != RANKING_PUBLISHED) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "榜单未发布");
        }
        return reason;
    }

    private CommentResponse toResponse(RankingReasonComment comment) {
        CommentResponse response = new CommentResponse();
        BeanUtils.copyProperties(comment, response);
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
}
