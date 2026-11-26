package sts1portrait;

import basemod.ReflectionHacks;
import basemod.patches.com.megacrit.cardcrawl.screens.SingleCardViewPopup.ScrollingTooltips;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.SingleRelicViewPopup;
import com.megacrit.cardcrawl.ui.panels.PotionPopUp;
import com.megacrit.cardcrawl.ui.panels.TopPanel;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class PortraitRelicDetails {

    private static final Matrix4 toolbarProjection = new Matrix4();
    private static final Matrix4 toolbarTransform = new Matrix4();
    private static final Matrix4 savedProjection = new Matrix4();
    private static final Matrix4 savedTransform = new Matrix4();
    private static final GlyphLayout headerLayout = new GlyphLayout();
    private static final PortraitRichText bodyRichText = new PortraitRichText(true);
    private static final Color TIP_TEXT = new Color(0.95f, 0.93f, 0.87f, 1f);
    private static float toolbarBottom;
    private static boolean toolbarDrawn;

    private static final ArrayList<PowerTip> statTips = new ArrayList<>(1);
    private static ArrayList<PowerTip> queuedTips;
    private static final PopupPlacement placement = new PopupPlacement();
    private static float potionMenuRight, potionMenuBottom;
    private static float potionMenuLeft, potionMenuTop;

    private PortraitRelicDetails() {}

    static void queueTopBar(AbstractRelic relic) {
        if (queuedTips == null) queue(relic.tips);
    }

    private static boolean toolbarTips() {
        return PortraitCombatHud.topBarActive() && !Settings.hideTopBar;
    }

    private static void queue(ArrayList<PowerTip> tips) {
        potionMenuRight = Float.NEGATIVE_INFINITY;
        potionMenuLeft = Float.POSITIVE_INFINITY;
        queuedTips = tips;
    }

    public static void queueToolbarStat(float x, float y, String title, String body) {
        if (!toolbarTips()) {
            TipHelper.renderGenericTip(x, y, title, body);
            return;
        }
        if (Settings.hidePopupDetails) return;
        if (statTips.isEmpty() || !title.equals(statTips.get(0).header) || !body.equals(statTips.get(0).body)) {
            statTips.clear();
            statTips.add(new PowerTip(title, body));
        }
        queue(statTips);
    }

    public static void queueToolbarPotion(float x, float y, ArrayList<PowerTip> tips) {
        if (toolbarTips()) {
            if (!Settings.hidePopupDetails) queue(tips);
        } else {
            TipHelper.queuePowerTips(x, y, tips);
        }
    }

    @SpirePatch(clz = TopPanel.class, method = "updateTips")
    @SpirePatch(clz = TopPanel.class, method = "renderPotionTips")
    public static class ToolbarSources {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (!TipHelper.class.getName().equals(call.getClassName())) return;
                    if ("renderGenericTip".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitRelicDetails.queueToolbarStat($$); }");
                    } else if ("queuePowerTips".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitRelicDetails.queueToolbarPotion($$); }");
                    }
                }
            };
        }
    }

    public static void queueOpenPotion(float x, float y, ArrayList<PowerTip> tips, PotionPopUp menu) {
        if (!toolbarTips()) {
            TipHelper.queuePowerTips(x, y, tips);
        } else if (!menu.isHidden && !Settings.hidePopupDetails) {
            queue(tips);
            float menuX = x - 180f * Settings.scale;
            float menuY = y - 70f * Settings.scale;
            PortraitPopupMenus.placePotionMenu(menuX, menuY);
            potionMenuRight = PortraitPopupMenus.potionMenuRight(menuX);
            potionMenuBottom = PortraitPopupMenus.potionMenuBottom(menuY);
            if (Sts2Preview.compact()) {
                potionMenuLeft = PortraitPopupMenus.potionMenuLeft(menuX);
                potionMenuTop = PortraitPopupMenus.potionMenuTop(menuY);
            }
        }
    }

    @SpirePatch(clz = PotionPopUp.class, method = "update")
    public static class OpenPotionSource {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (TipHelper.class.getName().equals(call.getClassName())
                            && "queuePowerTips".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitRelicDetails.queueOpenPotion($$, this); }");
                    }
                }
            };
        }
    }


    static void recordToolbar(SpriteBatch batch, float bottom) {
        toolbarProjection.set(batch.getProjectionMatrix());
        toolbarTransform.set(batch.getTransformMatrix());
        toolbarBottom = bottom;
        toolbarDrawn = true;
    }

    @SpirePatch(clz = CardCrawlGame.class, method = "render", paramtypez = {})
    public static class TopBarDrawing {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (TipHelper.class.getName().equals(call.getClassName()) && "render".equals(call.getMethodName())) {
                        call.replace("{ $proceed($$); sts1portrait.PortraitRelicDetails.drawQueuedToolbarTips($1); }");
                    }
                }
            };
        }
    }

    public static void drawQueuedToolbarTips(SpriteBatch batch) {
        ArrayList<PowerTip> tips = queuedTips;
        queuedTips = null;
        boolean hasToolbar = toolbarDrawn;
        toolbarDrawn = false;
        if (hasToolbar && tips != null && !tips.isEmpty() && toolbarTips() && !CardCrawlGame.isPopupOpen) {
            drawTopBar(batch, tips);
        }
    }

    private static void drawTopBar(SpriteBatch batch, ArrayList<PowerTip> tips) {
        float width = PortraitViewport.contentWidth();
        float gap = width * .025f;
        float top = toolbarBottom - gap;
        float center = PortraitViewport.x(.5f);
        float panelWidth = width * .56f;
        boolean nextToPotionMenu = potionMenuLeft != Float.POSITIVE_INFINITY;
        if (nextToPotionMenu) {
            float right = potionMenuLeft - gap;
            panelWidth = Math.min(panelWidth, right - PortraitViewport.x(0f) - gap);
            center = right - panelWidth * .5f;
            top = potionMenuTop;
        } else if (center - panelWidth * .5f < potionMenuRight + gap) {
            if (potionMenuRight + gap + panelWidth <= PortraitViewport.x(1f) - gap) {
                center = potionMenuRight + gap + panelWidth * .5f;
            } else {
                top = Math.min(top, potionMenuBottom - gap);
            }
        }
        float lowerBound = nextToPotionMenu ? PortraitBattle.playerBattleY() - width * .085f
                : PortraitViewport.y(PortraitCombatHud.active() ? .28f : .15f);
        float leftBound = PortraitViewport.x(0f) + gap * 0.5f;
        if (center - panelWidth * .5f >= potionMenuRight + gap) {
            leftBound = Math.max(leftBound, potionMenuRight + gap);
        }
        float rightBound = nextToPotionMenu ? potionMenuLeft - gap : PortraitViewport.x(1f) - gap * 0.5f;
        float availH = top - lowerBound;
        if (availH <= width * 0.08f || panelWidth <= width * 0.20f) return;

        float fontScale = 1f;
        float totalHeight = measureStack(tips, panelWidth, width, fontScale);
        if (totalHeight > availH && totalHeight > 1f) {
            fontScale = Math.max(0.68f, availH / totalHeight);
            totalHeight = measureStack(tips, panelWidth, width, fontScale);
        }
        float left = Math.max(leftBound, Math.min(rightBound - panelWidth, center - panelWidth * 0.5f));
        float drawTop = top;
        if (drawTop - totalHeight < lowerBound) {
            drawTop = Math.min(PortraitViewport.y(1f) - gap, lowerBound + totalHeight);
        }

        float color = batch.getPackedColor();
        savedProjection.set(batch.getProjectionMatrix());
        savedTransform.set(batch.getTransformMatrix());
        batch.setProjectionMatrix(toolbarProjection);
        batch.setTransformMatrix(toolbarTransform);
        try {
            float rowGap = width * 0.016f * fontScale;
            float rowTop = drawTop;
            for (PowerTip tip : tips) {
                float h = measureTip(tip, panelWidth, width, fontScale);
                drawCrispTip(batch, tip, left, rowTop, panelWidth, h, width, fontScale);
                rowTop -= h + rowGap;
            }
        } finally {
            batch.setTransformMatrix(savedTransform);
            batch.setProjectionMatrix(savedProjection);
            batch.setColor(color);
        }
    }

    private static boolean hasIcon(PowerTip tip) { return tip.img != null || tip.imgRegion != null; }

    private static float measureStack(ArrayList<PowerTip> tips, float panelWidth, float width, float scale) {
        float rowGap = width * 0.016f * scale;
        float total = -rowGap;
        for (PowerTip tip : tips) {
            total += measureTip(tip, panelWidth, width, scale) + rowGap;
        }
        return Math.max(0f, total);
    }

    private static float measureTip(PowerTip tip, float panelWidth, float width, float scale) {
        float padX = width * 0.032f * scale;
        float padY = width * 0.026f * scale;
        float titleCap = width * 0.031f * scale;
        float bodyCap = width * 0.026f * scale;
        float textW = Math.max(40f, panelWidth - padX * 2f);
        float headerBlock = titleCap * 1.48f;
        if (tip.body == null || tip.body.isEmpty()) {
            return padY * 2f + headerBlock;
        }
        float bodyH = bodyRichText.measureHeight(PortraitFonts.body(), tip.body, textW, bodyCap);
        float dividerGap = width * 0.018f * scale;
        return padY * 2f + headerBlock + dividerGap + bodyH;
    }

    private static void drawCrispTip(SpriteBatch batch, PowerTip tip, float left, float top,
            float panelWidth, float height, float width, float scale) {
        float bottom = top - height;
        float right = left + panelWidth;
        float cut = Math.min(width * 0.018f, height * 0.16f);
        float border = Math.max(2f, width * 0.0024f);
        float padX = width * 0.032f * scale;
        float padY = width * 0.026f * scale;
        float titleCap = width * 0.031f * scale;
        float bodyCap = width * 0.026f * scale;
        float textW = Math.max(40f, panelWidth - padX * 2f);

        batch.setColor(0f, 0f, 0f, 0.34f);
        PortraitUi.drawCutCorner(batch, left + width * 0.006f, bottom - width * 0.008f,
                right + width * 0.006f, top - width * 0.008f, cut);
        batch.setColor(0.84f, 0.70f, 0.40f, 0.86f);
        PortraitUi.drawCutCorner(batch, left, bottom, right, top, cut);
        batch.setColor(0.055f, 0.080f, 0.105f, 0.96f);
        PortraitUi.drawCutCorner(batch, left + border, bottom + border,
                right - border, top - border, Math.max(1f, cut - border * 0.6f));
        batch.setColor(0.95f, 0.82f, 0.48f, 0.22f);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + cut, top - border * 2.0f,
                panelWidth - cut * 2f, Math.max(1.5f, border * 0.7f));

        float textX = left + padX;
        float headerCenterY = top - padY - titleCap * 0.62f;
        float titleMaxW = textW;
        if (hasIcon(tip)) {
            float iconSize = titleCap * 1.25f;
            batch.setColor(Color.WHITE);
            if (tip.imgRegion != null) {
                batch.draw(tip.imgRegion, textX, headerCenterY - iconSize * 0.5f, iconSize, iconSize);
            } else if (tip.img != null) {
                batch.draw(tip.img, textX, headerCenterY - iconSize * 0.5f, iconSize, iconSize);
            }
            float step = iconSize + width * 0.012f * scale;
            textX += step;
            titleMaxW = Math.max(32f, titleMaxW - step);
        }

        if (tip.header != null && !tip.header.isEmpty()) {
            BitmapFont headerFont = PortraitFonts.header();
            float oldX = headerFont.getData().scaleX;
            float oldY = headerFont.getData().scaleY;
            boolean oldMarkup = headerFont.getData().markupEnabled;
            headerFont.getData().markupEnabled = false;
            headerLayout.setText(headerFont, tip.header);
            float fit = Math.min(titleMaxW / Math.max(1f, headerLayout.width),
                    titleCap / Math.max(1f, headerFont.getCapHeight()));
            headerFont.getData().setScale(oldX * fit, oldY * fit);
            try {
                FontHelper.renderFontLeft(batch, headerFont, tip.header, textX, headerCenterY, Settings.GOLD_COLOR);
            } finally {
                headerFont.getData().markupEnabled = oldMarkup;
                headerFont.getData().setScale(oldX, oldY);
            }
        }

        if (tip.body != null && !tip.body.isEmpty()) {
            float ruleY = top - padY - titleCap * 1.48f;
            batch.setColor(0.84f, 0.70f, 0.40f, 0.42f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + padX, ruleY, textW, Math.max(1.5f, width * 0.0020f));
            float bodyTop = ruleY - width * 0.018f * scale;
            bodyRichText.drawTop(batch, PortraitFonts.body(), tip.body, TIP_TEXT,
                    left + padX, bodyTop, textW, Math.max(bodyCap * 2f, bodyTop - bottom),
                    bodyCap, Align.left, AbstractDungeon.player);
        }
    }

    @SpirePatch(clz = SingleRelicViewPopup.class, method = "renderTips", paramtypez = {SpriteBatch.class})
    public static class Keywords {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    boolean scroll = ScrollingTooltips.class.getName().equals(call.getClassName())
                            && "queueScrollingPowerTips".equals(call.getMethodName());
                    if (!scroll && !(TipHelper.class.getName().equals(call.getClassName())
                            && "queuePowerTips".equals(call.getMethodName()))) return;
                    call.replace("{ if (sts1portrait.PortraitScreens.enabled()) {"
                            + " $1 = sts1portrait.PortraitViewport.x(0.36f);"
                            + " $2 = sts1portrait.PortraitViewport.y(0.34f);"
                            + (scroll ? " $4 = $2 - sts1portrait.PortraitViewport.y(0.075f);" : "")
                            + " } $proceed($$); }");
                }
            };
        }
    }

    @SpirePatch(cls = "basemod.patches.whatmod.RelicView", method = "Postfix",
            paramtypez = {SingleRelicViewPopup.class, SpriteBatch.class}, optional = true)
    public static class ModLabel {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if ("basemod.patches.whatmod.WhatMod".equals(call.getClassName())
                            && "renderModTooltipBottomLeft".equals(call.getMethodName())) {
                        call.replace("{ if (sts1portrait.PortraitScreens.enabled()) {"
                                + " basemod.patches.whatmod.WhatMod.renderModTooltip($1, sts1portrait.PortraitViewport.x(0.36f),"
                                + " sts1portrait.PortraitViewport.y(0.285f), $2);"
                                + " } else { $proceed($$); } }");
                    }
                }
            };
        }
    }
}
