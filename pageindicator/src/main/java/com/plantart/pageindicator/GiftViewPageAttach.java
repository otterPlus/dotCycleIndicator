package com.plantart.pageindicator;

import android.animation.ValueAnimator;
import android.database.DataSetObserver;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class GiftViewPageAttach extends AbstractGiftAttach<ViewPager> {

    private static final String TAG = "GiftViewPageAttach";
    private DataSetObserver dataSetObserver;
    private ViewPager.OnPageChangeListener onPageChangeListener;
    private ViewPager pager;
    private PagerAdapter pagerAdapter;
    private GiftPagerIndicator mGiftPagerIndicator;

    @Override
    public void attachPager(@NonNull final GiftPagerIndicator indicator, @NonNull final ViewPager pager) {
        pagerAdapter = pager.getAdapter();
        if (pagerAdapter == null) {
            throw new IllegalStateException("Set adapter before call attachPager() method");
        }

        this.pager = pager;
        mGiftPagerIndicator = indicator;
        updateIndicatorDotsAndPosition(indicator);

        dataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                indicator.reattach();
            }

            @Override
            public void onInvalidated() {
                onChanged();
            }
        };
        pagerAdapter.registerDataSetObserver(dataSetObserver);

        onPageChangeListener = new ViewPager.OnPageChangeListener() {

            boolean idleState = true;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixel) {
                updateIndicatorOnPagerScrolled(indicator, position, positionOffset);
            }

            @Override
            public void onPageSelected(int position) {
                if (idleState) {
                    updateIndicatorDotsAndPosition(indicator);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                idleState = state == ViewPager.SCROLL_STATE_IDLE;
            }
        };
        pager.addOnPageChangeListener(onPageChangeListener);
    }

    @Override
    public void detachFromPager() {
        pagerAdapter.unregisterDataSetObserver(dataSetObserver);
        pager.removeOnPageChangeListener(onPageChangeListener);
    }

    private void updateIndicatorDotsAndPosition(GiftPagerIndicator indicator) {
        updateIndicatorDotsAndPosition(indicator, pager.getCurrentItem());
    }

    private void updateIndicatorDotsAndPosition(GiftPagerIndicator indicator, int currentPosition) {
        indicator.setDotCount(pagerAdapter.getCount());
        indicator.setCurrentPosition(currentPosition);
    }


    private final AtomicInteger mCurrentPage = new AtomicInteger(0);

    public int goToIndex(int targetPage) {
        if (mCurrentPage.get() == targetPage) {
            Log.w(TAG, "target page can not same current page");
        } else if (mCurrentPage.get() < targetPage) {
            Log.d(TAG, "forward go to target page :" + targetPage);
            AtomicInteger previous = new AtomicInteger(mCurrentPage.get());
            mCurrentPage.set(targetPage);
            if (mCurrentPage.get() > pagerAdapter.getCount() - 1) {
                mCurrentPage.set(0);
                updateIndicatorDotsAndPosition(mGiftPagerIndicator, mCurrentPage.get());
            } else {
                ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
                valueAnimator.setDuration(200).addUpdateListener(valueAnimator1 -> {
                    mGiftPagerIndicator.onPageScrolled(previous.get(), (Float) valueAnimator1.getAnimatedValue());
                    if ((Float) valueAnimator1.getAnimatedValue() == 1f) {
                        updateIndicatorDotsAndPosition(mGiftPagerIndicator, mCurrentPage.get());
                    }
                });
                valueAnimator.start();
            }
        } else {
            Log.d(TAG, "previous go to target page : " + targetPage);
            mCurrentPage.set(targetPage);
            if (mCurrentPage.get() < 0) {
                mCurrentPage.set(pagerAdapter.getCount() - 1);
                updateIndicatorDotsAndPosition(mGiftPagerIndicator, mCurrentPage.get());
            } else {
                ValueAnimator valueAnimator = ValueAnimator.ofFloat(1, 0);
                valueAnimator.setDuration(200).addUpdateListener(valueAnimator1 -> {
                    mGiftPagerIndicator.onPageScrolled(mCurrentPage.get(), (Float) valueAnimator1.getAnimatedValue());
                    if ((Float) valueAnimator1.getAnimatedValue() == 0) {
                        updateIndicatorDotsAndPosition(mGiftPagerIndicator, mCurrentPage.get());
                    }
                });
                valueAnimator.start();
            }
        }
        return mCurrentPage.get();
    }

}
