package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.scenes.AbstractScene;
import com.megacrit.cardcrawl.scenes.TitleBackground;
import com.megacrit.cardcrawl.scenes.TitleCloud;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuButton;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import com.megacrit.cardcrawl.ui.panels.TopPanel;
import java.util.ArrayList;

public final class PortraitUi {
    private static final Matrix4 previousTransform = new Matrix4();
    private static final Matrix4 transform = new Matrix4();
    private static final GlyphLayout measure = new GlyphLayout();
    private static final float[] quadVertices = new float[20];
    private static final float[] octagonVertices = new float[60];

    private static final Color SHADOW_COLOR = new Color(0f, 0f, 0f, 0.68f);
    private static final Color HERO_TEXT = new Color(1f, 0.965f, 0.89f, 1f);
    private static final Color ABANDON_TEXT = new Color(0.98f, 0.80f, 0.76f, 1f);
    private static final Color QUIT_TEXT = new Color(0.84f, 0.86f, 0.88f, 0.94f);
    private static final Color PROFILE_HINT = new Color(0.72f, 0.80f, 0.86f, 0.84f);
    private static final Color VERSION_TEXT = new Color(0.86f, 0.90f, 0.94f, 0.50f);

    private static TitleBackground cachedTitle;
    private static AtlasRegion titleSky;
    private static AtlasRegion titleSpireBot;
    private static AtlasRegion titleSpireTop;
    private static AtlasRegion titleBotGlow;
    private static AtlasRegion titleTopGlow;
    private static AtlasRegion titleTopGlow2;
    private static ArrayList<TitleCloud> titleTopClouds;
    private static ArrayList<TitleCloud> titleMidClouds;
    private static Texture titleLogo;

    private PortraitUi() {
    }

    public static void renderRoomBackground(AbstractScene scene, SpriteBatch batch, boolean event) {
        if (!PortraitViewport.active()) {
            if (event) scene.renderEventRoom(batch);
            else scene.renderCombatRoomBg(batch);
            return;
        }
        float scale = Math.max(1f, Settings.HEIGHT / (1080f * Settings.scale));
        beginTransform(batch, (Settings.WIDTH - Settings.WIDTH * scale) * 0.5f,
                0f, scale, scale);
        try {
            if (event) scene.renderEventRoom(batch);
            else scene.renderCombatRoomBg(batch);
        } finally {
            batch.setTransformMatrix(previousTransform);
        }
    }

    public static void renderTopPanel(TopPanel panel, SpriteBatch batch) {
        if (PortraitCombatHud.topBarActive()) {
            PortraitCombatHud.render(panel, batch);
            return;
        }
        if (!PortraitViewport.active() || (PortraitViewport.x(0) == 0
                && PortraitViewport.x(1) == Settings.WIDTH && PortraitViewport.topInset() == 0)) {
            panel.render(batch);
            return;
        }
        beginTransform(batch, PortraitViewport.x(0), -PortraitViewport.topInset(),
                PortraitViewport.contentWidth() / Settings.WIDTH, 1);
        try {
            panel.render(batch);
        } finally {
            batch.setTransformMatrix(previousTransform);
        }
    }

    public static void updateTopPanel(TopPanel panel) {
        PortraitCombatHud.prepareInput(panel);
        if (PortraitCombatHud.topBarActive()) {
            PortraitCombatHud.updateRelics();
            panel.update();
            return;
        }
        if (!PortraitViewport.active()) {
            panel.update();
            return;
        }
        int mouseX = InputHelper.mX;
        int mouseY = InputHelper.mY;
        InputHelper.mX = Math.round((mouseX - PortraitViewport.x(0)) * Settings.WIDTH / PortraitViewport.contentWidth());
        InputHelper.mY = Math.round(mouseY + PortraitViewport.topInset());
        try {
            panel.update();
        } finally {
            InputHelper.mX = mouseX;
            InputHelper.mY = mouseY;
        }
    }

    private static void beginTransform(SpriteBatch batch, float x, float y, float sx, float sy) {
        previousTransform.set(batch.getTransformMatrix());
        transform.set(previousTransform).translate(x, y, 0).scale(sx, sy, 1);
        batch.setTransformMatrix(transform);
    }

    private static void drawVerticalGradient(SpriteBatch batch, float y0, float y1, float bottomBits, float topBits) {
        float w = Settings.WIDTH;
        setQuad(0, 0f, y0, bottomBits);
        setQuad(1, 0f, y1, topBits);
        setQuad(2, w, y1, topBits);
        setQuad(3, w, y0, bottomBits);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, quadVertices, 0, quadVertices.length);
    }

    private static void setQuad(int index, float x, float y, float colorBits) {
        int o = index * 5;
        quadVertices[o] = x;
        quadVertices[o + 1] = y;
        quadVertices[o + 2] = colorBits;
        quadVertices[o + 3] = 0.5f;
        quadVertices[o + 4] = 0.5f;
    }

    static void drawCutCorner(SpriteBatch batch, float left, float bottom, float right, float top, float cut) {
        float color = batch.getPackedColor();
        setOct(0, left + cut, bottom, color);
        setOct(1, left, bottom + cut, color);
        setOct(2, right, bottom + cut, color);
        setOct(3, right - cut, bottom, color);
        setOct(4, left, bottom + cut, color);
        setOct(5, left, top - cut, color);
        setOct(6, right, top - cut, color);
        setOct(7, right, bottom + cut, color);
        setOct(8, left, top - cut, color);
        setOct(9, left + cut, top, color);
        setOct(10, right - cut, top, color);
        setOct(11, right, top - cut, color);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, octagonVertices, 0, octagonVertices.length);
    }

    private static void setOct(int index, float x, float y, float colorBits) {
        int o = index * 5;
        octagonVertices[o] = x;
        octagonVertices[o + 1] = y;
        octagonVertices[o + 2] = colorBits;
        octagonVertices[o + 3] = 0.5f;
        octagonVertices[o + 4] = 0.5f;
    }

    private static void drawSpireLayer(SpriteBatch batch, AtlasRegion region, float originX, float originY, float scale) {
        if (region == null) return;
        batch.draw(region.getTexture(),
                originX + region.offsetX * scale,
                originY + region.offsetY * scale,
                0f, 0f,
                region.packedWidth, region.packedHeight,
                scale, scale, 0f,
                region.getRegionX(), region.getRegionY(),
                region.getRegionWidth(), region.getRegionHeight(),
                false, false);
    }

    private static void drawTitleClouds(SpriteBatch batch, ArrayList<TitleCloud> clouds,
            float originX, float baseY, float spireScale, float alpha) {
        if (clouds == null || clouds.isEmpty()) return;
        float ratio = Settings.scale > 0.01f ? (spireScale / Settings.scale) : 1f;
        batch.setColor(1f, 1f, 1f, alpha);
        for (int i = 0; i < clouds.size(); i++) {
            TitleCloud cloud = clouds.get(i);
            if (cloud == null) continue;
            AtlasRegion region = ReflectionHacks.getPrivate(cloud, TitleCloud.class, "region");
            if (region == null) continue;
            float cx = ReflectionHacks.<Float>getPrivate(cloud, TitleCloud.class, "x") * ratio;
            float jiggle = ReflectionHacks.<Float>getPrivate(cloud, TitleCloud.class, "sliderJiggle");
            drawSpireLayer(batch, region, originX + cx, baseY + jiggle * 4f * spireScale, spireScale);
        }
    }

    static void renderTitle(TitleBackground title, SpriteBatch batch) {
        if (cachedTitle != title) {
            cachedTitle = title;
            titleSky = ReflectionHacks.getPrivate(title, TitleBackground.class, "sky");
            titleSpireBot = ReflectionHacks.getPrivate(title, TitleBackground.class, "mg3Bot");
            titleSpireTop = ReflectionHacks.getPrivate(title, TitleBackground.class, "mg3Top");
            titleBotGlow = ReflectionHacks.getPrivate(title, TitleBackground.class, "botGlow");
            titleTopGlow = ReflectionHacks.getPrivate(title, TitleBackground.class, "topGlow");
            titleTopGlow2 = ReflectionHacks.getPrivate(title, TitleBackground.class, "topGlow2");
            titleTopClouds = ReflectionHacks.getPrivate(title, TitleBackground.class, "topClouds");
            titleMidClouds = ReflectionHacks.getPrivate(title, TitleBackground.class, "midClouds");
            titleLogo = ReflectionHacks.getPrivateStatic(TitleBackground.class, "titleLogoImg");
        }
        float color = batch.getPackedColor();
        float W = Settings.WIDTH;
        float H = Settings.HEIGHT;
        boolean main = CardCrawlGame.mainMenuScreen == null
                || CardCrawlGame.mainMenuScreen.screen == MainMenuScreen.CurScreen.MAIN_MENU;

        // 1. Deep twilight canvas base.
        batch.setColor(0.045f, 0.055f, 0.085f, 1f);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, 0f, 0f, W, H);

        // 2. Painterly turquoise twilight sky aligned behind the upper Spire & mountain silhouette.
        batch.setColor(main ? 0.90f : 0.78f, main ? 0.94f : 0.84f, main ? 0.97f : 0.88f, 1f);
        drawCover(batch, titleSky, H * 0.26f, H * 0.74f);

        // 3. Full vertical Spire composition (mg3Bot + mg3Top = 1920x2219) framed on the left fourth,
        //    showcasing the towering Spire, fiery storm crown, jagged snow peaks, and pilgrim road.
        float spireScale = Math.max(W / 1920f, H / 2219f);
        float visibleSrcW = W / spireScale;
        float srcLeft = Math.max(0f, Math.min(1920f - visibleSrcW, 305f - visibleSrcW * 0.235f));
        float originX = -srcLeft * spireScale;
        float slideY = -45f * spireScale * title.slider;
        float botY = H - 2219f * spireScale + slideY;
        float topY = botY + 1138.5f * spireScale;

        batch.setColor(Color.WHITE);
        drawSpireLayer(batch, titleSpireBot, originX, botY, spireScale);
        drawSpireLayer(batch, titleSpireTop, originX, topY, spireScale);

        // 4. Additive pulsing ember runes on the Spire shaft (exact float alignment, zero seam).
        batch.setBlendFunction(770, 1);
        float pulse = 0.14f + (MathUtils.cosDeg((float) (System.currentTimeMillis() / 16L % 360L)) + 1.25f) / 4.4f;
        batch.setColor(1f, 0.26f, 0.10f, pulse);
        drawSpireLayer(batch, titleBotGlow, originX, botY, spireScale);
        drawSpireLayer(batch, titleTopGlow, originX, topY, spireScale);
        drawSpireLayer(batch, titleTopGlow2, originX, topY, spireScale);
        batch.setBlendFunction(770, 771);

        // 5. Softly drifting twilight clouds across the mid/upper sky.
        float cloudBaseY = H - 1220f * spireScale + slideY;
        drawTitleClouds(batch, titleMidClouds, originX, cloudBaseY, spireScale, main ? 0.28f : 0.20f);
        drawTitleClouds(batch, titleTopClouds, originX, cloudBaseY, spireScale, main ? 0.34f : 0.24f);

        // 6. Silky GPU gradients anchoring menu buttons and top safe area while keeping the Spire luminous.
        if (main) {
            float deep = Color.toFloatBits(0.025f, 0.038f, 0.058f, 0.91f);
            float mid = Color.toFloatBits(0.025f, 0.038f, 0.058f, 0.56f);
            float clear = Color.toFloatBits(0.025f, 0.038f, 0.058f, 0f);
            drawVerticalGradient(batch, 0f, H * 0.35f, deep, mid);
            drawVerticalGradient(batch, H * 0.35f, H * 0.56f, mid, clear);
            float topShade = Color.toFloatBits(0.02f, 0.03f, 0.05f, 0.52f);
            float topClear = Color.toFloatBits(0.02f, 0.03f, 0.05f, 0f);
            drawVerticalGradient(batch, H * 0.87f, H, topClear, topShade);
        } else {
            float subDeep = Color.toFloatBits(0.025f, 0.038f, 0.058f, 0.74f);
            float subMid = Color.toFloatBits(0.025f, 0.038f, 0.058f, 0.36f);
            float subTop = Color.toFloatBits(0.025f, 0.038f, 0.058f, 0.44f);
            drawVerticalGradient(batch, 0f, H * 0.46f, subDeep, subMid);
            drawVerticalGradient(batch, H * 0.46f, H, subMid, subTop);
        }

        // 7. Prominent, sharp Slay the Spire logo with subtle grounding drop shadow.
        if (titleLogo != null && main) {
            float logoAlpha = ReflectionHacks.<Float>getPrivate(title, TitleBackground.class, "logoAlpha");
            if (logoAlpha <= 0.01f && title.slider <= 0.05f
                    && (CardCrawlGame.mainMenuScreen == null || !CardCrawlGame.mainMenuScreen.darken)) {
                logoAlpha = 1f;
            }
            if (logoAlpha > 0.01f) {
                float aspect = (float) titleLogo.getWidth() / Math.max(1f, titleLogo.getHeight());
                float logoWidth = Math.min(PortraitViewport.contentWidth() * 0.76f,
                        PortraitViewport.contentHeight() * 0.268f * aspect);
                float logoHeight = logoWidth / aspect;
                float cx = PortraitViewport.x(0.515f);
                float cy = PortraitViewport.y(0.752f) + slideY * 0.6f;
                float shadowDrop = Math.max(5f, logoHeight * 0.014f);
                batch.setColor(0f, 0f, 0f, 0.44f * logoAlpha);
                batch.draw(titleLogo, cx - logoWidth * 0.5f, cy - logoHeight * 0.5f - shadowDrop,
                        logoWidth, logoHeight);
                batch.setColor(1f, 1f, 1f, logoAlpha);
                batch.draw(titleLogo, cx - logoWidth * 0.5f, cy - logoHeight * 0.5f,
                        logoWidth, logoHeight);
            }
        }
        batch.setColor(color);
    }

    private static void drawCover(SpriteBatch batch, AtlasRegion region, float bottom, float height) {
        if (region == null || region.originalWidth <= 0 || region.originalHeight <= 0) return;
        float scale = Math.max(Settings.WIDTH / (float) region.originalWidth, height / region.originalHeight);
        float x = (Settings.WIDTH - region.originalWidth * scale) * 0.5f;
        batch.draw(region.getTexture(), x + region.offsetX * scale, bottom + region.offsetY * scale,
                0, 0, region.packedWidth, region.packedHeight, scale, scale, 0,
                region.getRegionX(), region.getRegionY(), region.getRegionWidth(), region.getRegionHeight(), false, false);
    }

    private static void drawFittedText(SpriteBatch batch, BitmapFont font, String text,
            float x, float y, float maxWidth, float targetCap, boolean centered, Color color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        float baseCap = Math.max(1f, font.getCapHeight());
        measure.setText(font, text);
        float fit = Math.min(maxWidth / Math.max(1f, measure.width), targetCap / baseCap);
        font.getData().setScale(oldX * fit, oldY * fit);
        try {
            float cap = font.getCapHeight();
            float sh = Math.max(1.5f, cap * 0.075f);
            if (centered) {
                if (shadow) FontHelper.renderFontCentered(batch, font, text, x + sh, y - sh, SHADOW_COLOR);
                FontHelper.renderFontCentered(batch, font, text, x, y, color);
            } else {
                if (shadow) FontHelper.renderFontLeftTopAligned(batch, font, text, x + sh, y + cap * 0.5f - sh, SHADOW_COLOR);
                FontHelper.renderFontLeftTopAligned(batch, font, text, x, y + cap * 0.5f, color);
            }
        } finally {
            font.getData().setScale(oldX, oldY);
        }
    }

    public static void drawMenuButton(SpriteBatch batch, MenuButton button, BitmapFont font, String text) {
        if (button == null || button.hb == null) return;
        Hitbox area = button.hb;
        float slider = CardCrawlGame.mainMenuScreen != null && CardCrawlGame.mainMenuScreen.bg != null
                ? CardCrawlGame.mainMenuScreen.bg.slider : 0f;
        float slideProgress = Interpolation.circleIn.apply(MathUtils.clamp(slider, 0f, 1f));
        float alpha = 1f - slideProgress;
        if (alpha <= 0.01f) return;

        float offsetY = -slideProgress * PortraitViewport.contentHeight() * 0.14f;
        float left = area.x;
        float bottom = area.y + offsetY;
        float right = left + area.width;
        float top = bottom + area.height;
        float cut = Math.min(area.height * 0.24f, area.width * 0.07f);
        float border = Math.max(2f, area.height * 0.026f);
        boolean hovered = area.hovered;

        MenuButton.ClickResult result = button.result;
        boolean hero = result == MenuButton.ClickResult.PLAY || result == MenuButton.ClickResult.RESUME_GAME;
        boolean abandon = result == MenuButton.ClickResult.ABANDON_RUN;
        boolean quit = result == MenuButton.ClickResult.QUIT;

        float packedColor = batch.getPackedColor();
        batch.setBlendFunction(770, 771);
        try {
            if (hero) {
                // Commanding gold-framed Spire ember hero button with rich warm bevel sheen.
                batch.setColor(hovered ? 1f : 0.90f, hovered ? 0.88f : 0.74f, hovered ? 0.56f : 0.40f, 0.98f * alpha);
                drawCutCorner(batch, left, bottom, right, top, cut);
                batch.setColor(hovered ? 0.50f : 0.37f, hovered ? 0.165f : 0.115f, hovered ? 0.125f : 0.095f, 0.94f * alpha);
                drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
                // Subtle warm inner top sheen + bottom ember depth line.
                batch.setColor(1f, 0.84f, 0.50f, (hovered ? 0.26f : 0.16f) * alpha);
                batch.draw(ImageMaster.WHITE_SQUARE_IMG,
                        left + cut, top - border * 2.2f, area.width - cut * 2f, Math.max(2f, border * 0.75f));
                batch.setColor(0f, 0f, 0f, 0.22f * alpha);
                batch.draw(ImageMaster.WHITE_SQUARE_IMG,
                        left + cut, bottom + border * 1.1f, area.width - cut * 2f, Math.max(2f, border * 0.9f));
            } else if (abandon) {
                // Distinct deep crimson-wine destructive bar.
                batch.setColor(hovered ? 0.94f : 0.74f, hovered ? 0.42f : 0.30f, hovered ? 0.38f : 0.28f,
                        (hovered ? 0.96f : 0.84f) * alpha);
                drawCutCorner(batch, left, bottom, right, top, cut);
                batch.setColor(hovered ? 0.26f : 0.15f, hovered ? 0.09f : 0.055f, hovered ? 0.10f : 0.068f,
                        (hovered ? 0.94f : 0.90f) * alpha);
                drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
            } else if (quit) {
                // Quiet deep slate glass tile for Exit.
                batch.setColor(hovered ? 0.88f : 0.42f, hovered ? 0.58f : 0.49f, hovered ? 0.42f : 0.56f,
                        (hovered ? 0.94f : 0.52f) * alpha);
                drawCutCorner(batch, left, bottom, right, top, cut);
                batch.setColor(hovered ? 0.15f : 0.048f, hovered ? 0.11f : 0.068f, hovered ? 0.125f : 0.092f,
                        (hovered ? 0.92f : 0.84f) * alpha);
                drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
            } else {
                // Deep frosted obsidian glass utility tile matching Character Select.
                batch.setColor(hovered ? 0.90f : 0.50f, hovered ? 0.74f : 0.62f, hovered ? 0.42f : 0.70f,
                        (hovered ? 0.96f : 0.64f) * alpha);
                drawCutCorner(batch, left, bottom, right, top, cut);
                batch.setColor(hovered ? 0.12f : 0.055f, hovered ? 0.185f : 0.088f, hovered ? 0.245f : 0.122f,
                        (hovered ? 0.94f : 0.88f) * alpha);
                drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
            }

            float width = PortraitViewport.contentWidth();
            float targetCap = hero
                    ? Math.min(area.height * 0.39f, width * 0.045f)
                    : Math.min(area.height * 0.36f, width * 0.032f);
            Color baseColor = hero ? HERO_TEXT : (abandon ? ABANDON_TEXT : (quit ? QUIT_TEXT : Settings.CREAM_COLOR));
            Color textColor = hovered ? Settings.GOLD_COLOR : baseColor;
            Color faded = new Color(textColor.r, textColor.g, textColor.b, textColor.a * alpha);
            // buttonLabelFont already renders with a crisp dark border; skip offset double-shadow for razor-sharp strokes.
            drawFittedText(batch, FontHelper.buttonLabelFont, text, area.cX, area.cY + offsetY,
                    area.width * 0.84f, targetCap, true, faded, false);
        } finally {
            batch.setColor(packedColor);
        }
    }

    public static void drawMenuLabel(SpriteBatch batch, BitmapFont font, String text, Hitbox area) {
        float packedColor = batch.getPackedColor();
        try {
            float targetCap = Math.min(area.height * 0.42f, PortraitViewport.contentWidth() * 0.040f);
            drawFittedText(batch, font, text, area.cX, area.cY, area.width * 0.88f, targetCap,
                    true, area.hovered ? Settings.GOLD_COLOR : Settings.CREAM_COLOR, true);
        } finally {
            batch.setColor(packedColor);
        }
    }

    static void placeProfileBadge(MainMenuScreen screen, Hitbox hb) {
        if (screen == null || hb == null) return;
        if (screen.screen != MainMenuScreen.CurScreen.MAIN_MENU) {
            hb.width = 0f;
            hb.height = 0f;
            hb.move(-10000f, -10000f);
            return;
        }
        float w = PortraitViewport.contentWidth();
        float h = PortraitViewport.contentHeight();
        float pillH = h * 0.046f;
        float iconSize = pillH * 0.62f;
        float padX = pillH * 0.28f;
        String playerName = CardCrawlGame.playerName != null ? CardCrawlGame.playerName.trim() : "";
        String hint = MainMenuScreen.TEXT[Settings.isTouchScreen || Settings.isMobile ? 5 : 3];
        float nameCap = pillH * 0.30f;
        float hintCap = pillH * 0.20f;

        float textW = measureScaledWidth(FontHelper.buttonLabelFont,
                playerName.isEmpty() ? hint : playerName, nameCap);
        if (!playerName.isEmpty()) {
            textW = Math.max(textW, measureScaledWidth(FontHelper.cardDescFont_N, hint, hintCap));
        }
        float pillW = MathUtils.clamp(padX + iconSize + padX * 0.55f + textW + padX * 1.15f, w * 0.28f, w * 0.60f);
        float left = PortraitViewport.x(0.055f);
        float top = PortraitViewport.y(0.972f);
        hb.width = pillW;
        hb.height = pillH;
        hb.move(left + pillW * 0.5f, top - pillH * 0.5f);
    }

    private static float measureScaledWidth(BitmapFont font, String text, float targetCap) {
        if (text == null || text.isEmpty()) return 0f;
        float oldX = font.getData().scaleX;
        float oldY = font.getData().scaleY;
        float scale = targetCap / Math.max(1f, font.getCapHeight());
        font.getData().setScale(oldX * scale, oldY * scale);
        try {
            measure.setText(font, text);
            return measure.width;
        } finally {
            font.getData().setScale(oldX, oldY);
        }
    }

    static void renderProfileBadge(MainMenuScreen screen, SpriteBatch batch, Hitbox hb) {
        if (screen == null || hb == null || screen.screen != MainMenuScreen.CurScreen.MAIN_MENU) return;
        placeProfileBadge(screen, hb);
        float slider = screen.bg != null ? screen.bg.slider : 0f;
        float slideProgress = Interpolation.circleIn.apply(MathUtils.clamp(slider, 0f, 1f));
        float alpha = 1f - slideProgress;
        if (alpha <= 0.01f) return;

        float left = hb.x;
        float bottom = hb.y;
        float right = left + hb.width;
        float top = bottom + hb.height;
        float cut = hb.height * 0.22f;
        float border = Math.max(2f, hb.height * 0.025f);
        boolean hovered = hb.hovered;

        float packedColor = batch.getPackedColor();
        try {
            batch.setColor(hovered ? 0.90f : 0.52f, hovered ? 0.74f : 0.63f, hovered ? 0.42f : 0.70f,
                    (hovered ? 0.94f : 0.62f) * alpha);
            drawCutCorner(batch, left, bottom, right, top, cut);
            batch.setColor(hovered ? 0.12f : 0.05f, hovered ? 0.18f : 0.082f, hovered ? 0.24f : 0.115f,
                    (hovered ? 0.92f : 0.84f) * alpha);
            drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                    Math.max(1f, cut - border * 0.6f));

            float padX = hb.height * 0.28f;
            float iconSize = hb.height * 0.62f;
            float iconX = left + padX;
            Texture slotIcon = CardCrawlGame.getSaveSlotImg();
            if (slotIcon != null) {
                batch.setColor(1f, 1f, 1f, alpha);
                batch.draw(slotIcon, iconX, hb.cY - iconSize * 0.5f, iconSize, iconSize);
            }

            float textX = iconX + iconSize + padX * 0.55f;
            float textMaxW = Math.max(10f, right - textX - padX * 0.85f);
            String playerName = CardCrawlGame.playerName != null ? CardCrawlGame.playerName.trim() : "";
            String hint = MainMenuScreen.TEXT[Settings.isTouchScreen || Settings.isMobile ? 5 : 3];

            if (playerName.isEmpty()) {
                Color labelColor = hovered ? Settings.GREEN_TEXT_COLOR : Settings.GOLD_COLOR;
                drawFittedText(batch, FontHelper.buttonLabelFont, hint, textX, hb.cY,
                        textMaxW, hb.height * 0.30f, false,
                        new Color(labelColor.r, labelColor.g, labelColor.b, labelColor.a * alpha), false);
            } else {
                Color nameColor = hovered ? Settings.GREEN_TEXT_COLOR : Settings.GOLD_COLOR;
                drawFittedText(batch, FontHelper.buttonLabelFont, playerName, textX, hb.cY + hb.height * 0.18f,
                        textMaxW, hb.height * 0.29f, false,
                        new Color(nameColor.r, nameColor.g, nameColor.b, nameColor.a * alpha), false);
                drawFittedText(batch, FontHelper.cardDescFont_N, hint, textX, hb.cY - hb.height * 0.20f,
                        textMaxW, hb.height * 0.20f, false,
                        new Color(PROFILE_HINT.r, PROFILE_HINT.g, PROFILE_HINT.b, PROFILE_HINT.a * alpha), false);
            }
        } finally {
            batch.setColor(packedColor);
        }
    }

    public static void drawVersionInfo(SpriteBatch batch, MainMenuScreen screen, String text) {
        if (screen == null || screen.screen != MainMenuScreen.CurScreen.MAIN_MENU || text == null || text.isEmpty()) {
            return;
        }
        float slider = screen.bg != null ? screen.bg.slider : 0f;
        float alpha = 1f - Interpolation.circleIn.apply(MathUtils.clamp(slider, 0f, 1f));
        if (alpha <= 0.01f) return;

        String singleLine = text.replace("\r\n", "\n")
                .replace(" NL ", "  ·  ")
                .replace("NL ", "  ·  ")
                .replace("\n", "  ·  ")
                .replaceAll("\\s{3,}", "  ·  ")
                .trim();
        float packedColor = batch.getPackedColor();
        try {
            drawFittedText(batch, FontHelper.cardDescFont_N, singleLine,
                    PortraitViewport.x(0.50f), PortraitViewport.y(0.028f),
                    PortraitViewport.contentWidth() * 0.88f, PortraitViewport.contentHeight() * 0.0092f,
                    true, new Color(VERSION_TEXT.r, VERSION_TEXT.g, VERSION_TEXT.b, VERSION_TEXT.a * alpha), false);
        } finally {
            batch.setColor(packedColor);
        }
    }

    static void drawRow(SpriteBatch batch, BitmapFont font, String text, Hitbox area, boolean disabled) {
        float color = batch.getPackedColor();
        batch.setColor(area.hovered ? 0.28f : 0.10f, area.hovered ? 0.20f : 0.09f, 0.094f, 0.92f);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, area.x, area.y, area.width, area.height);
        batch.setColor(0.75f, 0.60f, 0.39f, 0.80f);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, area.x, area.y + area.height - 1, area.width, 1);
        float scaleX = font.getData().scaleX;
        float scaleY = font.getData().scaleY;
        float textScale = Math.min(area.height * 0.35f, PortraitViewport.contentWidth() * 0.032f)
                / Math.max(1f, font.getCapHeight());
        font.getData().setScale(scaleX * textScale, scaleY * textScale);
        try {
            FontHelper.renderFontCentered(batch, font, text, area.cX, area.cY,
                    disabled ? Color.LIGHT_GRAY : Settings.CREAM_COLOR);
        } finally {
            font.getData().setScale(scaleX, scaleY);
            batch.setColor(color);
        }
    }

    @SpirePatch(clz = LargeDialogOptionButton.class, method = SpirePatch.CLASS)
    public static class OptionText {
        public static final SpireField<PortraitRichText> text = new SpireField<>(() -> new PortraitRichText(true, true));
    }

    private static final Color DISABLED_OPTION_TEXT = new Color(0.58f, 0.61f, 0.65f, 0.90f);

    static void drawOption(SpriteBatch batch, LargeDialogOptionButton option) {
        drawOption(batch, option, 2);
    }

    static void drawOption(SpriteBatch batch, LargeDialogOptionButton option, int count) {
        Hitbox area = option.hb;
        float left = area.x;
        float bottom = area.y;
        float right = left + area.width;
        float top = bottom + area.height;
        float cut = Math.min(area.height * 0.20f, area.width * 0.032f);
        float border = Math.max(2f, area.height * 0.024f);
        boolean disabled = option.isDisabled;
        boolean hovered = area.hovered && !disabled;
        boolean singleChoice = count <= 1;

        float color = batch.getPackedColor();
        try {
            if (disabled) {
                batch.setColor(0.32f, 0.36f, 0.40f, 0.48f);
                drawCutCorner(batch, left, bottom, right, top, cut);
                batch.setColor(0.055f, 0.068f, 0.082f, 0.82f);
                drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
            } else {
                batch.setColor(hovered ? 0.92f : 0.54f, hovered ? 0.76f : 0.66f, hovered ? 0.42f : 0.74f,
                        hovered ? 0.98f : 0.72f);
                drawCutCorner(batch, left, bottom, right, top, cut);
                batch.setColor(hovered ? 0.155f : 0.088f, hovered ? 0.235f : 0.135f, hovered ? 0.295f : 0.178f,
                        hovered ? 0.98f : 0.94f);
                drawCutCorner(batch, left + border, bottom + border, right - border, top - border,
                        Math.max(1f, cut - border * 0.6f));
                // Subtle warm gold vertical accent pill on multi-choice active options for crisp visual scanning.
                if (!singleChoice) {
                    float barW = Math.max(5f, area.width * 0.006f);
                    float barH = area.height * 0.46f;
                    batch.setColor(hovered ? 1f : 0.86f, hovered ? 0.86f : 0.70f, hovered ? 0.50f : 0.38f,
                            hovered ? 0.98f : 0.82f);
                    batch.draw(ImageMaster.WHITE_SQUARE_IMG,
                            left + border + area.width * 0.022f, area.cY - barH * 0.5f, barW, barH);
                }
            }
            float padLeft = area.width * 0.048f;
            float padRight = area.width * 0.028f;
            // Uniform crisp font size across all options so 1-line and 2-line choices match visually with generous padding.
            float targetCap = Math.min(area.height * 0.224f, PortraitViewport.contentWidth() * 0.0248f);
            OptionText.text.get(option).draw(batch, FontHelper.buttonLabelFont, option.msg,
                    disabled ? DISABLED_OPTION_TEXT : Settings.CREAM_COLOR,
                    left + padLeft, area.cY, area.width - padLeft - padRight, area.height * 0.72f,
                    targetCap, singleChoice ? Align.center : Align.left);
        } finally {
            batch.setColor(color);
        }
    }
}
