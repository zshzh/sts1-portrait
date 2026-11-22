package sts1portrait;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

@SpirePatch(clz = AbstractCreature.class, method = "renderHealth", paramtypez = {SpriteBatch.class})
public final class PortraitCreatureHealth {
    // Native bar textures start at -28 * scale and are 20 * scale tall.
    private static final float BAR_CENTER = -18f;
    private static final Matrix4 base = new Matrix4();
    private static final Matrix4 zoom = new Matrix4();
    private static boolean transformed;
    private static boolean compactPlayer;
    private static AbstractMonster namedMonster;
    private static float monsterBarTop;

    static float playerBarX() { return PortraitViewport.x(.283f); }

    public static boolean useCompactStatusRow() {
        return compactPlayer;
    }

    @SpireInstrumentPatch
    public static ExprEditor moveStatusIcons() {
        String compact = PortraitCreatureHealth.class.getName() + ".useCompactStatusRow()";
        String scale = Settings.class.getName() + ".scale";
        return new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws CannotCompileException {
                if (!call.getClassName().equals(AbstractCreature.class.getName())) return;
                if (call.getMethodName().equals("renderBlockIconAndValue")) {
                    call.replace("{ if (" + compact + ") $proceed($1, $2 + 34.0f * " + scale
                            + ", $3 - 34.0f * " + scale + "); else $proceed($$); }");
                } else if (call.getMethodName().equals("renderPowerIcons")) {
                    call.replace("{ if (" + compact + " && this.currentBlock != 0)"
                            + " $proceed($1, $2 + 55.0f * " + scale + ", $3); else $proceed($$); }");
                }
            }
        };
    }

    @SpirePrefixPatch
    public static void before(AbstractCreature creature, SpriteBatch batch, float ___hbYOffset) {
        transformed = false;
        compactPlayer = false;
        if (!PortraitBattle.active() || Settings.hideCombatElements) return;
        compactPlayer = creature instanceof AbstractPlayer && Sts2Preview.compact();
        Hitbox hit = creature.hb;
        float x = hit.cX;
        float y = hit.cY - hit.height * .5f + ___hbYOffset;
        float unit = PortraitViewport.contentWidth();
        float leftRoom = x - PortraitViewport.x(.02f);
        float rightRoom = PortraitViewport.x(.98f) - x;
        boolean monster = creature instanceof AbstractMonster;
        boolean singleEnemy = false;
        if (monster) {
            singleEnemy = AbstractDungeon.getMonsters().monsters.size() == 1;
            for (AbstractMonster other : AbstractDungeon.getMonsters().monsters) {
                if (other == creature || other.isDeadOrEscaped()
                        || Math.abs(other.healthHb.cY - creature.healthHb.cY) > unit * .09f) continue;
                float distance = other.hb.cX - x;
                if (distance < 0f) leftRoom = Math.min(leftRoom, -distance * .5f - unit * .012f);
                if (distance > 0f) rightRoom = Math.min(rightRoom, distance * .5f - unit * .012f);
            }
        }
        float reachLeft = hit.width * .5f + (creature.currentBlock != 0 ? 48f : 20f) * Settings.scale;
        float powerOffset = compactPlayer && creature.currentBlock != 0 ? 55f : 0f;
        float reachRight = Math.max(hit.width * .5f + 20f * Settings.scale,
                (creature.powers.size() * 32f + 16f + powerOffset) * Settings.scale - hit.width * .5f);
        float target = compactPlayer ? 1.78f : creature instanceof AbstractPlayer ? 1.65f
                : singleEnemy ? 1.85f : 1.55f;
        float scale = Math.max(1f, Math.min(target, Math.min(leftRoom / reachLeft, rightRoom / reachRight)));
        if (monster) {
            namedMonster = (AbstractMonster) creature;
            monsterBarTop = y + (BAR_CENTER + 10f) * Settings.scale * scale;
        }
        if (scale <= 1f && !compactPlayer) return;
        float displayX = compactPlayer ? playerBarX() : x;
        float displayY = compactPlayer ? PortraitBattle.playerBattleY() - BAR_CENTER * Settings.scale * scale : y;
        base.set(batch.getTransformMatrix());
        batch.setTransformMatrix(zoom.set(base).translate(displayX, displayY, 0f)
                .scale(compactPlayer ? scale * 1.17f : scale, scale, 1f).translate(-x, -y, 0f));
        transformed = true;
    }

    @SpirePostfixPatch
    public static void after(AbstractCreature creature, SpriteBatch batch) {
        if (transformed) batch.setTransformMatrix(base);
        transformed = false;
        compactPlayer = false;
    }

    @SpirePatch(clz = AbstractMonster.class, method = "renderName", paramtypez = {SpriteBatch.class})
    public static class MonsterName {
        private static final Matrix4 original = new Matrix4();
        private static final Matrix4 moved = new Matrix4();
        private static boolean applied;

        @SpirePrefixPatch
        public static void before(AbstractMonster monster, SpriteBatch batch) {
            applied = monster == namedMonster && PortraitBattle.active() && !Settings.hideCombatElements;
            if (!applied) return;
            float center = monsterBarTop + PortraitViewport.contentWidth() * .032f;
            original.set(batch.getTransformMatrix());
            batch.setTransformMatrix(moved.set(original).translate(0f, center - monster.healthHb.cY, 0f));
        }

        @SpirePostfixPatch
        public static void after(AbstractMonster monster, SpriteBatch batch) {
            if (applied) batch.setTransformMatrix(original);
        }
    }
}
