package sts1portrait;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.OverlayMenu;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.screens.select.HandCardSelectScreen;
import com.megacrit.cardcrawl.ui.buttons.EndTurnButton;
import com.megacrit.cardcrawl.ui.buttons.PeekButton;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitHandSelection {
    private static int selectedRevision = -1;

    private PortraitHandSelection() {}

    public static boolean active() {
        return PortraitScreens.enabled() && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.HAND_SELECT;
    }

    @SpirePatch(clz = HandCardSelectScreen.class, method = "updateHand", paramtypez = {})
    public static class HandLayout {
        @SpirePrefixPatch
        public static void before() {
            if (active()) PortraitBattle.layoutSelectionHand(AbstractDungeon.player.hand);
        }

        @SpirePostfixPatch
        public static void after() {
            if (active()) PortraitBattle.layoutSelectionHand(AbstractDungeon.player.hand);
        }
    }

    @SpirePatch(clz = HandCardSelectScreen.class, method = "refreshSelectedCards", paramtypez = {})
    public static class SelectedLayout {
        @SpirePostfixPatch
        public static void after(HandCardSelectScreen __instance, boolean ___forUpgrade) {
            if (!active() || ___forUpgrade) return;
            positionSelected(__instance);
        }
    }

    @SpirePatch(clz = HandCardSelectScreen.class, method = "update", paramtypez = {})
    public static class SelectedResize {
        @SpirePostfixPatch
        public static void after(HandCardSelectScreen __instance, boolean ___forUpgrade) {
            if (active() && !___forUpgrade && selectedRevision != PortraitViewport.revision()) {
                positionSelected(__instance);
            }
        }
    }

    private static void positionSelected(HandCardSelectScreen screen) {
        selectedRevision = PortraitViewport.revision();
        CardGroup selected = screen.selectedCards;
        int columns = selected.size() <= 5 ? Math.max(1, selected.size()) : (selected.size() + 1) / 2;
        float scale = Math.min(1.08f, PortraitViewport.contentWidth() * 0.85f
                / (columns * AbstractCard.IMG_WIDTH));
        float spacing = AbstractCard.IMG_WIDTH * scale + PortraitViewport.contentWidth() * 0.015f;
        for (int i = 0; i < selected.size(); i++) {
            int row = i / columns;
            int count = Math.min(columns, selected.size() - row * columns);
            AbstractCard card = selected.group.get(i);
            card.target_x = PortraitViewport.x(0.5f) + (i % columns - (count - 1) * 0.5f) * spacing;
            card.target_y = PortraitViewport.y(0.59f - row * 0.145f);
            card.targetDrawScale = scale;
            card.targetAngle = 0;
        }
    }

    @SpirePatch(clz = HandCardSelectScreen.class, method = "hoverCheck", paramtypez = {})
    @SpirePatch(clz = HandCardSelectScreen.class, method = "unhoverCheck", paramtypez = {})
    public static class StableHover {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && "HOVER_CARD_Y_POSITION".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitHandSelection.active() ? hoveredCard.target_y : $proceed(); }");
                    } else if (field.isWriter() && ("drawScale".equals(field.getFieldName())
                            || "targetDrawScale".equals(field.getFieldName()))) {
                        field.replace("{ $proceed(sts1portrait.PortraitHandSelection.active() ? $0.targetDrawScale : $1); }");
                    }
                }

                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (CardGroup.class.getName().equals(call.getClassName()) && "hoverCardPush".equals(call.getMethodName())) {
                        call.replace("{ if (!sts1portrait.PortraitHandSelection.active()) $proceed($$); }");
                    } else if (AbstractCard.class.getName().equals(call.getClassName())
                            && "isHoveredInHand".equals(call.getMethodName())) {
                        call.replace("{ $_ = $proceed(sts1portrait.PortraitHandSelection.active()"
                                + " ? $0.drawScale : $1); }");
                    }
                }
            };
        }
    }

    public static boolean hideEndTurn() {
        return active() && !PeekButton.isPeeking;
    }

    @SpirePatch(clz = OverlayMenu.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class HideEndTurn {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (EndTurnButton.class.getName().equals(call.getClassName()) && "render".equals(call.getMethodName())) {
                        call.replace("{ if (!sts1portrait.PortraitHandSelection.hideEndTurn()) $proceed($$); }");
                    }
                }
            };
        }
    }
}
