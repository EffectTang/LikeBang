package com.likebang.modules.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.dto.PageParam;
import com.likebang.modules.system.dto.request.ConfigUpdateRequest;
import com.likebang.modules.system.dto.response.SysConfigResponse;

/**
 * 系统参数配置 业务接口
 * <p>
 * 读接口（{@link #getInt}/{@link #getBoolean}/{@link #get}）面向业务侧，带默认值兜底，
 * 内部走本地缓存；写接口面向管理后台，带服务端校验与缓存失效。
 */
public interface SysConfigService {

    /**
     * 读取 int 型配置；键不存在或值非法时返回 defaultValue（绝不因配置问题影响主流程）
     */
    int getInt(String key, int defaultValue);

    /**
     * 读取 boolean 型配置；键不存在或值非法时返回 defaultValue
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * 读取原始字符串配置；键不存在或值为空时返回 defaultValue
     */
    String get(String key, String defaultValue);

    /**
     * 分页查询配置项（管理后台，可按分组过滤、按名称/键名搜索）
     */
    IPage<SysConfigResponse> pageConfigs(PageParam pageParam, String group);

    /**
     * 修改配置值（仅管理员；服务端按 value_type 与 min/max 校验，写后失效缓存）
     */
    void updateConfigValue(Long id, ConfigUpdateRequest request);
}
