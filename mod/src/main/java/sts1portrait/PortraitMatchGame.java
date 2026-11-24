package sts1portrait;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.events.shrines.GremlinMatchGame;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitMatchGame {
    private static final int COLS = 3;
    private static final int ROWS = 4;
    private static final GlyphLayout measure = new GlyphLayout();
    private static final PortraitRichText attemptsText = new PortraitRichText(true);

    private PortraitMatchGame() {}

    private static float headerCenterY() {
        float top = PortraitCombatHud.topBarActive()
                ? PortraitCombatHud.panelBottom()
                : PortraitViewport.y(0.88f);
        return top - PortraitViewport.contentHeight() * 0.044f;
    }

    private static float headerHeight() {
        return PortraitViewport.contentHeight() * 0.044f;
    }

    private static float gridTop() {
        return headerCenterY() - headerHeight() * 0.5f - PortraitViewport.contentHeight() * 0.022f;
    }

    private static float gridBottom() {
        return PortraitViewport.y(0.062f);
    }

    public static float baseCardScale() {
        float W = PortraitViewport.contentWidth();
        float H = Math.max(100f, gridTop() - gridBottom());
        float byW = W * 0.270f / Math.max(1f, AbstractCard.IMG_WIDTH);
        float byH = H / Math.max(1f, (ROWS + 0.36f) * AbstractCard.IMG_HEIGHT);
        return Math.min(byW, byH);
    }

    public static float mapScale(float vanillaScale) {
        if (!PortraitScreens.enabled()) return vanillaScale;
        float base = baseCardScale();
        if (vanillaScale <= 0.55f) return base;
        if (vanillaScale <= 0.80f) return base * 1.10f;
        return base * 1.16f;
    }

    @SpirePatch(clz = GremlinMatchGame.class, method = "placeCards", paramtypez = {})
    public static class PlaceCards {
        @SpirePostfixPatch
        public static void after(GremlinMatchGame __instance, CardGroup ___cards) {
            if (!PortraitScreens.enabled() || ___cards == null || ___cards.group == null) return;
            float scale = baseCardScale();
            float cardW = AbstractCard.IMG_WIDTH * scale;
            float cardH = AbstractCard.IMG_HEIGHT * scale;
            float W = PortraitViewport.contentWidth();
            float top = gridTop();
            float bottom = gridBottom();
            float gapX = Math.min(W * 0.036f, Math.max(8f, (W * 0.90f - COLS * cardW) / (COLS - 1)));
            float gapY = Math.min(cardH * 0.095f, Math.max(8f, ((top - bottom) - ROWS * cardH) / (ROWS - 1)));
            float totalW = COLS * cardW + (COLS - 1) * gapX;
            float totalH = ROWS * cardH + (ROWS - 1) * gapY;
            float startX = PortraitViewport.x(0.5f) - totalW * 0.5f + cardW * 0.5f;
            float startY = (top + bottom) * 0.5f + totalH * 0.5f - cardH * 0.5f;
            for (int i = 0; i < ___cards.group.size(); i++) {
                AbstractCard card = ___cards.group.get(i);
                int col = i % COLS;
                int row = i / COLS;
                card.target_x = startX + col * (cardW + gapX);
                card.target_y = startY - row * (cardH + gapY);
                card.drawScale = scale;
                card.targetDrawScale = scale;
                card.isFlipped = true;
            }
        }
    }

    @SpirePatch(clz = GremlinMatchGame.class, method = "updateMatchGameLogic", paramtypez = {})
    public static class CardScales {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isWriter() || !AbstractCard.class.getName().equals(field.getClassName())) return;
                    if ("drawScale".equals(field.getFieldName()) || "targetDrawScale".equals(field.getFieldName())) {
                        field.replace("{ $proceed(sts1portrait.PortraitMatchGame.mapScale($1)); }");
                    }
                }
            };
        }
    }

    public static void drawAttemptsBanner(SpriteBatch batch, String text) {
        if (text == null || text.isEmpty()) return;
        float cx = PortraitViewport.x(0.5f);
        float cy = headerCenterY();
        float h = headerHeight();
        BitmapFont font = PortraitFonts.header();
        float targetCap = Math.min(h * 0.44f, PortraitViewport.contentWidth() * 0.032f);
        String plain = text.replaceAll("#[rgbyp]", "");
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        boolean oldMarkup = font.getData().markupEnabled;
        float scale = targetCap / Math.max(1f, font.getCapHeight());
        font.getData().markupEnabled = false;
        font.getData().setScale(oldX * scale, oldY * scale);
        float measuredW;
        try {
            measure.setText(font, plain);
            measuredW = measure.width;
        } finally {
            font.getData().markupEnabled = oldMarkup;
            font.getData().setScale(oldX, oldY);
        }
        float packed = batch.getPackedColor();
        try {
            float w = Math.max(PortraitViewport.contentWidth() * 0.44f, measuredW + h * 1.10f);
            float left = cx - w * 0.5f;
            float right = cx + w * 0.5f;
            float bottom = cy - h * 0.5f;
            float top = cy + h * 0.5f;
            float cut = h * 0.24f;
            float border = Math.max(2f, h * 0.032f);
            batch.setColor(0.84f, 0.69f, 0.40f, 0.86f);
            PortraitUi.drawCutCorner(batch, left, bottom, right, top, cut);
            batch.setColor(0.058f, 0.088f, 0.122f, 0.94f);
            PortraitUi.drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                    Math.max(1f, cut - border * 0.6f));
            batch.setColor(0.98f, 0.85f, 0.52f, 0.22f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + cut, top - border * 2f,
                    w - cut * 2f, Math.max(2f, border * 0.8f));
            attemptsText.draw(batch, font, text, Settings.CREAM_COLOR,
                    left + h * 0.35f, cy, w - h * 0.70f, h * 0.78f, targetCap, Align.center);
        } finally {
            batch.setColor(packed);
        }
    }

    @SpirePatch(clz = GremlinMatchGame.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class RenderAttempts {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (FontHelper.class.getName().equals(call.getClassName())
                            && "renderSmartText".equals(call.getMethodName())) {
                        call.replace("{ if (sts1portrait.PortraitScreens.enabled())"
                                + " sts1portrait.PortraitMatchGame.drawAttemptsBanner($1, $3);"
                                + " else $proceed($$); }");
                    }
                }
            };
        }
    }
}
