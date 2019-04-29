package com.mycroft.flowlayoutmanager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

/**
 * 1. 层叠3张card, 不够则少呈现，至多3张
 * 2. 左滑移动图片
 * 3. 图片层叠右侧和下侧的card边距相同
 *
 * @author mycroft
 */
public class CardLayoutManager extends RecyclerView.LayoutManager {

    public CardLayoutManager() {
    }

    public CardLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return true;
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
     * 最大卡片数
     */
    private static final int MAX_CARD = 3;

    private int layout(RecyclerView.Recycler recycler, RecyclerView.State state, int horizontalOffset) {

        final int childCount = getChildCount();

        // 当第一次进行布局时
        if (childCount == 0) {
            int itemCount = getItemCount();
            int count = Math.min(MAX_CARD, itemCount);
            for (int i = count - 1; i >= 0; i--) {
                // 获取一个item view, 添加到RecyclerView中，进行测量、布局
                final View itemView = recycler.getViewForPosition(i);
                addView(itemView);
                // 测量，获取尺寸
                measureChildWithMargins(itemView, 0, 0);

                // TODO 默认横向500px， 纵向800px, 5:8的比例, 错位40px

                final int sizeHorizontal = getDecoratedMeasurementHorizontal(itemView);
                final int sizeVertical = getDecoratedMeasurementVertical(itemView);

                layoutDecoratedWithMargins(itemView, getWidth() - sizeHorizontal - 100 + 40 * i, getHeight() - sizeVertical - 100 + 40 * i, getWidth() - 100 + 40 * i, getHeight() - 100 + 40 * i);
            }

        } else {
            // nothing
        }



        return horizontalOffset;
    }

    /**
     * 横向偏移量
     */
    private int mHorizontalOffset = 0;

    /**
     * 处理横向滑动的距离
     *
     * @param dx       横向滑动的距离
     * @param recycler 回收器
     * @param state    当前状态
     * @return
     */
    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        // 如果滑动距离为0, 或是item view小于max card count, 则不移动
        if (dx == 0 || getChildCount() == MAX_CARD) {
            return 0;
        }

        // 实际滑动的距离，到达边界时需要进行修正
        int realOffset = dx;
        if (mHorizontalOffset + realOffset < 0) {
            realOffset = -mHorizontalOffset;
        } else if (realOffset > 0) {
            // 2019/4/29 判断是否到达右侧边界

            // 手指上滑，判断是否到达下边界
            final View lastChildView = getChildAt(getChildCount() - 1);
            if (lastChildView != null && getPosition(lastChildView) == getItemCount() - 1) {
                int end = getDecoratedRight(lastChildView);
                // 20是右侧固定的边距
                int gap = getWidth() - getPaddingEnd() - end - 20;

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

        mHorizontalOffset += realOffset;

        offsetChildrenVertical(-realOffset);

        return realOffset;
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public boolean canScrollVertically() {
        return false;
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
}
