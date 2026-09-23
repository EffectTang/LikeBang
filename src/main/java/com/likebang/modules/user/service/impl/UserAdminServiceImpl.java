package com.likebang.modules.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.likebang.common.auth.LoginUser;
import com.likebang.common.auth.UserRole;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.config.auth.AuthProperties;
import com.likebang.modules.user.dto.UserPageParam;
import com.likebang.modules.user.dto.request.UserCreateRequest;
import com.likebang.modules.user.dto.request.UserUpdateRequest;
import com.likebang.modules.user.dto.response.UserInfoResponse;
import com.likebang.modules.user.entity.SysUser;
import com.likebang.modules.user.mapper.SysUserMapper;
import com.likebang.modules.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * 用户管理服务实现：权限分层与域不变量的唯一收口点
 * <p>
 * 安全不变量（Controller 的 @RequireRole 只挡"能否进门"，以下规则挡"进门后能做什么"）：
 * 1. STAFF 仅能操作普通用户（USER），且角色变更接口对其根本不开放；
 * 2. 内置超管账号（配置 likebang.auth.super-admin-username）任何人不可禁用/删除/改角色，防锁死；
 * 3. 任何人不能禁用/删除/改角色到自己，防误操作自锁；
 * 4. 角色变更、删除均落审计日志（operator → target，旧值 → 新值）。
 * <p>
 * 禁用/删除即时生效：AuthInterceptor 每请求查库校验 status 与逻辑删除，
 * 无需额外的 Token 黑名单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final SysUserMapper sysUserMapper;
    private final AuthProperties authProperties;

    @Override
    public IPage<UserInfoResponse> page(UserPageParam param) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(param.getKeyword())) {
            String kw = param.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw)
                    .or().like(SysUser::getNickname, kw)
                    .or().like(SysUser::getEmail, kw));
        }
        if (param.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, param.getStatus());
        }
        if (param.getRole() != null) {
            wrapper.eq(SysUser::getRole, param.getRole());
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> page = sysUserMapper.selectPage(param.toPage(), wrapper);
        return page.convert(UserInfoResponse::from);
    }

    @Override
    public Long create(UserCreateRequest request, LoginUser operator) {
        // 内置超管账号名保留：禁止通过后台创建同名账号，避免身份混淆/覆盖，防权限体系被夺舍
        String superAdmin = authProperties.getSuperAdminUsername();
        if (StringUtils.hasText(superAdmin) && superAdmin.equals(request.getUsername())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "该用户名为系统保留账号");
        }

        // 用户名唯一（逻辑删除记录也纳入，与注册一致，避免账号身份混乱）
        Long usernameExists = sysUserMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, request.getUsername()));
        if (usernameExists != null && usernameExists > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }
        // 邮箱必填且小写归一化存储，避免大小写重复账号
        String email = request.getEmail().trim().toLowerCase();
        Long emailExists = sysUserMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getEmail, email));
        if (emailExists != null && emailExists > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS.getCode(), "该邮箱已被使用");
        }

        // 初始角色默认普通用户；入口已限 ADMIN，指定管理员/运营不构成 STAFF 提权路径
        int role = request.getRole() == null
                ? UserRole.USER.getCode() : parseStrict(request.getRole()).getCode();
        int status = request.getStatus() == null ? 1 : request.getStatus();

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setPasswordHash(BCrypt.hashpw(request.getPassword()));
        user.setEmail(email);
        user.setPhone(blankToNull(request.getPhone()));
        user.setStatus(status);
        user.setRole(role);
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 应用层查重挡不住并发，且历史逻辑删除记录仍占唯一键：uk_username / uk_email 才是最后防线
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS.getCode(), "用户名或邮箱已存在");
        }

        log.info("[AUDIT] 管理员新增用户: operator={}({}) newId={} username={} role={}",
                operator.getUsername(), operator.getUserId(), user.getId(), user.getUsername(), role);
        return user.getId();
    }

    @Override
    public void update(Long id, UserUpdateRequest request, LoginUser operator) {
        SysUser target = getExistingById(id);
        requireTargetManageable(target, operator);

        SysUser update = new SysUser();
        update.setId(id);
        boolean changed = false;

        if (StringUtils.hasText(request.getNickname())
                && !Objects.equals(request.getNickname(), target.getNickname())) {
            update.setNickname(request.getNickname());
            changed = true;
        }
        if (StringUtils.hasText(request.getEmail())) {
            // 与注册一致：邮箱统一小写归一化，避免大小写重复账号
            String email = request.getEmail().trim().toLowerCase();
            if (!Objects.equals(email, target.getEmail())) {
                update.setEmail(email);
                changed = true;
            }
        }
        if (StringUtils.hasText(request.getPhone())
                && !Objects.equals(request.getPhone(), target.getPhone())) {
            update.setPhone(request.getPhone());
            changed = true;
        }
        if (request.getStatus() != null && !Objects.equals(request.getStatus(), target.getStatus())) {
            if (request.getStatus() == 0) {
                if (isSuperAdmin(target)) {
                    throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "内置超级管理员不可禁用");
                }
                if (Objects.equals(id, operator.getUserId())) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能禁用当前登录账号");
                }
            }
            update.setStatus(request.getStatus());
            changed = true;
        }
        if (!changed) {
            return;
        }

        try {
            sysUserMapper.updateById(update);
        } catch (DuplicateKeyException e) {
            // 应用层未做重名预查（多数请求不改邮箱），唯一键 uk_email 才是并发防线
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS.getCode(), "该邮箱已被其他账号使用");
        }
        log.info("用户信息更新: operator={}, targetId={}, fields={}",
                operator.getUsername(), id, describeChangedFields(update));
    }

    @Override
    public void updateRole(Long id, Integer role, LoginUser operator) {
        UserRole newRole = parseStrict(role);
        SysUser target = getExistingById(id);

        if (Objects.equals(id, operator.getUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能变更当前登录账号的角色");
        }
        if (isSuperAdmin(target)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "内置超级管理员角色不可变更");
        }
        if (UserRole.of(target.getRole()) == newRole) {
            return;
        }

        SysUser update = new SysUser();
        update.setId(id);
        update.setRole(newRole.getCode());
        sysUserMapper.updateById(update);

        // 角色变更属敏感操作，独立审计日志
        log.info("[AUDIT] 角色变更: operator={}({}) target={}({}) role {} -> {}",
                operator.getUsername(), operator.getUserId(),
                target.getUsername(), id, target.getRole(), newRole.getCode());
    }

    @Override
    public void delete(Long id, LoginUser operator) {
        SysUser target = getExistingById(id);
        if (Objects.equals(id, operator.getUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能删除当前登录账号");
        }
        if (isSuperAdmin(target)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "内置超级管理员不可删除");
        }
        sysUserMapper.deleteById(id);
        log.info("[AUDIT] 删除用户: operator={}({}) target={}({})",
                operator.getUsername(), operator.getUserId(), target.getUsername(), id);
    }

    private SysUser getExistingById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 操作人层级规则：管理员可操作所有非超管保护场景（由调用点单独守卫）；
     * 运营管理员（STAFF）仅能操作普通用户，触碰管理员/其他运营一律 403
     */
    private void requireTargetManageable(SysUser target, LoginUser operator) {
        if (operator.isAdmin()) {
            return;
        }
        if (UserRole.of(target.getRole()) != UserRole.USER) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权限操作该用户");
        }
    }

    private boolean isSuperAdmin(SysUser target) {
        String superAdmin = authProperties.getSuperAdminUsername();
        return StringUtils.hasText(superAdmin) && superAdmin.equals(target.getUsername());
    }

    /**
     * 严格解析角色值：不复用 UserRole.of（其对未知值降级为 USER，适合鉴权兜底、不适合入参校验）
     */
    private UserRole parseStrict(Integer code) {
        return Arrays.stream(UserRole.values())
                .filter(r -> code != null && r.getCode() == code)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色取值非法"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String describeChangedFields(SysUser update) {
        StringBuilder sb = new StringBuilder();
        if (update.getNickname() != null) sb.append("nickname,");
        if (update.getEmail() != null) sb.append("email,");
        if (update.getPhone() != null) sb.append("phone,");
        if (update.getStatus() != null) sb.append("status,");
        return sb.toString();
    }
}
