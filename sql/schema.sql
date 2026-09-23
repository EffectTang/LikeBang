CREATE DATABASE IF NOT EXISTS ranking_platform
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE ranking_platform;

-- 1 用户表 sys_user
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL COMMENT '用户ID，Snowflake生成',

    username VARCHAR(32) NOT NULL COMMENT '登录用户名',
    nickname VARCHAR(32) NOT NULL COMMENT '用户昵称',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像地址',

    email VARCHAR(128) NOT NULL COMMENT '邮箱，注册必填，全局唯一',
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
CREATE TABLE IF NOT EXISTS ranking_item (
id BIGINT NOT NULL COMMENT '排名项ID，Snowflake生成',

ranking_id BIGINT NOT NULL COMMENT '排名ID',
creator_id BIGINT NOT NULL COMMENT '排名项创建者ID',

name VARCHAR(200) NOT NULL COMMENT '排名项名称',
description VARCHAR(1000) DEFAULT NULL COMMENT '排名项描述',
image_url VARCHAR(512) DEFAULT NULL COMMENT '排名项图片',

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
KEY idx_creator_id (creator_id)
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

-- 9 初始化分类数据（参考数据，可重复执行：依赖 uk_name 唯一键，INSERT IGNORE 跳过已存在项）
INSERT IGNORE INTO ranking_category (id, name, description, sort, status) VALUES
    (1001, '书籍',   '读书、小说、专业书等相关榜单', 1, 1),
    (1002, '电影',   '电影、剧集、纪录片等榜单',     2, 1),
    (1003, '音乐',   '歌曲、专辑、歌手等榜单',       3, 1),
    (1004, '游戏',   '端游、手游、主机游戏等榜单',   4, 1),
    (1005, '美食',   '餐厅、菜品、零食等榜单',       5, 1),
    (1006, '旅行',   '城市、景点、路线等榜单',       6, 1),
    (1007, '科技',   '产品、框架、工具等榜单',       7, 1),
    (1008, '其他',   '未归类的主题榜单',             99, 1);

-- 10 迁移：为已存在的旧 sys_user 表补充 role 列（新建库由上方 CREATE 已含此列，执行本行报错可忽略）
-- 注意：MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，此语句为一次性迁移，仅需执行一次
 ALTER TABLE sys_user ADD COLUMN role TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0普通用户，1管理员' AFTER status;

-- 11 内置超级管理员（幂等：依赖 uk_username）。以下为开发环境默认引导账号，生产上线务必重置密码与邮箱
-- password_hash 由与登录一致的 Hutool BCrypt（$2a$ 前缀）生成
INSERT IGNORE INTO sys_user
    (id, username, nickname, email, password_hash, status, role)
VALUES
    (1, 'admin', '超级管理员', 'admin@likebang.local',
     '$2a$10$l0akf4AwJv2vSqGce4FOGuC174SMiXwBf0.BnEMMyI8TDSZPr.unm', 1, 1);

-- 12 迁移：role 注释补充 2=运营管理员（仅改注释不改存储，一次性执行，已执行过可忽略报错）
ALTER TABLE sys_user MODIFY COLUMN role TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0普通用户，1管理员，2运营管理员（管理员授权）';

-- 13 迁移：为已存在的旧 ranking_reason 表补评论数列（新库由上方 CREATE 已含此列，执行本行报错可忽略；一次性迁移）
ALTER TABLE ranking_reason
    ADD COLUMN comment_count INT NOT NULL DEFAULT 0 COMMENT '评论数' AFTER score;

-- 14 系统参数配置表 sys_config
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

-- 14.1 播种：榜单详情理由展示条数（幂等：依赖 uk_config_key，重复执行自动跳过）
-- 业务侧读取 RankingServiceImpl.detail()：sysConfigService.getInt("ranking.detail.reason_limit", 10)
INSERT IGNORE INTO sys_config
    (id, config_key, config_value, config_name, config_group, value_type,
     description, editable, min_value, max_value, sort_order)
VALUES
    (2001, 'ranking.detail.reason_limit', '10', '榜单详情理由展示条数', 'ranking', 'int',
     '每个排名项在榜单详情页最多展示的理由条数（按认同数降序取 Top N）', 1, '1', '50', 10);


  