package com.example.healthrecorder;

import com.example.healthrecorder.breathing.BreathingRecord;
import com.example.healthrecorder.book.BookRecord;
import com.example.healthrecorder.cat.CatFoodRecord;
import com.example.healthrecorder.cat.CatRecord;
import com.example.healthrecorder.event.Event;
import com.example.healthrecorder.exercise.ExerciseRecord;
import com.example.healthrecorder.teeth.TeethRecord;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ExportHelper {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public static byte[] buildCsvBytes(List<Record> records, List<User> users,
                                        List<CatRecord> catRecords,
                                        List<TeethRecord> teethRecords,
                                        List<ExerciseRecord> exerciseRecords,
                                        List<BreathingRecord> breathingRecords,
                                        List<Event> events,
                                        List<BookRecord> bookRecords,
                                        List<CatFoodRecord> catFoodRecords) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(16384);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(baos, "UTF-8"));
        writer.write("\uFEFF");

        writer.write("=== 人员配置 ===");
        writer.newLine();
        writer.write("ID,姓名,颜色");
        writer.newLine();
        for (User u : users) {
            writer.write(String.format(Locale.US, "%d,%s,%d",
                    u.getId(), escapeCsv(u.getName()), u.getColor()));
            writer.newLine();
        }
        writer.newLine();

        writer.write("=== 健康记录 ===");
        writer.newLine();
        writer.write("日期,成员,体重(kg),收缩压(mmHg),舒张压(mmHg),备注");
        writer.newLine();
        for (Record r : records) {
            String userName = (r.getUserName() != null && !r.getUserName().isEmpty()) ? r.getUserName() : "本人";
            String weightStr = r.hasWeight() ? String.format(Locale.US, "%.2f", r.getWeight()) : "";
            String sysStr    = r.getSystolic() > 0 ? String.valueOf(r.getSystolic()) : "";
            String diaStr    = r.getDiastolic() > 0 ? String.valueOf(r.getDiastolic()) : "";
            String notesStr  = (r.getNotes() != null) ? r.getNotes().replace("\r", "").replace("\n", " ") : "";
            writer.write(String.format(Locale.US, "%s,%s,%s,%s,%s,%s",
                    DATE_FORMAT.format(r.getDate()), userName, weightStr, sysStr, diaStr, escapeCsv(notesStr)));
            writer.newLine();
        }
        writer.newLine();

        writer.write("=== 猫咪如厕记录 ===");
        writer.newLine();
        writer.write("日期,成员,评分,数量,如厕意愿,如厕过程,备注");
        writer.newLine();
        if (catRecords != null) {
            for (CatRecord c : catRecords) {
                String catName = c.getCatName() != null ? c.getCatName() : "未知";
                String notes = c.getNotes() != null ? c.getNotes().replace("\r", "").replace("\n", " ") : "";
                String willStr = c.getWillingnessLabel();
                String procStr = c.getProcessTypeLabel();
                writer.write(String.format(Locale.US, "%s,%s,%d,%d,%s,%s,%s",
                        DATE_FORMAT.format(c.getDate()), escapeCsv(catName),
                        c.getStoolType(), c.getStoolCount(),
                        escapeCsv(willStr), escapeCsv(procStr), escapeCsv(notes)));
                writer.newLine();
            }
        }
        writer.newLine();

        writer.write("=== 刷牙记录 ===");
        writer.newLine();
        writer.write("日期,成员,备注");
        writer.newLine();
        if (teethRecords != null) {
            for (TeethRecord t : teethRecords) {
                String userName = t.getUserName() != null ? t.getUserName() : "本人";
                String notes = t.getNotes() != null ? t.getNotes().replace("\r", "").replace("\n", " ") : "";
                writer.write(String.format(Locale.US, "%s,%s,%s",
                        DATE_FORMAT.format(t.getDate()), escapeCsv(userName), escapeCsv(notes)));
                writer.newLine();
            }
        }
        writer.newLine();

        writer.write("=== 运动记录 ===");
        writer.newLine();
        writer.write("日期,成员,类型,距离(km),时长(分),配速,心率,卡路里,爬升,难度,地点,备注");
        writer.newLine();
        if (exerciseRecords != null) {
            for (ExerciseRecord e : exerciseRecords) {
                String userName = e.getUserName() != null ? e.getUserName() : "本人";
                String typeStr = e.getTypeString();
                String loc = e.getLocation() != null ? e.getLocation().replace("\r", "").replace("\n", " ") : "";
                String notes = e.getNotes() != null ? e.getNotes().replace("\r", "").replace("\n", " ") : "";
                writer.write(String.format(Locale.US, "%s,%s,%s,%.2f,%d,%.1f,%d,%d,%d,%d,%s,%s",
                        DATE_FORMAT.format(e.getDate()), escapeCsv(userName), typeStr,
                        e.getDistance(), e.getDuration(), e.getPace(),
                        e.getHeartRate(), e.getCalories(), e.getElevationGain(),
                        e.getDifficulty(), escapeCsv(loc), escapeCsv(notes)));
                writer.newLine();
            }
        }

        writer.newLine();

        writer.write("=== 呼吸打卡记录 ===");
        writer.newLine();
        writer.write("日期,成员,类型,时长(秒),评分,自定义内容,自定义时长(秒),备注");
        writer.newLine();
        if (breathingRecords != null) {
            for (BreathingRecord b : breathingRecords) {
                String userName = b.getUserName() != null ? b.getUserName() : "本人";
                String notes = b.getNotes() != null ? b.getNotes().replace("\r", "").replace("\n", " ") : "";
                String cc = b.getCustomContent() != null ? b.getCustomContent() : "";
                writer.write(String.format(Locale.US, "%s,%s,%s,%d,%d,%s,%d,%s",
                        DATE_FORMAT.format(b.getDate()), escapeCsv(userName),
                        escapeCsv(b.getModeText()), b.getDuration(), b.getScore(),
                        escapeCsv(cc), b.getCustomDuration(), escapeCsv(notes)));
                writer.newLine();
            }
        }
        writer.newLine();

        writer.write("=== 重要事项记录 ===");
        writer.newLine();
        writer.write("日期,成员,标题,内容,优先级,状态,重复类型");
        writer.newLine();
        if (events != null) {
            for (Event ev : events) {
                String userName = ev.getUserName() != null ? ev.getUserName() : "本人";
                writer.write(String.format(Locale.US, "%s,%s,%s,%s,%s,%s,%s",
                        ev.getEventTime() != null ? DATE_FORMAT.format(ev.getEventTime()) : "",
                        escapeCsv(userName), escapeCsv(ev.getTitle()),
                        escapeCsv(ev.getContent() != null ? ev.getContent() : ""),
                        ev.getPriorityText(), ev.getStatusText(), ev.getRepeatText()));
                writer.newLine();
            }
        }
        writer.newLine();

        writer.write("=== 读书笔记记录 ===");
        writer.newLine();
        writer.write("日期,成员,书名,作者,总页数,当前页,状态,评分,备注");
        writer.newLine();
        if (bookRecords != null) {
            for (BookRecord bk : bookRecords) {
                String userName = bk.getUserName() != null ? bk.getUserName() : "本人";
                String notes = bk.getNotes() != null ? bk.getNotes().replace("\r", "").replace("\n", " ") : "";
                String statusStr = bk.getStatusText();
                writer.write(String.format(Locale.US, "%s,%s,%s,%s,%d,%d,%s,%d,%s",
                        bk.getCreatedAt() != null ? DATE_FORMAT.format(bk.getCreatedAt()) : "",
                        escapeCsv(userName), escapeCsv(bk.getTitle()), escapeCsv(bk.getAuthor()),
                        bk.getTotalPages(), bk.getCurrentPage(), statusStr,
                        bk.getRating(), escapeCsv(notes)));
                writer.newLine();
            }
        }
        writer.newLine();

        writer.write("=== 猫粮记录 ===");
        writer.newLine();
        writer.write("日期,成员,品牌,类型,开始日期,结束日期,备注");
        writer.newLine();
        if (catFoodRecords != null) {
            for (CatFoodRecord cf : catFoodRecords) {
                String userName = cf.getUserName() != null ? cf.getUserName() : "本人";
                String notes = cf.getNotes() != null ? cf.getNotes().replace("\r", "").replace("\n", " ") : "";
                String endStr = cf.getEndDate() != null ? DATE_FORMAT.format(cf.getEndDate()) : "持续中";
                writer.write(String.format(Locale.US, "%s,%s,%s,%s,%s,%s,%s",
                        cf.getStartDate() != null ? DATE_FORMAT.format(cf.getStartDate()) : "",
                        escapeCsv(userName), escapeCsv(cf.getBrand()),
                        escapeCsv(cf.getFoodType()),
                        cf.getStartDate() != null ? DATE_FORMAT.format(cf.getStartDate()) : "",
                        endStr, escapeCsv(notes)));
                writer.newLine();
            }
        }

        writer.flush();
        return baos.toByteArray();
    }

    public static byte[] buildExcelBytes(List<Record> records, List<User> users,
                                          List<CatRecord> catRecords,
                                          List<TeethRecord> teethRecords,
                                          List<ExerciseRecord> exerciseRecords,
                                          List<BreathingRecord> breathingRecords,
                                          List<Event> events,
                                          List<BookRecord> bookRecords,
                                          List<CatFoodRecord> catFoodRecords) throws Exception {
        Workbook workbook = new XSSFWorkbook();

        Sheet userSheet = workbook.createSheet("人员配置");
        Row uHeader = userSheet.createRow(0);
        uHeader.createCell(0).setCellValue("ID");
        uHeader.createCell(1).setCellValue("姓名");
        uHeader.createCell(2).setCellValue("颜色");
        int uRow = 1;
        for (User u : users) {
            Row row = userSheet.createRow(uRow++);
            row.createCell(0).setCellValue(u.getId());
            row.createCell(1).setCellValue(u.getName());
            row.createCell(2).setCellValue(u.getColor());
        }
        userSheet.setColumnWidth(0, 8 * 256);
        userSheet.setColumnWidth(1, 14 * 256);
        userSheet.setColumnWidth(2, 8 * 256);

        Sheet healthSheet = workbook.createSheet("健康记录");
        String[] healthHeaders = {"日期", "成员", "体重(kg)", "收缩压(mmHg)", "舒张压(mmHg)", "备注"};
        Row hHeaderRow = healthSheet.createRow(0);
        for (int i = 0; i < healthHeaders.length; i++) {
            hHeaderRow.createCell(i).setCellValue(healthHeaders[i]);
        }
        int hRow = 1;
        for (Record r : records) {
            Row row = healthSheet.createRow(hRow++);
            row.createCell(0).setCellValue(DATE_FORMAT.format(r.getDate()));
            row.createCell(1).setCellValue((r.getUserName() != null && !r.getUserName().isEmpty()) ? r.getUserName() : "本人");
            if (r.hasWeight()) row.createCell(2).setCellValue(r.getWeight());
            if (r.getSystolic() > 0) row.createCell(3).setCellValue(r.getSystolic());
            if (r.getDiastolic() > 0) row.createCell(4).setCellValue(r.getDiastolic());
            row.createCell(5).setCellValue(r.getNotes() != null ? r.getNotes() : "");
        }
        healthSheet.setColumnWidth(0, 19 * 256);
        healthSheet.setColumnWidth(1, 8 * 256);
        healthSheet.setColumnWidth(2, 10 * 256);
        healthSheet.setColumnWidth(3, 12 * 256);
        healthSheet.setColumnWidth(4, 12 * 256);
        healthSheet.setColumnWidth(5, 20 * 256);

        Sheet catSheet = workbook.createSheet("猫咪如厕记录");
        String[] catHeaders = {"日期", "成员", "评分", "数量", "如厕意愿", "如厕过程", "备注"};
        Row cHeaderRow = catSheet.createRow(0);
        for (int i = 0; i < catHeaders.length; i++) {
            cHeaderRow.createCell(i).setCellValue(catHeaders[i]);
        }
        int cRow = 1;
        if (catRecords != null) {
            for (CatRecord c : catRecords) {
                Row row = catSheet.createRow(cRow++);
                row.createCell(0).setCellValue(DATE_FORMAT.format(c.getDate()));
                row.createCell(1).setCellValue(c.getCatName() != null ? c.getCatName() : "未知");
                row.createCell(2).setCellValue(c.getStoolType());
                row.createCell(3).setCellValue(c.getStoolCount());
                row.createCell(4).setCellValue(c.getWillingnessLabel());
                row.createCell(5).setCellValue(c.getProcessTypeLabel());
                row.createCell(6).setCellValue(c.getNotes() != null ? c.getNotes() : "");
            }
        }
        catSheet.setColumnWidth(0, 18 * 256);
        catSheet.setColumnWidth(1, 10 * 256);
        catSheet.setColumnWidth(2, 8 * 256);
        catSheet.setColumnWidth(3, 8 * 256);
        catSheet.setColumnWidth(4, 14 * 256);
        catSheet.setColumnWidth(5, 14 * 256);
        catSheet.setColumnWidth(6, 16 * 256);

        Sheet teethSheet = workbook.createSheet("刷牙记录");
        String[] teethHeaders = {"日期", "成员", "备注"};
        Row tHeaderRow = teethSheet.createRow(0);
        for (int i = 0; i < teethHeaders.length; i++) {
            tHeaderRow.createCell(i).setCellValue(teethHeaders[i]);
        }
        int tRow = 1;
        if (teethRecords != null) {
            for (TeethRecord t : teethRecords) {
                Row row = teethSheet.createRow(tRow++);
                row.createCell(0).setCellValue(DATE_FORMAT.format(t.getDate()));
                row.createCell(1).setCellValue(t.getUserName() != null ? t.getUserName() : "本人");
                row.createCell(2).setCellValue(t.getNotes() != null ? t.getNotes() : "");
            }
        }
        teethSheet.setColumnWidth(0, 18 * 256);
        teethSheet.setColumnWidth(1, 12 * 256);
        teethSheet.setColumnWidth(2, 20 * 256);

        Sheet exerciseSheet = workbook.createSheet("运动记录");
        String[] exerciseHeaders = {"日期", "成员", "类型", "距离(km)", "时长(分)", "配速", "心率", "卡路里", "爬升", "难度", "地点", "备注"};
        Row eHeaderRow = exerciseSheet.createRow(0);
        for (int i = 0; i < exerciseHeaders.length; i++) {
            eHeaderRow.createCell(i).setCellValue(exerciseHeaders[i]);
        }
        int eRow = 1;
        if (exerciseRecords != null) {
            for (ExerciseRecord e : exerciseRecords) {
                Row row = exerciseSheet.createRow(eRow++);
                row.createCell(0).setCellValue(DATE_FORMAT.format(e.getDate()));
                row.createCell(1).setCellValue(e.getUserName() != null ? e.getUserName() : "本人");
                row.createCell(2).setCellValue(e.getTypeString());
                row.createCell(3).setCellValue(e.getDistance());
                row.createCell(4).setCellValue(e.getDuration());
                row.createCell(5).setCellValue(e.getPace());
                row.createCell(6).setCellValue(e.getHeartRate());
                row.createCell(7).setCellValue(e.getCalories());
                row.createCell(8).setCellValue(e.getElevationGain());
                row.createCell(9).setCellValue(e.getDifficulty());
                row.createCell(10).setCellValue(e.getLocation() != null ? e.getLocation() : "");
                row.createCell(11).setCellValue(e.getNotes() != null ? e.getNotes() : "");
            }
        }
        exerciseSheet.setColumnWidth(0, 18 * 256);
        exerciseSheet.setColumnWidth(1, 10 * 256);
        exerciseSheet.setColumnWidth(2, 8 * 256);
        exerciseSheet.setColumnWidth(3, 10 * 256);
        exerciseSheet.setColumnWidth(4, 8 * 256);
        exerciseSheet.setColumnWidth(5, 10 * 256);
        exerciseSheet.setColumnWidth(6, 8 * 256);
        exerciseSheet.setColumnWidth(7, 8 * 256);
        exerciseSheet.setColumnWidth(8, 8 * 256);
        exerciseSheet.setColumnWidth(9, 8 * 256);
        exerciseSheet.setColumnWidth(10, 14 * 256);
        exerciseSheet.setColumnWidth(11, 16 * 256);

        // ── 呼吸打卡 Sheet ──────────────────────────────────────────────
        Sheet breathingSheet = workbook.createSheet("呼吸打卡记录");
        String[] bHeaders = {"日期","成员","类型","时长(秒)","评分","自定义内容","自定义时长(秒)","备注"};
        Row bHeaderRow = breathingSheet.createRow(0);
        for (int i = 0; i < bHeaders.length; i++) bHeaderRow.createCell(i).setCellValue(bHeaders[i]);
        int bRow = 1;
        if (breathingRecords != null) {
            for (BreathingRecord b : breathingRecords) {
                Row row = breathingSheet.createRow(bRow++);
                row.createCell(0).setCellValue(DATE_FORMAT.format(b.getDate()));
                row.createCell(1).setCellValue(b.getUserName() != null ? b.getUserName() : "本人");
                row.createCell(2).setCellValue(b.getModeText());
                row.createCell(3).setCellValue(b.getDuration());
                row.createCell(4).setCellValue(b.getScore());
                row.createCell(5).setCellValue(b.getCustomContent() != null ? b.getCustomContent() : "");
                row.createCell(6).setCellValue(b.getCustomDuration());
                row.createCell(7).setCellValue(b.getNotes() != null ? b.getNotes() : "");
            }
        }
        breathingSheet.setColumnWidth(0, 18 * 256);
        breathingSheet.setColumnWidth(1, 8 * 256);
        breathingSheet.setColumnWidth(2, 12 * 256);
        breathingSheet.setColumnWidth(3, 10 * 256);
        breathingSheet.setColumnWidth(4, 6 * 256);
        breathingSheet.setColumnWidth(5, 18 * 256);
        breathingSheet.setColumnWidth(6, 12 * 256);
        breathingSheet.setColumnWidth(7, 16 * 256);

        // ── 重要事项 Sheet ──────────────────────────────────────────────
        Sheet eventSheet = workbook.createSheet("重要事项记录");
        String[] evHeaders = {"日期","成员","标题","内容","优先级","状态","重复类型"};
        Row evHeaderRow = eventSheet.createRow(0);
        for (int i = 0; i < evHeaders.length; i++) evHeaderRow.createCell(i).setCellValue(evHeaders[i]);
        int evRow = 1;
        if (events != null) {
            for (Event ev : events) {
                Row row = eventSheet.createRow(evRow++);
                row.createCell(0).setCellValue(ev.getEventTime() != null ? DATE_FORMAT.format(ev.getEventTime()) : "");
                row.createCell(1).setCellValue(ev.getUserName() != null ? ev.getUserName() : "本人");
                row.createCell(2).setCellValue(ev.getTitle() != null ? ev.getTitle() : "");
                row.createCell(3).setCellValue(ev.getContent() != null ? ev.getContent() : "");
                row.createCell(4).setCellValue(ev.getPriorityText());
                row.createCell(5).setCellValue(ev.getStatusText());
                row.createCell(6).setCellValue(ev.getRepeatText());
            }
        }
        eventSheet.setColumnWidth(0, 18 * 256);
        eventSheet.setColumnWidth(1, 8 * 256);
        eventSheet.setColumnWidth(2, 14 * 256);
        eventSheet.setColumnWidth(3, 24 * 256);
        eventSheet.setColumnWidth(4, 8 * 256);
        eventSheet.setColumnWidth(5, 8 * 256);
        eventSheet.setColumnWidth(6, 10 * 256);
        eventSheet.setColumnWidth(7, 16 * 256);

        // ── 读书笔记 Sheet ──────────────────────────────────────────────
        Sheet bookSheet = workbook.createSheet("读书笔记记录");
        String[] bkHeaders = {"日期","成员","书名","作者","总页数","当前页","状态","评分","备注"};
        Row bkHeaderRow = bookSheet.createRow(0);
        for (int i = 0; i < bkHeaders.length; i++) bkHeaderRow.createCell(i).setCellValue(bkHeaders[i]);
        int bkRow = 1;
        if (bookRecords != null) {
            for (BookRecord bk : bookRecords) {
                Row row = bookSheet.createRow(bkRow++);
                row.createCell(0).setCellValue(bk.getCreatedAt() != null ? DATE_FORMAT.format(bk.getCreatedAt()) : "");
                row.createCell(1).setCellValue(bk.getUserName() != null ? bk.getUserName() : "本人");
                row.createCell(2).setCellValue(bk.getTitle() != null ? bk.getTitle() : "");
                row.createCell(3).setCellValue(bk.getAuthor() != null ? bk.getAuthor() : "");
                row.createCell(4).setCellValue(bk.getTotalPages());
                row.createCell(5).setCellValue(bk.getCurrentPage());
                row.createCell(6).setCellValue(bk.getStatusText());
                row.createCell(7).setCellValue(bk.getRating());
                row.createCell(8).setCellValue(bk.getNotes() != null ? bk.getNotes() : "");
            }
        }
        bookSheet.setColumnWidth(0, 18 * 256);
        bookSheet.setColumnWidth(1, 8 * 256);
        bookSheet.setColumnWidth(2, 20 * 256);
        bookSheet.setColumnWidth(3, 14 * 256);
        bookSheet.setColumnWidth(4, 8 * 256);
        bookSheet.setColumnWidth(5, 8 * 256);
        bookSheet.setColumnWidth(6, 8 * 256);
        bookSheet.setColumnWidth(7, 6 * 256);
        bookSheet.setColumnWidth(8, 20 * 256);

        // ── 猫粮记录 Sheet ──────────────────────────────────────────────
        Sheet catFoodSheet = workbook.createSheet("猫粮记录");
        String[] cfHeaders = {"日期","成员","品牌","类型","开始日期","结束日期","备注"};
        Row cfHeaderRow = catFoodSheet.createRow(0);
        for (int i = 0; i < cfHeaders.length; i++) cfHeaderRow.createCell(i).setCellValue(cfHeaders[i]);
        int cfRow = 1;
        if (catFoodRecords != null) {
            for (CatFoodRecord cf : catFoodRecords) {
                Row row = catFoodSheet.createRow(cfRow++);
                row.createCell(0).setCellValue(cf.getStartDate() != null ? DATE_FORMAT.format(cf.getStartDate()) : "");
                row.createCell(1).setCellValue(cf.getUserName() != null ? cf.getUserName() : "本人");
                row.createCell(2).setCellValue(cf.getBrand() != null ? cf.getBrand() : "");
                row.createCell(3).setCellValue(cf.getFoodType() != null ? cf.getFoodType() : "");
                row.createCell(4).setCellValue(cf.getStartDate() != null ? DATE_FORMAT.format(cf.getStartDate()) : "");
                row.createCell(5).setCellValue(cf.getEndDate() != null ? DATE_FORMAT.format(cf.getEndDate()) : "持续中");
                row.createCell(6).setCellValue(cf.getNotes() != null ? cf.getNotes() : "");
            }
        }
        catFoodSheet.setColumnWidth(0, 18 * 256);
        catFoodSheet.setColumnWidth(1, 8 * 256);
        catFoodSheet.setColumnWidth(2, 16 * 256);
        catFoodSheet.setColumnWidth(3, 8 * 256);
        catFoodSheet.setColumnWidth(4, 18 * 256);
        catFoodSheet.setColumnWidth(5, 12 * 256);
        catFoodSheet.setColumnWidth(6, 16 * 256);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        userSheet.getRow(0).setRowStyle(headerStyle);
        healthSheet.getRow(0).setRowStyle(headerStyle);
        catSheet.getRow(0).setRowStyle(headerStyle);
        teethSheet.getRow(0).setRowStyle(headerStyle);
        exerciseSheet.getRow(0).setRowStyle(headerStyle);
        breathingSheet.getRow(0).setRowStyle(headerStyle);
        eventSheet.getRow(0).setRowStyle(headerStyle);
        bookSheet.getRow(0).setRowStyle(headerStyle);
        catFoodSheet.getRow(0).setRowStyle(headerStyle);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(16384);
        workbook.write(baos);
        baos.flush();
        workbook.close();
        return baos.toByteArray();
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public static byte[] buildCsvBytes(List<Record> records, List<User> users) throws Exception {
        return buildCsvBytes(records, users, null, null, null, null, null, null, null);
    }
    public static byte[] buildExcelBytes(List<Record> records, List<User> users) throws Exception {
        return buildExcelBytes(records, users, null, null, null, null, null, null, null);
    }
}
