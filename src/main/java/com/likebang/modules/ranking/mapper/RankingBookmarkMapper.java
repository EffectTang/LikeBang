package com.likebang.modules.ranking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.ranking.entity.RankingBookmark;
import org.apache.ibatis.annotations.Mapper;

/**
 * 榜单收藏表 Mapper 接口
 * <p>
 * 收藏关系的增删与「我是否收藏过」存在性判定都只涉及本表，走 BaseMapper 即可；
 * 「我的收藏」列表是榜单视角的查询（要按收藏时间排序、要过滤已删除榜单、要 total 精确），
 * 一条 join 优于「先查 id 再回表」，故定义在 {@link RankingMapper}（见 resources/mapper/ranking/RankingMapper.xml）。
 */
@Mapper
public interface RankingBookmarkMapper extends BaseMapper<RankingBookmark> {
}
