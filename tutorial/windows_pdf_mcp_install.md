# Windows 本地安装 pdf-mcp 教程

> 在 Windows 上安装 PDF MCP 服务，让 opencode 能直接读取 PDF 文件

---

## 一、概述

`pdf-mcp` 是一个 MCP 服务，让 AI 助手能读取、搜索和提取 PDF 文件内容。支持：
- 分页读取（不占满上下文）
- 全文搜索
- 表格提取
- 图片提取
- OCR（扫描件识别）
- SQLite 缓存

---

## 二、安装 Python

pdf-mcp 是用 Python 写的，需要 Python 环境。

### 2.1 检查是否已安装

```cmd
# 打开命令提示符（cmd）或 PowerShell，输入：
python --version
```

如果输出版本号（如 `Python 3.10+`），跳到第三节。

### 2.2 下载安装

1. 打开 https://www.python.org/downloads/
2. 点击下载按钮（建议 Python 3.10 - 3.12）
3. 运行安装程序
4. **关键：勾选 "Add Python to PATH"**（添加到系统路径）
5. 点击 "Install Now"

### 2.3 验证安装

```cmd
python --version
pip --version
```

两条命令都应该正常输出版本号。

---

## 三、安装 pdf-mcp

```cmd
pip install pdf-mcp
```

可选功能（按需安装）：

```cmd
# 语义搜索（需要更多磁盘空间，约 67MB）
pip install "pdf-mcp[semantic]"

# OCR 识别扫描件（需要额外安装 Tesseract，见下文）
pip install "pdf-mcp[ocr]"

# 全部功能
pip install "pdf-mcp[all]"
```

### 验证安装

```cmd
pdf-mcp --help
```

看到帮助信息即安装成功。

---

## 四、安装 OCR 支持（可选）

如果需要读取扫描版 PDF（图片格式的 PDF），需要安装 Tesseract：

1. 打开 https://github.com/UB-Mannheim/tesseract/wiki
2. 下载最新版安装包（如 `tesseract-ocr-w64-setup-5.x.x.exe`）
3. 安装，记住安装路径（默认 `C:\Program Files\Tesseract-OCR\`）
4. 将安装目录添加到系统 PATH：
   - 右键"此电脑" → 属性 → 高级系统设置 → 环境变量
   - 找到 `Path` 变量 → 编辑 → 新建 → 添加 `C:\Program Files\Tesseract-OCR\`
   - 确定保存

验证：

```cmd
tesseract --version
```

---

## 五、配置 opencode

找到 opencode 配置文件 `opencode.json`，添加 pdf-mcp 配置。

### 如果你用 WSL（和当前环境一样）

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "pdf-reader": {
      "type": "local",
      "command": ["python.exe", "-m", "pdf_mcp"]
    }
  }
}
```

原理：WSL 中 `python.exe` 会调用 Windows 侧的 Python。`-m pdf_mcp` 表示运行 `pdf_mcp` 这个 Python 模块。

### 如果你用原生 Windows（cmd / PowerShell）

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "pdf-reader": {
      "type": "local",
      "command": ["pdf-mcp"]
    }
  }
}
```

### 配置说明

| 字段 | 值 | 含义 |
|------|----|------|
| `"type"` | `"local"` | 本地服务 |
| `"command"` | `["python.exe", "-m", "pdf_mcp"]` | 用 Python 运行 pdf-mcp 模块 |

---

## 六、测试

重启 opencode，然后输入：

```
用 pdf-reader_pdf_info 查看 C:\path\to\test.pdf
```

可用的工具：

| 工具 | 功能 |
|------|------|
| `pdf_info` | 获取文档信息（页数、元数据、大小） |
| `pdf_read_pages` | 读取指定页 |
| `pdf_read_all` | 读取整个文档 |
| `pdf_search` | 搜索内容 |
| `pdf_render_pages` | 将页面渲染为图片 |
| `pdf_get_toc` | 获取目录 |
| `pdf_cache_stats` | 查看缓存状态 |
| `pdf_cache_clear` | 清除缓存 |

---

## 七、常见问题

### Q1: `pip install pdf-mcp` 报错

**原因：** 网络问题，pip 源在国外。

**解决：** 使用国内镜像：

```cmd
pip install pdf-mcp -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### Q2: `python.exe` not found in WSL

**原因：** WSL 找不到 Windows 的 Python。

**解决：** 用完整路径：

```json
"command": ["/mnt/c/Users/你的用户名/AppData/Local/Programs/Python/Python312/python.exe", "-m", "pdf_mcp"]
```

先确认 Python 实际位置：

```bash
# 在 WSL 中运行
which python.exe
# 或
ls /mnt/c/Users/*/AppData/Local/Programs/Python/Python*/python.exe
```

### Q3: WSL opencode + Windows python 通信问题

类似之前 Cambridge Dictionary MCP 的 npx 问题，如果直接用 `pdf-mcp` 命令不通，改用 `python.exe -m pdf_mcp` 指定 Windows Python 路径即可。

### Q4: 我想在 WSL 内直接装 Python

如果不想走 Windows Python，也可以在 WSL 内安装 Python 和 pip：

```bash
sudo apt update
sudo apt install python3 python3-pip
pip3 install pdf-mcp
```

然后配置用 WSL 的 Python：

```json
"command": ["python3", "-m", "pdf_mcp"]
```

### Q5: 端口冲突

默认无需端口，pdf-mcp 走 stdio 通信，不占用端口。

---

## 八、卸载

```cmd
pip uninstall pdf-mcp
```

---

*教程版本 1.0 | 适用于 pdf-mcp 最新版 + opencode 1.14.48*
