# MCP 与 Skill 安装使用完整教程

> 覆盖 6 个常用 MCP 服务 + 4 个实用 Skill，从零到可用

---

## 目录

- [MCP 与 Skill 安装使用完整教程](#mcp-与-skill-安装使用完整教程)
  - [目录](#目录)
  - [基础知识](#基础知识)
    - [什么是 MCP？](#什么是-mcp)
    - [什么是 Skill？](#什么是-skill)
    - [opencode 配置结构](#opencode-配置结构)
  - [第一部分：MCP 服务](#第一部分mcp-服务)
    - [1.1 GitHub MCP](#11-github-mcp)
    - [1.2 Filesystem MCP](#12-filesystem-mcp)
    - [1.3 Browser MCP](#13-browser-mcp)
    - [1.4 Docker MCP](#14-docker-mcp)
    - [1.5 MySQL MCP](#15-mysql-mcp)
    - [1.6 Linux / Terminal MCP](#16-linux--terminal-mcp)
  - [第二部分：Skill 技能](#第二部分skill-技能)
    - [2.1 Skill 文件结构](#21-skill-文件结构)
    - [2.2 命名规则](#22-命名规则)
    - [2.3 系统设计 Skill](#23-系统设计-skill)
    - [2.4 Fullstack Skill](#24-fullstack-skill)
    - [2.5 Code Review Skill](#25-code-review-skill)
    - [2.6 DevOps Skill](#26-devops-skill)
  - [第三部分：综合配置示例](#第三部分综合配置示例)
  - [第四部分：调试与排错](#第四部分调试与排错)
    - [4.1 MCP 服务排查流程](#41-mcp-服务排查流程)
    - [4.2 Skill 排查流程](#42-skill-排查流程)
  - [第五部分：常见问题](#第五部分常见问题)
    - [5.1 启动超时](#51-q1-启动报错-operation-timed-out)
    - [5.2 缺少 type 属性](#52-q2-报错-missing-property-type)
    - [5.3 mcpServers 不允许](#53-q3-报错-property-mcpservers-is-not-allowed)
    - [5.4 type 值错误](#54-q4-报错-value-is-not-accepted-valid-values-local)
    - [5.5 Skill 加载问题](#55-q5-skill-加载不出来)
    - [5.6 上下文爆炸](#56-q6-mcp-服务太多上下文爆炸)
  - [第六部分：实战排错记录](#第六部分实战排错记录)
    - [6.0 通用排查技能](#60-通用排查技能)
    - [6.1 cambridge-dict MCP 修复实录](#61-cambridge-dict-mcp-修复实录)
    - [6.2 github MCP 修复实录](#62-github-mcp-修复实录)
    - [6.3 filesystem MCP 修复实录](#63-filesystem-mcp-修复实录)
  - [速查表](#速查表)
    - [7.1 MCP 服务一览](#71-mcp-服务一览)
    - [7.2 Skill 一览](#72-skill-一览)

---

## 基础知识

### 什么是 MCP？

MCP = Model Context Protocol，是 **AI 助手的"外挂插件"系统**。

- AI 原本只能"看"和"说"，装了 MCP 之后能**查 GitHub、读写文件、操作浏览器、管理 Docker、查数据库、执行 Shell 命令**
- 每个 MCP 服务提供一组"工具"（tools），AI 在需要时会自动调用
- 配置在 `opencode.json` 的 `"mcp"` 字段下

### 什么是 Skill？

Skill = 技能，是 **AI 助手的"专业能力说明书"**。

- Skill 不是代码，而是**一份 Markdown 文档**（`SKILL.md`）
- 文档里写清楚：这个 AI 应该用什么思维方式、遵循什么规范、输出什么格式
- AI 在遇到匹配场景时会自动加载对应的 Skill
- 配置在 `.opencode/skills/<name>/SKILL.md`

### opencode 配置结构

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "服务名1": {
      "type": "local",
      "command": ["npx", "-y", "包名"]
    },
    "服务名2": {
      "type": "remote",
      "url": "https://example.com/mcp"
    }
  }
}
```

| 配置项 | 说明 |
|--------|------|
| `"mcp"` | MCP 服务配置的根字段（不是 `"mcpServers"`） |
| `"服务名"` | 自定义名称，你用这个名字来引用它 |
| `"type"` | `"local"` 本地进程 / `"remote"` 远程 HTTP |
| `"command"` | 启动命令，必须是数组（不是字符串 + args） |
| `"enabled"` | 可选，设为 `false` 可临时禁用 |
| `"environment"` | 可选，传入环境变量 |

---

## 第一部分：MCP 服务

---

### 1.1 GitHub MCP

让 AI 能直接操作 GitHub——查 Issue、提 PR、审代码、搜仓库。

#### 安装

```bash
npm install -g @modelcontextprotocol/server-github
```

#### 配置

```json
{
  "mcp": {
    "github": {
      "type": "local",
      "command": ["npx", "-y", "@modelcontextprotocol/server-github"],
      "environment": {
        "GITHUB_TOKEN": "xx"
      }
    }
  }
}
```

#### 获取 GitHub Token

1. 打开 https://github.com/settings/tokens
2. 点击 **Generate new token (classic)**
3. 勾选权限：`repo`（私有仓库）、`issues`、`pull_requests`
4. 复制 token，粘贴到配置的 `GITHUB_TOKEN` 处

> **安全建议：** 不要直接把 token 写在配置文件里。用环境变量引用：
> ```json
> "GITHUB_TOKEN": "{env:GITHUB_TOKEN}"
> ```
> 然后执行 `export GITHUB_TOKEN=你的token` 或在 `.bashrc` 中设置。

#### 可用工具

| 工具名 | 功能 |
|--------|------|
| `github_create_issue` | 创建 Issue |
| `github_list_issues` | 列出 Issue |
| `github_search_code` | 搜索代码 |
| `github_search_repositories` | 搜索仓库 |
| `github_create_pull_request` | 创建 PR |
| `github_get_pull_request` | 查看 PR 详情 |
| `github_list_pull_requests` | 列出 PR |
| `github_add_issue_comment` | 添加评论 |

#### 使用示例

```
用 github 工具帮我列出当前仓库的 open issues
```

> ⚠️ GitHub MCP 每次调用会消耗较多 token（上下文），建议只在需要时启用。

---

### 1.2 Filesystem MCP

让 AI 能直接读写文件、目录遍历、搜索替换。**慎用！权限很大。**

#### 安装

```bash
npm install -g @modelcontextprotocol/server-filesystem
```

#### 配置

```json
{
  "mcp": {
    "filesystem": {
      "type": "local",
      "command": [
        "npx", "-y", "@modelcontextprotocol/server-filesystem",
        "/mnt/d/opencode/study",   # 允许访问的目录
        "/tmp"                      # 可以加多个目录
      ]
    }
  }
}
```

> **安全提示：** 只有命令参数中列出的目录才允许访问。这是权限控制手段。

#### 可用工具

| 工具名 | 功能 |
|--------|------|
| `filesystem_read_file` | 读取文件 |
| `filesystem_write_file` | 写入文件 |
| `filesystem_edit_file` | 编辑文件 |
| `filesystem_create_directory` | 创建目录 |
| `filesystem_list_directory` | 列出目录 |
| `filesystem_search` | 搜索文件内容 |
| `filesystem_get_file_info` | 获取文件信息 |
| `filesystem_move_file` | 移动文件 |
| `filesystem_copy_file` | 复制文件 |
| `filesystem_delete_file` | 删除文件 |

#### 使用示例

```
用 filesystem 工具帮我搜索 src/ 目录下所有包含 "TODO" 的文件
```

---

### 1.3 Browser MCP

让 AI 能控制浏览器——截图、点击、填表单、抓取页面内容。

#### 方案一：Puppeteer MCP（经典方案）

```bash
npm install -g @modelcontextprotocol/server-puppeteer
```

```json
{
  "mcp": {
    "browser": {
      "type": "local",
      "command": ["npx", "-y", "@modelcontextprotocol/server-puppeteer"]
    }
  }
}
```

#### 方案二：Chrome DevTools MCP（Google 官方方案）

```bash
npm install -g chrome-devtools-mcp
```

```json
{
  "mcp": {
    "browser": {
      "type": "local",
      "command": ["npx", "-y", "chrome-devtools-mcp"],
      "environment": {
        "CHROME_PATH": "/usr/bin/google-chrome"
      }
    }
  }
}
```

> 在 WSL 中需要安装 Chrome/Chromium：
> ```bash
> sudo apt install chromium-browser
> which chromium-browser  # 获取路径
> ```

#### 可用工具（以 Puppeteer 为例）

| 工具名 | 功能 |
|--------|------|
| `browser_navigate` | 导航到 URL |
| `browser_screenshot` | 截图 |
| `browser_click` | 点击元素 |
| `browser_fill` | 填充表单 |
| `browser_select` | 选择下拉选项 |
| `browser_hover` | 悬停元素 |
| `browser_evaluate` | 执行 JavaScript |

#### 使用示例

```
用 browser 打开 https://example.com，截图保存到 /tmp/screenshot.png
```

---

### 1.4 Docker MCP

让 AI 能管理 Docker 容器、镜像、卷、Compose 服务。

#### 安装

```bash
npm install -g mcp-docker-server
```

#### 配置

```json
{
  "mcp": {
    "docker": {
      "type": "local",
      "command": ["npx", "-y", "mcp-docker-server"]
    }
  }
}
```

#### 可用工具

| 工具名 | 功能 |
|--------|------|
| `docker_list_containers` | 列出容器 |
| `docker_list_images` | 列出镜像 |
| `docker_start_container` | 启动容器 |
| `docker_stop_container` | 停止容器 |
| `docker_run_container` | 运行新容器 |
| `docker_remove_container` | 删除容器 |
| `docker_pull_image` | 拉取镜像 |
| `docker_list_volumes` | 列出卷 |
| `docker_compose_up` | 启动 Compose |
| `docker_compose_down` | 停止 Compose |
| `docker_logs` | 查看容器日志 |

#### 使用示例

```
用 docker 帮我列出所有运行中的容器，并查看 nginx 容器的日志
```

> **注意：** 需要当前用户有 docker 权限。如果没有，先执行：
> ```bash
> sudo usermod -aG docker $USER
> # 然后重新登录
> ```

---

### 1.5 MySQL MCP

让 AI 能连接 MySQL 数据库并执行查询。

#### 安装

```bash
npm install -g mcp-server-mysql
```

#### 配置

```json
{
  "mcp": {
    "mysql": {
      "type": "local",
      "command": ["npx", "-y", "mcp-server-mysql"],
      "environment": {
        "MYSQL_HOST": "localhost",
        "MYSQL_PORT": "3306",
        "MYSQL_USER": "root",
        "MYSQL_PASSWORD": "你的密码",
        "MYSQL_DATABASE": "你的数据库名"
      }
    }
  }
}
```

> **安全建议：** 使用环境变量引用，不要明文写密码：
> ```json
> "MYSQL_PASSWORD": "{env:MYSQL_PASSWORD}"
> ```

#### 备选方案

```bash
npm install -g @berthojoris/mcp-mysql-server
```

```json
{
  "mcp": {
    "mysql": {
      "type": "local",
      "command": ["npx", "-y", "@berthojoris/mcp-mysql-server"],
      "environment": {
        "MYSQL_CONNECTION_STRING": "mysql://user:password@localhost:3306/db"
      }
    }
  }
}
```

#### 可用工具

| 工具名 | 功能 |
|--------|------|
| `mysql_query` | 执行 SQL 查询 |
| `mysql_list_tables` | 列出所有表 |
| `mysql_describe_table` | 查看表结构 |
| `mysql_show_databases` | 列出数据库 |

#### 使用示例

```
用 mysql 帮我查询 users 表的前 10 条记录
```

---

### 1.6 Linux / Terminal MCP

让 AI 能直接执行 Shell 命令和/或与终端交互。

#### 方案一：Shell MCP（执行命令）

```bash
npm install -g shell-mcp-server
```

```json
{
  "mcp": {
    "terminal": {
      "type": "local",
      "command": ["npx", "-y", "shell-mcp-server"]
    }
  }
}
```

#### 方案二：Terminal MCP（交互式终端，零依赖）

```bash
npm install -g mcp-server-terminal
```

```json
{
  "mcp": {
    "terminal": {
      "type": "local",
      "command": ["npx", "-y", "mcp-server-terminal"]
    }
  }
}
```

> ⚠️ **安全警告：** 这两个 MCP 给了 AI 执行任意系统命令的能力。只在可信项目中启用。

#### 可用工具（以 shell-mcp-server 为例）

| 工具名 | 功能 |
|--------|------|
| `shell_execute` | 执行 Shell 命令 |
| `shell_execute_interactive` | 交互式执行 |
| `shell_get_working_directory` | 获取当前目录 |
| `shell_change_working_directory` | 切换目录 |
| `shell_read_output` | 读取输出 |

#### 使用示例

```
用 terminal 执行 df -h 查看磁盘空间使用情况
```

---

## 第二部分：Skill 技能

Skill 是 `SKILL.md` 文件，教 AI 如何思考和工作。

### 2.1 Skill 文件结构

```
.opencode/skills/<技能名>/SKILL.md
```

必须包含 YAML 头：

```markdown
---
name: 技能名
description: 一句话描述（1-1024 字符）
license: MIT            # 可选
compatibility: opencode # 可选
metadata:               # 可选
  key: value
---

## 技能内容

用 markdown 写清楚：
- 这个 AI 应该怎么思考
- 遵循什么规范
- 输出什么格式
- 有哪些工作流程
```

### 2.2 命名规则

- 1-64 字符
- 小写字母 + 数字 + 单连字符（`-`）
- 不能以 `-` 开头或结尾
- 不能有连续 `--`
- 正则：`^[a-z0-9]+(-[a-z0-9]+)*$`

---

### 2.3 系统设计 Skill

让 AI 以系统架构师身份思考，产出高质量的设计文档。

创建 `.opencode/skills/system-design/SKILL.md`：

```markdown
---
name: system-design
description: 系统架构设计技能——从需求到高可用架构的完整设计流程
license: MIT
compatibility: opencode
metadata:
  domain: architecture
  level: advanced
---

## 核心原则

1. **需求先行** — 先明确功能需求和非功能需求（QPS、延迟、可用性、一致性）
2. **估算驱动** — 做容量估算（DAU、存储量、带宽、QPS）
3. **从简到繁** — 先给出 MVP 架构，再逐层优化
4. **面面俱到** — 不只要功能设计，还要考虑：高可用、容灾、监控、安全、成本

## 输出格式

每次系统设计输出应包含：

### 1. 需求分析
- 功能需求
- 非功能需求（QPS 99p 延迟 SLA 可用性 数据一致性）
- 容量估算（存储 / 带宽 / 并发）

### 2. 数据模型
- 核心实体与关系
- 数据库选型理由（SQL vs NoSQL）

### 3. 高层架构
- 架构图（文字描述）
- 核心组件（LB / API Gateway / Service / Cache / DB / Queue / CDN）
- 数据流

### 4. 详细设计
- 核心 API 设计
- 数据库 Schema
- 关键算法或优化点

### 5. 纵深思考
- 高可用（故障转移、降级、限流、熔断）
- 扩展性（水平扩展、分片、CQRS）
- 安全性（认证、授权、数据加密）
- 监控（指标、日志、告警、分布式追踪）

## 思考框架

- 不要急于写方案，先用问题清单澄清需求
- 用数据说话（估算），不要凭感觉
- 每个方案都要说清 trade-off（取舍）
- 给出推荐方案及理由
```

#### 验证配置

```bash
ls .opencode/skills/system-design/SKILL.md
```

---

### 2.4 Fullstack Skill

让 AI 成为前后端全栈开发者，保持代码一致性和最佳实践。

创建 `.opencode/skills/fullstack/SKILL.md`：

````markdown
---
name: fullstack
description: 全栈开发技能——前后端统一的编码规范和架构模式
license: MIT
compatibility: opencode
metadata:
  domain: development
  level: intermediate
---

## 工作原则

1. **前后端一致** — API 命名风格、错误格式、日期格式等保持一致
2. **类型安全** — TypeScript 优先，所有 API 请求/响应都有类型定义
3. **渐进式构建** — 先核心功能可运行，再迭代优化
4. **可维护性优先** — 清晰的目录结构、模块划分、命名规范

## 通用规范

### Git 提交
- `feat:` 新功能
- `fix:` 修复
- `refactor:` 重构
- `docs:` 文档
- `test:` 测试
- `chore:` 构建/工具

### 代码风格
- TypeScript 严格模式
- ESLint + Prettier
- 单测覆盖核心逻辑（>80%）
- 组件/函数不超过 200 行

## 前端规范

### 技术栈
- React / Next.js（App Router）
- Tailwind CSS 或 Shadcn/ui
- React Query（服务端状态）
- Zustand（客户端状态）

### 目录结构
```
src/
  app/        # Next.js App Router pages
  components/ # 可复用组件
  hooks/      # 自定义 hooks
  lib/        # 工具函数
  api/        # API 客户端
  types/      # 类型定义
```

## 后端规范

### 技术栈
- Node.js / NestJS 或 Fastify
- Prisma（ORM）
- PostgreSQL（数据库）
- Redis（缓存）

### 目录结构
```
src/
  modules/    # 功能模块
  common/     # 通用工具
  config/     # 配置
  prisma/     # Schema + Migration
```

## API 设计规范

- RESTful 命名：`GET /users` `POST /users/:id`
- 统一响应格式：`{ code, data, message }`
- 分页：`{ list, total, page, pageSize }`
- 错误码体系：`BAD_REQUEST` / `UNAUTHORIZED` / `NOT_FOUND` / `INTERNAL_ERROR`
````

---

### 2.5 Code Review Skill

让 AI 以资深 Reviewer 的视角审查代码，关注质量、安全和性能。

创建 `.opencode/skills/code-review/SKILL.md`：

```markdown
---
name: code-review
description: 代码审查技能——系统性检查代码质量、安全、性能和可维护性
license: MIT
compatibility: opencode
metadata:
  domain: development
  level: advanced
---

## 审查流程

### 第一步：理解上下文
- PR/提交的目的是什么？
- 解决了什么问题？
- 影响范围有多大？

### 第二步：逐层审查

#### 1. 逻辑正确性
- 边界条件是否处理？（空值、越界、并发）
- 异常路径是否覆盖？
- 状态变更是否符合预期？

#### 2. 安全性
- SQL 注入？XSS？CSRF？
- 用户输入是否校验和转义？
- 权限检查是否到位？
- 敏感信息是否泄露？（日志、报错信息）

#### 3. 性能
- 是否有 N+1 查询？
- 是否有不必要的重复计算？
- 缓存策略是否合理？
- 大对象/大数据集是否分批处理？

#### 4. 可维护性
- 命名是否清晰表达意图？
- 函数/组件职责是否单一？
- 是否有重复代码可以抽象？
- 测试是否覆盖核心逻辑？

#### 5. 代码风格
- 是否符合项目现有风格？
- TypeScript 类型是否严格？
- 是否有死代码（注释掉的、未使用的）？

## 负面示例（要求改进）

以下情况应要求修改：

- ❌ 魔法数字/字符串未命名常量
- ❌ 500 行以上的函数
- ❌ 深层嵌套（3 层以上）
- ❌ 直接修改函数参数
- ❌ try-catch 吞掉异常
- ❌ 异步操作缺少 await

## 点评语气规范

- 用"建议"代替"你应该"
- 指出问题时**说明理由**（为什么不好）
- 给出改进的**代码示例**
- 区分"必须改"（blocker）和"建议改"（nit）
- 最后总结：变更的整体质量 + 主要风险点
```

---

### 2.6 DevOps Skill

让 AI 以 DevOps/SRE 视角工作，关注部署、监控、运维。

创建 `.opencode/skills/devops/SKILL.md`：

````markdown
---
name: devops
description: DevOps 与 SRE 技能——CI/CD、容器化、监控告警、基础设施即代码
license: MIT
compatibility: opencode
metadata:
  domain: operations
  level: advanced
---

## 核心理念

1. **不可变基础设施** — 不修改运行中的服务器，销毁重建
2. **一切皆代码** — 基础设施（IaC）、配置、流水线都版本控制
3. **可观测性优先** — 没有监控就不要上线
4. **渐进式发布** — 灰度、蓝绿、金丝雀

## CI/CD 规范

### Pipeline 结构
```
check (lint + typecheck + unit test)
  → build (Docker image)
    → integration test
      → staging deploy
        → e2e test
          → production rollout (canary)
```

### Docker 最佳实践
- 多阶段构建，减少镜像体积
- 使用 `slim` 或 `alpine` 基础镜像
- 不要以 root 运行容器
- 显式声明 `EXPOSE` 端口
- 使用 `.dockerignore`

```dockerfile
# 示例：Node.js 多阶段构建
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
USER node
EXPOSE 3000
CMD ["node", "dist/main.js"]
```

### Kubernetes 要点
- 资源限制（requests + limits）必须设置
- 存活探针 + 就绪探针
- 配置用 ConfigMap，密钥用 Secret
- PodDisruptionBudget 保证高可用
- HPA 自动扩缩容

## 监控体系

### 三大支柱
| 支柱 | 工具示例 | 关注点 |
|------|---------|--------|
| 指标 | Prometheus + Grafana | QPS、延迟、错误率、饱和度 |
| 日志 | ELK / Loki | 结构化日志、错误追踪 |
| 链路 | Jaeger / OpenTelemetry | 请求全链路耗时分布 |

### 关键告警规则
- 5xx 错误率 > 1%（持续 5 分钟）
- P99 延迟 > 500ms（持续 5 分钟）
- CPU/内存使用率 > 80%
- 磁盘剩余空间 < 10%

## IaC（基础设施即代码）

- Terraform / OpenTofu 管理云资源
- Ansible 管理配置
- Helm 管理 Kubernetes 应用
- 状态文件远程存储（S3/GCS + DynamoDB）

## 部署策略选择

| 策略 | 适用场景 | 风险 |
|------|---------|------|
| 蓝绿部署 | 无状态服务 | 双倍资源 |
| 金丝雀 | 关键业务 | 耗时较长 |
| 滚动更新 | 日常发布 | 回滚较慢 |
| 功能开关 | 功能测试 | 代码复杂度 |
```
````

---

## 第三部分：综合配置示例

一个包含所有 MCP 和 Skill 的完整 `opencode.json`：

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "github": {
      "type": "local",
      "command": ["npx", "-y", "@modelcontextprotocol/server-github"],
      "environment": {
        "GITHUB_TOKEN": "{env:GITHUB_TOKEN}"
      }
    },
    "filesystem": {
      "type": "local",
      "command": [
        "npx", "-y", "@modelcontextprotocol/server-filesystem",
        "/mnt/d/opencode/study",
        "/tmp"
      ]
    },
    "browser": {
      "type": "local",
      "command": ["npx", "-y", "@modelcontextprotocol/server-puppeteer"]
    },
    "docker": {
      "type": "local",
      "command": ["npx", "-y", "mcp-docker-server"]
    },
    "mysql": {
      "type": "local",
      "command": ["npx", "-y", "mcp-server-mysql"],
      "environment": {
        "MYSQL_HOST": "localhost",
        "MYSQL_PORT": "3306",
        "MYSQL_USER": "root",
        "MYSQL_PASSWORD": "{env:MYSQL_PASSWORD}",
        "MYSQL_DATABASE": "mydb"
      }
    },
    "terminal": {
      "type": "local",
      "command": ["npx", "-y", "shell-mcp-server"]
    }
  }
}
```

如果需要按 Agent 区分权限（比如只有特定 agent 能用 terminal）：

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": { /* ... 同上 ... */ },
  "tools": {
    "terminal_*": false
  },
  "agent": {
    "plan": {
      "tools": {
        "terminal_*": true
      }
    }
  }
}
```

---

## 第四部分：调试与排错

### 4.1 MCP 服务排查流程

```
MCP 不工作
│
├─ 1. 看日志
│  tail -f ~/.local/share/opencode/log/最新.log
│
├─ 2. 检查 JSON 格式
│  python3 -c "import json; json.load(open('opencode.json')); print('OK')"
│
├─ 3. 手动握手测试
│  echo '{"jsonrpc":"2.0","id":1,"method":"initialize",
│    "params":{"protocolVersion":"2024-11-05","capabilities":{},
│    "clientInfo":{"name":"test","version":"1.0"}}}' \
│  | npx -y <包名> 2>/dev/null
│
│  ├─ 有 JSON 返回 → 服务正常，检查 opencode 配置
│  └─ 无返回 → 管道不通，检查：
│     ├─ 包是否已安装（npm list -g）
│     ├─ WSL 跨系统路径问题
│     └─ 网络问题（npx 下载超时）
│
├─ 4. 查看 opencode 进程状态
│  ps aux | grep opencode
│  pgrep -la opencode
│
└─ 5. 重启 opencode
   /exit → 重新打开
```

### 4.2 Skill 排查流程

```
Skill 不显示
│
├─ 1. 检查文件名
│  必须是 SKILL.md，不是 skill.md 或 SKILL.MD
│
├─ 2. 检查文件路径
│  .opencode/skills/<name>/SKILL.md
│  ~/.config/opencode/skills/<name>/SKILL.md
│
├─ 3. 检查 YAML 头
│  必须有 name 和 description
│  name 必须和目录名一致
│
├─ 4. 检查命名规范
│  小写字母 + 连字符（-）
│  不能以 - 开头/结尾
│  不能有 --
│
└─ 5. 检查权限
   opencode.json 里有没有 deny 掉？
```

---

## 第五部分：常见问题

### 5.1 Q1: 启动报错 `Operation timed out`

**原因：** npx 首次运行下载包超时（默认 30 秒）。

**解决：** 先全局安装，再用 `--no-install`：

```bash
npm install -g <包名>
```

配置改为：

```json
"command": ["npx", "--no-install", "<包名>"]
```

### 5.2 Q2: 报错 `Missing property "type"`

**原因：** 漏写了 `"type": "local"`。

**解决：** 加上 `"type": "local"`。

### 5.3 Q3: 报错 `Property mcpServers is not allowed`

**原因：** 写成了 `"mcpServers"`，opencode 的正确字段是 `"mcp"`。

**解决：** 把 `"mcpServers"` 改成 `"mcp"`。

### 5.4 Q4: 报错 `Value is not accepted. Valid values: "local"`

**原因：** `"type"` 写成了 `"stdio"`。

**解决：** 把 `"type": "stdio"` 改成 `"type": "local"`。

### 5.5 Q5: Skill 加载不出来

```bash
# 检查文件路径
ls -la .opencode/skills/*/SKILL.md

# 检查 YAML 头语法
head -5 .opencode/skills/<name>/SKILL.md

# 检查目录名是否符合规范
ls .opencode/skills/
```

### 5.6 Q6: MCP 服务太多，上下文爆炸

**解决：** 用 `"enabled": false` 或 `"tools"` 字段按需启用：

```json
{
  "mcp": {
    "github": {
      "type": "local",
      "command": ["npx", "-y", "@modelcontextprotocol/server-github"],
      "enabled": false
    }
  },
  "tools": {
    "github_*": false,
    "filesystem_*": false
  }
}
```

需要时再让 AI 用：`use the github tool to ...`

---

## 第六部分：实战排错记录

> 每个 MCP 服务在安装使用中遇到的问题，按编号记录，方便后续查阅复现。
> 6.0 是通用预备知识，6.1 起是具体案例。

### 6.0 通用排查技能

在进入具体案例之前，先掌握两个最常用的排查命令。它们在 6.1~6.3 中反复出现，这里统一说明。

---

#### 6.0.1 查找入口文件

Node.js 的 npm 包通过 `package.json` 中的 `"bin"` 字段声明哪个文件是可执行入口。找到它才能用 `node` 直接启动，绕过 `npx`。

**命令：**

```bash
cat "$(npm root -g)/<包名>/package.json" | python3 -c "import json,sys; print(json.load(sys.stdin).get('bin'))"
```

**参数说明：**

| 部分 | 含义 |
|------|------|
| `$(npm root -g)` | 获取 npm 全局包的安装目录，例如 `/home/ttzz/.local/nodejs/lib/node_modules` |
| `<包名>` | 你要查的包，例如 `@modelcontextprotocol/server-github` |
| `python3 -c "..."` | 用 Python 解析 json，提取 `bin` 字段 |
| `.get('bin')` | 取 `package.json` 中的 `"bin"` 字段 |

**输出示例：**

```json
{"mcp-server-github": "dist/index.js"}
```

含义：这个包注册了一个叫 `mcp-server-github` 的可执行命令，实际入口是 `dist/index.js`。

**完整路径拼接：**

```
$(npm root -g)/<包名>/dist/index.js
```

例如：

```
/home/ttzz/.local/nodejs/lib/node_modules/@modelcontextprotocol/server-github/dist/index.js
```

**手动验证入口文件存在：**

```bash
ls "/home/ttzz/.local/nodejs/lib/node_modules/@modelcontextprotocol/server-github/dist/index.js"
```

---

#### 6.0.2 测试 MCP 握手

MCP 服务通过 **stdin/stdout**（标准输入/输出）与 opencode 通信。opencode 向 stdin 发送 JSON 请求，服务从 stdout 返回 JSON 响应。

我们可以模拟 opencode 的行为，手动发送初始化请求来验证服务是否正常。

**命令模板：**

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize",
  "params":{"protocolVersion":"2024-11-05","capabilities":{},
  "clientInfo":{"name":"test","version":"1.0"}}}' \
| <启动命令> 2>/dev/null
```

这个命令做了三件事：

```
echo '...'          → 1. 构造一段 JSON 请求（initialize）
|                   → 2. 通过管道（|）发给 MCP 服务进程
<启动命令> 2>/dev/null → 3. 启动服务，并丢掉 stderr（只保留 stdout 的 JSON 响应）
```

**请求参数详解：**

```json
{
  "jsonrpc": "2.0",          // 协议版本，固定
  "id": 1,                    // 请求 ID，递增序号
  "method": "initialize",     // MCP 初始化方法
  "params": {
    "protocolVersion": "2024-11-05",  // MCP 协议版本，当前最新
    "capabilities": {},               // 客户端能力声明，测试时留空
    "clientInfo": {
      "name": "test",                 // 客户端名称
      "version": "1.0"               // 客户端版本
    }
  }
}
```

**正常输出示例（以 cambridge-dict 为例）：**

```json
{
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": { "listChanged": true }
    },
    "serverInfo": {
      "name": "dictionary-server",
      "version": "0.0.1"
    }
  },
  "jsonrpc": "2.0",
  "id": 1
}
```

**各字段含义：**

| 字段 | 含义 |
|------|------|
| `result.protocolVersion` | 服务端同意的协议版本 |
| `result.capabilities.tools` | 服务端支持工具（tools）能力，`listChanged: true` 表示支持工具列表变更通知 |
| `result.serverInfo.name` | 服务端名称 |
| `result.serverInfo.version` | 服务端版本 |
| `jsonrpc` | 响应协议版本 |
| `id` | 对应请求的 ID，用于匹配请求和响应 |

**不正常的情况：**

| 现象 | 含义 |
|------|------|
| 无任何输出（空） | 管道不通。检查启动命令是否正确、依赖是否安装、路径是否存在 |
| 报错 `command not found` | 可执行文件不在 PATH 中。用完整路径代替 |
| 输出乱码或非 JSON | 服务启动失败，输出了错误信息到 stdout。去掉 `2>/dev/null` 重看完整输出 |

**常用变体：格式化输出**

如果返回的 JSON 很长，可以用 `python3 -m json.tool` 格式化：

```bash
echo '...' | <启动命令> 2>/dev/null | python3 -m json.tool
```

**快速校验：只看工具数量**

```bash
echo '...' | <启动命令> 2>/dev/null \
| python3 -c "import json,sys; d=json.load(sys.stdin); print('OK - tools:', len(d.get('result',{}).get('capabilities',{}).get('tools',{})))"
```

输出示例：`OK - tools: 0` 或 `OK - tools: 1`

---

#### 6.0.3 两命令的定位

这两个命令在整个排查流程中的位置：

```
MCP 服务不工作
│
├─ 1. 看日志           → ~/.local/share/opencode/log/
├─ 2. 检查包是否安装    → npm list -g <包名>
├─ 3. 查找入口文件      → 6.0.1  ← 你现在在这里
├─ 4. 测试 MCP 握手    → 6.0.2  ← 你现在在这里
│    ├─ 有 JSON 返回 → 服务正常，检查 opencode 配置
│    └─ 无输出       → 管道不通，检查路径和依赖
├─ 5. 修复配置         → 改用完整路径
└─ 6. 重启 opencode    → /exit → 重新打开
```

---

### 6.1 cambridge-dict MCP 修复实录

> 对应错误：`cambridge-dict Executable not found in $PATH: "node.exe"`
> 时间：2026-05-26 | 环境：WSL (Ubuntu 26.04)

#### 1. 现象

重启 opencode 后 cambridge-dict MCP 启动失败，日志显示：

```
ERROR service=mcp key=cambridge-dict
error=Executable not found in $PATH: "node.exe"
```

#### 2. 问题分析

错误信息很明确：**系统找不到 `node.exe`**。

原配置使用的是 Windows 侧的 Node.js：
```json
"command": ["node.exe", "C:\\Program Files\\nodejs\\node_global\\node_modules\\mcp-server-dictionary\\build\\index.js"]
```

排查步骤：

```bash
# 1. 检查 node.exe 在不在 WSL 的 PATH 里
which node.exe
# 输出: (空) — 找不到

# 2. 检查 node 在不在（Linux 版）
which node
# 输出: (空) — 也找不到

# 3. 检查 Windows 侧的 Node.js 安装目录
ls "/mnt/c/Program Files/nodejs/"
# 输出: node_cache/  node_global/
# 注意：没有 node.exe！说明 Node.js 已被卸载
# node_cache/ 和 node_global/ 只是 npm 残留数据

# 4. 全局搜索 node.exe
find /mnt/ -name "node.exe" 2>/dev/null
# 输出: (空) — Windows 侧的 Node.js 已完全卸载
```

**根因：** Windows 版的 Node.js 被卸载了，`node.exe` 从系统中消失。`/mnt/c/Program Files/nodejs/` 目录只剩下 npm 残留的 `node_cache/` 和 `node_global/`，实际的 `node.exe` 二进制文件已不存在。

#### 3. 方案选择

| 方案 | 操作 | 优点 | 缺点 |
|------|------|------|------|
| **A. WSL 内装 Node.js** | 在 Linux 侧安装 node | 与 opencode 同系统，管道通信无问题 | 需要下载安装 |
| **B. 重装 Windows 版 Node** | 在 Windows 侧重装 node.exe | 原配置不需改路径 | 还是要下载，且跨系统管道问题仍在 |

**选择方案 A** — 在 WSL 内安装 Node.js。理由：
- opencode 运行在 WSL（Linux）里，用 Linux 版 node 不需要跨系统管道通信
- 之前用 `node.exe`（Windows 版）是不得已（当时只有 Windows 装了 Node），现在可以彻底解决
- 命令直接用 `node` 即可，不再需要 `node.exe` + Windows 路径

#### 4. 修复过程

##### Step 1: 下载 Node.js 预编译包

```bash
# 下载 Linux x64 二进制包（约 25MB）
curl -L -o /tmp/node.tar.xz https://nodejs.org/dist/v20.18.0/node-v20.18.0-linux-x64.tar.xz

# 解压到用户目录
mkdir -p ~/.local
tar xf /tmp/node.tar.xz -C ~/.local/
mv ~/.local/node-v20.18.0-linux-x64 ~/.local/nodejs

# 清理
rm /tmp/node.tar.xz
```

> 不直接用 `apt install nodejs` 是因为 `sudo` 需要交互式密码，而 curl 下载到用户目录不需要提权。

##### Step 2: 验证安装

```bash
~/.local/nodejs/bin/node --version   # v20.18.0
~/.local/nodejs/bin/npm --version    # 10.8.2
```

##### Step 3: 加入 PATH（持久化）

```bash
echo 'export PATH="$HOME/.local/nodejs/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

##### Step 4: 安装 mcp-server-dictionary

```bash
npm install -g mcp-server-dictionary
```

##### Step 5: 测试 MCP 握手

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize",
  "params":{"protocolVersion":"2024-11-05","capabilities":{},
  "clientInfo":{"name":"test","version":"1.0"}}}' \
| node "$(npm root -g)/mcp-server-dictionary/build/index.js" 2>/dev/null
```

返回 JSON 说明握手成功。

##### Step 6: 更新 opencode.json

```json
{
  "cambridge-dict": {
    "type": "local",
    "command": [
      "/home/ttzz/.local/nodejs/bin/node",
      "/home/ttzz/.local/nodejs/lib/node_modules/mcp-server-dictionary/build/index.js"
    ]
  }
}
```

关键变化：
| 项目 | 修复前 | 修复后 |
|------|--------|--------|
| 可执行文件 | `node.exe`（Windows） | 完整路径 `~/.local/nodejs/bin/node` |
| 脚本路径 | Windows 风格 `C:\...` | Linux 风格 `/home/...` |
| Node 环境 | Windows（WSL 跨系统管道） | Linux（同系统，无兼容问题） |

##### Step 7: 重启 opencode 验证

退出 opencode 重新进入，不再报错。测试：
```
用 cambridge-dict_lookup_word 查一下 hello
```
正常返回词典数据。

#### 5. 核心经验

1. **`Executable not found in $PATH` 是明确信号** — 它直接告诉你哪个可执行文件找不到，不要绕弯子
2. **先确认问题根因** — 不要只改配置，先检查文件到底在不在：
   - `which node.exe` — 在 PATH 里找
   - `ls /path/to/directory` — 直接检查目录
   - `find /mnt -name "node.exe"` — 全局搜索
3. **WSL 环境双系统思维** — 你同时拥有 Linux 和 Windows 两套环境，某个工具可能在任意一侧。之前用 Windows 侧是历史原因，如果有机会切到同侧更稳定
4. **预编译包安装比 apt 更灵活** — 不需要 sudo，版本可控，装在自己的 home 目录下
5. **`.bashrc` 持久化 PATH** — 安装到 `~/.local/` 后一定要加 PATH，否则每次新终端都要手动 export

#### 6. 预防建议

- 定期检查关键命令是否存在：`node --version`
- 使用完整路径比依赖 PATH 更可靠（尤其对于 opencode 这种可能不加载 .bashrc 的场景）
- 建议把 Node.js 版本固定下来并文档化，避免未来升级破坏兼容性

---

### 6.2 github MCP 修复实录

> 对应错误：`github Executable not found in $PATH: "npx"`
> 时间：2026-05-26 | 环境：WSL (Ubuntu 26.04)

#### 1. 现象

重启 opencode 后 github MCP 启动失败，日志显示：

```
ERROR service=mcp key=github
error=Executable not found in $PATH: "npx"
```

#### 2. 问题分析

错误信息很明确：**系统找不到 `npx`**。

原配置：
```json
"github": {
  "command": ["npx", "-y", "@modelcontextprotocol/server-github"]
}
```

排查步骤：

```bash
# 1. 检查 npx 在不在 PATH 里
which npx
# 输出: /home/ttzz/.local/nodejs/bin/npx
# → npx 是存在的，但只在 shell 加载了 ~/.bashrc 后才在 PATH 里

# 2. 查看当前 shell 的 PATH
echo $PATH
# 输出中以 /home/ttzz/.local/nodejs/bin 开头
# → 因为当前 shell 已经 source 过 .bashrc

# 3. 关键问题：opencode 不加载 .bashrc
```

opencode 在启动 MCP 子进程时，**不会加载 `~/.bashrc`**。它继承的是系统级 PATH（`/usr/local/sbin:/usr/local/bin:...`），
而 `~/.local/nodejs/bin` 是我们手动加到 `.bashrc` 里的，opencode 并不知道这个路径。

**根因：** 与 6.1 同源但表现形式不同：

| 问题 | 根因 |
|------|------|
| 6.1 `node.exe` 找不到 | Windows 侧 Node.js 被卸载，`node.exe` 物理消失 |
| 6.2 `npx` 找不到 | WSL 侧 Node.js 已安装，但 `~/.local/nodejs/bin` 不在系统 PATH 中 |

**核心认识：** opencode 启动 MCP 子进程时的 PATH 继承自它的父进程（通常是系统 service 或桌面启动器），不会加载 shell 的 `~/.bashrc`。因此依赖 `npx`（它在 `~/.local/nodejs/bin/npx`）就会失败。

#### 3. 方案选择

| 方案 | 操作 | 优点 | 缺点 |
|------|------|------|------|
| **A. 系统 PATH 中添加路径** | 修改 `/etc/environment` 或 `~/.pam_environment` | 所有进程生效 | 需要 sudo |
| **B. 用完整路径替代 npx** | 直接写 `node` 全路径 + 包入口文件路径 | 不依赖 PATH，可靠 | 路径写死，升级需手动更新 |
| **C. 预装包 + `npx --no-install`** | 先全局安装，再用 `npx --no-install` | 接近原配置 | 仍然依赖 PATH 能找到 npx |

**选择方案 B** — 直接使用 `node` 全路径 + 包入口文件。理由：
- 与 6.1 中 cambridge-dict 的修复方式一致，统一风格
- 彻底消除对 PATH 的依赖
- opencode 官方也推荐用完整路径的数组格式

#### 4. 修复过程

##### Step 1: 确认包已安装

```bash
# 先检查 server-github 装没装
ls /home/ttzz/.local/nodejs/lib/node_modules/@modelcontextprotocol/server-github/

# 如果不存在，全局安装
npm install -g @modelcontextprotocol/server-github
```

> `@modelcontextprotocol/server-github` 已标记为 deprecated（不再维护），但仍然可用。后续可关注社区替代方案。

##### Step 2: 找到入口文件

查看 package.json 确认启动文件：

```bash
cat "$(npm root -g)/@modelcontextprotocol/server-github/package.json" | python3 -c "import json,sys; print(json.load(sys.stdin).get('bin'))"
# 输出: {'mcp-server-github': 'dist/index.js'}
```

入口文件路径：`/home/ttzz/.local/nodejs/lib/node_modules/@modelcontextprotocol/server-github/dist/index.js`

##### Step 3: 测试 MCP 握手

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize",
  "params":{"protocolVersion":"2024-11-05","capabilities":{},
  "clientInfo":{"name":"test","version":"1.0"}}}' \
| /home/ttzz/.local/nodejs/bin/node \
  /home/ttzz/.local/nodejs/lib/node_modules/@modelcontextprotocol/server-github/dist/index.js \
  2>/dev/null
```

返回完整的初始化 JSON，握手成功。

##### Step 4: 更新 opencode.json

```json
{
  "github": {
    "type": "local",
    "command": [
      "/home/ttzz/.local/nodejs/bin/node",
      "/home/ttzz/.local/nodejs/lib/node_modules/@modelcontextprotocol/server-github/dist/index.js"
    ],
    "environment": {
      "GITHUB_TOKEN": "你的_token"
    }
  }
}
```

关键变化：
| 项目 | 修复前 | 修复后 |
|------|--------|--------|
| 启动方式 | `npx -y <包名>` | 直接 `node` + 入口路径 |
| 依赖 PATH | 是（opencode 找不到 npx） | 否（绝对路径，永不丢失） |
| 首次启动 | npx 需要联网检查版本 | 直接执行，零延迟 |

#### 5. 核心经验

1. **`Executable not found in $PATH` 统一诊断流程：**
   - 在终端里 `which <命令名>` — 如果能找到，说明是 PATH 传递问题
   - 在终端里 `which <命令名>` — 如果找不到，说明是安装问题
   - 针对 opencode，**哪怕你终端能跑，opencode 也未必能找到**

2. **opencode 不加载 `~/.bashrc`** — 这是 WSL/桌面环境中常见的坑。`.bashrc` 只对交互式 shell 生效，opencode 启动 MCP 子进程时继承的是系统 service 的 PATH。

   **为什么 opencode 不加载 `~/.bashrc`？** 这涉及 Linux 进程启动机制：

   ```
   终端仿真器 (Windows Terminal / VSCode)
     → 启动 bash (交互式, 加载 ~/.bashrc)
       → bash 里执行 opencode
         → opencode 启动 MCP 子进程 (直接 fork/exec, 不经过 shell)
   ```

   - `~/.bashrc` 的定义是"交互式非登录 shell 的配置文件"——只有当你**打开一个新终端窗口**时，bash 才会读取它
   - opencode 是用 Node.js/Go 写的程序，它启动 MCP 进程的方式是系统调用 `fork()` + `exec()`，**不会启动一个新的 bash**，自然也就不会加载 `.bashrc`
   - MCP 子进程继承的是 opencode 进程的环境变量，而 opencode 本身是从你的 shell 启动的——所以 opencode 能看到你当前 shell 的 PATH，但 opencode 再去 spawn 子进程时，子进程看到的是**继承来的环境变量副本**，不会再重新加载任何配置文件

   **简单说：** `~/.bashrc` 只在"开新终端"时生效。opencode 开 MCP 进程相当于"直接在系统里创建一个新进程"，不是"打开一个终端再敲命令"。所以 PATH 里没有 `~/.local/nodejs/bin`。

3. **完整路径是终极方案** — 对于 opencode 的 `command` 配置，用绝对路径可彻底消除 PATH 相关的所有问题。代价是升级 npm 包后路径可能变化（但 `node_modules` 结构通常稳定）。

4. **统一风格** — 同一个项目内的 MCP 尽量用一致的启动方式。既然 6.1 已经用了 `node` + 全路径，6.2 也保持一致，后续的 MCP 同理（Node.js 系的都用同一套模式）。

#### 6. 补充：如何确认 PATH 问题

如果你怀疑是 PATH 问题，可以在 opencode 配置里加一个测试 MCP 来验证：

```bash
# 建一个简单脚本来打印 PATH
echo -e '#!/bin/bash\necho "PATH=$PATH"' > /tmp/print_path.sh && chmod +x /tmp/print_path.sh
```

```json
{
  "test-path": {
    "type": "local",
    "command": ["/tmp/print_path.sh"]
  }
}
```

重启 opencode 看日志输出的 PATH，就能确认 opencode 到底能看到哪些路径。

---

### 6.3 filesystem MCP 修复实录

> 对应错误：`filesystem Executable not found in $PATH: "npx"`
> 时间：2026-05-26 | 环境：WSL (Ubuntu 26.04)

#### 1. 现象

与 6.2 完全一致，只是服务名不同：

```
ERROR service=mcp key=filesystem
error=Executable not found in $PATH: "npx"
```

#### 2. 问题分析

**根因与 6.2 完全相同：** `npx` 位于 `~/.local/nodejs/bin/npx`，不在系统级 PATH 中，opencode spawn 子进程时找不到。

原配置：
```json
"filesystem": {
  "command": ["npx", "-y", "@modelcontextprotocol/server-filesystem", "/mnt/d/opencode/study", "/tmp"]
}
```

由于这是第三个遇到同样问题的 MCP，说明这不是偶然——**所有依赖 `npx` 启动的 Node.js MCP 服务在 WSL 环境下都会遇到这个坑。**

修复方案已经是标准流程了，见 6.2.3 方案对比。

#### 3. 修复过程

##### Step 1: 安装包

```bash
npm install -g @modelcontextprotocol/server-filesystem
```

##### Step 2: 找到入口文件

```bash
cat "$(npm root -g)/@modelcontextprotocol/server-filesystem/package.json" \
| python3 -c "import json,sys; print(json.load(sys.stdin).get('bin'))"
# 输出: {'mcp-server-filesystem': 'dist/index.js'}
```

入口：`/home/ttzz/.local/nodejs/lib/node_modules/@modelcontextprotocol/server-filesystem/dist/index.js`

##### Step 3: 测试 MCP 握手

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize",
  "params":{"protocolVersion":"2024-11-05","capabilities":{},
  "clientInfo":{"name":"test","version":"1.0"}}}' \
| /home/ttzz/.local/nodejs/bin/node \
  /home/ttzz/.local/nodejs/lib/node_modules/@modelcontextprotocol/server-filesystem/dist/index.js \
  /tmp 2>/dev/null
```

##### Step 4: 更新 opencode.json

```json
"filesystem": {
  "type": "local",
  "command": [
    "/home/ttzz/.local/nodejs/bin/node",
    "/home/ttzz/.local/nodejs/lib/node_modules/@modelcontextprotocol/server-filesystem/dist/index.js",
    "/mnt/d/opencode/study",
    "/tmp"
  ]
}
```

注意 `@modelcontextprotocol/server-filesystem` 和 `@modelcontextprotocol/server-github` 不同，它**接受目录参数**作为访问权限控制——只有列出的目录才允许读写。

#### 4. 模式总结

至此已修复三个 MCP，规律清晰：

| 服务 | 原配置 | 修复方式 | 本质 |
|------|--------|----------|------|
| cambridge-dict | `node.exe C:\...` | 完整 Linux node 路径 | Windows 版 Node 被卸载 |
| github | `npx -y @mcpsc/server-github` | 完整 node + 入口路径 | npx 不在系统 PATH |
| filesystem | `npx -y @mcpsc/server-filesystem` | 完整 node + 入口路径 | npx 不在系统 PATH |

**后续所有 Node.js 系的 MCP 服务都可以直接套用这个模式：**

```bash
# 1. 全局安装
npm install -g <包名>

# 2. 查看入口文件
cat "$(npm root -g)/<包名>/package.json" | python3 -c "import json,sys; print(json.load(sys.stdin).get('bin'))"

# 3. 配置
{
  "type": "local",
  "command": [
    "/home/ttzz/.local/nodejs/bin/node",
    "/home/ttzz/.local/nodejs/lib/node_modules/<包名>/<入口文件>"
  ]
}
```

---

## 速查表

### 7.1 MCP 服务一览

| # | 名称 | 包名 | 安装命令 | 主要能力 |
|---|------|------|----------|---------|
| 1 | GitHub | `@modelcontextprotocol/server-github` | `npm i -g` | 操作 GitHub（Issue/PR/Code） |
| 2 | Filesystem | `@modelcontextprotocol/server-filesystem` | `npm i -g` | 读写文件 |
| 3 | Browser | `@modelcontextprotocol/server-puppeteer` | `npm i -g` | 浏览器自动化 |
| 4 | Docker | `mcp-docker-server` | `npm i -g` | 管理 Docker |
| 5 | MySQL | `mcp-server-mysql` | `npm i -g` | 查询数据库 |
| 6 | Terminal | `shell-mcp-server` | `npm i -g` | 执行 Shell 命令 |

### 7.2 Skill 一览

| # | 名称 | 目录 | 适用场景 |
|---|------|------|---------|
| 7 | system-design | `.opencode/skills/system-design/SKILL.md` | 架构设计 |
| 8 | fullstack | `.opencode/skills/fullstack/SKILL.md` | 全栈开发 |
| 9 | code-review | `.opencode/skills/code-review/SKILL.md` | 代码审查 |
| 10 | devops | `.opencode/skills/devops/SKILL.md` | 运维部署 |

---

*教程版本 1.0 | 适用 opencode + Linux/WSL | 2026-05*
