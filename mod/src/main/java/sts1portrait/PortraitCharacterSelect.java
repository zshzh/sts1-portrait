package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.CharSelectInfo;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.ui.buttons.ConfirmButton;
import com.megacrit.cardcrawl.ui.panels.SeedPanel;
import com.megacrit.cardcrawl.screens.custom.CustomModeScreen;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class PortraitCharacterSelect {
    private static final Matrix4 original = new Matrix4();
    private static final Matrix4 moved = new Matrix4();
    private static final Widget ART = new Widget("portrait-art", "Character artwork", "角色贴图", .5f, .74f, .94f, .40f);
    private static final Widget INFO = new Widget("character-info", "Character description", "角色介绍", .5f, .439f, .94f, .175f);
    private static final Widget SEED = new Widget("character-seed", "Set seed", "设置种子", .27f, .202f, .43f, .044f);
    private static final Widget ASC = new Widget("character-ascension", "Ascension", "进阶开关", .73f, .202f, .43f, .044f);
    private static final Widget DOWN = new Widget("character-ascension-down", "Previous ascension", "降低进阶", .552f, .143f, .105f, .044f);
    private static final Widget UP = new Widget("character-ascension-up", "Next ascension", "提高进阶", .908f, .143f, .105f, .044f);
    private static final Widget BACK = new Widget("back", "Back", "返回", .258f, .07f, .41f, .053f);
    private static final Widget START = new Widget("character-embark", "Embark", "启程", .742f, .07f, .41f, .053f);
    private static final PortraitRichText description = new PortraitRichText();
    private static final PortraitRichText relicText = new PortraitRichText();
    private static final PortraitRichText ascensionText = new PortraitRichText();

    private static final Color DIM = new Color(0f, 0f, 0f, .66f);
    private static float iconScale = 1f;

    private PortraitCharacterSelect() {}

    private static final class Widget {

        final String chinese;
        final float cx, cy, w, h;
        float x, y, width, height;

        Widget(String id, String title, String chinese, float cx, float cy, float w, float h) {

            this.chinese = chinese;
            this.cx = cx;
            this.cy = cy;
            this.w = w;
            this.h = h;
        }

        void place() {
            x = PortraitViewport.x(cx);
            y = PortraitViewport.y(cy);
            width = PortraitViewport.contentWidth() * w;
            height = PortraitViewport.contentHeight() * h;
        }


        void setHitbox(Hitbox target) {
            if (target == null) return;
            place();
            target.width = width;
            target.height = height;
            target.move(x, y);
        }

        void map(Hitbox target, int mouseX, int mouseY, boolean blocked) {
            place();
            boolean hover = !blocked && Math.abs(mouseX - x) < width * .5f && Math.abs(mouseY - y) < height * .5f;
            InputHelper.mX = hover ? Math.round(target.cX) : -10000;
            InputHelper.mY = hover ? Math.round(target.cY) : -10000;
        }

        void button(SpriteBatch batch, Hitbox target, String label, Color normal) {
            place();
            PortraitDrawing.rectangle(batch, x - width * .5f, y - height * .5f, width, height,
                    target.hovered ? PortraitSettings.ACTIVE : normal);
            PortraitSettings.label(batch, label, x, y, width * .87f, height * .35f, true);
        }
    }

    private static Hitbox field(CharacterSelectScreen screen, String key) {
        return ReflectionHacks.getPrivate(screen, CharacterSelectScreen.class, key);
    }

    private static boolean seedOpen(CharacterSelectScreen screen) {
        return seedPanel(screen).shown;
    }

    private static SeedPanel seedPanel(CharacterSelectScreen screen) {
        return ReflectionHacks.getPrivate(screen, CharacterSelectScreen.class, "seedPanel");
    }

    private static boolean usesPortraitSeed(SeedPanel panel) {
        if (!PortraitScreens.enabled() || CardCrawlGame.mainMenuScreen == null) return false;
        CharacterSelectScreen character = CardCrawlGame.mainMenuScreen.charSelectScreen;
        if (character != null && seedPanel(character) == panel) return true;
        CustomModeScreen custom = CardCrawlGame.mainMenuScreen.customModeScreen;
        return custom != null && ReflectionHacks.<SeedPanel>getPrivate(custom, CustomModeScreen.class, "seedPanel") == panel;
    }

    private static float seedDialogWidth() { return Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth()) * .79f; }
    private static float seedDialogHeight() { return seedDialogWidth() * .65f; }

    @SpirePatch(clz = SeedPanel.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class SeedDialogDrawing {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(SeedPanel __instance, SpriteBatch sb) {
            if (!usesPortraitSeed(__instance)) return SpireReturn.Continue();
            if (!__instance.shown) return SpireReturn.Return();
            float x = PortraitViewport.x(.5f);
            float y = PortraitViewport.y(.50f);
            float w = seedDialogWidth();
            float h = seedDialogHeight();
            float color = sb.getPackedColor();
            PortraitDrawing.rectangle(sb, 0, 0, Settings.WIDTH, Settings.HEIGHT, DIM);

            try {
                PortraitSettings.frame(sb, x, y + h * .5f, w, h);
                PortraitSettings.label(sb, SeedPanel.TEXT[1], x, y + h * .345f, w * .88f, h * .095f, true);
                PortraitDrawing.rectangle(sb, x - w * .43f, y + h * .04f, w * .86f, h * .18f, PortraitSettings.ROW);
                PortraitSettings.label(sb, SeedPanel.textField, x, y + h * .13f, w * .79f, h * .074f, true);
                seedChoice(sb, __instance.yesHb, SeedPanel.TEXT[2], x - w * .225f, y - h * .12f, w, h);
                seedChoice(sb, __instance.noHb, SeedPanel.TEXT[3], x + w * .225f, y - h * .12f, w, h);
                PortraitSettings.wrappedLabel(sb, SeedPanel.TEXT[Settings.usesTrophies ? 5 : 4], x,
                        y - h * .365f, w * .86f, h * .054f, h * .19f);
            } finally {

                sb.setColor(color);
            }
            return SpireReturn.Return();
        }
    }

    private static void seedChoice(SpriteBatch batch, Hitbox area, String text, float x, float y, float w, float h) {
        PortraitDrawing.rectangle(batch, x - w * .20f, y - h * .10f, w * .40f, h * .20f,
                area.hovered ? PortraitSettings.ACTIVE : PortraitSettings.ROW);
        PortraitSettings.label(batch, text, x, y, w * .35f, h * .077f, true);
    }

    @SpirePatch(clz = SeedPanel.class, method = "update", paramtypez = {})
    public static class SeedDialogInput {
        private static boolean mapped;
        private static int rawX, rawY;

        @SpirePrefixPatch
        public static void before(SeedPanel __instance, @ByRef float[] ___animTimer) {
            mapped = usesPortraitSeed(__instance) && __instance.shown;
            if (!mapped) return;
            // The portrait renderer shows the popup immediately, so its controls open with it.
            ___animTimer[0] = 0f;
            rawX = InputHelper.mX;
            rawY = InputHelper.mY;
            float x = PortraitViewport.x(.5f);
            float y = PortraitViewport.y(.50f);
            float w = seedDialogWidth();
            float h = seedDialogHeight();

            float distance = ((rawX) - x) / w;
            Hitbox target = null;
            if (Math.abs(((rawY) - y) / h + .12f) < .10f
                    && Math.abs(Math.abs(distance) - .225f) < .20f) {
                target = distance < 0 ? __instance.yesHb : __instance.noHb;
            }
            InputHelper.mX = target == null ? -10000 : Math.round(target.cX);
            InputHelper.mY = target == null ? -10000 : Math.round(target.cY);
        }

        @SpirePostfixPatch
        public static void after(SeedPanel __instance) {
            if (!mapped) return;
            InputHelper.mX = rawX;
            InputHelper.mY = rawY;
        }
    }

    private static void placeIcons(CharacterSelectScreen screen) {
        if (!PortraitScreens.enabled() || screen.options.isEmpty()) return;
        float step = 0.90f / screen.options.size();
        iconScale = Math.min(1.43f, PortraitViewport.contentWidth() * step / (200f * Settings.scale));
        float size = Math.min(PortraitViewport.contentWidth() * step * 0.9f, 166f * Settings.scale * iconScale);
        for (int i = 0; i < screen.options.size(); i++) {
            CharacterOption option = screen.options.get(i);
            option.hb.width = option.hb.height = size;
            option.hb.move(PortraitViewport.x(0.05f + (i + 0.5f) * step), PortraitViewport.y(.301f));
        }
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "update")
    @SpirePatch(clz = CharacterSelectScreen.class, method = "render")
    public static class RefreshIcons {
        @SpirePrefixPatch
        public static void before(CharacterSelectScreen __instance) { placeIcons(__instance); }
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "updateAscensionToggle")
    public static class FooterPositions {
        @SpirePrefixPatch
        public static void before(CharacterSelectScreen __instance) {
            if (!PortraitScreens.enabled()) return;
            SEED.setHitbox(field(__instance, "seedHb"));
            ASC.setHitbox(field(__instance, "ascensionModeHb"));
            // The native updater still reads the arrows when ascension is off.
            placeArrow(DOWN, field(__instance, "ascLeftHb"), __instance.isAscensionMode);
            placeArrow(UP, field(__instance, "ascRightHb"), __instance.isAscensionMode);
        }
    }

    private static void placeArrow(Widget arrow, Hitbox area, boolean visible) {
        if (area == null) return;
        if (visible) arrow.setHitbox(area);
        else area.move(-10000f, -10000f);
    }

    public static void drawArtwork(SpriteBatch batch, Texture picture) {
        if (picture == null) return;
        ART.place();
        float sourceScale = Math.max(ART.width / picture.getWidth(), ART.height / picture.getHeight());
        int width = Math.min(picture.getWidth(), Math.round(ART.width / sourceScale));
        int height = Math.min(picture.getHeight(), Math.round(ART.height / sourceScale));
        int left = Math.min(picture.getWidth() - width, Math.max(0, Math.round(picture.getWidth() * .69f - width * .5f)));
        int top = Math.max(0, (picture.getHeight() - height) / 3);
        batch.draw(picture, ART.x - ART.width * .5f, ART.y - ART.height * .5f, ART.width, ART.height,
                left, top, width, height, false, false);
    }

    public static void drawStart(CharacterSelectScreen screen, SpriteBatch batch) {
        ConfirmButton button = screen.confirmButton;
        if (ReflectionHacks.<Boolean>getPrivate(button, ConfirmButton.class, "isHidden")) return;
        String label = ReflectionHacks.getPrivate(button, ConfirmButton.class, "buttonText");
        START.button(batch, button.hb, label, button.isDisabled ? PortraitSettings.ROW : PortraitSettings.ACTIVE);
    }

    public static void updateStart(CharacterSelectScreen screen, ConfirmButton button) {
        if (!PortraitScreens.enabled()) {
            button.update();
            return;
        }
        int x = InputHelper.mX;
        int y = InputHelper.mY;
        START.map(button.hb, x, y, seedOpen(screen));
        try {
            button.update();
        } finally {
            InputHelper.mX = x;
            InputHelper.mY = y;
        }
    }

    static boolean isBack(Object button) {
        return CardCrawlGame.mainMenuScreen != null && CardCrawlGame.mainMenuScreen.charSelectScreen != null
                && CardCrawlGame.mainMenuScreen.charSelectScreen.cancelButton == button;
    }

    static void drawBack(MenuCancelButton button, SpriteBatch batch) {
        if (button.isHidden) return;
        BACK.button(batch, button.hb, ReflectionHacks.getPrivate(button, MenuCancelButton.class, "buttonText"),
                PortraitSettings.ROW);
    }

    static void mapBack(MenuCancelButton button, int x, int y) {
        BACK.map(button.hb, x, y, button.isHidden || seedOpen(CardCrawlGame.mainMenuScreen.charSelectScreen));
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "render")
    @SpirePatch(clz = CharacterSelectScreen.class, method = "updateButtons")
    public static class SelectPresentation {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    String type = call.getClassName();
                    String method = call.getMethodName();
                    String portrait = "sts1portrait.PortraitScreens.enabled()";
                    if (SpriteBatch.class.getName().equals(type) && "draw".equals(method)
                            && "(Lcom/badlogic/gdx/graphics/Texture;FFFFFFFFFIIIIZZ)V".equals(call.getSignature())) {
                        call.replace("{ if (" + portrait + ") sts1portrait.PortraitCharacterSelect.drawArtwork($0,$1);"
                                + " else $proceed($$); }");
                    } else if (ConfirmButton.class.getName().equals(type) && "render".equals(method)) {
                        call.replace("{ if (" + portrait + ") sts1portrait.PortraitCharacterSelect.drawStart(this,$1);"
                                + " else $proceed($$); }");
                    } else if (ConfirmButton.class.getName().equals(type) && "update".equals(method)) {
                        call.replace("{ sts1portrait.PortraitCharacterSelect.updateStart(this,$0); }");
                    } else if (FontHelper.class.getName().equals(type) && "renderFontCentered".equals(method)) {
                        call.replace("{ if (" + portrait + ") $5 = sts1portrait.PortraitViewport.y(.65f); $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "renderSeedSettings")
    public static class SeedControl {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(CharacterSelectScreen __instance, SpriteBatch sb, boolean ___anySelected) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            if (___anySelected) {
                String label = Settings.seedSet ? SeedHelper.getUserFacingSeedString() : CharacterSelectScreen.TEXT[13];
                SEED.button(sb, field(__instance, "seedHb"), label, PortraitSettings.ROW);
            }
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "renderAscensionMode")
    public static class AscensionControls {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(CharacterSelectScreen __instance, SpriteBatch sb, boolean ___anySelected,
                boolean ___isAscensionModeUnlocked) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            if (!___anySelected || !___isAscensionModeUnlocked) return SpireReturn.Return();
            float color = sb.getPackedColor();
            Hitbox mode = field(__instance, "ascensionModeHb");
            ASC.button(sb, mode, CharacterSelectScreen.TEXT[6], PortraitSettings.ROW);
            PortraitSettings.checkmark(sb, ASC.x + ASC.width * .37f, ASC.y, ASC.height * .38f, __instance.isAscensionMode);
            if (__instance.isAscensionMode) {
                DOWN.button(sb, field(__instance, "ascLeftHb"), "<", PortraitSettings.ROW);
                UP.button(sb, field(__instance, "ascRightHb"), ">", PortraitSettings.ROW);
                PortraitSettings.label(sb, CharacterSelectScreen.TEXT[7] + __instance.ascensionLevel,
                        (DOWN.x + UP.x) * .5f, DOWN.y, ASC.width * .48f, ASC.height * .29f, true);
                float width = PortraitViewport.contentWidth() * .43f;
                ascensionText.draw(sb, FontHelper.buttonLabelFont, __instance.ascLevelInfoString, Settings.CREAM_COLOR,
                        PortraitViewport.x(.05f), DOWN.y, width, ASC.height * .9f, ASC.height * .24f, Align.left);
            }
            sb.setColor(color);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = CharacterOption.class, method = "renderInfo")
    public static class CharacterInfo {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(CharacterOption __instance, SpriteBatch sb, CharSelectInfo ___charInfo,
                int ___unlocksRemaining) {
            if (!PortraitScreens.enabled()) return SpireReturn.Continue();
            if (!__instance.selected || ___charInfo == null) return SpireReturn.Return();
            INFO.place();
            float color = sb.getPackedColor();
            float width = INFO.width;
            float height = INFO.height;
            float left = INFO.x - width * .45f;
            PortraitSettings.frame(sb, INFO.x, INFO.y + height * .5f, width, height);
            PortraitSettings.label(sb, __instance.name, left, INFO.y + height * .36f, width * .55f, height * .13f, false);
            float icon = height * .11f;
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.TP_HP, left, INFO.y + height * .185f - icon * .5f, icon, icon);
            sb.draw(ImageMaster.TP_GOLD, left + width * .31f, INFO.y + height * .185f - icon * .5f, icon, icon);
            PortraitSettings.label(sb, ___charInfo.hp, left + icon * 1.24f, INFO.y + height * .185f,
                    width * .22f, height * .074f, false);
            PortraitSettings.label(sb, Integer.toString(___charInfo.gold), left + width * .31f + icon * 1.24f,
                    INFO.y + height * .185f, width * .18f, height * .074f, false);
            description.draw(sb, FontHelper.buttonLabelFont, ___charInfo.flavorText, Settings.CREAM_COLOR,
                    left, INFO.y - height * .003f, width * .90f, height * .27f, height * .084f, Align.left, __instance.c);
            if (___charInfo.relics != null && !___charInfo.relics.isEmpty()) {
                drawRelics(sb, __instance.c, ___charInfo, left, width, height);
            }
            if (___unlocksRemaining > 0) {
                int progress = UnlockTracker.getCurrentProgress(__instance.c.chosenClass);
                int target = UnlockTracker.getCurrentScoreCost(__instance.c.chosenClass);
                PortraitSettings.label(sb, progress + "/" + target, INFO.x + width * .42f, INFO.y + height * .35f,
                        width * .22f, height * .069f, true);
            }
            sb.setColor(color);
            return SpireReturn.Return();
        }
    }

    private static void drawRelics(SpriteBatch batch, AbstractPlayer character, CharSelectInfo info,
            float left, float width, float height) {
        int count = info.relics.size();
        float size = Math.min(height * .23f, width * .86f / count);
        float y = INFO.y - height * .31f;
        batch.setColor(Color.WHITE);
        for (int i = 0; i < count; i++) {
            AbstractRelic relic = RelicLibrary.getRelic(info.relics.get(i));
            batch.draw(relic.img, left + i * size, y - size * .5f, size, size);
            if (count == 1) {
                PortraitSettings.label(batch, relic.name, left + size * 1.18f, y + height * .066f,
                        width * .74f, height * .071f, false);
                relicText.draw(batch, FontHelper.buttonLabelFont, relic.description, Settings.CREAM_COLOR,
                        left + size * 1.18f, y - height * .056f, width * .74f, height * .15f, height * .067f,
                        Align.left, character);
            }
        }
    }

    @SpirePatch(clz = CharacterOption.class, method = "renderOptionButton", paramtypez = {SpriteBatch.class})
    public static class OptionDrawing {
        @SpirePrefixPatch
        public static void before(CharacterOption __instance, SpriteBatch batch) {
            if (!PortraitScreens.enabled()) return;
            Hitbox area = __instance.hb;
            float scale = iconScale;
            original.set(batch.getTransformMatrix());
            moved.set(original).translate(area.cX * (1f - scale),
                    area.cY * (1f - scale), 0f).scale(scale, scale, 1f);
            batch.setTransformMatrix(moved);
        }

        @SpirePostfixPatch
        public static void after(CharacterOption __instance, SpriteBatch batch) {
            if (PortraitScreens.enabled()) batch.setTransformMatrix(original);
        }
    }
}
