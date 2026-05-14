-- ============================================================
-- 🎯 英语单词知识库 — 一键建库 v4
-- 6 主表 + 4 辅助表 | 无外键 | SM-2 间隔重复
-- 修复: synonyms/tags 独立表、ease_factor、拼写变体等
-- ============================================================

CREATE DATABASE IF NOT EXISTS word_learning
  DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE word_learning;

-- ============================================================
-- 清理（按依赖顺序）
-- ============================================================
DROP TABLE IF EXISTS word_variants;
DROP TABLE IF EXISTS word_tags;
DROP TABLE IF EXISTS word_forms;
DROP TABLE IF EXISTS word_relations;
DROP TABLE IF EXISTS examples;
DROP TABLE IF EXISTS prep_patterns;
DROP TABLE IF EXISTS collocations;
DROP TABLE IF EXISTS usage_notes;
DROP TABLE IF EXISTS definitions;
DROP TABLE IF EXISTS words;

-- ============================================================
-- 1. 单词主表
-- ============================================================
CREATE TABLE words (
    id              INT             AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
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
    frequency       TINYINT         NOT NULL DEFAULT 0 COMMENT '频次 0-4' CHECK (frequency BETWEEN 0 AND 4),

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
    added_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入知识库时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改',

    UNIQUE KEY uk_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='词汇主表（含 SM-2 间隔重复）';

-- ============================================================
-- 2. 释义表
-- ============================================================
CREATE TABLE definitions (
    id              INT             AUTO_INCREMENT PRIMARY KEY,
    word_id         INT             NOT NULL COMMENT '→ words.id',
    meaning_en      TEXT            NOT NULL COMMENT '英文释义',
    meaning_cn      VARCHAR(500)    NOT NULL COMMENT '中文释义',
    pos_detail      VARCHAR(30)     NULL     COMMENT '细分词性（与主表 pos 不同时填写）',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    INDEX idx_definitions_word (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='释义表（一词多义）';

-- ============================================================
-- 3. 用法说明表
-- ============================================================
CREATE TABLE usage_notes (
    id              INT             AUTO_INCREMENT PRIMARY KEY,
    word_id         INT             NOT NULL COMMENT '→ words.id',
    note_en         TEXT            NOT NULL COMMENT '英文说明',
    note_cn         VARCHAR(500)    NOT NULL COMMENT '中文说明',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    INDEX idx_usage_word (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用法说明表';

-- ============================================================
-- 4. 固定搭配表
-- ============================================================
CREATE TABLE collocations (
    id              INT             AUTO_INCREMENT PRIMARY KEY,
    word_id         INT             NOT NULL COMMENT '→ words.id',
    collocation     VARCHAR(200)    NOT NULL COMMENT '搭配（英文）',
    translation     VARCHAR(200)    NOT NULL COMMENT '翻译（中文）',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    INDEX idx_colloc_word (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固定搭配表';

-- ============================================================
-- 5. 介词搭配表
-- ============================================================
CREATE TABLE prep_patterns (
    id              INT             AUTO_INCREMENT PRIMARY KEY,
    word_id         INT             NOT NULL COMMENT '→ words.id（通常为动词）',
    pattern         VARCHAR(200)    NOT NULL COMMENT '介词模式（英文）',
    translation     VARCHAR(200)    NOT NULL COMMENT '中文翻译',
    preposition     VARCHAR(20)     NULL     COMMENT '核心介词',
    INDEX idx_prep_word (word_id),
    INDEX idx_prep_preposition (preposition)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='介词搭配表';

-- ============================================================
-- 6. 例句表
-- ============================================================
CREATE TABLE examples (
    id              INT             AUTO_INCREMENT PRIMARY KEY,
    word_id         INT             NOT NULL COMMENT '→ words.id',
    sentence_en     TEXT            NOT NULL COMMENT '英文例句',
    sentence_cn     TEXT            NOT NULL COMMENT '中文翻译',
    source_type     ENUM('CET46','KAOYAN','TOEFL','IELTS','ACADEMIC','COMMON') NULL COMMENT '来源分类',
    source_detail   VARCHAR(200)    NULL     COMMENT '具体出处（如 CET-4 2021-06, Nature 2021）',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    INDEX idx_example_word (word_id),
    INDEX idx_example_source (source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='例句表';

-- ============================================================
-- 7. 单词关系网络表（同义词/反义词等）
-- ============================================================
CREATE TABLE word_relations (
    id              INT             AUTO_INCREMENT PRIMARY KEY,
    word_id         INT             NOT NULL COMMENT '→ words.id（源词）',
    related_word_id INT             NOT NULL COMMENT '→ words.id（关联词）',
    relation_type   ENUM('synonym','antonym','hyponym','hypernym','derivative','see_also') NOT NULL COMMENT '关系类型',
    UNIQUE KEY uk_relation (word_id, related_word_id, relation_type),
    INDEX idx_rel_word (word_id),
    INDEX idx_rel_related (related_word_id),
    INDEX idx_rel_type (relation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词关系网络表';

-- ============================================================
-- 8. 单词标签表
-- ============================================================
CREATE TABLE word_tags (
    word_id         INT             NOT NULL COMMENT '→ words.id',
    tag             VARCHAR(30)     NOT NULL COMMENT '标签 (如 CET-4, 考研, 科技, 法律)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (word_id, tag),
    INDEX idx_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词标签表';

-- ============================================================
-- 9. 单词变形表
-- ============================================================
CREATE TABLE word_forms (
    id              INT             AUTO_INCREMENT PRIMARY KEY,
    word_id         INT             NOT NULL COMMENT '→ words.id',
    form_type       VARCHAR(20)     NOT NULL COMMENT 'past/participle/third/gerund/plural/comparative/superlative',
    form_value      VARCHAR(50)     NOT NULL COMMENT '变形值',
    UNIQUE KEY uk_form (word_id, form_type),
    INDEX idx_form_word (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词变形表';

-- ============================================================
-- 10. 拼写变体表
-- ============================================================
CREATE TABLE word_variants (
    id              INT             AUTO_INCREMENT PRIMARY KEY,
    word_id         INT             NOT NULL COMMENT '→ words.id',
    variant         VARCHAR(50)     NOT NULL COMMENT '变体拼写',
    region          VARCHAR(20)     NULL     COMMENT 'en-US / en-GB / en-AU',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_variant (word_id, variant, region),
    INDEX idx_variant_word (word_id),
    INDEX idx_variant (variant)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼写变体表';

-- ============================================================
-- 索引
-- ============================================================
ALTER TABLE words ADD INDEX idx_letter (first_letter);
ALTER TABLE words ADD INDEX idx_pos (pos);
ALTER TABLE words ADD INDEX idx_difficulty (difficulty);
ALTER TABLE words ADD INDEX idx_source (source);
ALTER TABLE words ADD INDEX idx_stage (stage);
ALTER TABLE words ADD INDEX idx_next_review (next_review);
ALTER TABLE words ADD INDEX idx_added_at (added_at);
ALTER TABLE words ADD INDEX idx_stage_review (stage, next_review) COMMENT '复习队列复合索引';
ALTER TABLE words ADD INDEX idx_letter_pos (first_letter, pos) COMMENT '字母+词性复合索引';
ALTER TABLE words ADD INDEX idx_confidence (confidence) COMMENT '掌握度筛选';
ALTER TABLE words ADD INDEX idx_frequency (frequency) COMMENT '频次筛选';

-- ============================================================
-- 示例数据：abandon
-- ============================================================
INSERT INTO words (id, word, pos, first_letter, phonetic_uk, meaning_cn,
                   etymology, etymology_cn, source, difficulty, frequency,
                   stage, confidence, review_count, consecutive_correct, ease_factor, interval_days)
VALUES (1, 'abandon', 'vt.', 'A', '/əˈbændən/',
        '放弃；遗弃；抛弃',
        'From Old French abandoner (mettre à bandon — "to put under control")',
        '源自古法语 abandoner，意为"置于控制之下"',
        'CET-4', 2, 3,
        2, 3, 5, 3, 2.50, 6);

INSERT INTO definitions (word_id, meaning_en, meaning_cn) VALUES
(1, 'to leave completely and finally; to give up', '放弃；遗弃；抛弃');

INSERT INTO usage_notes (word_id, note_en, note_cn, sort_order) VALUES
(1, 'Used with a direct object: abandon sth/sb', '及物动词，后直接跟宾语', 0),
(1, '"Abandon oneself to sth" means to give in completely', '沉溺于某种情感', 1);

INSERT INTO collocations (word_id, collocation, translation) VALUES
(1, 'abandon hope',     '放弃希望'),
(1, 'abandon ship',     '弃船'),
(1, 'abandon a plan',   '放弃计划');

INSERT INTO prep_patterns (word_id, pattern, translation, preposition) VALUES
(1, 'abandon sth to sb',   '把某物丢给某人',  'to'),
(1, 'abandon sth for sth', '放弃A选择B',     'for'),
(1, 'abandon hope of',     '放弃…的希望',    'of');

INSERT INTO examples (word_id, sentence_en, sentence_cn, source_type, source_detail) VALUES
(1, 'The captain ordered the crew to abandon the sinking ship.',
    '船长命令船员弃船。', 'CET46', 'CET-4 2019-06'),
(1, 'He abandoned his research after years of fruitless effort.',
    '经过多年努力，他放弃了研究。', 'KAOYAN', '考研英语 2018'),
(1, 'Many residents were forced to abandon their homes due to the volcanic eruption.',
    '许多居民因火山爆发撤离家园。', 'TOEFL', 'TOEFL TPO 31'),
(1, 'The study had to be abandoned due to insufficient funding.',
    '该研究因资金不足而中止。', 'ACADEMIC', 'Nature 2019'),
(1, 'Don''t abandon your dreams just because of one setback.',
    '不要因为一次挫折就放弃梦想。', 'COMMON', NULL);

-- 词关系（abandon 的同为 synonym，需先插入关联词，此处仅为演示表结构）
-- 实际插入前请确保关联词已存在于 words 表:
-- INSERT INTO word_relations (word_id, related_word_id, relation_type)
-- VALUES (1, (SELECT id FROM words WHERE word = 'desert'), 'synonym');

-- 标签
INSERT INTO word_tags (word_id, tag) VALUES
(1, 'CET-4'),
(1, '考研核心'),
(1, '高频动词');
