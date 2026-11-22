package sts1portrait;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.core.DisplayConfig;
import com.megacrit.cardcrawl.core.Settings;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitViewport {
    private static int width;
    private static int height;
    private static int revision;
    private static float referenceWidth;
    private static float referenceHeight;
    private static float referenceLeft;
    private static int referenceCanvasWidth;
    private static int referenceCanvasHeight;
    private static float left;
    private static float top;
    private static float right;
    private static float bottom;

    private PortraitViewport() {
    }

    public static void readSurface() {
        resizeSurface(PortraitDesktopViewport.renderWidth(Gdx.graphics.getWidth()),
                PortraitDesktopViewport.renderHeight(Gdx.graphics.getHeight()));
        referenceWidth = contentWidth();
        referenceHeight = contentHeight();
        referenceLeft = left;
        referenceCanvasWidth = width;
        referenceCanvasHeight = height;
    }

    public static void resizeSurface(int newWidth, int newHeight) {
        if (width == newWidth && height == newHeight) return;
        width = newWidth;
        height = newHeight;
        revision++;
        left = top = right = bottom = 0;
        String value = System.getProperty("sts1portrait.safe_area", "");
        String[] edges = value.split(",");
        if (active() && edges.length == 4) {
            left = edge(edges[0]) * width;
            top = edge(edges[1]) * height;
            right = edge(edges[2]) * width;
            bottom = edge(edges[3]) * height;
        }
    }

    private static float edge(String value) {
        try {
            float fraction = Float.parseFloat(value);
            return Float.isFinite(fraction) ? Math.max(0, Math.min(0.2f, fraction)) : 0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static float contentWidth() {
        return width - left - right;
    }

    static int revision() {
        return revision;
    }

    static float referenceWidth() {
        return referenceWidth > 0f ? referenceWidth : contentWidth();
    }

    static float referenceHeight() {
        return referenceHeight > 0f ? referenceHeight : contentHeight();
    }

    static float referenceX(float fraction) {
        return referenceLeft + referenceWidth() * fraction;
    }

    public static int referenceCanvasWidth() {
        return referenceCanvasWidth > 0 ? referenceCanvasWidth : Settings.WIDTH;
    }

    public static int referenceCanvasHeight() {
        return referenceCanvasHeight > 0 ? referenceCanvasHeight : Settings.HEIGHT;
    }

    public static float contentHeight() {
        return height - top - bottom;
    }

    public static float x(float fraction) {
        return left + contentWidth() * fraction;
    }

    public static float y(float fraction) {
        return bottom + contentHeight() * fraction;
    }

    static float topInset() {
        return top;
    }

    public static boolean active() {
        return width > 0 && width < height;
    }

    public static int widthOr(int original) {
        return active() || PortraitDesktopMode.legacyLandscape() ? width : original;
    }

    public static int heightOr(int original) {
        return active() || PortraitDesktopMode.legacyLandscape() ? height : original;
    }

    static void logDimensions() {
        System.out.println("[STS1 Portrait] surface=" + width + "x" + height
                + " game=" + Settings.WIDTH + "x" + Settings.HEIGHT
                + " framebuffer=" + Gdx.graphics.getBackBufferWidth() + "x"
                + Gdx.graphics.getBackBufferHeight() + " portrait=" + active()
                + " safe=" + Math.round(left) + "," + Math.round(top) + "," + Math.round(right) + "," + Math.round(bottom));
    }

    @SpirePatch(clz = Settings.class, method = "initializeDisplay", paramtypez = {})
    public static class DisplayPatch {
        @SpirePrefixPatch
        public static void prefix() {
            readSurface();
        }

        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!DisplayConfig.class.getName().equals(call.getClassName())) {
                        return;
                    }
                    if ("getWidth".equals(call.getMethodName())) {
                        call.replace("{ $_ = sts1portrait.PortraitViewport.widthOr($proceed($$)); }");
                    } else if ("getHeight".equals(call.getMethodName())) {
                        call.replace("{ $_ = sts1portrait.PortraitViewport.heightOr($proceed($$)); }");
                    }
                }

                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (!field.isWriter() || !Settings.class.getName().equals(field.getClassName())) return;
                    if ("isLetterbox".equals(field.getFieldName())) {
                        field.replace("{ $proceed($1 && !sts1portrait.PortraitViewport.active()); }");
                    } else if ("MAP_DST_Y".equals(field.getFieldName())) {
                        field.replace("{ $proceed(sts1portrait.PortraitViewport.active()"
                                + " ? sts1portrait.PortraitMap.floorSpacing() : $1); }");
                    }
                }
            };
        }
    }
}
