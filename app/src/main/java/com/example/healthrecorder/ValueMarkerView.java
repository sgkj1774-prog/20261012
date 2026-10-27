package com.example.healthrecorder;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * 图表点击气泡：显示记录的时间与数值。
 * Entry 的 x 值对应传入 records 列表的下标。
 */
public class ValueMarkerView extends MarkerView {

    private final TextView tvTime;
    private final TextView tvValue;
    private final List<Record> records;
    private final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public ValueMarkerView(Context context, List<Record> records) {
        super(context, R.layout.marker_value);
        this.records = records;
        tvTime = findViewById(R.id.tvMarkerTime);
        tvValue = findViewById(R.id.tvMarkerValue);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int idx = (int) e.getX();
        if (idx >= 0 && idx < records.size()) {
            Record r = records.get(idx);
            tvTime.setText(TIME_FMT.format(r.getDate()));
            if (r.hasWeight()) {
                tvValue.setText(String.format(Locale.US, "体重 %.1f kg", r.getWeight()));
            } else if (r.hasBloodPressure()) {
                tvValue.setText("血压 " + r.getSystolic() + "/" + r.getDiastolic() + " mmHg");
            } else {
                tvValue.setText("");
            }
        } else {
            tvTime.setText("");
            tvValue.setText("");
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // 气泡水平居中于数据点，垂直悬停在点上方
        return new MPPointF(-getWidth() / 2f, -getHeight() - 14);
    }
}
