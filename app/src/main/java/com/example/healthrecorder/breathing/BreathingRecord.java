package com.example.healthrecorder.breathing;

import java.util.Date;

public class BreathingRecord {
    private int id;
    private Date date;
    private int duration; // 秒
    private int score;     // 0-10
    private int mode;      // 0=腹式呼吸 1=胸式呼吸 2=478呼吸 3=箱式呼吸 4=自定义
    private String notes;
    private int userId = 1;
    private String userName;
    private String customContent;   // 自定义呼吸模式内容（如"吸气4秒→屏气7秒→呼气8秒"）
    private int customDuration;      // 自定义时长(秒)，仅mode=4时使用

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getMode() { return mode; }
    public void setMode(int mode) { this.mode = mode; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getCustomContent() { return customContent; }
    public void setCustomContent(String customContent) { this.customContent = customContent; }
    public int getCustomDuration() { return customDuration; }
    public void setCustomDuration(int customDuration) { this.customDuration = customDuration; }

    public String getModeText() {
        String[] modes = {"腹式呼吸", "胸式呼吸", "478呼吸", "箱式呼吸", "自定义"};
        if (mode < 0 || mode >= modes.length) return "自定义";
        // 自定义模式时优先显示自定义内容
        if (mode == 4 && customContent != null && !customContent.trim().isEmpty()) {
            return customContent;
        }
        return modes[mode];
    }
}
