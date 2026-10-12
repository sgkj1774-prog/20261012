package com.example.healthrecorder.event;

import java.util.Date;

public class Event {
    private int id;
    private String title;
    private String content;
    private Date eventTime;
    private Date remindTime;
    private boolean remindEnabled;
    private int priority;     // 1=低 2=中 3=高
    private int status;        // 0=待办 1=已完成 2=已取消
    private int repeatType;    // 0=不重复 1=每天 2=每周 3=每月 4=每年
    private int repeatInterval;
    private Date createdAt;
    private int userId = 1;
    private String userName;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Date getEventTime() { return eventTime; }
    public void setEventTime(Date eventTime) { this.eventTime = eventTime; }
    public Date getRemindTime() { return remindTime; }
    public void setRemindTime(Date remindTime) { this.remindTime = remindTime; }
    public boolean isRemindEnabled() { return remindEnabled; }
    public void setRemindEnabled(boolean remindEnabled) { this.remindEnabled = remindEnabled; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public int getRepeatType() { return repeatType; }
    public void setRepeatType(int repeatType) { this.repeatType = repeatType; }
    public int getRepeatInterval() { return repeatInterval; }
    public void setRepeatInterval(int repeatInterval) { this.repeatInterval = repeatInterval; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPriorityText() {
        String[] arr = {"", "低", "中", "高"};
        if (priority < 1 || priority >= arr.length) return "中";
        return arr[priority];
    }

    public String getStatusText() {
        String[] arr = {"待办", "已完成", "已取消"};
        if (status < 0 || status >= arr.length) return "待办";
        return arr[status];
    }

    public String getRepeatText() {
        String[] arr = {"不重复", "每天", "每周", "每月", "每年"};
        if (repeatType < 0 || repeatType >= arr.length) return "不重复";
        return arr[repeatType];
    }

    public static String[] repeatTextArr() {
        return new String[]{"不重复", "每天", "每周", "每月", "每年"};
    }
}
