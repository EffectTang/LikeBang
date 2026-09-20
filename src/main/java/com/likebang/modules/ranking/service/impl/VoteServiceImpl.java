package com.likebang.modules.ranking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.likebang.common.auth.LoginUser;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.common.utils.DateTimeUtils;
import com.likebang.modules.ranking.dto.request.VoteRequest;
import com.likebang.modules.ranking.dto.response.VoteResponse;
import com.likebang.modules.ranking.entity.Ranking;
import com.likebang.modules.ranking.entity.RankingItem;
import com.likebang.modules.ranking.entity.RankingItemVote;
import com.likebang.modules.ranking.entity.RankingReason;
import com.likebang.modules.ranking.entity.RankingReasonVote;
import com.likebang.modules.ranking.mapper.RankingItemMapper;
import com.likebang.modules.ranking.mapper.RankingItemVoteMapper;
import com.likebang.modules.ranking.mapper.RankingMapper;
import com.likebang.modules.ranking.mapper.RankingReasonMapper;
import com.likebang.modules.ranking.mapper.RankingReasonVoteMapper;
import com.likebang.modules.ranking.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 投票服务实现：一人对同一对象仅一条票（uk_item_user / uk_reason_user 唯一键保证），
 * 投票=换票（upsert），取消为物理删除；计数按状态矩阵推导 delta 后单条 SQL 原子落账。
 * <p>
 * 计数矩阵（对 item/reason）：首次投票 对应票数+1 且 participant+1；换票 旧票-1 新票+1；
 * 取消 对应票数-1 且 participant-1；重复投同类型 / 无票可取消 均幂等成功不动账。
 * ranking.participant_count 语义为"该榜单任一对象存在有效票的 distinct 用户数"。
 */
@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {

    /** 投票类型：认同 */
    private static final int VOTE_AGREE = 1;
    /** 投票类型：反对 */
    private static final int VOTE_OPPOSE = -1;
    /** 榜单状态：已发布 */
    private static final int STATUS_PUBLISHED = 1;
    /** 榜单状态：已删除 */
    private static final int STATUS_DELETED = 3;
    /** 排名项/理由状态：正常 */
    private static final int TARGET_NORMAL = 1;

    private final RankingMapper rankingMapper;
    private final RankingItemMapper rankingItemMapper;
    private final RankingReasonMapper rankingReasonMapper;
    private final RankingItemVoteMapper rankingItemVoteMapper;
    private final RankingReasonVoteMapper rankingReasonVoteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoteResponse voteItem(Long rankingId, Long itemId, VoteRequest request, LoginUser operator) {
        int voteType = requireValidVoteType(request.getVoteType());
        Long userId = requireUserId(operator);
        requirePublishedRanking(rankingId);
        requireItemOfRanking(rankingId, itemId);

        RankingItemVote existing = rankingItemVoteMapper.selectOne(
                Wrappers.<RankingItemVote>lambdaQuery()
                        .eq(RankingItemVote::getItemId, itemId)
                        .eq(RankingItemVote::getUserId, userId));
        if (existing != null && Objects.equals(existing.getVoteType(), voteType)) {
            // 已投同类型：幂等成功，不动票与计数
            return itemResponse(itemId, voteType);
        }

        // 推导对象级计数 delta：首次投 新票+1 participant+1；换票 旧票-1 新票+1
        int dAgree = voteType == VOTE_AGREE ? 1 : 0;
        int dOppose = voteType == VOTE_OPPOSE ? 1 : 0;
        int dParticipant = 1;
        int dRankParticipant = 0;
        if (existing != null) {
            if (Objects.equals(existing.getVoteType(), VOTE_AGREE)) {
                dAgree -= 1;
            } else {
                dOppose -= 1;
            }
            dParticipant = 0;
        } else if (!hasAnyVoteInRanking(rankingId, userId)) {
            // 本票是该用户在榜单下的第一张有效票 → 榜单参与人数 +1
            dRankParticipant = 1;
        }

        LocalDateTime now = DateTimeUtils.now();
        try {
            if (existing == null) {
                RankingItemVote vote = new RankingItemVote();
                vote.setRankingId(rankingId);
                vote.setItemId(itemId);
                vote.setUserId(userId);
                vote.setVoteType(voteType);
                vote.setCreatedAt(now);
                vote.setUpdatedAt(now);
                rankingItemVoteMapper.insert(vote);
            } else {
                RankingItemVote update = new RankingItemVote();
                update.setId(existing.getId());
                update.setVoteType(voteType);
                update.setUpdatedAt(now);
                rankingItemVoteMapper.updateById(update);
            }
        } catch (DuplicateKeyException e) {
            // 同一用户并发首次投票时 uk_item_user 兜底，让用户侧直接重试即可
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "操作过于频繁，请稍后重试");
        }

        rankingItemMapper.applyVoteDelta(itemId, dAgree, dOppose, dParticipant);
        updateRankingParticipant(rankingId, dRankParticipant);
        return itemResponse(itemId, voteType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoteResponse cancelItemVote(Long rankingId, Long itemId, LoginUser operator) {
        Long userId = requireUserId(operator);
        requirePublishedRanking(rankingId);
        requireItemOfRanking(rankingId, itemId);

        RankingItemVote existing = rankingItemVoteMapper.selectOne(
                Wrappers.<RankingItemVote>lambdaQuery()
                        .eq(RankingItemVote::getItemId, itemId)
                        .eq(RankingItemVote::getUserId, userId));
        if (existing == null) {
            // 无票可取消：幂等成功
            return itemResponse(itemId, null);
        }

        rankingItemVoteMapper.deleteById(existing.getId());

        int dAgree = Objects.equals(existing.getVoteType(), VOTE_AGREE) ? -1 : 0;
        int dOppose = Objects.equals(existing.getVoteType(), VOTE_OPPOSE) ? -1 : 0;
        rankingItemMapper.applyVoteDelta(itemId, dAgree, dOppose, -1);

        // 删除后该用户在此榜单已无任何有效票 → 榜单参与人数回退
        if (!hasAnyVoteInRanking(rankingId, userId)) {
            updateRankingParticipant(rankingId, -1);
        }
        return itemResponse(itemId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoteResponse voteReason(Long reasonId, VoteRequest request, LoginUser operator) {
        int voteType = requireValidVoteType(request.getVoteType());
        Long userId = requireUserId(operator);
        RankingReason reason = requireNormalReason(reasonId);
        requirePublishedRanking(reason.getRankingId());

        RankingReasonVote existing = rankingReasonVoteMapper.selectOne(
                Wrappers.<RankingReasonVote>lambdaQuery()
                        .eq(RankingReasonVote::getReasonId, reasonId)
                        .eq(RankingReasonVote::getUserId, userId));
        if (existing != null && Objects.equals(existing.getVoteType(), voteType)) {
            return reasonResponse(reasonId, voteType);
        }

        int dAgree = voteType == VOTE_AGREE ? 1 : 0;
        int dOppose = voteType == VOTE_OPPOSE ? 1 : 0;
        int dParticipant = 1;
        int dRankParticipant = 0;
        if (existing != null) {
            if (Objects.equals(existing.getVoteType(), VOTE_AGREE)) {
                dAgree -= 1;
            } else {
                dOppose -= 1;
            }
            dParticipant = 0;
        } else if (!hasAnyVoteInRanking(reason.getRankingId(), userId)) {
            dRankParticipant = 1;
        }

        LocalDateTime now = DateTimeUtils.now();
        try {
            if (existing == null) {
                RankingReasonVote vote = new RankingReasonVote();
                vote.setRankingId(reason.getRankingId());
                vote.setItemId(reason.getItemId());
                vote.setReasonId(reasonId);
                vote.setUserId(userId);
                vote.setVoteType(voteType);
                vote.setCreatedAt(now);
                vote.setUpdatedAt(now);
                rankingReasonVoteMapper.insert(vote);
            } else {
                RankingReasonVote update = new RankingReasonVote();
                update.setId(existing.getId());
                update.setVoteType(voteType);
                update.setUpdatedAt(now);
                rankingReasonVoteMapper.updateById(update);
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "操作过于频繁，请稍后重试");
        }

        rankingReasonMapper.applyVoteDelta(reasonId, dAgree, dOppose, dParticipant);
        updateRankingParticipant(reason.getRankingId(), dRankParticipant);
        return reasonResponse(reasonId, voteType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoteResponse cancelReasonVote(Long reasonId, LoginUser operator) {
        Long userId = requireUserId(operator);
        RankingReason reason = requireNormalReason(reasonId);

        RankingReasonVote existing = rankingReasonVoteMapper.selectOne(
                Wrappers.<RankingReasonVote>lambdaQuery()
                        .eq(RankingReasonVote::getReasonId, reasonId)
                        .eq(RankingReasonVote::getUserId, userId));
        if (existing == null) {
            return reasonResponse(reasonId, null);
        }

        rankingReasonVoteMapper.deleteById(existing.getId());

        int dAgree = Objects.equals(existing.getVoteType(), VOTE_AGREE) ? -1 : 0;
        int dOppose = Objects.equals(existing.getVoteType(), VOTE_OPPOSE) ? -1 : 0;
        rankingReasonMapper.applyVoteDelta(reasonId, dAgree, dOppose, -1);

        if (!hasAnyVoteInRanking(reason.getRankingId(), userId)) {
            updateRankingParticipant(reason.getRankingId(), -1);
        }
        return reasonResponse(reasonId, null);
    }

    // ---------------- 校验 ----------------

    private int requireValidVoteType(Integer voteType) {
        if (voteType == null
                || (voteType != VOTE_AGREE && voteType != VOTE_OPPOSE)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "投票类型只能是 1（认同）或 -1（反对）");
        }
        return voteType;
    }

    private Long requireUserId(LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return operator.getUserId();
    }

    private void requirePublishedRanking(Long rankingId) {
        Ranking ranking = rankingMapper.selectById(rankingId);
        if (ranking == null || ranking.getStatus() == STATUS_DELETED) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "榜单不存在");
        }
        if (ranking.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "榜单未发布");
        }
    }

    private void requireItemOfRanking(Long rankingId, Long itemId) {
        RankingItem item = rankingItemMapper.selectById(itemId);
        if (item == null
                || !Objects.equals(item.getRankingId(), rankingId)
                || !Objects.equals(item.getStatus(), TARGET_NORMAL)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "排名项不存在");
        }
    }

    private RankingReason requireNormalReason(Long reasonId) {
        RankingReason reason = rankingReasonMapper.selectById(reasonId);
        if (reason == null || !Objects.equals(reason.getStatus(), TARGET_NORMAL)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "理由不存在");
        }
        return reason;
    }

    // ---------------- 计数辅助 ----------------

    /**
     * 该用户在榜单下是否已有有效票（票记录均为实体行，存在即有效）
     */
    private boolean hasAnyVoteInRanking(Long rankingId, Long userId) {
        Long itemVotes = rankingItemVoteMapper.selectCount(
                Wrappers.<RankingItemVote>lambdaQuery()
                        .eq(RankingItemVote::getRankingId, rankingId)
                        .eq(RankingItemVote::getUserId, userId));
        if (itemVotes != null && itemVotes > 0) {
            return true;
        }
        Long reasonVotes = rankingReasonVoteMapper.selectCount(
                Wrappers.<RankingReasonVote>lambdaQuery()
                        .eq(RankingReasonVote::getRankingId, rankingId)
                        .eq(RankingReasonVote::getUserId, userId));
        return reasonVotes != null && reasonVotes > 0;
    }

    /**
     * 榜单参与人数原子增减，delta 仅取 -1/0/+1（int 字面量拼接无注入风险），下限 0
     */
    private void updateRankingParticipant(Long rankingId, int delta) {
        if (delta == 0) {
            return;
        }
        rankingMapper.update(null, Wrappers.<Ranking>lambdaUpdate()
                .eq(Ranking::getId, rankingId)
                .setSql("participant_count = GREATEST(participant_count + (" + delta + "), 0)"));
    }

    // ---------------- 响应构建 ----------------

    private VoteResponse itemResponse(Long itemId, Integer myVoteType) {
        RankingItem item = rankingItemMapper.selectById(itemId);
        VoteResponse response = new VoteResponse();
        response.setMyVoteType(myVoteType);
        if (item != null) {
            response.setAgreeCount(item.getAgreeCount());
            response.setOpposeCount(item.getOpposeCount());
            response.setParticipantCount(item.getParticipantCount());
            response.setAgreeRate(item.getAgreeRate());
        }
        return response;
    }

    private VoteResponse reasonResponse(Long reasonId, Integer myVoteType) {
        RankingReason reason = rankingReasonMapper.selectById(reasonId);
        VoteResponse response = new VoteResponse();
        response.setMyVoteType(myVoteType);
        if (reason != null) {
            response.setAgreeCount(reason.getAgreeCount());
            response.setOpposeCount(reason.getOpposeCount());
            response.setParticipantCount(reason.getParticipantCount());
            response.setAgreeRate(reason.getAgreeRate());
        }
        return response;
    }
}
