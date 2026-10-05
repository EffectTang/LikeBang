package com.likebang.modules.search.constant;

/**
 * 全站搜索维度类型。
 * <p>
 * {@code ALL} 走聚合导航（每维取前 N 条、不分页）；其余为单维分页搜索，委托对应域接口。
 * 前端传参以名匹配（大小写不敏感），非法值由 {@link #parse} 归一为 {@code null} 交由上层兜底为 ALL。
 */
public enum SearchType {

    /** 聚合：榜单 + 用户 + 排名项 + 作品名 */
    ALL,
    /** 榜单（标题/描述/发起人昵称） */
    RANKING,
    /** 用户（昵称） */
    USER,
    /** 排名项（名称/描述/来源作品名） */
    ITEM,
    /** 来源作品名（去重补全） */
    SOURCE;

    public static SearchType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        try {
            return SearchType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }
}
