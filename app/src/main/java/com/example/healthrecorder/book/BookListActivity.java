package com.example.healthrecorder.book;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

public class BookListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private BookAdapter adapter;
    private List<BookRecord> allRecords = new ArrayList<>();
    private HealthRepository repository;
    private Context ctx;

    private final ActivityResultLauncher<Intent> addLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK) loadData();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(ctx));
        adapter = new BookAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(r -> {
            Intent intent = new Intent(ctx, BookAddActivity.class);
            intent.putExtra("edit_id", r.getId());
            addLauncher.launch(intent);
        });

        adapter.setOnItemLongClickListener(r -> {
            new AlertDialog.Builder(ctx)
                    .setTitle("删除「" + r.getTitle() + "」")
                    .setMessage("确定要删除这条读书记录吗？")
                    .setPositiveButton("删除", (d, w) -> {
                        repository.deleteBookRecord(r.getId());
                        loadData();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAdd).setOnClickListener(v ->
                addLauncher.launch(new Intent(ctx, BookAddActivity.class)));
        findViewById(R.id.btnStats).setOnClickListener(v ->
                startActivity(new Intent(ctx, BookStatsActivity.class)));

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        allRecords.clear();
        allRecords.addAll(repository.getAllBookRecords());
        adapter.setList(allRecords);

        TextView tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setVisibility(allRecords.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(allRecords.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
