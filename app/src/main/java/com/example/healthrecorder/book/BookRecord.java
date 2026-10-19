package com.example.healthrecorder.book;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BookRecord {
    private int id;
    private String title;
    private String author;
    private int totalPages;
    private int currentPage;
    private int status;     // 0=想读 1=在读 2=已读
    private int rating;      // 0-5 星
    private String notes;   // 读后感
    private Date createdAt;
    private Date finishedAt;
    private int userId = 1;
    private String userName;
    private int finishedPage;      // 归档时的页数（读完时固化）
    private String finishedNotes;   // 归档时的读后感（读完时固化）
    private Date startAt;           // 开始阅读时间（v12 新增，精确到时分）
    /**
     * 仅用于导入过程的暂存：解析阶段把读后感明细挂在这里，
     * 等书籍插入拿到真实 id 后再落库。不参与持久化。
     */
    private List<BookNote> pendingNotes = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Date finishedAt) { this.finishedAt = finishedAt; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public int getFinishedPage() { return finishedPage; }
    public void setFinishedPage(int finishedPage) { this.finishedPage = finishedPage; }
    public String getFinishedNotes() { return finishedNotes; }
    public void setFinishedNotes(String finishedNotes) { this.finishedNotes = finishedNotes; }
    public Date getStartAt() { return startAt; }
    public void setStartAt(Date startAt) { this.startAt = startAt; }
    public List<BookNote> getPendingNotes() { return pendingNotes; }
    public void addPendingNote(BookNote note) { if (note != null) pendingNotes.add(note); }

    public String getStatusText() {
        if (status == 1) return "在读";
        if (status == 2) return "已读";
        return "想读";
    }

    public float getProgress() {
        if (totalPages <= 0) return 0f;
        return (float) currentPage / totalPages;
    }
}
