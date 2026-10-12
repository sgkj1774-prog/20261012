package com.example.healthrecorder.cat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;

import java.util.ArrayList;
import java.util.List;

/** 猫粮记录列表：显示所有猫粮记录，当前在吃的显示绿色边框；右上角切换编辑/删除模式 */
public class CatFoodListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CatFoodRecordAdapter adapter;
    private List<CatFoodRecord> allRecords = new ArrayList<>();
    private HealthRepository repository;
    private Context ctx;
    private Button btnEditMode;

    private final ActivityResultLauncher<Intent> addLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK) loadData();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cat_food_list);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(ctx));
        adapter = new CatFoodRecordAdapter();
        recyclerView.setAdapter(adapter);

        // 普通模式点击进入编辑
        adapter.setOnItemClickListener(r -> {
            Intent intent = new Intent(ctx, CatFoodAddActivity.class);
            intent.putExtra("edit_id", r.getId());
            addLauncher.launch(intent);
        });

        // 删除（删除模式点击 / 长按触发）
        adapter.setOnItemDeleteListener(r -> {
            new AlertDialog.Builder(ctx)
                    .setTitle("删除「" + r.getBrand() + "」")
                    .setMessage("确定要删除这条猫粮记录吗？")
                    .setPositiveButton("删除", (d, w) -> {
                        repository.deleteCatFoodRecord(r.getId());
                        loadData();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 右上角切换编辑/删除模式
        btnEditMode = findViewById(R.id.btnEditMode);
        btnEditMode.setOnClickListener(v -> {
            boolean newMode = !adapter.isDeleteMode();
            adapter.setDeleteMode(newMode);
            btnEditMode.setText(newMode ? "完成" : "管理");
            btnEditMode.setTextColor(newMode ? 0xFFE53935 : 0xFF333333);
        });

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                addLauncher.launch(new Intent(ctx, CatFoodAddActivity.class)));

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        allRecords.clear();
        allRecords.addAll(repository.getAllCatFoodRecords());
        adapter.setList(allRecords);

        // 顶部当前猫粮提示
        TextView tvCurrent = findViewById(R.id.tvCurrentFood);
        CatFoodRecord current = repository.getCurrentCatFoodRecord();
        if (current != null) {
            tvCurrent.setText("🍚 当前在吃：" + current.getBrand()
                    + "（" + current.getFoodType() + "）已 " + current.durationDays() + " 天");
            tvCurrent.setVisibility(View.VISIBLE);
        } else {
            tvCurrent.setText("🍚 当前暂无正在使用的猫粮，点击 + 添加");
            tvCurrent.setVisibility(allRecords.isEmpty() ? View.GONE : View.VISIBLE);
        }

        TextView tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setVisibility(allRecords.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(allRecords.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
