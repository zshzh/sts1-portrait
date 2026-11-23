package sts1portrait;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import com.megacrit.cardcrawl.orbs.Lightning;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.DarkOrbPassiveEffect;
import com.megacrit.cardcrawl.vfx.combat.FrostOrbPassiveEffect;
import com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect;
import com.megacrit.cardcrawl.vfx.combat.PlasmaOrbPassiveEffect;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class PortraitOrbSlots {
    private static final float NATIVE_ORB_SIZE = 128f;
    private static final int ORBS_PER_ROW = 5;
    private static final Rectangle clip = new Rectangle();
    private static final Matrix4 base = new Matrix4();
    private static final Matrix4 scaled = new Matrix4();

    private PortraitOrbSlots() {}

    static void place(AbstractPlayer player) {
        int count = player.orbs.size();
        for (int index = 0; index < count; index++) {
            place(player, player.orbs.get(index), index, count);
        }
    }

    private static float bottom() {
        return PortraitBattle.playerBattleY() + PortraitViewport.contentWidth() * .030f;
    }

    private static float sideGap() {
        return PortraitViewport.contentWidth() * .007f;
    }

    static float top() {
        float width = PortraitViewport.contentWidth();
        float top = PortraitBattle.playerBattleY() + width * .145f;
        if (AbstractDungeon.getMonsters() == null) return top;
        for (AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
            if (monster.isDeadOrEscaped() || monster.halfDead || monster.isDying) continue;
            // The enemy bar and status icons are drawn below this edge.
            top = Math.min(top, monster.hb.y - width * .075f);
        }
        return top;
    }

    private static float rowGap() {
        return PortraitViewport.contentWidth() * .010f;
    }

    private static float columnGap() {
        return PortraitViewport.contentWidth() * .011f;
    }

    private static float laneRight(AbstractPlayer player) {
        return Math.min(PortraitCreatureHealth.playerBarRight(player), Sts2Preview.potionLeft() - sideGap());
    }

    private static float laneWidth(AbstractPlayer player) {
        return Math.max(0f, laneRight(player) - PortraitCreatureHealth.playerBarLeft(player));
    }

    private static float diameter(AbstractPlayer player, int count) {
        // Reserve both rows so that unlocking the sixth slot leaves the first five in place.
        int rows = Math.max(2, (count + ORBS_PER_ROW - 1) / ORBS_PER_ROW);
        float height = top() - bottom();
        float maxByWidth = (laneWidth(player) - columnGap() * (ORBS_PER_ROW - 1)) / ORBS_PER_ROW;
        float maxByHeight = (height - rowGap() * (rows - 1)) / rows;
        return Math.max(0f, Math.min(96f * Settings.scale, Math.min(maxByWidth, maxByHeight)));
    }

    private static void place(AbstractPlayer player, AbstractOrb orb, int index, int count) {
        float diameter = diameter(player, count);
        float pitch = diameter + columnGap();
        orb.tX = PortraitCreatureHealth.playerBarLeft(player) + diameter * .5f + (index % ORBS_PER_ROW) * pitch;
        orb.tY = bottom() + diameter * .5f + (index / ORBS_PER_ROW) * (diameter + rowGap());
        orb.hb.resize(diameter, diameter);
        orb.hb.move(orb.tX, orb.tY);
    }

    public static void render(AbstractPlayer player, AbstractOrb orb, SpriteBatch sb) {
        if (!Sts2Preview.compact()) {
            orb.render(sb);
            return;
        }
        // The intercepted vanilla loop enters once per orb. Draw the row once so the shared
        // clip can contain particle and text extents as well as the orb artwork.
        if (orb != player.orbs.get(0)) return;
        float size = diameter(player, player.orbs.size());
        if (size <= 0f || !beginClip(player, sb)) return;
        try {
            for (AbstractOrb slot : player.orbs) renderSlot(slot, sb, size);
        } finally {
            endClip(sb);
        }
    }

    private static boolean beginClip(AbstractPlayer player, SpriteBatch sb) {
        float left = PortraitCreatureHealth.playerBarLeft(player);
        float right = laneRight(player);
        float height = top() - bottom();
        if (right <= left || height <= 0f) return false;
        sb.flush();
        clip.set(left, bottom(), right - left, height);
        PortraitDesktopViewport.clip(clip);
        return ScissorStack.pushScissors(clip);
    }

    private static void endClip(SpriteBatch sb) {
        sb.flush();
        ScissorStack.popScissors();
    }

    @SpirePatch(clz = LightningOrbPassiveEffect.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = FrostOrbPassiveEffect.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = DarkOrbPassiveEffect.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = PlasmaOrbPassiveEffect.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class AmbientDrawing {
        private static boolean clipped;

        @SpirePrefixPatch
        public static SpireReturn<Void> before(AbstractGameEffect __instance, SpriteBatch sb) {
            clipped = false;
            if (!Sts2Preview.compact()) return SpireReturn.Continue();
            clipped = beginClip(AbstractDungeon.player, sb);
            return clipped ? SpireReturn.Continue() : SpireReturn.Return();
        }

        @SpirePostfixPatch
        public static void after(AbstractGameEffect __instance, SpriteBatch sb) {
            if (clipped) endClip(sb);
            clipped = false;
        }
    }

    private static void renderSlot(AbstractOrb orb, SpriteBatch sb, float size) {
        // The native images have different amounts of transparent padding.
        float opticalSize = orb instanceof Lightning ? 52f : orb instanceof EmptyOrbSlot ? 67f : 56f;
        float factor = size * .94f / (opticalSize * Settings.scale);
        float previousX = orb.cX;
        float previousY = orb.cY;
        orb.cX = orb.tX;
        orb.cY = orb.tY;
        base.set(sb.getTransformMatrix());
        scaled.set(base).translate(orb.cX, orb.cY, 0f).scale(factor, factor, 1f).translate(-orb.cX, -orb.cY, 0f);
        sb.setTransformMatrix(scaled);
        try {
            orb.render(sb);
        } finally {
            sb.setTransformMatrix(base);
            orb.cX = previousX;
            orb.cY = previousY;
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
            place(AbstractDungeon.player, __instance, slot, totalSlots);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = EmptyOrbSlot.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class EmptySlot {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(EmptyOrbSlot __instance, SpriteBatch sb) {
            if (!PortraitBattle.active()) return SpireReturn.Continue();
            float diameter = NATIVE_ORB_SIZE * Settings.scale;
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
