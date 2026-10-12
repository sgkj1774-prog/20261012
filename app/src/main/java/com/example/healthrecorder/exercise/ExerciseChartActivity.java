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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_chart);
        textRunKm = findViewById(R.id.textRunKm);
        textRunMin = findViewById(R.id.textRunMin);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        loadData();
    }

    private void loadData() {
        SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();

        // === 本月跑步统计 ===
        Calendar calMonth = Calendar.getInstance();
        calMonth.set(Calendar.DAY_OF_MONTH, 1);
        calMonth.set(Calendar.HOUR_OF_DAY, 0);
        calMonth.set(Calendar.MINUTE, 0);
        calMonth.set(Calendar.SECOND, 0);
        calMonth.set(Calendar.MILLISECOND, 0);
        long startMonth = calMonth.getTimeInMillis();

        Cursor monthCursor = db.rawQuery(
                "SELECT SUM(distance), SUM(duration) FROM exercise_records WHERE date>=? AND type=?",
                new String[]{String.valueOf(startMonth), String.valueOf(ExerciseRecord.TYPE_RUNNING)});
        float monthRunKm = 0f;
        int monthRunMin = 0;
        if (monthCursor.moveToFirst()) {
            monthRunKm = monthCursor.isNull(0) ? 0f : monthCursor.getFloat(0);
            monthRunMin = monthCursor.isNull(1) ? 0 : monthCursor.getInt(1);
        }
        monthCursor.close();

        textRunKm.setText(String.format(Locale.US, "%.1f km", monthRunKm));
        textRunMin.setText(String.format(Locale.US, "%d min", monthRunMin));

        // === 近30天折线图 ===
        Map<String, Float> runKm = new HashMap<>();
        Map<String, Integer> runDur = new HashMap<>();
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

    private Map<String, Float> floatMap(Map<String, Integer> src) {
        Map<String, Float> r = new HashMap<>();
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
