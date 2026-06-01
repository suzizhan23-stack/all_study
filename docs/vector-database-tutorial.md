# 向量数据库完整教程：从原理到实现

> 作者视角：向量数据库内核架构师 + AI检索系统专家 + RAG架构师 + 分布式数据库专家 + 搜索引擎专家
>
> 读者背景：Java 开发工程师（熟悉 Spring Boot / MySQL / Redis / Kafka / Elasticsearch）

---

## 目录

- [第一部分：向量数据库发展历史](#第一部分向量数据库发展历史)
- [第二部分：向量数据库本质](#第二部分向量数据库本质)
- [第三部分：向量数据库存储原理](#第三部分向量数据库存储原理)
- [第四部分：向量索引原理](#第四部分向量索引原理)
- [第五部分：相似度计算](#第五部分相似度计算)
- [第六部分：向量数据库所有功能](#第六部分向量数据库所有功能)
- [第七部分：向量数据库架构](#第七部分向量数据库架构)
- [第八部分：自己实现向量数据库](#第八部分自己实现向量数据库)
- [第九部分：开源项目分析](#第九部分开源项目分析)
- [第十部分：向量数据库实际应用](#第十部分向量数据库实际应用)
- [第十一部分：优点与局限](#第十一部分优点与局限)

---

# 第一部分：向量数据库发展历史

## 第一阶段：关系数据库时代（1970s - 2000s）

### 一句话本质
关系数据库是为**精确匹配**和**结构化数据事务**设计的存储系统，不是为**语义相似性搜索**设计的。

### 代表系统
- **MySQL** — 最流行的开源关系数据库
- **PostgreSQL** — 功能最丰富的关系数据库
- **Oracle** — 企业级商业数据库

### 为什么适合传统场景

```
MySQL 表结构示例：

CREATE TABLE documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200),
    content TEXT,
    author VARCHAR(50),
    created_at TIMESTAMP
);

-- 精确查询 - MySQL 擅长
SELECT * FROM documents WHERE author = 'Tom';

-- 模糊匹配 - MySQL 勉强可以
SELECT * FROM documents WHERE content LIKE '%人工智能%';
```

**MySQL 擅长的事：**
- 精确等值查询：`WHERE name='Tom'` → B+Tree 索引 → O(log n)
- 范围查询：`WHERE age > 18` → B+Tree 索引 → O(log n + k)
- 事务处理：ACID 保证
- 关系模型：JOIN、外键约束

### 为什么关系数据库不适合 AI 检索

**核心问题：语义鸿沟**

```sql
-- 这个查询 MySQL 能快速执行
SELECT * FROM documents WHERE title = 'Java编程入门';

-- 但这个查询 MySQL 完全无能为力：
-- "找和'人工智能'语义相似的内容"
-- MySQL 无法理解"深度学习" ≈ "神经网络" ≈ "AI" 这种语义关系
```

**根本原因分析：**

| 能力 | MySQL | AI 检索需求 |
|------|-------|------------|
| 精确匹配 | ✅ 极快 | ❌ 需要模糊匹配 |
| 关键词模糊 | ❌ LIKE 全表扫描 | ❌ 需要语义理解 |
| 语义相似度 | ❌ 不支持 | ✅ 核心需求 |
| 向量运算 | ❌ 不支持 | ✅ 核心需求 |
| 高维索引 | ❌ 不支持 | ✅ 核心需求 |

### 为什么 "WHERE content LIKE '%人工智能%'" 不够？

```ascii
    查询："人工智能相关的内容"
                │
        ┌───────┴───────┐
        │               │
    LIKE 匹配      语义匹配
        │               │
    "人工智能"     "深度学习"
    "人工智障"     "神经网络"
    "人工只能"     "ChatGPT"
        │               │
    ❌ 字面匹配    ✅ 语义理解
        │               │
    漏掉大量       覆盖所有
    相关内容       语义相近内容
```

### 与 Redis 对比

```ascii
MySQL:    存的是行 (Row)          → 查询靠 B+Tree
Redis:    存的是键值对 (KV)        → 查询靠 Hash Table
VectorDB: 存的是向量 (Vector)     → 查询靠 ANN 索引
```

### 底层原理：B+Tree 为什么不能用于向量检索

B+Tree 的核心假设：**数据可以比较大小**（有序性）。

```ascii
B+Tree 索引结构：
        [50]
       /    \
    [20]    [80]
   /   \    /   \
 [1,20] [21,50] [51,80] [81,+∞]

比较规则：数值/字符串可比较大小
  10 < 20, "abc" < "abd"

问题：向量 [0.1, 0.2, 0.3] 和 [0.4, 0.5, 0.6]
哪个大？没有意义！向量没有全局大小顺序。
```

**面试题：** 为什么不能用 B+Tree 做向量索引？
> B+Tree 依赖全序关系（total order），而向量空间是部分序（partial order）或无序的。向量之间的核心关系是"距离"而非"大小"，B+Tree 无法高效支持距离度量下的最近邻搜索。

---

## 第二阶段：全文检索时代（2000s - 2010s）

### 一句话本质
全文检索引擎通过**倒排索引**将文本中的关键词映射到文档，实现了比 LIKE 更高效的文本搜索，但仍然停留在**关键词匹配**层面。

### 代表系统
- **Elasticsearch** — 分布式全文搜索引擎
- **Solr** — Apache 开源全文搜索平台
- **Lucene** — 底层全文检索引擎库（ES/Solr 的基础）

### Java 工程师视角

你在 Spring Boot 中用过 `@Query` 注解或 `ElasticsearchRepository`：

```java
// Spring Data Elasticsearch
@Query("{\"match\": {\"content\": \"?0\"}}")
List<Document> searchByContent(String keyword);

// 或者用 RestHighLevelClient
SearchRequest request = new SearchRequest("documents");
request.source().query(QueryBuilders.matchQuery("content", "人工智能"));
```

### 倒排索引原理

```ascii
正排索引 (MySQL 的方式)：
doc1: "Java编程入门教程"
doc2: "Python机器学习实战"
doc3: "Java并发编程艺术"

倒排索引 (ES 的方式)：
"Java"      → [doc1, doc3]
"编程"      → [doc1, doc3]
"入门"      → [doc1]
"教程"      → [doc1]
"Python"    → [doc2]
"机器学习"  → [doc2]
"实战"      → [doc2]
"并发"      → [doc3]
"艺术"      → [doc3]
```

**搜索流程：**
1. 用户搜索 "Java 编程"
2. 分词得到：["Java", "编程"]
3. 查倒排索引：找到 doc1, doc3
4. 按 TF-IDF / BM25 打分排序
5. 返回结果

### 为什么倒排索引 ≠ 语义搜索

```ascii
场景：搜索 "如何学习神经网络"

ES 倒排索引匹配：
"如何"     → ❌ 停用词被过滤
"学习"     → 找到包含"学习"的文档
"神经"     → 找到包含"神经"的文档
"网络"     → 找到包含"网络"的文档

结果：包含"计算机网络"的文档也会被命中！
     包含"神经"的文档（如神经内科）也会被命中！

真正想要的是：
"神经网络" → "深度学习" → "AI" → "机器学习"
这些语义相关的文档因为不包含相同的关键词而被漏掉。
```

### 与 MySQL 对比

```ascii
                MySQL LIKE                Elasticsearch
                ─────────                ─────────────
匹配方式    全表扫描 + 子串匹配      分词 + 倒排索引
性能        O(n) 全表扫描            O(1) 查词典
相关性排序  不支持                    BM25 打分
中文分词    不支持                    IK 分词器
语义理解    ❌                       ❌（仅关键词）

两者的共同局限：都无法理解语义。
```

### 底层原理：TF-IDF 与 BM25

```
TF-IDF 公式：
  score = TF * IDF
  
  TF (词频) = 词在文档中出现次数 / 文档总词数
  IDF (逆文档频率) = log(总文档数 / 包含该词的文档数)

BM25 (ES 默认)：
  score = IDF * (TF * (k1 + 1)) / (TF + k1 * (1 - b + b * |d| / avgdl))
  
  其中 k1=1.2, b=0.75 是调参常数
```

**局限**：这些公式只考虑词频统计，不考虑词义。

### 面试题

**Q：Elasticsearch 的 BM25 和向量数据库的余弦相似度有什么区别？**
> BM25 基于词频统计的稀疏向量（Sparse Vector），每个维度对应一个词，大部分维度为 0。余弦相似度基于语义 Embedding 的稠密向量（Dense Vector），每个维度是学习到的语义特征。BM25 无法处理同义词（"汽车"≠"车辆"），而向量可以。

---

## 第三阶段：Embedding 时代（2013 - 2018）

### 一句话本质
Embedding 技术将文字、图片等非结构化数据映射到**高维语义向量空间**，使得语义相似的实体在空间中距离更近——这是向量数据库存在的基石。

### 为什么文字可以变成向量？

```
核心思想：Distributional Hypothesis
"一个词的含义由其上下文决定"——J.R. Firth, 1957

举例：
"我___了一杯咖啡"   → 喝
"我___了一杯奶茶"   → 喝
"我___了一本书"     → 读/看

通过大量上下文学习，模型发现"喝"和"饮"的上下文相似，
因此它们的向量也相似。
```

### Word2Vec 原理（2013, Google）

```ascii
CBOW (Continuous Bag of Words):
输入：[w(t-2), w(t-1), w(t+1), w(t+2)]
输出：w(t) 的概率分布

Skip-gram:
输入：w(t)
输出：[w(t-2), w(t-1), w(t+1), w(t+2)] 的概率

训练结果：每个词变成一个 N 维向量

              "国王" - "男人" + "女人" ≈ "女王"
              vec(king) - vec(man) + vec(woman) ≈ vec(queen)
```

### 从 Word2Vec 到 BERT 到 Transformer

```ascii
Word2Vec (2013)     → 静态词向量，一词一义
    │                     "苹果" → 同一个向量（水果/公司不分）
    │
GloVe (2014)       → 利用全局共现统计
    │
FastText (2016)    → 子词信息，解决 OOV 问题
    │
ELMo (2018)        → 动态词向量，上下文相关
    │
BERT/Transformer   → 双向上下文，自注意力机制
(2018)                 "苹果很好吃" → [0.1, 0.3, ...]
                       "苹果发布了新手机" → [0.9, 0.1, ...]
```

### Java 工程师视角

你在 Spring Boot 中调用 Embedding API：

```java
// 调用 OpenAI Embedding API
@Service
public class EmbeddingService {

    @Value("${openai.api.key}")
    private String apiKey;

    public float[] getEmbedding(String text) {
        // 实际调用 OpenAI /text-embedding-ada-002
        // 返回 1536 维的 float 数组
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/embeddings"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"input\": \"" + text + "\", \"model\": \"text-embedding-ada-002\"}"
            ))
            .build();
        // ... 解析响应
        return embedding;
    }
}
```

```python
# Python 实现 - 使用 openai 库调用 Embedding API
import os
from openai import OpenAI

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

def get_embedding(text: str, model: str = "text-embedding-ada-002") -> list[float]:
    response = client.embeddings.create(input=[text], model=model)
    return response.data[0].embedding

# 或者使用 sentence-transformers（本地运行，无需 API）
# pip install sentence-transformers
from sentence_transformers import SentenceTransformer

model = SentenceTransformer("all-MiniLM-L6-v2")  # 384 维

def get_embedding_local(text: str) -> list[float]:
    return model.encode(text).tolist()

# 示例
# vec = get_embedding("什么是人工智能?")
# print(len(vec))  # 1536 (ada-002) 或 384 (MiniLM)
```

### 为什么 Embedding 推动向量数据库？

```ascii
传统数据库         Embedding模型            向量数据库
存储文本            ↓ 将文本转为向量         存储向量
                 "ChatGPT是AI助手"
                        │
                        ▼
                 [0.123, -0.456,          →  [0.123, -0.456, 
                  0.789, 0.012, ...]          0.789, 0.012, ...]
                        │
                 "AI助手是什么"                 │
                        │                      ▼
                 [0.130, -0.460,           相似度搜索
                  0.785, 0.015, ...]           │
                                           找到语义最近的文本
```

### 面试题

**Q：为什么说 Embedding 是向量数据库的"上游依赖"？**
> 没有 Embedding 就没有向量。向量数据库的查询质量完全取决于 Embedding 模型的质量 — 好的 Embedding 让相似语义聚在一起，差的 Embedding 让向量失去语义。选 Embedding 模型就像选 MySQL 的存储引擎一样影响全局。

---

## 第四阶段：LLM 时代（2020 - 至今）

### 一句话本质
大语言模型（LLM）爆发出强大的文本理解和生成能力，但存在**知识截止、幻觉、无法访问私有数据**等局限，RAG 架构引入向量数据库作为外部知识存储，解决了这些痛点。

### 为什么 LLM 推动向量数据库爆发？

```
LLM 的三大问题：

1. 知识截止
   GPT-4 的知识截止于 2023 年
   问 "2024 年发生了什么？" → ❌ 不知道

2. 幻觉 (Hallucination)
   问 "公司的离职政策是什么？" 
   → LLM 可能编造一个不存在的政策

3. 无法访问私有数据
   公司的内部文档、代码库、客户信息
   LLM 训练时没有见过

RAG 解决方案：
                     ┌──────────┐
    用户问题 ───────→│  Embedding  │
                     └─────┬────┘
                           │ 向量
                           ▼
                     ┌──────────┐
                     │ 向量数据库 │ ←─── 公司文档 (.docx, .pdf, .md)
                     └─────┬────┘      Wiki、代码库、知识库
                           │ Top-K 相关文档
                           ▼
                     ┌──────────┐
                     │    LLM    │ ←── 问题 + 相关文档
                     └─────┬────┘
                           │ 回答
                           ▼
                     准确、无幻觉的回答
```

### 从传统搜索到 RAG 的演进

```ascii
传统方式：
  用户 → MySQL 精确查询 → 精确结果（要求用户知道怎么搜）

ES 方式：
  用户 → Elasticsearch 关键词搜索 → 相关文档

RAG 方式：
  用户 → Embedding → 向量数据库 → LLM → 精准回答
         ↓自然语言    ↓语义搜索    ↓理解生成
```

### 为什么 RAG 需要向量数据库？

```ascii
RAG 流程中需要的关键能力：

1. 存储海量 Embedding 向量
   100 万份文档 → 100 万条 1536 维向量
   → MySQL 存不了（无法语义检索）
   → Redis 存不了（内存爆炸，无法相似度搜索）
   → ES 勉强可以（dense_vector 类型，但性能差）

2. 毫秒级 Top-K 近似搜索
   从 1000 万向量中找到最相似的 10 个
   → 暴力搜索需要计算 1000 万次距离 → 几百毫秒
   → ANN 索引只需几十次距离计算 → 几毫秒

3. 支持 Metadata 过滤
   "找 2024 年 AI 相关的文档"
   → 向量相似度 + 时间范围过滤 = 混合搜索
```

### 面试题

**Q：为什么 2023-2024 年向量数据库突然火了？是刚需还是炒作？**
> 刚需。LLM 的幻觉问题和知识截止问题是商业落地的致命障碍，RAG 是目前解决这些问题最有效的架构。没有向量数据库，RAG 只能用暴力搜索或 ES，性能和精度都不够。简单说：LLM 负责"说"，向量数据库负责"记"。

---

## 第五阶段：向量数据库时代（2019 - 至今）

### 一句话本质
向量数据库是专门为**向量 Embedding 存储和近似最近邻搜索（ANN）**设计的数据库系统，在 RAG、推荐、搜索等场景中补充而非取代传统数据库。

### 主流产品定位分析

```ascii
产品         定位                  开源/商业      底层语言     适用场景
─────       ────                  ─────────      ──────      ───────
FAISS       向量索引库（非DB）      开源/Facebook   C++         学术、原型
Milvus      分布式向量数据库        开源 LF         Go/C++      大规模生产
Qdrant      轻量向量数据库          开源 Rust       Rust        中等规模
Weaviate    AI原生向量搜索引擎      开源 Go         Go          知识图谱+AI
Chroma      开发者友好向量DB        开源 Python     Python      原型、小规模
Pinecone    托管向量数据库          商业 SaaS      -           企业（不想运维）
pgvector    PostgreSQL 向量扩展     开源           C           已有 PG 的团队
```

### 核心区别

```ascii
              FAISS           Milvus          Qdrant          pgvector
              ─────           ──────          ──────          ───────
是数据库吗？    ❌ 索引库       ✅              ✅              ✅ (PG 插件)
持久化         ❌ 内存          ✅ 磁盘           ✅ 磁盘           ✅ 
分布式         ❌              ✅              ✅ (集群版)      ❌ (PG 自己的)
多语言 SDK     ❌ C++/Python   ✅ Java/Go/...   ✅ Java/Go/...   ✅ SQL
事务           ❌              ❌              ❌              ✅ (PG 事务)
Metadata      ❌              ✅              ✅              ✅ (PG 列)
```

### 从 Java 工程师视角看技术选型

```java
// 你在 Spring Boot 中可以这样接入不同向量数据库：

// 1. Milvus (生产级, 大规模)
// Spring Boot + Milvus SDK
@Service
public class MilvusService {
    private final MilvusClient client;

    public MilvusService() {
        // MilvusClient 基于 Netty, gRPC 通信
        client = new MilvusServiceClient(
            ConnectParam.newBuilder()
                .withHost("localhost")
                .withPort(19530)
                .build()
        );
    }

    public SearchResult search(float[] vector) {
        return client.withSearchBuilder()
            .withCollectionName("documents")
            .withParams(Params.create("nprobe", 10))
            .withVectors(List.of(vector))
            .withTopK(10)
            .search();
    }
}

// 2. Qdrant (中等规模, 低延迟)
// 使用 REST API, 对 Spring Boot 友好
@Service
public class QdrantService {
    private final RestTemplate restTemplate;

    public List<PointStruct> search(float[] vector) {
        // HTTP 接口, 像调普通 REST API 一样
        String url = "http://localhost:6333/collections/docs/points/search";
        SearchRequest request = new SearchRequest(vector, 10);
        return restTemplate.postForObject(url, request, SearchResult.class).getResult();
    }
}
```

```python
# Python 实现 - 使用 pymilvus 连接 Milvus
# pip install pymilvus
from pymilvus import connections, Collection, CollectionSchema, FieldSchema, DataType

# 1. Milvus (生产级, 大规模)
connections.connect(host="localhost", port="19530")

schema = CollectionSchema([
    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True),
    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=1536),
])
collection = Collection(name="documents", schema=schema)

def milvus_search(vector: list[float], top_k: int = 10) -> list:
    search_params = {"metric_type": "COSINE", "params": {"nprobe": 10}}
    results = collection.search(
        data=[vector], anns_field="embedding",
        param=search_params, limit=top_k
    )
    return [(h.id, h.score) for h in results[0]]

# 2. Qdrant (中等规模, 低延迟)
# pip install qdrant-client
from qdrant_client import QdrantClient
from qdrant_client.models import PointStruct, VectorParams, Distance

qdrant = QdrantClient(host="localhost", port=6333)
qdrant.recreate_collection(
    collection_name="docs",
    vectors_config=VectorParams(size=1536, distance=Distance.COSINE),
)

def qdrant_search(vector: list[float], top_k: int = 10) -> list:
    results = qdrant.search(
        collection_name="docs",
        query_vector=vector,
        limit=top_k,
    )
    return [(r.id, r.score) for r in results]
```

### 面试题

**Q：FAISS 和 Milvus 有什么区别？什么时候该用 FAISS 而不是 Milvus？**
> FAISS 是向量索引库（类比：LevelDB 之于 Redis），Milvus 是完整数据库（类比：MySQL）。FAISS 只负责在内存中做 ANN 搜索，不关心数据持久化、分布式、权限等。如果你的数据量小（百万级）、单机够用、不需要持久化，FAISS 直接集成到 Java 应用中更轻量。如果数据量大、需要持久化、需要分布式，用 Milvus。

---

# 第二部分：向量数据库本质

## 什么是向量数据库？

### 一句话本质
向量数据库是**以高维向量为核心数据模型**，以**近似最近邻搜索（ANN）**为核心查询方式的数据库系统。

### 对比理解

```ascii
MySQL:              Redis:              Elasticsearch:       向量数据库:
┌──────────┐       ┌──────────┐        ┌──────────┐         ┌──────────┐
│  Row     │       │  Key     │        │ Document │         │  Vector  │
│  ───     │       │  ───     │        │  ──────── │         │  ──────  │
│ id int   │       │ key: val │        │ title    │         │ id: 1001 │
│ name str │       │          │        │ content  │         │ vec:[...]│
│ age int  │       │ O(1) GET │        │ author   │         │ metadata │
│          │       │ O(1) SET │        │          │         │          │
│ B+Tree   │       │ Hash     │        │ 倒排索引 │         │ ANN 索引 │
│ = 精确查 │       │ = 缓存放 │        │ = 关键词 │         │ = 语义找 │
└──────────┘       └──────────┘        └──────────┘         └──────────┘

数据模型：   行式             KV             文档             向量
核心操作：   SELECT          GET             SEARCH          SEARCH
查询方式：   精确匹配         精确匹配        关键词匹配       语义相似
索引结构：   B+Tree          Hash Table      倒排索引         HNSW/IVF
返回结果：   精确结果         精确值          相关文档         Top-K 最相似
```

### 完整数据链路

```ascii
文本/图片/音视频
      │
      ▼   Embedding 模型
  ┌──────┐    Word2Vec / BERT / OpenAI-ada-002
  │向量化 │    text → float[1536]
  └──┬───┘
      │ 向量 [0.123, -0.456, 0.789, ...]
      ▼
  ┌──────┐    
  │ 存储  │    向量 + metadata + id
  └──┬───┘    ┌─────────────────┐
      │        │ id: 1001        │
      ▼        │ text:"ChatGPT..."│
  ┌──────┐    │ vector:[...]    │
  │ 索引  │    │ metadata:{...}  │
  └──┬───┘    └─────────────────┘
      │ HNSW / IVF / PQ
      ▼
  ┌──────┐    
  │ 搜索  │    查询向量 → ANN → Top-K
  └──────┘
```

### 向量数据库中到底存什么？

```
不是一个单纯的向量列表，而是结构化记录：

{
  "id": "1001",                    // 主键 (像 MySQL PK)
  "vector": [0.123, -0.456, ...],  // Embedding 向量 (核心)
  "metadata": {                     // 元数据 (像 MySQL 其他列)
    "title": "ChatGPT介绍",
    "author": "张三",
    "created_at": "2024-01-15",
    "category": "AI",
    "tags": ["LLM", "GPT", "AI"]
  },
  "text": "ChatGPT is an AI assistant..."  // 原始文本 (可选)
}
```

### 为什么要这样存？

| 字段 | 为什么需要 | 类比 MySQL |
|------|-----------|-----------|
| id | 唯一标识，更新删除的依据 | PRIMARY KEY |
| vector | 语义搜索的核心，ANN 索引的输入 | 无（MySQL 没有这个类型） |
| metadata | 过滤条件，如只搜某分类 | 其他列（WHERE category='AI'） |
| text | 返回给用户的原始内容，或 LLM 的上下文 | SELECT content FROM ... |

### Java 工程师代码理解

```java
// 传统数据库中的一条记录
@Entity
@Table(name = "documents")
public class Document {
    @Id
    private Long id;
    private String title;
    private String content;
    private String author;
    private LocalDateTime createdAt;
}

// 向量数据库中的一条记录
public class VectorRecord {
    private String id;             // 主键
    private float[] vector;        // 向量，1536 维 float 数组
    private Map<String, Object> metadata;  // 元数据，类似 MySQL 的其他列
    private String originalText;   // 原始文本，用于展示或 LLM 上下文

    // 向量没有 setter/getter 的常规操作
    // 核心操作是：计算距离
    public double cosineSimilarity(float[] other) {
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < vector.length; i++) {
            dotProduct += vector[i] * other[i];
            normA += vector[i] * vector[i];
            normB += other[i] * other[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

```python
# Python 实现 - 向量记录数据结构
from dataclasses import dataclass, field
from typing import Any
import numpy as np

@dataclass
class VectorRecord:
    id: str
    vector: np.ndarray           # 高维向量, shape=(dim,)
    metadata: dict[str, Any] = field(default_factory=dict)
    original_text: str = ""

    def cosine_similarity(self, other: "VectorRecord") -> float:
        """计算与另一个向量的余弦相似度"""
        a, b = self.vector, other.vector
        dot = float(np.dot(a, b))
        norm = float(np.linalg.norm(a) * np.linalg.norm(b))
        return dot / norm if norm != 0 else 0.0

# 使用示例
# record = VectorRecord(
#     id="1001",
#     vector=np.random.randn(1536).astype(np.float32),
#     metadata={"title": "ChatGPT介绍", "author": "张三"},
#     original_text="ChatGPT is an AI assistant..."
# )
```

### 面试题

**Q：既然向量数据库存的是 float[]，那它是不是比 MySQL 更简单？**
> 存储结构看似简单（就是一个 float 数组），但核心复杂度在于**索引结构和搜索算法**。MySQL 的 B+Tree 插入是 O(log n) 且精确，向量数据库的 HNSW 插入涉及复杂的图结构维护，搜索是近似而非精确的。另外，向量的维度灾难、内存管理、分布式分片等都比传统数据库更复杂。

---

# 第三部分：向量数据库存储原理

## 向量数据库内部存什么？

### 一句话本质
向量数据库以**向量索引文件 + 元数据存储**的方式组织数据，向量通常以特定二进制格式存储在内存或磁盘上，元数据可以独立存储（SQLite、RocksDB 等）。

### 存储架构总览

```ascii
Collection (数据库/表)
    │
    ├── Segment 0 (数据段)
    │    ├── 向量文件 (.vec)     ← 纯二进制 float 数组
    │    ├── 索引文件 (.idx)     ← HNSW/IVF 索引结构
    │    ├── 元数据存储          ← SQLite / RocksDB
    │    └── 删除标记文件 (.del)   ← 标记删除
    │
    ├── Segment 1
    │    └── ...
    │
    └── Segment N (新数据持续写入)
         └── (正在写入的活跃段)
```

### 向量存储格式详解

```ascii
向量在内存中的布局：

float[] vector = [0.123, -0.456, 0.789, 0.012, ..., 0.555]
                 ↑                                    ↑
                dim 0                               dim 1535

1536 维 float 数组
每个 float = 4 字节
总共 = 1536 × 4 = 6144 字节 ≈ 6 KB

100 万条向量占内存：
    1,000,000 × 6 KB = 6 GB

二进制文件存储：
Offset 0:     [dimension]          (4 bytes)  ← 维度数
Offset 4:     [total_vectors]      (4 bytes)  ← 向量总数
Offset 8:     [vector_0_data]      (6144 bytes) ← 第0个向量
Offset 6152:  [vector_1_data]      (6144 bytes) ← 第1个向量
...
```

### 存储介质选择

```ascii
          内存 (RAM)                   磁盘 (SSD)
          ─────────                   ─────────
速度        纳秒级                      微秒级 (慢1000倍)
容量        有限 (GB级)                  大 (TB级)
成本        贵 (~$10/GB)                便宜 (~$0.1/GB)
持久性      断电丢失                      持久保存

主流策略：
  Milvus:    内存索引 (可配置 mmap 映射到磁盘)
  Qdrant:    内存向量 + 磁盘元数据
  FAISS:     纯内存 (GPU 也支持)
  DiskANN:   磁盘为主 + 内存缓存
```

### Milvus 存储架构

```ascii
Milvus 内部存储结构：

Collection
    │
    ├── Partition (逻辑分区)
    │    │
    │    └── Segment (最小存储单元)
    │         │
    │         ├── InsertLog (向量 + 元数据)
    │         │    ├── vector_log    ← 向量二进制
    │         │    └── metadata_log  ← 元数据 (Parquet)
    │         │
    │         ├── IndexFile (索引文件)
    │         │    └── HNSW/IVF_PQ 索引
    │         │
    │         └── DeltaLog (删除记录)
    │              └── 删除的 ID 列表
    │
    └── MinIO / S3 (对象存储)
         └── Segment 文件持久化
```

### Qdrant 存储架构

```ascii
Qdrant 内部存储结构：

Collection
    │
    └── Shard (分片)
         │
         ├── Segment (内存)
         │    ├── HNSW 图索引 (内存)
         │    ├── Vector 存储 (内存 mmap)
         │    └── Payload 索引 (RocksDB)
         │
         └── Segment (磁盘)
              └── (被合并到磁盘的旧段)
```

### 页结构（类似 MySQL 的 Page）

```ascii
向量数据库没有像 MySQL 那样统一的 Page 结构，
因为向量不适合按页组织（向量间没有顺序关系）。

MySQL Page:
┌─────────────────────────┐
│ Page Header (38 bytes)  │
├─────────────────────────┤
│ User Records            │
│   Row1, Row2, ...       │ ← 按主键顺序排列
├─────────────────────────┤
│ Free Space              │
├─────────────────────────┤
│ Page Directory          │ ← 稀疏目录
└─────────────────────────┘

向量存储（以 Qdrant 为例）：
┌─────────────────────────┐
│ 向量 ID 映射表           │
│   id → offset           │ ← HashMap, O(1) 定位
├─────────────────────────┤
│ 向量原始数据 (连续存储)   │
│   [vec0] [vec1] [vec2]  │ ← float[] 连续排列
├─────────────────────────┤
│ HNSW 图邻接表            │
│   node0 → [2, 5, 8]     │ ← 图结构，非连续访问
│   node1 → [3, 7]        │
└─────────────────────────┘
```

### Java 视角的向量存储实现

```java
// 极简向量存储实现
public class VectorStore {
    // 内存存储：连续数组
    private final List<float[]> vectors = new ArrayList<>();
    private final Map<String, Integer> idToIndex = new HashMap<>();
    private final Map<String, Map<String, Object>> metadataStore = new HashMap<>();

    // 存储向量
    public void insert(String id, float[] vector, Map<String, Object> metadata) {
        int index = vectors.size();
        vectors.add(vector);                    // 追加到末尾
        idToIndex.put(id, index);               // ID 到索引的映射
        metadataStore.put(id, metadata);        // 元数据单独存储
    }

    // 按 ID 获取向量
    public float[] getVector(String id) {
        Integer index = idToIndex.get(id);
        if (index == null) return null;
        return vectors.get(index);
    }

    // 全部向量数量
    public int size() {
        return vectors.size();
    }
}
```

```python
# Python 实现 - 极简向量存储
from typing import Any
import numpy as np

class VectorStore:
    def __init__(self):
        self.vectors: list[np.ndarray] = []           # 连续存储的向量
        self.id_to_index: dict[str, int] = {}         # ID → 索引映射
        self.metadata_store: dict[str, dict] = {}     # 元数据单独存储

    def insert(self, id: str, vector: np.ndarray, metadata: dict[str, Any] | None = None):
        index = len(self.vectors)
        self.vectors.append(vector)
        self.id_to_index[id] = index
        self.metadata_store[id] = metadata or {}

    def get_vector(self, id: str) -> np.ndarray | None:
        index = self.id_to_index.get(id)
        return self.vectors[index] if index is not None else None

    def __len__(self) -> int:
        return len(self.vectors)

# store = VectorStore()
# store.insert("1", np.random.randn(768).astype(np.float32), {"tag": "AI"})
```

### 面试题

**Q：为什么向量数据库不用 B+Tree 存储向量？为什么不把向量和元数据存在一起？**
> 1) B+Tree 需要有序性，向量在空间上无序，强行排序会破坏空间局部性。2) 向量和元数据分开存储是因为访问模式不同：向量用于距离计算（需要连续内存访问获得 CPU 缓存友好），元数据用于过滤（需要随机访问能力）。混合存储会让缓存命中率下降。

**Q：100 万条 1536 维向量需要多少内存？如何优化？**
> 100万 × 1536 × 4字节 = 6GB。优化方式：1) PQ 量化压缩到每个向量几十字节；2) 用 int8 替代 float32（精度损失小）；3) 只把索引放内存，向量放 SSD（DiskANN 方案）。

---

# 第四部分：向量索引原理（最重要）

## 为什么不能暴力搜索？

```ascii
暴力搜索 (Flat / Brute Force)：

查询向量 q 与 N 个向量逐一计算距离：

for i = 0 to N-1:
    dist[i] = cosineSimilarity(q, vectors[i])

时间复杂度：O(N × d)
  N = 向量数量
  d = 向量维度

数据量      每次计算     总时间
───────    ────────    ──────
1 万        6 KB        10 ms     ✅ 可接受
100 万      6 GB        1000 ms   ❌ 太慢
1000 万     60 GB       10000 ms  ❌ 不可接受
1 亿        600 GB      -         ❌ 不可能
```

### 为什么 B+Tree 不能用？

```ascii
B+Tree 的核心前提：数据存在全序关系 (Total Order)

MySQL 主键：1 < 2 < 3 < ... ✓
字符串：   "a" < "ab" < "b" ✓

但是向量：
  [0.1, 0.2, 0.3] 和 [0.4, 0.5, 0.6]
  没有大小关系！只有距离关系！

  B+Tree 搜索："比这个大的" → 向量没有"大"的概念
  
  ANN 搜索：  "离这个最近的" → 距离度量
```

## Flat Index (暴力搜索)

### 一句话本质
Flat 是最简单的索引方式，不做任何优化，查询时遍历所有向量计算距离，适合**数据量小（万级以下）**的场景。

### 时间复杂度
- 插入：O(1)（直接追加）
- 搜索：O(N × d)（N 个向量，d 维度）
- 内存：O(N × d)

### 优缺点

| 优点 | 缺点 |
|------|------|
| 100% 召回率（精确） | 速度慢，大数据量不可用 |
| 实现最简单 | 内存消耗大 |
| 适合小数据量 | 无法利用磁盘 |

### Java 实现

```java
public class FlatIndex {
    private final List<float[]> vectors = new ArrayList<>();
    private final List<String> ids = new ArrayList<>();

    public void insert(String id, float[] vector) {
        ids.add(id);
        vectors.add(vector);
    }

    public List<SearchResult> search(float[] query, int topK) {
        // 创建一个优先队列，容量 topK
        PriorityQueue<SearchResult> pq = new PriorityQueue<>(
            (a, b) -> Double.compare(a.score, b.score) // 最小堆
        );

        for (int i = 0; i < vectors.size(); i++) {
            double dist = cosineSimilarity(query, vectors.get(i));
            pq.offer(new SearchResult(ids.get(i), i, dist));
            if (pq.size() > topK) {
                pq.poll(); // 移除最小的（距离最大的）
            }
        }

        // 转换成降序列表
        List<SearchResult> results = new ArrayList<>(pq);
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results;
    }
}
```

```python
# Python 实现 - Flat Index (暴力搜索) 使用 numpy
import numpy as np
import heapq
from dataclasses import dataclass

@dataclass
class SearchResult:
    id: str
    score: float

class FlatIndex:
    def __init__(self):
        self.vectors: list[np.ndarray] = []
        self.ids: list[str] = []

    def insert(self, id: str, vector: np.ndarray):
        self.ids.append(id)
        self.vectors.append(vector)

    def search(self, query: np.ndarray, top_k: int = 10) -> list[SearchResult]:
        # 向量化计算余弦相似度
        query = query / np.linalg.norm(query)
        scores = []
        for vec in self.vectors:
            vec = vec / np.linalg.norm(vec)
            scores.append(float(np.dot(query, vec)))
        # 获取 Top-K 索引
        top_indices = heapq.nlargest(top_k, range(len(scores)), key=lambda i: scores[i])
        return [SearchResult(id=self.ids[i], score=scores[i]) for i in top_indices]

# index = FlatIndex()
# index.insert("doc1", np.random.randn(384))
# results = index.search(np.random.randn(384), top_k=5)
```

## IVF (Inverted File Index)

### 一句话本质
IVF 通过 **K-Means 聚类**将向量空间划分为多个区域，搜索时只搜索离查询最近的几个区域，以**牺牲少量精度换取大幅速度提升**。

### 为什么出现？

```
暴搜 100 万向量 → 100 万次计算 → 太慢

IVF 思路：
1. 训练阶段：把所有向量聚成 K 个簇 (如 K=1000)
2. 每个向量归入最近的簇
3. 搜索阶段：找到查询向量最近的 N 个簇 (如 nprobe=10)
4. 只在这 N 个簇内搜索

速度提升：100万 / (1000/10 × 1000/1000) = 100 倍
        ↓                ↓
    暴搜 100 万    只搜 1 万 (10个簇 × 平均1000个/簇)
```

### K-Means 聚类流程

```ascii
Step 1: 随机选 K 个中心点
           ·           ·           ·
             ·       ·     ·     ·
           ·     ·       ·         ·
         ·         ·           ·
           ·     ·     ·   ·

Step 2: 每个向量归入最近的中心
           ○           △           □
           ○ ○       △ △ △     □ □
           ○ ○ ○     △ △ △ △   □ □ □
           ○ ○       △ △       □ □

Step 3: 更新中心点 (取簇的平均)
           ○           △           □
           ○ ○       △ △ △     □ □
           ○ ○ ○  →  △ △ △ △   □ □ □
           ○ ○       △ △       □ □

Step 4: 重复 Step 2-3 直到收敛
```

### 查询流程

```ascii
输入：查询向量 q, nprobe=2

       ┌─────────────────────────────────────┐
       │  所有簇中心 (K=1000)                 │
       │   ○  ○  ○  ○  ○  ○  ○  ○  ○  ○    │
       │   ○  ○  ○  ○  ○  ○  ○  ○  ○  ○    │
       │   ○  ○  ○  ●  ○  ○  ○  ○  ○  ○    │ ← q 的位置
       │   ○  ○  ○  ○  ○  ○  ○  ○  ○  ○    │
       └─────────────────────────────────────┘

Step 1: 计算 q 到所有簇中心的距离 → 找到最近的 2 个簇
        最近簇: C1 (距离 0.1), C2 (距离 0.3)

Step 2: 只搜索 C1 和 C2 中的向量
        C1 中有 800 个向量，C2 中有 1200 个向量
        总计只搜索 2000 个向量

Step 3: 返回 Top-K 结果

对比：暴搜 100 万 vs IVF 搜 2000 → 500 倍加速
```

### IVF 的局限

```ascii
问题：簇边界附近的向量可能被遗漏

真实最近邻可能不在最近的 N 个簇中：
                
             簇 A                    簇 B
        ┌──────────────┐      ┌──────────────┐
        │   ·    ·     │      │  ·      ·    │
        │     ·  q     │      │    ·  ★    · │
        │   ·    ·     │      │  ·      ·    │
        └──────────────┘      └──────────────┘
        
q 在簇 A 中，但真正的最近邻 ★ 在簇 B 中
如果 nprobe 不够大，★ 会被遗漏

解决：增大 nprobe（但会降低速度）
     使用更高级的索引（HNSW）
```

## HNSW（核心重点）

### 一句话本质
HNSW（Hierarchical Navigable Small World）是一种**基于分层图的近似最近邻搜索算法**，通过构建多层图结构实现 O(log N) 的搜索复杂度，是目前**最流行的向量索引算法**。

### 为什么 HNSW 快？

```
核心洞察：Skip List 的思想 + 小世界网络

Skip List (跳跃表)：
  Level 3: 1 ────────────────────→ 9
  Level 2: 1 ───────→ 5 ────────→ 9
  Level 1: 1 ─→ 3 ─→ 5 ─→ 7 ─→ 9
  
  高层：大步跨越（快速定位区域）
  低层：精细搜索（找到精确位置）

HNSW 同样原理，只不过节点间的关系是"距离"而非"大小"：
  Level 2: [入口] ─→ [粗搜区域]
  Level 1: [粗搜区域] → [精细搜索]
  Level 0: [精细搜索] → [精确结果]
```

### 图结构

```ascii
HNSW 多层图示意：

Layer 2 (最高层，最稀疏):
    A ──────────────── C
     \               /
      └───── B ─────┘

Layer 1 (中间层):
    A ─── D ─── C
     \   / \   /
      E ─── F ─── G
       \ /     \
        H ───── I

Layer 0 (最底层，最稠密):
    A ─ D ─ C ─ K ─ M
    │   │   │   │   │
    E ─ F ─ G ─ L ─ N
    │   │   │   │   │
    H ─ I ─ J ─ O ─ P
    
    每条边代表：两个节点互为最近邻
```

### Node 结构

```c
// FAISS HNSW 源码中的节点结构 (简化)
struct Node {
    int id;                    // 节点 ID
    int level;                 // 所在层数 (0 为最底层)
    vector<vector<int>> neighbors;  // 每层的邻居列表
    // neighbors[0] = level 0 的邻居
    // neighbors[1] = level 1 的邻居
    // neighbors[2] = level 2 的邻居
    float* vector;             // 向量数据
};
```

### 搜索过程

```ascii
输入：查询向量 q, efSearch=50 (搜索宽度), topK=10

搜索流程（图解）：

Layer 2:
  enter_point = A (随机入口)
  
  Step 1: 从 A 开始，检查所有邻居 {C}
  距离(A, q) = 0.8, 距离(C, q) = 0.5
  C 更近 → 移动到 C

  Step 2: 从 C 开始，检查邻居 {A, B}
  距离(A, q) = 0.8, 距离(B, q) = 0.6
  都比 C (0.5) 远 → 停在 C

Layer 1:
  Step 3: 从 C 进入 Layer 1，检查邻居 {D, F, G}
  找到距离最近的，继续往下

Layer 0:
  Step 4: 逐步搜索，维护一个大小为 efSearch=50 的候选队列
  最终返回 topK=10 个最相似结果

搜索算法伪代码：
```
function search(q, efSearch, topK):
    # visited: 已访问集合 (防止重复)
    # candidates: 当前候选队列 (最小堆, 按距离排序)
    # results: 最终结果列表 (最大堆, 按距离排序)
    
    enter_point = get_entry_point()
    candidates.add(enter_point)
    results.add(enter_point)
    
    while candidates is not empty:
        c = candidates.pop()  # 最近的候选
        f = results.peek()    # 结果中最远的
        
        if distance(c, q) > distance(f, q):
            break  # 收敛条件
        
        for neighbor in c.neighbors[current_level]:
            if neighbor not in visited:
                visited.add(neighbor)
                if distance(neighbor, q) < distance(f, q) or results.size() < efSearch:
                    candidates.add(neighbor)
                    results.add(neighbor)
                    if results.size() > efSearch:
                        results.pop()  # 移除最远的
    
    return results.top(topK)  # 返回最近的 topK 个
```

### 插入过程

```ascii
插入新节点 new_node：

输入：new_node 的向量, M (邻居数), M_max (最大邻居数)

Step 1: 确定 new_node 的层数
  level = floor(-ln(random()) * mL)
  其中 mL = 1/ln(M)  (通常约 0.5-1.0)
  大多数节点在低层 (Level 0), 少数在高层

Step 2: 从最高层到 level+1 层搜索
  找到每层的最近节点作为入口

Step 3: 在 level 到 0 层:
  a. 搜索 efConstruction 个最近邻居
  b. 从最近邻居中选 M 个作为新节点的邻居
  c. 连接新节点和这些邻居
  d. 对每个邻居，检查是否超过 M_max 
     如果超过，剪枝 (保留最近的 M_max 个)

图示插入：
  插入前:    A ─── B ─── C
  
  插入 new_node X (Level 0):
  
  Step a: 找到最近的 M 个邻居
          A (0.1), B (0.2)
  
  Step b: 建立双向连接
          A ─── B
           │    │
           X ───┘
  
  Step c: 检查 A 的邻居数
          原来有 {B}, 现在多了 X → {B, X}
          不超过 M_max → 保留
```

### 为什么 HNSW 快？

```ascii
理论分析：
  搜索复杂度：O(log N)
  插入复杂度：O(log N)
  内存占用：  O(N × M × log N)

对比：
  Flat:    搜索 O(N)       500ms (100万)
  IVF:     搜索 O(N/K)     5ms (100万, K=1000)
  HNSW:    搜索 O(log N)   0.5ms (100万)

为什么这么高效？
1. 多层结构：高层快速定位区域，低层精细搜索
2. 小世界特性：任意两个节点之间存在短路径
3. 贪婪搜索：每次选最近的邻居前进，类似梯度下降
4. 度数控制：通过 M 和 M_max 控制图的稠密程度
```

### FAISS HNSW 源码分析

```cpp
// FAISS 源码: IndexHNSW.h (简化核心结构)

struct IndexHNSW : Index {
    // HNSW 核心结构
    struct HNSW {
        int nb_neighbors(int layer) const;  // 获取某层邻居数
        
        // 每层的邻居列表 (最重要的数据结构)
        std::vector<std::vector<idx_t>> levels;  
        // levels[i] 是第 i 层的所有邻居关系
        // 存储格式: [node0_neighbors..., node1_neighbors..., ...]
        
        std::vector<int> offsets;  // 每个节点的偏移量
        
        std::vector<int> cum_neighbors_per_level;  // 每层累计邻居数
        
        // 搜索
        void search(
            const float* q,           // 查询向量
            int k,                     // topK
            idx_t* labels,            // 结果 ID
            float* distances,         // 结果距离
            int efSearch,             // 搜索宽度
            const float* vectors,     // 所有向量数据
            int d                     // 维度
        ) const;
    };
    
    HNSW hnsw;
    bool own_fields;
    Index* storage;  // 向量存储
};
```

### Java 视角理解 HNSW

```java
// 简化的 HNSW 节点
public class HNSWNode {
    String id;
    float[] vector;
    int level;
    // 每层的邻居列表
    List<List<HNSWNode>> neighbors;  // neighbors.get(layer) = 该层邻居

    public HNSWNode(String id, float[] vector, int level) {
        this.id = id;
        this.vector = vector;
        this.level = level;
        this.neighbors = new ArrayList<>();
        for (int i = 0; i <= level; i++) {
            neighbors.add(new ArrayList<>());
        }
    }
}
```

### 面试题

**Q：HNSW 的 efSearch 和 efConstruction 参数分别控制什么？如何调优？**
> efSearch 控制搜索时的候选队列大小。值越大，搜索越精确但越慢（efSearch=100 比 efSearch=10 召回率高但慢 3-5 倍）。efConstruction 控制构建索引时的候选队列大小，值越大索引质量越高但构建越慢。调优策略：先固定 efConstruction（如 200），调 efSearch 在速度和召回率间找平衡。80% 场景下 efSearch=50 是一个好的起点。

**Q：HNSW 为什么比 IVF 快且召回率高？**
> IVF 先聚类再搜索，问题在于查询向量可能不在最近的簇中（边界问题）。HNSW 的多层图结构天然解决了这个问题：高层负责快速定位区域，低层通过多路径搜索保证不被局部最优困住。HNSW 的"小世界"特性保证了任意两点间存在短路径。

## PQ (Product Quantization)

### 一句话本质
PQ 通过将高维向量**分块量化**，把每个向量压缩到几十字节（原大小的 1/10 到 1/50），大幅降低内存占用和距离计算成本。

### 为什么需要压缩？

```
原始向量 (1536 维 float32):
  1536 × 4 bytes = 6144 bytes ≈ 6 KB

1000 万条向量占内存:
  10,000,000 × 6 KB = 60 GB ❌ 大多数机器扛不住

PQ 压缩后 (1536 维 → 96 维 code):
  96 × 1 byte = 96 bytes ≈ 0.09 KB

1000 万条向量占内存:
  10,000,000 × 0.09 KB = 0.9 GB ✅ 轻松放入内存
```

### PQ 压缩原理

```ascii
原始向量: [0.123, -0.456, 0.789, 0.012, 0.555, -0.333, 0.111, 0.888, ...]
                          ↓           ↓           ↓           ↓
分 M 段 (M=4):  [0.123, -0.456] | [0.789, 0.012] | [0.555, -0.333] | [0.111, 0.888]
                  段 1             段 2             段 3             段 4

对每段做 K-Means 聚类 (K=256):
  段1 聚类中心:
    c0 = [-0.1, 0.2]
    c1 = [0.5, -0.3]
    c2 = [-0.4, -0.1]
    ... 256 个聚类中心

  段2 聚类中心:
    c0 = [0.3, 0.4]
    c1 = [-0.2, 0.1]
    ...

量化: 每个段用最近的聚类中心索引表示

原始向量:                        量化后:
段1: [0.123, -0.456] → 最近 c1  → 1 (1 byte)
段2: [0.789, 0.012]  → 最近 c0  → 0 (1 byte)
段3: [0.555, -0.333] → 最近 c1  → 1 (1 byte)
段4: [0.111, 0.888]  → 最近 c3  → 3 (1 byte)

压缩结果: [1, 0, 1, 3] (4 bytes)
原始: 8 floats × 4 bytes = 32 bytes
压缩比: 32/4 = 8 倍
```

### PQ 距离计算 (SDC/ADC)

```ascii
查询向量 q 与压缩后的向量 c 的距离计算：

q = [q1, q2, q3, q4, q5, q6, q7, q8]
c = [code0, code1, code2, code3]  (量化后)

距离 ≈ sum( codebook[i][code_i] 中存储的距离 )

实际计算：
  dist_q_c = 
    L2(q_段1, 码本[0][code0]) +   ← 查表 O(1)
    L2(q_段2, 码本[1][code1]) +   ← 查表 O(1)
    L2(q_段3, 码本[2][code2]) +   ← 查表 O(1)
    L2(q_段4, 码本[3][code3])     ← 查表 O(1)

注意：距离用查表法计算，不需要真正计算浮点运算！

对称距离 (SDC): 查询和数据库都量化
非对称距离 (ADC): 查询不量化，数据库量化 (更精确，推荐)
```

### PQ 的精度与速度权衡

```ascii
M (段数)   压缩比     精度     速度
────────   ──────    ────    ────
  8         8 倍      95%    极快
  16       16 倍      90%    快
  24       24 倍      85%    中等
  32       32 倍      80%    中

nbits (每段比特数):
  8 bits → 256 个聚类中心 (常用)
  4 bits → 16 个聚类中心 (压缩率更高但精度损失大)
```

## DiskANN

### 一句话本质
DiskANN 是专为**SSD 存储设计的 ANN 算法**，通过将索引放在内存、向量数据放在磁盘的方式，支持**十亿级**向量搜索，大幅降低内存成本。

### 为什么出现？

```
纯内存方案 (HNSW/FAISS) 的瓶颈：
  10 亿条 1536 维向量
  → 10亿 × 6 KB = 6 TB 内存
  → 需要 150 台 64GB 内存的服务器
  → 成本极高

DiskANN 方案：
  内存：只存索引结构 (约 10GB)
  磁盘：存向量数据 (约 6TB SSD)
  成本降低 10 倍以上
```

### 如何利用磁盘？

```ascii
DiskANN 架构：

┌───────────── 内存 (RAM) ─────────────┐
│                                        │
│  Vamana 图索引 (约 10 GB)              │
│  ┌─────────────────────────────────┐  │
│  │ Node 0 → [neighbor list]        │  │
│  │ Node 1 → [neighbor list]        │  │
│  │ Node 2 → [neighbor list]        │  │
│  │ ...                             │  │
│  └─────────────────────────────────┘  │
│                                        │
│  Cached Vectors (LRU 缓存, 约 2GB)    │
│  ┌─────────────────────────────────┐  │
│  │ 最近访问过的向量数据             │  │
│  └─────────────────────────────────┘  │
└────────────────────────────────────────┘

┌───────────── 磁盘 (SSD) ─────────────┐
│                                        │
│  向量数据文件 (约 6TB)                 │
│  ┌─────────────────────────────────┐  │
│  │ Vector 0: [0.123, -0.456, ...] │  │
│  │ Vector 1: [0.789, 0.012, ...]  │  │
│  │ Vector 2: [...]                │  │
│  │ ...                            │  │
│  └─────────────────────────────────┘  │
└────────────────────────────────────────┘
```

### 搜索流程

```ascii
搜索 q:

Step 1: 在内存图索引上导航
  使用 Vamana 图搜索，找到候选节点
  
Step 2: 如果候选节点的向量在缓存中
  → 直接用内存计算距离
  否则
  → 从 SSD 读取向量 (4KB 对齐)
  → 放入 LRU 缓存
  → 计算距离

Step 3: 重复直到找到 Top-K

优化：
  预取 (Prefetch): 预测下一个要读的向量，提前加载
  批处理: 多个查询合并读取，减少 IO 次数
  SSD 的 4KB 对齐：一次读取一个页面
```

### 面试题

**Q：DiskANN 相比 HNSW 的优缺点是什么？**
> 优点：内存占用减少 90% 以上，支持十亿级向量，成本低。缺点：延迟比 HNSW 高（多了磁盘 IO），SSD 寿命问题（频繁读取），批量写入性能不如 HNSW。选型建议：十亿级用 DiskANN，百万到千万级用 HNSW。

---

# 第五部分：相似度计算

## 一句话本质
向量相似度计算是衡量两个向量在**语义空间中接近程度**的数学方法，核心假设：语义相似 → 向量距离近。

## 三种核心距离度量

### 1. 欧氏距离 (Euclidean Distance)

```ascii
公式:
                    __________________________
                   /  n
    d(p, q) = √   Σ   (p_i - q_i)²
                 √ i=1

几何意义: 两点之间的直线距离

二维示例:
    q = (1, 2)
    p = (4, 6)
    
    d = √((1-4)² + (2-6)²)
      = √(9 + 16)
      = √25
      = 5
    
    y
    ↑
  6 │     · p (4,6)
  5 │    /|
  4 │   / |
  3 │  /  |
  2 │ ·q  |
  1 │/    |
  0 └─────────────────→ x
     0 1 2 3 4

适用场景: 向量已经归一化时，欧氏距离等价于余弦相似度
特点: 对向量的"长度"敏感
```

### 2. 余弦相似度 (Cosine Similarity)

```ascii
公式:
                  p · q          Σ(p_i × q_i)
    cos(θ) = ────────────  =  ────────────────
               ‖p‖ × ‖q‖      √Σ(p_i²) × √Σ(q_i²)

取值范围: [-1, 1]
  1  → 方向相同 (最相似)
  0  → 垂直 (不相关)
  -1 → 方向相反 (最不相似)

二维示例:
    q = (1, 0)
    p = (0.8, 0.6)
    
    cos(θ) = (1×0.8 + 0×0.6) / (1 × 1)
           = 0.8 / 1
           = 0.8
    
    y
    ↑
  1 │   /
    │  / p (0.8, 0.6)
  0.5│ /
    │/θ
  0 └──────────→ x
     0   0.5  1
    q=(1,0)

特点: 只关心方向，不关心长度
      文本 Embedding 最常用
```

### 3. 点积 (Dot Product)

```ascii
公式:
                    n
    p · q =  Σ (p_i × q_i)
                   i=1

取值范围: (-∞, +∞)

特点: 同时考虑方向和长度
      当向量已归一化时 = 余弦相似度
      推荐系统常用 (用户向量 × 物品向量)

几何意义: p 在 q 上的投影长度 × q 的长度

    p · q = ‖p‖ × ‖q‖ × cos(θ)
```

### Java 实现

```java
public class SimilarityMetrics {

    // 余弦相似度 (最常用)
    public static double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // 欧氏距离
    public static double euclideanDistance(float[] a, float[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    // 点积
    public static double dotProduct(float[] a, float[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    // 向量归一化 (L2 normalization)
    public static float[] normalize(float[] vector) {
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        float[] result = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = (float)(vector[i] / norm);
        }
        return result;
    }
}
```

```python
# Python 实现 - 相似度计算 (numpy)
import numpy as np

class SimilarityMetrics:
    @staticmethod
    def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
        """余弦相似度 (最常用)"""
        dot = float(np.dot(a, b))
        norm = float(np.linalg.norm(a) * np.linalg.norm(b))
        return dot / norm if norm != 0 else 0.0

    @staticmethod
    def euclidean_distance(a: np.ndarray, b: np.ndarray) -> float:
        """欧氏距离"""
        return float(np.linalg.norm(a - b))

    @staticmethod
    def dot_product(a: np.ndarray, b: np.ndarray) -> float:
        """点积"""
        return float(np.dot(a, b))

    @staticmethod
    def normalize(vector: np.ndarray) -> np.ndarray:
        """L2 归一化"""
        norm = np.linalg.norm(vector)
        return vector / norm if norm != 0 else vector

# 使用示例
# a = np.array([0.1, 0.2, 0.3], dtype=np.float32)
# b = np.array([0.4, 0.5, 0.6], dtype=np.float32)
# print(SimilarityMetrics.cosine_similarity(a, b))  # ≈ 0.974
```

## 为什么 "ChatGPT" 和 "AI Assistant" 距离接近？

```ascii
语义空间示意 (简化到 2 维):

语义维度2 (技术性)
    ↑
  1 │     AI Assistant
    │        ●
  0.5│  ChatGPT
    │    ●
    │
  0 └────────────────→ 语义维度1 (对话性)
     0   0.5  1  1.5

为什么距离近？
ChatGPT 的 Embedding:
  → 对话相关 (维度1: +0.9)
  → AI 技术 (维度2: +0.6)
  → 助手功能 (维度3: +0.8)

AI Assistant 的 Embedding:
  → 对话相关 (维度1: +0.8)
  → AI 技术 (维度2: +0.7)
  → 助手功能 (维度3: +0.9)

余弦相似度 ≈ 0.95 (非常接近)
```

### 数学推导

```ascii
假设 Embedding 模型将文本映射到 3 维语义空间:

维度 1: "对话性" (Chat/Dialogue)
维度 2: "技术性" (Technology/AI)
维度 3: "助手性" (Assistant/Help)

"ChatGPT" Embedding:      [0.9, 0.6, 0.8]
"AI Assistant" Embedding: [0.8, 0.7, 0.9]
"Apple iPhone" Embedding: [0.1, 0.9, 0.1]
"美国总统" Embedding:     [0.3, 0.2, 0.2]

余弦相似度计算:

ChatGPT vs AI Assistant:
  dot = 0.9×0.8 + 0.6×0.7 + 0.8×0.9 = 0.72 + 0.42 + 0.72 = 1.86
  |ChatGPT| = √(0.81 + 0.36 + 0.64) = √1.81 = 1.345
  |AI Asst| = √(0.64 + 0.49 + 0.81) = √1.94 = 1.393
  cos = 1.86 / (1.345 × 1.393) = 1.86 / 1.874 = 0.993 ✅ 极接近

ChatGPT vs Apple iPhone:
  dot = 0.9×0.1 + 0.6×0.9 + 0.8×0.1 = 0.09 + 0.54 + 0.08 = 0.71
  cos = 0.71 / (1.345 × 0.906) = 0.71 / 1.219 = 0.582 ❌ 不接近

结论: Embedding 让语义相似的文本在向量空间中聚在一起
```

### 面试题

**Q：什么时候用余弦相似度，什么时候用欧氏距离？**
> 文本 Embedding 通常做 L2 归一化（所有向量长度为 1），此时余弦相似度 = 1 - 欧氏距离²/2，两者等价。但余弦只关心方向（语义方向），欧氏距离还关心长度（置信度）。推荐系统中用户对物品的喜好程度用点积更好。一句话：文本语义搜索用余弦；向量已归一化时三者等价；需要同时考虑方向和长度用点积。

---

# 第六部分：向量数据库所有功能

## Insert

### 一句话本质
向量插入是将**向量 + ID + Metadata** 写入向量数据库并构建索引的过程。

### 为什么需要？

```
用户上传文档 → Embedding 生成向量 → 插入向量数据库

业务流程：
  用户上传 PDF 文档
       ↓
  文本提取 (PDF Parser)
       ↓
  文本分块 (Chunking, 每块 512 tokens)
       ↓
  Embedding (每个 Chunk 生成一个向量)
       ↓
  插入向量数据库
       ↓
  用户提问 → 搜索最相关的 Chunk
```

### 如何实现？

```ascii
插入流程（以 Qdrant 为例）：

Client:
  POST /collections/docs/points
  {
    "points": [{
      "id": 1001,
      "vector": [0.123, -0.456, ...],
      "payload": {
        "title": "ChatGPT介绍",
        "author": "张三"
      }
    }]
  }
            │
            ▼
Qdrant Server:
  Step 1: 写入 WAL (Write-Ahead Log)
          → 确保数据不丢失
          
  Step 2: 追加到活跃 Segment
          → 向量写入内存缓冲区
          → Payload 写入 RocksDB
          
  Step 3: 更新索引
          → 如果 Segment 启用了 HNSW
          → 将新向量加入 HNSW 图
          → 建立与邻居的连接
          
  Step 4: 返回成功
```

### 与 MySQL/Redis/ES 对比

```ascii
MySQL:     INSERT INTO table VALUES (...)
           → 写入 InnoDB Buffer Pool
           → 写入 Redo Log
           → 写入 B+Tree

Redis:     SET key value
           → 写入内存 Hash Table
           → (可选) AOF/RDB 持久化

ES:        POST /index/_doc
           → 写入内存 Buffer
           → 刷新到 Segment
           → 构建倒排索引

VectorDB:  POST /collections/docs/points
           → 写入 WAL
           → 追加到 Segment
           → 更新 HNSW 图
```

### Python 代码示例

```python
# Python 实现 - 使用 qdrant-client 插入向量
from qdrant_client import QdrantClient
from qdrant_client.models import PointStruct, VectorParams, Distance
import numpy as np

# 连接 Qdrant
client = QdrantClient("localhost", port=6333)

# 创建 Collection
client.recreate_collection(
    collection_name="documents",
    vectors_config=VectorParams(size=1536, distance=Distance.COSINE),
)

# 批量插入
points = [
    PointStruct(
        id=1001,
        vector=np.random.rand(1536).tolist(),
        payload={"title": "ChatGPT介绍", "author": "张三", "year": 2024},
    ),
    PointStruct(
        id=1002,
        vector=np.random.rand(1536).tolist(),
        payload={"title": "AI基础", "author": "李四", "year": 2023},
    ),
]
client.upsert(collection_name="documents", points=points)
print(f"成功插入 {len(points)} 条向量")
```

```python
# Python 实现 - 使用 pymilvus 插入向量
from pymilvus import Collection, CollectionSchema, FieldSchema, DataType, connections

connections.connect("default", host="localhost", port="19530")

# 定义 Schema
fields = [
    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True),
    FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=1536),
    FieldSchema(name="title", dtype=DataType.VARCHAR, max_length=200),
    FieldSchema(name="author", dtype=DataType.VARCHAR, max_length=100),
]
schema = CollectionSchema(fields, description="文档集合")
collection = Collection("documents", schema)

# 插入数据
import random
entities = [
    [1001, 1002],  # id
    [[random.random() for _ in range(1536)],   # vector 0
     [random.random() for _ in range(1536)]],  # vector 1
    ["ChatGPT介绍", "AI基础"],   # title
    ["张三", "李四"],            # author
]
collection.insert(entities)
collection.flush()
print("插入完成")
```

### 面试题

**Q：向量数据库插入和 MySQL 插入有什么本质不同？**
> MySQL 插入 B+Tree 是 O(log n) 的精确维护，写入后立即对后续查询可见。向量数据库插入 HNSW 图不仅涉及当前节点，还要更新邻居的邻居列表（级联更新），且构建索引是异步的（写入内存 → 后台构建 → 刷盘），不像 MySQL 那样"写入即索引就绪"。

---

## Update

### 一句话本质
向量更新是替换已有 ID 的向量和/或元数据，通常实现为**删除+插入**。

### 为什么需要？

```
场景：文档被修改
  用户编辑了文章 → 重新 Embedding → 更新向量

场景：元数据变更
  文章分类从 "技术" 改为 "AI" → 只更新 metadata
```

### 如何实现？

```ascii
两种实现策略：

策略一: 原地更新 (Qdrant)
  Step 1: 标记旧向量为"已删除" (墓碑标记)
  Step 2: 写入新向量
  Step 3: (后台) 段合并时清理删除的数据

策略二: 删除+重新插入 (大多数实现)
  Step 1: 调用 delete(id)
  Step 2: 调用 insert(id, new_vector)

注意：HNSW 图不支持"更新"操作
  → 删除节点：标记删除，不真正移除 (否则需要重建图)
  → 插入新节点：重新插入
```

### Python 代码示例

```python
# Python 实现 - 向量更新 (删除 + 重新插入)
from qdrant_client import QdrantClient
from qdrant_client.models import PointStruct, Filter, FieldCondition, MatchValue
import numpy as np

client = QdrantClient("localhost", port=6333)

# 更新 ID=1001 的向量和元数据
# Step 1: 先删除
client.delete(
    collection_name="documents",
    points_selector=Filter(
        must=[FieldCondition(key="id", match=MatchValue(value=1001))]
    ),
)

# Step 2: 重新插入新向量
new_vector = np.random.rand(1536).tolist()
client.upsert(
    collection_name="documents",
    points=[
        PointStruct(
            id=1001,
            vector=new_vector,
            payload={"title": "ChatGPT 最新介绍", "author": "张三", "year": 2024},
        )
    ],
)
print("向量更新完成")
```

### 面试题

**Q：为什么向量数据库的更新比 MySQL 麻烦？**
> MySQL 的 B+Tree 支持原地更新（修改叶子节点的值）。HNSW 图不支持"修改节点的向量"，因为向量的变化意味着该节点在图中的位置可能完全改变（邻居关系需要重构）。所以向量数据库只能"删旧插新"。

---

## Delete

### 一句话本质
向量删除是标记或移除指定 ID 的向量及其索引关系。

### 如何实现？

```ascii
软删除 (墓碑标记):

Segment:
┌─────────────────────────────┐
│ Vector 0: [0.1, 0.2, ...]  │  ← 活跃
│ Vector 1: [0.3, 0.4, ...]  │  ← 活跃
│ Vector 2: [0.5, 0.6, ...]  │  ← 🪦 已删除 (标记)
│ Vector 3: [0.7, 0.8, ...]  │  ← 活跃
└─────────────────────────────┘

删除向量 2:
  - 在删除位图 (bitmap) 中标记第 2 位为 1
  - 搜索时跳过标记为删除的向量
  - 段合并时真正清理

为什么不用硬删除？
   删除 HNSW 图中的节点需要：
   1. 移除该节点所有邻居中指向它的边
   2. 调整邻居关系确保图连通性
   3. 代价非常高 → 用软删除
```

### Python 代码示例

```python
# Python 实现 - 向量删除 (按 ID 或 Filter)
from qdrant_client import QdrantClient, models

client = QdrantClient("localhost", port=6333)

# 按 ID 删除单个
client.delete(
    collection_name="documents",
    points_selector=models.PointIdsList(points=[1001]),
)

# 按 Filter 批量删除
client.delete(
    collection_name="documents",
    points_selector=models.Filter(
        must=[
            models.FieldCondition(
                key="year",
                range=models.Range(lt=2023),
            )
        ]
    ),
)

# 删除整个 Collection (相当于 MySQL DROP TABLE)
client.delete_collection("documents")
print("删除操作完成")
```

---

## Search

### 一句话本质
向量搜索是输入查询向量，返回向量数据库中最相似的 Top-K 个结果的过程，核心是 **ANN 索引**的搜索算法。

### 搜索流程

```ascii
完整搜索链路：

用户输入: "什么是人工智能？"
    │
    ▼
1. Embedding (客户端或服务端)
    "什么是人工智能？" → [0.123, -0.456, ...]
    │
    ▼
2. 路由
    查询向量 → 定位到 Collection
    → (如果分片) 广播到所有 Shard
    │
    ▼
3. 搜索索引 (核心)
    在每个 Segment 上执行 ANN 搜索
    Flat / IVF / HNSW / PQ 搜索算法
    → 每个 Segment 返回 local_topK
    │
    ▼
4. 合并结果
    合并所有 Segment 的结果
    按距离排序 → 取全局 Top-K
    │
    ▼
5. 获取元数据
    根据 ID 从元数据存储中获取 payload
    如 title, author, content 等
    │
    ▼
6. 返回给用户
    {
      "result": [
        {"id": 1001, "score": 0.95, "payload": {...}},
        {"id": 2003, "score": 0.87, "payload": {...}},
        ...
      ]
    }
```

### 搜索参数

```ascii
关键搜索参数：

topK:     返回结果数量 (10/50/100)
efSearch: HNSW 搜索宽度 (越大越精确越慢)
nprobe:   IVF 搜索的簇数 (越大越精确越慢)
radius:   距离阈值过滤 (只返回距离小于 radius 的)
score:    阈值过滤 (只返回分数大于 threshold 的)
```

### Python 代码示例

```python
# Python 实现 - 向量搜索
from qdrant_client import QdrantClient
import numpy as np

client = QdrantClient("localhost", port=6333)

# 生成一个查询向量 (实际应由 Embedding 模型生成)
query_vector = np.random.rand(1536).tolist()

# 基本搜索
results = client.search(
    collection_name="documents",
    query_vector=query_vector,
    limit=10,  # topK
)

for hit in results:
    print(f"ID: {hit.id}, Score: {hit.score:.4f}, Title: {hit.payload['title']}")
```

```python
# Python 实现 - 使用 pymilvus 搜索
from pymilvus import Collection, connections

connections.connect("default", host="localhost", port="19530")
collection = Collection("documents")
collection.load()

# 搜索
search_params = {"metric_type": "COSINE", "params": {"nprobe": 10}}
results = collection.search(
    data=[np.random.rand(1536).tolist()],
    anns_field="vector",
    param=search_params,
    limit=10,
    output_fields=["title", "author"],
)

for hits in results:
    for hit in hits:
        print(f"ID: {hit.id}, Distance: {hit.distance:.4f}, Title: {hit.entity.get('title')}")
```

---

## Filter

### 一句话本质
Filter 是在向量搜索的同时，根据元数据条件过滤结果，实现**向量相似度 + 结构化条件**的混合查询。

### 为什么需要？

```
场景：知识库搜索
  "找 AI 相关的文档" (向量搜索)
  + "2024 年发布的" (Metadata 过滤)
  + "作者是张三" (Metadata 过滤)
  + "分类是技术" (Metadata 过滤)

没有 Filter：
  → 搜索出所有与 AI 相似的文档
  → 包含 2023 年的、2024 年的
  → 需要应用层二次过滤

有 Filter：
  → 搜索时只考虑满足条件的向量
  → 更精确、更高效
```

### 如何实现？

```ascii
两种过滤策略：

策略一: Post-Filter (先搜后过滤) - 简单但效率低

  Step 1: 搜索 Top-1000 (远大于实际需要的 Top-10)
  Step 2: 在 1000 个结果中过滤 metadata
  Step 3: 返回过滤后的 Top-10

  问题：如果过滤后只剩 3 个，召回率低
        搜索量需要设置很大

策略二: Pre-Filter (先过滤后搜) - 高效但有限制

  Step 1: 找到满足 metadata 条件的所有 ID
  Step 2: 只在这些 ID 中搜索向量

  问题：如果过滤后的集合很小，搜索效果好
        如果过滤后仍有大量数据，优势不大
        需要在 metadata 上建索引

策略三: 结合搜索 (最佳实践)

  Step 1: 将 filter 条件下推到 Segment
  Step 2: Segment 内先检查 metadata 位图
  Step 3: 只对满足条件的向量计算距离

  实现：metadata 上用 Bitmap 索引
        满足条件的位为 1，不满足为 0
        搜索时跳过位为 0 的向量
```

### 与 MySQL 对比

```ascii
MySQL:
  SELECT * FROM documents 
  WHERE category = 'AI'          ← 条件过滤
  ORDER BY created_at DESC       ← 排序
  LIMIT 10

向量数据库:
  POST /collections/docs/points/search
  {
    "vector": [0.123, ...],      ← 相似度搜索
    "filter": {
      "must": [
        {"key": "category", "match": {"value": "AI"}},  ← 条件过滤
        {"key": "year", "range": {"gte": 2024}}
      ]
    },
    "limit": 10
  }

异同：
  相同：都需要过滤条件
  不同：MySQL 对过滤后的结果排序
        向量数据库对过滤后的结果算相似度
```

### Python 代码示例

```python
# Python 实现 - Filter 过滤搜索
from qdrant_client import QdrantClient, models
import numpy as np

client = QdrantClient("localhost", port=6333)

query_vector = np.random.rand(1536).tolist()

# 带 Filter 的搜索 (Post-Filter 方式)
results = client.search(
    collection_name="documents",
    query_vector=query_vector,
    query_filter=models.Filter(
        must=[
            models.FieldCondition(
                key="year",
                range=models.Range(gte=2024),  # year >= 2024
            ),
            models.FieldCondition(
                key="author",
                match=models.MatchValue(value="张三"),  # author == "张三"
            ),
        ],
    ),
    limit=10,
)

for hit in results:
    print(f"ID={hit.id}, Score={hit.score:.4f}, Year={hit.payload['year']}")
```

```python
# Python 实现 - 手动 Pre-Filter + Search
# 场景: 先用 metadata 过滤出候选 ID 集合，再搜索

# 先查询满足条件的 ID
filter_result = client.scroll(
    collection_name="documents",
    scroll_filter=models.Filter(
        must=[models.FieldCondition(key="category", match=models.MatchValue(value="AI"))],
    ),
    limit=1000,
)

filtered_ids = [point.id for point in filter_result[0]]
print(f"满足 category=AI 的文档数: {len(filtered_ids)}")

# 如果有满足条件的结果，在这些 ID 中搜索
if filtered_ids:
    results = client.search(
        collection_name="documents",
        query_vector=query_vector,
        query_filter=models.Filter(
            must=[models.FieldCondition(
                key="id",
                match=models.MatchAny(any=filtered_ids),
            )],
        ),
        limit=10,
    )
    for hit in results:
        print(f"ID={hit.id}, Score={hit.score:.4f}")
```

### 面试题

**Q：向量数据库的 Filter 为什么比 MySQL 的 WHERE 慢？**
> MySQL 的 WHERE 配合索引可以 O(log n) 精确过滤。向量数据库的 Filter 需要在搜索过程中检查每个候选向量的 metadata，且 metadata 索引和向量索引是分离的，需要额外的 IO。优化方法：1) metadata 上建索引（如 Bitmap）；2) 使用 Pre-Filter；3) 将常用过滤条件作为向量搜索的约束下推。

---

## Metadata

### 一句话本质
Metadata 是附着在向量上的**结构化数据**，用于存储除了向量本身之外的所有信息，支持过滤、排序、返回结果。

### 存储方式

```ascii
Metadata 存储选项：

1. SQLite (Qdrant)
   ┌─────────┬──────────┬────────────┐
   │  id     │  payload  │            │
   ├─────────┼──────────┼────────────┤
   │ 1001    │ {"title":│  JSON 字段  │
   │         │  "...",   │             │
   │         │ "author":│             │
   │         │  "张三"}  │             │
   └─────────┴──────────┴────────────┘

2. RocksDB (Milvus)
   Key: id → Value: Protobuf 序列化的 metadata

3. Parquet 列存
   列式存储，适合批量读取和分析

4. 单独字段 (Chroma)
   Python dict 直接存在内存中
```

### Python 代码示例

```python
# Python 实现 - 各种向量数据库的 Metadata 操作

# ---- Qdrant ----
from qdrant_client import QdrantClient, models

client = QdrantClient("localhost", port=6333)

# 写入带 metadata (payload) 的向量
client.upsert(
    collection_name="documents",
    points=[
        models.PointStruct(
            id=1,
            vector=[0.1] * 1536,
            payload={
                "title": "ChatGPT 入门",
                "author": "张三",
                "tags": ["AI", "LLM", "NLP"],
                "page_count": 120,
                "is_published": True,
            },
        ),
    ],
)

# 只更新 metadata (不改变向量)
client.set_payload(
    collection_name="documents",
    payload={"is_published": False, "reviewed_by": "李四"},
    points=[1],
)

# 删除指定 metadata 字段
client.delete_payload(
    collection_name="documents",
    keys=["reviewed_by"],
    points=[1],
)

# ---- Chroma (Metadata 最简洁) ----
import chromadb

chroma_client = chromadb.Client()
collection = chroma_client.create_collection("docs")

collection.add(
    ids=["1", "2"],
    embeddings=[[0.1, 0.2, 0.3], [0.4, 0.5, 0.6]],
    metadatas=[
        {"title": "Doc 1", "category": "AI"},
        {"title": "Doc 2", "category": "ML"},
    ],
    documents=["内容1", "内容2"],
)

# 按 metadata 过滤查询
results = collection.query(
    query_embeddings=[[0.1, 0.2, 0.3]],
    n_results=5,
    where={"category": "AI"},  # metadata 过滤
)
```

---

## Hybrid Search

### 一句话本质
Hybrid Search 是**向量搜索（语义）+ 关键词搜索（精确匹配）**的混合，两者结果通过 RRF（Reciprocal Rank Fusion）算法合并，兼顾语义理解和精确匹配。

### 为什么需要？

```ascii
纯向量搜索的问题：

搜索："iPhone 15 价格"
  
  Embedding 理解的是语义：
    → "iPhone 15 cost" (语义相似)
    → "手机 15 代 多少钱" (语义相似)
    → 但可能漏掉包含 "价格" 关键词但语义稍偏的文档

纯 ES 关键词搜索的问题：

搜索："机器学习"
  → 找到包含 "机器学习" 的文档
  → 但找不到包含 "深度学习" 但没写 "机器学习" 的文档

Hybrid Search = 向量搜索 + 关键词搜索

    "iPhone 15 价格"
         │
    ┌────┴────┐
    │         │
  向量搜索   关键词搜索
    │         │
  Top-100   Top-100
    │         │
    └────┬────┘
         │ RRF 融合
         ▼
     最终 Top-10
```

### RRF 算法

```ascii
RRF (Reciprocal Rank Fusion):

score = Σ 1 / (k + rank_i(s))

其中：
  rank_i(s) = 文档 s 在第 i 个搜索中的排名
  k = 常数 (通常 60)

示例：
  文档 A: 向量搜索排名第2, 关键词搜索排名第10
    score = 1/(60+2) + 1/(60+10) = 0.0161 + 0.0143 = 0.0304
  
  文档 B: 向量搜索排名第15, 关键词搜索排名第1
    score = 1/(60+15) + 1/(60+1) = 0.0133 + 0.0164 = 0.0297
  
  文档 A 和 B 都有机会出现在最终结果中
```

### 实现方式

```ascii
Hybrid Search 架构：

┌─────────┐
│  用户输入 │  "iPhone 15 价格"
└────┬────┘
     │
     ├──────────────────┐
     │                  │
     ▼                  ▼
  向量搜索           关键词搜索
  (ANN Index)       (倒排索引)
     │                  │
     ▼                  ▼
  Top-100 结果       Top-100 结果
     │                  │
     └────────┬─────────┘
              │ RRF 融合
              ▼
        最终结果 Top-10
```

### Python 代码示例

```python
# Python 实现 - Hybrid Search + RRF 融合
import numpy as np
from qdrant_client import QdrantClient, models

client = QdrantClient("localhost", port=6333)


def hybrid_search(query_text: str, query_vector: list, top_k: int = 10):
    """向量搜索 + 关键词搜索 + RRF 融合"""
    k = 60  # RRF 常数

    # 1. 向量语义搜索
    vector_results = client.search(
        collection_name="documents",
        query_vector=query_vector,
        limit=top_k * 2,
    )
    vector_ranks = {hit.id: rank for rank, hit in enumerate(vector_results)}

    # 2. 关键词全文搜索 (Qdrant 内置全文索引)
    keyword_results = client.search(
        collection_name="documents",
        query_vector=query_vector,  # 纯关键词用 dummy 向量
        query_filter=models.Filter(
            must=[
                models.FieldCondition(
                    key="title",
                    match=models.MatchText(text=query_text),
                )
            ],
        ),
        limit=top_k * 2,
    )
    keyword_ranks = {hit.id: rank for rank, hit in enumerate(keyword_results)}

    # 3. RRF 融合排序
    all_ids = set(vector_ranks.keys()) | set(keyword_ranks.keys())
    rrf_scores = {}
    for doc_id in all_ids:
        score = 0
        if doc_id in vector_ranks:
            score += 1 / (k + vector_ranks[doc_id])
        if doc_id in keyword_ranks:
            score += 1 / (k + keyword_ranks[doc_id])
        rrf_scores[doc_id] = score

    # 4. 按 RRF 分数降序排列
    ranked = sorted(rrf_scores.items(), key=lambda x: -x[1])
    return ranked[:top_k]


# 使用示例
query_vec = np.random.rand(1536).tolist()
results = hybrid_search("iPhone 15 价格", query_vec, top_k=5)
for doc_id, score in results:
    print(f"Doc {doc_id}: RRF Score = {score:.4f}")
```

```python
# Python 实现 - Qdrant 内置 Hybrid Search (prefer 方式)
from qdrant_client import QdrantClient, models

client = QdrantClient("localhost", port=6333)

# 创建支持全文搜索的 Collection
client.recreate_collection(
    collection_name="hybrid_demo",
    vectors_config=models.VectorParams(size=1536, distance=Distance.COSINE),
)

# 给文本字段创建全文索引
client.create_payload_index(
    collection_name="hybrid_demo",
    field_name="title",
    field_type=models.PayloadFieldType.TEXT,
)

client.create_payload_index(
    collection_name="hybrid_demo",
    field_name="content",
    field_type=models.PayloadFieldType.TEXT,
)

# Hybrid Search: 向量 + 关键词同时检索
results = client.search(
    collection_name="hybrid_demo",
    query_vector=np.random.rand(1536).tolist(),
    query_filter=models.Filter(
        must=[
            models.FieldCondition(
                key="content",
                match=models.MatchText(text="机器学习 深度学习"),
            )
        ]
    ),
    limit=10,
)
```

### 面试题

**Q：Hybrid Search 什么时候比纯向量搜索好？**
> 当查询包含专有名词（如产品名 "iPhone 15"）、代码标识符（如 "UserServiceImpl"）、精确 ID（如 "DOC-2024-001"）时，关键词搜索更准确；当查询是抽象概念（如 "机器学习的未来趋势"）时，向量搜索更好。Hybrid Search 结合两者优势。

---

## Rerank

### 一句话本质
Rerank 使用一个**更精确但更慢的模型**对 ANN 搜索的 Top-K 结果重新排序，提升最终结果的准确性。

### 为什么需要？

```ascii
问题：
  ANN 索引为了速度做了近似 → 结果可能有偏差
  Top-100 中有一些不相关的被排到前面了
  
解决：
  用 Cross-Encoder 模型重新打分
  Cross-Encoder 比双编码器（Bi-Encoder）更精确
  
流程：
  Step 1: ANN 搜索 → Top-100 (快但不精确)
  Step 2: Cross-Encoder Rerank → Top-10 (慢但精确)

模型对比：
  Bi-Encoder (Embedding 模型):
    文本 → 向量 (一次编码)
    优点：向量可缓存，搜索快
    缺点：精度一般

   Cross-Encoder (Rerank 模型):
     查询+文档 → 相关性分数 (每次重新编码)
     优点：精度高
     缺点：慢，不能缓存
```

### Python 代码示例

```python
# Python 实现 - 使用 Cross-Encoder 进行 Rerank
from sentence_transformers import CrossEncoder
import numpy as np

# 加载 Cross-Encoder 模型 (比 Bi-Encoder 更精确)
reranker = CrossEncoder("cross-encoder/ms-marco-MiniLM-L-6-v2")


def search_with_rerank(query: str, candidates: list[dict], top_k: int = 10):
    """搜索 + Rerank 流程"""
    # Step 1: ANN 搜索得到候选 (已由向量数据库完成)
    # 假设 candidates 是向量数据库返回的 Top-100

    # Step 2: Cross-Encoder 对 (query, doc) 重新打分
    pairs = [(query, doc["text"]) for doc in candidates]
    scores = reranker.predict(pairs)  # 返回每个 pair 的相关性分数

    # Step 3: 按新分数排序
    for i, doc in enumerate(candidates):
        doc["rerank_score"] = float(scores[i])

    candidates.sort(key=lambda x: -x["rerank_score"])
    return candidates[:top_k]


# 使用示例
query = "什么是 Transformer 架构？"
candidates = [
    {"id": 1, "text": "Transformer 是一种神经网络架构..."},
    {"id": 2, "text": "ChatGPT 是基于 GPT 的对话系统..."},
    {"id": 3, "text": "注意力机制是 Transformer 的核心..."},
    # ... 通常 100 个候选
]

top_results = search_with_rerank(query, candidates, top_k=3)
for doc in top_results:
    print(f"Doc {doc['id']}, Rerank Score: {doc['rerank_score']:.4f}")
    print(f"  {doc['text'][:50]}...")
```

```python
# Python 实现 - 使用 Cohere 的 Rerank API (生产推荐)
import cohere

co = cohere.Client("YOUR_API_KEY")

# Cohere Rerank (比本地模型更精确)
results = co.rerank(
    query="What is the capital of France?",
    documents=[
        "Paris is the capital of France.",
        "Berlin is the capital of Germany.",
        "Madrid is the capital of Spain.",
    ],
    top_n=3,
    model="rerank-english-v3.0",
)

for doc in results.results:
    print(f"Index: {doc.index}, Relevance: {doc.relevance_score:.4f}")
```

---

## Collection / Partition / Shard / Replication

### Collection (集合)

```ascii
Collection = MySQL 中的 表 (Table)

CREATE COLLECTION documents (
    id VARCHAR(64),
    vector FLOAT[] (dim=1536),
    metadata MAP
);

等价于 MySQL:
  CREATE TABLE documents (
      id VARCHAR(64) PRIMARY KEY,
      vector BLOB,           ← MySQL 没有向量类型
      ...其他元数据列
  );
```

### Partition (分区)

```ascii
Partition = MySQL 中的 分区表

Collection: documents
    ├── Partition: 2024-01
    │   ├── Segment 0
    │   └── Segment 1
    ├── Partition: 2024-02
    │   └── Segment 0
    └── Partition: 2024-03
        └── Segment 0

用途：
  按时间/地域等维度物理隔离数据
  查询时指定 partition → 减少搜索范围
  不同 partition 可独立管理

类比 MySQL:
  CREATE TABLE documents (
      ...
  ) PARTITION BY RANGE (YEAR(created_at)) (
      PARTITION p2024 VALUES LESS THAN (2025),
      PARTITION p2025 VALUES LESS THAN (2026)
  );
```

### Shard (分片)

```ascii
Shard = MySQL 中的 分库分表 (水平拆分)

Collection: documents
    ├── Shard 0 (Node A)
    │   ├── Segment 0
    │   └── Segment 1
    ├── Shard 1 (Node B)
    │   ├── Segment 0
    │   └── Segment 1
    └── Shard 2 (Node C)
        ├── Segment 0
        └── Segment 1

搜索流程：
  查询 → 广播到所有 Shard
  Shard 0 返回 Top-K
  Shard 1 返回 Top-K
  Shard 2 返回 Top-K
  → 合并 → 全局 Top-K

类比 MySQL:
  Shard 0: documents_0 (id % 3 = 0)
  Shard 1: documents_1 (id % 3 = 1)
  Shard 2: documents_2 (id % 3 = 2)
```

### Replication (副本)

```ascii
Replication = MySQL 的 主从复制

Shard 0:
    ├── Leader (主, 可读写)
    ├── Follower 1 (从, 只读)
    └── Follower 2 (从, 只读)

作用：
  - 高可用：Leader 挂了，Follower 接管
  - 读扩展：查询可以分摊到多个 Follower
  - 容灾：数据多副本

Milvus 实现：
  写请求 → Leader (Raft 共识协议)
  读请求 → Leader 或 Follower (一致性取决于配置)

类比 MySQL:
  Master: 写 binlog
  Slave: 读取 binlog → relay log → apply
```

### Python 代码示例

```python
# Python 实现 - Collection / Partition / Shard / Replication 操作

# ---- Collection 操作 (pymilvus) ----
from pymilvus import CollectionSchema, FieldSchema, DataType, Collection, connections

connections.connect("default", host="localhost", port="19530")

# 创建 Collection (相当于 CREATE TABLE)
fields = [
    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=False),
    FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=1536),
    FieldSchema(name="title", dtype=DataType.VARCHAR, max_length=512),
    FieldSchema(name="year", dtype=DataType.INT64),
]
schema = CollectionSchema(fields, "文档知识库")
collection = Collection(name="documents", schema=schema)
print(f"Collection 创建完成: {collection.name}")

# 创建索引
index_params = {
    "metric_type": "COSINE",
    "index_type": "HNSW",
    "params": {"M": 16, "efConstruction": 200},
}
collection.create_index(field_name="vector", index_params=index_params)
collection.load()

# 查看集合信息
print(f"Collection 名称: {collection.name}")
print(f"Schema: {collection.schema}")

# ---- Partition 操作 ----
# 创建分区 (相当于 CREATE PARTITION)
collection.create_partition(partition_name="p_2024")
collection.create_partition(partition_name="p_2023")

# 向指定分区插入数据
import random
collection.insert(
    [[1001], [[random.random() for _ in range(1536)]], ["2024年文档"], [2024]],
    partition_name="p_2024",
)

# 在指定分区内搜索
collection.load()
results = collection.search(
    data=[[random.random() for _ in range(1536)]],
    anns_field="vector",
    param={"metric_type": "COSINE", "params": {"nprobe": 10}},
    limit=5,
    partition_names=["p_2024"],  # 只搜这个分区
)
print(f"在 p_2024 分区找到 {len(results[0])} 个结果")

# ---- Shard 配置 (Milvus 在创建 Collection 时指定) ----
collection_with_shards = Collection(
    name="sharded_docs",
    schema=schema,
    # shards_num=4,  # 指定分片数
    properties={"collection.shards.num": 4},
)
print(f"4 分片的 Collection 创建完成")

# ---- 删除 Collection ----
# collection.drop()
```

```python
# Python 实现 - Qdrant Collection 创建与管理
from qdrant_client import QdrantClient, models

client = QdrantClient("localhost", port=6333)

# 创建 Collection (指定向量配置)
client.recreate_collection(
    collection_name="my_docs",
    vectors_config=models.VectorParams(
        size=1536,
        distance=models.Distance.COSINE,
        on_disk=False,  # True = 向量存磁盘, False = 内存
    ),
    # 分片和副本配置
    shard_number=3,        # 3 个分片
    replication_factor=2,  # 每个分片 2 个副本
)

# 查看 Collection 信息
info = client.get_collection("my_docs")
print(f"Collection: {info.name}")
print(f"  分片数: {info.config.params.shard_number}")
print(f"  副本数: {info.config.params.replication_factor}")
print(f"  向量数: {info.points_count}")
```

---

# 第七部分：向量数据库架构

## 完整链路

```ascii
RAG 系统完整架构：

┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  Client  │───→│Embedding │───→│Vector DB │───→│  Rerank  │───→│   LLM    │
│ (用户)    │    │ 模型     │    │          │    │ 模型     │    │ (GPT-4)  │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
     │               │               │               │               │
     │ 查询文本       │ 转为向量       │ 近似搜索       │ 精确排序       │ 生成回答
     │ "什么是AI?"    │ [0.123,...]   │ Top-100        │ Top-10         │
     │               │               │               │               │
     ▼               ▼               ▼               ▼               ▼
  用户输入        向量化          检索 TOP-K      重排序          生成
  "什么是AI?"    → [0.123,...]   → TOP-100 文档  → TOP-10 文档   → "人工智能是..."

数据流向：
  Client ──(1)──→ Embedding: 将自然语言转为向量
  Embedding ──(2)──→ Vector DB: 用向量搜索相关文档
  Vector DB ──(3)──→ Rerank: 对搜索结果精确排序
  Rerank ──(4)──→ LLM: 将问题+相关文档发给 LLM
  LLM ──(5)──→ Client: 返回最终回答
```

## 向量数据库内部架构

```ascii
向量数据库内部组件（以 Milvus 为例）：

                    ┌─────────────────────┐
                    │   Client / SDK       │
                    │   (Java, Go, Python) │
                    └──────────┬──────────┘
                               │ gRPC
                               ▼
┌──────────────────────────────────────────────────┐
│                   Proxy Layer                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ 请求路由  │  │ 权限校验  │  │ 协议转换  │       │
│  └──────────┘  └──────────┘  └──────────┘       │
│  功能：解析请求 → 路由到 Query Node              │
│        合并结果 → 返回给 Client                   │
└──────────────────────┬───────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────┐
│                   Query Layer                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │Query Node│  │Query Node│  │Query Node│       │
│  │ ┌──────┐ │  │ ┌──────┐ │  │ ┌──────┐ │       │
│  │ │Segment│ │  │ │Segment│ │  │ │Segment│ │       │
│  │ │ 0..N  │ │  │ │ 0..N  │ │  │ │ 0..N  │ │       │
│  │ └──────┘ │  │ └──────┘ │  │ └──────┘ │       │
│  │  HNSW    │  │  HNSW    │  │  HNSW    │       │
│  └──────────┘  └──────────┘  └──────────┘       │
│  功能：执行向量搜索，每个 Query Node 负责部分数据  │
└──────────────────────┬───────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────┐
│                  Index Layer                      │
│  ┌──────────────────────────────────────────┐    │
│  │  Index Node (构建 HNSW/IVF/PQ 索引)       │    │
│  │  - 定时从 Data Node 拉取数据               │    │
│  │  - 构建索引                               │    │
│  │  - 将索引文件推送到对象存储                 │    │
│  └──────────────────────────────────────────┘    │
└──────────────────────┬───────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────┐
│                  Storage Layer                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │  Data    │  │  Object  │  │  Meta    │       │
│  │  Node    │  │  Store   │  │  Store   │       │
│  │ (WAL)    │  │ (MinIO/  │  │ (etcd/   │       │
│  │          │  │  S3)     │  │  MySQL)  │       │
│  └──────────┘  └──────────┘  └──────────┘       │
│  功能：持久化数据、管理元数据、对象存储           │
└──────────────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────┐
│               Scheduler / Coordinator             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ Root     │  │ Data     │  │ Query    │       │
│  │Coord     │  │Coord     │  │Coord     │       │
│  │(集群管)   │  │(数据分)  │  │(查询调)  │       │
│  └──────────┘  └──────────┘  └──────────┘       │
│  功能：任务调度、负载均衡、故障恢复               │
└──────────────────────────────────────────────────┘
```

## Component Breakdown

```ascii
重要概念关系：

Collection (数据库/表)
    │
    ├── Schema (定义了向量维度、元数据字段)
    │       │
    │       ├── Field: vector (FLOAT_VECTOR, dim=1536)
    │       ├── Field: title (VARCHAR)
    │       ├── Field: author (VARCHAR)
    │       └── Field: created_at (INT64)
    │
    ├── Index (索引定义)
    │       │
    │       ├── Index Type: HNSW
    │       ├── Parameters: M=16, efConstruction=200
    │       └── Metric Type: COSINE
    │
    ├── Shard (分布式分片)
    │       │
    │       ├── Shard 0: Node A, Node D (Leader + Follower)
    │       ├── Shard 1: Node B, Node E
    │       └── Shard 2: Node C, Node F
    │
    └── Segment (数据单元)
            │
            ├── Active Segment (正在写入)
            │   └── 内存中的向量 + WAL
            │
            ├── Sealed Segment (已封存)
            │   ├── 向量文件 (.vec)
            │   ├── 索引文件 (.idx)
            │   └── 元数据 (RocksDB)
            │
            └── (后台合并 → 更大的 Segment)
```

### Segment 生命周期

```ascii
写入流程（Milvus 风格）：

时间轴 ──────────────────────────────────────────→

  [创建]           [写入]          [封存]          [合并]          [持续]
  Active Segment  → 持续追加向量  → 达到阈值(512MB) → 合并小段   → ...
  (可写)           (可写)          (只读)           (只读)
  
  功能：            功能：          功能：            功能：
  分配 Segment     追加写入      停止写入           合并多个小段
  初始化内存缓冲区  写入 WAL      开始构建索引       成大段
  创建索引占位      后台定期刷盘   构建 HNSW 图      减少段数量
                                                     优化查询性能
```

---

# 第八部分：自己实现向量数据库

## 从零实现 Mini Vector Database

### Step 1: 实现 Vector 存储

```python
# Python 实现 - 向量数据结构和存储
from dataclasses import dataclass, field
from typing import Any
import numpy as np

@dataclass
class VectorRecord:
    id: str
    vector: np.ndarray
    metadata: dict[str, Any] = field(default_factory=dict)

class VectorStore:
    def __init__(self):
        self.records: list[VectorRecord] = []
        self.id_index: dict[str, int] = {}

    def insert(self, record: VectorRecord):
        self.id_index[record.id] = len(self.records)
        self.records.append(record)

    def get(self, id: str) -> VectorRecord | None:
        idx = self.id_index.get(id)
        return self.records[idx] if idx is not None else None

    def __len__(self) -> int:
        return len(self.records)

    def all(self) -> list[VectorRecord]:
        return self.records
```

```java
package minivdb;

import java.util.*;

// 向量数据结构
public class VectorRecord {
    private String id;
    private float[] vector;
    private Map<String, Object> metadata;

    public VectorRecord(String id, float[] vector, Map<String, Object> metadata) {
        this.id = id;
        this.vector = vector;
        this.metadata = metadata;
    }

    public String getId() { return id; }
    public float[] getVector() { return vector; }
    public Map<String, Object> getMetadata() { return metadata; }
}

// 纯内存向量存储
public class VectorStore {
    private final List<VectorRecord> records = new ArrayList<>();
    private final Map<String, Integer> idIndex = new HashMap<>();

    public void insert(VectorRecord record) {
        idIndex.put(record.getId(), records.size());
        records.add(record);
    }

    public VectorRecord get(String id) {
        Integer index = idIndex.get(id);
        if (index == null) return null;
        return records.get(index);
    }

    public int size() { return records.size(); }
    public List<VectorRecord> all() { return records; }
}
```

### Step 2: 实现向量插入

```python
# Python 实现 - 向量插入管理器
class VectorIndex:
    def __init__(self):
        self.store = VectorStore()

    def insert(self, id: str, vector: np.ndarray, metadata: dict[str, Any] | None = None):
        record = VectorRecord(id=id, vector=vector, metadata=metadata or {})
        self.store.insert(record)

# index = VectorIndex()
# index.insert("1", np.array([0.1, 0.2, 0.3]), {"category": "AI"})
```

```java
// 向量插入管理器
public class VectorIndex {
    private final VectorStore store = new VectorStore();

    public void insert(String id, float[] vector) {
        insert(id, vector, new HashMap<>());
    }

    public void insert(String id, float[] vector, Map<String, Object> metadata) {
        VectorRecord record = new VectorRecord(id, vector, metadata);
        store.insert(record);
    }
}
```

### Step 3: 实现余弦相似度

```python
# Python 实现 - 相似度计算 (numpy)
import numpy as np

class Similarity:
    @staticmethod
    def cosine(a: np.ndarray, b: np.ndarray) -> float:
        dot = float(np.dot(a, b))
        norm = float(np.linalg.norm(a) * np.linalg.norm(b))
        return dot / norm if norm != 0 else 0.0

    @staticmethod
    def normalize(v: np.ndarray) -> np.ndarray:
        norm = np.linalg.norm(v)
        return v / norm if norm != 0 else v

    @staticmethod
    def l2(a: np.ndarray, b: np.ndarray) -> float:
        return float(np.linalg.norm(a - b))
```

```java
public class Similarity {

    public static double cosine(float[] a, float[] b) {
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static float[] normalize(float[] v) {
        double norm = 0;
        for (float x : v) norm += x * x;
        norm = Math.sqrt(norm);
        float[] result = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            result[i] = (float)(v[i] / norm);
        }
        return result;
    }

    // L2 距离
    public static double l2(float[] a, float[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
```

### Step 4: 实现 Top-K 搜索

```python
# Python 实现 - Top-K 搜索 (heapq)
import heapq
from dataclasses import dataclass
from enum import Enum
from typing import Any

@dataclass
class SearchResult:
    id: str
    score: float
    metadata: dict[str, Any] | None = None

    def __lt__(self, other):
        return self.score < other.score

class Metric(Enum):
    COSINE = "cosine"
    L2 = "l2"

class Searcher:
    def __init__(self, store: VectorStore, metric: Metric = Metric.COSINE):
        self.store = store
        self.metric = metric

    def search(self, query: np.ndarray, top_k: int = 10) -> list[SearchResult]:
        heap: list[SearchResult] = []
        for record in self.store.all():
            if self.metric == Metric.COSINE:
                score = Similarity.cosine(query, record.vector)
            else:
                score = -Similarity.l2(query, record.vector)
            heapq.heappush(heap, SearchResult(record.id, score, record.metadata))
            if len(heap) > top_k:
                heapq.heappop(heap)
        results = sorted(heap, key=lambda r: r.score, reverse=True)
        return results
```

```java
public class SearchResult implements Comparable<SearchResult> {
    private String id;
    private double score;
    private Map<String, Object> metadata;

    public SearchResult(String id, double score, Map<String, Object> metadata) {
        this.id = id;
        this.score = score;
        this.metadata = metadata;
    }

    public String getId() { return id; }
    public double getScore() { return score; }
    public Map<String, Object> getMetadata() { return metadata; }

    @Override
    public int compareTo(SearchResult o) {
        return Double.compare(this.score, o.score);
    }
}

public class Searcher {
    private final VectorStore store;
    private final Metric metric;

    public enum Metric { COSINE, L2 }

    public Searcher(VectorStore store, Metric metric) {
        this.store = store;
        this.metric = metric;
    }

    public List<SearchResult> search(float[] query, int topK) {
        PriorityQueue<SearchResult> pq = new PriorityQueue<>(topK);

        for (VectorRecord record : store.all()) {
            double score;
            if (metric == Metric.COSINE) {
                score = Similarity.cosine(query, record.getVector());
            } else {
                score = -Similarity.l2(query, record.getVector()); // 负数，距离越小越好
            }

            pq.offer(new SearchResult(record.getId(), score, record.getMetadata()));
            if (pq.size() > topK) {
                pq.poll(); // 移除最小的
            }
        }

        List<SearchResult> results = new ArrayList<>(pq);
        results.sort(Comparator.reverseOrder()); // 降序
        return results;
    }
}
```

### Step 5: 实现 Metadata 过滤

```python
# Python 实现 - Metadata 过滤搜索
class FilteredSearcher:
    def __init__(self, searcher: Searcher, store: VectorStore):
        self.searcher = searcher
        self.store = store

    def search(self, query: np.ndarray, top_k: int = 10,
               filter: dict[str, Any] | None = None) -> list[SearchResult]:
        search_k = min(top_k * 10, len(self.store))
        candidates = self.searcher.search(query, search_k)
        if not filter:
            return candidates[:top_k]
        filtered = []
        for r in candidates:
            if self._matches(r.metadata, filter):
                filtered.append(r)
                if len(filtered) >= top_k:
                    break
        return filtered

    @staticmethod
    def _matches(metadata: dict[str, Any] | None, filter: dict[str, Any]) -> bool:
        if metadata is None:
            return False
        for key, value in filter.items():
            if metadata.get(key) != value:
                return False
        return True
```

```java
public class FilteredSearcher {
    private final Searcher searcher;
    private final VectorStore store;

    public FilteredSearcher(Searcher searcher, VectorStore store) {
        this.searcher = searcher;
        this.store = store;
    }

    public List<SearchResult> search(float[] query, int topK, 
                                      Map<String, Object> filter) {
        // 方案：先搜更多结果，再过滤
        int searchK = Math.min(topK * 10, store.size());
        List<SearchResult> candidates = searcher.search(query, searchK);

        List<SearchResult> filtered = new ArrayList<>();
        for (SearchResult r : candidates) {
            if (matchesFilter(r.getMetadata(), filter)) {
                filtered.add(r);
                if (filtered.size() >= topK) break;
            }
        }
        return filtered;
    }

    private boolean matchesFilter(Map<String, Object> metadata, 
                                   Map<String, Object> filter) {
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            Object value = metadata.get(entry.getKey());
            if (value == null || !value.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
}
```

### Step 6: 实现 HNSW 简化版

```python
# Python 实现 - 简化的 HNSW
import math
import random
import heapq
from typing import Any
from collections import OrderedDict

class HNSWNode:
    def __init__(self, id: str, vector: np.ndarray, level: int):
        self.id = id
        self.vector = vector
        self.level = level
        self.neighbors: list[set["HNSWNode"]] = [set() for _ in range(level + 1)]

class HNSWIndex:
    M = 16
    M_MAX = 32
    EF_CONSTRUCTION = 200
    ML = 0.5

    def __init__(self):
        self.nodes: list[HNSWNode] = []
        self.entry_point: HNSWNode | None = None
        self.max_level = 0

    def _random_level(self) -> int:
        return int(-math.log(random.random()) * self.ML)

    def insert(self, id: str, vector: np.ndarray):
        level = self._random_level()
        new_node = HNSWNode(id, vector, level)

        if self.entry_point is None:
            self.entry_point = new_node
            self.max_level = level
            self.nodes.append(new_node)
            return

        current = self.entry_point
        for l in range(self.max_level, level, -1):
            current = self._search_layer(new_node.vector, current, 1, l)[0]

        for l in range(min(level, self.max_level), -1, -1):
            candidates = self._search_layer(new_node.vector, current, self.EF_CONSTRUCTION, l)
            neighbors = self._select_neighbors(candidates, self.M)
            for neighbor in neighbors:
                new_node.neighbors[l].add(neighbor)
                neighbor.neighbors[l].add(new_node)
                if len(neighbor.neighbors[l]) > self.M_MAX:
                    self._prune_neighbors(neighbor, l)

        if level > self.max_level:
            self.max_level = level
            self.entry_point = new_node
        self.nodes.append(new_node)

    def _search_layer(self, query: np.ndarray, entry: HNSWNode,
                      ef: int, layer: int) -> list[HNSWNode]:
        visited = OrderedDict()
        candidates = []

        dist = Similarity.cosine(query, entry.vector)
        visited[entry.id] = entry
        heapq.heappush(candidates, (-dist, entry))

        while candidates:
            neg_dist, current = heapq.heappop(candidates)
            farthest_dist = Similarity.cosine(query, list(visited.values())[-1].vector)
            if -neg_dist < farthest_dist:
                break
            for neighbor in current.neighbors[layer]:
                if neighbor.id not in visited:
                    visited[neighbor.id] = neighbor
                    nd = Similarity.cosine(query, neighbor.vector)
                    if len(visited) < ef:
                        heapq.heappush(candidates, (-nd, neighbor))
                    else:
                        far_dist = Similarity.cosine(query, list(visited.values())[-1].vector)
                        if nd > far_dist:
                            heapq.heappush(candidates, (-nd, neighbor))
                            visited.popitem(last=True)
                            heapq.heappush(candidates, (-nd, neighbor))

        return list(visited.values())[:ef]

    def _select_neighbors(self, candidates: list[HNSWNode], m: int) -> list[HNSWNode]:
        return candidates[:m]

    def _prune_neighbors(self, node: HNSWNode, layer: int):
        sorted_neighbors = sorted(
            node.neighbors[layer],
            key=lambda n: Similarity.cosine(node.vector, n.vector),
            reverse=True
        )
        node.neighbors[layer] = set(sorted_neighbors[:self.M_MAX])

    def search(self, query: np.ndarray, top_k: int = 10,
               ef_search: int = 50) -> list[SearchResult]:
        if self.entry_point is None:
            return []
        current = self.entry_point
        for l in range(self.max_level, 0, -1):
            current = self._search_layer(query, current, 1, l)[0]
        candidates = self._search_layer(query, current, ef_search, 0)
        results = []
        for i in range(min(top_k, len(candidates))):
            node = candidates[i]
            score = Similarity.cosine(query, node.vector)
            results.append(SearchResult(id=node.id, score=score))
        return results
```

```java
public class HNSWNode {
    String id;
    float[] vector;
    int level;
    List<Set<HNSWNode>> neighbors;  // per level

    public HNSWNode(String id, float[] vector, int level) {
        this.id = id;
        this.vector = vector;
        this.level = level;
        this.neighbors = new ArrayList<>();
        for (int i = 0; i <= level; i++) {
            neighbors.add(new HashSet<>());
        }
    }
}

public class HNSWIndex {
    private static final int M = 16;         // 每层最大邻居数
    private static final int M_MAX = 32;     // 下层最大邻居数
    private static final int EF_CONSTRUCTION = 200;
    private static final double ML = 0.5;    // 层数系数

    private final List<HNSWNode> nodes = new ArrayList<>();
    private HNSWNode entryPoint;
    private int maxLevel = 0;

    public HNSWIndex() {
        // 初始化空的入口点
    }

    private int randomLevel() {
        double r = Math.random();
        return (int) (-Math.log(r) * ML);
    }

    public void insert(String id, float[] vector) {
        int level = randomLevel();
        HNSWNode newNode = new HNSWNode(id, vector, level);

        if (entryPoint == null) {
            entryPoint = newNode;
            maxLevel = level;
            nodes.add(newNode);
            return;
        }

        // 从最高层搜索到 level+1 层，找到入口
        HNSWNode current = entryPoint;
        for (int l = maxLevel; l > level; l--) {
            current = searchLayer(newNode.vector, current, 1, l).get(0);
        }

        // 从 level 到 0 层：搜索 + 连接
        for (int l = Math.min(level, maxLevel); l >= 0; l--) {
            List<HNSWNode> candidates = searchLayer(
                newNode.vector, current, EF_CONSTRUCTION, l);
            
            List<HNSWNode> neighbors = selectNeighbors(candidates, M);
            for (HNSWNode neighbor : neighbors) {
                newNode.neighbors.get(l).add(neighbor);
                neighbor.neighbors.get(l).add(newNode);
                
                // 如果邻居的邻居数超过 M_MAX，剪枝
                if (neighbor.neighbors.get(l).size() > M_MAX) {
                    pruneNeighbors(neighbor, l);
                }
            }
        }

        if (level > maxLevel) {
            maxLevel = level;
            entryPoint = newNode;
        }

        nodes.add(newNode);
    }

    private List<HNSWNode> searchLayer(float[] query, HNSWNode entry, 
                                        int ef, int layer) {
        // 简化的贪婪搜索
        TreeSet<HNSWNode> visited = new TreeSet<>(
            (a, b) -> Double.compare(
                Similarity.cosine(query, a.vector),
                Similarity.cosine(query, b.vector)));

        PriorityQueue<HNSWNode> candidates = new PriorityQueue<>(
            (a, b) -> Double.compare(
                Similarity.cosine(query, b.vector),
                Similarity.cosine(query, a.vector)));

        visited.add(entry);
        candidates.add(entry);

        while (!candidates.isEmpty()) {
            HNSWNode current = candidates.poll();
            HNSWNode farthest = visited.last();

            if (Similarity.cosine(query, current.vector) < 
                Similarity.cosine(query, farthest.vector)) {
                break;
            }

            for (HNSWNode neighbor : current.neighbors.get(layer)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    if (visited.size() < ef) {
                        candidates.add(neighbor);
                    } else {
                        HNSWNode far = visited.last();
                        if (Similarity.cosine(query, neighbor.vector) > 
                            Similarity.cosine(query, far.vector)) {
                            candidates.add(neighbor);
                            visited.pollLast();
                            candidates.add(neighbor);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(visited).subList(
            0, Math.min(ef, visited.size()));
    }

    private List<HNSWNode> selectNeighbors(List<HNSWNode> candidates, int m) {
        // 简单选择：取距离最近的 m 个
        return candidates.stream()
            .limit(m)
            .collect(Collectors.toList());
    }

    private void pruneNeighbors(HNSWNode node, int layer) {
        // 保留最近的 M_MAX 个邻居
        List<HNSWNode> sorted = new ArrayList<>(node.neighbors.get(layer));
        sorted.sort((a, b) -> Double.compare(
            Similarity.cosine(node.vector, b.vector),
            Similarity.cosine(node.vector, a.vector)));
        
        Set<HNSWNode> pruned = new HashSet<>();
        for (int i = 0; i < Math.min(M_MAX, sorted.size()); i++) {
            pruned.add(sorted.get(i));
        }
        node.neighbors.set(layer, pruned);
    }

    public List<SearchResult> search(float[] query, int topK, int efSearch) {
        if (entryPoint == null) return Collections.emptyList();

        HNSWNode current = entryPoint;
        // 从最高层搜索到第 0 层
        for (int l = maxLevel; l > 0; l--) {
            current = searchLayer(query, current, 1, l).get(0);
        }

        // 在第 0 层搜索得到结果
        List<HNSWNode> candidates = searchLayer(query, current, efSearch, 0);
        
        // 取 Top-K
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, candidates.size()); i++) {
            HNSWNode node = candidates.get(i);
            double score = Similarity.cosine(query, node.vector);
            results.add(new SearchResult(node.id, score, null));
        }
        return results;
    }
}
```

### Step 7: 实现 REST API

```python
# Python 实现 - FastAPI REST API
# pip install fastapi uvicorn numpy
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import numpy as np
import uvicorn

app = FastAPI(title="Mini Vector DB")
index = HNSWIndex()

class InsertRequest(BaseModel):
    id: str
    vector: list[float]

class SearchRequest(BaseModel):
    vector: list[float]
    top_k: int = 10
    ef_search: int | None = 50

@app.post("/api/v1/vectors")
def insert(req: InsertRequest):
    index.insert(req.id, np.array(req.vector, dtype=np.float32))
    return {"status": "ok"}

@app.post("/api/v1/vectors/search")
def search(req: SearchRequest):
    query = np.array(req.vector, dtype=np.float32)
    results = index.search(query, req.top_k, req.ef_search or 50)
    return {"results": [{"id": r.id, "score": r.score} for r in results]}

@app.get("/api/v1/vectors/{id}")
def get_vector(id: str):
    record = index.store.get(id) if hasattr(index, 'store') else None
    if record is None:
        raise HTTPException(status_code=404, detail="Not found")
    return {"id": record.id, "vector": record.vector.tolist()}

# 启动: uvicorn.run(app, host="0.0.0.0", port=8000)
```

```java
// Spring Boot REST Controller
@RestController
@RestController
@RequestMapping("/api/v1/vectors")
public class VectorController {

    private final HNSWIndex index = new HNSWIndex();

    @PostMapping
    public ResponseEntity<Void> insert(
            @RequestBody InsertRequest request) {
        index.insert(request.getId(), request.getVector());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/search")
    public ResponseEntity<List<SearchResult>> search(
            @RequestBody SearchRequest request) {
        List<SearchResult> results = index.search(
            request.getVector(), 
            request.getTopK(), 
            request.getEfSearch() != null ? request.getEfSearch() : 50);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VectorRecord> get(@PathVariable String id) {
        // ... 根据 ID 获取
        return ResponseEntity.ok(record);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        // ... 标记删除
        return ResponseEntity.noContent().build();
    }

    static class InsertRequest {
        private String id;
        private float[] vector;
        // getters/setters
    }

    static class SearchRequest {
        private float[] vector;
        private int topK;
        private Integer efSearch;
        // getters/setters
    }
}
```

### Step 8: 实现 Python SDK

```python
# Python 实现 - 客户端 SDK (requests)
import requests
from typing import Any

class MiniVectorDBClient:
    def __init__(self, base_url: str = "http://localhost:8000"):
        self.base_url = base_url.rstrip("/")

    def insert(self, collection: str, id: str, vector: list[float]):
        url = f"{self.base_url}/api/v1/collections/{collection}/vectors"
        requests.post(url, json={"id": id, "vector": vector})

    def search(self, collection: str, query: list[float],
               top_k: int = 10) -> list[dict[str, Any]]:
        url = f"{self.base_url}/api/v1/collections/{collection}/search"
        resp = requests.post(url, json={"vector": query, "top_k": top_k})
        return resp.json().get("results", [])

    def delete(self, collection: str, id: str):
        url = f"{self.base_url}/api/v1/collections/{collection}/vectors/{id}"
        requests.delete(url)

# client = MiniVectorDBClient()
# client.insert("docs", "1", [0.1, 0.2, 0.3])
# results = client.search("docs", [0.1, 0.2, 0.3])
```

### Step 9: 实现 Java SDK

```java
// 客户端 SDK
public class MiniVectorDBClient {
    private final String baseUrl;
    private final RestTemplate restTemplate;

    public MiniVectorDBClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    public void insert(String collection, String id, float[] vector) {
        String url = baseUrl + "/api/v1/collections/" + collection + "/vectors";
        Map<String, Object> body = new HashMap<>();
        body.put("id", id);
        body.put("vector", vector);
        restTemplate.postForEntity(url, body, Void.class);
    }

    public List<SearchResult> search(String collection, float[] query, int topK) {
        String url = baseUrl + "/api/v1/collections/" + collection + "/search";
        Map<String, Object> body = new HashMap<>();
        body.put("vector", query);
        body.put("topK", topK);
        
        ResponseEntity<SearchResult[]> response = restTemplate.postForEntity(
            url, body, SearchResult[].class);
        return Arrays.asList(response.getBody());
    }

    public void delete(String collection, String id) {
        String url = baseUrl + "/api/v1/collections/" + collection + "/vectors/" + id;
        restTemplate.delete(url);
    }
}
```

---

# 第九部分：开源项目分析

## FAISS (Facebook AI Similarity Search)

### 一句话本质
FAISS 是 Facebook AI 开源的**向量索引库**（不是数据库），提供最高效的 ANN 算法实现，专注于**单机内存中的向量搜索**。

### 项目结构

```ascii
faiss/
├── faiss/                    # 核心 C++ 代码
│   ├── Index.h               # 所有索引的基类
│   ├── IndexFlat.cpp         # Flat (暴力搜索) 索引
│   ├── IndexIVF.cpp          # IVF 索引
│   ├── IndexHNSW.cpp         # HNSW 索引
│   ├── IndexPQ.cpp           # PQ 量化索引
│   ├── IndexLSH.cpp          # LSH 索引
│   ├── IndexBinary.cpp       # 二进制向量索引
│   ├── Clustering.cpp        # K-Means 聚类 (IVF 用)
│   ├── utils/                # 工具类
│   │   ├── distances.cpp     # 距离计算 (SIMD 优化)
│   │   ├── random.cpp        # 随机数
│   │   └── ...
│   ├── impl/                 # 底层实现
│   │   ├── HNSW.h            # HNSW 核心图结构
│   │   ├── ProductQuantizer.h# PQ 量化器
│   │   └── ...
│   └── python/               # Python 绑定
├── tests/                    # 测试
├── benchs/                   # 基准测试
└── cmake/                    # CMake 构建
```

### 核心调用链

```ascii
Java 程序
    │
    ├── JNI 调用 (faiss-jni)
    │   │
    │   ▼
    ├── Index::search()
    │   │
    │   ▼
    ├── IndexHNSW::search()
    │   │
    │   ├── hnsw.search()      ← HNSW 图搜索
    │   ├── distance_computer  ← 距离计算
    │   │   │
    │   │   └── fvec_L2sqr()  ← SSE/AVX 优化的 L2 距离
    │   │
    │   └── collect_results()
    │
    └── 返回 Top-K 结果
```

### 核心源码分析

```cpp
// IndexHNSW::search 简化源码 (faiss/IndexHNSW.cpp)
void IndexHNSW::search(idx_t n, const float* x, idx_t k,
                        float* distances, idx_t* labels,
                        const SearchParameters* params) const {
    
    const HNSW& hnsw = *this->hnsw;
    int efSearch = params ? params->efSearch : hnsw.efSearch;
    
    // 对每个查询向量
    for (idx_t i = 0; i < n; i++) {
        const float* q = x + i * d;
        
        // 调用 HNSW 搜索
        hnsw.search(q, k, labels + i * k, distances + i * k,
                     efSearch, storage->get_data(), d);
    }
}

// 距离计算 (FAISS 用 SSE/AVX 指令集优化)
// faiss/utils/distances.cpp
float fvec_L2sqr(const float* a, const float* b, size_t d) {
    __m256 sum = _mm256_setzero_ps();
    for (size_t i = 0; i < d; i += 8) {
        __m256 va = _mm256_loadu_ps(a + i);
        __m256 vb = _mm256_loadu_ps(b + i);
        __m256 diff = _mm256_sub_ps(va, vb);
        sum = _mm256_add_ps(sum, _mm256_mul_ps(diff, diff));
    }
    // 水平求和
    // ...
    return result;
}
```

## Milvus

### 一句话本质
Milvus 是**云原生分布式向量数据库**，采用存算分离架构，支持万亿级向量规模，是生产环境最常用的开源向量数据库。

### 项目结构

```ascii
milvus/
├── internal/                  # 核心逻辑 (Go)
│   ├── proxy/                 # 代理层：请求路由、鉴权
│   │   ├── impl/              # 接口实现
│   │   └── task/              # 任务调度
│   ├── querynode/             # 查询节点：执行搜索
│   │   ├── segment/           # 段管理
│   │   ├── index/             # 索引管理
│   │   └── search/            # 搜索执行
│   ├── datanode/              # 数据节点：持久化
│   │   ├── allocator/         # ID 分配
│   │   ├── flush/             # 刷盘管理
│   │   └── compaction/        # 段合并
│   ├── indexnode/             # 索引节点：构建索引
│   │   ├── index/             # 索引构建 (CGo 调用 FAISS)
│   │   └── storage/           # 索引文件存储
│   ├── rootcoord/             # Root 协调节点
│   ├── datacoord/             # 数据协调节点
│   └── querycoord/            # 查询协调节点
├── pkg/                       # 公共包
│   ├── mq/                    # 消息队列 (Pulsar/Kafka)
│   └── storage/               # 对象存储接口
└── milvus-java-sdk/           # Java SDK
```

### 核心调用链

```ascii
Java Client                           Milvus 集群
    │                                      │
    ├── insert() ───── gRPC ──────→ Proxy
    │                                      │
    │                               ┌──────┴──────┐
    │                               │  RootCoord   │
    │                               │  (表结构管理) │
    │                               └──────┬──────┘
    │                                      │
    │                               ┌──────┴──────┐
    │                               │  DataCoord   │
    │                               │  (数据调度)   │
    │                               └──────┬──────┘
    │                                      │
    │                               ┌──────┴──────┐
    │                               │  DataNode    │
    │                               │  (写 WAL +   │
    │                               │   存 Segment)│
    │                               └─────────────┘
    │                                      │
    ├── search() ───── gRPC ──────→ Proxy
    │                                      │
    │                               ┌──────┴──────┐
    │                               │  QueryCoord  │
    │                               │  (查询调度)   │
    │                               └──────┬──────┘
    │                                      │
    │                               ┌──────┴──────┐
    │                               │  QueryNode   │
    │                               │  (HNSW 搜索) │
    │                               └─────────────┘
```

## Qdrant

### 一句话本质
Qdrant 是**Rust 实现的轻量级向量数据库**，单机性能极强，API 设计简洁，适合中小规模生产部署。

### 项目结构

```ascii
qdrant/
├── src/
│   ├── main.rs                 # 入口
│   ├── config.rs               # 配置
│   ├── api/                    # API 层
│   │   ├── rest.rs             # REST API (Actix-web)
│   │   └── grpc.rs             # gRPC API
│   ├── collection/             # 集合管理
│   │   ├── collection.rs       # 集合 CRUD
│   │   ├── shard.rs            # 分片管理
│   │   └── segment/            # 段管理
│   │       ├── segment.rs      # 段结构
│   │       ├── segment_constructor.rs
│   │       ├── vector_storage/ # 向量存储
│   │       └── payload_storage/# Payload 存储 (RocksDB)
│   ├── search/                 # 搜索
│   │   ├── hnsw.rs             # HNSW 搜索
│   │   └── filter.rs           # 过滤
│   ├── common/                 # 公共
│   └── wal/                    # Write-Ahead Log
├── lib/                        # 客户端库
│   ├── qdrant-client/          # Rust 客户端
│   └── java/                   # Java 客户端
└── tests/                      # 测试
```

## Weaviate

### 一句话本质
Weaviate 是**AI 原生向量搜索引擎**，内置 Embedding 模块和知识图谱能力，强调"开箱即用"。

### 独特特性

```ascii
Weaviate 与传统向量数据库的关键区别：

1. 内置 Embedding 模块
   你只需要把原始文本发给 Weaviate，它自动调用
   内置的 Embedding 模型生成向量

2. GraphQL API
   查询用 GraphQL，天然支持复杂嵌套查询

3. 多模态
   文本、图片都可以直接存入，Weaviate 自动向量化

4. 知识图谱整合
   支持实体之间的关系 (类似图数据库)
```

## Chroma

### 一句话本质
Chroma 是**最 Pythonic 的向量数据库**，API 设计极简，适合快速原型开发和小规模应用。

### 为什么受欢迎？

```python
# Chroma 的 API 设计
# 3 行代码就可以用！

import chromadb

client = chromadb.Client()
collection = client.create_collection("docs")
collection.add(
    documents=["ChatGPT is AI", "Python is cool"],
    ids=["doc1", "doc2"]
)

results = collection.query(
    query_texts=["AI assistant"],
    n_results=2
)
```

## pgvector

### 一句话本质
pgvector 是 **PostgreSQL 的向量扩展**，让 PostgreSQL 支持向量类型和 ANN 搜索，适合已有 PostgreSQL 基础设施的团队。

### 架构

```ascii
pgvector 的工作方式：

PostgreSQL
    │
    ├── 自定义类型: vector
    │   CREATE TABLE docs (
    │       id SERIAL PRIMARY KEY,
    │       content TEXT,
    │       embedding vector(1536)  ← 新类型
    │   );
    │
    ├── 自定义索引: ivfflat / hnsw
    │   CREATE INDEX ON docs 
    │   USING hnsw (embedding vector_cosine_ops);
    │
    └── 自定义操作符: <=>
        SELECT * FROM docs
        ORDER BY embedding <=> '[0.1, 0.2, ...]'  ← 余弦距离
        LIMIT 10;

为什么用 pgvector？
  1. 不需要部署新系统
  2. PostgreSQL 的事务能力
  3. 一条 SQL 同时做向量搜索和传统查询
  4. 缺点：分布式能力弱，大规模性能不如专用 DB
```

---

# 第十部分：向量数据库实际应用

## RAG (检索增强生成)

### 一句话本质
RAG 通过**先检索再生成**的方式，让 LLM 基于检索到的私有知识回答，解决幻觉和知识截止问题。

### 架构图

```ascii
┌──────────────────────────────────────────────────────────┐
│                     RAG 系统架构                          │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─────────┐    ┌──────────┐    ┌──────────┐            │
│  │ 用户输入 │───→│Embedding │───→│向量数据库 │            │
│  │ "什么    │    │ 模型     │    │          │            │
│  │ 是RAG?"  │    └──────────┘    └─────┬────┘            │
│  └─────────┘                           │ Top-K 相关文档   │
│                                        ▼                 │
│                               ┌────────────────┐         │
│                               │    Prompt       │         │
│                               │    构造器       │         │
│                               │                 │         │
│                               │ 用户问题: ...   │         │
│                               │ 上下文: ...     │         │
│                               └───────┬────────┘         │
│                                       │                   │
│                                       ▼                   │
│                               ┌────────────────┐         │
│                               │    LLM         │         │
│                               │  (GPT-4/Claude)│         │
│                               └───────┬────────┘         │
│                                       │                   │
│                                       ▼                   │
│                               ┌────────────────┐         │
│                               │  最终回答      │         │
│                               │  "RAG指的是...  │         │
│                               └────────────────┘         │
└──────────────────────────────────────────────────────────┘
```

### 数据流

```ascii
数据预处理 (Indexing) 流程：

原始文档 (PDF/Word/Markdown)
    │
    ▼
文档解析 (Unstructured.io / LangChain Loaders)
    │
    ▼
文本分块 (Chunking)
    ├── 按段落分: 每段 512 tokens
    ├── 重叠 64 tokens (保持上下文连贯)
    └── 每个 Chunk 保留元数据 (来源、页码等)
    │
    ▼
Embedding (text-embedding-ada-002)
    │
    ▼
存入向量数据库
    └── id: chunk_hash
        vector: [0.123, ...]
        metadata: {source: "doc.pdf", page: 5}
        text: "原始文本..."

查询流程：

用户提问 → Embedding → 向量搜索 Top-5 → 构造 Prompt → LLM → 回答
     ↓                                        ↓
  "什么是RAG?"                          Prompt 模板:
                                        问题: {question}
                                        基于以下内容回答:
                                        {context}
     ↓
   [0.456, -0.123, ...]                  最终回答
```

```python
# Python 实现 - RAG 系统 (LangChain)
# pip install langchain langchain-community langchain-openai chromadb

from langchain_community.document_loaders import TextLoader, DirectoryLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import Chroma
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain.chains import RetrievalQA
from langchain.prompts import PromptTemplate

# ---- 索引阶段 (Indexing) ----
loader = DirectoryLoader("./docs/", glob="**/*.md", loader_cls=TextLoader)
documents = loader.load()

text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=512,
    chunk_overlap=64,
    separators=["\n## ", "\n### ", "\n", ". ", " "],
)
chunks = text_splitter.split_documents(documents)

vectorstore = Chroma.from_documents(
    documents=chunks,
    embedding=OpenAIEmbeddings(model="text-embedding-ada-002"),
    persist_directory="./chroma_db",
)
vectorstore.persist()

# ---- 查询阶段 (Retrieval + Generation) ----
llm = ChatOpenAI(model="gpt-4", temperature=0)

prompt_template = PromptTemplate(
    template="""基于以下上下文，回答用户问题。
如果上下文不足以回答问题，请说"我不知道"。

上下文：
{context}

问题：{question}

回答：""",
    input_variables=["context", "question"],
)

qa_chain = RetrievalQA.from_chain_type(
    llm=llm,
    retriever=vectorstore.as_retriever(search_kwargs={"k": 5}),
    chain_type="stuff",
    chain_type_kwargs={"prompt": prompt_template},
)

# 使用
# answer = qa_chain.invoke("什么是RAG?")
# print(answer["result"])
```

## 知识库

### 业务场景

```ascii
企业知识库搜索：

场景：员工搜索公司内部知识

之前：用 ES 搜索，必须知道关键词
  "离职流程" → 搜到
  "怎么辞职" → 搜不到 (语义不同)

之后：用向量数据库
  "怎么辞职" → Embedding → 语义匹配 "离职流程"
  "年假多少天" → 匹配 "带薪年假政策"

架构：
    企业员工
        │
        ├── 企业内部知识库 (Wiki, 文档, 代码)
        │       │
        │       ├── 文档解析 → Chunking → Embedding
        │       │
        │       └── 向量数据库 (索引)
        │
        └── 搜索入口
                │
                ├── 自然语言提问
                ├── 精确关键词搜索 (Hybrid)
                └── 搜索 → Rerank → 结果展示
```

## 客服机器人

### 业务场景

```ascii
智能客服系统：

用户: "我的订单还没到怎么办？"

流程:
  1. 用户问题 → Embedding → [0.123, ...]
  2. 向量搜索 → 找到相关 FAQ:
     - "订单延迟怎么办" (相似度 0.95)
     - "物流查询" (相似度 0.88)
     - "退货流程" (相似度 0.45)
  3. 将 Top-3 FAQ 作为上下文
  4. LLM 生成回答

优点:
  - 覆盖全量 FAQ (百万级)
  - 理解同义问法
  - 持续学习 (新 FAQ 直接入库)
  - 多语言支持 (Embedding 跨语言)
```

## 代码搜索

### 业务场景

```ascii
代码语义搜索：

搜索: "如何实现用户登录的 JWT 验证"

传统方式：grep/keyword
  → 搜 "JWT" → 找到用了 JWT 库的所有文件
  → 但可能漏掉叫 "token" 或 "auth" 的实现

向量搜索方式：
  → Embedding 理解代码语义
  → 找到在逻辑上实现"登录验证"的代码
  → 不管它叫什么名字

实现：
  CodeBERT / GraphCodeBERT 将代码转为向量
    ↓
  代码函数级 Embedding
    ↓
  存入向量数据库
    ↓
  搜索 "JWT 登录验证"
    → 找到 LoginController.java
    → 找到 SecurityConfig.java
    → 找到 JwtTokenProvider.java
```

## 图片搜索

```ascii
图片搜索架构：

1. 图片入库
  图片 → 视觉模型 (ResNet/ViT/CLIP) → 图片向量 → 向量数据库

2. 搜索
  文本搜索: "红色的汽车" → CLIP 文本编码器 → 文本向量
            → 向量搜索 → 匹配图片向量
            → 返回红色汽车图片
  
  图片搜索: 上传图片 → 图片向量 → 搜相似图片

3. 多模态
  文本 + 图片的向量在同一个空间
  所以 "一只狗" 的文字向量和狗图片的向量距离很近

应用场景：
  - 电商：搜"黑色连衣裙" → 返回商品
  - 设计：搜"极简风格LOGO" → 返回参考图
  - 安全：搜相似人脸 → 人脸识别
```

## 推荐系统

```ascii
推荐系统架构 (向量化召回)：

用户行为序列
    │
    ▼
用户 Embedding (双塔模型)
    │ user_vec = [0.123, ...]
    ▼
向量数据库 (物品向量库)
    │ 搜索 Top-100 最相似物品
    ▼
粗排 (简单规则/模型过滤)
    │ Top-100 → Top-20
    ▼
精排 (DeepFM/DIN 等复杂模型)
    │ Top-20 → Top-10
    ▼
用户最终看到推荐

对比传统方式：
  传统: 协同过滤 + 物品协同 → 计算量大，冷启动难
  向量: 双塔模型 + ANN 搜索 → 毫秒级，泛化能力强
```

---

# 第十一部分：优点与局限

## 优点：为什么 AI 必须用向量数据库？

```ascii
核心优势：

1. 语义理解 (对比 ES)
   ES: 关键词匹配 → 搜"汽车"不返回"车辆"
   VDB: 语义匹配 → 搜"汽车"也找到"车辆"、"轿车"

2. 毫秒级搜索 (对比暴力搜索)
   暴力: 1000 万向量 × 6KB/向量 = 几秒
   VDB: 1000 万向量 × HNSW = 几毫秒

3. 亿级扩展 (对比单机内存)
   单机: 100 万向量 ≈ 6GB → 1 亿 ≈ 600GB → 不可行
   VDB: 分布式分片 → 线性扩展

4. 多模态支持 (对比传统数据库)
   MySQL: 只存结构化文本和数字
   VDB: 文本、图片、音视频 → 统一向量空间

5. RAG 基础设施
   没有向量数据库，RAG 只能暴力搜索
   → 性能差、精度低、不可扩展
```

## 局限：为什么不能替代 MySQL/Redis/ES？

### 为什么不能替代 MySQL？

```ascii
MySQL 能做但向量数据库不能做的事：

1. 事务 (ACID)
   BEGIN TRANSACTION;
   UPDATE accounts SET balance = balance - 100 WHERE id = 1;
   UPDATE accounts SET balance = balance + 100 WHERE id = 2;
   COMMIT;
   → 向量数据库大多数不支持事务

2. 复杂 JOIN 查询
   SELECT * FROM orders o
   JOIN users u ON o.user_id = u.id
   JOIN products p ON o.product_id = p.id
   WHERE u.age > 18 AND p.category = 'electronics';
   → 向量数据库不支持 JOIN

3. 聚合查询
   SELECT category, COUNT(*), AVG(price)
   FROM products
   GROUP BY category
   HAVING COUNT(*) > 10;
   → 向量数据库不适合做 OLAP 聚合

4. 精确查询
   SELECT * FROM users WHERE email = 'alice@example.com';
   → 向量数据库可以做但效率不如 MySQL B+Tree

结论：向量数据库不是替代 MySQL 的，是补充。
      MySQL 存业务数据，向量数据库存语义向量。
```

### 为什么不能替代 Redis？

```ascii
Redis 能做但向量数据库不能做的事：

1. 缓存 (微秒级)
   Redis: GET key → <1ms
   VDB:   SEARCH vector → 10-100ms
   
2. 计数器
   Redis: INCR page_view → 原子自增
   VDB:   不支持

3. 分布式锁
   Redis: SETNX lock → 分布式锁
   VDB:   不支持

4. 消息队列
   Redis: LPUSH/RPOP → 队列
   VDB:   不支持

5. 排行榜
   Redis: ZADD leaderboard score member → 有序集合
   VDB:   不支持

结论：Redis 是缓存/消息/计数器，不是给数据建索引的。
      向量数据库不能做缓存层。
```

### 为什么不能替代 Elasticsearch？

```ascii
Elasticsearch 能做但向量数据库不能做的事：

1. 复杂文本分析
   ES: 分词器、同义词、词干提取、拼写纠错
   VDB: 不做文本分析 (只处理向量)

2. 聚合分析 (OLAP)
   ES: 按时间聚合、按字段统计、直方图
   VDB: 大多不支持

3. 精确关键词搜索
   ES: "搜索"匹配"搜索"，不匹配"搜寻"
   VDB: 可能把"搜寻"排前面如果语义更相关

4. 日志分析
   ES: 日志索引+搜索+聚合是核心场景
   VDB: 不适合

最佳实践：Hybrid Search
   ES + 向量数据库 一起用：
   ES: 精确关键词 + 全文搜索 + 聚合
   VDB: 语义搜索 + 向量检索
   RRF: 合并排序
```

## 总结：技术选型对比

```ascii
                    MySQL         Redis          ES         向量数据库
                    ─────         ─────         ──         ────────
数据模型          行式           KV            文档        向量
查询方式          精确匹配       精确匹配       关键词       语义相似
索引结构          B+Tree         Hash Table    倒排索引     HNSW/IVF
延迟              毫秒           <1ms          10-100ms    10-100ms
事务              ✅             ❌            ❌          ❌
JOIN              ✅             ❌            ❌          ❌
分词搜索           ❌             ❌            ✅          ❌
语义搜索           ❌             ❌            ❌          ✅
缓存               ❌             ✅            ❌          ❌
持久化            ✅             ❌(可选)       ✅          ✅
分布式            分库分表        Cluster       天然支持    ✅
大规模向量搜索    ❌             ❌            ❌(勉强)     ✅

RAG 架构中的角色:
  MySQL:   存储业务数据 (用户、订单等)
  Redis:   缓存 LLM 响应
  ES:      精确关键词搜索
  向量DB:  语义检索 (RAG 核心)
```

---

## 面试题汇总

### 基础篇

1. **向量数据库和传统数据库的核心区别是什么？**
   > 数据模型：传统数据库存行/KV/文档，靠精确匹配；向量库存向量，靠语义相似度。索引结构：B+Tree/Hash vs HNSW/IVF。查询方式：WHERE/LIKE vs ANN/Top-K。

2. **为什么 MySQL 的 B+Tree 不能用于向量检索？**
   > B+Tree 依赖全序关系（比较大小），向量空间无序，只有距离关系。B+Tree 无法高效回答"离这个最接近的 K 个"问题。

3. **向量数据库的召回率是什么意思？**
   > 召回率 = ANN 搜索找到的真正最近邻数量 / 暴力搜索的真正最近邻数量 × 100%。HNSW 通常可以达到 95%+ 召回率。

### 原理篇

4. **HNSW 和 IVF 的核心区别是什么？**
   > IVF 基于聚类（粗粒度分区），有边界问题。HNSW 基于多层图结构，高层快速定位，低层精细搜索，没有边界问题。HNSW 召回率通常高于 IVF。

5. **PQ 量化是怎么压缩向量的？**
   > 将高维向量分成 M 段，每段用 K-Means 聚类成 256 个中心，用 1 个 byte 表示属于哪个中心。原始 1536×4=6KB 可压缩到 96 bytes。

6. **为什么说向量数据库是"近似"搜索不是"精确"搜索？**
   > 为了速度，索引算法（HNSW/IVF/PQ）牺牲了一定精度。HNSW 可能漏掉真正的最近邻，IVF 的簇边界问题，PQ 的量化误差。只有 Flat 是精确的。

### RAG 篇

7. **RAG 系统中向量数据库扮演什么角色？**
   > 外部知识存储和检索器。将私有数据 Embedding 后存入，查询时搜索最相关文档，作为 LLM 的上下文，解决幻觉和知识截止问题。

8. **向量数据库的精度如何影响 RAG 效果？**
   > 如果向量搜索召回率低，LLM 得不到相关上下文，可能产生幻觉。召回率从 90% 降到 70%，RAG 答案准确率可能从 85% 降到 60%。

### 实践篇

9. **在 Spring Boot 项目中如何接入向量数据库？**
   > 添加 SDK 依赖（milvus-sdk-java / qdrant-java-client），配置连接参数（host、port、API key），注入客户端 Bean，调用 insert/search 方法。

10. **向量数据库和 Elasticsearch 什么时候一起用？**
    > 需要 Hybrid Search 时。ES 做精确关键词匹配，向量数据库做语义匹配，两路结果用 RRF 合并。如企业搜索：产品名用 ES，描述性查询用向量搜索。

11. **如何选择 Embedding 模型？**
    > 看场景：中文文本用 text2vec-base-chinese 或 BAAI/bge-large-zh；英文用 OpenAI text-embedding-ada-002；代码用 CodeBERT；多模态用 CLIP。

12. **向量数据库中 Metadata Filter 如何优化？**
    > 1) 在常用过滤字段上建 Bitmap 索引；2) Pre-Filter 优于 Post-Filter；3) 将高选择性的过滤条件下推到 Segment 层面。

---

> **下一步**
>
> 输入 `继续` 进入下一章节源码分析
> 输入 `源码` 进入源码分析模式
> 输入 `手写` 开始实战实现 Mini Vector Database
