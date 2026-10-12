package com.example.healthrecorder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.healthrecorder.breathing.BreathingRecord;
import com.example.healthrecorder.book.BookRecord;
import com.example.healthrecorder.cat.CatFoodRecord;
import com.example.healthrecorder.cat.CatRecord;
import com.example.healthrecorder.event.Event;
import com.example.healthrecorder.exercise.ExerciseRecord;
import com.example.healthrecorder.teeth.TeethRecord;

import java.util.List;

/**
 * HealthRecorder 数据仓库层
 * 采用单例模式，统一管理所有数据访问操作
 * 所有方法代理到 DatabaseHelper，确保数据访问的一致性和可维护性
 * 
 * 使用方式：
 * HealthRepository.getInstance(context).getAllRecords();
 */
public class HealthRepository {
    
    private static final String TAG = "HealthRepository";
    
    // 单例实例
    private static volatile HealthRepository sInstance;
    
    // DatabaseHelper 引用
    private final DatabaseHelper mDbHelper;
    
    /**
     * 私有构造函数，防止外部实例化
     * @param context 应用上下文（使用 getApplicationContext() 防止内存泄漏）
     */
    private HealthRepository(Context context) {
        mDbHelper = new DatabaseHelper(context.getApplicationContext());
        Log.d(TAG, "HealthRepository initialized");
    }
    
    /**
     * 获取单例实例
     * 使用双重检查锁定（Double-Check Locking）确保线程安全
     * 
     * @param context 上下文（建议传入 getApplicationContext()）
     * @return HealthRepository 单例实例
     */
    public static HealthRepository getInstance(Context context) {
        if (sInstance == null) {
            synchronized (HealthRepository.class) {
                if (sInstance == null) {
                    sInstance = new HealthRepository(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }
    
    // ============================================================
    // User CRUD 操作
    // ============================================================
    
    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        return mDbHelper.getAllUsers();
    }
    
    /**
     * 添加用户
     * @param user 用户对象
     * @return 新插入记录的 ID
     */
    public long addUser(User user) {
        return mDbHelper.addUser(user);
    }
    
    /**
     * 更新用户
     * @param user 用户对象
     */
    public void updateUser(User user) {
        mDbHelper.updateUser(user);
    }
    
    /**
     * 删除用户
     * @param id 用户 ID
     */
    public void deleteUser(int id) {
        mDbHelper.deleteUser(id);
    }
    
    /**
     * 根据 ID 获取用户
     * @param id 用户 ID
     * @return 用户对象，未找到返回 null
     */
    public User getUserById(int id) {
        return mDbHelper.getUserById(id);
    }
    
    /**
     * 根据姓名查找用户
     * @param name 用户姓名
     * @return 用户对象，未找到返回 null
     */
    public User getUserByName(String name) {
        return mDbHelper.getUserByName(name);
    }
    
    // ============================================================
    // Record CRUD 操作（健康记录）
    // ============================================================
    
    /**
     * 添加健康记录
     * @param record 记录对象
     * @return 新插入记录的 ID
     */
    public long addRecord(Record record) {
        return mDbHelper.addRecord(record);
    }
    
    /**
     * 更新健康记录
     * @param record 记录对象
     */
    public void updateRecord(Record record) {
        mDbHelper.updateRecord(record);
    }
    
    /**
     * 根据 ID 获取健康记录
     * @param id 记录 ID
     * @return 记录对象，未找到返回 null
     */
    public Record getRecordById(int id) {
        return mDbHelper.getRecordById(id);
    }
    
    /**
     * 获取所有健康记录
     * @return 所有记录列表
     */
    public List<Record> getAllRecords() {
        return mDbHelper.getAllRecords();
    }
    
    /**
     * 根据用户 ID 获取健康记录
     * @param userId 用户 ID（<=0 时返回全部记录）
     * @return 记录列表
     */
    public List<Record> getRecordsByUserId(int userId) {
        return mDbHelper.getRecordsByUserId(userId);
    }
    
    /**
     * 获取图表数据（按日期升序）
     * @return 所有记录列表
     */
    public List<Record> getRecordsForChart() {
        return mDbHelper.getRecordsForChart();
    }
    
    /**
     * 根据用户 ID 获取图表数据（按日期升序）
     * @param userId 用户 ID（<=0 时返回全部记录）
     * @return 记录列表
     */
    public List<Record> getRecordsForChartByUserId(int userId) {
        return mDbHelper.getRecordsForChartByUserId(userId);
    }
    
    /**
     * 删除健康记录
     * @param id 记录 ID
     */
    public void deleteRecord(int id) {
        mDbHelper.deleteRecord(id);
    }
    
    /**
     * 获取健康记录总数
     * @return 记录数量
     */
    public int getRecordCount() {
        return mDbHelper.getRecordCount();
    }
    
    /**
     * 判断记录是否已存在（用于导入去重）
     * @param r 记录对象
     * @return true 表示已存在
     */
    public boolean recordExists(Record r) {
        return mDbHelper.recordExists(r);
    }
    
    // ============================================================
    // Cat Record CRUD 操作（猫咪如厕记录）
    // ============================================================
    
    /**
     * 获取所有猫咪如厕记录
     */
    public List<CatRecord> getAllCatRecords() {
        return mDbHelper.getAllCatRecords();
    }
    
    /**
     * 添加猫咪如厕记录
     * @param r 记录对象
     * @return 新插入记录的 ID
     */
    public long addCatRecord(CatRecord r) {
        return mDbHelper.addCatRecord(r);
    }
    
    /**
     * 删除猫咪如厕记录
     * @param id 记录 ID
     */
    public void deleteCatRecord(int id) {
        mDbHelper.deleteCatRecord(id);
    }
    
    /**
     * 根据 ID 获取猫咪如厕记录
     * @param id 记录 ID
     * @return 记录对象，未找到返回 null
     */
    public CatRecord getCatRecordById(int id) {
        return mDbHelper.getCatRecordById(id);
    }
    
    /**
     * 更新猫咪如厕记录
     * @return 更新的行数
     */
    public int updateCatRecord(int id, int score, int count, int willingness, int processType, 
                              String notes, int userId, long dateMillis) {
        return mDbHelper.updateCatRecord(id, score, count, willingness, processType, notes, userId, dateMillis);
    }
    
    /**
     * 获取猫咪如厕记录总数
     */
    public int getCatRecordCount() {
        return mDbHelper.getCatRecordCount();
    }
    
    // ============================================================
    // Teeth Record CRUD 操作（刷牙记录）
    // ============================================================
    
    /**
     * 获取所有刷牙记录
     */
    public List<TeethRecord> getAllTeethRecords() {
        return mDbHelper.getAllTeethRecords();
    }
    
    /**
     * 添加刷牙记录
     * @param r 记录对象
     * @return 新插入记录的 ID
     */
    public long addTeethRecord(TeethRecord r) {
        return mDbHelper.addTeethRecord(r);
    }
    
    /**
     * 删除刷牙记录
     * @param id 记录 ID
     */
    public void deleteTeethRecord(int id) {
        mDbHelper.deleteTeethRecord(id);
    }
    
    /**
     * 根据 ID 获取刷牙记录
     * @param id 记录 ID
     * @return 记录对象，未找到返回 null
     */
    public TeethRecord getTeethRecordById(int id) {
        return mDbHelper.getTeethRecordById(id);
    }
    
    /**
     * 更新刷牙记录
     * @return 更新的行数
     */
    public int updateTeethRecord(int id, int period, int duration, String notes, 
                                int userId, long dateMillis) {
        return mDbHelper.updateTeethRecord(id, period, duration, notes, userId, dateMillis);
    }
    
    /**
     * 获取刷牙记录总数
     */
    public int getTeethRecordCount() {
        return mDbHelper.getTeethRecordCount();
    }
    
    // ============================================================
    // Exercise Record CRUD 操作（运动记录）
    // ============================================================
    
    /**
     * 获取所有运动记录
     */
    public List<ExerciseRecord> getAllExerciseRecords() {
        return mDbHelper.getAllExerciseRecords();
    }
    
    /**
     * 添加运动记录
     * @param r 记录对象
     * @return 新插入记录的 ID
     */
    public long addExerciseRecord(ExerciseRecord r) {
        return mDbHelper.addExerciseRecord(r);
    }
    
    /**
     * 删除运动记录
     * @param id 记录 ID
     */
    public void deleteExerciseRecord(int id) {
        mDbHelper.deleteExerciseRecord(id);
    }
    
    /**
     * 根据 ID 获取运动记录
     * @param id 记录 ID
     * @return 记录对象，未找到返回 null
     */
    public ExerciseRecord getExerciseRecordById(int id) {
        return mDbHelper.getExerciseRecordById(id);
    }
    
    /**
     * 更新运动记录
     * @return 更新的行数
     */
    public int updateExerciseRecord(int id, int type, float distance, int duration, float pace,
                                   int heartRate, int calories, int elevation, int difficulty,
                                   String notes, String location, int userId, long dateMillis) {
        return mDbHelper.updateExerciseRecord(id, type, distance, duration, pace, heartRate, 
                                             calories, elevation, difficulty, notes, location, userId, dateMillis);
    }
    
    /**
     * 获取运动记录总数
     */
    public int getExerciseRecordCount() {
        return mDbHelper.getExerciseRecordCount();
    }
    
    // ============================================================
    // Breathing Record CRUD 操作（呼吸打卡记录）
    // ============================================================
    
    /**
     * 获取所有呼吸打卡记录
     */
    public List<BreathingRecord> getAllBreathingRecords() {
        return mDbHelper.getAllBreathingRecords();
    }
    
    /**
     * 添加呼吸打卡记录
     * @param r 记录对象
     * @return 新插入记录的 ID
     */
    public long addBreathingRecord(BreathingRecord r) {
        return mDbHelper.addBreathingRecord(r);
    }
    
    /**
     * 删除呼吸打卡记录
     * @param id 记录 ID
     */
    public void deleteBreathingRecord(int id) {
        mDbHelper.deleteBreathingRecord(id);
    }
    
    /**
     * 获取呼吸打卡记录总数
     */
    public int getBreathingRecordCount() {
        return mDbHelper.getBreathingRecordCount();
    }
    
    // ============================================================
    // Event CRUD 操作（重要事项记录）
    // ============================================================
    
    /**
     * 获取所有重要事项
     */
    public List<Event> getAllEvents() {
        return mDbHelper.getAllEvents();
    }
    
    /**
     * 添加重要事项
     * @param ev 事项对象
     * @return 新插入记录的 ID
     */
    public long addEvent(Event ev) {
        return mDbHelper.addEvent(ev);
    }
    
    /**
     * 更新重要事项
     * @param ev 事项对象
     */
    public void updateEvent(Event ev) {
        mDbHelper.updateEvent(ev);
    }
    
    /**
     * 删除重要事项
     * @param id 事项 ID
     */
    public void deleteEvent(int id) {
        mDbHelper.deleteEvent(id);
    }
    
    /**
     * 获取重要事项总数
     */
    public int getEventCount() {
        return mDbHelper.getEventCount();
    }
    
    // ============================================================
    // Book Record CRUD 操作（读书记录）
    // ============================================================
    
    /**
     * 获取所有读书记录
     */
    public List<BookRecord> getAllBookRecords() {
        return mDbHelper.getAllBookRecords();
    }
    
    /**
     * 添加读书记录
     * @param r 记录对象
     * @return 新插入记录的 ID
     */
    public long addBookRecord(BookRecord r) {
        return mDbHelper.addBookRecord(r);
    }
    
    /**
     * 更新读书记录
     * @param r 记录对象
     */
    public void updateBookRecord(BookRecord r) {
        mDbHelper.updateBookRecord(r);
    }
    
    /**
     * 删除读书记录
     * @param id 记录 ID
     */
    public void deleteBookRecord(int id) {
        mDbHelper.deleteBookRecord(id);
    }
    
    /**
     * 获取读书记录总数
     */
    public int getBookRecordCount() {
        return mDbHelper.getBookRecordCount();
    }
    
    // ============================================================
    // Cat Food Record CRUD 操作（猫粮记录）
    // ============================================================

    /**
     * 新增猫粮记录（endDate 为 null 表示当前在吃）
     * @param r 记录对象
     * @return 新插入记录的 ID
     */
    public long insertCatFoodRecord(CatFoodRecord r) {
        return mDbHelper.insertCatFoodRecord(r);
    }

    /**
     * 获取所有猫粮记录（进行中的排最前，其余按开始日期倒序）
     */
    public List<CatFoodRecord> getAllCatFoodRecords() {
        return mDbHelper.getAllCatFoodRecords();
    }

    /**
     * 根据 ID 获取猫粮记录
     * @param id 记录 ID
     * @return 记录对象，未找到返回 null
     */
    public CatFoodRecord getCatFoodRecordById(int id) {
        return mDbHelper.getCatFoodRecordById(id);
    }

    /**
     * 更新猫粮记录
     * @return 更新的行数
     */
    public int updateCatFoodRecord(CatFoodRecord r) {
        return mDbHelper.updateCatFoodRecord(r);
    }

    /**
     * 删除猫粮记录
     * @param id 记录 ID
     */
    public void deleteCatFoodRecord(int id) {
        mDbHelper.deleteCatFoodRecord(id);
    }

    /**
     * 获取当前正在使用的猫粮（最新一条 end_date 为 null 的记录）
     * @return 记录对象，没有则返回 null
     */
    public CatFoodRecord getCurrentCatFoodRecord() {
        return mDbHelper.getCurrentCatFoodRecord();
    }

    /**
     * 获取指定日期正在使用的猫粮（用于猫咪如厕记录关联显示当日猫粮）
     * @param dateMillis 日期毫秒值
     * @return 记录对象，没有则返回 null
     */
    public CatFoodRecord getCatFoodRecordOnDate(long dateMillis) {
        return mDbHelper.getCatFoodRecordOnDate(dateMillis);
    }

    /**
     * 获取猫粮记录总数
     */
    public int getCatFoodRecordCount() {
        return mDbHelper.getCatFoodRecordCount();
    }

    // ============================================================
    // 工具方法
    // ============================================================
    
    /**
     * 关闭数据库连接（通常不需要手动调用，由系统管理）
     * 仅在特殊场景下使用，如测试或清理资源
     */
    public void closeDatabase() {
        mDbHelper.close();
        Log.d(TAG, "Database closed");
    }

    /**
     * 获取只读数据库实例（供复杂SQL查询使用）
     */
    public SQLiteDatabase getReadableDatabase() {
        return mDbHelper.getReadableDatabase();
    }

    /**
     * 获取可写数据库实例（供复杂SQL写入使用）
     */
    public SQLiteDatabase getWritableDatabase() {
        return mDbHelper.getWritableDatabase();
    }

    /**
     * 重置所有数据（清空所有表并重建默认成员）
     */
    public void resetAllData() {
        mDbHelper.resetAllData();
    }
}
