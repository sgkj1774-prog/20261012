package com.example.healthrecorder.teeth;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthrecorder.R;
import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.DatabaseHelper;
import com.example.healthrecorder.teeth.TeethRecord;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TeethRecordListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TeethRecordAdapter.Adapter adapter;
    private List<TeethRecord> recordList = new ArrayList<>();
    private HealthRepository repository;
    private Context ctx;

    private final ActivityResultLauncher<Intent> addLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK) loadData();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teeth_record_list);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(ctx));
        adapter = new TeethRecordAdapter.Adapter();
        recyclerView.setAdapter(adapter);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("🦷 刷牙记录");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAdd).setOnClickListener(v -> addLauncher.launch(new Intent(ctx, TeethRecordActivity.class)));
        findViewById(R.id.btnChart).setVisibility(View.VISIBLE);
        findViewById(R.id.btnChart).setOnClickListener(v -> {
            startActivity(new Intent(ctx, TeethChartActivity.class));
        });

        adapter.setOnItemClickListener(r -> {
            Intent intent = new Intent(ctx, TeethRecordActivity.class);
            intent.putExtra("edit_id", r.getId());
            addLauncher.launch(intent);
        });

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
                "SELECT t.*, u." + com.example.healthrecorder.DatabaseHelper.COLUMN_USER_NAME + " AS un "
                + "FROM teeth_records t LEFT JOIN users u ON t.user_id=u." + com.example.healthrecorder.DatabaseHelper.COLUMN_ID
                + " ORDER BY t.date DESC", null);
        if (c.moveToFirst()) {
            do {
                TeethRecord r = new TeethRecord();
                r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                r.setDate(new Date(c.getLong(c.getColumnIndexOrThrow("date"))));
                r.setPeriod(c.getInt(c.getColumnIndexOrThrow("period")));
                r.setDuration(c.getInt(c.getColumnIndexOrThrow("duration")));
                r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
                r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
                int unIdx = c.getColumnIndex("un");
                if (unIdx >= 0) r.setUserName(c.getString(unIdx));
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
