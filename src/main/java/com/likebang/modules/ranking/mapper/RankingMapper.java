package com.likebang.modules.ranking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.modules.ranking.entity.Ranking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 排名表 Mapper 接口
 */
@Mapper
public interface RankingMapper extends BaseMapper<Ranking> {

    /**
     * 浏览量原子自增，避免读改写并发覆盖
     */
    int incrementViewCount(@Param("id") Long id);

    /**
     * 我创建的排名项/理由收到的他人票数（排名项票 + 理由票，排除自投、仅统计有效内容）。
     * 见 resources/mapper/ranking/RankingMapper.xml
     */
    Long countReceivedVotes(@Param("userId") Long userId);

    /**
     * 我参与投票过的榜单数（按 ranking_item_vote 去重 ranking_id）。
     * 见 resources/mapper/ranking/RankingMapper.xml
     */
    Long countVotedRankings(@Param("userId") Long userId);

    /**
     * 我参与投票的榜单分页（去重 ranking_id、排除已删除，时间倒序），供「我的空间-动态」用。
     * 见 resources/mapper/ranking/RankingMapper.xml
     */
    IPage<Ranking> selectVotedRankingPage(IPage<Ranking> page, @Param("userId") Long userId);

    /**
     * 公开榜单分页搜索：在标题/描述之外，额外 LEFT JOIN sys_user 命中「发起人昵称」维度。
     * 昵称匹配仅对有有效账号（未删除、未禁用）的发起人生效；见 resources/mapper/ranking/RankingMapper.xml
     * creatorNickname 为可选二级过滤（与 keyword AND 叠加）
     */
    IPage<Ranking> selectPublicPageByKeyword(IPage<Ranking> page,
                                             @Param("categoryId") Long categoryId,
                                             @Param("keyword") String keyword,
                                             @Param("creatorNickname") String creatorNickname);
}
