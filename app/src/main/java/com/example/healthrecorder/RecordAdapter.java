package com.example.healthrecorder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordAdapter extends BaseAdapter {
    public interface OnItemClickListener {
        void onItemClick(Record record);
    }
    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }
    private Context context;
    private List<Record> recordList;
    private final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final int[] userColors = {0xFF4CAF50, 0xFF2196F3, 0xFFFF9800, 0xFF9C27B0, 0xFFF44336};

    // 状态色：正常/偏高/正常高值
    private static final int COLOR_NORMAL = 0xFF4CAF50;
    private static final int COLOR_WARN   = 0xFFFF9800;
    private static final int COLOR_DANGER = 0xFFF44336;

    public RecordAdapter(Context context, List<Record> recordList) {
        this.context = context;
        this.recordList = recordList;
    }

    @Override public int getCount() { return recordList.size(); }
    @Override public Object getItem(int p) { return recordList.get(p); }
    @Override public long getItemId(int p) { return p; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_record, parent, false);
            holder = new ViewHolder();
            holder.viewStatusBar     = convertView.findViewById(R.id.viewStatusBar);
            holder.textViewDate      = convertView.findViewById(R.id.textViewDate);
            holder.textViewTime      = convertView.findViewById(R.id.textViewTime);
            holder.textViewUser      = convertView.findViewById(R.id.textViewUser);
            holder.layoutWeight      = convertView.findViewById(R.id.layoutWeight);
            holder.textViewWeight    = convertView.findViewById(R.id.textViewWeight);
            holder.layoutBloodPressure = convertView.findViewById(R.id.layoutBloodPressure);
            holder.textViewBloodPressure = convertView.findViewById(R.id.textViewBloodPressure);
            holder.textViewNotes     = convertView.findViewById(R.id.textViewNotes);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Record record = recordList.get(position);
        if (listener != null) {
            convertView.setOnClickListener(v -> listener.onItemClick(record));
        }
        Date d = record.getDate();

        // 相对日期文字
        holder.textViewDate.setText(getRelativeDate(d));
        holder.textViewTime.setText(TIME_FMT.format(d));

        // 用户标签
        String userName = record.getUserName();
        if (userName == null || userName.isEmpty()) userName = "本人";
        holder.textViewUser.setText(userName);
        int colorIdx = 0;
        try {
            DatabaseHelper db = new DatabaseHelper(context);
            User u = db.getUserById(record.getUserId());
            if (u != null) colorIdx = u.getColor();
        } catch (Exception ignored) {}
        int tagColor = userColors[colorIdx % userColors.length];
        GradientDrawable tagBg = new GradientDrawable();
        tagBg.setShape(GradientDrawable.RECTANGLE);
        tagBg.setCornerRadius(12);
        tagBg.setColor(tagColor);
        holder.textViewUser.setBackground(tagBg);

        boolean hasWeight = record.hasWeight();
        boolean hasBP = record.hasBloodPressure();

        // 状态色条：体重优先判断，血压次之
        int statusColor = COLOR_NORMAL;
        if (hasWeight) {
            float w = record.getWeight();
            if (w > 90f || w < 35f) {
                statusColor = COLOR_DANGER;
            } else if (w > 75f || w < 42f) {
                statusColor = COLOR_WARN;
            }
        } else if (hasBP) {
            int sys = record.getSystolic();
            int dia = record.getDiastolic();
            if (sys >= 140 || dia >= 90) {
                statusColor = COLOR_DANGER;
            } else if (sys >= 130 || dia >= 85) {
                statusColor = COLOR_WARN;
            }
        }
        holder.viewStatusBar.setBackgroundColor(statusColor);

        // 体重
        holder.layoutWeight.setVisibility(hasWeight ? View.VISIBLE : View.GONE);
        if (hasWeight) {
            holder.textViewWeight.setText(String.format(Locale.getDefault(), "%.2f", record.getWeight()));
        }

        // 血压
        holder.layoutBloodPressure.setVisibility(hasBP ? View.VISIBLE : View.GONE);
        if (hasBP) {
            holder.textViewBloodPressure.setText(record.getSystolic() + "/" + record.getDiastolic());
        }

        // 备注
        if (!hasWeight && !hasBP && record.getNotes() != null && !record.getNotes().trim().isEmpty()) {
            holder.textViewNotes.setVisibility(View.VISIBLE);
            holder.textViewNotes.setText(record.getNotes());
        } else {
            holder.textViewNotes.setVisibility(View.GONE);
        }

        return convertView;
    }

    /** 返回相对日期文字：今天/昨天/周X/MM-dd */
    private String getRelativeDate(Date d) {
        Calendar cal = Calendar.getInstance();
        Calendar rec = Calendar.getInstance();
        rec.setTime(d);
        int todayDow = cal.get(Calendar.DAY_OF_WEEK);
        cal.add(Calendar.DAY_OF_YEAR, 0);
        rec.set(Calendar.HOUR_OF_DAY, 0);
        rec.set(Calendar.MINUTE, 0);
        rec.set(Calendar.SECOND, 0);
        rec.set(Calendar.MILLISECOND, 0);
        Calendar today0 = Calendar.getInstance();
        today0.set(Calendar.HOUR_OF_DAY, 0);
        today0.set(Calendar.MINUTE, 0);
        today0.set(Calendar.SECOND, 0);
        today0.set(Calendar.MILLISECOND, 0);
        Calendar yesterday0 = (Calendar) today0.clone();
        yesterday0.add(Calendar.DAY_OF_YEAR, -1);

        if (rec.after(yesterday0) || rec.equals(yesterday0)) {
            // 今天或昨天
            if (rec.after(today0) || rec.equals(today0)) return "今天";
            return "昨天";
        }
        // 同一年：MM-dd；跨年：yyyy-MM-dd
        Calendar thisYear0 = Calendar.getInstance();
        thisYear0.set(Calendar.MONTH, Calendar.JANUARY);
        thisYear0.set(Calendar.DAY_OF_MONTH, 1);
        thisYear0.set(Calendar.HOUR_OF_DAY, 0);
        thisYear0.set(Calendar.MINUTE, 0);
        thisYear0.set(Calendar.SECOND, 0);
        thisYear0.set(Calendar.MILLISECOND, 0);
        if (rec.after(thisYear0) || rec.equals(thisYear0)) {
            return String.format(Locale.getDefault(), "%02d-%02d",
                    rec.get(Calendar.MONTH) + 1, rec.get(Calendar.DAY_OF_MONTH));
        }
        return String.format(Locale.getDefault(), "%d-%02d-%02d",
                rec.get(Calendar.YEAR),
                rec.get(Calendar.MONTH) + 1, rec.get(Calendar.DAY_OF_MONTH));
    }

    static class ViewHolder {
        View      viewStatusBar;
        TextView  textViewDate, textViewTime, textViewUser;
        LinearLayout layoutWeight;
        TextView  textViewWeight;
        LinearLayout layoutBloodPressure;
        TextView  textViewBloodPressure;
        TextView  textViewNotes;
    }
}
