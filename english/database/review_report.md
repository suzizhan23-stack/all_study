# 英语词汇知识库 · 设计审查报告 v2

> **审查时间:** 2026-05-14
> **版本:** schema.sql v4（10 表）
> **审查角色:** 数据库架构 + 知识库系统设计

---

## 一、审查结论

**评级：B+（良好，有改进空间）**

| 维度 | 评分 | 说明 |
|------|:----:|------|
| 结构规范 | ★★★★☆ | 表设计合理，多数字段有约束 |
| 查询性能 | ★★★★☆ | 索引覆盖主要路径，复合索引就绪 |
| 数据完整性 | ★★★★☆ | CHECK + ENUM + UNIQUE 三重防护 |
| 知识库语义 | ★★★☆☆ | 支持同义词/标签网络，但缺少全文搜索 |
| 扩展性 | ★★★★☆ | 10 表覆盖单词学习全维度 |
| 开发友好 | ★★★☆☆ | 部分字段不规范，需应用层额外处理 |

---

## 二、已解决的遗留问题

上轮审查发现的问题已全部修复并验证通过：

| 问题 | 状态 | 验证方式 |
|------|:----:|----------|
| stage/confidence 无范围约束 | ✅ | CHECK (stage 0-3, confidence 0-5) |
| difficulty/frequency 无范围约束 | ✅ | CHECK (0-4) |
| ease_factor 无范围约束 | ✅ | CHECK (1.30-3.00) |
| relation_type 自由文本 | ✅ | ENUM 6 种关系 |
| word_variants 可重复 | ✅ | UNIQUE (word_id, variant, region) |
| word_variants 无时间戳 | ✅ | added created_at |
| 同义词示例自引用 | ✅ | 已移除 |
| 缺少 confidence/frequency 索引 | ✅ | 已添加 |

---

## 三、本轮新增审查发现

### 🔴 问题 1：缺少全文搜索能力

```sql
-- 当前无法做到：
SELECT * FROM words WHERE MATCH(word, meaning_cn) AGAINST('abandon');
SELECT * FROM definitions WHERE MATCH(meaning_en, meaning_cn) AGAINST('give up');
```

**影响：** 知识库的核心功能是"搜索"。当前只能精确匹配 `WHERE word = 'abandon'` 或 `LIKE '%abandon%'`（全表扫描）。无法搜索释义、例句中的关键词。

**修复：**

```sql
ALTER TABLE words ADD FULLTEXT INDEX ft_words (word, meaning_cn, etymology, etymology_cn);
ALTER TABLE definitions ADD FULLTEXT INDEX ft_defs (meaning_en, meaning_cn);
ALTER TABLE examples ADD FULLTEXT INDEX ft_examples (sentence_en, sentence_cn);
```

**注意：** MySQL 的 FULLTEXT 索引对中文支持有限（需要 ngram 分词器）：

```sql
ALTER TABLE words ADD FULLTEXT INDEX ft_words_cn (meaning_cn, etymology_cn)
  WITH PARSER ngram;
```

**优先级：** 🟡 P1 — 知识库不应该只能按单词查，也应该能按意思查。

---

### 🟡 问题 2：`definitions.meaning_cn` 长度与 `words.meaning_cn` 不一致

```sql
words.meaning_cn       VARCHAR(500)    -- 摘要用
definitions.meaning_cn VARCHAR(500)    -- 释义用
```

两个字段长度相同，但语义不同（一个是摘要，一个是完整释义）。当定义较长时，500 可能不够。

**建议：** `definitions.meaning_cn` 改为 `TEXT`，与 `meaning_en` 保持一致。`words.meaning_cn` 保持 VARCHAR(500) 作为摘要截断。

```sql
ALTER TABLE definitions MODIFY COLUMN meaning_cn TEXT NOT NULL COMMENT '中文释义';
```

**优先级：** 🔵 P3

---

### 🟡 问题 3：`word_forms.form_type` 自由文本

```sql
form_type VARCHAR(20) NOT NULL COMMENT 'past/participle/third/gerund/plural/comparative/superlative'
```

自由文本允许拼写错误（如写 "pass" 而不是 "past"）。

**建议：** 改为 ENUM：

```sql
ALTER TABLE word_forms MODIFY COLUMN form_type
  ENUM('past','participle','third','gerund','plural','comparative','superlative') NOT NULL;
```

**优先级：** 🔵 P3

---

### 🟡 问题 4：`word_relations` 缺少 `created_at`

除 `word_relations` 外所有辅助表都有时间戳。

```sql
ALTER TABLE word_relations ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
```

**优先级：** 🔵 P3

---

### 🟡 问题 5：缺少 `bookmarked` 标记

用户可能想标记"待复习"或"收藏"的单词，当前只能通过 `stage=1`（学习中）来近似表达，语义不够明确。

**建议：**

```sql
ALTER TABLE words ADD COLUMN bookmarked BOOLEAN NOT NULL DEFAULT FALSE COMMENT '收藏标记';
ALTER TABLE words ADD INDEX idx_bookmarked (bookmarked);
```

**优先级：** 🔵 P3

---

### 🔵 问题 6：`meaning_en` 类型差异

```sql
words:       无 meaning_en（只有 meaning_cn 摘要）
definitions: meaning_en TEXT, meaning_cn VARCHAR(500)
```

当列表页需要同时显示中英文简短释义时，需要 JOIN `definitions` 并取 `sort_order=0` 的第一条，或者把 `meaning_en` 也冗余到 `words` 表。

**建议：** 

```sql
-- 方案 A：添加冗余字段
ALTER TABLE words ADD COLUMN meaning_en VARCHAR(500) NULL COMMENT '英文释义摘要';
-- 应用层保持与 definitions 第一条同步

-- 或方案 B：用视图
CREATE VIEW word_summary AS
SELECT w.id, w.word, w.phonetic_uk, w.meaning_cn,
       d.meaning_en AS meaning_en
FROM words w
LEFT JOIN definitions d ON d.word_id = w.id AND d.sort_order = 0;
```

**优先级：** 🔵 P3

---

## 四、已知限制（暂不处理）

| 限制 | 原因 | 替代方案 |
|------|------|----------|
| 无外键约束 | 开发灵活性 | 应用层保证 |
| FULLTEXT 中文依赖 ngram | 需 MySQL 5.7+/8.0 | 可用 ES 或 MeiliSearch 替代 |
| 无用户系统 | 单机个人场景 | 暂不需要 |
| `sort_order` 无唯一约束 | 允许手动调整排序 | 应用层保证 |
| `pos` 自由文本 | 兼容复杂词性 | 保持现状 |

---

## 五、数据量预估与性能

| 数据量 | words | definitions | examples | 性能表现 |
|:------:|:-----:|:-----------:|:--------:|----------|
| 10³ | 1,000 | 1,500 | 10,000 | 无需优化 |
| 10⁴ | 10,000 | 15,000 | 100,000 | 全索引覆盖，单表 < 10ms |
| 10⁵ | 100,000 | 150,000 | 1,000,000 | 需要复合索引、分区 |
| 10⁶ | 1,000,000 | 1,500,000 | 10,000,000 | 建议分表/ES |

对于个人学习场景（10³~10⁴ 级别），当前设计无需额外优化。

---

## 六、最终评分

```
结构完整性    ████████░░  8/10  10 表，关系清晰
数据约束      ████████░░  8/10  CHECK + ENUM + UNIQUE
索引覆盖      ████████░░  8/10  15 索引，含复合索引
知识库语义    ██████░░░░  6/10  缺全文搜索
扩展性        ████████░░  8/10  word_relations/word_tags 就绪
开发友好      ███████░░░  7/10  部分字段需应用层处理

综合评分      ████████░░  7.5/10  满足个人学习场景需求
```
