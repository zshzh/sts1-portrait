# STS1 Portrait · 杀戮尖塔 1 竖屏方案

让《杀戮尖塔》第一代以原生纵向画布游玩。项目由 **STS1 Portrait MOD** 和 **Slay the Amethyst Plus** 两部分组成。MOD 负责重排游戏内界面，Plus 提供 Android 的竖屏渲染表面和触控映射。

| 组件 | 版本 | 获取 |
| --- | --- | --- |
| STS1 Portrait | `0.9.0-preview.42` | [下载 JAR](https://github.com/zshzh/sts1-portrait/releases/download/v0.9.0-preview.42/STS1Portrait-0.9.0-preview.42.jar) · [源码](./mod) |
| Slay the Amethyst Plus | `1.6.7-plus.5` | [源码补丁与构建步骤](./launcher/README.md) |

## Android · Amethyst Plus

1. 安装 Slay the Amethyst Plus。首次使用时，在启动器导入自己 Steam 版《杀戮尖塔》的 `desktop-1.0.jar`，并按启动器提示下载运行资源。
2. 在启动器「模组」页导入并启用 STS1 Portrait。确认 ModTheSpire 和 BaseMod 已就绪。若已有同名旧版，更新该条目，避免同时加载两份。
3. 打开「设置 → 画面与分辨率」，选择「铺满全屏（竖屏）」并保存。
4. 返回首页，以「模组」模式启动游戏。分辨率改动在下次启动生效。若画面吃力，可调低渲染分辨率滑块。

## Windows · Steam

1. 安装 [ModTheSpire](https://steamcommunity.com/sharedfiles/filedetails/?id=1605060445) 和 [BaseMod](https://steamcommunity.com/sharedfiles/filedetails/?id=1605833019)。将新版 JAR 放入游戏安装目录的 `mods/`。如果已订阅 [STS1 Portrait 创意工坊版](https://steamcommunity.com/sharedfiles/filedetails/?id=3842363554)，在加载器里只启用一个版本。
2. 在 Steam 选择 **Play with Mods**，勾选 BaseMod 和 STS1 Portrait。
3. 游戏默认横屏。在主菜单打开「Mods → STS1 Portrait → Config」，开启「自适应竖屏」并重启。关闭这一开关并重启即可切回横屏。

DroidDeck 需要先由其虚拟显示器提供纵向分辨率，再加载本 MOD。DroidDeck 真机兼容性待验证。

## 当前版本

- 竖屏布局覆盖手牌、敌人、状态、药水、地图和奖励等界面。
- `.42` 将故障机器人的充能球移到血条上方。槽位增加时，球栏向右展开并按可用宽度缩放。
- `.41` 将永存者和两个随从分排，校准心脏事件站位，修正故障机器人充能球与空槽，调整竖屏主菜单。
- 启动器基于紫水晶 `v1.6.7`。保留了显示区域调整和独立渲染倍率。启动器构建通过 62 项聚焦单元测试，MOD 在隔离桌面完成 1440×3120 和 1440×3200 的布局核验。手机上的完整安装和触控回归待补充。

[构建 MOD](./mod/README.md) · [构建 Plus 和检查上游更新](./launcher/README.md)

此项目是独立的社区改版。游戏本体由玩家自行提供。Plus 的上游源码和第三方组件归属见 [SlayTheAmethystModded](https://github.com/ModinMobileSTS/SlayTheAmethystModded) 与 [上游组件许可](https://github.com/ModinMobileSTS/SlayTheAmethystModded/blob/v1.6.7/THIRD_PARTY_LICENSES.md)。
