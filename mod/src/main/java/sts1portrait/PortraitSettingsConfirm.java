package sts1portrait;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.options.ConfirmPopup;
import com.megacrit.cardcrawl.ui.panels.DeleteSaveConfirmPopup;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitSettingsConfirm {
    private static final Color SHADE = new Color(0f, 0f, 0f, .60f);

    private static final Choice YES = new Choice(-.225f);
    private static final Choice NO = new Choice(.225f);

    private PortraitSettingsConfirm() {}

    static boolean usesSettingsStyle(ConfirmPopup popup) {
        if (popup instanceof DeleteSaveConfirmPopup) return true;
        if (popup.getClass() != ConfirmPopup.class) return false;
        return popup.type == ConfirmPopup.ConfirmType.EXIT
                || popup.type == ConfirmPopup.ConfirmType.ABANDON_MID_RUN
                || popup.type == ConfirmPopup.ConfirmType.ABANDON_MAIN_MENU;
    }

    private static float width() { return PortraitSettings.panelWidth() * .83f; }
    private static float height() { return width() * .56f; }
    private static float centerX() { return PortraitViewport.x(.5f); }
    private static float centerY() { return PortraitViewport.y(.51f); }

    static void draw(ConfirmPopup popup, SpriteBatch batch) {
        if (!popup.shown) return;
        float width = width();
        float height = height();
        float x = centerX();
        float y = centerY();
        float color = batch.getPackedColor();
        PortraitDrawing.rectangle(batch, 0f, 0f, Settings.WIDTH, Settings.HEIGHT, SHADE);

        PortraitSettings.frame(batch, x, y + height * .5f, width, height);
        PortraitSettings.label(batch, popup.title, x, y + height * .33f, width * .86f, height * .095f, true);
        PortraitSettings.wrappedLabel(batch, popup.desc, x, y + height * .055f,
                width * .83f, height * .067f, height * .37f);
        String[] text = CardCrawlGame.languagePack.getUIString("ConfirmPopup").TEXT;
        YES.draw(batch, popup.yesHb, text[2], popup.type == ConfirmPopup.ConfirmType.EXIT
                ? PortraitSettings.ACTIVE : PortraitSettings.DESTRUCTIVE);
        NO.draw(batch, popup.noHb, text[3], PortraitSettings.ROW);
        batch.setColor(color);
    }

    static void map(ConfirmPopup popup, int x, int y) {

        Hitbox target = YES.hit(x, y) ? popup.yesHb : NO.hit(x, y) ? popup.noHb : null;
        InputHelper.mX = target == null ? -10000 : Math.round(target.cX);
        InputHelper.mY = target == null ? -10000 : Math.round(target.cY);
    }

    private static final class Choice {

        final float offset;
        float x, y, width, height;

        Choice(float offset) {

            this.offset = offset;
        }

        void layout() {
            x = (centerX() + PortraitSettingsConfirm.width() * offset);
            y = (centerY() - PortraitSettingsConfirm.height() * .30f);
            width = PortraitSettingsConfirm.width() * .41f;
            height = PortraitSettingsConfirm.height() * .26f;
        }

        boolean hit(int mouseX, int mouseY) {
            layout();

            return Math.abs((mouseX) - x) < width * .5f
                    && Math.abs((mouseY) - y) < height * .5f;
        }

        void draw(SpriteBatch batch, Hitbox hitbox, String label, Color color) {
            layout();

            PortraitDrawing.rectangle(batch, x - width * .5f, y - height * .5f, width, height,
                    hitbox.hovered && color == PortraitSettings.ROW ? PortraitSettings.ACTIVE : color);
            PortraitSettings.label(batch, label, x, y, width * .88f, height * .32f, true);
        }
    }

    public static void updateButton(Hitbox hitbox) {
        boolean portrait = PortraitScreens.enabled();
        if (portrait && InputHelper.justClickedLeft && InputHelper.justReleasedClickLeft) {
            hitbox.clickStarted = true;
        }
        hitbox.update();
        // Native confirmation prioritizes the hover sound over capturing a press.
        if (portrait && (InputHelper.justClickedLeft || hitbox.clicked)) hitbox.justHovered = false;
    }

    @SpirePatch(clz = ConfirmPopup.class, method = "updateYes")
    @SpirePatch(clz = ConfirmPopup.class, method = "updateNo")
    public static class Tap {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (Hitbox.class.getName().equals(call.getClassName()) && "update".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitSettingsConfirm.updateButton($0); }");
                    }
                }

                @Override public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && InputHelper.class.getName().equals(field.getClassName())
                            && "justClickedLeft".equals(field.getFieldName())) {
                        field.replace("{ $_ = $proceed() && (!sts1portrait.PortraitScreens.enabled()"
                                + " || !com.megacrit.cardcrawl.helpers.input.InputHelper.justReleasedClickLeft); }");
                    }
                }
            };
        }
    }
}
