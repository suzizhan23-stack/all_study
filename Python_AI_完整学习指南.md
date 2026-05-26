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
```

**Python 对应代码：所有变量都是堆上 PyObject 引用。`a = 10` 创建一个 PyLongObject(10) 对象，`b = "hello"` 创建一个 PyUnicodeObject，`c = [1,2,3]` 创建一个 PyListObject。每个赋值都在堆上分配新对象，变量只保存指向这些对象的指针（引用）。**

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

### 1.3.3 技术原理

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

**核心规则：** 每个对象的 `ob_type` 指向它的类型，类型的类型是 `type`，`type` 的类型是它自己。

```
内存中完整关系图：

a = 42（实例对象）
  ┌──────────────┐      ob_type
  │ ob_refcnt: 3 │────────────┐
  │ ob_type ─────┼─┐          │
  │ ob_digit: 42 │ │          │
  └──────────────┘ │          │
                   │          ▼
                   │   ┌──────────────────────┐
                   │   │ PyTypeObject ("int")  │  ← 类的实例
                   │   │ tp_name: "int"        │
                   │   │ tp_basicsize: 28      │
                   │   │ tp_dealloc: ...       │
                   │   │ tp_repr: int_repr     │
                   │   │ tp_str: int_to_str    │
                   │   │ tp_as_number: + - *   │  ← 数值运算函数表
                   │   │ ob_type ────────┐     │
                   │   └──────────────────│─────┘
                   │                      │
                   │                      ▼
                   │            ┌──────────────────────┐
                   │            │ PyTypeObject ("type") │  ← 元类
                   │            │ tp_name: "type"       │
                   └────────────│ tp_basicsize: ...     │
                                │ tp_call: type_call    │  ← type() 函数
                                │ tp_new: type_new      │
                                │ ob_type ────────┐     │
                                └──────────────────│─────┘
                                                   │
                                                   ▼
                                ┌──────────────────────┐
                                │ PyTypeObject ("type") │  ← 指向自己
                                │ (同上对象)            │
                                └──────────────────────┘
```


**`PyTypeObject` 内部结构（简化）：**

```
PyTypeObject 模板结构（所有类型共享此布局）：

  +-------------------------------+
  | PyObject_VAR_HEAD             |  ← ob_refcnt + ob_type + ob_size
  +-------------------------------+
  | tp_name (const char*)         |  ← 类型名
  +-------------------------------+
  | tp_basicsize / tp_itemsize    |  ← 实例大小
  +-------------------------------+
  | tp_dealloc / tp_repr         |  ← 析构 / __repr__
  | tp_str / tp_hash / tp_compare|  ← __str__ / __hash__ / __eq__
  +-------------------------------+
  | tp_as_number / tp_as_sequence|  ← 数值 / 序列 操作函数表
  | tp_as_mapping                 |  ← 映射操作函数表
  +-------------------------------+
  | tp_call / tp_new / tp_init   |  ← 调用 / 创建 / 初始化
  +-------------------------------+
  | tp_mro / tp_bases / tp_dict   |  ← MRO / 基类 / 属性字典
  +-------------------------------+
```

内置类型 `int` 实例：                        用户自定义类 `Dog` 实例：

```
  +-------------------------------+          +-------------------------------+
  | ob_refcnt: 42                 |          | ob_refcnt: 3                  |
  | ob_type ───→ type            |          | ob_type ───→ type            |
  | ob_size: 0 (定长)            |          | ob_size: 0 (定长)            |
  +-------------------------------+          +-------------------------------+
  | tp_name: "int"                |          | tp_name: "Dog"                |
  +-------------------------------+          +-------------------------------+
  | tp_basicsize: 28              |          | tp_basicsize: 16              |
  | tp_itemsize: 0                |          | tp_itemsize: 0                |
  +-------------------------------+          +-------------------------------+
  | tp_dealloc: int_dealloc       |          | tp_dealloc: subtype_dealloc   |
  | tp_repr: int_repr             |          | tp_repr: object_repr          |
  | tp_hash: int_hash             |          | tp_hash: PyObject_HashNotImpl |
  +-------------------------------+          +-------------------------------+
  | tp_as_number:                  |          | tp_as_number: NULL            |
  |   nb_add → int_add           |          | tp_as_sequence: NULL          |
  |   nb_sub → int_sub           |          | tp_as_mapping: NULL           |
  |   nb_mul → int_mul           |          |                               |
  | tp_as_sequence: NULL          |          |                               |
  | tp_as_mapping: NULL           |          |                               |
  +-------------------------------+          +-------------------------------+
  | tp_new: int_new                |          | tp_new: type_new              |
  | tp_init: NULL (int 无需)      |          | tp_init: Dog.__init__ (默认)  |
  +-------------------------------+          +-------------------------------+
  | tp_mro: [int, object]          |          | tp_mro: [Dog, object]         |
  | tp_dict: {__add__: slot...}   |          | tp_dict: {bark: <func>}       |
  +-------------------------------+          +-------------------------------+
```

**关键区别：**
- `int` 是 C 实现的**内置类型**，`tp_as_number` 挂载了 C 函数指针，性能高
- `Dog` 是 Python 代码的**用户类**，`tp_dict` 直接存储 Python 函数对象，无 tp_as_number
- 两者都是 `PyTypeObject` 实例，`ob_type` 都指向 `type`——元类统一了所有类型
- `type` 自身也是 `PyTypeObject`，它的 `tp_name = "type"`, `ob_type` 指向自己

#### 1.3.3.2 `type` 是什么？
- `type` 是 Python 的**元类**（metaclass），是所有类的类
- `type` 本身是一个 `PyTypeObject` 实例
- `type` 的 `ob_type` 指向它自己（闭环）
- `type(name, bases, dict)` 可以**动态创建类**

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


#### 1.3.3.3 为什么 Python 对象模型比 Java 更灵活？
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

#### 1.3.3.4 mutable vs immutable

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

#### 1.3.3.5 Python 内存大小逻辑与对齐

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

### 1.4.6 函数内存模型与运行流程

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

# 字典推导式
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

```python
def calculate_ber(bit_rate: float, noise: float) -> float:
    """
    计算误比特率 (BER)。

    参数:
        bit_rate (float): 比特率 (bps)
        noise (float): 噪声功率 (dB)

    返回:
        float: 误比特率

    示例:
        >>> calculate_ber(1e6, -20)
        0.001
    """
    return 0.001

# 查看文档
print(calculate_ber.__doc__)
help(calculate_ber)
```

### 1.4.14 常见错误

```python
# 1. lambda 闭包陷阱
funcs = [lambda: i for i in range(5)]
print([f() for f in funcs])  # [4,4,4,4,4] ← 所有函数都返回最后一个 i

# 原因：lambda 捕获的是变量 i 的引用，循环结束时 i=4
# 正确：
funcs = [lambda x=i: x for i in range(5)]
print([f() for f in funcs])  # [0,1,2,3,4]

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

### 1.5.2 模块基础

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

### 1.5.4 导入流程

```
用户写: import requests
              ↓
1. 检查 sys.modules（模块缓存）
   → 如果已导入，直接复用
              ↓
2. 搜索 sys.path 列表找到模块文件
              ↓
3. 找到文件后：
   a. 创建 Module 对象（类型为 module）
   b. 编译源码为字节码 (.pyc)
   c. 执行模块代码（填充模块的 __dict__）
              ↓
4. 将模块存入 sys.modules["requests"]
              ↓
5. 当前命名空间绑定 "requests" 变量
```

**代码演示：通过 `sys.path` 查看 Python 模块搜索路径列表，顺序为：当前脚本目录 → PYTHONPATH 环境变量 → 标准库 → site-packages。通过 `sys.modules` 查看已导入模块缓存字典，可验证模块只加载一次的机制。这是排查 import 错误的基础工具。**

```python
import sys

# 查看模块搜索路径
print(sys.path)
# ['当前目录', 'PYTHONPATH', '标准库', 'site-packages']

# 查看已导入的模块
print(list(sys.modules.keys())[:10])

# 模块缓存验证
import random
print("random" in sys.modules)  # True ← 第二次 import 直接复用

# 重新加载模块（开发调试时使用）
import importlib
importlib.reload(random)
```

**`sys.path` 搜索顺序：**
```
1. 执行脚本所在目录（或当前目录）
2. PYTHONPATH 环境变量中的路径
3. Python 标准库路径（如 /usr/lib/python3.11/）
4. site-packages 第三方包路径（如 /usr/lib/python3.11/site-packages/）
```

### 1.5.5 `__name__` 和 `__main__`

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

**代码演示：Python 包的四种核心导入方式。`import my_project.utils.helpers` 使用完整路径导入；`from my_project.utils import helpers` 直接导入子模块到当前命名空间；`from my_project.utils.helpers import format_date` 直接导入具体函数；`from my_project import models` 导入子包。`__init__.py` 标记目录为包，可在其中定义 `__all__` 控制 `from package import *` 的行为，或做包的初始化工作。**

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

```python
# pip 安装的第三方包存放在 site-packages 目录
# pip list       → 列出已安装包
# pip install x  → 安装包
# pip freeze     → 导出当前环境所有包信息

# 虚拟环境（venv）为每个项目创建独立的 site-packages
# python -m venv .venv
# source .venv/bin/activate  # 激活后 pip install 安装到此环境
```

### 1.5.10 常见错误

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

**类创建流程：**
```
class Dog:
    species = "Canine"
    def bark(self): ...
        ↓
1. 收集类定义体中的所有名称 → namespace dict
        ↓
2. 确定元类（默认为 type）
        ↓
3. 调用 type.__new__(type, "Dog", (object,), namespace)
   → 创建 Dog 类对象（PyTypeObject 实例）
        ↓
4. 调用 type.__init__(Dog, ...)
        ↓
5. 将 Dog 绑定到当前命名空间
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

```
D 的 MRO 计算（C3 线性化）：

class D(B, C):
  ┌─────┐ ┌─────┐ ┌─────┐
  │  D  │ │  B  │ │  C  │ ┌───────┐
  │     │→│who=B│→│who=C│→│object │
  └─────┘ └──┬──┘ └──┬──┘ └───────┘
              │       │
              ▼       ▼
            ┌──────────┐
            │    A     │
            │  who=A   │
            └──────────┘

规则：D(B, C) 的 MRO =
  1. D 本身
  2. B 的 MRO（不含 D 已出现的类）
  3. C 的 MRO（不含 D 已出现的类）
  4. object
结果: D → B → C → A → object
```

**代码演示：`super()` 的正确用法。Python 的 `super()` 不是调用"父类"，而是调用 MRO 中的下一个类。在菱形继承 `D(MixinA, MixinB, Base)` 中，`MixinA.__init__` 里的 `super().__init__()` 会跳过 MixinA 本身，调用 MRO 链中的下一个 `MixinB.__init__`，再通过 `super()` 到达 `Base.__init__`。这种"协作式多重继承"要求所有类都调用 `super().__init__()` 才能正确串联初始化链。**

```python
# super() 的正确使用
class Base:
    def __init__(self):
        print("Base.__init__")

class MixinA:
    def __init__(self):
        print("MixinA.__init__")
        super().__init__()  # 不是调用父类！是调用 MRO 中的下一个！

class MixinB:
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

# 输出：
# Derived.__init__
# MixinA.__init__
# MixinB.__init__
# Base.__init__
# super() 沿着 MRO 链调用下一个类！
```

### 1.6.6 封装与属性

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

```python
# with 语句的本质是异常安全的资源管理
# __exit__ 接收异常信息

class DatabaseConnection:
    def __enter__(self):
        print("连接数据库")
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        # exc_type: 异常类型（没有异常则为 None）
        # exc_val:  异常实例
        # exc_tb:   堆栈跟踪
        print("关闭连接")
        if exc_type is not None:
            print(f"发生异常: {exc_val}")
            # return False  → 不抑制异常（默认）
            # return True   → 抑制异常，不传播
        return False  # 不抑制异常

with DatabaseConnection() as conn:
    raise ValueError("查询失败")  # 先执行 __exit__，再传播异常
```

### 1.7.9 EAFP vs LBYL

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

```python
import json

# 写 JSON
data = {
    "model": "gpt-4",
    "parameters": {
        "temperature": 0.7,
        "max_tokens": 2048
    },
    "tools": ["search", "calculate"]
}

with open("config.json", "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
    # indent=2 → 格式化缩进
    # ensure_ascii=False → 保留非 ASCII 字符

# 读 JSON
with open("config.json", "r", encoding="utf-8") as f:
    loaded = json.load(f)

# 字符串 ↔ Python 对象
json_str = json.dumps(data, indent=2)
parsed = json.loads(json_str)
```

### 1.8.6 pathlib（推荐）

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

### 1.8.7 CSV 读写

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

**装饰器是接受函数返回增强函数的高阶函数，`@decorator` 等价于 `func = decorator(func)`。**

### 2.1.2 基本用法

```python
def log_calls(func):
    def wrapper(*args, **kwargs):
        print(f"调用: {func.__name__}")
        return func(*args, **kwargs)
    return wrapper

@log_calls
def add(a, b):
    return a + b

add(1, 2)  # 调用: add
```

### 2.1.3 带参数装饰器

```python
def repeat(n: int):
    def decorator(func):
        def wrapper(*args, **kwargs):
            for _ in range(n):
                result = func(*args, **kwargs)
            return result
        return wrapper
    return decorator

@repeat(3)
def greet(name):
    print(f"Hello {name}")
```

### 2.1.4 functools.wraps

```python
import functools

def log_calls(func):
    @functools.wraps(func)  # 保留 __name__, __doc__
    def wrapper(*args, **kwargs):
        print(f"调用: {func.__name__}")
        return func(*args, **kwargs)
    return wrapper
```

### 2.1.5 AI 场景案例

```python
import time, functools, logging
logger = logging.getLogger(__name__)

def retry(max_attempts=3, delay=1):
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            for attempt in range(max_attempts):
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    if attempt == max_attempts - 1:
                        raise
                    time.sleep(delay * (2 ** attempt))
        return wrapper
    return decorator

def timer(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        logger.info(f"{func.__name__} 耗时: {time.perf_counter()-start:.3f}s")
        return result
    return wrapper

class AIService:
    @retry(max_attempts=3)
    @timer
    def call_llm(self, prompt: str) -> str:
        return openai.ChatCompletion.create(...)
```

---

## 2.2 生成器

### 2.2.1 一句话核心本质

**生成器是"惰性可迭代对象"——函数中有 `yield` 关键字，每次迭代执行到下一个 `yield` 暂停。**

```python
def count_up_to(n):
    i = 0
    while i < n:
        yield i
        i += 1

for num in count_up_to(5):
    print(num)  # 0 1 2 3 4
```

### 2.2.2 原理

```
def my_gen():
    print("开始")
    yield 1
    print("结束")

gen = my_gen()
next(gen)  # 打印"开始"，返回 1
next(gen)  # 打印"结束"，StopIteration
```

### 2.2.3 生成器 vs 列表

```python
import sys
list_squares = [x**2 for x in range(1000)]     # ~8856 bytes
gen_squares = (x**2 for x in range(1000))      # ~112 bytes
```

### 2.2.4 yield from

```python
def sub():
    yield 1
    yield 2

def main():
    yield "start"
    yield from sub()
    yield "end"
```

### 2.2.5 AI 场景案例

```python
def stream_llm_response(prompt: str):
    response = f"这是对'{prompt}'的回复..."
    for i in range(0, len(response), 10):
        yield response[i:i+10]
        time.sleep(0.1)

def batch_generator(data, batch_size=32):
    batch = []
    for item in data:
        batch.append(item)
        if len(batch) == batch_size:
            yield batch
            batch = []
    if batch:
        yield batch
```

---

## 2.3 迭代器

### 2.3.1 一句话核心本质

**实现了 `__iter__` 和 `__next__` 协议的对象，`for` 循环本质是不断调用 `next()` 直到 `StopIteration`。**

```python
class Range:
    def __init__(self, start, end):
        self.start = start

    def __iter__(self):
        return RangeIterator(self.start, self.end)

class RangeIterator:
    def __init__(self, start, end):
        self.current = start
        self.end = end

    def __next__(self):
        if self.current >= self.end:
            raise StopIteration
        self.current += 1
        return self.current - 1
```

### 2.3.2 for 循环本质

```python
# for x in iterable:
#     do_something(x)

# 等价于：
iterator = iter(iterable)
while True:
    try:
        x = next(iterator)
        do_something(x)
    except StopIteration:
        break
```

### 2.3.3 itertools

```python
from itertools import chain, cycle, islice

chain([1,2], [3,4])           # [1,2,3,4]
islice(range(1000), 5)        # 0,1,2,3,4
```

---

## 2.4 魔术方法

### 2.4.1 运算符重载

```python
class Vector:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __add__(self, other):
        return Vector(self.x + other.x, self.y + other.y)

    def __mul__(self, other):
        if isinstance(other, (int, float)):
            return Vector(self.x * other, self.y * other)

    def __eq__(self, other):
        return self.x == other.x and self.y == other.y

    def __repr__(self):
        return f"Vector({self.x}, {self.y})"

    def __abs__(self):
        return (self.x**2 + self.y**2)**0.5

    def __bool__(self):
        return bool(self.x or self.y)
```

### 2.4.2 容器模拟

```python
class CustomList:
    def __init__(self, items):
        self._items = list(items)

    def __len__(self): return len(self._items)
    def __getitem__(self, i): return self._items[i]
    def __setitem__(self, i, v): self._items[i] = v
    def __iter__(self): return iter(self._items)
    def __contains__(self, item): return item in self._items
```

### 2.4.3 可调用对象

```python
class Double:
    def __call__(self, x):
        return x * 2

d = Double()
print(d(5))  # 10
```

### 2.4.4 AI 场景案例

```python
class AIConfig:
    def __init__(self, config: dict):
        self._config = config

    def __getattr__(self, name):
        if name in self._config:
            return self._config[name]
        raise AttributeError(f"{name} 不存在")

    def __getitem__(self, key):
        return self._config[key]

    def __contains__(self, key):
        return key in self._config

config = AIConfig({"model": "gpt-4"})
print(config.model)     # gpt-4
print(config["model"])  # gpt-4
```

---

## 2.5 元类

### 2.5.1 一句话核心本质

**元类是"类的类"——类定义对象的行为，元类定义类的创建行为。**

```python
class MyClass: pass
obj = MyClass()
print(type(obj))       # <class 'MyClass'>
print(type(MyClass))   # <class 'type'>
```

### 2.5.2 类创建流程

```
class MyClass(Base, metaclass=Meta):
    attr = 42
        ↓
1. 收集属性和方法
2. 确定元类
3. MyClass = metaclass.__new__(metaclass, "MyClass", (Base,), namespace)
4. metaclass.__init__(MyClass, ...)
```

### 2.5.3 单例元类

```python
class SingletonMeta(type):
    _instances = {}
    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            cls._instances[cls] = super().__call__(*args, **kwargs)
        return cls._instances[cls]

class Database(metaclass=SingletonMeta):
    def __init__(self):
        print("初始化")

db1 = Database()  # 初始化
db2 = Database()  # 复用
print(db1 is db2)  # True
```

### 2.5.4 常见错误

```python
# 过度使用元类，99% 可用 __init_subclass__ 替代
class BaseModel:
    def __init_subclass__(cls, **kwargs):
        print(f"创建模型: {cls.__name__}")

class MyModel(BaseModel): pass
# 创建模型: MyModel
```

---

## 2.6 typing

### 2.6.1 一句话核心本质

**类型注解是"可选"的元数据，不影响运行时行为，但可被 mypy/pyright 静态检查。**

### 2.6.2 Java vs Python 对比

```java
// Java 编译期强制检查
public String greet(String name, int count) { ... }
```

**Python 对应代码：Python 类型注解只在写代码时提供提示，运行时不做任何检查。`def greet(name: str, count: int) -> str` 中所有注解都是 `__annotations__` 字典中的元数据，CPython 虚拟机直接忽略。`greet(42, "hello")` 传入 int 和 str 仍然正常运行。这与 Java 编译强制检查的本质区别，需要依赖 `mypy` / `pyright` 等外部工具做静态检查。**

```python
# Python 运行时不检查
def greet(name: str, count: int) -> str:
    return f"{name} says {count}"

print(greet(42, "hello"))  # 运行正常！但逻辑错误
```

### 2.6.3 类型注解语法

```python
from typing import List, Dict, Optional, Union, Any, Callable
from typing import TypeVar, Generic, Protocol, Literal, TypedDict

def process(names: List[str]) -> Dict[str, int]:
    return {n: len(n) for n in names}

def find(id: int) -> Optional[str]:
    return "Alice" if id == 1 else None

def handle(v: Union[int, str]) -> str:
    return str(v)

T = TypeVar("T")
def first(items: List[T]) -> T:
    return items[0]

class Stack(Generic[T]):
    def push(self, item: T): ...
    def pop(self) -> T: ...
```

### 2.6.4 Protocol

```python
class Quackable(Protocol):
    def quack(self) -> str: ...

def make_it_quack(thing: Quackable):
    print(thing.quack())

make_it_quack(Duck())    # 结构匹配，无需继承
```

### 2.6.5 常见错误

```python
# 误以为类型注解会运行时检查
def add(a: int, b: int) -> int:
    return a + b
add("hello", "world")  # 运行正常！
```

---

## 2.7 dataclass

### 2.7.1 一句话核心本质

**`@dataclass` 自动生成 `__init__`、`__repr__`、`__eq__`、`__hash__`。**

```python
from dataclasses import dataclass, field
from typing import List, Optional

@dataclass
class AIConfig:
    model_name: str
    temperature: float = 0.7
    tags: List[str] = field(default_factory=list)
    api_key: str = field(default="", repr=False)

    def __post_init__(self):
        if self.temperature < 0 or self.temperature > 2:
            raise ValueError("temperature 必须在 0-2 之间")

config = AIConfig(model_name="gpt-4", tags=["llm"])
print(config)  # AIConfig(model_name='gpt-4', temperature=0.7, tags=['llm'])
```

### 2.7.2 常见错误

```python
@dataclass
class Wrong:
    items: List[str] = []  # 所有实例共享同一列表！

@dataclass
class Right:
    items: List[str] = field(default_factory=list)
```

---

## 2.8 上下文管理器

### 2.8.1 一句话核心本质

**实现了 `__enter__` 和 `__exit__` 协议的对象，配合 `with` 语句自动管理资源。**

```python
class ManagedFile:
    def __enter__(self):
        self.file = open("test.txt", "w")
        return self.file

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.file.close()

with ManagedFile() as f:
    f.write("Hello")
```

### 2.8.2 contextlib

```python
from contextlib import contextmanager

@contextmanager
def timer(name: str = "操作"):
    import time
    start = time.perf_counter()
    try:
        yield
    finally:
        print(f"{name} 耗时: {time.perf_counter() - start:.3f}s")

with timer("AI 调用"):
    call_llm("Hello")
```

---

# 第三阶段：并发与网络

---

## 3.1 threading

### 3.1.1 一句话核心本质

**Python 线程是操作系统原生线程，但由于 GIL，同一时刻只有一个线程能执行 Python 字节码。**

### 3.1.2 GIL 详解

```
GIL (Global Interpreter Lock)
        ↓
CPython 内存管理不是线程安全的
        ↓
一次只有一个线程执行字节码
        ↓
多线程不能利用多核 CPU
        ↓
CPU 密集型 → multiprocessing
I/O 密集型 → threading/asyncio
```

### 3.1.3 基础用法

```python
import threading
import time

def worker(name):
    print(f"线程 {name} 启动")
    time.sleep(1)
    print(f"线程 {name} 结束")

threads = [threading.Thread(target=worker, args=(f"T{i}",)) for i in range(3)]
for t in threads:
    t.start()
for t in threads:
    t.join()
```

### 3.1.4 线程同步

```python
import threading

# Lock
lock = threading.Lock()
counter = 0

def increment():
    global counter
    for _ in range(100000):
        with lock:
            counter += 1

# Event
event = threading.Event()

def waiter():
    print("等待...")
    event.wait()
    print("收到事件！")
```

### 3.1.5 AI 场景案例

```python
import threading
import queue
import requests

class AIRequestQueue:
    def __init__(self, api_key: str, num_workers: int = 3):
        self.queue = queue.Queue()
        self.api_key = api_key
        for _ in range(num_workers):
            t = threading.Thread(target=self._worker, daemon=True)
            t.start()

    def _worker(self):
        while True:
            prompt = self.queue.get()
            try:
                requests.post("https://api.openai.com/v1/chat/completions",
                    headers={"Authorization": f"Bearer {self.api_key}"},
                    json={"model": "gpt-4", "messages": [{"role": "user", "content": prompt}]})
            finally:
                self.queue.task_done()

    def submit(self, prompt: str):
        self.queue.put(prompt)
```

### 3.1.6 常见错误

```python
counter = 0
def increment_bad():
    global counter
    for _ in range(100000):
        counter += 1  # 非原子操作！需要加锁
```

---

## 3.2 multiprocessing

### 3.2.1 一句话核心本质

**通过创建子进程绕开 GIL，每个进程拥有独立 Python 解释器，能真正利用多核 CPU。**

```python
import multiprocessing as mp
import time

def cpu_heavy(n):
    return sum(i * i for i in range(n))

# 多进程（4 核加速比 ≈ 4x）
with mp.Pool(4) as pool:
    results = pool.map(cpu_heavy, [10**7] * 4)
```

### 3.2.2 进程通信

```python
# Queue
q = mp.Queue()
def worker(q):
    q.put("子进程数据")

p = mp.Process(target=worker, args=(q,))
p.start()
print(q.get())

# Manager（共享对象）
with mp.Manager() as manager:
    shared_dict = manager.dict()
```

### 3.2.3 AI 场景案例

```python
class ParallelInferenceEngine:
    def __init__(self, model_path: str, num_workers: int = 4):
        self.pool = mp.Pool(num_workers)
        self.model_path = model_path

    def predict(self, texts: list):
        chunk_size = len(texts) // self.num_workers
        chunks = [texts[i:i+chunk_size] for i in range(0, len(texts), chunk_size)]
        results = self.pool.starmap(batch_inference, [(self.model_path, c) for c in chunks])
        return [r for cr in results for r in cr]
```

### 3.2.4 常见错误

```python
# 必须放在 if __name__ 中
if __name__ == "__main__":
    p = mp.Process(target=worker)
    p.start()
```

---

## 3.3 asyncio

### 3.3.1 一句话核心本质

**基于事件循环的单线程并发框架，通过 `async/await` 实现协作式多任务，适合 I/O 密集型场景。**

### 3.3.2 Java vs Python 对比

```java
// Java CompletableFuture
CompletableFuture.supplyAsync(() -> fetchData())
    .thenApply(data -> process(data))
```

**Python 对应代码：Python 用 `async/await` 实现异步编程，语法上比 Java CompletableFuture 的链式回调更直观。`async def` 定义协程函数，`await` 挂起当前协程等待异步结果返回。Python 的 await 等价于 Java 的 `thenApply`/`thenCompose`，但写起来像同步代码。底层由 Event Loop（事件循环）调度协程，而非线程池。**

```python
# Python asyncio
async def main():
    data = await fetch_data()
    result = await process(data)
# Java CompletableFuture.thenApply → Python await
# Java Netty EventLoop → Python asyncio EventLoop
```

### 3.3.3 核心概念

```
Event Loop
    ↓
┌──────────────┐
│  Ready Queue  │ ← 等待执行的协程
└──────┬───────┘
       ↓
┌──────────────┐
│  I/O Polling  │ ← epoll/select
└──────┬───────┘
       ↓
┌──────────────┐
│  Callback     │ ← 协程恢复
└──────────────┘

Coroutine → async def, 可暂停/恢复
await     → 挂起当前协程
Task      → 协程调度单元
Event Loop → "操作系统"
```

### 3.3.4 基础语法

```python
import asyncio

async def hello():
    print("Hello")
    await asyncio.sleep(1)
    print("World")

asyncio.run(hello())

async def main():
    # gather 并行
    results = await asyncio.gather(hello(), hello(), hello())

    # create_task
    task = asyncio.create_task(hello())
    await task

    # wait
    done, pending = await asyncio.wait([task], timeout=5.0)
```

### 3.3.5 同步原语

```python
import asyncio

# Lock
lock = asyncio.Lock()
async def update(key, value):
    async with lock:
        shared[key] = value

# Semaphore（限制并发数）
sem = asyncio.Semaphore(5)
async def limited_request(url):
    async with sem:
        return await fetch(url)

# Queue
queue = asyncio.Queue()
async def consumer():
    while True:
        item = await queue.get()
        print(f"处理: {item}")
        queue.task_done()
```

### 3.3.6 AI 场景案例

```python
import asyncio
import aiohttp

class AsyncLLMClient:
    def __init__(self, api_key: str):
        self.api_key = api_key

    async def __aenter__(self):
        self.session = aiohttp.ClientSession(
            headers={"Authorization": f"Bearer {self.api_key}"}
        )
        return self

    async def __aexit__(self, *args):
        await self.session.close()

    async def generate(self, prompt: str) -> str:
        payload = {"model": "gpt-4",
                   "messages": [{"role": "user", "content": prompt}]}
        async with self.session.post(
            "https://api.openai.com/v1/chat/completions", json=payload
        ) as response:
            data = await response.json()
            return data["choices"][0]["message"]["content"]

    async def batch_generate(self, prompts: list) -> list:
        tasks = [self.generate(p) for p in prompts]
        return await asyncio.gather(*tasks, return_exceptions=True)

async def main():
    async with AsyncLLMClient("sk-xxx") as client:
        results = await client.batch_generate(["讲个故事", "写首诗"])
        # 2 个请求并行，总耗时 ≈ 单个请求耗时

asyncio.run(main())
```

### 3.3.7 常见错误

```python
# 在异步函数中阻塞
async def bad():
    import time
    time.sleep(5)  # 阻塞整个 EventLoop！

# 正确：
async def good():
    await asyncio.sleep(5)

# 忘记 await
async def main():
    coro = async_func()  # 只是创建了协程对象！
    result = await coro  # 正确
```

---

## 3.4 FastAPI

### 3.4.1 一句话核心本质

**基于 Python 类型注解和 asyncio 的高性能 Web 框架，自动生成 OpenAPI 文档，AI 项目首选。**

### 3.4.2 Java vs Python 对比

```python
# Java Spring Boot: @RestController, @PostMapping
# Python FastAPI:
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

class ChatRequest(BaseModel):
    message: str
    temperature: float = 0.7

@app.post("/api/chat")
async def chat(req: ChatRequest):
    return {"response": await ai_service.chat(req.message)}
```

### 3.4.3 核心特性

```python
from fastapi import FastAPI, Depends, HTTPException, Query
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from typing import Optional, List
import json

app = FastAPI(title="AI Service")

class ChatRequest(BaseModel):
    model: str = "gpt-4"
    messages: List[dict]
    temperature: float = Field(default=0.7, ge=0, le=2)

async def verify_key(api_key: str = Query(...)):
    if api_key != "valid":
        raise HTTPException(401, "Unauthorized")
    return api_key

@app.post("/v1/chat/completions")
async def chat_completion(
    req: ChatRequest,
    key: str = Depends(verify_key)
):
    return await llm_service.generate(req.messages)

# 流式响应
@app.post("/v1/chat/stream")
async def chat_stream(req: ChatRequest):
    async def generate():
        async for chunk in llm_service.stream_generate(req.messages):
            yield f"data: {json.dumps(chunk)}\n\n"
        yield "data: [DONE]\n"
    return StreamingResponse(generate(), media_type="text/event-stream")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
```

### 3.4.4 常见错误

```python
@app.get("/predict")
async def predict(text: str):
    # time.sleep(5)  # 阻塞 EventLoop！
    result = await run_in_threadpool(model.predict, text)
    return result
```

---

# 第四阶段：工程化

---

## 4.1 pip & requirements.txt

### 4.1.1 一句话核心本质

**`pip` 是 Python 官方包管理器，从 PyPI 下载安装第三方包；`requirements.txt` 记录项目依赖。**

### 4.1.2 Java vs Python 对比

```java
// Maven pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.2.0</version>
</dependency>
```

**Python 对应依赖管理：`requirements.txt` 是 Python 最基础的依赖声明文件，等价于 Maven 的 `pom.xml`。每行一个依赖包，`==` 锁定精确版本，`>=` 指定最低版本，支持版本范围。配合 `pip install -r requirements.txt` 一次性安装所有依赖。与 Maven 自动解析传递依赖不同，pip 不会自动锁定传递依赖版本，通常结合 `pip freeze > requirements.txt` 生成完整锁定文件。**

```bash
# requirements.txt
fastapi==0.104.1
uvicorn==0.24.0
pydantic>=2.0,<3.0
```

### 4.1.3 AI 项目 requirements.txt

```txt
transformers>=4.36.0
torch>=2.1.0
langchain>=0.1.0
chromadb>=0.4.0
fastapi>=0.104.0
uvicorn[standard]>=0.24.0
gradio>=4.0.0
openai>=1.6.0
python-dotenv>=1.0.0
```

---

## 4.2 venv（虚拟环境）

### 4.2.1 使用

```bash
python -m venv venv                  # 创建
source venv/bin/activate             # Linux 激活
venv\Scripts\activate                # Windows 激活
deactivate                           # 退出
rm -rf venv                          # 删除
```

### 4.2.2 原理

```
venv/
├── bin/
│   ├── python          # 指向系统 Python 的符号链接
│   ├── pip
│   └── activate
└── lib/python3.x/site-packages/  # 独立包目录
```

---

## 4.3 poetry

### 4.3.1 一句话核心本质

**Poetry 是 Python 现代依赖管理和打包工具，统一了 `requirements.txt` 和 `setup.py`，使用 `pyproject.toml`。**

### 4.3.2 pyproject.toml

```toml
[tool.poetry]
name = "ai-agent"
version = "0.1.0"
description = "AI Agent 框架"
authors = ["dev@example.com"]

[tool.poetry.dependencies]
python = "^3.11"
fastapi = "^0.104.0"
openai = "^1.6.0"
transformers = "^4.36.0"
torch = {version = "^2.1.0", optional = true}

[tool.poetry.group.dev.dependencies]
pytest = "^7.4.0"
mypy = "^1.7.0"
ruff = "^0.1.0"

[build-system]
requires = ["poetry-core"]
build-backend = "poetry.core.masonry.api"
```

### 4.3.3 常用命令

```bash
poetry new my-project       # 创建项目
poetry init                 # 初始化
poetry install              # 安装依赖
poetry add fastapi          # 添加依赖
poetry shell                # 进入虚拟环境
poetry run python main.py   # 运行
poetry build                # 构建
```

---

## 4.4 pytest

### 4.4.1 一句话核心本质

**Python 最流行的测试框架，支持简单 assert、自动发现测试、fixture 和插件系统。**

### 4.4.2 Java vs Python 对比

```java
// JUnit
@Test
public void testAddition() {
    assertEquals(5, calc.add(2, 3));
}
```

**Python 等价测试代码：pytest 使用纯 `assert` 语句替代 JUnit 的 `assertEquals` 方法。Python 的 `assert` 失败时会自动输出表达式值和上下文，无需额外断言 API。测试函数以 `test_` 前缀命名，pytest 按约定自动发现，等价于 JUnit 的 `@Test` 注解 + `public void test*()` 命名约定。**

```python
# pytest
def test_addition():
    assert calc.add(2, 3) == 5
```

### 4.4.3 基础用法

```python
# test_math.py（自动发现）

def test_addition():
    assert 1 + 1 == 2

def test_raises():
    import pytest
    with pytest.raises(ValueError):
        int("abc")

@pytest.mark.parametrize("a,b,expected", [
    (1, 2, 3), (0, 0, 0), (-1, 1, 0)
])
def test_add(a, b, expected):
    assert add(a, b) == expected

@pytest.mark.slow
def test_heavy():
    import time
    time.sleep(10)
```

### 4.4.4 fixture

```python
import pytest

@pytest.fixture
def model():
    return {"name": "test-model", "version": "1.0"}

def test_model_name(model):
    assert model["name"] == "test-model"

@pytest.fixture
def ai_client():
    client = AsyncLLMClient("test-key")
    yield client
    client.close()

@pytest.mark.asyncio
async def test_ai_call(ai_client):
    result = await ai_client.generate("hello")
    assert result is not None
```

### 4.4.5 运行

```bash
pytest                        # 自动发现
pytest -v                     # 详细输出
pytest -k "test_add"          # 按名称过滤
pytest -m slow                # 按标记过滤
pytest --cov=src tests/       # 覆盖率
```

---

# 第五阶段：AI 工程（重点）

---

## 5.1 NumPy

### 5.1.1 一句话核心本质

**NumPy 是 Python 数值计算基础库，核心是 `ndarray`，底层 C 实现，提供向量化运算和广播机制。**

### 5.1.2 ndarray 内存模型

```
Python List: [PyObject*, PyObject*, PyObject*, ...]
每个元素是 8 字节指针，指向堆上对象

NumPy Array: [float64, float64, float64, ...]
连续内存块，原生 C 类型（如 double 8 字节）
```

### 5.1.3 核心 API

```python
import numpy as np

# 创建
np.zeros((3, 4))
np.ones((2, 3))
np.eye(3)
np.random.randn(3, 3)
np.arange(10)
np.linspace(0, 1, 5)

# 形状
arr = np.arange(12).reshape(3, 4)
arr.T                    # 转置
arr.flatten()            # 展平

# 运算
arr + 1, arr * 2         # 广播
a @ b                    # 矩阵乘法
np.linalg.svd(a)         # SVD 分解

# 统计
arr.mean(), arr.std()
arr.max(axis=0)
```

### 5.1.4 AI 场景案例

```python
def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

def batch_similarity(query: np.ndarray, db: np.ndarray) -> np.ndarray:
    return np.dot(db, query) / (
        np.linalg.norm(db, axis=1) * np.linalg.norm(query)
    )

def normalize(embeddings: np.ndarray) -> np.ndarray:
    return embeddings / np.linalg.norm(embeddings, axis=1, keepdims=True)
```

---

## 5.2 Pandas

### 5.2.1 一句话核心本质

**Pandas 是 Python 数据分析库，核心是 `DataFrame`（表格）和 `Series`（一维），提供数据清洗、转换、聚合。**

### 5.2.2 核心 API

```python
import pandas as pd

# 创建
df = pd.DataFrame({
    "name": ["Alice", "Bob"],
    "age": [25, 30],
    "score": [0.95, 0.87]
})

# 查看
df.head(2)
df.info()
df.describe()

# 索引
df["name"]
df[["name", "score"]]
df.iloc[0]
df[df["score"] > 0.9]

# 清洗
df.isnull().sum()
df.dropna()
df.fillna(0)

# 分组
df.groupby("active")["score"].mean()

# 合并
pd.concat([df1, df2])
pd.merge(df1, df2, on="id")

# 读取
pd.read_csv("data.csv")
pd.read_json("data.json")
```

### 5.2.3 AI 场景案例

```python
# 对话数据分析
conversations = pd.DataFrame([
    {"prompt": "讲个故事", "tokens": 150, "model": "gpt-3.5"},
    {"prompt": "写首诗", "tokens": 80, "model": "gpt-4"},
])

token_stats = conversations.groupby("model").agg({
    "tokens": ["sum", "mean", "count"]
})

# 特征工程
def extract_features(df):
    features = df.copy()
    features["prompt_length"] = features["prompt"].str.len()
    features["hour"] = pd.to_datetime(features["timestamp"]).dt.hour
    return features

# 评估结果分析
results = []
for epoch in range(10):
    results.append({"epoch": epoch, "loss": np.random.random()})
results_df = pd.DataFrame(results)
best = results_df.loc[results_df["loss"].idxmin()]
```

---

## 5.3 PyTorch

### 5.3.1 一句话核心本质

**PyTorch 是动态计算图的深度学习框架，核心是 `Tensor`（GPU 加速多维数组）和 `autograd`（自动求导）。**

### 5.3.2 为什么 AI 用 PyTorch

```
Python 控制逻辑 + C++/CUDA 计算核心

动态图 (Define-by-Run)：
- 运行时构建计算图
- 可调试，可直接 print tensor
- 条件分支和循环天然支持

生态：
- HuggingFace Transformers 基于 PyTorch
- 学术界默认框架
```

### 5.3.3 Tensor 基础

```python
import torch

# 创建
data = torch.tensor([[1, 2], [3, 4]], dtype=torch.float32)
zeros = torch.zeros(3, 4)
randn = torch.randn(3, 3)

# 设备
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
data = data.to(device)

# 形状
data.shape
data.view(4, 1)
data.unsqueeze(0)

# 运算
a + b, a @ b
torch.cat([a, b], dim=0)

# NumPy 互转
np_arr = data.cpu().numpy()
tensor = torch.from_numpy(np_arr)
```

### 5.3.4 自动求导

```python
x = torch.tensor([2.0, 3.0], requires_grad=True)
y = x ** 2 + 2 * x + 1
z = y.sum()
z.backward()
print(x.grad)  # tensor([6., 8.])
```

### 5.3.5 神经网络

```python
import torch.nn as nn
import torch.nn.functional as F

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

### 5.3.6 DataLoader

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

dataloader = DataLoader(dataset, batch_size=32, shuffle=True)

# 保存/加载
torch.save(model.state_dict(), "model.pt")
model.load_state_dict(torch.load("model.pt"))
```

---

## 5.4 Transformers (HuggingFace)

### 5.4.1 一句话核心本质

**HuggingFace Transformers 统一了 10000+ 预训练模型的加载、推理、训练、微调接口。**

### 5.4.2 Pipeline（一行代码推理）

```python
from transformers import pipeline

classifier = pipeline("sentiment-analysis")
result = classifier("I love this!")
print(result)  # [{'label': 'POSITIVE', 'score': 0.99}]

generator = pipeline("text-generation", model="gpt2")
result = generator("Once upon a time", max_length=50)
```

### 5.4.3 底层 API

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

```python
from transformers import AutoModelForCausalLM, AutoTokenizer

model_name = "microsoft/DialoGPT-medium"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForCausalLM.from_pretrained(
    model_name, torch_dtype=torch.float16, device_map="auto"
)

def chat(prompt: str):
    inputs = tokenizer.encode(prompt, return_tensors="pt").to(model.device)
    outputs = model.generate(inputs, max_length=100, temperature=0.7)
    return tokenizer.decode(outputs[0], skip_special_tokens=True)
```

### 5.4.5 AI 场景案例

```python
class EmbeddingModel:
    def __init__(self, model_name: str = "BAAI/bge-small-zh"):
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

**LangChain 是 LLM 应用开发框架，提供 Chain、Prompt、Memory、Tool、Agent 等核心抽象。**

### 5.5.2 核心概念

```
LangChain 核心抽象：

LLM      → 模型封装
Prompt   → 提示模板
Chain    → 调用链
Memory   → 对话记忆
Tool     → 工具（搜索、计算、API）
Agent    → 自主决策（LLM + Tool + Memory）
```

### 5.5.3 基础用法

```python
from langchain_openai import ChatOpenAI
from langchain.prompts import ChatPromptTemplate
from langchain.chains import LLMChain
from langchain.memory import ConversationBufferMemory
from langchain.agents import tool, create_tool_calling_agent, AgentExecutor

llm = ChatOpenAI(model="gpt-4")

# Prompt 模板
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个{role}专家"),
    ("human", "{question}")
])
chain = LLMChain(llm=llm, prompt=prompt)
result = chain.invoke({"role": "Python", "question": "什么是装饰器？"})

# 对话记忆
memory = ConversationBufferMemory()
from langchain.chains import ConversationChain
conversation = ConversationChain(llm=llm, memory=memory)
conversation.invoke("我的名字是 AI")
conversation.invoke("我叫什么名字？")  # 记得上下文
```

### 5.5.4 Agent

```python
@tool
def calculate(expression: str) -> str:
    """数学计算"""
    try:
        return str(eval(expression))
    except Exception as e:
        return f"错误: {e}"

@tool
def get_time(format: str = "%Y-%m-%d") -> str:
    """获取当前时间"""
    import datetime
    return datetime.datetime.now().strftime(format)

tools = [calculate, get_time]
agent = create_tool_calling_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)

result = agent_executor.invoke({
    "input": "2024年出生的孩子现在几岁？"
})
```

### 5.5.5 RAG

```python
from langchain_community.vectorstores import Chroma
from langchain_community.embeddings import OpenAIEmbeddings
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.chains import RetrievalQA

# 1. 加载文档
loader = TextLoader("knowledge.txt")
documents = loader.load()

# 2. 分块
text_splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
chunks = text_splitter.split_documents(documents)

# 3. 向量存储
vectorstore = Chroma.from_documents(
    documents=chunks,
    embedding=OpenAIEmbeddings()
)

# 4. RAG 链
qa_chain = RetrievalQA.from_chain_type(
    llm=ChatOpenAI(model="gpt-4"),
    chain_type="stuff",
    retriever=vectorstore.as_retriever(search_kwargs={"k": 3})
)

result = qa_chain.invoke({"query": "文档中提到了什么？"})
```

---

## 5.6 向量数据库 (ChromaDB)

### 5.6.1 一句话核心本质

**向量数据库存储和检索高维向量，支持近似最近邻搜索（ANN），是 RAG 系统核心基础设施。**

```python
import chromadb
from sentence_transformers import SentenceTransformer

class RAGVectorStore:
    def __init__(self, persist_dir="./chroma_db"):
        self.embedder = SentenceTransformer("all-MiniLM-L6-v2")
        self.client = chromadb.PersistentClient(path=persist_dir)

    def add_documents(self, collection_name, documents, metadatas=None):
        collection = self.client.get_or_create_collection(
            name=collection_name,
            embedding_function=self.embedder.encode
        )
        ids = [f"{collection_name}_{i}" for i in range(len(documents))]
        collection.add(documents=documents, metadatas=metadatas or [{}]*len(documents), ids=ids)

    def search(self, collection_name, query, k=5):
        collection = self.client.get_collection(name=collection_name)
        results = collection.query(query_texts=[query], n_results=k)
        return list(zip(results["documents"][0], results["distances"][0]))

store = RAGVectorStore()
store.add_documents("ai_knowledge", ["Transformer 是注意力机制", "BERT 是双向编码器"])
results = store.search("ai_knowledge", "什么是注意力机制？")
for doc, score in results:
    print(f"[{score:.3f}] {doc}")
```

---

## 5.7 AI Agent

### 5.7.1 一句话核心本质

**AI Agent 是能自主思考、规划、使用工具、执行任务的 LLM 应用，核心是 ReAct（Reasoning + Acting）循环。**

### 5.7.2 Agent 架构

```
用户输入
    ↓
Agent 思考 (Thought)
    ↓
选择行动 (Action) → 调用工具 (Tool)
    ↓
观察结果 (Observation)
    ↓
再次思考 → 行动 → 直到任务完成
    ↓
最终回答 (Final Answer)
```

### 5.7.3 手写简单 Agent

```python
import json
from typing import List, Dict, Callable

class SimpleAgent:
    def __init__(self, llm_func: Callable, tools: List[Dict]):
        self.llm = llm_func
        self.tools = {t["name"]: t for t in tools}
        self.messages = []

    def run(self, task: str, max_steps: int = 10) -> str:
        self.messages.append({"role": "user", "content": task})

        for step in range(max_steps):
            response = self.llm(str(self.messages))
            action = json.loads(response)

            if action["type"] == "answer":
                return action["content"]

            tool = self.tools.get(action["name"])
            if not tool:
                observation = f"错误: 工具 {action['name']} 不存在"
            else:
                observation = tool["func"](action.get("args", {}))

            self.messages.append({"role": "assistant", "content": response})
            self.messages.append({"role": "user", "content": f"观察: {observation}"})

        return "超出最大步数"

def search_web(query: str) -> str:
    return f"搜索 '{query}' 的结果..."

agent = SimpleAgent(
    llm_func=lambda msgs: json.dumps({
        "type": "tool", "name": "search_web", "args": {"query": "Python AI"}
    }),
    tools=[{"name": "search_web", "func": search_web}]
)
result = agent.run("查找 Python AI 框架")
```

---

# 第六阶段：GitHub AI 项目阅读（核心）

---

## 6.1 如何阅读大型 Python 项目

### 6.1.1 一句话核心本质

**先看项目结构 → 找入口文件 → 跟踪核心调用链 → 理解关键抽象。**

### 6.1.2 项目结构

```
project/
├── .github/              # CI/CD
├── docs/                 # 文档
├── examples/             # 使用示例
├── tests/                # 测试
├── src/
│   ├── main.py          # 入口
│   ├── app.py           # FastAPI 应用
│   ├── config.py        # 配置
│   ├── models/           # 数据模型
│   ├── routers/          # API 路由
│   ├── services/         # 业务逻辑
│   └── utils/            # 工具函数
├── requirements.txt      # 依赖
├── pyproject.toml        # 项目配置
├── Dockerfile
├── docker-compose.yml
└── README.md
```

### 6.1.3 如何找入口

```python
# 方法1: 看 README.md Usage
# 方法2: 搜索 main() 函数
# 方法3: 搜索 if __name__ == "__main__"
# 方法4: 看 setup.py/pyproject.toml 的 entry_points

# 典型入口
import argparse
import uvicorn

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()
    app = create_app()
    uvicorn.run(app, host="0.0.0.0", port=args.port)

if __name__ == "__main__":
    main()
```

### 6.1.4 如何分析调用链

```
用户请求 → FastAPI Router → Service Layer → LLM/Agent → Response

# 1. routers/chat.py
@router.post("/chat")
async def chat(request: ChatRequest):
    return await chat_service.process(request)

# 2. services/chat_service.py
class ChatService:
    async def process(self, request):
        context = await self.retrieve_context(request.message)
        prompt = self.build_prompt(request.message, context)
        return await self.llm.generate(prompt)

# 3. services/llm_service.py
class LLMService:
    async def generate(self, prompt):
        response = await openai.ChatCompletion.create(
            model=self.model, messages=[{"role": "user", "content": prompt}]
        )
        return response.choices[0].message.content
```

### 6.1.5 如何 debug

```python
# 方法1: print 调试
def complex_func(data):
    print(f"[DEBUG] 进入, data={data[:50]}...")
    result = process(data)
    print(f"[DEBUG] 结果: {result}")
    return result

# 方法2: pdb 交互调试
import pdb

def buggy_func():
    x = 1; y = 0
    pdb.set_trace()  # 在此暂停
    # n → 下一行, s → 进入函数, c → 继续
    # p x → 打印变量, l → 查看上下文, q → 退出
    return x / y

# 方法3: ipdb（增强版）
import ipdb; ipdb.set_trace()

# 方法4: 日志调试
import logging
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)
logger.debug("开始处理")
logger.exception("发生异常")  # 打印完整堆栈
```

### 6.1.6 如何跟踪代码执行

```python
# 1. sys.settrace 全局跟踪
import sys

def trace_calls(frame, event, arg):
    if event == "call":
        filename = frame.f_code.co_filename
        if "my_project" in filename:
            print(f"调用: {frame.f_code.co_name} 文件: {filename}:{frame.f_lineno}")
    return trace_calls

sys.settrace(trace_calls)

# 2. 装饰器跟踪
import functools

def trace(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        print(f"→ {func.__name__}")
        result = func(*args, **kwargs)
        print(f"← {func.__name__} = {result}")
        return result
    return wrapper

# 3. cProfile 性能分析
import cProfile, pstats

def run_analysis():
    profiler = cProfile.Profile()
    profiler.enable()
    my_function()
    profiler.disable()
    pstats.Stats(profiler).sort_stats("cumulative").print_stats(10)
```

### 6.1.7 真实项目分析示例

```python
# 项目结构：
# chat_ai/
# ├── main.py          # 入口
# ├── app.py           # FastAPI 应用
# ├── config.py        # 配置
# └── services/
#     └── chat.py      # 对话服务

# Step 1: 看 README.md → python main.py --port 8000
# Step 2: 看 requirements.txt → fastapi, uvicorn, openai
# Step 3: 看 main.py（入口）

# main.py
import uvicorn
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
