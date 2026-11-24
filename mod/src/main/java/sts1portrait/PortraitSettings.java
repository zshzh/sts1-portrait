package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.options.AbandonRunButton;
import com.megacrit.cardcrawl.screens.options.DropdownMenu;
import com.megacrit.cardcrawl.screens.options.ExitGameButton;
import com.megacrit.cardcrawl.screens.options.OptionsPanel;
import com.megacrit.cardcrawl.screens.options.Slider;
import com.megacrit.cardcrawl.screens.options.ToggleButton;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class PortraitSettings {
    static final float TAB_OFFSET = .25f;
    static final float TAB_WIDTH = .40f;
    static final float TAB_HEIGHT = .83f;
    static final Color ROW = new Color(0.14f, 0.20f, 0.24f, 1f);
    static final Color ACTIVE = new Color(0.23f, 0.36f, 0.40f, 1f);
    private static final Color FRAME = new Color(0.09f, 0.14f, 0.18f, 0.98f);
    private static final Color EDGE = new Color(0.51f, 0.66f, 0.68f, 1f);
    static final Color DESTRUCTIVE = new Color(0.35f, 0.20f, 0.18f, 1f);
    private static final GlyphLayout glyphs = new GlyphLayout();
    private static final Matrix4 previous = new Matrix4();
    private static final Matrix4 moved = new Matrix4();
    private static final ReflectionHacks.RMethod updateEffects = ReflectionHacks.privateMethod(OptionsPanel.class, "updateEffects");
    private static final ReflectionHacks.RMethod playtesterAvailable = ReflectionHacks.privateMethod(OptionsPanel.class,
            "canTogglePlaytesterArt");
    private static final ReflectionHacks.RMethod modifyVolume = ReflectionHacks.privateMethod(Slider.class, "modifyVolume");
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

    @SpirePatch(clz = OptionsPanel.class, method = SpirePatch.CLASS)
    public static class State {
        public static final SpireField<Screen> screen = new SpireField<>(() -> null);
    }

    private static Screen screen(OptionsPanel panel) {
        Screen result = State.screen.get(panel);
        if (result == null) {
            result = new Screen(panel);
            State.screen.set(panel, result);
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

    public static final class Screen {
        private final OptionsPanel panel;

        private final List<List<Control>> sections = new ArrayList<>();
        private final Control[] tabs = new Control[4];
        private final Control input, abandon, exit;
        private int current;
        private float width, row, centerX, centerY, top, height;
        private boolean showArt;

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
            add(0, "resolution", line(3, 0), "resoDropdown");
            add(0, "frame-rate", line(3, 1), "fpsDropdown");
            add(0, "fullscreen", line(4, 0), "fsToggle");
            add(0, "borderless", line(4, 1), "wfsToggle");
            add(0, "screen-shake", line(4, 2), "ssToggle");
            add(0, "vsync", text[17].replace("NL", "").trim(), "vSyncToggle");
            add(1, "master-volume", line(6, 0), "masterSlider");
            add(1, "music-volume", line(6, 1), "bgmSlider");
            add(1, "effects-volume", line(6, 2), "sfxSlider");
            add(1, "ambient-audio", line(7, 0), "ambienceToggle");
            add(1, "mute-background", line(7, 1), "muteBgToggle");
            add(2, "damage-total", line(9, 0), "sumToggle");
            add(2, "blocked-total", line(9, 1), "blockToggle");
            add(2, "single-confirm", line(9, 2), "confirmToggle");
            add(2, "particles", text[21], "effectsToggle");
            add(2, "fast-mode", text[10], "fastToggle");
            add(2, "card-shortcuts", text[19], "cardKeyOverlayToggle");
            add(3, "language", text[13], "languageDropdown");
            add(3, "telemetry", text[14].replace("#b", ""), "uploadToggle");
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
                String value = "frame-rate".equals(key) ? Integer.toString(Settings.MAX_FPS)
                        : Settings.WIDTH + " × " + Settings.HEIGHT;
                widget = value;
            }
            sections.get(section).add(new Control(key, label, widget));
        }

        private boolean shown(Control control) { return showArt || !"playtester-art".equals(control.key); }

        private void layout() {
            width = panelWidth();
            row = rowHeight();
            centerX = PortraitViewport.x(0.5f);
            centerY = PortraitViewport.y(0.57f);
            height = row * 9.25f;
            top = centerY + height * 0.5f;
            showArt = playtesterAvailable.invoke(panel);
            input.place(centerX + width * TAB_OFFSET, top - row * .62f, width * TAB_WIDTH, row * TAB_HEIGHT);
            for (int i = 0; i < tabs.length; i++) {
                tabs[i].place(centerX + (i - 1.5f) * width * 0.231f, top - row * 1.66f,
                        width * 0.218f, row * 0.80f);
            }
            int index = 0;
            for (Control control : sections.get(current)) {
                if (shown(control)) control.place(centerX, top - row * (2.73f + index++), width * 0.91f,
                        row * 0.91f);
            }
            abandon.place(centerX - width * 0.235f, top - row * 8.62f, width * 0.45f, row * 0.89f);
            exit.place(centerX + width * 0.235f, top - row * 8.62f, width * 0.45f, row * 0.89f);
        }

        private Control openMenu() {
            for (Control control : sections.get(current)) {
                if (control.widget instanceof DropdownMenu && ((DropdownMenu) control.widget).isOpen) return control;
            }
            return null;
        }

        void render(SpriteBatch batch) {
            layout();
            float color = batch.getPackedColor();
            frame(batch, centerX, top, width, height);
            activeTab(batch, OptionsPanel.TEXT[1], centerX - width * TAB_OFFSET,
                    top - row * .62f, width * TAB_WIDTH, row * TAB_HEIGHT);
            input.draw(batch, ROW, true);
            for (int i = 0; i < tabs.length; i++) tabs[i].draw(batch, i == current ? ACTIVE : ROW, true);
            Control open = openMenu();
            for (Control control : sections.get(current)) {
                if (shown(control)) control.draw(batch, ROW, false);
            }
            if (CardCrawlGame.isInARun()) abandon.draw(batch, DESTRUCTIVE, true);
            exit.draw(batch, ACTIVE, true);
            if (open != null) open.drawDropdown(batch, (DropdownMenu) open.widget);
            for (AbstractGameEffect effect : panel.effects) effect.render(batch);
            batch.setColor(color);
        }

        void update() {
            layout();
            Control open = openMenu();
            if (open != null) {
                open.update(panel, true);
                return;
            }
            Control selected = CardCrawlGame.isInARun() && abandon.hit() ? abandon : null;
            if (exit.hit()) selected = exit;
            List<Control> rows = sections.get(current);
            for (int i = rows.size() - 1; selected == null && i >= 0; i--) {
                Control control = rows.get(i);
                if (shown(control) && control.hit()) selected = control;
            }
            for (int i = 0; i < tabs.length; i++) {
                if (selected == null && tabs[i].hit() && InputHelper.justClickedLeft) {
                    current = i;
                    InputHelper.justClickedLeft = false;
                    return;
                }
            }
            input.update(panel, selected == null);
            if (CardCrawlGame.isInARun()) abandon.update(panel, selected == abandon);
            exit.update(panel, selected == exit);
            updateEffects.invoke(panel);
            for (Control control : rows) {
                if (shown(control)) control.update(panel, selected == control);
                if (control.widget instanceof DropdownMenu && ((DropdownMenu) control.widget).isOpen) break;
            }
            if (selected != null) selected.tip();
        }
    }

    private static final class Control {
        final String key, text;
        final Object widget;

        final Hitbox button;
        float x, y, width, height;
        float nativeX, nativeY, widgetX, widgetY, widgetScale, menuWidth, menuHeight;

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
            if (widget instanceof Slider) {
                Hitbox anchor = ((Slider) widget).bgHb;
                nativeX = anchor.cX;
                nativeY = anchor.cY;
                widgetX = x + width * 0.16f;
                widgetY = y;
                widgetScale = width * 0.35f / (250f * Settings.scale);
            } else if (widget instanceof DropdownMenu) {
                DropdownMenu dropdown = (DropdownMenu) widget;
                nativeX = Settings.WIDTH * 0.5f;
                nativeY = Settings.HEIGHT * 0.70f;
                float nativeWidth = Math.max(1f, dropdown.approximateOverallWidth());
                float nativeRow = Math.max(1f, dropdown.approximateRowHeight());
                int maxRows = ReflectionHacks.getPrivate(dropdown, DropdownMenu.class, "maxRows");
                float rowCount = Math.min(dropdown.rows.size(), maxRows) + 1.7f;
                if (dropdown.isOpen) {
                    widgetScale = Math.min(width * 0.86f / nativeWidth, height * 0.85f / nativeRow);
                    widgetScale = Math.min(widgetScale, PortraitViewport.contentHeight() * 0.49f / (rowCount * nativeRow));
                } else {
                    widgetScale = Math.min(width * 0.42f / nativeWidth, height * 0.56f / nativeRow);
                }
                menuWidth = nativeWidth * widgetScale;
                menuHeight = rowCount * nativeRow * widgetScale;
                widgetX = dropdown.isOpen ? x - menuWidth * 0.5f : x + width * 0.04f;
                widgetY = dropdown.isOpen ? Math.max(y + nativeRow * widgetScale * 0.45f,
                        PortraitViewport.y(0.08f) + menuHeight) : y + nativeRow * widgetScale * 0.45f;
            }
        }

        boolean hit() { return Math.abs(InputHelper.mX - x) < width * 0.5f && Math.abs(InputHelper.mY - y) < height * 0.5f; }

        void draw(SpriteBatch batch, Color background, boolean centered) {
            PortraitDrawing.rectangle(batch, x - width * 0.5f, y - height * 0.5f, width, height, background);
            boolean hasValue = widget instanceof Slider || widget instanceof DropdownMenu || widget instanceof String;
            float labelWidth = hasValue ? width * 0.47f
                    : widget instanceof ToggleButton ? width * 0.74f : width * 0.83f;
            label(batch, text, centered ? x : x - width * 0.46f, y, labelWidth, height * 0.30f, centered);
            if (widget instanceof ToggleButton) {
                checkmark(batch, x + width * 0.38f, y, height * 0.39f, ((ToggleButton) widget).enabled);
            } else if (widget instanceof Slider) {
                beginWidget(batch);
                try {
                    ((Slider) widget).render(batch);
                } finally {
                    batch.setTransformMatrix(previous);
                }
            } else if (widget instanceof DropdownMenu && !((DropdownMenu) widget).isOpen) {
                drawDropdown(batch, (DropdownMenu) widget);
            } else if (widget instanceof String) {
                label(batch, (String) widget, x + width * 0.25f, y + height * 0.14f,
                        width * 0.40f, height * 0.26f, true);
                label(batch, PortraitDrawing.chinese() ? "启动器管理" : "Managed by launcher",
                        x + width * 0.25f, y - height * 0.21f, width * 0.40f, height * 0.20f, true);
            }
        }

        private void beginWidget(SpriteBatch batch) {
            previous.set(batch.getTransformMatrix());
            moved.set(previous).translate(widgetX - nativeX * widgetScale, widgetY - nativeY * widgetScale, 0)
                    .scale(widgetScale, widgetScale, 1);
            batch.setTransformMatrix(moved);
        }

        void drawDropdown(SpriteBatch batch, DropdownMenu dropdown) {
            if (dropdown.rows.isEmpty()) return;
            boolean open = dropdown.isOpen;
            float cx = widgetX + menuWidth * 0.5f;
            float cy = widgetY - menuHeight * 0.5f;

            beginWidget(batch);
            try {
                batch.setColor(Color.WHITE);
                dropdown.render(batch, nativeX, nativeY);
            } finally {
                batch.setTransformMatrix(previous);

            }
        }

        void update(OptionsPanel panel, boolean allowed) {
            int rawX = InputHelper.mX;
            int rawY = InputHelper.mY;
            float px = rawX;
            float py = rawY;
            try {
                if (button != null) {
                    boolean hovered = allowed && hit();
                    InputHelper.mX = hovered ? Math.round(button.cX) : -10000;
                    InputHelper.mY = hovered ? Math.round(button.cY) : -10000;
                    if (widget instanceof ToggleButton) ((ToggleButton) widget).update();
                    else if (widget instanceof AbandonRunButton) ((AbandonRunButton) widget).update();
                    else if (widget instanceof ExitGameButton) ((ExitGameButton) widget).update();
                    else button.encapsulatedUpdate(panel);
                } else if (widget instanceof Slider) {
                    Slider slider = (Slider) widget;
                    InputHelper.mX = Math.round(nativeX + (px - widgetX) / widgetScale);
                    InputHelper.mY = allowed ? Math.round(nativeY) : -10000;
                    slider.update();
                    if (allowed && slider.bgHb.hovered && InputHelper.justClickedLeft) {
                        // The native widget grabs on press and applies volume on a subsequent frame.
                        // Apply the starting value here so a short touch also works.
                        float left = 1235f * Settings.xScale;
                        float distance = 230f * Settings.xScale;
                        float volume = MathUtils.clamp((InputHelper.mX - left) / distance, 0f, 1f);
                        ReflectionHacks.setPrivate(slider, Slider.class, "x", left + volume * distance);
                        ReflectionHacks.setPrivate(slider, Slider.class, "volume", volume);
                        modifyVolume.invoke(slider);
                    }
                } else if (widget instanceof DropdownMenu) {
                    DropdownMenu dropdown = (DropdownMenu) widget;
                    if (dropdown.rows.isEmpty()) return;
                    if (dropdown.isOpen) {

                        px = (px);
                        py = (py);
                        InputHelper.mX = Math.round(nativeX + (px - widgetX) / widgetScale);
                        InputHelper.mY = Math.round(nativeY + (py - widgetY) / widgetScale);
                    } else {
                        Hitbox anchor = dropdown.getHitbox();
                        InputHelper.mX = allowed ? Math.round(anchor.cX) : -10000;
                        InputHelper.mY = allowed ? Math.round(anchor.cY) : -10000;
                    }
                    dropdown.update();
                }
            } finally {
                InputHelper.mX = rawX;
                InputHelper.mY = rawY;
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

    static float panelWidth() { return Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth()) * 0.94f; }

    static float rowHeight() {
        return Math.min(panelWidth() * 0.115f, PortraitViewport.contentHeight() * 0.082f);
    }

    static float panelTop() { return PortraitViewport.y(0.57f) + rowHeight() * 4.625f; }

    static void activeTab(SpriteBatch batch, String text, float x, float y, float width, float height) {
        PortraitDrawing.rectangle(batch, x - width * .5f, y - height * .5f, width, height, ACTIVE);
        label(batch, text, x, y, width * .83f, height * .30f, true);
    }

    static void frame(SpriteBatch batch, float center, float top, float width, float height) {
        PortraitDrawing.rectangle(batch, center - width * 0.5f, top - height, width, height, FRAME);
        float line = width * 0.003f;
        PortraitDrawing.rectangle(batch, center - width * 0.5f, top - line, width, line, EDGE);
    }

    static void checkmark(SpriteBatch batch, float x, float y, float side, boolean checked) {
        batch.setColor(Color.WHITE);
        batch.draw(ImageMaster.OPTION_TOGGLE, x - side * 0.5f, y - side * 0.5f, side, side);
        if (checked) batch.draw(ImageMaster.OPTION_TOGGLE_ON, x - side * 0.5f, y - side * 0.5f, side, side);
    }

    static void wrappedLabel(SpriteBatch batch, String text, float x, float y, float width,
            float lineHeight, float maxHeight) {
        BitmapFont font = FontHelper.buttonLabelFont;
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        float fit = lineHeight / Math.max(1f, font.getCapHeight());
        font.getData().setScale(oldX * fit, oldY * fit);
        try {
            glyphs.setText(font, text, Settings.CREAM_COLOR, width, Align.center, true);
            if (glyphs.height > maxHeight) {
                fit *= maxHeight / glyphs.height;
                font.getData().setScale(oldX * fit, oldY * fit);
                glyphs.setText(font, text, Settings.CREAM_COLOR, width, Align.center, true);
            }
            font.draw(batch, glyphs, x - width * .5f, y + glyphs.height * .5f);
        } finally {
            font.getData().setScale(oldX, oldY);
        }
    }

    static void label(SpriteBatch batch, String text, float x, float y, float width, float height,
            boolean centered) {
        BitmapFont font = FontHelper.buttonLabelFont;
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        glyphs.setText(font, text);
        float fit = Math.min(width / Math.max(1f, glyphs.width), height / Math.max(1f, font.getCapHeight()));
        font.getData().setScale(oldX * fit, oldY * fit);
        try {
            if (centered) FontHelper.renderFontCentered(batch, font, text, x, y, Settings.CREAM_COLOR);
            else FontHelper.renderFontLeftTopAligned(batch, font, text, x, y + font.getCapHeight() * 0.5f,
                    Settings.CREAM_COLOR);
        } finally {
            font.getData().setScale(oldX, oldY);
        }
    }
}
