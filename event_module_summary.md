# 重要事项记录与提醒功能 — 任务完成记录

## 完成时间
2026-07-27

## 创建/修改的文件清单

### 新增文件（5个）

| 文件 | 路径 |
|------|------|
| EventListActivity.java | app/src/main/java/com/example/healthrecorder/event/ |
| EventAdapter.java | app/src/main/java/com/example/healthrecorder/event/ |
| EventAddActivity.java | app/src/main/java/com/example/healthrecorder/event/ |
| activity_event_list.xml | app/src/main/res/layout/ |
| item_event.xml | app/src/main/res/layout/ |
| activity_event_add.xml | app/src/main/res/layout/ |

### 修改文件（2个）

| 文件 | 改动 |
|------|------|
| Event.java | 新增静态方法 `repeatTextArr()`，供 Spinner 数组使用 |
| AndroidManifest.xml | 新增两个 Activity 注册 |

---

## AndroidManifest 新增内容

```xml
<!-- 重要事项 -->
<activity
    android:name=".event.EventListActivity"
    android:exported="false"
    android:parentActivityName=".MainActivity"/>
<activity
    android:name=".event.EventAddActivity"
    android:exported="false"
    android:parentActivityName=".event.EventListActivity"/>
```

---

## 各文件核心实现

### EventListActivity.java
- 复用 `activity_record_list.xml` 布局（实际使用独立 `activity_event_list.xml`）
- Toolbar 标题"📌 重要事项"，返回按钮
- RecyclerView + EventAdapter，按原始顺序展示（getAllEvents 已按 event_time ASC）
- 点击进入 EventAddActivity（edit 模式），长按弹出删除确认对话框
- 右上角 👤 成员管理 + ⚙️ 数据管理入口
- `onResume` 刷新数据

### EventAdapter.java
- 左侧 4dp 优先级色条：`priority=3 → #F44336`（红）/ `priority=2 → #FF9800`（橙）/ `priority=1 → #4CAF50`（绿）
- 右上角状态标签（已完成/已取消灰色）；右上角优先级文字标签
- 提醒时间显示（橙）：⏰ 相对日期+时间
- 重复类型标签（蓝）：🔁 重复文字
- 事件时间：相对日期（今天/明天/昨天）+ 具体时刻
- 内容/备注：2行截断

### EventAddActivity.java
- Intent extra `edit_id`：>0 时为编辑模式，加载数据并显示删除按钮
- 标题必填校验，Toast 提示
- 事件日期+时间（灰蓝色按钮 DatePicker + TimePicker）
- 提醒 Switch：关闭时隐藏提醒时间区，开启后显示橙黄色提醒时间选择器
- 优先级 RadioGroup：高🔴 / 中🟠（默认选中）/ 低🟢，使用 spinner_bg drawable
- 重复类型 Spinner：调用 `Event.repeatTextArr()` 生成选项
- 成员 Spinner：加载所有用户，默认选第一个
- 保存后 setResult(RESULT_OK) → finish()

### item_event.xml
- 复用 `list_item_record.xml` 的 card_bg 背景样式
- 左侧 4dp 优先级色条（View + viewPriorityBar ID）
- 时间区（相对日期 + 时刻）
- 主内容区：标题 + 状态标签 + 内容 + 提醒行 + 重复行 + 优先级标签

### activity_event_add.xml
- ScrollView > LinearLayout 结构
- 标题/内容（multiline）/ 事件时间（双按钮）/ 提醒开关（Switch）+ 提醒时间（条件显示）/ 优先级 RadioGroup（3列）/ 重复 Spinner / 成员 Spinner / 保存按钮（绿）/ 删除按钮（红，编辑时显示）

---

## 注意事项
- 提醒功能仅记录时间到数据库，不发送系统通知（后续迭代处理通知权限）
- 优先级用颜色区分，不用图标
- 布局风格与现有模块（ExerciseRecordListActivity、CatRecordListActivity 等）完全一致
- MainActivity 需自行添加入口按钮（跳转 EventListActivity）
