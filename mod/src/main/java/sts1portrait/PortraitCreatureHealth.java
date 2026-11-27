package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
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
import com.megacrit.cardcrawl.powers.AbstractPower;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

@SpirePatch(clz = AbstractCreature.class, method = "renderHealth", paramtypez = {SpriteBatch.class})
public final class PortraitCreatureHealth {
    // Native bar textures start at -28 * scale and are 20 * scale tall.
    private static final float BAR_CENTER = -18f;
    private static final Matrix4 base = new Matrix4();
    private static final Matrix4 zoom = new Matrix4();
    private static final Matrix4 powerBase = new Matrix4();
    private static final Matrix4 powerZoom = new Matrix4();
    private static boolean transformed;
    private static boolean compactPlayer;
    private static float activeCompactScale = 1f;
    private static float activeCompactDisplayX;
    private static AbstractMonster namedMonster;
    private static float monsterBarTop;

    private static float playerBarMinX() {
        return Sts2Preview.avatarRight() + PortraitViewport.contentWidth() * .020f;
    }

    private static float playerBarMaxX() {
        return Sts2Preview.potionLeft() - PortraitViewport.contentWidth() * .012f;
    }

    private static float playerBarX(float halfWidth) {
        return Math.max(playerBarMinX() + halfWidth,
                Math.min(Settings.WIDTH * .263f, playerBarMaxX() - halfWidth));
    }

    static float playerBarLeft(AbstractPlayer player) {
        float halfWidth = playerBarHalfWidth(player);
        return Math.max(playerBarMinX(), playerBarX(halfWidth) - halfWidth);
    }

    static float playerBarRight(AbstractPlayer player) {
        float halfWidth = playerBarHalfWidth(player);
        return Math.min(playerBarMaxX(), playerBarX(halfWidth) + halfWidth);
    }

    private static float playerBarUniformScale(AbstractCreature player) {
        float availableWidth = Math.max(1f, playerBarMaxX() - playerBarMinX());
        float maxByWidth = (availableWidth * .5f) / Math.max(1f, nativeBarHalfWidth(player));
        return Math.max(.85f, Math.min(1.78f, maxByWidth));
    }

    private static float playerBarHalfWidth(AbstractPlayer player) {
        return nativeBarHalfWidth(player) * playerBarUniformScale(player);
    }

    private static float playerBarHalfWidth(AbstractCreature player, float scale) {
        return nativeBarHalfWidth(player) * scale;
    }

    private static void syncHealthBarWidth(AbstractCreature creature) {
        if (creature == null || creature.maxHealth <= 0 || creature.hb == null) return;
        float expected = creature.currentHealth <= 0 ? 0f
                : creature.hb.width * creature.currentHealth / (float) creature.maxHealth;
        float currentTarget = ReflectionHacks.<Float>getPrivate(creature, AbstractCreature.class, "targetHealthBarWidth");
        if (Math.abs(currentTarget - expected) > 1.5f) {
            float currentBar = ReflectionHacks.<Float>getPrivate(creature, AbstractCreature.class, "healthBarWidth");
            ReflectionHacks.setPrivate(creature, AbstractCreature.class, "targetHealthBarWidth", expected);
            if (creature.currentHealth == creature.maxHealth || currentBar < expected) {
                ReflectionHacks.setPrivate(creature, AbstractCreature.class, "healthBarWidth", expected);
            }
        }
    }

    private static float nativeBarHalfWidth(AbstractCreature creature) {
        // Each 24-pixel end cap contributes five visible columns.
        return creature.hb.width * .5f + (20f * 5f / 24f) * Settings.scale;
    }

    private static float barScale(AbstractCreature creature, float leftRoom, float rightRoom,
                                  boolean compact, float target) {
        if (compact) return playerBarUniformScale(creature);
        float halfWidth = creature.hb.width * .5f;
        float reachLeft = halfWidth + (creature.currentBlock != 0 ? 48f : 20f) * Settings.scale;
        float reachRight = Math.max(halfWidth + 20f * Settings.scale,
                (creature.powers.size() * 32f + 16f) * Settings.scale - halfWidth);
        return Math.max(1f, Math.min(target, Math.min(leftRoom / reachLeft, rightRoom / reachRight)));
    }

    public static boolean useCompactStatusRow() {
        return compactPlayer;
    }

    public static void renderCompactPowers(AbstractCreature creature, SpriteBatch sb, float x, float y) {
        int count = creature.powers.size();
        if (count == 0) return;
        Color textColor = ReflectionHacks.getPrivate(creature, AbstractCreature.class, "hbTextColor");
        if (textColor == null) textColor = Color.WHITE;
        float startX = x + (creature.currentBlock != 0 ? 55f : 0f) * Settings.scale + 10f * Settings.scale;
        float iconY = y - (Settings.isMobile ? 53f : 48f) * Settings.scale;
        float amountY = y - (Settings.isMobile ? 75f : 66f) * Settings.scale;
        float defaultStep = (Settings.isMobile ? 48f : 38f) * Settings.scale;
        float scale = Math.max(.01f, activeCompactScale);
        float maxLocalX = creature.hb.cX + (playerBarMaxX() - activeCompactDisplayX) / scale - 18f * Settings.scale;
        float maxSpan = Math.max(20f * Settings.scale, maxLocalX - startX);
        float step = count <= 1 ? defaultStep : Math.min(defaultStep, maxSpan / (count - 1));
        float iconFactor = step < defaultStep ? Math.max(.68f, step / defaultStep) : 1f;
        for (int i = 0; i < count; i++) {
            AbstractPower power = creature.powers.get(i);
            float px = startX + i * step;
            if (iconFactor < .999f) {
                powerBase.set(sb.getTransformMatrix());
                sb.setTransformMatrix(powerZoom.set(powerBase)
                        .translate(px, iconY, 0f)
                        .scale(iconFactor, iconFactor, 1f)
                        .translate(-px, -iconY, 0f));
                try {
                    power.renderIcons(sb, px, iconY, textColor);
                    power.renderAmount(sb, px, amountY, textColor);
                } finally {
                    sb.setTransformMatrix(powerBase);
                }
            } else {
                power.renderIcons(sb, px, iconY, textColor);
                power.renderAmount(sb, px, amountY, textColor);
            }
        }
    }

    @SpireInstrumentPatch
    public static ExprEditor moveStatusIcons() {
        String compact = PortraitCreatureHealth.class.getName() + ".useCompactStatusRow()";
        String helper = PortraitCreatureHealth.class.getName() + ".renderCompactPowers";
        String scale = Settings.class.getName() + ".scale";
        return new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws CannotCompileException {
                if (!call.getClassName().equals(AbstractCreature.class.getName())) return;
                if (call.getMethodName().equals("renderBlockIconAndValue")) {
                    call.replace("{ if (" + compact + ") $proceed($1, $2 + 34.0f * " + scale
                            + ", $3 - 34.0f * " + scale + "); else $proceed($$); }");
                } else if (call.getMethodName().equals("renderPowerIcons")) {
                    call.replace("{ if (" + compact + ") " + helper + "(this, $1, $2, $3); else $proceed($$); }");
                }
            }
        };
    }

    @SpirePrefixPatch
    public static void before(AbstractCreature creature, SpriteBatch batch, float ___hbYOffset) {
        transformed = false;
        compactPlayer = false;
        if (!PortraitBattle.active() || Settings.hideCombatElements) return;
        syncHealthBarWidth(creature);
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
        float target = compactPlayer ? 1.78f : creature instanceof AbstractPlayer ? 1.65f
                : singleEnemy ? 1.85f : 1.55f;
        float scale = barScale(creature, leftRoom, rightRoom, compactPlayer, target);
        if (monster) {
            namedMonster = (AbstractMonster) creature;
            monsterBarTop = y + (BAR_CENTER + 10f) * Settings.scale * scale;
        }
        if (scale <= 1f && !compactPlayer) return;
        float compactHalfWidth = compactPlayer ? playerBarHalfWidth(creature, scale) : 0f;
        float displayX = compactPlayer ? playerBarX(compactHalfWidth) : x;
        float displayY = compactPlayer ? PortraitBattle.playerBattleY() - BAR_CENTER * Settings.scale * scale : y;
        activeCompactScale = scale;
        activeCompactDisplayX = displayX;
        base.set(batch.getTransformMatrix());
        batch.setTransformMatrix(zoom.set(base).translate(displayX, displayY, 0f)
                .scale(scale, scale, 1f).translate(-x, -y, 0f));
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
