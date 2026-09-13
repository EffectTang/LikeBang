package com.likebang.modules.ranking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.ranking.entity.RankingItemVote;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排名项投票记录表 Mapper 接口
 */
@Mapper
public interface RankingItemVoteMapper extends BaseMapper<RankingItemVote> {
}
