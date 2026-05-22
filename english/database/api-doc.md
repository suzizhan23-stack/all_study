# 英语学习网站 — API 接口文档

> 版本：v2.1  
> 关联：数据库 v8（36 表，INT AUTO_INCREMENT PK + UUID 双键策略）  
> 统一响应格式见下文

---

## 目录

- [统一约定](#统一约定)
- [1. 首页 Dashboard](#1-首页-dashboard)
- [2. 词典搜索](#2-词典搜索)
- [3. 单词详情页](#3-单词详情页)
- [4. 单词学习卡](#4-单词学习卡)
- [5. 复习测验页](#5-复习测验页)
- [6. 阅读文章列表](#6-阅读文章列表)
- [7. 文章阅读器](#7-文章阅读器)
- [8. 收藏夹管理](#8-收藏夹管理)
- [9. 收藏夹内容](#9-收藏夹内容)
- [10. 错题本](#10-错题本)
- [11. 学习计划](#11-学习计划)
- [12. 单词本浏览](#12-单词本浏览)
- [13. 个人中心](#13-个人中心)
- [14. 排行榜 & 徽章](#14-排行榜--徽章)
- [15. 管理后台](#15-管理后台)
- [附录 A：通用接口](#附录-a通用接口)
- [附录 B：词性分类字典](#附录-b词性分类字典)

---

## 统一约定

### 基础 URL

```
http://localhost:8080/api
```

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 200 成功，4xx 客户端错误，5xx 服务端错误 |
| message | string | 成功为 `"success"`，失败为具体错误描述 |
| data | object/null | 业务数据，失败时为 null |

### 分页响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [ ... ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 156,
      "total_pages": 8
    }
  }
}
```

### 认证方式

除登录注册外，所有接口需在请求头携带 JWT token：

```
Authorization: Bearer <token>
```

Token 过期返回 401：

```json
{ "code": 401, "message": "token_expired_or_invalid", "data": null }
```

### 通用错误码

| code | message | 说明 |
|---|---|---|
| 200 | success | 成功 |
| 400 | bad_request | 请求参数错误 |
| 401 | unauthorized | 未认证 / token 无效 |
| 403 | forbidden | 无权限 |
| 404 | not_found | 资源不存在 |
| 409 | conflict | 资源冲突（如重复收藏） |
| 422 | validation_error | 参数校验失败 |
| 429 | too_many_requests | 频率限制 |
| 500 | internal_error | 服务器内部错误 |

### uuid 格式说明

本文档中所有 `:id` 类型参数（`{id}` 路径变量）及响应中的 `id` 字段均为 UUID v4 格式（36 字符，含连字符），示例：`a1b2c3d4-e5f6-7890-abcd-ef1234567890`

**内部实现说明**：数据库使用 INT AUTO_INCREMENT 作为主键（PK）进行 JOIN 和索引，同时为每条记录分配一个 UUID（`uuid CHAR(36) UNIQUE`）对外暴露。Controller 层通过 `@PathVariable String uuid` 接收 UUID，Service 层调用 `repository.findByUuid(uuid)` 获取记录后，内部所有关联查询使用 INT PK。API 契约保持不变——外部调用者始终使用 UUID 字符串访问资源。

---

## 1. 首页 Dashboard

### 1.1 获取首页概览数据

获取用户今日进度、等级 XP、推荐学习项、快捷入口角标。

```
GET /api/dashboard
```

#### 请求参数

无

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "today": {
      "words_studied": 12,
      "daily_goal": 20,
      "pct": 60
    },
    "stats": {
      "streak_days": 3,
      "longest_streak": 15,
      "level": 5,
      "xp": 320,
      "xp_next_level": 500
    },
    "recommendations": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "entity_type": "word",
        "entity_id": "550e8400-e29b-41d4-a716-446655440001",
        "word": "abandon",
        "reason": "间隔复习到期",
        "is_consumed": false
      }
    ],
    "quick": {
      "due_review_count": 5,
      "unread_article_count": 3,
      "wrong_word_count": 2
    }
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| data.today.words_studied | int | 今日已学词数 |
| data.today.daily_goal | int | 用户设定的每日目标词数 |
| data.today.pct | int | 完成百分比（0-100） |
| data.stats.streak_days | int | 当前连续打卡天数 |
| data.stats.longest_streak | int | 历史最长连续打卡 |
| data.stats.level | int | 用户等级 |
| data.stats.xp | int | 当前经验值 |
| data.stats.xp_next_level | int | 升到下一级所需经验值 |
| data.recommendations[].id | uuid | 推荐记录 ID |
| data.recommendations[].entity_type | enum | `word` / `article` / `review_set` |
| data.recommendations[].word | string | 单词原文（实体为 article 时可为 null） |
| data.recommendations[].reason | string | 推荐理由，如"间隔复习到期""易错词巩固""新词推荐" |
| data.recommendations[].is_consumed | bool | 是否已消耗（点击即消耗） |
| data.quick.due_review_count | int | 待复习词数角标 |
| data.quick.unread_article_count | int | 未读完文章数角标 |
| data.quick.wrong_word_count | int | 近期错词数角标 |

#### SQL 查询

```sql
-- 1. 今日学习进度
SELECT words_studied, time_spent_sec
FROM learning_activities
WHERE user_id = :user_id AND activity_date = CURDATE();
-- 无记录返回空，前端按 0 处理

-- 2. 每日目标
SELECT setting_value
FROM user_settings
WHERE user_id = :user_id AND setting_key = 'daily_word_goal';
-- 无记录返回默认值 20

-- 3. 用户统计数据
SELECT level, xp, streak_days, longest_streak, total_words_learned, total_reviews
FROM user_stats
WHERE user_id = :user_id;

-- 4. 经验值升级表（计算 xp_next_level）
SELECT xp_required
FROM level_config
WHERE level = :current_level + 1;

-- 5. 今日推荐（未消耗的）
SELECT dr.*, w.word
FROM daily_recommendations dr
LEFT JOIN words w ON dr.entity_type = 'word' AND dr.entity_id = w.id
WHERE dr.user_id = :user_id
  AND dr.recommend_date = CURDATE()
  AND dr.is_consumed = FALSE
ORDER BY FIELD(dr.reason, '间隔复习到期', '易错词巩固', '新词推荐')
LIMIT 5;

-- 6. 待复习词数
SELECT COUNT(*)
FROM user_spaced_repetition usr
JOIN words w ON usr.word_id = w.id
WHERE usr.user_id = :user_id AND usr.next_review <= NOW();

-- 7. 未读完文章数
SELECT COUNT(*)
FROM reading_progress
WHERE user_id = :user_id AND is_completed = FALSE;

-- 8. 近7天错题数（去重词）
SELECT COUNT(DISTINCT word_id)
FROM review_log
WHERE user_id = :user_id AND is_correct = FALSE
  AND reviewed_at >= DATE_SUB(NOW(), INTERVAL 7 DAY);
```

---

### 1.2 消耗推荐项

用户点击推荐项后标记为已消耗。

```
PUT /api/dashboard/recommendations/:id/consume
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 推荐记录 ID |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

#### SQL

```sql
UPDATE daily_recommendations
SET is_consumed = TRUE, consumed_at = NOW()
WHERE uuid = :uuid AND user_id = :user_id;
```

---

## 2. 词典搜索

### 2.1 联想搜索（suggest）

输入前缀，返回匹配单词列表供下拉提示。

```
GET /api/search/suggest
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| q | query | string | 是 | 搜索前缀，至少 1 字符，最长 50 |
| limit | query | int | 否 | 返回条数，默认 8，最大 20 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "suggestions": ["abandon", "abandoned", "abandonment", "abandonedly"]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| data.suggestions[] | string[] | 匹配的单词列表 |

#### SQL

```sql
SELECT word
FROM words
WHERE word LIKE CONCAT(:q, '%')
ORDER BY frequency DESC, word ASC
LIMIT :limit;
```

---

### 2.2 单词搜索

```
GET /api/search
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| q | query | string | 是 | 搜索关键词，至少 1 字符 |
| page | query | int | 否 | 页码，默认 1 |
| size | query | int | 否 | 每页条数，默认 20，最大 50 |
| source | query | string | 否 | 筛选词库来源，如 `CET-4`、`CET-6`、`GRE` |
| pos | query | string | 否 | 筛选词性，如 `vt.`、`n.`、`adj.` |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "word": "abandon",
        "phonetic_uk": "/əˈbændən/",
        "phonetic_us": "/əˈbændən/",
        "pos": "vt.",
        "meaning_cn": "放弃；遗弃；抛弃",
        "source": "CET-4",
        "difficulty": 2,
        "frequency": 80,
        "is_collected": false
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 1,
      "total_pages": 1
    }
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| data.list[].id | uuid | 单词 ID |
| data.list[].word | string | 单词原文 |
| data.list[].phonetic_uk | string/null | 英式音标 |
| data.list[].phonetic_us | string/null | 美式音标 |
| data.list[].pos | string | 词性缩写 |
| data.list[].meaning_cn | string | 中文释义（分号分隔多义） |
| data.list[].source | string | 来源词库，如 `CET-4` |
| data.list[].difficulty | int | 难度等级 1-5 |
| data.list[].frequency | int | 词频（越高越常见） |
| data.list[].is_collected | bool | 当前用户是否已收藏此词 |

#### SQL

```sql
SELECT w.*,
  CASE WHEN f.id IS NOT NULL THEN TRUE ELSE FALSE END AS is_collected
FROM words w
LEFT JOIN favorites f ON f.entity_type = 'word' AND f.entity_id = w.id AND f.user_id = :user_id
WHERE w.word = :q
   OR w.word LIKE CONCAT(:q, '%')
  AND (:source IS NULL OR w.source = :source)
  AND (:pos IS NULL OR w.pos LIKE CONCAT(:pos, '%'))
ORDER BY FIELD(w.word, :q) DESC, w.frequency DESC
LIMIT :size OFFSET (:page - 1) * :size;
```

---

### 2.3 记录搜索历史

```
POST /api/search/history
```

#### 请求参数

```json
{
  "query": "abandon",
  "result_count": 12
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| query | string | 是 | 搜索词，最长 100 字符 |
| result_count | int | 否 | 搜索结果数，用于统计 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

#### SQL

```sql
INSERT INTO search_history (uuid, user_id, query, result_count, searched_at)
VALUES (:uuid, :user_id, :query, :result_count, NOW());
```

---

### 2.4 获取搜索历史

```
GET /api/search/history
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| limit | query | int | 否 | 返回条数，默认 10，最大 30 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "history": [
      { "query": "abandon", "last_searched": "2026-05-21 13:20:00", "result_count": 1 }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| data.history[].query | string | 搜索词 |
| data.history[].last_searched | datetime | 最近一次搜索时间 |
| data.history[].result_count | int | 最近一次搜索结果数 |

#### SQL

```sql
SELECT query, MAX(searched_at) AS last_searched, MAX(result_count) AS result_count
FROM search_history
WHERE user_id = :user_id
GROUP BY query
ORDER BY last_searched DESC
LIMIT :limit;
```

---

### 2.5 清除搜索历史

```
DELETE /api/search/history
```

#### 请求参数

无

#### 响应参数

```json
{ "code": 200, "message": "success", "data": null }
```

#### SQL

```sql
DELETE FROM search_history WHERE user_id = :user_id;
```

---

## 3. 单词详情页

### 3.1 获取单词详情

```
GET /api/words/:id
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 单词 ID |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "word": "abandon",
    "phonetic_uk": "/əˈbændən/",
    "phonetic_us": "/əˈbændən/",
    "audio_uk": "https://dict.youdao.com/dictvoice?audio=abandon&type=1",
    "audio_us": "https://dict.youdao.com/dictvoice?audio=abandon&type=2",
    "pos": "vt.",
    "meaning_cn": "放弃；遗弃；抛弃",
    "etymology_cn": "源自古法语 abandoner，源自 a-（到）+ bandon（控制权）",
    "source": "CET-4",
    "difficulty": 2,
    "frequency": 80,
    "first_letter": "A",
    "definitions": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440010",
        "meaning_en": "to leave someone or something completely",
        "meaning_cn": "放弃；遗弃",
        "sort_order": 1
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440011",
        "meaning_en": "to stop doing something before it is finished",
        "meaning_cn": "中止；停止",
        "sort_order": 2
      }
    ],
    "collocations": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440020",
        "collocation": "abandon hope",
        "translation": "放弃希望",
        "frequency": 5
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440021",
        "collocation": "abandon ship",
        "translation": "弃船",
        "frequency": 4
      }
    ],
    "prep_patterns": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440030",
        "pattern": "abandon something to somebody",
        "translation": "把某物丢给某人",
        "preposition": "to",
        "frequency": 3
      }
    ],
    "examples": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440040",
        "sentence_en": "The captain ordered everyone to abandon the sinking ship.",
        "sentence_cn": "船长命令所有人弃船。",
        "source_type": "CET46",
        "source_detail": "CET-4 2019-06",
        "frequency": 5,
        "rating": 4.5,
        "rating_count": 12
      }
    ],
    "relations": {
      "synonyms": [
        { "word_id": "uuid", "word": "desert", "meaning_cn": "抛弃" },
        { "word_id": "uuid", "word": "give up", "meaning_cn": "放弃" },
        { "word_id": "uuid", "word": "relinquish", "meaning_cn": "放弃" }
      ],
      "antonyms": [
        { "word_id": "uuid", "word": "keep", "meaning_cn": "保留" },
        { "word_id": "uuid", "word": "retain", "meaning_cn": "保留" }
      ]
    },
    "user_data": {
      "stage": 2,
      "confidence": 3,
      "next_review": "2026-05-27T00:00:00",
      "review_count": 5,
      "consecutive_correct": 3,
      "frequency": 72,
      "favorites": [
        { "folder_id": "uuid", "folder_name": "稍后复习" }
      ],
      "notes": {
        "id": "uuid",
        "content": "考研常考",
        "is_private": false,
        "updated_at": "2026-05-20"
      },
      "tags": [
        { "id": "uuid", "tag": "写作词汇", "color": "#FF5733" }
      ],
      "rating": 4
    },
    "related_articles": [
      {
        "id": "uuid",
        "title": "Climate Change and Policy",
        "snippet": "...abandon fossil fuels..."
      }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| id | uuid | 单词 ID |
| word | string | 单词原文 |
| phonetic_uk | string/null | 英式音标 |
| phonetic_us | string/null | 美式音标 |
| audio_uk | string/null | 英式发音 URL |
| audio_us | string/null | 美式发音 URL |
| pos | string | 词性 |
| meaning_cn | string | 中文释义摘要 |
| etymology_cn | string | 词源中文说明 |
| first_letter | string | 首字母（大写） |
| definitions[] | array | 详细释义列表 |
| definitions[].meaning_en | string | 英文释义 |
| definitions[].meaning_cn | string | 中文释义 |
| collocations[] | array | 固定搭配列表 |
| collocations[].collocation | string | 搭配短语 |
| collocations[].translation | string | 搭配中文翻译 |
| collocations[].frequency | int | 词频评分 1-10 |
| prep_patterns[] | array | 介词模式列表 |
| prep_patterns[].pattern | string | 模式模板 |
| prep_patterns[].preposition | string | 介词 |
| examples[] | array | 例句列表 |
| examples[].sentence_en | string | 英文例句 |
| examples[].sentence_cn | string | 中文翻译 |
| examples[].source_type | string | 来源类型：CET46 / GRE / IELTS / TOEFL / NEWS / ORIGINAL |
| examples[].source_detail | string | 来源详情 |
| examples[].rating | float | 平均评分 1-5 |
| relations.synonyms[].word | string | 同义词 |
| relations.antonyms[].word | string | 反义词 |
| user_data.stage | int | SM-2 阶段（0=新词，1-7） |
| user_data.confidence | int | 用户自评掌握度 1-5 |
| user_data.next_review | datetime/null | 下次复习时间 |
| user_data.review_count | int | 复习次数 |
| user_data.consecutive_correct | int | 连续答对次数 |
| user_data.frequency | int/null | 用户自设词频（覆盖系统词频） |
| user_data.favorites[] | array | 所在收藏夹列表 |
| user_data.notes | object/null | 用户笔记 |
| user_data.tags[] | array | 用户标签 |
| user_data.rating | int/null | 用户评分 1-5 |
| related_articles[] | array | 出现该词的文章 |

#### SQL 查询

```sql
-- 1. 单词基本信息
SELECT * FROM words WHERE uuid = :uuid;

-- 2. 详细释义
SELECT * FROM definitions
WHERE word_id = :wordId
ORDER BY sort_order;

-- 3. 固定搭配（含用户自定义频率覆写）
SELECT c.*, COALESCE(uf.frequency, c.frequency) AS effective_freq
FROM collocations c
LEFT JOIN user_frequencies uf
  ON uf.entity_type = 'collocation' AND uf.entity_id = c.id AND uf.user_id = :user_id
WHERE c.word_id = :wordId
ORDER BY effective_freq DESC;

-- 4. 介词模式
SELECT * FROM prep_patterns
WHERE word_id = :wordId
ORDER BY frequency DESC;

-- 5. 例句（含平均评分）
SELECT e.*, COALESCE(AVG(cr.rating), 0) AS avg_rating, COUNT(cr.id) AS rating_count
FROM examples e
LEFT JOIN content_ratings cr ON cr.entity_type = 'example' AND cr.entity_id = e.id
WHERE e.word_id = :wordId
GROUP BY e.id
ORDER BY e.frequency DESC;

-- 6. 同反义词（双向查询）
SELECT wr.relation_type, wr.related_word_id AS word_id, w.word, w.meaning_cn
FROM word_relations wr
JOIN words w ON wr.related_word_id = w.id
WHERE wr.word_id = :wordId
UNION
SELECT
  CASE WHEN wr.relation_type = 'synonym' THEN 'synonym' ELSE 'antonym' END,
  wr.word_id, w.word, w.meaning_cn
FROM word_relations wr
JOIN words w ON wr.word_id = w.id
WHERE wr.related_word_id = :wordId;

-- 7. 用户学习进度（SM-2）
SELECT stage, confidence, next_review, review_count, consecutive_correct
FROM user_spaced_repetition
WHERE user_id = :user_id AND word_id = :wordId;

-- 8. 用户自定义词频
SELECT frequency
FROM user_frequencies
WHERE user_id = :user_id AND entity_type = 'word' AND entity_id = :wordId;

-- 9. 收藏信息
SELECT ff.id AS folder_id, ff.name AS folder_name
FROM favorites f
JOIN favorite_folders ff ON f.folder_id = ff.id
WHERE f.user_id = :user_id AND f.entity_type = 'word' AND f.entity_id = :wordId;

-- 10. 用户笔记
SELECT * FROM user_notes
WHERE user_id = :user_id AND entity_type = 'word' AND entity_id = :wordId;

-- 11. 用户标签
SELECT t.*
FROM word_tags wt
JOIN tags t ON wt.tag_id = t.id
WHERE wt.user_id = :user_id AND wt.word_id = :wordId;

-- 12. 用户评分
SELECT rating
FROM content_ratings
WHERE user_id = :user_id AND entity_type = 'word' AND entity_id = :wordId;

-- 13. 相关文章（文章词索引中匹配该词的）
SELECT a.id, a.title, a.content AS snippet
FROM article_words aw
JOIN articles a ON aw.article_id = a.id
WHERE aw.word_id = :id
ORDER BY a.published_at DESC
LIMIT 5;
```

---

### 3.2 设置个人词频

```
PUT /api/words/:id/frequency
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 单词 ID |
| frequency | body | int | 是 | 个人词频 1-100 |

```json
{ "frequency": 72 }
```

#### 响应参数

```json
{ "code": 200, "message": "success", "data": null }
```

#### SQL

```sql
INSERT INTO user_frequencies (uuid, user_id, entity_type, entity_id, frequency)
VALUES (:uuid, :user_id, 'word', :id, :frequency)
ON DUPLICATE KEY UPDATE frequency = VALUES(frequency), updated_at = NOW();
```

---

### 3.3 保存笔记

```
PUT /api/words/:id/note
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 单词 ID |
| content | body | string | 是 | 笔记内容，最长 2000 字符 |
| is_private | body | bool | 否 | 是否私密，默认 true |

```json
{ "content": "考研常考，注意与 give up 的区别", "is_private": false }
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440100",
    "updated_at": "2026-05-21 14:00:00"
  }
}
```

#### SQL

```sql
INSERT INTO user_notes (uuid, user_id, entity_type, entity_id, content, is_private)
VALUES (:uuid, :user_id, 'word', :id, :content, :is_private)
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  is_private = VALUES(is_private),
  updated_at = NOW();
```

---

### 3.4 给单词打标签

```
POST /api/words/:id/tags
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 单词 ID |
| tag_id | body | uuid | 是 | 标签 ID |

```json
{ "tag_id": "550e8400-e29b-41d4-a716-446655440200" }
```

#### 响应参数

```json
{ "code": 200, "message": "success", "data": null }
```

#### SQL

```sql
INSERT IGNORE INTO word_tags (uuid, user_id, word_id, tag_id)
VALUES (:uuid, :user_id, :id, :tag_id);
```

---

### 3.5 移除单词标签

```
DELETE /api/words/:id/tags/:tagId
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 单词 ID |
| tagId | path | uuid | 是 | 标签 ID |

#### SQL

```sql
DELETE FROM word_tags WHERE user_id = :user_id AND word_id = :wordId AND tag_id = :tagId;
```

---

### 3.6 获取用户所有标签

```
GET /api/tags
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tags": [
      { "id": "uuid", "tag": "写作词汇", "color": "#FF5733", "word_count": 15 },
      { "id": "uuid", "tag": "听力高频", "color": "#33FF57", "word_count": 8 }
    ]
  }
}
```

#### SQL

```sql
SELECT t.*, COUNT(wt.word_id) AS word_count
FROM tags t
LEFT JOIN word_tags wt ON t.id = wt.tag_id AND wt.user_id = :user_id
WHERE t.user_id = :user_id
GROUP BY t.id
ORDER BY t.created_at;
```

---

### 3.7 收藏/取消收藏单词

```
POST /api/favorites
```

#### 请求参数

```json
{
  "folder_id": "550e8400-e29b-41d4-a716-446655440300",
  "entity_type": "word",
  "entity_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| folder_id | uuid | 是 | 收藏夹 ID |
| entity_type | string | 是 | 收藏实体类型：`word` / `example` / `article` |
| entity_id | uuid | 是 | 收藏实体 ID |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440400"
  }
}
```

#### SQL

```sql
INSERT INTO favorites (uuid, user_id, folder_id, entity_type, entity_id)
VALUES (:uuid, :user_id, :folder_id, :entity_type, :entity_id);
```

---

```
DELETE /api/favorites/:id
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 收藏记录 ID |

```sql
DELETE FROM favorites WHERE uuid = :uuid AND user_id = :user_id;
```

---

### 3.8 评分

```
PUT /api/words/:id/rating
```

#### 请求参数

```json
{ "rating": 4 }
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| rating | int | 是 | 评分 1-5 |

```sql
INSERT INTO content_ratings (uuid, user_id, entity_type, entity_id, rating)
VALUES (:uuid, :user_id, 'word', :id, :rating)
ON DUPLICATE KEY UPDATE rating = VALUES(rating), updated_at = NOW();
```

---

### 3.9 创建标签

```
POST /api/tags
```

```json
{ "tag": "写作词汇", "color": "#FF5733" }
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| tag | string | 是 | 标签名称，最长 20 字符，同一用户下唯一 |
| color | string | 否 | 颜色 hex，默认 `#2196F3` |

#### SQL

```sql
INSERT INTO tags (uuid, user_id, tag, color)
VALUES (:uuid, :user_id, :tag, :color);
```

---

## 4. 单词学习卡

### 4.1 获取某日学习计划

```
GET /api/plans/daily/words
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| date | query | string | 是 | 日期，格式 `YYYY-MM-DD` |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "date": "2026-05-21",
    "total": 8,
    "completed": 3,
    "words": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "word_id": "550e8400-e29b-41d4-a716-446655440500",
        "word": "abandon",
        "phonetic_uk": "/əˈbændən/",
        "pos": "verb",
        "pos_label": "动词",
        "meaning_cn": "放弃；遗弃",
        "is_completed": false,
        "entry_source": "user_added",
        "sort_order": 1,
        "collocations": [
          { "text": "abandon hope", "frequency": 5 },
          { "text": "abandon ship", "frequency": 4 }
        ],
        "preps": [
          { "pattern": "abandon something to somebody", "preposition": "to" }
        ]
      }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| data.date | string | 计划日期 |
| data.total | int | 计划词数 |
| data.completed | int | 已完成词数 |
| data.words[].id | uuid | 计划条目 ID（用于完成/删除） |
| data.words[].word_id | uuid | 单词 ID |
| data.words[].word | string | 单词原文 |
| data.words[].phonetic | string/null | 音标 |
| data.words[].pos | string | 词性缩写 |
| data.words[].pos_label | string | 词性中文标签 |
| data.words[].meaning_cn | string | 中文释义 |
| data.words[].is_completed | bool | 是否已完成 |
| data.words[].entry_source | string | `user_added` 用户手动添加 / `system_generated` 系统策略生成 |
| data.words[].collocations[] | array | 固定搭配（无中文） |
| data.words[].preps[].pattern | string | 介词模式 |
| data.words[].preps[].preposition | string | 介词 |

#### SQL

```sql
-- 用户手动添加的条目
SELECT upe.*, w.word, w.phonetic_uk, w.pos, w.meaning_cn
FROM user_daily_plan_entries upe
JOIN words w ON upe.word_id = w.id
WHERE upe.user_id = :user_id AND upe.plan_date = :date
ORDER BY upe.sort_order;

-- 系统生成的条目
SELECT dpi.*, w.word, w.phonetic_uk, w.pos, w.meaning_cn
FROM daily_plan_items dpi
JOIN words w ON dpi.word_id = w.id
WHERE dpi.user_id = :user_id AND dpi.plan_date = :date
ORDER BY dpi.sort_order;

-- 搭配数据（每个词独立查询或在代码中批量查询）
SELECT c.collocation AS text, c.frequency
FROM collocations c
WHERE c.word_id IN (:word_ids)
ORDER BY c.frequency DESC;

-- 介词模式（每个词独立查询或在代码中批量查询）
SELECT pp.pattern, pp.preposition
FROM prep_patterns pp
WHERE pp.word_id IN (:word_ids)
ORDER BY pp.frequency DESC;
```

---

### 4.2 获取所有有计划的日期

```
GET /api/plans/daily/dates
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| limit | query | int | 否 | 返回最近 N 个日期，默认 30 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dates": [
      { "date": "2026-05-21", "count": 8, "completed": 3 },
      { "date": "2026-05-20", "count": 10, "completed": 10 },
      { "date": "2026-05-18", "count": 5, "completed": 2 }
    ]
  }
}
```

#### SQL

```sql
SELECT plan_date, COUNT(*) AS count, SUM(is_completed) AS completed
FROM (
  SELECT plan_date, is_completed FROM user_daily_plan_entries WHERE user_id = :user_id
  UNION ALL
  SELECT plan_date, is_completed FROM daily_plan_items WHERE user_id = :user_id
) combined
GROUP BY plan_date
ORDER BY plan_date DESC
LIMIT :limit;
```

---

### 4.3 将单词加入计划

```
POST /api/plans/daily/entries
```

#### 请求参数

```json
{
  "word_id": "550e8400-e29b-41d4-a716-446655440500",
  "plan_date": "2026-05-21"
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| word_id | uuid | 是 | 单词 ID |
| plan_date | string | 是 | 计划日期 `YYYY-MM-DD`，不能早于今天 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440600"
  }
}
```

#### SQL

```sql
-- 检查是否已存在
SELECT id FROM user_daily_plan_entries
WHERE user_id = :user_id AND word_id = :word_id AND plan_date = :plan_date;
-- 若已存在则返回 409

-- 插入新条目
INSERT INTO user_daily_plan_entries (uuid, user_id, word_id, plan_date, sort_order)
VALUES (:uuid, :user_id, :word_id, :plan_date,
  (SELECT COALESCE(MAX(sort_order), 0) + 1
   FROM user_daily_plan_entries
   WHERE user_id = :user_id AND plan_date = :plan_date));
```

---

### 4.4 从计划中移除单词

```
DELETE /api/plans/daily/entries/:id
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 计划条目 ID |

```sql
DELETE FROM user_daily_plan_entries WHERE uuid = :uuid AND user_id = :user_id;
```

---

### 4.5 标记单词为已完成

```
PUT /api/plans/daily/entries/:id/complete
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 计划条目 ID |

```json
{ "code": 200, "message": "success", "data": null }
```

#### SQL

```sql
UPDATE user_daily_plan_entries
SET is_completed = TRUE, completed_at = NOW()
WHERE uuid = :uuid AND user_id = :user_id;
```

---

### 4.6 获取搭配数据（供卡片展示）

```
GET /api/words/:id/collocations
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 单词 ID |
| compact | query | bool | 否 | 是否紧凑格式（不含翻译），默认 false |
| limit | query | int | 否 | 返回条数，默认 10 |

#### 响应（compact=false）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "collocations": [
      { "id": "uuid", "collocation": "abandon hope", "translation": "放弃希望", "frequency": 5 }
    ]
  }
}
```

#### 响应（compact=true）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "collocations": [
      { "text": "abandon hope", "frequency": 5 }
    ]
  }
}
```

#### SQL

```sql
SELECT id, collocation, translation, frequency
FROM collocations
WHERE word_id = :wordId
ORDER BY frequency DESC
LIMIT :limit;
```

---

### 4.7 获取介词模式（供卡片展示）

```
GET /api/words/:id/prep-patterns
```

参数同 4.6。

#### SQL

```sql
SELECT id, pattern, translation, preposition, frequency
FROM prep_patterns
WHERE word_id = :wordId
ORDER BY frequency DESC
LIMIT :limit;
```

---

## 5. 复习测验页

### 5.1 获取复习队列

根据 SM-2 算法获取到期需复习的单词。

```
GET /api/review/queue
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| mode | query | string | 否 | 模式：`card`（默认，卡片翻页）、`quiz`（选择题）、`spelling`（拼写） |
| limit | query | int | 否 | 返回条数，默认 20，最大 50 |
| source | query | string | 否 | 筛选来源词库 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "queue": [
      {
        "word_id": "550e8400-e29b-41d4-a716-446655440000",
        "word": "abandon",
        "phonetic_uk": "/əˈbændən/",
        "phonetic_us": "/əˈbændən/",
        "meaning_cn": "放弃；遗弃；抛弃",
        "pos": "vt.",
        "stage": 2,
        "consecutive_correct": 3,
        "ease_factor": 2.5,
        "interval_days": 6,
        "last_reviewed_at": "2026-05-15T10:00:00",
        "next_review": "2026-05-21T10:00:00"
      }
    ],
    "total": 5,
    "new_words_available": 10
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| data.queue[].word_id | uuid | 单词 ID |
| data.queue[].stage | int | SM-2 阶段 0-7（0=新词从未复习） |
| data.queue[].consecutive_correct | int | 连续答对次数 |
| data.queue[].ease_factor | float | SM-2 难度系数 1.3-3.0 |
| data.queue[].interval_days | int | 当前间隔天数 |
| data.total | int | 队列总数 |
| data.new_words_available | int | 可学新词数（从未复习的词） |

#### SQL

```sql
-- 待复习的词（已学过的）
SELECT usr.*, w.word, w.phonetic_uk, w.phonetic_us, w.meaning_cn, w.pos
FROM user_spaced_repetition usr
JOIN words w ON usr.word_id = w.id
WHERE usr.user_id = :user_id
  AND usr.next_review IS NOT NULL
  AND usr.next_review <= NOW()
  AND (:source IS NULL OR w.source = :source)
ORDER BY usr.stage ASC, usr.next_review ASC
LIMIT :limit;

-- 可学新词数（从未学过的）
SELECT COUNT(*)
FROM words w
WHERE w.id NOT IN (
  SELECT word_id FROM user_spaced_repetition WHERE user_id = :user_id
)
AND (:source IS NULL OR w.source = :source);
```

---

### 5.2 提交答题结果

```
POST /api/review/result
```

#### 请求参数

```json
{
  "word_id": "550e8400-e29b-41d4-a716-446655440000",
  "quiz_type": "meaning",
  "is_correct": true,
  "response_time_ms": 3200,
  "wrong_answer": null
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| word_id | uuid | 是 | 单词 ID |
| quiz_type | string | 是 | 题型：`meaning`（选义）、`recall`（回忆）、`spelling`（拼写）、`listening`（听力）、`usage`（用法） |
| is_correct | bool | 是 | 是否正确 |
| response_time_ms | int | 否 | 答题耗时（毫秒） |
| wrong_answer | string/null | 否 | 用户填写的错误答案（拼写题型时记录） |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "xp_gained": 10,
    "stage": 3,
    "next_review": "2026-06-01T10:00:00"
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| data.xp_gained | int | 本次获得经验值 |
| data.stage | int | 更新后的 SM-2 阶段 |
| data.next_review | datetime | 下次复习时间 |

#### 后端事务 SQL

```sql
START TRANSACTION;

-- 1. 记录答题日志
INSERT INTO review_log (uuid, user_id, word_id, quiz_type, is_correct, response_time_ms, wrong_answer, reviewed_at)
VALUES (:uuid, :user_id, :word_id, :quiz_type, :is_correct, :response_time_ms, :wrong_answer, NOW());

-- 2. SM-2 参数计算（在代码中计算后落库）
-- 伪代码逻辑：
--   IF is_correct THEN
--     consecutive_correct = consecutive_correct + 1
--     IF consecutive_correct == 1 THEN interval = 1
--     ELSE IF consecutive_correct == 2 THEN interval = 6
--     ELSE interval = ROUND(interval * ease_factor)
--     ease_factor = ease_factor + 0.1
--   ELSE
--     consecutive_correct = 0
--     interval = 1
--     ease_factor = MAX(1.3, ease_factor - 0.2)
--   END IF

UPDATE user_spaced_repetition SET
  stage = :new_stage,
  consecutive_correct = :new_consecutive_correct,
  ease_factor = :new_ease_factor,
  interval_days = :new_interval,
  next_review = DATE_ADD(NOW(), INTERVAL :new_interval DAY),
  review_count = review_count + 1,
  last_reviewed_at = NOW()
WHERE user_id = :user_id AND word_id = :word_id;

-- 如果不存在则插入（首次学习）
INSERT INTO user_spaced_repetition (uuid, user_id, word_id, stage, ease_factor, interval_days, next_review, review_count, last_reviewed_at)
VALUES (:uuid, :user_id, :word_id, 1, 2.5, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), 1, NOW())
ON DUPLICATE KEY UPDATE word_id = word_id;  -- 已存在则忽略（通过 UNIQUE(user_id, word_id)）

-- 3. 更新每日学习活动
INSERT INTO learning_activities (uuid, user_id, activity_date, words_studied, reviews_done, time_spent_sec, correct_count, wrong_count)
VALUES (:uuid, :user_id, CURDATE(),
  IF(:is_correct AND :prev_review_count = 0, 1, 0), -- 首次正确算学了一个新词
  1, :response_time_ms / 1000,
  IF(:is_correct, 1, 0), IF(:is_correct, 0, 1))
ON DUPLICATE KEY UPDATE
  words_studied = words_studied + VALUES(words_studied),
  reviews_done = reviews_done + 1,
  time_spent_sec = time_spent_sec + VALUES(time_spent_sec),
  correct_count = correct_count + VALUES(correct_count),
  wrong_count = wrong_count + VALUES(wrong_count);

-- 4. 更新用户经验值
UPDATE user_stats SET
  xp = xp + :xp_gained,
  total_reviews = total_reviews + 1,
  total_words_learned = total_words_learned + IF(:is_correct AND :prev_review_count = 0, 1, 0)
WHERE user_id = :user_id;

-- 5. 检查是否升级
SELECT level, xp FROM user_stats WHERE user_id = :user_id;
-- 代码中对比 level_config 表，若达到升级条件则 UPDATE user_stats SET level = level + 1

COMMIT;
```

#### XP 计算规则

| 条件 | XP 奖励 |
|---|---|
| 答对 | +10 |
| 答错 | +2（鼓励） |
| 连续答对 >= 5 | +5 额外奖励 |
| 新词首次答对 | +15（含首次学习奖励） |
| 答题时间 < 3 秒 | +3 速度奖励 |
| 每日首轮复习 | +5 额外 |

---

### 5.3 获取选择题干扰项

```
GET /api/review/distractors
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| word_id | query | uuid | 是 | 正确单词 ID（排除此词释义） |
| pos | query | string | 否 | 词性，传入后可提高干扰项相关性 |
| count | query | int | 否 | 干扰项数量，默认 3，最大 5 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "distractors": ["保留", "坚持", "继续"]
  }
}
```

#### SQL

```sql
SELECT meaning_cn
FROM words
WHERE (:pos IS NULL OR pos = :pos)
  AND id != :word_id
  AND meaning_cn IS NOT NULL
ORDER BY RAND()
LIMIT :count;
```

---

### 5.4 更新打卡连续天数

```
PUT /api/user/stats/streak
```

#### 请求参数

无

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "streak_days": 3,
    "longest_streak": 15,
    "is_new_record": false
  }
}
```

#### SQL

```sql
-- 检查昨天是否有学习记录
SELECT 1 FROM learning_activities
WHERE user_id = :user_id AND activity_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY);

-- 更新连续天数
UPDATE user_stats SET
  streak_days = CASE WHEN EXISTS(SELECT 1 FROM learning_activities WHERE user_id = :user_id AND activity_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY))
    THEN streak_days + 1 ELSE 1 END,
  longest_streak = GREATEST(longest_streak,
    CASE WHEN EXISTS(...) THEN streak_days + 1 ELSE 1 END)
WHERE user_id = :user_id;
```

---

### 5.5 获取复习统计

```
GET /api/review/stats
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "today": { "total": 18, "correct": 15, "wrong": 3, "accuracy": 83.3 },
    "weekly": { "total": 120, "correct": 98, "wrong": 22, "accuracy": 81.7 },
    "by_type": {
      "meaning": { "total": 50, "correct": 45, "accuracy": 90 },
      "spelling": { "total": 30, "correct": 20, "accuracy": 66.7 },
      "listening": { "total": 20, "correct": 18, "accuracy": 90 }
    }
  }
}
```

#### SQL

```sql
-- 今日统计
SELECT COUNT(*) AS total, SUM(is_correct) AS correct, SUM(NOT is_correct) AS wrong
FROM review_log
WHERE user_id = :user_id AND DATE(reviewed_at) = CURDATE();

-- 本周统计
SELECT COUNT(*) AS total, SUM(is_correct) AS correct
FROM review_log
WHERE user_id = :user_id AND reviewed_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY);

-- 按题型统计
SELECT quiz_type, COUNT(*) AS total, SUM(is_correct) AS correct
FROM review_log
WHERE user_id = :user_id AND reviewed_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY quiz_type;
```

---

## 6. 阅读文章列表

### 6.1 获取文章列表

```
GET /api/articles
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| difficulty | query | int | 否 | 筛选难度 1-5 |
| source | query | string | 否 | 筛选来源如 `The Economist` |
| status | query | string | 否 | `all`（默认）、`unread`、`in_progress`、`completed` |
| page | query | int | 否 | 页码，默认 1 |
| size | query | int | 否 | 每页条数，默认 20 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440700",
        "title": "The Economic Logic of Climate Policy",
        "author": "John Smith",
        "source_name": "The Economist",
        "difficulty": 3,
        "word_count": 1200,
        "cover_image": null,
        "published_at": "2026-05-15",
        "tags": ["经济", "环境"],
        "progress": {
          "scroll_position": 960,
          "is_completed": false,
          "words_looked_up": 5,
          "last_read_at": "2026-05-20T20:30:00"
        }
      }
    ],
    "pagination": { "page": 1, "size": 20, "total": 45, "total_pages": 3 }
  }
}
```

#### SQL

```sql
SELECT a.*, rp.scroll_position, rp.is_completed, rp.words_looked_up, rp.last_read_at
FROM articles a
LEFT JOIN reading_progress rp ON a.id = rp.article_id AND rp.user_id = :user_id
WHERE (:difficulty IS NULL OR a.difficulty = :difficulty)
  AND (:source IS NULL OR a.source_name = :source)
  AND (
    :status = 'all' OR
    (:status = 'unread' AND rp.id IS NULL) OR
    (:status = 'in_progress' AND rp.is_completed = FALSE) OR
    (:status = 'completed' AND rp.is_completed = TRUE)
  )
ORDER BY rp.is_completed ASC, rp.last_read_at DESC, a.published_at DESC
LIMIT :size OFFSET (:page - 1) * :size;
```

---

## 7. 文章阅读器

### 7.1 获取文章内容

```
GET /api/articles/:id
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 文章 ID |
| include_content | query | bool | 否 | 是否返回完整正文，默认 true（列表页时可用 false 仅取元数据） |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "uuid",
    "title": "The Economic Logic of Climate Policy",
    "content": "<p>Climate change is one of the most pressing...</p>",
    "content_type": "html",
    "author": "John Smith",
    "source_name": "The Economist",
    "source_url": "https://www.economist.com/...",
    "difficulty": 3,
    "word_count": 1200,
    "published_at": "2026-05-15",
    "progress": {
      "scroll_position": 960,
      "is_completed": false,
      "words_looked_up": 5,
      "last_read_at": "2026-05-20T20:30:00"
    },
    "vocabulary": [
      { "word": "abandon", "meaning_cn": "放弃", "difficulty": 2 }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| data.content | string | 正文 HTML 或纯文本 |
| data.content_type | string | `html` 或 `text` |
| data.progress | object/null | 阅读进度，无记录时为 null |
| data.vocabulary[] | array | 文中出现的核心词汇（取前 20 个高频/难词） |

#### SQL

```sql
-- 文章基本信息
SELECT * FROM articles WHERE uuid = :uuid;

-- 阅读进度（articleId 为 resolved INT PK）
SELECT * FROM reading_progress WHERE user_id = :user_id AND article_id = :articleId;

-- 文中核心词汇
SELECT w.word, w.meaning_cn, w.difficulty
FROM article_words aw
JOIN words w ON aw.word_id = w.id
WHERE aw.article_id = :id
ORDER BY w.difficulty DESC, aw.frequency_in_article DESC
LIMIT 20;
```

---

### 7.2 保存阅读进度

```
PUT /api/articles/:id/progress
```

#### 请求参数

```json
{
  "scroll_position": 1200,
  "reading_time_sec": 180
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| scroll_position | int | 是 | 滚动位置（像素） |
| reading_time_sec | int | 否 | 本次阅读时长（秒） |

#### 响应参数

```json
{ "code": 200, "message": "success", "data": null }
```

#### SQL

```sql
INSERT INTO reading_progress (uuid, user_id, article_id, scroll_position, reading_time_sec, last_read_at)
VALUES (:uuid, :user_id, :id, :scroll_position, :reading_time_sec, NOW())
ON DUPLICATE KEY UPDATE
  scroll_position = VALUES(scroll_position),
  reading_time_sec = COALESCE(reading_time_sec, 0) + VALUES(reading_time_sec),
  last_read_at = NOW();
```

---

### 7.3 标记读完

```
PUT /api/articles/:id/complete
```

#### SQL

```sql
UPDATE reading_progress
SET is_completed = TRUE, scroll_position = 999999, last_read_at = NOW()
WHERE user_id = :user_id AND article_id = :articleId;

-- 增加 XP
UPDATE user_stats SET xp = xp + 30 WHERE user_id = :user_id;
```

---

### 7.4 阅读中查词

```
GET /api/articles/:id/lookup
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 文章 ID |
| word | query | string | 是 | 要查询的单词 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "word": "abandon",
    "phonetic_uk": "/əˈbændən/",
    "phonetic_us": "/əˈbændən/",
    "pos": "vt.",
    "meaning_cn": "放弃；遗弃；抛弃",
    "definitions": [
      { "meaning_en": "to leave completely and finally", "meaning_cn": "彻底放弃" }
    ],
    "collocations": [
      { "collocation": "abandon hope", "translation": "放弃希望" }
    ],
    "examples_in_article": [
      {
        "sentence": "Many countries <em>abandon</em> their climate goals.",
        "position": 3456
      }
    ]
  }
}
```

#### SQL

```sql
-- 查词
SELECT * FROM words WHERE word = :word;

-- 查找在文中的位置（通过全文索引或预存的 sentence 位置）
SELECT sentence, position FROM article_word_occurrences
WHERE article_id = :articleId AND word = :word;
```

#### 附加操作

```sql
UPDATE reading_progress
SET words_looked_up = words_looked_up + 1
WHERE user_id = :user_id AND article_id = :articleId;
```

---

## 8. 收藏夹管理

### 8.1 获取收藏夹列表

```
GET /api/folders
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| category | query | string | 否 | 筛选分类：`word` / `example` / `article` / `all`（默认） |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "folders": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440300",
        "name": "稍后复习",
        "category": "word",
        "is_default": true,
        "is_public": false,
        "item_count": 12,
        "sort_order": 1,
        "created_at": "2026-05-01"
      }
    ]
  }
}
```

#### SQL

```sql
SELECT ff.*, COUNT(f.id) AS item_count
FROM favorite_folders ff
LEFT JOIN favorites f ON ff.id = f.folder_id
WHERE ff.user_id = :user_id
  AND (:category IS NULL OR :category = 'all' OR ff.category = :category)
GROUP BY ff.id
ORDER BY ff.sort_order, ff.created_at;
```

---

### 8.2 创建收藏夹

```
POST /api/folders
```

#### 请求参数

```json
{
  "name": "精彩例句",
  "category": "example",
  "is_public": false
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 名称 1-50 字符，同一用户下唯一 |
| category | string | 是 | `word` / `example` / `article` |
| is_public | bool | 否 | 是否公开，默认 false |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "uuid",
    "sort_order": 3
  }
}
```

#### SQL

```sql
INSERT INTO favorite_folders (uuid, user_id, name, category, is_public, sort_order)
VALUES (:uuid, :user_id, :name, :category, :is_public,
  (SELECT COALESCE(MAX(sort_order), 0) + 1 FROM favorite_folders WHERE user_id = :user_id));
```

---

### 8.3 编辑收藏夹

```
PUT /api/folders/:id
```

#### 请求参数

```json
{ "name": "新名称", "is_public": true }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 否 | 新名称 |
| is_public | bool | 否 | 是否公开 |

```sql
UPDATE favorite_folders
SET name = COALESCE(:name, name),
    is_public = COALESCE(:is_public, is_public)
WHERE uuid = :uuid AND user_id = :user_id;
```

---

### 8.4 删除收藏夹

```
DELETE /api/folders/:id
```

```sql
DELETE FROM favorites WHERE folder_id = :folderId;
DELETE FROM favorite_folders WHERE uuid = :uuid AND user_id = :user_id;
```

---

### 8.5 调整收藏夹排序

```
PUT /api/folders/reorder
```

```json
{ "order": ["uuid1", "uuid2", "uuid3"] }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| order | uuid[] | 是 | 排序后的 ID 列表 |

```sql
-- 逐条更新（或在代码中批量）
UPDATE favorite_folders SET sort_order = :index WHERE uuid = :uuid AND user_id = :user_id;
```

---

## 9. 收藏夹内容

### 9.1 获取收藏夹条目

```
GET /api/folders/:id/items
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 收藏夹 ID |
| page | query | int | 否 | 默认 1 |
| size | query | int | 否 | 默认 20 |
| sort | query | string | 否 | `newest`（默认）/ `oldest` / `word_az` |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "folder": { "id": "uuid", "name": "稍后复习", "category": "word" },
    "items": [
      {
        "id": "uuid",
        "entity_type": "word",
        "entity_id": "uuid",
        "word": "abandon",
        "meaning_cn": "放弃",
        "note": "高频词",
        "created_at": "2026-05-20"
      }
    ],
    "pagination": { "page": 1, "size": 20, "total": 12, "total_pages": 1 }
  }
}
```

#### SQL

```sql
SELECT f.*, w.word, w.meaning_cn, w.pos, w.phonetic_uk
FROM favorites f
JOIN words w ON f.entity_type = 'word' AND f.entity_id = w.id
WHERE f.folder_id = :folderId AND f.user_id = :user_id
ORDER BY
  CASE WHEN :sort = 'newest' THEN f.created_at END DESC,
  CASE WHEN :sort = 'oldest' THEN f.created_at END ASC,
  CASE WHEN :sort = 'word_az' THEN w.word END ASC
LIMIT :size OFFSET (:page - 1) * :size;
```

---

### 9.2 批量删除

```
POST /api/favorites/batch-delete
```

```json
{ "ids": ["uuid1", "uuid2"] }
```

```sql
DELETE FROM favorites WHERE id IN (:ids) AND user_id = :user_id;
```

---

### 9.3 批量添加标签

```
POST /api/favorites/batch-tag
```

```json
{ "word_ids": ["uuid1", "uuid2"], "tag_id": "uuid" }
```

```sql
INSERT IGNORE INTO word_tags (uuid, user_id, word_id, tag_id)
SELECT UUID(), :user_id, word_id, :tag_id
FROM UNNEST(:word_ids) AS word_id;
```

---

## 10. 错题本

### 10.1 获取错题列表

```
GET /api/wrong-words
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| quiz_type | query | string | 否 | 筛选题型 |
| days | query | int | 否 | 近 N 天，默认 7（传 0 表示全部） |
| page | query | int | 否 | 默认 1 |
| size | query | int | 否 | 默认 20 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "stats": {
      "total_wrong_words": 12,
      "recent_days": 7,
      "top_wrong_word": { "word": "abandon", "count": 3 },
      "weak_types": [
        { "quiz_type": "spelling", "count": 7 },
        { "quiz_type": "meaning", "count": 3 }
      ]
    },
    "words": [
      {
        "word_id": "uuid",
        "word": "abandon",
        "meaning_cn": "放弃",
        "wrong_count": 3,
        "last_wrong": "2026-05-20T14:30:00",
        "logs": [
          {
            "id": "uuid",
            "quiz_type": "spelling",
            "wrong_answer": "abanden",
            "reviewed_at": "2026-05-20T14:30:00"
          }
        ]
      }
    ],
    "pagination": { "page": 1, "size": 20, "total": 12, "total_pages": 1 }
  }
}
```

#### SQL

```sql
-- 统计弱项题型
SELECT quiz_type, COUNT(*) AS count
FROM review_log
WHERE user_id = :user_id AND is_correct = FALSE
  AND (:days = 0 OR reviewed_at >= DATE_SUB(NOW(), INTERVAL :days DAY))
GROUP BY quiz_type
ORDER BY count DESC;

-- 按词聚合错题
SELECT rl.word_id, w.word, w.meaning_cn,
  COUNT(*) AS wrong_count,
  MAX(rl.reviewed_at) AS last_wrong
FROM review_log rl
JOIN words w ON rl.word_id = w.id
WHERE rl.user_id = :user_id AND rl.is_correct = FALSE
  AND (:days = 0 OR rl.reviewed_at >= DATE_SUB(NOW(), INTERVAL :days DAY))
  AND (:quiz_type IS NULL OR rl.quiz_type = :quiz_type)
GROUP BY rl.word_id
ORDER BY wrong_count DESC, last_wrong DESC
LIMIT :size OFFSET (:page - 1) * :size;

-- 每个单词的详细错题记录
SELECT id, quiz_type, wrong_answer, reviewed_at
FROM review_log
WHERE user_id = :user_id AND word_id = :word_id AND is_correct = FALSE
ORDER BY reviewed_at DESC;
```

---

### 10.2 一键复习错题

生成复习队列：取近期错题词，标记为待复习。

```
POST /api/wrong-words/review
```

#### 请求参数

```json
{
  "limit": 10,
  "days": 7
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| limit | int | 否 | 生成词数，默认 10 |
| days | int | 否 | 取近 N 天错题，默认 7 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "generated_count": 10,
    "words": [ ... ]
  }
}
```

#### SQL

```sql
-- 找出近期错题词
SELECT word_id, COUNT(*) AS wrong_count
FROM review_log
WHERE user_id = :user_id AND is_correct = FALSE
  AND reviewed_at >= DATE_SUB(NOW(), INTERVAL :days DAY)
GROUP BY word_id
ORDER BY wrong_count DESC
LIMIT :limit;

-- 更新为待复习（upsert）
INSERT INTO user_spaced_repetition (uuid, user_id, word_id, next_review, stage)
SELECT UUID(), :user_id, word_id, NOW(), 0
FROM UNNEST(:word_ids) AS word_id
ON DUPLICATE KEY UPDATE next_review = NOW();
```

---

## 11. 学习计划

### 11.1 获取用户当前进行中的计划

```
GET /api/plans/active
```

#### 请求参数

无

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "uuid",
    "plan_id": "uuid",
    "name": "CET-4 30天冲刺",
    "description": "覆盖四级核心 600 词，每天 20 个",
    "current_day": 3,
    "duration_days": 30,
    "daily_word_count": 20,
    "target_level": "CET-4",
    "book_name": "四级单词",
    "book_id": "uuid",
    "strategy_name": "艾宾浩斯递进",
    "strategy_id": "uuid",
    "started_at": "2026-05-19T00:00:00",
    "completed_at": null,
    "progress": { "done": 12, "target": 20 }
  }
}
```

#### SQL

```sql
SELECT up.*, lp.name, lp.description, lp.duration_days, lp.daily_word_count, lp.target_level,
  wb.name AS book_name, ss.name AS strategy_name
FROM user_plans up
JOIN learning_plans lp ON up.plan_id = lp.id
LEFT JOIN word_books wb ON lp.word_book_id = wb.id
LEFT JOIN study_strategies ss ON lp.strategy_id = ss.id
WHERE up.user_id = :user_id AND up.completed_at IS NULL
ORDER BY up.started_at DESC
LIMIT 1;
```

---

### 11.2 获取全部可选计划模板

```
GET /api/plans/templates
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "templates": [
      {
        "id": "uuid",
        "name": "CET-4 30天冲刺",
        "description": "适合四级备考，每天 20 词",
        "duration_days": 30,
        "daily_word_count": 20,
        "target_level": "CET-4",
        "word_book_id": "uuid",
        "strategy_id": "uuid",
        "is_active": true
      }
    ]
  }
}
```

#### SQL

```sql
SELECT * FROM learning_plans WHERE is_active = TRUE ORDER BY sort_order;
```

---

### 11.3 加入计划

```
POST /api/plans/join
```

#### 请求参数

```json
{
  "plan_id": "uuid"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| plan_id | uuid | 是 | 计划模板 ID |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "uuid",
    "started_at": "2026-05-21T00:00:00",
    "current_day": 1
  }
}
```

#### SQL

```sql
-- 检查是否有进行中的计划
SELECT id FROM user_plans WHERE user_id = :user_id AND completed_at IS NULL;
-- 如果有则返回 409

INSERT INTO user_plans (uuid, user_id, plan_id, started_at, current_day)
VALUES (:uuid, :user_id, :plan_id, CURDATE(), 1);
```

---

### 11.4 获取每日计划（系统生成版）

```
GET /api/plans/daily/system?date=2026-05-21
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| date | query | string | 是 | 日期 |
| book_id | query | uuid | 否 | 单词本 ID（有 active plan 时可不传） |

#### 响应参数

同 4.1，但 `entry_source` 为 `system_generated`。

#### SQL

```sql
SELECT dpi.*, w.word, w.phonetic_uk, w.pos, w.meaning_cn
FROM daily_plan_items dpi
JOIN words w ON dpi.word_id = w.id
WHERE dpi.user_id = :user_id AND dpi.plan_date = :date
ORDER BY dpi.sort_order;
```

---

### 11.5 生成每日计划

```
POST /api/plans/daily/generate
```

#### 请求参数

```json
{
  "word_book_id": "uuid",
  "strategy_id": "uuid",
  "date": "2026-05-22",
  "count": 10
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| word_book_id | uuid | 是 | 单词本 ID |
| strategy_id | uuid | 是 | 策略 ID |
| date | string | 是 | 目标日期 |
| count | int | 否 | 生成词数，默认按 plan 的 daily_word_count，最大 50 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "generated_count": 10,
    "date": "2026-05-22"
  }
}
```

#### 策略逻辑

| 策略类型 | 选词逻辑 |
|---|---|
| `random`（完全随机） | `SELECT word_id FROM word_book_entries WHERE word_book_id=? ORDER BY RAND() LIMIT :count` |
| `sequential`（顺序递进） | 按 sort_order 顺序选取尚未学过的词 + 已学过但阶段较低的优先 |
| `spaced`（间隔复习优先） | 选取到期复习词优先，不足时补新词 |
| `difficulty_asc`（先易后难） | `ORDER BY w.difficulty ASC, RAND() LIMIT :count` |
| `difficulty_desc`（先难后易） | `ORDER BY w.difficulty DESC, RAND() LIMIT :count` |

```sql
-- 以 random 为例
INSERT INTO daily_plan_items (uuid, user_id, word_book_id, word_id, plan_date, sort_order, strategy_id)
SELECT UUID(), :user_id, :word_book_id, wbe.word_id, :date, ROW_NUMBER() OVER (), :strategy_id
FROM word_book_entries wbe
WHERE wbe.word_book_id = :word_book_id
  AND wbe.word_id NOT IN (
    SELECT word_id FROM daily_plan_items
    WHERE user_id = :user_id AND plan_date = :date
  )
ORDER BY RAND()
LIMIT :count;
```

---

## 12. 单词本浏览

### 12.1 获取单词本列表

```
GET /api/word-books
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| difficulty_level | query | string | 否 | 筛选等级，如 `CET-4`、`CET-6`、`GRE` |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "books": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440800",
        "name": "四级单词",
        "description": "大学英语四级核心词汇",
        "difficulty_level": "CET-4",
        "word_count": 2500,
        "icon": "📘",
        "color": "#4CAF50",
        "sort_order": 1,
        "is_active": true
      }
    ]
  }
}
```

#### SQL

```sql
SELECT wb.*, COUNT(wbe.id) AS word_count
FROM word_books wb
LEFT JOIN word_book_entries wbe ON wb.id = wbe.word_book_id
WHERE wb.is_active = TRUE
  AND (:difficulty_level IS NULL OR wb.difficulty_level = :difficulty_level)
GROUP BY wb.id
ORDER BY wb.sort_order;
```

---

### 12.2 获取单词本词条（预览）

```
GET /api/word-books/:id/words
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| id | path | uuid | 是 | 单词本 ID |
| pos | query | string | 否 | 筛选词性，支持逗号分隔多个：`vt.,vi.` |
| letter | query | string | 否 | 筛选首字母（大写单个字母） |
| page | query | int | 否 | 默认 1 |
| size | query | int | 否 | 默认 30，最大 100 |
| search | query | string | 否 | 按单词原文关键词搜索 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "book": { "id": "uuid", "name": "四级单词" },
    "filters": {
      "pos_categories": [
        { "label": "动词", "key": "v", "pos_list": ["vt.", "vi.", "v."], "count": 1500 },
        { "label": "名词", "key": "n", "pos_list": ["n."], "count": 3000 },
        { "label": "形容词", "key": "adj", "pos_list": ["adj."], "count": 800 }
      ],
      "letters": ["A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"]
    },
    "words": [
      {
        "id": "uuid",
        "word": "abandon",
        "pos": "vt.",
        "meaning_cn": "放弃；遗弃",
        "first_letter": "A",
        "difficulty": 2,
        "frequency": 80,
        "is_in_plan": false,
        "is_completed": false
      }
    ],
    "pagination": { "page": 1, "size": 30, "total": 2500, "total_pages": 84 }
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| data.words[].is_in_plan | bool | 当前词是否已在用户的某个计划中 |
| data.words[].is_completed | bool | 当前词是否已完成（is_in_plan 为真时有效） |

#### SQL

```sql
SELECT w.id, w.word, w.pos, w.meaning_cn, w.first_letter, w.difficulty, w.frequency,
  CASE WHEN upe.id IS NOT NULL OR dpi.id IS NOT NULL THEN TRUE ELSE FALSE END AS is_in_plan,
  CASE WHEN upe.is_completed OR dpi.is_completed THEN TRUE ELSE FALSE END AS is_completed
FROM word_book_entries wbe
JOIN words w ON wbe.word_id = w.id
LEFT JOIN user_daily_plan_entries upe
  ON upe.word_id = w.id AND upe.user_id = :user_id AND upe.plan_date >= CURDATE()
LEFT JOIN daily_plan_items dpi
  ON dpi.word_id = w.id AND dpi.user_id = :user_id AND dpi.plan_date >= CURDATE()
WHERE wbe.word_book_id = :id
  AND (:pos IS NULL OR w.pos REGEXP :pos_regex)  -- 用 REGEXP 匹配多个
  AND (:letter IS NULL OR w.first_letter = :letter)
  AND (:search IS NULL OR w.word LIKE CONCAT('%', :search, '%'))
ORDER BY wbe.sort_order
LIMIT :size OFFSET (:page - 1) * :size;
```

---

### 12.3 获取词性分类（供前端筛选栏使用）

```
GET /api/word-books/pos-categories
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "categories": [
      { "key": "v", "label": "动词", "pos_list": ["vt.", "vi.", "v."] },
      { "key": "n", "label": "名词", "pos_list": ["n."] },
      { "key": "adj", "label": "形容词", "pos_list": ["adj."] },
      { "key": "adv", "label": "副词", "pos_list": ["adv."] },
      { "key": "prep", "label": "介词", "pos_list": ["prep."] },
      { "key": "conj", "label": "连词", "pos_list": ["conj."] },
      { "key": "pron", "label": "代词", "pos_list": ["pron."] },
      { "key": "art", "label": "冠词", "pos_list": ["art."] },
      { "key": "num", "label": "数词", "pos_list": ["num."] },
      { "key": "other", "label": "其他", "pos_list": ["int.", "abbr.", "aux."] }
    ]
  }
}
```

此为静态数据，可直接在代码中定义，无需数据库查询。

---

### 12.4 获取学习策略列表

```
GET /api/strategies
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "strategies": [
      {
        "id": "uuid",
        "name": "完全随机",
        "description": "从单词本中完全随机抽取",
        "type": "random",
        "sort_order": 1
      },
      {
        "id": "uuid",
        "name": "艾宾浩斯递进",
        "description": "按艾宾浩斯遗忘曲线安排新旧词比例（70% 复习 + 30% 新词）",
        "type": "spaced",
        "sort_order": 2
      },
      {
        "id": "uuid",
        "name": "顺序渐进",
        "description": "按单词本编排顺序从前往后学习",
        "type": "sequential",
        "sort_order": 3
      }
    ]
  }
}
```

#### SQL

```sql
SELECT * FROM study_strategies WHERE is_active = TRUE ORDER BY sort_order;
```

---

### 12.5 获取用户默认策略

```
GET /api/user/default-strategy
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "strategy_id": "uuid",
    "name": "完全随机",
    "type": "random"
  }
}
```

#### SQL

```sql
SELECT ss.*
FROM users u
JOIN study_strategies ss ON u.default_strategy_id = ss.id
WHERE u.id = :user_id;
```

---

### 12.6 设置用户默认策略

```
PUT /api/user/default-strategy
```

#### 请求参数

```json
{ "strategy_id": "uuid" }
```

#### SQL

```sql
UPDATE users SET default_strategy_id = :strategy_id WHERE id = :user_id;
```

---

## 13. 个人中心

### 13.1 获取个人信息

```
GET /api/user/profile
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "uuid",
    "username": "demo",
    "nickname": "演示用户",
    "avatar_url": "https://cdn.example.com/avatars/default.png",
    "bio": "英语学习者",
    "email": "demo@example.com",
    "phone": null,
    "role": "user",
    "level": 5,
    "xp": 320,
    "xp_next_level": 500,
    "streak_days": 3,
    "longest_streak": 15,
    "total_words_learned": 320,
    "total_reviews": 1245,
    "total_time_spent_sec": 102600,
    "accuracy": 87.5,
    "default_strategy_id": "uuid",
    "created_at": "2026-01-15"
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| accuracy | float | 历史正确率百分比（0-100） |

#### SQL

```sql
SELECT u.*, us.xp, us.level, us.streak_days, us.longest_streak,
  us.total_words_learned, us.total_reviews, us.total_time_spent_sec
FROM users u
JOIN user_stats us ON u.id = us.user_id
WHERE u.id = :user_id;

-- 正确率
SELECT ROUND(
  SUM(is_correct) / COUNT(*) * 100, 1
) AS accuracy
FROM review_log
WHERE user_id = :user_id;
```

---

### 13.2 更新资料

```
PUT /api/user/profile
```

#### 请求参数

```json
{
  "nickname": "新昵称",
  "bio": "新简介",
  "avatar_url": "https://cdn.example.com/avatars/new.png"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| nickname | string | 否 | 昵称 2-30 字符 |
| bio | string | 否 | 个人简介，最长 200 字符 |
| avatar_url | string | 否 | 头像 URL |

#### SQL

```sql
UPDATE users
SET nickname = COALESCE(:nickname, nickname),
    bio = COALESCE(:bio, bio),
    avatar_url = COALESCE(:avatar_url, avatar_url)
WHERE id = :user_id;
```

---

### 13.3 获取设置

```
GET /api/user/settings
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "settings": {
      "daily_word_goal": "20",
      "learning_mode": "card",
      "pronunciation": "uk",
      "theme": "light",
      "reminder_time": "08:00",
      "ui_language": "zh",
      "is_public_stats": "true"
    }
  }
}
```

#### SQL

```sql
SELECT setting_key, setting_value FROM user_settings WHERE user_id = :user_id;
```

---

### 13.4 保存设置（批量）

```
PUT /api/user/settings
```

#### 请求参数

```json
{
  "daily_word_goal": "25",
  "theme": "dark"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| 任意 key-value | string | 否 | key 为设置项，value 为设置值 |

#### SQL

```sql
INSERT INTO user_settings (uuid, user_id, setting_key, setting_value)
VALUES (:uuid, :user_id, :key, :value)
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
```

---

### 13.5 获取近 N 天学习活动

```
GET /api/user/activity
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| days | query | int | 否 | 天数，默认 7，最大 365 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "activity": [
      { "date": "2026-05-15", "words_studied": 12, "reviews_done": 18, "time_spent_sec": 540, "correct_count": 15, "wrong_count": 3 }
    ]
  }
}
```

#### SQL

```sql
SELECT activity_date, words_studied, reviews_done, time_spent_sec, correct_count, wrong_count
FROM learning_activities
WHERE user_id = :user_id AND activity_date >= DATE_SUB(CURDATE(), INTERVAL :days DAY)
ORDER BY activity_date;
```

---

### 13.6 获取徽章

```
GET /api/user/badges
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "badges": [
      {
        "id": "uuid",
        "name": "初次学习",
        "icon": "🎯",
        "description": "完成第一次学习",
        "condition_desc": "完成 1 次复习",
        "condition_type": "review_count",
        "condition_value": 1,
        "is_earned": true,
        "earned_at": "2026-05-01"
      },
      {
        "id": "uuid",
        "name": "连续打卡 7 天",
        "icon": "🔥",
        "description": "连续学习 7 天",
        "is_earned": false,
        "earned_at": null
      }
    ],
    "earned_count": 3,
    "total_count": 15
  }
}
```

#### SQL

```sql
SELECT b.*, ub.earned_at
FROM badges b
LEFT JOIN user_badges ub ON b.id = ub.badge_id AND ub.user_id = :user_id
ORDER BY b.sort_order;
```

---

## 14. 排行榜 & 徽章

### 14.1 总榜

```
GET /api/leaderboard
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| type | query | string | 否 | `global`（默认，总 XP） / `weekly`（近 7 天活跃度） / `streak`（连续天数） |
| limit | query | int | 否 | 默认 100，最大 200 |

#### 响应（global）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "type": "global",
    "my_rank": 42,
    "leaderboard": [
      {
        "rank": 1,
        "user_id": "uuid",
        "username": "admin",
        "nickname": "管理员",
        "avatar_url": null,
        "xp": 2450,
        "level": 12,
        "streak_days": 15,
        "accuracy": 89.5
      }
    ]
  }
}
```

#### SQL

```sql
-- global
SELECT u.id, u.username, u.nickname, u.avatar_url, us.xp, us.level, us.streak_days,
  ROW_NUMBER() OVER (ORDER BY us.xp DESC) AS rank
FROM user_stats us
JOIN users u ON us.user_id = u.id
JOIN user_settings uset ON uset.user_id = u.id AND uset.setting_key = 'is_public_stats' AND uset.setting_value = 'true'
ORDER BY us.xp DESC
LIMIT :limit;

-- 我的排名
SELECT ranked.rank FROM (
  SELECT user_id, ROW_NUMBER() OVER (ORDER BY xp DESC) AS rank
  FROM user_stats
) ranked WHERE ranked.user_id = :user_id;

-- weekly
SELECT u.id, u.username, u.avatar_url,
  SUM(la.words_studied + la.reviews_done) AS weekly_activity,
  ROW_NUMBER() OVER (ORDER BY SUM(la.words_studied + la.reviews_done) DESC) AS rank
FROM learning_activities la
JOIN users u ON la.user_id = u.id
JOIN user_settings uset ON uset.user_id = u.id AND uset.setting_key = 'is_public_stats' AND uset.setting_value = 'true'
WHERE la.activity_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY u.id
ORDER BY weekly_activity DESC
LIMIT :limit;
```

---

### 14.2 全量徽章列表

```
GET /api/badges
```

同 13.6，但返回所有徽章无需用户状态。

#### SQL

```sql
SELECT * FROM badges WHERE is_active = TRUE ORDER BY sort_order;
```

---

## 15. 管理后台

### 15.1 平台总览

```
GET /api/admin/overview
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total_users": 1234,
    "active_today": 89,
    "total_words": 5678,
    "total_reviews": 45678,
    "new_users_today": 5,
    "total_articles": 120
  }
}
```

#### SQL

```sql
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM words;
SELECT COUNT(*) FROM articles;
SELECT COUNT(DISTINCT user_id) FROM learning_activities WHERE activity_date = CURDATE();
SELECT COUNT(*) FROM users WHERE DATE(created_at) = CURDATE();
SELECT COUNT(*) FROM review_log;
```

---

### 15.2 用户管理

```
GET /api/admin/users
```

#### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| page | query | int | 否 | 默认 1 |
| size | query | int | 否 | 默认 20 |
| keyword | query | string | 否 | 搜索用户名/昵称/邮箱 |
| role | query | string | 否 | 筛选角色 |
| is_active | query | bool | 否 | 筛选是否激活 |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "uuid",
        "username": "admin",
        "nickname": "管理员",
        "email": "admin@example.com",
        "role": "admin",
        "is_active": true,
        "created_at": "2026-01-01",
        "last_login": "2026-05-21"
      }
    ],
    "pagination": { "page": 1, "size": 20, "total": 1234, "total_pages": 62 }
  }
}
```

#### SQL

```sql
SELECT id, username, nickname, email, role, is_active, created_at, last_login
FROM users
WHERE (:keyword IS NULL OR username LIKE CONCAT('%', :keyword, '%')
   OR nickname LIKE CONCAT('%', :keyword, '%')
   OR email LIKE CONCAT('%', :keyword, '%'))
  AND (:role IS NULL OR role = :role)
  AND (:is_active IS NULL OR is_active = :is_active)
ORDER BY created_at DESC
LIMIT :size OFFSET (:page - 1) * :size;
```

---

### 15.3 禁用/启用用户

```
PUT /api/admin/users/:id/status
```

```json
{ "is_active": false }
```

```sql
UPDATE users SET is_active = :is_active WHERE uuid = :uuid;
```

---

### 15.4 词库管理

```
GET /api/admin/words
POST /api/admin/words
PUT /api/admin/words/:id
DELETE /api/admin/words/:id
```

#### 新增单词（POST）

```json
{
  "word": "abandon",
  "phonetic_uk": "/əˈbændən/",
  "phonetic_us": "/əˈbændən/",
  "pos": "vt.",
  "meaning_cn": "放弃；遗弃",
  "source": "CET-4",
  "difficulty": 2,
  "frequency": 80
}
```

```sql
INSERT INTO words (uuid, word, phonetic_uk, phonetic_us, pos, meaning_cn, source, difficulty, frequency, first_letter)
VALUES (:uuid, :word, :phonetic_uk, :phonetic_us, :pos, :meaning_cn, :source, :difficulty, :frequency, UPPER(LEFT(:word, 1)));
```

#### 删除单词

```sql
DELETE FROM word_relations WHERE word_id = :wordId OR related_word_id = :wordId;
DELETE FROM collocations WHERE word_id = :wordId;
DELETE FROM prep_patterns WHERE word_id = :wordId;
DELETE FROM examples WHERE word_id = :wordId;
DELETE FROM definitions WHERE word_id = :wordId;
DELETE FROM word_book_entries WHERE word_id = :wordId;
DELETE FROM words WHERE id = :wordId;  -- INT PK from resolved entity
```

---

### 15.5 批量导入单词

```
POST /api/admin/words/batch-import
```

#### 请求参数

```json
{
  "words": [
    { "word": "abandon", "pos": "vt.", "meaning_cn": "放弃", "difficulty": 2, "source": "CET-4" },
    { "word": "remarkable", "adj.", "meaning_cn": "显著的", "difficulty": 2, "source": "CET-4" }
  ],
  "word_book_id": "uuid"
}
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "imported": 98,
    "skipped": 2,
    "errors": [
      { "row": 3, "reason": "duplicate word" }
    ]
  }
}
```

#### SQL

```sql
INSERT IGNORE INTO words (uuid, word, pos, meaning_cn, difficulty, source, first_letter)
VALUES (UUID(), :word, :pos, :meaning_cn, :difficulty, :source, UPPER(LEFT(:word, 1)));

-- 同时关联到单词本
INSERT INTO word_book_entries (uuid, word_book_id, word_id, sort_order)
SELECT UUID(), :word_book_id, w.id, :sort_order
FROM words w WHERE w.word = :word;
```

---

### 15.6 内容反馈

```
GET /api/admin/feedback
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "uuid",
        "username": "demo",
        "entity_type": "example",
        "entity_id": "uuid",
        "rating": 5,
        "feedback": "非常实用",
        "created_at": "2026-05-20"
      }
    ],
    "pagination": { "page": 1, "size": 20, "total": 5, "total_pages": 1 }
  }
}
```

#### SQL

```sql
SELECT cr.*, u.username
FROM content_ratings cr
JOIN users u ON cr.user_id = u.id
WHERE cr.feedback IS NOT NULL AND cr.feedback != ''
ORDER BY cr.created_at DESC
LIMIT :size OFFSET (:page - 1) * :size;
```

---

## 附录 A：通用接口

### A.1 用户注册

```
POST /api/auth/register
```

#### 请求参数

```json
{
  "username": "newuser",
  "password": "securePassword123",
  "email": "newuser@example.com",
  "nickname": "新用户"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 用户名 3-20 字符，字母数字下划线，唯一 |
| password | string | 是 | 密码 6-128 字符，BCrypt 加密存储 |
| email | string | 是 | 邮箱，唯一 |
| nickname | string | 否 | 昵称，默认同 username |

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "expires_in": 86400,
    "user": {
      "id": "uuid",
      "username": "newuser",
      "nickname": "新用户",
      "role": "user"
    }
  }
}
```

#### SQL

```sql
-- 检查唯一性
SELECT id FROM users WHERE username = :username OR email = :email;
-- 若已存在返回 409

INSERT INTO users (uuid, username, password_hash, email, nickname, role, created_at)
VALUES (:uuid, :username, :password_hash, :email, COALESCE(:nickname, :username), 'user', NOW());

INSERT INTO user_stats (uuid, user_id, xp, level, streak_days, longest_streak, total_words_learned, total_reviews)
VALUES (:uuid, :user_id, 0, 1, 0, 0, 0, 0);
```

---

### A.2 用户登录

```
POST /api/auth/login
```

#### 请求参数

```json
{
  "username": "demo",
  "password": "xxx"
}
```

#### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "expires_in": 86400,
    "user": {
      "id": "uuid",
      "username": "demo",
      "nickname": "演示用户",
      "avatar_url": null,
      "role": "user",
      "level": 5
    }
  }
}
```

```sql
SELECT id, username, password_hash, nickname, avatar_url, role
FROM users WHERE username = :username AND is_active = TRUE;
-- 校验 password_hash 与输入密码
```

---

### A.3 退出登录

```
POST /api/auth/logout
```

#### 请求参数

无（服务端可做 token 黑名单，也可由客户端直接丢弃）

---

## 附录 B：词性分类字典

用于前端词性筛选栏，以下为静态映射表：

```json
[
  { "key": "v",    "label": "动词",   "pos_list": ["vt.", "vi.", "v.", "aux.", "link-v."] },
  { "key": "n",    "label": "名词",   "pos_list": ["n."] },
  { "key": "adj",  "label": "形容词", "pos_list": ["adj.", "adj-comp.", "adj-sup."] },
  { "key": "adv",  "label": "副词",   "pos_list": ["adv."] },
  { "key": "prep", "label": "介词",   "pos_list": ["prep."] },
  { "key": "conj", "label": "连词",   "pos_list": ["conj."] },
  { "key": "pron", "label": "代词",   "pos_list": ["pron."] },
  { "key": "art",  "label": "冠词",   "pos_list": ["art."] },
  { "key": "num",  "label": "数词",   "pos_list": ["num."] },
  { "key": "det",  "label": "限定词", "pos_list": ["det."] },
  { "key": "other","label": "其他",   "pos_list": ["int.", "abbr.", "phr.", "suf.", "pref."] }
]
```

---

> **文档版本**: v2.1  
> **最后更新**: 2026-05-22  
> **关联数据库**: word_learning v8（36 表，INT AUTO_INCREMENT PK + UUID 双键）  
> **修改记录**:  
>   - v1.0: 初版，按页面列出接口路径与 SQL  
>   - v2.0: 增加统一响应格式、错误码、每个接口的请求参数表（含位置/类型/必填/说明）、响应字段表、完整 SQL（含参数绑定 `:param` 写法）  
>   - v2.1: 数据库迁移 INT AUTO_INCREMENT PK + UUID CHAR(36) UNIQUE；所有 SQL 中 `id` PK 列改为 `uuid` 列用于外部查询，INSERT 中的 `UUID()` 改为 `:uuid` 参数；API 契约保持不变
