package sts1portrait;

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
        float step = PortraitViewport.contentHeight() * 0.048f;
        Hitbox hitbox = button.hb;
        hitbox.width = PortraitViewport.contentWidth() * 0.74f;
        hitbox.height = step * 0.94f;
        hitbox.move(PortraitViewport.x(.5f), PortraitViewport.y(.105f) + index * step);
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
                                + " sts1portrait.PortraitUi.drawMenuLabel($1, $2, $3, this.hb);"
                                + " else $proceed($$); }");
                    } else if (SpriteBatch.class.getName().equals(call.getClassName())
                            && "draw".equals(call.getMethodName())) {
                        call.replace("{ if (!sts1portrait.PortraitViewport.active()) $proceed($$); }");
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

    @SpirePatch(clz = NeowRoom.class, method = "onPlayerEntry", paramtypez = {})
    public static class NeowPlayer {
        @SpirePostfixPatch
        public static void postfix(NeowRoom __instance) {
            AbstractPlayer player = AbstractDungeon.player;
            if (PortraitViewport.active() && player != null) {
                player.movePosition(PortraitViewport.x(0.19f), PortraitViewport.y(0.465f));
            }
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
                                + " $1 = sts1portrait.PortraitViewport.x(0.65f);"
                                + " $2 = sts1portrait.PortraitViewport.y(0.455f); } $_ = $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = NeowEvent.class, method = "talk", paramtypez = {String.class})
    @SpirePatch(clz = RoomEventDialog.class, method = "updateBodyText",
            paramtypez = {String.class, DialogWord.AppearEffect.class})
    public static class NeowText {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isReader()) return;
                    String value;
                    String condition = "sts1portrait.OpeningPatches.neowActive()";
                    switch (field.getFieldName()) {
                        case "DIALOG_X":
                            condition = "sts1portrait.PortraitViewport.active()";
                            value = "x(0.47f)";
                            break;
                        case "DIALOG_Y":
                            condition = "sts1portrait.PortraitViewport.active()";
                            value = "y(0.43f)";
                            break;
                        case "DIALOG_MSG_X": value = "x(0.08f)"; break;
                        case "DIALOG_MSG_Y": value = "y(0.38f)"; break;
                        case "DIALOG_MSG_W": value = "contentWidth() * 0.84f"; break;
                        default: return;
                    }
                    field.replace("{ $_ = " + condition + " ? sts1portrait.PortraitViewport."
                            + value + " : $proceed(); }");
                }
            };
        }
    }

}
