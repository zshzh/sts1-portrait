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
    private static final Matrix4 panelTransform = new Matrix4();
    private static final GlyphLayout headerLayout = new GlyphLayout();
    private static final Color TIP_TEXT = new Color(1f, .9725f, .8745f, 1f);
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
        float unit = Settings.scale;
        float nativeWidth = 320f * unit;
        float rowGap = 5f * unit;
        float nativeHeight = 14f * unit - rowGap;
        for (PowerTip tip : tips) nativeHeight += tooltipHeight(tip, unit) + rowGap;

        float gap = width * .025f;
        float top = toolbarBottom - gap;
        float center = PortraitViewport.x(.5f);
        float targetWidth = width * .53f;
        boolean nextToPotionMenu = potionMenuLeft != Float.POSITIVE_INFINITY;
        if (nextToPotionMenu) {
            float right = potionMenuLeft - gap;
            targetWidth = Math.min(targetWidth, right - PortraitViewport.x(0f) - gap);
            center = right - targetWidth * .5f;
            top = potionMenuTop;
        } else if (center - targetWidth * .5f < potionMenuRight + gap) {
            if (potionMenuRight + gap + targetWidth <= PortraitViewport.x(1f) - gap) {
                center = potionMenuRight + gap + targetWidth * .5f;
            } else {
                top = Math.min(top, potionMenuBottom - gap);
            }
        }
        float lowerBound = nextToPotionMenu ? PortraitBattle.playerBattleY() - width * .085f
                : PortraitViewport.y(PortraitCombatHud.active() ? .28f : .15f);
        float leftBound = PortraitViewport.x(0f);
        if (center - targetWidth * .5f >= potionMenuRight + gap) {
            leftBound = Math.max(leftBound, potionMenuRight + gap);
        }
        if (top <= lowerBound) return;
        float scale = Math.min(targetWidth / nativeWidth, (top - lowerBound) / nativeHeight);
        float panelWidth = nativeWidth * scale;
        float panelHeight = nativeHeight * scale;
        float centerY = top - panelHeight * .5f;
        float color = batch.getPackedColor();

        // Share the actual HUD coordinate space. Third-party tooltip hooks can change the live batch.
        savedProjection.set(batch.getProjectionMatrix());
        savedTransform.set(batch.getTransformMatrix());
        batch.setProjectionMatrix(toolbarProjection);
        batch.setTransformMatrix(toolbarTransform);
        float rightBound = nextToPotionMenu ? potionMenuLeft - gap : PortraitViewport.x(1f);
        placement.placeWithin(center, centerY, panelWidth, panelHeight, leftBound, lowerBound, rightBound, top);
        placement.begin(batch);
        batch.setTransformMatrix(panelTransform.set(batch.getTransformMatrix())
                .translate(center - panelWidth * .5f, top, 0f).scale(scale, scale, 1f));
        try {
            float rowTop = 0f;
            for (PowerTip tip : tips) {
                float height = tooltipHeight(tip, unit);
                drawToolbarTip(batch, tip, rowTop, nativeWidth, height, unit);
                rowTop -= height + rowGap;
            }
        } finally {
            placement.end(batch);
            batch.setTransformMatrix(savedTransform);
            batch.setProjectionMatrix(savedProjection);
            batch.setColor(color);
        }
    }

    private static boolean hasIcon(PowerTip tip) { return tip.img != null || tip.imgRegion != null; }

    private static void layoutTitle(PowerTip tip, float unit) {
        headerLayout.setText(FontHelper.tipHeaderFont, tip.header == null ? "" : tip.header,
                Settings.GOLD_COLOR, (hasIcon(tip) ? 244f : 278f) * unit, Align.left, true);
    }

    private static float titleHeight(float unit) {
        return Math.max(32f * unit, headerLayout.height);
    }

    private static float tooltipHeight(PowerTip tip, float unit) {
        layoutTitle(tip, unit);
        BitmapFont body = FontHelper.tipBodyFont;
        if (tip.body == null || tip.body.isEmpty()) return 36f * unit + titleHeight(unit);
        float lineSpacing = Math.max(26f * unit, body.getLineHeight());
        float wrapped = Math.abs(FontHelper.getSmartHeight(body, tip.body, 278f * unit, lineSpacing));
        return 41f * unit + titleHeight(unit) + wrapped + body.getCapHeight();
    }

    private static void drawToolbarTip(SpriteBatch batch, PowerTip tip, float top,
            float width, float height, float unit) {
        batch.setColor(Settings.TOP_PANEL_SHADOW_COLOR);
        drawFrame(batch, 9f * unit, top - 14f * unit, width, height, unit);
        batch.setColor(Color.WHITE);
        drawFrame(batch, 0f, top, width, height, unit);
        float textX = 22f * unit;
        float titleY = top - 18f * unit;
        if (tip.imgRegion != null) {
            batch.draw(tip.imgRegion, textX, titleY - 29f * unit, 29f * unit, 29f * unit);
            textX += 34f * unit;
        } else if (tip.img != null) {
            batch.draw(tip.img, textX, titleY - 29f * unit, 29f * unit, 29f * unit);
            textX += 34f * unit;
        }
        layoutTitle(tip, unit);
        FontHelper.tipHeaderFont.draw(batch, headerLayout, textX, titleY);
        if (tip.body != null && !tip.body.isEmpty()) {
            BitmapFont body = FontHelper.tipBodyFont;
            FontHelper.renderSmartText(batch, body, tip.body, 22f * unit, titleY - titleHeight(unit) - 6f * unit,
                    278f * unit, Math.max(26f * unit, body.getLineHeight()), TIP_TEXT);
        }
    }

    private static void drawFrame(SpriteBatch batch, float x, float top, float width, float height, float unit) {
        float edge = 32f * unit;
        batch.draw(ImageMaster.KEYWORD_TOP, x, top - edge, width, edge);
        batch.draw(ImageMaster.KEYWORD_BODY, x, top - height + edge, width, height - 2f * edge);
        batch.draw(ImageMaster.KEYWORD_BOT, x, top - height, width, edge);
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
