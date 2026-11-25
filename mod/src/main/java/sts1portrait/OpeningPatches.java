package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.neow.NeowRoom;
import com.megacrit.cardcrawl.scenes.TitleBackground;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuButton;
import com.megacrit.cardcrawl.ui.DialogWord;
import com.megacrit.cardcrawl.ui.panels.TopPanel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

public final class OpeningPatches {
    private OpeningPatches() {
    }

    public static boolean neowActive() {
        return PortraitViewport.active() && AbstractDungeon.currMapNode != null
                && AbstractDungeon.getCurrRoom() instanceof NeowRoom;
    }

    @SpirePatch(clz = TitleBackground.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class Title {
        @SpirePrefixPatch
        public static SpireReturn<Void> prefix(TitleBackground __instance, SpriteBatch batch) {
            if (!PortraitViewport.active()) return SpireReturn.Continue();
            PortraitUi.renderTitle(__instance, batch);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = MenuButton.class, method = SpirePatch.CONSTRUCTOR,
            paramtypez = {MenuButton.ClickResult.class, int.class})
    public static class MenuItemPlacement {
        @SpirePostfixPatch
        public static void postfix(MenuButton __instance, MenuButton.ClickResult result, int index, String ___label) {
            if (!PortraitViewport.active()) return;
            placeMenuItem(__instance, index);
        }
    }

    private static void placeMenuItem(MenuButton button, int index) {
        if (button == null || button.hb == null) return;
        ArrayList<MenuButton> all = CardCrawlGame.mainMenuScreen != null
                ? CardCrawlGame.mainMenuScreen.buttons : null;
        if (all == null || all.isEmpty() || !all.contains(button)) {
            float step = PortraitViewport.contentHeight() * 0.056f;
            button.hb.width = PortraitViewport.contentWidth() * 0.84f;
            button.hb.height = step * 0.90f;
            button.hb.move(PortraitViewport.x(0.5f), PortraitViewport.y(0.088f) + index * step);
            return;
        }

        MenuButton hero = null;
        MenuButton abandon = null;
        ArrayList<MenuButton> secondary = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            MenuButton item = all.get(i);
            if (item == null) continue;
            if (item.result == MenuButton.ClickResult.PLAY || item.result == MenuButton.ClickResult.RESUME_GAME) {
                hero = item;
            } else if (item.result == MenuButton.ClickResult.ABANDON_RUN) {
                abandon = item;
            } else {
                secondary.add(item);
            }
        }
        Collections.sort(secondary, new Comparator<MenuButton>() {
            @Override
            public int compare(MenuButton a, MenuButton b) {
                boolean qa = a.result == MenuButton.ClickResult.QUIT;
                boolean qb = b.result == MenuButton.ClickResult.QUIT;
                if (qa != qb) return qa ? 1 : -1;
                int ia = ReflectionHacks.<Integer>getPrivate(a, MenuButton.class, "index");
                int ib = ReflectionHacks.<Integer>getPrivate(b, MenuButton.class, "index");
                return Integer.compare(ib, ia);
            }
        });

        float W = PortraitViewport.contentWidth();
        float H = PortraitViewport.contentHeight();
        float menuW = W * 0.84f;
        float colGap = W * 0.030f;
        float tileW = (menuW - colGap) * 0.5f;

        int rows = (secondary.size() + 1) / 2;
        float tileH = H * 0.050f;
        float rowGap = H * 0.014f;
        float sectionGap = H * 0.022f;
        float abandonH = H * 0.048f;
        float abandonGap = H * 0.015f;
        float heroH = H * 0.062f;

        float secHeight = rows > 0 ? rows * tileH + (rows - 1) * rowGap : 0f;
        float topSection = (hero != null ? heroH : 0f)
                + (abandon != null ? (hero != null ? abandonGap : 0f) + abandonH : 0f);
        float totalH = secHeight + (rows > 0 && topSection > 0f ? sectionGap : 0f) + topSection;
        float maxH = H * 0.46f;
        if (totalH > maxH && totalH > 1f) {
            float s = maxH / totalH;
            tileH *= s;
            rowGap *= s;
            sectionGap *= s;
            abandonH *= s;
            abandonGap *= s;
            heroH *= s;
            secHeight = rows > 0 ? rows * tileH + (rows - 1) * rowGap : 0f;
        }

        float centerX = PortraitViewport.x(0.50f);
        float bottomY = PortraitViewport.y(0.088f);
        float secTopY = bottomY + secHeight;

        if (button == hero) {
            float y = secTopY + (rows > 0 ? sectionGap : 0f)
                    + (abandon != null ? abandonH + abandonGap : 0f) + heroH * 0.5f;
            button.hb.width = menuW;
            button.hb.height = heroH;
            button.hb.move(centerX, y);
            return;
        }
        if (button == abandon) {
            float y = secTopY + (rows > 0 ? sectionGap : 0f) + abandonH * 0.5f;
            button.hb.width = menuW;
            button.hb.height = abandonH;
            button.hb.move(centerX, y);
            return;
        }

        int pos = secondary.indexOf(button);
        if (pos < 0) return;
        int row = pos / 2;
        boolean hasPair = (row * 2 + 1) < secondary.size();
        float rowCy = secTopY - row * (tileH + rowGap) - tileH * 0.5f;
        if (!hasPair) {
            button.hb.width = menuW;
            button.hb.height = tileH;
            button.hb.move(centerX, rowCy);
        } else {
            boolean rightCol = (pos % 2) == 1;
            float colOffset = (tileW + colGap) * 0.5f;
            button.hb.width = tileW;
            button.hb.height = tileH;
            button.hb.move(rightCol ? centerX + colOffset : centerX - colOffset, rowCy);
        }
    }

    @SpirePatch(clz = MenuButton.class, method = "update", paramtypez = {})
    public static class MenuItemInput {
        @SpirePrefixPatch
        public static void before(MenuButton __instance, int ___index) {
            if (PortraitViewport.active()) placeMenuItem(__instance, ___index);
        }
    }

    @SpirePatch(clz = MenuButton.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class MenuItemStyle {
        @SpirePrefixPatch
        public static SpireReturn<Void> prefix(MenuButton __instance, SpriteBatch batch, int ___index, boolean ___hidden) {
            if (PortraitViewport.active()) {
                if (CardCrawlGame.mainMenuScreen != null
                        && CardCrawlGame.mainMenuScreen.screen != MainMenuScreen.CurScreen.MAIN_MENU) {
                    return SpireReturn.Return();
                }
                placeMenuItem(__instance, ___index);
            }
            return SpireReturn.Continue();
        }

        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (FontHelper.class.getName().equals(call.getClassName())
                            && "renderSmartText".equals(call.getMethodName())) {
                        call.replace("{ if (sts1portrait.PortraitViewport.active())"
                                + " sts1portrait.PortraitUi.drawMenuButton($1, this, $2, $3);"
                                + " else $proceed($$); }");
                    } else if (SpriteBatch.class.getName().equals(call.getClassName())
                            && "draw".equals(call.getMethodName())) {
                        call.replace("{ if (!sts1portrait.PortraitViewport.active()) $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = MainMenuScreen.class, method = "updateRenameArea", paramtypez = {})
    public static class ProfileBadgeUpdate {
        @SpirePrefixPatch
        public static void before(MainMenuScreen __instance, Hitbox ___nameEditHb) {
            if (PortraitViewport.active()) {
                PortraitUi.placeProfileBadge(__instance, ___nameEditHb);
            }
        }
    }

    @SpirePatch(clz = MainMenuScreen.class, method = "renderNameEdit", paramtypez = {SpriteBatch.class})
    public static class ProfileBadgeRender {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(MainMenuScreen __instance, SpriteBatch sb, Hitbox ___nameEditHb) {
            if (!PortraitViewport.active()) return SpireReturn.Continue();
            PortraitUi.renderProfileBadge(__instance, sb, ___nameEditHb);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = MainMenuScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class MainMenuVersion {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (FontHelper.class.getName().equals(call.getClassName())
                            && "renderSmartText".equals(call.getMethodName())) {
                        call.replace("{ if (sts1portrait.PortraitViewport.active())"
                                + " sts1portrait.PortraitUi.drawVersionInfo($1, this, $3);"
                                + " else $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(cls = "basemod.patches.com.megacrit.cardcrawl.screens.mainMenu.DisplayRunInfo$Render",
            method = "Insert", paramtypez = {MainMenuScreen.class, SpriteBatch.class}, optional = true)
    public static class HideCurrentRunPreview {
        @SpirePrefixPatch
        public static SpireReturn<Void> before() {
            return PortraitViewport.active() ? SpireReturn.Return() : SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = AbstractDungeon.class, method = "update", paramtypez = {})
    public static class TopPanelInput {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (TopPanel.class.getName().equals(call.getClassName()) && "update".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitUi.updateTopPanel($0); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = AbstractDungeon.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class TopPanelDrawing {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (TopPanel.class.getName().equals(call.getClassName()) && "render".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitUi.renderTopPanel($0, $1); }");
                    }
                }
            };
        }
    }

    static void placeNeowPlayer() {
        AbstractPlayer player = AbstractDungeon.player;
        if (!PortraitViewport.active() || player == null) return;
        float x = PortraitViewport.x(0.24f);
        float y = PortraitViewport.y(0.425f);
        if (Math.abs(player.drawX - x) > 0.5f || Math.abs(player.drawY - y) > 0.5f) {
            player.movePosition(x, y);
        }
    }

    @SpirePatch(clz = NeowRoom.class, method = "onPlayerEntry", paramtypez = {})
    public static class NeowPlayer {
        @SpirePostfixPatch
        public static void postfix(NeowRoom __instance) {
            placeNeowPlayer();
        }
    }

    @SpirePatch(clz = NeowRoom.class, method = "update", paramtypez = {})
    public static class NeowPlayerUpdate {
        @SpirePrefixPatch
        public static void before(NeowRoom __instance) {
            placeNeowPlayer();
        }
    }

    @SpirePatch(clz = NeowRoom.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class NeowPlayerRender {
        @SpirePrefixPatch
        public static void before(NeowRoom __instance, SpriteBatch sb) {
            placeNeowPlayer();
        }
    }

    @SpirePatch(clz = NeowEvent.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {boolean.class})
    public static class NeowNpc {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(NewExpr expr) throws CannotCompileException {
                    if ("com.megacrit.cardcrawl.characters.AnimatedNpc".equals(expr.getClassName())) {
                        expr.replace("{ if (sts1portrait.PortraitViewport.active()) {"
                                + " $1 = sts1portrait.PortraitViewport.x(0.74f);"
                                + " $2 = sts1portrait.PortraitViewport.y(0.485f); } $_ = $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = NeowEvent.class, method = "talk", paramtypez = {String.class})
    public static class NeowText {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isReader()) return;
                    String value;
                    switch (field.getFieldName()) {
                        case "DIALOG_X": value = "x(0.56f)"; break;
                        case "DIALOG_Y": value = "y(0.675f)"; break;
                        default: return;
                    }
                    field.replace("{ $_ = sts1portrait.PortraitViewport.active() ? sts1portrait.PortraitViewport."
                            + value + " : $proceed(); }");
                }
            };
        }
    }

    private static final com.badlogic.gdx.math.Matrix4 neowBubbleBase = new com.badlogic.gdx.math.Matrix4();
    private static final com.badlogic.gdx.math.Matrix4 neowBubbleScaled = new com.badlogic.gdx.math.Matrix4();
    private static final float NEOW_BUBBLE_SCALE = 1.48f;

    @SpirePatch(clz = com.megacrit.cardcrawl.vfx.InfiniteSpeechBubble.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = com.megacrit.cardcrawl.vfx.SpeechTextEffect.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class NeowSpeechScale {
        @SpirePrefixPatch
        public static void before(Object __instance, SpriteBatch batch, float ___x, float ___y) {
            if (!neowActive()) return;
            neowBubbleBase.set(batch.getTransformMatrix());
            neowBubbleScaled.set(neowBubbleBase)
                    .translate(___x * (1f - NEOW_BUBBLE_SCALE), ___y * (1f - NEOW_BUBBLE_SCALE), 0f)
                    .scale(NEOW_BUBBLE_SCALE, NEOW_BUBBLE_SCALE, 1f);
            batch.setTransformMatrix(neowBubbleScaled);
        }

        @SpirePostfixPatch
        public static void after(Object __instance, SpriteBatch batch) {
            if (neowActive()) batch.setTransformMatrix(neowBubbleBase);
        }
    }

}
