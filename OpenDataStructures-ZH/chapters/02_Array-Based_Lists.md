# 第2章 基于数组的列表

在本章中，我们将研究 List 和 Queue 接口的实现，其底层数据存储在数组中，这个数组称为**后备数组**（backing array）。下表总结了本章中介绍的数据结构的操作运行时间：

| | `get(i)` / `set(i,x)` | `add(i,x)` / `remove(i)` |
|---|---|---|
| ArrayStack | O(1) | O(n - i) |
| ArrayDeque | O(1) | O(min{i, n - i}) |
| DualArrayDeque | O(1) | O(min{i, n - i}) |
| RootishArrayStack | O(1) | O(n - i) |

通过将数据存储在单个数组中来工作的数据结构具有许多共同的优点和局限性：

- **数组提供常数时间的访问**（constant time access）。数组中的任何值都可以在常数时间内访问。这就是 `get(i)` 和 `set(i,x)` 能够在常数时间内运行的原因。

- **数组不够动态**（not very dynamic）。在列表中间附近添加或删除一个元素意味着数组中的大量元素需要移位，以便为新添加的元素腾出空间或填补删除元素留下的空缺。这就是 `add(i,x)` 和 `remove(i)` 操作的运行时间依赖于 n 和 i 的原因。

- **数组不能扩展或收缩**（cannot expand or shrink）。当数据结构中的元素数量超过后备数组的大小时，需要分配一个新数组，并将旧数组中的数据复制到新数组中。这是一个代价高昂的操作。

第三点很重要。上表中引用的运行时间不包括与增长和缩小后备数组相关的代价。我们将看到，如果管理得当，增长和缩小后备数组的代价并不会给平均操作增加太多成本。更精确地说，如果我们从一个空的数据结构开始，并执行任意序列的 m 次 `add(i,x)` 或 `remove(i)` 操作，那么在整个 m 次操作序列中，增长和缩小后备数组的总代价为 O(m)。虽然某些个别操作代价更高，但**摊销代价**（amortized cost）在所有 m 次操作上分摊后，每次操作仅为 O(1)。

---

## 2.1 ArrayStack：使用数组的快速栈操作

ArrayStack 使用数组 `a`（称为后备数组）来实现 List 接口。索引为 i 的列表元素存储在 `a[i]` 中。在大多数情况下，`a.length` 大于严格必要的长度，因此使用整数 `n` 来跟踪实际存储在 `a` 中的元素数量。这样，列表元素存储在 `a[0]`,...,`a[n-1]` 中，并且在任何时候都有 `a.length >= n`。

### 2.1.1 基础操作

使用 `get(i)` 和 `set(i,x)` 访问和修改 ArrayStack 的元素是简单的。在执行必要的边界检查之后，我们只需返回或设置 `a[i]`。

在 ArrayStack 中添加和删除元素的操作如图 2.1 所示。要实现 `add(i,x)` 操作，我们首先检查 `a` 是否已满。如果是，我们调用 `resize()` 方法来增加 `a` 的大小。如何实现 `resize()` 将在后面讨论。目前，只要知道在调用 `resize()` 之后，我们可以确保 `a.length > n` 就够了。完成这一步后，我们将元素 `a[i]`,...,`a[n-1]` 向右移动一个位置，为 `x` 腾出空间，将 `a[i]` 设置为 `x`，并递增 `n`。

> **图 2.1：** 在 ArrayStack 上的一系列 `add(i,x)` 和 `remove(i)` 操作。箭头表示被复制的元素。导致调用 `resize()` 的操作用星号标记。

如果我们忽略潜在的 `resize()` 调用的代价，那么 `add(i,x)` 操作的代价与我们必须移位以容纳 `x` 的元素数量成正比。因此，该操作的代价（忽略调整 `a` 大小的代价）为 O(n - i)。

实现 `remove(i)` 操作类似。我们将元素 `a[i+1]`,...,`a[n-1]` 向左移动一个位置（覆盖 `a[i]`），并减少 `n` 的值。完成此操作后，我们检查 `a.length >= 3n` 是否成立，如果是，则调用 `resize()` 来减小 `a` 的大小。

如果忽略 `resize()` 方法的代价，`remove(i)` 操作的代价与移位的元素数量成正比，即 O(n - i)。

### 2.1.2 增长和缩小

`resize()` 方法相当直接；它分配一个新的数组 `b`，其大小为 `max(1, 2n)`，并将 `a` 中的 `n` 个元素复制到 `b` 的前 `n` 个位置，然后将 `a` 设置为 `b`。因此，在调用 `resize()` 之后，`a.length = max(1, 2n)`。

分析 `resize()` 操作的实际代价很简单。它分配一个大小为 `max(1, 2n)` 的数组 `b`，并将 `a` 中的 `n` 个元素复制到 `b` 中。这需要 O(n) 时间。

上一节的运行时间分析忽略了 `resize()` 调用的代价。在本节中，我们使用一种称为**摊销分析**（amortized analysis）的技术来分析这一代价。这种技术并不试图确定每次单独的 `add(i,x)` 和 `remove(i)` 操作中调整大小的代价。相反，它考虑在一次序列的 `add(i,x)` 或 `remove(i)` 调用中所有 `resize()` 调用的代价。特别地，我们将证明：

> **引理 2.1** 如果创建了一个空的 ArrayStack，并执行了任意序列的 m 次 `add(i,x)` 和 `remove(i)` 调用，那么在所有 `resize()` 调用中花费的总时间为 O(m)。

**证明：** 我们将证明，每当调用 `resize()` 时，自上次调用 `resize()` 以来 `add(i,x)` 或 `remove(i)` 的调用次数至少为 n/2 - 1。因此，如果 `n_i` 表示第 i 次调用 `resize()` 期间的 n 值，r 表示 `resize()` 调用的次数，那么 `add(i,x)` 或 `remove(i)` 调用的总次数至少为

```
∑(i=1 to r) (n_i/2 - 1) = (1/2) ∑(i=1 to r) n_i - r
```

这等价于

```
∑(i=1 to r) n_i ≤ 2m + 2r
```

另一方面，所有 `resize()` 调用花费的总时间为

```
O(∑(i=1 to r) n_i) = O(m + r) = O(m)
```

因为 r 不大于 m。剩下的就是证明在第 i 次和第 i+1 次 `resize()` 调用之间 `add(i,x)` 或 `remove(i)` 的调用次数至少为 n_i/2。

有两种情况需要考虑。第一种情况，`resize()` 被 `add(i,x)` 调用是因为后备数组 `a` 已满，即 `a.length = n`。考虑上一次 `resize()` 调用：在这次调用之后，`a` 的大小为 `2 * n_{i-1}`，但存储在 `a` 中的元素数量最多为 n_{i-1}。但现在存储在 `a` 中的元素数量是 n_i，所以自上一次 `resize()` 调用以来必须至少有 `n_i - n_{i-1} ≥ n_i - n_i/2 = n_i/2` 次 `add(i,x)` 调用。

第二种情况发生在 `resize()` 被 `remove(i)` 调用是因为 `a.length ≥ 3n`。同样，在上一次 `resize()` 调用之后，存储在 `a` 中的元素数量至少为 `n_{i-1}/2`。现在有 `n_i` 个元素存储在 `a` 中。因此，自上次 `resize()` 调用以来的 `remove(i)` 操作数量至少为 `n_{i-1} - n_i ≥ n_{i-1} - (2/3)n_{i-1} = n_{i-1}/3`。

在任一种情况下，在第 i 次和第 i+1 次 `resize()` 调用之间发生的 `add(i,x)` 或 `remove(i)` 调用次数至少为 n_i/2，正如证明所需要的那样。□

### 2.1.3 总结

以下定理总结了 ArrayStack 的性能：

> **定理 2.1** ArrayStack 实现了 List 接口。忽略 `resize()` 调用的代价，ArrayStack 支持以下操作：
> - `get(i)` 和 `set(i,x)` 每次操作为 O(1) 时间；
> - `add(i,x)` 和 `remove(i)` 每次操作为 O(n - i) 时间。
>
> 此外，从一个空的 ArrayStack 开始，执行任意序列的 `add(i,x)` 和 `remove(i)` 操作，所有 `resize()` 调用花费的总时间为 O(m)。

ArrayStack 是实现 Stack 的一种高效方式。特别地，我们可以将 `push(x)` 实现为 `add(n,x)`，将 `pop()` 实现为 `remove(n-1)`，在这种情况下，这些操作将以摊销 O(1) 时间运行。

---

## 2.2 FastArrayStack：优化的 ArrayStack

ArrayStack 完成的大部分工作涉及数据的移位（通过 `add(i,x)` 和 `remove(i)`）和复制（通过 `resize()`）。在朴素的实现中，这将使用 `for` 循环来完成。事实证明，许多编程环境具有特定的函数，这些函数在复制和移动数据块方面非常高效。在 C 编程语言中，有 `memcpy()` 和 `memmove()` 函数。在 C++ 语言中，有 `std::copy` 算法。在 Java 中，有 `System.arraycopy()` 方法。

这些函数通常是高度优化的，甚至可能使用特殊的机器指令，这些指令可以比使用 `for` 循环更快地完成复制。虽然使用这些函数不会渐近地减少运行时间，但它仍然是一个值得的优化。

在我们的 C++ 和 Java 实现中，使用快速数组复制函数根据执行的操作类型，实现了 2 到 3 倍的加速。你的效果可能会有所不同。

---

## 2.3 ArrayQueue：基于数组的队列

在本节中，我们介绍 ArrayQueue 数据结构，它实现了 FIFO（先进先出）队列；元素从队列中移除（使用 `remove()` 操作）的顺序与它们被添加（使用 `add(x)` 操作）的顺序相同。

注意，ArrayStack 不是实现 FIFO 队列的好选择。原因是我们必须选择列表的一端来添加元素，然后从另一端移除元素。这两个操作中的一个必须在列表的头部操作，这涉及使用 i = 0 调用 `add(i,x)` 或 `remove(i)`。这给出了与 n 成正比的运行时间。

为了获得基于数组的高效队列实现，我们首先注意到如果有一个无限数组 `a`，问题会很容易。我们可以维护一个索引 `j` 来跟踪下一个要移除的元素，以及一个整数 `n` 来计数队列中的元素数量。队列元素将始终存储在：

```
a[j], a[j+1], ..., a[j+n-1]
```

最初，`j` 和 `n` 都设置为 0。要添加一个元素，我们将其放置在 `a[j+n]` 并递增 `n`。要移除一个元素，我们从 `a[j]` 移除它，递增 `j`，并递减 `n`。

当然，这个解决方案的问题在于它需要一个无限数组。ArrayQueue 通过使用有限数组 `a` 和模算术（modular arithmetic）来模拟这一点。这种算术类似于我们谈论一天中的时间时使用的算术。例如，10:00 加五小时得到 3:00。形式上，我们说：

```
15 ≡ 3 (mod 12)
```

我们读取这个等式的后一部分为"15 与 3 模 12 同余"。我们也可以将 `mod` 视为一个二元运算符，因此：

```
15 mod 12 = 3
```

更一般地，对于整数 a 和正整数 m，`a mod m` 是唯一的整数 r ∈ {0,...,m-1}，使得 a = qm + r 对于某个整数 q。不太正式地说，值 `a mod m` 是我们用 m 除 a 时得到的余数。在许多编程语言中，包括 C、C++ 和 Java，模运算符使用 `%` 符号表示。

模算术对于模拟无限数组很有用，因为 `j mod a.length` 总是给出范围 {0,...,a.length-1} 内的值。使用模算术，我们可以将队列元素存储在数组位置：

```
a[j mod a.length], a[(j+1) mod a.length], ..., a[(j+n-1) mod a.length]
```

这将数组 `a` 视为一个**环形数组**（circular array），其中大于 `a.length - 1` 的数组索引"绕回"到数组的开头。

剩下的唯一事情是确保 ArrayQueue 中的元素数量不超过 `a` 的大小。

ArrayQueue 上的一系列 `add(x)` 和 `remove()` 操作如图 2.2 所示。要实现 `add(x)`，我们首先检查 `a` 是否已满，如有必要，调用 `resize()` 来增加 `a` 的大小。接下来，我们将 `x` 存储在 `a[(j+n) mod a.length]` 中并递增 `n`。

> **图 2.2：** 在 ArrayQueue 上的一系列 `add(x)` 和 `remove()` 操作。箭头表示被复制的元素。导致调用 `resize()` 的操作用星号标记。

要实现 `remove()`，我们首先存储 `a[j]` 以便稍后返回。接下来，我们递减 `n` 并通过设置 `j = (j+1) mod a.length` 来递增 `j`（模 `a.length`）。最后，我们返回存储的 `a[j]` 值。如有必要，我们可以调用 `resize()` 来减小 `a` 的大小。

最后，`resize()` 操作与 ArrayStack 的 `resize()` 操作非常相似。它分配一个新的数组 `b`，大小为 `max(1, 2n)`，并将以下内容复制：

```
a[j], a[(j+1) mod a.length], ..., a[(j+n-1) mod a.length]
```

到

```
b[0], b[1], ..., b[n-1]
```

并设置 `a = b`，`j = 0`。

### 2.3.1 总结

以下定理总结了 ArrayQueue 数据结构的性能：

> **定理 2.2** ArrayQueue 实现了（FIFO）Queue 接口。忽略 `resize()` 调用的代价，ArrayQueue 支持 `add(x)` 和 `remove()` 操作，每次操作为 O(1) 时间。此外，从一个空的 ArrayQueue 开始，任意序列的 `add(x)` 和 `remove()` 操作导致所有 `resize()` 调用花费的总时间为 O(m)。

---

## 2.4 ArrayDeque：使用数组的快速双端队列操作

上一节的 ArrayQueue 是一种用于表示序列的数据结构，它允许我们高效地向序列的一端添加元素并从另一端移除元素。ArrayDeque 数据结构允许在两端进行高效的添加和移除操作。这种结构通过使用与 ArrayQueue 相同的环形数组技术来实现 List 接口。

ArrayDeque 上的 `get(i)` 和 `set(i,x)` 操作很简单。它们获取或设置数组元素 `a[(j+i) mod a.length]`。

`add(i,x)` 的实现更有趣一些。像往常一样，我们首先检查 `a` 是否已满，如有必要，调用 `resize()` 来调整 `a` 的大小。记住，我们希望当 i 很小（接近 0）或 i 很大（接近 n）时此操作快速。因此，我们检查 `i < n/2`。如果是，我们将元素 `a[j],...,a[(j+i-1) mod a.length]` 向左移动一个位置。否则（`i ≥ n/2`），我们将元素 `a[(j+i) mod a.length],...,a[(j+n-1) mod a.length]` 向右移动一个位置。参见图 2.3，了解 ArrayDeque 上的 `add(i,x)` 和 `remove(i)` 操作图示。

> **图 2.3：** 在 ArrayDeque 上的一系列 `add(i,x)` 和 `remove(i)` 操作。箭头表示被复制的元素。

通过以这种方式进行移位，我们保证 `add(i,x)` 永远不必移动超过 `min{i, n-i}` 个元素。因此，`add(i,x)` 操作的运行时间（忽略 `resize()` 操作的代价）为 O(min{i, n-i})。

`remove(i)` 操作的实现类似。它根据 `i < n/2` 将元素 `a[(j+i+1) mod a.length],...,a[(j+n-1) mod a.length]` 向左移动一个位置，或者将元素 `a[j],...,a[(j+i-1) mod a.length]` 向右移动一个位置。同样，这意味着 `remove(i)` 在移位元素上花费的时间永远不会超过 O(min{i, n-i})。

### 2.4.1 总结

以下定理总结了 ArrayDeque 数据结构的性能：

> **定理 2.3** ArrayDeque 实现了 List 接口。忽略 `resize()` 调用的代价，ArrayDeque 支持以下操作：
> - `get(i)` 和 `set(i,x)` 每次操作为 O(1) 时间；
> - `add(i,x)` 和 `remove(i)` 每次操作为 O(min{i, n-i}) 时间。
>
> 此外，从一个空的 ArrayDeque 开始，执行任意序列的 `add(i,x)` 和 `remove(i)` 操作，所有 `resize()` 调用花费的总时间为 O(m)。

---

## 2.5 DualArrayDeque：用两个栈构建双端队列

接下来，我们介绍一种数据结构——DualArrayDeque，它通过使用两个 ArrayStack 实现了与 ArrayDeque 相同的性能界限。虽然 DualArrayDeque 的渐近性能并不比 ArrayDeque 好，但它仍然值得学习，因为它提供了一个很好的例子，说明如何通过组合两个更简单的数据结构来构建复杂的数据结构。

DualArrayDeque 使用两个 ArrayStack 来表示一个列表。回想一下，ArrayStack 在操作修改靠近末尾的元素时速度很快。DualArrayDeque 将两个 ArrayStack（称为 `front` 和 `back`）背对背放置，以便在两端都能快速操作。

DualArrayDeque 不显式存储其包含的元素数量 n。它不需要，因为它包含 `front.size() + back.size()` 个元素。然而，在分析 DualArrayDeque 时，我们仍将使用 n 来表示其包含的元素数量。

`front` ArrayStack 存储索引在 {0,...,front.size()-1} 范围内的列表元素，但以逆序存储。`back` ArrayStack 按正常顺序存储索引在 {front.size(),...,n-1} 范围内的列表元素。这样，`get(i)` 和 `set(i,x)` 转换为对 `front` 或 `back` 上适当的 `get()` 或 `set()` 调用，每次操作需要 O(1) 时间。

注意，如果索引 i < front.size()，那么它对应于 `front` 中位置 `front.size()-i-1` 处的元素，因为 `front` 的元素以逆序存储。

在 DualArrayDeque 上添加和删除元素的操作如图 2.4 所示。`add(i,x)` 操作适当地操作 `front` 或 `back`：

> **图 2.4：** 在 DualArrayDeque 上的一系列 `add(i,x)` 和 `remove(i)` 操作。箭头表示被复制的元素。导致通过 `balance()` 重新平衡的操作用星号标记。

`add(i,x)` 方法通过调用 `balance()` 方法来执行两个 ArrayStack `front` 和 `back` 的重新平衡。`balance()` 的实现如下所述，但当前只要知道 `balance()` 确保除非 n < 2，否则 `front.size()` 和 `back.size()` 的差异不超过 3 倍就足够了。特别地，`3 * front.size() ≥ back.size()` 且 `3 * back.size() ≥ front.size()`。

接下来我们分析 `add(i,x)` 的代价，忽略 `balance()` 调用的代价。如果 i < front.size()，那么 `add(i,x)` 通过调用 `front.add(front.size()-i, x)` 来实现。由于 `front` 是一个 ArrayStack，这的代价是：

```
O(front.size() - (front.size() - i)) = O(i)          (2.1)
```

另一方面，如果 i ≥ front.size()，那么 `add(i,x)` 实现为 `back.add(i - front.size(), x)`。这的代价是：

```
O(back.size() - (i - front.size())) = O(n - i)        (2.2)
```

注意，第一种情况 (2.1) 发生在 i < front.size() 时。第二种情况 (2.2) 发生在 i ≥ front.size() 时。当 i = front.size() 时，我们无法确定操作是影响 `front` 还是 `back`，但在任何一种情况下，操作都需要 O(min{i, n-i}) 时间，因为 `front.size() ≥ n/3` 且 `back.size() ≥ n/3`。总结情况，我们有：

```
add(i,x) 的运行时间 = O(min{i, n-i})
```

因此，如果忽略 `balance()` 调用的代价，`add(i,x)` 的运行时间是 O(min{i, n-i})。

`remove(i)` 操作及其分析与 `add(i,x)` 操作及分析类似。

### 2.5.1 平衡

最后，我们讨论由 `add(i,x)` 和 `remove(i)` 执行的 `balance()` 操作。该操作确保 `front` 或 `back` 都不会变得太大（或太小）。它确保除非元素少于两个，否则 `front` 和 `back` 各自至少包含 n/4 个元素。如果不是这种情况，则它在它们之间移动元素，使得 `front` 和 `back` 分别恰好包含 n/2 个元素和 n - n/2 个元素。

这里几乎不需要分析。如果 `balance()` 操作执行重新平衡，那么它移动 O(n) 个元素，这需要 O(n) 时间。这很糟糕，因为 `balance()` 在每次调用 `add(i,x)` 和 `remove(i)` 时都被调用。然而，以下引理表明，平均而言，`balance()` 每次操作只花费常数时间。

> **引理 2.2** 如果创建了一个空的 DualArrayDeque，并执行了任意序列的 m 次 `add(i,x)` 和 `remove(i)` 调用，那么所有 `balance()` 调用花费的总时间为 O(m)。

**证明：** 我们将证明，如果 `balance()` 被迫移动元素，那么自上次 `balance()` 移动任何元素以来，`add(i,x)` 和 `remove(i)` 操作的数量至少为 n/3。如同引理 2.1 的证明，这足以证明 `balance()` 花费的总时间为 O(m)。

我们将使用一种称为**势能法**（potential method）的技术来进行分析。定义 DualArrayDeque 的势能 Φ 为 `front` 和 `back` 的大小之差：

```
Φ = |front.size() - back.size()|
```

关于这个势能的有趣之处在于，不进行任何平衡的 `add(i,x)` 或 `remove(i)` 调用最多可以将势能增加 1。

观察到，在移动元素的 `balance()` 调用之后，势能 Φ 最多为 1，因为：

```
|front.size() - back.size()| ≤ 1
```

考虑在移动元素的 `balance()` 调用之前的情况，并且假设不失一般性地，`balance()` 正在移动元素是因为 `front.size() < back.size()`。注意，在这种情况下：

```
front.size() < back.size()
       n/4 > front.size()
```

此外，此时势能为：

```
Φ = back.size() - front.size()
  > back.size() - n/4
  ≥ back.size() - (3/4) * back.size()    [因为 n ≤ 2 * back.size()]
  = back.size()/4
  ≥ n/8
```

因此，自上次 `balance()` 移动元素以来，`add(i,x)` 或 `remove(i)` 的调用次数至少为 Φ ≥ n/8 > n/3。这就完成了证明。□

### 2.5.2 总结

以下定理总结了 DualArrayDeque 的性质：

> **定理 2.4** DualArrayDeque 实现了 List 接口。忽略 `resize()` 和 `balance()` 调用的代价，DualArrayDeque 支持以下操作：
> - `get(i)` 和 `set(i,x)` 每次操作为 O(1) 时间；
> - `add(i,x)` 和 `remove(i)` 每次操作为 O(min{i, n-i}) 时间。
>
> 此外，从一个空的 DualArrayDeque 开始，任意序列的 `add(i,x)` 和 `remove(i)` 操作导致所有 `resize()` 和 `balance()` 调用花费的总时间为 O(m)。

---

## 2.6 RootishArrayStack：空间高效的数组栈

本章中所有先前数据结构的一个缺点是，由于它们将数据存储在一个或两个数组中，并且避免过于频繁地调整这些数组的大小，因此数组经常未满。例如，在 ArrayStack 上的 `remove(i)` 操作之后，后备数组 `a` 只有一半满。更糟糕的是，有时 `a` 中只有三分之一包含数据。

在本节中，我们讨论 RootishArrayStack 数据结构，它解决了空间浪费的问题。RootishArrayStack 使用 O(√n) 个数组存储 n 个元素。在这些数组中，最多有 O(√n) 个数组位置在任何时候未被使用。所有剩余的数组位置都被用来存储数据。因此，这些数据结构在存储 n 个元素时最多浪费 O(√n) 空间。

RootishArrayStack 将其元素存储在一个称为**块**（blocks）的数组列表 `blocks` 中，块的编号为 0,1,...,b-1。参见图 2.5。块 b 包含 b+1 个元素。因此，所有 b 个块总共包含：

```
1 + 2 + 3 + ... + b = b(b+1)/2
```

个元素。上面的公式可以通过如图 2.6 所示获得。

> **图 2.5：** 在 RootishArrayStack 上的一系列 `add(i,x)` 和 `remove(i)` 操作。箭头表示被复制的元素。

> **图 2.6：** 白色方块的数量是 1 + 2 + ... + b = b(b+1)/2。阴影方块的数量相同。白色和阴影方块一起构成一个由 b*(b+1) 个方块组成的矩形。

正如我们所期望的，列表的元素按顺序分布在块内。索引为 0 的列表元素存储在块 0 中，列表索引 1 和 2 存储在块 1 中，列表索引 3、4 和 5 存储在块 2 中，依此类推。我们要解决的主要问题是，给定一个索引 i，确定哪个块包含 i 以及在该块内对应于 i 的索引。

确定 i 在其块内的索引 j 很简单。如果索引 i 在块 b 中，那么块 0,...,b-1 中的元素数量是 b(b+1)/2。因此，i 存储在位置：

```
j = i - b(b+1)/2
```

在块 b 内。更具挑战性的是确定 b 的值。索引小于或等于 i 的元素数量是 i+1。另一方面，块 0,...,b 中的元素数量是 (b+1)(b+2)/2。因此，b 是满足以下条件的最小整数：

```
(b+1)(b+2)/2 ≥ i+1
```

我们可以将不等式改写为：

```
b^2 + 3b - 2i ≥ 0
```

对应的二次方程 b^2 + 3b - 2i = 0 有两个解：b = (-3 + √(9 + 8i))/2 和 b = (-3 - √(9 + 8i))/2。第二个解在我们的应用中没有意义，因为它总是给出负值。因此，我们得到解 b = (-3 + √(9 + 8i))/2。一般来说，这个解不是整数，但回到我们的不等式，我们想要满足 (b+1)(b+2)/2 ≥ i+1 的最小整数 b。这很简单：

```
b = ⌈(-3 + √(9 + 8i))/2⌉
```

解决了这个问题后，`get(i)` 和 `set(i,x)` 方法就很直接了。我们首先计算适当的块 b 和块内的适当索引 j，然后执行适当的操作：

```python
def get(self, i):
    b = self._block_index(i)
    j = i - b * (b + 1) // 2
    return self.blocks.get(b)[j]

def set(self, i, x):
    b = self._block_index(i)
    j = i - b * (b + 1) // 2
    y = self.blocks.get(b)[j]
    self.blocks.get(b)[j] = x
    return y
```

如果我们使用本章中的任何数据结构来表示 `blocks` 列表，那么 `get(i)` 和 `set(i,x)` 都将在常数时间内运行。

`add(i,x)` 方法现在看起来很熟悉。我们首先检查数据结构是否已满，通过检查块数 b 是否满足 `b(b+1)/2 < n + 1`。如果是，我们调用 `grow()` 来添加另一个块。完成此操作后，我们将索引为 i,...,n-1 的元素向右移动一个位置，为索引 i 的新元素腾出空间：

```python
def add(self, i, x):
    n = self.size()
    b = len(self.blocks)
    if b * (b + 1) // 2 < n + 1:
        self._grow()
    n += 1
    for j in range(n - 1, i, -1):
        self.set(j, self.get(j - 1))
    self.set(i, x)
```

`grow()` 方法做了我们期望的事情。它添加一个新块：

```python
def _grow(self):
    b = len(self.blocks)
    self.blocks.append(new_array(b + 1))
```

忽略 `grow()` 操作的代价，`add(i,x)` 操作的代价由移位的代价主导，因此为 O(n - i)，就像 ArrayStack 一样。

`remove(i)` 操作类似于 `add(i,x)`。它将索引为 i+1,...,n-1 的元素向左移动一个位置，然后，如果有多于一个空块，它调用 `shrink()` 方法来移除所有未使用的块，只保留一个：

```python
def remove(self, i):
    n = self.size()
    for j in range(i, n - 1):
        self.set(j, self.get(j + 1))
    n -= 1
    b = len(self.blocks)
    if (b - 1) * b // 2 > n:
        self._shrink()
```

同样，忽略 `shrink()` 操作的代价，`remove(i)` 操作的代价由移位代价主导，因此为 O(n - i)。

### 2.6.1 增长和缩小的分析

上述对 `add(i,x)` 和 `remove(i)` 的分析没有考虑 `grow()` 和 `shrink()` 的代价。注意，与 `resize()` 操作不同，`grow()` 和 `shrink()` 不复制任何数据。它们只分配或释放一个大小为 r 的数组。在某些环境中，这只需要常数时间，而在其他环境中，可能需要与 r 成正比的时间。

我们注意到，在调用 `grow()` 或 `shrink()` 之后，情况是清晰的。最后一个块完全为空，而所有其他块都完全填满。另一次 `grow()` 或 `shrink()` 调用不会发生，直到至少 n/2 个元素被添加或移除。因此，即使 `grow()` 和 `shrink()` 需要 O(r) 时间，这个代价也可以在至少 O(n) 次 `add(i,x)` 或 `remove(i)` 操作上摊销，因此 `grow()` 和 `shrink()` 的摊销代价为每次操作 O(1)。

### 2.6.2 空间使用情况

接下来，我们分析 RootishArrayStack 使用的额外空间量。特别地，我们想要计算 RootishArrayStack 使用的任何不是当前用于保存列表元素的数组元素的空间。我们称所有这样的空间为**浪费的空间**（wasted space）。

`grow()` 和 `shrink()` 操作确保 RootishArrayStack 从不会有超过两个未完全填满的块。存储 n 个元素的 RootishArrayStack 使用的块数 b 因此满足：

```
b(b+1)/2 - (b+1) < n ≤ b(b+1)/2
```

再次使用二次方程，我们得到：

```
b < (3 + √(9 + 8n))/2 ≈ √(2n)
```

最后两个块的大小分别为 b 和 b-1，因此这两个块浪费的空间最多为 b + (b-1) = 2b - 2 ≤ 2√(2n)。如果我们将块存储在（例如）ArrayStack 中，那么存储这 O(√n) 个块的 List 浪费的空间量也是 O(√n)。存储 `n` 和其他记账信息所需的其他空间为 O(1)。因此，RootishArrayStack 中浪费的空间总量为 O(√n)。

接下来，我们论证对于任何从空开始并且可以一次支持添加一个元素的数据结构，这个空间使用是最优的。更精确地说，我们将证明，在添加 n 个项的某个时刻，数据结构正在浪费至少 Ω(√n) 的空间（尽管可能只是浪费了片刻）。

假设我们从空的数据结构开始，并一次添加 n 个项。在此过程结束时，所有 n 个项都存储在结构中，并分布在总共 r 个内存块中。如果 r < √n，那么数据结构必须使用 r 个指针（或引用）来跟踪这 r 个块，而这些指针就是浪费的空间。另一方面，如果 r ≥ √n，那么根据鸽巢原理（pigeonhole principle），某个块的大小必须至少为 n/r ≥ √n。考虑这个块首次被分配的时刻。在它被分配之后，这个块是空的，因此浪费了 Ω(√n) 的空间。因此，在插入 n 个元素的某个时刻，数据结构正在浪费 Ω(√n) 的空间。

### 2.6.3 总结

以下定理总结了我们对 RootishArrayStack 数据结构的讨论：

> **定理 2.5** RootishArrayStack 实现了 List 接口。忽略 `grow()` 和 `shrink()` 调用的代价，RootishArrayStack 支持以下操作：
> - `get(i)` 和 `set(i,x)` 每次操作为 O(1) 时间；
> - `add(i,x)` 和 `remove(i)` 每次操作为 O(n - i) 时间。
>
> 此外，从一个空的 RootishArrayStack 开始，任意序列的 `add(i,x)` 和 `remove(i)` 操作导致所有 `grow()` 和 `shrink()` 调用花费的总时间为 O(m)。
>
> 存储 n 个元素的 RootishArrayStack 使用的空间（以字（words）为单位测量）为 O(n + √n)。

---

## 2.7 讨论与练习

本章描述的大多数数据结构都是民间传说（folklore）。它们可以在 30 多年前的实现中找到。例如，栈（stacks）、队列（queues）和双端队列（deques）的实现——这些很容易推广到本文描述的 ArrayStack、ArrayQueue 和 ArrayDeque 结构——由 Knuth [46, Section 2.2.2] 讨论过。

Brodnik 等人 [13] 似乎是第一个描述 RootishArrayStack 并证明类似于第 2.6.2 节中那样 Ω(√n) 下界的人。他们还提出了一种不同的结构，该结构使用更复杂的块大小选择，以避免在 `_block_index` 方法中计算平方根。在他们的方案中，包含 i 的块是块 ⌊log2(i+1)⌋，这仅仅是在 i+1 的二进制表示中前导 1 位的索引。一些计算机体系结构提供了用于计算整数中前导 1 位索引的指令。

与 RootishArrayStack 相关的一个结构是 Goodrich 和 Kloss [35] 的两层分级向量（tiered-vector）。该结构支持 `get(i)` 和 `set(i,x)` 操作在常数时间内，以及 `add(i,x)` 和 `remove(i)` 在 O(√n) 时间内。这些运行时间类似于通过练习 2.10 中讨论的 RootishArrayStack 的更仔细实现可以实现的。

---

### 练习

**练习 2.1** List 方法 `addAll(i, c)` 将集合 `c` 中的所有元素插入到列表中的位置 i。（`add(i,x)` 方法是 `addAll` 的一个特例，其中 `c` 只包含一个元素。）解释为什么对于本章中的数据结构，通过重复调用 `add(i,x)` 来实现 `addAll` 效率不高。设计并实现一个更高效的实现。

**练习 2.2** 设计并实现一个 RandomQueue。这是 Queue 接口的一个实现，其中 `remove()` 操作移除一个在所有当前队列中的元素中均匀随机选择的元素。（将 RandomQueue 想象成一个袋子，我们可以向其中添加元素，或者伸手进去盲目地移除某个随机元素。）RandomQueue 中的 `add(x)` 和 `remove()` 操作应该每次以常数时间运行。

**练习 2.3** 设计并实现一个 Treque（三端队列，triple-ended queue）。这是一个 List 实现，其中 `get(i)` 和 `set(i,x)` 以常数时间运行，而 `add(i,x)` 和 `remove(i)` 以 O(min{i, n-i, |n/2 - i|}) 时间运行。换句话说，如果修改靠近任一端或靠近列表中间，则修改是快速的。

**练习 2.4** 实现一个方法 `rotate(a,r)`，它"旋转"数组 `a`，使得 `a[i]` 移动到 `a[(i+r) mod a.length]`，对于所有 i ∈ {0,...,a.length-1}。

**练习 2.5** 实现一个方法 `rotate(r)`，它"旋转"一个 List，使得列表项 i 变为列表项 (i+r) mod n。当在 ArrayDeque 或 DualArrayDeque 上运行时，`rotate(r)` 应该以 O(1 + min{r, n-r}) 时间运行。

**练习 2.6** 此练习在伪代码版中被省略。

**练习 2.7** 修改 ArrayDeque 实现，使其不再使用 `%` 运算符（在某些系统上代价高昂）。相反，它应利用以下事实：如果 `a.length` 是 2 的幂，那么 `i mod a.length = i & (a.length - 1)`。（这里，`&` 是按位与运算符。）

**练习 2.8** 设计并实现一个 ArrayDeque 的变体，它根本不使用任何模算术。相反，所有数据都按顺序连续地坐在一个数组内的块中。当数据超出数组的开头或结尾时，执行一个修改后的 `resize()` 操作。所有操作的摊销代价应与 ArrayDeque 相同。提示：使其工作的关键在于如何实现 `resize()` 操作。你希望 `resize()` 将数据结构置于一个状态，使得数据在至少 n/2 次操作执行之前不会跑出任一端。测试你的实现与 ArrayDeque 的性能。优化你的实现（通过使用数组复制），看看能否使其胜过 ArrayDeque 实现。

**练习 2.9** 设计并实现一个 RootishArrayStack 的版本，它只有 O(√n) 的浪费空间，但可以执行 `get(i)` 和 `set(i,x)` 在 O(1) 时间，`add(i,x)` 和 `remove(i)` 在 O(n - i) 时间。

**练习 2.10** 设计并实现一个 RootishArrayStack 的版本，它只有 O(√n) 的浪费空间，但可以执行 `get(i)` 和 `set(i,x)` 在 O(1) 时间，`add(i,x)` 和 `remove(i)` 在 O(√n) 时间。（关于如何做到这一点的想法，请参见第 3.3 节。）

**练习 2.11** 设计并实现一个 RootishArrayStack 的版本，它只有 O(n^(1/3)) 的浪费空间，但可以执行 `get(i)` 和 `set(i,x)` 在 O(1) 时间，`add(i,x)` 和 `remove(i)` 在 O(n^(1/3)) 时间。（关于如何实现这一点的想法，请参见第 3.3 节。）

**练习 2.12** 设计并实现一个 CubishArrayStack。这个三层结构使用 O(n^(1/3)) 的浪费空间实现 List 接口。在这个结构中，`get(i)` 和 `set(i,x)` 花费常数时间；而 `add(i,x)` 和 `remove(i)` 花费摊销 O(n^(1/3)) 时间。
