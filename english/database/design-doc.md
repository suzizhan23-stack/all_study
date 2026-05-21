# 英语单词知识库 — 数据库设计文档 v7

> 系统名称：English Word Learning System  
> 数据库：word_learning  
> 版本：v7 — 30 表完整版  
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
8. [用户体系全景](#8-用户体系全景)
9. [间隔重复 (SM-2) 说明](#9-间隔重复-sm-2-说明)
10. [索引策略](#10-索引策略)
11. [数据流图](#11-数据流图)
12. [变更日志 v6→v7](#12-变更日志-v6v7)

---

## 1. ER 图 (ASCII)

```
  ┌──────────────────────────────────────────────────────────────────────────────────┐
  │                           word_learning v7  ER 图                                  │
  │                                                                                    │
  │  ┌──────────────────────────── 核心词汇域 ──────────────────────────────────────┐ │
  │  │                                                                              │ │
  │  │    words ──┬── definitions    │  word_relations (自引用 M:N)                  │ │
  │  │            ├── usage_notes    │  word_tags (系统标签)          word_forms     │ │
  │  │            ├── collocations   │  word_variants                 examples ──┐   │ │
  │  │            └── prep_patterns  │                                        │   │ │
  │  │                                                                        │   │ │
  │  │                                     ┌──────────────────────────────────┘   │ │
  │  │                                     ▼                                      │ │
  │  │                              articles (文章)                                │ │
  │  └──────────────────────────────────────────────────────────────────────────────┘ │
  │                                                                                    │
  │  ┌──────────────────────────── 用户体系 ────────────────────────────────────────┐ │
  │  │                                                                              │ │
  │  │    users ──┬── user_settings          (键值对偏好)                            │ │
  │  │            ├── user_stats             (XP/等级/打卡)                          │ │
  │  │            ├── user_tags              (自定义标签) ── user_entity_tags ── 实体  │ │
  │  │            ├── user_notes             (个人笔记 → 多态)                        │ │
  │  │            ├── search_history         (搜索历史)                              │ │
  │  │            ├── content_ratings        (评分反馈 → 多态)                        │ │
  │  │            ├── user_frequencies       (个人频率 → 多态)                        │ │
  │  │            ├── favorite_folders ── favorites (收藏 → 多态)                     │ │
  │  │            ├── learning_activities    (每日学习日志)                           │ │
  │  │            ├── review_log             (答题日志 → words)                      │ │
  │  │            ├── reading_progress       (阅读进度 → articles)                   │ │
  │  │            ├── daily_recommendations  (每日推荐 → 多态)                        │ │
  │  │            ├── user_badges ── badges  (徽章成就)                              │ │
  │  │            └── user_plans ── learning_plans (学习计划)                         │ │
  │  └──────────────────────────────────────────────────────────────────────────────┘ │
  │                                                                                    │
  │      图例: ──── 1:1    ──┬── 1:N    ── 多态关联                                   │
  └──────────────────────────────────────────────────────────────────────────────────┘
```

### 多态关联说明

系统中多处使用 `(entity_type, entity_id)` 模式实现多态关联：

```
  user_notes           content_ratings       daily_recommendations
  ───────────          ───────────────       ─────────────────────
  entity_type ───────┐ entity_type ────────┐ entity_type ────────┐
  entity_id   ───────┤ entity_id   ────────┤ entity_id   ────────┤
                     ▼                     ▼                     ▼
              words / collocations / prep_patterns / examples / articles
```

---

## 2. 架构概览 — 分层图

```
  ┌─────────────────────────────────────────────────────────────────────────────────┐
  │                              应用层 (Application)                                 │
  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐     │
  │  │ 单词学习  │ │ 文章阅读  │ │ 收藏管理  │ │ 游戏化   │ │ 自适应推荐      │     │
  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────────┬─────────┘     │
  └───────┼────────────┼────────────┼────────────┼────────────────┼───────────────┘
          │            │            │            │                │
  ┌───────▼────────────▼────────────▼────────────▼────────────────▼───────────────┐
  │                          服务层 (Service / DAO)                                 │
  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌────────┐ │
  │  │Word  │ │Article│ │Fav   │ │User  │ │Freq  │ │Stats │ │Plan  │ │Note    │ │
  │  │Repo  │ │Repo  │ │Repo  │ │Repo  │ │Repo  │ │Repo  │ │Repo  │ │Repo    │ │
  │  └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └───┬────┘ │
  └──────┼────────┼────────┼────────┼────────┼────────┼────────┼─────────┼──────┘
         │        │        │        │        │        │        │         │
  ┌──────▼────────▼────────▼────────▼────────▼────────▼────────▼─────────▼──────┐
  │                             数据层 (Database) 30 表                           │
  │                                                                              │
  │  ┌─────────── 核心词汇域 ───────────────────────────────────────────────┐   │
  │  │  words → {definitions, usage_notes, collocations, prep_patterns,    │   │
  │  │           examples, word_relations, word_forms, word_variants,      │   │
  │  │           word_tags}                                                │   │
  │  │  articles ← examples (article_id)                                   │   │
  │  └─────────────────────────────────────────────────────────────────────┘   │
  │                                                                              │
  │  ┌─────────── 用户个性化域 ─────────────────────────────────────────────┐   │
  │  │  users → user_settings / user_stats / user_tags / user_notes        │   │
  │  │       → search_history / content_ratings / user_frequencies         │   │
  │  └─────────────────────────────────────────────────────────────────────┘   │
  │                                                                              │
  │  ┌─────────── 学习行为域 ───────────────────────────────────────────────┐   │
  │  │  users → learning_activities (日汇总) / review_log (每道题)          │   │
  │  │       → reading_progress (阅读) / daily_recommendations (推荐)      │   │
  │  └─────────────────────────────────────────────────────────────────────┘   │
  │                                                                              │
  │  ┌─────────── 收藏 & 计划 & 游戏化 ─────────────────────────────────────┐   │
  │  │  favorite_folders → favorites (多态)                                 │   │
  │  │  badges → user_badges                                               │   │
  │  │  learning_plans → user_plans                                        │   │
  │  └─────────────────────────────────────────────────────────────────────┘   │
  └──────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 表清单

| # | 域 | 表名 | 说明 |
|---|---|---|---|
| 1 | 核心 | `users` | 用户（头像/简介） |
| 2 | 核心 | `words` | 单词主表（SM-2） |
| 3 | 核心 | `articles` | 英文文章 |
| 4 | 词汇 | `definitions` | 释义 |
| 5 | 词汇 | `usage_notes` | 用法说明 |
| 6 | 词汇 | `collocations` | 固定搭配 |
| 7 | 词汇 | `prep_patterns` | 介词搭配 |
| 8 | 词汇 | `examples` | 例句 |
| 9 | 词汇 | `word_relations` | 词关系网络 |
| 10 | 词汇 | `word_tags` | 系统标签 |
| 11 | 词汇 | `word_forms` | 单词变形 |
| 12 | 词汇 | `word_variants` | 拼写变体 |
| 13 | 个性化 | `user_settings` | ★ 用户偏好（键值对） |
| 14 | 个性化 | `user_tags` | ★ 自定义标签 |
| 15 | 个性化 | `user_entity_tags` | ★ 标签-实体关联 |
| 16 | 个性化 | `user_notes` | ★ 个人笔记 |
| 17 | 个性化 | `user_frequencies` | 个人频率 |
| 18 | 个性化 | `favorite_folders` | 收藏夹 |
| 19 | 个性化 | `favorites` | 收藏条目 |
| 20 | 个性化 | `content_ratings` | ★ 内容评分/反馈 |
| 21 | 行为 | `learning_activities` | ★ 学习活动日志（日汇总） |
| 22 | 行为 | `review_log` | ★ 答题日志（每道题） |
| 23 | 行为 | `search_history` | ★ 搜索历史 |
| 24 | 行为 | `reading_progress` | ★ 阅读进度 |
| 25 | 行为 | `daily_recommendations` | ★ 每日推荐 |
| 26 | 游戏化 | `user_stats` | ★ 统计（XP/等级/打卡） |
| 27 | 游戏化 | `badges` | ★ 徽章定义 |
| 28 | 游戏化 | `user_badges` | ★ 用户徽章 |
| 29 | 计划 | `learning_plans` | ★ 学习计划模板 |
| 30 | 计划 | `user_plans` | ★ 用户学习计划 |

**总计：30 表 | 3 核心 + 9 词汇 + 7 个性化 + 5 行为 + 3 游戏化 + 3 计划**  
（★ = v7 新增）

---

## 4. 核心实体详解

### 4.1 users — 用户表

```
┌────────────────┬──────────────────┬────────────┬────────────────────────┐
│ 字段           │ 类型             │ 约束        │ 说明                   │
├────────────────┼──────────────────┼────────────┼────────────────────────┤
│ id             │ CHAR(36)         │ PK          │ UUID 主键              │
│ username       │ VARCHAR(50)      │ UNIQUE      │ 用户名                 │
│ password_hash  │ VARCHAR(255)     │ NOT NULL    │ bcrypt 哈希            │
│ email          │ VARCHAR(200)     │ UNIQUE      │ 邮箱                   │
│ nickname       │ VARCHAR(100)     │ NULL        │ 昵称                   │
│ avatar_url     │ VARCHAR(500)     │ NULL        │ ★ 头像URL              │
│ bio            │ TEXT             │ NULL        │ ★ 个人简介              │
│ role           │ ENUM(...)        │ DEFAULT     │ 角色                   │
│ permission_lvl │ TINYINT          │ DEFAULT 1   │ 1/5/9 权限级别         │
│ is_active      │ TINYINT(1)       │ DEFAULT 1   │ 是否激活               │
│ last_login_at  │ DATETIME         │ NULL        │ 最后登录               │
│ created_at     │ DATETIME         │ DEFAULT     │ 注册时间               │
│ updated_at     │ DATETIME         │ ON UPDATE   │ 更新时间               │
└────────────────┴──────────────────┴────────────┴────────────────────────┘
```

### 4.2 user_settings — 用户偏好表

```
┌────────────────┬──────────────────┬────────────┬────────────────────────┐
│ 字段           │ 类型             │ 约束        │ 说明                   │
├────────────────┼──────────────────┼────────────┼────────────────────────┤
│ id             │ CHAR(36)         │ PK          │ UUID                  │
│ user_id        │ CHAR(36)         │ FK          │ 用户                   │
│ setting_key    │ VARCHAR(50)      │ NOT NULL    │ 键                     │
│ setting_value  │ TEXT             │ NOT NULL    │ 值                     │
│ created_at     │ DATETIME         │ DEFAULT     │ 创建时间               │
│ updated_at     │ DATETIME         │ ON UPDATE   │ 修改时间               │
└────────────────┴──────────────────┴────────────┴────────────────────────┘
唯一约束: (user_id, setting_key)
```

支持设置键说明：

| setting_key | 值示例 | 说明 |
|---|---|---|
| `daily_word_goal` | `20` | 每日学习目标词数 |
| `learning_mode` | `card`/`choice`/`spelling`/`listening` | 学习模式偏好 |
| `pronunciation` | `uk`/`us` | 发音偏好 |
| `theme` | `light`/`dark` | 主题 |
| `font_size` | `14`/`16`/`18` | 字号 |
| `auto_play_audio` | `true`/`false` | 自动播放发音 |
| `reminder_time` | `08:00` | 复习提醒时间 |
| `new_words_per_day` | `10` | 每日新词上限 |
| `ui_language` | `zh`/`en` | 界面语言 |

### 4.3 user_stats — 用户统计/游戏化表

```
┌─────────────────────┬──────────────────┬────────────┬────────────────────────┐
│ 字段                │ 类型             │ 约束        │ 说明                   │
├─────────────────────┼──────────────────┼────────────┼────────────────────────┤
│ id                  │ CHAR(36)         │ PK          │ UUID                  │
│ user_id             │ CHAR(36)         │ FK, UNIQUE  │ 用户                   │
│ xp                  │ INT              │ DEFAULT 0   │ 经验值                 │
│ level               │ INT              │ DEFAULT 1   │ 等级                   │
│ streak_days         │ INT              │ DEFAULT 0   │ 当前连续打卡天数       │
│ longest_streak      │ INT              │ DEFAULT 0   │ 最长连续打卡           │
│ total_words_learned │ INT              │ DEFAULT 0   │ 累计学习新词数         │
│ total_reviews       │ INT              │ DEFAULT 0   │ 累计复习次数           │
│ total_time_spent_sec│ INT              │ DEFAULT 0   │ 累计学习时长           │
│ is_public           │ TINYINT(1)       │ DEFAULT 0   │ 是否公开排行榜         │
│ created_at          │ DATETIME         │ DEFAULT     │ 创建时间               │
│ updated_at          │ DATETIME         │ ON UPDATE   │ 修改时间               │
└─────────────────────┴──────────────────┴────────────┴────────────────────────┘
```

### 4.4 learning_activities — 学习活动日志表

```
┌────────────────┬──────────────────┬────────────┬────────────────────────────────┐
│ 字段           │ 类型             │ 约束        │ 说明                           │
├────────────────┼──────────────────┼────────────┼────────────────────────────────┤
│ id             │ CHAR(36)         │ PK          │ UUID                          │
│ user_id        │ CHAR(36)         │ FK          │ 用户                           │
│ activity_date  │ DATE             │ NOT NULL    │ 活动日期                       │
│ words_studied  │ INT              │ DEFAULT 0   │ 学习新词数                     │
│ reviews_done   │ INT              │ DEFAULT 0   │ 复习次数                       │
│ time_spent_sec │ INT              │ DEFAULT 0   │ 学习时长（秒）                 │
│ correct_count  │ INT              │ DEFAULT 0   │ 答对次数                       │
│ wrong_count    │ INT              │ DEFAULT 0   │ 答错次数                       │
│ created_at     │ DATETIME         │ DEFAULT     │ 创建时间                       │
│ updated_at     │ DATETIME         │ ON UPDATE   │ 修改时间                       │
└────────────────┴──────────────────┴────────────┴────────────────────────────────┘
唯一约束: (user_id, activity_date)  — 一天一条汇总

用途: 连续打卡统计、学习曲线图、每日进度、排行榜积分
```

### 4.5 review_log — 答题日志表

```
┌─────────────────┬────────────────────┬────────────┬────────────────────────────┐
│ 字段            │ 类型               │ 约束        │ 说明                       │
├─────────────────┼────────────────────┼────────────┼────────────────────────────┤
│ id              │ CHAR(36)           │ PK          │ UUID                      │
│ user_id         │ CHAR(36)           │ FK          │ 用户                       │
│ word_id         │ CHAR(36)           │ FK          │ 单词                       │
│ quiz_type       │ ENUM(meaning,      │ NOT NULL    │ 题型                       │
│                 │      spelling,     │             │                            │
│                 │      listening,    │             │                            │
│                 │      usage,        │             │                            │
│                 │      sentence)     │             │                            │
│ is_correct      │ BOOLEAN            │ NOT NULL    │ 是否答对                   │
│ response_time_ms│ INT                │ NULL        │ 响应时间(毫秒)             │
│ wrong_answer    │ TEXT               │ NULL        │ 答了什么错                 │
│ reviewed_at     │ DATETIME           │ DEFAULT     │ 答题时间                   │
└─────────────────┴────────────────────┴────────────┴────────────────────────────┘

用途:
  - 易错词分析: 哪些词总是答错
  - 薄弱题型分析: 拼写差还是听力差
  - 自适应推送: 针对薄弱题型加强练习
  - 学习效率: 响应时间变化曲线
```

### 4.6 user_notes — 用户笔记表

```
┌────────────────┬──────────────────┬────────────┬────────────────────────┐
│ 字段           │ 类型             │ 约束        │ 说明                   │
├────────────────┼──────────────────┼────────────┼────────────────────────┤
│ id             │ CHAR(36)         │ PK          │ UUID                  │
│ user_id        │ CHAR(36)         │ FK          │ 用户                   │
│ entity_type    │ VARCHAR(50)      │ NOT NULL    │ 多态类型               │
│ entity_id      │ CHAR(36)         │ NOT NULL    │ 实体 UUID              │
│ content        │ TEXT             │ NOT NULL    │ 笔记内容               │
│ is_private     │ BOOLEAN          │ DEFAULT 1   │ 是否私密               │
│ created_at     │ DATETIME         │ DEFAULT     │ 创建时间               │
│ updated_at     │ DATETIME         │ ON UPDATE   │ 修改时间               │
└────────────────┴──────────────────┴────────────┴────────────────────────┘
```

### 4.7 user_tags & user_entity_tags — 用户标签系统

```
user_tags                          user_entity_tags
┌──────────────────────┐           ┌──────────────────────────────┐
│ id CHAR(36)     PK   │           │ user_id    CHAR(36)     PK   │
│ user_id CHAR(36) FK  │──1:N──    │ tag_id     CHAR(36)     PK   │
│ tag VARCHAR(30)      │           │ entity_type VARCHAR(50) PK   │
│ color VARCHAR(7)     │           │ entity_id  CHAR(36)     PK   │
│ created_at           │           │ created_at                   │
└──────────────────────┘           └──────────────────────────────┘

示例: 用户自定义 "写作词汇(#FF5733)" "口语词汇(#33FF57)" "考试必备(#3357FF)"
      然后对任意单词打上这些标签，形成个人分类体系
```

### 4.8 badges & user_badges — 徽章系统

```
badges                            user_badges
┌──────────────────────┐         ┌──────────────────────────┐
│ id CHAR(36)     PK   │         │ user_id CHAR(36)    PK   │
│ name VARCHAR(100)    │──1:N──  │ badge_id CHAR(36)   PK   │
│ icon VARCHAR(200)    │         │ earned_at DATETIME       │
│ description          │         └──────────────────────────┘
│ criteria JSON        │
│ sort_order INT       │
│ created_at           │
└──────────────────────┘

criteria 示例:
  {"type":"streak",      "days":7}
  {"type":"words_learned","count":100}
  {"type":"accuracy",    "rate":0.9}
  {"type":"level",       "level":10}
  {"type":"first_lesson"}
```

### 4.9 learning_plans & user_plans — 学习计划系统

```
learning_plans (模板)              user_plans (用户参与)
┌──────────────────────────┐     ┌────────────────────────────────┐
│ id CHAR(36)         PK   │     │ id CHAR(36)              PK   │
│ name VARCHAR(200)        │     │ user_id CHAR(36)         FK   │
│ description TEXT         │──┐  │ plan_id CHAR(36)         FK   │
│ target_level VARCHAR(50) │  │  │ started_at DATETIME           │
│ duration_days INT        │  └─ │ completed_at DATETIME NULL    │
│ daily_word_count INT     │     │ current_day INT DEFAULT 0     │
│ is_active BOOLEAN        │     │ daily_target INT NULL         │
│ sort_order INT           │     │ created_at / updated_at       │
│ created_at / updated_at  │     └────────────────────────────────┘
└──────────────────────────┘

模板示例:
  "CET-4 30天冲刺"   → 30天, 每日20词, 目标等级 CET-4
  "考研词汇 60天"    → 60天, 每日15词, 目标等级 考研
  "雅思7分词汇"      → 45天, 每日25词, 目标等级 雅思7.0
```

### 4.10 reading_progress — 阅读进度表

```
┌────────────────┬──────────────────┬────────────┬────────────────────────────────┐
│ 字段           │ 类型             │ 约束        │ 说明                           │
├────────────────┼──────────────────┼────────────┼────────────────────────────────┤
│ id             │ CHAR(36)         │ PK          │ UUID                          │
│ user_id        │ CHAR(36)         │ FK          │ 用户                           │
│ article_id     │ CHAR(36)         │ FK          │ 文章                           │
│ scroll_position│ INT              │ DEFAULT 0   │ 阅读进度（字符偏移）           │
│ is_completed   │ BOOLEAN          │ DEFAULT 0   │ 是否读完                       │
│ words_looked_up│ INT              │ DEFAULT 0   │ 阅读中查词数                   │
│ last_read_at   │ DATETIME         │ ON UPDATE   │ 最后阅读时间                   │
│ created_at     │ DATETIME         │ DEFAULT     │ 创建时间                       │
└────────────────┴──────────────────┴────────────┴────────────────────────────────┘
唯一约束: (user_id, article_id)

用途: 跨设备同步阅读位置、统计阅读量、推荐相关文章
```

---

## 5. 关系矩阵

### 5.1 词汇域

| 父表 | 子表 | 关系 | 外键 |
|---|---|---|---|
| `words` | `definitions` | 1:N | `word_id` |
| `words` | `usage_notes` | 1:N | `word_id` |
| `words` | `collocations` | 1:N | `word_id` |
| `words` | `prep_patterns` | 1:N | `word_id` |
| `words` | `examples` | 1:N | `word_id` |
| `words` | `word_relations.word_id` | 1:N | `word_id` |
| `words` | `word_relations.related_word_id` | 1:N | `related_word_id` |
| `words` | `word_tags` | 1:N | `word_id` |
| `words` | `word_forms` | 1:N | `word_id` |
| `words` | `word_variants` | 1:N | `word_id` |
| `articles` | `examples` | 1:N | `article_id` |

### 5.2 用户域 — 用户直连表

| 父表 | 子表 | 关系 | 外键 | 说明 |
|---|---|---|---|---|
| `users` | `user_settings` | 1:N | `user_id` | 偏好设置 |
| `users` | `user_stats` | 1:1 | `user_id` | 统计/游戏化 |
| `users` | `user_tags` | 1:N | `user_id` | 自定义标签 |
| `users` | `user_notes` | 1:N | `user_id` | 个人笔记 |
| `users` | `search_history` | 1:N | `user_id` | 搜索历史 |
| `users` | `content_ratings` | 1:N | `user_id` | 评分反馈 |
| `users` | `user_frequencies` | 1:N | `user_id` | 个人频率 |
| `users` | `favorite_folders` | 1:N | `user_id` | 收藏夹 |
| `users` | `learning_activities` | 1:N | `user_id` | 学习日志 |
| `users` | `review_log` | 1:N | `user_id` | 答题日志 |
| `users` | `reading_progress` | 1:N | `user_id` | 阅读进度 |
| `users` | `daily_recommendations` | 1:N | `user_id` | 每日推荐 |
| `users` | `user_plans` | 1:N | `user_id` | 学习计划 |
| `users` | `user_badges` | 1:N | `user_id` | 徽章 |

### 5.3 多态关联

| 表 | 多态字段 | 可关联实体 |
|---|---|---|
| `user_notes` | `(entity_type, entity_id)` | word / collocation / prep_pattern / example / article |
| `content_ratings` | `(entity_type, entity_id)` | 同上 |
| `daily_recommendations` | `(entity_type, entity_id)` | 同上 |
| `user_entity_tags` | `(entity_type, entity_id)` | 同上（通过 tag_id 间接） |
| `favorites` | `(entity_type, entity_id)` | word / collocation / prep_pattern / example / article |

### 5.4 其他关系

| 父表 | 子表 | 关系 | 外键 |
|---|---|---|---|
| `badges` | `user_badges` | 1:N | `badge_id` |
| `learning_plans` | `user_plans` | 1:N | `plan_id` |
| `user_tags` | `user_entity_tags` | 1:N | `tag_id` |
| `favorite_folders` | `favorites` | 1:N | `folder_id` |
| `words` | `review_log` | 1:N | `word_id` |
| `articles` | `reading_progress` | 1:N | `article_id` |

---

## 6. 频率体系设计

（同 v6，未变化）

实体表有 `frequency INT` 作为系统默认频率，`user_frequencies` 表允许用户覆盖。

```sql
SELECT COALESCE(uf.frequency, t.frequency) AS sort_freq
FROM words t
LEFT JOIN user_frequencies uf
  ON uf.entity_type = 'word' AND uf.entity_id = t.id AND uf.user_id = ?
ORDER BY sort_freq DESC;
```

---

## 7. 收藏夹体系设计

（同 v6，增加 `is_public` 字段支持公开分享）

---

## 8. 用户体系全景

```
                              ┌──────────────────┐
                              │     users         │
                              │  (账号/权限/头像) │
                              └────────┬─────────┘
          ┌────────────────────────────┼──────────────────────────────┐
          │              │             │              │               │
   ┌──────▼──────┐  ┌────▼────┐  ┌────▼────┐  ┌─────▼──────┐  ┌─────▼──────┐
   │ 偏好/配置    │  │ 统计/等级 │  │ 个性化   │  │ 学习行为    │  │ 收藏/计划   │
   │─────────────│  │─────────│  │─────────│  │───────────│  │───────────│
   │user_settings │  │user_stats│  │user_tags │  │learn_acti │  │fav_folders│
   │              │  │badges    │  │user_notes│  │review_log │  │favorites  │
   │              │  │user_badgs│  │cnt_rating│  │searc_hist │  │learn_plans│
   │              │  │         │  │user_freq │  │read_prog  │  │user_plans │
   │              │  │         │  │          │  │daily_rec  │  │           │
   └──────────────┘  └─────────┘  └──────────┘  └───────────┘  └───────────┘
```

用户适配能力总结：

| 适配维度 | 支撑表 | 实现方式 |
|---|---|---|
| **学习偏好** | `user_settings` | 键值对，支持任意扩展 |
| **内容排序** | `user_frequencies` | 个人频率覆盖默认频率 |
| **分类体系** | `user_tags` + `user_entity_tags` | 用户自建标签+颜色标记 |
| **个人笔记** | `user_notes` | 多态关联任意实体 |
| **内容评价** | `content_ratings` | 1-5 分 + 反馈文本 |
| **收藏管理** | `favorite_folders` + `favorites` | 分目录、多类别 |
| **进度跟踪** | `learning_activities` | 日维度学习汇总 |
| **薄弱分析** | `review_log` | 逐题记录+错题内容 |
| **游戏化** | `user_stats` + `badges` + `user_badges` | XP/等级/打卡/徽章 |
| **自适应** | `daily_recommendations` | 基于错题/间隔推荐 |
| **阅读** | `reading_progress` | 跨设备同步阅读位置 |
| **计划** | `learning_plans` + `user_plans` | 目标导向的学习路径 |

---

## 9. 间隔重复 (SM-2) 说明

（同 v6，未变化）

---

## 10. 索引策略

### 10.1 words 索引

```
idx_letter          (first_letter)             字母分区
idx_pos             (pos)                      词性筛选
idx_difficulty      (difficulty)               难度筛选
idx_source          (source)                   来源词表
idx_stage           (stage)                    学习阶段
idx_next_review     (next_review)              到期复习队列
idx_created_at      (created_at)               最新入库排序
idx_stage_review    (stage, next_review)       复习队列复合
idx_letter_pos      (first_letter, pos)        字母+词性复合
idx_confidence      (confidence)               掌握度筛选
idx_frequency       (frequency)                频率排序
```

### 10.2 新增表索引

| 表 | 索引字段 | 用途 |
|---|---|---|
| `user_settings` | `(user_id, setting_key)` UNIQUE | 用户设置 |
| `user_stats` | `user_id` UNIQUE, `xp DESC`, `level DESC`, `streak_days DESC` | 排行榜 |
| `user_tags` | `(user_id, tag)` UNIQUE | 标签唯一 |
| `user_entity_tags` | `(user_id,tag_id,entity_type,entity_id)` PK, `tag_id`, `(entity_type,entity_id)` | 多态关联 |
| `user_notes` | `user_id`, `(entity_type,entity_id)`, `(user_id,entity_type,entity_id)` | 多态查询 |
| `learning_activities` | `(user_id, activity_date)` UNIQUE, `activity_date` | 日汇总、打卡 |
| `review_log` | `user_id`, `word_id`, `reviewed_at`, `(user_id,word_id)`, `quiz_type` | 错题分析 |
| `search_history` | `user_id`, `(user_id, searched_at DESC)`, `query(20)` | 搜索联想 |
| `content_ratings` | `(user_id,entity_type,entity_id)` UNIQUE, `rating` | 评分防重复 |
| `reading_progress` | `(user_id, article_id)` UNIQUE | 阅读位置 |
| `daily_recommendations` | `(user_id, recommend_date)`, `(user_id, is_consumed)` | 推荐推送 |
| `user_plans` | `user_id`, `plan_id`, `(user_id, completed_at)` | 计划进度 |
| `user_badges` | `(user_id, badge_id)` PK, `badge_id`, `earned_at` | 徽章查询 |
| `badges` | `sort_order` | 排序 |

---

## 11. 数据流图

### 11.1 用户学习完整流程

```
用户登录
  │
  ├── [首页加载]
  │   ├── user_settings        → 读取偏好（每日目标、学习模式）
  │   ├── user_stats           → 读取等级/连续打卡天数
  │   ├── daily_recommendations→ 读取今日推荐
  │   └── learning_activities(今日) → 今日进度
  │
  ├── [单词学习/复习]
  │   ├── words WHERE next_review <= NOW()  → SM-2 到期词
  │   ├── 答题后:
  │   │   ├── INSERT review_log  (记录每题)
  │   │   ├── UPDATE words SM-2 字段
  │   │   └── UPDATE learning_activities (日汇总+1)
  │   └── 答错词 → INSERT daily_recommendations (明天再推)
  │
  ├── [查词]
  │   ├── SELECT words + definitions + collocations + examples
  │   ├── INSERT search_history
  │   └── 命中 → 可: 收藏 / 记笔记 / 打分 / 加标签 / 设频率
  │
  ├── [阅读文章]
  │   ├── INSERT/UPDATE reading_progress (同步位置)
  │   └── 点击查词 → words_looked_up +1
  │
  └── [日终结算]
      └── 更新 user_stats (xp += 今日所得, streak 判断, 徽章检查)
```

### 11.2 个性化推荐逻辑

```
daily_recommendations 生成算法 (每日凌晨执行):

  1. 复习推荐: 今天有 SM-2 到期未学的词 → reason='间隔复习到期'
  2. 错题推荐: 近7天答错≥2次的词 → reason='易错词巩固'
  3. 新词推荐: 今日未达 daily_word_goal → 取难度匹配的未学词
  4. 弱项推荐: review_log 分析薄弱题型 → reason='听力弱项提升'

查询时:
  SELECT FROM daily_recommendations
  WHERE user_id=? AND recommend_date=CURDATE() AND is_consumed=FALSE
  ORDER BY
    CASE reason
      WHEN '间隔复习到期' THEN 0
      WHEN '易错词巩固'   THEN 1
      WHEN '新词推荐'     THEN 2
      ELSE 3
    END
```

### 11.3 游戏化触发流程

```
学习事件发生
  │
  ├── 本次答对 +10 XP
  ├── 连续答对3题 +5 bonus XP
  ├── 完成每日目标 +20 bonus XP
  │
  ├── UPDATE user_stats (xp += 奖励, 检查升级)
  │
  ├── 每日首次学习:
  │   ├── UPDATE learning_activities (写入今日记录)
  │   └── streak_days = 昨天有记录 ? ++ : 1
  │
  └── 检查徽章解锁:
      └── SELECT badges WHERE criteria 满足
          └── INSERT user_badges (如未获得)
```

---

## 12. 变更日志 v6→v7

| 变更 | 对象 | 说明 |
|---|---|---|
| 🆕 新增 | `user_settings` | 用户偏好（键值对，支持任意设置项） |
| 🆕 新增 | `user_stats` | 游戏化统计（XP/等级/打卡/排行榜） |
| 🆕 新增 | `user_tags` | 用户自定义标签（含颜色） |
| 🆕 新增 | `user_entity_tags` | 标签与任意实体的多对多关联 |
| 🆕 新增 | `user_notes` | 个人笔记（多态关联） |
| 🆕 新增 | `learning_activities` | 每日学习活动日志 |
| 🆕 新增 | `review_log` | 逐题答题日志（错题分析） |
| 🆕 新增 | `search_history` | 搜索历史 |
| 🆕 新增 | `content_ratings` | 内容评分/反馈 |
| 🆕 新增 | `badges` | 徽章定义（JSON 条件） |
| 🆕 新增 | `user_badges` | 用户获得徽章 |
| 🆕 新增 | `reading_progress` | 文章阅读进度 |
| 🆕 新增 | `daily_recommendations` | 每日自适应推荐 |
| 🆕 新增 | `learning_plans` | 学习计划模板 |
| 🆕 新增 | `user_plans` | 用户参与的计划 |
| ✏️ 修改 | `users` | +`avatar_url`, +`bio` |
| ✏️ 修改 | `favorite_folders` | +`is_public` 支持公开分享 |
| 📈 总计 | 15 表 → **30 表** | 新增 15 表，翻倍 |

---

> **文档版本**: v7.0 | **最后更新**: 2026-05-21 | **设计者**: opencode  
> **数据库**: MySQL 8.0+ / MariaDB 10.5+ | **字符集**: utf8mb4 | **引擎**: InnoDB  
> **主键**: UUID v4 (CHAR(36)) | **总表数**: 30
