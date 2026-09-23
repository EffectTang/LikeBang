package com.likebang.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.system.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统参数配置表 Mapper 接口
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}
