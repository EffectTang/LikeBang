package com.likebang.modules.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.auth.RequireRole;
import com.likebang.common.dto.PageParam;
import com.likebang.common.result.Result;
import com.likebang.modules.system.dto.request.ConfigUpdateRequest;
import com.likebang.modules.system.dto.response.SysConfigResponse;
import com.likebang.modules.system.service.SysConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统参数配置 接口（管理后台）
 * <p>
 * 全部为管理员专属：既未在 AuthInterceptor 游客只读白名单内（写操作需登录），
 * 又叠加 {@link RequireRole} 角色校验，避免配置裸奔被越权修改。
 */
@RestController
@RequestMapping("/configs")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    /**
     * 分页查询配置项（可按分组过滤，keyword 搜名称/键名）
     */
    @RequireRole
    @GetMapping("/page")
    public Result<IPage<SysConfigResponse>> page(@ModelAttribute PageParam pageParam,
                                                 @RequestParam(required = false) String group) {
        return Result.success(sysConfigService.pageConfigs(pageParam, group));
    }

    /**
     * 修改配置值（仅管理员）
     */
    @RequireRole
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ConfigUpdateRequest request) {
        sysConfigService.updateConfigValue(id, request);
        return Result.success();
    }
}
