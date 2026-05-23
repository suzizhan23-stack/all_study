# 前端 API 调用参考文档

> 文档版本：2026-05-23  
> 覆盖范围：`english/frontend/` 项目全部 72 个 API 端点调用情况

---

## 一、HTTP 客户端层（`src/api/http.js`）

### 1.1 Token 管理

| 函数 | 实现 | 说明 |
|---|---|---|
| `getToken()` | `localStorage.getItem('auth_token')` | 从 localStorage 读取 JWT |
| `setToken(token)` | `localStorage.setItem('auth_token', token)` | 登录/注册成功后写入 JWT |
| `removeToken()` | `localStorage.removeItem('auth_token')` | 登出时清除 JWT |

### 1.2 核心请求函数 `request(url, options)`

**执行流程：**
```
request(url, options)
  │
  ├── url 自动加前缀 /api
  ├── 从 localStorage 读取 token
  ├── 设置请求头:
  │     Content-Type: application/json
  │     Authorization: Bearer {token}  (如果有 token)
  │
  ├── fetch() 发起 HTTP 请求
  │
  ├── 响应 401:
  │     → removeToken()
  │     → window.location.href = '/login'  (强制跳转)
  │     → throw Error('Unauthorized')
  │
  ├── 响应非 2xx:
  │     → throw Error(body.message || 'HTTP {status}')
  │
  └── 响应 2xx:
        → return body.data  (解包 ApiResponse 的 data 字段)
```

**关键规则：** `request()` 返回的是 `ApiResponse.data`，不是完整响应体。  
后端响应格式：`{ code: 200, message: "success", data: { ... } }` → 前端拿到的是 `{ ... }`

### 1.3 快捷方法

| 方法 | 实现 | 示例 |
|---|---|---|
| `api.get(url, params)` | 过滤 null/undefined 参数 → 拼 query string → `request(url + qs)` | `api.get('/api/search', { q: 'hello', page: 1 })` |
| `api.post(url, data)` | `request(url, { method: 'POST', body: JSON.stringify(data) })` | `api.post('/api/auth/login', { username, password })` |
| `api.put(url, data)` | `request(url, { method: 'PUT', body: JSON.stringify(data) })` | `api.put('/api/user/profile', { nickname })` |
| `api.delete(url, data)` | 有 data 则带 body，无 data 则无 body | `api.delete('/api/favorites/123')` |

---

## 二、API 端点层（`src/api/index.js`）

### 2.1 authApi — 认证

| 函数 | HTTP | 路径 | 请求体 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `register(data)` | POST | `/api/auth/register` | `{ username, password, email, nickname? }` | `{ token: string, expiresIn: number, user: UserInfo }` | `stores/user.js:login()` |
| `login(data)` | POST | `/api/auth/login` | `{ username, password }` | `{ token: string, expiresIn: number, user: UserInfo }` | `stores/user.js:register()` |
| `logout()` | POST | `/api/auth/logout` | — | `void` | ❌ 已声明但未被调用 |

### 2.2 userApi — 用户信息

| 函数 | HTTP | 路径 | 参数/请求体 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `getProfile()` | GET | `/api/user/profile` | — | `{ id, uuid, username, nickname, avatarUrl, bio, email, role, level, xp, xpNextLevel, streakDays, longestStreak, totalWordsLearned, totalReviews, totalTimeSpentSec, accuracy, defaultStrategyId, createdAt }` | `user.fetchProfile()` → main.js, AppLayout, Dashboard, login/register |
| `updateProfile(data)` | PUT | `/api/user/profile` | `{ nickname?, bio?, avatarUrl? }` | `void` | `Profile.vue` |
| `getSettings()` | GET | `/api/user/settings` | — | `Map<String, String>` (如 `{ daily_word_goal: "20", learning_mode: "card", pronunciation: "uk", theme: "light", reminder_time: "08:00", ui_language: "zh-CN" })` | `user.fetchSettings()` → Profile.vue |
| `updateSettings(data)` | PUT | `/api/user/settings` | `{ settings: { ... } }` 注意：包裹一层 settings | `void` | `user.updateSettings()` → Profile.vue |
| `getActivity(days)` | GET | `/api/user/activity` | `days=7` (query) | `{ activity: [{ date, wordsStudied, reviewsDone, timeSpentSec, correctCount, wrongCount }] }` | `user.fetchActivity(7)` → Profile.vue |
| `getBadges()` | GET | `/api/user/badges` | — | `{ badges: [...], earnedCount, totalCount }` 或 `BadgeListResponse` | ❌ 已声明但未被调用（改用 badgeApi.getList） |
| `getDefaultStrategy()` | GET | `/api/user/default-strategy` | — | `StrategyItem` | ❌ 已声明但未被调用 |
| `setDefaultStrategy(id)` | PUT | `/api/user/default-strategy` | `{ strategyId }` | `void` | ❌ 已声明但未被调用 |
| `updateStreak()` | PUT | `/api/user/stats/streak` | — | `int` (连续天数) | `user.updateStreak()` → Dashboard.vue |

### 2.3 wordApi — 单词操作

| 函数 | HTTP | 路径 | 参数/请求体 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `getDetail(id)` | GET | `/api/words/{id}` | `id` 为 PathVariable（单词 UUID） | `WordDetailResponse`（含 word, phoneticUk, phoneticUs, audioUk, audioUs, pos, meaningCn, etymologyCn, source, difficulty, frequency + definitions[], collocations[], prepPatterns[], examples[], relations{ synonyms, antonyms }, userData{ stage, confidence, nextReview, reviewCount, frequency, favorites[], notes, tags[], rating }, relatedArticles[] ） | `wordStore.fetchWordDetail(id)` → WordDetail.vue |
| `updateFrequency(id, frequency)` | PUT | `/api/words/{id}/frequency` | `{ frequency }` (int 1-100) | `void` | ❌ UI 有滑块但未连接 |
| `updateNote(id, content, isPrivate)` | PUT | `/api/words/{id}/note` | `{ content, isPrivate }` | `void` | `wordStore.updateNote()` → WordDetail.vue |
| `addTag(id, tagId)` | POST | `/api/words/{id}/tags` | `{ tagId }` | `void` | ❌ UI 有下拉菜单但未连接 |
| `removeTag(id, tagId)` | DELETE | `/api/words/{id}/tags/{tagId}` | — | `void` | ❌ 未连接 |
| `rate(id, rating)` | PUT | `/api/words/{id}/rating` | `{ rating }` (int) | `void` | ❌ 未连接 |

### 2.4 searchApi — 搜索

| 函数 | HTTP | 路径 | 参数 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `suggest(query, limit)` | GET | `/api/search/suggest` | `query, limit` (query) | `{ suggestions: string[] }` | ❌ 已声明但未被调用 |
| `search(params)` | GET | `/api/search` | `{ q, source?, pos?, page?, size? }` (query) | `{ list: SearchResult[], pagination: { page, size, total, totalPages } }` | `wordStore.search()` → Search.vue |
| `getHistory(limit)` | GET | `/api/search/history` | `limit` (query) | `[{ uuid, query, resultCount, searchedAt }]` | `wordStore.fetchSearchHistory()` → Search.vue |
| `saveHistory(query, resultCount)` | POST | `/api/search/history` | `{ query, resultCount }` | `void` | `wordStore.saveSearchHistory()` → Search.vue |
| `clearHistory()` | DELETE | `/api/search/history` | — | `void` | `wordStore.clearSearchHistory()` → Search.vue |

### 2.5 reviewApi — 复习

| 函数 | HTTP | 路径 | 参数/请求体 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `getQueue(params)` | GET | `/api/review/queue` | `{ mode?, limit?, source? }` (query) | `{ queue: ReviewItem[], total, newWordsAvailable }` | Review.vue |
| `submitResult(data)` | POST | `/api/review/result` | `{ wordId, quizType, isCorrect, responseTimeMs?, wrongAnswer? }` | `{ xpGained, stage, nextReview }` | Review.vue |
| `getDistractors(params)` | GET | `/api/review/distractors` | `{ wordId, pos?, count? }` (query) | `string[]` | ❌ 已声明但未被调用 |
| `getStats()` | GET | `/api/review/stats` | — | `Map<String, Object>` | ❌ 已声明但未被调用 |

### 2.6 articleApi — 文章

| 函数 | HTTP | 路径 | 参数/请求体 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `getList(params)` | GET | `/api/articles` | `{ difficulty?, source?, status?, page?, size? }` (query) | `{ list: ArticleItem[], pagination }` | Reading.vue |
| `getDetail(id)` | GET | `/api/articles/{id}` | — | `ArticleDetailResponse`（含 title, content, author, sourceName, difficulty, wordCount, progress, vocabulary[]） | ReadingArticle.vue |
| `updateProgress(id, data)` | PUT | `/api/articles/{id}/progress` | `{ scrollPosition, readingTimeSec? }` | `void` | ReadingArticle.vue（滚动事件，debounce 1s） |
| `complete(id)` | PUT | `/api/articles/{id}/complete` | — | `void` | ReadingArticle.vue（点击"标记读完"） |
| `lookup(id, word)` | GET | `/api/articles/{id}/lookup` | `word` (query) | `{ word, phonetic, meaning }` | ReadingArticle.vue（点击文章中的单词） |

### 2.7 folderApi — 收藏夹

| 函数 | HTTP | 路径 | 参数/请求体 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `getList(category?)` | GET | `/api/folders` | `category` (query, optional) | `{ folders: FolderItem[] }` | Favorites.vue |
| `create(data)` | POST | `/api/folders` | `{ name, category, isPublic? }` | `FolderItem` | Favorites.vue |
| `update(id, data)` | PUT | `/api/folders/{id}` | `{ name?, isPublic? }` | `void` | ❌ 已声明但未被调用 |
| `delete(id)` | DELETE | `/api/folders/{id}` | — | `void` | Favorites.vue |
| `reorder(order)` | PUT | `/api/folders/reorder` | `{ order: string[] }` (UUID 数组) | `void` | ❌ 已声明但未被调用 |
| `getItems(id, params)` | GET | `/api/folders/{id}/items` | `{ page?, size?, sort? }` (query) | `{ folder: FolderRef, items: FolderItemDetail[], pagination }` | FavoriteFolder.vue |

### 2.8 favoriteApi — 收藏条目

| 函数 | HTTP | 路径 | 请求体 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `add(data)` | POST | `/api/favorites` | `{ folderId, entityType, entityId, note? }` | `void` | ❌ 已声明但未被调用 |
| `remove(id)` | DELETE | `/api/favorites/{id}` | — | `void` | FavoriteFolder.vue |
| `batchDelete(ids)` | POST | `/api/favorites/batch-delete` | `{ ids: string[] }` | `void` | FavoriteFolder.vue |
| `batchTag(wordIds, tagId)` | POST | `/api/favorites/batch-tag` | `{ wordIds: string[], tagId: string }` | `void` | ❌ 已声明但未被调用 |

### 2.9 wordBookApi — 单词本

| 函数 | HTTP | 路径 | 参数 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `getList(difficultyLevel?)` | GET | `/api/word-books` | `difficultyLevel` (query) | `{ books: BookItem[] }` | WordBooks.vue, StudyPlans.vue |
| `getWords(id, params)` | GET | `/api/word-books/{id}/words` | `{ pos?, letter?, search?, page?, size? }` (query) | `{ book, filters, words: WordPreview[], pagination }` | WordBooks.vue |
| `getPosCategories()` | GET | `/api/word-books/pos-categories` | — | `[{ key, label }]` | WordBooks.vue |

### 2.10 planApi — 学习计划

| 函数 | HTTP | 路径 | 参数/请求体 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `getActive()` | GET | `/api/plans/active` | — | `{ id, name, currentDay, durationDays, dailyWordCount, wordBookName, strategyName, progressPct, todayDone, todayTotal }` | StudyPlans.vue |
| `getTemplates()` | GET | `/api/plans/templates` | — | `[{ id, name, description, targetLevel, durationDays, dailyWordCount, isActive, sortOrder }]` | StudyPlans.vue |
| `join(planId)` | POST | `/api/plans/join` | `{ planId }` | `void` | ❌ 已声明但未被调用 |
| `getDailyWords(date)` | GET | `/api/plans/daily/words` | `date` (query, YYYY-MM-DD) | `{ date, total, completed, words: WordEntry[] }` | Learning.vue, WordBooks.vue |
| `getDailyDates(limit)` | GET | `/api/plans/daily/dates` | `limit` (query) | `{ dates: [{ date, count, completed }] }` | Learning.vue |
| `addEntry(data)` | POST | `/api/plans/daily/entries` | `{ wordId, planDate }` | `void` | WordBooks.vue |
| `deleteEntry(id)` | DELETE | `/api/plans/daily/entries/{id}` | — | `void` | Learning.vue |
| `completeEntry(id)` | PUT | `/api/plans/daily/entries/{id}/complete` | — | `void` | ❌ 已声明但未被调用 |
| `generate(data)` | POST | `/api/plans/daily/generate` | `{ wordBookId, strategyId, date?, count? }` | `int` (生成的词条数) | StudyPlans.vue |

### 2.11 dashboardApi — 仪表盘

| 函数 | HTTP | 路径 | 成功响应 | 调用方 |
|---|---|---|---|---|
| `get()` | GET | `/api/dashboard` | `{ today: { wordsStudied, dailyGoal, pct }, stats: { streakDays, longestStreak, level, xp, xpNextLevel }, recommendations: [{ id, entityType, entityId, word, reason, isConsumed }], quick: { dueReviewCount, unreadArticleCount, wrongWordCount } }` | Dashboard.vue |
| `consumeRecommendation(id)` | PUT | `/api/dashboard/recommendations/{id}/consume` | `void` | ❌ 已声明但未被调用 |

### 2.12 leaderboardApi — 排行榜

| 函数 | HTTP | 路径 | 参数 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `get(type, limit)` | GET | `/api/leaderboard` | `type=global&limit=100` (query) | `{ type, myRank: int?, leaderboard: [{ rank, userId, username, nickname, avatarUrl, xp, level, streakDays, accuracy }] }` | Leaderboard.vue |

### 2.13 wrongWordApi — 错词本

| 函数 | HTTP | 路径 | 参数/请求体 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `getList(params)` | GET | `/api/wrong-words` | `{ quizType?, days?, page?, size? }` (query) | `{ stats: { totalWrongWords, recentDays, topWrongWord, weakTypes[] }, words: [{ wordId, word, meaningCn, wrongCount, lastWrong, logs[] }], pagination }` | WrongAnswers.vue |
| `getReviewWords(data)` | POST | `/api/wrong-words/review` | `{ limit?, days? }` | `string[]` (word IDs) | ❌ 已声明但未被调用 |

### 2.14 tagApi — 标签

| 函数 | HTTP | 路径 | 请求体 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `getList()` | GET | `/api/tags` | — | `[{ id, uuid, userId, tag, color, createdAt }]` | ❌ 已声明但未被调用 |
| `create(data)` | POST | `/api/tags` | `{ tag, color }` | `UserTag` | ❌ 已声明但未被调用 |

### 2.15 strategyApi — 学习策略

| 函数 | HTTP | 路径 | 成功响应 | 调用方 |
|---|---|---|---|---|
| `getList()` | GET | `/api/strategies` | `{ strategies: [{ id, name, description, type, config, sortOrder }] }` | WordBooks.vue, StudyPlans.vue |

### 2.16 badgeApi — 徽章

| 函数 | HTTP | 路径 | 成功响应 | 调用方 |
|---|---|---|---|---|
| `getList()` | GET | `/api/badges` | `[{ id, uuid, name, icon, description, criteria, sortOrder, createdAt }]` | `userStore.fetchBadges()` → Profile.vue |

### 2.17 adminApi — 管理后台

| 函数 | HTTP | 路径 | 参数/请求体 | 成功响应 | 调用方 |
|---|---|---|---|---|---|
| `getOverview()` | GET | `/api/admin/overview` | — | `{ totalUsers, activeToday, totalWords, totalReviews, newUsersToday, totalArticles }` | Admin.vue |
| `getUsers(params)` | GET | `/api/admin/users` | `{ keyword?, role?, isActive?, page?, size? }` (query) | `{ list: [{ id, uuid, username, role, isActive, createdAt }], pagination }` | Admin.vue |
| `toggleUserStatus(id, isActive)` | PUT | `/api/admin/users/{id}/status` | `{ isActive }` | `void` | Admin.vue |
| `getWords(params)` | GET | `/api/admin/words` | `{ page, size }` (query) | `{ list: [{ id, word, pos, meaningCn, source }], pagination }` | Admin.vue |
| `createWord(data)` | POST | `/api/admin/words` | `{ word, pos, meaningCn, ... }` | `Word` | ❌ 已声明但未被调用 |
| `updateWord(id, data)` | PUT | `/api/admin/words/{id}` | `{ word, pos, meaningCn, ... }` | `Word` | ❌ 已声明但未被调用 |
| `deleteWord(id)` | DELETE | `/api/admin/words/{id}` | — | `void` | Admin.vue |
| `batchImport(data)` | POST | `/api/admin/words/batch-import` | `{ words: [{ word, pos, meaning_cn }], wordBookId }` | `void` | Admin.vue |
| `getFeedback(params)` | GET | `/api/admin/feedback` | `{ page, size }` (query) | `{ list: [{ id, userId, entityType, entityId, rating, feedback, createdAt }], pagination }` | Admin.vue |

---

## 三、各 Store 中的 API 调用详情

### 3.1 `stores/user.js` — useUserStore

| Store 方法 | 调用的 API | 触发时机 | 响应数据处理 | 错误处理 |
|---|---|---|---|---|
| `login(username, password)` | `authApi.login({ username, password })` | 用户提交登录表单 | `res.token` → `setToken()` → `isLoggedIn=true` → `fetchProfile()` | Login.vue 的 catch 显示 loginError |
| `register(data)` | `authApi.register(data)` | 用户提交注册表单 | 同上 | Login.vue 的 catch 显示 registerError |
| `fetchProfile()` | `userApi.getProfile()` | app 启动、页面挂载、登录/注册后 | `data` 赋值 `user.value`，设置 `isLoggedIn=true` | catch → `removeToken()` + 重置状态 |
| `fetchSettings()` | `userApi.getSettings()` | Profile.vue onMounted | `data` 赋值 `settings.value` | 无 |
| `updateSettings(data)` | `userApi.updateSettings(data)` | 用户点击"保存设置" | 合并到 `settings.value` | 无 |
| `fetchActivity(days)` | `userApi.getActivity(days)` | Profile.vue onMounted (days=7) | `data` 赋值 `activity.value` | 无 |
| `fetchBadges()` | `badgeApi.getList()` | Profile.vue onMounted | `data` 处理为 `{ list, earnedCount=0, totalCount }` 或直接赋值 | 无 |
| `fetchDefaultStrategy()` | `userApi.getDefaultStrategy()` | — | 返回原始数据 | ❌ 未被调用 |
| `setDefaultStrategy(id)` | `userApi.setDefaultStrategy(id)` | — | — | ❌ 未被调用 |
| `updateStreak()` | `userApi.updateStreak()` | Dashboard.vue onMounted | 不处理响应 | silent catch |

### 3.2 `stores/words.js` — useWordStore

| Store 方法 | 调用的 API | 触发时机 | 响应数据处理 | 错误处理 |
|---|---|---|---|---|
| `search(params)` | `searchApi.search(params)` | Search.vue 用户输入搜索 | `res.data || res.list || res` → `searchResults`; `res.pagination || res.meta` → `searchPagination` | try/finally loading=false |
| `getSuggestions(query, limit)` | `searchApi.suggest(query, limit)` | — | 返回原始数据 | ❌ 未被调用 |
| `fetchWordDetail(id)` | `wordApi.getDetail(id)` | WordDetail.vue onMounted | `data` → `currentWord.value` | catch → `error='加载失败'` |
| `updateNote(id, content, isPrivate)` | `wordApi.updateNote(id, content, isPrivate)` | WordDetail.vue 用户点击"保存笔记" | 不处理响应 | 无 |
| `saveSearchHistory(query, resultCount)` | `searchApi.saveHistory(query, resultCount)` | Search.vue 搜索有结果后 | 不处理响应，然后重新读取 store.searchHistory | 无 |
| `clearSearchHistory()` | `searchApi.clearHistory()` | Search.vue 用户点击"清除历史" | 本地 `searchHistory.value = []` | 无 |
| `fetchSearchHistory(limit)` | `searchApi.getHistory(limit)` | Search.vue onMounted | `data` → `searchHistory.value` | 无 |

### 3.3 `stores/wordBooks.js` — useWordBookStore

| Store 方法 | 调用的 API | 触发时机 | 响应数据处理 |
|---|---|---|---|
| `fetchBooks(difficultyLevel)` | `wordBookApi.getList(difficultyLevel)` | WordBooks.vue/StudyPlans.vue onMounted | `data` → `books.value` |
| `fetchStrategies()` | `strategyApi.getList()` | WordBooks.vue/StudyPlans.vue onMounted | `data` → `strategies.value` |
| `fetchPosCategories()` | `wordBookApi.getPosCategories()` | WordBooks.vue onMounted | `data` → `posCategories.value` |
| `fetchBookWords(id, params)` | `wordBookApi.getWords(id, params)` | WordBooks.vue 筛选条件变化 (watch) | `res.data \|\| res.list \|\| res` → `currentBookWords`; `res.pagination` → `currentBookPagination` |

### 3.4 `stores/dailyPlan.js` — useDailyPlanStore

| Store 方法 | 调用的 API | 触发时机 | 响应数据处理 |
|---|---|---|---|
| `fetchDailyWords(date)` | `planApi.getDailyWords(date)` | Learning.vue onMounted / 日期切换 | `data` → `dailyWords.value` |
| `fetchDailyDates(limit)` | `planApi.getDailyDates(limit)` | Learning.vue onMounted / 日期切换 | `data` → `dailyDates.value` |
| `addEntry(data)` | `planApi.addEntry(data)` | WordBooks.vue 点击 + 号 | 不处理响应 |
| `deleteEntry(id)` | `planApi.deleteEntry(id)` | Learning.vue 点击"移出计划" | 不处理响应 |
| `generate(data)` | `planApi.generate(data)` | — | ❌ 未被 Store 调用（直接在 StudyPlans.vue 调用） |

---

## 四、各视图中的直接 API 调用（不经过 Store）

### 4.1 `views/Dashboard.vue`

| 调用 | 时机 | 响应数据映射 |
|---|---|---|
| `dashboardApi.get()` | `onMounted` | `res` → `todayWords`, `dueCount`, `unreadCount`, `wrongCount`, `recommendations` → 统计卡片 + 推荐列表 |
| `userStore.fetchProfile()` | `onMounted` | 刷新用户信息（等级、XP、连续天数） |
| `userStore.updateStreak()` | `onMounted` | 更新连续打卡天数 |

### 4.2 `views/Review.vue`

| 调用 | 时机 | 响应数据映射 |
|---|---|---|
| `reviewApi.getQueue({ mode, limit })` | `onMounted` | `data.queue` → `queue.value`（复习卡牌列表） |
| `reviewApi.submitResult({ wordId, quizType, isCorrect, responseTimeMs })` | 用户答题后 | 不处理响应（本地计分） |

### 4.3 `views/Reading.vue`

| 调用 | 时机 | 响应数据映射 |
|---|---|---|
| `articleApi.getList({ page, size })` | `onMounted` / 翻页 | `res.items` → `articles.value`, `res.total` → `total.value` |

### 4.4 `views/ReadingArticle.vue`

| 调用 | 时机 | 响应数据映射 |
|---|---|---|
| `articleApi.getDetail(articleId)` | `onMounted` | `data` → `Object.assign(article.value, data)` |
| `articleApi.lookup(articleId, word)` | 用户点击文章中的单词 | `data` → `lookupData.value`（音标 + 释义） |
| `articleApi.updateProgress(articleId, { scrollPosition, readingTimeSec })` | 滚动事件（debounce 1s） | 不处理响应 |
| `articleApi.complete(articleId)` | 用户点击"标记读完" | 不处理响应 → 弹出 alert |

### 4.5 `views/StudyPlans.vue`

| 调用 | 时机 | 响应数据映射 |
|---|---|---|
| `planApi.getActive()` | `onMounted` | `data` → `activePlan.value`（活跃计划卡片） |
| `planApi.getTemplates()` | `onMounted` | `data` → `templates.value`（推荐计划列表） |
| `planApi.generate({ wordBookId, strategyId, count })` | 用户点击"开始学习" | `plan` → `activePlan.value` → 跳转 /review |

### 4.6 `views/WordBooks.vue`

| 调用 | 时机 | 响应数据映射 |
|---|---|---|
| `planApi.getDailyWords(today)` | `onMounted` | `data` → `todayEntries` Set（标记已加入今日计划的词） |
| `planApi.addEntry({ wordId, planDate })` | 用户点击 + 号 | 将 wordId 加入 `todayEntries` Set |

### 4.7 `views/Profile.vue`

| 调用 | 时机 | 响应数据映射 |
|---|---|---|
| `userApi.updateProfile(editForm)` | 用户点击"保存"编辑资料 | 不处理响应 → 刷新 profile |

### 4.8 `views/Admin.vue`

| 调用 | 时机 | 响应数据映射 |
|---|---|---|
| `adminApi.getOverview()` | `onMounted` | `totalUsers, totalWords, activeToday` → 概览卡片 |
| `adminApi.getUsers({ page, size })` | `onMounted` | `res.list` → `users.value`（用户表格） |
| `adminApi.getWords({ page, size })` | `onMounted` | `res.list` → `wordList.value`（词库表格） |
| `adminApi.getFeedback({ page, size })` | `onMounted` | `res.list` → `feedbacks.value`（反馈表格） |
| `adminApi.toggleUserStatus(u.id, !u.isActive)` | 用户点击禁用/启用 | 本地切换 `u.isActive` |
| `adminApi.batchImport({ words, wordBookId })` | 用户点击导入 | 清空导入框 → 刷新词表 |
| `adminApi.deleteWord(w.id)` | 用户点击删除 | 从 `wordList.value` 中移除该项 |

---

## 五、调用时机分类统计

### 5.1 按触发时机分布

| 触发时机 | API 调用数量 | 涉及端点 |
|---|---|---|
| **页面挂载 (onMounted)** | 25+ | dashboard, profile, settings, activity, badges, search history, word detail, review queue, article list, article detail, folders, folder items, wrong words, word books, strategies, plans, leaderboard, admin (overview/users/words/feedback) |
| **用户操作** | 15+ | login, register, search, save note, submit review result, create/delete folder, remove/batch-delete favorite, add/delete plan entry, generate plan, save profile, save settings, toggle user status, batch import, delete word |
| **滚动/定时事件** | 2 | article progress save (scroll, debounce 1s), article reading time (interval) |
| **筛选条件变化 (watch)** | 2 | word book word list (pos/letter change), wrong words filter (quizType change) |

### 5.2 按 HTTP 方法分布

| 方法 | 数量 | 说明 |
|---|---|---|
| GET | 27 | 读取数据（大部分是页面初始化） |
| POST | 15 | 创建资源或提交操作 |
| PUT | 13 | 更新资源 |
| DELETE | 8 | 删除资源 |

### 5.3 未被调用的 API（共 20 个）

| 端点 | 函数 | 说明 |
|---|---|---|
| `/api/auth/logout` | `authApi.logout()` | 登出由前端直接清除 token 实现 |
| `/api/user/badges` | `userApi.getBadges()` | 改用 `/api/badges`（公共接口） |
| `/api/user/default-strategy` | `userApi.getDefaultStrategy/setDefaultStrategy` | 默认策略功能尚未在 UI 中实现 |
| `/api/words/{id}/frequency` | `wordApi.updateFrequency()` | 单词详情页有频率滑块但未挂载 onChange |
| `/api/words/{id}/tags` | `wordApi.addTag/removeTag` | 标签功能未在详情页实现 |
| `/api/words/{id}/rating` | `wordApi.rate()` | 评分未实现 |
| `/api/search/suggest` | `searchApi.suggest()` | 搜索建议未做 |
| `/api/review/distractors` | `reviewApi.getDistractors()` | 选择题干扰项由前端随机生成 |
| `/api/review/stats` | `reviewApi.getStats()` | 复习统计未在 UI 中使用 |
| `/api/folders/{id}` PUT | `folderApi.update()` | 编辑文件夹未实现 |
| `/api/folders/reorder` | `folderApi.reorder()` | 拖拽排序未实现 |
| `/api/favorites` POST | `favoriteApi.add()` | 添加收藏未实现 |
| `/api/favorites/batch-tag` | `favoriteApi.batchTag()` | 批量打标签未实现 |
| `/api/plans/join` | `planApi.join()` | 加入计划在 StudyPlans 中直接 generate |
| `/api/plans/daily/entries/{id}/complete` | `planApi.completeEntry()` | 完成单词条目标记未实现 |
| `/api/dashboard/recommendations/{id}/consume` | `dashboardApi.consumeRecommendation()` | 消费推荐未实现 |
| `/api/wrong-words/review` | `wrongWordApi.getReviewWords()` | 错词复习未实现 |
| `/api/tags` GET/POST | `tagApi.getList()/create()` | 标签管理未在 UI 中实现 |
| `/api/admin/words` POST | `adminApi.createWord()` | 后台新增单词未实现 |
| `/api/admin/words/{id}` PUT | `adminApi.updateWord()` | 后台编辑单词未实现 |

---

## 六、关键架构决策

### 6.1 请求流程

```
用户操作
   │
   ▼
View 组件 (如 Search.vue)
   │
   ├── 调用 Store 方法 (如 wordStore.search(params))
   │      │
   │      └── Store 调用 API 函数 (如 searchApi.search(params))
   │             │
   │             └── API 函数调用 api.get/post/put/delete()
   │                    │
   │                    └── http.js request()
   │                           │
   │                           ├── 拼接 /api 前缀
   │                           ├── 注入 Bearer Token
   │                           ├── fetch() 发送请求
   │                           └── 解包 ApiResponse.data 返回
   │
   └── 或直接调用 API 函数 (如 dashboardApi.get())
         在 mounted 或事件处理中
```

### 6.2 错误处理策略

| 错误类型 | 处理方式 | 所在层 |
|---|---|---|
| **401 Unauthorized** | 清除 Token，跳转 /login | `http.js request()` |
| **网络错误 / 后端异常** | 抛出 Error(body.message) | `http.js request()` |
| **数据加载失败** | catch 中设置空数组/默认值，显示"加载失败"提示 | 各 View / Store |
| **用户操作失败** | catch 中 alert 或 toast 提示错误消息 | Login.vue, WordBooks.vue, Profile.vue |

### 6.3 Token 生命周期

```
注册/登录成功
   → setToken(token) 写入 localStorage
   → fetchProfile() 获取用户信息
   → isLoggedIn = true

页面刷新
   → main.js 检查 localStorage 是否有 token
   → 有 → fetchProfile() 恢复会话
   → 无 → 不处理（导航守卫会跳转 /login）

API 请求
   → request() 从 localStorage 读 token
   → 注入 Authorization: Bearer xxx
   → 后端返回 401 → removeToken() → 跳转 /login

用户点击"退出"
   → removeToken()
   → user = null, isLoggedIn = false
   → router.push('/login')
```