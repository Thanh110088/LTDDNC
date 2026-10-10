package com.btlltdd.fitnesstracker.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.btlltdd.fitnesstracker.R;

import java.util.Locale;

/** Biểu đồ cột đơn giản, không thêm thư viện ngoài. */
public class WeekChartView extends View {
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private float[] values = new float[0];
    private String[] labels = new String[0];

    public WeekChartView(Context context) {
        this(context, null);
    }

    public WeekChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeekChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        barPaint.setColor(ContextCompat.getColor(context, R.color.primary));
        textPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sp(11));
        emptyPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
        emptyPaint.setTextSize(sp(14));
    }

    public void setData(String[] labels, float[] values) {
        this.labels = labels == null ? new String[0] : labels;
        this.values = values == null ? new float[0] : values;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.length == 0) {
            return;
        }
        float max = 0f;
        for (float value : values) {
            max = Math.max(max, value);
        }
        if (max <= 0f) {
            canvas.drawText("Chưa có buổi tập trong khoảng này", getWidth() / 2f, getHeight() / 2f, emptyPaint);
            return;
        }
        float pad = dp(8);
        float labelH = dp(22);
        float top = dp(18);
        float slot = (getWidth() - pad * 2) / values.length;
        float barWidth = slot * 0.52f;
        float bottom = getHeight() - labelH;
        for (int i = 0; i < values.length; i++) {
            float cx = pad + slot * i + slot / 2f;
            float ratio = values[i] / max;
            float barHeight = Math.max(dp(4), (bottom - top) * ratio);
            bar.set(cx - barWidth / 2f, bottom - barHeight, cx + barWidth / 2f, bottom);
            canvas.drawRoundRect(bar, dp(8), dp(8), barPaint);
            if (values[i] > 0) {
                canvas.drawText(String.format(Locale.US, "%.0f", values[i]), cx, bar.top - dp(4), textPaint);
            }
            String label = i < labels.length ? labels[i] : "";
            canvas.drawText(label, cx, getHeight() - dp(4), textPaint);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
