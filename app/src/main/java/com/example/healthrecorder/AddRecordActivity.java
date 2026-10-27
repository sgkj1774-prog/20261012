package com.example.healthrecorder;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddRecordActivity extends AppCompatActivity {
    private EditText editWeight, editSystolic, editDiastolic, editNotes;
    private TextView textDateTime;
    private Button btnSave;
    private HealthRepository repository;
    private Calendar selectedDateTime;
    private int editRecordId = -1;
    private Spinner spinnerUser;
    private List<User> userList;
    private List<Integer> userIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_record);

        repository = HealthRepository.getInstance(getApplicationContext());
        editWeight = findViewById(R.id.editWeight);
        editSystolic = findViewById(R.id.editSystolic);
        editDiastolic = findViewById(R.id.editDiastolic);
        editNotes = findViewById(R.id.editNotes);
        textDateTime = findViewById(R.id.textDateTime);
        btnSave = findViewById(R.id.btnSave);
        spinnerUser = findViewById(R.id.spinnerUser);

        selectedDateTime = Calendar.getInstance();
        updateDateTimeDisplay();
        setupUserSpinner();

        // 兼容两种 extra key：主页底部入口传 record_id，健康记录列表页传 edit_id
        editRecordId = getIntent().getIntExtra("record_id", -1);
        if (editRecordId == -1) {
            editRecordId = getIntent().getIntExtra("edit_id", -1);
        }
        if (editRecordId != -1) {
            loadRecord(editRecordId);
        }

        textDateTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRecord();
            }
        });
    }

    private void setupUserSpinner() {
        userList = repository.getAllUsers();
        List<String> names = new ArrayList<>();
        userIds = new ArrayList<>();
        for (User u : userList) {
            names.add(u.getName());
            userIds.add(u.getId());
        }
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUser.setAdapter(adapter);
        spinnerUser.setSelection(0);

        spinnerUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // selectedUserId captured via userIds list in saveRecord()
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadRecord(int id) {
        Record record = repository.getRecordById(id);
        if (record != null) {
            // 只回填实际存在的数据，避免把 0 写进空字段（保存时会被当成有效值）
            if (record.hasWeight()) {
                editWeight.setText(String.valueOf(record.getWeight()));
            }
            if (record.hasBloodPressure()) {
                editSystolic.setText(String.valueOf(record.getSystolic()));
                editDiastolic.setText(String.valueOf(record.getDiastolic()));
            }
            editNotes.setText(record.getNotes());
            selectedDateTime.setTime(record.getDate());
            updateDateTimeDisplay();
            // Set spinner selection to the record's user
            for (int i = 0; i < userIds.size(); i++) {
                if (userIds.get(i) == record.getUserId()) {
                    spinnerUser.setSelection(i);
                    break;
                }
            }
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, day);
                showTimePicker();
            }
        }, selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hour, int minute) {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hour);
                selectedDateTime.set(Calendar.MINUTE, minute);
                updateDateTimeDisplay();
            }
        }, selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE), true).show();
    }

    private void updateDateTimeDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        textDateTime.setText(sdf.format(selectedDateTime.getTime()));
    }

    private void saveRecord() {
        String weightStr = editWeight.getText().toString().trim();
        String systolicStr = editSystolic.getText().toString().trim();
        String diastolicStr = editDiastolic.getText().toString().trim();
        String notes = editNotes.getText().toString().trim();

        float weight = 0;
        int systolic = 0;
        int diastolic = 0;
        boolean hasWeight = !weightStr.isEmpty();
        boolean hasSystolic = !systolicStr.isEmpty();
        boolean hasDiastolic = !diastolicStr.isEmpty();

        if (!hasWeight && !hasSystolic && !hasDiastolic) {
            Toast.makeText(this, "请至少填写体重或血压其中一项", Toast.LENGTH_SHORT).show();
            return;
        }

        if ((hasSystolic && !hasDiastolic) || (!hasSystolic && hasDiastolic)) {
            Toast.makeText(this, "血压需要同时填写收缩压和舒张压", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (hasWeight) {
                weight = Float.parseFloat(weightStr);
                if (weight <= 0 || weight > 500) {
                    Toast.makeText(this, "体重数值异常（需在0.01~500 kg之间）", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (hasSystolic && hasDiastolic) {
                systolic = Integer.parseInt(systolicStr);
                diastolic = Integer.parseInt(diastolicStr);
                if (systolic < 60 || systolic > 300 || diastolic < 30 || diastolic > 200) {
                    Toast.makeText(this, "血压数值异常", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            int selectedUserId = userIds.get(spinnerUser.getSelectedItemPosition());

            Record record = new Record(weight, systolic, diastolic,
                    notes.isEmpty() ? null : notes, selectedDateTime.getTime());
            record.setUserId(selectedUserId);

            if (editRecordId != -1) {
                record.setId(editRecordId);
                repository.updateRecord(record);
                Toast.makeText(this, "记录已更新", Toast.LENGTH_SHORT).show();
            } else {
                repository.addRecord(record);
                Toast.makeText(this, "记录已保存", Toast.LENGTH_SHORT).show();
            }
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效数值", Toast.LENGTH_SHORT).show();
        }
    }
}
