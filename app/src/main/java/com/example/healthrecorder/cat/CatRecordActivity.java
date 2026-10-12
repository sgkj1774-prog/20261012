package com.example.healthrecorder.cat;

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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.example.healthrecorder.DatabaseHelper;

public class CatRecordActivity extends AppCompatActivity {
    private int selectedScore = 3;
    private int selectedCount = 8;
    private int selectedWillingness = 0;
    private int selectedProcessType = 0;
    private Calendar selectedDateTime = Calendar.getInstance();
    private final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private Button[] scoreButtons;
    private Button[] countButtons;
    private TextView textStoolCount;
    private HealthRepository repository;
    private Context ctx;
    private List<User> userList;
    private int selectedUserId = 1;
    private int editId = -1;
    private boolean editMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cat_record);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());
        editId = getIntent().getIntExtra("edit_id", -1);
        editMode = editId > 0;

        Button btnScore1 = findViewById(R.id.btnScore1);
        Button btnScore2 = findViewById(R.id.btnScore2);
        Button btnScore3 = findViewById(R.id.btnScore3);
        Button btnScore4 = findViewById(R.id.btnScore4);
        Button btnScore5 = findViewById(R.id.btnScore5);
        scoreButtons = new Button[]{btnScore1, btnScore2, btnScore3, btnScore4, btnScore5};

        textStoolCount = findViewById(R.id.textStoolCount);
        Button btnCount4 = findViewById(R.id.btnCount4);
        Button btnCount6 = findViewById(R.id.btnCount6);
        Button btnCount8 = findViewById(R.id.btnCount8);
        Button btnCount10 = findViewById(R.id.btnCount10);
        Button btnCount12 = findViewById(R.id.btnCount12);
        countButtons = new Button[]{btnCount4, btnCount6, btnCount8, btnCount10, btnCount12};

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Button btnDate = findViewById(R.id.btnDate);
        Button btnTime = findViewById(R.id.btnTime);
        btnDate.setText("📅 " + DATE_FMT.format(selectedDateTime.getTime()));
        btnTime.setText("🕐 " + TIME_FMT.format(selectedDateTime.getTime()));

        btnDate.setOnClickListener(v -> {
            new DatePickerDialog(ctx, (dp, y, m, d) -> {
                selectedDateTime.set(Calendar.YEAR, y);
                selectedDateTime.set(Calendar.MONTH, m);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, d);
                btnDate.setText("📅 " + DATE_FMT.format(selectedDateTime.getTime()));
                updateFoodBrand();
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnTime.setOnClickListener(v -> {
            new TimePickerDialog(ctx, (tp, h, min) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, h);
                selectedDateTime.set(Calendar.MINUTE, min);
                btnTime.setText("🕐 " + TIME_FMT.format(selectedDateTime.getTime()));
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE), true).show();
        });

        View.OnClickListener scoreListener = v -> {
            selectedScore = Integer.parseInt(v.getTag().toString());
            updateScoreButtons();
            TextView desc = findViewById(R.id.textScoreDesc);
            desc.setText(getScoreDesc(selectedScore));
        };
        btnScore1.setTag(1); btnScore2.setTag(2); btnScore3.setTag(3); btnScore4.setTag(4); btnScore5.setTag(5);
        for (Button b : scoreButtons) b.setOnClickListener(scoreListener);
        updateScoreButtons();
        ((TextView)findViewById(R.id.textScoreDesc)).setText(getScoreDesc(3));

        // 数量选择
        int[] presetCounts = {4, 6, 8, 10, 12};
        for (int i = 0; i < countButtons.length; i++) {
            final int count = presetCounts[i];
            countButtons[i].setOnClickListener(v -> {
                selectedCount = count;
                updateCountDisplay();
            });
        }
        findViewById(R.id.btnCountMinus).setOnClickListener(v -> {
            if (selectedCount > 1) { selectedCount--; updateCountDisplay(); }
        });
        findViewById(R.id.btnCountPlus).setOnClickListener(v -> {
            if (selectedCount < 20) { selectedCount++; updateCountDisplay(); }
        });
        updateCountDisplay();

        // 如厕意愿
        Button btnWillGood = findViewById(R.id.btnWillGood);
        Button btnWillMid = findViewById(R.id.btnWillMid);
        Button btnWillBad = findViewById(R.id.btnWillBad);
        View.OnClickListener willListener = v -> {
            selectedWillingness = v.getId() == R.id.btnWillGood ? 0 : v.getId() == R.id.btnWillMid ? 1 : 2;
            updateWillButtons();
        };
        btnWillGood.setOnClickListener(willListener);
        btnWillMid.setOnClickListener(willListener);
        btnWillBad.setOnClickListener(willListener);
        updateWillButtons();

        // 如厕过程
        Button btnProcGood = findViewById(R.id.btnProcGood);
        Button btnProcMid = findViewById(R.id.btnProcMid);
        Button btnProcBad = findViewById(R.id.btnProcBad);
        View.OnClickListener procListener = v -> {
            selectedProcessType = v.getId() == R.id.btnProcGood ? 0 : v.getId() == R.id.btnProcMid ? 1 : 2;
            updateProcButtons();
        };
        btnProcGood.setOnClickListener(procListener);
        btnProcMid.setOnClickListener(procListener);
        btnProcBad.setOnClickListener(procListener);
        updateProcButtons();

        // 成员下拉
        Spinner spinnerUser = findViewById(R.id.spinnerUser);
        userList = repository.getAllUsers();
        String[] items = new String[userList.size()];
        for (int i = 0; i < userList.size(); i++) {
            items[i] = userList.get(i).getName();
            if (userList.get(i).getId() == 1) selectedUserId = 1;
        }
        if (items.length == 0) {
            items = new String[]{"默认成员"};
        } else {
            selectedUserId = userList.get(0).getId();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUser.setAdapter(adapter);

        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        // 当日猫粮关联显示，点击进入猫粮管理
        TextView tvFood = findViewById(R.id.tvCurrentFood);
        if (tvFood != null) {
            tvFood.setOnClickListener(v ->
                    startActivity(new android.content.Intent(ctx, CatFoodListActivity.class)));
        }
        updateFoodBrand();

        if (editMode) loadForEdit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从猫粮管理页返回时刷新当日猫粮显示
        updateFoodBrand();
    }

    /** 与猫粮记录关联：显示当日（所选日期）使用的猫粮品牌 */
    private void updateFoodBrand() {
        TextView tvFood = findViewById(R.id.tvCurrentFood);
        if (tvFood == null) return;
        CatFoodRecord food = repository.getCatFoodRecordOnDate(selectedDateTime.getTimeInMillis());
        if (food != null) {
            tvFood.setText("🍚 当日猫粮：" + food.getBrand()
                    + "（" + food.getFoodType() + "）已喂 " + food.durationDays() + " 天");
        } else {
            tvFood.setText("🍚 当日猫粮：暂无记录（点击管理猫粮）");
        }
    }

    private void updateScoreButtons() {
        int[] colors = {0xFFE53935, 0xFFFF9800, 0xFF4CAF50, 0xFFFFEB3B, 0xFFE53935};
        for (int i = 0; i < 5; i++) {
            Button b = scoreButtons[i];
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    i + 1 == selectedScore ? colors[i] : 0xFFEEEEEE));
            b.setTextColor(android.graphics.Color.parseColor(
                    i + 1 == selectedScore ? (i == 3 ? "#212121" : "#FFFFFF") : "#9E9E9E"));
        }
    }

    private void updateCountDisplay() {
        textStoolCount.setText(selectedCount + " 颗");
        int[] presetCounts = {4, 6, 8, 10, 12};
        for (int i = 0; i < countButtons.length; i++) {
            Button b = countButtons[i];
            boolean isSelected = (selectedCount == presetCounts[i]);
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    isSelected ? 0xFF4CAF50 : 0xFFEEEEEE));
            b.setTextColor(isSelected ? 0xFFFFFFFF : 0xFF666666);
        }
    }

    private void updateWillButtons() {
        int g = 0xFF4CAF50, m = 0xFFFF9800, b = 0xFFE53935;
        Button[] btns = new Button[]{
                findViewById(R.id.btnWillGood),
                findViewById(R.id.btnWillMid),
                findViewById(R.id.btnWillBad)
        };
        int[] colors = {g, m, b};
        for (int i = 0; i < 3; i++) {
            boolean sel = i == selectedWillingness;
            btns[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(sel ? colors[i] : 0xFFEEEEEE));
            btns[i].setTextColor(android.graphics.Color.parseColor(sel ? "#FFFFFF" : "#9E9E9E"));
        }
    }

    private void updateProcButtons() {
        int g = 0xFF4CAF50, m = 0xFFFF9800, b = 0xFFE53935;
        Button[] btns = new Button[]{
                findViewById(R.id.btnProcGood),
                findViewById(R.id.btnProcMid),
                findViewById(R.id.btnProcBad)
        };
        int[] colors = {g, m, b};
        for (int i = 0; i < 3; i++) {
            boolean sel = i == selectedProcessType;
            btns[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(sel ? colors[i] : 0xFFEEEEEE));
            btns[i].setTextColor(android.graphics.Color.parseColor(sel ? "#FFFFFF" : "#9E9E9E"));
        }
    }

    private String getScoreDesc(int s) {
        switch (s) {
            case 1: return "便秘（硬、干结）⚠️ 建议多喝水+纤维";
            case 2: return "略干 - 继续观察";
            case 3: return "正常 👍 - 猫咪状态良好";
            case 4: return "略软 - 观察是否有其他症状";
            case 5: return "腹泻 ⚠️ - 建议就医或换粮";
            default: return "";
        }
    }

    private void save() {
        EditText editNotes = findViewById(R.id.editNotes);
        String notes = editNotes.getText().toString().trim();
        Spinner spinnerUser = findViewById(R.id.spinnerUser);
        int pos = spinnerUser.getSelectedItemPosition();
        if (userList != null && userList.size() > 0 && pos >= 0 && pos < userList.size()) {
            selectedUserId = userList.get(pos).getId();
        }

        SQLiteDatabase db = repository.getWritableDatabase();
        android.content.ContentValues v = new android.content.ContentValues();
        v.put("date", selectedDateTime.getTimeInMillis());
        v.put("stool_type", selectedScore);
        v.put("stool_count", selectedCount);
        v.put("willingness", selectedWillingness);
        v.put("process_type", selectedProcessType);
        v.put("notes", notes.isEmpty() ? null : notes);
        v.put("user_id", selectedUserId);
        if (editMode) {
            int n = db.update("cat_records", v, "id=?", new String[]{String.valueOf(editId)});
            db.close();
            if (n > 0) {
                Toast.makeText(ctx, "🐱 已更新", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(ctx, "更新失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            long id = db.insert("cat_records", null, v);
            db.close();
            if (id > 0) {
                Toast.makeText(ctx, "🐱 记录已保存", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(ctx, "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadForEdit() {
        com.example.healthrecorder.cat.CatRecord rec = repository.getCatRecordById(editId);
        if (rec == null) { finish(); return; }
        selectedScore = rec.getStoolType();
        selectedCount = rec.getStoolCount();
        selectedWillingness = rec.getWillingness();
        selectedProcessType = rec.getProcessType();
        if (rec.getDate() != null) selectedDateTime.setTime(rec.getDate());
        selectedUserId = rec.getUserId();
        updateFoodBrand();

        Button btnDate = findViewById(R.id.btnDate);
        Button btnTime = findViewById(R.id.btnTime);
        btnDate.setText("📅 " + DATE_FMT.format(selectedDateTime.getTime()));
        btnTime.setText("🕐 " + TIME_FMT.format(selectedDateTime.getTime()));
        updateScoreButtons();
        ((TextView) findViewById(R.id.textScoreDesc)).setText(getScoreDesc(selectedScore));
        updateCountDisplay();
        updateWillButtons();
        updateProcButtons();

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
        if (tvTitle != null) tvTitle.setText("🐱 编辑猫咪如厕记录");
        Button btnSave = findViewById(R.id.btnSave);
        if (btnSave != null) btnSave.setText("💾 保存修改");

        Button btnDelete = findViewById(R.id.btnDelete);
        if (btnDelete != null) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(ctx)
                        .setTitle("删除记录")
                        .setMessage("确定要删除这条猫咪如厕记录吗？")
                        .setPositiveButton("删除", (d, w) -> {
                            repository.deleteCatRecord(editId);
                            setResult(RESULT_OK);
                            finish();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
        }
    }
}
