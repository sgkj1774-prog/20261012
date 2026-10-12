package com.example.healthrecorder.teeth;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.example.healthrecorder.DatabaseHelper;

public class TeethChartActivity extends AppCompatActivity {
    private final SimpleDateFormat DAY_FMT = new SimpleDateFormat("MM-dd", Locale.getDefault());
    private final SimpleDateFormat FULL_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private TextView textWeekly, textMonth, textYear;
    private BarChart barChart;
    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teeth_chart);
        textWeekly = findViewById(R.id.textWeekly);
        textMonth = findViewById(R.id.textMonth);
        textYear = findViewById(R.id.textYear);
        barChart = findViewById(R.id.barChart);
        lineChart = findViewById(R.id.lineChart);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        setupCharts();
        loadData();
    }

    private void setupCharts() {
        for (BarChart c : new BarChart[]{barChart}) {
            c.getDescription().setEnabled(false);
            c.getLegend().setEnabled(true);
            c.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            c.getXAxis().setDrawGridLines(false);
            c.getXAxis().setGranularity(1f);
            c.getAxisRight().setEnabled(false);
            c.getAxisLeft().setAxisMinimum(0f);
            c.getAxisLeft().setGranularity(1f);
            c.animateY(500);
        }
        for (LineChart c : new LineChart[]{lineChart}) {
            c.getDescription().setEnabled(false);
            c.getLegend().setEnabled(false);
            c.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            c.getXAxis().setDrawGridLines(false);
            c.getXAxis().setGranularity(1f);
            c.getAxisRight().setEnabled(false);
            c.animateX(500);
        }
    }

    private void loadData() {
        SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();

        // 近7天
        Map<String, int[]> dayMap7 = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -i);
            dayMap7.put(FULL_FMT.format(c.getTime()), new int[]{0, 0});
        }
        Calendar cal7 = Calendar.getInstance();
        cal7.add(Calendar.DAY_OF_YEAR, -6);
        cal7.set(Calendar.HOUR_OF_DAY, 0); cal7.set(Calendar.MINUTE, 0); cal7.set(Calendar.SECOND, 0);
        long start7 = cal7.getTimeInMillis();

        Cursor c7 = db.rawQuery("SELECT date, period FROM teeth_records WHERE date>=?", new String[]{String.valueOf(start7)});
        if (c7.moveToFirst()) {
            do {
                Date d = new Date(c7.getLong(0));
                String key = FULL_FMT.format(d);
                if (dayMap7.containsKey(key)) {
                    int period = c7.getInt(1);
                    dayMap7.get(key)[period]++;
                }
            } while (c7.moveToNext());
        }
        c7.close();

        // 近30天
        Map<String, Float> day30Map = new HashMap<>();
        for (int i = 29; i >= 0; i--) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -i);
            day30Map.put(FULL_FMT.format(c.getTime()), 0f);
        }
        Calendar cal30 = Calendar.getInstance();
        cal30.add(Calendar.DAY_OF_YEAR, -29);
        cal30.set(Calendar.HOUR_OF_DAY, 0); cal30.set(Calendar.MINUTE, 0); cal30.set(Calendar.SECOND, 0);
        long start30 = cal30.getTimeInMillis();

        Cursor c30 = db.rawQuery("SELECT date FROM teeth_records WHERE date>=?", new String[]{String.valueOf(start30)});
        if (c30.moveToFirst()) {
            do {
                String key = FULL_FMT.format(new Date(c30.getLong(0)));
                if (day30Map.containsKey(key)) day30Map.put(key, day30Map.get(key) + 1);
            } while (c30.moveToNext());
        }
        c30.close();

        // 本月
        Calendar calMonth = Calendar.getInstance();
        calMonth.set(Calendar.DAY_OF_MONTH, 1);
        calMonth.set(Calendar.HOUR_OF_DAY, 0); calMonth.set(Calendar.MINUTE, 0); calMonth.set(Calendar.SECOND, 0);
        long startMonth = calMonth.getTimeInMillis();
        Cursor cMonth = db.rawQuery("SELECT COUNT(*) FROM teeth_records WHERE date>=?", new String[]{String.valueOf(startMonth)});
        int monthCount = 0;
        if (cMonth.moveToFirst()) monthCount = cMonth.getInt(0);
        cMonth.close();

        // 本年
        Calendar calYear = Calendar.getInstance();
        calYear.set(Calendar.MONTH, Calendar.JANUARY);
        calYear.set(Calendar.DAY_OF_MONTH, 1);
        calYear.set(Calendar.HOUR_OF_DAY, 0); calYear.set(Calendar.MINUTE, 0); calYear.set(Calendar.SECOND, 0);
        long startYear = calYear.getTimeInMillis();
        Cursor cYear = db.rawQuery("SELECT COUNT(*) FROM teeth_records WHERE date>=?", new String[]{String.valueOf(startYear)});
        int yearCount = 0;
        if (cYear.moveToFirst()) yearCount = cYear.getInt(0);
        cYear.close();

        db.close();

        // 绘制7天柱状图
        List<BarEntry> mEntries = new ArrayList<>();
        List<BarEntry> eEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int idx = 0;
        int weeklyTotal = 0;
        for (Map.Entry<String, int[]> e : dayMap7.entrySet()) {
            try { labels.add(DAY_FMT.format(FULL_FMT.parse(e.getKey()))); } catch (ParseException ex) { labels.add(""); }
            mEntries.add(new BarEntry(idx, e.getValue()[0]));
            eEntries.add(new BarEntry(idx, e.getValue()[1]));
            weeklyTotal += e.getValue()[0] + e.getValue()[1];
            idx++;
        }
        BarDataSet mSet = new BarDataSet(mEntries, "晨间");
        mSet.setColor(Color.parseColor("#00BCD4"));
        BarDataSet eSet = new BarDataSet(eEntries, "晚间");
        eSet.setColor(Color.parseColor("#0097A7"));
        BarData barData = new BarData(mSet, eSet);
        barData.setBarWidth(0.35f);
        barData.groupBars(-0.5f, 0.2f, 0.05f);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setLabelCount(labels.size());
        barChart.setData(barData);
        barChart.invalidate();

        // 近30天趋势
        List<Entry> trendEntries = new ArrayList<>();
        idx = 0;
        for (Map.Entry<String, Float> e : day30Map.entrySet()) {
            trendEntries.add(new Entry(idx, e.getValue()));
            idx++;
        }
        List<String> labels30 = new ArrayList<>();
        for (Map.Entry<String, Float> e : day30Map.entrySet()) labels30.add("");
        LineDataSet trendSet = new LineDataSet(trendEntries, "刷牙次数");
        trendSet.setColor(Color.parseColor("#00BCD4"));
        trendSet.setCircleColor(Color.parseColor("#00BCD4"));
        trendSet.setLineWidth(2f);
        trendSet.setDrawFilled(true);
        trendSet.setFillColor(Color.parseColor("#B2EBF2"));
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels30));
        lineChart.setData(new LineData(trendSet));
        lineChart.invalidate();

        // 汇总显示
        textWeekly.setText(weeklyTotal + " 次");
        textMonth.setText(monthCount + " 次");
        textYear.setText(yearCount + " 次");
    }
}
