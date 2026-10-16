package com.likebang.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 密码重置凭证表 实体类
 * <p>
 * 仅存令牌的 SHA-256 哈希（绝不落明文）；一次性短时效，完成重置或检测到重复使用即作废。
 */
@Data
@TableName("password_reset_token")
public class PasswordResetToken implements Serializable {

    /**
     * 凭证ID，Snowflake生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 目标邮箱（小写归一化，与 sys_user.email 一致）
     */
    private String email;

    /**
     * 重置令牌 SHA-256 十六进制哈希
     */
    private String tokenHash;

    /**
     * 过期时间：申请时间 + 有效期（默认30分钟）
     */
    private LocalDateTime expiryAt;

    /**
     * 核销时间：null=未使用；完成重置或作废时写入。
     * 注意：本表刻意不使用 @TableLogic 软删，used_at 兼作「未使用」标记与审计时间，
     * 语义与全局软删口径不同，勿按软删表习惯改造
     */
    private LocalDateTime usedAt;

    /**
     * 申请来源IP（审计用）
     */
    private String ip;

    /**
     * 申请时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
