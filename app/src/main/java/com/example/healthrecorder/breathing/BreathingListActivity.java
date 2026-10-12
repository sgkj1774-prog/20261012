package com.example.healthrecorder.breathing;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.DataManagementActivity;
import com.example.healthrecorder.R;
import com.example.healthrecorder.breathing.BreathingStatsActivity;

import java.util.ArrayList;
import java.util.List;
import com.example.healthrecorder.DatabaseHelper;

public class BreathingListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private BreathingAdapter adapter;
    private List<BreathingRecord> recordList = new ArrayList<>();
    private HealthRepository repository;
    private Context ctx;

    private final ActivityResultLauncher<Intent> addLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK) loadData();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing_list);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("呼吸打卡");
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.getMenu().add(0, 1, 0, "📊 统计");
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                startActivity(new Intent(ctx, BreathingStatsActivity.class));
                return true;
            }
            return false;
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(ctx));
        adapter = new BreathingAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(r -> {
            Intent intent = new Intent(ctx, BreathingAddActivity.class);
            intent.putExtra("edit_id", r.getId());
            addLauncher.launch(intent);
        });

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                addLauncher.launch(new Intent(ctx, BreathingAddActivity.class)));

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        recordList.clear();
        // getAllBreathingRecords 已按日期倒序返回
        recordList.addAll(repository.getAllBreathingRecords());
        adapter.setList(recordList);

        TextView tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setVisibility(recordList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(recordList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
