# backup.ps1 — 一键备份健康小助手项目
# 用法:右键 -> 使用 PowerShell 运行,或 `powershell -ExecutionPolicy Bypass -File backup.ps1`
# 作用:把源码、文档、记忆、环境配置打包到一个带时间戳的 zip,直接拷到新电脑即可恢复。

$ErrorActionPreference = "Stop"

# ====== 配置区(换电脑前只需改这一处:备份输出目录)======
$BackupRoot = "D:\ProjectBackup"   # 建议改成你的移动硬盘 / 网盘同步目录
# =====================================================

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = Join-Path $BackupRoot "HealthRecorder_Backup_$timestamp"

# 需要备份的源目录
$sources = @(
    "C:\Users\Administrator\.qclaw\workspace-agent-7b8b96bd\HealthRecorder",
    "C:\Users\Administrator\.qclaw\workspace-agent-7b8b96bd\memory",
    "C:\Users\Administrator\WorkBuddy\代可行\HealthRecorder",
    "C:\Users\Administrator\WorkBuddy\代可行\apk",
    "C:\Users\Administrator\WorkBuddy\代可行\*.md",
    "C:\Users\Administrator\.workbuddy\user-99ed8cd3-6a0a-4c59-b48a-48fe2250baab-personal\SOUL.md",
    "C:\Users\Administrator\.workbuddy\user-99ed8cd3-6a0a-4c59-b48a-48fe2250baab-personal\MEMORY.md",
    "C:\Users\Administrator\.workbuddy\user-99ed8cd3-6a0a-4c59-b48a-48fe2250baab-personal\USER.md",
    "C:\Users\Administrator\.workbuddy\user-99ed8cd3-6a0a-4c59-b48a-48fe2250baab-personal\AGENTS.md"
)

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
Write-Host "[1/4] 创建备份目录: $backupDir" -ForegroundColor Cyan

# 逐项复制(排除 .gradle / build / .idea 等缓存)
$excludeDirs = @(".gradle", "build", ".idea", ".cxx", "captures")
$idx = 0
foreach ($src in $sources) {
    $idx++
    Write-Host ("[2/4] ({0}/{1}) 处理: {2}" -f $idx, $sources.Count, $src) -ForegroundColor Cyan
    $items = Get-Item $src -ErrorAction SilentlyContinue
    foreach ($item in $items) {
        if ($item.PSIsContainer) {
            # 目录:递归复制但排除缓存
            $destSub = Join-Path $backupDir $item.Name
            robocopy $item.FullName $destSub /E /XD $excludeDirs /NFL /NDL /NJH /NJS /NP | Out-Null
        } else {
            Copy-Item $item.FullName -Destination $backupDir -Force
        }
    }
}

# 保存环境配置快照
Write-Host "[3/4] 记录环境配置..." -ForegroundColor Cyan
$envInfo = @()
$envInfo += "JAVA_HOME=$env:JAVA_HOME"
$envInfo += "OS=$([System.Environment]::OSVersion.VersionString)"
$envInfo += "================================"
$envInfo += "Android Studio 路径: C:\Program Files\Android\Android Studio"
$envInfo += "JDK (JBR): C:\Program Files\Android\Android Studio\jbr"
$envInfo += "备份时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$envInfo | Out-File -FilePath (Join-Path $backupDir "ENV_SNAPSHOT.txt") -Encoding UTF8

# 生成说明文件
$readme = @"
健康小助手项目备份  $timestamp

【恢复到新电脑】
1. 安装 Android Studio (含 Android SDK)
2. 把 HealthRecorder 目录放到任意路径
3. 用 Android Studio 打开该目录,Gradle 会自动同步
4. 如报 SDK 路径错误,改 local.properties 里的 sdk.dir
5. 编译: .\gradlew.bat clean assembleDebug
6. 详细说明见 HANDOVER.md

【不丢 App 数据的关键】
- App 内数据在手机本地数据库,换电脑不影响
- 换手机前用 App 内「数据管理 -> 导出」备份
- 重装 App 用 adb install -r (保留数据),不要先卸载

【本备份包含】
- 完整源码 (排除 .gradle / build 缓存)
- 项目文档与 AI 记忆文件
- 历史 APK 归档
- 环境配置快照 ENV_SNAPSHOT.txt
"@
$readme | Out-File -FilePath (Join-Path $backupDir "README_恢复说明.txt") -Encoding UTF8

# 压缩为 zip
$zipPath = "$backupDir.zip"
Write-Host "[4/4] 打包压缩: $zipPath" -ForegroundColor Cyan
Compress-Archive -Path "$backupDir\*" -DestinationPath $zipPath -Force

$sizeMB = [math]::Round((Get-Item $zipPath).Length / 1MB, 2)
Write-Host ""
Write-Host "===== 备份完成 =====" -ForegroundColor Green
Write-Host "文件: $zipPath" -ForegroundColor Green
Write-Host "大小: $sizeMB MB" -ForegroundColor Green
Write-Host ""
Write-Host "请把该 zip 复制到移动硬盘或云盘,即可在新电脑恢复。" -ForegroundColor Yellow
