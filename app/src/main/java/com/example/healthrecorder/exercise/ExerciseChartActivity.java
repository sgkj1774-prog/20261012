package com.example.healthrecorder.exercise;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.example.healthrecorder.DatabaseHelper;

public class ExerciseChartActivity extends AppCompatActivity {
    private final SimpleDateFormat FULL_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private TextView textRunKm, textRunMin;
    private TextView textLastMonthKm, textLastMonthMin;
    private TextView textYearKm, textYearMin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_chart);
        textRunKm = findViewById(R.id.textRunKm);
        textRunMin = findViewById(R.id.textRunMin);
        textLastMonthKm = findViewById(R.id.textLastMonthKm);
        textLastMonthMin = findViewById(R.id.textLastMonthMin);
        textYearKm = findViewById(R.id.textYearKm);
        textYearMin = findViewById(R.id.textYearMin);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        loadData();
    }

    private void loadData() {
        SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();

        // === 时间区间（左闭右开）===
        // 本月：本月 1 日 00:00 → 下月 1 日 00:00
        Calendar calMonth = startOfMonth(Calendar.getInstance(), 0);
        long startMonth = calMonth.getTimeInMillis();
        long endMonth = plusMonths(calMonth, 1).getTimeInMillis();

        // 上月：上月 1 日 00:00 → 本月 1 日 00:00
        Calendar calLastMonth = startOfMonth(Calendar.getInstance(), -1);
        long startLastMonth = calLastMonth.getTimeInMillis();
        long endLastMonth = startMonth;

        // 本年：1 月 1 日 00:00 → 明年 1 月 1 日 00:00
        Calendar calYear = Calendar.getInstance();
        calYear.set(Calendar.DAY_OF_YEAR, 1);
        clearTime(calYear);
        long startYear = calYear.getTimeInMillis();
        Calendar calYearEnd = (Calendar) calYear.clone();
        calYearEnd.add(Calendar.YEAR, 1);
        long endYear = calYearEnd.getTimeInMillis();

        float[] month = sumRunning(db, startMonth, endMonth);
        float[] lastMonth = sumRunning(db, startLastMonth, endLastMonth);
        float[] year = sumRunning(db, startYear, endYear);

        textRunKm.setText(String.format(Locale.US, "%.1f km", month[0]));
        textRunMin.setText(String.format(Locale.US, "%d min", (int) month[1]));
        textLastMonthKm.setText(String.format(Locale.US, "%.1f km", lastMonth[0]));
        textLastMonthMin.setText(String.format(Locale.US, "%d min", (int) lastMonth[1]));
        textYearKm.setText(String.format(Locale.US, "%.1f km", year[0]));
        textYearMin.setText(String.format(Locale.US, "%d min", (int) year[1]));

        // === 近30天折线图 ===
        // 用 LinkedHashMap 保证按日期顺序插入，避免折线图横轴乱序
        Map<String, Float> runKm = new java.util.LinkedHashMap<>();
        Map<String, Integer> runDur = new java.util.LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -i);
            String key = FULL_FMT.format(c.getTime());
            runKm.put(key, 0f);
            runDur.put(key, 0);
        }
        Calendar cal30 = Calendar.getInstance();
        cal30.add(Calendar.DAY_OF_YEAR, -29);
        cal30.set(Calendar.HOUR_OF_DAY, 0);
        cal30.set(Calendar.MINUTE, 0);
        cal30.set(Calendar.SECOND, 0);
        cal30.set(Calendar.MILLISECOND, 0);
        long start30 = cal30.getTimeInMillis();

        Cursor c = db.rawQuery(
                "SELECT date, distance, duration FROM exercise_records WHERE date>=? AND type=?",
                new String[]{String.valueOf(start30), String.valueOf(ExerciseRecord.TYPE_RUNNING)});
        if (c.moveToFirst()) {
            do {
                long date = c.getLong(0);
                float dist = c.getFloat(1);
                int dur = c.getInt(2);
                String key = FULL_FMT.format(new java.util.Date(date));
                if (runKm.containsKey(key)) runKm.put(key, runKm.get(key) + dist);
                if (runDur.containsKey(key)) runDur.put(key, runDur.get(key) + dur);
            } while (c.moveToNext());
        }
        c.close();
        db.close();

        // 绘制折线图
        drawLineChart(findViewById(R.id.lineChartRun), runKm, "#FF5722");
        drawLineChart(findViewById(R.id.lineChartRunDur), floatMap(runDur), "#2196F3");
    }

    /** 汇总指定区间内的跑步距离(km)与时长(分钟)，返回 [距离, 时长] */
    private float[] sumRunning(SQLiteDatabase db, long startInclusive, long endExclusive) {
        Cursor c = db.rawQuery(
                "SELECT SUM(distance), SUM(duration) FROM exercise_records "
                        + "WHERE date>=? AND date<? AND type=?",
                new String[]{String.valueOf(startInclusive), String.valueOf(endExclusive),
                        String.valueOf(ExerciseRecord.TYPE_RUNNING)});
        float km = 0f;
        int minutes = 0;
        if (c.moveToFirst()) {
            km = c.isNull(0) ? 0f : c.getFloat(0);
            minutes = c.isNull(1) ? 0 : c.getInt(1);
        }
        c.close();
        return new float[]{km, minutes};
    }

    private void clearTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    /** 取相对当前月偏移 offset 个月的那个月的 1 日 00:00（offset=0 本月，-1 上月） */
    private Calendar startOfMonth(Calendar base, int offset) {
        Calendar c = (Calendar) base.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        clearTime(c);
        c.add(Calendar.MONTH, offset);
        return c;
    }

    private Calendar plusMonths(Calendar base, int months) {
        Calendar c = (Calendar) base.clone();
        c.add(Calendar.MONTH, months);
        return c;
    }

    private Map<String, Float> floatMap(Map<String, Integer> src) {
        Map<String, Float> r = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : src.entrySet()) r.put(e.getKey(), (float) e.getValue());
        return r;
    }

    private void drawLineChart(LineChart chart, Map<String, Float> data, String colorHex) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<String, Float> e : data.entrySet()) {
            entries.add(new Entry(idx, e.getValue()));
            labels.add("");
            idx++;
        }
        LineDataSet set = new LineDataSet(entries, "");
        set.setColor(Color.parseColor(colorHex));
        set.setCircleColor(Color.parseColor(colorHex));
        set.setLineWidth(2f);
        set.setDrawFilled(true);
        set.setFillColor(Color.parseColor(colorHex + "33"));
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.setData(new LineData(set));
        chart.animateX(500);
        chart.invalidate();
    }
}
