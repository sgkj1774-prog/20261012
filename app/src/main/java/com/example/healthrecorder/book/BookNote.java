package com.example.healthrecorder.book;

import java.util.Date;

/**
 * 读后感明细：一本书可有多条，每条对应一个页面区间。
 * 与 BookRecord.notes（整本书的读后感）并存，互不影响。
 */
public class BookNote {
    private int id;
    private int bookId;
    private int startPage;
    private int endPage;
    private String content;
    private Date createdAt;
    private int userId = 1;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }
    public int getStartPage() { return startPage; }
    public void setStartPage(int startPage) { this.startPage = startPage; }
    public int getEndPage() { return endPage; }
    public void setEndPage(int endPage) { this.endPage = endPage; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    /** 页面区间展示文本，如 "P12-30"；只填一页时显示 "P12" */
    public String getRangeText() {
        if (startPage > 0 && endPage > 0 && endPage != startPage) {
            return "P" + startPage + "-" + endPage;
        }
        if (startPage > 0) return "P" + startPage;
        if (endPage > 0) return "P" + endPage;
        return "未标注页码";
    }
}
