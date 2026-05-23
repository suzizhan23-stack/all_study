-- ============================================================
-- 英语单词知识库 — 一键建库 v9
-- 36 表 | INT AUTO_INCREMENT 主键 + uuid CHAR(36) UNIQUE
-- 所有 FK 列改为 BIGINT 引用 INT PK
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
-- 1. 用户表
-- ============================================================
CREATE TABLE users (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    username        VARCHAR(50)     NOT NULL COMMENT '用户名',
    password_hash   VARCHAR(255)    NOT NULL COMMENT '密码哈希',
    email           VARCHAR(200)    NULL     COMMENT '邮箱',
    nickname        VARCHAR(100)    NULL     COMMENT '昵称',
    avatar_url      VARCHAR(500)    NULL     COMMENT '头像URL',
    bio             TEXT            NULL     COMMENT '个人简介',
    role            ENUM('admin','editor','user') NOT NULL DEFAULT 'user' COMMENT '角色',
    permission_level TINYINT        NOT NULL DEFAULT 1 COMMENT '权限级别 1=普通 5=编辑 9=管理员',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否激活',
    default_strategy_id INT         NULL     COMMENT '→ study_strategies.id，用户默认学习策略',
    last_login_at   DATETIME        NULL     COMMENT '最后登录',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    INDEX idx_role (role),
    INDEX idx_permission (permission_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 单词主表
-- ============================================================
CREATE TABLE words (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    word_id         INT             NOT NULL COMMENT '→ words.id',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    word_id         INT             NOT NULL COMMENT '→ words.id',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    word_id         INT             NOT NULL COMMENT '→ words.id',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    word_id         INT             NOT NULL COMMENT '→ words.id（通常为动词）',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    word_id         INT             NOT NULL COMMENT '→ words.id',
    sentence_en     TEXT            NOT NULL COMMENT '英文例句',
    sentence_cn     TEXT            NOT NULL COMMENT '中文翻译',
    source_type     ENUM('CET46','KAOYAN','TOEFL','IELTS','ACADEMIC','COMMON','ARTICLE') NULL COMMENT '来源分类',
    source_detail   VARCHAR(200)    NULL     COMMENT '具体出处',
    article_id      INT             NULL     COMMENT '→ articles.id，来源文章',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    word_id         INT             NOT NULL COMMENT '→ words.id（源词）',
    related_word_id INT             NOT NULL COMMENT '→ words.id（关联词）',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    word_id         INT             NOT NULL COMMENT '→ words.id',
    tag             VARCHAR(30)     NOT NULL COMMENT '标签 (如 CET-4, 考研, 科技, 法律)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_word_tag (word_id, tag),
    INDEX idx_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词标签表';

-- ============================================================
-- 11. 单词变形表
-- ============================================================
CREATE TABLE word_forms (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    word_id         INT             NOT NULL COMMENT '→ words.id',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    word_id         INT             NOT NULL COMMENT '→ words.id',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    entity_type     ENUM('word','collocation','prep_pattern','example','article') NOT NULL COMMENT '实体类型',
    entity_id       INT             NOT NULL COMMENT '对应实体表的 id',
    frequency       INT             NOT NULL DEFAULT 0 COMMENT '用户自定义频率',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_entity (user_id, entity_type, entity_id),
    INDEX idx_user (user_id),
    INDEX idx_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人频率表';

-- ============================================================
-- 14. 收藏夹表
-- ============================================================
CREATE TABLE favorite_folders (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    name            VARCHAR(100)    NOT NULL COMMENT '收藏夹名称',
    category        ENUM('word','example','phrase','article','other') NOT NULL DEFAULT 'other' COMMENT '收藏类别',
    is_default      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否默认',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    folder_id       INT             NOT NULL COMMENT '→ favorite_folders.id',
    user_id         INT             NOT NULL COMMENT '→ users.id（冗余，方便直接查询）',
    entity_type     ENUM('word','collocation','prep_pattern','example','article') NOT NULL COMMENT '收藏实体类型',
    entity_id       INT             NOT NULL COMMENT '对应实体表的 id',
    note            TEXT            NULL     COMMENT '用户备注',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_folder_entity (folder_id, entity_type, entity_id),
    INDEX idx_folder (folder_id),
    INDEX idx_user (user_id),
    INDEX idx_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏条目表';

-- ============================================================
-- 16. 用户设置表
-- ============================================================
CREATE TABLE user_settings (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    setting_key     VARCHAR(50)     NOT NULL COMMENT '设置键名',
    setting_value   TEXT            NOT NULL COMMENT '设置值',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_setting (user_id, setting_key),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表';

-- ============================================================
-- 17. 用户统计表
-- ============================================================
CREATE TABLE user_stats (
    id                  INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid                CHAR(36)    NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id             INT         NOT NULL COMMENT '→ users.id',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    tag             VARCHAR(30)     NOT NULL COMMENT '标签名称',
    color           VARCHAR(7)      NULL     COMMENT '标签颜色',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_tag (user_id, tag),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自定义标签表';

-- ============================================================
-- 19. 用户笔记表
-- ============================================================
CREATE TABLE user_notes (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    entity_type     VARCHAR(50)     NOT NULL COMMENT '实体类型 (word/collocation/prep_pattern/example/article)',
    entity_id       INT             NOT NULL COMMENT '对应实体表的 id',
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
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    tag_id          INT             NOT NULL COMMENT '→ user_tags.id',
    entity_type     VARCHAR(50)     NOT NULL COMMENT '实体类型',
    entity_id       INT             NOT NULL COMMENT '对应实体表的 id',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_tag_entity (user_id, tag_id, entity_type, entity_id),
    INDEX idx_tag (tag_id),
    INDEX idx_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户标签与实体关联表';

-- ============================================================
-- 21. 学习活动日志表
-- ============================================================
CREATE TABLE learning_activities (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习活动日志表';

-- ============================================================
-- 22. 答题日志表
-- ============================================================
CREATE TABLE review_log (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    word_id         INT             NOT NULL COMMENT '→ words.id',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题日志表';

-- ============================================================
-- 23. 搜索历史表
-- ============================================================
CREATE TABLE search_history (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    query           VARCHAR(200)    NOT NULL COMMENT '搜索关键词',
    result_count    INT             NOT NULL DEFAULT 0 COMMENT '结果数量',
    searched_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    INDEX idx_user (user_id),
    INDEX idx_user_time (user_id, searched_at DESC),
    INDEX idx_query (query(20))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索历史表';

-- ============================================================
-- 24. 内容评分表
-- ============================================================
CREATE TABLE content_ratings (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    entity_type     VARCHAR(50)     NOT NULL COMMENT '实体类型',
    entity_id       INT             NOT NULL COMMENT '对应实体表的 id',
    rating          TINYINT         NOT NULL COMMENT '评分 1-5',
    feedback        TEXT            NULL     COMMENT '反馈/报错内容',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_entity (user_id, entity_type, entity_id),
    INDEX idx_user (user_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容评分/反馈表';

-- ============================================================
-- 25. 徽章定义表
-- ============================================================
CREATE TABLE badges (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    name            VARCHAR(100)    NOT NULL COMMENT '徽章名称',
    icon            VARCHAR(200)    NULL     COMMENT '图标标识',
    description     VARCHAR(500)    NOT NULL COMMENT '徽章描述',
    criteria        JSON            NOT NULL COMMENT '解锁条件',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='徽章定义表';

-- ============================================================
-- 26. 用户徽章表
-- ============================================================
CREATE TABLE user_badges (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    badge_id        INT             NOT NULL COMMENT '→ badges.id',
    earned_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    UNIQUE KEY uk_user_badge (user_id, badge_id),
    INDEX idx_badge (badge_id),
    INDEX idx_earned (earned_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户获得徽章表';

-- ============================================================
-- 27. 文章阅读进度表
-- ============================================================
CREATE TABLE reading_progress (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    article_id      INT             NOT NULL COMMENT '→ articles.id',
    scroll_position INT             NOT NULL DEFAULT 0 COMMENT '阅读进度（字符偏移）',
    is_completed    BOOLEAN         NOT NULL DEFAULT FALSE COMMENT '是否读完',
    words_looked_up INT             NOT NULL DEFAULT 0 COMMENT '阅读中查词数',
    last_read_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后阅读时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_article (user_id, article_id),
    INDEX idx_user (user_id),
    INDEX idx_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章阅读进度表';

-- ============================================================
-- 28. 每日推荐表
-- ============================================================
CREATE TABLE daily_recommendations (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    recommend_date  DATE            NOT NULL COMMENT '推荐日期',
    entity_type     VARCHAR(50)     NOT NULL COMMENT '实体类型',
    entity_id       INT             NOT NULL COMMENT '对应实体表的 id',
    reason          VARCHAR(100)    NULL     COMMENT '推荐原因',
    is_consumed     BOOLEAN         NOT NULL DEFAULT FALSE COMMENT '是否已学习',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, recommend_date),
    INDEX idx_user_consumed (user_id, is_consumed),
    INDEX idx_date (recommend_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日推荐表';

-- ============================================================
-- 29. 学习计划模板表
-- ============================================================
CREATE TABLE learning_plans (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    name            VARCHAR(200)    NOT NULL COMMENT '计划名称',
    description     TEXT            NULL     COMMENT '计划描述',
    target_level    VARCHAR(50)     NULL     COMMENT '目标等级',
    duration_days   INT             NOT NULL COMMENT '计划总天数',
    daily_word_count INT            NOT NULL DEFAULT 10 COMMENT '每日学习词数',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE COMMENT '是否启用',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active (is_active),
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划模板表';

-- ============================================================
-- 30. 用户学习计划表
-- ============================================================
CREATE TABLE user_plans (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    plan_id         INT             NOT NULL COMMENT '→ learning_plans.id',
    started_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at    DATETIME        NULL     COMMENT '完成时间',
    current_day     INT             NOT NULL DEFAULT 0 COMMENT '当前进行到第几天',
    daily_target    INT             NULL     COMMENT '自定义每日目标',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_plan (plan_id),
    INDEX idx_user_active (user_id, completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户学习计划表';

-- ============================================================
-- 31. 单词本表
-- ============================================================
CREATE TABLE word_books (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    name            VARCHAR(200)    NOT NULL COMMENT '单词本名称',
    description     TEXT            NULL     COMMENT '描述',
    difficulty_level VARCHAR(50)    NULL     COMMENT '难度等级',
    word_count      INT             NOT NULL DEFAULT 0 COMMENT '词汇总量',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active (is_active),
    INDEX idx_sort (sort_order),
    INDEX idx_level (difficulty_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词本表';

-- ============================================================
-- 32. 单词本词条表
-- ============================================================
CREATE TABLE word_book_entries (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    word_book_id    INT             NOT NULL COMMENT '→ word_books.id',
    word_id         INT             NOT NULL COMMENT '→ words.id',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序位置',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_book_word (word_book_id, word_id),
    INDEX idx_word (word_id),
    INDEX idx_order (word_book_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词本词条表';

-- ============================================================
-- 33. 学习策略表
-- ============================================================
CREATE TABLE study_strategies (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    name            VARCHAR(100)    NOT NULL COMMENT '策略名称',
    description     VARCHAR(500)    NULL     COMMENT '描述',
    type            ENUM('random','alphabetical','pos_alphabetical','pos_random','difficulty_asc','difficulty_desc') NOT NULL COMMENT '策略类型',
    config          JSON            NULL     COMMENT '额外配置参数',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习策略表';

-- ============================================================
-- 34. 用户单词本学习进度表
-- ============================================================
CREATE TABLE user_word_book_progress (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    word_book_id    INT             NOT NULL COMMENT '→ word_books.id',
    strategy_id     INT             NOT NULL COMMENT '→ study_strategies.id',
    daily_count     INT             NOT NULL DEFAULT 10 COMMENT '每日学习词数',
    current_position INT            NOT NULL DEFAULT 0 COMMENT '当前进度位置',
    is_completed    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否完成',
    started_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at    DATETIME        NULL     COMMENT '完成时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_book (user_id, word_book_id),
    INDEX idx_user (user_id),
    INDEX idx_book (word_book_id),
    INDEX idx_strategy (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户单词本学习进度表';

-- ============================================================
-- 35. 每日计划词条表
-- ============================================================
CREATE TABLE daily_plan_items (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    word_book_id    INT             NOT NULL COMMENT '→ word_books.id',
    plan_date       DATE            NOT NULL COMMENT '计划日期',
    word_id         INT             NOT NULL COMMENT '→ words.id',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '当日排序',
    is_completed    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已完成学习',
    completed_at    DATETIME        NULL     COMMENT '完成时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, plan_date),
    INDEX idx_user_book_date (user_id, word_book_id, plan_date),
    INDEX idx_word (word_id),
    INDEX idx_date (plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日计划词条表';

-- ============================================================
-- 36. 用户每日计划条目表（自由添加）
-- ============================================================
CREATE TABLE user_daily_plan_entries (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    uuid            CHAR(36)        NOT NULL UNIQUE COMMENT '全局唯一标识',
    user_id         INT             NOT NULL COMMENT '→ users.id',
    plan_date       DATE            NOT NULL COMMENT '计划日期',
    word_id         INT             NOT NULL COMMENT '→ words.id',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '当日排序',
    is_completed    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已完成学习',
    completed_at    DATETIME        NULL     COMMENT '完成时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, plan_date),
    INDEX idx_user_date_word (user_id, plan_date, word_id),
    INDEX idx_word (word_id),
    INDEX idx_date (plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日计划条目表';

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
INSERT INTO users (uuid, username, password_hash, nickname, role, permission_level, avatar_url, bio, default_strategy_id)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'admin', '$2y$10$placeholder', '管理员', 'admin', 9, NULL, '系统管理员', NULL),
       ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'demo',  '$2y$10$placeholder', '演示用户', 'user', 1, NULL, '英语学习者', NULL);

-- 策略
INSERT INTO study_strategies (uuid, name, description, type, sort_order)
VALUES ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '完全随机', '从单词本中完全随机抽取', 'random', 1),
       ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '顺序递进', '按单词本编排顺序学习', 'alphabetical', 2),
       ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '先易后难', '从简单词开始循序渐进', 'difficulty_asc', 3);

-- 设置 admin 的默认策略
UPDATE users SET default_strategy_id = 1 WHERE uuid = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';

-- 用户统计 (user_id=2 = demo)
INSERT INTO user_stats (uuid, user_id, xp, level, streak_days)
VALUES ('a3eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 2, 320, 5, 3);

-- 用户设置
INSERT INTO user_settings (uuid, user_id, setting_key, setting_value) VALUES
('a4eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 2, 'daily_word_goal', '20'),
('a4eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 2, 'learning_mode', 'card'),
('a4eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 2, 'pronunciation', 'uk'),
('a4eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 2, 'theme', 'light'),
('a4eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 2, 'reminder_time', '08:00');

-- 单词 abandon (id=1)
INSERT INTO words (uuid, word, pos, first_letter, phonetic_uk, meaning_cn,
                   etymology, etymology_cn, source, difficulty, frequency,
                   stage, confidence, review_count, consecutive_correct, ease_factor, interval_days)
VALUES ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'abandon', 'vt.', 'A', '/əˈbændən/',
        '放弃；遗弃；抛弃',
        'From Old French abandoner (mettre à bandon — "to put under control")',
        '源自古法语 abandoner，意为"置于控制之下"',
        'CET-4', 2, 3,
        2, 3, 5, 3, 2.50, 6);

INSERT INTO definitions (uuid, word_id, meaning_en, meaning_cn) VALUES
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 1,
 'to leave completely and finally; to give up', '放弃；遗弃；抛弃');

INSERT INTO usage_notes (uuid, word_id, note_en, note_cn, sort_order) VALUES
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 1,
 'Used with a direct object: abandon sth/sb', '及物动词，后直接跟宾语', 0),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 1,
 '"Abandon oneself to sth" means to give in completely', '沉溺于某种情感', 1);

INSERT INTO collocations (uuid, word_id, collocation, translation, frequency) VALUES
('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 1, 'abandon hope',   '放弃希望', 5),
('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 1, 'abandon ship',   '弃船',     4),
('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 1, 'abandon a plan', '放弃计划', 3);

INSERT INTO prep_patterns (uuid, word_id, pattern, translation, preposition, frequency) VALUES
('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 1, 'abandon sth to sb',   '把某物丢给某人',  'to',  3),
('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 1, 'abandon sth for sth', '放弃A选择B',     'for', 4),
('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 1, 'abandon hope of',     '放弃…的希望',    'of',  3);

INSERT INTO examples (uuid, word_id, sentence_en, sentence_cn, source_type, source_detail, frequency) VALUES
('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 1,
 'The captain ordered the crew to abandon the sinking ship.',
 '船长命令船员弃船。', 'CET46', 'CET-4 2019-06', 5),
('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 1,
 'He abandoned his research after years of fruitless effort.',
 '经过多年努力，他放弃了研究。', 'KAOYAN', '考研英语 2018', 4),
('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 1,
 'Many residents were forced to abandon their homes due to the volcanic eruption.',
 '许多居民因火山爆发撤离家园。', 'TOEFL', 'TOEFL TPO 31', 3),
('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 1,
 'The study had to be abandoned due to insufficient funding.',
 '该研究因资金不足而中止。', 'ACADEMIC', 'Nature 2019', 3),
('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 1,
 'Don''t abandon your dreams just because of one setback.',
 '不要因为一次挫折就放弃梦想。', 'COMMON', NULL, 4);

INSERT INTO word_tags (uuid, word_id, tag) VALUES
('ff01', 1, 'CET-4'),
('ff02', 1, '考研核心'),
('ff03', 1, '高频动词');

-- 收藏夹 (user_id=1 = admin)
INSERT INTO favorite_folders (uuid, user_id, name, category, is_default, sort_order) VALUES
('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 1, '稍后复习', 'word', 1, 0),
('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 1, '精彩例句', 'example', 0, 1),
('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 1, '地道搭配', 'phrase', 0, 2),
('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 1, '好文收藏', 'article', 0, 3);

INSERT INTO favorites (uuid, folder_id, user_id, entity_type, entity_id, note) VALUES
('a2eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 1, 1, 'word', 1, '高频词，需要重点掌握');

-- 用户自定义标签 (user_id=2 = demo)
INSERT INTO user_tags (uuid, user_id, tag, color) VALUES
('a5eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 2, '写作词汇', '#FF5733'),
('a5eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 2, '口语词汇', '#33FF57'),
('a5eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 2, '考试必备', '#3357FF');

-- 用户标签关联 (user_id=2, tag_id=1, entity_id=1)
INSERT INTO user_entity_tags (uuid, user_id, tag_id, entity_type, entity_id) VALUES
('a5eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 2, 1, 'word', 1);

-- 用户笔记
INSERT INTO user_notes (uuid, user_id, entity_type, entity_id, content, is_private) VALUES
('a6eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 2, 'word', 1,
 '这个词在考研阅读里出现过好几次，注意它和 desert, give up 的区别。', 0);

-- 学习活动日志
INSERT INTO learning_activities (uuid, user_id, activity_date, words_studied, reviews_done, time_spent_sec, correct_count, wrong_count) VALUES
('a7eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 2, CURDATE(), 5, 20, 1800, 22, 3);

-- 答题日志
INSERT INTO review_log (uuid, user_id, word_id, quiz_type, is_correct, response_time_ms, wrong_answer) VALUES
('a8eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 2, 1, 'meaning', FALSE, 3200, '丢弃');

-- 搜索历史
INSERT INTO search_history (uuid, user_id, query, result_count) VALUES
('a9eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 2, 'abandon', 1),
('a9eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 2, 'remarkable', 0);

-- 内容评分
INSERT INTO content_ratings (uuid, user_id, entity_type, entity_id, rating, feedback) VALUES
('aaeebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 2, 'example', 1, 5, '非常实用的例句！');

-- 徽章
INSERT INTO badges (uuid, name, icon, description, criteria, sort_order)
VALUES ('abeebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '初次学习', '🎯', '完成第一次学习', '{"type":"first_lesson"}', 0),
       ('abeebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '打卡7天', '🔥', '连续学习7天', '{"type":"streak","days":7}', 1),
       ('abeebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '百词斩', '💪', '累计学习100个单词', '{"type":"words_learned","count":100}', 2),
       ('abeebc99-9c0b-4ef8-bb6d-6bb9bd380a04', '学霸', '🧠', '答对率超过90%', '{"type":"accuracy","rate":0.9}', 3);

-- 每日推荐
INSERT INTO daily_recommendations (uuid, user_id, recommend_date, entity_type, entity_id, reason) VALUES
('aceebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 2, CURDATE(), 'word', 1, '间隔复习到期');

-- 学习计划
INSERT INTO learning_plans (uuid, name, description, target_level, duration_days, daily_word_count, sort_order)
VALUES ('adeebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'CET-4 30天冲刺', '针对四级核心词汇的30天突破计划', 'CET-4', 30, 20, 0),
       ('adeebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '考研词汇 60天', '考研英语核心词汇60天系统学习', '考研', 60, 15, 1),
       ('adeebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '雅思7分词汇', '雅思核心词汇，目标7分以上', '雅思7.0', 45, 25, 2);

-- 用户参与计划 (user_id=2, plan_id=1)
INSERT INTO user_plans (uuid, user_id, plan_id, current_day) VALUES
('aeeebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 2, 1, 3);

-- 单词本
INSERT INTO word_books (uuid, name, description, difficulty_level, word_count, is_active, sort_order)
VALUES ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '四级单词', '大学英语四级核心词汇', 'CET-4', 2500, 1, 0),
       ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '考研单词', '考研英语核心词汇', '考研', 3500, 1, 1),
       ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '雅思词汇', '雅思核心词汇', '雅思', 4000, 1, 2);

-- 单词本词条 (word_book_id=1 = 四级单词, word_id=1 = abandon)
INSERT INTO word_book_entries (uuid, word_book_id, word_id, sort_order) VALUES
('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 1, 1, 0);

-- 用户单词本进度
INSERT INTO user_word_book_progress (uuid, user_id, word_book_id, strategy_id, daily_count, current_position)
VALUES ('b3eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 2, 1, 1, 10, 0);
