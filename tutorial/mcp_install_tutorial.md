# 零基础安装 MCP 词典服务教程

> 让 opencode AI 助手能直接查 Cambridge 词典

---

## 一、什么是 MCP？

MCP = Model Context Protocol，相当于 **AI 助手的"应用商店"**。

- 手机装 App 能拍照、导航
- AI 装 MCP 服务能**查词典、读 PDF、查数据库、操作 Git** 等等

本教程就是给 opencode 装一个"词典查词 App"。

---

## 二、你需要什么

| 东西 | 有什么用 |
|------|----------|
| opencode | AI 编程助手，你已经在用 |
| Node.js | 运行 JavaScript 代码的环境，MCP 词典服务依赖它 |
| npx / npm | Node.js 的"应用商店"，用来下载安装 MCP 服务 |
| WSL（你已在使用） | Windows 里的 Linux 子系统，opencode 运行在里面 |

---

## 三、检查环境

先确认 Node.js 有没有装：

```bash
node --version
npm --version
npx --version
```

如果输出类似 `v18.16.1`、`9.5.1`，说明已安装。

> 如果没装，去 https://nodejs.org 下载 LTS 版本安装。

---

## 四、安装 MCP 词典服务

```bash
# 全局安装（装一次，到处可用）
npm install -g mcp-server-dictionary
```

参数说明：

| 部分 | 含义 |
|------|------|
| `npm install` | 下载安装软件包 |
| `-g` | 全局安装（装一次，所有地方都能用） |
| `mcp-server-dictionary` | 包名，一个 MCP 词典查词服务 |

---

## 五、配置 opencode

找到 opencode 的配置文件 `opencode.json`（在项目根目录或 `~/.config/opencode/opencode.json`），写入：

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "cambridge-dict": {
      "type": "local",
      "command": ["node.exe", "C:\\Program Files\\nodejs\\node_global\\node_modules\\mcp-server-dictionary\\build\\index.js"]
    }
  }
}
```

### 配置字段解释

| 字段 | 值 | 含义 |
|------|----|------|
| `"mcp"` | `{}` | 告诉 opencode 我要配置 MCP 服务（注意不是 `mcpServers`） |
| `"cambridge-dict"` | 自定义名字 | 你自己给这个服务起的名，后面调用时用 |
| `"type"` | `"local"` | 本地服务（不是远程的） |
| `"command"` | 数组 | 如何启动这个服务 |

### 如果你没遇到 WSL 问题

如果你不是在 WSL 里运行，而是直接在 Linux 或 macOS 上，命令可以简化为：

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

## 六、重启并测试

完全退出 opencode，重新打开，然后输入：

```
用 cambridge-dict_lookup_word 查一下 abandon
```

opencode 会调用词典服务，返回 **发音、释义、词性、例句** 等结构化数据。

---

## 七、常见问题

### Q1: 报错 `Operation timed out after 30000ms`

**原因：** npx 首次运行需要下载包，网络慢导致超时。

**解决：** 预装后再加 `--no-install` 跳过下载：

```bash
npm install -g mcp-server-dictionary
```

配置改为：

```json
"command": ["npx", "--no-install", "mcp-server-dictionary"]
```

### Q2: 报错 `Missing property "type"`

**原因：** 配置里漏了 `"type": "local"`。

**解决：** 加上 `"type": "local"`。

### Q3: 报错 `Property mcpServers is not allowed`

**原因：** 写成了 `"mcpServers"`，正确的字段名是 `"mcp"`。

**解决：** 把 `"mcpServers"` 改成 `"mcp"`。

### Q4: 报错 `Value is not accepted. Valid values: "local"`

**原因：** `"type"` 写成了 `"stdio"`，openCode 里本地服务类型是 `"local"`。

**解决：** 把 `"type": "stdio"` 改成 `"type": "local"`。

### Q5: npx 在 WSL 下无法通信

**原因：** 你的 opencode 在 WSL（Linux 子系统）里运行，但 Node.js 安装在 Windows 侧。`npx` 在跨系统调用时管道（stdin/stdout）连接有问题。

**现象：** 服务能启动（看到 `running on stdio`），但 opencode 收不到响应，最终超时。

**解决：** 跳过 npx，直接用 `node.exe` 指定 Windows 路径：

```json
"command": ["node.exe", "C:\\Program Files\\nodejs\\node_global\\node_modules\\mcp-server-dictionary\\build\\index.js"]
```

> 路径用 Windows 风格（`C:\...`），不要用 WSL 风格（`/mnt/c/...`），因为 `node.exe` 是 Windows 程序，不认识 Linux 路径。

### Q6: 我怎么知道服务能不能跑？

```bash
# 手动启动
node.exe "C:\Program Files\nodejs\node_global\node_modules\mcp-server-dictionary\build\index.js"
```
看到 `Dictionary MCP server running on stdio` 说明服务正常。

---

## 八、调试工具

### 1. 看 opencode 日志

```bash
ls ~/.local/share/opencode/log/
cat ~/.local/share/opencode/log/最新.log | grep -i error
```

### 2. 手动测试 MCP 通信

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | node.exe "C:\Program Files\nodejs\node_global\node_modules\mcp-server-dictionary\build\index.js" 2>/dev/null
```

有 JSON 响应返回说明通信正常。

### 3. 查看 opencode 进程

```bash
pgrep -la opencode
```

---

## 九、核心概念速查

| 概念 | 一句话说明 |
|------|-----------|
| **MCP** | AI 助手的"外挂插件"，让它能做额外的事情 |
| **JSON** | 一种数据格式，用 `{}` `[]` 组织信息，配置文件常用 |
| **Node.js** | 运行 JavaScript 代码的环境，类似浏览器但更底层 |
| **npm** | Node.js 的"应用商店"，`npm install` 下载软件包 |
| **npx** | npm 的"即用即下"模式，适合偶尔使用的工具 |
| **WSL** | Windows 里的 Linux，让开发者可以同时用两个系统 |
| **stdin/stdout** | 程序的"输入口"和"输出口"——A 写入 stdout，B 从 stdin 读取 |
| **全局安装** | `-g` 参数，装一次，系统任何位置都能用 |

---

## 十、其他好用的 MCP 服务

| 名称 | 功能 | 安装命令 |
|------|------|----------|
| **server-filesystem** | 让 AI 读写文件 | `npm install -g @modelcontextprotocol/server-filesystem` |
| **server-github** | 让 AI 操作 GitHub | `npm install -g @modelcontextprotocol/server-github` |
| **server-postgres** | 让 AI 查数据库 | `npm install -g @modelcontextprotocol/server-postgres` |

---

*教程版本 1.0 | 适用于 opencode 1.14.48 + WSL + Node.js 18*
