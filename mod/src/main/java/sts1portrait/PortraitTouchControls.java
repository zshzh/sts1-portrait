package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.credits.CreditsScreen;
import com.megacrit.cardcrawl.daily.DailyScreen;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.HitboxListener;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.scenes.AbstractScene;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import com.megacrit.cardcrawl.screens.compendium.CardLibraryScreen;
import com.megacrit.cardcrawl.screens.custom.CustomModeScreen;
import com.megacrit.cardcrawl.screens.leaderboards.LeaderboardScreen;
import com.megacrit.cardcrawl.screens.mainMenu.ColorTabBar;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.options.GiantToggleButton;
import com.megacrit.cardcrawl.screens.options.InputSettingsScreen;
import com.megacrit.cardcrawl.screens.options.OptionsPanel;
import com.megacrit.cardcrawl.screens.options.RemapInputElement;
import com.megacrit.cardcrawl.ui.buttons.ConfirmButton;
import com.megacrit.cardcrawl.ui.buttons.GridSelectConfirmButton;
import com.megacrit.cardcrawl.ui.buttons.PeekButton;
import com.megacrit.cardcrawl.ui.buttons.SingingBowlButton;
import com.megacrit.cardcrawl.ui.buttons.SkipCardButton;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitTouchControls {
    private static final Color BORDER = new Color(0.55f, 0.45f, 0.29f, 1f);
    private static final Color SURFACE = new Color(0.12f, 0.20f, 0.25f, 1f);
    private static final Color HOVER = new Color(0.22f, 0.35f, 0.41f, 1f);
    private static final Control CAMPFIRE = new Control("campfire-confirm", "Campfire confirmation", "篝火确认",
            .50f, .35f, .51f, .105f);
    private static final Control SHOP = new Control("shop-confirm", "Shop confirmation", "商店确认",
            .74f, .077f, .43f, .105f);
    private static final Control SMITH = new Control("smith-confirm", "Smith confirmation", "锻造确认",
            .50f, .20f, .51f, .105f);
    private static final Control GRID = new Control("grid-confirm", "Confirm card selection", "确认选牌",
            .76f, .085f, .44f, .105f);
    private static final Control CUSTOM = new Control("custom-embark", "Embark", "启程",
            .64f, .085f, .34f, .095f);
    private static final Control CUSTOM_SEED = new Control("custom-seed", "Set seed", "设定种子",
            .32f, .085f, .26f, .095f);
    private static final Control DAILY = new Control("daily-embark", "Start daily climb", "开始每日挑战",
            .76f, .085f, .44f, .105f);
    private static final Control MY_SCORE = new Control("my-score", "View my score", "查看我的分数",
            .76f, .155f, .44f, .095f);
    private static final Control CREDITS_SKIP = new Control("credits-skip", "Skip credits", "跳过制作人员名单",
            .76f, .085f, .44f, .105f);
    private static final Hitbox CREDITS_HITBOX = new Hitbox(-50f, -50f, 100f, 100f);
    private static final Control VIEW_UPGRADE = new Control("library-view-upgrade", "View upgrades", "查看升级",
            .65f, .949f, .42f, .090f);
    private static final Control PEEK = new Control("peek", "View battlefield", "查看战场",
            .088f, .80f, .124f, .124f);
    private static final Control BOWL = new Control("singing-bowl", "Singing Bowl", "唱歌碗",
            .74f, .34f, .44f, .105f);
    private static final Control REWARD_SKIP = new Control("reward-skip-with-bowl", "Skip card reward", "跳过卡牌奖励",
            .26f, .34f, .44f, .105f);
    private static final Matrix4 worldPrevious = new Matrix4();
    private static final Matrix4 fireTransform = new Matrix4();
    private static final float[] edgeFade = {
            0f, 0f, 0f, .5f, .5f, 0f, 0f, Color.BLACK.toFloatBits(), .5f, .5f,
            0f, 0f, Color.BLACK.toFloatBits(), .5f, .5f, 0f, 0f, 0f, .5f, .5f
    };
    private static AbstractScene fadeScene;
    private static AtlasRegion fadeBackdrop;

    private PortraitTouchControls() {}

    private static final class Control {

        final Matrix4 previous = new Matrix4();
        final float x, y, width, height;
        int previousX, previousY;
        boolean combinedTap;

        Control(String key, String name, String chinese, float x, float y, float width, float height) {

            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        float centerX() {
            return PortraitViewport.x(x);
        }

        float centerY() {
            return PortraitViewport.y(y);
        }

        float size(float fraction) {
            float contentWidth = PortraitViewport.contentWidth();
            if (this == SHOP || this == CAMPFIRE) contentWidth = Math.min(contentWidth, PortraitViewport.referenceWidth());
            return contentWidth * fraction;
        }

        float controlWidth() {
            return size(width);
        }

        float controlHeight() {
            return size(height);
        }

        void map(Hitbox hitbox) {
            previousX = InputHelper.mX;
            previousY = InputHelper.mY;
            float cx = centerX();
            float cy = centerY();
            float w = controlWidth();
            float h = controlHeight();

            InputHelper.mX = Math.round(hitbox.cX
                    + ((PortraitScreens.baseMouseX()) - cx) * hitbox.width / w);
            InputHelper.mY = Math.round(hitbox.cY
                    + ((PortraitScreens.baseMouseY()) - cy) * hitbox.height / h);
        }

        void restorePointer() {
            InputHelper.mX = previousX;
            InputHelper.mY = previousY;
        }

        void mapButton(Hitbox hitbox, boolean acceptsClick) {
            map(hitbox);
            combinedTap = acceptsClick && InputHelper.justClickedLeft && InputHelper.justReleasedClickLeft;
            if (combinedTap) {
                hitbox.clickStarted = Math.abs(InputHelper.mX - hitbox.cX) < hitbox.width * .5f
                        && Math.abs(InputHelper.mY - hitbox.cY) < hitbox.height * .5f;
                InputHelper.justClickedLeft = false;
            }
        }

        void restoreButton() {
            restorePointer();
            if (combinedTap) InputHelper.justClickedLeft = true;
        }

        void draw(SpriteBatch batch, Hitbox hitbox, String label, Boolean checked) {
            float cx = centerX();
            float cy = centerY();
            float w = controlWidth();
            float h = controlHeight();
            float left = cx - w * .5f;
            float bottom = cy - h * .5f;
            float previousColor = batch.getPackedColor();
            previous.set(batch.getTransformMatrix());
            batch.setTransformMatrix(PortraitScreens.baseMatrix(batch));

            try {
                float border = h * .036f;
                PortraitDrawing.rectangle(batch, left, bottom, w, h, BORDER);
                Color fill = hitbox.hovered ? HOVER : SURFACE;
                if (this == PEEK && PeekButton.isPeeking) fill = HOVER;
                PortraitDrawing.rectangle(batch, left + border, bottom + border, w - 2 * border,
                        h - 2 * border, fill);
                if (this == PEEK) {
                    batch.setColor(Color.WHITE);
                    float side = w * .75f;
                    batch.draw(ImageMaster.PEEK_BUTTON, cx - side * .5f, cy - side * .5f, side, side);
                } else if (checked == null) {
                    PortraitSettings.label(batch, label, cx, cy, w * .86f, h * .36f, true);
                } else {
                    PortraitSettings.checkmark(batch, left + w * .87f, cy, h * .39f, checked);
                    PortraitSettings.label(batch, label, left + w * .07f, cy, w * .69f, h * .32f, false);
                }
            } finally {

                batch.setTransformMatrix(previous);
                batch.setColor(previousColor);
            }
        }
    }

    private static boolean campfireButton(ConfirmButton button) {
        if (!inPortraitRestRoom()) return false;
        CampfireUI campfire = ((RestRoom) AbstractDungeon.currMapNode.getRoom()).campfireUI;
        return campfire != null && campfire.confirmButton == button;
    }

    private static Control confirmControl(ConfirmButton button) {
        if (campfireButton(button)) return CAMPFIRE;
        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SHOP
                && AbstractDungeon.shopScreen != null && AbstractDungeon.shopScreen.confirmButton == button) {
            return SHOP;
        }
        if (!PortraitScreens.enabled() || CardCrawlGame.mainMenuScreen == null
                || CardCrawlGame.mainMenuScreen.screen != MainMenuScreen.CurScreen.DAILY) return null;
        DailyScreen daily = CardCrawlGame.mainMenuScreen.dailyScreen;
        return button == ReflectionHacks.<ConfirmButton>getPrivate(daily, DailyScreen.class, "confirmButton") ? DAILY : null;
    }

    static boolean inPortraitRestRoom() {
        return PortraitScreens.enabled() && AbstractDungeon.currMapNode != null
                && AbstractDungeon.currMapNode.getRoom() instanceof RestRoom;
    }

    private static boolean smithButton(GridSelectConfirmButton button) {
        return inPortraitRestRoom() && AbstractDungeon.gridSelectScreen != null
                && AbstractDungeon.gridSelectScreen.forUpgrade && AbstractDungeon.gridSelectScreen.confirmButton == button;
    }

    private static Control gridControl(GridSelectConfirmButton button) {
        if (!PortraitScreens.enabled()) return null;
        if (smithButton(button)) return SMITH;
        if (AbstractDungeon.gridSelectScreen != null && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID
                && AbstractDungeon.gridSelectScreen.confirmButton == button) return GRID;
        if (CardCrawlGame.mainMenuScreen != null && CardCrawlGame.mainMenuScreen.customModeScreen != null
                && CardCrawlGame.mainMenuScreen.customModeScreen.confirmButton == button) return CUSTOM;
        return null;
    }

    @SpirePatch(clz = ConfirmButton.class, method = "render")
    public static class ConfirmDrawing {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(ConfirmButton __instance, SpriteBatch sb) {
            Control control = confirmControl(__instance);
            if (control == null) return SpireReturn.Continue();
            if (!ReflectionHacks.<Boolean>getPrivate(__instance, ConfirmButton.class, "isHidden")
                    && !__instance.isDisabled) {
                String label = ReflectionHacks.getPrivate(__instance, ConfirmButton.class, "buttonText");
                control.draw(sb, __instance.hb, label, null);
            }
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = ConfirmButton.class, method = "update")
    public static class ConfirmPointer {
        private static Control mapped;

        @SpirePrefixPatch
        public static void before(ConfirmButton __instance, boolean ___isHidden) {
            mapped = confirmControl(__instance);
            if (mapped != null) mapped.mapButton(__instance.hb, !___isHidden && !__instance.isDisabled);
        }

        @SpirePostfixPatch
        public static void after() {
            if (mapped != null) mapped.restoreButton();
        }
    }

    @SpirePatch(clz = GridSelectConfirmButton.class, method = "render")
    public static class GridConfirmDrawing {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(GridSelectConfirmButton __instance, SpriteBatch sb) {
            Control control = gridControl(__instance);
            if (control == null) return SpireReturn.Continue();
            if (!ReflectionHacks.<Boolean>getPrivate(__instance, GridSelectConfirmButton.class, "isHidden")
                    && !__instance.isDisabled) {
                String label = ReflectionHacks.getPrivate(__instance, GridSelectConfirmButton.class, "buttonText");
                control.draw(sb, __instance.hb, label, null);
            }
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = GridSelectConfirmButton.class, method = "update")
    public static class GridConfirmPointer {
        private static Control mapped;

        @SpirePrefixPatch
        public static void before(GridSelectConfirmButton __instance, boolean ___isHidden) {
            mapped = gridControl(__instance);
            if (mapped != null) mapped.mapButton(__instance.hb, !___isHidden && !__instance.isDisabled);
        }

        @SpirePostfixPatch
        public static void after() {
            if (mapped != null) mapped.restoreButton();
        }
    }

    public static void updateCustomSeed(Hitbox hitbox) {
        updateControlHitbox(hitbox, PortraitScreens.enabled() ? CUSTOM_SEED : null);
    }

    private static void updateControlHitbox(Hitbox hitbox, Control control) {
        if (control != null) control.map(hitbox);
        try {
            hitbox.update();
        } finally {
            if (control != null) control.restorePointer();
        }
    }

    @SpirePatch(clz = CustomModeScreen.class, method = "updateSeed", paramtypez = {})
    public static class CustomSeedPointer {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if ("update".equals(call.getMethodName()) && Hitbox.class.getName().equals(call.getClassName())) {
                        call.replace("{ sts1portrait.PortraitTouchControls.updateCustomSeed($0); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = CustomModeScreen.class, method = "renderSeed", paramtypez = {SpriteBatch.class})
    public static class CustomSeedDrawing {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(CustomModeScreen __instance, SpriteBatch sb, Hitbox ___seedHb,
                String ___currentSeed) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            CUSTOM_SEED.draw(sb, ___seedHb, CustomModeScreen.TEXT[8] + ": " + ___currentSeed, null);
            return SpireReturn.Return();
        }
    }

    private static boolean libraryUpgrade() {
        return PortraitScreens.enabled() && CardCrawlGame.mainMenuScreen != null
                && CardCrawlGame.mainMenuScreen.screen == MainMenuScreen.CurScreen.CARD_LIBRARY;
    }

    public static void updateLibraryHitbox(ColorTabBar bar, Hitbox hitbox) {
        updateControlHitbox(hitbox, libraryUpgrade() && hitbox == bar.viewUpgradeHb ? VIEW_UPGRADE : null);
    }

    public static boolean libraryUpgradeHovered(ColorTabBar bar) {
        return libraryUpgrade() && bar.viewUpgradeHb.hovered;
    }

    @SpirePatch(clz = ColorTabBar.class, method = "update", paramtypez = {float.class})
    public static class LibraryUpgradePointer {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if ("update".equals(call.getMethodName()) && Hitbox.class.getName().equals(call.getClassName())) {
                        call.replace("{ sts1portrait.PortraitTouchControls.updateLibraryHitbox(this, $0); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = ColorTabBar.class, method = "renderViewUpgrade", paramtypez = {SpriteBatch.class, float.class})
    public static class NativeLibraryUpgradeDrawing {
        @SpirePrefixPatch
        public static SpireReturn<Void> before() {
            return libraryUpgrade() ? SpireReturn.Return() : SpireReturn.Continue();
        }
    }

    public static void updateMyScore(Hitbox hitbox) {
        if (!PortraitScreens.enabled()) {
            hitbox.update();
            return;
        }
        MY_SCORE.map(hitbox);
        try {
            PortraitSettingsConfirm.updateButton(hitbox);
        } finally {
            MY_SCORE.restorePointer();
        }
    }

    @SpirePatch(clz = DailyScreen.class, method = "updateViewMyScore", paramtypez = {})
    @SpirePatch(clz = LeaderboardScreen.class, method = "updateViewMyScore", paramtypez = {})
    public static class MyScorePointer {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if ("update".equals(call.getMethodName()) && Hitbox.class.getName().equals(call.getClassName())) {
                        call.replace("{ sts1portrait.PortraitTouchControls.updateMyScore($0); }");
                    }
                }

                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && InputHelper.class.getName().equals(field.getClassName())
                            && "justClickedLeft".equals(field.getFieldName())) {
                        field.replace("{ $_ = $proceed() && (!sts1portrait.PortraitScreens.enabled()"
                                + " || !com.megacrit.cardcrawl.helpers.input.InputHelper.justReleasedClickLeft); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = DailyScreen.class, method = "renderViewMyScoreBox", paramtypez = {SpriteBatch.class})
    public static class DailyScoreDrawing {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(DailyScreen __instance, SpriteBatch sb, Hitbox ___viewMyScoreHb,
                boolean ___waiting) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            if (!___waiting) MY_SCORE.draw(sb, ___viewMyScoreHb, DailyScreen.TEXT_2[5], null);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = LeaderboardScreen.class, method = "renderViewMyScoreBox", paramtypez = {SpriteBatch.class})
    public static class LeaderboardScoreDrawing {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(LeaderboardScreen __instance, SpriteBatch sb, Hitbox ___viewMyScoreHb,
                boolean ___waiting) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            if (!___waiting && !__instance.regionButtons.isEmpty() && !__instance.regionButtons.get(0).active) {
                MY_SCORE.draw(sb, ___viewMyScoreHb, LeaderboardScreen.TEXT[5], null);
            }
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = CreditsScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class CreditsSkipDrawing {
        @SpirePostfixPatch
        public static void after(CreditsScreen __instance, SpriteBatch sb) {
            if (PortraitScreens.enabled()) CREDITS_SKIP.draw(sb, CREDITS_HITBOX, SkipCardButton.TEXT[0], null);
        }
    }

    @SpirePatch(clz = CreditsScreen.class, method = "update", paramtypez = {})
    public static class CreditsSkipPointer {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(CreditsScreen __instance) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            CREDITS_SKIP.mapButton(CREDITS_HITBOX, true);
            try {
                CREDITS_HITBOX.update();
                if (CREDITS_HITBOX.hovered && InputHelper.justClickedLeft) CREDITS_HITBOX.clickStarted = true;
            } finally {
                CREDITS_SKIP.restoreButton();
            }
            if (!CREDITS_HITBOX.clicked) return SpireReturn.Continue();
            CREDITS_HITBOX.clicked = false;
            __instance.close();
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = CreditsScreen.class, method = "skipLogic", paramtypez = {})
    public static class CreditsSkipTrigger {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && InputHelper.class.getName().equals(field.getClassName())
                            && "justClickedLeft".equals(field.getFieldName())) {
                        field.replace("{ $_ = $proceed() && !sts1portrait.PortraitScreens.enabled(); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = CardLibraryScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class LibraryUpgradeDrawing {
        @SpirePostfixPatch
        public static void after(CardLibraryScreen __instance, SpriteBatch sb, ColorTabBar ___colorBar) {
            if (libraryUpgrade()) VIEW_UPGRADE.draw(sb, ___colorBar.viewUpgradeHb, CardLibraryScreen.TEXT[7],
                    SingleCardViewPopup.isViewingUpgrade);
        }
    }

    @SpirePatch(clz = CardLibraryScreen.class, method = "update", paramtypez = {})
    public static class LibraryCardPointer {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && InputHelper.class.getName().equals(field.getClassName())
                            && "justClickedLeft".equals(field.getFieldName())) {
                        field.replace("{ $_ = $proceed()"
                                + " && !sts1portrait.PortraitTouchControls.libraryUpgradeHovered(colorBar); }");
                    }
                }
            };
        }
    }

    private static Control auxiliaryControl(Object button) {
        if (!PortraitScreens.enabled()) return null;
        if (button instanceof PeekButton) return PEEK;
        if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.CARD_REWARD) return null;
        if (button instanceof SingingBowlButton) return BOWL;
        SingingBowlButton bowl = ReflectionHacks.getPrivate(AbstractDungeon.cardRewardScreen,
                CardRewardScreen.class, "bowlButton");
        return bowl.isHidden() ? null : REWARD_SKIP;
    }

    @SpirePatch(clz = SingingBowlButton.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = SkipCardButton.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class AuxiliaryDrawing {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(Object __instance, SpriteBatch sb, Hitbox ___hb, boolean ___isHidden) {
            Control control = auxiliaryControl(__instance);
            if (control == null) return SpireReturn.Continue();
            if (!___isHidden) {
                String label = control == BOWL ? SingingBowlButton.TEXT[2] : SkipCardButton.TEXT[0];
                control.draw(sb, ___hb, label, null);
            }
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = PeekButton.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class PeekDrawing {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    String method = call.getMethodName();
                    // The original render method also consumes the click and changes the overlay state.
                    if ("renderButton".equals(method) || "renderControllerUi".equals(method)
                            || ("draw".equals(method) && SpriteBatch.class.getName().equals(call.getClassName()))) {
                        call.replace("{ if (!sts1portrait.PortraitScreens.enabled()) $proceed($$); }");
                    }
                }
            };
        }

        @SpirePostfixPatch
        public static void after(PeekButton __instance, SpriteBatch sb, boolean ___isHidden) {
            if (PortraitScreens.enabled() && !___isHidden) PEEK.draw(sb, __instance.hb, null, null);
        }
    }

    @SpirePatch(clz = PeekButton.class, method = "update", paramtypez = {})
    @SpirePatch(clz = SingingBowlButton.class, method = "update", paramtypez = {})
    @SpirePatch(clz = SkipCardButton.class, method = "update", paramtypez = {})
    public static class AuxiliaryPointer {
        private static Control mapped;

        @SpirePrefixPatch
        public static void before(Object __instance, Hitbox ___hb, boolean ___isHidden) {
            mapped = ___isHidden ? null : auxiliaryControl(__instance);
            if (mapped != null) mapped.mapButton(___hb, true);
        }

        @SpirePostfixPatch
        public static void after() {
            if (mapped != null) mapped.restoreButton();
        }
    }

    @SpirePatch(clz = PeekButton.class, method = "update", paramtypez = {})
    public static class PeekParticles {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                private boolean particlesGuard;

                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (!particlesGuard && field.isReader() && "isPeeking".equals(field.getFieldName())) {
                        particlesGuard = true;
                        field.replace("{ $_ = !sts1portrait.PortraitScreens.enabled() && $proceed(); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = CampfireUI.class, method = "render")
    public static class CampfireWorld {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    boolean fire = CampfireUI.class.getName().equals(call.getClassName())
                            && "renderFire".equals(call.getMethodName());
                    if (!fire && !("com.megacrit.cardcrawl.characters.AbstractPlayer".equals(call.getClassName())
                            && "render".equals(call.getMethodName()))) return;
                    call.replace("{ if (!sts1portrait.PortraitScreens.enabled()) { $proceed($$); } else {"
                            + " sts1portrait.PortraitTouchControls.beginWorld($1, " + fire + ");"
                            + " try { $proceed($$); } finally { sts1portrait.PortraitTouchControls.endWorld($1); } } }");
                }
            };
        }
    }

    public static void beginWorld(SpriteBatch batch, boolean fire) {
        worldPrevious.set(batch.getTransformMatrix());
        Matrix4 base = PortraitScreens.baseMatrix(batch);
        if (fire) {
            float nativeX = 1485f * Settings.xScale;
            float nativeY = Settings.HEIGHT * .5f - 200f * Settings.yScale;
            batch.setTransformMatrix(fireTransform.set(base)
                    .translate(PortraitViewport.x(.76f) - .72f * nativeX,
                            PortraitViewport.y(.08f) - .72f * nativeY, 0f)
                    .scale(.72f, .72f, 1f));
        } else {
            batch.setTransformMatrix(base);
        }
    }

    public static void endWorld(SpriteBatch batch) { batch.setTransformMatrix(worldPrevious); }

    public static void fadeCampfireEdge(AbstractScene scene, SpriteBatch batch) {
        if (!PortraitViewport.active()) return;
        if (scene != fadeScene) {
            fadeScene = scene;
            fadeBackdrop = ReflectionHacks.getPrivate(scene, AbstractScene.class, "campfireBg");
        }
        if (fadeBackdrop == null) return;
        float top = (fadeBackdrop.offsetY + fadeBackdrop.packedHeight) * Settings.scale + 2f;
        float bottom = top - PortraitViewport.contentWidth() * .19f;
        edgeFade[10] = edgeFade[15] = Settings.WIDTH;
        edgeFade[6] = edgeFade[11] = top;
        edgeFade[1] = edgeFade[16] = bottom;
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, edgeFade, 0, edgeFade.length);
    }
}
