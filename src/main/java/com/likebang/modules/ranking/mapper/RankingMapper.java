package com.likebang.modules.ranking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
