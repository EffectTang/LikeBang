package com.likebang.modules.system.service.impl;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.likebang.common.dto.PageParam;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.common.utils.DateTimeUtils;
import com.likebang.modules.system.dto.request.ConfigUpdateRequest;
import com.likebang.modules.system.dto.response.SysConfigResponse;
import com.likebang.modules.system.entity.SysConfig;
import com.likebang.modules.system.mapper.SysConfigMapper;
import com.likebang.modules.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统参数配置 业务实现
 * <p>
 * 读走 Hutool {@link TimedCache} 本地缓存（TTL 5 分钟），miss 回源 DB；写后主动失效对应键。
 * 单节点部署下即可满足一致性与"改后即时生效"；若后续多节点水平扩容，
 * 只需在写操作后追加一次广播（Redis Pub/Sub / MQ）通知各节点失效缓存，读逻辑无需改动。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    /** 缓存过期时间：5 分钟，兼顾"改后及时生效"与"避免每次详情都查库" */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    private static final String TYPE_INT = "int";
    private static final String TYPE_BOOLEAN = "boolean";

    private final SysConfigMapper configMapper;

    /**
     * 键 → 原始字符串值 的本地缓存。注意：仅缓存 DB 中真实存在的值，
     * 未命中/回落默认值不写缓存（配置项数量有限，穿透成本可忽略）。
     */
    private final TimedCache<String, String> valueCache = CacheUtil.newTimedCache(CACHE_TTL_MS);

    @Override
    public int getInt(String key, int defaultValue) {
        String raw = get(key, null);
        if (StrUtil.isBlank(raw)) {
            return defaultValue;
        }
        if (!NumberUtil.isInteger(raw.trim())) {
            log.warn("系统配置[{}]值非法(非整数)：{}，回落默认值：{}", key, raw, defaultValue);
            return defaultValue;
        }
        return Integer.parseInt(raw.trim());
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String raw = get(key, null);
        if (StrUtil.isBlank(raw)) {
            return defaultValue;
        }
        if (!isBooleanLiteral(raw.trim())) {
            log.warn("系统配置[{}]值非法(非布尔)：{}，回落默认值：{}", key, raw, defaultValue);
            return defaultValue;
        }
        return BooleanUtil.toBoolean(raw.trim().toLowerCase());
    }

    @Override
    public String get(String key, String defaultValue) {
        if (StrUtil.isBlank(key)) {
            return defaultValue;
        }
        // get(key, false)：读不刷新访问时间，保证 TTL 相对"写入时刻"计算，最长 5 分钟后必然回源刷新
        String cached = valueCache.get(key, false);
        if (cached != null) {
            return cached;
        }
        SysConfig config = configMapper.selectOne(
                Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, key));
        if (config == null || StrUtil.isBlank(config.getConfigValue())) {
            return defaultValue;
        }
        valueCache.put(key, config.getConfigValue());
        return config.getConfigValue();
    }

    @Override
    public IPage<SysConfigResponse> pageConfigs(PageParam pageParam, String group) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(group)) {
            wrapper.eq(SysConfig::getConfigGroup, group);
        }
        if (StrUtil.isNotBlank(pageParam.getKeyword())) {
            String keyword = pageParam.getKeyword();
            wrapper.and(w -> w.like(SysConfig::getConfigName, keyword)
                    .or().like(SysConfig::getConfigKey, keyword));
        }
        wrapper.orderByAsc(SysConfig::getConfigGroup)
                .orderByAsc(SysConfig::getSortOrder)
                .orderByAsc(SysConfig::getId);

        // 统一经 PageParam.toPage() 构建：非法页码兜底 + size 上限钳制
        Page<SysConfig> page = configMapper.selectPage(pageParam.toPage(), wrapper);
        return page.convert(this::toResponse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigValue(Long id, ConfigUpdateRequest request) {
        SysConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "配置项不存在");
        }
        if (config.getEditable() != null && config.getEditable() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(),
                    "配置项「" + config.getConfigName() + "」为只读，不允许修改");
        }

        String value = request.getConfigValue() == null ? null : request.getConfigValue().trim();
        validate(config, value);

        // 局部更新：只写 value 与更新时间，避免整档回写覆盖并发修改
        SysConfig update = new SysConfig();
        update.setId(id);
        update.setConfigValue(value);
        update.setUpdatedAt(DateTimeUtils.now());
        configMapper.updateById(update);

        // 写后失效，下次读穿透回源，保证管理页改后即时生效
        valueCache.remove(config.getConfigKey());
    }

    /**
     * 服务端强校验：非空 + 按 value_type 校验类型与 min/max 范围（前端约束仅为体验，不可依赖）
     */
    private void validate(SysConfig config, String value) {
        if (StrUtil.isBlank(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "配置值不能为空");
        }
        String type = config.getValueType();
        if (TYPE_INT.equalsIgnoreCase(type)) {
            if (!NumberUtil.isLong(value)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        config.getConfigName() + " 必须为整数");
            }
            long num = Long.parseLong(value);
            Long min = parseBound(config.getMinValue());
            Long max = parseBound(config.getMaxValue());
            if (min != null && num < min) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        config.getConfigName() + " 不能小于 " + min);
            }
            if (max != null && num > max) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        config.getConfigName() + " 不能大于 " + max);
            }
        } else if (TYPE_BOOLEAN.equalsIgnoreCase(type)) {
            if (!isBooleanLiteral(value)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        config.getConfigName() + " 必须为布尔值(true/false)");
            }
        }
        // string/json 一期不做进一步格式校验
    }

    /**
     * 严格布尔字面量校验：仅接受 true/false（忽略大小写）
     */
    private boolean isBooleanLiteral(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private Long parseBound(String bound) {
        if (StrUtil.isBlank(bound) || !NumberUtil.isLong(bound)) {
            return null;
        }
        return Long.parseLong(bound);
    }

    private SysConfigResponse toResponse(SysConfig source) {
        SysConfigResponse target = new SysConfigResponse();
        BeanUtils.copyProperties(source, target);
        return target;
    }
}
