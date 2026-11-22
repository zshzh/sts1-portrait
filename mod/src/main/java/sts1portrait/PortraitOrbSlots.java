package sts1portrait;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class PortraitOrbSlots {
    private static final float LEFT = .095f;
    private static final float RIGHT = .905f;
    private static final float TOP_GAP = .063f;
    private static final Matrix4 base = new Matrix4();
    private static final Matrix4 scaled = new Matrix4();

    private PortraitOrbSlots() {}

    static void place(AbstractPlayer player) {
        int count = player.orbs.size();
        for (int index = 0; index < count; index++) {
            place(player.orbs.get(index), index, count);
        }
    }

    private static float unit() {
        return Math.min(PortraitViewport.contentWidth(), PortraitViewport.referenceWidth());
    }

    private static float pitch(int count) {
        return Math.min(unit() * .09f,
                PortraitViewport.contentWidth() * (RIGHT - LEFT) / Math.max(1, count - 1));
    }

    private static void place(AbstractOrb orb, int index, int count) {
        float pitch = pitch(count);
        float span = (count - 1) * pitch;
        float start = Math.max(PortraitViewport.x(LEFT), PortraitCreatureHealth.playerBarX() - span * .5f);
        orb.tX = Math.min(start, PortraitViewport.x(RIGHT) - span) + index * pitch;
        orb.tY = PortraitBattle.playerBattleY() + unit() * TOP_GAP;
        float hitSize = Math.min(96f * Settings.scale, pitch * .86f);
        orb.hb.resize(hitSize, hitSize);
        orb.hb.move(orb.tX, orb.tY);
    }

    public static void render(AbstractPlayer player, AbstractOrb orb, SpriteBatch sb) {
        if (!PortraitBattle.active()) {
            orb.render(sb);
            return;
        }
        // Scale both full and empty orbs when a narrower window or a larger capacity tightens the row.
        float factor = Math.min(1f, pitch(player.orbs.size()) * .90f / (128f * Settings.scale));
        if (factor >= 1f) {
            orb.render(sb);
            return;
        }
        base.set(sb.getTransformMatrix());
        scaled.set(base).translate(orb.cX, orb.cY, 0f).scale(factor, factor, 1f).translate(-orb.cX, -orb.cY, 0f);
        sb.setTransformMatrix(scaled);
        try {
            orb.render(sb);
        } finally {
            sb.setTransformMatrix(base);
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class OrbDrawing {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (AbstractOrb.class.getName().equals(call.getClassName()) && "render".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitOrbSlots.render(this, $0, $1); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = AbstractOrb.class, method = "setSlot", paramtypez = {int.class, int.class})
    public static class SlotPosition {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(AbstractOrb __instance, int slot, int totalSlots) {
            if (!PortraitBattle.active()) return SpireReturn.Continue();
            place(__instance, slot, totalSlots);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = EmptyOrbSlot.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class EmptySlot {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(EmptyOrbSlot __instance, SpriteBatch sb) {
            if (!PortraitBattle.active()) return SpireReturn.Continue();
            float diameter = 128f * Settings.scale;
            float x = __instance.cX - diameter * .5f;
            float y = __instance.cY - diameter * .5f;
            float color = sb.getPackedColor();
            sb.setColor(.66f, .81f, .91f, 1f);
            sb.draw(ImageMaster.ORB_SLOT_2, x, y, diameter, diameter);
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.ORB_SLOT_1, x, y, diameter, diameter);
            sb.setColor(color);
            return SpireReturn.Return();
        }
    }
}
