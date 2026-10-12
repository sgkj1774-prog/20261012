# 健康小助手日历组件修改记录

## 任务目标
为 `HomeCalendarView` 添加自然月大标题显示，优化日期标记与点击交互。

## 修改文件
1. `HealthRecorder/app/src/main/res/layout/view_home_calendar.xml`
2. `HealthRecorder/app/src/main/java/com/example/healthrecorder/HomeCalendarView.java`

---

## 改动一：view_home_calendar.xml

### 布局结构（自上而下）
```
headerMonth (LinearLayout, 40dp)
  ├── btnPrev (ImageButton)     ← 导航箭头不变
  ├── tvMonth (TextView, 13sp, #888888)  ← 缩小为灰色小月份
  └── btnNext (ImageButton)     ← 导航箭头不变

tvMonthBig (TextView, 20sp, #222222, bold)  ← 新增大标题，独立一行

headerDays (LinearLayout, 28dp)   ← 星期标题，一二三四五六日

gridDays (GridLayout, 7列)        ← 日期格子，紧凑排列

tvNoRecord (TextView)            ← 无数据提示
```

### 关键变更
- `tvMonth` 字号从 15sp → **13sp**，颜色从 #333 → **#888888**（灰色副标题）
- 新增 `tvMonthBig`（id=`tvMonthBig`）：**20sp bold**，独立一行，自然月核心标题

---

## 改动二：HomeCalendarView.java

### 1. updateMonthLabel() 方法
```java
private void updateMonthLabel() {
    String bigLabel = MONTH_BIG_FMT.format(displayCal.getTime());  // "2024年7月"
    String navLabel = MONTH_FMT.format(displayCal.getTime());     // "2024年 7月"
    if (tvMonthBig != null) tvMonthBig.setText(bigLabel);
    if (tvMonth    != null) tvMonth.setText(navLabel);
}
```
- `MONTH_BIG_FMT = "yyyy年M月"` → 大标题（无多余空格）
- `MONTH_FMT = "yyyy年 M月"` → 导航栏小标题（中间有空格）
- 导航箭头点击后同步调用 `updateMonthLabel()` + `buildCalendar()`

### 2. 星期标题（setupHeaderDays）
```java
String[] days = {"一","二","三","四","五","六","日"};  // 改为周一开始
```

### 3. 日期对齐逻辑修正
```java
// 原来 firstDow=0 表示周日起始
// 改为周一起始后，空白格子数 = firstDow==0 ? 6 : firstDow-1
for (int i = 0; i < (firstDow == 0 ? 6 : firstDow - 1); i++) {
    addDayCell(null, false, false, 0, 0);
}
```

### 4. 颜色标记圆点（多类型支持）
```java
// 颜色对应（需求指定）
TYPE_HEALTH    → #2196F3  蓝
TYPE_CAT       → #FF9800  橙
TYPE_BREATHING → #4CAF50  绿
TYPE_EVENT     → #9C27B0  紫
TYPE_BOOK      → #9E9E9E  灰

// 每种类型最多显示1个圆点，水平排列在同一行
if ((types & TYPE_HEALTH)    != 0) addColorDot(dotRow, dotLp, COLOR_HEALTH);
if ((types & TYPE_CAT)       != 0) addColorDot(dotRow, dotLp, COLOR_CAT);
if ((types & TYPE_BREATHING) != 0) addColorDot(dotRow, dotLp, COLOR_BREATHING);
if ((types & TYPE_EVENT)     != 0) addColorDot(dotRow, dotLp, COLOR_EVENT);
if ((types & TYPE_BOOK)      != 0) addColorDot(dotRow, dotLp, COLOR_BOOK);
```

### 5. 新增 API
```java
// 设置带类型的日期标记（推荐）
public void setDateMarkersWithTypes(Map<String, Integer> typeMap)

// 设置每日记录摘要（用于点击弹窗）
public void setDaySummaries(Map<String, List<String>> summaryMap)
```

### 6. 点击弹出 PopupWindow
- 仅当日期有标记（types != 0）时弹出
- 内容：日期标题 + 分隔线 + 摘要列表（bullet 样式）
- 位置：日历控件中央偏下，水平边界智能修正
- 无摘要时显示"该日有记录"占位文本

---

## 兼容性
- `setDateMarkers(Set<String>)` 保持兼容，内部统一用 `TYPE_HEALTH` 蓝色
- 月份导航箭头（btnPrev/btnNext）功能完全不变
- `buildCalendar()` 结构与原有调用链一致

## 产物
- view_home_calendar.xml：含 `tvMonthBig` 大标题，导航栏小月份降级为副标题
- HomeCalendarView.java：含 `updateMonthLabel()`、`showRecordPopup()`、多类型颜色圆点
