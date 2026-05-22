# 英语学习平台 — 后端技术文档

> 版本 1.0.0 | Spring Boot 3.2.4 | Java 17 | MySQL 8.0  
> 总代码量 ~12,000 行 Java | 150 源文件 | 36 数据库表 | 60+ API 端点

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈详解](#2-技术栈详解)
3. [项目结构](#3-项目结构)
4. [请求-响应全流程可视化](#4-请求-响应全流程可视化)
5. [代码层逐层解读](#5-代码层逐层解读)
6. [数据库设计](#6-数据库设计)
7. [API 设计](#7-api-设计)
8. [认证与安全](#8-认证与安全)
9. [核心业务逻辑](#9-核心业务逻辑)
10. [配置说明](#10-配置说明)
11. [部署与运行](#11-部署与运行)
12. [附录](#12-附录)

---

## 1. 项目概述

英语学习平台后端服务，单词学习、复习测验、阅读文章、收藏管理、学习计划、错题本、排行榜等完整功能的 RESTful API。

### 功能模块矩阵

| 模块 | 核心价值 | 关键 API | 数据表依赖 |
|---|---|---|---|
| 用户认证 | 身份鉴权 | 3 | users, user_stats |
| Dashboard | 学习总览 | 2 | learning_activities, user_stats, daily_recommendations |
| 词典搜索 | 查词入口 | 5 | words, search_history |
| 单词详情 | 深度学词 | 10 | words + 10 张子表 |
| 学习卡片 | 每日学习 | 3 | user_daily_plan_entries, daily_plan_items |
| 复习测验 | 间隔重复 | 4 | review_log, words (SM-2) |
| 阅读文章 | 阅读学词 | 5 | articles, reading_progress |
| 收藏管理 | 知识沉淀 | 14 | favorite_folders, favorites |
| 错题本 | 薄弱攻克 | 2 | review_log |
| 学习计划 | 系统学习 | 9 | learning_plans, user_plans |
| 单词本 | 词库浏览 | 10 | word_books, word_book_entries |
| 个人中心 | 数据统计 | 10 | users, user_stats, user_settings |
| 排行榜 | 社交激励 | 2 | user_stats, learning_activities |
| 管理后台 | 运营管理 | 9 | users, words, content_ratings |

---

## 2. 技术栈详解

### 2.1 核心框架

#### Spring Boot 3.2.4

选择 Spring Boot 而非传统 Spring 的核心理由：

- **自动配置**：`spring-boot-starter-*` 一键引入完整功能栈，无需手写 XML
- **嵌入式容器**：内嵌 Tomcat，`java -jar` 即可运行，无需部署 WAR
- **Actuator**（可插拔）：可随时开启 `/actuator/health` 等生产监控端点
- **生态成熟**：与 JPA、Security、Validation 无缝集成

关键注解：

```java
@SpringBootApplication  // = @Configuration + @EnableAutoConfiguration + @ComponentScan
public class WordLearningApplication {
    public static void main(String[] args) {
        SpringApplication.run(WordLearningApplication.class, args);
    }
}
```

#### Spring Data JPA

采用 JPA 而非 MyBatis 的决策依据：

| 对比维度 | JPA (Hibernate) | MyBatis |
|---|---|---|
| 开发效率 | 高——无需写 SQL，方法名即查询 | 低——每接口配 XML |
| 关联查询 | `@Entity` 映射自动 JOIN | 手写 SQL JOIN |
| 动态查询 | `Specification` / `@Query` | `<if>` 标签 |
| 学习成本 | 中 | 低 |
| 细粒度 SQL 控制 | 弱 | 强 |

本项目实践：标准 CRUD 全用方法命名约定，复杂聚合用 `@Query` JPQL。

```java
// 方法命名自动实现查询（无需写 SQL）
public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    // uuid 查找
    Optional<ReviewLog> findByUuid(String uuid);

    // 方法命名自动实现查询（无需写 SQL）
    List<ReviewLog> findByUserIdAndWordIdAndIsCorrectFalseOrderByReviewedAtDesc(
        Long userId, Long wordId);

    // 自定义 JPQL 复杂聚合
    @Query("SELECT rl.wordId, COUNT(rl) AS cnt FROM ReviewLog rl " +
           "WHERE rl.userId = :uid AND rl.isCorrect = false " +
           "GROUP BY rl.wordId ORDER BY cnt DESC")
    List<Object[]> countWrongWordsByUser(@Param("uid") Long userId, Pageable p);
}
```

#### Spring Security

三层安全模型：

```
SecurityFilterChain  →  定义 URL 权限矩阵
        ↓
JwtAuthFilter        →  每个请求拦截，解析 Token
        ↓
SecurityContextHolder →  线程级安全上下文，Controller 直接读取 userId
```

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())  // JWT 无状态，CSRF 无关
        .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()     // 登录注册开放
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

#### Spring Validation

```java
@PostMapping("/register")
public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
    // @Valid 触发校验，校验失败由 GlobalExceptionHandler 处理
}

// DTO 中的校验注解
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 20, message = "username 3-20 chars")
    private String username;
    @NotBlank @Size(min = 6, max = 128)
    private String password;
    @Email @NotBlank
    private String email;
}
```

### 2.2 认证与加密

| 组件 | 用途 | 实现细节 |
|---|---|---|
| **jjwt 0.12.5** | JWT 签发与验证 | HMAC-SHA256，24h 过期 |
| **BCryptPasswordEncoder** | 密码哈希 | 自动加盐，每次哈希结果不同 |

```java
// JwtUtil.java — 对称签名密钥，HMAC-SHA256
public String generateToken(String userUuid, String username, String role) {
    return Jwts.builder()
        .subject(userUuid)                       // 存入 user.uuid 而非 INT id
        .claim("username", username)
        .claim("role", role)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expirationMs))
        .signWith(key)                           // SecretKey (256-bit)
        .compact();
}
```

### 2.3 构建与依赖管理

**Maven 依赖树（核心）**：

```
spring-boot-starter-web
  ├── spring-webmvc (DispatcherServlet, @RestController)
  ├── tomcat-embed-core (嵌入式容器)
  └── jackson-databind (JSON 序列化)
  
spring-boot-starter-data-jpa
  ├── spring-orm (Hibernate JPA 实现)
  ├── spring-data-jpa (Repository 抽象)
  └── hikariCP (连接池)

spring-boot-starter-security
  └── spring-security-web + spring-security-config

jjwt-api + jjwt-impl + jjwt-jackson (0.12.5)

mysql-connector-j (8.x)
lombok
```

### 2.4 关键依赖决策

| 依赖 | 替代方案 | 选择理由 |
|---|---|---|
| Lombok | 手写 getter/setter | 减少 60% 样板代码，3 行代替 30 行 |
| jjwt | auth0-java-jwt, nimbus-jose | API 简洁，Spring 社区主流选择 |
| BCrypt | scrypt, argon2 | Spring Security 内置，足够安全 |
| HikariCP | Druid, DBCP2 | Spring Boot 默认，性能最优 |

---

## 3. 项目结构

### 3.1 完整目录树（150 源文件）

```
word-learning-backend/
├── pom.xml                                              # Maven 构建 (Spring Boot 3.2.4)
│
└── src/main/
    ├── java/com/wordlearning/
    │   │
    │   ├── WordLearningApplication.java                 # @SpringBootApplication 启动入口
    │   │
    │   ├── config/                                      # ── 框架配置层 ──
    │   │   ├── CorsConfig.java                          #   跨域 CORS 配置
    │   │   ├── JwtAuthFilter.java                       #   JWT 请求拦截过滤器
    │   │   └── SecurityConfig.java                      #   Spring Security 策略
    │   │
    │   ├── util/                                        # ── 工具层 ──
    │   │   └── JwtUtil.java                             #   JWT 令牌生成/验证
    │   │
    │   ├── exception/                                   # ── 异常层 ──
    │   │   ├── GlobalExceptionHandler.java              #   @RestControllerAdvice
    │   │   ├── ResourceNotFoundException.java           #   404 异常
    │   │   └── BusinessException.java                   #   业务异常 (含状态码)
    │   │
    │   ├── entity/                                      # ── 数据层 (36 个 JPA 实体) ──
    │   │   ├── User.java                                #   用户表
    │   │   ├── Word.java                                #   单词主表 (含 SM-2 字段)
    │   │   ├── Article.java                             #   文章
    │   │   ├── Definition.java                          #   释义
    │   │   ├── Collocation.java                         #   固定搭配
    │   │   ├── PrepPattern.java                         #   介词模式
    │   │   ├── Example.java                             #   例句
    │   │   ├── WordRelation.java                        #   同反义词关系
    │   │   ├── WordTag.java                             #   单词标签
    │   │   ├── WordForm.java                            #   单词变形
    │   │   ├── WordVariant.java                         #   拼写变体
    │   │   ├── UsageNote.java                           #   用法说明
    │   │   ├── UserFrequency.java                       #   用户自定义词频
    │   │   ├── FavoriteFolder.java                      #   收藏夹
    │   │   ├── Favorite.java                            #   收藏条目
    │   │   ├── UserSetting.java                         #   用户设置
    │   │   ├── UserStat.java                            #   用户统计
    │   │   ├── UserTag.java                             #   用户自定义标签
    │   │   ├── UserNote.java                            #   用户笔记
    │   │   ├── UserEntityTag.java                       #   标签-实体关联
    │   │   ├── LearningActivity.java                    #   学习活动日志
    │   │   ├── ReviewLog.java                           #   答题日志
    │   │   ├── SearchHistory.java                       #   搜索历史
    │   │   ├── ContentRating.java                       #   内容评分
    │   │   ├── Badge.java                               #   徽章定义
    │   │   ├── UserBadge.java                           #   用户徽章
    │   │   ├── ReadingProgress.java                     #   阅读进度
    │   │   ├── DailyRecommendation.java                 #   每日推荐
    │   │   ├── LearningPlan.java                        #   学习计划模板
    │   │   ├── UserPlan.java                            #   用户学习计划
    │   │   ├── WordBook.java                            #   单词本
    │   │   ├── WordBookEntry.java                       #   单词本词条
    │   │   ├── StudyStrategy.java                       #   学习策略
    │   │   ├── UserWordBookProgress.java                #   单词本学习进度
    │   │   ├── DailyPlanItem.java                       #   每日计划(系统生成)
    │   │   └── UserDailyPlanEntry.java                  #   每日计划(手动添加)
    │   │
    │   ├── repository/                                  # ── 数据访问层 (36 接口) ──
    │   │   ├── UserRepository.java                      #   findByUsername, findByEmail
    │   │   ├── WordRepository.java                      #   findByWord, findByStageAndNextReview
    │   │   ├── ReviewLogRepository.java                 #   @Query 错题聚合统计
    │   │   └── ...                                      #   每个 entity 对应一个 repository
    │   │
    │   ├── service/                                     # ── 业务逻辑层 (13 服务) ──
    │   │   ├── AuthService.java                         #   注册/登录/JWT
    │   │   ├── DashboardService.java                    #   首页聚合
    │   │   ├── SearchService.java                       #   搜索/联想/历史
    │   │   ├── WordService.java                         #   单词详情/笔记/标签/评分
    │   │   ├── ReviewService.java                       #   复习队列/SM-2/答题
    │   │   ├── ArticleService.java                      #   文章阅读/进度
    │   │   ├── FavoriteService.java                     #   收藏夹/收藏 CRUD
    │   │   ├── WrongWordService.java                    #   错题统计/一键复习
    │   │   ├── PlanService.java                         #   学习计划/每日生成
    │   │   ├── WordBookService.java                     #   单词本/策略
    │   │   ├── UserService.java                         #   资料/设置/徽章
    │   │   ├── LeaderboardService.java                  #   排行榜/徽章列表
    │   │   └── AdminService.java                        #   后台管理/词库导入
    │   │
    │   ├── controller/                                  # ── API 接口层 (17 控制器) ──
    │   │   ├── AuthController.java                      #   POST /api/auth/{register,login,logout}
    │   │   ├── DashboardController.java                 #   GET /api/dashboard
    │   │   ├── SearchController.java                    #   GET /api/search/**
    │   │   ├── WordController.java                      #   GET/PUT /api/words/{id}
    │   │   ├── TagController.java                       #   GET/POST /api/tags
    │   │   ├── ReviewController.java                    #   GET/POST /api/review/**
    │   │   ├── ArticleController.java                   #   GET/PUT /api/articles/**
    │   │   ├── FolderController.java                    #   CRUD /api/folders
    │   │   ├── FavoriteController.java                  #   POST/DEL /api/favorites
    │   │   ├── WrongWordController.java                 #   GET /api/wrong-words
    │   │   ├── PlanController.java                      #   /api/plans/**
    │   │   ├── WordBookController.java                  #   /api/word-books/**
    │   │   ├── StrategyController.java                  #   GET /api/strategies
    │   │   ├── UserController.java                      #   /api/user/**
    │   │   ├── LeaderboardController.java               #   GET /api/leaderboard
    │   │   ├── BadgeController.java                     #   GET /api/badges
    │   │   └── AdminController.java                     #   /api/admin/**
    │   │
    │   └── dto/                                         # ── 数据传输对象 ──
    │       ├── request/                                 #   12 个请求体 DTO
    │       │   ├── LoginRequest.java
    │       │   ├── RegisterRequest.java
    │       │   ├── ReviewResultRequest.java
    │       │   └── ... (12 files)
    │       └── response/                                #   27 个响应体 DTO
    │           ├── ApiResponse.java                     #   统一响应封装 {code,message,data}
    │           ├── PageResponse.java                    #   分页响应封装
    │           ├── DashboardResponse.java               #   首页聚合
    │           ├── WordDetailResponse.java              #   单词详情 (含 11 个内部类)
    │           └── ... (27 files)
    │
    └── resources/
        └── application.yml                             # 数据源/JWT/Server 配置
```

### 3.2 各层命名规范

| 层 | 命名后缀 | 职责 | 举例 |
|---|---|---|---|
| Entity | 名词单数 | DB 表映射 | `Word.java` → `words` 表 |
| Repository | `Repository` | 数据访问 | `WordRepository.java` |
| Service | `Service` | 业务逻辑 | `WordService.java` |
| Controller | `Controller` | HTTP 接口 | `WordController.java` |
| Request | `Request` | 请求参数 | `ReviewResultRequest.java` |
| Response | `Response` | 响应数据 | `DashboardResponse.java` |

### 3.3 依赖方向与注入

```
Controller  ( @RestController )
    │  @RequiredArgsConstructor (构造器注入)
    ▼
Service     ( @Service, @Transactional )
    │  @RequiredArgsConstructor
    ▼
Repository  ( extends JpaRepository )
    │
    ▼
Entity      ( @Entity, @Table )
    │
    ▼
MySQL       ( information_schema )

DTO 流向:
  Request DTO  → Controller ← Service → Response DTO
       (@Valid)     │                       │
                    ▼                       ▼
              参数校验后传给 Service    ApiResponse.success() 包装返回
```

**无循环依赖保证**：Controller 仅依赖 Service，Service 仅依赖 Repository，Repository 仅依赖 Entity。反向绝无引用。

---

## 4. 请求-响应全流程可视化

### 4.1 一次完整请求的生命周期

以 `POST /api/review/result`（提交答题结果）为例，展示完整的 12 步流程：

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  Client (浏览器/移动端)                                                         │
│  curl -X POST http://localhost:8080/api/review/result \                        │
│    -H "Authorization: Bearer eyJhbGci..." \                                   │
│    -d '{"wordId":"xxx","quizType":"meaning","isCorrect":true,"responseTimeMs":3200}' │
└─────────────────────┬───────────────────────────────────────────────────────────┘
                      │ HTTP 请求
                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  STEP 1: Tomcat 接收请求                                                        │
│  HttpServletRequest → HttpServletResponse                                      │
│  默认 8080 端口，最大连接数 200 (tomcat.threads.max)                           │
└─────────────────────┬───────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  STEP 2: CorsFilter (com.wordlearning.config.CorsConfig)                       │
│  ┌─ 检查 Origin 头部                                                            │
│  ├─ 添加 CORS 响应头:                                                           │
│  │    Access-Control-Allow-Origin: *                                           │
│  │    Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS                │
│  │    Access-Control-Allow-Headers: *                                          │
│  │    Access-Control-Allow-Credentials: true                                   │
│  ├─ OPTIONS 预检请求直接返回 200                                                │
│  └─ 非 OPTIONS 放行到下一过滤器                                                  │
└─────────────────────┬───────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  STEP 3: JwtAuthFilter (com.wordlearning.config.JwtAuthFilter)                 │
│  ┌─ 从 Header 提取: "Bearer eyJhbGci..."                                       │
│  ├─ jwtUtil.validateToken(token):                                              │
│  │    1. Jwts.parser().verifyWith(key).build().parseSignedClaims(token)        │
│  │    2. 检查签名 HMAC-SHA256 是否匹配                                          │
│  │    3. 检查是否过期 (exp < now?)                                             │
│  │    4. 成功 → 获取 Claims {sub, username, role}                              │
│  ├─ 创建 UsernamePasswordAuthenticationToken(userId, null, [ROLE_USER])        │
│  ├─ SecurityContextHolder.getContext().setAuthentication(auth)                 │
│  └─ 放行到 DispatcherServlet                                                    │
└─────────────────────┬───────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  STEP 4: DispatcherServlet (Spring MVC 前端控制器)                              │
│  ┌─ 根据请求路径 /api/review/result 查找 HandlerMapping                         │
│  ├─ 匹配到 ReviewController.submitResult() 方法                                 │
│  │   @PostMapping("/result")                                                   │
│  │   public ApiResponse<ReviewResultResponse> submitResult(                    │
│  │       @Valid @RequestBody ReviewResultRequest req)                          │
│  ├─ 参数解析:                                                                   │
│  │    @RequestBody → Jackson 反序列化 JSON → ReviewResultRequest 对象            │
│  │    @Valid → 触发 Bean Validation 校验                                        │
│  ├─ 前置拦截: GlobalExceptionHandler 已注册                                     │
│  └─ 调用 Controller 方法                                                        │
└─────────────────────┬───────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  STEP 5: Controller 层 (ReviewController.java)                                 │
│                                                                                │
│  @RequiredArgsConstructor 注入 ReviewService                                   │
│                                                                                │
│  String userId = (String) SecurityContextHolder                               │
│      .getContext().getAuthentication().getPrincipal();                         │
│  // userId = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"                         │
│                                                                                │
│  ReviewResultResponse result = reviewService.submitResult(userId, req);       │
│  return ApiResponse.success(result);                                          │
│                                                                                │
│  Controller 职责:                                                              │
│  ├─ 提取认证用户 ID                                                             │
│  ├─ 调用 Service 执行业务逻辑                                                    │
│  └─ 用 ApiResponse.success() 包装返回                                           │
└─────────────────────┬───────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  STEP 6: Service 层 — 事务开始 (ReviewService.java)                            │
│                                                                                │
│  @Transactional                                                                │
│  public ReviewResultResponse submitResult(String userId, ReviewResultRequest r)│
│                                                                                │
│  1. 查找单词:                                                                   │
│     // 通过 uuid 查找（API 传参为 UUID 字符串）                                 │
│     Word word = wordRepository.findByUuid(r.getWordId())                     │
│                 .orElseThrow(() -> new ResourceNotFoundException("Word"));     │
│     Long wordId = word.getId();              // 获取 INT PK 用于 JOIN          │
│                                                                                │
│  2. 创建答题日志:                                                               │
│     ReviewLog log = ReviewLog.builder()                                       │
│         .uuid(UUID.randomUUID().toString())   // 业务标识                      │
│         .userId(userId)                                                        │
│         .wordId(wordId)                       // 使用 INT FK                  │
│         .quizType(QuizType.valueOf(r.getQuizType()))                           │
│         .isCorrect(r.getIsCorrect())                                           │
│         .responseTimeMs(r.getResponseTimeMs())                                │
│         .wrongAnswer(r.getWrongAnswer())                                      │
│         .reviewedAt(LocalDateTime.now())                                      │
│         .build();                                                              │
│     reviewLogRepository.save(log);                                             │
│                                                                                │
│  3. SM-2 算法:                                                                 │
│     int consecutive = word.getConsecutiveCorrect();                           │
│     if (r.getIsCorrect()) {                                                   │
│         consecutive++;                                                         │
│         if (consecutive == 1) interval = 1;                                   │
│         else if (consecutive == 2) interval = 6;                              │
│         else interval = Math.round(interval * easeFactor);                    │
│         easeFactor = Math.min(3.0, easeFactor + 0.1);                         │
│     } else {                                                                   │
│         consecutive = 0;                                                       │
│         interval = 1;                                                          │
│         easeFactor = Math.max(1.3, easeFactor - 0.2);                         │
│     }                                                                          │
│                                                                                │
│  4. 更新 Word 实体的 SM-2 参数:                                                │
│     word.setConsecutiveCorrect(consecutive);                                  │
│     word.setEaseFactor(BigDecimal.valueOf(easeFactor));                       │
│     word.setIntervalDays(interval);                                            │
│     word.setNextReview(LocalDateTime.now().plusDays(interval));               │
│     word.setReviewCount(word.getReviewCount() + 1);                           │
│     word.setStage(Math.min(consecutive, 7));                                  │
│     wordRepository.save(word);                                                 │
│                                                                                │
│  5. 更新每日活动:                                                               │
│     learningActivityRepository.findByUserIdAndActivityDate(userId, today)     │
│     // 存在则累加，不存在则创建                                                 │
│                                                                                │
│  6. 更新 XP:                                                                   │
│     userStat.setXp(userStat.getXp() + xpGained);                               │
│     userStat.setTotalReviews(userStat.getTotalReviews() + 1);                  │
│     // 检查是否升级                                                             │
│                                                                                │
│  7. 返回结果:                                                                   │
│     return ReviewResultResponse.builder()                                     │
│         .xpGained(xpGained)                                                   │
│         .stage(word.getStage())                                               │
│         .nextReview(word.getNextReview().toString())                           │
│         .build();                                                              │
│                                                                                │
└─────────────────────┬───────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  STEP 7: Repository 层 (Spring Data JPA)                                       │
│                                                                                │
│  reviewLogRepository.save(log) → EntityManager.persist()                      │
│  wordRepository.save(word)    → EntityManager.merge()                          │
│                                                                                │
│  Spring Data JPA 自动实现:                                                     │
│  ├─ SimpleJpaRepository.save() → 判断是 persist 还是 merge (根据 ID 是否存在)    │
│  ├─ Hibernate 生成 INSERT/UPDATE SQL                                           │
│  └─ HikariCP 从连接池获取 JDBC 连接                                             │
│                                                                                │
│  生成的 SQL 示意:                                                               │
│  INSERT INTO review_log (uuid, user_id, word_id, quiz_type, is_correct,        │
│      response_time_ms, wrong_answer, reviewed_at)                              │
│  VALUES (?, ?, ?, ?, ?, ?, ?, ?);   -- id 自增, uuid 由 Java 生成               │
│                                                                                │
│  UPDATE words SET consecutive_correct = ?, ease_factor = ?, interval_days = ?, │
│      next_review = ?, review_count = ?, stage = ?, last_reviewed_at = NOW()    │
│  WHERE id = ?;                      -- 内部使用 INT PK                        │
│                                                                                │
└─────────────────────┬───────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  STEP 8: Entity 层 (JPA 生命周期回调)                                          │
│                                                                                │
│  @PrePersist / @PreUpdate 自动触发:                                            │
│  Word.java:                                                                    │
│    @PrePersist                                                                 │
│    protected void onCreate() {                                                 │
│        createdAt = LocalDateTime.now();                                        │
│        updatedAt = LocalDateTime.now();                                        │
│    }                                                                           │
│    @PreUpdate                                                                  │
│    protected void onUpdate() {                                                 │
│        updatedAt = LocalDateTime.now();                                        │
│    }                                                                           │
│                                                                                │
│  ReviewLog.java:                                                               │
│    @PrePersist                                                                 │
│    protected void onCreate() {                                                 │
│        if (reviewedAt == null) reviewedAt = LocalDateTime.now();               │
│    }                                                                           │
│                                                                                │
│  自动填充审计字段，业务代码无需手动管理时间戳。                                   │
│                                                                                │
└─────────────────────┬───────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  STEP 9: MySQL 执行 SQL                                                        │
│                                                                                │
│  Connection 来自 HikariCP 连接池 (默认 10 个连接)                               │
│                                                                                │
│  AUTOCOMMIT = false (由 @Transactional 管理)                                    │
│  ┌─ INSERT INTO review_log ...                                                │
│  ├─ UPDATE words SET ...                                                       │
│  ├─ INSERT INTO learning_activities ... ON DUPLICATE KEY UPDATE               │
│  ├─ UPDATE user_stats SET ...                                                  │
│  └─ COMMIT (所有操作在同一事务中)                                               │
│                                                                                │
│  事务特性:                                                                     │
│  ├─ 原子性: 任何一个步骤失败 → ROLLBACK                                        │
│  ├─ 一致性: 数据前后一致                                                       │
│  ├─ 隔离性: READ_COMMITTED (MySQL 默认)                                        │
│  └─ 持久性: COMMIT 后数据落盘                                                   │
│                                                                                │
└─────────────────────┬───────────────────────────────────────────────────────────┘
                      │ 结果集
                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  STEP 10: 响应序列化 (反向路径)                                                 │
│                                                                                │
│  Service 返回 ReviewResultResponse DTO                                         │
│      ↓                                                                        │
│  Controller 包装: ApiResponse.success(result)                                  │
│      ↓                                                                        │
│  Jackson ObjectMapper 序列化为 JSON:                                           │
│  {                                                                             │
│    "code": 200,                                                                │
│    "message": "success",                                                       │
│    "data": {                                                                   │
│      "xpGained": 10,                                                           │
│      "stage": 3,                                                               │
│      "nextReview": "2026-06-01T10:00:00"                                       │
│    }                                                                           │
│  }                                                                             │
│      ↓                                                                        │
│  HttpResponse 写入 OutputStream                                                │
│      ↓                                                                        │
│  Tomcat 发送响应体 + 状态码 200                                                │
│                                                                                │
└─────────────────────┬───────────────────────────────────────────────────────────┘
                      │ HTTP 响应
                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  STEP 11: 异常路径 (如果出错)                                                   │
│                                                                                │
│  场景 A: 单词不存在                                                             │
│  Service 抛出 ResourceNotFoundException("Word not found with id: xxx")        │
│      ↓                                                                        │
│  GlobalExceptionHandler.handleNotFound():                                      │
│  return ResponseEntity.status(404)                                             │
│      .body(ApiResponse.notFound("Word not found with id: xxx"));              │
│      ↓                                                                        │
│  Response: {"code": 404, "message": "Word not found with id: xxx", "data": null}│
│                                                                                │
│  场景 B: 参数校验失败                                                           │
│  @Valid 触发 MethodArgumentNotValidException                                   │
│      ↓                                                                        │
│  GlobalExceptionHandler.handleValidation():                                    │
│  return ResponseEntity.status(400)                                             │
│      .body(ApiResponse.badRequest("wordId: must not be blank"));              │
│      ↓                                                                        │
│  Response: {"code": 400, "message": "wordId must not be blank", "data": null}  │
│                                                                                │
│  场景 C: 未捕获异常                                                             │
│  Exception → GlobalExceptionHandler.handleGeneric():                           │
│  return ResponseEntity.status(500)                                             │
│      .body(ApiResponse.internalError(e.getMessage()));                        │
│      ↓                                                                        │
│  Response: {"code": 500, "message": "NullPointerException: ...", "data": null}│
│                                                                                │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 时序图（简化）

```
Client          Tomcat         CORS        JWT Auth     Dispatcher   Controller   Service     Repository   MySQL
  │               │             │            │             │            │           │            │         │
  │──POST /review─►             │            │             │            │           │            │         │
  │               │──► CorsFilter ──► JwtFilter ──► Dispatch ──► ReviewController  │         │         │
  │               │             │            │             │            │           │            │         │
  │               │             │            │             │            │──submit─► │           │         │
  │               │             │            │             │            │           │──findById─►│         │
  │               │             │            │             │            │           │◄─word──────│         │
  │               │             │            │             │            │           │           │         │
  │               │             │            │             │            │           │──save(log)─►│──INSERT►│
  │               │             │            │             │            │           │            │         │
  │               │             │            │             │            │           │──save(word)─►──UPDATE►│
  │               │             │            │             │            │           │            │         │
  │               │             │            │             │            │           │──upsert(act)─►──INSERT►│
  │               │             │            │             │            │           │──update(xp)─►──UPDATE►│
  │               │             │            │             │            │           │            │         │
  │               │             │            │             │            │◄─result───│            │         │
  │               │             │            │             │◄─JSON──────│           │            │         │
  │◄──200 JSON─────             │            │             │            │           │            │         │
```

---

## 5. 代码层逐层解读

### 5.1 Entity 层 — 数据库映射

每个实体类映射一张数据库表。以 `Word.java` 为例：

```java
@Entity                     // 标记为 JPA 实体
@Table(name = "words")      // 映射 words 表
@Data                       // Lombok: 自动生成 getter/setter/toString/equals/hashCode
@NoArgsConstructor           // JPA 需要无参构造器
@AllArgsConstructor          // Builder 模式需要全参构造器
@Builder                    // 支持链式构造: Word.builder().id(...).word(...).build()
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // INT AUTO_INCREMENT 主键

    @Column(length = 36, nullable = false, unique = true)
    private String uuid;                // UUID 业务标识（API 暴露）

    @Column(nullable = false, length = 50)
    private String word;                // 单词原文

    @Column(nullable = false, length = 30)
    private String pos;                 // 词性 (vt./n./adj. 等)

    @Column(name = "first_letter", nullable = false, length = 1)
    private String firstLetter;         // 首字母，用于字母筛选

    @Column(name = "phonetic_uk", length = 100)
    private String phoneticUk;          // 英式音标

    @Column(name = "meaning_cn", length = 500)
    private String meaningCn;           // 中文释义

    @Column(nullable = false)
    private int stage;                  // SM-2 学习阶段 0-3

    @Column(nullable = false)
    private int consecutiveCorrect;     // SM-2 连续答对数

    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeFactor;      // SM-2 难度系数 (1.30-3.00)

    @Column(name = "next_review")
    private LocalDateTime nextReview;   // SM-2 下次复习时间

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── 生命周期回调 ──
    @PrePersist                     // INSERT 前自动执行
    protected void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate                      // UPDATE 前自动执行
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

**取消复合主键示例**（`WordBookEntry.java`，改为 INT PK + UNIQUE）：

```java
@Entity
@Table(name = "word_book_entries", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"word_book_id", "word_id"})
})
public class WordBookEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // INT AUTO_INCREMENT PK

    @Column(length = 36, nullable = false, unique = true)
    private String uuid;                // UUID 业务标识

    @Column(name = "word_book_id", nullable = false)
    private Long wordBookId;            // INT FK → word_books.id

    @Column(name = "word_id", nullable = false)
    private Long wordId;                // INT FK → words.id

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    // ...

    @PrePersist
    protected void onCreate() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
    }
}
// 原 @IdClass(WordBookEntryId.class) 已移除
// 复合唯一由 UNIQUE(word_book_id, word_id) 约束保证
```

**枚举持久化**（`User.java`）：

```java
@Column(nullable = false, length = 20)
@Enumerated(EnumType.STRING)    // 存储字符串值而非 ordinal
private Role role;

public enum Role {
    admin, editor, user
    // 数据库存 "admin" 而非 0
}
```

### 5.2 Repository 层 — 数据访问

Spring Data JPA 根据方法名自动生成查询实现。

```java
public interface WordRepository extends JpaRepository<Word, Long> {

    // ── UUID 查找（API 入口） ──

    // select * from words where uuid = ?
    Optional<Word> findByUuid(String uuid);

    // ── 自动实现 (方法名解析) ──

    // select * from words where word = ?
    Optional<Word> findByWord(String word);

    // select * from words where word like ? order by frequency desc
    List<Word> findByWordStartingWith(String prefix, Pageable p);

    // select * from words where pos = ? and id != ? order by rand() limit ?
    List<Word> findByPosAndIdNot(String pos, Long id, Pageable p);

    // ── 自定义 JPQL 查询 ──

    @Query("SELECT w FROM Word w WHERE " +
           "(w.nextReview IS NULL OR w.nextReview <= :now) " +
           "AND (:source IS NULL OR w.source = :source) " +
           "ORDER BY w.stage ASC, w.nextReview ASC")
    List<Word> findReviewQueue(@Param("now") LocalDateTime now,
                               @Param("source") String source,
                               Pageable p);

    // ── 聚合查询 ──

    @Query("SELECT COUNT(w) FROM Word w WHERE w.id NOT IN " +
           "(SELECT usr.wordId FROM UserSpacedRepetition usr WHERE usr.userId = :uid)")
    long countNewWords(@Param("uid") String userId);
}
```

**`ReviewLogRepository` — 复杂聚合**：

```java
public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    // 按词统计错题数
    @Query("SELECT rl.wordId AS wordId, w.word AS word, w.meaningCn AS meaningCn, " +
           "COUNT(rl) AS wrongCount, MAX(rl.reviewedAt) AS lastWrong " +
           "FROM ReviewLog rl JOIN Word w ON rl.wordId = w.id " +
           "WHERE rl.userId = :uid AND rl.isCorrect = false " +
           "AND (:days = 0 OR rl.reviewedAt >= :since) " +
           "AND (:quizType IS NULL OR rl.quizType = :quizType) " +
           "GROUP BY rl.wordId, w.word, w.meaningCn " +
           "ORDER BY wrongCount DESC, lastWrong DESC")
    List<Object[]> findWrongWordsGrouped(@Param("uid") String userId,
                                         @Param("days") int days,
                                         @Param("since") LocalDateTime since,
                                         @Param("quizType") String quizType,
                                         Pageable p);

    // 按题型统计错误分布
    @Query("SELECT rl.quizType AS type, COUNT(rl) AS cnt " +
           "FROM ReviewLog rl WHERE rl.userId = :uid " +
           "AND rl.isCorrect = false AND rl.reviewedAt >= :since " +
           "GROUP BY rl.quizType ORDER BY cnt DESC")
    List<Object[]> countByQuizType(@Param("uid") String userId,
                                   @Param("since") LocalDateTime since);
}
```

### 5.3 Service 层 — 业务逻辑

**`ReviewService.submitResult()` — 核心业务方法（完整代码解读）**：

```java
@Service
@RequiredArgsConstructor          // 构造器注入（final 字段）
@Transactional                   // 所有 public 方法在事务中执行
public class ReviewService {

    private final WordRepository wordRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final LearningActivityRepository learningActivityRepository;
    private final UserStatRepository userStatRepository;

    /**
     * 提交答题结果。
     *
     * 事务包括：
     * 1. 记录答题日志
     * 2. 执行 SM-2 间隔重复算法
     * 3. 更新单词的 SM-2 参数
     * 4. 更新每日学习活动（累加或创建）
     * 5. 更新用户 XP 和统计
     * 6. 检查是否升级
     *
     * @param userId 当前用户 UUID（从 JWT 获取，内部按 INT FK 关联）
     * @param req    请求体（wordId UUID, quizType, isCorrect, responseTimeMs, wrongAnswer）
     * @return 返回获得的 XP、新阶段、下次复习时间
     */
    public ReviewResultResponse submitResult(String userId, ReviewResultRequest req) {
        // ──── 1. 查找单词 ────
        // req.getWordId() 是 UUID 字符串，通过 uuid 列查找实体，获得 INT PK
        Word word = wordRepository.findByUuid(req.getWordId())
                .orElseThrow(() -> new ResourceNotFoundException("Word", req.getWordId()));
        Long wordId = word.getId();  // 获取 INT PK 用于后续 JOIN

        boolean wasNewWord = word.getReviewCount() == 0;  // 用于判断是否为新词首学
        boolean isCorrect = req.getIsCorrect();
        int prevConsecutive = word.getConsecutiveCorrect();
        double easeFactor = word.getEaseFactor().doubleValue();
        int interval = word.getIntervalDays();

        // ──── 2. 记录答题日志 ────
        ReviewLog log = ReviewLog.builder()
                .uuid(UUID.randomUUID().toString())  // uuid 由 Java 生成
                .userId(userId)                      // userId 是用户 UUID（外部），内部已通过 service 解析
                .wordId(wordId)                      // INT PK 用于外键
                .quizType(QuizType.valueOf(req.getQuizType()))
                .isCorrect(isCorrect)
                .responseTimeMs(req.getResponseTimeMs())
                .wrongAnswer(req.getWrongAnswer())
                .reviewedAt(LocalDateTime.now())
                .build();
        reviewLogRepository.save(log);

        // ──── 3. SM-2 算法 ────
        if (isCorrect) {
            prevConsecutive++;                     // 连续答对数 +1
            if (prevConsecutive == 1) {
                interval = 1;                       // 首次答对：1 天后复习
            } else if (prevConsecutive == 2) {
                interval = 6;                       // 二次答对：6 天后
            } else {
                interval = (int) Math.round(interval * easeFactor);  // 指数增长
            }
            easeFactor = Math.min(3.0, easeFactor + 0.1);  // 难度系数缓慢增加
        } else {
            prevConsecutive = 0;                    // 答错重置
            interval = 1;                            // 明天重新复习
            easeFactor = Math.max(1.3, easeFactor - 0.2);  // 难度降低
        }

        // ──── 4. 更新单词 SM-2 参数 ────
        word.setConsecutiveCorrect(prevConsecutive);
        word.setEaseFactor(BigDecimal.valueOf(easeFactor));
        word.setIntervalDays(interval);
        word.setNextReview(LocalDateTime.now().plusDays(interval));
        word.setReviewCount(word.getReviewCount() + 1);
        word.setLastReviewedAt(LocalDateTime.now());
        word.setStage(Math.min(prevConsecutive, 7));  // stage 上限 7
        wordRepository.save(word);

        // ──── 5. 计算 XP ────
        int xpGained = isCorrect ? 10 : 2;           // 基本 XP
        if (isCorrect && wasNewWord) xpGained += 15; // 新词奖励
        if (prevConsecutive >= 5) xpGained += 5;     // 连续答对奖励
        if (req.getResponseTimeMs() != null && req.getResponseTimeMs() < 3000)
            xpGained += 3;                            // 速度奖励

        // ──── 6. 更新每日活动 ────
        LocalDate today = LocalDate.now();
        LearningActivity activity = learningActivityRepository
                .findByUserIdAndActivityDate(userId, today)
                .orElse(LearningActivity.builder()
                        .uuid(UUID.randomUUID().toString())  // uuid 业务标识
                        .userId(userId)
                        .activityDate(today)
                        .wordsStudied(0)
                        .reviewsDone(0)
                        .timeSpentSec(0)
                        .correctCount(0)
                        .wrongCount(0)
                        .build());

        activity.setReviewsDone(activity.getReviewsDone() + 1);
        activity.setTimeSpentSec(activity.getTimeSpentSec()
                + (req.getResponseTimeMs() != null ? req.getResponseTimeMs() / 1000 : 0));
        if (isCorrect) activity.setCorrectCount(activity.getCorrectCount() + 1);
        else activity.setWrongCount(activity.getWrongCount() + 1);
        if (isCorrect && wasNewWord)
            activity.setWordsStudied(activity.getWordsStudied() + 1);
        learningActivityRepository.save(activity);

        // ──── 7. 更新用户统计 ────
        // userId 为用户 UUID，user_stat 通过 user.uuid 对应，底层使用 INT FK
        UserStat stat = userStatRepository.findByUserUuid(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserStat"));
        stat.setXp(stat.getXp() + xpGained);
        stat.setTotalReviews(stat.getTotalReviews() + 1);
        if (isCorrect && wasNewWord)
            stat.setTotalWordsLearned(stat.getTotalWordsLearned() + 1);
        userStatRepository.save(stat);

        // ──── 8. 构建返回 ────
        return ReviewResultResponse.builder()
                .xpGained(xpGained)
                .stage(word.getStage())
                .nextReview(word.getNextReview().toString())
                .build();
    }
}
```

**`PlanService.generateDailyPlan()` — 策略选词**：

```java
public int generateDailyPlan(String userId, GeneratePlanRequest req) {
    String bookId = req.getWordBookId();
    String strategyId = req.getStrategyId();
    LocalDate date = LocalDate.parse(req.getDate());
    int count = req.getCount() != null ? req.getCount() : 10;

    StudyStrategy strategy = studyStrategyRepository.findByUuid(strategyId)
            .orElseThrow(() -> new ResourceNotFoundException("Strategy"));
    List<WordBookEntry> entries;

    // 根据策略类型选择不同排序方式
    switch (strategy.getType()) {
        case random:
            // JPQL: ORDER BY RAND()
            entries = wordBookEntryRepository.findRandom(bookId, count);
            break;
        case sequential:
            entries = wordBookEntryRepository
                    .findByWordBookIdOrderBySortOrder(bookId,
                            PageRequest.of(0, count));
            break;
        case difficulty_asc:
            entries = wordBookEntryRepository
                    .findByWordBookIdOrderByDifficultyAsc(bookId, count);
            break;
        case difficulty_desc:
            entries = wordBookEntryRepository
                    .findByWordBookIdOrderByDifficultyDesc(bookId, count);
            break;
        default:
            entries = wordBookEntryRepository.findRandom(bookId, count);
    }

    // 插入每日计划
    for (int i = 0; i < entries.size(); i++) {
        DailyPlanItem item = DailyPlanItem.builder()
                .uuid(UUID.randomUUID().toString())  // uuid 业务标识
                .userId(userId)
                .wordBookId(bookId)
                .planDate(date)
                .wordId(entries.get(i).getWordId())
                .sortOrder(i)
                .isCompleted(false)
                .build();
        dailyPlanItemRepository.save(item);
    }

    return entries.size();
}
```

### 5.4 Controller 层 — API 接口

```java
@RestController                    // = @Controller + @ResponseBody
@RequestMapping("/api/review")     // 路径前缀
@RequiredArgsConstructor           // 构造器注入 service
public class ReviewController {

    private final ReviewService reviewService;

    // ──── GET /api/review/queue?mode=card&limit=20&source=CET-4 ────
    @GetMapping("/queue")
    public ApiResponse<ReviewQueueResponse> getQueue(
            @RequestParam(defaultValue = "card") String mode,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String source) {
        String userId = getCurrentUserId();
        ReviewQueueResponse result = reviewService.getQueue(userId, mode, limit, source);
        return ApiResponse.success(result);
    }

    // ──── POST /api/review/result ────
    @PostMapping("/result")
    public ApiResponse<ReviewResultResponse> submitResult(
            @Valid @RequestBody ReviewResultRequest req) {
        String userId = getCurrentUserId();
        ReviewResultResponse result = reviewService.submitResult(userId, req);
        return ApiResponse.success(result);
    }

    // ──── GET /api/review/distractors?wordId=xxx&pos=vt.&count=3 ────
    @GetMapping("/distractors")
    public ApiResponse<?> getDistractors(
            @RequestParam String wordId,
            @RequestParam(required = false) String pos,
            @RequestParam(defaultValue = "3") int count) {
        // 选择题干扰项
        List<String> distractors = reviewService.getDistractors(wordId, pos, count);
        return ApiResponse.success(Map.of("distractors", distractors));
    }

    // ──── GET /api/review/stats ────
    @GetMapping("/stats")
    public ApiResponse<?> getStats() {
        return ApiResponse.success(reviewService.getReviewStats(getCurrentUserId()));
    }

    /**
     * 从 Spring Security 上下文中提取当前用户 ID。
     * SecurityContext 在 JwtAuthFilter 中被设置。
     */
    private String getCurrentUserId() {
        // JWT 中存储的是 user.uuid（业务标识），需在 Service 层 resolve 为 INT PK
        return (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
```

### 5.5 DTO 层 — 数据传输

**统一响应：**

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;                  // 状态码 (200/400/401/403/404/409/500)
    private String message;            // 描述信息
    private T data;                    // 业务数据

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }

    public static <T> ApiResponse<T> conflict(String message) {
        return new ApiResponse<>(409, message, null);
    }
    // ... badRequest, unauthorized, forbidden, internalError
}
```

**复杂嵌套 DTO 示例**（`WordDetailResponse.java`，含 11 个内部类）：

```java
@Data
@Builder
public class WordDetailResponse {
    private String id;              // 对外暴露 uuid 值（兼容旧 API）
    private String word;
    private String phoneticUk;
    private String phoneticUs;
    private String pos;
    private String meaningCn;
    // ...
    
    private List<DefinitionDTO> definitions;
    private List<CollocationDTO> collocations;
    private UserDataDTO userData;       // 嵌套用户数据
    
    @Data @Builder
    public static class DefinitionDTO {
        private String id;          // 对外暴露 uuid 值
        private String meaningEn;
        private String meaningCn;
        private int sortOrder;
    }
    
    @Data @Builder
    public static class UserDataDTO {
        private int stage;
        private int confidence;
        private String nextReview;
        private int reviewCount;
        private Integer frequency;
        private List<FavoriteRefDTO> favorites;
        private NoteDTO notes;
        private Integer rating;
    }
    
    @Data @Builder
    public static class FavoriteRefDTO {
        private String folderId;    // 对外暴露 uuid 值
        private String folderName;
    }
    // ... CollocationDTO, PrepPatternDTO, ExampleDTO, RelationDTO, TagDTO ...
}

// Service 层 DTO 转换示例：
// response.setId(word.getUuid());  // DTO 的 id 字段映射 entity.uuid
// response.getDefinitions().forEach(d -> d.setId(def.getUuid()));
```

### 5.6 Config/Exception 层 — 基础设施

**`JwtAuthFilter.java`** — 每个请求必经的认证拦截：

```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // 无 Token → 直接放行（后续 Spring Security 会拦截需要认证的路径）
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Token 无效 → 放行但不会设置认证（后续被 Spring Security 拦截返回 401）
        if (!jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Token 有效 → 从 Claims 中提取用户信息
        String userUuid = jwtUtil.getUserUuid(token);  // JWT 存 user.uuid
        var claims = jwtUtil.parseToken(token);
        String role = claims.get("role", String.class);

        // 创建 Spring Security 认证令牌
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    userUuid,                          // principal = user.uuid
                    null,                              // credentials = null
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                );

        // 设置到安全上下文 → Controller 可通过 SecurityContextHolder 获取
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
```

**`GlobalExceptionHandler.java`** — 统一错误处理：

```java
@RestControllerAdvice   // 全局拦截 @Controller 异常
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound(e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getCode())
                .body(new ApiResponse<>(e.getCode(), e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest(msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.internalError(e.getMessage()));
    }
}
```

---

## 6. 数据库设计

### 6.1 总览

36 张表，全部采用 **INT AUTO_INCREMENT 主键 + UUID CHAR(36) UNIQUE 双键策略**，统一 `created_at` / `updated_at` 审计字段。

INT PK 用于内部 JOIN 和索引（高性能、省空间），UUID 用于对外 API 暴露（安全、防爬虫、支持分库分表）。

### 6.2 ER 关系图（文本格式）

> 所有表的 PK 均为 INT AUTO_INCREMENT，`uuid CHAR(36) UNIQUE` 用于 API 查找。图中 `id (PK)` 表示 INT PK，FK 列亦为 INT。

```
┌───────────────┐       ┌──────────────────┐       ┌──────────────────┐
│     users     │1──N→  │  user_settings   │       │    user_stats    │
│───────────────│       │──────────────────│       │──────────────────│
│ id (PK)       │       │ user_id (FK)     │       │ user_id (FK, UN) │
│ username (UN) │       │ setting_key (UN) │       │ xp               │
│ password_hash │       │ setting_value    │       │ level            │
│ email (UN)    │       └──────────────────┘       │ streak_days      │
│ role          │                                   └──────────────────┘
│ default_      │       ┌──────────────────┐
│ strategy_id   │1──N→  │  user_plans      │       ┌──────────────────┐
│ is_active     │       │──────────────────│       │  learning_plans  │
└──────┬────────┘       │ user_id (FK)     │       │──────────────────│
       │                │ plan_id (FK) ────│───N→1 │ id               │
       │1──N→           │ started_at       │       │ name             │
       │                │ current_day      │       │ duration_days    │
       │                │ completed_at     │       │ daily_word_count │
       │                └──────────────────┘       └──────────────────┘
       │
       │1──N→  ┌──────────────────┐       ┌──────────────────┐
       │       │favorite_folders  │1──N→  │    favorites     │
       │       │──────────────────│       │──────────────────│
       │       │ user_id (FK)     │       │ folder_id (FK)   │
       │       │ name             │       │ entity_type      │
       │       │ category         │       │ entity_id        │
       │       │ is_default       │       │ note             │
       │       └──────────────────┘       └──────────────────┘
       │
       │1──N→  ┌──────────────────┐       ┌──────────────────┐
       │       │  user_tags       │1──N→  │user_entity_tags  │
       │       │──────────────────│       │──────────────────│
       │       │ user_id (FK)     │       │ user_id (FK)     │
       │       │ tag (UN)         │       │ tag_id (FK)      │
       │       │ color            │       │ entity_type      │
       │       └──────────────────┘       │ entity_id        │
       │                                  └──────────────────┘
       │
       │1──N→  ┌──────────────────┐       ┌──────────────────┐
       │       │   user_notes     │       │  review_log      │
       │       │──────────────────│       │──────────────────│
       │       │ user_id (FK)     │       │ user_id (FK)     │
       │       │ entity_type      │       │ word_id (FK)     │
       │       │ entity_id        │       │ is_correct       │
       │       │ content          │       │ quiz_type        │
       │       │ is_private       │       │ response_time_ms │
       │       └──────────────────┘       │ wrong_answer     │
       │                                  └────────┬─────────┘
       │1──N→  ┌──────────────────┐               │
       │       │learning_activities│               │
       │       │──────────────────│               │
       │       │ user_id (FK)     │               │
       │       │ activity_date    │               │
       │       │ words_studied    │               │
       │       │ reviews_done     │               │
       │       └──────────────────┘               │
       │                                          │
       │1──N→  ┌──────────────────┐    ┌──────────┴──────────┐
       │       │search_history    │    │       words         │
       │       │──────────────────│    │─────────────────────│
       │       │ user_id (FK)     │    │ id (PK)             │
       │       │ query            │    │ word (UN)           │
       │       │ searched_at      │    │ pos                 │
       │       └──────────────────┘    │ meaning_cn          │
       │                               │ stage (SM-2)        │
       │1──N→  ┌──────────────────┐    │ consecutive_correct │
       │       │daily_recommend.  │    │ ease_factor         │
       │       │──────────────────│    │ next_review         │
       │       │ user_id (FK)     │    └─────────┬───────────┘
       │       │ recommend_date   │              │
       │       │ entity_type      │1─────────────┘
       │       │ entity_id        │
       │       │ is_consumed      │     ┌──────────────────┐
       │       └──────────────────┘     │  definitions     │
       │                                │──────────────────│
       │1──N→  ┌──────────────────┐     │ word_id (FK)     │
       │       │user_daily_plan   │     │ meaning_en       │
       │       │_entries          │     │ meaning_cn       │
       │       │──────────────────│     └──────────────────┘
       │       │ user_id (FK)     │
       │       │ plan_date        │     ┌──────────────────┐
       │       │ word_id (FK) ────│──N→1│  collocations    │
       │       │ is_completed     │     │──────────────────│
       │       └──────────────────┘     │ word_id (FK)     │
       │                                │ collocation      │
       │1──N→  ┌──────────────────┐     │ translation      │
       │       │user_word_book    │     └──────────────────┘
       │       │_progress         │
       │       │──────────────────│     ┌──────────────────┐
       │       │ user_id (FK)     │     │  prep_patterns   │
       │       │ word_book_id(FK) │     │──────────────────│
       │       │ strategy_id(FK)  │     │ word_id (FK)     │
       │       │ current_position │     │ pattern          │
       │       │ daily_count      │     │ preposition      │
       │       └──────────────────┘     └──────────────────┘

┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│    word_books    │1──N→ word_book_entries  │    │    examples       │
│──────────────────│    │──────────────────│    │──────────────────│
│ id (PK)          │    │ word_book_id(FK)  │    │ word_id (FK)     │
│ name             │    │ word_id (FK)      │    │ sentence_en      │
│ difficulty_level │    │ sort_order        │    │ sentence_cn      │
│ word_count       │    └────────┬──────────┘    │ source_type      │
│ is_active        │             │               └──────────────────┘
└──────────────────┘             │
                                 │N──→1  ┌──────────────────┐
┌──────────────────┐             │       │  word_relations  │
│study_strategies  │             │       │──────────────────│
│──────────────────│             │       │ word_id (FK)     │
│ id (PK)          │             │       │ related_word_id  │
│ name             │             │       │ relation_type    │
│ type             │             │       └──────────────────┘
│ config (JSON)    │
└──────────────────┘             │       ┌──────────────────┐
                                 │       │  word_forms      │
┌──────────────────┐             │       │──────────────────│
│daily_plan_items  │             │       │ word_id (FK)     │
│──────────────────│             │       │ form_type        │
│ user_id (FK)     │             │       │ form_value       │
│ word_book_id(FK) │             │       └──────────────────┘
│ plan_date        │             │
│ word_id (FK) ────│─────────────┘       ┌──────────────────┐
│ is_completed     │                     │  word_variants   │
└──────────────────┘                     │──────────────────│
                                         │ word_id (FK)     │
                                         │ variant          │
                                         │ region           │
                                         └──────────────────┘
```

### 6.3 核心索引策略

```sql
-- 复习队列复合索引（SM-2 查询的核心）
ALTER TABLE words ADD INDEX idx_stage_review (stage, next_review);

-- 单词本筛选复合索引
ALTER TABLE words ADD INDEX idx_letter_pos (first_letter, pos);

-- 答题日志用户+单词复合索引
CREATE INDEX idx_user_word ON review_log (user_id, word_id);

-- 每日计划用户+日期复合索引
CREATE INDEX idx_user_date ON daily_plan_items (user_id, plan_date);
```

### 6.4 关键设计决策

| 决策 | 说明 | 代码体现 |
|---|---|---|---|
| **INT PK + UUID 双键** | INT PK 高性能 JOIN，UUID 暴露给 API 保证安全，拒绝自增 ID 泄露数据规模 | `Long id`（`@GeneratedValue`）+ `String uuid`（`@PrePersist`） |
| **UUID 查询入口** | 外部 API 通过 UUID 查找，内部 JOIN 用 INT PK | `findByUuid(String)`（Repo）+ `findById(Long)`（内部） |
| **复合主键取消** | 原 @IdClass 复合主键改为 INT PK + UNIQUE 约束，简化 JPA 映射 | `WordBookEntry` 用 `Long id` + `UNIQUE(word_book_id, word_id)` |
| **枚举字符串** | `@Enumerated(STRING)` 而非 ORDINAL，可读+可迁移 | `User.Role`、`StudyStrategy.StrategyType` |
| **JSON 字段** | badges.criteria / strategies.config 存 JSON | `String criteria` JPA 读取，业务层解析 |
| **软删除** | `is_active` 标记而非 DELETE | `WHERE is_active = TRUE` |
| **Lombok @Data** | 自动 getter/setter/equals/hashCode/toString | `@Data @Builder @NoArgsConstructor` |

---

## 7. API 设计

### 7.1 URI 设计规范

```
/api/{资源名}                   集合
/api/{资源名}/{id}             单个资源
/api/{资源名}?page=1&size=20   分页
/api/{资源名}/{id}/{子资源}     子资源
```

### 7.2 HTTP 方法与语义

| 方法 | 语义 | 幂等 | 响应 |
|---|---|---|---|
| GET | 查询 | 是 | 200 + data |
| POST | 创建 | 否 | 200 + data / 201 |
| PUT | 全量更新或 upsert | 是 | 200 |
| DELETE | 删除 | 是 | 200 / 204 |

### 7.3 参数规范

| 参数位置 | 用途 | 示例 |
|---|---|---|
| Path | 资源 ID | `/api/words/{id}` |
| Query | 筛选/分页 | `?page=1&size=20&pos=vt.` |
| Body | 创建/更新数据 | `{"wordId":"xxx","isCorrect":true}` |

### 7.4 分页规范

所有列表接口统一分页参数和响应格式：

**请求**：`?page=1&size=20`（page 从 1 开始）

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [...],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 156,
      "total_pages": 8
    }
  }
}
```

### 7.5 接口概览

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| Auth | POST | `/api/auth/register` | 注册 |
| | POST | `/api/auth/login` | 登录 |
| | POST | `/api/auth/logout` | 退出 |
| Dashboard | GET | `/api/dashboard` | 首页概览 |
| | PUT | `/api/dashboard/recommendations/{id}/consume` | 消耗推荐 |
| Search | GET | `/api/search/suggest?q=` | 联想 |
| | GET | `/api/search?q=` | 搜索 |
| | POST | `/api/search/history` | 记录历史 |
| | GET | `/api/search/history` | 获取历史 |
| | DELETE | `/api/search/history` | 清除历史 |
| Word | GET | `/api/words/{id}` | 单词详情 |
| | PUT | `/api/words/{id}/frequency` | 设置词频 |
| | PUT | `/api/words/{id}/note` | 保存笔记 |
| | POST | `/api/words/{id}/tags` | 打标签 |
| | DELETE | `/api/words/{id}/tags/{tagId}` | 移除标签 |
| | PUT | `/api/words/{id}/rating` | 评分 |
| Tag | GET | `/api/tags` | 标签列表 |
| | POST | `/api/tags` | 创建标签 |
| Review | GET | `/api/review/queue` | 复习队列 |
| | POST | `/api/review/result` | 提交答题 |
| | GET | `/api/review/distractors` | 干扰项 |
| | GET | `/api/review/stats` | 复习统计 |
| Article | GET | `/api/articles` | 文章列表 |
| | GET | `/api/articles/{id}` | 文章内容 |
| | PUT | `/api/articles/{id}/progress` | 保存进度 |
| | PUT | `/api/articles/{id}/complete` | 标记读完 |
| | GET | `/api/articles/{id}/lookup?word=` | 文中查词 |
| Folder | GET | `/api/folders` | 收藏夹列表 |
| | POST | `/api/folders` | 创建 |
| | PUT | `/api/folders/{id}` | 编辑 |
| | DELETE | `/api/folders/{id}` | 删除 |
| | PUT | `/api/folders/reorder` | 排序 |
| | GET | `/api/folders/{id}/items` | 收藏内容 |
| Favorite | POST | `/api/favorites` | 添加收藏 |
| | DELETE | `/api/favorites/{id}` | 取消收藏 |
| | POST | `/api/favorites/batch-delete` | 批量删除 |
| | POST | `/api/favorites/batch-tag` | 批量打标签 |
| WrongWord | GET | `/api/wrong-words` | 错题列表 |
| | POST | `/api/wrong-words/review` | 生成错题复习 |
| Plan | GET | `/api/plans/active` | 当前计划 |
| | GET | `/api/plans/templates` | 计划模板 |
| | POST | `/api/plans/join` | 加入计划 |
| | GET | `/api/plans/daily/words` | 每日词表 |
| | GET | `/api/plans/daily/dates` | 有计划的日期 |
| | POST | `/api/plans/daily/entries` | 添加计划词 |
| | DELETE | `/api/plans/daily/entries/{id}` | 移除计划词 |
| | PUT | `/api/plans/daily/entries/{id}/complete` | 标记完成 |
| | POST | `/api/plans/daily/generate` | 策略生成计划 |
| WordBook | GET | `/api/word-books` | 单词本列表 |
| | GET | `/api/word-books/{id}/words` | 单词本词条 |
| | GET | `/api/word-books/pos-categories` | 词性分类 |
| Strategy | GET | `/api/strategies` | 策略列表 |
| User | GET | `/api/user/profile` | 个人信息 |
| | PUT | `/api/user/profile` | 更新资料 |
| | GET | `/api/user/settings` | 获取设置 |
| | PUT | `/api/user/settings` | 保存设置 |
| | GET | `/api/user/activity` | 学习活动 |
| | GET | `/api/user/badges` | 用户徽章 |
| | GET | `/api/user/default-strategy` | 默认策略 |
| | PUT | `/api/user/default-strategy` | 设置默认策略 |
| | PUT | `/api/user/stats/streak` | 更新打卡 |
| Leaderboard | GET | `/api/leaderboard` | 排行榜 |
| Badge | GET | `/api/badges` | 全量徽章 |
| Admin | GET | `/api/admin/overview` | 平台总览 |
| | GET | `/api/admin/users` | 用户管理 |
| | PUT | `/api/admin/users/{id}/status` | 禁用/启用 |
| | GET | `/api/admin/words` | 词库列表 |
| | POST | `/api/admin/words` | 新增单词 |
| | PUT | `/api/admin/words/{id}` | 编辑单词 |
| | DELETE | `/api/admin/words/{id}` | 删除单词 |
| | POST | `/api/admin/words/batch-import` | 批量导入 |
| | GET | `/api/admin/feedback` | 反馈审核 |

### 7.6 单词详情接口数据结构

`GET /api/words/{id}` 返回的 JSON 结构（含注释）：

```json
{
  "id": "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01",
  "word": "abandon",
  "pos": "vt.",

  /* ── 发音 ── */
  "phonetic_uk": "/əˈbændən/",
  "phonetic_us": "/əˈbændən/",
  "audio_uk": "https://...",
  "audio_us": "https://...",

  /* ── 释义 ── */
  "meaning_cn": "放弃；遗弃；抛弃",
  "etymology_cn": "源自古法语 abandoner，意为置于控制之下",
  "source": "CET-4",
  "difficulty": 2,
  "frequency": 80,

  /* ── 详细释义（一词多义） ── */
  "definitions": [
    { "id": "uuid", "meaning_en": "to leave completely", "meaning_cn": "放弃", "sort_order": 1 }
  ],

  /* ── 固定搭配 ── */
  "collocations": [
    { "id": "uuid", "collocation": "abandon hope", "translation": "放弃希望", "frequency": 5 }
  ],

  /* ── 介词模式 ── */
  "prep_patterns": [
    { "id": "uuid", "pattern": "abandon sth to sb", "preposition": "to", "frequency": 3 }
  ],

  /* ── 例句（含评分） ── */
  "examples": [
    { "id": "uuid", "sentence_en": "The captain ordered...", "rating": 4.5 }
  ],

  /* ── 同反义词 ── */
  "relations": {
    "synonyms": [{ "word_id": "uuid", "word": "desert" }],
    "antonyms": [{ "word_id": "uuid", "word": "keep" }]
  },

  /* ── 用户个人数据 ── */
  "user_data": {
    "stage": 2,           /* SM-2 阶段 */
    "confidence": 3,      /* 掌握度 1-5 */
    "next_review": "2026-05-27T00:00:00",
    "review_count": 5,
    "consecutive_correct": 3,
    "favorites": [{ "folder_id": "uuid", "folder_name": "稍后复习" }],
    "notes": { "content": "考研常考", "is_private": false },
    "tags": [{ "id": "uuid", "tag": "写作词汇", "color": "#FF5733" }],
    "rating": 4           /* 用户评分 */
  }
}
```

---

## 8. 认证与安全

### 8.1 JWT 完整生命周期

```
┌──────────────────────────────────────────────────────────────────────┐
│                         JWT 生命周期                                   │
│                                                                      │
│  注册/登录 ──→ 服务端验证 ──→ 签发 Token ──→ 返回给客户端              │
│                                                   │                  │
│                                       客户端存储到 localStorage       │
│                                                   │                  │
│                              ┌────────────────────┘                  │
│                              ▼                                       │
│                   后续每个请求携带 Token                               │
│                   Authorization: Bearer eyJhbG...                    │
│                              │                                       │
│                              ▼                                       │
│                   服务端 JwtAuthFilter 拦截                            │
│                              │                                       │
│                    ┌─────────┴──────────┐                           │
│                    ▼                    ▼                           │
│               Token 有效           Token 无效/过期                    │
│                    │                    │                           │
│                    ▼                    ▼                           │
│          设置 SecurityContext     返回 401                            │
│          Controller 正常执行                                         │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### 8.2 JWT 结构

```
Header:     { "alg": "HS256", "typ": "JWT" }
Payload:    {
              "sub": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12",  // user.uuid
              "username": "demo",
              "role": "user",
              "iat": 1716000000,    // 签发时间
              "exp": 1716086400     // 过期时间（24h）
            }
Signature:  HMAC-SHA256(base64(Header) + "." + base64(Payload), secret)
```

### 8.3 密码存储

```
用户输入密码 "abc123"
        ↓
BCryptPasswordEncoder.encode()
        ↓
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
  ├── $2a    = BCrypt 算法版本
  ├── $10    = 2^10 轮迭代（强度因子）
  ├── salt   = 22 字符随机盐
  └── hash   = 31 字符哈希值
        ↓
每次 encode() 结果不同（不同盐），但 matches() 可验证
```

### 8.4 权限矩阵

| 路径 | 匿名 | USER | ADMIN |
|---|---|---|---|
| `POST /api/auth/register` | ✅ | ✅ | ✅ |
| `POST /api/auth/login` | ✅ | ✅ | ✅ |
| `GET /api/badges` | ✅ | ✅ | ✅ |
| `/api/admin/**` | ❌ | ❌ | ✅ |
| 其余 `/api/**` | ❌ | ✅ | ✅ |

### 8.5 安全最佳实践

```yaml
# 生产环境建议
app:
  jwt:
    secret: <环境变量注入，非明文配置>
    expiration-ms: 3600000  # 生产建议 1h 而非 24h
```

```java
// Controller 中避免参数注入风险
// ❌ 不要这样写：
@RequestMapping("/api/dynamic/{table}")
public void inject(@PathVariable String table) {
    // 表名拼接 SQL 有注入风险
}

// ✅ 正确做法：从 SecurityContext 获取当前用户
        String userUuid = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();          // JWT 存用户 UUID

        // ──── POST /api/review/result ────
    @PostMapping("/result")
    public ApiResponse<ReviewResultResponse> submitResult(
            @Valid @RequestBody ReviewResultRequest req) {
        String userUuid = getCurrentUserId();
        ReviewResultResponse result = reviewService.submitResult(userUuid, req);
        return ApiResponse.success(result);
    }

word.setConsecutiveCorrect(consecutive);
word.setEaseFactor(BigDecimal.valueOf(ease));
word.setIntervalDays(interval);
word.setNextReview(LocalDateTime.now().plusDays(interval));
word.setStage(Math.min(consecutive, 7));
```

### 9.2 XP 与等级系统

```
答题
  ├── 答对         +10 XP
  ├── 答错          +2 XP
  ├── 连续 5+ 答对   +5 XP（额外奖励）
  ├── 新词首学      +15 XP
  └── 3s 内答对     +3 XP

阅读
  └── 读完一篇      +30 XP

等级阈值:
  Lv.1     0 XP    → Lv.2   100 XP
  Lv.2   100 XP    → Lv.3   250 XP
  Lv.3   250 XP    → Lv.4   500 XP
  Lv.4   500 XP    → Lv.5   800 XP
  Lv.5   800 XP    → Lv.6  1200 XP
  Lv.6  1200 XP    → Lv.7  1700 XP
  Lv.7  1700 XP    → Lv.8  2300 XP
  Lv.8  2300 XP    → Lv.9  3000 XP
  Lv.9  3000 XP    → Lv.10 4000 XP
```

### 9.3 单词本策略选词

```java
public enum StrategyType {
    random,             // ORDER BY RAND()
    alphabetical,       // ORDER BY w.word
    pos_alphabetical,   // ORDER BY w.pos, w.word
    pos_random,         // ORDER BY w.pos, RAND()
    difficulty_asc,     // ORDER BY w.difficulty
    difficulty_desc     // ORDER BY w.difficulty DESC
}
```

### 9.4 每日计划合并逻辑

```
用户请求某日计划 (GET /api/plans/daily/words?date=2026-05-21)
        │
        ├── 查 user_daily_plan_entries (手动添加)
        │   WHERE user_id=? AND plan_date='2026-05-21'
        │   ORDER BY sort_order
        │
        ├── 查 daily_plan_items (系统生成)
        │   WHERE user_id=? AND plan_date='2026-05-21'
        │   ORDER BY sort_order
        │
        └── 合并两个 List，按 sort_order 排序后返回
```

---

## 10. 配置说明

### 10.1 `application.yml` 完整配置

```yaml
# ── 服务器 ──
server:
  port: 8080
  tomcat:
    threads:
      max: 200            # 最大工作线程数
    max-connections: 8192  # 最大连接数

# ── Spring ──
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/word_learning
      ?useSSL=false
      &serverTimezone=Asia/Shanghai
      &allowPublicKeyRetrieval=true
      &characterEncoding=utf8mb4
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:                         # HikariCP 连接池
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000

  jpa:
    hibernate:
      ddl-auto: validate             # validate | update | create | create-drop
    show-sql: false                  # 开发可改为 true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

# ── 应用配置 ──
app:
  jwt:
    secret: "YTJmOWE0ZjJjM2I0NTY3ODkwYWJjZGVmMDEyMzQ1Njc4OTBhYmNkZWYwMTIzNDU2Nzg5MGFiY2RlZjAxMjM0"
    expiration-ms: 86400000          # Token 有效期 24h
```

### 10.2 Spring Boot 配置类别

| 配置前缀 | 作用域 | 关键属性 |
|---|---|---|
| `server.*` | HTTP 服务器 | port, tomcat.threads.max |
| `spring.datasource.*` | 数据源 | url, username, password |
| `spring.jpa.*` | JPA/Hibernate | ddl-auto, show-sql, dialect |
| `spring.jackson.*` | JSON 序列化 | date-format, time-zone |
| `app.jwt.*` | 自定义 JWT | secret, expiration-ms |

### 10.3 多环境配置（建议）

```yaml
# application-dev.yml  (开发)
spring:
  jpa:
    hibernate:
      ddl-auto: update    # 自动建表
    show-sql: true

# application-prod.yml (生产)
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 仅校验，不改库
    show-sql: false
```

启动时指定：`java -jar app.jar --spring.profiles.active=prod`

---

## 11. 部署与运行

### 11.1 环境要求

```
JDK    >= 17 (推荐 Eclipse Temurin 17 LTS)
MySQL  >= 8.0 (推荐 8.0.32+)
Maven  >= 3.8
内存   >= 512MB (开发) / 2GB (生产)
```

### 11.2 快速启动

```bash
# 1. 创建数据库
mysql -u root -p < ../database/schema.sql

# 2. 修改配置
vim src/main/resources/application.yml
#   修改 spring.datasource.{url, username, password}

# 3. 编译打包
mvn clean package -DskipTests

# 4. 运行
java -jar target/word-learning-backend-1.0.0.jar

# 或用开发模式（自动重启）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 11.3 验证 API

```bash
# 注册
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"123456","email":"demo@test.com"}'

# 返回: {"code":200,"message":"success","data":{"token":"eyJ...","user":{...}}}

# 登录（获取 Token）
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"123456"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

echo $TOKEN

# 访问首页
curl http://localhost:8080/api/dashboard \
  -H "Authorization: Bearer $TOKEN"

# 获取复习队列
curl http://localhost:8080/api/review/queue?limit=5 \
  -H "Authorization: Bearer $TOKEN"

# 获取单词详情
curl http://localhost:8080/api/words/b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01 \
  -H "Authorization: Bearer $TOKEN"

# 查看健康状态
curl http://localhost:8080/actuator/health
```

### 11.4 生产部署 Checklist

- [ ] JWT secret 改为环境变量注入（`${JWT_SECRET}`）
- [ ] `spring.jpa.hibernate.ddl-auto` 设为 `validate`
- [ ] MySQL 用户使用最小权限（仅 CRUD 权限）
- [ ] 开启 HTTPS（Nginx 反向代理或 Spring 配置 SSL）
- [ ] 配置 Actuator 端点安全（`management.endpoints.web.exposure.include=health,info`）
- [ ] JVM 内存调优：`-Xms512m -Xmx1024m`
- [ ] 日志配置（`logging.level.com.wordlearning=INFO`）
- [ ] 数据库连接池调优（`maximum-pool-size`）

---

## 12. 附录

### A. 文件统计

| 类别 | 数量 | 代码行数 |
|---|---|---|
| Entity | 36 | ~2,500 |
| Repository | 36 | ~500 |
| Service | 13 | ~2,800 |
| Controller | 17 | ~1,200 |
| Request DTO | 12 | ~150 |
| Response DTO | 27 | ~1,800 |
| Config | 3 | ~150 |
| Exception | 3 | ~100 |
| Util | 1 | ~80 |
| Application | 1 | ~10 |
| **总计 Java** | **149** | **~9,300** |
| application.yml | 1 | ~40 |
| pom.xml | 1 | ~80 |
| **项目总计** | **151** | **~9,400** |

### B. 扩展指南

#### 新增 API 的标准化流程

```
步骤 1: 数据库 — schema.sql 新增表（或改已有表）
步骤 2: Entity — 创建 JPA 实体类
步骤 3: Repository — 创建 Repository 接口
步骤 4: DTO — 创建 Request/Response DTO
步骤 5: Service — 实现业务逻辑 (@Transactional)
步骤 6: Controller — 暴露 REST 接口
步骤 7: 验证 — curl 测试
步骤 8: 文档 — 更新 api-doc.md 和 TECHNICAL.md
```

#### 数据库迁移（无 Flyway/Liquibase 时）

```sql
-- 方式一：ddl-auto=update（开发用）
-- spring.jpa.hibernate.ddl-auto: update

-- 方式二：手动执行迁移脚本
-- sql/migration/V2__add_column.sql
ALTER TABLE words ADD COLUMN example_count INT DEFAULT 0;
```

#### 常用 Maven 命令

```bash
mvn clean                  # 清理 target
mvn compile                # 编译
mvn test                   # 运行测试
mvn package -DskipTests    # 打包（跳过测试）
mvn spring-boot:run        # 开发运行
mvn dependency:tree        # 查看依赖树
```

---

### C. Troubleshooting

| 问题 | 原因 | 解决 |
|---|---|---|
| `Access denied for user` | MySQL 密码/权限错 | 检查 `application.yml` 数据源配置 |
| `Table 'xxx' doesn't exist` | 未执行 schema.sql | `mysql -u root -p < schema.sql` |
| `Field 'uuid' doesn't have a default value` | uuid 业务标识为空 | 检查 `@PrePersist` 中是否生成 `UUID.randomUUID().toString()` |
| 401 所有接口 | Token 缺失/过期 | 重新登录获取 Token |
| 403 管理接口 | 用户不是 admin | 数据库 `users.role` 设为 `admin` |

---

> **文档版本**: 2.0.0  
> **最后更新**: 2026-05-22  
> **代码行数**: ~9,400 Java + ~120 配置  
> **作者**: opencode
