# 英语学习应用 — 完整请求流程文档

> 本文档涵盖系统中所有不同的请求场景，每个场景从前端浏览器到后端服务器的完整链路，包含路由守卫、Token 校验、过滤器链等每一个步骤，并附带对应代码及 ASCII 时序图。

---

## 目录

1. [场景一：用户注册](#场景一用户注册)
2. [场景二：用户登录](#场景二用户登录)
3. [场景三：页面初始化（路由跳转 + Token 恢复）](#场景三页面初始化路由跳转--token-恢复)
4. [场景四：已认证用户发起 API 请求（以学习页面为例）](#场景四已认证用户发起-api-请求以学习页面为例)
5. [场景五：Token 过期 / 401 处理](#场景五token-过期--401-处理)
6. [场景六：用户退出](#场景六用户退出)
7. [场景七：无需认证的公开请求（获取 Badge 列表）](#场景七无需认证的公开请求获取-badge-列表)
8. [全局中间件/过滤器总览](#全局中间件过滤器总览)

---

## 场景一：用户注册

### 流程说明

用户在登录页面切换到「注册」Tab，填写用户名、邮箱、昵称、密码，点击注册按钮触发的完整流程。

### 步骤分解

| 步骤 | 角色 | 动作 | 详细说明 |
|------|------|------|---------|
| 1 | **浏览器 (Login.vue)** | 用户填写表单，点击「注册」 | `handleRegister()` 被调用，读取 `registerForm` 数据 |
| 2 | **浏览器 (Login.vue)** | 调用 `userStore.register()` | 传入 `{ username, password, email, nickname }` |
| 3 | **浏览器 (user store)** | 调用 `authApi.register(data)` | `api.post('/auth/register', data)` — 无 Token 附加 |
| 4 | **浏览器 (http.js)** | 发送 HTTP POST 请求 | 构造 `fetch('/api/auth/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) })` |
| 5 | **Vite DevServer** | 代理请求 | `vite.config.js` 中配置 `'/api' → 'http://localhost:8080'`，请求被转发到后端 |
| 6 | **后端 (CorsFilter)** | CORS 检查 | `CorsConfig` 允许所有 origin/header/method，通过 |
| 7 | **后端 (SecurityConfig)** | 安全过滤链 | `/api/auth/**` 配置为 `.permitAll()`，不需要认证 |
| 8 | **后端 (JwtAuthFilter)** | Token 校验 | 请求无 `Authorization` 头，直接 `filterChain.doFilter()` 放行 |
| 9 | **后端 (AuthController)** | 路由匹配 | `POST /api/auth/register` → `register()` 方法 |
| 10 | **后端 (AuthService)** | 校验并创建用户 | 检查 username/email 唯一性，BCrypt 加密密码，创建 User + UserStat |
| 11 | **后端 (JwtUtil)** | 生成 JWT | `generateToken(uuid, username, role)` — 使用 HMAC-SHA256 签名，有效期 24h |
| 12 | **后端 (AuthService)** | 返回 LoginResponse | `{ token: "eyJ...", expiresIn: 86400, user: { id, username, nickname, role, level } }` |
| 13 | **后端 (AuthController)** | 包装 ApiResponse | `ApiResponse.success(loginResponse)` → `{ code: 200, data: { token, expiresIn, user }, message: "success" }` |
| 14 | **浏览器 (http.js)** | 解析响应 | `res.json()` → `body.data` → `{ token, expiresIn, user }` |
| 15 | **浏览器 (user store)** | 保存 Token | `setToken(res.token)` → `localStorage.setItem('auth_token', token)` |
| 16 | **浏览器 (user store)** | 获取用户信息 | `fetchProfile()` → `api.get('/user/profile')`（此时已携带 Token） |
| 17 | **浏览器 (user store)** | 设置登录状态 | `isLoggedIn.value = true`, `user.value = profile` |
| 18 | **浏览器 (Login.vue)** | 导航到首页 | `router.push('/')` |

### 时序图

```
┌──────────┐    ┌──────────┐    ┌───────────┐    ┌───────────┐    ┌────────────┐    ┌──────────┐
│ Browser  │    │  Vue     │    │  http.js  │    │  Vite     │    │  Backend   │    │  MySQL   │
│ (Login)  │    │  Store   │    │(fetch)    │    │  Proxy    │    │(SpringBoot)│    │          │
└────┬─────┘    └────┬─────┘    └─────┬─────┘    └─────┬─────┘    └──────┬─────┘    └────┬─────┘
     │               │               │               │               │               │
     │ 1.点击注册     │               │               │               │               │
     │──────────────>│               │               │               │               │
     │               │               │               │               │               │
     │ 2.userStore.register(data)     │               │               │               │
     │               │──────────────>│               │               │               │
     │               │               │               │               │               │
     │               │ 3.POST /api/auth/register     │               │               │
     │               │               │──────────────>│               │               │
     │               │               │               │               │               │
     │               │               │   4.代理转发    │               │               │
     │               │               │               │──────────────>│               │
     │               │               │               │               │               │
     │               │               │               │ 5.CorsFilter  │               │
     │               │               │               │   放行        │               │
     │               │               │               │               │               │
     │               │               │               │ 6.permitAll   │               │
     │               │               │               │   放行        │               │
     │               │               │               │               │               │
     │               │               │               │ 7.无Token     │               │
     │               │               │               │   JwtFilter   │               │
     │               │               │               │   跳过        │               │
     │               │               │               │               │               │
     │               │               │               │ 8.检查用户名唯一性           │
     │               │               │               │──────────────>│──────────────>│
     │               │               │               │               │<──────────────│
     │               │               │               │               │               │
     │               │               │               │ 9.BCrypt加密密码+创建用户    │
     │               │               │               │──────────────>│──────────────>│
     │               │               │               │               │<──────────────│
     │               │               │               │               │               │
     │               │               │               │10.生成JWT Token              │
     │               │               │               │               │               │
     │               │               │               │11.返回ApiResponse(data)      │
     │               │               │               │<──────────────│               │
     │               │               │               │               │               │
     │               │               │ 12.返回body.data                    │
     │               │               │<──────────────│               │               │
     │               │               │               │               │               │
     │               │ 13.setToken + fetchProfile    │               │               │
     │               │<──────────────│               │               │               │
     │               │               │               │               │               │
     │               │ 14.router.push('/')           │               │               │
     │<──────────────│               │               │               │               │
```

### 对应代码

**Login.vue** — 注册表单提交：
```js
// 第173-179行
async function handleRegister() {
  registerError.value = ''
  try {
    await userStore.register(registerForm)
    router.push('/')
  } catch (e) {
    registerError.value = e.message || '注册失败，请重试'
  }
}
```

**user store (stores/user.js)** — 注册方法：
```js
// 第22-29行
async function register(data) {
  loading.value = true
  try {
    const res = await authApi.register(data)
    setToken(res.token)
    isLoggedIn.value = true
    await fetchProfile()
  } finally {
    loading.value = false
  }
}
```

**api index (api/index.js)** — 注册 API：
```js
authApi.register = (data) => api.post('/auth/register', data)
```

**http.js** — 发送请求与接收响应：
```js
// 第17-38行
async function request(url, options = {}) {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
  const res = await fetch(`${BASE}${url}`, { ...options, headers })
  const body = await res.json()
  if (!res.ok) {
    throw new Error(body.message || `HTTP ${res.status}`)
  }
  return body.data   // ← 返回 ApiResponse 中的 data 字段
}
```

**AuthController.java** — 注册端点：
```java
// 第22-25行
@PostMapping("/register")
public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
    return ApiResponse.success(authService.register(req));
}
```

**AuthService.java** — 注册逻辑：
```java
// 第34-79行
public LoginResponse register(RegisterRequest req) {
    // 1. 检查用户名/邮箱唯一性
    if (userRepository.findByUsername(req.getUsername()).isPresent()) {
        throw BusinessException.conflict("username already exists");
    }
    // 2. 创建用户（BCrypt加密密码）
    User user = User.builder()
        .username(req.getUsername())
        .passwordHash(passwordEncoder.encode(req.getPassword()))
        .role(User.Role.user)
        .isActive(true)
        .build();
    userRepository.save(user);
    // 3. 创建用户统计记录
    UserStat stat = UserStat.builder().userId(user.getId()).build();
    userStatRepository.save(stat);
    // 4. 生成JWT
    String token = jwtUtil.generateToken(user.getUuid(), user.getUsername(), user.getRole().name());
    return LoginResponse.builder().token(token).build();
}
```

---

## 场景二：用户登录

### 流程说明

用户在登录页面输入用户名和密码，点击「登录」按钮触发的完整流程。

### 步骤分解

| 步骤 | 角色 | 动作 | 详细说明 |
|------|------|------|---------|
| 1 | **浏览器 (Login.vue)** | 用户填写表单，点击「登录」 | `handleLogin()` 被调用，读取 `loginForm` 数据 |
| 2 | **浏览器 (Login.vue)** | 调用 `userStore.login()` | 传入 `{ username, password }` |
| 3 | **浏览器 (user store)** | 调用 `authApi.login(data)` | `api.post('/auth/login', { username, password })` — 无 Token |
| 4 | **浏览器 (http.js)** | 发送 HTTP POST 请求 | `fetch('/api/auth/login', method: 'POST', body: JSON) ` |
| 5 | **Vite DevServer** | 代理到后端 8080 | 同上 |
| 6-8 | **后端过滤器链** | 同注册流程 — `/api/auth/**` 为 `permitAll` | 放行 |
| 9 | **后端 (AuthController)** | 路由匹配 | `POST /api/auth/login` → `login()` 方法 |
| 10 | **后端 (AuthService)** | 校验密码 | 查询用户 → `passwordEncoder.matches(raw, hash)` |
| 11 | **后端 (AuthService)** | 更新最后登录时间 | `user.setLastLoginAt(LocalDateTime.now())` |
| 12 | **后端 (JwtUtil)** | 生成 JWT | `generateToken(uuid, username, role)` |
| 13 | **后端 (AuthService)** | 返回 LoginResponse | `{ token, expiresIn, user }` |
| 14 | **浏览器 (http.js)** | 解析响应 | `body.data` → `{ token, expiresIn, user }` |
| 15 | **浏览器 (user store)** | 保存 Token | `setToken(res.token)` → `localStorage` |
| 16 | **浏览器 (user store)** | 获取用户信息 | `fetchProfile()`（携带 Token 请求 `/api/user/profile`）|
| 17 | **浏览器 (Login.vue)** | 导航到首页 | `router.push('/')` |

### ASCII 时序图

```
┌──────────┐    ┌──────────┐    ┌───────────┐    ┌───────────┐    ┌────────────┐    ┌──────────┐
│ Browser  │    │  Vue     │    │  http.js  │    │  Vite     │    │  Backend   │    │  MySQL   │
│ (Login)  │    │  Store   │    │ (fetch)   │    │  Proxy    │    │(SpringBoot)│    │          │
└────┬─────┘    └────┬─────┘    └─────┬─────┘    └─────┬─────┘    └──────┬─────┘    └────┬─────┘
     │               │               │               │               │               │
     │ 1.点击登录     │               │               │               │               │
     │──────────────>│               │               │               │               │
     │               │               │               │               │               │
     │ 2.userStore.login(u, p)       │               │               │               │
     │               │──────────────>│               │               │               │
     │               │               │               │               │               │
     │               │ 3.POST /api/auth/login        │               │               │
     │               │               │──────────────>│               │               │
     │               │               │               │               │               │
     │               │               │   4.代理转发    │               │               │
     │               │               │               │──────────────>│               │
     │               │               │               │               │               │
     │               │               │               │ 5.CorsFilter  │               │
     │               │               │               │   放行        │               │
     │               │               │               │               │               │
     │               │               │               │ 6.permitAll   │               │
     │               │               │               │   放行        │               │
     │               │               │               │               │               │
     │               │               │               │ 7.查询用户    │               │
     │               │               │               │──────────────>│──────────────>│
     │               │               │               │               │<──────────────│
     │               │               │               │               │               │
     │               │               │               │ 8.BCrypt校验密码              │
     │               │               │               │   matches()   │               │
     │               │               │               │               │               │
     │               │               │               │ 9.更新last_login_at           │
     │               │               │               │──────────────>│──────────────>│
     │               │               │               │               │               │
     │               │               │               │10.生成JWT Token               │
     │               │               │               │               │               │
     │               │               │               │11.返回ApiResponse             │
     │               │               │               │<──────────────│               │
     │               │               │               │               │               │
     │               │               │ 12.body.data   │               │               │
     │               │               │<──────────────│               │               │
     │               │               │               │               │               │
     │               │ 13.setToken(res.token)        │               │               │
     │               │ 14.fetchProfile() (带Token)    │               │               │
     │               │<──────────────│               │               │               │
     │               │               │               │               │               │
     │               │ 15.router.push('/')           │               │               │
     │<──────────────│               │               │               │               │
```

### 对应代码

**Login.vue** — 登录提交：
```js
// 第168-174行
async function handleLogin() {
  loginError.value = ''
  try {
    await userStore.login(loginForm.username, loginForm.password)
    router.push('/')
  } catch (e) {
    loginError.value = e.message || '登录失败，请重试'
  }
}
```

**user store** — login 方法：
```js
// 第14-20行
async function login(username, password) {
  loading.value = true
  try {
    const res = await authApi.login({ username, password })
    setToken(res.token)
    isLoggedIn.value = true
    await fetchProfile()
  } finally {
    loading.value = false
  }
}
```

**AuthService.java** — 登录验证：
```java
// 第83-109行
public LoginResponse login(LoginRequest req) {
    User user = userRepository.findByUsername(req.getUsername())
        .orElseThrow(() -> BusinessException.badRequest("invalid username or password"));

    if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
        throw BusinessException.badRequest("invalid username or password");
    }

    user.setLastLoginAt(LocalDateTime.now());
    userRepository.save(user);

    String token = jwtUtil.generateToken(user.getUuid(), user.getUsername(), user.getRole().name());
    return LoginResponse.builder()
        .token(token)
        .expiresIn(expirationMs / 1000)
        .user(LoginResponse.UserInfo.builder()
            .id(user.getUuid()).username(user.getUsername())
            .nickname(user.getNickname()).role(user.getRole().name()).build())
        .build();
}
```

**SecurityConfig.java** — 公开路由配置：
```java
// 第20-21行
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
```

---

## 场景三：页面初始化（路由跳转 + Token 恢复）

### 流程说明

用户已登录（Token 存在 localStorage），刷新页面或从外部链接进入应用。涵盖两种子场景：**已登录访问受保护页面** 和 **未登录访问受保护页面**。

### 场景 3a：已登录 — 访问受保护页面

#### 步骤分解

| 步骤 | 角色 | 动作 | 详细说明 |
|------|------|------|---------|
| 1 | **浏览器** | 输入 URL 或刷新 | 浏览器导航到 `/learn` |
| 2 | **Vue Router** | `beforeEach` 导航守卫 | `to.name !== 'Login' && !getToken()` — Token 存在，放行 |
| 3 | **Vue** | 初始化 App | `main.js` 执行，创建 Pinia + Router |
| 4 | **Vue** | 读取 Token 恢复登录态 | `main.js` 检查 `localStorage` 中是否有 `auth_token` |
| 5 | **Vue** | 调用 `fetchProfile()` | 异步请求 `/api/user/profile`（携带 Bearer Token） |
| 6 | **浏览器 (http.js)** | 发送 GET 请求 | `fetch('/api/user/profile', { headers: { Authorization: 'Bearer <token>' } })` |
| 7-10 | **Vite Proxy → Backend** | 代理、CORS、JWT 过滤器 | 见场景四 |
| 11 | **后端 (UserController)** | 获取当前用户信息 | `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` → userId |
| 12 | **后端** | 返回用户信息 | `ApiResponse.success(profile)` |
| 13 | **浏览器 (user store)** | 设置 `user` 和 `isLoggedIn` | `user.value = data; isLoggedIn.value = true` |
| 14 | **Vue** | `app.mount('#app')` | 挂载应用，渲染 AppLayout → router-view → Learning 页面 |
| 15 | **Learning.vue** | `onMounted` 执行 | 调用 `fetchPlanData(today)` 加载学习计划数据 |

### 场景 3b：未登录 — 访问受保护页面

#### 步骤分解

| 步骤 | 角色 | 动作 | 详细说明 |
|------|------|------|---------|
| 1 | **浏览器** | 输入 `/learn` URL | 导航到受保护页面 |
| 2 | **Vue Router** | `beforeEach` 导航守卫 | `to.name !== 'Login' && !getToken()` → Token 为 null |
| 3 | **Vue Router** | 重定向到登录页 | `return { name: 'Login' }` |
| 4 | **浏览器** | 渲染 Login 页面 | 用户看到登录/注册界面 |

### ASCII 时序图

```
┌───────────┐    ┌──────────┐    ┌──────────┐    ┌───────────┐    ┌───────────┐    ┌────────────┐
│  Browser  │    │  Vue     │    │  Router  │    │  http.js  │    │  Vite     │    │  Backend   │
│           │    │  main.js │    │  Guard   │    │ (fetch)   │    │  Proxy    │    │(SpringBoot)│
└─────┬─────┘    └────┬─────┘    └────┬─────┘    └─────┬─────┘    └─────┬─────┘    └──────┬─────┘
      │               │               │               │               │               │
      │ 场景3a: 已登录访问 /learn      │               │               │               │
      │──────────────>│               │               │               │               │
      │               │               │               │               │               │
      │               │ beforeEach()  │               │               │               │
      │               │──────────────>│               │               │               │
      │               │               │               │               │               │
      │               │ 检查getToken()→有Token         │               │               │
      │               │               │               │               │               │
      │               │ 放行(next=true)                │               │               │
      │               │<──────────────│               │               │               │
      │               │               │               │               │               │
      │               │ 读取localStorage('auth_token') │               │               │
      │               │ Token存在→fetchProfile()       │               │               │
      │               │──────────────>│──────────────>│               │               │
      │               │               │               │               │               │
      │               │               │ GET /api/user/profile        │               │
      │               │               │ (Authorization: Bearer xxx)  │               │
      │               │               │──────────────>│──────────────>│               │
      │               │               │               │               │               │
      │               │               │               │ JwtAuthFilter验证Token          │
      │               │               │               │ SecurityContext设置认证          │
      │               │               │               │<──────────────│               │
      │               │               │               │               │               │
      │               │               │               │ ←返回ApiResponse(profile)     │
      │               │               │               │<──────────────│               │
      │               │               │               │               │               │
      │               │ user.value=profile            │               │               │
      │               │ isLoggedIn=true                │               │               │
      │               │               │               │               │               │
      │               │ app.mount('#app')              │               │               │
      │<──────────────│               │               │               │               │
      │               │               │               │               │               │
      │ 场景3b: 未登录访问 /learn      │               │               │               │
      │──────────────>│               │               │               │               │
      │               │               │               │               │               │
      │               │ beforeEach()  │               │               │               │
      │               │──────────────>│               │               │               │
      │               │               │               │               │               │
      │               │ 检查getToken()→null            │               │               │
      │               │               │               │               │               │
      │               │ return {name:'Login'}          │               │               │
      │               │<──────────────│               │               │               │
      │               │               │               │               │               │
      │               │ 渲染Login页面  │               │               │               │
      │<──────────────│               │               │               │               │
```

### 对应代码

**router/index.js** — 导航守卫：
```js
// 第28-32行
router.beforeEach((to) => {
  if (to.name !== 'Login' && !getToken()) {
    return { name: 'Login' }
  }
})
```

**main.js** — 启动时恢复登录态：
```js
// 第8-15行
const token = localStorage.getItem('auth_token')
if (token) {
  const { useUserStore } = await import('@/stores/user')
  const userStore = useUserStore()
  userStore.fetchProfile()
}
app.mount('#app')
```

**user store** — fetchProfile：
```js
// 第36-43行
async function fetchProfile() {
  try {
    const data = await userApi.getProfile()
    user.value = data
    isLoggedIn.value = true
  } catch {
    removeToken()
    user.value = null
    isLoggedIn.value = false
  }
}
```

**AppLayout.vue** — onMounted 中重复 fetchProfile（兜底）：
```js
// 第43-45行
onMounted(() => {
  userStore.fetchProfile()
})
```

---

## 场景四：已认证用户发起 API 请求（以学习页面为例）

### 流程说明

用户登录后进入 `/learn` 学习页面，页面 `onMounted` 时请求每日学习计划数据。这是系统中最典型的请求场景。

### 步骤分解

| 步骤 | 角色 | 动作 | 详细说明 |
|------|------|------|---------|
| 1 | **浏览器 (Learning.vue)** | `onMounted` 执行 | `fetchPlanData(today)` |
| 2 | **浏览器 (Learning.vue)** | 并行请求 | `Promise.all([planStore.fetchDailyDates(30), planStore.fetchDailyWords(date)])` |
| 3 | **浏览器 (dailyPlan store)** | 调用 API | `planApi.getDailyDates(limit)` 和 `planApi.getDailyWords(date)` |
| 4 | **浏览器 (api/index.js)** | 调用 api.get | `api.get('/plans/daily/dates', { limit: 30 })` |
| 5 | **浏览器 (http.js)** | 读取 Token | `getToken()` → `localStorage.getItem('auth_token')` |
| 6 | **浏览器 (http.js)** | 注入 Authorization 头 | `headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer <token>' }` |
| 7 | **浏览器 (http.js)** | 构造 URL | `/api/plans/daily/dates?limit=30` (过滤掉 null/undefined 参数) |
| 8 | **浏览器 (http.js)** | 发送 fetch 请求 | `fetch('/api/plans/daily/dates?limit=30', { headers })` |
| 9 | **Vite DevServer** | 代理转发 | `vite.config.js`: `/api` → `http://localhost:8080` |
| 10 | **后端 (CorsFilter)** | CORS 放行 | 允许所有 origin，添加 CORS 响应头 |
| 11 | **后端 (SecurityConfig)** | 请求匹配 | 非 `/api/auth/**`、非 `GET /api/badges`、非 `/api/admin/**` → 需要认证 |
| 12 | **后端 (JwtAuthFilter)** | 提取 Token | 读取 `Authorization` 头，去掉 `Bearer ` 前缀 |
| 13 | **后端 (JwtUtil)** | 校验 Token | `parseToken(token)` → 验证签名 + 过期时间 |
| 14 | **后端 (JwtAuthFilter)** | 解析 Claims | 提取 `subject` (userId) 和 `role` |
| 15 | **后端 (JwtAuthFilter)** | 设置 SecurityContext | `UsernamePasswordAuthenticationToken(userId, null, [ROLE_USER])` → `SecurityContextHolder.getContext().setAuthentication(auth)` |
| 16 | **后端 (JwtAuthFilter)** | 放行到 Controller | `filterChain.doFilter(request, response)` |
| 17 | **后端 (PlanController)** | 获取当前用户 ID | `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` → userId 字符串 |
| 18 | **后端 (PlanService)** | 处理业务逻辑 | 查询数据库、统计、组装响应 |
| 19 | **后端 (PlanController)** | 包装响应 | `ApiResponse.success(planDatesResponse)` → `{ code: 200, data: { dates: [...] }, message: "success" }` |
| 20 | **浏览器 (http.js)** | 解析 JSON | `res.json()` → 检查 `res.ok` → 返回 `body.data` |
| 21 | **浏览器 (dailyPlan store)** | 处理数据 | `dailyDates.value = res.dates.map(d => d.date)` / `dailyWords.value = res.words` |
| 22 | **Vue (Learning.vue)** | 响应式更新 | `loading = false`，模板重新渲染，显示单词卡片 |

### ASCII 时序图

```
┌───────────┐    ┌──────────────┐    ┌──────────┐    ┌───────────┐    ┌───────────┐    ┌──────────────┐    ┌──────────┐
│ Learning  │    │ dailyPlan    │    │  http.js │    │  Vite     │    │  Backend  │    │  Filters &   │    │  MySQL   │
│ .vue      │    │  Store       │    │ (fetch)  │    │  Proxy    │    │Controller │    │  Security    │    │          │
└─────┬─────┘    └──────┬───────┘    └─────┬─────┘    └─────┬─────┘    └─────┬─────┘    └──────┬───────┘    └────┬─────┘
      │                 │                 │               │               │               │               │
      │ 1.onMounted()   │                 │               │               │               │               │
      │─────────────────>                 │               │               │               │               │
      │                 │                 │               │               │               │               │
      │ 2.fetchPlanData(date)             │               │               │               │               │
      │ loading=true     │                 │               │               │               │               │
      │                 │                 │               │               │               │               │
      │ 3.Promise.all([                   │               │               │               │               │
      │   fetchDailyDates(30),            │               │               │               │               │
      │   fetchDailyWords(date)])         │               │               │               │               │
      │                 │                 │               │               │               │               │
      │                 │ 4.api.get()     │               │               │               │               │
      │                 │────────────────>│               │               │               │               │
      │                 │                 │               │               │               │               │
      │                 │                 │ 5.getToken()  │               │               │               │
      │                 │                 │ localStorage  │               │               │               │
      │                 │                 │               │               │               │               │
      │                 │                 │ 6.构造Header  │               │               │               │
      │                 │                 │ Authorization: Bearer xxx      │               │               │
      │                 │                 │               │               │               │               │
      │                 │                 │ 7.fetch('/api/plans/daily/dates?limit=30')        │               │
      │                 │                 │──────────────>│               │               │               │
      │                 │                 │               │               │               │               │
      │                 │                 │               │ 8.代理转发    │               │               │
      │                 │                 │               │──────────────>│               │               │
      │                 │                 │               │               │               │               │
      │                 │                 │               │               │ 9.CorsFilter  │               │
      │                 │                 │               │               │   放行        │               │
      │                 │                 │               │               │               │               │
      │                 │                 │               │               │10.JwtAuthFilter│              │
      │                 │                 │               │               │   提取Token    │               │
      │                 │                 │               │               │──────────────>│               │
      │                 │                 │               │               │               │               │
      │                 │                 │               │               │11.JwtUtil     │               │
      │                 │                 │               │               │   parseToken   │               │
      │                 │                 │               │               │   验证签名+过期 │               │
      │                 │                 │               │               │<──────────────│               │
      │                 │                 │               │               │               │               │
      │                 │                 │               │               │12.设置SecurityContext          │
      │                 │                 │               │               │   auth=new UsernamePasswordAuth │
      │                 │                 │               │               │   (userId, null, [ROLE_USER])   │
      │                 │                 │               │               │               │               │
      │                 │                 │               │               │13.filterChain.doFilter()        │
      │                 │                 │               │               │──────────────>│               │
      │                 │                 │               │               │               │               │
      │                 │                 │               │               │14.getPrincipal()  → userId      │
      │                 │                 │               │               │15.PlanService   │               │
      │                 │                 │               │               │   查询数据库    │               │
      │                 │                 │               │               │──────────────>│──────────────>│
      │                 │                 │               │               │<──────────────│<──────────────│
      │                 │                 │               │               │               │               │
      │                 │                 │               │               │16.ApiResponse.success(data)     │
      │                 │                 │               │<──────────────│               │               │
      │                 │                 │               │               │               │               │
      │                 │                 │ 17.res.json()→body.data       │               │               │
      │                 │                 │<──────────────│               │               │               │
      │                 │                 │               │               │               │               │
      │                 │ 18.提取数据      │               │               │               │               │
      │                 │ dailyDates=res.dates.map(d=>d.date)            │               │               │
      │                 │ dailyWords=res.words                           │               │               │
      │                 │<────────────────│               │               │               │               │
      │                 │                 │               │               │               │               │
      │ 19.loading=false，模板更新         │               │               │               │               │
      │<────────────────│                 │               │               │               │               │
```

### 对应代码

**Learning.vue** — onMounted 及请求：
```js
// 第150-165行
async function fetchPlanData(date) {
  loading.value = true
  try {
    await Promise.all([
      planStore.fetchDailyDates(30),
      planStore.fetchDailyWords(date),
    ])
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  selectedDate.value = today.value
  await fetchPlanData(selectedDate.value)
})
```

**dailyPlan store (stores/dailyPlan.js)** — API 调用及数据提取：
```js
// 第30-38行
async function fetchDailyWords(date) {
  const res = await planApi.getDailyWords(date)
  dailyWords.value = res.words || []
}

async function fetchDailyDates(limit) {
  const res = await planApi.getDailyDates(limit)
  dailyDates.value = (res.dates || []).map(d => d.date)
}
```

**http.js** — Token 注入及响应解析：
```js
// 第17-38行
async function request(url, options = {}) {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
  const res = await fetch(`${BASE}${url}`, { ...options, headers })
  // 401处理（见场景五）
  const body = await res.json()
  if (!res.ok) throw new Error(body.message || `HTTP ${res.status}`)
  return body.data
}
```

**JwtAuthFilter.java** — JWT 过滤器：
```java
// 全部代码
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        // 1. 无Authorization头或非Bearer格式 → 放行（后续SecurityConfig会拒绝）
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 提取Token
        String token = authHeader.substring(7);

        // 3. 校验Token（签名+过期）
        if (!jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. 从Claims中提取userId和role
        String userId = jwtUtil.getUserId(token);
        var claims = jwtUtil.parseToken(token);
        String role = claims.get("role", String.class);

        // 5. 创建认证对象并设置到SecurityContext
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 6. 放行到Controller
        filterChain.doFilter(request, response);
    }
}
```

**SecurityConfig.java** — 安全配置 & 过滤器注册：
```java
// 第17-28行
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()    // 公开
            .requestMatchers("/api/admin/**").hasRole("ADMIN")  // 仅管理员
            .requestMatchers(HttpMethod.GET, "/api/badges").permitAll()  // 公开GET
            .anyRequest().authenticated()                  // 其他都需要认证
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

---

## 场景五：Token 过期 / 401 处理

### 流程说明

用户的 JWT Token 已过期（超过 24h），或 Token 被篡改，后端返回 401，前端自动清理 Token 并跳转登录页。

### 步骤分解

| 步骤 | 角色 | 动作 | 详细说明 |
|------|------|------|---------|
| 1 | **浏览器 (http.js)** | 发送带过期 Token 的请求 | fetch 携带 `Authorization: Bearer <expired_token>` |
| 2-10 | **Vite Proxy → Backend Filters** | 同上 | 请求到达后端 |
| 11 | **后端 (JwtAuthFilter)** | 提取 Token | 读取 `Authorization` 头 |
| 12 | **后端 (JwtUtil)** | 校验 Token | `parseToken(token)` → 抛出 `JwtException`（签名不匹配或过期） |
| 13 | **后端 (JwtAuthFilter)** | validateToken 返回 false | 直接 `filterChain.doFilter()` 放行，不设置 SecurityContext |
| 14 | **后端 (SecurityConfig)** | 检查认证状态 | SecurityContext 为空 → 该请求需要认证 |
| 15 | **后端 (Spring Security)** | 返回 401 | `AuthenticationEntryPoint` 发送 401 Unauthorized |
| 16 | **浏览器 (http.js)** | 检查响应状态 | `res.status === 401` |
| 17 | **浏览器 (http.js)** | 清除 Token | `removeToken()` → `localStorage.removeItem('auth_token')` |
| 18 | **浏览器 (http.js)** | 硬跳转到登录页 | `window.location.href = '/login'`（非 Vue Router 导航，全页刷新） |
| 19 | **浏览器 (http.js)** | 抛出错误 | `throw new Error('Unauthorized')` |

### ASCII 时序图

```
┌───────────┐    ┌──────────────┐    ┌───────────┐    ┌───────────┐    ┌────────────┐    ┌──────────┐
│  Vue      │    │  http.js     │    │  Vite     │    │ JwtAuth   │    │  Spring    │    │  JwtUtil │
│  Component│    │  (fetch)     │    │  Proxy    │    │  Filter   │    │  Security  │    │          │
└─────┬─────┘    └──────┬───────┘    └─────┬─────┘    └─────┬─────┘    └──────┬─────┘    └────┬─────┘
      │                 │                 │               │               │               │
      │ API请求(过期Token)                 │               │               │               │
      │─────────────────>                 │               │               │               │
      │                 │                 │               │               │               │
      │                 │ 转发请求        │               │               │               │
      │                 │────────────────>│               │               │               │
      │                 │                 │──────────────>│               │               │
      │                 │                 │               │               │               │
      │                 │                 │               │ 提取Token     │               │
      │                 │                 │               │ parseToken()  │               │
      │                 │                 │               │──────────────>│               │
      │                 │                 │               │               │               │
      │                 │                 │               │ JwtException  │               │
      │                 │                 │               │<──────────────│               │
      │                 │                 │               │               │               │
      │                 │                 │               │ validateToken()=false        │
      │                 │                 │               │ 不设置SecurityContext         │
      │                 │                 │               │               │               │
      │                 │                 │               │ filterChain.doFilter()        │
      │                 │                 │               │──────────────>│               │
      │                 │                 │               │               │               │
      │                 │                 │               │               │ SecurityContext为空           │
      │                 │                 │               │               │ → 需要认证但未认证            │
      │                 │                 │               │               │ → 返回 401                    │
      │                 │                 │               │               │               │
      │                 │                 │  401响应      │               │               │
      │                 │                 │<──────────────│               │               │
      │                 │<────────────────│               │               │               │
      │                 │                 │               │               │               │
      │                 │ res.status===401                │               │               │
      │                 │ removeToken()   │               │               │               │
      │                 │ localStorage.removeItem         │               │               │
      │                 │                 │               │               │               │
      │                 │ window.location.href='/login'   │               │               │
      │                 │ (整页刷新，非SPA跳转)             │               │               │
      │                 │                 │               │               │               │
```

### 对应代码

**http.js** — 401 处理：
```js
// 第27-31行
const res = await fetch(`${BASE}${url}`, { ...options, headers })

if (res.status === 401) {
  removeToken()
  window.location.href = '/login'
  throw new Error('Unauthorized')
}
```

注意：401 检查在 `res.json()` 之前执行，因此即使是无效 JSON 的 401 响应，也会被正确处理。

**JwtAuthFilter.java** — Token 校验失败处理：
```java
// 第29-31行
if (!jwtUtil.validateToken(token)) {
    filterChain.doFilter(request, response);  // ← 不放认证信息，Spring Security 后续返回 401
    return;
}
```

**JwtUtil.java** — Token 校验：
```java
// 第36-41行
public boolean validateToken(String token) {
    try {
        parseToken(token);
        return true;
    } catch (JwtException | IllegalArgumentException e) {
        return false;  // ← 过期/签名错误都会导致返回 false
    }
}
```

---

## 场景六：用户退出

### 流程说明

用户点击导航栏的「退出」按钮触发的登出流程。

### 步骤分解

| 步骤 | 角色 | 动作 | 详细说明 |
|------|------|------|---------|
| 1 | **浏览器 (AppLayout.vue)** | 用户点击「退出」 | `logout()` 函数被调用 |
| 2 | **浏览器 (AppLayout.vue)** | 调用 `userStore.logout()` | 清除 Pinia store 中的用户状态 |
| 3 | **浏览器 (user store)** | 清除 Token | `removeToken()` → `localStorage.removeItem('auth_token')` |
| 4 | **浏览器 (user store)** | 重置所有状态 | `user = null`, `settings = {}`, `isLoggedIn = false` |
| 5 | **浏览器 (AppLayout.vue)** | 路由跳转 | `router.push('/login')` — Vue Router 导航到登录页 |
| 6 | **Vue Router** | `beforeEach` 导航守卫 | `to.name === 'Login'` → 不检查 Token，放行 |
| 7 | **浏览器** | 渲染 Login 页面 | 用户看到登录界面 |

### ASCII 时序图

```
┌───────────┐    ┌──────────────┐    ┌──────────┐
│ AppLayout │    │  User Store  │    │ localStorage
│   .vue    │    │  (Pinia)     │    │          │
└─────┬─────┘    └──────┬───────┘    └────┬─────┘
      │                 │               │
      │ 1.点击「退出」    │               │
      │                 │               │
      │ 2.logout()      │               │
      │────────────────>│               │
      │                 │               │
      │                 │ 3.removeToken()│
      │                 │──────────────>│
      │                 │               │
      │                 │ 4.重置状态     │
      │                 │ user=null     │
      │                 │ isLoggedIn=false│
      │                 │               │
      │ 5.router.push('/login')         │
      │<────────────────│               │
      │                 │               │
      │ 6.渲染Login页面  │               │
      │                 │               │
```

### 对应代码

**AppLayout.vue** — 退出按钮：
```js
// 第53-56行
function logout() {
  userStore.logout()
  router.push('/login')
}
```

**user store (stores/user.js)** — logout 方法：
```js
// 第31-38行
function logout() {
  removeToken()
  user.value = null
  settings.value = {}
  activity.value = []
  badges.value = { list: [], earnedCount: 0, totalCount: 0 }
  isLoggedIn.value = false
}
```

---

## 场景七：无需认证的公开请求（获取 Badge 列表）

### 流程说明

Badge 列表接口 `GET /api/badges` 在 `SecurityConfig` 中配置为 `permitAll`，无需 Token 即可访问。这是系统中唯一的公开 GET 接口。

### 步骤分解

| 步骤 | 角色 | 动作 | 详细说明 |
|------|------|------|---------|
| 1 | **浏览器 (user store)** | 调用 `badgeApi.getList()` | `api.get('/badges')` |
| 2 | **浏览器 (http.js)** | 无 Token 或携带 Token | 不强制需要 Token |
| 3 | **浏览器 (http.js)** | 发送请求 | `fetch('/api/badges')` |
| 4-6 | **Vite → Backend** | 代理、CORS | 同前 |
| 7 | **后端 (SecurityConfig)** | 检查路由 | `GET /api/badges` → `.permitAll()` |
| 8 | **后端 (JwtAuthFilter)** | 即使无 Token 也放行 | 有 Token 则验证，无 Token 也继续 |
| 9 | **后端 (BadgeController)** | 返回 Badge 列表 | `ApiResponse.success(badges)` |
| 10 | **浏览器** | 解析响应 | `body.data` → badge 数组 |

### 对应代码

**SecurityConfig.java** — 公开路由声明：
```java
// 第22行
.requestMatchers(HttpMethod.GET, "/api/badges").permitAll()
```

---

## 全局中间件/过滤器总览

### 前端请求生命周期（从组件到服务器）

```
Vue Component
      │
      ▼
Pinia Store (api/index.js → 调用 api.get/post/put/delete)
      │
      ▼
http.js (api对象)
  │  ├─ getToken() → 从 localStorage 读取 auth_token
  │  ├─ 组装 headers: { Content-Type, Authorization: Bearer <token> }
  │  ├─ 构造 URL: BASE + path + ?params
  │  ├─ fetch() 发出请求
  │  ├─ res.status === 401? → removeToken() + window.location.href='/login'
  │  ├─ res.json() 解析
  │  ├─ !res.ok? → throw Error(body.message)
  │  └─ return body.data
  │
      ▼
Vite DevServer (开发环境)
  │  proxy: { '/api': 'http://localhost:8080' }
  │
      ▼
```

### 后端请求处理链

```
Vite Proxy → localhost:8080
      │
      ▼
CorsFilter (CorsConfig.java)
  │  允许所有 origin/header/method, 支持 credentials
  │
      ▼
Spring Security Filter Chain (SecurityConfig.java)
  │  ├─ SecurityContextPersistenceFilter (STATELESS → 不创建 session)
  │  ├─ ... 其他默认过滤器 ...
  │  ├─ JwtAuthFilter (在 UsernamePasswordAuthenticationFilter 之前)
  │  │    ├─ 读取 Authorization: Bearer <token>
  │  │    ├─ JwtUtil.validateToken(token) — 验证签名+过期
  │  │    ├─ 解析 userId + role
  │  │    ├─ 设置 SecurityContextHolder
  │  │    └─ filterChain.doFilter()
  │  ├─ 根据 URL 匹配 SecurityConfig 规则:
  │  │    ├─ /api/auth/** → permitAll
  │  │    ├─ GET /api/badges → permitAll
  │  │    ├─ /api/admin/** → hasRole(ADMIN)
  │  │    └─ 其他 → authenticated
  │  └─ 认证失败 → 401 Unauthorized
  │
      ▼
DispatcherServlet → Controller
  │
      ▼
GlobalExceptionHandler (异常时)
  │  ├─ ResourceNotFoundException → 404
  │  ├─ BusinessException → 对应 HTTP 状态码
  │  ├─ MethodArgumentNotValidException → 400
  │  ├─ DataIntegrityViolationException → 409
  │  └─ Exception → 500
  │
      ▼
Controller → Service → Repository → MySQL
  │
      ▼
返回 ApiResponse<T> → JSON 序列化 → 响应浏览器
```

### 认证状态流转图

```
┌──────────────────────────────────────────────────────────────────┐
│                       认证状态流转                                │
└──────────────────────────────────────────────────────────────────┘

                  ┌──────────────┐
                  │   未登录     │
                  │ 无 Token    │
                  └──────┬──────┘
                         │
              ┌──────────┴──────────┐
              │                     │
              ▼                     ▼
      ┌──────────────┐    ┌──────────────────┐
      │ 访问 /login  │    │ 访问其他页面      │
      │ 正常渲染     │    │ beforeEach守卫检测 │
      └──────────────┘    │ getToken() = null │
                          └────────┬─────────┘
                                   │
                                   ▼
                          ┌──────────────────┐
                          │ 重定向到 /login  │
                          └──────────────────┘
                                   │
                         ┌─────────┴─────────┐
                         │  登录/注册成功     │
                         │  setToken()        │
                         │  router.push('/')  │
                         └─────────┬─────────┘
                                   │
                                   ▼
                          ┌──────────────────┐
                          │    已登录         │
                          │  Token 在 localStorage │
                          │  isLoggedIn = true │
                          └─────────┬──────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
                    ▼                             ▼
          ┌──────────────────┐          ┌──────────────────┐
          │ 正常使用         │          │ Token 过期       │
          │ API请求携带Token │          │ API 返回 401     │
          └──────────────────┘          │ http.js 处理     │
                    │                   │ removeToken()    │
                    │                   │ 跳转 /login      │
                    │                   └────────┬─────────┘
                    │                            │
                    └──────────────┬─────────────┘
                                   ▼
                          ┌──────────────────┐
                          │ 用户主动退出      │
                          │ logout() →       │
                          │ removeToken()     │
                          │ router.push('/login')│
                          └─────────┬────────┘
                                    │
                                    ▼
                           ┌──────────────┐
                           │   未登录     │
                           └──────────────┘
```

---

## 附录：完整文件索引

| 层 | 文件 | 作用 |
|----|------|------|
| 前端 | `frontend/src/main.js` | 应用入口，初始化 Pinia/Router，恢复 Token |
| 前端 | `frontend/src/router/index.js` | 路由守卫，`beforeEach` 检查 Token |
| 前端 | `frontend/src/api/http.js` | HTTP 客户端，Token 注入、401 处理 |
| 前端 | `frontend/src/api/index.js` | 所有 API 端点定义 |
| 前端 | `frontend/src/stores/user.js` | 用户认证状态管理 |
| 前端 | `frontend/src/stores/dailyPlan.js` | 学习计划状态管理 |
| 前端 | `frontend/src/views/Login.vue` | 登录/注册页面 |
| 前端 | `frontend/src/views/Learning.vue` | 学习页面 |
| 前端 | `frontend/src/components/layout/AppLayout.vue` | 应用布局，顶栏导航 |
| 前端 | `frontend/vite.config.js` | Vite 配置，API 代理 |
| 后端 | `config/SecurityConfig.java` | Spring Security 安全配置 |
| 后端 | `config/JwtAuthFilter.java` | JWT 认证过滤器 |
| 后端 | `config/CorsConfig.java` | CORS 跨域配置 |
| 后端 | `util/JwtUtil.java` | JWT 生成/解析/校验工具 |
| 后端 | `controller/AuthController.java` | 认证控制器 |
| 后端 | `controller/PlanController.java` | 学习计划控制器 |
| 后端 | `service/AuthService.java` | 认证业务逻辑 |
| 后端 | `service/PlanService.java` | 学习计划业务逻辑 |
| 后端 | `exception/GlobalExceptionHandler.java` | 全局异常处理 |
| 后端 | `dto/response/ApiResponse.java` | 统一 API 响应体 |
| 后端 | `dto/response/LoginResponse.java` | 登录响应 DTO |
| 后端 | `entity/User.java` | 用户实体 |
| 后端 | `resources/application.yml` | 应用配置（JWT secret、数据源） |
