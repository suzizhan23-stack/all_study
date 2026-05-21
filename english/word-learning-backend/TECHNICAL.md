# 英语学习平台 — 后端技术文档

> 版本 1.0.0 | Spring Boot 3.2.4 | Java 17 | MySQL 8.0

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈](#2-技术栈)
3. [项目结构](#3-项目结构)
4. [架构设计](#4-架构设计)
5. [数据库设计](#5-数据库设计)
6. [API 设计](#6-api-设计)
7. [认证与安全](#7-认证与安全)
8. [核心业务逻辑](#8-核心业务逻辑)
9. [配置说明](#9-配置说明)
10. [部署与运行](#10-部署与运行)

---

## 1. 项目概述

英语学习平台后端服务，提供单词学习、复习测验、阅读文章、收藏管理、学习计划、错题本、排行榜等完整功能的 RESTful API。

### 功能模块

| 模块 | 说明 |
|---|---|
| 用户认证 | 注册、登录、JWT 鉴权 |
| 首页 Dashboard | 今日进度、等级 XP、推荐学习项、快捷入口 |
| 词典搜索 | 联想搜索、全文搜索、搜索历史 |
| 单词详情 | 释义、搭配、介词模式、例句、词源、同反义词 |
| 学习卡片 | 每日计划、搭配/介词卡片展示 |
| 复习测验 | SM-2 间隔重复、选择题/拼写/听力多种题型 |
| 阅读文章 | 文章列表、阅读进度同步、文中查词 |
| 收藏管理 | 多收藏夹、跨实体收藏（词/例句/文章） |
| 错题本 | 按词/题型聚合、一键复习 |
| 学习计划 | 计划模板、单词本+策略生成每日计划 |
| 单词本 | 词性/字母筛选、多策略选词 |
| 个人中心 | 资料、设置、学习活动日历、徽章 |
| 排行榜 | 总榜/周榜/连续天数排行 |
| 管理后台 | 用户管理、词库 CRUD、批量导入、反馈审核 |

---

## 2. 技术栈

### 后端框架

| 组件 | 版本 | 说明 |
|---|---|---|
| Spring Boot | 3.2.4 | 应用框架 |
| Spring Data JPA | 3.2.4 | ORM + 数据访问 |
| Spring Security | 3.2.4 | 认证授权 |
| Spring Validation | 3.2.4 | 参数校验 |
| Spring Web | 3.2.4 | RESTful API |

### 数据库与连接

| 组件 | 版本 | 说明 |
|---|---|---|
| MySQL | 8.0+ | 生产数据库 |
| H2 | — | 测试/开发备选 |
| MySQL Connector | 8.x | JDBC 驱动 |

### 工具库

| 组件 | 版本 | 说明 |
|---|---|---|
| Lombok | 最新 | 减少样板代码 |
| jjwt (io.jsonwebtoken) | 0.12.5 | JWT 生成与验证 |
| BCrypt | Spring 内置 | 密码哈希 |

### 构建工具

| 组件 | 说明 |
|---|---|
| Maven | 项目构建 |
| spring-boot-maven-plugin | 打包为可执行 JAR |

---

## 3. 项目结构

```
word-learning-backend/
├── pom.xml
└── src/main/
    ├── java/com/wordlearning/
    │   ├── WordLearningApplication.java     # 启动入口
    │   ├── config/
    │   │   ├── CorsConfig.java              # 跨域配置
    │   │   ├── JwtAuthFilter.java           # JWT 请求过滤器
    │   │   └── SecurityConfig.java          # Spring Security 配置
    │   ├── util/
    │   │   └── JwtUtil.java                 # JWT 令牌工具
    │   ├── exception/
    │   │   ├── GlobalExceptionHandler.java  # 全局异常处理
    │   │   ├── ResourceNotFoundException.java
    │   │   └── BusinessException.java
    │   ├── entity/                          # 36 个 JPA 实体
    │   │   ├── User.java
    │   │   ├── Word.java
    │   │   ├── Article.java
    │   │   ├── Definition.java
    │   │   ├── Collocation.java
    │   │   ├── PrepPattern.java
    │   │   ├── Example.java
    │   │   └── ... (36 表全映射)
    │   ├── repository/                      # 36 个 Spring Data 接口
    │   ├── service/                         # 13 个业务服务
    │   │   ├── AuthService.java
    │   │   ├── DashboardService.java
    │   │   ├── SearchService.java
    │   │   ├── WordService.java
    │   │   ├── ReviewService.java
    │   │   ├── ArticleService.java
    │   │   ├── FavoriteService.java
    │   │   ├── WrongWordService.java
    │   │   ├── PlanService.java
    │   │   ├── WordBookService.java
    │   │   ├── UserService.java
    │   │   ├── LeaderboardService.java
    │   │   └── AdminService.java
    │   ├── controller/                      # 17 个 REST 控制器
    │   │   ├── AuthController.java
    │   │   ├── DashboardController.java
    │   │   ├── SearchController.java
    │   │   ├── WordController.java
    │   │   ├── ReviewController.java
    │   │   ├── ArticleController.java
    │   │   ├── FolderController.java
    │   │   ├── FavoriteController.java
    │   │   ├── WrongWordController.java
    │   │   ├── PlanController.java
    │   │   ├── WordBookController.java
    │   │   ├── TagController.java
    │   │   ├── StrategyController.java
    │   │   ├── UserController.java
    │   │   ├── LeaderboardController.java
    │   │   ├── BadgeController.java
    │   │   └── AdminController.java
    │   └── dto/
    │       ├── request/                     # 12 个请求 DTO
    │       │   ├── LoginRequest.java
    │       │   ├── RegisterRequest.java
    │       │   ├── ReviewResultRequest.java
    │       │   └── ...
    │       └── response/                    # 27 个响应 DTO
    │           ├── ApiResponse.java         # 统一响应封装
    │           ├── PageResponse.java        # 分页封装
    │           ├── DashboardResponse.java
    │           ├── WordDetailResponse.java
    │           └── ...
    └── resources/
        └── application.yml                 # 配置文件
```

### 依赖方向

```
Controller → Service → Repository → Entity
                  ↘
              DTO (Request/Response) ← → Controller
```

所有层通过 Spring DI 注入，无循环依赖。

---

## 4. 架构设计

### 4.1 分层架构

```
┌─────────────────────────────────────────┐
│             Controller 层               │
│  接收 HTTP 请求，参数校验，返回 JSON     │
├─────────────────────────────────────────┤
│              Service 层                  │
│  业务逻辑组织，事务管理，跨仓库协调       │
├─────────────────────────────────────────┤
│             Repository 层                │
│  数据访问，JPA 自动实现查询方法          │
├─────────────────────────────────────────┤
│              Entity 层                   │
│  数据库表映射，字段注解，自动时间戳       │
└─────────────────────────────────────────┘
```

### 4.2 请求处理流程

```
Client → CorsFilter → JwtAuthFilter → DispatcherServlet
                                          ↓
                                     Controller
                                          ↓
                                      Service
                                          ↓
                                    Repository → DB
                                          ↓
                ApiResponse.success(data) ← DTO
                                          ↓
                                     JSON Response
```

### 4.3 统一响应格式

所有 API 返回统一结构：

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

分页接口：

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

### 4.4 错误码体系

| code | 含义 | 触发场景 |
|---|---|---|
| 200 | 成功 | 正常响应 |
| 400 | 请求参数错误 | `@Valid` 校验失败、参数格式错误 |
| 401 | 未认证 | Token 缺失或过期 |
| 403 | 无权限 | 非管理员访问 `/api/admin` |
| 404 | 资源不存在 | 查询 ID 不存在 |
| 409 | 资源冲突 | 用户名/邮箱重复、计划已存在 |
| 500 | 服务器错误 | 未捕获异常 |

---

## 5. 数据库设计

### 5.1 总览

36 张表，全部使用 UUID 主键，统一 `created_at` / `updated_at` 审计字段。

### 5.2 核心表关系

```
users (1) ──→ user_stats (1)
    │
    ├──→ user_settings (N)
    ├──→ user_plans (N) ──→ learning_plans
    ├──→ favorite_folders (N) ──→ favorites (N) ──→ words / examples / articles
    ├──→ user_tags (N) ──→ user_entity_tags (N) ──→ entities
    ├──→ user_notes (N) ──→ entities
    ├──→ learning_activities (N)
    ├──→ review_log (N) ──→ words
    ├──→ search_history (N)
    ├──→ daily_recommendations (N)
    ├──→ user_daily_plan_entries (N) ──→ words
    ├──→ daily_plan_items (N) ──→ word_book_entries
    └──→ user_word_book_progress (N) ──→ word_books / study_strategies

words (1) ──→ definitions (N)
    ├──→ collocations (N)
    ├──→ prep_patterns (N)
    ├──→ examples (N)
    ├──→ word_relations (N) ──→ related_word
    ├──→ word_forms (N)
    ├──→ word_variants (N)
    ├──→ word_book_entries (N) ──→ word_books
    └──→ word_tags (N)

articles (1) ──→ reading_progress (N) ──→ users
    └──→ examples (N)

word_books (1) ──→ word_book_entries (N) ──→ words
    └──→ study_strategies (N)
```

### 5.3 关键设计决策

| 决策 | 说明 |
|---|---|
| UUID 主键 | 避免自增 ID 暴露规模，方便分库分表 |
| 复合主键 | `@IdClass` 用于 word_book_entries、user_badges 等多对多关联表 |
| 软删除 | `is_active` 字段用于 users、word_books 等 |
| JSON 字段 | badges.criteria、study_strategies.config 使用 JSON 存储灵活配置 |
| 枚举字符串 | 全部使用 `@Enumerated(EnumType.STRING)` 可读性强 |
| 索引策略 | 复合索引（stage+next_review 用于复习队列，first_letter+pos 用于单词本筛选） |

---

## 6. API 设计

### 6.1 接口风格

- RESTful 风格，资源路径使用名词复数
- 查询参数用于筛选/分页
- Path 参数用于资源 ID
- Body 用于 POST/PUT 请求数据
- 统一的 `ApiResponse<T>` 包裹

### 6.2 接口概览（按模块）

| 模块 | 路径 | 方法数 |
|---|---|---|
| Auth | `/api/auth/*` | 3 |
| Dashboard | `/api/dashboard/*` | 2 |
| Search | `/api/search/*` | 5 |
| Word | `/api/words/*`, `/api/tags/*` | 10 |
| Review | `/api/review/*` | 4 |
| Article | `/api/articles/*` | 5 |
| Folder/Favorite | `/api/folders/*`, `/api/favorites/*` | 14 |
| WrongWord | `/api/wrong-words/*` | 2 |
| Plan | `/api/plans/*` | 9 |
| WordBook | `/api/word-books/*`, `/api/strategies/*` | 10 |
| User | `/api/user/*` | 10 |
| Leaderboard/Badge | `/api/leaderboard`, `/api/badges` | 2 |
| Admin | `/api/admin/*` | 9 |

### 6.3 典型接口示例

**获取单词详情**

```
GET /api/words/{id}
Authorization: Bearer <token>
```

响应按语义分块：基本信息 → 发音 → 释义 → 搭配 → 介词模式 → 例句 → 同反义词 → 用户数据（进度/收藏/笔记/标签/评分）。

**提交答题结果**

```
POST /api/review/result
```

后端事务：答题日志 → SM-2 参数更新 → 每日活动汇总 → XP 更新 → 等级检查。

---

## 7. 认证与安全

### 7.1 JWT 认证流程

```
注册/登录 → 服务端验证 → 签发 JWT (含 userId, username, role)
                              ↓
客户端存储 Token → 后续请求携带 Authorization: Bearer <token>
                              ↓
JwtAuthFilter 解析 Token → 设置 SecurityContext → Controller 获取 userId
```

### 7.2 Token 格式

```json
// JWT Payload
{
  "sub": "userId (UUID)",
  "username": "demo",
  "role": "user",
  "iat": 1716000000,
  "exp": 1716086400
}
```

有效期 24 小时，HMAC-SHA256 签名。

### 7.3 密码安全

- BCrypt 哈希存储，每次哈希自动加盐
- 密码长度 6-128 字符，前端可加传输加密

### 7.4 权限控制

| 路径 | 权限 |
|---|---|
| `/api/auth/**` | 无需认证 |
| `/api/badges` (GET) | 无需认证 |
| `/api/admin/**` | `ROLE_ADMIN` |
| 其余所有 `/api/**` | 需有效 JWT |

---

## 8. 核心业务逻辑

### 8.1 SM-2 间隔重复算法

答题结果提交时执行：

```
correct:
  consecutive_correct++
  if consecutive_correct == 1: interval = 1
  if consecutive_correct == 2: interval = 6
  else: interval = round(interval * ease_factor)
  ease_factor += 0.1

incorrect:
  consecutive_correct = 0
  interval = 1
  ease_factor = max(1.3, ease_factor - 0.2)

stage = min(consecutive_correct, 7)
next_review = now + interval days
```

约束：`ease_factor` 范围 `[1.3, 3.0]`。

### 8.2 XP 计算规则

| 事件 | XP |
|---|---|
| 答对 | +10 |
| 答错 | +2 |
| 连续答对 >= 5 | +5 |
| 新词首次答对 | +15 |
| 响应时间 < 3s | +3 |
| 每日首轮复习 | +5 |
| 读完一篇文章 | +30 |

### 8.3 单词本策略选词

| 策略类型 | SQL 逻辑 |
|---|---|
| `random` | `ORDER BY RAND()` |
| `sequential` | `ORDER BY sort_order` |
| `difficulty_asc` | `ORDER BY w.difficulty, RAND()` |
| `difficulty_desc` | `ORDER BY w.difficulty DESC, RAND()` |
| `alphabetical` | `ORDER BY w.word` |

### 8.4 每日计划合并

每日学习词条来自两个来源：

1. **用户手动添加** — `user_daily_plan_entries` (不依赖单词本)
2. **系统生成** — `daily_plan_items` (按策略从单词本取词)

查询时 UNION 两个表，按 sort_order 排序。

---

## 9. 配置说明

### 9.1 `application.yml` 核心配置

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/word_learning?characterEncoding=utf8mb4
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: validate    # 生产用 validate，开发可改为 update
    show-sql: false

app:
  jwt:
    secret: <256-bit HMAC key>
    expiration-ms: 86400000  # 24h
```

### 9.2 等级配置（硬编码）

```
Lv.1: 0 XP    | Lv.6: 1200 XP
Lv.2: 100 XP  | Lv.7: 1700 XP
Lv.3: 250 XP  | Lv.8: 2300 XP
Lv.4: 500 XP  | Lv.9: 3000 XP
Lv.5: 800 XP  | Lv.10: 4000 XP
```

---

## 10. 部署与运行

### 10.1 环境要求

- JDK 17+
- MySQL 8.0+
- Maven 3.8+

### 10.2 运行步骤

```bash
# 1. 建库
mysql -u root -p < ../database/schema.sql

# 2. 修改配置 (application.yml 中数据源信息)

# 3. 编译
cd word-learning-backend
mvn clean package -DskipTests

# 4. 运行
java -jar target/word-learning-backend-1.0.0.jar

# 或用 Maven 直接启动
mvn spring-boot:run
```

### 10.3 验证

```bash
# 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"123456","email":"demo@test.com"}'

# 使用返回的 token 访问其他接口
curl http://localhost:8080/api/dashboard \
  -H "Authorization: Bearer <token>"
```

---

## 附录 A：文件统计

| 类别 | 文件数 |
|---|---|
| Java 源文件 | 149 |
| 配置文件 | 2 (pom.xml, application.yml) |
| **总计** | **151** |

---

## 附录 B：扩展指南

### 增加新功能

1. **新数据库表** → 在 `entity/` 创建实体，在 `repository/` 建接口
2. **新业务逻辑** → 在 `service/` 添加方法
3. **新 API** → 在 `controller/` 添加控制器（或扩展现有控制器）
4. **新 DTO** → 在 `dto/request/` 或 `dto/response/` 添加类

### 迁移到其他数据库

`application.yml` 中替换 `spring.datasource` 和 `spring.jpa.properties.hibernate.dialect`。

例如切换到 PostgreSQL：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/word_learning
    driver-class-name: org.postgresql.Driver
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

---

> **文档版本**: 1.0.0  
> **最后更新**: 2026-05-21  
> **代码行数**: ~12,000 Java
