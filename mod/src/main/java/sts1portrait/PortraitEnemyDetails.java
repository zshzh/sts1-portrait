package sts1portrait;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class PortraitEnemyDetails {
    private static final Matrix4 originalTransform = new Matrix4();
    private static final Matrix4 fittedTransform = new Matrix4();
    private static final ArrayList<PortraitRichText> tipBodies = new ArrayList<>();
    private static AbstractCreature queuedCreature;
    private static ArrayList<PowerTip> queuedTips;

    private PortraitEnemyDetails() {
    }

    private static boolean enabled() {
        return PortraitBattle.active() && !AbstractDungeon.isScreenUp && !CardCrawlGame.isPopupOpen;
    }

    public static boolean queue(AbstractCreature creature, ArrayList<PowerTip> tips) {
        if (!enabled()) return false;
        if (!tips.isEmpty() && (queuedCreature == null || creature instanceof AbstractPlayer)) {
            queuedCreature = creature;
            queuedTips = tips;
        }
        return true;
    }

    @SpirePatch(clz = AbstractMonster.class, method = "renderTip", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = AbstractPlayer.class, method = "renderPowerTips", paramtypez = {SpriteBatch.class})
    public static class ReplaceCreatureTips {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (TipHelper.class.getName().equals(call.getClassName())
                            && "queuePowerTips".equals(call.getMethodName())) {
                        call.replace("{ if (!sts1portrait.PortraitEnemyDetails.queue(this, $3)) $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = TipHelper.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class DrawCreatureDetails {
        @SpirePostfixPatch
        public static void postfix(SpriteBatch batch) {
            AbstractCreature creature = queuedCreature;
            ArrayList<PowerTip> tips = queuedTips;
            queuedCreature = null;
            queuedTips = null;
            if (creature != null && enabled()) draw(batch, creature, tips);
        }
    }

    private static void draw(SpriteBatch batch, AbstractCreature creature, ArrayList<PowerTip> tips) {
        boolean player = creature instanceof AbstractPlayer;
        float width = PortraitViewport.contentWidth();
        float panelWidth = width * 0.78f;
        float x = PortraitViewport.x(0.5f) - panelWidth * 0.5f;
        float padX = width * 0.032f;
        float padTop = width * 0.030f;
        float padBot = width * 0.030f;
        float textWidth = panelWidth - 2f * padX;

        float titleCap = width * 0.030f;
        float labelCap = width * 0.0235f;
        float bodyCap = width * 0.0235f;
        float titleToDivider = titleCap * 1.72f;
        float dividerToBody = width * 0.024f;
        float labelToBody = labelCap * 1.68f;
        float tipGap = Math.max(width * 0.030f, bodyCap * 1.28f);

        BitmapFont heading = PortraitFonts.header();
        BitmapFont body = PortraitFonts.body();
        float headingX = heading.getData().scaleX;
        float headingY = heading.getData().scaleY;
        float headingBaseCap = Math.max(1f, heading.getCapHeight());
        boolean headingMarkup = heading.getData().markupEnabled;
        float packed = batch.getPackedColor();

        ArrayList<PowerTip> validTips = new ArrayList<>();
        if (tips != null) {
            for (PowerTip tip : tips) {
                if (tip != null) validTips.add(tip);
            }
        }
        while (tipBodies.size() < validTips.size()) {
            tipBodies.add(new PortraitRichText(true));
        }

        float[] bodyHeights = new float[validTips.size()];
        float contentHeight = titleToDivider + dividerToBody;
        for (int i = 0; i < validTips.size(); i++) {
            PowerTip tip = validTips.get(i);
            float bh = tip.body != null && !tip.body.isEmpty()
                    ? tipBodies.get(i).measureHeight(body, tip.body, textWidth, bodyCap)
                    : 0f;
            bodyHeights[i] = bh;
            if (i > 0) contentHeight += tipGap;
            if (tip.header != null && !tip.header.isEmpty()) contentHeight += labelToBody;
            if (bh > 0f) contentHeight += bh + bodyCap * 0.20f;
        }
        float panelHeight = padTop + contentHeight + padBot;

        boolean fitted = false;
        try {
            float preferredTop = creature.healthHb.y - width * 0.027f;
            if (player) {
                preferredTop = PortraitViewport.y(0.72f);
            } else {
                for (AbstractMonster enemy : AbstractDungeon.getMonsters().monsters) {
                    if (!enemy.isDeadOrEscaped()) {
                        preferredTop = Math.min(preferredTop, enemy.healthHb.y - width * 0.027f);
                    }
                }
            }
            float upperLimit = PortraitCombatHud.topBarActive()
                    ? PortraitCombatHud.panelBottom() - width * 0.02f
                    : PortraitViewport.y(0.86f);
            float lowerLimit = Math.min(PortraitViewport.y(player ? 0.43f : 0.40f),
                    PortraitBattle.battleHandTop() + width * 0.03f);
            float fit = Math.min(1f, (upperLimit - lowerLimit) / Math.max(1f, panelHeight));
            float top = Math.min(upperLimit, Math.max(preferredTop, lowerLimit + panelHeight * fit));
            float bottom = top - panelHeight;

            float center = x + panelWidth * 0.5f;
            if (fit != 1f) {
                originalTransform.set(batch.getTransformMatrix());
                fittedTransform.set(originalTransform).translate(center, top, 0f)
                        .scale(fit, fit, 1f).translate(-center, -top, 0f);
                batch.setTransformMatrix(fittedTransform);
                fitted = true;
            }

            float cut = Math.min(panelWidth * 0.022f, panelHeight * 0.075f);
            float border = Math.max(2f, panelWidth * 0.0026f);
            batch.setColor(0f, 0f, 0f, 0.48f);
            PortraitUi.drawCutCorner(batch, x + 5f, bottom - 6f, x + panelWidth + 5f, top - 6f, cut);
            batch.setColor(0.78f, 0.64f, 0.37f, 0.80f);
            PortraitUi.drawCutCorner(batch, x, bottom, x + panelWidth, top, cut);
            batch.setColor(0.060f, 0.088f, 0.120f, 0.96f);
            PortraitUi.drawCutCorner(batch, x + border, bottom + border, x + panelWidth - border, top - border,
                    Math.max(1f, cut - border * 0.6f));
            batch.setColor(0.96f, 0.82f, 0.48f, 0.24f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, x + cut, top - border * 2f,
                    panelWidth - cut * 2f, Math.max(2f, border * 0.8f));

            // Title
            float cursorY = top - padTop;
            String title = player ? ((AbstractPlayer) creature).getLocalizedCharacterName() : creature.name;
            float titleScale = titleCap / headingBaseCap;
            heading.getData().markupEnabled = false;
            heading.getData().setScale(headingX * titleScale, headingY * titleScale);
            FontHelper.renderFontLeftTopAligned(batch, heading, title, x + padX, cursorY, Settings.CREAM_COLOR);

            // Clean divider well below title descenders
            cursorY -= titleToDivider;
            batch.setColor(0.78f, 0.64f, 0.37f, 0.34f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, x + padX, cursorY, textWidth, 2f);
            cursorY -= dividerToBody;

            // Power tips (must scale against unmutated headingBaseCap, not post-title capHeight)
            float labelScale = labelCap / headingBaseCap;
            for (int i = 0; i < validTips.size(); i++) {
                PowerTip tip = validTips.get(i);
                if (i > 0) {
                    float sepY = cursorY - tipGap * 0.42f;
                    batch.setColor(0.56f, 0.66f, 0.74f, 0.20f);
                    batch.draw(ImageMaster.WHITE_SQUARE_IMG, x + padX, sepY, textWidth, 1.5f);
                    cursorY -= tipGap;
                }
                if (tip.header != null && !tip.header.isEmpty()) {
                    float icon = labelCap * 1.24f;
                    float tx = x + padX;
                    float iconBottom = cursorY - labelCap * 0.58f - icon * 0.5f;
                    batch.setColor(Color.WHITE);
                    if (tip.imgRegion != null) {
                        batch.draw(tip.imgRegion, tx, iconBottom, icon, icon);
                        tx += icon + width * 0.012f;
                    } else if (tip.img != null) {
                        batch.draw(tip.img, tx, iconBottom, icon, icon);
                        tx += icon + width * 0.012f;
                    }
                    heading.getData().markupEnabled = false;
                    heading.getData().setScale(headingX * labelScale, headingY * labelScale);
                    FontHelper.renderFontLeftTopAligned(batch, heading, tip.header, tx, cursorY, Settings.GOLD_COLOR);
                    cursorY -= labelToBody;
                }
                if (tip.body != null && !tip.body.isEmpty()) {
                    float bh = bodyHeights[i];
                    tipBodies.get(i).drawTop(batch, body, tip.body, Settings.CREAM_COLOR,
                            x + padX, cursorY, textWidth, Math.max(bh + 12f, bodyCap * 2f),
                            bodyCap, Align.left, AbstractDungeon.player);
                    cursorY -= bh + bodyCap * 0.20f;
                }
            }
        } finally {
            if (fitted) batch.setTransformMatrix(originalTransform);
            heading.getData().markupEnabled = headingMarkup;
            heading.getData().setScale(headingX, headingY);
            batch.setColor(packed);
        }
    }
}
