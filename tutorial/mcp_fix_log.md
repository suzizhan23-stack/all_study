# MCP 服务修复完整实录

> Cambridge Dictionary MCP + PDF Reader MCP 从零到可用的完整过程，
> 包含日志分析、问题诊断、排错思路

---

## 目录

- [MCP 服务修复完整实录](#mcp-服务修复完整实录)
  - [目录](#目录)
  - [一、opencode 日志系统](#一opencode-日志系统)
    - [1.1 日志在哪](#11-日志在哪)
    - [1.2 日志文件命名规则](#12-日志文件命名规则)
    - [1.3 怎么看日志](#13-怎么看日志)
    - [1.4 日志关键字段含义](#14-日志关键字段含义)
- [第二部分：Cambridge Dictionary MCP 修复过程](#第二部分cambridge-dictionary-mcp-修复过程)
  - [2.1 第一次尝试：标准配置](#21-第一次尝试标准配置)
  - [2.2 第二次尝试：加 type 字段](#22-第二次尝试加-type-字段)
  - [2.3 第三次尝试：换 key 名称](#23-第三次尝试换-key-名称)
  - [2.4 第四次尝试：换 type 值](#24-第四次尝试换-type-值)
  - [2.5 启动成功但超时](#25-启动成功但超时)
  - [2.6 手动测试 MCP 握手](#26-手动测试-mcp-握手)
  - [2.7 预装包消除下载延迟](#27-预装包消除下载延迟)
  - [2.8 找到根因：npx 管道问题](#28-找到根因npx-管道问题)
  - [2.9 最终解决：直接用 node.exe](#29-最终解决直接用-nodeexe)
- [第三部分：PDF Reader MCP 修复过程](#第三部分pdf-reader-mcp-修复过程)
  - [3.1 初始状态：远程服务已下线](#31-初始状态远程服务已下线)
  - [3.2 方案选择](#32-方案选择)
  - [3.3 安装 pdf-mcp](#33-安装-pdf-mcp)
  - [3.4 验证握手](#34-验证握手)
- [第四部分：最终配置](#第四部分最终配置)
- [第五部分：调试速查表](#第五部分调试速查表)
  - [5.1 完整排查流程](#51-完整排查流程)
  - [5.2 常用命令汇总](#52-常用命令汇总)
  - [5.3 opencode 配置字段速查](#53-opencode-配置字段速查)
- [第六部分：总结与心得](#第六部分总结与心得)

---

## 一、opencode 日志系统

### 1.1 日志在哪

```bash
# 日志目录
~/.local/share/opencode/log/

# 列出所有日志（按时间倒序）
ls -lt ~/.local/share/opencode/log/
```

示例输出：

```
total 38728
-rw-r--r-- 1 ttzz ttzz  22563 May 13 22:53 2026-05-13T145228.log
-rw-r--r-- 1 ttzz ttzz   3726 May 13 22:52 2026-05-13T145227.log
-rw-r--r-- 1 ttzz ttzz 148522 May 13 22:52 2026-05-13T145005.log
-rw-r--r-- 1 ttzz ttzz  11752 May 13 22:50 2026-05-13T144936.log
```

### 1.2 日志文件命名规则

`2026-05-13T145228.log` = `日期T时间.log`

- `2026-05-13` = 2026 年 5 月 13 日
- `T` = 分隔符
- `145228` = 14:52:28（UTC）
- 每次启动 opencode 产生一个新日志文件

**最新日志永远是 `ls -t` 的第一条。**

### 1.3 怎么看日志

```bash
# 查看最新日志
cat ~/.local/share/opencode/log/最新.log

# 只看 MCP 相关
grep -i "mcp" ~/.local/share/opencode/log/最新.log

# 只看错误
grep -i "error\|timeout\|fail" ~/.local/share/opencode/log/最新.log

# 看某个 MCP 服务的启动过程
grep "cambridge-dict" ~/.local/share/opencode/log/最新.log
grep "pdf-reader" ~/.local/share/opencode/log/最新.log

# 实时查看（先找到最新日志，然后 -f 跟读）
tail -f ~/.local/share/opencode/log/$(ls -t ~/.local/share/opencode/log/ | head -1)
```

### 1.4 日志关键字段含义

```
INFO   2026-05-13T14:24:20  +1604ms  service=mcp  key=cambridge-dict  mcp stderr: Dictionary MCP server running on stdio
^      ^                    ^         ^            ^                   ^
级别   时间                 耗时      服务名        MCP 名称            具体消息
```

| 字段 | 含义 |
|------|------|
| `INFO` | 普通信息 |
| `ERROR` | 错误 |
| `service=mcp` | 这条日志来自 MCP 子系统 |
| `key=xxx` | 哪个 MCP 服务 |
| `mcp stderr: xxx` | MCP 服务进程输出的错误流内容 |
| `+1604ms` | 距离上一条日志过去了 1604 毫秒 |

关键日志示例解读：

```
# 服务被发现（配置读取成功）
INFO service=mcp key=cambridge-dict type=local found

# 服务启动成功（输出信息到 stderr）
INFO service=mcp key=cambridge-dict mcp stderr: Dictionary MCP server running on stdio

# 工具加载成功
INFO service=mcp key=pdf-reader toolCount=8 create() successfully created client

# 服务启动失败（超时）
ERROR service=mcp key=cambridge-dict error=Operation timed out after 30000ms local mcp startup failed
```

---

## 第二部分：Cambridge Dictionary MCP 修复过程

### 2.1 第一次尝试：标准配置

**配置：**

```json
{
  "mcpServers": {
    "cambridge-dict": {
      "command": "npx",
      "args": ["-y", "mcp-server-dictionary"]
    }
  }
}
```

**报错：**

```
Missing property "type"
```

**分析：** opencode 不认识 `mcpServers` 这个字段，而且缺少 `type`。

**修复：** 看 opencode 官方文档确认正确字段名。

---

### 2.2 第二次尝试：加 type 字段

**配置：**

```json
{
  "mcpServers": {
    "cambridge-dict": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "mcp-server-dictionary"]
    }
  }
}
```

**报错：**

```
Property mcpServers is not allowed
```

**分析：** opencode 的配置文件不使用 `mcpServers` 这个键名。

**修复：** 查阅文档发现正确的键是 `"mcp"`（不是 `"mcpServers"`）。

---

### 2.3 第三次尝试：换 key 名称

**配置：**

```json
{
  "mcp": {
    "cambridge-dict": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "mcp-server-dictionary"]
    }
  }
}
```

**报错：**

```
Value is not accepted. Valid values: "local"
```

**分析：** `"type": "stdio"` 不对，opencode 里本地服务要用 `"local"`。

**修复：** 查阅 opencode MCP 服务器文档，发现类型分两种：
- `"local"` = 本地启动的进程（stdio 通信）
- `"remote"` = 远程 HTTP 服务（SSE 通信）

---

### 2.4 第四次尝试：换 type 值

**配置：**

```json
{
  "mcp": {
    "cambridge-dict": {
      "type": "local",
      "command": "npx",
      "args": ["-y", "mcp-server-dictionary"]
    }
  }
}
```

**再报错：** schema 验证失败。查阅文档发现 `"command"` 必须是**数组**，不能分开写 `"command"` + `"args"`。

**最终正确的基础格式：**

```json
{
  "mcp": {
    "cambridge-dict": {
      "type": "local",
      "command": ["npx", "-y", "mcp-server-dictionary"]
    }
  }
}
```

---

### 2.5 启动成功但超时

配置对了，重启 opencode，看日志：

```bash
grep "cambridge-dict" ~/.local/share/opencode/log/最新.log
```

输出：

```
INFO  service=mcp key=cambridge-dict type=local found
INFO  service=mcp key=cambridge-dict mcp stderr: Dictionary MCP server running on stdio
ERROR service=mcp key=cambridge-dict command=["npx","-y","mcp-server-dictionary"]
      cwd=/mnt/d/opencode/study
      error=Operation timed out after 30000ms local mcp startup failed
```

**分析日志：**

| 行 | 含义 |
|----|------|
| `type=local found` | 配置文件读取成功 |
| `mcp stderr: Dictionary MCP server running on stdio` | 服务进程**确实启动了**，输出了正常信息 |
| `Operation timed out after 30000ms` | 但 opencode 等它初始化，等了 30 秒没等到 |

这说明：服务能跑，但 opencode 和它之间的通信出了问题。

---

### 2.6 手动测试 MCP 握手

MCP 服务通过 stdin/stdout 通信。可以模拟 opencode 发送初始化消息来测试：

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize",
  "params":{"protocolVersion":"2024-11-05","capabilities":{},
  "clientInfo":{"name":"test","version":"1.0"}}}' \
| npx -y mcp-server-dictionary 2>/dev/null
```

**没有输出。** 说明 npx 启动的服务没有正确处理通过管道送去的消息。

对比测试一个已知好的 MCP 服务：

```bash
npx -y @modelcontextprotocol/server-everything
# 输出: Starting default (STDIO) server...
```

这说明问题出在**这个特定的 MCP 服务**或其启动方式上。

---

### 2.7 预装包消除下载延迟

怀疑是 npx 首次下载包太慢导致超时。先全局安装：

```bash
npm install -g mcp-server-dictionary
```

配置加上 `--no-install` 跳过 npx 的版本检查：

```json
"command": ["npx", "--no-install", "mcp-server-dictionary"]
```

**仍然超时。** 说明不是下载速度的问题。

---

### 2.8 找到根因：npx 管道问题

进一步排查发现运行环境是 **WSL（Windows Subsystem for Linux）**：

| 组件 | 运行位置 |
|------|----------|
| opencode | WSL (Linux) |
| Node.js | Windows 原生 |
| npx | Windows 原生（从 WSL 调用） |

当 opencode（Linux）通过 npx（Windows）启动 MCP 服务时，**stdin/stdout 管道在跨系统调用中没有正确连接**。服务虽然启动了（输出到 stderr 的消息能收到），但 opencode 发给它的消息（stdin）收不到，它回复的消息（stdout）也发不回。

验证方法：直接用 Windows 路径 + `node.exe` 测试：

```bash
# 用 Windows 路径风格（不是 /mnt/c/... 而是 C:\...）
echo '{"jsonrpc":"2.0","id":1,"method":"initialize",...}' \
| node.exe "C:\Program Files\nodejs\node_global\node_modules\mcp-server-dictionary\build\index.js" 2>/dev/null
```

**这次有输出了！** MCP 握手成功返回 JSON。

---

### 2.9 最终解决：直接用 node.exe

把命令从 `npx` 换成 `node.exe` + Windows 绝对路径：

```json
{
  "mcp": {
    "cambridge-dict": {
      "type": "local",
      "command": [
        "node.exe",
        "C:\\Program Files\\nodejs\\node_global\\node_modules\\mcp-server-dictionary\\build\\index.js"
      ]
    }
  }
}
```

**路径要点：**
- 必须用 `node.exe` 而不是 `node`（WSL 中没有 `node` 命令，只有 `node.exe`）
- 路径必须用 Windows 风格 `C:\...`（不能用 WSL 风格 `/mnt/c/...`），因为 `node.exe` 是 Windows 程序，不认识 Linux 路径
- JSON 中反斜杠要转义：`\` 写成 `\\`

重启 opencode，日志变成：

```
INFO service=mcp key=cambridge-dict type=local found
INFO service=mcp key=cambridge-dict create() successfully created client
```

没有 ERROR 了，服务正常。

---

## 第三部分：PDF Reader MCP 修复过程

### 3.1 初始状态：远程服务已下线

**原有配置：**

```json
{
  "pdf-reader": {
    "type": "remote",
    "url": "https://pdf-mcp.jztan.com/mcp",
    "enabled": true
  }
}
```

**日志：**

```
ERROR SSE error: Non-200 status code (404)
```

**分析：**

```bash
# 测试 URL
curl -s -o /dev/null -w "HTTP %{http_code}" "https://pdf-mcp.jztan.com/mcp"
# 输出: HTTP 404
```

远程 MCP 端点返回 404，服务已下线。GitHub 仓库虽然还在更新，但托管服务停了。

### 3.2 方案选择

需要从远程服务切换到**本地安装**。可选方案：

| 方案 | 需要 | 状态 |
|------|------|------|
| `jztan/pdf-mcp`（Python） | Python + pip | ✅ 最优，功能最全 |
| `@modelcontextprotocol/server-pdf` (Node.js) | Node.js 22+ | ❌ 当前 Node.js 18 太旧 |
| `@rturv/mcp-pdf-reader` (Node.js) | Node.js 18+ | 未测试 |

选择 `pdf-mcp`（Python 版），因为功能最全（支持搜索、OCR、图片提取、表格提取、缓存）。

### 3.3 安装 pdf-mcp

**检查环境：**

```bash
# Windows 侧已有 Python 3.10
python.exe --version
# Python 3.10.7
```

**安装：**

```bash
# 在 WSL 中执行，会调用 Windows 的 pip
python.exe -m pip install pdf-mcp
```

如果提示 pip 找不到，先查看 Python 脚本目录：

```bash
python.exe -c "import sysconfig; print(sysconfig.get_path('scripts'))"
# C:\Users\ttzz\AppData\Local\Programs\Python\Python310\Scripts
```

确认安装成功：

```bash
ls "/mnt/c/Users/ttzz/AppData/Local/Programs/Python/Python310/Scripts/pdf-mcp.exe"
```

### 3.4 验证握手

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize",
  "params":{"protocolVersion":"2024-11-05","capabilities":{},
  "clientInfo":{"name":"test","version":"1.0"}}}' \
| "/mnt/c/Users/ttzz/AppData/Local/Programs/Python/Python310/Scripts/pdf-mcp.exe" 2>/dev/null
```

返回了完整的 MCP 初始化响应（包含 8 个工具），说明握手成功。

**配置：**

```json
{
  "pdf-reader": {
    "type": "local",
    "command": [
      "/mnt/c/Users/ttzz/AppData/Local/Programs/Python/Python310/Scripts/pdf-mcp.exe"
    ]
  }
}
```

注意这里用 WSL 路径风格（`/mnt/c/...`），因为 pdf-mcp.exe 是通过 WSL 调用的 Windows 程序，但 WSL 能正确识别自己的路径格式并传递给 Windows。

重启 opencode，日志：

```
INFO service=mcp key=pdf-reader type=local found
INFO service=mcp key=pdf-reader toolCount=8 create() successfully created client
```

8 个工具全部加载成功。

---

## 第四部分：最终配置

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "cambridge-dict": {
      "type": "local",
      "command": [
        "node.exe",
        "C:\\Program Files\\nodejs\\node_global\\node_modules\\mcp-server-dictionary\\build\\index.js"
      ]
    },
    "pdf-reader": {
      "type": "local",
      "command": [
        "/mnt/c/Users/ttzz/AppData/Local/Programs/Python/Python310/Scripts/pdf-mcp.exe"
      ]
    }
  }
}
```

**两个服务的对比：**

| | cambridge-dict | pdf-reader |
|---|---|---|
| 编程语言 | Node.js (JavaScript) | Python |
| 安装方式 | `npm install -g mcp-server-dictionary` | `pip install pdf-mcp` |
| 通信方式 | stdio | stdio |
| 路径风格 | Windows 风格 `C:\...` | WSL 风格 `/mnt/c/...` |
| 启动命令 | `node.exe C:\...index.js` | `/mnt/c/...pdf-mcp.exe` |

路径风格不同的原因：

- `node.exe` 是 Windows 原生程序，**不认识** `/mnt/c/...`，必须用 `C:\...`
- `pdf-mcp.exe` 也是 Windows 程序，但通过 WSL 的 `/mnt/c/...` 路径调用时，WSL 会自动将其转换为 Windows 路径传给程序

---

## 第五部分：调试速查表

### 5.1 完整排查流程

```
MCP 服务不工作
│
├─ 第 1 步：看日志
│  tail -f ~/.local/share/opencode/log/最新.log
│  
├─ 第 2 步：检查配置语法
│  python3 -c "import json; json.load(open('opencode.json'))"
│  
├─ 第 3 步：手动启动服务
│  # 看能不能跑起来
│  <command> 
│  
├─ 第 4 步：测试 MCP 握手（核心！）
│  echo '{"jsonrpc":"2.0","id":1,"method":"initialize",...}' | <command> 2>/dev/null
│  
│  ├─ 有 JSON 返回 → 握手成功，检查 config 或重启 opencode
│  └─ 无返回 → 管道不通
│       ├─ 检查命令本身能否独立运行
│       ├─ 检查 WSL/Windows 跨系统路径问题
│       └─ 尝试用完整路径 + 直接可执行文件
│  
├─ 第 5 步：检查预装
│  # npm 包
│  npm list -g <package-name>
│  # pip 包
│  python.exe -m pip list | grep <package-name>
│  
└─ 第 6 步：重启 opencode
   /exit → 重新打开
```

### 5.2 常用命令汇总

```bash
# === 日志 ===
# 查看最新日志
ls -lt ~/.local/share/opencode/log/
cat ~/.local/share/opencode/log/$(ls -t ~/.local/share/opencode/log/ | head -1)
# 实时跟读
tail -f ~/.local/share/opencode/log/$(ls -t ~/.local/share/opencode/log/ | head -1)

# === 配置文件 ===
cat opencode.json
python3 -c "import json; json.load(open('opencode.json')); print('JSON OK')"

# === MCP 握手测试模板 ===
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' \
| [启动命令] 2>/dev/null

# === Node.js 相关 ===
node --version
npm list -g | grep dictionary
npm install -g mcp-server-dictionary

# === Python 相关 ===
python.exe --version
python.exe -m pip list | grep pdf
python.exe -m pip install pdf-mcp

# === 找安装路径 ===
# npm 全局包
npm root -g
# pip 脚本
python.exe -c "import sysconfig; print(sysconfig.get_path('scripts'))"
```

### 5.3 opencode 配置字段速查

```json
{
  "mcp": {                      // ← 不是 "mcpServers"
    "服务名": {
      "type": "local",          // ← 本地服务用 "local"，不用 "stdio"
      "command": [              // ← 必须是数组，不能分开写 command + args
        "可执行文件",
        "参数1",
        "参数2"
      ]
    }
  }
}
```

---

## 第六部分：总结与心得

### 这次修复的核心经验

1. **日志是第一手证据** — 每次报错都能在日志里找到精确描述，比猜要快得多
2. **配置格式是最大坑** — opencode 的配置字段名和 Claude Desktop 不完全一样，必须查官方文档
3. **WSL + Windows 混合环境有坑** — 跨系统调用时，文件路径、管道通信都可能出问题。核心原则：**哪个系统的程序，就用哪个系统的路径**
4. **MCP 握手测试是万能钥匙** — 不管什么 MCP 服务，只要能通过 `echo ... | command 2>/dev/null` 返回 JSON，就能正常工作
5. **区分"服务启动"和"服务通信"** — 看到 "running on stdio" 只说明服务进程起来了，不等于 opencode 能跟它正常通信

### 环境信息

```
opencode 版本: 1.14.48
系统: WSL (Ubuntu) + Windows 11
Node.js: 18.16.1 (Windows)
Python: 3.10.7 (Windows)
npm root: C:\Program Files\nodejs\node_global
Python scripts: C:\Users\ttzz\AppData\Local\Programs\Python\Python310\Scripts
```
