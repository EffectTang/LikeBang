package com.likebang.modules.user.dto.response;

import com.likebang.modules.user.entity.SysUser;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应（不含密码等敏感字段，可返回给前端）
 */
@Data
public class UserInfoResponse {

    private Long id;

    private String username;

    private String nickname;

    private String avatarUrl;

    private String email;

    private String phone;

    private Integer status;

    /**
     * 角色：0普通用户，1管理员，2运营管理员（管理员授权）
     */
    private Integer role;

    private LocalDateTime createdAt;

    /**
     * 最后登录时间（管理后台用户列表展示）
     */
    private LocalDateTime lastLoginAt;

    public static UserInfoResponse from(SysUser user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginAt(user.getLastLoginAt());
        return response;
    }
}
