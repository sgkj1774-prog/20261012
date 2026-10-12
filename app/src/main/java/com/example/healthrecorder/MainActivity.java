package com.example.healthrecorder;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.book.BookListActivity;
import com.example.healthrecorder.breathing.BreathingListActivity;
import com.example.healthrecorder.event.EventListActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private TextView textEmpty;
    private RecordAdapter adapter;
    private HealthRepository repository;
    private Spinner spinnerUser;
    private int selectedUserId = -1;
    private int currentUserPos = 0;
    private FloatingActionButton fabAdd;
    private HomeChartView homeChartView;
    private HomeCalendarView homeCalendarView;

    private int screenW, screenH;
    private int fabW, fabH;
    private int lastX, lastY;
    private boolean dragging = false;

    private final SimpleDateFormat DAY_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 检查上次崩溃日志（有日志则阻止初始化，只显示对话框）
        if (checkCrashLog()) {
            repository = HealthRepository.getInstance(getApplicationContext());
            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText("上次崩溃，请查看日志");
            setContentView(tv);
            return;
        }

        try {
            setContentView(R.layout.activity_main);
        } catch (android.view.InflateException ie) {
            StringBuilder sb = new StringBuilder();
            sb.append("InflateException: ").append(ie.getMessage()).append("\n");
            Throwable cause = ie;
            while (cause != null) {
                sb.append("→ ").append(cause.getClass().getName())
                  .append(": ").append(cause.getMessage()).append("\n");
                for (StackTraceElement el : cause.getStackTrace()) {
                    sb.append("  at ").append(el.toString()).append("\n");
                }
                cause = cause.getCause();
            }
            writeAndToast(this, sb.toString(), "InflateException");
            return;
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "null" : e.getMessage();
            StringBuilder sb = new StringBuilder();
            sb.append(e.getClass().getName()).append(": ").append(msg).append("\n");
            for (StackTraceElement el : e.getStackTrace()) {
                sb.append("  at ").append(el.toString()).append("\n");
            }
            writeAndToast(this, sb.toString(), e.getClass().getSimpleName());
            return;
        }

        try {
            repository = HealthRepository.getInstance(getApplicationContext());
            listView = findViewById(R.id.listViewRecords);
            textEmpty = findViewById(R.id.textViewEmpty);
            spinnerUser = findViewById(R.id.spinnerUser);
            fabAdd = findViewById(R.id.fabAdd);

            // 手动创建 HomeChartView（包 try-catch，崩溃时 fallback TextView）
            FrameLayout chartContainer = findViewById(R.id.homeChartViewContainer);
            try {
                homeChartView = new HomeChartView(this);
                chartContainer.addView(homeChartView);
            } catch (Throwable t) {
                android.util.Log.e("MainActivity", "HomeChartView init failed", t);
                android.widget.TextView tv = new android.widget.TextView(this);
                tv.setText("图表加载失败: " + t.getClass().getSimpleName());
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(0xFF999999);
                chartContainer.addView(tv);
            }

            // 手动创建 HomeCalendarView
            FrameLayout calContainer = findViewById(R.id.homeCalendarViewContainer);
            try {
                homeCalendarView = new HomeCalendarView(this);
                calContainer.addView(homeCalendarView);
            } catch (Throwable t) {
                android.util.Log.e("MainActivity", "HomeCalendarView init failed", t);
                android.widget.TextView tv = new android.widget.TextView(this);
                tv.setText("日历加载失败: " + t.getClass().getSimpleName());
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(0xFF999999);
                calContainer.addView(tv);
            }

        Button btnHealthRecords = findViewById(R.id.btnHealthRecords);
        Button btnChart = findViewById(R.id.btnChart);
        Button btnExport = findViewById(R.id.btnExport);
        Button btnCatMain = findViewById(R.id.btnCatMain);
        Button btnExercise = findViewById(R.id.btnExercise);
        Button btnBreathing = findViewById(R.id.btnBreathing);
        Button btnEvent = findViewById(R.id.btnEvent);
        Button btnBook = findViewById(R.id.btnBook);

        screenW = getResources().getDisplayMetrics().widthPixels;
        screenH = getResources().getDisplayMetrics().heightPixels;

        fabAdd.post(() -> {
            fabW = fabAdd.getWidth();
            fabH = fabAdd.getHeight();
            snapToRightBottom();
        });

        fabAdd.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (fabW == 0 || fabH == 0) { fabW = v.getWidth(); fabH = v.getHeight(); }
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    lastX = (int) event.getRawX(); lastY = (int) event.getRawY(); dragging = false; return false;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) event.getRawX() - lastX;
                    int dy = (int) event.getRawY() - lastY;
                    if (!dragging && (Math.abs(dx) > 10 || Math.abs(dy) > 10)) dragging = true;
                    if (dragging) {
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
                        lp.leftMargin += dx; lp.topMargin += dy;
                        lp.rightMargin = 0; lp.bottomMargin = 0;
                        lp.gravity = android.view.Gravity.NO_GRAVITY;
                        v.setLayoutParams(lp);
                        lastX = (int) event.getRawX(); lastY = (int) event.getRawY();
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                    if (dragging) {
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
                        int cx = lp.leftMargin + fabW / 2;
                        int margin = 20;
                        if (cx < screenW / 2) lp.leftMargin = margin;
                        else lp.leftMargin = screenW - fabW - margin;
                        lp.gravity = android.view.Gravity.NO_GRAVITY;
                        v.setLayoutParams(lp);
                        dragging = false;
                        return true;
                    }
                    return false;
                default: return false;
            }
        });

        loadUserSpinner();
        spinnerUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) selectedUserId = -1;
                else {
                    List<User> users = repository.getAllUsers();
                    if (position - 1 < users.size()) selectedUserId = users.get(position - 1).getId();
                }
                currentUserPos = position;
                loadAll();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnHealthRecords.setOnClickListener(v -> startActivity(new Intent(this, RecordListActivity.class)));
        btnChart.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChartActivity.class);
            intent.putExtra("user_id", selectedUserId);
            startActivity(intent);
        });
        btnExport.setOnClickListener(v -> startActivity(new Intent(this, DataManagementActivity.class)));
        btnCatMain.setOnClickListener(v -> startActivity(new Intent(this, com.example.healthrecorder.cat.CatHomeActivity.class)));
        btnExercise.setOnClickListener(v -> startActivity(new Intent(this, com.example.healthrecorder.exercise.ExerciseRecordListActivity.class)));
        btnBreathing.setOnClickListener(v -> {
            try { startActivity(new Intent(this, BreathingListActivity.class)); }
            catch (Exception e) { Toast.makeText(this, "呼吸打卡功能正在加载中", Toast.LENGTH_SHORT).show(); }
        });
        btnEvent.setOnClickListener(v -> {
            try { startActivity(new Intent(this, EventListActivity.class)); }
            catch (Exception e) { Toast.makeText(this, "事项功能正在加载中", Toast.LENGTH_SHORT).show(); }
        });
        btnBook.setOnClickListener(v -> {
            try { startActivity(new Intent(this, BookListActivity.class)); }
            catch (Exception e) { Toast.makeText(this, "读书功能正在加载中", Toast.LENGTH_SHORT).show(); }
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddRecordActivity.class);
            intent.putExtra("user_id", selectedUserId > 0 ? selectedUserId : 1);
            startActivity(intent);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Record record = (Record) adapter.getItem(position);
            new AlertDialog.Builder(this)
                    .setTitle("删除记录")
                    .setMessage("确定删除这条记录吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        repository.deleteRecord(record.getId());
                        loadAll();
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Record record = (Record) adapter.getItem(position);
            Intent intent = new Intent(this, AddRecordActivity.class);
            intent.putExtra("record_id", record.getId());
            startActivity(intent);
        });

        homeCalendarView.setOnDateClickListener(date -> {
            Intent intent = new Intent(this, DayDetailActivity.class);
            intent.putExtra(DayDetailActivity.EXTRA_DATE_MILLIS, date.getTime());
            startActivity(intent);
        });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "初始化异常：" + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void snapToRightBottom() {
        if (fabW == 0 || fabH == 0) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fabAdd.getLayoutParams();
        int margin = 20;
        lp.leftMargin = screenW - fabW - margin;
        lp.topMargin = screenH - fabH - margin - getNavBarHeight();
        lp.rightMargin = margin; lp.bottomMargin = margin;
        lp.gravity = android.view.Gravity.NO_GRAVITY;
        fabAdd.setLayoutParams(lp);
    }

    private int getNavBarHeight() {
        int resId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return resId > 0 ? getResources().getDimensionPixelSize(resId) : 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            loadUserSpinnerQuiet();
            loadAll();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "数据加载异常：" + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadUserSpinnerQuiet() {
        try {
            if (spinnerUser == null) {
                android.util.Log.w("MainActivity", "spinnerUser is null, skip");
                return;
            }
            if (repository == null) {
                android.util.Log.w("MainActivity", "repository is null, skip");
                return;
            }
            List<User> users = repository.getAllUsers();
            if (users == null) {
                android.util.Log.w("MainActivity", "users is null, skip");
                return;
            }
            String[] items = new String[users.size() + 1];
            items[0] = "全部成员";
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                items[i + 1] = (u != null && u.getName() != null) ? u.getName() : "未知";
            }
            android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, items);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerUser.setAdapter(spinnerAdapter);
            if (currentUserPos < items.length) spinnerUser.setSelection(currentUserPos);
        } catch (Throwable e) {
            android.util.Log.e("MainActivity", "loadUserSpinnerQuiet error", e);
            writeLog("loadUserSpinnerQuiet: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private void loadUserSpinner() { loadUserSpinnerQuiet(); }

    private void loadAll() {
        try { loadChart(); } catch (Throwable e) { android.util.Log.e("MainActivity", "loadChart error", e); }
        try { loadCalendar(); } catch (Throwable e) { android.util.Log.e("MainActivity", "loadCalendar error", e); }
        try { loadRecords(); } catch (Throwable e) { android.util.Log.e("MainActivity", "loadRecords error", e); }
    }

    private void loadChart() {
        if (homeChartView == null) {
            android.util.Log.w("MainActivity", "homeChartView is null, skip loadChart");
            return;
        }
        List<Record> weightRecords = repository.getRecordsForChartByUserId(selectedUserId);
        List<com.example.healthrecorder.exercise.ExerciseRecord> exerciseRecords =
                repository.getAllExerciseRecords();
        homeChartView.setData(weightRecords, exerciseRecords);
    }

    private void loadCalendar() {
        if (homeCalendarView == null) {
            android.util.Log.w("MainActivity", "homeCalendarView is null, skip loadCalendar");
            return;
        }
        Set<String> markers = new HashSet<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            for (Record r : repository.getRecordsByUserId(selectedUserId)) markers.add(sdf.format(r.getDate()));
            for (com.example.healthrecorder.cat.CatRecord r : repository.getAllCatRecords()) markers.add(sdf.format(r.getDate()));
            for (com.example.healthrecorder.teeth.TeethRecord r : repository.getAllTeethRecords()) markers.add(sdf.format(r.getDate()));
            for (com.example.healthrecorder.exercise.ExerciseRecord r : repository.getAllExerciseRecords()) markers.add(sdf.format(r.getDate()));
            for (com.example.healthrecorder.breathing.BreathingRecord r : repository.getAllBreathingRecords()) markers.add(sdf.format(r.getDate()));
            for (com.example.healthrecorder.event.Event ev : repository.getAllEvents()) {
                if (ev.getEventTime() != null) markers.add(sdf.format(ev.getEventTime()));
            }
            for (com.example.healthrecorder.book.BookRecord r : repository.getAllBookRecords()) {
                if (r.getCreatedAt() != null) markers.add(sdf.format(r.getCreatedAt()));
            }
        } catch (Exception e) { android.util.Log.e("MainActivity", "loadCalendar inner error", e); }
        homeCalendarView.setDateMarkers(markers);
    }

    private void loadRecords() {
        if (listView == null || textEmpty == null) {
            android.util.Log.w("MainActivity", "listView or textEmpty is null, skip loadRecords");
            return;
        }
        List<Record> all = repository.getRecordsByUserId(selectedUserId);
        // 主页仅展示“今日”健康记录，最多 3 条
        String todayStr = DAY_FMT.format(new Date());
        List<Record> records = new ArrayList<>();
        if (all != null) {
            for (Record r : all) {
                if (r == null || r.getDate() == null) continue;
                if (DAY_FMT.format(r.getDate()).equals(todayStr)) {
                    records.add(r);
                    if (records.size() >= 3) break;
                }
            }
        }
        if (records.isEmpty()) {
            listView.setVisibility(View.GONE);
            textEmpty.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            textEmpty.setVisibility(View.GONE);
            adapter = new RecordAdapter(this, records);
            listView.setAdapter(adapter);
        }
    }

    private boolean checkCrashLog() {
        try {
            java.io.File dir = new java.io.File(getFilesDir(), "crash_logs");
            if (!dir.exists()) return false;
            java.io.File[] files = dir.listFiles();
            if (files == null || files.length == 0) return false;
            java.io.File latest = files[files.length - 1];
            String msg;
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(latest))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null && sb.length() < 4000) {
                    sb.append(line).append("\n");
                }
                msg = sb.toString().trim();
            }
            // 立即删除，避免被本轮崩溃覆盖
            latest.delete();
            
            if (!msg.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("⚠ 上次崩溃日志")
                        .setMessage(msg)
                        .setPositiveButton("知道了，继续", null)
                        .setCancelable(false)
                        .show();
                return true;  // 有崩溃日志，阻止后续初始化
            }
        } catch (Exception e) { android.util.Log.e("MainActivity", "gesture error", e); }
        return false;
    }

    /** 写日志到 /sdcard/HealthRecorder_crashes/ */
    private void writeLog(String msg) {
        try {
            java.io.File dir = new java.io.File("/sdcard/HealthRecorder_crashes");
            dir.mkdirs();
            String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                    .format(new java.util.Date());
            java.io.File f = new java.io.File(dir, "log_" + ts + ".txt");
            java.io.FileWriter fw = new java.io.FileWriter(f);
            fw.write(msg);
            fw.close();
        } catch (Exception ignored) {}
    }

    /** 写崩溃日志到 /sdcard/ 并弹 Toast */
    private void writeAndToast(android.app.Activity ctx, String msg, String tag) {
        String filePath = "";
        try {
            java.io.File dir = new java.io.File("/sdcard/HealthRecorder_crashes");
            dir.mkdirs();
            String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.US)
                    .format(new java.util.Date());
            java.io.File f = new java.io.File(dir, "main_init_" + ts + ".txt");
            java.io.FileWriter fw = new java.io.FileWriter(f);
            fw.write(msg);
            fw.close();
            filePath = f.getAbsolutePath();
            android.util.Log.e("MainActivity", "Init error log: " + filePath);
        } catch (Exception ex) {
            android.util.Log.e("MainActivity", "Failed to write init error log", ex);
        }
        String shown = msg.substring(0, Math.min(1500, msg.length()));
        if (!filePath.isEmpty()) shown = shown + "\n日志：" + filePath;
        Toast.makeText(ctx, "初始化异常：" + tag + "\n" + shown, Toast.LENGTH_LONG * 3).show();
        android.util.Log.e("MainActivity", "setContentView Exception: " + tag + ": " + msg);
    }
}
