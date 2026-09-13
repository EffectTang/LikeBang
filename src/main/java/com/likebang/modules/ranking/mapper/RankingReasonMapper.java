package com.likebang.modules.ranking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.ranking.entity.RankingReason;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排名项理由表 Mapper 接口
 */
@Mapper
public interface RankingReasonMapper extends BaseMapper<RankingReason> {
}
