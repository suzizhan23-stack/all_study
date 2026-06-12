# 第13章 整数数据结构 (Data Structures for Integers)

在本章中，我们回到实现 SSet 的问题。不同之处在于，我们假设存储在 SSet 中的元素是 $w$ 位整数。也就是说，我们希望实现 $\mathrm{add}(x)$、$\mathrm{remove}(x)$ 和 $\mathrm{find}(x)$，其中 $x \in \{0,\ldots,2^w-1\}$。很容易想到许多应用场景，其中数据——或者至少是我们用于排序数据的键——是整数。

我们将讨论三种数据结构，每一种都建立在之前的思想之上。第一种结构 BinaryTrie 以 $O(w)$ 时间执行所有三种 SSet 操作。这并不令人印象深刻，因为 $\{0,\ldots,2^w-1\}$ 的任何子集的大小 $n \le 2^w$，因此 $\log n \le w$。本书讨论的所有其他 SSet 实现都以 $O(\log n)$ 时间执行所有操作，因此它们至少与 BinaryTrie 一样快。

第二种结构 XFastTrie 通过使用哈希（hashing）来加速 BinaryTrie 中的搜索。借助这种加速，$\mathrm{find}(x)$ 操作在 $O(\log w)$ 时间内运行。然而，XFastTrie 中的 $\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 操作仍然需要 $O(w)$ 时间，并且 XFastTrie 使用的空间为 $O(n\cdot w)$。

第三种数据结构 YFastTrie 使用 XFastTrie 仅存储大约每 $w$ 个元素中的一个样本，并将剩余元素存储在标准的 SSet 结构中。这个技巧将 $\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 的运行时间降低到 $O(\log w)$，并将空间减少到 $O(n)$。

本章用作示例的实现可以存储任何类型的数据，只要可以关联一个整数。在代码示例中，变量 $ix$ 始终是与 $x$ 关联的整数值，方法 $\mathrm{int\_value}(x)$ 将 $x$ 转换为其关联的整数。但在文本中，我们将简单地视 $x$ 为一个整数。

## 13.1 BinaryTrie：数字搜索树 (A Digital Search Tree)

BinaryTrie 将一组 $w$ 位整数编码为一棵二叉树。树中的所有叶子节点深度均为 $w$，每个整数编码为一条从根到叶子的路径。整数 $x$ 的路径在 level $i$ 处向左转，如果 $x$ 的第 $i$ 个最高有效位是 0，则向右转。图 13.1 展示了 $w=4$ 时的示例，其中 trie 存储了整数 3(0011)、9(1001)、12(1100) 和 13(1101)。

**图 13.1：** 存储在二进制 trie 中的整数被编码为从根到叶子的路径。

由于值 $x$ 的搜索路径取决于 $x$ 的二进制位，因此为节点的子节点命名会很有帮助：$u.child[0]$（左子节点）和 $u.child[1]$（右子节点）。这些子指针实际上将起到双重作用。由于二进制 trie 中的叶子节点没有子节点，这些指针被用于将叶子节点串成一个双向链表（doubly-linked list）。对于二进制 trie 中的叶子节点，$u.child[0]$（prev）是链表中 $u$ 的前驱节点，$u.child[1]$（next）是链表中 $u$ 的后继节点。一个特殊的节点 $dummy$ 被用在链表的第一个节点之前和最后一个节点之后（参见第 3.2 节）。

每个节点 $u$ 还包含一个额外的指针 $u.jump$。如果 $u$ 缺少左子节点，那么 $u.jump$ 指向 $u$ 的子树中最小的叶子节点。如果 $u$ 缺少右子节点，那么 $u.jump$ 指向 $u$ 的子树中最大的叶子节点。图 13.2 展示了一个 BinaryTrie 的示例，显示了 $jump$ 指针和叶子节点处的双向链表。

**图 13.2：** 一个 BinaryTrie，其中 $jump$ 指针以弯曲虚线边显示。

BinaryTrie 中的 $\mathrm{find}(x)$ 操作相当直接。我们尝试在 trie 中跟随 $x$ 的搜索路径。如果我们到达一个叶子节点，那么我们就找到了 $x$。如果我们到达一个无法继续前进的节点 $u$（因为 $u$ 缺少某个子节点），那么我们就跟随 $u.jump$，它要么将我们带到大于 $x$ 的最小叶子节点，要么将我们带到小于 $x$ 的最大叶子节点。这两种情况中哪一种发生取决于 $u$ 缺少的是左子节点还是右子节点。在前一种情况（$u$ 缺少左子节点）中，我们已经找到了我们想要的节点。在后一种情况（$u$ 缺少右子节点）中，我们可以使用链表到达我们想要的节点。图 13.3 展示了每种情况。

**图 13.3：** $\mathrm{find}(5)$ 和 $\mathrm{find}(8)$ 所遵循的路径。

$\mathrm{find}(x)$ 方法的运行时间取决于跟随从根到叶子的路径所需的时间，因此它在 $O(w)$ 时间内运行。

BinaryTrie 中的 $\mathrm{add}(x)$ 操作也相当直接，但有很多工作要做：

1. 跟随 $x$ 的搜索路径，直到到达一个无法继续前进的节点 $u$。
2. 创建从 $u$ 到包含 $x$ 的叶子节点的剩余搜索路径。
3. 将包含 $x$ 的节点 $u'$ 添加到叶子节点的链表中（在第 1 步中遇到的最后一个节点 $u$ 的 $jump$ 指针提供了链表中 $u'$ 的前驱节点 $pred$）。
4. 沿 $x$ 的搜索路径向上回溯，调整那些 $jump$ 指针现在应指向 $x$ 的节点的 $jump$ 指针。

图 13.4 展示了一次添加操作。

**图 13.4：** 将值 2 和 15 添加到图 13.2 的 BinaryTrie 中。

此方法执行一次沿 $x$ 搜索路径向下和一次向上。每一步花费常数时间，因此 $\mathrm{add}(x)$ 方法在 $O(w)$ 时间内运行。

$\mathrm{remove}(x)$ 操作撤销了 $\mathrm{add}(x)$ 的工作。与 $\mathrm{add}(x)$ 类似，它有很多工作要做：

1. 跟随 $x$ 的搜索路径，直到到达包含 $x$ 的叶子节点 $u$。
2. 从双向链表中移除 $u$。
3. 删除 $u$，然后沿 $x$ 的搜索路径向上回溯，删除节点，直到到达一个拥有不在 $x$ 搜索路径上的子节点的节点 $v$。
4. 从 $v$ 向上到根节点，更新任何指向 $u$ 的 $jump$ 指针。

图 13.5 展示了一次移除操作。

**图 13.5：** 从图 13.2 的 BinaryTrie 中移除值 9。

**定理 13.1.** *BinaryTrie 为 $w$ 位整数实现了 SSet 接口。BinaryTrie 支持 $\mathrm{add}(x)$、$\mathrm{remove}(x)$ 和 $\mathrm{find}(x)$ 操作，每次操作时间为 $O(w)$。存储 $n$ 个值的 BinaryTrie 使用的空间为 $O(n\cdot w)$。*

## 13.2 XFastTrie：双对数时间搜索 (Searching in Doubly-Logarithmic Time)

BinaryTrie 结构的性能并不令人印象深刻。结构中存储的元素数量 $n$ 最多为 $2^w$，因此 $\log n \le w$。换句话说，本书其他部分描述的任何基于比较的 SSet 结构至少与 BinaryTrie 一样高效，并且不受限于仅存储整数。

接下来我们描述 XFastTrie，它只是一个带有 $w+1$ 个哈希表（hash tables）的 BinaryTrie——每个 trie 层级一个。这些哈希表用于将 $\mathrm{find}(x)$ 操作加速到 $O(\log w)$ 时间。回顾一下，BinaryTrie 中的 $\mathrm{find}(x)$ 操作在到达一个节点 $u$ 时几乎完成，此时 $x$ 的搜索路径想要前进到 $u.right$（或 $u.left$），但 $u$ 没有右（相应地，左）子节点。此时，搜索使用 $u.jump$ 跳转到 BinaryTrie 的一个叶子节点 $v$，并返回 $v$ 或其在叶子节点链表中的后继节点。XFastTrie 通过在 trie 的层级上使用二分搜索（binary search）来定位节点 $u$，从而加速搜索过程。

为了使用二分搜索，我们需要一种方法来确定我们正在寻找的节点 $u$ 是在特定层级 $i$ 之上，还是在层级 $i$ 或之下。这些信息由 $x$ 的二进制表示中的最高 $i$ 位提供；这些位决定了 $x$ 从根节点到层级 $i$ 的搜索路径。例如，参见图 13.6；在该图中，14（其二进制表示为 1110）的搜索路径上的最后一个节点 $u$ 是标有 $11{*}{*}$ 的层级 2 节点，因为在层级 3 没有标有 $111{*}$ 的节点。因此，我们可以将层级 $i$ 处的每个节点标为一个 $i$ 位整数。那么，我们正在搜索的节点 $u$ 在层级 $i$ 或以下，当且仅当在层级 $i$ 存在一个节点，其标签与 $x$ 的最高 $i$ 位匹配。

**图 13.6：** 由于没有标有 $111\star$ 的节点，14 (1110) 的搜索路径在标有 $11\star\star$ 的节点处结束。

在 XFastTrie 中，我们为每个 $i \in \{0,\ldots,w\}$ 将层级 $i$ 处的所有节点存储在一个 USet 中，记为 $t[i]$，该 USet 实现为哈希表（第 5 章）。使用这个 USet，我们可以在常数期望时间内检查层级 $i$ 是否存在一个标签与 $x$ 的最高 $i$ 位匹配的节点。实际上，我们甚至可以使用 $t[i].\mathrm{find}(x \gg (w-i))$ 来找到这个节点。

哈希表 $t[0],\ldots,t[w]$ 使我们能够使用二分搜索来找到 $u$。最初，我们知道 $u$ 在某个层级 $i$ 处，满足 $0 \le i < w+1$。因此我们初始化 $\ell=0$ 和 $h=w+1$，并反复查看哈希表 $t[i]$，其中 $i = \lfloor (\ell+h)/2 \rfloor$。如果 $t[i]$ 包含一个标签与 $x$ 的最高 $i$ 位匹配的节点，则我们设置 $\ell \gets i$（$u$ 在层级 $i$ 或以下）；否则我们设置 $h \gets i$（$u$ 在层级 $i$ 以上）。当 $h-\ell \le 1$ 时此过程终止，此时我们确定 $u$ 在层级 $\ell$。然后我们使用 $u.jump$ 和叶子节点的双向链表完成 $\mathrm{find}(x)$ 操作。

上述方法中 while 循环的每次迭代将 $h-\ell$ 减少大约一半，因此该循环在 $O(\log w)$ 次迭代后找到 $u$。每次迭代执行恒定量的工作以及在 USet 中的一次 $\mathrm{find}(x)$ 操作，这需要常数期望时间。剩余工作只需常数时间，因此 XFastTrie 中的 $\mathrm{find}(x)$ 方法只需要 $O(\log w)$ 期望时间。

XFastTrie 的 $\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 方法与 BinaryTrie 中的相同方法几乎相同。唯一的修改是管理哈希表 $t[0],\ldots,t[w]$。在 $\mathrm{add}(x)$ 操作期间，当在层级 $i$ 创建新节点时，该节点被添加到 $t[i]$。在 $\mathrm{remove}(x)$ 操作期间，当从层级 $i$ 移除节点时，该节点从 $t[i]$ 中移除。由于从哈希表中添加和移除需要常数期望时间，这不会使 $\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 的运行时间增加超过一个常数因子。我们省略了 $\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 的代码清单，因为这些代码与已经在 BinaryTrie 中为相同方法提供的（较长的）代码清单几乎相同。

以下定理总结了 XFastTrie 的性能：

**定理 13.2.** *XFastTrie 为 $w$ 位整数实现了 SSet 接口。XFastTrie 支持：*
- *$\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$，每次操作期望时间 $O(w)$，以及*
- *$\mathrm{find}(x)$，每次操作期望时间 $O(\log w)$。*
*存储 $n$ 个值的 XFastTrie 使用的空间为 $O(n\cdot w)$。*

## 13.3 YFastTrie：双对数时间 SSet (A Doubly-Logarithmic Time SSet)

XFastTrie 在查询时间上比 BinaryTrie 有了巨大的——甚至是指数级的——改进，但 $\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 操作仍然不是非常快。此外，空间使用量 $O(n\cdot w)$ 高于本书描述的其他 SSet 实现，后者都使用 $O(n)$ 空间。这两个问题是相关的；如果 $n$ 次 $\mathrm{add}(x)$ 操作构建了一个大小为 $n\cdot w$ 的结构，那么 $\mathrm{add}(x)$ 操作每次至少需要 $w$ 量级的时间（和空间）。

接下来讨论的 YFastTrie 同时改进了 XFastTrie 的空间和速度。YFastTrie 使用一个 XFastTrie $xft$，但只在 $xft$ 中存储 $O(n/w)$ 个值。这样，$xft$ 使用的总空间仅为 $O(n)$。此外，YFastTrie 中每 $w$ 个 $\mathrm{add}(x)$ 或 $\mathrm{remove}(x)$ 操作中只有一个会导致 $xft$ 中的一次 $\mathrm{add}(x)$ 或 $\mathrm{remove}(x)$ 操作。通过这样做，调用 $xft$ 的 $\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 操作所产生的平均成本仅为常数。

显而易见的问题是：如果 $xft$ 只存储 $n/w$ 个元素，剩下的 $n(1-1/w)$ 个元素去哪里了？这些元素进入**二级结构**（secondary structures），在这里是 treap（第 7.2 节）的扩展版本。大约有 $n/w$ 个这样的二级结构，因此平均而言，每个结构存储 $O(w)$ 个元素。Treap 支持对数时间的 SSet 操作，因此对这些 treap 的操作将在 $O(\log w)$ 时间内运行，符合要求。

更具体地说，YFastTrie 包含一个 XFastTrie $xft$，其中包含数据的随机样本，每个元素独立地以概率 $1/w$ 出现在样本中。为方便起见，值 $2^w-1$ 始终包含在 $xft$ 中。令 $x_0 < x_1 < \cdots < x_{k-1}$ 表示存储在 $xft$ 中的元素。每个元素 $x_i$ 关联一个 treap $t_i$，该 treap 存储区间 $x_{i-1}+1,\ldots,x_i$ 中的所有值。如图 13.7 所示。

**图 13.7：** 包含值 0, 1, 3, 4, 6, 8, 9, 10, 11 和 13 的 YFastTrie。

YFastTrie 中的 $\mathrm{find}(x)$ 操作相当简单。我们在 $xft$ 中搜索 $x$，找到与 treap $t_i$ 关联的某个值 $x_i$。然后我们在 $t_i$ 上使用 treap 的 $\mathrm{find}(x)$ 方法来回答查询。整个方法只有一行代码：

```python
def find(self, x):
    return self.xft.find(x).t.find(x)
```

第一个 $\mathrm{find}(x)$ 操作（在 $xft$ 上）需要 $O(\log w)$ 时间。第二个 $\mathrm{find}(x)$ 操作（在 treap 上）需要 $O(\log r)$ 时间，其中 $r$ 是 treap 的大小。在本节后面，我们将证明 treap 的期望大小为 $O(w)$，因此此操作需要 $O(\log w)$ 时间。[^13.1]

向 YFastTrie 添加元素也相当简单——大多数时候。$\mathrm{add}(x)$ 方法调用 $xft.\mathrm{find}(x)$ 来定位应插入 $x$ 的 treap $t$。然后它调用 $t.\mathrm{add}(x)$ 将 $x$ 添加到 $t$。此时，它抛出一枚有偏硬币，正面概率为 $1/w$，反面概率为 $1-1/w$。如果这枚硬币是正面，那么 $x$ 将被添加到 $xft$ 中。

这就是事情变得稍微复杂的地方。当 $x$ 被添加到 $xft$ 时，treap $t$ 需要被分割成两个 treap，$t_1$ 和 $t'$。Treap $t_1$ 包含所有小于或等于 $x$ 的值；$t'$ 是原始 treap $t$ 移除了 $t_1$ 的元素。完成后，我们将键值对 $(x, t_1)$ 添加到 $xft$。图 13.8 展示了一个示例。

```python
def add(self, x):
    ix = int_value(x)
    u = self.xft.find(Rec(ix))
    t = u.t
    if t.add(x):
        n += 1
        if random.randrange(1, w+1) == 1:
            t1 = t.split(x)
            self.xft.add(Rec(ix, t1))
        return True
    return False
```

**图 13.8：** 将值 2 和 6 添加到 YFastTrie。6 的硬币投掷结果是正面，因此 6 被添加到 $xft$ 中，包含 $4,5,6,8,9$ 的 treap 被分割。

将 $x$ 添加到 $t$ 需要 $O(\log w)$ 时间。练习 7.12 表明将 $t$ 分割成 $t_1$ 和 $t'$ 也可以在 $O(\log w)$ 期望时间内完成。将键值对 $(x, t_1)$ 添加到 $xft$ 需要 $O(w)$ 时间，但发生的概率仅为 $1/w$。因此，$\mathrm{add}(x)$ 操作的期望运行时间为

$$O(\log w) + \frac{1}{w}O(w) = O(\log w).$$

$\mathrm{remove}(x)$ 方法撤销 $\mathrm{add}(x)$ 执行的工作。我们使用 $xft$ 找到 $xft$ 中包含 $xft.\mathrm{find}(x)$ 答案的叶子节点 $u$。从 $u$ 中，我们得到包含 $x$ 的 treap $t$，并从 $t$ 中移除 $x$。如果 $x$ 也存储在 $xft$ 中（且 $x$ 不等于 $2^w-1$），则我们从 $xft$ 中移除 $x$，并将 $x$ 的 treap 中的元素添加到 $u$ 在链表中的后继节点所存储的 treap $t_2$ 中。如图 13.9 所示。

```python
def remove(self, x):
    ix = int_value(x)
    u = self.xft.find(Rec(ix))
    ret = u.t.remove(x)
    if ret:
        n -= 1
        if u.x == ix:
            t2 = u.next.t
            t2.merge(u.t)
            self.xft.remove(u)
        return True
    return False
```

**图 13.9：** 从图 13.8 的 YFastTrie 中移除值 1 和 9。

在 $xft$ 中找到节点 $u$ 需要 $O(\log w)$ 期望时间。从 $t$ 中移除 $x$ 需要 $O(\log w)$ 期望时间。同样，练习 7.12 表明将 $t$ 的所有元素合并到 $t_2$ 中可以在 $O(\log w)$ 时间内完成。如有必要，从 $xft$ 中移除 $x$ 需要 $O(w)$ 时间，但 $x$ 仅以概率 $1/w$ 包含在 $xft$ 中。因此，从 YFastTrie 中移除一个元素的期望时间为 $O(\log w)$。

在之前的讨论中，我们将关于此结构中 treap 大小的论证推迟到后面。在结束本章之前，我们证明所需的结果。

**引理 13.1.** *令 $x$ 为存储在 YFastTrie 中的整数，并令 $n_x$ 表示包含 $x$ 的 treap $t$ 中的元素数量。则 $\mathrm{E}[n_x] \le 2w-1$。*

**证明：** 参见图 13.10。令 $x_1 < x_2 < \cdots < x_i = x < x_{i+1} < \cdots < x_n$ 表示存储在 YFastTrie 中的元素。Treap $t$ 包含一些大于或等于 $x$ 的元素，这些元素是 $x_i, x_{i+1}, \ldots, x_{i+j-1}$，其中 $x_{i+j-1}$ 是这些元素中唯一一个在 $\mathrm{add}(x)$ 方法中执行的有偏硬币投掷结果为正面。换句话说，$\mathrm{E}[j]$ 等于获得第一个正面所需的期望有偏硬币投掷次数。[^13.2] 每次投掷是独立的，且以概率 $1/w$ 出现正面，因此 $\mathrm{E}[j] \le w$。（参见引理 4.2 对 $w=2$ 情况的分析。）

类似地，$t$ 中小于 $x$ 的元素是 $x_{i-1},\ldots,x_{i-k}$，其中所有这些 $k$ 次硬币投掷都是反面，而 $x_{i-k-1}$ 的硬币投掷是正面。因此，$\mathrm{E}[k] \le w-1$，因为这与前一段中考虑的相同硬币投掷实验相同，只是最后一次投掷不被计数。总之，$n_x = j + k$，因此

$$\mathrm{E}[n_x] = \mathrm{E}[j+k] = \mathrm{E}[j] + \mathrm{E}[k] \le 2w-1.$$

**图 13.10：** 包含 $x$ 的 treap $t$ 中的元素数量由两次硬币投掷实验决定。

引理 13.1 是以下定理证明中的最后一块，该定理总结了 YFastTrie 的性能：

**定理 13.3.** *YFastTrie 为 $w$ 位整数实现了 SSet 接口。YFastTrie 支持 $\mathrm{add}(x)$、$\mathrm{remove}(x)$ 和 $\mathrm{find}(x)$ 操作，每次操作期望时间为 $O(\log w)$。存储 $n$ 个值的 YFastTrie 使用的空间为 $O(n+w)$。*

空间需求中的 $w$ 项来自于 $xft$ 总是存储值 $2^w-1$。实现可以修改（以在代码中添加一些额外情况为代价），使得不需要存储此值。在这种情况下，定理中的空间需求变为 $O(n)$。

## 13.4 讨论与练习 (Discussion and Exercises)

第一个提供 $O(\log w)$ 时间 $\mathrm{add}(x)$、$\mathrm{remove}(x)$ 和 $\mathrm{find}(x)$ 操作的数据结构由 van Emde Boas 提出，后来被称为 **van Emde Boas 树**（或**分层树**）[72]。原始的 van Emde Boas 结构大小为 $2^w$，使其对于大整数不实用。

XFastTrie 和 YFastTrie 数据结构由 Willard 发现 [75]。XFastTrie 结构与 van Emde Boas 树密切相关；例如，XFastTrie 中的哈希表取代了 van Emde Boas 树中的数组。也就是说，van Emde Boas 树不是存储哈希表 $t[i]$，而是存储一个长度为 $2^i$ 的数组。

另一种用于存储整数的结构是 Fredman 和 Willard 的融合树（fusion trees）[32]。这种结构可以在 $O(n)$ 空间中存储 $n$ 个 $w$ 位整数，使得 $\mathrm{find}(x)$ 操作在 $O((\log n)/(\log w))$ 时间内运行。通过在 $\log w > \sqrt{\log n}$ 时使用融合树，在 $\log w \le \sqrt{\log n}$ 时使用 YFastTrie，我们得到一个 $O(n)$ 空间的数据结构，可以在 $O(\sqrt{\log n})$ 时间内实现 $\mathrm{find}(x)$ 操作。最近 Pătraşcu 和 Thorup [57] 的下界结果表明，这些结果或多或少是最优的，至少对于仅使用 $O(n)$ 空间的结构而言。

**练习 13.1.** 设计并实现 BinaryTrie 的简化版本，该版本没有链表或 jump 指针，但 $\mathrm{find}(x)$ 仍然在 $O(w)$ 时间内运行。

**练习 13.2.** 设计并实现 XFastTrie 的简化实现，该实现根本不使用二进制 trie。相反，你的实现应该将所有内容存储在一个双向链表和 $w+1$ 个哈希表中。

**练习 13.3.** 我们可以将 BinaryTrie 视为一种存储长度为 $w$ 的位串的结构，每个位串表示为一条从根到叶子的路径。将此思想扩展为一个 SSet 实现，该实现存储可变长度字符串，并实现 $\mathrm{add}(s)$、$\mathrm{remove}(s)$ 和 $\mathrm{find}(s)$，时间与 $s$ 的长度成正比。

提示：数据结构中的每个节点应存储一个由字符值索引的哈希表。

**练习 13.4.** 对于整数 $x \in \{0,\ldots,2^w-1\}$，令 $d(x)$ 表示 $x$ 与 $\mathrm{find}(x)$ 返回的值之间的差值（如果 $\mathrm{find}(x)$ 返回 $\mathrm{nil}$，则将 $d(x)$ 定义为 $2^w$）。例如，如果 $\mathrm{find}(23)$ 返回 43，则 $d(23)=20$。

1. 设计并实现 XFastTrie 中 $\mathrm{find}(x)$ 操作的修改版本，该版本在 $O(1+\log d(x))$ 期望时间内运行。提示：哈希表 $t[w]$ 包含所有 $d(x)=0$ 的值 $x$，因此这将是一个好的起点。
2. 设计并实现 XFastTrie 中 $\mathrm{find}(x)$ 操作的修改版本，该版本在 $O(1+\log\log d(x))$ 期望时间内运行。

---

[^13.1]: 这是**詹森不等式**（Jensen's Inequality）的一个应用：如果 $\mathrm{E}[r]=w$，则 $\mathrm{E}[\log r] \le \log w$。
[^13.2]: 此分析忽略了 $j$ 永远不会超过 $n-i+1$ 的事实。然而，这只会降低 $\mathrm{E}[j]$，因此上界仍然成立。
