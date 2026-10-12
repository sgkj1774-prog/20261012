package com.example.healthrecorder.breathing;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.example.healthrecorder.DatabaseHelper;

public class BreathingAddActivity extends AppCompatActivity {
    private static final String[] MODES = {"腹式呼吸", "胸式呼吸", "478呼吸", "箱式呼吸", "自定义"};

    private int editId = -1;
    private boolean editMode = false;
    private Calendar selectedDateTime = Calendar.getInstance();
    private final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private HealthRepository repository;
    private Context ctx;

    private Button btnDate, btnTime;
    private NumberPicker numberPickerDuration;
    private Spinner spinnerMode;
    private SeekBar seekBarScore;
    private TextView textScoreValue, textScoreStars;
    private EditText editNotes, editCustomContent, editCustomMinutes;
    private View customModeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing_add);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());

        editId = getIntent().getIntExtra("edit_id", -1);
        editMode = editId > 0;

        btnDate = findViewById(R.id.btnDate);
        btnTime = findViewById(R.id.btnTime);
        numberPickerDuration = findViewById(R.id.numberPickerDuration);
        spinnerMode = findViewById(R.id.spinnerMode);
        seekBarScore = findViewById(R.id.seekBarScore);
        textScoreValue = findViewById(R.id.textScoreValue);
        textScoreStars = findViewById(R.id.textScoreStars);
        editNotes = findViewById(R.id.editNotes);
        editCustomContent = findViewById(R.id.editCustomContent);
        editCustomMinutes = findViewById(R.id.editCustomMinutes);
        customModeLayout = findViewById(R.id.customModeLayout);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 日期时间
        updateDateTimeButtons();
        btnDate.setOnClickListener(v -> new DatePickerDialog(ctx, (dp, y, m, d) -> {
            selectedDateTime.set(Calendar.YEAR, y);
            selectedDateTime.set(Calendar.MONTH, m);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, d);
            updateDateTimeButtons();
        }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)).show());
        btnTime.setOnClickListener(v -> new TimePickerDialog(ctx, (tp, h, min) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, h);
            selectedDateTime.set(Calendar.MINUTE, min);
            updateDateTimeButtons();
        }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), true).show());

        // 时长 NumberPicker（分钟）
        numberPickerDuration.setMinValue(1);
        numberPickerDuration.setMaxValue(180);
        numberPickerDuration.setValue(5);
        numberPickerDuration.setWrapSelectorWheel(false);

        // 模式 Spinner
        spinnerMode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                customModeLayout.setVisibility(pos == MODES.length - 1 ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(ctx,
                android.R.layout.simple_spinner_item, MODES);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(modeAdapter);

        // 评分 SeekBar
        seekBarScore.setMax(10);
        seekBarScore.setProgress(5);
        updateScoreText(5);
        seekBarScore.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) { updateScoreText(p); }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        if (editMode) loadForEdit();
    }

    private void updateDateTimeButtons() {
        btnDate.setText("📅 " + DATE_FMT.format(selectedDateTime.getTime()));
        btnTime.setText("🕐 " + TIME_FMT.format(selectedDateTime.getTime()));
    }

    private void updateScoreText(int score) {
        textScoreValue.setText(score + " 分");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < score; i++) sb.append("★");
        for (int i = 0; i < 10 - score; i++) sb.append("☆");
        textScoreStars.setText(sb.toString());
    }

    private void save() {
        int minutes = numberPickerDuration.getValue();
        int duration = minutes * 60; // 存储为秒
        int score = seekBarScore.getProgress();
        int mode = spinnerMode.getSelectedItemPosition();
        String notes = editNotes.getText().toString().trim();

        SQLiteDatabase db = repository.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("date", selectedDateTime.getTimeInMillis());
        v.put("duration", duration);
        v.put("score", score);
        v.put("mode", mode);
        v.put("notes", notes.isEmpty() ? null : notes);
        if (spinnerMode.getSelectedItemPosition() == MODES.length - 1) {
            String cc = editCustomContent.getText().toString().trim();
            v.put("custom_content", cc.isEmpty() ? null : cc);
            String cm = editCustomMinutes.getText().toString().trim();
            v.put("custom_duration", cm.isEmpty() ? 300 : Integer.parseInt(cm) * 60);
        } else {
            v.putNull("custom_content");
            v.putNull("custom_duration");
        }
        v.put("user_id", 1);

        if (editMode) {
            int n = db.update("breathing_records", v, "id=?", new String[]{String.valueOf(editId)});
            db.close();
            if (n > 0) {
                Toast.makeText(ctx, "🫁 已更新", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(ctx, "更新失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            long id = db.insert("breathing_records", null, v);
            db.close();
            if (id > 0) {
                Toast.makeText(ctx, "🫁 打卡已保存", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(ctx, "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadForEdit() {
        BreathingRecord rec = findRecordById(editId);
        if (rec == null) { finish(); return; }

        if (rec.getDate() != null) selectedDateTime.setTime(rec.getDate());
        updateDateTimeButtons();

        int minutes = Math.max(1, Math.min(180, rec.getDuration() / 60));
        numberPickerDuration.setValue(minutes);

        int mode = rec.getMode();
        if (mode >= 0 && mode < MODES.length) spinnerMode.setSelection(mode);

        int score = Math.max(0, Math.min(10, rec.getScore()));
        seekBarScore.setProgress(score);
        updateScoreText(score);

        if (rec.getNotes() != null) editNotes.setText(rec.getNotes());
        String cc = rec.getCustomContent();
        if (cc != null && !cc.isEmpty()) {
            spinnerMode.setSelection(MODES.length - 1);
            customModeLayout.setVisibility(View.VISIBLE);
            editCustomContent.setText(cc);
            int cdMin = rec.getCustomDuration() > 0 ? rec.getCustomDuration() / 60 : 5;
            editCustomMinutes.setText(String.valueOf(cdMin));
        }

        ((TextView) findViewById(R.id.tvTitle)).setText("编辑呼吸打卡");
        ((Button) findViewById(R.id.btnSave)).setText("💾 保存修改");

        Button btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setVisibility(View.VISIBLE);
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(ctx)
                .setTitle("删除记录")
                .setMessage("确定要删除这条呼吸打卡记录吗？")
                .setPositiveButton("删除", (d, w) -> {
                    repository.deleteBreathingRecord(editId);
                    setResult(RESULT_OK);
                    finish();
                })
                .setNegativeButton("取消", null)
                .show());
    }

    private BreathingRecord findRecordById(int id) {
        List<BreathingRecord> all = repository.getAllBreathingRecords();
        for (BreathingRecord r : all) {
            if (r.getId() == id) return r;
        }
        return null;
    }
}
