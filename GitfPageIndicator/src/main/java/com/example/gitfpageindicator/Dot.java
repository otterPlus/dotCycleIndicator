package com.example.gitfpageindicator;

import android.animation.ValueAnimator;

public class Dot {
    float radius = 0;
    boolean selected = false;
    ValueAnimator valueAnimator;

    public Dot(float radius,boolean select) {
        setRadius(radius);
        setSelected(select);
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public boolean getSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public ValueAnimator getValueAnimator() {
        return valueAnimator;
    }

    public void setValueAnimator(ValueAnimator valueAnimator) {
        this.valueAnimator = valueAnimator;
    }

    @Override
    public String toString() {
        return "Dot{" +
                "radius=" + radius +
                ", selected=" + selected +
                '}';
    }
}
