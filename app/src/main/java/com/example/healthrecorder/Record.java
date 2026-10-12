package com.example.healthrecorder;

import java.util.Date;

public class Record {
    private int id;
    private float weight;
    private int systolic;
    private int diastolic;
    private String notes;
    private Date date;
    private int userId = 1;
    private String userName = "本人";

    public Record() {}

    public Record(float weight, int systolic, int diastolic, String notes, Date date) {
        this.weight = weight;
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.notes = notes;
        this.date = date;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public float getWeight() { return weight; }
    public void setWeight(float weight) { this.weight = weight; }
    public int getSystolic() { return systolic; }
    public void setSystolic(int systolic) { this.systolic = systolic; }
    public int getDiastolic() { return diastolic; }
    public void setDiastolic(int diastolic) { this.diastolic = diastolic; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getBloodPressure() {
        if (systolic <= 0 && diastolic <= 0) return "—";
        return systolic + "/" + diastolic + " mmHg";
    }
    public boolean hasWeight() { return weight > 0; }
    public boolean hasBloodPressure() { return systolic > 0 || diastolic > 0; }
}
