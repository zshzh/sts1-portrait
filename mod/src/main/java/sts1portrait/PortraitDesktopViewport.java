package sts1portrait;

import basemod.ReflectionHacks;
import basemod.patches.com.megacrit.cardcrawl.core.CardCrawlGame.ApplyScreenPostProcessor;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.Rectangle;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

/** Keeps the initial logical-pixels-per-window-pixel ratio while the visible area changes. */
public final class PortraitDesktopViewport {
    private static final String PRIMARY_BUFFER = "primaryFrameBuffer";
    private static final String SECONDARY_BUFFER = "secondaryFrameBuffer";
    private static float density = 1f;
    private static boolean enabled;
    private static int windowWidth, windowHeight, width, height, left, top, bottom;

    private PortraitDesktopViewport() {}

    public static boolean desktopBackend() {
        return System.getProperty("amethyst.bridge.mode") == null
                && System.getProperty("sts1portrait.safe_area") == null
                && !Boolean.getBoolean("sts1portrait.desktop.fixed");
    }

    static boolean enable(int width, int height, boolean fullscreen) {
        enabled = desktopBackend() && width > 0 && height > width && !fullscreen;
        if (enabled) {
            String value = System.getProperty("sts1portrait.desktop.density", "1");
            try {
                float requested = Float.parseFloat(value);
                if (Float.isFinite(requested)) density = Math.max(1f, Math.min(4f, requested));
            } catch (NumberFormatException ignored) {
                density = 1f;
            }
            System.out.println("[STS1 Portrait Live] enabled at " + width + "x" + height + ", density=" + density);
        }
        return enabled;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static int renderWidth(int fallback) {
        return enabled ? Math.round(Math.min(fallback, Gdx.graphics.getHeight() * 0.72f) * density) : fallback;
    }

    public static int renderHeight(int fallback) {
        return enabled ? Math.round(fallback * density) : fallback;
    }

    public static void beginFrame(OrthographicCamera camera) {
        if (!enabled || camera == null) return;
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        if (w <= 0 || h <= 0 || (windowWidth == w && windowHeight == h)) return;
        windowWidth = w;
        windowHeight = h;
        int canvasWidth = renderWidth(w);
        int canvasHeight = renderHeight(h);
        width = Math.max(1, Math.round(canvasWidth / density));
        height = h;
        left = (w - width) / 2;
        bottom = (h - height) / 2;
        top = h - height - bottom;

        if (canvasWidth != Settings.WIDTH || canvasHeight != Settings.HEIGHT) {
            Settings.WIDTH = canvasWidth;
            Settings.HEIGHT = canvasHeight;
            PortraitViewport.resizeSurface(canvasWidth, canvasHeight);
            camera.setToOrtho(false, canvasWidth, canvasHeight);
            if (CardCrawlGame.viewport != null) {
                CardCrawlGame.viewport.setWorldSize(canvasWidth, canvasHeight);
                CardCrawlGame.viewport.setScreenBounds(left, bottom, width, height);
            }
            releaseBuffer(PRIMARY_BUFFER);
            releaseBuffer(SECONDARY_BUFFER);
            System.out.println("[STS1 Portrait Live] window=" + w + "x" + h
                    + " canvas=" + canvasWidth + "x" + canvasHeight);
        }
    }

    private static void releaseBuffer(String name) {
        FrameBuffer buffer = ReflectionHacks.getPrivateStatic(ApplyScreenPostProcessor.class, name);
        if (buffer == null) return;
        buffer.dispose();
        ReflectionHacks.setPrivateStatic(ApplyScreenPostProcessor.class, name, null);
    }

    public static void applySceneViewport() {
        HdpiUtils.glViewport(left, bottom, width, height);
    }

    public static int inputX(int x) {
        return width > 0 ? Math.round((x - left) * ((float) Settings.WIDTH / width)) : x;
    }

    public static int inputY(int y) {
        return height > 0 ? Math.round((y - top) * ((float) Settings.HEIGHT / height)) : y;
    }

    public static int windowX(int x) {
        return width > 0 ? left + Math.round(x * (float) width / Settings.WIDTH) : x;
    }

    public static int windowY(int y) {
        return height > 0 ? top + Math.round(y * (float) height / Settings.HEIGHT) : y;
    }

    public static void blockMargins() {
        if (width == 0 || width == windowWidth) return;
        int x = Gdx.input.getX();
        int y = Gdx.input.getY();
        if (x >= left && x < left + width && y >= top && y < top + height) return;
        InputHelper.mX = -10000;
        InputHelper.mY = -10000;
        InputHelper.justClickedLeft = false;
        InputHelper.justClickedRight = false;
    }

    static void clipProjected(Rectangle area) {
        if (!enabled || windowWidth == 0 || windowHeight == 0) return;
        float sx = Settings.WIDTH / (float) windowWidth;
        float sy = Settings.HEIGHT / (float) windowHeight;
        area.set(area.x * sx, area.y * sy, area.width * sx, area.height * sy);
        clip(area);
    }

    public static void clip(Rectangle area) {
        if (!enabled || width == 0) return;
        // ScissorStack expects window units. BaseMod's framebuffer has the logical canvas size.
        float sx = windowWidth / (float) Gdx.graphics.getBackBufferWidth();
        float sy = windowHeight / (float) Gdx.graphics.getBackBufferHeight();
        area.set(area.x * sx, area.y * sy, area.width * sx, area.height * sy);
    }
}
