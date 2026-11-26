package sts1portrait;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.options.ConfirmPopup;
import com.megacrit.cardcrawl.ui.panels.DeleteSaveConfirmPopup;
import java.util.regex.Pattern;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitSettingsConfirm {
    private static final Color SHADE = new Color(0.014f, 0.022f, 0.036f, 0.68f);
    private static final Color EXIT_BADGE_BG = new Color(0.11f, 0.225f, 0.270f, 0.96f);
    private static final Color EXIT_BADGE_BORDER = new Color(0.47f, 0.71f, 0.78f, 0.65f);
    private static final Color EXIT_BADGE_TEXT = new Color(0.62f, 0.88f, 0.94f, 1f);
    private static final Color WARN_BADGE_BG = new Color(0.295f, 0.135f, 0.118f, 0.96f);
    private static final Color WARN_BADGE_BORDER = new Color(0.89f, 0.56f, 0.47f, 0.70f);
    private static final Color WARN_BADGE_TEXT = new Color(1f, 0.72f, 0.63f, 1f);
    private static final Color WARN_BOX_BG = new Color(0.17f, 0.09f, 0.086f, 0.90f);
    private static final Color WARN_BOX_BORDER = new Color(0.56f, 0.27f, 0.22f, 0.65f);
    private static final Color WARN_TITLE = new Color(1f, 0.82f, 0.72f, 1f);
    private static final Color WARN_DIVIDER = new Color(0.88f, 0.55f, 0.41f, 0.55f);

    private static final Pattern ASTERISK_EMPHASIS = Pattern.compile("\\*([^*\\n]+)\\*");
    private static final PortraitRichText bodyText = new PortraitRichText(false, false);

    private PortraitSettingsConfirm() {}

    static boolean usesSettingsStyle(ConfirmPopup popup) {
        if (popup instanceof DeleteSaveConfirmPopup) return true;
        if (popup.getClass() != ConfirmPopup.class) return false;
        return popup.type == ConfirmPopup.ConfirmType.EXIT
                || popup.type == ConfirmPopup.ConfirmType.ABANDON_MID_RUN
                || popup.type == ConfirmPopup.ConfirmType.ABANDON_MAIN_MENU;
    }

    private static boolean isUnsavedExitWarning(ConfirmPopup popup) {
        if (popup.type != ConfirmPopup.ConfirmType.EXIT || popup.desc == null) return false;
        String d = popup.desc;
        return d.indexOf('*') >= 0 || d.contains("警告") || d.contains("WARNING") || d.contains("NOT");
    }

    private static boolean isWarnStyle(ConfirmPopup popup) {
        return popup.type != ConfirmPopup.ConfirmType.EXIT || isUnsavedExitWarning(popup);
    }

    private static String badgeLabel(ConfirmPopup popup) {
        boolean cn = PortraitDrawing.chinese();
        if (popup instanceof DeleteSaveConfirmPopup || popup.type == ConfirmPopup.ConfirmType.DELETE_SAVE) {
            return cn ? "删除存档" : "Delete Save";
        }
        if (popup.type == ConfirmPopup.ConfirmType.EXIT) {
            if (isUnsavedExitWarning(popup)) return cn ? "未保存进度警告" : "Progress Not Saved";
            return cn ? "保存并退出" : "Save & Quit";
        }
        return cn ? "放弃当前爬塔" : "Abandon Run";
    }

    private static String formatDesc(ConfirmPopup popup) {
        if (popup.desc == null) return "";
        String s = popup.desc.replace("\r\n", "\n").trim();
        // Convert vanilla *emphasis* markers (e.g. "进度*没有*保存。") into crisp coral-red highlight.
        s = ASTERISK_EMPHASIS.matcher(s).replaceAll("[#FF7B6B]$1[]");
        if (isUnsavedExitWarning(popup)) {
            s = s.replace("警告：", "[#FF967D]警告：[]")
                    .replace("WARNING:", "[#FF967D]WARNING:[]");
        }
        return s;
    }

    private static final class Layout {
        float width, height, cx, cy, top, padX, padY;
        float badgeCy, badgeW, badgeH;
        float titleCy, titleCap;
        float dividerY;
        float descCy, descBoxH, descTextW, descCap;
        float btnCy, btnW, btnH, yesCx, noCx;

        void compute(ConfirmPopup popup) {
            width = PortraitSettings.panelWidth() * 0.90f;
            padX = width * 0.068f;
            padY = width * 0.062f;
            float innerW = width - padX * 2f;
            boolean warnBox = isUnsavedExitWarning(popup);

            badgeH = width * 0.068f;
            badgeW = Math.min(innerW * 0.64f, Math.max(width * 0.36f, badgeH * 4.4f));
            titleCap = width * 0.056f;
            float titleH = titleCap * 1.45f;
            descCap = width * 0.042f;
            descTextW = warnBox ? (innerW - width * 0.08f) : innerW * 0.94f;

            String formatted = formatDesc(popup);
            BitmapFont bodyFont = PortraitFonts.body();
            float measuredH = bodyText.measureHeight(bodyFont, formatted, descTextW, descCap);
            float minDescH = descCap * 2.1f;
            float maxDescH = PortraitViewport.contentHeight() * 0.34f;
            float clampedTextH = Math.max(minDescH, Math.min(maxDescH, measuredH + descCap * 0.45f));
            descBoxH = warnBox ? (clampedTextH + width * 0.075f) : clampedTextH;

            btnH = Math.min(width * 0.132f, PortraitViewport.contentHeight() * 0.064f);
            float btnGap = innerW * 0.045f;
            btnW = (innerW - btnGap) * 0.5f;

            float g1 = width * 0.032f;
            float g2 = width * 0.036f;
            float g3 = width * 0.044f;
            float g4 = width * 0.052f;

            height = padY + badgeH + g1 + titleH + g2 + g3 + descBoxH + g4 + btnH + padY;
            cx = PortraitViewport.x(0.5f);
            cy = PortraitViewport.y(0.52f);
            top = cy + height * 0.5f;

            float cursor = top - padY;
            badgeCy = cursor - badgeH * 0.5f;
            cursor -= badgeH + g1;
            titleCy = cursor - titleH * 0.5f;
            cursor -= titleH + g2 * 0.5f;
            dividerY = cursor;
            cursor -= g2 * 0.5f + g3;
            descCy = cursor - descBoxH * 0.5f;
            cursor -= descBoxH + g4;
            btnCy = cursor - btnH * 0.5f;
            yesCx = cx - (btnW + btnGap) * 0.5f;
            noCx = cx + (btnW + btnGap) * 0.5f;
        }

        boolean hitYes(int mx, int my) {
            return Math.abs(mx - yesCx) < btnW * 0.5f && Math.abs(my - btnCy) < btnH * 0.5f;
        }

        boolean hitNo(int mx, int my) {
            return Math.abs(mx - noCx) < btnW * 0.5f && Math.abs(my - btnCy) < btnH * 0.5f;
        }
    }

    private static final Layout layout = new Layout();

    static void draw(ConfirmPopup popup, SpriteBatch batch) {
        if (!popup.shown) return;
        layout.compute(popup);
        boolean warn = isWarnStyle(popup);
        boolean warnBox = isUnsavedExitWarning(popup);

        float packed = batch.getPackedColor();
        try {
            PortraitDrawing.rectangle(batch, 0f, 0f, Settings.WIDTH, Settings.HEIGHT, SHADE);
            PortraitSettings.drawPanel(batch, layout.cx, layout.top, layout.width, layout.height, warn);

            // Top status badge pill
            PortraitSettings.drawChamferedBox(batch,
                    layout.cx - layout.badgeW * 0.5f, layout.badgeCy - layout.badgeH * 0.5f,
                    layout.badgeW, layout.badgeH,
                    layout.badgeH * 0.42f, Math.max(1.5f, layout.badgeH * 0.045f),
                    warn ? WARN_BADGE_BORDER : EXIT_BADGE_BORDER,
                    warn ? WARN_BADGE_BG : EXIT_BADGE_BG);
            PortraitSettings.drawFittedText(batch, PortraitFonts.header(), badgeLabel(popup),
                    layout.cx, layout.badgeCy, layout.badgeW * 0.86f, layout.badgeH * 0.48f,
                    true, warn ? WARN_BADGE_TEXT : EXIT_BADGE_TEXT);

            // Title & divider
            PortraitSettings.drawFittedText(batch, PortraitFonts.header(), popup.title,
                    layout.cx, layout.titleCy, layout.width - layout.padX * 2f, layout.titleCap,
                    true, warn ? WARN_TITLE : PortraitSettings.TEXT_GOLD);
            PortraitSettings.drawDivider(batch, layout.cx, layout.dividerY,
                    layout.width * 0.42f, warn ? WARN_DIVIDER : PortraitSettings.DIVIDER_GOLD);

            // Description (with recessed amber-red card for unsaved progress warning)
            if (warnBox) {
                float boxW = layout.width - layout.padX * 2f;
                PortraitSettings.drawChamferedBox(batch,
                        layout.cx - boxW * 0.5f, layout.descCy - layout.descBoxH * 0.5f,
                        boxW, layout.descBoxH,
                        layout.width * 0.022f, Math.max(1.5f, layout.width * 0.003f),
                        WARN_BOX_BORDER, WARN_BOX_BG);
            }
            bodyText.draw(batch, PortraitFonts.body(), formatDesc(popup),
                    PortraitSettings.TEXT_CREAM,
                    layout.cx - layout.descTextW * 0.5f, layout.descCy,
                    layout.descTextW, layout.descBoxH * 0.90f,
                    layout.descCap, Align.center);

            // Yes & No buttons
            String[] text = CardCrawlGame.languagePack.getUIString("ConfirmPopup").TEXT;
            drawChoice(batch, layout.yesCx, layout.btnCy, layout.btnW, layout.btnH,
                    popup.yesHb != null && popup.yesHb.hovered, text[2], warn, true);
            drawChoice(batch, layout.noCx, layout.btnCy, layout.btnW, layout.btnH,
                    popup.noHb != null && popup.noHb.hovered, text[3], false, false);
        } finally {
            batch.setColor(packed);
        }
    }

    private static void drawChoice(SpriteBatch batch, float cx, float cy, float w, float h,
            boolean hovered, String label, boolean danger, boolean primary) {
        Color border;
        Color fill;
        Color textColor;
        if (danger) {
            border = PortraitSettings.BORDER_DANGER;
            fill = hovered ? PortraitSettings.DESTRUCTIVE_HOVER : PortraitSettings.DESTRUCTIVE;
            textColor = PortraitSettings.TEXT_DANGER;
        } else if (primary) {
            border = hovered ? PortraitSettings.BORDER_GOLD_BRIGHT : PortraitSettings.BORDER_GOLD;
            fill = hovered ? PortraitSettings.ACTIVE_HOVER : PortraitSettings.ACTIVE;
            textColor = PortraitSettings.TEXT_CREAM;
        } else {
            border = hovered ? PortraitSettings.BORDER_GOLD : PortraitSettings.BORDER_SUBTLE;
            fill = hovered ? PortraitSettings.ROW_HOVER : PortraitSettings.ROW;
            textColor = PortraitSettings.TEXT_CREAM;
        }
        PortraitSettings.drawChamferedBox(batch, cx - w * 0.5f, cy - h * 0.5f, w, h,
                h * 0.22f, Math.max(1.8f, h * 0.032f), border, fill);
        PortraitSettings.drawFittedText(batch, PortraitFonts.header(), label, cx, cy,
                w * 0.84f, h * 0.39f, true, textColor);
    }

    static void map(ConfirmPopup popup, int x, int y) {
        layout.compute(popup);
        Hitbox target = layout.hitYes(x, y) ? popup.yesHb : layout.hitNo(x, y) ? popup.noHb : null;
        InputHelper.mX = target == null ? -10000 : Math.round(target.cX);
        InputHelper.mY = target == null ? -10000 : Math.round(target.cY);
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
