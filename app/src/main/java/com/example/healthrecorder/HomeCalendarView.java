package com.example.healthrecorder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 健康日历自定义视图 - 使用 LinearLayout 实现，避免 GridLayout 兼容性问题
 */
public class HomeCalendarView extends LinearLayout {

    private LinearLayout headerMonth;
    private TextView tvMonth;
    private TextView tvMonthBig;
    private LinearLayout headerDays;
    private LinearLayout calendarRows; // 包含所有日期行的容器
    private TextView tvNoRecord;
    private ImageButton btnPrev;
    private ImageButton btnNext;

    private Calendar displayCal = Calendar.getInstance();
    private Calendar today0 = Calendar.getInstance();

    private final SimpleDateFormat MONTH_FMT = new SimpleDateFormat("yyyy年 M月", Locale.getDefault());
    private final SimpleDateFormat MONTH_BIG_FMT = new SimpleDateFormat("yyyy年M月", Locale.getDefault());
    private final SimpleDateFormat DATE_KEY_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public static final int TYPE_HEALTH = 0x01;
    public static final int TYPE_CAT = 0x02;
    public static final int TYPE_TEETH = 0x04;
    public static final int TYPE_EXERCISE = 0x08;
    public static final int TYPE_BREATHING = 0x10;
    public static final int TYPE_EVENT = 0x20;
    public static final int TYPE_BOOK = 0x40;

    private static final int COLOR_HEALTH = Color.parseColor("#2196F3");
    private static final int COLOR_CAT = Color.parseColor("#FF9800");
    private static final int COLOR_TEETH = Color.parseColor("#FFC107");
    private static final int COLOR_EXERCISE = Color.parseColor("#FF9800");
    private static final int COLOR_BREATHING = Color.parseColor("#4CAF50");
    private static final int COLOR_EVENT = Color.parseColor("#9C27B0");
    private static final int COLOR_BOOK = Color.parseColor("#9E9E9E");

    private static final int[] DOT_COLORS = {
            COLOR_HEALTH, COLOR_CAT, COLOR_TEETH,
            COLOR_EXERCISE, COLOR_BREATHING, COLOR_EVENT, COLOR_BOOK
    };

    private Map<Long, Integer> dateTypeMap = new HashMap<>();
    private Map<Long, List<String>> dateSummaryMap = new HashMap<>();

    private OnDateClickListener dateClickListener;

    public interface OnDateClickListener {
        void onDateClick(Date date);
    }

    public HomeCalendarView(Context context) {
        super(context);
        init(context);
    }

    public HomeCalendarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HomeCalendarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context ctx) {
        try {
            initInternal(ctx);
        } catch (Throwable t) {
            android.util.Log.e("HomeCalendarView", "init failed", t);
            TextView errorTv = new TextView(ctx);
            errorTv.setText("日历初始化失败: " + t.getClass().getSimpleName() + "\n" + t.getMessage());
            errorTv.setTextColor(Color.RED);
            errorTv.setPadding(20, 20, 20, 20);
            addView(errorTv);
        }
    }

    private void initInternal(Context ctx) {
        today0.set(Calendar.HOUR_OF_DAY, 0);
        today0.set(Calendar.MINUTE, 0);
        today0.set(Calendar.SECOND, 0);
        today0.set(Calendar.MILLISECOND, 0);

        setOrientation(VERTICAL);
        float density = getResources().getDisplayMetrics().density;

        // 顶部导航栏
        headerMonth = new LinearLayout(ctx);
        headerMonth.setOrientation(LinearLayout.HORIZONTAL);
        headerMonth.setGravity(Gravity.CENTER_VERTICAL);
        headerMonth.setPadding((int) (12 * density), 0, (int) (12 * density), 0);
        headerMonth.setBackgroundColor(Color.WHITE);
        addView(headerMonth, new LayoutParams(LayoutParams.MATCH_PARENT, (int) (40 * density)));

        btnPrev = new ImageButton(ctx);
        btnPrev.setImageResource(android.R.drawable.ic_media_previous);
        btnPrev.setBackgroundColor(Color.TRANSPARENT);
        btnPrev.setLayoutParams(new LinearLayout.LayoutParams((int) (32 * density), (int) (32 * density)));
        headerMonth.addView(btnPrev);

        tvMonth = new TextView(ctx);
        tvMonth.setGravity(Gravity.CENTER);
        tvMonth.setTextSize(13f);
        tvMonth.setTextColor(Color.parseColor("#888888"));
        LinearLayout.LayoutParams tvMonthLp = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        tvMonth.setLayoutParams(tvMonthLp);
        headerMonth.addView(tvMonth);

        btnNext = new ImageButton(ctx);
        btnNext.setImageResource(android.R.drawable.ic_media_next);
        btnNext.setBackgroundColor(Color.TRANSPARENT);
        btnNext.setLayoutParams(new LinearLayout.LayoutParams((int) (32 * density), (int) (32 * density)));
        headerMonth.addView(btnNext);

        // 月份大标题
        tvMonthBig = new TextView(ctx);
        tvMonthBig.setGravity(Gravity.CENTER);
        tvMonthBig.setTextSize(20f);
        tvMonthBig.setTextColor(Color.parseColor("#222222"));
        tvMonthBig.getPaint().setFakeBoldText(true);
        tvMonthBig.setPadding(0, (int) (4 * density), 0, (int) (4 * density));
        tvMonthBig.setBackgroundColor(Color.WHITE);
        addView(tvMonthBig, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 星期标题行
        headerDays = new LinearLayout(ctx);
        headerDays.setOrientation(LinearLayout.HORIZONTAL);
        headerDays.setBackgroundColor(Color.parseColor("#FAFAFA"));
        headerDays.setPadding((int) (4 * density), 0, (int) (4 * density), 0);
        addView(headerDays, new LayoutParams(LayoutParams.MATCH_PARENT, (int) (28 * density)));
        setupHeaderDays();

        // 日期行容器（用 LinearLayout 代替 GridLayout）
        calendarRows = new LinearLayout(ctx);
        calendarRows.setOrientation(LinearLayout.VERTICAL);
        calendarRows.setBackgroundColor(Color.WHITE);
        calendarRows.setPadding((int) (4 * density), 0, (int) (4 * density), (int) (4 * density));
        addView(calendarRows, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 无记录提示
        tvNoRecord = new TextView(ctx);
        tvNoRecord.setText("暂无记录");
        tvNoRecord.setTextSize(14f);
        tvNoRecord.setTextColor(Color.parseColor("#999999"));
        tvNoRecord.setGravity(Gravity.CENTER);
        tvNoRecord.setPadding(0, (int) (16 * density), 0, 0);
        tvNoRecord.setVisibility(View.GONE);
        addView(tvNoRecord, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 初始化
        updateMonthLabel();
        buildCalendar();

        // 导航按钮
        btnPrev.setOnClickListener(v -> {
            displayCal.add(Calendar.MONTH, -1);
            updateMonthLabel();
            buildCalendar();
        });
        btnNext.setOnClickListener(v -> {
            displayCal.add(Calendar.MONTH, 1);
            updateMonthLabel();
            buildCalendar();
        });
    }

    private void setupHeaderDays() {
        String[] days = {"一", "二", "三", "四", "五", "六", "日"};
        for (String d : days) {
            TextView tv = new TextView(getContext());
            tv.setText(d);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(12f);
            tv.setTextColor(Color.parseColor("#999999"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            headerDays.addView(tv, lp);
        }
    }

    private void buildCalendar() {
        if (calendarRows == null) return;

        calendarRows.removeAllViews();

        Calendar cal = (Calendar) displayCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=周日
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int firstBlank = (firstDow == 0) ? 6 : firstDow - 1; // 空白格子数（对齐周一）

        // 计算需要的行数
        int totalCells = firstBlank + maxDay;
        int needRows = (totalCells + 6) / 7;
        if (needRows < 1) needRows = 1;

        // 创建每一行
        LinearLayout[] rows = new LinearLayout[needRows];
        for (int i = 0; i < needRows; i++) {
            rows[i] = new LinearLayout(getContext());
            rows[i].setOrientation(LinearLayout.HORIZONTAL);
            rows[i].setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            calendarRows.addView(rows[i]);
        }

        float density = getResources().getDisplayMetrics().density;

        // 添加空白格子
        for (int i = 0; i < firstBlank; i++) {
            int row = i / 7;
            if (row < needRows) {
                addDayCellToRow(rows[row], null, false, false, 0, 0, density);
            }
        }

        // 添加日期格子
        for (int d = 1; d <= maxDay; d++) {
            cal.set(Calendar.DAY_OF_MONTH, d);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            long dayKey = cal.getTimeInMillis();
            int types = dateTypeMap.containsKey(dayKey) ? dateTypeMap.get(dayKey) : 0;
            boolean isToday = (dayKey == today0.getTimeInMillis());

            int idx = firstBlank + d - 1;
            int row = idx / 7;
            if (row < needRows) {
                final Calendar clickCal = (Calendar) cal.clone();
                addDayCellToRow(rows[row], clickCal, true, isToday, dayKey, types, density);
            }
        }

        // 最后一行末尾填充空白占位格（保证所有日期格左对齐）
        if (needRows > 0 && rows[needRows - 1] != null) {
            int lastRowCount = rows[needRows - 1].getChildCount();
            for (int i = lastRowCount; i < 7; i++) {
                View placeholder = new View(getContext());
                LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, (int) (52 * density), 1f);
                placeholder.setLayoutParams(pLp);
                rows[needRows - 1].addView(placeholder);
            }
        }

        // 无记录提示
        if (tvNoRecord != null) {
            tvNoRecord.setVisibility(dateTypeMap.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void addDayCellToRow(LinearLayout row, final Calendar cal, boolean valid, boolean isToday,
                                  final long dayKey, int types, float density) {
        // 格子容器
        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(0, (int) (4 * density), 0, (int) (2 * density));

        LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, (int) (52 * density), 1f);
        cell.setLayoutParams(cellLp);

        // 日期文字
        TextView tvDay = new TextView(getContext());
        tvDay.setGravity(Gravity.CENTER);
        tvDay.setTextSize(13f);

        if (valid && cal != null) {
            tvDay.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));

            if (isToday) {
                tvDay.setBackgroundResource(R.drawable.circle_green);
                tvDay.setTextColor(Color.WHITE);
            } else {
                tvDay.setTextColor(Color.parseColor("#333333"));
            }

            // 颜色标记圆点行
            if (types != 0) {
                LinearLayout dotRow = new LinearLayout(getContext());
                dotRow.setOrientation(LinearLayout.HORIZONTAL);
                dotRow.setGravity(Gravity.CENTER_HORIZONTAL);
                dotRow.setPadding(0, (int) (2 * density), 0, 0);

                int dotSize = (int) (5 * density);
                LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);

                if ((types & TYPE_HEALTH) != 0) addColorDot(dotRow, dotLp, COLOR_HEALTH);
                if ((types & TYPE_CAT) != 0) addColorDot(dotRow, dotLp, COLOR_CAT);
                if ((types & TYPE_TEETH) != 0) addColorDot(dotRow, dotLp, COLOR_TEETH);
                if ((types & TYPE_EXERCISE) != 0) addColorDot(dotRow, dotLp, COLOR_EXERCISE);
                if ((types & TYPE_BREATHING) != 0) addColorDot(dotRow, dotLp, COLOR_BREATHING);
                if ((types & TYPE_EVENT) != 0) addColorDot(dotRow, dotLp, COLOR_EVENT);
                if ((types & TYPE_BOOK) != 0) addColorDot(dotRow, dotLp, COLOR_BOOK);

                cell.addView(dotRow);
            }

            // 点击事件
            final int finalTypes = types;
            cell.setOnClickListener(v -> {
                if (dateClickListener != null) {
                    dateClickListener.onDateClick(cal.getTime());
                }
                if (finalTypes != 0) {
                    showRecordPopup(cal, dayKey);
                }
            });
            cell.setClickable(true);
        } else {
            tvDay.setText("");
        }

        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvLp.gravity = Gravity.CENTER;
        cell.addView(tvDay, tvLp);

        row.addView(cell);
    }

    private void addColorDot(LinearLayout dotRow, LinearLayout.LayoutParams dotLp, int color) {
        View dot = new View(getContext());
        dot.setBackgroundColor(color);
        dot.setLayoutParams(dotLp);
        dotRow.addView(dot);
    }

    private void showRecordPopup(Calendar cal, long dayKey) {
        List<String> summaries = dateSummaryMap.get(dayKey);
        if (summaries == null || summaries.isEmpty()) return;

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 24, 32, 24);
        container.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(getContext());
        SimpleDateFormat sdf = new SimpleDateFormat("M月d日 E", Locale.CHINESE);
        title.setText(sdf.format(cal.getTime()));
        title.setTextSize(16f);
        title.setTextColor(Color.parseColor("#222222"));
        title.getPaint().setFakeBoldText(true);
        container.addView(title);

        for (String s : summaries) {
            TextView tv = new TextView(getContext());
            tv.setText("• " + s);
            tv.setTextSize(14f);
            tv.setTextColor(Color.parseColor("#555555"));
            tv.setPadding(0, 8, 0, 0);
            container.addView(tv);
        }

        PopupWindow popup = new PopupWindow(container,
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        popup.setElevation(16);
        popup.showAtLocation(this, Gravity.CENTER, 0, 0);
    }

    private void updateMonthLabel() {
        String bigLabel = MONTH_BIG_FMT.format(displayCal.getTime());
        String navLabel = MONTH_FMT.format(displayCal.getTime());
        if (tvMonthBig != null) tvMonthBig.setText(bigLabel);
        if (tvMonth != null) tvMonth.setText(navLabel);
    }

    public void setOnDateClickListener(OnDateClickListener listener) {
        this.dateClickListener = listener;
    }

    public void setDateMarkers(Set<String> markers) {
        this.dateTypeMap.clear();
        this.dateSummaryMap.clear();
        if (markers != null) {
            for (String m : markers) {
                try {
                    long key = DATE_KEY_FMT.parse(m).getTime();
                    dateTypeMap.put(key, TYPE_HEALTH);
                } catch (Exception ignored) {
                }
            }
        }
        buildCalendar();
    }

    public void setDateMarkersWithTypes(Map<String, Integer> typeMap) {
        this.dateTypeMap.clear();
        this.dateSummaryMap.clear();
        if (typeMap != null) {
            for (Map.Entry<String, Integer> e : typeMap.entrySet()) {
                try {
                    long key = DATE_KEY_FMT.parse(e.getKey()).getTime();
                    dateTypeMap.put(key, e.getValue());
                } catch (Exception ignored) {
                }
            }
        }
        buildCalendar();
    }

    public void setDaySummaries(Map<String, List<String>> summaryMap) {
        this.dateSummaryMap.clear();
        if (summaryMap != null) {
            for (Map.Entry<String, List<String>> e : summaryMap.entrySet()) {
                try {
                    long key = DATE_KEY_FMT.parse(e.getKey()).getTime();
                    dateSummaryMap.put(key, e.getValue());
                } catch (Exception ignored) {
                }
            }
        }
        buildCalendar();
    }

    public void refresh() {
        buildCalendar();
    }
}
