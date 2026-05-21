-- ============================================================
-- 英语单词知识库 — 一键建库 v6
-- 15 表 | UUID 主键 | 全表统一 created_at/updated_at
-- ============================================================

CREATE DATABASE IF NOT EXISTS word_learning
  DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE word_learning;

-- ============================================================
-- 清理（按依赖顺序）
-- ============================================================
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
    id              CHAR(36)        NOT NULL PRIMARY KEY COMMENT 'UUID 主键',
    username        VARCHAR(50)     NOT NULL COMMENT '用户名',
    password_hash   VARCHAR(255)    NOT NULL COMMENT '密码哈希',
    email           VARCHAR(200)    NULL     COMMENT '邮箱',
    nickname        VARCHAR(100)    NULL     COMMENT '昵称',
    role            ENUM('admin','editor','user') NOT NULL DEFAULT 'user' COMMENT '角色',
    permission_level TINYINT        NOT NULL DEFAULT 1 COMMENT '权限级别 1=普通 5=编辑 9=管理员',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否激活',
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
-- 3. 释义表
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
-- 4. 用法说明表
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
-- 5. 固定搭配表
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
-- 6. 介词搭配表
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
-- 7. 例句表
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
-- 8. 单词关系网络表
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
-- 9. 单词标签表
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
-- 10. 单词变形表
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
-- 11. 拼写变体表
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
-- 12. 英文文章表
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
-- 示例数据：用户
-- ============================================================
INSERT INTO users (id, username, password_hash, nickname, role, permission_level)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'admin', '$2y$10$placeholder', '管理员', 'admin', 9),
       ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'demo',  '$2y$10$placeholder', '演示用户', 'user', 1);

-- ============================================================
-- 示例数据：单词 abandon
-- ============================================================
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

-- 标签
INSERT INTO word_tags (word_id, tag) VALUES
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'CET-4'),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '考研核心'),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '高频动词');

-- ============================================================
-- 示例数据：admin 默认收藏夹
-- ============================================================
INSERT INTO favorite_folders (id, user_id, name, category, is_default, sort_order) VALUES
('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '稍后复习', 'word', 1, 0),
('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '精彩例句', 'example', 0, 1),
('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '地道搭配', 'phrase', 0, 2),
('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '好文收藏', 'article', 0, 3);

-- 收藏条目
INSERT INTO favorites (id, folder_id, entity_type, entity_id, note) VALUES
('a2eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
 'word', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '高频词，需要重点掌握');
