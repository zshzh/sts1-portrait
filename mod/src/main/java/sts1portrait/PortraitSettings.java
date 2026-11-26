package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Align;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.options.AbandonRunButton;
import com.megacrit.cardcrawl.screens.options.DropdownMenu;
import com.megacrit.cardcrawl.screens.options.DropdownMenuListener;
import com.megacrit.cardcrawl.screens.options.ExitGameButton;
import com.megacrit.cardcrawl.screens.options.GiantToggleButton;
import com.megacrit.cardcrawl.screens.options.InputSettingsScreen;
import com.megacrit.cardcrawl.screens.options.OptionsPanel;
import com.megacrit.cardcrawl.screens.options.RemapInputElement;
import com.megacrit.cardcrawl.screens.options.Slider;
import com.megacrit.cardcrawl.screens.options.ToggleButton;
import com.megacrit.cardcrawl.ui.buttons.CancelButton;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class PortraitSettings {
    static final Color ROW = new Color(0.086f, 0.129f, 0.173f, 0.94f);
    static final Color ROW_HOVER = new Color(0.122f, 0.180f, 0.235f, 0.96f);
    static final Color ROW_ALT = new Color(0.068f, 0.104f, 0.140f, 0.94f);
    static final Color ACTIVE = new Color(0.165f, 0.295f, 0.345f, 0.98f);
    static final Color ACTIVE_HOVER = new Color(0.205f, 0.360f, 0.420f, 0.98f);
    static final Color DESTRUCTIVE = new Color(0.355f, 0.145f, 0.125f, 0.98f);
    static final Color DESTRUCTIVE_HOVER = new Color(0.450f, 0.185f, 0.155f, 0.98f);

    static final Color SCRIM = new Color(0.018f, 0.028f, 0.045f, 0.58f);
    static final Color PANEL_BG = new Color(0.065f, 0.098f, 0.130f, 0.975f);
    static final Color WELL_BG = new Color(0.044f, 0.068f, 0.092f, 0.96f);
    static final Color TABLE_HEADER_BG = new Color(0.108f, 0.178f, 0.226f, 0.98f);
    static final Color BORDER_GOLD = new Color(0.86f, 0.74f, 0.45f, 0.86f);
    static final Color BORDER_GOLD_BRIGHT = new Color(0.95f, 0.84f, 0.54f, 0.96f);
    static final Color BORDER_WARN = new Color(0.88f, 0.54f, 0.36f, 0.90f);
    static final Color BORDER_DANGER = new Color(0.86f, 0.48f, 0.40f, 0.86f);
    static final Color BORDER_SUBTLE = new Color(0.32f, 0.44f, 0.53f, 0.58f);
    static final Color TOP_GLOW = new Color(0.98f, 0.91f, 0.72f, 0.14f);
    static final Color DIVIDER_GOLD = new Color(0.74f, 0.61f, 0.34f, 0.38f);
    static final Color GOLD_BAR = new Color(0.95f, 0.81f, 0.47f, 0.98f);

    static final Color TEXT_CREAM = new Color(0.955f, 0.918f, 0.835f, 1f);
    static final Color TEXT_GOLD = new Color(0.975f, 0.885f, 0.615f, 1f);
    static final Color TEXT_MUTED = new Color(0.62f, 0.70f, 0.76f, 0.95f);
    static final Color TEXT_DANGER = new Color(1f, 0.91f, 0.88f, 1f);
    static final Color BADGE_BG = new Color(0.14f, 0.225f, 0.275f, 0.96f);
    static final Color BADGE_BORDER = new Color(0.30f, 0.46f, 0.54f, 0.65f);
    static final Color BADGE_TEXT = new Color(0.65f, 0.80f, 0.86f, 1f);
    static final Color KEYCAP_BG = new Color(0.133f, 0.204f, 0.258f, 0.96f);
    static final Color KEYCAP_BORDER = new Color(0.31f, 0.43f, 0.52f, 0.65f);
    static final Color BACK_BG = new Color(0.125f, 0.210f, 0.265f, 0.98f);
    static final Color BACK_HOVER = new Color(0.175f, 0.285f, 0.355f, 0.98f);

    private static final Color TOGGLE_ON_BG = new Color(0.235f, 0.430f, 0.390f, 1f);
    private static final Color TOGGLE_OFF_BG = new Color(0.098f, 0.137f, 0.176f, 1f);
    private static final Color TOGGLE_OFF_BORDER = new Color(0.31f, 0.39f, 0.46f, 0.88f);
    private static final Color KNOB_ON = new Color(0.97f, 0.89f, 0.64f, 1f);
    private static final Color KNOB_OFF = new Color(0.49f, 0.57f, 0.64f, 1f);
    private static final Color SLIDER_TRACK = new Color(0.133f, 0.192f, 0.247f, 1f);
    private static final Color SLIDER_FILL = new Color(0.863f, 0.722f, 0.396f, 1f);
    private static final Color SLIDER_KNOB_DARK = new Color(0.14f, 0.10f, 0.05f, 1f);
    private static final Color SCROLL_TRACK = new Color(1f, 1f, 1f, 0.06f);

    private static final GlyphLayout glyphs = new GlyphLayout();
    private static final ReflectionHacks.RMethod updateEffects = ReflectionHacks.privateMethod(OptionsPanel.class, "updateEffects");
    private static final ReflectionHacks.RMethod playtesterAvailable = ReflectionHacks.privateMethod(OptionsPanel.class,
            "canTogglePlaytesterArt");
    private static final ReflectionHacks.RMethod modifyVolume = ReflectionHacks.privateMethod(Slider.class, "modifyVolume");
    private static final ReflectionHacks.RMethod remapKeyText = ReflectionHacks.privateMethod(RemapInputElement.class, "getKeyColumnText");
    private static final ReflectionHacks.RMethod remapControllerText = ReflectionHacks.privateMethod(RemapInputElement.class,
            "getControllerColumnText");
    private static final ReflectionHacks.RMethod remapControllerImg = ReflectionHacks.privateMethod(RemapInputElement.class,
            "getControllerColumnImg");
    private static final String DISPLAY_POLICY =
            "io.stamethyst.compatmod.ui.DisplaySettingsControlsCompatPatches";
    private static final Method dropdownPolicy = displayPolicy("shouldRenderDropdown", DropdownMenu.class);
    private static final Method togglePolicy = displayPolicy("shouldRenderToggle", ToggleButton.class);

    private PortraitSettings() {}

    private static Method displayPolicy(String name, Class<?> parameter) {
        try {
            return Class.forName(DISPLAY_POLICY).getMethod(name, parameter);
        } catch (ReflectiveOperationException | LinkageError unavailable) {
            return null;
        }
    }

    private static boolean showNative(Method policy, Object widget) {
        if (policy == null) return true;
        try {
            return (Boolean) policy.invoke(null, widget);
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    private static String cleanDropdownText(String raw) {
        if (raw == null) return "";
        return raw.replace("TAB", "").replaceAll("\\s+", " ").trim();
    }

    private static void drawDownChevron(SpriteBatch batch, float cx, float cy, float size, Color color) {
        float step = Math.max(2f, size * 0.20f);
        float w1 = size;
        float w2 = size * 0.64f;
        float w3 = size * 0.28f;
        PortraitDrawing.rectangle(batch, cx - w1 * 0.5f, cy + step * 0.5f, w1, step, color);
        PortraitDrawing.rectangle(batch, cx - w2 * 0.5f, cy - step * 0.5f, w2, step, color);
        PortraitDrawing.rectangle(batch, cx - w3 * 0.5f, cy - step * 1.5f, w3, step, color);
    }

    private static String cleanLabel(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        while (s.endsWith(":") || s.endsWith("：")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }

    @SpirePatch(clz = OptionsPanel.class, method = SpirePatch.CLASS)
    public static class State {
        public static final SpireField<Screen> screen = new SpireField<>(() -> null);
    }

    @SpirePatch(clz = InputSettingsScreen.class, method = SpirePatch.CLASS)
    public static class InputState {
        public static final SpireField<InputScreen> screen = new SpireField<>(() -> null);
    }

    private static Screen screen(OptionsPanel panel) {
        Screen result = State.screen.get(panel);
        if (result == null) {
            result = new Screen(panel);
            State.screen.set(panel, result);
        }
        return result;
    }

    private static InputScreen inputScreen(InputSettingsScreen nativeScreen) {
        InputScreen result = InputState.screen.get(nativeScreen);
        if (result == null) {
            result = new InputScreen(nativeScreen);
            InputState.screen.set(nativeScreen, result);
        }
        return result;
    }

    @SpirePatch(clz = OptionsPanel.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class Draw {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(OptionsPanel __instance, SpriteBatch batch) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            screen(__instance).render(batch);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = OptionsPanel.class, method = "update", paramtypez = {})
    public static class Input {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(OptionsPanel __instance) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            screen(__instance).update();
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = OptionsPanel.class, method = "refresh", paramtypez = {})
    public static class Rebind {
        @SpirePostfixPatch
        public static void after(OptionsPanel __instance) {
            State.screen.set(__instance, null);
        }
    }

    @SpirePatch(clz = InputSettingsScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class InputDraw {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(InputSettingsScreen __instance, SpriteBatch sb) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            inputScreen(__instance).render(sb);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = InputSettingsScreen.class, method = "update", paramtypez = {})
    public static class InputUpdate {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(InputSettingsScreen __instance) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            inputScreen(__instance).update();
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = InputSettingsScreen.class, method = "open", paramtypez = {boolean.class})
    public static class InputResetScroll {
        @SpirePostfixPatch
        public static void after(InputSettingsScreen __instance) {
            InputScreen is = InputState.screen.get(__instance);
            if (is != null) is.resetScroll();
        }
    }

    // ---- Shared Layout Metrics ----

    static float panelWidth() {
        return Math.min(PortraitViewport.referenceWidth() * 0.94f, PortraitViewport.contentWidth() * 0.92f);
    }

    static float rowHeight() {
        return Math.min(panelWidth() * 0.116f, PortraitViewport.contentHeight() * 0.068f);
    }

    static float backButtonWidth() { return panelWidth() * 0.60f; }

    static float backButtonHeight() {
        return Math.min(panelWidth() * 0.122f, PortraitViewport.contentHeight() * 0.062f);
    }

    static float backButtonCenterY() {
        return PortraitViewport.y(0.056f) + backButtonHeight() * 0.5f;
    }

    static float safeTop() {
        float pad = Math.max(12f, panelWidth() * 0.026f);
        if (CardCrawlGame.isInARun() && PortraitCombatHud.topBarActive()) {
            return PortraitCombatHud.panelBottom() - pad;
        }
        return PortraitViewport.y(0.93f);
    }

    static float safeBottom() {
        float pad = Math.max(14f, panelWidth() * 0.032f);
        return backButtonCenterY() + backButtonHeight() * 0.5f + pad;
    }

    static float safeCenterY() { return (safeTop() + safeBottom()) * 0.5f; }

    static float maxPanelHeight() { return Math.max(rowHeight() * 5f, safeTop() - safeBottom()); }

    static boolean anyConfirmPopupOpen() {
        if (CardCrawlGame.isInARun() && AbstractDungeon.settingsScreen != null) {
            if (AbstractDungeon.settingsScreen.exitPopup != null && AbstractDungeon.settingsScreen.exitPopup.shown) return true;
            if (AbstractDungeon.settingsScreen.abandonPopup != null && AbstractDungeon.settingsScreen.abandonPopup.shown) return true;
        }
        if (CardCrawlGame.mainMenuScreen != null && CardCrawlGame.mainMenuScreen.abandonPopup != null
                && CardCrawlGame.mainMenuScreen.abandonPopup.shown) {
            return true;
        }
        return false;
    }

    static boolean isSettingsBack(Object button) {
        if (button instanceof CancelButton) {
            return CardCrawlGame.isInARun()
                    && AbstractDungeon.overlayMenu != null
                    && AbstractDungeon.overlayMenu.cancelButton == button
                    && (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SETTINGS
                    || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.INPUT_SETTINGS);
        }
        if (button instanceof MenuCancelButton) {
            if (CardCrawlGame.isInARun() && AbstractDungeon.inputSettingsScreen != null
                    && AbstractDungeon.inputSettingsScreen.button == button) {
                return true;
            }
            if (CardCrawlGame.mainMenuScreen != null) {
                if (CardCrawlGame.mainMenuScreen.inputSettingsScreen != null
                        && CardCrawlGame.mainMenuScreen.inputSettingsScreen.button == button) {
                    return true;
                }
                if (CardCrawlGame.mainMenuScreen.cancelButton == button
                        && (CardCrawlGame.mainMenuScreen.screen == MainMenuScreen.CurScreen.SETTINGS
                        || CardCrawlGame.mainMenuScreen.isSettingsUp)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean backHidden(Object button) {
        if (anyConfirmPopupOpen()) return true;
        if (button instanceof CancelButton) {
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.INPUT_SETTINGS
                    && AbstractDungeon.inputSettingsScreen != null
                    && AbstractDungeon.inputSettingsScreen.button != null
                    && !AbstractDungeon.inputSettingsScreen.button.isHidden) {
                return true;
            }
            return ((CancelButton) button).isHidden;
        }
        if (button instanceof MenuCancelButton) return ((MenuCancelButton) button).isHidden;
        return true;
    }

    private static Hitbox backHitbox(Object button) {
        if (button instanceof CancelButton) return ((CancelButton) button).hb;
        if (button instanceof MenuCancelButton) return ((MenuCancelButton) button).hb;
        return null;
    }

    private static String backLabel(Object button) {
        String raw = null;
        if (button instanceof CancelButton) {
            raw = ((CancelButton) button).buttonText;
        } else if (button instanceof MenuCancelButton) {
            raw = ReflectionHacks.getPrivate(button, MenuCancelButton.class, "buttonText");
        }
        if (CardCrawlGame.isInARun() && PortraitDrawing.chinese()) return "返回游戏";
        if (raw == null || raw.trim().isEmpty()) return PortraitDrawing.chinese() ? "返回" : "Return";
        return raw;
    }

    static void drawSettingsBack(Object button, SpriteBatch batch) {
        if (backHidden(button)) return;
        Hitbox hb = backHitbox(button);
        float w = backButtonWidth();
        float h = backButtonHeight();
        float cx = PortraitViewport.x(0.5f);
        float cy = backButtonCenterY();
        boolean hovered = hb != null && hb.hovered;
        float packed = batch.getPackedColor();
        try {
            drawChamferedBox(batch, cx - w * 0.5f, cy - h * 0.5f, w, h,
                    h * 0.24f, Math.max(2f, h * 0.032f),
                    hovered ? BORDER_GOLD_BRIGHT : BORDER_GOLD,
                    hovered ? BACK_HOVER : BACK_BG);
            drawFittedText(batch, PortraitFonts.header(), backLabel(button), cx, cy,
                    w * 0.84f, h * 0.36f, true, TEXT_CREAM);
        } finally {
            batch.setColor(packed);
        }
    }

    static void mapSettingsBack(Object button, int x, int y) {
        Hitbox hb = backHitbox(button);
        if (hb == null) return;
        if (backHidden(button)) {
            InputHelper.mX = -10000;
            InputHelper.mY = -10000;
            return;
        }
        float w = backButtonWidth();
        float h = backButtonHeight();
        float cx = PortraitViewport.x(0.5f);
        float cy = backButtonCenterY();
        boolean inside = Math.abs(x - cx) < w * 0.5f && Math.abs(y - cy) < h * 0.5f;
        if (inside && InputHelper.justClickedLeft && InputHelper.justReleasedClickLeft) {
            hb.clickStarted = true;
        }
        InputHelper.mX = inside ? Math.round(hb.cX) : -10000;
        InputHelper.mY = inside ? Math.round(hb.cY) : -10000;
    }

    // ---- Shared Chamfered Drawing Primitives ----

    static void drawChamferedBox(SpriteBatch batch, float left, float bottom, float width, float height,
            float cut, float border, Color borderColor, Color fillColor) {
        float c = Math.min(cut, Math.min(width, height) * 0.45f);
        if (border > 0f && borderColor != null) {
            batch.setColor(borderColor);
            PortraitUi.drawCutCorner(batch, left, bottom, left + width, bottom + height, c);
            if (fillColor != null) {
                float ic = Math.max(1f, c - border * 0.55f);
                batch.setColor(fillColor);
                PortraitUi.drawCutCorner(batch, left + border, bottom + border,
                        left + width - border, bottom + height - border, ic);
            }
        } else if (fillColor != null) {
            batch.setColor(fillColor);
            PortraitUi.drawCutCorner(batch, left, bottom, left + width, bottom + height, c);
        }
    }

    static void drawPanel(SpriteBatch batch, float centerX, float top, float width, float height, boolean warnStyle) {
        float left = centerX - width * 0.5f;
        float bottom = top - height;
        float cut = Math.min(width * 0.036f, height * 0.08f);
        float border = Math.max(2f, width * 0.0045f);
        drawChamferedBox(batch, left, bottom, width, height, cut, border,
                warnStyle ? BORDER_WARN : BORDER_GOLD, PANEL_BG);
        float glowW = Math.max(0f, width - cut * 2f - border * 2f);
        if (glowW > 4f) {
            PortraitDrawing.rectangle(batch, left + cut + border, top - border - Math.max(2f, border * 0.8f),
                    glowW, Math.max(1.5f, border * 0.7f), TOP_GLOW);
        }
    }

    static void drawDivider(SpriteBatch batch, float centerX, float y, float width, Color color) {
        float h = Math.max(1.5f, width * 0.0028f);
        PortraitDrawing.rectangle(batch, centerX - width * 0.5f, y - h * 0.5f, width, h, color);
    }

    static void drawToggleSwitch(SpriteBatch batch, float cx, float cy, float width, float height, boolean on) {
        float left = cx - width * 0.5f;
        float bottom = cy - height * 0.5f;
        float cut = height * 0.44f;
        float border = Math.max(1.8f, height * 0.08f);
        drawChamferedBox(batch, left, bottom, width, height, cut, border,
                on ? BORDER_GOLD_BRIGHT : TOGGLE_OFF_BORDER,
                on ? TOGGLE_ON_BG : TOGGLE_OFF_BG);
        float knobSize = height - border * 3.4f;
        float travel = Math.max(0f, width - height);
        float knobCx = (left + height * 0.5f) + (on ? travel : 0f);
        drawChamferedBox(batch, knobCx - knobSize * 0.5f, cy - knobSize * 0.5f, knobSize, knobSize,
                knobSize * 0.42f, 0f, null, on ? KNOB_ON : KNOB_OFF);
    }

    static void drawPrimaryTabs(SpriteBatch batch, float centerX, float cy, float width, float height,
            boolean gameActive, boolean gameHover, boolean inputHover) {
        float pad = Math.max(4f, height * 0.10f);
        float gap = Math.max(6f, width * 0.018f);
        float tabW = (width - pad * 2f - gap) * 0.5f;
        float tabH = height - pad * 2f;
        float left = centerX - width * 0.5f;
        float bottom = cy - height * 0.5f;
        drawChamferedBox(batch, left, bottom, width, height, height * 0.18f,
                Math.max(1.5f, height * 0.025f), BORDER_SUBTLE, WELL_BG);

        float leftCx = left + pad + tabW * 0.5f;
        float rightCx = left + pad + tabW + gap + tabW * 0.5f;
        drawPrimaryTabItem(batch, OptionsPanel.TEXT[1], leftCx, cy, tabW, tabH, gameActive, gameHover);
        drawPrimaryTabItem(batch, OptionsPanel.TEXT[20], rightCx, cy, tabW, tabH, !gameActive, inputHover);
    }

    private static void drawPrimaryTabItem(SpriteBatch batch, String text, float cx, float cy, float w, float h,
            boolean active, boolean hovered) {
        float left = cx - w * 0.5f;
        float bottom = cy - h * 0.5f;
        if (active) {
            drawChamferedBox(batch, left, bottom, w, h, h * 0.20f, Math.max(1.8f, h * 0.032f),
                    BORDER_GOLD_BRIGHT, ACTIVE);
            float barH = Math.max(3f, h * 0.065f);
            PortraitDrawing.rectangle(batch, left + w * 0.16f, bottom + Math.max(1.5f, h * 0.025f),
                    w * 0.68f, barH, GOLD_BAR);
        } else if (hovered) {
            drawChamferedBox(batch, left, bottom, w, h, h * 0.20f, Math.max(1.2f, h * 0.02f),
                    BORDER_SUBTLE, ROW_HOVER);
        }
        drawFittedText(batch, PortraitFonts.header(), text, cx, cy, w * 0.84f, h * 0.40f,
                true, active ? TEXT_GOLD : (hovered ? TEXT_CREAM : TEXT_MUTED));
    }

    static void drawFittedText(SpriteBatch batch, BitmapFont font, String text, float x, float y,
            float maxWidth, float targetCapHeight, boolean centered, Color color) {
        if (text == null || text.isEmpty()) return;
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        boolean oldMarkup = font.getData().markupEnabled;
        font.getData().markupEnabled = false;
        try {
            float baseScale = targetCapHeight / Math.max(1f, font.getCapHeight());
            font.getData().setScale(oldX * baseScale, oldY * baseScale);
            glyphs.setText(font, text);
            if (glyphs.width > maxWidth && glyphs.width > 1f) {
                float shrink = maxWidth / glyphs.width;
                font.getData().setScale(oldX * baseScale * shrink, oldY * baseScale * shrink);
                glyphs.setText(font, text);
            }
            if (centered) {
                FontHelper.renderFontCentered(batch, font, text, x, y, color);
            } else {
                FontHelper.renderFontLeftTopAligned(batch, font, text, x, y + font.getCapHeight() * 0.5f, color);
            }
        } finally {
            font.getData().setScale(oldX, oldY);
            font.getData().markupEnabled = oldMarkup;
        }
    }

    static void drawLeftWrappedText(SpriteBatch batch, BitmapFont font, String text, float leftX, float cy,
            float maxWidth, float maxHeight, float targetCapHeight, Color color) {
        if (text == null || text.isEmpty()) return;
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        boolean oldMarkup = font.getData().markupEnabled;
        font.getData().markupEnabled = false;
        try {
            float fit = targetCapHeight / Math.max(1f, font.getCapHeight());
            font.getData().setScale(oldX * fit, oldY * fit);
            glyphs.setText(font, text, color, maxWidth, Align.left, true);
            if (glyphs.height > maxHeight && glyphs.height > 1f) {
                fit *= maxHeight / glyphs.height;
                font.getData().setScale(oldX * fit, oldY * fit);
                glyphs.setText(font, text, color, maxWidth, Align.left, true);
            }
            font.draw(batch, glyphs, leftX, cy + glyphs.height * 0.5f);
        } finally {
            font.getData().setScale(oldX, oldY);
            font.getData().markupEnabled = oldMarkup;
        }
    }

    private static boolean wrapsToMultipleLines(BitmapFont font, String text, float maxWidth, float targetCapHeight) {
        if (text == null || text.isEmpty()) return false;
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        boolean oldMarkup = font.getData().markupEnabled;
        font.getData().markupEnabled = false;
        try {
            float fit = targetCapHeight / Math.max(1f, font.getCapHeight());
            font.getData().setScale(oldX * fit, oldY * fit);
            glyphs.setText(font, text);
            return glyphs.width > maxWidth * 0.98f;
        } finally {
            font.getData().setScale(oldX, oldY);
            font.getData().markupEnabled = oldMarkup;
        }
    }

    // ---- Legacy Public / Package Helpers (kept for PortraitCharacterSelect & PortraitTouchControls) ----

    static void frame(SpriteBatch batch, float center, float top, float width, float height) {
        drawPanel(batch, center, top, width, height, false);
    }

    static void checkmark(SpriteBatch batch, float x, float y, float side, boolean checked) {
        drawToggleSwitch(batch, x, y, side * 1.72f, side * 0.96f, checked);
    }

    static void wrappedLabel(SpriteBatch batch, String text, float x, float y, float width,
            float lineHeight, float maxHeight) {
        BitmapFont font = PortraitFonts.body();
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        boolean oldMarkup = font.getData().markupEnabled;
        font.getData().markupEnabled = false;
        float fit = lineHeight / Math.max(1f, font.getCapHeight());
        font.getData().setScale(oldX * fit, oldY * fit);
        try {
            glyphs.setText(font, text, TEXT_CREAM, width, Align.center, true);
            if (glyphs.height > maxHeight && glyphs.height > 1f) {
                fit *= maxHeight / glyphs.height;
                font.getData().setScale(oldX * fit, oldY * fit);
                glyphs.setText(font, text, TEXT_CREAM, width, Align.center, true);
            }
            font.draw(batch, glyphs, x - width * 0.5f, y + glyphs.height * 0.5f);
        } finally {
            font.getData().setScale(oldX, oldY);
            font.getData().markupEnabled = oldMarkup;
        }
    }

    static void label(SpriteBatch batch, String text, float x, float y, float width, float height,
            boolean centered) {
        drawFittedText(batch, PortraitFonts.header(), text, x, y, width, height, centered, TEXT_CREAM);
    }

    // ---- Helper for Android Single-Frame Tap + Standard Hitbox Click ----

    private static void triggerHitboxUpdate(Hitbox hb, boolean inside, Runnable updater) {
        int rawX = InputHelper.mX;
        int rawY = InputHelper.mY;
        boolean combinedTap = inside && InputHelper.justClickedLeft && InputHelper.justReleasedClickLeft;
        try {
            InputHelper.mX = inside ? Math.round(hb.cX) : -10000;
            InputHelper.mY = inside ? Math.round(hb.cY) : -10000;
            if (combinedTap) {
                hb.clickStarted = true;
                InputHelper.justClickedLeft = false;
            }
            updater.run();
        } finally {
            InputHelper.mX = rawX;
            InputHelper.mY = rawY;
            if (combinedTap) InputHelper.justClickedLeft = true;
        }
    }

    // ---- Game Settings Screen (OptionsPanel) ----

    public static final class Screen {
        private final OptionsPanel panel;
        private final List<List<Control>> sections = new ArrayList<>();
        private final Control[] tabs = new Control[4];
        private final Control input, abandon, exit;
        private int current;
        private float width, innerW, pad, row, centerX, centerY, top, height;
        private float primaryCy, primaryH, dividerY;
        private boolean showArt;
        private Control grabbedSlider;

        // Open dropdown overlay state
        private Control openDropdownControl;
        private float dropdownScroll;
        private float dropdownMaxScroll;
        private boolean dropdownDragging;
        private boolean dropdownMoved;
        private float dropdownPressY;
        private float dropdownLastY;
        private final Rectangle dropdownClip = new Rectangle();

        Screen(OptionsPanel panel) {
            this.panel = panel;
            for (int i = 0; i < 4; i++) sections.add(new ArrayList<>());
            String[] text = OptionsPanel.TEXT;
            int[] headings = {2, 5, 8, 12};
            String[] ids = {"graphics", "sound", "preferences", "other"};
            for (int i = 0; i < tabs.length; i++) tabs[i] = new Control("tab." + ids[i], text[headings[i]], null);
            input = new Control("input", text[20], field("inputSettingsHb"));
            abandon = new Control("abandon", PortraitDrawing.chinese() ? "放弃这局游戏" : "Abandon run", field("abandonBtn"));
            exit = new Control("exit", text[16], field("exitBtn"));
            add(0, "resolution", cleanLabel(line(3, 0)), "resoDropdown");
            add(0, "frame-rate", cleanLabel(line(3, 1)), "fpsDropdown");
            add(0, "fullscreen", line(4, 0), "fsToggle");
            add(0, "borderless", line(4, 1), "wfsToggle");
            add(0, "screen-shake", line(4, 2), "ssToggle");
            add(0, "vsync", text[17].replace("NL", "").trim(), "vSyncToggle");
            add(1, "master-volume", cleanLabel(line(6, 0)), "masterSlider");
            add(1, "music-volume", cleanLabel(line(6, 1)), "bgmSlider");
            add(1, "effects-volume", cleanLabel(line(6, 2)), "sfxSlider");
            add(1, "ambient-audio", line(7, 0), "ambienceToggle");
            add(1, "mute-background", line(7, 1), "muteBgToggle");
            add(2, "damage-total", line(9, 0), "sumToggle");
            add(2, "blocked-total", line(9, 1), "blockToggle");
            add(2, "single-confirm", line(9, 2), "confirmToggle");
            add(2, "particles", text[21], "effectsToggle");
            add(2, "fast-mode", text[10], "fastToggle");
            add(2, "card-shortcuts", text[19], "cardKeyOverlayToggle");
            add(3, "language", cleanLabel(text[13]), "languageDropdown");
            add(3, "telemetry", text[14].replace("#b", "").replace("(?)", "").trim(), "uploadToggle");
            add(3, "long-press", text[25], "longPressToggle");
            add(3, "larger-type", text[26], "bigTextToggle");
            add(3, "playtester-art", text[18], "playtesterToggle");
        }

        private Object field(String name) { return ReflectionHacks.getPrivate(panel, OptionsPanel.class, name); }

        private static String line(int index, int line) {
            String[] lines = OptionsPanel.TEXT[index].split("\\s+NL\\s+");
            return lines[Math.min(line, lines.length - 1)].trim();
        }

        private void add(int section, String key, String label, String field) {
            Object widget = field(field);
            if (widget == null || (widget instanceof ToggleButton && !showNative(togglePolicy, widget))) return;
            if (widget instanceof DropdownMenu && !showNative(dropdownPolicy, widget)) {
                String value = "frame-rate".equals(key) ? (Settings.MAX_FPS + " FPS")
                        : (Settings.WIDTH + " × " + Settings.HEIGHT);
                widget = value;
            }
            sections.get(section).add(new Control(key, label, widget));
        }

        private boolean shown(Control control) { return showArt || !"playtester-art".equals(control.key); }

        private String liveExitLabel() {
            if (exit.widget instanceof ExitGameButton) {
                String lbl = ReflectionHacks.getPrivate(exit.widget, ExitGameButton.class, "label");
                if (lbl != null && !lbl.trim().isEmpty()) return lbl;
            }
            return CardCrawlGame.isInARun() ? OptionsPanel.TEXT[16] : OptionsPanel.TEXT[15];
        }

        private void layout() {
            width = panelWidth();
            pad = width * 0.044f;
            innerW = width - pad * 2f;
            showArt = playtesterAvailable.invoke(panel);

            List<Control> visible = new ArrayList<>();
            for (Control c : sections.get(current)) {
                if (shown(c)) visible.add(c);
            }

            float baseRow = rowHeight();
            float primaryMul = 1.02f;
            float subMul = 0.78f;
            float footerMul = 0.96f;
            float gapMul = 0.24f;
            float rowGapMul = 0.16f;
            float dividerMul = 0.44f;

            // Measure row multipliers (allow slightly taller rows for 2-line Chinese preferences).
            float[] rowMuls = new float[visible.size()];
            float rowsMulSum = 0f;
            for (int i = 0; i < visible.size(); i++) {
                Control c = visible.get(i);
                float mul = 0.98f;
                if (c.widget instanceof ToggleButton) {
                    float labelAvailW = innerW * 0.72f;
                    if (wrapsToMultipleLines(PortraitFonts.body(), c.text, labelAvailW, baseRow * 0.30f)) {
                        mul = 1.18f;
                    }
                }
                rowMuls[i] = mul;
                rowsMulSum += mul;
            }
            if (visible.size() > 1) rowsMulSum += (visible.size() - 1) * rowGapMul;

            float totalMul = primaryMul + gapMul + subMul + gapMul + rowsMulSum + dividerMul + footerMul;
            float maxInnerH = Math.max(baseRow * 4f, maxPanelHeight() - pad * 2f);
            row = Math.min(baseRow, maxInnerH / Math.max(1f, totalMul));

            primaryH = row * primaryMul;
            float subH = row * subMul;
            float footerH = row * footerMul;
            float gap = row * gapMul;
            float rowGap = row * rowGapMul;
            float dividerGap = row * dividerMul;

            float innerH = primaryH + gap + subH + gap + rowsMulSum * row + dividerGap + footerH;
            height = innerH + pad * 2f;
            centerX = PortraitViewport.x(0.5f);
            centerY = safeCenterY();
            top = centerY + height * 0.5f;

            float cursor = top - pad;
            primaryCy = cursor - primaryH * 0.5f;
            float segPad = Math.max(4f, primaryH * 0.10f);
            float segGap = Math.max(6f, innerW * 0.018f);
            float segW = (innerW - segPad * 2f - segGap) * 0.5f;
            input.place(centerX + segGap * 0.5f + segW * 0.5f, primaryCy, segW, primaryH - segPad * 2f);
            cursor -= primaryH + gap;

            float subCy = cursor - subH * 0.5f;
            float subGap = innerW * 0.018f;
            float subW = (innerW - subGap * 3f) * 0.25f;
            float subStart = centerX - innerW * 0.5f + subW * 0.5f;
            for (int i = 0; i < tabs.length; i++) {
                tabs[i].place(subStart + i * (subW + subGap), subCy, subW, subH);
            }
            cursor -= subH + gap;

            for (int i = 0; i < visible.size(); i++) {
                float rh = row * rowMuls[i];
                visible.get(i).place(centerX, cursor - rh * 0.5f, innerW, rh);
                cursor -= rh + (i + 1 < visible.size() ? rowGap : 0f);
            }

            dividerY = cursor - dividerGap * 0.5f;
            cursor -= dividerGap;

            float footerCy = cursor - footerH * 0.5f;
            if (CardCrawlGame.isInARun()) {
                float fGap = innerW * 0.028f;
                float fW = (innerW - fGap) * 0.5f;
                abandon.place(centerX - (fW + fGap) * 0.5f, footerCy, fW, footerH);
                exit.place(centerX + (fW + fGap) * 0.5f, footerCy, fW, footerH);
            } else {
                abandon.place(-10000f, -10000f, 1f, 1f);
                exit.place(centerX, footerCy, innerW * 0.62f, footerH);
            }
        }

        private Control findOpenDropdown() {
            for (Control c : sections.get(current)) {
                if (c.widget instanceof DropdownMenu && ((DropdownMenu) c.widget).isOpen) return c;
            }
            return null;
        }

        void render(SpriteBatch batch) {
            layout();
            float packed = batch.getPackedColor();
            try {
                PortraitDrawing.rectangle(batch, 0f, 0f, Settings.WIDTH, Settings.HEIGHT, SCRIM);
                drawPanel(batch, centerX, top, width, height, false);
                drawPrimaryTabs(batch, centerX, primaryCy, innerW, primaryH, true, false, input.hit());

                for (int i = 0; i < tabs.length; i++) {
                    Control t = tabs[i];
                    boolean active = i == current;
                    boolean hovered = t.hit();
                    drawChamferedBox(batch, t.x - t.width * 0.5f, t.y - t.height * 0.5f, t.width, t.height,
                            t.height * 0.22f, Math.max(1.5f, t.height * 0.032f),
                            active ? BORDER_GOLD_BRIGHT : BORDER_SUBTLE,
                            active ? ACTIVE : (hovered ? ROW_HOVER : ROW));
                    drawFittedText(batch, PortraitFonts.header(), t.text, t.x, t.y,
                            t.width * 0.84f, t.height * 0.42f, true,
                            active ? TEXT_GOLD : (hovered ? TEXT_CREAM : TEXT_MUTED));
                }

                Control open = findOpenDropdown();
                for (Control control : sections.get(current)) {
                    if (shown(control)) control.drawRow(batch, open == null && control.hit());
                }

                drawDivider(batch, centerX, dividerY, innerW * 0.92f, DIVIDER_GOLD);

                if (CardCrawlGame.isInARun()) {
                    boolean ah = open == null && abandon.hit();
                    drawChamferedBox(batch, abandon.x - abandon.width * 0.5f, abandon.y - abandon.height * 0.5f,
                            abandon.width, abandon.height, abandon.height * 0.22f, Math.max(1.8f, abandon.height * 0.032f),
                            BORDER_DANGER, ah ? DESTRUCTIVE_HOVER : DESTRUCTIVE);
                    drawFittedText(batch, PortraitFonts.header(), abandon.text, abandon.x, abandon.y,
                            abandon.width * 0.86f, abandon.height * 0.37f, true, TEXT_DANGER);
                }

                boolean eh = open == null && exit.hit();
                drawChamferedBox(batch, exit.x - exit.width * 0.5f, exit.y - exit.height * 0.5f,
                        exit.width, exit.height, exit.height * 0.22f, Math.max(1.8f, exit.height * 0.032f),
                        eh ? BORDER_GOLD_BRIGHT : BORDER_GOLD, eh ? ACTIVE_HOVER : ACTIVE);
                drawFittedText(batch, PortraitFonts.header(), liveExitLabel(), exit.x, exit.y,
                        exit.width * 0.86f, exit.height * 0.37f, true, TEXT_CREAM);

                if (open != null) {
                    renderDropdownOverlay(batch, open, (DropdownMenu) open.widget);
                }

                for (AbstractGameEffect effect : panel.effects) effect.render(batch);
            } finally {
                batch.setColor(packed);
            }
        }

        void update() {
            layout();
            if (anyConfirmPopupOpen()) return;

            Control open = findOpenDropdown();
            if (open != null) {
                updateDropdownOverlay(open, (DropdownMenu) open.widget);
                return;
            }
            openDropdownControl = null;

            // Active slider drag takes priority so sliding finger outside the row stays smooth.
            if (grabbedSlider != null) {
                grabbedSlider.updateSliderDrag(true);
                if (!InputHelper.isMouseDown) grabbedSlider = null;
                updateEffects.invoke(panel);
                return;
            }

            Control selected = CardCrawlGame.isInARun() && abandon.hit() ? abandon : null;
            if (selected == null && exit.hit()) selected = exit;
            List<Control> rows = sections.get(current);
            for (int i = rows.size() - 1; selected == null && i >= 0; i--) {
                Control c = rows.get(i);
                if (shown(c) && c.hit()) selected = c;
            }

            for (int i = 0; i < tabs.length; i++) {
                if (selected == null && tabs[i].hit() && InputHelper.justClickedLeft) {
                    if (current != i) {
                        CardCrawlGame.sound.play("UI_CLICK_1");
                        current = i;
                    }
                    InputHelper.justClickedLeft = false;
                    return;
                }
            }

            input.updateControl(panel, selected == null && input.hit());
            if (CardCrawlGame.isInARun()) abandon.updateControl(panel, selected == abandon);
            exit.updateControl(panel, selected == exit);
            updateEffects.invoke(panel);

            for (Control c : rows) {
                if (!shown(c)) continue;
                if (c.widget instanceof Slider) {
                    if (selected == c && InputHelper.justClickedLeft) {
                        grabbedSlider = c;
                        c.updateSliderDrag(true);
                    }
                } else if (c.widget instanceof DropdownMenu) {
                    DropdownMenu dm = (DropdownMenu) c.widget;
                    if (selected == c && InputHelper.justClickedLeft && !dm.rows.isEmpty()) {
                        CardCrawlGame.sound.play("UI_CLICK_1");
                        dm.isOpen = true;
                        openDropdownControl = c;
                        dropdownScroll = Math.max(0f, (dm.getSelectedIndex() - 2) * dropdownItemH());
                        dropdownDragging = false;
                        dropdownMoved = false;
                        InputHelper.justClickedLeft = false;
                        break;
                    }
                } else {
                    c.updateControl(panel, selected == c);
                }
            }
            if (selected != null) selected.tip();
        }

        private float dropdownItemH() { return Math.min(row * 0.92f, PortraitViewport.contentHeight() * 0.056f); }

        private float[] dropdownBoxBounds(DropdownMenu dm) {
            float itemH = dropdownItemH();
            float boxW = innerW * 0.92f;
            float headerH = itemH * 0.95f;
            float padBox = Math.max(10f, boxW * 0.03f);
            float maxListH = Math.min(maxPanelHeight() * 0.68f, itemH * 7.2f);
            float contentH = dm.rows.size() * itemH;
            float listH = Math.min(maxListH, contentH);
            float totalH = padBox * 2f + headerH + listH;
            float left = centerX - boxW * 0.5f;
            float bottom = safeCenterY() - totalH * 0.5f;
            return new float[] {left, bottom, boxW, totalH, padBox, headerH, listH, itemH};
        }

        private void renderDropdownOverlay(SpriteBatch batch, Control owner, DropdownMenu dm) {
            if (dm.rows.isEmpty()) return;
            float[] b = dropdownBoxBounds(dm);
            float left = b[0], bottom = b[1], boxW = b[2], totalH = b[3];
            float padBox = b[4], headerH = b[5], listH = b[6], itemH = b[7];
            float topBox = bottom + totalH;
            float listTop = topBox - padBox - headerH;
            float listBottom = listTop - listH;
            dropdownMaxScroll = Math.max(0f, dm.rows.size() * itemH - listH);
            dropdownScroll = MathUtils.clamp(dropdownScroll, 0f, dropdownMaxScroll);

            PortraitDrawing.rectangle(batch, 0f, 0f, Settings.WIDTH, Settings.HEIGHT,
                    new Color(0.01f, 0.015f, 0.025f, 0.65f));
            drawPanel(batch, centerX, topBox, boxW, totalH, false);

            drawFittedText(batch, PortraitFonts.header(), owner.text, centerX, topBox - padBox - headerH * 0.45f,
                    boxW * 0.80f, headerH * 0.42f, true, TEXT_GOLD);
            drawDivider(batch, centerX, listTop + 2f, boxW - padBox * 2f, DIVIDER_GOLD);

            float listLeft = left + padBox;
            float scrollW = dropdownMaxScroll > 1f ? Math.max(8f, boxW * 0.016f) : 0f;
            float listW = boxW - padBox * 2f - (scrollW > 0f ? scrollW + 10f : 0f);

            dropdownClip.set(listLeft, listBottom, listW, listH);
            PortraitDesktopViewport.clip(dropdownClip);
            batch.flush();
            boolean clipped = ScissorStack.pushScissors(dropdownClip);
            try {
                int selectedIdx = dm.getSelectedIndex();
                for (int i = 0; i < dm.rows.size(); i++) {
                    float ry = listTop - (i + 1) * itemH + dropdownScroll;
                    if (ry + itemH < listBottom || ry > listTop) continue;
                    Object rowObj = dm.rows.get(i);
                    String text = cleanDropdownText(ReflectionHacks.getPrivate(rowObj, rowObj.getClass(), "text"));
                    boolean sel = i == selectedIdx;
                    boolean hov = InputHelper.mX >= listLeft && InputHelper.mX <= listLeft + listW
                            && InputHelper.mY >= Math.max(listBottom, ry) && InputHelper.mY <= Math.min(listTop, ry + itemH);
                    Color fill = sel ? ACTIVE : (hov ? ROW_HOVER : (i % 2 == 0 ? ROW : ROW_ALT));
                    drawChamferedBox(batch, listLeft, ry + 2f, listW, itemH - 4f, itemH * 0.16f,
                            sel ? 1.5f : 0f, sel ? BORDER_GOLD : null, fill);
                    drawFittedText(batch, PortraitFonts.body(), text, listLeft + listW * 0.05f, ry + itemH * 0.5f,
                            listW * 0.80f, itemH * 0.36f, false, sel ? TEXT_GOLD : TEXT_CREAM);
                    if (sel) {
                        float dot = Math.max(10f, itemH * 0.24f);
                        drawChamferedBox(batch, listLeft + listW * 0.92f - dot * 0.5f, ry + itemH * 0.5f - dot * 0.5f,
                                dot, dot, dot * 0.45f, 0f, null, GOLD_BAR);
                    }
                }
            } finally {
                if (clipped) {
                    batch.flush();
                    ScissorStack.popScissors();
                }
            }

            if (dropdownMaxScroll > 1f) {
                float trackX = left + boxW - padBox - scrollW;
                PortraitDrawing.rectangle(batch, trackX, listBottom + 4f, scrollW, listH - 8f, SCROLL_TRACK);
                float ratio = MathUtils.clamp(listH / (dm.rows.size() * itemH), 0.14f, 1f);
                float thumbH = Math.max(24f, (listH - 8f) * ratio);
                float pct = dropdownScroll / dropdownMaxScroll;
                float thumbY = (listTop - 4f - thumbH) - pct * ((listH - 8f) - thumbH);
                drawChamferedBox(batch, trackX, thumbY, scrollW, thumbH, scrollW * 0.45f, 0f, null, BORDER_GOLD);
            }
        }

        private void updateDropdownOverlay(Control owner, DropdownMenu dm) {
            if (dm.rows.isEmpty()) {
                dm.isOpen = false;
                return;
            }
            float[] b = dropdownBoxBounds(dm);
            float left = b[0], bottom = b[1], boxW = b[2], totalH = b[3];
            float padBox = b[4], headerH = b[5], listH = b[6], itemH = b[7];
            float topBox = bottom + totalH;
            float listTop = topBox - padBox - headerH;
            float listBottom = listTop - listH;
            dropdownMaxScroll = Math.max(0f, dm.rows.size() * itemH - listH);

            if (InputHelper.pressedEscape) {
                InputHelper.pressedEscape = false;
                dm.isOpen = false;
                return;
            }
            if (InputHelper.scrolledDown) dropdownScroll += itemH * 1.4f;
            if (InputHelper.scrolledUp) dropdownScroll -= itemH * 1.4f;

            boolean inBox = InputHelper.mX >= left && InputHelper.mX <= left + boxW
                    && InputHelper.mY >= bottom && InputHelper.mY <= topBox;
            boolean inList = InputHelper.mX >= left + padBox && InputHelper.mX <= left + boxW - padBox
                    && InputHelper.mY >= listBottom && InputHelper.mY <= listTop;

            if (InputHelper.justClickedLeft) {
                if (!inBox) {
                    dm.isOpen = false;
                    InputHelper.justClickedLeft = false;
                    return;
                }
                if (inList) {
                    dropdownDragging = true;
                    dropdownMoved = false;
                    dropdownPressY = InputHelper.mY;
                    dropdownLastY = InputHelper.mY;
                }
            }
            if (dropdownDragging && InputHelper.isMouseDown) {
                float dy = InputHelper.mY - dropdownLastY;
                if (Math.abs(InputHelper.mY - dropdownPressY) > Math.max(10f, itemH * 0.22f)) {
                    dropdownMoved = true;
                }
                if (dropdownMoved) {
                    dropdownScroll += dy;
                }
                dropdownLastY = InputHelper.mY;
            }
            if (InputHelper.justReleasedClickLeft) {
                if (dropdownDragging && !dropdownMoved && inList) {
                    float fromTop = (listTop - InputHelper.mY) + dropdownScroll;
                    int idx = (int) Math.floor(fromTop / itemH);
                    if (idx >= 0 && idx < dm.rows.size()) {
                        Object rowObj = dm.rows.get(idx);
                        String text = ReflectionHacks.getPrivate(rowObj, rowObj.getClass(), "text");
                        CardCrawlGame.sound.play("UI_CLICK_1");
                        dm.setSelectedIndex(idx);
                        DropdownMenuListener listener = ReflectionHacks.getPrivate(dm, DropdownMenu.class, "listener");
                        if (listener != null) listener.changedSelectionTo(dm, idx, text);
                        dm.isOpen = false;
                    }
                }
                dropdownDragging = false;
                dropdownMoved = false;
            }
            dropdownScroll = MathUtils.clamp(dropdownScroll, 0f, dropdownMaxScroll);
        }
    }

    private static final class Control {
        final String key, text;
        final Object widget;
        final Hitbox button;
        float x, y, width, height;

        Control(String key, String text, Object widget) {
            this.key = key;
            this.text = text;
            this.widget = widget;
            button = widget instanceof ToggleButton ? ((ToggleButton) widget).hb
                    : widget instanceof Hitbox ? (Hitbox) widget
                    : widget instanceof AbandonRunButton || widget instanceof ExitGameButton
                    ? ReflectionHacks.getPrivate(widget, widget.getClass(), "hb") : null;
        }

        void place(float cx, float cy, float w, float h) {
            x = cx;
            y = cy;
            width = w;
            height = h;
        }

        boolean hit() {
            return Math.abs(InputHelper.mX - x) < width * 0.5f && Math.abs(InputHelper.mY - y) < height * 0.5f;
        }

        private float sliderTrackLeft() { return x - width * 0.04f; }
        private float sliderTrackRight() { return x + width * 0.34f; }

        void drawRow(SpriteBatch batch, boolean hovered) {
            float left = x - width * 0.5f;
            float bottom = y - height * 0.5f;
            drawChamferedBox(batch, left, bottom, width, height, height * 0.18f,
                    Math.max(1.3f, height * 0.022f),
                    hovered ? BORDER_GOLD : BORDER_SUBTLE,
                    hovered ? ROW_HOVER : ROW);

            float padX = width * 0.042f;
            if (widget instanceof ToggleButton) {
                float swH = Math.min(height * 0.48f, width * 0.072f);
                float swW = swH * 1.82f;
                float swCx = left + width - padX - swW * 0.5f;
                float labelMaxW = (swCx - swW * 0.5f) - (left + padX) - padX * 0.6f;
                float cap = Math.min(height * 0.285f, width * 0.036f);
                drawLeftWrappedText(batch, PortraitFonts.body(), text, left + padX, y,
                        labelMaxW, height * 0.78f, cap, TEXT_CREAM);
                drawToggleSwitch(batch, swCx, y, swW, swH, ((ToggleButton) widget).enabled);
            } else if (widget instanceof Slider) {
                Slider slider = (Slider) widget;
                float vol = MathUtils.clamp(ReflectionHacks.<Float>getPrivate(slider, Slider.class, "volume"), 0f, 1f);
                float trackL = sliderTrackLeft();
                float trackR = sliderTrackRight();
                float trackW = Math.max(20f, trackR - trackL);
                float trackH = Math.max(6f, height * 0.12f);
                float labelMaxW = trackL - (left + padX) - padX * 0.5f;
                drawFittedText(batch, PortraitFonts.body(), text, left + padX, y,
                        labelMaxW, Math.min(height * 0.31f, width * 0.036f), false, TEXT_CREAM);

                drawChamferedBox(batch, trackL, y - trackH * 0.5f, trackW, trackH, trackH * 0.42f, 0f, null, SLIDER_TRACK);
                float fillW = trackW * vol;
                if (fillW > 2f) {
                    drawChamferedBox(batch, trackL, y - trackH * 0.5f, fillW, trackH, trackH * 0.42f, 0f, null, SLIDER_FILL);
                }
                float knobX = trackL + fillW;
                float knobSide = Math.max(16f, height * 0.34f);
                drawChamferedBox(batch, knobX - knobSide * 0.5f, y - knobSide * 0.5f, knobSide, knobSide,
                        knobSide * 0.46f, Math.max(1.8f, knobSide * 0.10f), SLIDER_KNOB_DARK, KNOB_ON);

                String pct = Math.round(vol * 100f) + "%";
                drawFittedText(batch, PortraitFonts.header(), pct, left + width - padX - width * 0.055f, y,
                        width * 0.12f, Math.min(height * 0.29f, width * 0.033f), true, TEXT_GOLD);
            } else if (widget instanceof DropdownMenu) {
                DropdownMenu dm = (DropdownMenu) widget;
                String currentVal = "";
                if (!dm.rows.isEmpty()) {
                    int idx = MathUtils.clamp(dm.getSelectedIndex(), 0, dm.rows.size() - 1);
                    Object rowObj = dm.rows.get(idx);
                    currentVal = cleanDropdownText(ReflectionHacks.getPrivate(rowObj, rowObj.getClass(), "text"));
                }
                float pillW = width * 0.44f;
                float pillH = height * 0.64f;
                float pillLeft = left + width - padX - pillW;
                drawFittedText(batch, PortraitFonts.body(), text, left + padX, y,
                        pillLeft - (left + padX) - padX * 0.5f, Math.min(height * 0.31f, width * 0.036f),
                        false, TEXT_CREAM);
                drawChamferedBox(batch, pillLeft, y - pillH * 0.5f, pillW, pillH, pillH * 0.22f,
                        Math.max(1.5f, pillH * 0.04f), BORDER_GOLD, KEYCAP_BG);
                float chevronSize = Math.max(12f, pillH * 0.26f);
                float chevronCx = pillLeft + pillW - pillH * 0.42f;
                drawFittedText(batch, PortraitFonts.body(), currentVal,
                        pillLeft + (pillW - pillH * 0.55f) * 0.5f, y,
                        pillW - pillH * 0.85f, pillH * 0.46f, true, TEXT_GOLD);
                drawDownChevron(batch, chevronCx, y, chevronSize, TEXT_GOLD);
            } else if (widget instanceof String) {
                String value = (String) widget;
                String badgeText = PortraitDrawing.chinese() ? "启动器管理" : "Launcher";
                float badgeH = height * 0.46f;
                float badgeW = Math.min(width * 0.24f, Math.max(width * 0.18f, badgeH * 3.1f));
                float badgeLeft = left + width - padX - badgeW;
                drawChamferedBox(batch, badgeLeft, y - badgeH * 0.5f, badgeW, badgeH, badgeH * 0.24f,
                        Math.max(1.2f, badgeH * 0.04f), BADGE_BORDER, BADGE_BG);
                drawFittedText(batch, PortraitFonts.body(), badgeText, badgeLeft + badgeW * 0.5f, y,
                        badgeW * 0.86f, badgeH * 0.48f, true, BADGE_TEXT);

                float valRight = badgeLeft - width * 0.025f;
                float valW = width * 0.34f;
                drawFittedText(batch, PortraitFonts.header(), value, valRight - valW * 0.5f, y,
                        valW, Math.min(height * 0.30f, width * 0.035f), true, TEXT_GOLD);
                drawFittedText(batch, PortraitFonts.body(), text, left + padX, y,
                        (valRight - valW) - (left + padX) - padX * 0.3f,
                        Math.min(height * 0.31f, width * 0.036f), false, TEXT_CREAM);
            }
        }

        void updateSliderDrag(boolean active) {
            if (!active || !(widget instanceof Slider)) return;
            Slider slider = (Slider) widget;
            float left = sliderTrackLeft();
            float right = sliderTrackRight();
            float vol = MathUtils.clamp((InputHelper.mX - left) / Math.max(1f, right - left), 0f, 1f);
            float nativeLeft = 1235f * Settings.xScale;
            float nativeSpan = 230f * Settings.xScale;
            ReflectionHacks.setPrivate(slider, Slider.class, "x", nativeLeft + vol * nativeSpan);
            ReflectionHacks.setPrivate(slider, Slider.class, "volume", vol);
            modifyVolume.invoke(slider);
        }

        void updateControl(OptionsPanel panel, boolean allowed) {
            if (button == null) return;
            if (widget instanceof ToggleButton) {
                int rawX = InputHelper.mX;
                int rawY = InputHelper.mY;
                try {
                    InputHelper.mX = allowed ? Math.round(button.cX) : -10000;
                    InputHelper.mY = allowed ? Math.round(button.cY) : -10000;
                    ((ToggleButton) widget).update();
                } finally {
                    InputHelper.mX = rawX;
                    InputHelper.mY = rawY;
                }
            } else {
                triggerHitboxUpdate(button, allowed, () -> {
                    if (widget instanceof AbandonRunButton) ((AbandonRunButton) widget).update();
                    else if (widget instanceof ExitGameButton) ((ExitGameButton) widget).update();
                    else button.encapsulatedUpdate(panel);
                });
            }
        }

        void tip() {
            int index;
            switch (key) {
                case "telemetry": index = 0; break;
                case "fullscreen": index = 1; break;
                case "borderless": index = 2; break;
                case "frame-rate": index = 3; break;
                default: return;
            }
            if (!Settings.isTouchScreen) {
                TipHelper.renderGenericTip(PortraitViewport.x(0.04f), y - height * 0.5f,
                        OptionsPanel.LABEL[index], OptionsPanel.MSG[index]);
            }
        }
    }

    // ---- Input Settings Screen (InputSettingsScreen) — Native 1:1 High-DPI ----

    public static final class InputScreen {
        private final InputSettingsScreen nativeScreen;
        private float width, innerW, pad, centerX, centerY, top, height;
        private float primaryCy, primaryH, primaryTabW, primaryTabH, gameTabCx;
        private float tableLeft, tableBottom, tableW, tableH, headerH, listTop, listBottom, listH, itemH;
        private float scrollTrackX, scrollTrackW;
        private float ctrlCx, ctrlCy, ctrlW, ctrlH;
        private float touchCx, touchCy, touchW, touchH;
        private float resetCx, resetCy, resetW, resetH;

        private float scrollOffset;
        private float maxScroll;
        private boolean listDragging;
        private boolean listMoved;
        private boolean barDragging;
        private float pressY;
        private float lastY;
        private final Rectangle listClip = new Rectangle();

        InputScreen(InputSettingsScreen nativeScreen) {
            this.nativeScreen = nativeScreen;
        }

        void resetScroll() {
            scrollOffset = 0f;
            listDragging = false;
            listMoved = false;
            barDragging = false;
        }

        private ArrayList<RemapInputElement> elements() {
            return ReflectionHacks.getPrivate(nativeScreen, InputSettingsScreen.class, "elements");
        }

        private boolean showControllerColumn() {
            return Settings.isControllerMode;
        }

        private void layout() {
            width = panelWidth();
            pad = width * 0.044f;
            innerW = width - pad * 2f;
            float baseRow = rowHeight();

            height = Math.min(maxPanelHeight(), Math.max(baseRow * 9.5f, maxPanelHeight() * 0.94f));
            centerX = PortraitViewport.x(0.5f);
            centerY = safeCenterY();
            top = centerY + height * 0.5f;

            primaryH = baseRow * 1.02f;
            float gap = baseRow * 0.24f;
            float toggleH = baseRow * 0.88f;
            resetH = baseRow * 0.84f;
            float footerGap = baseRow * 0.18f;

            float cursor = top - pad;
            primaryCy = cursor - primaryH * 0.5f;
            float segPad = Math.max(4f, primaryH * 0.10f);
            float segGap = Math.max(6f, innerW * 0.018f);
            primaryTabW = (innerW - segPad * 2f - segGap) * 0.5f;
            primaryTabH = primaryH - segPad * 2f;
            gameTabCx = centerX - (primaryTabW + segGap) * 0.5f;
            cursor -= primaryH + gap;

            float bottomLimit = top - height + pad;
            resetW = innerW;
            resetCx = centerX;
            resetCy = bottomLimit + resetH * 0.5f;

            float toggleCy = bottomLimit + resetH + footerGap + toggleH * 0.5f;
            if (Settings.isConsoleBuild) {
                ctrlW = innerW;
                ctrlH = toggleH;
                ctrlCx = centerX;
                ctrlCy = toggleCy;
                touchW = 1f;
                touchH = 1f;
                touchCx = -10000f;
                touchCy = -10000f;
            } else {
                float tGap = innerW * 0.024f;
                ctrlW = (innerW - tGap) * 0.5f;
                ctrlH = toggleH;
                ctrlCx = centerX - (ctrlW + tGap) * 0.5f;
                ctrlCy = toggleCy;
                touchW = ctrlW;
                touchH = toggleH;
                touchCx = centerX + (touchW + tGap) * 0.5f;
                touchCy = toggleCy;
            }

            tableLeft = centerX - innerW * 0.5f;
            tableBottom = toggleCy + toggleH * 0.5f + gap;
            tableW = innerW;
            tableH = Math.max(baseRow * 3f, cursor - tableBottom);

            headerH = Math.min(baseRow * 0.74f, tableH * 0.16f);
            itemH = Math.min(baseRow * 0.84f, Math.max(baseRow * 0.66f, (tableH - headerH) / 8.5f));
            listTop = tableBottom + tableH - headerH;
            listBottom = tableBottom;
            listH = Math.max(1f, listTop - listBottom);

            scrollTrackW = Math.max(7f, tableW * 0.015f);
            scrollTrackX = tableLeft + tableW - scrollTrackW - Math.max(6f, tableW * 0.014f);

            ArrayList<RemapInputElement> list = elements();
            int dataCount = Math.max(0, (list == null ? 0 : list.size()) - 1);
            maxScroll = Math.max(0f, dataCount * itemH - listH);
            scrollOffset = MathUtils.clamp(scrollOffset, 0f, maxScroll);
        }

        private boolean inRect(float cx, float cy, float w, float h) {
            return Math.abs(InputHelper.mX - cx) < w * 0.5f && Math.abs(InputHelper.mY - cy) < h * 0.5f;
        }

        void render(SpriteBatch sb) {
            layout();
            float packed = sb.getPackedColor();
            try {
                PortraitDrawing.rectangle(sb, 0f, 0f, Settings.WIDTH, Settings.HEIGHT, SCRIM);
                drawPanel(sb, centerX, top, width, height, false);
                drawPrimaryTabs(sb, centerX, primaryCy, innerW, primaryH, false,
                        inRect(gameTabCx, primaryCy, primaryTabW, primaryTabH), false);

                GiantToggleButton ctrlToggle = ReflectionHacks.getPrivate(nativeScreen, InputSettingsScreen.class,
                        "controllerEnabledToggleButton");
                GiantToggleButton touchToggle = ReflectionHacks.getPrivate(nativeScreen, InputSettingsScreen.class,
                        "touchscreenModeButton");
                ArrayList<RemapInputElement> list = elements();
                boolean showPad = showControllerColumn();

                // Table container
                float tblBorder = Math.max(1.5f, tableW * 0.0028f);
                drawChamferedBox(sb, tableLeft, tableBottom, tableW, tableH, tableW * 0.022f,
                        tblBorder, BORDER_SUBTLE, WELL_BG);

                float contentLeft = tableLeft + tblBorder;
                float contentRight = (maxScroll > 1f ? scrollTrackX - 6f : tableLeft + tableW - tblBorder);
                float contentW = Math.max(40f, contentRight - contentLeft);
                float col0X = contentLeft + contentW * 0.035f;
                float col0W = contentW * (showPad ? 0.42f : 0.56f);
                float col1Cx = contentLeft + contentW * (showPad ? 0.58f : 0.78f);
                float col1W = contentW * (showPad ? 0.24f : 0.34f);
                float col2Cx = contentLeft + contentW * 0.85f;
                float col2W = contentW * 0.24f;

                // Sticky Table Header
                PortraitDrawing.rectangle(sb, contentLeft, listTop, tableW - tblBorder * 2f, headerH - tblBorder, TABLE_HEADER_BG);
                drawDivider(sb, centerX, listTop, tableW - tblBorder * 2f, DIVIDER_GOLD);

                String hAction = InputSettingsScreen.TEXT[3];
                String hKey = InputSettingsScreen.TEXT[4];
                String hPad = InputSettingsScreen.TEXT[5];
                float hCap = Math.min(headerH * 0.40f, tableW * 0.031f);
                drawFittedText(sb, PortraitFonts.header(), hAction, col0X, listTop + headerH * 0.5f, col0W, hCap, false, TEXT_GOLD);
                drawFittedText(sb, PortraitFonts.header(), hKey, col1Cx, listTop + headerH * 0.5f, col1W, hCap, true, TEXT_GOLD);
                if (showPad) {
                    drawFittedText(sb, PortraitFonts.header(), hPad, col2Cx, listTop + headerH * 0.5f, col2W, hCap, true, TEXT_GOLD);
                }

                // Scissor-clipped keybinding rows
                listClip.set(contentLeft, listBottom + tblBorder, contentW, Math.max(1f, listH - tblBorder));
                PortraitDesktopViewport.clip(listClip);
                sb.flush();
                boolean clipped = ScissorStack.pushScissors(listClip);
                try {
                    if (list != null) {
                        for (int i = 1; i < list.size(); i++) {
                            int idx = i - 1;
                            float ry = listTop - (idx + 1) * itemH + scrollOffset;
                            if (ry + itemH < listBottom || ry > listTop) continue;

                            RemapInputElement el = list.get(i);
                            boolean focused = el.hasInputFocus;
                            boolean hovered = !listMoved && InputHelper.mX >= contentLeft && InputHelper.mX <= contentRight
                                    && InputHelper.mY >= Math.max(listBottom, ry)
                                    && InputHelper.mY <= Math.min(listTop, ry + itemH);
                            Color fill = focused ? ACTIVE : (hovered ? ROW_HOVER : (idx % 2 == 0 ? ROW : ROW_ALT));
                            PortraitDrawing.rectangle(sb, contentLeft, ry, contentW, itemH - 1.5f, fill);
                            if (focused) {
                                PortraitDrawing.rectangle(sb, contentLeft, ry, Math.max(4f, contentW * 0.01f),
                                        itemH - 1.5f, GOLD_BAR);
                            }

                            String labelText = ReflectionHacks.getPrivate(el, RemapInputElement.class, "labelText");
                            String keyText = remapKeyText.invoke(el);
                            float cy = ry + itemH * 0.5f;
                            float rowCap = Math.min(itemH * 0.36f, tableW * 0.032f);
                            drawFittedText(sb, PortraitFonts.body(), labelText, col0X, cy, col0W, rowCap,
                                    false, focused ? TEXT_GOLD : TEXT_CREAM);

                            if (keyText != null && !keyText.trim().isEmpty()) {
                                float capW = Math.min(col1W * 0.92f, Math.max(col1W * 0.56f, itemH * 2.2f));
                                float capH = itemH * 0.64f;
                                drawChamferedBox(sb, col1Cx - capW * 0.5f, cy - capH * 0.5f, capW, capH,
                                        capH * 0.22f, 1.2f, focused ? BORDER_GOLD_BRIGHT : KEYCAP_BORDER,
                                        focused ? ACTIVE_HOVER : KEYCAP_BG);
                                drawFittedText(sb, PortraitFonts.body(), keyText, col1Cx, cy,
                                        capW * 0.86f, rowCap * 0.92f, true, TEXT_GOLD);
                            }

                            if (showPad) {
                                Texture padImg = null;
                                try {
                                    padImg = remapControllerImg.invoke(el);
                                } catch (RuntimeException ignored) {
                                }
                                if (padImg != null) {
                                    float iconSide = itemH * 0.62f;
                                    sb.setColor(Color.WHITE);
                                    sb.draw(padImg, col2Cx - iconSide * 0.5f, cy - iconSide * 0.5f, iconSide, iconSide);
                                } else {
                                    String padText = null;
                                    try {
                                        padText = remapControllerText.invoke(el);
                                    } catch (RuntimeException ignored) {
                                    }
                                    if (padText != null && !padText.trim().isEmpty()) {
                                        drawFittedText(sb, PortraitFonts.body(), padText, col2Cx, cy,
                                                col2W * 0.90f, rowCap * 0.90f, true, BADGE_TEXT);
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    if (clipped) {
                        sb.flush();
                        ScissorStack.popScissors();
                    }
                }

                // Integrated slim gold scrollbar inside table right edge
                if (maxScroll > 1f) {
                    float trackPad = 6f;
                    float trackH = Math.max(20f, listH - trackPad * 2f);
                    PortraitDrawing.rectangle(sb, scrollTrackX, listBottom + trackPad, scrollTrackW, trackH, SCROLL_TRACK);
                    float totalContent = Math.max(listH, listH + maxScroll);
                    float thumbH = Math.max(28f, trackH * MathUtils.clamp(listH / totalContent, 0.14f, 1f));
                    float pct = scrollOffset / maxScroll;
                    float thumbY = (listTop - trackPad - thumbH) - pct * (trackH - thumbH);
                    drawChamferedBox(sb, scrollTrackX, thumbY, scrollTrackW, thumbH,
                            scrollTrackW * 0.46f, 0f, null, barDragging ? BORDER_GOLD_BRIGHT : BORDER_GOLD);
                }

                // Footer toggle cards
                if (ctrlToggle != null) {
                    drawFooterToggleCard(sb, ctrlToggle, ctrlCx, ctrlCy, ctrlW, ctrlH,
                            inRect(ctrlCx, ctrlCy, ctrlW, ctrlH));
                }
                if (!Settings.isConsoleBuild && touchToggle != null) {
                    drawFooterToggleCard(sb, touchToggle, touchCx, touchCy, touchW, touchH,
                            inRect(touchCx, touchCy, touchW, touchH));
                }

                // Full-width Reset to Default button
                boolean resetHover = inRect(resetCx, resetCy, resetW, resetH);
                drawChamferedBox(sb, resetCx - resetW * 0.5f, resetCy - resetH * 0.5f, resetW, resetH,
                        resetH * 0.22f, Math.max(1.5f, resetH * 0.03f),
                        resetHover ? BORDER_GOLD : BORDER_SUBTLE,
                        resetHover ? ROW_HOVER : ROW);
                drawFittedText(sb, PortraitFonts.header(), InputSettingsScreen.TEXT[6], resetCx, resetCy,
                        resetW * 0.86f, resetH * 0.38f, true, resetHover ? TEXT_GOLD : TEXT_CREAM);

                nativeScreen.button.render(sb);
            } finally {
                sb.setColor(packed);
            }
        }

        private void drawFooterToggleCard(SpriteBatch sb, GiantToggleButton toggle,
                float cx, float cy, float w, float h, boolean hovered) {
            float left = cx - w * 0.5f;
            float bottom = cy - h * 0.5f;
            drawChamferedBox(sb, left, bottom, w, h, h * 0.20f, Math.max(1.4f, h * 0.025f),
                    hovered ? BORDER_GOLD : BORDER_SUBTLE, hovered ? ROW_HOVER : ROW);
            float padX = w * 0.06f;
            float swH = Math.min(h * 0.46f, w * 0.145f);
            float swW = swH * 1.76f;
            float swCx = left + w - padX - swW * 0.5f;
            String label = ReflectionHacks.getPrivate(toggle, GiantToggleButton.class, "label");
            drawFittedText(sb, PortraitFonts.body(), label, left + padX, cy,
                    (swCx - swW * 0.5f) - (left + padX) - padX * 0.4f,
                    Math.min(h * 0.32f, w * 0.088f), false, TEXT_CREAM);
            drawToggleSwitch(sb, swCx, cy, swW, swH, toggle.ticked);
        }

        void update() {
            layout();

            nativeScreen.button.update();
            if (nativeScreen.button.hb.clicked || InputHelper.pressedEscape || CInputActionSet.cancel.isJustPressed()) {
                if (CardCrawlGame.isInARun()) {
                    AbstractDungeon.closeCurrentScreen();
                } else {
                    InputHelper.pressedEscape = false;
                    CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
                }
                nativeScreen.hide();
                return;
            }

            // Switch back to Game Settings tab
            Hitbox gameSettingsHb = ReflectionHacks.getPrivate(nativeScreen, InputSettingsScreen.class, "gameSettingsHb");
            boolean inGameTab = inRect(gameTabCx, primaryCy, primaryTabW, primaryTabH);
            if (gameSettingsHb != null) {
                triggerHitboxUpdate(gameSettingsHb, inGameTab, () -> gameSettingsHb.encapsulatedUpdate(nativeScreen));
            }

            // Reset to default button
            Hitbox resetHb = ReflectionHacks.getPrivate(nativeScreen, InputSettingsScreen.class, "resetButtonHb");
            boolean inReset = inRect(resetCx, resetCy, resetW, resetH);
            if (resetHb != null) {
                triggerHitboxUpdate(resetHb, inReset, () -> resetHb.encapsulatedUpdate(nativeScreen));
            }

            // Controller & Touchscreen toggles
            GiantToggleButton ctrlToggle = ReflectionHacks.getPrivate(nativeScreen, InputSettingsScreen.class,
                    "controllerEnabledToggleButton");
            if (ctrlToggle != null) {
                Hitbox hb = ReflectionHacks.getPrivate(ctrlToggle, GiantToggleButton.class, "hb");
                triggerHitboxUpdate(hb, inRect(ctrlCx, ctrlCy, ctrlW, ctrlH), ctrlToggle::update);
            }
            if (!Settings.isConsoleBuild) {
                GiantToggleButton touchToggle = ReflectionHacks.getPrivate(nativeScreen, InputSettingsScreen.class,
                        "touchscreenModeButton");
                if (touchToggle != null) {
                    Hitbox hb = ReflectionHacks.getPrivate(touchToggle, GiantToggleButton.class, "hb");
                    triggerHitboxUpdate(hb, inRect(touchCx, touchCy, touchW, touchH), touchToggle::update);
                }
            }

            // Scrolling (wheel + scrollbar drag + touch list drag)
            if (InputHelper.scrolledDown) scrollOffset += itemH * 1.5f;
            if (InputHelper.scrolledUp) scrollOffset -= itemH * 1.5f;

            boolean inBar = maxScroll > 1f
                    && InputHelper.mX >= scrollTrackX - 18f && InputHelper.mX <= tableLeft + tableW
                    && InputHelper.mY >= listBottom && InputHelper.mY <= listTop;
            boolean inList = !inBar
                    && InputHelper.mX >= tableLeft && InputHelper.mX <= tableLeft + tableW
                    && InputHelper.mY >= listBottom && InputHelper.mY <= listTop;

            if (InputHelper.justClickedLeft) {
                if (inBar) {
                    barDragging = true;
                } else if (inList) {
                    listDragging = true;
                    listMoved = false;
                    pressY = InputHelper.mY;
                    lastY = InputHelper.mY;
                }
            }
            if (barDragging) {
                if (InputHelper.isMouseDown && maxScroll > 1f) {
                    float pct = MathUtils.clamp((listTop - InputHelper.mY) / Math.max(1f, listH), 0f, 1f);
                    scrollOffset = pct * maxScroll;
                } else {
                    barDragging = false;
                }
            } else if (listDragging) {
                if (InputHelper.isMouseDown) {
                    float dy = InputHelper.mY - lastY;
                    if (Math.abs(InputHelper.mY - pressY) > Math.max(10f, itemH * 0.24f)) {
                        listMoved = true;
                    }
                    if (listMoved) {
                        scrollOffset += dy;
                    }
                    lastY = InputHelper.mY;
                }
            }
            scrollOffset = MathUtils.clamp(scrollOffset, 0f, maxScroll);

            // Keybinding rows update (tap to remap; suppress click when user scrolled by dragging)
            ArrayList<RemapInputElement> list = elements();
            if (list != null) {
                for (int i = 1; i < list.size(); i++) {
                    int idx = i - 1;
                    float ry = listTop - (idx + 1) * itemH + scrollOffset;
                    boolean visible = ry + itemH > listBottom && ry < listTop;
                    RemapInputElement el = list.get(i);
                    el.isHidden = !visible && !el.hasInputFocus;
                    boolean hovered = visible && inList && !listMoved && !barDragging
                            && InputHelper.mY >= Math.max(listBottom, ry)
                            && InputHelper.mY <= Math.min(listTop, ry + itemH);
                    if (el.hasInputFocus || hovered || el.hb.clickStarted) {
                        triggerHitboxUpdate(el.hb, hovered, el::update);
                    }
                }
            }

            if (InputHelper.justReleasedClickLeft) {
                listDragging = false;
                listMoved = false;
                barDragging = false;
            }
        }
    }
}
