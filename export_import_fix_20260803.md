# 导出/导入问题排查与修复（2026-08-03）

## 用户反馈
1. 数据导出未包含猫粮、呼吸打卡、读书笔记
2. 数据导入选择文件后跳转到主页，未生效

## 根因确认

### 问题1（导出缺数据）：APK为旧版本
- **结论**：源码完全正确，APK缓存为7月31日build25旧版本
- ExportHelper.java 实际已包含完整9段CSV写入代码和9个XLSX Sheet创建代码
- ImportHelper.java 实际已包含完整9段CSV解析和XLSX解析代码
- 排查过程：用 Select-String / node.js 逐行检查，确认各段 tag 标签（`=== 呼吸打卡记录 ===`、`=== 读书笔记记录 ===`、`=== 猫粮记录 ===`）全部存在于正确位置

### 问题2（导入不可用）：DataManagementActivity.java L268格式串参数不匹配
- **结论**：10个占位符只传6个参数，运行时抛 MissingFormatArgumentException，异常被吞没导致静默跳转主页
- 修复前：`rc, uc, cc, tc, ec, sk);`（6个参数）
- 修复后：`rc, uc, cc, tc, ec, bc, evc, bkc, cfc, sk);`（10个参数）
- 同理 L208 导出确认消息（12个占位符，12个参数）也已修复

## 修复内容
- `DataManagementActivity.java` L268: 格式串参数补齐 bc/evc/bkc/cfc
- `DataManagementActivity.java` L208: 格式串参数补齐（同步确认）
- `gradlew clean assembleDebug` 强制重建APK

## 验证
- APK: `健康小助手_V1.4.4_build25_debug.apk` (15.07MB, 2026-08-03 10:23)
- 源码行号确认：
  - ExportHelper: CSV tag L122/140/157/176，XLSX Sheet L332/387/417
  - ImportHelper: parseCsv L86~185，parseExcel L590~603，ImportResult构造 L184/600
  - DataManagementActivity: performImport L228~287，doInsertData L287~395
