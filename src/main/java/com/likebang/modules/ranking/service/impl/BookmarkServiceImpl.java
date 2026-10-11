package com.likebang.modules.ranking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.common.utils.DateTimeUtils;
import com.likebang.modules.ranking.entity.Ranking;
import com.likebang.modules.ranking.entity.RankingBookmark;
import com.likebang.modules.ranking.mapper.RankingBookmarkMapper;
import com.likebang.modules.ranking.mapper.RankingMapper;
import com.likebang.modules.ranking.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 榜单收藏实现：一人对同一榜单至多一条收藏（uk_ranking_user 兜底），收藏与取消均幂等。
 * <p>
 * 与投票同款防御口径：先查后写挡掉绝大多数重复点击，并发穿透由唯一键报错再捕获视为成功，
 * 前端不会因为重复收藏看到失败提示。
 * 收藏不改动 ranking 任何列（一期无 bookmark_count 冗余列，见 schema.sql 第 23 段说明）。
 */
@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    /** 榜单状态：已发布 */
    private static final int STATUS_PUBLISHED = 1;
    /** 榜单状态：已删除 */
    private static final int STATUS_DELETED = 3;

    private final RankingMapper rankingMapper;
    private final RankingBookmarkMapper rankingBookmarkMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bookmark(Long rankingId, Long userId) {
        requireVisibleRanking(rankingId);

        if (isBookmarked(rankingId, userId)) {
            // 已收藏：幂等成功，不重复插入
            return true;
        }

        LocalDateTime now = DateTimeUtils.now();
        RankingBookmark bookmark = new RankingBookmark();
        bookmark.setRankingId(rankingId);
        bookmark.setUserId(userId);
        bookmark.setCreatedAt(now);
        bookmark.setUpdatedAt(now);
        try {
            rankingBookmarkMapper.insert(bookmark);
        } catch (DuplicateKeyException e) {
            // 同一用户并发重复收藏：唯一键兜底，语义上「已收藏」即成功
            return true;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long rankingId, Long userId) {
        // 刻意不校验榜单状态：即便榜已下架/删除，用户也应能清掉自己的收藏
        rankingBookmarkMapper.delete(Wrappers.<RankingBookmark>lambdaQuery()
                .eq(RankingBookmark::getRankingId, rankingId)
                .eq(RankingBookmark::getUserId, userId));
        return false;
    }

    /**
     * 该用户是否已收藏此榜（走 uk_ranking_user 前缀的存在性查询）
     */
    @Override
    public boolean isBookmarked(Long rankingId, Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = rankingBookmarkMapper.selectCount(Wrappers.<RankingBookmark>lambdaQuery()
                .eq(RankingBookmark::getRankingId, rankingId)
                .eq(RankingBookmark::getUserId, userId));
        return count != null && count > 0;
    }

    @Override
    public long countByUser(Long userId) {
        if (userId == null) {
            return 0L;
        }
        Long count = rankingBookmarkMapper.selectCount(Wrappers.<RankingBookmark>lambdaQuery()
                .eq(RankingBookmark::getUserId, userId));
        return count == null ? 0L : count;
    }

    /**
     * 收藏前置校验：榜存在且未删除、且处于已发布态（与投票同一口径，草稿/下架榜不产生新收藏关系）
     */
    private void requireVisibleRanking(Long rankingId) {
        Ranking ranking = rankingMapper.selectById(rankingId);
        if (ranking == null || ranking.getStatus() == STATUS_DELETED) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "榜单不存在");
        }
        if (ranking.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "榜单未发布");
        }
    }
}
