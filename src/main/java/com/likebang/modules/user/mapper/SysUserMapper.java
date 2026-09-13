package com.likebang.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户表 Mapper 接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
