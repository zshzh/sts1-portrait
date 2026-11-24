package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireRawPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.DiscardPileViewScreen;
import com.megacrit.cardcrawl.screens.DrawPileViewScreen;
import com.megacrit.cardcrawl.screens.ExhaustPileViewScreen;
import com.megacrit.cardcrawl.screens.MasterDeckSortHeader;
import com.megacrit.cardcrawl.screens.MasterDeckViewScreen;
import com.megacrit.cardcrawl.screens.compendium.CardLibraryScreen;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.Opcode;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitCardViews {
    private PortraitCardViews() {}

    public static int columns() {
        return PortraitScreens.enabled() ? 3 : 5;
    }

    public static float geometry(String name, float original) {
        if (!PortraitScreens.enabled()) return original;
        switch (name) {
            case "drawStartX": return PortraitViewport.x(0.20f);
            case "padX": return PortraitViewport.contentWidth() * 0.30f;
            case "padY": return AbstractCard.IMG_HEIGHT * gridScale() + layoutWidth() * 0.036f;
            default: return original;
        }
    }

    private static float layoutWidth() {
        return Math.min(PortraitViewport.referenceWidth(), PortraitViewport.contentWidth());
    }

    public static float selectionTop(GridCardSelectScreen screen, float original) {
        if (!PortraitScreens.enabled() || screen.targetGroup == null) return original;
        return PortraitViewport.y(screen.targetGroup.size() <= columns() ? .53f : .75f);
    }

    private static float gridScale() {
        return Math.min(1.10f, layoutWidth() * 0.24f / AbstractCard.IMG_WIDTH);
    }

    public static void sizeCard(AbstractCard card) {
        if (PortraitScreens.enabled() && Math.abs(card.targetDrawScale - 0.75f) < 0.001f) {
            card.targetDrawScale = gridScale();
        }
    }

    public static float libraryTop(float original) {
        return PortraitScreens.enabled() ? PortraitViewport.y(.745f) + Settings.DEFAULT_SCROLL_LIMIT : original;
    }

    @SpirePatch(clz = CardLibraryScreen.class, method = SpirePatch.CLASS)
    public static class LibraryViewport {
        public static final SpireField<Integer> applied = new SpireField<>(() -> -1);
    }

    @SpirePatch(clz = CardLibraryScreen.class, method = "calculateScrollBounds", paramtypez = {})
    public static class LibraryRange {
        @SpirePostfixPatch
        public static void after(CardLibraryScreen __instance, CardGroup ___visibleCards) {
            if (!PortraitScreens.enabled() || ___visibleCards == null) return;
            int lastRow = Math.max(0, (___visibleCards.size() - 1) / columns());
            float halfCard = AbstractCard.IMG_HEIGHT * gridScale() * .5f;
            float upper = PortraitMenuPages.footerTop() + halfCard - libraryTop(0f) + lastRow * geometry("padY", 0f);
            ReflectionHacks.setPrivate(__instance, CardLibraryScreen.class, "scrollUpperBound",
                    Math.max(Settings.DEFAULT_SCROLL_LIMIT, upper));
            LibraryViewport.applied.set(__instance, PortraitViewport.revision());
        }
    }

    @SpirePatch(clz = CardLibraryScreen.class, method = "update", paramtypez = {})
    public static class LibraryResize {
        @SpirePrefixPatch
        public static void before(CardLibraryScreen __instance) {
            if (PortraitScreens.enabled() && LibraryViewport.applied.get(__instance) != PortraitViewport.revision()) {
                ReflectionHacks.privateMethod(CardLibraryScreen.class, "calculateScrollBounds").invoke(__instance);
            }
        }
    }

    @SpirePatch(clz = CardLibraryScreen.class, method = "updateCards", paramtypez = {})
    public static class LibraryFooterInput {
        private static int pointerY;
        private static boolean blocked;

        @SpirePrefixPatch
        public static void before() {
            pointerY = InputHelper.mY;
            blocked = PortraitScreens.enabled() && pointerY < PortraitMenuPages.footerTop();
            if (blocked) InputHelper.mY = -100000;
        }

        @SpirePostfixPatch
        public static void after() {
            if (blocked) InputHelper.mY = pointerY;
        }
    }

    @SpirePatch(clz = CardLibraryScreen.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class LibraryFooterDraw {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (CardLibraryScreen.class.getName().equals(call.getClassName())
                            && "renderGroup".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitMenuPages.beginMenuContent($1); $proceed($$); }");
                    } else if (MenuCancelButton.class.getName().equals(call.getClassName())
                            && "render".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitMenuPages.endMenuContent($1); $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = GridCardSelectScreen.class, method = SpirePatch.CLASS)
    public static class GridViewport {
        public static final SpireField<Integer> applied = new SpireField<>(() -> -1);
    }

    @SpirePatch(clz = GridCardSelectScreen.class, method = "calculateScrollBounds", paramtypez = {})
    public static class SelectionRange {
        @SpirePostfixPatch
        public static void after(GridCardSelectScreen __instance) {
            if (!PortraitScreens.enabled() || __instance.targetGroup == null) return;
            int rows = (__instance.targetGroup.size() + columns() - 1) / columns();
            float bottom = selectionTop(__instance, 0f) - Math.max(0, rows - 1) * geometry("padY", 0f)
                    - AbstractCard.IMG_HEIGHT * gridScale() * .5f;
            float range = Math.max(Settings.DEFAULT_SCROLL_LIMIT, PortraitViewport.y(.19f) - bottom);
            ReflectionHacks.setPrivate(__instance, GridCardSelectScreen.class, "scrollUpperBound", range);
            GridViewport.applied.set(__instance, PortraitViewport.revision());
        }
    }

    @SpirePatch(clz = GridCardSelectScreen.class, method = "update", paramtypez = {})
    public static class SelectionResize {
        @SpirePrefixPatch
        public static void before(GridCardSelectScreen __instance) {
            if (PortraitScreens.enabled() && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID
                    && GridViewport.applied.get(__instance) != PortraitViewport.revision()) {
                ReflectionHacks.privateMethod(GridCardSelectScreen.class, "calculateScrollBounds").invoke(__instance);
                if (__instance.forPurge && __instance.confirmScreenUp) {
                    AbstractCard card = ReflectionHacks.getPrivate(__instance, GridCardSelectScreen.class, "hoveredCard");
                    if (card != null) {
                        card.current_x = card.target_x = PortraitViewport.x(.5f);
                        card.current_y = card.target_y = PortraitViewport.y(.5f);
                    }
                }
            }
        }
    }

    @SpirePatch(clz = MasterDeckViewScreen.class, method = "updatePositions", paramtypez = {})
    @SpirePatch(clz = DrawPileViewScreen.class, method = "updatePositions", paramtypez = {})
    @SpirePatch(clz = DiscardPileViewScreen.class, method = "updatePositions", paramtypez = {})
    @SpirePatch(clz = ExhaustPileViewScreen.class, method = "updatePositions", paramtypez = {})
    @SpirePatch(clz = GridCardSelectScreen.class, method = "updateCardPositionsAndHoverLogic", paramtypez = {})
    @SpirePatch(clz = MasterDeckViewScreen.class, method = "hideCards", paramtypez = {})
    @SpirePatch(clz = DrawPileViewScreen.class, method = "hideCards", paramtypez = {})
    @SpirePatch(clz = DiscardPileViewScreen.class, method = "hideCards", paramtypez = {})
    @SpirePatch(clz = ExhaustPileViewScreen.class, method = "hideCards", paramtypez = {})
    @SpirePatch(clz = GridCardSelectScreen.class, method = "hideCards", paramtypez = {})
    @SpirePatch(clz = MasterDeckViewScreen.class, method = "calculateScrollBounds", paramtypez = {})
    @SpirePatch(clz = DrawPileViewScreen.class, method = "calculateScrollBounds", paramtypez = {})
    @SpirePatch(clz = DiscardPileViewScreen.class, method = "calculateScrollBounds", paramtypez = {})
    @SpirePatch(clz = ExhaustPileViewScreen.class, method = "calculateScrollBounds", paramtypez = {})
    @SpirePatch(clz = GridCardSelectScreen.class, method = "calculateScrollBounds", paramtypez = {})
    @SpirePatch(clz = CardLibraryScreen.class, method = "updateCards", paramtypez = {})
    @SpirePatch(clz = CardLibraryScreen.class, method = "calculateScrollBounds", paramtypez = {})
    public static class GridGeometry {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    String name = field.getFieldName();
                    if (field.isReader() && "drawStartY".equals(name)
                            && GridCardSelectScreen.class.getName().equals(field.getClassName())) {
                        field.replace("{ $_ = sts1portrait.PortraitCardViews.selectionTop(this, $proceed()); }");
                    } else if (field.isReader() && "drawStartY".equals(name)
                            && CardLibraryScreen.class.getName().equals(field.getClassName())) {
                        field.replace("{ $_ = sts1portrait.PortraitCardViews.libraryTop($proceed()); }");
                    } else if (field.isReader() && ("drawStartX".equals(name) || "padX".equals(name) || "padY".equals(name))) {
                        field.replace("{ $_ = sts1portrait.PortraitCardViews.geometry(\"" + name + "\", $proceed()); }");
                    } else if (field.isReader() && "CARDS_PER_LINE".equals(name)) {
                        field.replace("{ $_ = sts1portrait.PortraitScreens.enabled()"
                                + " ? sts1portrait.PortraitCardViews.columns() : $proceed(); }");
                    }
                }

                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (AbstractCard.class.getName().equals(call.getClassName()) && "update".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitCardViews.sizeCard($0); $proceed($$); }");
                    } else if ((MasterDeckSortHeader.class.getName().equals(call.getClassName())
                            || "com.megacrit.cardcrawl.screens.compendium.CardLibSortHeader".equals(call.getClassName()))
                            && "updateScrollPosition".equals(call.getMethodName())) {
                        call.replace("{ $proceed(sts1portrait.PortraitScreens.enabled() ? $1"
                                + " + sts1portrait.PortraitViewport.contentWidth() * 0.058f : $1); }");
                    }
                }
            };
        }

        @SpireRawPatch
        public static void replaceGridArithmetic(CtBehavior behavior) throws Exception {
            // These five vanilla grids store the column count directly before the integer divide or remainder.
            CodeAttribute code = behavior.getMethodInfo().getCodeAttribute();
            CodeIterator iterator = code.iterator();
            ArrayList<Integer> constants = new ArrayList<>();
            while (iterator.hasNext()) {
                int index = iterator.next();
                if (iterator.byteAt(index) != Opcode.ICONST_5) continue;
                int next = iterator.byteAt(index + 1);
                if (next == Opcode.IREM || next == Opcode.IDIV) constants.add(index);
            }
            Bytecode columns = new Bytecode(behavior.getMethodInfo().getConstPool());
            columns.addInvokestatic(PortraitCardViews.class.getName(), "columns", "()I");
            for (int i = constants.size() - 1; i >= 0; i--) {
                int index = constants.get(i);
                iterator.writeByte(Opcode.NOP, index);
                iterator.insertAt(index, columns.get());
            }
            code.setMaxStack(code.computeMaxStack());
            behavior.getMethodInfo().rebuildStackMap(behavior.getDeclaringClass().getClassPool());
        }
    }
}
