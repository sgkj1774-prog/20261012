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
 * 打卡粒度是「一种药一天一条」，重复点击即取消。
 */
public class MedicationListActivity extends AppCompatActivity {

    private ListView listView;
    private TextView tvSummary, tvEmpty;
    private MedAdapter adapter;
    private HealthRepository repository;

    private final List<Medication> meds = new ArrayList<>();
    private final Map<Integer, String> statsMap = new HashMap<>();
    private final Map<Integer, Boolean> todayMap = new HashMap<>();

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

        adapter = new MedAdapter(this);
        listView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            Intent intent = new Intent(this, MedicationAddActivity.class);
            addLauncher.launch(intent);
        });

        listView.setOnItemClickListener((parent, view, position, id) -> openEdit(meds.get(position)));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showActions(meds.get(position));
            return true;
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

    /** 长按弹操作菜单：编辑 / 删除 */
    private void showActions(final Medication m) {
        new AlertDialog.Builder(this)
                .setTitle(m.getName())
                .setItems(new String[]{"编辑药品信息", "删除药品"}, (d, which) -> {
                    if (which == 0) openEdit(m);
                    else confirmDelete(m);
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
        meds.clear();
        List<Medication> all = repository.getAllMedications();
        if (all != null) meds.addAll(all);

        String today = DatabaseHelper.DATE_KEY_FORMAT.format(new Date());
        statsMap.clear();
        todayMap.clear();

        int due = 0, taken = 0;
        for (Medication m : meds) {
            MedicationLog log = repository.getMedicationLog(m.getId(), today);
            boolean isTaken = log != null && log.isTaken();
            todayMap.put(m.getId(), isTaken);
            statsMap.put(m.getId(), buildStats(m));
            due++;
            if (isTaken) taken++;
        }

        if (due == 0) {
            tvSummary.setText("今日暂无服药安排");
        } else if (taken >= due) {
            tvSummary.setText("今日 " + due + " 种药已全部服用 ✓");
        } else {
            tvSummary.setText("今日：应服 " + due + " 种，已服 " + taken + " 种");
        }

        adapter.notifyDataSetChanged();
        boolean empty = meds.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        listView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /**
     * 近 7 天依从性：统计区间为「建档日（或 6 天前）到昨天」，
     * 今天还没过完，不计入漏服。
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

        if (m.getCreatedAt() != null) {
            Calendar created = Calendar.getInstance();
            created.setTime(m.getCreatedAt());
            created.set(Calendar.HOUR_OF_DAY, 0);
            created.set(Calendar.MINUTE, 0);
            created.set(Calendar.SECOND, 0);
            created.set(Calendar.MILLISECOND, 0);
            if (created.after(start)) start = created;
        }

        int dueDays = 0, takenDays = 0;
        Calendar cur = (Calendar) start.clone();
        while (cur.before(today0)) {
            dueDays++;
            if (takenDates.contains(DatabaseHelper.DATE_KEY_FORMAT.format(cur.getTime()))) takenDays++;
            cur.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (dueDays <= 0) {
            return "刚建档，暂无历史记录";
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
    // 列表适配器
    // ============================================================
    private class MedAdapter extends BaseAdapter {
        private final LayoutInflater inflater;

        MedAdapter(Context context) {
            this.inflater = LayoutInflater.from(context);
        }

        @Override public int getCount() { return meds.size(); }
        @Override public Object getItem(int p) { return meds.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_medication, parent, false);
                holder = new ViewHolder();
                holder.name = convertView.findViewById(R.id.tvMedName);
                holder.dosage = convertView.findViewById(R.id.tvMedDosage);
                holder.schedule = convertView.findViewById(R.id.tvMedSchedule);
                holder.detail = convertView.findViewById(R.id.tvMedDetail);
                holder.expiry = convertView.findViewById(R.id.tvMedExpiry);
                holder.notes = convertView.findViewById(R.id.tvMedNotes);
                holder.stats = convertView.findViewById(R.id.tvMedStats);
                holder.btnTake = convertView.findViewById(R.id.btnTake);
                holder.btnEdit = convertView.findViewById(R.id.btnEdit);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final Medication m = meds.get(position);
            holder.name.setText(m.getName());

            if (m.getDosage() != null && !m.getDosage().trim().isEmpty()) {
                holder.dosage.setVisibility(View.VISIBLE);
                holder.dosage.setText(m.getDosage());
            } else {
                holder.dosage.setVisibility(View.GONE);
            }

            holder.schedule.setText(m.getScheduleText());

            String detail = m.getSpecSummary();
            if (detail != null) {
                holder.detail.setVisibility(View.VISIBLE);
                holder.detail.setText(detail);
            } else {
                holder.detail.setVisibility(View.GONE);
            }

            String expiry = m.getExpirySummary();
            if (expiry != null) {
                holder.expiry.setVisibility(View.VISIBLE);
                holder.expiry.setText(expiry);
                int state = m.getExpiryState();
                if (state == Medication.EXPIRY_EXPIRED) {
                    holder.expiry.setTextColor(0xFFE53935);
                } else if (state == Medication.EXPIRY_SOON) {
                    holder.expiry.setTextColor(0xFFFB8C00);
                } else {
                    holder.expiry.setTextColor(0xFF999999);
                }
            } else {
                holder.expiry.setVisibility(View.GONE);
            }

            if (m.getNotes() != null && !m.getNotes().trim().isEmpty()) {
                holder.notes.setVisibility(View.VISIBLE);
                holder.notes.setText(m.getNotes());
            } else {
                holder.notes.setVisibility(View.GONE);
            }

            String stats = statsMap.get(m.getId());
            holder.stats.setText(stats != null ? stats : "");

            boolean isTaken = Boolean.TRUE.equals(todayMap.get(m.getId()));
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
            holder.btnTake.setOnClickListener(v -> toggleToday(m));
            holder.btnEdit.setOnClickListener(v -> openEdit(m));

            return convertView;
        }
    }

    private static class ViewHolder {
        TextView name, dosage, schedule, detail, expiry, notes, stats;
        Button btnTake, btnEdit;
    }
}
