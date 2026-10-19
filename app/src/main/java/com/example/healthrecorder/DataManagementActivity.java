package com.example.healthrecorder;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import com.example.healthrecorder.UserManagementActivity;

/**
 * 数据管理（父菜单）：包含「导入数据」与「导出数据」两个子功能。
 *  - 导出：将全部数据（成员配置+健康记录）备份到用户选择的目录中。
 *  - 导入：从 CSV / XLSX 备份文件还原全部数据（先还原成员，再还原记录，按姓名匹配成员归属，重复记录自动跳过）。
 */
public class DataManagementActivity extends AppCompatActivity {

    private Button btnExportCsv, btnExportExcel, btnImportCsv, btnImportExcel;
    private ProgressBar progressBarExport, progressBarImport;
    private TextView textStatusExport, textStatusImport, textViewFolderPath;
    private HealthRepository repository;
    private ExecutorService executor;
    private DocumentFile selectedFolderDoc = null;
    private static String sPendingImportMime = null;

    // 选择保存目录
    private final ActivityResultLauncher<Intent> folderPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, flags);
                        selectedFolderDoc = DocumentFile.fromTreeUri(this, uri);

                        String path = uri.getLastPathSegment();
                        if (path != null) path = path.replace("primary:", "手机存储/");
                        textViewFolderPath.setText(path != null ? path : "已选择目录");

                        int total = repository.getRecordCount();
                        textStatusExport.setText("已就绪，可导出全部数据\n数据库共 " + total + " 条记录");
                        Toast.makeText(this, "已选择保存位置", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // 选择导入文件
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        String mime = sPendingImportMime;
                        sPendingImportMime = null;
                        performImport(fileUri, mime);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_management);

        btnExportCsv = findViewById(R.id.btnExportCsv);
        btnExportExcel = findViewById(R.id.btnExportExcel);
        btnImportCsv = findViewById(R.id.btnImportCsv);
        btnImportExcel = findViewById(R.id.btnImportExcel);
        progressBarExport = findViewById(R.id.progressBarExport);
        progressBarImport = findViewById(R.id.progressBarImport);
        textStatusExport = findViewById(R.id.textStatusExport);
        textStatusImport = findViewById(R.id.textStatusImport);
        textViewFolderPath = findViewById(R.id.textViewFolderPath);
        findViewById(R.id.btnVersionInfo).setOnClickListener(v ->
                startActivity(new Intent(this, VersionHistoryActivity.class)));
        findViewById(R.id.btnResetData).setOnClickListener(v -> showResetConfirm());
        findViewById(R.id.btnMemberManagement).setOnClickListener(v ->
                startActivity(new Intent(this, UserManagementActivity.class)));

        repository = HealthRepository.getInstance(getApplicationContext());

        AtomicInteger execCount = new AtomicInteger(0);
        executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "DataWorker-" + execCount.incrementAndGet());
                t.setUncaughtExceptionHandler((th, ex) -> runOnUiThread(() -> {
                    progressBarExport.setVisibility(View.GONE);
                    progressBarImport.setVisibility(View.GONE);
                    textStatusExport.setText("线程异常: " + ex.getClass().getSimpleName());
                    Toast.makeText(DataManagementActivity.this, "操作失败: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }));
                return t;
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSelectFolder).setOnClickListener(v -> selectFolder());
        btnExportCsv.setOnClickListener(v -> performExport("csv"));
        btnExportExcel.setOnClickListener(v -> performExport("xlsx"));
        btnImportCsv.setOnClickListener(v -> launchFilePicker("text/csv"));
        btnImportExcel.setOnClickListener(v -> launchFilePicker("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        int total = repository.getRecordCount();
        textStatusExport.setText("数据库共 " + total + " 条记录（将导出全部成员）");
    }

    private void selectFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        folderPickerLauncher.launch(intent);
    }

    private void launchFilePicker(String mimeType) {
        sPendingImportMime = mimeType;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        filePickerLauncher.launch(intent);
    }

    // ============================ 导出 ============================
    private void performExport(String format) {
        if (selectedFolderDoc == null) {
            textStatusExport.setText("请先选择保存位置");
            Toast.makeText(this, "请先选择保存位置", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!selectedFolderDoc.canWrite()) {
            textStatusExport.setText("目录无写入权限，请重新选择");
            Toast.makeText(this, "目录无写入权限，请重新选择", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarExport.setVisibility(View.VISIBLE);
        setExportButtonsEnabled(false);
        textStatusExport.setText("正在导出 " + format.toUpperCase() + " ...");

        executor.execute(() -> {
            try {
                List<User> users = repository.getAllUsers();
                List<Record> records = repository.getAllRecords();
                List<com.example.healthrecorder.cat.CatRecord> catRecords = repository.getAllCatRecords();
                List<com.example.healthrecorder.teeth.TeethRecord> teethRecords = repository.getAllTeethRecords();
                List<com.example.healthrecorder.exercise.ExerciseRecord> exerciseRecords = repository.getAllExerciseRecords();
                List<com.example.healthrecorder.breathing.BreathingRecord> breathingRecords = repository.getAllBreathingRecords();
                List<com.example.healthrecorder.event.Event> events = repository.getAllEvents();
                List<com.example.healthrecorder.book.BookRecord> bookRecords = repository.getAllBookRecords();
                List<com.example.healthrecorder.cat.CatFoodRecord> catFoodRecords = repository.getAllCatFoodRecords();
                List<com.example.healthrecorder.book.BookNote> bookNotes = repository.getAllBookNotes();

                if (records.isEmpty() && users.isEmpty() && catRecords.isEmpty() && teethRecords.isEmpty() && exerciseRecords.isEmpty() && breathingRecords.isEmpty() && events.isEmpty() && bookRecords.isEmpty() && catFoodRecords.isEmpty()) {
                    postOnUi(() -> {
                        progressBarExport.setVisibility(View.GONE);
                        setExportButtonsEnabled(true);
                        textStatusExport.setText("数据库为空，无内容可导出");
                    });
                    return;
                }

                String timeStr = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new java.util.Date());
                String fileName = "健康小助手备份_" + timeStr + (format.equals("csv") ? ".csv" : ".xlsx");
                String mimeType = format.equals("csv") ? "text/csv" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

                DocumentFile targetFile = selectedFolderDoc.createFile(mimeType, fileName);
                if (targetFile == null) throw new Exception("创建文件失败");

                byte[] data = format.equals("csv")
                        ? ExportHelper.buildCsvBytes(records, users, catRecords, teethRecords, exerciseRecords, breathingRecords, events, bookRecords, catFoodRecords, bookNotes)
                        : ExportHelper.buildExcelBytes(records, users, catRecords, teethRecords, exerciseRecords, breathingRecords, events, bookRecords, catFoodRecords, bookNotes);
                if (data == null || data.length == 0) throw new Exception("内容生成失败（空数据）");

                Uri fileUri = targetFile.getUri();
                try (OutputStream os = getContentResolver().openOutputStream(fileUri)) {
                    if (os == null) throw new Exception("无法打开输出流");
                    os.write(data);
                    os.flush();
                }

                DocumentFile verify = DocumentFile.fromSingleUri(this, fileUri);
                long verifySize = (verify != null) ? verify.length() : -1;
                if (verifySize <= 0) throw new Exception("文件写入后为空");

                final long size = verifySize;
                final int rc = records.size(), uc = users.size();
                final int cc = catRecords.size(), tc = teethRecords.size(), ec = exerciseRecords.size();
                final int bc = breathingRecords.size(), evc = events.size(), bkc = bookRecords.size(), cfc = catFoodRecords.size();
                postOnUi(() -> {
                    progressBarExport.setVisibility(View.GONE);
                    setExportButtonsEnabled(true);
                    textStatusExport.setText(String.format(Locale.getDefault(),
                            "✓ 导出成功\n健康 %d | 成员 %d | 猫咪 %d | 刷牙 %d | 运动 %d\n呼吸 %d | 事项 %d | 读书 %d | 猫粮 %d\n文件大小 %s\n%s",
                            rc, uc, cc, tc, ec, bc, evc, bkc, cfc, formatSize(size), fileName));
                    Toast.makeText(this, "已导出 " + rc + " 条记录", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                postOnUi(() -> {
                    progressBarExport.setVisibility(View.GONE);
                    setExportButtonsEnabled(true);
                    textStatusExport.setText("✗ 导出失败\n" + e.getClass().getSimpleName() + "\n" + e.getMessage());
                    Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setExportButtonsEnabled(boolean enabled) {
        btnExportCsv.setEnabled(enabled);
        btnExportExcel.setEnabled(enabled);
        findViewById(R.id.btnSelectFolder).setEnabled(enabled);
    }

    // ============================ 导入 ============================
    private void performImport(Uri fileUri, String mime) {
        String typeLabel = (mime != null && mime.contains("spreadsheet")) ? "Excel" : "CSV";
        progressBarImport.setVisibility(View.VISIBLE);
        textStatusImport.setText("正在解析 " + typeLabel + " 文件...");
        setImportButtonsEnabled(false);

        executor.execute(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(fileUri);
                if (is == null) throw new Exception("无法打开文件");
                String fileName = getFileName(fileUri);
                ImportHelper.ImportResult result = ImportHelper.parseFile(is, fileName);
                is.close();

                if (result.records.isEmpty() && result.users.isEmpty() && result.catRecords.isEmpty() && result.teethRecords.isEmpty() && result.exerciseRecords.isEmpty() && result.breathingRecords.isEmpty() && result.events.isEmpty() && result.bookRecords.isEmpty() && result.catFoodRecords.isEmpty()) {
                    postOnUi(() -> {
                        progressBarImport.setVisibility(View.GONE);
                        setImportButtonsEnabled(true);
                        textStatusImport.setText(String.format(Locale.getDefault(),
                                "⚠️ 未解析到有效数据\n（共 %d 行，跳过 %d 行）", result.totalLines, result.skippedLines));
                    });
                    return;
                }

                int rc = result.records.size();
                int uc = result.users.size();
                int cc = result.catRecords.size();
                int tc = result.teethRecords.size();
                int ec = result.exerciseRecords.size();
                int bc = result.breathingRecords.size();
                int evc = result.events.size();
                int bkc = result.bookRecords.size();
                int cfc = result.catFoodRecords.size();
                int sk = result.skippedLines;
                postOnUi(() -> {
                    progressBarImport.setVisibility(View.GONE);
                    setImportButtonsEnabled(true);
                    String msg = String.format(Locale.getDefault(),
                            "解析完成！\n\n健康 %d | 成员 %d | 猫咪 %d | 刷牙 %d | 运动 %d\n呼吸 %d | 事项 %d | 读书 %d | 猫粮 %d\n跳过 %d 行无效数据\n\n导入方式：合并（按姓名匹配成员，重复记录自动跳过）",
                            rc, uc, cc, tc, ec, bc, evc, bkc, cfc, sk);
                    new AlertDialog.Builder(DataManagementActivity.this)
                            .setTitle("📥 确认导入")
                            .setMessage(msg)
                            .setPositiveButton("确认导入", (d, w) -> doInsertData(result))
                            .setNegativeButton("取消", (d, w) -> textStatusImport.setText("已取消"))
                            .show();
                });
            } catch (Exception e) {
                postOnUi(() -> {
                    progressBarImport.setVisibility(View.GONE);
                    setImportButtonsEnabled(true);
                    textStatusImport.setText("✗ 解析失败\n" + e.getClass().getSimpleName() + "\n" + e.getMessage());
                    Toast.makeText(this, "解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void doInsertData(ImportHelper.ImportResult result) {
        progressBarImport.setVisibility(View.VISIBLE);
        textStatusImport.setText("正在写入数据...");
        setImportButtonsEnabled(false);

        executor.execute(() -> {
            try {
                // 成员名称 -> 数据库ID 映射
                Map<String, Integer> nameToId = new HashMap<>();
                for (User existing : repository.getAllUsers()) {
                    nameToId.put(existing.getName(), existing.getId());
                }

                // 1) 还原成员
                int usersAdded = 0;
                for (User u : result.users) {
                    User existing = repository.getUserByName(u.getName());
                    if (existing != null) {
                        nameToId.put(u.getName(), existing.getId());
                    } else {
                        User nu = new User();
                        nu.setName(u.getName());
                        nu.setColor(u.getColor());
                        long id = repository.addUser(nu);
                        nameToId.put(u.getName(), (int) id);
                        usersAdded++;
                    }
                }

                // 2) 还原健康记录
                int inserted = 0, skipped = 0, dup = 0;
                for (Record r : result.records) {
                    if (!r.hasWeight() && !r.hasBloodPressure()) { skipped++; continue; }
                    Integer uid = nameToId.get(r.getUserName());
                    r.setUserId(uid != null ? uid : 1);
                    if (repository.recordExists(r)) { dup++; continue; }
                    repository.addRecord(r);
                    inserted++;
                }

                // 3) 还原猫咪记录
                int catInserted = 0;
                for (com.example.healthrecorder.cat.CatRecord c : result.catRecords) {
                    Integer uid = nameToId.get(c.getCatName());
                    c.setUserId(uid != null ? uid : 1);
                    repository.addCatRecord(c);
                    catInserted++;
                }

                // 4) 还原刷牙记录
                int teethInserted = 0;
                for (com.example.healthrecorder.teeth.TeethRecord t : result.teethRecords) {
                    Integer uid = nameToId.get(t.getUserName());
                    t.setUserId(uid != null ? uid : 1);
                    repository.addTeethRecord(t);
                    teethInserted++;
                }

                // 5) 还原运动记录
                int exerciseInserted = 0;
                for (com.example.healthrecorder.exercise.ExerciseRecord e : result.exerciseRecords) {
                    Integer uid = nameToId.get(e.getUserName());
                    e.setUserId(uid != null ? uid : 1);
                    repository.addExerciseRecord(e);
                    exerciseInserted++;
                }

                // 6) 还原呼吸打卡记录
                int breathingInserted = 0;
                for (com.example.healthrecorder.breathing.BreathingRecord b : result.breathingRecords) {
                    Integer uid = nameToId.get(b.getUserName());
                    b.setUserId(uid != null ? uid : 1);
                    repository.addBreathingRecord(b);
                    breathingInserted++;
                }

                // 7) 还原重要事项记录
                int eventInserted = 0;
                for (com.example.healthrecorder.event.Event ev : result.events) {
                    Integer uid = nameToId.get(ev.getUserName());
                    ev.setUserId(uid != null ? uid : 1);
                    repository.addEvent(ev);
                    eventInserted++;
                }

                // 8) 还原读书笔记记录
                int bookInserted = 0;
                int bookNotesInserted = 0;
                for (com.example.healthrecorder.book.BookRecord bk : result.bookRecords) {
                    Integer uid = nameToId.get(bk.getUserName());
                    bk.setUserId(uid != null ? uid : 1);
                    long newBookId = repository.addBookRecord(bk);
                    bookInserted++;
                    // 该书的分段读后感需要真实 id，落库时一并写入
                    if (newBookId > 0 && bk.getPendingNotes() != null) {
                        for (com.example.healthrecorder.book.BookNote n : bk.getPendingNotes()) {
                            n.setBookId((int) newBookId);
                            n.setUserId(bk.getUserId());
                            repository.addBookNote(n);
                            bookNotesInserted++;
                        }
                    }
                }

                // 9) 还原猫粮记录
                int catFoodInserted = 0;
                for (com.example.healthrecorder.cat.CatFoodRecord cf : result.catFoodRecords) {
                    Integer uid = nameToId.get(cf.getUserName());
                    cf.setUserId(uid != null ? uid : 1);
                    repository.insertCatFoodRecord(cf);
                    catFoodInserted++;
                }

                final int fInserted = inserted, fSkipped = skipped, fDup = dup, fUsers = usersAdded;
                final int fCat = catInserted, fTeeth = teethInserted, fEx = exerciseInserted;
                final int fBreath = breathingInserted, fEvent = eventInserted, fBook = bookInserted, fCatFood = catFoodInserted;
                final int fBookNotes = bookNotesInserted;
                postOnUi(() -> {
                    progressBarImport.setVisibility(View.GONE);
                    setImportButtonsEnabled(true);
                    textStatusImport.setText(String.format(Locale.getDefault(),
                            "✓ 导入完成！\n新增成员 %d 个\n健康 %d | 猫咪 %d | 刷牙 %d | 运动 %d\n呼吸 %d | 事项 %d | 读书 %d（段落读后感 %d） | 猫粮 %d\n跳过空记录：%d 条 | 重复跳过 %d 条",
                            fUsers, fInserted, fCat, fTeeth, fEx, fBreath, fEvent, fBook, fBookNotes, fCatFood, fSkipped, fDup));
                    Toast.makeText(this, "已导入 " + fInserted + " 条记录", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                postOnUi(() -> {
                    progressBarImport.setVisibility(View.GONE);
                    setImportButtonsEnabled(true);
                    textStatusImport.setText("✗ 写入失败\n" + e.getClass().getSimpleName() + "\n" + e.getMessage());
                    Toast.makeText(this, "写入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setImportButtonsEnabled(boolean enabled) {
        btnImportCsv.setEnabled(enabled);
        btnImportExcel.setEnabled(enabled);
    }

    private String getFileName(Uri uri) {
        String name = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
            if (idx >= 0) name = cursor.getString(idx);
            cursor.close();
        }
        return name != null ? name : (uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "");
    }

    private void showResetConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ 确认数据重置")
                .setMessage("此操作将清空所有数据（健康记录/猫咪/刷牙/运动/呼吸打卡/重要事项/读书笔记/猫粮/成员），操作不可恢复！\n\n建议先执行「导出」备份。")
                .setPositiveButton("确认重置", (d, w) -> performReset())
                .setNegativeButton("取消", null)
                .show();
    }

    private void performReset() {
        new AlertDialog.Builder(this)
                .setTitle("最后确认")
                .setMessage("再次确认：所有数据将被永久删除，确定继续？")
                .setPositiveButton("立即删除", (d, w) -> {
                    repository.resetAllData();
                    Toast.makeText(this, "数据已全部清除", Toast.LENGTH_LONG).show();
                    textStatusExport.setText("数据已重置，共 0 条记录");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void postOnUi(Runnable r) {
        runOnUiThread(r);
    }

    private String formatSize(long bytes) {
        if (bytes < 0) return "未知";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
