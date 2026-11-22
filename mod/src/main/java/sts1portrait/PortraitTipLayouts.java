package sts1portrait;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.ui.buttons.EndTurnButton;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitTipLayouts {
    public static void queueHoveredEndTurn(float x, float y, String title, String body) {
        // Amethyst can deliver taps as mouse events and leave the cursor on the last tap.
        if (PortraitCombatHud.active() && (Settings.isTouchScreen
                || Gdx.app.getType() == Application.ApplicationType.Android
                || System.getProperty("amethyst.bridge.mode") != null)) return;
        TipHelper.renderGenericTip(x, y, title, body);
    }

    @SpirePatch(clz = EndTurnButton.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class EndTurnSource {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (TipHelper.class.getName().equals(call.getClassName())
                            && "renderGenericTip".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitTipLayouts.queueHoveredEndTurn($$); }");
                    }
                }
            };
        }

    }

    private PortraitTipLayouts() {}
}
