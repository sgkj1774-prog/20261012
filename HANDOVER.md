# HANDOVER.md — 健康小助手 App 项目交接文档

> **用途**:换电脑、重装系统、或新协作者接手时,照此文档 30 分钟内恢复完整开发环境。
> **最后更新**:2026-10-12
> **当前版本**:V1.4.8 (build29)

---

## 一、项目基本信息

| 项 | 值 |
|---|---|
| App 名称 | 健康小助手 (HealthRecorder) |
| 包名 | `com.example.healthassistant` |
| 当前版本 | versionCode **29** / versionName **"1.4.8"** |
| 下次版本 | versionCode **30** / versionName **"1.4.9"** |
| 数据库版本 | **11** |
| 技术栈 | 原生 Android (Java) + SQLite + Gradle |
| 图表库 | MPAndroidChart (自定义 SafeCombinedChart) |
| 语言 | Java (无 Kotlin) |

## 二、源码位置

**主开发目录**(Agent 日常工作区):
```
C:\Users\Administrator\.qclaw\workspace-agent-7b8b96bd\HealthRecorder
```

**工作区副本**(同一份代码,双保险):
```
C:\Users\Administrator\WorkBuddy\代可行\HealthRecorder
```

> 源码已用 Git 管理,首次提交已完成。**换电脑只需克隆仓库,无需拷贝这两个目录。**

## 三、环境要求(必须完全一致)

| 组件 | 版本 | 说明 |
|---|---|---|
| **JDK** | Android Studio 自带 JBR | 路径 `C:\Program Files\Android\Android Studio\jbr` |
| **Android Studio** | 任意近期版本 | 含 Android SDK |
| **Gradle** | 8.5 (wrapper 自动下载) | 见 `gradle/wrapper/gradle-wrapper.properties` |
| **Android SDK** | compileSdk / targetSdk 见 `app/build.gradle` | 需装对应 Build-Tools |
| **操作系统** | Windows (开发机为 Windows 10/11) | 路径风格为 Windows |

### 关键环境变量
```bat
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
```
`local.properties` 里需指向本机 SDK 路径(此文件不入库,需本地重建):
```
sdk.dir=C\:\\Users\\<你的用户名>\\AppData\\Local\\Android\\Sdk
```

## 四、编译与打包

```bash
cd HealthRecorder

# 普通编译(增量,可能因缓存跳过 Java 编译)
.\gradlew.bat assembleDebug

# ★ 修改 Java 后必须用 clean 强制重编(踩过坑!)
.\gradlew.bat clean assembleDebug
```

**产物路径**:
```
app/build/outputs/apk/debug/健康小助手_V<版本>_build<构建号>_debug.apk
```

**安装到手机**:连 USB → `adb install -r <apk路径>`
> ⚠️ `-r` 是覆盖安装,**保留数据**;不要用 uninstall,会清空用户数据。

## 五、⚠️ 踩过的坑(重要经验)

### 1. Gradle 增量编译陷阱
**现象**:改了 Java 代码,重新编译后装上,App 行为没变化。
**原因**:`compileDebugJavaWithJavac UP-TO-DATE`,改动没打包进 APK。
**解决**:一律用 `.\gradlew.bat clean assembleDebug`。

### 2. Spinner 初始化时序 bug
**现象**:编辑页面回填数据时,状态被错误重置。
**原因**:`setAdapter()` 后自动触发 `onItemSelected(position=0)`,早于 `loadEditData()`。
**解决**:监听器加守卫 `if (editingRecord == null && selectedStatus == 0 && pos == 0) return;`,并在 `loadEditData()` 末尾显式 `spinner.setSelection(selectedStatus, false)`。

### 3. 布局复用导致 NPE 闪退
**现象**:点击某入口闪退,日志 `findViewById 返回 null`。
**原因**:多个 Activity 复用同一布局,把 `RecyclerView` 改成 `ListView` 后,原 Activity 找不到 `R.id.recyclerView`。
**解决**:每个 Activity 配独立布局文件,不要共用可变的列表布局。

### 4. 中文编码损坏
**现象**:PowerShell 写入 Java 文件后中文乱码、文件损坏。
**原因**:PowerShell `Set-Content` 默认 GBK,会破坏 UTF-8 源文件。
**解决**:读写中文 Java 文件用 Node 脚本以 `latin1`/`gbk` 显式读写,或用 IDE 直接编辑。

### 5. 数据库升级
新增字段必须在 `DatabaseHelper.onUpgrade()` 里 `ALTER TABLE`,并提升 `DATABASE_VERSION`。旧数据行新字段为 NULL,读取代码要降级处理。

## 六、模块结构速查

```
com.example.healthrecorder/
├── MainActivity.java            # 主页(日历 + 趋势图 + 入口)
├── DatabaseHelper.java          # SQLite 建表/升级/重置
├── HealthRepository.java        # 唯一数据入口(Facade)
├── HomeChartView.java           # 主页趋势图(双指缩放/点击浮层)
├── HomeCalendarView.java        # 主页日历
├── DayDetailActivity.java       # 日历点某天的详情页
├── RecordListActivity.java      # 健康记录列表(主页入口)
├── DataManagementActivity.java  # 数据管理(重置/成员/导入导出)
├── book/          # 读书笔记(BookAdd/BookList/BookStats)
├── breathing/     # 呼吸打卡(+ BreathingStats 统计)
├── cat/           # 猫咪记录(猫粮/便便)
├── event/         # 重要事项
├── exercise/      # 运动(ExerciseChart 含本月跑步)
└── teeth/         # 刷牙记录
```

## 七、版本历史维护约定

- 每次发版:`app/build.gradle` 的 versionCode +1、versionName 按 `1.4.x` 递增
- 同时更新 `activity_version_history.xml`(界面「关于」页),**按最新到旧排序**
- APK 归档到 `apk/archive/健康小助手_V<版本>_build<号>_debug.apk`
- Git 打 tag:`git tag v1.4.8_build29`

## 八、数据说明(重要澄清)

| 数据类型 | 存放位置 | 换电脑影响 |
|---|---|---|
| 源代码/配置 | Git 仓库 | 克隆即可 |
| **App 内用户数据** | **手机本地 SQLite** | **完全不受影响** |
| 项目记忆/文档 | 工作区 md 文件 | 见备份脚本 |

> 🔑 **换电脑不会丢 App 数据**,因为数据在手机上。只有**卸载 App** 或**换手机**才会丢。
> 换手机前,务必用 App 内「数据管理 → 导出」保存备份文件。
