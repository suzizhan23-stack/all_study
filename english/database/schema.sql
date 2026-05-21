-- ============================================================
-- 英语单词知识库 — 一键建库 v8
-- 36 表 | UUID 主键 | 全表 created_at/updated_at
-- 新增：default_strategy_id, user_daily_plan_entries
-- ============================================================

CREATE DATABASE IF NOT EXISTS word_learning
  DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE word_learning;

-- ============================================================
-- 清理（按依赖顺序，叶子先删）
-- ============================================================
DROP TABLE IF EXISTS user_daily_plan_entries;
DROP TABLE IF EXISTS daily_plan_items;
DROP TABLE IF EXISTS user_word_book_progress;
DROP TABLE IF EXISTS word_book_entries;
DROP TABLE IF EXISTS word_books;
DROP TABLE IF EXISTS study_strategies;
DROP TABLE IF EXISTS user_badges;
DROP TABLE IF EXISTS user_entity_tags;
DROP TABLE IF EXISTS user_plans;
DROP TABLE IF EXISTS daily_recommendations;
DROP TABLE IF EXISTS reading_progress;
DROP TABLE IF EXISTS content_ratings;
DROP TABLE IF EXISTS search_history;
DROP TABLE IF EXISTS review_log;
DROP TABLE IF EXISTS learning_activities;
DROP TABLE IF EXISTS user_notes;
DROP TABLE IF EXISTS user_tags;
DROP TABLE IF EXISTS user_settings;
DROP TABLE IF EXISTS user_stats;
DROP TABLE IF EXISTS learning_plans;
DROP TABLE IF EXISTS badges;
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS favorite_folders;
DROP TABLE IF EXISTS user_frequencies;
DROP TABLE IF EXISTS word_variants;
DROP TABLE IF EXISTS word_tags;
DROP TABLE IF EXISTS word_forms;
DROP TABLE IF EXISTS word_relations;
DROP TABLE IF EXISTS examples;
DROP TABLE IF EXISTS prep_patterns;
DROP TABLE IF EXISTS collocations;
DROP TABLE IF EXISTS usage_notes;
DROP TABLE IF EXISTS definitions;
DROP TABLE IF EXISTS articles;
DROP TABLE IF EXISTS words;
DROP TABLE IF EXISTS users;

-- ============================================================
-- 1. 用户表（增强：头像/简介）
-- ============================================================
CREATE TABLE users (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    username        VARCHAR(50)     NOT NULL COMMENT '用户名',
    password_hash   VARCHAR(255)    NOT NULL COMMENT '密码哈希',
    email           VARCHAR(200)    NULL     COMMENT '邮箱',
    nickname        VARCHAR(100)    NULL     COMMENT '昵称',
    avatar_url      VARCHAR(500)    NULL     COMMENT '头像URL',
    bio             TEXT            NULL     COMMENT '个人简介',
    role            ENUM('admin','editor','user') NOT NULL DEFAULT 'user' COMMENT '角色',
    permission_level TINYINT        NOT NULL DEFAULT 1 COMMENT '权限级别 1=普通 5=编辑 9=管理员',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否激活',
    default_strategy_id CHAR(36)     NULL     COMMENT '→ study_strategies.id，用户默认学习策略',
    last_login_at   DATETIME        NULL     COMMENT '最后登录',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    INDEX idx_role (role),
    INDEX idx_permission (permission_level),
    INDEX idx_default_strategy (default_strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 单词主表
-- ============================================================
CREATE TABLE words (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    word            VARCHAR(50)     NOT NULL COMMENT '单词',
    pos             VARCHAR(30)     NOT NULL COMMENT '词性 (vt./vi./n./adj./adv./prep.)',
    first_letter    CHAR(1)         NOT NULL COMMENT '首字母分区键',

    -- 发音
    phonetic_uk     VARCHAR(100)    NULL     COMMENT '英式音标 (IPA)',
    phonetic_us     VARCHAR(100)    NULL     COMMENT '美式音标 (IPA)',
    audio_uk        VARCHAR(500)    NULL     COMMENT '英式发音音频URL',
    audio_us        VARCHAR(500)    NULL     COMMENT '美式发音音频URL',

    -- 释义与词源
    meaning_cn      VARCHAR(500)    NULL     COMMENT '中文释义（摘要，与 definitions 同步）',
    etymology       TEXT            NULL     COMMENT '词源（英文）',
    etymology_cn    TEXT            NULL     COMMENT '词源（中文）',

    -- 分类
    source          VARCHAR(50)     NULL     COMMENT '来源词表 (CET-4, CET-6, 考研, TOEFL 等，单值)',
    difficulty      TINYINT         NOT NULL DEFAULT 0 COMMENT '难度 0-4' CHECK (difficulty BETWEEN 0 AND 4),
    frequency       INT             NOT NULL DEFAULT 0 COMMENT '默认频率（系统级，越大越常见）',

    -- SM-2 间隔重复
    stage           TINYINT         NOT NULL DEFAULT 0 COMMENT '学习阶段 0=未学 1=学习中 2=复习中 3=已掌握' CHECK (stage BETWEEN 0 AND 3),
    confidence      TINYINT         NOT NULL DEFAULT 0 COMMENT '掌握度 0-5' CHECK (confidence BETWEEN 0 AND 5),
    review_count    INT             NOT NULL DEFAULT 0 COMMENT '总复习次数',
    consecutive_correct INT         NOT NULL DEFAULT 0 COMMENT '连续答对次数（SM-2 核心参数）',
    ease_factor     DECIMAL(4,2)    NOT NULL DEFAULT 2.50 COMMENT '难度系数 1.3~3.0 (SM-2, 默认 2.5)' CHECK (ease_factor BETWEEN 1.30 AND 3.00),
    interval_days   INT             NOT NULL DEFAULT 0 COMMENT '当前复习间隔天数',
    last_reviewed_at DATETIME       NULL     COMMENT '上次复习时间',
    next_review     DATETIME        NULL     COMMENT '下次复习时间',

    -- 时间审计
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',

    UNIQUE KEY uk_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='词汇主表（含 SM-2 间隔重复）';

-- ============================================================
-- 3. 英文文章表
-- ============================================================
CREATE TABLE articles (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    title           VARCHAR(500)    NOT NULL COMMENT '文章标题',
    author          VARCHAR(200)    NULL     COMMENT '作者',
    content         TEXT            NOT NULL COMMENT '正文内容',
    summary         TEXT            NULL     COMMENT '摘要',
    source_url      VARCHAR(500)    NULL     COMMENT '文章来源链接',
    source_name     VARCHAR(200)    NULL     COMMENT '来源名称（如 The Economist, Nature）',
    difficulty      TINYINT         NOT NULL DEFAULT 0 COMMENT '难度 0-4',
    frequency       INT             NOT NULL DEFAULT 0 COMMENT '默认频率（系统级）',
    word_count      INT             NULL     COMMENT '单词数',
    language_level  VARCHAR(20)     NULL     COMMENT '语言等级（CET-4, CET-6, GRE 等）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_difficulty (difficulty),
    INDEX idx_frequency (frequency),
    INDEX idx_language_level (language_level),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='英文文章表';

-- ============================================================
-- 4. 释义表
-- ============================================================
CREATE TABLE definitions (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id',
    meaning_en      TEXT            NOT NULL COMMENT '英文释义',
    meaning_cn      VARCHAR(500)    NOT NULL COMMENT '中文释义',
    pos_detail      VARCHAR(30)     NULL     COMMENT '细分词性（与主表 pos 不同时填写）',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_definitions_word (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='释义表（一词多义）';

-- ============================================================
-- 5. 用法说明表
-- ============================================================
CREATE TABLE usage_notes (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id',
    note_en         TEXT            NOT NULL COMMENT '英文说明',
    note_cn         VARCHAR(500)    NOT NULL COMMENT '中文说明',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_usage_word (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用法说明表';

-- ============================================================
-- 6. 固定搭配表
-- ============================================================
CREATE TABLE collocations (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id',
    collocation     VARCHAR(200)    NOT NULL COMMENT '搭配（英文）',
    translation     VARCHAR(200)    NOT NULL COMMENT '翻译（中文）',
    frequency       INT             NOT NULL DEFAULT 0 COMMENT '默认频率，用于排序',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_colloc_word (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固定搭配表';

-- ============================================================
-- 7. 介词搭配表
-- ============================================================
CREATE TABLE prep_patterns (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id（通常为动词）',
    pattern         VARCHAR(200)    NOT NULL COMMENT '介词模式（英文）',
    translation     VARCHAR(200)    NOT NULL COMMENT '中文翻译',
    preposition     VARCHAR(20)     NULL     COMMENT '核心介词',
    frequency       INT             NOT NULL DEFAULT 0 COMMENT '默认频率，用于排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_prep_word (word_id),
    INDEX idx_prep_preposition (preposition)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='介词搭配表';

-- ============================================================
-- 8. 例句表
-- ============================================================
CREATE TABLE examples (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id',
    sentence_en     TEXT            NOT NULL COMMENT '英文例句',
    sentence_cn     TEXT            NOT NULL COMMENT '中文翻译',
    source_type     ENUM('CET46','KAOYAN','TOEFL','IELTS','ACADEMIC','COMMON','ARTICLE') NULL COMMENT '来源分类（ARTICLE=来自文章，NULL=无来源）',
    source_detail   VARCHAR(200)    NULL     COMMENT '具体出处（如 CET-4 2021-06, Nature 2021）',
    article_id      CHAR(36)        NULL     COMMENT '→ articles.id，来源文章（NULL 表示不来自文章）',
    frequency       INT             NOT NULL DEFAULT 0 COMMENT '默认频率，用于排序',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_example_word (word_id),
    INDEX idx_example_source (source_type),
    INDEX idx_example_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='例句表';

-- ============================================================
-- 9. 单词关系网络表
-- ============================================================
CREATE TABLE word_relations (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id（源词）',
    related_word_id CHAR(36)        NOT NULL COMMENT '→ words.id（关联词）',
    relation_type   ENUM('synonym','antonym','hyponym','hypernym','derivative','see_also') NOT NULL COMMENT '关系类型',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_relation (word_id, related_word_id, relation_type),
    INDEX idx_rel_word (word_id),
    INDEX idx_rel_related (related_word_id),
    INDEX idx_rel_type (relation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词关系网络表';

-- ============================================================
-- 10. 单词标签表
-- ============================================================
CREATE TABLE word_tags (
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id',
    tag             VARCHAR(30)     NOT NULL COMMENT '标签 (如 CET-4, 考研, 科技, 法律)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (word_id, tag),
    INDEX idx_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词标签表';

-- ============================================================
-- 11. 单词变形表
-- ============================================================
CREATE TABLE word_forms (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id',
    form_type       VARCHAR(20)     NOT NULL COMMENT 'past/participle/third/gerund/plural/comparative/superlative',
    form_value      VARCHAR(50)     NOT NULL COMMENT '变形值',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_form (word_id, form_type),
    INDEX idx_form_word (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词变形表';

-- ============================================================
-- 12. 拼写变体表
-- ============================================================
CREATE TABLE word_variants (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id',
    variant         VARCHAR(50)     NOT NULL COMMENT '变体拼写',
    region          VARCHAR(20)     NULL     COMMENT 'en-US / en-GB / en-AU',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_variant (word_id, variant, region),
    INDEX idx_variant_word (word_id),
    INDEX idx_variant (variant)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼写变体表';

-- ============================================================
-- 13. 个人频率表
-- ============================================================
CREATE TABLE user_frequencies (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    entity_type     ENUM('word','collocation','prep_pattern','example','article') NOT NULL COMMENT '实体类型',
    entity_id       CHAR(36)        NOT NULL COMMENT '对应实体表的 UUID',
    frequency       INT             NOT NULL DEFAULT 0 COMMENT '用户自定义频率（覆盖系统默认频率）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_entity (user_id, entity_type, entity_id),
    INDEX idx_user (user_id),
    INDEX idx_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人频率表（用户可覆盖系统默认频率）';

-- ============================================================
-- 14. 收藏夹表
-- ============================================================
CREATE TABLE favorite_folders (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    name            VARCHAR(100)    NOT NULL COMMENT '收藏夹名称',
    category        ENUM('word','example','phrase','article','other') NOT NULL DEFAULT 'other' COMMENT '收藏类别',
    is_default      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否默认（注册时自动创建"稍后复习"）',
    is_public       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否公开',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_category (category),
    INDEX idx_user_default (user_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏夹表';

-- ============================================================
-- 15. 收藏条目表
-- ============================================================
CREATE TABLE favorites (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    folder_id       CHAR(36)        NOT NULL COMMENT '→ favorite_folders.id',
    entity_type     ENUM('word','collocation','prep_pattern','example','article') NOT NULL COMMENT '收藏的实体类型',
    entity_id       CHAR(36)        NOT NULL COMMENT '对应实体表的 UUID',
    note            TEXT            NULL     COMMENT '用户备注',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_folder_entity (folder_id, entity_type, entity_id),
    INDEX idx_folder (folder_id),
    INDEX idx_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏条目表';

-- ============================================================
-- 16. 用户设置表（偏好/配置）
-- ============================================================
CREATE TABLE user_settings (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    setting_key     VARCHAR(50)     NOT NULL COMMENT '设置键名',
    setting_value   TEXT            NOT NULL COMMENT '设置值',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_setting (user_id, setting_key),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表（偏好/配置键值对）';

-- ============================================================
-- 17. 用户统计表（游戏化数据）
-- ============================================================
CREATE TABLE user_stats (
    id                  CHAR(36)    NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id             CHAR(36)    NOT NULL COMMENT '→ users.id',
    xp                  INT         NOT NULL DEFAULT 0 COMMENT '经验值',
    level               INT         NOT NULL DEFAULT 1 COMMENT '等级',
    streak_days         INT         NOT NULL DEFAULT 0 COMMENT '当前连续打卡天数',
    longest_streak      INT         NOT NULL DEFAULT 0 COMMENT '最长连续打卡纪录',
    total_words_learned INT         NOT NULL DEFAULT 0 COMMENT '累计学习新词数',
    total_reviews       INT         NOT NULL DEFAULT 0 COMMENT '累计复习次数',
    total_time_spent_sec INT        NOT NULL DEFAULT 0 COMMENT '累计学习时长（秒）',
    is_public           TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否公开排行榜',
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user (user_id),
    INDEX idx_xp (xp DESC),
    INDEX idx_level (level DESC),
    INDEX idx_streak (streak_days DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户统计/游戏化数据表';

-- ============================================================
-- 18. 用户自定义标签表
-- ============================================================
CREATE TABLE user_tags (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    tag             VARCHAR(30)     NOT NULL COMMENT '标签名称',
    color           VARCHAR(7)      NULL     COMMENT '标签颜色（如 #FF5733）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_tag (user_id, tag),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自定义标签表（个人分类体系）';

-- ============================================================
-- 19. 用户笔记表
-- ============================================================
CREATE TABLE user_notes (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    entity_type     VARCHAR(50)     NOT NULL COMMENT '实体类型 (word/collocation/prep_pattern/example/article)',
    entity_id       CHAR(36)        NOT NULL COMMENT '对应实体表的 UUID',
    content         TEXT            NOT NULL COMMENT '笔记内容',
    is_private      TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否私密',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_user_entity (user_id, entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户个人笔记表';

-- ============================================================
-- 20. 用户标签-实体关联表
-- ============================================================
CREATE TABLE user_entity_tags (
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    tag_id          CHAR(36)        NOT NULL COMMENT '→ user_tags.id',
    entity_type     VARCHAR(50)     NOT NULL COMMENT '实体类型',
    entity_id       CHAR(36)        NOT NULL COMMENT '对应实体表的 UUID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, tag_id, entity_type, entity_id),
    INDEX idx_tag (tag_id),
    INDEX idx_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户标签与实体关联表（多对多）';

-- ============================================================
-- 21. 学习活动日志表
-- ============================================================
CREATE TABLE learning_activities (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    activity_date   DATE            NOT NULL COMMENT '活动日期',
    words_studied   INT             NOT NULL DEFAULT 0 COMMENT '学习新词数',
    reviews_done    INT             NOT NULL DEFAULT 0 COMMENT '复习次数',
    time_spent_sec  INT             NOT NULL DEFAULT 0 COMMENT '学习时长（秒）',
    correct_count   INT             NOT NULL DEFAULT 0 COMMENT '答对次数',
    wrong_count     INT             NOT NULL DEFAULT 0 COMMENT '答错次数',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, activity_date),
    INDEX idx_user (user_id),
    INDEX idx_date (activity_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习活动日志表（每日汇总，用于打卡/图表）';

-- ============================================================
-- 22. 答题日志表（细粒度错题分析）
-- ============================================================
CREATE TABLE review_log (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id',
    quiz_type       ENUM('meaning','spelling','listening','usage','sentence') NOT NULL COMMENT '题型',
    is_correct      BOOLEAN         NOT NULL COMMENT '是否答对',
    response_time_ms INT            NULL     COMMENT '响应时间（毫秒）',
    wrong_answer    TEXT            NULL     COMMENT '用户答错的内容',
    reviewed_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '答题时间',
    INDEX idx_user (user_id),
    INDEX idx_word (word_id),
    INDEX idx_reviewed_at (reviewed_at),
    INDEX idx_user_word (user_id, word_id),
    INDEX idx_quiz_type (quiz_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题日志表（细粒度错题分析与自适应学习）';

-- ============================================================
-- 23. 搜索历史表
-- ============================================================
CREATE TABLE search_history (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    query           VARCHAR(200)    NOT NULL COMMENT '搜索关键词',
    result_count    INT             NOT NULL DEFAULT 0 COMMENT '结果数量',
    searched_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    INDEX idx_user (user_id),
    INDEX idx_user_time (user_id, searched_at DESC),
    INDEX idx_query (query(20))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索历史表（用于联想/推荐）';

-- ============================================================
-- 24. 内容评分/反馈表
-- ============================================================
CREATE TABLE content_ratings (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    entity_type     VARCHAR(50)     NOT NULL COMMENT '实体类型',
    entity_id       CHAR(36)        NOT NULL COMMENT '对应实体表的 UUID',
    rating          TINYINT         NOT NULL COMMENT '评分 1-5',
    feedback        TEXT            NULL     COMMENT '反馈/报错内容',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_entity (user_id, entity_type, entity_id),
    INDEX idx_user (user_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容评分/反馈表（用户对词条、例句等的评价）';

-- ============================================================
-- 25. 徽章定义表
-- ============================================================
CREATE TABLE badges (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    name            VARCHAR(100)    NOT NULL COMMENT '徽章名称',
    icon            VARCHAR(200)    NULL     COMMENT '图标标识',
    description     VARCHAR(500)    NOT NULL COMMENT '徽章描述',
    criteria        JSON            NOT NULL COMMENT '解锁条件（如 {"type":"streak","days":7}）',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='徽章定义表（游戏化成就系统）';

-- ============================================================
-- 26. 用户徽章表
-- ============================================================
CREATE TABLE user_badges (
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    badge_id        CHAR(36)        NOT NULL COMMENT '→ badges.id',
    earned_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    PRIMARY KEY (user_id, badge_id),
    INDEX idx_badge (badge_id),
    INDEX idx_earned (earned_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户获得徽章表';

-- ============================================================
-- 27. 文章阅读进度表
-- ============================================================
CREATE TABLE reading_progress (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    article_id      CHAR(36)        NOT NULL COMMENT '→ articles.id',
    scroll_position INT             NOT NULL DEFAULT 0 COMMENT '阅读进度（字符偏移）',
    is_completed    BOOLEAN         NOT NULL DEFAULT FALSE COMMENT '是否读完',
    words_looked_up INT             NOT NULL DEFAULT 0 COMMENT '阅读中查词数',
    last_read_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后阅读时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_article (user_id, article_id),
    INDEX idx_user (user_id),
    INDEX idx_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章阅读进度表（跨设备同步阅读位置）';

-- ============================================================
-- 28. 每日推荐表
-- ============================================================
CREATE TABLE daily_recommendations (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    recommend_date  DATE            NOT NULL COMMENT '推荐日期',
    entity_type     VARCHAR(50)     NOT NULL COMMENT '实体类型',
    entity_id       CHAR(36)        NOT NULL COMMENT '对应实体表的 UUID',
    reason          VARCHAR(100)    NULL     COMMENT '推荐原因（易错词/新词/间隔到期等）',
    is_consumed     BOOLEAN         NOT NULL DEFAULT FALSE COMMENT '是否已学习',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, recommend_date),
    INDEX idx_user_consumed (user_id, is_consumed),
    INDEX idx_date (recommend_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日推荐表（自适应个性化推送）';

-- ============================================================
-- 29. 学习计划模板表
-- ============================================================
CREATE TABLE learning_plans (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    name            VARCHAR(200)    NOT NULL COMMENT '计划名称',
    description     TEXT            NULL     COMMENT '计划描述',
    target_level    VARCHAR(50)     NULL     COMMENT '目标等级（CET-4/雅思6.5/GRE等）',
    duration_days   INT             NOT NULL COMMENT '计划总天数',
    daily_word_count INT            NOT NULL DEFAULT 10 COMMENT '每日学习词数',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE COMMENT '是否启用',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active (is_active),
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划模板表（如"30天冲刺CET-4"）';

-- ============================================================
-- 30. 用户学习计划表
-- ============================================================
CREATE TABLE user_plans (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    plan_id         CHAR(36)        NOT NULL COMMENT '→ learning_plans.id',
    started_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at    DATETIME        NULL     COMMENT '完成时间',
    current_day     INT             NOT NULL DEFAULT 0 COMMENT '当前进行到第几天',
    daily_target    INT             NULL     COMMENT '自定义每日目标（覆盖模板默认）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_plan (plan_id),
    INDEX idx_user_active (user_id, completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户学习计划表（用户参与的计划）';

-- ============================================================
-- 31. 单词本表
-- ============================================================
CREATE TABLE word_books (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    name            VARCHAR(200)    NOT NULL COMMENT '单词本名称（如 四级单词、考研单词）',
    description     TEXT            NULL     COMMENT '描述',
    difficulty_level VARCHAR(50)    NULL     COMMENT '难度等级（CET-4/CET-6/考研/GRE等）',
    word_count      INT             NOT NULL DEFAULT 0 COMMENT '词汇总量',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active (is_active),
    INDEX idx_sort (sort_order),
    INDEX idx_level (difficulty_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词本表（四级/六级/考研等词库集合）';

-- ============================================================
-- 32. 单词本词条表
-- ============================================================
CREATE TABLE word_book_entries (
    word_book_id    CHAR(36)        NOT NULL COMMENT '→ word_books.id',
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序位置（用于字母序等顺序策略）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (word_book_id, word_id),
    INDEX idx_word (word_id),
    INDEX idx_order (word_book_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词本词条表（单词与单词本的多对多关联）';

-- ============================================================
-- 33. 学习策略表
-- ============================================================
CREATE TABLE study_strategies (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    name            VARCHAR(100)    NOT NULL COMMENT '策略名称',
    description     VARCHAR(500)    NULL     COMMENT '描述',
    type            ENUM('random','alphabetical','pos_alphabetical','pos_random','difficulty_asc','difficulty_desc') NOT NULL COMMENT '策略类型',
    config          JSON            NULL     COMMENT '额外配置参数',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习策略表（定义从单词本取词的策略）';

-- ============================================================
-- 34. 用户单词本学习进度表
-- ============================================================
CREATE TABLE user_word_book_progress (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    word_book_id    CHAR(36)        NOT NULL COMMENT '→ word_books.id',
    strategy_id     CHAR(36)        NOT NULL COMMENT '→ study_strategies.id',
    daily_count     INT             NOT NULL DEFAULT 10 COMMENT '每日学习词数',
    current_position INT            NOT NULL DEFAULT 0 COMMENT '当前进度位置（已学到第几个词）',
    is_completed    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否完成',
    started_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at    DATETIME        NULL     COMMENT '完成时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_book (user_id, word_book_id),
    INDEX idx_user (user_id),
    INDEX idx_book (word_book_id),
    INDEX idx_strategy (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户单词本学习进度表（用户选择单词本+策略后的进度）';

-- ============================================================
-- 35. 每日计划词条表
-- ============================================================
CREATE TABLE daily_plan_items (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    word_book_id    CHAR(36)        NOT NULL COMMENT '→ word_books.id',
    plan_date       DATE            NOT NULL COMMENT '计划日期',
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '当日排序',
    is_completed    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已完成学习',
    completed_at    DATETIME        NULL     COMMENT '完成时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, plan_date),
    INDEX idx_user_book_date (user_id, word_book_id, plan_date),
    INDEX idx_word (word_id),
    INDEX idx_date (plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日计划词条表（按策略从单词本生成每日学习列表）';

-- ============================================================
-- 36. 用户每日计划条目表（自由添加，不依赖单词本）
-- ============================================================
CREATE TABLE user_daily_plan_entries (
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    user_id         CHAR(36)        NOT NULL COMMENT '→ users.id',
    plan_date       DATE            NOT NULL COMMENT '计划日期',
    word_id         CHAR(36)        NOT NULL COMMENT '→ words.id',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '当日排序',
    is_completed    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已完成学习',
    completed_at    DATETIME        NULL     COMMENT '完成时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, plan_date),
    INDEX idx_user_date_word (user_id, plan_date, word_id),
    INDEX idx_word (word_id),
    INDEX idx_date (plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日计划条目表（用户从单词本自由添加单词到某日计划，不依赖系统生成策略）';

-- ============================================================
-- words 索引
-- ============================================================
ALTER TABLE words ADD INDEX idx_letter (first_letter);
ALTER TABLE words ADD INDEX idx_pos (pos);
ALTER TABLE words ADD INDEX idx_difficulty (difficulty);
ALTER TABLE words ADD INDEX idx_source (source);
ALTER TABLE words ADD INDEX idx_stage (stage);
ALTER TABLE words ADD INDEX idx_next_review (next_review);
ALTER TABLE words ADD INDEX idx_created_at (created_at);
ALTER TABLE words ADD INDEX idx_stage_review (stage, next_review) COMMENT '复习队列复合索引';
ALTER TABLE words ADD INDEX idx_letter_pos (first_letter, pos) COMMENT '字母+词性复合索引';
ALTER TABLE words ADD INDEX idx_confidence (confidence) COMMENT '掌握度筛选';
ALTER TABLE words ADD INDEX idx_frequency (frequency) COMMENT '频率筛选';

-- ============================================================
-- 示例数据
-- ============================================================

-- 用户
INSERT INTO users (id, username, password_hash, nickname, role, permission_level, avatar_url, bio, default_strategy_id)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'admin', '$2y$10$placeholder', '管理员', 'admin', 9, NULL, '系统管理员', NULL),
       ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'demo',  '$2y$10$placeholder', '演示用户', 'user', 1, NULL, '英语学习者', 'c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01');

-- 用户统计
INSERT INTO user_stats (id, user_id, xp, level, streak_days) VALUES
('a3eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 320, 5, 3);

-- 用户设置
INSERT INTO user_settings (id, user_id, setting_key, setting_value) VALUES
('a4eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'daily_word_goal', '20'),
('a4eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'learning_mode', 'card'),
('a4eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'pronunciation', 'uk'),
('a4eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'theme', 'light'),
('a4eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'reminder_time', '08:00');

-- 单词 abandon
INSERT INTO words (id, word, pos, first_letter, phonetic_uk, meaning_cn,
                   etymology, etymology_cn, source, difficulty, frequency,
                   stage, confidence, review_count, consecutive_correct, ease_factor, interval_days)
VALUES ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'abandon', 'vt.', 'A', '/əˈbændən/',
        '放弃；遗弃；抛弃',
        'From Old French abandoner (mettre à bandon — "to put under control")',
        '源自古法语 abandoner，意为"置于控制之下"',
        'CET-4', 2, 3,
        2, 3, 5, 3, 2.50, 6);

INSERT INTO definitions (id, word_id, meaning_en, meaning_cn) VALUES
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'to leave completely and finally; to give up', '放弃；遗弃；抛弃');

INSERT INTO usage_notes (id, word_id, note_en, note_cn, sort_order) VALUES
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'Used with a direct object: abandon sth/sb', '及物动词，后直接跟宾语', 0),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 '"Abandon oneself to sth" means to give in completely', '沉溺于某种情感', 1);

INSERT INTO collocations (id, word_id, collocation, translation, frequency) VALUES
('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'abandon hope',   '放弃希望', 5),
('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'abandon ship',   '弃船',     4),
('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'abandon a plan', '放弃计划', 3);

INSERT INTO prep_patterns (id, word_id, pattern, translation, preposition, frequency) VALUES
('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'abandon sth to sb',   '把某物丢给某人',  'to',  3),
('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'abandon sth for sth', '放弃A选择B',     'for', 4),
('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'abandon hope of',     '放弃…的希望',    'of',  3);

INSERT INTO examples (id, word_id, sentence_en, sentence_cn, source_type, source_detail, frequency) VALUES
('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'The captain ordered the crew to abandon the sinking ship.',
 '船长命令船员弃船。', 'CET46', 'CET-4 2019-06', 5),
('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'He abandoned his research after years of fruitless effort.',
 '经过多年努力，他放弃了研究。', 'KAOYAN', '考研英语 2018', 4),
('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'Many residents were forced to abandon their homes due to the volcanic eruption.',
 '许多居民因火山爆发撤离家园。', 'TOEFL', 'TOEFL TPO 31', 3),
('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'The study had to be abandoned due to insufficient funding.',
 '该研究因资金不足而中止。', 'ACADEMIC', 'Nature 2019', 3),
('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'Don''t abandon your dreams just because of one setback.',
 '不要因为一次挫折就放弃梦想。', 'COMMON', NULL, 4);

INSERT INTO word_tags (word_id, tag) VALUES
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'CET-4'),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '考研核心'),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '高频动词');

-- 收藏夹
INSERT INTO favorite_folders (id, user_id, name, category, is_default, sort_order) VALUES
('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '稍后复习', 'word', 1, 0),
('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '精彩例句', 'example', 0, 1),
('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '地道搭配', 'phrase', 0, 2),
('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '好文收藏', 'article', 0, 3);

INSERT INTO favorites (id, folder_id, entity_type, entity_id, note) VALUES
('a2eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'word', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '高频词，需要重点掌握');

-- 用户自定义标签
INSERT INTO user_tags (id, user_id, tag, color) VALUES
('a5eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', '写作词汇', '#FF5733'),
('a5eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', '口语词汇', '#33FF57'),
('a5eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', '考试必备', '#3357FF');

-- 用户标签关联
INSERT INTO user_entity_tags (user_id, tag_id, entity_type, entity_id) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'a5eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'word', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01');

-- 用户笔记
INSERT INTO user_notes (id, user_id, entity_type, entity_id, content, is_private) VALUES
('a6eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
 'word', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 '这个词在考研阅读里出现过好几次，注意它和 desert, give up 的区别。', 0);

-- 学习活动日志
INSERT INTO learning_activities (id, user_id, activity_date, words_studied, reviews_done, time_spent_sec, correct_count, wrong_count) VALUES
('a7eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', CURDATE(), 5, 20, 1800, 22, 3);

-- 答题日志
INSERT INTO review_log (id, user_id, word_id, quiz_type, is_correct, response_time_ms, wrong_answer) VALUES
('a8eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'meaning', FALSE, 3200, '丢弃');

-- 搜索历史
INSERT INTO search_history (id, user_id, query, result_count) VALUES
('a9eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'abandon', 1),
('a9eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'remarkable', 0);

-- 内容评分
INSERT INTO content_ratings (id, user_id, entity_type, entity_id, rating, feedback) VALUES
('aaeebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
 'example', 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 5, '非常实用的例句！');

-- 徽章
INSERT INTO badges (id, name, icon, description, criteria, sort_order)
VALUES ('abeebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '初次学习', '🎯', '完成第一次学习', '{"type":"first_lesson"}', 0),
       ('abeebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '打卡7天', '🔥', '连续学习7天', '{"type":"streak","days":7}', 1),
       ('abeebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '百词斩', '💪', '累计学习100个单词', '{"type":"words_learned","count":100}', 2),
       ('abeebc99-9c0b-4ef8-bb6d-6bb9bd380a04', '学霸', '🧠', '答对率超过90%', '{"type":"accuracy","rate":0.9}', 3);

-- 每日推荐
INSERT INTO daily_recommendations (id, user_id, recommend_date, entity_type, entity_id, reason) VALUES
('aceebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', CURDATE(),
 'word', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '间隔复习到期');

-- 学习计划
INSERT INTO learning_plans (id, name, description, target_level, duration_days, daily_word_count, sort_order)
VALUES ('adeebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'CET-4 30天冲刺', '针对四级核心词汇的30天突破计划', 'CET-4', 30, 20, 0),
       ('adeebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '考研词汇 60天', '考研英语核心词汇60天系统学习', '考研', 60, 15, 1),
       ('adeebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '雅思7分词汇', '雅思核心词汇，目标7分以上', '雅思7.0', 45, 25, 2);

-- 用户参与计划
INSERT INTO user_plans (id, user_id, plan_id, current_day) VALUES
('aeeebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
 'adeebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 3);

-- ============================================================
-- 示例数据：单词本 & 策略
-- ============================================================

-- 单词本
INSERT INTO word_books (id, name, description, difficulty_level, word_count, sort_order) VALUES
('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '四级单词', '大学英语四级核心词汇', 'CET-4', 2500, 0),
('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '六级单词', '大学英语六级核心词汇', 'CET-6', 3000, 1),
('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '考研单词', '考研英语核心词汇', '考研', 3500, 2);

-- 单词本词条（abandon 加入四级和考研单词本）
INSERT INTO word_book_entries (word_book_id, word_id, sort_order) VALUES
('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 1),
('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 1);

-- 学习策略
INSERT INTO study_strategies (id, name, description, type, sort_order) VALUES
('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '完全随机', '从单词本中完全随机选取', 'random', 0),
('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '字母顺序', '按照首字母 A→Z 顺序选取', 'alphabetical', 1),
('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '按词性+字母序', '选定词性后按字母顺序选取', 'pos_alphabetical', 2),
('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', '按词性+随机', '选定词性后随机选取', 'pos_random', 3),
('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', '难度递增', '从简单到困难顺序选取', 'difficulty_asc', 4),
('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', '难度递减', '从困难到简单顺序选取', 'difficulty_desc', 5);

-- 用户单词本进度（demo 用户在四级单词本中用随机策略，每日10词）
INSERT INTO user_word_book_progress (id, user_id, word_book_id, strategy_id, daily_count, current_position) VALUES
('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 10, 12);

-- 今日计划词条
INSERT INTO daily_plan_items (id, user_id, word_book_id, plan_date, word_id, sort_order) VALUES
('e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', CURDATE(), 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 1);

-- 用户自由添加的每日计划条目
INSERT INTO user_daily_plan_entries (id, user_id, plan_date, word_id, sort_order) VALUES
('f1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
 CURDATE(), 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 1),
('f1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
 CURDATE(), 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 2);
