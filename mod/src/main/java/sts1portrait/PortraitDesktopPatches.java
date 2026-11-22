package sts1portrait;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitDesktopPatches {
    private static final String VIEWPORT = "sts1portrait.PortraitDesktopViewport";

    private PortraitDesktopPatches() {}

    @SpirePatch(cls = "com.megacrit.cardcrawl.desktop.DesktopLauncher", method = "loadSettings",
            paramtypes = {"com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration"})
    public static class DesktopWindow {
        @SpirePostfixPatch
        public static void configure(Object __0) throws ReflectiveOperationException {
            if (!PortraitDesktopViewport.desktopBackend()) return;
            Class<?> config = __0.getClass();
            PortraitDesktopMode.apply(__0, config);
            if (PortraitDesktopViewport.enable(config.getField("width").getInt(__0),
                    config.getField("height").getInt(__0), config.getField("fullscreen").getBoolean(__0))) {
                config.getField("resizable").setBoolean(__0, true);
            }
        }

        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                private boolean widthChecked;

                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!widthChecked && "com.megacrit.cardcrawl.core.DisplayConfig".equals(call.getClassName())
                            && "getWidth".equals(call.getMethodName())) {
                        // The first read validates the width; later reads size the actual window.
                        call.replace("{ $_ = $proceed($$); if (" + VIEWPORT
                                + ".desktopBackend() && $_ > 0 && $_ < $0.getHeight()) $_ = Math.max(800, $_); }");
                        widthChecked = true;
                    }
                }
            };
        }
    }

    @SpirePatch(cls = "com.megacrit.cardcrawl.helpers.input.InputHelper", method = "updateFirst", paramtypez = {})
    public static class ResizedPointer {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!"com.badlogic.gdx.Input".equals(call.getClassName())) return;
                    String axis = call.getMethodName();
                    if ("getX".equals(axis) || "getY".equals(axis)) {
                        call.replace("{ $_ = " + VIEWPORT + ".input" + axis.charAt(3) + "($proceed($$)); }");
                    }
                }
            };
        }
    }

    @SpirePatch(cls = "com.megacrit.cardcrawl.characters.AbstractPlayer", method = "clickAndDragCards",
            paramtypez = {})
    public static class DragCursor {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                private boolean initialTouchCheck;

                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (initialTouchCheck || !field.isReader() || !"isTouchScreen".equals(field.getFieldName())
                            || !"com.megacrit.cardcrawl.core.Settings".equals(field.getClassName())) return;
                    initialTouchCheck = true;
                    // The vanilla inspection moves both the card and the cursor; live layout handles long presses.
                    field.replace("{ $_ = $proceed() && !" + VIEWPORT + ".enabled(); }");
                }

                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if ("com.badlogic.gdx.Input".equals(call.getClassName())
                            && "setCursorPosition".equals(call.getMethodName())) {
                        call.replace("{ $proceed(" + VIEWPORT + ".windowX($1), " + VIEWPORT + ".windowY($2)); }");
                    }
                }
            };
        }
    }

    @SpirePatch(cls = "com.megacrit.cardcrawl.characters.AbstractPlayer", method = "updateSingleTargetInput",
            paramtypez = {})
    public static class TargetCursor {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!"com.badlogic.gdx.Input".equals(call.getClassName())) return;
                    switch (call.getMethodName()) {
                        case "getX":
                            call.replace("{ $_ = " + VIEWPORT + ".inputX($proceed($$)); }");
                            break;
                        case "getY":
                            call.replace("{ $_ = " + VIEWPORT + ".inputY($proceed($$)); }");
                            break;
                        case "setCursorPosition":
                            call.replace("{ $proceed(" + VIEWPORT + ".windowX($1), " + VIEWPORT + ".windowY($2)); }");
                            break;
                        default:
                            break;
                    }
                }
            };
        }
    }

    @SpirePatch(cls = "com.megacrit.cardcrawl.core.CardCrawlGame", method = "create", paramtypez = {})
    @SpirePatch(cls = "basemod.patches.com.megacrit.cardcrawl.core.CardCrawlGame.ApplyScreenPostProcessor",
            method = "initFrameBuffer", paramtypez = {})
    public static class HighResolutionCanvas {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!"com.badlogic.gdx.Graphics".equals(call.getClassName())) return;
                    String method = call.getMethodName();
                    if ("getWidth".equals(method) || "getHeight".equals(method)) {
                        call.replace("{ $_ = " + VIEWPORT + ".render" + method.substring(3) + "($proceed($$)); }");
                    }
                }
            };
        }
    }

    @SpirePatch(cls = "basemod.patches.com.megacrit.cardcrawl.core.CardCrawlGame.ApplyScreenPostProcessor",
            method = "BeforeSpriteBatchEnd", paramtypes = {
                    "com.badlogic.gdx.graphics.g2d.SpriteBatch", "com.badlogic.gdx.graphics.OrthographicCamera"
            })
    public static class RestoreSceneViewport {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if ("com.badlogic.gdx.utils.viewport.FitViewport".equals(call.getClassName())
                            && "apply".equals(call.getMethodName())) {
                        call.replace("{ if (!" + VIEWPORT + ".enabled()) $proceed($$); }");
                    } else if ("com.badlogic.gdx.graphics.g2d.SpriteBatch".equals(call.getClassName())
                            && "setProjectionMatrix".equals(call.getMethodName())) {
                        call.replace("{ if (" + VIEWPORT + ".enabled()) " + VIEWPORT + ".applySceneViewport(); $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch2(cls = "com.megacrit.cardcrawl.core.CardCrawlGame", method = "render")
    public static class LiveProjection {
        @SpirePrefixPatch
        public static void prefix(OrthographicCamera ___camera) {
            PortraitDesktopViewport.beginFrame(___camera);
        }
    }

    @SpirePatch(cls = "com.megacrit.cardcrawl.core.CardCrawlGame", method = "update", paramtypez = {})
    public static class MarginInput {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if ("com.megacrit.cardcrawl.helpers.input.InputHelper".equals(call.getClassName())
                            && "updateFirst".equals(call.getMethodName())) {
                        call.replace("{ $proceed($$); " + VIEWPORT + ".blockMargins(); }");
                    }
                }
            };
        }
    }

}
