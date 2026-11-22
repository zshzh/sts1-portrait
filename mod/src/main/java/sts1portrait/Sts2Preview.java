package sts1portrait;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.PotionSlot;
import java.util.ArrayList;
import java.util.WeakHashMap;

public final class Sts2Preview {
    private static final float POTION_RIGHT = .966f;
    private static final float POTION_SLOT_WIDTH = .087f;
    private static final float MAX_POTION_FRAME_WIDTH = .48f;
    private static final Matrix4 potionBase = new Matrix4();
    private static final Matrix4 potionTransform = new Matrix4();
    private static final float[] potionFrameVertices = new float[60];
    private static final WeakHashMap<Texture, Vector2> potionArtworkCenters = new WeakHashMap<>();
    private static AbstractPotion drawingPotion;

    static boolean compact() {
        return PortraitCombatHud.active() && !AbstractDungeon.isScreenUp;
    }

    static boolean aiming() {
        AbstractPlayer p = AbstractDungeon.player;
        return compact() && p.hoveredCard != null
                && (p.hoveredCard.target == AbstractCard.CardTarget.ENEMY
                || p.hoveredCard.target == AbstractCard.CardTarget.SELF_AND_ENEMY)
                && (p.isDraggingCard || p.inSingleTargetMode);
    }

    public static float bandY() {
        return PortraitBattle.battleHandTop() + PortraitViewport.referenceWidth() * .037f;
    }

    static float bandHeight() { return PortraitViewport.referenceWidth() * .022f; }

    static float bandWidth(ArrayList<AbstractMonster> monsters, AbstractMonster monster) {
        float width = PortraitViewport.contentWidth();
        float max = width * (monsters.size() == 1 ? .86f : .40f);
        for (AbstractMonster other : monsters) {
            if (monster != other && !other.isDeadOrEscaped()) {
                max = Math.min(max, Math.abs(monster.hb.cX - other.hb.cX) - width * .016f);
            }
        }
        return Math.max(width * .08f, max);
    }

    static boolean canUseBand(ArrayList<AbstractMonster> enemies) {
        return enemies.size() <= 2 && (enemies.size() < 2
                || !SlimeSplitLayout.isSplitChild(enemies.get(0))
                || Math.abs(enemies.get(0).hb.cX - enemies.get(1).hb.cX) > PortraitViewport.contentWidth() * .15f);
    }

    static void drawBand(SpriteBatch batch) {
        if (!aiming()) return;
        ArrayList<AbstractMonster> enemies = SlimeSplitLayout.roster(AbstractDungeon.getMonsters());
        if (!canUseBand(enemies)) return;
        float y = bandY(), h = bandHeight();
        float color = batch.getPackedColor();
        for (AbstractMonster monster : enemies) {
            if (monster.isDeadOrEscaped()) continue;
            float x = monster.hb.cX, w = bandWidth(enemies, monster);
            boolean active = Math.abs(InputHelper.mX - x) < w * .5f
                    && Math.abs(InputHelper.mY - y) < h * .5f;
            batch.setColor(active ? .50f : .13f, active ? .35f : .19f, active ? .22f : .27f, .95f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, x - w * .5f, y - h * .5f, w, h);
            com.badlogic.gdx.graphics.g2d.BitmapFont font = FontHelper.tipHeaderFont;
            float originalX = font.getData().scaleX, originalY = font.getData().scaleY;
            float scale = PortraitViewport.contentWidth() * .015f / Math.max(1f, font.getCapHeight());
            font.getData().setScale(originalX * scale, originalY * scale);
            try {
                String release = Settings.language == Settings.GameLanguage.ZHS ? "松手攻击 · " : "Release · ";
                FontHelper.renderFontCentered(batch, font,
                        active ? release + monster.name : monster.name, x, y, Settings.CREAM_COLOR);
            } finally {
                font.getData().setScale(originalX, originalY);
            }
        }
        batch.setColor(color);
    }

    static float potionY(float fallback) { return compact() ? PortraitBattle.playerBattleY() : fallback; }

    static float potionPitch() {
        return PortraitViewport.contentWidth()
                * Math.min(POTION_SLOT_WIDTH, MAX_POTION_FRAME_WIDTH / Math.max(1, AbstractDungeon.player.potions.size()));
    }

    static float potionIconScale() {
        return potionPitch() / (POTION_SLOT_WIDTH * PortraitViewport.contentWidth()) * 2.05f;
    }

    static float potionX(AbstractPotion potion) {
        ArrayList<AbstractPotion> potions = AbstractDungeon.player.potions;
        return PortraitViewport.x(POTION_RIGHT)
                - potionPitch() * (potions.size() - .5f - potions.indexOf(potion));
    }

    static void drawPotions(SpriteBatch batch) {
        ArrayList<AbstractPotion> potions = AbstractDungeon.player.potions;
        if (potions.isEmpty()) return;
        float width = PortraitViewport.contentWidth();
        float y = PortraitBattle.playerBattleY();
        float right = PortraitViewport.x(POTION_RIGHT);
        float frameWidth = potionPitch() * potions.size();
        float frameHeight = width * .082f;
        float border = width * .0032f;
        float corner = width * .013f;
        float scale = potionIconScale();
        float left = right - frameWidth;
        float bottom = y - frameHeight * .5f;
        float color = batch.getPackedColor();
        batch.setColor(.23f, .34f, .40f, 1f);
        drawCutCornerFrame(batch, left, bottom, right, bottom + frameHeight, corner);
        batch.setColor(.075f, .116f, .150f, 1f);
        drawCutCornerFrame(batch, left + border, bottom + border, right - border,
                bottom + frameHeight - border, corner - border * .59f);
        potionBase.set(batch.getTransformMatrix());
        try {
            for (AbstractPotion potion : potions) {
                batch.setColor(Color.WHITE);
                batch.setTransformMatrix(potionTransform.set(potionBase).translate(potionX(potion), y, 0f)
                        .scale(scale, scale, 1f).translate(-potion.posX, -potion.posY, 0f));
                drawingPotion = potion;
                potion.render(batch);
            }
        } finally {
            drawingPotion = null;
            batch.setTransformMatrix(potionBase);
            batch.setColor(color);
        }
    }

    @SpirePatch(clz = AbstractPotion.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class CenterPotionArtwork {
        @SpirePrefixPatch
        public static void before(AbstractPotion potion, SpriteBatch batch, Texture ___containerImg) {
            if (potion != drawingPotion) return;
            Texture artwork = potion instanceof PotionSlot ? ImageMaster.POTION_PLACEHOLDER : ___containerImg;
            if (artwork == null) return;
            Vector2 center = potionArtworkCenters.get(artwork);
            if (center == null) {
                center = measurePotionArtwork(artwork);
                potionArtworkCenters.put(artwork, center);
            }
            if (center.x == 0f && center.y == 0f) return;
            batch.setTransformMatrix(potionTransform.set(batch.getTransformMatrix())
                    .translate(-center.x * potion.scale, -center.y * potion.scale, 0f));
        }
    }

    private static Vector2 measurePotionArtwork(Texture artwork) {
        if (!(artwork.getTextureData() instanceof FileTextureData)) return new Vector2();
        FileTextureData source = (FileTextureData) artwork.getTextureData();
        // The vanilla renderer samples the first 64 × 64 pixels of each bottle texture.
        try {
            Pixmap sprite = new Pixmap(source.getFileHandle());
            try {
                int left = 64, top = 64, right = -1, bottom = -1;
                for (int y = 0; y < Math.min(64, sprite.getHeight()); y++) {
                    for (int x = 0; x < Math.min(64, sprite.getWidth()); x++) {
                        if ((sprite.getPixel(x, y) & 0xff) < 32) continue;
                        left = Math.min(left, x);
                        right = Math.max(right, x);
                        top = Math.min(top, y);
                        bottom = Math.max(bottom, y);
                    }
                }
                if (right >= left) return new Vector2((left + right + 1f) * .5f - 32f, (63f - top - bottom) * .5f);
            } finally {
                sprite.dispose();
            }
        } catch (RuntimeException unavailable) {
            // Some mods supply a texture without a readable source image.
        }
        return new Vector2();
    }

    private static void drawCutCornerFrame(SpriteBatch batch, float left, float bottom, float right, float top,
                                           float cut) {
        // Three quads cover the octagon without a custom shader or a new texture.
        float color = batch.getPackedColor();
        frameVertex(0, left + cut, bottom, color);
        frameVertex(1, left, bottom + cut, color);
        frameVertex(2, right, bottom + cut, color);
        frameVertex(3, right - cut, bottom, color);
        frameVertex(4, left, bottom + cut, color);
        frameVertex(5, left, top - cut, color);
        frameVertex(6, right, top - cut, color);
        frameVertex(7, right, bottom + cut, color);
        frameVertex(8, left, top - cut, color);
        frameVertex(9, left + cut, top, color);
        frameVertex(10, right - cut, top, color);
        frameVertex(11, right, top - cut, color);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, potionFrameVertices, 0, potionFrameVertices.length);
    }

    private static void frameVertex(int index, float x, float y, float color) {
        int offset = index * 5;
        potionFrameVertices[offset] = x;
        potionFrameVertices[offset + 1] = y;
        potionFrameVertices[offset + 2] = color;
        potionFrameVertices[offset + 3] = .5f;
        potionFrameVertices[offset + 4] = .5f;
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "renderPlayerImage", paramtypez = {SpriteBatch.class})
    public static class Avatar {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(AbstractPlayer __instance, SpriteBatch batch) {
            if (!compact()) return SpireReturn.Continue();
            Texture portrait;
            int cropX, cropY, cropSize;
            float red, green, blue;
            switch (__instance.chosenClass) {
                case IRONCLAD:
                    portrait = ImageMaster.CHAR_SELECT_BG_IRONCLAD;
                    cropX = 1090; cropY = 45; cropSize = 630;
                    red = .72f; green = .45f; blue = .28f;
                    break;
                case THE_SILENT:
                    portrait = ImageMaster.CHAR_SELECT_BG_SILENT;
                    cropX = 1285; cropY = 70; cropSize = 635;
                    red = .40f; green = .60f; blue = .31f;
                    break;
                case DEFECT:
                    portrait = ImageMaster.CHAR_SELECT_BG_DEFECT;
                    cropX = 1145; cropY = 80; cropSize = 630;
                    red = .36f; green = .55f; blue = .70f;
                    break;
                case WATCHER:
                    portrait = ImageMaster.CHAR_SELECT_BG_WATCHER;
                    cropX = 690; cropY = 115; cropSize = 630;
                    red = .56f; green = .39f; blue = .70f;
                    break;
                default: return SpireReturn.Continue();
            }
            float size = PortraitViewport.contentWidth() * .092f;
            float x = PortraitViewport.x(.079f), y = PortraitBattle.playerBattleY();
            float color = batch.getPackedColor();
            batch.setColor(.087f, .105f, .128f, 1f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, x - size * .54f, y - size * .54f, size * 1.08f, size * 1.08f);
            batch.setColor(red, green, blue, 1f);
            batch.draw(ImageMaster.WHITE_SQUARE_IMG, x - size * .51f, y - size * .51f, size * 1.02f, size * 1.02f);
            batch.setColor(Color.WHITE);
            batch.draw(portrait, x - size * .48f, y - size * .48f, size * .96f, size * .96f,
                    cropX, cropY, cropSize, cropSize, false, false);
            batch.setColor(color);
            return SpireReturn.Return();
        }
    }
}
