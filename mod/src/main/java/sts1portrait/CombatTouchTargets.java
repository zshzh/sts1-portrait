package sts1portrait;

import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

public final class CombatTouchTargets {
    private static final IdentityHashMap<Hitbox, Target> targets = new IdentityHashMap<>();
    private static final ArrayList<Target> available = new ArrayList<>();
    private static int used;

    private CombatTouchTargets() {
    }

    static void reset() {
        targets.clear();
        used = 0;
    }

    static void place(Hitbox hitbox, float x, float y, float width, float height) {
        if (used == available.size()) available.add(new Target());
        Target target = available.get(used++);
        target.x = x;
        target.y = y;
        target.width = width;
        target.height = height;
        targets.put(hitbox, target);
    }

    public static float pointerX(Hitbox hitbox, int x) {
        Target target = targets.get(hitbox);
        return target == null ? x : hitbox.x + hitbox.width * (0.5f + (x - target.x) / target.width);
    }

    public static float pointerY(Hitbox hitbox, int y) {
        Target target = targets.get(hitbox);
        return target == null ? y : hitbox.y + hitbox.height * (0.5f + (y - target.y) / target.height);
    }

    private static final class Target {
        float x;
        float y;
        float width;
        float height;
    }

    @SpirePatch(cls = "io.stamethyst.compatmod.touch.TouchscreenCardInputRuntime",
            method = "isInputInsideHitbox", paramtypez = {Hitbox.class, float.class},
            requiredModId = "amethystruntimecompat", optional = true)
    public static class AmethystTargetAssist {
        @SpirePrefixPatch
        public static SpireReturn<Boolean> prefix(Hitbox hitbox, float padding) {
            Target target = targets.get(hitbox);
            if (target == null) return SpireReturn.Continue();
            return SpireReturn.Return(Math.abs(InputHelper.mX - target.x) <= target.width * 0.5f + padding
                    && Math.abs(InputHelper.mY - target.y) <= target.height * 0.5f + padding);
        }
    }

    @SpirePatch(clz = Hitbox.class, method = "update", paramtypez = {float.class, float.class})
    public static class HitTest {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isReader() || !InputHelper.class.getName().equals(field.getClassName())) return;
                    String axis = field.getFieldName();
                    if ("mX".equals(axis) || "mY".equals(axis)) {
                        field.replace("{ $_ = (int) sts1portrait.CombatTouchTargets.pointer"
                                + axis.substring(1) + "(this, $proceed()); }");
                    }
                }
            };
        }
    }
}
