# Prompt: 生成 CET-4/6 动词学习文档

从 `CET_4+6_edited.txt` 词表中提取**指定字母开头的所有动词**，生成一份完整的中英双语学习文档。以 A 字母为例，最终产出为 `a_verbs_CET46.md`（14234 行，~600KB，148 动词）。

---

## 一、文档整体结构

```
1. 文档头（标题、描述、CSS 样式）
2. 目录（HTML 表格，2 列，每列显示：序号 + 可点击动词名 + 全量搭配预览）
3. 动词详情（每个动词一个章节）
4. 参考来源
```

---

## 二、CSS 样式（放在文档头）

```html
<style>
body, p, li, td, small { font-size: 18px !important; line-height: 1.8; }
h1 { font-size: 32px !important; }
h2 { font-size: 26px !important; }
h3 { font-size: 20px !important; }
details summary { font-size: 18px !important; font-weight: bold; }
code, pre { font-size: 16px !important; }
</style>
```

---

## 三、目录格式

```html
## 📖 Table of Contents

<table>
<tr><td><b>1.</b> <a href="#1-verb">verb</a><br><small>colloc1, colloc2, colloc3, colloc4, colloc5...</small></td><td><b>2.</b> <a href="#2-verb">verb</a><br><small>colloc1, colloc2, colloc3, colloc4, colloc5...</small></td></tr>
（每行 2 个动词，2 列）
</table>

<h2>Verb Details</h2>
```

- 目录和第一个动词之间有一个 `<h2>Verb Details</h2>` 分隔
- 每个条目显示：`N. [verb](#N-verb)` → ALL collocations 拼接（不限数量）
- collocations 来自 **General Collocations (English)** 和 **Preposition Patterns (English)** 两个子节的全部条目
- 用逗号 `, ` 拼接所有搭配，不限制数量
- `N` 必须与动词的锚点 ID 一致

---

## 四、每个动词的章节格式

严格按照以下格式，**不得增减字段、不得改变顺序**：

```markdown
### N. verb <a id="N-verb"></a>

**Etymology:** [English etymology, 1-2 sentences]

**词源：** [中文词源翻译]

**Definitions:**

- EN: [English definition]
- CN: [中文释义]

**Part of Speech:** [vt./vi./both/linking v.]

**Usage Notes:**

- [English note] — [中文说明]
- [English note] — [中文说明]

**Common Collocations/Phrases:**

**General Collocations (English):**

- [colloc]
- [colloc]

<details>
<summary><b>General Collocations (中文)</b></summary>

- [中文翻译]
- [中文翻译]
</details>

**Preposition Patterns (English):**

- [pattern]
- [pattern]

<details>
<summary><b>Preposition Patterns (中文)</b></summary>

- [中文翻译]
- [中文翻译]
</details>

<details open>
<summary>📖 EN Example Sentences</summary>

- CET-4/6: [English sentence] (CET-4 YYYY-MM)
- 考研: [English sentence] (考研英语 YYYY)
- TOEFL/IELTS: [English sentence] (TOEFL TPO XX)
- Academic: [English sentence] (Nature/YYYY)
- Common: [English sentence]
- 介词搭配: [English sentence]
- 介词搭配: [English sentence]
（介词搭配例句数量必须与 Preposition Patterns (English) 条目数完全一致）
</details>

<details style="font-size: 13px !important">
<summary style="font-size: 16px; font-weight: bold">📖 例句翻译 (Chinese)</summary>

- [中文翻译1]
- [中文翻译2]
（与 EN Example Sentences 一一对应，数量完全一致）
</details>

[↑ Back to Top](#top)

---
```

---

## 五、内容要求

### 1. 词源 (Etymology)
- 1-2 句话说明来源语种、原始词形、字面含义
- 英文行和 `**词源：**` 中文行之间**必须有一个空行**

### 2. 释义 (Definitions)
- EN: 英文释义
- CN: 中文释义（分号分隔多个义项）

### 3. 词性 (Part of Speech)
- vt. (及物), vi. (不及物), vt./vi., linking v. 等

### 4. 使用说明 (Usage Notes)
- 每条英文后接 `— 中文翻译`（注意：是 em dash `—`，不是连字符 `-`）
- 重点：语法句型、常见错误、语域提示、易混词辨析

### 5. 一般搭配 (General Collocations)
- 不含介词的固定搭配（副词+动词、名词+动词组合）
- EN 先列出，CN 用 `<details>` 包裹（默认折叠）
- 约 8-15 条，EN/CN 条目一一对应

### 6. 介词搭配 (Preposition Patterns)
- 动词后接介词的常见模式（to, for, of, with, from, in, on, at, about, against, into, through, toward, upon, by, as, out of 等）
- **每个模式单独列一条**（如 abide by the rules, abide by the law, abide by a decision 各算一条）
- EN 先列出，CN 用 `<details>` 包裹（默认折叠）
- EN/CN 条目一一对应
- **必须全面**：列出该动词所有高频介词搭配

### 7. 例句 (Examples)

**标准例句（5 条）：**
- CET-4/6: 标注具体年份（如 CET-4 2021-06）
- 考研: 标注年份（如 考研英语 2020）
- TOEFL/IELTS: 标注来源（如 TOEFL TPO 54, IELTS Cambridge 16）
- Academic: 标注期刊（如 Nature 2021, Science 2020, The Lancet 2022）
- Common: 日常表达

**介词搭配例句（N 条）：**
- 数量必须与 **Preposition Patterns (English)** 条目数**完全一致**
- 每条针对一个特定的介词模式
- 标注为 `- 介词搭配: ...`

**EN/CN 分离：**
- 所有英文例句放在 `<details open>` 块中（默认展开）
- 所有中文翻译放在 `<details style="font-size: 13px !important">` 块中（默认折叠）
- 中英文条目逐条对应，顺序一致

---

## 六、格式规则（重要！）

### 空行规则
```
**词源：** [中文]
                    ← 必须有空行
**Definitions:**
                    ← 必须有空行
- EN: ...
- CN: ...
                    ← 必须有空行
**Part of Speech:** ...
                    ← 必须有空行
```

- 每个 `**SectionName:**` 与前后内容之间都有空行
- 每个列表（`- item`）与其标题之间有空行
- 每个列表与其后的标题之间有空行

### HTML 标签与 markdown 之间的空行（关键！）

**当 markdown 内容紧接 HTML 标签时，必须用空行隔开，否则 markdown 语法不会被渲染。**

```
</details>
                    ← 必须有空行！
**Preposition Patterns (English):**     ← 没有这个空行，**粗体** 不会渲染
```

正确写法：
```
</details>

**Preposition Patterns (English):**     ← ✅ 正常渲染为粗体
```

这条规则适用于：
- `</details>` 之后的 markdown 标题行
- `<details>` 标签前后的 markdown 内容

### 不要在 HTML 标签内使用 markdown 语法

**错误：** `<summary>**General Collocations (中文):**</summary>` → `**` 不会被解析，直接显示为文字

**正确：** `<summary><b>General Collocations (中文)</b></summary>` → 用 `<b>` 标签代替 `**`

### 锚点与跳转
- 每个动词标题：`### N. verb <a id="N-verb"></a>`
- 页面顶部锚点：`<div id="top"></div>`
- 每条动词末尾：`[↑ Back to Top](#top)` 独占一行，前面有空行
- `[↑ Back to Top](#top)` 后紧跟 `---` 分隔，`---` 前后均有空行
- 目录链接：`(#N-verb)` 与 `id="N-verb"` 完全匹配

### 中英文分离规则总表

| 模块 | EN 部分 | CN 部分 |
|------|---------|---------|
| 搭配 | `**General Collocations (English):**` 展开 | `<details>` 折叠块，`<summary><b>...<b></summary>` |
| 介词 | `**Preposition Patterns (English):**` 展开 | `<details>` 折叠块，`<summary><b>...<b></summary>` |
| 例句 | `<details open>` 展开 | `<details style="font-size: 13px !important">` 折叠 |
| 词源 | `**Etymology:**` 独立行 | `**词源：**` 独立行 |
| 释义 | `- EN:` | `- CN:` |
| 用法 | 英文在前 | `— 中文` 后接 |

---

## 七、质量检查清单

```bash
# 基本结构
echo "动词条目: $(grep -cE '^### [0-9]+\.' file.md)"
echo "TOC: $(grep -c '## 📖 Table of Contents' file.md)"
echo "Verb Details: $(grep -c '<h2>Verb Details</h2>' file.md)"
echo "Back to Top: $(grep -c '↑ Back to Top' file.md)"

# 中英文分离完整性
echo "EN 搭配: $(grep -c 'General Collocations (English)' file.md)"
echo "CN 搭配折叠: $(grep -c '<summary><b>General Collocations (中文)</b></summary>' file.md)"
echo "EN 介词: $(grep -c 'Preposition Patterns (English)' file.md)"
echo "CN 介词折叠: $(grep -c '<summary><b>Preposition Patterns (中文)</b></summary>' file.md)"
echo "EN 例句: $(grep -c '📖 EN Example' file.md)"
echo "CN 例句: $(grep -c '📖 例句翻译' file.md)"

# 关键数据一致性（必须全部相等）
# EN 例句数 = CN 例句数
# EN 搭配条目数 = CN 搭配条目数
# EN 介词条目数 = CN 介词条目数
# Preposition Patterns 条目数 = 介词搭配例句数
```

| 检查项 | 期望值 |
|--------|--------|
| 动词条目数 | = 该字母动词总数 |
| `## 📖 Table of Contents` | = 1 |
| `<h2>Verb Details</h2>` | = 1 |
| `↑ Back to Top` | = 动词数 + 1 |
| `General Collocations (English)` | = 动词数 |
| `<summary><b>General Collocations (中文)</b></summary>` | = 动词数 |
| `Preposition Patterns (English)` | = 动词数 |
| `<summary><b>Preposition Patterns (中文)</b></summary>` | = 动词数 |
| `📖 EN Example Sentences` | = 动词数 |
| `📖 例句翻译 (Chinese)` | = 动词数 |
| 搭配 EN/CN 条目数 | 相等 |
| 介词 EN/CN 条目数 | 相等 |
| 例句 EN/CN 条目数 | 相等 |
| Preposition Patterns 条目 = 介词搭配例句 | 完全一致 |
| TOC 链接 `#N-verb` = 锚点 `id="N-verb"` | 全部匹配 |

---

## 八、常见渲染问题

| 问题 | 原因 | 修复 |
|------|------|------|
| `**文字**` 显示为文字（不是粗体） | 在 `<summary>` 等 HTML 标签内使用 markdown | 改用 `<b>文字</b>` |
| `**标题**` 显示为文字（不是粗体） | HTML 标签后缺少空行就接 markdown | 在 `</details>` 和 `**标题**` 之间加空行 |
| `[↑ Back to Top](#top)` 不是链接 | 代码写成了 `[↑ Back to Top](#top)` 但缺少 `[` 或 `]` | 确保格式为 `[↑ Back to Top](#top)` |
| 中文字体没有变小 | 页级 CSS 用 `!important` 覆盖了内联样式 | 内联样式也要加 `!important` |
| 中文折叠块没有折叠 | `<details>` 漏写了 `open` 属性，或 `open` 写成了 `<details open>` | CN 块用 `<details>`（无 open），EN 块用 `<details open>` |

---

## 九、参考来源（文档末尾）

```markdown
## 📚 References

1. **Oxford English Dictionary (OED)** — https://www.oed.com/
2. **Merriam-Webster Collegiate Dictionary** — https://www.merriam-webster.com/
3. **Collins COBUILD Advanced Learner's Dictionary** — https://www.collinsdictionary.com/
4. **Cambridge International Dictionary of English** — https://dictionary.cambridge.org/
5. **Longman Dictionary of Contemporary English** — https://www.ldoceonline.com/
6. **Etymonline (Online Etymology Dictionary)** — https://www.etymonline.com/
7. **百度百科 / 百度翻译** — https://fanyi.baidu.com/
8. **CET-4/6 Past Exam Papers** — 历年大学英语四六级考试真题
9. **全国硕士研究生入学统一考试英语试题** — National Postgraduate Entrance Exam English Papers
10. **TOEFL TPO (Test Preparation Online)** — TOEFL Practice Online
11. **IELTS Cambridge Series** — Cambridge IELTS Practice Tests
12. **Nature** — https://www.nature.com/
13. **Science** — https://www.science.org/
14. **The Lancet** — https://www.thelancet.com/
15. **Cell** — https://www.cell.com/
16. **The New England Journal of Medicine** — https://www.nejm.org/

---

*Document generated for study purposes. Example sentences are adapted from authentic sources for educational use.*

[↑ Back to Top](#top)
```

---

## 十、数据流说明

### 入门步骤
1. 从 `CET_4+6_edited.txt` 提取指定字母开头的所有单词
2. 人工筛选出动词（排除名词/形容词等）
3. 对每个动词按上述格式生成条目
4. 组装为完整文档

### 关键数字（以 A 为例）
```
单词总数（含非动词）: ~616
动词数: 148
Preposition Patterns 条目总数: 771
介词搭配例句总数: 771
文档行数: ~14234
文档大小: ~600KB
```

### 耗时最多的步骤
1. **生成介词搭配例句**（每动词 3-15 条，与 Preposition Patterns 数量一致）
2. **确保中英文条目一一对应**（搭配、介词、例句三个维度都要对）
3. **确保每个 Preposition Patterns 都有对应的 `- 介词搭配:` 例句**
