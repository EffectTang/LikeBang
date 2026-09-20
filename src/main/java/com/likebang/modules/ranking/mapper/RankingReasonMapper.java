package com.likebang.modules.ranking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.ranking.entity.RankingReason;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 排名项理由表 Mapper 接口
 */
@Mapper
public interface RankingReasonMapper extends BaseMapper<RankingReason> {

    /**
     * 理由投票计数原子落账，规则同 {@link RankingItemMapper#applyVoteDelta}
     */
    int applyVoteDelta(@Param("reasonId") Long reasonId,
                       @Param("dAgree") int dAgree,
                       @Param("dOppose") int dOppose,
                       @Param("dParticipant") int dParticipant);
}
