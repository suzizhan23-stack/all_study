# 英语单词知识库 — 数据库设计文档 v6

> 系统名称：English Word Learning System  
> 数据库：word_learning  
> 版本：v6  
> 字符集：utf8mb4 / utf8mb4_unicode_ci  
> 引擎：InnoDB  
> 主键策略：UUID (CHAR(36))

---

## 目录

1. [ER 图 (ASCII)](#1-er-图-ascii)
2. [架构概览 — 分层图](#2-架构概览--分层图)
3. [表清单](#3-表清单)
4. [核心实体详解](#4-核心实体详解)
5. [关系矩阵](#5-关系矩阵)
6. [频率体系设计](#6-频率体系设计)
7. [收藏夹体系设计](#7-收藏夹体系设计)
8. [用户权限模型](#8-用户权限模型)
9. [间隔重复 (SM-2) 说明](#9-间隔重复-sm-2-说明)
10. [索引策略](#10-索引策略)
11. [数据流图](#11-数据流图)
12. [变更日志 v5→v6](#12-变更日志-v5v6)

---

## 1. ER 图 (ASCII)

### 1.1 全局实体关系

```
  ┌──────────────────────────────────────────────────────────────────────────┐
  │                           word_learning  ER 图                           │
  │                                                                          │
  │    ┌───────────┐    ┌──────────────┐    ┌────────────┐                  │
  │    │  words    │───<│ definitions  │    │ word_tags  │                  │
  │    │  (主)     │    └──────────────┘    └────────────┘                  │
  │    └────┬──────┘                              ▲                         │
  │         │                                     │                         │
  │         │──────────────┐                      │                         │
  │         │              │                      │                         │
  │         ▼              ▼                      │                         │
  │    ┌───────────┐  ┌───────────┐    ┌──────────┴─────────┐              │
  │    │examples   │  │usage_     │    │  word_relations    │              │
  │    │           │  │notes      │    │  (自引用多对多)     │              │
  │    └─────┬─────┘  └───────────┘    └────────────────────┘              │
  │          │                                                              │
  │          │ (article_id)           ┌──────────────┐                     │
  │          └────────────► articles  │ word_forms   │                     │
  │                               │   └──────────────┘                     │
  │                               │   ┌──────────────┐                     │
  │                               │   │ word_variants │                    │
  │                               │   └──────────────┘                     │
  │                               ▼                                        │
  │    ┌───────────┐    ┌──────────────┐    ┌────────────┐                 │
  │    │colloca-   │    │prep_patterns │    │  ...others │                 │
  │    │tions      │    │              │    └────────────┘                 │
  │    └───────────┘    └──────────────┘                                   │
  │                                                                          │
  │    ┌──────────────────────────────────────────────────────────────┐     │
  │    │                    用户体系                                    │     │
  │    │  ┌─────────┐     ┌──────────────┐     ┌──────────────────┐  │     │
  │    │  │  users  │────<│user_frequencies│    │ favorite_folders │  │     │
  │    │  └─────────┘     └──────────────┘     └────────┬─────────┘  │     │
  │    │                                                   │           │     │
  │    │                                                ┌──▼────────┐ │     │
  │    │                                                │ favorites │ │     │
  │    │                                                └───────────┘ │     │
  │    └──────────────────────────────────────────────────────────────┘     │
  └──────────────────────────────────────────────────────────────────────────┘

  图例: ──── 1:1    ───< 1:N    >──< M:N
```

### 1.2 详细 ER 图（纵向展开）

```
  ┌══════════════════════════════════════════════════════════════════┐
  ║                         word_learning v6                         ║
  ║                    实体-关系 详细图 (ASCII)                       ║
  ║                   主键: UUID (CHAR(36))                          ║
  └══════════════════════════════════════════════════════════════════┘


  ┌─────────────────────────────────────────────────────────────────┐
  │  1. 核心词汇域                                                    │
  │                                                                  │
  │                          ┌─────────────────┐                     │
  │                          │      words       │                    │
  │                          │─────────────────│                    │
  │                          │PK id (CHAR(36))  │        ──── 标签 │
  │                          │ word (UNIQUE)    │        │          │
  │                          │ pos              │        ▼          │
  │                          │ first_letter     │  ┌──────────────┐│
  │                          │ phonetic_uk/us   │  │  word_tags   ││
  │   ┌────────────────      │ meaning_cn       │  │──────────────││
  │   │                     │ etymology         │  │PK word_id    ││
  │   │                     │ source            │  │PK tag        ││
  │   │   │                 │ difficulty        │  └──────────────┘│
  │   │                     │ frequency(默认)   │                   │
  │   │   │                 │ stage             │                   │
  │   │                     │ confidence        │                   │
  │   │   │                 │ review_count      │                   │
  │   │                     │ consecutive_..... │                   │
  │   │   │                 │ ease_factor       │                   │
  │   │                     │ interval_days     │                   │
  │   │   │                 │ last_reviewed_at  │                   │
  │   │                     │ next_review       │                   │
  │   │   │                 │ created_at        │                   │
  │   │                     │ updated_at        │                   │
  │   │   │                 └────────┬────────┘                     │
  │   │                             │                              │
  │   │          ┌──────────────────┼──────────────────────┐       │
  │   │          ▼                  ▼                      ▼       │
  │   │  ┌──────────────┐  ┌────────────────┐  ┌──────────────────┐│
  │   │  │ definitions  │  │  usage_notes   │  │  word_relations  ││
  │   │  │──────────────│  │────────────────│  │──────────────────││
  │   │  │PK id CHAR(36)│  │PK id CHAR(36)  │  │PK id CHAR(36)    ││
  │   │  │FK word_id    │  │FK word_id      │  │FK word_id        ││
  │   │  │ meaning_en   │  │ note_en        │  │FK related_word_id││
  │   │  │ meaning_cn   │  │ note_cn        │  │ relation_type    ││
  │   │  │ pos_detail   │  │ sort_order     │  │ created_at       ││
  │   │  │ sort_order   │  │ created_at     │  │ updated_at       ││
  │   │  │ created_at   │  │ updated_at     │  └──────────────────┘│
  │   │  │ updated_at   │  └────────────────┘                       │
  │   │  └──────────────┘                                           │
  │   │                                                              │
  │   │  ┌──────────────┐  ┌────────────────┐  ┌──────────────────┐ │
  │   │  │ collocations │  │ prep_patterns  │  │    examples      │ │
  │   │  │──────────────│  │────────────────│  │──────────────────│ │
  │   │  │PK id CHAR(36)│  │PK id CHAR(36)  │  │PK id CHAR(36)    │ │
  │   │  │FK word_id    │  │FK word_id      │  │FK word_id        │ │
  │   │  │ collocation  │  │ pattern        │  │ sentence_en      │ │
  │   │  │ translation  │  │ translation    │  │ sentence_cn      │ │
  │   │  │★ frequency   │  │ preposition    │  │ source_type      │ │
  │   │  │ sort_order   │  │★ frequency     │  │ source_detail    │ │
  │   │  │ created_at   │  │ created_at     │  │FK article_id──── │ │
  │   │  │ updated_at   │  │ updated_at     │  │★ frequency       │ │
  │   │  └──────────────┘  └────────────────┘  │ sort_order       │ │
  │   │                                        │ created_at       │ │
  │   │  ┌──────────────┐  ┌────────────────┐  │ updated_at       │ │
  │   │  │ word_forms   │  │ word_variants  │  └──────────────────┘ │
  │   │  │──────────────│  │────────────────│                       │
  │   │  │PK id CHAR(36)│  │PK id CHAR(36)  │        ┌────────────┐ │
  │   │  │FK word_id    │  │FK word_id      │        │  articles  │ │
  │   │  │ form_type    │  │ variant        │        │────────────│ │
  │   │  │ form_value   │  │ region         │        │PK id CHAR. │ │
  │   │  │ created_at   │  │ created_at     │        │ title      │ │
  │   │  │ updated_at   │  │ updated_at     │        │ author     │ │
  │   │  └──────────────┘  └────────────────┘        │ content    │ │
  │   │                                              │★ frequency │ │
  │   │                                              │ created_at  │ │
  │   │                                              │ updated_at  │ │
  │   │                                              └────────────┘ │
  │                                                                  │
  └──────────────────────────────────────────────────────────────────┘


  ┌──────────────────────────────────────────────────────────────────┐
  │  2. 用户域                                                       │
  │                                                                  │
  │     ┌──────────────────────────────────────────────────┐        │
  │     │                     users                         │        │
  │     │──────────────────────────────────────────────────│        │
  │     │  PK id CHAR(36)                                   │        │
  │     │  username (UNIQUE)                                │        │
  │     │  password_hash                                    │        │
  │     │  email (UNIQUE)                                   │        │
  │     │  nickname                                         │        │
  │     │  role (admin/editor/user)                         │        │
  │     │  permission_level (1/5/9)                         │        │
  │     │  is_active                                        │        │
  │     │  last_login_at                                    │        │
  │     │  created_at                                       │        │
  │     │  updated_at                                       │        │
  │     └──────────┬───────────────────────────────────────┘        │
  │                │                                                  │
  │        ┌───────┴────────┐                                         │
  │        ▼                ▼                                         │
  │  ┌────────────┐  ┌──────────────┐                                │
  │  │user_freq   │  │favorite_    │                                 │
  │  │uencies     │  │folders      │                                 │
  │  │────────────│  │──────────────│                                │
  │  │PK id CHAR  │  │PK id CHAR(36)│                               │
  │  │FK user_id  │  │FK user_id    │                                │
  │  │entity_type │  │ name         │                                │
  │  │entity_id   │  │ category     │                                │
  │  │★ frequency │  │ is_default   │                                │
  │  │ created_at │  │ sort_order   │                                │
  │  │ updated_at │  │ created_at   │                                │
  │  └────────────┘  │ updated_at   │                                │
  │                   └──────┬───────┘                               │
  │                          │                                        │
  │                     ┌────▼───────┐                               │
  │                     │ favorites  │                               │
  │                     │────────────│                               │
  │                     │PK id CHAR  │                               │
  │                     │FK folder_id│                               │
  │                     │entity_type │                               │
  │                     │entity_id   │                               │
  │                     │note        │                               │
  │                     │ created_at │                               │
  │                     │ updated_at │                               │
  │                     └────────────┘                               │
  └──────────────────────────────────────────────────────────────────┘


  ┌──────────────────────────────────────────────────────────────────┐
  │  3. 频率体系（逻辑视图）                                           │
  │                                                                  │
  │    ┌─────────────┐                                              │
  │    │ 实体表       │   words / collocations / prep_patterns      │
  │    │             │   examples / articles                        │
  │    │ frequency   │  ← 默认频率（系统级）                        │
  │    └──────┬──────┘                                              │
  │           │                                                      │
  │           │ 如果用户设置了个人频率，则覆盖默认                    │
  │           │                                                      │
  │    ┌──────▼──────┐                                              │
  │    │ user_freq   │  (user_id, entity_type, entity_id) CHAR(36) │
  │    │ uencies     │  frequency ← 个人频率                        │
  │    └─────────────┘                                              │
  │                                                                  │
  │    排序 SQL 模式:                                                │
  │      SELECT ..., COALESCE(uf.frequency, t.frequency) AS sort_freq│
  │      FROM ... t                                                  │
  │      LEFT JOIN user_frequencies uf                               │
  │        ON uf.entity_type = :type AND uf.entity_id = t.id        │
  │        AND uf.user_id = :uid                                     │
  │      ORDER BY sort_freq DESC                                     │
  └──────────────────────────────────────────────────────────────────┘
```

---

## 2. 架构概览 — 分层图

```
  ┌─────────────────────────────────────────────────────────────────┐
  │                    应用层 (Application)                          │
  │   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
  │   │ 单词学习  │ │ 文章阅读  │ │ 收藏管理  │ │  间隔重复(SM-2)│ │
  │   └────┬─────┘ └────┬─────┘ └────┬─────┘ └────────┬─────────┘ │
  └────────┼─────────────┼────────────┼────────────────┼───────────┘
           │             │            │                │
  ┌────────▼─────────────▼────────────▼────────────────▼───────────┐
  │                    服务层 (Service / DAO)                        │
  │  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ ┌─────────┐ │
  │  │WordRepo │ │ArticleRepo│ │FavRepo   │ │UserRepo│ │FreqRepo │ │
  │  └────┬────┘ └────┬─────┘ └────┬─────┘ └───┬────┘ └────┬────┘ │
  └───────┼───────────┼────────────┼────────────┼───────────┼──────┘
          │           │            │            │           │
  ┌───────▼───────────▼────────────▼────────────▼───────────▼──────┐
  │                    数据层 (Database)                             │
  │                                                                  │
  │  ┌──────────────── 核心词汇域 ───────────────────────────────┐  │
  │  │  words → {definitions, usage_notes, collocations,         │  │
  │  │           prep_patterns, examples, word_relations,        │  │
  │  │           word_forms, word_variants, word_tags}           │  │
  │  │  主键: UUID CHAR(36)，所有外键 UUID 一致                     │  │
  │  └───────────────────────────────────────────────────────────┘  │
  │                                                                  │
  │  ┌──────────────── 文章域 ──────────────────────────────────┐  │
  │  │  articles ← examples (article_id)                         │  │
  │  └───────────────────────────────────────────────────────────┘  │
  │                                                                  │
  │  ┌──────────────── 用户域 ──────────────────────────────────┐  │
  │  │  users → user_frequencies → {words, collocations, ...}    │  │
  │  │  users → favorite_folders → favorites → {任意实体}        │  │
  │  └───────────────────────────────────────────────────────────┘  │
  └─────────────────────────────────────────────────────────────────┘
```

---

## 3. 表清单

| # | 表名 | 类型 | 主键 | 说明 |
|---|---|---|---|---|
| 1 | `users` | 主表 | UUID | 账号+权限 |
| 2 | `words` | 主表 | UUID | 单词主表，含 SM-2 |
| 3 | `definitions` | 从表 | UUID | 一词多义 |
| 4 | `usage_notes` | 从表 | UUID | 用法说明 |
| 5 | `collocations` | 从表 | UUID | 固定搭配，**有频率** |
| 6 | `prep_patterns` | 从表 | UUID | 介词搭配，**有频率** |
| 7 | `examples` | 从表 | UUID | 例句，可关联文章，**有频率** |
| 8 | `word_relations` | 从表 | UUID | 同/反义词网络 |
| 9 | `word_tags` | 从表 | (UUID,tag) | 标签多对多 |
| 10 | `word_forms` | 从表 | UUID | 时态/复数/比较级 |
| 11 | `word_variants` | 从表 | UUID | 美式/英式拼写 |
| 12 | `articles` | 主表 | UUID | 英文文章，**有频率** |
| 13 | `user_frequencies` | 辅助表 | UUID | 个人频率覆盖 |
| 14 | `favorite_folders` | 主表 | UUID | 收藏夹分类 |
| 15 | `favorites` | 从表 | UUID | 收藏条目（多态） |

**总计：15 表 | 全部 UUID 主键 | 全部含 created_at/updated_at**

---

## 4. 核心实体详解

### 4.1 words — 单词主表

```
┌────────────────┬──────────────────┬────────────┬────────────────────────┐
│ 字段           │ 类型             │ 约束        │ 说明                   │
├────────────────┼──────────────────┼────────────┼────────────────────────┤
│ id             │ CHAR(36)         │ PK          │ UUID 主键              │
│ word           │ VARCHAR(50)      │ NOT NULL    │ 单词（唯一）           │
│                │                  │ UNIQUE      │                        │
│ pos            │ VARCHAR(30)      │ NOT NULL    │ 词性                   │
│ first_letter   │ CHAR(1)          │ NOT NULL    │ 首字母分区             │
│ phonetic_uk    │ VARCHAR(100)     │ NULL        │ 英式音标               │
│ phonetic_us    │ VARCHAR(100)     │ NULL        │ 美式音标               │
│ audio_uk       │ VARCHAR(500)     │ NULL        │ 英式发音 URL           │
│ audio_us       │ VARCHAR(500)     │ NULL        │ 美式发音 URL           │
│ meaning_cn     │ VARCHAR(500)     │ NULL        │ 中文摘要（冗余）       │
│ etymology      │ TEXT             │ NULL        │ 英文词源               │
│ etymology_cn   │ TEXT             │ NULL        │ 中文词源               │
│ source         │ VARCHAR(50)      │ NULL        │ 来源词表               │
│ difficulty     │ TINYINT          │ 0-4         │ 难度                   │
│ frequency      │ INT              │ 默认 0      │ ★ 系统默认频率         │
│ stage          │ TINYINT          │ 0-3         │ SM-2 学习阶段          │
│ confidence     │ TINYINT          │ 0-5         │ 掌握度                 │
│ review_count   │ INT              │ 默认 0      │ 复习次数               │
│ consec_correct │ INT              │ 默认 0      │ 连续正确（SM-2）       │
│ ease_factor    │ DECIMAL(4,2)     │ 1.3-3.0     │ 难度系数               │
│ interval_days  │ INT              │ 默认 0      │ 间隔天数               │
│ last_reviewed  │ DATETIME         │ NULL        │ 上次复习               │
│ next_review    │ DATETIME         │ NULL        │ 下次复习               │
│ created_at     │ DATETIME         │ DEFAULT     │ ★ 创建时间             │
│ updated_at     │ DATETIME         │ ON UPDATE   │ ★ 修改时间             │
└────────────────┴──────────────────┴────────────┴────────────────────────┘
```

### 4.2 users — 用户表

```
┌────────────────┬──────────────────┬────────────┬────────────────────────┐
│ 字段           │ 类型             │ 约束        │ 说明                   │
├────────────────┼──────────────────┼────────────┼────────────────────────┤
│ id             │ CHAR(36)         │ PK          │ UUID 主键              │
│ username       │ VARCHAR(50)      │ UNIQUE      │ 用户名                 │
│ password_hash  │ VARCHAR(255)     │ NOT NULL    │ bcrypt 哈希            │
│ email          │ VARCHAR(200)     │ UNIQUE      │ 邮箱                   │
│ nickname       │ VARCHAR(100)     │ NULL        │ 昵称                   │
│ role           │ ENUM(admin,      │ DEFAULT     │ 角色                   │
│                │      editor,user)│ 'user'      │                        │
│ permission_    │ TINYINT          │ DEFAULT 1   │ 1=普通 5=编辑 9=管理员 │
│ level          │                  │             │                        │
│ is_active      │ TINYINT(1)       │ DEFAULT 1   │ 是否激活               │
│ last_login_at  │ DATETIME         │ NULL        │ 最后登录时间           │
│ created_at     │ DATETIME         │ DEFAULT     │ ★ 注册时间             │
│ updated_at     │ DATETIME         │ ON UPDATE   │ ★ 更新时间             │
└────────────────┴──────────────────┴────────────┴────────────────────────┘

权限级别对照:
  ┌─────────┬──────────┬─────────────────────────────────────────┐
  │ 级别     │ 角色      │ 可执行操作                               │
  ├─────────┼──────────┼─────────────────────────────────────────┤
  │ 1       │ user     │ 学习、收藏、设置个人频率                   │
  │ 5       │ editor   │ 增删改词库/文章内容                        │
  │ 9       │ admin    │ 管理用户、系统配置                         │
  └─────────┴──────────┴─────────────────────────────────────────┘
```

### 4.3 articles — 文章表

```
┌────────────────┬──────────────────┬────────────┬────────────────────────┐
│ 字段           │ 类型             │ 约束        │ 说明                   │
├────────────────┼──────────────────┼────────────┼────────────────────────┤
│ id             │ CHAR(36)         │ PK          │ UUID 主键              │
│ title          │ VARCHAR(500)     │ NOT NULL    │ 文章标题               │
│ author         │ VARCHAR(200)     │ NULL        │ 作者                   │
│ content        │ TEXT             │ NOT NULL    │ 正文                   │
│ summary        │ TEXT             │ NULL        │ 摘要                   │
│ source_url     │ VARCHAR(500)     │ NULL        │ 原文链接               │
│ source_name    │ VARCHAR(200)     │ NULL        │ 来源媒体               │
│ difficulty     │ TINYINT          │ 0-4         │ 难度                   │
│ frequency      │ INT              │ 默认 0      │ ★ 系统默认频率         │
│ word_count     │ INT              │ NULL        │ 单词数                 │
│ language_level │ VARCHAR(20)      │ NULL        │ 语言等级               │
│ created_at     │ DATETIME         │ DEFAULT     │ ★ 入库时间             │
│ updated_at     │ DATETIME         │ ON UPDATE   │ ★ 最后修改             │
└────────────────┴──────────────────┴────────────┴────────────────────────┘
```

### 4.4 favorite_folders — 收藏夹表

```
┌────────────────┬──────────────────────────┬────────────┬────────────────┐
│ 字段           │ 类型                     │ 约束        │ 说明           │
├────────────────┼──────────────────────────┼────────────┼────────────────┤
│ id             │ CHAR(36)                 │ PK          │ UUID 主键      │
│ user_id        │ CHAR(36)                 │ FK          │ 所属用户       │
│ name           │ VARCHAR(100)             │ NOT NULL    │ 收藏夹名称     │
│ category       │ ENUM(word,example,       │ DEFAULT     │ 收藏类别       │
│                │      phrase,article,other)│ 'other'    │                │
│ is_default     │ TINYINT(1)               │ 默认 0      │ 系统默认夹     │
│ sort_order     │ INT                      │ 默认 0      │ 排序序号       │
│ created_at     │ DATETIME                 │ DEFAULT     │ ★ 创建时间     │
│ updated_at     │ DATETIME                 │ ON UPDATE   │ ★ 更新时间     │
└────────────────┴──────────────────────────┴────────────┴────────────────┘

类别 -- 实体类型对应关系:
  word    ──→ words
  example ──→ examples
  phrase  ──→ collocations + prep_patterns
  article ──→ articles
  other   ──→ 任意（预留）

默认收藏夹（注册时自动创建）:
  "稍后复习" → category=word, is_default=1
```

### 4.5 favorites — 收藏条目表

```
┌────────────────┬──────────────────────────────┬────────────┬────────────┐
│ 字段           │ 类型                         │ 约束        │ 说明       │
├────────────────┼──────────────────────────────┼────────────┼────────────┤
│ id             │ CHAR(36)                     │ PK          │ UUID 主键  │
│ folder_id      │ CHAR(36)                     │ FK          │ 所属收藏夹 │
│ entity_type    │ ENUM(word,collocation,       │ NOT NULL    │ 多态类型   │
│                │      prep_pattern,example,   │             │            │
│                │      article)                │             │            │
│ entity_id      │ CHAR(36)                     │ NOT NULL    │ 实体 UUID  │
│ note           │ TEXT                         │ NULL        │ 备注       │
│ created_at     │ DATETIME                     │ DEFAULT     │ ★ 收藏时间 │
│ updated_at     │ DATETIME                     │ ON UPDATE   │ ★ 更新时间 │
└────────────────┴──────────────────────────────┴────────────┴────────────┘

唯一约束: (folder_id, entity_type, entity_id) —— 同一夹子内不重复
```

### 4.6 user_frequencies — 个人频率表

```
┌────────────────┬──────────────────────────────┬────────────┬────────────────┐
│ 字段           │ 类型                         │ 约束        │ 说明           │
├────────────────┼──────────────────────────────┼────────────┼────────────────┤
│ id             │ CHAR(36)                     │ PK          │ UUID 主键      │
│ user_id        │ CHAR(36)                     │ FK          │ 用户           │
│ entity_type    │ ENUM(word,collocation,       │ NOT NULL    │ 实体类型       │
│                │      prep_pattern,example,   │             │                │
│                │      article)                │             │                │
│ entity_id      │ CHAR(36)                     │ NOT NULL    │ 实体 UUID      │
│ frequency      │ INT                          │ 默认 0      │ ★ 个人频率     │
│ created_at     │ DATETIME                     │ DEFAULT     │ ★ 创建时间     │
│ updated_at     │ DATETIME                     │ ON UPDATE   │ ★ 更新时间     │
└────────────────┴──────────────────────────────┴────────────┴────────────────┘

唯一约束: (user_id, entity_type, entity_id) —— 一用户对一实体只有一个频率
```

---

## 5. 关系矩阵

| 父表 | 子表 | 关系 | 外键 | 外键类型 |
|---|---|---|---|---|
| `words` | `definitions` | 1:N | `word_id` | CHAR(36) |
| `words` | `usage_notes` | 1:N | `word_id` | CHAR(36) |
| `words` | `collocations` | 1:N | `word_id` | CHAR(36) |
| `words` | `prep_patterns` | 1:N | `word_id` | CHAR(36) |
| `words` | `examples` | 1:N | `word_id` | CHAR(36) |
| `words` | `word_relations.word_id` | 1:N | `word_id` | CHAR(36) |
| `words` | `word_relations.related_word_id` | 1:N | `related_word_id` | CHAR(36) |
| `words` | `word_tags` | 1:N | `word_id` | CHAR(36) |
| `words` | `word_forms` | 1:N | `word_id` | CHAR(36) |
| `words` | `word_variants` | 1:N | `word_id` | CHAR(36) |
| `articles` | `examples` | 1:N | `article_id` | CHAR(36) |
| `users` | `favorite_folders` | 1:N | `user_id` | CHAR(36) |
| `users` | `user_frequencies` | 1:N | `user_id` | CHAR(36) |
| `favorite_folders` | `favorites` | 1:N | `folder_id` | CHAR(36) |
| — | `favorites.entity_type` | 多态 | `entity_id` | CHAR(36) |

**所有主键与外键统一使用 CHAR(36) UUID**，保证类型一致。
多态关联 `(entity_type, entity_id)` 中 entity_id 也是 CHAR(36)，
与其他表主键类型一致。

---

## 6. 频率体系设计

### 6.1 设计目标

1. **统一频率字段**：所有可排序实体都有 `frequency INT` 作为**系统默认频率**
2. **个人覆盖**：`user_frequencies` 表允许用户独立设置个人频率
3. **回退机制**：没有个人频率时，自动使用默认频率

### 6.2 数据模型

```
实体表                          user_frequencies
┌────────────────┐            ┌──────────────────────────────┐
│ words          │            │ user_id CHAR(36)             │
│  ├ frequency   │            │ entity_type                  │
│ collocations   │◄──LEFT────│ entity_id CHAR(36)           │
│  ├ frequency   │    JOIN   │ frequency (个人)              │
│ prep_patterns  │            └──────────────────────────────┘
│  ├ frequency   │
│ examples       │
│  ├ frequency   │
│ articles       │
│  └ frequency   │
└────────────────┘
```

### 6.3 查询模式

```sql
-- 排序示例：单词列表（UUID 查询）
SELECT w.*,
       COALESCE(uf.frequency, w.frequency) AS sort_freq
FROM words w
LEFT JOIN user_frequencies uf
    ON uf.entity_type = 'word'
   AND uf.entity_id = w.id
   AND uf.user_id = ?
ORDER BY sort_freq DESC, w.word ASC;

-- 排序示例：搭配列表（按频率降序）
SELECT c.*,
       COALESCE(uf.frequency, c.frequency) AS sort_freq
FROM collocations c
LEFT JOIN user_frequencies uf
    ON uf.entity_type = 'collocation'
   AND uf.entity_id = c.id
   AND uf.user_id = ?
ORDER BY sort_freq DESC, c.sort_order ASC;
```

### 6.4 频率语义

| 频率值 | 语义 | 示例 |
|---|---|---|
| 0 | 未评/默认 | 新词条 |
| 1-20 | 低频 | 罕见词汇 |
| 21-50 | 中低频 | 专业术语 |
| 51-80 | 中频 | 常用词 |
| 81-100 | 高频 | 核心词汇 |

---

## 7. 收藏夹体系设计

### 7.1 数据结构

```
users (1) ──── (N) favorite_folders (1) ──── (N) favorites

favorite_folders:
  - id:        CHAR(36) UUID
  - name:      "稍后复习" / "阅读精粹" ...
  - category:  word | example | phrase | article | other
  - is_default: 每个用户仅一个默认夹

favorites:
  - entity_type + entity_id (CHAR(36)) -> 多态关联任意实体
  - note: 用户备注
```

### 7.2 用户注册流程

```
用户注册
  │
  ├── INSERT INTO users (id, username, password_hash, ...)
  │     VALUES (UUID(), 'demo', '...', ...)
  │
  └── INSERT INTO favorite_folders (id, user_id, name='稍后复习',
                                    category='word', is_default=1)
        VALUES (UUID(), @user_id, '稍后复习', 'word', 1)
```

### 7.3 查询模式

```sql
-- 查询用户的收藏夹树
SELECT
    f.id AS folder_id,
    f.name AS folder_name,
    f.category,
    COUNT(fav.id) AS item_count
FROM favorite_folders f
LEFT JOIN favorites fav ON fav.folder_id = f.id
WHERE f.user_id = ?
GROUP BY f.id, f.name, f.category
ORDER BY f.sort_order ASC;

-- 查询收藏夹内具体内容
SELECT
    fav.id AS fav_id,
    fav.entity_type,
    fav.entity_id,
    fav.note,
    fav.created_at,
    CASE fav.entity_type
        WHEN 'word'        THEN w.word
        WHEN 'collocation' THEN col.collocation
        WHEN 'prep_pattern' THEN pp.pattern
        WHEN 'example'     THEN e.sentence_en
        WHEN 'article'     THEN a.title
    END AS entity_title
FROM favorites fav
LEFT JOIN words          w  ON fav.entity_type='word'        AND fav.entity_id = w.id
LEFT JOIN collocations   col ON fav.entity_type='collocation' AND fav.entity_id = col.id
LEFT JOIN prep_patterns  pp  ON fav.entity_type='prep_pattern' AND fav.entity_id = pp.id
LEFT JOIN examples       e   ON fav.entity_type='example'    AND fav.entity_id = e.id
LEFT JOIN articles       a   ON fav.entity_type='article'    AND fav.entity_id = a.id
WHERE fav.folder_id = ?
ORDER BY fav.created_at DESC;
```

---

## 8. 用户权限模型

### 8.1 权限矩阵

```
┌─────────────────────┬──────────┬──────────┬──────────┐
│ 操作                │ user(1)  │ editor(5)│ admin(9) │
├─────────────────────┼──────────┼──────────┼──────────┤
│ 查看词库             │ ✅       │ ✅       │ ✅       │
│ 学习/复习           │ ✅       │ ✅       │ ✅       │
│ 收藏/取消收藏       │ ✅       │ ✅       │ ✅       │
│ 设置个人频率         │ ✅       │ ✅       │ ✅       │
│ 创建自定义收藏夹     │ ✅       │ ✅       │ ✅       │
│ ─────────────────── │ ─────── │ ─────── │ ─────── │
│ 新增/编辑单词       │ ❌       │ ✅       │ ✅       │
│ 新增/编辑文章       │ ❌       │ ✅       │ ✅       │
│ 批量导入           │ ❌       │ ✅       │ ✅       │
│ ─────────────────── │ ─────── │ ─────── │ ─────── │
│ 管理用户            │ ❌       │ ❌       │ ✅       │
│ 系统配置            │ ❌       │ ❌       │ ✅       │
│ 删除数据            │ ❌       │ ❌       │ ✅       │
└─────────────────────┴──────────┴──────────┴──────────┘
```

### 8.2 角色定义

```sql
ENUM('admin', 'editor', 'user')

-- 对应 permission_level:
--   'user'   → 1
--   'editor' → 5
--   'admin'  → 9
```

---

## 9. 间隔重复 (SM-2) 说明

SM-2 (SuperMemo 2) 算法参数集中在 `words` 表：

| 参数 | 字段 | 说明 |
|---|---|---|
| 学习阶段 | `stage` | 0=未学 → 1=学习中 → 2=复习中 → 3=已掌握 |
| 掌握度 | `confidence` | 0-5 自评/测试分数 |
| 连续正确 | `consecutive_correct` | SM-2 核心：连续答对次数 |
| 难度系数 | `ease_factor` | 1.3-3.0，答对增加，答错降低，最低 1.3 |
| 间隔天数 | `interval_days` | 当前复习间隔 |

```
SM-2 算法伪代码:

function sm2(quality: 0-5):
    if quality >= 3:                       // 答对
        consec_correct++
        if consec_correct == 1:  interval = 1
        elif consec_correct == 2: interval = 6
        else:                    interval = round(interval_prev * ease_factor)
    else:                                  // 答错
        consec_correct = 0
        interval = 1

    // 更新难度系数
    ease_factor = ease_factor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
    ease_factor = clamp(ease_factor, 1.3, 3.0)

    next_review = now + interval
```

---

## 10. 索引策略

### 10.1 单词表索引

```
┌────────────────────────────┬────────────┬──────────────────────────────┐
│ 索引名                     │ 字段        │ 用途                         │
├────────────────────────────┼────────────┼──────────────────────────────┤
│ idx_letter                 │ first_letter│ 字母分区查询                 │
│ idx_pos                    │ pos         │ 词性筛选                     │
│ idx_difficulty             │ difficulty  │ 难度筛选                     │
│ idx_source                 │ source      │ 来源词表筛选                 │
│ idx_stage                  │ stage       │ 学习阶段筛选                 │
│ idx_next_review            │ next_review  │ 到期复习查询                 │
│ idx_created_at             │ created_at  │ 最新入库排序                 │
│ idx_stage_review           │ stage,next_review │ 复习队列（复合）         │
│ idx_letter_pos             │ first_letter,pos  │ 字母+词性（复合）         │
│ idx_confidence             │ confidence  │ 掌握度筛选                   │
│ idx_frequency              │ frequency   │ 频率排序                     │
└────────────────────────────┴────────────┴──────────────────────────────┘
```

### 10.2 各表索引汇总

| 表 | 索引字段 | 索引类型 |
|---|---|---|
| `definitions` | `word_id` | B-tree |
| `usage_notes` | `word_id` | B-tree |
| `collocations` | `word_id` | B-tree |
| `prep_patterns` | `word_id`, `preposition` | B-tree |
| `examples` | `word_id`, `source_type`, `article_id` | B-tree |
| `word_relations` | `word_id`, `related_word_id`, `relation_type` | B-tree + UNIQUE |
| `word_tags` | `(word_id,tag)` PK, `tag` | B-tree |
| `word_forms` | `word_id`, `(word_id,form_type)` UNIQUE | B-tree |
| `word_variants` | `word_id`, `variant`, `(word_id,variant,region)` UNIQUE | B-tree |
| `articles` | `difficulty`, `frequency`, `language_level`, `created_at` | B-tree |
| `user_frequencies` | `user_id`, `(entity_type,entity_id)`, `(user_id,entity_type,entity_id)` UNIQUE | B-tree |
| `favorite_folders` | `user_id`, `category`, `(user_id,is_default)` | B-tree |
| `favorites` | `folder_id`, `(entity_type,entity_id)`, `(folder_id,entity_type,entity_id)` UNIQUE | B-tree |

---

## 11. 数据流图

### 11.1 用户学习流程

```
用户登录
  │
  ├── [单词学习] ──→ SELECT ... FROM words WHERE next_review <= NOW()
  │                      ORDER BY next_review ASC LIMIT 20
  │                   │
  │                   ├── 答对 → UPDATE ... SET consec_correct+1, ease_factor+
  │                   │          interval_days = SM2(...), next_review = ...
  │                   │
  │                   └── 答错 → UPDATE ... SET consec_correct=0, ease_factor-
  │                              interval_days=1, next_review = NOW()+1
  │
  ├── [查看详情] ──→ SELECT ... FROM words
  │                   LEFT JOIN definitions
  │                   LEFT JOIN collocations  ORDER BY frequency DESC
  │                   LEFT JOIN prep_patterns ORDER BY frequency DESC
  │                   LEFT JOIN examples      ORDER BY frequency DESC
  │                   WHERE words.id = ?
  │
  ├── [收藏] ──→ INSERT INTO favorites (id, folder_id, entity_type, entity_id)
  │                VALUES (UUID(), ?, 'word', ?)
  │
  └── [设置频率] ──→ INSERT INTO user_frequencies
                         (id, user_id, entity_type, entity_id, frequency)
                      VALUES (UUID(), ?, 'word', ?, 80)
                      ON DUPLICATE KEY UPDATE frequency = VALUES(frequency)
```

### 11.2 UUID 生成策略

```
应用层调用 UUID() 函数或应用代码生成 UUID v4:

  MySQL:   INSERT INTO words (id, ...) VALUES (UUID(), ...)
  Python:  import uuid; str(uuid.uuid4())
  Java:    java.util.UUID.randomUUID().toString()
  JS:      crypto.randomUUID()

所有 INSERT 示例使用显式 UUID 常量仅为演示，
生产环境应使用 UUID() 函数或应用层生成。
```

---

## 12. 变更日志 v5→v6

| 变更类型 | 对象 | 详细说明 |
|---|---|---|
| 🔄 修改 | **所有 15 表** | `INT AUTO_INCREMENT PK` → `CHAR(36) PK` (UUID) |
| 🔄 修改 | **所有外键** | `INT` → `CHAR(36)`，与主键类型一致 |
| 🔄 修改 | `definitions` | 新增 `created_at`, `updated_at` |
| 🔄 修改 | `usage_notes` | 新增 `created_at`, `updated_at` |
| 🔄 修改 | `collocations` | 新增 `created_at`, `updated_at` |
| 🔄 修改 | `prep_patterns` | 新增 `created_at`, `updated_at` |
| 🔄 修改 | `examples` | 新增 `created_at`, `updated_at` |
| 🔄 修改 | `word_relations` | 新增 `created_at`, `updated_at` |
| 🔄 修改 | `word_forms` | 新增 `created_at`, `updated_at` |
| 🔄 修改 | `word_variants` | 新增 `updated_at`（已有 `created_at`） |
| 🔄 修改 | `word_tags` | 新增 `updated_at`（已有 `created_at`） |
| 🔄 修改 | `favorite_folders` | 新增 `updated_at`（已有 `created_at`） |
| 🔄 修改 | `favorites` | `added_at` → `created_at`，新增 `updated_at` |
| 🔄 修改 | `user_frequencies` | 新增 `created_at`（已有 `updated_at`） |
| 🔄 修改 | `words` | `added_at` → `created_at` |
| 🔄 修改 | `articles` | `added_at` → `created_at` |
| 🔄 修改 | `words` 索引 | `idx_added_at` → `idx_created_at` |
| 🔄 修改 | `articles` 索引 | `idx_added_at` → `idx_created_at` |

**核心原则**：
- 全部 15 表统一使用 `CHAR(36) UUID` 做主键，**不用 INT**
- 全部 15 表统一含 `created_at` + `updated_at` 时间审计字段
- `entity_id` 在多态表（`favorites`、`user_frequencies`）中也是 `CHAR(36)`，与实体表主键一致

---

> **文档版本**: v6.0  
> **最后更新**: 2026-05-21  
> **设计者**: opencode  
> **数据库**: MySQL 8.0+ / MariaDB 10.5+  
> **字符集**: utf8mb4  
> **存储引擎**: InnoDB  
> **主键策略**: UUID v4 (CHAR(36))
