package com.example.healthrecorder.cat;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.example.healthrecorder.DatabaseHelper;

public class CatStatsActivity extends AppCompatActivity {
    private TextView textDaysSinceLast, textAvgCount, textWeekCount, textMonthCount;
    private TextView textAvgInterval, textMaxInterval, textMinInterval;
    private LinearLayout layoutScoreBars, layoutRecentRecords;
    private HealthRepository repository;
    private final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    // 评分颜色和描述
    private final int[] SCORE_COLORS = {
            0xFFE53935, // 1 - 红色
            0xFFFF9800, // 2 - 橙色
            0xFF4CAF50, // 3 - 绿色
            0xFFFFEB3B, // 4 - 黄色
            0xFFE53935  // 5 - 红色
    };
    private final String[] SCORE_DESCS = {
            "便秘", "略干", "正常", "略软", "腹泻"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cat_stats);

        repository = HealthRepository.getInstance(getApplicationContext());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        textDaysSinceLast = findViewById(R.id.textDaysSinceLast);
        textAvgCount = findViewById(R.id.textAvgCount);
        textWeekCount = findViewById(R.id.textWeekCount);
        textMonthCount = findViewById(R.id.textMonthCount);
        textAvgInterval = findViewById(R.id.textAvgInterval);
        textMaxInterval = findViewById(R.id.textMaxInterval);
        textMinInterval = findViewById(R.id.textMinInterval);
        layoutScoreBars = findViewById(R.id.layoutScoreBars);
        layoutRecentRecords = findViewById(R.id.layoutRecentRecords);

        loadStats();
    }

    private void loadStats() {
        List<CatRecord> records = repository.getAllCatRecords();
        if (records.isEmpty()) {
            textDaysSinceLast.setText("--");
            textWeekCount.setText("0");
            textMonthCount.setText("0");
            return;
        }

        // 距上次天数
        long lastTime = records.get(0).getDate().getTime();
        long now = System.currentTimeMillis();
        int daysSinceLast = (int) ((now - lastTime) / (1000 * 60 * 60 * 24));
        textDaysSinceLast.setText(String.valueOf(daysSinceLast));

        // 本周/本月次数
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // 本周一
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        long weekStart = cal.getTimeInMillis();

        // 本月1日
        cal.set(Calendar.DAY_OF_MONTH, 1);
        long monthStart = cal.getTimeInMillis();

        int weekCount = 0, monthCount = 0;
        for (CatRecord r : records) {
            long t = r.getDate().getTime();
            if (t >= weekStart) weekCount++;
            if (t >= monthStart) monthCount++;
        }
        textWeekCount.setText(String.valueOf(weekCount));
        textMonthCount.setText(String.valueOf(monthCount));

        // 平均粪便数量
        int totalStoolCount = 0, countWithStool = 0;
        for (CatRecord r : records) {
            if (r.getStoolCount() > 0) {
                totalStoolCount += r.getStoolCount();
                countWithStool++;
            }
        }
        if (countWithStool > 0) {
            textAvgCount.setText(String.format("%.1f", totalStoolCount * 1.0 / countWithStool));
        } else {
            textAvgCount.setText("--");
        }

        // 间隔统计（计算相邻记录的间隔）
        if (records.size() >= 2) {
            List<Integer> intervals = new ArrayList<>();
            for (int i = 0; i < records.size() - 1; i++) {
                long t1 = records.get(i).getDate().getTime();
                long t2 = records.get(i + 1).getDate().getTime();
                int days = (int) ((t1 - t2) / (1000 * 60 * 60 * 24));
                if (days >= 0) intervals.add(days);
            }

            if (!intervals.isEmpty()) {
                int sum = 0, max = 0, min = Integer.MAX_VALUE;
                for (int d : intervals) {
                    sum += d;
                    if (d > max) max = d;
                    if (d < min) min = d;
                }
                textAvgInterval.setText(String.format("%.1f 天", sum * 1.0 / intervals.size()));
                textMaxInterval.setText(max + " 天");
                textMinInterval.setText(min + " 天");
            }
        }

        // 评分分布
        int[] scoreCounts = new int[5];
        for (CatRecord r : records) {
            int s = r.getStoolType();
            if (s >= 1 && s <= 5) scoreCounts[s - 1]++;
        }
        int maxCount = 0;
        for (int c : scoreCounts) if (c > maxCount) maxCount = c;

        layoutScoreBars.removeAllViews();
        for (int i = 0; i < 5; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);

            TextView label = new TextView(this);
            label.setText((i + 1) + "分");
            label.setTextSize(12);
            label.setTextColor(Color.parseColor("#666666"));
            label.setWidth(80);
            row.addView(label);

            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setLayoutParams(new LinearLayout.LayoutParams(0, 24, 1));
            bar.setMax(maxCount > 0 ? maxCount : 1);
            bar.setProgress(scoreCounts[i]);
            bar.getProgressDrawable().setTint((int) SCORE_COLORS[i]);
            row.addView(bar);

            TextView count = new TextView(this);
            count.setText(String.valueOf(scoreCounts[i]));
            count.setTextSize(12);
            count.setTextColor(Color.parseColor("#212121"));
            count.setPadding(16, 0, 0, 0);
            row.addView(count);

            layoutScoreBars.addView(row);
        }

        // 最近5条记录
        layoutRecentRecords.removeAllViews();
        int showCount = Math.min(5, records.size());
        for (int i = 0; i < showCount; i++) {
            CatRecord r = records.get(i);

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(0, 12, 0, 12);

            TextView date = new TextView(this);
            date.setText(DATE_FMT.format(r.getDate()));
            date.setTextSize(13);
            date.setTextColor(Color.parseColor("#666666"));
            date.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            item.addView(date);

            // 评分标签
            TextView score = new TextView(this);
            score.setText(SCORE_DESCS[r.getStoolType() - 1]);
            score.setTextSize(12);
            score.setTextColor(Color.WHITE);
            score.setPadding(12, 4, 12, 4);
            score.setBackgroundColor((int) SCORE_COLORS[r.getStoolType() - 1]);
            item.addView(score);

            layoutRecentRecords.addView(item);

            if (i < showCount - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                layoutRecentRecords.addView(divider);
            }
        }
    }
}
