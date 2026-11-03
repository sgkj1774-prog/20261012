package com.example.healthrecorder.medication;

import java.util.Date;

/**
 * 药品档案：一种药一条记录，描述「吃什么、吃多少、什么时候吃」。
 */
public class Medication {

    public static final int TIMING_MORNING = 0;
    public static final int TIMING_NOON    = 1;
    public static final int TIMING_EVENING = 2;
    public static final int TIMING_BEDTIME = 3;

    private static final String[] TIMING_TEXT = {"早晨", "中午", "晚上", "睡前"};

    private int id;
    private String name;        // 药品名，如「优甲乐」
    private String dosage;      // 剂量，如「50μg」「1片」
    private int timing;         // 服用时段
    private String timeText;    // 具体时间 HH:mm，可空
    private String notes;       // 备注，如「空腹服用」
    private boolean active = true;
    private Date createdAt;
    private int userId = 1;
    private String userName;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public int getTiming() { return timing; }
    public void setTiming(int timing) { this.timing = timing; }

    public String getTimeText() { return timeText; }
    public void setTimeText(String timeText) { this.timeText = timeText; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    /** 时段文字，用于导出与显示 */
    public static String timingText(int timing) {
        if (timing < 0 || timing >= TIMING_TEXT.length) return TIMING_TEXT[0];
        return TIMING_TEXT[timing];
    }

    /** 由文字反解时段（导入用），解析失败按早晨处理 */
    public static int parseTiming(String text) {
        if (text == null) return TIMING_MORNING;
        String t = text.trim();
        for (int i = 0; i < TIMING_TEXT.length; i++) {
            if (TIMING_TEXT[i].equals(t)) return i;
        }
        if (t.contains("早")) return TIMING_MORNING;
        if (t.contains("午")) return TIMING_NOON;
        if (t.contains("晚")) return TIMING_EVENING;
        if (t.contains("睡")) return TIMING_BEDTIME;
        return TIMING_MORNING;
    }

    /** 「早晨 08:00」这样的调度描述 */
    public String getScheduleText() {
        String base = timingText(timing);
        if (timeText != null && !timeText.trim().isEmpty()) {
            return base + " " + timeText.trim();
        }
        return base;
    }
}
