package sts1portrait;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.ui.panels.PotionPopUp;
import com.megacrit.cardcrawl.ui.panels.RenamePopup;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class PortraitPopupMenus {
    private static final PopupPlacement POTIONS = new PopupPlacement();
    private static final Pointer POTION_POINTER = new Pointer();
    private static boolean drawingPotion;

    private PortraitPopupMenus() {}

    private static final class Pointer {
        int x, y;
        boolean active;

        void begin(boolean enabled, PopupPlacement popup) {
            active = enabled;
            if (!active) return;
            x = InputHelper.mX;
            y = InputHelper.mY;
            InputHelper.mX = Math.round(popup.unmapX(x));
            InputHelper.mY = Math.round(popup.unmapY(y));
        }

        void end() {
            if (!active) return;
            active = false;
            InputHelper.mX = x;
            InputHelper.mY = y;
        }
    }

    @SpirePatch(clz = PotionPopUp.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class PotionDrawing {
        @SpirePrefixPatch
        public static void before(PotionPopUp __instance, SpriteBatch batch, float ___x, float ___y) {
            drawingPotion = PortraitScreens.enabled() && !__instance.isHidden && !__instance.targetMode;
            if (drawingPotion) {
                placePotionMenu(___x, ___y);
                POTIONS.begin(batch);
            }
        }

        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (SpriteBatch.class.getName().equals(call.getClassName()) && "draw".equals(call.getMethodName())
                            && "(Lcom/badlogic/gdx/graphics/Texture;FFFFFFFFFIIIIZZ)V".equals(call.getSignature())) {
                        call.replace("{ if (!sts1portrait.PortraitPopupMenus.drawPotionBackground($0, $1,"
                                + " $2 + 200f, $3 + 169f)) $proceed($1, $2, $3 + "
                                + "sts1portrait.PortraitPopupMenus.potionHighlightOffset($1), "
                                + "$4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16); }");
                    }
                }
            };
        }


        @SpirePostfixPatch
        public static void after(PotionPopUp __instance, SpriteBatch batch) {
            if (drawingPotion) POTIONS.end(batch);
        }
    }

    static void placePotionMenu(float x, float y) {
        if (!PortraitCombatHud.topBarActive()) {
            POTIONS.reset();
            return;
        }
        float margin = Settings.WIDTH * .006f;
        // The sides of the native 400 px texture have 20 transparent pixels.
        POTIONS.placeWithin(x, y, 360f * Settings.scale, 338f * Settings.scale,
                margin, PortraitViewport.y(0f), Settings.WIDTH - margin, PortraitViewport.y(1f));
    }

    static float potionMenuRight(float x) { return POTIONS.mapX(x + 180f * Settings.scale); }
    static float potionMenuLeft(float x) { return POTIONS.mapX(x - 180f * Settings.scale); }
    static float potionMenuBottom(float y) { return POTIONS.mapY(y - 169f * Settings.scale); }
    static float potionMenuTop(float y) { return POTIONS.mapY(y + 169f * Settings.scale); }
    static float potionRowOffset() { return Sts2Preview.compact() ? 32f * Settings.scale : 0f; }

    public static float potionHighlightOffset(Texture texture) {
        return drawingPotion && (texture == ImageMaster.POTION_UI_TOP || texture == ImageMaster.POTION_UI_BOT)
                ? potionRowOffset() : 0f;
    }

    public static boolean drawPotionBackground(SpriteBatch batch, Texture texture, float x, float y) {
        if (!drawingPotion || !PortraitCombatHud.topBarActive() || texture != ImageMaster.POTION_UI_BG) return false;
        float scale = Settings.scale;
        boolean abovePotion = Sts2Preview.compact();
        float shift = (x - POTIONS.mapX(x)) / (scale * POTIONS.scale());
        shift = Math.max(-70f, Math.min(70f, shift));
        if (!abovePotion && Math.abs(shift) < 1f) return false;

        // Keep the original body and corners. Stretch the straight portions of the top border
        // so the arrow can slide toward the clicked potion without changing button hitboxes.
        float left = x - 200f * scale;
        float bottom = y - 169f * scale;
        int bandHeight = 96;
        float bodyY = abovePotion ? bottom + bandHeight * scale : bottom;
        batch.draw(texture, left, bodyY, 400f * scale, (338 - bandHeight) * scale,
                0, bandHeight, 400, 338 - bandHeight, false, abovePotion);
        float bandY = abovePotion ? bottom : bottom + (338 - bandHeight) * scale;
        drawPotionBand(batch, texture, left, bandY, scale, bandHeight, 0, 56, 0f, 56f, abovePotion);
        drawPotionBand(batch, texture, left, bandY, scale, bandHeight, 56, 130, 56f, 130f + shift, abovePotion);
        drawPotionBand(batch, texture, left, bandY, scale, bandHeight, 130, 270, 130f + shift, 270f + shift, abovePotion);
        drawPotionBand(batch, texture, left, bandY, scale, bandHeight, 270, 344, 270f + shift, 344f, abovePotion);
        drawPotionBand(batch, texture, left, bandY, scale, bandHeight, 344, 400, 344f, 400f, abovePotion);
        return true;
    }

    private static void drawPotionBand(SpriteBatch batch, Texture texture, float left, float y, float scale,
            int height, int start, int end, float drawStart, float drawEnd, boolean flipY) {
        batch.draw(texture, left + drawStart * scale, y, (drawEnd - drawStart) * scale, height * scale,
                start, 0, end - start, height, false, flipY);
    }


    @SpirePatch(clz = PotionPopUp.class, method = "update", paramtypez = {})
    public static class PotionInput {
        @SpirePrefixPatch
        public static void before(PotionPopUp __instance, float ___x, float ___y) {
            boolean useLayout = PortraitScreens.enabled() && !__instance.isHidden && !__instance.targetMode;
            if (useLayout) placePotionMenu(___x, ___y);
            POTION_POINTER.begin(useLayout, POTIONS);
        }

        @SpirePostfixPatch
        public static void after(PotionPopUp __instance) { POTION_POINTER.end(); }
    }

}
