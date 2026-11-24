package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.esotericsoftware.spine.Skeleton;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.AnimatedNpc;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.events.beyond.SpireHeart;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.VictoryRoom;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.ui.DialogWord;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import com.megacrit.cardcrawl.vfx.DamageHeartEffect;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

public final class PortraitEvents {
    private static final float BODY_SCALE = 2.45f;
    private static final float ROOM_STORY_SCALE = 2.35f;

    private static final Matrix4 roomStoryBase = new Matrix4();
    private static final Matrix4 roomStoryText = new Matrix4();
    private static final GlyphLayout titleMeasure = new GlyphLayout();
    private static boolean drawingRoomStory;
    private static LargeDialogOptionButton eventOption;
    private static boolean roomOption;
    private static boolean drawingWord;
    private static boolean lastMeasuredClosingPunct;
    private static boolean currentDialogHasImage;
    private static final Rectangle bodyClip = new Rectangle();
    private static final Matrix4 bodyBase = new Matrix4();
    private static final Matrix4 bodyLayout = new Matrix4();
    private static boolean drawingBody, bodyClipped, draggingBody;
    private static int bodyOptions;
    private static float bodyScroll;
    private static int dragY;
    private static ArrayList<DialogWord> bodyWords;
    private static float lineSpacing;
    private static final Color SCROLL_THUMB = new Color(0.84f, 0.70f, 0.42f, 0.78f);

    private PortraitEvents() {}

    // --- Card & Banner Geometry ---

    private static float cardLeft() { return PortraitViewport.x(0.042f); }
    private static float cardRight() { return PortraitViewport.x(0.958f); }
    private static float cardWidth() { return cardRight() - cardLeft(); }
    private static float cardBottom() { return PortraitViewport.y(0.068f); }

    private static float cardTop() {
        float maxTop = PortraitViewport.y(0.855f);
        if (PortraitCombatHud.topBarActive()) {
            maxTop = Math.min(maxTop, PortraitCombatHud.panelBottom() - PortraitViewport.contentHeight() * 0.014f);
        }
        return Math.max(cardBottom() + PortraitViewport.contentHeight() * 0.50f, maxTop);
    }

    private static float titleCenterY() {
        return cardTop() - PortraitViewport.contentHeight() * 0.031f;
    }

    private static float titleDividerY() {
        return cardTop() - PortraitViewport.contentHeight() * 0.058f;
    }

    private static float artSize() {
        return Math.min(cardWidth() * 0.56f, PortraitViewport.contentHeight() * 0.235f);
    }

    private static float artTop() {
        return titleDividerY() - PortraitViewport.contentHeight() * 0.018f;
    }

    private static float artBottom() {
        return artTop() - artSize();
    }

    // --- SpireHeart & Room Event Story Banner ---

    public static float heartX() { return PortraitViewport.x(.66f); }

    private static float heartFloor() { return PortraitViewport.y(.385f); }

    private static float heartY() { return heartFloor() - PortraitViewport.referenceWidth() * .065f; }

    public static float heartImpactY() { return heartY() + 320f * Settings.scale; }

    public static boolean heartSceneActive() {
        return PortraitViewport.active() && AbstractDungeon.currMapNode != null
                && AbstractDungeon.getCurrRoom() != null && AbstractDungeon.getCurrRoom().event instanceof SpireHeart;
    }

    public static boolean roomStoryActive() {
        return PortraitScreens.enabled() && AbstractDungeon.currMapNode != null
                && AbstractDungeon.getCurrRoom() != null && AbstractDungeon.getCurrRoom().event != null;
    }

    private static float roomStoryScale() {
        return ROOM_STORY_SCALE * Math.min(1f, PortraitViewport.contentWidth() / PortraitViewport.referenceWidth());
    }

    private static float roomBannerTop() {
        float maxTop = PortraitViewport.y(0.850f);
        if (PortraitCombatHud.topBarActive()) {
            maxTop = Math.min(maxTop, PortraitCombatHud.panelBottom() - PortraitViewport.contentHeight() * 0.018f);
        }
        return maxTop;
    }

    private static float roomBannerHeight(RoomEventDialog dialog) {
        float minH = PortraitViewport.contentHeight() * 0.125f;
        float maxH = PortraitViewport.contentHeight() * 0.285f;
        if (dialog == null) return minH;
        ArrayList<DialogWord> words = ReflectionHacks.getPrivate(dialog, RoomEventDialog.class, "words");
        if (words == null || words.isEmpty()) return minH;
        int lines = words.get(words.size() - 1).line + 1;
        float lineH = 34f * Settings.scale * roomStoryScale();
        float pad = PortraitViewport.contentHeight() * 0.036f;
        return MathUtils.clamp(lines * lineH + pad, minH, maxH);
    }

    private static float roomStoryBaseline() {
        return roomBannerTop() - PortraitViewport.contentHeight() * 0.036f;
    }

    public static float roomStoryWrapWidth() {
        return PortraitViewport.referenceWidth() * 0.80f / ROOM_STORY_SCALE;
    }

    @SpirePatch(clz = AbstractEvent.class, method = "renderRoomEventPanel", paramtypez = {SpriteBatch.class})
    public static class RoomStoryBackground {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(AbstractEvent __instance, SpriteBatch sb, float ___panelAlpha) {
            if (!roomStoryActive()) return SpireReturn.Continue();
            if (___panelAlpha <= 0.01f) return SpireReturn.Return();
            float left = PortraitViewport.x(0.045f);
            float right = PortraitViewport.x(0.955f);
            float top = roomBannerTop();
            float height = roomBannerHeight(__instance.roomEventText);
            float bottom = top - height;
            float cut = Math.min(height * 0.08f, (right - left) * 0.025f);
            float border = Math.max(2f, (right - left) * 0.0025f);
            float alpha = MathUtils.clamp(___panelAlpha * 2.0f, 0f, 1f);
            float packed = sb.getPackedColor();
            try {
                sb.setColor(0.80f, 0.65f, 0.38f, 0.78f * alpha);
                PortraitUi.drawCutCorner(sb, left, bottom, right, top, cut);
                sb.setColor(0.058f, 0.086f, 0.118f, 0.95f * alpha);
                PortraitUi.drawCutCorner(sb, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
                sb.setColor(0.96f, 0.82f, 0.50f, 0.22f * alpha);
                sb.draw(ImageMaster.WHITE_SQUARE_IMG, left + cut, top - border * 2f,
                        (right - left) - cut * 2f, Math.max(2f, border * 0.8f));
            } finally {
                sb.setColor(packed);
            }
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = RoomEventDialog.class, method = "bodyTextEffect", paramtypez = {})
    @SpirePatch(clz = RoomEventDialog.class, method = "bodyTextEffectCN", paramtypez = {})
    public static class RoomStoryWrap {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && RoomEventDialog.class.getName().equals(field.getClassName())
                            && "DIALOG_MSG_W".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitEvents.roomStoryActive()"
                                + " ? sts1portrait.PortraitEvents.roomStoryWrapWidth() : $proceed(); }");
                    }
                }
            };
        }
    }

    public static void renderRoomWord(DialogWord word, SpriteBatch batch, float offsetY) {
        if (!drawingRoomStory && roomStoryActive()) {
            drawingRoomStory = true;
            float scale = roomStoryScale();
            roomStoryBase.set(batch.getTransformMatrix());
            // Match RoomEventDialog's native starting position, including its render-time offset.
            float originalX = PortraitViewport.referenceCanvasWidth() * 0.1f;
            float originalY = offsetY + 250f * Settings.scale;
            roomStoryText.set(roomStoryBase)
                    .translate(PortraitViewport.x(0.095f) - originalX * scale,
                            roomStoryBaseline() - originalY * scale, 0f)
                    .scale(scale, scale, 1f);
            batch.setTransformMatrix(roomStoryText);
        }
        word.render(batch, offsetY);
    }

    private static void endRoomStory(SpriteBatch batch) {
        if (drawingRoomStory) {
            batch.setTransformMatrix(roomStoryBase);
            drawingRoomStory = false;
        }
    }

    @SpirePatch(clz = RoomEventDialog.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class RoomStoryDrawing {
        @SpirePostfixPatch
        public static void after(RoomEventDialog __instance, SpriteBatch batch) {
            endRoomStory(batch);
        }
    }

    @SpirePatch(clz = AbstractDungeon.class, method = "nextRoomTransition", paramtypez = {SaveFile.class})
    public static class HeartTransition {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (AbstractPlayer.class.getName().equals(call.getClassName()) && "movePosition".equals(call.getMethodName())) {
                        call.replace("{ if (sts1portrait.PortraitEvents.heartSceneActive())"
                                + " sts1portrait.PortraitEvents.placeHeartPlayer($0); else $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = VictoryRoom.class, method = "update", paramtypez = {})
    public static class HeartPlayerPosition {
        @SpirePrefixPatch
        public static void before(VictoryRoom __instance) {
            if (PortraitViewport.active() && __instance.event instanceof SpireHeart && AbstractDungeon.player != null) {
                placeHeartPlayer(AbstractDungeon.player);
            }
        }
    }

    public static void placeHeartPlayer(AbstractPlayer player) {
        float x = PortraitViewport.x(.26f);
        float y = heartFloor();
        if (player.drawX != x || player.drawY != y) player.movePosition(x, y);
    }

    public static void renderHeart(AnimatedNpc npc, SpriteBatch batch) {
        if (!PortraitViewport.active()) {
            npc.render(batch);
            return;
        }
        Skeleton skeleton = npc.skeleton;
        float oldX = skeleton.getX();
        float oldY = skeleton.getY();
        float originalFloor = AbstractDungeon.floorY - 80f * Settings.scale;
        float rise = MathUtils.clamp((oldY - originalFloor) / Math.max(1f, Settings.HEIGHT - originalFloor), 0f, 1f);
        skeleton.setPosition(heartX(), MathUtils.lerp(heartY(), Settings.HEIGHT, rise));
        try {
            npc.render(batch);
        } finally {
            skeleton.setPosition(oldX, oldY);
        }
    }

    @SpirePatch(clz = SpireHeart.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = SpireHeart.class, method = "buttonEffect", paramtypez = {int.class})
    public static class HeartScene {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (AnimatedNpc.class.getName().equals(call.getClassName()) && "render".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitEvents.renderHeart($0, $1); }");
                    }
                }

                @Override public void edit(NewExpr expr) throws CannotCompileException {
                    if (DamageHeartEffect.class.getName().equals(expr.getClassName())) {
                        expr.replace("{ if (sts1portrait.PortraitViewport.active()) {"
                                + " $2 = sts1portrait.PortraitEvents.heartX();"
                                + " $3 = sts1portrait.PortraitEvents.heartImpactY(); } $_ = $proceed($$); }");
                    }
                }
            };
        }
    }

    // --- Unified Event Option Stack (Illustrated Events + Room Events + Neow + Heart) ---

    private static float optionMetricsScale(int count, boolean room) {
        int n = Math.max(1, count);
        float H = PortraitViewport.contentHeight();
        float idealH = H * 0.056f;
        float idealGap = H * 0.011f;
        float total = n * idealH + (n - 1) * idealGap;
        float maxStack = H * (room ? 0.36f : 0.34f);
        return total > maxStack && total > 1f ? maxStack / total : 1f;
    }

    private static float optionHeight(int count, boolean room) {
        return PortraitViewport.contentHeight() * 0.056f * optionMetricsScale(count, room);
    }

    private static float optionGap(int count, boolean room) {
        return PortraitViewport.contentHeight() * 0.011f * optionMetricsScale(count, room);
    }

    private static float optionsBottom(boolean room, boolean heart) {
        if (heart) return PortraitViewport.y(0.145f);
        if (room) return PortraitViewport.y(0.088f);
        return cardBottom() + PortraitViewport.contentHeight() * 0.022f;
    }

    private static float optionsTop(int count, boolean room) {
        int n = Math.max(0, count);
        if (n == 0) return optionsBottom(room, false);
        float h = optionHeight(n, room);
        float gap = optionGap(n, room);
        return optionsBottom(room, room && heartSceneActive()) + n * h + (n - 1) * gap;
    }

    private static void place(LargeDialogOptionButton option, int count, boolean room) {
        int n = Math.max(1, count);
        int slot = Math.max(0, Math.min(n - 1, option.slot));
        boolean heart = room && heartSceneActive();
        float h = optionHeight(n, room);
        float gap = optionGap(n, room);
        float step = h + gap;
        float bottom = optionsBottom(room, heart);
        Hitbox area = option.hb;
        area.width = PortraitViewport.contentWidth() * (heart ? 0.74f : 0.855f);
        area.height = h;
        area.move(PortraitViewport.x(0.50f), bottom + h * 0.5f + (n - 1 - slot) * step);
    }

    public static void updateOption(LargeDialogOptionButton option, int count, boolean room) {
        eventOption = PortraitScreens.enabled() ? option : null;
        roomOption = room;
        try {
            option.update(count);
        } finally {
            eventOption = null;
        }
    }

    @SpirePatch(clz = LargeDialogOptionButton.class, method = "calculateY", paramtypez = {int.class})
    public static class OptionHitbox {
        @SpirePostfixPatch
        public static void after(LargeDialogOptionButton __instance, int count) {
            if (eventOption == __instance) {
                place(__instance, roomOption ? RoomEventDialog.optionList.size() : count, roomOption);
            }
        }
    }

    public static void renderOption(LargeDialogOptionButton option, SpriteBatch batch, int count, boolean room) {
        if (room) endRoomStory(batch);
        else endBody(batch);
        if (PortraitScreens.enabled()) {
            place(option, count, room);
            PortraitUi.drawOption(batch, option, count);
        } else {
            option.render(batch);
        }
    }

    @SpirePatch(clz = LargeDialogOptionButton.class, method = "renderCardPreview", paramtypez = {SpriteBatch.class})
    public static class PortraitCardPreview {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(LargeDialogOptionButton __instance, SpriteBatch sb,
                AbstractCard ___cardToPreview) {
            if (!PortraitScreens.enabled() || ___cardToPreview == null || !__instance.hb.hovered) {
                return SpireReturn.Continue();
            }
            float scale = Math.min(1.0f, PortraitViewport.contentWidth() * 0.44f / AbstractCard.IMG_WIDTH);
            float cardH = AbstractCard.IMG_HEIGHT * scale;
            ___cardToPreview.drawScale = scale;
            ___cardToPreview.current_x = PortraitViewport.x(0.50f);
            ___cardToPreview.current_y = Math.min(cardTop() - cardH * 0.55f,
                    __instance.hb.y + __instance.hb.height + cardH * 0.55f);
            ___cardToPreview.render(sb);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = LargeDialogOptionButton.class, method = "renderRelicPreview", paramtypez = {SpriteBatch.class})
    public static class PortraitRelicPreview {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(LargeDialogOptionButton __instance, SpriteBatch sb,
                AbstractRelic ___relicToPreview) {
            if (!PortraitScreens.enabled() || Settings.isControllerMode
                    || ___relicToPreview == null || !__instance.hb.hovered) {
                return SpireReturn.Continue();
            }
            float tipY = Math.min(cardTop() - PortraitViewport.contentHeight() * 0.05f,
                    __instance.hb.y + __instance.hb.height + PortraitViewport.contentHeight() * 0.14f);
            TipHelper.queuePowerTips(PortraitViewport.x(0.08f), tipY, ___relicToPreview.tips);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = RoomEventDialog.class, method = "update", paramtypez = {})
    @SpirePatch(clz = RoomEventDialog.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class RoomDialog {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (DialogWord.class.getName().equals(call.getClassName()) && "render".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitEvents.renderRoomWord($0, $$); }");
                        return;
                    }
                    if (!LargeDialogOptionButton.class.getName().equals(call.getClassName())) return;
                    if ("update".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitEvents.updateOption($0, $1, true); }");
                    } else if ("render".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitEvents.renderOption($0, $1, optionList.size(), true); }");
                    }
                }
            };
        }
    }

    // --- Illustrated Event Body Text Layout & Scrolling ---

    private static final String CLOSING_PUNCT = "。，！？、；：”’）】》.,!?;:";

    public static void measureWord(GlyphLayout layout, BitmapFont font, CharSequence word) {
        lastMeasuredClosingPunct = word != null && word.length() == 1 && CLOSING_PUNCT.indexOf(word.charAt(0)) >= 0;
        if (!PortraitScreens.enabled()) {
            layout.setText(font, word);
            return;
        }
        float scaleX = font.getData().scaleX;
        float scaleY = font.getData().scaleY;
        font.getData().setScale(scaleX * BODY_SCALE, scaleY * BODY_SCALE);
        try {
            layout.setText(font, word);
        } finally {
            font.getData().setScale(scaleX, scaleY);
        }
    }

    public static float nativeBodyX() {
        return PortraitViewport.referenceX(0.088f);
    }

    public static float nativeBodyY() {
        return PortraitViewport.referenceHeight() * 0.649f;
    }

    public static float nativeBodyWidth() {
        // Optical hanging punctuation: allow closing CJK/Latin punctuation to hang slightly into the right padding
        // so an orphan period or comma never wraps onto a line by itself.
        return PortraitViewport.referenceWidth() * (lastMeasuredClosingPunct ? 0.858f : 0.808f);
    }

    private static float bodyBottom(int options) {
        return optionsTop(options, false) + PortraitViewport.contentHeight() * 0.020f;
    }

    private static float bodyTop() {
        if (currentDialogHasImage) {
            return artBottom() - PortraitViewport.contentHeight() * 0.016f;
        }
        return titleDividerY() - PortraitViewport.contentHeight() * 0.022f;
    }

    private static float bodyTopPadding() {
        return Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth()) * 0.044f;
    }

    private static float bodyFit() {
        return Math.min(1f, PortraitViewport.contentWidth() / PortraitViewport.referenceWidth());
    }

    private static float scrollLimit(int options) {
        if (bodyWords == null) {
            bodyWords = ReflectionHacks.getPrivateStatic(GenericEventDialog.class, "words");
            lineSpacing = ReflectionHacks.<Float>getPrivateStatic(GenericEventDialog.class, "LINE_SPACING") * BODY_SCALE;
        }
        if (bodyWords == null || bodyWords.isEmpty()) return 0f;
        float height = (bodyWords.get(bodyWords.size() - 1).line + 1) * lineSpacing * bodyFit();
        return Math.max(0f, height + bodyTopPadding() - (bodyTop() - bodyBottom(options)));
    }

    @SpirePatch(clz = GenericEventDialog.class, method = "updateBodyText",
            paramtypez = {String.class, DialogWord.AppearEffect.class})
    public static class NewBody {
        @SpirePostfixPatch
        public static void after(GenericEventDialog __instance) {
            bodyScroll = 0f;
            draggingBody = false;
        }
    }

    @SpirePatch(clz = GenericEventDialog.class, method = "update", paramtypez = {})
    public static class BodyInput {
        @SpirePrefixPatch
        public static void before(GenericEventDialog __instance, boolean ___show, Texture ___img) {
            if (!PortraitScreens.enabled() || !___show) return;
            currentDialogHasImage = ___img != null;
            int options = __instance.optionList.size();
            float limit = scrollLimit(options);
            boolean inside = InputHelper.mX >= PortraitViewport.x(0.08f) && InputHelper.mX <= PortraitViewport.x(0.92f)
                    && InputHelper.mY >= bodyBottom(options) && InputHelper.mY <= bodyTop();
            if (inside && limit > 0f) {
                if (InputHelper.scrolledDown) bodyScroll += lineSpacing * 2f;
                if (InputHelper.scrolledUp) bodyScroll -= lineSpacing * 2f;
                if (InputHelper.justClickedLeft) {
                    draggingBody = true;
                    dragY = InputHelper.mY;
                }
            }
            if (draggingBody) {
                if (InputHelper.isMouseDown) {
                    bodyScroll += InputHelper.mY - dragY;
                    dragY = InputHelper.mY;
                } else {
                    InputHelper.justReleasedClickLeft = false;
                    draggingBody = false;
                }
            }
            bodyScroll = MathUtils.clamp(bodyScroll, 0f, limit);
        }
    }

    public static void renderWord(DialogWord word, SpriteBatch batch, int options) {
        if (!PortraitScreens.enabled()) {
            word.render(batch);
            return;
        }
        if (!drawingBody) {
            drawingBody = true;
            bodyOptions = options;
            float bottom = bodyBottom(options);
            float top = bodyTop();
            if (top <= bottom + 4f) {
                bodyClipped = false;
                return;
            }
            batch.flush();
            bodyClip.set(PortraitViewport.x(0.082f), bottom, PortraitViewport.contentWidth() * 0.836f, top - bottom);
            PortraitDesktopViewport.clip(bodyClip);
            bodyClipped = ScissorStack.pushScissors(bodyClip);
            if (bodyClipped) {
                float fit = bodyFit();
                float scroll = Math.min(bodyScroll, scrollLimit(options));
                bodyBase.set(batch.getTransformMatrix());
                bodyLayout.set(bodyBase).translate(PortraitViewport.x(0.088f), top - bodyTopPadding() + scroll, 0f)
                        .scale(fit, fit, 1f).translate(-nativeBodyX(), -nativeBodyY(), 0f);
                batch.setTransformMatrix(bodyLayout);
            }
        }
        if (!bodyClipped) return;
        drawingWord = true;
        try {
            word.render(batch);
        } finally {
            drawingWord = false;
        }
    }

    private static void endBody(SpriteBatch batch) {
        if (!drawingBody) return;
        drawingBody = false;
        if (bodyClipped) {
            batch.flush();
            ScissorStack.popScissors();
            batch.setTransformMatrix(bodyBase);
            bodyClipped = false;
            drawScrollThumb(batch);
        }
    }

    private static void drawScrollThumb(SpriteBatch batch) {
        float limit = scrollLimit(bodyOptions);
        if (limit <= 0f) return;
        float bottom = bodyBottom(bodyOptions);
        float height = bodyTop() - bottom;
        float thumb = Math.max(PortraitViewport.contentHeight() * 0.03f, height * height / (height + limit));
        float progress = MathUtils.clamp(bodyScroll / limit, 0f, 1f);
        float previousColor = batch.getPackedColor();
        PortraitDrawing.rectangle(batch, PortraitViewport.x(0.922f), bottom + (1f - progress) * (height - thumb),
                Math.max(4f, PortraitViewport.referenceWidth() * 0.0055f), thumb, SCROLL_THUMB);
        batch.setColor(previousColor);
    }

    @SpirePatch(clz = GenericEventDialog.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class BodyRendering {
        @SpirePrefixPatch
        public static void before(GenericEventDialog __instance, SpriteBatch batch, Texture ___img) {
            if (PortraitScreens.enabled()) {
                currentDialogHasImage = ___img != null;
            }
        }

        @SpirePostfixPatch
        public static void after(GenericEventDialog __instance, SpriteBatch batch) {
            endBody(batch);
        }
    }

    public static float bodySpacing(float spacing) {
        return PortraitScreens.enabled() ? spacing * BODY_SCALE : spacing;
    }

    public static float wordScale(float scale) {
        return drawingWord ? scale * BODY_SCALE : scale;
    }

    @SpirePatch(clz = DialogWord.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class EventWordDrawing {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                private boolean firstScale;

                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!firstScale && BitmapFont.BitmapFontData.class.getName().equals(call.getClassName())
                            && "setScale".equals(call.getMethodName())) {
                        firstScale = true;
                        call.replace("{ $proceed(sts1portrait.PortraitEvents.wordScale($1)); }");
                    }
                }
            };
        }
    }

    @SpirePatch(cls = "basemod.patches.whatmod.Events", method = "Postfix",
            paramtypez = {AbstractEvent.class, SpriteBatch.class}, optional = true)
    public static class EventSource {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (!"basemod.patches.whatmod.WhatMod".equals(call.getClassName())
                            || !"renderModTooltip".equals(call.getMethodName())) return;
                    call.replace("{ if (sts1portrait.PortraitScreens.enabled()) {"
                            + " $3 = sts1portrait.PortraitViewport.x(.045f);"
                            + " $4 = sts1portrait.PortraitViewport.y(.046f);"
                            + " } $proceed($$); }");
                }
            };
        }
    }

    // --- Illustrated Event Chamfered Card, Artwork Frame, and Title ---

    public static void drawEventTitle(SpriteBatch batch, BitmapFont font, String title, Color titleColor) {
        if (title == null || title.isEmpty()) return;
        float alpha = titleColor != null ? titleColor.a : 1f;
        if (alpha <= 0.01f) return;
        BitmapFont useFont = FontHelper.buttonLabelFont;
        float oldX = useFont.getData().scaleX;
        float oldY = useFont.getData().scaleY;
        float maxW = cardWidth() * 0.86f;
        float targetCap = PortraitViewport.contentHeight() * 0.023f;
        titleMeasure.setText(useFont, title);
        float fit = Math.min(maxW / Math.max(1f, titleMeasure.width),
                targetCap / Math.max(1f, useFont.getCapHeight()));
        useFont.getData().setScale(oldX * fit, oldY * fit);
        float packed = batch.getPackedColor();
        try {
            Color gold = new Color(Settings.GOLD_COLOR.r, Settings.GOLD_COLOR.g, Settings.GOLD_COLOR.b, alpha);
            FontHelper.renderFontCentered(batch, useFont, title, PortraitViewport.x(0.50f), titleCenterY(), gold);
        } finally {
            useFont.getData().setScale(oldX, oldY);
            batch.setColor(packed);
        }
    }

    public static void drawEventPanel(SpriteBatch batch, GenericEventDialog dialog, Texture texture, float x, float y,
                                      float originX, float originY, float width, float height,
                                      float scaleX, float scaleY, float rotation,
                                      int srcX, int srcY, int srcW, int srcH, boolean flipX, boolean flipY) {
        if (!PortraitScreens.enabled()) {
            batch.draw(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation,
                    srcX, srcY, srcW, srcH, flipX, flipY);
            return;
        }
        float alpha = batch.getColor().a;
        if (srcW == 1763) {
            // Replace the stretched landscape parchment with a crisp chamfered frosted-glass portrait card.
            float left = cardLeft();
            float right = cardRight();
            float bottom = cardBottom();
            float top = cardTop();
            float w = right - left;
            float h = top - bottom;
            float cut = Math.min(w * 0.032f, h * 0.018f);
            float border = Math.max(2f, w * 0.0028f);
            float packed = batch.getPackedColor();
            try {
                // Soft dim backdrop scrim behind the event card.
                batch.setColor(0.02f, 0.03f, 0.048f, 0.56f * alpha);
                batch.draw(ImageMaster.WHITE_SQUARE_IMG, 0f, 0f, Settings.WIDTH, Settings.HEIGHT);
                // Antique-gold chamfered frame + deep obsidian glass body.
                batch.setColor(0.80f, 0.65f, 0.38f, 0.74f * alpha);
                PortraitUi.drawCutCorner(batch, left, bottom, right, top, cut);
                batch.setColor(0.058f, 0.086f, 0.118f, 0.93f * alpha);
                PortraitUi.drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
                // Subtle warm gold bevel at top of card.
                batch.setColor(0.96f, 0.82f, 0.50f, 0.22f * alpha);
                batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + cut, top - border * 2f,
                        w - cut * 2f, Math.max(2f, border * 0.75f));
                // Hairline title divider beneath the header.
                float divPad = w * 0.055f;
                batch.setColor(0.80f, 0.65f, 0.38f, 0.34f * alpha);
                batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + divPad, titleDividerY(), w - divPad * 2f, 2f);
                // Subtle hairline divider above the options stack when choices are present.
                int optCount = dialog != null && dialog.optionList != null ? dialog.optionList.size() : 0;
                if (optCount > 0) {
                    float optDivY = optionsTop(optCount, false) + PortraitViewport.contentHeight() * 0.011f;
                    batch.setColor(0.56f, 0.66f, 0.74f, 0.24f * alpha);
                    batch.draw(ImageMaster.WHITE_SQUARE_IMG, left + divPad, optDivY, w - divPad * 2f, 2f);
                }
            } finally {
                batch.setColor(packed);
            }
            return;
        }
        if (srcW == 610) {
            // Skip vanilla mismatched 610x610 border; our chamfered gold frame wraps the artwork directly.
            return;
        }
        // Event illustration (600x600): draw prominently centered inside a crisp antique-gold frame.
        currentDialogHasImage = true;
        float size = artSize();
        float artLeft = PortraitViewport.x(0.50f) - size * 0.5f;
        float artBot = artBottom();
        float framePad = Math.max(4f, size * 0.008f);
        float cut = size * 0.025f;
        float packed = batch.getPackedColor();
        try {
            batch.setColor(0.80f, 0.65f, 0.38f, 0.78f * alpha);
            PortraitUi.drawCutCorner(batch, artLeft - framePad, artBot - framePad,
                    artLeft + size + framePad, artBot + size + framePad, cut);
            batch.setColor(0.04f, 0.05f, 0.07f, alpha);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, artLeft, artBot, size, size);
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(texture, artLeft, artBot, 0f, 0f, size, size, 1f, 1f, 0f,
                    srcX, srcY, srcW, srcH, flipX, flipY);
        } finally {
            batch.setColor(packed);
        }
    }

    @SpirePatch(clz = GenericEventDialog.class, method = "update", paramtypez = {})
    @SpirePatch(clz = GenericEventDialog.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = GenericEventDialog.class, method = "bodyTextEffect", paramtypez = {})
    @SpirePatch(clz = GenericEventDialog.class, method = "bodyTextEffectCN", paramtypez = {})
    public static class EventDialog {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (LargeDialogOptionButton.class.getName().equals(call.getClassName())) {
                        if ("update".equals(call.getMethodName())) {
                            call.replace("{ sts1portrait.PortraitEvents.updateOption($0, $1, false); }");
                        } else if ("render".equals(call.getMethodName())) {
                            call.replace("{ sts1portrait.PortraitEvents.renderOption($0, $1, optionList.size(), false); }");
                        }
                    } else if (SpriteBatch.class.getName().equals(call.getClassName())
                            && "draw".equals(call.getMethodName())
                            && "(Lcom/badlogic/gdx/graphics/Texture;FFFFFFFFFIIIIZZ)V".equals(call.getSignature())) {
                        call.replace("{ sts1portrait.PortraitEvents.drawEventPanel($0, this, $$); }");
                    } else if (GlyphLayout.class.getName().equals(call.getClassName()) && "setText".equals(call.getMethodName())
                            && "(Lcom/badlogic/gdx/graphics/g2d/BitmapFont;Ljava/lang/CharSequence;)V".equals(call.getSignature())) {
                        call.replace("{ sts1portrait.PortraitEvents.measureWord($0, $$); }");
                    } else if (DialogWord.class.getName().equals(call.getClassName()) && "render".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitEvents.renderWord($0, $1, optionList.size()); }");
                    } else if (FontHelper.class.getName().equals(call.getClassName())
                            && "renderFontCentered".equals(call.getMethodName())) {
                        call.replace("{ if (sts1portrait.PortraitScreens.enabled())"
                                + " sts1portrait.PortraitEvents.drawEventTitle($1, $2, $3, $6);"
                                + " else $proceed($$); }");
                    }
                }

                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isReader()) return;
                    if ("LINE_SPACING".equals(field.getFieldName()) || "CHAR_SPACING".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitEvents.bodySpacing($proceed()); }");
                        return;
                    }
                    String geometry;
                    switch (field.getFieldName()) {
                        case "TITLE_X": geometry = "x(0.5f)"; break;
                        case "TITLE_Y": geometry = "y(0.82f)"; break;
                        case "DIALOG_MSG_X": geometry = "nativeBodyX()"; break;
                        case "DIALOG_MSG_Y": geometry = "nativeBodyY()"; break;
                        case "DIALOG_MSG_W": geometry = "nativeBodyWidth()"; break;
                        default: return;
                    }
                    String owner = field.getFieldName().startsWith("TITLE_") ? "PortraitViewport" : "PortraitEvents";
                    field.replace("{ $_ = sts1portrait.PortraitScreens.enabled() ? sts1portrait." + owner + "."
                            + geometry + " : $proceed(); }");
                }
            };
        }
    }
}
