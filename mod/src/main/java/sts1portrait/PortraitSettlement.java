package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.neow.NeowUnlockScreen;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.DeathScreen;
import com.megacrit.cardcrawl.screens.GameOverScreen;
import com.megacrit.cardcrawl.screens.GameOverStat;
import com.megacrit.cardcrawl.screens.VictoryScreen;
import com.megacrit.cardcrawl.ui.buttons.DynamicBanner;
import com.megacrit.cardcrawl.ui.buttons.ReturnToMenuButton;
import com.megacrit.cardcrawl.ui.buttons.UnlockConfirmButton;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;
import com.megacrit.cardcrawl.unlock.UnlockCharacterScreen;
import com.megacrit.cardcrawl.vfx.ConeEffect;
import com.megacrit.cardcrawl.vfx.TintEffect;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class PortraitSettlement {
    private static final Color CREAM = new Color(0.96f, 0.92f, 0.84f, 1f);
    private static final Color MUTED = new Color(0.76f, 0.82f, 0.88f, 0.94f);
    private static final Color VALUE_COLOR = new Color(1f, 0.90f, 0.56f, 1f);
    private static final Color BUTTON_TEXT = new Color(1f, 0.97f, 0.90f, 1f);
    private static final Color DEFEAT_ACCENT = new Color(0.86f, 0.42f, 0.36f, 0.86f);
    private static final Color VICTORY_ACCENT = new Color(0.90f, 0.74f, 0.40f, 0.92f);
    private static final Color DEFEAT_TITLE = new Color(0.98f, 0.82f, 0.76f, 1f);

    private static final GlyphLayout measure = new GlyphLayout();
    private static final PortraitRichText richText = new PortraitRichText(true);
    private static final Color workColor = new Color();
    private static final ArrayList<GameOverStat> visibleItems = new ArrayList<>();
    private static int selectedUnlockRelic = 0;
    private static NeowUnlockScreen trackedUnlockScreen;

    private PortraitSettlement() {}

    public static boolean gameOverActive() {
        return PortraitScreens.enabled()
                && (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DEATH
                || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.VICTORY);
    }

    public static boolean neowUnlockActive() {
        return PortraitScreens.enabled()
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NEOW_UNLOCK;
    }

    public static boolean characterUnlockActive() {
        return PortraitScreens.enabled()
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.UNLOCK;
    }

    public static boolean anyActive() {
        return gameOverActive() || neowUnlockActive() || characterUnlockActive();
    }

    static boolean ownsReturnButton(Object button) {
        return button instanceof ReturnToMenuButton && gameOverActive();
    }

    private static float layoutWidth() {
        return Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth());
    }

    private static float headerY() {
        float top = PortraitCombatHud.topBarActive()
                ? PortraitCombatHud.panelBottom()
                : PortraitViewport.y(0.92f);
        float margin = Math.max(layoutWidth() * 0.068f, PortraitViewport.contentHeight() * 0.045f);
        return Math.min(PortraitViewport.y(0.84f), top - margin);
    }

    private static float actionButtonY() {
        return PortraitViewport.y(0.095f);
    }

    private static float actionButtonW() {
        return layoutWidth() * 0.62f;
    }

    private static float actionButtonH() {
        return layoutWidth() * 0.114f;
    }

    private static boolean isDefeatStyle() {
        return AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DEATH && !GameOverScreen.isVictory;
    }

    @SpirePatch(clz = DynamicBanner.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class HideVanillaBanner {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(DynamicBanner __instance, SpriteBatch sb) {
            if (anyActive() && __instance == AbstractDungeon.dynamicBanner) {
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = GameOverScreen.class, method = "renderStatsScreen", paramtypez = {SpriteBatch.class})
    public static class StatsScreenRender {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(GameOverScreen __instance, SpriteBatch sb) {
            if (!gameOverActive()) return SpireReturn.Continue();
            renderPortraitStats(__instance, sb);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = DeathScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class DeathScreenRender {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (FontHelper.class.getName().equals(call.getClassName())
                            && "renderFontCentered".equals(call.getMethodName())) {
                        call.replace("{ if (sts1portrait.PortraitSettlement.gameOverActive()) {"
                                + " sts1portrait.PortraitSettlement.drawDeathQuote($1, $3, $6);"
                                + " } else { $proceed($$); } }");
                    }
                }
            };
        }

        @SpirePostfixPatch
        public static void after(DeathScreen __instance, SpriteBatch sb) {
            if (!gameOverActive()) return;
            boolean showingStats = ReflectionHacks.<Boolean>getPrivate(__instance, GameOverScreen.class, "showingStats");
            if (!showingStats) {
                drawBannerHeader(sb, isDefeatStyle());
            }
        }
    }

    @SpirePatch(clz = VictoryScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class VictoryScreenRender {
        @SpirePostfixPatch
        public static void after(VictoryScreen __instance, SpriteBatch sb) {
            if (!gameOverActive()) return;
            boolean showingStats = ReflectionHacks.<Boolean>getPrivate(__instance, GameOverScreen.class, "showingStats");
            if (!showingStats) {
                drawBannerHeader(sb, false);
            }
        }
    }

    @SpirePatch(clz = ReturnToMenuButton.class, method = "update", paramtypez = {})
    public static class ReturnButtonUpdate {
        @SpirePrefixPatch
        public static void before(ReturnToMenuButton __instance) {
            if (!gameOverActive()) return;
            float cx = PortraitViewport.x(0.5f);
            float cy = actionButtonY();
            __instance.x = cx;
            __instance.y = cy;
            float w = actionButtonW();
            float h = actionButtonH();
            if (__instance.hb.width != w || __instance.hb.height != h) {
                __instance.hb.resize(w, h);
            }
        }
    }

    @SpirePatch(clz = ReturnToMenuButton.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class ReturnButtonRender {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(ReturnToMenuButton __instance, SpriteBatch sb) {
            if (!gameOverActive()) return SpireReturn.Continue();
            TintEffect textTint = ReflectionHacks.getPrivate(__instance, ReturnToMenuButton.class, "textTint");
            float alpha = textTint != null && textTint.color != null ? textTint.color.a : (__instance.show ? 1f : 0f);
            if (alpha <= 0.005f || __instance.label == null || __instance.label.isEmpty()) {
                return SpireReturn.Return();
            }
            drawActionButton(sb, __instance.label, __instance.hb.hovered, __instance.hb.clickStarted,
                    MathUtils.clamp(alpha, 0f, 1f), isDefeatStyle());
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = UnlockConfirmButton.class, method = "update", paramtypez = {})
    public static class UnlockButtonUpdate {
        @SpirePrefixPatch
        public static void before(UnlockConfirmButton __instance) {
            if (!neowUnlockActive() && !characterUnlockActive()) return;
            float w = actionButtonW();
            float h = actionButtonH();
            float cx = PortraitViewport.x(0.5f);
            float cy = actionButtonY();
            if (__instance.hb.width != w || __instance.hb.height != h) {
                __instance.hb.resize(w, h);
            }
            if (__instance.hb.cX != cx || __instance.hb.cY != cy) {
                __instance.hb.move(cx, cy);
            }
        }
    }

    @SpirePatch(clz = UnlockConfirmButton.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class UnlockButtonRender {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(UnlockConfirmButton __instance, SpriteBatch sb) {
            if (!neowUnlockActive() && !characterUnlockActive()) return SpireReturn.Continue();
            Color btnColor = ReflectionHacks.getPrivate(__instance, UnlockConfirmButton.class, "btnColor");
            float alpha = btnColor != null ? btnColor.a : 1f;
            if (alpha <= 0.005f) return SpireReturn.Return();
            String label = ReflectionHacks.getPrivate(__instance, UnlockConfirmButton.class, "buttonText");
            if (label == null || label.isEmpty()) {
                label = UnlockConfirmButton.TEXT != null && UnlockConfirmButton.TEXT.length > 0
                        ? UnlockConfirmButton.TEXT[0] : "Confirm";
            }
            drawActionButton(sb, label, __instance.hb.hovered, __instance.hb.clickStarted,
                    MathUtils.clamp(alpha, 0f, 1f), false);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = NeowUnlockScreen.class, method = "open", paramtypez = {ArrayList.class, boolean.class})
    public static class NeowUnlockOpen {
        @SpirePostfixPatch
        public static void after(NeowUnlockScreen __instance, ArrayList<AbstractUnlock> unlock, boolean isVictory) {
            selectedUnlockRelic = 0;
            trackedUnlockScreen = __instance;
            if (!neowUnlockActive()) return;
            layoutNeowUnlocks(__instance, true);
        }
    }

    @SpirePatch(clz = NeowUnlockScreen.class, method = "update", paramtypez = {})
    public static class NeowUnlockUpdate {
        @SpirePrefixPatch
        public static void before(NeowUnlockScreen __instance) {
            if (!neowUnlockActive()) return;
            if (trackedUnlockScreen != __instance) {
                trackedUnlockScreen = __instance;
                selectedUnlockRelic = 0;
            }
            layoutNeowUnlocks(__instance, false);
        }
    }

    @SpirePatch(clz = NeowUnlockScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class NeowUnlockRender {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(NeowUnlockScreen __instance, SpriteBatch sb) {
            if (!neowUnlockActive()) return SpireReturn.Continue();
            renderPortraitNeowUnlock(__instance, sb);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = UnlockCharacterScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class CharacterUnlockRender {
        @SpirePostfixPatch
        public static void after(UnlockCharacterScreen __instance, SpriteBatch sb) {
            if (!characterUnlockActive()) return;
            drawBannerHeader(sb, false);
            if (__instance.unlock != null && __instance.unlock.player != null) {
                String title = __instance.unlock.player.getLocalizedCharacterName();
                if (title != null && !title.isEmpty()) {
                    float W = layoutWidth();
                    float y = actionButtonY() + actionButtonH() * 0.5f + W * 0.085f;
                    drawSubtitlePill(sb, title, y, Settings.GOLD_COLOR, W * 0.044f);
                }
            }
        }
    }

    public static void drawDeathQuote(SpriteBatch batch, String quote, Color color) {
        if (quote == null || quote.isEmpty() || color == null || color.a <= 0.005f) return;
        float W = layoutWidth();
        float cx = PortraitViewport.x(0.5f);
        float maxTextW = W * 0.80f;
        float cap = W * 0.036f;
        BitmapFont font = PortraitFonts.body();
        float textH = richText.measureHeight(font, quote, maxTextW, cap);
        float padX = W * 0.055f;
        float padY = W * 0.036f;
        float boxW = maxTextW + padX * 2f;
        float boxH = textH + padY * 2f;
        float cy = Math.max(actionButtonY() + actionButtonH() * 0.5f + boxH * 0.5f + W * 0.055f,
                PortraitViewport.y(0.27f));
        float left = cx - boxW * 0.5f;
        float bottom = cy - boxH * 0.5f;
        float cut = Math.min(boxH * 0.24f, W * 0.022f);
        float border = Math.max(2f, W * 0.0026f);
        float packed = batch.getPackedColor();
        try {
            batch.setColor(0.82f, 0.42f, 0.36f, 0.60f * color.a);
            PortraitUi.drawCutCorner(batch, left, bottom, left + boxW, bottom + boxH, cut);
            batch.setColor(0.05f, 0.065f, 0.085f, 0.84f * color.a);
            PortraitUi.drawCutCorner(batch, left + border, bottom + border,
                    left + boxW - border, bottom + boxH - border, Math.max(1f, cut - border * 0.6f));
            workColor.set(CREAM.r, CREAM.g, CREAM.b, color.a);
            richText.drawTop(batch, font, quote, workColor,
                    left + padX, bottom + boxH - padY, maxTextW, textH + cap,
                    cap, Align.center, AbstractDungeon.player);
        } finally {
            batch.setColor(packed);
        }
    }

    private static void renderPortraitStats(GameOverScreen screen, SpriteBatch batch) {
        boolean showingStats = ReflectionHacks.<Boolean>getPrivate(screen, GameOverScreen.class, "showingStats");
        if (!showingStats) return;

        float statsTimer = ReflectionHacks.<Float>getPrivate(screen, GameOverScreen.class, "statsTimer");
        Color fadeBgColor = ReflectionHacks.getPrivate(screen, GameOverScreen.class, "fadeBgColor");
        float screenAlpha = MathUtils.clamp(1f - statsTimer, 0f, 1f);
        float packed = batch.getPackedColor();
        try {
            if (fadeBgColor != null) {
                fadeBgColor.a = screenAlpha * 0.72f;
                batch.setColor(fadeBgColor);
            } else {
                batch.setColor(0.02f, 0.03f, 0.05f, screenAlpha * 0.72f);
            }
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, 0f, 0f, Settings.WIDTH, Settings.HEIGHT);

            boolean defeat = isDefeatStyle();
            drawBannerHeader(batch, defeat);

            float W = layoutWidth();
            float cx = PortraitViewport.x(0.5f);
            float buttonTop = actionButtonY() + actionButtonH() * 0.5f;

            boolean maxLevel = ReflectionHacks.<Boolean>getPrivate(screen, GameOverScreen.class, "maxLevel");
            float progressAlpha = ReflectionHacks.<Float>getPrivate(screen, GameOverScreen.class, "progressBarAlpha");
            boolean showProgress = !maxLevel && progressAlpha > 0.005f;
            float progressBlockH = showProgress ? W * 0.132f : 0f;
            float progressBottom = buttonTop + W * 0.038f;
            float cardBottomLimit = showProgress
                    ? progressBottom + progressBlockH + W * 0.036f
                    : buttonTop + W * 0.048f;
            float cardTopLimit = headerY() - W * 0.072f;
            float maxCardH = Math.max(W * 0.42f, cardTopLimit - cardBottomLimit);

            ArrayList<GameOverStat> rawStats = ReflectionHacks.getPrivate(screen, GameOverScreen.class, "stats");
            visibleItems.clear();
            GameOverStat totalStat = null;
            if (rawStats != null) {
                for (GameOverStat stat : rawStats) {
                    if (stat != null && stat.label != null && stat.value != null) {
                        visibleItems.add(stat);
                    }
                }
                if (!visibleItems.isEmpty()) {
                    totalStat = visibleItems.remove(visibleItems.size() - 1);
                }
            }

            float cardW = W * 0.88f;
            float padX = cardW * 0.068f;
            float padY = W * 0.044f;
            float rowCap = W * 0.034f;
            float rowPitch = rowCap * 1.94f;
            float totalCap = W * 0.044f;
            float dividerGap = W * 0.038f;
            float totalBlockH = totalStat != null ? (dividerGap + totalCap * 1.55f) : 0f;

            int count = visibleItems.size();
            float neededH = padY * 2f + count * rowPitch + totalBlockH;
            if (neededH > maxCardH && count > 0) {
                padY = W * 0.030f;
                dividerGap = W * 0.026f;
                totalCap = W * 0.038f;
                totalBlockH = totalStat != null ? (dividerGap + totalCap * 1.48f) : 0f;
                float availRows = Math.max(W * 0.22f, maxCardH - padY * 2f - totalBlockH);
                rowPitch = Math.min(rowPitch, availRows / count);
                rowCap = MathUtils.clamp(rowPitch / 1.72f, W * 0.021f, W * 0.034f);
                neededH = padY * 2f + count * rowPitch + totalBlockH;
            }

            float cardH = Math.min(maxCardH, Math.max(W * 0.26f, neededH));
            float freeY = Math.max(0f, maxCardH - cardH);
            float cardBottom = cardBottomLimit + freeY * 0.54f;
            float cardLeft = cx - cardW * 0.5f;
            float cardRight = cx + cardW * 0.5f;
            float cardTop = cardBottom + cardH;

            float cut = Math.min(W * 0.028f, cardH * 0.14f);
            float border = Math.max(2.5f, W * 0.0032f);
            Color accent = defeat ? DEFEAT_ACCENT : VICTORY_ACCENT;

            batch.setColor(accent.r, accent.g, accent.b, accent.a * screenAlpha);
            PortraitUi.drawCutCorner(batch, cardLeft, cardBottom, cardRight, cardTop, cut);
            batch.setColor(0.055f, 0.075f, 0.100f, 0.94f * screenAlpha);
            PortraitUi.drawCutCorner(batch, cardLeft + border, cardBottom + border,
                    cardRight - border, cardTop - border, Math.max(1f, cut - border * 0.6f));
            batch.setColor(accent.r, accent.g, accent.b, 0.26f * screenAlpha);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, cardLeft + cut, cardTop - border * 2.2f,
                    cardW - cut * 2f, Math.max(2f, border * 0.8f));

            float innerLeft = cardLeft + padX;
            float innerRight = cardRight - padX;
            float innerW = innerRight - innerLeft;
            float cursorY = cardTop - padY;

            BitmapFont bodyFont = PortraitFonts.body();
            BitmapFont headerFont = PortraitFonts.header();

            for (int i = 0; i < count; i++) {
                GameOverStat stat = visibleItems.get(i);
                Color statColor = ReflectionHacks.getPrivate(stat, GameOverStat.class, "color");
                float alpha = statColor != null ? statColor.a : 1f;
                float rawOffset = ReflectionHacks.<Float>getPrivate(stat, GameOverStat.class, "offsetX");
                float slideX = rawOffset * 1.4f;
                float rowCenterY = cursorY - rowPitch * 0.5f;

                if (stat.hb != null) {
                    stat.hb.resize(innerW + padX * 0.7f, Math.max(rowPitch * 0.90f, W * 0.048f));
                    stat.hb.move(cx, rowCenterY);
                }

                if (alpha > 0.005f) {
                    boolean hovered = stat.hb != null && stat.hb.hovered && stat.description != null;
                    if (hovered) {
                        batch.setColor(accent.r, accent.g, accent.b, 0.16f * alpha);
                        batch.draw(ImageMaster.WHITE_SQUARE_IMG, cardLeft + padX * 0.5f,
                                rowCenterY - rowPitch * 0.44f, cardW - padX, rowPitch * 0.88f);
                    }
                    workColor.set(hovered ? 0.62f : CREAM.r, hovered ? 0.96f : CREAM.g,
                            hovered ? 0.68f : CREAM.b, alpha);
                    drawLeftCentered(batch, bodyFont, stat.label, innerLeft + slideX, rowCenterY,
                            innerW * 0.74f, rowCap, workColor);
                    workColor.set(VALUE_COLOR.r, VALUE_COLOR.g, VALUE_COLOR.b, alpha);
                    drawRightCentered(batch, bodyFont, stat.value, innerRight + slideX, rowCenterY,
                            innerW * 0.24f, rowCap, workColor);
                }
                cursorY -= rowPitch;
            }

            if (totalStat != null) {
                Color totalColor = ReflectionHacks.getPrivate(totalStat, GameOverStat.class, "color");
                float totalAlpha = totalColor != null ? totalColor.a : screenAlpha;
                float rawOffset = ReflectionHacks.<Float>getPrivate(totalStat, GameOverStat.class, "offsetX");
                float slideX = rawOffset * 1.4f;
                float lineY = cursorY - dividerGap * 0.44f;
                float lineH = Math.max(2f, W * 0.0028f);

                if (totalAlpha > 0.005f) {
                    batch.setColor(accent.r, accent.g, accent.b, 0.72f * totalAlpha);
                    batch.draw(ImageMaster.WHITE_SQUARE_IMG, innerLeft, lineY, innerW, lineH);
                    float totalCenterY = lineY - dividerGap * 0.52f - totalCap * 0.58f;
                    workColor.set(Settings.GOLD_COLOR.r, Settings.GOLD_COLOR.g, Settings.GOLD_COLOR.b, totalAlpha);
                    drawLeftCentered(batch, headerFont, totalStat.label, innerLeft + slideX, totalCenterY,
                            innerW * 0.62f, totalCap, workColor);
                    drawRightCentered(batch, headerFont, totalStat.value, innerRight + slideX, totalCenterY,
                            innerW * 0.36f, totalCap, workColor);
                }
            }

            if (showProgress) {
                drawProgressCard(screen, batch, cardLeft, progressBottom, cardW, progressBlockH, progressAlpha);
            }
        } finally {
            batch.setColor(packed);
        }
    }

    private static void drawProgressCard(GameOverScreen screen, SpriteBatch batch,
            float left, float bottom, float width, float height, float alpha) {
        float W = layoutWidth();
        float cut = Math.min(W * 0.022f, height * 0.22f);
        float border = Math.max(2f, W * 0.0026f);
        float padX = width * 0.060f;
        float progressPercent = MathUtils.clamp(
                ReflectionHacks.<Float>getPrivate(screen, GameOverScreen.class, "progressPercent"), 0f, 1f);
        float unlockProgress = ReflectionHacks.<Float>getPrivate(screen, GameOverScreen.class, "unlockProgress");
        int unlockCost = ReflectionHacks.<Integer>getPrivate(screen, GameOverScreen.class, "unlockCost");
        int unlockLevel = ReflectionHacks.<Integer>getPrivate(screen, GameOverScreen.class, "unlockLevel");
        String[] text = ReflectionHacks.getPrivateStatic(GameOverScreen.class, "TEXT");

        batch.setColor(0.84f, 0.69f, 0.40f, 0.68f * alpha);
        PortraitUi.drawCutCorner(batch, left, bottom, left + width, bottom + height, cut);
        batch.setColor(0.055f, 0.075f, 0.100f, 0.92f * alpha);
        PortraitUi.drawCutCorner(batch, left + border, bottom + border,
                left + width - border, bottom + height - border, Math.max(1f, cut - border * 0.6f));

        float barW = width - padX * 2f;
        float barH = Math.max(10f, W * 0.024f);
        float barX = left + padX;
        float barY = bottom + height * 0.60f;

        batch.setColor(1f, 1f, 1f, 0.22f * alpha);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, barX, barY, barW, barH);
        if (progressPercent > 0.001f) {
            batch.setColor(1f, 0.80f, 0.30f, 0.92f * alpha);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, barX, barY, barW * progressPercent, barH);
            batch.setColor(0f, 0f, 0f, 0.22f * alpha);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, barX, barY, barW * progressPercent, barH * 0.28f);
        }

        String leftLabel = "[" + (int) unlockProgress + "/" + unlockCost + "]";
        int remaining = Math.max(0, 5 - unlockLevel);
        String rightLabel = "";
        if (text != null && text.length > 42) {
            rightLabel = (remaining == 1 ? text[42] : text[41]) + remaining;
        }
        float textY = bottom + height * 0.29f;
        float textCap = W * 0.027f;
        workColor.set(CREAM.r, CREAM.g, CREAM.b, 0.95f * alpha);
        BitmapFont bodyFont = PortraitFonts.body();
        drawLeftCentered(batch, bodyFont, leftLabel, barX, textY, barW * 0.42f, textCap, workColor);
        if (!rightLabel.isEmpty()) {
            drawRightCentered(batch, bodyFont, rightLabel, barX + barW, textY, barW * 0.56f, textCap, workColor);
        }
    }

    private static void layoutNeowUnlocks(NeowUnlockScreen screen, boolean snap) {
        if (screen.unlockBundle == null || screen.unlockBundle.isEmpty()) return;
        AbstractUnlock first = screen.unlockBundle.get(0);
        if (first == null || first.type == null) return;
        float W = layoutWidth();
        float cx = PortraitViewport.x(0.5f);
        int count = screen.unlockBundle.size();

        if (first.type == AbstractUnlock.UnlockType.CARD) {
            float step = count <= 1 ? 0f : Math.min(W * 0.315f, (W * 0.90f) / count);
            float cardScale = MathUtils.clamp((step * 0.92f) / Math.max(1f, AbstractCard.IMG_WIDTH), 0.65f, 1.35f);
            float cardY = PortraitViewport.y(0.54f);
            for (int i = 0; i < count; i++) {
                AbstractCard card = screen.unlockBundle.get(i).card;
                if (card == null) continue;
                float tx = cx + (i - (count - 1) * 0.5f) * step;
                card.target_x = tx;
                card.target_y = cardY;
                card.targetDrawScale = card.hb != null && card.hb.hovered ? cardScale * 1.18f : cardScale;
                if (snap) {
                    card.current_x = tx;
                    card.current_y = cardY;
                    card.drawScale = Math.max(0.08f, cardScale * 0.25f);
                }
            }
        } else if (first.type == AbstractUnlock.UnlockType.RELIC) {
            float slotStep = count <= 1 ? 0f : Math.min(W * 0.308f, (W * 0.90f) / count);
            float slotW = Math.min(W * 0.284f, count <= 1 ? W * 0.36f : slotStep * 0.92f);
            float slotH = W * 0.34f;
            float slotY = headerY() - W * 0.275f;
            for (int i = 0; i < count; i++) {
                AbstractRelic relic = screen.unlockBundle.get(i).relic;
                if (relic == null) continue;
                float tx = cx + (i - (count - 1) * 0.5f) * slotStep;
                relic.currentX = tx;
                relic.targetX = tx;
                relic.currentY = slotY + slotH * 0.10f;
                relic.targetY = relic.currentY;
                relic.hb.resize(slotW, slotH);
                relic.hb.move(tx, slotY);
                if (relic.hb.hovered && InputHelper.justClickedLeft) {
                    selectedUnlockRelic = i;
                }
            }
            if (selectedUnlockRelic < 0 || selectedUnlockRelic >= count) {
                selectedUnlockRelic = 0;
            }
        }
    }

    private static void renderPortraitNeowUnlock(NeowUnlockScreen screen, SpriteBatch batch) {
        float packed = batch.getPackedColor();
        try {
            batch.setColor(0.05f, 0.11f, 0.14f, 1f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, 0f, 0f, Settings.WIDTH, Settings.HEIGHT);

            ArrayList<ConeEffect> cones = ReflectionHacks.getPrivate(screen, NeowUnlockScreen.class, "cones");
            if (cones != null) {
                batch.setBlendFunction(770, 1);
                for (ConeEffect cone : cones) {
                    cone.render(batch);
                }
                batch.setBlendFunction(770, 771);
            }

            drawBannerHeader(batch, false);

            float W = layoutWidth();
            float cx = PortraitViewport.x(0.5f);
            if (screen.unlockBundle != null && !screen.unlockBundle.isEmpty()) {
                AbstractUnlock first = screen.unlockBundle.get(0);
                if (first != null && first.type == AbstractUnlock.UnlockType.CARD) {
                    AbstractCard hoveredCard = null;
                    for (AbstractUnlock unlock : screen.unlockBundle) {
                        if (unlock.card == null) continue;
                        if (unlock.card.hb != null && unlock.card.hb.hovered) {
                            hoveredCard = unlock.card;
                            continue;
                        }
                        unlock.card.renderHoverShadow(batch);
                        unlock.card.render(batch);
                    }
                    if (hoveredCard != null) {
                        hoveredCard.renderHoverShadow(batch);
                        hoveredCard.render(batch);
                        hoveredCard.renderCardTip(batch);
                    }
                } else if (first != null && first.type == AbstractUnlock.UnlockType.RELIC) {
                    drawUnlockRelics(screen, batch);
                } else if (first != null && first.type == AbstractUnlock.UnlockType.CHARACTER) {
                    first.render(batch);
                    if (first.player != null) first.player.renderPlayerImage(batch);
                }
            }

            boolean isRelicUnlock = screen.unlockBundle != null && !screen.unlockBundle.isEmpty()
                    && screen.unlockBundle.get(0) != null
                    && screen.unlockBundle.get(0).type == AbstractUnlock.UnlockType.RELIC;
            String subtitle = null;
            if (NeowUnlockScreen.TEXT != null) {
                if (isRelicUnlock && NeowUnlockScreen.TEXT.length > 4) {
                    subtitle = NeowUnlockScreen.TEXT[4];
                } else if (NeowUnlockScreen.TEXT.length > 3) {
                    subtitle = NeowUnlockScreen.TEXT[3];
                }
            }
            if (subtitle != null && !subtitle.isEmpty()) {
                float subY = actionButtonY() + actionButtonH() * 0.5f + W * 0.082f;
                drawSubtitlePill(batch, subtitle, subY, CREAM, W * 0.032f);
            }

            if (screen.button != null) {
                screen.button.render(batch);
            }
        } finally {
            batch.setColor(packed);
        }
    }

    private static void drawUnlockRelics(NeowUnlockScreen screen, SpriteBatch batch) {
        float W = layoutWidth();
        float cx = PortraitViewport.x(0.5f);
        int count = screen.unlockBundle.size();
        float slotStep = count <= 1 ? 0f : Math.min(W * 0.308f, (W * 0.90f) / count);
        float slotW = Math.min(W * 0.284f, count <= 1 ? W * 0.36f : slotStep * 0.92f);
        float slotH = W * 0.34f;
        float slotY = headerY() - W * 0.275f;
        float cut = Math.min(W * 0.022f, slotW * 0.085f);
        float border = Math.max(2f, W * 0.0030f);

        AbstractRelic activeRelic = null;
        for (int i = 0; i < count; i++) {
            AbstractRelic relic = screen.unlockBundle.get(i).relic;
            if (relic == null) continue;
            boolean hovered = relic.hb != null && relic.hb.hovered;
            boolean selected = i == selectedUnlockRelic || hovered;
            if (hovered) selectedUnlockRelic = i;
            if (i == selectedUnlockRelic) activeRelic = relic;

            float sx = cx + (i - (count - 1) * 0.5f) * slotStep;
            float left = sx - slotW * 0.5f;
            float bottom = slotY - slotH * 0.5f;
            float right = sx + slotW * 0.5f;
            float top = slotY + slotH * 0.5f;

            batch.setColor(selected ? 0.95f : 0.62f, selected ? 0.80f : 0.56f, selected ? 0.44f : 0.42f,
                    selected ? 0.96f : 0.62f);
            PortraitUi.drawCutCorner(batch, left, bottom, right, top, cut);
            batch.setColor(selected ? 0.10f : 0.06f, selected ? 0.15f : 0.09f, selected ? 0.20f : 0.12f, 0.94f);
            PortraitUi.drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                    Math.max(1f, cut - border * 0.6f));

            Texture icon = relic.largeImg != null ? relic.largeImg : relic.img;
            if (icon != null) {
                float iconSize = slotW * 0.68f;
                float iconY = slotY + slotH * 0.10f;
                if (relic.outlineImg != null && selected) {
                    batch.setColor(1f, 0.86f, 0.48f, 0.36f);
                    batch.draw(relic.outlineImg, sx - iconSize * 0.54f, iconY - iconSize * 0.54f,
                            iconSize * 1.08f, iconSize * 1.08f);
                }
                batch.setColor(Color.WHITE);
                batch.draw(icon, sx - iconSize * 0.5f, iconY - iconSize * 0.5f, iconSize, iconSize);
            }

            drawCenteredLabel(batch, PortraitFonts.header(), relic.name, sx, bottom + slotH * 0.19f,
                    slotW * 0.86f, W * 0.028f, selected ? Settings.GOLD_COLOR : CREAM);
        }

        if (activeRelic == null && count > 0) activeRelic = screen.unlockBundle.get(0).relic;
        if (activeRelic != null) {
            float descTop = slotY - slotH * 0.5f - W * 0.042f;
            float subTop = actionButtonY() + actionButtonH() * 0.5f + W * 0.155f;
            float availH = Math.max(W * 0.22f, descTop - subTop);
            float cardW = W * 0.88f;
            float padX = cardW * 0.065f;
            float padY = W * 0.040f;
            float titleCap = W * 0.038f;
            float bodyCap = W * 0.031f;
            String desc = activeRelic.description != null ? activeRelic.description : "";
            float textW = cardW - padX * 2f;
            float descH = richText.measureHeight(PortraitFonts.body(), desc, textW, bodyCap);
            float cardH = Math.min(availH, padY * 2f + titleCap * 1.60f + W * 0.032f + descH + W * 0.014f);
            float cardBottom = descTop - cardH;
            float left = cx - cardW * 0.5f;
            float dCut = Math.min(W * 0.024f, cardH * 0.18f);

            batch.setColor(0.86f, 0.72f, 0.42f, 0.86f);
            PortraitUi.drawCutCorner(batch, left, cardBottom, left + cardW, descTop, dCut);
            batch.setColor(0.055f, 0.080f, 0.105f, 0.94f);
            PortraitUi.drawCutCorner(batch, left + border, cardBottom + border,
                    left + cardW - border, descTop - border, Math.max(1f, dCut - border * 0.6f));

            float titleY = descTop - padY - titleCap * 0.52f;
            drawCenteredLabel(batch, PortraitFonts.header(), activeRelic.name, cx, titleY,
                    textW, titleCap, Settings.GOLD_COLOR);
            float ruleY = descTop - padY - titleCap * 1.58f;
            batch.setColor(0.84f, 0.69f, 0.40f, 0.52f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + padX, ruleY, textW, Math.max(2f, W * 0.0024f));
            richText.drawTop(batch, PortraitFonts.body(), desc, CREAM,
                    left + padX, ruleY - W * 0.032f, textW, Math.max(descH + bodyCap, W * 0.10f),
                    bodyCap, Align.left, AbstractDungeon.player);
        }
    }

    private static void drawBannerHeader(SpriteBatch batch, boolean defeat) {
        String title = AbstractDungeon.dynamicBanner != null
                ? ReflectionHacks.getPrivate(AbstractDungeon.dynamicBanner, DynamicBanner.class, "label") : null;
        if (title == null || title.isEmpty()) {
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.VICTORY || GameOverScreen.isVictory) {
                title = PortraitDrawing.chinese() ? "胜利！" : "Victory!";
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DEATH) {
                title = PortraitDrawing.chinese() ? "败北" : "Defeat";
            } else {
                return;
            }
        }
        float W = layoutWidth();
        float cx = PortraitViewport.x(0.5f);
        float cy = headerY();
        BitmapFont font = PortraitFonts.header();
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        boolean oldMarkup = font.getData().markupEnabled;
        float targetCap = W * 0.060f;
        font.getData().markupEnabled = false;
        measure.setText(font, title);
        float fit = Math.min(W * 0.72f / Math.max(1f, measure.width), targetCap / Math.max(1f, font.getCapHeight()));
        font.getData().setScale(oldX * fit, oldY * fit);
        float packed = batch.getPackedColor();
        try {
            measure.setText(font, title);
            float halfText = measure.width * 0.5f;
            float gap = W * 0.036f;
            float lineLeft = PortraitViewport.x(0.06f);
            float lineRight = PortraitViewport.x(0.94f);
            float lineH = Math.max(2.5f, W * 0.0030f);
            Color accent = defeat ? DEFEAT_ACCENT : VICTORY_ACCENT;
            batch.setColor(accent.r, accent.g, accent.b, 0.62f);
            float leftLen = (cx - halfText - gap) - lineLeft;
            if (leftLen > 12f) {
                batch.draw(ImageMaster.WHITE_SQUARE_IMG, lineLeft, cy - lineH * 0.5f, leftLen, lineH);
                batch.draw(ImageMaster.WHITE_SQUARE_IMG, cx + halfText + gap, cy - lineH * 0.5f,
                        lineRight - (cx + halfText + gap), lineH);
            }
            FontHelper.renderFontCentered(batch, font, title, cx, cy, defeat ? DEFEAT_TITLE : Settings.GOLD_COLOR);
        } finally {
            font.getData().markupEnabled = oldMarkup;
            font.getData().setScale(oldX, oldY);
            batch.setColor(packed);
        }
    }

    private static void drawSubtitlePill(SpriteBatch batch, String text, float cy, Color color, float cap) {
        float W = layoutWidth();
        float cx = PortraitViewport.x(0.5f);
        float maxW = W * 0.80f;
        float textH = richText.measureHeight(PortraitFonts.body(), text, maxW, cap);
        float boxW = W * 0.88f;
        float padY = W * 0.026f;
        float boxH = textH + padY * 2f;
        float left = cx - boxW * 0.5f;
        float bottom = cy - boxH * 0.5f;
        float cut = Math.min(boxH * 0.25f, W * 0.020f);
        float border = Math.max(2f, W * 0.0024f);
        float packed = batch.getPackedColor();
        try {
            batch.setColor(0.82f, 0.68f, 0.40f, 0.58f);
            PortraitUi.drawCutCorner(batch, left, bottom, left + boxW, bottom + boxH, cut);
            batch.setColor(0.045f, 0.068f, 0.088f, 0.86f);
            PortraitUi.drawCutCorner(batch, left + border, bottom + border,
                    left + boxW - border, bottom + boxH - border, Math.max(1f, cut - border * 0.6f));
            richText.drawTop(batch, PortraitFonts.body(), text, color,
                    cx - maxW * 0.5f, bottom + boxH - padY, maxW, textH + cap,
                    cap, Align.center, AbstractDungeon.player);
        } finally {
            batch.setColor(packed);
        }
    }

    private static void drawActionButton(SpriteBatch batch, String label, boolean hovered, boolean pressed,
            float alpha, boolean defeat) {
        float cx = PortraitViewport.x(0.5f);
        float cy = actionButtonY();
        float w = actionButtonW();
        float h = actionButtonH();
        float left = cx - w * 0.5f;
        float right = cx + w * 0.5f;
        float bottom = cy - h * 0.5f;
        float top = cy + h * 0.5f;
        float cut = Math.min(h * 0.25f, w * 0.055f);
        float border = Math.max(2.5f, h * 0.030f);
        float packed = batch.getPackedColor();
        try {
            if (defeat) {
                batch.setColor(hovered ? 0.98f : 0.88f, hovered ? 0.78f : 0.66f, hovered ? 0.52f : 0.42f, 0.96f * alpha);
                PortraitUi.drawCutCorner(batch, left, bottom, right, top, cut);
                float shade = pressed ? 0.78f : 1f;
                batch.setColor((hovered ? 0.24f : 0.14f) * shade, (hovered ? 0.20f : 0.13f) * shade,
                        (hovered ? 0.24f : 0.17f) * shade, 0.96f * alpha);
                PortraitUi.drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
            } else {
                batch.setColor(hovered ? 1f : 0.92f, hovered ? 0.88f : 0.76f, hovered ? 0.56f : 0.42f, 0.98f * alpha);
                PortraitUi.drawCutCorner(batch, left, bottom, right, top, cut);
                float shade = pressed ? 0.78f : 1f;
                batch.setColor((hovered ? 0.18f : 0.12f) * shade, (hovered ? 0.36f : 0.26f) * shade,
                        (hovered ? 0.40f : 0.30f) * shade, 0.96f * alpha);
                PortraitUi.drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
            }
            batch.setColor(1f, 0.88f, 0.56f, (hovered ? 0.30f : 0.18f) * alpha);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + cut, top - border * 2.1f,
                    w - cut * 2f, Math.max(2f, border * 0.75f));

            workColor.set(hovered ? Settings.GOLD_COLOR.r : BUTTON_TEXT.r,
                    hovered ? Settings.GOLD_COLOR.g : BUTTON_TEXT.g,
                    hovered ? Settings.GOLD_COLOR.b : BUTTON_TEXT.b, alpha);
            drawCenteredLabel(batch, PortraitFonts.header(), label, cx, cy, w * 0.84f, h * 0.40f, workColor);
        } finally {
            batch.setColor(packed);
        }
    }

    private static void drawCenteredLabel(SpriteBatch batch, BitmapFont font, String text,
            float cx, float cy, float maxWidth, float targetCap, Color color) {
        if (text == null || text.isEmpty()) return;
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        boolean oldMarkup = font.getData().markupEnabled;
        font.getData().markupEnabled = false;
        measure.setText(font, text);
        float fit = Math.min(maxWidth / Math.max(1f, measure.width), targetCap / Math.max(1f, font.getCapHeight()));
        font.getData().setScale(oldX * fit, oldY * fit);
        try {
            FontHelper.renderFontCentered(batch, font, text, cx, cy, color);
        } finally {
            font.getData().markupEnabled = oldMarkup;
            font.getData().setScale(oldX, oldY);
        }
    }

    private static void drawLeftCentered(SpriteBatch batch, BitmapFont font, String text,
            float x, float cy, float maxWidth, float targetCap, Color color) {
        if (text == null || text.isEmpty()) return;
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        boolean oldMarkup = font.getData().markupEnabled;
        font.getData().markupEnabled = false;
        measure.setText(font, text);
        float fit = Math.min(maxWidth / Math.max(1f, measure.width), targetCap / Math.max(1f, font.getCapHeight()));
        font.getData().setScale(oldX * fit, oldY * fit);
        try {
            FontHelper.renderFontLeft(batch, font, text, x, cy, color);
        } finally {
            font.getData().markupEnabled = oldMarkup;
            font.getData().setScale(oldX, oldY);
        }
    }

    private static void drawRightCentered(SpriteBatch batch, BitmapFont font, String text,
            float rightX, float cy, float maxWidth, float targetCap, Color color) {
        if (text == null || text.isEmpty()) return;
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        boolean oldMarkup = font.getData().markupEnabled;
        font.getData().markupEnabled = false;
        measure.setText(font, text);
        float fit = Math.min(maxWidth / Math.max(1f, measure.width), targetCap / Math.max(1f, font.getCapHeight()));
        font.getData().setScale(oldX * fit, oldY * fit);
        try {
            measure.setText(font, text);
            FontHelper.renderFontLeft(batch, font, text, rightX - measure.width, cy, color);
        } finally {
            font.getData().markupEnabled = oldMarkup;
            font.getData().setScale(oldX, oldY);
        }
    }
}
