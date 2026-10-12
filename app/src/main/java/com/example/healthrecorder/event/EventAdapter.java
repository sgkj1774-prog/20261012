package com.example.healthrecorder.event;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthrecorder.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {
    private List<Event> list = new ArrayList<>();
    private final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public interface OnItemClickListener { void onItemClick(Event ev); }
    public interface OnItemLongClickListener { void onItemLongClick(Event ev); }
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    public void setOnItemClickListener(OnItemClickListener l) { this.clickListener = l; }
    public void setOnItemLongClickListener(OnItemLongClickListener l) { this.longClickListener = l; }

    public void setList(List<Event> list) {
        this.list = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    private String getRelativeDate(Date d) {
        if (d == null) return "—";
        Calendar cal = Calendar.getInstance();
        Calendar evCal = Calendar.getInstance();
        evCal.setTime(d);
        if (cal.get(Calendar.YEAR) == evCal.get(Calendar.YEAR)
                && cal.get(Calendar.DAY_OF_YEAR) == evCal.get(Calendar.DAY_OF_YEAR)) {
            return "今天";
        }
        evCal.add(Calendar.DAY_OF_YEAR, 1);
        if (cal.get(Calendar.YEAR) == evCal.get(Calendar.YEAR)
                && cal.get(Calendar.DAY_OF_YEAR) == evCal.get(Calendar.DAY_OF_YEAR)) {
            return "明天";
        }
        evCal.add(Calendar.DAY_OF_YEAR, -2);
        if (cal.get(Calendar.YEAR) == evCal.get(Calendar.YEAR)
                && cal.get(Calendar.DAY_OF_YEAR) == evCal.get(Calendar.DAY_OF_YEAR)) {
            return "昨天";
        }
        return DATE_FMT.format(d);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Event ev = list.get(pos);
        h.textTitle.setText(ev.getTitle() != null ? ev.getTitle() : "");

        // 优先级色条
        int barColor;
        if (ev.getPriority() == 3) {
            barColor = Color.parseColor("#F44336"); // 高=红
        } else if (ev.getPriority() == 2) {
            barColor = Color.parseColor("#FF9800"); // 中=橙
        } else {
            barColor = Color.parseColor("#4CAF50"); // 低=绿
        }
        h.viewPriorityBar.setBackgroundColor(barColor);

        // 事件时间
        String dateStr = getRelativeDate(ev.getEventTime());
        String timeStr = ev.getEventTime() != null ? TIME_FMT.format(ev.getEventTime()) : "—";
        h.textDate.setText(dateStr);
        h.textTime.setText(timeStr);

        // 内容/备注
        if (ev.getContent() != null && !ev.getContent().isEmpty()) {
            h.textContent.setVisibility(View.VISIBLE);
            h.textContent.setText(ev.getContent());
        } else {
            h.textContent.setVisibility(View.GONE);
        }

        // 提醒时间
        if (ev.isRemindEnabled() && ev.getRemindTime() != null) {
            h.textRemind.setVisibility(View.VISIBLE);
            h.textRemind.setText("⏰ " + getRelativeDate(ev.getRemindTime()) + " " + TIME_FMT.format(ev.getRemindTime()));
        } else {
            h.textRemind.setVisibility(View.GONE);
        }

        // 重复类型标签
        String repeatText = ev.getRepeatText();
        if (repeatText != null && !repeatText.equals("不重复")) {
            h.textRepeat.setVisibility(View.VISIBLE);
            h.textRepeat.setText("🔁 " + repeatText);
        } else {
            h.textRepeat.setVisibility(View.GONE);
        }

        // 状态标签
        String statusText = ev.getStatusText();
        if (ev.getStatus() == 1) {
            h.textStatus.setVisibility(View.VISIBLE);
            h.textStatus.setText("已完成");
            h.textStatus.setTextColor(Color.parseColor("#999999"));
            h.textStatus.setBackgroundResource(R.drawable.user_tag_bg);
        } else if (ev.getStatus() == 2) {
            h.textStatus.setVisibility(View.VISIBLE);
            h.textStatus.setText("已取消");
            h.textStatus.setTextColor(Color.parseColor("#999999"));
            h.textStatus.setBackgroundResource(R.drawable.user_tag_bg);
        } else {
            h.textStatus.setVisibility(View.GONE);
        }

        // 优先级文字标签
        String priText = ev.getPriorityText();
        h.textPriority.setText(priText);
        int priColor;
        if (ev.getPriority() == 3) {
            priColor = Color.parseColor("#F44336");
        } else if (ev.getPriority() == 2) {
            priColor = Color.parseColor("#FF9800");
        } else {
            priColor = Color.parseColor("#4CAF50");
        }
        h.textPriority.setTextColor(priColor);

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(ev);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onItemLongClick(ev);
            return true;
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        View viewPriorityBar;
        TextView textTitle, textDate, textTime, textContent;
        TextView textRemind, textRepeat, textStatus, textPriority;

        VH(View v) {
            super(v);
            viewPriorityBar = v.findViewById(R.id.viewPriorityBar);
            textTitle = v.findViewById(R.id.textTitle);
            textDate = v.findViewById(R.id.textDate);
            textTime = v.findViewById(R.id.textTime);
            textContent = v.findViewById(R.id.textContent);
            textRemind = v.findViewById(R.id.textRemind);
            textRepeat = v.findViewById(R.id.textRepeat);
            textStatus = v.findViewById(R.id.textStatus);
            textPriority = v.findViewById(R.id.textPriority);
        }
    }
}
