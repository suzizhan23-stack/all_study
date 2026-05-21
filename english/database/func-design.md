# 英语学习网站 — 功能设计文档

> 版本：v1.0  
> 关联：数据库 v7（30 表）

---

## 目录

1. [站点地图](#1-站点地图)
2. [首页 / Dashboard](#2-首页--dashboard)
3. [词典搜索](#3-词典搜索)
4. [单词详情页](#4-单词详情页)
5. [单词学习（卡牌/测验）](#5-单词学习卡牌测验)
6. [阅读文章](#6-阅读文章)
7. [收藏夹管理](#7-收藏夹管理)
8. [错题本](#8-错题本)
9. [学习计划](#9-学习计划)
10. [个人中心](#10-个人中心)
11. [排行榜 & 徽章](#11-排行榜--徽章)
12. [管理后台](#12-管理后台)

---

## 1. 站点地图

```
  ┌─────────────────────────────────────────────────────────────────┐
  │                        英语学习网站                               │
  └─────────────────────────────────────────────────────────────────┘
       │
       ├── 🏠 首页 (Dashboard)
       │     ├── 今日学习进度卡片
       │     ├── 连续打卡天数
       │     ├── 每日推荐列表
       │     └── 快捷操作（继续学习、快速查词）
       │
       ├── 🔍 词典搜索
       │     ├── 搜索框（联想下拉）
       │     ├── 搜索结果
       │     └── 搜索历史
       │
       ├── 📖 单词详情
       │     ├── 基本信息（发音/释义/词源）
       │     ├── 搭配/介词短语/例句
       │     ├── 用户操作（收藏/笔记/标签/评分/频率）
       │     └── 同反义词
       │
       ├── 🎴 单词学习
       │     ├── 卡牌模式 / 选择题 / 拼写 / 听力
       │     ├── 学习进度
       │     └── 答题反馈
       │
       ├── 📰 文章阅读
       │     ├── 文章列表
       │     ├── 阅读器（点词查义、进度同步）
       │     └── 文章详情
       │
       ├── 📁 收藏夹
       │     ├── 收藏夹列表
       │     ├── 收藏内容
       │     └── 公开收藏夹
       │
       ├── ❌ 错题本
       │     ├── 错题列表（按词/按题型）
       │     ├── 错题统计
       │     └── 针对性练习
       │
       ├── 📋 学习计划
       │     ├── 计划列表
       │     ├── 计划详情（第X天/共X天）
       │     └── 今日任务
       │
       ├── 👤 个人中心
       │     ├── 资料编辑
       │     ├── 偏好设置
       │     ├── 学习统计
       │     └── 徽章墙
       │
       ├── 🏆 排行榜
       │     ├── 总榜 / 周榜 / 好友榜
       │     └── 徽章墙
       │
       └── ⚙️ 管理后台 (admin)
             ├── 用户管理
             ├── 词库管理
             ├── 文章管理
             └── 数据统计
```

---

## 2. 首页 / Dashboard

### 2.1 页面设计

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  🔍 [    搜索单词...    ]            👤 demo用户  ⚙️ 设置  🚪     │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                      │
  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
  │  │  今日学习      │  │  连续打卡     │  │  等级                    │  │
  │  │  12/20 词     │  │  🔥 3 天     │  │  Lv.5  320 XP          │  │
  │  │  ████████░░░  │  │  最长 15 天  │  │  ████████░░░░░ 60%     │  │
  │  └──────────────┘  └──────────────┘  └──────────────────────────┘  │
  │                                                                      │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  今日推荐                                           更多 →  │   │
  │  ├──────────────────────────────────────────────────────────────┤   │
  │  │  📌 abandon       间隔复习到期       ⭐ 复习   👍👍👍    │   │
  │  │  📌 remarkable    易错词巩固         ⭐ 复习               │   │
  │  │  📌 contribute    推荐新词           ▶️ 学习               │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
  │  │  🎴 继续学习      │  │  📰 文章推荐     │  │  ❌ 错题回顾     │  │
  │  │  5 个词待复习    │  │  3 篇未读完     │  │  2 个易错词     │  │
  │  └──────────────────┘  └──────────────────┘  └──────────────────┘  │
  └─────────────────────────────────────────────────────────────────────┘
```

### 2.2 功能 & 数据查询

| 功能 | 说明 | 查询语句 / 数据源 |
|---|---|---|
| **今日进度** | 当日学习/复习词数 vs 目标 | `SELECT * FROM learning_activities WHERE user_id=? AND activity_date=CURDATE()` <br>`SELECT setting_value FROM user_settings WHERE user_id=? AND setting_key='daily_word_goal'` |
| **连续打卡** | streak_days + longest_streak | `SELECT streak_days, longest_streak FROM user_stats WHERE user_id=?` |
| **等级/XP** | 等级进度条 | `SELECT level, xp FROM user_stats WHERE user_id=?` |
| **今日推荐** | 自适应推送列表 | `SELECT d.*, w.word FROM daily_recommendations d JOIN words w ON d.entity_type='word' AND d.entity_id=w.id WHERE d.user_id=? AND d.recommend_date=CURDATE() AND d.is_consumed=FALSE ORDER BY FIELD(reason,'间隔复习到期','易错词巩固','新词推荐')` |
| **继续学习** | SM-2 到期待复习 | `SELECT COUNT(*) FROM words WHERE next_review <= NOW() LIMIT 5` |
| **文章推荐** | 未读完的文章 | `SELECT a.* FROM articles a JOIN reading_progress rp ON a.id=rp.article_id WHERE rp.user_id=? AND rp.is_completed=FALSE ORDER BY rp.last_read_at DESC LIMIT 3` |
| **错题概览** | 近期高频错词 | `SELECT w.word, COUNT(*) AS wrong_count FROM review_log rl JOIN words w ON rl.word_id=w.id WHERE rl.user_id=? AND rl.is_correct=FALSE AND rl.reviewed_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY w.id, w.word ORDER BY wrong_count DESC LIMIT 5` |

---

## 3. 词典搜索

### 3.1 页面设计

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  🔍 [ abandon                        ]  🔎                        │
  │     ┌──────────────────────────────────┐                           │
  │     │ abandon  放弃；遗弃；抛弃         │  ← 联想下拉              │
  │     │ abandoned 被抛弃的               │                           │
  │     │ abandonment 放弃                 │                           │
  │     └──────────────────────────────────┘                           │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                      │
  │  搜索结果: 找到 1 个                                                  │
  │                                                                      │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  abandon  /əˈbændən/  🔊 🔊                                 │   │
  │  │  vt.  CET-4  难度 ⭐⭐                                      │   │
  │  │  放弃；遗弃；抛弃                                              │   │
  │  │  [查看详情 →]  [⭐收藏]  [📝笔记]  [🏷️标签]                 │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  搜索历史                                                      │   │
  │  │  abandon     13:20     │  remarkable  昨天                     │   │
  │  │  contribution 昨天     │  🗑️ 清除历史                         │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  └─────────────────────────────────────────────────────────────────────┘
```

### 3.2 功能 & 数据查询

| 功能 | 说明 | 数据源 |
|---|---|---|
| **联想搜索** | 输入前缀实时匹配 | `SELECT word, meaning_cn FROM words WHERE word LIKE CONCAT(:query, '%') ORDER BY frequency DESC, word ASC LIMIT 8` |
| **单词搜索** | 精确匹配 + 关联数据 | `SELECT * FROM words WHERE word = :query OR word LIKE CONCAT(:query, '%') ORDER BY FIELD(word, :query) DESC, frequency DESC LIMIT 20` |
| **记录搜索历史** | 每次搜索都记录 | `INSERT INTO search_history (id, user_id, query, result_count) VALUES (UUID(), ?, ?, ?)` |
| **搜索历史** | 最近搜索展示 | `SELECT DISTINCT query, MAX(searched_at) AS last_searched FROM search_history WHERE user_id=? GROUP BY query ORDER BY last_searched DESC LIMIT 10` |
| **清除历史** | 删除个人搜索记录 | `DELETE FROM search_history WHERE user_id=?` |

---

## 4. 单词详情页

### 4.1 页面设计

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  ← 返回                              demo用户  ⭐收藏  📝笔记     │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                      │
  │  abandon  /əˈbændən/          🔊 UK  🔊 US                         │
  │  vt.  放弃；遗弃；抛弃                                              │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  ⭐ 个人频率: [===●=========] 72    📊 默认频率: 80        │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌─ 详细释义 ───────────────────────────────────────────────────┐   │
  │  │  ① to leave completely and finally; to give up               │   │
  │  │     放弃；遗弃；抛弃                                          │   │
  │  │  ② to stop doing something before it is finished             │   │
  │  │     中止；停止                                                │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌─ 固定搭配 ─────────────────────┬─ 介词模式 ─────────────────┐   │
  │  │  abandon hope        放弃希望  │  abandon sth to sb  把…丢给 │   │
  │  │  abandon ship        弃船      │  abandon sth for sth 放弃…  │   │
  │  │  abandon a plan      放弃计划  │  abandon hope of    放弃…希 │   │
  │  └────────────────────────────────┴──────────────────────────────┘   │
  │                                                                      │
  │  ┌─ 例句 ───────────────────────────────────────────────────────┐   │
  │  │  📌 The captain ordered the crew to abandon the sinking ship.│   │
  │  │     船长命令船员弃船。            [CET-4 2019-06]    👍5     │   │
  │  │  📌 He abandoned his research...                              │   │
  │  │     经过多年努力他放弃了研究。     [考研英语 2018]    👍3     │   │
  │  │  [查看全部 5 条]                                              │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌─ 词源 ──────────────────────────────────────────────────────┐   │
  │  │  源自古法语 abandoner，意为"置于控制之下"                     │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌─ 同反义词 ──────────────────────────────────────────────────┐   │
  │  │  同义: desert, give up, relinquish, forsake                  │   │
  │  │  反义: keep, retain, maintain                                │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌─ 我的笔记 ──────────────────────────────────────────────────┐   │
  │  │  这个词在考研阅读里出现过好几次，注意它和 desert 的区别。    │   │
  │  │  [编辑] [公开/私密]                                           │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌─ 我的标签 ──────────────────────────────────────────────────┐   │
  │  │  [#写作词汇] [#考试必备]  [+ 添加标签]                       │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  [🎴 开始学习这个词]  [❌ 报错]  [👍 有用(5)]                     │
  └─────────────────────────────────────────────────────────────────────┘
```

### 4.2 功能 & 数据查询

| 功能 | 数据源 |
|---|---|
| **单词基本信息** | `SELECT * FROM words WHERE id=?` |
| **发音音频** | `SELECT audio_uk, audio_us FROM words WHERE id=?` |
| **释义** | `SELECT * FROM definitions WHERE word_id=? ORDER BY sort_order` |
| **固定搭配** | `SELECT * FROM collocations WHERE word_id=? ORDER BY frequency DESC, sort_order` |
| **介词模式** | `SELECT * FROM prep_patterns WHERE word_id=? ORDER BY frequency DESC` |
| **例句** | `SELECT e.*, COALESCE(cr.avg_rating, 0) AS rating FROM examples e LEFT JOIN (SELECT entity_id, AVG(rating) AS avg_rating FROM content_ratings WHERE entity_type='example' GROUP BY entity_id) cr ON e.id=cr.entity_id WHERE e.word_id=? ORDER BY e.frequency DESC` |
| **词源** | `SELECT etymology, etymology_cn FROM words WHERE id=?` |
| **同反义词** | `SELECT wr.relation_type, w.word FROM word_relations wr JOIN words w ON wr.related_word_id=w.id WHERE wr.word_id=? UNION SELECT wr.relation_type, w.word FROM word_relations wr JOIN words w ON wr.word_id=w.id WHERE wr.related_word_id=?` |
| **个人频率** | `SELECT frequency FROM user_frequencies WHERE user_id=? AND entity_type='word' AND entity_id=?` |
| **设置频率** (拖拽滑块时) | `INSERT INTO user_frequencies (id, user_id, entity_type, entity_id, frequency) VALUES (UUID(), ?, 'word', ?, ?) ON DUPLICATE KEY UPDATE frequency=VALUES(frequency)` |
| **用户笔记** | `SELECT * FROM user_notes WHERE user_id=? AND entity_type='word' AND entity_id=?` |
| **保存笔记** | `INSERT INTO user_notes ... ON DUPLICATE KEY UPDATE content=VALUES(content)` |
| **用户标签** | `SELECT ut.* FROM user_tags ut JOIN user_entity_tags uet ON ut.id=uet.tag_id WHERE uet.user_id=? AND uet.entity_type='word' AND uet.entity_id=?` |
| **用户所有标签** (添加下拉) | `SELECT * FROM user_tags WHERE user_id=? ORDER BY tag` |
| **添加标签** | `INSERT INTO user_entity_tags (user_id, tag_id, entity_type, entity_id) VALUES (?, ?, 'word', ?)` |
| **内容评分** | `SELECT rating FROM content_ratings WHERE user_id=? AND entity_type='word' AND entity_id=?` |
| **提交评分** | `INSERT INTO content_ratings ... ON DUPLICATE KEY UPDATE rating=VALUES(rating)` |
| **收藏状态** | `SELECT f.id, ff.name AS folder_name FROM favorites f JOIN favorite_folders ff ON f.folder_id=ff.id WHERE ff.user_id=? AND f.entity_type='word' AND f.entity_id=?` |
| **个人频率排序 (搭配)** | `SELECT c.*, COALESCE(uf.frequency, c.frequency) AS sort_freq FROM collocations c LEFT JOIN user_frequencies uf ON uf.entity_type='collocation' AND uf.entity_id=c.id AND uf.user_id=? WHERE c.word_id=? ORDER BY sort_freq DESC` |

---

## 5. 单词学习（卡牌/测验）

### 5.1 页面设计

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  🎴 学习模式: [卡牌] [选择] [拼写] [听力]      进度 5/20 ███░░░░  │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                      │
  │                         ┌──────────────┐                            │
  │                         │              │                            │
  │                         │   abandon     │  ← 卡牌正面: 显示英文    │
  │                         │              │                            │
  │                         │  [点击翻转]   │                            │
  │                         │              │                            │
  │                         └──────────────┘                            │
  │                                                                      │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  提示: 这个词在考研阅读中出现过 5 次                         │   │
  │  │  上次复习: 3 天前   │  答对率: 3/5                           │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │          [😢 忘记了]     [🤔 模糊]     [😊 记住了]                │
  │                                                                      │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  本次学习统计: 已学 5 词 | 正确 4 | 错误 1 | 耗时 12m       │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  └─────────────────────────────────────────────────────────────────────┘
```

### 5.2 功能 & 数据查询

| 功能 | 数据源 |
|---|---|
| **获取学习队列** | `SELECT * FROM words WHERE next_review IS NULL OR next_review <= NOW() ORDER BY stage ASC, next_review ASC LIMIT 20` (SM-2 队列) |
| **获取推荐队列** | `SELECT w.* FROM daily_recommendations dr JOIN words w ON dr.entity_id=w.id WHERE dr.user_id=? AND dr.recommend_date=CURDATE() AND dr.is_consumed=FALSE AND dr.entity_type='word' ORDER BY FIELD(dr.reason, ...)` |
| **记录答题结果** | `INSERT INTO review_log (id, user_id, word_id, quiz_type, is_correct, response_time_ms, wrong_answer) VALUES (UUID(), ?, ?, ?, ?, ?, ?)` |
| **更新 SM-2 参数** | `UPDATE words SET consecutive_correct=?, ease_factor=?, interval_days=?, next_review=DATE_ADD(NOW(), INTERVAL ? DAY), review_count=review_count+1, last_reviewed_at=NOW(), stage=? WHERE id=?` |
| **更新每日学习日志** | `INSERT INTO learning_activities (id, user_id, activity_date, words_studied, reviews_done, time_spent_sec, correct_count, wrong_count) VALUES (UUID(), ?, CURDATE(), 1, 1, ?, ?, ?) ON DUPLICATE KEY UPDATE words_studied=words_studied+1, reviews_done=reviews_done+1, time_spent_sec=time_spent_sec+VALUES(time_spent_sec), correct_count=correct_count+VALUES(correct_count), wrong_count=wrong_count+VALUES(wrong_count)` |
| **标记推荐已消费** | `UPDATE daily_recommendations SET is_consumed=TRUE WHERE id=?` |
| **增加 XP** | `UPDATE user_stats SET xp=xp+? WHERE user_id=?` (答对+10, 连续+5等) |
| **更新打卡** | `UPDATE user_stats SET streak_days=IF(EXISTS(SELECT 1 FROM learning_activities WHERE user_id=? AND activity_date=DATE_SUB(CURDATE(), INTERVAL 1 DAY)), streak_days+1, 1), longest_streak=GREATEST(longest_streak, IF(...)) WHERE user_id=?` |
| **选择题模式** | `SELECT w.word, w.meaning_cn FROM words w WHERE w.id=?` (正确答案) + `SELECT word, meaning_cn FROM words ORDER BY RAND() LIMIT 3` (干扰项, 同一词性) |
| **拼写模式** | `SELECT meaning_cn, phonetic_uk FROM words WHERE id=?` (显示中文+音标, 用户输入英文) |
| **答对后推送同反义词** | `SELECT w.word, wr.relation_type FROM word_relations wr JOIN words w ON wr.related_word_id=w.id WHERE wr.word_id=?` |

---

## 6. 阅读文章

### 6.1 页面设计

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  📰 文章列表                                👤 demo  [+ 收藏文章] │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                      │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  ⭐ The Economic Logic of Climate Policy                    │   │
  │  │  The Economist · 2024-03 · 难度 ⭐⭐⭐ · 1200词           │   │
  │  │  ████████████████████░░░░ 80%  继续阅读 →                   │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  🔖 How Memory Works: The Science Behind Forgetting          │   │
  │  │  Nature · 2024-01 · 难度 ⭐⭐⭐⭐ · 2000词                  │   │
  │  │  ████░░░░░░░░░░░░░░░░░░ 15%  继续阅读 →                     │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  ├─────────────────────────────────────────────────────────────────────┤
  │  📖 The Economic Logic of Climate Policy                            │
  │                                                                      │
  │  Climate change is one of the most pressing challenges of our       │
  │  time. [abandon → 放弃] ──┐  ← 点击单词弹出释义浮窗               │
  │  The economic logic behind │                                        │
  │  climate policy is...     │  abandon /əˈbændən/                    │
  │                            │  vt. 放弃；遗弃；抛弃                  │
  │  ┌────────────────────────┘  [⭐收藏] [📝笔记] [🔊]              │
  │  │                                                               │
  │  │                              ┌─────────────────────────┐      │
  │  │                              │ 已读 80%  ████████████   │      │
  │  │                              └─────────────────────────┘      │
  │                                                                      │
  │  ┌──── 文章操作 ───────────────────────────────────────────────┐   │
  │  │  查词: 5个  |  已收藏 3个生词  |  读完标记  [❌ 退出]       │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  └─────────────────────────────────────────────────────────────────────┘
```

### 6.2 功能 & 数据查询

| 功能 | 数据源 |
|---|---|
| **文章列表** | `SELECT a.*, rp.scroll_position, rp.is_completed, rp.words_looked_up FROM articles a LEFT JOIN reading_progress rp ON a.id=rp.article_id AND rp.user_id=? ORDER BY rp.is_completed ASC, rp.last_read_at DESC` |
| **文章内容** | `SELECT * FROM articles WHERE id=?` |
| **阅读进度** | `SELECT * FROM reading_progress WHERE user_id=? AND article_id=?` |
| **保存进度** | `INSERT INTO reading_progress (id, user_id, article_id, scroll_position) VALUES (UUID(), ?, ?, ?) ON DUPLICATE KEY UPDATE scroll_position=VALUES(scroll_position), last_read_at=NOW()` |
| **标记完成** | `UPDATE reading_progress SET is_completed=TRUE WHERE user_id=? AND article_id=?` |
| **点击查词** | `SELECT * FROM words WHERE word=?` → 同时: `UPDATE reading_progress SET words_looked_up=words_looked_up+1 WHERE user_id=? AND article_id=?` |
| **文章内收藏单词** | `INSERT INTO favorites (id, folder_id, entity_type, entity_id) VALUES (UUID(), ?, 'word', ?)` |
| **获取文章难度匹配词** (生词标注) | `SELECT word FROM words WHERE difficulty > (SELECT difficulty FROM articles WHERE id=?) AND id NOT IN (SELECT entity_id FROM favorites WHERE user_id=? AND entity_type='word') ORDER BY RAND() LIMIT 10` |

---

## 7. 收藏夹管理

### 7.1 页面设计

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  📁 我的收藏夹                                [+ 新建收藏夹]       │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                      │
  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
  │  │  📁 稍后复习   │  │  📁 精彩例句  │  │  📁 地道搭配  │             │
  │  │  word         │  │  example     │  │  phrase      │             │
  │  │  12 个条目    │  │  5 个条目    │  │  8 个条目    │             │
  │  │  [默认]       │  │              │  │              │             │
  │  └──────────────┘  └──────────────┘  └──────────────┘             │
  │                                                                      │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  ← 稍后复习 (word)                         [编辑] [分享]    │   │
  │  ├──────────────────────────────────────────────────────────────┤   │
  │  │  □ abandon   放弃       ⭐ freq 72  📝笔记  🏷️标签  🗑️  │   │
  │  │  □ remarkable 显著的    ⭐ freq 50  📝笔记  🏷️标签  🗑️  │   │
  │  │  □ contribute 贡献      ⭐ freq 30            🗑️          │   │
  │  │  ...                                                         │   │
  │  │  [批量删除]  [批量加标签]  共 12 项                          │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  └─────────────────────────────────────────────────────────────────────┘
```

### 7.2 功能 & 数据查询

| 功能 | 数据源 |
|---|---|
| **收藏夹列表** | `SELECT ff.*, COUNT(f.id) AS item_count FROM favorite_folders ff LEFT JOIN favorites f ON ff.id=f.folder_id WHERE ff.user_id=? GROUP BY ff.id ORDER BY ff.sort_order` |
| **收藏夹内容** | `SELECT f.*, w.word, w.meaning_cn FROM favorites f JOIN words w ON f.entity_type='word' AND f.entity_id=w.id WHERE f.folder_id=? ORDER BY f.created_at DESC` (按 entity_type 切换 JOIN) |
| **新建收藏夹** | `INSERT INTO favorite_folders (id, user_id, name, category) VALUES (UUID(), ?, ?, ?)` |
| **编辑收藏夹** | `UPDATE favorite_folders SET name=? WHERE id=? AND user_id=?` |
| **删除收藏夹** | `DELETE FROM favorites WHERE folder_id=?; DELETE FROM favorite_folders WHERE id=? AND user_id=?` (禁止删除 is_default=1) |
| **收藏条目** (从详情页) | `INSERT INTO favorites (id, folder_id, entity_type, entity_id, note) VALUES (UUID(), ?, ?, ?, ?)` |
| **取消收藏** | `DELETE FROM favorites WHERE id=?` |
| **公开/私密切换** | `UPDATE favorite_folders SET is_public=? WHERE id=? AND user_id=?` |
| **查看公开收藏夹** | `SELECT * FROM favorite_folders WHERE is_public=1 AND user_id=?` (他人视角) |

---

## 8. 错题本

### 8.1 页面设计

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  ❌ 错题本                      [全部] [拼写] [听力] [释义] ⬇️  │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                      │
  │  ┌─ 统计 ───────────────────────────────────────────────────────┐   │
  │  │   近7天错题: 12 个   |   最高错词: abandon (错3次)           │   │
  │  │   薄弱题型: 拼写 (错7次)  >  释义 (错3次)  >  听力 (错2次)  │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  ❌ abandon     错 3 次                     [巩固复习]       │   │
  │  │     最近错误: 拼写  "abanden"   2026-05-20                  │   │
  │  │              释义  "丢弃"       2026-05-19                  │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  ❌ remarkable  错 2 次                     [巩固复习]       │   │
  │  │     最近错误: 拼写  "remarkble"  2026-05-18                 │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  [一键复习全部错题]                                                  │
  └─────────────────────────────────────────────────────────────────────┘
```

### 8.2 功能 & 数据查询

| 功能 | 数据源 |
|---|---|
| **按词统计错题** | `SELECT rl.word_id, w.word, w.meaning_cn, COUNT(*) AS wrong_count, MAX(rl.reviewed_at) AS last_wrong FROM review_log rl JOIN words w ON rl.word_id=w.id WHERE rl.user_id=? AND rl.is_correct=FALSE GROUP BY rl.word_id ORDER BY wrong_count DESC, last_wrong DESC` |
| **按题型筛选** | 同上 + `AND rl.quiz_type=?` |
| **某词错题详情** | `SELECT rl.* FROM review_log rl WHERE rl.user_id=? AND rl.word_id=? AND rl.is_correct=FALSE ORDER BY rl.reviewed_at DESC` |
| **薄弱题型分析** | `SELECT quiz_type, COUNT(*) AS wrong_count FROM review_log WHERE user_id=? AND is_correct=FALSE AND reviewed_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY quiz_type ORDER BY wrong_count DESC` |
| **一键复习错题** | `SELECT DISTINCT rl.word_id FROM review_log rl WHERE rl.user_id=? AND rl.is_correct=FALSE GROUP BY rl.word_id ORDER BY MAX(rl.reviewed_at) ASC` → 送入学习队列 |
| **生成每日推荐错题** | `INSERT INTO daily_recommendations (id, user_id, recommend_date, entity_type, entity_id, reason) SELECT UUID(), ?, CURDATE(), 'word', word_id, '易错词巩固' FROM (SELECT word_id, COUNT(*) AS cnt FROM review_log WHERE user_id=? AND is_correct=FALSE AND reviewed_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY word_id HAVING cnt >= 2) t ON DUPLICATE KEY UPDATE recommend_date=CURDATE()` |

---

## 9. 学习计划

### 9.1 页面设计

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  📋 学习计划                                   [浏览全部计划]     │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                      │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  进行中                                                        │   │
  │  │  ┌──────────────────────────────────────────────────────┐    │   │
  │  │  │  📋 CET-4 30天冲刺                                   │    │   │
  │  │  │  第 3 天 / 共 30 天                                  │    │   │
  │  │  │  ██████░░░░░░░░░░░░░░░░░░░░░░░░░░  10%               │    │   │
  │  │  │  今日任务: 20 词  |  已完成 12 词                     │    │   │
  │  │  │  [继续学习 →]                                         │    │   │
  │  │  └──────────────────────────────────────────────────────┘    │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  可选计划                                                      │   │
  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │   │
  │  │  │ 考研词汇60天  │  │ 雅思7分词汇  │  │ GRE 核心词  │       │   │
  │  │  │ 60天·每日15词 │  │ 45天·每日25词│  │ 90天·每日30 │       │   │
  │  │  │ [加入]        │  │ [加入]        │  │ [加入]      │       │   │
  │  │  └──────────────┘  └──────────────┘  └──────────────┘       │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  └─────────────────────────────────────────────────────────────────────┘
```

### 9.2 功能 & 数据查询

| 功能 | 数据源 |
|---|---|
| **用户当前计划** | `SELECT up.*, lp.name, lp.description, lp.duration_days, lp.daily_word_count, lp.target_level FROM user_plans up JOIN learning_plans lp ON up.plan_id=lp.id WHERE up.user_id=? AND up.completed_at IS NULL ORDER BY up.started_at DESC LIMIT 1` |
| **今日任务进度** | `SELECT COALESCE(la.words_studied, 0) AS done, lp.daily_word_count AS target FROM learning_activities la RIGHT JOIN user_plans up ON up.user_id=? AND up.completed_at IS NULL JOIN learning_plans lp ON up.plan_id=lp.id AND la.user_id=up.user_id AND la.activity_date=CURDATE()` |
| **全部计划** | `SELECT * FROM learning_plans WHERE is_active=TRUE ORDER BY sort_order` |
| **加入计划** | `INSERT INTO user_plans (id, user_id, plan_id) VALUES (UUID(), ?, ?)` |
| **进度保存** (每天结束) | `UPDATE user_plans SET current_day=current_day+1 WHERE id=? AND user_id=?` |
| **标记完成** | `UPDATE user_plans SET completed_at=NOW() WHERE id=? AND user_id=? AND current_day >= (SELECT duration_days FROM learning_plans WHERE id=plan_id)` |

---

## 10. 个人中心

### 10.1 页面设计

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  👤 个人中心                                                        │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                      │
  │  ┌── 个人信息 ─────────────────────────────────────────────────┐   │
  │  │  [🖼️ 头像]  demo用户 · Lv.5                                  │   │
  │  │  邮箱: demo@example.com                                      │   │
  │  │  简介: 英语学习者                                            │   │
  │  │  [编辑资料]                                                  │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌── 学习统计 ─────────────────────────────────────────────────┐   │
  │  │  累计学习: 320 词  |  累计复习: 1,245 次                     │   │
  │  │  累计时长: 28.5 小时  |  当前打卡: 🔥 3 天                   │   │
  │  │  最长打卡: 15 天    |  总 XP: 320                           │   │
  │  │  ┌─ 近 7 天学习曲线 ─────────────────────────┐              │   │
  │  │  │  ████  ██████  ██  ████████  █████  ██  █│              │   │
  │  │  │  一   二    三   四   五      六     日  一│              │   │
  │  │  └────────────────────────────────────────────┘              │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌── 偏好设置 ─────────────────────────────────────────────────┐   │
  │  │  📚 每日目标: [20 词  ▼]                                      │   │
  │  │  🎮 学习模式: [卡牌  ▼]                                      │   │
  │  │  🔊 发音偏好: [英式  ▼]                                      │   │
  │  │  🎨 主题: [🌙 深色模式]                                      │   │
  │  │  ⏰ 提醒时间: [08:00]                                        │   │
  │  │  🌐 界面语言: [中文  ▼]                                      │   │
  │  │  [保存设置]                                                  │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌── 徽章墙 ─────────────────────────────────────────────────┐   │
  │  │  🎯 初次学习    🔥 打卡7天(未获得)  💪 百词斩(未获得)      │   │
  │  │  [查看全部 →]                                               │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  [🏆 排行榜]  [🔒 修改密码]  [🚪 退出登录]                       │   │
  └─────────────────────────────────────────────────────────────────────┘
```

### 10.2 功能 & 数据查询

| 功能 | 数据源 |
|---|---|
| **个人信息展示** | `SELECT * FROM users WHERE id=?` |
| **更新资料** | `UPDATE users SET nickname=?, avatar_url=?, bio=? WHERE id=?` |
| **学习统计** | `SELECT * FROM user_stats WHERE user_id=?` |
| **近7天活动** | `SELECT activity_date, words_studied, reviews_done FROM learning_activities WHERE user_id=? AND activity_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) ORDER BY activity_date` |
| **读取偏好** | `SELECT setting_key, setting_value FROM user_settings WHERE user_id=?` (批量读，缓存到本地) |
| **保存偏好** (单个) | `INSERT INTO user_settings (id, user_id, setting_key, setting_value) VALUES (UUID(), ?, ?, ?) ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value)` |
| **批量保存偏好** | 逐条 UPSERT，或用事务包裹 |
| **已获得徽章** | `SELECT b.*, ub.earned_at FROM user_badges ub JOIN badges b ON ub.badge_id=b.id WHERE ub.user_id=? ORDER BY ub.earned_at DESC` |
| **全部徽章** (含未获得) | `SELECT b.*, ub.earned_at FROM badges b LEFT JOIN user_badges ub ON b.id=ub.badge_id AND ub.user_id=? ORDER BY b.sort_order` |

---

## 11. 排行榜 & 徽章

### 11.1 页面设计

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  🏆 排行榜          [🏅 总榜] [🔥 周榜] [👥 好友]               │
  ├─────────────────────────────────────────────────────────────────────┤
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  🥇 1  admin       Lv.12  2,450 XP   🔥 15 天    👍 89%   │   │
  │  │  🥈 2  demo        Lv.5     320 XP   🔥 3 天     👍 75%   │   │
  │  │  🥉 3  user1       Lv.4     280 XP   🔥 5 天     👍 82%   │   │
  │  │  4    user2       Lv.3     150 XP   🔥 2 天     👍 70%   │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  │                                                                      │
  │  ┌──────────────────────────────────────────────────────────────┐   │
  │  │  🏅 徽章墙                                                      │   │
  │  ├──────────────────────────────────────────────────────────────┤   │
  │  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐              │   │
  │  │  │🎯新手│ │🔥7天 │ │💪百词│ │🧠学霸│ │📚书虫│              │   │
  │  │  │ 已获  │ │ 未获  │ │ 未获  │ │ 未获  │ │ 未获  │              │   │
  │  │  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘              │   │
  │  └──────────────────────────────────────────────────────────────┘   │
  └─────────────────────────────────────────────────────────────────────┘
```

### 11.2 功能 & 数据查询

| 功能 | 数据源 |
|---|---|
| **总榜** | `SELECT u.id, u.username, u.avatar_url, us.xp, us.level, us.streak_days FROM user_stats us JOIN users u ON us.user_id=u.id WHERE us.is_public=1 ORDER BY us.xp DESC LIMIT 100` |
| **周榜** | `SELECT u.id, u.username, SUM(la.words_studied + la.reviews_done) AS weekly_activity FROM learning_activities la JOIN users u ON la.user_id=u.id JOIN user_stats us ON u.id=us.user_id WHERE la.activity_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND us.is_public=1 GROUP BY u.id ORDER BY weekly_activity DESC LIMIT 100` |
| **好友榜** | 同上 + `AND u.id IN (SELECT friend_id FROM user_friends WHERE user_id=?)` (需建好友表) |
| **徽章检查** (学习事件触发) | 每次学习事件后遍历 badges → `SELECT * FROM badges WHERE criteria->'$.type' = 'streak' AND criteria->'$.days' <= (SELECT streak_days FROM user_stats WHERE user_id=?)` 等 |

---

## 12. 管理后台

### 12.1 页面设计

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  ⚙️ 管理后台             admin 管理员                              │
  ├──────┬──────────────────────────────────────────────────────────────┤
  │      │                                                               │
  │  📊  │  ┌─ 平台概览 ──────────────────────────────────────────┐   │
  │  总览 │  │  总用户: 1,234  |  总词汇: 5,678  |  今日活跃: 89  │   │
  │      │  └──────────────────────────────────────────────────────┘   │
  │  👥  │                                                               │
  │  用户 │  ┌─ 用户列表 ──────────────────────────────────────────┐   │
  │      │  │  admin   管理员    2026-01-01  [编辑] [禁用]         │   │
  │  📖  │  │  demo    user      2026-05-20  [编辑] [禁用]         │   │
  │  词库 │  │  ...                                               │   │
  │      │  └──────────────────────────────────────────────────────┘   │
  │  📰  │                                                               │
  │  文章 │  ┌─ 词库管理 ──────────────────────────────────────────┐   │
  │      │  │  [批量导入] [新增单词]                                │   │
  │  📋  │  │  abandon  vt.  放弃  编辑 删除                       │   │
  │  数据 │  │  remarkable  adj. 显著的  编辑 删除                  │   │
  │      │  └──────────────────────────────────────────────────────┘   │
  └──────┴──────────────────────────────────────────────────────────────┘
```

### 12.2 功能 & 数据查询

| 功能 | 数据源 |
|---|---|
| **平台总览** | `SELECT COUNT(*) FROM users` + `SELECT COUNT(*) FROM words` + `SELECT COUNT(DISTINCT user_id) FROM learning_activities WHERE activity_date=CURDATE()` |
| **用户管理** | `SELECT * FROM users ORDER BY created_at DESC` |
| **禁用用户** | `UPDATE users SET is_active=0 WHERE id=?` |
| **内容管理** | `SELECT * FROM words ORDER BY created_at DESC` (分页) |
| **批量导入** | `LOAD DATA LOCAL INFILE` 或逐条 INSERT |
| **内容审核** (反馈) | `SELECT cr.*, u.username FROM content_ratings cr JOIN users u ON cr.user_id=u.id WHERE cr.feedback IS NOT NULL ORDER BY cr.created_at DESC` |

---

## 附录：关键业务 SQL 汇总

### A. 首页查询总合

```sql
-- 首页全量数据（一次查完，减少 N+1）
SELECT
  (SELECT COALESCE(SUM(words_studied), 0) FROM learning_activities
   WHERE user_id = ? AND activity_date = CURDATE()) AS today_words,
  (SELECT setting_value FROM user_settings
   WHERE user_id = ? AND setting_key = 'daily_word_goal') AS daily_goal,
  (SELECT streak_days FROM user_stats WHERE user_id = ?) AS streak,
  (SELECT level FROM user_stats WHERE user_id = ?) AS level,
  (SELECT xp FROM user_stats WHERE user_id = ?) AS xp,
  (SELECT COUNT(*) FROM words WHERE next_review <= NOW()) AS due_reviews;
```

### B. 排行榜（总榜）

```sql
SELECT
  u.id, u.username, u.avatar_url,
  us.xp, us.level, us.streak_days,
  ROW_NUMBER() OVER (ORDER BY us.xp DESC) AS rank
FROM user_stats us
JOIN users u ON us.user_id = u.id
WHERE us.is_public = 1
ORDER BY us.xp DESC
LIMIT 100;
```

### C. 答题后更新事务

```sql
START TRANSACTION;

INSERT INTO review_log (id, user_id, word_id, quiz_type, is_correct, response_time_ms, wrong_answer)
VALUES (UUID(), ?, ?, ?, ?, ?, ?);

UPDATE words SET
  consecutive_correct = CASE WHEN ? THEN consecutive_correct + 1 ELSE 0 END,
  ease_factor = GREATEST(1.3, LEAST(3.0, ease_factor + ?)),
  interval_days = ?,
  next_review = DATE_ADD(NOW(), INTERVAL ? DAY),
  review_count = review_count + 1,
  last_reviewed_at = NOW(),
  stage = ?
WHERE id = ?;

INSERT INTO learning_activities (id, user_id, activity_date, words_studied, reviews_done, time_spent_sec, correct_count, wrong_count)
VALUES (UUID(), ?, CURDATE(), 0, 1, ?, ?, ?)
ON DUPLICATE KEY UPDATE
  reviews_done = reviews_done + 1,
  time_spent_sec = time_spent_sec + VALUES(time_spent_sec),
  correct_count = correct_count + VALUES(correct_count),
  wrong_count = wrong_count + VALUES(wrong_count);

UPDATE user_stats SET
  xp = xp + ?,
  total_reviews = total_reviews + 1
WHERE user_id = ?;

COMMIT;
```

---

> **文档版本**: v1.0  
> **最后更新**: 2026-05-21  
> **关联数据库**: word_learning v7 (30 表)
