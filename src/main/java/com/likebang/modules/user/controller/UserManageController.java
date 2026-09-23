package com.likebang.modules.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.auth.RequireRole;
import com.likebang.common.auth.UserRole;
import com.likebang.common.result.Result;
import com.likebang.common.utils.UserContext;
import com.likebang.modules.user.dto.UserPageParam;
import com.likebang.modules.user.dto.request.UserCreateRequest;
import com.likebang.modules.user.dto.request.UserUpdateRequest;
import com.likebang.modules.user.dto.request.UserRoleUpdateRequest;
import com.likebang.modules.user.dto.response.UserInfoResponse;
import com.likebang.modules.user.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理 接口（管理后台域）
 * <p>
 * 与 /user/auth（自助域：注册/登录/me）按操作主体拆分，路径 /users 天然不在
 * 免登录白名单内，登录后由 @RequireRole 分层：
 * 查询/编辑 = 管理员 + 运营管理员；新增/角色变更/删除 = 仅管理员。
 * 管理员代建用户（POST /users）仅对 ADMIN 开放，与注册共用同口径口令策略；内置超管账号名保留不可占用。
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserManageController {

    private final UserAdminService userAdminService;

    /**
     * 分页查询用户（关键词匹配用户名/昵称/邮箱，可按状态、角色筛选）
     */
    @RequireRole({UserRole.ADMIN, UserRole.STAFF})
    @GetMapping
    public Result<IPage<UserInfoResponse>> page(@ModelAttribute UserPageParam param) {
        return Result.success(userAdminService.page(param));
    }

    /**
     * 新增用户（仅管理员）：后台代建账号，初始角色/状态可选，返回新用户 ID
     */
    @RequireRole(UserRole.ADMIN)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userAdminService.create(request, UserContext.requireLoginUser()));
    }

    /**
     * 编辑用户基础信息与状态（null=不修改；运营管理员仅能操作普通用户）
     */
    @RequireRole({UserRole.ADMIN, UserRole.STAFF})
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody UserUpdateRequest request) {
        userAdminService.update(id, request, UserContext.requireLoginUser());
        return Result.success();
    }

    /**
     * 变更用户角色（仅管理员，含授予/撤销管理员与运营管理员）
     */
    @RequireRole(UserRole.ADMIN)
    @PatchMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id,
                                   @Valid @RequestBody UserRoleUpdateRequest request) {
        userAdminService.updateRole(id, request.getRole(), UserContext.requireLoginUser());
        return Result.success();
    }

    /**
     * 删除用户（逻辑删除，仅管理员）
     */
    @RequireRole(UserRole.ADMIN)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userAdminService.delete(id, UserContext.requireLoginUser());
        return Result.success();
    }
}
