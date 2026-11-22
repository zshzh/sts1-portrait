package sts1portrait;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.helpers.FontHelper;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class RewardCardText {
    private static final Matrix4 screenTransform = new Matrix4();
    private static final Matrix4 combinedTransform = new Matrix4();

    private RewardCardText() {}

    public static Matrix4 compose(SpriteBatch batch, Matrix4 textTransform) {
        if (!PortraitScreens.isDrawingCardReward()) return textTransform;
        screenTransform.set(batch.getTransformMatrix());
        return combinedTransform.set(screenTransform).mul(textTransform);
    }

    public static Matrix4 restore(Matrix4 fallback) {
        return PortraitScreens.isDrawingCardReward() ? screenTransform : fallback;
    }

    @SpirePatch(clz = FontHelper.class, method = "renderRotatedText",
            paramtypez = {SpriteBatch.class, BitmapFont.class, String.class, float.class, float.class,
                    float.class, float.class, float.class, boolean.class, Color.class})
    public static class PreserveRewardLayout {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                private boolean entering = true;

                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!SpriteBatch.class.getName().equals(call.getClassName())
                            || !"setTransformMatrix".equals(call.getMethodName())) return;
                    // The first call enters text space and the second leaves it.
                    String transform = entering ? "compose($0, $1)" : "restore($1)";
                    entering = false;
                    call.replace("{ $proceed(sts1portrait.RewardCardText." + transform + "); }");
                }
            };
        }
    }
}
