package com.likebang.modules.ranking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.ranking.entity.RankingReasonVote;
import org.apache.ibatis.annotations.Mapper;

/**
 * 理由投票记录表 Mapper 接口
 */
@Mapper
public interface RankingReasonVoteMapper extends BaseMapper<RankingReasonVote> {
}
