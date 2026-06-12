# 第6章 二叉树

本章介绍计算机科学中最基础的结构之一：二叉树（binary tree）。这里使用"树"这个词，是因为当我们绘制它们时，得到的图形常常类似于森林中的树木。定义二叉树的方式有很多种。从数学上讲，二叉树是一个连通、无向、有限且无环的图，其中所有顶点的度数都不大于三。

在大多数计算机科学应用中，二叉树是有根的：一个特殊的节点 ![$ \ensuremath{\ensuremath{\mathit{r}}}$](img2579.png)（度数最多为二）被称为树的根（root）。对于每个节点 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{u}}}}\neq \ensuremath{\ensuremath{\ensuremath{\mathit{r}}}}$](img2580.png)，从 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2581.png) 到 ![$ \ensuremath{\ensuremath{\mathit{r}}}$](img2582.png) 路径上的第二个节点被称为 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2583.png) 的父节点（parent）。与 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2584.png) 相邻的其他每个节点被称为 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2585.png) 的子节点（child）。我们感兴趣的大多数二叉树都是有序的，因此我们区分 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2586.png) 的左子节点（left child）和右子节点（right child）。

在插图中，二叉树通常从根向下绘制，根在绘图的顶部，左右子节点分别位于绘图的左右位置（图 [6.1](#fig:bintree-orientation)）。例如，图 [6.2](#fig:binary-tree).a 展示了一棵有九个节点的二叉树。

**图 6.1:** BinaryTree 中节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2588.png) 的父节点、左子节点和右子节点。

![\includegraphics[scale=0.90909]{figs-python/bintree-traverse-1}](img2587.png)

**图 6.2:** (a) 九个真实节点和 (b) 十个外部节点（external node）的二叉树。

![\includegraphics[width=\textwidth ]{figs-python/bintree-1}](img2589.png)

![\includegraphics[width=\textwidth ]{figs-python/bintree-2}](img2590.png)

(a)

(b)

由于二叉树非常重要，因此形成了特定的术语：二叉树中节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2591.png) 的深度（depth）是从 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2592.png) 到树根路径的长度。如果节点 ![$ \ensuremath{\ensuremath{\mathit{w}}}$](img2593.png) 位于从 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2594.png) 到 ![$ \ensuremath{\ensuremath{\mathit{r}}}$](img2595.png) 的路径上，则称 ![$ \ensuremath{\ensuremath{\mathit{w}}}$](img2596.png) 为 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2597.png) 的祖先（ancestor），称 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2598.png) 为 ![$ \ensuremath{\ensuremath{\mathit{w}}}$](img2599.png) 的后代（descendant）。节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2600.png) 的子树（subtree）是以 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2601.png) 为根且包含 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2602.png) 所有后代的二叉树。节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2603.png) 的高度（height）是从 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2604.png) 到其某个后代的最长路径的长度。树的高度是其根的高度。节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2605.png) 如果没有子节点，则称为叶子节点（leaf）。

我们有时认为树被外部节点（external node）扩充了。任何没有左子节点的节点都有一个外部节点作为其左子节点，相应地，任何没有右子节点的节点都有一个外部节点作为其右子节点（见图 [6.2](#fig:binary-tree).b）。通过归纳法很容易验证，一棵有 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}}\ge 1$](img2606.png) 个真实节点的二叉树有 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}}+1$](img2607.png) 个外部节点。

---

**子章节**

- [6.1 BinaryTree：基本二叉树](#61-binarytree-基本二叉树)
- [6.2 BinarySearchTree：不平衡二叉搜索树](#62-binarysearchtree-不平衡二叉搜索树)
- [6.3 讨论与练习](#63-讨论与练习)

---

## 6.1 BinaryTree：基本二叉树

表示二叉树中节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2608.png) 的最简单方式是显式存储 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2609.png) 的（最多三个）邻居。当这三个邻居中的一个不存在时，我们将其设为 ![$ \ensuremath{\ensuremath{\mathit{nil}}}$](img2610.png)。这样，树的外部节点和根的父节点都对应于值 ![$ \ensuremath{\ensuremath{\mathit{nil}}}$](img2611.png)。

二叉树本身可以通过对其根节点 ![$ \ensuremath{\ensuremath{\mathit{r}}}$](img2612.png) 的引用来表示：
![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{initialize}(...
...{\ensuremath{\mathit{r}} \gets \ensuremath{nil}}\\
\end{flushleft}\end{leftbar}](img2613.png)

我们可以通过计算从 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2614.png) 到根的路径上的步数来计算二叉树中节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2615.png) 的深度：
![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{depth}(\ensu...
...bf{return}} \ensuremath{\ensuremath{\mathit{d}}}\\
\end{flushleft}\end{leftbar}](img2616.png)

### 6.1.1 递归算法

使用递归算法可以非常方便地计算二叉树的各类属性。例如，要计算以节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2617.png) 为根的二叉树的大小（节点数），我们可以递归计算 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2618.png) 的两个子树的大小，将它们相加，然后加一：

![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{size}(\ensur...
...remath{\mathit{u}}.\ensuremath{\mathit{right}})}\\
\end{flushleft}\end{leftbar}](img2619.png)

要计算节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2620.png) 的高度，我们可以计算 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2621.png) 的两个子树的高度，取最大值，然后加一：

![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{height}(\ens...
...emath{\mathit{u}}.\ensuremath{\mathit{right}})})\\
\end{flushleft}\end{leftbar}](img2622.png)

### 6.1.2 遍历二叉树

上一节的两个算法都使用递归来访问二叉树中的所有节点。它们都以与以下代码相同的顺序访问二叉树的节点：
![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{traverse}(\e...
...remath{\mathit{u}}.\ensuremath{\mathit{right}})}\\
\end{flushleft}\end{leftbar}](img2623.png)

这样使用递归会产生非常简短、简单的代码，但也可能存在问题。递归的最大深度由二叉树中节点的最大深度（即树的高度）决定。如果树的高度非常大，那么这种递归可能会使用超过可用的栈空间，从而导致崩溃。

要无递归地遍历二叉树，可以使用一种依赖于节点来自何处来决定下一步去向的算法。见图 [6.3](#fig:bintree-traverse)。如果我们从 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{parent}}}$](img2625.png) 到达节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2624.png)，那么下一步是访问 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{left}}}$](img2626.png)。如果我们从 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{left}}}$](img2628.png) 到达 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2627.png)，那么下一步是访问 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{right}}}$](img2629.png)。如果我们从 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{right}}}$](img2631.png) 到达 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2630.png)，那么我们就完成了对 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2632.png) 子树的访问，因此返回到 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{parent}}}$](img2633.png)。以下代码实现了这个想法，并包含了处理 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{left}}}$](img2634.png)、 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{right}}}$](img2635.png) 或 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{parent}}}$](img2636.png) 为 ![$ \ensuremath{\ensuremath{\mathit{nil}}}$](img2637.png) 的情况的代码：
![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{traverse2}()...
...{\ensuremath{\mathit{u}} \gets \ensuremath{nxt}}\\
\end{flushleft}\end{leftbar}](img2638.png)

**图 6.3:** 非递归遍历二叉树时，节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2641.png) 处出现的三种情况，以及由此产生的树的遍历顺序。

![\includegraphics[scale=0.90909]{figs-python/bintree-traverse-2}](img2639.png) ![\includegraphics[scale=0.90909]{figs-python/bintree-3}](img2640.png)

可以用递归算法计算的相同信息也可以用这种方式无递归地计算。例如，要计算树的大小，我们维护一个计数器 ![$ \ensuremath{\ensuremath{\mathit{n}}}$](img2642.png)，并在每次首次访问节点时递增 ![$ \ensuremath{\ensuremath{\mathit{n}}}$](img2643.png)：
![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{size2}()}\\ ...
... {\color{black} \textbf{return}} \ensuremath{n} \\
\end{flushleft}\end{leftbar}](img2644.png)

在某些二叉树的实现中，不使用 ![$ \ensuremath{\ensuremath{\mathit{parent}}}$](img2645.png) 字段。在这种情况下，仍然可以实现非递归遍历，但必须使用列表（List）或栈（Stack）来跟踪从当前节点到根的路径。

一种不属于上述函数模式的特殊遍历是广度优先遍历（breadth-first traversal）。在广度优先遍历中，节点按层逐层访问，从根开始向下移动，每层从左到右访问节点（见图 [6.4](#fig:bintree-bfs)）。这类似于我们阅读英文页面的方式。广度优先遍历使用队列 ![$ \ensuremath{\ensuremath{\mathit{q}}}$](img2646.png) 实现，该队列初始时仅包含根 ![$ \ensuremath{\ensuremath{\mathit{r}}}$](img2647.png)。在每一步，我们从 ![$ \ensuremath{\ensuremath{\mathit{q}}}$](img2649.png) 中取出下一个节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2648.png)，处理 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2650.png)，然后将 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{left}}}$](img2651.png) 和 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{right}}}$](img2652.png)（如果它们不为 ![$ \ensuremath{\ensuremath{\mathit{nil}}}$](img2653.png)）添加到 ![$ \ensuremath{\ensuremath{\mathit{q}}}$](img2654.png) 中：
![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{bf\_traverse...
...remath{\mathit{u}}.\ensuremath{\mathit{right}})}\\
\end{flushleft}\end{leftbar}](img2655.png)

**图 6.4:** 在广度优先遍历中，二叉树的节点按层逐层访问，每层内从左到右访问。

![\includegraphics[scale=0.90909]{figs-python/bintree-4}](img2656.png)

---

## 6.2 BinarySearchTree：不平衡二叉搜索树

BinarySearchTree 是一种特殊的二叉树，其中每个节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2657.png) 还存储一个来自某个全序的数据值 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{x}}}$](img2658.png)。二叉搜索树中的数据值遵循**二叉搜索树性质**（binary search tree property）：对于节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2659.png)，存储在 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{left}}}$](img2660.png) 子树中的每个数据值都小于 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{x}}}$](img2661.png)，存储在 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{right}}}$](img2662.png) 子树中的每个数据值都大于 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{x}}}$](img2663.png)。图 [6.5](#fig:bst) 展示了一个 BinarySearchTree 的示例。

**图 6.5:** 一棵二叉搜索树。

![\includegraphics[scale=0.90909]{figs-python/bst-example}](img2664.png)

### 6.2.1 搜索

二叉搜索树性质非常有用，因为它允许我们快速定位二叉搜索树中的值 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2665.png)。为此，我们从根 ![$ \ensuremath{\ensuremath{\mathit{r}}}$](img2667.png) 开始搜索 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2666.png)。在检查节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2668.png) 时，有三种情况：

1. 如果 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{x}}}}< \ensuremath{\ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{x}}}}$](img2669.png)，则搜索继续到 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{left}}}$](img2670.png)；
2. 如果 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{x}}}}> \ensuremath{\ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{x}}}}$](img2671.png)，则搜索继续到 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{right}}}$](img2672.png)；
3. 如果 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{x}}}}= \ensuremath{\ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{x}}}}$](img2673.png)，则我们找到了包含 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2675.png) 的节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2674.png)。

搜索在情况 3 发生时或当 ![$ \ensuremath{\ensuremath{\mathit{u}}\gets \ensuremath{nil}}$](img2676.png) 时终止。在前一种情况下，我们找到了 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2677.png)。在后一种情况下，我们得出结论， ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2678.png) 不在二叉搜索树中。
![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{find\_eq}(\e...
...{return}} \ensuremath{\ensuremath{\mathit{nil}}}\\
\end{flushleft}\end{leftbar}](img2679.png)

图 [6.6](#fig:bst-search) 展示了二叉搜索树中两次搜索的示例。如第二个示例所示，即使我们在树中没有找到 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2680.png)，我们仍然获得了一些有价值的信息。如果我们观察情况 1 最后一次发生的节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2681.png)，我们会发现 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{x}}}$](img2682.png) 是树中大于 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2683.png) 的最小值。类似地，情况 2 最后一次发生的节点包含树中小于 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2684.png) 的最大值。因此，通过跟踪情况 1 最后一次发生的节点 ![$ \ensuremath{\ensuremath{\mathit{z}}}$](img2685.png)，BinarySearchTree 可以实现 ![$ \ensuremath{\mathrm{find}(\ensuremath{\mathit{x}})}$](img2686.png) 操作，该操作返回树中存储的大于或等于 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2687.png) 的最小值：
![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{find}(\ensur...
...\ensuremath{\mathit{z}}.\ensuremath{\mathit{x}}}\\
\end{flushleft}\end{leftbar}](img2688.png)

**图 6.6:** 二叉搜索树中 (a) 成功搜索（查找 ![$ 6$](img2691.png)）和 (b) 不成功搜索（查找 ![$ 10$](img2692.png)）的示例。

![\includegraphics[width=\textwidth ]{figs-python/bst-example-2}](img2689.png)

![\includegraphics[width=\textwidth ]{figs-python/bst-example-3}](img2690.png)

(a)

(b)

### 6.2.2 添加

要向 BinarySearchTree 添加新值 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2695.png)，我们首先搜索 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2696.png)。如果找到了它，则无需插入。否则，我们将 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2697.png) 存储在搜索 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2699.png) 过程中遇到的最后一个节点 ![$ \ensuremath{\ensuremath{\mathit{p}}}$](img2698.png) 的叶子子节点上。新节点是 ![$ \ensuremath{\ensuremath{\mathit{p}}}$](img2700.png) 的左子节点还是右子节点取决于比较 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2701.png) 和 ![$ \ensuremath{\ensuremath{\mathit{p}}.\ensuremath{\mathit{x}}}$](img2702.png) 的结果。
![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{add}(\ensure...
...}, \mathrm{new\_node}(\ensuremath{\mathit{x}})})\\
\end{flushleft}\end{leftbar}](img2703.png)

![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{find\_last}(...
...return}} \ensuremath{\ensuremath{\mathit{prev}}}\\
\end{flushleft}\end{leftbar}](img2704.png)

![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{add\_child}(...
...eturn}} \ensuremath{\ensuremath{\mathit{true}}} \\
\end{flushleft}\end{leftbar}](img2705.png)

图 [6.7](#fig:bst-insert) 展示了一个示例。此过程中最耗时的部分是最初对 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2706.png) 的搜索，其时间与新增节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2707.png) 的高度成正比。在最坏情况下，这等于 BinarySearchTree 的高度。

**图 6.7:** 将值 ![$ 8.5$](img2710.png) 插入二叉搜索树。

![\includegraphics[width=\textwidth ]{figs-python/bst-example-4}](img2708.png)

![\includegraphics[width=\textwidth ]{figs-python/bst-example-5}](img2709.png)

### 6.2.3 删除

删除存储在 BinarySearchTree 节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2712.png) 中的值要稍微困难一些。如果 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2713.png) 是叶子节点，那么我们可以直接将其从父节点上断开。更好的是：如果 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2715.png) 只有一个子节点，那么我们可以通过让 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{parent}}}$](img2717.png) 收养 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2718.png) 的子节点，将 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2716.png) 从树中拼接出去（见图 [6.8](#fig:bst-splice)）：
![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{splice}(\ens...
... \gets \ensuremath{\ensuremath{\mathit{n}} - 1}}\\
\end{flushleft}\end{leftbar}](img2719.png)

**图 6.8:** 删除叶子节点（ ![$ 6$](img2721.png)）或只有一个子节点的节点（ ![$ 9$](img2722.png)）很容易。

![\includegraphics[scale=0.90909]{figs-python/bst-splice}](img2720.png)

然而，当 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2725.png) 有两个子节点时，情况就变得棘手了。在这种情况下，最简单的方法是找到一个子节点少于两个的节点 ![$ \ensuremath{\ensuremath{\mathit{w}}}$](img2726.png)，使得 ![$ \ensuremath{\ensuremath{\mathit{w}}.\ensuremath{\mathit{x}}}$](img2727.png) 可以替换 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{x}}}$](img2728.png)。为了维护二叉搜索树性质，值 ![$ \ensuremath{\ensuremath{\mathit{w}}.\ensuremath{\mathit{x}}}$](img2729.png) 应该接近 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{x}}}$](img2730.png) 的值。例如，选择 ![$ \ensuremath{\ensuremath{\mathit{w}}}$](img2731.png) 使得 ![$ \ensuremath{\ensuremath{\mathit{w}}.\ensuremath{\mathit{x}}}$](img2732.png) 是大于 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{x}}}$](img2733.png) 的最小值即可。找到节点 ![$ \ensuremath{\ensuremath{\mathit{w}}}$](img2734.png) 很容易；它是以 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{right}}}$](img2735.png) 为根的子树中的最小值。这个节点很容易被移除，因为它没有左子节点（见图 [6.9](#fig:bst-remove)）。
![\begin{leftbar}
\begin{flushleft}
\hspace*{1em} \ensuremath{\mathrm{remove\_node...
...remath{\mathrm{splice}(\ensuremath{\mathit{w}})}\\
\end{flushleft}\end{leftbar}](img2736.png)

**图 6.9:** 从有两个子节点的节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2740.png) 中删除值（ ![$ 11$](img2739.png)），通过用 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2741.png) 右子树中的最小值替换 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2742.png) 的值来完成。

![\includegraphics[width=\textwidth ]{figs-python/bst-delete-1}](img2737.png) ![\includegraphics[width=\textwidth ]{figs-python/bst-delete-2}](img2738.png)

### 6.2.4 总结

BinarySearchTree 中的 ![$ \ensuremath{\mathrm{find}(\ensuremath{\mathit{x}})}$](img2743.png)、 ![$ \ensuremath{\mathrm{add}(\ensuremath{\mathit{x}})}$](img2744.png) 和 ![$ \ensuremath{\mathrm{remove}(\ensuremath{\mathit{x}})}$](img2745.png) 操作都涉及沿着从树根到树中某个节点的路径。在不了解树形状更多信息的情况下，很难对这条路径的长度做过多描述，只知道它小于树中节点数 ![$ \ensuremath{\ensuremath{\mathit{n}}}$](img2746.png)。以下（并不令人印象深刻的）定理总结了 BinarySearchTree 数据结构的性能：

**定理 6.1** *BinarySearchTree 实现了 SSet 接口，并支持 ![$ \ensuremath{\mathrm{add}(\ensuremath{\mathit{x}})}$](img2747.png)、 ![$ \ensuremath{\mathrm{remove}(\ensuremath{\mathit{x}})}$](img2748.png) 和 ![$ \ensuremath{\mathrm{find}(\ensuremath{\mathit{x}})}$](img2749.png) 操作，每次操作的时间为 ![$ O(\ensuremath{\ensuremath{\ensuremath{\mathit{n}}}})$](img2750.png)。*

定理 [6.1](#thm:bst) 与定理 [4.1](4_2_SkiplistSSet_Efficient_.html#thm:skiplist) 相比相形见绌，后者表明 SkiplistSSet 结构可以在每次操作 ![$ O(\log \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}})$](img2751.png) 的期望时间内实现 SSet 接口。BinarySearchTree 结构的问题在于它可能变得不平衡。它可能不像图 [6.5](#fig:bst) 中的树，而可能看起来像一条由 ![$ \ensuremath{\ensuremath{\mathit{n}}}$](img2752.png) 个节点组成的长链，除最后一个节点外每个节点恰好有一个子节点。

有许多方法可以避免二叉搜索树的不平衡，所有这些方法都能产生具有 ![$ O(\log \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}})$](img2753.png) 时间操作的数据结构。在第 [7](7_Random_Binary_Search_Tree.html#chap:rbs) 章中，我们展示了如何通过随机化实现 ![$ O(\log \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}})$](img2754.png) 的期望时间操作。在第 [8](8_Scapegoat_Trees.html#chap:scapegoat) 章中，我们展示了如何通过部分重建操作实现 ![$ O(\log \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}})$](img2755.png) 的摊还时间操作。在第 [9](9_Red_Black_Trees.html#chap:redblack) 章中，我们展示了如何通过模拟一种非二叉树（节点最多可以有四个子节点）来实现 ![$ O(\log \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}})$](img2756.png) 的最坏情况时间操作。

---

## 6.3 讨论与练习

二叉树已经被用来建模各种关系数千年了。其原因之一是二叉树自然地建模了（谱系）家谱。在这些家谱中，根是一个人，左子节点和右子节点分别是此人的父母，依此类推递归下去。在近几个世纪中，二叉树也被用于生物学中建模物种树，其中树的叶子代表现存物种，内部节点代表一个物种的两个种群进化成两个独立物种的物种形成事件。

二叉搜索树似乎是在 1950 年代由几个研究小组独立发现的 \[[48](Bibliography.html#k97v3), 第 6.2.2 节\]。关于特定类型二叉搜索树的更多参考文献将在后续章节中提供。

在从头实现二叉树时，需要做出几个设计决策。其中之一是每个节点是否存储指向其父节点的指针。如果大多数操作只是沿着根到叶子的路径进行，那么父指针是不必要的，会浪费空间，并且是编码错误的潜在来源。另一方面，缺少父指针意味着树遍历必须递归地或使用显式栈来完成。其他一些方法（例如在某种平衡二叉搜索树中插入或删除）也会因缺少父指针而变得复杂。

另一个设计决策涉及如何在节点中存储父节点、左子节点和右子节点指针。在此处给出的实现中，这些指针存储为独立的变量。另一种选择是将它们存储在一个长度为 3 的数组 ![$ \ensuremath{\ensuremath{\mathit{p}}}$](img2757.png) 中，这样 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{p}}[0]}$](img2758.png) 是 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2759.png) 的左子节点， ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{p}}[1]}$](img2760.png) 是 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2761.png) 的右子节点， ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{p}}[2]}$](img2762.png) 是 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2763.png) 的父节点。这样使用数组意味着某些 ![$ {\color{black} \textbf{if}}$](img2764.png) 语句序列可以简化为代数表达式。

这种简化的一个例子发生在树遍历期间。如果遍历从 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{p}}[\ensuremath{\mathit{i}}]}$](img2766.png) 到达节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2765.png)，那么遍历中的下一个节点是 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{p}}}}[(\ensuremath{\ensuremath{\ensuremath{\mathit{i}}}}+1)\bmod 3]$](img2767.png)。类似的例子出现在左右对称的情况中。例如， ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{p}}[\ensuremath{\mathit{i}}]}$](img2768.png) 的兄弟节点是 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{p}}}}[(\ensuremath{\ensuremath{\ensuremath{\mathit{i}}}}+1)\bmod 2]$](img2769.png)。这个技巧无论 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{p}}[\ensuremath{\mathit{i}}]}$](img2770.png) 是 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2773.png) 的左子节点（ ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{i}}}}=0$](img2771.png)）还是右子节点（ ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{i}}}}=1$](img2772.png)）都有效。在某些情况下，这意味着原本需要同时具有左版本和右版本的复杂代码可以只编写一次。参见第 ![[*]](crossref.png) 页的 ![$ \ensuremath{\mathrm{rotate\_left}(\ensuremath{\mathit{u}})}$](img2774.png) 和 ![$ \ensuremath{\mathrm{rotate\_right}(\ensuremath{\mathit{u}})}$](img2775.png) 方法示例。

**练习 6.1** 证明一棵有 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}}\ge 1$](img2776.png) 个节点的二叉树有 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}}-1$](img2777.png) 条边。

**练习 6.2** 证明一棵有 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}}\ge 1$](img2778.png) 个真实节点的二叉树有 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}}+1$](img2779.png) 个外部节点。

**练习 6.3** 证明，如果一棵二叉树 ![$ T$](img2780.png) 至少有一个叶子节点，那么要么 (a) ![$ T$](img2781.png) 的根最多有一个子节点，要么 (b) ![$ T$](img2782.png) 有多个叶子节点。

**练习 6.4** 实现一个非递归方法 ![$ \ensuremath{\mathrm{size2}(\ensuremath{\mathit{u}})}$](img2783.png)，计算以节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2784.png) 为根的子树的大小。

**练习 6.5** 编写一个非递归方法 ![$ \ensuremath{\mathrm{height2}(\ensuremath{\mathit{u}})}$](img2785.png)，计算 BinaryTree 中节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2786.png) 的高度。

**练习 6.6** 如果对于每个节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2787.png)，以 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{left}}}$](img2788.png) 和 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{right}}}$](img2789.png) 为根的子树的大小相差不超过一，则称该二叉树是大小平衡的。编写一个递归方法 ![$ \ensuremath{\mathrm{is\_balanced}()}$](img2790.png)，测试二叉树是否平衡。你的方法应在 ![$ O(\ensuremath{\ensuremath{\ensuremath{\mathit{n}}}})$](img2791.png) 时间内运行。（请务必在不同形状的大型树上测试你的代码；很容易写出一个耗时远超过 ![$ O(\ensuremath{\ensuremath{\ensuremath{\mathit{n}}}})$](img2792.png) 的方法。）

二叉树的前序遍历（pre-order traversal）是在访问任何子节点之前访问每个节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2793.png) 的遍历。中序遍历（in-order traversal）在访问 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2794.png) 左子树中的所有节点之后、但在访问 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2796.png) 右子树中任何节点之前访问 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2795.png)。后序遍历（post-order traversal）仅在访问 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2798.png) 子树中所有其他节点之后才访问 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2797.png)。树的前/中/后序编号（pre/in/post-order numbering）用整数 ![$ 0,\ldots,\ensuremath{\ensuremath{\ensuremath{\mathit{n}}}}-1$](img2799.png) 标记树的节点，顺序为前序/中序/后序遍历遇到它们的顺序。示例见图 [6.10](#fig:binarytree-numbering)。

**图 6.10:** 二叉树的前序、后序和中序编号。

![\includegraphics[scale=0.90909]{figs-python/binarytree-numbering-1}](img2800.png) ![\includegraphics[scale=0.90909]{figs-python/binarytree-numbering-2}](img2801.png)

![\includegraphics[scale=0.90909]{figs-python/binarytree-numbering-3}](img2802.png)

**练习 6.7** 创建 BinaryTree 的一个子类，其节点具有存储前序、后序和中序编号的字段。编写递归方法 ![$ \ensuremath{\mathrm{pre\_order\mathrm{Number}}()}$](img2803.png)、 ![$ \ensuremath{\mathrm{in\_order\mathrm{Number}}()}$](img2804.png) 和 ![$ \ensuremath{\mathrm{post\_order\mathrm{Numbers}}()}$](img2805.png) 来正确分配这些编号。这些方法应分别在 ![$ O(\ensuremath{\ensuremath{\ensuremath{\mathit{n}}}})$](img2806.png) 时间内运行。

**练习 6.8** 实现非递归函数 ![$ \ensuremath{\mathrm{next\_pre\mathrm{Order}}(\ensuremath{\mathit{u}})}$](img2807.png)、 ![$ \ensuremath{\mathrm{next\_in\mathrm{Order}}(\ensuremath{\mathit{u}})}$](img2808.png) 和 ![$ \ensuremath{\mathrm{next\_post\mathrm{Order}}(\ensuremath{\mathit{u}})}$](img2809.png)，它们分别返回在前序、中序或后序遍历中跟随 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2810.png) 的下一个节点。这些函数应具有摊还常数时间；如果我们从任意节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2811.png) 开始，重复调用这些函数之一并将返回值赋给 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2812.png)，直到 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{u}}}}=\ensuremath{\ensuremath{\ensuremath{\mathit{nil}}}}$](img2813.png)，那么所有这些调用的总代价应为 ![$ O(\ensuremath{\ensuremath{\ensuremath{\mathit{n}}}})$](img2814.png)。

**练习 6.9** 假设我们有一棵二叉树，其节点已被分配了前序、后序和中序编号。展示如何利用这些编号在常数时间内回答以下每个问题：

1. 给定节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2815.png)，确定以 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2816.png) 为根的子树的大小。
2. 给定节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2817.png)，确定 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2818.png) 的深度。
3. 给定两个节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2819.png) 和 ![$ \ensuremath{\ensuremath{\mathit{w}}}$](img2820.png)，判断 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2821.png) 是否是 ![$ \ensuremath{\ensuremath{\mathit{w}}}$](img2822.png) 的祖先。

**练习 6.10** 假设你有一个节点列表，其上分配了前序和中序编号。证明最多存在一棵可能的树具有这样的前序/中序编号，并展示如何构造它。

**练习 6.11** 证明任意一棵有 ![$ \ensuremath{\ensuremath{\mathit{n}}}$](img2823.png) 个节点的二叉树的形状最多可以用 ![$ 2(\ensuremath{\ensuremath{\ensuremath{\mathit{n}}}}-1)$](img2824.png) 位表示。（提示：考虑记录遍历过程中发生的情况，然后重放该记录以重建树。）

**练习 6.12** 说明当我们向图 [6.5](6_2_BinarySearchTree_Unbala.html#fig:bst) 中的二叉搜索树依次添加值 ![$ 3.5$](img2825.png) 和 4.5 时会发生什么。

**练习 6.13** 说明当我们从图 [6.5](6_2_BinarySearchTree_Unbala.html#fig:bst) 中的二叉搜索树依次删除值 ![$ 3$](img2826.png) 和 5 时会发生什么。

**练习 6.14** 实现一个 BinarySearchTree 方法 ![$ \ensuremath{\mathrm{get\_lE}(\ensuremath{\mathit{x}})}$](img2827.png)，返回树中所有小于或等于 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2828.png) 的项的列表。你的方法的运行时间应为 ![$ O(\ensuremath{\ensuremath{\ensuremath{\mathit{n}}}}'+\ensuremath{\ensuremath{\ensuremath{\mathit{h}}}})$](img2829.png)，其中 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}}'$](img2830.png) 是小于或等于 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2831.png) 的项数， ![$ \ensuremath{\ensuremath{\mathit{h}}}$](img2832.png) 是树的高度。

**练习 6.15** 描述如何将元素 ![$ \{1,\ldots,\ensuremath{\ensuremath{\ensuremath{\mathit{n}}}}\}$](img2833.png) 添加到一个初始为空的 BinarySearchTree 中，使得结果树的高度为 ![$ \ensuremath{\ensuremath{\ensuremath{\mathit{n}}}}-1$](img2834.png)。有多少种方法可以做到这一点？

**练习 6.16** 如果我们有一个 BinarySearchTree，并执行 ![$ \ensuremath{\mathrm{add}(\ensuremath{\mathit{x}})}$](img2835.png) 后跟 ![$ \ensuremath{\mathrm{remove}(\ensuremath{\mathit{x}})}$](img2836.png) 操作（使用相同的 ![$ \ensuremath{\ensuremath{\mathit{x}}}$](img2837.png) 值），我们是否一定会回到原始树？

**练习 6.17** 一次 ![$ \ensuremath{\mathrm{remove}(\ensuremath{\mathit{x}})}$](img2838.png) 操作会增加 BinarySearchTree 中任何节点的高度吗？如果能，会增加多少？

**练习 6.18** 一次 ![$ \ensuremath{\mathrm{add}(\ensuremath{\mathit{x}})}$](img2839.png) 操作会增加 BinarySearchTree 中任何节点的高度吗？它能增加树的高度吗？如果能，会增加多少？

**练习 6.19** 设计并实现一个 BinarySearchTree 版本，其中每个节点 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2840.png) 维护值 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{size}}}$](img2841.png)（以 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2842.png) 为根的子树的大小）、 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{depth}}}$](img2843.png)（ ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2844.png) 的深度）和 ![$ \ensuremath{\ensuremath{\mathit{u}}.\ensuremath{\mathit{height}}}$](img2845.png)（以 ![$ \ensuremath{\ensuremath{\mathit{u}}}$](img2846.png) 为根的子树的高度）。

即使在调用 ![$ \ensuremath{\mathrm{add}(\ensuremath{\mathit{x}})}$](img2847.png) 和 ![$ \ensuremath{\mathrm{remove}(\ensuremath{\mathit{x}})}$](img2848.png) 操作期间，这些值也应被维护，但这不应使这些操作的成本增加超过一个常数因子。
