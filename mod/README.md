# STS1 Portrait

Slay the Spire 1 的竖屏布局模组。当前版本为 `0.9.0-preview.70`。

在 Steam 使用「With Mods」，启用 ModTheSpire、BaseMod 和本模组。Windows 默认横屏。打开「Mods → STS1 Portrait → Config」，启用「自适应竖屏」并重启即可切换。Amethyst Plus 使用「铺满全屏（竖屏）」启动。

- [Steam 创意工坊](https://steamcommunity.com/sharedfiles/filedetails/?id=3842363554)
- [ModTheSpire](https://steamcommunity.com/sharedfiles/filedetails/?id=1605060445)
- [BaseMod](https://steamcommunity.com/sharedfiles/filedetails/?id=1605833019)

## 本地构建

需要 Windows、JDK 8、从框架取得的 `ModTheSpire.jar` 和 `BaseMod.jar`，以及正版 Steam 游戏的 `desktop-1.0.jar`。

```powershell
.\build.ps1 -JdkDirectory 'C:/path/to/jdk8' -FrameworkDirectory 'C:/path/to/framework' -GameJar 'C:/path/to/desktop-1.0.jar'
```

输出至 `build/STS1Portrait.jar`。运行游戏需要单独安装本体和前置模组。
