package com.plantart.pageindicator;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GiftRecyclerViewAttach implements GiftPagerAttach<RecyclerView> {

    private GiftPagerIndicator indicator;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private RecyclerView.Adapter<?> attachedAdapter;

    private RecyclerView.OnScrollListener scrollListener;
    private RecyclerView.AdapterDataObserver dataObserver;

    private final boolean centered;
    private final int currentPageOffset;

    private int measuredChildWidth;
    private int measuredChildHeight;

    public GiftRecyclerViewAttach() {
        currentPageOffset = 0;
        centered = true;
    }

    public GiftRecyclerViewAttach(int currentPageOffset) {
        this.currentPageOffset = currentPageOffset;
        this.centered = false;
    }

    @Override
    public void attachPager(@NonNull final GiftPagerIndicator indicator, @NonNull final RecyclerView pager) {
        if (!(pager.getLayoutManager() instanceof LinearLayoutManager)) {
            throw new IllegalStateException("Only LinearLayoutManager is supported");
        }
        if (pager.getAdapter() == null) {
            throw new IllegalStateException("RecyclerView has not Adapter attached");
        }
        this.layoutManager = (LinearLayoutManager) pager.getLayoutManager();
        this.recyclerView = pager;
        this.attachedAdapter = pager.getAdapter();
        this.indicator = indicator;

        dataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                indicator.setDotCount(attachedAdapter.getItemCount());
                updateCurrentOffset();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                onChanged();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                onChanged();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                onChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                onChanged();
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                onChanged();
            }
        };
        attachedAdapter.registerAdapterDataObserver(dataObserver);
        indicator.setDotCount(attachedAdapter.getItemCount());
        updateCurrentOffset();

        scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && isInIdleState()) {
                    int newPosition = findCompletelyVisiblePosition();
                    if (newPosition != RecyclerView.NO_POSITION) {
                        indicator.setDotCount(attachedAdapter.getItemCount());
                        if (newPosition < attachedAdapter.getItemCount()) {
                            indicator.setCurrentPosition(newPosition);
                        }
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateCurrentOffset();
            }
        };

        recyclerView.addOnScrollListener(scrollListener);
    }

    @Override
    public void detachFromPager() {
        attachedAdapter.unregisterAdapterDataObserver(dataObserver);
        recyclerView.removeOnScrollListener(scrollListener);
        measuredChildWidth = 0;
    }

    private void updateCurrentOffset() {
        final View firstView = findFirstVisibleView();
        if (firstView == null) {
            return;
        }

        int position = recyclerView.getChildAdapterPosition(firstView);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        final int itemCount = attachedAdapter.getItemCount();

        if (position >= itemCount && itemCount != 0) {
            position = position % itemCount;
        }

        float offset;
        if (layoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
            offset = (getCurrentFrameLeft() - firstView.getX()) / firstView.getMeasuredWidth();
        } else {
            offset = (getCurrentFrameBottom() - firstView.getY()) / firstView.getMeasuredHeight();
        }

        if (offset >= 0 && offset <= 1 && position < itemCount) {
            indicator.onPageScrolled(position, offset);
        }
    }

    private int findCompletelyVisiblePosition() {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);

            float position = child.getX();
            int size = child.getMeasuredWidth();
            float currentStart = getCurrentFrameLeft();
            float currentEnd = getCurrentFrameRight();
            if (layoutManager.getOrientation() == LinearLayoutManager.VERTICAL) {
                position = child.getY();
                size = child.getMeasuredHeight();
                currentStart = getCurrentFrameTop();
                currentEnd = getCurrentFrameBottom();
            }

            if (position >= currentStart && position + size <= currentEnd) {
                RecyclerView.ViewHolder holder = recyclerView.findContainingViewHolder(child);
                if (holder != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                    return holder.getAdapterPosition();
                }
            }
        }
        return RecyclerView.NO_POSITION;
    }

    private boolean isInIdleState() {
        return findCompletelyVisiblePosition() != RecyclerView.NO_POSITION;
    }

    @Nullable
    private View findFirstVisibleView() {
        int childCount = layoutManager.getChildCount();
        if (childCount == 0) {
            return null;
        }

        View closestChild = null;
        int firstVisibleChild = Integer.MAX_VALUE;

        for (int i = 0; i < childCount; i++) {
            final View child = layoutManager.getChildAt(i);

            if (layoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
                int childStart = (int) child.getX();
                if (childStart + child.getMeasuredWidth() < firstVisibleChild
                        && childStart + child.getMeasuredWidth() >= getCurrentFrameLeft()) {
                    firstVisibleChild = childStart;
                    closestChild = child;
                }
            } else {
                int childStart = (int) child.getY();
                if (childStart + child.getMeasuredHeight() < firstVisibleChild
                        && childStart + child.getMeasuredHeight() >= getCurrentFrameBottom()) {
                    firstVisibleChild = childStart;
                    closestChild = child;
                }
            }
        }

        return closestChild;
    }

    private float getCurrentFrameLeft() {
        if (centered) {
            return (recyclerView.getMeasuredWidth() - getChildWidth()) / 2;
        } else {
            return currentPageOffset;
        }
    }

    private float getCurrentFrameRight() {
        if (centered) {
            return (recyclerView.getMeasuredWidth() - getChildWidth()) / 2 + getChildWidth();
        } else {
            return currentPageOffset + getChildWidth();
        }
    }

    private float getCurrentFrameTop() {
        if (centered) {
            return (recyclerView.getMeasuredHeight() - getChildHeight()) / 2;
        } else {
            return currentPageOffset;
        }
    }

    private float getCurrentFrameBottom() {
        if (centered) {
            return (recyclerView.getMeasuredHeight() - getChildHeight()) / 2 + getChildHeight();
        } else {
            return currentPageOffset + getChildHeight();
        }
    }

    private float getChildWidth() {
        if (measuredChildWidth == 0) {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                if (child.getMeasuredWidth() != 0) {
                    measuredChildWidth = child.getMeasuredWidth();
                    return measuredChildWidth;
                }
            }
        }
        return measuredChildWidth;
    }

    private float getChildHeight() {
        if (measuredChildHeight == 0) {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                if (child.getMeasuredHeight() != 0) {
                    measuredChildHeight = child.getMeasuredHeight();
                    return measuredChildHeight;
                }
            }
        }
        return measuredChildHeight;
    }
}
