package com.example.healthrecorder.event;

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
import com.example.healthrecorder.UserManagementActivity;
import com.example.healthrecorder.event.EventStatsActivity;

import java.util.ArrayList;
import java.util.List;
import com.example.healthrecorder.DatabaseHelper;

public class EventListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> allEvents = new ArrayList<>();
    private HealthRepository repository;
    private Context ctx;

    private final ActivityResultLauncher<Intent> addLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK) loadData();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(ctx));
        adapter = new EventAdapter();
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAdd).setOnClickListener(v ->
                addLauncher.launch(new Intent(ctx, EventAddActivity.class)));

        // 成员管理入口
        findViewById(R.id.btnMembers).setOnClickListener(v ->
                startActivity(new Intent(ctx, UserManagementActivity.class)));
        // 数据统计入口（原数据管理入口已移除）
        findViewById(R.id.btnDataManagement).setOnClickListener(v ->
                startActivity(new Intent(ctx, EventStatsActivity.class)));

        adapter.setOnItemClickListener(ev -> {
            Intent intent = new Intent(ctx, EventAddActivity.class);
            intent.putExtra("edit_id", ev.getId());
            addLauncher.launch(intent);
        });

        adapter.setOnItemLongClickListener(ev -> {
            new AlertDialog.Builder(ctx)
                    .setTitle("删除重要事项")
                    .setMessage("确定要删除「" + ev.getTitle() + "」吗？")
                    .setPositiveButton("删除", (d, w) -> {
                        repository.deleteEvent(ev.getId());
                        loadData();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        allEvents.clear();
        List<Event> raw = repository.getAllEvents();
        if (raw != null) allEvents.addAll(raw);
        adapter.setList(allEvents);

        TextView tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setVisibility(allEvents.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(allEvents.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
