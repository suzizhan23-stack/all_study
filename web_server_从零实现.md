# Web Server 从零实现 — 内核级架构完全指南

> 作者视角：Web 服务器内核级架构导师 + 开源服务器框架作者 + 操作系统网络专家
>
> 目标：带您从零写出一个可运行的 Web Server，并理解它为什么这样设计。

---

## 目录

1. [Web Server 本质是什么](#一web-server-本质是什么)
2. [Socket 编程——地基](#二socket-编程地基)
3. [HTTP 协议解析——与浏览器对话](#三http-协议解析与浏览器对话)
4. [路由系统——请求分发](#四路由系统请求分发)
5. [响应生成与返回](#五响应生成与返回)
6. [并发模型——从 BIO 到 Reactor](#六并发模型从-bio-到-reactor)
7. [静态资源服务与中间件系统](#七静态资源服务与中间件系统)
8. [日志系统](#八日志系统)
9. [完整架构整合](#九完整架构整合)
10. [企业级设计对比](#十企业级设计对比)
11. [常见错误与排查](#十一常见错误与排查)
12. [学习路线](#十二学习路线)

---

## 1. Web Server 本质是什么

### 1.1 一句话本质

**Web Server 是一个永不退出的 while 循环，它反复做三件事：等请求 → 解析请求 → 返回响应。**

### 1.2 生活类比

Web Server 就像一个 **餐厅前台**：

| 角色 | 类比 |
|------|------|
| 服务器进程 | 前台服务员 |
| 端口（:80） | 餐厅门口 |
| 客户端请求 | 客人到店点餐 |
| HTTP 请求 | 客人写的菜单纸条 |
| 路由匹配 | 判断客人要什么菜系 |
| Handler | 后厨做菜 |
| HTTP 响应 | 做好的菜端给客人 |
| keep-alive | 客人吃完不走，继续点下一份 |
| 并发模型 | 多个服务员 vs 一个高效率服务员 |

### 1.3 技术原理

**Web Server 的核心抽象只有 4 个系统调用：**

```
socket() → bind() → listen() → accept() → read()/write() → close()
```

**内核视角发生了什么：**

1. `socket()` — 内核创建一个 **socket 对象**，包含发送缓冲区、接收缓冲区、等待队列
2. `bind()` — 将 socket 绑定到一个 **(IP, Port)** 元组，内核在协议控制块（PCB）中注册
3. `listen()` — 将 socket 从 CLOSED 状态切换到 LISTEN 状态，内核为该 socket 分配两个队列：
   - **半连接队列**（SYN_RCVD）：收到 SYN 但未完成三次握手
   - **全连接队列**（ESTABLISHED）：三次握手完成，等待 accept
4. `accept()` — 从全连接队列取出一个已完成连接的 socket，返回新 fd

### 1.4 完整请求流程

```
用户请求
│
▼ 步骤 1: DNS 解析 (域名 → IP)
│
▼ 步骤 2: TCP 三次握手
│   Client → SYN (seq=x)
│   Server → SYN+ACK (seq=y, ack=x+1)
│   Client → ACK (seq=x+1, ack=y+1)
│
▼ 步骤 3: 发送 HTTP 请求
│   GET /index.html HTTP/1.1\r\n
│   Host: example.com\r\n
│   \r\n
│
▼ 步骤 4: 内核接收数据
│   NIC → DMA → 内核 ring buffer → 软中断 → TCP 协议栈 → socket 接收缓冲区
│
▼ 步骤 5: accept 取出连接 (如果全连接队列非空)
│
▼ 步骤 6: read() 从 socket 读取 HTTP 数据
│
▼ 步骤 7: 解析 HTTP 请求行 + 请求头 + body
│
▼ 步骤 8: 路由匹配，找到对应 handler
│
▼ 步骤 9: handler 执行业务逻辑 (读文件 / 查数据库 / 代理)
│
▼ 步骤 10: 构造 HTTP 响应 → write() 写回 socket
│
▼ 步骤 11: TCP 四次挥手 (或 keep-alive 复用)
```

### 1.5 可视化流程图

```
┌───────────────────────────────────────────────────────┐
│                    操作系统内核                         │
│  ┌──────────┐   ┌──────────────┐   ┌───────────────┐  │
│  │ NIC 网卡  │→  │ ring buffer  │→  │ TCP 协议栈    │  │
│  │ (硬中断)  │   │ (DMA 写入)   │   │ (软中断处理)   │  │
│  └──────────┘   └──────────────┘   └──────┬────────┘  │
│                                            │           │
│  ┌─────────────────────────────────────────▼────────┐  │
│  │              socket 接收缓冲区                     │  │
│  └─────────────────────────────────────────┬────────┘  │
└────────────────────────────────────────────┼──────────┘
                                             │
┌────────────────────────────────────────────▼──────────┐
│                  用户空间 (Web Server)                  │
│                                                       │
│  socket() → bind() → listen() → accept()              │
│                                     │                 │
│                          ┌──────────▼──────────┐     │
│                          │  全连接队列 (已完成)   │     │
│                          └──────────┬──────────┘     │
│                                     │                 │
│                          ┌──────────▼──────────┐     │
│                          │  accept → 新 fd      │     │
│                          └──────────┬──────────┘     │
│                                     │                 │
│                          ┌──────────▼──────────┐     │
│                          │  read() HTTP 解析     │     │
│                          └──────────┬──────────┘     │
│                                     │                 │
│                          ┌──────────▼──────────┐     │
│                          │  路由匹配 → handler   │     │
│                          └──────────┬──────────┘     │
│                                     │                 │
│                          ┌──────────▼──────────┐     │
│                          │  write() 返回响应     │     │
│                          └──────────┬──────────┘     │
│                                     │                 │
│                          ┌──────────▼──────────┐     │
│                          │  close() 或 keep     │     │
│                          └─────────────────────┘     │
└───────────────────────────────────────────────────────┘
```

---

## 2. Socket 编程——地基

### 2.1 一句话本质

**Socket 是操作系统提供的一个文件描述符（fd），进程通过读写这个 fd 来与网络对端交换数据——内核在背后完成了所有网络协议栈的工作。**

### 2.2 生活类比

Socket = **电话机**：

- `socket()` = 安装一部电话机
- `bind()` = 给电话机分配一个号码（端口）
- `listen()` = 将电话机设为响铃模式
- `accept()` = 接听电话，建立通话
- `read()/write()` = 在通话中说话/听对方说话
- `close()` = 挂断电话

### 2.3 技术原理——深入操作系统底层

#### 2.3.1 每个系统调用——内核到底做了什么

当你的 Python 代码调用 `socket.socket(AF_INET, SOCK_STREAM)` 时，它触发了一条贯穿用户态 → 内核态 → VFS 层 → TCP 协议栈 → 内存管理器的完整路径。下面逐条拆解。

---

##### 2.3.1.1 `socket()` 内部流程

###### 2.3.1.1.1 理解思路

`socket()` 不是在创建"连接"，而是在创建**一个通信的端点**。可以理解为"装了一部电话机，但还没有号码、没有连线、没有响铃"。

内核在这个调用中做了三件核心的事：

1. **分配两个内核对象**：`struct socket`（通用网络抽象）和 `struct sock`（TCP 协议状态）。这就像电话机的机身和内部电路——机身决定它能打电话（网络通信），电路决定它用什么制式（TCP 协议）。
2. **初始化缓冲区**：每个 socket 有独立的接收队列 (`sk_receive_queue`) 和发送队列 (`sk_write_queue`)。数据到达时，网卡中断 → TCP 协议栈处理后，就是把 `sk_buff`（内核网络数据包）挂到这个队列上。你的程序 `recv()` 就是从队列里取。
3. **创建文件描述符**：内核把 socket 包装成一个 `struct file`，注册 `socket_file_ops` 函数表。从此你就可以用 `read(fd)` / `write(fd)` 来读写网络——这就是 **Unix"一切皆文件"** 的体现。

注意，此时**还不涉及任何网络通信**。没有三次握手、没有端口分配——只是在内核堆里划了一块内存，初始化了几个链表头和定时器。

```
Python: socket.socket(AF_INET, SOCK_STREAM)
  ↓ CPython 解释器: 调用 libc 的 socket(2)
  ↓ libc: 执行 syscall 指令, 从用户态切换内核态 (ring3 → ring0)
  ↓ 内核 sys_socket() → sock_create()
    │
    ├── 1. 分配 struct socket (约 128 字节, slab 分配器)
    │      └── 设置 ops = inet_stream_ops (TCP 操作函数表)
    │
    ├── 2. 分配 struct sock (约 1.5 KB, 位于内核堆的特定 slab cache)
    │      └── 设置 sk_family = AF_INET, sk_type = SOCK_STREAM
    │      └── 初始化 sk_receive_queue (空 sk_buff 链表头)
    │      └── 初始化 sk_write_queue (空 sk_buff 链表头)
    │      └── 初始化 sk_wait (等待队列, 用于阻塞唤醒)
    │      └── 设置默认缓冲区大小:
    │            sk_rcvbuf = tcp_rmem[1] (内核默认 ~87380 bytes)
    │            sk_sndbuf = tcp_wmem[1] (内核默认 ~16384 bytes)
    │      └── 初始化拥塞控制状态: snd_cwnd = 10 (初始拥塞窗口)
    │      └── 初始化定时器: retransmit_timer (重传), keepalive_timer 等
    │
    ├── 3. sock->state = SS_UNCONNECTED
    │       (注意: 这是 socket 层的状态, 不是 TCP 状态)
    │
    ├── 4. sock_map_fd(): 创建 struct file
    │      └── 分配 fd 数字 (当前进程 fdtable 中最小可用)
    │      └── file->f_op = socket_file_ops (指向 socket 的 read/write/ioctl)
    │      └── file->private_data = socket (反向关联)
    │      └── fdtable 中填入 fd → struct file 指针
    │
    └── 5. 返回 fd 数字 (如 3) 到用户空间
```

**一句话总结：** `socket()` = 装电话机，但还没插线、没号码。这个"空的通信端点"在你调用 `bind()` 和 `listen()` 之前什么也做不了。

---

##### 2.3.1.2 `bind()` 内部流程

###### 2.3.1.2.1 理解思路

`bind()` 是给刚才创建的"空端点"分配一个**身份**——IP 地址和端口号。类比：电话机装好了，现在给它分配一个电话号码（端口）并告诉电信局这是哪条线路（IP）。

内核做了三件事：

1. **检查端口能不能用**：端口 < 1024 需要 root 权限（为什么？因为 80/443 等标准端口如果被普通用户占用，系统 Web 服务可能被恶意程序冒充）。同时检查端口没有被其他程序占用 —— 通过全局 `bhash` 哈希表 (`port → socket` 的映射)。
2. **绑定地址**：`0.0.0.0` 表示"监听所有网卡"——不管数据包从 eth0 还是 eth1 进来，都接收。如果指定具体 IP（如 `192.168.1.100`），则只接收发往该 IP 的包。
3. **注册到内核哈希表**：把 (IP, Port) 写入内核的全局监听表中。这样当 TCP 协议栈收到一个 SYN 包时，才能根据目标端口找到对应的 socket。

注意：`bind()` 只创建了身份，还没有开始监听。真正的"接客"从 `listen()` 开始。

```
server.bind(('0.0.0.0', 8080))
  ↓ sys_bind()
    │
    ├── 1. sock->ops->bind() → inet_bind()
    │
    ├── 2. 端口检查:
    │      └── port < 1024 → 需要 CAP_NET_BIND_SERVICE (root)
    │      └── port 8080 → 无需特权
    │      └── 检查端口是否已被占用 (遍历 TCP 哈希表 bhash)
    │            └── 若已占用 → EADDRINUSE
    │            └── SO_REUSEADDR 可绕过此检查 (用于 TIME_WAIT 重用)
    │
    ├── 3. 地址合法性检查
    │      └── IP 0.0.0.0 (INADDR_ANY) → 监听所有网卡
    │      └── 若指定具体 IP → 仅监听该网卡
    │
    ├── 4. 写入 inet_sock:
    │      └── inet_rcv_saddr = INADDR_ANY (接收地址)
    │      └── inet_saddr = INADDR_ANY (源地址)
    │      └── inet_num = 8080 (端口号, 主机字节序)
    │
    └── 5. 将 (IP, Port) 注册到全局 TCP 哈希表 (bhash)
           └── 用于快速查找冲突端口
```

---

##### 2.3.1.3 `listen()` 内部流程

###### 2.3.1.3.1 理解思路

`listen()` 是 socket 从"一个端点"变成"一个服务器"的质变点。这通电话机终于被设置成了"响铃模式"——任何人都可以打进来了。

内核在这个调用中做了最关键的事：**分配了两个队列**。

为什么需要两个队列？因为 TCP 三次握手需要时间（至少一个网络往返 RTT）。如果不做队列分离，你的程序在 `accept()` 处理一个连接时，新来的连接请求就会丢失。双队列设计让内核可以**异步地完成握手**：

- **半连接队列 (syn_table)**：收到 SYN 握手请求但还没完成握手的连接放在这里。它们"正在路上"。
- **全连接队列 (accept_queue)**：三次握手已经完成、等待 `accept()` 取走的连接放在这里。

这两个队列是 Web 服务器能够同时处理大量并发连接的基础。`listen(128)` 中的 128 就是全连接队列的上限——当队列满了，内核会直接丢包（或启用 SYN Cookie 防御）。

###### 2.3.1.3.2 两个队列的数据从哪来

队列里的数据**不是用户程序放进去的**，全部由内核在**软中断上下文**中自动填充。完整过程：

**第 1 步：SYN 到达 → 半连接队列**

```
客户端发来 SYN (seq=x)
        │
        ▼
网卡收到 → DMA 写入 ring buffer → 硬中断 → 软中断 (NET_RX_SOFTIRQ)
        │
        ▼
tcp_v4_do_rcv() → tcp_v4_conn_request()
        │
        ├── 创建一个 struct request_sock (约 200 字节, slab 分配)
        │      ├── req->ir_remote = 客户端 IP:Port
        │      ├── req->ir_isn    = 服务端初始序列号 (随机)
        │      ├── req->mss       = 客户端通告的 MSS (用于避免 IP 分片)
        │      └── req->num_retrans = 0 (SYN+ACK 重传计数)
        │
        ├── request_sock 存入 syn_table 哈希桶
        │      └── 哈希 key = (saddr, dport) 的 hash 值
        │      └── 这就是"半连接队列"——本质是一个哈希表
        │
        └── 回复 SYN+ACK (seq=y, ack=x+1)
```

**第 2 步：ACK 到达 → 半连接移出，全连接移入**

```
客户端回复 ACK (seq=x+1, ack=y+1)
        │
        ▼
同样路径: 硬中断 → 软中断
        │
        ▼
tcp_v4_rcv() → tcp_check_req()
        │
        ├── 从 syn_table 中找到对应的 request_sock
        │      根据 (saddr, sport, daddr, dport) 哈希查找
        │
        ├── 验证 ACK 序列号是否合法 (防止伪造)
        │
        ├── request_sock 从 syn_table 移除 (半连接 -1)
        │
        └── request_sock 加入 accept_queue 链表 (全连接 +1)
              └── icsk_accept_queue.rskq_accept_tail->next = req
              └── sk->sk_ack_backlog++ (当前全连接数)
              └── 唤醒在 sk->sk_wait 上睡眠的 accept() 进程
```

**第 3 步：accept() 取走**

```
用户程序调用 accept()
        │
        ▼
从 accept_queue 链表头取出 request_sock
        ├── 分配新的 struct socket + struct sock (真正的连接对象)
        ├── 分配新的 fd
        └── 销毁 request_sock (它的使命完成)
```

**关键理解：**

- 半连接队列 = `syn_table` **哈希表**，不是先进先出队列。因为需要快速根据 (IP, Port) 查找对应的 request_sock 来匹配 ACK。
- 全连接队列 = **单向链表**，先进先出。按握手完成的顺序排队，`accept()` 按顺序取走。
- 一个 `request_sock` 在握手期间**先驻留在半连接队列**，握手完成后**迁移到全连接队列**。自始至终都是同一个内核对象。
- 整个过程中，**你的 Web Server 进程完全没有参与**——它在做自己的事（比如处理上一个请求），内核在软中断里默默完成了这一切。这就是异步 IO 的雏形。

```
server.listen(128)
  ↓ sys_listen()
    │
    ├── 1. 状态检查: sock->state 必须为 SS_UNCONNECTED
    │      也就是说, 只能对未连接的 socket 调用 listen
    │
    ├── 2. inet_listen() → inet_csk_listen_start()
    │
    ├── 3. TCP 状态转换:
    │      └── inet_sk_state_store(sk, TCP_LISTEN)
    │      └── 这是第一次 TCP 状态变化: CLOSED → LISTEN
    │      └── 注意: TCP 状态机一共 11 个状态 (见下方 2.3.3)
    │
    ├── 4. 分配半连接队列 (request_sock_queue):
    │      │
    │      ├── 4a. icsk_accept_queue.rskq_accept_head = NULL
    │      │       (全连接队列头指针, 初始为空)
    │      │
    │      ├── 4b. icsk_accept_queue.listen_opt 指向一个
    │      │       struct listen_sock (分配在 slab cache 中):
    │      │       ├── max_qlen_log = backlog 的 2 的对数
    │      │       │   (用于计算哈希表大小)
    │      │       ├── nr_table_entries = 取整到 2 的幂
    │      │       │   (例如 backlog=128 → table_size=128)
    │      │       ├── syn_table[...] 哈希桶数组
    │      │       │   (存储 struct request_sock 的哈希链)
    │      │       └── qlen_young / qlen_old (SYN Flood 保护计数)
    │      │
    │      └── 4c. sk->sk_ack_backlog = 0 (当前已完成握手数)
    │              sk->sk_max_ack_backlog = backlog (最大队列深度)
    │
    └── 5. 全连接队列限制
           └── 实际 max 取 min(backlog, net.core.somaxconn)
           └── /proc/sys/net/core/somaxconn (默认 4096, 旧内核 128)
           └── backlog 参数传入 128 → 全连接队列上限 = 128
```

---

##### 2.3.1.4 `accept()` 内部流程

###### 2.3.1.4.1 理解思路

`accept()` 是面试官，不是 HR——它不负责筛选简历（三次握手已经在内核中完成了），只负责从"已通过的候选人队列"中叫下一个进来面试。

关键要理解：**`accept()` 不参与任何网络通信**。三次握手的数据包（SYN、SYN+ACK、ACK）全部由内核的 TCP 协议栈在软中断中处理。`accept()` 做的只是：

1. **检查全连接队列**：看有没有已经完成握手的连接在排队
2. **如果队列空且 socket 是阻塞的**：进程挂起（`TASK_INTERRUPTIBLE`），等待内核唤醒
3. **如果队列非空**：取出 `request_sock`（握手阶段内核创建的小结构），在其基础上创建真正的 `struct socket` 和 `struct sock`，分配新 fd，返回给用户

所以"accept 阻塞"其实不是真的在等网络握手，而是在等**其他请求完成握手后被放入队列**。内核一旦把新连接放入全连接队列，就会唤醒 `accept()` 的等待者。

```
conn, addr = server.accept()
  ↓ sys_accept4()
    │
    ├── 1. 获取 socket fd → 找到 struct socket
    │
    ├── 2. 检查 socket 状态: 必须 TCP_LISTEN
    │      如果不是 LISTEN → EINVAL
    │
    ├── 3. inet_csk_accept():
    │      │
    │      ├── 3a. 检查全连接队列是否为空:
    │      │       sk_acceptq_is_empty(sk) ?
    │      │       └─ 是 → 进程进入睡眠:
    │      │             │ 设置进程状态: TASK_INTERRUPTIBLE
    │      │             │ 加入 sk->sk_wait 等待队列
    │      │             │ 调度器选择其他进程执行
    │      │             │ (等待: 网卡中断 → 软中断 → TCP 协议栈
    │      │             │  → 完成握手 → 加入 accept_queue
    │      │             │  → 唤醒等待队列中的进程)
    │      │             └─ 被唤醒 → 再次检查队列
    │      │
    │      └─ 3b. 否 → 从 icsk_accept_queue 取头节点
    │                  ↓
    │                  struct request_sock *req = 出队
    │
    ├── 4. 从 req 创建新的 struct socket + struct sock
    │      │
    │      ├── 4a. tcp_v4_syn_recv_sock():
    │      │       └── 分配新 struct sock (约 1.5KB)
    │      │       └── 复制监听 socket 的所有配置
    │      │       └── TCP 状态设为 TCP_ESTABLISHED
    │      │       └── 填充对端地址: ireq->ir_remote_addr
    │      │       └── 填充发送/接收序列号 (来自三次握手)
    │      │
    │      ├── 4b. 分配新 struct socket
    │      │       └── state = SS_CONNECTED
    │      │       └── ops = inet_stream_ops
    │      │
    │      ├── 4c. sock_map_fd() → 分配新 fd 数字
    │      │       └── 分配 struct file
    │      │       └── 写入 fdtable
    │      │       └── fd 返回给用户空间
    │      │
    │      └── 4d. 返回新 sock 给用户
    │
    ├── 5. 从接收队列中移除该请求块
    │      └── sk_acceptq_added(sk) 减 1
    │
    └── 6. 填充对端地址到用户传入的 sockaddr 结构
```

###### 2.3.1.4.2 accept() 之后连接保存在哪

`accept()` 返回后，连接保存在两个地方：

**1. 用户空间：通过 fd 持有**
`accept()` 返回一个整数 fd（如 5），你的程序把这个 fd 赋值给变量 `conn`。但这只是一个数字索引，真正的连接对象全在内核里：

```
conn = 5  ← 这只是个数字
               │
               ▼ 通过进程的 fdtable[5] 找到:
struct file  (VFS 层, 约 256 字节)
  └── private_data → struct socket (网络层, 约 128 字节)
                        └── sk → struct sock (TCP 层, 约 1.5 KB)
                                    ├── sk_receive_queue (接收缓冲区)
                                    ├── sk_write_queue   (发送缓冲区)
                                    ├── sk_state = TCP_ESTABLISHED
                                    ├── sk_rcvbuf (接收窗口大小)
                                    └── sk_sndbuf (发送窗口大小)
```

fd 只是一个**钥匙**，你的程序通过它告诉内核："我要操作 fd=5 对应的那个 TCP 连接"。fd 本身不保存连接数据，它只是一个索引——真正的连接状态全在内核的 `struct sock` 里。

**2. 内核空间：保存在进程的 fdtable + sock 结构体中**
只要你不 `close(conn)`，这个 `struct file` → `socket` → `sock` 的链条就一直在内核中存活着。内核 TCP 协议栈会持续为该连接维护：
- 接收/发送缓冲区（`sk_receive_queue` / `sk_write_queue`）
- TCP 状态（`sk_state`）
- 拥塞控制状态（`snd_cwnd`、`ssthresh` 等）
- 定时器（重传定时器、保活定时器）
- 序列号（`snd_nxt`、`rcv_nxt`）

这个连接的生命周期完全由 fd 的存活决定——**fd 活着，连接就活着；fd 关闭，连接就释放**。

###### 2.3.1.4.3 accept() 之后怎么持续使用（长连接 keep-alive）

**核心问题：** `accept()` 拿到的连接，读完一次数据后怎么继续读下一次？

答案很简单：**不要 `close()` 这个 fd，反复用同一个 fd 做 read/write**。

```python
# 单次连接（短连接）——读完就关
conn, addr = server.accept()
data = conn.recv(4096)
conn.send(response)
conn.close()    # ← 连接到此结束, 四次挥手

# 长连接（keep-alive）——反复用同一个 fd
conn, addr = server.accept()
while True:                        # ← 保持 fd 不关闭
    data = conn.recv(4096)          # 第一次读: GET /index.html
    if not data:
        break                      # 对端关闭了连接
    # 处理请求...
    conn.send(response)            # 返回响应
    # 不 close, 继续循环 → 读下一个请求
    # 第二次读: GET /style.css (同一个 TCP 连接)
    data = conn.recv(4096)
    # ...处理第二个请求...
```

**内核视角在这个循环中发生了什么：**

```
第 1 次循环:
  conn.recv(4096)  → 内核从 sk_receive_queue 取 sk_buff (HTTP 请求)
  conn.send(resp)  → 内核把数据放入 sk_write_queue → TCP 发送
                      (内核仍然持有 struct sock, 连接状态 ESTABLISHED)

第 2 次循环:
  conn.recv(4096)  → 同一个 struct sock 的 sk_receive_queue
                      (浏览器复用了同一条 TCP 连接发来第二个请求)
  conn.send(resp)  → 同一条 TCP 连接返回第二个响应

直到:
  conn.recv(4096) 返回 b""  → 对端 FIN, 连接关闭
  或 conn.close()           → 主动关闭, 触发四次挥手
```

**关键：** 内核不会因为你已经读完一次数据就销毁连接。只要 fd 还开着，内核就保留 `struct sock` 及其所有状态。下一次 `recv()` 时，如果客户端通过同一个 TCP 连接发来新数据，网卡中断 → 软中断 → TCP 协议栈处理后，数据仍然进入同一个 `sk_receive_queue`——你的 `recv()` 仍然能读到。

**HTTP keep-alive 在代码层面的体现：**

```python
def handle_client(conn):
    parser = HTTPRequestParser()
    keep_alive = True
    while keep_alive:                          # ← 不 close fd 的循环
        data = conn.recv(65536)
        if not data:
            break                              # 对端关闭
        parser.feed(data)
        while True:
            req = parser.parse()
            if req is None:
                break                          # 还没收到完整请求
            resp = handle_route(req)
            conn.sendall(resp.to_bytes())      # 返回响应
            # 检查 Connection 头决定是否继续复用
            if req.headers.get("Connection", "").lower() == "close":
                keep_alive = False
    conn.close()  # ← 循环结束后才真正关闭
```

**所以长连接的本质就是：**
- **你（用户程序）不 close fd**
- **内核不释放 struct sock**
- **TCP 连接保持在 ESTABLISHED 状态**
- **双方可以持续收发数据，直到任意一方主动关闭**

这就解释了为什么 HTTP/1.1 默认 keep-alive——减少反复三次握手/四次挥手的开销。一个 TCP 连接可以传输几十个 HTTP 请求再关闭。

---

###### 2.3.1.4.4 连接断开会发生什么

连接断开分两种情况：**正常断开**（优雅关闭）和**异常断开**（突然断网/RST）。

**情况 1：正常断开——四次挥手（优雅关闭）**

```
    主动方                     被动方
       │                         │
  close() 或 FIN
       │────── FIN ────────────► │  recv() 返回空 (b"")
       │                         │  此时被动方还 write() 仍然可以
       │◄──── ACK ───────────── │
       │                         │
       │                         │  被动方 close()
       │◄──── FIN ───────────── │
       │────── ACK ────────────► │
       │                         │
  TIME_WAIT (2MSL=60s)
       │
  CLOSED
```

**在你的程序里表现为：**

```python
# 服务端代码
while True:
    data = conn.recv(4096)
    if not data:          # ← 对端 FIN 到达后, recv 返回空字节
        break             #     这就是"连接断开"的信号
    # 处理请求...
conn.close()              # 服务端也 close, 完成四次挥手
```

**关键理解：** `recv()` 返回 `b""`（空字节）是 TCP 协议给你发信号的方式——这意味着对端已经调用了 `close()`，发送了 FIN。你收到空字节后应该 `close()` 自己的 fd，释放内核资源。

---

**情况 2：对方突然断开（拔网线 / 进程崩溃 / RST）**

```
    服务端                         客户端
       │                             │
       │    ← 网线断了 / 进程崩了     │
       │                             │
  conn.recv(4096)
       │                             │
       ├── 如果数据还在内核缓冲区:
       │     正常返回数据, 你完全不知道对方已经断了
       │
       ├── 如果缓冲区的数据已经读完:
       │     继续 recv → 内核尝试重传
       │     重传几次失败后 (tcp_retries2, 默认 15 次 ≈ 15~30 分钟)
       │     内核判定连接死亡 → recv 抛异常或返回 -1
       │
       └── 如果对方发了 RST (比如进程崩溃, 内核自动发 RST):
              recv 抛异常: Connection reset by peer (ECONNRESET)
```

**内核的保活机制（TCP Keepalive）：**

```python
# 默认: 2 小时没有任何数据交换后, 内核才开始探测
# 可以开启 SO_KEEPALIVE 让内核自动检测死连接

# 参数 (通过 setsockopt 设置):
#   TCP_KEEPIDLE  = 7200 秒 (2 小时无数据才开始探测)
#   TCP_KEEPINTVL = 75 秒   (每次探测间隔)
#   TCP_KEEPCNT   = 9 次    (连续 9 次无回复就判定死亡)

# 从程序视角:
try:
    # 开启保活
    conn.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
    # 调整探测参数 (Linux 2.6+)
    conn.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPIDLE, 60)   # 60 秒无数据就开始
    conn.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPINTVL, 10)  # 10 秒探一次
    conn.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPCNT, 3)     # 3 次无回复就断开

    data = conn.recv(4096)  # 如果连接已经死了, 这里会抛异常
except ConnectionResetError:
    print("连接被对端重置")
except TimeoutError:
    print("连接超时, 可能对端已经断了")
```

---

**情况 3：`close()` 之后，内核做了什么**

```c
close(fd);
  │
  ├── 1. 从 fdtable 中移除 fd 条目 (fd 数字可被后续 accept 复用)
  │
  ├── 2. struct file->f_count-- (引用计数减 1)
  │      └── f_count 变为 0 → 释放 struct file
  │
  ├── 3. 触发 tcp_close():
  │      ├── sk_receive_queue 中的 sk_buff 全部释放
  │      ├── sk_write_queue 中未发送的数据:
  │      │     ├── 若有未发送数据 → 尝试发送 (LINGER 控制是否等待)
  │      │     └── SO_LINGER 选项控制等待超时
  │      ├── TCP 状态转换:
  │      │     ESTABLISHED → FIN_WAIT_1 → FIN_WAIT_2 → TIME_WAIT → CLOSED
  │      │     (或 ESTABLISHED → CLOSE_WAIT → LAST_ACK → CLOSED, 取决于谁主动)
  │      └── 释放 struct socket + struct sock (归还 slab cache)
  │
  └── 4. fd 数字回到空闲池, 下次 accept() 可能复用这个数字
```

**四种断开场景速查表：**

| 场景 | recv() 返回值 | 原因 | 你的程序怎么办 |
|------|--------------|------|--------------|
| 对端正常 close 四次挥手 | `b""`（空字节） | FIN 到达, 数据发完了 | break 循环, close(fd) |
| 对端发 RST | 抛 `ConnectionResetError` | 对端进程崩溃 / 强制关闭 | 捕获异常, close(fd) |
| 对端断网/死机 | 卡住或最终超时抛异常 | 重传完所有次数仍未收到 ACK | 用 SO_KEEPALIVE 检测 |
| 本地 close(fd) | 不适用 | 主动关闭 | fd 变为无效, 不能再 recv/send |

**一句话总结：** 从你调用 `listen()` 的那一刻起，TCP 三次握手的处理就从你的程序交给了内核——内核在中断上下文（软中断）中自动完成握手，把结果排队。你的程序只需要在合适的时机调用 `accept()` 取走结果即可。

---

##### 2.3.1.5 `read()` / `recv()` 内部流程

###### 2.3.1.5.1 理解思路

`accept()` 处理完后，你拿到一个新 fd，代表一个已经建立好的 TCP 连接。现在需要通过 `read()` / `recv()` 从连接中读取数据（HTTP 请求）。

核心逻辑：**从内核的接收缓冲区拷贝数据到用户空间**。

数据到达的完整路径是：**网线 → 网卡 (硬中断) → DMA 写入 ring buffer → 软中断 → TCP 协议栈（校验/重组/排序）→ 放入 socket 的 sk_receive_queue → 你的 recv() 拷贝到用户程序**。

当你的程序调用 `recv()` 时：
- **如果有数据**：直接从 `sk_receive_queue` 取出 `sk_buff`（内核网络包结构），把数据拷贝到你的 buffer 中，然后释放 `sk_buff`。
- **如果没有数据且 socket 是阻塞的**：进程进入睡眠（`TASK_INTERRUPTIBLE`），被加入 socket 的等待队列。当数据到达时，软中断会唤醒等待队列中的进程。
- **如果没有数据且 socket 是非阻塞的**：立即返回 `EAGAIN`，告诉调用者"现在没数据，你过会儿再试"。

最耗时的操作是 **`skb_copy_datagram_msg()`**——内核到用户空间的数据拷贝。一次 `recv(4096)` 就要拷贝 4KB 数据。这就是为什么零拷贝技术（`splice` / `sendfile`）能大幅提升性能——它们避免了这层拷贝。

```
data = conn.recv(4096)
  ↓ sys_recvfrom()
    │
    ├── 1. fd → struct file → socket → sock
    │
    ├── 2. 从接收缓冲区 sk_receive_queue 取 sk_buff:
    │      │
    │      ├── 如果 sk_receive_queue 为空:
    │      │     └─ 阻塞 socket:
    │      │           │ 设置 TASK_INTERRUPTIBLE
    │      │           │ 加入 sk->sk_wait
    │      │           │ 调度出去
    │      │           └─ (内核 TCP 收到数据后: 软中断 →
    │      │               排队 sk_buff → 唤醒 sk_wait)
    │      │     └─ 非阻塞 socket:
    │      │           └─ 立即返回 EAGAIN / EWOULDBLOCK
    │      │
    │      └── 有数据: 取出 sk_buff 链表
    │
    ├── 3. skb_copy_datagram_msg():
    │      └── 内核 → 用户空间数据拷贝
    │      └── 从 sk_buff->data 拷贝到用户提供的 buffer
    │      └── 这是整个 recv 最耗时的操作之一
    │      └── 零拷贝 (splice) 可避免此拷贝
    │
    ├── 4. 释放 sk_buff (如果数据全部被读取)
    │      └── kfree_skb() → 归还到 slab 缓存
    │
    └── 5. 更新接收窗口:
          └── 可用空间变大 → 发送窗口更新通告给对端
```

---

##### 2.3.1.6 `write()` / `send()` 内部流程

###### 2.3.1.6.1 理解思路

`send()` 是读的逆过程——把用户空间的数据拷贝到内核的发送缓冲区，然后由内核 TCP 协议栈分片、加校验和、推送给网卡。

关键路径：**用户 buffer → copy_from_iter() → sk_buff → sk_write_queue → tcp_write_xmit() → 拥塞控制 → TCP 分段 → 校验和 → 网卡驱动**。

这里面有几个重要机制：

1. **Nagle 算法**（默认开启）：把多个小包合并成一个大包再发送，避免网络中充斥大量小包（如 1 字节的 TCP 包）。`TCP_NODELAY` 可以关闭它——Web 服务器通常关闭 Nagle，因为 HTTP 响应是完整的消息，不需要等待合并。
2. **拥塞控制 (cwnd)**：内核不会把所有数据一口气全发出去，而是根据网络的拥塞程度逐步发送。初始拥塞窗口 `cwnd=10`（约 14KB），每收到一个 ACK 就扩大一点。这就是为什么新建立的 TCP 连接发送速度一开始较慢，几轮 RTT 后达到峰值（慢启动）。
3. **发送缓冲区满**：如果对方接收太慢（接收窗口为 0），或者网络拥堵，你的数据会在内核发送缓冲区排队。如果满了，阻塞 socket 会睡眠等待，非阻塞返回 `EAGAIN`。

```
conn.send(response)
  ↓ sys_sendto()
    │
    ├── 1. fd → struct file → socket → sock
    │
    ├── 2. tcp_sendmsg():
    │      │
    │      ├── 2a. 将用户数据拷贝到内核 sk_buff
    │      │       └── sock_alloc_send_skb() 分配 sk_buff
    │      │       └── copy_from_iter() 从用户空间拷贝
    │      │       └── 如果发送缓冲区满:
    │      │             └─ 阻塞 socket: 睡眠等待
    │      │             └─ 非阻塞: EAGAIN
    │      │
    │      ├── 2b. 排队到 sk_write_queue
    │      │
    │      ├── 2c. tcp_write_xmit(): 拥塞控制 + 发送
    │      │       └── 检查拥塞窗口 (cwnd) 是否允许发送
    │      │       └── 检查接收窗口 (rwnd) 是否允许
    │      │       └── TCP 分段: MSS 大小切割
    │      │       └── 计算校验和
    │      │       └── 添加到输出队列
    │      │
    │      └── 2d. 触发软中断 → 网卡驱动发送
    │
    └── 3. 若设置了 TCP_NODELAY → 立即发送 (无 Nagle 延迟)
```

---

#### 2.3.2 Socket 有多少限制？为什么？一个 socket 消耗多少资源？

##### 2.3.2.1 限制一览

**直观理解：一个 Web 服务器能同时处理多少连接，取决于 5 道"门禁"。**

| 门禁 | 参数 | 默认值 | 白话解释 |
|------|------|--------|----------|
| **❶ 一个进程能打开多少个文件/socket** | `ulimit -n` | 1024 | 你的程序同一时间最多能 hold 住 1024 个 socket。超过就报 `Too many open files`。就像一个人最多同时拿 1024 张房卡——再多就拿不住了。 |
| **❷ 整个系统能打开多少个文件/socket** | `fs.file-max` | ~20% 内存页数 | 所有进程加一起，整个 Linux 系统总的 fd 上限。比如 16GB 内存的机器大概 160 万。如果所有进程的 fd 总数超过这个值，谁都开不了新连接——就像酒店所有房间加起来最多只能发 100 万张房卡。 |
| **❸ 一台机器作为客户端能发起多少个连接** | `ip_local_port_range` | 32768~60999（约 2.8 万个） | 当你的程序去 `connect()` 别人时（比如作为代理），内核会分配一个本地端口。范围只有 2.8 万个，所以到同一个目标 IP:Port 最多只能同时有 ~2.8 万个连接。就像酒店前台用 32768~60999 号纸条标记客人——用完了就得等回收。 |
| **❹ 监听端口一次能排队多少人** | `net.core.somaxconn` + `listen(backlog)` | 4096（旧内核 128） | 三次握手完成但还没来得及 `accept()` 的连接能排多长的队。如果 wait 队列满了，新连接内核直接丢。就像餐厅门口等位的座位只有 4096 个——坐满了后面的客人就不让进了。 |
| **❺ TCP 协议栈一共能吃多少内存** | `tcp_mem`（三个数字） | 动态，按内存算 | 所有 socket 的收发缓冲区加一起不能超过这个值。超了内核会压缩甚至丢包。就像整个酒店的洗衣房容量有限——同时洗太多床单就转不动了。 |
| **❻ 每个 socket 的发送缓冲区** | `tcp_wmem` | 最小 4K / 默认 16K / 最大 64K | 你的程序 `send()` 出去的数据，在内核里临时存着等发送。这个缓冲区大小决定了"一次能塞多少数据等发送"。就像每个房间门口的信箱——默认能塞 16K 的数据等邮差来取。 |
| **❼ 每个 socket 的接收缓冲区** | `tcp_rmem` | 最小 4K / 默认 87K / 最大 6M | 对方发来的数据还没被你的 `recv()` 取走时，存在内核的这个缓冲区里。默认 87K——如果你的程序读得慢，数据就在这排队等着。就像每个房间的收件箱——默认 87K 的空间放收到的信件，你读得慢箱子就满了。 |

##### 2.3.2.2 为什么有这些限制

**A. `ulimit -n`（单进程 fd 限制）**
- 每个 fd 背后是一个 `struct file` (约 256 字节) + 对应的内核对象
- 如果不限制，一个进程可以耗尽系统内存
- `ulimit -n 1000000` 意味着: fdtable 数组需要 8MB (每个指针 8 字节 × 1M)
- 但这只是索引数组，实际每个 fd 对应的对象内存另算

**B. `fs.file-max`（系统级限制）**
- 保护整个系统不被文件描述符耗尽
- 计算方法: `NR_FILE = (ram_pages * 10 / 100)` ≈ 20% 的物理页
- 例如 16GB RAM → `fs.file-max` ≈ 1.6M
- 可以通过 `/proc/sys/fs/file-nr` 查看当前状况:
  ```
  $ cat /proc/sys/fs/file-nr
  12736    0    1602178
  ────     ─    ───────
  已分配   未用  上限
  ```

**C. 本地端口范围 (`ip_local_port_range`)**
- 当你的程序作为客户端 `connect()` 时，内核会自动分配一个本地端口
- 端口范围: 32768~60999, 约 28K 个端口
- 这是 (源IP, 源端口, 目标IP, 目标端口) 四元组中的一部分
- 所以从一台机器到同一个目标 IP:Port 最多同时建立 ~28K 个连接
- 突破方法: 绑定多个客户端 IP (多网卡/虚拟 IP)

##### 2.3.2.3 一个 socket 到底消耗多少内存

```
一个 TCP socket 的内核内存消耗估算 (Linux x86_64):

必选开销 (创建时就分配):
  struct sock      ≈ 1.5 KB   (包含所有 TCP 状态, 在 slab cache 中)
  struct socket    ≈ 0.1 KB
  struct file      ≈ 0.3 KB
  fdtable 条目     ≈ 0.008 KB (8 字节指针)
                       ─────────
  socket() 开销     ≈ 1.9 KB

附加开销 (有数据时):
  接收缓冲区 (tcp_rmem default)  ≈ 87 KB (实际按需增长)
  发送缓冲区 (tcp_wmem default)  ≈ 16 KB
  sk_buff 元数据     ≈ 0.2 KB / 包

所以:
  空闲连接 (无数据) ≈ 2 KB
  活跃连接 (有数据) ≈ 2 KB + 103 KB 缓冲区 ≈ 105 KB

10000 空闲连接 ≈ 20 MB
10000 活跃连接 ≈ 1 GB+

线程模型对比:
  BIO:  10000 线程 × 8MB 线程栈 ≈ 80 GB  ← 不可行
  NIO:  10000 连接 × 2KB (空闲) ≈ 20 MB  ← 可行
```

这就是为什么 Nginx/Netty 使用事件驱动: **不在连接上分配线程, 只在线程上处理事件**。C10K 问题的本质不是网络, 是内存。

---

#### 2.3.3 TCP 状态机——LISTEN 与其他 10 个状态

**全 11 种 TCP 状态 (RFC 793):**

```
                      ┌──────────┐
                      │  CLOSED  │ ← 初始状态, 没有连接
                      └────┬─────┘
                           │ socket() / 被动打开
                           │
                      ┌────▼─────┐  ─── 主动 connect() ────→ ┌──────────┐
                      │  LISTEN  │                             │ SYN_SENT │
                      └────┬─────┘                             └────┬─────┘
                           │ 收到 SYN                              │ 收到 SYN+ACK
                           │                                        │
                      ┌────▼─────┐                             ┌────▼─────┐
                      │ SYN_RCVD │                             │           │
                      └────┬─────┘                             │           │
                           │ 收到 ACK (三次握手完成)              │           │
                           │                                     │           │
                      ┌────▼─────────────────────────────────────▼─────┐
                      │                  ESTABLISHED                    │
                      │           (这就是 accept 之后的状态)              │
                      └────┬──────────────────────────────────────┬─────┘
                           │                                      │
                   主动关闭│                                      │ 被动关闭
                           │                                      │
                      ┌────▼─────┐                          ┌─────▼──────┐
                      │ FIN_WAIT_1│                          │ CLOSE_WAIT │
                      └────┬─────┘                          └─────┬──────┘
                           │ 收到 ACK                         │ 应用程序调用 close()
                      ┌────▼─────┐                          ┌─────▼──────┐
                      │ FIN_WAIT_2│                          │  LAST_ACK   │
                      └────┬─────┘                          └─────┬──────┘
                           │ 收到 FIN                          │ 收到 ACK
                           │                                     │
                      ┌────▼─────┐                          ┌─────▼──────┐
                      │  TIME_WAIT│                          │             │
                      └────┬─────┘                          │             │
                           │ 2MSL 超时 (60s)                 │             │
                           │                                     │
                           └─────────→ ┌──────────┐ ←────────────┘
                                       │  CLOSED  │
                                       └──────────┘

                    还有一种罕见状态:
                    ┌──────────┐
                    │ CLOSING  │ ← 双方同时发起关闭
                    └──────────┘
                    (FIN_WAIT_1 收到 FIN 而非 ACK)
```

**LISTEN 状态详解：**
- 只有服务端 socket 处于此状态
- socket 进入此状态后，内核开始为它**接受新的 TCP 连接请求**
- 处于 LISTEN 的 socket 不能发送或接收数据，它的唯一工作就是接受连接
- `ss -tln` 可以看到所有 LISTEN 状态的端口

---

###### 2.3.3.1 场景 1：正常 Web 请求——“我访问一个网站”

这是最常见的场景：浏览器访问 80 端口，服务器返回网页后主动关闭连接。

```
浏览器                             Web 服务器 (Nginx/你的程序)
  │                                     │
  │         (浏览器主动 connect)          │
  │────── SYN ────────────────────────► │
  │         CLOSED → SYN_SENT           │
  │◄───── SYN+ACK ───────────────────── │
  │                                     │ CLOSED → LISTEN (启动时)
  │────── ACK ────────────────────────► │
  │         SYN_SENT → ESTABLISHED      │ LISTEN → SYN_RCVD (收到 SYN)
  │                                     │ SYN_RCVD → ESTABLISHED (收到 ACK)
  │                                     │ 全连接队列 +1
  │  ~~~~~~~~~~~ HTTP 请求/响应 ~~~~~~~~~~ │
  │────── GET / HTTP/1.1 ─────────────► │
  │◄───── HTTP 200 OK ──────────────── │
  │                                     │
  │         (服务器主动关闭)              │
  │◄───── FIN ───────────────────────── │ ESTABLISHED → FIN_WAIT_1
  │────── ACK ────────────────────────► │ FIN_WAIT_1 → FIN_WAIT_2
  │         ESTABLISHED → CLOSE_WAIT    │
  │         (浏览器还没关)               │
  │────── FIN ────────────────────────► │ FIN_WAIT_2 → TIME_WAIT
  │◄───── ACK ───────────────────────── │
  │         CLOSE_WAIT → LAST_ACK       │ TIME_WAIT 持续 60 秒
  │         LAST_ACK → CLOSED           │ 60 秒后 → CLOSED
```

**这个场景的关键观察：**
- **谁主动 close，谁进入 TIME_WAIT**。这个例子里服务器主动 close，所以服务器端的连接进入 TIME_WAIT，客户端的连接直接 CLOSED。
- **TIME_WAIT 只出现在主动关闭方**。这就是为什么 Web 服务器上总会看到大量 TIME_WAIT——通常服务器是主动关闭的一方（返回完响应就 close）。
- `ss -tan` 看到的 TIME_WAIT 绝大多数是服务器的**主动关闭留下的正常痕迹**，不是问题。

---

###### 2.3.3.2 场景 2：客户端先关闭——“用户关掉了浏览器标签页”

```
浏览器                             Web 服务器
  │                                     │
  │  ~~~~~~~~~~~ 正常请求/响应 ~~~~~~~~~~  │
  │                                     │
  │         (用户关标签页, 浏览器主动关闭) │
  │────── FIN ─────────────────────────► │ ESTABLISHED → CLOSE_WAIT
  │         ESTABLISHED → FIN_WAIT_1    │
  │◄───── ACK ───────────────────────── │ (服务器程序还在运行,
  │         FIN_WAIT_1 → FIN_WAIT_2    │  还没调用 close())
  │                                     │
  │       (服务器发现 recv 返回空字节)    │
  │          while 循环 break            │
  │          conn.close()                │
  │◄───── FIN ───────────────────────── │ CLOSE_WAIT → LAST_ACK
  │────── ACK ────────────────────────► │ LAST_ACK → CLOSED
  │         FIN_WAIT_2 → TIME_WAIT      │
  │         60 秒后 → CLOSED            │
```

**这个场景的关键观察：**
- **CLOSE_WAIT 出现在被动关闭方**。服务器收到了浏览器的 FIN，但还没调用 `close()`——这段"还没调用 close"的时间就是 CLOSE_WAIT。
- **大量 CLOSE_WAIT = 你的程序忘记 close(fd)**。如果你看到几千个 CLOSE_WAIT，说明对端已经断开，但你的代码没有调用 `conn.close()`。这是 bug，不是正常现象。
- `ss -tan | grep CLOSE_WAIT | wc -l` 可以用来排查连接泄漏。

---

###### 2.3.3.3 场景 3：连接被拒绝——“端口没开”

```
客户端                              服务器 (没开 8080 端口)
  │                                     │
  │────── SYN ────────────────────────► │
  │         SYN_SENT                    │ 没有 socket 在 LISTEN 8080
  │◄───── RST ───────────────────────── │ 内核回复 RST
  │         SYN_SENT → CLOSED           │
  │  connect() 抛异常:                   │
  │  ConnectionRefused                  │
```

**发生在：** 你 `connect()` 了一个没有进程在监听的端口。内核收到 SYN 后，查找该端口的 LISTEN socket，找不到，直接回复 RST。

**程序表现：** `ConnectionRefusedError (errno 111)`。

---

###### 2.3.3.4 场景 4：双方同时关闭——“同时说再见”

罕见但 TCP 规范确实定义了这种情况：

```
进程 A                              进程 B
  │                                     │
  │  (A 和 B 同时调用了 close())        │
  │────── FIN ────────────────────────► │
  │         ESTABLISHED → FIN_WAIT_1    │ ESTABLISHED → FIN_WAIT_1
  │◄───── FIN ───────────────────────── │
  │         (没收到 ACK, 先收到 FIN!)   │
  │         FIN_WAIT_1 → CLOSING        │ FIN_WAIT_1 → CLOSING
  │────── ACK ────────────────────────► │
  │◄───── ACK ───────────────────────── │
  │         CLOSING → TIME_WAIT         │ CLOSING → TIME_WAIT
  │         60 秒后 → CLOSED            │ 60 秒后 → CLOSED
```

**CLOSING 状态：** 表示 FIN_WAIT_1 状态下收到的是 FIN 而不是 ACK。双方都进入了"等待对方的 ACK"的状态。这种状态很少见，通常在双方应用程序同时调用 close() 且网络没有丢包时发生。

---

###### 2.3.3.5 场景 5：SYN Flood 攻击——“半连接队列被打满”

```
攻击者                              服务器
  │                                     │
  │────── SYN (假 IP) ────────────────► │
  │         (源 IP 是伪造的, 不存在)     │ LISTEN → SYN_RCVD
  │◄───── SYN+ACK ───────────────────── │ request_sock 加入半连接队列
  │         (发往假 IP, 永远没人回)      │ 半连接队列 +1
  │                                     │
  │────── SYN (假 IP) ────────────────► │
  │◄───── SYN+ACK ───────────────────── │ 半连接队列 +1
  │                                     │
  │   ... 重复发送大量假 SYN ...        │
  │                                     │ 半连接队列满了!
  │────── SYN (正常用户) ─────────────► │
  │                                     │ 队列已满 → 丢包!
  │         (正常用户连接不上!)          │ 正常用户 SYN 被丢弃
```

**这个场景的状态机含义：**
- 正常用户的 SYN 到达时，如果半连接队列已满，**内核直接丢包**，不回复任何东西
- 正常用户没收到 SYN+ACK，会重试几次（`net.ipv4.tcp_syn_retries`，默认 6 次 ≈ 1 分钟），超时后放弃
- **SYN Cookie 防御**：当半连接队列满时，内核可以不存储 request_sock，而是把握手信息编码在 SYN+ACK 的序列号中（`seq = 加密哈希 | 时间戳 | MSS`），客户端回复 ACK 时从序列号解码——彻底绕过半连接队列

**通过状态数量定位问题：**
```bash
# 查看当前连接状态分布
$ ss -tan | awk '{print $1}' | sort | uniq -c | sort -rn

  # 正常服务器:
  843  ESTAB       # 正在处理的活跃连接
   32  TIME_WAIT   # 主动关闭的正常残留
    3  LISTEN      # 监听的端口
    0  CLOSE_WAIT  # 好, 没有泄漏

  # 有问题的服务器 (连接泄漏):
  5432  CLOSE_WAIT # ← 你的代码忘了 close(), 要排查
  1234  TIME_WAIT  # 主动关闭的正常残留

  # 被攻击的服务器:
  1024  SYN_RCVD   # ← 半连接队列满了, 可能是 SYN Flood
    0   ESTAB      # 正常用户连不上
```

---

#### 2.3.4 半连接队列和全连接队列——从底层拆解

##### 2.3.4.1 为什么需要两个队列

TCP 三次握手是异步的：从收到第一个 SYN 到完全建立连接需要一次网络往返 (RTT)。如果每次握手都必须等待 `accept()` 才能继续，服务器一次只能处理一个连接。双队列设计将连接建立拆分为两个阶段：

```
三次握手过程                   队列变化
─────────────────              ────────
1. 收到 SYN
   创建 request_sock
   加入半连接队列 (SYN_RCVD)
                               半连接队列 +1
2. 回复 SYN+ACK
   等待客户端 ACK
                               (request_sock 在半连接队列等待)
3. 收到 ACK
   三次握手完成
   移除半连接队列
   加入全连接队列 (ESTABLISHED)
                               半连接队列 -1
                               全连接队列 +1
4. accept() 取走
                               全连接队列 -1
```

##### 2.3.4.2 数据结构定义（Linux 5.x 内核源码）

```c
// include/net/inet_connection_sock.h
struct request_sock_queue {
    // ---- 全连接队列 ----
    struct request_sock      *rskq_accept_head;  // 队列头
    struct request_sock      *rskq_accept_tail;  // 队列尾

    // ---- 半连接队列配置 ----
    struct listen_sock       *listen_opt;        // 半连接哈希表
};

// 半连接哈希表（旧的实现, 新内核已改为 ehash/bhash 统一管理）
struct listen_sock {
    u8                       max_qlen_log;       // 哈希表大小的对数
    int                      nr_table_entries;   // 哈希桶数
    struct request_sock      *syn_table[0];      // 变长数组, 哈希桶指针
    int                      qlen;               // 当前半连接数量
    u32                      max_qlen;           // 半连接上限
};

// 全连接队列
// (没有单独的结构体, 用链表头 + 长度计数表示)
// sk->sk_ack_backlog     = 当前全连接队列长度
// sk->sk_max_ack_backlog = 全连接队列最大值 (backlog 参数)
```

##### 2.3.4.3 队列数据从哪里来

```c
// 半连接队列的每个元素: struct request_sock
// 由 tcp_v4_conn_request() 在收到 SYN 时创建
struct request_sock {
    struct request_sock      *dl_next;        // 哈希链表
    u16                      mss;            // 客户端通告的 MSS
    u8                       num_retrans;    // SYN+ACK 重传次数
    u8                       cookie_ts;      // SYN Cookie 标记
    /* 以下由 req.tcp_request_sock_ops 填充: */
    u32                      ir_isn;         // 初始序列号 (服务端)
    u32                      ir_rcv_nxt;     // 期待的下一个序列号
    struct ipv6_pinfo        ...             // IP 层信息
};

// 全连接队列的每个元素: 同一个 struct request_sock
// 三次握手完成后, 从 syn_table 移到 accept_queue
// 直到 accept() 取出, 才在此基础上创建真正的 struct sock
```

##### 2.3.4.4 队列大小限制

```
半连接队列上限 = min(backlog, net.core.somaxconn, tcp_max_syn_backlog)
                  但实际由 tcp_max_syn_backlog 主导
                  /proc/sys/net/ipv4/tcp_max_syn_backlog (默认 128/256/1024)

全连接队列上限 = min(backlog, net.core.somaxconn)
                  /proc/sys/net/core/somaxconn (默认 4096, 旧内核 128)

检查队列状况:
  $ ss -tlnp
  State    Recv-Q Send-Q  Local Address:Port
  LISTEN   0      128     0.0.0.0:8080
              │      │
              │      └── 全连接队列上限 (Send-Q = sk_max_ack_backlog)
              └── 当前全连接队列中等待 accept 的连接数
```

##### 2.3.4.5 队列满了会发生什么

| 场景 | 现象 | 内核行为 |
|------|------|----------|
| 半连接队列满 | 新 SYN 被丢弃，客户端 connect 超时 | 内核直接丢掉 SYN 包，或开启 SYN Cookie |
| 全连接队列满 | 三次握手已经完成但 accept 来不及取 | 内核会保留 ESTABLISHED 连接，但不再增加队列。<br>若 `tcp_abort_on_overflow` 为 1，则发 RST 断开。<br>客户端看连接已建立，但服务端不会回复数据。 |

##### 2.3.4.6 SYN Flood 攻击与 SYN Cookie

```
攻击: 攻击者发送大量 SYN, 但从不回复 SYN+ACK 的 ACK
结果: 半连接队列被填满, 正常用户的 SYN 被丢弃

防御——SYN Cookie (默认开启):
  net.ipv4.tcp_syncookies = 1

SYN Cookie 原理:
  不存储 request_sock 到半连接队列
  而是把连接信息编码到 SYN+ACK 的序列号中:
    seq = hash(saddr, daddr, sport, dport, secret) + t << 1 + MSS

  客户端回复 ACK 时 (seq=seq+1):
    服务端从 ACK 的 seq-1 解码出原始信息
    无需查找半连接队列, 直接建立连接

  CPU 代价: 解码计算 (极低)
  代价: 丢失部分 TCP 选项 (如大窗口、SACK)
```

---

#### 2.3.5 文件描述符 (fd) 到底是什么

##### 2.3.5.1 一句话本质

**文件描述符是一个整数，它是当前进程的 fdtable 数组的索引。通过这个索引，内核可以找到对应的 `struct file`，从而找到 read/write 的具体实现函数。**

---

###### 2.3.5.1.1 白话版：fd 到底是什么

**用超市储物柜来理解最直观。**

你进超市，找到一排储物柜，选一个空柜子，把包放进去，柜门关上，弹出一张**小纸条**——上面印着一个数字，比如 **`5`**。

- 这张纸条就是 **fd（文件描述符）**——一个数字，`5`。
- 储物柜本身（包括里面的包）就是 **`struct file` + `socket` + `sock`**——真正的数据放在里面。
- 你拿着纸条去取包时，保安看一眼纸条上的数字，走到 **`5` 号柜**，打开门把包递给你。

**对应到计算机：**

| 现实 | 计算机 |
|------|--------|
| 纸条上的数字 `5` | fd 整数 `5` |
| 保安看纸条找柜子 | 内核通过 fd 查 fdtable，找到 fd[5] 对应的 `struct file*` |
| 5 号储物柜 | `struct file` 对象（里面有 `private_data` 指向 `socket`） |
| 储物柜里的包 | `sk_buff` 数据包（在 `sk_receive_queue` 里排队） |
| 保安开门递包 | `skb_copy_datagram_msg()` 把数据从内核拷贝到你的程序 |
| 你把包放回去再取 | 不 `close(fd)`，反复 `read/write` —— 长连接 |

**关键点，必须记住：**

**`fd = 5` 这个数字本身不存储任何数据。** 它只是一个编号，让内核知道要去 fdtable[5] 找对应的 `struct file` 指针。就像储物柜纸条上的数字——纸条本身不装你的包，它只是告诉你柜子在哪。

所以：
- `accept()` 返回 `conn = 5` → 内核悄悄在 fdtable[5] 存了一个指针，指向新创建的 TCP 连接对象
- `conn.recv(4096)` → 内核用 `5` 在 fdtable 中找到那个 TCP 连接，从它的接收缓冲区取数据
- `conn.close()` → 内核清空 fdtable[5]，释放 TCP 连接对象

**这就是为什么 socket 可以用 read/write 来读写。** 因为内核把 socket 包装成了一个"文件"——fdtable[fd] 指向的 `struct file` 的 `f_op` 被替换成了 `socket_file_ops`，它的 `read` 实际指向 `tcp_recvmsg()`，`write` 指向 `tcp_sendmsg()`。所以 `read(fd)` 其实是在读网卡，`write(fd)` 其实是在写网卡——但你用起来的感觉和读写普通文件一模一样。

##### 2.3.5.2 三层结构——task_struct / files_struct / fdtable 详解

**全链路全景图（从进程到内核 TCP 连接）：**

```
用户空间 (进程)                     内核空间
                                    
进程 A 的 task_struct                                  
  ┌─────────────────────┐         ┌──────────────────────┐
  │ files_struct        │         │  fdtable             │
  │   ┌─────────────┐   │         │  ┌────────┐          │
  │   │ fdt (指针)  │───┼────────►│  │ [0]    │───► /dev/stdin
  │   └─────────────┘   │         │  ├────────┤          │
  └─────────────────────┘         │  │ [1]    │───► /dev/stdout
                                  │  ├────────┤          │
                                  │  │ [2]    │───► /dev/stderr
                                  │  ├────────┤          │
                                  │  │ [3]    │───► struct file (监听 socket)
                                  │  ├────────┤          │
                                  │  │ [4]    │───► struct file (客户端连接)
                                  │  ├────────┤          │
                                  │  │ ...    │          │
                                  └──┴────────┘          │
                                                          │
                     ┌────────────────────────────────────┘
                     ▼
          ┌──────────────────┐
          │ struct file      │← 每次 open/accept 分配一个
          ├──────────────────┤                        ┌───────────────┐
          │ f_count = 1      │← 引用计数             │ sk_buff 链表   │
          │ f_flags          │← O_RDONLY / O_NONBLOCK│  ┌───┐ ┌───┐  │
          │ f_pos            │← 读写偏移 (socket 忽略)│  │   │→│   │  │
          │ f_op =           │← 操作函数表           │  └───┘ └───┘  │
          │  socket_file_ops │  socket_read/write    │  sk_receive    │
          │ private_data =   │───► &socket           │  _queue        │
          └──────────────────┘                       └──────┬────────┘
                     │                                      │
                     ▼                                      │
          ┌──────────────────┐                              │
          │ struct socket    │                              │
          ├──────────────────┤                              │
          │ ops =            │──► inet_stream_ops            │
          │ sk (struct sock) │──┐                           │
          └──────────────────┘  │                           │
                                ▼                           │
          ┌──────────────────────────────────┐              │
          │ struct sock (TCP)                │              │
          ├──────────────────────────────────┤              │
          │ sk_state = ESTABLISHED            │              │
          │ sk_rcvbuf = 87380                 │◄─────────────┘
          │ sk_sndbuf = 16384                 │
          │ sk_receive_queue ─── sk_buff 链表 │
          │ sk_write_queue   ─── sk_buff 链表 │
          │ sk_wait          ─── 等待队列     │
          └──────────────────────────────────┘
```

**图中各层的角色：**
- **`task_struct`** — 进程/线程在内核中的描述符（每进程一个）
- **`files_struct`** — 管理进程所有打开的文件描述符（每进程一个）
- **`fdtable`** — fd 整数 → `struct file*` 的映射数组（每个 files_struct 一个）
- **`struct file`** — VFS 层的统一文件对象（每个打开的文件/socket 一个）
- **`struct socket`** — 网络通信的通用抽象
- **`struct sock`** — 具体协议（TCP）的状态和数据缓冲区

下面的各节逐一深入每个结构体的内部细节。

---

###### 2.3.5.2.1 task_struct——进程描述符

**是什么：** `task_struct` 是 Linux 内核中**每个进程/线程**的核心数据结构，定义在 `include/linux/sched.h`。它描述了一个进程的全部信息。

```c
// include/linux/sched.h — 每个进程一份, 约 2~4 KB
struct task_struct {
    // ── 进程标识 ──
    pid_t                pid;               // 进程 ID (唯一)
    pid_t                tgid;              // 线程组 ID (主线程的 pid)
    struct task_struct   *group_leader;     // 线程组 leader

    // ── 状态 ──
    volatile long        state;             // TASK_RUNNING / TASK_INTERRUPTIBLE 等
    int                  exit_state;        // EXIT_ZOMBIE / EXIT_DEAD

    // ── 文件系统 ──
    struct files_struct  *files;            // ← 指向当前进程的 fd 管理结构 (本节核心)

    // ── 内存 ──
    struct mm_struct     *mm;               // 进程地址空间 (页表)

    // ── 调度 ──
    int                  prio;              // 动态优先级
    unsigned int         time_slice;        // 时间片

    // ── 信号 ──
    struct signal_struct *signal;           // 信号处理

    // ── 网络 ── (以下与 socket 直接相关)
    struct fs_struct     *fs;               // 文件系统信息
    struct namespace     *nsproxy;          // 命名空间 (每个 container 一份)

    // ... 总共约 200+ 字段, 大小约 2~4 KB
};
```

**有几个：** 系统中**每进程(线程)一个**。`ps aux` 看到的每一个条目背后都是一个 `task_struct`。1000 个线程 = 1000 个 `task_struct`。

**在哪里：** 所有 `task_struct` 通过双向链表串联，内核用 `pid` 哈希表快速查找。当前运行的进程通过 `current` 宏（指向 `per-CPU` 变量）访问。

---

###### 2.3.5.2.2 files_struct——进程的文件描述符管理器

**是什么：** `files_struct` 是每个进程独有的、**管理所有打开的文件描述符的容器**，定义在 `include/linux/fdtable.h`。它包含了 fdtable、计数和锁。

```c
// include/linux/fdtable.h — 每个进程一份
struct files_struct {
    // ── 原子引用计数 ──
    atomic_t             count;             // 引用计数 (fork 时父子进程共享, count++)

    // ── fdtable 指针 (真正的核心) ──
    struct fdtable       *fdt;              // ← 主 fdtable 指针 (指向动态分配的 fdtable)
    struct fdtable       fdtab;             // ← 嵌入式 fdtable (小规模时直接用, 避免分配)

    // ── 锁 ──
    spinlock_t           file_lock;         // 保护 fdtable 并发访问的自旋锁

    // ── 缓存加速 ──
    int                  next_fd;           // 下一个可用的 fd (分配加速, 不必从头扫描)
    unsigned long        close_on_exec;     // bitmask: 哪些 fd 在 exec 时自动关闭
    unsigned long        open_fds;          // bitmask: 哪些 fd 当前已打开
    unsigned long        full_fds_bits[1];  // 用于快速跳过已满的 64-fd 块

    // ── 嵌入式 fd 数组 (前 64 个 fd, 小规模优化) ──
    // struct file __rcu *fd_array[NR_OPEN_DEFAULT];  // NR_OPEN_DEFAULT = 64
    // 当进程打开的 fd ≤ 64 时, fd_array 直接嵌入在 files_struct 中
    // 无需额外分配内存, 也无需额外指针解引用
} __randomize_layout;
```

**有几个：** 默认**每进程一个**。但 `fork()` 时父子进程**共享同一个** `files_struct`（`count++`），直到任何一方修改 fd 时才会 `copy_files()` 复制一份（COW 语义）。

**设计要点：**
- 嵌入式 `fdtab` 处理小规模 fd（≤64），避免额外内存分配
- `fdt` 指针在需要扩容时指向新分配的 `fdtable`（fd > 64 时）
- `count` 字段支持 fork 后父子进程共享 fdtable

---

###### 2.3.5.2.3 fdtable (fdt)——文件描述符表

**是什么：** `fdtable` 是真正的**fd 数字 → struct file 指针**的映射表，定义在 `include/linux/fdtable.h`。它就是一个数组 + 辅助元数据。

```c
// include/linux/fdtable.h
struct fdtable {
    unsigned int         max_fds;           // 当前数组容量 (fd 最大有效值 + 1)
    struct file __rcu   **fd;               // fd 指针数组: fd[0] ~ fd[max_fds-1]
                                            // 每个元素是一个 struct file*
                                            // 空 = NULL, 有效 = 指向 struct file
    unsigned long       *close_on_exec;     // bitmask 数组 (按位标记 exec 时关闭的 fd)
    unsigned long       *open_fds;          // bitmask 数组 (按位标记已打开的 fd)
    unsigned long       *full_fds_bits;     // 每 64 个 fd 为一个块, 标记是否已满
    struct rcu_head     rcu;                // RCU 回调 (用于无锁读 + 延迟释放)
};
```

###### 2.3.5.2.4 三层串联关系——直观总结

```
┌─────────────────────────────────────────────────────────────────────┐
│                       整个系统                                       │
│   ┌───────────────┐  ┌───────────────┐  ┌───────────────┐         │
│   │  task_struct  │  │  task_struct  │  │  task_struct  │  ...     │
│   │  PID=1000     │  │  PID=1001     │  │  PID=1002     │         │
│   │  ┌─────────┐  │  │  ┌─────────┐  │  │  ┌─────────┐  │         │
│   │  │ files   │──┼──│──│ files   │──┼──│──│ files   │  │         │
│   │  └────┬────┘  │  │  └────┬────┘  │  │  └────┬────┘  │         │
│   └───────┼───────┘  └───────┼───────┘  └───────┼───────┘         │
│           │                  │                  │                  │
│           ▼                  ▼                  ▼                  │
│   ┌───────────────┐  ┌───────────────┐  ┌───────────────┐         │
│   │ files_struct  │  │ files_struct  │  │ files_struct  │         │
│   │ count=1       │  │ count=2       │  │ count=1       │         │
│   │ ┌───────────┐ │  │ ┌───────────┐ │  │ ┌───────────┐ │         │
│   │ │ fdt  ──────┼─│──│─│ fdt  ─────┼─│──│─│ fdt       │ │         │
│   │ └───────────┘ │  │ └───────────┘ │  │ └───────────┘ │         │
│   └───────┼───────┘  └───────┼───────┘  └───────┼───────┘         │
│           │                  │  (fork 后共享)    │                  │
│           ▼                  ▼                  ▼                  │
│   ┌───────────────┐  ┌───────────────┐  ┌───────────────┐         │
│   │   fdtable     │  │   fdtable     │  │   fdtable     │         │
│   │ max_fds=64    │  │ max_fds=128   │  │ max_fds=256   │         │
│   │               │  │               │  │               │         │
│   │ fd[0] → stdin │  │ fd[0] → stdin │  │ fd[0] → stdin │         │
│   │ fd[1] → stdout│  │ fd[1] → stdout│  │ fd[1] → stdout│         │
│   │ fd[2] → stderr│  │ fd[2] → stderr│  │ fd[2] → stderr│         │
│   │ fd[3] → sock  │  │ fd[3] → sockA │  │ fd[3] → sock  │         │
│   │               │  │ fd[4] → sockB │  │ fd[4] → file  │         │
│   │               │  │ fd[5] → file  │  │ ...           │         │
│   └───────────────┘  └───────────────┘  └───────────────┘         │
│                                                                     │
│  每个 fdtable 中的指针指向:                                         │
│      struct file (约 256 字节)                                      │
│         └── private_data → struct socket (约 128 字节)               │
│                               └── sk → struct sock (约 1.5 KB)       │
└─────────────────────────────────────────────────────────────────────┘
```

###### 2.3.5.2.5 数量关系总结

| 结构体 | 数量 | 存放位置 | 大小 |
|--------|------|----------|------|
| `task_struct` | **每进程/线程一个** | slab cache `task_struct_cache` | ~2~4 KB |
| `files_struct` | **每个独立 fd 空间一个**（默认每进程一个，fork 共享） | slab cache `files_cache` | ~1 KB (不含 fd 数组) |
| `fdtable` | **每个 files_struct 一个**（动态扩容时替换） | 随 files_struct 嵌入式或单独 slab 分配 | 变长: fd 数组 × 8 字节 |
| `struct file` | **每个打开的文件/socket 一个** | slab cache `filp_cache` | ~256 字节 |
| `struct socket` | **每个 socket 一个** | slab cache `sock_inode_cache` | ~128 字节 |
| `struct sock` | **每个 TCP 连接一个** | slab cache `tcp_sock` | ~1.5 KB |

**关键理解：**
- 1 个 `task_struct` → 1 个 `files_struct` → 1 个 `fdtable` → N 个 `struct file`（N = 打开的 fd 数）
- `fdt` 只是 `files_struct` 中的一个指针字段，它指向 `fdtable`
- 每个 fd 背后的完整链路: `fd(int)` → `fdtable.fd[fd]` → `struct file` → `private_data` → `struct socket` → `sk` → `struct sock`
- 这之间的多级间接引用并非多余——每一层解决一个不同的问题:
  - **`fdtable`** 管理 fd 到 file 的映射和容量扩容
  - **`struct file`** 提供统一 VFS 接口 (read/write 对所有"文件"类型一致)
  - **`struct socket`** 封装网络通信语义
  - **`struct sock`** 封装 TCP 协议状态

##### 2.3.5.3 fd 数字分配规则

```c
// 内核 fs/file.c 中的分配算法
// 1. 从 fdtable 中找到最小可用 fd
// 2. 如果超出当前 fdtable 大小, 扩展 fdtable
// 3. 扩展: 新 fdtable 大小 = 2^(原有大小的对数值 + 1)
//    例如: 32 → 64 → 128 → 256 → ... → NR_OPEN 上限

// 所以前 1024 个 fd 的分配顺序:
// stdin (0), stdout (1), stderr (2), 第一个 socket (3), ...
```

##### 2.3.5.4 fd 与 socket 的关系——为什么 socket 可以用 read/write

```c
// 关键: struct file 的 f_op 指向 socket_file_ops
// 这使得所有 VFS 层的操作都重定向到 socket 实现

static const struct file_operations socket_file_ops = {
    .read_iter  = sock_read_iter,    // 等价于 recvmsg
    .write_iter = sock_write_iter,   // 等价于 sendmsg
    .poll       = sock_poll,         // 等价于 select/poll/epoll
    .ioctl      = sock_ioctl,        // 等价于 setsockopt/getsockopt
    .release    = sock_close,        // 等价于 close
    .mmap       = sock_mmap,         // 可用于零拷贝
};

// 所以当你调用:
int fd = accept(...);          // 返回数字 N
read(fd, buf, 1024);           // 实际调用 socket_file_ops->read_iter
                               // → tcp_recvmsg() → 从 sk_receive_queue 取数据
write(fd, buf, 1024);          // 实际调用 socket_file_ops->write_iter
                               // → tcp_sendmsg() → 数据进入 sk_write_queue
close(fd);                     // 实际调用 socket_file_ops->release
                               // → tcp_close() → 四次挥手 + 释放资源
```

##### 2.3.5.5 为什么 fd 从 3 开始

```c
// 每个进程默认打开 3 个 fd:
fd 0 = stdin  (键盘输入)
fd 1 = stdout (终端输出)
fd 2 = stderr (终端错误输出)

// 所以第一个 socket 得到 fd 3
// 如果程序关闭了 stdin (close(0)), 下一个打开的 fd 会复用 0
```

##### 2.3.5.6 fd 的限制如何突破

```bash
# 查看当前进程的 fd 限制
ulimit -n
# 查看所有进程的 fd 使用量
cat /proc/sys/fs/file-nr
# 查看特定进程的 fd 列表
ls -la /proc/<pid>/fd/

# 提高限制 (需要 CAP_SYS_RESOURCE 或 root)
ulimit -n 65535              # 临时修改
echo "65535" > /proc/sys/fs/file-max  # 系统级

# 永久修改 (/etc/security/limits.conf)
# * soft nofile 65535
# * hard nofile 65535

# 查看 socket 内存使用
ss -m  # 显示每个 socket 的内存占用
```

### 2.4 完整请求流程（纯 socket 层）

```
Client                          Server
  │                               │
  │────── connect :80 ──────────►│  TCP 三次握手开始
  │                               │  半连接队列 SYN_RCVD
  │◄──── SYN+ACK ───────────────│
  │────── ACK ─────────────────►│  全连接队列 ESTABLISHED
  │                               │
  │                               │  accept() 从全连接队列取出
  │                               │  返回新 fd=5
  │────── GET / HTTP/1.1 ──────►│  read(fd=5)
  │                               │  内核从网卡 → DMA → ring buffer
  │                               │  → 软中断 → TCP 协议栈
  │                               │  → socket 接收缓冲区 → 用户空间
  │◄──── HTTP/1.1 200 OK ───────│  write(fd=5)
  │                               │  用户数据 → 内核发送缓冲区
  │                               │  → TCP 分段 → IP 分片
  │                               │  → 网卡发送
  │────── FIN ─────────────────►│  close() / 四次挥手
```

### 2.5 从 0 实现代码

#### 2.5.1 Step 1：最简单 Socket Server

```python
import socket

# Step 1: 创建 TCP socket
# AF_INET = IPv4, SOCK_STREAM = TCP
# 内核: 创建 socket 对象, 分配 fd
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Step 2: 允许地址重用 (避免 TIME_WAIT 导致 bind 失败)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

# Step 3: 绑定到 0.0.0.0:8080
# 内核: 检查端口, 注册协议控制块
server.bind(('0.0.0.0', 8080))

# Step 4: 开始监听, backlog=128 是全连接队列大小
# 内核: 创建半连接/全连接队列, 状态 → LISTEN
server.listen(128)

print("Server listening on :8080")

# Step 5: 永不退出的 while 循环
while True:
    # Step 6: 阻塞等待连接
    # 内核: 若全连接队列空, 进程睡眠; 有连接则唤醒
    # 返回新 fd (conn) 和对端地址
    conn, addr = server.accept()
    print(f"Connection from {addr}")

    # Step 7: 读取 HTTP 请求
    # 内核: 从 socket 接收缓冲区拷贝到用户空间
    data = conn.recv(4096)
    print(f"Received: {data[:100]}...")

    # Step 8: 返回 HTTP 响应
    response = b"HTTP/1.1 200 OK\r\nContent-Length: 13\r\n\r\nHello, World!"
    conn.send(response)

    # Step 9: 关闭连接
    # 内核: 触发 TCP 四次挥手
    conn.close()
```

**运行测试：**
```bash
python server.py &
curl http://localhost:8080
# → Hello, World!
```

**整体流程图：从 socket 创建到连接关闭**

下面这张图把整个代码的每一行和内核内部发生的事对应起来：

```
┌─────────────────────────────────────────────────────────────────────┐
│                    用户空间 (Python 进程)                            │
│                                                                     │
│  server = socket()      server.listen(128)                          │
│  └── 内核: 创建 socket     └── 内核: 创建半连接队列 + 全连接队列    │
│       分配 fd=3                 状态 → LISTEN                        │
│                                                                     │
│  while True:                                                        │
│    conn, addr = server.accept()  ←─── 阻塞等待 (全连接队列为空)     │
│    │                                  ↑       │                     │
│    │    ┌──────────────────────────────┘       │                     │
│    │    │ 有新连接 → 全连接队列取出            │                     │
│    │    │ 内核: 创建新 fd=4                    │                     │
│    │    │       sk_receive_queue + sk_write_queue                   │
│    │    │       tcp_rmem + tcp_wmem 分配                          │
│    │    │                                                          │
│    ├── data = conn.recv(4096)                                      │
│    │    └── 内核: 从 sk_receive_queue 拷贝数据到用户 buffer          │
│    │                                                               │
│    ├── conn.send(response)                                         │
│    │    └── 内核: 数据拷贝到 sk_write_queue → tcp_write_xmit()     │
│    │                                                               │
│    └── conn.close()                                                │
│         └── 内核: 四次挥手 → 释放 fd=4, 释放 sk_receive/write_queue│
│                                                                     │
│  下一个连接 accept() → 拿到 fd=4 (刚释放的)                          │
└─────────────────────────────────────────────────────────────────────┘
         │                          │
         ▼                          ▼
┌─────────────────┐      ┌──────────────────────┐
│ 内核协议栈       │      │   全连接队列          │
│ 三次握手完成后    │      │   (accept 等在这)    │
│ → 全连接队列 +1   │      │                      │
└─────────────────┘      │ conn1 | conn2 | ...   │
                         │ fd=4  | fd=5  |       │
                         └──────────┬───────────┘
                                    │
                             ┌──────▼──────┐
                             │ 三次握手完成  │
                             │ TCP 协议栈    │
                             │ 软中断上下文中 │
                             └──────────────┘
```

**时间线视角——curl 和你的服务器怎么交互：**

```
curl                              Python 服务器                          内核
  │                                   │                                    │
  │  socket()                         │  socket()                          │
  │  connect(":8080")                 │  bind() + listen()                 │
  │       │                           │       │                            │
  │       │          ── SYN ──────────┼───────┼───────────────────────────►│
  │       │                           │       │  半连接 (SYN_RCVD) +1      │
  │       │          ◄── SYN+ACK ─────┼───────┼────────────────────────────│
  │       │                           │       │                            │
  │       │          ── ACK ──────────┼───────┼───────────────────────────►│
  │       │                           │       │  半连接 -1                  │
  │       │                           │       │  全连接 +1                  │
  │       │                           │       │  唤醒 accept               │
  │       │                           │       │                            │
  │       │                           │◄──────┤  accept() 取出             │
  │       │                           │  conn │  全连接 -1                  │
  │       │                           │ fd=4  │  创建 socket/sock           │
  │       │                           │       │                            │
  │  ── GET / ────────────────────────┼───────┼───────────────────────────►│
  │       │                           │       │  sk_receive_queue +1       │
  │       │                           │◄──────┤  recv() 取出               │
  │       │                           │       │                            │
  │       │◄── HTTP 200 OK ───────────┼───────┼────────────────────────────│
  │       │          send()           │       │  sk_write_queue 排队        │
  │       │                           │       │  tcp_write_xmit() 发送     │
  │       │                           │       │                            │
  │       │◄── FIN ───────────────────┼───────┼────────────────────────────│
  │       │          close()          │       │  四次挥手                   │
  │       │                           │       │  TIME_WAIT (60s) → CLOSED  │
  │       │                           │       │  fd=4 释放                  │
```

**针对这个例子，回答几个关键问题：**

**Q1: 每次 `accept()` 返回的是一个新的 fd 吗？**

**是的。** 假设 `server.fileno() = 3`（socket fd），那么：

```
第 1 次 accept() → 返回 conn.fileno() = 4
第 2 次 accept() → 返回 conn.fileno() = 5
第 3 次 accept() → 返回 conn.fileno() = 6
...
```

`server` fd 是**监听 fd**，它始终是 `3`，不会被消耗。每次 `accept()` 从全连接队列取出一个已完成三次握手的连接，内核为它创建一个新 `struct file`，放入当前进程中第一个空闲的 fd 槽位。

**Q2: 之前的连接 fd 呢？**

**在每次循环末尾被 `conn.close()` 关闭了。** 这个例子是**串行服务器**——同一时间只处理一个连接：

```
while True:
    conn, addr = server.accept()   # 等 → 拿到 fd=4
    data = conn.recv(4096)          # 收请求
    conn.send(response)             # 发响应
    conn.close()                    # 关闭 fd=4 (四次挥手)

    # 此时 fd=4 已释放, 下一次 accept 会拿到 fd=4 (被复用)
    conn, addr = server.accept()   # 拿到 fd=4 (刚才释放的)
```

**所以 fd 会循环使用**：4 → 5 → 4 → 5 → 6 → ... 取决于什么顺序被 close 释放。

**Q3: 如果之前连接还没结束，又来了新连接怎么办？**

在这个串行例子里：**等。**

```
while True:
    conn, addr = server.accept()    # ← 阻塞在这
    ... recv + send + close ...

# 第 1 个连接没处理完 → while 没回到 accept → 新连接在全连接队列里排队
# 队列满了 (backlog=128) → 内核直接丢包
```

新连接的三次握手**可以正常完成**（TCP 协议栈在中断上下文处理，不依赖你的 Python 代码），握手完成后的连接会进入全连接队列等待 `accept()`。但如果你的程序一直不 `accept()`（比如卡在某次 recv 上），全连接队列堆满，后面的新连接三次握手完成后也入不了队——内核会发 RST 断开。

**这就是串行模型的致命弱点：一个慢连接会阻塞所有后续连接。**

**Q4: 同一个连接上，消息怎么交互？**

`accept()` 返回的 `conn` fd 代表一个已建立的 TCP 连接。在这个连接上：

```
# 同一个 fd (比如 4), 可以反复 recv / send
conn, addr = server.accept()   # fd = 4

# 在 HTTP keep-alive 场景下:
while True:
    data = conn.recv(4096)     # 从 fd=4 的接收队列读
    if not data:               # 对端关闭了
        break
    conn.send(response)        # 写入 fd=4 的发送队列
```

同一个 `conn` fd 对应内核里同一个 `socket` → `sock` 结构体，它的 `sk_receive_queue` 和 `sk_write_queue` 会累积所有来自/发往对端的数据。HTTP keep-alive 就是在同一个 fd 上反复收发多条请求/响应。

**Q5: curl 测试时发生了什么？**

```
你运行 curl http://localhost:8080
     │
     ├── 1. curl 创建 socket → connect("127.0.0.1:8080")
     │        三次握手 → 全连接队列 +1
     │
     ├── 2. 你的 Python server.accept() 醒来
     │        全连接队列 -1, 返回 conn (fd=4)
     │
     ├── 3. curl 发送 "GET / HTTP/1.1\r\n..."
     │        → 内核放到 conn 的 sk_receive_queue
     │        → conn.recv(4096) 取出
     │
     ├── 4. conn.send(response)
     │        → 内核放到 sk_write_queue
     │        → TCP 协议栈发往 curl
     │
     └── 5. conn.close()
             → 四次挥手 → fd=4 释放 → curl 收到 FIN → 退出
```

每个箭头都对应一次用户态 ↔ 内核态的切换和内核内部的数据流动。

#### 2.5.2 为什么这样设计？

- **socket + bind + listen 是标准三部曲**：socket 创建通信端点，bind 绑定身份（端口），listen 表示愿意接受连接——这是 UNIX 网络编程几十年沉淀的 API 设计。
- **阻塞 accept**：最简单，但每个连接只能串行处理。后面的连接必须等前面的处理完。
- **recv/send vs read/write**：recv/send 提供 flags 参数，更灵活，底层都是 `sock_read/sock_write`。

---

## 3. HTTP 协议解析——与浏览器对话

### 3.1 一句话本质

**HTTP 协议是基于 TCP 的文本协议——请求和响应都是按照固定格式编排的 ASCII 字符串，解析就是按规则切割这段文本。**

### 3.2 生活类比

HTTP 请求 = **填写快递单**：

```
POST /api/order HTTP/1.1           ← 请求行 = 快递单抬头 (方法 + 地址 + 版本)
Host: example.com                   ←
Content-Type: application/json     ←  请求头 = 快递选项 (加急/保价/到付)
Content-Length: 27                 ←
Authorization: Bearer xxx          ←
                                    ← 空行 = 分隔线
{"item":"book","qty":2}            ← Body = 包裹内容
```

### 3.3 技术原理

**HTTP 请求格式：**
```
<method> <path> <version>\r\n        ← 请求行
<headername>: <headervalue>\r\n      ← 请求头 (0个或多个)
<headername>: <headervalue>\r\n
\r\n                                 ← 空行 (headers 结束标志)
<body>                               ← 请求体 (可选)
```

**HTTP 响应格式：**
```
<version> <status_code> <reason>\r\n ← 状态行
<headername>: <headervalue>\r\n      ← 响应头
\r\n                                 ← 空行
<body>                               ← 响应体
```

**解析核心逻辑：**
```
1. 读取直到 \r\n → 得到请求行
2. split(" ") → method, path, version
3. 逐行读取直到 \r\n\r\n → 得到所有 headers
4. 每个 header split(": ") → key, value
5. 根据 Content-Length / Transfer-Encoding 读取 body
```

**关键边界情况（画图说明）：**

---

#### 3.3.1 TCP 粘包——一次 recv 收到多个请求

TCP 是流，不是消息。`send()` 两次，`recv()` 可能一次全收到：

```
客户端 send 两次:             服务器 recv 一次:

send("GET /a HTTP/1.1\r\n")   ──┐
                                │  TCP 协议栈把两次发送
send("Host: x\r\n\r\n")       ──┘  的数据合并成一个包

                                ▼
服务器 recv(4096) 收到:
"GET /a HTTP/1.1\r\nHost: x\r\n\r\n"
↑ 一个完整的请求
```

如果两次 send 间隔很短，还可能两个完整请求粘在一起：

```
客户端:
send("GET /a HTTP/1.1\r\n\r\n")    ← 请求 1 (GET 无 body)
send("POST /b HTTP/1.1\r\n\r\n")   ← 请求 2

服务器 recv(4096) 收到:
"GET /a HTTP/1.1\r\n\r\nPOST /b HTTP/1.1\r\n\r\n"
↑__________________↑↑__________________↑
    请求 1 完整        请求 2 紧接着开始
```

**错误做法：** 认为 `recv()` 一次恰好返回一个请求。

```python
# 错误代码 (会丢请求):
data = conn.recv(4096)
req = parse_one_request(data)  # 只解析了请求 1
# 请求 2 还在 data 末尾, 被丢弃了!
```

**正确做法：** 每次都从 buffer 循环解析，直到 buffer 不够一个完整请求。

```python
# 正确代码 (粘包处理):
buffer = b""
while True:
    data = conn.recv(4096)
    if not data:
        break
    buffer += data                              # 追加到 buffer
    while True:
        req = parser.parse()                    # 从 buffer 头部解析
        if req is None:                         # buffer 不够 → 等更多数据
            break
        handler(req)                            # 取出一个完整请求, 处理它
        # 继续循环, 尝试从 buffer 解析下一个请求
```

粘包时 `parse()` 会依次从 buffer 取出请求 1、请求 2、...，直到 buffer 被耗尽。

---

#### 3.3.2 TCP 半包——一次 recv 只收到半个请求

比粘包更常见。请求太大或网络慢时，一个请求被切成多段到达：

```
真实的请求 (74 字节):
  GET /very-long-path HTTP/1.1\r\n
  Host: example.com\r\n
  Content-Length: 20\r\n
  \r\n
  {"key":"hello world"}
```

网络逐段送达（分 3 次 recv）：

```
第 1 次 recv:
"GET /very-long-path HTTP/1.1\r\nHost: exam"
↑ 请求行完整, 但 headers 没完, body 还没开始

第 2 次 recv:
"ple.com\r\nContent-Length: 20\r\n\r\n{\"key\":\"hel"
↑ headers 完整了, 但 body 才收到一半 (10/20 字节)

第 3 次 recv:
"lo world\"}"
↑ body 收完剩下的 10 字节, 整个请求完整了
```

**错误做法：** 假设 `\r\n` 一定在 recv 的末尾，或者假设一次 recv 一定包含完整的 header 行。

```python
# 错误代码 (会崩):
def parse(data):
    lines = data.split(b"\r\n")          # ← 第 1 次 recv 时 data 以 "exam" 结尾
                                         #    split 会得到不完整的最后一行
    method, path, ver = lines[0].split()  # ← 这条可能没问题
    for line in lines[1:]:                # ← 遍历到 "exam", 它不含 ":" → 崩
        key, val = line.split(b": ")
```

**正确做法：** 状态机——每次只从 buffer 头部检查当前状态需要的数据是否足够。

```python
# 正确代码 (半包处理 - 状态机核心逻辑):
class HTTPRequestParser:
    def parse(self):
        # REQUEST_LINE 阶段: 需要找到 \r\n
        if self.state == "REQUEST_LINE":
            idx = self.buffer.find(b"\r\n")
            if idx == -1:           # ← 没有 \r\n → 半包! 等下一次 recv
                return None
            line = self.buffer[:idx]
            self.buffer = self.buffer[idx+2:]
            parts = line.split(b" ")
            self.request.method = parts[0].decode()
            self.request.path = parts[1].decode()
            self.state = "HEADERS"

        # HEADERS 阶段: 逐行读取, 直到空行
        if self.state == "HEADERS":
            while True:
                idx = self.buffer.find(b"\r\n")
                if idx == -1:           # ← 还没收到完整 header 行 → 半包
                    return None
                if idx == 0:            # ← 空行, headers 结束
                    self.buffer = self.buffer[2:]
                    self.state = "BODY"
                    break
                # ... 解析 header 行 ...

        # BODY 阶段: 需要够 Content-Length 字节
        if self.state == "BODY":
            cl = int(self.request.headers.get("Content-Length", 0))
            if len(self.buffer) < cl:   # ← body 还没收够 → 半包
                return None             #    等下一次 recv
            self.request.body = self.buffer[:cl]
            self.buffer = self.buffer[cl:]
            self.state = "DONE"

        if self.state == "DONE":
            return self.request
        return None
```

**关键：** 每次发现"数据不够"就返回 `None`，外界等下一次 `recv()` 后再调 `feed()` + `parse()`。

---

#### 3.3.3 body 不存在——GET 请求没有 body

```
请求行: GET / HTTP/1.1\r\n
Headers: Host: x\r\n
         \r\n                         ← 空行后直接结束, 没有 body
```

解析器检测到 `method == "GET"`，知道 body 长度为 0，解析完 headers 直接切到 DONE。

```python
# 正确代码: 跳过 body 阶段
if self.state == "HEADERS":
    # ... 解析 headers, 遇到空行 ...
    # 判断: 如果 method 不允许有 body, 直接跳到 DONE
    if self.request.method in ("GET", "HEAD", "DELETE", "OPTIONS", "TRACE"):
        self.state = "DONE"
        self.request.body = b""
    else:
        # POST / PUT / PATCH → 需要读 body
        self.state = "BODY"
        self._body_remaining = int(self.request.headers.get("Content-Length", 0))
```

```python
# 错误做法 (卡死):
if self.state == "BODY":
    cl = int(self.request.headers.get("Content-Length", 0))
    # 对于 GET 请求, Content-Length 不存在 → int(None) 抛异常!
    # 或者没设 Content-Length 头 → cl = 0 → len(buffer) >= 0 永远成立
    # 但如果客户端错误地传了 Content-Length: 100, 解析器会一直等
```

---

#### 3.3.4 Content-Length 与 chunked 互斥

一个请求或响应里，body 的编码方式只能选一种：

```
两种方式, 不能同时存在:

① Content-Length: N
   ────────────────
   body 正好 N 字节, 读完 N 字节就结束
   适用场景: 静态文件、小响应 (提前知道长度)

② Transfer-Encoding: chunked
   ──────────────────────────
   body 分成多块, 每块前面标长度, 最后一块 0 结束
   适用场景: 动态生成的大响应 (提前不知道长度)
```

**如果同时出现两个头，RFC 规定 `Transfer-Encoding` 优先级更高**——`Content-Length` 被忽略。

```
错误的服务器返回:
  HTTP/1.1 200 OK\r\n
  Content-Length: 100\r\n         ← 说 body 100 字节
  Transfer-Encoding: chunked\r\n  ← 又说用 chunked
  \r\n
  5\r\nHello\r\n0\r\n\r\n         ← 实际是 chunked

  客户端解析: 遵循 Transfer-Encoding → chunked 解析
  Content-Length: 100 直接被抛弃
```

```python
# 正确代码: headers 解析完成后判断 body 编码方式
if self.state == "HEADERS":
    # ... 解析到空行, headers 结束 ...
    # 判断 body 编码方式
    if "Transfer-Encoding" in self.request.headers:
        # Transfer-Encoding 优先级更高
        # 忽略 Content-Length
        self.state = "CHUNK_SIZE"          # ← 进入 chunked 子状态机
        self.chunk_state = "CHUNK_SIZE"
    elif "Content-Length" in self.request.headers:
        cl = int(self.request.headers["Content-Length"])
        if cl == 0:
            self.state = "DONE"            # ← body 不存在
        else:
            self.state = "BODY"
            self._body_remaining = cl
    else:
        # 两个头都没有 → body 长度为 0
        self.state = "DONE"
```

---

#### 3.3.5 Content-Length 与实际 body 不符

如果声明的 Content-Length 和实际收到的不一致，解析器怎么处理？

```
场景 A (body 比 Content-Length 短):
  Content-Length: 100
  body 实际只收到 80 字节 → 解析器 wait, 永远等不够 100
  → 连接卡死, 直到超时断开

场景 B (body 比 Content-Length 长):
  Content-Length: 5
  body 实际收到 "HelloWorld"
                  ↑____↑
                  body 取前 5 字节 "Hello", 剩下 "World" 在 buffer
                  buffer 里的 "World" 会被当作下一个请求的开始!
```

场景 B 很危险：

```
TCP 流: "POST /a HTTP/1.1\r\nContent-Length: 5\r\n\r\nHelloWorldPOST /b..."

解析器解析请求 A:
  看到 Content-Length: 5
  从 buffer 取前 5 字节 "Hello" 作为 body
  buffer 剩下 "WorldPOST /b..."

调用 handler:
  req.body = "Hello"    ← 完整, 看起来正常
  buffer 剩下 "World"   ← 脏数据!

解析器解析下一个请求:
  buffer 开头是 "WorldPOST /b..."  ← "World" 不是合法 HTTP 请求行!
  解析器报错或返回 400
```

**正确的做法：** 解析器严格按 Content-Length 取值，多出来的留在 buffer 等下一个请求。

```python
# 正确代码: 严格按 Content-Length 取值
if self.state == "BODY":
    cl = int(self.request.headers.get("Content-Length", 0))
    if len(self.buffer) < cl:
        return None                     # 不够 → 等下一次 recv
    # 只取前 cl 字节, 剩下的留在 buffer
    self.request.body = self.buffer[:cl]
    self.buffer = self.buffer[cl:]      # ← 多出来的留在 buffer!
    self.state = "DONE"
```

```python
# 如果担心恶意请求 (Content-Length 超大, 如 10GB):
# 设置每个请求的 body 上限
MAX_BODY_SIZE = 10 * 1024 * 1024  # 10MB

if self.state == "BODY":
    cl = int(self.request.headers.get("Content-Length", 0))
    if cl > MAX_BODY_SIZE:
        raise HTTPError(413, "Request Entity Too Large")  # ← 直接拒绝
    if len(self.buffer) < cl:
        return None
    self.request.body = self.buffer[:cl]
    self.buffer = self.buffer[cl:]
    self.state = "DONE"
```

---

#### 3.3.6 连接在中途断开

```
客户端 recv 到一半, Ctrl+C 关了:

你的服务器:
  data = conn.recv(4096)
  # 客户端已 FIN, 内核收到 EOF
  # data = b""  (空字节)
  # 这时必须退出循环, 不能继续读!
```

```python
# 正确处理:
while True:
    data = conn.recv(4096)
    if not data:          # ← 对端关闭了
        break             # ← 退出循环, 关闭连接
    parser.feed(data)
    # ... 继续解析
```

如果客户端在发送请求体的中间断开了，你**可能收到部分 body**——解析器应该检测到连接断开（recv 返回空），丢弃当前不完整的请求，关闭连接。

```python
# 更健壮的处理: 断连时丢弃不完整的请求
buffer = b""
parser = HTTPRequestParser()

while True:
    data = conn.recv(4096)
    if not data:                          # ← 连接断开
        if parser.state != "DONE":        # ← 请求还没解析完
            print("Incomplete request discarded")
            # parser 自动重置, 不处理半截请求
        break

    parser.feed(data)
    while True:
        req = parser.parse()
        if req is None:
            break
        # 完整请求, 正常处理
        response = handle_request(req)
        conn.send(response.to_bytes())
```

### 3.4 完整请求流程（HTTP 解析层）

```
                 TCP 流 (字节流, 无边界)
                       │
                       ▼
              ┌─────────────────┐
              │  接收缓冲区       │
              │ [GET /a HTTP...] │  ← 可能粘包
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │ 查找 \r\n        │  ← 第一次定位
              ├─────────────────┤
              │ GET /a HTTP/1.1 │  ← 请求行
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │ split(' ') 解析  │
              ├─────────────────┤
              │ method  = "GET" │
              │ path    = "/a"  │
              │ version = 1.1   │
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │ 逐行读取 headers │
              │ Host: x.com\r\n  │
              │ \r\n             │  ← 空行结束
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │ 解析 Content-Length│
              │ 或 Transfer-Encoding│
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │ 读取 body 部分   │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │   完整 Request  │
              │   交给 Router   │
              └─────────────────┘
```

### 3.5 从 0 实现代码

#### 3.5.1 HTTP 请求解析器——结合 Socket 的完整代码

###### 3.5.1.1 整体架构：socket 负责收发，parser 负责解析

```
┌──────────────────────────────────────────────────────────────────┐
│                        你的 Server 主循环                          │
│                                                                   │
│  while True:                                                      │
│    conn, addr = server.accept()     ← 等客户端连接 (TCP 层面)      │
│                                                                   │
│    while keep_alive:                                              │
│      data = conn.recv(4096)         ← 从网卡读字节流               │
│      if not data: break              ← 客户端断开了                │
│                                                                   │
│      parser.feed(data)               ← 把字节追加到 buffer          │
│      │                                                           │
│      ▼                                                           │
│    ┌─────────────────────────────────────┐                        │
│    │  HTTPRequestParser (状态机)          │                        │
│    │                                     │                        │
│    │  ┌─────────────┐                    │                        │
│    │  │ REQUEST_LINE│ → "GET /a HTTP/1.1"                        │
│    │  └──────┬──────┘                    │                        │
│    │         ▼                           │                        │
│    │  ┌─────────────┐                    │                        │
│    │  │   HEADERS   │ → "Host: x" 等     │                        │
│    │  └──────┬──────┘                    │                        │
│    │         ▼                           │                        │
│    │  ┌─────────────┐                    │                        │
│    │  │    BODY     │ → body 字节        │                        │
│    │  └──────┬──────┘                    │                        │
│    │         ▼                           │                        │
│    │  ┌─────────────┐                    │                        │
│    │  │    DONE     │ → 完整 HTTPRequest │                        │
│    └──────┬──────┘                       │                        │
│           │                              │                        │
│           ▼                              │                        │
│      req = parser.parse() 返回完整请求    │                        │
│           │                              │                        │
│           ▼                              │                        │
│      response = handle_request(req)      │                        │
│      conn.send(response)                 ← 写回网卡                │
│                                                                   │
│    conn.close()                          ← 四次挥手, 结束连接      │
└──────────────────────────────────────────────────────────────────┘

  socket/网卡层面               HTTP 解析层面              业务逻辑层面
  ──────────────              ──────────────              ────────────
  accept()                    feed(raw bytes)             handle(req)
  recv()                      parse() → request          生成 response
  send()                                
  close()
```

###### 3.5.1.2 解析器状态机详解——四个状态 + 转移条件

```
                            ┌─────────────────────────────────────┐
                            │          初始状态                   │
                            │    state = "REQUEST_LINE"            │
                            └──────────────┬──────────────────────┘
                                           │
                            ┌──────────────▼──────────────────────┐
                            │         REQUEST_LINE                │
                            │                                     │
                            │  buffer.find("\r\n")                │
                            │     ├── 找到 → split(" ") 解析      │
                            │     │         method, path, version  │
                            │     │         state = "HEADERS"      │
                            │     │                                │
                            │     └── 没找到 → return None         │
                            │               (半包, 等更多数据)      │
                            └──────────────┬──────────────────────┘
                                           │
                            ┌──────────────▼──────────────────────┐
                            │          HEADERS                    │
                            │                                     │
                            │  while True:                        │
                            │    buffer.find("\r\n")              │
                            │      ├── idx == 0 (空行)            │
                            │      │     → state = "BODY"         │
                            │      │                                │
                            │      ├── idx > 0 (有 header)        │
                            │      │     → 解析 key: value        │
                            │      │     → 继续下一行              │
                            │      │                                │
                            │      └── idx == -1                  │
                            │            → return None (半包)      │
                            └──────────────┬──────────────────────┘
                                           │
                            ┌──────────────▼──────────────────────┐
                            │            BODY                     │
                            │                                     │
                            │  CL = headers["Content-Length"]     │
                            │                                     │
                            │  len(buffer) >= CL ?                │
                            │     ├── 是 → buffer[:CL] 取 body    │
                            │     │       state = "DONE"           │
                            │     │                                │
                            │     └── 否 → return None             │
                            │             (body 没收完, 半包)      │
                            └──────────────┬──────────────────────┘
                                           │
                            ┌──────────────▼──────────────────────┐
                            │            DONE                     │
                            │                                     │
                            │  返回完整 HTTPRequest               │
                            │  self.__init__() 重置               │
                            │  (回到 REQUEST_LINE, 等下一个请求)   │
                            └─────────────────────────────────────┘
```

每个转移点要么产出新状态（数据够了），要么 `return None`（数据不够）。外层循环收到 `None` 就知道要等下一次 `recv()`。

```
buffer 的变化过程 (以 "GET / HTTP/1.1\r\nHost: x\r\n\r\n" 为例):

  feed("GET / HTTP/1.1\r\nHost")       → buffer = "GET / HTTP/1.1\r\nHost"
  parse():
    REQUEST_LINE: 找 \r\n → 找到!
                  消耗 "GET / HTTP/1.1\r\n" → buffer = "Host"
                  state = HEADERS
    HEADERS: 找 \r\n → 没找到 → return None

  feed(": x\r\n\r\n")                  → buffer = "Host: x\r\n\r\n"
  parse():
    HEADERS: 逐行读:
              "Host: x" 解析完毕
              空行! → state = BODY
    BODY: Content-Length 不存在 → 0 → state = DONE
    DONE: 返回 HTTPRequest(method="GET", path="/", ...)
          重置 → buffer = "", state = REQUEST_LINE
```

###### 3.5.1.3 完整代码：Server + Parser 一起跑

```python
import socket

# ========== HTTP 请求和响应 ==========

class HTTPRequest:
    def __init__(self):
        self.method = ""
        self.path = ""
        self.version = ""
        self.headers = {}
        self.body = b""

class HTTPResponse:
    def __init__(self, status_code, body=b"", headers=None,
                 content_type="text/plain"):
        self.status_code = status_code
        self.body = body if isinstance(body, bytes) else body.encode()  # 统一转 bytes, 调用方传 str 或 bytes 都行
        self.headers = headers or {}
        self.content_type = content_type

    def to_bytes(self):
        status_map = {
            200: b"OK", 404: b"Not Found", 500: b"Internal Server Error",
        }
        reason = status_map.get(self.status_code, b"Unknown")
        lines = [f"HTTP/1.1 {self.status_code} {reason.decode()}\r\n".encode()]
        self.headers.setdefault("Content-Type", self.content_type)
        self.headers["Content-Length"] = str(len(self.body))
        for k, v in self.headers.items():
            lines.append(f"{k}: {v}\r\n".encode())
        lines.append(b"\r\n")
        lines.append(self.body)
        return b"".join(lines)

# ========== HTTP 请求解析器 (状态机) ==========

class HTTPRequestParser:
    def __init__(self):
        self.buffer = b""
        self.state = "REQUEST_LINE"

    def feed(self, data: bytes):
        self.buffer += data

    def parse(self):
        if self.state == "REQUEST_LINE":
            idx = self.buffer.find(b"\r\n")
            if idx == -1:
                return None
            line = self.buffer[:idx]
            self.buffer = self.buffer[idx+2:]
            parts = line.split(b" ")
            if len(parts) < 3:
                return None
            self.request = HTTPRequest()
            self.request.method = parts[0].decode()
            self.request.path = parts[1].decode()
            self.request.version = parts[2].decode()
            self.state = "HEADERS"

        if self.state == "HEADERS":
            while True:
                idx = self.buffer.find(b"\r\n")
                if idx == 0:
                    self.buffer = self.buffer[2:]
                    self.state = "BODY"
                    break
                if idx == -1:
                    return None
                header_line = self.buffer[:idx]
                self.buffer = self.buffer[idx+2:]
                colon = header_line.find(b":")
                if colon != -1:
                    key = header_line[:colon].decode().strip()
                    value = header_line[colon+1:].decode().strip()
                    self.request.headers[key] = value

        if self.state == "BODY":
            content_length = int(self.request.headers.get("Content-Length", 0))
            if len(self.buffer) >= content_length:
                self.request.body = self.buffer[:content_length]
                self.buffer = self.buffer[content_length:]
                self.state = "DONE"
            else:
                return None

        if self.state == "DONE":
            req = self.request
            self.__init__()
            return req

        return None

# ========== Socket Server + Parser 集成 ==========

def handle_request(req):
    """根据请求路径返回不同响应"""
    if req.path == "/":
        return HTTPResponse(200, b"Hello, World!")
    elif req.path == "/about":
        return HTTPResponse(200, b"This is about page")
    else:
        return HTTPResponse(404, b"Not Found")

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(('0.0.0.0', 8080))
server.listen(128)
print("Server listening on :8080")

while True:
    conn, addr = server.accept()
    print(f"Connection from {addr}")

    parser = HTTPRequestParser()
    keep_alive = True

    while keep_alive:
        data = conn.recv(4096)
        if not data:
            break

        parser.feed(data)

        while True:
            req = parser.parse()
            if req is None:
                break

            print(f"  {req.method} {req.path}")
            if req.headers.get("Connection", "").lower() == "close":
                keep_alive = False

            response = handle_request(req)
            conn.send(response.to_bytes())

    conn.close()
    print(f"Connection closed: {addr}")
```

###### 3.5.1.4 画成图

```
                         全连接队列 (内核)
                          ┌──────────┐
                          │ conn1    │
                          │ conn2    │
                          │ conn3    │
                          └──────────┘
                               │
                     ┌─────────▼─────────────────────────────────────────────────────────┐
                     │          外层循环                                                  │
                     │  while True:                                                      │
                     │    conn, addr = server.accept()                                   │
                     │    ↑ 从全连接队列取出一个已握手完成的连接                            │
                     │                                                                   │
                     │    parser = HTTPRequestParser()                                   │
                     │    keep_alive = True                                              │
                     │                                                                   │
                     │    ┌──── 内层循环 1 ───────────────────────────────────────────┐   │
                     │    │  while keep_alive:                                       │    │
                     │    │    data = conn.recv(4096)                                │    │
                     │    │       │                                                  │    │
                     │    │       ├── data 为空 (b"")                                 │    │
                     │    │       │   → 客户端已 FIN break (退出内层1, 关闭连接)        │    │
                     │    │       │                                                   │    │
                     │    │       └── data 有内容                                      │    │
                     │    │           → parser.feed(data),进入内层循环 2                │    │
                     │    │                                                            │    │
                     │    │   ┌── 内层循环 2 ───────────────────────────────────┐      │     │
                     │    │   │  while True:                                   │     │     │
                     │    │   │    req = parser.parse()                        │     │     │
                     │    │   │       │                                        │     │     │
                     │    │   │       ├── 返回 None                            │     │     │
                     │    │   │       │   → buffer 不够一个完整请求 (半包)      │    │     │
                     │    │   │       │   → break 内层2回到内层1,继续 recv 更多 │    │     │
                     │    │   │       │                                       │    │     │
                     │    │   │       └── 返回 HTTPRequest                     │    │     │
                     │    │   │           → 请求完整!                          │    │     │
                     │    │   │           → handle(req)                        │    │     │
                     │    │   │           → conn.send(resp)                    │    │     │
                     │    │   │           → 继续内层2(buffer 可能还有下一个请求) │    │     │
                     │    │   └────────────────────────────────────────────────┘    │     │
                     │    │                                                         │     │
                     │    └─────────────────────────────────────────────────────────┘     │
                     │                                                                    │
                     │    conn.close()  (四次挥手)                                         │
                     │    → 回到外层循环, accept 下一个连接                                   │
                     └────────────────────────────────────────────────────────────────────┘

三种常见的数据到达情况在这个流程里各怎么走:

① 正常: 一次 recv 拿到完整请求
   recv → feed → parse → DONE → handle → parse → None → recv(下一个)

② 半包: 一次 recv 只拿到半个请求
   recv → feed → parse → None → recv(更多) → feed → parse → DONE → ...

③ 粘包: 一次 recv 拿到多个请求
   recv → feed → parse → DONE → handle → parse → DONE → handle → parse → None → recv

看到没有? 三种情况走的是**同一套循环结构**——区别只在于内层2循环体执行了几次。
```

###### 3.5.1.5 数据流：一次 curl 请求，每个字节怎么走

```
curl → "GET /about HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
        │
        ▼  [网络 → 内核 sk_receive_queue]
    conn.recv(4096) 取出全部 (粘包? 半包? 取决于网络)
        │
        ▼
    parser.feed(data)       → buffer = "GET /about..."
        │
        ▼
    parser.parse()
      │
      ├─ state=REQUEST_LINE → 找 \r\n → "GET /about HTTP/1.1"
      │                        → method=GET, path=/about, version=HTTP/1.1
      │                        → state = HEADERS
      │
      ├─ state=HEADERS      → 逐行读: "Host: localhost"
      │                        → "Connection: close"
      │                        → 空行 → state = BODY
      │
      ├─ state=BODY         → Content-Length 不存在 → body 长度 = 0
      │                        → state = DONE
      │
      └─ state=DONE         → 返回完整 HTTPRequest
                               parser 重置 (self.__init__())
        │
        ▼
    req.method = "GET", req.path = "/about", req.headers["Host"] = "localhost"
        │
        ▼
    handle_request(req)     → 匹配 "/about" → HTTPResponse(200, b"This is about page")
        │
        ▼
    conn.send(response.to_bytes())
        │
        ▼  [内核 sk_write_queue → 网卡 → curl]
    curl 收到:
      HTTP/1.1 200 OK\r\n
      Content-Type: text/plain\r\n
      Content-Length: 18\r\n
      \r\n
      This is about page
        │
        ▼
    Connection: close → keep_alive = False → 跳出内层循环
    conn.close() → 四次挥手
```

**关键点：** 这个代码里 `parser` 是**无状态的**——每次 `parse()` 返回 DONE 后自动 `__init__()` 重置，然后同一个 parser 可以继续解析同一个 TCP 连接上后续请求（keep-alive）。Server 主循环只负责 `accept → recv → feed → parse → handle → send → close`，解析的复杂性全部被封装在状态机里。

---



#### 3.5.2 为什么用状态机？

###### 3.5.2.1 一句话：TCP 没有消息边界，你必须“记住”读到哪了

HTTP 解析器最难的地方**不是解析本身**，而是**不知道一次 `recv()` 能收到多少数据**。

```python
# 天真版本——假设一次 recv 收到完整的 HTTP 请求
def naive_parse(data):
    lines = data.split(b"\r\n")
    method, path, version = lines[0].split(b" ")   # 假设一定在第一行
    # ...
```

这个版本在本地测试能跑，但到了真实网络**一定会崩溃**——因为 TCP 是流，不是消息。

##### 3.5.2.2 问题展示：三次 recv 收到同一个请求

```
客户端 send:         "GET /index.html HTTP/1.1\r\nHo"
                      ↑ recv() 只收到这么多!

客户端 send(继续):   "st: localhost\r\nContent-Length: 5\r\n\r\nHel"
                      ↑ recv() 又收了一部分!

客户端 send(继续):   "lo\x00"
                      ↑ recv() 收完最后的部分
```

三次 `recv()` 返回的数据分别是：

| 次数 | 收到内容 | 你的程序看到的是 |
|------|----------|-----------------|
| 第 1 次 | `"GET /index.html HTTP/1.1\r\nHo"` | 请求行只收到一半！后面多了个 `Ho` |
| 第 2 次 | `"st: localhost\r\nContent-Length: 5\r\n\r\nHel"` | 一堆 headers + body 开头 `Hel` |
| 第 3 次 | `"lo\x00"` | body 剩下的部分 |

如果不用状态机，第 1 次 `split("\r\n")` 会得到：

```python
["GET /index.html HTTP/1.1", "Ho"]   # ← "Ho" 是什么鬼?
```

你根本不知道 `"Ho"` 是 header 的一部分还是什么脏数据。**因为你没有"记住"当前解析到哪了。**

##### 3.5.2.3 状态机解决的就是这个问题

状态机本质就一个词：**当前位置 (parse position)**。

```
                          ┌─────────────┐
                          │ REQUEST_LINE│  ← 正解析"GET /path HTTP/1.1"
                          └──────┬──────┘
                                 │ 找到 \r\n → 解析成功
                                 ▼
                          ┌─────────────┐
                          │   HEADERS   │  ← 正解析"Host: xxx" 这类行
                          └──────┬──────┘
                                 │ 找到空行 \r\n → headers 结束
                                 ▼
                          ┌─────────────┐
                          │    BODY     │  ← 正读 body, 根据 Content-Length 确定长度
                          └──────┬──────┘
                                 │ buffer 里凑够了 Content-Length 字节
                                 ▼
                          ┌─────────────┐
                          │    DONE     │  ← 完整请求解析完毕, 可以交给 handler
                          └─────────────┘
```

**状态机的核心逻辑：**

```
每次 recv 回来:
    把数据追加到 buffer                 ← feed(data)
    while 当前状态可以继续:
        从 buffer 头部读取当前状态需要的数据
        如果 buffer 不够 → break, 等下一次 recv
        如果够了 → 消耗 buffer, 状态转移到下一步
        返回一个完整请求 (如果状态达到 DONE)
```

##### 3.5.2.4 用一个具体例子跑一遍流程

假设网络分 4 次送来一个 POST 请求，看看状态机怎么处理：

```
数据一共 73 字节:
  POST /api HTTP/1.1\r\n
  Host: a.com\r\n
  Content-Length: 5\r\n
  \r\n
  Hello
```

| 步骤 | recv 收到 | buffer | 状态 | 发生什么 |
|------|-----------|--------|------|---------|
| 1 | `POST /api HT` | `POST /api HT` | REQUEST_LINE | 扫描 `\r\n` → 没找到 → 返回 None (等下一次) |
| 2 | `TP/1.1\r\nHost: a.c` | `POST /api HTTP/1.1\r\nHost: a.c` | REQUEST_LINE | 扫描 `\r\n` → 找到了! 解析请求行: method=POST, path=/api, version=HTTP/1.1 → **状态切到 HEADERS**, buffer 剩下 `Host: a.c` |
| 3 | `om\r\nContent-Length: 5\r\n\r\nHel` | `Host: a.c` + `om\r\nContent-Length: 5\r\n\r\nHel` = `Host: a.com\r\nContent-Length: 5\r\n\r\nHel` | HEADERS | while 循环逐行解析: `Host: a.com` 解析完 → `Content-Length: 5` 解析完 → 空行 `\r\n` 遇到 idx==0! → **状态切到 BODY**, buffer 剩下 `Hel` |
| 4 | `lo` | `Hel` + `lo` = `Hello` | BODY | Content-Length 说 body 是 5 字节，buffer 够 5 了 → 取出 `Hello` → **状态到 DONE** |

看到关键了吗？**每次 recv 的数据都是残缺的**，但状态机不关心——它每次都从 buffer 的头部开始，检查当前状态需要的数据够不够。不够就等下一次 recv，够了就前进到下一个状态。

##### 3.5.2.5 状态机 vs 非状态机的对比

```
非状态机:
  recv 完整数据 → 一次性 parse → 返回结果
  如果数据不完整 → 崩溃或乱解析

状态机:
  recv 一点 → parse 一点 → 记住位置 → 等更多数据
  无论数据完整与否 → 正常工作
```

##### 3.5.2.6 生活类比：拼拼图 vs 读一整段话

**非状态机 = 读一整段话**：你必须拿到完整的文章才能读。如果只给你半篇文章，你读不下去。

```
"今天天气真" → 读不了, 不知道后面是什么
```

**状态机 = 拼拼图**：你一次拿几块，拼到对应的位置，记住进度。下次再拿几块，继续拼。

```
第 1 次: "今天天气真"  → 拼到 "今天天气真"
第 2 次: "好,我们去公" → 拼到 "今天天气真好,我们去公"
第 3 次: "园散步"     → 完成 "今天天气真好,我们去公园散步"
```

每次来的碎片数量不确定，但你知道排到哪了——这就是状态。

#### 3.5.3 keep-alive 如何工作？

###### 3.5.3.1 白话：省掉“握手—挥手”的反复折腾

**没有 keep-alive 时，每个 HTTP 请求都要新建一个 TCP 连接：**

```
  ──三次握手──→ 请求 → 响应 → 四次挥手
                    ──三次握手──→ 请求 → 响应 → 四次挥手
                                      ──三次握手──→ 请求 → 响应 → 四次挥手
```

每次请求都新建连接 = 3 个报文握手 + 4 个报文挥手 = **7 个额外报文**。网页上有 100 个资源（CSS/JS/图片）就要重复 100 次——浪费带宽，延迟巨大。

**有 keep-alive 时，同一个 TCP 连接上反复发送请求/响应：**

```
  ──三次握手──→ 请求 → 响应 → 请求 → 响应 → 请求 → 响应 → 四次挥手
```

一次握手，反复使用。

**HTTP/1.1 默认就是 keep-alive**。只有当请求头里带了 `Connection: close` 时，服务器才在响应后关闭连接。

##### 3.5.3.2 核心难题：连续请求在同一个 TCP 流里怎么分隔？

TCP 是字节流，没有消息边界。没有 keep-alive 时，你发完一个请求，连接就断了，所以你知道"到 EOF 就是一个请求结束"。

但 keep-alive 下，**一个 TCP 连接上会连续到达多个请求**：

```python
# recv 一次可能收到多个请求 (粘包)!
data = conn.recv(4096)
# data 内容:
#   "GET /a HTTP/1.1\r\nHost: x\r\n\r\n
#    GET /b HTTP/1.1\r\nHost: x\r\nContent-Length: 3\r\n\r\nabc"
#                                  ↑ 两个请求粘在一起了!
```

怎么知道第一个请求在哪结束、第二个在哪开始？

**答案：靠 `Content-Length` 或 `Transfer-Encoding: chunked` 计算 body 长度。**

但注意：**这段代码**里第 2-4 步是在 parser 内部完成的。对于 GET 请求，parser 的 BODY 阶段检查 `Content-Length` 时发现这个头不存在，`get("Content-Length", 0)` 返回默认值 0，于是**瞬间通过 BODY 阶段到达 DONE**。不是"不需要 Content-Length"，而是 GET 请求的 body 长度隐式为 0，Content-Length 的默认值恰好覆盖了这个场景。

如果把两个 GET 换成 POST + body，没有 Content-Length 头 parser 就不知道 body 在哪结束，会把 body 数据连同下一个请求的请求行一起吞掉。

```python
# HTTP 请求边界规则:
#
# 请求行:    遇到 \r\n 结束
# Headers:   遇到 \r\n\r\n 结束
# Body:      根据 Content-Length 或 chunked 规则确定长度
#            (没有 body 的请求如 GET, body 长度为 0)
#
# 一个完整请求 = 请求行 + headers + 空行 + body
# 下一个请求紧随其后开始
```

所以 keep-alive 的完整流程是：

```
同一个 TCP 连接 (同一个 conn fd) 上:

  conn.recv() → "GET /a...\r\n\r\nGET /b...\r\n\r\nabc"
                     ↑___________↑↑__________________
                    第 1 个请求  第 2 个请求紧随其后

  状态机解析:
    第 1 次 parse():
      消耗 buffer → 取出 "GET /a..."  → 返回一个完整请求
      buffer 剩下 "GET /b...\r\n\r\nabc"

    第 2 次 parse():
      消耗 buffer → 取出 "GET /b..."  → 返回又一个完整请求
      buffer 剩下 ""  (或 0)

    第 3 次 parse():
      buffer 空了 → 返回 None (等下一次 recv)
```

##### 3.5.3.3 看代码，一步一步拆解

```python
class PersistentServer:
    def handle_connection(self, conn):
        parser = HTTPRequestParser()          # ① 每个连接对应一个 parser
        keep_alive = True
        while keep_alive:                     # ② 外层循环: 不断读
            data = conn.recv(4096)            # ③ 从 TCP 收数据
            if not data:                      # ④ 对端关闭 → 退出
                break
            parser.feed(data)                 # ⑤ 追加到 buffer

            while True:                       # ⑥ 内层循环: 反复从 buffer 提取请求
                req = parser.parse()          # ⑦ 状态机解析, 可能返回 None
                if req is None:               # ⑧ buffer 不够一个完整请求 → 跳出内层, 等更多数据
                    break
                # ⑨ 取出一个完整请求, 处理它
                keep_alive = req.headers.get("Connection", "").lower() != "close"
                self.process_request(conn, req)
                if not keep_alive:            # ⑩ 客户端说 Connection: close → 本次处理完就关闭
                    break

        conn.close()                          # ⑪ 关闭 TCP 连接
```

**逐句说明：**

| 行号 | 作用 | 白话 |
|------|------|------|
| ① | 每个连接一个 parser | 因为每个连接的请求流是独立的, 不能混用 |
| ② | `keep_alive` 标志控制循环 | 只要客户端不说 close, 就一直复用这个 TCP 连接 |
| ③ | `recv(4096)` | 从内核接收缓冲区取最多 4096 字节。可能拿到 0~4096 任意长度的数据 |
| ④ | `data` 为空 | 对端 FIN 了 (四次挥手), 读到 EOF, 退出 |
| ⑤ | `feed(data)` | 追加到 parser 内部的 buffer 中 |
| ⑥ | `while True` | 一个 TCP 报文里可能包含多个完整请求, 需要循环提取 |
| ⑦ | `parse()` | 状态机从 buffer 头部尝试提取一个请求。够 → 返回请求；不够 → 返回 None |
| ⑧ | `req is None` | buffer 不够一个完整请求 → 跳出内层, 等下一次 recv 更多数据 |
| ⑨ | 处理请求 | 业务逻辑, 比如路由匹配 + 生成响应 |
| ⑩ | 检查 Connection | 如果客户端说 `Connection: close`, 处理完当前请求就关闭 |
| ⑪ | `conn.close()` | 关闭 TCP 连接, 触发四次挥手 |

##### 3.5.3.4 用具体报文跑一遍

假设客户端通过同一个 TCP 连接发来 3 个请求：

```
请求 1: GET /index.html HTTP/1.1\r\nHost: x\r\n\r\n
请求 2: POST /api HTTP/1.1\r\nHost: x\r\nContent-Length: 4\r\n\r\ndata
请求 3: GET /bye HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n
```

网络可能这样送达：

```
第 1 次 recv: "GET /index.html HTTP/1.1\r\nHost: x\r\n\r\nPOST /api HT"  ← 请求 1 完整 + 请求 2 的一部分
第 2 次 recv: "TP/1.1\r\nHost: x\r\nContent-Length: 4\r\n\r\ndataGET /bye HTTP/1.1\r\nHost: x\r\n"
第 3 次 recv: "Connection: close\r\n\r\n"
```

处理过程：

```
recv 第 1 次:
  feed("GET /index.html HTTP/1.1\r\nHost: x\r\n\r\nPOST /api HT")
  parse() → 返回请求 1 (消耗掉"GET /index.html..."
  parse() → None (buffer 里"POST /api HT"不够一个请求)
  buffer 剩下: "POST /api HT"

recv 第 2 次:
  feed("TP/1.1\r\nHost: x\r\nContent-Length: 4\r\n\r\ndataGET /bye HTTP/1.1\r\nHost: x\r\n")
  parse() → 返回请求 2 (消耗掉"POST /api...data")
  buffer 剩下: "GET /bye HTTP/1.1\r\nHost: x\r\n"
  parse() → None (还差 Connection: close 那行 + 空行)

recv 第 3 次:
  feed("Connection: close\r\n\r\n")
  parse() → 返回请求 3 (消耗全部)
  keep_alive = False (请求 3 有 Connection: close)
  break → 关闭连接
```

**关键点：状态机 + keep-alive 的组合。** 状态机保证了无论 TCP 怎么切分数据，你都能正确提取出一个个完整的 HTTP 请求。而 keep-alive 保证了你不会在提取完一个请求后关闭连接，而是继续用同一个 fd 提取下一个。

##### 3.5.3.5 一个类比：流水线上的包裹

```
你站在流水线旁边:
  - 传送带 (TCP 流) 不断送来包裹零件
  - 你从传送带拿零件 (recv)
  - 拼出一个完整包裹 (parse)
  - 交给打包处 (process_request)
  - 然后继续拼下一个
  - 直到传送带停了 (连接关闭) 或标签上写"最后一个" (Connection: close)
```

没有 keep-alive = 拼完一个包裹就下班, 明天重新开一条新传送带。
有 keep-alive = 拼完一个继续拼下一个, 同一台机器一直干活。

#### 3.5.4 chunked encoding 解析

###### 3.5.4.1 为什么需要 chunked？因为有时你提前不知道 body 有多大

`Content-Length` 要求服务器在发响应前就精确知道 body 总长度。但有些场景根本算不出来：

- **动态生成的页面**：一个大型报表要查数据库、逐行生成，总大小只有在生成完成后才知道
- **实时推送数据**：服务器推送事件 (SSE)，数据会源源不断产生，总长度是无限的
- **文件流式传输**：边读边发，不知道文件有多大

这时就需要 **chunked transfer encoding**——把 body 切成一块一块的，每块前面标长度，最后一块长度为 0 表示结束。

##### 3.5.4.2 格式：像一串香肠

```
HTTP/1.1 200 OK\r\n
Content-Type: text/plain\r\n
Transfer-Encoding: chunked\r\n      ← 告诉客户端: 我要用 chunked 发 body
\r\n
------------------------------------ body 开始 ------------------------------------
5\r\n                                                                  ← 第 1 块: 长度 (十六进制)
Hello\r\n                                                            ← 第 1 块: 数据 (5 字节)
6\r\n                                                                  ← 第 2 块: 长度 (十六进制)
 World\r\n                                                           ← 第 2 块: 数据 (6 字节)
0\r\n                                                                  ← 最后一块: 长度 = 0, 表示结束
\r\n                                                                  ← trailer (可选, 通常为空行)
------------------------------------ body 结束 ------------------------------------
```

**核心规则：**

```
chunked-body = 1*( chunk-size CRLF chunk-data CRLF )
               0 CRLF                 ← 终止块: 长度 = 0
               [trailer CRLF]         ← 可选尾部头
               CRLF                   ← 最后的空行
```

每个 chunk 的格式：

```
┌─────────────────────────────────┐
│  chunk-size (十六进制 ASCII)    │  例: "5"
│  CRLF (\r\n)                   │
│  chunk-data (正好 chunk-size    │  例: "Hello"
│    个字节)                      │
│  CRLF (\r\n)                   │
└─────────────────────────────────┘
```

##### 3.5.4.3 解析 chunked 的核心就是“读长度 → 读数据 → 读长度 → 读数据 → ...”

```python
# 原始数据 (假设已经从 TCP recv 完整了):
data = b"5\r\nHello\r\n6\r\n World\r\n0\r\n\r\n"

# 你会遇到两种 TCP 情况:

# 情况 A: 整个 chunked body 一次 recv 到齐 (上面这样)
# 情况 B: 分多次 recv 收到, 比如:
#   第 1 次: "5\r\nHel"
#   第 2 次: "lo\r\n6\r\n Wo"
#   第 3 次: "rld\r\n0\r\n\r\n"
# chunked 解析必须有状态机, 不能一次读完!
```

所以你需要**在 HTTP 解析器的状态机里增加 CHUNK 状态**：

```
原先的状态机:
  REQUEST_LINE → HEADERS → BODY → DONE

如果 headers 中有 "Transfer-Encoding: chunked":
  REQUEST_LINE → HEADERS → CHUNK_SIZE → CHUNK_DATA → ... → DONE

展开:
                    ┌─────────────────────┐
                    │     CHUNK_SIZE      │  ← 读 "\r\n" 之前的十六进制数字
                    └─────────┬───────────┘
                              │ 读到 \r\n, 解析出 size
                              │ size > 0             size = 0
                    ┌─────────▼───────────┐        ┌────▼────┐
                    │     CHUNK_DATA      │        │ TRAILER  │
                    │  读 size 字节 + \r\n│        └────┬─────┘
                    └─────────┬───────────┘            │ 读到 \r\n
                              │                        ▼
                              │                  ┌──────────┐
                              └─────────────────►│   DONE   │
                                                  └──────────┘
```

###### 3.5.4.4 看代码，一行一行拆

```python
def parse_chunked_body(data):
    """解析 chunked transfer encoding"""
    body = b""
    pos = 0
    while True:
        # 读取 chunk size (十六进制 + \r\n)
        end = data.find(b"\r\n", pos)    # ① 找当前 chunk 的尾部
        if end == -1:
            break                         # ② 还没收到完整的 chunk-size 行
        chunk_size = int(data[pos:end], 16)  # ③ 十六进制 → 整数
        if chunk_size == 0:
            break                         # ④ size=0, 结束
        pos = end + 2                     # ⑤ 跳过 \r\n, 到达数据开始
        body += data[pos:pos + chunk_size] # ⑥ 取出 chunk 数据
        pos += chunk_size + 2             # ⑦ 跳过数据 + \r\n, 到达下一个 chunk-size
    return body
```

**逐句说明：**

| 行 | 作用 | 白话 |
|----|------|------|
| ① | `find(b"\r\n", pos)` | 从当前位置找下一个 `\r\n`，这里应该是 chunk-size 行结束的地方 |
| ② | `end == -1` | 没找到 `\r\n` → 数据不够 → 等下一次 recv |
| ③ | `int(..., 16)` | `"5"` → `5`, `"1f"` → `31`。十六进制是因为 chunk-size 可能很大（比如 `"100000"` = 1MB） |
| ④ | `chunk_size == 0` | 终止块，body 结束 |
| ⑤ | `pos = end + 2` | 跳过 `\r\n`，pos 指向 chunk-data 的第一个字节 |
| ⑥ | `data[pos:pos + chunk_size]` | 取出 chunk 数据。注意这里是 **切片索引运算**, 不是真的从哪拆出数据 |
| ⑦ | `pos += chunk_size + 2` | 跳过 chunk-data 和后面的 `\r\n`，指向下一个 chunk-size 或尾部 |

##### 3.5.4.5 用“5\r\nHello\r\n0\r\n\r\n”一步一步走

```
初始: data = "5\r\nHello\r\n0\r\n\r\n", pos=0, body=""

第 1 轮:
  end = data.find("\r\n", 0) = 1           ← "5\r\n" 中 \r\n 的位置
  chunk_size = int("5", 16) = 5
  pos = 1 + 2 = 3                          ← 跳过 "\r\n"
  body += data[3:3+5] = "Hello"            ← 取出 5 字节
  pos = 3 + 5 + 2 = 10                     ← 跳过 "Hello\r\n"
  此时 body="Hello"

第 2 轮:
  end = data.find("\r\n", 10) = 11         ← "0\r\n" 中 \r\n 的位置
  chunk_size = int("0", 16) = 0
  break                                     ← 结束
  body="Hello" (最终结果)
```

##### 3.5.4.6 跟 HTTP 解析器状态机结合

一个完整的 HTTP 请求解析器（带 chunked 支持）在 BODY 阶段需要判断：

```python
class HTTPRequestParser:
    def parse(self):
        # ... 前面 REQUEST_LINE, HEADERS 同之前的代码 ...
        
        if self.state == "BODY":
            # 有两种读 body 的方式
            if self.is_chunked:                     # ← Transfer-Encoding: chunked
                return self._parse_chunked_body()
            else:                                    # ← 普通 Content-Length
                content_length = int(self.request.headers.get("Content-Length", 0))
                if len(self.buffer) >= content_length:
                    self.request.body = self.buffer[:content_length]
                    self.buffer = self.buffer[content_length:]
                    self.state = "DONE"
                else:
                    return None
        
        if self.state == "DONE":
            req = self.request
            self.__init__()
            return req
        return None

    def _parse_chunked_body(self):
        """chunked 状态机: CHUNK_SIZE ↔ CHUNK_DATA"""
        while True:
            if self.chunk_state == "CHUNK_SIZE":
                idx = self.buffer.find(b"\r\n")
                if idx == -1:
                    return None
                size = int(self.buffer[:idx], 16)
                self.buffer = self.buffer[idx+2:]
                if size == 0:
                    # 最后一块, 跳过 trailer 和最后的 \r\n
                    if self.buffer[:2] == b"\r\n":
                        self.buffer = self.buffer[2:]
                    self.state = "DONE"
                    break
                self.chunk_size = size
                self.chunk_state = "CHUNK_DATA"
            
            if self.chunk_state == "CHUNK_DATA":
                if len(self.buffer) < self.chunk_size + 2:  # 数据 + \r\n
                    return None
                self.request.body += self.buffer[:self.chunk_size]
                self.buffer = self.buffer[self.chunk_size + 2:]  # 跳过 \r\n
                self.chunk_state = "CHUNK_SIZE"
```

**关键理解：** chunked 本质上是一个**嵌套的状态机**。外层的 HTTP 解析器在 `BODY` 状态时，需要进入内层的 chunked 子状态机（`CHUNK_SIZE ↔ CHUNK_DATA`），直到遇到 size=0 才回到外层的 `DONE` 状态。

##### 3.5.4.7 一个完整的 chunked 响应在 TCP 上怎么流过来

```
服务器生成一个大报表, 边生成边发:

TCP 流:
  "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n"
  + "1E\r\n"                              ← chunk-size = 30 (十六进制 1E)
  + "2024-01-01  Login   user_a\n"        ← 30 字节报表第 1 行
  + "\r\n"
  + "1E\r\n"                              ← 下一块
  + "2024-01-02  Login   user_b\n"        ← 30 字节报表第 2 行
  + "\r\n"
  + ...                                  ← 继续生成...
  + "0\r\n\r\n"                            ← 结束

客户端浏览器收到后:
  逐块拼接 → body = "2024-01-01  Login   user_a\n2024-01-02  Login   user_b\n..."
  因为 Transfer-Encoding: chunked, 浏览器知道要按 chunked 规则解析 body
  size=0 时, 浏览器知道响应结束了
```

##### 3.5.4.8 一句话总结

**Content-Length = 发之前就知道总大小（信封上写明了重量）**

**Chunked = 不知道总大小，切成一节一节的香肠，每节标长度，最后一节标 0（一节一节地递给你，你不知道一共多少节，直到收到标 0 的那节）**

---

## 4. 路由系统——请求分发

### 4.1 一句话本质

**路由系统 = URL 模式到 handler 函数的映射表，加上参数提取和中间件链。**

### 4.2 生活类比

路由 = **公司前台的分发逻辑**：

```
客人说 "找销售部"          → 路由匹配 "/sales"
客人说 "找技术部工号 42"   → 路由匹配 "/tech/:id"  提取 id=42
所有客人必须先登记         → 中间件 (日志/鉴权)
```

### 4.3 技术原理

**核心数据结构：路由表**

```python
routes = {
    "GET": {
        "/":          home_handler,       # ← handler 函数
        "/users":     list_users,         # ← handler 函数
        "/users/:id": get_user,           # ← handler 函数 (动态参数)
    },
    "POST": {
        "/users":     create_user,        # ← handler 函数
    },
}
```

**上面 dict 里的每个值都是 handler 函数**——签名统一为 `handler(request: HTTPRequest) -> HTTPResponse`，接收解析好的 HTTP 请求对象，返回 HTTP 响应。

**handler 函数签名：**

```python
def handler(request: HTTPRequest) -> HTTPResponse:
    ...
    return HTTPResponse(200, body, headers)
```

### 4.4 从 0 实现代码

#### 4.4.1 Step 3：路由系统

```python
class Router:
    def __init__(self):
        #    key=方法          value=list[ (路径段数组, handler, 是否动态) ]
        self.routes = {}      # 例: {"GET": [(["users", ":id"], get_user, True)]}

    def add_route(self, method, path, handler):
        """注册一条路由: 把路径按 / 切段后存入 routes"""
        if method not in self.routes:
            self.routes[method] = []
        segments = path.strip("/").split("/")   # "/users/:id" → ["users", ":id"]
        is_dynamic = ":" in path
        self.routes[method].append((segments, handler, is_dynamic))

    def get(self, path):
        """@router.get("/path") 装饰器的底层实现"""
        return lambda h: self.add_route("GET", path, h)

    def post(self, path):
        """@router.post("/path") 装饰器的底层实现"""
        return lambda h: self.add_route("POST", path, h)

    def match(self, method, path):
        """
        匹配路由: 按方法找到列表, 逐条比对路径段
        返回: (handler, params)
              handler=None → 未匹配 (404)
              handler=函数  → params 为 dict, 包含 :param 提取的参数
        """
        if method not in self.routes:
            return None, {}                      # 该方法没注册任何路由
        path_segments = path.strip("/").split("/")
        for segments, handler, is_dynamic in self.routes[method]:
            if len(segments) != len(path_segments):
                continue                         # 段数不同 → 跳过
            params = {}
            match = True
            for pattern_seg, actual_seg in zip(segments, path_segments):
                if pattern_seg.startswith(":"):      # 动态参数, 如 ":id"
                    params[pattern_seg[1:]] = actual_seg
                elif pattern_seg != actual_seg:      # 静态段不匹配 → 本条失败
                    match = False
                    break
            if match:
                return handler, params           # 返回 handler + 提取的参数
        return None, {}                          # 没匹配上 → 404

router = Router()

@router.get("/")
def home(req):
    return HTTPResponse(200, b"Home Page")

@router.get("/users/:id")
def get_user(req):
    user_id = req.params["id"]
    return HTTPResponse(200, f"User {user_id}".encode())
```

###### 4.4.1.1 核心思路：把路径切成段，逐段比对

路由匹配的核心逻辑在 `match()` 方法里。假设注册了 `/users/:id`：

**注册时：**
```
add_route("GET", "/users/:id", get_user)
  → routes["GET"] 追加一项: (["users", ":id"], get_user, True)
    ↑                  ↑           ↑           ↑
    HTTP 方法        路径段数组     handler     是否有动态参数
```

**请求到达时 `match("GET", "/users/42")`：**

```
请求路径:           "/users/42"
切段:              ["users", "42"]

遍历 routes["GET"] 的每一项:

第 1 项: (["users", ":id"], get_user, True)
         ↑   ↑
         段数一样 (2 == 2) → 继续

        逐段比对:
          pattern[0] = "users"  vs  actual[0] = "users"   → 相同 ✓
          pattern[1] = ":id"    vs  actual[1] = "42"      → :param! 保存 params["id"]="42"

        全部通过 → 返回 (get_user, {"id": "42"})
```

**如果没匹配上：**
```
match("GET", "/users/42/profile")
切段: ["users", "42", "profile"]

第 1 项: (["users", ":id"], get_user, True)
         ↑                 段数 2 ≠ 3 → continue, 跳过

没有任何项匹配 → 返回 (None, None, {})
  → 你的代码返回 404
```

###### 4.4.1.2 两个装饰器 `@router.get()` 和 `@router.post()`

```python
def get(self, path):
    return lambda h: self.add_route("GET", path, h)
```

这个写法叫**装饰器工厂**。`@router.get("/")` 等价于：

```python
home = router.get("/")(home)
# router.get("/") 返回一个 lambda: lambda h: add_route("GET", "/", h)
# 然后 lambda(home) 调用 add_route("GET", "/", home)
```

最终效果：把 `home` 函数注册到 `routes["GET"]` 里，路径为 `/`。

###### 4.4.1.3 `match()` 返回后，谁把 params 塞到 req 里？

`match()` 始终返回三个值 `(handler, params, _)`（第三个保留字段暂未使用），其中 handler 函数签名是 `def handler(req)`，params 需要合并到 request 对象中。这一步在**服务器的主循环**里完成：

```python
# 服务器主循环中的路由处理:
req = parser.parse()                     # 从 TCP 解析出 HTTP 请求
handler, params = router.match(req.method, req.path)
if handler:
    req.params = params                   # ← 把路由提取的参数塞进 req
    response = handler(req)
else:
    response = HTTPResponse(404, b"Not Found")
conn.send(response.to_bytes())
```

所以 `@router.get("/users/:id")` 装饰的 `get_user(req)` 里能直接读到 `req.params["id"]`——是服务器主循环在调用 handler 之前塞进去的。

###### 4.4.1.4 这个路由器的局限

| 特点 | 说明 |
|------|------|
| 匹配复杂度 O(n) | 遍历所有路由项，n 大时慢。生产环境用前缀树（radix tree） |
| 不支持通配符 | `/*` 或 `/**` 需要额外实现 |
| 不支持正则 | `/posts/\d+` 需要额外判断 |
| 不支持中间件 | 中间件需要在 match 之前/之后嵌入主循环 |
| **但足够简单** | 对于理解路由原理，30 行代码展示了最核心的思想 |

#### 4.4.2 为什么这样设计？

- **方法 + 路径双重索引**：RESTful API 需要同一路径的不同方法映射到不同 handler
- **动态参数提取**：`/users/:id` 模式最常用，必须支持
- **路由表前置注册**：启动时构建好，运行时 O(n) 匹配（小型 server 够用，Nginx 用基数树实现 O(1)）

---

## 5. 响应生成与返回

### 5.1 一句话本质

**HTTP 响应 = 状态行 + headers + body 三段拼接，写成字节流到 socket。**

### 5.2 技术原理

```python
class HTTPResponse:
    def __init__(self, status_code, body=b"", headers=None,
                 content_type="text/plain"):
        self.status_code = status_code
        self.body = body if isinstance(body, bytes) else body.encode()  # 统一转 bytes, 调用方传 str 或 bytes 都行
        self.headers = headers or {}
        self.content_type = content_type

    def to_bytes(self):
        """将响应编码为字节流"""
        status_map = {
            200: b"OK",
            201: b"Created",
            301: b"Moved Permanently",
            400: b"Bad Request",
            404: b"Not Found",
            500: b"Internal Server Error",
        }
        reason = status_map.get(self.status_code, b"Unknown")

        # 状态行
        lines = [f"HTTP/1.1 {self.status_code} {reason.decode()}\r\n".encode()]

        # 自动设置 Content-Length
        self.headers.setdefault("Content-Type", self.content_type)
        self.headers["Content-Length"] = str(len(self.body))

        # headers
        for key, value in self.headers.items():
            lines.append(f"{key}: {value}\r\n".encode())

        # 空行分隔
        lines.append(b"\r\n")

        # body
        lines.append(self.body)

        return b"".join(lines)
```

**完整的 socket 处理循环：**

```python
def handle_client(conn):
    parser = HTTPRequestParser()
    buffer = b""
    while True:
        data = conn.recv(65536)
        if not data:
            break
        buffer += data
        parser.feed(data)
        req = parser.parse()
        if req is None:
            continue

        # 路由匹配
        handler, params = router.match(req.method, req.path)
        if handler:
            req.params = params
            response = handler(req)
        else:
            response = HTTPResponse(404, b"Not Found")

        conn.sendall(response.to_bytes())

        # 是否 keep-alive
        if req.headers.get("Connection", "").lower() == "close":
            break

    conn.close()
```

---

## 6. 并发模型——从 BIO 到 Reactor

### 6.1 一句话本质

**并发模型回答的是同一个问题：当 IO 未就绪时，CPU 是空转等待 (BIO) 还是去干别的 (NIO/事件驱动)。**

这个问题有个著名的称呼——**C10K 问题**（C = 1000, 10K = 10000）：一台服务器能不能同时处理 10,000 个客户端连接？用 BIO 每连接一个线程，1 万连接就需要 80GB+ 内存（线程栈）。所以 C10K 的本质**不是网络，是内存**——解决方案就是 NIO / epoll / Reactor 这些不依赖"每连接一线程"的事件驱动模型。

### 6.2 四种模型的本质对比

| 模型 | 本质 | 类比 | 代表 |
|------|------|------|------|
| BIO (Blocking I/O) | 每个连接一个线程，线程阻塞在 IO 上 | 每个客人配一个服务员，上菜前站着等 | Apache |
| NIO + Selector (Non-Blocking I/O) | 一个线程轮询所有连接 | 一个服务员同时看所有桌，哪桌有需求就过去 | Tomcat NIO |
| Reactor | 事件驱动 + 回调/协程 | 客人按铃，服务员来服务 | Netty, Nginx |
| 单线程事件循环 | 一个线程做所有事，非阻塞 | 一个超级服务员跑着服务所有桌 | Node.js |

### 6.3 BIO（阻塞 IO）

#### 6.3.1 单线程实现

```python
# 2.5.1 的串行版本——一次只能处理一个连接
while True:
    conn, addr = server.accept()   # 阻塞等待
    handle_client(conn)            # 处理完了才能 accept 下一个
    conn.close()
```

**问题：** 一个连接没处理完，后续所有连接都得等——全连接队列堆满就丢包。

#### 6.3.2 线程池方式

```python
from concurrent.futures import ThreadPoolExecutor

pool = ThreadPoolExecutor(max_workers=200)

while True:
    conn, addr = server.accept()
    # 从线程池取一个线程处理，避免无限创建
    pool.submit(handle_client, conn)
```

**问题：**
- 200 个线程处理 10000 个连接时，大部分线程在 `recv()` 上阻塞
- 线程切换浪费 CPU

#### 6.3.3 Python 线程池接口知识

Python 提供两种线程池接口：

**① `concurrent.futures.ThreadPoolExecutor`（推荐）**

```python
from concurrent.futures import ThreadPoolExecutor

pool = ThreadPoolExecutor(max_workers=200)

# 提交任务, 返回 Future 对象
future = pool.submit(handle_client, conn)

# 可以等结果 (会阻塞当前线程)
result = future.result(timeout=5)

# 批量提交
futures = [pool.submit(fn, args) for args in task_list]

# 批量等结果
from concurrent.futures import as_completed
for future in as_completed(futures):
    print(future.result())
```

**② `threading.Thread`（低级接口）**

```python
import threading

# 直接创建线程 (不推荐大量使用——每个线程 ~8MB 栈)
t = threading.Thread(target=handle_client, args=(conn,))
t.start()

# 可以设成守护线程, 主线程退出时自动结束
t = threading.Thread(target=fn, daemon=True)
t.start()
```

**关键参数对比：**

| 参数 | `ThreadPoolExecutor` | `threading.Thread` |
|------|---------------------|-------------------|
| 创建方式 | 池化管理, 复用线程 | 每次新建 |
| 最大线程数 | `max_workers` 控制 | 不设限 (会耗尽内存) |
| 返回结果 | `Future.result()` 获取 | 需自行用 `Queue` 传回 |
| 异常处理 | `Future` 自动捕获, result() 时抛出 | 需自行 try/except |
| 适用场景 | **Web 服务器、通用任务池** | 少量长时任务 |

**线程池在 Web 服务器中的局限（为什么还需要 NIO）：**

```python
pool = ThreadPoolExecutor(max_workers=200)

# 看似能处理很多连接, 但:
# 连接 1: 占用线程 1, recv() 阻塞等数据
# 连接 2: 占用线程 2, recv() 阻塞等数据
# ...
# 连接 200: 占用线程 200, recv() 阻塞等数据
# 连接 201: 等待! 没有空闲线程了

# 大部分线程其实什么都没干, 只是在等 IO
# 这就是"阻塞 + 线程池"的根本矛盾: 线程是 CPU 资源,
# 但被 IO 等待浪费了
```

所以线程池只解决了"线程数爆炸"的问题，没有解决"线程空等 IO"的问题——这就是为什么需要 6.4 的 NIO / Selector。

### 6.4 NIO + Selector（I/O 多路复用）

##### 6.4.1 解决了什么问题

线程池的 200 个线程，大部分时间在 `recv()` 里**阻塞等数据**——线程没干活，但占着内存和 CPU 上下文。

NIO 的思路：**把"等数据"这件事从应用程序交给内核**。应用程序告诉内核"哪些 fd 我有兴趣"，内核在数据到达时通知你。

```
BIO 方式:                          NIO 方式:
线程 1: recv(conn1) ← 阻塞         线程 1: select(conn1, conn2, conn3...)
线程 2: recv(conn2) ← 阻塞                  ↑ 阻塞, 但一次等所有 fd
线程 3: recv(conn3) ← 阻塞                   ↓ 任意一个 fd 有数据 → 返回
...                                        处理有数据的 fd, 然后继续 select
线程 200: recv(conn200) ← 阻塞
               ↑
         200 个线程全睡着,   <--->    1 个线程等所有 fd, 有数据才处理
         切换开销巨大
```

**一个线程处理所有连接，不再每个连接配一个线程。**

##### 6.4.2 NIO 的核心：非阻塞 socket + I/O 多路复用

###### 6.4.2.1 先搞清楚：BIO 到底在等什么

看一个典型的 BIO + 线程池服务器：

```python
# 主线程: 阻塞等连接
while True:
    conn, addr = server.accept()    # ← 阻塞点 1: 等新连接到达
    pool.submit(handle, conn)

# worker 线程: 阻塞等数据
def handle(conn):
    while True:
        data = conn.recv(4096)      # ← 阻塞点 2: 等请求数据到达
        if not data:
            break
        process(data)
```

**BIO 有两层阻塞：**

```
┌─────────────────────────────────────────────┐
│  主线程: accept() ←── 阻塞, 等新连接          │
│                                              │
│  ┌─ worker 1: recv() ←── 阻塞, 等数据        │
│  ├─ worker 2: recv() ←── 阻塞, 等数据        │
│  ├─ worker 3: recv() ←── 阻塞, 等数据        │
│  ...                                         │
│  └─ worker N: recv() ←── 阻塞, 等数据        │
└─────────────────────────────────────────────┘

10000 连接 → 1 个主线程等连接 + 10000 个 worker 线程等数据
          = 10001 个线程全睡着, 占着内存不干活
```

**NIO 把这两层阻塞合并成一层：**

```
┌─────────────────────────────────────────────┐
│  一个线程: select(server_fd, conn1, conn2, ...)   │
│                                                   │
│  这个 select() 同时等两件事:                       │
│    - server_fd 可读 → 有**新连接到达**             │
│    - connX 可读    → 有**请求数据到达**             │
│                                                   │
│  任一事件发生 → select 返回 → 处理对应 fd          │
└─────────────────────────────────────────────┘

10000 连接 → 1 个线程等 (10000 个 fd 的连接事件 + 数据事件)
          = 1 个线程睡着, 省了 10000 个线程的内存
```

**一句话总结：BIO 是 N 个线程每人等一个 fd，NIO 是 1 个线程等 N 个 fd。省的不是"等"这个动作，是"等的线程数"。**

###### 6.4.2.2 实现这个效果需要两个东西



**阻塞 vs 非阻塞 recv() 的差别：**

```
阻塞模式 (BIO):
  conn.recv(4096)
    │
    ├── 有数据 → 立即返回
    │
    └── 无数据 → 进程挂起 (TASK_INTERRUPTIBLE)
                  ↓
            sock 的 sk_receive_queue 为空
            进程加入 sock 的等待队列
            进程状态: S (sleeping)
                  ↓
            数据到达 → 软中断 → sk_receive_queue 有数据
            唤醒等待队列中的进程
            进程状态: R (running)
            recv() 返回数据

非阻塞模式 (NIO):
  conn.setblocking(False)
  conn.recv(4096)
    │
    ├── 有数据 → 立即返回
    │
    └── 无数据 → 立即返回 -1, errno = EAGAIN
                 进程不挂起, 继续执行下一条指令
```

**只有非阻塞还不够——为什么还需要 select：**

```
❌ 只设非阻塞, 不用 select (忙等):
  while True:
      data = conn.recv(4096)          # 不阻塞, 立即返回
      if data:
          process(data)
      # else: EAGAIN, 但 CPU 在空转!
      # 这个循环每秒吃掉 100% CPU

✅ 非阻塞 + select:
  while True:
      sel.select()                     # 进程睡眠, CPU 0%
      # 内核只在数据到达时唤醒进程：其实就是等待上面监听的事件
      data = conn.recv(4096)           # 一定不阻塞, 一定有数据
      process(data)
```

**所以 NIO 的完整工作流是：**

```
┌─────────────────────────────────────────────────────┐
│                    进程                                 │
│                                                        │
│  ┌────────────────────────────────────────────────┐   │
│  │  sel.select()                                   │   │
│  │    │                                            │   │
│  │    ├── 没有任何 fd 有数据                        │   │
│  │    │    → 进程睡眠 (不占 CPU)                    │   │
│  │    │    → 内核在数据到达时唤醒                   │   │
│  │    │                                            │   │
│  │    └── 有 fd 就绪                               │   │
│  │         → 遍历就绪列表                          │   │
│  │         → 对每个就绪 fd:                        │   │
│  │             如果是监听 fd:                       │   │
│  │               conn = accept()  (非阻塞, 必有)     │   │
│  │               conn.setblocking(False)             │   │
│  │               sel.register(conn, EVENT_READ)      │   │
│  │             如果是连接 fd:                       │   │
│  │               data = conn.recv(4096) (非阻塞, 必有)│   │
│  │               process(data)                       │   │
│  └────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
           │                            ↑
           │ 进程睡眠                    │ 数据到达唤醒
           ▼                            │
┌──────────────────────┐                │
│      内核             │────────────────┘
│                      │
│  ┌── epoll 实例 ───┐ │
│  │ 就绪链表        │ │
│  │ [fd=4] [fd=7]   │ │
│  └─────────────────┘ │
│                      │
│  当网卡收到数据包:    │
│    1. DMA → 内核缓冲区 │
│    2. 软中断 → TCP 处理 │
│    3. 数据放到 sock 的  │
│       sk_receive_queue  │
│    4. epoll 把 fd 加入  │
│       就绪链表          │
│    5. 唤醒等待的进程    │
└──────────────────────┘
```

**select/poll/epoll 的本质区别用图表示：**

```
select (1983):
  用户空间:  fd_set readfds = {fd 1, 2, 3, ..., 1024}
              ↓  每次调用都拷贝到内核
  内核:      遍历所有 fd, 检查每个 fd 是否有数据
              ↓  修改 readfds 位图
  用户空间:  遍历 readfds 找到哪些 fd 还在集合里
              * O(n) 遍历 + O(n) 拷贝

poll (1997):
  用户空间:  pollfd[] = array of fd + events
              ↓  每次调用都拷贝到内核
  内核:      遍历所有 pollfd, 检查每个 fd
              ↓  修改 revents
  用户空间:  遍历 pollfd 检查 revents
              * O(n) 遍历 + O(n) 拷贝 (但去掉了 1024 上限)

epoll (2002):
  用户空间:  epoll_ctl(ADD, fd) → 内核红黑树 (只做一次)
             ↓  epoll_wait() → 只读取就绪链表
  内核:      数据到达 → 回调 → fd 加入就绪链表
             ↓  返回就绪事件列表
  用户空间:  直接处理就绪列表中的 fd
              * O(1) 获取就绪事件, 只返回有数据的 fd
```

##### 6.4.3 代码实现

**Python 版（selector 封装了 epoll/kqueue 的差异）：**

```python
import selectors
import socket

sel = selectors.DefaultSelector()  # Linux → epoll, macOS → kqueue

server = socket.socket()
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(('0.0.0.0', 8080))
server.listen(128)
server.setblocking(False)  # NIO 第一步: 非阻塞

# 注册监听 socket: 关注"可读"事件 (即有新连接到达)
sel.register(server, selectors.EVENT_READ, data="accept")

def accept(server):
    conn, addr = server.accept()
    conn.setblocking(False)
    # 注册连接 socket: 关注"可读" (即有 HTTP 请求数据)
    sel.register(conn, selectors.EVENT_READ, data="read")

def read(conn):
    data = conn.recv(4096)
    if data:
        # 解析处理...
        conn.sendall(response.to_bytes())
    else:
        sel.unregister(conn)
        conn.close()

# NIO 第二步: 事件循环
while True:
    events = sel.select(timeout=None)  # 阻塞等待至少一个 fd 就绪
    for key, mask in events:
        if key.data == "accept":
            accept(key.fileobj)
        elif key.data == "read":
            read(key.fileobj)
```

**Java 版（java.nio 包, Selector + Channel + ByteBuffer）：**

```java
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioServer {
    public static void main(String[] args) throws Exception {
        // 打开 Selector (Linux → epoll, macOS → kqueue)
        Selector selector = Selector.open();

        // 打开 ServerSocketChannel, 绑定端口, 设非阻塞
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(8080));
        server.configureBlocking(false);  // NIO 第一步: 非阻塞

        // 注册到 Selector, 关注 OP_ACCEPT
        server.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            // NIO 第二步: select() 阻塞等待事件
            selector.select();                    // 等价于 epoll_wait()
            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();                   // 必须手动移除已处理的 key

                if (key.isAcceptable()) {         // 新连接到达
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                    SocketChannel conn = ssc.accept();  // 非阻塞, 一定有连接
                    conn.configureBlocking(false);
                    // 注册到 Selector, 关注 OP_READ
                    conn.register(selector, SelectionKey.OP_READ);

                } else if (key.isReadable()) {    // 有数据可读
                    SocketChannel conn = (SocketChannel) key.channel();
                    ByteBuffer buf = ByteBuffer.allocate(4096);
                    int n = conn.read(buf);       // 非阻塞, 返回实际读到的字节数

                    if (n == -1) {                 // 对端关闭
                        key.cancel();
                        conn.close();
                    } else {
                        buf.flip();                // 切换为读模式
                        byte[] data = new byte[buf.limit()];
                        buf.get(data);
                        // data 就是 HTTP 请求字节, 交给解析器处理
                        // ...

                        // 响应: 这里简化直接写回
                        String response = "HTTP/1.1 200 OK\r\nContent-Length: 13\r\n\r\nHello, World!";
                        conn.write(ByteBuffer.wrap(response.getBytes()));
                    }
                }
            }
        }
    }
}
```

**Python 和 Java 的对应关系：**

| 概念 | Python | Java |
|------|--------|------|
| 多路复用器 | `selectors.DefaultSelector()` | `Selector.open()` |
| 监听 socket | `socket.socket()` + `setblocking(False)` | `ServerSocketChannel.open()` + `configureBlocking(false)` |
| 连接 socket | `accept()` 返回的 `conn` | `accept()` 返回的 `SocketChannel` |
| 注册事件 | `sel.register(fd, EVENT_READ, data)` | `channel.register(selector, OP_ACCEPT)` |
| 等待事件 | `sel.select()` | `selector.select()` |
| 遍历就绪事件 | `for key, mask in events:` | `iter = selectedKeys.iterator()` |
| 读数据 | `conn.recv(4096)` | `channel.read(ByteBuffer.allocate(4096))` |
| 关闭检测 | `data == b""` | `read() == -1` |

##### 6.4.4 内核视角：select / poll / epoll 的演进

###### 先讲一个故事：三个快递员的进化

```
你是一个前台, 负责收快递 (网络数据)。
每天有 10000 个包裹可能到达, 但不知道哪个先到。

用 select 的方式:
  你拿一张 1024 格的表格 (fd_set 位图)。
  每天早上一上班, 把今天的 10000 个包裹编号填进去,
  但表格只有 1024 格 → 只能填前 1024 个!
  然后你跑去仓库问管理员: "1 号到了吗? 2 号到了吗? 3 号到了吗? ..."
  管理员一个个检查, 把到了的标记出来。
  你拿着标记过的表格回来, 一个个找哪些打了勾。
  → 每天跑两趟仓库, 每次检查 1024 个包裹。

用 poll 的方式:
  表格换成一个长长的清单 (pollfd 数组), 没有 1024 格限制了。
  但还是要每天跑仓库, 一个个问: "1 号到了吗? 2 号到了吗? ... 10000 号到了吗?"
  → 还是每天跑仓库, 还是一个个检查, 只是不限数量了。

用 epoll 的方式:
  你第一天把 10000 个包裹编号告诉管理员:
    "1 号到了通知我, 2 号到了通知我, ..., 10000 号到了通知我"
  管理员在每格货架上装了个铃铛。
  之后你坐在前台等, 不用去仓库。
  哪个包裹到了 → 铃铛响 → 你过去拿那个包裹就行。
  → 不用跑仓库, 不用一个个问, 到了的自然知道。
```

**select/poll = 你主动去仓库逐个检查；epoll = 到了会响铃通知你。**

###### ① select——最早的方案（1983）

```c
fd_set readfds;               // 位图数组, 只有 1024 位
FD_SET(fd, &readfds);         // 把 fd 标记在位图上
int ret = select(maxfd+1, &readfds, NULL, NULL, NULL);
```

Python 的 `select` 模块直接对应 C 的 select 系统调用：

```python
import select

readfds = [server_fd, conn1, conn2]    # 要监控的 fd 列表 (不限于 1024 个?)

# select 的工作方式: 把列表传给内核, 内核修改后返回
r, w, x = select.select(readfds, [], [], timeout=None)
# r 里只有"就绪可读"的 fd
# 但 Python 的 select() 在 Windows 上支持 socket 对象,
# 在 Unix 上支持任何 fd, 不过底层仍然是那个古老的 select 系统调用
# → 性能瓶颈和 C 的 select 一样: O(n) 遍历 + 1024 限制
```

注意 Python 的 `select.select()` 接受的列表长度也受 `FD_SETSIZE` 限制（通常 1024），超过会抛 `ValueError`。所以在 Python 里如果用裸 `select.select()` 处理 10000 连接，同样第一步就卡死了。

Java 没有直接暴露 select()——Java NIO 的 `Selector` 底层用的是 poll/epoll/kqueue。如果要模拟 select 的"1024 上限 + O(n)遍历"效果，只能自己限制：

```java
// Java 模拟 select 的行为 (非实际使用方式)
Selector selector = Selector.open();
// 注册 fd... 
while (true) {
    selector.select();                        // 阻塞等待
    Set<SelectionKey> keys = selector.selectedKeys();
    // select 的 O(n) 体现在这里:
    for (SelectionKey key : keys) {           // 遍历就绪的 key
        // 处理这个 key
    }
    // 但 Java Selector 底层实际用的是 epoll/kqueue,
    // 所以这里的 "遍历" 只遍历就绪的 key, 不是全部 fd
}
```

```
select 的工作方式:
               用户空间                        内核空间
    ┌──────────────────────┐
    │ fd_set (1024 位位图)  │
    │ [1][1][0][1][0]...   │──── ① 拷贝到内核 ──►┌─────────────────┐
    │ 标记了要关注的 fd     │                     │ 遍历所有 fd,     │
    │                      │◄─── ② 拷贝回用户 ───│ 修改位图标记就绪 │
    │                      │                     └─────────────────┘
    │ 遍历位图, 找哪些 fd  │
    │ 还在集合里 (O(n))     │
    └──────────────────────┘

问题:
  ① fd_set 只有 1024 位 → 最多监控 1024 个 fd
  ② 每次 select() 都要把整个位图拷贝到内核 (O(n))
  ③ 内核要遍历所有 fd 检查状态 (O(n))
  ④ 返回后用户还得遍历位图找出就绪的 fd (O(n))
  → 总共 O(n) 拷贝 + O(n) 内核遍历 + O(n) 用户遍历
```

**select 处理 10000 连接会怎样？** 第一步就卡死了——位图只有 1024 格。10000 个 fd 根本塞不进去。

###### ② poll——去掉了 1024 限制（1997）

```c
struct pollfd fds[10000];         // 数组, 想多大就多大
fds[0].fd = fd;
fds[0].events = POLLIN;          // 关注读事件
int ret = poll(fds, nfds, -1);   // 阻塞等
```

Python 的 `select.poll()` 对应 C 的 poll，去掉了 1024 限制：

```python
import select

poll = select.poll()

# 注册 fd (不限制数量)
poll.register(server_fd, select.POLLIN)
poll.register(conn1, select.POLLIN)
poll.register(conn2, select.POLLIN)
# ... 可以注册 10000 个, 不会报错

# 阻塞等待, 返回就绪列表
events = poll.poll(timeout=None)   # 返回 [(fd, event), ...]

# 但内部仍然遍历所有注册的 fd
# 10000 个注册 → 每次 poll() 拷贝 10000 个 fd 到内核
#             → 内核遍历 10000 次
#             → 还是 O(n), 只是没有 1024 上限了
```

Java 的 `Selector` 在 Linux 上底层就是 epoll，但在一些旧平台或特定配置下可能退化到 poll 模式：

```java
// Java Selector —— 底层可能是 epoll, 也可能是 poll
// 可以通过 -Djava.nio.channels.spi.SelectorProvider 指定实现
Selector selector = Selector.open();             // Linux → EPollSelectorProvider

ServerSocketChannel server = ServerSocketChannel.open();
server.bind(new InetSocketAddress(8080));
server.configureBlocking(false);
server.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();                           // 底层: epoll_wait() 或 poll()
    for (SelectionKey key : selector.selectedKeys()) {
        // 处理就绪的事件...
        // Java 帮你做了"遍历就绪列表"这一步
    }
}
```

和 select 本质一样，只是**把 1024 位图换成了无长度限制的数组**。其他问题照旧：
- 每次还是要把整个数组拷贝到内核 (O(n))
- 内核还是遍历所有 fd (O(n))
- 用户还是遍历所有 fd 检查状态 (O(n))

**poll 处理 10000 连接会怎样？** 每次调用拷贝 10000 个 fd 到内核 → 内核轮询 10000 次 → 返回后用户再遍历 10000 次。**连 CPU 缓存都装不下，全是内存访问，慢。**

###### ③ epoll——主动通知（2002，Linux 2.5.44）

**核心思想转变：从"应用程序去问内核"变成"内核主动告诉应用程序"。**

```c
// 1. 创建 epoll 实例 (只做一次)
int epfd = epoll_create1(0);

// 2. 注册 fd (只做一次, 不是每次)
struct epoll_event ev;
ev.events = EPOLLIN;
ev.data.fd = fd;
epoll_ctl(epfd, EPOLL_CTL_ADD, fd, &ev);

// 3. 等事件 (只拿就绪的)
while (1) {
    int n = epoll_wait(epfd, events, MAX_EVENTS, -1);
    for (int i = 0; i < n; i++) {
        process(events[i].data.fd);  // 直接处理, 不需要遍历
    }
}
```

Java 的 `Selector` 在 Linux 上底层就是 epoll：

```java
// Java Selector 在 Linux 上默认使用 epoll (从 Java 1.4 开始)
Selector selector = Selector.open();   // epoll_create 的封装

ServerSocketChannel server = ServerSocketChannel.open();
server.configureBlocking(false);
server.register(selector, SelectionKey.OP_ACCEPT);  // epoll_ctl ADD

while (true) {
    int n = selector.select();         // epoll_wait 的封装
    Set<SelectionKey> keys = selector.selectedKeys();
    // 返回的 keys 只有就绪的 fd, 不需要遍历全部
    for (SelectionKey key : keys) {
        if (key.isAcceptable()) {
            // accept 新连接
        } else if (key.isReadable()) {
            // recv 数据
        }
    }
    keys.clear();                      // 必须清空, 否则下次还会处理
}
```

Java 的 `Selector.select()` 直接对应 C 的 `epoll_wait()`，`register()` 对应 `epoll_ctl(ADD)`。你写的 Java NIO 代码就是在用 epoll。

Python 的 `selectors` 模块提供了比裸 `select.epoll()` 更友好的封装，但本质相同：

```python
import selectors
import socket

# selectors 是 Python 3.4+ 对 select/poll/epoll 的统一封装
# 不同平台自动选择: Linux → EpollSelector, macOS → KqueueSelector, Windows → SelectSelector
sel = selectors.DefaultSelector()

# 底层调用 epoll_create1() (Linux)
# 注册 server socket → 底层调用 epoll_ctl(EPOLL_CTL_ADD)
server = socket.socket()
server.bind(('0.0.0.0', 8080))
server.listen(128)
server.setblocking(False)
sel.register(server, selectors.EVENT_READ, data="accept")

# epoll_wait 就绪事件 (只返回就绪的 fd, O(1))
while True:
    events = sel.select(timeout=None)   # ← 对应 epoll_wait
    for key, mask in events:
        # key.fileobj → fd, key.data → 你附加的数据
        pass
```

如果你直接用低阶的 `select.epoll()`（Linux only），能更清楚地看到和 C 的一一对应：

```python
import select

# 1. epoll_create  →  epoll() | epoll_create1()
ep = select.epoll()

# 2. epoll_ctl ADD →  register()
ep.register(fd, select.EPOLLIN)

# 3. epoll_wait    →  poll()
events = ep.poll(timeout=None)

# 4. events 只包含就绪的 fd, 不是全部
for fd, event in events:
    data = epfd.recv(1024)   # 直接读, 不需要问"这个 fd 就绪了吗"
    print(fd, event)
```

`select.epoll()` → `epoll_create()`  |  `.register()` → `epoll_ctl(ADD)`  |  `.poll()` → `epoll_wait()`  |  返回就绪列表 → 无需遍历全体 fd

**epoll 内核里维护了三张表：**

```
内核里的 eventpoll 结构体:

┌─────────────────────────────────────┐
│           ① 红黑树 (rbr)             │
│                                     │
│ 用于管理所有注册的 fd:                │
│   epoll_ctl(ADD) → 插入红黑树         │
│   epoll_ctl(DEL) → 从红黑树删除       │
│   查找速度: O(log n)                 │
│                                     │
│     ┌───┐  ┌───┐  ┌───┐            │
│     │fd4│  │fd7│  │fd9│  ...        │
│     └───┘  └───┘  └───┘            │
├─────────────────────────────────────┤
│          ② 就绪链表 (rdlist)         │
│                                     │
│ 数据到达时, 内核把就绪的 fd 加进来:   │
│   硬件中断 → TCP 处理 → 数据到      │
│   → 回调 → fd 加入就绪链表           │
│   添加速度: O(1) (链表头插)           │
│                                     │
│     ┌───┐  ┌───┐                   │
│     │fd7│→ │fd4│→ NULL             │
│     └───┘  └───┘                   │
├─────────────────────────────────────┤
│          ③ 等待队列 (wq)             │
│                                     │
│ 当没有就绪 fd 时:                     │
│   调用 epoll_wait 的进程挂在这里      │
│   数据到达 → 唤醒进程                 │
│   → 进程从等待队列移到运行队列        │
└─────────────────────────────────────┘
```

**对比三者的工作方式，差异一目了然：**

```
select / poll:
  每次调用 → 拷贝全部 fd 到内核
          → 内核逐个检查每个 fd 的状态
          → 返回全部 fd 集合
          → 用户遍历全部 fd 找出就绪的
  ★ N 个 fd → O(N) 拷贝 + O(N) 检查 + O(N) 遍历

epoll:
  注册时   → 把 fd 加入内核红黑树 (只做一次)
  每次调用 → 只读取就绪链表
          → 返回的只有就绪的 fd
  ★ 无论注册了多少 fd, 每次只拿有事件的 → O(就绪数量)
```

---

###### 三张表是怎么协调工作的

epoll 的三张表（红黑树、就绪链表、等待队列）不是各管各的，而是**一个完整的数据流管道**：

```
┌─────────────────────────────────────────────────────────────┐
│                   三张表的协作流程                              │
│                                                               │
│  ① 你调 epoll_ctl(ADD, fd)                                    │
│     │                                                         │
│     ├── 把 fd 插入红黑树                                       │
│     │   (以后 epoll_wait 只需要检查红黑树中的 fd)                │
│     │                                                         │
│     └── 在内核的 sock→sk_data_ready 回调链上挂一个钩子           │
│         这个钩子叫 ep_poll_callback                            │
│         (告诉内核: "这个 fd 有数据了, 通知我")                   │
│                                                               │
│  ② 你调 epoll_wait()                                          │
│     │                                                         │
│     ├── 检查就绪链表                                           │
│     │   ├── 链表不为空 → 取出链表内容, 返回给用户                │
│     │   └── 链表为空 → 当前进程加入等待队列, 进程睡眠            │
│     │                                                         │
│     └── 函数返回, 用户拿到就绪 fd 列表                          │
│                                                               │
│  ③ 网卡收到数据包 (硬件中断上下文)                               │
│     │                                                         │
│     ├── 1. DMA → 内核缓冲区                                    │
│     ├── 2. 软中断 → TCP 协议栈处理                              │
│     ├── 3. 数据放到 sock 的 sk_receive_queue                    │
│     │                                                         │
│     ├── 4. 触发 sk_data_ready 回调 → ep_poll_callback          │
│     │     │                                                    │
│     │     ├── 检查是否已在就绪链表 (通过红黑树找到 fd 的状态)      │
│     │     ├── 不在 → 加入就绪链表 (O(1), 链表头插)              │
│     │     │                                                    │
│     │     └── 检查等待队列是否有进程                             │
│     │          有 → 唤醒进程 (进程从 wq 移到运行队列)            │
│     │                                                           │
│     └── ⑤ 进程被唤醒, epoll_wait() 返回就绪链表内容             │
```

**按"谁操作哪张表"来画：**

```
                 应用程序 (用户空间)                   内核空间
                 ──────────────────                  ────────

  epoll_ctl(ADD)                                  ① 红黑树
                 ──────────────────────────────►    (存储所有注册的 fd)
                 register(fd, events)              
                                                      │
                                                   ② 就绪链表
                 ◄──────────────────────────────    (存储有事件的 fd)
  epoll_wait()   返回就绪的 fd                      ↑
                                                    │ 数据到达 → 回调
                                                 ③ 等待队列
                 epoll_wait() 进程睡眠 ──────────►  (存储等待的进程)
                 数据到达 → 唤醒 ◄────────────────
```

**一个完整的生命周期例子：**

```
初始状态: 红黑树有 3 个 fd (4, 7, 9), 就绪链表为空, 等待队列为空

你的进程调 epoll_wait():
  → 就绪链表为空 → 进程加入等待队列, 睡眠

网卡收到 fd=7 的数据:
  → DMA → 软中断 → TCP 处理 → 数据放到 sk_receive_queue
  → 触发回调 ep_poll_callback
    → fd=7 不在就绪链表中 → 头插加入就绪链表
    → 等待队列有进程 → 唤醒

你的进程醒来, epoll_wait() 返回:
  → 拿到就绪链表: [7]
  → 处理 fd=7 的数据
  → 下次再调 epoll_wait() → 就绪链表空了 → 又睡眠

同时 fd=4 和 fd=9 也有数据到了:
  → 各自触发回调 → 加入就绪链表 → [4, 9]
  → 但你的进程在忙, 没调 epoll_wait()
  → 就绪链表滞留 [4, 9]

你处理完 fd=7, 又调 epoll_wait():
  → 就绪链表不为空 → 直接返回 [4, 9]
  → 不需要睡眠, 不需要等
```

**所以三张表的角色一句话总结：**
- **红黑树** = 花名册（记录了所有要关注的 fd，长期保存）
- **就绪链表** = 叫号屏（只显示当前有数据要处理的 fd，临时缓存）
- **等待队列** = 候诊椅（没有叫号时，坐着等叫号的人）

一个具体的数让你感受差距：

```
假设有 10000 连接, 其中 5 个有数据:

select / poll:
  ① 拷贝 10000 个 fd 到内核   ← 拷贝 10000
  ② 内核遍历 10000 个 fd      ← 检查 10000 次
  ③ 返回后遍历 10000 个 fd    ← 遍历 10000 次
  ④ 找到 5 个就绪的            ← 工作只做 5 个
  → 总共操作: 30000 次, 有效工作: 5 次
  → 有效率: 0.017%

epoll:
  ① 只读取就绪链表 (5 个 fd)   ← 拷贝 5
  ② 直接处理这 5 个 fd         ← 工作 5 个
  → 总共操作: 5 次, 有效工作: 5 次
  → 有效率: 100%
```

这就是 epoll 让 NIO 支持 C10K 的根本原因——**操作量与连接总数无关，只与活跃连接数有关。**

###### 那 select 和 poll 还有存在的必要吗？

有。原因不是性能，是**跨平台兼容性**：

| 函数 | 可用平台 | 说明 |
|------|---------|------|
| `select` | **所有操作系统** | POSIX 标准, 从 1983 年就有。嵌入式、旧 Unix、Windows 都能用 |
| `poll` | **几乎所有类 Unix** | POSIX 标准, Linux/BSD/macOS 都有。Windows 没有原生 poll |
| `epoll` | **仅 Linux** | Linux 2.5.44+ 专属, 其他系统用不了 |
| `kqueue` | **仅 BSD/macOS** | macOS/iOS/FreeBSD 用这个, Linux 上没有 |
| `IOCP` | **仅 Windows** | Windows 的异步 IO 模型, 和其他都不一样 |

所以写跨平台代码时，你不能直接调 `epoll_create`——它在 macOS 上不存在。解决办法：

```
低层库的适配策略 (libevent / libuv / Python selector / Java Selector 都是这么做的):

  epoll   ← Linux 优先
  kqueue  ← macOS/BSD 用这个
  poll    ← 其他 Unix
  select  ← 最后的保底 (所有平台都有)

  Python DefaultSelector 的源码:
    try:    from select import epoll      → 用 epoll
    except: from select import kqueue     → 用 kqueue
    except: from select import poll       → 用 poll
    except: from select import select     → 用 select (保底)

  Java Selector 的实现:
    Linux → EPollSelectorProvider     (epoll)
    macOS → KQueueSelectorProvider    (kqueue)
    Windows → WindowsSelectorProvider (select 模拟, Windows 没有 poll/epoll)
```

**结论：** select 和 poll 就像"万用螺丝刀"——它不先进，但**在所有工具箱里都能找到**。epoll 是电动螺丝刀——快得多，但你得先确认插座在哪。

**select/poll/epoll 一句话对比：**

```
select:  你给我 1024 个 fd 位图, 我一个个检查谁有数据
poll:    你给我一堆 fd 数组, 我一个个检查谁有数据
epoll:   你注册 fd, 数据来了我主动把 fd 加入就绪列表, 你来取
```

###### ④ 统一视角：三种机制本质上都是"注册 → 等事件 → 回调"

三者的核心流程完全一致，区别只在内核怎么"等事件"：

```
                      用户程序                         内核
                 ┌─────────────────┐
    ① 注册      │  注册 fd 到内核   │─────── select/poll/epoll_ctl ────►┌─────────────┐
                 │  (告诉内核我想     │                                   │ 注册列表:     │
                 │   关注哪些 fd)    │                                   │ fd 1,2,3...  │
                 └─────────────────┘                                   └─────────────┘
                                                                              │
                 ┌─────────────────┐                                        │
    ② 等待事件   │  阻塞等待        │◄────── 数据来了! ────────────────────┘
                 │  select/poll/   │         内核检查哪些 fd 就绪          内核搜索 fd 的
                 │  epoll_wait     │                                       过程:
                 └──────┬──────────┘
                        │                                                  ┌─────────────┐
                        ▼                                                   │ fd 1: 有数据 │
                 ┌─────────────────┐                                       │ fd 2: 无数据 │
    ③ 回调处理   │  遍历就绪列表    │◄──── 返回就绪的 fd ──────────────────│ fd 3: 有数据 │
                 │  处理每个 fd 的  │                                       │ fd 4: 无数据 │
                 │  数据            │                                       └─────────────┘
                 └─────────────────┘                                        select/poll: O(n) 遍历全部
                                                                           epoll: O(1) 只取就绪链表
```

###### 核心问题：用户程序怎么知道要注册哪些 fd？

答案：**一开始只知道 server socket 的 fd**，其他 fd 是随着 accept() 逐个发现的。

```
① 初始时: 只有 server socket
   用户程序                     内核
   ┌────────────────┐
   │ server_fd = 3  │──注册──►┌──────────────┐
   │ sel.register(  │         │ 监听列表      │
   │  server_fd,    │         │ ┌─┐          │
   │  EVENT_READ)   │         │ │3│← server  │
   └────────────────┘         │ └─┘          │
                              └──────────────┘
         │                              │
         ▼                              ▼
   sel.select() 阻塞              等待事件到来
         │                              │
         ▼                              ▼
② 新连接来了: server_fd 就绪
   用户程序                     内核
   ┌────────────────┐
   │ sel.select()   │◄──就绪───┐──────────────┐
   │    返回 →      │  返回     │ 监听列表      │
   │ server_fd 可读  │  [fd=3]  │ ┌─┐          │
   │                │          │ │3│          │
   │ conn, addr =   │          │ └─┘          │
   │   server.      │          └──────────────┘
   │   accept()     │
   │ conn_fd = 4    │
   │                │
   │ sel.register(  │──注册──►┌──────────────┐
   │  conn_fd,      │         │ 监听列表      │
   │  EVENT_READ)   │         │ ┌─┬─┐        │
   └────────────────┘         │ │3│4│← 新增   │
                              │ └─┴─┘        │
                              └──────────────┘
         │                              │
         ▼                              ▼
   sel.select() 阻塞              等待事件到来
         │                              │
         ▼                              ▼
③ conn 发来数据: conn_fd 就绪
   用户程序                     内核
   ┌────────────────┐
   │ sel.select()   │◄──就绪───┐──────────────┐
   │    返回 →      │  返回     │ 监听列表      │
   │ conn_fd 可读   │  [fd=4]  │ ┌─┬─┐        │
   │                │          │ │3│4│        │
   │ data = recv(   │          │ └─┴─┘        │
   │   conn_fd)     │          └──────────────┘
   │ → "GET /..."   │
   └────────────────┘

反复循环:
  每次 accept() → 把新 conn 注册进去
  每次 close()  → 把该 fd 从监听列表移除
  这样内核就一直知道"用户对哪些 fd 感兴趣"
```

所以**用户程序不是预先知道所有 fd 的**，而是:
1. 最开始只注册 server socket（唯一一个预先知道的 fd）
2. `select()` 返回后检查是不是 server socket 就绪 → 是的话 `accept()` → 拿到新 fd 并注册
3. 下次 `select()` 返回后检查是不是某个 conn socket 就绪 → 是的话 `recv()` 读数据
4. 连接关闭时 `unregister()` 移除

这就是事件驱动的核心：**你不知道谁会来，但你注册了"谁来都要通知我"。**

###### 追问：内核收到数据时已经知道 fd 了，为什么 select/poll 还要遍历？

```


    你问得好! 数据到了内核确实知道是哪个 fd:
       网卡 → 内核协议栈 → 根据 IP:PORT 找到 socket → fd=2
       这一步不需要遍历, 路由查找是 O(1) 的

    那为什么 select/poll 还要遍历全部 fd 呢?

    答: 因为一个核心矛盾——
        "数据到 socket"  和  "select 要知道哪个 fd 就绪"
        是两套独立的机制, 中间没有"注册数据库"连接它们


    先看数据到了之后内核做了什么:

    ┌──────────────────────────────────────────────────────────┐
    │  数据包到达 → 协议栈找到 socket(fd=2)                      │
    │                                                          │
    │  ① 把数据放入 sk_receive_queue (socket 自己的接收队列)     │
    │                                                          │
    │  ② 调用 sock_def_wakeup() 或 sk_data_ready()              │
    │     → 这个函数遍历 socket 的 wait_queue (等待队列)          │
    │     → 唤醒正在睡眠的进程 (比如在 select() 里阻塞的)          │
    │                                                          │
    │  ③ select() 被唤醒, 但它只知道"有数据来了",                 │
    │     它不知道是哪个 fd!                                     │
    │     因为 wait_queue 里只记录了"哪个进程在等",                │
    │     没有记录"这个进程在等哪些 fd"                            │
    │                                                          │
    │  ④ 所以 select() 只能重新遍历用户传进来的所有 fd:            │
    │     for (int i = 0; i < nfds; i++) {                     │
    │         if (FD_ISSET(i, &readfds)) {                     │
    │             // 挨个问: 你现在有数据吗?                      │
    │             if (poll(&fds[i])) → 标记就绪                  │
    │         }                                                │
    │     }                                                    │
    │     这个遍历是 socket 和 select 之间的"信息缺口"             │
    └──────────────────────────────────────────────────────────┘


    来张图的对比, 为什么 epoll 不用遍历:

    select/poll 的架构:
                            TCP 数据包到了 fd=2
                                   │
                                   ▼
                         ┌─────────────────┐
                         │ socket 的等待队列  │
                         │ (wait_queue)     │
                         │ ┌───────────────┐│
                         │ │ 睡眠中的进程   ││  ← 只记录了"谁在等"
                         │ └───────────────┘│     没记录"等哪些 fd"
                         └─────────────────┘
                                   │ 唤醒进程
                                   ▼
         ┌──────────────────────────────────┐
         │ 进程醒来, 在 select() 内核代码里   │
         │ 遍历用户传的 fd_set:              │
         │ for fd in 位图:                   │
         │   poll(fd) → 有数据?              │  ← O(n) 遍历
         └──────────────────────────────────┘


    epoll 的架构:
                            TCP 数据包到了 fd=2
                                   │
                                   ▼
                         ┌─────────────────┐
                         │ socket 的等待队列  │
                         │ (wait_queue)     │
                         │ ┌───────────────┐│
                         │ │ epoll 的回调   ││  ← 注意这里!
                         │ │ ep_poll_      ││    不是"进程"而是"回调函数"
                         │ │ callback()    ││
                         │ └───────────────┘│
                         └─────────────────┘
                                   │ 执行回调
                                   ▼
         ┌──────────────────────────────────────────┐
         │ ep_poll_callback() 直接把 fd=2            │
         │ 加入 epoll 的 ready_list (就绪链表)         │
         │ 然后唤醒进程                              │
         │                                          │
         │ 进程在 epoll_wait() 里醒来:                │
         │ n = epoll_wait(epfd, events, max, -1)    │
         │ events 里直接就是 [fd=2]                  │  ← O(1), 不遍历
         └──────────────────────────────────────────┘


    用类比理解:

    select/poll:  老师(内核)说"作业做完了的举个手"
                 → 所有学生(fd)都举手, 但有些没做完也举手了
                 → 老师得一个个检查: 你真的做完了吗?
                 (唤醒所有人, 再一个个确认)

    epoll:       学生做完作业后自己去讲台登记
                 → 老师只念登记表上的人名
                 (谁做完谁来登记, 老师直接拿名单)

    select/poll 每次唤醒都"广撒网", epoll 只动"有变化"的。
    这就是为什么 epoll 叫"事件驱动"——不是轮询, 而是真正的事件通知。
```

###### 具体流程展开（以 4 个连接为例）：

```
连 接 池:        [conn1]  [conn2]  [conn3]  [conn4]
                    │        │        │        │
                    ▼        ▼        ▼        ▼
① 注册阶段:      注册到内核的监听列表
                 ┌──────────────────────────────┐
                 │  内核监听列表                  │
                 │  ┌─┬─┬─┬─┐                  │
                 │  │1│2│3│4│  ← fd            │
                 │  └─┴─┴─┴─┘                  │
                 └──────────────────────────────┘
                         │
                         ▼
② 新数据到来:    conn2 收到了 HTTP 请求
                 ┌──────────────────────────────┐
                 │  内核收到数据包                │
                 │  sk_data_ready(fd=2)         │
                 │                              │
                 │  select/poll:                │
                 │    遍历 {1,2,3,4}            │
                 │    发现 2 有数据              │
                 │    标记 fd=2 就绪            │
                 │                              │
                 │  epoll:                     │
                 │    回调 → 把 fd=2 加入        │
                 │    就绪链表（不用遍历）        │
                 └──────────────────────────────┘
                         │
                         ▼
③ 返回就绪:      内核返回给用户
                 select/poll: 修改后的位图/数组
                  （用户还需要遍历找哪些变了）
                 epoll: 就绪链表 [fd=2]
                  （直接拿到就绪的, 不用遍历）
                         │
                         ▼
④ 用户回调:      用户程序处理 conn2 的数据
                 read(conn2_fd) → "GET / HTTP/1.1..."
                 process() → send()
                 ↓
                 继续阻塞在 select/poll/epoll_wait
                 等待下一批事件
```

**关键结论：** 无论 select、poll 还是 epoll，对应用程序来说使用模式一模一样——**注册事件、阻塞等待、处理就绪**。区别是：
- `select/poll`：**每次调用都遍历全部注册的 fd**——连接越多，遍历越慢
- `epoll`：**只取真正就绪的 fd**——处理速度与活跃连接数有关，与总连接数无关

这就是为什么 NIO 要用 epoll 才能扛住 C10K，用 select 的话光遍历 10000 个 fd 就已经把 CPU 吃完了。

##### 6.4.5 四种 IO 模型对比

从"一个线程能处理多少连接"这个维度，把所有模型排开：

```
 单线程 ─────────────────────────────────────────────── 多线程
 BIO                    线程池      NIO+Selector      AIO/Proactor
单线程                   │              │                  │
  │                      │              │                  │
  ▼                      ▼              ▼                  ▼
 1 连接                  N 连接         ∞ 连接              ∞ 连接
 1 线程                  线程池 N      1/N 线程            1/N 线程
 串行处理                串行+排队      事件驱动            异步回调
```

**详细对比表：**

| 模型 | 线程数 | 一个线程等多少连接 | 谁在等数据 | 内存开销 | CPU 效率 | 开发复杂度 | 适合场景 |
|------|--------|-----------|---------|---------|---------|---------|---------|
| **BIO 单线程** (2.5.1) | 1 | 1 | 应用程序 recv() | 极低 | 低 (串行) | 极低 | 学习演示 |
| **BIO 每连接一线程** | N = 连接数 | 各 1 | 每个线程 recv() | **极高** (N×8MB) | 低 (大量线程切换) | 低 | 连接少 <500 |
| **线程池 + BIO** | 固定池大小 | 各 1 (池满排队) | 线程池线程 recv() | 中 (池大小×8MB) | 中 (池内切换) | 中 | 连接少, 任务重 |
| **NIO + Selector** | 1~N (核心数) | **成千上万** | **内核 epoll_wait()** | 极低 (N×KB) | **高** (只处理就绪) | **高** | 连接多 >1000 |
| **AIO / Proactor** | 1~N | 成千上万 | 内核回调 | 极低 | 高 | **最高** | 高性能文件/网络 |

**具体算一笔账，感受差距：**

```
场景: 10000 连接, 每个连接平均每秒发 1 个请求
      (活跃连接 ≈ 10000, 但瞬间就绪 ≈ 几十个)

BIO 每连接一线程:
  线程数 = 10000
  每个线程栈 8MB → 10000 × 8MB = 80GB 内存
  CPU: 10000 个线程调度, 大量上下文切换
  → 操作系统先崩溃, 程序根本跑不起来

线程池 (max_workers=200):
  线程数 = 200
  每个线程栈 8MB → 200 × 8MB = 1.6GB 内存
  但: 200 个线程全在 recv() 上阻塞等数据
      只有几十个连接真有数据, 但 200 个线程都在抢 CPU 轮询?
      不, 阻塞的线程不占 CPU, 但占内存
      而且: 只有 200 个线程 → 最多同时处理 200 个 recv
      其他 9800 个连接的数据在 sk_receive_queue 排队

NIO + Selector (1 线程):
  线程数 = 1
  栈内存 ≈ 8MB (1 个线程)
  epoll 实例: eventpoll + 10000×epitem ≈ 几百 KB
  → 总内存 ≈ 9MB (vs BIO 的 80GB)
  → 1 个线程等 10000 个 fd, 就绪几个处理几个
  → 没有线程切换开销
```

**四种模型的全链路数据流对比：**

```
BIO 单线程:
  accept() → recv() → process() → send() → close()
  ↑ 整个过程串行, 下一个连接必须等前一个完成

BIO 多线程:
  主线程: accept() → 开线程 → accept() → 开线程 → ...
  每个工作线程: recv() → process() → send() → close()
  ↑ 每个连接独占一个线程, 线程数 = 连接数

线程池:
  主线程: accept() → pool.submit() → accept() → ...
  工作线程池: 取一个空闲线程 → recv()(阻塞) → process() → send() → close() → 回池
  ↑ 线程数固定, 超过池大小的连接在任务队列排队

NIO + Selector:
  单线程: select() 等事件
              ↓
          accept → 注册 conn 到 selector
              ↓
          recv → process → send (全部非阻塞)
              ↓
          select() 等下一批事件
  ↑ 不等待具体某个连接, 而是等待"任何连接有事件"
```

**一句话选择指南：**

- 写作业、做实验 → **BIO 单线程**（最简单）
- 内部工具、连接数 < 500 → **线程池 + BIO**（够用）
- 高并发 Web 服务器、连接数 > 1000 → **NIO + Selector**（必须）
- 文件 IO 密集、需要极致性能 → **AIO / Proactor**（Netty 的方案）

##### 6.4.6 NIO 的局限——为什么还需要 Reactor

上面的 NIO 代码有个问题：**所有逻辑混在事件循环里**。

```python
while True:
    events = sel.select()
    for key, mask in events:
        # 如果在这里处理请求, 一个请求慢了会阻塞后续所有事件
        # 比如这里的 read() 里调用了数据库查询...
        if key.data == "read":
            handle_request(key.fileobj)   # ← 如果阻塞了, 整个事件循环卡死
```

而且没有**注册/注销/错误处理**的统一框架。所以 NIO 通常是更上层框架（Reactor / Proactor）的底层基础设施，不会直接裸露使用——这就是下一节 6.5 Reactor 要做的事。

### 6.5 Reactor 模型

##### 6.5.1 Reactor 是什么——先解决 NIO 的两个问题

裸 NIO（6.4 的代码）有两个问题：

**问题 1：事件处理逻辑散落一地**

```python
# 6.4 的代码: accept() 和 read() 是游离的函数
while True:
    events = sel.select()
    for key, mask in events:
        if key.data == "accept":   # ← 字符串匹配, 没有类型安全
            accept(key.fileobj)
        elif key.data == "read":
            read(key.fileobj)
```

每增加一种事件类型（如 `write`、`error`），就要在事件循环里加一个 `elif`。而且 accept/read 函数之间没有关系，没有统一的生命周期管理。

**问题 2：一个慢操作会卡死整个循环**

```python
while True:
    events = sel.select()
    for key, mask in events:
        handle_request(key.fileobj)  # ← 如果处理请求时查询数据库
                                      #    IO 阻塞, 所有连接都等
```

**Reactor 的方案：**
- 把每种事件的处理逻辑封装成**独立的 Handler 对象** → 解决问题 1
- Handler 内部必须**非阻塞**，耗时操作扔到线程池 → 解决问题 2

##### 6.5.2 Reactor 架构

```
                          ┌──────────────┐
                          │  新连接到达    │
                          └──────┬───────┘
                                 │
                    ┌────────────▼────────────┐
                    │     Reactor (反应器)     │
                    │                         │
                    │  ┌───────────────────┐  │
                    │  │ Event Demultiplexer│  │  ← epoll_wait() 从这里拿到事件
                    │  │ (select/epoll)    │  │
                    │  └────────┬──────────┘  │
                    │           │              │
                    │  ┌────────▼──────────┐  │
                    │  │  Event Dispatcher  │  │  ← 根据事件类型, 分发给对应 Handler
                    │  │  分发器            │  │
                    │  └────────┬──────────┘  │
                    │           │              │
                    └────────────┼────────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
   ┌──────▼──────┐      ┌───────▼───────┐      ┌───────▼───────┐
   │ AcceptHandler│      │  ReadHandler  │      │  WriteHandler  │
   │              │      │               │      │               │
   │ accept()→    │      │ recv()→       │      │ send()→       │
   │ 注册新连接   │      │ 解析+处理     │      │ 发送响应      │
   └──────────────┘      └───────────────┘      └───────────────┘
```

对比裸 NIO 和 Reactor 的事件循环：

```python
# 裸 NIO (6.4):          # Reactor (6.5):
while True:               while True:
    events = select()         events = select()
    for ev in events:         for ev in events:
        if ev == "accept":        handler = map[ev.fd]
            accept(fd)            handler.handle(fd)
        elif ev == "read":
            read(fd)
        # 每加一种类型加一个 elif
```

区别就是：**裸 NIO 的事件循环知道事件类型；Reactor 的事件循环只知道"把事件交给对应的 Handler"**。

##### 6.5.3 代码实现——完整的 Mini Reactor

先看整体架构：**Reactor 持有 epoll，Handler 处理具体业务，两者通过 register 绑定。**

```
 ┌─────────────────────────────────────────────────────────────┐
 │                     Mini Reactor 架构                        │
 │                                                             │
 │   ┌───────────────┐                                         │
 │   │   Reactor     │  (事件反应器, 持有 epoll)                 │
 │   │               │                                         │
 │   │  sel = epoll  │──── 事件循环 ────► 就绪 fd ──→ 查 Handler │
 │   │  handlers = {fd → handler}                              │
 │   └───────┬───────┘                                         │
 │           │ register(fd, handler)                           │
 │           ▼                                                 │
 │   ┌───────────────────┐   ┌───────────────────┐             │
 │   │  AcceptHandler    │   │  ReadHandler      │             │
 │   │                   │   │                   │             │
 │   │  handle_read():   │   │  handle_read():   │             │
 │   │  accept 新连接    │   │  recv 数据        │             │
 │   │  → 注册 ReadHdlr │   │  → 解析 HTTP      │             │
 │   └───────────────────┘   └───────────────────┘             │
 │                                                             │
 │   一个 Handler 绑定一个 fd:                                   │
 │   server_fd  →  AcceptHandler (只处理 accept)                │
 │   conn_fd    →  ReadHandler   (只处理 recv)                  │
 └─────────────────────────────────────────────────────────────┘
```

###### ① Reactor 核心——事件循环

Reactor 做的事很简单：**等事件 → 找到这个 fd 对应的 Handler → 调用它**。

```python
import selectors
import socket
```

```
事件循环内部:
                         sel.select()
                             │
                             ▼
                     ┌───────────────┐
                     │  阻塞等待      │
                     │  (epoll_wait) │
                     └───────┬───────┘
                             │ 返回就绪事件列表
                             ▼
                     ┌───────────────┐
                     │  遍历 events  │
                     │               │
                     │  key.data     │ ← 取出 Handler
                     │  key.fileobj  │ ← 取出 fd
                     └───────┬───────┘
                             │
                             ▼
                     ┌───────────────┐
                     │  handler.     │
                     │  handle_read( │
                     │   fileobj)    │ ← 处理业务
                     └───────────────┘
                             │
                             ▼
                        继续循环
```

```python
class Reactor:
    """事件反应器: 持有 epoll, 管理 Handler 注册, 运行事件循环"""
    def __init__(self):
        self.sel = selectors.DefaultSelector()  # Linux → epoll, macOS → kqueue
        self.handlers = {}                      # fd → handler 映射

    def register(self, fd, events, handler):
        """注册 fd 到 epoll, 关联对应的 Handler
           底层调用 epoll_ctl(EPOLL_CTL_ADD)
        """
        self.sel.register(fd, events, handler)  # handler 存在 key.data 里
        # 注意: sel.register 的 data 参数就是我们要的 handler
        # 所以在事件循环里可以直接 key.data 拿到 handler
        self.handlers[fd.fileno()] = handler     # 冗余记录, 方便查找

    def unregister(self, fd):
        """从 epoll 移除 fd, 底层调用 epoll_ctl(EPOLL_CTL_DEL)"""
        self.sel.unregister(fd)
        del self.handlers[fd.fileno()]

    def run(self):
        """事件循环: select → dispatch

           这是 Reactor 的核心, 也是唯一阻塞的地方
           所有事件处理都从这里分发出去
        """
        while True:
            events = self.sel.select()           # 阻塞等待就绪事件
            for key, mask in events:
                handler = key.data                # ← 从 key 里取出 Handler
                # 这里不需要判断 fd 类型了!
                # 因为注册时就绑定了正确的 Handler
                if mask & selectors.EVENT_READ:
                    handler.handle_read(key.fileobj)
                if mask & selectors.EVENT_WRITE:
                    handler.handle_write(key.fileobj)
```

**对比裸 NIO 的事件循环：**

```
裸 NIO:                            Reactor:
────────                            ────────
events = sel.select()              events = sel.select()
for key, mask in events:           for key, mask in events:
    if key.data == "accept":            handler = key.data    # ← 多态
        conn = server.accept()          handler.handle_read()
        ...
    elif key.data == "read":      不用写 if/elif!
        data = conn.recv()         Handler 自己知道该做什么
        ...
```

裸 NIO 的 `key.data` 里存的是字符串 `"accept"` / `"read"`，然后在事件循环里用 `if/elif` 判断该做什么。Reactor 把 `key.data` 存成 **Handler 对象**，事件循环只管调用 `handler.handle_read()`，具体逻辑由 Handler 子类决定。这就是 **策略模式**——把"判断"变成了"多态"。

###### ② Handler 基类——定义接口

```python
class EventHandler:
    """所有事件处理器的基类

    子类只需要覆盖自己关心的方法:
    - AcceptHandler → handle_read (accept 新连接)
    - ReadHandler   → handle_read (recv 请求数据)
    - WriteHandler  → handle_write (send 响应数据)
    """
    def handle_read(self, fd): pass
    def handle_write(self, fd): pass
```

```
Handler 类体系:

        EventHandler  (接口)
        ┌──┴──┐
        │     │
    AcceptHandler  ReadHandler
    (accept)       (recv/process)

    ┌─ AcceptHandler.handle_read():
    │   server.accept() → conn
    │   → 注册 conn + ReadHandler 到 Reactor
    │
    └─ ReadHandler.handle_read():
        conn.recv() → data
        → 解析 HTTP → 生成响应
```

###### ③ AcceptHandler——新连接从哪里来

```python
class AcceptHandler(EventHandler):
    """当监听 socket 可读时调用

    监听 socket "可读" 意味着有新连接等待 accept()
    这是新连接进入系统的唯一入口
    """
    def __init__(self, reactor):
        self.reactor = reactor                     # 持有 Reactor 引用
                                                   # 这样才能注册新 conn

    def handle_read(self, server):
        conn, addr = server.accept()               # 非阻塞, 一定有新连接
        conn.setblocking(False)                    # 新 conn 也设非阻塞
        handler = ReadHandler(self.reactor)        # 为这个 conn 创建 Handler
        self.reactor.register(conn,                # 注册到事件循环
                              selectors.EVENT_READ,
                              handler)
```

```
accept 数据流:

    客户端连接 → 内核完成三次握手 → server_fd 可读
                                          │
                                          ▼
    Reactor 事件循环:
       sel.select() 返回 → key.data = AcceptHandler
                                          │
                                          ▼
    AcceptHandler.handle_read():
       conn, addr = server.accept()       ← 拿到新连接
       conn.setblocking(False)            ← 设非阻塞
       handler = ReadHandler(...)
       reactor.register(conn, handler)    ← 注册到 epoll
                                          │
                                          ▼
    s
    epoll 现在监听:
       server_fd → AcceptHandler  (一直存在)
       conn_fd   → ReadHandler    (刚才注册的)
```

注意：**AcceptHandler 只需要 Handle_read**，因为监听 socket 只需要关注"可读"事件（有新连接）。它不需要 handle_write。

###### ④ ReadHandler——真正的业务处理

```python
class ReadHandler(EventHandler):
    """当连接 socket 可读时调用

    每个连接有一个自己的 ReadHandler 实例
    实例里维护自己的 buffer (拆包粘包)
    """
    def __init__(self, reactor):
        self.reactor = reactor
        self.buffer = b""                          # 每个连接的私有缓冲区

    def handle_read(self, conn):
        data = conn.recv(4096)                     # 非阻塞, 不一定能读完
        if not data:                               # 对端关闭连接 → 清理
            self.reactor.unregister(conn)
            conn.close()
            return

        self.buffer += data
        # 这里配合 3.5 的状态机解析 HTTP 请求
        # 如果解析出一个完整请求:
        #   response = handle_request(request)
        #   conn.sendall(response.to_bytes())
        #
        # 注意: sendall 可能阻塞! 因为 send buffer 满了时 send() 会阻塞
        # 完整方案是在 handle_write 里分批发送 (注册 EVENT_WRITE)
```

```
ReadHandler 数据流:

    ┌─ 新数据到达 → conn_fd 就绪 ──────────────────────┐
    │                                                  │
    ▼                                                  │
    Reactor 事件循环返回                               │
    → key.data = ReadHandler(conn)                     │
    → handler.handle_read(conn)                        │
                                                       │
    ┌──────────────────────────────────────────┐        │
    │ recv(4096)                               │        │
    │  → 有数据 → buffer += data               │  不断  │
    │  → 空数据 → unregister + close           │  循环  │
    │                                           │        │
    │ 尝试解析 buffer:                          │        │
    │  → 不完整 → 等下一次 recv                 │        │
    │  → 完整    → 处理请求 → send 响应         │        │
    └──────────────────────────────────────────┘        │
                                                       │
    └──────────────────────────────────────────────────┘
```

###### ⑤ 启动——把所有零件拼起来

```python
# 1. 创建 server socket
server = socket.socket()
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(('0.0.0.0', 8080))
server.listen(128)
server.setblocking(False)

# 2. 创建 Reactor
reactor = Reactor()

# 3. 注册 server_fd → 绑定 AcceptHandler
#    这是注册的第一个 fd, 也是唯一一个"预先知道"的 fd
reactor.register(server, selectors.EVENT_READ, AcceptHandler(reactor))

# 4. 启动事件循环 (永不返回)
reactor.run()
```

**完整的启动时序：**

```
① main 注册 server → Reactor → epoll:
   main → Reactor.register(server, AcceptHandler)
        → epoll_ctl(ADD, server_fd)   ✓ epoll 开始监听
        → Reactor.run()

② Reactor 等待事件:
   Reactor → epoll_wait(...)   阻塞, 等任何 fd 就绪

③ 新客户端连进来:
   内核三次握手完成 → server_fd 就绪
   epoll 返回 → Reactor 拿到 key.data = AcceptHandler

④ AcceptHandler 处理:
   Reactor → AcceptHandler.handle_read(server)
           → conn = server.accept()      ← 拿到新连接
           → ReadHandler = new handler
           → epoll_ctl(ADD, conn_fd)     ← 新 conn 加入监听
           → 回到步骤 ②

⑤ 客户端发来 HTTP 请求:
   数据到达 → conn_fd 就绪
   epoll 返回 → Reactor 拿到 key.data = ReadHandler

⑥ ReadHandler 处理:
   Reactor → ReadHandler.handle_read(conn)
           → data = conn.recv(4096)      ← 读请求数据
           → 解析 HTTP → 生成响应
           → 回到步骤 ②

⑦ 客户端断开:
   conn.recv() → b"" (空)
   → unregister(conn)                    ← 从 epoll 移除
   → conn.close()
   → 回到步骤 ②
```

**用图表示前 4 步（从启动到第一个连接建立）：**

```
 main      Reactor         epoll        AcceptHandler
  │          │               │               │
  │ register │               │               │
  │─────────►│ epoll_ctl ADD │               │
  │          │──────────────►│               │
  │          │               │               │
  │ run()    │               │               │
  │─────────►│ epoll_wait()  │               │
  │          │──────────────►│ (阻塞)        │
  │          │               │               │
  │          │  ── 客户端连上 ──              │
  │          │               │               │
  │          │◄──────────────│ server 就绪    │
  │          │               │               │
  │          │──── handle_read(server) ─────►│
  │          │               │               │
  │          │               │     accept()  │
  │          │               │     conn=4    │
  │          │ epoll_ctl ADD │               │
  │          │◄──────────────│ conn_fd       │
  │          │               │               │
  │          │ epoll_wait()  │               │
  │          │──────────────►│ (等待 data)   │
```

**步骤 ⑤ - ⑥（请求处理）：**

```
 Reactor         epoll         ReadHandler
   │               │               │
   │ epoll_wait()  │               │
   │──────────────►│ (阻塞)        │
   │               │               │
   │  ── HTTP 请求到 ──            │
   │               │               │
   │◄──────────────│ conn 就绪      │
   │               │               │
   │──── handle_read(conn) ───────►│
   │               │     recv()    │
   │               │     "GET /"   │
   │               │     process() │
   │               │     send()    │
   │               │               │
    │ epoll_wait()  │               │
    │──────────────►│ (下个事件)    │
```

**关键设计对比：裸 NIO vs Reactor**

```
裸 NIO:                              Reactor:
───────                              ───────

事件循环里写 if/elif 判断 fd 类型    事件循环只做 dispatch
                                   Handler 多态处理业务

注册时存字符串:                      注册时存 Handler 对象:
  sel.register(fd, "accept")          sel.register(fd, handler)
  sel.register(fd, "read")            sel.register(fd, handler)

key.data = "accept" → if 判断       key.data = handler → 直接调用
                                   key.data.handle_read(fd)

fd 和处理逻辑分散在事件循环里         fd 和处理逻辑封装在 Handler 里
                                   每个 Handler 可以有自己的状态 (buffer)

新功能需要改事件循环代码             新功能 → 新 Handler 子类
(开闭原则违反)                        (开闭原则满足)
```

##### 6.5.4 进阶：多线程 Reactor（Main-Reactor / Sub-Reactor）

先看清楚问题：**单线程 Reactor 里，一个慢 Handler 会卡死所有连接。**

```
时间轴 ───────────────────────────────────────────────────────────►

单线程 Reactor (6.5.3):

 线程 1  │ 读conn1 │  读conn2  │ ████ 处理 conn2 请求 ████ (3秒) │ 读conn3 │ ...
         │         │           │                                  │         │
         │ conn1~N → → → → 全部在 epoll 就绪队列里等着 ← ← ← ← ← ←│         │
         │         │           │                                  │         │
         第 1 秒    第 2 秒     第 3 秒                             第 4 秒   第 5 秒

   → CPU 4 核, 但只有 1 个线程在工作
   → conn2 的数据库查询花了 3 秒 → 这 3 秒其他连接全部饿死

多线程 Reactor (4 个 Sub Reactor):

 线程 1  │ 读conn1 │  读conn2  │ ████ 处理 conn2 请求 ████ (3秒) │ 写回    │ ...
 线程 2  │         │ 读conn3   │ ████ 处理 conn3 请求 ████ (3秒) │ 写回    │ ...
 线程 3  │         │           │ 读conn4 │ ████ 处理 conn4 ████  │ 写回    │ ...
 线程 4  │         │           │         │ 读conn5 │ 处理 conn5  │ 写回    │ ...
         │         │           │         │         │             │         │
         第 1 秒    第 2 秒     第 3 秒    第 4 秒   第 5 秒      第 6 秒

   → 4 核同时工作, 4 个请求并行处理
   → conn2 慢 → 只影响线程 1 上的 conn1 和 conn6
   → 线程 2/3/4 上的连接完全不受影响
```

**核心思想：多个事件循环并行，每个有自己独立的 epoll 实例和线程。**

```
单线程: 1 个 epoll 管所有 conn → Handler 慢 → 全部卡住

多线程: 每个 Sub Reactor 有自己 epoll 和自己的 conn 子集
        Sub 1: epoll1 + {conn1, conn5, conn9}
        Sub 2: epoll2 + {conn2, conn6, conn10}
        Sub 3: epoll3 + {conn3, conn7, conn11}
        Sub 4: epoll4 + {conn4, conn8, conn12}

        Sub 1 卡了? 其他 3 个照样跑
```

**那 Main Reactor 是干嘛的？**

Main Reactor 只做一件事：**accept 新连接，然后决定把这个 conn 交给哪个 Sub Reactor**。它不参与 read/write，所以永远不会被慢 Handler 拖累。

```
完整数据流:

          新连接连进来
               │
               ▼
     ┌─────────────────────┐
     │  Main Reactor       │  ← 1 个线程, 只 accept
     │  (1 个 epoll)       │
     │  → 只监听 server_fd │
     └──────┬──────────────┘
            │ 轮询分发
     ┌──────┴──────┐
     │  谁下一个?   │  round-robin
     └──────┬──────┘
            ▼
     ┌─────────────────────┐
     │  拿到 conn, 把它    │
     │  交给 Sub Reactor N │
     └──────┬──────────────┘
            │
            ▼
     ┌─────────────────────────────────────────────────────┐
     │  Sub Reactor N (自己的线程, 自己的 epoll)             │
     │                                                     │
     │  ① 把 conn 注册到自己的 epoll                         │
     │  ② epoll_wait() → conn 有数据 → ReadHandler 处理    │
     │  ③ 处理过程中卡了? 没事, 其他 Sub Reactor 继续跑     │
     └─────────────────────────────────────────────────────┘
```

**那跨线程怎么通信？（如何把一个 conn 从 Main 交给 Sub）**

```
Main 线程把 conn 给 Sub 线程:

    Main 线程                    Sub 线程 (在 epoll_wait 里阻塞)
    ┌───────────────┐
    │ conn_fd = 7   │          ┌────────────────────────────┐
    │               │          │ epoll_wait(epfd, ..., -1)   │
    │ ① 放入队列    │───────►  │       ↑ 阻塞在这里          │
    │ queue.put(7)  │          └────────────────────────────┘
    │               │
    │ ② 写 pipe     │          ┌────────────────────────────┐
    │ wakeup_w.send │───────►  │  epoll_wait 收到 pipe 事件  │
    └───────────────┘          │  → 醒来                     │
                               │  → 从队列取出 conn_fd=7    │
                               │  → epoll_ctl(ADD, 7)       │
                               │  → 继续 epoll_wait()       │
                               └────────────────────────────┘

    这个 pipe 没有传输数据:
    conn_fd=7 已经通过 queue(内存队列)传递了
    pipe 只是用来"敲醒" Sub 线程: "醒醒, 有东西在队列里"
```

**所以在实际代码中，分工是这样的：**

```
Main Reactor → 1 个线程, 1 个 epoll → 只监听 server_fd
Sub Reactor 1 → 1 个线程, 1 个 epoll → 监听 {pipe + conn1, conn5, ...}
Sub Reactor 2 → 1 个线程, 1 个 epoll → 监听 {pipe + conn2, conn6, ...}
Sub Reactor 3 → 1 个线程, 1 个 epoll → 监听 {pipe + conn3, conn7, ...}
Sub Reactor 4 → 1 个线程, 1 个 epoll → 监听 {pipe + conn4, conn8, ...}
```

每个 Sub Reactor 的 epoll 里都多了一个 pipe 读端——这是 Main 唤醒它的专用通道。

**代码（只看最核心的逻辑，其他和 6.5.3 一样）：**

```python
class SubReactor:
    """每个 Sub Reactor = 自己的线程 + 自己的 epoll + 自己的 conn 池"""
    def __init__(self):
        self.sel = selectors.DefaultSelector()
        self.conn_queue = queue.Queue()
        # 创建 pipe → 用来被 Main 唤醒
        self.pipe_r, self.pipe_w = socket.socketpair()
        self.pipe_r.setblocking(False)
        self.sel.register(self.pipe_r, selectors.EVENT_READ)
        # 启动线程
        self.thread = threading.Thread(target=self._run, daemon=True)
        self.thread.start()

    def add_connection(self, conn):
        """Main 线程调用: 把 conn 加进队列, 然后敲醒 Sub"""
        self.conn_queue.put(conn)
        self.pipe_w.send(b"\x00")          # ← 敲醒! epoll_wait 立即返回

    def _run(self):
        while True:
            events = self.sel.select()      # ← 每个 Sub 自己阻塞
            for key, mask in events:
                if key.fileobj == self.pipe_r:
                    # 被 Main 唤醒了, 取出队列里的 conn
                    self.pipe_r.recv(1024)
                    while not self.conn_queue.empty():
                        conn = self.conn_queue.get_nowait()
                        self.sel.register(conn, selectors.EVENT_READ, ReadHandler())
                else:
                    # 普通 conn 有数据, 正常处理
                    key.data.handle_read(key.fileobj)


class MainReactor:
    """只 accept, 然后把 conn 分发给 Sub Reactor"""
    def __init__(self, sub_count=4):
        self.sel = selectors.DefaultSelector()
        self.subs = [SubReactor() for _ in range(sub_count)]
        self.next = 0

    def handle_accept(self, server):
        conn, addr = server.accept()
        self.subs[self.next].add_connection(conn)   # 交给 Sub
        self.next = (self.next + 1) % len(self.subs)

    def run(self, host="0.0.0.0", port=8080):
        server = socket.socket()
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind((host, port))
        server.listen(128)
        server.setblocking(False)
        self.sel.register(server, selectors.EVENT_READ, self)
        while True:
            events = self.sel.select()     # ← Main 自己阻塞 (只等 accept)
            for key, mask in events:
                key.data.handle_accept(key.fileobj)


# 启动: 4 个 Sub Reactor (对应 4 核 CPU)
MainReactor(sub_count=4).run()
```

**总结：多线程 Reactor 到底解决了什么？**

```
单线程 Reactor:
  1 个线程, 1 个 epoll, N 个连接
  Handler 慢 → 事件循环卡死 → 所有连接等 → ❌

多线程 Reactor:
  1 + N 个线程, 1 + N 个 epoll, 连接被分成 N 组
  Handler 慢 → 只卡 1/N 的连接 → 其他正常 → ✅
  还利用了多核 CPU 并行处理 → 🚀
```

Netty 的 boss group (Main) + worker group (Sub) 和 Nginx 的 master + worker 全都是这个思想——**用多个事件循环并行，避免单点瓶颈。**

### 6.6 为什么 Nginx / Netty 用事件驱动模型

**Nginx 的架构：**

```
Nginx Master Process
  │
  ├── Worker 1 (事件循环)
  │     │
  │     └── epoll_wait() → 处理数千连接
  │
  ├── Worker 2 (事件循环)
  │     │
  │     └── epoll_wait() → 处理数千连接
  │
  └── Worker N (事件循环)
        │
        └── epoll_wait() → 处理数千连接
```

**核心原因：**

| 原因 | 说明 |
|------|------|
| C10K 问题 | 10000 连接时，BIO 需要 10000 线程，事件驱动只需要几个 |
| 内存效率 | 每个线程 ~8MB 栈，10000 线程 = 80GB；事件驱动只需 N 个线程 |
| 上下文切换 | 活跃连接数远小于总连接数，轮询只为就绪 fd，CPU 效率高 |
| 非阻塞 | 一次 read 只返回已有数据，不会阻塞等待 |
| 一致性 | 单线程处理，无锁编程，避免竞态条件（Node.js 模型） |

---

## 7. 静态资源服务与中间件系统

### 7.1 静态资源服务

```python
import os
import mimetypes

class StaticFileHandler:
    def __init__(self, root_dir):
        self.root_dir = root_dir

    def handle(self, request):
        # 防止路径遍历攻击
        safe_path = os.path.normpath(
            os.path.join(self.root_dir, request.path.lstrip("/"))
        )
        if not safe_path.startswith(os.path.normpath(self.root_dir)):
            return HTTPResponse(403, b"Forbidden")

        if not os.path.isfile(safe_path):
            return HTTPResponse(404, b"Not Found")

        with open(safe_path, "rb") as f:
            body = f.read()

        content_type, _ = mimetypes.guess_type(safe_path)
        return HTTPResponse(200, body, content_type=content_type or "application/octet-stream")

# 注册
router.get("/static/*")(StaticFileHandler("./public").handle)
```

**安全要点：** 必须做路径规范化检查，防止 `../../../etc/passwd` 攻击。

### 7.2 中间件系统

##### 7.2.1 一句话

**中间件 = 处理请求/响应的责任链。**

```
请求 → Middleware1 → Middleware2 → Middleware3 → Handler → Response
         ↑              ↑              ↑           ↓
         └──────────────┴──────────────┴───────────┘
```

##### 7.2.2 谁调用了中间件——从头到尾的完整链路

从主循环的 `accept()` 到中间件，完整调用链：

```
服务器主循环                             ← while True: conn = accept()
  │
  ├── parser.feed(data)
  ├── parser.parse() → req              ← 解析 HTTP 请求
  │
  └── handle_request(req)               ← 处理请求 (下面展开)
        │
        ├── router.match() → handler    ← 路由匹配
        │
        └── chain.run(req, handler)     ← 进入中间件链
              │
              ├── Middleware1(req, next)
              ├── Middleware2(req, next)
              ├── Middleware3(req, next)
              └── handler(req)          ← 最终的业务逻辑
```

在代码里对应：

```python
# ========== 1. 主循环 (在 server 代码里) ==========
while True:
    conn, addr = server.accept()
    parser = HTTPRequestParser()

    while keep_alive:
        data = conn.recv(4096)
        if not data:
            break
        parser.feed(data)

        while True:
            req = parser.parse()          # 解析出 HTTP 请求
            if req is None:
                break

            response = handle_request(req)  # ← 调用处理函数
            conn.send(response.to_bytes())

    conn.close()

# ========== 2. handle_request (调度路由 + 中间件) ==========
def handle_request(req):
    handler, params = router.match(req.method, req.path)  # 找 handler
    req.params = params
    if handler is None:
        return HTTPResponse(404, b"Not Found")

    # 不用中间件时: return handler(req)
    # 用中间件时:
    return chain.run(req, handler)        # ← 进入中间件链

# ========== 3. chain.run (执行中间件链) ==========
class MiddlewareChain:
    def run(self, request, handler):
        def call_next(i, req):
            if i >= len(self.middlewares):
                return handler(req)        # ← 最后执行真正的 handler
            return self.middlewares[i](req, lambda r: call_next(i + 1, r))
        return call_next(0, request)

# ========== 4. 每个中间件可以做自己的事 ==========
def logging_middleware(request, next_middleware):
    print(f"[{datetime.now()}] {request.method} {request.path}")
    return next_middleware(request)         # 继续往后传
```

**完整的数据流方向：**

```
                         主循环
                           │
                    ┌──────▼──────┐
                    │  parse()    │  ← 从 TCP 字节流解析出 HTTP 请求对象
                    └──────┬──────┘
                           │ req
                    ┌──────▼──────┐
                    │handle_request│  ← 你写的业务入口函数
                    │   │         │
                    │   ├─ match()→ 找 handler
                    │   │
                    │   └─ chain.run()
                    │        │
                    │   ┌────▼────────┐
                    │   │ auth_middle │  ← 检查 token, 不合法直接返回 401
                    │   └────┬────────┘
                    │        │ next(req)
                    │   ┌────▼────────┐
                    │   │  log_middle │  ← 打印日志
                    │   └────┬────────┘
                    │        │ next(req)
                    │   ┌────▼────────┐
                    │   │ cors_middle │  ← 添加跨域头
                    │   └────┬────────┘
                    │        │ next(req)
                    │   ┌────▼────────┐
                    │   │  handler()  │  ← 你的业务代码
                    │   └────┬────────┘
                    │        │ response (逐层返回)
                    └──────┬──────┘
                           │ response bytes
                    ┌──────▼──────┐
                    │  conn.send() │  ← 写回网卡
                    └─────────────┘
```

**所以谁调了中间件？** 主循环调 `handle_request`，`handle_request` 调 `chain.run`，`chain.run` 递归调每个注册的中间件，最后一个中间件调真正的 `handler(req)`。

##### 7.2.3 执行流程

```
handle_request(req)
  │
  ├── router.match() → 找到 handler
  │
  └── chain.run(req, handler)
        │
        ├── Middleware1(req, next):
        │     ├── 前置逻辑 (日志/鉴权)
        │     ├── next(req) → Middleware2(req, next):
        │     │               ├── 前置逻辑
        │     │               ├── next(req) → Middleware3(req, next):
        │     │               │               ├── 前置逻辑
        │     │               │               ├── next(req) → handler(req)
        │     │               │               │               └── 返回 response
        │     │               │               ├── 后置逻辑 (改 headers)
        │     │               │               └── 返回 response
        │     │               └── 返回 response
        │     └── 返回 response
        │
        └── 最终 response 返回给 conn.send()
```

每个中间件都可以选择：
- 在 `next(req)` 之前执行逻辑（请求进入）
- 不调 `next(req)` 直接返回响应（短路，如鉴权失败返回 401）
- 在 `next(req)` 返回之后执行逻辑（响应离开，如添加 CORS header）

##### 7.2.4 代码实现

```python
class MiddlewareChain:
    def __init__(self):
        self.middlewares = []

    def use(self, middleware):
        self.middlewares.append(middleware)

    def run(self, request, handler):
        def call_next(i, req):
            if i >= len(self.middlewares):
                return handler(req)
            return self.middlewares[i](req, lambda r: call_next(i + 1, r))
        return call_next(0, request)

# 中间件示例
def logging_middleware(request, next_middleware):
    print(f"[{datetime.now()}] {request.method} {request.path}")
    return next_middleware(request)

def cors_middleware(request, next_middleware):
    response = next_middleware(request)
    response.headers["Access-Control-Allow-Origin"] = "*"
    return response

def auth_middleware(request, next_middleware):
    token = request.headers.get("Authorization", "")
    if not token.startswith("Bearer "):
        return HTTPResponse(401, b"Unauthorized")
    request.user = verify_token(token[7:])
    return next_middleware(request)
```

---

## 8. 日志系统

### 8.1 一句话本质

**日志系统 = 带级别过滤 + 格式化 + 写入目标 (stdout/文件) 的 Writer。**

### 8.2 实现

```python
import datetime

class Logger:
    DEBUG, INFO, WARN, ERROR = 0, 1, 2, 3

    def __init__(self, level=INFO, output=None):
        self.level = level
        self.output = output  # 文件对象, None 表示 stdout

    def _log(self, level, msg):
        if level < self.level:
            return
        timestamp = datetime.datetime.now().isoformat()
        levels = ["DEBUG", "INFO", "WARN", "ERROR"]
        line = f"[{timestamp}] [{levels[level]}] {msg}\n"
        if self.output:
            self.output.write(line)
            self.output.flush()
        else:
            print(line, end="")

    def debug(self, msg): self._log(self.DEBUG, msg)
    def info(self, msg):  self._log(self.INFO, msg)
    def warn(self, msg):  self._log(self.WARN, msg)
    def error(self, msg): self._log(self.ERROR, msg)

# 集成到请求中
logger = Logger(level=Logger.INFO)

def logging_middleware(request, next_):
    start = datetime.datetime.now()
    response = next_(request)
    duration = (datetime.datetime.now() - start).total_seconds() * 1000
    logger.info(f"{request.method} {request.path} {response.status_code} {duration:.0f}ms")
    return response
```

---

## 9. 完整架构整合

### 9.1 内部结构设计

```
┌────────────────────────────────────────────────────────────┐
│                    WebServer                                │
│                                                             │
│  ┌───────────────────────────────────────────────────┐     │
│  │              Server (main loop)                    │     │
│  │  accept → dispatch → parse → route → handle → send│     │
│  └──────────┬────────────────────────────────────────┘     │
│             │                                               │
│  ┌──────────▼────────────────────────────────────────┐     │
│  │          Connection Handler                        │     │
│  │  - socket accept/reuse                            │     │
│  │  - read/write 管理                                │     │
│  └───────────────────────────────────────────────────┘     │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │Request   │  │ Router   │  │Middleware│  │Response  │  │
│  │Parser    │  │          │  │Chain     │  │Builder   │  │
│  ├──────────┤  ├──────────┤  ├──────────┤  ├──────────┤  │
│  │状态机解析│  │路由表匹配│  │洋葱模型  │  │编码为字节│  │
│  │粘包处理  │  │参数提取  │  │拦截/增强 │  │自动CL/CHK│  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────┐     │
│  │            Static File Handler                     │     │
│  │  MIME 类型, 路径安全, Range 支持 (可选)            │     │
│  └───────────────────────────────────────────────────┘     │
│                                                             │
│  ┌───────────────────────────────────────────────────┐     │
│  │              Logger                                │     │
│  │  级别过滤 / 格式化 / 多输出目标                    │     │
│  └───────────────────────────────────────────────────┘     │
└────────────────────────────────────────────────────────────┘
```

### 9.2 完整 300+ 行 Web Server

```python
import socket
import selectors
import datetime
import os
import mimetypes

# ────────── HTTP 数据结构 ──────────

class HTTPRequest:
    def __init__(self):
        self.method = ""
        self.path = ""
        self.version = ""
        self.headers = {}
        self.body = b""
        self.params = {}

class HTTPResponse:
    def __init__(self, status_code, body=b"", headers=None, content_type="text/plain"):
        self.status_code = status_code
        self.body = body if isinstance(body, bytes) else body.encode()  # 统一转 bytes, 调用方传 str 或 bytes 都行
        self.headers = headers or {}
        self.content_type = content_type

    def to_bytes(self):
        status_map = {
            200: b"OK", 201: b"Created", 301: b"Moved Permanently",
            400: b"Bad Request", 403: b"Forbidden", 404: b"Not Found",
            405: b"Method Not Allowed", 500: b"Internal Server Error",
        }
        reason = status_map.get(self.status_code, b"Unknown")
        lines = [f"HTTP/1.1 {self.status_code} {reason.decode()}\r\n".encode()]
        self.headers.setdefault("Content-Type", self.content_type)
        self.headers["Content-Length"] = str(len(self.body))
        for k, v in self.headers.items():
            lines.append(f"{k}: {v}\r\n".encode())
        lines.append(b"\r\n")
        lines.append(self.body)
        return b"".join(lines)

# ────────── HTTP 请求解析器 ──────────

class HTTPRequestParser:
    def __init__(self):
        self.buffer = b""
        self.state = "REQUEST_LINE"
        self.request = None

    def feed(self, data):
        self.buffer += data

    def parse(self):
        if self.state == "REQUEST_LINE":
            idx = self.buffer.find(b"\r\n")
            if idx == -1:
                return None
            line = self.buffer[:idx]
            self.buffer = self.buffer[idx+2:]
            parts = line.split(b" ")
            if len(parts) < 3:
                return None
            self.request = HTTPRequest()
            self.request.method = parts[0].decode()
            self.request.path = parts[1].decode()
            self.request.version = parts[2].decode()
            self.state = "HEADERS"

        if self.state == "HEADERS":
            while True:
                idx = self.buffer.find(b"\r\n")
                if idx == 0:
                    self.buffer = self.buffer[2:]
                    self.state = "BODY"
                    break
                if idx == -1:
                    return None
                hline = self.buffer[:idx]
                self.buffer = self.buffer[idx+2:]
                colon = hline.find(b":")
                if colon != -1:
                    k = hline[:colon].decode().strip()
                    v = hline[colon+1:].decode().strip()
                    self.request.headers[k] = v

        if self.state == "BODY":
            cl = int(self.request.headers.get("Content-Length", 0))
            if len(self.buffer) >= cl:
                self.request.body = self.buffer[:cl]
                self.buffer = self.buffer[cl:]
                self.state = "DONE"
            else:
                return None

        if self.state == "DONE":
            req = self.request
            self.__init__()
            return req
        return None

# ────────── 路由器 ──────────

class Router:
    def __init__(self):
        self.routes = {}

    def add(self, method, path, handler):
        self.routes.setdefault(method, [])
        segments = path.strip("/").split("/")
        is_dynamic = ":" in path
        self.routes[method].append((segments, handler, is_dynamic, path))

    def get(self, path):
        return lambda h: self.add("GET", path, h)

    def post(self, path):
        return lambda h: self.add("POST", path, h)

    def match(self, method, path):
        if method not in self.routes:
            return None, {}
        ps = path.strip("/").split("/")
        for segments, handler, is_dynamic, _ in self.routes[method]:
            if len(segments) != len(ps):
                continue
            params = {}
            for pat, actual in zip(segments, ps):
                if pat.startswith(":"):
                    params[pat[1:]] = actual
                elif pat != actual:
                    break
            else:
                return handler, params
        return None, {}

# ────────── 中间件链 ──────────

class MiddlewareChain:
    def __init__(self):
        self.middlewares = []

    def use(self, mw):
        self.middlewares.append(mw)

    def run(self, request, handler):
        def call_next(i, req):
            if i >= len(self.middlewares):
                return handler(req)
            return self.middlewares[i](req, lambda r: call_next(i + 1, r))
        return call_next(0, request)

# ────────── 日志 ──────────

class Logger:
    DEBUG, INFO, WARN, ERROR = 0, 1, 2, 3
    _NAMES = ["DEBUG", "INFO", "WARN", "ERROR"]

    def __init__(self, level=INFO):
        self.level = level

    def _log(self, level, msg):
        if level < self.level:
            return
        ts = datetime.datetime.now().isoformat()
        print(f"[{ts}] [{self._NAMES[level]}] {msg}")

    def info(self, msg):  self._log(self.INFO, msg)
    def error(self, msg): self._log(self.ERROR, msg)

# ────────── 静态文件 ──────────

class StaticHandler:
    def __init__(self, root):
        self.root = os.path.abspath(root)

    def handle(self, req):
        safe = os.path.normpath(os.path.join(self.root, req.path.lstrip("/")))
        if not safe.startswith(self.root):
            return HTTPResponse(403, b"Forbidden")
        if not os.path.isfile(safe):
            return HTTPResponse(404, b"Not Found")
        with open(safe, "rb") as f:
            body = f.read()
        ct, _ = mimetypes.guess_type(safe)
        return HTTPResponse(200, body, content_type=ct or "application/octet-stream")

# ────────── WebServer ──────────

class WebServer:
    def __init__(self, host="0.0.0.0", port=8080):
        self.host = host
        self.port = port
        self.router = Router()
        self.middleware = MiddlewareChain()
        self.logger = Logger()
        self.sel = selectors.DefaultSelector()
        self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server.bind((host, port))
        self.server.listen(128)
        self.server.setblocking(False)
        self._register_routes()

    def _register_routes(self):
        self.router.add("GET", "/static/*", StaticHandler("./public").handle)

    def use(self, mw):
        self.middleware.use(mw)

    def get(self, path):
        return self.router.get(path)

    def post(self, path):
        return self.router.post(path)

    def _handle_accept(self, server):
        conn, addr = server.accept()
        conn.setblocking(False)
        self.logger.info(f"Accept {addr}")
        self.sel.register(conn, selectors.EVENT_READ, self._make_reader(conn))

    def _make_reader(self, conn):
        parser = HTTPRequestParser()
        def reader(conn):
            try:
                data = conn.recv(65536)
            except Exception:
                data = b""
            if not data:
                self.sel.unregister(conn)
                conn.close()
                return
            parser.feed(data)
            while True:
                req = parser.parse()
                if req is None:
                    break
                handler, params = self.router.match(req.method, req.path)
                if handler is None:
                    resp = HTTPResponse(404, b"Not Found")
                else:
                    req.params = params
                    resp = self.middleware.run(req, handler)
                keep = req.headers.get("Connection", "").lower() != "close"
                conn.sendall(resp.to_bytes())
                if not keep or req.version == "HTTP/1.0":
                    self.sel.unregister(conn)
                    conn.close()
                    return
        return reader

    def run(self):
        self.sel.register(self.server, selectors.EVENT_READ, self._handle_accept)
        self.logger.info(f"Listening on {self.host}:{self.port}")
        try:
            while True:
                events = self.sel.select()
                for key, _ in events:
                    callback = key.data
                    callback(key.fileobj)
        except KeyboardInterrupt:
            self.logger.info("Shutting down...")
            self.sel.close()
            self.server.close()

# ────────── 使用示例 ──────────

app = WebServer(port=8080)

@app.get("/")
def home(req):
    return HTTPResponse(200, b"Hello from Mini Web Server!")

@app.get("/hello/:name")
def hello(req):
    body = f"Hello, {req.params['name']}!\n".encode()
    return HTTPResponse(200, body)

@app.post("/echo")
def echo(req):
    return HTTPResponse(200, req.body, content_type="application/octet-stream")

# 中间件
def logging_mw(req, next_):
    app.logger.info(f"{req.method} {req.path}")
    return next_(req)

app.use(logging_mw)

if __name__ == "__main__":
    app.run()
```

**测试：**
```bash
python server.py &
curl http://localhost:8080/
# → Hello from Mini Web Server!
curl http://localhost:8080/hello/world
# → Hello, world!
curl -X POST -d 'test' http://localhost:8080/echo
# → test
```

---

## 10. 企业级设计对比

### 10.1 Nginx 为什么快

| 特性 | 说明 |
|------|------|
| 事件驱动 + 异步非阻塞 | 单 worker 处理数千连接，无线程切换开销 |
| 多进程 + accept_mutex | 避免惊群，每个 worker 独立事件循环 |
| 阶段式模块化 | 请求处理分 11 个阶段，每个模块只做一件事 |
| 零拷贝 (sendfile) | 静态文件直接从内核页缓存发送到网卡，绕过用户空间 |
| 内存池管理 | 减少 malloc/free 开销和内存碎片 |

**Nginx 请求处理 11 个阶段：**
```
NGX_HTTP_POST_READ_PHASE → SERVER_REWRITE → FIND_CONFIG
→ REWRITE → POST_REWRITE → PREACCESS → ACCESS
→ POST_ACCESS → PRECONTENT → CONTENT → LOG
```

### 10.2 Tomcat 为什么适合 Java

| 特性 | 说明 |
|------|------|
| 线程池模型 (BIO/NIO/APR) | 每个请求一个线程执行 Servlet，开发简单 |
| Servlet 容器 | 管理 Java EE 组件的生命周期 |
| JVM 生态 | 天然适合 Java 企业应用，集成 JDBC/JPA/JSF |
| 适用场景 | 同步阻塞的 Java 业务逻辑，计算密集型 |

**对比：** Nginx 适合反向代理和静态文件，Tomcat 适合运行业务逻辑。

### 10.3 Node.js 为什么适合 IO 密集

| 特性 | 说明 |
|------|------|
| 单线程事件循环 | 无锁，无上下文切换，开发简单 |
| 非阻塞 IO | 所有 IO 操作都异步回调或 async/await |
| 适用场景 | 聊天 / 实时推送 / API 网关 / BFF |
| 不适用场景 | CPU 密集计算（会阻塞事件循环） |

**事件循环流程图：**
```
   ┌──────────────┐
   │   timers     │  ← setTimeout / setInterval
   └──────┬───────┘
   ┌──────▼───────┐
   │ pending cb   │  ← 系统回调 (如 TCP 错误)  
   └──────┬───────┘
   ┌──────▼───────┐
   │ idle/prepare │  ← 内部使用
   └──────┬───────┘
   ┌──────▼───────┐
   │   poll       │  ← IO 事件 (epoll_wait)
   └──────┬───────┘
   ┌──────▼───────┐
   │   check      │  ← setImmediate
   └──────┬───────┘
   ┌──────▼───────┐
   │ close cb     │  ← socket.on('close')
   └──────┬───────┘
          ▼  (循环)
```

### 10.4 Netty 为什么用于高性能 RPC

| 特性 | 说明 |
|------|------|
| Reactor 模型 | 主从 Reactor，boss 处理 accept，worker 处理读写 |
| Pipeline 链 | 类似中间件，可插拔编解码/处理 |
| 零拷贝 | 直接内存分配，CompositeByteBuf |
| 内存池 | 预分配 arena，减少 GC 压力 |
| 适用场景 | Dubbo / gRPC / Spark 等高性能 RPC 框架 |

---

## 11. 常见错误与排查

### 11.1 socket 卡住

**症状：** `accept()` 或 `recv()` 永不返回。

**原因：**
- 忘记设置非阻塞模式（`setblocking(False)`）
- 未处理 `BlockingIOError`（在非阻塞 socket 上 recv 无数据会抛异常）
- 未设置 socket 超时（`settimeout()`）

**排查：**
```bash
# 查看端口是否监听
ss -tlnp | grep 8080
# strace 追踪系统调用
strace -p <pid> -e accept,read,write
# 查看 TCP 连接状态
ss -tpn | grep 8080
```

### 11.2 HTTP 解析失败

**症状：** 请求返回 400 / 乱码 / 部分响应。

**原因：**
- 未正确处理 `\r\n`（是 `\r\n` 不是 `\n`）
- 假设一次 recv 收到完整请求（TCP 粘包/拆包问题）
- Content-Length 解析错误（大小写敏感：`Content-Length` 不是 `Content-length`）
- chunked 解析未正确处理 `0\r\n\r\n` 终止符

**调试：**
```python
# 打印原始字节
print(repr(data))
```

### 11.3 连接泄漏

**症状：** 服务器返回 `Too many open files`，`ss` 看到大量 `CLOSE_WAIT` 或 `TIME_WAIT`。

**原因：**
- 没有调用 `conn.close()`
- 异常时没有 `finally` 确保关闭
- 全连接队列满 — `listen(backlog)` 太小

**CLOSE_WAIT 大量出现：** 对端关闭了连接，但你的代码没调用 `close()`。
**TIME_WAIT 大量出现：** 主动关闭连接的一方，正常现象，`SO_REUSEADDR` 可缓解。

**排查：**
```bash
# 查看连接状态统计
ss -tnp | awk '{print $1}' | sort | uniq -c
# 查看文件描述符限制
ulimit -n
# 查看当前打开的文件描述符
ls /proc/<pid>/fd | wc -l
```

### 11.4 并发崩溃

**症状：** 多线程下随机挂掉，数据错乱。

**原因：**
- 共享状态未加锁（如共享的 `self.buffer`）
- 非线程安全的资源（如 `selectors` 不是线程安全的）
- 惊群效应（多个进程同时 accept）

**解决方案：**
- 每个连接独立的状态对象（不要共享 `parser`）
- 使用线程安全的数据结构（`queue.Queue`）
- 多进程使用 `accept_mutex`（Nginx 的做法）

### 11.5 内存泄漏

**症状：** `RES` 持续增长，GC 不回收。

**原因：**
- 请求对象被全局变量引用，无法 GC
- 缓冲区无限增长（未限制 `self.buffer` 大小）
- 连接对象未从 `selectors` 注销

**排查：**
```bash
# 查看内存
ps -o pid,rss,cmd -p <pid>
# Python: 查看引用链
import gc; gc.get_objects()
# 使用 objgraph
import objgraph; objgraph.show_most_common_types()
```

---

## 12. 学习路线

### 12.1 第 1 阶段：Socket 编程（1-2 天）

- 学 `socket() / bind() / listen() / accept() / connect()`
- 理解 TCP 三次握手和四次挥手
- 实现一个 echo server
- **参考资料：** 《UNIX 网络编程》第 1-5 章

### 12.2 第 2 阶段：HTTP 协议解析（1-2 天）

- 学 HTTP/1.1 协议规范 (RFC 7230-7235)
- 理解请求行 / 请求头 / body 结构
- 实现请求解析器（状态机）
- 处理粘包 / 分块传输 / keep-alive
- **参考资料：** RFC 7230, 《HTTP 权威指南》

### 12.3 第 3 阶段：简单 Web Server（1 天）

- 整合 socket + HTTP 解析
- 实现单线程串行处理的 web server
- 支持 GET / POST / 静态文件

### 12.4 第 4 阶段：路由系统（半天）

- 路由表设计
- 动态参数提取
- 中间件链

### 12.5 第 5 阶段：并发模型（2-3 天）

- BIO → 线程池 → NIO → Reactor 逐步演进
- 理解 epoll 原理 (mmap + 红黑树 + 回调链表)
- 实现 mini Reactor
- **参考资料：** 《Netty 权威指南》, Nginx 事件模块源码

### 12.6 第 6 阶段：Reactor 模型（1-2 天）

- 主从 Reactor 设计
- Netty 源码结构 (`EventLoopGroup` / `ChannelPipeline` / `ByteBuf`)
- **参考资料：** Netty 源码 (git clone)

### 12.7 第 7 阶段：高性能优化（1 天）

- 零拷贝 (sendfile / splice)
- Buffer 复用
- 连接池
- 内存池

### 12.8 第 8 阶段：阅读源码（持续）

- **Nginx：** `src/event/ngx_epoll_module.c` — 事件循环
- **Netty：** `NioEventLoop.java` — Reactor 线程
- **Tomcat：** `NioEndpoint.java` — 线程池 + NIO

---

## 13. 最终目标检查清单

| 目标 | 达成标准 |
|------|---------|
| 自己写 mini Web Server | ≥300 行，完整运行（上文已实现 ~350 行） |
| 理解 Nginx 为什么快 | 事件驱动 + 多进程 + 零拷贝 + 内存池 |
| 解释 HTTP 请求全过程 | DNS → TCP → accept → 解析 → 路由 → handler → 响应 → 关闭 |
| 画完整请求链路图 | 见上文流程图 |
| 看懂 Netty / Nginx 基础源码 | 知道 `epoll_wait` / `ChannelPipeline` / `EventLoop` 的作用 |

---

> **路标：** 你已经从 Socket 系统调用走到了完整的 Event-Driven Web Server。剩下的路——看 Nginx 的 `ngx_epoll_module.c`、Netty 的 `NioEventLoop.java`——只是把这里学到的东西，用 C 和 Java 再看一遍。
>
> Web Server 的本质就是一个 while 循环——现在你知道了它里面跑的是什么。
