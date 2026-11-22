# Slay the Amethyst Plus

基于紫水晶 [v1.6.7](https://github.com/ModinMobileSTS/SlayTheAmethystModded/releases/tag/v1.6.7)（`dadad2b`）。Plus 在新版「画面与分辨率」中增加横屏、竖屏铺满预设，并在模组游戏启动前确认纵向渲染表面就绪。与 STS1 Portrait 搭配时，MOD 会按纵向画布重排界面。

本目录提供 Plus 的迁移补丁。公开的预编译 APK 需先确认上游及随包组件的再分发许可。

## Windows 构建

需要 Git、PowerShell 7、JDK 21、Android SDK 36、NDK 27.0.12077973 和 CMake 3.22.1。设置 `JAVA_HOME` 和 `ANDROID_HOME`。从项目根目录运行：

```powershell
pwsh -File ./launcher/Check-AmethystUpstream.ps1 -Tag v1.6.7 -ApplyTo ./amethyst-v1.6.7
```

把自己 Steam 游戏的 `desktop-1.0.jar` 复制到 `amethyst-v1.6.7/build-deps/desktop.jar`。将 [Java 8 运行时](https://github.com/ModinMobileSTS/SlayTheAmethystModdedDependence/releases/download/pojav-jre8/jre8-pojav.zip) 放到同一目录。随后构建：

```powershell
Set-Location ./amethyst-v1.6.7
.\gradlew.bat '-Papplication.id=io.stamethyst.portraitpreview' '-Papplication.version.name=1.6.7-plus.5' '-Papplication.version.code=5' '-Papplication.label=Slay the Amethyst Plus' :app:assembleDebug
```

产物位于 `app/build/outputs/apk/debug/app-debug.apk`。如果从旧 Plus 原地更新，Android 要求包名相同且由同一个证书签名。手机上的启用方式见[项目首页](../README.md#android--amethyst-plus)。

## 检查上游更新

可在本地手动检查上游正式版本：

```powershell
pwsh -File ./launcher/Check-AmethystUpstream.ps1
```

检查脚本在干净检出中执行 `git apply --check`。若补丁发生冲突，需要迁移冲突后再构建、测试和签名。检查通过不会自动发布新 APK。仓库管理员可将 [工作流模板](./check-upstream.workflow.yml) 放到 .github/workflows/check-upstream.yml，启用每日两次检查。
