# LayoutManager分析与实践

自从```RecyclerView```出来之后就得到广泛的应用，但是由于其使用和定制化比较复杂，所以很多时候都停留在使用阶段上。这片文章简单讲述一个自定义一个简单的```LayoutManager```.

## 翻译

```Java
/**
 * A <code>LayoutManager</code> is responsible for measuring and positioning item views
 * within a <code>RecyclerView</code> as well as determining the policy for when to recycle
 * item views that are no longer visible to the user. By changing the <code>LayoutManager</code>
 * a <code>RecyclerView</code> can be used to implement a standard vertically scrolling list,
 * a uniform grid, staggered grids, horizontally scrolling collections and more. Several stock
 * layout managers are provided for general use.
 * <p/>
 * If the LayoutManager specifies a default constructor or one with the signature
 * ({@link Context}, {@link AttributeSet}, {@code int}, {@code int}), RecyclerView will
 * instantiate and set the LayoutManager when being inflated. Most used properties can
 * be then obtained from {@link #getProperties(Context, AttributeSet, int, int)}. In case
 * a LayoutManager specifies both constructors, the non-default constructor will take
 * precedence.
 *
 */
```

翻译：

一个```LayoutManager```负责测量和定位```RecyclerView```的item views, 同时需要处理在item views不再可见时的回收工作。通过设置不同的```LayoutManager```, ```RecyclerView```可以用来实现标准的纵向滑动列表，通用的网格、交错型、横向滑动等效果。这几种```LayoutManager```都已经提供了。

如果一个```LayoutManager```设定了默认的构造器或者一个参数分别为```Context```, ```AttributeSet```, ```int```, ```int```的构造器，```RecyclerView```可以通过xml来进行实例化该```LayoutManager```, 不用在代码中设置。可以在```LayoutManager```构造器中使用方法```getProperties(Context, AttributeSet, int, int)```来获取自定义的属性。如果两种构造器都制定了，那么优先使用默认的构造器。

## 功能

从官方文档中可以看出，一个```LayoutManager```的工作是：测量、定位和回收```RecyclerView```的item views. 简而言之，```LayoutManager```实际上就是让item views合理的显示在```RecyclerView```, 同时需要进行回收。这些工作和定义一个使用了回收机制的```ViewGroup```非常相似，就像```ListView```一样，不过它并不考虑数据的绑定。

定义一个有```View```回收机制的```ViewGroup```需要处理的工作：

- 测量（measure）
- 布局（定位, layout）
- 考虑滑动
- 回收```View```

这里我们先讨论一下在```LayoutManager```需要怎么处理这些工作。

- 测量：实际上是在```Adapter```中构造```View```时的工作，只需要获取即可
- 布局：对获取到的```View```根据要求放置在界面上，同时需要考虑滑动
- 滑动：滑动的处理是由```RecyclerView```来完成的，```LayoutManager```需要处理的是决定滑动方向和修正滑动距离
- 回收```View```：关于如何回收```View```都是放在```Recycler```中的，```LayoutManager```需要告诉那些```View```需要被回收


接下来说明一下```LayoutManager```做这些工作可能使用到的方法。

### 测量

实际的测量工作其实是放在```View```构造阶段的，即```Adapter```中，所以在获取到```View```时，就可以直接获取到该```View```的尺寸。不过另外需要注意的是```View```的margin尺寸。

下面两个方法是计算一个child view布局时所占用的空间尺寸，考虑到了margin:

```Java
/**
 * 获取 child view 横向上需要占用的空间，margin计算在内
 *
 * @param view item view
 * @return child view 横向占用的空间
 */
private int getDecoratedMeasurementHorizontal(View view) {
    final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
            view.getLayoutParams();
    return getDecoratedMeasuredWidth(view) + params.leftMargin
            + params.rightMargin;
}

/**
 * 获取 child view 纵向上需要占用的空间，margin计算在内
 *
 * @param view item view
 * @return child view 纵向占用的空间
 */
private int getDecoratedMeasurementVertical(View view) {
    final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
    return getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
}
```

因为```ItemDecoration```的存在，所以获取尺寸时使用的是```getDecoratedMeasuredWidth(View)```和```getDecoratedMeasuredHeight(View)```, 将```ItemDecoration```考虑在内了。


### 布局

布局是```LayoutManager```最基本的功能，同时有比较复杂的尺寸结算。如果对计算不那么排斥或有比较好的计算方法的话，实际上也很简单。

下面是```LayoutManager```对一个child view进行布局（定位）的方法：

```Java
/**
 * Lay out the given child view within the RecyclerView using coordinates that
 * include any current {@link ItemDecoration ItemDecorations} and margins.
 *
 * <p>LayoutManagers should prefer working in sizes and coordinates that include
 * item decoration insets whenever possible. This allows the LayoutManager to effectively
 * ignore decoration insets within measurement and layout code. See the following
 * methods:</p>
 * <ul>
 *     <li>{@link #layoutDecorated(View, int, int, int, int)}</li>
 *     <li>{@link #measureChild(View, int, int)}</li>
 *     <li>{@link #measureChildWithMargins(View, int, int)}</li>
 *     <li>{@link #getDecoratedLeft(View)}</li>
 *     <li>{@link #getDecoratedTop(View)}</li>
 *     <li>{@link #getDecoratedRight(View)}</li>
 *     <li>{@link #getDecoratedBottom(View)}</li>
 *     <li>{@link #getDecoratedMeasuredWidth(View)}</li>
 *     <li>{@link #getDecoratedMeasuredHeight(View)}</li>
 * </ul>
 *
 * @param child Child to lay out
 * @param left Left edge, with item decoration insets and left margin included
 * @param top Top edge, with item decoration insets and top margin included
 * @param right Right edge, with item decoration insets and right margin included
 * @param bottom Bottom edge, with item decoration insets and bottom margin included
 *
 * @see View#layout(int, int, int, int)
 * @see #layoutDecorated(View, int, int, int, int)
 */
public void layoutDecoratedWithMargins(View child, int left, int top, int right,
        int bottom) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    final Rect insets = lp.mDecorInsets;
    child.layout(left + insets.left + lp.leftMargin, top + insets.top + lp.topMargin,
            right - insets.right - lp.rightMargin,
            bottom - insets.bottom - lp.bottomMargin);
}
```

注释翻译：

在```RecyclerView```中定位一个child view, 考虑了```ItemDecoration```和margin的影响。

**LayoutManager**应该尽可能地更加关注包含```ItemDecoration```嵌入时的尺寸和定位。这个方法允许```LayoutManager```不用考虑受```ItemDecoration```影响的尺寸和布局代码，获取的直接是最终结果。

参考的方法：忽略。

参数

- child: child view
- left, 左侧边距，包括```ItemDecoration```和margin
- top, 上边距
- right, 右边距
- bottom, 下边距

可以看出，这个方法布局时需要考虑```ItemDecoration```的影响和margin, 而进行测量的时候其实已经包括了```ItemDecoration```的影响和margin，我们就不再做过多的计算工作。

### 滑动

滑动有两个方向，一般允许一个方向的滑动，实际上，也可以同时允许两个方向上的滑动，不过一般没有这个需求。

下面是考虑滑动时可能需要处理的几个方法：

```Java
/**
 * Scroll horizontally by dx pixels in screen coordinates and return the distance traveled.
 * The default implementation does nothing and returns 0.
 *
 * @param dx            distance to scroll by in pixels. X increases as scroll position
 *                      approaches the right.
 * @param recycler      Recycler to use for fetching potentially cached views for a
 *                      position
 * @param state         Transient state of RecyclerView
 * @return The actual distance scrolled. The return value will be negative if dx was
 * negative and scrolling proceeeded in that direction.
 * <code>Math.abs(result)</code> may be less than dx if a boundary was reached.
 */
public int scrollHorizontallyBy(int dx, Recycler recycler, State state) {
    return 0;
}

/**
 * Scroll vertically by dy pixels in screen coordinates and return the distance traveled.
 * The default implementation does nothing and returns 0.
 *
 * @param dy            distance to scroll in pixels. Y increases as scroll position
 *                      approaches the bottom.
 * @param recycler      Recycler to use for fetching potentially cached views for a
 *                      position
 * @param state         Transient state of RecyclerView
 * @return The actual distance scrolled. The return value will be negative if dy was
 * negative and scrolling proceeeded in that direction.
 * <code>Math.abs(result)</code> may be less than dy if a boundary was reached.
 */
public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
    return 0;
}

/**
 * Query if horizontal scrolling is currently supported. The default implementation
 * returns false.
 *
 * @return True if this LayoutManager can scroll the current contents horizontally
 */
public boolean canScrollHorizontally() {
    return false;
}

/**
 * Query if vertical scrolling is currently supported. The default implementation
 * returns false.
 *
 * @return True if this LayoutManager can scroll the current contents vertically
 */
public boolean canScrollVertically() {
    return false;
}
```

注释翻译：

- scrollHorizontallyBy(int, Recycler, State): 根据屏幕横向滑动的距离dx计算实际滑动的距离。默认不进行滑动，返回值为0
- scrollVerticallyBy(int, Recycler, State): 根据屏幕纵向滑动的距离dy计算实际滑动的距离。默认不进行滑动，返回值为0
- canScrollHorizontally(): 查询目前是否支持横向滑动。默认返回false
- canScrollVertically(): 查询目前是否支持横向滑动。默认返回false

方法```canScrollHorizontally()```和```canScrollVertically()```是判断是否处理```RecyclerView```上的滑动，这个根据需求自行实现。真正处理滑动的是在```scrollHorizontallyBy(int, Recycler, State)```和```scrollVerticallyBy(int, Recycler, State)```中，手指滑动距离作为参数传入这两个方法中，而根据逻辑，处理滑动，最后返回处理后的滑动距离，一般情况下，如果没有到达边界，那么处理后的滑动距离和实际滑动距离是一样的，到达边界时对滑动距离进行修正。

另外需要注意的是在```scrollHorizontallyBy(int, Recycler, State)```和```scrollVerticallyBy(int, Recycler, State)```中需要进行滑动时的布局问题，即滑动一定距离之后，实际上是重新进行了布局的。

### 回收```View```

回收```View```原因和原理是，在根据逻辑布局```View```时，它超出了用户的可视范围，所以为了性能考虑，我们应该及时进行回收，避免耗费过多的内存。而通常的处理方法是，在用户的可视范围上进行布局，查看超出边界的child view, 然后进行回收，当然，及时发现不可能显示在界面上的child view, 在布局过程中就可以决定是否需要对余下的child view进行布局。

#### 概念

在```RecyclerView```的概念中，有几种对回收的概念

- attach/detach: 将item view添加到```RecyclerView```中，和add/remove不同的是不会触发layout
- scrap: 标识一个item view表示其已经从```RecyclerView```中移除，但是实际上还是在```RecyclerView```中
- recycle: 表示一个没有parent的```View```的回收处理工作，可以销毁，也可以用来复用

```Java
/**
 * Temporarily detach and scrap all currently attached child views. Views will be scrapped
 * into the given Recycler. The Recycler may prefer to reuse scrap views before
 * other views that were previously recycled.
 *
 * @param recycler Recycler to scrap views into
 */
public void detachAndScrapAttachedViews(Recycler recycler) {
    final int childCount = getChildCount();
    for (int i = childCount - 1; i >= 0; i--) {
        final View v = getChildAt(i);
        scrapOrRecycleView(recycler, i, v);
    }
}
```

注释翻译：

临时detach和scrap所有的child view. 这些view将被放置到```Recycler```中处理。相对于之前回收的那些view, ```Recycler```会首先使用利用这些views.

除了回收所有的child view之外，还有很多其他的处理child view的方法，全部列举在下面，就不再一一翻译，可能会用到的一个方法```removeAndRecycleView(View, Recycler)```, 表示移除一个child view, 并交由指定的```Recycler```处理。

```Java
/**
 * Temporarily detach a child view.
 *
 * <p>LayoutManagers may want to perform a lightweight detach operation to rearrange
 * views currently attached to the RecyclerView. Generally LayoutManager implementations
 * will want to use {@link #detachAndScrapView(android.view.View, RecyclerView.Recycler)}
 * so that the detached view may be rebound and reused.</p>
 *
 * <p>If a LayoutManager uses this method to detach a view, it <em>must</em>
 * {@link #attachView(android.view.View, int, RecyclerView.LayoutParams) reattach}
 * or {@link #removeDetachedView(android.view.View) fully remove} the detached view
 * before the LayoutManager entry point method called by RecyclerView returns.</p>
 *
 * @param child Child to detach
 */
public void detachView(View child) {
    final int ind = mChildHelper.indexOfChild(child);
    if (ind >= 0) {
        detachViewInternal(ind, child);
    }
}

/**
 * Temporarily detach a child view.
 *
 * <p>LayoutManagers may want to perform a lightweight detach operation to rearrange
 * views currently attached to the RecyclerView. Generally LayoutManager implementations
 * will want to use {@link #detachAndScrapView(android.view.View, RecyclerView.Recycler)}
 * so that the detached view may be rebound and reused.</p>
 *
 * <p>If a LayoutManager uses this method to detach a view, it <em>must</em>
 * {@link #attachView(android.view.View, int, RecyclerView.LayoutParams) reattach}
 * or {@link #removeDetachedView(android.view.View) fully remove} the detached view
 * before the LayoutManager entry point method called by RecyclerView returns.</p>
 *
 * @param index Index of the child to detach
 */
public void detachViewAt(int index) {
    detachViewInternal(index, getChildAt(index));
}

private void detachViewInternal(int index, View view) {
    if (DISPATCH_TEMP_DETACH) {
        ViewCompat.dispatchStartTemporaryDetach(view);
    }
    mChildHelper.detachViewFromParent(index);
}

/**
 * Reattach a previously {@link #detachView(android.view.View) detached} view.
 * This method should not be used to reattach views that were previously
 * {@link #detachAndScrapView(android.view.View, RecyclerView.Recycler)}  scrapped}.
 *
 * @param child Child to reattach
 * @param index Intended child index for child
 * @param lp LayoutParams for child
 */
public void attachView(View child, int index, LayoutParams lp) {
    ViewHolder vh = getChildViewHolderInt(child);
    if (vh.isRemoved()) {
        mRecyclerView.mViewInfoStore.addToDisappearedInLayout(vh);
    } else {
        mRecyclerView.mViewInfoStore.removeFromDisappearedInLayout(vh);
    }
    mChildHelper.attachViewToParent(child, index, lp, vh.isRemoved());
    if (DISPATCH_TEMP_DETACH)  {
        ViewCompat.dispatchFinishTemporaryDetach(child);
    }
}

/**
 * Reattach a previously {@link #detachView(android.view.View) detached} view.
 * This method should not be used to reattach views that were previously
 * {@link #detachAndScrapView(android.view.View, RecyclerView.Recycler)}  scrapped}.
 *
 * @param child Child to reattach
 * @param index Intended child index for child
 */
public void attachView(View child, int index) {
    attachView(child, index, (LayoutParams) child.getLayoutParams());
}

/**
 * Reattach a previously {@link #detachView(android.view.View) detached} view.
 * This method should not be used to reattach views that were previously
 * {@link #detachAndScrapView(android.view.View, RecyclerView.Recycler)}  scrapped}.
 *
 * @param child Child to reattach
 */
public void attachView(View child) {
    attachView(child, -1);
}

/**
 * Finish removing a view that was previously temporarily
 * {@link #detachView(android.view.View) detached}.
 *
 * @param child Detached child to remove
 */
public void removeDetachedView(View child) {
    mRecyclerView.removeDetachedView(child, false);
}

/**
 * Moves a View from one position to another.
 *
 * @param fromIndex The View's initial index
 * @param toIndex The View's target index
 */
public void moveView(int fromIndex, int toIndex) {
    View view = getChildAt(fromIndex);
    if (view == null) {
        throw new IllegalArgumentException("Cannot move a child from non-existing index:"
                + fromIndex);
    }
    detachViewAt(fromIndex);
    attachView(view, toIndex);
}

/**
 * Detach a child view and add it to a {@link Recycler Recycler's} scrap heap.
 *
 * <p>Scrapping a view allows it to be rebound and reused to show updated or
 * different data.</p>
 *
 * @param child Child to detach and scrap
 * @param recycler Recycler to deposit the new scrap view into
 */
public void detachAndScrapView(View child, Recycler recycler) {
    int index = mChildHelper.indexOfChild(child);
    scrapOrRecycleView(recycler, index, child);
}

/**
 * Detach a child view and add it to a {@link Recycler Recycler's} scrap heap.
 *
 * <p>Scrapping a view allows it to be rebound and reused to show updated or
 * different data.</p>
 *
 * @param index Index of child to detach and scrap
 * @param recycler Recycler to deposit the new scrap view into
 */
public void detachAndScrapViewAt(int index, Recycler recycler) {
    final View child = getChildAt(index);
    scrapOrRecycleView(recycler, index, child);
}

/**
 * Remove a child view and recycle it using the given Recycler.
 *
 * @param child Child to remove and recycle
 * @param recycler Recycler to use to recycle child
 */
public void removeAndRecycleView(View child, Recycler recycler) {
    removeView(child);
    recycler.recycleView(child);
}

/**
 * Remove a child view and recycle it using the given Recycler.
 *
 * @param index Index of child to remove and recycle
 * @param recycler Recycler to use to recycle child
 */
public void removeAndRecycleViewAt(int index, Recycler recycler) {
    final View view = getChildAt(index);
    removeViewAt(index);
    recycler.recycleView(view);
}
```

## 实现FlowLayoutManger

看过很多例子，在学习```LayoutManager```的时候通常都是以```FlowLayoutManager```来作为实践的，因为我们对其比较了解，而且也没有更好的示例来作为练习。

### ```FlowLayoutManager```的功能

- 每个item view从第一行开始进行横向排列，当一行不足显示下一个item的时候，将其布局在下一行
- 支持纵向滑动（也可以支持横向滑动，不过目前不在考虑范围内）
- 每个item高度不一致的考虑

### 构造器

根据前面的阐述，我们实现两个构造器，分别用于代码构造和xml中使用。

```Java
public FlowLayoutManager() {
    setAutoMeasureEnabled(true);
}

public FlowLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    setAutoMeasureEnabled(true);
}
```

注意```setAutoMeasureEnabled(boolean)```是表明```RecyclerView```的布局是否交由```LayoutManager```进行处理，否则应该重写```LayoutManager#onMeasure(int, int)```来自定义测量的实现。一般传true, 除非有特殊需求。

### 实现默认方法

在```LayoutManager```必须实现这个方法。

```Java
@Override
public RecyclerView.LayoutParams generateDefaultLayoutParams() {
    return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
}
```

就是生成默认的```LayoutParams```, 参考```LinearLayoutManager```, 除非有特殊需要再改变。

### 暂时不考虑滑动进行布局

#### 只考虑第一次进行布局时

```Java
@Override
public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
    if (getItemCount() == 0) {
        detachAndScrapAttachedViews(recycler);
        return;
    }

    // 如果正在进行动画，则不进行布局
    if (getChildCount() == 0 && state.isPreLayout()) {
        return;
    }

    detachAndScrapAttachedViews(recycler);

    // 进行布局
    layout(recycler, state);
}

/**
 * 布局操作
 *
 * @param recycler
 * @param state
 */
private void layout(RecyclerView.Recycler recycler, RecyclerView.State state) {

    // 纵向计算偏移量，考虑padding
    int topOffset = getPaddingTop();
    // 横向计算偏移量，考虑padding
    int leftOffset = getPaddingLeft();
    // 行高，以最高的item作为参考
    int maxLineHeight = 0;

    final int childCount = getChildCount();

    // 当第一次进行布局时
    if (childCount == 0) {
        for (int i = 0; i < getItemCount(); i++) {
            // 获取一个item view, 添加到RecyclerView中，进行测量、布局
            final View itemView = recycler.getViewForPosition(i);
            addView(itemView);
            // 测量，获取尺寸
            measureChildWithMargins(itemView, 0, 0);
            final int sizeHorizontal = getDecoratedMeasurementHorizontal(itemView);
            final int sizeVertical = getDecoratedMeasurementVertical(itemView);
            // 进行布局
            if (leftOffset + sizeHorizontal <= getHorizontalSpace()) {
                // 如果这行能够布局，则往后排
                // layout
                layoutDecoratedWithMargins(itemView, leftOffset, topOffset, leftOffset + sizeHorizontal, topOffset + sizeVertical);

                // 修正横向计算偏移量
                leftOffset += sizeHorizontal;
                maxLineHeight = Math.max(maxLineHeight, sizeVertical);
            } else {
                // 如果当前行不够，则往下一行挪
                // 修正计算偏移量、行高
                topOffset += maxLineHeight;
                maxLineHeight = 0;
                leftOffset = getPaddingLeft();

                // layout
                if (topOffset > getHeight() - getPaddingBottom()) {
                    // 如果超出下边界
                    // 移除并回收该item view
                    removeAndRecycleView(itemView, recycler);
                } else {
                    // 如果没有超出下边界，则继续布局
                    layoutDecoratedWithMargins(itemView, leftOffset, topOffset, leftOffset + sizeHorizontal, topOffset + sizeVertical);
                    // 修正计算偏移量、行高
                    leftOffset += sizeHorizontal;
                    maxLineHeight = Math.max(maxLineHeight, sizeVertical);
                }
            }

        }
    } else {
        // nothing
    }
}

//...
/**
 * @return 横向的可布局的空间
 */
private int getHorizontalSpace() {
    return getWidth() - getPaddingLeft() - getPaddingRight();
}
```

#### 考虑滑动

因为是纵向滑动，我们将```RecyclerView```想象成一个宽度一定，长度可变的纸张，长度有其中的item布局来决定。当手指滑动屏幕时，实际上是让纸张在上面移动，同时保证纸张顶部不能低于```RecyclerView```顶部，纸张底部不能高于```RecyclerView```底部。

布局亦是这样，我们假定所有的item view都已经布局在张纸上面（不考虑回收），我们滑动时，只是改变了这张纸相对于屏幕的滑动距离。

##### 不考虑边界

```Java
/**
 * @return 可以纵向滑动
 */
@Override
public boolean canScrollVertically() {
    return true;
}

// 纵向偏移量
private int mVerticalOffset = 0;

@Override
public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
    Log.e(TAG, String.valueOf(dy));
    // 如果滑动距离为0, 或是没有任何item view, 则不移动
    if (dy == 0 || getChildCount() == 0) {
        return 0;
    }

    mVerticalOffset += dy;

    detachAndScrapAttachedViews(recycler);

    layout(recycler, state, mVerticalOffset);

    return dy;
}

@Override
public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
    if (getItemCount() == 0) {
        detachAndScrapAttachedViews(recycler);
        return;
    }

    // 如果正在进行动画，则不进行布局
    if (getChildCount() == 0 && state.isPreLayout()) {
        return;
    }

    detachAndScrapAttachedViews(recycler);

    // 进行布局
    layout(recycler, state, 0);
}

/**
 * 布局操作
 *
 * @param recycler
 * @param state
 */
private void layout(RecyclerView.Recycler recycler, RecyclerView.State state, int verticalOffset) {

    // 纵向计算偏移量，考虑padding
    int topOffset = getPaddingTop();
    // 横向计算偏移量，考虑padding
    int leftOffset = getPaddingLeft();
    // 行高，以最高的item作为参考
    int maxLineHeight = 0;

    for (int i = 0; i < getItemCount(); i++) {
        // 获取一个item view, 添加到RecyclerView中，进行测量、布局
        final View itemView = recycler.getViewForPosition(i);
        addView(itemView);
        // 测量，获取尺寸
        measureChildWithMargins(itemView, 0, 0);
        final int sizeHorizontal = getDecoratedMeasurementHorizontal(itemView);
        final int sizeVertical = getDecoratedMeasurementVertical(itemView);
        // 进行布局
        if (leftOffset + sizeHorizontal <= getHorizontalSpace()) {
            // 如果这行能够布局，则往后排
            // layout
            layoutDecoratedWithMargins(itemView, leftOffset, topOffset - verticalOffset, leftOffset + sizeHorizontal, topOffset + sizeVertical - verticalOffset);

            // 修正横向计算偏移量
            leftOffset += sizeHorizontal;
            maxLineHeight = Math.max(maxLineHeight, sizeVertical);
        } else {
            // 如果当前行不够，则往下一行挪
            // 修正计算偏移量、行高
            topOffset += maxLineHeight;
            maxLineHeight = 0;
            leftOffset = getPaddingLeft();

            // layout
            // 不考虑边界
            layoutDecoratedWithMargins(itemView, leftOffset, topOffset - verticalOffset, leftOffset + sizeHorizontal, topOffset + sizeVertical - verticalOffset);
            // 修正计算偏移量、行高
            leftOffset += sizeHorizontal;
            maxLineHeight = Math.max(maxLineHeight, sizeVertical);
        }
    }
}
```

上面示例，对滑动的处理比较简单，记录总共滑动的距离，并在定位时将滑动距离计算上。

##### 考虑边界

对于```FlowLayoutManager```来说，上边界比较容易处理，下边界的处理则需要稍加处理。

上边界：因为每一行的顶部都是相同的，所以手指下滑时，我们考虑第一行的顶部是否已经到达上边界，取第一个item即可。

下边界：每个item的高度不一定相同，为每一个item进行布局时，这样不会出现问题，但是在手指上滑时，判断是否到达下边界，需要知道最后一行中最高的item是多少，所以在计算的时候，要将最后一行的所有item考虑在内。

注：**我们认为，一旦一个item构造完成，那么它的尺寸是不应该发生变化的，如果在滑动过程中，尺寸发生变化，会影响到布局的计算。**

一个概念和tip: ```RecyclerView```就是一个```ViewGroup```, 而通常显示的顺序，也是```FlowLayoutManager```layout的顺序都是从第一个开始的，所以可以这样认为，在position较大的item view显示之前，较小position的item view都是经过测量和布局过的。我们可以将布局过的item view的位置保存，在手指下滑，即加载position较小的item view时，不再对其进行测量和布局，只需要取出其位置，使用滑动修正当前的位置即可。

但是会如果数据改变，item view的尺寸和布局则会发生改变，原先的记录则会被认为是脏数据，所以当任何数据改变时，需要对其修正。不过一个好的修正方案需要有好的策略，在实现中，我只采用了清理的方法，暂时不能提供更加理想化的策略。

下面是完整代码：

```Java
import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Mycroft on 2017/1/11.
 */
public final class FlowLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = FlowLayoutManager.class.getSimpleName();

    public FlowLayoutManager() {
        setAutoMeasureEnabled(true);
    }

    public FlowLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setAutoMeasureEnabled(true);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }

        // 如果正在进行动画，则不进行布局
        if (getChildCount() == 0 && state.isPreLayout()) {
            return;
        }

        detachAndScrapAttachedViews(recycler);

        // 进行布局
        layout(recycler, state, 0);
    }

    /**
     * @return 可以纵向滑动
     */
    @Override
    public boolean canScrollVertically() {
        return true;
    }

    // 纵向偏移量
    private int mVerticalOffset = 0;

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        // 如果滑动距离为0, 或是没有任何item view, 则不移动
        if (dy == 0 || getChildCount() == 0) {
            return 0;
        }

        // 实际滑动的距离，到达边界时需要进行修正
        int realOffset = dy;
        if (mVerticalOffset + realOffset < 0) {
            realOffset = -mVerticalOffset;
        } else if (realOffset > 0) {
            // 手指上滑，判断是否到达下边界
            final View lastChildView = getChildAt(getChildCount() - 1);
            if (getPosition(lastChildView) == getItemCount() - 1) {
                int maxBottom = getDecoratedBottom(lastChildView);

                int lastChildTop = getDecoratedTop(lastChildView);
                for (int i = getChildCount() - 2; i >= 0; i--) {
                    final View child = getChildAt(i);
                    if (getDecoratedTop(child) == lastChildTop) {
                        maxBottom = Math.max(maxBottom, getDecoratedBottom(getChildAt(i)));
                    } else {
                        break;
                    }
                }

                int gap = getHeight() - getPaddingBottom() - maxBottom;
                if (gap > 0) {
                    realOffset = -gap;
                } else if (gap == 0) {
                    realOffset = 0;
                } else {
                    realOffset = Math.min(realOffset, -gap);
                }
            }
        }

        realOffset = layout(recycler, state, realOffset);

        mVerticalOffset += realOffset;

        offsetChildrenVertical(-realOffset);

        return realOffset;
    }

    private final SparseArray<Rect> mItemRects = new SparseArray<>();

    /**
     * 布局操作
     *
     * @param recycler
     * @param state
     * @param dy       用于判断回收、显示item, 对布局/定位本身没有影响
     * @return
     */
    private int layout(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {

        int firstVisiblePos = 0;

        // 纵向计算偏移量，考虑padding
        int topOffset = getPaddingTop();
        // 横向计算偏移量，考虑padding
        int leftOffset = getPaddingLeft();
        // 行高，以最高的item作为参考
        int maxLineHeight = 0;

        int childCount = getChildCount();

        // 当是滑动进入时（在onLayoutChildren方法里面，我们移除了所有的child view, 所以只有可能从scrollVerticalBy方法里面进入这个方法）
        if (childCount > 0) {
            // 计算滑动后，需要被回收的child view

            if (dy > 0) {
                // 手指上滑，可能需要回收顶部的view
                for (int i = 0; i < childCount; i++) {
                    final View child = getChildAt(i);
                    if (getDecoratedBottom(child) - dy < topOffset) {
                        // 超出顶部的item
                        removeAndRecycleView(child, recycler);
                        i--;
                        childCount--;
                    } else {
                        firstVisiblePos = i;
                        break;
                    }
                }
            } else if (dy < 0) {
                // 手指下滑，可能需要回收底部的view
                for (int i = childCount - 1; i >= 0; i--) {
                    final View child = getChildAt(i);
                    if (getDecoratedTop(child) - dy > getHeight() - getPaddingBottom()) {
                        // 超出底部的item
                        removeAndRecycleView(child, recycler);
                    } else {
                        break;
                    }
                }
            }
        }

        // 进行布局
        if (dy >= 0) {
            // 手指上滑，按顺序布局item

            int minPosition = firstVisiblePos;
            if (getChildCount() > 0) {
                final View lastVisibleChild = getChildAt(getChildCount() - 1);
                // 修正当前偏移量
                topOffset = getDecoratedTop(lastVisibleChild);
                leftOffset = getDecoratedRight(lastVisibleChild);
                // 修正第一个应该进行布局的item view
                minPosition = getPosition(lastVisibleChild) + 1;

                // 使用排在最后一行的所有的child view进行高度修正
                maxLineHeight = Math.max(maxLineHeight, getDecoratedMeasurementVertical(lastVisibleChild));
                for (int i = getChildCount() - 2; i >= 0; i--) {
                    final View child = getChildAt(i);
                    if (getDecoratedTop(child) == topOffset) {
                        maxLineHeight = Math.max(maxLineHeight, getDecoratedMeasurementVertical(child));
                    } else {
                        break;
                    }
                }
            }

            // 布局新的 item view
            for (int i = minPosition; i < getItemCount(); i++) {

                // 获取item view, 添加、测量、获取尺寸
                final View itemView = recycler.getViewForPosition(i);
                addView(itemView);
                measureChildWithMargins(itemView, 0, 0);

                final int sizeHorizontal = getDecoratedMeasurementHorizontal(itemView);
                final int sizeVertical = getDecoratedMeasurementVertical(itemView);
                // 进行布局
                if (leftOffset + sizeHorizontal <= getHorizontalSpace()) {
                    // 如果这行能够布局，则往后排
                    // layout
                    layoutDecoratedWithMargins(itemView, leftOffset, topOffset, leftOffset + sizeHorizontal, topOffset + sizeVertical);
                    final Rect rect = new Rect(leftOffset, topOffset + mVerticalOffset, leftOffset + sizeHorizontal, topOffset + sizeVertical + mVerticalOffset);
                    // 保存布局信息
                    mItemRects.put(i, rect);

                    // 修正横向计算偏移量
                    leftOffset += sizeHorizontal;
                    maxLineHeight = Math.max(maxLineHeight, sizeVertical);
                } else {
                    // 如果当前行不够，则往下一行挪
                    // 修正计算偏移量、行高
                    topOffset += maxLineHeight;
                    maxLineHeight = 0;
                    leftOffset = getPaddingLeft();

                    // layout
                    if (topOffset - dy > getHeight() - getPaddingBottom()) {
                        // 如果超出下边界
                        // 移除并回收该item view
                        removeAndRecycleView(itemView, recycler);
                        break;
                    } else {
                        // 如果没有超出下边界，则继续布局
                        layoutDecoratedWithMargins(itemView, leftOffset, topOffset, leftOffset + sizeHorizontal, topOffset + sizeVertical);
                        final Rect rect = new Rect(leftOffset, topOffset + mVerticalOffset, leftOffset + sizeHorizontal, topOffset + sizeVertical + mVerticalOffset);
                        // 保存布局信息
                        mItemRects.put(i, rect);
                        // 修正计算偏移量、行高
                        leftOffset += sizeHorizontal;
                        maxLineHeight = Math.max(maxLineHeight, sizeVertical);
                    }
                }
            }
        } else {
            // 手指下滑，逆序布局新的child
            int maxPos = getItemCount() - 1;
            if (getChildCount() > 0) {
                maxPos = getPosition(getChildAt(0)) - 1;
            }

            for (int i = maxPos; i >= 0; i--) {
                Rect rect = mItemRects.get(i);
                // 判断底部是否在上边界下面
                if (rect.bottom - mVerticalOffset - dy >= getPaddingTop()) {
                    // 获取item view, 添加、设置尺寸、布局
                    final View itemView = recycler.getViewForPosition(i);
                    addView(itemView, 0);
                    measureChildWithMargins(itemView, 0, 0);
                    layoutDecoratedWithMargins(itemView, rect.left, rect.top - mVerticalOffset, rect.right, rect.bottom - mVerticalOffset);
                }
            }
        }

        return dy;
    }

    /* 对数据改变时的一些修正 */

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        mVerticalOffset = 0;
        mItemRects.clear();
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        mVerticalOffset = 0;
        mItemRects.clear();
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        mVerticalOffset = 0;
        mItemRects.clear();
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        mVerticalOffset = 0;
        mItemRects.clear();
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        mVerticalOffset = 0;
        mItemRects.clear();
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
        mVerticalOffset = 0;
        mItemRects.clear();
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        mVerticalOffset = 0;
        mItemRects.clear();
    }

    /**
     * 获取 child view 横向上需要占用的空间，margin计算在内
     *
     * @param view item view
     * @return child view 横向占用的空间
     */
    private int getDecoratedMeasurementHorizontal(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedMeasuredWidth(view) + params.leftMargin
                + params.rightMargin;
    }

    /**
     * 获取 child view 纵向上需要占用的空间，margin计算在内
     *
     * @param view item view
     * @return child view 纵向占用的空间
     */
    private int getDecoratedMeasurementVertical(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
    }

    /**
     * @return 横向的可布局的空间
     */
    private int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }
}
```



