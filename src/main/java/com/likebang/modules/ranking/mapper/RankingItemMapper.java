package com.likebang.modules.ranking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.ranking.entity.RankingItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 排名项表 Mapper 接口
 */
@Mapper
public interface RankingItemMapper extends BaseMapper<RankingItem> {

    /**
     * 投票计数原子落账：按 delta（-1/0/+1）增减认同/反对/参与数并同步重算认同率，
     * 见 resources/mapper/ranking/RankingItemMapper.xml
     */
    int applyVoteDelta(@Param("itemId") Long itemId,
                       @Param("dAgree") int dAgree,
                       @Param("dOppose") int dOppose,
                       @Param("dParticipant") int dParticipant);

    /**
     * 站内历史来源作品名去重列表（创建表单自动补全），LIMIT 钳制防拖库，
     * 见 resources/mapper/ranking/RankingItemMapper.xml
     */
    List<String> selectDistinctSourceNames(@Param("keyword") String keyword, @Param("limit") int limit);
}
