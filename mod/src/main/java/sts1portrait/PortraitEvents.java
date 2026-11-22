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
import com.esotericsoftware.spine.Skeleton;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.AnimatedNpc;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.events.beyond.SpireHeart;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
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
    private static final float BODY_SCALE = 2.25f;

    private static final float HEART_STORY_SCALE = 1.65f;
    private static final Matrix4 heartStoryBase = new Matrix4();
    private static final Matrix4 heartStoryText = new Matrix4();
    private static boolean drawingHeartStory;
    private static LargeDialogOptionButton eventOption;
    private static boolean roomOption;
    private static boolean drawingWord;
    private static final Rectangle bodyClip = new Rectangle();
    private static final Matrix4 bodyBase = new Matrix4();
    private static final Matrix4 bodyLayout = new Matrix4();
    private static boolean drawingBody, bodyClipped, draggingBody;
    private static int bodyOptions;
    private static float bodyScroll;
    private static int dragY;
    private static ArrayList<DialogWord> bodyWords;
    private static float lineSpacing;
    private PortraitEvents() {}

    public static float heartX() { return PortraitViewport.x(.66f); }

    private static float heartFloor() { return PortraitViewport.y(.385f); }

    private static float heartY() { return heartFloor() - PortraitViewport.referenceWidth() * .065f; }

    public static float heartImpactY() { return heartY() + 320f * Settings.scale; }

    public static boolean heartSceneActive() {
        return PortraitViewport.active() && AbstractDungeon.currMapNode != null
                && AbstractDungeon.getCurrRoom() != null && AbstractDungeon.getCurrRoom().event instanceof SpireHeart;
    }

    private static float heartStoryScale() {
        return HEART_STORY_SCALE * Math.min(1f, PortraitViewport.contentWidth() / PortraitViewport.referenceWidth());
    }

    private static float heartStoryBaseline() { return PortraitViewport.y(.76f); }

    private static float heartStoryHeight() { return 300f * Settings.scale * heartStoryScale(); }

    private static float heartStoryCenterY() { return heartStoryBaseline() - heartStoryHeight() / 6f; }

    public static float heartStoryWrapWidth() { return PortraitViewport.referenceWidth() * .73f / HEART_STORY_SCALE; }

    @SpirePatch(clz = AbstractEvent.class, method = "renderRoomEventPanel", paramtypez = {SpriteBatch.class})
    public static class HeartStoryBackground {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (SpriteBatch.class.getName().equals(call.getClassName()) && "draw".equals(call.getMethodName())) {
                        call.replace("{ if (this instanceof com.megacrit.cardcrawl.events.beyond.SpireHeart"
                                + " && sts1portrait.PortraitEvents.heartSceneActive()) {"
                                + " sts1portrait.PortraitEvents.drawHeartStoryBackground($0, $1);"
                                + " } else { $proceed($$); } }");
                    }
                }
            };
        }
    }

    public static void drawHeartStoryBackground(SpriteBatch batch, Texture texture) {

        float height = heartStoryHeight();
        batch.draw(texture, (PortraitViewport.x(0f)),
                (heartStoryCenterY() - height * .5f),
                PortraitViewport.contentWidth(), height);
    }

    @SpirePatch(clz = RoomEventDialog.class, method = "bodyTextEffect", paramtypez = {})
    @SpirePatch(clz = RoomEventDialog.class, method = "bodyTextEffectCN", paramtypez = {})
    public static class HeartStoryWrap {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && RoomEventDialog.class.getName().equals(field.getClassName())
                            && "DIALOG_MSG_W".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitEvents.heartSceneActive()"
                                + " ? sts1portrait.PortraitEvents.heartStoryWrapWidth() : $proceed(); }");
                    }
                }
            };
        }
    }

    public static void renderRoomWord(DialogWord word, SpriteBatch batch, float offsetY) {
        if (!drawingHeartStory && heartSceneActive()) {
            drawingHeartStory = true;

            float scale = heartStoryScale();
            heartStoryBase.set(batch.getTransformMatrix());
            // Match RoomEventDialog's native starting position, including its render-time offset.
            float originalX = PortraitViewport.referenceCanvasWidth() * .1f;
            float originalY = offsetY + 250f * Settings.scale;
            heartStoryText.set(heartStoryBase)
                    .translate(PortraitViewport.x(.135f) - originalX * scale,
                            heartStoryBaseline() - originalY * scale, 0f)
                    .scale(scale, scale, 1f);
            batch.setTransformMatrix(heartStoryText);
        }
        word.render(batch, offsetY);
    }

    private static void endHeartStory(SpriteBatch batch) {
        if (drawingHeartStory) {
            batch.setTransformMatrix(heartStoryBase);

            drawingHeartStory = false;
        }
    }

    @SpirePatch(clz = RoomEventDialog.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class HeartStoryDrawing {
        @SpirePostfixPatch
        public static void after(RoomEventDialog __instance, SpriteBatch batch) {
            endHeartStory(batch);
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

    private static void place(LargeDialogOptionButton option, int count, boolean room) {
        int slot = Math.max(0, option.slot);
        float step = optionStep(count);
        Hitbox area = option.hb;
        boolean heart = room && heartSceneActive();
        area.width = PortraitViewport.contentWidth() * (heart ? .70f : .88f);
        area.height = step * 0.90f;
        area.move(PortraitViewport.x(0.5f),
                PortraitViewport.y(heart ? .22f : room ? .095f : .145f)
                        + Math.max(0, count - 1 - slot) * step);
    }

    private static float optionStep(int count) {
        return Math.min(PortraitViewport.referenceHeight() * .073f,
                PortraitViewport.contentHeight() * .36f / Math.max(count, 1));
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
            if (eventOption == __instance) place(__instance, roomOption ? RoomEventDialog.optionList.size() : count, roomOption);
        }
    }

    public static void renderOption(LargeDialogOptionButton option, SpriteBatch batch, int count, boolean room) {
        if (room) endHeartStory(batch);
        else endBody(batch);
        if (PortraitScreens.enabled()) {
            place(option, count, room);
            Hitbox area = option.hb;
            PortraitUi.drawOption(batch, option);
        } else {
            option.render(batch);
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

    public static void measureWord(GlyphLayout layout, BitmapFont font, CharSequence word) {
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
        return PortraitViewport.referenceX(.095f);
    }

    public static float nativeBodyY() {
        return PortraitViewport.referenceHeight() * .649f;
    }

    public static float nativeBodyWidth() {
        return PortraitViewport.referenceWidth() * .81f;
    }

    private static float bodyBottom(int options) {
        float last = Math.max(0, options - 1);
        return PortraitViewport.y(.145f) + (last + .58f) * optionStep(options);
    }

    private static float bodyTop() { return PortraitViewport.y(.667f); }

    private static float bodyTopPadding() {
        return Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth()) * .042f;
    }

    private static float bodyFit() {
        return Math.min(1f, PortraitViewport.contentWidth() / PortraitViewport.referenceWidth());
    }

    private static float scrollLimit(int options) {
        if (bodyWords == null) {
            bodyWords = ReflectionHacks.getPrivateStatic(GenericEventDialog.class, "words");
            lineSpacing = ReflectionHacks.<Float>getPrivateStatic(GenericEventDialog.class, "LINE_SPACING") * BODY_SCALE;
        }
        if (bodyWords.isEmpty()) return 0f;
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
        public static void before(GenericEventDialog __instance, boolean ___show) {
            if (!PortraitScreens.enabled() || !___show) return;
            int options = __instance.optionList.size();
            float limit = scrollLimit(options);
            boolean inside = InputHelper.mX >= PortraitViewport.x(.095f) && InputHelper.mX <= PortraitViewport.x(.905f)
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
            batch.flush();
            bodyClip.set(PortraitViewport.x(.09f), bottom, PortraitViewport.contentWidth() * .82f, top - bottom);
            PortraitDesktopViewport.clip(bodyClip);
            bodyClipped = ScissorStack.pushScissors(bodyClip);
            if (bodyClipped) {
                float fit = bodyFit();
                float scroll = Math.min(bodyScroll, scrollLimit(options));
                bodyBase.set(batch.getTransformMatrix());
                bodyLayout.set(bodyBase).translate(PortraitViewport.x(.095f), top - bodyTopPadding() + scroll, 0f)
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

    private static final Color SCROLL_THUMB = new Color(.75f, .65f, .44f, .7f);

    private static void drawScrollThumb(SpriteBatch batch) {
        float limit = scrollLimit(bodyOptions);
        if (limit <= 0f) return;
        float bottom = bodyBottom(bodyOptions);
        float height = bodyTop() - bottom;
        float thumb = height * height / (height + limit);
        float progress = MathUtils.clamp(bodyScroll / limit, 0f, 1f);
        float previousColor = batch.getPackedColor();
        PortraitDrawing.rectangle(batch, PortraitViewport.x(.91f), bottom + (1f - progress) * (height - thumb),
                Math.max(2f, PortraitViewport.referenceWidth() * .005f), thumb, SCROLL_THUMB);
        batch.setColor(previousColor);
    }

    @SpirePatch(clz = GenericEventDialog.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class BodyRendering {
        @SpirePostfixPatch
        public static void after(GenericEventDialog __instance, SpriteBatch batch) { endBody(batch); }
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
                            + " $3 = sts1portrait.PortraitViewport.x(.095f);"
                            + " $4 = sts1portrait.PortraitViewport.y(.057f);"

                            + " } $proceed($$); }");
                }
            };
        }
    }

    public static void drawEventPanel(SpriteBatch batch, Texture texture, float x, float y,
                                      float originX, float originY, float width, float height,
                                      float scaleX, float scaleY, float rotation,
                                      int srcX, int srcY, int srcW, int srcH, boolean flipX, boolean flipY) {
        if (PortraitScreens.enabled()) {
            if (srcW == 1763) {
                x = PortraitViewport.x(0.025f);
                y = PortraitViewport.y(0.10f);
                width = PortraitViewport.contentWidth() * 0.95f;
                height = PortraitViewport.contentHeight() * 0.79f;
            } else {
                width = height = Math.min(PortraitViewport.contentWidth() * 0.35f,
                        PortraitViewport.contentHeight() * 0.15f);
                x = PortraitViewport.x(0.5f) - width * 0.5f;
                y = PortraitViewport.y(0.72f);
            }
            originX = originY = 0;
            scaleX = scaleY = 1;
        }
        batch.draw(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation,
                srcX, srcY, srcW, srcH, flipX, flipY);
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
                        call.replace("{ sts1portrait.PortraitEvents.drawEventPanel($0, $$); }");
                    } else if (GlyphLayout.class.getName().equals(call.getClassName()) && "setText".equals(call.getMethodName())
                            && "(Lcom/badlogic/gdx/graphics/g2d/BitmapFont;Ljava/lang/CharSequence;)V".equals(call.getSignature())) {
                        call.replace("{ sts1portrait.PortraitEvents.measureWord($0, $$); }");
                    } else if (DialogWord.class.getName().equals(call.getClassName()) && "render".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitEvents.renderWord($0, $1, optionList.size()); }");
                    } else if ("com.megacrit.cardcrawl.helpers.FontHelper".equals(call.getClassName())
                            && "renderFontCentered".equals(call.getMethodName())) {
                        call.replace("{ if (sts1portrait.PortraitScreens.enabled()) $7 *= 1.48f; $proceed($$); }");
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
                        case "TITLE_Y": geometry = "y(0.695f)"; break;
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
