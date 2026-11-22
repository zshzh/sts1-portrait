package sts1portrait;

import basemod.BaseMod;
import basemod.ModLabel;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import java.io.IOException;
import java.lang.reflect.Field;

final class PortraitDesktopMode {
    private static final String AUTO_PROPERTY = "sts1portrait.desktop.auto";
    private static final String PORTRAIT_KEY = "portrait";
    private static boolean legacyLandscape;
    private static boolean savedPortrait;
    private static boolean saveFailed;

    private PortraitDesktopMode() {}

    private static boolean windows() {
        return System.getProperty("os.name", "").startsWith("Windows");
    }

    static boolean legacyLandscape() {
        return legacyLandscape;
    }

    static void apply(Object configuration, Class<?> type) throws ReflectiveOperationException {
        if (System.getProperty("sts1portrait.desktop.density") != null) return;
        String override = System.getProperty(AUTO_PROPERTY);
        if ("false".equalsIgnoreCase(override)) return;

        boolean windows = windows();
        boolean linux = "Linux".equalsIgnoreCase(System.getProperty("os.name", ""));
        if (!windows && !linux) return;

        try {
            Field width = type.getField("width");
            Field height = type.getField("height");
            boolean legacyPortrait = width.getInt(configuration) < height.getInt(configuration);
            boolean windowsPortrait = windows && ("true".equalsIgnoreCase(override) || readPreference());
            if (windows && !windowsPortrait && !legacyPortrait) return;

            DisplayMode display = (DisplayMode) type.getMethod("getDesktopDisplayMode").invoke(null);
            if (display == null || display.width <= 0 || display.height <= 0) return;
            WindowSize size = chooseSize(windows, windowsPortrait, legacyPortrait, display.width, display.height);
            if (size == null) {
                if (linux) System.out.println("[STS1 Portrait] Linux game display "
                        + display.width + "x" + display.height + " is not portrait");
                return;
            }

            Field fullscreen = type.getField("fullscreen");
            width.setInt(configuration, size.width);
            height.setInt(configuration, size.height);
            fullscreen.setBoolean(configuration, false);
            System.setProperty("org.lwjgl.opengl.Window.undecorated", Boolean.toString(linux));
            legacyLandscape = windows && !windowsPortrait;
            System.out.println("[STS1 Portrait] " + (windows ? "Windows" : "Linux virtual display")
                    + " window " + size.width + "x" + size.height + " on " + display.width + "x" + display.height);
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            System.out.println("[STS1 Portrait] desktop display unavailable, using saved window settings");
        }
    }

    static WindowSize chooseSize(boolean windows, boolean windowsPortrait, boolean legacyPortrait,
                                 int displayWidth, int displayHeight) {
        if (!windows) {
            return displayWidth < displayHeight ? new WindowSize(displayWidth, displayHeight) : null;
        }
        if (windowsPortrait) {
            int height = Math.min(Math.round(displayHeight * .90f),
                    Math.round(displayWidth * .92f * 19.5f / 9f));
            return new WindowSize(Math.round(height * 9f / 19.5f), height);
        }
        if (!legacyPortrait) return null;
        int height = Math.min(720, Math.min(Math.round(displayHeight * .90f),
                Math.round(displayWidth * .92f * 9f / 16f)));
        return new WindowSize(Math.round(height * 16f / 9f), height);
    }

    private static SpireConfig config() throws IOException {
        return new SpireConfig("sts1portrait", "desktop-display");
    }

    private static boolean readPreference() {
        try {
            return config().getBool(PORTRAIT_KEY);
        } catch (IOException | IllegalArgumentException failure) {
            System.out.println("[STS1 Portrait] could not read desktop display preference");
            return false;
        }
    }

    private static boolean savePreference(boolean portrait) {
        try {
            SpireConfig config = config();
            config.setBool(PORTRAIT_KEY, portrait);
            config.save();
            savedPortrait = portrait;
            return true;
        } catch (IOException | IllegalArgumentException failure) {
            System.out.println("[STS1 Portrait] could not save desktop display preference");
            return false;
        }
    }

    static void registerSettings() {
        if (!PortraitDesktopViewport.desktopBackend() || !windows()) return;
        savedPortrait = readPreference();
        boolean simplified = Settings.language == Settings.GameLanguage.ZHS;
        boolean traditional = Settings.language == Settings.GameLanguage.ZHT;
        ModPanel panel = new ModPanel();
        panel.addUIElement(new ModLabel(localize(simplified, traditional, "Desktop display mode",
                "电脑显示模式", "電腦顯示模式"), 430f, 805f, Settings.GOLD_COLOR, FontHelper.tipHeaderFont,
                panel, label -> {}));
        panel.addUIElement(new ModLabeledToggleButton(localize(simplified, traditional, "Adaptive portrait window",
                "自适应竖屏", "自適應直向視窗"), 430f, 715f, Settings.CREAM_COLOR, FontHelper.tipBodyFont,
                savedPortrait, panel, label -> {}, button -> {
                    saveFailed = !savePreference(button.enabled);
                    if (saveFailed) button.enabled = savedPortrait;
                }));
        panel.addUIElement(new ModLabel(localize(simplified, traditional,
                "Off: original landscape. Restart the game to apply.",
                "关闭时使用原版横屏。重启游戏后生效。", "關閉時使用原版橫向。重新啟動遊戲後生效。"),
                430f, 630f, Settings.CREAM_COLOR, FontHelper.tipBodyFont, panel, label -> {}));
        panel.addUIElement(new ModLabel("", 430f, 557f, Settings.RED_TEXT_COLOR, FontHelper.tipBodyFont,
                panel, label -> label.text = saveFailed ? localize(simplified, traditional, "Could not save the setting.",
                        "无法保存显示设置。", "無法儲存顯示設定。") : ""));
        BaseMod.registerModBadge(badge(), "STS1 Portrait", "STS1 Portrait contributors",
                localize(simplified, traditional, "Desktop display mode", "电脑显示模式", "電腦顯示模式"), panel);
    }

    private static String localize(boolean simplified, boolean traditional, String english, String chinese, String taiwanese) {
        return simplified ? chinese : traditional ? taiwanese : english;
    }

    private static Texture badge() {
        Pixmap pixels = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
        try {
            pixels.setColor(.14f, .23f, .29f, 1f);
            pixels.fill();
            pixels.setColor(.59f, .77f, .81f, 1f);
            pixels.drawRectangle(1, 1, 38, 38);
            pixels.fillRectangle(11, 6, 18, 28);
            pixels.setColor(.14f, .23f, .29f, 1f);
            pixels.fillRectangle(13, 8, 14, 21);
            return new Texture(pixels);
        } finally {
            pixels.dispose();
        }
    }

    static final class WindowSize {
        final int width;
        final int height;

        WindowSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
