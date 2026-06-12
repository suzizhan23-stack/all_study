# 第4章 跳跃表

在本章中，我们讨论一种优美的数据结构：跳跃表（skiplist），它有着广泛的应用。使用跳跃表，我们可以实现一个列表（List），其 `get(i)`、`set(i,x)`、`add(i,x)` 和 `remove(i)` 操作的时间复杂度均为 $O(\log n)$。我们还可以实现一个有序集合（SSet），其中所有操作的期望运行时间均为 $O(\log n)$。

跳跃表的效率依赖于随机化（randomization）的使用。当一个新的元素被添加到跳跃表时，跳跃表使用随机抛硬币的结果来决定新元素的高度。跳跃表的性能用期望运行时间和路径长度（expected running time and path length）来衡量。这里的期望是在跳跃表所使用的随机抛硬币结果上取的。在实现中，跳跃表所使用的随机抛硬币结果是通过伪随机数（或比特位）生成器模拟的。

## 4.1 基本结构

从概念上讲，跳跃表是 $L_0,\ldots,L_h$ 这样一个单向链表（singly-linked list）序列。每个列表 $L_r$ 包含 $L_{r-1}$ 中项的一个子集。我们从包含 $n$ 个项的输入列表 $L_0$ 开始，从 $L_0$ 构建 $L_1$，从 $L_1$ 构建 $L_2$，以此类推。$L_r$ 中的项是通过对 $L_{r-1}$ 中的每个元素 $x$ 抛一枚硬币，如果硬币正面朝上则将其包含在 $L_r$ 中。这个过程一直持续到我们创建了一个空列表 $L_h$ 为止。图 4.1 给出了一个跳跃表的示例。

![图 4.1: 包含七个元素的跳跃表](img1578.png)

**图 4.1:** 包含七个元素的跳跃表。

对于跳跃表中的一个元素 $x$，我们称 $x$ 的**高度**（height）为最大的 $r$ 使得 $x$ 出现在 $L_r$ 中。因此，例如，仅出现在 $L_0$ 中的元素的高度为 0。如果我们花点时间思考一下，就会发现 $x$ 的高度对应于以下实验：反复抛一枚硬币，直到它出现反面。它出现了多少次正面？不出所料，答案是一个节点的期望高度为 1（我们期望在得到反面之前抛两次硬币，但我们不计算最后一次抛掷）。跳跃表的**高度**是其最高节点的高度。

在每个列表的头部有一个特殊的节点，称为**哨兵**（sentinel），它充当列表的虚拟节点。跳跃表的关键特性是存在一条从 $L_h$ 中的哨兵到 $L_0$ 中每个节点的短路径，称为**搜索路径**（search path）。记住如何为节点 $u$ 构建搜索路径很容易（见图 4.2）：从跳跃表的左上角（$L_h$ 中的哨兵）开始，始终向右走，除非这样会越过 $u$，在这种情况下你应该向下进入下面的列表。

更精确地说，为了构建 $L_0$ 中节点 $u$ 的搜索路径，我们从 $L_h$ 中的哨兵 $w$ 开始。接下来，我们检查 $w.\mathit{next}$。如果 $w.\mathit{next}$ 包含一个在 $L_0$ 中出现在 $u$ 之前的项，那么我们设置 $w = w.\mathit{next}$。否则，我们向下移动并在列表 $L_{h-1}$ 中 $w$ 的出现位置继续搜索。我们继续这种方式，直到在 $L_0$ 中到达 $u$ 的前驱。

![图 4.2: 跳跃表中包含 4 的节点的搜索路径](img1604.png)

**图 4.2:** 跳跃表中包含 4 的节点的搜索路径。

以下结果（我们将在第 4.4 节中证明）表明搜索路径非常短：

> **引理 4.1** *$L_0$ 中任意节点 $u$ 的搜索路径的期望长度至多为 $2\log n + O(1) = O(\log n)$。*

一种节省空间的实现跳跃表的方法是定义一个节点 $u$，它由一个数据值 $x$ 和一个指针数组 $\mathit{next}$ 组成，其中 $u.\mathit{next}[i]$ 指向 $u$ 在列表 $L_i$ 中的后继。这样，节点中的数据 $x$ 只被引用一次，即使 $x$ 可能出现在多个列表中。

接下来两节讨论跳跃表的两种不同应用。在每一种应用中，$L_0$ 存储主要结构（一个元素列表或一个排序的元素集合）。这些结构之间的主要区别在于搜索路径的导航方式；具体来说，它们的不同之处在于如何决定搜索路径应该向下进入 $L_{r-1}$ 还是在 $L_r$ 中向右移动。

## 4.2 SkiplistSSet：高效的有序集合

SkiplistSSet 使用跳跃表结构来实现 SSet 接口。以这种方式使用时，列表 $L_0$ 按排序顺序存储 SSet 的元素。`find(x)` 方法通过遵循满足 $y \ge x$ 的最小值 $y$ 的搜索路径来工作：

```python
def find_pred(self, x):
    u = self._sentinel
    r = self._height - 1
    while r >= 0:
        while u.next[r] is not None and u.next[r].x < x:
            u = u.next[r]
        r -= 1
    return u

def find(self, x):
    u = self.find_pred(x)
    if u.next[0] is None:
        return None
    return u.next[0].x
```

遵循 $y$ 的搜索路径很容易：当位于 $L_r$ 中的某个节点 $u$ 时，我们向右查看 $u.\mathit{next}[r].x$。如果 $x > u.\mathit{next}[r].x$，那么我们在 $L_r$ 中向右走一步；否则，我们向下移动到 $L_{r-1}$。这个搜索中的每一步（向右或向下）只需要常数时间；因此，根据引理 4.1，`find(x)` 的期望运行时间为 $O(\log n)$。

在我们可以向 SkipListSSet 添加元素之前，我们需要一个方法来模拟抛硬币以确定新节点的高度 $k$。我们通过选择一个随机整数 $z$ 并计算 $z$ 的二进制表示中尾随 1 的个数来实现这一点[^4.1]：

```python
def pick_height(self):
    z = random.getrandbits(32)
    k = 0
    while z & 1:
        k += 1
        z >>= 1
    return k
```

为了在 SkiplistSSet 中实现 `add(x)` 方法，我们搜索 $x$，然后将 $x$ 拼接到几个列表 $L_0,\ldots,L_k$ 中，其中 $k$ 是使用 `pick_height()` 方法选择的。最简单的方法是使用一个数组 `stack`，它记录搜索路径从某个列表 $L_r$ 向下进入 $L_{r-1}$ 时的节点。更精确地说，`stack[r]` 是 $L_r$ 中搜索路径向下进入 $L_{r-1}$ 的节点。我们修改以插入 $x$ 的节点正是 $\mathit{stack}[0],\ldots,\mathit{stack}[k]$。以下代码实现了 `add(x)` 的算法：

```python
def add(self, x):
    u = self._sentinel
    r = self._height - 1
    stack = [None] * (self._height + 1)
    while r >= 0:
        while u.next[r] is not None and u.next[r].x < x:
            u = u.next[r]
        stack[r] = u
        r -= 1
    if u.next[0] is not None and u.next[0].x == x:
        return False
    k = self.pick_height()
    w = self._new_node(x, k)
    while self._height < k + 1:
        self._height += 1
        stack[self._height - 1] = self._sentinel
    for i in range(k + 1):
        w.next[i] = stack[i].next[i]
        stack[i].next[i] = w
    self._n += 1
    return True
```

![图 4.3: 将包含 3.5 的节点添加到跳跃表。stack 中存储的节点被高亮显示。](img1660.png)

**图 4.3:** 将包含 3.5 的节点添加到跳跃表。`stack` 中存储的节点被高亮显示。

移除元素 $x$ 以类似的方式完成，只是不需要 `stack` 来跟踪搜索路径。我们可以在跟随搜索路径的同时进行移除。我们搜索 $x$，每次搜索从节点 $u$ 向下移动时，我们检查 $u.\mathit{next}.x == x$，如果是，则将 $u$ 从列表中拼接出来：

```python
def remove(self, x):
    u = self._sentinel
    r = self._height - 1
    removed = False
    while r >= 0:
        while u.next[r] is not None and u.next[r].x < x:
            u = u.next[r]
        if u.next[r] is not None and u.next[r].x == x:
            removed = True
            u.next[r] = u.next[r].next[r]
            if u == self._sentinel and u.next[r] is None:
                self._height -= 1
        r -= 1
    if removed:
        self._n -= 1
    return removed
```

![图 4.4: 从跳跃表中移除包含 3 的节点。](img1670.png)

**图 4.4:** 从跳跃表中移除包含 3 的节点。

### 4.2.1 小结

以下定理总结了将跳跃表用于实现有序集合时的性能：

> **定理 4.1** *SkiplistSSet 实现了 SSet 接口。SkiplistSSet 支持 `add(x)`、`remove(x)` 和 `find(x)` 操作，每次操作的期望时间为 $O(\log n)$。*

## 4.3 SkiplistList：高效的随机访问列表

SkiplistList 使用跳跃表结构来实现 List 接口。在 SkiplistList 中，$L_0$ 按列表中的顺序包含列表的元素。与 SkiplistSSet 一样，可以在 $O(\log n)$ 时间内添加、移除和访问元素。

为了实现这一点，我们需要一种方法来遵循 $L_0$ 中第 $i$ 个元素的搜索路径。最简单的方法是定义某个列表 $L_r$ 中边的**长度**（length）的概念。我们将 $L_0$ 中每条边的长度定义为 1。$L_r$（$r > 0$）中边 $e$ 的长度定义为 $e$ 下方 $L_{r-1}$ 中边的长度之和。等价地，$e$ 的长度是 $L_0$ 中 $e$ 下方的边的数量。图 4.5 显示了一个跳跃表及其边的长度。由于跳跃表的边存储在数组中，长度也可以以相同的方式存储：

```python
class Node(object):
    def __init__(self, x, h):
        self.x = x
        self.next = [None] * h
        self.length = [0] * h
```

![图 4.5: 跳跃表中边的长度。](img1691.png)

**图 4.5:** 跳跃表中边的长度。

这种长度定义的有用性质是，如果当前在 $L_0$ 中位置为 $j$ 的节点处，并且我们沿着一条长度为 $\ell$ 的边移动，那么我们将移动到 $L_0$ 中位置为 $j + \ell$ 的节点。这样，在跟随搜索路径时，我们可以跟踪当前节点在 $L_0$ 中的位置 $j$。当在 $L_r$ 中的节点 $u$ 时，如果 $j$ 加上边 $u.\mathit{next}[r]$ 的长度小于 $i$，则向右走；否则，向下进入 $L_{r-1}$。

```python
def find_pred(self, i):
    u = self._sentinel
    r = self._height - 1
    j = -1
    while r >= 0:
        while u.next[r] is not None and j + u.length[r] < i:
            j += u.length[r]
            u = u.next[r]
        r -= 1
    return u

def get(self, i):
    u = self.find_pred(i)
    return u.next[0].x

def set(self, i, x):
    u = self.find_pred(i)
    y = u.next[0].x
    u.next[0].x = x
    return y
```

由于 `get(i)` 和 `set(i,x)` 操作中最困难的部分是找到 $L_0$ 中的第 $i$ 个节点，这些操作在 $O(\log n)$ 时间内运行。

在 SkiplistList 的位置 $i$ 处添加元素相当简单。与 SkiplistSSet 不同，我们确定新节点实际上会被添加，因此我们可以在搜索新节点位置的同时进行添加。我们首先选择新插入节点 $w$ 的高度 $k$，然后遵循 $i$ 的搜索路径。每当搜索路径从 $L_r$ 向下移动且 $r \le k$ 时，我们将 $w$ 拼接到 $L_r$ 中。唯一需要额外注意的是确保边的长度被正确更新。参见图 4.6。

![图 4.6: 向 SkiplistList 添加元素。](img1720.png)

**图 4.6:** 向 SkiplistList 添加元素。

注意，每次搜索路径在 $L_r$ 中的节点 $u$ 处向下时，边 $u.\mathit{next}[r]$ 的长度增加 1，因为我们在该边下方的位置 $i$ 处添加了一个元素。将节点 $w$ 拼接到两个节点 $u$ 和 $z$ 之间，如图 4.7 所示。在跟随搜索路径时，我们已经跟踪了 $u$ 在 $L_0$ 中的位置 $j$。因此，我们知道从 $u$ 到 $w$ 的边的长度为 $i - j$。我们还可以从 $u$ 到 $z$ 的边的长度 $\ell$ 推导出从 $w$ 到 $z$ 的边的长度。因此，我们可以在常数时间内拼接 $w$ 并更新边的长度。

![图 4.7: 将节点 w 拼接到跳跃表时更新边的长度。](img1740.png)

**图 4.7:** 将节点 $w$ 拼接到跳跃表时更新边的长度。

这听起来比实际复杂，因为代码实际上相当简单：

```python
def add(self, i, x):
    w = self._new_node(x, self.pick_height())
    if w.next.length > self._height:
        self._height = w.next.length
    u = self._sentinel
    r = self._height - 1
    j = -1
    while r >= 0:
        while u.next[r] is not None and j + u.length[r] < i:
            j += u.length[r]
            u = u.next[r]
        u.length[r] += 1
        if r < len(w.next):
            w.next[r] = u.next[r]
            u.next[r] = w
            w.length[r] = u.length[r] - (i - j)
            u.length[r] = i - j
        r -= 1
    self._n += 1
    return u
```

到现在为止，SkiplistList 中 `remove(i)` 操作的实现应该是显而易见的。我们遵循位置 $i$ 处节点的搜索路径。每次搜索路径在级别 $r$ 处从节点 $u$ 向下迈出一步时，我们减少 $u$ 在该级别离开的边的长度。我们还检查 $u.\mathit{next}[r]$ 是否为排名 $i$ 的元素，如果是，则在该级别将其从列表中拼接出去。图 4.8 显示了一个示例。

![图 4.8: 从 SkiplistList 中移除元素。](img1751.png)

**图 4.8:** 从 SkiplistList 中移除元素。

```python
def remove(self, i):
    u = self._sentinel
    r = self._height - 1
    j = -1
    x = None
    while r >= 0:
        while u.next[r] is not None and j + u.length[r] < i:
            j += u.length[r]
            u = u.next[r]
        u.length[r] -= 1
        if u.next[r] is not None and j + u.length[r] + 1 == i:
            x = u.next[r].x
            u.length[r] += u.next[r].length[r]
            u.next[r] = u.next[r].next[r]
            if u == self._sentinel and u.next[r] is None:
                self._height -= 1
        r -= 1
    self._n -= 1
    return x
```

### 4.3.1 小结

以下定理总结了 SkiplistList 数据结构的性能：

> **定理 4.2** *SkiplistList 实现了 List 接口。SkiplistList 支持 `get(i)`、`set(i,x)`、`add(i,x)` 和 `remove(i)` 操作，每次操作的期望时间为 $O(\log n)$。*

## 4.4 跳跃表的分析

在本节中，我们分析跳跃表的期望高度、大小和搜索路径长度。本节需要基本的概率论知识。几个证明基于以下关于抛硬币的基本观察。

> **引理 4.2** *设 $T$ 为抛一枚均匀硬币直到第一次出现正面所需的抛掷次数（包含该次）。则 $\mathrm{E}[T]=2$。*

*证明*。假设我们在第一次出现正面时停止抛硬币。定义指示变量
$$
I_{i} = \left\{\begin{array}{ll}
     0 & \text{如果硬币被抛的次数少于 $i$ 次} \\
     1 & \text{如果硬币被抛了 $i$ 次或更多次}
     \end{array}\right.
$$
注意 $I_i=1$ 当且仅当前 $i-1$ 次抛掷都是反面，所以 $\mathrm{E}[I_i]=\Pr\{I_i=1\}=1/2^{i-1}$。观察 $T$，即总的抛掷次数，可以写成 $T=\sum_{i=1}^{\infty} I_i$。因此，
$$
\begin{aligned}
\mathrm{E}[T] &= \mathrm{E}\left[\sum_{i=1}^\infty I_i\right] \\
&= \sum_{i=1}^\infty \mathrm{E}\left[I_i\right] \\
&= \sum_{i=1}^\infty 1/2^{i-1} \\
&= 1 + 1/2 + 1/4 + 1/8 + \cdots \\
&= 2 \enspace .
\end{aligned}
$$
∎

以下两个引理告诉我们跳跃表具有线性大小：

> **引理 4.3** *包含 $n$ 个元素（不包括哨兵出现次数）的跳跃表的期望节点数为 $2n$。*

*证明*。任意特定元素 $x$ 被包含在列表 $L_r$ 中的概率为 $1/2^r$，所以 $L_r$ 中的期望节点数为 $n/2^r$。[^4.2] 因此，所有列表中节点总数的期望为
$$
\sum_{r=0}^\infty n/2^{r} = n(1+1/2+1/4+1/8+\cdots) = 2n \enspace .
$$
∎

> **引理 4.4** *包含 $n$ 个元素的跳跃表的期望高度至多为 $\log n + 2$。*

*证明*。对于每个 $r \in \{1,2,3,\ldots,\infty\}$，定义指示随机变量
$$
I_{r} = \left\{\begin{array}{ll}
     0 & \text{如果 $L_r$ 为空} \\
     1 & \text{如果 $L_r$ 非空}
     \end{array}\right.
$$
跳跃表的高度 $h$ 由下式给出：
$$
h = \sum_{i=1}^\infty I_{r} \enspace .
$$
注意 $I_{r}$ 永远不会超过 $L_r$ 的长度 $|L_r|$，所以
$$
\mathrm{E}[I_{r}] \le \mathrm{E}[|L_{r}|] = n/2^{r} \enspace .
$$
因此，我们有
$$
\begin{aligned}
\mathrm{E}[h] &= \mathrm{E}\left[\sum_{r=1}^\infty I_{r}\right] \\
&= \sum_{r=1}^{\infty} E[I_{r}] \\
&= \sum_{r=1}^{\lfloor \log n \rfloor} E[I_{r}] + \sum_{r=\lfloor \log n \rfloor+1}^{\infty} E[I_{r}] \\
&\le \sum_{r=1}^{\lfloor \log n \rfloor} 1 + \sum_{r=\lfloor \log n \rfloor+1}^{\infty} n/2^{r} \\
&\le \log n + \sum_{r=0}^{\infty} 1/2^{r} \\
&= \log n + 2 \enspace .
\end{aligned}
$$
∎

> **引理 4.5** *包含 $n$ 个元素（包括所有哨兵出现次数）的跳跃表的期望节点数为 $2n + O(\log n)$。*

*证明*。根据引理 4.3，不包括哨兵的期望节点数为 $2n$。哨兵的出现次数等于跳跃表的高度 $h$，因此根据引理 4.4，哨兵的期望出现次数至多为 $\log n + 2 = O(\log n)$。∎

> **引理 4.6** *跳跃表中搜索路径的期望长度至多为 $2\log n + O(1)$。*

*证明*。理解这一点最简单的方法是考虑节点 $x$ 的**反向搜索路径**（reverse search path）。这条路径从 $L_0$ 中 $x$ 的前驱开始。在任何时刻，如果路径可以向上走一层，它就向上走。如果不能向上走，它就向左走。思考片刻就会相信，$x$ 的反向搜索路径与 $x$ 的搜索路径相同，只是顺序相反。

反向搜索路径在特定级别 $r$ 访问的节点数量与以下实验有关：抛一枚硬币。如果硬币出现正面，则向上移动并停止。否则，向左移动并重复实验。在出现正面之前的抛硬币次数代表了反向搜索路径在特定级别向左走的步数。[^4.3] 引理 4.2 告诉我们，在第一次正面出现之前的期望抛硬币次数为 1。

设 $S_r$ 表示前向搜索路径在级别 $r$ 向右走的步数。我们刚刚论证了 $\mathrm{E}[S_r] \le 1$。此外，$S_r \le |L_r|$，因为我们在 $L_r$ 中走的步数不能超过 $L_r$ 的长度，所以
$$
\mathrm{E}[S_r] \le \mathrm{E}[|L_r|] = n/2^{r} \enspace .
$$
我们现在可以像引理 4.4 的证明那样完成。设 $S$ 为跳跃表中某个节点 $u$ 的搜索路径长度，设 $h$ 为跳跃表的高度。那么
$$
\begin{aligned}
\mathrm{E}[S] &= \mathrm{E}\left[ h + \sum_{r=0}^\infty S_r \right] \\
&= \mathrm{E}[h] + \sum_{r=0}^\infty \mathrm{E}[S_r] \\
&= \mathrm{E}[h] + \sum_{r=0}^{\lfloor \log n \rfloor} \mathrm{E}[S_r] + \sum_{r=\lfloor \log n \rfloor+1}^\infty \mathrm{E}[S_r] \\
&\le \mathrm{E}[h] + \sum_{r=0}^{\lfloor \log n \rfloor} 1 + \sum_{r=\lfloor \log n \rfloor+1}^{\infty} n/2^{r} \\
&\le \mathrm{E}[h] + \sum_{r=0}^{\lfloor \log n \rfloor} 1 + \sum_{r=0}^{\infty} 1/2^{r} \\
&\le \mathrm{E}[h] + \log n + 3 \\
&\le 2\log n + 5 \enspace .
\end{aligned}
$$
∎

以下定理总结了本节的结果：

> **定理 4.3** *包含 $n$ 个元素的跳跃表的期望大小为 $O(n)$，且任意特定元素的搜索路径的期望长度至多为 $2\log n + O(1)$。*

## 4.5 讨论与练习

跳跃表由 Pugh [60] 提出，他还介绍了跳跃表的许多应用和扩展 [59]。自那以后，它们得到了广泛的研究。几位研究人员对跳跃表中第 $i$ 个元素的搜索路径的期望长度和方差进行了非常精确的分析 [45,44,56]。确定性版本 [53]、偏置版本 [8,26] 和自调整版本 [12] 的跳跃表都已被开发出来。跳跃表实现已经为各种语言和框架编写，并已被用于开源数据库系统 [69,61]。跳跃表的一个变体用于 HP-UX 操作系统内核的进程管理结构中 [42]。

---

> **练习 4.1** 在图 4.1 的跳跃表上画出 2.5 和 5.5 的搜索路径。

> **练习 4.2** 在图 4.1 的跳跃表上画出添加值 0.5（高度为 1）和 3.5（高度为 2）的过程。

> **练习 4.3** 在图 4.1 的跳跃表上画出移除值 1 和 3 的过程。

> **练习 4.4** 在图 4.5 的 SkiplistList 上画出执行 `remove(2)` 的过程。

> **练习 4.5** 在图 4.5 的 SkiplistList 上画出执行 `add(3,x)` 的过程。假设 `pick_height()` 为新创建的节点选择高度 4。

> **练习 4.6** 证明在执行 `add(x)` 或 `remove(x)` 操作期间，SkiplistSet 中被更改的指针的期望数量是常数。

> **练习 4.7** 假设我们不是基于抛硬币将元素从 $L_{i-1}$ 提升到 $L_i$，而是以概率 $p$（$0 < p < 1$）提升它。
>
> 1. 证明经过此修改后，搜索路径的期望长度至多为 $(1/p)\log_{1/p} n + O(1)$。
> 2. 最小化上述表达式的 $p$ 值是多少？
> 3. 跳跃表的期望高度是多少？
> 4. 跳跃表中的期望节点数是多少？

> **练习 4.8** SkiplistSet 中的 `find(x)` 方法有时会执行**冗余比较**（redundant comparison）；当 $x$ 与同一个值比较多次时就会发生这种情况。当某个节点 $u$ 满足 $u.\mathit{next}[r] = u.\mathit{next}[r-1]$ 时，就可能发生这种情况。展示这些冗余比较是如何发生的，并修改 `find(x)` 以避免它们。分析修改后的 `find(x)` 方法的期望比较次数。

> **练习 4.9** 设计并实现一个跳跃表的版本，它实现 SSet 接口，同时还支持按排名快速访问元素。即，它还支持函数 `get(i)`，该函数在 $O(\log n)$ 期望时间内返回排名为 $i$ 的元素。（SSet 中元素 $x$ 的排名是 SSet 中小于 $x$ 的元素数量。）

> **练习 4.10** 跳跃表中的**手指**（finger）是一个数组，它存储搜索路径上路径向下走的节点序列。（第 4.2 节 `add(x)` 代码中的变量 `stack` 就是一个手指；图 4.3 中高亮的节点显示了手指的内容。）可以认为手指指向最低列表 $L_0$ 中某个节点的路径。
>
> **手指搜索**（finger search）使用手指来实现 `find(x)` 操作，它利用手指向上遍历列表，直到到达一个满足 $u.x < x$ 且 $u.\mathit{next} = \mathit{nil}$ 或 $u.\mathit{next}.x > x$ 的节点 $u$，然后从 $u$ 开始对 $x$ 进行正常搜索。可以证明手指搜索所需的期望步数为 $O(1+\log r)$，其中 $r$ 是 $L_0$ 中在 $x$ 和手指指向的值之间的值的数量。
>
> 实现 Skiplist 的一个子类 SkiplistWithFinger，它使用内部手指实现 `find(x)` 操作。该子类存储一个手指，然后每个 `find(x)` 操作都作为手指搜索来实现。在每个 `find(x)` 操作期间，手指被更新，使得每个 `find(x)` 操作都使用指向上一次 `find(x)` 操作结果的手指作为起点。

> **练习 4.11** 编写一个方法 `truncate(i)`，它在位置 $i$ 处截断一个 SkiplistList。在此方法执行后，列表的大小为 $i$，并且只包含索引 $0,\ldots,i-1$ 处的元素。返回值是另一个 SkiplistList，它包含索引 $i,\ldots,n-1$ 处的元素。此方法应在 $O(\log n)$ 时间内运行。

> **练习 4.12** 编写一个 SkiplistList 方法 `absorb(l_2)`，它接受一个 SkiplistList `l_2` 作为参数，将其清空并按顺序将其内容追加到接收者中。例如，如果 `l_1` 包含 $a,b,c$，`l_2` 包含 $d,e,f$，那么调用 `l_1.absorb(l_2)` 后，`l_1` 将包含 $a,b,c,d,e,f$，而 `l_2` 将为空。此方法应在 $O(\log n)$ 时间内运行。

> **练习 4.13** 使用节省空间的列表 SEList 的思路，设计并实现一个节省空间的有序集合 SESSet。为此，将数据按顺序存储在一个 SEList 中，并将该 SEList 的块存储在一个 SSet 中。如果原始 SSet 实现使用 $O(n)$ 空间存储 $n$ 个元素，那么 SESSet 将使用足够存储 $n$ 个元素的空间加上 $O(n/b + b)$ 的浪费空间。

> **练习 4.14** 使用 SSet 作为底层结构，设计并实现一个应用程序，它读取一个（大型）文本文件，并允许你交互式地搜索文本中包含的任何子串。当用户输入查询时，文本的匹配部分（如果有）应作为结果出现。
>
> 提示 1：每个子串都是某个后缀的前缀，因此存储文本文件的所有后缀就足够了。
>
> 提示 2：任何后缀都可以紧凑地表示为一个整数，指示该后缀在文本中的起始位置。
>
> 在一些大型文本（如 Project Gutenberg [1] 提供的书籍）上测试你的应用程序。如果实现正确，你的应用程序将非常灵敏；按键输入和看到结果之间不应有明显的延迟。

> **练习 4.15** （此练习应在阅读第 6.2 节关于二叉搜索树的内容后进行。）以以下方式比较跳跃表和二叉搜索树：
>
> 1. 解释移除跳跃表中的某些边如何导致一个看起来像二叉树且类似于二叉搜索树的结构。
> 2. 跳跃表和二叉搜索树使用大致相同数量的指针（每个节点 2 个）。然而，跳跃表更好地利用了这些指针。解释原因。

---

[^4.1]: 这种方法并不完全复制抛硬币实验，因为 $k$ 的值将始终小于一个 `int` 中的位数。然而，这产生的影响可以忽略不计，除非结构中的元素数量远大于 $2^{32}=4294967296$。

[^4.2]: 参见第 1.3.4 节了解如何使用指示变量和期望线性推导出这一点。

[^4.3]: 注意，这可能会多计向左走的步数，因为实验应该在第一次出现正面时或搜索路径到达哨兵时结束（以先到者为准）。这不是问题，因为引理只声明了一个上界。
