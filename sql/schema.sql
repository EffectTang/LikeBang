-- ============================================================================
-- LikeBang 数据库初始化脚本（一次性执行版）
-- 说明：历史增量迁移（ADD role / openid / intro / source_* / comment_count /
--       image_url / idx_source_name / idx_name 等 ALTER 语句）已全部收敛进
--       下方 CREATE TABLE 终态定义，本脚本对全新库与已建库均可安全重复执行：
--       · CREATE TABLE IF NOT EXISTS —— 已存在则跳过
--       · INSERT IGNORE             —— 依赖唯一键，已存在则跳过
-- ============================================================================
CREATE DATABASE IF NOT EXISTS ranking_platform
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE ranking_platform;
-- 1 用户表 sys_user
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL COMMENT '用户ID，Snowflake生成',
    username VARCHAR(32) NOT NULL COMMENT '登录用户名',
    openid VARCHAR(64) DEFAULT NULL COMMENT '微信小程序 openid，微信登录用户唯一标识；账号密码用户为 NULL',
    nickname VARCHAR(32) NOT NULL COMMENT '用户昵称',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    intro VARCHAR(500) DEFAULT NULL COMMENT '自我介绍（用户可自助填写/清空）',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱，账号密码注册必填且全局唯一；微信登录用户可为空',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码BCrypt哈希',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1正常',
    role TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0普通用户，1管理员，2运营管理员（管理员授权）',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    UNIQUE KEY uk_openid (openid),
    KEY idx_phone (phone),
    KEY idx_created_at (created_at),
    KEY idx_status (status)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户表';
-- 2 排名分类表 ranking_category
CREATE TABLE IF NOT EXISTS ranking_category (
    id BIGINT NOT NULL COMMENT '分类ID，Snowflake生成',
    name VARCHAR(32) NOT NULL COMMENT '分类名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    icon_url VARCHAR(512) DEFAULT NULL COMMENT '分类图标',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1正常',
    source_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否开启元素来源填写：0关闭，1开启（台词/歌词/书摘等摘录型分类）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name),
    KEY idx_status_sort (status, sort)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='排名分类表';
-- 3 核心：排名表 ranking
CREATE TABLE IF NOT EXISTS ranking (
    id BIGINT NOT NULL COMMENT '排名ID，Snowflake生成',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    category_id BIGINT DEFAULT NULL COMMENT '分类ID',
    title VARCHAR(100) NOT NULL COMMENT '排名标题',
    description VARCHAR(1000) DEFAULT NULL COMMENT '排名描述',
    cover_url VARCHAR(512) DEFAULT NULL COMMENT '封面地址',
    item_limit INT NOT NULL DEFAULT 10 COMMENT '最大排名项数量',
    item_count INT NOT NULL DEFAULT 0 COMMENT '当前排名项数量',
    participant_count BIGINT NOT NULL DEFAULT 0 COMMENT '参与人数',
    agree_count BIGINT NOT NULL DEFAULT 0 COMMENT '累计认同数',
    view_count BIGINT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    status TINYINT NOT NULL DEFAULT 0
        COMMENT '状态：0草稿，1正常，2下架，3删除',
    visibility TINYINT NOT NULL DEFAULT 1
        COMMENT '可见性：0私有，1公开，2仅链接可见',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_creator_id (creator_id),
    KEY idx_category_status (category_id, status),
    KEY idx_status_created (status, created_at),
    KEY idx_status_view (status, view_count)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='排名表';
-- 4 排名项 ranking_item
-- idx_name：多维搜索 LIKE 候选集收窄/排序用（前导通配符无法命中 B+Tree，数据量上来后升级全文索引/ES）
CREATE TABLE IF NOT EXISTS ranking_item (
    id BIGINT NOT NULL COMMENT '排名项ID，Snowflake生成',
    ranking_id BIGINT NOT NULL COMMENT '排名ID',
    creator_id BIGINT NOT NULL COMMENT '排名项创建者ID',
    name VARCHAR(200) NOT NULL COMMENT '排名项名称',
    description VARCHAR(1000) DEFAULT NULL COMMENT '排名项描述',
    image_url VARCHAR(512) DEFAULT NULL COMMENT '排名项图片',
    source_type VARCHAR(32) DEFAULT NULL COMMENT '来源类型：MOVIE/TV_DRAMA/MUSIC/BOOK 等，见 SourceTypes 白名单',
    source_name VARCHAR(100) DEFAULT NULL COMMENT '来源作品名称（不带书名号，展示时包《》）',
    source_desc VARCHAR(500) DEFAULT NULL COMMENT '来源补充说明（场景/章节等）',
    current_rank INT NOT NULL DEFAULT 0 COMMENT '当前排名',
    score DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '当前综合得分',
    agree_count BIGINT NOT NULL DEFAULT 0 COMMENT '认同数',
    oppose_count BIGINT NOT NULL DEFAULT 0 COMMENT '反对数',
    participant_count BIGINT NOT NULL DEFAULT 0 COMMENT '参与人数',
    agree_rate DECIMAL(7,4) NOT NULL DEFAULT 0
        COMMENT '认同率：认同数/参与人数，取值0~1',
    reason_count INT NOT NULL DEFAULT 0 COMMENT '理由数量',
    status TINYINT NOT NULL DEFAULT 1
        COMMENT '状态：0删除，1正常，2隐藏',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ranking_name (ranking_id, name),
    KEY idx_ranking_rank (ranking_id, current_rank),
    KEY idx_ranking_score (ranking_id, score),
    KEY idx_ranking_status (ranking_id, status),
    KEY idx_creator_id (creator_id),
    KEY idx_source_name (source_name),
    KEY idx_name (name)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='排名项表';
-- 5 理由表 ranking_reason
CREATE TABLE IF NOT EXISTS ranking_reason (
    id BIGINT NOT NULL COMMENT '理由ID，Snowflake生成',
    ranking_id BIGINT NOT NULL COMMENT '排名ID',
    item_id BIGINT NOT NULL COMMENT '排名项ID',
    creator_id BIGINT NOT NULL COMMENT '理由创建者ID',
    content VARCHAR(1000) NOT NULL COMMENT '理由内容',
    image_url VARCHAR(512) DEFAULT NULL COMMENT '理由配图（一期单图，存 /uploads/ 相对路径）',
    agree_count BIGINT NOT NULL DEFAULT 0 COMMENT '认同数',
    oppose_count BIGINT NOT NULL DEFAULT 0 COMMENT '反对数',
    participant_count BIGINT NOT NULL DEFAULT 0 COMMENT '参与人数',
    agree_rate DECIMAL(7,4) NOT NULL DEFAULT 0
        COMMENT '认同率：认同数/参与人数，取值0~1',
    current_rank INT NOT NULL DEFAULT 0 COMMENT '理由当前排名',
    score DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '理由综合得分',
    comment_count INT NOT NULL DEFAULT 0 COMMENT '评论数',
    status TINYINT NOT NULL DEFAULT 1
        COMMENT '状态：0删除，1正常，2隐藏',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_item_rank (item_id, current_rank),
    KEY idx_item_score (item_id, score),
    KEY idx_ranking_item (ranking_id, item_id),
    KEY idx_creator_id (creator_id),
    KEY idx_status_created (status, created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='排名项理由表';
-- 6 排名项投票表 ranking_item_vote
CREATE TABLE IF NOT EXISTS ranking_item_vote (
    id BIGINT NOT NULL COMMENT '投票记录ID，Snowflake生成',
    ranking_id BIGINT NOT NULL COMMENT '排名ID',
    item_id BIGINT NOT NULL COMMENT '排名项ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    vote_type TINYINT NOT NULL COMMENT '投票类型：1认同，-1反对',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_user (item_id, user_id),
    KEY idx_ranking_item (ranking_id, item_id),
    KEY idx_user_id (user_id),
    KEY idx_ranking_user (ranking_id, user_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='排名项投票记录表';
-- 7 理由投票表 ranking_reason_vote
CREATE TABLE IF NOT EXISTS ranking_reason_vote (
    id BIGINT NOT NULL COMMENT '投票记录ID，Snowflake生成',
    ranking_id BIGINT NOT NULL COMMENT '排名ID',
    item_id BIGINT NOT NULL COMMENT '排名项ID',
    reason_id BIGINT NOT NULL COMMENT '理由ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    vote_type TINYINT NOT NULL COMMENT '投票类型：1认同，-1反对',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reason_user (reason_id, user_id),
    KEY idx_ranking_item_reason (ranking_id, item_id, reason_id),
    KEY idx_user_id (user_id),
    KEY idx_ranking_user (ranking_id, user_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='理由投票记录表';
-- 8 理由评论表 ranking_reason_comment
-- 评论挂在"理由"之下：理由即排名项级观点跟帖，评论是理由下的楼中楼讨论（表态定立场，评论讲论据）。
-- 一期单层平铺无嵌套（reply_to_id 二期按需在线加列）；不上 target_type 多态表，延续本项目专表+全链路冗余风格
CREATE TABLE IF NOT EXISTS ranking_reason_comment (
    id BIGINT NOT NULL COMMENT '评论ID，Snowflake生成',
    ranking_id BIGINT NOT NULL COMMENT '排名ID（冗余，鉴权/级联自包含）',
    item_id BIGINT NOT NULL COMMENT '排名项ID（冗余，同 ranking_reason_vote 风格）',
    reason_id BIGINT NOT NULL COMMENT '所属理由ID',
    creator_id BIGINT NOT NULL COMMENT '评论者ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容（比理由短，防长文刷屏）',
    like_count BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数（预留列，评论点赞二期随 vote 表一并实现）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0删除，1正常，2隐藏',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    -- 主查询：某理由下评论流，时间正序（贴吧楼层习惯）
    KEY idx_reason_status_created (reason_id, status, created_at),
    KEY idx_ranking_id (ranking_id),
    KEY idx_creator_id (creator_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='理由评论表';
-- 9 榜单封面多图表 ranking_cover
-- 延续“专表 + 冗余”原则：不在 ranking.cover_url 单列上塞 JSON 多图列表。
-- cover_url 仍保留且恒等于第 1 张封面（列表卡片单图视角不变），本表存全量供详情页轮播。
-- 封面是榜单私有轻量媒体引用（无计数/投票依赖），编辑即整组删旧插新（硬删），不做软删
CREATE TABLE IF NOT EXISTS ranking_cover (
    id BIGINT NOT NULL COMMENT '封面ID，Snowflake生成',
    ranking_id BIGINT NOT NULL COMMENT '排名ID',
    image_url VARCHAR(512) NOT NULL COMMENT '封面图地址（/uploads/ 相对路径）',
    sort INT NOT NULL DEFAULT 0 COMMENT '展示顺序，越小越靠前；第 1 张同步写回 ranking.cover_url',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ranking_sort (ranking_id, sort)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='榜单封面多图表';
-- 10 我的收藏：榜单收藏关系表 ranking_bookmark
-- 收藏是「用户 × 榜单」的二元关系，延续本项目「专表 + 全链路冗余」风格，不上 target_type 多态竖表：
-- 将来要收藏排名项/用户时各自新增专表即可，不必为尚不存在的诉求先付动态表单与聚合查询的成本。
-- 刻意不加 ranking.bookmark_count 冗余计数列：一期无任何消费方（热门榜未立项），
-- 待需要时按 RankingItemMapper.applyVoteDelta 的「单条 SQL 原子增减 + GREATEST 下限」口径补列，
-- 避免留下“写了没人读”的字段（同 score/current_rank 的历史教训）。
-- 取消收藏为物理删除：收藏行不承载投票/审计语义，同 ranking_cover 硬删口径；
-- 榜单被软删（status=3）时收藏行保留（榜单恢复即可继续用），由列表查询在 SQL 层过滤掉。
-- 索引说明：uk_ranking_user 前缀已覆盖「按 ranking_id 查我是否收藏过」，故不再单建 idx_ranking_id（避免冗余索引）；
-- idx_user_created 让「我的收藏按收藏时间倒序」分页直接走索引定位 + 索引序，无 filesort。
CREATE TABLE IF NOT EXISTS ranking_bookmark (
    id BIGINT NOT NULL COMMENT '收藏ID，Snowflake生成',
    ranking_id BIGINT NOT NULL COMMENT '排名ID',
    user_id BIGINT NOT NULL COMMENT '收藏者ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（即收藏时间，我的收藏按其倒序）',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ranking_user (ranking_id, user_id),
    KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='榜单收藏表';
-- 11 找回密码：重置凭证专表 password_reset_token
-- 延续「专表设计」：不在 sys_user 上塞 reset_token 单列（历史凭证无处留存、并发申请互相覆盖难审计）。
-- 安全口径：
--   1) 仅存 token 的 SHA-256 哈希，绝不存明文——库表泄露不等于重置链接泄露；
--   2) 一次性使用：完成重置或检测到重复使用时，同邮箱其它未用凭证一律作废（used_at 标记，硬删语义之外的归档保留）；
--   3) 短时效由业务侧 expiry_at 控制（默认 30 分钟，可配），到期自动失效无需清理任务；
--   4) 重置完成即修改密码，旧链接连用一次的机会。
-- 不做软删：凭证行无业务实体语义，与 ranking_cover 硬删口径一致
CREATE TABLE IF NOT EXISTS password_reset_token (
    id BIGINT NOT NULL COMMENT '凭证ID，Snowflake生成',
    email VARCHAR(128) NOT NULL COMMENT '目标邮箱（小写归一化，与 sys_user.email 一致）',
    token_hash CHAR(64) NOT NULL COMMENT '重置令牌 SHA-256 十六进制哈希，不存明文',
    expiry_at DATETIME NOT NULL COMMENT '过期时间：申请时间 + 有效期（默认30分钟）',
    used_at DATETIME DEFAULT NULL COMMENT '核销时间：NULL=未使用；完成重置或作废时写入',
    ip VARCHAR(64) DEFAULT NULL COMMENT '申请来源IP（审计用，X-Forwarded-For 优先）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_token_hash (token_hash),
    KEY idx_email_unused (email, used_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='密码重置凭证表（仅存令牌哈希，一次性短时效）';
-- 12 系统参数配置表 sys_config
-- 通用键值配置中心：把“管理员可调的运营/展示参数”与业务表解耦。
-- 新增一个可配置参数只需 INSERT 一行，无需改表结构、无需发版（区别于 ranking.item_limit 这类业务实体字段）。
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT NOT NULL COMMENT '配置ID，Snowflake生成（内置项用固定小ID）',
    config_key VARCHAR(128) NOT NULL COMMENT '配置键，三段式命名：module.feature.param',
    config_value VARCHAR(512) NOT NULL COMMENT '配置值，统一以字符串存储，读取时按 value_type 转换',
    config_name VARCHAR(128) NOT NULL COMMENT '配置中文名，后台展示用',
    config_group VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '配置分组：ranking/display/system',
    value_type VARCHAR(16) NOT NULL DEFAULT 'string' COMMENT '值类型：int/boolean/string/json',
    description VARCHAR(255) DEFAULT NULL COMMENT '配置说明，展示在后台表单下方',
    editable TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许管理员修改：0只读，1可改',
    min_value VARCHAR(32) DEFAULT NULL COMMENT '数值型最小值校验（服务端强校验）',
    max_value VARCHAR(32) DEFAULT NULL COMMENT '数值型最大值校验（服务端强校验）',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同组内展示排序，越小越靠前',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key),
    KEY idx_group_sort (config_group, sort_order)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统参数配置表';
-- ============================ 种子数据（均幂等，可重复执行） ============================
-- 13 初始化分类数据（依赖 uk_name 唯一键，INSERT IGNORE 跳过已存在项）
-- 摘录型分类（书籍/电影/音乐）直接带 source_enabled=1，对应工作台原“步骤18播种+UPDATE补开”
INSERT IGNORE INTO ranking_category (id, name, description, sort, status, source_enabled) VALUES
    (1001, '书籍',   '读书、小说、专业书等相关榜单', 1, 1, 1),
    (1002, '电影',   '电影、剧集、纪录片等榜单',     2, 1, 1),
    (1003, '音乐',   '歌曲、专辑、歌手等榜单',       3, 1, 1),
    (1004, '游戏',   '端游、手游、主机游戏等榜单',   4, 1, 0),
    (1005, '美食',   '餐厅、菜品、零食等榜单',       5, 1, 0),
    (1006, '旅行',   '城市、景点、路线等榜单',       6, 1, 0),
    (1007, '科技',   '产品、框架、工具等榜单',       7, 1, 0),
    (1008, '其他',   '未归类的主题榜单',             99, 1, 0);
-- 13.1 兜底补开来源开关：旧库若已存在 1001~1003 且上方 IGNORE 跳过，此处补齐（幂等，无副作用）
UPDATE ranking_category SET source_enabled = 1 WHERE id IN (1001, 1002, 1003) AND source_enabled = 0;
-- 14 内置超级管理员（幂等：依赖 uk_username）。以下为开发环境默认引导账号，生产上线务必重置密码与邮箱
-- password_hash 由与登录一致的 Hutool BCrypt（$2a$ 前缀）生成
INSERT IGNORE INTO sys_user
    (id, username, nickname, email, password_hash, status, role)
VALUES
    (1, 'admin', '超级管理员', 'admin@likebang.local',
     '$2a$10$l0akf4AwJv2vSqGce4FOGuC174SMiXwBf0.BnEMMyI8TDSZPr.unm', 1, 1);
-- 15 播种：榜单详情理由展示条数（幂等：依赖 uk_config_key，重复执行自动跳过）
-- 业务侧读取 RankingServiceImpl.detail()：sysConfigService.getInt("ranking.detail.reason_limit", 10)
INSERT IGNORE INTO sys_config
    (id, config_key, config_value, config_name, config_group, value_type,
     description, editable, min_value, max_value, sort_order)
VALUES
    (2001, 'ranking.detail.reason_limit', '10', '榜单详情理由展示条数', 'ranking', 'int',
     '每个排名项在榜单详情页最多展示的理由条数（按认同数降序取 Top N）', 1, '1', '50', 10);
-- 16 播种：榜单封面最大数量上限（幂等：依赖 uk_config_key，重复执行自动跳过）
-- 后台“系统设置”仅设置“最多几张”；用户至少可上传 1 张，下限固定为 1 不可配
-- 业务侧读取 RankingServiceImpl：sysConfigService.getInt("ranking.detail.cover_limit", 3)
INSERT IGNORE INTO sys_config
    (id, config_key, config_value, config_name, config_group, value_type,
     description, editable, min_value, max_value, sort_order)
VALUES
    (2002, 'ranking.detail.cover_limit', '3', '榜单封面最大数量', 'ranking', 'int',
     '一个榜单最多允许上传的封面图数量（详情页轮播展示；下限固定为 1，取值范围 1~10）', 1, '1', '10', 20);
-- 17 播种：全站搜索“全部”聚合区每维展示条数（幂等：依赖 uk_config_key，重复执行自动跳过）
-- 后台“系统设置”可调；业务侧读取 SearchServiceImpl.aggregate()：sysConfigService.getInt("search.aggregate.size", 5)
INSERT IGNORE INTO sys_config
    (id, config_key, config_value, config_name, config_group, value_type,
     description, editable, min_value, max_value, sort_order)
VALUES
    (2003, 'search.aggregate.size', '5', '搜索聚合区每维条数', 'search', 'int',
     '全站搜索“全部”模式下，榜单/用户/排名项每一维最多展示的条数（取值范围 1~20）', 1, '1', '20', 30);
