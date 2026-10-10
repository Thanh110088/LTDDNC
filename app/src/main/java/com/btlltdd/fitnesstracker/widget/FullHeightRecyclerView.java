package com.btlltdd.fitnesstracker.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/** RecyclerView nằm trong ScrollView vẫn đo đủ các dòng. */
public class FullHeightRecyclerView extends RecyclerView {
    public FullHeightRecyclerView(Context context) {
        this(context, null);
    }

    public FullHeightRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FullHeightRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setNestedScrollingEnabled(false);
        setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int expand = MeasureSpec.makeMeasureSpec(1 << 28, MeasureSpec.AT_MOST);
        super.onMeasure(widthSpec, expand);
    }
}
