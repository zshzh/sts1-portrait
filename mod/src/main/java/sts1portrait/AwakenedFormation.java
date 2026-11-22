package sts1portrait;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.AwakenedOne;
import com.megacrit.cardcrawl.monsters.exordium.Cultist;
import java.util.List;

final class AwakenedFormation {
    private static AbstractMonster boss;
    private static AbstractMonster left;
    private static AbstractMonster right;

    private AwakenedFormation() {}

    static void reset() {
        boss = left = right = null;
    }

    static boolean place(List<AbstractMonster> monsters) {
        reset();
        if (monsters.size() != 3) return false;
        for (AbstractMonster monster : monsters) {
            if (monster instanceof AwakenedOne) {
                boss = monster;
            } else if (monster instanceof Cultist) {
                if (left == null) left = monster;
                else right = monster;
            }
        }
        if (boss == null || left == null || right == null) {
            reset();
            return false;
        }

        float width = PortraitViewport.contentWidth();
        float unit = PortraitViewport.referenceWidth();
        float lower = PortraitBattle.enemyBaseline();
        PortraitBattle.positionEnemy(left, PortraitViewport.x(.265f), lower, unit);
        PortraitBattle.positionEnemy(right, PortraitViewport.x(.735f), lower, unit);
        PortraitBattle.positionEnemy(boss, PortraitViewport.x(.49f), lower + width * .55f, unit);
        return true;
    }

    static float bodyScale(AbstractCreature creature) {
        if (creature == boss) return .80f;
        return creature == left || creature == right ? .88f : 1f;
    }

    static boolean contains(AbstractMonster monster) {
        return monster == boss || monster == left || monster == right;
    }

    static float targetWidthFraction(AbstractMonster monster) {
        return monster == boss ? .60f : .40f;
    }
}
