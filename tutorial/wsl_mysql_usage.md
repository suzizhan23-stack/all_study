# WSL 中使用 Windows MySQL 教程

> opencode 运行在 WSL（Linux 子系统）中，MySQL 安装在 Windows 侧。
> 本文档教你如何在 WSL 中连接和管理 Windows 上的 MySQL。

---

## 一、环境说明

```
opencode / WSL (Linux)
         │
         │ TCP :3306
         ▼
Windows MySQL Server 8.0    ← 安装在 Windows 侧
```

你的 Windows MySQL 信息：

| 项目 | 值 |
|------|-----|
| MySQL 版本 | 8.0.31 |
| 主机 | localhost (127.0.0.1) |
| 端口 | 3306 |
| 用户名 | root |
| 密码 | 123456 |
| Windows 安装路径 | `C:\Program Files\MySQL\MySQL Server 8.0\` |
| Windows 客户端路径 | `C:\ttzz\DB\mysql-8.0.31-winx64\bin\mysql.exe` |

---

## 二、检查 MySQL 是否运行

```bash
# 方法1: 检查端口
timeout 3 bash -c 'echo > /dev/tcp/localhost/3306' 2>/dev/null && echo "MySQL 运行中" || echo "MySQL 未运行"

# 方法2: 直接连接测试
/mnt/c/ttzz/DB/mysql-8.0.31-winx64/bin/mysql.exe -h localhost -u root -p123456 -e "SELECT VERSION();"
```

有版本号输出说明 MySQL 在运行。

---

## 三、连接 MySQL

### 方式一：Windows mysql 客户端（推荐，已装好）

```bash
# 完整命令
/mnt/c/ttzz/DB/mysql-8.0.31-winx64/bin/mysql.exe -h localhost -u root -p123456

# 不带密码交互式输入
/mnt/c/ttzz/DB/mysql-8.0.31-winx64/bin/mysql.exe -h localhost -u root -p
# 然后输入密码

# 指定数据库
/mnt/c/ttzz/DB/mysql-8.0.31-winx64/bin/mysql.exe -h localhost -u root -p123456 word_learning
```

### 方式二：用别名简化（推荐）

将以下内容添加到 `~/.bashrc`：

```bash
echo 'alias mysql="/mnt/c/ttzz/DB/mysql-8.0.31-winx64/bin/mysql.exe -h localhost -u root -p123456"' >> ~/.bashrc
source ~/.bashrc
```

之后就可以简化为：

```bash
mysql -e "SELECT VERSION();"
mysql word_learning
```

### 方式三：安装 WSL 原生 mysql 客户端

```bash
# 需要 sudo 密码
sudo apt update
sudo apt install -y mysql-client

# 安装后可直接用 mysql 命令（但仍然需要 -h 指定 Windows MySQL）
mysql -h 127.0.0.1 -u root -p123456
```

> **注意：** `mysql -h localhost` 在 WSL 中默认使用 socket 文件，
> 而 Windows MySQL 不支持 Linux socket，必须用 `-h 127.0.0.1` 强制走 TCP。

---

## 四、常用命令

### 4.1 管理数据库

```bash
# 查看所有数据库
mysql -e "SHOW DATABASES;"

# 创建数据库
mysql -e "CREATE DATABASE mydb DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 删除数据库
mysql -e "DROP DATABASE mydb;"

# 选择数据库
mysql mydb
```

### 4.2 管理表

```bash
# 查看所有表
mysql word_learning -e "SHOW TABLES;"

# 查看表结构
mysql word_learning -e "DESC words;"

# 查看建表语句
mysql word_learning -e "SHOW CREATE TABLE words\G"
```

### 4.3 增删改查

```bash
# 查询
mysql word_learning -e "SELECT word, pos, frequency FROM words WHERE first_letter='A' ORDER BY frequency DESC;"

# 插入
mysql word_learning -e "INSERT INTO words (word, pos, first_letter, frequency) VALUES ('test', 'n.', 'T', 0);"

# 更新
mysql word_learning -e "UPDATE words SET frequency=1 WHERE word='test';"

# 删除
mysql word_learning -e "DELETE FROM words WHERE word='test';"
```

### 4.4 导入 SQL 文件

```bash
# 导入表结构
mysql word_learning < /mnt/d/opencode/study/database/schema.sql

# 导入索引
mysql word_learning < /mnt/d/opencode/study/database/indexes.sql

# 导入数据
mysql word_learning < /mnt/d/opencode/study/database/data.sql
```

### 4.5 导出 SQL 文件

```bash
# 导出整个数据库
/mnt/c/ttzz/DB/mysql-8.0.31-winx64/bin/mysqldump.exe -h localhost -u root -p123456 word_learning > backup.sql

# 只导出结构（不要数据）
/mnt/c/ttzz/DB/mysql-8.0.31-winx64/bin/mysqldump.exe -h localhost -u root -p123456 --no-data word_learning > schema_only.sql

# 只导出一张表
/mnt/c/ttzz/DB/mysql-8.0.31-winx64/bin/mysqldump.exe -h localhost -u root -p123456 word_learning words > words_table.sql
```

---

## 五、建好的数据库：word_learning

### 表结构

```bash
# 查看所有表
mysql word_learning -e "SHOW TABLES;"
```

输出：

```
Tables_in_word_learning
collocations
definitions
example_sources
examples
preposition_patterns
tags
usage_notes
word_forms
word_tags
words
```

### 快捷查询示例

```bash
# 查看 A 开头的所有动词
mysql word_learning -e "
SELECT w.word, w.pos, w.frequency,
       COUNT(c.id) AS collocations,
       COUNT(pp.id) AS prep_patterns,
       COUNT(e.id) AS examples
FROM words w
LEFT JOIN collocations c ON c.word_id = w.id
LEFT JOIN preposition_patterns pp ON pp.word_id = w.id
LEFT JOIN examples e ON e.word_id = w.id
WHERE w.first_letter = 'A' AND w.pos LIKE 'v%'
GROUP BY w.id
ORDER BY w.frequency DESC, w.word;
"

# 查看所有带 to 介词的搭配
mysql word_learning -e "
SELECT w.word, pp.pattern_en, pp.pattern_cn
FROM preposition_patterns pp
JOIN words w ON w.id = pp.word_id
WHERE pp.preposition = 'to'
ORDER BY w.word;
"

# 按词性统计数量
mysql word_learning -e "
SELECT pos, COUNT(*) AS count
FROM words
GROUP BY pos
ORDER BY count DESC;
"
```

---

## 六、MySQL 服务管理

如果 MySQL 无法连接，可能是服务未启动。在 Windows 中管理：

```bash
# 从 WSL 启动 MySQL 服务（需管理员权限）
/mnt/c/Windows/System32/net.exe start MySQL80

# 从 WSL 停止 MySQL 服务
/mnt/c/Windows/System32/net.exe stop MySQL80

# 手动启动 mysqld
"/mnt/c/Program Files/MySQL/MySQL Server 8.0/bin/mysqld.exe" --console
```

> 如果 `net start` 提示"拒绝访问"，则需要以管理员身份运行 WSL。
> 或者在 Windows 中搜索"服务" → 找到 MySQL80 → 右键启动。

---

## 七、常见问题

### Q1: `ERROR 1045 (28000): Access denied`

**原因：** 密码错误。

**解决：** 确认密码正确，用 `-p` 选项交互式输入：

```bash
/mnt/c/ttzz/DB/mysql-8.0.31-winx64/bin/mysql.exe -h localhost -u root -p
```

### Q2: `Can't connect to MySQL server on 'localhost'`

**原因：** MySQL 服务未运行。

**解决：**

```bash
# 启动服务
/mnt/c/Windows/System32/net.exe start MySQL80
```

### Q3: `Can't connect through socket`

**原因：** `mysql -h localhost` 在 Linux 下尝试用 socket 文件连接，但 Windows MySQL 不支持。

**解决：** 用 `-h 127.0.0.1` 强制走 TCP：

```bash
mysql -h 127.0.0.1 -u root -p123456
```

### Q4: 命令行密码安全警告

```bash
mysql: [Warning] Using a password on the command line interface can be insecure.
```

这是正常警告，不影响使用。也可以用交互式输入代替：

```bash
mysql -h localhost -u root -p
```

---

## 八、快捷脚本

创建一个启动脚本 `~/mysql.sh`：

```bash
cat > ~/mysql.sh << 'EOF'
#!/bin/bash
MYSQL="/mnt/c/ttzz/DB/mysql-8.0.31-winx64/bin/mysql.exe -h localhost -u root -p123456"
if [ "$1" = "" ]; then
  $MYSQL
else
  $MYSQL "$@"
fi
EOF
chmod +x ~/mysql.sh
```

使用：

```bash
~/mysql.sh -e "SHOW DATABASES;"
~/mysql.sh word_learning -e "SELECT * FROM words;"
```
