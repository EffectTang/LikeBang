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

  