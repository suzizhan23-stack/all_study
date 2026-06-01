# Python + AI 完整学习指南

> 目标读者：Java 开发工程师 → AI 工程架构师
> 学习目标：掌握 Python 全部核心知识 → 能阅读/修改 GitHub AI 项目 → 能独立开发 AI 系统

---

# 第一阶段：Python 基础

---

## 1.1 Python 语言本质

### 1.1.1 一句话核心本质

**Python 是一种动态解释型、面向对象的通用编程语言，以"开发效率优先"为最高原则。**

### 1.1.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 类型系统 | 静态强类型 | 动态强类型 |
| 编译方式 | 编译为字节码 (JVM) | 解释执行 (CPython) |
| 运行速度 | 快 (JIT) | 慢 (解释执行) |
| 开发效率 | 低 (模板代码多) | 高 (简洁) |
| 变量声明 | `String s = "hello"` | `s = "hello"` |
| 代码块 | `{}` + `;` | 缩进 |
| 入口 | `public static void main` | `if __name__ == "__main__"` |

**思维差异：**
- Java：一切都需要显式声明，类型安全由编译器保障
- Python：一切以简洁优先，类型安全由运行时保障 + 可选 typing

**设计哲学差异：**
- Java："显式优于隐式"
- Python："简洁优于复杂"

### 1.1.3 技术原理

> CPython 执行流程

```
Python Source Code (.py)
        ↓
  词法分析 (Lexer) → Token 流
        ↓
  语法分析 (Parser) → AST (抽象语法树)
        ↓
  编译 (Compiler) → Bytecode (.pyc)
        ↓
  Python 虚拟机 (Python VM) → 执行 Bytecode
        ↓
  对象模型 (PyObject) → 内存中的 Python 对象
        ↓
   执行结果
```

**代码演示：使用 `dis` 模块反编译 Python 函数，查看 CPython 虚拟机实际执行的字节码指令。`dis.dis()` 将函数编译后的 `code object` 转换为可读的字节码序列（LOAD_CONST、STORE_FAST、BINARY_OP、RETURN_VALUE 等），是理解 Python 执行模型最重要的调试工具。**

```python
import dis

def hello():
    x = 1
    y = 2
    return x + y

dis.dis(hello)
# 3           0 LOAD_CONST               1 (1)
#             2 STORE_FAST               0 (x)
# 4           4 LOAD_CONST               2 (2)
#             6 STORE_FAST               1 (y)
# 5           8 LOAD_FAST                0 (x)
#            10 LOAD_FAST                1 (y)
#            12 BINARY_OP                0 (+)
#            16 RETURN_VALUE
```

这段代码用 `dis`（disassembler，反汇编器）展示了 Python 源码被编译成的**字节码**。
它对应上方 ASCII 流程图中的环节：

```
Python Source Code → Parser → AST → **Bytecode** → Python VM → 执行
```

Python 不是直接执行源码，而是先编译成字节码再由虚拟机执行——`.pyc` 文件中存储的就是这些指令。

### 1.1.4 为什么 AI 领域偏爱 Python

1. **快速原型**：AI 研究需要快速实验
2. **生态完整**：numpy/pandas/pytorch/transformers 优先支持 Python
3. **C扩展**：计算密集型用 C/C++，Python 做胶水层
4. **社区驱动**：AI 论文第一个实现几乎都是 Python

```python
import numpy as np
data = np.random.randn(1000, 1000)
result = np.linalg.svd(data)
```

### 1.1.5 GitHub 项目实战

这是典型 AI 项目的入口文件结构：

- `if __name__ == "__main__"` → 等价于 Java 的 `public static void main(String[] args)`，只有直接运行此文件时才执行，被 `import` 时不执行
- `argparse` → 等价于 Java 的 args4j/picocli，解析命令行参数
- `Path` → 等价于 Java 的 `java.nio.file.Path`，处理路径
- `logging.basicConfig` → Python 标准日志配置

```python
import argparse
import logging
from pathlib import Path

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", type=str, default="gpt-4")
    parser.add_argument("--config", type=Path, default=Path("config.yaml"))
    return parser.parse_args()

def main():
    args = parse_args()
    logging.basicConfig(level=logging.INFO)

if __name__ == "__main__":
    main()
```

### 1.1.6 常见错误

**以下是常见的错误示例：**
```python
# 忘记 if __name__ 守卫
def helper():
    print("running...")

helper()  # 被 import 时会直接执行

# 正确：
if __name__ == "__main__":
    helper()
```

---

## 1.2 基础语法

### 1.2.1 一句话核心本质

**Python 用缩进代替括号、用动态类型代替静态声明、用鸭子类型代替接口约束。**

### 1.2.2 Java vs Python 对比

```java
public class HelloWorld {
    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            System.out.println("Hello " + i);
        }
    }
}
```

**Python 等价代码：Python 用 `range()` 替代 Java 的 `for (int i=0; i<5; i++)`，用 `f-string` 替代字符串拼接，用缩进替代 `{}`。`range()` 返回惰性迭代器，不预先分配整个列表。**

```python
for i in range(5):
    print(f"Hello {i}")
```

**设计哲学：** Java 用 `{}` 表示作用域，Python 用缩进。Python 认为代码可读性比显式边界更重要。

### 1.2.3 缩进代替括号

Python 解析器用 **INDENT/DEDENT token** 替代 Java 的 `{}` 识别代码块：

```
line 1: if x > 0:          →  IF  COLON
line 2:     print("ok")    →  **INDENT** PRINT    ← 缩进增加 = 块开始
line 3: print("done")      →  **DEDENT** PRINT    ← 缩进减少 = 块结束
```

对比 Java：
```java
if (x > 0) {        // {
    System.out.println("ok");
}                    // }
```

`INDENT`/`DEDENT` 相当于自动插入的 `{`/`}`，在 Parser 层生成，之后才构建 AST 和生成字节码。

### 1.2.4 常见错误

**以下是常见的错误示例：**
```python
# 混用 tab 和空格
def hello():
	print("hello")  # tab
    print("world")   # 空格 → IndentationError

# 正确：统一 4 个空格
```

---

## 1.3 数据类型

### 1.3.1 一句话核心本质

**Python 中"一切皆对象"，变量是对象的引用，类型是对象的属性而非变量的属性。**

### 1.3.2 Java vs Python 对比

```java
int a = 10;              // 栈上分配，值类型
Integer b = new Integer(10);  // 堆上分配
```


**Python 对应代码：所有变量都是堆上 PyObject 引用。**
`a = 10` 创建一个 PyLongObject(10) 对象，`b = "hello"` 创建一个 PyUnicodeObject，`c = [1,2,3]` 创建一个 PyListObject。每个赋值都在堆上分配新对象，变量只保存指向这些对象的指针（引用）。

```python
a = 10                   # PyLongObject, 堆上分配
b = "hello"              # PyUnicodeObject
c = [1, 2, 3]            # PyListObject

print(type(10))          # <class 'int'>
print(type(type))        # <class 'type'>
```

**差异：** Java int 是值类型（栈），Python 所有类型都是堆上对象。

**内存模型对比：**

```java
// Java: 变量直接存值
int a = 10;       // 栈帧: [a = 10]
int b = a;        // 栈帧: [a = 10, b = 10] ← 复制值
b = 20;           // a=10, b=20 ← 互不影响
```


**Python 内存模型对比代码：Python 变量是引用（指针），`a = 10` 在栈上存指针指向堆上的 PyLongObject，`b = a` 复制指针让两个变量指向同一对象，`b = 20` 创建新对象并让 b 指向它，a 不受影响。这种"对象在堆、引用在栈"的模型与 Java 引用类型一致，但与 Java 基本类型完全不同。**

```python
# Python: 变量存引用（指针）
a = 10            // 栈帧: [a] ──→ 堆: [PyLongObject(10)]
b = a             // 栈帧: [a, b] ──→ 堆: [PyLongObject(10)] ← 同一对象
b = 20            // 栈帧: [a] ──→ [PyLongObject(10)]
                  //        [b] ──→ [PyLongObject(20)] ← 创建新对象
```

```
Java 内存布局：                    Python 内存布局：

int a = 10                       a = 10
┌───────────┐                    ┌───────────┐    ┌──────────────────┐
│ 栈: a = 10 │                    │ 栈: a ────────→ │ PyLongObject     │
└───────────┘                    └───────────┘    │ ob_refcnt = 2    │
                                                  │ ob_type → int    │
int b = a (复制值)               b = a (复制引用)   │ ob_digit = 10    │
┌───────────┐ ┌───────────┐     ┌───────────┐ ┌──┴──────────────────┘
│ 栈: a=10  │ │ 栈: b=10  │     │ 栈: a ────┼─┘
└───────────┘ └───────────┘     │ 栈: b ────┼──┘  ← 指向同一个对象
                                └───────────┘

b = 20 之后                      b = 20 之后
┌───────────┐ ┌───────────┐     ┌───────────┐   ┌──────────────────┐
│ 栈: a=10  │ │ 栈: b=20  │     │ 栈: a ──────→  │ PyLongObject(10) │
└───────────┘ └───────────┘     │ 栈: b ──────→  │ PyLongObject(20) │
                                └───────────┘   └──────────────────┘
```

**结论：** Java `int` 在栈上直接存值，`b = a` 复制值；Python 所有变量都是栈上的指针（引用），`b = a` 复制指针，两变量指向同一对象，赋值给 `b` 只是让 `b` 指向新对象。

### 1.3.3 内存原理

```
每个 Python 对象在内存中：
+------------------+
| ob_refcnt        |  ← 引用计数 (GC)，8 bytes
+------------------+
| ob_type          |  ← 指向类型对象，8 bytes
+------------------+
| ob_size          |  ← 变长对象元素个数，8 bytes（仅 list/dict/str 等变长对象有）
+------------------+
| 对象具体数据      |
+------------------+
```

**关键说明：**
- `ob_type` 指向该对象的类型对象（`PyTypeObject`），例如 `int`、`str`、`list`、`dict`、`float` 等。类型对象本身也是对象（`type` 类的实例），存储在堆上，全局唯一。
- `ob_size` 仅变长对象（`PyVarObject`）有，例如 `list` 的长度、`str` 的字符数。定长对象如 `int`、`float` 没有此字段。
- 对象大小（64位系统）：
  - `int`(42)：28 bytes（ob_refcnt 8 + ob_type 8 + ob_digit 8 + 对齐）= C `long` 的 **7 倍**
  - `float`(3.14)：24 bytes = C `double` 的 **3 倍**
  - `str`("a")：50 bytes（含 PyUnicodeObject 头部 + 1 字符）
  - `list`([])：56 bytes（头部）+ 每元素 8 bytes 指针
- Python 对象比 C 类型大得多，因为每个对象都附加了引用计数、类型指针等元信息。这是"一切皆对象"的代价——灵活性的开销。

#### 1.3.3.1 ob_type 深入：Python 类型系统

`ob_type` 指向的是 **`PyTypeObject`** 结构体，它描述了一个类型的所有信息（类型名、方法、基类等）。Python 的类型体系是**三层结构**：

```
实例 (instance)  →  类 (class/type)  →  元类 (metaclass)
  a = 42        →  int              →  type
  "hello"       →  str              →  type
  [1,2,3]       →  list             →  type
  int           →  type             →  type  ← 闭环
```

**核心规则：** 每个对象的 `ob_type` 指向它的类型PyTypeObject（即类的 C 层结构体），类型的类型是 `type`，`type` 的类型是它自己。


```
内存中完整关系图：

a = 42（实例对象）
  ┌──────────────┐
  │ ob_refcnt: 3 │
  │ ob_type ─────┼─────────────────────────┐
  │ ob_digit: 42 │                         │
  └──────────────┘                         │
                                           ▼
                               ┌──────────────────────┐
                               │ PyTypeObject ("int")  │  ← a 的类型
                               │ tp_name: "int"        │
                               │ tp_basicsize: 28      │
                               │ tp_dealloc: ...       │
                               │ tp_repr: int_repr     │
                               │ tp_str: int_to_str    │
                               │ tp_as_number: + - *   │  ← 数值运算函数表
                               │ ob_type ──────────────┼───────┐
                               └──────────────────────┘       │
                                                               ▼
                                                   ┌──────────────────────┐
                                                   │ PyTypeObject ("type") │  ← int 的类型（元类）
                                                   │ tp_name: "type"       │
                                                   │ tp_basicsize: ...     │
                                                   │ tp_call: type_call    │  ← type() 函数
                                                   │ tp_new: type_new      │
                                                   │ ob_type ────────┐     │
                                                   └──────────────────│─────┘
                                                                      │
                                                                      ▼
                                                   ┌──────────────────────┐
                                                   │ PyTypeObject ("type") │  ← 指向自己
                                                   │ (同一个对象)          │
                                                   └──────────────────────┘
```


#### 1.3.3.2 PyTypeObject 每个属性白话详解

`PyTypeObject` 可以理解成 Python 类在 C 层的"户口本"——这个类型叫什么名字、它的实例占多大内存、支持哪些运算、怎么创建和销毁、继承谁，全记在这个结构体里。下面分组讲每个字段是干什么的，以及你在 Python 层怎么看到它。

**字段总览（约 9 组）：**

```
┌─────────────────────────────────────────┐
│ ① PyObject_VAR_HEAD  ← 公用头部          │  ← 每个 Python 对象都有的东西
├─────────────────────────────────────────┤
│ ② tp_name            ← 类型名            │  ← "int", "str", "Dog"
├─────────────────────────────────────────┤
│ ③ tp_basicsize       ← 实例多大（字节）   │  ← sys.getsizeof(x)
├─────────────────────────────────────────┤
│ ④ tp_dealloc         ← 销毁逻辑          │  ← 引用归零时怎么清理
├─────────────────────────────────────────┤
│ ⑤ tp_repr/tp_str/tp_hash               │
│    ← 打印 / 转字符串 / 哈希 / 比较       │  ← repr() / str() / hash() / ==
├─────────────────────────────────────────┤
│ ⑥ tp_as_number/tp_as_sequence          │
│    /tp_as_mapping  ← 运算符函数表         │  ← + - * [] 这些操作
├─────────────────────────────────────────┤
│ ⑦ tp_new/tp_init/tp_call               │
│    ← 创建、初始化、可调用                 │  ← __new__ / __init__ / obj()
├─────────────────────────────────────────┤
│ ⑧ tp_getattro/tp_setattro              │
│    ← 属性读取和赋值                      │  ← obj.x = 1 底层怎么工作
├─────────────────────────────────────────┤
│ ⑨ tp_mro/tp_bases/tp_dict              │
│    ← 继承链、基类、属性字典               │  ← __mro__ / __bases__ / __dict__
└─────────────────────────────────────────┘
```

**① `PyObject_VAR_HEAD`——所有 Python 对象的公用头**

这不是 `PyTypeObject` 独有的，是每个 Python 对象内存布局的前几个字段：

```
每个 Python 对象的开头：
┌──────────────┐
│ ob_refcnt    │  8 字节  ← 引用计数（有多少变量在引用我）
│ ob_type      │  8 字节  ← 指向我的类型的 PyTypeObject
│ ob_size      │  8 字节  ← 如果是变长对象（list/str），这里存长度
└──────────────┘

说白了：Python 里任何对象，内存前 24 字节都是这 3 个东西。
ob_type 指向的就是我们正在讲的 PyTypeObject。
```

Python 层对应：你看不到 `ob_refcnt`（`sys.getrefcount()` 能看到近似值），但 `type(x)` 读的就是 `ob_type`，`len(x)` 读的是 `ob_size`。

**② `tp_name`——类型名字符串**

```
C 字段: const char *tp_name;    ← 就是一个字符串指针
Python: type(x).__name__

int 的 tp_name = "int"
str 的 tp_name = "str"
Dog 的 tp_name = "Dog"
```

作用：错误消息里显示 `<class 'int'>`、`repr()` 输出、`__name__` 属性，都来自这里。

**③ `tp_basicsize` / `tp_itemsize`——我的实例占多大内存**

```
C 字段: Py_ssize_t tp_basicsize;    ← 每个实例基础大小（字节）
       Py_ssize_t tp_itemsize;     ← 每多一个元素加多少（变长对象用）
Python: sys.getsizeof(x)

各类型的尺寸：
  int:   basicsize=28, itemsize=0    → int 实例总是 28 字节
  str:   basicsize=48, itemsize=1    → "hello" = 48 + 5*1 = 53
  list:  basicsize=56, itemsize=8    → [1,2,3] = 56 + 3*8 + 额外缓冲区
```

```python
import sys
print(sys.getsizeof(42))        # 28  ← 就是 tp_basicsize
print(sys.getsizeof("hello"))   # 54  ← 48 + 6（含 \0）
print(sys.getsizeof([1,2,3]))   # 88  ← 56 + 3*8 + 多分配的空间
```

**④ `tp_dealloc`——对象怎么被销毁**

```
C 字段: destructor tp_dealloc;    ← 一个函数指针
Python: __del__() 方法（但不常用）

当对象的 ob_refcnt 降到 0 时，CPython 调用 tp_dealloc：
1. 先释放资源（关闭文件、释放锁）
2. 然后调用 tp_free 把内存还给 CPython 的内存池
```

内置类型（int/str）的 `tp_dealloc` 很简单——直接释放内存。自定义类的 `tp_dealloc` 是通用的 `subtype_dealloc`，它会调用你的 `__del__` 方法（如果定义了）。

**⑤ `tp_repr` / `tp_str` / `tp_hash` / `tp_richcompare`——打印、哈希、比较**

```
字段             作用                Python 层对应
────────────────  ─────              ─────────────
tp_repr           repr(obj)          obj.__repr__()
tp_str            str(obj)           obj.__str__()
tp_hash           哈希值              obj.__hash__()
tp_richcompare    比较（== < >）      obj.__eq__() / __lt__()
```

**`repr` 和 `str` 的核心区别——`repr` 给开发者看，`str` 给用户看：**

```python
import datetime
now = datetime.datetime(2025, 1, 1, 10, 30, 0)

print(str(now))   # 2025-01-01 10:30:00       ← 用户友好，干净
print(repr(now))  # datetime.datetime(2025, 1, 1, 10, 30)  ← 开发友好，可重现

# repr 的黄金法则：eval(repr(x)) == x  （尽量成立）
print(eval(repr(now)) == now)  # True
```

**两者在 PyTypeObject 里是独立的两个 C 函数指针：**

| 角度 | `tp_repr` | `tp_str` |
|------|-----------|----------|
| Python 调用 | `repr(x)` / `f"{x!r}"` | `str(x)` / `print(x)` / `f"{x}"` |
| 设计目标 | 精确、无歧义、可调试 | 可读、美观、用户友好 |
| 默认行为 | `object_repr` → `<类名 object at 地址>` | `object_str` → 回退到 `tp_repr` |
| `int` 实现 | `int_repr` → `"42"` | `tp_str = NULL`（直接用 tp_repr） |
| 字符串自身 | `repr("a")` → `"'a'"`（带引号！） | `str("a")` → `"a"`（不带） |

```python
# 自定义类演示区别
class Person:
    def __init__(self, name, age):
        self.name = name
        self.age = age

    def __repr__(self):
        # 返回可重现的表达式
        return f"Person('{self.name}', {self.age})"

    def __str__(self):
        # 返回用户友好的描述
        return f"{self.name} ({self.age}岁)"

p = Person("小明", 25)
print(repr(p))   # Person('小明', 25)   ← 开发调试用
print(str(p))    # 小明 (25岁)          ← 用户看
print(p)         # 小明 (25岁)          ← print 调 str
```

`tp_hash` 和 `tp_richcompare`：
```python
print(hash(42))        # 42     ← int.tp_hash 直接把值当哈希
print(hash("hello"))   # 大整数  ← str.tp_hash 算字符的哈希
print(42 == 42)        # True   ← int.tp_richcompare
print(42 < 100)        # True
```

**⑥ `tp_as_number` / `tp_as_sequence` / `tp_as_mapping`——运算符函数表**

这是 PyTypeObject 里最重要的设计之一：**函数指针表**。

```
字段                     作用                 Python 层对应
──────────────────       ─────               ─────────────
tp_as_number             + - * / 等数值运算    __add__, __sub__ 等
tp_as_sequence           [] 索引、切片、len    __getitem__, __len__
tp_as_mapping            字典键操作            __getitem__, keys()
```

每个字段指向一个结构体，里面全是函数指针：

```
tp_as_number 指向的 PyNumberMethods（数值运算函数表）：
┌────────────────────┐
│ nb_add   → int_add  │  ← a + b 时调用
│ nb_sub   → int_sub  │  ← a - b 时调用
│ nb_mul   → int_mul  │  ← a * b 时调用
│ nb_true_div → ...   │  ← a / b 时调用
│ nb_floor_div → ...  │  ← a // b 时调用
│ nb_negative → ...   │  ← -a 时调用
│ nb_absolute → ...   │  ← abs(a) 时调用
│ ... 一共约 30 个槽位  │
└────────────────────┘
```

`int` 的 `tp_as_number` 填满了所有数值运算函数，所以 `int` 支持完整算术。用户自定义类默认 `tp_as_number = NULL`，所以两个 `Dog` 实例 `d + d` 会报错——除非你在类里定义了 `__add__`，Python 会在类定义完成后把 `__add__` 注册到 `tp_as_number.nb_add`。

**用人生经验类比 `tp_as_number`：**

```
int 类型就像一个全科医生，什么手术都能做（+ - * / 都预装了）。
Dog 类就像一个普通人，没学过做手术（没装加减乘除），
但如果你让 Dog 去学（定义 __add__），它也能做特定运算。
```

**⑦ `tp_new` / `tp_init` / `tp_call`——对象怎么创建和调用**

```
字段             作用                   Python 层对应
────              ────                  ─────────────
tp_new           分配内存、创建原始对象    __new__()
tp_init          初始化字段              __init__()
tp_call          让实例可调用            obj(args) 走 __call__()
```

`a = int(42)` 的执行过程：

```
step 1: int.__new__(int, 42) 被调用
        → tp_new (C 函数 int_new)
        → 分配 28 字节内存
        → 填 ob_type = &PyInt_Type（指向 int 的 PyTypeObject）
        → 填 ob_digit = 42（实际数字）
        → 返回新 int 对象

step 2: 检查 int.__init__ 是否存在
        → int 的 tp_init = NULL（int 不需要初始化，new 就够了）
        → 跳过

step 3: 赋值给 a

自定义类 Dog：
step 1: Dog.__new__(Dog) → tp_new = type_new（通用分配器）
step 2: Dog.__init__(self, ...) → tp_init 调用你写的 __init__
```

`tp_call` 让你能把实例当函数调用。`class A: def __call__(self): ...` 就是设置了 `A` 的 `tp_call`。

**⑧ `tp_getattro` / `tp_setattro`——属性访问**

```
字段              作用                  Python 层对应
────               ────                ─────────────
tp_getattro       读取属性              getattr(obj, "x") 或 obj.x
tp_setattro       写入属性              setattr(obj, "x", v) 或 obj.x = v
```

默认的 `tp_getattro` 是 `PyObject_GenericGetAttr`，它的工作流程就是 Python 属性查找的完整逻辑：

```
查找 obj.attr_name：
1. 查 obj 的类型（obj.ob_type）的 tp_dict  →  有没有 attr_name？
   → 有？返回
2. 查 obj 自己有没有 __dict__  →  实例属性？
   → 有？返回
3. 沿 tp_mro 往父类找  →  基类有没有？
   → 有？返回
4. 都没找到 → 抛出 AttributeError

整个过程 C 代码就在几十行，比 Python 层用 getattr() 快得多。
```

**⑨ `tp_mro` / `tp_bases` / `tp_dict`——继承和属性存储**

```
字段        作用                     Python 层对应
────         ────                   ─────────────
tp_mro      方法解析顺序（元组）       cls.__mro__
tp_bases    直接基类（元组）           cls.__bases__
tp_dict     类型的属性字典             cls.__dict__
```

- `tp_mro` 就是 `int.__mro__` 返回的内容，Python 用这个列表决定方法查找顺序（C3 线性化算法）
- `tp_dict` 就是 `int.__dict__`，存着所有类属性：方法、类变量、`__add__` 等

```
int 的继承链（tp_mro）：[int, object]

查找 a.__repr__ 的 C 层路径：
a.ob_type → &PyInt_Type → 查 PyInt_Type.tp_dict
  → tp_dict 里没有 __repr__
  → 沿 tp_mro 找到 object
  → 查 object.tp_dict → 找到 __repr__
  → 调用 object.__repr__(a)
```

**int（内置类型）vs Dog（自定义类）对比表：**

```
字段              int                              Dog
────              ───                              ───
tp_name           "int"                            "Dog"
tp_basicsize      28                               56（obj头+空dict）
tp_dealloc        int_dealloc（简单释放）             subtype_dealloc（通用，会调__del__）
tp_repr           int_repr（C函数，直接拼字符串）     object_repr（调 Python 层 __repr__）
tp_hash           int_hash（C函数，直接返回值）       默认不能被哈希
tp_as_number      {nb_add: int_add, ...}            NULL（除非你定义 __add__）
tp_new            int_new（C函数，直接设值）           type_new（解析 Python 类，通用）
tp_init           NULL（int 不需要初始化）            调用 Python 的 __init__
tp_mro            [int, object]                     [Dog, object]
tp_dict           {"__add__": 槽位指针, ...}          {"bark": <function>, ...}

本质区别：int 的运算直接走 C 函数指针（tp_as_number.nb_add），CPU 零开销。
         Dog 的运算走 Python 解释器（查 tp_dict → 调 Python 函数），有解释器循环开销。
两者相同：都是 PyTypeObject 实例，ob_type 都指向 type。
```

#### 补充：函数的 PyTypeObject 长什么样

函数（`def` 定义的）也有自己的类型——`function`。它的 PyTypeObject 和类的结构不同：

```
Python 里每个函数也是一个对象，类型是 <class 'function'>：
>>> def f(): pass
>>> type(f)
<class 'function'>

function 的 PyTypeObject 特点：
┌─────────────────────────────────────┐
│ tp_name = "function"                 │
│ tp_call = function_call              │  ← f() 时调用
│ tp_repr = func_repr                  │  ← 显示 <function f at 0x...>
│ tp_dict = {"__annotations__": ...}   │  ← 函数属性字典
│ tp_new = function_new                │  ← 创建函数
│ tp_dealloc = func_dealloc            │
│ 注意：没有 tp_as_number！             │  函数不能直接做加减法
│       没有 tp_as_sequence！           │
└─────────────────────────────────────┘

函数对象本身（不是 PyTypeObject，而是 PyFunctionObject）：
┌──────────────────────────────┐
│ ob_refcnt / ob_type → function│
│ func_code                    │  ← 编译后的字节码
│ func_globals                 │  ← 全局变量字典
│ func_defaults                │  ← 默认参数值
│ func_closure                 │  ← 闭包 cell 对象元组
│ func_dict                    │  ← 函数属性字典（f.attr = 1）
│ func_name                    │  ← 函数名
└──────────────────────────────┘
```

```python
def f(a, b=10):
    """doc"""
    return a + b

print(type(f))                # <class 'function'>
print(f.__name__)             # "f"
print(f.__code__)             # <code object f at ...>
print(f.__defaults__)         # (10,)  ← 默认参数
print(f.__closure__)          # None   ← 没有闭包
```

#### 补充：可以自己修改 PyTypeObject 的属性吗？

**① 自定义类可以改，但有限度：**

```python
class Dog:
    def bark(self):
        return "Woof!"

# 可以改
Dog.name = "Rex"              # 加到 tp_dict
Dog.bark = lambda self: "Hav!"  # 替换 tp_dict 里的函数
del Dog.bark                  # 从 tp_dict 删除

d = Dog()
print(d.bark())  # AttributeError  bark 被删了

# 可以加运算符
Dog.__add__ = lambda self, other: Dog()
print(d + d)  # 虽然不报错，但不会注册到 tp_as_number！
# 实际上 __add__ 只是进了 tp_dict，调用时走解释器查找，
# 不会变成 C 级 tp_as_number.nb_add，所以性能不如内置类型
```

**② 内置类型基本不能改——CPython 在启动时冻结了：**

```python
# ❌ 不能给 int 加属性
# int.foo = 42  # TypeError: cannot set 'foo' attribute of immutable type 'int'

# ❌ 不能删内置类型的属性
# del int.__add__  # TypeError: cannot delete 'nb_add' attribute of immutable type 'int'

# ✅ 唯一能做的事情：monkey-patch 内置类型的方法（不推荐）
# 注意：这不是改 PyTypeObject，而是改 Python 层的包装
# 实际上大多数内置类型的槽位是只读的
```

```python
# 例外情况：通过子类扩展
class MyInt(int):
    """可以继承 int，然后自由修改"""
    def describe(self):
        return f"值={self}"

x = MyInt(42)
print(x.describe())  # "值=42"

# 但 MyInt 的 PyTypeObject 是全新的，不是修改 int 本身的
```

**③ 函数对象可以自由修改属性：**

```python
def f():
    pass

f.custom_attr = "anything"    # ✅ 函数可以当属性字典用
f.__dict__["key"] = 42        # 等价
print(f.custom_attr)          # "anything"
```

**总结：可修改性规则**

```
对象类型          可以改属性？        可以改方法？        可以加新属性？
────────          ──────────         ──────────         ───────────
自定义类实例       ✅                 通过类改 ✅         ✅
自定义类本身       ✅（加到 tp_dict）    ✅                 ✅
内置类型（int）    ❌（不可变类型）      ❌（冻结）          ❌
内置类型实例       ❌（int 没 __dict__） 通过类改 ❌        ❌
函数               ✅                  ✅                 ✅
模块               ✅                  ✅                 ✅
```

#### 补充：PyTypeObject 的字段什么时候、怎么被调用的？

这些字段不是摆着看的——Python 每执行一行代码都会用到它们。下面按执行场景分组说明：

**场景 1：`a + b`——运算符走 `tp_as_number`**

```python
result = a + b
```

```
CPython 执行 a + b 时的调用链：

1. 解析成字节码：BINARY_OP (+)

2. CPython 执行 BINARY_OP：
   → 读 a.ob_type → 拿到 a 的 PyTypeObject
   → 读 tp_as_number → 拿到 PyNumberMethods 结构体
   → 读 nb_add → 拿到 C 函数指针
   → 调用 nb_add(a, b)

3. 如果是自定义类（没有 tp_as_number）：
   → 回退到 Python 层：在 a 的 tp_dict 里找 __add__
   → 找不到？在 tp_mro 链的父类里找
   → 找不到？抛 TypeError: unsupported operand type

什么时候调用？  → 执行 a + b 的那一瞬间
谁调用的？     → CPython 的字节码执行引擎（eval_frame.c）
```

**场景 2：`print(x)`——显示走 `tp_str` / `tp_repr`**

```python
print(x)       # 走 tp_str
repr(x)        # 走 tp_repr
f"{x}"         # 走 tp_str
f"{x!r}"       # 走 tp_repr
```

```
CPython 执行 print(x) 时的调用链：

print(x) → x.__str__() → tp_str(x)

1. CPython 调用 PyObject_Str(x)
2. 读 x.ob_type → 拿到 PyTypeObject
3. 读 tp_str
   → 如果 tp_str != NULL：直接调用 tp_str(x)
   → 如果 tp_str == NULL（比如 int）：回退到 tp_repr(x)

自定义类：
4. 如果 tp_str == NULL 且 tp_repr == NULL：
   → 在 tp_dict 里找 __str__
   → 在 tp_mro 链上找 __str__
   → 都找不到 → 用 object_repr（<对象名 at 地址>）
```

**场景 3：`a.b`——属性访问走 `tp_getattro`**

```python
value = obj.attr
```

```
CPython 执行 obj.attr 的调用链：

1. 解析成字节码：LOAD_ATTR
2. 执行 LOAD_ATTR：
   → 读 obj.ob_type → 拿到 PyTypeObject
   → 读 tp_getattro → 调用 PyObject_GenericGetAttr(obj, "attr")

3. PyObject_GenericGetAttr 内部：
   a) 查 obj.ob_type.tp_dict 有没有 "attr"？（类属性/方法）
      → 找到？如果是 descriptor（如函数），触发 descriptor 协议，返回绑定方法
   b) 查 obj 自身的 __dict__ 有没有 "attr"？（实例属性）
   c) 沿 tp_mro 向父类查找
   d) 都没有 → AttributeError
```

**场景 4：`MyClass()`——创建对象走 `tp_new` + `tp_init`**

```python
obj = MyClass(args)
```

```
CPython 执行 MyClass(args) 的调用链：

1. MyClass 是一个 PyTypeObject（假设 MyClass 是自定义类）
   → MyClass.ob_type = type
   → type 的 tp_call → type_call() 被调用

2. type_call 内部：
   a) 调 MyClass.tp_new(args) — 分配内存，返回空对象
   b) 调 MyClass.tp_init(obj, args) — 初始化字段
   c) 返回 obj

什么时候调用？  → MyClass(args) 执行时
谁调用？       → type.tp_call（即 type_call 函数）
```

**场景 5：`del obj`——销毁走 `tp_dealloc`**

```python
del obj  # 或 obj 离开作用域，引用归零
```

```
CPython 的引用计数归零时：

1. 检查 ob_refcnt
   → 如果降为 0
   → 调 obj.ob_type.tp_dealloc(obj)

2. tp_dealloc 做什么：
   → 释放资源（关文件、释放锁等）
   → 调 tp_free(obj) 把内存还给 CPython

什么时候调用？  → 引用计数归零时，不是马上也不是定时
谁调用的？     → CPython 的引用计数机制（Objects/object.c）
```

**一句话总结 PyTypeObject 的调用时机：**

| 你写的 Python 代码 | PyTypeObject 字段 | 什么时候触发 |
|---|---|---|
| `a + b` | `tp_as_number.nb_add` | 执行加法运算时 |
| `a[b]` | `tp_as_sequence.sq_item` | 执行索引时 |
| `print(x)` | `tp_str` 或 `tp_repr` | 转字符串时 |
| `obj.attr` | `tp_getattro` | 读取属性时 |
| `obj.attr = v` | `tp_setattro` | 写入属性时 |
| `MyClass()` | `tp_call` → `tp_new` + `tp_init` | 创建实例时 |
| `del obj` | `tp_dealloc` | 引用归零销毁时 |
| `hash(x)` | `tp_hash` | 计算哈希时 |
| `a == b` | `tp_richcompare` | 比较时 |
| `isinstance(x, int)` | `tp_mro` | 检查类型时 |
| `len(x)` | `tp_as_sequence.sq_length` | 取长度时 |

**核心规律**：PyTypeObject 的字段就是 Python 语法和 C 实现之间的**映射表**——**你写一句 Python，CPython 去读一个对应字段的 C 函数指针来执行。**

#### 1.3.3.2b 实战：int 运行时的完整 PyTypeObject 调用流程

下面以 `a = 42; b = a + 8; print(b); del b` 这四行代码为例，逐行展示 `int` 的 PyTypeObject 是如何参与工作的。

```
内存初始状态：

a ──→ ┌───────────────────┐
       │ PyObject (int)    │      PyTypeObject (int)
       │ ob_refcnt = 1     │      ┌─────────────────────────────┐
       │ ob_type ──────────┼─────→│ tp_name = "int"              │
       │ ob_digit = 42     │      │ tp_basicsize = 28            │
       └───────────────────┘      │ tp_repr   → int_repr         │
                                  │ tp_str    → NULL(用repr)     │
b ──→ ?                          │ tp_hash   → int_hash         │
                                  │ tp_dealloc→ int_dealloc      │
                                  │ tp_as_number:                │
                                  │   nb_add  → int_add          │
                                  │   nb_sub  → int_sub          │
                                  │   nb_mul  → int_mul          │
                                  │ tp_new    → int_new          │
                                  │ tp_init   → NULL             │
                                  │ tp_mro    → [int, object]    │
                                  └─────────────────────────────┘
```

**第 1 行：`a = 42`**

```
CPython 执行步骤：

① CPython 遇到字面量 42，调 int.tp_new(int, 42)
   → int_new() 在堆上分配 28 字节（tp_basicsize = 28）
   → 写 ob_refcnt = 1
   → 写 ob_type = &PyInt_Type（即 int 的 PyTypeObject 地址）
   → 写 ob_digit = 42

② 检查 tp_init → NULL，跳过（int 不需要初始化）

③ 赋值给 a：
   a 是 PyObject* 指针，存的是步骤①分配的对象的地址
   → a 只是指向这个 PyObject 的指针
```

```
执行完后内存：

a ──→ ┌───────────────────┐
       │ ob_refcnt = 1     │
       │ ob_type ───→ &PyInt_Type
       │ ob_digit = 42     │
       └───────────────────┘
```

**第 2 行：`b = a + 8`**

```
① CPython 编译 a + 8 为字节码 BINARY_OP (+)

② 执行引擎处理 BINARY_OP：
   → 从栈顶取两个对象：a（PyObject*）和 8（也是 PyObject*）
   → 读 a.ob_type → 拿到 &PyInt_Type
   → 读 PyInt_Type.tp_as_number → 拿到 PyNumberMethods*
   → 读 PyNumberMethods.nb_add → 拿到 int_add 函数指针
   → 调用 int_add(a, 8)

③ int_add 内部：
   → 取 a.ob_digit = 42
   → 取 8.ob_digit = 8
   → 计算 42 + 8 = 50
   → 调 int.tp_new(int, 50) 创建新 int 对象（ob_digit=50, ob_refcnt=1）
   → 返回新 PyObject*

④ 赋值给 b：
   b 指向这个新 int 对象（ob_digit=50）
```

```
执行完后内存：

a ──→ ┌───────────────────┐
       │ refcnt=1, val=42  │
       └───────────────────┘

b ──→ ┌───────────────────┐
       │ refcnt=1, val=50  │  ← int_add 返回的新对象
       └───────────────────┘
```

**第 3 行：`print(b)`**

```
① CPython 编译 print(b) → 调 PyObject_Str(b)

② PyObject_Str(b)：
   → 读 b.ob_type → &PyInt_Type
   → 读 tp_str → NULL（int 的 tp_str 是 NULL）

③ 因为 tp_str == NULL，回退到 tp_repr：
   → 读 tp_repr → int_repr 函数指针
   → 调 int_repr(b) → int_repr 读 b.ob_digit = 50
   → 将 50 转为字符串 "50"
   → 返回 PyUnicodeObject（Python 字符串）

④ print 把 "50" 写出到 stdout
```

**第 4 行：`del b`**

```
① del b 移除变量 b 对 int 对象的引用
   → 该 int 对象的 ob_refcnt 从 1 降到 0

② CPython 检查 ob_refcnt == 0：
   → 读 ob_type → &PyInt_Type
   → 读 tp_dealloc → int_dealloc
   → 调 int_dealloc(b)
   → 释放 28 字节内存

③ 如果此时 a 也被 del 或离开作用域：
   → 42 对象的 ob_refcnt 归零
   → 同样走 int_dealloc 释放
```

**完整流程图：**

```
你写：a = 42
                 │
                 ▼
         int.__new__(int, 42)
                 │
                 ▼
         tp_new → int_new()
           ├─ allocate 28 bytes (tp_basicsize)
           ├─ ob_refcnt = 1
           ├─ ob_type = &PyInt_Type  ────→  PyTypeObject(int)
           └─ ob_digit = 42
                 │
                 ▼
         检查 tp_init → NULL(跳过)
                 │
                 ▼
         a 指向该 PyObject


你写：b = a + 8
                 │
                 ▼
         读 a.ob_type → &PyInt_Type
                 │
                 ▼
         读 PyInt_Type.tp_as_number
                 │
                 ▼
         读 tp_as_number.nb_add → int_add
                 │
                 ▼
         int_add(42, 8) → 50
                 │
                 ▼
         调 int_new(int, 50) → 新 PyObject(ob_digit=50)
                 │
                 ▼
         b 指向该新对象


你写：print(b)
                 │
                 ▼
         PyObject_Str(b)
           ├─ 读 b.ob_type → &PyInt_Type
           ├─ 读 tp_str → NULL → 回退
           └─ 读 tp_repr → int_repr(b) → "50"
                 │
                 ▼
         输出 "50"


你写：del b
                 │
                 ▼
         ob_refcnt 从 1 降为 0
                 │
                 ▼
         读 ob_type → &PyInt_Type
                 │
                 ▼
         读 tp_dealloc → int_dealloc(b)
                 │
                 ▼
         释放 28 字节内存
```

**如果没有 PyTypeObject，这一切会怎样？**

> 如果没有 `ob_type` 指针和 `PyTypeObject`，CPython 就不知道 `a` 是什么类型。加法的 `int_add` 没法找到，打印的 `int_repr` 没法找到，连 `a` 占多大内存都不知道。
>
> **`PyTypeObject` 的存在意义就是：给每个对象挂一个"类型说明书"，CPython 执行任何操作时先查说明书，再决定怎么做。这就是"一切皆对象"的底层实现——对象本身只存数据，怎么操作这些数据全在 PyTypeObject 里。**

#### 1.3.3.2c 实战：函数的完整底层流程

函数虽然也是对象，但它的内部比 `int` 复杂得多——函数有自己的 `PyFunctionObject` 结构体，执行时涉及栈帧创建、参数绑定、字节码执行、闭包处理。

**① 函数定义：`def` 是执行语句，不是声明**

```python
def square(x):
    return x * x
```

```
Python 源码:                       字节码序列:
def square(x):          ──→        LOAD_CONST  (<code object>)
    return x * x                    LOAD_CONST  ('square')
                                    MAKE_FUNCTION
                                    STORE_NAME  ('square')

CPython 执行 MAKE_FUNCTION 时的步骤：
① 编译函数体 → 生成 code object（字节码 + 常量表 + 变量名表）
② 从常量栈取出 code object
③ 创建 PyFunctionObject（堆分配）
④ 设置函数属性：
   func_name     = "square"
   func_code     = 步骤①的 code object
   func_globals  = 当前全局变量 dict（引用，不复制）
   func_defaults = None
   func_closure  = None
⑤ STORE_NAME 将函数对象赋值给变量 'square'
```

**`PyFunctionObject` 内存结构：**

```
┌─────────────────────────────────────────────┐
│ PyFunctionObject                             │
├─────────────────────────────────────────────┤
│ PyObject_HEAD (ob_refcnt + ob_type→function) │
├─────────────────────────────────────────────┤
│ func_name       → "square"                   │  函数名
│ func_code       → <code object>              │  编译好的字节码
│ func_globals    → {...}                      │  全局变量 dict（引用，不复制）
│ func_defaults   → None / (10, 20)            │  默认参数值元组
│ func_closure    → None / (<cell>, ...)       │  闭包 cell 元组
│ func_dict       → {}                         │  函数属性字典（可自由扩展）
│ func_module     → "__main__"                 │  定义模块名
│ func_annotations→ {"x": int, "return": int}  │  类型注解
├─────────────────────────────────────────────┤
│ 函数自己的 PyTypeObject:                      │
│   tp_name = "function"                       │
│   tp_call → function_call（C 函数）           │
│   tp_repr → func_repr                        │
│   没有 tp_as_number（函数不能加减）            │
└─────────────────────────────────────────────┘
```

```python
# 验证：函数对象和 int 对象一样，都是堆上的 PyObject
def square(x):
    return x * x

print(type(square))               # <class 'function'>
print(square.__name__)            # "square"
print(square.__code__)            # <code object square at 0x...>
print(square.__globals__)         # {...}
print(square.__defaults__)        # None
print(square.__closure__)         # None
```

**② 函数调用：`square(5)` 的完整执行链**

```
执行 square(5)：

① 变量名 'square' 取到 PyFunctionObject 指针

② 读 square.ob_type → &PyFunction_Type
   读 tp_call → function_call（C 函数）
   调用 function_call(square, (5,), {})

③ function_call 内部：
   → 分配新的 PyFrameObject（栈帧）
   ┌───────────────────────────────────────────┐
   │ f_back → 调用者的栈帧                      │
   │ f_code → square.__code__                  │
   │ f_locals → {"x": 5}（参数绑定）            │
   │ f_globals → square.__globals__            │
   │ f_builtins → __builtins__ dict            │
   └───────────────────────────────────────────┘

④ 参数绑定：位置参数 5 写入 f_locals["x"]

⑤ 字节码执行引擎启动（PyEval_EvalFrameDefault）：
   LOAD_FAST    0 (x)     → 从 f_locals 读 x → 5
   LOAD_FAST    0 (x)     → 从 f_locals 读 x → 5
   BINARY_MULTIPLY        → 5 * 5 = 25
                            （这里走 int.tp_as_number.nb_mul！）
   RETURN_VALUE           → 返回值 25

⑥ 销毁栈帧，返回值 25 压入调用者栈
```

```
调用 square(5) 时的内存状态：

[调用者栈帧]                      [堆]
  square ──────────────────→   PyFunctionObject
  a = 10                        func_code → <code object>
                                func_globals → 全局 dict
  ↑
  │ f_back                      [新栈帧 (PyFrameObject)]
  └──────────────────────────   f_code → square.__code__
                                f_locals → {"x": 5}
                                f_globals → 同一全局 dict

  步骤⑤中 x * x 执行时：
   → 读 x.ob_type → &PyInt_Type
   → 读 PyInt_Type.tp_as_number.nb_mul → int_mul
   → int_mul(5, 5) → 新 int 对象 (ob_digit=25)
```

**③ 闭包：cell 对象如何让局部变量"起死回生"**

```python
def outer(x):
    def inner(y):
        return x + y
    return inner

fn = outer(10)
print(fn(5))  # 15
```

```
执行 outer(10) 时的底层过程：

① 创建 outer 栈帧，绑定 x=10

② 编译 inner 时，CPython 检测到 inner 引用了 x：
   → x 不在 inner 的参数中
   → x 不在 inner 的局部变量中
   → x 来自 outer 的局部作用域
   → CPython 标记 x 为"自由变量"（free variable）
   → 编译 inner 时生成 LOAD_DEREF x（不是 LOAD_FAST！）

③ 创建 inner 的 PyFunctionObject：
   → 检测到自由变量 x
   → 在堆上创建 cell 对象：cell_x.cell_contents = 10
   → 设置 inner.func_closure = (cell_x,)

④ outer(10) 返回 inner：
   → inner 对象被返回（包含 func_closure 元组）
   → outer 栈帧销毁，但 cell 对象在堆上存活
```

```
闭包的内存状态（outer(10) 返回后）：

[模块作用域]
fn ──────────────────────────┐
                              │
                              ▼
                    ┌─────────────────────┐
                    │ PyFunctionObject     │
                    │ func_closure = (──,) │──┐
                    │ func_code → inner    │  │
                    └─────────────────────┘  │
                                             │
                                             ▼
                                      ┌──────────┐
                                      │ cell 对象  │
                                      │ contents  │
                                      │ = 10      │ ← outer 的 x，
                                      └──────────┘   但 outer 栈帧已销毁！

调用 fn(5) 时：
                    LOAD_DEREF x → cell.cell_contents → 10（从堆读！）
                    LOAD_FAST y → 5
                    BINARY_ADD → 15
```

```python
# 验证 cell 对象
fn = outer(10)
print(fn.__closure__)                      # (<cell at 0x...: int object at 0x...>,)
print(fn.__closure__[0].cell_contents)     # 10
```

**④ 高阶函数：本质就是指针传递**

```python
def apply_twice(f, value):
    return f(f(value))

def inc(x):
    return x + 1

print(apply_twice(inc, 5))  # 7
```

```
执行 apply_twice(inc, 5)：

apply_twice 的栈帧 f_locals = {"f": PyFunctionObject(inc), "value": 5}

执行 f(f(value))：
① value → 5
② f(5) → 读 f（PyFunctionObject 指针）→ tp_call → inc(5) → 6
③ f(6) → 读 f（同一指针）→ tp_call → inc(6) → 7
④ return 7

"函数作为参数"的底层本质：
  变量 f 里存的就是 PyFunctionObject 的内存地址，
  f(5) 就是读这个地址，走 tp_call。
  和传递 int 42 在底层没有本质区别——都是复制一个值/指针。
```

**⑤ 函数 vs int：PyObject 家族对比**

```
对比维度           int (42)                   function (square)
────────           ───────                   ─────────────────
C 结构体           PyLongObject               PyFunctionObject
ob_type 指向       PyTypeObject("int")         PyTypeObject("function")
数据存在哪         ob_digit 字段              func_code + func_closure
创建方式           int_new() → 直接赋值        MAKE_FUNCTION → 编译+打包
tp_call?           NULL（int 不可调用）         function_call（调函数）
tp_as_number?      {nb_add, nb_mul, ...}       NULL（函数不能加减）
可加属性？         ❌ int 实例没有 __dict__    ✅ func_dict 是标准字段
```

**核心结论：函数和 int 一样是堆上的 PyObject，通过 ob_type 找到自己的"说明书"。区别在于函数存的是字节码（func_code）和环境（func_closure/globals），int 存的是数值。`f(5)` 调用的本质就是 `f.ob_type.tp_call(f, (5,), {})`。**

#### 1.3.3.2d 实战：class 的完整底层流程

class 的定义和实例化比 int 和 function 更复杂——因为涉及**类型创建**（`type.__call__`）、**方法绑定**（descriptor 协议）、**实例字典**（`__dict__`）。

**① 类定义：`class Dog:` 执行时发生了什么**

```python
class Dog:
    """A friendly dog"""
    species = "Canine"

    def __init__(self, name):
        self.name = name

    def bark(self):
        return f"{self.name} says Woof!"
```

```
class 语句的执行流程（和 def 一样是运行时执行的！）：

[Python 源码]                         [字节码]
class Dog:                     ──→    LOAD_BUILD_CLASS
    species = "Canine"                 LOAD_CONST  ('Dog')
    def __init__(self, name):          LOAD_CONST  (<code object __init__>)
        self.name = name               MAKE_FUNCTION
    def bark(self):                    LOAD_CONST  (<code object bark>)
        return "Woof!"                 MAKE_FUNCTION
                                       LOAD_CONST  (...)
                                       BUILD_CLASS（核心！）

执行 BUILD_CLASS 时的完整步骤：

① 收集类体代码（class body）中定义的所有名字：
   → species = "Canine"               → 命名空间 {"species": "Canine"}
   → def __init__ → PyFunctionObject  → 命名空间 {"__init__": <function>}
   → def bark     → PyFunctionObject  → 命名空间 {"bark": <function>}

② 调 type.__call__("Dog", (object,), namespace)：
   → type.tp_call → type_call() 被调用

③ type_call 内部：
   a) 调 type.__new__(type, "Dog", (object,), namespace)
      → tp_new → type_new() 在堆上分配 PyTypeObject
      ├─ 设置 tp_name = "Dog"
      ├─ 设置 tp_basicsize = 56（obj头+空__dict__）
      ├─ 设置 tp_dict = namespace
      ├─ 设置 tp_mro = [Dog, object]
      ├─ 设置 tp_bases = (object,)
      ├─ 设置 tp_dealloc = subtype_dealloc
      ├─ 设置 tp_new = type_new（继承）
      ├─ 设置 tp_init = slot_tp_init（自动生成）
      └─ 检查 namespace 里的特殊方法（__init__, __repr__ 等）
         → 如果找到 __init__，注册到 tp_init
         → 如果找到 __add__，注册到 tp_as_number.nb_add（这就是协议）
   b) 调 type.__init__(Dog, "Dog", (object,), namespace)
      → 实际上 type_new 已经完成了所有工作，__init__ 基本是空操作

④ Dog 变量指向这个新创建的 PyTypeObject
```

**类定义后的内存状态：**

```
[堆上的 PyTypeObject("Dog")]           [int 的 PyTypeObject 做对比]
┌──────────────────────────────┐      ┌──────────────────────────────┐
│ PyObject_HEAD                │      │ PyObject_HEAD                │
│   ob_type → &PyType_Type     │      │   ob_type → &PyType_Type     │
├──────────────────────────────┤      ├──────────────────────────────┤
│ tp_name = "Dog"              │      │ tp_name = "int"              │
│ tp_basicsize = 56            │      │ tp_basicsize = 28            │
├──────────────────────────────┤      ├──────────────────────────────┤
│ tp_dealloc = subtype_dealloc │      │ tp_dealloc = int_dealloc     │
│ tp_new = type_new            │      │ tp_new = int_new             │
│ tp_init = slot_tp_init       │      │ tp_init = NULL               │
│ tp_call = NULL               │      │ tp_call = NULL               │
├──────────────────────────────┤      ├──────────────────────────────┤
│ tp_dict:                     │      │ tp_dict:                     │
│   ├─ "species" → "Canine"   │      │   ├─ "__add__" → 槽位指针    │
│   ├─ "__init__" → <function> │      │   ├─ "__sub__" → 槽位指针    │
│   ├─ "bark" → <function>    │      │   └─ ...                     │
├──────────────────────────────┤      ├──────────────────────────────┤
│ tp_as_number → NULL          │      │ tp_as_number → {nb_add, ...}│
│ tp_as_sequence → NULL        │      │ tp_as_sequence → NULL        │
│ tp_mro → [Dog, object]       │      │ tp_mro → [int, object]       │
│ tp_bases → (object,)         │      │ tp_bases → (object,)         │
└──────────────────────────────┘      └──────────────────────────────┘

核心区别：
  int 的 tp_new = int_new（C函数，直接写 ob_digit）
  Dog 的 tp_new = type_new（通用，解析 Python 类定义 → 创建实例）

  int 的 tp_as_number 装满了 C 函数指针（int_add 等）
  Dog 的 tp_as_number = NULL（除非定义了 __add__，Python 会自动注册）
```

```python
# 验证：class 就是创建一个 PyTypeObject
class Dog:
    species = "Canine"
    def bark(self):
        return "Woof!"

print(type(Dog))               # <class 'type'> ← Dog 是 type 的实例！
print(type(type))               # <class 'type'>
print(Dog.__name__)             # "Dog"
print(Dog.__dict__)             # {'species': 'Canine', 'bark': <function>, ...}
print(Dog.__mro__)              # (<class '__main__.Dog'>, <class 'object'>)
print(Dog.__bases__)            # (<class 'object'>,)
```

**② 实例化：`d = Dog("小白")` 的完整流程**

```
执行 d = Dog("小白")：

① Dog("小白") 等价于 type.__call__(Dog, ("小白",), {})
   → Dog 是 PyTypeObject，它的 ob_type = &PyType_Type
   → 读 PyType_Type.tp_call → type_call（C 函数）

② type_call 内部：
   a) 调 Dog.tp_new(Dog, "小白") → type_new(Dog, "小白")
      ├─ 分配 56 字节内存（tp_basicsize = 56）
      ├─ 设置 ob_type = &Dog（指向类的 PyTypeObject！）
      ├─ 设置 ob_refcnt = 1
      ├─ 创建空 __dict__（实例属性字典）
      └─ 返回"空"的 Dog 实例

   b) 调 Dog.tp_init(实例, "小白")
      → slot_tp_init 被调用
      → 在 Dog.tp_dict 里找 __init__
      → 找到 __init__ = <function>
      → 调 __init__(实例, "小白")
      → 执行 self.name = name
      → 在实例的 __dict__ 写入 {"name": "小白"}

③ d 变量指向这个 Dog 实例
```

**实例化后的内存状态：**

```
             ┌─────────────────────────────┐
d ──────────→│ Dog 实例（PyObject）          │
             │   ob_refcnt = 1              │
             │   ob_type ──────────────┐     │
             │   __dict__:             │     │
             │     "name" → "小白"     │     │
             └─────────────────────────┘     │
                                             │
                                             ▼
                                  ┌─────────────────────────────┐
                                  │ PyTypeObject("Dog")          │
                                  │   tp_name = "Dog"            │
                                  │   tp_dict:                   │
                                  │     "species" → "Canine"     │
                                  │     "__init__" → <function>  │
                                  │     "bark" → <function>      │
                                  │   tp_mro → [Dog, object]     │
                                  └─────────────────────────────┘
```

**③ 方法调用：`d.bark()` 的隐藏步骤——descriptor 协议**

这是 Python 最精妙的设计之一：`d.bark()` 不是直接调 `Dog.__dict__["bark"](d)`，中间有个**方法绑定**步骤。

```
d.bark() 的完整执行链：

① LOAD_ATTR 指令：读 d.bark
   → 读 d.ob_type → &Dog（PyTypeObject）
   → 读 tp_getattro → PyObject_GenericGetAttr
   → 在 Dog.tp_dict 里查找 "bark"
   → 找到 bark = <function object>（一个 PyFunctionObject）

② 关键步骤：检查 bark 是"descriptor"！！
   → PyFunctionObject 实现了 __get__ 方法
   → 因为 bark 有 __get__，触发 descriptor 协议
   → 调 bark.__get__(d, Dog) → 返回"绑定方法"对象

③ "绑定方法"（PyMethodObject）：
   ┌─────────────────────────┐
   │ im_func → bark 函数     │  ← 原始函数
   │ im_self → d（实例）     │  ← 绑定的实例
   │ im_class → Dog          │
   └─────────────────────────┘
   → 这个对象就是 d.bark 的结果
   → 它已经"记住"了 self=d

④ CALL 指令：执行 ()
   → 调绑定方法的 tp_call
   → 绑定方法把 im_self（d）作为第一个参数传给 im_func
   → 等价于 bark(d) → self=d
   → 执行 return f"{self.name} says Woof!" → "小白 says Woof!"
```

```
d.bark() 可视化流程：

        d.bark()
           │
           ▼
    d.ob_type.tp_getattro(d, "bark")
           │
           ▼
    Dog.tp_dict["bark"] → <function bark>
           │
           ▼
    bark 是函数 → 有 __get__ → 调 __get__(d, Dog)
           │
           ▼
    返回 PyMethodObject(im_func=bark, im_self=d)
           │
           ▼
    () 调这个绑定方法 → im_func(im_self) → bark(d)
           │
           ▼
    "小白 says Woof!"
```

```python
# 验证 d.bark 返回的是什么
d = Dog("小白")
print(d.bark)                  # <bound method Dog.bark of <__main__.Dog object at 0x...>>
print(type(d.bark))            # <class 'method'>

# 解开绑定方法的内部
print(d.bark.__func__)         # <function Dog.bark at 0x...> ← 原始函数
print(d.bark.__self__)         # <__main__.Dog object at 0x...> ← d 实例
print(d.bark.__self__.name)    # "小白"

# 等价调用方式
print(Dog.bark(d))             # "小白 says Woof!" ← 显式传 self
print(d.bark())                # "小白 says Woof!" ← 自动绑定 self
```

**④ 属性查找：`d.name` 和 `d.species` 的路径**

```
d.name 的查找路径：
① d.ob_type → Dog
② Dog.tp_dict → 查 "name" → 没有
③ 查 d.__dict__ → {"name": "小白"} → 找到了！返回 "小白"

d.species 的查找路径：
① d.ob_type → Dog
② Dog.tp_dict → 查 "species" → 找到了 "Canine"！返回
   （注意：实例的 __dict__ 里没有 species，它是类属性）

d.bark 的查找路径：
① d.ob_type → Dog
② Dog.tp_dict → 查 "bark" → 找到 <function>
③ 函数是 descriptor → 触发 __get__ → 绑定方法
   返回 <bound method Dog.bark of d>

三者的路径差异 built into tp_getattro（PyObject_GenericGetAttr）：
   tp_dict（类属性）→ __dict__（实例属性）→ tp_mro 父类
```

**⑤ int vs function vs class：三种 PyObject 对比**

```
对比维度         int(42)            function(square)     class Dog 的实例 d
────────         ───────            ─────────────────    ──────────────────
C 结构体         PyLongObject       PyFunctionObject     PyObject + __dict__
ob_type 指向     PyType("int")      PyType("function")   PyType("Dog")
数据存在哪       ob_digit           func_code            __dict__（字典）
创建方式         int_new            MAKE_FUNCTION        type_new + tp_init
tp_call?         NULL               调 function_call     NULL（默认不可调用）
tp_as_number?    {nb_add, ...}      NULL                 NULL（除非定义__add__）
属性访问         无 __dict__         func_dict           实例 __dict__
方法调用         无                  tp_call 直接执行     descriptor → 绑定方法

底层统一性：
  三者都是 PyObject*，前 16 字节都是 ob_refcnt + ob_type
  type(x) 就是读 ob_type → 返回对应的 PyTypeObject
  isinstance(x, int) 就是沿着 tp_mro 查 int 在不在链上
```

```python
# type 的角色 1：获取类型（等价于 .__class__）
print(type(42))       # <class 'int'>
print((42).__class__) # <class 'int'>

# type 的角色 2：动态创建类
MyClass = type("MyClass", (object,), {"x": 10})
# 等价于：
# class MyClass(object):
#     x = 10

# 类型三角关系验证
print(type(42))                  # <class 'int'>
print(type(int))                 # <class 'type'>
print(type(type))                # <class 'type'>
print(isinstance(type, type))    # True ← type 是自己的实例
print((42).__class__.__class__)  # <class 'type'>
```

**`type(obj)` 返回的是什么？** 返回的是 `obj` 的 `__class__` 属性，即 `obj` 的 `ob_type` 指针指向的那个 `PyTypeObject` 实例。这个返回对象本身也是一个 Python 对象，它有自己的一套接口：

| 接口 | 作用 | 对应 `PyTypeObject` 字段 |
|------|------|------------------------|
| `t.__name__` | 类型名称字符串 | `tp_name` |
| `t.__bases__` | 基类元组 | `tp_bases` |
| `t.__mro__` | 方法解析顺序列表 | `tp_mro` |
| `t.__dict__` | 属性字典（方法/字段都在这里） | `tp_dict` |
| `t.__module__` | 定义该类型的模块名 | `tp_module` |
| `t.__doc__` | 文档字符串 | `tp_doc` |
| `t.__subclasses__()` | 返回所有直接子类列表 | 运行时动态查询 |
| `isinstance(obj, t)` | 判断 `obj` 是否为 `t` 的实例 | 遍历 `tp_mro` 查找 |
| `issubclass(c, t)` | 判断 `c` 是否为 `t` 的子类 | 遍历 `tp_mro` 查找 |

```python
# type(obj) 返回的是一个 PyTypeObject 实例，它是对象！
t = type(42)  # t = <class 'int'>
print(t)                    # <class 'int'>
print(t.__name__)           # "int"
print(t.__bases__)          # (<class 'object'>,)
print(t.__mro__)            # (<class 'int'>, <class 'object'>)
print(t.__dict__)           # mappingproxy({...}) ← 所有 int 方法

# 这个返回的对象可以继续被 type() 调用
print(type(t))              # <class 'type'>
print(t.__class__)          # <class 'type'> ← 等价

# 在 AI 中的实际用途：动态检查和处理类型
def process_batch(data):
    """批量处理不同类型的数据"""
    if isinstance(data, list):
        return [process_item(x) for x in data]
    elif isinstance(data, dict):
        return {k: process_item(v) for k, v in data.items()}
    t_name = type(data).__name__
    raise TypeError(f"不支持的数据类型: {t_name}")

# isinstance 比 type() == 更好（支持继承）
class Animal: pass
class Dog(Animal): pass

d = Dog()
print(type(d) == Dog)      # True
print(type(d) == Animal)   # False ← 不认父类！
print(isinstance(d, Dog))   # True
print(isinstance(d, Animal))# True ← 认父类
```


#### 1.3.3.4 为什么 Python 对象模型比 Java 更灵活？
所有类型信息存储在堆上的 `PyTypeObject` 中，运行时可修改：

```python
# 运行时修改类型（monkey patching，慎用！）
class Dog:
    def bark(self):
        return "Woof!"

def new_bark(self):
    return "Meow!"  # 猫叫的狗

Dog.bark = new_bark  # 运行时替换方法
d = Dog()
print(d.bark())  # "Meow!"

# Java 做不到这点——类在编译期固定
```

**代码演示：通过 `id()`（对象内存地址）、`type()`（对象类型）、`sys.getrefcount()`（引用计数）三个内置接口窥探 Python 对象系统的底层实现。`id(a)` 返回 PyLongObject(42) 的堆内存地址，`sys.getrefcount(a)` 返回当前引用数（含临时引用+1），`b = a` 后 `id(a) == id(b)` 为 True 证明两变量指向同一对象。**

```python
a = 42
print(id(a))           # 内存地址
print(type(a))         # <class 'int'>
import sys
print(sys.getrefcount(a))

b = a
print(id(a) == id(b))  # True，同一个对象
```

#### 1.3.3.5 mutable vs immutable

```python
# immutable
a = 42
a = 43  # 创建新对象

# mutable
lst = [1, 2, 3]
lst.append(4)  # 修改原对象
```

| 不可变 | 可变 |
|--------|------|
| int, float, bool, str, tuple | list, dict, set |

**关键说明：** Python 的"一切皆对象"体现在每个值都是 `PyObject` 结构体，`id()` 返回其在堆上的内存地址，`type()` 返回其 `ob_type` 指针指向的类型对象。`b = a` 不复制对象，只复制指向该对象的指针（引用计数+1）。immutable 对象的"修改"实际是创建新对象，这是 Python 内存模型的核心设计。

#### 1.3.3.6 Python 内存大小逻辑与对齐

**一个空对象占多少字节？**

```
import sys

# 查看各种对象的大小
print(sys.getsizeof(42))          # 28 bytes  ← 一个 int
print(sys.getsizeof(3.14))        # 24 bytes  ← 一个 float
print(sys.getsizeof(""))          # 49 bytes  ← 空字符串
print(sys.getsizeof("a"))         # 50 bytes  ← 1 字符
print(sys.getsizeof([]))           # 56 bytes  ← 空列表
print(sys.getsizeof([1]))          # 64 bytes  ← 1 元素（56 + 8）
print(sys.getsizeof({}))           # 72 bytes  ← 空字典
print(sys.getsizeof(object()))    # 16 bytes  ← 最小对象
print(sys.getsizeof(None))        # 16 bytes  ← None 对象
```

**为什么大小不是整数（如 28、24、56）？**

Python 使用 **内存对齐（Memory Alignment）**——CPU 访问对齐的内存地址效率远高于未对齐的：

```
未对齐（CPU 需要 2 次内存访问 + 拼接）：
地址: 0x01  0x02  0x03  0x04  0x05  0x06  0x07  0x08
数据: [── int ──] [── int ──] ← 跨越 2 个内存块

对齐后（CPU 1 次内存访问）：
地址: 0x00  0x01  0x02  0x03 | 0x04  0x05  0x06  0x07
数据: [── int ──]            | [── int ──]
```

CPython 的对齐规则（64 位系统）：

```
对齐单位 = sizeof(void*) = 8 bytes

对象的真正大小 = ceil(实际数据大小 / 8) * 8

例如 int(42):
  PyObject_HEAD (ob_refcnt + ob_type) = 8 + 8 = 16
  ob_digit (存储数值的数组)           = 4 （但需要对齐到 8）
  ceil(20 / 8) * 8 = 24            ← 不对！
  
  实际：PyLongObject 包含：
  ob_refcnt: 8
  ob_type:   8
  ob_size:   4（digit 数量）
  ob_digit:  4（1 个 30-bit digit，实际只占 4 字节）
  合计 24 + 4 = 28（包含 padding）
```

**各对象内存布局详解：**

```c
// PyObject（最小对象头部，16 bytes）
struct _object {
    Py_ssize_t ob_refcnt;     // 8 bytes  ← 引用计数
    PyTypeObject *ob_type;    // 8 bytes  ← 类型指针
};

// PyVarObject（变长对象头部，24 bytes = 16 + 8）
typedef struct {
    PyObject ob_base;         // 16 bytes ← 继承 PyObject
    Py_ssize_t ob_size;       // 8 bytes  ← 元素个数
} PyVarObject;

// PyLongObject (int, 28 bytes)
struct _longobject {
    PyVarObject ob_base;      // 24 bytes ← ob_refcnt + ob_type + ob_size
    digit ob_digit[1];        // 4 bytes  ← 实际数值（30-bit 小整数）
    // 28 bytes = 24 + 4 → 无需 padding（已是 4 的倍数）
};

// PyFloatObject (float, 24 bytes)
typedef struct {
    PyObject ob_base;         // 16 bytes
    double ob_fval;           // 8 bytes  ← C double
    // 24 bytes = 16 + 8 → 完全对齐
} PyFloatObject;

// PyListObject (空列表, 56 bytes)
typedef struct {
    PyVarObject ob_base;      // 24 bytes
    PyObject **ob_item;       // 8 bytes  ← 指向元素数组的指针
    Py_ssize_t allocated;     // 8 bytes  ← 已分配容量
    // 40 bytes 头部
    // + 预分配数组... PyListObject 实际结构含额外字段
    // 实际 sys.getsizeof([]) = 56 bytes
} PyListObject;
```

**额外开销来源总结：**

```
Python 对象 vs C 原生类型 的内存开销对比：

C int (4 bytes)        → Python int (28 bytes)   → 7 倍
C double (8 bytes)     → Python float (24 bytes)  → 3 倍
C char[] (N bytes)     → Python str (49 + N)      → ~49 bytes 固定开销
C struct (sum fields)  → Python object (16+)      → 16 bytes 最小头部

开销来源：
1. ob_refcnt (8 bytes)  ← GC 必需
2. ob_type (8 bytes)    ← 运行时类型识别
3. ob_size (8 bytes)    ← 变长对象必需
4. 内存对齐 padding    ← CPU 性能优化
5. 预分配 (list/dict)   ← 扩容优化
```

**为什么 Python 愿意付出这个代价？**

```python
# 没有 ob_type → 无法实现鸭子类型
def process(thing):
    return thing.quack()  # 运行时通过 ob_type 查找 quack

# 没有 ob_refcnt → 无法自动内存管理
# Java 也没有引用计数（用 GC root tracing）
# Python 引用计数的好处：对象立即释放，GC 做循环引用兜底

# 没有对齐 → 性能灾难
# CPU 读取未对齐地址会触发异常或多次内存访问
```

**对齐的实际影响：**

```python
import struct

# CPU 访问对齐地址：1 次内存读取
address = 0x1000  # 8 的倍数
# 读取 8 bytes → 1 次总线事务 ✅

# CPU 访问未对齐地址：2 次内存读取 + 拼接
address = 0x1001  # 不是 8 的倍数
# 需读取 0x1000 + 0x1008 再拼接 → 至少 2 次 ❌
```

### 1.3.4 可视化流程图

```
变量赋值：a = 42

[栈帧]                       [堆内存]
   a ──────────────────→ +-----------+
                         | ob_ref: 1 |
                         | ob_type   |──→ <class 'int'>
                         | ob_digit  |──→ 42
                         +-----------+
```

### 1.3.5 常见错误

**以下是常见的错误示例：**
```python
# 误以为 = 是拷贝
original = [1, 2, 3]
copied = original
copied.append(4)
print(original)  # [1, 2, 3, 4] 也被改了！

# 正确：
copied = original.copy()
import copy
copied = copy.deepcopy(original)

# 可变默认参数
def add_item(item, lst=[]):
    lst.append(item)
    return lst

print(add_item(1))  # [1]
print(add_item(2))  # [1, 2]  ← 同一个列表！

# 正确：
def add_item(item, lst=None):
    if lst is None:
        lst = []
    lst.append(item)
    return lst
```

---

## 1.4 函数

### 1.4.1 一句话核心本质

**Python 函数是一等公民对象——用 `def` 定义、用 `()` 调用，可赋值给变量、传入参数、嵌套定义、作为返回值。**

### 1.4.2 函数的定义与调用

**下面是 函数的定义与调用 的代码示例：**
```python
# 定义函数：def 函数名(参数):
def greet(name):
    """向某人打招呼"""  # 文档字符串 docstring
    return f"Hello, {name}!"

# 调用函数
result = greet("AI")
print(result)  # Hello, AI!

# 无参数函数
def say_hello():
    print("Hello!")

say_hello()  # Hello!

# 无 return → 返回 None
def do_nothing():
    pass

print(do_nothing())  # None
```

**Java 对比：**
- Java：`public String greet(String name) { return "Hello " + name; }`
- Python：`def greet(name): return f"Hello {name}!"`
- Python 不需要声明 `public/static`，不需要写返回值类型（可选 typing），没有 `{}` 用缩进

### 1.4.3 参数系统（全面）

Python 的参数系统比 Java 灵活得多，有 5 种参数类型：

```python
# 1. 位置参数（最常用）：按顺序匹配
def greet(first_name, last_name):
    return f"{first_name} {last_name}"

greet("John", "Doe")     # "John Doe"

# 2. 默认参数：在 Java 中需要方法重载
def greet(first_name, greeting="Hello"):
    return f"{greeting}, {first_name}!"

greet("AI")              # "Hello, AI!"
greet("AI", "Hi")        # "Hi, AI!"

# 3. 关键字参数：调用时指定参数名，Java 不支持
def introduce(name, age, city):
    return f"{name}, {age}岁, 来自{city}"

introduce(city="北京", name="AI", age=5)  # 顺序随意

# 4. 可变位置参数 *args → 接收不定数量的位置参数（Java 的 ...）
def sum_all(*numbers):
    total = 0
    for n in numbers:
        total += n
    return total

print(sum_all(1, 2, 3, 4, 5))  # 15

# 5. 可变关键字参数 **kwargs → 接收不定数量的关键字参数
def create_profile(**info):
    return info  # 返回 dict

print(create_profile(name="AI", age=5, role="assistant"))
# {'name': 'AI', 'age': 5, 'role': 'assistant'}

# 6. 强制关键字参数（* 后面的参数只能用关键字传）
def configure(host, port, *, ssl=True, timeout=30):
    # ssl 和 timeout 只能用 configure(..., ssl=True) 传
    pass

configure("localhost", 8080, ssl=True, timeout=60)  # 正确
# configure("localhost", 8080, True, 60)  # 错误！

# 7. 组合使用（顺序必须固定）
def complex_func(a, b, *args, c=10, **kwargs):
    print(f"a={a}, b={b}")
    print(f"args={args}")
    print(f"c={c}")
    print(f"kwargs={kwargs}")

complex_func(1, 2, 3, 4, c=20, name="AI", task="RAG")
# a=1, b=2
# args=(3, 4)
# c=20
# kwargs={'name': 'AI', 'task': 'RAG'}
```

**参数顺序规则（必须遵守）：**
```
def func(位置参数, 默认参数, *args, 强制关键字, **kwargs):
```

### 1.4.4 返回值

**下面是 返回值 的代码示例：**
```python
# 单个返回值
def square(x):
    return x * x

# 多个返回值 → 实际上返回一个 tuple（Python 特有）
def get_user():
    return "Alice", 30, "alice@email.com"

name, age, email = get_user()  # 解包赋值
print(name, age, email)  # Alice 30 alice@email.com

# 等价于 Java 需要创建一个类或 Object[]
# public Object[] getUser() { return new Object[]{"Alice", 30}; }

# 没有 return → 返回 None
def log(msg):
    print(f"[LOG] {msg}")  # 没有 return

result = log("test")
print(result)  # None

# 提前 return
def find_first_even(numbers):
    for n in numbers:
        if n % 2 == 0:
            return n  # 找到立即返回
    return None  # 没找到
```

### 1.4.5 函数是一等公民

**这是 Python 和 Java 最核心的区别之一：**

```python
# 1. 函数可以赋值给变量
def add(a, b):
    return a + b

my_func = add          # 不加 ()，只是引用函数
print(my_func(3, 4))   # 7

# 2. 函数可以作为参数传入
def apply(func, x, y):
    return func(x, y)

print(apply(add, 3, 4))  # 7

# 3. 函数可以作为返回值
def make_adder(n):
    def adder(x):
        return x + n
    return adder  # 返回内部定义的函数对象

add_5 = make_adder(5)
print(add_5(10))  # 15

# 4. 函数可以存储在容器中
operations = {
    "add": add,
    "mul": lambda a, b: a * b,
    "pow": lambda a, b: a ** b,
}
print(operations["mul"](3, 4))  # 12
```

**Java 对比：**
- Java 8 之前：函数不是对象，只能用接口 + 匿名类模拟
- Java 8+：函数式接口 + lambda（底层还是接口实现类）
- Python：函数是原生的 `function` 类型对象

```python
# 验证函数是对象
def foo():
    pass

print(type(foo))          # <class 'function'>
print(id(foo))            # 内存地址
print(foo.__name__)       # "foo"
print(foo.__code__)       # 字节码对象
```

### 1.4.5b 方法的秘密：函数怎么变成方法的？

**小白版：`obj.method()` 背后发生了什么**

当你在 Python 里写 `s.run("test")`，这不是直接调 `run(s, "test")`——中间有个隐藏步骤把"函数"变成了"方法"。下面从零讲清楚。

#### ① 函数 vs 方法——区别在哪？

```python
# 函数：定义在类外面的，直接调
def greet(name):
    return f"Hello {name}"

greet("Tom")  # 直接调，传一个参数

# 方法：定义在类里面的，通过实例调
class Service:
    def run(self, data):
        return f"{self} 处理 {data}"

s = Service()
s.run("test")  # 传了一个参数"test"，但 run 要两个参数（self, data）？
               # 谁传了 self？答案是 Python 自动传的
```

**问题：** `run(self, data)` 需要 2 个参数，你只传了 `"test"` 一个，为什么没报错？

答案藏在 `s.run` 和 `Service.run` 的区别里：

```python
class Service:
    def run(self, data):
        return f"{self} 处理 {data}"

s = Service()

# 通过类访问 run → 原始函数
print(Service.run)  # <function Service.run at 0x...>
print(type(Service.run))  # <class 'function'>

# 通过实例访问 run → 绑定方法
print(s.run)       # <bound method Service.run of <__main__.Service object at 0x...>>
print(type(s.run)) # <class 'method'>

# 区别就在这里！
print(Service.run(s, "test"))  # 手动传 self → 3 个参数"函数"调用
print(s.run("test"))           # 自动传 self → 1 个参数"方法"调用，结果一样
```

**结论：** `s.run` 返回的不是原始函数，而是一个"绑定方法"对象，它已经记住了 `s` 就是 `self`。

#### ② 绑定方法长什么样？——`im_self` 和 `im_func`

绑定方法对象（`PyMethodObject`）内部只有两个关键东西：

```python
class Service:
    def run(self, data):
        return f"{self} 处理 {data}"

s = Service()
bound_method = s.run

# 绑定方法内部：
print(bound_method.__func__)   # <function Service.run at 0x...> ← 原始函数
print(bound_method.__self__)   # <__main__.Service object at 0x...> ← s 实例
```

```
内存里：
s.run 返回的这个"绑定方法"对象：
┌──────────────────────────────────┐
│ PyMethodObject                   │
├──────────────────────────────────┤
│ im_func (__func__) ──→ run 函数  │  ← 真正的函数体（字节码）
│ im_self (__self__) ──→ s 实例    │  ← 被绑定的实例
└──────────────────────────────────┘

当你调 s.run("test")：
① Python 看到这是绑定方法
② 调 im_func(im_self, "test")  → 就是 run(s, "test")
③ im_self → s, "test" → data
```

验证：
```python
s = Service()
bm = s.run

# 手动模拟绑定方法的工作方式
original_func = bm.__func__    # 取出原始函数
instance = bm.__self__         # 取出绑定的实例

# 下面两行完全等价：
original_func(instance, "test")  # 手动传 self
bm("test")                       # 自动传 self（绑定方法干的）
```

#### ③ 关键问题：为什么 `s.run` 能返回绑定方法？——descriptor 协议

`run` 是定义在类里的函数，`s.run` 怎么知道要返回绑定方法而不是原始函数？

答案在**函数的 `__get__` 方法**——所有函数都自带 `__get__`：

```python
def my_func():
    pass

# 函数也有 __get__ 方法！
print(hasattr(my_func, "__get__"))  # True

# 有 __get__ 的对象叫"descriptor"（描述符）
# 当它被作为类属性访问时，Python 触发 __get__
```

**descriptor 协议**是 Python 属性查找的核心机制，只有 3 个方法：

```
descriptor 协议：
  obj.attr 访问时，如果 attr 的值有 __get__ 方法，就调它

__get__(self, instance, owner)
  self    → 属性本身（这里是函数）
  instance → 访问属性的实例（如果是类访问则为 None）
  owner   → 实例的类

三种情况：
  s.run   → 触发 __get__(run, s, Service) → 返回绑定方法（im_self=s）
  Service.run → 触发 __get__(run, None, Service) → 返回原始函数
  run      → 不触发 __get__（直接访问变量）
```

```
s.run 的完整查找链：

① s.run
   → s.ob_type → Service（PyTypeObject）
   → Service.tp_getattro → PyObject_GenericGetAttr
   → 在 Service.tp_dict（就是 Service.__dict__）里查 "run"
      ┌─────────────────────────────┐
      │ Service.__dict__            │  ← 这就是 tp_dict 在 Python 层的名字
      │   "run" → <function>        │
      │   "__init__" → <function>   │
      │   ...                       │
      └─────────────────────────────┘
   → 找到 run = <function>

② 检查 run 是不是 descriptor：
   → run 有 __get__ 方法 ✓（所有函数都有）
   → 调 run.__get__(s, Service)
   → 返回 PyMethodObject(im_func=run, im_self=s)

③ s.run 现在指向这个绑定方法对象
```

```python
# 手动模拟 descriptor 协议
class Service:
    def run(self, data):
        return f"{self} 处理 {data}"

s = Service()

# 第 1 步：从类里取出 run
raw_func = Service.__dict__["run"]
print(raw_func)       # <function Service.run at 0x...>

# 第 2 步：手动调 __get__ 模拟绑定
bound = raw_func.__get__(s, Service)
print(bound)          # <bound method Service.run of <__main__.Service object at 0x...>>
print(bound("test"))  # 和 s.run("test") 完全一样
```

#### ④ `tp_dict` 到底是什么？

`tp_dict` 是 `PyTypeObject` 结构体里的一个字段（1.3.3.2 讲过的），对应 Python 层的 `类.__dict__`。它就是一个**字典**，存着类的所有属性和方法：

```python
class Service:
    cls_var = 42
    def run(self):
        pass

# Service.__dict__ 就是 tp_dict 在 Python 层的窗口
print(type(Service.__dict__))     # <class 'mappingproxy'>
print(dict(Service.__dict__))     # 转成普通 dict 看

# 输出：
# {
#   "__module__": "__main__",
#   "cls_var": 42,
#   "run": <function Service.run at 0x...>,
#   "__dict__": <attribute '__dict__' of 'Service' objects>,
#   "__weakref__": <attribute '__weakref__' of 'Service' objects>,
#   "__doc__": None
# }
```

`Service.__dict__` 就是 C 层的 `Service.tp_dict`。当 Python 查找 `s.run` 时，它先在这个字典里找 `"run"`，找到后检查它是不是 descriptor。

#### ⑤ 完整流程图：`s.run("test")` 从头到尾

```
你写：s.run("test")

Step 1 [属性查找]:
  Python 找 s.run
  → s.ob_type → Service（PyTypeObject）
  → Service.__dict__（tp_dict）→ 找 "run"
  → 找到 <function run at 0x...>

Step 2 [descriptor 检查]:
  run 有 __get__ 吗？有（所有函数都有）
  → 调 run.__get__(s, Service)
  → 返回 PyMethodObject(im_func=run, im_self=s)

Step 3 [调用]:
  Python 看到这是绑定方法
  → 等价于调 run(s, "test")
  → self=s, data="test"
```

```
内存里完整的样子：

Service (PyTypeObject)
  ┌────────────────────────────┐
  │ tp_dict ("__dict__")       │
  │   "run" ───────────────────┼────┐
  └────────────────────────────┘    │
                                    ▼
                            ┌───────────────┐
                            │ <function run> │  ← 原始函数
                            │ 有 __get__ ✓  │
                            └───────┬───────┘
                                    │
          s.run("test") ──→ run.__get__(s, Service)
                                    │
                                    ▼
                            ┌───────────────────────┐
                            │ PyMethodObject         │
                            │   im_func → run        │
                            │   im_self → s          │
                            └───────────────────────┘
                                    │
                          调 im_func(im_self, "test")
                          → run(s, "test")
                          → s 收到 self, "test" 收到 data
```

#### ⑥ 用 Java 类比理解

```java
// Java 里没有"绑定方法"这种东西
class Service {
    String run(String data) {
        return this + " 处理 " + data;
    }
}

Service s = new Service();
s.run("test");
// 编译期就确定了：invokevirtual Service.run
// JVM 自动把 s 压到局部变量 0（this）
// 不需要"绑定方法"对象，编译时就已经安排好了

// Python 不同：
// s.run 是运行时从 dict 里查出来的
// 查出来的是原始函数，需要"包一层"记住 s 是谁
// 这层包装就是绑定方法（PyMethodObject）
```

**Python 要绑定方法根本原因：** Java 编译时就确定了 `s.run` 就是 `Service.run`、`this=s` 是 JVM 自动压栈的。Python 里 `s.run` 是**运行时**从类的 `tp_dict` 里查出来的——查出来的是原始函数，不知道 `s` 是谁。所以需要 descriptor 协议在中间包一层绑定方法，把 `s` 记住。这是动态语言的代价，也是为什么 `s.run` 比直接调函数慢一点的原因。

```
Python 执行 def 语句时，不是"声明"，而是"执行"：

def square(x):
    return x * x

执行 def：
        ↓
创建 PyFunctionObject 实例
        ↓
设置 __code__（编译好的字节码）
设置 __defaults__（默认参数值元组）
设置 __closure__（闭包变量，没有就是 None）
设置 __globals__（全局变量引用）
设置 __name__（函数名 "square"）
        ↓
将函数对象赋值给变量 "square"

调用 square(5) 时：
        ↓
创建新的栈帧 (frame)  ← Java 的栈帧
        ↓
将 5 绑定到参数 x
        ↓
执行字节码：LOAD_FAST x → LOAD_CONST x → BINARY_MULTIPLY → RETURN_VALUE
        ↓
销毁栈帧
        ↓
返回结果 25
```

**代码演示：函数在 Python 中是 `function` 类型的对象，拥有和普通对象一样的属性。通过 `square.__name__`（函数名）、`square.__doc__`（文档字符串）、`square.__code__`（编译后的字节码对象）可以查看函数的所有内部元信息。`dis.dis(square)` 可直接反编译该函数的字节码，验证上图的执行流程。**

```python
# 查看函数对象的内部结构
def square(x):
    """计算 x 的平方"""
    return x * x

print(square.__name__)       # "square"
print(square.__doc__)        # "计算 x 的平方"
print(square.__code__)       # <code object square at ...>
print(square.__defaults__)   # None（没有默认参数）
print(square.__globals__)    # 全局变量 dict
print(square.__closure__)    # None（没有闭包）

import dis
dis.dis(square)
# 2           0 LOAD_FAST                0 (x)
#             2 LOAD_FAST                0 (x)
#             4 BINARY_MULTIPLY
#             6 RETURN_VALUE
```

### 1.4.7 作用域规则（LEGB）

**下面是 作用域规则（LEGB） 的代码示例：**
```python
# Python 变量查找顺序：Local → Enclosing → Global → Built-in

x = "global"  # Global 作用域

def outer():
    x = "enclosing"  # Enclosing 作用域

    def inner():
        x = "local"  # Local 作用域
        print(x)     # "local"

    inner()
    print(x)         # "enclosing"

outer()
print(x)             # "global"

# global 关键字：修改全局变量
count = 0

def increment():
    global count     # 不声明的话，count += 1 会创建局部变量
    count += 1

# nonlocal 关键字：修改外层函数的变量
def make_counter():
    count = 0
    def counter():
        nonlocal count
        count += 1
        return count
    return counter

c = make_counter()
print(c())  # 1
print(c())  # 2
```

**Java 对比：**
- Java 的作用域：`{}` 块级作用域
- Python：函数级作用域（if/for/while 不创建新作用域）

```python
# Python 的块不创建作用域
if True:
    x = 42  # x 属于函数/全局作用域
print(x)    # 42 ← Java 会编译错误
```

### 1.4.8 匿名函数（lambda）

**下面是 匿名函数（lambda） 的代码示例：**
```python
# lambda 参数: 表达式  → 只能写一个表达式，不能写语句

# 简单 lambda
square = lambda x: x ** 2
print(square(5))  # 25

# 多参数 lambda
add = lambda a, b: a + b
print(add(3, 4))  # 7

# 在排序中作为 key
students = [
    {"name": "Alice", "score": 95},
    {"name": "Bob", "score": 87},
    {"name": "Charlie", "score": 92},
]
students.sort(key=lambda s: s["score"], reverse=True)
# Java: students.sort(Comparator.comparing(Student::getScore).reversed())

# 在 map/filter 中使用（但 Python 更推荐列表推导式）
numbers = [1, 2, 3, 4, 5]
squares = list(map(lambda x: x**2, numbers))
evens = list(filter(lambda x: x % 2 == 0, numbers))

# 推荐：列表推导式（Pythonic）
squares = [x**2 for x in numbers]
evens = [x for x in numbers if x % 2 == 0]
```

### 1.4.9 类型注解（可读性 + 静态检查）

**下面是 类型注解（可读性 + 静态检查） 的代码示例：**
```python
# 为参数和返回值添加类型注解（不影响运行）
def greet(name: str, age: int = 0) -> str:
    return f"{name} is {age} years old"

# 复杂类型注解
from typing import List, Dict, Optional, Tuple

def process(items: List[str]) -> Dict[str, int]:
    return {item: len(item) for item in items}

def find(user_id: int) -> Optional[str]:
    # 返回值可能是 str 或 None
    return "Alice" if user_id == 1 else None

# 注解只在 IDE/mypy 中起作用，运行时不检查
greet(42, "hello")  # 正常运行！但 IDE 会警告
```

### 1.4.10 高阶函数与闭包

**下面是 高阶函数与闭包 的代码示例：**
```python
# 闭包：内部函数捕获外部函数的变量
def make_multiplier(n):
    """返回一个乘以 n 的函数"""
    def multiplier(x):
        return x * n  # n 被 multiplier 捕获（闭包）
    return multiplier

double = make_multiplier(2)
triple = make_multiplier(3)

print(double(10))  # 20
print(triple(10))  # 30

# 查看闭包捕获的变量
print(double.__closure__[0].cell_contents)  # 2

# 闭包在 AI 项目中的应用
def create_api_client(base_url: str, api_key: str):
    """创建一个带认证的 API 客户端"""
    def request(endpoint: str, data: dict = None):
        url = f"{base_url}{endpoint}"
        headers = {"Authorization": f"Bearer {api_key}"}
        print(f"请求 {url}")
        # requests.post(url, headers=headers, json=data)
        return {"status": 200, "data": "mock"}
    return request

openai_client = create_api_client("https://api.openai.com/v1", "sk-xxx")
result = openai_client("/chat/completions", {"model": "gpt-4"})
```

### 1.4.11 列表推导式（Python 专属）

**下面是 列表推导式（Python 专属） 的代码示例：**
```python
# 列表推导式 = 结合了 map + filter 的语法糖
# [表达式 for 变量 in 可迭代对象 if 条件]

numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

# 基本
squares = [n**2 for n in numbers]
# [1, 4, 9, 16, 25, 36, 49, 64, 81, 100]

# 带条件
evens = [n for n in numbers if n % 2 == 0]
# [2, 4, 6, 8, 10]

# 表达式可以是任意复杂
labels = ["even" if n % 2 == 0 else "odd" for n in numbers]
# ['odd', 'even', 'odd', ...]

# 嵌套循环
pairs = [(x, y) for x in range(3) for y in range(3)]
# [(0,0), (0,1), (0,2), (1,0), (1,1), (1,2), (2,0), (2,1), (2,2)]

# 字典推导式，n自动作为key
squares_dict = {n: n**2 for n in range(5)}
# {0: 0, 1: 1, 2: 4, 3: 9, 4: 16}

# 集合推导式
unique_lengths = {len(word) for word in ["hello", "world", "hi", "python"]}
# {2, 5, 6}

# 生成器表达式（惰性，节约内存）
lazy_squares = (n**2 for n in numbers)  # 不是 list，是 generator
print(type(lazy_squares))  # <class 'generator'>
```

### 1.4.12 递归

**下面是 递归 的代码示例：**
```python
# Python 递归（注意：Python 有递归深度限制）
def factorial(n):
    if n <= 1:
        return 1
    return n * factorial(n - 1)

print(factorial(5))  # 120

# 查看递归限制
import sys
print(sys.getrecursionlimit())  # 1000

# 设置递归限制
sys.setrecursionlimit(2000)
```

### 1.4.13 文档字符串（docstring）

#### 一句话核心本质

**文档字符串（docstring）是定义在模块/函数/类/方法开头的字符串字面量——Python 编译器在编译时将其存入对象的 `__doc__` 属性，供 `help()`、IDE 提示、文档生成工具使用。注释被编译器丢弃，docstring 保留在运行时。**

#### Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 文档形式 | Javadoc 注释 `/** ... */` | docstring `"""..."""` |
| 存储方式 | 编译到 `.class` 文件的可选 `@Deprecated` 等注解 | 运行时 `__doc__` 属性 |
| 生成工具 | Javadoc | Sphinx / pydoc |
| 类型信息 | `@param name description` | Google/NumPy/Sphinx 三种风格 |
| 访问方式 | IDE 悬停读源码 | `help(obj)` / `obj.__doc__` |
| 编译时 | 默认不保留注释 | 必须保留（运行时可用） |

#### 技术原理：`__doc__` 是如何产生的

```
Python 编译器处理 def/class/module 时：

源码:                      字节码:
def foo():                  LOAD_CONST 0 (<code object foo>)
    """Do stuff"""          LOAD_CONST 1 ('foo')
    return 42               MAKE_FUNCTION
                            STORE_NAME 'foo'

编译阶段（CPython/compile.c）：
1. 解析器生成 AST，看到 def 节点的第一个子节点是 Expr(Str)
2. 编译器将该字符串存入 code object 的 co_consts[0]
3. MAKE_FUNCTION 指令会检查 co_consts[0]
4. 如果是字符串，自动设置 func.__doc__ = co_consts[0]

运行时真相：
>>> def f():
...     """hello"""
...     pass
>>> f.__doc__
'hello'
>>> f.__code__.co_consts   # ('hello', None)
('hello', None)

关键区别：
- # 注释：词法分析阶段直接被丢弃，不存在于字节码中
- """docstring"""：被编译为常量，是字节码的一部分
```

#### 三种主流格式

```python
# ─── Google Style（推荐，简洁清晰） ───
def calculate_snr(signal_power: float, noise_power: float) -> float:
    """计算信噪比 (SNR)。

    Args:
        signal_power: 信号功率 (mW)
        noise_power: 噪声功率 (mW)

    Returns:
        SNR 值 (dB)

    Raises:
        ValueError: 如果噪声功率 <= 0

    Example:
        >>> calculate_snr(100, 1)
        20.0
    """
    if noise_power <= 0:
        raise ValueError("噪声功率必须大于 0")
    return 10 * math.log10(signal_power / noise_power)

# ─── NumPy Style（AI/科学计算社区常用） ───
def calculate_snr_numpy(signal_power, noise_power):
    """
    Calculate Signal-to-Noise Ratio.

    Parameters
    ----------
    signal_power : float
        信号功率 (mW)
    noise_power : float
        噪声功率 (mW)

    Returns
    -------
    float
        SNR in dB

    Raises
    ------
    ValueError
        If noise_power <= 0
    """
    return 10 * math.log10(signal_power / noise_power)

# ─── Sphinx/reST Style（Java 开发者最熟悉） ───
def calculate_snr_sphinx(signal_power, noise_power):
    """计算信噪比。

    :param signal_power: 信号功率 (mW)
    :type signal_power: float
    :param noise_power: 噪声功率 (mW)
    :type noise_power: float
    :returns: SNR 值 (dB)
    :rtype: float
    :raises ValueError: 噪声功率 <= 0
    """
    return 10 * math.log10(signal_power / noise_power)
```

#### 进阶用法

```python
# ─── 模块级 docstring ───
"""AI 通信模块。

该模块提供无线通信系统的信噪比、误比特率等计算函数。

典型用法:
    >>> from ai_comm import calculate_snr
    >>> calculate_snr(100, 1)
    20.0
"""

# ─── 类 docstring ───
class Modem:
    """调制解调器基类。

    Attributes:
        modulation: 调制方式 ('QPSK', '16QAM', '64QAM')
        bit_rate: 比特率 (bps)

    Todo:
        - 支持 OFDM
        - 添加 MIMO 支持
    """

# ─── __doc__ 是可变属性 ───
def func():
    """原始文档"""
    pass

func.__doc__ = "动态修改的文档"
print(func.__doc__)  # "动态修改的文档"

# ─── inspect.getdoc() 自动清理缩进 ───
import inspect

class Outer:
    class Inner:
        """内层类文档"""
        pass

print(Outer.Inner.__doc__)            # "内层类文档"
print(repr(inspect.getdoc(Outer.Inner)))  # 自动去除多余缩进

# ─── dataclass 自动生成 docstring ───
from dataclasses import dataclass

@dataclass
class Config:
    """AI 模型配置"""
    model_name: str = "gpt-4"
    temperature: float = 0.7

print(Config.__doc__)  # "AI 模型配置"
```

#### 常见错误

**错误 1：用注释代替 docstring**

```python
# ❌ 错误：注释无法被 help() 读取
def calculate(x, y):
    # 计算两个数的和  ← 这是注释，运行时不存在
    return x + y

help(calculate)  # 只显示函数签名，没有说明

# ✅ 正确：用 docstring
def calculate(x, y):
    """返回 x 和 y 的和"""
    return x + y

help(calculate)  # 显示完整文档
```

**错误 2：docstring 不是第一个语句**

```python
# ❌ 错误：print 在 docstring 之前
def bad():
    print("start")  # 这不是第一个语句了！
    """这是文档？不，这是个无用的字符串表达式"""
    return 42

print(bad.__doc__)  # None  ← Python 不会把它当 docstring

# ✅ 正确：docstring 必须是第一个表达式
def good():
    """这是真正的文档"""
    print("start")
    return 42
```

**错误 3：字符串字面量而非三引号字符串**

```python
# ❌ 错误：单引号字符串作为 docstring 也可以，但不推荐
def f():
    'short doc'  # 语法正确，但可读性差
    pass

print(f.__doc__)  # 'short doc'

# ✅ 正确：统一用 """ 三引号
def f():
    """推荐写法，支持换行"""
    pass
```

#### AI 场景案例：自动生成 API 文档

```python
import ast
import inspect

def auto_document(obj) -> str:
    """从对象及其 docstring 生成 Markdown 文档。"""
    doc = inspect.getdoc(obj) or "无文档"
    source = inspect.getsource(obj)
    return f"## {obj.__name__}\n\n{doc}\n\n```python\n{source}\n```"

def train_model(data: list, epochs: int = 10) -> float:
    """训练 AI 模型。

    Args:
        data: 训练数据列表
        epochs: 训练轮数，默认 10

    Returns:
        训练后的准确率
    """
    return 0.95

# 生成文档
print(auto_document(train_model))
```

### 1.4.14 常见错误

**以下是常见的错误示例：**
```python
# 1. lambda 闭包陷阱
funcs = [lambda: i for i in range(5)]
print([f() for f in funcs])  # [4,4,4,4,4] ← 所有函数都返回最后一个 i
```

**这行代码是每个 Python 开发者都会踩的坑，本质是"闭包捕获变量引用而非值"：**

#### 先理解 cell 是什么——它是 Python 闭包的"桥接对象"

```python
def outer():
    x = 42
    def inner():
        return x
    return inner

f = outer()
print(f.__closure__)       # (<cell at 0x...: int object at 0x...>,)
print(f.__closure__[0])    # <cell at 0x...: int object at 0x...>
print(f.__closure__[0].cell_contents)  # 42 ← 真正存的值
```

`cell` 是 CPython 内部的一个**中间容器**，用于在嵌套函数之间传递自由变量。当 `outer()` 执行 `def inner()` 时，`inner` 引用了外部变量 `x`，CPython 不会把变量值直接复制给 `inner`——它创建一个 `cell` 对象，`inner` 通过 `__closure__` 拿到 cell，调用时从 cell 读取当前值。

回到 `funcs = [lambda: i for i in range(5)]`，问题出在**作用域**上：

```
Python 作用域规则：
列表推导式 [] 在 Python 3 中不是新作用域！
（生成器推导式 () 才是）

所以变量 i 不属于推导式，而属于外层函数/模块的同一个作用域

内存模型：
外层作用域的变量 i
         │
         ▼
    ┌──────────────────┐
    │  cell 对象        │ ←── 只创建了 1 个 cell！
    │  cell_contents: 4 │ ←── lambda_0.__closure__[0]
    │                   │ ←── lambda_1.__closure__[0]
    │                   │ ←── lambda_2.__closure__[0]
    │                   │ ←── lambda_3.__closure__[0]
    │                   │ ←── lambda_4.__closure__[0]
    └──────────────────┘
         │
  for i in range(5):
      i → 0 → 1 → 2 → 3 → 4（每次都修改同一个 cell 的内容）
                            ↑
                     循环结束时 i=4，cell 里存的是 4

所有 5 个 lambda 的 __closure__[0] 指向的是同一个 cell 对象！
调用时读取 cell_contents，得到的是最新的 i=4
```

**验证：**

```python
funcs = [lambda: i for i in range(3)]
# 所有 lambda 共享同一个 cell
print(funcs[0].__closure__[0] is funcs[1].__closure__[0])  # True
print(funcs[1].__closure__[0] is funcs[2].__closure__[0])  # True
print(funcs[0].__closure__[0].cell_contents)  # 2
```

**对比 `make_lambda(i)` 修复版——为什么函数调用能创建独立 cell？**

```python
def make_lambda(x):        # ① x 是形参，属于 make_lambda 的局部作用域
    return lambda: x       # ② lambda 捕获的 x 是当前的 x

funcs = [make_lambda(i) for i in range(3)]
#         ↑ 每次调用 make_lambda，都创建一个新的栈帧
```

```
循环调用过程（重点对比变量绑定）：

错误版（列表推导式直接 lambda）：
[同一个作用域]  变量 i
    ├─ 迭代0: 创建 lambda#0，闭包引用 i（同一个变量）
    ├─ 迭代1: 创建 lambda#1，闭包引用 i（同一个变量）
    ├─ 迭代2: 创建 lambda#2，闭包引用 i（同一个变量）
    └─ 循环结束: i = 2，所有 lambda 读到 2

修复版（make_lambda(i) 调用）：
[外层作用域]  i ← 循环变量
    ├─ 迭代0: make_lambda(0) 被调用
    │          ┌─ [新栈帧 #0]  形参 x 绑定到值 0  ← 创建新 cell#0，内容=0
    │          └─ 返回 lambda: x  → 捕获 cell#0
    ├─ 迭代1: make_lambda(1) 被调用
    │          ┌─ [新栈帧 #1]  形参 x 绑定到值 1  ← 创建新 cell#1，内容=1
    │          └─ 返回 lambda: x  → 捕获 cell#1
    ├─ 迭代2: make_lambda(2) 被调用
    │          ┌─ [新栈帧 #2]  形参 x 绑定到值 2  ← 创建新 cell#2，内容=2
    │          └─ 返回 lambda: x  → 捕获 cell#2
    └─ 循环结束: 但 cell#0=0, cell#1=1, cell#2=2 互相独立
```

**关键区别一句话**：`for` 循环不创建新作用域——所有迭代共享同一个变量 `i`。但**函数调用一定创建新作用域**——每次 `make_lambda(i)` 都新开一个栈帧，形参 `x` 属于那个栈帧，每个栈帧的 `x` 是不同的绑定。CPython 为每个绑定创建独立的 cell 对象。

```python
# 验证：每个 lambda 拥有独立的 cell
print(funcs[0].__closure__[0] is funcs[1].__closure__[0])  # False ← 不同 cell！
print(funcs[0].__closure__[0].cell_contents)  # 0
print(funcs[1].__closure__[0].cell_contents)  # 1
```

**Java 为什么没这问题？**

```java
// Java：lambda 要求变量是 effectively final，编译就禁止修改
for (int i = 0; i < 5; i++) {
    // IntSupplier s = () -> i;  // 编译错误！i 不是 final
    int finalI = i;
    IntSupplier s = () -> finalI;  // 正确：拷贝到 final 局部变量
}
```

每次循环用 `int finalI = i` 创建了新的局部变量（栈上的新槽位），lambda 捕获的是这块新内存——每个 lambda 捕获不同的槽位，互不干扰。

**Python 的三种正确写法：**

```python
# 写法1：默认参数绑定（最常用）
funcs = [lambda x=i: x for i in range(5)]
# 原理：def 执行时，默认参数立即求值，x 得到 i 的当前值副本
#       不再通过闭包引用 i

# 写法2：闭包+工厂函数
def make_lambda(x):
    return lambda: x
funcs = [make_lambda(i) for i in range(5)]
# 原理：每次调用 make_lambda(i) 创建新作用域，
#       新 cell 对象保存 x 的当前值

# 写法3：functools.partial
from functools import partial
funcs = [partial(lambda x: x, i) for i in range(5)]

print([f() for f in funcs])  # [0,1,2,3,4] 三种写法都正确

# 2. 可变默认参数
def add_item(item, lst=[]):
    lst.append(item)
    return lst

print(add_item(1))  # [1]
print(add_item(2))  # [1, 2] ← 默认列表跨调用共享！

# 原因：默认参数值只在 def 执行时创建一次
# 正确：
def add_item(item, lst=None):
    if lst is None:
        lst = []
    lst.append(item)
    return lst

# 3. 修改全局变量忘记 global
x = 10
def change_x():
    x = 20  # 这创建了一个局部变量 x，不是修改全局！

change_x()
print(x)  # 10 ← 全局 x 没变！

# 正确：
def change_x():
    global x
    x = 20

# 4. 混淆 return 和 print
def square(x):
    print(x * x)  # 只是打印，没有返回值

result = square(5)  # 打印 25
print(result)       # None ← 没有 return！

# 5. 在函数内部修改可变参数误以为会影响原始值
def bad_swap(a, b):
    a, b = b, a  # 只交换了局部引用，不影响外部

x, y = 1, 2
bad_swap(x, y)
print(x, y)  # 1, 2 ← 没变！
```

---

## 1.5 模块与包

### 1.5.1 一句话核心本质

**一切 `.py` 文件都是模块（module），模块也是对象（`<class 'module'>`）；含 `__init__.py` 的目录是包（package），用于组织模块命名空间。**

### 1.5.1b 命名空间（namespace）到底是什么

命名空间是 Python 最核心但最容易模糊的概念。一句话：

> **命名空间就是一个名字 → 对象的映射表。Python 里每个变量名查找，本质上就是去某个 dict 里查 key。**

```
命名空间 = 字典（dict）
  名字（字符串）   →  对象（任何 Python 对象）
  ────────────       ─────────────────────
  "PI"              →  3.14159
  "greet"           →  <function greet at 0x...>
  "Calculator"      →  <class 'Calculator'>
  "math"            →  <module 'math'>
```

**Python 有 4 种命名空间，按查找顺序（LEGB 规则）：**

```
当你写 print(x) 时，Python 按这个顺序查：
① Local（局部）      → 当前函数内的局部变量 dict
② Enclosing（外层）  → 外层函数的局部变量 dict（闭包）
③ Global（全局）     → 当前模块的全局变量 dict（module.__dict__）
④ Builtins（内置）   → builtins 模块的 dict（print, len, int 等）
```

```
内存中的命名空间：

[模块 my_module.py 的全局命名空间]
┌───────────────────────────────────┐
│ my_module.__dict__（就是一个字典！） │
├───────────────────────────────────┤
│ "PI"          → 3.14159           │
│ "greet"       → <function>        │
│ "Calculator"  → <class>           │
│ "__name__"    → "my_module"       │
│ "__doc__"     → "..."             │
│ "math"        → <module 'math'>   │  ← import math 后，math 模块也在这里
└───────────────────────────────────┘

[调用 greet("AI") 时的局部命名空间]
┌───────────────────────────────────┐
│ greet 栈帧的 f_locals              │
├───────────────────────────────────┤
│ "name"       → "AI"               │
└───────────────────────────────────┘
```

**模块和命名空间的关系——每个模块的全局变量就是一个独立命名空间：**

```python
# a.py
x = 10
def f():
    print(x)  # 这里 x 是 a.py 的全局命名空间里的 x

# b.py
import a
x = 20        # b.py 有自己的全局命名空间，和 a 的 x 互不干扰
a.f()         # 调 a.f()，f 内部查的是 a 的全局命名空间 → 10
print(x)      # 20 ← b 自己的 x
```

```python
# 验证：module.__dict__ 就是它的命名空间
import math
print(type(math.__dict__))      # <class 'dict'>
print("sqrt" in math.__dict__)  # True
print(math.__dict__["sqrt"])    # <built-in function sqrt>
print(math.sqrt)                # 等价

# global 和 local 命名空间也在函数里可见
def demo():
    x = 42
    print(globals())   # 全局命名空间（当前模块的 __dict__）
    print(locals())    # 局部命名空间（当前函数的 f_locals）
```

**Java 类比：**

```
Java 的"命名空间"：
  - package 是命名空间（com.example.utils）
  - 类名必须全路径，编译器静态解析
  - 没有运行时"查 dict"的过程

Python 的"命名空间"：
  - 每个模块就是一个独立的 dict
  - 变量名解析 = 在嵌套的 dict 里逐层查 key
  - import 的本质 = 把模块对象的引用放入当前模块的 dict
```

**关键区别**：Java 的变量名在编译期就确定了地址（栈偏移量或方法区地址），Python 的变量名在**运行时查 dict**——这就是动态语言的核心代价，也是动态语言灵活性的来源（可以动态加属性、替换函数、热更新）。

### 1.5.2 模块基础

**以下是 模块 的基本用法：**
```python
# 每个 .py 文件自动成为一个模块，文件名即模块名

# my_module.py
PI = 3.14159

def greet(name):
    return f"Hello {name}"

class Calculator:
    def add(self, a, b):
        return a + b

# 在其他文件中导入使用
import my_module
print(my_module.PI)              # 3.14159
print(my_module.greet("AI"))     # Hello AI
calc = my_module.Calculator()
print(calc.add(1, 2))            # 3
```

**Java 对比：** Java 要求文件名与 public 类名一致；Python 没有此限制，一个 `.py` 可以包含任意多个类/函数。

### 1.5.3 导入方式

**下面是 导入方式 的代码示例：**
```python
# 1. import 整个模块（推荐）
import math
print(math.sqrt(16))  # 4.0

# 2. from ... import 特定名称
from math import sqrt, pi
print(sqrt(16))       # 4.0（直接使用，不用 math.）

# 3. 别名（常用）
import numpy as np
import pandas as pd
from pathlib import Path

# 4. 导入所有（不推荐，污染命名空间）
from math import *    # 导入 math 中所有公开名称

# 5. 导入子模块
import os.path
from os.path import join

# 6. 动态导入（运行时）
module_name = "json"
import importlib
json = importlib.import_module(module_name)
data = json.loads('{"key": "value"}')
```

### 1.5.4 导入流程（ABC 三步详解）

```
用户写: import requests
              ↓
1. 检查 sys.modules（模块缓存）
   → 如果已导入，直接复用（模块只加载一次！）
              ↓
2. 搜索 sys.path 列表找到模块文件
              ↓
3. 找到文件后，执行 ABC 三步：
   a. 创建 Module 对象（PyModuleObject）
   b. 编译源码为字节码 (.pyc)
   c. 执行模块代码（填充模块的 __dict__）
              ↓
4. 将模块存入 sys.modules["requests"]
              ↓
5. 当前命名空间绑定 "requests" 变量
```

#### 步骤 A：创建 Module 对象

CPython 调用 `module_new()` 在堆上创建一个空模块对象：

```
┌──────────────────────────────────┐
│ PyModuleObject                   │
├──────────────────────────────────┤
│ PyObject_HEAD                    │
│   ob_refcnt = 1                  │
│   ob_type → &PyModule_Type       │
├──────────────────────────────────┤
│ md_dict → 空 dict（PyDictObject） │ ← 这就是 __dict__ 的出生地！
│ md_name = "requests"             │
│ md_code = <code object>          │
└──────────────────────────────────┘

此时 __dict__ 是空的——还没往里面放任何东西。
```

**`md_dict` 就是模块的 `__dict__`**。它在模块对象创建时作为空字典分配，后续执行模块代码时往里填充内容。

#### 步骤 B：编译源码为字节码

```
CPython 读 .py 文件 → 编译成字节码：
① 词法分析：源码字符串 → token 流
   "def get(): pass" → NAME('def'), NAME('get'), OP('('), OP(')'), OP(':'), NAME('pass')

② 语法分析：token 流 → AST（抽象语法树）
   Module(body=[FunctionDef(name='get', ...)])

③ 生成字节码：AST → code object
   <code object get at 0x...>
   ├── co_code: 字节码指令 (LOAD_CONST, RETURN_VALUE)
   ├── co_consts: (None,)
   ├── co_names: ()
   └── co_varnames: ()

④ 结果：code object 存入模块对象的 md_dict["__code__"]
```

#### 步骤 C：执行字节码——填充 `__dict__`

这是最终键的一步——**执行模块的顶层代码**，把所有定义的名字写入 `__dict__`：

```python
# 假如 requests.py 内容如下：
VERSION = "2.28.1"

def get(url):
    return "response"

class Session:
    pass

print("requests loaded")
```

执行这段代码时，CPython 逐条执行字节码，`__dict__` 一步步被填充：

```
执行前：           requests.__dict__ = {}  ← 空字典

第 1 行：VERSION = "2.28.1"
                 ↓
    STORE_NAME "VERSION"
    → __dict__["VERSION"] = "2.28.1"

第 2 行：def get(url):
                 ↓
    MAKE_FUNCTION → 创建 PyFunctionObject
    STORE_NAME "get"
    → __dict__["get"] = <function get>

第 3 行：class Session:
                 ↓
    BUILD_CLASS → 创建 PyTypeObject("Session")
    STORE_NAME "Session"
    → __dict__["Session"] = <class 'Session'>

第 4 行：print("requests loaded")
                 ↓
    LOAD_NAME "print" → 从 builtins 取 print 函数
    LOAD_CONST "requests loaded"
    CALL_FUNCTION → 执行 print
    → 不写 __dict__（只是副作用）

执行后：           requests.__dict__ = {
                      "VERSION": "2.28.1",
                      "get": <function>,
                      "Session": <class>,
                      "__name__": "requests",
                      "__doc__": None,
                      "__package__": None,
                      "__loader__": <...>,
                      "__spec__": <...>,
                      "__file__": "/path/to/requests.py",
                  }
```

#### `__dict__` 到底是什么？——完整讲解

**① 本质：就是一个普通字典**

```python
import requests
print(type(requests.__dict__))    # <class 'dict'> ← 就是字典！
print(isinstance(requests.__dict__, dict))  # True
```

`__dict__` 就是一个 `PyDictObject`，存着 **变量名（字符串）→ 对象** 的映射。和你在代码里写的 `d = {"a": 1}` 没有区别。

**② 怎么来的？**

```
模块创建时（步骤 A）：
  module.__init__ → md_dict = PyDict_New()  ← 空字典

模块执行时（步骤 C）：
  每个赋值语句 → __dict__[名字] = 值
  每个 def     → __dict__[名字] = PyFunctionObject
  每个 class   → __dict__[名字] = PyTypeObject

模块导入后：
  __dict__ 的内容就是模块里定义的所有名字
```

**③ 有哪些值？**

```python
import requests

# 你写的代码里的东西
print("VERSION" in requests.__dict__)    # True  ← 变量
print("get" in requests.__dict__)        # True  ← 函数
print("Session" in requests.__dict__)    # True  ← 类

# Python 自动加的东西（每个模块都有）
print("__name__" in requests.__dict__)   # True
print("__file__" in requests.__dict__)   # True  ← .py 文件路径
print("__doc__" in requests.__dict__)    # True  ← 模块文档字符串
print("__builtins__" in requests.__dict__) # True ← print, len 等
```

**④ 什么时候用？**

```python
# 场景 1：调试——查看模块里有什么
import math
print(list(math.__dict__.keys()))  # 列出 math 所有属性

# 场景 2：动态访问——名字是字符串
attr_name = "sqrt"
print(math.__dict__[attr_name](16))  # 4.0
# 等价于：
print(getattr(math, attr_name)(16))  # 4.0

# 场景 3：动态注入（不推荐，但某些框架需要）
import my_module
my_module.__dict__["new_var"] = 42
print(my_module.new_var)  # 42
# 等价于：
setattr(my_module, "new_var", 42)

# 场景 4：检查模块是否定义了某个东西
if "get" in requests.__dict__:
    print("模块有 get 函数")
```

**⑤ 实例、类、函数的 `__dict__` 都是什么？**

```python
class Dog:
    species = "Canine"     # 存在 Dog.__dict__ 里
    def __init__(self, name):
        self.name = name   # 存在实例的 __dict__ 里

d = Dog("小白")

# 类的 __dict__（PyTypeObject.tp_dict）
print(Dog.__dict__)
# mappingproxy({
#   'species': 'Canine',
#   '__init__': <function>,
#   '__module__': '__main__',
#   ...
# })  ← 注意是 mappingproxy（只读包装），不是普通 dict

# 实例的 __dict__
print(d.__dict__)
# {'name': '小白'}  ← 普通 dict！

# 函数的 __dict__
def f(): pass
f.attr = "hello"
print(f.__dict__)
# {'attr': 'hello'}  ← 函数可以当字典用
```

```
各种 __dict__ 对比：
              内容                       类型              来源
──────        ────                       ────              ────
模块.__dict__  模块里所有变量/函数/类       dict             步骤 C 执行时填充
类.__dict__    类属性、方法                mappingproxy      class 语句执行时由 type_new 填充
实例.__dict__  实例属性（self.xxx）        dict              __init__ 执行时写入
函数.__dict__  函数自定义属性              dict              默认空，可自由添加
```

**⑥ 一句话总结 `__dict__`：**

> **`__dict__` 就是一个普通字典，存着"名字 → 对象"的映射。模块的 `__dict__` 就是它里面定义的所有东西，类的 `__dict__` 就是它的属性和方法，实例的 `__dict__` 就是它的字段。Python 的变量名查找本质上就是在这些 dict 里查 key。**

```python
# 终极验证：obj.attr 就是 obj.__dict__["attr"]
import math

# 模块属性访问
print(math.pi)                    # 3.14159...
print(math.__dict__["pi"])        # 3.14159...  ← 完全一样！

# 实例属性访问
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y

p = Point(3, 4)
print(p.x)                        # 3
print(p.__dict__["x"])            # 3  ← 完全一样！

# 类属性访问
print(Point.__init__)             # <function Point.__init__ at ...>
print(Point.__dict__["__init__"]) # <function Point.__init__ at ...>  ← 一样

# 所以 obj.attr 底层就是：
# 1. 如果 obj 是实例 → 先查 type(obj).__dict__（类属性），再查 obj.__dict__（实例属性）
# 2. 如果 obj 是类   → 查 obj.__dict__，再沿 __mro__ 往上查
# 3. 如果 obj 是模块 → 查 obj.__dict__
```

### 1.5.5 `__name__` 和 `__main__`

**下面是 `__name__` 和 `__main__` 的代码示例：**
```python
# 每个模块都有一个 __name__ 属性
# 直接运行的文件：__name__ = "__main__"
# 被导入的文件：__name__ = 模块名

# my_script.py
print(f"模块名: {__name__}")

if __name__ == "__main__":
    # 只有直接运行此文件时才会执行
    print("这是入口文件")
    main()

# 当被 import 时：
# import my_script  → 输出 "模块名: my_script"（不会执行 main）
# 当直接运行时：
# python my_script.py → 输出 "模块名: __main__"（会执行 main）
```

```
文件执行流程：

python my_script.py
        ↓
设置 __name__ = "__main__"
        ↓
执行模块顶层代码
        ↓
遇到 if __name__ == "__main__":  → 条件成立 → 执行 main()

import my_script
        ↓
设置 __name__ = "my_script"
        ↓
执行模块顶层代码
        ↓
遇到 if __name__ == "__main__":  → 条件不成立 → 跳过
```

### 1.5.6 包（Package）

```
项目目录结构：

my_project/
├── __init__.py        ← 标记 my_project 是包
├── main.py
├── utils/
│   ├── __init__.py    ← 标记 utils 是子包
│   ├── helpers.py
│   └── validators.py
└── models/
    ├── __init__.py    ← 可以在此导入子模块
    ├── user.py
    └── request.py
```

**代码演示：Python 包的四种核心导入方式。**
+ `import my_project.utils.helpers` 使用完整路径导入；
+ `from my_project.utils import helpers` 直接导入子模块到当前命名空间；
+ `from my_project.utils.helpers import format_date` 直接导入具体函数；
+ `from my_project import models` 导入子包。`__init__.py` 标记目录为包，可在其中定义`__all__` 控制 `from package import *` 的行为，或做包的初始化工作。

```python
# 各种导入方式
import my_project.utils.helpers       # 完整路径
from my_project.utils import helpers   # 导入子模块
from my_project.utils.helpers import format_date  # 导入函数
from my_project import models          # 导入子包

# __init__.py 的作用
# 1. 标记目录为包（Python 3.3+ 可以省略，但推荐保留）
# 2. 在导入包时执行，可以做初始化
# 3. 定义 __all__ 控制 from package import * 的行为

# my_project/__init__.py
print("初始化 my_project 包")
__all__ = ["utils", "models"]  # from my_project import * 时导入的模块

# my_project/utils/__init__.py
from .helpers import format_date  # 简化导入路径
from .validators import validate_email

# 现在可以：
from my_project.utils import format_date  # 不需要写 helpers 了
```

### 1.5.7 相对导入

**下面是 相对导入 的代码示例：**
```python
# 包内部使用相对导入（只能在包内使用，不能在直接运行的脚本中使用）

# my_project/models/user.py
from ..utils.helpers import format_date       # 上一级 utils 包
from .request import APIRequest               # 同级 request 模块
from .. import __version__                    # 上上级包

# .    = 当前目录
# ..   = 上级目录
# ...  = 上上级目录

# 注意：相对导入只能在包内用 -m 运行时生效
# python -m my_project.models.user  ✓
# python my_project/models/user.py  ✗ 相对导入会报错
```

### 1.5.8 模块作为单例

**下面是 模块作为单例 的代码示例：**
```python
# Python 模块是天然的单例——无论 import 多少次，模块只加载一次

# config.py
class Config:
    def __init__(self):
        self.model = "gpt-4"
        self.temperature = 0.7

config = Config()  # 直接创建实例

# app.py
from config import config  # 第一次 import → 执行 config.py
from config import config  # 第二次 → 直接从 sys.modules 取

config.model = "gpt-4-turbo"  # 所有引用都看到这个修改
```

### 1.5.9 第三方包管理

**下面是 第三方包管理 的代码示例：**
```python
# pip 安装的第三方包存放在 site-packages 目录
# pip list       → 列出已安装包
# pip install x  → 安装包
# pip freeze     → 导出当前环境所有包信息

# 虚拟环境（venv）为每个项目创建独立的 site-packages
# python -m venv .venv
# source .venv/bin/activate  # 激活后 pip install 安装到此环境
```

#### 运行原理图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       第三方包管理 —— 运行原理                              │
└─────────────────────────────────────────────────────────────────────────────┘

一、pip install 的完整流程
═══════════════════════════════════════════════════════════════════════════════

  你敲: pip install requests
        │
        ▼
  ┌─────────────┐     HTTPS      ┌───────────────────┐
  │ pip 客户端   │ ──────────▶    │ PyPI 服务器        │
  │ (你的机器)   │ ◀──────────    │ pypi.org          │
  └──────┬──────┘     .whl       └───────────────────┘
         │
         ▼ 下载 .whl 文件到本地缓存 ( ~/cache/pip/ )
         │
         ▼ 解压 .whl → 复制到 site-packages/
         │
  ┌──────┴─────────────────────────────────────────────────────┐
  │  site-packages/  (以 Linux 为例)                           │
  │  ┌──────────────────────────────────────────────────────┐  │
  │  │  /usr/lib/python3.11/site-packages/                  │  │
  │  │  ├── requests/                    ← 包本体           │  │
  │  │  │   ├── __init__.py                                 │  │
  │  │  │   ├── api.py                                      │  │
  │  │  │   └── models.py                                   │  │
  │  │  ├── requests-2.28.1.dist-info/  ← 元数据（版本等）   │  │
  │  │  │   ├── METADATA                                    │  │
  │  │  │   └── RECORD                                      │  │
  │  │  └── urllib3/                    ← 依赖包也自动装上  │  │
  │  └──────────────────────────────────────────────────────┘  │
  └─────────────────────────────────────────────────────────────┘

  .whl 其实就是 zip 包，解压后就是 Python 源码目录：
  requests-2.28.1-py3-none-any.whl
         │
         ▼ 改名（去掉版本号）
    requests/  ← 改名后就是包名

二、import 时的查找路径（sys.path）
═══════════════════════════════════════════════════════════════════════════════

  import requests
         │
         ▼ 搜索 sys.path，找到第一个匹配的目录
         │
  sys.path 的内容（按顺序查找）：
  ┌──────────────────────────────────────────────────────────────────────┐
  │  [0] 当前脚本所在目录           /home/me/my_project/                │
  │  [1] PYTHONPATH 环境变量        /home/me/lib/                       │
  │  [2] 标准库                     /usr/lib/python3.11/                │
  │  [3] site-packages              /usr/lib/python3.11/site-packages/  │  ← 在这里找到 requests/
  └──────────────────────────────────────────────────────────────────────┘
                                                    │
                                                    ▼
                          加载 /usr/lib/python3.11/site-packages/requests/__init__.py

三、虚拟环境 venv 的原理（隔离机制）
═══════════════════════════════════════════════════════════════════════════════

  场景：同时开发项目 A 和项目 B，依赖不同版本

                          ┌─────────────────────┐
                          │     系统 Python       │
                          │  /usr/bin/python3.11  │
                          │  site-packages:       │
                          │    requests==2.28.0   │
                          └──────────┬────────────┘
                                     │
                 ┌───────────────────┼───────────────────┐
                 │                   │                   │
                 ▼                   ▼                   ▼
  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐
  │  项目 A（无 venv）   │  │  项目 B（有 venv）   │  │  项目 C（有 venv）   │
  │                     │  │                     │  │                     │
  │  import requests    │  │  import requests    │  │  import requests    │
  │       │             │  │       │             │  │       │             │
  │       ▼ 用系统级     │  │       ▼ 用 venv 级  │  │       ▼ 用 venv 级  │
  │  requests==2.28.0   │  │  requests==2.29.0   │  │  requests==1.0.0    │
  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘

  venv 工作原理（以项目 B 为例）：
  ┌─────────────────────────────────────────────────────────────────┐
  │  my_project_B/                                                  │
  │  ├── .venv/                         ← 虚拟环境目录               │
  │  │   ├── bin/                                                    │
  │  │   │   ├── python                ← 软链接 → /usr/bin/python3.11│
  │  │   │   ├── pip                   ← 和系统是同一个 pip          │
  │  │   │   └── activate              ← 激活脚本（设环境变量）       │
  │  │   ├── lib/                                                    │
  │  │   │   └── python3.11/                                         │
  │  │   │       └── site-packages/    ← 独立的！初始为空             │
  │  │   │           ├── requests/     ← pip install 装到这里         │
  │  │   │           └── ...                                        │
  │  │   └── pyvenv.cfg                 ← 指向系统 Python             │
  │  ├── main.py                       ← 项目代码                    │
  │  └── ...                                                         │
  └─────────────────────────────────────────────────────────────────┘

  激活 venv 后发生了什么？——只是改了 PATH 和 sys.prefix！
  ┌─────────────────────────────────────────────────────────────────┐
  │  source .venv/bin/activate                                      │
  │         │                                                       │
  │         ▼                                                       │
  │  ① PATH 前插：/home/me/my_project_B/.venv/bin                  │
  │     → 现在敲 python，先找到 .venv/bin/python（软链到系统）      │
  │     → 敲 pip，找到 .venv/bin/pip（也是软链）                       │
  │  ② sys.prefix 变为 .venv 路径                                    │
  │     → site-packages 查找路径变为 .venv/.../site-packages/        │
  │  ③ pip install 的目标变为 .venv 的 site-packages                 │
  │     → 不污染系统！                                               │
  └─────────────────────────────────────────────────────────────────┘

四、pip freeze 与 requirements.txt（依赖管理）
═══════════════════════════════════════════════════════════════════════════════

  pip freeze                        pip install -r requirements.txt
       │                                       │
       ▼                                       ▼
  requests==2.28.1                 读取 requirements.txt
  flask==2.3.0                     逐行 pip install
  numpy==1.24.0                    │
       │                           ├─ requests==2.28.1
       ▼                           ├─ flask==2.3.0
  输出到 requirements.txt          └─ numpy==1.24.0
       │                                       │
       ▼                                       ▼
  ┌────────────────┐                ┌──────────────────────┐
  │ requirements    │                │  site-packages/      │
  │ .txt            │                │  ├── requests/       │
  │ requests==2.    │                │  ├── flask/          │
  │ 28.1            │                │  ├── Werkzeug/       │  ← flask 的依赖
  │ ...             │                │  ├── Jinja2/         │  ← flask 的依赖
  └────────────────┘                │  └── numpy/          │
                                    └──────────────────────┘
  （记录清单）                         （实际安装的目录树）

五、site-packages 目录结构实例
═══════════════════════════════════════════════════════════════════════════════

  site-packages/
  ├── requests/                    ← 包代码（import requests 找这里）
  │   ├── __init__.py
  │   ├── api.py
  │   └── models.py
  ├── requests-2.28.1.dist-info/   ← pip 管理的元数据
  │   ├── METADATA                 ← 版本、作者、依赖信息
  │   ├── RECORD                   ← 安装的文件列表和哈希
  │   ├── INSTALLER                ← 安装工具（pip）
  │   └── LICENSE                  ← 许可证
  ├── urllib3/                     ← requests 的依赖
  ├── pip/                         ← pip 自己也是第三方包！
  ├── pip-23.0.dist-info/
  ├── setuptools/                  ← 打包工具
  └── _virtualenv.pth              ← venv 标记文件（标记这是虚拟环境）

  .dist-info 里没有代码，只有元数据！
  pip list / pip show / pip uninstall 全靠这些目录识别：

  pip list          → 扫描 site-packages/ 所有 *.dist-info/
  pip show requests → 读 METADATA 文件
  pip uninstall     → 读 RECORD 文件，删除所有列出的文件

六、常见操作的数据流向
═══════════════════════════════════════════════════════════════════════════════

  pip install requests
       │
       ├─▶ 查询 PyPI 最新版本
       ├─▶ 下载 .whl 到缓存
       ├─▶ 解压到 site-packages/
       │   ├─ requests/            （代码）
       │   └─ requests-2.28.1.dist-info/  （元数据）
       ├─▶ 安装依赖（urllib3, charset 等，递归）
       └─▶ 完成

  import requests
       │
       ├─▶ sys.path 顺序搜索
       ├─▶ 找到 site-packages/requests/
       ├─▶ 加载 __init__.py
       ├─▶ 递归加载其 import 的子模块
       └─▶ 当前命名空间绑定 requests
```

### 1.5.10 常见错误

**以下是常见的错误示例：**
```python
# 1. 循环导入：a.py 和 b.py 互相导入
# a.py
from b import func_b  # 执行到此，b.py 尚未完全加载
def func_a():
    pass

# b.py
from a import func_a  # a.py 正在加载中，func_a 还不存在
def func_b():
    pass

# 运行时会报：ImportError: cannot import name 'func_a'
# 解决1：延迟导入（函数内部导入）
# a.py
def func_a():
    from b import func_b
    func_b()

# 解决2：将公共代码抽到第三个模块
# common.py 放 func_a 和 func_b 共同依赖的代码

# 2. 相对导入在脚本中运行
# 如果直接运行：python my_package/sub_module.py
# 相对导入会报错：ImportError: attempted relative import with no known parent package
# 正确：python -m my_package.sub_module

# 3. 文件名与标准库冲突
# 如果自己写 math.py，则 import math 会导入自己的文件而不是标准库
# 而且自己的 math.py 内部再 import math 会再次导入自己（无限递归！）
# 解决：不要与标准库同名

# 4. __pycache__ 缓存问题
# Python 缓存编译后的 .pyc 文件
# 如果修改了 .py 但 .pyc 没更新，可以删除 __pycache__ 目录或使用 -B 参数
# python -B main.py  # 不生成缓存文件
```


## 1.6 OOP

### 1.6.1 一句话核心本质

**Python 的 OOP 基于运行时对象模型，一切皆对象，类也是对象（type 的实例）；支持多继承、MRO、鸭子类型、魔术方法，比 Java 更灵活但约束更少。**

### 1.6.2 类与实例

**下面是 类与实例 的代码示例：**
```python
# 最基本的类定义
class Dog:
    """狗类"""

    # 类属性（所有实例共享）
    species = "Canine"

    # __init__ 是初始化方法（不是构造！），__new__ 才是真正的构造
    def __init__(self, name: str, age: int):
        self.name = name    # 实例属性
        self.age = age

    # 实例方法（第一个参数必须是 self）
    def bark(self) -> str:
        return f"{self.name} says Woof!"

    # 类方法（第一个参数是 cls，操作类本身）
    @classmethod
    def create_puppy(cls, name: str) -> "Dog":
        return cls(name, age=0)

    # 静态方法（不需要 self 或 cls，纯工具函数）
    @staticmethod
    def species_info() -> str:
        return "Dogs are domesticated mammals."

# 创建实例
d = Dog("Buddy", 3)
print(d.name)          # Buddy
print(d.bark())        # Buddy says Woof!
print(Dog.species)     # Canine（通过类访问类属性）
print(d.species)       # Canine（通过实例也能访问类属性）

# 类方法的调用
puppy = Dog.create_puppy("Puppy")
print(puppy.age)       # 0
```

#### `cls` 参数的作用（类方法中的第一个参数）

`cls` 就是类本身——和实例方法的 `self` 是完全对称的概念：

```
实例方法          def method(self, ...):    # self → 当前实例
类方法 @classmethod def method(cls, ...):   # cls  → 当前类（不是实例！）
```

`cls` 的六大用途：

**① 创建实例（工厂方法）**
```python
class Dog:
    @classmethod
    def create_puppy(cls, name: str) -> "Dog":
        return cls(name, age=0)
        # 等价于 Dog(...)，但如果被继承，cls 指向子类！

class Puppy(Dog):
    pass

p = Puppy.create_puppy("小白")
print(type(p))  # <class 'Puppy'>  ← cls 是 Puppy，不是 Dog！
# 如果写的是 Dog(name, age=0)，这里就是 Dog 了，工厂就错了
```

**② 访问/修改类属性**
```python
class Counter:
    total = 0
    
    @classmethod
    def increment(cls):
        cls.total += 1  # cls.total 等价于 Counter.total
        # 但如果被继承，cls.total 指向子类的 total
    
    @classmethod
    def show(cls):
        return f"{cls.__name__}: {cls.total}"

Counter.increment()
print(Counter.show())  # Counter: 1
```

**③ 读取类级别的配置/注册表**
```python
class Plugin:
    registry = {}  # 类字典，所有子类共享（但可覆盖）

    @classmethod
    def register(cls):
        Plugin.registry[cls.__name__] = cls
        # cls.__name__ → 自动取当前类的名字

class Loader(Plugin): pass
class Saver(Plugin): pass

Loader.register()
Saver.register()
print(Plugin.registry)  # {'Loader': <class Loader>, 'Saver': <class Saver>}
```

**④ 替代构造器（多个构造方式）**
```python
from datetime import datetime, date

class Person:
    def __init__(self, name: str, age: int):
        self.name = name
        self.age = age

    @classmethod
    def from_birth_year(cls, name: str, birth_year: int) -> "Person":
        age = datetime.now().year - birth_year
        return cls(name, age)  # ← 调用 __init__

    @classmethod
    def from_string(cls, data: str) -> "Person":
        name, age = data.split(",")
        return cls(name.strip(), int(age.strip()))

# 三种不同的创建方式
p1 = Person("Alice", 30)
p2 = Person.from_birth_year("Bob", 1994)
p3 = Person.from_string("Charlie, 25")
```

**⑤ 在继承中动态绑定正确的类**
```python
class Shape:
    @classmethod
    def create(cls, *args):
        return cls(*args)  # cls 自动指向调用者的类

class Circle(Shape):
    def __init__(self, radius):
        self.radius = radius

class Square(Shape):
    def __init__(self, side):
        self.side = side

c = Circle.create(5)   # cls → Circle，等价于 Circle(5)
s = Square.create(4)   # cls → Square，等价于 Square(4)
```

**⑥ 替代全局变量（命名空间隔离）**
```python
# 不好的做法：
GLOBAL_CONFIG = {"debug": True}

class App:
    def run(self):
        print(GLOBAL_CONFIG["debug"])  # 依赖全局变量

# 好的做法：把配置放在类属性里，用类方法访问
class App:
    config = {"debug": True}
    
    @classmethod
    def set_debug(cls, value: bool):
        cls.config["debug"] = value
    
    @classmethod
    def is_debug(cls) -> bool:
        return cls.config["debug"]
    
    def run(self):
        print(self.is_debug())
```

`cls` 和 `self` 的本质对比：

```
参数    指向谁            传入方式                    调用方式
────    ────              ────                        ────
self    当前实例           Python 自动传入             instance.method() 或 Class.method(instance)
cls     当前类（不是实例）  Python 自动传入             Class.method() 或 instance.method()
                                                                      ↑ 实例调类方法时，
                                                                        cls 仍是类，不是实例！

验证：
d = Dog("Buddy", 3)
print(Dog.create_puppy("P"))   # cls → Dog
print(d.create_puppy("P"))     # cls → Dog（不是 d！cls 永远是类）
```

#### 类创建流程 —— 内存图逐步演示

当 Python 执行 `class Dog:` 语句时，背后发生了以下 8 步：

```
源代码：
class Dog:
    species = "Canine"
    def bark(self):
        return "Woof!"
```

**第 1 步：收集类体代码 → 创建独立命名空间**

```
class Dog:                      Python 内部：
    species = "Canine"  ──────▶ 创建临时 namespace = {}
                                namespace["species"] = "Canine"
                                namespace["bark"] = <function bark>
```

内存状态：
```
┌──────────────────────────┐
│ 临时 namespace（dict）    │
│  ┌──────────────────────┐│
│  │ "species" → "Canine" ││
│  │ "bark"    → <func>   ││
│  └──────────────────────┘│
└──────────────────────────┘
```

**第 2 步：确定元类（metaclass）**

```
查找顺序：
① class Dog(metaclass=...)       → 显式指定的元类
② class Dog(metaclass=ABCMeta)   → 父类的元类
③ 无指定                         → 默认元类 type
```

```
                                       ┌──────────┐
type  ←─ 一切元类的根源 ───────────────▶│  type    │
                                       │ (元类)   │
                                       └──────────┘
                                            ▲
                                       ┌────┴─────┐
                                       │ object    │
                                       │ (最基类)  │
                                       └──────────┘
```

**第 3 步：确定基类（bases）**

```
class Dog:                  → 隐式继承 object
class Dog(Animal):          → 显式继承 Animal
class Dog(Animal, Pet):     → 多继承
```

```
                      ┌──────────┐
                      │  object  │
                      └────┬─────┘
                           ▲
                      ┌────┴─────┐
                      │   Dog    │  ← 还没有创建出来
                      └──────────┘
```

**第 4 步：调用 `type.__new__` — 申请内存，创建 PyTypeObject**

```
type.__new__(type, "Dog", (object,), namespace)
      │
      ├─ 参数1: type           → 元类自己
      ├─ 参数2: "Dog"          → 类名（字符串）
      ├─ 参数3: (object,)      → 基类元组
      └─ 参数4: namespace      → 第 1 步收集的 dict

      │
      ▼  CPython 内部：type_new()

      ① PyType_Type.tp_alloc(type, 0)
         → 在堆上分配 PyTypeObject 大小的内存
         → 全部初始化为 0（ob_refcnt = 1）

      ② 关键字段填充：
         tp_name     = "Dog"                    ← 类名
         tp_basicsize = sizeof(PyObject)         ← 实例大小
         tp_dict     = namespace（取出内容后打包为 mappingproxy）
         tp_base     = &PyBaseObject_Type        ← object
         tp_mro      = (Dog, object) 的元组      ← 方法解析顺序
         tp_flags    = Py_TPFLAGS_DEFAULT | ...
```

内存中的 PyTypeObject：

```
  堆内存：PyTypeObject for "Dog"
  ┌──────────────────────────────────────────────┐
  │ PyObject_HEAD                                │
  │   ob_refcnt = 1                              │
  │   ob_type → &PyType_Type  ←── 元类是 type！  │
  ├──────────────────────────────────────────────┤
  │ tp_name  = "Dog"                             │
  │ tp_basicsize = 32（sizeof(PyObject)）         │
  │ tp_dict   → mappingproxy({                   │  ← 第1步的 namespace 转换而来
  │     "species": "Canine",                     │
  │     "bark": <function>,                      │
  │     "__module__": "__main__",                │  ← Python 自动添加
  │     "__qualname__": "Dog",                   │  ← Python 自动添加
  │ })                                           │
  │ tp_base   → &PyBaseObject_Type  → object     │
  │ tp_mro    → (Dog, object)                    │
  │ tp_bases  → (<class 'object'>,)              │
  │ tp_init   → &type_init                       │
  │ tp_new    → &type_new                        │
  │ tp_alloc  → &PyType_GenericAlloc             │
  │ tp_dealloc→ &type_dealloc                    │
  └──────────────────────────────────────────────┘
```

**第 5 步：调用 `type.__init__` — 初始化类对象**

```
type.__init__(Dog, "Dog", (object,), namespace)

CPython 内部：type_init()
→ 设置 __module__, __doc__ 等
→ 将方法包装为 descriptor（绑定机制）
→ 检查基类兼容性
→ 计算 tp_mro
```

```
重点：bark 函数被包装为 descriptor！

在 namespace 里，bark 是一个普通函数：
  namespace["bark"] = <function bark at 0x...>

在 type.__init__ 之后，tp_dict 里的 bark 仍是函数，
但 Python 的函数实现了 __get__ 方法（descriptor 协议），
所以当通过实例访问时，会自动绑定为 bound method。

验证：
print(Dog.bark)                     # <function Dog.bark at 0x...>
print(Dog.__dict__["bark"])         # <function Dog.bark at 0x...>
print(Dog("A",1).bark)              # <bound method Dog.bark of <Dog object>>
```

**第 6 步：设置 `__dict__` 为映射代理（mappingproxy）**

```
namespace 被包装为只读的 mappingproxy：
    普通的 dict：  可以增删改
    mappingproxy： 读操作正常，写操作抛出 TypeError

为什么？
→ 防止外部乱改类属性，破坏 type 的内部一致性
→ type 内部用自己的 tp_dict 指针操作，不需要经过 __dict__ 的 __setitem__

验证：
Dog.__dict__["species"] = "Feline"  # TypeError: 'mappingproxy' object does not support item assignment
Dog.species = "Feline"              # ✅ 正确方式（通过 type.__setattr__）
```

**第 7 步：处理装饰器和 `__init_subclass__`**

```
class Dog:
    ...

# 如果 object 或其父类定义了 __init_subclass__，在此调用
# object.__init_subclass__(cls=Dog)
# 这是 PEP 487 的机制，让父类在子类创建时执行自定义逻辑

# 例：
class PluginBase:
    def __init_subclass__(cls, **kwargs):
        super().__init_subclass__(**kwargs)
        cls.registered = True

class MyPlugin(PluginBase):
    pass

print(MyPlugin.registered)  # True  ← 创建 MyPlugin 时自动触发了 __init_subclass__
```

**第 8 步：将 Dog 绑定到当前命名空间**

```
class Dog:
    ...

# 执行完 class 语句后，等价于：
# Dog = type("Dog", (object,), namespace)
# 当前命名空间（模块或函数作用域）多了一个名字 "Dog" → 指向新创建的 PyTypeObject
```

当前命名空间绑定后的内存全貌：

```
模块的 __dict__
┌───────────────────────────────────────────┐
│ ...                                        │
│ "Dog": → PyTypeObject  "Dog"              │
│           ├── tp_dict: mappingproxy       │
│           │   ├── "species" → "Canine"    │
│           │   ├── "bark"    → <function>  │
│           │   ├── "__init__" → <function> │
│           │   └── ...                     │
│           ├── tp_base: object             │
│           └── tp_mro: (Dog, object)       │
│                                           │
│ "d":   → PyObject 实例                     │
│           ├── ob_type → &PyTypeObject Dog  │
│           ├── __dict__                     │
│           │   ├── "name": "Buddy"         │
│           │   └── "age": 3                │
│           └── ...                         │
└───────────────────────────────────────────┘
```

**完整类创建流程总结图：**

```
源代码                       CPython 内部
───────                      ────────────
                              ┌──────────┐
class Dog:                    │ 扫描所有 │
    species = "Canine"  ──▶   │  类体语句 │
    def bark(self): ...       └────┬─────┘
                                   │
                                   ▼ namespace dict
                              ┌──────────────┐
                              │ species→"Can" │
                              │ bark→<func>  │
                              └──────┬───────┘
                                     │
               ┌─────────────────────┼─────────────────────┐
               │  确定元类           │                     │
               ▼                    ▼                     ▼
         ┌──────────┐       ┌──────────────┐      ┌──────────────┐
         │ type     │       │ (object,)    │      │ namespace    │
         │ (元类)   │       │ (基类元组)    │      │ (名字空间)    │
         └────┬─────┘       └──────┬───────┘      └──────┬───────┘
              │                    │                      │
              └────────────────────┼──────────────────────┘
                                   │
                                   ▼
                    type.__new__(type, "Dog", (object,), namespace)
                                   │
                                   ▼
                          ┌──────────────────┐
                          │ 分配 PyTypeObject │
                          │ 内存（堆上）      │
                          │ tp_dict←namespace│
                          │ tp_base←object   │
                          │ tp_mro←(Dog,obj) │
                          └──────┬───────────┘
                                   │
                                   ▼
                    type.__init__(Dog, "Dog", (object,), namespace)
                                   │
                                   ├─ 设置 __module__
                                   ├─ 包装 descriptor
                                   ├─ 调用 __init_subclass__
                                   └─ 其他初始化
                                   │
                                   ▼
                          ┌──────────────────┐
                          │ mappingproxy 包装 │
                          │ （只读保护）      │
                          └──────┬───────────┘
                                   │
                                   ▼
                    当前模块命名空间: Dog = <PyTypeObject>
```

### 1.6.3 Java vs Python OOP 对比

```
               Java                          Python
               ────                          ──────
类定义    public class Dog { }          class Dog:
构造器    Dog(String name) { }          def __init__(self, name):
实例创建  new Dog("Buddy")              Dog("Buddy")（无 new）
this/self 隐式                         显式第一参数 self
访问控制  private/protected/public      _约定/_ _name mangling
方法重载  同名不同参数                    默认参数 /*args 替代
接口      interface 关键字              鸭子类型 / ABC / Protocol
继承      单继承 + 接口                  多继承 + Mixin
静态方法  static 关键字                  @staticmethod
类方法    无（用 static 勉强代替）         @classmethod
属性      getter/setter                  @property / @xxx.setter
```

### 1.6.4 实例方法、类方法、静态方法

**下面是 实例方法、类方法、静态方法 的代码示例：**
```python
class MethodDemo:
    # 类属性
    counter = 0

    def __init__(self, name):
        self.name = name
        MethodDemo.counter += 1

    # 实例方法 —— 操作实例数据
    def greet(self) -> str:
        return f"Hello, I'm {self.name}"

    # 类方法 —— 操作类数据，可被继承覆盖
    @classmethod
    def total_instances(cls) -> int:
        return cls.counter

    # 静态方法 —— 与类相关但不依赖类/实例数据
    @staticmethod
    def is_valid_name(name: str) -> bool:
        return len(name) > 0 and name[0].isalpha()

    # 魔术方法 —— 让实例像函数一样调用
    def __call__(self, times: int) -> str:
        return (self.name + " ") * times

d = MethodDemo("AI")
print(d.greet())              # Hello, I'm AI
print(MethodDemo.total_instances())  # 1 ← 类方法
print(MethodDemo.is_valid_name("AI"))  # True ← 静态方法
print(d(3))                   # AI AI AI  ← __call__
```

```
实例方法调用流程：
d.greet()
   ↓
Python 自动将实例 d 作为第一个参数传入
   ↓
d.greet() 等价于 MethodDemo.greet(d)
   ↓
self = d, 可以通过 self.name 访问实例属性

类方法调用流程：
MethodDemo.total_instances()
   ↓
Python 自动将类 MethodDemo 作为第一个参数传入
   ↓
cls = MethodDemo, 可以通过 cls.counter 访问类属性
```

### 1.6.5 继承与 MRO

**下面是 继承与 MRO 的代码示例：**
```python
# 单继承
class Animal:
    def __init__(self, name):
        self.name = name

    def speak(self) -> str:
        return "..."

class Dog(Animal):
    def speak(self) -> str:
        return "Woof!"

class Cat(Animal):
    def speak(self) -> str:
        return "Meow!"

# 多继承 + MRO（C3 线性化）
class A:
    def who(self): return "A"

class B(A):
    def who(self): return "B"

class C(A):
    def who(self): return "C"

class D(B, C):  # 多继承
    pass

d = D()
print(d.who())       # "B" ← 按 MRO 顺序 B 优先于 C
print(D.__mro__)     # D → B → C → A → object
```

#### MRO 是什么？

**MRO = Method Resolution Order（方法解析顺序）**，就是 Python 在 `d.who()` 时查找 `who` 方法的顺序。

```
d.who()
  │
  ├─▶ 1. 查 d.__class__.__dict__ (D)           → 没有 who
  ├─▶ 2. 查 D.__mro__[0]  (D)                  → 没有 who
  ├─▶ 3. 查 D.__mro__[1]  (B)                  → 有 who！→ 返回 "B"
  ├─▶ 4. 查 D.__mro__[2]  (C)                  → 有，但不用了（B 已经找到了）
  ├─▶ 5. 查 D.__mro__[3]  (A)                  → 跳过
  └─▶ 6. 查 D.__mro__[4]  (object)             → 跳过
```

MRO 保证三条原则：
```
① 单调性：子类的 MRO 不会违反父类的 MRO 顺序
② 局部优先：父类列表中的顺序被保留（D(B, C) → B 在 C 前）
③ 单调性延伸：某类始终在它的子类之后出现
```

#### MRO 是怎么算出来的？—— C3 线性化算法

C3 是 Python 2.3 引入的算法，取代了之前的深度优先搜索（DFS），解决了菱形继承中的不一致问题。

**算法定义：**

```
L[C] = C + merge(L[B1], L[B2], ..., L[Bn], (B1, B2, ..., Bn))

其中：
  L[C]   = 类 C 的 MRO 列表
  C      = 当前类
  B1..Bn = C 的直接父类（按声明顺序）
  merge  = 合并算法（核心！）
```

**`merge` 算法步骤：**

```
merge(列表1, 列表2, ..., 列表N):
  while 还有未处理元素:
      取每个列表的第 0 个元素（候选类）
      如果此候选类不在任何列表的尾部（第 1 个及之后）中出现：
          → 把它加入结果，并从所有列表中移除
      否则：
          → 尝试下一个候选类
  如果循环结束后还有未处理元素：报错（继承不合法）
```

**逐步计算 D(B, C) 的 MRO：**

首先要知道 B 和 C 自己的 MRO：

```
L[A] = A, object        （单继承很简单）

L[B(A)] = B + merge(L[A], (A))
        = B + merge(A, object, A)
        → 候选 A：不在任何列表尾部 → 取出
        = B, A + merge(object)
        → 候选 object：不在尾部 → 取出
        = B, A, object

L[C(A)] = C + merge(L[A], (A))
        = C + merge(A, object, A)
        → 同上
        = C, A, object
```

现在计算 L[D(B, C)]：

```
L[D] = D + merge(L[B], L[C], (B, C))
     = D + merge(B, A, object,   ← L[B]
                C, A, object,   ← L[C]
                B, C)           ← 父类列表
```

merge 合并过程（5 轮）：

```
第 1 轮：取每个列表头元素
        列表1 头 = B    列表2 头 = C    列表3 头 = B
        B 是否在其他列表尾部？检查：
          C, A, object 的尾部 = A, object     → B 不在里面
          B, C 的尾部 = C                     → B 不在里面
        ✅ B 不在任何尾部 → 取出 B

        结果: [D, B]
        剩余: merge(A, object,   ← L[B] 去掉了头 B
                     C, A, object,   ← L[C] 不变
                     C)              ← 父类列表去掉了头 B

第 2 轮：取每个列表头元素
        列表1 头 = A    列表2 头 = C    列表3 头 = C
        C 是否在其他列表尾部？检查：
          A, object 的尾部 = object         → C 不在里面
          C 的尾部为空                     → 安全
        ✅ C 不在任何尾部 → 取出 C

        结果: [D, B, C]
        剩余: merge(A, object,   ← L[B] 不变
                     A, object,   ← L[C] 去掉了头 C
                     )            ← 父类列表为空了

第 3 轮：取每个列表头元素
        列表1 头 = A    列表2 头 = A
        A 是否在其他列表尾部？检查：
          A, object 的尾部 = object       → A 不在里面
          A, object 的尾部 = object       → A 不在里面
        ✅ A 不在任何尾部 → 取出 A

        结果: [D, B, C, A]
        剩余: merge(object,   ← L[B] 去掉了头 A
                     object)   ← L[C] 去掉了头 A

第 4 轮：取每个列表头元素
        列表1 头 = object    列表2 头 = object
        object 不在任何尾部 → 取出 object

        结果: [D, B, C, A, object]

第 5 轮：所有列表为空 ✅ 完成！
```

```
最终 MRO：
    ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌────────┐
    │  D  │→│  B  │→│  C  │→│  A  │→│ object │
    └─────┘ └─────┘ └─────┘ └─────┘ └────────┘
       ▲       ▲       ▲       ▲       ▲
       │       │       │       │       │
       who()  who=B   who=C   who=A  谁都没有
       不写   先找到  被跳过  被跳过
```

**验证结果：** `D.__mro__` = `(D, B, C, A, object)`。和代码输出一致。

#### 复杂的 MRO 例子——验证单调性

```python
class O: pass
class A(O): pass
class B(O): pass
class C(O): pass
class D(O): pass
class E(O): pass
class K1(A, B, C): pass
class K2(D, B, E): pass
class K3(D, A): pass
class Z(K1, K2, K3): pass

print(Z.__mro__)
# Z → K1 → K2 → K3 → D → A → B → C → E → O → object
```

我们来验证这个 MRO 的单调性：

```
检查局部顺序：
  K1(A, B, C)  → A 在 B 前，B 在 C 前。Z 的 MRO：...A→B→C... ✅
  K2(D, B, E)  → D 在 B 前，B 在 E 前。Z 的 MRO：...D→...→B→...→E... ✅
  K3(D, A)     → D 在 A 前。Z 的 MRO：...D→A... ✅

检查父类 MRO 保留：
  L[K1] = K1, A, B, C, O, object
  L[K2] = K2, D, B, E, O, object
  L[K3] = K3, D, A, O, object

  L[Z] 中：K1 在 K2 前 ✅，K2 在 K3 前 ✅
  A 在 B 前，B 在 C 前 ✅（继承自 K1 的顺序）
  D 在 A 前 ✅（继承自 K3 的顺序）
```

#### 为什么 Python 要抛弃 DFS 改用 C3？

**DFS 的问题 —— 菱形继承中的方法覆盖失败：**

```
class A:
    def who(self): return "A"

class B(A):
    def who(self): return "B"

class C(A):
    pass                    # 没有重写 who

class D(B, C):
    pass

# DFS（Python 2 的旧算法）：
# D → B → A → C → object
# d.who() → B ✅（本来正确，但只是运气好）

# 如果换成：
class A:
    def who(self): return "A"

class B(A):
    pass                    # B 没有重写

class C(A):
    def who(self): return "C"  # C 重写了

class D(B, C):
    pass

# DFS：D → B → A → C → object
# d.who() → A ❌（应该用 C 的！C 更具体！）
# 因为 DFS 先走 B 这条线到 A 找到了 who，永远不会走到 C

# C3：D → B → C → A → object
# d.who() → C ✅（C 在 A 之前，正确！）
```

#### MRO 在继承设计中怎么用？

**用法 1：用 `.__mro__` 或 `.mro()` 查看解析顺序**

```python
class Base: pass
class MixinA(Base): pass
class MixinB(Base): pass
class Derived(MixinA, MixinB): pass

# 两种查看方式
print(Derived.__mro__)
# (<class 'Derived'>, <class 'MixinA'>, <class 'MixinB'>, <class 'Base'>, <class 'object'>)

print(Derived.mro())
# [<class 'Derived'>, <class 'MixinA'>, <class 'MixinB'>, <class 'Base'>, <class 'object'>]
```

**用法 2：`super()` 遵循 MRO 链**

```python
class Base:
    def __init__(self):
        print("Base.__init__")

class MixinA(Base):
    def __init__(self):
        print("MixinA.__init__")
        super().__init__()
        # super() 不是找父类 Base！
        # 是在 MRO 链中找 MixinA 的下一个 → 看 MRO 决定

class MixinB(Base):
    def __init__(self):
        print("MixinB.__init__")
        super().__init__()

class Derived(MixinA, MixinB, Base):
    def __init__(self):
        print("Derived.__init__")
        super().__init__()

d = Derived()
print(Derived.__mro__)
# Derived → MixinA → MixinB → Base → object

# 执行流程：
# Derived.__init__      → 打印 "Derived.__init__"
#   → super().__init__() 在 MRO 中找 Derived 的下一个：MixinA
#      → MixinA.__init__  → 打印 "MixinA.__init__"
#        → super().__init__() 在 MRO 中找 MixinA 的下一个：MixinB
#           → MixinB.__init__  → 打印 "MixinB.__init__"
#             → super().__init__() 在 MRO 中找 MixinB 的下一个：Base
#                → Base.__init__  → 打印 "Base.__init__"
```

**用法 3：手动显式指定 MRO 中的类**

```python
class A:
    def who(self): return "A"

class B(A):
    def who(self): return "B"

class C(A):
    def who(self): return "C"

class D(B, C):
    def who(self):
        # 显式跳过 B 直接调用 C 的 who
        return C.who(self)  # ❌ 硬编码，破坏了 MRO

    def who_proper(self):
        # 正确方式：调用 MRO 中 B 的下一个
        return super().who()  # → C.who() 按 MRO 链走

d = D()
print(d.who())         # "C"（硬编码指定）
print(d.who_proper())  # "C"（通过 MRO 自动找到 C）

# 当继承改变时，super() 自动适配，硬编码会错：
class E(C, B):  # 调换了顺序
    pass

e = E()
print(E.__mro__)  # E → C → B → A → object

# 如果 E 里用 C.who(self) 还是 "C"，但 MRO 中 C 本来就是第一个
# 问题出在更复杂的继承中，硬编码会忽略 MRO 的调整
```

**用法 4：MRO 不合法的情况——算法会报错**

```python
# ❌ 无法构建合法的 MRO
class X: pass
class Y: pass
class A(X, Y): pass
class B(Y, X): pass

class C(A, B): pass  # TypeError: Cannot create a consistent method resolution
# 原因：A 要求 X 在 Y 前，B 要求 Y 在 X 前，矛盾！
```

**用法 5：设计 Mixin 时要确保所有类都调用 `super().__init__()`**

```python
class PluginBase:
    def __init__(self, **kwargs):
        print("PluginBase.__init__")
        super().__init__()  # 最终继承链的末端是 object

class LogMixin:
    def __init__(self, **kwargs):
        print("LogMixin.__init__")
        super().__init__(**kwargs)

class AuthMixin:
    def __init__(self, **kwargs):
        print("AuthMixin.__init__")
        super().__init__(**kwargs)

class MyPlugin(LogMixin, AuthMixin, PluginBase):
    def __init__(self, **kwargs):
        print("MyPlugin.__init__")
        super().__init__(**kwargs)

p = MyPlugin()
print(MyPlugin.__mro__)
# MyPlugin → LogMixin → AuthMixin → PluginBase → object

# 输出：
# MyPlugin.__init__
# LogMixin.__init__
# AuthMixin.__init__
# PluginBase.__init__

# 如果任何一个类没有调用 super().__init__()，链就断了！
class BrokenMixin:
    def __init__(self, **kwargs):
        print("BrokenMixin.__init__")
        # 没有 super().__init__() ← 链断了
        # AuthMixin 和 PluginBase 都不会被调用！

class BadPlugin(LogMixin, BrokenMixin, PluginBase):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)

bp = BadPlugin()
# 输出只有：
# LogMixin.__init__
# BrokenMixin.__init__
# ❌ AuthMixin 和 PluginBase 永远不执行！
```

#### MRO 速查表

```
场景                    MRO 结果                          说明
────                    ────────                          ────
class A:                A → object                       没有父类就是 object
class B(A):             B → A → object                   单继承，简单线性
class C(A, B):          C → A → B → object               保持父类顺序
菱形 D(B,C), B(A),C(A)  D → B → C → A → object          C3 保证 B 在 C 前
复杂多继承              根据 C3 算法自动计算              使用 .__mro__ 查看
```

### 1.6.6 封装与属性

**下面是 封装与属性 的代码示例：**
```python
class Account:
    def __init__(self, owner: str, balance: float = 0):
        # 公开属性
        self.owner = owner
        # 约定保护属性（外部可见但不建议直接访问）
        self._transactions = []
        # 名称改写（name mangling），避免子类意外覆盖
        self.__balance = balance

    # getter
    @property
    def balance(self) -> float:
        return self.__balance

    # setter（有校验）
    @balance.setter
    def balance(self, value: float):
        if value < 0:
            raise ValueError("余额不能为负")
        self.__balance = value

    # 计算属性（只读）
    @property
    def has_funds(self) -> bool:
        return self.__balance > 0

    def deposit(self, amount: float):
        self._transactions.append(f"存入: {amount}")
        self.__balance += amount

# 使用
acc = Account("Alice", 1000)
print(acc.balance)           # 1000 ← 调用 getter
acc.balance = 500            # 调用 setter（有校验）
# acc.balance = -100         # ValueError!
print(acc.has_funds)         # True

# 封装级别的本质区别
print(acc.owner)             # Alice ← 公开
print(acc._transactions)     # ['存入: ...'] ← 约定保护（仍可访问）
# print(acc.__balance)       # AttributeError! ← 名称改写
print(acc._Account__balance) # 500 ← 实际存储名（Python 不阻止访问）
```

```
Python 的访问控制本质：

公开（public）:  name        → 任何人都可以访问
保护（protected）: _name     → 约定"不要直接访问"，仍可访问
私有（private）:  __name     → 改写为 _ClassName__name（伪私有）

Java 的 private 由编译器强制执行
Python 的 __xxx 只做名称改写，不阻止访问——这是设计哲学差异：
"我们都是有共识的成年人"（We are all consenting adults here）
```

### 1.6.7 鸭子类型与多态

**下面是 鸭子类型与多态 的代码示例：**
```python
# "如果它走路像鸭子，叫起来像鸭子，那它就是鸭子"
# Python 不看类型声明，只看对象有没有相应方法

class Duck:
    def quack(self): return "嘎嘎"
    def fly(self): return "飞走了"

class Person:
    def quack(self): return "模仿鸭叫"

class Robot:
    def quack(self): return "滴滴，嘎嘎"

# 多态函数 —— 接受任何实现了 quack() 的对象
def make_it_quack(thing):
    return thing.quack()

print(make_it_quack(Duck()))    # 嘎嘎
print(make_it_quack(Person()))  # 模仿鸭叫
print(make_it_quack(Robot()))   # 滴滴，嘎嘎

# 运行时类型检查
def process(data):
    # 检查行为而非类型
    if hasattr(data, "quack"):
        return data.quack()
    if hasattr(data, "read"):
        return data.read()  # 文件对象也可以用！
    raise TypeError(f"不支持的类型: {type(data)}")

# 使用 ABC 做显式接口（Java 风格）
from abc import ABC, abstractmethod

class Quackable(ABC):
    @abstractmethod
    def quack(self) -> str:
        ...

class FormalDuck(Quackable):
    def quack(self) -> str:
        return "Formal Quack"
```

### 1.6.8 魔术方法（选讲，详见第二阶段）

**下面是 魔术方法（选讲，详见第二阶段） 的代码示例：**
```python
class Vector:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    # 字符串表示
    def __str__(self):      # print() / str()
        return f"({self.x}, {self.y})"
    def __repr__(self):     # repr() / 交互式
        return f"Vector({self.x}, {self.y})"

    # 运算符
    def __add__(self, other): return Vector(self.x + other.x, self.y + other.y)
    def __eq__(self, other):  return self.x == other.x and self.y == other.y
    def __abs__(self):        return (self.x**2 + self.y**2)**0.5

    # 容器行为
    def __len__(self):   return 2
    def __getitem__(self, i): return (self.x, self.y)[i]

v = Vector(3, 4)
print(v)                 # (3, 4)     ← __str__
print(v + Vector(1, 2))  # (4, 6)    ← __add__
print(abs(v))            # 5.0       ← __abs__
print(v[0])              # 3         ← __getitem__
```

### 1.6.9 抽象基类（ABC）

**下面是 抽象基类（ABC） 的代码示例：**
```python
from abc import ABC, abstractmethod

class Shape(ABC):  # 继承 ABC 表示这是一个抽象类
    @abstractmethod
    def area(self) -> float:
        """子类必须实现此方法"""
        pass

    @abstractmethod
    def perimeter(self) -> float:
        pass

    # 非抽象方法可以直接使用
    def description(self) -> str:
        return f"Area: {self.area()}, Perimeter: {self.perimeter()}"

class Circle(Shape):
    def __init__(self, radius):
        self.radius = radius

    def area(self) -> float:
        return 3.14159 * self.radius ** 2

    def perimeter(self) -> float:
        return 2 * 3.14159 * self.radius

# s = Shape()       # TypeError! ← 不能实例化抽象类
c = Circle(5)
print(c.area())          # 78.53975
print(c.description())   # Area: 78.54, Perimeter: 31.42
```

### 1.6.10 `__slots__`（内存优化）

**下面是 `__slots__`（内存优化） 的代码示例：**
```python
# __slots__ 限制实例属性名称，减少内存开销（每个实例省一个 __dict__）

class WithoutSlots:
    def __init__(self, x, y):
        self.x = x
        self.y = y

class WithSlots:
    __slots__ = ("x", "y")  # 只允许 x 和 y 两个属性
    def __init__(self, x, y):
        self.x = x
        self.y = y

import sys
wos = WithoutSlots(1, 2)
ws = WithSlots(1, 2)
print(sys.getsizeof(wos))  # 56 bytes（有 __dict__）
print(sys.getsizeof(ws))   # 48 bytes（无 __dict__，8 bytes 差异）

# 副作用：不能添加 __slots__ 之外的属性
# ws.z = 3  # AttributeError!
```

### 1.6.11 常见错误

**以下是常见的错误示例：**
```python
# 1. self 参数缺失
class Dog:
    def __init__(self, name):
        self.name = name
    def bark():               # 错误：少 self
        return "Woof!"

# d = Dog("Buddy")
# d.bark()  # TypeError: bark() takes 0 positional arguments but 1 was given
# 正确：
    def bark(self):
        return "Woof!"

# 2. __init__ 返回非 None
class MyClass:
    def __init__(self):
        return 42  # TypeError: __init__() should return None

# 3. 可变默认参数在类属性中
class Wrong:
    items = []  # 所有实例共享！
a = Wrong()
b = Wrong()
a.items.append(1)
print(b.items)  # [1]

# 正确：在 __init__ 中创建
class Right:
    def __init__(self):
        self.items = []

# 4. 误用 __ 双下划线
class MyClass:
    def __init__(self):
        self.__secret = 42

obj = MyClass()
# print(obj.__secret)          # AttributeError
print(obj._MyClass__secret)    # 42 ← 名称改写后的实际名称

# 5. getter/setter 的直接访问
class Temperature:
    def __init__(self, celsius):
        self._celsius = celsius

    @property
    def celsius(self):
        return self._celsius

t = Temperature(25)
# t.celsius = 30  # AttributeError!（没有 setter）
# 如果你想要 setter，需要显式写 @celsius.setter
```


## 1.7 异常

### 1.7.1 一句话核心本质

**Python 异常是继承自 `BaseException` 的对象，遇到错误时抛出（raise）、在调用链上传播直到被捕获（except）；哲学是"请求原谅比获得许可容易"（EAFP）。**

### 1.7.2 异常层级

```
BaseException                   ← 所有异常的基类
├── SystemExit                  ← sys.exit()
├── KeyboardInterrupt           ← Ctrl+C
└── Exception                   ← 所有常规异常的基类
    ├── TypeError               ← 类型不匹配
    ├── ValueError              ← 值不合法
    ├── KeyError                ← dict 键不存在
    ├── IndexError              ← 列表索引越界
    ├── AttributeError          ← 属性不存在
    ├── FileNotFoundError       ← 文件不存在
    ├── ZeroDivisionError       ← 除以零
    ├── ImportError             ← 导入失败
    │   └── ModuleNotFoundError ← 模块未找到
    ├── RuntimeError            ← 运行时错误
    ├── StopIteration           ← 迭代器结束
    └── OSError                 ← 系统调用错误
        ├── FileNotFoundError
        └── PermissionError
```

**注意：** `BaseException` 和 `Exception` 的区别——永远不要捕获 `BaseException`（会捕获 `SystemExit` 和 `KeyboardInterrupt`！）

### 1.7.3 Java vs Python 对比

```java
// Java：checked exception 必须声明或捕获
public String readFile(String path) throws IOException {
    try {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        return reader.readLine();
    } catch (FileNotFoundException e) {
        throw new RuntimeException("文件未找到", e);
    } finally {
        // 清理
    }
}
```

**Python 等价代码：Python 没有 checked/unchecked 之分，所有异常都是可选捕获。方法签名不需要 `throws` 声明。`with open(path) as f` 替代 Java 的 try-with-resources，自动管理文件句柄。`raise ... from e` 设置异常链（chained exception），保留原始异常上下文，等价于 Java 的 `initCause()`。**

```python
# Python：没有 checked exception，全部可选捕获
def read_file(path: str) -> str:
    try:
        with open(path) as f:
            return f.readline()
    except FileNotFoundError as e:
        raise RuntimeError(f"文件未找到: {path}") from e
```

**关键差异：**
- Java 区分 checked / unchecked exception，方法签名必须声明 throws
- Python 所有异常都是 unchecked，不需要声明，不需要强制捕获
- Python 的 `finally` 常被 `with` 语句替代

### 1.7.4 try/except/else/finally 完整结构

**下面是 try/except/else/finally 完整结构 的代码示例：**
```python
try:
    # 可能引发异常的代码
    file = open("data.txt")
    data = file.read()
    number = int(data)
except FileNotFoundError:
    # 特定异常处理
    print("文件不存在，使用默认值")
    number = 0
except ValueError as e:
    # 另一个异常处理（可以访问异常对象）
    print(f"数据格式错误: {e}")
    number = -1
except (TypeError, KeyError):
    # 捕获多个异常，同一方式处理
    print("类型或键错误")
except Exception:
    # 捕获所有其他 Exception 子类（兜底）
    print("未知错误")
    # 通常应该记录日志
else:
    # 没有异常发生时执行（可选）
    print(f"成功读取数据: {data}")
finally:
    # 无论是否有异常都执行（可选）
    print("清理资源")
    file.close()  # 放在 finally 确保关闭
```

```
执行流程：

try 块执行
  ├── 没有异常 → 执行 else 块 → 执行 finally 块 → 继续
  ├── 有 except 匹配 → 执行对应 except 块 → 执行 finally → 继续
  └── 有 except 不匹配 → 执行 finally → 向上传播异常
```

### 1.7.5 自定义异常

**下面是 自定义异常 的代码示例：**
```python
# 继承 Exception 或其子类
class AIAPIError(Exception):
    """AI API 调用错误的基类"""
    pass

class RateLimitError(AIAPIError):
    """API 限流异常"""
    def __init__(self, retry_after: int, message: str = "触发限流"):
        self.retry_after = retry_after
        super().__init__(f"{message}，{retry_after}s 后重试")

class ModelOverloadedError(AIAPIError):
    """模型负载过高"""
    def __init__(self, model: str):
        self.model = model
        super().__init__(f"模型 {model} 当前负载过高")

class InvalidPromptError(AIAPIError, ValueError):
    """多重继承：既是 AI 错误又是值错误"""
    def __init__(self, prompt: str, reason: str):
        self.prompt = prompt
        super().__init__(f"Prompt 无效: {reason}")

# 使用
def call_llm(prompt: str) -> str:
    if len(prompt) > 4096:
        raise InvalidPromptError(prompt, "超过最大长度")
    if rate_limited:
        raise RateLimitError(retry_after=30)
    return "response"

try:
    result = call_llm("very long prompt..." * 1000)
except InvalidPromptError as e:
    print(f"请缩短输入: {e}")
except RateLimitError as e:
    print(f"等待 {e.retry_after}s 后重试")
    time.sleep(e.retry_after)
    result = call_llm(prompt)  # 重试
except AIAPIError as e:
    print(f"AI 调用失败: {e}")
    # 全部 AIAPIError 子类都被捕获
```

### 1.7.6 raise 与异常链

**raise 手动抛出异常，异常链 就是抛出异常时，附带原异常信息**
```python
# 基本 raise
def divide(a, b):
    if b == 0:
        raise ValueError("除数不能为零")
    return a / b

# 重新抛出
try:
    divide(1, 0)
except ValueError:
    print("捕获到错误")
    raise  # 重新抛出当前异常（保留完整堆栈）

# 异常链 —— 保留原始异常
def process_data(path: str):
    try:
        with open(path) as f:
            return json.load(f)
    except FileNotFoundError as e:
        raise RuntimeError(f"处理数据失败: {path}") from e
    #                             从原始异常创建链 ↑

# 显式抑制链
def load_config():
    try:
        return json.load(open("config.json"))
    except FileNotFoundError:
        return {}  # 文件不存在用默认配置
    except json.JSONDecodeError as e:
        raise RuntimeError("配置文件损坏") from None  # 不暴露内部细节
```

```
异常链的打印：

try:
    process_data("nonexistent.json")
except RuntimeError as e:
    print(e)            # 处理数据失败: nonexistent.json
    print(e.__cause__)  # FileNotFoundError ← 原始异常
    raise
```

### 1.7.7 assert（断言）

**下面是 assert（断言） 的代码示例：**
```python
# assert 条件, 错误信息
def set_temperature(value: float):
    assert 0 <= value <= 2, f"temperature 必须在 0-2 之间，当前值: {value}"
    self.temperature = value

# 断言可以在运行时禁用（python -O main.py）
# 不要用 assert 做数据验证！assert 可能被关闭

# 正确用法：内部调试/测试
def divide(a, b):
    assert b != 0, "除数不能为0"  # 开发调试用
    return a / b

# 数据验证用显式 if + raise
def set_age(age):
    if age < 0 or age > 150:
        raise ValueError(f"年龄不合法: {age}")
    self.age = age
```

### 1.7.8 上下文管理器与异常

#### 上下文管理器是什么

```
上下文管理器 = 定义了 __enter__ 和 __exit__ 方法的对象

用途：自动管理资源（文件、锁、数据库连接、网络请求），保证无论是否发生异常都能正确清理。

with open("file.txt") as f:    ← open 返回的文件对象是上下文管理器
    data = f.read()             ← 使用资源
                                ← 离开 with 块自动关闭文件（即使发生异常）
```

#### `with` 语句的执行逻辑（伪代码）

```
with EXPR as VAR:
    BLOCK

等价于：

管理器 = EXPR                  # 1. 计算表达式，得到上下文管理器对象
VAR = 管理器.__enter__()       # 2. 调用 __enter__，返回值赋给 VAR
try:
    BLOCK                      # 3. 执行代码块
except Exception as e:
    抑制 = 管理器.__exit__(type(e), e, e.__traceback__)
                               # 4. 发生异常时调用 __exit__
    if not 抑制:
        raise                  # 5. 不抑制 → 继续传播异常
else:
    管理器.__exit__(None, None, None)
                               # 6. 没有异常也调用 __exit__
```

#### `__exit__` 的三个参数

```
def __exit__(self, exc_type, exc_val, exc_tb):
    # exc_type: 异常的类型（class）       → 比如 ValueError
    # exc_val:  异常的实例（object）       → 比如 ValueError("xxx")
    # exc_tb:   异常的堆栈追踪（traceback）→ 用于调试
    #
    # 没有异常时：三个参数都是 None
    # 有异常时：三个参数都有值
    #
    # 返回值：
    #   False → 不抑制异常，异常继续传播（默认行为）
    #   True  → 抑制异常，with 块正常结束，异常不往外抛

    return False  # 默认不抑制
```

#### 逻辑全流程图

```
开始 with
    │
    ▼
r = __enter__()        ← 打开资源，返回资源对象
    │
    ▼
as VAR                 ← VAR = r，拿到资源
    │
    ▼
    执行 with 代码块
    │
    ├──── 正常完成 ──────────────────┐
    │                                │
    ▼                                ▼
__exit__(None,None,None)         遇到异常 → __exit__(exc_type, exc_val, exc_tb)
    │                                │
    ▼                                ├── return False ──▶ 异常继续往外抛
结束 with（正常）                    │
                                    ├── return True  ──▶ 异常被吃掉
                                    │                   with 块后面继续执行
                                    ▼
                              结束 with（异常被抑制 或 被传播）
```

#### 异常处理四种场景

**场景 1：没有异常（最理想）**

```python
class Demo:
    def __enter__(self):
        print("进入")
        return self
    def __exit__(self, *args):
        print(f"退出，异常参数: {args}")
        return False

with Demo():
    print("干活")
# 输出：
# 进入
# 干活
# 退出，异常参数: (None, None, None)
```

**场景 2：发生异常，不抑制（`return False`）**

```python
class NoSuppress:
    def __enter__(self):
        print("打开连接")
        return self
    def __exit__(self, exc_type, exc_val, exc_tb):
        print(f"关闭连接")
        print(f"异常类型: {exc_type.__name__}")
        print(f"异常信息: {exc_val}")
        return False  # ← 不抑制，异常往外传

with NoSuppress() as conn:
    print("执行查询")
    raise ValueError("数据损坏")
    print("这行不会执行")  # ← 永远不会执行

print("这里也不会执行")  # ← 异常已传播到这里
# 输出：
# 打开连接
# 执行查询
# 关闭连接
# 异常类型: ValueError
# 异常信息: 数据损坏
# Traceback (most recent call last):
#   ...
# ValueError: 数据损坏
```

**场景 3：发生异常，主动抑制（`return True`）**

```python
class SuppressAll:
    def __enter__(self):
        return self
    def __exit__(self, exc_type, exc_val, exc_tb):
        if exc_type is ValueError:
            print(f"压住异常: {exc_val}")
            return True  # ← 抑制！异常不传播
        return False

with SuppressAll():
    print("开始")
    raise ValueError("小问题，不管了")
    print("这行不会执行")  # ← 确实不会（raise 之后的代码不执行）

print("但这里会执行！")  # ← 因为异常被抑制了！
# 输出：
# 开始
# 压住异常: 小问题，不管了
# 但这里会执行！
```

**场景 4：在 `__exit__` 中自己抛出异常**

```python
class CloseFail:
    def __enter__(self):
        return self
    def __exit__(self, *args):
        raise RuntimeError("关闭失败！")

try:
    with CloseFail():
        print("正常干活")
except RuntimeError as e:
    print(f"捕获: {e}")
# 输出：
# 正常干活
# 捕获: 关闭失败！

# 如果 with 块本身就有异常，__exit__ 又抛出新异常：
class DoubleFail:
    def __enter__(self):
        return self
    def __exit__(self, *args):
        raise RuntimeError("关闭炸了")
        return True  # ← 这行不会执行，上面已经抛了

try:
    with DoubleFail():
        raise ValueError("干活炸了")
except RuntimeError as e:
    print(f"外层只看到 __exit__ 的异常: {e}")
    # ValueError 被替代了！原始异常丢失
    # Python 3.11+ 会链式附加 __context__
```

#### `contextlib` 工具

**`@contextmanager` 装饰器——用生成器写上下文管理器：**

```python
from contextlib import contextmanager

@contextmanager
def database_connection(dsn: str):
    # 相当于 __enter__
    print(f"连接: {dsn}")
    conn = {"dsn": dsn}
    try:
        yield conn  # ← 这里的值就是 as 后面的变量
    except Exception as e:
        print(f"异常处理: {e}")
        # 如果想抑制：不重新 raise
        # 如果想传播：重新 raise
        raise
    finally:
        # 相当于 __exit__（无论如何都执行）
        print("关闭连接")

# 使用
with database_connection("postgres://...") as conn:
    print(f"使用连接: {conn}")
# 输出：
# 连接: postgres://...
# 使用连接: {'dsn': 'postgres://...'}
# 关闭连接
```

`@contextmanager` 的等价实现逻辑：

```
def database_connection(dsn):
    # 前半段 → __enter__
    conn = open_connection(dsn)
    try:
        yield conn
    except:
        # 中间 → 异常处理
        ...
    finally:
        # 后半段 → __exit__（保证执行）
        close_connection(conn)

等价于：
class Wrapper:
    def __enter__(self):
        gen = database_connection(dsn)
        self.gen = gen
        try:
            return next(gen)   # 执行到 yield，返回 yield 的值
        except StopIteration:
            raise RuntimeError("生成器没 yield")

    def __exit__(self, *exc_info):
        try:
            if exc_info[0] is None:
                next(self.gen)  # 无异常 → 继续生成器（到末尾）
            else:
                self.gen.throw(*exc_info)  # 有异常 → 注入异常到生成器
        except StopIteration:
            return True  # 生成器正常结束 → 相当于抑制
        except:
            raise        # 生成器抛出了其他异常 → 传播
        return False
```

**`contextlib.suppress`——抑制指定异常：**

```python
from contextlib import suppress

# 不用 suppress 的写法：
try:
    os.remove("temp.txt")
except FileNotFoundError:
    pass

# 用 suppress：
with suppress(FileNotFoundError):
    os.remove("temp.txt")
# 等价，更简洁
```

**`contextlib.closing`——确保调用 `close()`：**

```python
from contextlib import closing
import urllib.request

# 不用 closing：
resp = urllib.request.urlopen("http://example.com")
try:
    data = resp.read()
finally:
    resp.close()

# 用 closing：
with closing(urllib.request.urlopen("http://example.com")) as resp:
    data = resp.read()
# 自动 resp.close()
```

**`contextlib.redirect_stdout`——临时重定向输出：**

```python
from contextlib import redirect_stdout
import io

buf = io.StringIO()
with redirect_stdout(buf):
    print("这是一段偷偷收集的文字")
    print("不会显示在屏幕上")

print(buf.getvalue())  # '这是一段偷偷收集的文字\n不会显示在屏幕上\n'
```

#### with 嵌套的真正用途——资源栈

```python
# 连续打开多个资源：
with open("a.txt") as f1:
    with open("b.txt") as f2:
        data1 = f1.read()
        data2 = f2.read()

# 等价于（Python 3.1+ 单行写法）：
with open("a.txt") as f1, open("b.txt") as f2:
    data1 = f1.read()
    data2 = f2.read()

# 执行顺序（资源的释放是反序的！）：
# 1. 打开 a.txt（__enter__）
# 2. 打开 b.txt（__enter__）
# 3. 读取数据
# 4. 关闭 b.txt（__exit__）← 后打开的，先关闭
# 5. 关闭 a.txt（__exit__）← 先打开的，后关闭
#
# 类似栈：后进先出
```

#### 真实世界的例子

```python
import sqlite3
from contextlib import contextmanager

@contextmanager
def transaction(db_path: str):
    """自动提交/回滚的事务上下文"""
    conn = sqlite3.connect(db_path)
    try:
        yield conn
        conn.commit()          # 正常 → 提交
    except Exception as e:
        conn.rollback()        # 异常 → 回滚
        raise                  # 继续传播异常
    finally:
        conn.close()           # 无论如何关闭连接

# 使用：
with transaction("app.db") as conn:
    conn.execute("INSERT INTO users VALUES (?, ?)", ("Alice", 30))
    # 如果这里出错，自动回滚，不污染数据库
```

```
上下文管理器总结：
                     without with                    with
                     ────────────                    ────
必须写 try/finally   ✅                            自动处理
忘记释放资源         ⚠️ 常见 bug                    ✅ 不可能
异常安全            ✅ 写对了就安全                  ✅ 天然安全
代码行数            N 行                            N-3 行
可读性              try/finally 分散注意力          专注于主逻辑
```

### 1.7.9 EAFP vs LBYL

**下面是 EAFP vs LBYL 的代码示例：**
```python
# EAFP: Easier to Ask for Forgiveness than Permission（Python 风格）
# "先做，错了再处理"
def get_value_eafp(d, key):
    try:
        return d[key]
    except KeyError:
        return None

# LBYL: Look Before You Leap（Java 风格）
# "先检查，再做"
def get_value_lbyl(d, key):
    if key in d:
        return d[key]
    return None

# EAFP 的优势：
# 1. 避免竞态条件（检查后到使用前，状态可能已变）
# 2. 代码更专注主逻辑（try 中写正向流程）
# 3. Python 异常的成本比 Java 低

# EAFP 在 AI 中的典型应用
async def call_llm_with_fallback(prompt: str) -> str:
    models = ["gpt-4", "gpt-3.5-turbo", "claude-3"]
    for model in models:
        try:
            return await call_model(model, prompt)
        except (RateLimitError, TimeoutError) as e:
            logger.warning(f"{model} 失败: {e}，尝试下一个")
            continue
    raise RuntimeError("所有模型都不可用")
```

### 1.7.10 最佳实践

**下面是 最佳实践 的代码示例：**
```python
import logging

logger = logging.getLogger(__name__)

class AIService:
    async def chat(self, prompt: str) -> str:
        try:
            response = await self._call_api(prompt)
            return response
        except RateLimitError:
            logger.warning("触发限流，等待后重试")
            await asyncio.sleep(30)
            return await self.chat(prompt)  # 递归重试
        except (TimeoutError, ConnectionError) as e:
            logger.error(f"网络错误: {e}")
            raise AIAPIError("服务暂时不可用") from e
        except Exception as e:
            logger.exception("未预期的错误")  # 记录完整堆栈
            raise  # 重新抛出
        else:
            logger.info("API 调用成功")
        finally:
            logger.debug("本次调用结束")
```

### 1.7.11 常见错误

**以下是常见的错误示例：**
```python
# 1. 捕获 too broad
try:
    user_input = input()
    number = int(user_input)
except:  # 也会捕获 SystemExit, KeyboardInterrupt！
    print("错误")

# 正确：精确捕获
try:
    number = int(input())
except ValueError:
    print("输入不是数字")

# 2. 吞掉异常
try:
    risky_operation()
except Exception:
    pass  # 异常消失了！无法排查

# 正确：至少记录日志
import logging
logger = logging.getLogger(__name__)
try:
    risky_operation()
except Exception as e:
    logger.exception("操作失败")  # logger.exception 自动记录堆栈
    raise  # 或 raise AIAPIError(...) from e

# 3. try 代码块过大
try:
    # 50 行代码 —— 不知道哪行抛什么异常！
    data = load_data()
    processed = transform(data)
    result = save(processed)
except Exception:
    # 不知道哪个操作失败
    pass

# 正确：每个操作独立 try
data = load_data()
try:
    processed = transform(data)
except TransformError as e:
    logger.error(f"转换失败: {e}")
    return None
result = save(processed)

# 4. 在 finally 中 return 会覆盖异常
def bad():
    try:
        raise ValueError("原始错误")
    finally:
        return "覆盖了"  # 吃掉 ValueError！返回 "覆盖了"

print(bad())  # "覆盖了" ← ValueError 消失了！
```


## 1.8 文件 IO

### 1.8.1 一句话核心本质

**Python 用 `open()` 打开文件返回文件对象，配合 `with` 语句自动关闭；文件也是对象（实现了 `__enter__`/`__exit__`），文本/二进制模式通过 mode 参数区分。**

### 1.8.2 文件打开模式

```
模式    用途                 文件指针   文件存在     文件不存在
───    ───                  ──────    ──────      ──────
"r"    只读文本              开头      正常          报错
"w"    只写文本（覆盖）      开头      清空重写      新建
"a"    追加文本             末尾      末尾追加      新建
"x"    独占创建             开头      报错          新建
"rb"   只读二进制            开头      正常          报错
"wb"   只写二进制            开头      清空重写      新建
"ab"   追加二进制           末尾      末尾追加      新建
"r+"   读写                 开头      正常          报错
"w+"   读写（覆盖）          开头      清空重写      新建
"a+"   读写（追加）         末尾      末尾追加      新建
```

**代码演示：Python 文件打开和关闭的标准方式。`open()` 返回文件对象，`encoding` 参数指定字符编码（Java 需额外指定 `Charset`）。手动调用 `close()` 是易错点（异常时可能跳过），推荐使用 `with` 语句，它在代码块结束时自动调用 `__exit__` 关闭文件，等价于 Java 的 try-with-resources。**

```python
# 基本打开/关闭
file = open("data.txt", "r", encoding="utf-8")
content = file.read()
file.close()  # 必须手动关闭！否则可能丢失数据

# 推荐：with 语句（自动关闭）
with open("data.txt", "r", encoding="utf-8") as f:
    content = f.read()
# with 块结束自动关闭，即使发生异常
```

### 1.8.3 读写操作

**下面是 读写操作 的代码示例：**
```python
# 读取
with open("file.txt", encoding="utf-8") as f:
    content = f.read()              # 读取全部内容（大文件小心内存）
    line = f.readline()             # 读取一行
    lines = f.readlines()           # 读取所有行 → list
    for line in f:                  # 逐行遍历（内存友好，推荐）
        print(line.strip())

# 写入
with open("output.txt", "w", encoding="utf-8") as f:
    f.write("Hello\n")              # 写入字符串（不会自动换行）
    f.write("World\n")
    f.writelines(["line1\n", "line2\n"])  # 写入多行

# 追加
with open("log.txt", "a", encoding="utf-8") as f:
    f.write("新的日志行\n")

# 读写模式
with open("data.txt", "r+", encoding="utf-8") as f:
    content = f.read()              # 先读
    f.seek(0)                       # 文件指针回到开头
    f.write("新内容")               # 覆盖写入
```

```
文件指针操作：

with open("file.txt", "r") as f:
    content = f.read(10)    # 读 10 字节，指针移动到第 10 字节
    pos = f.tell()          # 获取当前指针位置
    f.seek(0)               # 回到文件开头
    f.seek(0, 2)            # 跳到文件末尾
    f.seek(-5, 2)           # 从末尾向前 5 字节
```

### 1.8.4 文本 vs 二进制

**下面是 文本 vs 二进制 的代码示例：**
```python
# 文本模式（默认）：自动处理换行符和编码
with open("text.txt", "w", encoding="utf-8") as f:
    f.write("中文内容")  # 自动编码为 UTF-8 字节

with open("text.txt", "r", encoding="utf-8") as f:
    text = f.read()      # 自动解码为 str

# 二进制模式：不处理编码，直接读写 bytes
with open("image.jpg", "rb") as f:
    data = f.read()      # bytes 对象
    print(type(data))    # <class 'bytes'>
    print(len(data))     # 文件大小（字节数）

with open("output.bin", "wb") as f:
    f.write(b"\x00\x01\x02")  # 写入原始字节

# struct 模块读写结构化二进制
import struct
# 打包：整数(4字节) + 浮点数(8字节)
packed = struct.pack("Id", 42, 3.14)
with open("data.bin", "wb") as f:
    f.write(packed)

# 解包
with open("data.bin", "rb") as f:
    raw = f.read()
    number, pi = struct.unpack("Id", raw)
```

### 1.8.5 JSON 读写

#### 类型映射表

```
Python                     JSON
──────                     ────
dict                       object
list, tuple                array
str                        string
int, float (incl. NaN)     number
True / False               true / false
None                       null
```

#### 核心接口一览

```python
import json

data = {
    "model": "gpt-4",
    "parameters": {"temperature": 0.7, "max_tokens": 2048},
    "tools": ["search", "calculate"]
}

# ──── 写入 ────
with open("config.json", "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

json_str = json.dumps(data)       # → str
json_str = json.dumps(data, indent=2)

# ──── 读取 ────
with open("config.json", "r", encoding="utf-8") as f:
    loaded = json.load(f)

parsed = json.loads(json_str)     # ← 从 str 解析
```

#### `json.dumps()` 全部参数详解

```python
json.dumps(
    obj,                          # 要序列化的 Python 对象
    *,
    skipkeys=False,               # True → 跳过非 str 的 key（否则 TypeError）
    ensure_ascii=True,            # True → 非 ASCII 转 \uXXXX；False → 保留中文等
    check_circular=True,          # True → 检测循环引用（否则 RecursionError）
    allow_nan=True,               # True → NaN/Infinity/-Infinity 转 JavaScript 格式
    cls=None,                     # 自定义 JSONEncoder 子类
    indent=None,                  # None → 紧凑输出；0 → 换行不缩进；N → N 格缩进
    separators=None,              # (",", ":") → 紧凑；默认 (", ", ": ")
    default=None,                 # 遇到不可序列化类型时调用的函数
    sort_keys=False,              # True → 按 key 字母排序输出
)
```

**参数实战：**

```python
d = {"name": "小明", "score": 95, "active": True}

# 默认（紧凑、ASCII 转义）
print(json.dumps(d))
# {"name": "\u5c0f\u660e", "score": 95, "active": true}

# 保留中文
print(json.dumps(d, ensure_ascii=False))
# {"name": "小明", "score": 95, "active": true}

# 格式化输出
print(json.dumps(d, ensure_ascii=False, indent=2))
# {
#   "name": "小明",
#   "score": 95,
#   "active": true
# }

# 按 key 排序 + 最紧凑
print(json.dumps(d, sort_keys=True, separators=(",", ":")))
# {"active":true,"name":"小明","score":95}

# 跳过非法 key
d2 = {(1, 2): "tuple_key", "name": "hello"}
json.dumps(d2)                            # TypeError: keys must be str
json.dumps(d2, skipkeys=True)             # {"name": "hello"}  ← 跳过 tuple key
```

#### `json.loads()` 全部参数详解

```python
json.loads(
    s,                            # JSON 字符串
    *,
    cls=None,                     # 自定义 JSONDecoder 子类
    object_hook=None,             # 每个 dict 解析后调用（可改对象）
    parse_float=None,             # 浮点数解析函数（默认 float）
    parse_int=None,               # 整数解析函数（默认 int）
    parse_constant=None,          # NaN/Infinity/-Infinity 处理
    object_pairs_hook=None,       # 有序 dict 回调（比 object_hook 优先）
)
```

**参数实战：**

```python
# object_hook → dict 解析后自动转换
def as_complex(dct):
    if "__complex__" in dct:
        return complex(dct["real"], dct["imag"])
    return dct

data = '{"__complex__": true, "real": 1, "imag": 2}'
result = json.loads(data, object_hook=as_complex)
print(result, type(result))  # (1+2j) <class 'complex'>

# parse_float → 自定义浮点数（比如全用 Decimal）
from decimal import Decimal
data = '{"price": 19.99, "ratio": 0.333}'
result = json.loads(data, parse_float=Decimal)
print(type(result["price"]))   # <class 'decimal.Decimal'>

# object_pairs_hook → 保留 key 顺序
items = json.loads('{"b": 1, "a": 2}', object_pairs_hook=list)
print(items)  # [('b', 1), ('a', 2)]
```

#### 处理不可序列化的类型

```python
import json
from datetime import datetime
from pathlib import Path

# ❌ 默认会失败
json.dumps({"time": datetime.now()})
# TypeError: Object of type datetime is not JSON serializable

# ✅ 方案 1：default 回调
def json_default(obj):
    if isinstance(obj, datetime):
        return obj.isoformat()
    if isinstance(obj, Path):
        return str(obj)
    if isinstance(obj, set):
        return list(obj)
    if isinstance(obj, bytes):
        return obj.hex()
    raise TypeError(f"不能序列化 {type(obj)}: {obj}")

data = {
    "time": datetime.now(),
    "path": Path("/tmp/data"),
    "tags": {"py", "js", "go"},
}
print(json.dumps(data, default=json_default, indent=2))
# {
#   "time": "2026-05-31T10:30:00.123456",
#   "path": "/tmp/data",
#   "tags": ["py", "js", "go"]
# }

# ✅ 方案 2：继承 JSONEncoder（适合复用）
class CustomEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, datetime):
            return obj.isoformat()
        if isinstance(obj, Path):
            return str(obj)
        if isinstance(obj, set):
            return list(obj)
        if isinstance(obj, bytes):
            return obj.hex()
        if isinstance(obj, Decimal):
            return float(obj)
        return super().default(obj)

json.dumps(data, cls=CustomEncoder, indent=2)

# ✅ 方案 3：先转成可序列化的中间结构
def serialize_for_json(obj):
    match obj:
        case datetime() as dt:    return {"$type": "datetime", "value": dt.isoformat()}
        case Path() as p:         return {"$type": "path", "value": str(p)}
        case set() as s:          return [*s]
        case _:                   raise TypeError(f"不支持: {type(obj)}")
```

#### 自定义反序列化（恢复原始类型）

```python
class SmartDecoder(json.JSONDecoder):
    def __init__(self):
        super().__init__(object_hook=self.object_hook)

    def object_hook(self, dct):
        if "$type" in dct:
            match dct["$type"]:
                case "datetime":
                    return datetime.fromisoformat(dct["value"])
                case "path":
                    return Path(dct["value"])
        return dct

raw = '{"time": {"$type": "datetime", "value": "2026-05-31T10:30:00"}}'
obj = json.loads(raw, cls=SmartDecoder)
print(obj["time"], type(obj["time"]))  # 2026-05-31 10:30:00  <class 'datetime'>
```

#### 性能调优

```python
import json

# 紧凑输出（省带宽）
data = [{"id": i, "name": f"item_{i}"} for i in range(1000)]

# 默认 ≈ 慢（有空格）
s1 = json.dumps(data)

# 紧凑 ≈ 快 20-30%
s2 = json.dumps(data, separators=(",", ":"))

print(len(s1), len(s2))  # 默认更长

# 大文件逐行处理（避免一次性加载全部）
def iter_json_lines(filepath: str):
    """逐行读取 JSON Lines 格式（每行一个 JSON 对象）"""
    with open(filepath, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                yield json.loads(line)

# 写入 JSON Lines
def write_json_lines(filepath: str, items: list):
    with open(filepath, "w", encoding="utf-8") as f:
        for item in items:
            f.write(json.dumps(item, ensure_ascii=False) + "\n")
```

#### AI 项目中的 JSON 读写模式

```python
# ──── 保存/加载对话历史 ────
def save_conversation(history: list, path: str = "chat_history.json"):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(history, f, ensure_ascii=False, indent=2)

def load_conversation(path: str) -> list:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

# ──── API 请求/响应的 JSON 处理 ────
import httpx

async def call_llm(prompt: str) -> dict:
    payload = {
        "model": "gpt-4",
        "messages": [{"role": "user", "content": prompt}],
        "temperature": 0.7,
    }
    async with httpx.AsyncClient() as client:
        resp = await client.post(
            "https://api.openai.com/v1/chat/completions",
            json=payload,            # httpx 自动 json.dumps
        )
        return resp.json()           # httpx 自动 json.loads

# ──── 配置文件管理 ────
DEFAULT_CONFIG = {
    "model": "gpt-4",
    "temperature": 0.7,
    "max_tokens": 2048,
}

def load_config(path: str = "config.json") -> dict:
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        save_config(path, DEFAULT_CONFIG)
        return DEFAULT_CONFIG

def save_config(path: str, config: dict):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(config, f, indent=2, ensure_ascii=False)
    print(f"配置文件已保存: {path}")

# ──── 处理 API 返回的 JSON 流（SSE 事件流） ────
async def stream_json_events(url: str):
    async with httpx.AsyncClient() as client:
        async with client.stream("GET", url) as resp:
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    yield json.loads(line[6:])  # 去掉 "data: " 前缀
```

#### 常见错误

```python
# ❌ 忘记 ensure_ascii=False → 中文变成转义
json.dumps({"name": "张三"})        # {"name": "\u5f20\u4e09"}
json.dumps({"name": "张三"}, ensure_ascii=False)  # {"name": "张三"} ✅

# ❌ 忘记 indent → 文件一坨，不可读
json.dump(data, f)                  # 一行
json.dump(data, f, indent=2)        # 格式化 ✅

# ❌ JSON key 必须是双引号
json.loads("{'name': 'hello'}")      # ❌ json.decoder.JSONDecodeError
json.loads('{"name": "hello"}')      # ✅

# ❌ JSON 不允许末尾逗号
json.loads('[1, 2, 3,]')             # ❌
json.loads('[1, 2, 3]')              # ✅

# ❌ 混合类型 key 的 dict
json.dumps({1: "a", "1": "b"})       # ❌ 会丢数据（1 → "1" 覆盖）
# dumps 会先把所有 key 转 str，1 和 "1" 冲突
```

### 1.8.6 pathlib（推荐）

**下面是 pathlib（推荐） 的代码示例：**
```python
from pathlib import Path

# 创建路径
p = Path("/home/user/data/config.json")
p = Path("relative/path")
p = Path.home() / "data" / "config.json"  # 用 / 拼接路径

# 路径属性
print(p.name)       # config.json
print(p.stem)       # config（无后缀）
print(p.suffix)     # .json
print(p.parent)     # /home/user/data
print(p.parents)    # [data, user, home, /] 所有上级目录

# 路径存在性
print(p.exists())       # 是否存在
print(p.is_file())      # 是否是文件
print(p.is_dir())       # 是否是目录

# 文件操作
content = p.read_text(encoding="utf-8")        # 读取文本文件
p.write_text("Hello", encoding="utf-8")        # 写入文本文件
data = p.read_bytes()                           # 读取二进制文件
p.write_bytes(b"binary data")                   # 写入二进制文件

# 目录操作
p.mkdir(exist_ok=True)                          # 创建目录
p.mkdir(parents=True, exist_ok=True)            # 递归创建
p.rmdir()                                        # 删除空目录

# 遍历目录
for f in Path(".").glob("*.py"):               # 通配符匹配
    print(f.name)
for f in Path(".").rglob("**/*.py"):            # 递归匹配
    print(f)

# AI 项目中常用
project_root = Path(__file__).parent.parent      # 项目根目录
data_dir = project_root / "data" / "raw"
data_dir.mkdir(parents=True, exist_ok=True)

config_path = project_root / "config.yaml"
if not config_path.exists():
    raise FileNotFoundError(f"配置文件不存在: {config_path}")

# 替换 os.path 旧写法
# 旧: os.path.join(os.path.dirname(__file__), "data", "config.json")
# 新: Path(__file__).parent / "data" / "config.json"
```

#### `Path.read_text()` vs `open()`：区别与选择

```
                    Path.read_text() / Path.write_text()          open()
                    ─────────────────────────────────────         ────
本质                Path 的便捷方法                                Python 内置函数
返回值              read_text → str（整个文件）                     read() → str 或 逐行迭代
                    write_text → None（写整个文件）                 write() → 可多次写入
文件打开          自动打开和关闭                                 需要 with 手动管理
一次性读写         ✅ 是（整个文件一次读完/写完）                  ❌ 不是（可逐行/逐块）
大文件处理         ❌ 不适合（全量加载到内存）                     ✅ 适合（循环 readline / 迭代）
写入模式          仅覆盖写（"w"）                                  "w" / "a" / "x" / "r+" 全支持
编码              必须显式 encoding=                              必须显式 encoding=
异常处理          无特殊（FileNotFoundError 等）                  同左
链式调用          可以：p.read_text().strip().upper()             不行（open 返回文件对象）
代码行数          1 行                                            3 行（with open + 缩进块）
```

**`Path.read_text()` 适合——纯读/写，一步到位：**

```python
# ✅ 配置文件一次性读取
config = json.loads(Path("config.json").read_text(encoding="utf-8"))

# ✅ 小文件快速读取
prompt = Path("prompt.txt").read_text(encoding="utf-8")

# ✅ 覆盖写入
Path("output.txt").write_text("Hello", encoding="utf-8")

# ✅ 链式操作
words = Path("text.txt").read_text(encoding="utf-8").split()
```

**`open()` 适合——需要精细控制或处理大文件：**

```python
# ✅ 大文件逐行读（不占内存）
with open("big_file.log", "r", encoding="utf-8") as f:
    for line in f:
        process(line)  # 一次只处理一行

# ✅ 追加写入
with open("log.txt", "a", encoding="utf-8") as f:
    f.write("新的一行\n")

# ✅ 混合读写模式
with open("data.txt", "r+", encoding="utf-8") as f:
    content = f.read()
    f.seek(0)
    f.write("修改后的内容")

# ✅ 二进制流处理
with open("image.jpg", "rb") as f:
    chunk = f.read(4096)  # 按块读
```

**它们不是重复——是不同抽象层次：**

```
抽象层次：
  Path.read_text()      ← 最上层：路径 → 字符串，一句搞定
       │
  open() + read()       ← 中间层：文件对象 → 字符串，可控
       │
  os.open() + os.read() ← 最底层：文件描述符 → bytes，最灵活

选择标准：
  整个文件小（< 100MB） 且 一次性操作         → Path.read_text()
  需要追加/逐行/精细控制/处理大文件            → open()
  只是读/写，不做别的事                       → Path.read_text()
  又要读又要写，或者要 seek                   → open()
```

**混合使用也是最常见的模式：**

```python
# 用 pathlib 处理路径，用 open 处理文件
data_dir = Path("data")
data_dir.mkdir(exist_ok=True)

file_path = data_dir / "records.json"

# pathlib 帮忙构建路径和检查
if not file_path.exists():
    raise FileNotFoundError(f"文件不存在: {file_path}")

# open 负责大文件逐行处理
with open(file_path, "r", encoding="utf-8") as f:
    for line in f:
        record = json.loads(line)
        process(record)
```

**一句话：`Path.read_text()` 是 `open().read()` 的语法糖，省掉 3 行模板代码，但失去控制权。小文件用 pathlib，大文件用 open，不冲突。**
```

### 1.8.7 CSV 读写

**下面是 CSV 读写 的代码示例：**
```python
import csv

# 读 CSV
with open("data.csv", "r", encoding="utf-8") as f:
    reader = csv.reader(f)
    header = next(reader)  # 跳过表头
    for row in reader:
        print(row)  # ['value1', 'value2', 'value3']

# DictReader（推荐，按列名访问）
with open("data.csv", "r", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        print(row["name"], row["score"])

# 写 CSV
with open("output.csv", "w", encoding="utf-8", newline="") as f:
    writer = csv.writer(f)
    writer.writerow(["name", "score", "active"])  # 表头
    writer.writerow(["Alice", 95, True])
    writer.writerow(["Bob", 87, False])
```

### 1.8.8 内存文件（BytesIO / StringIO）

**下面是 内存文件（BytesIO / StringIO） 的代码示例：**
```python
from io import StringIO, BytesIO

# StringIO：在内存中读写字符串（就像文件一样）
buffer = StringIO()
buffer.write("第一行\n")
buffer.write("第二行\n")
buffer.seek(0)
print(buffer.read())  # 第一行\n第二行\n
content = buffer.getvalue()  # 获取全部内容

# BytesIO：在内存中读写二进制数据
bio = BytesIO()
bio.write(b"\x00\x01\x02")
bio.seek(0)
data = bio.read()

# 应用场景：临时处理数据而不写磁盘
import json

def process_json_string(json_str: str) -> dict:
    """处理 JSON 字符串（模拟文件处理）"""
    f = StringIO(json_str)
    return json.load(f)

data = process_json_string('{"key": "value"}')
```

### 1.8.9 临时文件

**下面是 临时文件 的代码示例：**
```python
import tempfile

# 创建临时文件（自动删除）
with tempfile.NamedTemporaryFile(mode="w", suffix=".txt", delete=True) as f:
    f.write("临时数据")
    f.flush()
    # 此时可以拿到临时文件路径处理
    print(f.name)  # /tmp/tmpXXXXXX.txt
# with 结束自动删除

# 创建临时目录
with tempfile.TemporaryDirectory() as tmp_dir:
    print(tmp_dir)  # /tmp/tmpXXXXXX/
    # 在此目录下工作
# 目录自动删除

# 持久化临时文件（不自动删除）
import tempfile
tmp = tempfile.NamedTemporaryFile(delete=False)
print(tmp.name)
tmp.close()
# 需要手动删除
import os
os.unlink(tmp.name)
```

### 1.8.10 文件编码

**下面是 文件编码 的代码示例：**
```python
# 编码是文件读写最常见的坑

# 写入时指定编码（UTF-8 是默认，但不是所有系统）
with open("file.txt", "w", encoding="utf-8") as f:
    f.write("中文")  # 编码为 UTF-8 字节

# 读取时必须用相同的编码
with open("file.txt", "r", encoding="utf-8") as f:
    text = f.read()  # 正确

# 编码不匹配 → UnicodeDecodeError 或乱码
with open("file.txt", "r", encoding="gbk") as f:
    # UnicodeDecodeError: 'gbk' codec can't decode byte...
    text = f.read()

# 常见编码：
# utf-8    → 通用标准（推荐）
# gbk      → 中文 Windows 默认
# latin-1  → 不抛异常（可读取任何字节）
# utf-8-sig → 带 BOM 的 UTF-8（Windows 记事本导出）

# 不确定编码时
import chardet  # pip install chardet
with open("unknown.txt", "rb") as f:
    raw = f.read(10000)
    result = chardet.detect(raw)
    print(result)  # {'encoding': 'utf-8', 'confidence': 0.99}

# 然后用检测到的编码读取
with open("unknown.txt", "r", encoding=result["encoding"]) as f:
    text = f.read()
```

### 1.8.11 常见错误

**以下是常见的错误示例：**
```python
# 1. 忘记关闭文件
f = open("file.txt")
data = f.read()
# 如果发生异常，文件不会关闭 → 文件句柄泄漏

# 正确：使用 with
with open("file.txt") as f:
    data = f.read()

# 2. 读取大文件一次性全部加载
with open("huge_file.log") as f:
    lines = f.readlines()  # 如果文件 10GB → 内存溢出！

# 正确：逐行迭代
with open("huge_file.log") as f:
    for line in f:  # 一次只读一行到内存
        process(line)

# 3. 二进制模式读写文本
with open("image.jpg", "r") as f:  # 错误！
    data = f.read()  # 乱码

# 正确：
with open("image.jpg", "rb") as f:
    data = f.read()  # bytes

# 4. 编码未指定
with open("file.txt") as f:  # 默认系统编码（不同系统可能不同）
    text = f.read()
# Mac 上默认 utf-8，Windows 上默认 gbk → 跨平台问题

# 正确：始终显式指定编码
with open("file.txt", encoding="utf-8") as f:
    text = f.read()

# 5. 写模式下文件原有内容被清空
with open("data.txt", "w") as f:  # "w" 会清空文件！
    f.write("新内容")
# 原有数据全部丢失！

# 追加用 "a"
with open("data.txt", "a") as f:
    f.write("追加的内容\n")

# 6. 换行符跨平台问题
# Windows: \r\n, Linux: \n, Mac: \n
# 文本模式读取时自动转换（\r\n → \n）
# 二进制模式读取时不转换
```

---

# 第二阶段：Python 进阶

---

## 2.1 装饰器

### 2.1.1 一句话核心本质

**装饰器是接受函数返回新函数的高阶函数，`@decorator` 语法糖等价于 `func = decorator(func)`，在函数定义时执行。**

### 2.1.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 实现方式 | 注解（Annotation）+ 反射/APT，编译期保留或运行时反射 | 装饰器是运行时高阶函数，定义时立即执行 |
| 修改能力 | 注解本身不修改行为，需配合反射/代理/AOP 框架（如 Spring AOP） | 装饰器直接返回新函数替换原函数，零框架依赖 |
| 执行时机 | Spring AOP 在 Bean 初始化时通过代理织入 | `@decorator` 在类/函数定义语句执行时立即调用 |
| 参数传递 | 注解属性编译期确定，只能是常量 | 装饰器参数可以是任意表达式、函数调用、变量 |
| 堆叠顺序 | 多层 AOP 由代理嵌套顺序决定 | 多层 `@A @B def f()` 等价于 `f = A(B(f))`，从下到上 |
| 典型场景 | `@Transactional`, `@Cacheable`, `@Controller` | `@route`, `@staticmethod`, `@retry`, `@timer` |

**本质区别：** Java 注解是声明式元数据，行为由框架在运行时解释；Python 装饰器是代码变换，在定义时直接重写函数。Python 装饰器运行时无额外开销（已替换完成），Java AOP 代理每次调用都有拦截开销。

### 2.1.3 技术原理：装饰器执行流程

**`@decorator` 的完整执行过程（以 `@log_calls def add(a, b)` 为例）：**

```
Python 解析到 @log_calls 时的执行流程：

1. 解析 def add(a, b)：
   ┌─────────────────────────────────────────────┐
   │ 编译函数体 → 创建 function 对象              │
   │ add = <function add at 0x...>               │
   │ add.__name__ = "add"                        │
   │ add.__code__ = <code object add>            │
   └─────────────────────────────────────────────┘
                       ↓
2. 遇到 @log_calls（装饰器表达式求值）：
   ┌─────────────────────────────────────────────┐
   │ log_calls 已经定义（在上一行或之前）          │
   │ log_calls = <function log_calls at 0x...>    │
   └─────────────────────────────────────────────┘
                       ↓
3. 执行装饰器调用：add = log_calls(add)
   ┌─────────────────────────────────────────────┐
   │ log_calls(add) 被调用：                      │
   │   ┌─ def wrapper(*args, **kwargs):          │
   │   │     print(f"调用: {func.__name__}")     │
   │   │     return func(*args, **kwargs)        │
   │   └─ return wrapper  ← 返回新函数           │
   │                                              │
   │ add 变量现在指向 wrapper：                    │
   │ add = <function log_calls.<locals>.wrapper>  │
   │ add.__name__ = "wrapper"  ← 名字变了！       │
   └─────────────────────────────────────────────┘
                       ↓
4. 后续调用 add(1, 2)：
   ┌─────────────────────────────────────────────┐
   │ 实际执行的是 wrapper(1, 2)                   │
   │   → print("调用: add")                      │
   │   → 执行原始 add(1, 2) → 返回 3             │
   └─────────────────────────────────────────────┘
```

**多层装饰器堆叠顺序（从下到上）：**

```
@A
@B
@C
def f(): ...

# 等价执行顺序：
# 1. 创建原始函数 f_raw
# 2. f = C(f_raw)    ← C 最靠近函数，先执行
# 3. f = B(f)        ← B 接收 C(f_raw) 的结果
# 4. f = A(f)        ← A 接收 B(C(f_raw)) 的结果
#
# 调用 f() 时执行顺序：A.wrapper → B.wrapper → C.wrapper → f_raw
```

### 2.1.4 基本用法

**以下是基本用法示例：**
```python
def log_calls(func):
    """最简单的装饰器：在每次调用前后打印日志"""
    def wrapper(*args, **kwargs):
        print(f"[调用] {func.__name__}(args={args}, kwargs={kwargs})")
        result = func(*args, **kwargs)
        print(f"[返回] {func.__name__} -> {result}")
        return result
    return wrapper

@log_calls
def add(a, b):
    return a + b

add(1, 2)
# [调用] add(args=(1, 2), kwargs={})
# [返回] add -> 3

@log_calls
def greet(name, greeting="Hello"):
    return f"{greeting}, {name}!"

greet("Python", greeting="Hi")
# [调用] greet(args=("Python",), kwargs={'greeting': 'Hi'})
# [返回] greet -> Hi, Python!
```

**Java 对比：** 上述等价于 Spring AOP 的 `@Around` 通知。但 Python 不需要定义切面类、不需要注解处理器、不需要代理工厂，函数定义本身就完成了全部 AOP。

### 2.1.5 带参数装饰器

 **`@repeat(3)` 等价于 `repeat(3)(greet)`**

```python
@repeat(3)    # ① 先求值 repeat(3)，结果必须是一个可调用对象
def greet():  # ② 该可调用对象被调用，参数是 greet
    pass
# 等价于：greet = repeat(3)(greet)
```

`repeat(3)` 先执行，所以 `repeat` 必须接收参数（而不是函数）；它的返回值再被调用、接收函数。这自然需要两层：一层接收参数，一层接收函数。加上 wrapper 总共 3 层。

**3 层结构的职责划分**

```python
def repeat(n: int):                    # 【第 1 层】接收装饰器参数
    """n 在此被捕获到闭包中"""
    def decorator(func):               # 【第 2 层】接收被装饰函数
        """func 在此被捕获到闭包中"""
        def wrapper(*args, **kwargs):  # 【第 3 层】接收实际调用参数，替换原函数
            for _ in range(n):
                result = func(*args, **kwargs)
            return result
        return wrapper
    return decorator

# 调用链：
# repeat(3) → decorator → wrapper
# greet = repeat(3)(greet)  → decorator(greet) → wrapper
# greet("World")  → wrapper("World")
```

```
内存布局：
┌─────────────────────────────────────────────────┐
│  repeat(3) 的闭包                                │
│  ┌─────┐                                        │
│  │ n=3 │  ← 第 1 层捕获                          │
│  └─────┘                                        │
│  ┌──────────────────────────────────────────┐    │
│  │ decorator(greet) 的闭包                   │    │
│  │  ┌──────┐                               │    │
│  │  │func= │  ← 第 2 层捕获                  │    │
│  │  │greet │                               │    │
│  │  └──────┘                               │    │
│  │  ┌───────────────────────────────────┐  │    │
│  │  │ wrapper(*args) 的闭包              │  │    │
│  │  │  (无需额外捕获，n 和 func 来自外层) │  │    │
│  │  └───────────────────────────────────┘  │    │
│  └──────────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```




### 2.1.6 functools.wraps 原理

不加 `@wraps` 时，装饰后的函数元信息会丢失——这在实际工程中会造成严重问题（文档丢失、签名错误、调试困难）：

```python
import functools

def bad_decorator(func):
    def wrapper(*args, **kwargs):
        """我是 wrapper 的文档"""
        return func(*args, **kwargs)
    return wrapper

def good_decorator(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        """我是 wrapper 的文档"""
        return func(*args, **kwargs)
    return wrapper

@bad_decorator
def hello(name):
    """Say hello to someone"""
    return f"Hello {name}"

@good_decorator
def hello2(name):
    """Say hello to someone"""
    return f"Hello {name}"

print(hello.__name__)    # "wrapper"       ← 原始函数名丢失！
print(hello.__doc__)     # "我是 wrapper 的文档"  ← 文档覆盖！
print(hello2.__name__)   # "hello2"         ← 正确保留
print(hello2.__doc__)    # "Say hello to someone"  ← 正确保留
```

**`functools.wraps` 的本质：**

```
@functools.wraps(func) def wrapper():
  ↓
wrapper.__name__ = func.__name__
wrapper.__doc__ = func.__doc__
wrapper.__module__ = func.__module__
wrapper.__qualname__ = func.__qualname__
wrapper.__annotations__ = func.__annotations__
wrapper.__dict__.update(func.__dict__)
wrapper.__wrapped__ = func  # 保留原始函数引用
```

关键点：`@wraps` 本身也是一个装饰器，它把 `func` 的元属性复制到 `wrapper` 上，使装饰后的函数"看起来像"原函数。

### 2.1.7 类装饰器 vs 装饰器类

**一、用类实现装饰器（通过 `__call__`）：**

```python
import functools

class CountCalls:
    def __init__(self, func):
        functools.update_wrapper(self, func)
        self.func = func
        self.calls = 0

    def __call__(self, *args, **kwargs):
        self.calls += 1
        print(f"调用 {self.func.__name__} 第 {self.calls} 次")
        return self.func(*args, **kwargs)

@CountCalls
def process(text):
    return text.upper()

print(process("hello"))  # 调用 process 第 1 次\nHELLO
print(process("world"))  # 调用 process 第 2 次\nWORLD
print(process.calls)     # 2  ← 状态存储在类实例中
```

**二、带参数的类装饰器：**

```python
def add_method(method_name: str):
    """带参数的类装饰器：给类动态添加指定名字的方法"""
    def decorator(cls):
        def new_method(self):
            return f"这是动态添加的方法 '{method_name}'"
        setattr(cls, method_name, new_method)
        return cls
    return decorator

@add_method("greet")
class MyClass:
    pass

obj = MyClass()
print(obj.greet())  # "这是动态添加的方法 'greet'"

# 等价于：add_method("greet")(MyClass)
#                              ↑
#          add_method("greet") 返回 decorator
#          decorator(MyClass)  执行修改

# 也可以用类实现（装饰器类）：
class AddProperty:
    """类装饰器的类实现"""
    def __init__(self, name: str, value):
        self.name = name
        self.value = value

    def __call__(self, cls):
        setattr(cls, self.name, self.value)
        return cls

@AddProperty("version", "1.0")
class Config:
    pass

print(Config.version)  # "1.0"
```

**三、装饰器在类上的特殊行为：**

```python
def log_methods(cls):
    """类装饰器：自动为类的每个方法添加日志"""
    import functools
    for name, val in cls.__dict__.items():
        if callable(val):
            @functools.wraps(val)
            def logged(self, *args, **kwargs):
                print(f"[调用] {name}")
                return val(self, *args, **kwargs)
            setattr(cls, name, logged)
    return cls

@log_methods
class Service:
    def process(self, data):
        return data * 2

s = Service()
s.process(10)  # [调用] process\n20
```

### 2.1.8 装饰器堆叠顺序详解

**下面是 装饰器堆叠顺序详解 的代码示例：**
```python
@A
@B
@C
def f():
    pass

# 执行顺序（从下到上）：
# step 1: f = C(f)     ← C 最靠近函数
# step 2: f = B(f)     ← B 装饰的是 C 返回的 wrapper
# step 3: f = A(f)     ← A 装饰的是 B 返回的 wrapper

# 调用 f() 时的执行顺序（从上到下，像洋葱）：
# A.wrapper → B.wrapper → C.wrapper → f_raw → C.wrapper返回 → B.wrapper返回 → A.wrapper返回

import functools

def A(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        print("A before")
        result = func(*args, **kwargs)
        print("A after")
        return result
    return wrapper

def B(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        print("  B before")
        result = func(*args, **kwargs)
        print("  B after")
        return result
    return wrapper

def C(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        print("    C before")
        result = func(*args, **kwargs)
        print("    C after")
        return result
    return wrapper

@A
@B
@C
def say(msg):
    print(f"      {msg}")

say("Hello")
# A before
#   B before
#     C before
#       Hello
#     C after
#   B after
# A after
```

**工程原则：** 离函数最近的装饰器（最下面）先执行 wrapper 的进入逻辑，最后执行退出逻辑——这是装饰器堆叠的"洋葱模型"。

### 2.1.9 常见错误

#### 错误 1：忘记返回 wrapper

```python
def wrong_decorator(func):
    def wrapper(*args, **kwargs):
        print("before")
        func(*args, **kwargs)  # 没有 return！
    # 没有 return wrapper！← 装饰后 func 变成 None
```

#### 错误 2：不使用 `@wraps` 造成调试困难**

```python
def no_wraps(func):
    def wrapper(*args, **kwargs):
        return func(*args, **kwargs)
    return wrapper

@no_wraps
def important():
    """Critical business logic"""
    pass

help(important)  # 函数名变成 wrapper，文档丢失
```

#### 错误 3：装饰器参数忘记加括号

```python
@repeat      # ← 错误：没有加括号，等价于 repeat(greet)
def greet(): ...

@repeat(3)   # ← 正确：repeat(3) 返回 decorator
def greet(): ...
```

#### 错误 4：类装饰器用于方法时，`__call__` 收到意外参数

先理解正常方法调用——Python 会自动把实例传给 `self`：

```python
class Service:
    def run(self, data):
        return f"self={self}, data={data}"

s = Service()
s.run("test")
```

```
正常方法调用机制：
① s.run → Service.tp_dict 找到 run（函数对象）
② 函数是 descriptor → 触发 __get__ → 返回绑定方法
③ 绑定方法已记住 im_self=s
④ ("test") → 实际调 run(s, "test")
⑤ self=s, data="test"  ✓
```

现在看类装饰器的问题——`@Echo` 把方法变成了 Echo 实例：

```python
class Echo:
    def __init__(self, func):
        self.func = func
    def __call__(self, *args, **kwargs):
        print(f"[Echo] args={args}")
        return self.func(*args, **kwargs)

@Echo                         # greet = Echo(greet)
def greet(name):              # greet 是 Echo 实例
    return f"Hello {name}"

greet("World")
# 输出：[Echo] args=('World',)
# 返回："Hello World"  ✓
# 调用链：Echo.__call__(echo_inst, "World")
# → self.func("World") → greet("World")  ✓
```

但如果用 `@Echo` 装饰**类的方法**：

```python
class Service:
    @Echo                     # run = Echo(原始 run 函数)
    def run(self, data):      # run 是 Echo 实例，不是函数！
        return f"self={self}, data={data}"

s = Service()
s.run("test")
```

```
调用链：
① s.run → Python 找 Service.tp_dict["run"]
   → 发现值是一个 Echo 实例（不是函数）
   → Echo 实例没有 __get__ 方法（不是 descriptor）
   → 所以不触发方法绑定！直接返回 Echo 实例本身

② ( ) 调用 Echo 实例
   → Echo.__call__(echo_inst, "test")
   → args = ("test",)     ← 没有 s！Python 没有把 s 传进来！
   → print args → ('test',)

③ self.func("test")
   → 原始函数 run(self, data)
   → 收到 self="test", data 缺失
   → TypeError: run() missing 1 required positional argument: 'data'

根因：类装饰器把方法变成了"非函数"对象，破坏了 Python 的方法绑定机制。
     正常函数的 __get__ 会自动注入实例，但 Echo 实例没有 __get__。
```

**那 `Service.run(s, "test")` 能工作吗？**

```python
Service.run(s, "test")
# 调用链：
# Service.run → 取到 Echo 实例
# (s, "test") → Echo.__call__(echo_inst, s, "test")
# → args = (s, "test")
# → self.func(s, "test") → run(self=s, data="test")  ✓ 碰巧正确

# 但这不是你想要的 s.run("test") 语法！
```

**解决方案：**

```python
# 方案1：用函数装饰器代替类装饰器（推荐）
def echo(func):
    def wrapper(*args, **kwargs):
        print(f"[Echo] args={args}")
        return func(*args, **kwargs)
    return wrapper

class Service:
    @echo                    # 函数装饰器，保留 descriptor 特性
    def run(self, data):
        return f"self={self}, data={data}"

s = Service()
s.run("test")                # ✓ 正常！函数装饰器返回的是函数
# 输出：[Echo] args=(<Service object>, 'test')

# 方案2：类装饰器手动实现 __get__（让 Echo 变成 descriptor）
class Echo:
    def __init__(self, func):
        self.func = func
    def __call__(self, *args, **kwargs):
        return self.func(*args, **kwargs)
    def __get__(self, obj, objtype=None):
        """让 Echo 实例也支持方法绑定"""
        if obj is None:
            return self
        import functools
        return functools.partial(self, obj)  # 绑定 obj 到第一个参数

class Service:
    @Echo
    def run(self, data):
        return f"self={self}, data={data}"

s = Service()
s.run("test")                # ✓ 现在正常了！__get__ 自动绑定实例
```

**一句话总结**：类装饰器把函数变成了类实例，而类实例不是 descriptor，所以 Python 不触发方法绑定、不自动传 `self`。用函数装饰器（返回函数的）可以避免这个问题，因为函数本身就是 descriptor。

#### 错误 5：装饰器执行时机误解——`@` 在定义时执行，不是调用时！

Java 开发者最容易犯的错。Java 的 `@Override` 是元数据（编译时读取），Python 的 `@decorator` 是**可执行代码**——它在 `def` 语句执行时**当场调用**。

```python
import time

def timestamp(func):
    print(f"[装饰器执行] {time.strftime('%H:%M:%S')}")
    return func

print("1. 开始导入模块")

@timestamp
def f():
    print("2. 函数被调用了")

print("3. 模块导入完成")

f()
print("4. 函数返回")
```

```
输出：
  [装饰器执行] 10:00:00    ← @timestamp 在 def f 时就执行了！
  1. 开始导入模块          ← 但实际上 print("1.") 在 def 之后
  3. 模块导入完成          ← 注意：1 和 3 的顺序取决于 def 的位置
  2. 函数被调用了          ← f() 调用时才执行函数体
  4. 函数返回
```

**为什么顺序是这样？**

```
@timestamp                 ← ① @ 是语法糖
def f():                   ← def f() 先被处理
    pass                   ← 函数对象被创建，赋值给变量 f
                           ← ② 然后 timestamp(f) 立即执行！
                           ← ③ 结果重新赋值给 f

等价于：
def f():
    pass
f = timestamp(f)           ← 定义完 f 后立即执行 timestamp(f)
                           ③   ①       ②
```

**实际影响：装饰器里的耗时操作（加载模型、连接数据库）会卡住模块导入！**

```python
# ❌ AI 项目常见错误：在装饰器里加载大模型
MODEL_CACHE = {}

def load_ai_model(model_name):
    print(f"正在加载 {model_name}...")
    MODEL_CACHE[model_name] = HeavyModel(model_name)  # 耗时 10 秒！
    def decorator(func):
        def wrapper(*args, **kwargs):
            model = MODEL_CACHE[model_name]
            return func(model, *args, **kwargs)
        return wrapper
    return decorator

@load_ai_model("gpt-4")     # ← 导入模块时就执行！
def chat(model, prompt):
    return model.generate(prompt)

# 用户只是想导入你写的库，结果被卡了 10 秒！

# ✅ 正确：用懒加载，在第一次调用时再加载
MODEL_CACHE = {}

def lazy_load(model_name):
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            if model_name not in MODEL_CACHE:
                print(f"首次调用时加载 {model_name}")
                MODEL_CACHE[model_name] = HeavyModel(model_name)
            model = MODEL_CACHE[model_name]
            return func(model, *args, **kwargs)
        return wrapper
    return decorator

@lazy_load("gpt-4")         # 只返回 wrapper，不加载模型
def chat(model, prompt):
    return model.generate(prompt)
```

**总结**：`@` 的本质就是 `f = decorator(f)`，它在 `def` 语句执行时立即求值。Java 的 `@` 是声明式元数据，Python 的 `@` 是命令式函数调用——这两个名字一样但行为截然不同。

### 2.1.10 AI 场景案例

#### 场景一：重试 + 退避 + 监控（AI 服务调用标配）

```python
import time
import functools
import logging

logger = logging.getLogger(__name__)

def retry(max_attempts=3, delay=1, backoff=2, exceptions=(Exception,)):
    """带指数退避的自动重试装饰器，AI 服务调用必备"""
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            last_exception = None
            for attempt in range(1, max_attempts + 1):
                try:
                    return func(*args, **kwargs)
                except exceptions as e:
                    last_exception = e
                    if attempt == max_attempts:
                        logger.error(f"重试 {max_attempts} 次全部失败: {func.__name__}")
                        raise
                    wait = delay * (backoff ** (attempt - 1))
                    logger.warning(f"第 {attempt} 次失败 ({e}), {wait}s 后重试...")
                    time.sleep(wait)
            return None  # unreachable
        return wrapper
    return decorator

@retry(max_attempts=5, delay=1, backoff=2, exceptions=(TimeoutError, ConnectionError))
def call_llm_api(prompt: str) -> str:
    """调用 LLM API，自动处理超时和连接错误"""
    return openai.ChatCompletion.create(
        model="gpt-4",
        messages=[{"role": "user", "content": prompt}]
    )
```

#### 场景二：性能监控装饰器

```python
import time
import functools
from dataclasses import dataclass, field
from typing import Dict

@dataclass
class MetricsCollector:
    stats: Dict[str, list] = field(default_factory=dict)

    def record(self, name: str, duration: float):
        if name not in self.stats:
            self.stats[name] = []
        self.stats[name].append(duration)

    def report(self):
        for name, times in self.stats.items():
            avg = sum(times) / len(times)
            print(f"{name}: 调用 {len(times)} 次, 平均 {avg*1000:.1f}ms")

metrics = MetricsCollector()

def monitor(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        try:
            return func(*args, **kwargs)
        finally:
            duration = time.perf_counter() - start
            metrics.record(func.__name__, duration)
    return wrapper

class EmbeddingService:
    @monitor
    @retry(max_attempts=3)
    def embed(self, texts: list[str]) -> list[list[float]]:
        return openai.Embedding.create(input=texts, model="text-embedding-3-small")

# 运行后查看指标
# metrics.report()
# embed: 调用 42 次, 平均 231.5ms
```

#### 场景三：输入验证/缓存装饰器

```python
import functools
import hashlib
import json

def validate_input(min_length=1, max_length=10000):
    """AI 输入验证：防止注入和非法输入"""
    def decorator(func):
        @functools.wraps(func)
        def wrapper(prompt: str, *args, **kwargs):
            if not isinstance(prompt, str):
                raise TypeError(f"prompt 必须是字符串, 收到 {type(prompt)}")
            if len(prompt) < min_length:
                raise ValueError(f"prompt 太短 ({len(prompt)} < {min_length})")
            if len(prompt) > max_length:
                raise ValueError(f"prompt 太长 ({len(prompt)} > {max_length})")
            return func(prompt, *args, **kwargs)
        return wrapper
    return decorator

def cache_result(func):
    """AI 请求缓存：相同输入直接返回缓存"""
    cache = {}
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        key = hashlib.md5(
            json.dumps((args, kwargs), sort_keys=True, default=str).encode()
        ).hexdigest()
        if key in cache:
            print(f"[缓存命中] {func.__name__}")
            return cache[key]
        result = func(*args, **kwargs)
        cache[key] = result
        return result
    return wrapper

class AIService:
    @validate_input(min_length=1, max_length=8000)
    @cache_result
    @retry(max_attempts=3)
    @monitor
    def chat(self, prompt: str) -> str:
        """多层装饰器堆叠：验证 → 缓存 → 重试 → 监控"""
        return openai.ChatCompletion.create(...)
```

---

## 2.2 生成器

### 2.2.1 一句话核心本质

**生成器是惰性执行的迭代器——函数体内出现 `yield` 关键字时，该函数返回一个生成器对象，每次调用 `next()` 执行到下一个 `yield` 并暂停，函数帧保持存活。**

### 2.2.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 惰性序列 | `Stream.iterate()` / `Stream.generate()`（Java 8+） | 生成器表达式 `(x for x in range(n))` 或 `yield` 函数 |
| 状态保持 | Stream 需要外部状态或 lambda 闭包 | 生成器自动保持函数帧（所有局部变量、指令指针） |
| 实现复杂度 | 需要实现 `Spliterator` 或 `Iterator<T>` 接口 | 一个 `yield` 关键字自动完成 |
| 双向通信 | 不支持（Stream 是单向的） | `gen.send(value)` 从外部向生成器发送值，`gen.throw()` 抛出异常 |
| 资源释放 | 需要显式 `close()` 或 try-with-resources | `gen.close()` 或在 with 块中使用 |

**本质区别：** Java Stream 是"数据流"的抽象，需要 Collector 终结操作才触发计算；Python 生成器是"暂停的函数"，每次 `next()` 恢复执行，函数帧完整保留。Python 生成器更底层——它实际上是协程的前身（PEP 342）。

### 2.2.3 技术原理：生成器内存模型

**一个包含 `yield` 的函数与普通函数的区别：**

```
普通函数：
┌────────────────────────────────────────┐
│ def add(a, b):                         │
│     return a + b                       │
│                                        │
│ add(1, 2) → 调用 → 创建栈帧 → 执行 →   │
│ 返回结果 → 销毁栈帧                    │
└────────────────────────────────────────┘

生成器函数：
┌────────────────────────────────────────┐
│ def count_up_to(n):                    │
│     i = 0                              │
│     while i < n:                       │
│         yield i        ← 暂停点         │
│         i += 1                         │
│                                        │
│ gen = count_up_to(3)  → 不执行函数体！   │
│   → 返回生成器对象                      │
│                                        │
│ next(gen) → 创建栈帧 → 执行到 yield →   │
│   → 冻结栈帧 → 返回值 → 保存状态         │
│                                        │
│ next(gen) → 恢复栈帧 → 继续执行 →       │
│   ... → 下一个 yield / StopIteration    │
└────────────────────────────────────────┘
```

**生成器对象的内部结构（CPython 源码 `Include/internal/pycore_genobject.h`）：**

```
typedef struct {
    PyObject_HEAD                          # 对象头（ob_refcnt, ob_type）
    PyFrameObject *gi_frame;              # 冻结的栈帧（关键！函数暂停时的全部状态）
    PyObject *gi_code;                    # 代码对象
    PyObject *gi_name;                    # 生成器名
    PyObject *gi_qualname;               # 限定名
    PyObject *gi_running;                 # 是否正在运行
    PyObject *gi_yieldfrom;               # yield from 的子生成器
    int gi_exc_state;                     # 异常状态
    int gi_suspended;                     # 是否暂停
    int running;                          # 运行标志（防止递归）
} PyGenObject;
```

**关键：** 生成器对象包含一个 `gi_frame`（栈帧指针），函数暂停时整个栈帧（局部变量、指令指针、异常状态）都冻结在堆上，这是生成器能"记住执行到哪里"的根本原因。

### 2.2.4 生成器执行流程

```
调用 gen = my_gen():
┌──────────────────────────────────────────────────────┐
│ my_gen() 识别到 yield → 不执行函数体                  │
│   → 创建 PyGenObject                                │
│   → gi_frame = NULL（尚未执行）                       │
│   → 返回生成器对象                                   │
└──────────────────────────────────────────────────────┘

第一次调用 next(gen):
┌──────────────────────────────────────────────────────┐
│ next(gen) → gen.__next__()                           │
│   → 分配栈帧（PyFrameObject）                         │
│   → 执行到第一个 yield:                               │
│     gi_frame.f_lasti = yield 的指令偏移               │
│     gi_frame.f_locals = 当前局部变量                   │
│   → 冻结栈帧                                         │
│   → 返回 yield 的值                                   │
└──────────────────────────────────────────────────────┘

第二次调用 next(gen):
┌──────────────────────────────────────────────────────┐
│ next(gen) → gen.__next__()                           │
│   → 恢复栈帧（从 gi_frame 还原）                       │
│   → 从 f_lasti 继续执行                              │
│   → 执行到下一个 yield 或函数结束                      │
│   → 如果函数结束：                                    │
│     → 销毁栈帧                                       │
│     → 抛出 StopIteration                             │
└──────────────────────────────────────────────────────┘
```

### 2.2.5 基本用法

**以下是基本用法示例：**
```python
def count_up_to(n):
    i = 0
    while i < n:
        yield i
        i += 1

gen = count_up_to(3)
print(type(gen))  # <class 'generator'>

# 手动驱动
print(next(gen))  # 0
print(next(gen))  # 1
print(next(gen))  # 2
print(next(gen))  # StopIteration!

# for 循环自动处理 StopIteration
for num in count_up_to(5):
    print(num)  # 0 1 2 3 4
```

### 2.2.6 send()、throw()、close()——生成器双向通信

生成器不只是"发出数据"，还可以从外部接收数据——这是 Python 协程的基础。

#### 基本原理：生成器 = 可暂停/恢复的函数

```
普通函数：                生成器函数：
def f():                  def gen():
    x = 1                      x = 1
    y = 2                      y = yield  ← 挂起点
    return x + y               z = yield
                               return x + y + z

调用 f():                调用 gen():
  分配栈帧 → 执行 → 返回      分配栈帧 → 执行到 yield → 冻结栈帧
                               │
                               └── 栈帧保留在堆上（gi_frame != None）
                                  等待被 next() / send() 恢复

yield 做了三件事：
  ① 暂停当前执行，保存栈帧（局部变量、指令指针等）
  ② 将 yield 右边的值返回给调用者
  ③ 等待被再次唤醒（通过 next() 或 send()）

send(value) 恢复时：
  ① 恢复 gi_frame（从堆上拿回栈帧，还原局部变量）
  ② 将 value 赋给 yield 表达式（作为 yield 的返回值）
  ③ 继续执行直到下一个 yield 或函数结束
```

**所以 `gen.send(value)` 的完整执行流程：**

```
gen.send(value):
1. 检查生成器状态：
   - 未启动（尚未执行到 yield）→ 只能 send(None)，否则 TypeError
   - 已暂停（yeild 挂起中）    → 正常恢复
   - 已结束                    → 抛出 StopIteration
2. 恢复 gi_frame（从堆上取出冻结的栈帧）
3. 将 value 写入 yield 所在局部变量（yield 表达式的返回值 = value）
4. 继续执行字节码，直到下一个 yield 或 return
5. 遇到 yield → 再次冻结栈帧，返回 yield 的值
6. 遇到 return → 设置 GeneratorExit，抛出 StopIteration
```

#### 为什么第一次必须 `next(gen)` 或 `gen.send(None)`？

```
生成器刚创建时：
  gen = echo()
  │
  ├── gen.gi_frame = None     ← 还没分配栈帧
  ├── gen.gi_running = False
  └── gen.gi_yieldfrom = None
      这个生成器是"空的"，函数体一行都没执行！

调用 next(gen) 后：
  next(gen)  → 执行到第一个 yield 后暂停
  │
  ├── gen.gi_frame = <frame>  ← 有了！栈帧冻结在 yield 处
  ├── gen.gi_frame.f_lasti = 指令索引  ← 停在哪条字节码
  └── gen.gi_frame.f_locals = {}       ← 局部变量（如果有）

此时 yield 表达式等在那里，准备接收值。
现在才能 send("Hello")——yield 表达式的返回值 = "Hello"

所以结论：第一次必须 next(gen) 是为了"把生成器推到第一个 yield 处"
在那之前，没有 yield 等着接收值，send(value) 无处可放。
```

#### 预激（priming）模式——就是提前调一次 next

```python
# 如果不预激：
gen = running_average()
gen.send(10)  # TypeError: can't send non-None value to a just-started generator
               # 生成器还没跑到 yield，没法接收值！

# 预激：
gen = running_average()
next(gen)     # 跑到第一个 yield，现在可以接收了
gen.send(10)  # ✅ 正常工作
```

**`@coroutine` 装饰器的作用就是自动调这一次 `next()`：**

```python
def coroutine(func):
    """装饰器：自动预激生成器（执行到第一个 yield）"""
    from functools import wraps
    @wraps(func)
    def primer(*args, **kwargs):
        gen = func(*args, **kwargs)
        next(gen)  # ★ 就是这一句：把生成器推到 yield 处
        return gen
    return primer
```

**所以回到你的问题——"注解的作用就是为了提前调一次 next 吗？"**

**对，就是这一个目的。**

没有这个装饰器，每次创建生成器后都得手动写一行 `next(gen)`，容易忘。忘了就报 `TypeError`，这是很常见的 bug。装饰器把"预激"这个步骤自动化了，让使用者可以立即 `send()`。

```
使用装饰器前：                     使用装饰器后：
gen = running_average()            gen = running_average()
next(gen)         ← 手动，易忘    gen.send(10)    ← 直接用
gen.send(10)                       gen.send(20)
gen.send(20)
```

**`throw()`——从外部注入异常到生成器内部：**

```python
def injectable():
    i = 0
    while True:
        try:
            received = yield i
            i += 1
        except ValueError as e:
            print(f"注入异常: {e}，重置计数器")
            i = 0
        except GeneratorExit:
            print("被关闭了")
            break

gen = injectable()
next(gen)          # 预激
print(gen.send(1)) # 1
print(gen.send(2)) # 2
gen.throw(ValueError, "手动重置")  # 注入异常 → 输出 "注入异常: 手动重置，重置计数器"
# 然后执行回 while，遇到下一个 yield → 0
print(gen.send(1)) # 1  ← 重置后从 0 重新计数
gen.close()
# "被关闭了"

# throw 的内部流程：
# gen.throw(ExceptionType, value)
# 1. 恢复 gi_frame
# 2. 在 yield 表达式位置抛出指定异常（就像 yield 那行代码 raise 了一样）
# 3. 如果生成器内部捕获了 → 继续执行到下一个 yield，返回 yield 的值
# 4. 如果没捕获 → 异常传播到外部调用者
```

**`close()`——优雅关闭生成器：**

```python
def closable():
    try:
        while True:
            yield
    except GeneratorExit:
        print("清理资源：关闭文件、释放连接")
        raise  # 必须重新抛出，否则 RuntimeError

gen = closable()
next(gen)
gen.close()
# "清理资源：关闭文件、释放连接"

# close() 的内部流程：
# gen.close()
# 1. 在 yield 处注入 GeneratorExit 异常（和 throw 类似）
# 2. 如果生成器捕获后重新抛出 → 正常结束
# 3. 如果产生器产生下一个值 → RuntimeError（close 不应该 yield）
# 4. 如果生成器没捕获 → 自动传播，生成器结束
# 5. 生成器状态变为 GEN_CLOSED
```

#### 三种方法对比总结

```
方法        触发方式                效果                      必须预激？
────        ─────                  ────                      ────────
next(gen)   调用内置 next()         恢复执行到下一个 yield      N/A（它就是预激）
gen.send(v) 调用 send(value)       恢复 + 传值到 yield 表达式   ✅ 必须
gen.throw(e)在 yield 处抛异常       注入异常到生成器内部         ✅ 必须
gen.close() 注入 GeneratorExit      触发清理、关闭生成器         ✅ 建议

底层统一机制：
所有四种方法都在做同一件事——恢复 gi_frame，让冻结的栈帧继续跑字节码。
区别只在于：
  next()    → 传递 None
  send(v)   → 传递 value
  throw(e)  → 在恢复点抛异常
  close()   → 抛 GeneratorExit
```

#### 生成器状态机

```
           ┌──────────┐
           │ GEN_CREATED  │  ← 刚创建，还没执行任何代码
           └─────┬──────┘
                 │ next(gen) 或 gen.send(None)
                 ▼
           ┌──────────┐
           │ GEN_RUNNING │  ← 正在执行中（不可从外部操作）
           └─────┬──────┘
                 │ 遇到 yield 暂停
                 ▼
           ┌──────────┐
           │ GEN_SUSPENDED│  ← 冻结在 yield 处，等待恢复
           └─────┬──────┘
                 │ next / send / throw / close
                 ├──────────────────────────────
                 ▼
           ┌──────────┐
           │ GEN_CLOSED  │  ← 函数 return 或 close() 调用后
           └──────────┘
            不能再恢复，再调用 throw StopIteration

验证生成器状态（Python 3.5+）：
from inspect import getgeneratorstate

gen = echo()
print(getgeneratorstate(gen))  # GEN_CREATED
next(gen)
print(getgeneratorstate(gen))  # GEN_SUSPENDED
gen.close()
print(getgeneratorstate(gen))  # GEN_CLOSED
```

### 2.2.7 yield from——委托给子生成器

`yield from` 是 Python 3.3 引入的语法，它让一个生成器可以委托（delegate）给另一个生成器，自动处理子生成器的全部 `yield` 和 `send()` 转发。

```python
def sub_gen():
    yield 1
    yield 2
    return "done"  # return 值被 yield from 表达式的值接收

def main_gen():
    yield "start"
    result = yield from sub_gen()  # 委托
    yield result
    yield "end"

print(list(main_gen()))
# ['start', 1, 2, 'done', 'end']
```

**`yield from` 的内部机制（简化等价）：**

```python
# yield from sub_gen() 大致等价于：
for value in sub_gen():  # 但实际上更复杂
    yield value

# 实际等价代码（简化）：
_i = iter(sub_gen())  # 获取子生成器
try:
    _y = next(_i)     # 启动子生成器
except StopIteration as _e:
    _r = _e.value     # 捕获 return 值
else:
    while True:
        _s = yield _y  # 转发产量，接收外部 send
        try:
            _y = _i.send(_s)  # 转发 send 给子生成器
        except StopIteration as _e:
            _r = _e.value
            break
result = _r  # yield from 表达式的值 = 子生成器的 return
```

### 2.2.8 生成器 vs 列表的工程决策

**下面是 生成器 vs 列表的工程决策 的代码示例：**
```python
import sys

# 列表：一次性全部计算，占用大量内存
list_squares = [x**2 for x in range(1_000_000)]
print(sys.getsizeof(list_squares))  # ~8,000,056 bytes

# 生成器：惰性计算，几乎不占内存
gen_squares = (x**2 for x in range(1_000_000))
print(sys.getsizeof(gen_squares))   # ~112 bytes

# 输出相同，但生成器只能迭代一次
print(sum(list_squares))  # 332833500
print(sum(gen_squares))   # 332833500
```

| 场景 | 用列表 | 用生成器 |
|------|--------|----------|
| 需要随机访问 | `list[i]` | 不行，生成器无索引 |
| 需要多次迭代 | 可反复使用 | 只能一次（可再创建新生成器） |
| 数据量小 | 可读性好 | 没必要 |
| 数据量大/无限 | 内存溢出 | 必须用生成器 |
| 流式处理 | 阻塞 | 惰性，逐条处理 |
| 需要 `len()` | 可用 | 不行 |

### 2.2.9 常见错误

**错误 1：误以为生成器可以多次迭代**

```python
gen = (x**2 for x in range(3))
print(list(gen))  # [0, 1, 2]
print(list(gen))  # []  ← 生成器已耗尽！

# 解决：需要再次创建生成器
```

**错误 2：生成器函数中 return 带值的误解**

```python
def my_gen():
    yield 1
    yield 2
    return "done"  # return 值不会出现在 for 循环中

for x in my_gen():
    print(x)  # 只打印 1, 2，"done" 不会被打印

# 如果想获取 return 值，需要手动处理 StopIteration
gen = my_gen()
try:
    while True:
        print(next(gen))
except StopIteration as e:
    print(f"Return value: {e.value}")  # "done"
```

**错误 3：在生成器函数外使用 yield**

```python
def wrong():
    if True:
        print("test")
    yield 1  # 函数体内有 yield → 变成生成器函数

# 即使 yiled 永远不会执行到，函数也变成了生成器函数
```

**错误 4：在生成器内部混用 return 和 yield（见错误 2）**

**错误 5：`send()` 到未预激的生成器**

```python
gen = echo()
gen.send("Hello")  # TypeError: can't send non-None value to a just-started generator
# 必须先 next(gen) 或 gen.send(None)
```

### 2.2.10 AI 场景案例

**场景一：流式 LLM 响应（逐 token 产出，实时回显给用户）**

```python
import time
import requests

def stream_llm(prompt: str, api_key: str):
    """模拟 LLM 流式输出：每次 yield 一个 token"""
    response = requests.post(
        "https://api.openai.com/v1/chat/completions",
        headers={"Authorization": f"Bearer {api_key}"},
        json={
            "model": "gpt-4",
            "messages": [{"role": "user", "content": prompt}],
            "stream": True,  # 流式模式
        },
        stream=True
    )
    for line in response.iter_lines():
        if line:
            # yield 每个 token，前端可以逐步展示
            token = line.decode().strip()
            yield token

# 使用：前端 SSE 推送
def chat_handler(prompt: str):
    for token in stream_llm(prompt, "sk-xxx"):
        send_to_client(token)  # 逐字推送
```

**场景二：批量推理 + 结果收集**

```python
def batched(iterable, batch_size: int):
    """通用批处理器：将流式数据按 batch_size 分组"""
    batch = []
    for item in iterable:
        batch.append(item)
        if len(batch) == batch_size:
            yield batch
            batch = []
    if batch:
        yield batch

def process_embeddings(texts: list[str], embed_fn, batch_size=32):
    """分批处理嵌入向量，避免一次发送过多 token"""
    for batch in batched(texts, batch_size):
        embeddings = embed_fn(batch)  # 一次 API 调用处理一批
        yield from embeddings

# 使用
texts = open("corpus.txt").readlines()
for emb in process_embeddings(texts, openai.Embedding.create, batch_size=16):
    store_embedding(emb)
```

**场景三：无限数据增强流水线**

```python
import random

def augment_text(text: str):
    """单条数据增强"""
    words = text.split()
    if random.random() < 0.3:
        idx = random.randint(0, len(words))
        words.insert(idx, random.choice(["好的", "那么", "也就是说"]))
    if random.random() < 0.3:
        idx = random.randint(0, len(words)-1)
        words[idx] = words[idx].upper() if random.random() < 0.5 else words[idx].lower()
    return " ".join(words)

def infinite_augment(corpus: list[str], max_examples=None):
    """无限数据增强生成器：从不加载全部数据到内存"""
    count = 0
    while True:
        random.shuffle(corpus)
        for text in corpus:
            yield augment_text(text)
            count += 1
            if max_examples and count >= max_examples:
                return

# 使用：训练时 on-the-fly 增强
train_pipeline = infinite_augment(train_texts, max_examples=100000)
for augmented in train_pipeline:
    model.train(augmented)
```

---

## 2.3 迭代器

### 2.3.1 一句话核心本质

**迭代器是实现了 `__iter__` 和 `__next__` 协议的对象，`for` 循环的本质是重复调用 `iter()` 获取迭代器，然后 `next()` 取值直到 `StopIteration`。**

### 2.3.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 接口 | `Iterator<T>` 接口（`hasNext()`, `next()`） | 鸭子类型协议（`__iter__`, `__next__`） |
| 终止信号 | `hasNext()` 返回 false（需主动检查） | `__next__()` 抛出 `StopIteration`（异常即终止） |
| 可迭代对象 | `Iterable<T>` 返回 `Iterator<T>` | `__iter__()` 返回迭代器，或定义 `__getitem__()` |
| for-each 本质 | `for (T x : iterable)` 编译为 `iterator()` + `hasNext()` + `next()` | `for x in iterable` 编译为 `iter()` + `next()` + `except StopIteration` |
| 惰性 | Stream 是惰性链，终结操作触发 | 所有迭代器天生惰性，`next()` 一次计算一个 |

**本质区别：** Java 用 `hasNext()` 检查是否有下一个元素（普通方法返回 boolean），Python 用 `StopIteration` 异常信号终止（异常即流程控制）。Python 的方式更简洁——不需要检查边界，`for` 循环自动捕获异常。

### 2.3.3 技术原理：迭代器协议

```
Python 迭代器协议完整工作流：

任何对象 x 满足以下条件即可用于 for 循环：

┌─────────────────────────────────────────────────────┐
│  for x in obj:                                      │
│      process(x)                                     │
│                                                      │
│  等价于：                                            │
│  _iter = obj.__iter__()  ← 1. 获取迭代器             │
│  while True:                                         │
│      try:                                            │
│          x = _iter.__next__()  ← 2. 获取下一个元素   │
│          process(x)                                   │
│      except StopIteration:  ← 3. 捕获终止信号        │
│          break                                        │
└─────────────────────────────────────────────────────┘

可迭代对象 (Iterable) vs 迭代器 (Iterator)：

      __iter__()
      ──────────→
┌───────────────┐                              ┌─────────────────┐
│  Iterable     │                              │  Iterator       │
│  (可迭代对象)  │                              │  (迭代器)        │
│               │                              │                  │
│  __iter__()   │                              │  __iter__()      │
│  返回迭代器    │                              │  返回自身 (self) │
│               │                              │                  │
│  list, tuple, │                              │  __next__()      │
│  dict, set,   │                              │  返回下一个元素   │
│  str, file    │                              │  或 StopIteration│
│               │                              │                  │
│               │                              │  generator,      │
│               │             __next__()        │  zip, map,       │
│               │     元素 ────────             │  enumerate       │
│               │     (取出元素，不是可迭代对象)   │                  │
└───────────────┘                              └─────────────────┘

注：__next__() 返回的是集合中的单个元素（int/str/object），
    不是可迭代对象！旧版箭头方向容易误解，已修正。
```

**CPython 中 `for` 循环的实际执行（`ceval.c`）：**

```
FOR_ITER 字节码指令：
1. 调用 iter.__next__()
2. 如果成功 → 将值压栈，继续执行
3. 如果抛出 StopIteration → 跳转到循环结束
4. 同时递减引用计数，防止内存泄漏
```

### 2.3.4 手动实现迭代器

**方式一：经典双类模式（可迭代对象 + 迭代器）**

```python
class Range:
    """可迭代对象：实现 __iter__ 返回迭代器"""
    def __init__(self, start, end):
        self.start = start
        self.end = end

    def __iter__(self):
        return RangeIterator(self.start, self.end)

class RangeIterator:
    """迭代器：实现 __iter__ 和 __next__"""
    def __init__(self, start, end):
        self.current = start
        self.end = end

    def __iter__(self):
        return self  # 迭代器的 __iter__ 返回自身

    def __next__(self):
        if self.current >= self.end:
            raise StopIteration
        value = self.current
        self.current += 1
        return value

# 使用
for i in Range(0, 5):
    print(i)  # 0 1 2 3 4
```

**方式二：单类自迭代模式（迭代器 = 可迭代对象）**

```python
class CountDown:
    """自迭代类：__iter__ 返回 self"""
    def __init__(self, start):
        self.start = start

    def __iter__(self):
        self.n = self.start  # 重置状态
        return self

    def __next__(self):
        if self.n < 0:
            raise StopIteration
        value = self.n
        self.n -= 1
        return value

# 可以创建多个独立迭代器
c = CountDown(3)
print(list(c))  # [3, 2, 1, 0]
print(list(c))  # []  ← 已耗尽！因为 __iter__ 重置了状态
```

#### 双类 vs 单类：各自场景

```
特性              双类模式                           单类模式
────              ──────                             ──────
类数量            2 个（可迭代对象 + 迭代器）          1 个（自己是迭代器）
多次 for 循环     每次迭代创建新迭代器                必须手动重置状态
                  ✅ 每次都是全新的从头迭代            ⚠️ 依赖 __iter__ 重置

状态隔离          每个迭代器独立状态                  同一实例共享状态
                  ✅ 并行多个循环互不干扰              ❌ 嵌套循环会打架

内存              每次 for 创建新对象                 无额外对象
                  ⚠️ 多一次分配                       ✅ 零开销

使用难度          稍复杂（两个类）                    简单直观

典型场景          标准库全部采用此模式                简单的一次性迭代
                  容器类（list, dict, set）           生成器函数
                  任何需要多次独立遍历的对象            自定义迭代器快速实现
```

**双类不总是更好的——看具体需求：**

```python
# ──── 场景 1：需要多次独立遍历 → 双类模式 ✅ ────
class Playlist:
    """歌曲列表：多次遍历各不影响"""
    def __init__(self, songs):
        self.songs = songs

    def __iter__(self):
        return PlaylistIterator(self.songs)

class PlaylistIterator:
    def __init__(self, songs):
        self.songs = songs
        self.index = 0

    def __iter__(self):
        return self

    def __next__(self):
        if self.index >= len(self.songs):
            raise StopIteration
        song = self.songs[self.index]
        self.index += 1
        return song

pl = Playlist(["A", "B", "C"])

# 两个 for 循环独立运行，互不干扰
for s in pl:           # 第一次：创建 Iterator #1
    print(s)           # A, B, C
for s in pl:           # 第二次：创建 Iterator #2
    print(s)           # A, B, C  ← 能再次从头遍历

# ──── 场景 2：只需遍历一次 → 单类模式 ✅ ────
class LogReader:
    """逐行读取日志文件：一次性的流式读取"""
    def __init__(self, path: str):
        self.path = path

    def __iter__(self):
        self.f = open(self.path, "r")
        return self

    def __next__(self):
        line = self.f.readline()
        if not line:
            self.f.close()
            raise StopIteration
        return line.rstrip("\n")

    def __del__(self):
        if hasattr(self, "f") and not self.f.closed:
            self.f.close()

# 日志文件通常只遍历一次，没必要双类

# ──── 场景 3：单类模式嵌套循环的问题 ────
class BadSingle:
    def __init__(self, data):
        self.data = data

    def __iter__(self):
        self.i = 0
        return self

    def __next__(self):
        if self.i >= len(self.data):
            raise StopIteration
        val = self.data[self.i]
        self.i += 1
        return val

b = BadSingle([1, 2, 3])
# 嵌套循环：
for x in b:      # 外部循环
    for y in b:  # 内部循环 —— 共享同一个 self.i！
        print(x, y)
# 实际输出：
# 1, 1
# 1, 2
# 1, 3
# 外部循环只执行了一次！因为内层循环把 self.i 耗尽了

# 要修复就得在 __iter__ 中创建新迭代器 → 退化成双类模式

# ──── 场景 4：生成器 = 天然的单类迭代器 ────
def fibonacci(n):
    a, b = 0, 1
    for _ in range(n):
        yield a
        a, b = b, a + b

# 生成器函数每次调用都创建新生成器对象
# 所以它看起来是"单类"，但每次 for 都调一次 fibonacci(n)
# → 每次都是新对象，天然隔离
for x in fibonacci(5): print(x)  # 0 1 1 2 3
for x in fibonacci(5): print(x)  # 0 1 1 2 3  ← 又能从头
```

**标准库的选择（权威参考）：**

```python
# 双类模式：
list      → list.__iter__()  → list_iterator
dict      → dict.__iter__()  → dict_keyiterator
str       → str.__iter__()   → str_iterator
tuple     → tuple.__iter__() → tuple_iterator
set       → set.__iter__()   → set_iterator
range     → range.__iter__() → range_iterator

# 单类模式或生成器：
open()    → file object 自身就是迭代器
zip()     → zip object 自身就是迭代器
map()     → map object 自身就是迭代器
enumerate → enumerate 自身就是迭代器
reversed  → reversed 自身就是迭代器

# 规律：
# 容器类（存数据的）→ 双类模式 ✅ 保证多次遍历
# 流/视图类（算数据的）→ 单类模式 ✅ 一次性使用
```

**结论：**

```
使用双类的场景：                          使用单类的场景：
• 容器/集合类（底层有数据存储）             • 流式数据（文件、网络、传感器）
• 需要多次 for 循环遍历                     • 只遍历一次就丢弃
• 需要并行/嵌套循环                         • 计算型迭代器（每次重新计算）
• 需要独立迭代状态                         • 生成器函数和表达式

双类是"正确"的做法，但不是"总更好"的做法。
单类在以下情况下更合适：
1. 数据是一次性的（文件流、网络包）
2. 每次迭代都重新计算（斐波那契、素数生成）
3. 代码极简优先（生成器一行搞定）
4. 迭代器本身很轻量（创建新对象的开销 > 状态重置的开销）
```

**方式三：利用 `__getitem__` 实现迭代（Python 的备选协议）**

```python
class Fibonacci:
    """只实现 __getitem__ 也可迭代——Python 会回退到此协议"""
    def __getitem__(self, i):
        if i < 0:
            raise IndexError("negative index")
        a, b = 0, 1
        for _ in range(i):
            a, b = b, a + b
        return a

f = Fibonacci()
for i, val in enumerate(f):
    if i > 10:
        break
    print(val)  # 0 1 1 2 3 5 8 13 21 34 55
```

**验证迭代器协议：**

```python
from collections.abc import Iterator, Iterable

class MyIter:
    def __iter__(self):
        return self
    def __next__(self):
        raise StopIteration

print(isinstance(MyIter(), Iterable))  # True
print(isinstance(MyIter(), Iterator))  # True

class MyIterable:
    def __iter__(self):
        return MyIter()

print(isinstance(MyIterable(), Iterable))  # True
print(isinstance(MyIterable(), Iterator))  # False
```

### 2.3.5 for 循环本质深度解析

**下面是 for 循环本质深度解析 的代码示例：**
```python
# Python 中 for x in iterable 的完整等价展开：

def for_loop(iterable, func):
    # 1. 获取迭代器
    iterator = iter(iterable)
    # 等价于:
    # if hasattr(iterable, '__iter__'):
    #     iterator = iterable.__iter__()
    # elif hasattr(iterable, '__getitem__'):
    #     iterator = SequenceIterator(iterable)
    # else:
    #     raise TypeError("not iterable")

    # 2. 循环取值
    while True:
        try:
            x = next(iterator)
            # 等价于: x = iterator.__next__()
        except StopIteration:
            break  # 正常终止
        else:
            func(x)  # 循环体

# 测试
for_loop([1, 2, 3], print)
# 1 2 3
```

### 2.3.6 itertools 标准库详解

#### itertools 是什么？

`itertools` 是 **Python 标准库**（stdlib）自带的模块——"标准库"就是安装 Python 时自带的模块，不需要 `pip install`，`import itertools` 直接用。它提供了一组 **生成器函数**，专门用于构建高效的迭代器链，不占额外内存。

```
为什么叫 itertools？
  iter   = iterator（迭代器）
  tools  = 工具集
  → "迭代器的工具集"——用来创建和操作迭代器的函数集合
```

#### 三大分类

```
itertools 函数           无限迭代器         有限终止迭代器         组合生成器
                        ────────          ──────────            ────────
                        count()           chain()               product()
                        cycle()           compress()            permutations()
                        repeat()          dropwhile()           combinations()
                                           takewhile()           combinations_with_replacement()
                                           filterfalse()
                                           groupby()
                                           islice()
                                           starmap()
                                           accumulate()
                                           zip_longest()
                                           pairwise()
                                           batched()  (3.12+)
                                           tee()
```

---

#### 无限迭代器（Infinite Iterators）

**`count(start=0, step=1)` —— 无限等差数列**

```python
from itertools import count

# 基本用法：0, 1, 2, 3, ...
for i in count():
    if i > 3: break
    print(i)  # 0 1 2 3

# 指定起点和步长
for i in count(10, 2):
    if i > 16: break
    print(i)  # 10 12 14 16

# AI 场景：自动生成序列 ID
def generate_batch_id():
    for i in count(1):
        yield f"batch_{i:04d}"

gen = generate_batch_id()
print(next(gen))  # batch_0001
print(next(gen))  # batch_0002
```

**`cycle(iterable)` —— 无限循环重复**

```python
from itertools import cycle

# 基本用法：A, B, C, A, B, C, ...
limited = 0
for item in cycle("ABC"):
    if limited >= 5: break
    print(item, end=" ")  # A B C A B
    limited += 1

# AI 场景：轮询 API key
api_keys = ["key1", "key2", "key3"]
key_pool = cycle(api_keys)

def get_next_key():
    return next(key_pool)

for _ in range(5):
    print(get_next_key(), end=" ")  # key1 key2 key3 key1 key2

# AI 场景：交替使用不同模型做负载均衡
models = cycle(["gpt-4", "claude-3", "gemini-pro"])
tasks = ["翻译", "总结", "分类", "翻译", "总结"]
for task in tasks:
    model = next(models)
    print(f"{task} → {model}")
    # 翻译 → gpt-4
    # 总结 → claude-3
    # 分类 → gemini-pro
    # 翻译 → gpt-4
    # 总结 → claude-3
```

**`repeat(object, times=None)` —— 重复同一个值**

```python
from itertools import repeat

# 重复 3 次
print(list(repeat("hello", 3)))  # ['hello', 'hello', 'hello']

# times=None → 无限重复（极少用，配合 map/zip 才实用）

# AI 场景：给 batch 数据打固定标签
data = ["文本1", "文本2", "文本3"]
labeled = list(zip(data, repeat("训练集")))
print(labeled)  # [('文本1', '训练集'), ('文本2', '训练集'), ('文本3', '训练集')]
```

---

#### 有限终止迭代器（Terminating Iterators）

**`chain(*iterables)` —— 串联多个可迭代对象**

```python
from itertools import chain

# 串联列表
print(list(chain([1, 2], [3, 4], [5])))    # [1, 2, 3, 4, 5]

# 串联不同类型的可迭代对象
print(list(chain("abc", [1, 2], (3, 4))))  # ['a', 'b', 'c', 1, 2, 3, 4]

# 展平嵌套列表
nested = [[1, 2], [3, 4], [5]]
flattened = list(chain.from_iterable(nested))  # chain.from_iterable 专门展平
print(flattened)  # [1, 2, 3, 4, 5]

# 等价于：
flattened = [x for sub in nested for x in sub]

# AI 场景：合并多轮对话的 token
messages = [
    ["用户", "你好"],
    ["助手", "你好！有什么可以帮你"],
    ["用户", "帮我翻译"],
]
all_tokens = list(chain.from_iterable(messages))
print(all_tokens)  # ['用户', '你好', '助手', '你好！有什么可以帮你', '用户', '帮我翻译']
```

**`compress(data, selectors)` —— 按选择器过滤**

```python
from itertools import compress

data = ["a", "b", "c", "d", "e"]
selector = [1, 0, 1, 1, 0]
print(list(compress(data, selector)))  # ['a', 'c', 'd']

# AI 场景：根据置信度过滤预测结果
predictions = ["猫", "狗", "猫", "鸟", "狗"]
confidence = [0.95, 0.3, 0.88, 0.92, 0.2]
high_conf = [c > 0.5 for c in confidence]
print(list(compress(predictions, high_conf)))  # ['猫', '猫', '鸟']
```

**`dropwhile(predicate, iterable)` —— 跳过开头满足条件的元素**

```python
from itertools import dropwhile

# 跳过小于 5 的元素
print(list(dropwhile(lambda x: x < 5, [1, 4, 6, 3, 7])))
# [6, 3, 7]  ← 遇到第一个不满足条件的 6 之后，后面的 3 也保留！

# ❌ 不是过滤！dropwhile 只在开头生效，一旦条件不成立，后面全收
# ✅ filter(lambda x: x >= 5, ...) → [6, 7] 才是过滤

# AI 场景：跳过日志文件的 header 行
log_lines = [
    "# 日志文件",
    "# 生成时间: 2026-01-01",
    "# =============",
    "INFO: 开始训练",
    "INFO: epoch 1/10",
]
for line in dropwhile(lambda x: x.startswith("#"), log_lines):
    print(line)
# INFO: 开始训练
# INFO: epoch 1/10
```

**`takewhile(predicate, iterable)` —— 取出开头满足条件的元素**

```python
from itertools import takewhile

# 取出小于 5 的元素（遇到第一个不满足的就停）
print(list(takewhile(lambda x: x < 5, [1, 4, 6, 3, 7])))
# [1, 4]  ← 遇到 6 不满足条件，立即停止，后面的 3、7 不要

# AI 场景：读取配置文件中连续的 key=value
config = [
    "model=gpt-4",
    "temperature=0.7",
    "",
    "# 以下是推理配置",
    "max_tokens=2048",
]
settings = list(takewhile(lambda x: "=" in x, config))
print(settings)  # ['model=gpt-4', 'temperature=0.7']
```

**`filterfalse(predicate, iterable)` —— 反过滤（保留条件为 False 的）**

```python
from itertools import filterfalse

# 等价于 filter(lambda x: not pred(x), iterable)
print(list(filterfalse(lambda x: x % 2 == 0, [1, 2, 3, 4, 5])))
# [1, 3, 5]  ← 保留奇数（偶数的条件为 True，被反过滤掉）

# AI 场景：过滤掉空字符串
texts = ["hello", "", "world", "", "!"]
print(list(filterfalse(lambda x: not x, texts)))
# ['hello', 'world', '!']
```

**`groupby(iterable, key=None)` —— 相邻元素分组**

```python
from itertools import groupby

# ⚠️ groupby 只对相邻的相同 key 生效！必须先排序！
data = [("A", 1), ("A", 2), ("B", 3), ("B", 4)]
for key, group in groupby(data, key=lambda x: x[0]):
    print(f"{key}: {list(group)}")
# A: [('A', 1), ('A', 2)]
# B: [('B', 3), ('B', 4)]

# 如果不排序：
data2 = [("B", 1), ("A", 2), ("A", 3), ("B", 4)]
for key, group in groupby(data2, key=lambda x: x[0]):
    print(f"{key}: {list(group)}")
# B: [('B', 1)]
# A: [('A', 2), ('A', 3)]    ← B 在后面又出现，但已经是新一组了
# B: [('B', 4)]               ← 没有聚合到上面的 B

# 正确用法：先排序，再分组
sorted_data = sorted(data2, key=lambda x: x[0])
for key, group in groupby(sorted_data, key=lambda x: x[0]):
    print(f"{key}: {list(group)}")
# A: [('A', 2), ('A', 3)]
# B: [('B', 1), ('B', 4)]

# AI 场景：按日期分组日志
logs = [
    ("2026-01-01", "10:00", "启动"),
    ("2026-01-01", "11:00", "训练"),
    ("2026-01-02", "09:00", "启动"),
]
for day, entries in groupby(logs, key=lambda x: x[0]):
    times = [e[1] for e in entries]
    print(f"{day}: {times}")
# 2026-01-01: ['10:00', '11:00']
# 2026-01-02: ['09:00']
```

**`islice(iterable, stop)` / `islice(iterable, start, stop, step=1)` —— 惰性切片**

```python
from itertools import islice

# 取前 5 个
print(list(islice(range(1_000_000_000), 5)))  # [0, 1, 2, 3, 4]

# 切片 [2:5]
print(list(islice(range(10), 2, 5)))  # [2, 3, 4]

# 步长
print(list(islice(range(10), 0, 10, 3)))  # [0, 3, 6, 9]

# ⚠️ islice 是惰性的！range(1_000_000_000) 根本不占内存
# ✅ 普通切片 range(1_000_000_000)[:5] 会先创建巨量列表 → 内存爆炸

# AI 场景：从大文件中读取前 N 行
def read_lines(path: str):
    with open(path, "r") as f:
        for line in f:
            yield line.rstrip("\n")

first_10 = list(islice(read_lines("huge.log"), 10))

# AI 场景：数据集抽样——每隔 100 条取 1 条
sampled = list(islice(dataset, 0, None, 100))  # [0, 100, 200, ...]
```

**`starmap(function, iterable)` —— 解包映射**

```python
from itertools import starmap

# starmap 把每个元素解包后传给函数
# 等价于：[func(*item) for item in iterable]

def power(base, exp):
    return base ** exp

pairs = [(2, 3), (3, 2), (4, 2)]
print(list(starmap(power, pairs)))  # [8, 9, 16]

# 对比普通 map：
# map(power, bases, exps)          → 需要两个可迭代对象
# starmap(power, pairs)            → 从元组列表解包

# AI 场景：批量调用 API（参数已打包）
api_calls = [
    ("gpt-4", "翻译这段文本", {"temperature": 0.3}),
    ("claude-3", "总结这篇文章", {"max_tokens": 100}),
]

def call_api(model, prompt, **kwargs):
    return f"{model} 处理: {prompt[:10]}..."

results = list(starmap(call_api, api_calls))
```

**`accumulate(iterable, func=operator.add)` —— 累积计算**

```python
from itertools import accumulate
import operator

# 默认累加
print(list(accumulate([1, 2, 3, 4])))  # [1, 3, 6, 10]

# 累乘
print(list(accumulate([1, 2, 3, 4], operator.mul)))  # [1, 2, 6, 24]

# 最大值（记录历史最大值）
print(list(accumulate([3, 1, 4, 1, 5], max)))  # [3, 3, 4, 4, 5]

# AI 场景：计算模型训练的累积损失
losses = [2.5, 1.8, 1.2, 0.9, 0.7]
cumulative_avg = []
for i, total in enumerate(accumulate(losses), 1):
    cumulative_avg.append(round(total / i, 2))
print(cumulative_avg)  # [2.5, 2.15, 1.83, 1.6, 1.42]

# AI 场景：生成 mini-batch 的累积梯度
gradients = [0.1, -0.05, 0.2, -0.1]
acc_grad = list(accumulate(gradients))
print(acc_grad)  # [0.1, 0.05, 0.25, 0.15]
```

**`zip_longest(*iterables, fillvalue=None)` —— 长 zip（补齐短的）**

```python
from itertools import zip_longest

# zip 以最短为准
print(list(zip([1, 2, 3], "ab")))       # [(1, 'a'), (2, 'b')]

# zip_longest 以最长为准，短的填充 fillvalue
print(list(zip_longest([1, 2, 3], "ab", fillvalue="?")))
# [(1, 'a'), (2, 'b'), (3, '?')]

# AI 场景：对齐不同长度的序列标签
tokens = ["我", "爱", "Python", "编程"]
labels = ["O", "O"]   # 短了
aligned = list(zip_longest(tokens, labels, fillvalue="O"))
print(aligned)  # [('我', 'O'), ('爱', 'O'), ('Python', 'O'), ('编程', 'O')]
```

**`pairwise(iterable)` —— 相邻元素配对（Python 3.10+）**

```python
from itertools import pairwise

print(list(pairwise([1, 2, 3, 4])))  # [(1, 2), (2, 3), (3, 4)]

# 等价于 zip(s, s[1:]) 但不创建新列表

# AI 场景：计算时间序列的变化量
prices = [100, 102, 98, 105, 103]
changes = [b - a for a, b in pairwise(prices)]
print(changes)  # [2, -4, 7, -2]

# AI 场景：文本中的 n-gram 特征
text = "自然语言处理"
chars = list(text)
bigrams = [a + b for a, b in pairwise(chars)]
print(bigrams)  # ['自然', '然语', '语言', '言处', '处理']
```

**`batched(iterable, n)` —— 批量分组（Python 3.12+）**

```python
from itertools import batched

print(list(batched([1, 2, 3, 4, 5, 6, 7], 3)))
# [(1, 2, 3), (4, 5, 6), (7,)]

# AI 场景：将数据集分批送入模型
rows = range(100)  # 100 条数据
for batch in batched(rows, 32):  # 每批 32 条
    process_batch(batch)         # 最后一批可能不足 32
```

**`tee(iterable, n=2)` —— 复制迭代器为 n 个**

```python
from itertools import tee

# 把一个迭代器变成 n 个独立的副本
original = [1, 2, 3, 4, 5]
it1, it2 = tee(original, 2)

print(list(it1))  # [1, 2, 3, 4, 5]
print(list(it2))  # [1, 2, 3, 4, 5]  ← 独立副本，互不影响

# ⚠️ tee 内部用队列缓存未被消费的元素
# 如果 it1 消费了 3 个但 it2 才消费 1 个，中间 2 个元素缓存在内存
# 所以：如果两个迭代器消费速度不同，可能占用大量内存

# AI 场景：同一份数据同时做两件事
data = range(10)

# 同时计算平均值和最大值
it_avg, it_max = tee(data, 2)

avg = sum(it_avg) / 10
mx = max(it_max)
print(avg, mx)  # 4.5 9

# 等价但更慢的做法：list() 两次（但 tee 避免了全量拷贝）
```

#### 组合生成器（Combinatoric Iterators）

**`product(*iterables, repeat=1)` —— 笛卡尔积**

```python
from itertools import product

# 两个集合的笛卡尔积
print(list(product([1, 2], ["a", "b"])))
# [(1, 'a'), (1, 'b'), (2, 'a'), (2, 'b')]

# repeat 参数：自身多次乘积
print(list(product([0, 1], repeat=2)))
# [(0, 0), (0, 1), (1, 0), (1, 1)]  ← 等价于 product([0,1], [0,1])

print(list(product([0, 1], repeat=3)))
# 8 个组合 —— 等价于二进制 000～111

# AI 场景：超参数网格搜索
param_grid = {
    "lr": [1e-3, 1e-4],
    "batch_size": [32, 64],
    "dropout": [0.1, 0.3],
}
keys, values = zip(*param_grid.items())
for combo in product(*values):
    config = dict(zip(keys, combo))
    print(config)
# {'lr': 0.001, 'batch_size': 32, 'dropout': 0.1}
# {'lr': 0.001, 'batch_size': 32, 'dropout': 0.3}
# ...总共 2×2×2 = 8 种组合
```

**`permutations(iterable, r=None)` —— 排列**

```python
from itertools import permutations

# 所有排列（r=len 时即全排列）
print(list(permutations("ABC", 2)))
# [('A', 'B'), ('A', 'C'), ('B', 'A'), ('B', 'C'), ('C', 'A'), ('C', 'B')]
# 顺序有关：('A', 'B') ≠ ('B', 'A')

# AI 场景：计算序列的所有可能顺序
items = ["a", "b", "c"]
for perm in permutations(items, 2):
    print(perm)
# ('a', 'b'), ('a', 'c'), ('b', 'a'), ('b', 'c'), ('c', 'a'), ('c', 'b')
```

**`combinations(iterable, r)` —— 组合**

```python
from itertools import combinations

# 所有 2-组合
print(list(combinations("ABC", 2)))
# [('A', 'B'), ('A', 'C'), ('B', 'C')]
# 顺序无关：('A', 'B') 和 ('B', 'A') 是同一个，只出现一次

# AI 场景：特征组合
features = ["年龄", "收入", "教育"]
pairs = list(combinations(features, 2))
print(pairs)  # [('年龄', '收入'), ('年龄', '教育'), ('收入', '教育')]
```

**`combinations_with_replacement(iterable, r)` —— 可重复组合**

```python
from itertools import combinations_with_replacement

# 允许同一个元素重复选取
print(list(combinations_with_replacement("ABC", 2)))
# [('A', 'A'), ('A', 'B'), ('A', 'C'), ('B', 'B'), ('B', 'C'), ('C', 'C')]
# 对比 combinations: [('A', 'B'), ('A', 'C'), ('B', 'C')] 少了 ('A','A'), ('B','B'), ('C','C')
```

---

#### AI 工程中 itertools 的使用模式

```python
from itertools import *

# 模式 1：无限循环 + 惰性切片 = 可控无限流
def generate_data():
    for i in count():
        yield {"id": i, "value": f"data_{i}"}

for item in islice(generate_data(), 100):
    process(item)

# 模式 2：串联多个数据源
train_data = chain(load_jsonl("part1.jsonl"), load_jsonl("part2.jsonl"))

# 模式 3：分组聚合
def analyze_logs(logs):
    sorted_logs = sorted(logs, key=lambda x: x["level"])
    for level, group in groupby(sorted_logs, key=lambda x: x["level"]):
        count = len(list(group))
        yield {"level": level, "count": count}

# 模式 4：分批处理流式数据
def batched_stream(stream, batch_size=32):
    """通用的流式分批工具"""
    it = iter(stream)
    while True:
        batch = list(islice(it, batch_size))
        if not batch:
            break
        yield batch

# 模式 5：tee 实现多路复用
def eval_model(model, data):
    data_tee = tee(data, 3)
    preds = (model.predict(x) for x in data_tee[0])
    truths = (x["label"] for x in data_tee[1])
    features = (x["feature"] for x in data_tee[2])
    return zip(preds, truths, features)
```

```
一句话总结：
itertools 就是 Python 自带的"迭代器瑞士军刀"
——用生成器组合替代 for 循环，省内存、更声明式、更高效。
```

### 2.3.7 常见错误

**错误 1：迭代器只能使用一次**

```python
nums = [1, 2, 3]
it = iter(nums)
print(list(it))  # [1, 2, 3]
print(list(it))  # []  ← 已耗尽！

# 修复：重新创建迭代器
```

**错误 2：在迭代时修改列表**

```python
lst = [1, 2, 3, 4, 5]
for i, x in enumerate(lst):
    if x % 2 == 0:
        lst.pop(i)  # RuntimeError: list changed during iteration

# 修复：迭代副本
for x in lst[:]:  # lst[:] 创建副本
    if x % 2 == 0:
        lst.remove(x)
```

**错误 3：忘记在 `__next__` 中 `raise StopIteration`**

```python
class BadIterator:
    def __next__(self):
        return 42  # 永远不终止 → 无限循环

for x in BadIterator():  # 永远不停止！
    print(x)
```

**错误 4：迭代器和可迭代对象混淆**

```python
class BadRange:
    def __init__(self, n):
        self.n = n
        self.i = 0

    def __iter__(self):
        return self

    def __next__(self):
        if self.i < self.n:
            val = self.i
            self.i += 1
            return val
        raise StopIteration

r = BadRange(3)
print(list(r))  # [0, 1, 2]
print(list(r))  # []  ← 第二次就空了！

# 正确做法：__iter__ 应该重置状态或返回新迭代器
```

### 2.3.8 AI 场景案例

**场景一：从 LLM 响应中流式提取 JSON 片段**

```python
import json

class JsonStreamExtractor:
    """迭代器：从流式 LLM 输出中提取完整 JSON 对象"""
    def __init__(self, token_stream):
        self.tokens = token_stream
        self.buffer = ""
        self.depth = 0
        self.in_string = False

    def __iter__(self):
        return self

    def __next__(self):
        while True:
            token = next(self.tokens)  # 可能 StopIteration
            self.buffer += token
            result = self._try_extract()
            if result is not None:
                return result

    def _try_extract(self):
        """尝试从 buffer 中提取完整 JSON"""
        depth = 0
        in_str = False
        start = -1
        for i, c in enumerate(self.buffer):
            if c == '"' and (i == 0 or self.buffer[i-1] != '\\'):
                in_str = not in_str
            if in_str:
                continue
            if c == '{':
                if depth == 0:
                    start = i
                depth += 1
            elif c == '}':
                depth -= 1
                if depth == 0 and start >= 0:
                    obj = json.loads(self.buffer[start:i+1])
                    self.buffer = self.buffer[i+1:]
                    return obj
        return None

# 使用
stream = stream_llm("写一个JSON配置...")
for json_obj in JsonStreamExtractor(stream):
    print(f"解析到配置: {json_obj}")
```

**场景二：数据集 Pipeline（迭代器链）**

```python
import random

class DataPipeline:
    """可组合的数据处理流水线——每个阶段都是一个迭代器"""
    @staticmethod
    def read(path):
        with open(path) as f:
            for line in f:
                yield line.strip()

    @staticmethod
    def tokenize(texts, max_length=512):
        for text in texts:
            tokens = text.split()
            yield tokens[:max_length]

    @staticmethod
    def shuffle(iterable, buffer_size=1000):
        """伪混洗：维护一个 buffer 随机输出"""
        buffer = []
        for item in iterable:
            buffer.append(item)
            if len(buffer) >= buffer_size:
                idx = random.randrange(len(buffer))
                yield buffer.pop(idx)
        random.shuffle(buffer)
        yield from buffer

    @staticmethod
    def batched(iterable, batch_size=32):
        batch = []
        for item in iterable:
            batch.append(item)
            if len(batch) == batch_size:
                yield batch
                batch = []
        if batch:
            yield batch

# 使用迭代器链构建流水线
pipeline = DataPipeline.batched(
    DataPipeline.shuffle(
        DataPipeline.tokenize(
            DataPipeline.read("corpus.txt")
        )
    ),
    batch_size=32
)

for batch in pipeline:
    model.train(batch)
```

**场景三：自定义 `enumerate` 变体——为 AI 训练提供带权重的迭代**

```python
class WeightedIterator:
    """带权重采样的迭代器：重要样本被抽中概率更高"""
    def __init__(self, items, weights):
        assert len(items) == len(weights)
        self.items = items
        self.weights = weights
        self.total = sum(weights)

    def __iter__(self):
        return self

    def __next__(self):
        r = random.uniform(0, self.total)
        cumsum = 0
        for i, w in enumerate(self.weights):
            cumsum += w
            if r <= cumsum:
                return self.items[i]
        return self.items[-1]

# 使用：高错误样本更高概率被抽到
losses = compute_losses(dataset)
weights = [l / sum(losses) for l in losses]
sampler = WeightedIterator(dataset, weights)
for sample in sampler:
    model.train(sample)
```

---

## 2.4 魔术方法

### 2.4.1 一句话核心本质

**魔术方法是 Python 对象模型的核心钩子——以双下划线命名的特殊方法，让自定义对象可以重载运算符、模拟容器、控制属性访问、参与序列化等一切 Python 语言级行为。**

### 2.4.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 运算符重载 | 不支持（Java 不支持运算符重载） | 全面支持：`__add__`, `__sub__`, `__mul__` 等 |
| 容器模拟 | 实现 `List<T>`, `Map<K,V>` 等接口 | `__getitem__`, `__setitem__`, `__len__`, `__iter__` |
| 可调用对象 | 函数式接口 + lambda | `__call__` 让任意对象可调用 |
| 字符串表示 | `toString()` | `__str__`（用户）、`__repr__`（开发者） |
| 属性访问 | 编译期确定，`getter/setter` 需显式定义 | `__getattr__`, `__setattr__`, `__getattribute__`, `@property` |
| 上下文管理 | try-with-resources + `AutoCloseable` | `__enter__` / `__exit__`，语言级 with 语句 |
| 迭代 | `Iterator<T>` + `Iterable<T>` | `__iter__` / `__next__` 鸭子类型 |
| 哈希 | `hashCode()` | `__hash__` |
| 比较 | `equals()`, `compareTo()` | `__eq__`, `__lt__`, `__le__`, `__gt__`, `__ge__` + `functools.total_ordering` |

### 2.4.3 技术原理：魔术方法的底层分发机制

```
Python 的运算符/语法 → CPython 底层查找链：

a + b  →  type(a).__add__(a, b)               # a.__add__(b)
a[b]   →  type(a).__getitem__(a, b)            # a.__getitem__(b)
a.b    →  type(a).__getattribute__(a, "b")     # 默认行为
a()    →  type(a).__call__(a)                  # 调用对象
len(a) →  type(a).__len__(a)                   # 而不是 a.__len__()
str(a) →  type(a).__str__(a)
repr(a) → type(a).__repr__(a)
with a as x: → type(a).__enter__(a), type(a).__exit__(a, ...)

关键规则：魔术方法在 类型（类）上查找，而不是实例上！
type(obj).__method__(obj)  ← 正确
obj.__method__()           ← 错误！会绕过类型查找

原因：为了避免在实例的 __dict__ 中搜索魔术方法，
CPython 直接在类型的 tp_* 槽位中查找，速度快 10x。
```

#### 你的问题："都在类型上查，那重载魔术方法还有意义吗？"

**有意义——但不是在单个实例上重载，而是在类定义中重载。**

```python
# ─── 你理解的"重载"（对单个实例）—— 对魔术方法无效！───
class A:
    def __len__(self):
        return 10

obj = A()
obj.__len__ = lambda: 999  # 在实例上定义 __len__

print(obj.__len__())       # 999  ← 直接属性访问，走实例 __dict__
print(len(obj))            # 10   ← len() 走 type(obj).__len__！无视实例上的定义！

# 对比：普通方法可以在实例上重载
obj.自定义方法 = lambda: 999
print(obj.自定义方法())    # 999  ← 普通方法可以！因为普通方法走实例 __dict__ 查找
```

**为什么这么设计？**

```python
# 场景：如果魔术方法也走实例查找
class Vector:
    def __add__(self, other):
        return Vector(self.x + other.x, self.y + other.y)

v1 = Vector(1, 2)
v2 = Vector(3, 4)

# 如果不小心在某处设置了实例属性：
v1.__add__ = "not a function"  # 意外赋值

# 如果 __add__ 走实例查找：
# v1 + v2 → v1.__add__  → "not a function" → 崩溃！
# 但是如果走类型查找：
# v1 + v2 → type(v1).__add__ → Vector.__add__ → 正常工作 ✅
```

**所以规则很清晰：**

```
普通方法（走实例 __dict__ → 类型 tp_dict）：
  obj.foo()
  → 先查 obj.__dict__ 有没有 "foo"
  → 再查 type(obj).__dict__ 有没有 "foo"
  → ✅ 可以在单个实例上重载

魔术方法（直接走类型 tp_* 槽位）：
  len(obj), obj + other, obj[key]
  → 直接从 type(obj) 的 tp_* 槽位取
  → ❌ 在单个实例上定义无效
  → ✅ 在类定义中定义有效——这就是重载的意义！
```

**那什么时候魔术方法会在实例上搜索？——极少特殊情况：**

```python
# 例外 1：__class__（实例可以指向另一个类）
class FakeList:
    def __len__(self):
        return 3

class RealList:
    def __len__(self):
        return 999

obj = RealList()
print(len(obj))        # 999
obj.__class__ = FakeList
print(len(obj))        # 3  ← __class__ 被改了，type() 变了！

# 例外 2：__dict__ 本身
class WithDict:
    pass
obj = WithDict()
print(obj.__dict__)    # {}  ← 这是实例属性，不是类型属性

# 例外 3：显式调用 obj.__len__()（走普通属性查找）
obj.__len__ = lambda: 42
print(obj.__len__())   # 42  ← 直接调用，走实例属性查找
# 但 len(obj) 仍然走 Type 查找，和刚才的不一样！
print(len(obj))        # 看类有没有定义 __len__

# 例外 4：Python 3.12+ —— __class_getitem__ 在类上查找
# list[int] → type(list).__class_getitem__(int)

# 例外 5：__init__ 和 __new__ 特殊处理
class Meta(type):
    def __call__(cls, *args, **kwargs):
        print("拦截创建")
        return super().__call__(*args, **kwargs)

# 总结例外：都是特殊情况，99% 的魔术方法严格走类型查找
```

#### 实例方法 vs 魔术方法：查找链对比

```python
# ─── 普通方法查找链 ───
class Demo:
    def method(self):
        return "class method"

obj = Demo()
obj.method()        # "class method"

obj.method = lambda: "instance method"
obj.method()        # "instance method"  ← 实例覆盖了类方法

# ─── 魔术方法查找链 ───
class Demo2:
    def __len__(self):
        return 100

obj2 = Demo2()
len(obj2)           # 100  ← 走类型

obj2.__len__ = lambda: 200
len(obj2)           # 100  ← 还是走类型！实例上挂的 __len__ 没用
obj2.__len__()      # 200  ← 只有显式调用时才走实例
```

#### CPython 底层查找（`Objects/typeobject.c` slot lookup）：

```
Python 的 slot 查找链（以 __add__ 为例）：

type(a) → PyTypeObject
  ├── tp_as_number (PyNumberMethods*)
  │   └── nb_add → __add__ 的实现
  ├── tp_as_sequence (PySequenceMethods*)
  │   └── sq_concat → 序列拼接
  └── tp_as_mapping (PyMappingMethods*)
      └── mp_subscript → 映射下标

当 Python 执行 a + b:
1. 尝试 type(a).tp_as_number.nb_add(a, b)    ← 快速路径
2. 如果为 NULL，尝试 type(b).tp_as_number.nb_add(b, a)  ← 反向操作
3. 如果都失败 → TypeError

注意：tp_* 是 C 层面的函数指针数组，不是 Python dict！
所以查找速度是 O(1) 指针解引用，而不是 dict 哈希查找。
这是为什么魔术方法必须在类型上查找——C 层面没有为实例预留槽位。
```

#### 一句话

> **魔术方法的重载意义在于"为你的类定义运算符行为"，而不是"为某个实例临时修改行为"。类的所有实例共享运算符实现保证了代码的确定性和性能。你要修改单个实例的行为，用普通方法或属性，魔术方法不适合干这个。**

### 2.4.4 运算符重载

**下面是 运算符重载 的代码示例：**
```python
class Vector:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    # ─── 字符串表示 ───
    def __repr__(self):
        """开发者的字符串表示（repr() 调用）"""
        return f"Vector({self.x}, {self.y})"

    def __str__(self):
        """用户的字符串表示（print() 调用）"""
        return f"({self.x}, {self.y})"

    # ─── 算术运算符 ───
    def __add__(self, other):
        """a + b → type(a).__add__(a, b)"""
        return Vector(self.x + other.x, self.y + other.y)

    def __sub__(self, other):
        return Vector(self.x - other.x, self.y - other.y)

    def __mul__(self, other):
        """标量乘法"""
        if isinstance(other, (int, float)):
            return Vector(self.x * other, self.y * other)
        return NotImplemented  # 让 other.__rmul__ 有机会处理

    def __rmul__(self, other):
        """反向乘法：3 * v 时调用"""
        return self.__mul__(other)

    # ─── 比较运算符 ───
    def __eq__(self, other):
        return self.x == other.x and self.y == other.y

    def __lt__(self, other):
        return (self.x**2 + self.y**2) < (other.x**2 + other.y**2)

    def __hash__(self):
        """可哈希：需要与 __eq__ 一致"""
        return hash((self.x, self.y))

    # ─── 一元运算符 ───
    def __abs__(self):
        return (self.x**2 + self.y**2) ** 0.5

    def __neg__(self):
        return Vector(-self.x, -self.y)

    def __bool__(self):
        return bool(self.x or self.y)

# 使用
v1 = Vector(1, 2)
v2 = Vector(3, 4)
print(v1 + v2)          # (4, 6)
print(3 * v1)           # (3, 6)
print(abs(v1))          # 2.236...
print(v1 == Vector(1, 2))  # True
print({v1: "point"})    # 可用作字典键（因为有 __hash__）
```

**运算符到魔术方法完整映射表：**

```python
# ─── 算术 ───
# __add__(self, o)        → self + o
# __sub__(self, o)        → self - o
# __mul__(self, o)        → self * o
# __truediv__(self, o)    → self / o
# __floordiv__(self, o)   → self // o
# __mod__(self, o)        → self % o
# __pow__(self, o)        → self ** o
# __matmul__(self, o)     → self @ o  (矩阵乘法，AI 场景常用)
#
# ─── 反向运算 ─── (左侧无对应方法时调用)
# __radd__, __rsub__, __rmul__, __rdiv__ ...
#
# ─── 就地运算 ───
# __iadd__(self, o)       → self += o
# __isub__, __imul__, ...
#
# ─── 比较 ───
# __eq__(self, o)         → self == o
# __ne__(self, o)         → self != o
# __lt__(self, o)         → self <  o
# __le__(self, o)         → self <= o
# __gt__(self, o)         → self >  o
# __ge__(self, o)         → self >= o
```

### 2.4.5 容器模拟（让你的对象像 list/dict）

**下面是 容器模拟（让你的对象像 list/dict） 的代码示例：**
```python
class CustomList:
    def __init__(self, items):
        self._items = list(items)

    # ─── 长度 ───
    def __len__(self):
        return len(self._items)

    # ─── 下标访问 ───
    def __getitem__(self, i):
        """支持 obj[i]、obj[i:j]（切片自动传递 slice 对象）"""
        return self._items[i]

    def __setitem__(self, i, v):
        self._items[i] = v

    def __delitem__(self, i):
        del self._items[i]

    # ─── 迭代 ───
    def __iter__(self):
        return iter(self._items)

    # ─── 成员检测 ───
    def __contains__(self, item):
        return item in self._items

    # ─── 反转 ───
    def __reversed__(self):
        return reversed(self._items)

    # ─── 追加 ───
    def __iadd__(self, other):
        self._items.extend(other)
        return self

# 使用
cl = CustomList([1, 2, 3])
print(len(cl))           # 3
print(cl[1])             # 2
print(2 in cl)           # True
cl += [4, 5]
print(list(cl))          # [1, 2, 3, 4, 5]
```

### 2.4.6 属性访问控制

**下面是 属性访问控制 的代码示例：**
```python
class Config:
    """魔术方法控制属性访问的完整示例"""
    def __init__(self, data: dict):
        self._data = data
        self._locked = False

    # __getattr__: 仅在正常属性查找失败时调用
    def __getattr__(self, name):
        if name in self._data:
            return self._data[name]
        raise AttributeError(f"Config 没有属性 '{name}'")

    # __setattr__: 所有属性赋值都经过此方法
    def __setattr__(self, name, value):
        if name == "_locked":
            super().__setattr__(name, value)
        elif self._locked:
            raise RuntimeError("Config 已锁定，不能修改")
        else:
            self._data[name] = value

    # __delattr__: 删除属性时调用
    def __delattr__(self, name):
        if name in self._data:
            del self._data[name]
        else:
            raise AttributeError(f"'{name}' 不存在")

    # __getattribute__: 所有属性访问都经过此方法（慎用！）
    # def __getattribute__(self, name):
    #     print(f"访问 {name}")
    #     return super().__getattribute__(name)

    def lock(self):
        self._locked = True

# 使用
cfg = Config({"model": "gpt-4", "temperature": 0.7})
print(cfg.model)         # gpt-4  ← 走 __getattr__
cfg.temperature = 0.8    # 走 __setattr__
cfg.lock()
cfg.temperature = 0.5    # RuntimeError: Config 已锁定
```

**`__getattr__` vs `__getattribute__` 区别：**

```
obj.attr 的查找链：

1. type(obj).__getattribute__(obj, "attr")
   ├── obj.__class__.__dict__["attr"]          ← 类属性
   ├── 沿 MRO 搜索（父类、混合类）               ← 继承链
   ├── obj.__dict__["attr"]                    ← 实例属性（数据描述符优先于实例 dict）
   ├── type(obj).__dict__["attr"].__get__()    ← 描述符协议
   └── 找不到 → __getattr__(obj, "attr")       ← 兜底钩子

2. 如果定义了 __getattribute__，它完全接管上述查找
   99% 的情况下不需要重写 __getattribute__，用 __getattr__ 就够了
```

#### `@property` vs `__getattr__` 区别

这两个经常被混淆，但它们是完全不同的机制：

```
                    @property                         __getattr__
                    ─────────                         ──────────
本质              描述符（descriptor）                实例方法的兜底钩子
触发时机          属性存在时触发                      属性查找失败时触发
查找链位置        在正常查找链中（类 __dict__）         在查找链最末尾
声明方式          显式命名每个属性                     动态处理任意属性
作用域           作用于特定属性名                     作用于所有未定义的属性

obj.attr 的完整查找链中，它们的位置：

obj.attr
  │
  ├─ type(obj).__getattribute__(obj, "attr")  ← __getattribute__ 接管全部
  │    │
  │    ├─ @property（如果类中定义了同名的 property）← 在这里拦截！
  │    │    → 返回 property.fget() 的值
  │    │    → 不继续往下搜了！
  │    │
  │    ├─ 类 __dict__ → 父类 __dict__ → ...  ← 普通属性查找
  │    ├─ obj.__dict__                         ← 实例属性
  │    ├─ 非数据描述符的 __get__               ← 方法绑定等
  │    │
  │    └─ 找不到属性 → __getattr__(obj, "attr")  ← 兜底！只有上面都失败了才到这
  │
  └─ 最终：返回结果或 AttributeError
```

**关键区别一：触发条件不同**

```python
class Demo:
    def __getattr__(self, name):
        return f"兜底: {name}"

    @property
    def color(self):
        return "red"

d = Demo()

# @property 触发的场景：
print(d.color)      # red  ← @property 正常触发

# __getattr__ 触发的场景：
print(d.size)       # 兜底: size  ← color 不存在，走 __getattr__
print(d.xxx)        # 兜底: xxx   ← 任何不存在的属性都走这里

# __getattr__ 不会被调用的场景：
d.color = "blue"    # 给了实例属性
print(d.color)      # blue  ← 现在 obj.__dict__["color"] = "blue"
                    #        查找链在 __dict__ 阶段就找到了，不会到 __getattr__
```

**关键区别二：@property 是具名的，__getattr__ 是通配的**

```python
class WithProperty:
    @property
    def name(self):
        return "固定的名字"

    @property
    def age(self):
        return 25

    # 每个属性都要写一个 @property → 具名

class WithGetattr:
    def __getattr__(self, name):
        if name.startswith("_"):
            raise AttributeError(name)
        if name in self._db:
            return self._db[name]
        raise AttributeError(name)

    # 一个 __getattr__ 处理所有 → 通配
```

**关键区别三：@property 是类级别的，__getattr__ 不受实例属性影响**

```python
class A:
    @property
    def x(self):
        return 1

    def __getattr__(self, name):
        return 999

a = A()
print(a.x)        # 1    ← @property 永远在类 __dict__ 里

# 在实例上覆盖 x：
a.__dict__["x"] = 100
print(a.x)        # 1    ← @property 是数据描述符，优先于实例 __dict__！
                  #        这体现了描述符的优先级规则

# 对比：普通方法可以在实例上覆盖
class B:
    def get_x(self):
        return 1

b = B()
b.get_x = lambda: 100
print(b.get_x())  # 100  ← 实例属性覆盖了类方法
```

**关键区别四：写入行为不同**

```python
class Demo:
    @property
    def readonly(self):
        return 42

    def __getattr__(self, name):
        return f"动态: {name}"

d = Demo()

# @property 默认只读
d.readonly = 100  # AttributeError: can't set attribute
# 解决方法：加 @readonly.setter

# __getattr__ 不影响写入，__setattr__ 才是负责写入的
d.anything = 100  # ✅ 正常写入到 __dict__
print(d.anything) # 100  ← 现在 it's in __dict__, __getattr__ 不会被触发

# 但先有 __setattr__ 可以拦截：
class Demo2:
    def __setattr__(self, name, value):
        if name == "locked":
            raise RuntimeError("locked!")
        super().__setattr__(name, value)
```

**什么时候用哪个？**

```
选 @property：                               选 __getattr__：
─────────────                                ──────────────
• 已知的具体属性，有明确的计算逻辑              • 动态属性名（从 DB/API/config 查询）
• 需要 getter/setter/deleter                  • 属性名在运行时才确定
• 想要只读属性（不写 setter）                   • 对象的属性数量不确定
• 属性名固定、数量少                            • 做代理/包装对象
• 需要类型检查或数据转换                        • 实现 Dict2Obj 等动态访问模式

【典型场景对比】

@propery 场景：                                     __getattr__ 场景：

class Circle:                                       class DynamicConfig:
    def __init__(self, radius):                         def __init__(self, data: dict):
        self._radius = radius                               self._data = data

    @property                                         def __getattr__(self, name):
    def radius(self):                                       if name in self._data:
        return self._radius                                     return self._data[name]
                                                             raise AttributeError(name)
    @property
    def area(self):                                     # 不存在的 key 自动变成属性
        return 3.14 * self._radius ** 2                 cfg = DynamicConfig({"host": "localhost"})
                                                        print(cfg.host)  # localhost ✅
    @property                                           print(cfg.port)  # AttributeError ❌
    def diameter(self):
        return self._radius * 2

# 固定三个属性，每个都有自己的逻辑
# 不能写 c.area = xxx（只读计算属性）
```

**一句话总结：**

> **`@property` 是"精确拦截"——针对特定已知属性定制 getter/setter。`__getattr__` 是"最终兜底"——所有找不到的属性统一处理，适合动态场景。前者用于已知属性名的计算逻辑，后者用于未知属性名的动态分发。**

### 2.4.7 `__dict__` 与 `__slots__`

#### `__dict__` 是什么——对象的属性存储区

每个 Python 对象内部都有一个字典 `__dict__`，存着该对象的所有实例属性。

```python
class Dog:
    species = "Canine"  # 类属性（存在 Dog.__dict__ 里）

    def __init__(self, name, age):
        self.name = name  # 实例属性（存在 self.__dict__ 里）
        self.age = age    # 同上

d = Dog("小白", 3)

print(d.__dict__)       # {'name': '小白', 'age': 3}  ← 实例属性
print(Dog.__dict__)     # mappingproxy({...})         ← 类属性（只读包装）

# 本质：obj.attr = value 就是在操作 obj.__dict__["attr"] = value
d.__dict__["color"] = "white"
print(d.color)          # white  ← 等价于 d.__dict__["color"]
```

#### 对象的内存布局

```
实例对象的内存：

┌────────────────────────────┐
│ PyObject_HEAD              │
│   ob_refcnt = 1            │
│   ob_type → &PyType_Dog    │  ← 指向类对象
├────────────────────────────┤
│ __dict__ (PyDictObject*) ──┼──→ {"name": "小白", "age": 3}
│                            │      ↑  这就是 __dict__
└────────────────────────────┘      ↑
                                    ↑
当你写 d.name 时：                   ↑
  type(d).__getattribute__(d, "name")
  → 先搜类型（Dog.__dict__ "species" 在这找到）
  → 再搜实例（d.__dict__ "name" 在这找到）
  → 返回 "小白"
```

#### 类、实例、函数各自的 `__dict__` 对比

```python
class Demo:
    cls_attr = "类级别的"

    def __init__(self):
        self.inst_attr = "实例级别的"

    def method(self):
        pass

d = Demo()

print(type(d.__dict__))          # <class 'dict'>          ← 普通 dict，可读写
print(type(Demo.__dict__))       # <class 'mappingproxy'>  ← 只读包装，防乱改

# mappingproxy 和 dict 的区别：
Demo.__dict__["cls_attr"] = "??"  # TypeError: 'mappingproxy' object does not support item assignment
Demo.cls_attr = "改了"             # OK：Python 内部走 type.__setattr__，不走 __dict__

# 函数的 __dict__：
def f(): pass
f.custom_attr = "函数属性"
print(f.__dict__)  # {'custom_attr': '函数属性'}
```

```
           __dict__ 类型       是否可写          存储内容
          ────────────         ───────          ────────
实例       dict                可读写            实例属性（self.xxx）
类         mappingproxy        只读              类属性、方法、描述符
函数       dict                可读写            自定义属性（极少用）
模块       dict                可读写            模块中定义的所有名字
```

#### `__dict__` 的三大用途

**用途 1：内省（introspection）——查看对象有什么**

```python
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y
        self._hidden = True

p = Point(3, 4)

# 列出所有实例属性名
print(list(p.__dict__.keys()))     # ['x', 'y', '_hidden']

# 列出所有实例属性值
print(list(p.__dict__.values()))   # [3, 4, True]

# 遍历所有属性
for key, val in p.__dict__.items():
    print(f"{key} = {val}")
# x = 3
# y = 4
# _hidden = True

# vars(obj) 是访问 __dict__ 的推荐方式
print(vars(p))  # {'x': 3, 'y': 4, '_hidden': True}
```

**用途 2：批量赋值/更新属性**

```python
class Config:
    def __init__(self):
        self.host = "localhost"
        self.port = 8080
        self.debug = False

    def update(self, **kwargs):
        """批量更新属性"""
        for key, value in kwargs.items():
            if key in self.__dict__:  # 只允许更新已有属性
                self.__dict__[key] = value
            else:
                raise AttributeError(f"未知配置项: {key}")

cfg = Config()
cfg.update(port=9090, debug=True)
print(cfg.port)   # 9090
print(cfg.debug)  # True
# cfg.update(unknown=1)  # AttributeError: 未知配置项: unknown
```

**用途 3：实现动态对象（属性即 dict key）**

```python
class DynamicObject:
    """类似 JavaScript 对象——任意属性随意设"""
    def __getattr__(self, name):
        if name.startswith("_"):
            raise AttributeError(name)
        # 属性不存在时，默认返回 None 而不是报错
        return None

    def __setattr__(self, name, value):
        self.__dict__[name] = value

    def __delattr__(self, name):
        if name in self.__dict__:
            del self.__dict__[name]

    def to_dict(self):
        return dict(self.__dict__)

obj = DynamicObject()
obj.name = "动态"
obj.value = 42
print(obj.name)   # 动态
print(obj.xxx)    # None  ← 不报错
print(obj.to_dict())  # {'name': '动态', 'value': 42}
```

#### 面试常考：obj.__dict__ 和 type(obj).__dict__ 不是一个东西

```python
class A:
    x = 100  # A.__dict__["x"] = 100

a = A()
a.x = 200   # a.__dict__["x"] = 200

print(a.x)            # 200  ← 实例属性优先于类属性
print(A.x)            # 100  ← 类属性不变
print(a.__dict__)     # {'x': 200}
print(A.__dict__)     # {'x': 100, ...}

# 删除实例属性后，类属性就暴露出来了
del a.x
print(a.x)            # 100  ← 实例的 x 删了，现在看到类的 x

# 关键理解：
# obj.attr 查找链：
# 1. type(obj).__getattribute__(obj, "attr")
# 2. 先看类型（类属性、描述符）
# 3. 再看实例（__dict__）
# 4. 实例属性遮住类属性，但两者独立存储
```

#### `__slots__`——没有 __dict__ 的对象

```python
class Point:
    """用 __slots__ 限制只能有 x, y 两个属性"""
    __slots__ = ("x", "y")

    def __init__(self, x, y):
        self.x = x
        self.y = y

p = Point(3, 4)
print(p.x)           # 3
print(p.y)           # 4

# p.z = 5  # AttributeError: 'Point' object has no attribute 'z'

print(p.__dict__)    # AttributeError: 'Point' object has no attribute '__dict__'！
# ⚠️ __slots__ 的类没有 __dict__！

# 好处 1：省内存（一个空 dict 占 ~56 字节，1 亿个实例省 5.6GB）
# 好处 2：属性访问更快（C 层面的固定偏移访问，不是 dict 哈希查找）

# __slots__ 和 __dict__ 对比：
class WithDict:
    def __init__(self, x, y):
        self.x = x
        self.y = y

class WithSlots:
    __slots__ = ("x", "y")
    def __init__(self, x, y):
        self.x = x
        self.y = y

import sys
d = WithDict(1, 2)
s = WithSlots(1, 2)
print(sys.getsizeof(d.__dict__))  # 56 bytes（dict 本体）
print(sys.getsizeof(d))           # 56 bytes（对象 + dict 指针）
print(sys.getsizeof(s))           # 32 bytes（只有 x, y 两个指针，无 dict）

# 什么时候用 __slots__？
# - 需要创建大量对象（百万级），内存敏感
# - 属性名固定，不会动态添加
# - 大多数情况用普通类就好，__slots__ 是优化手段
```

#### 一句话总结

> **`__dict__` 是 Python 对象的"属性背包"——所有实例属性都存在里面。类也有 `__dict__` 但被 mappingproxy 保护。`vars(obj)` 是读 `__dict__` 的推荐写法。追求极致省内存时用 `__slots__` 干掉 `__dict__`，代价是不能动态加属性。**

### 2.4.8 可调用对象（`__call__`）

#### 核心概念：让实例像函数一样调用

```python
class Adder:
    def __init__(self, n):
        self.n = n

    def __call__(self, x):
        """实例(...) → 触发 __call__"""
        return self.n + x

add5 = Adder(5)      # 创建实例
print(add5(3))       # 8  ← 实例当函数用！等价于 add5.__call__(3)
print(add5(10))      # 15 ← 每次调用都加 5

# 没有 __call__ 会怎样？
class NonCallable:
    pass

obj = NonCallable()
# obj()  # TypeError: 'NonCallable' object is not callable
```

**实现原理：**

```
obj(args)  →  type(obj).__call__(obj, args)

和所有魔术方法一样，__call__ 在类型上查找：
- obj()  → type(obj).__call__(obj)
- 本质上和 len(obj)、obj[key] 是同一机制
```

#### 判断对象是否可调用

```python
callable(Adder(5))   # True  ← 定义了 __call__
callable(42)         # False ← 整数不可调用
callable(print)      # True  ← 函数当然可调用
callable(int)        # True  ← 类型也可调用（int("42")）

# 检查类有没有 __call__：
print(hasattr(Adder, "__call__"))  # True
print(hasattr(NonCallable, "__call__"))  # False
```

#### 场景 1：替代函数工厂（保存状态的可调用对象）

```python
# ─── 函数方式 ───
def make_counter():
    count = 0
    def counter():
        nonlocal count
        count += 1
        return count
    return counter

c1 = make_counter()
print(c1())  # 1
print(c1())  # 2

# ─── __call__ 方式（更清楚）───
class Counter:
    def __init__(self):
        self.count = 0

    def __call__(self):
        self.count += 1
        return self.count

    def reset(self):
        self.count = 0

c2 = Counter()
print(c2())     # 1  ← 实例当函数用
print(c2())     # 2
c2.reset()      # 额外功能：重置（闭包做不到）
print(c2())     # 1

# 两种方法等价，但类方式可读性更好，还能加额外方法
```

#### 场景 2：偏函数（固定参数）

```python
class Partial:
    """手写偏函数：固定某些参数"""
    def __init__(self, func, *args, **kwargs):
        self.func = func
        self.args = args
        self.kwargs = kwargs

    def __call__(self, *args, **kwargs):
        all_args = self.args + args
        all_kwargs = {**self.kwargs, **kwargs}
        return self.func(*all_args, **all_kwargs)

def power(base, exp):
    return base ** exp

square = Partial(power, exp=2)  # 固定 exp=2
cube = Partial(power, exp=3)    # 固定 exp=3

print(square(5))    # 25   ← 等价于 power(5, exp=2)
print(cube(5))      # 125  ← 等价于 power(5, exp=3)
```

#### 场景 3：可调用对象做装饰器

```python
import time

class Timer:
    """装饰器：统计函数调用次数和耗时"""
    def __init__(self, func):
        self.func = func
        self.count = 0
        self.total_time = 0

    def __call__(self, *args, **kwargs):
        self.count += 1
        start = time.time()
        result = self.func(*args, **kwargs)
        elapsed = time.time() - start
        self.total_time += elapsed
        print(f"{self.func.__name__} 第 {self.count} 次调用，耗时 {elapsed:.3f}s")
        return result

@Timer
def slow_add(a, b):
    time.sleep(0.1)
    return a + b

print(slow_add(1, 2))  # slow_add 第 1 次调用，耗时 0.100s → 3
print(slow_add(3, 4))  # slow_add 第 2 次调用，耗时 0.101s → 7
```

#### 场景 4：AI 中的可调用对象

```python
class LLMWrapper:
    """把 API 调用包装成可调用对象，方便传参"""
    def __init__(self, model: str, temperature: float = 0.7):
        self.model = model
        self.temperature = temperature
        self.call_count = 0

    def __call__(self, prompt: str) -> str:
        self.call_count += 1
        # 实际会调用 API
        return f"[{self.model}] 回复: {prompt[:20]}..."

llm = LLMWrapper("gpt-4", 0.3)
print(llm("翻译这段文本"))   # 就像调用函数一样
print(llm("总结这篇文章"))
print(f"共调用 {llm.call_count} 次")
```

#### 一句话总结

> **`__call__` 让类的实例像函数一样调用 `obj()`。本质和 `__len__`、`__getitem__` 一样是魔术方法——查 `type(obj).__call__(obj)`。适合做有状态的函数替代品（计数器、偏函数、装饰器、API 封装）。**

### 2.4.9 序列化相关魔术方法

#### 核心概念：把对象转成字符串/字节

序列化相关魔术方法控制对象在以下场景的字符串/字节表示：

```
str(obj)   → obj.__str__()        # 用户可读
repr(obj)  → obj.__repr__()       # 开发者可读（调试用）
f"{obj}"   → obj.__format__("")   # 格式化
bytes(obj) → obj.__bytes__()      # 字节序列（极少用）
```

#### `__repr__` vs `__str__` —— 最常用的两个

```python
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    # __repr__：调试用，目标是"能还原出这个对象"
    def __repr__(self):
        return f"Point({self.x}, {self.y})"

    # __str__：展示用，目标是"用户看得懂"
    def __str__(self):
        return f"({self.x}, {self.y})"

p = Point(3, 4)

print(str(p))    # (3, 4)       ← 用户友好的
print(repr(p))   # Point(3, 4)  ← 开发者友好的
print(p)         # (3, 4)       ← print() 优先调 __str__

# 在容器中，显示 __repr__：
print([p, p])    # [Point(3, 4), Point(3, 4)]

# 只定义 __repr__ 时，__str__ 会复用 __repr__：
class OnlyRepr:
    def __repr__(self):
        return "OnlyRepr()"

obj = OnlyRepr()
print(str(obj))   # OnlyRepr()  ← 没有 __str__，fallback 到 __repr__
```

**经验法则：**

```
__repr__ → 开发者看，尽可能包含足够信息，最好能被 eval() 还原
           repr(Point(3,4)) → "Point(3, 4)"  ← 复制到代码里就能用
__str__  → 用户看，简洁漂亮
           str(Point(3,4)) → "(3, 4)"  ← 用户直接看
```

#### `__format__` —— 自定义格式化

```python
class Person:
    def __init__(self, name, age):
        self.name = name
        self.age = age

    def __format__(self, format_spec):
        """f"{obj}" 和 format(obj) 调用
           format_spec 是冒号后面的部分
        """
        if format_spec == "v":
            # 详细格式
            return f"Person: {self.name}, 年龄: {self.age}"
        elif format_spec == "s":
            # 简短格式
            return f"{self.name}({self.age})"
        else:
            # 默认
            return f"{self.name} (age {self.age})"

p = Person("Alice", 30)

print(f"{p}")        # Alice (age 30)     ← format_spec = ""
print(f"{p:v}")      # Person: Alice, 年龄: 30  ← format_spec = "v"
print(f"{p:s}")      # Alice(30)          ← format_spec = "s"
print(format(p, "v")) # Person: Alice, 年龄: 30  ← format() 函数

# 对于数值类型，format_spec 有标准语法：
# f"{3.14159:.2f}" → "3.14"  ← .2f 是 format_spec
# f"{100:x}"       → "64"    ← x 是十六进制
```

#### `__bytes__` —— 转字节序列

```python
class Icon:
    def __init__(self, data: bytes):
        self.data = data

    def __bytes__(self):
        """bytes(obj) 调用"""
        return self.data

icon = Icon(b"\x89PNG\r\n\x1a\n")
print(bytes(icon))  # b'\x89PNG\r\n\x1a\n'
```

#### 让自定义类支持 `json.dumps` —— 正确做法

```python
import json
from datetime import datetime

class AIResponse:
    def __init__(self, text: str, confidence: float, created_at: datetime = None):
        self.text = text
        self.confidence = confidence
        self.created_at = created_at or datetime.now()

    def __repr__(self):
        """调试用：能看到所有字段"""
        return f"AIResponse({self.text[:20]!r}, {self.confidence:.1%})"

    def to_dict(self):
        """序列化为 dict（json.dumps 用）"""
        return {
            "text": self.text,
            "confidence": self.confidence,
            "created_at": self.created_at.isoformat(),
        }

# ─── 方式 1：手动转 dict 再 dumps（最简单）───
resp = AIResponse("The answer is 42", 0.95)
print(json.dumps(resp.to_dict(), indent=2))
# {
#   "text": "The answer is 42",
#   "confidence": 0.95,
#   "created_at": "2026-05-31T10:30:00"
# }

# ─── 方式 2：自定义 JSONEncoder（一劳永逸）───
class CustomEncoder(json.JSONEncoder):
    def default(self, obj):
        # 只有 json.dumps 遇到"不认识"的类型时才调 default
        if isinstance(obj, AIResponse):
            return obj.to_dict()
        if isinstance(obj, datetime):
            return obj.isoformat()
        # 还有不认识的 → 让父类报错
        return super().default(obj)

resp2 = AIResponse("Hello world", 0.88)
print(json.dumps(resp2, cls=CustomEncoder, indent=2))
# {
#   "text": "Hello world",
#   "confidence": 0.88,
#   "created_at": "2026-05-31T10:30:00"
# }

# ─── 方式 3：用 default 参数（更简洁）───
def json_default(obj):
    if isinstance(obj, AIResponse):
        return obj.to_dict()
    if isinstance(obj, datetime):
        return obj.isoformat()
    raise TypeError(f"不能序列化 {type(obj)}")

print(json.dumps(resp2, default=json_default, indent=2))
# 同上
```

#### `__json__` 是官方魔术方法吗？—— 不是！

```python
# ❌ Python 没有 __json__ 这个魔术方法！
class Wrong:
    def __json__(self):
        return {"data": 123}

# json.dumps(Wrong())  # TypeError: Object of type Wrong is not JSON serializable
# ❌ json.dumps 不认识 __json__，还是会报错

# 要在类里加 __json__ 并让 json.dumps 识别，必须写自定义 Encoder：
class WithJson:
    def __json__(self):
        return {"data": 123}

class MyEncoder(json.JSONEncoder):
    def default(self, obj):
        if hasattr(obj, "__json__"):
            return obj.__json__()
        return super().default(obj)

print(json.dumps(WithJson(), cls=MyEncoder))  # {"data": 123}
# __json__ 只是我们约定的一个普通方法名，不是 Python 的魔术方法
```

#### 常见的序列化/反序列化模式

```python
from dataclasses import dataclass, asdict
import json

@dataclass
class Config:
    """dataclass 自动生成 __repr__、__init__ 等"""
    model: str
    temperature: float
    max_tokens: int = 2048

    def save(self, path: str):
        with open(path, "w") as f:
            json.dump(asdict(self), f, indent=2)

    @classmethod
    def load(cls, path: str) -> "Config":
        with open(path) as f:
            data = json.load(f)
        return cls(**data)

cfg = Config("gpt-4", 0.7)
cfg.save("config.json")
loaded = Config.load("config.json")
print(loaded)  # Config(model='gpt-4', temperature=0.7, max_tokens=2048)
```

#### 一句话总结

> **序列化魔术方法控制对象如何转字符串：`__repr__` 给开发者调试、`__str__` 给用户展示、`__format__` 控制 `f"{obj}"` 格式。json.dumps 不认识任何魔术方法，要用 `default` 参数或自定义 `JSONEncoder`。dataclass + asdict 是最省事的 JSON 序列化方案。**

### 2.4.10 完整魔术方法分类表

**下面是 Python 所有魔术方法的完整分类速查表：**
```python
# ─── 对象生命周期 ───
# __new__(cls, ...)    创建实例（先于 __init__）
# __init__(self, ...)   初始化实例
# __del__(self)        销毁前清理（析构函数）
#
# ─── 字符串表示 ───
# __repr__(self)       repr() → 开发者可读
# __str__(self)        str(), print() → 用户可读
# __format__(self, fmt) f"{obj:fmt}" 格式化
#
# ─── 容器 ───
# __len__(self)        len()
# __getitem__(self, k) obj[k], 切片
# __setitem__(self, k, v) obj[k] = v
# __delitem__(self, k) del obj[k]
# __iter__(self)       iter(), for
# __reversed__(self)   reversed()
# __contains__(self, x) x in obj
# __missing__(self, k) dict 子类中查找不到时
#
# ─── 可调用 ───
# __call__(self, ...)  obj()
#
# ─── 数值 ───
# __bool__(self)       bool(obj), if obj
# __int__(self)        int(obj)
# __float__(self)      float(obj)
# __hash__(self)       hash(obj), set/dict 键
# __index__(self)      切片索引
#
# ─── 上下文管理 ───
# __enter__(self)      with obj as x:
# __exit__(self, ...)  退出 with 块
#
# ─── 异步 ───
# __aiter__(self)      async for
# __anext__(self)      async for 的下一个
# __aenter__(self)     async with
# __aexit__(self, ...) 退出 async with
# __await__(self)      await obj
#
# ─── 类创建 ───
# __init_subclass__(cls) 子类继承时调用
# __set_name__(cls, name) 描述符被赋值到类时
# __class_getitem__(cls) 泛型订阅：SomeClass[T]
```

### 2.4.11 常见错误

**错误 1：魔术方法在实例上定义不在类上**

```python
class Wrong:
    pass

obj = Wrong()
obj.__len__ = lambda: 42  # 实例上定义
print(len(obj))           # TypeError: object of type 'Wrong' has no len()

# 正确：必须在类上定义
class Right:
    def __len__(self):
        return 42
```

**错误 2：忘记 `__eq__` 时 `__hash__` 自动为 None**

```python
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y
    def __eq__(self, other):
        return self.x == other.x and self.y == other.y
    # 没定义 __hash__ → hash() 抛出 TypeError

p = Point(1, 2)
d = {p: "value"}  # TypeError: unhashable type: 'Point'

# 修复：
# class Point:
#     def __hash__(self):
#         return hash((self.x, self.y))
```
**错误 3：`__getattr__` 和 `__getattribute__` 无限递归**

```python
class Bad:
    def __getattr__(self, name):
        return self.__dict__[name]  # 递归！self.__dict__ 触发 __getattr__

    # 正确：
    # def __getattr__(self, name):
    #     return super().__getattribute__(name)  # 跳过 __getattr__
    #     # 或
    #     return object.__getattribute__(self, name)
```

**错误 4：`NotImplemented` vs `NotImplementedError`**

```python
class A:
    def __add__(self, other):
        if isinstance(other, A):
            return ...
        return NotImplemented  # ← 常量，告诉 Python 尝试反向运算

    # NotImplementedError 是异常，完全不同！
```

**错误 5：`__del__` 不保证被调用**

```python
class Resource:
    def __del__(self):
        print("清理资源")  # 不一定执行！

# 不要依赖 __del__ 做关键清理，用上下文管理器 + with 语句
```

### 2.4.12 AI 场景案例

**场景一：灵活配置类（点号访问 + 锁定机制）**

```python
class AIConfig:
    """AI 配置：支持 dict 下标、点号属性、锁定"""
    def __init__(self, config: dict):
        self._config = config
        self._locked = False

    def __getattr__(self, name):
        if name.startswith("_"):
            raise AttributeError(name)
        if name in self._config:
            return self._config[name]
        raise AttributeError(f"配置项 '{name}' 不存在")

    def __setattr__(self, name, value):
        if name.startswith("_"):
            super().__setattr__(name, value)
        elif self._locked:
            raise RuntimeError(f"配置已锁定，无法修改 '{name}'")
        else:
            self._config[name] = value

    def __getitem__(self, key):
        return self._config[key]

    def __setitem__(self, key, value):
        if self._locked:
            raise RuntimeError(f"配置已锁定")
        self._config[key] = value

    def __contains__(self, key):
        return key in self._config

    def __repr__(self):
        return f"AIConfig({self._config})"

    def lock(self):
        self._locked = True

    @classmethod
    def from_yaml(cls, path):
        import yaml
        with open(path) as f:
            return cls(yaml.safe_load(f))

# 使用
config = AIConfig({
    "model": "gpt-4",
    "temperature": 0.7,
    "max_tokens": 2048,
    "api_key": "sk-xxx",
})
print(config.model)           # gpt-4          ← __getattr__
print(config["max_tokens"])   # 2048            ← __getitem__
print("api_key" in config)    # True            ← __contains__
config.temperature = 0.8       #                 ← __setattr__
config.lock()
config.temperature = 0.5       # RuntimeError    ← 锁定保护
```

**场景二：可调用模型包装器**

```python
class ModelWrapper:
    """可调用的 AI 模型包装器——同时支持推理和统计"""
    def __init__(self, model_name: str, device="cuda"):
        self.model_name = model_name
        self.device = device
        self.call_count = 0
        self.total_latency = 0.0
        self._model = None  # 延迟加载

    def __call__(self, prompt: str, **kwargs) -> str:
        """调用模型推理"""
        if self._model is None:
            self._load_model()

        import time
        start = time.perf_counter()
        result = self._model.generate(prompt, **kwargs)
        latency = time.perf_counter() - start

        self.call_count += 1
        self.total_latency += latency
        return result

    def __repr__(self):
        avg = self.total_latency / self.call_count if self.call_count else 0
        return (f"ModelWrapper({self.model_name}, "
                f"calls={self.call_count}, avg_latency={avg:.2f}s)")

    def __getattr__(self, name):
        """透明代理到底层模型"""
        if self._model and hasattr(self._model, name):
            return getattr(self._model, name)
        raise AttributeError(f"'{self.model_name}' 没有属性 '{name}'")

    def _load_model(self):
        from transformers import AutoModelForCausalLM, AutoTokenizer
        self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)
        self._model = AutoModelForCausalLM.from_pretrained(
            self.model_name, device_map=self.device
        )

model = ModelWrapper("gpt2")
response = model("Hello, how are you?")  # 可调用
print(model)  # ModelWrapper(gpt2, calls=1, avg_latency=0.53s)
```

---

## 2.5 元类

### 2.5.1 一句话核心本质

**元类是创建类的类——普通类定义对象的行为，元类定义类的创建行为。`type` 是所有元类的基类，`class A:` 等价于 `A = type("A", (), {})`。**

### 2.5.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 类的创建者 | JVM 类加载器 + 编译器 | 元类（默认 `type`） |
| 运行时创建类 | 不支持（类在编译期固定） | `type(name, bases, dict)` 动态创建 |
| 自定义创建逻辑 | 注解处理器（APT）编译期处理 | 元类 `__new__` 运行时拦截 |
| 类即对象 | 不是（`Class<T>` 是类的描述，但不是类本身） | 是！类型是 `type` 的实例 |
| 常见框架 | Spring 注解 + 反射 | ORM 模型声明、Django models、SQLAlchemy |

**本质区别：** Java 的类在编译期固定，运行时可以通过反射读取元信息但无法改变类的创建流程。Python 中类本身是 `type` 的实例，通过自定义元类可以在类创建时（定义时）注入任意逻辑。

### 2.5.3 技术原理：类的创建流程

**`class MyClass(Base, metaclass=Meta)` 的完整执行过程：**

```
遇到 class 语句时 Python 的执行链：

1. 解析类体（class body）：
   ┌─────────────────────────────────────────────────┐
   │ namespace = {}  ← 准备命名空间                   │
   │ 执行类体中的代码（属性、方法定义）                │
   │   → attr = 42                                   │
   │   → def method(self): ...                       │
   │ namespace = {"attr": 42, "method": <function>}   │
   └─────────────────────────────────────────────────┘
                       ↓
2. 确定元类（metaclass resolution）：
   ┌─────────────────────────────────────────────────┐
   │ 1. metaclass = type("MyClass", (), {}).__class__│
   │    → __init_subclass__ 也可能参与               │
   │ 2. 如果有 metaclass= 参数，使用显式指定的        │
   │ 3. 如果有基类，找到基类中最派生的元类            │
   └─────────────────────────────────────────────────┘
                       ↓
3. 调用元类创建类对象：
   ┌─────────────────────────────────────────────────┐
   │ MyClass = metaclass.__new__(                    │
   │     metaclass,          # <class 'Meta'>        │
   │     "MyClass",          # 类名                   │
   │     (Base,),            # 基类元组               │
   │     namespace           # 类属性字典             │
   │ )                                               │
   │                                                 │
   │ metaclass.__init__(MyClass, "MyClass",           │
   │     (Base,), namespace)                         │
   └─────────────────────────────────────────────────┘
                       ↓
4. 命名空间绑定：
   ┌─────────────────────────────────────────────────┐
   │ 当前作用域的 __class__ = MyClass（闭包引用）     │
   │ 将 MyClass 绑定到当前命名空间                    │
   └─────────────────────────────────────────────────┘
```

**`type.__new__` 的内部行为（CPython `Objects/typeobject.c`）：**

```
type.__new__(metacls, name, bases, namespace):
1. 分配 PyTypeObject 的内存
2. 设置 tp_name = name
3. 设置 tp_bases = bases
4. 设置 tp_dict = namespace
5. 计算 MRO（C3 线性化）
6. 填充 tp_* 槽位（从基类继承）
7. 设置 __dict__、__weakref__ 等描述符
8. 返回新创建的类对象
```

### 2.5.4 基本用法：自定义元类

**以下是 ：自定义元类 的基本用法：**
```python
class LogMeta(type):
    """元类：自动记录所有子类的创建"""
    def __new__(mcs, name, bases, namespace):
        print(f"[元类] 创建类: {name}")
        print(f"[元类]   基类: {bases}")
        print(f"[元类]   属性: {list(namespace.keys())}")

        # 自动添加类属性
        namespace["created_by"] = mcs.__name__

        return super().__new__(mcs, name, bases, namespace)

    def __init__(cls, name, bases, namespace):
        """类初始化完成后调用"""
        print(f"[元类] {name} 初始化完成")
        super().__init__(name, bases, namespace)

class BaseModel(metaclass=LogMeta):
    pass
# [元类] 创建类: BaseModel
# [元类]   基类: ()
# [元类]   属性: ['__module__', '__qualname__']
# [元类] BaseModel 初始化完成

class UserModel(BaseModel):
    table = "users"
    def get_name(self): ...
# [元类] 创建类: UserModel
# [元类]   基类: (<class 'BaseModel'>,)
# [元类]   属性: ['__module__', '__qualname__', 'table', 'get_name']
# [元类] UserModel 初始化完成

print(UserModel.created_by)  # "LogMeta"
```

### 2.5.5 单例元类

**下面是 单例元类 的代码示例：**
```python
class SingletonMeta(type):
    """元类实现的单例模式——intercept __call__"""
    _instances = {}

    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            instance = super().__call__(*args, **kwargs)
            cls._instances[cls] = instance
        return cls._instances[cls]

class Database(metaclass=SingletonMeta):
    def __init__(self):
        self.connection = "db_conn_123"
        print("初始化数据库连接")

db1 = Database()  # 初始化数据库连接（只打印一次）
db2 = Database()
print(db1 is db2)  # True
```

**元类 `__call__` 的执行流程：**

```
MyClass() 调用时：
1. type.__call__(MyClass, *args, **kwargs)
   ├── MyClass.__new__(MyClass, *args, **kwargs)  → 创建实例
   └── 如果 __new__ 返回 MyClass 的实例：
       MyClass.__init__(instance, *args, **kwargs)  → 初始化

自定义元类的 __call__ 可以拦截整个过程
```

### 2.5.6 注册表元类（自动收集子类）

**下面是 注册表元类（自动收集子类） 的代码示例：**
```python
class ModelRegistry(type):
    """元类：自动注册所有子类到全局注册表"""
    registry = {}

    def __new__(mcs, name, bases, namespace):
        cls = super().__new__(mcs, name, bases, namespace)

        # 不注册基类本身
        if not bases:  # 直接继承 ModelRegistry 的类
            return cls

        # 自动注册
        ModelRegistry.registry[name] = cls

        # 添加类方法
        cls.get_by_id = classmethod(lambda cls, id: f"查询{cls.__name__}:{id}")

        return cls

class BaseModel(metaclass=ModelRegistry):
    pass

class User(BaseModel):
    pass

class Product(BaseModel):
    pass

print(ModelRegistry.registry)
# {'User': <class 'User'>, 'Product': <class 'Product'>}

print(User.get_by_id(42))  # "查询User:42"
```

### 2.5.7 ORM 字段验证元类（AI 配置验证）

**下面是 ORM 字段验证元类（AI 配置验证） 的代码示例：**
```python
class Field:
    """描述符：字段定义"""
    def __init__(self, type_, required=True, default=None):
        self.type = type_
        self.required = required
        self.default = default

    def __set_name__(self, owner, name):
        self.name = name

    def __get__(self, instance, owner):
        if instance is None:
            return self
        return instance.__dict__.get(self.name, self.default)

    def __set__(self, instance, value):
        if not isinstance(value, self.type):
            raise TypeError(
                f"{self.name} 必须是 {self.type.__name__}, "
                f"收到 {type(value).__name__}"
            )
        instance.__dict__[self.name] = value

class ModelMeta(type):
    """元类：验证所有 Field 定义，自动计算需要初始化的字段"""
    def __new__(mcs, name, bases, namespace):
        # 收集当前类中所有的 Field
        fields = {
            k: v for k, v in namespace.items()
            if isinstance(v, Field)
        }

        # 验证 required 字段
        for field_name, field in fields.items():
            if field.required and field.default is None:
                # 标记为必要传入
                namespace.setdefault("_required_fields", set()).add(field_name)

        return super().__new__(mcs, name, bases, namespace)

class AIConfig(metaclass=ModelMeta):
    model = Field(str, required=True)
    temperature = Field(float, default=0.7)
    api_key = Field(str, required=True)
    max_tokens = Field(int, default=2048)

# 使用
cfg = AIConfig()
cfg.model = "gpt-4"       # OK
cfg.api_key = "sk-xxx"    # OK
cfg.temperature = 0.8     # OK
cfg.temperature = "hot"   # TypeError: temperature 必须是 float, 收到 str
```

### 2.5.8 `__init_subclass__` —— 元类的轻量替代

99% 的场景不需要自定义元类，`__init_subclass__` 就够了：

```python
class PluginBase:
    """插件基类：子类定义时自动注册"""
    registry = {}

    def __init_subclass__(cls, **kwargs):
        super().__init_subclass__(**kwargs)
        PluginBase.registry[cls.__name__] = cls
        print(f"注册插件: {cls.__name__}")

class ImageProcessor(PluginBase):
    pass

class TextProcessor(PluginBase):
    pass

print(PluginBase.registry)
# {'ImageProcessor': <class 'ImageProcessor'>,
#  'TextProcessor': <class 'TextProcessor'>}
```

**`__init_subclass__` vs 元类选择指南：**

| 需求 | 推荐方式 |
|------|----------|
| 子类继承时执行逻辑 | `__init_subclass__` |
| 修改类的属性/方法字典 | 元类 `__new__` |
| 拦截类实例化 | 元类 `__call__` |
| 类名/基类校验 | 元类 `__new__` |
| 简单注册 | `__init_subclass__` |
| ORM 字段声明 | 元类 + 描述符 |

### 2.5.9 常见错误

**错误 1：元类冲突（metaclass conflict）**

```python
class MetaA(type): pass
class MetaB(type): pass

class A(metaclass=MetaA): pass
class B(metaclass=MetaB): pass

class C(A, B): pass  # TypeError: metaclass conflict

# 修复：创建一个继承自 MetaA 和 MetaB 的新元类
class MetaC(MetaA, MetaB): pass
class C(A, B, metaclass=MetaC): pass
```

**错误 2：忘记调用 `super().__new__` 或 `super().__init__`**

```python
class BrokenMeta(type):
    def __new__(mcs, name, bases, namespace):
        # 没有调用 super().__new__ → 类创建失败
        return None  # 类型错误

class Bad(metaclass=BrokenMeta): pass
# TypeError: __new__ returned non-type
```

**错误 3：误认为元类在运行时每个实例创建都调用**

```python
class MyMeta(type):
    def __new__(mcs, name, bases, namespace):
        print("元类 __new__ 执行")  # 只在定义类时执行一次！
        return super().__new__(mcs, name, bases, namespace)

class A(metaclass=MyMeta): pass  # 打印 "元类 __new__ 执行"
a1 = A()  # 不打印
a2 = A()  # 不打印
```

### 2.5.10 AI 场景案例：配置模型 + 自动注册

**下面是一个 配置模型 + 自动注册 的实战案例：**
```python
class AIModelMeta(type):
    """AI 模型元类：自动注册到模型工厂"""
    _model_registry = {}

    def __new__(mcs, name, bases, namespace):
        cls = super().__new__(mcs, name, bases, namespace)

        # 跳过基类
        if not bases:
            return cls

        # 自动注册
        model_name = namespace.get("model_name", name.lower())
        AIModelMeta._model_registry[model_name] = cls

        # 添加元数据
        cls.model_version = namespace.get("version", "1.0.0")

        return cls

    @classmethod
    def get_model(mcs, name):
        if name not in mcs._model_registry:
            raise KeyError(f"未知模型: {name}, 可选: {list(mcs._model_registry.keys())}")
        return mcs._model_registry[name]()

class BaseAIModel(metaclass=AIModelMeta):
    model_name = "base"

    def generate(self, prompt: str) -> str:
        raise NotImplementedError

class GPT4(BaseAIModel):
    model_name = "gpt-4"
    version = "4.0"

    def generate(self, prompt):
        return f"[GPT-4] {prompt}"

class Claude(BaseAIModel):
    model_name = "claude-3"
    version = "3.0"

    def generate(self, prompt):
        return f"[Claude] {prompt}"

# 使用：通过字符串名称创建模型
model = AIModelMeta.get_model("gpt-4")
print(model.generate("Hello"))  # [GPT-4] Hello

model = AIModelMeta.get_model("claude-3")
print(model.generate("Hi"))     # [Claude] Hi

# 查看注册表
print(AIModelMeta._model_registry)
# {'gpt-4': <class 'GPT4'>, 'claude-3': <class 'Claude'>}
```

---

## 2.6 typing

### 2.6.1 一句话核心本质

**类型注解是附加到变量/参数/返回值上的元数据，运行时完全忽略，仅被 mypy/pyright 等外部静态检查工具使用。它让 Python 在鸭子类型的基础上获得了可选的静态类型检查能力。**

### 2.6.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 检查时机 | 编译期强制检查 | 运行时完全不检查，需外部工具 |
| 类型系统 | 名义类型（nominal typing）——类型由名称决定 | 结构类型（structural typing）+ 名义类型 |
| 泛型 | 编译期擦除（Type Erasure），运行时不保留 | 参数化类型在 `__annotations__` 中保留 |
| 可选性 | 必须写 | 完全可选（不写也能运行） |
| 字节码 | 类型信息编码在 `.class` 文件中 | `__annotations__` 字典，完全无运行时效果 |
| 检查工具 | javac 内建 | mypy, pyright, pytype 等第三方 |
| 运行时获取 | 反射 `getDeclaredFields()` | `func.__annotations__`, `typing.get_type_hints()` |

**本质区别：** Java 的类型检查是语言核心功能，`javac` 强制执行。Python 的类型注解是"文档"，只是开发时的辅助，运行时所有的类型信息都是 `__annotations__` 字典中的字符串或类型对象，CPython 虚拟机完全忽略它们。

### 2.6.3 技术原理：类型注解的运行时真相

**下面通过代码来验证类型注解在运行时的真实行为：**
```python
def greet(name: str, count: int) -> str:
    return f"{name} says {count}"

# 类型注解存储在函数的 __annotations__ 字典中
print(greet.__annotations__)
# {'name': <class 'str'>, 'count': <class 'int'>, 'return': <class 'str'>}

# 运行时完全不检查！
print(greet(42, "hello"))  # 42 says hello  ← 正常运行！

# 甚至可以在运行时修改注解
greet.__annotations__["name"] = int
print(greet.__annotations__)
# {'name': <class 'int'>, 'count': <class 'int'>, 'return': <class 'str'>}

# 变量注解也不产生任何效果
x: int = "hello"  # 完全合法！
```

**CPython 如何处理注解（`compile.c` / `symtable.c`）：**

```
Python 编译 def greet(name: str) -> str:
1. 解析到参数中的 `: str` 和 `-> str`
2. 将类型表达式编译为字节码
3. 在函数创建时（def 语句执行时）：
   - 执行类型表达式字节码（获取类型对象）
   - 存入 func.__annotations__ 字典
   - 类型本身被求值但被丢弃（运行时无用）

关键：类型表达式在 def 定义时被求值！
```

**`from __future__ import annotations` 的效果（PEP 563，Python 3.7+）：**

```python
from __future__ import annotations

def greet(name: str) -> str:
    return f"Hello {name}"

# 默认行为：__annotations__ 存储类型对象
print(greet.__annotations__)
# {'name': str, 'return': str}

# 使用 future import 后：存储为字符串（延迟求值）
# {'name': 'str', 'return': 'str'}
# 好处：支持前向引用，减少 import 开销
```

### 2.6.4 类型注解语法详解

**下面是 类型注解语法详解 的代码示例：**
```python
from typing import (
    List, Dict, Tuple, Set,      # 容器类型
    Optional, Union, Any,        # 组合类型
    Callable,                    # 可调用类型
    TypeVar, Generic,            # 泛型
    Protocol,                    # 协议（结构类型）
    Literal, TypedDict,          # 字面量和字典类型
    Final, ClassVar,             # 修饰符
    overload,                    # 函数重载
    Iterator, Iterable,          # 迭代器
    Type,                        # 类型本身
)

# ─── 基础类型 ───
def process(names: List[str]) -> Dict[str, int]:
    return {n: len(n) for n in names}

# ─── Optional（等价于 Union[T, None]）───
def find(id: int) -> Optional[str]:
    return "Alice" if id == 1 else None

# ─── Union（联合类型）───
def handle(v: Union[int, str, float]) -> str:
    return str(v)

# ─── Any（任意类型，跳过检查）───
def load(data: Any) -> str:
    return str(data)

# ─── Callable[[参数类型], 返回类型] ───
def apply(func: Callable[[int, int], int], a: int, b: int) -> int:
    return func(a, b)

# ─── TypeVar + Generic（泛型）───
T = TypeVar("T")                # 类型变量
K = TypeVar("K")
V = TypeVar("V")

def first(items: list[T]) -> T:
    return items[0]

class Stack(Generic[T]):
    def __init__(self):
        self._items: list[T] = []

    def push(self, item: T) -> None:
        self._items.append(item)

    def pop(self) -> T:
        return self._items.pop()

# ─── TypeVar 约束 ───
Number = TypeVar("Number", int, float)  # 只能是 int 或 float
def double(x: Number) -> Number:
    return x * 2

# ─── Literal（字面量类型）───
from typing import Literal
def set_mode(mode: Literal["train", "eval", "predict"]) -> None:
    print(f"模式: {mode}")

set_mode("train")    # OK
set_mode("unknown")  # mypy 报错

# ─── TypedDict（字典类型）───
class ModelConfig(TypedDict):
    model_name: str
    temperature: float
    max_tokens: int

config: ModelConfig = {
    "model_name": "gpt-4",
    "temperature": 0.7,
    "max_tokens": 2048,
}

# ─── Final（常量）───
API_VERSION: Final = "v1"
ENDPOINTS: Final[list[str]] = ["chat", "embed", "completion"]
# mypy 会阻止对 Final 变量重新赋值

# ─── Type（类型本身）───
def create_model(model_class: Type[BaseModel]) -> BaseModel:
    return model_class()
```

### 2.6.5 Protocol——结构类型（Structural Subtyping）

**下面是 Protocol——结构类型（Structural Subtyping） 的代码示例：**
```python
from typing import Protocol, runtime_checkable

class HasName(Protocol):
    """定义了 __name__ 属性的协议"""
    name: str

class Person:
    name: str = "Alice"

class Robot:
    name: str = "R2D2"

def greet(obj: HasName) -> str:
    return f"Hello {obj.name}"

# 无需继承！结构匹配
print(greet(Person()))  # "Hello Alice"
print(greet(Robot()))   # "Hello R2D2"

# ─── 方法协议 ───
@runtime_checkable  # 允许 isinstance 检查
class Runnable(Protocol):
    def run(self) -> None: ...

class Trainer:
    def run(self) -> None:
        print("training...")

class Evaluator:
    def run(self) -> None:
        print("evaluating...")

# 结构匹配
trainer: Runnable = Trainer()
evaluator: Runnable = Evaluator()

print(isinstance(trainer, Runnable))  # True（因为有 @runtime_checkable）
```

**Protocol vs ABC 选择：**

```python
# ABC（抽象基类）：需要显式继承
from abc import ABC, abstractmethod

class RunnableABC(ABC):
    @abstractmethod
    def run(self) -> None: ...

class Trainer(RunnableABC):  # 必须显式继承
    def run(self): ...

# Protocol：隐式结构匹配
class RunnableProto(Protocol):
    def run(self) -> None: ...

class Evaluator:  # 不需要继承！
    def run(self): ...

# AI 场景：Protocol 更适合处理多种模型的后端
class EmbeddingModel(Protocol):
    def embed(self, texts: list[str]) -> list[list[float]]: ...

class OpenAIEmbedding:
    def embed(self, texts):
        return [[0.1, 0.2] for _ in texts]

class LocalEmbedding:
    def embed(self, texts):
        return [[0.3, 0.4] for _ in texts]

# 两个类都可以作为 EmbeddingModel 使用，无需继承
models: list[EmbeddingModel] = [OpenAIEmbedding(), LocalEmbedding()]
```

### 2.6.6 泛型高级用法

**以下是 泛型用法 的进阶用法：**
```python
from typing import TypeVar, Generic, Sequence, TypeVarTuple, Self

# ─── 协变、逆变、不变（Covariance, Contravariance, Invariance）───
T_co = TypeVar("T_co", covariant=True)      # 协变（List[str] 是 List[object] 的子类型）
T_con = TypeVar("T_con", contravariant=True)  # 逆变

class Reader(Generic[T_co]):
    """读取器：协变——能读 str 的地方也能读 object？是的"""
    def read(self) -> T_co: ...

class Writer(Generic[T_con]):
    """写入器：逆变——能写 object 的地方也能写 str？是的"""
    def write(self, item: T_con) -> None: ...

# ─── Self 类型（Python 3.11+）───
from typing import Self

class Model:
    def set_name(self, name: str) -> Self:
        self.name = name
        return self  # 返回类型自动推断为当前类

class GPTModel(Model):
    pass

gpt = GPTModel().set_name("gpt-4")  # 类型推断为 GPTModel，不是 Model
```

### 2.6.7 `@overload`——函数重载（类型级别的重载）

**下面是 `@overload`——函数重载（类型级别的重载） 的代码示例：**
```python
from typing import overload

@overload
def process(data: str) -> str: ...
@overload
def process(data: list[str]) -> list[str]: ...
@overload
def process(data: dict) -> dict: ...

def process(data):
    """实际实现——运行时只有一个函数"""
    if isinstance(data, str):
        return data.upper()
    elif isinstance(data, list):
        return [d.upper() for d in data]
    elif isinstance(data, dict):
        return {k: v.upper() for k, v in data.items()}
    raise TypeError("不支持的类型")

# mypy 根据参数类型推断返回类型
result1: str = process("hello")        # OK
result2: list[str] = process(["a"])    # OK
result3: dict = process({"k": "v"})    # OK
```

### 2.6.8 运行时可用的类型工具

**下面是 运行时可用的类型工具 的代码示例：**
```python
from typing import get_type_hints, get_origin, get_args
import typing

def process(name: str, count: int = 0) -> str:
    return f"{name}: {count}"

# 获取完整类型注解（含前向引用解析）
print(get_type_hints(process))
# {'name': <class 'str'>, 'count': <class 'int'>, 'return': <class 'str'>}

# 解析泛型类型
from typing import List, Dict, Optional, Union
origin = get_origin(List[str])      # list
args = get_args(List[str])           # (str,)

origin = get_origin(Dict[str, int]) # dict
args = get_args(Dict[str, int])      # (str, int)

origin = get_origin(Optional[str])  # Union
args = get_args(Optional[str])       # (str, NoneType)

# 运行时判断类型关系
print(typing.get_origin(List[int]))  # list
```

### 2.6.9 常见错误

**错误 1：误以为类型注解会运行时检查**

```python
def add(a: int, b: int) -> int:
    return a + b

add("hello", "world")  # "helloworld" ← 运行正常！类型注解完全被忽略
```

**错误 2：`Optional[str]` 不等于 `str | None`（Python 3.10+ 原生支持）**

```python
# Python 3.10+ 可以使用：
def find(id: int) -> str | None:
    return "Alice" if id == 1 else None

# 等价于：
def find(id: int) -> Optional[str]:
    return "Alice" if id == 1 else None
```

**错误 3：可变默认参数的类型陷阱**

```python
from typing import List

def add_item(item: str, items: List[str] = []) -> List[str]:
    # mypy 不会捕获这个错误！
    items.append(item)
    return items
# 运行时所有调用共享同一个列表！
```

**错误 4：循环 import 和类型注解前向引用**

```python
# a.py
from __future__ import annotations  # 解决前向引用
class A:
    def get_b(self) -> "B": ...  # 字符串引用

# b.py
class B:
    pass
```

**错误 5：过分依赖 `Any`（放弃类型检查）**

```python
def process(data: Any) -> Any:
    return data  # 完全放弃类型检查

# 应该尽可能精确：
def process(data: dict[str, str]) -> dict[str, str]:
    return data
```

### 2.6.10 AI 场景案例：类型安全的 AI 配置

**下面是一个 类型安全的 AI 配置 的实战案例：**
```python
from typing import (
    TypedDict, Literal, Optional, Protocol,
    runtime_checkable, Final, overload,
)

class LLMConfig(TypedDict, total=False):
    """AI 配置的类型定义——提供 IDE 自动补全和静态检查"""
    model: str
    temperature: float
    max_tokens: int
    top_p: float
    frequency_penalty: float
    presence_penalty: float
    stop: list[str]
    api_key: str

# 使用：IDE 自动提示所有可用字段
config: LLMConfig = {
    "model": "gpt-4",
    "temperature": 0.7,
    "max_tokens": 2048,
}

@runtime_checkable
class EmbeddingFunction(Protocol):
    """嵌入函数的类型协议"""
    def __call__(self, texts: list[str]) -> list[list[float]]: ...

class OpenAIEmbedder:
    def __call__(self, texts: list[str]) -> list[list[float]]:
        return [[0.1] * 384 for _ in texts]

embedder: EmbeddingFunction = OpenAIEmbedder()
print(isinstance(embedder, EmbeddingFunction))  # True

# @overload 实现多态 AI 调用
@overload
def query(model: Literal["gpt-4"], prompt: str) -> str: ...

@overload
def query(model: Literal["gpt-4-vision"], prompt: str, image: str) -> str: ...

def query(model: str, prompt: str, image: Optional[str] = None):
    if model == "gpt-4":
        return f"[GPT-4] {prompt}"
    elif model == "gpt-4-vision":
        return f"[GPT-4-Vision] {prompt} (image: {image})"
    raise ValueError(f"未知模型: {model}")

result = query("gpt-4", "Hello")         # OK，返回 str
result = query("gpt-4-vision", "See", image="photo.jpg")  # OK
```

---

## 2.7 dataclass

### 2.7.1 一句话核心本质

**`@dataclass` 基于类注解自动生成 `__init__`、`__repr__`、`__eq__`、`__hash__` 等样板方法——把"纯数据容器"的定义从几十行 Java POJO 简化为几行 Python 声明。**

### 2.7.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 数据类定义 | 手写 POJO: 字段、getter/setter、equals、hashCode、toString | `@dataclass` + 类型注解：5 行 = 全部 |
| 不可变 | `record Foo(...)` 或手写 `final` 字段 | `@dataclass(frozen=True)` |
| 建造者模式 | Lombok `@Builder` | `replace()` 函数 + `field(default_factory=...)` |
| 继承 | 标准 OOP 继承 | dataclass 支持继承，但字段顺序需小心 |
| 默认值 | 在构造器中赋值 | 直接在字段上赋值，或 `field(default=...)` |
| 校验 | Bean Validation (`@NotNull`, `@Min`) | `__post_init__` + 自定验证 |
| 序列化 | Jackson / Gson 注解 | `asdict()`, `astuple()` 内置函数 |

### 2.7.3 技术原理：dataclass 生成的代码

`@dataclass` 是一个类装饰器，它在定义时读取类型注解并自动生成方法。以下两个类完全等价：

```python
from dataclasses import dataclass

@dataclass
class Point:
    x: float
    y: float
    label: str = "origin"

# 等价于手动编写：
class PointManual:
    def __init__(self, x: float, y: float, label: str = "origin"):
        self.x = x
        self.y = y
        self.label = label

    def __repr__(self):
        return f"PointManual(x={self.x!r}, y={self.y!r}, label={self.label!r})"

    def __eq__(self, other):
        if not isinstance(other, PointManual):
            return NotImplemented
        return (self.x, self.y, self.label) == (other.x, other.y, other.label)

    def __hash__(self):
        return hash((self.x, self.y, self.label))

# 验证
p1 = Point(1.0, 2.0)
p2 = PointManual(1.0, 2.0)
print(p1)        # Point(x=1.0, y=2.0, label='origin')
print(p1 == p2)  # True（字段值相同即可，类型不同也是 True！）
```

**dataclass 工作原理（`Lib/dataclasses.py`）：**

```
@dataclass 执行流程：

1. 读取类属性注解（__annotations__）
2. 按声明顺序排列字段
3. 生成 __init__ 方法
   - 所有无默认值的字段优先（必须传入）
   - 有默认值的字段在后
4. 生成 __repr__（每个字段的 repr）
5. 生成 __eq__（所有字段的元组比较）
6. 生成 __hash__（若 eq=True 且 frozen=True）
7. 可选生成 __lt__, __le__, __gt__, __ge__（order=True）
8. 执行 __post_init__（如果定义了）
```

### 2.7.4 基本用法

**以下是基本用法示例：**
```python
from dataclasses import dataclass, field, asdict, astuple, replace

@dataclass
class AIConfig:
    # 字段声明（类型注解 → 自动 __init__ 参数）
    model_name: str                         # 必需，无默认值
    temperature: float = 0.7                # 可选，有默认值
    max_tokens: int = 2048                  # 可选
    api_key: str = field(default="", repr=False)  # repr=False 敏感信息不打印
    tags: list[str] = field(default_factory=list)  # 可变默认值

    def __post_init__(self):
        """初始化后自动调用的验证钩子"""
        if not 0 <= self.temperature <= 2:
            raise ValueError(f"temperature 必须在 0-2 之间, 收到 {self.temperature}")
        if self.max_tokens < 1:
            raise ValueError("max_tokens 必须 > 0")

# 创建
config = AIConfig(
    model_name="gpt-4",
    temperature=0.8,
    tags=["llm", "chat"],
)
print(config)
# AIConfig(model_name='gpt-4', temperature=0.8, max_tokens=2048, tags=['llm', 'chat'])

# 转为 dict
print(asdict(config))
# {'model_name': 'gpt-4', 'temperature': 0.8, 'max_tokens': 2048,
#  'api_key': '', 'tags': ['llm', 'chat']}

# 转为 tuple
print(astuple(config))
# ('gpt-4', 0.8, 2048, '', ['llm', 'chat'])

# 创建副本并修改部分字段
config2 = replace(config, temperature=0.5)
print(config2.temperature)  # 0.5
print(config.temperature)   # 0.8 ← 原对象不变
```

### 2.7.5 高级选项

**以下是 选项 的进阶用法：**
```python
@dataclass(frozen=True)  # 不可变（所有字段自动 final）
class FrozenConfig:
    model: str
    version: int

cfg = FrozenConfig("gpt-4", 1)
# cfg.model = "gpt-5"  # FrozenInstanceError: cannot assign to field 'model'

@dataclass(order=True)   # 自动生成比较运算符
class Version:
    major: int
    minor: int

v1 = Version(2, 0)
v2 = Version(3, 0)
print(v1 < v2)   # True ← 自动 __lt__
print(v1 >= v2)  # False

@dataclass(slots=True)  # Python 3.10+：使用 __slots__ 节省内存
class Efficient:
    x: int
    y: int
# 无 __dict__，内存节省 30-50%

@dataclass(unsafe_hash=True)  # 强制生成 __hash__（即使 frozen=False）
class MutablePoint:
    x: int
    y: int
    # 谨慎使用：可变对象应该不可哈希
```

### 2.7.6 dataclass 字段详解

**下面是 dataclass 字段详解 的代码示例：**
```python
from dataclasses import field, InitVar

@dataclass
class Dataset:
    name: str

    # field() 参数详解
    path: str = field(
        default="./data",           # 默认值
        default_factory=None,       # 默认值工厂（和 default 只能选一个）
        init=True,                  # 是否作为 __init__ 参数
        repr=True,                  # 是否出现在 __repr__ 中
        hash=True,                  # 是否参与 __hash__ 计算
        compare=True,               # 是否参与比较
        metadata={"unit": "MB"},    # 附加元数据
    )

    # InitVar：只用于 __init__ 和 __post_init__，不作为字段
    raw_data: InitVar[str] = None

    # 在 __post_init__ 中处理 InitVar
    def __post_init__(self, raw_data):
        if raw_data:
            self.name = f"{self.name}_{len(raw_data)}"

# field() 的 metadata 可在运行时通过 fields() 获取
from dataclasses import fields
for f in fields(Dataset):
    print(f.name, f.metadata)
# name {}
# path {'unit': 'MB'}
```

### 2.7.7 dataclass 继承

**下面是 dataclass 继承 的代码示例：**
```python
@dataclass
class BaseModel:
    name: str
    version: str = "1.0"

@dataclass
class LLMModel(BaseModel):
    model_type: str = "LLM"           # OK：新增字段
    temperature: float = 0.7          # OK：新增字段
    # name: str = "default"           # 错误：不能覆盖已有字段的默认值
    # 如果子类字段有默认值而父类字段无默认值，会导致参数顺序问题

# Python 3.10+ 解决方案：只在子类中添加无默认值字段
@dataclass
class VisionModel(BaseModel):
    vision_type: str                 # 无默认值，在 name 之后
    image_size: int = 512            # 有默认值

# 参数顺序：name(基类必需) → vision_type(子类必需) → version(基类可选) → image_size(子类可选)
vm = VisionModel("gpt-4-vision", "detection", image_size=1024)
```

### 2.7.8 `@dataclass` vs NamedTuple vs TypedDict

| 特性 | `@dataclass` | `NamedTuple` | `TypedDict` |
|------|-------------|-------------|-------------|
| 可变 | 是（frozen=True 可不可变） | 元组（不可变） | dict（可变） |
| 方法 | 可添加方法 | 可添加方法 | 纯字典 |
| 继承 | 支持 OOP 继承 | 支持元组继承 | dict 继承 |
| 内存 | 普通对象（有 `__dict__`） | 紧凑元组 | dict（和 dict 一样） |
| 性能 | 一般 | 快速 | 快速 |
| 用途 | 复杂业务数据 | 简单不可变数据 | JSON/dict 结构 |

### 2.7.9 常见错误

**错误 1：可变默认值**

```python
@dataclass
class Wrong:
    items: list = []  # 所有实例共享同一列表！

a = Wrong()
b = Wrong()
a.items.append("x")
print(b.items)  # ['x']  ← 受到了影响！

@dataclass
class Right:
    items: list = field(default_factory=list)  # 每次创建新列表
```

**错误 2：继承时字段顺序冲突**

```python
@dataclass
class Base:
    x: int = 0

# @dataclass    # 取消注释会报错
# class Child(Base):
#     y: str     # 非默认字段在默认字段之后 → TypeError: non-default argument follows default argument
```

**错误 3：`frozen=True` 时修改嵌套可变对象**

```python
@dataclass(frozen=True)
class Config:
    tags: list

c = Config(["a", "b"])
# c.tags = ["c"]          # 错误：frozen
c.tags.append("c")        # 成功！list 本身是可变的
# frozen 只阻止字段重新赋值，不阻止对象内部修改
```

**错误 4：`__post_init__` 定义错误**

```python
@dataclass
class Test:
    x: int

    # def __post_init__(self, extra):  # 签名不匹配！
    #     pass

    def __post_init__(self):  # 正确：只接收 self
        if self.x < 0:
            raise ValueError("x must be >= 0")
```

### 2.7.10 AI 场景案例

**下面是 AI 场景的实战案例：**
```python
from dataclasses import dataclass, field, asdict
from typing import Optional
import json

@dataclass
class LLMRequest:
    """AI 请求数据类——完整的 API 参数模型"""
    model: str
    messages: list[dict]
    temperature: float = 0.7
    max_tokens: int = 1024
    top_p: float = 1.0
    frequency_penalty: float = 0.0
    presence_penalty: float = 0.0
    stop: Optional[list[str]] = None
    api_key: str = field(default="", repr=False)

    def __post_init__(self):
        if not 0 <= self.temperature <= 2:
            raise ValueError(f"temperature 超出范围: {self.temperature}")
        if self.max_tokens < 1:
            raise ValueError("max_tokens 必须为正数")

    def to_openai_body(self) -> dict:
        """转换为 OpenAI API 请求体"""
        body = {
            "model": self.model,
            "messages": self.messages,
            "temperature": self.temperature,
            "max_tokens": self.max_tokens,
        }
        if self.stop:
            body["stop"] = self.stop
        return body

    def to_json(self) -> str:
        return json.dumps(asdict(self), ensure_ascii=False)

@dataclass
class LLMResponse:
    """AI 响应数据类——解析和缓存 LLM 返回结果"""
    text: str
    finish_reason: str = "stop"
    usage: dict = field(default_factory=dict)
    latency_ms: float = 0.0
    cached: bool = False
    model: str = ""

    def tokens_per_second(self) -> float:
        if self.latency_ms <= 0:
            return 0.0
        return len(self.text.split()) / (self.latency_ms / 1000)

    def __bool__(self):
        return bool(self.text)

@dataclass
class TrainingExample:
    """训练数据样本——自动计算字段"""
    input_text: str
    target_text: str
    domain: str = "general"
    weight: float = 1.0

    def __post_init__(self):
        self.token_count = len(self.input_text.split())
        self.input_length = len(self.input_text)

# 使用
request = LLMRequest(
    model="gpt-4",
    messages=[{"role": "user", "content": "Hello"}],
    temperature=0.8,
)
print(request.to_json())
# {"model": "gpt-4", "messages": [...], "temperature": 0.8, ...}
```

---

## 2.8 上下文管理器

### 2.8.1 一句话核心本质

**上下文管理器通过 `__enter__` 和 `__exit__` 协议封装资源的获取与释放，`with` 语句确保无论是否发生异常，资源都能被正确释放——等价于 Java 的 try-with-resources。**

### 2.8.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 语法 | `try (Resource r = new Resource()) { ... }` | `with Resource() as r: ...` |
| 资源接口 | `AutoCloseable` / `Closeable`（`close()` 方法） | `__enter__` + `__exit__`（无需继承特定基类） |
| 异常处理 | try-with-resources 自动抑制异常（Suppressed Exceptions） | `__exit__` 接收异常参数，可选择处理或忽略 |
| 多个资源 | `try (A a = ...; B b = ...)` | `with A() as a, B() as b:` |
| 生成器风格 | 不支持 | `@contextmanager` + `yield` 生成器实现 |
| 异步 | `CompletableFuture` 链式 | `async with` + `__aenter__` / `__aexit__` |

**本质区别：** Java 的 try-with-resources 是异常处理机制的扩展，`close()` 在 finally 块中被调用。Python 的 `with` 语句是独立语法特性，通过协议方法完全封装资源生命周期——包括获取、使用、清理三个阶段的完整控制。

### 2.8.3 技术原理：with 语句执行流程

```
with EXPR as VAR:
    BLOCK

等价于：

1. manager = EXPR              # 获取上下文管理器对象
2. VAR = manager.__enter__()    # 进入：获取资源，返回给 as
3. try:
       BLOCK                   # 执行代码块
   except Exception as e:
       if manager.__exit__(type(e), e, e.__traceback__):
           pass  # __exit__ 返回 True → 抑制异常
       else:
           raise  # __exit__ 返回 False → 继续传播异常
   else:
       manager.__exit__(None, None, None)  # 正常退出
   finally:  # 注意：这是简化的表示
       # __exit__ 在 finally 中保证执行
```

**`__exit__` 的三个参数和返回值：**

```python
def __exit__(self, exc_type, exc_val, exc_tb):
    """
    exc_type: 异常类型（如 ValueError）或 None
    exc_val:  异常实例或 None
    exc_tb:   回溯对象（traceback）或 None

    返回值：
    True  → 抑制异常（with 块中不会抛出）
    False → 继续传播异常（默认行为）
    """
    if exc_type is not None:
        print(f"发生异常: {exc_type.__name__}: {exc_val}")
    return False  # 不抑制，异常继续传播
```

### 2.8.4 基本用法：实现上下文管理器

**方式一：基于类（`__enter__` + `__exit__`）**

```python
class ManagedFile:
    def __init__(self, path: str, mode: str = "r"):
        self.path = path
        self.mode = mode

    def __enter__(self):
        self.file = open(self.path, self.mode, encoding="utf-8")
        return self.file  # as 子句接收这个返回值

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.file:
            self.file.close()
        # 返回 False 或 None → 异常继续传播
        # 返回 True → 抑制异常
        if exc_type is FileNotFoundError:
            print(f"文件不存在: {self.path}")
            return True  # 抑制 FileNotFoundError
        return False  # 其他异常继续传播

# 使用
with ManagedFile("test.txt", "w") as f:
    f.write("Hello World")
# 文件自动关闭，无需显式 close()

with ManagedFile("不存在.txt") as f:
    content = f.read()
# 打印 "文件不存在: 不存在.txt"，但不会抛出异常
```

**方式二：基于 `@contextmanager` 生成器**

```python
from contextlib import contextmanager

@contextmanager
def managed_file(path: str, mode: str = "r"):
    """使用 contextmanager 装饰器的生成器版本"""
    try:
        file = open(path, mode, encoding="utf-8")
        yield file  # → as 子句接收
    finally:
        file.close()
        print("文件已关闭")

# @contextmanager 的原理：
# 1. 获取生成器对象
# 2. next(gen) → 执行到 yield，将 yield 的值返回给 as
# 3. with 块执行
# 4. 无论异常与否，调用 gen.throw() 或 next(gen) 继续执行 finally
# 5. close() 生成器
```

### 2.8.5 contextlib 高级工具

**以下是 contextlib 工具 的进阶用法：**
```python
from contextlib import (
    contextmanager,
    closing,
    suppress,
    redirect_stdout,
    redirect_stderr,
    ExitStack,
    nullcontext,
)

# ─── closing：自动调用 close() 方法 ───
from contextlib import closing
from urllib.request import urlopen

with closing(urlopen("https://api.openai.com")) as response:
    data = response.read()
# 退出时自动调用 response.close()

# ─── suppress：忽略指定异常 ───
from contextlib import suppress

with suppress(FileNotFoundError):
    os.remove("temp.json")  # 文件不存在也不报错

# 等价于：
try:
    os.remove("temp.json")
except FileNotFoundError:
    pass

# ─── redirect_stdout：临时重定向输出 ───
import io
from contextlib import redirect_stdout

buf = io.StringIO()
with redirect_stdout(buf):
    print("这句话会写入 buf")
    print("这句话也会")
output = buf.getvalue()
print(repr(output))  # "这句话会写入 buf\n这句话也会\n"

# ─── ExitStack：动态管理多个上下文 ───
from contextlib import ExitStack

def open_many(files: list[str]) -> ExitStack:
    """动态打开任意数量文件，按顺序关闭"""
    stack = ExitStack()
    for path in files:
        f = open(path, "w")
        stack.enter_context(f)  # 动态注册上下文管理器
    return stack

with open_many(["a.txt", "b.txt", "c.txt"]) as stack:
    # 所有文件自动管理
    pass  # 退出时按逆序关闭

# ─── nullcontext：条件性上下文 ───
from contextlib import nullcontext

def process(need_profiling: bool):
    ctx = cProfile.Profile() if need_profiling else nullcontext()
    with ctx:
        do_work()
```

### 2.8.6 嵌套上下文管理器

**下面是 嵌套上下文管理器 的代码示例：**
```python
# 多个 with 语句等价于嵌套：
with open("a.txt") as f1, open("b.txt") as f2:
    data1 = f1.read()
    data2 = f2.read()

# 等价于：
with open("a.txt") as f1:
    with open("b.txt") as f2:
        data1 = f1.read()
        data2 = f2.read()

# Python 3.10+：括号支持换行
with (
    open("a.txt") as f1,
    open("b.txt") as f2,
    open("c.txt") as f3,
):
    data = f1.read() + f2.read() + f3.read()
```

### 2.8.7 异步上下文管理器

**下面是 异步上下文管理器 的代码示例：**
```python
import asyncio

class AsyncResource:
    async def __aenter__(self):
        print("异步获取资源")
        await asyncio.sleep(0.1)
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        print("异步释放资源")
        await asyncio.sleep(0.1)

async def main():
    async with AsyncResource() as res:
        print("使用资源")

asyncio.run(main())
```

### 2.8.8 常见错误

**错误 1：`__exit__` 返回值误解**

```python
class Suppressor:
    def __exit__(self, *args):
        # 返回 True → 抑制异常！
        return True

with Suppressor():
    raise ValueError("something wrong")
print("这里会执行！")  # ← 异常被抑制了！

# 只应在明确知道需要抑制异常时返回 True
```

**错误 2：`@contextmanager` 中忘记 try/finally**

```python
@contextmanager
def wrong():
    yield open("file.txt")  # 如果这里抛出异常，文件不会关闭！

@contextmanager
def correct():
    f = open("file.txt")
    try:
        yield f
    finally:
        f.close()  # 确保关闭
```

**错误 3：在 `with` 块外使用资源**

```python
with open("data.txt") as f:
    data = f.read()
# 文件已关闭
# print(f.read())  # ValueError: I/O operation on closed file
```

**错误 4：混淆上下文管理器和迭代器**

```python
# 上下文管理器是 with，迭代器是 for
# 两者不同！不要混淆

# 生成器 + @contextmanager 的 yield 是特例
# 普通生成器不能用 with
```

### 2.8.9 AI 场景案例

**场景一：AI 模型推理计时 + 资源追踪**

```python
import time
from contextlib import contextmanager

@contextmanager
def track_inference(model_name: str):
    """追踪 AI 推理耗时和资源使用"""
    start = time.perf_counter()
    import tracemalloc
    tracemalloc.start()

    yield  # 推理代码在这里执行

    current, peak = tracemalloc.get_traced_memory()
    elapsed = time.perf_counter() - start
    print(f"[{model_name}]")
    print(f"  耗时: {elapsed*1000:.1f}ms")
    print(f"  当前内存: {current/1024:.1f}KB")
    print(f"  峰值内存: {peak/1024:.1f}KB")
    tracemalloc.stop()

with track_inference("gpt-4"):
    result = model.generate(prompt)
```

**场景二：管理 API 密钥和认证上下文**

```python
import os
from contextlib import contextmanager

@contextmanager
def openai_api_key(key: str):
    """临时设置 OpenAI API 密钥，退出时恢复"""
    original = os.environ.get("OPENAI_API_KEY")
    os.environ["OPENAI_API_KEY"] = key
    try:
        yield
    finally:
        if original:
            os.environ["OPENAI_API_KEY"] = original
        else:
            del os.environ["OPENAI_API_KEY"]

# 使用：临时切换 API 密钥
with openai_api_key("sk-temporary-key"):
    result = openai.ChatCompletion.create(...)
# 退出后自动恢复原始密钥
```

**场景三：数据库事务管理**

```python
class TransactionManager:
    def __init__(self, db):
        self.db = db
        self.tx = None

    def __enter__(self):
        self.tx = self.db.begin_transaction()
        return self.tx

    def __exit__(self, exc_type, exc_val, exc_tb):
        if exc_type is None:
            self.tx.commit()  # 无异常 → 提交
        else:
            self.tx.rollback()  # 有异常 → 回滚
        return False  # 不抑制异常

# 使用
db = Database()
with TransactionManager(db) as tx:
    tx.insert("users", {"name": "Alice"})
    tx.update("stats", {"count": 1})
# 自动提交或回滚
```

**场景四：训练过程中的临时环境变量**

```python
from contextlib import ExitStack

def train_model(config: dict):
    """使用 ExitStack 同时管理多个上下文"""
    with ExitStack() as stack:
        # 设置临时环境变量
        if "api_key" in config:
            stack.enter_context(openai_api_key(config["api_key"]))

        # 设置运行目录
        if "work_dir" in config:
            original_cwd = os.getcwd()
            os.chdir(config["work_dir"])
            stack.callback(lambda: os.chdir(original_cwd))

        # 追踪性能
        if config.get("profiling"):
            stack.enter_context(track_inference(config["model"]))

        # 执行训练
        model.train()
    # 退出 ExitStack 时，所有上下文按逆序清理
```

---

# 第三阶段：并发与网络

---

## 3.1 threading

### 3.1.1 一句话核心本质

**Python 线程是操作系统原生（POSIX 线程 / Windows 线程），但由于 CPython 的 GIL，同一时刻只有一个线程能执行 Python 字节码——多线程适合 I/O 密集型任务，不适用于 CPU 密集型。**

### 3.1.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 线程模型 | 原生线程 + 线程池（`ThreadPoolExecutor`） | 原生线程，但 GIL 限制并行 |
| CPU 利用 | 多线程 → 多核并行 | 多线程 → 只能跑一核（GIL） |
| I/O 并发 | 线程阻塞时让出 CPU | 线程阻塞时释放 GIL，其他线程可执行 |
| 内存 | 共享内存，需显式同步（`synchronized`） | 共享内存，`threading.Lock` |
| 线程池 | `Executors.newFixedThreadPool(n)` | `concurrent.futures.ThreadPoolExecutor` |
| 守护线程 | `setDaemon(true)` | `thread.daemon = True` |

**本质区别：** Java 线程能真正并行（多核多线程同时执行），Python 线程因为 GIL 同一时刻只能执行一个。但 CPython 的 GIL 在 I/O 操作时会主动释放，因此 Python 多线程对 I/O 密集型任务仍然有效。

### 3.1.3 技术原理：GIL 详解

```
GIL（Global Interpreter Lock）的工作机制：

时间线（多线程交错执行，一秒内交替多次）：

Thread A  ██████░░░░████░░░░████░░░░████░░░░
Thread B  ░░░░████░░░░████░░░░████░░░░████░░
          ──────────────────────────────────→ 时间

GIL 切换条件：
1. 每执行 100 条字节码（sys.getswitchinterval()，默认 5ms）
2. I/O 操作前主动释放
3. C 扩展代码可手动释放（如 NumPy 在计算时释放 GIL）

GIL 释放时机：

┌──────────────────────────────────────────────────┐
│ Python 字节码执行                    │ GIL 持有状态 │
├──────────────────────────────────────┼─────────────┤
│ a = 1 + 2    ← 纯 Python 计算        │ 持有        │
│ time.sleep(1)  ← 主动释放            │ 释放        │
│ file.read()   ← I/O 操作前释放       │ 释放        │
│ numpy.dot()   ← C 扩展释放 GIL       │ 释放        │
│ a + b         ← 返回 Python 重新获取 │ 持有        │
└──────────────────────────────────────┴─────────────┘

CPython 的 GIL 实现（ceval.c 中的 take_gil/release_gil）：
实际上是一个条件变量（Condition Variable）+ 互斥锁
线程通过轮换时间片来交替执行
```

### 3.1.4 基础用法

**以下是基本用法示例：**
```python
import threading
import time

def worker(name: str, delay: float):
    """线程工作函数"""
    print(f"[{name}] 启动")
    for i in range(3):
        time.sleep(delay)  # I/O 等待时释放 GIL
        print(f"[{name}] 步骤 {i}")
    print(f"[{name}] 结束")

# ─── 创建和启动线程 ───
threads = []
for i in range(3):
    t = threading.Thread(
        target=worker,
        args=(f"T{i}", 0.5),
        daemon=False,  # 非守护线程：主线程等待它结束
    )
    t.start()
    threads.append(t)

# ─── 等待所有线程 ───
for t in threads:
    t.join()  # 阻塞直到线程结束

print("所有线程完成")

# ─── 守护线程 ───
daemon_thread = threading.Thread(target=lambda: time.sleep(10), daemon=True)
daemon_thread.start()
# 主线程退出时守护线程自动结束
```

### 3.1.5 线程同步

**下面是 线程同步 的代码示例：**
```python
import threading
from threading import Lock, RLock, Event, Condition, Semaphore, Barrier

# ─── Lock（互斥锁）───
lock = Lock()
counter = 0

def increment():
    global counter
    for _ in range(100000):
        with lock:  # 等价于 lock.acquire() / lock.release()
            counter += 1

t1 = threading.Thread(target=increment)
t2 = threading.Thread(target=increment)
t1.start(); t2.start()
t1.join(); t2.join()
print(counter)  # 200000（正确）

# ─── RLock（可重入锁：同一线程可多次 acquire）───
rlock = RLock()

def recursive_lock(n: int):
    with rlock:  # 同一线程再次进入不会死锁
        if n > 0:
            recursive_lock(n - 1)

# ─── Event（事件通知）───
event = Event()

def waiter():
    print("等待事件...")
    event.wait()  # 阻塞直到 event.set()
    print("收到事件！继续执行")

def setter():
    time.sleep(2)
    print("发送事件...")
    event.set()

threading.Thread(target=waiter).start()
threading.Thread(target=setter).start()

# ─── Semaphore（信号量：限制并发数）───
sem = Semaphore(3)  # 最多 3 个线程同时访问

def limited_access(id: int):
    with sem:
        print(f"线程 {id} 进入临界区")
        time.sleep(1)
        print(f"线程 {id} 离开")

# ─── Barrier（屏障：等待所有线程到达某点）───
barrier = Barrier(3)  # 3 个线程到齐后才继续

def rendezvous(id: int):
    print(f"线程 {id} 到达屏障")
    barrier.wait()  # 阻塞直到 3 个线程都到这里
    print(f"线程 {id} 通过屏障")

# ─── ThreadLocal（线程局部变量）───
thread_local = threading.local()

def set_and_print(value):
    thread_local.value = value
    print(f"{threading.current_thread().name}: {thread_local.value}")

# 每个线程有自己的 thread_local.value
```

### 3.1.6 concurrent.futures（高级线程池）

**以下是 concurrent.futures（线程池） 的进阶用法：**
```python
from concurrent.futures import ThreadPoolExecutor, as_completed
import time

def fetch_url(url: str) -> tuple[str, int]:
    """模拟 HTTP 请求"""
    time.sleep(1)  # I/O 等待
    return url, len(url) * 10

# ─── 线程池 ───
with ThreadPoolExecutor(max_workers=4) as executor:
    urls = ["https://api.openai.com", "https://api.anthropic.com", "https://api.google.com"]

    # map：按顺序返回结果
    results = executor.map(fetch_url, urls)
    for url, size in results:
        print(f"{url}: {size}")

    # submit + as_completed：按完成顺序返回
    futures = {executor.submit(fetch_url, url): url for url in urls}
    for future in as_completed(futures):
        url, size = future.result()
        print(f"[完成] {url}: {size}")
```

### 3.1.7 常见错误

**错误 1：没有用锁保护共享变量**

```python
counter = 0
def wrong():
    global counter
    for _ in range(100000):
        counter += 1  # 非原子操作！可能丢失更新

# counter += 1 实际是 3 条字节码：
# LOAD counter → 加载
# LOAD_CONST 1 → 加载常量
# INPLACE_ADD → 相加
# STORE counter → 写回
# GIL 可能在中间切换，导致多个线程读到同一个值
```

**错误 2：死锁**

```python
lock_a = Lock()
lock_b = Lock()

def deadlock1():
    with lock_a:
        time.sleep(0.1)  # 给另一个线程时间获取 lock_b
        with lock_b:     # 此时 lock_b 已被 deadlock2 持有
            pass

def deadlock2():
    with lock_b:
        with lock_a:     # lock_a 被 deadlock1 持有
            pass
```

**错误 3：在回调中修改 UI 或输出（Python 多线程是真实线程）**

```python
import sys

def print_safe(*args):
    """多线程安全打印：加锁"""
    with threading.Lock():
        print(*args, file=sys.stdout)
```

### 3.1.8 AI 场景案例：异步 AI 请求队列

**下面是一个 异步 AI 请求队列 的实战案例：**
```python
import threading
import queue
import time
from dataclasses import dataclass
from typing import Optional

@dataclass
class AIRequest:
    prompt: str
    model: str = "gpt-4"
    temperature: float = 0.7
    max_tokens: int = 1024

@dataclass
class AIResponse:
    text: str
    latency: float
    success: bool

class AIWorkerPool:
    """AI 请求线程池：管理并发 LLM 调用的工作线程"""

    def __init__(self, api_key: str, num_workers: int = 3):
        self.api_key = api_key
        self.request_queue: queue.Queue[AIRequest] = queue.Queue()
        self.response_queue: queue.Queue[AIResponse] = queue.Queue()
        self._running = True

        # 创建工作线程
        self.workers = []
        for i in range(num_workers):
            t = threading.Thread(
                target=self._worker_loop,
                args=(i,),
                daemon=True,
            )
            t.start()
            self.workers.append(t)

    def _worker_loop(self, worker_id: int):
        """工作线程主循环：从队列取请求，调用 LLM API"""
        while self._running:
            try:
                request = self.request_queue.get(timeout=1.0)
            except queue.Empty:
                continue

            start = time.perf_counter()
            try:
                # 模拟 LLM API 调用
                result = self._call_llm(request)
                latency = time.perf_counter() - start
                self.response_queue.put(AIResponse(
                    text=result, latency=latency, success=True
                ))
            except Exception as e:
                self.response_queue.put(AIResponse(
                    text=str(e), latency=0, success=False
                ))
            finally:
                self.request_queue.task_done()

    def _call_llm(self, request: AIRequest) -> str:
        """实际的 LLM API 调用（模拟）"""
        import requests
        response = requests.post(
            "https://api.openai.com/v1/chat/completions",
            headers={"Authorization": f"Bearer {self.api_key}"},
            json={
                "model": request.model,
                "messages": [{"role": "user", "content": request.prompt}],
                "temperature": request.temperature,
                "max_tokens": request.max_tokens,
            },
            timeout=30,
        )
        response.raise_for_status()
        return response.json()["choices"][0]["message"]["content"]

    def submit(self, request: AIRequest):
        """提交请求到队列"""
        self.request_queue.put(request)

    def results(self, timeout: Optional[float] = None) -> list[AIResponse]:
        """收集所有结果"""
        self.request_queue.join()  # 等待所有请求完成
        responses = []
        while not self.response_queue.empty():
            responses.append(self.response_queue.get_nowait())
        return responses

    def shutdown(self):
        self._running = False
        for w in self.workers:
            w.join(timeout=2.0)

# 使用
pool = AIWorkerPool("sk-xxx", num_workers=5)
for i in range(10):
    pool.submit(AIRequest(prompt=f"帮我写一首诗 #{i}"))
responses = pool.results()
for r in responses:
    print(f"[{'✓' if r.success else '✗'}] {r.text[:50]}... ({r.latency:.2f}s)")
pool.shutdown()
```

---

## 3.2 multiprocessing

### 3.2.1 一句话核心本质

**通过创建子进程绕过 GIL，每个进程拥有独立 Python 解释器和独立内存空间，能真正利用多核 CPU。进程间通信（IPC）比线程间共享内存更重，但有完全隔离的安全性。**

### 3.2.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 多核利用 | 多线程自然利用多核 | 必须用 `multiprocessing` 绕开 GIL |
| 进程创建 | `ProcessBuilder` / `Runtime.exec()` | `multiprocessing.Process` |
| 进程池 | `ForkJoinPool` / `ThreadPoolExecutor` | `multiprocessing.Pool` / `concurrent.futures.ProcessPoolExecutor` |
| 内存模型 | 线程共享堆内存 | 进程完全隔离（需序列化传递数据） |
| 通信方式 | 共享内存 + `volatile` + `synchronized` | `Queue`, `Pipe`, `Manager`, `shared_memory` |
| 序列化 | 不需要（共享堆） | pickle 序列化所有传递的数据 |

**本质区别：** Java 多线程直接共享堆内存，通信几乎零开销。Python 多进程需要 pickle 序列化所有传递的数据（参数、返回值），通信代价显著。选择时权衡：CPU 密集→multiprocessing，I/O 密集→threading/asyncio。

### 3.2.3 技术原理：进程与 GIL

```
单线程（无并行）：
┌─────────────────────────────────┐
│  CPython 进程                    │
│  ┌───┐ ┌───┐ ┌───┐             │
│  │ 线程1 │ │ 线程2 │ │ 线程3 │  GIL 只 │
│  └───┘ └───┘ └───┘ 允许一个 │
│  ──→ 总 CPU 使用率 ≈ 1 核    │
└─────────────────────────────────┘

多进程（真正并行）：
┌─────────────────────────────────┐
│  CPython 进程 1（CPU 0）         │
│  ┌───┐                          │
│  │ 线程1 │ 独立 GIL              │
│  └───┘                          │
├─────────────────────────────────┤
│  CPython 进程 2（CPU 1）         │
│  ┌───┐                          │
│  │ 线程2 │ 独立 GIL              │
│  └───┘                          │
├─────────────────────────────────┤
│  CPython 进程 3（CPU 2）         │
│  ┌───┐                          │
│  │ 线程3 │ 独立 GIL              │
│  └───┘                          │
│  ──→ 总 CPU 使用率 ≈ N 核       │
└─────────────────────────────────┘

进程间通信方式对比：
┌────────────────────────────────────────────────────┐
│ 方式            │ 速度   │ 复杂度 │ 数据量           │
├─────────────────┼────────┼────────┼─────────────────┤
│ Queue / Pipe    │ 中等   │ 低     │ 中小数据         │
│ Manager         │ 慢     │ 低     │ 复杂对象         │
│ shared_memory   │ 极快   │ 高     │ 大数据（numpy）  │
│ Socket          │ 中     │ 中     │ 网络通信         │
│ File            │ 慢     │ 低     │ 大数据持久化      │
└────────────────────────────────────────────────────┘
```

### 3.2.4 基本用法

**以下是基本用法示例：**
```python
import multiprocessing as mp
import time
import os

def cpu_heavy(n: int) -> int:
    """CPU 密集型计算——会利用 100% 的 CPU 核"""
    pid = os.getpid()
    print(f"进程 {pid} 开始计算 n={n}")
    result = sum(i * i for i in range(n))
    print(f"进程 {pid} 完成")
    return result

if __name__ == "__main__":
    # ─── Pool（进程池）───
    with mp.Pool(processes=4) as pool:
        # map：阻塞，按顺序返回
        results = pool.map(cpu_heavy, [10**7, 2*10**7, 3*10**7])
        print(results)

        # map_async：非阻塞
        async_result = pool.map_async(cpu_heavy, [10**7, 2*10**7])
        async_result.wait()  # 等待完成
        print(async_result.get())

        # apply：单任务
        result = pool.apply(cpu_heavy, args=(10**7,))

    # ─── Process（手动管理）───
    p = mp.Process(target=cpu_heavy, args=(10**7,))
    p.start()
    p.join()
```

### 3.2.5 进程通信

**下面是 进程通信 的代码示例：**
```python
import multiprocessing as mp

# ─── Queue（队列：生产者-消费者模式）───
def producer(q: mp.Queue):
    for i in range(5):
        q.put(f"消息 {i}")
    q.put(None)  # 哨兵：结束信号

def consumer(q: mp.Queue):
    while True:
        msg = q.get()
        if msg is None:
            break
        print(f"收到: {msg}")

if __name__ == "__main__":
    q = mp.Queue()
    p1 = mp.Process(target=producer, args=(q,))
    p2 = mp.Process(target=consumer, args=(q,))
    p1.start(); p2.start()
    p1.join(); p2.join()

# ─── Pipe（管道：双向通信）───
def worker(conn):
    conn.send("子进程数据")
    data = conn.recv()
    print(f"子进程收到: {data}")
    conn.close()

if __name__ == "__main__":
    parent_conn, child_conn = mp.Pipe()
    p = mp.Process(target=worker, args=(child_conn,))
    p.start()
    print(f"主进程收到: {parent_conn.recv()}")
    parent_conn.send("回复")
    p.join()

# ─── Manager（共享复杂对象）───
def manager_worker(d, l):
    d["key"] = "value"
    l.append(42)

if __name__ == "__main__":
    with mp.Manager() as manager:
        shared_dict = manager.dict()
        shared_list = manager.list()

        p = mp.Process(target=manager_worker, args=(shared_dict, shared_list))
        p.start()
        p.join()

        print(shared_dict)  # {"key": "value"}
        print(shared_list)  # [42]

# ─── SharedMemory（Python 3.8+：零拷贝共享）───
from multiprocessing import shared_memory
import numpy as np

def shm_worker(shm_name: str, shape, dtype):
    shm = shared_memory.SharedMemory(name=shm_name)
    arr = np.ndarray(shape, dtype=dtype, buffer=shm.buf)
    arr *= 2  # 直接操作共享内存

if __name__ == "__main__":
    arr = np.array([1, 2, 3, 4], dtype=np.float64)
    shm = shared_memory.SharedMemory(create=True, size=arr.nbytes)
    shared_arr = np.ndarray(arr.shape, dtype=arr.dtype, buffer=shm.buf)
    shared_arr[:] = arr[:]

    p = mp.Process(target=shm_worker, args=(shm.name, arr.shape, arr.dtype))
    p.start()
    p.join()
    print(shared_arr)  # [2. 4. 6. 8.]
    shm.close()
    shm.unlink()
```

### 3.2.6 concurrent.futures（高级进程池）

**以下是 concurrent.futures（进程池） 的进阶用法：**
```python
from concurrent.futures import ProcessPoolExecutor, as_completed
import math

def is_prime(n: int) -> bool:
    if n < 2:
        return False
    for i in range(2, int(math.sqrt(n)) + 1):
        if n % i == 0:
            return False
    return True

# CPU 密集型任务用 ProcessPoolExecutor 加速比接近核数
if __name__ == "__main__":
    numbers = [9999991, 9999997, 9999999, 10000019, 10000079]

    with ProcessPoolExecutor(max_workers=4) as executor:
        futures = {executor.submit(is_prime, n): n for n in numbers}
        for future in as_completed(futures):
            n = futures[future]
            print(f"{n}: {'素数' if future.result() else '非素数'}")
```

### 3.2.7 常见错误

**错误 1：忘记 `if __name__ == "__main__"` 保护**

```python
# multiprocessing 在 Windows 上会重新导入模块
# 如果没有 if __name__ 保护，子进程会递归创建进程！

# 错误：
p = mp.Process(target=worker)  # 在模块顶层执行
p.start()  # 子进程重新导入此文件 → 又执行 p.start() → 无穷递归！

# 正确：
if __name__ == "__main__":
    p = mp.Process(target=worker)
    p.start()
```

**错误 2：传递不可 pickle 的对象**

```python
# 所有跨进程传递的数据必须可 pickle 序列化
# lambda 函数不可 pickle！
def wrong():
    pool.apply(lambda x: x * 2)  # AttributeError: Can't pickle local object

# 类方法也有限制
class MyClass:
    def method(self):
        pass

pool.apply(MyClass().method)  # 可能失败！
```

**错误 3：共享状态被每个进程独立复制**

```python
shared_list = []  # 每个子进程独立复制一份！

def worker(item):
    shared_list.append(item)  # 不影响其他进程的 shared_list

# 正确使用 Manager 或 Queue
```

**错误 4：进程启动方式（fork vs spawn vs forkserver）**

```python
# macOS 默认 spawn，Linux 默认 fork
# fork：子进程继承父进程全部内存（但只复制页表，写时复制）
# spawn：子进程从头导入模块（慢但安全）
# forkserver：先启动 server 进程，后续创建 fork

# fork 可能死锁（如果父进程有锁被持有状态）
# 推荐在 macOS 上设置：
# mp.set_start_method("spawn")
```

### 3.2.8 AI 场景案例

**下面是 AI 场景的实战案例：**
```python
import multiprocessing as mp
import numpy as np
from typing import List

def batch_embed(texts: List[str]) -> np.ndarray:
    """子进程中的嵌入计算：加载模型，处理一段文本"""
    from sentence_transformers import SentenceTransformer
    # 每个子进程独立加载模型（有独立显存/内存）
    model = SentenceTransformer("all-MiniLM-L6-v2")
    embeddings = model.encode(texts)
    return embeddings

class ParallelEmbeddingEngine:
    """并行嵌入引擎：多个 GPU/CPU 进程同时计算"""

    def __init__(self, num_workers: int = 4):
        self.num_workers = num_workers

    def encode(self, texts: List[str], batch_size: int = 32) -> List[np.ndarray]:
        # 将文本分片，每个进程处理一片
        chunks = [texts[i:i + batch_size] for i in range(0, len(texts), batch_size)]

        with mp.Pool(self.num_workers) as pool:
            results = pool.map(batch_embed, chunks)

        return np.vstack(results)

if __name__ == "__main__":
    engine = ParallelEmbeddingEngine(num_workers=4)
    all_texts = ["Hello world"] * 1000
    embeddings = engine.encode(all_texts)
    print(f"嵌入完成: {embeddings.shape}")  # (1000, 384)
```

---

## 3.3 asyncio

### 3.3.1 一句话核心本质

**asyncio 是基于事件循环的单线程协作式并发框架——`async def` 定义协程，`await` 挂起当前协程让 Event Loop 调度其他协程，单线程内实现高并发 I/O。**

### 3.3.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 异步语法 | `CompletableFuture` 链式回调（`.thenApply()`） | `async/await` 原生协程（读起来像同步代码） |
| 事件循环 | Netty EventLoop / Vert.x | `asyncio.run()` 内置 Event Loop |
| 调度单元 | `CompletableFuture` + ForkJoinPool | `Task`（协程的调度包装） |
| 并发模型 | 回调链式（Callback Chain） | 协程挂起（Coroutine Suspension） |
| 取消 | `future.cancel(true)` | `task.cancel()` 注入 `CancelledError` |
| 超时 | `orTimeout()` / `get(timeout, unit)` | `asyncio.wait_for(coro, timeout)` |
| 性能 | 线程池调度（上下文切换有开销） | 纯用户态切换（极低开销） |

**本质区别：** Java 的 `CompletableFuture` 本质上是回调的语法糖，底层仍依赖线程池执行。Python 的 `async/await` 是编译器级别的协程支持——`await` 在字节码层面生成协程挂起/恢复指令，切换开销比线程低 1-2 个数量级。

### 3.3.3 技术原理：事件循环 + 协程执行流程

```
asyncio 执行模型：

单线程内部的协作式调度：

时间线 ──────────────────────────────────────────────────→

协程A  ████████████░░░░░░░░████████░░░░░░░░████████████
       执行中       await I/O   恢复      await I/O   完成
                    ↓                    ↓
协程B  ░░░░░░░░░████████████████░░░░░░░░░░░░░░░░░░░░░░░
                   ↑ 恢复      执行中
                   等待 I/O 完成

事件循环内部（简化）：
┌─────────────────────────────────────────────┐
│ Event Loop                                  │
│                                              │
│  1. 检查 Ready Queue 中的 Task              │
│     ├── Task A 就绪 → resume A              │
│     └── Task B 就绪 → resume B              │
│                                              │
│  2. 调用 selector.select()（epoll/kqueue）  │
│     └── 等待 I/O 事件（socket readable/...  │
│          writable, timer expired）           │
│                                              │
│  3. 处理就绪的 I/O                           │
│     └── 恢复等待该 I/O 的 Task              │
│                                              │
│  4. 回到 1                                  │
└─────────────────────────────────────────────┘

协程的执行（await 的字节码行为）：
┌─────────────────────────────────────────────┐
│ async def fetch():                          │
│     data = await http_get(url)  ← 挂起点    │
│     return data                             │
│                                              │
│ 1. await 将协程的控制权交还给 Event Loop    │
│ 2. Event Loop 注册回调（I/O 就绪时恢复）     │
│ 3. 当前协程的状态被保存                     │
│ 4. I/O 就绪 → Event Loop 恢复协程           │
│ 5. 协程从暂停点继续执行                     │
└─────────────────────────────────────────────┘
```

### 3.3.4 基础语法

**以下是 语法 的基本用法：**
```python
import asyncio
import time

async def hello():
    """最基本的协程"""
    print(f"{time.strftime('%H:%M:%S')} Hello")
    await asyncio.sleep(1)  # 挂起 1 秒，让出 Event Loop
    print(f"{time.strftime('%H:%M:%S')} World")
    return "done"

# ─── 运行协程 ───
result = asyncio.run(hello())  # 创建 Event Loop 并运行到完成
print(result)  # "done"

# ─── 创建 Task（调度协程在后台运行）───
async def main():
    # create_task：创建 Task，立即调度
    task = asyncio.create_task(hello())
    print("Task 已创建")

    # 等待 Task 完成
    result = await task
    print(f"任务结果: {result}")

    # ─── gather（并发运行多个协程）───
    results = await asyncio.gather(
        hello(), hello(), hello(),
        return_exceptions=True,  # 异常作为结果返回，不中断其他协程
    )
    print(results)  # ["done", "done", "done"]

    # ─── wait_for（超时控制）───
    try:
        result = await asyncio.wait_for(hello(), timeout=0.5)
    except asyncio.TimeoutError:
        print("超时！")

    # ─── wait（更灵活的任务等待）───
    tasks = [asyncio.create_task(hello()) for _ in range(3)]
    done, pending = await asyncio.wait(
        tasks,
        timeout=3.0,
        return_when=asyncio.FIRST_COMPLETED,
    )
    print(f"已完成: {len(done)}, 待定: {len(pending)}")

    # 取消待定任务
    for task in pending:
        task.cancel()

asyncio.run(main())
```

### 3.3.5 高级用法

**以下是 用法 的进阶用法：**
```python
import asyncio
from asyncio import Lock, Semaphore, Queue, Event

# ─── 异步上下文管理器 ───
class AsyncResource:
    async def __aenter__(self):
        await asyncio.sleep(0.1)
        return self

    async def __aexit__(self, *args):
        await asyncio.sleep(0.1)

# ─── 异步迭代器 ───
class AsyncCounter:
    def __init__(self, limit):
        self.limit = limit
        self.current = 0

    def __aiter__(self):
        return self

    async def __anext__(self):
        if self.current >= self.limit:
            raise StopAsyncIteration
        await asyncio.sleep(0.1)
        self.current += 1
        return self.current

async def use_async_iter():
    async for i in AsyncCounter(5):
        print(i)

# ─── Semaphore（限制并发数）───
sem = Semaphore(5)

async def limited_request(url: str):
    async with sem:  # 同时最多 5 个请求
        return await fetch(url)

# ─── Queue（生产者-消费者）───
async def producer(queue: Queue, n: int):
    for i in range(n):
        await queue.put(f"item_{i}")
        await asyncio.sleep(0.1)

async def consumer(queue: Queue, name: str):
    while True:
        item = await queue.get()
        print(f"{name} 处理 {item}")
        queue.task_done()

async def run_pipeline():
    q = Queue(maxsize=10)
    producers = [asyncio.create_task(producer(q, 5)) for _ in range(2)]
    consumers = [asyncio.create_task(consumer(q, f"C{i}")) for _ in range(3)]

    await asyncio.gather(*producers)
    await q.join()  # 等待所有项目被处理

    for c in consumers:
        c.cancel()

# ─── 超时和取消 ───
async def timeout_example():
    async def slow_op():
        await asyncio.sleep(10)
        return "done"

    # 方法 1：wait_for
    try:
        result = await asyncio.wait_for(slow_op(), timeout=1.0)
    except asyncio.TimeoutError:
        print("操作超时")

    # 方法 2：timeout (Python 3.11+)
    try:
        async with asyncio.timeout(1.0):
            result = await slow_op()
    except asyncio.TimeoutError:
        print("超时")

    # 方法 3：shield（保护协程不被取消）
    task = asyncio.create_task(slow_op())
    await asyncio.sleep(0.5)
    task.cancel()  # 取消
    try:
        result = await task
    except asyncio.CancelledError:
        print("任务被取消")
```

### 3.3.6 同步 vs 异步性能对比

**下面用实际代码对比同步和异步 I/O 的性能差异：**
```python
import asyncio
import time
import requests

# ─── 同步版本（串行，总耗时 = N × 每个请求）───
def sync_fetch(urls: list):
    results = []
    for url in urls:
        resp = requests.get(url)
        results.append(resp.status_code)
    return results

# ─── 异步版本（并发，总耗时 ≈ 最慢请求）───
async def async_fetch(urls: list):
    async def fetch_one(url):
        async with aiohttp.ClientSession() as session:
            async with session.get(url) as resp:
                return resp.status

    tasks = [fetch_one(url) for url in urls]
    return await asyncio.gather(*tasks)

# ─── 混合：asyncio + 同步代码（run_in_executor）───
async def mixed(urls: list):
    loop = asyncio.get_running_loop()

    def blocking_fetch(url):
        return requests.get(url).status_code

    tasks = [loop.run_in_executor(None, blocking_fetch, url) for url in urls]
    return await asyncio.gather(*tasks)
```

### 3.3.7 常见错误

**错误 1：在异步函数中调用阻塞 API**

```python
async def bad():
    import time
    time.sleep(5)  # 阻塞 5 秒！整个 Event Loop 都卡住了

async def good():
    await asyncio.sleep(5)  # 正确的异步等待

# 如果必须使用阻塞 API，用 run_in_executor：
async def correct():
    loop = asyncio.get_running_loop()
    result = await loop.run_in_executor(None, time.sleep, 5)
```

**错误 2：忘记 await**

```python
async def main():
    coro = async_func()  # 只是创建了协程对象！没有执行！
    result = await coro  # 正确

# 常见的坑：
task = asyncio.create_task(hello())
# 如果没有 await task，main 退出时 task 可能还没完成
```

**错误 3：在非异步环境中调用异步代码**

```python
async def f():
    return 42

# 错误：
x = f()  # 返回 coroutine 对象，不是 42

# 正确：
x = asyncio.run(f())  # 创建 Event Loop 并执行
```

**错误 4：Event Loop 关闭后不能再运行**

```python
asyncio.run(coro1())
asyncio.run(coro2())  # OK：创建新 Event Loop

# 但如果在同一线程中同时有两个 Event Loop 在运行：
loop = asyncio.new_event_loop()
loop.run_until_complete(coro1())
loop.run_until_complete(coro2())  # OK：同一个 loop
```

**错误 5：`asyncio.run()` 不能在事件循环内部调用**

```python
async def main():
    asyncio.run(other())  # RuntimeError: asyncio.run() cannot be called from a running event loop

    # 正确：
    await other()
```

### 3.3.8 AI 场景案例：异步 LLM 客户端

**下面是一个 异步 LLM 客户端 的实战案例：**
```python
import asyncio
import aiohttp
from typing import Optional

class AsyncLLMClient:
    """异步 LLM API 客户端：支持批量请求、流式输出、超时控制"""

    def __init__(self, api_key: str, base_url: str = "https://api.openai.com/v1"):
        self.api_key = api_key
        self.base_url = base_url
        self._session: Optional[aiohttp.ClientSession] = None

    async def __aenter__(self):
        self._session = aiohttp.ClientSession(
            headers={"Authorization": f"Bearer {self.api_key}"},
            timeout=aiohttp.ClientTimeout(total=30),
        )
        return self

    async def __aexit__(self, *args):
        if self._session:
            await self._session.close()

    async def chat(
        self,
        messages: list[dict],
        model: str = "gpt-4",
        temperature: float = 0.7,
        max_tokens: int = 1024,
    ) -> str:
        """单个聊天请求"""
        async with self._session.post(
            f"{self.base_url}/chat/completions",
            json={
                "model": model,
                "messages": messages,
                "temperature": temperature,
                "max_tokens": max_tokens,
            },
        ) as resp:
            data = await resp.json()
            return data["choices"][0]["message"]["content"]

    async def batch_chat(
        self,
        prompts: list[str],
        system_prompt: str = "You are a helpful assistant.",
        max_concurrent: int = 5,
    ) -> list[str]:
        """批量并发聊天请求，限制并发数"""
        sem = asyncio.Semaphore(max_concurrent)

        async def limited(prompt: str):
            async with sem:
                messages = [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": prompt},
                ]
                return await self.chat(messages)

        tasks = [limited(p) for p in prompts]
        return await asyncio.gather(*tasks, return_exceptions=True)

    async def stream_chat(
        self,
        messages: list[dict],
        model: str = "gpt-4",
    ) -> AsyncIterator[str]:
        """流式输出：逐 token yield"""
        async with self._session.post(
            f"{self.base_url}/chat/completions",
            json={
                "model": model,
                "messages": messages,
                "stream": True,
            },
        ) as resp:
            async for line_bytes in resp.content:
                line = line_bytes.decode().strip()
                if line.startswith("data: ") and line != "data: [DONE]":
                    import json
                    chunk = json.loads(line[6:])
                    if chunk["choices"][0].get("delta", {}).get("content"):
                        yield chunk["choices"][0]["delta"]["content"]

async def main():
    prompts = ["讲个故事", "解释相对论", "写首诗", "翻译成英文"]

    async with AsyncLLMClient("sk-xxx") as client:
        # 批量并发：4 个请求同时发出
        results = await client.batch_chat(prompts, max_concurrent=4)

        for prompt, result in zip(prompts, results):
            if isinstance(result, Exception):
                print(f"[{prompt}] 失败: {result}")
            else:
                print(f"[{prompt}] {result[:50]}...")

        # 流式输出
        async for token in client.stream_chat(
            [{"role": "user", "content": "讲个故事"}]
        ):
            print(token, end="", flush=True)

asyncio.run(main())
```

---

## 3.4 FastAPI

### 3.4.1 一句话核心本质

**FastAPI 是基于 Python 类型注解和 asyncio 的高性能 Web 框架——利用 Pydantic 做请求/响应校验，自动生成 OpenAPI 文档，是 AI 工程中最流行的 API 框架。**

### 3.4.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 框架 | Spring Boot / Spring WebFlux | FastAPI |
| 请求校验 | `@Valid` + Bean Validation + 注解 | Pydantic `BaseModel` + 类型注解 |
| 路由定义 | `@RestController` + `@PostMapping("/api/chat")` | `@app.post("/api/chat")` |
| 依赖注入 | `@Autowired` + `@Component` | `Depends()` 函数级 DI |
| 异步 | `WebFlux` + `Mono`/`Flux` | 原生 `async/await` + `StreamingResponse` |
| 文档生成 | Swagger + `@ApiOperation` 注解 | 自动从类型注解生成 OpenAPI |
| 启动 | 编译打包 → 部署到 Tomcat | `uvicorn.run(app)` 直接启动 |
| 性能 | Netty/虚拟线程（高吞吐） | 基于 asyncio + uvicorn（接近 Node.js） |

### 3.4.3 技术原理：FastAPI 请求处理流程

```
FastAPI 请求生命周期：

客户端请求
    ↓
uvicorn（ASGI Server）接收 HTTP 请求
    ↓
FastAPI Router 匹配 URL 和方法
    ↓
Pydantic 校验请求体/参数/查询
    ↓
Depends() 解析依赖注入链
    ↓
执行路由处理函数（sync/async）
    ↓
Pydantic 序列化响应
    ↓
OpenAPI 自动生成文档（/docs, /redoc）

ASGI 协议（Async Server Gateway Interface）：

HTTP Request → ASGI Scope (dict) → ASGI Application
    ↓
async def app(scope, receive, send):
    # scope: 请求信息
    # receive: 接收事件的异步生成器
    # send: 发送事件的异步生成器
    await send({"type": "http.response.start", ...})
    await send({"type": "http.response.body", "body": ...})
```

### 3.4.4 基本用法

**以下是基本用法示例：**
```python
from fastapi import FastAPI, Depends, HTTPException, Query
from fastapi.responses import StreamingResponse, JSONResponse
from pydantic import BaseModel, Field
from typing import Optional, List
import json

app = FastAPI(
    title="AI Service API",
    description="基于 FastAPI 的 AI 服务",
    version="1.0.0",
)

# ─── 请求模型（Pydantic 自动校验）───
class ChatRequest(BaseModel):
    model: str = Field(default="gpt-4", description="模型名称")
    messages: List[dict] = Field(..., description="消息列表")
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)
    max_tokens: int = Field(default=1024, ge=1, le=4096)
    stream: bool = Field(default=False, description="是否流式输出")

class ChatResponse(BaseModel):
    content: str
    model: str
    usage: dict = Field(default_factory=dict)

# ─── 依赖注入：API 密钥验证 ───
async def verify_api_key(api_key: str = Query(..., description="API 密钥")):
    if not api_key.startswith("sk-"):
        raise HTTPException(status_code=401, detail="无效的 API 密钥")
    return api_key

# ─── 路由定义 ───
@app.get("/health")
async def health_check():
    """健康检查接口（自动出现在 OpenAPI 文档中）"""
    return {"status": "ok", "version": "1.0.0"}

@app.post("/v1/chat/completions", response_model=ChatResponse)
async def chat_completion(
    request: ChatRequest,
    api_key: str = Depends(verify_api_key),  # 依赖注入
):
    """聊天补全接口"""
    if request.stream:
        return StreamingResponse(
            stream_response(request),
            media_type="text/event-stream",
        )

    result = await llm_service.generate(
        model=request.model,
        messages=request.messages,
        temperature=request.temperature,
        max_tokens=request.max_tokens,
    )
    return ChatResponse(content=result, model=request.model)

# ─── 流式响应 ───
async def stream_response(request: ChatRequest):
    """流式响应生成器"""
    async for chunk in llm_service.stream_generate(
        model=request.model,
        messages=request.messages,
    ):
        yield f"data: {json.dumps({'content': chunk})}\n\n"
    yield "data: [DONE]\n"

# ─── 错误处理 ───
@app.exception_handler(ValueError)
async def value_error_handler(request, exc):
    return JSONResponse(
        status_code=400,
        content={"error": str(exc)},
    )
```

### 3.4.5 Pydantic 模型详解

**下面是 Pydantic 模型详解 的代码示例：**
```python
from pydantic import BaseModel, Field, validator, ConfigDict
from typing import Optional, List
from datetime import datetime

class AIConfig(BaseModel):
    """Pydantic 模型的完整功能"""
    model_config = ConfigDict(
        extra="forbid",     # 禁止额外字段
        frozen=True,        # 不可变
        validate_default=True,
    )

    model: str = Field(default="gpt-4", description="模型名称")
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)
    max_tokens: int = Field(default=1024, ge=1)
    api_key: str = Field(default="", repr=False)  # 不在 repr 中显示

    # 自定义校验器
    @validator("temperature")
    def check_temperature(cls, v):
        if v < 0 or v > 2:
            raise ValueError("temperature 必须在 0-2 之间")
        return v

    # 模型方法
    def to_openai_kwargs(self) -> dict:
        return self.model_dump(exclude={"api_key"})

# 自动校验
config = AIConfig(model="gpt-4", temperature=0.8)
print(config.model_dump())  # 转为 dict
print(config.model_dump_json())  # 转为 JSON
```

### 3.4.6 高级特性

**以下是 特性 的进阶用法：**
```python
from fastapi import FastAPI, Depends, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
import asyncio

app = FastAPI()

# ─── CORS 中间件（AI 前端调用需要）───
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─── 后台任务 ───
async def log_request(prompt: str):
    await asyncio.sleep(1)  # 模拟异步日志
    print(f"日志记录: {prompt}")

@app.post("/chat")
async def chat(
    prompt: str,
    background_tasks: BackgroundTasks,
):
    background_tasks.add_task(log_request, prompt)  # 后台执行，不阻塞响应
    result = await llm_service.generate(prompt)
    return {"response": result}

# ─── 文件上传 ───
from fastapi import UploadFile, File

@app.post("/upload")
async def upload_file(file: UploadFile = File(...)):
    content = await file.read()
    return {
        "filename": file.filename,
        "content_type": file.content_type,
        "size": len(content),
    }

# ─── WebSocket ───
from fastapi import WebSocket

@app.websocket("/ws/chat")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    try:
        while True:
            data = await websocket.receive_text()
            async for chunk in llm_service.stream_generate([{"role": "user", "content": data}]):
                await websocket.send_text(chunk)
    except WebSocketDisconnect:
        print("客户端断开")
```

### 3.4.7 模块化路由

**下面是 模块化路由 的代码示例：**
```python
from fastapi import APIRouter

# ─── 创建子路由 ───
chat_router = APIRouter(prefix="/v1", tags=["Chat"])

@chat_router.post("/chat/completions")
async def chat(req: ChatRequest):
    return await llm_service.generate(req)

@chat_router.post("/chat/stream")
async def chat_stream(req: ChatRequest):
    return StreamingResponse(stream_response(req))

# ─── 注册到主应用 ───
app.include_router(chat_router)

# ─── 应用生命周期 ───
@app.on_event("startup")
async def startup():
    print("服务启动——加载模型...")
    await llm_service.load_model()

@app.on_event("shutdown")
async def shutdown():
    print("服务关闭——清理资源...")
    await llm_service.cleanup()
```

### 3.4.8 常见错误

**错误 1：在 async 路由中使用阻塞调用**

```python
@app.get("/predict")
async def predict(text: str):
    # ❌ 阻塞 Event Loop
    # time.sleep(5)

    # ✅ 使用 run_in_executor
    result = await asyncio.get_event_loop().run_in_executor(
        None, model.predict, text
    )
    return {"result": result}
```

**错误 2：Pydantic 模型忘记 `model_config` 导致静默忽略额外字段**

```python
class Config(BaseModel):
    model: str = "gpt-4"

# 默认：extra 字段被静默忽略
c = Config(model="gpt-4", api_key="secret")  # api_key 被吞掉！

# 正确：禁止额外字段
class StrictConfig(BaseModel):
    model_config = ConfigDict(extra="forbid")
    model: str = "gpt-4"

# c = StrictConfig(model="gpt-4", api_key="secret")  # ValidationError!
```

**错误 3：uvicorn 启动方式错误**

```python
# ❌ 直接在脚本中运行
if __name__ == "__main__":
    uvicorn.run(app)  # 某些系统会死锁

# ✅ 推荐
# 终端执行：uvicorn main:app --reload
# 或：
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
```

### 3.4.9 AI 场景案例：完整 AI API 服务

**下面是一个 完整 AI API 服务 的实战案例：**
```python
from fastapi import FastAPI, Depends, HTTPException, File, UploadFile
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from contextlib import asynccontextmanager
from typing import AsyncIterator
import json, asyncio

# ─── 模拟 AI 服务 ───
class AIService:
    async def load_model(self):
        """加载模型"""
        await asyncio.sleep(1)
        print("模型加载完成")

    async def generate(self, prompt: str) -> str:
        await asyncio.sleep(0.5)
        return f"[回答] {prompt}"

    async def stream(self, prompt: str) -> AsyncIterator[str]:
        for token in ["这是", "一个", "流式", "响应"]:
            await asyncio.sleep(0.2)
            yield token

    async def cleanup(self):
        print("清理资源")

ai_service = AIService()

# ─── 应用生命周期 ───
@asynccontextmanager
async def lifespan(app: FastAPI):
    await ai_service.load_model()
    yield
    await ai_service.cleanup()

app = FastAPI(title="AI API Service", lifespan=lifespan)

# ─── 请求/响应模型 ───
class ChatRequest(BaseModel):
    prompt: str = Field(..., min_length=1, max_length=10000)
    temperature: float = Field(default=0.7, ge=0, le=2)
    max_tokens: int = Field(default=1024, ge=1)

class ChatResponse(BaseModel):
    content: str
    model: str = "gpt-4"

# ─── API 端点 ───
@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    content = await ai_service.generate(req.prompt)
    return ChatResponse(content=content)

@app.post("/chat/stream")
async def chat_stream(req: ChatRequest):
    async def generate():
        async for token in ai_service.stream(req.prompt):
            yield f"data: {json.dumps({'token': token})}\n\n"
        yield "data: [DONE]\n"
    return StreamingResponse(generate(), media_type="text/event-stream")

@app.post("/analyze-file")
async def analyze_file(file: UploadFile = File(...)):
    content = await file.read()
    text = content.decode()[:1000]
    result = await ai_service.generate(f"分析文件内容: {text}")
    return {"filename": file.filename, "analysis": result}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
```
# 第四阶段：工程化

---

## 4.1 pip & requirements.txt

### 4.1.1 一句话核心本质

**`pip` 是 Python 官方包管理器，从 PyPI（Python Package Index）下载安装第三方包；`requirements.txt` 是纯文本格式的依赖声明文件，等价于 Maven 的 `pom.xml` 或 Gradle 的 `build.gradle`。**

### 4.1.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 包管理器 | Maven / Gradle | pip（标准），poetry / pdm（现代替代） |
| 包仓库 | Maven Central | PyPI（pypi.org） |
| 依赖文件 | `pom.xml` / `build.gradle` | `requirements.txt` / `pyproject.toml` |
| 版本范围 | Maven 范围语法 `[1.0,2.0)` | `>=1.0,<2.0` 或 `~=`、`==`、`^` |
| 锁定文件 | `pom.xml` 内嵌版本管理 | `pip freeze` 输出 / `poetry.lock` |
| 传递依赖 | 自动解析传递依赖 | pip 不锁定传递依赖版本 |
| 构建打包 | `mvn package` → jar/war | `pip wheel` / `poetry build` → wheel |

**本质区别：** `requirements.txt` 是纯文本的"清单"——它列出需要安装的包和版本，但不做依赖解析。而 Maven 的 `pom.xml` 是完整的项目描述，包含构建、测试、部署等全部配置。Python 的 `pyproject.toml`（PEP 517/518/621）正在统一这些功能。

### 4.1.3 pip 基础命令

```bash
# ─── 安装 ───
pip install requests                    # 安装最新版
pip install requests==2.31.0            # 指定精确版本
pip install "requests>=2.0,<3.0"        # 版本范围
pip install requests flask django       # 一次安装多个
pip install -r requirements.txt         # 从文件安装
pip install --upgrade pip               # 升级 pip 自身

# ─── 卸载 ───
pip uninstall requests
pip uninstall -r requirements.txt -y    # 卸载所有依赖

# ─── 查看 ───
pip list                                # 列出已安装包
pip list --outdated                     # 列出可升级的包
pip show requests                       # 查看包详情
pip freeze                              # 输出当前环境所有包（含版本）
pip check                               # 检查依赖冲突

# ─── 其他 ───
pip download requests                    # 下载但不安装
pip wheel requests                       # 构建 wheel 包
pip cache list                           # 查看缓存
```

### 4.1.4 requirements.txt 语法详解

```txt
# ─── 精确版本（推荐用于生产锁定）───
fastapi==0.104.1
uvicorn==0.24.0

# ─── 最低版本（推荐用于库）───
pydantic>=2.0,<3.0

# ─── 兼容版本（等价于 >=X.Y, ==X.*）
# ~=0.104.0  等价于 >=0.104.0, ==0.104.*
# ~=0.104    等价于 >=0.104, ==0.*

# ─── 可选依赖（extras）───
uvicorn[standard]        # 安装 uvicorn 加上 standard 组依赖

# ─── Git 仓库 ───
git+https://github.com/user/project.git@v1.0
git+ssh://git@github.com/user/project.git#egg=my-package

# ─── 本地路径 ───
./packages/my-lib/

# ─── 注释 ───
# 这是注释
torch>=2.1.0  # 也可以行尾加注释

# ─── 条件依赖（环境标记）───
pywin32; sys_platform == "win32"        # 仅 Windows
python-dotenv; python_version >= "3.8"  # 特定 Python 版本
```

### 4.1.5 pip 工作流程

```
pip install requests 的执行过程：

1. 解析参数
       ↓
2. 连接 PyPI（pypi.org/simple/requests/）
       ↓
3. 下载 requests 的元数据（METADATA）
       ↓
4. 解析依赖：requests 依赖 urllib3、charset-normalizer、certifi
       ↓
5. 递归解析这些传递依赖
       ↓
6. 构建依赖图，解决版本冲突
       ↓
7. 下载所有 wheel/sdist 包
       ↓
8. 验证包的哈希值
       ↓
9. 解压安装到 site-packages/
       ↓
10. 可选：执行 setup.py/post-install 脚本
```

### 4.1.6 常见错误

**错误 1：`requirements.txt` 中混用 `>=` 导致构建不稳定**

```txt
# 问题：每次 pip install 可能安装不同版本
requests>=2.0

# 解决：生产环境用 freeze 锁定
pip freeze > requirements.txt
# 结果：
requests==2.31.0
urllib3==2.1.0
```

**错误 2：不使用虚拟环境导致全局污染**

```bash
# ❌ pip install 直接装在系统 Python
pip install torch  # 可能破坏系统工具依赖

# ✅ 始终使用 venv
python -m venv .venv
source .venv/bin/activate
pip install torch
```

**错误 3：`pip freeze` 包含无关包**

```bash
# pip freeze 输出当前环境所有包
# 如果不在 venv 中，会包含全局包
# 正确做法：先在 venv 中安装项目依赖，再 freeze
```

**错误 4：忘记 `-r` 参数**

```bash
pip install requirements.txt  # 错误！会安装名为 requirements.txt 的包
pip install -r requirements.txt  # 正确
```

### 4.1.7 AI 项目 requirements.txt 示例

```txt
# ─── AI 框架 ───
torch>=2.1.0
transformers>=4.36.0
accelerate>=0.25.0
sentence-transformers>=2.2.0

# ─── LLM 工具 ───
openai>=1.6.0
langchain>=0.1.0
chromadb>=0.4.0
tiktoken>=0.5.0

# ─── Web 服务 ───
fastapi>=0.104.0
uvicorn[standard]>=0.24.0
pydantic>=2.0,<3.0

# ─── 工具 ───
python-dotenv>=1.0.0
rich>=13.0.0
typing-extensions>=4.8.0

# ─── 开发工具 ───
pytest>=7.4.0
mypy>=1.7.0
ruff>=0.1.0
```

---

## 4.2 venv（虚拟环境）

### 4.2.1 一句话核心本质

**venv 是 Python 内置的虚拟环境工具——为每个项目创建隔离的 Python 运行环境，不同项目可以有不同版本的依赖，互不干扰。**

### 4.2.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| 依赖隔离 | 每个项目有自己的 `pom.xml`/`build.gradle`，本地仓库共享 | 每个项目有独立 `venv`，site-packages 完全隔离 |
| 环境复制 | Maven 本地仓库 `~/.m2/repository` | venv + `requirements.txt` 重装 |
| JDK 版本 | `SDKMAN!` 管理 JDK 版本 | `pyenv` 管理 Python 版本 |
| 切换方式 | 无内建方案 | `source venv/bin/activate` |
| 空间占用 | GRADLE/Maven 仓库全局共享，增量 | venv 每次完整复制标准库 |

**本质区别：** Java 通过 Maven/Gradle 的"依赖解析+全局缓存"实现隔离（多个项目共享 `~/.m2` 缓存），Python venv 通过"独立 Python 环境"实现隔离（每个项目有完整副本）。

### 4.2.3 基本用法

```bash
# ─── 创建虚拟环境 ───
python -m venv venv                  # 创建名为 venv 的虚拟环境
python3.11 -m venv venv              # 指定 Python 3.11 创建

# ─── 激活虚拟环境 ───
source venv/bin/activate             # Linux / macOS
# 或
venv\Scripts\activate                # Windows

# 激活后提示符前会出现 (venv)
# (venv) user@host:~/project$

# ─── 在 venv 中安装依赖 ───
pip install -r requirements.txt      # 依赖安装在 venv/ 中

# ─── 退出虚拟环境 ───
deactivate

# ─── 删除虚拟环境（直接删目录）───
rm -rf venv                          # Linux / macOS
# rmdir /s venv                      # Windows
```

### 4.2.4 原理：venv 目录结构

```
venv/                          # 虚拟环境根目录
├── bin/                       # 可执行文件
│   ├── python                 # → 指向系统 Python（符号链接）
│   ├── pip                    # → venv 专用的 pip
│   ├── activate               # bash 激活脚本
│   ├── activate.fish          # fish 激活脚本
│   ├── activate.csh           # csh 激活脚本
│   └── deactivate             # 退出函数
│
├── include/                   # C 头文件（编译扩展用）
│
├── lib/
│   └── python3.11/
│       └── site-packages/     # 独立包目录（pip 安装在此）
│           ├── fastapi/
│           ├── pydantic/
│           └── ...
│
└── pyvenv.cfg                 # venv 配置文件
```

### 4.2.5 虚拟环境工作流程

```
项目开发流程：

1. 创建项目目录
   mkdir my-ai-project && cd my-ai-project

2. 创建虚拟环境
   python -m venv .venv          # .venv 是常见命名（点开头，隐藏目录）

3. 激活虚拟环境
   source .venv/bin/activate

4. 安装依赖
   pip install torch transformers fastapi

5. 锁定依赖版本
   pip freeze > requirements.txt  # 生成锁定文件

6. 开发...

7. 切换项目时 deactivate
   deactivate

团队协作时其他人的操作：
git clone my-ai-project
cd my-ai-project
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt   # 精确还原版本
```

### 4.2.6 常见错误

**错误 1：忘记激活 venv**

```bash
# 即使目录下有 venv，也需要手动激活
pip install torch  # 安装到系统 Python 了！

# 检查当前 Python 路径：
which python
# 如果不在 venv 中，显示 /usr/bin/python
# 如果已激活，显示 /path/to/project/.venv/bin/python
```

**错误 2：将 venv 目录加入版本控制**

```bash
# .gitignore 中添加：
venv/
.venv/
__pycache__/
*.pyc
```

**错误 3：激活后仍使用系统 `pip3`**

```bash
# 激活后使用 pip（不是 pip3）
pip install ...  # 正确，在 venv 中
pip3 install ... # 可能用了系统的 pip3！

# 建议：
python -m pip install ...  # 始终使用当前 Python 的 pip
```

---

## 4.3 poetry

### 4.3.1 一句话核心本质

**Poetry 是 Python 现代依赖管理和打包工具，统一了 `requirements.txt` + `setup.py` + `setup.cfg`，使用 `pyproject.toml` 作为单一项目配置文件，自动生成 `poetry.lock` 锁定文件。**

### 4.3.2 Java vs Python 对比

| 维度 | Maven | Pip + venv | Poetry |
|------|-------|------------|--------|
| 项目文件 | `pom.xml` | `requirements.txt` + `setup.py` | `pyproject.toml`（统一） |
| 锁定文件 | 依赖集成在 pom.xml 中 | `pip freeze > requirements.txt` | `poetry.lock`（自动管理） |
| 虚拟环境 | Maven 不管理 | `venv` 手动管理 | 自动创建和管理 |
| 打包发布 | `mvn deploy` | `python setup.py sdist` | `poetry build && poetry publish` |
| 依赖解析 | Maven 自动 | pip 无冲突解决 | 高级依赖解析器 |

### 4.3.3 pyproject.toml 详解

```toml
[project]
name = "ai-agent"
version = "0.1.0"
description = "AI Agent 框架"
readme = "README.md"
authors = [
    {name = "Developer", email = "dev@example.com"}
]
license = {text = "MIT"}
requires-python = ">=3.11"

dependencies = [
    "fastapi>=0.104.0",
    "openai>=1.6.0",
    "transformers>=4.36.0",
    "torch>=2.1.0",
]

[project.optional-dependencies]
dev = [
    "pytest>=7.4.0",
    "mypy>=1.7.0",
    "ruff>=0.1.0",
]
gpu = ["torch>=2.1.0"]

[project.urls]
Homepage = "https://github.com/user/ai-agent"
Repository = "https://github.com/user/ai-agent.git"

[build-system]
requires = ["setuptools>=68.0"]
build-backend = "setuptools.build_meta"
```

### 4.3.4 Poetry 常用命令

```bash
poetry new my-project          # 创建新项目（标准目录结构）
poetry init                    # 在当前目录初始化

poetry install                 # 安装所有依赖
poetry add fastapi             # 添加依赖（自动安装）
poetry add --dev pytest        # 添加开发依赖
poetry add "torch>=2.1.0"     # 带版本约束

poetry remove fastapi          # 移除依赖
poetry update                  # 更新所有依赖到最新
poetry show --tree             # 查看依赖树

poetry env info                # 查看虚拟环境信息
poetry shell                   # 进入虚拟环境
poetry run python main.py      # 在 venv 中运行

poetry build                   # 构建 wheel + sdist
poetry publish                 # 发布到 PyPI

poetry lock                    # 生成/更新 poetry.lock
poetry export --format requirements.txt > requirements.txt
```

### 4.3.5 常见错误

**错误 1：`pyproject.toml` 与 `requirements.txt` 同步问题**

```bash
# 同时维护两个文件，版本不一致
# 解决方案：只维护 pyproject.toml，用 poetry export 生成 requirements.txt
```

**错误 2：poetry 创建的环境路径找不到**

```bash
# 查看 poetry venv 路径：
poetry env info --path
# 通常位于：~/Library/Caches/pypoetry/virtualenvs/ 或 ~/.cache/pypoetry/
```

---

## 4.4 pytest

### 4.4.1 一句话核心本质

**pytest 是 Python 最流行的测试框架——用纯 `assert` 替代 JUnit 的断言 API，自动发现测试函数，通过 fixture 管理依赖，通过插件系统覆盖全场景。**

### 4.4.2 Java vs Python 对比

| 维度 | JUnit 5 | pytest |
|------|---------|--------|
| 断言 | `assertEquals(5, result)` | `assert result == 5` |
| 测试发现 | `@Test` 注解 + 命名约定 | 按文件名 `test_*.py` / `*_test.py` 自动发现 |
| 参数化 | `@ParameterizedTest` + `@ValueSource` | `@pytest.mark.parametrize("a,b,expected", [(1,2,3)])` |
| 测试固件 | `@BeforeEach` / `@AfterEach` | `@pytest.fixture`（更灵活的作用域和自动注入） |
| 异常测试 | `assertThrows(Exc.class, () -> ...)` | `with pytest.raises(ValueError): ...` |
| 跳过 | `@Disabled` | `@pytest.mark.skip` / `@pytest.mark.skipif` |
| Mock | Mockito | pytest-mock（内置 mocking） |
| 覆盖率 | JaCoCo | pytest-cov |
| 运行 | IDE 或 `mvn test` | `pytest`（命令行） |

### 4.4.3 基础用法

**以下是基本用法示例：**
```python
# test_math.py（pytest 自动发现所有 test_ 开头的函数和文件）

def test_addition():
    """最基本的测试：assert 一个表达式"""
    assert 1 + 1 == 2

def test_string_ops():
    result = "hello " + "world"
    assert result == "hello world"
    assert "hello" in result
    assert len(result) == 11

# ─── 异常测试 ───
import pytest

def test_raises():
    """用 pytest.raises 验证期望的异常"""
    with pytest.raises(ValueError):
        int("abc")

    with pytest.raises(ZeroDivisionError):
        1 / 0

    # 还可以检查异常消息
    with pytest.raises(ValueError, match="invalid literal"):
        int("abc")

# ─── 参数化测试 ───
@pytest.mark.parametrize("a,b,expected", [
    (1, 2, 3),
    (0, 0, 0),
    (-1, 1, 0),
    (100, -100, 0),
])
def test_add(a, b, expected):
    assert add(a, b) == expected

# ─── 跳过和预期失败 ───
@pytest.mark.skip(reason="尚未实现")
def test_not_ready():
    pass

@pytest.mark.skipif(
    sys.version_info < (3, 10),
    reason="需要 Python 3.10+"
)
def test_new_feature():
    pass

@pytest.mark.xfail(reason="已知 bug #42")
def test_known_bug():
    assert 1/0 == 0

# ─── 分组标记 ───
@pytest.mark.slow
def test_heavy_computation():
    import time
    time.sleep(10)

# 运行：pytest -m slow
# 或排除：pytest -m "not slow"
```

### 4.4.4 fixture（测试固件）

**下面是 fixture（测试固件） 的代码示例：**
```python
import pytest
from typing import Generator

# ─── 基本 fixture ───
@pytest.fixture
def model():
    """提供测试用的模型实例"""
    return {"name": "test-model", "version": "1.0"}

def test_model_name(model):  # 参数名匹配 fixture 名称，自动注入
    assert model["name"] == "test-model"

# ─── 带 yield 的 fixture（setup + teardown）───
@pytest.fixture
def database() -> Generator:
    """设置数据库连接，测试结束后自动清理"""
    db = Database(":memory:")  # setup
    db.create_tables()
    yield db                   # 测试用例使用 db
    db.close()                 # teardown（即使测试失败也执行）

def test_insert(database):
    database.insert("users", {"name": "Alice"})
    assert database.count("users") == 1

# ─── fixture 作用域 ───
@pytest.fixture(scope="session")   # 所有测试共享一次（整个会话）
@pytest.fixture(scope="module")    # 每个模块一次
@pytest.fixture(scope="class")     # 每个类一次
@pytest.fixture(scope="function")  # 默认：每个测试函数一次

@pytest.fixture(scope="session")
def api_client():
    """整个测试会话只初始化一次"""
    client = AsyncLLMClient("test-key")
    yield client
    client.close()

# ─── fixture 依赖 fixture ───
@pytest.fixture
def user(database):
    user_id = database.insert("users", {"name": "Alice"})
    return user_id

@pytest.fixture
def user_posts(user, database):  # 自动注入 user fixture
    posts = database.query("posts", user_id=user)
    return posts

def test_user_has_posts(user_posts):
    assert len(user_posts) >= 0

# ─── conftest.py（共享 fixture）───
# 将共享 fixture 放在 tests/conftest.py 中
# 所有测试文件自动可用，无需 import

# tests/conftest.py:
# @pytest.fixture
# def llm_mock(mocker):
#     return mocker.patch("my_app.llm.chat")
```

### 4.4.5 Mock 测试

**下面是 Mock 测试 的代码示例：**
```python
# 需要安装：pip install pytest-mock

def test_llm_call(mocker):
    """mock LLM API 调用，不实际发出网络请求"""
    # 创建 mock
    mock_response = {"choices": [{"message": {"content": "Hello!"}}]}

    # patch openai 模块
    mocker.patch("openai.ChatCompletion.create", return_value=mock_response)

    # 测试代码（不再调用真实 API）
    result = my_app.chat("Hi")
    assert result == "Hello!"

    # 验证调用参数
    openai.ChatCompletion.create.assert_called_once_with(
        model="gpt-4",
        messages=[{"role": "user", "content": "Hi"}],
    )

def test_llm_failure(mocker):
    """模拟 LLM 调用失败"""
    mocker.patch("openai.ChatCompletion.create", side_effect=TimeoutError("API 超时"))

    with pytest.raises(TimeoutError):
        my_app.chat("Hi")

# ─── 异步 Mock ───
@pytest.mark.asyncio
async def test_async_llm(mocker):
    mock = mocker.patch("my_app.llm_service.generate", return_value="Mock response")
    result = await my_app.chat_async("Hi")
    assert result == "Mock response"
    mock.assert_awaited_once()
```

### 4.4.6 运行命令

```bash
# ─── 基础 ───
pytest                          # 自动发现所有 test_ 文件
pytest -v                       # 详细模式（每个测试一行）
pytest -q                       # 安静模式（只显示结果摘要）

# ─── 过滤 ───
pytest -k "test_add"            # 按名称过滤
pytest -k "test_add or test_sub" # 多个名称
pytest -m slow                  # 按标记过滤
pytest --ignore=tests/integration/  # 忽略目录

# ─── 调试 ───
pytest -x                       # 第一个失败就停止
pytest --maxfail=3              # 最多允许 3 个失败
pytest --tb=short               # 简短回溯
pytest --tb=long                # 完整回溯
pytest -s                       # 显示 print 输出（不捕获 stdout）

# ─── 覆盖率 ───
pytest --cov=src tests/         # 计算覆盖率
pytest --cov=src --cov-report=html tests/  # 生成 HTML 报告

# ─── 并行 ───
pytest -n auto                  # 自动检测 CPU 核数并行（需 pytest-xdist）

# ─── 失败重试 ───
pytest --reruns 3               # 失败重试 3 次（需 pytest-rerunfailures）
```

### 4.4.7 常见错误

**错误 1：`assert` 使用不正确**

```python
def test_wrong():
    assert result == True   # 应该用 assert result
    assert result == None   # 应该用 assert result is None
```

**错误 2：测试修改 fixture 导致其他测试失败**

```python
@pytest.fixture
def data():
    return [1, 2, 3]

def test_first(data):
    data.append(4)          # 修改了 fixture 返回的对象！

def test_second(data):
    assert len(data) == 3   # FAIL！现在长度是 4

# 修复：fixture 返回新对象
@pytest.fixture
def data():
    return [1, 2, 3]  # 每次测试都创建新列表（scope="function" 默认）
```

**错误 3：测试代码与生产代码耦合**

```python
# 坏：测试需要导入实际模型
from my_model import LargeModel
model = LargeModel()  # 加载实际模型，测试很慢

# 好：注入 mock
def test_inference(mocker):
    mock_model = mocker.Mock()
    mock_model.predict.return_value = "result"
    assert inference(mock_model, "input") == "result"
```

### 4.4.8 AI 项目测试示例

**下面是 AI 项目测试示例 的代码示例：**
```python
import pytest
from unittest.mock import AsyncMock

@pytest.fixture
def config():
    return {
        "model": "gpt-4",
        "temperature": 0.7,
        "api_key": "test-key",
    }

@pytest.mark.asyncio
async def test_llm_success(mocker, config):
    """测试 LLM 成功调用"""
    mock_chat = mocker.patch("openai.ChatCompletion.acreate")
    mock_chat.return_value = {
        "choices": [{"message": {"content": "Hello!"}}]
    }

    service = AIService(config)
    result = await service.chat("Hi")
    assert result == "Hello!"

@pytest.mark.asyncio
async def test_llm_retry(mocker, config):
    """测试 LLM 重试机制"""
    mock_chat = mocker.patch("openai.ChatCompletion.acreate")
    mock_chat.side_effect = [
        TimeoutError("timeout"),
        TimeoutError("timeout"),
        {"choices": [{"message": {"content": "OK"}}]},
    ]

    service = AIService(config, max_retries=3)
    result = await service.chat("Hi")
    assert result == "OK"
    assert mock_chat.call_count == 3

@pytest.mark.parametrize("prompt,expected_len", [
    ("", 0),
    ("Hello", 5),
    ("Hello World", 11),
])
def test_prompt_preprocessing(prompt, expected_len):
    assert preprocess(prompt) == expected_len
```

---

# 第五阶段：AI 工程（重点）

---

## 5.1 NumPy

### 5.1.1 一句话核心本质

**NumPy 是 Python 数值计算的基础——核心是 `ndarray`（N-dimensional array），底层用 C 实现的连续内存块，提供向量化运算（免 Python 循环）和广播机制（不同形状数组自动对齐）。**

### 5.1.2 Java vs Python 对比

| 维度 | Java | Python/NumPy |
|------|------|--------------|
| 多维数组 | `double[][]` 是数组的数组（内存不连续） | `np.ndarray` 连续内存块 |
| 向量运算 | 需要 for 循环 | `arr * 2` 向量化，底层 C 循环 |
| 矩阵乘法 | 三重 for 循环 | `a @ b` 调用 BLAS |
| 性能 | JIT 编译后接近 C | C/ Fortran 后端（接近 C 性能） |
| 维度操作 | 手写索引计算 | `reshape`, `transpose`, `broadcasting` |
| GPU 加速 | 需要 CUDA/专门库 | NumPy 本身 CPU 只，PyTorch 支持 GPU |

### 5.1.3 技术原理：ndarray 内存模型

```
Python List 内存布局（按指针访问，随机分散）：
┌─────┬─────┬─────┬─────┐
│ Ptr │ Ptr │ Ptr │ Ptr │  ← 栈/堆上的指针数组
└──┬──┴──┬──┴──┬──┴──┬──┘
   ↓     ↓     ↓     ↓
┌────┐ ┌────┐ ┌────┐ ┌────┐
│int │ │int │ │int │ │int │  ← 堆上的 PyObject
│ 42 │ │ 43 │ │ 44 │ │ 45 │
└────┘ └────┘ └────┘ └────┘
每个元素：8 字节指针 + 28 字节 PyObject 头

NumPy ndarray 内存布局（连续内存，直接访问）：
┌──────────────────────────────┐
│ 42 │ 43 │ 44 │ 45 │ 46 │ ...│  ← 连续 C 类型数组
└──────────────────────────────┘
每个元素：8 字节（float64）

ndarray 对象结构：
┌─────────────────────────────────────┐
│ ndarray                              │
│ ├── data: 指向连续内存块的指针        │
│ ├── shape: (3, 4)                    │
│ ├── strides: (32, 8)                 │
│ ├── dtype: float64                   │
│ ├── ndim: 2                          │
│ └── size: 12                         │
└─────────────────────────────────────┘
```

### 5.1.4 核心 API

**下面是 核心 API 的代码示例：**
```python
import numpy as np

# ─── 创建 ───
np.zeros((3, 4))          # 全 0
np.ones((2, 3))           # 全 1
np.eye(3)                 # 单位矩阵
np.random.randn(3, 3)     # 标准正态分布随机数
np.arange(10)             # [0, 1, ..., 9]
np.linspace(0, 1, 5)      # [0.0, 0.25, 0.5, 0.75, 1.0]
np.full((2, 3), 42)       # 全 42

# ─── 形状操作 ───
arr = np.arange(12).reshape(3, 4)
arr.T                     # 转置
arr.flatten()             # 展平为一维（复制）
arr.ravel()               # 展平（可能返回视图）
arr.transpose(1, 0)       # 指定轴转置

# ─── 索引和切片 ───
arr[0, :]                 # 第一行
arr[:, 1]                 # 第二列
arr[arr > 5]              # 布尔索引
np.where(arr > 5, arr, 0) # 条件选择

# ─── 运算（向量化）───
arr + 1                   # 广播加法
arr * 2                   # 标量乘法
a @ b                     # 矩阵乘法（dot product）
np.dot(a, b)              # 点积
np.matmul(a, b)           # 矩阵乘法

# ─── 统计 ───
arr.mean()                # 平均值
arr.std()                 # 标准差
arr.max(axis=0)           # 沿第 0 轴的最大值
arr.sum(axis=1)           # 沿第 1 轴求和
np.percentile(arr, 90)    # 百分位数

# ─── 线性代数 ───
np.linalg.inv(a)          # 矩阵求逆
np.linalg.eig(a)          # 特征值分解
np.linalg.svd(a)          # SVD 分解
np.linalg.norm(arr)       # 范数
```

### 5.1.5 广播机制

**下面是 广播机制 的代码示例：**
```python
# 广播规则：从后往前比较维度，维度为 1 或缺失则广播
a = np.array([[1], [2], [3]])     # shape: (3, 1)
b = np.array([10, 20, 30])        # shape: (3,)
c = a + b                         # 广播后 shape: (3, 3)
# [[11, 21, 31],
#  [12, 22, 32],
#  [13, 23, 33]]

# 广播逻辑：
# a.shape: (3, 1)
# b.shape: (3,)  → 补齐为 (1, 3)
# 结果:    (3, 3) ← 两个维度都对齐
```

### 5.1.6 常见错误

**错误：Python list 和 NumPy array 混淆**

```python
a = [1, 2, 3]      # Python list
b = [4, 5, 6]
print(a + b)       # [1, 2, 3, 4, 5, 6]  ← 拼接！

a = np.array([1, 2, 3])
b = np.array([4, 5, 6])
print(a + b)       # [5, 7, 9]  ← 元素相加！
```

### 5.1.7 AI 场景案例

**下面是 AI 场景的实战案例：**
```python
def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    """计算两个向量的余弦相似度"""
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

def batch_similarity(query: np.ndarray, db: np.ndarray) -> np.ndarray:
    """查询向量与数据库中所有向量的相似度"""
    return np.dot(db, query) / (
        np.linalg.norm(db, axis=1) * np.linalg.norm(query)
    )

def normalize(embeddings: np.ndarray) -> np.ndarray:
    """L2 归一化：每个向量除以其范数"""
    return embeddings / np.linalg.norm(embeddings, axis=1, keepdims=True)

# 使用示例
embeddings = np.random.randn(1000, 384)  # 1000 个 384 维向量
query = np.random.randn(384)
scores = batch_similarity(query, embeddings)
top_k = np.argsort(scores)[-5:][::-1]     # 获取前 5 个最相似
```

---

## 5.2 Pandas

### 5.2.1 一句话核心本质

**Pandas 是 Python 的"Excel 在代码中"——核心是 `DataFrame`（二维表格）和 `Series`（一维列），提供数据读取、清洗、转换、分组、聚合、合并的全套数据分析能力。**

### 5.2.2 Java vs Python 对比

| 维度 | Java | Python/Pandas |
|------|------|---------------|
| 表格数据结构 | `ResultSet` / `List<Map<K,V>>` | `pd.DataFrame`（原生表格支持） |
| 列操作 | Stream API + map | `df["col"].apply(func)` |
| 分组聚合 | `Collectors.groupingBy` + `sum`/`avg` | `df.groupby("col").agg({"val": ["sum", "mean"]})` |
| 缺失值处理 | 手动 null 检查 | `df.isnull()`, `df.dropna()`, `df.fillna()` |
| CSV 读取 | OpenCSV / 手动解析 | `pd.read_csv("file.csv")` |
| 数据可视化 | 需额外图表库 | `df.plot()` 直接集成 Matplotlib |

### 5.2.3 技术原理：DataFrame 结构

```
DataFrame 内部结构：

            ┌─────────┬─────────┬─────────┬─────────┐
            │   name   │   age   │  score  │  active  │
├─────────┼─────────┼─────────┼─────────┤
│  0      │  Alice   │   25    │  0.95   │   True   │
│  1      │  Bob     │   30    │  0.87   │   False  │
│  2      │  Charlie │   35    │  0.92   │   True   │
└─────────┴─────────┴─────────┴─────────┘

底层实现：
- 列式存储：每列是一个独立的 pd.Series → NumPy ndarray
- 索引（Index）：行标签，默认 RangeIndex
- 每列可以是不同 dtype（int64, float64, object, bool...）

DataFrame = dict of Series (列名 → Series)
Series = Index + ndarray (标签 → 值)
```

### 5.2.4 核心 API

**下面是 核心 API 的代码示例：**
```python
import pandas as pd
import numpy as np

# ─── 创建 ───
df = pd.DataFrame({
    "name": ["Alice", "Bob", "Charlie"],
    "age": [25, 30, 35],
    "score": [0.95, 0.87, 0.92],
    "active": [True, False, True],
})

# ─── 查看 ───
df.head(2)           # 前 2 行
df.tail()            # 后 5 行
df.info()            # 概览（列名、类型、非空数）
df.describe()        # 统计摘要
df.shape             # (行数, 列数)
df.columns           # 列名列表
df.dtypes            # 每列类型

# ─── 索引和选择 ───
df["name"]           # 单列 → Series
df[["name", "score"]]  # 多列 → DataFrame
df.iloc[0]           # 按行号（第 1 行）
df.loc[0]            # 按行标签（索引为 0 的行）
df.iloc[1:3, 0:2]    # 切片
df[df["score"] > 0.9]  # 布尔索引

# ─── 清洗 ───
df.isnull().sum()    # 每列 null 值数量
df.dropna()          # 删除含 null 的行
df.fillna(0)         # 用 0 填充 null
df["age"].fillna(df["age"].mean())  # 用均值填充
df.drop_duplicates() # 删除重复行

# ─── 转换 ───
df["age_squared"] = df["age"] ** 2   # 添加列
df["name_len"] = df["name"].str.len()  # 字符串操作
df["active_int"] = df["active"].astype(int)  # 类型转换
df.rename(columns={"score": "accuracy"}, inplace=True)  # 重命名

# ─── 分组聚合 ───
df.groupby("active")["score"].mean()
df.groupby("active").agg({
    "score": ["mean", "std", "count"],
    "age": "mean",
})

# ─── 合并 ───
pd.concat([df1, df2])              # 纵向拼接
pd.concat([df1, df2], axis=1)      # 横向拼接
pd.merge(df1, df2, on="id")        # SQL JOIN
pd.merge(df1, df2, on="id", how="left")

# ─── 读取/写入 ───
pd.read_csv("data.csv")
pd.read_json("data.json")
pd.read_parquet("data.parquet")    # 推荐格式：快 10x
df.to_csv("output.csv", index=False)
df.to_json("output.json", orient="records")
```

### 5.2.5 常见错误

**错误 1：链式赋值的 SettingWithCopyWarning**

```python
df[df["age"] > 30]["score"] = 0.9  # SettingWithCopyWarning！

# 正确：
df.loc[df["age"] > 30, "score"] = 0.9
```

**错误 2：`inplace=True` 的误解**

```python
# 很多 pandas 方法默认返回新对象
df.dropna(inplace=True)   # 修改原对象
df = df.dropna()          # 返回新对象
# 推荐用新对象赋值方式
```

### 5.2.6 AI 场景案例

**下面是 AI 场景的实战案例：**
```python
# ─── 对话日志分析 ───
conversations = pd.DataFrame([
    {"prompt": "讲个故事", "tokens": 150, "model": "gpt-3.5", "latency_ms": 1200, "timestamp": "2024-01-01 10:00"},
    {"prompt": "写首诗", "tokens": 80, "model": "gpt-4", "latency_ms": 2300, "timestamp": "2024-01-01 10:05"},
    {"prompt": "解释相对论", "tokens": 200, "model": "gpt-4", "latency_ms": 3500, "timestamp": "2024-01-01 10:10"},
])

# 按模型分析
stats = conversations.groupby("model").agg({
    "tokens": ["sum", "mean", "max"],
    "latency_ms": ["mean", "max"],
    "prompt": "count",
})
print(stats)

# ─── 训练日志分析 ───
def analyze_training(log_path: str):
    """分析训练日志：找到最佳 epoch 和早停时机"""
    df = pd.read_csv(log_path)

    # 最佳 epoch
    best = df.loc[df["val_loss"].idxmin()]
    print(f"最佳 epoch: {best['epoch']}, val_loss: {best['val_loss']:.4f}")

    # 早停检查：连续 N 轮不改善
    patience = 5
    df["val_loss_min"] = df["val_loss"].cummin()
    df["no_improve"] = df["val_loss"] > df["val_loss_min"].shift(patience)
    early_stop = df["no_improve"].idxmax() if df["no_improve"].any() else None

    return df

# ─── 特征工程 ───
def build_features(df: pd.DataFrame) -> pd.DataFrame:
    features = df.copy()
    features["prompt_len"] = features["prompt"].str.len()
    features["word_count"] = features["prompt"].str.split().str.len()
    features["hour"] = pd.to_datetime(features["timestamp"]).dt.hour
    features["is_weekend"] = pd.to_datetime(features["timestamp"]).dt.dayofweek >= 5
    return features
```

---

## 5.3 PyTorch

### 5.3.1 一句话核心本质

**PyTorch 是动态计算图深度学习框架——`Tensor` 提供 GPU 加速的多维数组，`autograd` 自动构建计算图并计算梯度，`nn.Module` 封装神经网络层。定义网络就像写普通 Python 类（Define-by-Run）。**

### 5.3.2 Java vs Python 对比

| 维度 | Java | Python/PyTorch |
|------|------|----------------|
| 深度学习框架 | DL4J (DeepLearning4J) | PyTorch (学术界首选) |
| 计算图 | 静态图（先构建再执行） | 动态图（运行时构建） |
| 自动求导 | 需手动实现 backward | `loss.backward()` 自动计算所有梯度 |
| GPU 加速 | CUDA 代码需手写 | `.to("cuda")` 自动迁移 |
| 调试 | 无法在图中打断点 | 可 print tensor 值，随时打断点 |
| 生态 | 企业级应用少 | HuggingFace + 学术界 + 生产全链路 |

### 5.3.3 技术原理：动态图 vs 静态图

```
静态图（TensorFlow 1.x）：
[构建图] → [编译优化] → [执行]
def model(x):
    y = x @ W + b     # 只是构建符号节点
    return y

动态图（PyTorch）：
[执行即构建]
def model(x):
    y = x @ W + b     # 实际执行了！可 print(y)
    return y

自动求导原理：
z = (x * w).sum()

计算图（DAG）：
x ──┐
    ├── (*) ── z ── sum ── loss
w ──┘

backward() 链式法则：
loss → ∂loss/∂w = x
      → ∂loss/∂x = w
```

### 5.3.4 Tensor 基础

**以下是 Tensor 的基本用法：**
```python
import torch

# ─── 创建 ───
data = torch.tensor([[1, 2], [3, 4]], dtype=torch.float32)
zeros = torch.zeros(3, 4)
randn = torch.randn(3, 3)          # 标准正态
arange = torch.arange(10)

# ─── 设备 ───
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
data = data.to(device)             # 迁移到 GPU

# ─── 形状 ───
data.shape                         # torch.Size([2, 2])
data.view(4, 1)                    # 重塑（视图，不复制）
data.unsqueeze(0)                  # 增加维度 → (1, 2, 2)
data.squeeze()                     # 移除大小为 1 的维度

# ─── 运算 ───
a + b, a * b, a @ b                # 加减乘+矩阵乘
torch.cat([a, b], dim=0)           # 拼接
torch.stack([a, b], dim=0)         # 堆叠（新维度）

# ─── NumPy 互转 ───
np_arr = data.cpu().numpy()        # GPU 上的 tensor 要先 .cpu()
tensor = torch.from_numpy(np_arr)

# ─── 自动求导 ───
x = torch.tensor([2.0, 3.0], requires_grad=True)
y = x ** 2 + 2 * x + 1
z = y.sum()
z.backward()                       # 反向传播
print(x.grad)                      # tensor([6., 8.])  ← ∂z/∂x
```

### 5.3.5 神经网络

**下面是 神经网络 的代码示例：**
```python
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim

class SimpleNN(nn.Module):
    def __init__(self, input_dim=768, hidden_dim=256, num_classes=10):
        super().__init__()
        self.fc1 = nn.Linear(input_dim, hidden_dim)
        self.fc2 = nn.Linear(hidden_dim, num_classes)
        self.dropout = nn.Dropout(0.3)

    def forward(self, x):
        x = F.relu(self.fc1(x))
        x = self.dropout(x)
        return self.fc2(x)

model = SimpleNN().to(device)
criterion = nn.CrossEntropyLoss()
optimizer = optim.Adam(model.parameters(), lr=0.001)

for epoch in range(10):
    for batch_x, batch_y in dataloader:
        batch_x, batch_y = batch_x.to(device), batch_y.to(device)
        optimizer.zero_grad()
        loss = criterion(model(batch_x), batch_y)
        loss.backward()
        optimizer.step()
```

### 5.3.6 DataLoader 和模型保存

**下面是 DataLoader 和模型保存 的代码示例：**
```python
from torch.utils.data import Dataset, DataLoader

class TextDataset(Dataset):
    def __init__(self, texts, labels):
        self.texts = texts
        self.labels = labels

    def __len__(self):
        return len(self.texts)

    def __getitem__(self, idx):
        return {"text": self.texts[idx], "label": self.labels[idx]}

dataset = TextDataset(texts, labels)
dataloader = DataLoader(dataset, batch_size=32, shuffle=True, num_workers=4)

# 保存/加载
torch.save(model.state_dict(), "model.pt")
model.load_state_dict(torch.load("model.pt", map_location=device))
model.eval()

# 推理模式（关闭 dropout 等）
with torch.no_grad():
    predictions = model(batch_x)
```

### 5.3.7 常见错误

**错误：`Tensor` 在 GPU 和 CPU 之间混用**

```python
a = torch.tensor([1, 2])      # CPU
b = torch.tensor([3, 4]).cuda()  # GPU
c = a + b                      # RuntimeError: Tensor for CPU and GPU!

# 修复：统一设备
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
a = a.to(device)
b = b.to(device)
c = a + b
```

---

## 5.4 Transformers (HuggingFace)

### 5.4.1 一句话核心本质

**HuggingFace Transformers 提供统一的接口加载 10000+ 预训练模型（BERT、GPT、LLaMA 等），`AutoModel.from_pretrained("model-name")` 一行代码获取模型，`pipeline()` 一行代码推理。**

### 5.4.2 Pipeline（一行代码推理）

**下面是 Pipeline（一行代码推理） 的代码示例：**
```python
from transformers import pipeline

# 情感分析
classifier = pipeline("sentiment-analysis")
result = classifier("I love this!")
print(result)  # [{'label': 'POSITIVE', 'score': 0.99}]

# 文本生成
generator = pipeline("text-generation", model="gpt2")
result = generator("Once upon a time", max_length=50)

# 问答
qa = pipeline("question-answering")
result = qa(question="什么是 Python?", context="Python 是一种编程语言...")

# 翻译
translator = pipeline("translation", model="Helsinki-NLP/opus-mt-zh-en")
result = translator("你好世界")
```

### 5.4.3 底层 API

**下面是 底层 API 的代码示例：**
```python
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import torch

model_name = "bert-base-uncased"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSequenceClassification.from_pretrained(model_name)

texts = ["I love Python", "I hate bugs"]
inputs = tokenizer(texts, padding=True, truncation=True,
                    return_tensors="pt", max_length=512)

with torch.no_grad():
    outputs = model(**inputs)
    predictions = torch.softmax(outputs.logits, dim=-1)
```

### 5.4.4 LLM 生成

**下面是 LLM 生成 的代码示例：**
```python
from transformers import AutoModelForCausalLM, AutoTokenizer

model_name = "microsoft/DialoGPT-medium"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForCausalLM.from_pretrained(
    model_name, torch_dtype=torch.float16, device_map="auto"
)

def chat(prompt: str) -> str:
    inputs = tokenizer.encode(prompt, return_tensors="pt").to(model.device)
    outputs = model.generate(
        inputs,
        max_length=100,
        temperature=0.7,
        do_sample=True,
        top_p=0.9,
    )
    return tokenizer.decode(outputs[0], skip_special_tokens=True)
```

### 5.4.5 常见错误

**错误：模型加载时 `device_map` 设置不当**

```python
# 单 GPU
model = AutoModel.from_pretrained("bert-base", device_map="cuda:0")

# 多 GPU 自动分配
model = AutoModel.from_pretrained("llama-7b", device_map="auto")

# CPU only
model = AutoModel.from_pretrained("bert-base", device_map="cpu")
```

### 5.4.6 AI 场景案例：嵌入模型封装

下面将嵌入模型封装成类，统一管理 tokenizer 和 model：
```python
class EmbeddingModel:
    def __init__(self, model_name: str = "BAAI/bge-small-zh-v1.5"):
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModel.from_pretrained(model_name)
        self.model.eval()

    def encode(self, texts: list[str]) -> np.ndarray:
        inputs = self.tokenizer(texts, padding=True, truncation=True,
                                 return_tensors="pt", max_length=512)
        with torch.no_grad():
            embeddings = self.model(**inputs).last_hidden_state[:, 0, :]
        return torch.nn.functional.normalize(embeddings, p=2, dim=1).numpy()
```

---

## 5.5 LangChain

### 5.5.1 一句话核心本质

**LangChain 是 LLM 应用开发框架——用 Chain 串联 Prompt→LLM→Tool，用 Memory 管理对话历史，用 Agent 让 LLM 自主决策调用工具。核心价值是把"调用 API"升级为"编排智能流程"。**

### 5.5.2 Java vs Python 对比

| 维度 | Java | Python/LangChain |
|------|------|------------------|
| LLM 编排框架 | LangChain4j / Spring AI | LangChain (Python 原生态) |
| 链式调用 | 手动编排 Service 层 | `Chain | LCEL 管道符` |
| Prompt 管理 | 字符串拼接 | `ChatPromptTemplate` + Message 类型 |
| 工具注册 | 注解 + 反射 | `@tool` 装饰器 |
| 对话记忆 | 自行管理 Session | `ConversationBufferMemory` |
| Agent 循环 | 手写状态机 | `AgentExecutor` + ReAct |

### 5.5.3 核心架构

```
LCEL (LangChain Expression Language)：
              ┌──────────────┐
Prompt ──────→│  LLM Chain   │──────→ Output Parser ───→ 结构化输出
              │              │
              │ system + user│
              │ messages     │
              └──────────────┘

完整流程：
用户输入 → Prompt 模板(填充变量) → LLM(生成) → OutputParser(解析)
                                              → Memory(存储) → 下轮对话

Agent 流程（ReAct）：
Thought → Action(调用工具) → Observation → Thought → ... → Final Answer
```

### 5.5.4 基础用法

以下是 LangChain 最常用功能的代码示例：
```python
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

llm = ChatOpenAI(model="gpt-4")

# ─── LCEL Chain ───
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个{role}专家，用中文回答"),
    ("human", "{question}"),
])
chain = prompt | llm | StrOutputParser()
result = chain.invoke({"role": "Python", "question": "什么是装饰器？"})

# ─── 对话记忆 ───
from langchain.memory import ConversationBufferMemory
from langchain.chains import ConversationChain

memory = ConversationBufferMemory()
conversation = ConversationChain(llm=llm, memory=memory)
conversation.invoke("我的名字是 AI")
conversation.invoke("我叫什么名字？")  # 还记得之前说的
```

### 5.5.5 Agent

创建一个能调用数学计算和获取时间工具的 Agent：
```python
from langchain.agents import tool, create_tool_calling_agent, AgentExecutor

@tool
def calculate(expression: str) -> str:
    """执行数学计算。输入应为数学表达式如 '2 + 2 * 3'"""
    try:
        return str(eval(expression))
    except Exception as e:
        return f"计算错误: {e}"

@tool
def get_current_time(format: str = "%Y-%m-%d %H:%M:%S") -> str:
    """获取当前日期和时间。format 参数为 strftime 格式，默认 %Y-%m-%d %H:%M:%S"""
    import datetime
    return datetime.datetime.now().strftime(format)

tools = [calculate, get_current_time]
agent_prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个智能助手，可以使用工具回答问题"),
    ("human", "{input}"),
    ("placeholder", "{agent_scratchpad}"),
])
agent = create_tool_calling_agent(llm, tools, agent_prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)

result = agent_executor.invoke({"input": "今天是几号？100 + 200 等于多少？"})
```

### 5.5.6 RAG

用 LangChain 实现完整的 RAG 检索增强生成流程：
```python
from langchain_community.vectorstores import Chroma
from langchain_openai import OpenAIEmbeddings
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.chains import RetrievalQA
from langchain_community.document_loaders import TextLoader

# 1. 加载 → 分块 → 向量化 → 存储
loader = TextLoader("knowledge.txt")
documents = loader.load()

text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=500, chunk_overlap=50, separators=["\n\n", "\n", "。", " ", ""]
)
chunks = text_splitter.split_documents(documents)

vectorstore = Chroma.from_documents(
    documents=chunks,
    embedding=OpenAIEmbeddings()
)

# 2. RAG 链：检索 + 生成
qa_chain = RetrievalQA.from_chain_type(
    llm=ChatOpenAI(model="gpt-4", temperature=0),
    chain_type="stuff",          # 将所有检索结果塞入 prompt
    retriever=vectorstore.as_retriever(search_kwargs={"k": 3}),
    return_source_documents=True, # 返回来源以便追溯
)
result = qa_chain.invoke({"query": "文档中提到了什么关键技术？"})
```

### 5.5.7 常见错误

**错误：LCEL 管道类型不匹配**

```python
# ❌ 错误：prompt 输出是 ChatPromptValue，不能直接传给 llm
chain = prompt | llm  # OK：LangChain 自动转换

# ❌ 错误：invoke 参数与 prompt 变量不匹配
chain.invoke({"name": "AI"})  # 如果 prompt 需要 {role} 和 {question}

# ✅ 修复：确保变量名一致
chain.invoke({"role": "Python", "question": "什么是装饰器？"})
```

### 5.5.8 AI 场景案例：文档问答助手

将 RAG 流程封装为可复用的文档问答助手类：
```python
class DocQAAssistant:
    def __init__(self, llm_model: str = "gpt-4"):
        self.llm = ChatOpenAI(model=llm_model, temperature=0)
        self.vectorstore = None

    def load_documents(self, file_paths: list[str]):
        docs = []
        for path in file_paths:
            loader = TextLoader(path)
            docs.extend(loader.load())
        splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
        chunks = splitter.split_documents(docs)
        self.vectorstore = Chroma.from_documents(chunks, OpenAIEmbeddings())

    def ask(self, question: str) -> str:
        if not self.vectorstore:
            return "请先加载文档"
        retriever = self.vectorstore.as_retriever(search_kwargs={"k": 3})
        relevant_docs = retriever.invoke(question)
        context = "\n\n".join(d.page_content for d in relevant_docs)
        prompt = f"基于以下内容回答问题：\n\n{context}\n\n问题：{question}"
        return self.llm.invoke(prompt).content
```

---

## 5.6 向量数据库 (ChromaDB)

### 5.6.1 一句话核心本质

**向量数据库存储高维嵌入向量并支持 ANN（近似最近邻）搜索——把文本/图片变成数学向量，然后在向量空间中找"语义最接近"的 Top-K 个结果。ChromaDB 是轻量级嵌入式向量数据库，一行代码启动、零配置、适合原型开发。**

### 5.6.2 Java vs Python 对比

| 维度 | Java | Python/ChromaDB |
|------|------|-----------------|
| 向量数据库 | Milvus / Pinecone / Qdrant | ChromaDB（本地嵌入式） |
| 嵌入模型 | 需自行调用 REST API | 内置 `embedding_function` 接口 |
| 搜索算法 | 需配置索引参数 | 自动使用 HNSW（分层可导航小世界） |
| 部署方式 | 需启动独立服务 | `pip install chromadb` 即可使用 |
| 持久化 | 需配置存储 | `PersistentClient(path=...)` |

### 5.6.3 技术原理：ANN 搜索

```
暴力搜索 vs ANN 搜索：
暴力搜索（KNN）：O(n × d)   — 计算所有向量距离
    [v1]→[v2]→[v3]→...→[v100万]
    ↑ 距离 = ||v_q - v_i||²

ANN 搜索（HNSW）：O(log n)
    层级导航图（跳表思想）：
    Layer 3:  v1 ─── v1000 ─── v50000
    Layer 2:  v1 ─ v50 ─ v100 ─ v1000 ─ v5000
    Layer 1:  所有节点，精细邻居
    查询从顶层进入，逐层向下，每次只查局部邻居

余弦距离：
similarity = cos(θ) = (A · B) / (||A|| × ||B||)
值域 [-1, 1]，越大越相似
```

### 5.6.4 基础用法

下面是 ChromaDB 的增删查基本操作：
```python
import chromadb

# ─── 创建客户端 ───
client = chromadb.PersistentClient(path="./chroma_db")

# ─── 创建/获取集合 ───
collection = client.get_or_create_collection(
    name="knowledge_base",
    metadata={"hnsw:space": "cosine"}   # 使用余弦距离
)

# ─── 添加文档 ───
documents = [
    "Transformer 使用自注意力机制处理序列",
    "BERT 是双向编码器，使用 MLM 预训练",
    "GPT 是自回归解码器，使用 Next Token Prediction",
]
metadatas = [
    {"source": "paper", "year": 2017},
    {"source": "paper", "year": 2018},
    {"source": "paper", "year": 2018},
]
ids = [f"doc_{i}" for i in range(len(documents))]

collection.add(
    documents=documents,
    metadatas=metadatas,
    ids=ids
)

# ─── 搜索 ───
results = collection.query(
    query_texts=["什么是注意力机制？"],
    n_results=2,
    where={"source": "paper"},              # 过滤条件
)
```

### 5.6.5 自定义嵌入模型

如果要使用本地嵌入模型而不依赖 OpenAI API，可以实现自定义 EmbeddingFunction：
```python
from chromadb import Documents, EmbeddingFunction, Embeddings
from sentence_transformers import SentenceTransformer

class LocalEmbeddingFunction(EmbeddingFunction):
    def __init__(self, model_name: str = "all-MiniLM-L6-v2"):
        self.model = SentenceTransformer(model_name)

    def __call__(self, input: Documents) -> Embeddings:
        return self.model.encode(input).tolist()

client = chromadb.PersistentClient(path="./chroma_db")
collection = client.get_or_create_collection(
    name="custom_embeddings",
    embedding_function=LocalEmbeddingFunction()
)
```

### 5.6.6 常见错误

**错误：metadata 过滤语法错误**

```python
# ❌ 错误：类型不匹配
collection.query(query_texts=["test"], where={"year": "2018"})  # year 是 int

# ✅ 修复：使用正确类型
collection.query(query_texts=["test"], where={"year": 2018})

# ❌ 错误：集合已存在但 embedding_function 不同
collection = client.create_collection(
    name="docs",
    embedding_function=LocalEmbeddingFunction()  # RuntimeError!
)
# ✅ 修复：已存在则用 get_collection，或先 delete
try:
    collection = client.create_collection(...)
except Exception:
    collection = client.get_collection("docs")
```

### 5.6.7 AI 场景案例：RAG 检索器封装

将向量数据库操作封装为通用检索器：
```python
class VectorRetriever:
    def __init__(self, persist_dir: str = "./chroma_db"):
        self.client = chromadb.PersistentClient(path=persist_dir)
        self.embedder = SentenceTransformer("all-MiniLM-L6-v2")

    def add_texts(self, collection: str, texts: list[str], metadata: list[dict] | None = None):
        coll = self.client.get_or_create_collection(
            name=collection,
            embedding_function=self.embedder.encode
        )
        ids = [f"{collection}_{i}" for i in range(len(texts))]
        coll.add(documents=texts, metadatas=metadata or [{}]*len(texts), ids=ids)

    def query(self, collection: str, query: str, k: int = 5) -> list[tuple[str, float]]:
        coll = self.client.get_collection(name=collection)
        results = coll.query(query_texts=[query], n_results=k)
        return list(zip(results["documents"][0], results["distances"][0]))
```

---

## 5.7 AI Agent

### 5.7.1 一句话核心本质

**AI Agent = LLM（大脑） + Tools（手脚） + Memory（记忆） + Planning（规划）——LLM 自主思考"下一步做什么"并调用工具，形成 ReAct（Reasoning + Acting）循环直到任务完成。**

### 5.7.2 Java vs Python 对比

| 维度 | Java | Python |
|------|------|--------|
| Agent 框架 | LangChain4j / Spring AI | LangChain / CrewAI / AutoGen |
| ReAct 模式 | 手写状态机 | `AgentExecutor` 内置循环 |
| 工具调用 | 接口 + 实现类 + 反射注册 | `@tool` 装饰器自动注册 |
| 多 Agent | 需自建通信机制 | `CrewAI` 一行代码创建团队 |
| 规划能力 | 硬编码流程 | ReAct / Plan-and-Solve / Tree-of-Thought |

### 5.7.3 ReAct 循环

```
ReAct = Reason + Act

循环流程图：
                   ┌──────────────────────────────┐
                   │   Thought(思考): "我需要计算"  │
                   │   Action(行动): calculate()    │
        用户输入───→│   Observation(观察): "42"     │──→ 再思考 → ...
                   │   条件检查: 任务完成了吗?       │
                   └──────────────────────────────┘
                              ↓ 完成
                         Final Answer(最终回答)

示例循环：
1. Thought: 用户问"2024年出生的孩子几岁？"
2. Action: get_current_time(format="%Y")
3. Observation: "2025"
4. Thought: 2025 - 2024 = 1
5. Action: calculate("2025 - 2024")
6. Observation: "1"
7. Final Answer: "1岁"
```

### 5.7.4 手写 Agent

从零实现一个基于 ReAct 循环的 Agent：
```python
import json
from typing import Any

class ReActAgent:
    def __init__(self, llm, tools: list[dict]):
        self.llm = llm
        self.tools = {t["name"]: t for t in tools}
        self.messages = []

    def run(self, task: str, max_steps: int = 10) -> str:
        self.messages.append({"role": "user", "content": task})

        for step in range(max_steps):
            response = self.llm(json.dumps(self.messages))
            action = json.loads(response)

            if action.get("type") == "answer":
                return action["content"]

            tool = self.tools.get(action.get("name", ""))
            if not tool:
                observation = f"错误: 工具 '{action.get('name')}' 不存在"
            else:
                try:
                    observation = tool["func"](**action.get("args", {}))
                except Exception as e:
                    observation = f"工具执行错误: {e}"

            self.messages.extend([
                {"role": "assistant", "content": response},
                {"role": "user", "content": f"观察结果: {observation}"},
            ])

        return f"无法在 {max_steps} 步内完成"

# Usage
def search_web(query: str) -> str:
    return f"搜索 '{query}' 的结果：[模拟结果]"

agent = ReActAgent(
    llm=lambda msgs: json.dumps({
        "type": "tool", "name": "search_web",
        "args": {"query": "Python AI framework"}
    }),
    tools=[{"name": "search_web", "func": search_web}]
)
result = agent.run("查找最好的 Python AI 框架")
```

### 5.7.5 常见错误

**错误：Agent 陷入死循环**

```python
# 症状：Agent 不断调用同一工具但无法前进
# 修复1：设置 max_iterations
agent_executor = AgentExecutor(agent=agent, tools=tools,
                                max_iterations=5,  # 限制循环次数
                                early_stopping_method="generate")  # 超限后生成

# 修复2：工具返回要结构化，便于 Agent 理解
# ❌ 模糊
@tool
def search(q: str) -> str:
    return results  # 无格式

# ✅ 清晰
@tool
def search(q: str) -> str:
    return f"找到 {len(results)} 个结果：{results[:3]}..."
```

### 5.7.6 AI 场景案例：多工具 Agent

构建一个研究员 Agent，同时拥有搜索和计算能力：
```python
class ResearchAgent:
    def __init__(self, llm_model: str = "gpt-4"):
        self.llm = ChatOpenAI(model=llm_model, temperature=0)

    @tool
    def web_search(self, query: str) -> str:
        """搜索网络信息。输入：搜索关键词"""
        return f"[搜索结果] 关于'{query}'的信息..."

    @tool
    def calculator(self, expression: str) -> str:
        """执行数学计算。输入：数学表达式"""
        return str(eval(expression))

    def build(self) -> AgentExecutor:
        tools = [self.web_search, self.calculator]
        prompt = ChatPromptTemplate.from_messages([
            ("system", "你是一个研究员，使用工具查找信息和计算"),
            ("human", "{input}"),
            ("placeholder", "{agent_scratchpad}"),
        ])
        agent = create_tool_calling_agent(self.llm, tools, prompt)
        return AgentExecutor(agent=agent, tools=tools, verbose=True)

agent = ResearchAgent().build()
result = agent.invoke({"input": "搜索 Python 最新版本，然后计算该数字的平方"})
```

---

# 第六阶段：GitHub AI 项目阅读（核心）

---

## 6.1 如何阅读大型 Python 项目

### 6.1.1 一句话核心本质

**阅读 AI 项目跟阅读 Java Spring 项目本质相同——先看 README 定目标 → 找 main/entrypoint 确定启动路径 → 跟踪核心调用链（Router → Service → LLM） → 理解关键抽象（Model/DTO/Autograd）。Python 的动态特性使得阅读难度略高于 Java，但有方法可循。**

### 6.1.2 Java vs Python 项目结构对比

| 维度 | Java 项目 | Python AI 项目 |
|------|-----------|----------------|
| 入口 | `main()` 在 `Application.java` | `main()` 在 `main.py` 或 CLI 脚本 |
| 依赖管理 | `pom.xml` / `build.gradle` | `requirements.txt` / `pyproject.toml` |
| 类型信息 | 编译期强类型，IDE 可直接导航 | 动态类型，需运行时类型提示 (`:type`) |
| 配置 | `application.yml` | 环境变量 + `config.py` + `.env` |
| 构建 | Maven/Gradle | `pip install -e .` / `poetry install` |
| 测试 | `@Test` + JUnit | `pytest` + conftest.py |
| 代码导航 | IDE 类层次结构 | 需 grep `class Xxx` + 函数定义 |

### 6.1.3 如何找入口

```
# 方法1：看 README.md
  ## Quick Start → python main.py --port 8000

# 方法2：搜索 if __name__ == "__main__"
  grep -rn '__main__' src/          # 最常见的入口点

# 方法3：看 pyproject.toml entry_points
  [project.scripts]
  myapp = "myapp.cli:main"          # 命令行入口

# 方法4：看 setup.py
  entry_points={
      "console_scripts": ["myapp = myapp.cli:main"],
  }

# 方法5：搜索 main() 函数
  grep -rn 'def main' src/
```

下面是一个典型 Python AI 项目的入口文件示例：
```python
# 典型入口示例
import argparse
import uvicorn

def main():
    parser = argparse.ArgumentParser(description="AI Chat API")
    parser.add_argument("--port", type=int, default=8000)
    parser.add_argument("--model", type=str, default="gpt-4")
    args = parser.parse_args()
    app = create_app(model_name=args.model)
    uvicorn.run(app, host="0.0.0.0", port=args.port)

if __name__ == "__main__":
    main()
```

### 6.1.4 如何分析调用链

```
# AI 项目典型三层架构

用户请求 ──→ FastAPI Router ──→ Service Layer ──→ LLM/Agent ──→ Response
                │                     │                 │
            参数校验              业务逻辑编排       模型调用/工具执行

# 实际跟踪方法：
# 1. 从 Router 开始
grep -rn '@router\|@app\.' src/routers/

# 2. 找到 Service 调用
grep -rn 'class.*Service' src/services/

# 3. 追踪到 LLM 调用
grep -rn 'openai\|ChatOpenAI\|llm\.' src/services/
```

对应的 API 路由和 Service 层实现：
```python
# routers/chat.py
@router.post("/chat")
async def chat(request: ChatRequest):
    return await chat_service.process(request)

# services/chat_service.py
class ChatService:
    def __init__(self):
        self.llm = ChatOpenAI(model="gpt-4")
        self.retriever = VectorRetriever()

    async def process(self, request):
        context = await self.retriever.query(request.message)
        prompt = f"上下文：{context}\n问题：{request.message}"
        return await self.llm.invoke(prompt)
```

### 6.1.5 Python 特有调试技巧

以下是 Python AI 项目中常用的调试方法：
```python
# 1. print 一行调试（Python 最常用）
def process(data):
    print(f"[DEBUG] data type={type(data).__name__}, len={len(data)}")
    # AI 项目常用：打印 tensor 形状而非全部值
    print(f"[DEBUG] tensor shape={data.shape}, device={data.device}")

# 2. pdb 交互式调试（比 Java 的 System.out 更强）
import pdb

def train_step(batch):
    pdb.set_trace()  # 在此暂停
    # n → 下一行      s → 进入函数
    # p x → 打印变量  l → 查看上下文
    # c → 继续执行    q → 退出
    return model(batch) / divisor

# 3. ipdb 增强版（支持语法高亮和自动补全）
import ipdb; ipdb.set_trace()

# 4. logging 分级调试
import logging
logging.basicConfig(level=logging.DEBUG,
                    format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger(__name__)
logger.debug(f"Input shape: {x.shape}")      # 开发调试
logger.info("Training epoch %d", epoch)       # 运行信息
logger.warning("GPU memory usage high")       # 警告
logger.exception("Training failed")           # 异常 + 完整堆栈
```

### 6.1.6 代码执行跟踪（超越 Java 的能力）

Python 可以跟踪代码执行过程，这在 Java 中需要 AOP 或字节码增强才能实现：
```python
# 1. sys.settrace() 全局函数级跟踪（Java 没有的能力）
import sys

def trace_calls(frame, event, arg):
    if event == "call":
        filename = frame.f_code.co_filename
        func_name = frame.f_code.co_name
        if "my_project" in filename:
            print(f"→ {func_name} at {filename}:{frame.f_lineno}")
    return trace_calls

sys.settrace(trace_calls)

# 2. 装饰器自动跟踪（类似 AOP @Around）
import functools

def trace(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        print(f"→ {func.__name__}(args={args}, kwargs={kwargs})")
        result = func(*args, **kwargs)
        print(f"← {func.__name__} → {type(result).__name__}")
        return result
    return wrapper

@trace
def load_model(path: str):
    return torch.load(path)

# 3. cProfile 性能分析（找瓶颈）
import cProfile, pstats

def profile_func():
    profiler = cProfile.Profile()
    profiler.enable()
    train()  # 被分析的函数
    profiler.disable()
    pstats.Stats(profiler).sort_stats("cumulative").print_stats(20)
    # 输出：ncalls tottime percall cumtime percall ...
```

### 6.1.7 真实项目分析示例

下面用实际项目一步步演示如何分析调用链：
```python
"""
项目：chat_ai（AI 聊天应用）
├── main.py              # 入口
├── app.py               # FastAPI 应用
├── config.py            # 配置管理
├── requirements.txt     # 依赖
├── routers/
│   └── chat.py          # API 路由
├── services/
│   ├── chat.py          # 对话业务
│   └── llm.py           # LLM 调用封装
└── models/
    └── schemas.py       # 请求/响应模型

分析步骤：
Step 1: cat README.md → python main.py --port 8000
Step 2: cat requirements.txt → fastapi, uvicorn, openai, chromadb
Step 3: 看 main.py
Step 4: 跟踪 router → service → llm
"""

# Chapter 6 starts here
from app import create_app

def main():
    app = create_app()
    uvicorn.run(app, host="0.0.0.0", port=8000)

if __name__ == "__main__":
    main()

# Step 4: 看 app.py（路由）
# app.py
from fastapi import FastAPI
from services.chat import ChatService

def create_app():
    app = FastAPI()
    chat_service = ChatService()

    @app.post("/chat")
    async def chat(request: dict):
        return await chat_service.generate(request["message"])

    return app

# Step 5: 看 services/chat.py（核心逻辑）
# services/chat.py
import openai

class ChatService:
    async def generate(self, message: str):
        response = await openai.ChatCompletion.acreate(
            model="gpt-3.5-turbo",
            messages=[{"role": "user", "content": message}]
        )
        return response.choices[0].message.content

# 完整调用链：
# POST /chat {"message": "你好"}
#   → create_app() → @app.post("/chat")
#     → ChatService.generate("你好")
#       → openai.ChatCompletion.acreate(...)
#         → 返回 {"response": "你好！"}
```

### 6.1.8 如何修改 AI 项目

掌握阅读方法后，下面是常见的项目修改场景：
```python
# 场景1：给对话增加记忆
class ChatService:
    def __init__(self):
        self.memory = []  # 新增

    async def generate(self, message: str):
        messages = self.memory + [{"role": "user", "content": message}]
        response = await openai.ChatCompletion.acreate(
            model="gpt-3.5-turbo", messages=messages
        )
        self.memory.append({"role": "user", "content": message})
        self.memory.append({"role": "assistant", "content": response.choices[0].message.content})
        return response.choices[0].message.content

# 场景2：修改 prompt
messages = [
    {"role": "system", "content": "你是一个 Python 编程助手，用中文回答"},
    {"role": "user", "content": message}
]

# 场景3：从 OpenAI 改为本地模型
import requests
response = requests.post(
    "http://localhost:8000/v1/chat/completions",
    json={"model": "local-model", "messages": messages}
)

# 场景4：接入数据库
from sqlalchemy import create_engine, Column, Integer, Text
from sqlalchemy.orm import declarative_base, Session

Base = declarative_base()

class Conversation(Base):
    __tablename__ = "conversations"
    id = Column(Integer, primary_key=True)
    user_message = Column(Text)
    ai_response = Column(Text)

engine = create_engine("sqlite:///chat.db")
Base.metadata.create_all(engine)

class ChatService:
    async def generate(self, message: str):
        response = await self.call_llm(message)
        with Session(engine) as session:
            session.add(Conversation(user_message=message, ai_response=response))
            session.commit()
        return response
```

---

## 总结：从 Java 到 Python 的思维转变

```
Java 思维                Python 思维
─────────                ──────────
静态类型                 动态类型 + typing
编译期检查               运行时 + 静态检查
接口 (Interface)        鸭子类型 (Protocol)
重载 (Overload)         默认参数 / *args / **kwargs
匿名类                   lambda
Stream API              列表推导式 / 生成器
try-with-resources      with 语句
Maven/Gradle             pip / poetry
Spring Boot              FastAPI
JPA/Hibernate            SQLAlchemy
JUnit                    pytest
线程锁/并发包             asyncio
```

**最终检验清单：**
- [ ] 能独立用 Python 写 FastAPI + Pydantic + asyncio 项目
- [ ] 能使用 PyTorch 训练简单模型
- [ ] 能用 Transformers 加载和微调预训练模型
- [ ] 能用 LangChain 构建 RAG 应用
- [ ] 能读懂 GitHub AI 项目的目录结构和调用链
- [ ] 能修改现有 AI 项目的功能和配置
- [ ] 能使用 pytest 编写测试
- [ ] 理解 GIL、asyncio、装饰器等核心机制
