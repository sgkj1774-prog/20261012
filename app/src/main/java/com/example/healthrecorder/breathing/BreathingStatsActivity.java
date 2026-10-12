package com.example.healthrecorder.breathing;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BreathingStatsActivity extends AppCompatActivity {
    private HealthRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing_stats);
        repository = HealthRepository.getInstance(getApplicationContext());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvTotal = findViewById(R.id.tvTotal);
        TextView tvAvgDuration = findViewById(R.id.tvAvgDuration);
        TextView tvTotalDuration = findViewById(R.id.tvTotalDuration);
        TextView tvAvgScore = findViewById(R.id.tvAvgScore);
        TextView tvByMode = findViewById(R.id.tvByMode);

        try {
            List<BreathingRecord> records = repository.getAllBreathingRecords();
            int total = records == null ? 0 : records.size();

            long totalDuration = 0;
            int totalScore = 0;
            int scoreCount = 0;
            Map<String, Integer> byMode = new HashMap<>();

            String[] modeNames = {"腹式呼吸", "胸式呼吸", "478呼吸", "箱式呼吸", "自定义"};

            if (records != null) {
                for (BreathingRecord r : records) {
                    if (r == null) continue;
                    totalDuration += r.getDuration();
                    if (r.getScore() > 0) {
                        totalScore += r.getScore();
                        scoreCount++;
                    }
                    String modeText = r.getModeText();
                    byMode.put(modeText, byMode.getOrDefault(modeText, 0) + 1);
                }
            }

            tvTotal.setText("总打卡次数：" + total);
            tvAvgDuration.setText("平均时长：" + (total > 0 ? formatDuration((int)(totalDuration / total)) : "0秒"));
            tvTotalDuration.setText("累计时长：" + formatDuration((int) totalDuration));
            tvAvgScore.setText("平均感受分：" + (scoreCount > 0 ? String.format("%.1f", (double) totalScore / scoreCount) + " / 10" : "（无数据）"));
            tvByMode.setText(mapToText(byMode));
        } catch (Throwable t) {
            tvTotal.setText("统计失败：" + t.getClass().getSimpleName());
        }
    }

    private String formatDuration(int seconds) {
        if (seconds < 60) return seconds + "秒";
        int min = seconds / 60;
        int sec = seconds % 60;
        if (min < 60) return min + "分" + (sec > 0 ? sec + "秒" : "");
        int hour = min / 60;
        int remainMin = min % 60;
        return hour + "小时" + (remainMin > 0 ? remainMin + "分" : "");
    }

    private String mapToText(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return "（无数据）";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> en : map.entrySet()) {
            if (!first) sb.append("\n");
            first = false;
            sb.append("• ").append(en.getKey()).append("：").append(en.getValue()).append(" 次");
        }
        return sb.toString();
    }
}
