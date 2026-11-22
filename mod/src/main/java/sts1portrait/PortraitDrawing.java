package sts1portrait;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;

final class PortraitDrawing {
    private PortraitDrawing() {}

    static boolean chinese() { return Settings.language == Settings.GameLanguage.ZHS; }

    static void rectangle(SpriteBatch batch, float x, float y, float width, float height, Color color) {
        batch.setColor(color);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, width, height);
    }

    static void text(SpriteBatch batch, String label, float x, float y, float scale) {
        FontHelper.renderFontCentered(batch, FontHelper.buttonLabelFont, label, x, y, Color.WHITE, scale);
    }
}
