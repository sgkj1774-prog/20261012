package com.example.healthrecorder.cat;

import java.util.Date;

/**
 * 猫粮记录
 * end_date 为 null 表示该猫粮当前正在使用中（持续中）
 */
public class CatFoodRecord {
    private int id;
    private String brand;      // 品牌型号
    private String foodType;   // 类型: 干粮/湿粮/罐头/冻干/零食
    private Date startDate;    // 开始日期
    private Date endDate;      // 结束日期（null = 持续中）
    private String notes;      // 备注
    private int userId = 1;
    private String userName;
    private int userColor;     // 成员颜色索引 0-4

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public int getUserColor() { return userColor; }
    public void setUserColor(int userColor) { this.userColor = userColor; }

    /** @return 是否正在使用中（end_date 为 null） */
    public boolean isOngoing() { return endDate == null; }

    /**
     * @return 持续天数：
     * 持续中 = 开始日期到今天；已结束 = 开始日期到结束日期。
     * 不足一天按 1 天计。
     */
    public int durationDays() {
        if (startDate == null) return 0;
        long end = (endDate != null) ? endDate.getTime() : System.currentTimeMillis();
        long diff = end - startDate.getTime();
        if (diff < 0) return 0;
        int days = (int) (diff / (24L * 60 * 60 * 1000));
        return Math.max(days, 1);
    }

    /** @return 类型标签颜色 */
    public int getTypeColor() {
        if (foodType == null) return 0xFF9E9E9E;
        switch (foodType) {
            case "干粮": return 0xFF795548; // 棕
            case "湿粮": return 0xFF2196F3; // 蓝
            case "罐头": return 0xFFFF9800; // 橙
            case "冻干": return 0xFF9C27B0; // 紫
            case "零食": return 0xFFE91E63; // 粉
            default: return 0xFF9E9E9E;
        }
    }
}
