package com.likebang.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统参数配置表 实体类
 * <p>
 * 通用键值配置中心：把"管理员可调的运营/展示参数"与业务表解耦，
 * 新增一个可配置参数只需插入一行，无需改表结构、无需发版。
 */
@Data
@TableName("sys_config")
public class SysConfig implements Serializable {

    /**
     * 配置ID，Snowflake生成（内置项用固定小ID）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 配置键，三段式命名：module.feature.param
     */
    private String configKey;

    /**
     * 配置值，统一以字符串存储，读取时按 valueType 转换
     */
    private String configValue;

    /**
     * 配置中文名，后台展示用
     */
    private String configName;

    /**
     * 配置分组：ranking/display/system
     */
    private String configGroup;

    /**
     * 值类型：int/boolean/string/json
     */
    private String valueType;

    /**
     * 配置说明，展示在后台表单下方
     */
    private String description;

    /**
     * 是否允许管理员修改：0只读，1可改
     */
    private Integer editable;

    /**
     * 数值型最小值校验（服务端强校验）
     */
    private String minValue;

    /**
     * 数值型最大值校验（服务端强校验）
     */
    private String maxValue;

    /**
     * 同组内展示排序，越小越靠前
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
