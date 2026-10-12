package com.example.healthrecorder.breathing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthrecorder.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BreathingAdapter extends RecyclerView.Adapter<BreathingAdapter.VH> {
    private List<BreathingRecord> list = new ArrayList<>();

    public interface OnItemClickListener { void onItemClick(BreathingRecord r); }
    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    public void setList(List<BreathingRecord> list) {
        this.list = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_breathing_record, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        BreathingRecord r = list.get(pos);
        Date d = r.getDate();

        h.textViewDate.setText(relativeDay(d));
        h.textViewTime.setText(timeText(d));
        h.textMode.setText(r.getModeText());
        h.textDuration.setText(durationText(r.getDuration()));
        h.textStars.setText(starText(r.getScore()));

        if (r.getNotes() != null && !r.getNotes().isEmpty()) {
            h.textViewNotes.setVisibility(View.VISIBLE);
            h.textViewNotes.setText(r.getNotes());
        } else {
            h.textViewNotes.setVisibility(View.GONE);
        }

        if (r.getUserName() != null && !r.getUserName().isEmpty()) {
            h.textViewUser.setText(r.getUserName());
        } else {
            h.textViewUser.setText("本人");
        }

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(r); });
    }

    @Override public int getItemCount() { return list.size(); }

    private String relativeDay(Date d) {
        if (d == null) return "";
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        long diffDays = (startOfDay(new Date()).getTimeInMillis()
                - startOfDay(d).getTimeInMillis()) / (24L * 60 * 60 * 1000);
        if (diffDays == 0) return "今天";
        if (diffDays == 1) return "昨天";
        if (diffDays == 2) return "前天";
        return String.format(java.util.Locale.getDefault(), "%02d-%02d",
                cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    private Calendar startOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private String timeText(Date d) {
        if (d == null) return "";
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return String.format(java.util.Locale.getDefault(), "%02d:%02d",
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    private String durationText(int seconds) {
        if (seconds <= 0) return "0分钟";
        int min = seconds / 60;
        int sec = seconds % 60;
        if (min == 0) return sec + "秒";
        if (sec == 0) return min + "分钟";
        return min + "分" + sec + "秒";
    }

    private String starText(int score) {
        if (score < 0) score = 0;
        if (score > 10) score = 10;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < score; i++) sb.append("★");
        for (int i = 0; i < 10 - score; i++) sb.append("☆");
        return sb.toString();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView textViewDate, textViewTime, textMode, textDuration, textStars, textViewNotes, textViewUser;
        VH(View v) {
            super(v);
            textViewDate = v.findViewById(R.id.textViewDate);
            textViewTime = v.findViewById(R.id.textViewTime);
            textMode = v.findViewById(R.id.textMode);
            textDuration = v.findViewById(R.id.textDuration);
            textStars = v.findViewById(R.id.textStars);
            textViewNotes = v.findViewById(R.id.textViewNotes);
            textViewUser = v.findViewById(R.id.textViewUser);
        }
    }
}
