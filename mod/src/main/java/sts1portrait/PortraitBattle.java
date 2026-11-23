package sts1portrait;

import basemod.ReflectionHacks;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
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
    private static final int CARDS_PER_ROW = 5;
    // Match the apparent gaps between icons, including the two pile counters.
    private static final float[] BOTTOM_CONTROL_X = { .090f, .236f, .500f, .770f, .909f };
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

    private static float fixedHandScale() {
        float width = Math.min(PortraitViewport.referenceCanvasWidth() * CARD_WIDTH,
                Settings.WIDTH * (COLUMN_PITCH - MIN_CARD_GAP));
        return width / AbstractCard.IMG_WIDTH;
    }

    private static float handY(int row, float cardHeight) {
        float width = PortraitViewport.referenceWidth();
        return hudY() + width * HAND_HUD_GAP - handDrop() + cardHeight * 0.5f
                + (1 - row) * (cardHeight + width * CARD_ROW_GAP);
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
            card.target_x = Settings.WIDTH * .5f + (index % columns - (columns - 1) * 0.5f) * pitch;
            card.target_y = handY(index / columns, cardHeight);
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
        return Math.max(originalScale, (battleHandTop(cardHeight) + Settings.WIDTH * 0.02f) / originalMinimum);
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
        if (!AwakenedFormation.place(roster) && !SlimeSplitLayout.place(group, roster)) layoutMonsters(roster);
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
        float baseline = enemyBaseline();
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
            // Leave room above the lower intents for the upper row's health and powers.
            baseline += shortDrag ? width * .22f : Math.max(width * .40f, rowHeight + width * .255f);
            start = end;
        }
    }

    static float enemyBaseline() {
        return formationHandTop() + PortraitViewport.referenceWidth() * .13f + PortraitViewport.contentWidth() * .096f;
    }

    static void positionEnemy(AbstractMonster enemy, float centerX, float baseline, float unit) {
        enemy.hb.width = Math.max(enemy.hb.width, unit * 0.13f);
        enemy.hb_w = Math.max(enemy.hb_w, unit * 0.11f);
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
        return formationHandTop() + PortraitViewport.referenceWidth() * .098f;
    }

    private static float handDrop() {
        return Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth()) * .09f;
    }

    static float playerHudDrop() {
        return handDrop() + Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth()) * .016f;
    }

    static float playerBattleY() {
        return playerFormationY() - playerHudDrop();
    }

    static float hudY() {
        return PortraitViewport.y(0) + PortraitViewport.referenceWidth() * 0.076f;
    }

    static float bottomControlX(int slot) {
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
