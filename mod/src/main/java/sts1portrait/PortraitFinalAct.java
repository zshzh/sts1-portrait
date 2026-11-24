package sts1portrait;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.cutscenes.Cutscene;
import com.megacrit.cardcrawl.cutscenes.CutscenePanel;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import com.megacrit.cardcrawl.monsters.ending.SpireShield;
import com.megacrit.cardcrawl.monsters.ending.SpireSpear;
import com.megacrit.cardcrawl.scenes.TheEndingScene;
import java.util.List;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class PortraitFinalAct {
    private PortraitFinalAct() {}

    static boolean placeElitePair(List<AbstractMonster> monsters) {
        if (monsters.size() != 2) return false;
        AbstractMonster first = monsters.get(0);
        AbstractMonster second = monsters.get(1);
        AbstractMonster shield = first instanceof SpireShield ? first : second;
        AbstractMonster spear = first instanceof SpireSpear ? first : second;
        if (!(shield instanceof SpireShield) || !(spear instanceof SpireSpear)) return false;
        float baseline = PortraitBattle.enemyBaseline();
        float unit = PortraitViewport.referenceWidth();
        PortraitBattle.positionEnemy(shield, PortraitViewport.x(.24f), baseline, unit);
        PortraitBattle.positionEnemy(spear, PortraitViewport.x(.76f), baseline, unit);
        return true;
    }

    static float bodyScale(AbstractCreature enemy) {
        if (enemy instanceof CorruptHeart) return .80f;
        if (enemy instanceof SpireShield || enemy instanceof SpireSpear) return .74f;
        return 1f;
    }

    public static void drawCutsceneArt(SpriteBatch batch, Texture art) {
        float scale = Math.min(Settings.WIDTH / (float) art.getWidth(),
                Settings.HEIGHT / (float) art.getHeight());
        float width = art.getWidth() * scale;
        float height = art.getHeight() * scale;
        batch.draw(art, (Settings.WIDTH - width) * .5f, (Settings.HEIGHT - height) * .5f, width, height);
    }

    @SpirePatch(clz = Cutscene.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = TheEndingScene.class, method = "renderCampfireRoom", paramtypez = {SpriteBatch.class})
    public static class OpaqueBackdrop {
        @SpirePrefixPatch
        public static void before(Object __instance, SpriteBatch sb) {
            if (!PortraitViewport.active()) return;
            float color = sb.getPackedColor();
            sb.setColor(Color.BLACK);
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0f, 0f, Settings.WIDTH, Settings.HEIGHT);
            sb.setColor(color);
        }
    }

    @SpirePatch(clz = Cutscene.class, method = "renderImg", paramtypez = {SpriteBatch.class, Texture.class})
    @SpirePatch(clz = CutscenePanel.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class CutsceneArt {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (SpriteBatch.class.getName().equals(call.getClassName()) && "draw".equals(call.getMethodName())
                            && "(Lcom/badlogic/gdx/graphics/Texture;FFFF)V".equals(call.getSignature())) {
                        call.replace("{ if (sts1portrait.PortraitViewport.active())"
                                + " sts1portrait.PortraitFinalAct.drawCutsceneArt($0, $1); else $proceed($$); }");
                    }
                }
            };
        }
    }
}
