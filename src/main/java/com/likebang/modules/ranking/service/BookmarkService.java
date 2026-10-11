package com.likebang.modules.ranking.service;

/**
 * 榜单收藏服务接口：收藏 / 取消收藏 / 收藏态与收藏数查询
 * <p>
 * 独立于 RankingService：收藏是用户侧行为关系，不触碰榜单元数据与内容结构，
 * 拆开后 RankingServiceImpl（榜单读写主体）不再继续膨胀，口径也更容易单独演进
 * （如后续收藏提醒、收藏数热度均只改这里）。「我的收藏」列表是榜单视角的读查询，
 * 与「我投票的榜单」同构，故留在 {@link RankingService#pageBookmarkedByMe}。
 */
public interface BookmarkService {

    /**
     * 收藏榜单（幂等：已收藏直接返回成功，不重复插入）
     *
     * @return 收藏后的状态，恒为 true（供前端把按钮置为「已收藏」）
     */
    boolean bookmark(Long rankingId, Long userId);

    /**
     * 取消收藏（幂等：无收藏行也返回成功，不做存在性前置校验）
     *
     * @return 取消后的状态，恒为 false
     */
    boolean cancel(Long rankingId, Long userId);

    /**
     * 该用户是否已收藏此榜（详情页回填收藏态用；无登录上下文时由调用方自行短路）
     */
    boolean isBookmarked(Long rankingId, Long userId);

    /**
     * 该用户的收藏总数（「我的空间」概览栏，与「我的收藏」列表同一张表口径）
     */
    long countByUser(Long userId);
}
