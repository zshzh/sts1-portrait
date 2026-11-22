package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.map.DungeonMap;
import com.megacrit.cardcrawl.map.Legend;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;
import com.megacrit.cardcrawl.vfx.MapDot;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitMap {
    private static Hitbox legendButton;

    private static boolean showLegend;
    private static DungeonMap cachedMap;
    private static Legend nativeLegend;
    private static final Rectangle routeClip = new Rectangle();
    private static boolean clipping;
    private static int layoutRevision = -1;
    private static float previousStart;
    private static final Matrix4 legendBase = new Matrix4();
    private static final Matrix4 legendLayout = new Matrix4();

    private static float legendX, legendY, legendWidth, legendHeight, legendScale;

    private PortraitMap() {
    }

    public static float floorSpacing() {
        return PortraitViewport.referenceHeight() * 0.125f;
    }

    public static float columnSpacing() {
        return PortraitViewport.contentWidth() * 0.14f;
    }

    public static float startOffset() {
        return PortraitViewport.y(0.19f) - 180f * Settings.scale;
    }

    public static float upperOffset(int lastFloor) {
        return Math.min(startOffset(), PortraitViewport.y(0.66f) - 180f * Settings.scale
                - lastFloor * floorSpacing());
    }

    private static float routeBottom() {
        return PortraitViewport.y(0f) + PortraitViewport.referenceWidth() * 0.15f;
    }

    private static float routeTop() {
        return PortraitCombatHud.panelBottom();
    }

    public static void beginRoutes(SpriteBatch batch) {
        if (!PortraitViewport.active() || AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MAP) return;
        batch.flush();
        routeClip.set(0, routeBottom(), Settings.WIDTH, routeTop() - routeBottom());
        PortraitDesktopViewport.clip(routeClip);
        clipping = ScissorStack.pushScissors(routeClip);
    }

    public static void endRoutes(SpriteBatch batch) {
        if (!clipping) return;
        batch.flush();
        ScissorStack.popScissors();
        clipping = false;
    }

    public static boolean routePointerVisible() {
        return !PortraitViewport.active() || Settings.isControllerMode
                || (InputHelper.mY >= routeBottom() && InputHelper.mY <= routeTop()
                    && InputHelper.mX >= 0 && InputHelper.mX < Settings.WIDTH);
    }

    @SpirePatch(clz = DungeonMapScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class ClipRoutes {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!DungeonMap.class.getName().equals(call.getClassName())) return;
                    if ("render".equals(call.getMethodName())) {
                        call.replace("{ $proceed($$); sts1portrait.PortraitMap.beginRoutes($1); }");
                    } else if ("renderBossIcon".equals(call.getMethodName())) {
                        call.replace("{ try { $proceed($$); } finally { sts1portrait.PortraitMap.endRoutes($1); } }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = DungeonMapScreen.class, method = SpirePatch.STATICINITIALIZER)
    public static class ScrollGeometry {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isWriter()) return;
                    String expression;
                    switch (field.getFieldName()) {
                        case "MAP_UPPER_SCROLL_DEFAULT": expression = "upperOffset(14)"; break;
                        case "MAP_UPPER_SCROLL_FINAL_ACT": expression = "upperOffset(3)"; break;
                        case "MAP_SCROLL_LOWER": expression = "startOffset()"; break;
                        case "ICON_SPACING_Y": expression = "floorSpacing()"; break;
                        default: return;
                    }
                    field.replace("{ $proceed(sts1portrait.PortraitViewport.active()"
                            + " ? sts1portrait.PortraitMap." + expression + " : $1); }");
                }
            };
        }
    }

    @SpirePatch(clz = MapRoomNode.class, method = SpirePatch.STATICINITIALIZER)
    @SpirePatch(clz = MapEdge.class, method = SpirePatch.STATICINITIALIZER)
    public static class RouteWidth {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isWriter()) return;
                    if ("OFFSET_X".equals(field.getFieldName())) {
                        field.replace("{ $proceed(sts1portrait.PortraitViewport.active()"
                                + " ? sts1portrait.PortraitMap.initialOffsetX() : $1); }");
                    } else if ("SPACING_X".equals(field.getFieldName()) || "SPACE_X".equals(field.getFieldName())) {
                        field.replace("{ $proceed(sts1portrait.PortraitViewport.active()"
                                + " ? sts1portrait.PortraitMap.initialColumnSpacing() : $1); }");
                    }
                }
            };
        }
    }

    public static float initialOffsetX() {
        return PortraitViewport.referenceX(.08f);
    }

    public static float initialColumnSpacing() {
        return PortraitViewport.referenceWidth() * .14f;
    }

    @SpirePatch(clz = MapRoomNode.class, method = "update", paramtypez = {})
    @SpirePatch(clz = MapRoomNode.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class NodeColumns {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isReader() || !MapRoomNode.class.getName().equals(field.getClassName())) return;
                    if ("SPACING_X".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitViewport.active()"
                                + " ? sts1portrait.PortraitMap.columnSpacing() : $proceed(); }");
                    } else if ("OFFSET_X".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitViewport.active()"
                                + " ? sts1portrait.PortraitViewport.x(.08f) : $proceed(); }");
                    }
                }
            };
        }
    }

    public static float dotX(float nativeX) {
        if (!PortraitViewport.active()) return nativeX;
        return PortraitViewport.x(.08f)
                + (nativeX - initialOffsetX()) * (PortraitViewport.contentWidth() / PortraitViewport.referenceWidth());
    }

    @SpirePatch(clz = MapDot.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class EdgeColumns {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && MapDot.class.getName().equals(field.getClassName())
                            && "x".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitMap.dotX($proceed()); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = DungeonMapScreen.class, method = "update", paramtypez = {})
    @SpirePatch(clz = DungeonMapScreen.class, method = "resetScrolling", paramtypez = {})
    @SpirePatch(clz = DungeonMapScreen.class, method = "updateAnimation", paramtypez = {})
    public static class ScrollBounds {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && "MAP_SCROLL_LOWER".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitViewport.active()"
                                + " ? sts1portrait.PortraitMap.startOffset() : $proceed(); }");
                    }
                }
            };
        }
    }

    private static float refreshScrollBounds(DungeonMapScreen screen) {
        float lower = startOffset();
        float upper = upperOffset(AbstractDungeon.map.size() - 1);
        ReflectionHacks.setPrivate(screen, DungeonMapScreen.class, "mapScrollUpperLimit", upper);
        if (layoutRevision != PortraitViewport.revision()) {
            if (layoutRevision >= 0) {
                float change = lower - previousStart;
                float target = ReflectionHacks.getPrivate(screen, DungeonMapScreen.class, "targetOffsetY");
                DungeonMapScreen.offsetY = Math.max(upper, Math.min(lower, DungeonMapScreen.offsetY + change));
                ReflectionHacks.setPrivate(screen, DungeonMapScreen.class, "targetOffsetY",
                        Math.max(upper, Math.min(lower, target + change)));
            }
            layoutRevision = PortraitViewport.revision();
            previousStart = lower;
        }
        return upper;
    }

    public static float bossBottom(float mapOffset) {
        return 180f * Settings.scale + (AbstractDungeon.map.size() - 0.55f) * floorSpacing() - mapOffset;
    }

    @SpirePatch(clz = DungeonMap.class, method = "update", paramtypez = {})
    @SpirePatch(clz = DungeonMap.class, method = "renderBossIcon", paramtypez = {SpriteBatch.class})
    public static class BossPlacement {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && "BOSS_OFFSET_Y".equals(field.getFieldName())) {
                        field.replace("{ $_ = sts1portrait.PortraitViewport.active()"
                                + " ? sts1portrait.PortraitMap.bossBottom(mapOffsetY) : $proceed(); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = MapRoomNode.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {int.class, int.class})
    public static class NodeTouchArea {
        @SpirePostfixPatch
        public static void postfix(MapRoomNode __instance, int column, int floor) {
            if (PortraitViewport.active()) {
                Hitbox hb = __instance.hb;
                float size = PortraitViewport.referenceWidth() * 0.098f;
                hb.width = Math.max(hb.width, size);
                hb.height = Math.max(hb.height, size);
            }
        }
    }

    @SpirePatch(clz = MapRoomNode.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class NodeDrawing {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && MapRoomNode.class.getName().equals(field.getClassName())
                            && "scale".equals(field.getFieldName())) {
                        field.replace("{ $_ = $proceed() * (sts1portrait.PortraitViewport.active() ? 1.75f : 1.0f); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = DungeonMapScreen.class, method = "open", paramtypez = {boolean.class})
    public static class RouteFocus {
        @SpirePostfixPatch
        public static void postfix(DungeonMapScreen __instance, boolean initial) {
            if (!PortraitViewport.active()) return;
            MapRoomNode current = AbstractDungeon.currMapNode;
            float focus = startOffset() - Math.max(0, current == null ? 0 : current.y) * floorSpacing();
            float limit = refreshScrollBounds(__instance);
            DungeonMapScreen.offsetY = Math.max(limit, focus);
            ReflectionHacks.setPrivate(__instance, DungeonMapScreen.class, "targetOffsetY", DungeonMapScreen.offsetY);
            ReflectionHacks.setPrivate(__instance, DungeonMapScreen.class, "scrollWaitTimer", 0f);
            showLegend = false;
        }
    }

    @SpirePatch(clz = DungeonMap.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class RouteBackground {
        @SpirePrefixPatch
        public static SpireReturn<Void> prefix(DungeonMap __instance, SpriteBatch batch) {
            if (!PortraitViewport.active()) return SpireReturn.Continue();
            if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MAP) return SpireReturn.Return();
            float color = batch.getPackedColor();
            batch.setColor(0.79f, 0.71f, 0.55f, 1);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, 0, 0, Settings.WIDTH, Settings.HEIGHT);
            batch.setColor(color);
            return SpireReturn.Return();
        }
    }

    @SpirePatch(clz = DungeonMapScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class LegendDrawing {
        @SpirePostfixPatch
        public static void postfix(DungeonMapScreen __instance, SpriteBatch batch) {
            if (!PortraitViewport.active() || AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MAP) return;
            if (showLegend) {
                if (cachedMap != __instance.map) {
                    cachedMap = __instance.map;
                    nativeLegend = ReflectionHacks.getPrivate(cachedMap, DungeonMap.class, "legend");
                }
                placeLegend();

                legendBase.set(batch.getTransformMatrix());
                legendLayout.set(legendBase).translate(legendX, legendY, 0f).scale(legendScale, legendScale, 1f)
                        .translate(-Legend.X, -Legend.Y, 0f);
                batch.setTransformMatrix(legendLayout);
                try {
                    nativeLegend.render(batch);
                } finally {
                    batch.setTransformMatrix(legendBase);

                }
            }
            Hitbox button = getLegendButton();
            PortraitUi.drawRow(batch, FontHelper.buttonLabelFont, Settings.language == Settings.GameLanguage.ZHS
                    ? "图例" : "Legend", button, false);
        }
    }

    @SpirePatch(clz = DungeonMapScreen.class, method = "update", paramtypez = {})
    public static class LegendInput {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!"update".equals(call.getMethodName())) return;
                    boolean node = MapRoomNode.class.getName().equals(call.getClassName());
                    if (!node && !DungeonMap.class.getName().equals(call.getClassName())) return;
                    call.replace("{ int oldY = com.megacrit.cardcrawl.helpers.input.InputHelper.mY;"
                            + " if (!sts1portrait.PortraitMap.routePointerVisible()"
                            + (node ? " || sts1portrait.PortraitMap.legendBlocksRoute()" : "") + ")"
                            + " com.megacrit.cardcrawl.helpers.input.InputHelper.mY = -100000;"
                            + " try { $proceed($$); } finally { com.megacrit.cardcrawl.helpers.input.InputHelper.mY = oldY; } }");
                }
            };
        }

        @SpirePrefixPatch
        public static void prefix(DungeonMapScreen __instance) {
            if (!PortraitViewport.active() || AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MAP) return;
            if (layoutRevision != PortraitViewport.revision()) refreshScrollBounds(__instance);
            if (showLegend) placeLegend();
            Hitbox button = getLegendButton();
            button.update();
            if (button.hovered && InputHelper.justClickedLeft) {
                showLegend = !showLegend;
                InputHelper.justClickedLeft = false;
            }
        }
    }

    @SpirePatch(clz = DungeonMap.class, method = "update", paramtypez = {})
    public static class LegendHover {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (Legend.class.getName().equals(call.getClassName()) && "update".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitMap.updateLegend($0, $1, $2); }");
                    }
                }
            };
        }
    }

    public static boolean legendBlocksRoute() {
        if (!PortraitViewport.active() || !showLegend) return false;
        return Math.abs((InputHelper.mX) - legendX) < legendWidth * .5f
                && Math.abs((InputHelper.mY) - legendY) < legendHeight * .5f;
    }

    private static void placeLegend() {
        float nativeWidth = 512f * Settings.scale;
        float nativeHeight = 800f * Settings.yScale;
        legendScale = Math.min(1f, Math.min(PortraitViewport.contentWidth() * .48f / nativeWidth,
                (routeTop() - routeBottom()) * .72f / nativeHeight));
        legendWidth = nativeWidth * legendScale;
        legendHeight = nativeHeight * legendScale;
        legendX = PortraitViewport.x(.98f) - legendWidth * .5f;
        legendY = routeBottom() + legendHeight * .5f;

    }

    public static void updateLegend(Legend legend, float alpha, boolean onMap) {
        if (!PortraitViewport.active()) {
            legend.update(alpha, onMap);
        } else if (showLegend) {
            placeLegend();
            int x = InputHelper.mX, y = InputHelper.mY;
            InputHelper.mX = Math.round(Legend.X + ((x) - legendX) / legendScale);
            InputHelper.mY = Math.round(Legend.Y + ((y) - legendY) / legendScale);
            try {
                legend.update(alpha, onMap);
            } finally {
                InputHelper.mX = x;
                InputHelper.mY = y;
            }
        }
    }

    private static Hitbox getLegendButton() {
        if (legendButton == null) legendButton = new Hitbox(0, 0);
        float navigationScale = 1.30f * PortraitViewport.contentWidth() / Settings.WIDTH;
        legendButton.width = Math.min(PortraitViewport.referenceWidth() * .18f, PortraitViewport.contentWidth() * .18f);
        legendButton.height = PortraitViewport.referenceWidth() * .095f;
        legendButton.move(PortraitViewport.x(0.90f),
                PortraitScreens.cornerButtonY(navigationScale));
        return legendButton;
    }
}
