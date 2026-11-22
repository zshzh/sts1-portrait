package sts1portrait;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.compendium.RelicViewScreen;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitRelicLibrary {
    private static final Matrix4 original = new Matrix4();
    private static final Matrix4 moved = new Matrix4();

    private PortraitRelicLibrary() {}

    public static void beginIcon(SpriteBatch batch, AbstractRelic relic) {
        original.set(batch.getTransformMatrix());
        float scale = 1.55f;
        moved.set(original).translate(relic.currentX * (1 - scale), relic.currentY * (1 - scale), 0)
                .scale(scale, scale, 1);
        batch.setTransformMatrix(moved);
    }

    public static void endIcon(SpriteBatch batch) {
        batch.setTransformMatrix(original);
    }

    public static void updateRelic(AbstractRelic relic) {
        if (PortraitScreens.enabled()) {
            relic.hb.width = PortraitViewport.contentWidth() * 0.083f;
            relic.hb.height = com.megacrit.cardcrawl.core.Settings.scale * 116f;
            relic.hb.move(relic.currentX, relic.currentY);
        }
        relic.update();
    }

    @SpirePatch(clz = RelicViewScreen.class, method = "renderList",
            paramtypez = {SpriteBatch.class, String.class, String.class, ArrayList.class})
    @SpirePatch(clz = RelicViewScreen.class, method = "updateList", paramtypez = {ArrayList.class})
    @SpirePatch(clz = RelicViewScreen.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {})
    public static class Library {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isWriter() && "currentX".equals(field.getFieldName())) {
                        field.replace("{ $proceed(sts1portrait.PortraitScreens.enabled()"
                                + " ? sts1portrait.PortraitViewport.x(0.095f + col * 0.089f) : $1); }");
                    } else if (field.isReader() && "SPACE".equals(field.getFieldName())) {
                        field.replace("{ $_ = $proceed() * (sts1portrait.PortraitScreens.enabled() ? 1.55f : 1f); }");
                    } else if (field.isWriter() && "scrollUpperBound".equals(field.getFieldName())) {
                        field.replace("{ $proceed($1 * (sts1portrait.PortraitScreens.enabled() ? 1.55f : 1f)); }");
                    }
                }

                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!AbstractRelic.class.getName().equals(call.getClassName())) return;
                    String name = call.getMethodName();
                    if ("update".equals(name)) {
                        call.replace("{ sts1portrait.PortraitRelicLibrary.updateRelic($0); }");
                    } else if ("render".equals(name) || "renderLock".equals(name)) {
                        call.replace("{ if (!sts1portrait.PortraitScreens.enabled()) { $proceed($$); } else {"
                                + " sts1portrait.PortraitRelicLibrary.beginIcon($1, $0);"
                                + " try { $proceed($$); } finally { sts1portrait.PortraitRelicLibrary.endIcon($1); } } }");
                    }
                }
            };
        }
    }
}
