package com.example.healthrecorder.medication;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;

import java.util.Calendar;
import java.util.Locale;

public class MedicationAddActivity extends AppCompatActivity {

    private EditText editName, editDosage, editNotes;
    private EditText editSpec, editApprovalNo, editMah, editManufacturer, editPacker, editCompanyAddress;
    private Spinner spinnerTiming;
    private TextView textTime, tvTitle, btnClearTime;
    private TextView textProductionDate, textExpiryDate;
    private HealthRepository repository;

    private int editId = -1;
    private String timeText = null;   // HH:mm，null 表示未设置
    private String productionDate = null; // yyyy-MM-dd
    private String expiryDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_add);

        repository = HealthRepository.getInstance(getApplicationContext());

        editName = findViewById(R.id.editName);
        editDosage = findViewById(R.id.editDosage);
        editNotes = findViewById(R.id.editNotes);
        editSpec = findViewById(R.id.editSpec);
        editApprovalNo = findViewById(R.id.editApprovalNo);
        editMah = findViewById(R.id.editMah);
        editManufacturer = findViewById(R.id.editManufacturer);
        editPacker = findViewById(R.id.editPacker);
        editCompanyAddress = findViewById(R.id.editCompanyAddress);
        spinnerTiming = findViewById(R.id.spinnerTiming);
        textTime = findViewById(R.id.textTime);
        tvTitle = findViewById(R.id.tvTitle);
        btnClearTime = findViewById(R.id.btnClearTime);
        textProductionDate = findViewById(R.id.textProductionDate);
        textExpiryDate = findViewById(R.id.textExpiryDate);

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

        textProductionDate.setOnClickListener(v -> pickDate(true));
        textExpiryDate.setOnClickListener(v -> pickDate(false));
        findViewById(R.id.btnClearProduction).setOnClickListener(v -> {
            productionDate = null;
            updateDateDisplay();
        });
        findViewById(R.id.btnClearExpiry).setOnClickListener(v -> {
            expiryDate = null;
            updateDateDisplay();
        });

        editId = getIntent().getIntExtra("edit_id", -1);
        if (editId != -1) {
            tvTitle.setText("💊 编辑药品");
            loadMedication();
        }
        updateTimeDisplay();
        updateDateDisplay();

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

        editSpec.setText(m.getSpec());
        editApprovalNo.setText(m.getApprovalNo());
        editMah.setText(m.getMah());
        editManufacturer.setText(m.getManufacturer());
        editPacker.setText(m.getPacker());
        editCompanyAddress.setText(m.getCompanyAddress());
        productionDate = m.getProductionDate();
        expiryDate = m.getExpiryDate();
    }

    /** 选日期：已有值则定位到那一年，否则默认今天 */
    private void pickDate(final boolean production) {
        Calendar c = Medication.parseDate(production ? productionDate : expiryDate);
        if (c == null) c = Calendar.getInstance();
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                if (production) productionDate = Medication.formatDate(year, month, dayOfMonth);
                else expiryDate = Medication.formatDate(year, month, dayOfMonth);
                updateDateDisplay();
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateDisplay() {
        setDateText(textProductionDate, productionDate);
        setDateText(textExpiryDate, expiryDate);
    }

    private void setDateText(TextView tv, String value) {
        if (value == null || value.trim().isEmpty()) {
            tv.setText("点击选择");
            tv.setTextColor(0xFF999999);
        } else {
            tv.setText(value.trim());
            tv.setTextColor(0xFF333333);
        }
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

    private static String textOrNull(EditText e) {
        String s = e.getText().toString().trim();
        return s.isEmpty() ? null : s;
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

        m.setProductionDate(productionDate);
        m.setExpiryDate(expiryDate);
        m.setSpec(textOrNull(editSpec));
        m.setApprovalNo(textOrNull(editApprovalNo));
        m.setMah(textOrNull(editMah));
        m.setManufacturer(textOrNull(editManufacturer));
        m.setPacker(textOrNull(editPacker));
        m.setCompanyAddress(textOrNull(editCompanyAddress));

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
