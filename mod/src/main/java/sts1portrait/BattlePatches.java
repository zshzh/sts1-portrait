package sts1portrait;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.OverlayMenu;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.scenes.AbstractScene;
import com.megacrit.cardcrawl.ui.buttons.EndTurnButton;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class BattlePatches {
    private BattlePatches() {
    }

    @SpirePatch(clz = AbstractRoom.class, method = "update", paramtypez = {})
    public static class RoomUpdate {
        @SpirePrefixPatch
        public static void prefix(AbstractRoom __instance) {
            PortraitBattle.layoutRoom(__instance);
        }
    }

    @SpirePatch(clz = AbstractDungeon.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class RoomLayers {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!AbstractScene.class.getName().equals(call.getClassName())) return;
                    if ("renderCombatRoomBg".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitUi.renderRoomBackground($0, $1, false); }");
                    } else if ("renderEventRoom".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitUi.renderRoomBackground($0, $1, true); }");
                    } else if ("renderCampfireRoom".equals(call.getMethodName())) {
                        call.replace("{ $proceed($$); sts1portrait.PortraitTouchControls.fadeCampfireEdge($0, $1); }");
                    } else if ("renderCombatRoomFg".equals(call.getMethodName())) {
                        call.replace("{ if (!sts1portrait.PortraitViewport.active()) { $proceed($$); } }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = CardGroup.class, method = "refreshHandLayout", paramtypez = {})
    public static class HandLayout {
        @SpirePostfixPatch
        public static void postfix(CardGroup __instance) {
            PortraitBattle.layoutHand(__instance);
        }
    }

    @SpirePatch(clz = CardGroup.class, method = "hoverCardPush", paramtypez = {AbstractCard.class})
    public static class HandSpacingOnHover {
        @SpirePrefixPatch
        public static SpireReturn<Void> prefix(CardGroup __instance) {
            return PortraitBattle.playingHand(__instance) ? SpireReturn.Return() : SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = CardGroup.class, method = "getHoveredCard", paramtypez = {})
    public static class HandHitTest {
        @SpirePrefixPatch
        public static SpireReturn<AbstractCard> before(CardGroup __instance) {
            boolean selecting = PortraitHandSelection.active() && AbstractDungeon.player != null
                    && AbstractDungeon.player.hand == __instance;
            if (!PortraitBattle.playingHand(__instance) && !selecting) return SpireReturn.Continue();
            ArrayList<CardQueueItem> queue = AbstractDungeon.actionManager.cardQueue;
            // Hand cards draw in list order. Check from front to back where they overlap.
            for (int i = __instance.group.size() - 1; i >= 0; i--) {
                AbstractCard card = __instance.group.get(i);
                if (!card.isHoveredInHand(card.drawScale)) continue;
                boolean queued = false;
                for (int j = 0; j < queue.size(); j++) {
                    if (queue.get(j).card == card) {
                        queued = true;
                        break;
                    }
                }
                if (!queued) return SpireReturn.Return(card);
            }
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "updateInput", paramtypez = {})
    @SpirePatch(clz = AbstractPlayer.class, method = "clickAndDragCards", paramtypez = {})
    public static class CardInput {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (AbstractCard.class.getName().equals(call.getClassName())
                            && "isHoveredInHand".equals(call.getMethodName())) {
                        call.replace("{ $_ = $proceed(sts1portrait.PortraitBattle.playingHand(this.hand)"
                                + " ? $0.drawScale : $1); }");
                    }
                }

                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    String name = field.getFieldName();
                    if (field.isWriter()) {
                        if (AbstractCard.class.getName().equals(field.getClassName())
                                && ("drawScale".equals(name) || "targetDrawScale".equals(name))) {
                            field.replace("{ $proceed(sts1portrait.PortraitBattle.handVisualScale(this, $1)); }");
                        } else if (AbstractCard.class.getName().equals(field.getClassName())
                                && "target_y".equals(name) && "clickAndDragCards".equals(field.where().getName())) {
                            field.replace("{ $proceed(sts1portrait.PortraitBattle.aimingCardY(this, $1)); }");
                        }
                        return;
                    }
                    if ("HOVER_CARD_Y_POSITION".equals(name)) {
                        field.replace("{ $_ = sts1portrait.PortraitBattle.hoverHeight(this, $proceed()); }");
                        return;
                    }
                    if (!Settings.class.getName().equals(field.getClassName())
                            || !"scale".equals(field.getFieldName())) {
                        return;
                    }
                    float threshold = dropThreshold(field);
                    if (threshold > 0.0f) {
                        field.replace("{ $_ = sts1portrait.PortraitBattle.dropZoneScale(this, $proceed(), "
                                + threshold + "f); }");
                    }
                }
            };
        }

        // In the supported build the two drop-zone tests use LDC{,_W}, GETSTATIC scale, FMUL.
        // Match the constants and multiplication so other scale reads retain their semantics.
        private static float dropThreshold(FieldAccess field) {
            CodeIterator code = field.where().getMethodInfo2().getCodeAttribute().iterator();
            ConstPool pool = field.where().getMethodInfo2().getConstPool();
            int index = field.indexOfBytecode();
            if (code.byteAt(index + 3) != Opcode.FMUL) {
                return 0.0f;
            }
            int constantIndex;
            if (index >= 3 && code.byteAt(index - 3) == Opcode.LDC_W) {
                constantIndex = code.u16bitAt(index - 2);
            } else if (index >= 2 && code.byteAt(index - 2) == Opcode.LDC) {
                constantIndex = code.byteAt(index - 1);
            } else {
                return 0.0f;
            }
            if (constantIndex >= pool.getSize() || pool.getTag(constantIndex) != ConstPool.CONST_Float) {
                return 0.0f;
            }
            float value = pool.getFloatInfo(constantIndex);
            return value == 300.0f || value == 350.0f ? value : 0.0f;
        }
    }

    @SpirePatch(clz = OverlayMenu.class, method = "showCombatPanels", paramtypez = {})
    public static class Hud {
        @SpirePrefixPatch
        public static void prefix(OverlayMenu __instance) {
            PortraitBattle.layoutHud(__instance);
        }
    }

    @SpirePatch(clz = EndTurnButton.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {})
    public static class EndTurnHeight {
        @SpirePostfixPatch
        public static void postfix(EndTurnButton __instance) {
            if (PortraitViewport.active()) {
                PortraitBattle.enlargeEndTurn(basemod.ReflectionHacks.getPrivate(__instance, EndTurnButton.class, "hb"));
            }
        }

        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return endTurnAnchor("SHOW_Y", "endTurnY");
        }
    }

    @SpirePatch(clz = EndTurnButton.class, method = "show", paramtypez = {})
    public static class EndTurnWidth {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return endTurnAnchor("SHOW_X", "endTurnX");
        }
    }

    private static ExprEditor endTurnAnchor(String fieldName, String methodName) {
        return new ExprEditor() {
            @Override
            public void edit(FieldAccess field) throws CannotCompileException {
                if (field.isReader() && fieldName.equals(field.getFieldName())) {
                    field.replace("{ $_ = sts1portrait.PortraitBattle." + methodName + "($proceed()); }");
                }
            }
        };
    }
}
