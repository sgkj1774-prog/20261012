package com.example.healthrecorder.teeth;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;
import com.example.healthrecorder.User;
import com.example.healthrecorder.teeth.TeethRecord;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TeethRecordActivity extends AppCompatActivity {
    private int selectedUserId = 1;
    private int editId = -1;
    private boolean editMode = false;
    private List<User> userList;
    private Calendar selectedDateTime = Calendar.getInstance();
    private HealthRepository repository;
    private final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teeth_record);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());

        editId = getIntent().getIntExtra("edit_id", -1);
        editMode = editId > 0;

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

        // 成员下拉
        Spinner spinnerUser = findViewById(R.id.spinnerUser);
        userList = repository.getAllUsers();
        String[] items = new String[userList.size()];
        for (int i = 0; i < userList.size(); i++) {
            items[i] = userList.get(i).getName();
        }
        if (items.length == 0) items = new String[]{"默认成员"};
        else selectedUserId = userList.get(0).getId();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUser.setAdapter(adapter);

        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        if (editMode) loadForEdit();
    }

    private void save() {
        EditText editNotes = findViewById(R.id.editNotes);
        Spinner spinnerUser = findViewById(R.id.spinnerUser);
        String notes = editNotes.getText().toString().trim();
        int pos = spinnerUser.getSelectedItemPosition();
        if (userList != null && userList.size() > 0 && pos >= 0 && pos < userList.size()) {
            selectedUserId = userList.get(pos).getId();
        }

        SQLiteDatabase db = repository.getWritableDatabase();
        android.content.ContentValues v = new android.content.ContentValues();
        v.put("date", selectedDateTime.getTimeInMillis());
        v.put("period", 0);  // 默认晨间
        v.put("duration", 0);  // 默认0分钟
        v.put("notes", notes.isEmpty() ? null : notes);
        v.put("user_id", selectedUserId);

        if (editMode) {
            int n = db.update("teeth_records", v, "id=?", new String[]{String.valueOf(editId)});
            db.close();
            if (n > 0) {
                Toast.makeText(ctx, "🦷 已更新", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(ctx, "更新失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            long id = db.insert("teeth_records", null, v);
            db.close();
            if (id > 0) {
                Toast.makeText(ctx, "🦷 记录已保存", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(ctx, "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadForEdit() {
        TeethRecord rec = repository.getTeethRecordById(editId);
        if (rec == null) { finish(); return; }
        selectedUserId = rec.getUserId();
        if (rec.getDate() != null) selectedDateTime.setTime(rec.getDate());

        Button btnDate = findViewById(R.id.btnDate);
        Button btnTime = findViewById(R.id.btnTime);
        btnDate.setText("📅 " + DATE_FMT.format(selectedDateTime.getTime()));
        btnTime.setText("🕐 " + TIME_FMT.format(selectedDateTime.getTime()));

        EditText editNotes = findViewById(R.id.editNotes);
        editNotes.setText(rec.getNotes() != null ? rec.getNotes() : "");

        Spinner spinnerUser = findViewById(R.id.spinnerUser);
        if (userList != null) {
            for (int i = 0; i < userList.size(); i++) {
                if (userList.get(i).getId() == selectedUserId) {
                    spinnerUser.setSelection(i);
                    break;
                }
            }
        }

        TextView tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText("🦷 编辑刷牙记录");
        Button btnSave = findViewById(R.id.btnSave);
        if (btnSave != null) btnSave.setText("💾 保存修改");

        Button btnDelete = findViewById(R.id.btnDelete);
        if (btnDelete != null) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(ctx)
                        .setTitle("删除记录")
                        .setMessage("确定要删除这条刷牙记录吗？")
                        .setPositiveButton("删除", (d, w) -> {
                            repository.deleteTeethRecord(editId);
                            setResult(RESULT_OK);
                            finish();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
        }
    }
}
