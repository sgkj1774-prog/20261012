package com.example.healthrecorder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class HomeChartView extends LinearLayout {

    private CombinedChart chart;
    private TextView tvNoData;
    private TextView tvLegend;
    private TextView tvHint;
    private CardView tooltipCard;
    private TextView tvTooltip;

    private List<String> displayDates = new ArrayList<>();
    private boolean hasWeight = false;
    private boolean hasExercise = false;

    public HomeChartView(Context context) { super(context); init(context); }
    public HomeChartView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(context); }
    public HomeChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init(context);
    }

    private void init(Context ctx) {
        setOrientation(VERTICAL);

        float density = ctx.getResources().getDisplayMetrics().density;

        // 图例
        tvLegend = new TextView(ctx);
        tvLegend.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        tvLegend.setPadding(dpToPx(12, density), dpToPx(4, density), dpToPx(12, density), 0);
        tvLegend.setTextSize(11f);
        tvLegend.setTextColor(Color.parseColor("#999999"));
        tvLegend.setGravity(Gravity.CENTER_HORIZONTAL);
        addView(tvLegend);

        // tooltip 浮层（初始隐藏）
        tooltipCard = new CardView(ctx);
        tooltipCard.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        tooltipCard.setCardBackgroundColor(Color.parseColor("#333333"));
        tooltipCard.setCardElevation(8);
        tooltipCard.setRadius(dpToPx(8, density));
        tooltipCard.setVisibility(View.GONE);
        tvTooltip = new TextView(ctx);
        tvTooltip.setTextSize(12f);
        tvTooltip.setTextColor(Color.WHITE);
        tvTooltip.setPadding(dpToPx(10, density), dpToPx(6, density), dpToPx(10, density), dpToPx(6, density));
        tooltipCard.addView(tvTooltip);
        addView(tooltipCard);

        // 图表
        chart = new SafeCombinedChart(ctx);
        chart.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, (int)(220 * density)));
        chart.setBackgroundColor(Color.WHITE);
        addView(chart);

        // 提示文字
        tvHint = new TextView(ctx);
        tvHint.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        tvHint.setPadding(dpToPx(12, density), dpToPx(4, density), dpToPx(12, density), dpToPx(4, density));
        tvHint.setTextSize(11f);
        tvHint.setTextColor(Color.parseColor("#888888"));
        tvHint.setGravity(Gravity.CENTER_HORIZONTAL);
        addView(tvHint);

        // 无数据提示
        tvNoData = new TextView(ctx);
        tvNoData.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, (int)(220 * density)));
        tvNoData.setGravity(Gravity.CENTER);
        tvNoData.setTextSize(13f);
        tvNoData.setTextColor(Color.parseColor("#BBBBBB"));
        tvNoData.setText("暂无趋势数据\n记录体重或运动后自动显示");
        tvNoData.setBackgroundColor(Color.WHITE);
        tvNoData.setVisibility(View.GONE);
        addView(tvNoData);

        setupChart();
    }

    private int dpToPx(int dp, float density) {
        return Math.round(dp * density);
    }

    private void setupChart() {
        if (chart == null) return;

        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setHighlightFullBarEnabled(false);
        chart.getLegend().setEnabled(false);

        // 右轴：运动时长（分钟），橙色
        chart.getAxisRight().setEnabled(true);
        chart.getAxisRight().setTextColor(Color.parseColor("#FF9800"));
        chart.getAxisRight().setAxisMinimum(0f);
        chart.getAxisRight().setGridColor(Color.parseColor("#FFE0B2"));
        chart.getAxisRight().setTextSize(9f);
        chart.getAxisRight().setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return (int) value + "min";
            }
        });

        // 左轴：体重（kg），蓝色
        chart.getAxisLeft().setEnabled(true);
        chart.getAxisLeft().setTextColor(Color.parseColor("#2196F3"));
        chart.getAxisLeft().setGridColor(Color.parseColor("#BBDEFB"));
        chart.getAxisLeft().setTextSize(9f);
        chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1fkg", value);
            }
        });

        // X轴
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#999999"));
        xAxis.setTextSize(9f);
        xAxis.setLabelRotationAngle(-30f);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.parseColor("#DDDDDD"));

        // 交互：启用缩放 + 高亮
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleXEnabled(true);   // 允许横向缩放
        chart.setScaleYEnabled(false);  // 纵向不缩放
        chart.setPinchZoom(true);       // 双指缩放
        chart.setHighlightPerTapEnabled(true);
        chart.setHighlightPerDragEnabled(true);
        chart.setNoDataText("暂无趋势数据");
        chart.setNoDataTextColor(Color.parseColor("#BBBBBB"));

        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int idx = (int) e.getX();
                String dateLabel = (idx >= 0 && idx < displayDates.size()) ? displayDates.get(idx) : "";
                String label;
                if (h.getDataSetIndex() == 0) {
                    label = dateLabel + "  体重：" + String.format(Locale.getDefault(), "%.1fkg", e.getY());
                } else {
                    label = dateLabel + "  运动：" + (int) e.getY() + "min";
                }
                showTooltip(label, h.getXPx(), h.getYPx());
            }

            @Override
            public void onNothingSelected() {
                hideTooltip();
            }
        });

        chart.animateY(600);
    }

    private void showTooltip(String text, float xPx, float yPx) {
        tvTooltip.setText(text);
        tooltipCard.setVisibility(View.VISIBLE);
        int parentW = ((ViewGroup)getParent()).getWidth();
        int cardW = tooltipCard.getMeasuredWidth();
        int cardH = tooltipCard.getMeasuredHeight();
        if (cardW == 0) cardW = 180;

        int left = (int) xPx - cardW / 2;
        left = Math.max(8, Math.min(left, parentW - cardW - 8));

        int top = (int) yPx - cardH - 16;
        if (top < 8) top = (int) yPx + 16;

        tooltipCard.setTranslationX(left);
        tooltipCard.setTranslationY(top);
    }

    private void hideTooltip() {
        tooltipCard.setVisibility(View.GONE);
    }

    public void setData(List<Record> weightData,
                        List<com.example.healthrecorder.exercise.ExerciseRecord> exerciseData) {
        try {
            hasWeight = weightData != null && !weightData.isEmpty();
            hasExercise = exerciseData != null && !exerciseData.isEmpty();

            if (!hasWeight && !hasExercise) {
                chart.setVisibility(View.GONE);
                tvNoData.setVisibility(View.GONE);
                tvLegend.setVisibility(View.GONE);
                tvHint.setVisibility(View.GONE);
                hideTooltip();
                return;
            }

            chart.setVisibility(View.VISIBLE);
            tvLegend.setVisibility(View.VISIBLE);
            tvHint.setVisibility(View.VISIBLE);
            tvNoData.setVisibility(View.GONE);

            // 图例
            StringBuilder legendSb = new StringBuilder();
            if (hasWeight) legendSb.append("● 体重 (左轴, kg)");
            if (hasWeight && hasExercise) legendSb.append("   |   ");
            if (hasExercise) legendSb.append("● 运动时长 (右轴, min)");
            legendSb.append("   （点击查看详情）");
            tvLegend.setText(legendSb.toString());

            // Y轴启用控制
            chart.getAxisLeft().setEnabled(hasWeight);
            chart.getAxisRight().setEnabled(hasExercise);

            // 收集日期
            LinkedHashSet<String> dateSet = new LinkedHashSet<>();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());
            if (hasWeight) {
                for (Record r : weightData) {
                    if (r.hasWeight()) dateSet.add(sdf.format(r.getDate()));
                }
            }
            if (hasExercise) {
                for (com.example.healthrecorder.exercise.ExerciseRecord r : exerciseData) {
                    dateSet.add(sdf.format(r.getDate()));
                }
            }

            displayDates.clear();
            displayDates.addAll(dateSet);
            final int displaySize = displayDates.size();

            // 超过12个日期点自动启用横向缩放
            chart.setScaleXEnabled(displaySize > 12);

            chart.getXAxis().setValueFormatter(new ValueFormatter() {
                @Override public String getFormattedValue(float value) {
                    int idx = (int) value;
                    if (idx >= 0 && idx < displaySize) return displayDates.get(idx);
                    return "";
                }
            });
            chart.getXAxis().setLabelCount(Math.min(6, displaySize), true);

            // 构建 Entry
            List<Entry> weightEntries = new ArrayList<>();
            List<Entry> exerciseEntries = new ArrayList<>();

            if (hasWeight) {
                for (Record r : weightData) {
                    if (r.hasWeight()) {
                        int idx = displayDates.indexOf(sdf.format(r.getDate()));
                        if (idx >= 0) weightEntries.add(new Entry(idx, r.getWeight()));
                    }
                }
            }
            if (hasExercise) {
                for (com.example.healthrecorder.exercise.ExerciseRecord r : exerciseData) {
                    int idx = displayDates.indexOf(sdf.format(r.getDate()));
                    if (idx >= 0) exerciseEntries.add(new Entry(idx, r.getDuration()));
                }
            }

            // 动态Y轴范围
            float weightMin = 0, weightMax = 100, exerciseMax = 60;
            if (!weightEntries.isEmpty()) {
                float minW = Float.MAX_VALUE, maxW = Float.MIN_VALUE;
                for (Entry e : weightEntries) {
                    if (e.getY() < minW) minW = e.getY();
                    if (e.getY() > maxW) maxW = e.getY();
                }
                float pad = Math.max(1f, (maxW - minW) * 0.15f);
                weightMin = minW - pad;
                weightMax = maxW + pad;
                chart.getAxisLeft().setAxisMinimum(weightMin);
                chart.getAxisLeft().setAxisMaximum(weightMax);
            }
            if (!exerciseEntries.isEmpty()) {
                float maxE = Float.MIN_VALUE;
                for (Entry e : exerciseEntries) {
                    if (e.getY() > maxE) maxE = e.getY();
                }
                exerciseMax = maxE * 1.2f;
                chart.getAxisRight().setAxisMaximum(Math.max(exerciseMax, 30f));
            }

            // 提示文字
            tvHint.setText(buildHintText(weightEntries, exerciseEntries));

            CombinedData combinedData = new CombinedData();
            LineData lineData = new LineData();

            if (!weightEntries.isEmpty()) {
                LineDataSet ws = makeLineDataSet(weightEntries, "#2196F3",
                        YAxis.AxisDependency.LEFT);
                lineData.addDataSet(ws);
            }
            if (!exerciseEntries.isEmpty()) {
                LineDataSet es = makeLineDataSet(exerciseEntries, "#FF9800",
                        YAxis.AxisDependency.RIGHT);
                lineData.addDataSet(es);
            }

            combinedData.setData(lineData);
            chart.setData(combinedData);
            chart.invalidate();
            hideTooltip();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LineDataSet makeLineDataSet(List<Entry> entries, String colorHex,
                                         YAxis.AxisDependency dep) {
        LineDataSet set = new LineDataSet(entries, "");
        set.setColor(Color.parseColor(colorHex));
        set.setLineWidth(2.2f);
        set.setCircleRadius(3.5f);
        set.setCircleColor(Color.parseColor(colorHex));
        set.setCircleHoleColor(Color.WHITE);
        set.setCircleHoleRadius(1.5f);
        set.setDrawCircleHole(true);
        set.setDrawValues(false);
        set.setAxisDependency(dep);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.15f);
        set.setDrawFilled(true);
        set.setFillColor(Color.parseColor(colorHex));
        set.setFillAlpha(30);
        // 数据点少时显示圆点，多时隐藏以减少视觉干扰
        set.setDrawCircles(entries.size() <= 30);
        return set;
    }

    private String buildHintText(List<Entry> weightEntries, List<Entry> exerciseEntries) {
        StringBuilder sb = new StringBuilder();
        if (!weightEntries.isEmpty()) {
            float first = weightEntries.get(0).getY();
            float last = weightEntries.get(weightEntries.size() - 1).getY();
            float diff = last - first;
            if (diff < -0.5f) sb.append("📉 体重持续下降，继续加油！");
            else if (diff > 0.5f) sb.append("📈 体重有所上升，注意饮食控制");
            else sb.append("⚖️ 体重基本稳定");
        }
        if (!exerciseEntries.isEmpty()) {
            float totalMin = 0f;
            for (Entry e : exerciseEntries) totalMin += e.getY();
            int days = exerciseEntries.size();
            int avgMin = Math.round(totalMin / days);
            if (!sb.toString().isEmpty()) sb.append("  |  ");
            if (avgMin >= 30) sb.append("🏃 运动充足，平均每天").append(avgMin).append("分钟");
            else if (avgMin > 0) sb.append("💪 坚持运动，平均每天").append(avgMin).append("分钟");
            else sb.append("⏰ 今天还没有运动记录");
        }
        return sb.toString();
    }
}
