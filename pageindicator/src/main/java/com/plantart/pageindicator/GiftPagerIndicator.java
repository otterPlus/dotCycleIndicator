package com.plantart.pageindicator;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

public class GiftPagerIndicator extends View {

    @IntDef({RecyclerView.HORIZONTAL, RecyclerView.VERTICAL})
    public @interface Orientation{}

    private int infiniteDotCount;

    private final int mDotMinimumSize;
    private final int mDotNormalSize;
    private final int mDotSelectedSize;
    private final int mSpaceBetweenDotCenters;
    private int mVisibleDotCount;
    private int mVisibleDotThreshold;
    private int mOrientation;

    private float mVisibleFramePosition;
    private float mVisibleFrameWidth;

    private float mFirstDotOffset;
    private SparseArray<Float> mDotScale;

    private int mItemCount;

    private final Paint mPaint;
    private final ArgbEvaluator mColorEvaluator = new ArgbEvaluator();

    @ColorInt
    private int mDotColor;

    @ColorInt
    private int mSelectedDotColor;

    private boolean mLooped;

    private Runnable mAttachRunnable;
    private GiftPagerAttach<?> mCurrentGiftAttach;

    private boolean mDotCountInitialized;

    public GiftPagerIndicator(Context context) {
        this(context, null);
    }

    public GiftPagerIndicator(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.GiftPagerIndicatorStyle);
    }

    public GiftPagerIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray attributes = context.obtainStyledAttributes(
                attrs, R.styleable.GiftPagerIndicator, defStyleAttr, R.style.GiftPagerIndicator);
        mDotColor = attributes.getColor(R.styleable.GiftPagerIndicator_gift_dotColor, 0);
        mSelectedDotColor = attributes.getColor(R.styleable.GiftPagerIndicator_gift_dotSelectedColor, mDotColor);
        mDotNormalSize = attributes.getDimensionPixelSize(R.styleable.GiftPagerIndicator_gift_dotSize, 0);
        mDotSelectedSize = attributes.getDimensionPixelSize(R.styleable.GiftPagerIndicator_gift_dotSelectedSize, 0);
        int dotMinimumSize = attributes.getDimensionPixelSize(R.styleable.GiftPagerIndicator_gift_dotMinimumSize, -1);
        this.mDotMinimumSize = dotMinimumSize <= mDotNormalSize ? dotMinimumSize : -1;

        mSpaceBetweenDotCenters = attributes.getDimensionPixelSize(R.styleable.GiftPagerIndicator_gift_dotSpacing, 0) + mDotNormalSize;
        mLooped = attributes.getBoolean(R.styleable.GiftPagerIndicator_gift_looped, false);
        int visibleDotCount = attributes.getInt(R.styleable.GiftPagerIndicator_gift_visibleDotCount, 0);
        setVisibleDotCount(visibleDotCount);
        mVisibleDotThreshold = attributes.getInt(R.styleable.GiftPagerIndicator_gift_visibleDotThreshold, 2);
        mOrientation = attributes.getInt(R.styleable.GiftPagerIndicator_gift_orientation, RecyclerView.HORIZONTAL);
        attributes.recycle();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        if (isInEditMode()) {
            setDotCount(visibleDotCount);
            onPageScrolled(visibleDotCount / 2, 0);
        }
    }

    public void setLooped(boolean mLooped) {
        this.mLooped = mLooped;
        reattach();
        invalidate();
    }

    @ColorInt
    public int getDotColor() {
        return mDotColor;
    }


    public void setDotColor(@ColorInt int color) {
        this.mDotColor = color;
        invalidate();
    }


    @ColorInt
    public int getSelectedDotColor() {
        return mSelectedDotColor;
    }


    public void setSelectedDotColor(@ColorInt int color) {
        this.mSelectedDotColor = color;
        invalidate();
    }

    public int getVisibleDotCount() {
        return mVisibleDotCount;
    }

    public void setVisibleDotCount(int mVisibleDotCount) {
        if (mVisibleDotCount % 2 == 0) {
            throw new IllegalArgumentException("visibleDotCount must be odd");
        }
        this.mVisibleDotCount = mVisibleDotCount;
        this.infiniteDotCount = mVisibleDotCount + 2;

        if (mAttachRunnable != null) {
            reattach();
        } else {
            requestLayout();
        }
    }

    public int getVisibleDotThreshold() {
        return mVisibleDotThreshold;
    }

    public void setVisibleDotThreshold(int mVisibleDotThreshold) {
        this.mVisibleDotThreshold = mVisibleDotThreshold;
        if (mAttachRunnable != null) {
            reattach();
        } else {
            requestLayout();
        }
    }

    @Orientation
    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(@Orientation int mOrientation) {
        this.mOrientation = mOrientation;
        if (mAttachRunnable != null) {
            reattach();
        } else {
            requestLayout();
        }
    }


    public void attachViewPager(@NonNull ViewPager pager) {
        attachViewPager(pager, new GiftViewPageAttach());
    }



    public void attachRecyclerView(@NonNull RecyclerView recyclerView) {
        attachViewPager(recyclerView, new GiftRecyclerViewAttach());
    }


    public void attachRecyclerView(@NonNull RecyclerView recyclerView, int currentPageOffset) {
        attachViewPager(recyclerView, new GiftRecyclerViewAttach(currentPageOffset));
    }


    public <T> void attachViewPager(@NonNull final T pager, @NonNull final GiftPagerAttach<T> giftPagerAttach) {
        detachFromPager();
        giftPagerAttach.attachPager(this, pager);
        mCurrentGiftAttach = giftPagerAttach;
        mAttachRunnable = () -> {
            mItemCount = -1;
            attachViewPager(pager, giftPagerAttach);
        };
    }


    public void detachFromPager() {
        if (mCurrentGiftAttach != null) {
            mCurrentGiftAttach.detachFromPager();
            mCurrentGiftAttach = null;
            mAttachRunnable = null;
        }
        mDotCountInitialized = false;
    }

    public void reattach() {
        if (mAttachRunnable != null) {
            mAttachRunnable.run();
            invalidate();
        }
    }


    public void onPageScrolled(int page, float offset) {
        if (offset < 0 || offset > 1) {
            throw new IllegalArgumentException("Offset must be [0, 1]");
        } else if (page < 0 || page != 0 && page >= mItemCount) {
            throw new IndexOutOfBoundsException("page must be [0, adapter.getItemCount())");
        }

        if (!mLooped || mItemCount <= mVisibleDotCount && mItemCount > 1) {
            mDotScale.clear();

            if (mOrientation == LinearLayout.HORIZONTAL) {
                scaleDotByOffset(page, offset);

                if (page < mItemCount - 1) {
                    scaleDotByOffset(page + 1, 1 - offset);
                } else if (mItemCount > 1) {
                    scaleDotByOffset(0, 1 - offset);
                }
            } else {
                scaleDotByOffset(page - 1, offset);
                scaleDotByOffset(page, 1 - offset);
            }

            invalidate();
        }
        if (mOrientation == LinearLayout.HORIZONTAL) {
            adjustFramePosition(offset, page);
        } else {
            adjustFramePosition(offset, page - 1);
        }
        invalidate();
    }


    public void setDotCount(int count) {
        initDots(count);
    }


    public void setCurrentPosition(int position) {
        if (position != 0 && (position < 0 || position >= mItemCount)) {
            throw new IndexOutOfBoundsException("Position must be [0, adapter.getItemCount()]");
        }
        if (mItemCount == 0) {
            return;
        }
        adjustFramePosition(0, position);
        updateScaleInIdleState(position);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth;
        int measuredHeight;

        if (mOrientation == LinearLayoutManager.HORIZONTAL) {
            if (isInEditMode()) {
                measuredWidth = (mVisibleDotCount - 1) * mSpaceBetweenDotCenters + mDotSelectedSize;
            } else {
                measuredWidth = mItemCount >= mVisibleDotCount
                        ? (int) mVisibleFrameWidth
                        : (mItemCount - 1) * mSpaceBetweenDotCenters + mDotSelectedSize;
            }
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);

            int desiredHeight = mDotSelectedSize;

            switch (heightMode) {
                case MeasureSpec.EXACTLY:
                    measuredHeight = heightSize;
                    break;
                case MeasureSpec.AT_MOST:
                    measuredHeight = Math.min(desiredHeight, heightSize);
                    break;
                case MeasureSpec.UNSPECIFIED:
                default:
                    measuredHeight = desiredHeight;
            }
        } else {
            if (isInEditMode()) {
                measuredHeight = (mVisibleDotCount - 1) * mSpaceBetweenDotCenters + mDotSelectedSize;
            } else {
                measuredHeight = mItemCount >= mVisibleDotCount
                        ? (int) mVisibleFrameWidth
                        : (mItemCount - 1) * mSpaceBetweenDotCenters + mDotSelectedSize;
            }

            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);

            int desiredWidth = mDotSelectedSize;

            switch (widthMode) {
                case MeasureSpec.EXACTLY:
                    measuredWidth = widthSize;
                    break;
                case MeasureSpec.AT_MOST:
                    measuredWidth = Math.min(desiredWidth, widthSize);
                    break;
                case MeasureSpec.UNSPECIFIED:
                default:
                    measuredWidth = desiredWidth;
            }
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int dotCount = getDotCount();
        if (dotCount < mVisibleDotThreshold) {
            return;
        }

        float scaleDistance = (mSpaceBetweenDotCenters + (mDotSelectedSize - mDotNormalSize) / 2f) * 0.7f;
        float smallScaleDistance = mDotSelectedSize / 2f;
        float centerScaleDistance = 6f / 7f * mSpaceBetweenDotCenters;


        int firstVisibleDotPos = (int) (mVisibleFramePosition - mFirstDotOffset) / mSpaceBetweenDotCenters;
        int lastVisibleDotPos = firstVisibleDotPos
                + (int) (mVisibleFramePosition + mVisibleFrameWidth - getDotOffsetAt(firstVisibleDotPos))
                / mSpaceBetweenDotCenters;

        if (firstVisibleDotPos == 0 && lastVisibleDotPos + 1 > dotCount) {
            lastVisibleDotPos = dotCount - 1;
        }

        for (int i = firstVisibleDotPos; i <= lastVisibleDotPos; i++) {

            float dot = getDotOffsetAt(i);
            if (dot >= mVisibleFramePosition && dot < mVisibleFramePosition + mVisibleFrameWidth) {
                float diameter;
                float scale;
                if (mLooped && mItemCount > mVisibleDotCount) {
                    float frameCenter = mVisibleFramePosition + mVisibleFrameWidth / 2;
                    if (dot >= frameCenter - centerScaleDistance
                            && dot <= frameCenter) {
                        scale = (dot - frameCenter + centerScaleDistance) / centerScaleDistance;
                    } else if (dot > frameCenter
                            && dot < frameCenter + centerScaleDistance) {
                        scale = 1 - (dot - frameCenter) / centerScaleDistance;
                    } else {
                        scale = 0;
                    }
                } else {
                    scale = getDotScaleAt(i);
                }
                diameter = mDotNormalSize + (mDotSelectedSize - mDotNormalSize) * scale;
                if (mItemCount > mVisibleDotCount) {
                    float currentScaleDistance;

                    if (!mLooped && (i < 2 || i >= dotCount - 2)) {
                        scaleDistance = (mSpaceBetweenDotCenters + (mDotSelectedSize - mDotNormalSize) / 2f);
                        smallScaleDistance = mDotSelectedSize / 2f;
                    } else {
                        scaleDistance = (mSpaceBetweenDotCenters + (mDotSelectedSize - mDotNormalSize) / 2f) * 1.6f;
                        smallScaleDistance = mDotSelectedSize;
                    }

                    if (!mLooped && (i == 0 || i == dotCount - 1)) {
                        currentScaleDistance = smallScaleDistance;
                    } else {
                        currentScaleDistance = scaleDistance;
                    }

                    int size = getWidth();
                    if (mOrientation == LinearLayoutManager.VERTICAL) {
                        size = getHeight();
                    }
                    if (dot - mVisibleFramePosition < currentScaleDistance) {
                        float calculatedDiameter = diameter * (dot - mVisibleFramePosition) / currentScaleDistance;
                        if (calculatedDiameter <= mDotMinimumSize) {
                            diameter = mDotMinimumSize;
                        } else if (calculatedDiameter < diameter) {
                            diameter = calculatedDiameter;
                        }
                    } else if (dot - mVisibleFramePosition > size - currentScaleDistance) {
                        float calculatedDiameter = diameter * (-dot + mVisibleFramePosition + size) / currentScaleDistance;
                        if (calculatedDiameter <= mDotMinimumSize) {
                            diameter = mDotMinimumSize;
                        } else if (calculatedDiameter < diameter) {
                            diameter = calculatedDiameter;
                        }
                    }
                }

                mPaint.setColor(calculateDotColor(scale));
                if (mOrientation == LinearLayoutManager.HORIZONTAL) {
                    canvas.drawCircle(dot - mVisibleFramePosition,
                            getMeasuredHeight() / 2f,
                            diameter / 2,
                            mPaint);
                } else {
                    canvas.drawCircle(getMeasuredWidth() / 2f,
                            dot - mVisibleFramePosition,
                            diameter / 2,
                            mPaint);
                }
            }
        }
    }

    @ColorInt
    private int calculateDotColor(float dotScale) {
        return (Integer) mColorEvaluator.evaluate(dotScale, mDotColor, mSelectedDotColor);
    }

    private void updateScaleInIdleState(int currentPos) {
        if (!mLooped || mItemCount < mVisibleDotCount) {
            mDotScale.clear();
            mDotScale.put(currentPos, 1f);
            invalidate();
        }
    }

    private void initDots(int itemCount) {
        if (this.mItemCount == itemCount && mDotCountInitialized) {
            return;
        }
        this.mItemCount = itemCount;
        mDotCountInitialized = true;
        mDotScale = new SparseArray<>();

        if (itemCount < mVisibleDotThreshold) {
            requestLayout();
            invalidate();
            return;
        }

        mFirstDotOffset = mLooped && this.mItemCount > mVisibleDotCount ? 0 : mDotSelectedSize / 2f;
        mVisibleFrameWidth = (mVisibleDotCount - 1) * mSpaceBetweenDotCenters + mDotSelectedSize;

        requestLayout();
        invalidate();
    }

    private int getDotCount() {
        if (mLooped && mItemCount > mVisibleDotCount) {
            return infiniteDotCount;
        } else {
            return mItemCount;
        }
    }

    private void adjustFramePosition(float offset, int pos) {
        if (mItemCount <= mVisibleDotCount) {
            mVisibleFramePosition = 0;
        } else if (!mLooped) {
            float center = getDotOffsetAt(pos) + mSpaceBetweenDotCenters * offset;
            mVisibleFramePosition = center - mVisibleFrameWidth / 2;
            int firstCenteredDotIndex = mVisibleDotCount / 2;
            float lastCenteredDot = getDotOffsetAt(getDotCount() - 1 - firstCenteredDotIndex);
            if (mVisibleFramePosition + mVisibleFrameWidth / 2 < getDotOffsetAt(firstCenteredDotIndex)) {
                mVisibleFramePosition = getDotOffsetAt(firstCenteredDotIndex) - mVisibleFrameWidth / 2;
            } else if (mVisibleFramePosition + mVisibleFrameWidth / 2 > lastCenteredDot) {
                mVisibleFramePosition = lastCenteredDot - mVisibleFrameWidth / 2;
            }
        } else {
            float center = getDotOffsetAt(infiniteDotCount / 2) + mSpaceBetweenDotCenters * offset;
            mVisibleFramePosition = center - mVisibleFrameWidth / 2;
        }
    }

    private void scaleDotByOffset(int position, float offset) {
        if (mDotScale == null || getDotCount() == 0) {
            return;
        }
        setDotScaleAt(position, 1 - Math.abs(offset));
    }

    private float getDotOffsetAt(int index) {
        return mFirstDotOffset + index * mSpaceBetweenDotCenters;
    }

    private float getDotScaleAt(int index) {
        Float scale = mDotScale.get(index);
        if (scale != null) {
            return scale;
        }
        return 0;
    }

    private void setDotScaleAt(int index, float scale) {
        if (scale == 0) {
            mDotScale.remove(index);
        } else {
            mDotScale.put(index, scale);
        }
    }
}
