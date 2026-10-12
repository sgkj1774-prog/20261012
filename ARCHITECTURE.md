# 健康小助手 — 架构准则

> 基于 2026 年 Android 现代化架构理念，参考"Local First + Repository 单一数据源 + Flow 响应式"核心思想。
> 适用年限：2026–2032。

---

## 核心架构分层

```
UI (Activity / Fragment)
    │
ViewModel（状态持有，暴露 Flow）
    │
UseCase（可选，业务逻辑封装）
    │
Repository（单一数据入口，Single Source of Truth）
    │
┌─────────────────────────────────┐
│            Data Layer            │
│  Local DB (Room)  │  Remote API  │
│          Sync Engine            │
└─────────────────────────────────┘
```

**原则：**
- UI 永远不知道数据来自本地还是网络
- Repository 是唯一可信源，UI 只调 Repository
- 换数据库/换服务器/加 AI 层，不动 UI

---

## 数据分层（Data Layer）

### 现代化模型分层（演进路径）

```
API Response (JSON)
    ↓ Mapper
Entity (Room 数据库模型)
    ↓ Mapper
Domain Model (业务模型)
    ↓ Mapper
UIModel (UI 展示模型)
```

**当前项目状态：** Entity/Domain/UIModel 暂时混用（Record.java 同时承担了数据库读写和 UI 展示），短期可接受，中期需拆分。

### 数据库 Schema 版本管理

- **必须维护 DATABASE_VERSION，每次表结构变更必须 +1**
- onUpgrade 按版本顺序写（从低到高：`<3` → `<4` → `<5` → ...）
- ALTER TABLE 加 `try-catch`，防止列已存在报错
- onCreate 必须包含所有表的完整建表语句（新用户首次安装走 onCreate）
- **当前版本：DATABASE_VERSION = 7**

### 四级缓存策略（未来方向）

```
Memory Cache  →  热点数据
Room          →  结构化持久数据（当前）
Disk Cache    →  图片/大文件
Cloud         →  远程备份（未来同步引擎）
```

---

## Local First 原则

**错误思路（请求优先）：**
```
打开APP → 请求网络 → 显示数据 → 失败就无数据
```

**正确思路（Local First）：**
```
打开APP → Room 读取 → 立即显示 → 后台同步服务器 → 更新 Room → UI 自动刷新
```

**实现要点：**
- 启动时优先从 Room 加载数据，立即渲染
- 写入操作先落本地（乐观更新），后台同步远程
- 同步失败不阻塞用户，队列化重试
- 支持完全离线可用

---

## 响应式数据流（Flow）

### 核心原则

- 数据用 `Flow<T>` 而非 `T`，ViewModel 暴露 Flow
- 数据库更新 → Flow → ViewModel → UI 自动刷新
- Compose 对 Flow 天然友好，collectAsState() 自动驱动重组

### 事件数据（Transient Data）

**不要**存数据库的数据（Toast、通知、跳转、支付结果）：
- 使用 `SharedFlow<UiEvent>` 或 `Channel`
- 一次性事件，消费即消失
- ViewModel 负责 emit，UI 负责 collect

**当前项目缺失项：** 事件（如保存成功 Toast、删除确认）目前用 `runOnUiThread` + `Handler`，后续应迁移到 SharedFlow。

---

## 同步引擎（Sync Engine）

### Change Log 同步模式

```
本地修改 → 写入 Change Log 表 → 后台 WorkManager 同步 → 推送服务器
                                         ↓
                              服务器响应 → 冲突检测 → 合并/覆盖 → 更新 Room
```

**核心要素：**
1. **Change Log 表**：记录每条本地修改（操作类型、实体ID、时间戳、Payload）
2. **冲突处理**：Last-Write-Wins 或服务端优先，或用户选择
3. **增量同步**：只同步变更部分，不全量拉取
4. **断网恢复**：未同步的 Change Log 持久化，联网后自动补发

**当前项目现状：** 数据存储在 Room，但无同步引擎，无 Change Log，无远程备份（之前接入的百度网盘同步已移除）。

**未来规划：** 先做本地数据可靠（迁移到 Repository 模式），再做增量备份（导出 SAF + 云存储 API），最后考虑实时同步。

---

## AI Context Store（未来扩展）

用户数据统一管理，为 AI 能力提供上下文：

```
用户行为 → Context Store → LLM → 智能建议/总结/预测
```

**Context 数据：**
- 近期操作记录
- 用户偏好设置
- 健康趋势摘要
- 收藏/标记的记录

**当前项目状态：** 尚未规划。短期无需，架构预留扩展点（Repository 模式建立后可平滑接入）。

---

## 权限与 Feature Flag

**错误写法：**
```java
if (isVip) showFeature();
if (!login) showLogin();
```

**正确写法：**
```
PermissionRepository
    ↓
FeatureFlag 检查
    ↓
UI 响应
```

**当前项目状态：** 功能权限硬编码散布在各 Activity/Fragment 中。短期内可接受，但应逐步收敛。

---

## 推荐技术栈（当前项目适用）

| 层级 | 推荐技术 | 当前项目状态 |
|------|----------|------------|
| UI | Jetpack Compose | ❌ 仍在用 XML |
| 架构 | MVVM + Repository | ⚠️ 部分 MVVM，Repository 缺失 |
| 数据库 | Room | ✅ 已用 SQLite/OpenHelper |
| 异步 | Kotlin Coroutines + Flow | ⚠️ 偶用 Handler/AsyncTask |
| 网络 | Retrofit / Ktor | ❌ 无网络层 |
| 配置 | DataStore | ❌ 用 SharedPreferences |
| 后台任务 | WorkManager | ❌ 无 |
| 依赖注入 | Hilt | ❌ 手动 new 对象 |
| 同步 | 自建 Sync Engine | ❌ 无 |

---

## 架构迁移路线图

### 短期（当前可执行）
- [ ] Repository 模式：所有数据访问走 Repository，ViewModel 不直接 new DatabaseHelper
- [ ] Flow 化：列表数据从 `List<T>` 改为 `Flow<List<T>>`，LiveData/Handler 事件迁移到 SharedFlow
- [ ] 完善 Schema 版本管理：保持 DATABASE_VERSION 严格递增

### 中期（需要较大重构）
- [ ] Entity / Domain / UIModel 拆分
- [ ] DataStore 替代 SharedPreferences
- [ ] 事件层 SharedFlow 统一

### 远期（未来规划）
- [ ] Jetpack Compose 迁移
- [ ] WorkManager 后台同步
- [ ] Sync Engine（Change Log + 增量同步）
- [ ] AI Context Store 预留

---

## 架构决策记录

| 决策 | 状态 | 说明 |
|------|------|------|
| Local First 优先 | ✅ 遵循 | 读写先本地，后同步 |
| Repository 单一数据源 | ⚠️ 部分遵循 | DatabaseHelper 直接暴露，Repository 缺失 |
| Flow 响应式 | ⚠️ 部分遵循 | 列表偶用 Handler，需全面迁移 |
| Schema 版本管理 | ✅ 遵循 | DATABASE_VERSION=7，onUpgrade 顺序正确 |
| Sync Engine | ❌ 暂无 | 计划先 SAF 导出，远期云同步 |
| AI Context | ❌ 暂无 | 架构预留扩展点 |
