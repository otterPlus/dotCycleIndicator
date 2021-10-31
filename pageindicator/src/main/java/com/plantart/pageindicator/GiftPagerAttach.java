package com.plantart.pageindicator;

import androidx.annotation.NonNull;

public interface GiftPagerAttach<T> {

    void attachPager(@NonNull GiftPagerIndicator indicator, @NonNull T pager);

    void detachFromPager();
}
