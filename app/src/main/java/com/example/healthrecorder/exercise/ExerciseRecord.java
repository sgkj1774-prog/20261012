package com.example.healthrecorder.exercise;

import java.util.Date;

/** 运动记录 */
public class ExerciseRecord {
    public static final int TYPE_RUNNING = 0;
    public static final int TYPE_HIKING  = 1;

    private int id;
    private Date date;
    private int userId;
    private String userName;
    private int type;          // 0=跑步, 1=徒步
    private float distance;    // 公里
    private int duration;      // 分钟
    private float pace;        // 分钟/公里
    private int heartRate;     // bpm
    private int calories;      // kcal
    private int elevationGain; // 米（徒步用）
    private int difficulty;    // 1-5 难度
    private String notes;
    private String location;   // 运动地点

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    /** @return TYPE_RUNNING / TYPE_HIKING */
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public String getTypeLabel() {
        return type == TYPE_RUNNING ? "🏃 跑步" : "🥾 徒步";
    }

    public String getTypeString() {
        return type == TYPE_RUNNING ? "跑步" : "徒步";
    }

    public void setTypeFromString(String s) {
        if (s.contains("徒步")) type = TYPE_HIKING;
        else type = TYPE_RUNNING;
    }

    public float getDistance() { return distance; }
    public void setDistance(float distance) { this.distance = distance; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    /** @return 分钟/公里，自动计算 */
    public float getPace() {
        if (pace > 0) return pace;
        if (distance > 0 && duration > 0) return duration / distance;
        return 0;
    }

    public String getPaceFormatted() {
        float p = getPace();
        if (p <= 0) return "—";
        int m = (int) p, s = Math.round((p - m) * 60);
        return String.format("%d'%02d\"/km", m, s);
    }

    /** @return 距离(km) + 时长(分钟) 自动计算卡路里 */
    public int getCalories() {
        if (calories > 0) return calories;
        if (distance <= 0) return 0;
        // 粗略估算：跑步 ~60kcal/km，徒步 ~40kcal/km
        float cal = type == TYPE_RUNNING ? distance * 60f : distance * 40f;
        return Math.round(cal);
    }

    public void setPace(float pace) { this.pace = pace; }
    public void setHeartRate(int heartRate) { this.heartRate = heartRate; }
    public void setCalories(int calories) { this.calories = calories; }
    public int getHeartRate() { return heartRate; }
    public int getCaloriesField() { return calories; }

    public int getElevationGain() { return elevationGain; }
    public void setElevationGain(int elevationGain) { this.elevationGain = elevationGain; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public String getDifficultyLabel() {
        switch (difficulty) {
            case 1: return "⭐ 轻松";
            case 2: return "⭐⭐ 较轻松";
            case 3: return "⭐⭐⭐ 中等";
            case 4: return "⭐⭐⭐⭐ 较累";
            case 5: return "⭐⭐⭐⭐⭐ 极限";
            default: return "未知";
        }
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
