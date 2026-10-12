package com.example.healthrecorder.book;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;

import java.util.Date;
import com.example.healthrecorder.DatabaseHelper;

public class BookAddActivity extends AppCompatActivity {
    private HealthRepository repository;
    private EditText editTitle, editAuthor, editTotalPages, editCurrentPage, editNotes;
    private TextView tvProgress, tvTitle;
    private View layoutStars;
    private TextView[] starViews;
    private int selectedRating = 0;
    private int selectedStatus = 0;
    private int editId = -1;
    private BookRecord editingRecord = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_add);
        repository = HealthRepository.getInstance(getApplicationContext());

        editTitle = findViewById(R.id.editTitle);
        editAuthor = findViewById(R.id.editAuthor);
        editTotalPages = findViewById(R.id.editTotalPages);
        editCurrentPage = findViewById(R.id.editCurrentPage);
        editNotes = findViewById(R.id.editNotes);
        tvProgress = findViewById(R.id.tvProgress);
        tvTitle = findViewById(R.id.tvTitle);
        layoutStars = findViewById(R.id.layoutStars);

        starViews = new TextView[]{
                findViewById(R.id.star1),
                findViewById(R.id.star2),
                findViewById(R.id.star3),
                findViewById(R.id.star4),
                findViewById(R.id.star5)
        };

        // 星星点击
        for (int i = 0; i < starViews.length; i++) {
            final int rating = i + 1;
            starViews[i].setOnClickListener(v -> {
                selectedRating = rating;
                updateStars();
            });
        }

        // 进度监听
        editTotalPages.setOnFocusChangeListener((v, hasFocus) -> updateProgress());
        editCurrentPage.setOnFocusChangeListener((v, hasFocus) -> updateProgress());
        editTotalPages.setOnClickListener(v -> updateProgress());
        editCurrentPage.setOnClickListener(v -> updateProgress());

        // 状态Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.book_status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        android.widget.Spinner spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerStatus.setAdapter(adapter);
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // 跳过首次自动触发（Android Spinner 初始化时会自动调用一次）
                if (editingRecord == null && selectedStatus == 0 && pos == 0) {
                    return;
                }
                selectedStatus = pos;
                if (pos == 2) { // 已读
                    editTotalPages.setText(String.valueOf(editingRecord != null ? editingRecord.getTotalPages() : ""));
                    editCurrentPage.setText(String.valueOf(editingRecord != null ? editingRecord.getTotalPages() : ""));
                    updateProgress();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 返回
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 保存
        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        // 加载编辑数据
        editId = getIntent().getIntExtra("edit_id", -1);
        if (editId != -1) {
            tvTitle.setText("📖 编辑读书记录");
            loadEditData();
        } else {
            tvTitle.setText("📖 添加读书记录");
        }
    }

    private void loadEditData() {
        for (BookRecord r : repository.getAllBookRecords()) {
            if (r.getId() == editId) {
                editingRecord = r;
                break;
            }
        }
        if (editingRecord == null) {
            Toast.makeText(this, "记录不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        editTitle.setText(editingRecord.getTitle());
        editAuthor.setText(editingRecord.getAuthor());
        if (editingRecord.getTotalPages() > 0) {
            editTotalPages.setText(String.valueOf(editingRecord.getTotalPages()));
            editCurrentPage.setText(String.valueOf(editingRecord.getCurrentPage()));
        }
        editNotes.setText(editingRecord.getNotes());
        selectedStatus = editingRecord.getStatus();
        selectedRating = editingRecord.getRating();
        updateStars();
        updateProgress();
        // 在 Spinner 设置完初始值后同步 UI
        android.widget.Spinner spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerStatus.setSelection(selectedStatus, false);
    }

    private void updateStars() {
        for (int i = 0; i < starViews.length; i++) {
            starViews[i].setText(i < selectedRating ? "★" : "☆");
            starViews[i].setTextColor(getColor(i < selectedRating ? R.color.star_active : R.color.star_inactive));
        }
    }

    private void updateProgress() {
        try {
            int total = Integer.parseInt(editTotalPages.getText().toString().trim());
            int current = Integer.parseInt(editCurrentPage.getText().toString().trim());
            if (total > 0) {
                int pct = (int) ((float) current / total * 100);
                tvProgress.setText("阅读进度：" + pct + "%");
                tvProgress.setVisibility(View.VISIBLE);
            } else {
                tvProgress.setVisibility(View.GONE);
            }
        } catch (NumberFormatException e) {
            tvProgress.setVisibility(View.GONE);
        }
    }

    private void save() {
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            editTitle.setError("请输入书名");
            return;
        }

        String author = editAuthor.getText().toString().trim();
        int totalPages = 0;
        int currentPage = 0;
        try {
            if (!editTotalPages.getText().toString().trim().isEmpty())
                totalPages = Integer.parseInt(editTotalPages.getText().toString().trim());
            if (!editCurrentPage.getText().toString().trim().isEmpty())
                currentPage = Integer.parseInt(editCurrentPage.getText().toString().trim());
        } catch (NumberFormatException ignored) {}

        String notes = editNotes.getText().toString().trim();

        BookRecord record = new BookRecord();
        if (editId != -1) record.setId(editId);
        record.setTitle(title);
        record.setAuthor(author);
        record.setTotalPages(totalPages);
        record.setCurrentPage(currentPage);
        record.setStatus(selectedStatus);
        record.setRating(selectedRating);
        record.setNotes(notes);
        record.setUserId(1);
        record.setUserName("本人");

        if (editId == -1) {
            record.setCreatedAt(new Date());
        } else {
            record.setCreatedAt(editingRecord.getCreatedAt());
        }

        // 已读时：固化归档字段（读后感和页数绑定到归档版本）
        if (selectedStatus == 2) {
            record.setFinishedAt(new Date());
            record.setFinishedPage(currentPage);
            record.setFinishedNotes(notes.isEmpty() ? null : notes);
        } else {
            record.setFinishedAt(editingRecord != null ? editingRecord.getFinishedAt() : null);
            record.setFinishedPage(editingRecord != null ? editingRecord.getFinishedPage() : 0);
            record.setFinishedNotes(editingRecord != null ? editingRecord.getFinishedNotes() : null);
        }
        if (editId == -1) {
            repository.addBookRecord(record);
            Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
        } else {
            repository.updateBookRecord(record);
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK);
        finish();
    }
}
