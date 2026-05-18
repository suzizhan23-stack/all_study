# Git 实战手册 — 从入门到日常协作

> 面向开发者的 Git 速查与原理指南

---

## 一、核心概念

### 1.1 三个区域

```
工作区 (Working Directory)    →   暂存区 (Staging Area)    →   本地仓库 (Local Repo)
     ↓                                                             ↓
  你正在编辑的文件                                             .git 目录
```

| 区域 | 说明 | 命令 |
|------|------|------|
| 工作区 | 你肉眼看到的文件目录 | `git status` |
| 暂存区 | 准备提交的变更列表 | `git add` |
| 本地仓库 | 已提交的历史记录 | `git commit` |

### 1.2 文件状态流转

```mermaid
graph LR
    UNTRACKED["未追踪(Untracked)<br/>新建文件"] -->|"git add"| STAGED
    MODIFIED["已修改(Modified)<br/>改了没暂存"] -->|"git add"| STAGED
    STAGED["已暂存(Staged)<br/>等提交"] -->|"git commit"| COMMITTED
    COMMITTED["已提交(Committed)<br/>安全了"] -->|"修改文件"| MODIFIED
```

---

## 二、常用命令速查

### 2.1 配置

```bash
# 首次使用必须设置
git config --global user.name "你的名字"
git config --global user.email "你的邮箱@example.com"

# 查看配置
git config --list

# 查看单条配置
git config user.name
```

### 2.2 创建仓库

```bash
# 方式 A：从零创建
mkdir my-project && cd my-project
git init

# 方式 B：克隆已有
git clone https://github.com/user/repo.git
git clone git@github.com:user/repo.git   # SSH
```

### 2.3 日常操作

```bash
# 查看状态（最常用）
git status

# 查看具体改了什么
git diff                 # 工作区 vs 暂存区
git diff --staged        # 暂存区 vs 上次提交

# 添加文件到暂存区
git add filename         # 添加单个文件
git add .                # 添加当前目录所有变更

# 提交
git commit -m "feat: 提交说明"

# 跳过暂存区，直接提交已追踪的文件
git commit -a -m "fix: 直接提交"
```

### 2.4 查看历史

```bash
git log                          # 完整历史
git log --oneline                # 一行一个提交
git log --oneline --graph        # 带分支图
git log -5                       # 最近 5 条
git log --oneline --since="2024-01-01"

# 查看某次提交的具体变更
git show commit_hash
```

### 2.5 分支操作

```bash
# 查看分支
git branch                       # 本地分支列表
git branch -a                    # 包含远程分支

# 创建与切换
git branch feature-login         # 创建分支
git checkout feature-login       # 切换分支
git switch feature-login         # 新语法，同上
git checkout -b feature-login    # 创建并切换（最常用）

# 合并
git checkout main                # 先切到目标分支
git merge feature-login          # 把 feature 合并过来

# 删除分支
git branch -d feature-login      # 删除本地分支（已合并）
git branch -D feature-login      # 强制删除（未合并也要删）
```

### 2.6 远程仓库

```bash
# 查看远程
git remote -v

# 添加远程
git remote add origin https://github.com/user/repo.git

# 推送
git push -u origin main          # 首次推送（-u 建立追踪关系）
git push                         # 后续只需 git push

# 拉取
git pull                         # 拉取远程最新代码并合并

# 设置/更换远程地址
git remote set-url origin https://github.com/new-user/repo.git
git remote set-url origin git@github.com:new-user/repo.git
```

### 2.7 撤销操作

```bash
# 工作区：放弃对某个文件的修改（未 git add）
git checkout -- filename         # 老语法
git restore filename             # 新语法

# 暂存区：把文件从暂存区撤回（保留工作区修改）
git reset HEAD filename          # 老语法
git restore --staged filename    # 新语法

# 提交：修改最后一次提交信息
git commit --amend -m "新信息"

# 提交：把新变更追加到最后一次提交（不新增 commit）
git add . && git commit --amend --no-edit

# 回退到某个版本（谨慎使用）
git reset --soft HEAD~1          # 撤销 commit，保留暂存区
git reset --mixed HEAD~1         # 撤销 commit 和暂存，保留工作区
git reset --hard HEAD~1          # 全部撤销，工作区也回退（危险！）
```

### 2.8 储藏（临时切换分支）

```bash
# 工作做到一半要切分支，先把修改存起来
git stash                        # 储藏当前修改
git stash pop                    # 恢复最近一次储藏并删除
git stash list                   # 查看所有储藏
git stash apply stash@{0}        # 恢复指定储藏（不删除）
git stash drop stash@{0}         # 删除指定储藏
```

---

## 三、工作流与分支策略

### 3.1 常用分支模型

```
main        → 生产环境代码（受保护，不能直接推）
develop     → 开发主分支
feature/xxx → 功能分支（从 develop 拉，合回 develop）
hotfix/xxx  → 紧急修复（从 main 拉，合回 main 和 develop）
release/x.x → 发布准备分支
```

### 3.2 日常开发流程

```bash
# ① 拉取最新代码
git checkout main && git pull

# ② 创建功能分支
git checkout -b feature/my-feature

# ③ 开发，多次提交
git add . && git commit -m "feat: add xxx"
git add . && git commit -m "fix: yyy"

# ④ 提交前同步主干（避免冲突）
git fetch origin
git rebase origin/main    # 或 git merge origin/main

# ⑤ 推送分支到远程
git push -u origin feature/my-feature

# ⑥ 在 GitHub 上创建 Pull Request
```

---

## 四、HTTPS 认证与多账号

### 4.1 Personal Access Token

```bash
# GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens
# 权限: Contents → Write

# 推送时用户名输你的 GitHub 名，密码输 Token
git push -u origin main
```

### 4.2 更换 HTTPS 账号

```bash
# 清除缓存的凭据
git credential reject <<< 'protocol=https
host=github.com
'

# 下次 push 会重新要求输入
```

### 4.3 SSH 配置（推荐）

```bash
# 生成 SSH Key
ssh-keygen -t ed25519 -C "your_email@example.com"
```

| 参数 | 含义 | 说明 |
|------|------|------|
| `-t ed25519` | **type** — 密钥类型 | `ed25519` 是目前最推荐的算法，比传统 RSA 更安全且密钥更短。也可以用 `-t rsa -b 4096` |
| `-C "..."` | **comment** — 注释 | 纯粹为了识别，通常填邮箱。不传也行，但建议加，GitHub 上方便看是哪个 key |

执行后终端会输出：

```
Your identification has been saved in C:\Users\ttzz/.ssh/id_ed25519
Your public key has been saved in C:\Users\ttzz/.ssh/id_ed25519.pub
The key fingerprint is:
SHA256:Jnyp5lhqkff0SH+CYO2t3v9gV70KgHVG2KAw6le1xtM suzizhan23@gmail.com
The key's randomart image is:
+--[ED25519 256]--+
|    ...          |
|   . o..         |
|  . o o..        |
|   . =.*o        |
|    = BES.       |
|   . = @ .       |
|    o = +        |
|     . .         |
|                 |
+----[SHA256]-----+
```

| 输出行 | 含义 |
|--------|------|
| `identification saved` | **私钥**已保存（`id_ed25519`），自己留着，**绝对不能给别人** |
| `public key saved` | **公钥**已保存（`id_ed25519.pub`），把这个文件的内容粘贴到 GitHub |
| `fingerprint` | **密钥指纹**，用于验证你拿到的公钥是不是真的是你的。`SHA256:...` 是哈希值，后面是你传入的 `-C` 注释 |
| `randomart image` | **随机艺术图**，纯视觉辅助。记住这个图案形状就能快速辨认是否是同一个密钥，不用对比一长串哈希 |

```bash
# 查看公钥并添加到 GitHub
cat ~/.ssh/id_ed25519.pub
```

| 文件 | 说明 |
|------|------|
| `~/.ssh/id_ed25519` | **私钥** — 留在本地，永远不要泄露给任何人 |
| `~/.ssh/id_ed25519.pub` | **公钥** — 可以公开，粘贴到 GitHub |

```bash
# 验证连接
ssh -T git@github.com
```

执行后会看到两种情况之一：

```
# ✅ 成功（公钥已加到 GitHub）：
Hi suzizhan23! You've successfully authenticated, but GitHub does not provide shell access.

# ❌ 失败（公钥没加或配错了）：
git@github.com: Permission denied (publickey).
```

| 看到什么 | 什么意思 | 怎么办 |
|---------|---------|--------|
| `Hi 用户名! You've successfully authenticated` | **连接成功** ✅ | 可以 push 了 |
| `Permission denied` | **认证失败** ❌ | 检查：① 公钥是否已贴到 GitHub？② 用的是不是正确的私钥？ |

**`-T` 参数不用记，它只是让输出更干净。** 不加也能用，但会多一堆乱七八糟的 shell 信息。

**`git@github.com` 中的 `git` 是 GitHub 固定的 SSH 用户名**，不是你 GitHub 的昵称。所有 GitHub 用户共用这一个用户名 `git`，GitHub 通过你的私钥来判断你是谁。

### 4.4 SSH 多账号

```bash
# 为新账号生成单独的 Key
ssh-keygen -t ed25519 -C "new@example.com" -f ~/.ssh/id_ed25519_new
```

| 参数 | 含义 | 说明 |
|------|------|------|
| `-f ~/.ssh/id_ed25519_new` | **file** — 输出文件名 | 不指定 `-f` 默认生成 `id_ed25519`。多账号必须指定不同文件名，否则会覆盖 |

```bash
# 配置 ~/.ssh/config
Host github-new               # 别名，自定义。之后用这个别名代替 github.com
  HostName github.com         # 真实服务器地址
  User git                    # 登录用户名（GitHub 固定用 git）
  IdentityFile ~/.ssh/id_ed25519_new  # 使用哪个私钥文件
```

| 配置项 | 含义 | 说明 |
|--------|------|------|
| `Host` | **别名** | 你自己起的名字，之后 `git@github-new:user/repo.git` 就用这个 |
| `HostName` | **真实地址** | 实际连接的服务器 |
| `User` | **用户名** | GitHub 固定是 `git` |
| `IdentityFile` | **私钥路径** | 告诉 SSH 用哪个私钥来认证，不写则用默认的 `id_ed25519` |

```bash
# 仓库 remote 用别名
git remote set-url origin git@github-new:user/repo.git
# 原来的 git@github.com:xxx 变成了 git@github-new:xxx
# SSH 读取 config，发现 Host 是 github-new → 用 id_ed25519_new 连接
```

---

## 五、常见问题

| 问题 | 解决 |
|------|------|
| 提交信息写错了 | `git commit --amend -m "新信息"` |
| 漏了文件想补到上次提交 | `git add 漏掉的文件 && git commit --amend --no-edit` |
| 想放弃所有本地修改 | `git restore .` 或 `git checkout -- .` |
| 想回到某个历史版本 | `git log` 找到 hash → `git reset --hard hash`（慎用） |
| 合并冲突了 | 手动编辑冲突文件 → `git add .` → `git commit` |
| 刚 push 了想撤回 | `git reset --hard HEAD~1` → `git push --force-with-lease`（⚠️ 危险） |
| 不小心把大文件 commit 了 | `git rm --cached 大文件` → 加到 `.gitignore` |
| 想放弃某个文件的修改 | `git restore filename` |

---

## 六、Push 超时与网络排查

`git push` 超时（timeout）通常不是 Git 本身的问题，而是本地网络无法连通 GitHub 服务器。按顺序排查：

### 6.1 快速定位问题

```bash
# ① 检查是否能连通 GitHub
ping github.com

# ② 检查 Git 远程地址用的是 HTTPS 还是 SSH
git remote -v
# 输出示例：
# origin  https://github.com/user/repo.git (fetch)    ← HTTPS
# origin  git@github.com:user/repo.git (push)          ← SSH

# ③ 测试 SSH 连通性（仅 SSH 协议）
ssh -T git@github.com
# 成功 → Hi 用户名! You've successfully authenticated...
# 失败 → ssh: connect to host github.com port 22: Connection timed out
```

| 现象 | 可能原因 | 下一步 |
|------|---------|--------|
| `ping` 不通 | DNS 污染 / 墙 | 换 DNS 或开代理 |
| `ping` 通但 `git push` 超时 | HTTPS 被干扰 | 换 SSH，或配代理 |
| `ssh -T` 连接超时（端口 22） | SSH 端口被墙 | 换 HTTPS + 代理，或用 SSH 的 443 端口 |
| `ssh -T` 显示 `Permission denied` | SSH Key 没配好 | 检查公钥是否添加到 GitHub |

### 6.2 DNS 问题

```bash
# 查看当前 DNS 解析
nslookup github.com

# Windows：修改 DNS 为公共 DNS
# 设置 → 网络和 Internet → 更改适配器选项 → 右键网卡 → 属性 → IPv4 → 使用以下 DNS
# 首选 1.1.1.1（Cloudflare）或 8.8.8.8（Google）
```

### 6.3 代理配置

**场景 A：系统级代理（Clash / V2Ray / 机场）：**

```bash
# 查看 Git 是否已配代理
git config --global --get http.proxy
git config --global --get https.proxy

# 设置代理（地址和端口按实际代理软件填写）
git config --global http.proxy http://127.0.0.1:7890
git config --global https.proxy http://127.0.0.1:7890

# 取消代理
git config --global --unset http.proxy
git config --global --unset https.proxy
```

**常见代理端口：**

| 软件 | 默认 HTTP 端口 |
|------|---------------|
| Clash / Clash Meta | 7890 |
| V2RayN | 10809 |
| Sing-box | 7890 |
| Shadowsocks | 1080（SOCKS5） |

如果代理软件只开了 SOCKS5，Git 需要额外配置：

```bash
git config --global http.proxy socks5://127.0.0.1:1080
git config --global https.proxy socks5://127.0.0.1:1080
```

**场景 B：只想当前命令走代理（不全局配置）：**

```bash
# Linux / macOS
ALL_PROXY=socks5://127.0.0.1:1080 git push

# Windows PowerShell
$env:ALL_PROXY="socks5://127.0.0.1:1080"; git push
```

**场景 C：IDE（IntelliJ IDEA / WebStorm 等）配置代理：**

```
File → Settings → Appearance & Behavior → System Settings → HTTP Proxy
选择 "Manual proxy configuration" → 填入代理地址和端口
Git 操作会走 IDE 的代理设置
```

### 6.4 更换协议

如果一种协议不通，切到另一种试试：

```bash
# HTTPS → SSH
git remote set-url origin git@github.com:user/repo.git

# SSH → HTTPS
git remote set-url origin https://github.com/user/repo.git
```

**SSH 使用 443 端口**（适用于 22 端口被封的环境）：

编辑 `~/.ssh/config`：

```
Host github.com
  HostName ssh.github.com
  Port 443
  User git
```

验证：`ssh -T git@github.com`

### 6.5 换源（Gitee 镜像）

国内用户可以配置 Gitee 镜像加速 clone，但 **push 仍然建议用 GitHub 原地址**：

```bash
# clone 加速（仅下载）
git clone https://github.com/user/repo.git
# 改回源地址以便 push
git remote set-url origin https://github.com/user/repo.git
```

### 6.6 WSL 特殊问题

Windows WSL 里访问 Windows 主机的代理：

```bash
# WSL 2 中 localhost 指向 WSL 自己，需用 Windows 主机 IP
# Windows IP 通常在 /etc/resolv.conf 能看到
cat /etc/resolv.conf | grep nameserver | awk '{print $2}'
# 输出示例：172.25.112.1

# 或直接用宿主机的 hostname
git config --global http.proxy http://$(hostname).local:7890
```

---

## 七、提交信息规范（Conventional Commits）

```bash
<type>: <简短描述>

# type 列表
feat      # 新功能
fix       # 修补 bug
docs      # 文档
style     # 格式（不影响代码运行）
refactor  # 重构
test      # 测试
chore     # 构建/工具变动
perf      # 性能优化

# 示例
feat: add user login page
fix: correct pagination logic when page is 0
docs: update README with setup instructions
refactor: extract validation logic into composable
```

---

## 八、.gitignore 模板

```gitignore
# 依赖
node_modules/
vendor/
.pnp
.pnp.js

# 构建输出
dist/
build/
*.tsbuildinfo

# 环境变量
.env
.env.local
.env.*.local

# IDE
.vscode/
.idea/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# 日志
*.log
npm-debug.log*
```

---

> 底线：`git status` 随时看状态，`git diff` 确认改了什么再提交。不确定时不要用 `--hard` 和 `--force`。
