package sts1portrait;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
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
            AbstractPlayer player = AbstractDungeon.player;
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
        float x = PortraitViewport.x(0.12f);
        float panelWidth = width * 0.76f;
        float pad = width * 0.023f;
        float textWidth = panelWidth - 2f * pad;
        float rowGap = width * 0.012f;
        float titleHeight = width * 0.050f;
        float labelHeight = width * 0.036f;
        BitmapFont heading = FontHelper.tipHeaderFont;
        BitmapFont body = FontHelper.tipBodyFont;
        float headingX = heading.getData().scaleX;
        float headingY = heading.getData().scaleY;
        float bodyX = body.getData().scaleX;
        float bodyY = body.getData().scaleY;
        float packed = batch.getPackedColor();
        float hFactor = width * 0.030f / Math.max(1f, heading.getCapHeight());
        float bFactor = width * 0.025f / Math.max(1f, body.getCapHeight());
        heading.getData().setScale(headingX * hFactor, headingY * hFactor);
        body.getData().setScale(bodyX * bFactor, bodyY * bFactor);
        boolean fitted = false;
        try {
            float line = body.getLineHeight() * 1.03f;
            float panelHeight = 2f * pad + titleHeight;
            for (PowerTip tip : tips) {
                if (tip != null) panelHeight += labelHeight + line + paragraph(body, tip, textWidth, line) + rowGap;
            }
            float preferredTop = creature.healthHb.y - width * 0.027f;
            if (player) {
                preferredTop = PortraitViewport.y(0.72f);
            } else {
                for (AbstractMonster enemy : AbstractDungeon.getMonsters().monsters) {
                    if (!enemy.isDeadOrEscaped()) preferredTop = Math.min(preferredTop, enemy.healthHb.y - width * 0.027f);
                }
            }
            float upperLimit = PortraitViewport.y(0.86f);
            float lowerLimit = PortraitViewport.y(player ? 0.43f : 0.40f);
            float fit = Math.min(1f, (upperLimit - lowerLimit) / panelHeight);
            float top = Math.min(upperLimit, Math.max(preferredTop, lowerLimit + panelHeight * fit));
            float bottom = top - panelHeight;

            float center = x + panelWidth * 0.5f;
            float scale = fit;
            if (scale != 1f) {
                originalTransform.set(batch.getTransformMatrix());
                fittedTransform.set(originalTransform).translate(center, top, 0f)
                        .scale(scale, scale, 1f).translate(-center, -top, 0f);
                batch.setTransformMatrix(fittedTransform);
                fitted = true;
            }
            batch.setColor(0f, 0f, 0f, 0.46f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, x + 4, bottom - 5, panelWidth, panelHeight);
            batch.setColor(0.29f, 0.34f, 0.36f, 1f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, x, bottom, panelWidth, panelHeight);
            batch.setColor(0.084f, 0.105f, 0.13f, 1f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, x + 2, bottom + 2, panelWidth - 4, panelHeight - 4);
            batch.setColor(0.72f, 0.56f, 0.32f, 1f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, x, top - 2, panelWidth, 2);
            String title = player ? ((AbstractPlayer) creature).getLocalizedCharacterName() : creature.name;
            FontHelper.renderFontLeftTopAligned(batch, heading, title, x + pad, top - pad, Settings.CREAM_COLOR);
            float y = top - pad - titleHeight;
            batch.setColor(0.26f, 0.32f, 0.36f, 1f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, x + pad, y + rowGap * 0.35f, textWidth, 1);
            for (PowerTip tip : tips) {
                if (tip == null) continue;
                float icon = labelHeight * 0.79f;
                float tx = x + pad;
                batch.setColor(Color.WHITE);
                if (tip.imgRegion != null) {
                    batch.draw(tip.imgRegion, tx, y - icon, icon, icon);
                    tx += icon + rowGap * 0.5f;
                } else if (tip.img != null) {
                    batch.draw(tip.img, tx, y - icon, icon, icon);
                    tx += icon + rowGap * 0.5f;
                }
                if (tip.header != null) {
                    FontHelper.renderFontLeftTopAligned(batch, body, tip.header, tx, y, Settings.GOLD_COLOR);
                }
                y -= labelHeight;
                if (tip.body != null) {
                    FontHelper.renderSmartText(batch, body, tip.body, x + pad, y, textWidth, line, Settings.CREAM_COLOR);
                }
                y -= line + paragraph(body, tip, textWidth, line) + rowGap;
            }
        } finally {
            if (fitted) batch.setTransformMatrix(originalTransform);
            heading.getData().setScale(headingX, headingY);
            body.getData().setScale(bodyX, bodyY);
            batch.setColor(packed);
        }
    }

    private static float paragraph(BitmapFont body, PowerTip tip, float width, float line) {
        return tip.body == null ? 0f : Math.abs(FontHelper.getSmartHeight(body, tip.body, width, line));
    }
}
