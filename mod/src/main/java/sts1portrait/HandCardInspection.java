package sts1portrait;

import basemod.patches.com.megacrit.cardcrawl.screens.SingleCardViewPopup.ScrollingTooltips;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class HandCardInspection {
    // Open before the launcher's 500 ms long-press right click.
    private static final long HOLD_NANOS = 450_000_000L;
    private static AbstractCard candidate;
    private static long pressedAt;
    private static int pressedX;
    private static int pressedY;
    private static boolean ownPopup;
    private static boolean consumeHold;
    private static boolean blockDungeon;

    private HandCardInspection() {
    }

    public static void afterInputSample() {
        SingleCardViewPopup popup = CardCrawlGame.cardPopup;
        ownPopup = ownPopup && popup != null && popup.isOpen;
        blockDungeon = ownPopup;
        if (consumeHold) {
            InputHelper.justClickedLeft = false;
            InputHelper.justReleasedClickLeft = false;
            InputHelper.justClickedRight = false;
            InputHelper.justReleasedClickRight = false;
            consumeHold = InputHelper.isMouseDown || InputHelper.isMouseDown_R;
        }
        AbstractPlayer player = AbstractDungeon.player;
        if (ownPopup || popup == null || !canInspect(player)) {
            candidate = null;
            return;
        }
        if (InputHelper.justClickedLeft && InputHelper.isMouseDown) {
            candidate = player.hand.getHoveredCard();
            if (candidate != null) {
                pressedAt = System.nanoTime();
                pressedX = InputHelper.mX;
                pressedY = InputHelper.mY;
            }
        }
        if (candidate == null) return;
        float slop = Math.max(8f, PortraitViewport.contentWidth() * 0.014f);
        float dx = InputHelper.mX - pressedX;
        float dy = InputHelper.mY - pressedY;
        // The drop-zone flag can still describe the previous pointer position.
        if (dx * dx + dy * dy > slop * slop || player.inSingleTargetMode
                || !player.hand.contains(candidate)) {
            candidate = null;
            return;
        }
        if (System.nanoTime() - pressedAt >= HOLD_NANOS
                && (InputHelper.isMouseDown || InputHelper.justReleasedClickLeft || InputHelper.justClickedRight)) {
            AbstractCard card = candidate;
            candidate = null;
            boolean held = InputHelper.isMouseDown;
            player.releaseCard();
            // The popup consumes the held gesture. Let the card receive a new press after it closes.
            card.hoverTimer = 0f;
            // releaseCard also clears the sampled button state. Keep the physical hold for input bookkeeping.
            InputHelper.isMouseDown = held;
            player.toHover = null;
            popup.open(card);
            ownPopup = true;
            blockDungeon = true;
            consumeHold = InputHelper.isMouseDown || InputHelper.isMouseDown_R;
            InputHelper.justClickedLeft = false;
            InputHelper.justReleasedClickLeft = false;
            InputHelper.justClickedRight = false;
            InputHelper.justReleasedClickRight = false;
        } else if (!InputHelper.isMouseDown) {
            candidate = null;
        }
    }

    private static boolean canInspect(AbstractPlayer player) {
        return PortraitBattle.active() && !Settings.isControllerMode && player != null
                && CardCrawlGame.mode == CardCrawlGame.GameMode.GAMEPLAY
                && !CardCrawlGame.isPopupOpen && !AbstractDungeon.isScreenUp
                && !player.isEndingTurn && !player.endTurnQueued
                && !player.inSingleTargetMode;
    }

    public static boolean blocksDungeonUpdate() {
        return blockDungeon;
    }

    @SpirePatch(clz = CardCrawlGame.class, method = "update", paramtypez = {})
    public static class ObservePointer {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (InputHelper.class.getName().equals(call.getClassName())
                            && "updateFirst".equals(call.getMethodName())) {
                        call.replace("{ $proceed($$); sts1portrait.HandCardInspection.afterInputSample(); }");
                    } else if (AbstractDungeon.class.getName().equals(call.getClassName())
                            && "update".equals(call.getMethodName())) {
                        call.replace("{ if (!sts1portrait.HandCardInspection.blocksDungeonUpdate()) $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = SingleCardViewPopup.class, method = "renderTips")
    public static class KeywordPlacement {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    boolean scrolling = ScrollingTooltips.class.getName().equals(call.getClassName())
                            && "queueScrollingPowerTips".equals(call.getMethodName());
                    boolean vanilla = TipHelper.class.getName().equals(call.getClassName())
                            && "queuePowerTips".equals(call.getMethodName());
                    if (!scrolling && !vanilla) return;
                    call.replace("{ if (sts1portrait.PortraitScreens.enabled()) {"
                            + " $1 = sts1portrait.PortraitViewport.x(0.36f);"
                            + " $2 = sts1portrait.PortraitViewport.y(0.365f);"
                            + (scrolling ? " $4 = $2 - sts1portrait.PortraitViewport.y(0.06f);" : "")
                            + " } $proceed($$); }");
                }
            };
        }
    }
}
