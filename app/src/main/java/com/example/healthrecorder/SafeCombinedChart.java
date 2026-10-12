package com.example.healthrecorder;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.CombinedChart;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * CombinedChart 安全子类，在 onDraw 层捕获 MPAndroidChart v3.1.0 的
 * NegativeArraySizeException bug（generateTransformedValuesLine 中数组分配负数）。
 *
 * 捕获后自动重试 3 次（间距 500ms），仍失败则只日志不崩溃。
 */
public class SafeCombinedChart extends CombinedChart {

    private static final int MAX_RETRY = 3;
    private final AtomicInteger retryCount = new AtomicInteger(0);

    public SafeCombinedChart(Context context) {
        super(context);
    }

    public SafeCombinedChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SafeCombinedChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            super.onDraw(canvas);
            retryCount.set(0); // 成功绘制则重置重试计数
        } catch (NegativeArraySizeException e) {
            e.printStackTrace();

            // 限制重试次数，防止死循环
            int r = retryCount.getAndIncrement();
            if (r < MAX_RETRY) {
                // 延迟重用以让布局/动画完成
                postDelayed(this::invalidate, 500L * (r + 1));
            } else {
                android.util.Log.e("SafeCombinedChart",
                        "NegativeArraySizeException after " + MAX_RETRY + " retries, giving up.");
                retryCount.set(0);
            }
        } catch (Exception e) {
            // 兜住所有其他异常，不崩溃
            e.printStackTrace();
        }
    }
}
