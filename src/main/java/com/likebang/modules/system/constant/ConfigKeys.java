package com.likebang.modules.system.constant;

/**
 * 系统配置键常量
 * <p>
 * 集中登记 {@code sys_config.config_key}，业务侧统一从这里引用键名，
 * 避免字符串字面量散落到各 Service，改键名只需改一处。
 * 每个键都应在 schema.sql 中播种，并配套一个默认值兜底（DB 未初始化/异常时回落）。
 */
public final class ConfigKeys {

    private ConfigKeys() {
    }

    /**
     * 榜单详情页每个排名项的理由展示条数（按认同数降序取 Top N）
     */
    public static final String RANKING_DETAIL_REASON_LIMIT = "ranking.detail.reason_limit";

    /**
     * RANKING_DETAIL_REASON_LIMIT 的兜底默认值
     */
    public static final int DEFAULT_RANKING_DETAIL_REASON_LIMIT = 10;

    /**
     * 一个榜单最多允许上传的封面图数量（详情页轮播展示，后台只配“最多几张”，下限固定为 1）
     */
    public static final String RANKING_DETAIL_COVER_LIMIT = "ranking.detail.cover_limit";

    /**
     * RANKING_DETAIL_COVER_LIMIT 的兜底默认值
     */
    public static final int DEFAULT_RANKING_DETAIL_COVER_LIMIT = 3;

    /**
     * 全站多维搜索“全部”聚合区每一维度（榜单/用户/排名项）展示的条数
     */
    public static final String SEARCH_AGGREGATE_SIZE = "search.aggregate.size";

    /**
     * SEARCH_AGGREGATE_SIZE 的兜底默认值
     */
    public static final int DEFAULT_SEARCH_AGGREGATE_SIZE = 5;
}
