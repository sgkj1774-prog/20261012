package com.example.healthrecorder.event;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthrecorder.HealthRepository;
import com.example.healthrecorder.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventStatsActivity extends AppCompatActivity {

    private HealthRepository repository;
    private final SimpleDateFormat DAY_FMT = new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.getDefault());
    private final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_stats);
        repository = HealthRepository.getInstance(getApplicationContext());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvTotal = findViewById(R.id.tvTotal);
        TextView tvPending = findViewById(R.id.tvPending);
        TextView tvDone = findViewById(R.id.tvDone);
        TextView tvCancel = findViewById(R.id.tvCancel);
        TextView tvRemindOn = findViewById(R.id.tvRemindOn);
        TextView tvByStatus = findViewById(R.id.tvByStatus);
        TextView tvByPriority = findViewById(R.id.tvByPriority);
        TextView tvByRepeat = findViewById(R.id.tvByRepeat);

        try {
            List<Event> events = repository.getAllEvents();
            int total = events == null ? 0 : events.size();

            int pending = 0, done = 0, cancel = 0, remindOn = 0;
            Map<String, Integer> byStatus = new HashMap<>();
            Map<String, Integer> byPriority = new HashMap<>();
            Map<String, Integer> byRepeat = new HashMap<>();

            if (events != null) {
                for (Event e : events) {
                    if (e == null) continue;
                    int st = e.getStatus();
                    if (st == 1) done++;
                    else if (st == 2) cancel++;
                    else pending++;
                    byStatus.put(e.getStatusText(), byStatus.getOrDefault(e.getStatusText(), 0) + 1);

                    byPriority.put(e.getPriorityText(), byPriority.getOrDefault(e.getPriorityText(), 0) + 1);
                    byRepeat.put(e.getRepeatText(), byRepeat.getOrDefault(e.getRepeatText(), 0) + 1);

                    if (e.isRemindEnabled()) remindOn++;
                }
            }

            // 日历：橙点=记录时间，蓝环=提醒时间
            EventCalendarView calendar = findViewById(R.id.eventCalendar);
            if (calendar != null) {
                calendar.setEvents(events);
                calendar.setOnDayClickListener(this::showDayDetail);
            }

            tvTotal.setText("总事项数：" + total);
            tvPending.setText("待办：" + pending);
            tvDone.setText("已完成：" + done);
            tvCancel.setText("已取消：" + cancel);
            tvRemindOn.setText("已开启提醒：" + remindOn);

            tvByStatus.setText(mapToText(byStatus));
            tvByPriority.setText(mapToText(byPriority));
            tvByRepeat.setText(mapToText(byRepeat));
        } catch (Throwable t) {
            tvTotal.setText("统计失败：" + t.getClass().getSimpleName());
        }
    }

    /** 点击日历某天：列出当天事项，并分别标注记录时间与提醒时间 */
    private void showDayDetail(Calendar day, List<Event> dayEvents) {
        StringBuilder sb = new StringBuilder();
        if (dayEvents == null || dayEvents.isEmpty()) {
            sb.append("这一天没有安排事项");
        } else {
            for (Event e : dayEvents) {
                boolean onEventDay = isSameDay(e.getEventTime(), day);
                boolean onRemindDay = e.isRemindEnabled() && isSameDay(e.getRemindTime(), day);
                sb.append("【").append(e.getPriorityText()).append("】")
                        .append(e.getTitle() == null || e.getTitle().isEmpty() ? "(无标题)" : e.getTitle())
                        .append("  · ").append(e.getStatusText()).append("\n");
                if (onEventDay) {
                    sb.append("🟠 记录时间 ").append(TIME_FMT.format(e.getEventTime())).append("\n");
                }
                if (onRemindDay) {
                    sb.append("🔵 提醒时间 ").append(TIME_FMT.format(e.getRemindTime())).append("\n");
                }
                sb.append("\n");
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(DAY_FMT.format(day.getTime()))
                .setMessage(sb.toString().trim())
                .setPositiveButton("关闭", null)
                .show();
    }

    private boolean isSameDay(java.util.Date date, Calendar day) {
        if (date == null) return false;
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.YEAR) == day.get(Calendar.YEAR)
                && c.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR);
    }

    private String mapToText(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return "（无数据）";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> en : map.entrySet()) {
            if (!first) sb.append("\n");
            first = false;
            sb.append("• ").append(en.getKey()).append("：").append(en.getValue()).append(" 项");
        }
        return sb.toString();
    }
}
