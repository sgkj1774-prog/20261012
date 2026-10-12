package com.example.healthrecorder.exercise;

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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.example.healthrecorder.DatabaseHelper;

public class ExerciseRecordListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ExerciseRecordAdapter adapter;
    private List<ExerciseRecord> recordList = new ArrayList<>();
    private HealthRepository repository;
    private Context ctx;

    private final ActivityResultLauncher<Intent> addLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK) loadData();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_record_list);
        ctx = this;
        repository = HealthRepository.getInstance(getApplicationContext());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(ctx));
        adapter = new ExerciseRecordAdapter();
        recyclerView.setAdapter(adapter);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("🏃 运动记录");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAdd).setOnClickListener(v -> addLauncher.launch(new Intent(ctx, ExerciseRecordActivity.class)));
        findViewById(R.id.btnChart).setVisibility(View.VISIBLE);
        findViewById(R.id.btnChart).setOnClickListener(v -> {
            startActivity(new Intent(ctx, ExerciseChartActivity.class));
        });

        adapter.setOnItemClickListener(r -> {
            Intent intent = new Intent(ctx, ExerciseRecordActivity.class);
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
                "SELECT e.*, u." + com.example.healthrecorder.DatabaseHelper.COLUMN_USER_NAME + " AS un "
                + "FROM exercise_records e LEFT JOIN users u ON e.user_id=u." + com.example.healthrecorder.DatabaseHelper.COLUMN_ID
                + " ORDER BY e.date DESC", null);
        if (c.moveToFirst()) {
            do {
                ExerciseRecord r = new ExerciseRecord();
                r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                r.setDate(new Date(c.getLong(c.getColumnIndexOrThrow("date"))));
                r.setType(c.getInt(c.getColumnIndexOrThrow("type")));
                r.setDistance(c.getFloat(c.getColumnIndexOrThrow("distance")));
                r.setDuration(c.getInt(c.getColumnIndexOrThrow("duration")));
                r.setPace(c.getFloat(c.getColumnIndexOrThrow("pace")));
                r.setHeartRate(c.getInt(c.getColumnIndexOrThrow("heart_rate")));
                r.setCalories(c.getInt(c.getColumnIndexOrThrow("calories")));
                r.setElevationGain(c.getInt(c.getColumnIndexOrThrow("elevation_gain")));
                r.setDifficulty(c.getInt(c.getColumnIndexOrThrow("difficulty")));
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
