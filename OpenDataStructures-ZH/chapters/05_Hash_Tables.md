# 第5章 哈希表

哈希表（Hash Table）是一种高效的方法，用于存储来自大范围 $U=\{0,\ldots,2^{w}-1\}$ 的小数量整数 $n$。术语"哈希表"包含了一系列广泛的数据结构。本章的第一部分聚焦于两种最常见的哈希表实现：链地址法哈希（hashing with chaining）和线性探测（linear probing）。

通常，哈希表存储的数据类型并非整数。在这种情况下，每个数据项会关联一个整数**哈希码**（hash code），并用于哈希表中。本章的第二部分讨论如何生成这样的哈希码。

本章中使用的一些方法需要在特定范围内随机选择整数。在代码示例中，其中一些"随机"整数是硬编码的常量。这些常量是通过从大气噪声中生成的随机比特获得的。

## 5.1 ChainedHashTable：链地址法哈希

**ChainedHashTable** 数据结构使用链地址法哈希（hashing with chaining）将数据存储为一个数组 $t$ 的列表（list）。一个整数 $n$ 跟踪所有列表中的总项数（见图 5.1）：

```python
class ChainedHashTable(BaseSet):
    def __init__(self, iterable=[]):
        self._initialize()
        self.add_all(iterable)

    def _initialize(self):
        self.t = array(1)
        self.n = 0
```

数据项 $x$ 的哈希值记作 $\texttt{hash}(x)$，取值范围为 $\{0,\ldots,\texttt{len}(t)-1\}$。所有哈希值为 $i$ 的项都存储在列表 $t[i]$ 中。为确保列表不会过长，我们维护以下不变量：

$$
\texttt{len}(t) \ge n
$$

这样，每个列表中存储的平均元素数为 $n/\texttt{len}(t) \le 1$。

要向哈希表中添加元素 $x$，我们首先检查是否需要增加 $t$ 的长度，如果需要，则扩展 $t$。完成此操作后，我们对 $x$ 进行哈希，得到一个在 $\{0,\ldots,\texttt{len}(t)-1\}$ 范围内的整数 $i$，并将 $x$ 追加到列表 $t[i]$：

```python
    def add(self, x):
        if self.find(x) is not None:
            return False
        if self.n + 1 > len(self.t):
            self._resize()
        self.t[hash(x) % len(self.t)].append(x)
        self.n += 1
        return True
```

必要时扩展表格（growing the table）涉及将 $t$ 的长度翻倍，并将所有元素重新插入到新表中。这一策略与 ArrayStack 实现中使用的策略完全相同，且结果相同：在一系列插入操作中均摊（amortized）计算时，扩展操作的成本仅为常数（参见第 2.1 节的引理 2.1）。

除了扩展操作，向 ChainedHashTable 添加新值 $x$ 时唯一的工作就是将 $x$ 追加到 $t[\texttt{hash}(x)]$。对于第 2 章或第 3 章中描述的任何列表实现，这仅需常数时间。

要从哈希表中删除元素 $x$，我们遍历列表 $t[\texttt{hash}(x)]$，直到找到 $y$ 等于 $x$，然后将其移除：

```python
    def remove(self, x):
        i = hash(x) % len(self.t)
        for y in self.t[i]:
            if y == x:
                self.t[i].remove(y)
                self.n -= 1
                return y
        return None
```

这需要 $O(n_{\texttt{hash}(x)})$ 时间，其中 $n_i$ 表示存储在 $t[i]$ 的列表长度。

在哈希表中查找元素 $x$ 也是类似的。我们对列表 $t[\texttt{hash}(x)]$ 执行线性搜索：

```python
    def find(self, x):
        i = hash(x) % len(self.t)
        for y in self.t[i]:
            if y == x:
                return y
        return None
```

同样，这需要的时间与列表 $t[\texttt{hash}(x)]$ 的长度成正比。

哈希表的性能在很大程度上取决于哈希函数（hash function）的选择。一个好的哈希函数会将元素均匀分布在 $t$ 的 $n$ 个列表中，使得 $t[\texttt{hash}(x)]$ 的期望大小为 $O(1)$。相反，一个坏的哈希函数会将所有值（包括 $x$）哈希到同一个表位置，此时 $t[\texttt{hash}(x)]$ 的大小将为 $n$。在下一节中，我们将描述一个好的哈希函数。

### 5.1.1 乘法哈希（Multiplicative Hashing）

乘法哈希是一种基于模运算（modular arithmetic，见第 2.3 节）和整数除法（integer division）生成哈希值的高效方法。它使用 $\lfloor \cdot \rfloor$ 运算符，该运算符计算商的整数部分，丢弃余数。形式上，对于任意整数 $a$ 和 $b$，$\lfloor a/b \rfloor$ 是 $a$ 除以 $b$ 的整数商。

在乘法哈希中，我们使用的哈希表大小为 $2^d$，其中 $d$ 是某个整数（称为维度，dimension）。对整数 $x$ 进行哈希的公式为：

$$
\texttt{hash}(x) = \lfloor (z \cdot x) \bmod 2^w / 2^{w-d} \rfloor
$$

这里，$z$ 是在 $\{1,\ldots,2^w-1\}$ 中随机选择的奇数。这个哈希函数可以通过观察以下事实非常高效地实现：默认情况下，整数运算已经在模 $2^w$ 下完成，其中 $w$ 是整数中的比特数（bit）$^{1}$。（见图 5.2。）此外，整数除以 $2^{w-d}$ 等价于在二进制表示中丢弃最右边的 $w-d$ 个比特（这可以通过使用 `>>` 运算符将比特右移 $w-d$ 位来实现）。

> **脚注 5.1:** 这对大多数编程语言（包括 C、C#、C++ 和 Java）都是成立的。值得注意的例外是 Python 和 Ruby，在这两种语言中，固定长度 $w$ 位整数运算溢出的结果会被升级为可变长度表示。

下面的引理（其证明推迟到本节后面）表明乘法哈希在避免冲突方面表现良好：

**引理 5.1** 设 $x$ 和 $y$ 是 $\{0,\ldots,2^w-1\}$ 中的任意两个值，且 $x \neq y$。则 $\Pr[\texttt{hash}(x) = \texttt{hash}(y)] \le 2/2^d$。

借助引理 5.1，我们可以轻松分析 $\texttt{add}$、$\texttt{remove}$ 和 $\texttt{find}$ 的性能：

**引理 5.2** 对于任何数据值 $x$，列表 $t[\texttt{hash}(x)]$ 的期望长度最多为 $1 + n/2^d$，其中 $n$ 是哈希表中 $x$ 的出现次数。

*证明。* 设 $S$ 为哈希表中存储的不等于 $x$ 的元素的（多重）集合。对于元素 $y \in S$，定义指示变量（indicator variable）

$$
I_y = \begin{cases}
1 & \text{如果 } \texttt{hash}(x) = \texttt{hash}(y) \\
0 & \text{其他}
\end{cases}
$$

并注意到根据引理 5.1，$E[I_y] \le 2/2^d$。列表 $t[\texttt{hash}(x)]$ 的期望长度为：

$$
\begin{aligned}
E\left[ 1 + \sum_{y \in S} I_y \right] &= 1 + \sum_{y \in S} E[I_y] \\
&\le 1 + \sum_{y \in S} 2/2^d \\
&\le 1 + \frac{2n}{2^d}
\end{aligned}
$$

得证。$\square$

现在，我们想要证明引理 5.1，但首先需要一个数论（number theory）的结果。在以下证明中，我们使用记号 $(b_k,\ldots,b_0)_2$ 表示 $\sum_{i=0}^k b_i2^i$，其中每个 $b_i$ 是一个比特（bit），取 0 或 1。换句话说，$(b_k,\ldots,b_0)_2$ 是二进制表示为 $b_k\cdots b_0$ 的整数。我们使用 $?$ 表示未知值的比特。

**引理 5.3** 设 $O$ 是 $\{1,\ldots,2^w-1\}$ 中奇数的集合；设 $q$ 和 $r$ 是 $O$ 中的任意两个元素。则恰好存在一个 $z \in O$ 使得 $zq = r \pmod{2^w}$。

*证明。* 由于 $z$ 和 $q$ 的选择数量相同，只需证明最多有一个 $z \in O$ 满足 $zq = r \pmod{2^w}$ 即可。

为推导矛盾，假设存在两个这样的值 $z$ 和 $z'$，且 $z \neq z'$。则

$$
zq \equiv z'q \pmod{2^w}
$$

所以

$$
(z - z')q \equiv 0 \pmod{2^w}
$$

但这意味着

$$
(z - z')q = k2^w \tag{5.1}
$$

对于某个整数 $k$。从二进制数的角度思考，我们有

$$
z - z' = (?\cdots???0\cdots0)_2
$$

因此 $(z - z')$ 的二进制表示中末尾有 $t$ 个 0，其中 $t \ge 0$。此外，由于 $z \neq z'$ 且 $z,z' \in O$，$z - z'$ 是偶数且 $|z - z'| \ge 2$。由于 $z$ 是奇数，它没有末尾的 0。

由于 $q$ 是奇数，$q$ 也没有末尾的 0。因此，乘积 $(z - z')q$ 在二进制表示中恰好有 $t$ 个末尾的 0。由于 $k2^w$ 在二进制表示中至少有 $w$ 个末尾的 0，所以 $t \ge w$。但 $z - z'$ 最多有 $w$ 位，因此 $|z - z'| < 2^w$，这意味着 $t < w$。因此 $(z - z')q$ 的二进制表示中末尾 0 的个数少于 $w$：

$$
(z - z')q = (?????0\cdots0)_2 \quad \text{（少于 $w$ 个末尾 0）}
$$

因此 $(z - z')q$ 不可能满足 (5.1)，产生矛盾，证明完毕。$\square$

引理 5.3 的效用来自以下观察：如果 $z$ 是从 $O$ 中均匀随机选择的，那么 $zq \bmod 2^w$ 在 $O$ 上是均匀分布的。在以下证明中，考虑 $z$ 的二进制表示会有帮助，它由 $w-1$ 个随机比特后跟一个 1 组成。

*引理 5.1 的证明。* 首先我们注意到条件 $\texttt{hash}(x) = \texttt{hash}(y)$ 等价于"$z \cdot x \bmod 2^w$ 的最高 $d$ 位与 $z \cdot y \bmod 2^w$ 的最高 $d$ 位相同"。该陈述的一个必要条件是 $(z \cdot (x-y)) \bmod 2^w$ 的二进制表示中最高 $d$ 位要么全是 0，要么全是 1。也就是说，

$$
(z\cdot(x-y))\bmod 2^w = (0\cdots0???\cdots?)_2 \tag{5.2}
$$

当 $x \ge y$ 时，或

$$
(z\cdot(x-y))\bmod 2^w = (1\cdots1???\cdots?)_2 \tag{5.3}
$$

当 $x < y$ 时。因此，我们只需限定 $z\cdot(x-y)\bmod 2^w$ 看起来像 (5.2) 或 (5.3) 的概率。

设 $q$ 是唯一的奇数，使得 $(x-y) = q2^r$ 对某个整数 $r \ge 0$ 成立。根据引理 5.3，$zq \bmod 2^w$ 的二进制表示有 $w-1$ 个随机比特，后跟一个 1：

$$
zq \bmod 2^w = (?\cdots?1)_2
$$

因此，$z(x-y) \bmod 2^w = (zq)2^r \bmod 2^w$ 的二进制表示有 $w-1-r$ 个随机比特，后跟一个 1，再后跟 $r$ 个 0：

$$
z(x-y)\bmod 2^w = (?\cdots?1\underbrace{0\cdots0}_{r})_2
$$

我们现在可以完成证明了：如果 $r \ge w-d$，那么 $z(x-y) \bmod 2^w$ 的 $d$ 个高阶位同时包含 0 和 1，因此看起来像 (5.2) 或 (5.3) 的概率为 0。如果 $r = w-d-1$，那么看起来像 (5.2) 的概率为 0，但看起来像 (5.3) 的概率为 $1/2^{d+1}$（因为我们必须有 $?\cdots? = 1\cdots1$）。如果 $r < w-d-1$，那么我们必须有 $?\cdots? = 0\cdots0$ 或 $?\cdots? = 1\cdots1$。每种情况的概率为 $1/2^{d+1}$，且它们互斥，因此任一情况发生的概率为 $1/2^{d}$。证明完毕。$\square$

### 5.1.2 总结

以下定理总结了 ChainedHashTable 数据结构的性能：

**定理 5.1** ChainedHashTable 实现了 USet 接口。忽略调用 $\texttt{resize}$ 的成本，ChainedHashTable 的 $\texttt{add}$、$\texttt{remove}$ 和 $\texttt{find}$ 操作每次均摊期望时间（expected amortized time）为 $O(1)$。

此外，从一个空的 ChainedHashTable 开始，任何 $m$ 次 $\texttt{add}$ 和 $\texttt{remove}$ 操作的序列在所有 $\texttt{resize}$ 调用中花费的总时间为 $O(m)$。

## 5.2 LinearHashTable：线性探测

ChainedHashTable 数据结构使用一个列表数组，其中第 $i$ 个列表存储所有满足 $\texttt{hash}(x)=i$ 的元素 $x$。另一种方法称为**开放寻址**（open addressing），是将元素直接存储在数组 $t$ 中，$t$ 中的每个数组位置最多存储一个值。本节描述的 LinearHashTable 采用了这种方法。在某些地方，这种数据结构被称为使用线性探测的开放寻址。

LinearHashTable 的主要思想是，理想情况下，我们希望将哈希值为 $\texttt{hash}(x) = i$ 的元素 $x$ 存储在表位置 $t[i]$。如果无法做到这一点（因为某个元素已经存储在那里），那么我们尝试将其存储在位置 $t[(i+1)\bmod \texttt{len}(t)]$；如果这也不可能，则尝试 $t[(i+2)\bmod \texttt{len}(t)]$，依此类推，直到我们为 $x$ 找到一个位置。

$t$ 中存储着三种类型的条目：

1. **数据值**：我们正在表示的 USet 中的实际值；
2. **$\texttt{null}$ 值**：从未存储过数据的数组位置；
3. **$\texttt{del}$ 值**：曾经存储过数据但已被删除的数组位置。

除了计数器 $n$（跟踪 LinearHashTable 中元素的数量）之外，还有一个计数器 $q$ 跟踪类型 1 和类型 3 的元素数量。也就是说，$q$ 等于 $n$ 加上 $t$ 中 $\texttt{del}$ 值的数量。为了使其高效工作，我们需要 $\texttt{len}(t)$ 远大于 $q$，以便 $t$ 中有大量的 $\texttt{null}$ 值。因此，LinearHashTable 上的操作维护以下不变量：

$$
\texttt{len}(t) \ge 2q
$$

总结来说，一个 LinearHashTable 包含一个数组 $t$（存储数据元素），以及整数 $n$ 和 $q$（分别跟踪数据元素的数量和 $t$ 中非 $\texttt{null}$ 值的数量）。由于许多哈希函数只适用于大小为 2 的幂的表，我们还维护一个整数 $d$ 并保持不变量 $\texttt{len}(t) = 2^d$。

```python
class LinearHashTable(BaseSet):
    def __init__(self, iterable=[]):
        self._initialize()
        self.add_all(iterable)

    def _initialize(self):
        self.t = array(1)
        self.n = 0
        self.d = 0
        self.q = 0
```

LinearHashTable 中的 $\texttt{find}$ 操作很简单。我们从数组条目 $t[i]$ 开始，其中 $i = \texttt{hash}(x)$，然后搜索条目 $t[i]$、$t[(i+1)\bmod \texttt{len}(t)]$、$t[(i+2)\bmod \texttt{len}(t)]$，依此类推，直到找到索引 $j$ 使得 $t[j] = x$ 或 $t[j] = \texttt{null}$。在前一种情况下，我们返回 $x$。在后一种情况下，我们得出结论：$x$ 不在哈希表中，并返回 $\texttt{None}$。

```python
    def find(self, x):
        i = hash(x) % len(self.t)
        while self.t[i] is not None:
            if self.t[i] != del and self.t[i] == x:
                return self.t[i]
            i = (i + 1) % len(self.t)
        return None
```

$\texttt{add}$ 操作实现起来也相当简单。在检查 $x$ 尚未存储在表中（使用 $\texttt{find}$）后，我们搜索 $t[i]$、$t[(i+1)\bmod \texttt{len}(t)]$、$t[(i+2)\bmod \texttt{len}(t)]$，依此类推，直到找到 $\texttt{null}$ 或 $\texttt{del}$，并将 $x$ 存储在该位置，并递增 $n$，如果合适也递增 $q$。

```python
    def add(self, x):
        if self.find(x) is not None:
            return False
        if 2*(self.q + 1) > len(self.t):
            self._resize()
        i = hash(x) % len(self.t)
        while self.t[i] is not None and self.t[i] != del:
            i = (i + 1) % len(self.t)
        if self.t[i] is None:
            self.q += 1
        self.n += 1
        self.t[i] = x
        return True
```

至此，$\texttt{remove}$ 操作的实现应该显而易见了。我们搜索 $t[i]$、$t[(i+1)\bmod \texttt{len}(t)]$、$t[(i+2)\bmod \texttt{len}(t)]$，依此类推，直到找到索引 $j$ 使得 $t[j] = x$ 或 $t[j] = \texttt{null}$。在前一种情况下，我们将 $t[j]$ 设置为 $\texttt{del}$ 并返回 $x$。在后一种情况下，我们得出结论：$x$ 未存储在表中（因此无法删除）并返回 $\texttt{None}$。

```python
    def remove(self, x):
        i = hash(x) % len(self.t)
        while self.t[i] is not None:
            if self.t[i] != del and self.t[i] == x:
                self.t[i] = del
                self.n -= 1
                return x
            i = (i + 1) % len(self.t)
        return None
```

$\texttt{find}$、$\texttt{add}$ 和 $\texttt{remove}$ 方法的正确性很容易验证，尽管它依赖于 $\texttt{del}$ 值的使用。注意，这些操作都不会将非 $\texttt{null}$ 条目设置为 $\texttt{null}$。因此，当我们到达一个索引 $j$ 使得 $t[j] = \texttt{null}$ 时，这就证明我们正在搜索的元素 $x$ 没有存储在表中；$t[j]$ 一直是 $\texttt{null}$，因此之前的 $\texttt{add}$ 操作没有理由会经过索引 $j$。

当非 $\texttt{null}$ 条目的数量超过 $\texttt{len}(t)/2$ 时，$\texttt{add}$ 方法会调用 $\texttt{resize}$ 方法；当数据条目的数量少于 $\texttt{len}(t)/8$ 时，$\texttt{remove}$ 方法也会调用它。$\texttt{resize}$ 方法的工作方式与其他基于数组的数据结构中的 $\texttt{resize}$ 方法类似。我们找到最小的非负整数 $d$ 使得 $2^d \ge 3n$。我们重新分配数组 $t$ 使其大小为 $2^d$，然后将旧版本 $t$ 中的所有元素插入到新调整大小的 $t$ 副本中。在此过程中，我们将 $q$ 重置为 $n$，因为新分配的 $t$ 中不包含 $\texttt{del}$ 值。

```python
    def _resize(self):
        self.d = 1
        while (1 << self.d) < 3*self.n:
            self.d += 1
        new_t = array(1 << self.d)
        for i in range(len(self.t)):
            if self.t[i] is not None and self.t[i] != del:
                k = hash(self.t[i]) % len(new_t)
                while new_t[k] is not None:
                    k = (k + 1) % len(new_t)
                new_t[k] = self.t[i]
        self.t = new_t
        self.q = self.n
```

### 5.2.1 线性探测的分析

注意，每个操作（$\texttt{add}$、$\texttt{remove}$ 或 $\texttt{find}$）在发现（或之前发现）$t$ 中的第一个 $\texttt{null}$ 条目时就会完成。线性探测分析背后的直觉是，由于 $t$ 中至少有一半的元素等于 $\texttt{null}$，一个操作应该不会花很长时间就能完成，因为它很快就会遇到一个 $\texttt{null}$ 条目。不过，我们不应该过于依赖这种直觉，因为它会让我们得出（不正确的）结论：操作检查的 $t$ 中位置数的期望值最多为 2。

在本节的其余部分，我们将假设所有哈希值在 $\{0,\ldots,\texttt{len}(t)-1\}$ 上是独立且均匀分布的。这不是一个现实的假设，但能让我们分析线性探测。稍后在本节中，我们将描述一种称为**表格哈希**（tabulation hashing）的方法，它产生的哈希函数对于线性探测来说"足够好"。我们还将假设所有对 $t$ 位置的索引都以 $\texttt{len}(t)$ 为模，因此 $t[i]$ 实际上是 $t[i\bmod \texttt{len}(t)]$ 的简写。

当所有表条目 $t[i],t[(i+1)\bmod \texttt{len}(t)],\ldots,t[(i+k-1)\bmod \texttt{len}(t)]$ 均为非 $\texttt{null}$ 且 $t[(i+k)\bmod \texttt{len}(t)] = \texttt{null}$ 时，我们说一个长度为 $k$ 的运行（run）从 $i$ 开始。$t$ 中非 $\texttt{null}$ 元素的数量恰好为 $q$，并且 $\texttt{resize}$ 方法确保在所有时间 $\texttt{len}(t) \ge 2q$。自上次 $\texttt{resize}$ 操作以来，有 $n \le q$ 个元素被插入到 $t$ 中。根据我们的假设，这些元素中的每一个都有一个哈希值 $\texttt{hash}(x)$，该值是均匀且独立于其他值的。基于此设定，我们可以证明分析线性探测所需的主要引理。

**引理 5.4** 固定一个值 $i \in \{0,\ldots,\texttt{len}(t)-1\}$。则一个长度为 $k$ 的运行从 $i$ 开始的概率最多为 $c2^{-k}$，其中 $c$ 是某个常数。

*证明。* 如果一个长度为 $k$ 的运行从 $i$ 开始，那么恰好存在 $k$ 个元素 $x$ 使得 $\texttt{hash}(x) \in \{i,\ldots,i+k-1\}$。发生这种情况的概率恰好是

$$
\binom{q}{k} \cdot \left( \frac{k}{2^d} \right)^k \left( 1 - \frac{k}{2^d} \right)^{q-k}
$$

因为，对于每种 $k$ 个元素的选择，这 $k$ 个元素必须哈希到 $k$ 个位置之一，而剩余的 $q-k$ 个元素必须哈希到其他 $2^d - k$ 个表位置$^{2}$。

> **脚注 5.2:** 注意，这个概率大于长度为 $k$ 的运行从 $i$ 开始的真实概率，因为运行的定义中不包含 $t[(i+k)\bmod 2^d] = \texttt{null}$ 的要求。

在接下来的推导中，我们会稍微取巧，用 $q$ 替换 $q-1$。斯特林近似（Stirling's Approximation，第 1.3.2 节）表明这与真实值的差距仅为 $O(1/\sqrt{k})$ 因子。这只是为了简化推导；练习 5.4 要求读者使用完整的斯特林近似更严格地重新计算。

$q$ 的值在 $q$ 最小时最大化，且数据结构维护不变量 $q \le 2^d / 2 = 2^{d-1}$，因此

$$
\begin{aligned}
\Pr[\text{从 $i$ 开始长度为 $k$ 的运行}]
&\le \binom{2^{d-1}}{k} \left( \frac{k}{2^d} \right)^k \left( 1 - \frac{k}{2^d} \right)^{2^{d-1}-k} \\
&\le \left( \frac{2^{d-1} e}{k} \right)^k \left( \frac{k}{2^d} \right)^k
      \left( 1 - \frac{k}{2^d} \right)^{2^{d-1}} \\
&\quad\text{[斯特林近似]} \\
&= \left( \frac{e}{2} \right)^k \left( 1 - \frac{k}{2^d} \right)^{2^{d-1}} \\
&\le \left( \frac{e}{2} \right)^k e^{-k/2} \\
&= \left( \frac{e}{2e^{1/2}} \right)^k \\
&= \left( \frac{\sqrt{e}}{2} \right)^k
\end{aligned}
$$

（在最后一步，我们使用了不等式 $1 - t \le e^{-t}$，这对所有 $t$ 都成立。）由于 $\sqrt{e}/2 < 1$，证明完毕。$\square$

使用引理 5.4 来证明 $\texttt{find}$、$\texttt{add}$ 和 $\texttt{remove}$ 的期望运行时间上界现在相当直接。考虑最简单的情况，即我们对某个从未存储在 LinearHashTable 中的值 $x$ 执行 $\texttt{find}$。在这种情况下，$i = \texttt{hash}(x)$ 是一个在 $\{0,\ldots,2^d-1\}$ 中独立于 $t$ 内容的随机值。如果 $i$ 是一个长度为 $k$ 的运行的一部分，那么执行 $\texttt{find}$ 操作所需的时间最多为 $k+1$。因此，期望运行时间可以上界为：

$$
\begin{aligned}
E[T] &\le \sum_{k=0}^{\infty} (k+1) \cdot \Pr[\text{$i$ 在长度为 $k$ 的运行中}] \\
&\le \sum_{k=0}^{\infty} (k+1) \sum_{j=0}^{\infty} \Pr[\text{从 $j$ 开始长度为 $k$ 的运行}]
\end{aligned}
$$

注意，每个长度为 $k$ 的运行对内层和的贡献为 $k+1$ 次，总贡献为 $(k+1)k$，因此上述和可重写为

$$
\begin{aligned}
E[T] &\le \sum_{k=0}^{\infty} (k+1)k \cdot \Pr[\text{从某处开始长度为 $k$ 的运行}] \\
&\le 2^d \cdot \sum_{k=0}^{\infty} k^2 \cdot c2^{-k} \\
&= O(1)
\end{aligned}
$$

这个推导的最后一步源于 $\sum_{k=0}^\infty k^2 / 2^k$ 是指数递减级数这一事实$^{3}$。因此，我们得出结论：对于未包含在 LinearHashTable 中的值 $x$，$\texttt{find}$ 操作的期望运行时间为 $O(1)$。

> **脚注 5.3:** 用许多微积分教科书的术语来说，这个和通过了比值测试（ratio test）：存在一个正整数 $N$ 使得对所有 $k \ge N$，$((k+1)^2 / 2^{k+1}) / (k^2 / 2^k) < 1$。

如果忽略 $\texttt{resize}$ 操作的成本，那么上述分析为我们提供了分析 LinearHashTable 操作成本所需的一切。

首先，上述对 $\texttt{find}$ 的分析适用于 $x$ 不包含在表中时的 $\texttt{find}$ 操作。要分析 $x$ 包含在表中时的 $\texttt{find}$ 操作，我们只需注意这等同于之前将 $x$ 添加到表时的 $\texttt{add}$ 操作的成本。最后，$\texttt{remove}$ 操作的成本与 $\texttt{find}$ 操作的成本相同。

总之，如果忽略 $\texttt{resize}$ 调用的成本，LinearHashTable 上的所有操作都以 $O(1)$ 期望时间运行。与对 ArrayStack 数据结构（第 2.1 节）所做的相同类型的均摊分析可以计入 $\texttt{resize}$ 的成本。

### 5.2.2 总结

以下定理总结了 LinearHashTable 数据结构的性能：

**定理 5.2** LinearHashTable 实现了 USet 接口。忽略调用 $\texttt{resize}$ 的成本，LinearHashTable 每次操作的 $\texttt{add}$、$\texttt{remove}$ 和 $\texttt{find}$ 操作的均摊期望时间为 $O(1)$。

此外，从一个空的 LinearHashTable 开始，任何 $m$ 次 $\texttt{add}$ 和 $\texttt{remove}$ 操作的序列在所有 $\texttt{resize}$ 调用中花费的总时间为 $O(m)$。

### 5.2.3 表格哈希（Tabulation Hashing）

在分析 LinearHashTable 结构时，我们做了一个非常强的假设：对于任何元素集合 $S$，哈希值 $\{\texttt{hash}(x) : x \in S\}$ 在集合 $\{0,\ldots,2^d-1\}$ 上是独立且均匀分布的。实现这一点的一种方法是存储一个巨大的数组 $T$，长度为 $2^w$，其中每个条目是一个随机的 $d$ 位整数，独立于所有其他条目。这样，我们可以通过从 $T$ 中提取一个 $d$ 位整数来实现 $\texttt{hash}(x)$：

$$
\texttt{hash}(x) = T[x]
$$

然而，存储一个大小为 $2^w$ 的数组在内存使用上是不可行的。表格哈希采用的方法是，将 $w$ 位整数视为由 $r$ 个整数组成，每个整数只有 $p = w/r$ 位。这样，表格哈希只需要 $r$ 个数组，每个长度为 $2^p$。所有这些数组中的条目都是独立的随机 $d$ 位整数。为了获得 $\texttt{hash}(x)$ 的值，我们将 $x$ 分割成 $r$ 个 $p$ 位整数，并使用这些作为索引进入这些数组。然后，我们使用按位异或（bitwise exclusive-or）运算符将所有值组合起来得到 $\texttt{hash}(x)$。以下代码展示了当 $p = 8$ 且 $r = 4$ 时的工作原理：

```python
    def hash(self, x):
        h0 = self.tab[0][x & 0xff]
        h1 = self.tab[1][(x >> 8) & 0xff]
        h2 = self.tab[2][(x >> 16) & 0xff]
        h3 = self.tab[3][(x >> 24) & 0xff]
        return (h0 ^ h1 ^ h2 ^ h3) % len(self.t)
```

在这种情况下，`self.tab` 是一个二维数组，有 4 列和 $2^8$ 行。

像 `0xff` 这样的量是十六进制数（hexadecimal numbers），其数字有 16 个可能值：0-9（具有通常含义）和 a-f（表示 10-15）。数字 `0xff` 等于 255。`&` 符号是按位与（bitwise and）运算符，因此像 `x & 0xff` 这样的代码提取 $x$ 的第 0 到第 7 位。

可以很容易地验证，对于任何 $x$，$\texttt{hash}(x)$ 在 $\{0,\ldots,2^d-1\}$ 上是均匀分布的。稍加努力，甚至可以验证任何一对值都具有独立的哈希值。这意味着表格哈希可以替代乘法哈希用于 ChainedHashTable 实现。

然而，任何一组 $n$ 个不同的值并不一定会产生一组 $n$ 个独立的哈希值。尽管如此，当使用表格哈希时，定理 5.2 的界仍然成立。本章末尾提供了相关参考文献。

## 5.3 哈希码（Hash Codes）

上一节讨论的哈希表用于将数据与由 $w$ 位组成的整数键（key）关联起来。在许多情况下，我们的键不是整数。它们可能是字符串、对象、数组或其他复合结构。为了对这些类型的数据使用哈希表，我们必须将这些数据类型映射到 $w$ 位哈希码。哈希码映射应具有以下属性：

1. 如果 $x$ 和 $y$ 相等，则 $\texttt{hash\_code}(x)$ 和 $\texttt{hash\_code}(y)$ 相等。
2. 如果 $x$ 和 $y$ 不相等，则 $\texttt{hash\_code}(x) = \texttt{hash\_code}(y)$ 的概率应该很小（接近 $1/2^w$）。

第一个属性确保，如果我们将 $x$ 存储在哈希表中，随后查找一个等于 $x$ 的值 $y$，那么我们将找到 $x$——这是应该的。第二个属性最大程度地减少了将对象转换为整数所带来的损失。它确保不相等的对象通常具有不同的哈希码，因此很可能存储在我们的哈希表中的不同位置。

### 5.3.1 基本数据类型（Primitive Data Types）的哈希码

像 `bool`、`char`、`int` 和 `float` 这样的小型基本数据类型通常很容易找到哈希码。这些数据类型总是具有二进制表示，并且这种二进制表示通常由 $w$ 或更少的位组成。在这些情况下，我们只需将这些位视为 $\{0,\ldots,2^w-1\}$ 范围内整数的表示。如果两个值不同，它们会得到不同的哈希码。如果它们相同，则得到相同的哈希码。

一些基本数据类型由超过 $w$ 位组成，通常是 $cw$ 位，其中 $c$ 是某个常数整数（Java 的 `long` 和 `double` 类型就是例子，其中 $c = 2$）。这些数据类型可以视为由 $c$ 部分组成的复合对象，如下一节所述。

### 5.3.2 复合对象（Compound Objects）的哈希码

对于一个复合对象，我们希望通过组合对象各个组成部分的单独哈希码来创建哈希码。这并不像听起来那么容易。尽管人们可以找到许多技巧（例如，使用按位异或运算组合哈希码），但其中许多技巧很容易被攻破（参见练习 5.7-5.9）。然而，如果愿意使用 $2w$ 位精度的算术运算，则有简单且稳健的方法可用。

假设我们有一个由多个部分 $x_0,\ldots,x_{r-1}$ 组成的对象，其哈希码为 $h_0,\ldots,h_{r-1}$。那么我们可以选择相互独立的随机 $2w$ 位整数 $z_0,\ldots,z_{r-1}$ 和一个随机 $w$ 位奇数 $z$，并通过以下方式计算我们对象的哈希码：

$$
\texttt{hash\_code}(x) = \left\lfloor \left( \left( \sum_{i=0}^{r-1} z_i h_i \right) \bmod 2^{2w} \right) / 2^w \right\rfloor \cdot z \bmod 2^w / 2^{w-d}
$$

注意，这个哈希码的最终步骤（乘以 $z$ 并除以 $2^{w-d}$）使用了第 5.1.1 节中的乘法哈希函数，将 $2w$ 位的中间结果缩减为 $d$ 位的最终结果。以下是将此方法应用于具有三个部分 $x_0$、$x_1$ 和 $x_2$ 的简单复合对象的示例：

```python
    def hash_code(self, x0, x1, x2):
        z0 = 0x9e3779b97f4a7c15
        z1 = 0xbf58476d1ce4e5b9
        z2 = 0x94d049bb133111eb
        h0 = x0.hash_code()
        h1 = x1.hash_code()
        h2 = x2.hash_code()
        return (z0*h0 + z1*h1 + z2*h2) >> w
```

以下定理表明，除了实现简单之外，该方法还是可证明的优良方法：

**定理 5.3** 设 $h = (h_0,\ldots,h_{r-1})$ 和 $h' = (h'_0,\ldots,h'_{r-1})$ 是 $\{0,\ldots,2^{2w}-1\}$ 中的 $r$ 位整数序列，并假设对于至少一个索引 $i$ 有 $h_i \neq h'_i$。则

$$
\Pr\left[ \sum_{i=0}^{r-1} z_i h_i \equiv \sum_{i=0}^{r-1} z_i h'_i \pmod{2^{2w}} \right] \le 2^{-w}
$$

*证明。* 我们首先忽略最终的乘法哈希步骤，稍后再看该步骤的贡献。定义：

$$
H = \sum_{i=0}^{r-1} z_i h_i \quad \text{和} \quad H' = \sum_{i=0}^{r-1} z_i h'_i
$$

假设 $H \equiv H' \pmod{2^{2w}}$。我们可以将其重写为：

$$
\sum_{i=0}^{r-1} z_i (h_i - h'_i) \equiv 0 \pmod{2^{2w}} \tag{5.4}
$$

其中

$$
z_i (h_i - h'_i) \bmod 2^{2w}
$$

在不失一般性的情况下假设 $h_0 \neq h'_0$，那么 (5.4) 变为

$$
z_0 (h_0 - h'_0) \equiv -\sum_{i=1}^{r-1} z_i (h_i - h'_i) \pmod{2^{2w}} \tag{5.5}
$$

由于 $h_0 - h'_0$ 和边界项最多为 $2^{2w}$，它们的乘积最多为 $2^{4w}$。根据假设 $h_0 \neq h'_0$，因此 (5.5) 在 $z_0$ 中最多有一个解。因此，由于 $z_0$ 和 $z'_0$ 是独立的（$z_i$ 是相互独立的），我们选择 $z_0$ 使得 $H \equiv H' \pmod{2^{2w}}$ 的概率最多为 $1/2^{2w}$。

哈希函数的最后一步是应用乘法哈希，将我们的 $2w$ 位中间结果 $H$ 缩减为 $d$ 位最终结果 $\texttt{hash}(x)$。根据定理 5.3，如果 $H \neq H'$，则 $\Pr[\texttt{hash}(x) = \texttt{hash}(y)] \le 2/2^d$。

总结来说，

$$
\begin{aligned}
\Pr[\texttt{hash\_code}(x) = \texttt{hash\_code}(y)]
&= \Pr[H = H'] + \Pr[H \neq H' \land \texttt{hash}(x) = \texttt{hash}(y)] \\
&\le 2^{-2w} + 2/2^d \\
&= O(1/2^d)
\end{aligned}
$$

$\square$

### 5.3.3 数组和字符串的哈希码

上一节的方法适用于具有固定、恒定数量组成部分的对象。然而，当我们要将其用于具有可变数量组成部分的对象时，它就会失效，因为它需要为每个组成部分提供一个随机的 $2w$ 位整数 $z_i$。我们可以使用伪随机序列（pseudorandom sequence）按需生成任意多个 $z_i$，但此时 $z_i$ 不是相互独立的，并且很难证明伪随机数不会与我们使用的哈希函数产生不良交互。特别是，定理 5.3 证明中 $h_i$ 和 $h_j$ 的值不再独立。

一种更严格的方法是将我们的哈希码建立在素数域（prime fields）上的多项式（polynomials）基础上；这些正是以某个素数 $p$ 为模求值的常规多项式。这种方法基于以下定理，该定理说明素数域上的多项式与通常的多项式行为非常相似：

**定理 5.4** 设 $p$ 是一个素数，设 $q(x) = a_0 + a_1 x + \cdots + a_{k-1} x^{k-1}$ 是系数 $a_0,\ldots,a_{k-1} \in \{0,\ldots,p-1\}$ 的非平凡多项式。则方程 $q(x) \equiv 0 \pmod{p}$ 对于 $x \in \{0,\ldots,p-1\}$ 最多有 $k-1$ 个解。

为了使用定理 5.4，我们通过以下公式使用随机整数 $z \in \{0,\ldots,p-1\}$ 对整数序列 $x_0,\ldots,x_{k-1}$ 进行哈希，其中每个 $x_i \in \{0,\ldots,p-1\}$：

$$
\texttt{hash\_code}(x_0,\ldots,x_{k-1}) = \left( \sum_{i=0}^{k-1} x_i z^{k-i} \right) \bmod p
$$

注意公式末尾额外的 $z$ 项。将 $p$ 视为序列 $x_0,\ldots,x_{k-1}$ 中的最后一个元素 $x_k$ 有助于理解。注意，这个元素与序列中的每个其他元素都不同（每个其他元素都在集合 $\{0,\ldots,p-2\}$ 中）。我们可以将 $p-1$ 视为序列结束标记（end-of-sequence marker）。

以下定理考虑了等长序列的情况，表明这个哈希函数在生成 $z$ 所需的少量随机化下给出了良好的回报：

**定理 5.5** 设 $p = 2^{31} - 1 = 2147483647$ 是一个素数，设 $x_0,\ldots,x_{k-1}$ 和 $x'_0,\ldots,x'_{k-1}$ 各为 $\{0,\ldots,p-2\}$ 中的 $k$ 位整数序列，并假设对于至少一个索引 $j$ 有 $x_j \neq x'_j$。则

$$
\Pr\left[ \sum_{i=0}^{k-1} x_i z^{k-i} \equiv \sum_{i=0}^{k-1} x'_i z^{k-i} \pmod{p} \right] \le \frac{k-1}{p}
$$

*证明。* 方程

$$
\sum_{i=0}^{k-1} x_i z^{k-i} \equiv \sum_{i=0}^{k-1} x'_i z^{k-i} \pmod{p}
$$

可重写为

$$
\sum_{i=0}^{k-1} (x_i - x'_i) z^{k-i} \equiv 0 \pmod{p} \tag{5.6}
$$

由于 $(x_j - x'_j) \neq 0$，这个多项式是非平凡的。因此，根据定理 5.4，它在 $z \in \{0,\ldots,p-1\}$ 中最多有 $k$ 个解。我们选择 $z$ 为这些解之一的概率最多为 $k/p$。$\square$

注意，这个哈希函数也处理两个序列长度不同的情况，即使其中一个序列是另一个的前缀。这是因为该函数实际上哈希了无限序列

$$
x_0,\ldots,x_{k-1},p-1,0,0,\ldots
$$

这保证了如果两个序列的长度分别为 $k$ 和 $k'$ 且 $k < k'$，那么这两个序列在索引 $k$ 处不同。在这种情况下，(5.6) 变为

$$
(x_0 - x'_0)z^{k'} + (x_1 - x'_1)z^{k'-1} + \cdots + (x_{k-1} - x'_{k-1})z^{k'-k+1} + (p-1)z^{k'-k} + \cdots \equiv 0 \pmod{p}
$$

根据定理 5.4，这在 $z$ 中最多有 $k'$ 个解。这与定理 5.5 一起足以证明以下更一般的定理：

**定理 5.6** 设 $p = 2^{31} - 1 = 2147483647$ 是一个素数，设 $x_0,\ldots,x_{k-1}$ 和 $x'_0,\ldots,x'_{k'-1}$ 是 $\{0,\ldots,p-2\}$ 中不同的整数序列。则

$$
\Pr\left[ \sum_{i=0}^{k-1} x_i z^{k-i} \equiv \sum_{i=0}^{k'-1} x'_i z^{k'-i} \pmod{p} \right] \le \frac{\max\{k,k'\}}{p}
$$

以下示例代码展示了如何将此哈希函数应用于包含数组 $x$ 值的对象：

```python
    def hash_code(self, x):
        p = 2**31 - 1
        z = 0x9e3779b97f4a7c15
        h = 0
        for i in range(len(x)):
            h = (h * z + x[i]) % p
        return h % (len(self.t))
```

上述代码为了实现的方便牺牲了一些碰撞概率。特别是，它应用了第 5.1.1 节中的乘法哈希函数（使用 $d = 31$ 时 $z = 2654435769$）将 $h$ 缩减为一个 31 位的值。这样做是为了对素数 $p = 2^{31} - 1$ 进行的模加法和模乘法可以使用无符号 63 位算术运算来执行。因此，两个不同序列（较长者长度为 $k$）具有相同哈希码的概率最多为

$$
\frac{k}{2^{31}} + \frac{2}{2^{31}}
$$

而不是定理 5.6 中指定的 $k/p$。

## 5.4 讨论与练习

哈希表和哈希码代表了一个巨大且活跃的研究领域，本章仅是浅尝辄止。在线哈希文献目录 [10] 包含近 2000 条条目。

存在多种不同的哈希表实现。第 5.1 节描述的实现称为链地址法哈希（每个数组条目包含一个元素链（列表））。链地址法哈希可追溯到 1953 年 1 月 H. P. Luhn 撰写的 IBM 内部备忘录。这份备忘录似乎也是最早引用链表的文献之一。

链地址法哈希的一个替代方案是开放寻址方案，其中所有数据直接存储在数组中。这些方案包括第 5.2 节的 LinearHashTable 结构。这个想法也由 IBM 的一个团队在 1950 年代独立提出。开放寻址方案必须处理冲突解决（collision resolution）问题：即两个值哈希到同一个数组位置的情况。存在不同的冲突解决策略；这些策略提供不同的性能保证，并且通常需要比此处描述的哈希函数更复杂的哈希函数。

还有另一类哈希表实现，即所谓的完美哈希（perfect hashing）方法。这些方法中 $\texttt{find}$ 操作在最坏情况下需要 $O(1)$ 时间。对于静态数据集，这可以通过为数据找到完美哈希函数来实现；这些函数将每条数据映射到唯一的数组位置。对于随时间变化的数据，完美哈希方法包括 FKS 两级哈希表 [31,24] 和布谷鸟哈希（cuckoo hashing）[55]。

本章介绍的哈希函数可能是目前已知的、对于任何数据集都能证明表现良好的最实用方法之一。其他可证明优良的方法可追溯到 Carter 和 Wegman 的开创性工作，他们引入了全域哈希（universal hashing）的概念，并描述了针对不同场景的几种哈希函数 [14]。第 5.2.3 节描述的表格哈希归功于 Carter 和 Wegman [14]，但其在应用于线性探测（以及其他几种哈希表方案）时的分析归功于 Ptracu 和 Thorup [58]。

乘法哈希的思想非常古老，似乎是哈希领域中的常识 [48, Section 6.4]。然而，将乘数 $z$ 选择为随机奇数以及第 5.1.1 节的分析归功于 Dietzfelbinger 等人 [23]。这个版本的乘法哈希是最简单的之一，但其碰撞概率 $2/2^d$ 比使用从 $\{0,\ldots,2^w-1\}$ 到 $\{0,\ldots,2^d-1\}$ 的随机函数所能期望的碰撞概率高两倍。乘法加法哈希（multiply-add hashing）方法使用函数

$$
\texttt{hash}(x) = \lfloor ((z x + z') \bmod 2^{2w}) / 2^{w+d} \rfloor
$$

其中 $z$ 和 $z'$ 分别从 $\{0,\ldots,2^{2w}-1\}$ 中随机选择。乘法加法哈希的碰撞概率仅为 $1/2^d$ [21]，但需要 $2w$ 位精度的算术运算。

从固定长度的 $c$ 位整数序列中获取哈希码有多种方法。一种特别快的方法 [11] 是函数

$$
h(x_0,\ldots,x_{r-1}) = \left( \sum_{i=0}^{r-1} (x_{2i} + z_{2i})(x_{2i+1} + z_{2i+1}) \right) \bmod 2^{2w}
$$

其中 $r$ 是偶数且 $z_0,\ldots,z_{r-1}$ 从 $\{0,\ldots,2^{2w}-1\}$ 中随机选择。这会产生一个 $2w$ 位的哈希码，其碰撞概率为 $1/2^w$。可以使用乘法（或乘法加法）哈希将其缩减为 $d$ 位的哈希码。这个方法很快，因为它只需要 $r/2$ 次 $2w$ 位乘法，而第 5.3.2 节描述的方法需要 $r$ 次乘法（除法和乘法分别通过使用 $2d$ 和 $2w$ 位算术运算隐式发生）。

第 5.3.3 节中使用的基于素数域上的多项式来哈希可变长度数组和字符串的方法归功于 Dietzfelbinger 等人 [22]。由于使用了依赖于昂贵机器指令的 `%` 运算符，它不幸地不是很快速。该方法的一些变体选择 $p$ 为 $2^{w} - 1$ 形式的素数，此时 `%` 运算符可以用加法（`+`）和按位与（`&`）运算代替 [47, Section 3.6]。另一种选择是将固定长度字符串的快速方法应用于长度为 $c$ 的块（对于某个常数 $c$），然后将素数域方法应用于结果序列 $\lceil r/c \rceil$ 哈希码。

**练习 5.1** 某大学在每位学生首次注册课程时分配学号。这些号码是顺序整数，多年前从 0 开始，现在已达数百万。假设我们有一个由一百名一年级学生组成的班级，我们想根据他们的学号分配哈希码。使用学号的前两位还是后两位更有意义？证明你的答案。

**练习 5.2** 考虑第 5.1.1 节中的哈希方案，且 $d = 2$ 和 $w = 3$。

1. 证明：对于任何乘数 $z$ 的选择，存在 4 个具有相同哈希码的值。（提示：这很简单，不需要任何数论知识。）
2. 给定乘数 $z$，描述 4 个具有相同哈希码的值。（提示：这更难，需要一些基础数论知识。）

**练习 5.3** 证明引理 5.1 中的界 $2/2^d$ 是最佳可能的界，通过证明如果 $2^{d+1} < 2^w$ 且 $x \neq y$，则 $\Pr[\texttt{hash}(x) = \texttt{hash}(y)] = 2/2^d$。（提示：查看 $x$ 和 $y$ 的二进制表示，并利用 $x - y$ 的事实。）

**练习 5.4** 使用第 1.3.2 节给出的完整版斯特林近似重新证明引理 5.4。

**练习 5.5** 考虑以下向 LinearHashTable 添加元素 $x$ 的简化版本代码，它将 $x$ 简单地存储在找到的第一个 $t[i]$ 条目中。解释为什么这可能非常慢，给出一个序列 $\texttt{add}$、$\texttt{remove}$ 和 $\texttt{find}$ 操作的示例，该序列的执行时间数量级为 $O(m^2)$。

**练习 5.6** 早期版本的 Java String 类的 `hashCode()` 方法不使用长字符串中的所有字符。例如，对于一个十六个字符的字符串，哈希码仅使用八个偶数索引字符计算。解释为什么这是一个非常糟糕的主意，给出一个大量字符串都拥有相同哈希码的示例。

**练习 5.7** 假设你有一个由两个 $w$ 位整数 $x$ 和 $y$ 组成的对象。证明为什么 $x \oplus y$（按位异或）不能作为该对象的良好哈希码。给出一个大量对象都拥有哈希码 0 的示例。

**练习 5.8** 假设你有一个由两个 $w$ 位整数 $x$ 和 $y$ 组成的对象。证明为什么 $x + y$ 不能作为该对象的良好哈希码。给出一个大量对象都拥有相同哈希码的示例。

**练习 5.9** 假设你有一个由两个 $w$ 位整数 $x$ 和 $y$ 组成的对象。假设你的对象的哈希码由某个确定性函数 $h(x,y)$ 定义，该函数产生一个单独的 $w$ 位整数。证明存在大量拥有相同哈希码的对象。

**练习 5.10** 设 $n = 2^d$ 对某个正整数 $d$。解释为什么，对于一个正整数 $x$，

$$
x \bmod n = x \& (n - 1)
$$

（这给出了一种通过重复设置 $x = x \& (x - 1)$ 直到 $x = 0$ 来计算 $\texttt{popcount}(x)$ 的算法。）

**练习 5.11** 找一些常用的哈希表实现，例如 Java 的 `HashSet` 或 `HashMap`，或本书中的 `ChainedHashTable` 或 `LinearHashTable` 实现，并设计一个程序在此数据结构中存储整数，使得存在整数 $x$，其 $\texttt{find}(x)$ 耗时线性。也就是说，找出一组 $n$ 个整数，其中有 $n$ 个元素哈希到同一个表位置。

根据实现的好坏，你可能只需检查实现的代码即可做到这一点，或者你可能需要编写一些代码来执行试验性插入和搜索，并计时添加和查找特定值所需的时间。（这已经并仍然被用来对 Web 服务器发起拒绝服务攻击 [17]。）
