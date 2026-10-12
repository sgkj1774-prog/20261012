package com.example.healthrecorder.teeth;

import java.util.Date;

/** 刷牙记录 */
public class TeethRecord {
    private int id;
    private Date date;
    private int userId;
    private String userName;
    private int period;        // 0=晨间, 1=晚间
    private int duration;      // 秒
    private String notes;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    /** @return 0=晨间, 1=晚间 */
    public int getPeriod() { return period; }
    public void setPeriod(int period) { this.period = period; }

    public String getPeriodLabel() {
        return period == 0 ? "🌅 晨间" : "🌙 晚间";
    }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    /** @return 格式化时长（mm:ss） */
    public String getDurationFormatted() {
        if (duration <= 0) return "—";
        int m = duration / 60, s = duration % 60;
        return String.format("%d:%02d", m, s);
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
