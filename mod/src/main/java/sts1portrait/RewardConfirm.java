package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.ui.buttons.ConfirmButton;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/** Keeps the reward confirmation in its original lower-right corner in portrait. */
public final class RewardConfirm {
    private static final Matrix4 original = new Matrix4();
    private static final Matrix4 moved = new Matrix4();


    private RewardConfirm() {}

    private static float offsetX(ConfirmButton button, CardRewardScreen screen, float scale) {
        float rightInRewardSpace = PortraitScreens.sourceCenterX(screen)
                + (Settings.WIDTH - PortraitViewport.x(0.5f)) / scale;
        return rightInRewardSpace - button.hb.width * 0.5f - button.hb.cX;
    }

    private static float offsetY(ConfirmButton button, CardRewardScreen screen, float scale) {
        float centerInRewardSpace = PortraitScreens.sourceCenterY(screen)
                + (PortraitScreens.cornerButtonY(scale) - PortraitScreens.cardRewardCenterY(screen)) / scale;
        return centerInRewardSpace - button.hb.cY;
    }

    public static void render(ConfirmButton button, SpriteBatch batch, CardRewardScreen screen) {
        if (!PortraitScreens.isDrawingCardReward()) {
            button.render(batch);
            return;
        }
        if (ReflectionHacks.<Boolean>getPrivate(button, ConfirmButton.class, "isHidden")) return;
        float scale = PortraitScreens.scale(screen);
        float x = offsetX(button, screen, scale);
        float y = offsetY(button, screen, scale);
        original.set(batch.getTransformMatrix());
        batch.setTransformMatrix(moved.set(original).translate(x, y, 0));
        try {
            button.render(batch);
        } finally {
            batch.setTransformMatrix(original);
        }
    }

    public static void update(ConfirmButton button, CardRewardScreen screen) {
        if (!PortraitScreens.enabled()) {
            button.update();
            return;
        }
        float scale = PortraitScreens.scale(screen);
        int x = InputHelper.mX;
        int y = InputHelper.mY;
        InputHelper.mX = Math.round((x - offsetX(button, screen, scale)));
        InputHelper.mY = Math.round((y - offsetY(button, screen, scale)));
        try {
            button.update();
        } finally {
            InputHelper.mX = x;
            InputHelper.mY = y;
        }
    }

    public static void show(ConfirmButton button) {
        button.show();
        if (PortraitScreens.enabled()) {
            // The original entrance slides in from the right. Align the graphic
            // with the relocated hitbox as soon as the selection is made.
            float target = ReflectionHacks.getPrivate(button, ConfirmButton.class, "target_x");
            ReflectionHacks.setPrivate(button, ConfirmButton.class, "current_x", target);
        }
    }

    @SpirePatch(clz = CardRewardScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = CardRewardScreen.class, method = "update", paramtypez = {})
    @SpirePatch(clz = CardRewardScreen.class, method = "cardSelectUpdate", paramtypez = {})
    public static class RewardButtonCalls {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!ConfirmButton.class.getName().equals(call.getClassName())) return;
                    switch (call.getMethodName()) {
                        case "render":
                            call.replace("{ sts1portrait.RewardConfirm.render($0, $1, this); }");
                            break;
                        case "update":
                            call.replace("{ sts1portrait.RewardConfirm.update($0, this); }");
                            break;
                        case "show":
                            call.replace("{ sts1portrait.RewardConfirm.show($0); }");
                            break;
                        default:
                            break;
                    }
                }
            };
        }
    }
}
