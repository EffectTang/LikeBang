package com.likebang.modules.ranking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.ranking.entity.RankingCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排名分类表 Mapper 接口
 */
@Mapper
public interface RankingCategoryMapper extends BaseMapper<RankingCategory> {
}
