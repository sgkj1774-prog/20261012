package com.example.healthrecorder;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.book.BookRecord;
import com.example.healthrecorder.breathing.BreathingRecord;
import com.example.healthrecorder.cat.CatRecord;
import com.example.healthrecorder.event.Event;
import com.example.healthrecorder.exercise.ExerciseRecord;
import com.example.healthrecorder.medication.Medication;
import com.example.healthrecorder.medication.MedicationLog;
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
    private Date currentDate = new Date();

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
        currentDate = date;
        contentContainer.removeAllViews();

        String dayKey = DAY_KEY_FMT.format(date);
        boolean hasAny = false;

        // === 服药打卡（放在最前，点到某天就能直接补/取消打卡）===
        if (addMedicationSection(dayKey)) hasAny = true;

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

    // ============================================================
    // 服药打卡（按日粒度，可在任意历史日期补录/取消）
    // ============================================================

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    /**
     * 渲染当日服药打卡区块。药品按「建档日不晚于该日」过滤，
     * 避免翻到建档前的日期时冒出一堆没意义的药。
     */
    private boolean addMedicationSection(String dayKey) {
        List<Medication> all = repository.getAllMedications();
        if (all == null || all.isEmpty()) return false;

        List<Medication> show = new ArrayList<>();
        String todayKey = DAY_KEY_FMT.format(new Date());
        for (Medication m : all) {
            // 只显示该日期处于服用周期内的药（按起始/截止日期判断；未设日期的旧记录仍显示）
            if (!m.coversDate(dayKey)) continue;
            // 手动归档的周期：历史日期仍展示（便于追溯），但当天不再纳入打卡
            if (m.isArchived() && dayKey.equals(todayKey)) continue;
            show.add(m);
        }
        if (show.isEmpty()) return false;

        addSection("💊 服药记录", "#00ACC1");
        boolean future = dayKey.compareTo(DAY_KEY_FMT.format(new Date())) > 0;
        addHintLine(future
                ? "这一天还没到，暂不能打卡"
                : "点击整行切换「已服 / 未服」，长按可写备注");

        for (Medication m : show) {
            MedicationLog log = repository.getMedicationLog(m.getId(), dayKey);
            addMedicationRow(m, log, future);
        }
        return true;
    }

    private void addHintLine(String text) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setTextColor(Color.parseColor("#999999"));
        tv.setPadding(0, 0, 0, dp(8));
        contentContainer.addView(tv);
    }

    private void addMedicationRow(final Medication m, final MedicationLog log, final boolean future) {
        final boolean taken = log != null && log.isTaken();

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackgroundColor(Color.WHITE);
        row.setElevation(dp(1));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(lp);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        left.setLayoutParams(leftLp);

        TextView name = new TextView(this);
        name.setText(m.getName());
        name.setTextSize(15f);
        name.setTextColor(Color.parseColor("#333333"));
        name.getPaint().setFakeBoldText(true);
        left.addView(name);

        StringBuilder sub = new StringBuilder(m.getScheduleText());
        if (m.getDosage() != null && !m.getDosage().trim().isEmpty()) {
            sub.append("  ").append(m.getDosage().trim());
        }
        if (m.getBatchNo() != null && !m.getBatchNo().trim().isEmpty()) {
            sub.append("  ·  批号 ").append(m.getBatchNo().trim());
        }
        if (log != null && log.getNotes() != null && !log.getNotes().trim().isEmpty()) {
            sub.append("  ·  ").append(log.getNotes().trim());
        }
        TextView tvSub = new TextView(this);
        tvSub.setText(sub.toString());
        tvSub.setTextSize(12f);
        tvSub.setTextColor(Color.parseColor("#999999"));
        tvSub.setPadding(0, dp(2), 0, 0);
        left.addView(tvSub);

        row.addView(left);

        TextView status = new TextView(this);
        if (future) {
            status.setText("未到");
            status.setTextColor(Color.parseColor("#999999"));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(12));
            bg.setColor(Color.parseColor("#F0F0F0"));
            status.setBackground(bg);
        } else if (taken) {
            status.setText("已服 ✓");
            status.setTextColor(Color.WHITE);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(12));
            bg.setColor(Color.parseColor("#43A047"));
            status.setBackground(bg);
        } else {
            status.setText("未服");
            status.setTextColor(Color.WHITE);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(12));
            bg.setColor(Color.parseColor("#BDBDBD"));
            status.setBackground(bg);
        }
        status.setTextSize(12f);
        status.setPadding(dp(12), dp(5), dp(12), dp(5));
        row.addView(status);

        row.setOnClickListener(v -> toggleMedication(m, log, taken, future));
        row.setOnLongClickListener(v -> {
            showMedicationNoteDialog(m, log, future);
            return true;
        });

        contentContainer.addView(row);
    }

    private void toggleMedication(Medication m, MedicationLog existing, boolean taken, boolean future) {
        if (future) {
            Toast.makeText(this, "还没到这一天，不能提前打卡", Toast.LENGTH_SHORT).show();
            return;
        }
        String dayKey = DAY_KEY_FMT.format(currentDate);
        MedicationLog log = new MedicationLog();
        log.setMedId(m.getId());
        log.setLogDate(dayKey);
        log.setTaken(!taken);
        log.setTakenAt(!taken ? new Date() : null);
        log.setUserId(m.getUserId());
        if (existing != null) log.setNotes(existing.getNotes());

        repository.saveMedicationLog(log);
        Toast.makeText(this,
                !taken ? m.getName() + " 已标记为已服" : "已取消 " + m.getName() + " 的打卡",
                Toast.LENGTH_SHORT).show();
        loadDayData(currentDate);
    }

    private void showMedicationNoteDialog(final Medication m, final MedicationLog log, final boolean future) {
        if (future) {
            Toast.makeText(this, "这一天还没到，暂不能记录", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(2);
        input.setPadding(dp(16), dp(8), dp(16), dp(8));
        input.setHint("如：漏服，忘带药 / 改到饭后服");
        if (log != null && log.getNotes() != null) input.setText(log.getNotes());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(m.getName() + " · 当日备注")
                .setView(input)
                .setPositiveButton("保存", (d, w) -> {
                    String note = input.getText().toString().trim();
                    MedicationLog l = new MedicationLog();
                    l.setMedId(m.getId());
                    l.setLogDate(DAY_KEY_FMT.format(currentDate));
                    l.setTaken(log != null && log.isTaken());
                    l.setTakenAt(log != null ? log.getTakenAt() : null);
                    l.setUserId(m.getUserId());
                    l.setNotes(note.isEmpty() ? null : note);
                    repository.saveMedicationLog(l);
                    loadDayData(currentDate);
                })
                .setNeutralButton("清空备注", (d, w) -> {
                    // 只清备注，打卡状态保持原样；本来就没记录则不写库
                    if (log == null) return;
                    MedicationLog l = new MedicationLog();
                    l.setMedId(m.getId());
                    l.setLogDate(DAY_KEY_FMT.format(currentDate));
                    l.setTaken(log.isTaken());
                    l.setTakenAt(log.getTakenAt());
                    l.setUserId(m.getUserId());
                    l.setNotes(null);
                    repository.saveMedicationLog(l);
                    loadDayData(currentDate);
                })
                .setNegativeButton("取消", null)
                .show();
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
