package sts1portrait;

import basemod.ReflectionHacks;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.OverlayMenu;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.exordium.SlimeBoss;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.ui.panels.AbstractPanel;
import com.megacrit.cardcrawl.ui.panels.DiscardPilePanel;
import com.megacrit.cardcrawl.ui.panels.DrawPilePanel;
import java.util.ArrayList;

public final class PortraitBattle {
    private static final float CARD_WIDTH = 0.164f;
    private static final float MIN_CARD_GAP = 0.019f;
    private static final float COLUMN_PITCH = 0.183f;
    private static final float CARD_ROW_GAP = 0.023f;
    private static final float HAND_HUD_GAP = 0.167f;
    private static final float BASE_ASPECT = 2.05f;
    private static final float MIN_ASPECT = 1.25f;
    private static final int CARDS_PER_ROW = 5;
    // Match the apparent gaps between icons, including the two pile counters.
    private static final float[] BOTTOM_CONTROL_X = { .095f, .240f, .500f, .765f, .905f };
    private static final float DISCARD_NO_EXHAUST_X = .815f;
    private static final ArrayList<AbstractMonster> laidOutMonsters = new ArrayList<>();
    private static AbstractRoom laidOutRoom;
    private static AbstractPlayer laidOutPlayer;
    private static OverlayMenu laidOutHud;
    private static int roomRevision = -1;
    private static int hudRevision = -1;
    private static float drawOffset, discardOffset, pileBottom;

    private PortraitBattle() {
    }

    static boolean active(AbstractRoom room) {
        return PortraitViewport.active() && room != null && room.phase == AbstractRoom.RoomPhase.COMBAT;
    }

    public static boolean active() {
        return PortraitViewport.active() && AbstractDungeon.currMapNode != null
                && active(AbstractDungeon.getCurrRoom());
    }

    // Also called from the patched AbstractPlayer in the game's package.
    public static boolean playingHand(CardGroup group) {
        return active() && !AbstractDungeon.isScreenUp
                && AbstractDungeon.player != null && AbstractDungeon.player.hand == group;
    }

    static float heightFactor() {
        float w = Math.max(1f, PortraitViewport.contentWidth());
        float aspect = PortraitViewport.contentHeight() / w;
        if (aspect >= BASE_ASPECT) return 1f;
        return Math.max(0f, (aspect - MIN_ASPECT) / (BASE_ASPECT - MIN_ASPECT));
    }

    private static float blend(float compact, float tall) {
        float t = heightFactor();
        return compact + (tall - compact) * t;
    }

    private static float fixedHandScale() {
        float width = Math.min(PortraitViewport.referenceCanvasWidth() * CARD_WIDTH,
                Settings.WIDTH * (COLUMN_PITCH - MIN_CARD_GAP));
        float byWidth = width / AbstractCard.IMG_WIDTH;
        float maxCardHeight = PortraitViewport.contentHeight() * 0.134f;
        float byHeight = maxCardHeight / AbstractCard.IMG_HEIGHT;
        return Math.min(byWidth, byHeight);
    }

    private static float handY(int row, float cardHeight) {
        float width = PortraitViewport.referenceWidth();
        float hudGap = blend(0.118f, HAND_HUD_GAP);
        float rowGap = blend(0.014f, CARD_ROW_GAP);
        return handAnchorY() + width * hudGap - handDrop() + cardHeight * 0.5f
                + (1 - row) * (cardHeight + width * rowGap);
    }

    static void layoutHand(CardGroup hand) {
        layoutHand(hand, false);
    }

    static void layoutSelectionHand(CardGroup hand) {
        layoutHand(hand, true);
    }

    private static void layoutHand(CardGroup hand, boolean selecting) {
        int size = hand.size();
        if ((!selecting && !playingHand(hand)) || size == 0) {
            return;
        }
        float handScale = fixedHandScale();
        float cardHeight = AbstractCard.IMG_HEIGHT * handScale;
        // Larger hands from other mods still fit within the two-row area.
        int columns = handColumns(size);
        float pitch = Settings.WIDTH * COLUMN_PITCH * (CARDS_PER_ROW - 1) / (columns - 1);
        AbstractPlayer player = AbstractDungeon.player;
        for (int index = 0; index < size; index++) {
            AbstractCard card = hand.group.get(index);
            if (!selecting && card == player.hoveredCard && (player.isDraggingCard || player.inSingleTargetMode)) {
                continue;
            }
            int logicalRow = index / columns;
            int rowCount = Math.min(columns, size - logicalRow * columns);
            // Keep the first row of cards anchored on the bottom row; overflow cards (>5) fill the upper row.
            int visualRow = 1 - logicalRow;
            card.target_x = Settings.WIDTH * .5f + (index % columns - (rowCount - 1) * 0.5f) * pitch;
            card.target_y = handY(visualRow, cardHeight);
            card.targetAngle = 0.0f;
            card.targetDrawScale = handScale;
        }
    }

    private static int handColumns(int size) {
        return Math.max(CARDS_PER_ROW, (size + 1) / 2);
    }

    public static float hoverHeight(AbstractPlayer player, float original) {
        if (!playingHand(player.hand) || player.hoveredCard == null) {
            return original;
        }
        return player.hoveredCard.target_y;
    }

    public static float handVisualScale(AbstractPlayer player, float original) {
        if (!playingHand(player.hand)) return original;
        return fixedHandScale() * (player.isDraggingCard || player.inSingleTargetMode ? 2f : 1f);
    }

    public static float aimingCardY(AbstractPlayer player, float original) {
        if (!playingHand(player.hand) || !player.inSingleTargetMode) return original;
        float bottom = PortraitViewport.y(0f) + PortraitViewport.contentWidth() * .018f;
        return Math.max(original, bottom + AbstractCard.IMG_HEIGHT * fixedHandScale());
    }

    public static float dropZoneScale(AbstractPlayer player, float originalScale, float originalMinimum) {
        if (!playingHand(player.hand)) {
            return originalScale;
        }
        float cardHeight = AbstractCard.IMG_HEIGHT * fixedHandScale();
        float top = player.hand.size() <= CARDS_PER_ROW
                ? handY(1, cardHeight) + cardHeight * 0.5f
                : battleHandTop(cardHeight);
        return Math.max(originalScale, (top + Settings.WIDTH * 0.02f) / originalMinimum);
    }

    static void layoutRoom(AbstractRoom room) {
        if (!active(room)) {
            if (laidOutRoom != null) {
                laidOutRoom = null;
                laidOutPlayer = null;
                laidOutMonsters.clear();
                SlimeSplitLayout.reset();
                AwakenedFormation.reset();
            }
            return;
        }
        AbstractPlayer player = AbstractDungeon.player;
        int revision = PortraitViewport.revision();
        boolean resized = roomRevision != revision;
        if (room != laidOutRoom || player != laidOutPlayer || resized) {
            if (room != laidOutRoom) SlimeSplitLayout.reset();
            roomRevision = revision;
            laidOutRoom = room;
            laidOutPlayer = player;
            laidOutMonsters.clear();
            if (player != null) {
                player.movePosition(PortraitViewport.x(0.245f), playerBattleY());
                if (resized) layoutHand(player.hand);
            }
        }

        if (player != null) PortraitOrbSlots.place(player);

        MonsterGroup group = room.monsters;
        if (group == null) return;
        if (!SlimeSplitLayout.owns(group)) {
            for (AbstractMonster monster : group.monsters) {
                if (monster instanceof SlimeBoss) {
                    SlimeSplitLayout.track(group);
                    break;
                }
            }
        }
        ArrayList<AbstractMonster> roster = SlimeSplitLayout.roster(group);
        if (matchesRoster(roster)) {
            return;
        }
        if (!AwakenedFormation.place(roster) && !SlimeSplitLayout.place(group, roster)
                && !PortraitFinalAct.placeElitePair(roster)) layoutMonsters(roster);
        laidOutMonsters.clear();
        laidOutMonsters.addAll(roster);
        System.out.println("[STS1 Portrait] battle layout applied, enemies=" + laidOutMonsters.size());
    }

    private static boolean matchesRoster(ArrayList<AbstractMonster> monsters) {
        if (monsters.size() != laidOutMonsters.size()) {
            return false;
        }
        for (int index = 0; index < monsters.size(); index++) {
            if (monsters.get(index) != laidOutMonsters.get(index)) {
                return false;
            }
        }
        return true;
    }

    private static void layoutMonsters(ArrayList<AbstractMonster> monsters) {
        float width = PortraitViewport.contentWidth();
        float span = width * 0.92f;
        float unit = PortraitViewport.referenceWidth();
        float gap = Math.min(width, unit) * 0.11f;
        float minEnemyWidth = Math.min(width * 0.28f, unit * (monsters.size() <= 2 ? 0.24f : 0.19f));
        boolean shortDrag = monsters.size() <= 2;
        int rowLimit = monsters.size() == 4 ? 2 : 3;

        // First partition monsters into rows so vertical placement can fit cleanly within the real arena bounds.
        ArrayList<Integer> rowEnds = new ArrayList<>();
        ArrayList<Float> rowWidths = new ArrayList<>();
        ArrayList<Float> rowHeights = new ArrayList<>();
        int start = 0;
        while (start < monsters.size()) {
            int end = start;
            float rowWidth = 0;
            float rowHeight = 0;
            while (end < monsters.size()) {
                AbstractMonster enemy = monsters.get(end);
                float enemyWidth = Math.max(enemy.hb.width, minEnemyWidth);
                float occupied = enemyWidth + (end == start ? 0 : gap);
                if (end != start && (rowWidth + occupied > span || end - start == rowLimit)) {
                    break;
                }
                rowWidth += occupied;
                rowHeight = Math.max(rowHeight, 1.35f * enemy.hb.height + enemy.intentHb.height);
                end++;
            }
            rowEnds.add(end);
            rowWidths.add(rowWidth);
            rowHeights.add(rowHeight);
            start = end;
        }

        int totalRows = rowEnds.size();
        float baseline = enemyBaseline(totalRows);
        float maxStep = maxMultiRowStep(baseline, totalRows);
        start = 0;
        for (int r = 0; r < totalRows; r++) {
            int end = rowEnds.get(r);
            float rowWidth = rowWidths.get(r);
            float rowHeight = rowHeights.get(r);
            float x = PortraitViewport.x(0.5f) - rowWidth * 0.5f;
            boolean stagger = monsters.size() == 4 && start == 2 && end - start == 2;
            for (int index = start; index < end; index++) {
                AbstractMonster enemy = monsters.get(index);
                float enemyWidth = Math.max(enemy.hb.width, minEnemyWidth);
                float centerX = x + enemyWidth * 0.5f;
                if (stagger) {
                    float offset = width * .11f;
                    float inset = Math.min(width * .30f, enemyWidth * .5f + width * .04f);
                    centerX = Math.max(PortraitViewport.x(0f) + inset, Math.min(PortraitViewport.x(1f) - inset,
                            centerX + (index == start ? -offset : offset)));
                }
                positionEnemy(enemy, centerX, baseline, unit);
                x += enemyWidth + gap;
            }
            // Leave room above the lower intents for the upper row's health and powers, clamped to the arena ceiling.
            float desiredStep = shortDrag ? width * .22f : Math.max(width * .40f, rowHeight + width * .255f);
            baseline += Math.min(desiredStep, maxStep);
            start = end;
        }
    }

    static float enemyArenaFloor() {
        float w = PortraitViewport.contentWidth();
        return playerBattleY() + w * blend(0.175f, 0.235f);
    }

    static float enemyArenaCeiling() {
        float topBar = PortraitCombatHud.topBarActive()
                ? PortraitCombatHud.panelBottom()
                : PortraitViewport.y(0.88f);
        return topBar - PortraitViewport.referenceWidth() * 0.075f;
    }

    static float enemyArenaHeight() {
        return Math.max(PortraitViewport.contentWidth() * 0.18f, enemyArenaCeiling() - enemyArenaFloor());
    }

    static float enemyBaseline() {
        return enemyBaseline(1);
    }

    static float enemyBaseline(int rows) {
        float tallBaseline = formationHandTop() + PortraitViewport.referenceWidth() * .13f
                + PortraitViewport.contentWidth() * .226f;
        float floor = enemyArenaFloor();
        float ceiling = enemyArenaCeiling();
        float arena = Math.max(PortraitViewport.contentWidth() * 0.18f, ceiling - floor);
        if (heightFactor() >= 1f) {
            float maxSafe = ceiling - PortraitViewport.contentWidth() * (rows > 1 ? 0.56f : 0.34f);
            return Math.max(floor, Math.min(tallBaseline, maxSafe));
        }
        float riseFraction = rows > 1 ? blend(0.0f, 0.08f) : blend(0.04f, 0.14f);
        float adaptive = floor + arena * riseFraction;
        return Math.min(tallBaseline, adaptive);
    }

    static float maxMultiRowStep(float lowerBaseline, int rows) {
        if (rows <= 1) return Float.POSITIVE_INFINITY;
        float w = PortraitViewport.contentWidth();
        float ceiling = enemyArenaCeiling();
        float arena = enemyArenaHeight();
        float topAllowance = Math.min(arena * 0.50f, w * blend(0.20f, 0.32f));
        return Math.max(w * 0.16f, (ceiling - lowerBaseline - topAllowance) / (rows - 1));
    }

    static void positionEnemy(AbstractMonster enemy, float centerX, float baseline, float unit) {
        float previousWidth = enemy.hb.width;
        float minWidth = unit * 0.13f;
        if (enemy.hb.width < minWidth) {
            enemy.hb.width = minWidth;
        }
        enemy.hb_w = Math.max(enemy.hb_w, enemy.hb.width);
        if (enemy.healthHb.width < enemy.hb.width) {
            enemy.healthHb.width = enemy.hb.width;
        }
        if (Math.abs(enemy.hb.width - previousWidth) > 0.5f && enemy.maxHealth > 0) {
            float target = enemy.hb.width * enemy.currentHealth / (float) enemy.maxHealth;
            float ratio = previousWidth > 1f ? enemy.hb.width / previousWidth : 1f;
            float oldBar = ReflectionHacks.<Float>getPrivate(enemy, AbstractCreature.class, "healthBarWidth");
            ReflectionHacks.setPrivate(enemy, AbstractCreature.class, "targetHealthBarWidth", target);
            ReflectionHacks.setPrivate(enemy, AbstractCreature.class, "healthBarWidth",
                    enemy.currentHealth == enemy.maxHealth ? target : Math.max(target, oldBar * ratio));
        }
        enemy.drawX = centerX - enemy.hb_x;
        enemy.drawY = baseline - enemy.hb_y;
        enemy.hb.move(centerX + enemy.animX, baseline + enemy.hb_h * 0.5f);
        enemy.healthHb.move(enemy.hb.cX, baseline - enemy.healthHb.height * 0.5f);
        enemy.refreshIntentHbLocation();
    }

    static void layoutHud(OverlayMenu menu) {
        if (!PortraitViewport.active()) return;
        int revision = PortraitViewport.revision();
        if (menu != laidOutHud) {
            laidOutHud = menu;
            hudRevision = -1;
            drawOffset = discardOffset = pileBottom = 0f;
        }
        if (hudRevision != revision) {
            hudRevision = revision;
            movePanel(menu.energyPanel, bottomControlX(0), hudY());
            movePanel(menu.exhaustPanel, bottomControlX(4), hudY());
            float draw = PortraitViewport.x(0.015f);
            float discard = PortraitViewport.x(0.985f);
            float bottom = PortraitViewport.y(0);
            alignPile(menu.combatDeckPanel, DrawPilePanel.class, draw - drawOffset, bottom - pileBottom);
            alignPile(menu.discardPilePanel, DiscardPilePanel.class,
                    discard - discardOffset - (discardOffset == 0f ? Settings.WIDTH : 0f), bottom - pileBottom);
            drawOffset = draw;
            discardOffset = discard;
            pileBottom = bottom;
        }
    }

    private static void alignPile(AbstractPanel panel, Class<?> type, float dx, float dy) {
        movePanel(panel, panel.show_x + dx, panel.show_y + dy);
        moveHitbox(ReflectionHacks.getPrivate(panel, type, "hb"), dx, dy);
        moveHitbox(ReflectionHacks.getPrivate(panel, type, "bannerHb"), dx, dy);
    }

    private static void moveHitbox(Hitbox hitbox, float dx, float dy) {
        hitbox.move(hitbox.cX + dx, hitbox.cY + dy);
    }
    private static void movePanel(AbstractPanel panel, float showX, float showY) {
        float dx = showX - panel.show_x;
        float dy = showY - panel.show_y;
        panel.show_x = showX;
        panel.show_y = showY;
        if (!panel.isHidden) {
            panel.target_x += dx;
            panel.target_y += dy;
        }
    }

    static float battleHandTop() {
        return battleHandTop(AbstractCard.IMG_HEIGHT * fixedHandScale());
    }

    private static float battleHandTop(float cardHeight) {
        return handY(0, cardHeight) + cardHeight * .5f;
    }

    private static float formationHandTop() {
        return battleHandTop() + handDrop();
    }

    static float playerFormationY() {
        return formationHandTop() + PortraitViewport.referenceWidth() * blend(0.082f, .098f);
    }

    private static float handDrop() {
        return Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth()) * blend(0.062f, .09f);
    }

    static float playerHudDrop() {
        return handDrop() + Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth()) * .016f;
    }

    static float playerBattleY() {
        return playerFormationY() - playerHudDrop();
    }

    private static float handAnchorY() {
        // Lift the combat lower zone cleanly above the OS bottom gesture bar while adapting smoothly on wider tablets.
        return PortraitViewport.y(0) + PortraitViewport.referenceWidth() * blend(0.088f, 0.128f);
    }

    private static float hudY() {
        float width = PortraitViewport.referenceWidth();
        return Math.max(width * blend(0.072f, .108f), handAnchorY() - width * blend(0.032f, .044f));
    }

    static float bottomControlX(int slot) {
        if (slot == 3 && AbstractDungeon.player != null && AbstractDungeon.player.exhaustPile.isEmpty()) {
            return PortraitViewport.x(DISCARD_NO_EXHAUST_X);
        }
        return PortraitViewport.x(BOTTOM_CONTROL_X[slot]);
    }

    static float pileY() {
        return hudY() - PortraitViewport.referenceWidth() * .011f;
    }

    public static float endTurnX(float original) {
        return PortraitViewport.active() ? bottomControlX(2) : original;
    }

    public static float endTurnY(float original) {
        return PortraitViewport.active() ? hudY() : original;
    }

    static void enlargeEndTurn(Hitbox button) {
        if (PortraitViewport.active()) {
            button.width = Math.max(button.width, PortraitViewport.referenceWidth() * 0.25f);
            button.height = Math.max(button.height, PortraitViewport.referenceWidth() * 0.11f);
        }
    }
}
