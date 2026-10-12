package com.example.healthrecorder;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.book.BookRecord;
import com.example.healthrecorder.breathing.BreathingRecord;
import com.example.healthrecorder.cat.CatRecord;
import com.example.healthrecorder.event.Event;
import com.example.healthrecorder.exercise.ExerciseRecord;
import com.example.healthrecorder.teeth.TeethRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DayDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DATE_MILLIS = "date_millis";

    private final SimpleDateFormat DATE_DISPLAY = new SimpleDateFormat("yyyy年M月d日 E", Locale.CHINESE);
    private final SimpleDateFormat DAY_KEY_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private LinearLayout contentContainer;
    private HealthRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_detail);

        repository = HealthRepository.getInstance(this);

        long dateMillis = getIntent().getLongExtra(EXTRA_DATE_MILLIS, System.currentTimeMillis());
        Date date = new Date(dateMillis);

        TextView tvDate = findViewById(R.id.tvDate);
        tvDate.setText(DATE_DISPLAY.format(date));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        contentContainer = findViewById(R.id.contentContainer);

        loadDayData(date);
    }

    private void loadDayData(Date date) {
        String dayKey = DAY_KEY_FMT.format(date);
        boolean hasAny = false;

        // === 健康体重记录 ===
        List<Record> allHealth = repository.getAllRecords();
        List<Record> healthOfDay = new ArrayList<>();
        for (Record r : allHealth) {
            if (DAY_KEY_FMT.format(r.getDate()).equals(dayKey)) {
                healthOfDay.add(r);
            }
        }
        if (!healthOfDay.isEmpty()) {
            hasAny = true;
            addSection("💪 健康记录", "#2196F3");
            for (Record r : healthOfDay) {
                StringBuilder sb = new StringBuilder();
                if (r.hasWeight()) sb.append("体重：").append(r.getWeight()).append("kg");
                if (r.hasBloodPressure()) {
                    if (sb.length() > 0) sb.append("  |  ");
                    sb.append("血压：").append(r.getSystolic()).append("/").append(r.getDiastolic());
                }
                if (sb.length() == 0) sb.append("有记录");
                addItem(r.getDate(), sb.toString(), "#2196F3");
            }
        }

        // === 运动记录 ===
        List<ExerciseRecord> allExercise = repository.getAllExerciseRecords();
        List<ExerciseRecord> exerciseOfDay = new ArrayList<>();
        for (ExerciseRecord r : allExercise) {
            if (r.getDate() != null && DAY_KEY_FMT.format(r.getDate()).equals(dayKey)) {
                exerciseOfDay.add(r);
            }
        }
        if (!exerciseOfDay.isEmpty()) {
            hasAny = true;
            addSection("🏃 运动记录", "#FF5722");
            for (ExerciseRecord r : exerciseOfDay) {
                String detail = r.getTypeLabel() + "  "
                        + String.format(Locale.getDefault(), "%.1fkm  %d分钟  配速%s",
                                r.getDistance(), r.getDuration(), r.getPaceFormatted());
                if (r.getCalories() > 0) detail += "  " + r.getCalories() + "kcal";
                addItem(r.getDate(), detail, "#FF5722");
            }
        }

        // === 刷牙记录 ===
        List<TeethRecord> allTeeth = repository.getAllTeethRecords();
        List<TeethRecord> teethOfDay = new ArrayList<>();
        for (TeethRecord r : allTeeth) {
            if (r.getDate() != null && DAY_KEY_FMT.format(r.getDate()).equals(dayKey)) {
                teethOfDay.add(r);
            }
        }
        if (!teethOfDay.isEmpty()) {
            hasAny = true;
            addSection("🦷 刷牙记录", "#FFC107");
            for (TeethRecord r : teethOfDay) {
                String detail = r.getPeriodLabel() + "  " + r.getDurationFormatted();
                if (r.getNotes() != null && !r.getNotes().isEmpty()) {
                    detail += "  " + r.getNotes();
                }
                addItem(r.getDate(), detail, "#FFC107");
            }
        }

        // === 呼吸打卡 ===
        List<BreathingRecord> allBreath = repository.getAllBreathingRecords();
        List<BreathingRecord> breathOfDay = new ArrayList<>();
        for (BreathingRecord r : allBreath) {
            if (r.getDate() != null && DAY_KEY_FMT.format(r.getDate()).equals(dayKey)) {
                breathOfDay.add(r);
            }
        }
        if (!breathOfDay.isEmpty()) {
            hasAny = true;
            addSection("🌬️ 呼吸打卡", "#4CAF50");
            for (BreathingRecord r : breathOfDay) {
                String detail = r.getModeText() + "  " + r.getDuration() + "分钟"
                        + "  感受 " + r.getScore() + "/10";
                if (r.getNotes() != null && !r.getNotes().isEmpty()) {
                    detail += "  " + r.getNotes();
                }
                addItem(r.getDate(), detail, "#4CAF50");
            }
        }

        // === 重要事项 ===
        List<Event> allEvents = repository.getAllEvents();
        List<Event> eventOfDay = new ArrayList<>();
        for (Event e : allEvents) {
            if (e.getEventTime() != null) {
                String eventKey = DAY_KEY_FMT.format(e.getEventTime());
                if (eventKey.equals(dayKey)) {
                    eventOfDay.add(e);
                }
            }
        }
        if (!eventOfDay.isEmpty()) {
            hasAny = true;
            addSection("📌 重要事项", "#9C27B0");
            for (Event e : eventOfDay) {
                String detail = e.getTitle();
                if (e.getContent() != null && !e.getContent().isEmpty()) {
                    detail += "：" + e.getContent();
                }
                detail += "  [" + e.getStatusText() + "]";
                addItem(e.getEventTime(), detail, "#9C27B0");
            }
        }

        // === 猫咪记录 ===
        List<CatRecord> allCat = repository.getAllCatRecords();
        List<CatRecord> catOfDay = new ArrayList<>();
        for (CatRecord r : allCat) {
            if (r.getDate() != null && DAY_KEY_FMT.format(r.getDate()).equals(dayKey)) {
                catOfDay.add(r);
            }
        }
        if (!catOfDay.isEmpty()) {
            hasAny = true;
            addSection("🐱 猫咪记录", "#FF9800");
            for (CatRecord r : catOfDay) {
                String detail = r.getCatName() + "  便型：" + r.getStoolScore()
                        + "  次数：" + r.getStoolCount();
                if (r.getNotes() != null && !r.getNotes().isEmpty()) {
                    detail += "  " + r.getNotes();
                }
                addItem(r.getDate(), detail, "#FF9800");
            }
        }

        // === 读书记录 ===
        List<BookRecord> allBooks = repository.getAllBookRecords();
        List<BookRecord> bookOfDay = new ArrayList<>();
        for (BookRecord b : allBooks) {
            if (b.getCreatedAt() != null && DAY_KEY_FMT.format(b.getCreatedAt()).equals(dayKey)) {
                bookOfDay.add(b);
            }
        }
        if (!bookOfDay.isEmpty()) {
            hasAny = true;
            addSection("📚 读书笔记", "#607D8B");
            for (BookRecord b : bookOfDay) {
                String detail = b.getTitle();
                if (b.getNotes() != null && !b.getNotes().isEmpty()) {
                    detail += "：" + b.getNotes();
                }
                addItem(b.getCreatedAt(), detail, "#607D8B");
            }
        }

        if (!hasAny) {
            addEmptyHint();
        }
    }

    private void addSection(String title, String colorHex) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setText(title);
        tv.setTextSize(15f);
        tv.setTextColor(android.graphics.Color.parseColor(colorHex));
        tv.getPaint().setFakeBoldText(true);
        tv.setPadding(0, 16, 0, 8);

        LinearLayout divider = new LinearLayout(this);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divLp.bottomMargin = 12;
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor(android.graphics.Color.parseColor(colorHex + "33"));

        contentContainer.addView(tv);
        contentContainer.addView(divider);
    }

    private void addItem(Date date, String detail, String colorHex) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(12, 8, 12, 8);
        row.setBackgroundColor(android.graphics.Color.WHITE);
        row.setElevation(1);
        row.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        int marginPx = (int) (4 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) row.getLayoutParams();
        lp.setMargins(0, marginPx, 0, marginPx);
        row.setLayoutParams(lp);

        TextView tvDetail = new TextView(this);
        tvDetail.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tvDetail.setText(detail);
        tvDetail.setTextSize(14f);
        tvDetail.setTextColor(android.graphics.Color.parseColor("#333333"));

        row.addView(tvDetail);
        contentContainer.addView(row);
    }

    private void addEmptyHint() {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setText("该日暂无任何记录");
        tv.setTextSize(15f);
        tv.setTextColor(android.graphics.Color.parseColor("#BBBBBB"));
        tv.setGravity(Gravity.CENTER);
        int padPx = (int) (32 * getResources().getDisplayMetrics().density);
        tv.setPadding(padPx, padPx, padPx, padPx);
        contentContainer.addView(tv);
    }
}
