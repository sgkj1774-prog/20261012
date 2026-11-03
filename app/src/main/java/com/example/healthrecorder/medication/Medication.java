package com.example.healthrecorder.medication;

import java.util.Date;

/**
 * 药品档案：一种药一条记录，描述「吃什么、吃多少、什么时候吃」。
 */
public class Medication {

    public static final int TIMING_MORNING = 0;
    public static final int TIMING_NOON    = 1;
    public static final int TIMING_EVENING = 2;
    public static final int TIMING_BEDTIME = 3;

    private static final String[] TIMING_TEXT = {"早晨", "中午", "晚上", "睡前"};

    private int id;
    private String name;        // 药品名，如「优甲乐」
    private String dosage;      // 剂量，如「50μg」「1片」
    private int timing;         // 服用时段
    private String timeText;    // 具体时间 HH:mm，可空
    private String notes;       // 备注，如「空腹服用」
    private boolean active = true;
    private Date createdAt;
    private int userId = 1;
    private String userName;

    // ── 药品包装信息（v14），全部可选，日期统一存 yyyy-MM-dd ──
    private String productionDate;  // 生产日期
    private String expiryDate;      // 有效期至
    private String spec;            // 规格，如「50μg×100片」
    private String approvalNo;      // 批准文号，如「国药准字H20100523」
    private String mah;             // 上市许可持有人
    private String manufacturer;    // 生产厂
    private String packer;          // 包装厂
    private String companyAddress;  // 企业地址

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public int getTiming() { return timing; }
    public void setTiming(int timing) { this.timing = timing; }

    public String getTimeText() { return timeText; }
    public void setTimeText(String timeText) { this.timeText = timeText; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getProductionDate() { return productionDate; }
    public void setProductionDate(String productionDate) { this.productionDate = productionDate; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public String getApprovalNo() { return approvalNo; }
    public void setApprovalNo(String approvalNo) { this.approvalNo = approvalNo; }

    public String getMah() { return mah; }
    public void setMah(String mah) { this.mah = mah; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getPacker() { return packer; }
    public void setPacker(String packer) { this.packer = packer; }

    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }

    /** 时段文字，用于导出与显示 */
    public static String timingText(int timing) {
        if (timing < 0 || timing >= TIMING_TEXT.length) return TIMING_TEXT[0];
        return TIMING_TEXT[timing];
    }

    /** 由文字反解时段（导入用），解析失败按早晨处理 */
    public static int parseTiming(String text) {
        if (text == null) return TIMING_MORNING;
        String t = text.trim();
        for (int i = 0; i < TIMING_TEXT.length; i++) {
            if (TIMING_TEXT[i].equals(t)) return i;
        }
        if (t.contains("早")) return TIMING_MORNING;
        if (t.contains("午")) return TIMING_NOON;
        if (t.contains("晚")) return TIMING_EVENING;
        if (t.contains("睡")) return TIMING_BEDTIME;
        return TIMING_MORNING;
    }

    /** 「早晨 08:00」这样的调度描述 */
    public String getScheduleText() {
        String base = timingText(timing);
        if (timeText != null && !timeText.trim().isEmpty()) {
            return base + " " + timeText.trim();
        }
        return base;
    }

    // ============================================================
    // 日期与有效期
    // ============================================================

    /** 药品信息日期统一格式 */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /** 有效期状态：未填 / 正常 / 临期（30 天内）/ 已过期 */
    public static final int EXPIRY_NONE = 0;
    public static final int EXPIRY_VALID = 1;
    public static final int EXPIRY_SOON = 2;
    public static final int EXPIRY_EXPIRED = 3;

    /** 生成 yyyy-MM-dd */
    public static String formatDate(int year, int month, int day) {
        return String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day);
    }

    /**
     * 解析 yyyy-MM-dd（也接受 yyyy/MM/dd、yyyy-MM、yyyy年MM月），失败返回 null。
     * 只有年月时按当月最后一天算，避免「有效期 2027-08」被当成 8 月 1 日提前过期。
     */
    public static java.util.Calendar parseDate(String text) {
        if (text == null) return null;
        String s = text.trim().replace('/', '-').replace('年', '-').replace('月', '-').replace("日", "");
        if (s.isEmpty()) return null;
        try {
            String[] p = s.split("-");
            if (p.length < 2) return null;
            int y = Integer.parseInt(p[0].trim());
            int mo = Integer.parseInt(p[1].trim());
            if (mo < 1 || mo > 12) return null;
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.clear();
            if (p.length >= 3 && !p[2].trim().isEmpty()) {
                c.set(y, mo - 1, Integer.parseInt(p[2].trim()));
            } else {
                c.set(y, mo - 1, 1);
                c.set(java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
            }
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    /** 有效期状态，供列表打标签 */
    public int getExpiryState() {
        java.util.Calendar exp = parseDate(expiryDate);
        if (exp == null) return EXPIRY_NONE;
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);
        if (exp.before(today)) return EXPIRY_EXPIRED;
        long days = (exp.getTimeInMillis() - today.getTimeInMillis()) / (24L * 3600 * 1000);
        return days <= 30 ? EXPIRY_SOON : EXPIRY_VALID;
    }

    /** 「有效期至 2027-08-31 · 还剩 45 天」这类摘要，未填返回 null */
    public String getExpirySummary() {
        if (expiryDate == null || expiryDate.trim().isEmpty()) return null;
        int state = getExpiryState();
        if (state == EXPIRY_EXPIRED) return "有效期至 " + expiryDate + "（已过期）";
        if (state == EXPIRY_SOON) return "有效期至 " + expiryDate + "（临期）";
        return "有效期至 " + expiryDate;
    }

    /** 「规格 · 批准文号」摘要，都为空返回 null */
    public String getSpecSummary() {
        StringBuilder sb = new StringBuilder();
        if (spec != null && !spec.trim().isEmpty()) sb.append(spec.trim());
        if (approvalNo != null && !approvalNo.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(approvalNo.trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
