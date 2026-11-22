package sts1portrait;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.leaderboards.FilterButton;
import com.megacrit.cardcrawl.screens.leaderboards.LeaderboardScreen;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class PortraitLeaderboard {
    private static final Section FILTERS = new Section(
            440f, 665f, .76f, .73f, .28f, 520f, 560f);
    private static final Section RESULTS = new Section(
            1310f, 490f, .36f, .86f, .34f, 1050f, 760f);

    private PortraitLeaderboard() {}

    private static final class Section {
        final Matrix4 original = new Matrix4();
        final Matrix4 moved = new Matrix4();
        final float nativeX, nativeY, targetY, width, height, nativeWidth, nativeHeight;
        float x, y, scale, centerX, centerY, size;
        int mouseX, mouseY;

        Section(float nativeX, float nativeY, float targetY,
                float width, float height, float nativeWidth, float nativeHeight) {
            this.nativeX = nativeX;
            this.nativeY = nativeY;
            this.targetY = targetY;
            this.width = width;
            this.height = height;
            this.nativeWidth = nativeWidth;
            this.nativeHeight = nativeHeight;
        }

        void layout() {
            float w = PortraitViewport.contentWidth() * width / nativeWidth;
            float h = PortraitViewport.contentHeight() * height / nativeHeight;
            size = Math.min(w, h);
            scale = size / Settings.scale;
            centerX = PortraitViewport.x(.5f);
            centerY = PortraitViewport.y(targetY);
            x = centerX - nativeX * size;
            y = centerY - nativeY * size;
        }

        void begin(SpriteBatch batch) {
            if (!PortraitScreens.enabled()) return;
            layout();
            original.set(batch.getTransformMatrix());
            batch.setTransformMatrix(moved.set(original).translate(x, y, 0f).scale(scale, scale, 1f));
        }

        void end(SpriteBatch batch) {
            if (PortraitScreens.enabled()) batch.setTransformMatrix(original);
        }

        void map() {
            layout();
            mouseX = InputHelper.mX;
            mouseY = InputHelper.mY;
            InputHelper.mX = Math.round((mouseX - x) / scale);
            InputHelper.mY = Math.round((mouseY - y) / scale);
        }

        void unmap() {
            InputHelper.mX = mouseX;
            InputHelper.mY = mouseY;
        }
    }

    @SpirePatch(clz = LeaderboardScreen.class, method = "renderFilters", paramtypez = {SpriteBatch.class})
    public static class FilterDrawing {
        @SpirePrefixPatch public static void before(LeaderboardScreen __instance, SpriteBatch sb) { FILTERS.begin(sb); }
        @SpirePostfixPatch public static void after(LeaderboardScreen __instance, SpriteBatch sb) {
            FILTERS.end(sb);
        }
    }

    @SpirePatch(clz = LeaderboardScreen.class, method = "renderScoreHeaders", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = LeaderboardScreen.class, method = "renderScores", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = LeaderboardScreen.class, method = "renderArrows", paramtypez = {SpriteBatch.class})
    public static class ResultDrawing {
        @SpirePrefixPatch public static void before(LeaderboardScreen __instance, SpriteBatch sb) { RESULTS.begin(sb); }
        @SpirePostfixPatch public static void after(LeaderboardScreen __instance, SpriteBatch sb) {
            RESULTS.end(sb);
        }
    }

    public static void updateFilter(FilterButton filter) {
        boolean mapped = PortraitScreens.enabled();
        if (mapped) FILTERS.map();
        try {
            filter.update();
        } finally {
            if (mapped) FILTERS.unmap();
        }
    }

    @SpirePatch(clz = LeaderboardScreen.class, method = "update", paramtypez = {})
    public static class FilterPointer {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (FilterButton.class.getName().equals(call.getClassName()) && "update".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitLeaderboard.updateFilter($0); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = LeaderboardScreen.class, method = "updateArrows", paramtypez = {})
    public static class ResultPointer {
        @SpirePrefixPatch public static void before() { if (PortraitScreens.enabled()) RESULTS.map(); }
        @SpirePostfixPatch public static void after() { if (PortraitScreens.enabled()) RESULTS.unmap(); }
    }
}
