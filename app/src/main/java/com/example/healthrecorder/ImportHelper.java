package com.example.healthrecorder;

import com.example.healthrecorder.breathing.BreathingRecord;
import com.example.healthrecorder.cat.CatRecord;
import com.example.healthrecorder.cat.CatFoodRecord;
import com.example.healthrecorder.event.Event;
import com.example.healthrecorder.book.BookRecord;
import com.example.healthrecorder.teeth.TeethRecord;
import com.example.healthrecorder.exercise.ExerciseRecord;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 导入工具：解析 CSV / Excel(XLSX) 备份文件，还原全部数据。
 *
 * 文件格式（V1.2）支持六种分段：
 *   === 人员配置 ===
 *   === 健康记录 ===
 *   === 猫咪如厕记录 ===
 *   === 刷牙记录 ===
 *   === 运动记录 ===
 *   === 呼吸打卡记录 ===
 *   === 重要事项记录 ===
 *   === 读书笔记记录 ===
 *   === 猫粮记录 ===
 *
 * 向前兼容 V1.1/V1.0 旧格式（只有人员+健康记录）。
 */
public class ImportHelper {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public static class ImportResult {
        public final List<Record> records;
        public final List<User> users;
        public final List<CatRecord> catRecords;
        public final List<TeethRecord> teethRecords;
        public final List<ExerciseRecord> exerciseRecords;
        public final List<BreathingRecord> breathingRecords;
        public final List<Event> events;
        public final List<BookRecord> bookRecords;
        public final List<CatFoodRecord> catFoodRecords;
        public final int totalLines;
        public final int skippedLines;
        public final String errorMessage;

        public ImportResult(List<Record> records, List<User> users,
                            List<CatRecord> catRecords, List<TeethRecord> teethRecords, List<ExerciseRecord> exerciseRecords,
                            List<BreathingRecord> breathingRecords, List<Event> events, List<BookRecord> bookRecords, List<CatFoodRecord> catFoodRecords,
                            int totalLines, int skippedLines, String errorMessage) {
            this.records = records;
            this.users = users;
            this.catRecords = catRecords;
            this.teethRecords = teethRecords;
            this.exerciseRecords = exerciseRecords;
            this.breathingRecords = breathingRecords;
            this.events = events;
            this.bookRecords = bookRecords;
            this.catFoodRecords = catFoodRecords;
            this.totalLines = totalLines;
            this.skippedLines = skippedLines;
            this.errorMessage = errorMessage;
        }

        public boolean hasError() {
            return errorMessage != null && !errorMessage.isEmpty();
        }
    }

    public static ImportResult parseFile(InputStream inputStream, String fileName) throws Exception {
        if (fileName != null && fileName.toLowerCase(Locale.US).endsWith(".xlsx")) {
            return parseExcel(inputStream);
        }
        return parseCsv(inputStream);
    }

    // ============================ CSV ============================
    public static ImportResult parseCsv(InputStream inputStream) throws Exception {
        List<Record> records = new ArrayList<>();
        List<User> users = new ArrayList<>();
        List<CatRecord> catRecords = new ArrayList<>();
        List<TeethRecord> teethRecords = new ArrayList<>();
        List<ExerciseRecord> exerciseRecords = new ArrayList<>();
        List<BreathingRecord> breathingRecords = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        List<BookRecord> bookRecords = new ArrayList<>();
        List<CatFoodRecord> catFoodRecords = new ArrayList<>();
        List<NoteRow> noteRows = new ArrayList<>();
        int totalLines = 0;
        int skippedLines = 0;
        String currentSection = "records"; // 默认兼容旧格式
        boolean headerSeen = false;

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String rawLine;
        boolean first = true;
        // 某些字段（如重要事项内容）本身含换行，CSV 里会被引号包裹跨多行。
        // 这里按引号奇偶判断是否仍在同一条记录内，保证整条记录被完整读入。
        StringBuilder multi = null;
        while ((rawLine = reader.readLine()) != null) {
            totalLines++;
            if (first) { first = false; rawLine = stripBom(rawLine); }

            String line;
            if (multi != null) {
                multi.append('\n').append(rawLine);
                if (countQuotes(multi) % 2 != 0) continue;   // 引号未闭合，继续拼接
                line = multi.toString();
                multi = null;
            } else if (countQuotes(rawLine) % 2 != 0) {
                multi = new StringBuilder(rawLine);
                continue;
            } else {
                line = rawLine;
            }

            String trimmed = line.trim();
            if (trimmed.isEmpty()) { continue; }

            // 分段标记
            if (trimmed.startsWith("===") && trimmed.endsWith("===")) {
                if (trimmed.contains("人员配置")) { currentSection = "users"; headerSeen = false; }
                else if (trimmed.contains("健康记录")) { currentSection = "records"; headerSeen = false; }
                else if (trimmed.contains("猫咪如厕记录")) { currentSection = "cat"; headerSeen = false; }
                else if (trimmed.contains("刷牙记录")) { currentSection = "teeth"; headerSeen = false; }
                else if (trimmed.contains("运动记录")) { currentSection = "exercise"; headerSeen = false; }
                else if (trimmed.contains("呼吸打卡记录")) { currentSection = "breathing"; headerSeen = false; }
                else if (trimmed.contains("重要事项记录")) { currentSection = "event"; headerSeen = false; }
                else if (trimmed.contains("读书笔记记录")) { currentSection = "book"; headerSeen = false; }
                else if (trimmed.contains("读后感明细")) { currentSection = "bookNote"; headerSeen = false; }
                else if (trimmed.contains("猫粮记录")) { currentSection = "catFood"; headerSeen = false; }
                continue;
            }

            // 跳过表头
            if (!headerSeen) {
                headerSeen = true;
                if (trimmed.startsWith("ID") || trimmed.startsWith("日期") || trimmed.startsWith("书名")) { continue; }
            }

            // 解析行
            try {
                switch (currentSection) {
                    case "users":
                        User u = parseUserLine(line);
                        if (u != null) users.add(u); else skippedLines++;
                        break;
                    case "records":
                        Record r = parseCsvLine(line);
                        if (r != null) records.add(r); else skippedLines++;
                        break;
                    case "cat":
                        CatRecord c = parseCatLine(line);
                        if (c != null) catRecords.add(c); else skippedLines++;
                        break;
                    case "teeth":
                        TeethRecord t = parseTeethLine(line);
                        if (t != null) teethRecords.add(t); else skippedLines++;
                        break;
                    case "exercise":
                        ExerciseRecord ex = parseExerciseLine(line);
                        if (ex != null) exerciseRecords.add(ex); else skippedLines++;
                        break;
                    case "breathing":
                        BreathingRecord br = parseBreathingLine(line);
                        if (br != null) breathingRecords.add(br); else skippedLines++;
                        break;
                    case "event":
                        Event ev = parseEventLine(line);
                        if (ev != null) events.add(ev); else skippedLines++;
                        break;
                    case "book":
                        BookRecord b = parseBookLine(line);
                        if (b != null) bookRecords.add(b); else skippedLines++;
                        break;
                    case "catFood":
                        CatFoodRecord cf = parseCatFoodLine(line);
                        if (cf != null) catFoodRecords.add(cf); else skippedLines++;
                        break;
                    case "bookNote":
                        NoteRow nr = parseNoteLine(line);
                        if (nr != null) noteRows.add(nr); else skippedLines++;
                        break;
                }
            } catch (Exception e) {
                skippedLines++;
            }
        }
        reader.close();
        attachNoteRows(bookRecords, noteRows);
        return new ImportResult(records, users, catRecords, teethRecords, exerciseRecords,
                breathingRecords, events, bookRecords, catFoodRecords,
                totalLines, skippedLines, null);
    }

    private static User parseUserLine(String rawLine) {
        List<String> f = splitCsvLine(rawLine);
        if (f.size() < 3) return null;
        try {
            User u = new User();
            u.setId(Integer.parseInt(f.get(0).trim()));
            u.setName(f.get(1).trim());
            u.setColor(Integer.parseInt(f.get(2).trim()));
            if (u.getName().isEmpty()) return null;
            return u;
        } catch (Exception e) {
            return null;
        }
    }

    private static Record parseCsvLine(String rawLine) {
        List<String> fields = splitCsvLine(rawLine);
        if (fields.size() < 6) return null;
        try {
            String dateStr  = fields.get(0).trim();
            String userName = fields.get(1).trim();
            String weightStr = fields.get(2).trim();
            String sysStr   = fields.get(3).trim();
            String diaStr   = fields.get(4).trim();
            String notes    = fields.size() > 5 ? fields.get(5).trim() : "";

            Date date = parseDate(dateStr);
            if (date == null) return null;

            float weight = 0f;
            if (!weightStr.isEmpty()) { try { weight = Float.parseFloat(weightStr); } catch (NumberFormatException ignored) {} }
            int sys = 0, dia = 0;
            if (!sysStr.isEmpty()) { try { sys = Integer.parseInt(sysStr); } catch (NumberFormatException ignored) {} }
            if (!diaStr.isEmpty()) { try { dia = Integer.parseInt(diaStr); } catch (NumberFormatException ignored) {} }

            Record r = new Record();
            r.setDate(date);
            r.setUserName(userName.isEmpty() ? "本人" : userName);
            r.setWeight(weight);
            r.setSystolic(sys);
            r.setDiastolic(dia);
            r.setNotes(notes.isEmpty() ? null : notes);
            r.setUserId(0);
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    private static CatRecord parseCatLine(String rawLine) {
        List<String> f = splitCsvLine(rawLine);
        if (f.size() < 5) return null;
        try {
            Date date = parseDate(f.get(0).trim());
            if (date == null) return null;
            CatRecord c = new CatRecord();
            c.setDate(date);
            c.setCatName(f.get(1).trim());
            c.setStoolType(Integer.parseInt(f.get(2).trim()));
            c.setStoolCount(Integer.parseInt(f.get(3).trim()));
            // 向前兼容：新格式有如厕意愿/过程字段（第5、6列），旧格式无则跳过
            if (f.size() > 5) c.setWillingnessFromString(f.get(4).trim());
            if (f.size() > 6) c.setProcessTypeFromString(f.get(5).trim());
            // 备注列位置：新格式第7列，旧格式第5列
            int notesIdx = f.size() > 6 ? 6 : 4;
            c.setNotes(f.get(notesIdx).trim().isEmpty() ? null : f.get(notesIdx).trim());
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private static TeethRecord parseTeethLine(String rawLine) {
        List<String> f = splitCsvLine(rawLine);
        if (f.size() < 2) return null;
        try {
            Date date = parseDate(f.get(0).trim());
            if (date == null) return null;
            TeethRecord t = new TeethRecord();
            t.setDate(date);
            t.setUserName(f.get(1).trim());
            // 向前兼容：旧格式有5列（时段/时长），新格式只有3列（日期/成员/备注）
            if (f.size() >= 5) {
                String periodStr = f.get(2).trim();
                t.setPeriod(periodStr.equals("晚上") ? 1 : 0);
                t.setDuration(Integer.parseInt(f.get(3).trim()));
                t.setNotes(f.get(4).trim().isEmpty() ? null : f.get(4).trim());
            } else if (f.size() >= 3) {
                t.setNotes(f.get(2).trim().isEmpty() ? null : f.get(2).trim());
            }
            return t;
        } catch (Exception e) {
            return null;
        }
    }

    private static ExerciseRecord parseExerciseLine(String rawLine) {
        List<String> f = splitCsvLine(rawLine);
        if (f.size() < 11) return null;
        try {
            Date date = parseDate(f.get(0).trim());
            if (date == null) return null;
            ExerciseRecord e = new ExerciseRecord();
            e.setDate(date);
            e.setUserName(f.get(1).trim());
            e.setTypeFromString(f.get(2).trim());
            e.setDistance(Float.parseFloat(f.get(3).trim()));
            e.setDuration(Integer.parseInt(f.get(4).trim()));
            e.setPace(Float.parseFloat(f.get(5).trim()));
            e.setHeartRate(Integer.parseInt(f.get(6).trim()));
            e.setCalories(Integer.parseInt(f.get(7).trim()));
            e.setElevationGain(Integer.parseInt(f.get(8).trim()));
            e.setDifficulty(Integer.parseInt(f.get(9).trim()));
            // 新格式：地点在第11列(索引10)，备注在第12列(索引11)
            // 旧格式：备注在第11列(索引10)，无地点
            if (f.size() >= 12) {
                e.setLocation(f.get(10).trim().isEmpty() ? null : f.get(10).trim());
                e.setNotes(f.get(11).trim().isEmpty() ? null : f.get(11).trim());
            } else {
                e.setNotes(f.get(10).trim().isEmpty() ? null : f.get(10).trim());
            }
            return e;
        } catch (Exception ex) {
            return null;
        }
    }

    // 呼吸打卡 CSV 格式：日期,成员,类型,时长(秒),评分,自定义内容,自定义时长(秒),备注
    private static BreathingRecord parseBreathingLine(String rawLine) {
        List<String> f = splitCsvLine(rawLine);
        if (f.size() < 5) return null;
        try {
            Date date = parseDate(f.get(0).trim());
            if (date == null) return null;
            BreathingRecord br = new BreathingRecord();
            br.setDate(date);
            br.setUserName(f.get(1).trim());
            try { br.setMode(Integer.parseInt(f.get(2).trim())); } catch (NumberFormatException ignored) {}
            br.setDuration(Integer.parseInt(f.get(3).trim()));
            br.setScore(Integer.parseInt(f.get(4).trim()));
            if (f.size() > 5) br.setCustomContent(f.get(5).trim().isEmpty() ? null : f.get(5).trim());
            if (f.size() > 6) {
                String cd = f.get(6).trim();
                if (!cd.isEmpty()) br.setCustomDuration(Integer.parseInt(cd));
            }
            if (f.size() > 7) br.setNotes(f.get(7).trim().isEmpty() ? null : f.get(7).trim());
            return br;
        } catch (Exception e) {
            return null;
        }
    }

    // 重要事项 CSV 格式：日期,成员,标题,内容,优先级,状态,重复类型
    // 重要事项 CSV 格式：日期,成员,标题,内容,优先级(1低/2中/3高或中文),状态(0待办/1已完成/2已取消或中文),重复类型(0-4或中文)
    private static Event parseEventLine(String rawLine) {
        List<String> f = splitCsvLine(rawLine);
        if (f.size() < 4) return null;
        try {
            Date eventTime = parseDate(f.get(0).trim());
            if (eventTime == null) return null;
            Event ev = new Event();
            ev.setEventTime(eventTime);
            ev.setUserName(f.get(1).trim());
            ev.setTitle(f.get(2).trim().isEmpty() ? null : f.get(2).trim());
            ev.setContent(f.get(3).trim().isEmpty() ? null : f.get(3).trim());
            // 优先级：支持数字(1低/2中/3高)和中文(低/中/高)
            if (f.size() > 4) {
                String pri = f.get(4).trim();
                if (!pri.isEmpty()) {
                    try { ev.setPriority(Integer.parseInt(pri)); }
                    catch (NumberFormatException e) {
                        if (pri.contains("高")) ev.setPriority(3);
                        else if (pri.contains("中")) ev.setPriority(2);
                        else if (pri.contains("低")) ev.setPriority(1);
                    }
                }
            }
            // 状态：支持数字(0待办/1已完成/2已取消)和中文(待办/已完成/已取消)
            if (f.size() > 5) {
                String sta = f.get(5).trim();
                if (!sta.isEmpty()) {
                    try { ev.setStatus(Integer.parseInt(sta)); }
                    catch (NumberFormatException e) {
                        if (sta.contains("已完成") || sta.contains("完成")) ev.setStatus(1);
                        else if (sta.contains("已取消") || sta.contains("取消")) ev.setStatus(2);
                        else ev.setStatus(0);
                    }
                }
            }
            // 重复类型：支持数字和中文
            if (f.size() > 6) {
                String rt = f.get(6).trim();
                if (!rt.isEmpty()) {
                    try { ev.setRepeatType(Integer.parseInt(rt)); }
                    catch (NumberFormatException e) {
                        if (rt.contains("每天")) ev.setRepeatType(1);
                        else if (rt.contains("每周")) ev.setRepeatType(2);
                        else if (rt.contains("每月")) ev.setRepeatType(3);
                        else if (rt.contains("每年")) ev.setRepeatType(4);
                        else ev.setRepeatType(0);
                    }
                }
            }
            // 第 8 列（可选）：提醒时间。旧备份无此列时不开启提醒
            if (f.size() > 7) {
                String remindStr = f.get(7).trim();
                if (!remindStr.isEmpty()) {
                    Date remindTime = parseDate(remindStr);
                    if (remindTime != null) {
                        ev.setRemindTime(remindTime);
                        ev.setRemindEnabled(true);
                    }
                }
            }
            return ev;
        } catch (Exception e) {
            return null;
        }
    }

    // 读书笔记 CSV 格式：日期,成员,书名,作者,总页数,当前页,状态(0想读/1在读/2已读或中文),评分,备注
    private static BookRecord parseBookLine(String rawLine) {
        List<String> f = splitCsvLine(rawLine);
        if (f.size() < 4) return null;
        try {
            Date date = parseDate(f.get(0).trim());
            if (date == null) return null;
            BookRecord b = new BookRecord();
            b.setCreatedAt(date);
            b.setUserName(f.get(1).trim());
            b.setTitle(f.get(2).trim().isEmpty() ? null : f.get(2).trim());
            b.setAuthor(f.get(3).trim().isEmpty() ? null : f.get(3).trim());
            if (f.size() > 4) {
                String totalStr = f.get(4).trim();
                if (!totalStr.isEmpty()) b.setTotalPages((int) Double.parseDouble(totalStr));
            }
            if (f.size() > 5) {
                String curStr = f.get(5).trim();
                if (!curStr.isEmpty()) b.setCurrentPage((int) Double.parseDouble(curStr));
            }
            // 状态：支持数字(0想读/1在读/2已读)和中文(想读/在读/已读)
            if (f.size() > 6) {
                String sta = f.get(6).trim();
                if (!sta.isEmpty()) {
                    try { b.setStatus((int) Double.parseDouble(sta)); }
                    catch (NumberFormatException e) {
                        if (sta.contains("已读") || sta.contains("读完")) b.setStatus(2);
                        else if (sta.contains("在读") || sta.contains("读中")) b.setStatus(1);
                        else b.setStatus(0);
                    }
                }
            }
            // 评分
            if (f.size() > 7) {
                String rat = f.get(7).trim();
                if (!rat.isEmpty()) {
                    try { b.setRating((int) Double.parseDouble(rat)); }
                    catch (NumberFormatException e) { /* 评分只能是数字，忽略 */ }
                }
            }
            if (f.size() > 8) b.setNotes(f.get(8).trim().isEmpty() ? null : f.get(8).trim());
            // 第 10 列（可选）：开始阅读时间。旧备份无此列则保持空
            if (f.size() > 9) {
                Date st = parseDate(f.get(9).trim());
                if (st != null) b.setStartAt(st);
            }
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    // 猫粮记录 CSV 格式：日期,成员,品牌,类型,开始日期,结束日期,备注
    // 结束日期为空或"持续中"则 endDate = null
    private static CatFoodRecord parseCatFoodLine(String rawLine) {
        List<String> f = splitCsvLine(rawLine);
        if (f.size() < 5) return null;
        try {
            Date date = parseDate(f.get(0).trim());
            if (date == null) return null;
            CatFoodRecord cf = new CatFoodRecord();
            cf.setStartDate(date);
            cf.setUserName(f.get(1).trim());
            cf.setBrand(f.get(2).trim().isEmpty() ? null : f.get(2).trim());
            cf.setFoodType(f.get(3).trim().isEmpty() ? null : f.get(3).trim());
            Date startDate = parseDate(f.get(4).trim());
            if (startDate != null) cf.setStartDate(startDate);
            // 结束日期：空或"持续中"为 null
            if (f.size() > 5) {
                String endStr = f.get(5).trim();
                if (!endStr.isEmpty() && !endStr.equals("持续中") && !endStr.equals("持续")) {
                    Date endDate = parseDate(endStr);
                    cf.setEndDate(endDate);
                }
            }
            if (f.size() > 6) cf.setNotes(f.get(6).trim().isEmpty() ? null : f.get(6).trim());
            return cf;
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================
    // 读后感明细（页面区间）解析与挂载 — v12 新增
    // ============================================================

    /** 导入中间态：一条读后感明细，尚未绑定数据库 id */
    private static class NoteRow {
        String bookTitle;
        String userName;
        int startPage;
        int endPage;
        String content;
        Date createdAt;
    }

    // CSV 列：书名,成员,起始页,结束页,内容,创建时间
    private static NoteRow parseNoteLine(String rawLine) {
        List<String> f = splitCsvLine(rawLine);
        if (f.size() < 5) return null;
        try {
            NoteRow n = new NoteRow();
            n.bookTitle = f.get(0).trim();
            if (n.bookTitle.isEmpty()) return null;
            n.userName = f.get(1).trim();
            String s = f.get(2).trim();
            String e = f.get(3).trim();
            if (!s.isEmpty()) n.startPage = (int) Double.parseDouble(s);
            if (!e.isEmpty()) n.endPage = (int) Double.parseDouble(e);
            n.content = f.get(4).trim().isEmpty() ? null : f.get(4).trim();
            if (f.size() > 5) n.createdAt = parseDate(f.get(5).trim());
            return n;
        } catch (Exception e) {
            return null;
        }
    }

    /** 按「书名 + 成员」把明细挂到对应 BookRecord，等插入拿到真实 id 后再落库 */
    private static void attachNoteRows(List<BookRecord> books, List<NoteRow> rows) {
        if (books == null || rows == null || rows.isEmpty()) return;
        for (NoteRow nr : rows) {
            BookRecord target = null;
            for (BookRecord b : books) {
                if (b.getTitle() == null) continue;
                if (!b.getTitle().trim().equals(nr.bookTitle)) continue;
                String bu = b.getUserName() != null ? b.getUserName().trim() : "";
                if (nr.userName != null && !nr.userName.isEmpty()
                        && !bu.isEmpty() && !bu.equals(nr.userName)) continue;
                target = b;
                break;
            }
            if (target != null) {
                com.example.healthrecorder.book.BookNote note = new com.example.healthrecorder.book.BookNote();
                note.setStartPage(nr.startPage);
                note.setEndPage(nr.endPage);
                note.setContent(nr.content);
                note.setCreatedAt(nr.createdAt);
                target.addPendingNote(note);
            }
        }
    }

    private static List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                    else inQuotes = false;
                } else sb.append(c);
            } else {
                if (c == '"') inQuotes = true;
                else if (c == ',') { result.add(sb.toString()); sb = new StringBuilder(); }
                else sb.append(c);
            }
        }
        result.add(sb.toString());
        return result;
    }

    // ============================ XLSX ============================
    public static ImportResult parseExcel(InputStream inputStream) throws Exception {
        List<Record> records = new ArrayList<>();
        List<User> users = new ArrayList<>();
        List<CatRecord> catRecords = new ArrayList<>();
        List<TeethRecord> teethRecords = new ArrayList<>();
        List<ExerciseRecord> exerciseRecords = new ArrayList<>();
        List<BreathingRecord> breathingRecords = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        List<BookRecord> bookRecords = new ArrayList<>();
        List<CatFoodRecord> catFoodRecords = new ArrayList<>();
        int totalRows = 0;
        int skippedRows = 0;

        Workbook workbook = new XSSFWorkbook(inputStream);

        // 人员配置
        Sheet userSheet = workbook.getSheet("人员配置");
        if (userSheet != null) {
            boolean first = true;
            for (Row row : userSheet) {
                if (row == null) continue;
                totalRows++;
                if (first) { first = false; continue; }
                User u = parseUserRow(row);
                if (u != null) users.add(u); else skippedRows++;
            }
        }

        // 健康记录（兼容旧版单 Sheet）
        Sheet recordSheet = workbook.getSheet("健康记录");
        if (recordSheet == null) recordSheet = workbook.getSheetAt(0);
        if (recordSheet != null) {
            boolean first = true;
            for (Row row : recordSheet) {
                if (row == null) continue;
                totalRows++;
                if (first) { first = false; continue; }
                Record r = parseExcelRow(row);
                if (r != null) records.add(r); else skippedRows++;
            }
        }

        // 猫咪如厕记录
        Sheet catSheet = workbook.getSheet("猫咪如厕记录");
        if (catSheet != null) {
            boolean first = true;
            for (Row row : catSheet) {
                if (row == null) continue;
                totalRows++;
                if (first) { first = false; continue; }
                CatRecord c = parseCatRow(row);
                if (c != null) catRecords.add(c); else skippedRows++;
            }
        }

        // 刷牙记录
        Sheet teethSheet = workbook.getSheet("刷牙记录");
        if (teethSheet != null) {
            boolean first = true;
            for (Row row : teethSheet) {
                if (row == null) continue;
                totalRows++;
                if (first) { first = false; continue; }
                TeethRecord t = parseTeethRow(row);
                if (t != null) teethRecords.add(t); else skippedRows++;
            }
        }

        // 运动记录
        Sheet exerciseSheet = workbook.getSheet("运动记录");
        if (exerciseSheet != null) {
            boolean first = true;
            for (Row row : exerciseSheet) {
                if (row == null) continue;
                totalRows++;
                if (first) { first = false; continue; }
                ExerciseRecord ex = parseExerciseRow(row);
                if (ex != null) exerciseRecords.add(ex); else skippedRows++;
            }
        }

        // 呼吸打卡记录
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet != null && sheet.getSheetName() != null && sheet.getSheetName().contains("呼吸")) {
                boolean first = true;
                for (Row row : sheet) {
                    if (row == null) continue;
                    totalRows++;
                    if (first) { first = false; continue; }
                    BreathingRecord br = parseBreathingRow(row);
                    if (br != null) breathingRecords.add(br); else skippedRows++;
                }
                break;
            }
        }

        // 重要事项记录
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet != null && sheet.getSheetName() != null && sheet.getSheetName().contains("重要事项")) {
                boolean first = true;
                for (Row row : sheet) {
                    if (row == null) continue;
                    totalRows++;
                    if (first) { first = false; continue; }
                    Event ev = parseEventRow(row);
                    if (ev != null) events.add(ev); else skippedRows++;
                }
                break;
            }
        }

        // 读书笔记记录
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet != null && sheet.getSheetName() != null && sheet.getSheetName().contains("读书")) {
                boolean first = true;
                for (Row row : sheet) {
                    if (row == null) continue;
                    totalRows++;
                    if (first) { first = false; continue; }
                    BookRecord b = parseBookRow(row);
                    if (b != null) bookRecords.add(b); else skippedRows++;
                }
                break;
            }
        }

        // 猫粮记录
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet != null && sheet.getSheetName() != null && sheet.getSheetName().contains("猫粮")) {
                boolean first = true;
                for (Row row : sheet) {
                    if (row == null) continue;
                    totalRows++;
                    if (first) { first = false; continue; }
                    CatFoodRecord cf = parseCatFoodRow(row);
                    if (cf != null) catFoodRecords.add(cf); else skippedRows++;
                }
                break;
            }
        }

        // 读后感明细（v12）：按「书名 + 成员」挂到对应书籍上
        List<NoteRow> noteRows = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet != null && sheet.getSheetName() != null && sheet.getSheetName().contains("读后感")) {
                boolean first = true;
                for (Row row : sheet) {
                    if (row == null) continue;
                    totalRows++;
                    if (first) { first = false; continue; }
                    NoteRow nr = parseNoteRow(row);
                    if (nr != null) noteRows.add(nr); else skippedRows++;
                }
                break;
            }
        }
        attachNoteRows(bookRecords, noteRows);

        workbook.close();
        return new ImportResult(records, users, catRecords, teethRecords, exerciseRecords,
                breathingRecords, events, bookRecords, catFoodRecords,
                totalRows, skippedRows, null);
    }

    private static User parseUserRow(Row row) {
        try {
            String idStr  = cellToString(row.getCell(0)).trim();
            String name  = cellToString(row.getCell(1)).trim();
            String colorStr = cellToString(row.getCell(2)).trim();
            if (name.isEmpty()) return null;
            User u = new User();
            try { u.setId(Integer.parseInt(idStr)); } catch (Exception ignored) {}
            u.setName(name);
            try { u.setColor(Integer.parseInt(colorStr)); } catch (Exception ignored) { u.setColor(0); }
            return u;
        } catch (Exception e) {
            return null;
        }
    }

    private static Record parseExcelRow(Row row) {
        try {
            String dateStr   = cellToString(row.getCell(0)).trim();
            String userName  = cellToString(row.getCell(1)).trim();
            String weightStr = cellToString(row.getCell(2)).trim();
            String sysStr    = cellToString(row.getCell(3)).trim();
            String diaStr    = cellToString(row.getCell(4)).trim();
            String notes     = cellToString(row.getCell(5)).trim();

            Date date = parseDate(dateStr);
            if (date == null) return null;

            float weight = 0f;
            if (!weightStr.isEmpty()) { try { weight = Float.parseFloat(weightStr); } catch (NumberFormatException ignored) {} }
            int sys = 0, dia = 0;
            if (!sysStr.isEmpty()) { try { sys = (int) Double.parseDouble(sysStr); } catch (NumberFormatException ignored) {} }
            if (!diaStr.isEmpty()) { try { dia = (int) Double.parseDouble(diaStr); } catch (NumberFormatException ignored) {} }

            Record r = new Record();
            r.setDate(date);
            r.setUserName(userName.isEmpty() ? "本人" : userName);
            r.setWeight(weight);
            r.setSystolic(sys);
            r.setDiastolic(dia);
            r.setNotes(notes.isEmpty() ? null : notes);
            r.setUserId(0);
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    private static CatRecord parseCatRow(Row row) {
        try {
            Date date = parseDate(cellToString(row.getCell(0)).trim());
            if (date == null) return null;
            CatRecord c = new CatRecord();
            c.setDate(date);
            c.setCatName(cellToString(row.getCell(1)).trim());
            c.setStoolType((int) Double.parseDouble(cellToString(row.getCell(2)).trim()));
            c.setStoolCount((int) Double.parseDouble(cellToString(row.getCell(3)).trim()));
            // 向前兼容：新格式有如厕意愿/过程字段（列E/F），旧格式无则跳过
            String willCell = cellToString(row.getCell(4)).trim();
            if (!willCell.isEmpty()) c.setWillingnessFromString(willCell);
            String procCell = cellToString(row.getCell(5)).trim();
            if (!procCell.isEmpty()) c.setProcessTypeFromString(procCell);
            // 备注列位置：新格式第G列（6），旧格式第E列（4）
            int notesIdx = row.getLastCellNum() > 6 ? 6 : 4;
            String notes = cellToString(row.getCell(notesIdx)).trim();
            c.setNotes(notes.isEmpty() ? null : notes);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private static TeethRecord parseTeethRow(Row row) {
        try {
            Date date = parseDate(cellToString(row.getCell(0)).trim());
            if (date == null) return null;
            TeethRecord t = new TeethRecord();
            t.setDate(date);
            t.setUserName(cellToString(row.getCell(1)).trim());
            // 向前兼容：旧格式有5列（时段/时长），新格式只有3列
            int lastCol = row.getLastCellNum();
            if (lastCol > 4) {
                // 旧格式：日期/成员/时段/时长/备注
                String periodStr = cellToString(row.getCell(2)).trim();
                t.setPeriod(periodStr.equals("晚上") ? 1 : 0);
                String durStr = cellToString(row.getCell(3)).trim();
                if (!durStr.isEmpty()) t.setDuration((int) Double.parseDouble(durStr));
                t.setNotes(cellToString(row.getCell(4)).trim());
            } else if (lastCol > 2) {
                // 新格式：日期/成员/备注
                t.setNotes(cellToString(row.getCell(2)).trim());
            }
            return t;
        } catch (Exception e) {
            return null;
        }
    }

    private static ExerciseRecord parseExerciseRow(Row row) {
        try {
            Date date = parseDate(cellToString(row.getCell(0)).trim());
            if (date == null) return null;
            ExerciseRecord e = new ExerciseRecord();
            e.setDate(date);
            e.setUserName(cellToString(row.getCell(1)).trim());
            e.setTypeFromString(cellToString(row.getCell(2)).trim());
            e.setDistance((float) Double.parseDouble(cellToString(row.getCell(3)).trim()));
            e.setDuration((int) Double.parseDouble(cellToString(row.getCell(4)).trim()));
            e.setPace((float) Double.parseDouble(cellToString(row.getCell(5)).trim()));
            e.setHeartRate((int) Double.parseDouble(cellToString(row.getCell(6)).trim()));
            e.setCalories((int) Double.parseDouble(cellToString(row.getCell(7)).trim()));
            e.setElevationGain((int) Double.parseDouble(cellToString(row.getCell(8)).trim()));
            e.setDifficulty((int) Double.parseDouble(cellToString(row.getCell(9)).trim()));
            // 新格式有12列（含地点），旧格式有11列
            int lastCol = row.getLastCellNum();
            if (lastCol > 11) {
                String loc = cellToString(row.getCell(10)).trim();
                e.setLocation(loc.isEmpty() ? null : loc);
                String notes = cellToString(row.getCell(11)).trim();
                e.setNotes(notes.isEmpty() ? null : notes);
            } else {
                String notes = cellToString(row.getCell(10)).trim();
                e.setNotes(notes.isEmpty() ? null : notes);
            }
            return e;
        } catch (Exception ex) {
            return null;
        }
    }

    // 呼吸打卡 Sheet 列：0=日期,1=成员,2=类型,3=时长,4=评分,5=自定义内容,6=自定义时长,7=备注
    private static BreathingRecord parseBreathingRow(Row row) {
        try {
            Date date = parseDate(cellToString(row.getCell(0)).trim());
            if (date == null) return null;
            BreathingRecord br = new BreathingRecord();
            br.setDate(date);
            br.setUserName(cellToString(row.getCell(1)).trim());
            try { br.setMode(Integer.parseInt(cellToString(row.getCell(2)).trim())); } catch (NumberFormatException e) { br.setMode(0); }
            String dur = cellToString(row.getCell(3)).trim();
            if (!dur.isEmpty()) br.setDuration((int) Double.parseDouble(dur));
            String score = cellToString(row.getCell(4)).trim();
            if (!score.isEmpty()) br.setScore((int) Double.parseDouble(score));
            String cc = cellToString(row.getCell(5)).trim();
            if (!cc.isEmpty()) br.setCustomContent(cc);
            String cd = cellToString(row.getCell(6)).trim();
            if (!cd.isEmpty()) br.setCustomDuration((int) Double.parseDouble(cd));
            String notes = cellToString(row.getCell(7)).trim();
            br.setNotes(notes.isEmpty() ? null : notes);
            return br;
        } catch (Exception e) {
            return null;
        }
    }

    // 重要事项 Sheet 列：0=日期,1=成员,2=标题,3=内容,4=优先级,5=状态,6=重复类型
    private static Event parseEventRow(Row row) {
        try {
            Date eventTime = parseDate(cellToString(row.getCell(0)).trim());
            if (eventTime == null) return null;
            Event ev = new Event();
            ev.setEventTime(eventTime);
            ev.setUserName(cellToString(row.getCell(1)).trim());
            String title = cellToString(row.getCell(2)).trim();
            if (!title.isEmpty()) ev.setTitle(title);
            String content = cellToString(row.getCell(3)).trim();
            if (!content.isEmpty()) ev.setContent(content);
            String pri = cellToString(row.getCell(4)).trim();
            if (!pri.isEmpty()) {
                try { ev.setPriority((int) Double.parseDouble(pri)); }
                catch (NumberFormatException e) {
                    if (pri.contains("高")) ev.setPriority(3);
                    else if (pri.contains("中")) ev.setPriority(2);
                    else if (pri.contains("低")) ev.setPriority(1);
                }
            }
            String sta = cellToString(row.getCell(5)).trim();
            if (!sta.isEmpty()) {
                try { ev.setStatus((int) Double.parseDouble(sta)); }
                catch (NumberFormatException e) {
                    if (sta.contains("已完成") || sta.contains("完成")) ev.setStatus(1);
                    else if (sta.contains("已取消") || sta.contains("取消")) ev.setStatus(2);
                    else ev.setStatus(0);
                }
            }
            String rt = cellToString(row.getCell(6)).trim();
            if (!rt.isEmpty()) {
                try { ev.setRepeatType((int) Double.parseDouble(rt)); }
                catch (NumberFormatException e) {
                    if (rt.contains("每天")) ev.setRepeatType(1);
                    else if (rt.contains("每周")) ev.setRepeatType(2);
                    else if (rt.contains("每月")) ev.setRepeatType(3);
                    else if (rt.contains("每年")) ev.setRepeatType(4);
                    else ev.setRepeatType(0);
                }
            }
            // 第 8 列（可选）：提醒时间。旧备份没有这一列，读不到就保持未开启提醒
            String remindStr = cellToString(row.getCell(7)).trim();
            if (!remindStr.isEmpty()) {
                Date remindTime = parseDate(remindStr);
                if (remindTime != null) {
                    ev.setRemindTime(remindTime);
                    ev.setRemindEnabled(true);
                }
            }
            return ev;
        } catch (Exception e) {
            return null;
        }
    }

    // 读书笔记 Sheet 列：0=日期,1=成员,2=书名,3=作者,4=总页数,5=当前页,6=状态,7=评分,8=备注
    private static BookRecord parseBookRow(Row row) {
        try {
            Date date = parseDate(cellToString(row.getCell(0)).trim());
            if (date == null) return null;
            BookRecord b = new BookRecord();
            b.setCreatedAt(date);
            b.setUserName(cellToString(row.getCell(1)).trim());
            String title = cellToString(row.getCell(2)).trim();
            if (!title.isEmpty()) b.setTitle(title);
            String author = cellToString(row.getCell(3)).trim();
            if (!author.isEmpty()) b.setAuthor(author);
            String totalStr = cellToString(row.getCell(4)).trim();
            if (!totalStr.isEmpty()) b.setTotalPages((int) Double.parseDouble(totalStr));
            String curStr = cellToString(row.getCell(5)).trim();
            if (!curStr.isEmpty()) b.setCurrentPage((int) Double.parseDouble(curStr));
            String sta = cellToString(row.getCell(6)).trim();
            if (!sta.isEmpty()) {
                try { b.setStatus((int) Double.parseDouble(sta)); }
                catch (NumberFormatException e) {
                    if (sta.contains("已读") || sta.contains("读完")) b.setStatus(2);
                    else if (sta.contains("在读") || sta.contains("读中")) b.setStatus(1);
                    else b.setStatus(0);
                }
            }
            String rat = cellToString(row.getCell(7)).trim();
            if (!rat.isEmpty()) {
                try { b.setRating((int) Double.parseDouble(rat)); }
                catch (NumberFormatException e) { /* 评分只能是数字，忽略 */ }
            }
            String notes = cellToString(row.getCell(8)).trim();
            b.setNotes(notes.isEmpty() ? null : notes);
            // 第 10 列（可选）：开始阅读时间
            String startStr = cellToString(row.getCell(9)).trim();
            if (!startStr.isEmpty()) {
                Date st = parseDate(startStr);
                if (st != null) b.setStartAt(st);
            }
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    // 读后感明细 Sheet 列：0=书名,1=成员,2=起始页,3=结束页,4=内容,5=创建时间
    private static NoteRow parseNoteRow(Row row) {
        try {
            String title = cellToString(row.getCell(0)).trim();
            if (title.isEmpty()) return null;
            NoteRow n = new NoteRow();
            n.bookTitle = title;
            n.userName = cellToString(row.getCell(1)).trim();
            String s = cellToString(row.getCell(2)).trim();
            String e = cellToString(row.getCell(3)).trim();
            if (!s.isEmpty()) n.startPage = (int) Double.parseDouble(s);
            if (!e.isEmpty()) n.endPage = (int) Double.parseDouble(e);
            String c = cellToString(row.getCell(4)).trim();
            n.content = c.isEmpty() ? null : c;
            String d = cellToString(row.getCell(5)).trim();
            if (!d.isEmpty()) n.createdAt = parseDate(d);
            return n;
        } catch (Exception e) {
            return null;
        }
    }

    // 猫粮记录 Sheet 列：0=日期,1=成员,2=品牌,3=类型,4=开始日期,5=结束日期(空/"持续中"=null),6=备注
    private static CatFoodRecord parseCatFoodRow(Row row) {
        try {
            Date date = parseDate(cellToString(row.getCell(0)).trim());
            if (date == null) return null;
            CatFoodRecord cf = new CatFoodRecord();
            cf.setStartDate(date);
            cf.setUserName(cellToString(row.getCell(1)).trim());
            String brand = cellToString(row.getCell(2)).trim();
            if (!brand.isEmpty()) cf.setBrand(brand);
            String foodType = cellToString(row.getCell(3)).trim();
            if (!foodType.isEmpty()) cf.setFoodType(foodType);
            Date startDate = parseDate(cellToString(row.getCell(4)).trim());
            if (startDate != null) cf.setStartDate(startDate);
            String endStr = cellToString(row.getCell(5)).trim();
            if (!endStr.isEmpty() && !endStr.equals("持续中") && !endStr.equals("持续")) {
                Date endDate = parseDate(endStr);
                cf.setEndDate(endDate);
            }
            String notes = cellToString(row.getCell(6)).trim();
            cf.setNotes(notes.isEmpty() ? null : notes);
            return cf;
        } catch (Exception e) {
            return null;
        }
    }

    // ============================ 工具 ============================
    static Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        s = s.trim();
        try { return DATE_FORMAT.parse(s); } catch (ParseException ignored) {}
        try { return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s); } catch (ParseException ignored) {}
        try { return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).parse(s); } catch (ParseException ignored) {}
        try { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(s); } catch (ParseException ignored) {}
        return null;
    }

    private static String cellToString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return DATE_FORMAT.format(cell.getDateCellValue());
                }
                double v = cell.getNumericCellValue();
                return (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue(); } catch (Exception ignored) {}
                try { return String.valueOf(cell.getNumericCellValue()); } catch (Exception ignored) {}
                return "";
            default:
                return "";
        }
    }

    /** 统计引号数量，用于判断 CSV 记录是否跨行（未闭合） */
    private static int countQuotes(CharSequence s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '"') n++;
        }
        return n;
    }

    private static String stripBom(String line) {
        if (line != null && line.length() > 0 && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }
}
