package com.example.healthrecorder.exercise;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.example.healthrecorder.DatabaseHelper;

public class ExerciseRecordActivity extends AppCompatActivity {
    private int selectedType = ExerciseRecord.TYPE_RUNNING;
    private int selectedDifficulty = 3;
    private int editId = -1;
    private boolean editMode = false;
    private Calendar selectedDateTime = Calendar.getInstance();
    private final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private HealthRepository repository;
    private Context ctx;
    private Button[] diffButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_record);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());

        editId = getIntent().getIntExtra("edit_id", -1);
        editMode = editId > 0;

        Button btnRunning = findViewById(R.id.btnRunning);
        Button btnHiking = findViewById(R.id.btnHiking);
        Button btnDiff1 = findViewById(R.id.btnDiff1);
        Button btnDiff2 = findViewById(R.id.btnDiff2);
        Button btnDiff3 = findViewById(R.id.btnDiff3);
        Button btnDiff4 = findViewById(R.id.btnDiff4);
        Button btnDiff5 = findViewById(R.id.btnDiff5);
        diffButtons = new Button[]{btnDiff1, btnDiff2, btnDiff3, btnDiff4, btnDiff5};

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Button btnDate = findViewById(R.id.btnDate);
        Button btnTime = findViewById(R.id.btnTime);
        btnDate.setText("📅 " + DATE_FMT.format(selectedDateTime.getTime()));
        btnTime.setText("🕐 " + TIME_FMT.format(selectedDateTime.getTime()));

        btnDate.setOnClickListener(v -> new DatePickerDialog(ctx, (dp, y, m, d) -> {
            selectedDateTime.set(Calendar.YEAR, y);
            selectedDateTime.set(Calendar.MONTH, m);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, d);
            btnDate.setText("📅 " + DATE_FMT.format(selectedDateTime.getTime()));
        },
        selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH),
        selectedDateTime.get(Calendar.DAY_OF_MONTH)).show());

        btnTime.setOnClickListener(v -> new TimePickerDialog(ctx, (tp, h, min) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, h);
            selectedDateTime.set(Calendar.MINUTE, min);
            btnTime.setText("🕐 " + TIME_FMT.format(selectedDateTime.getTime()));
        },
        selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), true).show());

        // 地点：从历史地点中快速选择
        findViewById(R.id.btnLocationHistory).setOnClickListener(v -> showLocationHistory());

        btnRunning.setOnClickListener(v -> {            selectedType = ExerciseRecord.TYPE_RUNNING;
            updateTypeButtons(btnRunning, btnHiking);
        });
        btnHiking.setOnClickListener(v -> {
            selectedType = ExerciseRecord.TYPE_HIKING;
            updateTypeButtons(btnRunning, btnHiking);
        });
        updateTypeButtons(btnRunning, btnHiking);

        View.OnClickListener diffListener = v -> {
            selectedDifficulty = Integer.parseInt(v.getTag().toString());
            updateDiffButtons();
            ((TextView)findViewById(R.id.textDiffDesc)).setText(getDiffDesc(selectedDifficulty));
        };
        btnDiff1.setTag(1); btnDiff2.setTag(2); btnDiff3.setTag(3); btnDiff4.setTag(4); btnDiff5.setTag(5);
        for (Button b : diffButtons) b.setOnClickListener(diffListener);
        updateDiffButtons();
        ((TextView)findViewById(R.id.textDiffDesc)).setText(getDiffDesc(3));

        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        if (editMode) loadForEdit();
    }

    /** 弹窗展示历史地点，点选后填入地点输入框 */
    private void showLocationHistory() {
        List<String> locations = repository.getExerciseLocations(30);
        if (locations == null || locations.isEmpty()) {
            Toast.makeText(ctx, "还没有历史地点，先手动输入一次就会记住", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] items = locations.toArray(new String[0]);
        new AlertDialog.Builder(ctx)
                .setTitle("选择历史地点")
                .setItems(items, (d, which) ->
                        ((EditText) findViewById(R.id.editLocation)).setText(items[which]))
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateTypeButtons(Button running, Button hiking) {
        int runColor = selectedType == ExerciseRecord.TYPE_RUNNING ? 0xFFFF5722 : 0xFFEEEEEE;
        int hikColor = selectedType == ExerciseRecord.TYPE_HIKING ? 0xFFFF5722 : 0xFFEEEEEE;
        running.setBackgroundTintList(android.content.res.ColorStateList.valueOf(runColor));
        hiking.setBackgroundTintList(android.content.res.ColorStateList.valueOf(hikColor));
        running.setTextColor(android.graphics.Color.parseColor(selectedType == ExerciseRecord.TYPE_RUNNING ? "#FFFFFF" : "#9E9E9E"));
        hiking.setTextColor(android.graphics.Color.parseColor(selectedType == ExerciseRecord.TYPE_HIKING ? "#FFFFFF" : "#9E9E9E"));

        LinearLayout runningLayout = findViewById(R.id.layoutRunning);
        LinearLayout hikingLayout = findViewById(R.id.layoutHiking);
        runningLayout.setVisibility(selectedType == ExerciseRecord.TYPE_RUNNING ? View.VISIBLE : View.GONE);
        hikingLayout.setVisibility(selectedType == ExerciseRecord.TYPE_HIKING ? View.VISIBLE : View.GONE);
    }

    private void updateDiffButtons() {
        int[] colors = {0xFF4CAF50, 0xFF8BC34A, 0xFFFF9800, 0xFFFF5722, 0xFFE53935};
        for (int i = 0; i < 5; i++) {
            Button b = diffButtons[i];
            boolean selected = i + 1 == selectedDifficulty;
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selected ? colors[i] : 0xFFEEEEEE));
        }
    }

    private String getDiffDesc(int d) {
        switch (d) {
            case 1: return "⭐ 轻松";
            case 2: return "⭐⭐ 较轻松";
            case 3: return "⭐⭐⭐ 中等";
            case 4: return "⭐⭐⭐⭐ 较累";
            case 5: return "⭐⭐⭐⭐⭐ 极限";
            default: return "";
        }
    }

    private void save() {
        EditText editDistance = findViewById(R.id.editDistance);
        EditText editDuration = findViewById(R.id.editDuration);
        EditText editHeartRate = findViewById(R.id.editHeartRate);
        EditText editCalories = findViewById(R.id.editCalories);
        EditText editElevation = findViewById(R.id.editElevation);
        EditText editNotes = findViewById(R.id.editNotes);
        EditText editLocation = findViewById(R.id.editLocation);

        String distStr = editDistance.getText().toString().trim();
        String durStr = editDuration.getText().toString().trim();
        String notes = editNotes.getText().toString().trim();

        if (distStr.isEmpty() || durStr.isEmpty()) {
            Toast.makeText(ctx, "请填写距离和时长", Toast.LENGTH_SHORT).show();
            return;
        }

        float distance;
        int duration, heartRate = 0, calories = 0, elevation = 0;
        try {
            distance = Float.parseFloat(distStr);
            duration = Integer.parseInt(durStr);
            String hr = editHeartRate.getText().toString().trim();
            if (!hr.isEmpty()) heartRate = Integer.parseInt(hr);
            String cal = editCalories.getText().toString().trim();
            if (!cal.isEmpty()) calories = Integer.parseInt(cal);
            String elev = editElevation.getText().toString().trim();
            if (!elev.isEmpty()) elevation = Integer.parseInt(elev);
        } catch (NumberFormatException e) {
            Toast.makeText(ctx, "请输入有效数值", Toast.LENGTH_SHORT).show();
            return;
        }

        float pace = (distance > 0 && duration > 0) ? duration / distance : 0;

        SQLiteDatabase db = repository.getWritableDatabase();
        android.content.ContentValues v = new android.content.ContentValues();
        v.put("date", selectedDateTime.getTimeInMillis());
        v.put("type", selectedType);
        v.put("distance", distance);
        v.put("duration", duration);
        v.put("pace", pace);
        v.put("heart_rate", heartRate);
        v.put("calories", calories);
        v.put("elevation_gain", elevation);
        v.put("difficulty", selectedDifficulty);
        v.put("notes", notes.isEmpty() ? null : notes);
        v.put("location", editLocation.getText().toString().trim());
        v.put("user_id", 1);

        if (editMode) {
            int n = db.update("exercise_records", v, "id=?", new String[]{String.valueOf(editId)});
            db.close();
            if (n > 0) {
                Toast.makeText(ctx, "🏃 已更新", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(ctx, "更新失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            long id = db.insert("exercise_records", null, v);
            db.close();
            if (id > 0) {
                Toast.makeText(ctx, "🏃 记录已保存", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(ctx, "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadForEdit() {
        ExerciseRecord rec = repository.getExerciseRecordById(editId);
        if (rec == null) { finish(); return; }
        selectedType = rec.getType();
        selectedDifficulty = rec.getDifficulty();
        if (rec.getDate() != null) selectedDateTime.setTime(rec.getDate());

        Button btnDate = findViewById(R.id.btnDate);
        Button btnTime = findViewById(R.id.btnTime);
        btnDate.setText("📅 " + DATE_FMT.format(selectedDateTime.getTime()));
        btnTime.setText("🕐 " + TIME_FMT.format(selectedDateTime.getTime()));
        updateTypeButtons(findViewById(R.id.btnRunning), findViewById(R.id.btnHiking));
        updateDiffButtons();
        ((TextView)findViewById(R.id.textDiffDesc)).setText(getDiffDesc(selectedDifficulty));

        ((EditText)findViewById(R.id.editDistance)).setText(String.valueOf(rec.getDistance()));
        ((EditText)findViewById(R.id.editDuration)).setText(String.valueOf(rec.getDuration()));
        if (rec.getHeartRate() > 0)
            ((EditText)findViewById(R.id.editHeartRate)).setText(String.valueOf(rec.getHeartRate()));
        if (rec.getCaloriesField() > 0)
            ((EditText)findViewById(R.id.editCalories)).setText(String.valueOf(rec.getCaloriesField()));
        if (rec.getElevationGain() > 0)
            ((EditText)findViewById(R.id.editElevation)).setText(String.valueOf(rec.getElevationGain()));
        if (rec.getNotes() != null)
            ((EditText)findViewById(R.id.editNotes)).setText(rec.getNotes());
        if (rec.getLocation() != null && !rec.getLocation().isEmpty())
            ((EditText)findViewById(R.id.editLocation)).setText(rec.getLocation());

        TextView tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText("🏃 编辑运动记录");
        Button btnSave = findViewById(R.id.btnSave);
        if (btnSave != null) btnSave.setText("💾 保存修改");

        Button btnDelete = findViewById(R.id.btnDelete);
        if (btnDelete != null) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(ctx)
                        .setTitle("删除记录")
                        .setMessage("确定要删除这条运动记录吗？")
                        .setPositiveButton("删除", (d, w) -> {
                            repository.deleteExerciseRecord(editId);
                            setResult(RESULT_OK);
                            finish();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
        }
    }
}
