package com.example.healthrecorder.event;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 重要事项月历。
 *
 * 用两种标记区分时间：
 *  - 记录时间（event_time）：橙色实心圆点
 *  - 提醒时间（remind_time，且已开启提醒）：蓝色空心圆环
 * 同一天两者都有时，两个标记并排显示。
 */
public class EventCalendarView extends LinearLayout {

    private static final int COLOR_EVENT = 0xFFFF9800;   // 记录时间：橙
    private static final int COLOR_REMIND = 0xFF2196F3;  // 提醒时间：蓝
    private static final int COLOR_TEXT = 0xFF333333;
    private static final int COLOR_TEXT_MUTED = 0xFFBBBBBB;

    private final SimpleDateFormat MONTH_FMT = new SimpleDateFormat("yyyy年M月", Locale.getDefault());

    private final Calendar displayCal = Calendar.getInstance();
    private List<Event> events = new ArrayList<>();

    private TextView tvMonth;
    private GridLayout grid;
    private OnDayClickListener dayClickListener;

    public interface OnDayClickListener {
        /** @param date 被点击的日期（当天 00:00） @param dayEvents 当天的全部事项 */
        void onDayClick(Calendar date, List<Event> dayEvents);
    }

    public EventCalendarView(Context context) {
        super(context);
        init(context);
    }

    public EventCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EventCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context ctx) {
        setOrientation(VERTICAL);
        setBackgroundColor(0xFFFFFFFF);
        int pad = dp(12);
        setPadding(pad, pad, pad, pad);

        // ── 标题 ──
        TextView title = new TextView(ctx);
        title.setText("📅 事项日历");
        title.setTextSize(16);
        title.setTextColor(COLOR_TEXT);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LayoutParams titleLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleLp.bottomMargin = dp(10);
        addView(title, titleLp);

        // ── 月份切换 ──
        LinearLayout nav = new LinearLayout(ctx);
        nav.setOrientation(HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);

        Button btnPrev = makeNavButton(ctx, "‹");
        btnPrev.setOnClickListener(v -> {
            displayCal.add(Calendar.MONTH, -1);
            build();
        });
        nav.addView(btnPrev, new LayoutParams(dp(36), dp(36)));

        tvMonth = new TextView(ctx);
        tvMonth.setTextSize(16);
        tvMonth.setTextColor(COLOR_TEXT);
        tvMonth.setTypeface(null, android.graphics.Typeface.BOLD);
        tvMonth.setGravity(Gravity.CENTER);
        nav.addView(tvMonth, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        Button btnNext = makeNavButton(ctx, "›");
        btnNext.setOnClickListener(v -> {
            displayCal.add(Calendar.MONTH, 1);
            build();
        });
        nav.addView(btnNext, new LayoutParams(dp(36), dp(36)));
        addView(nav, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // ── 星期表头（周一起始，与主页日历一致）──
        LinearLayout weekHeader = new LinearLayout(ctx);
        weekHeader.setOrientation(HORIZONTAL);
        String[] weekdays = {"一", "二", "三", "四", "五", "六", "日"};
        for (String w : weekdays) {
            TextView t = new TextView(ctx);
            t.setText(w);
            t.setTextSize(12);
            t.setTextColor(0xFF888888);
            t.setGravity(Gravity.CENTER);
            weekHeader.addView(t, new LayoutParams(0, dp(28), 1f));
        }
        LayoutParams weekLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        weekLp.topMargin = dp(8);
        weekLp.bottomMargin = dp(4);
        addView(weekHeader, weekLp);

        // ── 日期网格 ──
        grid = new GridLayout(ctx);
        grid.setColumnCount(7);
        grid.setRowCount(6);
        addView(grid, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // ── 图例 ──
        LinearLayout legend = new LinearLayout(ctx);
        legend.setOrientation(HORIZONTAL);
        legend.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams legendLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        legendLp.topMargin = dp(10);

        legend.addView(makeDot(COLOR_EVENT, false), dotLp());
        legend.addView(makeLegendText(ctx, "记录时间"), legendTextLp());
        legend.addView(makeDot(COLOR_REMIND, true), dotLp());
        legend.addView(makeLegendText(ctx, "提醒时间"), legendTextLp());
        addView(legend, legendLp);
    }

    private Button makeNavButton(Context ctx, String text) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setTextSize(20);
        b.setTextColor(COLOR_TEXT);
        b.setBackgroundColor(0x00000000);
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setStateListAnimator(null);
        return b;
    }

    private TextView makeLegendText(Context ctx, String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(11);
        t.setTextColor(0xFF888888);
        return t;
    }

    private LayoutParams dotLp() {
        return new LayoutParams(dp(8), dp(8));
    }

    private LayoutParams legendTextLp() {
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(14);
        return lp;
    }

    private View makeDot(int color, boolean ring) {
        View v = new View(getContext());
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        if (ring) {
            d.setColor(0x00000000);
            d.setStroke(dp(2), color);
        } else {
            d.setColor(color);
        }
        v.setBackground(d);
        return v;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    public void setEvents(List<Event> events) {
        this.events = events != null ? events : new ArrayList<>();
        build();
    }

    public void setOnDayClickListener(OnDayClickListener l) {
        this.dayClickListener = l;
    }

    private void build() {
        tvMonth.setText(MONTH_FMT.format(displayCal.getTime()));
        grid.removeAllViews();

        Calendar cal = (Calendar) displayCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        clearTime(cal);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int firstDow = cal.get(Calendar.DAY_OF_WEEK);   // 1=周日 ... 7=周六
        int leadingBlank = (firstDow + 5) % 7;          // 周一起始时的前置空格数

        for (int i = 0; i < leadingBlank; i++) {
            grid.addView(emptyCell(), cellParams());
        }

        for (int day = 1; day <= daysInMonth; day++) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.set(Calendar.DAY_OF_MONTH, day);
            grid.addView(dayCell(dayCal), cellParams());
        }

        int used = leadingBlank + daysInMonth;
        int trailing = (7 - used % 7) % 7;
        for (int i = 0; i < trailing; i++) {
            grid.addView(emptyCell(), cellParams());
        }
    }

    private GridLayout.LayoutParams cellParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = LayoutParams.WRAP_CONTENT;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        return lp;
    }

    private View emptyCell() {
        return new View(getContext());
    }

    private View dayCell(Calendar dayCal) {
        Context ctx = getContext();
        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(0, dp(3), 0, dp(3));

        int dow = dayCal.get(Calendar.DAY_OF_WEEK);
        boolean weekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY;

        TextView num = new TextView(ctx);
        num.setText(String.valueOf(dayCal.get(Calendar.DAY_OF_MONTH)));
        num.setTextSize(14);
        num.setGravity(Gravity.CENTER);
        if (weekend) num.setTextColor(0xFF999999);
        else num.setTextColor(COLOR_TEXT);
        // 今天用橙色数字提亮
        if (isToday(dayCal)) {
            num.setTextColor(COLOR_EVENT);
            num.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        cell.addView(num, new LayoutParams(LayoutParams.WRAP_CONTENT, dp(22)));

        boolean hasEvent = false;
        boolean hasRemind = false;
        List<Event> dayEvents = new ArrayList<>();
        for (Event e : events) {
            if (e == null) continue;
            boolean sameEventDay = isSameDay(e.getEventTime(), dayCal);
            boolean sameRemindDay = e.isRemindEnabled() && isSameDay(e.getRemindTime(), dayCal);
            if (sameEventDay) { hasEvent = true; dayEvents.add(e); }
            if (sameRemindDay) {
                hasRemind = true;
                if (!sameEventDay) dayEvents.add(e);
            }
        }

        LinearLayout dots = new LinearLayout(ctx);
        dots.setOrientation(HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        if (hasEvent) {
            View d = makeDot(COLOR_EVENT, false);
            LayoutParams lp = new LayoutParams(dp(5), dp(5));
            lp.rightMargin = hasRemind ? dp(3) : 0;
            dots.addView(d, lp);
        }
        if (hasRemind) {
            View d = makeDot(COLOR_REMIND, true);
            dots.addView(d, new LayoutParams(dp(6), dp(6)));
        }
        // 无标记也占位，保证各格高度一致
        cell.addView(dots, new LayoutParams(LayoutParams.WRAP_CONTENT, dp(8)));

        final List<Event> finalDayEvents = dayEvents;
        cell.setOnClickListener(v -> {
            if (dayClickListener != null) {
                dayClickListener.onDayClick(dayCal, finalDayEvents);
            }
        });
        return cell;
    }

    private boolean isToday(Calendar c) {
        Calendar now = Calendar.getInstance();
        return c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isSameDay(java.util.Date date, Calendar day) {
        if (date == null) return false;
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.YEAR) == day.get(Calendar.YEAR)
                && c.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR);
    }

    private void clearTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    public void setDisplayMonth(int year, int monthZeroBased) {
        displayCal.set(Calendar.YEAR, year);
        displayCal.set(Calendar.MONTH, monthZeroBased);
        displayCal.set(Calendar.DAY_OF_MONTH, 1);
        clearTime(displayCal);
        build();
    }
}
