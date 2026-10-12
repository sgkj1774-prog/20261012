package com.example.healthrecorder.event;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;
import com.example.healthrecorder.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EventAddActivity extends AppCompatActivity {
    private HealthRepository repository;
    private Context ctx;
    private int editId = -1;
    private boolean editMode = false;

    private EditText editTitle;
    private EditText editContent;
    private Button btnEventDate;
    private Button btnEventTime;
    private Switch switchRemind;
    private View layoutRemindTime;
    private Button btnRemindDate;
    private Button btnRemindTime;
    private RadioGroup radioGroupPriority;
    private Spinner spinnerRepeat;

    private Calendar eventDateTime = Calendar.getInstance();
    private Calendar remindDateTime = Calendar.getInstance();
    private boolean remindEnabled = false;
    private int selectedPriority = 2; // 默认中优先级
    private int selectedRepeatType = 0;

    private final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private List<User> userList;
    private int selectedUserId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_add);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());

        editId = getIntent().getIntExtra("edit_id", -1);
        editMode = editId > 0;

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        btnEventDate = findViewById(R.id.btnEventDate);
        btnEventTime = findViewById(R.id.btnEventTime);
        switchRemind = findViewById(R.id.switchRemind);
        layoutRemindTime = findViewById(R.id.layoutRemindTime);
        btnRemindDate = findViewById(R.id.btnRemindDate);
        btnRemindTime = findViewById(R.id.btnRemindTime);
        radioGroupPriority = findViewById(R.id.radioGroupPriority);
        spinnerRepeat = findViewById(R.id.spinnerRepeat);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 事件日期
        btnEventDate.setText("📅 " + DATE_FMT.format(eventDateTime.getTime()));
        btnEventDate.setOnClickListener(v -> new DatePickerDialog(ctx, (dp, y, m, d) -> {
            eventDateTime.set(Calendar.YEAR, y);
            eventDateTime.set(Calendar.MONTH, m);
            eventDateTime.set(Calendar.DAY_OF_MONTH, d);
            btnEventDate.setText("📅 " + DATE_FMT.format(eventDateTime.getTime()));
        },
        eventDateTime.get(Calendar.YEAR), eventDateTime.get(Calendar.MONTH),
        eventDateTime.get(Calendar.DAY_OF_MONTH)).show());

        // 事件时间
        btnEventTime.setText("🕐 " + TIME_FMT.format(eventDateTime.getTime()));
        btnEventTime.setOnClickListener(v -> new TimePickerDialog(ctx, (tp, h, min) -> {
            eventDateTime.set(Calendar.HOUR_OF_DAY, h);
            eventDateTime.set(Calendar.MINUTE, min);
            btnEventTime.setText("🕐 " + TIME_FMT.format(eventDateTime.getTime()));
        },
        eventDateTime.get(Calendar.HOUR_OF_DAY), eventDateTime.get(Calendar.MINUTE), true).show());

        // 提醒开关
        switchRemind.setOnCheckedChangeListener((buttonView, isChecked) -> {
            remindEnabled = isChecked;
            layoutRemindTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // 提醒日期
        btnRemindDate.setText("📅 " + DATE_FMT.format(remindDateTime.getTime()));
        btnRemindDate.setOnClickListener(v -> new DatePickerDialog(ctx, (dp, y, m, d) -> {
            remindDateTime.set(Calendar.YEAR, y);
            remindDateTime.set(Calendar.MONTH, m);
            remindDateTime.set(Calendar.DAY_OF_MONTH, d);
            btnRemindDate.setText("📅 " + DATE_FMT.format(remindDateTime.getTime()));
        },
        remindDateTime.get(Calendar.YEAR), remindDateTime.get(Calendar.MONTH),
        remindDateTime.get(Calendar.DAY_OF_MONTH)).show());

        // 提醒时间
        btnRemindTime.setText("🕐 " + TIME_FMT.format(remindDateTime.getTime()));
        btnRemindTime.setOnClickListener(v -> new TimePickerDialog(ctx, (tp, h, min) -> {
            remindDateTime.set(Calendar.HOUR_OF_DAY, h);
            remindDateTime.set(Calendar.MINUTE, min);
            btnRemindTime.setText("🕐 " + TIME_FMT.format(remindDateTime.getTime()));
        },
        remindDateTime.get(Calendar.HOUR_OF_DAY), remindDateTime.get(Calendar.MINUTE), true).show());

        // 优先级默认选中"中"
        radioGroupPriority.check(R.id.radioMid);
        radioGroupPriority.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioHigh) selectedPriority = 3;
            else if (checkedId == R.id.radioMid) selectedPriority = 2;
            else selectedPriority = 1;
        });

        // 重复类型 Spinner
        String[] repeatItems = Event.repeatTextArr();
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(ctx,
                android.R.layout.simple_spinner_item, repeatItems);
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeat.setAdapter(repeatAdapter);

        // 成员 Spinner
        Spinner spinnerUser = findViewById(R.id.spinnerUser);
        userList = repository.getAllUsers();
        String[] userNames = new String[userList.size()];
        for (int i = 0; i < userList.size(); i++) {
            userNames[i] = userList.get(i).getName();
            if (userList.get(i).getId() == 1) selectedUserId = 1;
        }
        if (userNames.length == 0) {
            userNames = new String[]{"默认成员"};
        } else {
            selectedUserId = userList.get(0).getId();
        }
        ArrayAdapter<String> userAdapter = new ArrayAdapter<>(ctx,
                android.R.layout.simple_spinner_item, userNames);
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUser.setAdapter(userAdapter);

        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        Button btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(ctx)
                    .setTitle("删除重要事项")
                    .setMessage("确定要删除这条重要事项吗？")
                    .setPositiveButton("删除", (d, w) -> {
                        repository.deleteEvent(editId);
                        setResult(RESULT_OK);
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        if (editMode) {
            TextView tvTitle = findViewById(R.id.tvTitle);
            if (tvTitle != null) tvTitle.setText("✏️ 编辑重要事项");
            Button btnSave = findViewById(R.id.btnSave);
            if (btnSave != null) btnSave.setText("💾 保存修改");
            btnDelete.setVisibility(View.VISIBLE);
            loadForEdit();
        }
    }

    private void save() {
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(ctx, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }
        String content = editContent.getText().toString().trim();

        Spinner spinnerUser = findViewById(R.id.spinnerUser);
        int pos = spinnerUser.getSelectedItemPosition();
        if (userList != null && userList.size() > 0 && pos >= 0 && pos < userList.size()) {
            selectedUserId = userList.get(pos).getId();
        }

        selectedRepeatType = spinnerRepeat.getSelectedItemPosition();

        Event ev = new Event();
        ev.setTitle(title);
        ev.setContent(content.isEmpty() ? null : content);
        ev.setEventTime(eventDateTime.getTime());
        ev.setRemindEnabled(remindEnabled);
        ev.setRemindTime(remindEnabled ? remindDateTime.getTime() : null);
        ev.setPriority(selectedPriority);
        ev.setRepeatType(selectedRepeatType);
        ev.setRepeatInterval(1);
        ev.setUserId(selectedUserId);

        if (editMode) {
            ev.setId(editId);
            ev.setStatus(0);
            repository.updateEvent(ev);
            Toast.makeText(ctx, "✓ 已更新", Toast.LENGTH_SHORT).show();
        } else {
            ev.setStatus(0);
            ev.setCreatedAt(new java.util.Date());
            long id = repository.addEvent(ev);
            if (id > 0) {
                Toast.makeText(ctx, "✓ 已保存", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ctx, "保存失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        setResult(RESULT_OK);
        finish();
    }

    private void loadForEdit() {
        java.util.List<Event> all = repository.getAllEvents();
        Event ev = null;
        if (all != null) {
            for (Event e : all) {
                if (e.getId() == editId) {
                    ev = e;
                    break;
                }
            }
        }
        if (ev == null) { finish(); return; }

        editTitle.setText(ev.getTitle() != null ? ev.getTitle() : "");
        editContent.setText(ev.getContent() != null ? ev.getContent() : "");

        if (ev.getEventTime() != null) eventDateTime.setTime(ev.getEventTime());
        btnEventDate.setText("📅 " + DATE_FMT.format(eventDateTime.getTime()));
        btnEventTime.setText("🕐 " + TIME_FMT.format(eventDateTime.getTime()));

        remindEnabled = ev.isRemindEnabled();
        switchRemind.setChecked(remindEnabled);
        layoutRemindTime.setVisibility(remindEnabled ? View.VISIBLE : View.GONE);
        if (remindEnabled && ev.getRemindTime() != null) {
            remindDateTime.setTime(ev.getRemindTime());
        }
        btnRemindDate.setText("📅 " + DATE_FMT.format(remindDateTime.getTime()));
        btnRemindTime.setText("🕐 " + TIME_FMT.format(remindDateTime.getTime()));

        selectedPriority = ev.getPriority();
        if (selectedPriority == 3) radioGroupPriority.check(R.id.radioHigh);
        else if (selectedPriority == 2) radioGroupPriority.check(R.id.radioMid);
        else radioGroupPriority.check(R.id.radioLow);

        if (ev.getRepeatType() >= 0 && ev.getRepeatType() < 5) {
            spinnerRepeat.setSelection(ev.getRepeatType());
        }

        selectedUserId = ev.getUserId();
        Spinner spinnerUser = findViewById(R.id.spinnerUser);
        if (userList != null) {
            for (int i = 0; i < userList.size(); i++) {
                if (userList.get(i).getId() == selectedUserId) {
                    spinnerUser.setSelection(i);
                    break;
                }
            }
        }
    }
}
