package com.example.healthrecorder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
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

    private Spinner spinnerFilterUser;
    private List<Integer> filterUserIds = new ArrayList<>();
    private int filterUserId = -1;  // -1 = 全部成员

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

        setupUserFilter();
        loadData();
    }

    /** 顶部成员筛选 */
    private void setupUserFilter() {
        spinnerFilterUser = findViewById(R.id.spinnerFilterUser);
        List<String> names = new ArrayList<>();
        filterUserIds.clear();
        names.add("全部成员");
        filterUserIds.add(-1);
        for (User u : repository.getAllUsers()) {
            names.add(u.getName());
            filterUserIds.add(u.getId());
        }
        ArrayAdapter<String> adapterU = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapterU.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterUser.setAdapter(adapterU);
        spinnerFilterUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int newId = filterUserIds.get(position);
                if (newId != filterUserId) {
                    filterUserId = newId;
                    loadData();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        recordList.clear();
        for (Record r : repository.getAllRecords()) {
            if (filterUserId == -1 || r.getUserId() == filterUserId) {
                recordList.add(r);
            }
        }
        adapter.notifyDataSetChanged();

        TextView tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setVisibility(recordList.isEmpty() ? View.VISIBLE : View.GONE);
        listView.setVisibility(recordList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
