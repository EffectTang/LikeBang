package com.likebang.modules.ranking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.ranking.entity.RankingCover;
import org.apache.ibatis.annotations.Mapper;

/**
 * 榜单封面多图表 Mapper 接口（通用 CRUD 走 BaseMapper，无自定义 SQL）
 */
@Mapper
public interface RankingCoverMapper extends BaseMapper<RankingCover> {
}
