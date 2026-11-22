package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen;
import com.megacrit.cardcrawl.ui.buttons.ConfirmButton;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import java.util.ArrayList;
import java.util.List;

public final class PortraitBossReward {
    private static final Color TILE = new Color(.09f, .14f, .18f, .96f);
    private static final Color HIGHLIGHT = new Color(.24f, .39f, .46f, 1f);
    private static final Color SKIP_FILL = new Color(.34f, .22f, .21f, 1f);
    private static final Color CONFIRM_FILL = new Color(.21f, .34f, .39f, 1f);

    private static final Bounds BOUNDS = new Bounds();
    private static final Matrix4 descriptionBase = new Matrix4();
    private static final Matrix4 descriptionZoom = new Matrix4();
    private static List<PowerTip> detailsSource;
    private static String detailsDescription;
    private static String detailsBody;

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

    private static final class Bounds {
        float x, y, width, height;

        void set(float xFraction, float yFraction, float wFraction, float hFraction) {
            float w = layoutWidth();
            x = PortraitViewport.x(xFraction);
            y = PortraitViewport.y(yFraction);
            width = w * wFraction;
            height = w * hFraction;
        }

        void slot(int index, int count) {
            set(.5f + (index - (count - 1) * .5f) * .30f, .56f, .27f, .28f);
        }

        void button(boolean confirm) {
            set(confirm ? .77f : .23f, .083f, .42f, .102f);
        }

        void hitbox(Hitbox hb) {
            if (hb.width == width && hb.height == height && hb.cX == x && hb.cY == y) return;
            hb.resize(width, height);
            hb.move(x, y);
        }

        void fill(SpriteBatch batch, Color color) {
            PortraitDrawing.rectangle(batch, x - width * .5f, y - height * .5f, width, height, color);
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
                                + " ? sts1portrait.PortraitViewport.y(0.68f) : $1); }");
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
            if (!active()) {
                detailsSource = null;
                detailsBody = null;
                return;
            }
            drawChoices(__instance, sb);
            MenuCancelButton skip = skip(__instance);
            if (!skip.isHidden) {
                drawButton(sb, false, skip.hb.hovered,
                        ReflectionHacks.getPrivate(skip, MenuCancelButton.class, "buttonText"));
            }
            ConfirmButton confirm = __instance.confirmButton;
            if (!ReflectionHacks.<Boolean>getPrivate(confirm, ConfirmButton.class, "isHidden")) {
                drawButton(sb, true, confirm.isHovered && !confirm.isDisabled,
                        ReflectionHacks.getPrivate(confirm, ConfirmButton.class, "buttonText"));
            }
            sb.setColor(Color.WHITE);
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
            drawSlot(batch, i, relics.size(), relic.name, relic.img, highlighted);
            if (selected == null && highlighted) selected = relic;
        }
        ArrayList<AbstractBlight> blights = blights(screen);
        for (int i = 0; i < blights.size(); i++) {
            AbstractBlight blight = blights.get(i);
            if (blight.isObtained) continue;
            boolean highlighted = blight == selectedBlight || blight.hb.hovered;
            drawSlot(batch, i, blights.size(), blight.name, blight.img, highlighted);
            if (selectedBlight == null && highlighted) selectedBlight = blight;
        }
        if (selected != null && !selected.isObtained) drawDetails(batch, selected.name, selected.description, selected.tips);
        else if (selectedBlight != null && !selectedBlight.isObtained)
            drawDetails(batch, selectedBlight.name, selectedBlight.description, selectedBlight.tips);
    }

    private static void drawSlot(SpriteBatch batch, int index, int count, String name, Texture texture, boolean selected) {
        BOUNDS.slot(index, count);
        BOUNDS.fill(batch, selected ? HIGHLIGHT : TILE);
        float icon = BOUNDS.width * .71f;
        if (texture != null) {
            float ratio = icon / Math.max(texture.getWidth(), texture.getHeight());
            float w = texture.getWidth() * ratio;
            float h = texture.getHeight() * ratio;
            batch.setColor(Color.WHITE);
            batch.draw(texture, BOUNDS.x - w * .5f, BOUNDS.y + BOUNDS.height * .11f - h * .5f, w, h);
        }
        PortraitSettings.label(batch, name, BOUNDS.x, BOUNDS.y - BOUNDS.height * .33f,
                BOUNDS.width * .88f, BOUNDS.width * .123f, true);
    }

    private static void drawDetails(SpriteBatch batch, String name, String description, List<PowerTip> tips) {
        if (detailsSource != tips || !description.equals(detailsDescription)) {
            detailsSource = tips;
            detailsDescription = description;
            StringBuilder body = new StringBuilder(description);
            for (int i = 1; i < tips.size(); i++) {
                PowerTip tip = tips.get(i);
                body.append(" NL NL ").append(tip.header).append(" NL ").append(tip.body);
            }
            detailsBody = body.toString();
        }
        float w = PortraitViewport.contentWidth() * .87f;
        float h = Math.min(w * (tips.size() > 1 ? .49f : .29f), PortraitViewport.contentHeight() * .23f);
        float x = PortraitViewport.x(.5f);
        float top = PortraitViewport.y(.56f) - layoutWidth() * .20f;

        PortraitSettings.frame(batch, x, top, w, h);
        PortraitSettings.label(batch, name, x, top - w * .055f, w * .86f, w * .037f, true);
        drawDescription(batch, x - w * .43f, top - w * .090f, w * .86f, h - w * .112f, w * .032f);
    }

    private static void drawDescription(SpriteBatch batch, float x, float top, float width, float height, float cap) {
        BitmapFont font = FontHelper.tipBodyFont;
        BitmapFont.BitmapFontData data = font.getData();
        float oldX = data.scaleX;
        float oldY = data.scaleY;
        boolean oldMarkup = data.markupEnabled;
        boolean oldIntegerPositions = font.usesIntegerPositions();
        float zoom = 2.5f;
        width /= zoom;
        height /= zoom;
        cap /= zoom;
        float scale = cap / Math.max(1f, font.getCapHeight());
        data.markupEnabled = Settings.lineBreakViaCharacter;
        font.setUseIntegerPositions(false);
        descriptionBase.set(batch.getTransformMatrix());
        descriptionZoom.set(descriptionBase).translate(x, top, 0f).scale(zoom, zoom, 1f);
        batch.setTransformMatrix(descriptionZoom);
        try {
            data.setScale(oldX * scale, oldY * scale);
            float line = font.getLineHeight() * 1.08f;
            // The game renderer draws energy tokens as the current character's orb.
            for (int i = 0; i < 4; i++) {
                float usedHeight = font.getCapHeight()
                        + Math.abs(FontHelper.getSmartHeight(font, detailsBody, width, line));
                if (usedHeight <= height) break;
                scale *= Math.min(.96f, height / usedHeight);
                data.setScale(oldX * scale, oldY * scale);
                line = font.getLineHeight() * 1.08f;
            }
            FontHelper.renderSmartText(batch, font, detailsBody, 0f, 0f, width, line, Settings.CREAM_COLOR);
        } finally {
            batch.setTransformMatrix(descriptionBase);
            font.setUseIntegerPositions(oldIntegerPositions);
            data.markupEnabled = oldMarkup;
            data.setScale(oldX, oldY);
        }
    }

    private static void drawButton(SpriteBatch batch, boolean confirm, boolean hovered, String text) {
        BOUNDS.button(confirm);
        BOUNDS.fill(batch, hovered ? HIGHLIGHT : confirm ? CONFIRM_FILL : SKIP_FILL);
        PortraitSettings.label(batch, text, BOUNDS.x, BOUNDS.y, BOUNDS.width * .88f, BOUNDS.height * .38f, true);
    }
}
