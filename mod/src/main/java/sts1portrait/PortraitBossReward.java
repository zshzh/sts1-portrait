package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen;
import com.megacrit.cardcrawl.ui.buttons.ConfirmButton;
import com.megacrit.cardcrawl.ui.buttons.DynamicBanner;
import java.util.ArrayList;
import java.util.List;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitBossReward {
    private static final Color KEYWORD_BODY = new Color(0.84f, 0.88f, 0.92f, 0.96f);
    private static final Color HINT_TEXT = new Color(0.76f, 0.82f, 0.88f, 0.88f);
    private static final Color SKIP_TEXT = new Color(0.98f, 0.84f, 0.80f, 1f);
    private static final Color CONFIRM_TEXT = new Color(1f, 0.97f, 0.90f, 1f);
    private static final Color DISABLED_TEXT = new Color(0.56f, 0.60f, 0.64f, 0.80f);

    private static final Bounds BOUNDS = new Bounds();
    private static final GlyphLayout measure = new GlyphLayout();
    private static final PortraitRichText mainDesc = new PortraitRichText(true);
    private static final ArrayList<PortraitRichText> tipTexts = new ArrayList<>();

    public static boolean active() {
        return PortraitScreens.enabled()
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD
                && !AbstractDungeon.topPanel.twitch.isPresent();
    }

    static boolean isSkip(Object button) {
        return button instanceof MenuCancelButton && active() && button == skip(AbstractDungeon.bossRelicScreen);
    }

    private static MenuCancelButton skip(BossRelicSelectScreen screen) {
        return ReflectionHacks.getPrivate(screen, BossRelicSelectScreen.class, "cancelButton");
    }

    private static ArrayList<AbstractRelic> relics(BossRelicSelectScreen screen) {
        return ReflectionHacks.getPrivate(screen, BossRelicSelectScreen.class, "relics");
    }

    private static ArrayList<AbstractBlight> blights(BossRelicSelectScreen screen) {
        return ReflectionHacks.getPrivate(screen, BossRelicSelectScreen.class, "blights");
    }

    private static float layoutWidth() {
        return Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth());
    }

    private static float headerY() {
        float top = PortraitCombatHud.topBarActive()
                ? PortraitCombatHud.panelBottom()
                : PortraitViewport.y(0.88f);
        return Math.min(PortraitViewport.y(0.79f), top - PortraitViewport.contentHeight() * 0.056f);
    }

    private static float slotCenterY() {
        return headerY() - layoutWidth() * 0.275f;
    }

    private static float slotHeight() {
        return layoutWidth() * 0.345f;
    }

    private static float buttonY() {
        return PortraitViewport.y(0.105f);
    }

    private static float buttonHeight() {
        return layoutWidth() * 0.112f;
    }

    private static final class Bounds {
        float x, y, width, height;

        void slot(int index, int count) {
            float w = layoutWidth();
            float step = count <= 3 ? 0.308f : Math.min(0.308f, 0.90f / Math.max(1, count));
            width = w * Math.min(0.284f, step * 0.92f);
            height = slotHeight();
            x = PortraitViewport.x(0.5f) + (index - (count - 1) * 0.5f) * w * step;
            y = slotCenterY();
        }

        void button(boolean confirm) {
            float w = layoutWidth();
            x = PortraitViewport.x(confirm ? 0.742f : 0.258f);
            y = buttonY();
            width = w * 0.435f;
            height = buttonHeight();
        }

        void hitbox(Hitbox hb) {
            if (hb.width == width && hb.height == height && hb.cX == x && hb.cY == y) return;
            hb.resize(width, height);
            hb.move(x, y);
        }
    }

    @SpirePatch(clz = DynamicBanner.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class HideVanillaBanner {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(DynamicBanner __instance, SpriteBatch sb) {
            return active() ? SpireReturn.Return() : SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = BossRelicSelectScreen.class, method = SpirePatch.STATICINITIALIZER)
    public static class BannerPosition {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isWriter() && "BANNER_Y".equals(field.getFieldName())) {
                        field.replace("{ $proceed(sts1portrait.PortraitViewport.active()"
                                + " ? sts1portrait.PortraitViewport.y(0.76f) : $1); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = BossRelicSelectScreen.class, method = "update", paramtypez = {})
    public static class Update {
        @SpirePrefixPatch
        public static void before(BossRelicSelectScreen __instance) {
            if (!active()) return;
            ArrayList<AbstractRelic> relics = relics(__instance);
            for (int i = 0; i < relics.size(); i++) {
                AbstractRelic relic = relics.get(i);
                if (relic.isObtained) continue;
                BOUNDS.slot(i, relics.size());
                BOUNDS.hitbox(relic.hb);
                relic.currentX = relic.targetX = BOUNDS.x;
                relic.currentY = relic.targetY = BOUNDS.y;
            }
            ArrayList<AbstractBlight> blights = blights(__instance);
            for (int i = 0; i < blights.size(); i++) {
                AbstractBlight blight = blights.get(i);
                if (blight.isObtained) continue;
                BOUNDS.slot(i, blights.size());
                BOUNDS.hitbox(blight.hb);
                blight.currentX = blight.targetX = BOUNDS.x;
                blight.currentY = blight.targetY = BOUNDS.y;
            }
            BOUNDS.button(false);
            BOUNDS.hitbox(skip(__instance).hb);
            BOUNDS.button(true);
            BOUNDS.hitbox(__instance.confirmButton.hb);
        }
    }

    @SpirePatch(clz = BossRelicSelectScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class Render {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    String owner = call.getClassName();
                    boolean smoke = owner.equals(SpriteBatch.class.getName()) && call.getMethodName().equals("draw")
                            && call.getSignature().equals("(Lcom/badlogic/gdx/graphics/Texture;FFFF)V");
                    boolean duplicate = call.getMethodName().equals("render")
                            && (owner.equals(AbstractRelic.class.getName()) || owner.equals(AbstractBlight.class.getName())
                            || owner.equals(ConfirmButton.class.getName()) || owner.equals(MenuCancelButton.class.getName())
                            || owner.equals("com.megacrit.cardcrawl.rewards.chests.AbstractChest")
                            || owner.equals("com.megacrit.cardcrawl.characters.AbstractPlayer"));
                    if (smoke || duplicate) call.replace("{ if (!sts1portrait.PortraitBossReward.active()) $proceed($$); }");
                }
            };
        }

        @SpirePostfixPatch
        public static void after(BossRelicSelectScreen __instance, SpriteBatch sb) {
            if (!active()) return;
            drawHeader(sb);
            drawChoices(__instance, sb);
            MenuCancelButton skip = skip(__instance);
            if (!skip.isHidden) {
                drawButton(sb, false, skip.hb.hovered, false,
                        ReflectionHacks.getPrivate(skip, MenuCancelButton.class, "buttonText"));
            }
            ConfirmButton confirm = __instance.confirmButton;
            boolean confirmHidden = ReflectionHacks.<Boolean>getPrivate(confirm, ConfirmButton.class, "isHidden");
            String confirmText = ReflectionHacks.getPrivate(confirm, ConfirmButton.class, "buttonText");
            if (!confirmHidden) {
                drawButton(sb, true, confirm.isHovered && !confirm.isDisabled, confirm.isDisabled, confirmText);
            } else {
                String fallback = confirmText != null && !confirmText.isEmpty() ? confirmText
                        : (Settings.language == Settings.GameLanguage.ZHS ? "确认" : "Confirm");
                drawButton(sb, true, false, true, fallback);
            }
            sb.setColor(Color.WHITE);
        }
    }

    private static void drawHeader(SpriteBatch batch) {
        String title = ReflectionHacks.getPrivateStatic(BossRelicSelectScreen.class, "SELECT_MSG");
        if (title == null || title.isEmpty()) {
            title = Settings.language == Settings.GameLanguage.ZHS ? "选择一样遗物" : "Choose a Relic";
        }
        float cx = PortraitViewport.x(0.5f);
        float cy = headerY();
        float W = layoutWidth();
        BitmapFont font = PortraitFonts.header();
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        boolean oldMarkup = font.getData().markupEnabled;
        float targetCap = W * 0.044f;
        font.getData().markupEnabled = false;
        measure.setText(font, title);
        float fit = Math.min(W * 0.62f / Math.max(1f, measure.width), targetCap / Math.max(1f, font.getCapHeight()));
        font.getData().setScale(oldX * fit, oldY * fit);
        float packed = batch.getPackedColor();
        try {
            measure.setText(font, title);
            float halfText = measure.width * 0.5f;
            float gap = W * 0.032f;
            float lineLeft = PortraitViewport.x(0.06f);
            float lineRight = PortraitViewport.x(0.94f);
            float lineH = Math.max(2f, W * 0.0024f);
            batch.setColor(0.84f, 0.69f, 0.40f, 0.52f);
            float leftLen = (cx - halfText - gap) - lineLeft;
            if (leftLen > 12f) {
                batch.draw(ImageMaster.WHITE_SQUARE_IMG, lineLeft, cy - lineH * 0.5f, leftLen, lineH);
                batch.draw(ImageMaster.WHITE_SQUARE_IMG, cx + halfText + gap, cy - lineH * 0.5f,
                        lineRight - (cx + halfText + gap), lineH);
            }
            FontHelper.renderFontCentered(batch, font, title, cx, cy, Settings.GOLD_COLOR);
        } finally {
            font.getData().markupEnabled = oldMarkup;
            font.getData().setScale(oldX, oldY);
            batch.setColor(packed);
        }
    }

    private static void drawChoices(BossRelicSelectScreen screen, SpriteBatch batch) {
        AbstractRelic selected = ReflectionHacks.getPrivate(screen, BossRelicSelectScreen.class, "touchRelic");
        AbstractBlight selectedBlight = ReflectionHacks.getPrivate(screen, BossRelicSelectScreen.class, "touchBlight");
        ArrayList<AbstractRelic> relics = relics(screen);
        for (int i = 0; i < relics.size(); i++) {
            AbstractRelic relic = relics.get(i);
            if (relic.isObtained) continue;
            boolean highlighted = relic == selected || relic.hb.hovered;
            drawSlot(batch, i, relics.size(), relic.name, relic.img, relic.outlineImg, highlighted);
            if (selected == null && highlighted) selected = relic;
        }
        ArrayList<AbstractBlight> blights = blights(screen);
        for (int i = 0; i < blights.size(); i++) {
            AbstractBlight blight = blights.get(i);
            if (blight.isObtained) continue;
            boolean highlighted = blight == selectedBlight || blight.hb.hovered;
            drawSlot(batch, i, blights.size(), blight.name, blight.img, blight.outlineImg, highlighted);
            if (selectedBlight == null && highlighted) selectedBlight = blight;
        }
        if (selected != null && !selected.isObtained) {
            drawDetails(batch, selected.name, selected.description, selected.tips);
        } else if (selectedBlight != null && !selectedBlight.isObtained) {
            drawDetails(batch, selectedBlight.name, selectedBlight.description, selectedBlight.tips);
        } else {
            drawEmptyHint(batch);
        }
    }

    private static void drawSlot(SpriteBatch batch, int index, int count, String name,
            Texture texture, Texture outline, boolean selected) {
        BOUNDS.slot(index, count);
        float left = BOUNDS.x - BOUNDS.width * 0.5f;
        float right = BOUNDS.x + BOUNDS.width * 0.5f;
        float bottom = BOUNDS.y - BOUNDS.height * 0.5f;
        float top = BOUNDS.y + BOUNDS.height * 0.5f;
        float cut = Math.min(BOUNDS.width * 0.085f, BOUNDS.height * 0.075f);
        float border = Math.max(2f, BOUNDS.width * (selected ? 0.011f : 0.008f));
        float packed = batch.getPackedColor();
        try {
            // Soft drop shadow
            batch.setColor(0f, 0f, 0f, selected ? 0.48f : 0.36f);
            PortraitUi.drawCutCorner(batch, left + 4f, bottom - 6f, right + 4f, top - 6f, cut);
            // Outer chamfered border
            if (selected) {
                batch.setColor(0.95f, 0.80f, 0.44f, 0.98f);
            } else {
                batch.setColor(0.52f, 0.62f, 0.70f, 0.64f);
            }
            PortraitUi.drawCutCorner(batch, left, bottom, right, top, cut);
            // Inner obsidian glass body
            if (selected) {
                batch.setColor(0.115f, 0.195f, 0.255f, 0.97f);
            } else {
                batch.setColor(0.060f, 0.090f, 0.124f, 0.94f);
            }
            PortraitUi.drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                    Math.max(1f, cut - border * 0.6f));
            // Warm top bevel highlight & bottom selection accent bar
            if (selected) {
                batch.setColor(1f, 0.88f, 0.56f, 0.28f);
                batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + cut, top - border * 2.2f,
                        BOUNDS.width - cut * 2f, Math.max(2f, border * 0.7f));
                float barW = BOUNDS.width * 0.46f;
                float barH = Math.max(4f, BOUNDS.height * 0.016f);
                batch.setColor(0.98f, 0.84f, 0.46f, 0.95f);
                batch.draw(ImageMaster.WHITE_SQUARE_IMG, BOUNDS.x - barW * 0.5f, bottom + border * 1.4f, barW, barH);
            }
            // Relic icon with silhouette glow / drop shadow
            float iconMax = BOUNDS.width * (selected ? 0.68f : 0.62f);
            float iconCy = BOUNDS.y + BOUNDS.height * (selected ? 0.115f : 0.095f);
            if (texture != null) {
                float ratio = iconMax / Math.max(1f, Math.max(texture.getWidth(), texture.getHeight()));
                float iw = texture.getWidth() * ratio;
                float ih = texture.getHeight() * ratio;
                if (outline != null) {
                    if (selected) {
                        batch.setColor(0.98f, 0.84f, 0.46f, 0.48f);
                        float gw = iw * 1.08f;
                        float gh = ih * 1.08f;
                        batch.draw(outline, BOUNDS.x - gw * 0.5f, iconCy - gh * 0.5f, gw, gh);
                    } else {
                        batch.setColor(0f, 0f, 0f, 0.42f);
                        batch.draw(outline, BOUNDS.x - iw * 0.5f + 3f, iconCy - ih * 0.5f - 4f, iw, ih);
                    }
                }
                batch.setColor(Color.WHITE);
                batch.draw(texture, BOUNDS.x - iw * 0.5f, iconCy - ih * 0.5f, iw, ih);
            }
            // Crisp relic title
            drawFittedLabel(batch, PortraitFonts.header(), name, BOUNDS.x, BOUNDS.y - BOUNDS.height * 0.305f,
                    BOUNDS.width * 0.88f, BOUNDS.width * 0.115f, selected ? Settings.GOLD_COLOR : Settings.CREAM_COLOR);
        } finally {
            batch.setColor(packed);
        }
    }

    private static void drawEmptyHint(SpriteBatch batch) {
        float W = PortraitViewport.contentWidth() * 0.885f;
        float H = layoutWidth() * 0.16f;
        float cx = PortraitViewport.x(0.5f);
        float top = slotCenterY() - slotHeight() * 0.5f - layoutWidth() * 0.048f;
        float left = cx - W * 0.5f;
        float right = cx + W * 0.5f;
        float bottom = top - H;
        float cut = Math.min(W * 0.024f, H * 0.22f);
        float border = Math.max(2f, W * 0.0024f);
        float packed = batch.getPackedColor();
        try {
            batch.setColor(0.52f, 0.62f, 0.70f, 0.52f);
            PortraitUi.drawCutCorner(batch, left, bottom, right, top, cut);
            batch.setColor(0.055f, 0.082f, 0.114f, 0.90f);
            PortraitUi.drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                    Math.max(1f, cut - border * 0.6f));
            String hint = Settings.language == Settings.GameLanguage.ZHS
                    ? "点击上方遗物查看效果"
                    : "Tap a relic above to inspect";
            drawFittedLabel(batch, PortraitFonts.body(), hint, cx, (top + bottom) * 0.5f,
                    W * 0.86f, layoutWidth() * 0.030f, HINT_TEXT);
        } finally {
            batch.setColor(packed);
        }
    }

    private static void drawDetails(SpriteBatch batch, String name, String description, List<PowerTip> tips) {
        float W = PortraitViewport.contentWidth() * 0.895f;
        float lw = layoutWidth();
        float padX = W * 0.056f;
        float padTop = lw * 0.042f;
        float padBot = lw * 0.040f;
        float textW = W - padX * 2f;
        float cx = PortraitViewport.x(0.5f);
        float top = slotCenterY() - slotHeight() * 0.5f - lw * 0.046f;
        float minBottom = buttonY() + buttonHeight() * 0.5f + lw * 0.046f;
        float maxH = Math.max(lw * 0.24f, top - minBottom);

        BitmapFont headerFont = PortraitFonts.header();
        BitmapFont bodyFont = PortraitFonts.body();
        float titleCap = lw * 0.037f;
        float bodyCap = lw * 0.029f;
        float tipHeaderCap = lw * 0.026f;
        float tipBodyCap = lw * 0.025f;
        float titleBand = titleCap * 1.45f;
        float divGap = lw * 0.026f;
        float tipGap = lw * 0.024f;

        int extraCount = tips != null && tips.size() > 1 ? tips.size() - 1 : 0;
        while (tipTexts.size() < extraCount) tipTexts.add(new PortraitRichText(true));

        float mainH = mainDesc.measureHeight(bodyFont, description, textW, bodyCap);
        float extraTotalH = 0f;
        float[] tipHeights = new float[extraCount];
        for (int i = 0; i < extraCount; i++) {
            PowerTip tip = tips.get(i + 1);
            float th = tip != null && tip.body != null
                    ? tipTexts.get(i).measureHeight(bodyFont, tip.body, textW, tipBodyCap) : 0f;
            tipHeights[i] = th;
            extraTotalH += tipGap + tipHeaderCap * 1.45f + th;
        }

        float neededH = padTop + titleBand + divGap + mainH + (extraCount > 0 ? divGap + extraTotalH : 0f) + padBot;
        float scaleDown = neededH > maxH ? (maxH / neededH) : 1f;
        if (scaleDown < 1f) {
            bodyCap *= scaleDown;
            tipHeaderCap *= scaleDown;
            tipBodyCap *= scaleDown;
            titleBand *= scaleDown;
            divGap *= scaleDown;
            tipGap *= scaleDown;
            mainH = mainDesc.measureHeight(bodyFont, description, textW, bodyCap);
            extraTotalH = 0f;
            for (int i = 0; i < extraCount; i++) {
                PowerTip tip = tips.get(i + 1);
                float th = tip != null && tip.body != null
                        ? tipTexts.get(i).measureHeight(bodyFont, tip.body, textW, tipBodyCap) : 0f;
                tipHeights[i] = th;
                extraTotalH += tipGap + tipHeaderCap * 1.45f + th;
            }
            neededH = padTop + titleBand + divGap + mainH + (extraCount > 0 ? divGap + extraTotalH : 0f) + padBot;
        }
        float H = Math.min(maxH, Math.max(lw * 0.23f, neededH));
        float left = cx - W * 0.5f;
        float right = cx + W * 0.5f;
        float bottom = top - H;
        float cut = Math.min(W * 0.026f, H * 0.085f);
        float border = Math.max(2f, W * 0.0026f);

        float packed = batch.getPackedColor();
        try {
            // Drop shadow + antique-gold chamfered frame + frosted obsidian glass
            batch.setColor(0f, 0f, 0f, 0.44f);
            PortraitUi.drawCutCorner(batch, left + 5f, bottom - 6f, right + 5f, top - 6f, cut);
            batch.setColor(0.82f, 0.67f, 0.39f, 0.80f);
            PortraitUi.drawCutCorner(batch, left, bottom, right, top, cut);
            batch.setColor(0.056f, 0.084f, 0.116f, 0.95f);
            PortraitUi.drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                    Math.max(1f, cut - border * 0.6f));
            batch.setColor(0.98f, 0.84f, 0.50f, 0.24f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + cut, top - border * 2f,
                    W - cut * 2f, Math.max(2f, border * 0.8f));

            // Title + hairline gold divider
            float cursorY = top - padTop;
            drawFittedLabel(batch, headerFont, name, cx, cursorY - titleCap * 0.45f,
                    textW, titleCap, Settings.GOLD_COLOR);
            cursorY -= titleBand;
            batch.setColor(0.82f, 0.67f, 0.39f, 0.34f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + padX, cursorY, textW, 2f);
            cursorY -= divGap;

            // Main description (crisp PortraitFonts.body + inline icons/colors/CJK wrapping)
            mainDesc.drawTop(batch, bodyFont, description, Settings.CREAM_COLOR,
                    left + padX, cursorY, textW, Math.max(mainH + 12f, bodyCap * 2f),
                    bodyCap, Align.left, AbstractDungeon.player);
            cursorY -= mainH;

            // Extra keyword tips (e.g. Curse / Exhaust)
            if (extraCount > 0) {
                cursorY -= divGap;
                batch.setColor(0.56f, 0.66f, 0.74f, 0.28f);
                batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + padX, cursorY, textW, 2f);
                for (int i = 0; i < extraCount; i++) {
                    PowerTip tip = tips.get(i + 1);
                    if (tip == null) continue;
                    cursorY -= tipGap;
                    if (tip.header != null && !tip.header.isEmpty()) {
                        drawLeftLabel(batch, headerFont, tip.header, left + padX, cursorY,
                                textW, tipHeaderCap, Settings.GOLD_COLOR);
                        cursorY -= tipHeaderCap * 1.45f;
                    }
                    if (tip.body != null && !tip.body.isEmpty()) {
                        tipTexts.get(i).drawTop(batch, bodyFont, tip.body, KEYWORD_BODY,
                                left + padX, cursorY, textW, Math.max(tipHeights[i] + 12f, tipBodyCap * 2f),
                                tipBodyCap, Align.left, AbstractDungeon.player);
                        cursorY -= tipHeights[i];
                    }
                }
            }
        } finally {
            batch.setColor(packed);
        }
    }

    private static void drawButton(SpriteBatch batch, boolean confirm, boolean hovered, boolean disabled, String text) {
        BOUNDS.button(confirm);
        float left = BOUNDS.x - BOUNDS.width * 0.5f;
        float right = BOUNDS.x + BOUNDS.width * 0.5f;
        float bottom = BOUNDS.y - BOUNDS.height * 0.5f;
        float top = BOUNDS.y + BOUNDS.height * 0.5f;
        float cut = Math.min(BOUNDS.height * 0.24f, BOUNDS.width * 0.06f);
        float border = Math.max(2f, BOUNDS.height * 0.028f);
        float packed = batch.getPackedColor();
        try {
            if (disabled) {
                batch.setColor(0.36f, 0.42f, 0.46f, 0.48f);
                PortraitUi.drawCutCorner(batch, left, bottom, right, top, cut);
                batch.setColor(0.055f, 0.072f, 0.090f, 0.80f);
                PortraitUi.drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
            } else if (confirm) {
                batch.setColor(hovered ? 1f : 0.92f, hovered ? 0.88f : 0.77f, hovered ? 0.56f : 0.42f, 0.98f);
                PortraitUi.drawCutCorner(batch, left, bottom, right, top, cut);
                batch.setColor(hovered ? 0.18f : 0.12f, hovered ? 0.38f : 0.28f, hovered ? 0.42f : 0.32f, 0.96f);
                PortraitUi.drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
                batch.setColor(1f, 0.88f, 0.56f, hovered ? 0.28f : 0.18f);
                batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + cut, top - border * 2.1f,
                        BOUNDS.width - cut * 2f, Math.max(2f, border * 0.75f));
            } else {
                batch.setColor(hovered ? 0.94f : 0.74f, hovered ? 0.44f : 0.34f, hovered ? 0.40f : 0.32f,
                        hovered ? 0.96f : 0.84f);
                PortraitUi.drawCutCorner(batch, left, bottom, right, top, cut);
                batch.setColor(hovered ? 0.26f : 0.15f, hovered ? 0.10f : 0.068f, hovered ? 0.11f : 0.078f, 0.94f);
                PortraitUi.drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
            }
            Color labelColor = disabled ? DISABLED_TEXT
                    : (hovered ? Settings.GOLD_COLOR : (confirm ? CONFIRM_TEXT : SKIP_TEXT));
            drawFittedLabel(batch, PortraitFonts.header(), text, BOUNDS.x, BOUNDS.y,
                    BOUNDS.width * 0.84f, BOUNDS.height * 0.38f, labelColor);
        } finally {
            batch.setColor(packed);
        }
    }

    private static void drawFittedLabel(SpriteBatch batch, BitmapFont font, String text,
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

    private static void drawLeftLabel(SpriteBatch batch, BitmapFont font, String text,
            float x, float topY, float maxWidth, float targetCap, Color color) {
        if (text == null || text.isEmpty()) return;
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        boolean oldMarkup = font.getData().markupEnabled;
        font.getData().markupEnabled = false;
        measure.setText(font, text);
        float fit = Math.min(maxWidth / Math.max(1f, measure.width), targetCap / Math.max(1f, font.getCapHeight()));
        font.getData().setScale(oldX * fit, oldY * fit);
        try {
            FontHelper.renderFontLeftTopAligned(batch, font, text, x, topY, color);
        } finally {
            font.getData().markupEnabled = oldMarkup;
            font.getData().setScale(oldX, oldY);
        }
    }
}
