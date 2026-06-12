# 第8章 替罪羊树

<FONT size=2>译者：陈志斌 &lt;alittlezhou@gmail.com&gt;</FONT>

在本章中，我们研究一种二叉搜索树数据结构——ScapegoatTree（替罪羊树）。这种结构基于一个常见的智慧：当出现问题的时候，人们首先倾向于找一个替罪羊（scapegoat）。一旦明确找到了替罪羊，我们就可以让它去解决问题。

ScapegoatTree 通过**部分重建操作**（partial rebuilding operations）来保持平衡。在部分重建操作中，整个子树被拆解并重建为一棵完美平衡的子树。有许多方法可以将以节点 $u$ 为根的子树重建为一棵完美平衡的树。最简单的方法之一是遍历 $u$ 的子树，将其所有节点收集到一个数组 $a$ 中，然后递归地使用 $a$ 构建一棵平衡子树。令 $m = \mathrm{length}(a)/2$，则元素 $a[m]$ 成为新子树的根，$a[0],\ldots,a[m-1]$ 递归地存储在左子树中，$a[m+1],\ldots,a[\mathrm{length}(a)-1]$ 递归地存储在右子树中。

```python
def rebuild(u):
    a = new_array(size(u))
    pack_into_array(u, a, 0)
    return build_balanced(a, 0, size(u))

def pack_into_array(u, a, i):
    if u == nil:
        return i
    i = pack_into_array(u.left, a, i)
    a[i] = u
    i += 1
    return pack_into_array(u.right, a, i)

def build_balanced(a, i, ns):
    if ns == 0:
        return nil
    m = ns // 2
    a[i+m].left = build_balanced(a, i, m)
    if a[i+m].left != nil:
        a[i+m].left.parent = a[i+m]
    a[i+m].right = build_balanced(a, i+m+1, ns-m-1)
    if a[i+m].right != nil:
        a[i+m].right.parent = a[i+m]
    return a[i+m]
```

调用 $\mathrm{rebuild}(u)$ 需要 $O(\mathrm{size}(u))$ 时间。得到的子树具有最小高度；不存在具有 $\mathrm{size}(u)$ 个节点且高度更小的树。

---

## 8.1 ScapegoatTree：一种带部分重建的二叉搜索树

ScapegoatTree 是一种 BinarySearchTree（二叉搜索树），它除了跟踪树中节点的数量 $n$ 之外，还维护一个计数器 $q$，用于保持节点数量的上界。

```python
def _initialize(self):
    self.n = 0
    self.q = 0
```

在任何时候，$n$ 和 $q$ 都满足以下不等式：

$$q/2 \le n \le q$$

此外，ScapegoatTree 具有对数高度（logarithmic height）；在任何时候，替罪羊树的高度不超过：

$$\log_{3/2} q \le \log_{3/2} 2n < \log_{3/2} n + 2 \tag{8.1}$$

即使有这一约束，ScapegoatTree 看起来仍然可能出奇地不平衡。图 8.1 中的树有 $q=n=10$ 和高度 $5<\log_{3/2}10 \approx 5.679$。

**图 8.1：** 具有 10 个节点和高度 5 的 ScapegoatTree。

![ScapegoatTree with 10 nodes and height 5](img/scapegoat-insert-1.png)

在 ScapegoatTree 中实现 $\mathrm{find}(x)$ 操作使用标准的 BinarySearchTree 搜索算法（见第 6.2 节）。这需要与树高度成正比的时间，由公式 (8.1) 可知为 $O(\log n)$。

为了实现 $\mathrm{add}(x)$ 操作，我们首先递增 $n$ 和 $q$，然后使用通常的算法将 $x$ 添加到二叉搜索树中；我们搜索 $x$，然后添加一个值为 $x$ 的新叶节点 $u$。此时，我们可能会很幸运，$u$ 的深度不超过 $\log_{3/2} q$。如果是这样，我们就不做任何其他事情。

不幸的是，有时会发生 $\mathrm{depth}(u) > \log_{3/2} q$ 的情况。在这种情况下，我们需要降低高度。这并不算大任务；只有一个节点，即 $u$，其深度超过 $\log_{3/2} q$。为了修复 $u$，我们从 $u$ 开始向根回溯，寻找一个**替罪羊**（scapegoat）$w$。替罪羊 $w$ 是一个非常不平衡的节点。它具有以下性质：

$$\frac{\mathrm{size}(w.\mathrm{child})}{\mathrm{size}(w)} > \frac{2}{3} \tag{8.2}$$

其中 $w.\mathrm{child}$ 是从根到 $u$ 路径上 $w$ 的子节点。我们很快就会证明替罪羊一定存在。现在我们姑且接受这一点。一旦找到了替罪羊 $w$，我们彻底摧毁以 $w$ 为根的子树，并将其重建为一棵完美平衡的二叉搜索树。从 (8.2) 可知，即使在添加 $u$ 之前，$w$ 的子树也不是一棵完全二叉树。因此，当我们重建 $w$ 时，高度至少减少 1，使得 ScapegoatTree 的高度再次不超过 $\log_{3/2} q$。

```python
def add(self, x):
    (u, d) = self.add_with_depth(x)
    if d > log32(self.q):
        # 深度超限，寻找替罪羊
        w = u.parent
        while 3*self._size(w) <= 2*self._size(w.parent):
            w = w.parent
        self.rebuild(w.parent)
    return d >= 0
```

**图 8.2：** 向 ScapegoatTree 插入 3.5 使其高度增加到 6，这违反了 (8.1)，因为 $6 > \log_{3/2} 11 \approx 5.914$。在包含 5 的节点处找到了一个替罪羊。

![Inserting 3.5 into a ScapegoatTree](img/scapegoat-insert-3.png) ![Scapegoat found at node containing 5](img/scapegoat-insert-4.png)

如果忽略寻找替罪羊 $w$ 和重建以 $w$ 为根的子树的开销，那么 $\mathrm{add}(x)$ 的运行时间主要由初始搜索决定，需要 $O(\log q) = O(\log n)$ 时间。我们将在下一节中使用均摊分析（amortized analysis）来核算寻找替罪羊和重建的开销。

ScapegoatTree 中 $\mathrm{remove}(x)$ 的实现非常简单。我们搜索 $x$ 并使用通常的算法从 BinarySearchTree 中删除一个节点。（注意，这永远不会增加树的高度。）接下来，我们递减 $n$，但保持 $q$ 不变。最后，我们检查 $q > 2n$，如果是，则**重建整棵树**为一棵完美平衡的二叉搜索树，并设置 $q = n$。

```python
def remove(self, x):
    if super().remove(x):
        if 2*self.n < self.q:
            self.rebuild(self.r)
            self.q = self.n
        return True
    return False
```

同样，如果忽略重建的开销，$\mathrm{remove}(x)$ 操作的运行时间与树的高度成正比，因此为 $O(\log n)$。

---

### 8.1.1 正确性和运行时间分析

在本节中，我们分析 ScapegoatTree 操作的正确性和均摊运行时间（amortized running time）。我们首先证明正确性，表明当 $\mathrm{add}(x)$ 操作导致一个节点违反条件 (8.1) 时，我们总能找到一个替罪羊：

**引理 8.1.** 令 $u$ 是 ScapegoatTree 中深度 $h > \log_{3/2} q$ 的一个节点。那么存在从 $u$ 到根的路径上的一个节点 $w$，使得
$$\frac{\mathrm{size}(w)}{\mathrm{size}(\mathrm{parent}(w))} > 2/3$$

**证明：** 为得出矛盾，假设情况并非如此，且对所有从 $u$ 到根路径上的节点 $w$ 都有：
$$\frac{\mathrm{size}(w)}{\mathrm{size}(\mathrm{parent}(w))} \le 2/3$$

将从根到 $u$ 的路径记为 $r=u_0,\ldots,u_h=u$。那么，我们有 $\mathrm{size}(u_0)=n$，$\mathrm{size}(u_1) \le \frac{2}{3}n$，$\mathrm{size}(u_2) \le \frac{4}{9}n$，更一般地：
$$\mathrm{size}(u_i) \le \left(\frac{2}{3}\right)^i n$$

但这会产生矛盾，因为 $\mathrm{size}(u) \ge 1$，因此
$$1 \le \mathrm{size}(u) \le \left(\frac{2}{3}\right)^h n < \left(\frac{2}{3}\right)^{\log_{3/2} q} n \le \left(\frac{2}{3}\right)^{\log_{3/2} n} n = \left(\frac{1}{n}\right)n = 1$$

$\square$

接下来，我们分析尚未核算的运行时间部分。有两部分：在搜索替罪羊节点时调用 $\mathrm{size}(u)$ 的开销，以及在找到替罪羊 $w$ 后调用 $\mathrm{rebuild}(w)$ 的开销。调用 $\mathrm{size}(u)$ 的开销可以与调用 $\mathrm{rebuild}(w)$ 的开销相关联，如下所示：

**引理 8.2.** 在 ScapegoatTree 中调用 $\mathrm{add}(x)$ 期间，寻找替罪羊 $w$ 和重建以 $w$ 为根的子树的代价为 $O(\mathrm{size}(w))$。

**证明：** 找到替罪羊后重建节点 $w$ 的代价是 $O(\mathrm{size}(w))$。在搜索替罪羊节点时，我们在一系列节点 $u_0,\ldots,u_k$ 上调用 $\mathrm{size}(u)$，直到找到替罪羊 $u_k = w$。但是，由于 $u_k$ 是该序列中第一个是替罪羊的节点，我们知道对所有 $i \in \{0,\ldots,k-2\}$ 有：
$$\mathrm{size}(u_i) < \frac{2}{3}\mathrm{size}(u_{i+1})$$

因此，所有 $\mathrm{size}(u)$ 调用的总开销为：

$$
\begin{aligned}
O\left( \sum_{i=0}^k \mathrm{size}(u_{k-i}) \right)
&= O\left( \mathrm{size}(u_k) + \sum_{i=0}^{k-1} \mathrm{size}(u_{k-i-1}) \right) \\
&= O\left( \mathrm{size}(u_k) + \sum_{i=0}^{k-1} \left(\frac{2}{3}\right)^i \mathrm{size}(u_k) \right) \\
&= O\left( \mathrm{size}(u_k)\left(1 + \sum_{i=0}^{k-1} \left(\frac{2}{3}\right)^i \right) \right) \\
&= O(\mathrm{size}(u_k)) = O(\mathrm{size}(w))
\end{aligned}
$$

其中最后一行利用了该级数是几何递减级数这一事实。$\square$

剩下要证明的是在一个 $m$ 次操作的序列中，所有 $\mathrm{rebuild}(u)$ 调用的总代价的上界：

**引理 8.3.** 从空的 ScapegoatTree 开始，任意 $m$ 次 $\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 操作的序列导致 $\mathrm{rebuild}(u)$ 操作最多使用 $O(m\log m)$ 时间。

**证明：** 为证明这一点，我们将使用一个**信用方案**（credit scheme）。我们设想每个节点存储一定数量的信用点（credits）。每个信用点可以支付重建所花费的某个常量 $c$ 单位时间。该方案总共发放 $O(m\log m)$ 个信用点，并且每次调用 $\mathrm{rebuild}(u)$ 都使用存储在 $u$ 处的信用点支付。

在插入或删除操作期间，我们给到被插入或被删除节点 $u$ 路径上的每个节点一个信用点。这样，每次操作最多发放 $\log_{3/2} q \le \log_{3/2} m$ 个信用点。在删除操作期间，我们还会额外存储一个信用点"在边上"。因此，我们总共最多发放 $O(m\log m)$ 个信用点。剩下的就是证明这些信用点足以支付所有 $\mathrm{rebuild}(u)$ 的调用。

如果在插入期间调用 $\mathrm{rebuild}(u)$，那是因为 $u$ 是一个替罪羊。不失一般性，假设：
$$\frac{\mathrm{size}(u.\mathrm{left})}{\mathrm{size}(u)} > \frac{2}{3}$$

利用 $\mathrm{size}(u) = 1 + \mathrm{size}(u.\mathrm{left}) + \mathrm{size}(u.\mathrm{right})$ 这一事实，我们推导出：
$$\frac{1}{2}\mathrm{size}(u.\mathrm{left}) > \mathrm{size}(u.\mathrm{right})$$

因此：
$$\mathrm{size}(u.\mathrm{left}) - \mathrm{size}(u.\mathrm{right}) > \frac{1}{2}\mathrm{size}(u.\mathrm{left}) > \frac{1}{3}\mathrm{size}(u)$$

现在，上次重建包含 $u$ 的子树时（或者如果包含 $u$ 的子树从未被重建过，则在 $u$ 被插入时），我们有：
$$\mathrm{size}(u.\mathrm{left}) - \mathrm{size}(u.\mathrm{right}) \le 1$$

因此，自那时以来影响 $u.\mathrm{left}$ 或 $u.\mathrm{right}$ 的 $\mathrm{add}(x)$ 或 $\mathrm{remove}(x)$ 操作的数量至少为：
$$\frac{1}{3}\mathrm{size}(u) - 1$$

因此，存储在 $u$ 处至少有这么多信用点可用于支付调用 $\mathrm{rebuild}(u)$ 所需的 $O(\mathrm{size}(u))$ 时间。

如果在删除期间调用 $\mathrm{rebuild}(u)$，那是因为 $q > 2n$。在这种情况下，我们有 $q - n > n$ 个信用点存储"在边上"，我们用这些来支付重建根所需的 $O(n)$ 时间。证明完毕。$\square$

---

### 8.1.2 总结

以下定理总结了 ScapegoatTree 数据结构的性能：

**定理 8.1.** ScapegoatTree 实现了 SSet 接口。忽略 $\mathrm{rebuild}(u)$ 操作的开销，ScapegoatTree 支持 $\mathrm{add}(x)$、$\mathrm{remove}(x)$ 和 $\mathrm{find}(x)$ 操作，每次操作时间为 $O(\log n)$。

此外，从空 ScapegoatTree 开始，任意 $m$ 次 $\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 操作的序列，在所有 $\mathrm{rebuild}(u)$ 调用上花费的总时间为 $O(m\log m)$。

---

## 8.2 讨论与练习

**替罪羊树**（scapegoat tree）这个术语源于 Galperin 和 Rivest [33]，他们定义并分析了这些树。然而，相同的结构更早之前由 Andersson [5, 7] 发现，他称之为**通用平衡树**（general balanced trees），因为它们可以具有任何形状，只要高度较小。

对 ScapegoatTree 实现进行实验会发现，它通常比本书中的其他 SSet 实现慢得多。这可能有点令人惊讶，因为高度界：
$$\log_{3/2} q \approx 1.709\log n + O(1)$$

比 Skiplist 中搜索路径的期望长度更好，并且与 Treap 相差不多。该实现可以通过在每个节点显式存储子树大小或重用已计算的子树大小来优化（练习 8.5 和 8.6）。即使有了这些优化，总存在一些 $\mathrm{add}(x)$ 和 $\mathrm{delete}(x)$ 操作的序列，使 ScapegoatTree 比其他 SSet 实现花费更长时间。

这种性能差距源于以下事实：与本书中讨论的其他 SSet 实现不同，ScapegoatTree 会花费大量时间进行自我重建。练习 8.3 要求你证明存在 $n$ 次操作的序列，使得 ScapegoatTree 在 $\mathrm{rebuild}(u)$ 调用中花费 $\Omega(n\log n)$ 时间。这与本书中讨论的其他 SSet 实现形成对比，后者在 $n$ 次操作序列中只进行 $O(n)$ 次结构变化。不幸的是，这是 ScapegoatTree 通过调用 $\mathrm{rebuild}(u)$ 来完成所有重建的必然结果 [20]。

尽管性能不佳，但在某些应用中 ScapegoatTree 可能是正确的选择。当节点有附加数据，而这些数据在旋转（rotation）时无法在常数时间内更新，但可以在 $\mathrm{rebuild}(u)$ 操作期间更新时，就会出现这种情况。在这种情况下，ScapegoatTree 和基于部分重建的相关结构可能适用。练习 8.11 概述了此类应用的一个例子。

---

**练习 8.1.** 说明在图 8.1 的 ScapegoatTree 上依次添加值 1.5 和 1.6 的过程。

**练习 8.2.** 说明当序列 $1,5,2,4,3$ 添加到空 ScapegoatTree 时会发生什么，并说明引理 8.3 证明中描述的信用点流向何处，以及它们如何在此添加序列期间被使用。

**练习 8.3.** 证明，如果从空 ScapegoatTree 开始，对 $x=1,2,3,\ldots,n$ 依次调用 $\mathrm{add}(x)$，那么在 $\mathrm{rebuild}(u)$ 调用中花费的总时间至少为 $c n\log n$，其中 $c>0$ 为某个常数。

**练习 8.4.** 本章描述的 ScapegoatTree 保证搜索路径长度不超过 $\log_{3/2} q$。

1. 设计、分析并实现 ScapegoatTree 的一个修改版本，其中搜索路径长度不超过 $\log_b q$，其中 $b$ 是满足 $1<b<2$ 的参数。
2. 你的分析和/或实验对 $\mathrm{find}(x)$、$\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 关于 $n$ 和 $b$ 的均摊代价有何结论？

**练习 8.5.** 修改 ScapegoatTree 的 $\mathrm{add}(x)$ 方法，使其不会浪费任何时间重新计算已经计算过的子树大小。这是可行的，因为当方法想要计算 $\mathrm{size}(w)$ 时，它已经计算了 $\mathrm{size}(w.\mathrm{left})$ 或 $\mathrm{size}(w.\mathrm{right})$ 中的一个。将修改后的实现与这里给出的实现进行性能比较。

**练习 8.6.** 实现 ScapegoatTree 数据结构的第二个版本，明确存储和维护每个节点子树的大小。将所得实现的性能与原始 ScapegoatTree 实现以及练习 8.5 中的实现进行比较。

**练习 8.7.** 重新实现本章开头讨论的 $\mathrm{rebuild}(u)$ 方法，使其不需要使用数组来存储正在重建的子树的节点。相反，它应该使用递归先将节点连接成一个链表，然后将此链表转换为完美平衡的二叉树。（这两个步骤都有非常优雅的递归实现。）

**练习 8.8.** 分析并实现 WeightBalancedTree。这是一种树，其中除根节点外的每个节点 $u$ 都维护**平衡不变性**（balance invariant）：$\mathrm{size}(u) \le (2/3)\mathrm{size}(u.\mathrm{parent})$。$\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 操作与标准的 BinarySearchTree 操作相同，只是在节点 $u$ 处违反平衡不变性时，重建以 $u.\mathrm{parent}$ 为根的子树。你的分析应表明 WeightBalancedTree 上的操作在均摊时间 $O(\log n)$ 内运行。

**练习 8.9.** 分析并实现 CountdownTree。在 CountdownTree 中，每个节点 $u$ 保留一个**计时器**（timer）$u.t$。$\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 操作与标准 BinarySearchTree 完全相同，只是每当这些操作影响 $u$ 的子树时，$u.t$ 就递减。当 $u.t=0$ 时，以 $u$ 为根的整个子树被重建为一棵完美平衡的二叉搜索树。当节点 $u$ 参与重建操作时（无论是 $u$ 被重建还是 $u$ 的某个祖先被重建），$u.t$ 被重置为 $\mathrm{size}(u)/3$。

你的分析应表明 CountdownTree 上的操作在均摊时间 $O(\log n)$ 内运行。（提示：首先证明每个节点 $u$ 满足某个版本的平衡不变性。）

**练习 8.10.** 分析并实现 DynamiteTree。在 DynamiteTree 中，每个节点 $u$ 在变量 $u.\mathrm{size}$ 中跟踪以 $u$ 为根的子树的大小。$\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 操作与标准 BinarySearchTree 完全相同，只是每当这些操作影响节点 $u$ 的子树时，$u$ 以概率 $1/u.\mathrm{size}$ **爆炸**（explodes）。当 $u$ 爆炸时，其整个子树被重建为一棵完美平衡的二叉搜索树。

你的分析应表明 DynamiteTree 上的操作在期望时间 $O(\log n)$ 内运行。

**练习 8.11.** 设计并实现一个 Sequence 数据结构，用于维护一个序列（列表）的元素。它支持以下操作：

- $\mathrm{add\_after}(e)$：在序列中元素 $e$ 之后添加一个新元素。返回新添加的元素。（如果 $e$ 为 null，则在序列开头添加新元素。）
- $\mathrm{remove}(e)$：从序列中删除 $e$。
- $\mathrm{test\_before}(e_1, e_2)$：当且仅当 $e_1$ 在序列中出现在 $e_2$ 之前时返回 true。

前两个操作应在均摊时间 $O(\log n)$ 内运行。第三个操作应在常数时间内运行。

Sequence 数据结构可以通过将元素按它们在序列中出现的顺序存储在类似 ScapegoatTree 的结构中来实现。为了在常数时间内实现 $\mathrm{test\_before}(e_1, e_2)$，每个元素 $e$ 被标记一个整数，该整数编码从根到 $e$ 的路径。这样，$\mathrm{test\_before}(e_1, e_2)$ 可以通过比较 $e_1$ 和 $e_2$ 的标记来实现。
