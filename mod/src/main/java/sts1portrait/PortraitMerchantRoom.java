package sts1portrait;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.shop.Merchant;

public final class PortraitMerchantRoom {
    private static final float FIGURE_SCALE = 1.55f;
    private static final float PLAYER_X = .30f;
    private static final float MERCHANT_X = .70f;
    private static final float FLOOR_Y = .25f;
    private static final Matrix4 spriteBefore = new Matrix4();
    private static final Matrix4 polygonBefore = new Matrix4();
    private static final Matrix4 moved = new Matrix4();

    private PortraitMerchantRoom() {}

    private static boolean active() {
        return PortraitViewport.active() && AbstractDungeon.currMapNode != null
                && AbstractDungeon.getCurrRoom() instanceof ShopRoom;
    }

    private static boolean active(Merchant merchant) {
        return active() && ((ShopRoom) AbstractDungeon.getCurrRoom()).merchant == merchant;
    }

    private static float merchantSourceX() { return Merchant.DRAW_X + 256f * Settings.scale; }
    private static float merchantSourceY() { return AbstractDungeon.floorY + 30f * Settings.scale; }

    private static void begin(SpriteBatch batch, float sourceX, float sourceY, float destinationX, float destinationY) {
        spriteBefore.set(batch.getTransformMatrix());
        polygonBefore.set(CardCrawlGame.psb.getTransformMatrix());
        batch.setTransformMatrix(transform(spriteBefore, sourceX, sourceY, destinationX, destinationY));
        CardCrawlGame.psb.setTransformMatrix(transform(polygonBefore, sourceX, sourceY, destinationX, destinationY));
    }

    private static Matrix4 transform(Matrix4 base, float sourceX, float sourceY, float x, float y) {
        return moved.set(base).translate(x - sourceX * FIGURE_SCALE, y - sourceY * FIGURE_SCALE, 0f)
                .scale(FIGURE_SCALE, FIGURE_SCALE, 1f);
    }

    private static void end(SpriteBatch batch) {
        batch.setTransformMatrix(spriteBefore);
        CardCrawlGame.psb.setTransformMatrix(polygonBefore);
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "renderPlayerImage", paramtypez = {SpriteBatch.class})
    public static class PlayerDrawing {
        @SpirePrefixPatch
        public static void before(AbstractPlayer player, SpriteBatch batch) {
            if (active()) begin(batch, player.drawX, player.drawY, PortraitViewport.x(PLAYER_X), PortraitViewport.y(FLOOR_Y));
        }

        @SpirePostfixPatch
        public static void after(AbstractPlayer player, SpriteBatch batch) {
            if (active()) end(batch);
        }
    }

    @SpirePatch(clz = Merchant.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class MerchantDrawing {
        @SpirePrefixPatch
        public static void before(Merchant merchant, SpriteBatch batch) {
            if (active(merchant)) begin(batch, merchantSourceX(), merchantSourceY(), PortraitViewport.x(MERCHANT_X),
                    PortraitViewport.y(FLOOR_Y));
        }

        @SpirePostfixPatch
        public static void after(Merchant merchant, SpriteBatch batch) {
            if (active(merchant)) end(batch);
        }
    }

    @SpirePatch(clz = Merchant.class, method = "update")
    public static class MerchantTarget {
        @SpirePrefixPatch
        public static void before(Merchant merchant, float ___modX, float ___modY) {
            if (!active(merchant)) return;
            Hitbox hb = merchant.hb;
            hb.width = 360f * Settings.scale * FIGURE_SCALE;
            hb.height = 300f * Settings.scale * FIGURE_SCALE;
            float originalX = Merchant.DRAW_X + (250f + ___modX) * Settings.scale;
            float originalY = Merchant.DRAW_Y + (130f + ___modY) * Settings.scale;
            hb.move(PortraitViewport.x(MERCHANT_X) + (originalX - merchantSourceX()) * FIGURE_SCALE,
                    PortraitViewport.y(FLOOR_Y) + (originalY - merchantSourceY()) * FIGURE_SCALE);
        }
    }
}
