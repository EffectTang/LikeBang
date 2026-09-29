package com.likebang.modules.ranking.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 排名项来源类型常量
 * <p>
 * 摘录型榜单元素（台词/歌词/书摘等）的出处作品类型白名单。
 * 库内存 key（英文），展示名 label（中文）由 {@code GET /rankings/source-types} 接口下发，
 * 前端不硬编码选项，新增类型只需在此处扩展。
 */
public final class SourceTypes {

    private SourceTypes() {
    }

    /** key -> 展示名，LinkedHashMap 保持下拉选项顺序 */
    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put("MOVIE", "电影");
        LABELS.put("TV_DRAMA", "电视剧");
        LABELS.put("MUSIC", "音乐");
        LABELS.put("BOOK", "书籍");
        LABELS.put("VARIETY", "综艺");
        LABELS.put("GAME", "游戏");
        LABELS.put("OTHER", "其他");
    }

    /**
     * 是否为合法来源类型（null/空串视为"未填写"，由调用方先行过滤）
     */
    public static boolean isValid(String type) {
        return type != null && LABELS.containsKey(type);
    }

    /**
     * 全部可选类型：key -> 展示名
     */
    public static Map<String, String> labels() {
        return LABELS;
    }
}
