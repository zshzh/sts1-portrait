package sts1portrait;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuPanelButton;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Locale;

/** Portrait placement for the Play and Compendium tiles. */
public final class PortraitMenuPanels {
    private static final Matrix4 PREVIOUS = new Matrix4();
    private static final Matrix4 MOVED = new Matrix4();
    private static float x, y, scale;

    private PortraitMenuPanels() {}

    private static boolean place(MainMenuPanelButton button) {
        if (!PortraitScreens.enabled() || CardCrawlGame.mainMenuScreen == null
                || CardCrawlGame.mainMenuScreen.screen != MainMenuScreen.CurScreen.PANEL_MENU) return false;
        ArrayList<MainMenuPanelButton> panels = CardCrawlGame.mainMenuScreen.panelScreen.panels;
        int index = panels.indexOf(button);
        if (index < 0) return false;
        int row = index / 2;
        int rows = (panels.size() + 1) / 2;
        int inRow = Math.min(2, panels.size() - row * 2);
        float step = Math.min(.345f, .72f / rows);
        float cx = PortraitViewport.x(.5f + (index % 2 - (inRow - 1) * .5f) * .45f);
        float cy = PortraitViewport.y(.535f + ((rows - 1) * .5f - row) * step);
        // The native texture is 800 units tall and can grow slightly on hover.
        scale = Math.min(PortraitViewport.contentWidth() * .37f / button.hb.width,
                PortraitViewport.contentHeight() * step / (900f * Settings.scale));
        x = cx - button.hb.cX * scale;
        y = cy - button.hb.cY * scale;

        return true;
    }

    @SpirePatch(clz = MainMenuPanelButton.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class Drawing {
        private static boolean applied;

        @SpirePrefixPatch
        public static void before(MainMenuPanelButton __instance, SpriteBatch sb,
                MainMenuPanelButton.PanelClickResult ___result, String ___header) {
            applied = place(__instance);
            if (!applied) return;
            PREVIOUS.set(sb.getTransformMatrix());
            sb.setTransformMatrix(MOVED.set(PREVIOUS).translate(x, y, 0f).scale(scale, scale, 1f));
        }

        @SpirePostfixPatch
        public static void after(MainMenuPanelButton __instance, SpriteBatch sb) {
            if (applied) sb.setTransformMatrix(PREVIOUS);
        }
    }

    @SpirePatch(clz = MainMenuPanelButton.class, method = "update", paramtypez = {})
    public static class Pointer {
        private static boolean applied;
        private static int mouseX, mouseY;

        @SpirePrefixPatch
        public static void before(MainMenuPanelButton __instance, MainMenuPanelButton.PanelClickResult ___result,
                String ___header) {
            applied = place(__instance);
            if (!applied) return;
            mouseX = InputHelper.mX;
            mouseY = InputHelper.mY;
            InputHelper.mX = Math.round((mouseX - x) / scale);
            InputHelper.mY = Math.round((mouseY - y) / scale);
        }

        @SpirePostfixPatch
        public static void after() {
            if (!applied) return;
            InputHelper.mX = mouseX;
            InputHelper.mY = mouseY;
        }
    }
}
