package com.example.healthrecorder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "health_recorder.db";
    private static final int DATABASE_VERSION = 11;

    static final String TABLE_RECORDS = "records";
    static final String TABLE_USERS = "users";
    static final String TABLE_CAT_FOOD = "cat_food_records";

    public static final String COLUMN_ID = "id";
    static final String COLUMN_WEIGHT = "weight";
    static final String COLUMN_SYSTOLIC = "systolic";
    static final String COLUMN_DIASTOLIC = "diastolic";
    static final String COLUMN_NOTES = "notes";
    static final String COLUMN_DATE = "date";
    static final String COLUMN_USER_ID = "user_id";

    // 统一：users 表列名（常量+建表+查询 三处必须保持一致）
    public static final String COLUMN_USER_NAME = "user_name";
    public static final String COLUMN_USER_COLOR = "color";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ============================================================
    // 表创建 / 升级
    // ============================================================

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_NAME + " TEXT NOT NULL,"
                + COLUMN_USER_COLOR + " INTEGER DEFAULT 0"
                + ")");
        db.execSQL("INSERT INTO " + TABLE_USERS + "(" + COLUMN_USER_NAME + "," + COLUMN_USER_COLOR + ") VALUES ('本人', 0)");

        db.execSQL("CREATE TABLE " + TABLE_RECORDS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_WEIGHT + " REAL,"
                + COLUMN_SYSTOLIC + " INTEGER,"
                + COLUMN_DIASTOLIC + " INTEGER,"
                + COLUMN_NOTES + " TEXT,"
                + COLUMN_DATE + " INTEGER,"
                + COLUMN_USER_ID + " INTEGER DEFAULT 1"
                + ")");

        // 猫咪如厕记录表
        db.execSQL("CREATE TABLE IF NOT EXISTS cat_records ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "date INTEGER,"
                + "cat_name TEXT,"
                + "stool_type INTEGER DEFAULT 3,"
                + "stool_count INTEGER DEFAULT 8,"
                + "willingness INTEGER DEFAULT 0,"
                + "process_type INTEGER DEFAULT 0,"
                + "notes TEXT,"
                + "user_id INTEGER DEFAULT 1"
                + ")");

        // 刷牙记录表
        db.execSQL("CREATE TABLE IF NOT EXISTS teeth_records ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "date INTEGER,"
                + "period INTEGER DEFAULT 0,"
                + "duration INTEGER,"
                + "notes TEXT,"
                + "user_id INTEGER DEFAULT 1"
                + ")");

        // 运动记录表
        db.execSQL("CREATE TABLE IF NOT EXISTS exercise_records ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "date INTEGER,"
                + "type INTEGER DEFAULT 0,"
                + "distance REAL,"
                + "duration INTEGER,"
                + "pace REAL,"
                + "heart_rate INTEGER,"
                + "calories INTEGER,"
                + "elevation_gain INTEGER,"
                + "difficulty INTEGER DEFAULT 3,"
                + "notes TEXT,"
                + "location TEXT,"
                + "user_id INTEGER DEFAULT 1"
                + ")");

        // 呼吸打卡记录表
        db.execSQL("CREATE TABLE IF NOT EXISTS breathing_records ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "date INTEGER,"
                + "duration INTEGER,"
                + "score INTEGER DEFAULT 0,"
                + "mode INTEGER DEFAULT 0,"
                + "notes TEXT,"
                + "user_id INTEGER DEFAULT 1,"
                + "custom_content TEXT,"
                + "custom_duration INTEGER"
                + ")");

        // 重要事项记录表
        db.execSQL("CREATE TABLE IF NOT EXISTS events ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "title TEXT NOT NULL,"
                + "content TEXT,"
                + "event_time INTEGER,"
                + "remind_time INTEGER,"
                + "remind_enabled INTEGER DEFAULT 0,"
                + "priority INTEGER DEFAULT 0,"
                + "status INTEGER DEFAULT 0,"
                + "repeat_type INTEGER DEFAULT 0,"
                + "repeat_interval INTEGER DEFAULT 0,"
                + "created_at INTEGER,"
                + "user_id INTEGER DEFAULT 1"
                + ")");

        // 读书记录表
        db.execSQL("CREATE TABLE IF NOT EXISTS book_reading_records ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "title TEXT NOT NULL,"
                + "author TEXT,"
                + "total_pages INTEGER,"
                + "current_page INTEGER,"
                + "status INTEGER DEFAULT 0,"
                + "rating INTEGER DEFAULT 0,"
                + "notes TEXT,"
                + "created_at INTEGER,"
                + "finished_at INTEGER,"
                + "user_id INTEGER DEFAULT 1,"
                + "finished_page INTEGER,"
                + "finished_notes TEXT"
                + ")");

        // 猫粮记录表（end_date 为 NULL 表示当前在吃）
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CAT_FOOD + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "brand TEXT,"
                + "food_type TEXT,"
                + "start_date INTEGER,"
                + "end_date INTEGER,"
                + "notes TEXT,"
                + "user_id INTEGER DEFAULT 1"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_USER_ID + " INTEGER DEFAULT 1");
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_USER_NAME + " TEXT NOT NULL,"
                    + COLUMN_USER_COLOR + " INTEGER DEFAULT 0"
                    + ")");
            db.execSQL("INSERT INTO " + TABLE_USERS + "(" + COLUMN_USER_NAME + "," + COLUMN_USER_COLOR + ") VALUES ('本人', 0)");
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS cat_records ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "date INTEGER,"
                    + "cat_name TEXT,"
                    + "stool_type INTEGER DEFAULT 3,"
                    + "stool_count INTEGER DEFAULT 8,"
                    + "notes TEXT,"
                    + "user_id INTEGER DEFAULT 1"
                    + ")");
            db.execSQL("CREATE TABLE IF NOT EXISTS teeth_records ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "date INTEGER,"
                    + "period INTEGER DEFAULT 0,"
                    + "duration INTEGER,"
                    + "notes TEXT,"
                    + "user_id INTEGER DEFAULT 1"
                    + ")");
            db.execSQL("CREATE TABLE IF NOT EXISTS exercise_records ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "date INTEGER,"
                    + "type INTEGER DEFAULT 0,"
                    + "distance REAL,"
                    + "duration INTEGER,"
                    + "pace REAL,"
                    + "heart_rate INTEGER,"
                    + "calories INTEGER,"
                    + "elevation_gain INTEGER,"
                    + "difficulty INTEGER DEFAULT 3,"
                    + "notes TEXT,"
                    + "location TEXT,"
                    + "user_id INTEGER DEFAULT 1"
                    + ")");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE cat_records ADD COLUMN willingness INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE cat_records ADD COLUMN process_type INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE cat_records ADD COLUMN stool_count INTEGER DEFAULT 8");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE exercise_records ADD COLUMN location TEXT");
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE cat_records ADD COLUMN stool_count INTEGER DEFAULT 8");
        }
        if (oldVersion < 8) {
            // 呼吸打卡记录表
            db.execSQL("CREATE TABLE IF NOT EXISTS breathing_records ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "date INTEGER,"
                    + "duration INTEGER,"
                    + "score INTEGER DEFAULT 0,"
                    + "mode INTEGER DEFAULT 0,"
                    + "notes TEXT,"
                    + "user_id INTEGER DEFAULT 1,"
                    + "custom_content TEXT,"
                    + "custom_duration INTEGER"
                    + ")");
            // 重要事项记录表
            db.execSQL("CREATE TABLE IF NOT EXISTS events ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "title TEXT NOT NULL,"
                    + "content TEXT,"
                    + "event_time INTEGER,"
                    + "remind_time INTEGER,"
                    + "remind_enabled INTEGER DEFAULT 0,"
                    + "priority INTEGER DEFAULT 0,"
                    + "status INTEGER DEFAULT 0,"
                    + "repeat_type INTEGER DEFAULT 0,"
                    + "repeat_interval INTEGER DEFAULT 0,"
                    + "created_at INTEGER,"
                    + "user_id INTEGER DEFAULT 1"
                    + ")");
            // 读书记录表
            db.execSQL("CREATE TABLE IF NOT EXISTS book_reading_records ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "title TEXT NOT NULL,"
                    + "author TEXT,"
                    + "total_pages INTEGER,"
                    + "current_page INTEGER,"
                    + "status INTEGER DEFAULT 0,"
                    + "rating INTEGER DEFAULT 0,"
                    + "notes TEXT,"
                    + "created_at INTEGER,"
                    + "finished_at INTEGER,"
                    + "user_id INTEGER DEFAULT 1,"
                    + "finished_page INTEGER,"
                    + "finished_notes TEXT"
                    + ")");
        }
        if (oldVersion < 9) {
            // 猫粮记录表
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CAT_FOOD + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "brand TEXT,"
                    + "food_type TEXT,"
                    + "start_date INTEGER,"
                    + "end_date INTEGER,"
                    + "notes TEXT,"
                    + "user_id INTEGER DEFAULT 1"
                    + ")");
            // 呼吸记录新增自定义列（v9 升级）
            try { db.execSQL("ALTER TABLE breathing_records ADD COLUMN custom_content TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE breathing_records ADD COLUMN custom_duration INTEGER"); } catch (Exception ignored) {}
        }

        // v10：确保呼吸自定义列存在（兼容从 v9 但无 custom 列的旧库；幂等）
        if (oldVersion < 10) {
            try { db.execSQL("ALTER TABLE breathing_records ADD COLUMN custom_content TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE breathing_records ADD COLUMN custom_duration INTEGER"); } catch (Exception ignored) {}
        }
        if (oldVersion < 11) {
            try { db.execSQL("ALTER TABLE book_reading_records ADD COLUMN finished_page INTEGER"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE book_reading_records ADD COLUMN finished_notes TEXT"); } catch (Exception ignored) {}
        }
    }

    // ============================================================
    // User CRUD
    // ============================================================

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_USERS, null);
        if (cursor.moveToFirst()) {
            do {
                User u = new User();
                u.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                u.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)));
                u.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_COLOR)));
                users.add(u);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return users;
    }

    public long addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_USER_NAME, user.getName());
        v.put(COLUMN_USER_COLOR, user.getColor());
        long id = db.insert(TABLE_USERS, null, v);
        db.close();
        return id;
    }

    public void updateUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_USER_NAME, user.getName());
        v.put(COLUMN_USER_COLOR, user.getColor());
        db.update(TABLE_USERS, v, COLUMN_ID + "=?", new String[]{String.valueOf(user.getId())});
        db.close();
    }

    public void deleteUser(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_RECORDS + " SET " + COLUMN_USER_ID + "=1 WHERE " + COLUMN_USER_ID + "=" + id);
        db.delete(TABLE_USERS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public User getUserById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        User u = null;
        if (cursor.moveToFirst()) {
            u = new User();
            u.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            u.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)));
            u.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_COLOR)));
        }
        cursor.close();
        db.close();
        return u;
    }

    /** 按姓名查找成员（用于导入时按姓名匹配成员归属） */
    public User getUserByName(String name) {
        if (name == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_USER_NAME + "=?",
                new String[]{name}, null, null, null);
        User u = null;
        if (cursor.moveToFirst()) {
            u = new User();
            u.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            u.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)));
            u.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_COLOR)));
        }
        cursor.close();
        db.close();
        return u;
    }

    // ============================================================
    // Record CRUD
    // ============================================================

    public long addRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WEIGHT, record.getWeight());
        values.put(COLUMN_SYSTOLIC, record.getSystolic());
        values.put(COLUMN_DIASTOLIC, record.getDiastolic());
        values.put(COLUMN_NOTES, record.getNotes());
        values.put(COLUMN_DATE, record.getDate().getTime());
        values.put(COLUMN_USER_ID, record.getUserId());
        long id = db.insert(TABLE_RECORDS, null, values);
        db.close();
        return id;
    }

    public void updateRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WEIGHT, record.getWeight());
        values.put(COLUMN_SYSTOLIC, record.getSystolic());
        values.put(COLUMN_DIASTOLIC, record.getDiastolic());
        values.put(COLUMN_NOTES, record.getNotes());
        values.put(COLUMN_DATE, record.getDate().getTime());
        values.put(COLUMN_USER_ID, record.getUserId());
        db.update(TABLE_RECORDS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(record.getId())});
        db.close();
    }

    public Record getRecordById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT r." + COLUMN_ID
                + ", r." + COLUMN_WEIGHT
                + ", r." + COLUMN_SYSTOLIC
                + ", r." + COLUMN_DIASTOLIC
                + ", r." + COLUMN_NOTES
                + ", r." + COLUMN_DATE
                + ", r." + COLUMN_USER_ID
                + ", u." + COLUMN_USER_NAME + " AS " + COLUMN_USER_NAME
                + ", u." + COLUMN_USER_COLOR + " AS " + COLUMN_USER_COLOR
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_USERS + " u ON r." + COLUMN_USER_ID + " = u." + COLUMN_ID
                + " WHERE r." + COLUMN_ID + " = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(id)});
        Record record = null;
        if (cursor.moveToFirst()) {
            record = cursorToRecord(cursor);
        }
        cursor.close();
        db.close();
        return record;
    }

    public List<Record> getAllRecords() {
        return getRecordsByUserId(-1);
    }

    /**
     * @param userId <= 0 时返回全部记录；> 0 时返回指定用户的记录
     */
    public List<Record> getRecordsByUserId(int userId) {
        List<Record> recordList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql;
        Cursor cursor;
        if (userId <= 0) {
            sql = "SELECT r." + COLUMN_ID
                    + ", r." + COLUMN_WEIGHT
                    + ", r." + COLUMN_SYSTOLIC
                    + ", r." + COLUMN_DIASTOLIC
                    + ", r." + COLUMN_NOTES
                    + ", r." + COLUMN_DATE
                    + ", r." + COLUMN_USER_ID
                    + ", u." + COLUMN_USER_NAME + " AS " + COLUMN_USER_NAME
                    + ", u." + COLUMN_USER_COLOR + " AS " + COLUMN_USER_COLOR
                    + " FROM " + TABLE_RECORDS + " r"
                    + " LEFT JOIN " + TABLE_USERS + " u ON r." + COLUMN_USER_ID + " = u." + COLUMN_ID
                    + " ORDER BY r." + COLUMN_DATE + " DESC";
            cursor = db.rawQuery(sql, null);
        } else {
            sql = "SELECT r." + COLUMN_ID
                    + ", r." + COLUMN_WEIGHT
                    + ", r." + COLUMN_SYSTOLIC
                    + ", r." + COLUMN_DIASTOLIC
                    + ", r." + COLUMN_NOTES
                    + ", r." + COLUMN_DATE
                    + ", r." + COLUMN_USER_ID
                    + ", u." + COLUMN_USER_NAME + " AS " + COLUMN_USER_NAME
                    + ", u." + COLUMN_USER_COLOR + " AS " + COLUMN_USER_COLOR
                    + " FROM " + TABLE_RECORDS + " r"
                    + " LEFT JOIN " + TABLE_USERS + " u ON r." + COLUMN_USER_ID + " = u." + COLUMN_ID
                    + " WHERE r." + COLUMN_USER_ID + " = ?"
                    + " ORDER BY r." + COLUMN_DATE + " DESC";
            cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
        }
        if (cursor.moveToFirst()) {
            do {
                Record r = cursorToRecord(cursor);
                if (r != null) recordList.add(r);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return recordList;
    }

    public List<Record> getRecordsForChart() {
        return getRecordsForChartByUserId(-1);
    }

    public List<Record> getRecordsForChartByUserId(int userId) {
        List<Record> recordList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql;
        Cursor cursor;
        if (userId <= 0) {
            sql = "SELECT r." + COLUMN_ID
                    + ", r." + COLUMN_WEIGHT
                    + ", r." + COLUMN_SYSTOLIC
                    + ", r." + COLUMN_DIASTOLIC
                    + ", r." + COLUMN_NOTES
                    + ", r." + COLUMN_DATE
                    + ", r." + COLUMN_USER_ID
                    + ", u." + COLUMN_USER_NAME + " AS " + COLUMN_USER_NAME
                    + ", u." + COLUMN_USER_COLOR + " AS " + COLUMN_USER_COLOR
                    + " FROM " + TABLE_RECORDS + " r"
                    + " LEFT JOIN " + TABLE_USERS + " u ON r." + COLUMN_USER_ID + " = u." + COLUMN_ID
                    + " ORDER BY r." + COLUMN_DATE + " ASC";
            cursor = db.rawQuery(sql, null);
        } else {
            sql = "SELECT r." + COLUMN_ID
                    + ", r." + COLUMN_WEIGHT
                    + ", r." + COLUMN_SYSTOLIC
                    + ", r." + COLUMN_DIASTOLIC
                    + ", r." + COLUMN_NOTES
                    + ", r." + COLUMN_DATE
                    + ", r." + COLUMN_USER_ID
                    + ", u." + COLUMN_USER_NAME + " AS " + COLUMN_USER_NAME
                    + ", u." + COLUMN_USER_COLOR + " AS " + COLUMN_USER_COLOR
                    + " FROM " + TABLE_RECORDS + " r"
                    + " LEFT JOIN " + TABLE_USERS + " u ON r." + COLUMN_USER_ID + " = u." + COLUMN_ID
                    + " WHERE r." + COLUMN_USER_ID + " = ?"
                    + " ORDER BY r." + COLUMN_DATE + " ASC";
            cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
        }
        if (cursor.moveToFirst()) {
            do {
                Record r = cursorToRecord(cursor);
                if (r != null) recordList.add(r);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return recordList;
    }

    /**
     * 从 Cursor 提取 Record
     */
    private Record cursorToRecord(Cursor cursor) {
        Record record = new Record();
        record.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        record.setWeight(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT)));
        record.setSystolic(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYSTOLIC)));
        record.setDiastolic(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DIASTOLIC)));
        record.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)));
        record.setDate(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE))));
        record.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
        int nameIdx = cursor.getColumnIndex(COLUMN_USER_NAME);
        if (nameIdx >= 0) {
            String name = cursor.getString(nameIdx);
            record.setUserName(name != null ? name : "本人");
        } else {
            record.setUserName("本人");
        }
        return record;
    }

    public void deleteRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECORDS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public int getRecordCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_RECORDS, null);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        db.close();
        return count;
    }

    /** 判断是否存在完全相同的记录（用于导入去重） */
    public boolean recordExists(Record r) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_RECORDS
                        + " WHERE CAST(" + COLUMN_DATE + " AS TEXT)=?"
                        + " AND CAST(" + COLUMN_USER_ID + " AS TEXT)=?"
                        + " AND CAST(" + COLUMN_WEIGHT + " AS TEXT)=?"
                        + " AND CAST(" + COLUMN_SYSTOLIC + " AS TEXT)=?"
                        + " AND CAST(" + COLUMN_DIASTOLIC + " AS TEXT)=?",
                new String[]{
                        String.valueOf(r.getDate().getTime()),
                        String.valueOf(r.getUserId()),
                        String.valueOf(r.getWeight()),
                        String.valueOf(r.getSystolic()),
                        String.valueOf(r.getDiastolic())});
        int count = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        db.close();
        return count > 0;
    }

    // ============================================================
    // Cat Record CRUD
    // ============================================================

    public List<com.example.healthrecorder.cat.CatRecord> getAllCatRecords() {
        List<com.example.healthrecorder.cat.CatRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT c.*, u." + COLUMN_USER_NAME + " AS un "
                + "FROM cat_records c LEFT JOIN " + TABLE_USERS + " u ON c.user_id=u." + COLUMN_ID
                + " ORDER BY c.date DESC", null);
        if (c.moveToFirst()) {
            do {
                com.example.healthrecorder.cat.CatRecord r = new com.example.healthrecorder.cat.CatRecord();
                r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                r.setDate(new Date(c.getLong(c.getColumnIndexOrThrow("date"))));
                r.setCatName(c.getString(c.getColumnIndexOrThrow("cat_name")));
                r.setStoolType(c.getInt(c.getColumnIndexOrThrow("stool_type")));
                int scIdx = c.getColumnIndex("stool_count");
                r.setStoolCount(scIdx >= 0 ? c.getInt(scIdx) : 8);
                r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
                r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
                int unIdx = c.getColumnIndex("un");
                if (unIdx >= 0) r.setUserName(c.getString(unIdx));
                int wlIdx = c.getColumnIndex("willingness");
                if (wlIdx >= 0) r.setWillingness(c.getInt(wlIdx));
                int ptIdx = c.getColumnIndex("process_type");
                if (ptIdx >= 0) r.setProcessType(c.getInt(ptIdx));
                list.add(r);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public long addCatRecord(com.example.healthrecorder.cat.CatRecord r) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("date", r.getDate().getTime());
        v.put("cat_name", r.getCatName());
        v.put("stool_type", r.getStoolType());
        v.put("stool_count", r.getStoolCount());
        v.put("willingness", r.getWillingness());
        v.put("process_type", r.getProcessType());
        v.put("notes", r.getNotes());
        v.put("user_id", r.getUserId());
        long id = db.insert("cat_records", null, v);
        db.close();
        return id;
    }

    public void deleteCatRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("cat_records", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public com.example.healthrecorder.cat.CatRecord getCatRecordById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT c.*, u." + COLUMN_USER_NAME + " AS un, u." + COLUMN_USER_COLOR + " AS uc "
                + "FROM cat_records c LEFT JOIN " + TABLE_USERS + " u ON c.user_id=u." + COLUMN_ID
                + " WHERE c.id=?", new String[]{String.valueOf(id)});
        com.example.healthrecorder.cat.CatRecord r = null;
        if (c.moveToFirst()) {
            r = new com.example.healthrecorder.cat.CatRecord();
            r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            r.setDate(new Date(c.getLong(c.getColumnIndexOrThrow("date"))));
            r.setCatName(c.getString(c.getColumnIndexOrThrow("cat_name")));
            r.setStoolType(c.getInt(c.getColumnIndexOrThrow("stool_type")));
            int scIdx = c.getColumnIndex("stool_count");
            r.setStoolCount(scIdx >= 0 ? c.getInt(scIdx) : 8);
            r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
            r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
            int unIdx = c.getColumnIndex("un");
            if (unIdx >= 0) r.setUserName(c.getString(unIdx));
            int ucIdx = c.getColumnIndex("uc");
            if (ucIdx >= 0 && !c.isNull(ucIdx)) r.setUserColor(c.getInt(ucIdx));
            int wlIdx = c.getColumnIndex("willingness");
            if (wlIdx >= 0) r.setWillingness(c.getInt(wlIdx));
            int ptIdx = c.getColumnIndex("process_type");
            if (ptIdx >= 0) r.setProcessType(c.getInt(ptIdx));
        }
        c.close();
        db.close();
        return r;
    }

    public int updateCatRecord(int id, int score, int count, int willingness, int processType, String notes, int userId, long dateMillis) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("date", dateMillis);
        v.put("stool_type", score);
        v.put("stool_count", count);
        v.put("willingness", willingness);
        v.put("process_type", processType);
        v.put("notes", notes);
        v.put("user_id", userId);
        int n = db.update("cat_records", v, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return n;
    }

    // ============================================================
    // Teeth Record CRUD
    // ============================================================

    public List<com.example.healthrecorder.teeth.TeethRecord> getAllTeethRecords() {
        List<com.example.healthrecorder.teeth.TeethRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT t.*, u." + COLUMN_USER_NAME + " AS un "
                + "FROM teeth_records t LEFT JOIN " + TABLE_USERS + " u ON t.user_id=u." + COLUMN_ID
                + " ORDER BY t.date DESC", null);
        if (c.moveToFirst()) {
            do {
                com.example.healthrecorder.teeth.TeethRecord r = new com.example.healthrecorder.teeth.TeethRecord();
                r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                r.setDate(new Date(c.getLong(c.getColumnIndexOrThrow("date"))));
                r.setPeriod(c.getInt(c.getColumnIndexOrThrow("period")));
                r.setDuration(c.getInt(c.getColumnIndexOrThrow("duration")));
                r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
                r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
                int unIdx = c.getColumnIndex("un");
                if (unIdx >= 0) r.setUserName(c.getString(unIdx));
                list.add(r);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public long addTeethRecord(com.example.healthrecorder.teeth.TeethRecord r) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("date", r.getDate().getTime());
        v.put("period", r.getPeriod());
        v.put("duration", r.getDuration());
        v.put("notes", r.getNotes());
        v.put("user_id", r.getUserId());
        long id = db.insert("teeth_records", null, v);
        db.close();
        return id;
    }

    public void deleteTeethRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("teeth_records", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public com.example.healthrecorder.teeth.TeethRecord getTeethRecordById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT t.*, u." + COLUMN_USER_NAME + " AS un "
                + "FROM teeth_records t LEFT JOIN " + TABLE_USERS + " u ON t.user_id=u." + COLUMN_ID
                + " WHERE t.id=?", new String[]{String.valueOf(id)});
        com.example.healthrecorder.teeth.TeethRecord r = null;
        if (c.moveToFirst()) {
            r = new com.example.healthrecorder.teeth.TeethRecord();
            r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            r.setDate(new Date(c.getLong(c.getColumnIndexOrThrow("date"))));
            r.setPeriod(c.getInt(c.getColumnIndexOrThrow("period")));
            r.setDuration(c.getInt(c.getColumnIndexOrThrow("duration")));
            r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
            r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
            int unIdx = c.getColumnIndex("un");
            if (unIdx >= 0) r.setUserName(c.getString(unIdx));
        }
        c.close();
        db.close();
        return r;
    }

    public int updateTeethRecord(int id, int period, int duration, String notes, int userId, long dateMillis) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("date", dateMillis);
        v.put("period", period);
        v.put("duration", duration);
        v.put("notes", notes);
        v.put("user_id", userId);
        int n = db.update("teeth_records", v, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return n;
    }

    // ============================================================
    // Exercise Record CRUD
    // ============================================================

    public List<com.example.healthrecorder.exercise.ExerciseRecord> getAllExerciseRecords() {
        List<com.example.healthrecorder.exercise.ExerciseRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT e.*, u." + COLUMN_USER_NAME + " AS un "
                + "FROM exercise_records e LEFT JOIN " + TABLE_USERS + " u ON e.user_id=u." + COLUMN_ID
                + " ORDER BY e.date DESC", null);
        if (c.moveToFirst()) {
            do {
                com.example.healthrecorder.exercise.ExerciseRecord r = new com.example.healthrecorder.exercise.ExerciseRecord();
                r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                r.setDate(new Date(c.getLong(c.getColumnIndexOrThrow("date"))));
                r.setType(c.getInt(c.getColumnIndexOrThrow("type")));
                r.setDistance(c.getFloat(c.getColumnIndexOrThrow("distance")));
                r.setDuration(c.getInt(c.getColumnIndexOrThrow("duration")));
                r.setPace(c.getFloat(c.getColumnIndexOrThrow("pace")));
                r.setHeartRate(c.getInt(c.getColumnIndexOrThrow("heart_rate")));
                r.setCalories(c.getInt(c.getColumnIndexOrThrow("calories")));
                r.setElevationGain(c.getInt(c.getColumnIndexOrThrow("elevation_gain")));
                r.setDifficulty(c.getInt(c.getColumnIndexOrThrow("difficulty")));
                r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
                int locIdx = c.getColumnIndex("location");
                if (locIdx >= 0) r.setLocation(c.getString(locIdx));
                r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
                int unIdx = c.getColumnIndex("un");
                if (unIdx >= 0) r.setUserName(c.getString(unIdx));
                list.add(r);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public long addExerciseRecord(com.example.healthrecorder.exercise.ExerciseRecord r) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("date", r.getDate().getTime());
        v.put("type", r.getType());
        v.put("distance", r.getDistance());
        v.put("duration", r.getDuration());
        v.put("pace", r.getPace());
        v.put("heart_rate", r.getHeartRate());
        v.put("calories", r.getCaloriesField());
        v.put("elevation_gain", r.getElevationGain());
        v.put("difficulty", r.getDifficulty());
        v.put("notes", r.getNotes());
        v.put("location", r.getLocation());
        v.put("user_id", r.getUserId());
        long id = db.insert("exercise_records", null, v);
        db.close();
        return id;
    }

    public void deleteExerciseRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("exercise_records", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public com.example.healthrecorder.exercise.ExerciseRecord getExerciseRecordById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT e.*, u." + COLUMN_USER_NAME + " AS un "
                + "FROM exercise_records e LEFT JOIN " + TABLE_USERS + " u ON e.user_id=u." + COLUMN_ID
                + " WHERE e.id=?", new String[]{String.valueOf(id)});
        com.example.healthrecorder.exercise.ExerciseRecord r = null;
        if (c.moveToFirst()) {
            r = new com.example.healthrecorder.exercise.ExerciseRecord();
            r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            r.setDate(new Date(c.getLong(c.getColumnIndexOrThrow("date"))));
            r.setType(c.getInt(c.getColumnIndexOrThrow("type")));
            r.setDistance(c.getFloat(c.getColumnIndexOrThrow("distance")));
            r.setDuration(c.getInt(c.getColumnIndexOrThrow("duration")));
            r.setPace(c.getFloat(c.getColumnIndexOrThrow("pace")));
            r.setHeartRate(c.getInt(c.getColumnIndexOrThrow("heart_rate")));
            r.setCalories(c.getInt(c.getColumnIndexOrThrow("calories")));
            r.setElevationGain(c.getInt(c.getColumnIndexOrThrow("elevation_gain")));
            r.setDifficulty(c.getInt(c.getColumnIndexOrThrow("difficulty")));
            r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
            int locIdx = c.getColumnIndex("location");
            if (locIdx >= 0) r.setLocation(c.getString(locIdx));
            r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
            int unIdx = c.getColumnIndex("un");
            if (unIdx >= 0) r.setUserName(c.getString(unIdx));
        }
        c.close();
        db.close();
        return r;
    }

    public int updateExerciseRecord(int id, int type, float distance, int duration, float pace,
                                   int heartRate, int calories, int elevation, int difficulty,
                                   String notes, String location, int userId, long dateMillis) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("date", dateMillis);
        v.put("type", type);
        v.put("distance", distance);
        v.put("duration", duration);
        v.put("pace", pace);
        v.put("heart_rate", heartRate);
        v.put("calories", calories);
        v.put("elevation_gain", elevation);
        v.put("difficulty", difficulty);
        v.put("notes", notes);
        v.put("location", location);
        v.put("user_id", userId);
        int n = db.update("exercise_records", v, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return n;
    }

    // ============================================================
    // Breathing Record CRUD
    // ============================================================

    public List<com.example.healthrecorder.breathing.BreathingRecord> getAllBreathingRecords() {
        List<com.example.healthrecorder.breathing.BreathingRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT b.*, u." + COLUMN_USER_NAME + " AS un "
                + "FROM breathing_records b LEFT JOIN " + TABLE_USERS + " u ON b.user_id=u." + COLUMN_ID
                + " ORDER BY b.date DESC", null);
        if (c.moveToFirst()) {
            do {
                com.example.healthrecorder.breathing.BreathingRecord r = new com.example.healthrecorder.breathing.BreathingRecord();
                r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                r.setDate(new Date(c.getLong(c.getColumnIndexOrThrow("date"))));
                r.setDuration(c.getInt(c.getColumnIndexOrThrow("duration")));
                r.setScore(c.getInt(c.getColumnIndexOrThrow("score")));
                r.setMode(c.getInt(c.getColumnIndexOrThrow("mode")));
                r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
                r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
                int unIdx = c.getColumnIndex("un");
                if (unIdx >= 0) r.setUserName(c.getString(unIdx));
                int ccIdx = c.getColumnIndex("custom_content");
                if (ccIdx >= 0 && !c.isNull(ccIdx)) r.setCustomContent(c.getString(ccIdx));
                int cdIdx = c.getColumnIndex("custom_duration");
                if (cdIdx >= 0 && !c.isNull(cdIdx)) r.setCustomDuration(c.getInt(cdIdx));
                list.add(r);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public long addBreathingRecord(com.example.healthrecorder.breathing.BreathingRecord r) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("date", r.getDate().getTime());
        v.put("duration", r.getDuration());
        v.put("score", r.getScore());
        v.put("mode", r.getMode());
        v.put("notes", r.getNotes());
        v.put("user_id", r.getUserId());
        if (r.getCustomContent() != null) v.put("custom_content", r.getCustomContent());
        if (r.getCustomDuration() > 0) v.put("custom_duration", r.getCustomDuration());
        long id = db.insert("breathing_records", null, v);
        db.close();
        return id;
    }

    public void deleteBreathingRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("breathing_records", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public int getBreathingRecordCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM breathing_records", null);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        db.close();
        return count;
    }

    // ============================================================
    // Event CRUD
    // ============================================================

    public List<com.example.healthrecorder.event.Event> getAllEvents() {
        List<com.example.healthrecorder.event.Event> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT e.*, u." + COLUMN_USER_NAME + " AS un "
                + "FROM events e LEFT JOIN " + TABLE_USERS + " u ON e.user_id=u." + COLUMN_ID
                + " ORDER BY e.event_time ASC", null);
        if (c.moveToFirst()) {
            do {
                com.example.healthrecorder.event.Event ev = new com.example.healthrecorder.event.Event();
                ev.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                ev.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
                ev.setContent(c.getString(c.getColumnIndexOrThrow("content")));
                int etIdx = c.getColumnIndex("event_time");
                if (etIdx >= 0 && !c.isNull(etIdx)) ev.setEventTime(new Date(c.getLong(etIdx)));
                int rtIdx = c.getColumnIndex("remind_time");
                if (rtIdx >= 0 && !c.isNull(rtIdx)) ev.setRemindTime(new Date(c.getLong(rtIdx)));
                ev.setRemindEnabled(c.getInt(c.getColumnIndexOrThrow("remind_enabled")) == 1);
                ev.setPriority(c.getInt(c.getColumnIndexOrThrow("priority")));
                ev.setStatus(c.getInt(c.getColumnIndexOrThrow("status")));
                ev.setRepeatType(c.getInt(c.getColumnIndexOrThrow("repeat_type")));
                ev.setRepeatInterval(c.getInt(c.getColumnIndexOrThrow("repeat_interval")));
                int caIdx = c.getColumnIndex("created_at");
                if (caIdx >= 0 && !c.isNull(caIdx)) ev.setCreatedAt(new Date(c.getLong(caIdx)));
                ev.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
                int unIdx = c.getColumnIndex("un");
                if (unIdx >= 0) ev.setUserName(c.getString(unIdx));
                list.add(ev);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public long addEvent(com.example.healthrecorder.event.Event ev) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", ev.getTitle());
        v.put("content", ev.getContent());
        v.put("event_time", ev.getEventTime() != null ? ev.getEventTime().getTime() : null);
        v.put("remind_time", ev.getRemindTime() != null ? ev.getRemindTime().getTime() : null);
        v.put("remind_enabled", ev.isRemindEnabled() ? 1 : 0);
        v.put("priority", ev.getPriority());
        v.put("status", ev.getStatus());
        v.put("repeat_type", ev.getRepeatType());
        v.put("repeat_interval", ev.getRepeatInterval());
        v.put("created_at", ev.getCreatedAt() != null ? ev.getCreatedAt().getTime() : System.currentTimeMillis());
        v.put("user_id", ev.getUserId());
        long id = db.insert("events", null, v);
        db.close();
        return id;
    }

    public void updateEvent(com.example.healthrecorder.event.Event ev) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", ev.getTitle());
        v.put("content", ev.getContent());
        v.put("event_time", ev.getEventTime() != null ? ev.getEventTime().getTime() : null);
        v.put("remind_time", ev.getRemindTime() != null ? ev.getRemindTime().getTime() : null);
        v.put("remind_enabled", ev.isRemindEnabled() ? 1 : 0);
        v.put("priority", ev.getPriority());
        v.put("status", ev.getStatus());
        v.put("repeat_type", ev.getRepeatType());
        v.put("repeat_interval", ev.getRepeatInterval());
        db.update("events", v, "id=?", new String[]{String.valueOf(ev.getId())});
        db.close();
    }

    public void deleteEvent(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("events", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public int getEventCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM events", null);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        db.close();
        return count;
    }

    // ============================================================
    // Book Record CRUD
    // ============================================================

    public List<com.example.healthrecorder.book.BookRecord> getAllBookRecords() {
        List<com.example.healthrecorder.book.BookRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT b.*, u." + COLUMN_USER_NAME + " AS un "
                + "FROM book_reading_records b LEFT JOIN " + TABLE_USERS + " u ON b.user_id=u." + COLUMN_ID
                + " ORDER BY b.created_at DESC", null);
        if (c.moveToFirst()) {
            do {
                com.example.healthrecorder.book.BookRecord r = new com.example.healthrecorder.book.BookRecord();
                r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                r.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
                r.setAuthor(c.getString(c.getColumnIndexOrThrow("author")));
                r.setTotalPages(c.getInt(c.getColumnIndexOrThrow("total_pages")));
                r.setCurrentPage(c.getInt(c.getColumnIndexOrThrow("current_page")));
                r.setStatus(c.getInt(c.getColumnIndexOrThrow("status")));
                r.setRating(c.getInt(c.getColumnIndexOrThrow("rating")));
                r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
                int caIdx = c.getColumnIndex("created_at");
                if (caIdx >= 0 && !c.isNull(caIdx)) r.setCreatedAt(new Date(c.getLong(caIdx)));
                int faIdx = c.getColumnIndex("finished_at");
                if (faIdx >= 0 && !c.isNull(faIdx)) r.setFinishedAt(new Date(c.getLong(faIdx)));
                r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
                int unIdx = c.getColumnIndex("un");
                if (unIdx >= 0) r.setUserName(c.getString(unIdx));
                int fpIdx = c.getColumnIndex("finished_page");
                if (fpIdx >= 0 && !c.isNull(fpIdx)) r.setFinishedPage(c.getInt(fpIdx));
                int fnIdx = c.getColumnIndex("finished_notes");
                if (fnIdx >= 0 && !c.isNull(fnIdx)) r.setFinishedNotes(c.getString(fnIdx));
                list.add(r);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public long addBookRecord(com.example.healthrecorder.book.BookRecord r) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", r.getTitle());
        v.put("author", r.getAuthor());
        v.put("total_pages", r.getTotalPages());
        v.put("current_page", r.getCurrentPage());
        v.put("status", r.getStatus());
        v.put("rating", r.getRating());
        v.put("notes", r.getNotes());
        v.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().getTime() : System.currentTimeMillis());
        v.put("finished_at", r.getFinishedAt() != null ? r.getFinishedAt().getTime() : null);
        v.put("user_id", r.getUserId());
        v.put("finished_page", r.getFinishedPage());
        v.put("finished_notes", r.getFinishedNotes());
        long id = db.insert("book_reading_records", null, v);
        db.close();
        return id;
    }

    public void updateBookRecord(com.example.healthrecorder.book.BookRecord r) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", r.getTitle());
        v.put("author", r.getAuthor());
        v.put("total_pages", r.getTotalPages());
        v.put("current_page", r.getCurrentPage());
        v.put("status", r.getStatus());
        v.put("rating", r.getRating());
        v.put("notes", r.getNotes());
        v.put("finished_at", r.getFinishedAt() != null ? r.getFinishedAt().getTime() : null);
        v.put("finished_page", r.getFinishedPage());
        v.put("finished_notes", r.getFinishedNotes());
        db.update("book_reading_records", v, "id=?", new String[]{String.valueOf(r.getId())});
        db.close();
    }

    public void deleteBookRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("book_reading_records", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public int getBookRecordCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM book_reading_records", null);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        db.close();
        return count;
    }

    // ============================================================
    // Cat Food Record CRUD
    // ============================================================

    /** 从 Cursor 提取猫粮记录 */
    private com.example.healthrecorder.cat.CatFoodRecord cursorToCatFoodRecord(Cursor c) {
        com.example.healthrecorder.cat.CatFoodRecord r = new com.example.healthrecorder.cat.CatFoodRecord();
        r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        r.setBrand(c.getString(c.getColumnIndexOrThrow("brand")));
        r.setFoodType(c.getString(c.getColumnIndexOrThrow("food_type")));
        int sdIdx = c.getColumnIndex("start_date");
        if (sdIdx >= 0 && !c.isNull(sdIdx)) r.setStartDate(new Date(c.getLong(sdIdx)));
        int edIdx = c.getColumnIndex("end_date");
        if (edIdx >= 0 && !c.isNull(edIdx)) r.setEndDate(new Date(c.getLong(edIdx)));
        r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
        r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
        int unIdx = c.getColumnIndex("un");
        if (unIdx >= 0) r.setUserName(c.getString(unIdx));
        int ucIdx = c.getColumnIndex("uc");
        if (ucIdx >= 0 && !c.isNull(ucIdx)) r.setUserColor(c.getInt(ucIdx));
        return r;
    }

    public long insertCatFoodRecord(com.example.healthrecorder.cat.CatFoodRecord r) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("brand", r.getBrand());
        v.put("food_type", r.getFoodType());
        v.put("start_date", r.getStartDate() != null ? r.getStartDate().getTime() : System.currentTimeMillis());
        if (r.getEndDate() != null) v.put("end_date", r.getEndDate().getTime());
        else v.putNull("end_date");
        v.put("notes", r.getNotes());
        v.put("user_id", r.getUserId());
        long id = db.insert(TABLE_CAT_FOOD, null, v);
        db.close();
        return id;
    }

    public List<com.example.healthrecorder.cat.CatFoodRecord> getAllCatFoodRecords() {
        List<com.example.healthrecorder.cat.CatFoodRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT f.*, u." + COLUMN_USER_NAME + " AS un, u." + COLUMN_USER_COLOR + " AS uc "
                + "FROM " + TABLE_CAT_FOOD + " f LEFT JOIN " + TABLE_USERS + " u ON f.user_id=u." + COLUMN_ID
                + " ORDER BY (f.end_date IS NULL) DESC, f.start_date DESC", null);
        if (c.moveToFirst()) {
            do { list.add(cursorToCatFoodRecord(c)); }
            while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public com.example.healthrecorder.cat.CatFoodRecord getCatFoodRecordById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT f.*, u." + COLUMN_USER_NAME + " AS un, u." + COLUMN_USER_COLOR + " AS uc "
                + "FROM " + TABLE_CAT_FOOD + " f LEFT JOIN " + TABLE_USERS + " u ON f.user_id=u." + COLUMN_ID
                + " WHERE f.id=?", new String[]{String.valueOf(id)});
        com.example.healthrecorder.cat.CatFoodRecord r = null;
        if (c.moveToFirst()) { r = cursorToCatFoodRecord(c); }
        c.close();
        db.close();
        return r;
    }

    public int updateCatFoodRecord(com.example.healthrecorder.cat.CatFoodRecord r) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("brand", r.getBrand());
        v.put("food_type", r.getFoodType());
        v.put("start_date", r.getStartDate() != null ? r.getStartDate().getTime() : System.currentTimeMillis());
        if (r.getEndDate() != null) v.put("end_date", r.getEndDate().getTime());
        else v.putNull("end_date");
        v.put("notes", r.getNotes());
        v.put("user_id", r.getUserId());
        int n = db.update(TABLE_CAT_FOOD, v, "id=?", new String[]{String.valueOf(r.getId())});
        db.close();
        return n;
    }

    public void deleteCatFoodRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CAT_FOOD, "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public com.example.healthrecorder.cat.CatFoodRecord getCurrentCatFoodRecord() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT f.*, u." + COLUMN_USER_NAME + " AS un, u." + COLUMN_USER_COLOR + " AS uc "
                + "FROM " + TABLE_CAT_FOOD + " f LEFT JOIN " + TABLE_USERS + " u ON f.user_id=u." + COLUMN_ID
                + " WHERE f.end_date IS NULL ORDER BY f.start_date DESC LIMIT 1", null);
        com.example.healthrecorder.cat.CatFoodRecord r = null;
        if (c.moveToFirst()) { r = cursorToCatFoodRecord(c); }
        c.close();
        db.close();
        return r;
    }

    public com.example.healthrecorder.cat.CatFoodRecord getCatFoodRecordOnDate(long dateMillis) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT f.*, u." + COLUMN_USER_NAME + " AS un, u." + COLUMN_USER_COLOR + " AS uc "
                + "FROM " + TABLE_CAT_FOOD + " f LEFT JOIN " + TABLE_USERS + " u ON f.user_id=u." + COLUMN_ID
                + " WHERE f.start_date<=? AND (f.end_date IS NULL OR f.end_date>=?)"
                + " ORDER BY f.start_date DESC LIMIT 1",
                new String[]{String.valueOf(dateMillis), String.valueOf(dateMillis)});
        com.example.healthrecorder.cat.CatFoodRecord r = null;
        if (c.moveToFirst()) { r = cursorToCatFoodRecord(c); }
        c.close();
        db.close();
        return r;
    }

    public int getCatFoodRecordCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CAT_FOOD, null);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        db.close();
        return count;
    }

    public int getCatRecordCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM cat_records", null);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        db.close();
        return count;
    }

    public int getTeethRecordCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM teeth_records", null);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        db.close();
        return count;
    }

    public int getExerciseRecordCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM exercise_records", null);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        db.close();
        return count;
    }

    // ============================================================
    // Data Reset
    // ============================================================
    public void resetAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("records", null, null);
        db.delete("users", null, null);
        db.delete("cat_records", null, null);
        db.delete("teeth_records", null, null);
        db.delete("exercise_records", null, null);
        db.delete("breathing_records", null, null);
        db.delete("events", null, null);
        db.delete("book_reading_records", null, null);
        db.delete("cat_food_records", null, null);
        db.execSQL("INSERT INTO " + TABLE_USERS + "(" + COLUMN_USER_NAME + "," + COLUMN_USER_COLOR + ") VALUES ('本人', 0)");
        db.close();
    }

    // Proxy for HealthRepository compatibility
    public SQLiteDatabase getWritableDatabase() { return super.getWritableDatabase(); }
    public SQLiteDatabase getReadableDatabase() { return super.getReadableDatabase(); }
}
