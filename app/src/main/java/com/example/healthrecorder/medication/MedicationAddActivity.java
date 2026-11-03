package com.example.healthrecorder.medication;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;

import java.util.Locale;

public class MedicationAddActivity extends AppCompatActivity {

    private EditText editName, editDosage, editNotes;
    private Spinner spinnerTiming;
    private TextView textTime, tvTitle, btnClearTime;
    private HealthRepository repository;

    private int editId = -1;
    private String timeText = null;   // HH:mm，null 表示未设置

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_add);

        repository = HealthRepository.getInstance(getApplicationContext());

        editName = findViewById(R.id.editName);
        editDosage = findViewById(R.id.editDosage);
        editNotes = findViewById(R.id.editNotes);
        spinnerTiming = findViewById(R.id.spinnerTiming);
        textTime = findViewById(R.id.textTime);
        tvTitle = findViewById(R.id.tvTitle);
        btnClearTime = findViewById(R.id.btnClearTime);

        ArrayAdapter<String> timingAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"早晨", "中午", "晚上", "睡前"});
        timingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTiming.setAdapter(timingAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        textTime.setOnClickListener(v -> showTimePicker());
        btnClearTime.setOnClickListener(v -> {
            timeText = null;
            updateTimeDisplay();
        });

        editId = getIntent().getIntExtra("edit_id", -1);
        if (editId != -1) {
            tvTitle.setText("💊 编辑药品");
            loadMedication();
        }

        findViewById(R.id.btnSave).setOnClickListener(v -> save());
    }

    private void loadMedication() {
        Medication m = repository.getMedicationById(editId);
        if (m == null) return;
        editName.setText(m.getName());
        editDosage.setText(m.getDosage());
        editNotes.setText(m.getNotes());
        spinnerTiming.setSelection(m.getTiming(), false);
        timeText = m.getTimeText();
        updateTimeDisplay();
    }

    private void showTimePicker() {
        int hour = 8, minute = 0;
        if (timeText != null && timeText.contains(":")) {
            try {
                String[] parts = timeText.split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
        }
        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int h, int mnt) {
                timeText = String.format(Locale.US, "%02d:%02d", h, mnt);
                updateTimeDisplay();
            }
        }, hour, minute, true).show();
    }

    private void updateTimeDisplay() {
        if (timeText == null) {
            textTime.setText("未设置");
            textTime.setTextColor(0xFF999999);
        } else {
            textTime.setText(timeText);
            textTime.setTextColor(0xFF333333);
        }
    }

    private void save() {
        String name = editName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请填写药品名", Toast.LENGTH_SHORT).show();
            return;
        }
        String dosage = editDosage.getText().toString().trim();
        String notes = editNotes.getText().toString().trim();

        Medication m = new Medication();
        m.setName(name);
        m.setDosage(dosage.isEmpty() ? null : dosage);
        m.setNotes(notes.isEmpty() ? null : notes);
        m.setTiming(spinnerTiming.getSelectedItemPosition());
        m.setTimeText(timeText);
        m.setActive(true);

        if (editId == -1) {
            m.setUserId(1);
            repository.addMedication(m);
            Toast.makeText(this, "已添加", Toast.LENGTH_SHORT).show();
        } else {
            Medication old = repository.getMedicationById(editId);
            m.setId(editId);
            m.setUserId(old != null ? old.getUserId() : 1);
            repository.updateMedication(m);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK);
        finish();
    }
}
