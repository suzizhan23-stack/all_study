# Math / 数学知识库

按学段分类，矢量图与文档分层存放，方便后续扩展。

## 目录结构

```
math/
├── README.md                              ← 本文件：目录索引
│
├── junior/                                ← 初中数学（7~9年级）
│   ├── 初中数学知识大全.md                 ← 主文档（含 17 个嵌入式 SVG 图）
│   └── diagrams/                          ← 手绘矢量图 (.excalidraw)
│       ├── 勾股定理可视化.excalidraw
│       ├── 四边形家族关系.excalidraw
│       ├── 圆的基本性质.excalidraw
│       └── 三种函数图像对比.excalidraw
│
├── senior/                                ← 高中数学
│   ├── 高中数学知识大全.md                 ← 主文档（含 12 个嵌入式 SVG 图）
│   └── diagrams/                          ← 高中矢量图
│       ├── 幂指对函数对比.excalidraw
│       ├── 圆锥曲线对比.excalidraw
│       └── 导数几何意义.excalidraw
│
└── advanced/                              ← 高等数学（预留）
    └── diagrams/                          ← 高数矢量图
```

## 使用说明

| 文件类型 | 说明 |
|----------|------|
| `.md` | 知识主文档，Markdown 格式，内嵌 SVG 矢量图 |
| `.excalidraw` | 手绘风格矢量图，拖到 [excalidraw.com](https://excalidraw.com) 即可查看/编辑 |

## 命名规范

- 文档：`{学段}_{主题}_Guide.md`
- 矢量图：`{主题名称}.excalidraw`
- 目录：`junior/`（初中）、`senior/`（高中）、`advanced/`（高数）
