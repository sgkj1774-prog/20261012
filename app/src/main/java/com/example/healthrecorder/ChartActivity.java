package com.example.healthrecorder;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChartActivity extends AppCompatActivity {
    private LineChart chartWeight, chartBP;
    private List<Record> allRecords;
    private HealthRepository repository;
    private Spinner spinnerRange;
    private Spinner spinnerChartUser;
    private List<User> userList;
    private List<Integer> userIds;
    private int selectedUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        chartWeight = findViewById(R.id.chartWeight);
        chartBP = findViewById(R.id.chartBP);
        spinnerRange = findViewById(R.id.spinnerRange);
        spinnerChartUser = findViewById(R.id.spinnerChartUser);

        repository = HealthRepository.getInstance(getApplicationContext());

        // Check if MainActivity passed a user_id
        selectedUserId = getIntent().getIntExtra("user_id", -1);

        setupChartUserSpinner();

        setupChart(chartWeight, "体重趋势 (kg)", Color.rgb(76, 175, 80));
        setupChart(chartBP, "血压变化 (mmHg)", Color.rgb(244, 67, 54));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.chart_range_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRange.setAdapter(adapter);

        spinnerRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] ranges = {"all", "week", "month", "3month"};
                loadChartData(ranges[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadChartData("all");
    }

    private void setupChartUserSpinner() {
        userList = repository.getAllUsers();
        List<String> names = new ArrayList<>();
        userIds = new ArrayList<>();
        names.add("全部成员");
        userIds.add(-1);
        for (User u : userList) {
            names.add(u.getName());
            userIds.add(u.getId());
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChartUser.setAdapter(spinnerAdapter);

        // Set selection if a user was pre-selected
        if (selectedUserId > 0) {
            for (int i = 0; i < userIds.size(); i++) {
                if (userIds.get(i) == selectedUserId) {
                    spinnerChartUser.setSelection(i);
                    break;
                }
            }
        }

        spinnerChartUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedUserId = userIds.get(position);
                loadChartData("all");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupChart(LineChart chart, String title, int color) {
        chart.getDescription().setText(title);
        chart.getDescription().setTextSize(14f);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);
        chart.setMaxVisibleValueCount(100);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < allRecords.size()) {
                    return sdf.format(allRecords.get(idx).getDate());
                }
                return "";
            }
        });

        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.setAutoScaleMinMaxEnabled(true);
    }

    private void loadChartData(String range) {
        long now = System.currentTimeMillis();
        long cutoff = 0;
        if ("week".equals(range)) cutoff = now - 7L * 24 * 3600 * 1000;
        else if ("month".equals(range)) cutoff = now - 30L * 24 * 3600 * 1000;
        else if ("3month".equals(range)) cutoff = now - 90L * 24 * 3600 * 1000;

        allRecords = repository.getRecordsForChartByUserId(selectedUserId);

        List<Record> filtered = new ArrayList<>();
        for (Record r : allRecords) {
            if (cutoff == 0 || r.getDate().getTime() >= cutoff) filtered.add(r);
        }

        // Weight chart - 优化大数据量显示
        ArrayList<Entry> weightEntries = new ArrayList<>();
        boolean hasWeight = false;
        for (int i = 0; i < filtered.size(); i++) {
            float w = filtered.get(i).getWeight();
            if (w > 0) {
                weightEntries.add(new Entry(i, w));
                hasWeight = true;
            }
        }
        if (hasWeight) {
            LineDataSet weightSet = new LineDataSet(weightEntries, "体重 (kg)");
            weightSet.setColor(Color.rgb(76, 175, 80));
            weightSet.setCircleColor(Color.rgb(76, 175, 80));
            weightSet.setLineWidth(2f);
            weightSet.setCircleRadius(filtered.size() > 50 ? 2f : 4f);
            weightSet.setValueTextSize(0f);  // 大数据量隐藏数值标注
            weightSet.setDrawFilled(true);
            weightSet.setFillColor(Color.rgb(200, 230, 201));
            weightSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);  // 平滑曲线
            chartWeight.setData(new LineData(weightSet));
            chartWeight.setVisibleXRangeMinimum(10f);
            chartWeight.moveViewToX(weightEntries.size() - 1);
        } else {
            chartWeight.clear();
            chartWeight.getDescription().setText("暂无体重数据");
        }

        // BP chart
        ArrayList<Entry> sysEntries = new ArrayList<>();
        ArrayList<Entry> diaEntries = new ArrayList<>();
        boolean hasBP = false;
        for (int i = 0; i < filtered.size(); i++) {
            int sys = filtered.get(i).getSystolic();
            int dia = filtered.get(i).getDiastolic();
            if (sys > 0 && dia > 0) {
                sysEntries.add(new Entry(i, sys));
                diaEntries.add(new Entry(i, dia));
                hasBP = true;
            }
        }
        if (hasBP) {
            LineDataSet sysSet = new LineDataSet(sysEntries, "收缩压");
            sysSet.setColor(Color.rgb(244, 67, 54));
            sysSet.setCircleColor(Color.rgb(244, 67, 54));
            sysSet.setLineWidth(2f);
            sysSet.setCircleRadius(filtered.size() > 50 ? 2f : 4f);
            sysSet.setValueTextSize(0f);
            LineDataSet diaSet = new LineDataSet(diaEntries, "舒张压");
            diaSet.setColor(Color.rgb(33, 150, 243));
            diaSet.setCircleColor(Color.rgb(33, 150, 243));
            diaSet.setLineWidth(2f);
            diaSet.setCircleRadius(filtered.size() > 50 ? 2f : 4f);
            diaSet.setValueTextSize(0f);
            chartBP.setData(new LineData(sysSet, diaSet));
            chartBP.setVisibleXRangeMinimum(10f);
            chartBP.moveViewToX(sysEntries.size() - 1);
        } else {
            chartBP.clear();
            chartBP.getDescription().setText("暂无血压数据");
        }

        chartWeight.invalidate();
        chartBP.invalidate();
    }
}
