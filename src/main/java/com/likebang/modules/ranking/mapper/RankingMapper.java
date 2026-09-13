package com.likebang.modules.ranking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.ranking.entity.Ranking;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排名表 Mapper 接口
 */
@Mapper
public interface RankingMapper extends BaseMapper<Ranking> {
}
