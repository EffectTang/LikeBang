package com.likebang.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likebang.modules.user.entity.PasswordResetToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 密码重置凭证 Mapper
 */
@Mapper
public interface PasswordResetTokenMapper extends BaseMapper<PasswordResetToken> {
}
