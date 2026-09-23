package com.likebang.modules.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.auth.LoginUser;
import com.likebang.modules.user.dto.UserPageParam;
import com.likebang.modules.user.dto.request.UserCreateRequest;
import com.likebang.modules.user.dto.request.UserUpdateRequest;
import com.likebang.modules.user.dto.response.UserInfoResponse;

/**
 * 用户管理服务接口（管理后台域，操作对象是"他人"）
 * <p>
 * 与 UserService（注册/登录/self 自助域）按主体拆分：两者受众、鉴权边界、
 * 变更影响完全不同，合并只会让 200 行的"上帝服务"继续膨胀。
 */
public interface UserAdminService {

    /**
     * 分页查询用户（管理员/运营管理员）
     */
    IPage<UserInfoResponse> page(UserPageParam param);

    /**
     * 管理员后台新增用户（仅 ADMIN）。
     * 口令策略与注册同口径；用户名/邮箱唯一性由 Service 查重 + 唯一键兜底；
     * 内置超管账号名保留不可占用；返回新用户 ID。
     */
    Long create(UserCreateRequest request, LoginUser operator);

    /**
     * 编辑用户基础信息与状态（null=不修改）。
     * 运营管理员仅能操作普通用户；超管不可被禁用；不能禁用自己。
     */
    void update(Long id, UserUpdateRequest request, LoginUser operator);

    /**
     * 变更用户角色（仅管理员，Controller 已限；此处守卫超管与自操作）
     */
    void updateRole(Long id, Integer role, LoginUser operator);

    /**
     * 删除用户（逻辑删除，仅管理员；超管与本人不可删）
     */
    void delete(Long id, LoginUser operator);
}
