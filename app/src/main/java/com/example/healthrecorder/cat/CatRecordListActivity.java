package com.example.healthrecorder.cat;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import com.example.healthrecorder.R;
import com.example.healthrecorder.HealthRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.example.healthrecorder.DatabaseHelper;

public class CatRecordListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CatRecordAdapter adapter;
    private List<CatRecord> recordList = new ArrayList<>();
    private HealthRepository repository;
    private Context ctx;
    private String title;

    private final ActivityResultLauncher<Intent> addLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK) loadData();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cat_record_list);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());
        title = getIntent().getStringExtra("title");
        if (title == null) title = "🐱 猫咪如厕记录";

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(ctx));
        adapter = new CatRecordAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(r -> {
            Intent intent = new Intent(ctx, CatRecordActivity.class);
            intent.putExtra("edit_id", r.getId());
            addLauncher.launch(intent);
        });

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(title);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAdd).setOnClickListener(v -> addLauncher.launch(new Intent(ctx, CatRecordActivity.class)));
        findViewById(R.id.btnChart).setVisibility(View.VISIBLE);
        findViewById(R.id.btnChart).setOnClickListener(v -> startActivity(new Intent(ctx, CatStatsActivity.class)));

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        recordList.clear();
        SQLiteDatabase db = repository.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT c.*, u." + com.example.healthrecorder.DatabaseHelper.COLUMN_USER_NAME + " AS un, u." + com.example.healthrecorder.DatabaseHelper.COLUMN_USER_COLOR + " AS uc "
                + "FROM cat_records c LEFT JOIN users u ON c.user_id=u." + com.example.healthrecorder.DatabaseHelper.COLUMN_ID
                + " ORDER BY c.date DESC", null);
        if (c.moveToFirst()) {
            do {
                CatRecord r = new CatRecord();
                r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                r.setDate(new Date(c.getLong(c.getColumnIndexOrThrow("date"))));
                r.setCatName(c.getString(c.getColumnIndexOrThrow("cat_name")));
                r.setStoolType(c.getInt(c.getColumnIndexOrThrow("stool_type")));
                int scIdx = c.getColumnIndex("stool_count");
                r.setStoolCount(scIdx >= 0 ? c.getInt(scIdx) : 8);
                r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
                r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
                int unIdx = c.getColumnIndex("un");
                if (unIdx >= 0) r.setUserName(c.getString(unIdx));
                int ucIdx = c.getColumnIndex("uc");
                if (ucIdx >= 0 && !c.isNull(ucIdx)) r.setUserColor(c.getInt(ucIdx));
                recordList.add(r);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        adapter.setList(recordList);

        TextView tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setVisibility(recordList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(recordList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
