package com.example.healthrecorder;

public class User {
    private int id;
    private String name;
    private int color; // 0=绿,1=蓝,2=橙,3=紫,4=红

    public User() {}
    public User(String name, int color) { this.name = name; this.color = color; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public String getColorHex() {
        switch(color) {
            case 1: return "#2196F3";
            case 2: return "#FF9800";
            case 3: return "#9C27B0";
            case 4: return "#F44336";
            default: return "#4CAF50";
        }
    }
}
