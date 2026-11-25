package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
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
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;
import com.megacrit.cardcrawl.screens.SingleRelicViewPopup;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.options.InputSettingsScreen;
import com.megacrit.cardcrawl.screens.options.ConfirmPopup;
import com.megacrit.cardcrawl.ui.buttons.CancelButton;
import com.megacrit.cardcrawl.ui.buttons.CardSelectConfirmButton;
import com.megacrit.cardcrawl.ui.buttons.DynamicBanner;
import com.megacrit.cardcrawl.ui.buttons.ProceedButton;
import com.megacrit.cardcrawl.ui.buttons.ReturnToMenuButton;
import com.megacrit.cardcrawl.ui.buttons.SkipCardButton;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitScreens {
    private static final Matrix4 original = new Matrix4();
    private static final Matrix4 moved = new Matrix4();
    private static int mouseX;
    private static int mouseY;
    private static int drawingDepth;
    private static int inputDepth;
    private static boolean drawingApplied;
    private static boolean drawingCardReward;
    private static int rewardRevision = -1;
    private static boolean inputApplied;

    private PortraitScreens() {}

    static Matrix4 baseMatrix(SpriteBatch batch) {
        return drawingDepth > 0 && drawingApplied ? original : batch.getTransformMatrix();
    }

    static int baseMouseX() { return inputDepth > 0 && inputApplied ? mouseX : InputHelper.mX; }
    static int baseMouseY() { return inputDepth > 0 && inputApplied ? mouseY : InputHelper.mY; }

    public static boolean enabled() {
        return PortraitViewport.active() && !Settings.isControllerMode;
    }

    public static boolean isDrawingCardReward() {
        return drawingCardReward;
    }

    private static boolean leftNavigation(Object screen) {
        return screen instanceof CancelButton || screen instanceof MenuCancelButton;
    }

    private static boolean navigation(Object screen) {
        return leftNavigation(screen) || screen instanceof ProceedButton || screen instanceof ReturnToMenuButton;
    }

    private static boolean hiddenCornerButton(Object button) {
        if (button instanceof CancelButton) return ((CancelButton) button).isHidden;
        if (button instanceof MenuCancelButton) return ((MenuCancelButton) button).isHidden;
        return button instanceof ProceedButton
                && ReflectionHacks.<Boolean>getPrivate(button, ProceedButton.class, "isHidden");
    }

    private static final class NavigationLayout {
        float scale, x, y;

        void set(Object button) {
            scale = scale(button);
            x = offsetX(button, scale);
            y = offsetY(button, scale);
        }
    }

    static float scale(Object screen) {
        float safeWidthRatio = PortraitViewport.contentWidth() / Settings.WIDTH;
        if (navigation(screen)) {
            return 1.30f * safeWidthRatio;
        }
        if (screen instanceof ConfirmPopup) return 2.15f * safeWidthRatio;
        if (screen instanceof CardSelectConfirmButton) return 1.52f * safeWidthRatio;
        if (screen instanceof SingleRelicViewPopup) return 1.63f * safeWidthRatio;
        if (screen instanceof CombatRewardScreen) {
            float fixed = 2.55f * PortraitViewport.referenceWidth() / PortraitViewport.referenceCanvasWidth();
            float fitWidth = PortraitViewport.contentWidth() * .95f / (612f * Settings.xScale);
            float fitHeight = PortraitViewport.contentHeight() * .76f / (716f * Settings.scale);
            return Math.min(fixed, Math.min(fitWidth, fitHeight));
        }
        if (screen instanceof InputSettingsScreen) return 1.44f * safeWidthRatio;
        if (screen instanceof CardRewardScreen) {
            ArrayList<AbstractCard> cards = ((CardRewardScreen) screen).rewardGroup;
            int count = cards == null ? 3 : cards.size();
            float size = count <= 3 ? 1.52f : count == 4 ? 1.18f : 1f;
            if (!stableCardReward((CardRewardScreen) screen)) return size * safeWidthRatio;
            float span = Math.max(0, count - 1) * rewardCardSpacing()
                    + AbstractCard.IMG_WIDTH;
            float enlarged = count <= 3 ? 2.12f : 1.96f;
            return Math.min(enlarged * PortraitViewport.referenceWidth() / PortraitViewport.referenceCanvasWidth(),
                    PortraitViewport.contentWidth() * .95f / span);
        }
        if (screen instanceof CampfireUI) {
            float width = Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth());
            float fixed = 1.70f * width / PortraitViewport.referenceCanvasWidth();
            return Math.min(fixed, PortraitViewport.contentHeight() * .48f / (740f * Settings.scale));
        }
        return 1.10f * safeWidthRatio;
    }

    private static float rewardCardSpacing() {
        return AbstractCard.IMG_WIDTH + 16f * Settings.xScale;
    }

    public static boolean stableCardReward(CardRewardScreen screen) {
        return screen != null && screen.rItem != null && screen.rewardGroup != null && screen.rewardGroup.size() <= 4;
    }

    static float sourceCenterX(Object screen) {
        return sourceWidth(screen) * .5f;
    }

    private static int sourceWidth(Object screen) {
        if (screen instanceof CombatRewardScreen || screen instanceof CardRewardScreen
                && stableCardReward((CardRewardScreen) screen)) return PortraitViewport.referenceCanvasWidth();
        return Settings.WIDTH;
    }

    static float sourceCenterY(Object screen) {
        if (screen instanceof CardRewardScreen && stableCardReward((CardRewardScreen) screen)) {
            return PortraitViewport.referenceCanvasHeight() * .45f;
        }
        return Settings.HEIGHT * .5f;
    }

    private static float offsetX(Object screen, float scale) {
        if (leftNavigation(screen)) return 0;
        if (screen instanceof ProceedButton) {
            // The original hitbox is centered at SHOW_X, which uses xScale rather than scale.
            float originalCenter = 1670f * Settings.xScale;
            float targetCenter = PortraitViewport.x(0.965f) - 165f * Settings.scale * scale - 6f;
            return targetCenter - originalCenter * scale;
        }
        if (screen instanceof InputSettingsScreen) {
            return PortraitViewport.x(0.5f) - Settings.WIDTH * 0.5f * scale;
        }
        if (screen instanceof CampfireUI) {
            return PortraitViewport.x(0.5f) - PortraitViewport.referenceCanvasWidth() * 0.5f * scale;
        }
        if (screen instanceof CombatRewardScreen) {
            return PortraitViewport.x(0.5f) - sourceCenterX(screen) * scale;
        }
        if (screen instanceof CardSelectConfirmButton) {
            return PortraitBattle.endTurnX(PortraitViewport.x(0.5f)) - sourceCenterX(screen) * scale;
        }
        return PortraitViewport.x(0.5f) - sourceCenterX(screen) * scale;
    }

    static float cardRewardCenterY(CardRewardScreen screen) {
        return PortraitViewport.y(stableCardReward(screen) ? .53f : .50f);
    }

    private static float offsetY(Object screen, float scale) {
        if (screen instanceof CardRewardScreen) return cardRewardCenterY((CardRewardScreen) screen) - sourceCenterY(screen) * scale;
        if (leftNavigation(screen) || screen instanceof ProceedButton) {
            float originalCenter = leftNavigation(screen) ? 188f : 320f;
            return cornerButtonY(scale) - originalCenter * Settings.scale * scale;
        }
        if (screen instanceof ReturnToMenuButton) return PortraitViewport.y(0.012f);
        if (screen instanceof CardSelectConfirmButton) return PortraitBattle.endTurnY(0) - 475f * Settings.scale * scale;
        if (screen instanceof InputSettingsScreen) {
            return PortraitViewport.y(0.58f) - Settings.OPTION_Y * scale;
        }
        if (screen instanceof CampfireUI) {
            return PortraitViewport.y(0.54f) - PortraitViewport.referenceCanvasHeight() * 0.5f * scale;
        }
        if (screen instanceof CombatRewardScreen) {
            return PortraitViewport.y(0.54f) - Settings.HEIGHT * 0.5f * scale;
        }
        return PortraitViewport.y(0.5f) - sourceCenterY(screen) * scale;
    }

    static float cornerButtonY(float scale) {
        return PortraitViewport.y(0.02f) + 94f * Settings.scale * scale + 6f;
    }

    @SpirePatch(clz = InputSettingsScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = CombatRewardScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = CardRewardScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = CampfireUI.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = SingleRelicViewPopup.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class Drawing {
        @SpirePrefixPatch
        public static void before(Object __instance, SpriteBatch batch) {
            if (drawingDepth++ != 0) return;
            drawingApplied = enabled();
            if (!drawingApplied) return;
            drawingCardReward = __instance instanceof CardRewardScreen;
            float scale = scale(__instance);
            original.set(batch.getTransformMatrix());
            moved.set(original).translate(offsetX(__instance, scale), offsetY(__instance, scale), 0)
                    .scale(scale, scale, 1);
            batch.setTransformMatrix(moved);
        }

        @SpirePostfixPatch
        public static void after(Object __instance, SpriteBatch batch) {
            if (--drawingDepth != 0) return;
            drawingCardReward = false;
            if (drawingApplied) batch.setTransformMatrix(original);
        }
    }

    @SpirePatch(clz = DynamicBanner.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class CombatRewardBanner {
        private static final Matrix4 previous = new Matrix4();
        private static final Matrix4 positioned = new Matrix4();
        private static boolean applied;

        @SpirePrefixPatch
        public static void before(DynamicBanner __instance, SpriteBatch batch) {
            boolean combat = AbstractDungeon.screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD;
            boolean card = AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD
                    && stableCardReward(AbstractDungeon.cardRewardScreen);
            boolean boss = AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD
                    && PortraitBossReward.active();
            applied = enabled() && (combat || card || boss) && __instance == AbstractDungeon.dynamicBanner;
            if (!applied) return;
            CombatRewardScreen reward = AbstractDungeon.combatRewardScreen;
            float scale = scale(reward);
            float centerX = PortraitViewport.x(.5f);
            float originalY = __instance.y;
            float titleScale = Math.min(1f, PortraitViewport.contentWidth() / PortraitViewport.referenceWidth());
            float centerY = PortraitViewport.y(boss ? .68f : .67f);
            if (card) {
                CardRewardScreen cardScreen = AbstractDungeon.cardRewardScreen;
                centerY = cardRewardCenterY(cardScreen) + AbstractCard.IMG_HEIGHT * scale(cardScreen) * .50f
                        + PortraitViewport.contentWidth() * .084f;
            }
            if (combat) {
                float anchorY = Settings.HEIGHT * .5f + 234f * Settings.scale;
                centerY = (anchorY + originalY - DynamicBanner.Y) * scale + offsetY(reward, scale);
                titleScale *= 1.35f;
            }
            previous.set(batch.getTransformMatrix());
            positioned.set(baseMatrix(batch)).translate(centerX, centerY, 0f)
                    .scale(titleScale, titleScale, 1f).translate(-Settings.WIDTH * .5f, -originalY, 0f);
            batch.setTransformMatrix(positioned);
        }

        @SpirePostfixPatch
        public static void after(DynamicBanner __instance, SpriteBatch batch) {
            if (applied) batch.setTransformMatrix(previous);
        }
    }

    private static final GlyphLayout rewardTip = new GlyphLayout();

    public static void drawRewardTip(SpriteBatch batch, BitmapFont font, String text, float x, float y, Color color) {
        if (!enabled()) {
            FontHelper.renderFontCentered(batch, font, text, x, y, color);
            return;
        }
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        rewardTip.setText(font, text);
        float available = 612f * Settings.xScale;
        float fit = Math.min(1f, available / Math.max(1f, rewardTip.width));
        font.getData().setScale(oldX * fit, oldY * fit);
        try {
            float restingY = Settings.HEIGHT * .5f - 460f * Settings.scale;
            FontHelper.renderFontCentered(batch, font, text, x, restingY, color);
        } finally {
            font.getData().setScale(oldX, oldY);
        }
    }

    @SpirePatch(clz = CombatRewardScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class CombatRewardTip {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (FontHelper.class.getName().equals(call.getClassName())
                            && "renderFontCentered".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitScreens.drawRewardTip($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = MenuCancelButton.class, method = "show", paramtypez = {String.class})
    public static class MenuBackEntrance {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(MenuCancelButton __instance, String label) {
            if (!enabled()) return SpireReturn.Continue();
            __instance.showInstantly(label);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = CancelButton.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = MenuCancelButton.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = ProceedButton.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = ReturnToMenuButton.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = CardSelectConfirmButton.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = ConfirmPopup.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class NavigationDrawing {
        private static final Matrix4 previous = new Matrix4();
        private static final Matrix4 navigation = new Matrix4();
        private static final NavigationLayout layout = new NavigationLayout();
        private static boolean applied;

        @SpirePrefixPatch
        public static SpireReturn<Void> before(Object __instance, SpriteBatch batch) {
            applied = false;
            if (!enabled() || PortraitSettlement.ownsReturnButton(__instance)) return SpireReturn.Continue();
            if (__instance instanceof ConfirmPopup && PortraitSettingsConfirm.usesSettingsStyle((ConfirmPopup) __instance)) {
                PortraitSettingsConfirm.draw((ConfirmPopup) __instance, batch);
                return SpireReturn.Return();
            }
            if (PortraitCharacterSelect.isBack(__instance)) {
                PortraitCharacterSelect.drawBack((MenuCancelButton) __instance, batch);
                return SpireReturn.Return();
            }
            // Landscape hides these sprites by moving them past the original screen edge.
            if (hiddenCornerButton(__instance)) return SpireReturn.Return();
            previous.set(batch.getTransformMatrix());
            layout.set(__instance);
            navigation.set(drawingDepth == 0 || !drawingApplied ? previous : original)
                    .translate(layout.x, layout.y, 0)
                    .scale(layout.scale, layout.scale, 1);
            batch.setTransformMatrix(navigation);
            applied = true;
            return SpireReturn.Continue();
        }

        @SpirePostfixPatch
        public static void after(Object __instance, SpriteBatch batch) {
            if (applied) batch.setTransformMatrix(previous);
        }
    }

    public static void drawConfirmBackdrop(SpriteBatch batch, Texture texture, float x, float y, float w, float h) {
        if (texture != ImageMaster.WHITE_SQUARE_IMG || !enabled()) {
            batch.draw(texture, x, y, w, h);
            return;
        }
        batch.setTransformMatrix(drawingDepth == 0 || !drawingApplied ? NavigationDrawing.previous : original);
        try {
            batch.draw(texture, x, y, w, h);
        } finally {
            batch.setTransformMatrix(NavigationDrawing.navigation);
        }
    }

    @SpirePatch(clz = ConfirmPopup.class, method = "renderPopupBg", paramtypez = {SpriteBatch.class})
    public static class ConfirmBackdrop {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (SpriteBatch.class.getName().equals(call.getClassName()) && "draw".equals(call.getMethodName())
                            && "(Lcom/badlogic/gdx/graphics/Texture;FFFF)V".equals(call.getSignature())) {
                        call.replace("{ sts1portrait.PortraitScreens.drawConfirmBackdrop($0, $$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = InputSettingsScreen.class, method = "update", paramtypez = {})
    @SpirePatch(clz = CombatRewardScreen.class, method = "update", paramtypez = {})
    @SpirePatch(clz = CardRewardScreen.class, method = "update", paramtypez = {})
    @SpirePatch(clz = CampfireUI.class, method = "update", paramtypez = {})
    @SpirePatch(clz = SingleRelicViewPopup.class, method = "update", paramtypez = {})
    public static class Pointer {
        @SpirePrefixPatch
        public static void before(Object __instance) {
            if (inputDepth++ != 0) return;
            inputApplied = enabled();
            if (!inputApplied) return;
            if (__instance instanceof CombatRewardScreen && rewardRevision != PortraitViewport.revision()) {
                rewardRevision = PortraitViewport.revision();
                ((CombatRewardScreen) __instance).positionRewards();
            }
            float scale = scale(__instance);
            mouseX = InputHelper.mX;
            mouseY = InputHelper.mY;
            InputHelper.mX = Math.round((mouseX - offsetX(__instance, scale)) / scale);
            InputHelper.mY = Math.round((mouseY - offsetY(__instance, scale)) / scale);
        }

        @SpirePostfixPatch
        public static void after(Object __instance) {
            if (--inputDepth != 0 || !inputApplied) return;
            InputHelper.mX = mouseX;
            InputHelper.mY = mouseY;
        }
    }

    @SpirePatch(clz = CancelButton.class, method = "update", paramtypez = {})
    @SpirePatch(clz = MenuCancelButton.class, method = "update", paramtypez = {})
    @SpirePatch(clz = ProceedButton.class, method = "update", paramtypez = {})
    @SpirePatch(clz = ReturnToMenuButton.class, method = "update", paramtypez = {})
    @SpirePatch(clz = CardSelectConfirmButton.class, method = "update", paramtypez = {})
    @SpirePatch(clz = ConfirmPopup.class, method = "update", paramtypez = {})
    public static class NavigationPointer {
        private static final NavigationLayout layout = new NavigationLayout();
        private static int previousX;
        private static int previousY;
        private static boolean applied;

        @SpirePrefixPatch
        public static void before(Object __instance) {
            applied = enabled() && !PortraitBossReward.isSkip(__instance)
                    && !PortraitSettlement.ownsReturnButton(__instance);
            if (!applied) return;
            previousX = InputHelper.mX;
            previousY = InputHelper.mY;
            int x = inputDepth == 0 || !inputApplied ? previousX : mouseX;
            int y = inputDepth == 0 || !inputApplied ? previousY : mouseY;
            if (__instance instanceof ConfirmPopup && PortraitSettingsConfirm.usesSettingsStyle((ConfirmPopup) __instance)) {
                PortraitSettingsConfirm.map((ConfirmPopup) __instance, x, y);
                return;
            }
            if (PortraitCharacterSelect.isBack(__instance)) {
                PortraitCharacterSelect.mapBack((MenuCancelButton) __instance, x, y);
                return;
            }
            layout.set(__instance);
            InputHelper.mX = Math.round((x - layout.x) / layout.scale);
            InputHelper.mY = Math.round((y - layout.y) / layout.scale);
        }

        @SpirePostfixPatch
        public static void after(Object __instance) {
            if (!applied) return;
            InputHelper.mX = previousX;
            InputHelper.mY = previousY;
        }
    }

    @SpirePatch(cls = "basemod.patches.com.megacrit.cardcrawl.screens.CombatRewardScreen.RewardsScrolling$RenderScissor",
            method = "Insert", paramtypez = {CombatRewardScreen.class, SpriteBatch.class}, optional = true)
    public static class RewardScissor {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if ("com.badlogic.gdx.scenes.scene2d.utils.ScissorStack".equals(call.getClassName())
                            && "pushScissors".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitScreens.rewardScissor($1); $_ = $proceed($$); }");
                    }
                }

                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && Settings.class.getName().equals(field.getClassName())
                            && "WIDTH".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitScreens.enabled()"
                                + " ? sts1portrait.PortraitViewport.referenceCanvasWidth() : $proceed(); }");
                    }
                }
            };
        }
    }

    public static void rewardScissor(Rectangle area) {
        if (enabled()) PortraitDesktopViewport.clipProjected(area);
    }

    @SpirePatch(clz = CombatRewardScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = CombatRewardScreen.class, method = "renderItemReward", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = RewardItem.class, method = SpirePatch.STATICINITIALIZER)
    @SpirePatch(clz = RewardItem.class, method = "move", paramtypez = {float.class})
    @SpirePatch(clz = RewardItem.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class CombatRewardSourceWidth {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && Settings.class.getName().equals(field.getClassName())
                            && "WIDTH".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitScreens.enabled()"
                                + " ? sts1portrait.PortraitViewport.referenceCanvasWidth() : $proceed(); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = CardRewardScreen.class, method = "open",
            paramtypez = {ArrayList.class, RewardItem.class, String.class})
    @SpirePatch(clz = CardRewardScreen.class, method = "renderCardReward", paramtypez = {SpriteBatch.class})
    public static class StandardCardRewardSource {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isReader() || !Settings.class.getName().equals(field.getClassName())) return;
                    String axis = field.getFieldName();
                    if (!"WIDTH".equals(axis) && !"HEIGHT".equals(axis)) return;
                    field.replace("{ $_ = sts1portrait.PortraitScreens.enabled()"
                            + " && sts1portrait.PortraitScreens.stableCardReward(this)"
                            + " ? sts1portrait.PortraitViewport.referenceCanvas"
                            + ("WIDTH".equals(axis) ? "Width" : "Height") + "() : $proceed(); }");
                }
            };
        }
    }

    @SpirePatch(clz = CardRewardScreen.class, method = "renderCardReward", paramtypez = {SpriteBatch.class})
    public static class RewardCardGap {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && "PAD_X".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitScreens.enabled()"
                                + " && sts1portrait.PortraitScreens.stableCardReward(this)"
                                + " ? 16f * com.megacrit.cardcrawl.core.Settings.xScale : $proceed(); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = CardRewardScreen.class, method = SpirePatch.STATICINITIALIZER)
    @SpirePatch(clz = CampfireUI.class, method = SpirePatch.STATICINITIALIZER)
    @SpirePatch(clz = CampfireUI.class, method = "renderCampfireButtons", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = SkipCardButton.class, method = SpirePatch.STATICINITIALIZER)
    public static class RewardButtonPosition {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && Settings.class.getName().equals(field.getClassName())) {
                        if ("WIDTH".equals(field.getFieldName()) || "HEIGHT".equals(field.getFieldName())) {
                            String axis = "WIDTH".equals(field.getFieldName()) ? "Width" : "Height";
                            field.replace("{ $_ = sts1portrait.PortraitViewport.active()"
                                    + " ? sts1portrait.PortraitViewport.referenceCanvas" + axis + "() : $proceed(); }");
                        }
                    } else if (field.isWriter() && "TAKE_Y".equals(field.getFieldName())) {
                        field.replace("{ $proceed(sts1portrait.PortraitViewport.active()"
                                + " ? sts1portrait.PortraitViewport.referenceCanvasHeight() * 0.5f"
                                + " - 500f * com.megacrit.cardcrawl.core.Settings.scale : $1); }");
                    }
                }
            };
        }
    }
}
