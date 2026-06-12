# 第7章 随机二叉搜索树

在本章中，我们介绍一种使用随机化（randomization）的二叉搜索树结构，其所有操作均能在期望时间 $O(\log n)$ 内完成。

---

## 7.1 随机二叉搜索树（Random Binary Search Trees）

考虑图 7.1 中所示的两棵二叉搜索树，每棵都有 $n=15$ 个节点。左边的是一棵链表，右边是完美平衡的二叉搜索树。左边的高度为 $n-1=14$，右边的高度为 3。

<div align="center">
**图 7.1:** 包含整数 $0,\ldots,14$ 的两棵二叉搜索树。
</div>

想象一下这两棵树是如何构造出来的。左边的那棵出现在我们从一个空的 BinarySearchTree 开始，并按以下序列添加：
$$
\langle 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14 \rangle .
$$
没有其他添加序列能创建这棵树（你可以通过对 $n$ 进行归纳证明）。另一方面，右边的树可以通过以下序列创建：
$$
\langle 7,3,11,1,5,9,13,0,2,4,6,8,10,12,14 \rangle .
$$
其他序列也能实现，包括：
$$
\langle 7,3,1,5,0,2,4,6,11,9,13,8,10,12,14 \rangle ,
$$
以及
$$
\langle 7,3,1,11,5,0,2,4,6,9,13,8,10,12,14 \rangle .
$$
事实上，有 $21,964,800$ 种添加序列能生成右边的树，而只有一种能生成左边的树。

上述例子给出了一些经验性证据：如果我们选择一个 $0,\ldots,14$ 的随机排列（random permutation）并将其添加到二叉搜索树中，那么我们更有可能得到一棵非常平衡的树（图 7.1 右侧），而不是一棵非常不平衡的树（图 7.1 左侧）。

我们可以通过研究随机二叉搜索树（random binary search tree）来形式化这一概念。一棵大小为 $n$ 的随机二叉搜索树按以下方式获得：取整数 $0,\ldots,n-1$ 的一个随机排列 $x_0,\ldots,x_{n-1}$，并将其元素逐个添加到 BinarySearchTree 中。所谓随机排列，是指所有 $n!$ 种可能的排列（顺序）都是等可能的，因此得到任何特定排列的概率为 $1/n!$。

注意，值 $0,\ldots,n-1$ 可以被任何大小为 $n$ 的有序集合替换，而不会改变随机二叉搜索树的任何性质。元素 $x\in\{0,\ldots,n-1\}$ 只是代表大小为 $n$ 的有序集合中排在第 $x$ 位的元素。

在给出关于随机二叉搜索树的主要结论之前，我们必须先花点时间讨论一种在研究随机化结构时常出现的数。对于非负整数 $k$，第 $k$ 个调和数（harmonic number），记作 $H_k$，定义为
$$
H_k = 1 + 1/2 + 1/3 + \cdots + 1/k .
$$
调和数 $H_k$ 没有简单的封闭形式，但它与 $k$ 的自然对数密切相关。特别地，
$$
\ln k < H_k \le \ln k + 1 .
$$
学过微积分的读者可能会注意到，这是因为积分 $\int_1^k (1/x)\, \mathrm{d}x = \ln k$。考虑到积分可以解释为曲线与 $x$ 轴之间的面积，$H_k$ 的值可以被积分 $\int_1^k (1/x)\, \mathrm{d}x$ 下界限定，并被 $1+\int_1^k (1/x)\, \mathrm{d}x$ 上界限定。（参见图 7.2 的图形解释。）

<div align="center">
**图 7.2:** 第 $k$ 个调和数 $H_k=\sum_{i=1}^k 1/i$ 被两个积分上下界限定。这些积分的值由阴影区域的面积给出，而 $H_k$ 的值由矩形的面积给出。
</div>

**引理 7.1.** *在大小为 $n$ 的随机二叉搜索树中，以下结论成立：*

1. 对于任意 $x\in\{0,\ldots,n-1\}$，查找 $x$ 的搜索路径（search path）的期望长度为 $H_{x+1} + H_{n-x} - O(1)$。$^1$
2. 对于任意 $x\in(-1,n)\setminus\{0,\ldots,n-1\}$，查找 $x$ 的搜索路径的期望长度为 $H_{\lceil x\rceil} + H_{n-\lceil x\rceil}$。

> $^1$ 表达式 $x+1$ 和 $n-x$ 可以分别解释为树中小于或等于 $x$ 的元素个数，以及树中大于或等于 $x$ 的元素个数。

我们将在下一节证明引理 7.1。现在，想想引理 7.1 的两个部分告诉我们什么。第一部分告诉我们，如果在一棵大小为 $n$ 的树中查找一个元素，搜索路径的期望长度最多为 $2\ln n + O(1)$。第二部分告诉我们，在查找一个不在树中的值时也是同样的结论。当我们比较引理的两个部分时，我们发现查找树中已有的元素只比查找不存在的元素稍微快一点。

### 7.1.1 引理 7.1 的证明

证明引理 7.1 所需的关键观察如下：在随机二叉搜索树 $T$ 中，对于开区间 $(-1,n)$ 内的值 $x$，其查找路径包含键值为 $i < x$ 的节点当且仅当，在用于创建 $T$ 的随机排列中，$i$ 出现在 $\{i+1,i+2,\ldots,\lfloor x\rfloor\}$ 中的任何元素之前。

要理解这一点，请参考图 7.3，注意在 $\{i,i+1,\ldots,\lfloor x\rfloor\}$ 中的某个值被添加之前，开区间 $(i-1,\lfloor x\rfloor+1)$ 内每个值的查找路径都是相同的。（记住，要使两个值具有不同的查找路径，树中必须有某个元素对它们的比较结果不同。）设 $j$ 为 $\{i,i+1,\ldots,\lfloor x\rfloor\}$ 中第一个出现在随机排列中的元素。注意 $j$ 现在（并且将始终）位于 $x$ 的查找路径上。如果 $j\neq i$，那么包含 $j$ 的节点 $u_j$ 在包含 $i$ 的节点 $u_i$ 之前创建。之后，当 $i$ 被添加时，它将被添加到以 $u_j.left$ 为根的子树中，因为 $i<j$。另一方面，$x$ 的查找路径将永远不会访问这个子树，因为在访问 $u_j$ 之后它会继续前往 $u_j.right$。

<div align="center">
**图 7.3:** 值 $i<x$ 在 $x$ 的查找路径上当且仅当 $i$ 是 $\{i,i+1,\ldots,\lfloor x\rfloor\}$ 中第一个被添加到树中的元素。
</div>

类似地，对于 $i>x$，$i$ 出现在 $x$ 的查找路径上当且仅当，在用于创建 $T$ 的随机排列中，$i$ 出现在 $\{\lceil x\rceil,\lceil x\rceil+1,\ldots,i-1\}$ 中的任何元素之前。

注意，如果我们从一个 $\{0,\ldots,n\}$ 的随机排列开始，那么只包含 $\{i,i+1,\ldots,\lfloor x\rfloor\}$ 和 $\{\lceil x\rceil,\lceil x\rceil+1,\ldots,i-1\}$ 的子序列也是各自元素的随机排列。因此，子集 $\{i,i+1,\ldots,\lfloor x\rfloor\}$ 和 $\{\lceil x\rceil,\lceil x\rceil+1,\ldots,i-1\}$ 中的每个元素在用于创建 $T$ 的随机排列中，出现在其子集中任何其他元素之前的概率是相等的。所以我们有
$$
\Pr\{\text{$i$ 在 $x$ 的查找路径上}\}
= \left\{ \begin{array}{ll}
1/(\lfloor x\rfloor-i+1) & \text{若 $i < x$} \\
1/(i-\lceil x\rceil+1) & \text{若 $i > x$}
\end{array}\right .
$$

有了这个观察，引理 7.1 的证明就涉及一些简单的调和数计算：

*证明.* [引理 7.1 的证明] 设 $I_i$ 为指示随机变量（indicator random variable），当 $i$ 出现在 $x$ 的查找路径上时取值为 1，否则为 0。那么查找路径的长度由下式给出：
$$
\sum_{i\in\{0,\ldots,n-1\}\setminus\{x\}} I_i
$$
因此，如果 $x\in\{0,\ldots,n-1\}$，查找路径的期望长度为（见图 7.4.a）：
$$
\begin{aligned}
\mathrm{E}\left[\sum_{i=0}^{x-1} I_i + \sum_{i=x+1}^{n-1} I_i\right]
&= \sum_{i=0}^{x-1} \mathrm{E}\left[I_i\right] + \sum_{i=x+1}^{n-1} \mathrm{E}\left[I_i\right] \\
&= \sum_{i=0}^{x-1} 1/(\lfloor x\rfloor-i+1) + \sum_{i=x+1}^{n-1} 1/(i-\lceil x\rceil+1) \\
&= \sum_{i=0}^{x-1} 1/(x-i+1) + \sum_{i=x+1}^{n-1} 1/(i-x+1) \\
&= \frac{1}{2}+\frac{1}{3}+\cdots+\frac{1}{x+1} \\
&\quad {} + \frac{1}{2}+\frac{1}{3}+\cdots+\frac{1}{n-x} \\
&= H_{x+1} + H_{n-x} - 2 .
\end{aligned}
$$
对于搜索值 $x\in(-1,n)\setminus\{0,\ldots,n-1\}$ 的相应计算几乎完全相同（见图 7.4.b）。 $\square$

<div align="center">
**图 7.4:** 元素在 $x$ 的查找路径上的概率，当 (a) $x$ 为整数时和 (b) $x$ 不为整数时。
</div>

### 7.1.2 小结

以下定理总结了随机二叉搜索树的性能：

**定理 7.1.** *一棵随机二叉搜索树可以在 $O(n\log n)$ 时间内构造。在随机二叉搜索树中，$\mathrm{find}(x)$ 操作的期望时间为 $O(\log n)$。*

我们应该再次强调，定理 7.1 中的期望是相对于用于创建随机二叉搜索树的随机排列而言的。特别地，它并不依赖于 $x$ 的随机选择；对于每个 $x$ 的值都成立。

---

## 7.2 Treap：一种随机化的二叉搜索树（A Randomized Binary Search Tree）

随机二叉搜索树的问题在于它们不是动态的。它们不支持实现 SSet 接口所需的 $\mathrm{add}(x)$ 或 $\mathrm{remove}(x)$ 操作。在本节中，我们描述一种称为 Treap 的数据结构，它利用引理 7.1 来实现 SSet 接口。$^2$

> $^2$ Treap 这个名称源于该数据结构同时是一棵二叉搜索**树**（**tr**ee）和一个**堆**（**h**eap）。

Treap 中的节点类似于 BinarySearchTree 中的节点，它有一个数据值 $x$，但同时包含一个唯一的数值型优先级（priority）$p$，该优先级是随机分配的：

除了是一棵二叉搜索树，Treap 中的节点还遵循**堆性质**（heap property）：

- （堆性质）在每个非根节点 $u$ 处，$u.parent.p < u.p$。

换句话说，每个节点的优先级都小于其两个子节点的优先级。图 7.5 显示了一个示例。

<div align="center">
**图 7.5:** 一个 Treap 示例，包含整数 $0,\ldots,9$。每个节点 $u$ 以包含 $u.x, u.p$ 的方框表示。
</div>

堆性质和二叉搜索树条件共同确保，一旦每个节点的键（$x$）和优先级（$p$）被定义，Treap 的形状就完全确定了。堆性质告诉我们，优先级最小的节点必须是 Treap 的根 $r$。二叉搜索树性质告诉我们，所有键小于 $r.x$ 的节点存储在 $r.left$ 为根的子树中，所有键大于 $r.x$ 的节点存储在 $r.right$ 为根的子树中。

关于 Treap 中优先级值的重要之处在于它们是唯一的并且是随机分配的。正因为如此，我们可以用两种等价的方式来看待 Treap。如上定义，Treap 遵循堆性质和二叉搜索树性质。或者，我们可以将 Treap 视为一棵 BinarySearchTree，其节点按优先级递增的顺序添加。例如，图 7.5 中的 Treap 可以通过将以下 $(x,p)$ 值序列添加到 BinarySearchTree 中获得：
$$
\langle (3,1), (1,6), (0,9), (5,11), (4,14), (9,17), (7,22), (6,42), (8,49), (2,99) \rangle
$$

由于优先级是随机选择的，这等价于取一个键的随机排列——在这个例子中排列是
$$
\langle 3, 1, 0, 5, 9, 4, 7, 6, 8, 2 \rangle
$$
——并将它们添加到 BinarySearchTree 中。但这意味着 treap 的形状与随机二叉搜索树完全相同。特别地，如果我们将每个键 $x$ 替换为其秩（rank）$^3$，那么引理 7.1 也适用。将引理 7.1 用 Treap 的术语重述，我们得到：

> $^3$ 元素 $x$ 在集合 $S$ 中的秩是指 $S$ 中小于 $x$ 的元素个数。

**引理 7.2.** *在存储了 $n$ 个键的集合 $S$ 的 Treap 中，以下结论成立：*

1. 对于任意 $x\in S$，查找 $x$ 的搜索路径的期望长度为 $H_{r(x)+1} + H_{n-r(x)} - O(1)$。
2. 对于任意 $x\not\in S$，查找 $x$ 的搜索路径的期望长度为 $H_{r(x)} + H_{n-r(x)}$。

*这里，$r(x)$ 表示 $x$ 在集合 $S\cup\{x\}$ 中的秩。*

同样，我们强调引理 7.2 中的期望是针对每个节点优先级的随机选择而言的。它不需要对键的随机性做任何假设。

引理 7.2 告诉我们 Treap 可以高效地实现 $\mathrm{find}(x)$ 操作。然而，Treap 的真正好处在于它可以支持 $\mathrm{add}(x)$ 和 $\mathrm{delete}(x)$ 操作。为此，它需要执行旋转（rotation）以维护堆性质。参考图 7.6。二叉搜索树中的旋转是一种局部修改，它接受节点 $w$ 的父节点 $u$，并使 $w$ 成为 $u$ 的父节点，同时保持二叉搜索树性质。旋转有两种类型：**左旋**（left rotation）和**右旋**（right rotation），取决于 $w$ 是 $u$ 的右子节点还是左子节点。

<div align="center">
**图 7.6:** 二叉搜索树中的左旋和右旋。
</div>

实现这一操作的代码需要处理这两种可能性，并注意边界情况（当 $u$ 是根时），所以实际代码比图 7.6 所示的要稍长一些：

```python
def rotate_left(self, u):
    w = u.right
    w.parent = u.parent
    if w.parent != self.nil:
        if w.parent.left == u:
            w.parent.left = w
        else:
            w.parent.right = w
    u.right = w.left
    if u.right != self.nil:
        u.right.parent = u
    u.parent = w
    w.left = u
    if u == self.r: 
        self.r = w
        self.r.parent = self.nil

def rotate_right(self, u):
    w = u.left
    w.parent = u.parent
    if w.parent != self.nil:
        if w.parent.left == u:
            w.parent.left = w
        else:
            w.parent.right = w
    u.left = w.right
    if u.left != self.nil:
        u.left.parent = u
    u.parent = w
    w.right = u
    if u == self.r:
        self.r = w
        self.r.parent = self.nil
```

就 Treap 数据结构而言，旋转最重要的性质是 $w$ 的深度减少 1，而 $u$ 的深度增加 1。

使用旋转，我们可以如下实现 $\mathrm{add}(x)$ 操作：我们创建一个新节点 $u$，赋值 $u.x \gets x$，并为 $u.p$ 选择一个随机值。接下来，我们使用 BinarySearchTree 的常规 $\mathrm{add}(x)$ 算法添加 $u$，使得 $u$ 现在成为 Treap 的一个叶子节点。此时，我们的 Treap 满足二叉搜索树性质，但不一定满足堆性质。特别地，可能出现 $u.parent.p > u.p$ 的情况。如果是这样，我们就在节点 $w=u.parent$ 处进行一次旋转，使得 $u$ 成为 $w$ 的父节点。如果 $u$ 仍然违反堆性质，我们将重复这一过程，每次使 $u$ 的深度减少 1，直到 $u$ 成为根节点或 $u.parent.p < u.p$。

```python
class Treap(BinarySearchTree):
    class Node(BinarySearchTree.Node):
        def __init__(self, x):
            super(Treap.Node, self).__init__(x)
            self.p = random.random()
            
    def __init__(self, iterable=[]):
        super(Treap, self).__init__(iterable)
    
    def _new_node(self, x):
        return Treap.Node(x)
        
    def add(self, x):
        u = self._new_node(x)
        if self.add_node(u):
            self.bubble_up(u)
            return True
        return False
            
    def bubble_up(self, u):
        while u != self.r and u.parent.p > u.p:
            if u.parent.right == u:
                self.rotate_left(u.parent)
            else:
                self.rotate_right(u.parent)
        if u.parent == self.nil:
            self.r = u
```

图 7.7 给出了一个 $\mathrm{add}(x)$ 操作的示例。

<div align="center">
**图 7.7:** 将值 1.5 添加到图 7.5 的 Treap 中。
</div>

$\mathrm{add}(x)$ 操作的运行时间由跟随 $x$ 的搜索路径所需的时间加上将新添加的节点 $u$ 向上移动到 Treap 中正确位置所执行的旋转次数决定。根据引理 7.2，搜索路径的期望长度最多为 $2\ln n + O(1)$。此外，每次旋转都会减少 $u$ 的深度。如果 $u$ 成为根节点则停止，因此期望的旋转次数不会超过搜索路径的期望长度。因此，Treap 中 $\mathrm{add}(x)$ 操作的期望运行时间为 $O(\log n)$。（练习 7.5 要求你证明添加期间执行的期望旋转次数实际上只有 $O(1)$。）

Treap 中的 $\mathrm{remove}(x)$ 操作与 $\mathrm{add}(x)$ 操作相反。我们搜索包含 $x$ 的节点 $u$，然后执行旋转将 $u$ 向下移动直到它成为叶子节点，然后将 $u$ 从 Treap 中拼接（splice）掉。注意，为了向下移动 $u$，我们可以在 $u$ 处执行左旋或右旋，这将分别用 $u.right$ 或 $u.left$ 替换 $u$。选择由以下第一条适用的规则决定：

1. 如果 $u.left$ 和 $u.right$ 都是 $nil$，那么 $u$ 是叶子节点，不执行旋转。
2. 如果 $u.left$（或 $u.right$）是 $nil$，则在 $u$ 处执行右旋（或左旋）。
3. 如果 $u.left.p < u.right.p$（或 $u.left.p > u.right.p$），则在 $u$ 处执行右旋（或左旋）。

这三条规则确保 Treap 不会断开连接，并且在移除 $u$ 后堆性质得以恢复。

```python
class Treap(BinarySearchTree):
    # ... (continued)
    def remove(self, x):
        u = self._find_last(x)
        if u is not None and u.x == x:
            self.trickle_down(u)
            self.splice(u)
            return True
        return False
        
    def trickle_down(self, u):
        while u.left is not None or u.right is not None:
            if u.left is None:
                self.rotate_left(u)
            elif u.right is None:
                self.rotate_right(u)
            elif u.left.p < u.right.p:
                self.rotate_right(u)
            else:
                self.rotate_left(u)
            if self.r == u:
                self.r = u.parent
```

图 7.8 给出了一个 $\mathrm{remove}(x)$ 操作的示例。

<div align="center">
**图 7.8:** 从图 7.5 的 Treap 中移除值 9。
</div>

分析 $\mathrm{remove}(x)$ 操作运行时间的技巧是注意该操作是 $\mathrm{add}(x)$ 操作的逆操作。特别地，如果我们使用相同的优先级 $u.p$ 重新插入 $x$，那么 $\mathrm{add}(x)$ 操作将执行完全相同次数的旋转，并将 Treap 恢复到 $\mathrm{remove}(x)$ 操作之前的状态。（从下往上阅读图 7.8，展示了将值 9 添加到 Treap 的过程。）这意味着，在大小为 $n$ 的 Treap 上执行 $\mathrm{remove}(x)$ 的期望运行时间与在大小为 $n-1$ 的 Treap 上执行 $\mathrm{add}(x)$ 的期望运行时间成正比。我们得出结论：$\mathrm{remove}(x)$ 的期望运行时间为 $O(\log n)$。

### 7.2.1 小结

以下定理总结了 Treap 数据结构的性能：

**定理 7.2.** *Treap 实现了 SSet 接口。Treap 支持 $\mathrm{add}(x)$、$\mathrm{remove}(x)$ 和 $\mathrm{find}(x)$ 操作，每个操作的期望时间为 $O(\log n)$。*

值得比较 Treap 数据结构和 SkiplistSSet 数据结构。两者都以每个操作 $O(\log n)$ 的期望时间实现 SSet 操作。在两种数据结构中，$\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 都涉及一次搜索，然后是指针的常数次修改（见下面的练习 7.5）。因此，对于这两种结构，搜索路径的期望长度是评估其性能的关键值。在 SkiplistSSet 中，搜索路径的期望长度为
$$
2\log n + O(1) .
$$
而在 Treap 中，搜索路径的期望长度为
$$
2\ln n +O(1) \approx 1.386\log n + O(1) .
$$
因此，Treap 中的搜索路径要短得多，这意味着 Treap 上的操作比 Skiplist 明显更快。第 4 章的练习 4.7 展示了如何通过使用有偏抛硬币将 Skiplist 中搜索路径的期望长度减少到
$$
e\ln n + O(1) \approx 1.884\log n + O(1) .
$$
即使有了这种优化，SkiplistSSet 中搜索路径的期望长度仍然明显长于 Treap。

---

## 7.3 讨论与练习（Discussion and Exercises）

随机二叉搜索树已被广泛研究。Devroye [19] 给出了引理 7.1 及相关结果的证明。文献中还有更强有力的结果，其中最令人印象深刻的是 Reed [62] 的成果，他证明了随机二叉搜索树的期望高度为
$$
\alpha\ln n - \beta\ln\ln n + O(1)
$$
其中 $\alpha\approx4.31107$ 是方程 $\alpha\ln((2e/\alpha))=1$ 在区间 $[2,\infty)$ 上的唯一解，且 $\beta=\frac{3}{2\ln(\alpha/2)}$。此外，高度的方差是常数。

Treap 这个名称由 Seidel 和 Aragon [65] 创造，他们讨论了 Treap 及其一些变体。然而，其基本结构更早之前就被 Vuillemin [74] 研究过，他称其为笛卡尔树（Cartesian trees）。

Treap 数据结构的一种可能的空间优化是消除每个节点中优先级 $p$ 的显式存储。取而代之的是，节点 $u$ 的优先级通过哈希 $u$ 的内存地址来计算。虽然许多哈希函数在实践中可能对此效果良好，但为了保持引理 7.1 证明中的重要部分仍然有效，哈希函数应该是随机化的，并具有**最小独立性质**（min-wise independent property）：对于任意不同的值 $x_1,\ldots,x_k$，每个哈希值 $h(x_1),\ldots,h(x_k)$ 应以高概率互不相同，并且对于每个 $i\in\{1,\ldots,k\}$，
$$
\Pr\{h(x_i) = \min\{h(x_1),\ldots,h(x_k)\}\} \le c/k
$$
对于某个常数 $c$。一类易于实现且相当快速的哈希函数是**制表哈希**（tabulation hashing）（第 5.2.3 节）。

另一种不在每个节点存储优先级的 Treap 变体是 Martínez 和 Roura [51] 的随机化二叉搜索树（randomized binary search tree）。在这种变体中，每个节点 $u$ 存储以 $u$ 为根的子树的大小 $u.size$。$\mathrm{add}(x)$ 和 $\mathrm{remove}(x)$ 算法都是随机化的。将 $x$ 添加到以 $u$ 为根的子树中的算法执行以下操作：

1. 以概率 $1/(\mathrm{size}(u)+1)$，值 $x$ 以通常方式作为叶子添加，然后执行旋转将 $x$ 提升到该子树的根。
2. 否则（以概率 $1-1/(\mathrm{size}(u)+1)$），值 $x$ 被递归地添加到以 $u.left$ 或 $u.right$ 为根的子树中（视情况而定）。

第一种情况对应于 Treap 中 $x$ 的节点接收到的随机优先级小于 $u$ 子树中任何优先级的情况，并且这种情况以完全相同的概率发生。

从随机化二叉搜索树中移除值 $x$ 类似于从 Treap 中移除的过程。我们找到包含 $x$ 的节点 $u$，然后执行旋转反复增加 $u$ 的深度，直到它成为叶子节点，此时我们可以将其从树中拼接出去。每一步选择左旋还是右旋是随机化的。

1. 以概率 $u.left.size/(u.size-1)$，我们在 $u$ 处执行右旋，使 $u.left$ 成为原以 $u$ 为根的子树的新根。
2. 以概率 $u.right.size/(u.size-1)$，我们在 $u$ 处执行左旋，使 $u.right$ 成为原以 $u$ 为根的子树的新根。

同样，我们可以轻松验证，这些正好是 Treap 中移除算法对 $u$ 执行左旋或右旋的相同概率。

与 treap 相比，随机化二叉搜索树的缺点是在添加和删除元素时需要做出许多随机选择，并且必须维护子树的大小。随机化二叉搜索树相对于 treap 的一个优点是，子树大小可以服务于另一个有用的目的，即以期望时间 $O(\log n)$ 提供按秩访问（参见练习 7.10）。相比之下，treap 节点中存储的随机优先级除了保持 treap 平衡之外没有其他用途。

---

**练习 7.1.** 在图 7.5 的 Treap 上，演示添加 4.5（优先级 7）然后添加 7.5（优先级 20）的过程。

**练习 7.2.** 在图 7.5 的 Treap 上，演示移除 5 然后移除 7 的过程。

**练习 7.3.** 证明存在 $21,964,800$ 种序列能生成图 7.1 右侧的树的断言。（提示：给出生成高度为 $h$ 的完全二叉树的序列数量的递归公式，并对 $h=3$ 计算该公式。）

**练习 7.4.** 设计并实现 $\mathrm{permute}(a)$ 方法，该方法接收包含 $n$ 个不同值的数组 $a$，并随机打乱 $a$。该方法应在 $O(n)$ 时间内运行，且你应该证明 $a$ 的每种可能的排列都是等概率的。

**练习 7.5.** 使用引理 7.2 的两部分来证明一次 $\mathrm{add}(x)$ 操作（以及 $\mathrm{remove}(x)$ 操作）执行的期望旋转次数是 $O(1)$。

**练习 7.6.** 修改此处给出的 Treap 实现，使其不显式存储优先级。取而代之，它通过哈希每个节点的 $\mathrm{hash\_code}()$ 来模拟优先级。

**练习 7.7.** 假设一棵二叉搜索树在每个节点 $u$ 处存储以 $u$ 为根的子树的高度 $u.height$ 和大小 $u.size$。

1. 说明如果我们在 $u$ 处执行左旋或右旋，那么这两个量对于所有受旋转影响的节点都可以在常数时间内更新。
2. 解释为什么如果我们也尝试存储每个节点 $u$ 的深度 $u.depth$，则无法得到相同的结果。

**练习 7.8.** 设计并实现一个算法，从已排序的 $n$ 个元素的数组 $a$ 构造一棵 Treap。该方法应以 $O(n)$ 最坏情况时间运行，并且应构造出一棵与通过 $\mathrm{add}(x)$ 方法逐个添加 $a$ 的元素无法区分的 Treap。

**练习 7.9.** 本练习详细说明如何利用一个接近待搜索节点的指针来高效搜索 Treap。

1. 设计并实现一个 Treap 实现，其中每个节点跟踪其子树中的最小值和最大值。
2. 利用这一额外信息，添加一个 $\mathrm{finger\_find}(x, u)$ 方法，该方法借助指向节点 $u$ 的指针执行 $\mathrm{find}(x)$ 操作（该指针希望离包含 $x$ 的节点不远）。该操作应从 $u$ 开始向上走，直到到达一个满足 $w.min \le x \le w.max$ 的节点 $w$。从那一点开始，它应执行从 $w$ 开始的标准搜索以查找 $x$。（可以证明 $\mathrm{finger\_find}(x, u)$ 的时间为 $O(1+\log r)$，其中 $r$ 是 treap 中值介于 $x$ 和 $u.x$ 之间的元素个数。）
3. 将你的实现扩展为 Treap 的一个版本，其所有 $\mathrm{find}(x)$ 操作都从最近一次 $\mathrm{find}(x)$ 找到的节点开始。

**练习 7.10.** 设计并实现一个版本的 Treap，其中包含一个 $\mathrm{get}(i)$ 操作，返回 Treap 中秩为 $i$ 的键。（提示：让每个节点 $u$ 跟踪以 $u$ 为根的子树的 大小。）

**练习 7.11.** 实现 TreapList，即 List 接口的一个 Treap 实现。Treap 中的每个节点存储一个列表项，对 Treap 的中序遍历按照它们在列表中出现的顺序找到所有项。所有 List 操作 $\mathrm{get}(i)$、$\mathrm{set}(i,x)$、$\mathrm{add}(i,x)$ 和 $\mathrm{remove}(i)$ 的期望时间应为 $O(\log n)$。

**练习 7.12.** 设计并实现一个版本的 Treap，支持 $\mathrm{split}(x)$ 操作。该操作从 Treap 中移除所有大于 $x$ 的值，并返回一个包含所有被移除值的新 Treap。

示例：代码 $t_2 \gets t.\mathrm{split}(x)$ 从 $t$ 中移除所有大于 $x$ 的值，并返回一个包含所有这些值的新 Treap $t_2$。$\mathrm{split}(x)$ 操作应在期望时间 $O(\log n)$ 内运行。

警告：为了使此修改正常工作并仍允许 $\mathrm{size}()$ 方法在常数时间内运行，有必要实现练习 7.10 中的修改。

**练习 7.13.** 设计并实现一个版本的 Treap，支持 $\mathrm{absorb}(t_2)$ 操作，该操作可视为 $\mathrm{split}(x)$ 操作的逆操作。此操作从 Treap $t_2$ 中移除所有值并将它们添加到接收者中。该操作的前提是 $t_2$ 中的最小值大于接收者中的最大值。$\mathrm{absorb}(t_2)$ 操作应在期望时间 $O(\log n)$ 内运行。

**练习 7.14.** 实现本节讨论的 Martínez 随机化二叉搜索树。将你的实现与 Treap 实现的性能进行比较。
