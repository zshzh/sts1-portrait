package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
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
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import com.megacrit.cardcrawl.ui.panels.TopPanel;

public final class PortraitUi {
    private static final Matrix4 previousTransform = new Matrix4();
    private static final Matrix4 transform = new Matrix4();
    private static TitleBackground cachedTitle;
    private static AtlasRegion titleSky;
    private static AtlasRegion titleLandscape;
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

    static void renderTitle(TitleBackground title, SpriteBatch batch) {
        if (cachedTitle != title) {
            cachedTitle = title;
            titleSky = ReflectionHacks.getPrivate(title, TitleBackground.class, "sky");
            titleLandscape = ReflectionHacks.getPrivate(title, TitleBackground.class, "mg3Top");
            titleLogo = ReflectionHacks.getPrivateStatic(TitleBackground.class, "titleLogoImg");
        }
        float color = batch.getPackedColor();
        batch.setColor(0.12f, 0.10f, 0.17f, 1f);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, 0, 0, Settings.WIDTH, Settings.HEIGHT);
        boolean main = CardCrawlGame.mainMenuScreen == null
                || CardCrawlGame.mainMenuScreen.screen == MainMenuScreen.CurScreen.MAIN_MENU;
        if (main) batch.setColor(.75f, .80f, .82f, 1f);
        else batch.setColor(Color.WHITE);
        drawCover(batch, titleSky, 0, Settings.HEIGHT);
        drawCover(batch, titleLandscape, Settings.HEIGHT * 0.16f, Settings.HEIGHT * 0.84f);
        if (titleLogo != null && main) {
            batch.setColor(Color.WHITE);
            float logoWidth = PortraitViewport.contentWidth() * 0.61f;
            float logoHeight = logoWidth * titleLogo.getHeight() / titleLogo.getWidth();
            float x = PortraitViewport.x(0.5f);
            float y = PortraitViewport.y(0.78f);
            batch.draw(titleLogo, x - logoWidth * 0.5f, y - logoHeight * 0.5f, logoWidth, logoHeight);
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

    public static void drawMenuLabel(SpriteBatch batch, BitmapFont font, String text, Hitbox area) {
        float packedColor = batch.getPackedColor();
        float scaleX = font.getData().scaleX;
        float scaleY = font.getData().scaleY;
        float cap = Math.max(1f, font.getCapHeight());
        float width = PortraitViewport.contentWidth();
        float size = Math.min(area.height * .44f, width * .043f);
        font.getData().setScale(scaleX * size / cap, scaleY * size / cap);
        try {
            Color textColor = area.hovered ? Settings.GOLD_COLOR : Settings.CREAM_COLOR;
            float shadow = Math.max(1f, size * .085f);
            FontHelper.renderFontCentered(batch, font, text, area.cX + shadow, area.cY - shadow, Color.BLACK);
            FontHelper.renderFontCentered(batch, font, text, area.cX, area.cY, textColor);
        } finally {
            font.getData().setScale(scaleX, scaleY);
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
        public static final SpireField<PortraitRichText> text = new SpireField<>(() -> new PortraitRichText(true));
    }

    static void drawOption(SpriteBatch batch, LargeDialogOptionButton option) {
        Hitbox area = option.hb;
        float color = batch.getPackedColor();
        batch.setColor(area.hovered && !option.isDisabled ? 0.24f : 0.09f, 0.13f, 0.16f, 0.96f);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, area.x, area.y, area.width, area.height);
        float pad = area.width * .048f;
        try {
            OptionText.text.get(option).draw(batch, FontHelper.buttonLabelFont, option.msg,
                    option.isDisabled ? Color.LIGHT_GRAY : Settings.CREAM_COLOR, area.x + pad, area.cY,
                    area.width - 2 * pad, area.height * .72f, Math.min(area.width * .036f, area.height * .43f),
                    Align.center);
        } finally {
            batch.setColor(color);
        }
    }
}
