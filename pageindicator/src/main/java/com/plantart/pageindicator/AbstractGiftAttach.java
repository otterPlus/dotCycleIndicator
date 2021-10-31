package com.plantart.pageindicator;

public abstract class AbstractGiftAttach<T> implements GiftPagerAttach<T> {

    public void updateIndicatorOnPagerScrolled(GiftPagerIndicator indicator, int position, float positionOffset) {
        final float offset;
        if (positionOffset < 0) {
            offset = 0;
        } else if (positionOffset > 1) {
            offset = 1;
        } else {
            offset = positionOffset;
        }
        indicator.onPageScrolled(position, offset);
    }
}
