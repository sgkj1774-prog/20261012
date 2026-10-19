package com.example.healthrecorder.book;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    // 开始阅读时间（v12）
    private final Calendar startCal = Calendar.getInstance();
    private final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    // 分段读后感（页面区间）
    private LinearLayout layoutBookNotes;
    private final List<BookNote> bookNotes = new ArrayList<>();
    private final List<Integer> deletedNoteIds = new ArrayList<>();

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

        // 开始阅读时间：默认取当前时间
        layoutBookNotes = findViewById(R.id.layoutBookNotes);
        Button btnStartDate = findViewById(R.id.btnStartDate);
        Button btnStartTime = findViewById(R.id.btnStartTime);
        updateStartButtons(btnStartDate, btnStartTime);
        btnStartDate.setOnClickListener(v -> new DatePickerDialog(this, (dp, y, m, d) -> {
            startCal.set(Calendar.YEAR, y);
            startCal.set(Calendar.MONTH, m);
            startCal.set(Calendar.DAY_OF_MONTH, d);
            updateStartButtons(findViewById(R.id.btnStartDate), findViewById(R.id.btnStartTime));
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH),
                startCal.get(Calendar.DAY_OF_MONTH)).show());
        btnStartTime.setOnClickListener(v -> new TimePickerDialog(this, (tp, h, min) -> {
            startCal.set(Calendar.HOUR_OF_DAY, h);
            startCal.set(Calendar.MINUTE, min);
            updateStartButtons(findViewById(R.id.btnStartDate), findViewById(R.id.btnStartTime));
        }, startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE), true).show());

        // 添加段落读后感
        findViewById(R.id.btnAddBookNote).setOnClickListener(v -> showNoteDialog(null));

        // 保存
        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        // 加载编辑数据
        editId = getIntent().getIntExtra("edit_id", -1);
        if (editId != -1) {
            tvTitle.setText("📖 编辑读书记录");
            loadEditData();
        } else {
            tvTitle.setText("📖 添加读书记录");
            renderBookNotes();
        }
    }

    private void updateStartButtons(Button dateBtn, Button timeBtn) {
        if (dateBtn != null) dateBtn.setText("📅 " + DATE_FMT.format(startCal.getTime()));
        if (timeBtn != null) timeBtn.setText("🕐 " + TIME_FMT.format(startCal.getTime()));
    }

    // ============================================================
    // 分段读后感
    // ============================================================

    private void renderBookNotes() {
        if (layoutBookNotes == null) return;
        layoutBookNotes.removeAllViews();

        if (bookNotes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("还没有分段读后感，点击下方按钮添加");
            empty.setTextSize(13);
            empty.setTextColor(0xFF9E9E9E);
            empty.setPadding(dp(8), dp(12), dp(8), dp(12));
            layoutBookNotes.addView(empty);
            return;
        }

        for (BookNote n : bookNotes) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(8), dp(8), dp(8), dp(8));

            TextView head = new TextView(this);
            head.setText("📄 " + n.getRangeText());
            head.setTextSize(13);
            head.setTextColor(0xFFEF6C00);
            head.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(head);

            TextView body = new TextView(this);
            body.setText(n.getContent() == null ? "" : n.getContent());
            body.setTextSize(14);
            body.setTextColor(0xFF333333);
            body.setPadding(0, dp(4), 0, 0);
            row.addView(body);

            row.setOnClickListener(v -> showNoteDialog(n));
            row.setOnLongClickListener(v -> {
                confirmDeleteNote(n);
                return true;
            });
            layoutBookNotes.addView(row);

            View divider = new View(this);
            divider.setBackgroundColor(0xFFEEEEEE);
            layoutBookNotes.addView(divider, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        }
    }

    /** 新增或编辑一条段落读后感 */
    private void showNoteDialog(BookNote existing) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), dp(8));

        EditText editStart = new EditText(this);
        editStart.setHint("起始页，如 12");
        editStart.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        box.addView(editStart);

        EditText editEnd = new EditText(this);
        editEnd.setHint("结束页，如 30");
        editEnd.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        box.addView(editEnd);

        EditText editContent = new EditText(this);
        editContent.setHint("这一段的想法...");
        editContent.setMinLines(4);
        editContent.setGravity(android.view.Gravity.TOP);
        editContent.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        box.addView(editContent);

        if (existing != null) {
            if (existing.getStartPage() > 0) editStart.setText(String.valueOf(existing.getStartPage()));
            if (existing.getEndPage() > 0) editEnd.setText(String.valueOf(existing.getEndPage()));
            editContent.setText(existing.getContent() == null ? "" : existing.getContent());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "添加段落读后感" : "编辑段落读后感")
                .setView(box)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.show();

        // 自己接管点击，校验不通过时不关闭对话框
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int s = parseIntSafe(editStart.getText().toString());
            int e = parseIntSafe(editEnd.getText().toString());
            String content = editContent.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "请填写读后感内容", Toast.LENGTH_SHORT).show();
                return;
            }
            if (s > 0 && e > 0 && e < s) {
                Toast.makeText(this, "结束页不能小于起始页", Toast.LENGTH_SHORT).show();
                return;
            }
            if (existing == null) {
                BookNote n = new BookNote();
                n.setStartPage(s);
                n.setEndPage(e);
                n.setContent(content);
                n.setCreatedAt(new Date());
                bookNotes.add(n);
            } else {
                existing.setStartPage(s);
                existing.setEndPage(e);
                existing.setContent(content);
            }
            renderBookNotes();
            dialog.dismiss();
        });
    }

    private void confirmDeleteNote(BookNote n) {
        new AlertDialog.Builder(this)
                .setTitle("删除这条读后感？")
                .setMessage(n.getRangeText() + "\n" + (n.getContent() == null ? "" : n.getContent()))
                .setPositiveButton("删除", (d, w) -> {
                    if (n.getId() > 0) deletedNoteIds.add(n.getId());
                    bookNotes.remove(n);
                    renderBookNotes();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 保存时把读后感写入数据库：已删除的删掉，已有的更新，新增的插入 */
    private void persistNotes(int bookId) {
        for (int id : deletedNoteIds) {
            repository.deleteBookNote(id);
        }
        for (BookNote n : bookNotes) {
            n.setBookId(bookId);
            n.setUserId(1);
            if (n.getId() > 0) repository.updateBookNote(n);
            else repository.addBookNote(n);
        }
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
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

        // 开始阅读时间：优先用新字段，老数据回退到创建时间
        if (editingRecord.getStartAt() != null) {
            startCal.setTime(editingRecord.getStartAt());
        } else if (editingRecord.getCreatedAt() != null) {
            startCal.setTime(editingRecord.getCreatedAt());
        }
        updateStartButtons(findViewById(R.id.btnStartDate), findViewById(R.id.btnStartTime));

        // 载入已有的分段读后感
        bookNotes.clear();
        bookNotes.addAll(repository.getBookNotes(editId));
        renderBookNotes();
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
        record.setStartAt(startCal.getTime());
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
        int bookId = editId;
        if (editId == -1) {
            long newId = repository.addBookRecord(record);
            bookId = (int) newId;
            Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
        } else {
            repository.updateBookRecord(record);
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        }
        // 段落读后感依赖书的 id，必须在书保存之后再写
        if (bookId > 0) persistNotes(bookId);
        setResult(RESULT_OK);
        finish();
    }
}
