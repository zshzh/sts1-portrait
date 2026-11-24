package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.CharSelectInfo;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.custom.CustomModeScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.ui.buttons.ConfirmButton;
import com.megacrit.cardcrawl.ui.panels.SeedPanel;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class PortraitCharacterSelect {
    private static final Matrix4 original = new Matrix4();
    private static final Matrix4 moved = new Matrix4();
    private static final GlyphLayout measure = new GlyphLayout();
    private static final float[] quadVertices = new float[20];
    private static final float[] octagonVertices = new float[60];

    private static final Widget INFO = new Widget(.50f, .366f, .88f, .160f);
    private static final Widget SEED_SPLIT = new Widget(.280f, .166f, .42f, .041f);
    private static final Widget SEED_CENTER = new Widget(.500f, .166f, .52f, .041f);
    private static final Widget ASC = new Widget(.720f, .166f, .42f, .041f);
    private static final Widget DOWN = new Widget(.575f, .115f, .092f, .038f);
    private static final Widget UP = new Widget(.885f, .115f, .092f, .038f);
    private static final Widget BACK = new Widget(.235f, .057f, .35f, .050f);
    private static final Widget START = new Widget(.675f, .057f, .49f, .050f);

    private static final PortraitRichText description = new PortraitRichText();
    private static final PortraitRichText relicText = new PortraitRichText(true);
    private static final PortraitRichText ascensionText = new PortraitRichText();

    private static final Color DIM = new Color(0f, 0f, 0f, .68f);
    private static final Color GOLD_BORDER = new Color(.84f, .68f, .38f, .95f);
    private static final Color SUBTLE_BORDER = new Color(.52f, .60f, .65f, .55f);
    private static final Color BUTTON_SURFACE = new Color(.10f, .15f, .19f, .86f);
    private static final Color BUTTON_HOVER = new Color(.18f, .27f, .33f, .94f);
    private static final Color STAT_PILL = new Color(.06f, .09f, .12f, .66f);

    private static final Theme THEME_IRONCLAD = new Theme(.095f, .045f, .048f, .88f, .45f, .24f, .38f, .15f, .12f, .70f);
    private static final Theme THEME_SILENT = new Theme(.045f, .090f, .068f, .45f, .78f, .44f, .14f, .32f, .22f, .79f);
    private static final Theme THEME_DEFECT = new Theme(.045f, .072f, .125f, .38f, .72f, .94f, .13f, .26f, .40f, .72f);
    private static final Theme THEME_WATCHER = new Theme(.088f, .048f, .122f, .78f, .52f, .90f, .29f, .16f, .38f, .52f);
    private static final Theme THEME_DEFAULT = new Theme(.065f, .085f, .112f, .82f, .67f, .38f, .19f, .31f, .37f, .69f);

    private static Theme activeTheme = THEME_DEFAULT;
    private static float iconScale = 1f;

    private PortraitCharacterSelect() {}

    private static final class Theme {
        final float baseR, baseG, baseB;
        final float accentR, accentG, accentB;
        final float ctaR, ctaG, ctaB;
        final float focalX;

        Theme(float br, float bg, float bb, float ar, float ag, float ab,
                float cr, float cg, float cb, float focalX) {
            this.baseR = br; this.baseG = bg; this.baseB = bb;
            this.accentR = ar; this.accentG = ag; this.accentB = ab;
            this.ctaR = cr; this.ctaG = cg; this.ctaB = cb;
            this.focalX = focalX;
        }
    }

    private static Theme themeForTexture(Texture picture) {
        if (picture == ImageMaster.CHAR_SELECT_BG_IRONCLAD) return THEME_IRONCLAD;
        if (picture == ImageMaster.CHAR_SELECT_BG_SILENT) return THEME_SILENT;
        if (picture == ImageMaster.CHAR_SELECT_BG_DEFECT) return THEME_DEFECT;
        if (picture == ImageMaster.CHAR_SELECT_BG_WATCHER) return THEME_WATCHER;
        return THEME_DEFAULT;
    }

    private static Theme themeForPlayer(AbstractPlayer player) {
        if (player == null || player.chosenClass == null) return activeTheme;
        switch (player.chosenClass) {
            case IRONCLAD: return THEME_IRONCLAD;
            case THE_SILENT: return THEME_SILENT;
            case DEFECT: return THEME_DEFECT;
            case WATCHER: return THEME_WATCHER;
            default: return THEME_DEFAULT;
        }
    }

    private static final class Widget {
        final float cx, cy, w, h;
        float x, y, width, height;

        Widget(float cx, float cy, float w, float h) {
            this.cx = cx;
            this.cy = cy;
            this.w = w;
            this.h = h;
        }

        void place() {
            x = PortraitViewport.x(cx);
            y = PortraitViewport.y(cy);
            width = PortraitViewport.contentWidth() * w;
            height = PortraitViewport.contentHeight() * h;
        }

        void setHitbox(Hitbox target) {
            if (target == null) return;
            place();
            target.width = width;
            target.height = height;
            target.move(x, y);
        }

        void map(Hitbox target, int mouseX, int mouseY, boolean blocked) {
            place();
            boolean hover = !blocked && Math.abs(mouseX - x) < width * .5f && Math.abs(mouseY - y) < height * .5f;
            InputHelper.mX = hover ? Math.round(target.cX) : -10000;
            InputHelper.mY = hover ? Math.round(target.cY) : -10000;
        }

        void drawButton(SpriteBatch batch, Hitbox target, String label, boolean primary, Color textColor) {
            place();
            float left = x - width * .5f;
            float bottom = y - height * .5f;
            float cut = Math.min(height * .24f, width * .08f);
            float border = Math.max(2f, height * .028f);
            boolean hovered = target != null && target.hovered;
            Theme theme = activeTheme;

            float color = batch.getPackedColor();
            if (primary) {
                batch.setColor(hovered ? 1f : GOLD_BORDER.r, hovered ? .86f : GOLD_BORDER.g,
                        hovered ? .54f : GOLD_BORDER.b, 1f);
                drawCutCorner(batch, left, bottom, left + width, bottom + height, cut);
                float boost = hovered ? 1.28f : 1f;
                batch.setColor(Math.min(1f, theme.ctaR * boost), Math.min(1f, theme.ctaG * boost),
                        Math.min(1f, theme.ctaB * boost), .96f);
                drawCutCorner(batch, left + border, bottom + border, left + width - border,
                        bottom + height - border, Math.max(1f, cut - border * .6f));
            } else {
                batch.setColor(hovered ? theme.accentR : SUBTLE_BORDER.r,
                        hovered ? theme.accentG : SUBTLE_BORDER.g,
                        hovered ? theme.accentB : SUBTLE_BORDER.b, hovered ? .92f : SUBTLE_BORDER.a);
                drawCutCorner(batch, left, bottom, left + width, bottom + height, cut);
                Color fill = hovered ? BUTTON_HOVER : BUTTON_SURFACE;
                batch.setColor(fill);
                drawCutCorner(batch, left + border, bottom + border, left + width - border,
                        bottom + height - border, Math.max(1f, cut - border * .6f));
            }
            drawLabel(batch, label, x, y, width * .82f, height * .34f, true,
                    textColor == null ? Settings.CREAM_COLOR : textColor, true);
            batch.setColor(color);
        }
    }

    private static Widget seedWidget(CharacterSelectScreen screen) {
        boolean unlocked = ReflectionHacks.getPrivate(screen, CharacterSelectScreen.class, "isAscensionModeUnlocked");
        return unlocked ? SEED_SPLIT : SEED_CENTER;
    }

    private static Hitbox field(CharacterSelectScreen screen, String key) {
        return ReflectionHacks.getPrivate(screen, CharacterSelectScreen.class, key);
    }

    private static boolean seedOpen(CharacterSelectScreen screen) {
        return seedPanel(screen).shown;
    }

    private static SeedPanel seedPanel(CharacterSelectScreen screen) {
        return ReflectionHacks.getPrivate(screen, CharacterSelectScreen.class, "seedPanel");
    }

    private static boolean usesPortraitSeed(SeedPanel panel) {
        if (!PortraitScreens.enabled() || CardCrawlGame.mainMenuScreen == null) return false;
        CharacterSelectScreen character = CardCrawlGame.mainMenuScreen.charSelectScreen;
        if (character != null && seedPanel(character) == panel) return true;
        CustomModeScreen custom = CardCrawlGame.mainMenuScreen.customModeScreen;
        return custom != null && ReflectionHacks.<SeedPanel>getPrivate(custom, CustomModeScreen.class, "seedPanel") == panel;
    }

    private static float seedDialogWidth() { return Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth()) * .79f; }
    private static float seedDialogHeight() { return seedDialogWidth() * .65f; }

    @SpirePatch(clz = SeedPanel.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class SeedDialogDrawing {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(SeedPanel __instance, SpriteBatch sb) {
            if (!usesPortraitSeed(__instance)) return SpireReturn.Continue();
            if (!__instance.shown) return SpireReturn.Return();
            float x = PortraitViewport.x(.5f);
            float y = PortraitViewport.y(.50f);
            float w = seedDialogWidth();
            float h = seedDialogHeight();
            float color = sb.getPackedColor();
            PortraitDrawing.rectangle(sb, 0, 0, Settings.WIDTH, Settings.HEIGHT, DIM);
            try {
                PortraitSettings.frame(sb, x, y + h * .5f, w, h);
                PortraitSettings.label(sb, SeedPanel.TEXT[1], x, y + h * .345f, w * .88f, h * .095f, true);
                PortraitDrawing.rectangle(sb, x - w * .43f, y + h * .04f, w * .86f, h * .18f, PortraitSettings.ROW);
                PortraitSettings.label(sb, SeedPanel.textField, x, y + h * .13f, w * .79f, h * .074f, true);
                seedChoice(sb, __instance.yesHb, SeedPanel.TEXT[2], x - w * .225f, y - h * .12f, w, h);
                seedChoice(sb, __instance.noHb, SeedPanel.TEXT[3], x + w * .225f, y - h * .12f, w, h);
                PortraitSettings.wrappedLabel(sb, SeedPanel.TEXT[Settings.usesTrophies ? 5 : 4], x,
                        y - h * .365f, w * .86f, h * .054f, h * .19f);
            } finally {
                sb.setColor(color);
            }
            return SpireReturn.Return();
        }
    }

    private static void seedChoice(SpriteBatch batch, Hitbox area, String text, float x, float y, float w, float h) {
        PortraitDrawing.rectangle(batch, x - w * .20f, y - h * .10f, w * .40f, h * .20f,
                area.hovered ? PortraitSettings.ACTIVE : PortraitSettings.ROW);
        PortraitSettings.label(batch, text, x, y, w * .35f, h * .077f, true);
    }

    @SpirePatch(clz = SeedPanel.class, method = "update", paramtypez = {})
    public static class SeedDialogInput {
        private static boolean mapped;
        private static int rawX, rawY;

        @SpirePrefixPatch
        public static void before(SeedPanel __instance, @ByRef float[] ___animTimer) {
            mapped = usesPortraitSeed(__instance) && __instance.shown;
            if (!mapped) return;
            ___animTimer[0] = 0f;
            rawX = InputHelper.mX;
            rawY = InputHelper.mY;
            float x = PortraitViewport.x(.5f);
            float y = PortraitViewport.y(.50f);
            float w = seedDialogWidth();
            float h = seedDialogHeight();
            float distance = ((rawX) - x) / w;
            Hitbox target = null;
            if (Math.abs(((rawY) - y) / h + .12f) < .10f
                    && Math.abs(Math.abs(distance) - .225f) < .20f) {
                target = distance < 0 ? __instance.yesHb : __instance.noHb;
            }
            InputHelper.mX = target == null ? -10000 : Math.round(target.cX);
            InputHelper.mY = target == null ? -10000 : Math.round(target.cY);
        }

        @SpirePostfixPatch
        public static void after(SeedPanel __instance) {
            if (!mapped) return;
            InputHelper.mX = rawX;
            InputHelper.mY = rawY;
        }
    }

    private static void placeIcons(CharacterSelectScreen screen) {
        if (!PortraitScreens.enabled() || screen.options.isEmpty()) return;
        float span = Math.min(.84f, .205f * screen.options.size());
        float start = .5f - span * .5f;
        float step = span / screen.options.size();
        iconScale = Math.min(1.26f, PortraitViewport.contentWidth() * step / (195f * Settings.scale));
        float size = Math.min(PortraitViewport.contentWidth() * step * .86f, 152f * Settings.scale * iconScale);
        for (int i = 0; i < screen.options.size(); i++) {
            CharacterOption option = screen.options.get(i);
            option.hb.width = option.hb.height = size;
            option.hb.move(PortraitViewport.x(start + (i + .5f) * step), PortraitViewport.y(.236f));
        }
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "update")
    @SpirePatch(clz = CharacterSelectScreen.class, method = "render")
    public static class RefreshIcons {
        @SpirePrefixPatch
        public static void before(CharacterSelectScreen __instance) { placeIcons(__instance); }
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "updateAscensionToggle")
    public static class FooterPositions {
        @SpirePrefixPatch
        public static void before(CharacterSelectScreen __instance) {
            if (!PortraitScreens.enabled()) return;
            seedWidget(__instance).setHitbox(field(__instance, "seedHb"));
            boolean unlocked = ReflectionHacks.getPrivate(__instance, CharacterSelectScreen.class, "isAscensionModeUnlocked");
            if (unlocked) ASC.setHitbox(field(__instance, "ascensionModeHb"));
            else placeArrow(ASC, field(__instance, "ascensionModeHb"), false);
            placeArrow(DOWN, field(__instance, "ascLeftHb"), unlocked && __instance.isAscensionMode);
            placeArrow(UP, field(__instance, "ascRightHb"), unlocked && __instance.isAscensionMode);
        }
    }

    private static void placeArrow(Widget arrow, Hitbox area, boolean visible) {
        if (area == null) return;
        if (visible) arrow.setHitbox(area);
        else area.move(-10000f, -10000f);
    }

    private static void drawVerticalGradient(SpriteBatch batch, float y0, float y1, float bottomBits, float topBits) {
        float w = Settings.WIDTH;
        setQuad(0, 0f, y0, bottomBits);
        setQuad(1, 0f, y1, topBits);
        setQuad(2, w, y1, topBits);
        setQuad(3, w, y0, bottomBits);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, quadVertices, 0, quadVertices.length);
    }

    private static void setQuad(int index, float x, float y, float colorBits) {
        int o = index * 5;
        quadVertices[o] = x;
        quadVertices[o + 1] = y;
        quadVertices[o + 2] = colorBits;
        quadVertices[o + 3] = .5f;
        quadVertices[o + 4] = .5f;
    }

    private static void drawCutCorner(SpriteBatch batch, float left, float bottom, float right, float top, float cut) {
        float color = batch.getPackedColor();
        setOct(0, left + cut, bottom, color);
        setOct(1, left, bottom + cut, color);
        setOct(2, right, bottom + cut, color);
        setOct(3, right - cut, bottom, color);
        setOct(4, left, bottom + cut, color);
        setOct(5, left, top - cut, color);
        setOct(6, right, top - cut, color);
        setOct(7, right, bottom + cut, color);
        setOct(8, left, top - cut, color);
        setOct(9, left + cut, top, color);
        setOct(10, right - cut, top, color);
        setOct(11, right, top - cut, color);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, octagonVertices, 0, octagonVertices.length);
    }

    private static void setOct(int index, float x, float y, float colorBits) {
        int o = index * 5;
        octagonVertices[o] = x;
        octagonVertices[o + 1] = y;
        octagonVertices[o + 2] = colorBits;
        octagonVertices[o + 3] = .5f;
        octagonVertices[o + 4] = .5f;
    }

    private static void drawLabel(SpriteBatch batch, String text, float x, float y, float maxWidth,
            float targetCapHeight, boolean centered, Color color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        BitmapFont font = FontHelper.buttonLabelFont;
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        measure.setText(font, text);
        float fit = Math.min(maxWidth / Math.max(1f, measure.width),
                targetCapHeight / Math.max(1f, font.getCapHeight()));
        font.getData().setScale(oldX * fit, oldY * fit);
        try {
            float cap = font.getCapHeight();
            float sh = Math.max(1.5f, cap * .07f);
            if (centered) {
                if (shadow) FontHelper.renderFontCentered(batch, font, text, x + sh, y - sh, new Color(0f, 0f, 0f, .65f));
                FontHelper.renderFontCentered(batch, font, text, x, y, color);
            } else {
                if (shadow) FontHelper.renderFontLeftTopAligned(batch, font, text, x + sh, y + cap * .5f - sh,
                        new Color(0f, 0f, 0f, .65f));
                FontHelper.renderFontLeftTopAligned(batch, font, text, x, y + cap * .5f, color);
            }
        } finally {
            font.getData().setScale(oldX, oldY);
        }
    }

    public static void drawArtwork(SpriteBatch batch, Texture picture) {
        if (picture == null) return;
        Theme theme = themeForTexture(picture);
        activeTheme = theme;
        float W = Settings.WIDTH;
        float H = Settings.HEIGHT;
        float packed = batch.getPackedColor();

        // 1. Deep character-themed full-screen atmospheric backdrop (zero dead black bars).
        float deepBottom = Color.toFloatBits(theme.baseR * .62f, theme.baseG * .62f, theme.baseB * .68f, 1f);
        float midBase = Color.toFloatBits(theme.baseR, theme.baseG, theme.baseB, 1f);
        drawVerticalGradient(batch, 0f, H * .56f, deepBottom, midBase);
        drawVerticalGradient(batch, H * .56f, H, midBase, deepBottom);

        // 2. Full-width hero illustration spanning the upper 67% of the screen,
        //    preserving generous horizontal composition (~840px of 1920px) so face/shoulders/aura shine.
        float artBottom = H * .33f;
        float artHeight = H - artBottom;
        float sourceScale = Math.max(W / (float) picture.getWidth(), artHeight / (float) picture.getHeight());
        int srcW = Math.min(picture.getWidth(), Math.round(W / sourceScale));
        int srcH = Math.min(picture.getHeight(), Math.round(artHeight / sourceScale));
        int left = Math.min(picture.getWidth() - srcW,
                Math.max(0, Math.round(picture.getWidth() * theme.focalX - srcW * .5f)));
        int top = Math.max(0, Math.min(picture.getHeight() - srcH, (picture.getHeight() - srcH) / 5));
        batch.setColor(Color.WHITE);
        batch.draw(picture, 0f, artBottom, W, artHeight, left, top, srcW, srcH, false, false);

        // 3. Tall, silky two-stage feather gradient melting the painting seamlessly into the lower themed canvas.
        float opaqueBase = Color.toFloatBits(theme.baseR, theme.baseG, theme.baseB, 1f);
        float midFade = Color.toFloatBits(theme.baseR, theme.baseG, theme.baseB, .56f);
        float clearBase = Color.toFloatBits(theme.baseR, theme.baseG, theme.baseB, 0f);
        drawVerticalGradient(batch, artBottom - 2f, H * .47f, opaqueBase, midFade);
        drawVerticalGradient(batch, H * .47f, H * .64f, midFade, clearBase);

        // 4. Subtle top edge vignette to frame the status bar area cleanly.
        float topEdge = Color.toFloatBits(0f, 0f, 0f, .34f);
        float topClear = Color.toFloatBits(0f, 0f, 0f, 0f);
        drawVerticalGradient(batch, H * .90f, H, topClear, topEdge);

        batch.setColor(packed);
    }

    public static void drawStart(CharacterSelectScreen screen, SpriteBatch batch) {
        ConfirmButton button = screen.confirmButton;
        if (ReflectionHacks.<Boolean>getPrivate(button, ConfirmButton.class, "isHidden")) return;
        String label = ReflectionHacks.getPrivate(button, ConfirmButton.class, "buttonText");
        START.drawButton(batch, button.hb, label, !button.isDisabled,
                button.isDisabled ? Color.LIGHT_GRAY : Settings.CREAM_COLOR);
    }

    public static void updateStart(CharacterSelectScreen screen, ConfirmButton button) {
        if (!PortraitScreens.enabled()) {
            button.update();
            return;
        }
        int x = InputHelper.mX;
        int y = InputHelper.mY;
        START.map(button.hb, x, y, seedOpen(screen));
        try {
            button.update();
        } finally {
            InputHelper.mX = x;
            InputHelper.mY = y;
        }
    }

    static boolean isBack(Object button) {
        return CardCrawlGame.mainMenuScreen != null && CardCrawlGame.mainMenuScreen.charSelectScreen != null
                && CardCrawlGame.mainMenuScreen.charSelectScreen.cancelButton == button;
    }

    static void drawBack(MenuCancelButton button, SpriteBatch batch) {
        if (button.isHidden) return;
        BACK.drawButton(batch, button.hb, ReflectionHacks.getPrivate(button, MenuCancelButton.class, "buttonText"),
                false, Settings.CREAM_COLOR);
    }

    static void mapBack(MenuCancelButton button, int x, int y) {
        BACK.map(button.hb, x, y, button.isHidden || seedOpen(CardCrawlGame.mainMenuScreen.charSelectScreen));
    }

    public static void drawChooseTitle(SpriteBatch batch, String text) {
        drawLabel(batch, text, PortraitViewport.x(.5f), PortraitViewport.y(.36f),
                PortraitViewport.contentWidth() * .82f, PortraitViewport.contentHeight() * .026f,
                true, Settings.GOLD_COLOR, true);
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "render")
    @SpirePatch(clz = CharacterSelectScreen.class, method = "updateButtons")
    public static class SelectPresentation {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    String type = call.getClassName();
                    String method = call.getMethodName();
                    String portrait = "sts1portrait.PortraitScreens.enabled()";
                    if (SpriteBatch.class.getName().equals(type) && "draw".equals(method)
                            && "(Lcom/badlogic/gdx/graphics/Texture;FFFFFFFFFIIIIZZ)V".equals(call.getSignature())) {
                        call.replace("{ if (" + portrait + ") sts1portrait.PortraitCharacterSelect.drawArtwork($0,$1);"
                                + " else $proceed($$); }");
                    } else if (ConfirmButton.class.getName().equals(type) && "render".equals(method)) {
                        call.replace("{ if (" + portrait + ") sts1portrait.PortraitCharacterSelect.drawStart(this,$1);"
                                + " else $proceed($$); }");
                    } else if (ConfirmButton.class.getName().equals(type) && "update".equals(method)) {
                        call.replace("{ sts1portrait.PortraitCharacterSelect.updateStart(this,$0); }");
                    } else if (FontHelper.class.getName().equals(type) && "renderFontCentered".equals(method)) {
                        call.replace("{ if (" + portrait + ") sts1portrait.PortraitCharacterSelect.drawChooseTitle($1,$3);"
                                + " else $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "renderSeedSettings")
    public static class SeedControl {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(CharacterSelectScreen __instance, SpriteBatch sb, boolean ___anySelected) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            if (___anySelected) {
                String label = Settings.seedSet ? SeedHelper.getUserFacingSeedString() : CharacterSelectScreen.TEXT[13];
                seedWidget(__instance).drawButton(sb, field(__instance, "seedHb"), label, false,
                        Settings.seedSet ? Settings.GOLD_COLOR : Settings.CREAM_COLOR);
            }
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "renderAscensionMode")
    public static class AscensionControls {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(CharacterSelectScreen __instance, SpriteBatch sb, boolean ___anySelected,
                boolean ___isAscensionModeUnlocked) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            if (!___anySelected || !___isAscensionModeUnlocked) return SpireReturn.Return();
            float color = sb.getPackedColor();
            Hitbox mode = field(__instance, "ascensionModeHb");
            ASC.drawButton(sb, mode, CharacterSelectScreen.TEXT[6], false,
                    __instance.isAscensionMode ? Settings.GOLD_COLOR : Settings.CREAM_COLOR);
            PortraitSettings.checkmark(sb, ASC.x + ASC.width * .36f, ASC.y, ASC.height * .40f, __instance.isAscensionMode);
            if (__instance.isAscensionMode) {
                DOWN.drawButton(sb, field(__instance, "ascLeftHb"), "<", false, Settings.CREAM_COLOR);
                UP.drawButton(sb, field(__instance, "ascRightHb"), ">", false, Settings.CREAM_COLOR);
                drawLabel(sb, CharacterSelectScreen.TEXT[7] + __instance.ascensionLevel,
                        (DOWN.x + UP.x) * .5f, DOWN.y, (UP.x - DOWN.x) * .68f, DOWN.height * .38f,
                        true, Settings.GOLD_COLOR, true);
                float descLeft = PortraitViewport.x(.06f);
                float descWidth = PortraitViewport.contentWidth() * .44f;
                ascensionText.draw(sb, FontHelper.buttonLabelFont, __instance.ascLevelInfoString, Settings.CREAM_COLOR,
                        descLeft, DOWN.y, descWidth, DOWN.height * 1.05f, DOWN.height * .30f, Align.left);
            }
            sb.setColor(color);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = CharacterOption.class, method = "renderInfo")
    public static class CharacterInfo {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(CharacterOption __instance, SpriteBatch sb, CharSelectInfo ___charInfo,
                int ___unlocksRemaining) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            if (!__instance.selected || ___charInfo == null) return SpireReturn.Return();
            INFO.place();
            Theme theme = themeForPlayer(__instance.c);
            activeTheme = theme;
            float color = sb.getPackedColor();
            float cardW = INFO.width;
            float cardH = INFO.height;
            float cardLeft = INFO.x - cardW * .5f;
            float cardBottom = INFO.y - cardH * .5f;
            float cardTop = INFO.y + cardH * .5f;
            float padX = cardW * .048f;

            // --- Editorial Identity Header directly above the frosted card ---
            float headerY = cardTop + PortraitViewport.contentHeight() * .036f;
            drawLabel(sb, __instance.name, cardLeft + padX * .2f, headerY, cardW * .54f,
                    PortraitViewport.contentHeight() * .027f, false, Settings.CREAM_COLOR, true);

            // Thin character-themed accent bar beneath the name
            float barY = cardTop + PortraitViewport.contentHeight() * .014f;
            sb.setColor(theme.accentR, theme.accentG, theme.accentB, .92f);
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, cardLeft + padX * .2f, barY, cardW * .16f, 4f);

            // Compact right-aligned HP + Gold stats pill on the same baseline as the name
            float pillH = PortraitViewport.contentHeight() * .031f;
            float pillW = cardW * .42f;
            float pillRight = cardLeft + cardW;
            float pillLeft = pillRight - pillW;
            float pillBottom = headerY - pillH * .5f;
            sb.setColor(STAT_PILL);
            drawCutCorner(sb, pillLeft, pillBottom, pillRight, pillBottom + pillH, pillH * .22f);

            float statIcon = pillH * .54f;
            float hpIconX = pillLeft + pillW * .08f;
            float goldIconX = pillLeft + pillW * .58f;
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.TP_HP, hpIconX, headerY - statIcon * .5f, statIcon, statIcon);
            sb.draw(ImageMaster.TP_GOLD, goldIconX, headerY - statIcon * .5f, statIcon, statIcon);
            float statCap = pillH * .38f;
            drawLabel(sb, ___charInfo.hp, hpIconX + statIcon * 1.18f, headerY, pillW * .34f,
                    statCap, false, Settings.CREAM_COLOR, false);
            drawLabel(sb, Integer.toString(___charInfo.gold), goldIconX + statIcon * 1.18f, headerY, pillW * .26f,
                    statCap, false, Settings.GOLD_COLOR, false);

            if (___unlocksRemaining > 0) {
                int progress = UnlockTracker.getCurrentProgress(__instance.c.chosenClass);
                int target = UnlockTracker.getCurrentScoreCost(__instance.c.chosenClass);
                drawLabel(sb, progress + "/" + target, pillRight - pillW * .14f, barY + 4f,
                        cardW * .22f, pillH * .28f, true, new Color(.78f, .82f, .86f, .78f), true);
            }

            // --- Frosted Glass Chamfered Card (Flavor Text + Divider + Starting Relic) ---
            float cut = cardH * .075f;
            float border = Math.max(2f, cardW * .0025f);
            sb.setColor(theme.accentR, theme.accentG, theme.accentB, .52f);
            drawCutCorner(sb, cardLeft, cardBottom, cardLeft + cardW, cardTop, cut);
            sb.setColor(theme.baseR * .68f, theme.baseG * .68f, theme.baseB * .76f, .84f);
            drawCutCorner(sb, cardLeft + border, cardBottom + border,
                    cardLeft + cardW - border, cardTop - border, Math.max(1f, cut - border * .6f));

            // Uniform font size across ALL characters for flavor text & relic description
            float bodyCap = cardH * .076f;
            float innerLeft = cardLeft + padX;
            float innerW = cardW - padX * 2f;

            String cleanFlavor = ___charInfo.flavorText == null ? ""
                    : ___charInfo.flavorText.replaceAll("NL[ \\t\\u3000]+", "NL ").trim();
            description.drawTop(sb, FontHelper.buttonLabelFont, cleanFlavor, Settings.CREAM_COLOR,
                    innerLeft, cardTop - cardH * .095f, innerW, cardH * .37f, bodyCap, Align.left, __instance.c);

            // Subtle horizontal divider separating flavor text and starting relic
            float dividerY = cardBottom + cardH * .505f;
            sb.setColor(theme.accentR, theme.accentG, theme.accentB, .26f);
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, innerLeft, dividerY, innerW, 2f);

            if (___charInfo.relics != null && !___charInfo.relics.isEmpty()) {
                drawRelics(sb, __instance.c, ___charInfo, innerLeft, innerW, cardBottom, cardH, bodyCap);
            }

            sb.setColor(color);
            return SpireReturn.Return();
        }
    }

    private static void drawRelics(SpriteBatch batch, AbstractPlayer character, CharSelectInfo info,
            float left, float innerW, float cardBottom, float cardH, float bodyCap) {
        int count = info.relics.size();
        float size = Math.min(cardH * .28f, innerW * .88f / count);
        float centerY = cardBottom + cardH * .265f;
        batch.setColor(Color.WHITE);
        for (int i = 0; i < count; i++) {
            AbstractRelic relic = RelicLibrary.getRelic(info.relics.get(i));
            batch.draw(relic.img, left + i * size, centerY - size * .5f, size, size);
            if (count == 1) {
                float textX = left + size * 1.15f;
                float textW = innerW - size * 1.15f;
                drawLabel(batch, relic.name, textX, centerY + cardH * .135f, textW,
                        cardH * .080f, false, Settings.GOLD_COLOR, false);
                relicText.drawTop(batch, FontHelper.buttonLabelFont, relic.description,
                        Settings.CREAM_COLOR, textX, centerY + cardH * .045f, textW, cardH * .32f, bodyCap,
                        Align.left, character);
            }
        }
    }

    @SpirePatch(clz = CharacterOption.class, method = "renderOptionButton", paramtypez = {SpriteBatch.class})
    public static class OptionDrawing {
        @SpirePrefixPatch
        public static void before(CharacterOption __instance, SpriteBatch batch) {
            if (!PortraitScreens.enabled()) return;
            Hitbox area = __instance.hb;
            if (__instance.selected) {
                Theme theme = themeForPlayer(__instance.c);
                float prev = batch.getPackedColor();
                float barW = area.width * .62f;
                float barH = Math.max(5f, area.height * .045f);
                batch.setColor(theme.accentR, theme.accentG, theme.accentB, .95f);
                batch.draw(ImageMaster.WHITE_SQUARE_IMG, area.cX - barW * .5f, area.y - barH * 2.2f, barW, barH);
                batch.setColor(prev);
            }
            float scale = iconScale;
            original.set(batch.getTransformMatrix());
            moved.set(original).translate(area.cX * (1f - scale),
                    area.cY * (1f - scale), 0f).scale(scale, scale, 1f);
            batch.setTransformMatrix(moved);
        }

        @SpirePostfixPatch
        public static void after(CharacterOption __instance, SpriteBatch batch) {
            if (PortraitScreens.enabled()) batch.setTransformMatrix(original);
        }
    }
}
