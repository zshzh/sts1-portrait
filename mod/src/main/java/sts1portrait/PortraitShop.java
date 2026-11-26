package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.ShopSpeechBubble;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitShop {
    private static final float ITEM_SCALE = 1.57f;
    private static final Matrix4 previous = new Matrix4();
    private static final Matrix4 enlarged = new Matrix4();
    private static final Matrix4 speechBase = new Matrix4();
    private static final Matrix4 speechLayout = new Matrix4();


    private PortraitShop() {}

    @SpirePatch(clz = ShopScreen.class, method = SpirePatch.CLASS)
    public static class Slots {
        public static final SpireField<IdentityHashMap<AbstractCard, Integer>> cards = new SpireField<>(() -> null);
    }

    public static boolean enabled() {
        return PortraitScreens.enabled() && AbstractDungeon.shopScreen != null
                && Slots.cards.get(AbstractDungeon.shopScreen) != null;
    }

    @SpirePatch(clz = ShopScreen.class, method = "init", paramtypez = {ArrayList.class, ArrayList.class})
    public static class AssignSlots {
        @SpirePostfixPatch
        public static void after(ShopScreen __instance) {
            int count = __instance.coloredCards.size() + __instance.colorlessCards.size();
            if (!PortraitScreens.enabled() || count > 8) {
                Slots.cards.set(__instance, null);
                return;
            }
            IdentityHashMap<AbstractCard, Integer> slots = new IdentityHashMap<>();
            for (AbstractCard card : __instance.coloredCards) slots.put(card, slots.size());
            for (AbstractCard card : __instance.colorlessCards) slots.put(card, slots.size());
            Slots.cards.set(__instance, slots);
        }
    }

    @SpirePatch(clz = ShopScreen.class, method = "updateCards", paramtypez = {})
    public static class CardGrid {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(ShopScreen __instance, float ___rugY) {
            if (!enabled()) return SpireReturn.Continue();
            IdentityHashMap<AbstractCard, Integer> slots = Slots.cards.get(__instance);
            syncRestockedSlots(__instance, slots, ___rugY);
            update(__instance.coloredCards, slots, ___rugY);
            update(__instance.colorlessCards, slots, ___rugY);
            return SpireReturn.Return();
        }

        private static float cardScale() {
            float height = Math.min(PortraitViewport.referenceHeight(), PortraitViewport.contentHeight());
            return Math.min(1.10f, Math.min(layoutWidth() * .255f / AbstractCard.IMG_WIDTH,
                    height * .123f / AbstractCard.IMG_HEIGHT));
        }

        private static float slotX(int slot) {
            return PortraitViewport.x(0.20f + (slot % 3) * 0.30f);
        }

        private static float slotY(int slot, float rugY) {
            return PortraitViewport.y(0.775f - (slot / 3) * 0.151f) + slide(rugY);
        }

        private static void syncRestockedSlots(ShopScreen screen, IdentityHashMap<AbstractCard, Integer> slots,
                                               float rugY) {
            boolean[] used = new boolean[8];
            for (AbstractCard card : screen.coloredCards) {
                Integer slot = slots.get(card);
                if (slot != null && slot >= 0 && slot < used.length) used[slot] = true;
            }
            for (AbstractCard card : screen.colorlessCards) {
                Integer slot = slots.get(card);
                if (slot != null && slot >= 0 && slot < used.length) used[slot] = true;
            }
            float scale = cardScale();
            assignMissing(screen.coloredCards, slots, used, rugY, scale);
            assignMissing(screen.colorlessCards, slots, used, rugY, scale);
        }

        private static void assignMissing(ArrayList<AbstractCard> cards, IdentityHashMap<AbstractCard, Integer> slots,
                                          boolean[] used, float rugY, float scale) {
            for (AbstractCard card : cards) {
                if (slots.get(card) != null) continue;
                int best = -1;
                float bestDist = Float.POSITIVE_INFINITY;
                for (int s = 0; s < used.length; s++) {
                    if (used[s]) continue;
                    float dx = card.current_x - slotX(s);
                    float dy = card.current_y - slotY(s, rugY);
                    float dist = dx * dx + dy * dy;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = s;
                    }
                }
                if (best >= 0) {
                    used[best] = true;
                    slots.put(card, best);
                    card.drawScale = scale;
                    card.targetDrawScale = scale;
                }
            }
        }

        private static void update(ArrayList<AbstractCard> cards, IdentityHashMap<AbstractCard, Integer> slots,
                                   float rugY) {
            float scale = cardScale();
            for (AbstractCard card : cards) {
                Integer slot = slots.get(card);
                if (slot != null) {
                    card.target_x = card.current_x = slotX(slot);
                    card.target_y = card.current_y = slotY(slot, rugY);
                    card.targetDrawScale = scale;
                }
                card.update();
                card.updateHoverLogic();
                if (slot != null) {
                    card.targetDrawScale = card.hb.hovered ? Math.max(scale * 1.12f, 1.0f) : scale;
                }
            }
        }
    }

    private static float layoutWidth() {
        return Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth());
    }

    public static float itemX(int slot) {
        return PortraitViewport.x(0.22f + slot * 0.28f);
    }

    public static float itemY(float rugY, boolean potion) {
        return PortraitViewport.y(potion ? 0.199f : 0.311f) + slide(rugY);
    }

    private static float slide(float rugY) {
        return rugY - Math.max(0f, PortraitViewport.referenceCanvasHeight() * 0.5f - 540f * Settings.yScale);
    }

    @SpirePatch(clz = ShopScreen.class, method = "open", paramtypez = {})
    @SpirePatch(clz = ShopScreen.class, method = "updateRug", paramtypez = {})
    public static class RugTravel {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && Settings.class.getName().equals(field.getClassName())
                            && "HEIGHT".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitShop.enabled()"
                                + " ? sts1portrait.PortraitViewport.referenceCanvasHeight() : $proceed(); }");
                    }
                }
            };
        }
    }

    public static void drawRug(SpriteBatch batch, Texture rug, float x, float y, float width, float height) {
        if (!enabled()) {
            batch.draw(rug, x, y, width, height);
            return;
        }
        float bottom = PortraitViewport.y(0.115f);
        batch.draw(rug, 0, bottom + slide(y), Settings.WIDTH, Settings.HEIGHT - bottom);
    }

    @SpirePatch(clz = ShopScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class Rug {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                private boolean rugHandled;

                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (rugHandled || !SpriteBatch.class.getName().equals(call.getClassName())
                            || !"draw".equals(call.getMethodName()) || !"(Lcom/badlogic/gdx/graphics/Texture;FFFF)V"
                                                .equals(call.getSignature())) return;
                    rugHandled = true;
                    call.replace("{ sts1portrait.PortraitShop.drawRug($0, $$); }");
                }
            };
        }
    }

    private static float speechScale(Hitbox nativeArea) {
        return Math.min(1f, PortraitViewport.contentHeight() * .071f / Math.max(1f, nativeArea.height));
    }

    public static void renderSpeech(AbstractGameEffect effect, SpriteBatch batch, ShopSpeechBubble bubble) {
        if (!enabled() || bubble == null) {
            effect.render(batch);
            return;
        }
        Hitbox nativeArea = bubble.hb;
        float x = PortraitViewport.x(.5f);
        float y = PortraitViewport.y(.875f);
        float scale = speechScale(nativeArea);

        speechBase.set(batch.getTransformMatrix());
        speechLayout.set(speechBase).translate(x, y, 0f).scale(scale, scale, 1f)
                .translate(-nativeArea.cX, -nativeArea.cY, 0f);
        batch.setTransformMatrix(speechLayout);
        try {
            effect.render(batch);
        } finally {
            batch.setTransformMatrix(speechBase);

        }
    }

    public static void updateSpeech(ShopSpeechBubble bubble) {
        if (!enabled()) {
            bubble.update();
            return;
        }
        Hitbox nativeArea = bubble.hb;
        float x = PortraitViewport.x(.5f);
        float y = PortraitViewport.y(.875f);
        float scale = speechScale(nativeArea);

        int previousX = InputHelper.mX;
        int previousY = InputHelper.mY;
        InputHelper.mX = Math.round(nativeArea.cX + ((previousX) - x) / scale);
        InputHelper.mY = Math.round(nativeArea.cY + ((previousY) - y) / scale);
        try {
            bubble.update();
        } finally {
            InputHelper.mX = previousX;
            InputHelper.mY = previousY;
        }
    }

    @SpirePatch(clz = ShopScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = ShopScreen.class, method = "updateSpeech", paramtypez = {})
    public static class MerchantSpeech {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    boolean bubble = ShopSpeechBubble.class.getName().equals(call.getClassName());
                    if (bubble && "update".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitShop.updateSpeech($0); }");
                    } else if ("render".equals(call.getMethodName()) && (bubble
                            || "com.megacrit.cardcrawl.vfx.SpeechTextEffect".equals(call.getClassName()))) {
                        call.replace("{ sts1portrait.PortraitShop.renderSpeech($0, $1, speechBubble); }");
                    }
                }
            };
        }
    }

    private static void moveItem(Hitbox hitbox, float x, float y) {
        float size = layoutWidth() * .115f;
        hitbox.width = size;
        hitbox.height = size;
        hitbox.move(x, y);
    }

    @SpirePatch(clz = StoreRelic.class, method = "update", paramtypez = {float.class})
    @SpirePatch(clz = StorePotion.class, method = "update", paramtypez = {float.class})
    public static class ItemPlacement {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isWriter()) return;
                    String name = field.getFieldName();
                    String position;
                    if ("currentX".equals(name) || "posX".equals(name)) {
                        position = "itemX(slot)";
                    } else if ("currentY".equals(name) || "posY".equals(name)) {
                        position = "itemY($1 - " + ("posY".equals(name) ? "200f" : "400f")
                                + " * com.megacrit.cardcrawl.core.Settings.yScale, " + "posY".equals(name) + ")";
                    } else return;
                    field.replace("{ $proceed(sts1portrait.PortraitShop.enabled() && !isPurchased"
                            + " ? sts1portrait.PortraitShop." + position + " : $1); }");
                }
            };
        }
    }

    @SpirePatch(clz = StoreRelic.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = StorePotion.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class ItemDrawing {
        @SpirePrefixPatch
        public static void before(Object __instance, SpriteBatch batch) {
            if (!enabled()) return;
            previous.set(batch.getTransformMatrix());
            if (__instance instanceof StoreRelic) {
                StoreRelic item = (StoreRelic) __instance;
                if (!item.isPurchased && item.relic != null) {
                    enlarge(batch, item.relic.currentX, item.relic.currentY, ITEM_SCALE);
                }
            } else {
                StorePotion item = (StorePotion) __instance;
                if (!item.isPurchased && item.potion != null) {
                    enlarge(batch, item.potion.posX, item.potion.posY, ITEM_SCALE);
                }
            }
        }

        @SpirePostfixPatch
        public static void after(Object __instance, SpriteBatch batch) {
            if (enabled()) batch.setTransformMatrix(previous);
        }
    }

    @SpirePatch(clz = StoreRelic.class, method = "update", paramtypez = {float.class})
    @SpirePatch(clz = StorePotion.class, method = "update", paramtypez = {float.class})
    public static class ItemHitboxes {
        @SpirePrefixPatch
        public static void before(Object __instance, float rugY, int ___slot) {
            if (!enabled()) return;
            if (__instance instanceof StoreRelic) {
                StoreRelic item = (StoreRelic) __instance;
                AbstractRelic relic = item.relic;
                if (!item.isPurchased && relic != null) moveItem(relic.hb, itemX(___slot), itemY(rugY, false));
            } else {
                StorePotion item = (StorePotion) __instance;
                AbstractPotion potion = item.potion;
                if (!item.isPurchased && potion != null) moveItem(potion.hb, itemX(___slot), itemY(rugY, true));
            }
        }
    }

    private static void enlarge(SpriteBatch batch, float x, float y, float scale) {
        enlarged.set(previous).translate(x * (1f - scale), y * (1f - scale), 0).scale(scale, scale, 1);
        batch.setTransformMatrix(enlarged);
    }

    public static float purgeX() {
        return PortraitViewport.x(0.80f);
    }

    public static float purgeY(float rugY) {
        return PortraitViewport.y(0.473f) + slide(rugY);
    }

    @SpirePatch(clz = ShopScreen.class, method = "updatePurgeCard", paramtypez = {})
    public static class PurgePlacement {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isWriter()) return;
                    String position;
                    if ("purgeCardX".equals(field.getFieldName())) position = "purgeX()";
                    else if ("purgeCardY".equals(field.getFieldName())) position = "purgeY(rugY)";
                    else return;
                    field.replace("{ $proceed(sts1portrait.PortraitShop.enabled()"
                            + " ? sts1portrait.PortraitShop." + position + " : $1); }");
                }
            };
        }
    }

    @SpirePatch(clz = ShopScreen.class, method = "renderCardsAndPrices", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = ShopScreen.class, method = "renderPurge", paramtypez = {SpriteBatch.class})
    public static class PriceFont {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!"com.megacrit.cardcrawl.helpers.FontHelper".equals(call.getClassName())
                            || !"renderFontLeftTopAligned".equals(call.getMethodName())) return;
                    call.replace("{ if (!sts1portrait.PortraitShop.enabled()) { $proceed($$); } else {"
                            + " float fontX = $2.getData().scaleX; float fontY = $2.getData().scaleY;"
                            + " $2.getData().setScale(fontX * 1.28f, fontY * 1.28f);"
                            + " try { $proceed($$); } finally { $2.getData().setScale(fontX, fontY); } } }");
                }
            };
        }
    }
}
