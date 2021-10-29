package com.example.gitfpageindicator;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;

import java.util.ArrayList;

public class GiftPageIndicator extends View implements PageIndicator {
    private static final int INVALID_POINTER = -1;
    private static final String TAG = "GiftPageIndicator";
    //大圆半径
    private float mRadius;
    //中圆半径
    private float mMidRadius;
    //小圆半径
    private float mSmallRadius;

    //圆之间的间隔
    private float mDotSpace;

    private final Paint mPaintPageFill = new Paint(ANTI_ALIAS_FLAG);
    private final Paint mPaintStroke = new Paint(ANTI_ALIAS_FLAG);
    private final Paint mPaintFill = new Paint(ANTI_ALIAS_FLAG);

    protected ViewPager mViewPager;
    protected ViewPager.OnPageChangeListener mListener;
    protected int mCurrentPage = 0;
    protected int mSnapPage;
    protected float mPageOffset;
    protected int mScrollState;
    private boolean mCentered;

    public static final int MAX_SHOW_DOT = 10;

    private ArrayList<ValueAnimator> valueAnimators = new ArrayList<>();
    private SparseArray<Dot> mDots = new SparseArray<>();
    private float scrollAmount;

    public GiftPageIndicator(Context context) {
        this(context, null);
    }

    public GiftPageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.vpiCirclePageIndicatorStyle);
    }

    public GiftPageIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (isInEditMode()) return;

        //Load defaults from resources
        final Resources res = getResources();
        final int defaultPageColor = res.getColor(R.color.default_circle_indicator_page_color);
        final int defaultFillColor = res.getColor(R.color.default_circle_indicator_fill_color);
        final int defaultStrokeColor = res.getColor(R.color.default_circle_indicator_stroke_color);
        final float defaultStrokeWidth = res.getDimension(R.dimen.default_circle_indicator_stroke_width);
        final float defaultRadius = res.getDimension(R.dimen.default_circle_indicator_radius);
        final float defaultMidRadius = res.getDimension(R.dimen.default_mid_circle_indicator_radius);
        final float defaultSmallRadius = res.getDimension(R.dimen.default_small_circle_indicator_radius);
        final boolean defaultCentered = res.getBoolean(R.bool.default_circle_indicator_centered);
        //Retrieve styles attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CirclePageIndicator, defStyle, 0);
        mCentered = a.getBoolean(R.styleable.CirclePageIndicator_centered, defaultCentered);
        mPaintPageFill.setStyle(Style.FILL);
        mPaintPageFill.setColor(a.getColor(R.styleable.CirclePageIndicator_pageColor, defaultPageColor));
        mPaintStroke.setStyle(Style.STROKE);
        mPaintStroke.setColor(a.getColor(R.styleable.CirclePageIndicator_strokeColor, defaultStrokeColor));
        mPaintStroke.setStrokeWidth(a.getDimension(R.styleable.CirclePageIndicator_strokeWidth, defaultStrokeWidth));
        mPaintFill.setStyle(Style.FILL);
        mPaintFill.setColor(a.getColor(R.styleable.CirclePageIndicator_fillColor, defaultFillColor));
        mRadius = a.getDimension(R.styleable.CirclePageIndicator_radius, defaultRadius);
        mMidRadius =  a.getDimension(R.styleable.CirclePageIndicator_radius, defaultMidRadius);
        mSmallRadius =  a.getDimension(R.styleable.CirclePageIndicator_radius, defaultSmallRadius);

        //圆之间间隔默认为大圆半径
        mDotSpace = mRadius;

        a.recycle();
    }


    public int getViewPagerAdapterCount() {
        PagerAdapter adapter = mViewPager.getAdapter();
        if (adapter != null && adapter.getCount() > 0) {
            return adapter.getCount();
        }
        return 0;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mViewPager == null) {
            return;
        }
        final int count = getViewPagerAdapterCount();
        if (count == 0) {
            return;
        }

        if (mCurrentPage >= count) {
            setCurrentItem(count - 1);
            return;
        }

        float longPaddingBefore = getPaddingLeft();
        int shortPaddingBefore = getPaddingTop();
        final float shortOffset = shortPaddingBefore + mRadius;
        float dX = 0;
        float dY;
        //Draw stroked circles
        for (int iLoop = 0; iLoop < mDots.size(); iLoop++) {
            Dot dot = mDots.get(iLoop);
            //计算下个圆的坐标，应该等于上个圆的圆心加上上个圆的半径加上间隔加上当前圆的半径
            if(iLoop == 0){
                dX = dX + longPaddingBefore + dot.radius;
            }else {
                dX = dX + longPaddingBefore + mDotSpace + dot.radius;
            }
            dY = shortOffset;
            //判断是否被选择，选择到了需要画颜色
            if (!dot.selected) {
                canvas.drawCircle(dX - scrollAmount, dY, dot.radius, mPaintStroke);
            } else {
                canvas.drawCircle(dX - scrollAmount, dY, dot.radius, mPaintFill);
            }
            //画完之后加上当前半径，使得坐标位于圆的边上
            dX += dot.radius;
        }
    }


    @Override
    public void setViewPager(ViewPager view) {
        if (mViewPager == view) {
            return;
        }
        if (view.getAdapter() == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }
        mViewPager = view;
        mViewPager.addOnPageChangeListener(this);
        for (int i = 0; i < view.getAdapter() .getCount(); i++) {
            valueAnimators.add(new ValueAnimator());
            if (i == 0) {
                mDots.put(i, new Dot(mRadius, true));
            } else if (i == MAX_SHOW_DOT - 2) {
                mDots.put(i, new Dot(mMidRadius, false));
            } else if (i == MAX_SHOW_DOT - 1) {
                mDots.put(i, new Dot(mSmallRadius, false));
            } else {
                mDots.put(i, new Dot(mRadius, false));
            }
        }
        invalidate();
    }

    @Override
    public void setViewPager(ViewPager view, int initialPosition) {
        setViewPager(view);
        setCurrentItem(initialPosition);
    }


    public void setViewPagerWithoutListener(ViewPager view) {
        if (mViewPager == view) {
            return;
        }
        if (mViewPager != null) {
            mViewPager.setOnPageChangeListener(null);
        }
        if (view.getAdapter() == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }
        mViewPager = view;
        invalidate();
    }

    @Override
    public void setCurrentItem(int item) {
        if (mViewPager == null) {
            throw new IllegalStateException("ViewPager has not been bound.");
        }
        mViewPager.setCurrentItem(item);
        mCurrentPage = item;
        invalidate();
    }

    @Override
    public void notifyDataSetChanged() {
        invalidate();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mScrollState = state;
        if (mListener != null) {
            mListener.onPageScrollStateChanged(state);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mCurrentPage = position;
        invalidate();
        if (mListener != null) {
            mListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
            mCurrentPage = position;
            invalidate();
        }

        if (mListener != null) {
            mListener.onPageSelected(position);
        }
    }

    @Override
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        mListener = listener;
    }

    ValueAnimator scrollAnimator = new ValueAnimator();

    private void scrollToTarget(boolean forward) {
        scrollAnimator.cancel();
        if (forward) {
            scrollAnimator = ValueAnimator.ofFloat(0, mRadius * 3);
        } else {
            scrollAnimator = ValueAnimator.ofFloat(0, -mRadius * 3);
        }
        scrollAnimator.setDuration(200).addUpdateListener(animation -> {
            scrollAmount = (float) scrollAnimator.getAnimatedValue();
            invalidate();
        });
        scrollAnimator.start();
    }


    /*
     * (non-Javadoc)
     *
     * @see android.view.View#onMeasure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureLong(widthMeasureSpec), measureShort(heightMeasureSpec));
    }

    private int measureLong(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if ((specMode == MeasureSpec.EXACTLY) || (mViewPager == null)) {
            //We were told how big to be
            result = specSize;
        } else {
            //Calculate the width according the views count
            //大圆圈的数量
            int bigDotCount;
            //中等大小圆圈数量
            int midDotCount;
            //小圆圈数量
            int smallDotCount;
            if(mCurrentPage > MAX_SHOW_DOT - 1 && mCurrentPage < getViewPagerAdapterCount() - 3){
                bigDotCount = 6;
                midDotCount = 2;
                smallDotCount = 2;
            }else {
                bigDotCount = 8;
                midDotCount = 1;
                smallDotCount = 1;
            }
            result = (int) (getPaddingLeft() + getPaddingRight()
                    + (bigDotCount * 2 * mRadius) + (midDotCount * 2 * mMidRadius) + ( smallDotCount * 2 * mSmallRadius) + (MAX_SHOW_DOT - 1) * mRadius + 1);
            //Respect AT_MOST value if that was what is called for by measureSpec
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private int measureShort(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            //We were told how big to be
            result = specSize;
        } else {
            //Measure the height
            result = (int) (2 * mRadius + getPaddingTop() + getPaddingBottom() + 1);
            //Respect AT_MOST value if that was what is called for by measureSpec
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mCurrentPage = savedState.currentPage;
        mSnapPage = savedState.currentPage;
        requestLayout();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentPage = mCurrentPage;
        return savedState;
    }

    public void gotoNext() {
        if (mCurrentPage >= getViewPagerAdapterCount() - 1) {
        } else {
            mDots.get(mCurrentPage).setSelected(false);
            mCurrentPage++;
            if(mCurrentPage >= MAX_SHOW_DOT - 2){
                mDots.get(mCurrentPage).setSelected(true);
                scrollToTarget(true);
            } else {
                mDots.get(mCurrentPage).setSelected(true);
                invalidate();
            }
        }
        Log.d(TAG, " gotoNext mCurrentPage: " + mCurrentPage);
    }

    //圆点动画，从小到大和从大到小
    private void animateDots() {
        for (int i = 0; i < mDots.size(); i++) {
            ValueAnimator valueAnimator = mDots.get(i).getValueAnimator();
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            valueAnimator = ValueAnimator.ofFloat(mDots.get(i));
        }
    }


    public void gotoPreivous() {
        if (mCurrentPage <= 0) {
        } else {
            mDots.get(mCurrentPage).setSelected(false);
            mCurrentPage--;
            mDots.get(mCurrentPage).setSelected(true);
            scrollToTarget(false);
        }
        Log.d(TAG, " gotoPreivous mCurrentPage: " + mCurrentPage);
    }

    static class SavedState extends BaseSavedState {
        int currentPage;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentPage = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentPage);
        }

        @SuppressWarnings("UnusedDeclaration")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
