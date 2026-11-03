package com.example.healthrecorder.medication;

import java.util.Date;

/**
 * 每日服药打卡：一种药每天最多一条，记录「吃没吃」。
 */
public class MedicationLog {

    private int id;
    private int medId;
    private String medName;     // 便于导出/显示，不入库
    private String logDate;     // yyyy-MM-dd
    private boolean taken;
    private Date takenAt;       // 实际服用时间，可空
    private String notes;
    private int userId = 1;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMedId() { return medId; }
    public void setMedId(int medId) { this.medId = medId; }

    public String getMedName() { return medName; }
    public void setMedName(String medName) { this.medName = medName; }

    public String getLogDate() { return logDate; }
    public void setLogDate(String logDate) { this.logDate = logDate; }

    public boolean isTaken() { return taken; }
    public void setTaken(boolean taken) { this.taken = taken; }

    public Date getTakenAt() { return takenAt; }
    public void setTakenAt(Date takenAt) { this.takenAt = takenAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}
