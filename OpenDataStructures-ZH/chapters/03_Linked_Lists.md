# 第3章 链表

在本章中，我们继续研究 List（列表）接口的实现，这次使用基于指针（pointer）的数据结构而非数组。本章中的结构由包含列表元素的节点（node）组成。通过引用（指针），这些节点被链接成一个序列。我们首先研究单向链表（singly-linked list），它可以在常数时间内实现 Stack（栈）和（FIFO）Queue（队列）操作，然后研究双向链表（doubly-linked list），它可以在常数时间内实现 Deque（双端队列）操作。

与基于数组的 List 接口实现相比，链表有其优点和缺点。主要的缺点是我们失去了在常数时间内通过 `get(i)` 或 `set(i,x)` 访问任意元素的能力。相反，我们必须逐个元素遍历列表，直到到达第 i 个元素。主要的优点是它们更加动态：给定对任意列表节点 u 的引用，我们可以在常数时间内删除 u 或在 u 附近插入一个节点。无论 u 在列表中的什么位置都是如此。

## 3.1 SLList：单向链表

SLList（单向链表）是一个节点（Node）序列。每个节点 u 存储一个数据值 `u.x` 和一个指向序列中下一个节点的引用 `u.next`。对于序列中的最后一个节点 w，有 `w.next = nil`。

为了提高效率，SLList 使用变量 `head` 和 `tail` 来跟踪序列中的第一个和最后一个节点，以及一个整数 `n` 来跟踪序列的长度：

```python
class SLList:
    class Node:
        def __init__(self, x):
            self.x = x
            self.next = None

    def __init__(self):
        self.head = None
        self.tail = None
        self.n = 0
```

图 3.1 展示了 SLList 上的一系列 Stack 和 Queue 操作。

SLList 可以高效地实现 Stack 操作 `push()` 和 `pop()`，通过在序列的头部添加和删除元素。`push()` 操作简单地创建一个带有数据值 x 的新节点 u，将 `u.next` 设置为旧的头节点，并使 u 成为新的头节点。最后，它增加 n，因为 SLList 的大小增加了 1：

```python
    def push(self, x):
        u = Node(x)
        u.next = self.head
        self.head = u
        if self.n == 0:
            self.tail = u
        self.n += 1
        return x
```

`pop()` 操作在检查 SLList 非空后，通过设置 `head = head.next` 并递减 n 来移除头节点。当移除最后一个元素时会出现特殊情况，此时 `tail` 被设置为 `nil`：

```python
    def pop(self):
        if self.n == 0:
            raise IndexError('pop from empty list')
        x = self.head.x
        self.head = self.head.next
        self.n -= 1
        if self.n == 0:
            self.tail = None
        return x
```

显然，`push(x)` 和 `pop()` 操作都在 O(1) 时间内运行。

### 3.1.1 队列操作

SLList 也可以在常数时间内实现 FIFO 队列操作 `add(x)` 和 `remove()`。移除操作在列表头部进行，与 `pop()` 操作相同：

```python
    def remove(self):
        return self.pop()
```

另一方面，添加操作在列表尾部进行。在大多数情况下，通过设置 `tail.next = u` 来完成，其中 u 是包含 x 的新创建的节点。然而，当 `n == 0` 时会出现特殊情况，此时 `tail == head == nil`。在这种情况下，`tail` 和 `head` 都被设置为 u。

```python
    def add(self, x):
        u = Node(x)
        if self.n == 0:
            self.head = u
        else:
            self.tail.next = u
        self.tail = u
        self.n += 1
        return True
```

显然，`add(x)` 和 `remove()` 都花费常数时间。

### 3.1.2 总结

以下定理总结了 SLList 的性能：

**定理 3.1** *SLList 实现了 Stack 和（FIFO）Queue 接口。`push(x)`、`pop()`、`add(x)` 和 `remove()` 操作每次运行时间为 O(1)。*

SLList 几乎实现了完整的 Deque 操作集。唯一缺少的操作是从 SLList 尾部删除。从尾部删除很困难，因为它需要更新 `tail` 的值，使其指向 SLList 中位于 `tail` 之前的节点 w，即满足 `w.next == tail` 的节点 w。不幸的是，到达 w 的唯一方法是从 `head` 开始遍历 SLList 并走 n-2 步。

## 3.2 DLList：双向链表

DLList（双向链表）与 SLList 非常相似，只是 DLList 中的每个节点 u 既有指向其后继节点 `u.next` 的引用，也有指向其前驱节点 `u.prev` 的引用。

在实现 SLList 时，我们发现总是有几个特殊情况需要处理。例如，从 SLList 中删除最后一个元素或向空 SLList 中添加元素时，需要小心确保 `head` 和 `tail` 被正确更新。在 DLList 中，这些特殊情况的数目大大增加。处理 DLList 中所有这些特殊情况最简洁的方法可能是引入一个哨兵（dummy）节点。这是一个不包含任何数据的节点，但充当占位符，使得没有特殊节点；每个节点都有 `next` 和 `prev`，`dummy` 充当列表中最后一个节点的后继和第一个节点的前驱。通过这种方式，列表中的节点被（双向）链接成一个循环，如图 3.2 所示。

```python
class DLList:
    class Node:
        def __init__(self, x):
            self.x = x
            self.next = None
            self.prev = None

    def __init__(self):
        self.dummy = Node(None)
        self.dummy.next = self.dummy
        self.dummy.prev = self.dummy
        self.n = 0
```

在 DLList 中查找特定索引的节点很容易；我们可以从列表头部（`dummy.next`）开始向前查找，或者从列表尾部（`dummy.prev`）开始向后查找。这允许我们在 O(1 + min{i, n-i}) 时间内到达第 i 个节点：

```python
    def get_node(self, i):
        if i < 0 or i > self.n - 1:
            raise IndexError
        if i < self.n // 2:
            p = self.dummy.next
            for _ in range(i):
                p = p.next
        else:
            p = self.dummy
            for _ in range(self.n - i):
                p = p.prev
        return p
```

`get(i)` 和 `set(i, x)` 操作现在也很容易。我们首先找到第 i 个节点，然后获取或设置其 x 值：

```python
    def get(self, i):
        return self.get_node(i).x

    def set(self, i, x):
        u = self.get_node(i)
        y = u.x
        u.x = x
        return y
```

这些操作的运行时间由查找第 i 个节点所需的时间决定，因此为 O(1 + min{i, n-i})。

### 3.2.1 添加和删除

如果我们有对 DLList 中节点 w 的引用，并且想在 w 之前插入一个节点 u，那么只需要设置 `u.next = w`、`u.prev = w.prev`，然后调整 `u.prev.next` 和 `u.next.prev`。（见图 3.3。）得益于哨兵节点，无需担心 `w.prev` 或 `w.next` 不存在。

```python
    def add_before(self, w, x):
        u = Node(x)
        u.prev = w.prev
        u.next = w
        u.next.prev = u
        u.prev.next = u
        self.n += 1
        return u
```

现在，列表操作 `add(i, x)` 的实现非常简单。我们找到 DLList 中的第 i 个节点，并在它之前插入一个包含 x 的新节点 u：

```python
    def add(self, i, x):
        self.add_before(self.get_node(i), x)
```

`add(i, x)` 运行时间的非常数部分只是查找第 i 个节点（使用 `get_node(i)`）所需的时间。因此，`add(i, x)` 在 O(1 + min{i, n-i}) 时间内运行。

从 DLList 中删除节点 w 很容易。我们只需要调整 `w.next` 和 `w.prev` 处的指针，使它们跳过 w。同样，使用哨兵节点消除了考虑任何特殊情况的必要：

```python
    def remove_node(self, w):
        w.prev.next = w.next
        w.next.prev = w.prev
        self.n -= 1

    def remove(self, i):
        self.remove_node(self.get_node(i))
```

同样，这个操作唯一昂贵的部分是使用 `get_node(i)` 查找第 i 个节点，因此 `remove(i)` 在 O(1 + min{i, n-i}) 时间内运行。

### 3.2.2 总结

以下定理总结了 DLList 的性能：

**定理 3.2** *DLList 实现了 List 接口。在此实现中，`get(i)`、`set(i,x)`、`add(i,x)` 和 `remove(i)` 操作每次运行时间为 O(1 + min{i, n-i})。*

值得注意的是，如果忽略 `get_node(i)` 操作的成本，那么 DLList 上的所有操作都花费常数时间。因此，DLList 操作中唯一昂贵的部分就是查找相关节点。一旦我们有了相关节点，添加、删除或访问该节点的数据只需要常数时间。

这与第 2 章中基于数组的 List 实现形成鲜明对比；在这些实现中，可以在常数时间内找到相关的数组元素。然而，添加或删除需要移动数组中的元素，通常需要非常数时间。

出于这个原因，链表结构非常适合可以通过外部方式获得对列表节点引用的应用场景。

## 3.3 SEList：空间高效链表

链表的一个缺点（除了访问列表中深层元素所需的时间外）是它们的空间使用。DLList 中的每个节点需要额外两个指向列表中下一个和前一个节点的引用。Node 中的两个字段用于维护列表，只有一个字段用于存储数据！

SEList（空间高效列表）通过一个简单的想法减少了这种空间浪费：我们不在 DLList 中存储单个元素，而是存储一个包含多个元素的块（数组）。更准确地说，SEList 由一个块大小 b 参数化。SEList 中的每个节点存储一个可以容纳最多 b+1 个元素的块。

出于稍后将明确的原因，如果我们能在每个块上进行 Deque 操作会很有帮助。为此选择的数据结构是 BDeque（有界双端队列），它派生自第 2.4 节中描述的 ArrayDeque 结构。BDeque 与 ArrayDeque 在一个小方面有所不同：当创建一个新的 BDeque 时，后备数组 a 的大小固定为 b+1，并且从不增长或收缩。BDeque 的重要特性是它允许在常数时间内在前端或后端添加或删除元素。这在元素从一个块移动到另一个块时将非常有用。

SEList 只是一个块的双向链表。除了 `next` 和 `prev` 指针外，SEList 中的每个节点 u 还包含一个 BDeque `u.d`。

```python
class SEList:
    class BDeque:
        def __init__(self, b):
            self.a = [None] * (b + 1)
            self.j = 0
            self.n = 0

        def add_last(self, x):
            self.a[(self.j + self.n) % len(self.a)] = x
            self.n += 1

        def remove_last(self):
            self.n -= 1
            return self.a[(self.j + self.n) % len(self.a)]

        def add_first(self, x):
            self.j = (self.j - 1) % len(self.a)
            self.a[self.j] = x
            self.n += 1

        def remove_first(self):
            x = self.a[self.j]
            self.j = (self.j + 1) % len(self.a)
            self.n -= 1
            return x

        def size(self):
            return self.n

        def get(self, i):
            return self.a[(self.j + i) % len(self.a)]

        def set(self, i, x):
            y = self.a[(self.j + i) % len(self.a)]
            self.a[(self.j + i) % len(self.a)] = x
            return y

    class Node:
        def __init__(self, b):
            self.d = SEList.BDeque(b)
            self.prev = None
            self.next = None

    def __init__(self, b):
        self.b = b
        self.dummy = SEList.Node(b)
        self.dummy.next = self.dummy
        self.dummy.prev = self.dummy
        self.n = 0
```

### 3.3.1 空间需求

SEList 对块中元素的数量施加了非常严格的条件：除非一个块是最后一个块，否则该块包含至少 b-1 个和至多 b+1 个元素。这意味着，如果一个 SEList 包含 n 个元素，那么它最多有

n/(b-1) + 1 = O(n/b)

个块。每个块的 BDeque 包含一个长度为 b+1 的数组，但对于除最后一个块外的每个块，该数组中最多浪费常数量的空间。一个块使用的其余内存也是常数。这意味着 SEList 中浪费的空间只有 O(b + n/b)。通过选择在 √n 的常数因子范围内的 b 值，我们可以使 SEList 的空间开销接近第 2.6.2 节中给出的 √n 下界。

### 3.3.2 查找元素

SEList 面临的第一个挑战是找到给定索引 i 的列表项。请注意，元素的位置由两部分组成：

1. 包含具有索引 i 的元素的块所在的节点 u；以及
2. 该元素在其块内的索引 j。

要找到包含特定元素的块，我们采用与 DLList 中相同的方式。我们可以从列表的前端开始向前遍历，或者从列表的后端开始向后遍历，直到到达我们想要的节点。唯一的区别是，每次从一个节点移动到下一个节点时，我们跳过一整块元素。

```python
    def get_location(self, i):
        if i < 0 or i > self.n - 1:
            raise IndexError
        if i < self.n // 2:
            p = self.dummy.next
            idx = 0
            while idx + p.d.size() <= i:
                idx += p.d.size()
                p = p.next
            return (p, i - idx)
        else:
            p = self.dummy
            idx = self.n
            while idx - p.d.size() > i:
                p = p.prev
                idx -= p.d.size()
            return (p, i - idx + p.d.size())
```

请记住，除最多一个块外，每个块至少包含 b-1 个元素，因此搜索中的每一步让我们接近我们要找的元素 b-1 个位置。如果我们向前搜索，这意味着我们在 O(1 + i/b) 步后到达我们想要的节点。如果我们向后搜索，那么我们在 O(1 + (n-i)/b) 步后到达我们想要的节点。算法根据 i 的值取这两个量中较小的一个，因此定位索引为 i 的元素的时间为 O(1 + min{i, n-i}/b)。

一旦我们知道如何定位索引为 i 的元素，`get(i)` 和 `set(i,x)` 操作就转化为在正确的块中获取或设置特定索引：

```python
    def get(self, i):
        u, j = self.get_location(i)
        return u.d.get(j)

    def set(self, i, x):
        u, j = self.get_location(i)
        return u.d.set(j, x)
```

这些操作的运行时间由定位元素所需的时间决定，因此它们也在 O(1 + min{i, n-i}/b) 时间内运行。

### 3.3.3 添加元素

向 SEList 添加元素稍微复杂一些。在考虑一般情况之前，我们先考虑较简单的操作 `add(x)`，其中 x 被添加到列表的末尾。如果最后一个块已满（或者因为还没有块而不存在），那么我们首先分配一个新块并将其附加到块列表的末尾。现在，我们确保最后一个块存在且未满，然后将 x 追加到最后一个块。

```python
    def append(self, x):
        last = self.dummy.prev
        if last == self.dummy or last.d.size() == self.b + 1:
            last = self.add_before(self.dummy, self.b)
        last.d.add_last(x)
        self.n += 1

    def add(self, x):
        self.append(x)
        return True
```

当使用 `add(i,x)` 向列表内部添加时，事情变得更加复杂。我们首先定位 i 以获取其块包含第 i 个列表项的节点 u。问题在于，我们想将 x 插入到 u 的块中，但必须为 u 的块已经包含 b+1 个元素（即已满且没有空间容纳 x）的情况做准备。

设 u₀, u₁, u₂, ... 表示 u, u.next, u.next.next, 等等。我们探索 u₀, u₁, u₂, ... 寻找可以为 x 提供空间的节点。在我们的空间探索过程中可能出现三种情况（见图 3.4）：

1. 我们很快（在 r+1 ≤ b 步内）找到一个块未满的节点 uᵣ。在这种情况下，我们执行 r 次元素从一个块到下一个块的移位，使得 uᵣ 中的空闲空间变为 u₀ 中的空闲空间。然后我们可以将 x 插入到 u₀ 的块中。

2. 我们很快（在 r+1 ≤ b 步内）跑到了块列表的末尾。在这种情况下，我们在块列表的末尾添加一个新的空块，然后按照第一种情况继续。

3. 经过 b 步后，我们没有找到任何未满的块。在这种情况下，u₀, ..., u_{b-1} 是一个包含 b 个块的序列，每个块包含 b+1 个元素。我们在这个序列的末尾插入一个新块 u_b，并将原始的 b(b+1) 个元素展开（spread），使得 u₀, ..., u_b 的每个块恰好包含 b 个元素。现在 u₀ 的块只包含 b 个元素，因此有空间让我们插入 x。

```python
    def add(self, i, x):
        if i == self.n:
            self.append(x)
            return True
        u, j = self.get_location(i)
        r = 0
        w = u
        while r <= self.b and w != self.dummy and w.d.size() == self.b + 1:
            w = w.next
            r += 1
        if r == self.b + 1:
            self.spread(u)
            w = u
        if w == self.dummy:
            w = self.add_before(w, self.b)
            while w.prev != u:
                w.prev.d.add_last(w.d.remove_first())
                w = w.prev
        else:
            while w != u:
                w.prev.d.add_last(w.d.remove_first())
                w = w.prev
        u.d.add(j, x)
        self.n += 1
```

`add(i,x)` 操作的运行时间取决于上述三种情况中哪一种发生。情况 1 和 2 涉及检查和移动最多 b 个块中的元素，需要 O(b) 时间。情况 3 涉及调用 `spread(u)` 方法，该方法移动 b(b+1) 个元素，需要 O(b²) 时间。如果我们忽略情况 3 的成本（稍后我们将通过摊销来核算），那么定位 i 并执行 x 的插入的总运行时间为 O(b + min{i, n-i}/b)。

### 3.3.4 删除元素

从 SEList 中删除元素类似于添加元素。我们首先定位包含索引 i 的元素的节点 u。现在，我们必须为无法从 u 中删除元素而不导致 u 的块变得小于 b-1 的情况做好准备。

同样，设 u₀, u₁, u₂, ... 表示 u, u.next, u.next.next, 等等。我们检查 u₀, u₁, u₂, ... 以便寻找可以从中借用一个元素来使 u₀ 的块大小至少为 b-1 的节点。需要考虑三种情况（见图 3.5）：

1. 我们很快（在 r+1 ≤ b 步内）找到一个块包含多于 b-1 个元素的节点。在这种情况下，我们执行 r 次元素从一个块到前一个块的移位，使得 uᵣ 中的额外元素变为 u₀ 中的额外元素。然后我们可以从 u₀ 的块中删除适当的元素。

2. 我们很快（在 r+1 ≤ b 步内）跑到了块列表的末尾。在这种情况下，uᵣ 是最后一个块，uᵣ 的块不需要包含至少 b-1 个元素。因此，我们按照上述方式继续，从 uᵣ 借用一个元素到 u₀ 中。如果这导致 uᵣ 的块变空，那么我们就删除它。

3. 经过 b 步后，我们没有找到任何包含多于 b-1 个元素的块。在这种情况下，u₀, ..., u_{b-1} 是一个包含 b 个块的序列，每个块包含 b-1 个元素。我们将这 b(b-1) 个元素聚集（gather）到 u₀, ..., u_{b-2} 中，使得这 b-1 个块中的每一个恰好包含 b 个元素，并删除现在为空的 u_{b-1}。现在 u₀ 的块包含 b 个元素，然后我们可以从中删除适当的元素。

```python
    def remove(self, i):
        u, j = self.get_location(i)
        r = 0
        w = u
        while r <= self.b and w != self.dummy and w.d.size() == self.b - 1:
            w = w.next
            r += 1
        if r == self.b + 1:
            self.gather(u)
        u.d.remove(j)
        self.n -= 1
        while u.d.size() < self.b - 1 and u.next != self.dummy:
            u.d.add_last(u.next.d.remove_first())
            u = u.next
        if u.d.size() == 0:
            self.remove_node(u)
```

与 `add(i,x)` 操作类似，如果忽略情况 3 中出现的 `gather(u)` 方法的成本，那么 `remove(i)` 操作的运行时间为 O(b + min{i, n-i}/b)。

### 3.3.5 展开和聚集的摊销分析

接下来，我们考虑 `add(i,x)` 和 `remove(i)` 方法可能执行的 `gather(u)` 和 `spread(u)` 方法的成本。为完整起见，这里给出它们：

```python
    def spread(self, u):
        w = u
        for _ in range(self.b):
            w = w.next
        w = self.add_before(w, self.b)
        while w != u:
            while w.prev.d.size() < self.b:
                w.prev.d.add_last(w.d.remove_first())
            w = w.prev
        self.remove_node(u)

    def gather(self, u):
        w = u
        for _ in range(self.b):
            w = w.next
        while w != u:
            while w.d.size() > self.b:
                w.prev.d.add_last(w.d.remove_first())
            w = w.prev
        self.remove_node(w)
```

每个方法的运行时间由两个嵌套循环主导。内层和外层循环都最多执行 b+1 次，因此每个方法的总运行时间为 O((b+1)²) = O(b²)。然而，以下引理表明，这些方法每 b 次调用 `add(i,x)` 或 `remove(i)` 最多执行一次。

**引理 3.1** *如果创建一个空的 SEList 并执行任何 m ≥ 1 次对 `add(i,x)` 和 `remove(i)` 的调用序列，那么所有对 `spread(u)` 和 `gather(u)` 的调用中花费的总时间为 O(b²m/b) = O(bm)。*

**证明：** 我们将使用摊销分析（amortized analysis）的势能法（potential method）。如果节点 u 的块不包含 b-1 个元素（即 u 是最后一个节点，或者包含 b-2 或 b+1 个元素），则称该节点为脆弱节点（fragile）。任何块包含 b-1 个元素的节点称为坚固节点（rugged）。将 SEList 的势能定义为其包含的脆弱节点数量。我们将仅考虑 `add(i,x)` 操作及其与对 `spread(u)` 的调用次数的关系。对 `remove(i)` 和 `gather(u)` 的分析是相同的。

注意，如果在 `add(i,x)` 方法中发生情况 1，那么只有一个节点 u₀ 的块大小发生改变。因此，最多有一个节点（即 u₀）从坚固变为脆弱。如果发生情况 2，则创建一个新节点，并且该节点是脆弱的，但没有其他节点改变大小，因此脆弱节点数量增加 1。因此，在情况 1 或情况 2 中，SEList 的势能最多增加 1。

最后，如果发生情况 3，那是因为 u₀, ..., u_{b-1} 都是脆弱节点。然后调用 `spread(u)`，这 b 个脆弱节点被 b+1 个坚固节点取代。最后，将 x 添加到 u₀ 的块中，使 u₀ 变为脆弱。总的来说，势能减少了 b-1。

总之，势能从 0 开始（列表中没有节点）。每次发生情况 1 或情况 2 时，势能最多增加 1。每次发生情况 3 时，势能减少 b-1。势能（计算脆弱节点的数量）永远不会小于 0。我们得出结论，对于每次情况 3 的发生，至少有 b-1 次情况 1 或情况 2 的发生。因此，每次对 `spread(u)` 的调用，至少有 b-1 次对 `add(i,x)` 的调用。这就完成了证明。

### 3.3.6 总结

以下定理总结了 SEList 数据结构的性能：

**定理 3.3** *SEList 实现了 List 接口。忽略对 `spread(u)` 和 `gather(u)` 调用的成本，块大小为 b 的 SEList 支持以下操作：*

- *`get(i)` 和 `set(i,x)` 每次操作时间为 O(1 + min{i, n-i}/b)；且*
- *`add(i,x)` 和 `remove(i)` 每次操作时间为 O(b + min{i, n-i}/b)。*

*此外，从一个空的 SEList 开始，任何 m 次 `add(i,x)` 和 `remove(i)` 操作的序列导致所有对 `spread(u)` 和 `gather(u)` 的调用中花费的总时间为 O(bm)。*

*存储 n 个元素的 SEList 使用的空间（以字¹⁾ 为单位）为 O(n + b²)。*

SEList 是 ArrayList 和 DLList 之间的权衡，这两种结构的相对混合取决于块大小 b。在极端情况 b = 1 下，每个 SEList 节点最多存储三个值，这与 DLList 差别不大。在另一个极端 b = n 下，所有元素都存储在一个单独的数组中，就像在 ArrayList 中一样。在这两个极端之间，存在添加或删除列表项的时间与定位特定列表项的时间之间的权衡。

> ¹⁾ 回忆第 1.4 节关于如何度量内存的讨论。

## 3.4 讨论和练习

单向链表和双向链表都是成熟的技术，已在程序中使用超过 40 年。例如，Knuth [46, 第 2.2.3-2.2.5 节] 讨论了它们。甚至 SEList 数据结构似乎也是一个众所周知的数据结构练习。SEList 有时被称为展开链表（unrolled linked list）[67]。

另一种在双向链表中节省空间的方法是使用所谓的 XOR 链表（异或链表）。在 XOR 链表中，每个节点 u 只包含一个指针，称为 `u.nextprev`，它保存 `u.prev` 和 `u.next` 的按位异或。列表本身需要存储两个指针，一个指向 dummy 节点，另一个指向 `dummy.next`（第一个节点，如果列表为空则为 dummy）。这种技术利用了以下事实：如果我们有指向 u 和 `u.prev` 的指针，那么我们可以使用公式提取 `u.next`：

```
u.next = u.prev ^ u.nextprev
```

（这里 `^` 计算其两个参数的按位异或。）这种技术使代码稍微复杂一些，并且在一些具有垃圾回收机制的语言（如 Java 和 Python）中无法使用，但它提供了每个节点只需要一个指针的双向链表实现。有关 XOR 链表的详细讨论，参见 Sinha 的杂志文章 [68]。

---

**练习 3.1** 为什么不能在 SLList 中使用哨兵节点来避免 `push(x)`、`pop()`、`add(x)` 和 `remove()` 操作中出现的所有特殊情况？

**练习 3.2** 设计并实现一个 SLList 方法 `second_last()`，返回 SLList 的倒数第二个元素。不要使用跟踪列表大小的成员变量 n。

**练习 3.3** 在 SLList 上实现 List 操作 `get(i)`、`set(i,x)`、`add(i,x)` 和 `remove(i)`。每个操作应在 O(1 + i) 时间内运行。

**练习 3.4** 设计并实现一个 SLList 方法 `reverse()`，反转 SLList 中元素的顺序。该方法应在 O(n) 时间内运行，不应使用递归，不应使用任何辅助数据结构，也不应创建任何新节点。

**练习 3.5** 设计并实现 SLList 和 DLList 的方法 `check_size()`。这些方法遍历列表并计算节点数量，看是否与列表存储的值 n 匹配。这些方法不返回任何内容，但如果它们计算出的大小与 n 的值不匹配，则抛出异常。

**练习 3.6** 尝试重现 `add_before(w)` 操作的代码，该操作创建一个节点 u，并将其添加到 DLList 中节点 w 之前。不要参考本章。即使你的代码与本书给出的代码不完全匹配，它仍然可能是正确的。测试它并看看它是否有效。

接下来的几个练习涉及对 DLList 进行操作。你应该在不分配任何新节点或临时数组的情况下完成它们。它们都可以仅通过更改现有节点的 `prev` 和 `next` 值来完成。

**练习 3.7** 编写一个 DLList 方法 `is_palindrome()`，如果列表是回文（palindrome）则返回 true，即对于所有 i ∈ {0, ..., n-1}，位置 i 的元素等于位置 n-i-1 的元素。你的代码应在 O(n) 时间内运行。

**练习 3.8** 实现一个方法 `rotate(r)`，用于"旋转"DLList，使得列表项 i 变为列表项 (i+r) mod n。该方法应在 O(1 + min{r, n-r}) 时间内运行，并且不应修改列表中的任何节点。

**练习 3.9** 编写一个方法 `truncate(i)`，在位置 i 处截断 DLList。执行此方法后，列表的大小将为 i，并且应只包含索引 0, ..., i-1 处的元素。返回值是另一个包含索引 i, ..., n-1 处元素的 DLList。该方法应在 O(min{i, n-i}) 时间内运行。

**练习 3.10** 编写一个 DLList 方法 `absorb(l2)`，它接受一个 DLList 参数 l2，将其清空并按顺序将其内容追加到接收者。例如，如果 l1 包含 a,b,c 且 l2 包含 d,e,f，则在调用 l1.absorb(l2) 后，l1 将包含 a,b,c,d,e,f，而 l2 将为空。

**练习 3.11** 编写一个方法 `deal()`，从 DLList 中删除所有奇数索引的元素，并返回一个包含这些元素的 DLList。例如，如果 l1 包含元素 a,b,c,d,e,f，则在调用 l1.deal() 后，l1 应包含 a,c,e，并返回一个包含 b,d,f 的列表。

**练习 3.12** 编写一个方法 `reverse()`，反转 DLList 中元素的顺序。

**练习 3.13** 本练习指导你实现用于对 DLList 排序的归并排序（merge-sort）算法，如第 11.1.1 节所述。

1. 编写一个名为 `take_first(l2)` 的 DLList 方法。该方法从 l2 中取出第一个节点并将其追加到接收列表。这相当于 `add(size(), l2.remove(0))`，只是它不应创建新节点。
2. 编写一个 DLList 静态方法 `merge(l1, l2)`，接受两个已排序的列表 l1 和 l2，合并它们，并返回一个新的包含结果的已排序列表。这会导致 l1 和 l2 在此过程中被清空。例如，如果 l1 包含 a,c,d 且 l2 包含 b,e,f，则此方法返回一个包含 a,b,c,d,e,f 的新列表。
3. 编写一个 DLList 方法 `sort()`，使用归并排序算法对列表中包含的元素进行排序。这个递归算法的工作方式如下：
   - 如果列表包含 0 或 1 个元素，则无需做任何事。否则，
   - 使用 `truncate(size()/2)` 方法，将列表分成两个长度大致相等的列表 l1 和 l2；
   - 递归地对 l1 排序；
   - 递归地对 l2 排序；最后，
   - 将 l1 和 l2 合并成一个单一的已排序列表。

接下来的几个练习更高级，需要清楚理解当添加和删除项时 Stack 或 Queue 中存储的最小值会发生什么变化。

**练习 3.14** 设计并实现一个 MinStack 数据结构，可以存储可比较的元素，并支持栈操作 `push(x)`、`pop()` 和 `size()`，以及 `min()` 操作（返回当前存储在数据结构中的最小值）。所有操作应在常数时间内运行。

**练习 3.15** 设计并实现一个 MinQueue 数据结构，可以存储可比较的元素，并支持队列操作 `add(x)`、`remove()` 和 `size()`，以及 `min()` 操作（返回当前存储在数据结构中的最小值）。所有操作应在常数摊销时间内运行。

**练习 3.16** 设计并实现一个 MinDeque 数据结构，可以存储可比较的元素，并支持所有双端队列操作 `add_first(x)`、`add_last(x)`、`remove_first()`、`remove_last()` 和 `size()`，以及 `min()` 操作（返回当前存储在数据结构中的最小值）。所有操作应在常数摊销时间内运行。

接下来的练习旨在测试读者对空间高效 SEList 的实现和分析的理解：

**练习 3.17** 证明，如果 SEList 像 Stack 一样使用（即对 SEList 的唯一修改是使用 `push(x)`（等价于 `add(size(), x)`）和 `pop()`（等价于 `remove(size()-1)`）），那么这些操作在常数摊销时间内运行，与 b 的值无关。

**练习 3.18** 设计并实现一个 SEList 的版本，支持所有 Deque 操作，每次操作在常数摊销时间内完成，与 b 的值无关。

**练习 3.19** 解释如何使用按位异或运算符 `^` 在不使用第三个变量的情况下交换两个 int 变量的值。
