package com.example.healthrecorder.medication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.DatabaseHelper;
import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 服药记录：药品档案列表 + 每日打卡。
 * 每个 medications 行 = 一个服药周期 / 一批药；截止日期过后自动归档。
 */
public class MedicationListActivity extends AppCompatActivity {

    private ListView listView;
    private TextView tvSummary, tvEmpty, tvArchivedToggle;
    private MedListAdapter adapter;
    private HealthRepository repository;

    private final List<Object> items = new ArrayList<>();
    private final List<Medication> activeMeds = new ArrayList<>();
    private final List<Medication> archivedMeds = new ArrayList<>();
    private final Map<Integer, String> statsMap = new HashMap<>();
    private final Map<Integer, Boolean> todayMap = new HashMap<>();
    private boolean showArchived = false;

    private final ActivityResultLauncher<Intent> addLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK) loadData();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_list);

        repository = HealthRepository.getInstance(getApplicationContext());

        listView = findViewById(R.id.listView);
        tvSummary = findViewById(R.id.tvSummary);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvArchivedToggle = findViewById(R.id.tvArchivedToggle);

        adapter = new MedListAdapter(this);
        listView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            Intent intent = new Intent(this, MedicationAddActivity.class);
            addLauncher.launch(intent);
        });

        tvArchivedToggle.setOnClickListener(v -> {
            showArchived = !showArchived;
            loadData();
        });

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void openEdit(Medication m) {
        Intent intent = new Intent(this, MedicationAddActivity.class);
        intent.putExtra("edit_id", m.getId());
        addLauncher.launch(intent);
    }

    /** 长按弹操作菜单：归档/恢复 / 编辑 / 删除 */
    private void showActions(final Medication m) {
        List<String> opts = new ArrayList<>();
        if (m.isArchivedState()) opts.add("恢复（取消归档）");
        else opts.add("归档");
        opts.add("编辑药品信息");
        opts.add("删除药品");
        new AlertDialog.Builder(this)
                .setTitle(m.getName() + (m.getBatchNo() != null ? "（批号 " + m.getBatchNo() + "）" : ""))
                .setItems(opts.toArray(new String[0]), (d, which) -> {
                    String choice = opts.get(which);
                    if ("归档".equals(choice)) {
                        repository.setMedicationArchived(m.getId(), true);
                        loadData();
                    } else if ("恢复（取消归档）".equals(choice)) {
                        repository.setMedicationArchived(m.getId(), false);
                        if (m.isExpired()) {
                            Toast.makeText(this,
                                    "已取消归档；但该周期截止日期已过，请在编辑页调整截止日期以重新启用每日打卡",
                                    Toast.LENGTH_LONG).show();
                        }
                        loadData();
                    } else if ("编辑药品信息".equals(choice)) {
                        openEdit(m);
                    } else {
                        confirmDelete(m);
                    }
                })
                .show();
    }

    private void confirmDelete(Medication m) {
        new AlertDialog.Builder(this)
                .setTitle("删除药品")
                .setMessage("删除「" + m.getName() + "」会同时清掉它的全部服药打卡记录，确定吗？")
                .setPositiveButton("删除", (d, w) -> {
                    repository.deleteMedication(m.getId());
                    loadData();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadData() {
        List<Medication> all = repository.getAllMedications();
        activeMeds.clear();
        archivedMeds.clear();
        statsMap.clear();
        todayMap.clear();

        if (all != null) {
            for (Medication m : all) {
                if (m.isArchivedState()) archivedMeds.add(m);
                else activeMeds.add(m);
                statsMap.put(m.getId(), buildStats(m));
            }
        }

        String today = DatabaseHelper.DATE_KEY_FORMAT.format(new Date());
        int due = 0, taken = 0;
        for (Medication m : activeMeds) {
            boolean isTaken = false;
            if (m.coversDate(today)) {
                MedicationLog log = repository.getMedicationLog(m.getId(), today);
                isTaken = log != null && log.isTaken();
                due++;
                if (isTaken) taken++;
            }
            todayMap.put(m.getId(), isTaken);
        }

        if (due == 0) {
            tvSummary.setText("今日暂无进行中的服药安排");
        } else if (taken >= due) {
            tvSummary.setText("今日 " + due + " 种药已全部服用 ✓");
        } else {
            tvSummary.setText("今日：应服 " + due + " 种，已服 " + taken + " 种");
        }

        // 组装列表
        items.clear();
        if (!activeMeds.isEmpty()) {
            items.add("进行中（" + activeMeds.size() + "）");
            items.addAll(activeMeds);
        }
        if (!archivedMeds.isEmpty()) {
            items.add("已归档（" + archivedMeds.size() + "）");
            if (showArchived) items.addAll(archivedMeds);
        }

        tvArchivedToggle.setVisibility(archivedMeds.isEmpty() ? View.GONE : View.VISIBLE);
        tvArchivedToggle.setText(showArchived
                ? "已归档（" + archivedMeds.size() + "）▾ 点击收起"
                : "已归档（" + archivedMeds.size() + "）▸ 点击展开");

        adapter.notifyDataSetChanged();
        boolean empty = activeMeds.isEmpty() && archivedMeds.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        listView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /**
     * 近 7 天依从性：统计区间为「起始日（或建档日，或 6 天前）到昨天」。
     */
    private String buildStats(Medication m) {
        Set<String> takenDates = new HashSet<>();
        List<MedicationLog> logs = repository.getMedicationLogsForMed(m.getId(), 8);
        for (MedicationLog l : logs) {
            if (l.isTaken() && l.getLogDate() != null) takenDates.add(l.getLogDate());
        }

        Calendar today0 = Calendar.getInstance();
        today0.set(Calendar.HOUR_OF_DAY, 0);
        today0.set(Calendar.MINUTE, 0);
        today0.set(Calendar.SECOND, 0);
        today0.set(Calendar.MILLISECOND, 0);

        Calendar start = (Calendar) today0.clone();
        start.add(Calendar.DAY_OF_YEAR, -6);

        Calendar medStart = null;
        if (m.getStartDate() != null) medStart = Medication.parseDate(m.getStartDate());
        if (medStart == null && m.getCreatedAt() != null) {
            medStart = Calendar.getInstance();
            medStart.setTime(m.getCreatedAt());
        }
        if (medStart != null) {
            medStart.set(Calendar.HOUR_OF_DAY, 0);
            medStart.set(Calendar.MINUTE, 0);
            medStart.set(Calendar.SECOND, 0);
            medStart.set(Calendar.MILLISECOND, 0);
            if (medStart.after(start)) start = medStart;
        }

        int dueDays = 0, takenDays = 0;
        Calendar cur = (Calendar) start.clone();
        while (cur.before(today0)) {
            String key = DatabaseHelper.DATE_KEY_FORMAT.format(cur.getTime());
            if (m.coversDate(key)) {
                dueDays++;
                if (takenDates.contains(key)) takenDays++;
            }
            cur.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (dueDays <= 0) {
            return m.isArchivedState() ? "已归档" : "刚建档，暂无历史记录";
        }
        return "近 7 天：已服 " + takenDays + " 天 · 漏服 " + (dueDays - takenDays) + " 天";
    }

    private void toggleToday(Medication m) {
        String today = DatabaseHelper.DATE_KEY_FORMAT.format(new Date());
        MedicationLog existing = repository.getMedicationLog(m.getId(), today);
        boolean newTaken = existing == null || !existing.isTaken();

        MedicationLog log = new MedicationLog();
        log.setMedId(m.getId());
        log.setLogDate(today);
        log.setTaken(newTaken);
        log.setTakenAt(newTaken ? new Date() : null);
        log.setUserId(m.getUserId());
        if (existing != null) log.setNotes(existing.getNotes());

        repository.saveMedicationLog(log);
        Toast.makeText(this,
                newTaken ? m.getName() + " 今日已服 ✓" : "已取消 " + m.getName() + " 的今日打卡",
                Toast.LENGTH_SHORT).show();
        loadData();
    }

    // ============================================================
    // 列表适配器：表头 + 药品卡片
    // ============================================================
    private class MedListAdapter extends BaseAdapter {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_MED = 1;
        private final LayoutInflater inflater;

        MedListAdapter(Context context) {
            this.inflater = LayoutInflater.from(context);
        }

        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int p) { return items.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int p) {
            return items.get(p) instanceof String ? TYPE_HEADER : TYPE_MED;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (getItemViewType(position) == TYPE_HEADER) {
                TextView tv = new TextView(MedicationListActivity.this);
                tv.setText((String) items.get(position));
                tv.setPadding(dp(12), dp(16), dp(12), dp(4));
                tv.setTextSize(13f);
                tv.setTextColor(Color.parseColor("#888888"));
                tv.getPaint().setFakeBoldText(true);
                return tv;
            }

            Medication m = (Medication) items.get(position);
            ViewHolder holder;
            if (convertView == null || convertView.getTag() == null
                    || !(convertView.getTag() instanceof ViewHolder)) {
                convertView = inflater.inflate(R.layout.item_medication, parent, false);
                holder = new ViewHolder();
                holder.name = convertView.findViewById(R.id.tvMedName);
                holder.dosage = convertView.findViewById(R.id.tvMedDosage);
                holder.schedule = convertView.findViewById(R.id.tvMedSchedule);
                holder.cycle = convertView.findViewById(R.id.tvMedCycle);
                holder.detail = convertView.findViewById(R.id.tvMedDetail);
                holder.expiry = convertView.findViewById(R.id.tvMedExpiry);
                holder.notes = convertView.findViewById(R.id.tvMedNotes);
                holder.stats = convertView.findViewById(R.id.tvMedStats);
                holder.badge = convertView.findViewById(R.id.tvBadge);
                holder.btnTake = convertView.findViewById(R.id.btnTake);
                holder.btnEdit = convertView.findViewById(R.id.btnEdit);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final Medication med = m;
            holder.name.setText(med.getName());

            if (med.getDosage() != null && !med.getDosage().trim().isEmpty()) {
                holder.dosage.setVisibility(View.VISIBLE);
                holder.dosage.setText(med.getDosage());
            } else {
                holder.dosage.setVisibility(View.GONE);
            }

            holder.schedule.setText(med.getScheduleText());

            // 周期 + 批号
            StringBuilder cyc = new StringBuilder();
            if ((med.getStartDate() != null && !med.getStartDate().trim().isEmpty())
                    || (med.getEndDate() != null && !med.getEndDate().trim().isEmpty())) {
                cyc.append("周期 ").append(med.getCycleText());
            }
            if (med.getBatchNo() != null && !med.getBatchNo().trim().isEmpty()) {
                if (cyc.length() > 0) cyc.append(" · ");
                cyc.append("批号 ").append(med.getBatchNo().trim());
            }
            if (cyc.length() > 0) {
                holder.cycle.setVisibility(View.VISIBLE);
                holder.cycle.setText(cyc.toString());
            } else {
                holder.cycle.setVisibility(View.GONE);
            }

            String detail = med.getSpecSummary();
            if (detail != null) {
                holder.detail.setVisibility(View.VISIBLE);
                holder.detail.setText(detail);
            } else {
                holder.detail.setVisibility(View.GONE);
            }

            String expiry = med.getExpirySummary();
            if (expiry != null) {
                holder.expiry.setVisibility(View.VISIBLE);
                holder.expiry.setText(expiry);
                int state = med.getExpiryState();
                if (state == Medication.EXPIRY_EXPIRED) holder.expiry.setTextColor(0xFFE53935);
                else if (state == Medication.EXPIRY_SOON) holder.expiry.setTextColor(0xFFFB8C00);
                else holder.expiry.setTextColor(0xFF999999);
            } else {
                holder.expiry.setVisibility(View.GONE);
            }

            if (med.getNotes() != null && !med.getNotes().trim().isEmpty()) {
                holder.notes.setVisibility(View.VISIBLE);
                holder.notes.setText(med.getNotes());
            } else {
                holder.notes.setVisibility(View.GONE);
            }

            if (med.isArchivedState()) {
                holder.stats.setText("已归档");
                holder.stats.setTextColor(0xFF999999);
            } else {
                String stats = statsMap.get(med.getId());
                holder.stats.setText(stats != null ? stats : "");
                holder.stats.setTextColor(0xFF999999);
            }

            // 归档/未开始 状态
            String today = DatabaseHelper.DATE_KEY_FORMAT.format(new Date());
            boolean started = med.coversDate(today);
            if (med.isArchivedState()) {
                holder.badge.setVisibility(View.VISIBLE);
                holder.badge.setText(med.isExpired() ? "已过期" : "已归档");
                holder.btnTake.setText("已归档");
                holder.btnTake.setEnabled(false);
                holder.btnTake.setTextColor(Color.WHITE);
                holder.btnTake.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFBDBDBD));
                holder.btnTake.setOnClickListener(null);
            } else if (!started) {
                holder.badge.setVisibility(View.VISIBLE);
                holder.badge.setText("未开始");
                holder.btnTake.setText("未开始");
                holder.btnTake.setEnabled(false);
                holder.btnTake.setTextColor(Color.WHITE);
                holder.btnTake.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFBDBDBD));
                holder.btnTake.setOnClickListener(null);
            } else {
                holder.badge.setVisibility(View.GONE);
                boolean isTaken = Boolean.TRUE.equals(todayMap.get(med.getId()));
                if (isTaken) {
                    holder.btnTake.setText("已服 ✓");
                    holder.btnTake.setTextColor(Color.WHITE);
                    holder.btnTake.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF43A047));
                } else {
                    holder.btnTake.setText("标记已服");
                    holder.btnTake.setTextColor(Color.WHITE);
                    holder.btnTake.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFBDBDBD));
                }
                holder.btnTake.setEnabled(true);
                holder.btnTake.setOnClickListener(v -> toggleToday(med));
            }
            holder.btnEdit.setOnClickListener(v -> openEdit(med));

            return convertView;
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private static class ViewHolder {
        TextView name, dosage, schedule, cycle, detail, expiry, notes, stats, badge;
        Button btnTake, btnEdit;
    }
}
