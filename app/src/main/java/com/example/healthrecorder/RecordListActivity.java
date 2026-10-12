package com.example.healthrecorder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class RecordListActivity extends AppCompatActivity {
    private ListView listView;
    private RecordAdapter adapter;
    private List<Record> recordList = new ArrayList<>();
    private HealthRepository repository;
    private Context ctx;

    private final ActivityResultLauncher<Intent> addLauncher =
    registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
        if (r.getResultCode() == RESULT_OK) loadData();
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_list);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());

        listView = findViewById(R.id.listView);
        adapter = new RecordAdapter(this, recordList);
        listView.setAdapter(adapter);
        adapter.setOnItemClickListener(r -> {
            Intent intent = new Intent(ctx, AddRecordActivity.class);
            intent.putExtra("edit_id", r.getId());
            addLauncher.launch(intent);
        });

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("💪 健康记录");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            Intent intent = new Intent(ctx, AddRecordActivity.class);
            addLauncher.launch(intent);
        });
        findViewById(R.id.btnChart).setVisibility(View.VISIBLE);
        findViewById(R.id.btnChart).setOnClickListener(v ->
            startActivity(new Intent(ctx, ChartActivity.class)));

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        recordList.clear();
        recordList.addAll(repository.getAllRecords());
        adapter.notifyDataSetChanged();

        TextView tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setVisibility(recordList.isEmpty() ? View.VISIBLE : View.GONE);
        listView.setVisibility(recordList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
