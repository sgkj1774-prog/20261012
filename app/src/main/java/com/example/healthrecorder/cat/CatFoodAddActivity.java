package com.example.healthrecorder.cat;

import android.app.DatePickerDialog;
import android.content.Context;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 猫粮记录添加/编辑页
 * 业务逻辑：
 * - 新增记录时默认 end_date=null（表示当前在吃）
 * - 换粮时：旧的"进行中"记录 end_date 更新为新记录的开始日期，同时新增一条新记录
 * - 点击"换粮了"按钮弹出日期选择填写结束日期
 * - 顶部显示当前正在使用的猫粮品牌（与猫如厕关联）
 */
public class CatFoodAddActivity extends AppCompatActivity {
    private static final String[] FOOD_TYPES = {"干粮", "湿粮", "罐头", "冻干", "零食"};

    private HealthRepository repository;
    private Context ctx;
    private final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private EditText editBrand, editNotes;
    private Spinner spinnerType;
    private Button btnStartDate, btnChangeFood, btnClearEnd;
    private TextView tvTitle, tvDuration, tvEndDate, tvCurrentFood;

    private Calendar startCal = Calendar.getInstance();
    private Date endDate = null;   // null = 持续中（当前在吃）
    private int editId = -1;
    private CatFoodRecord editingRecord = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cat_food_add);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());

        tvTitle = findViewById(R.id.tvTitle);
        tvCurrentFood = findViewById(R.id.tvCurrentFood);
        editBrand = findViewById(R.id.editBrand);
        editNotes = findViewById(R.id.editNotes);
        spinnerType = findViewById(R.id.spinnerType);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnChangeFood = findViewById(R.id.btnChangeFood);
        btnClearEnd = findViewById(R.id.btnClearEnd);
        tvDuration = findViewById(R.id.tvDuration);
        tvEndDate = findViewById(R.id.tvEndDate);

        // 类型下拉：干粮/湿粮/罐头/冻干/零食
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(ctx,
                android.R.layout.simple_spinner_item, FOOD_TYPES);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        // 开始日期选择
        btnStartDate.setOnClickListener(v -> {
            new DatePickerDialog(ctx, (dp, y, m, d) -> {
                startCal.set(Calendar.YEAR, y);
                startCal.set(Calendar.MONTH, m);
                startCal.set(Calendar.DAY_OF_MONTH, d);
                normalizeStart();
                refreshDateViews();
            },
            startCal.get(Calendar.YEAR),
            startCal.get(Calendar.MONTH),
            startCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // "换粮了"：弹出日期选择填写结束日期
        btnChangeFood.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            if (endDate != null) c.setTime(endDate);
            new DatePickerDialog(ctx, (dp, y, m, d) -> {
                Calendar end = Calendar.getInstance();
                end.set(y, m, d, 23, 59, 59);
                end.set(Calendar.MILLISECOND, 0);
                if (end.getTimeInMillis() < startCal.getTimeInMillis()) {
                    Toast.makeText(ctx, "结束日期不能早于开始日期", Toast.LENGTH_SHORT).show();
                    return;
                }
                endDate = end.getTime();
                refreshDateViews();
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 清除结束日期（恢复为"当前在吃"）
        btnClearEnd.setOnClickListener(v -> {
            endDate = null;
            refreshDateViews();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        // 编辑模式
        editId = getIntent().getIntExtra("edit_id", -1);
        if (editId != -1) {
            tvTitle.setText("🍚 编辑猫粮记录");
            loadEditData();
        } else {
            tvTitle.setText("🍚 添加猫粮记录");
        }

        normalizeStart();
        refreshCurrentFoodBanner();
        refreshDateViews();
    }

    /** 开始日期归一化到当天 00:00 */
    private void normalizeStart() {
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
    }

    /** 与猫如厕关联：显示当前正在使用的猫粮品牌 */
    private void refreshCurrentFoodBanner() {
        CatFoodRecord current = repository.getCurrentCatFoodRecord();
        if (current != null && current.getId() != editId) {
            tvCurrentFood.setText("🐱 当前在吃：" + current.getBrand()
                    + "（" + current.getFoodType() + "）已 " + current.durationDays()
                    + " 天\n保存新记录后将自动结束上一条");
            tvCurrentFood.setVisibility(View.VISIBLE);
        } else {
            tvCurrentFood.setVisibility(View.GONE);
        }
    }

    /** 刷新开始/结束日期与持续时长显示 */
    private void refreshDateViews() {
        btnStartDate.setText("📅 " + DATE_FMT.format(startCal.getTime()));

        // 持续时长
        long end = (endDate != null) ? endDate.getTime() : System.currentTimeMillis();
        long diff = end - startCal.getTimeInMillis();
        int days = diff < 0 ? 0 : Math.max((int) (diff / (24L * 60 * 60 * 1000)), 1);
        if (endDate == null) {
            tvDuration.setText("⏱ 持续中 · 已喂 " + days + " 天");
            tvDuration.setTextColor(0xFF4CAF50);
            tvEndDate.setText("未设置（当前在吃）");
            btnClearEnd.setVisibility(View.GONE);
        } else {
            tvDuration.setText("⏱ 共喂了 " + days + " 天");
            tvDuration.setTextColor(0xFF999999);
            tvEndDate.setText(DATE_FMT.format(endDate));
            btnClearEnd.setVisibility(View.VISIBLE);
        }
    }

    private void loadEditData() {
        editingRecord = repository.getCatFoodRecordById(editId);
        if (editingRecord == null) {
            Toast.makeText(this, "记录不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        editBrand.setText(editingRecord.getBrand());
        editNotes.setText(editingRecord.getNotes());
        if (editingRecord.getStartDate() != null) {
            startCal.setTime(editingRecord.getStartDate());
        }
        endDate = editingRecord.getEndDate();
        // 类型回显
        String type = editingRecord.getFoodType();
        for (int i = 0; i < FOOD_TYPES.length; i++) {
            if (FOOD_TYPES[i].equals(type)) {
                spinnerType.setSelection(i);
                break;
            }
        }
    }

    private void save() {
        String brand = editBrand.getText().toString().trim();
        if (brand.isEmpty()) {
            editBrand.setError("请输入猫粮品牌/型号");
            return;
        }
        String foodType = FOOD_TYPES[spinnerType.getSelectedItemPosition()];
        String notes = editNotes.getText().toString().trim();

        CatFoodRecord record = new CatFoodRecord();
        if (editId != -1) record.setId(editId);
        record.setBrand(brand);
        record.setFoodType(foodType);
        record.setStartDate(startCal.getTime());
        record.setEndDate(endDate);  // null = 当前在吃
        record.setNotes(notes.isEmpty() ? null : notes);
        record.setUserId(editingRecord != null ? editingRecord.getUserId() : 1);

        if (editId == -1) {
            // 换粮逻辑：新记录为"进行中"时，自动结束旧的"进行中"记录
            if (endDate == null) {
                CatFoodRecord current = repository.getCurrentCatFoodRecord();
                if (current != null) {
                    current.setEndDate(startCal.getTime());
                    repository.updateCatFoodRecord(current);
                }
            }
            repository.insertCatFoodRecord(record);
            Toast.makeText(this, "🍚 猫粮记录已添加", Toast.LENGTH_SHORT).show();
        } else {
            repository.updateCatFoodRecord(record);
            Toast.makeText(this, "🍚 保存成功", Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK);
        finish();
    }
}
