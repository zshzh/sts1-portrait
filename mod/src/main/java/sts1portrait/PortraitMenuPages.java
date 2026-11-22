package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.custom.CustomModeScreen;
import com.megacrit.cardcrawl.screens.custom.CustomMod;
import com.megacrit.cardcrawl.screens.options.DropdownMenu;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.runHistory.RunHistoryScreen;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitMenuPages {
    private static final float FILTER_SCALE = 1.35f;
    private static final Rectangle menuClip = new Rectangle();
    private static boolean clipping;

    private PortraitMenuPages() {}

    public static float lift(float nativeY) {
        return PortraitScreens.enabled() ? nativeY + PortraitViewport.contentHeight() * 0.545f : nativeY;
    }

    static float footerTop() {
        return PortraitViewport.y(0.13f);
    }

    public static void beginMenuContent(SpriteBatch batch) {
        if (!PortraitScreens.enabled()) return;
        batch.flush();
        menuClip.set(0, footerTop(), Settings.WIDTH, PortraitViewport.y(1f) - footerTop());
        PortraitDesktopViewport.clip(menuClip);
        clipping = ScissorStack.pushScissors(menuClip);
    }

    public static void endMenuContent(SpriteBatch batch) {
        if (!clipping) return;
        batch.flush();
        ScissorStack.popScissors();
        clipping = false;
    }

    public static void updateCustomModifier(CustomMod option, float scrollY) {
        int mouse = InputHelper.mY;
        if (PortraitScreens.enabled() && mouse < footerTop()) InputHelper.mY = -100000;
        try {
            option.update(scrollY);
        } finally {
            InputHelper.mY = mouse;
        }
    }

    @SpirePatch(clz = CustomModeScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = StatsScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = CustomModeScreen.class, method = "updateMods", paramtypez = {})
    public static class MenuFooter {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    switch (call.getMethodName()) {
                        case "renderScreen":
                        case "renderStatScreen":
                            call.replace("{ sts1portrait.PortraitMenuPages.beginMenuContent($1);"
                                    + " try { $proceed($$); } finally { sts1portrait.PortraitMenuPages.endMenuContent($1); } }");
                            break;
                        case "renderSeed":
                            if (CustomModeScreen.class.getName().equals(call.getClassName())) {
                                call.replace("{ sts1portrait.PortraitMenuPages.endMenuContent($1);"
                                        + " $proceed($$); sts1portrait.PortraitMenuPages.beginMenuContent($1); }");
                            }
                            break;
                        case "render":
                            String type = call.getClassName();
                            if ("com.megacrit.cardcrawl.ui.buttons.GridSelectConfirmButton".equals(type)) {
                                call.replace("{ $proceed($$); sts1portrait.PortraitMenuPages.beginMenuContent($1); }");
                            } else if ("com.megacrit.cardcrawl.ui.panels.SeedPanel".equals(type)) {
                                call.replace("{ sts1portrait.PortraitMenuPages.endMenuContent($1); $proceed($$); }");
                            }
                            break;
                        case "update":
                            if (CustomMod.class.getName().equals(call.getClassName())) {
                                call.replace("{ sts1portrait.PortraitMenuPages.updateCustomModifier($0, $1); }");
                            }
                            break;
                        default: break;
                    }
                }
            };
        }
    }

    @SpirePatch(clz = CustomModeScreen.class, method = "open", paramtypez = {})
    public static class CustomScrollStart {
        @SpirePostfixPatch
        public static void after(CustomModeScreen __instance) {
            if (!PortraitScreens.enabled()) return;
            float start = ReflectionHacks.getPrivate(__instance, CustomModeScreen.class, "scrollLowerBound");
            ReflectionHacks.setPrivate(__instance, CustomModeScreen.class, "scrollY", start);
            ReflectionHacks.setPrivate(__instance, CustomModeScreen.class, "targetY", start);
            ReflectionHacks.setPrivate(__instance, CustomModeScreen.class, "grabbedScreen", false);
            ScrollBar bar = ReflectionHacks.getPrivate(__instance, CustomModeScreen.class, "scrollBar");
            bar.parentScrolledToPercent(0f);
        }
    }

    @SpirePatch(clz = CustomModeScreen.class, method = "updateScrolling", paramtypez = {})
    @SpirePatch(clz = CustomModeScreen.class, method = "scrolledUsingBar", paramtypez = {float.class})
    @SpirePatch(clz = CustomModeScreen.class, method = "calculateScrollBounds", paramtypez = {})
    public static class CustomScrollBounds {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isWriter()) return;
                    if ("scrollY".equals(field.getFieldName())) {
                        field.replace("{ $proceed(sts1portrait.PortraitScreens.enabled()"
                                + " ? Math.max(scrollLowerBound, Math.min(scrollUpperBound, $1)) : $1); }");
                    } else if ("scrollLowerBound".equals(field.getFieldName())) {
                        field.replace("{ $proceed(sts1portrait.PortraitMenuPages.lift($1)); }");
                    } else if ("scrollUpperBound".equals(field.getFieldName())) {
                        field.replace("{ $proceed(sts1portrait.PortraitScreens.enabled()"
                                + " ? Math.max(sts1portrait.PortraitViewport.contentHeight() * 0.6f,"
                                + " $1 + sts1portrait.PortraitViewport.contentHeight() * 0.14f) : $1); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = StatsScreen.class, method = "renderStatScreen", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = RunHistoryScreen.class, method = "renderRunHistoryScreen", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = RunHistoryScreen.class, method = "renderFilters", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = RunHistoryScreen.class, method = "layoutTinyCards",
            paramtypez = {java.util.ArrayList.class, float.class, float.class})
    public static class ContentPosition {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && "scrollY".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitMenuPages.lift($proceed()); }");
                    }
                }

                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!"renderFilters".equals(call.where().getName())
                            || !DropdownMenu.class.getName().equals(call.getClassName())
                            || !"render".equals(call.getMethodName())) return;
                    call.replace("{ sts1portrait.PortraitMenuPages.renderFilter($0, $1, $2, $3,"
                            + " $0 == winLossFilter ? 0 : $0 == characterFilter ? 1 : 2); }");
                }
            };
        }
    }

    @SpirePatch(clz = DropdownMenu.class, method = SpirePatch.CLASS)
    public static class FilterPlacement {
        public static final SpireField<HistoryFilter> layout = new SpireField<>(() -> null);
    }

    public static final class HistoryFilter {

        private final Matrix4 previous = new Matrix4();
        private final Matrix4 enlarged = new Matrix4();
        private float left, top, scale;

        void render(DropdownMenu menu, SpriteBatch batch, float x, float y) {
            float sourceWidth = Math.max(1f, menu.approximateOverallWidth());
            scale = Math.min(FILTER_SCALE, PortraitViewport.contentWidth() * .28f / sourceWidth);
            left = x;
            top = y;
            previous.set(batch.getTransformMatrix());
            enlarged.set(previous).translate(left * (1f - scale), top * (1f - scale), 0)
                    .scale(scale, scale, 1f);
            batch.setTransformMatrix(enlarged);
            try {
                menu.render(batch, left, top);
            } finally {
                batch.setTransformMatrix(previous);

            }
        }

        void update(DropdownMenu menu) {
            int x = InputHelper.mX;
            int y = InputHelper.mY;
            InputHelper.mX = Math.round(left + (x - left) / scale);
            InputHelper.mY = Math.round(top + (y - top) / scale);
            try {
                menu.update();
            } finally {
                InputHelper.mX = x;
                InputHelper.mY = y;
            }
        }
    }

    public static void renderFilter(DropdownMenu menu, SpriteBatch batch, float x, float y, int index) {
        if (!PortraitScreens.enabled()) {
            menu.render(batch, x, y);
            return;
        }
        HistoryFilter layout = FilterPlacement.layout.get(menu);
        if (layout == null) {
            layout = new HistoryFilter();
            FilterPlacement.layout.set(menu, layout);
        }
        layout.render(menu, batch, PortraitViewport.x(0.06f + index * 0.295f),
                y + index * 54f * Settings.scale + PortraitViewport.contentHeight() * 0.065f);
    }

    public static void updateFilter(DropdownMenu menu) {
        HistoryFilter layout = PortraitScreens.enabled() ? FilterPlacement.layout.get(menu) : null;
        if (layout == null) menu.update();
        else layout.update(menu);
    }
    @SpirePatch(clz = RunHistoryScreen.class, method = "update", paramtypez = {})
    public static class HistoryFilters {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (DropdownMenu.class.getName().equals(call.getClassName()) && "update".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitMenuPages.updateFilter($0); }");
                    }
                }
            };
        }
    }
}
