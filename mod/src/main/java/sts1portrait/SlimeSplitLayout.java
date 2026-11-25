package sts1portrait;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.exordium.AcidSlime_L;
import com.megacrit.cardcrawl.monsters.exordium.AcidSlime_M;
import com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_L;
import com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_M;
import java.util.ArrayList;

final class SlimeSplitLayout {
    private static final ArrayList<AbstractMonster> live = new ArrayList<>(4);
    private static final ArrayList<AbstractMonster> acidSlots = new ArrayList<>(2);
    private static final ArrayList<AbstractMonster> spikeSlots = new ArrayList<>(2);
    private static MonsterGroup trackedGroup;

    private SlimeSplitLayout() {}

    static void track(MonsterGroup group) { trackedGroup = group; }

    static boolean owns(MonsterGroup group) { return group != null && group == trackedGroup; }

    static void reset() {
        trackedGroup = null;
        live.clear();
        acidSlots.clear();
        spikeSlots.clear();
    }

    static ArrayList<AbstractMonster> roster(MonsterGroup group) {
        if (!owns(group)) return group.monsters;
        live.clear();
        for (AbstractMonster monster : group.monsters) {
            if (!monster.isDeadOrEscaped() && !monster.isDying && !monster.halfDead && monster.currentHealth > 0) {
                live.add(monster);
            }
        }
        return live;
    }

    static boolean isSplitChild(AbstractCreature creature) {
        return trackedGroup != null && (creature instanceof AcidSlime_L || creature instanceof AcidSlime_M
                || creature instanceof SpikeSlime_L || creature instanceof SpikeSlime_M);
    }

    static boolean place(MonsterGroup group, ArrayList<AbstractMonster> roster) {
        if (!owns(group)) return false;
        for (AbstractMonster monster : roster) {
            if (!isSplitChild(monster)) return false;
        }
        float width = PortraitViewport.contentWidth();
        float baseline = Math.min(PortraitBattle.playerFormationY() + width * .128f, PortraitBattle.enemyBaseline(2));
        float rowStep = Math.min(width * .255f, PortraitBattle.maxMultiRowStep(baseline, 2));
        for (AbstractMonster monster : roster) {
            boolean acid = monster instanceof AcidSlime_L || monster instanceof AcidSlime_M;
            ArrayList<AbstractMonster> slots = acid ? acidSlots : spikeSlots;
            int row = 0;
            if (!(monster instanceof AcidSlime_L) && !(monster instanceof SpikeSlime_L)) {
                if (!slots.contains(monster)) slots.add(monster);
                row = slots.indexOf(monster) % 2;
            }
            PortraitBattle.positionEnemy(monster, PortraitViewport.x(acid ? .745f : .255f),
                    baseline + row * rowStep, PortraitViewport.referenceWidth());
        }
        return true;
    }

    static float fitBody(AbstractCreature creature, float scale) {
        if (!isSplitChild(creature)) return scale;
        boolean large = creature instanceof AcidSlime_L || creature instanceof SpikeSlime_L;
        return large ? scale * .81f : scale;
    }

    @SpirePatch(clz = MonsterGroup.class, method = "addMonster",
            paramtypez = {int.class, AbstractMonster.class})
    public static class PositionOnSpawn {
        @SpirePostfixPatch
        public static void after(MonsterGroup group) {
            if (owns(group) && PortraitBattle.active() && AbstractDungeon.getMonsters() == group) {
                PortraitBattle.layoutRoom(AbstractDungeon.getCurrRoom());
            }
        }
    }
}
