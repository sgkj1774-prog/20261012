package com.example.healthrecorder.book;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookStatsActivity extends AppCompatActivity {

    private HealthRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_stats);
        repository = HealthRepository.getInstance(getApplicationContext());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvTotal = findViewById(R.id.tvTotal);
        TextView tvReading = findViewById(R.id.tvReading);
        TextView tvFinished = findViewById(R.id.tvFinished);
        TextView tvWish = findViewById(R.id.tvWish);
        TextView tvAvgRating = findViewById(R.id.tvAvgRating);
        TextView tvTotalPages = findViewById(R.id.tvTotalPages);
        TextView tvFinishedPages = findViewById(R.id.tvFinishedPages);
        TextView tvByStatus = findViewById(R.id.tvByStatus);

        try {
            List<BookRecord> books = repository.getAllBookRecords();
            int total = books == null ? 0 : books.size();

            int reading = 0, finished = 0, wish = 0;
            int totalPages = 0, finishedPages = 0;
            int ratedCount = 0, ratingSum = 0;

            Map<String, Integer> byStatus = new HashMap<>();

            if (books != null) {
                for (BookRecord b : books) {
                    if (b == null) continue;
                    int st = b.getStatus();
                    if (st == 1) { reading++; byStatus.put("在读", byStatus.getOrDefault("在读", 0) + 1); }
                    else if (st == 2) { finished++; byStatus.put("已读", byStatus.getOrDefault("已读", 0) + 1); }
                    else { wish++; byStatus.put("想读", byStatus.getOrDefault("想读", 0) + 1); }

                    if (b.getTotalPages() > 0) totalPages += b.getTotalPages();
                    if (st == 2 && b.getFinishedPage() > 0) finishedPages += b.getFinishedPage();
                    else if (st == 2 && b.getTotalPages() > 0) finishedPages += b.getTotalPages();

                    if (b.getRating() > 0) { ratedCount++; ratingSum += b.getRating(); }
                }
            }

            tvTotal.setText("总书籍数：" + total + " 本");
            tvReading.setText("在读：" + reading + " 本");
            tvFinished.setText("已读：" + finished + " 本");
            tvWish.setText("想读：" + wish + " 本");

            if (ratedCount > 0) {
                tvAvgRating.setText("已读均分：" + String.format("%.1f", (float) ratingSum / ratedCount) + " ★（共 " + ratedCount + " 本评分）");
            } else {
                tvAvgRating.setText("已读均分：暂无评分");
            }

            tvTotalPages.setText("记录总页数：" + totalPages + " 页");
            tvFinishedPages.setText("读完累计页数：" + finishedPages + " 页");

            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, Integer> en : byStatus.entrySet()) {
                if (!first) sb.append("\n");
                first = false;
                sb.append("• ").append(en.getKey()).append("：").append(en.getValue()).append(" 本");
            }
            tvByStatus.setText(sb.length() > 0 ? sb.toString() : "（无数据）");

        } catch (Throwable t) {
            tvTotal.setText("统计失败：" + t.getClass().getSimpleName());
        }
    }
}
