package com.example.healthrecorder.cat;

import java.util.Date;

/** 猫咪如厕记录 */
public class CatRecord {
    private int id;
    private Date date;
    private String catName;
    private int stoolType;   // 1-5 Bristol粪便评分
    private int stoolCount;  // 粪便数量（颗）
    private int willingness;   // 如厕意愿: 0=好, 1=中, 2=差
    private int processType;   // 如厕过程: 0=好, 1=中, 2=差
    private String stoolScore; // 文字描述
    private int frequency;    // 当日次数
    private String notes;
    private int userId;
    private String userName;
    private int userColor; // 成员颜色索引 0-4

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getCatName() { return catName; }
    public void setCatName(String catName) { this.catName = catName; }

    /** @return 1-5 粪便评分（1=便秘，3=正常，5=腹泻） */
    public int getStoolType() { return stoolType; }
    public void setStoolType(int stoolType) { this.stoolType = stoolType; }

    /** @return 粪便数量（颗） */
    public int getStoolCount() { return stoolCount; }
    public void setStoolCount(int stoolCount) { this.stoolCount = stoolCount; }

    /** @return 如厕意愿 0=好 1=中 2=差 */
    public int getWillingness() { return willingness; }
    public void setWillingness(int w) { this.willingness = w; }
    public String getWillingnessLabel() {
        switch (willingness) {
            case 1: return "意愿·中";
            case 2: return "意愿·差";
            default: return "意愿·好";
        }
    }

    public void setWillingnessFromString(String s) {
        if (s.contains("差")) setWillingness(2);
        else if (s.contains("中")) setWillingness(1);
        else setWillingness(0);
    }

    /** @return 如厕过程 0=好 1=中 2=差 */
    public int getProcessType() { return processType; }
    public void setProcessType(int p) { this.processType = p; }
    public String getProcessTypeLabel() {
        switch (processType) {
            case 1: return "过程·中";
            case 2: return "过程·差";
            default: return "过程·好";
        }
    }

    public void setProcessTypeFromString(String s) {
        if (s.contains("差")) setProcessType(2);
        else if (s.contains("中")) setProcessType(1);
        else setProcessType(0);
    }

    /** @return 粪便评分文字描述 */
    public String getStoolScore() {
        if (stoolScore != null && !stoolScore.isEmpty()) return stoolScore;
        switch (stoolType) {
            case 1: return "便秘（硬、干）";
            case 2: return "略干";
            case 3: return "正常 ✅";
            case 4: return "略软";
            case 5: return "腹泻 ⚠️";
            default: return "未知";
        }
    }
    public void setStoolScore(String stoolScore) { this.stoolScore = stoolScore; }

    public int getFrequency() { return frequency; }
    public void setFrequency(int frequency) { this.frequency = frequency; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public int getUserColor() { return userColor; }
    public void setUserColor(int userColor) { this.userColor = userColor; }
    public String getUserColorHex() {
        switch (userColor) {
            case 1: return "#2196F3";
            case 2: return "#FF9800";
            case 3: return "#9C27B0";
            case 4: return "#F44336";
            default: return "#4CAF50";
        }
    }

    /** 评分颜色：1红 2橙 3绿 4黄 5红 */
    public int getScoreColor() {
        switch (stoolType) {
            case 1: return 0xFFE53935; // 红（便秘）
            case 2: return 0xFFFF9800; // 橙
            case 3: return 0xFF4CAF50; // 绿（正常）
            case 4: return 0xFFFFEB3B; // 黄
            case 5: return 0xFFE53935; // 红（腹泻）
            default: return 0xFF9E9E9E;
        }
    }
}
