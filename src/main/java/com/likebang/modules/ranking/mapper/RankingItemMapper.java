package com.likebang.modules.ranking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.ranking.entity.RankingItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排名项表 Mapper 接口
 */
@Mapper
public interface RankingItemMapper extends BaseMapper<RankingItem> {
}
