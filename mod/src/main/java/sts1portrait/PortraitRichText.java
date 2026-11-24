package sts1portrait;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PortraitRichText {
    // Treat brackets enclosing a choice as text so the color tokens inside remain visible to the parser.
    private static final Pattern MARKUP = Pattern.compile(
            "(?<![^\\s\\[])(NL|TAB)(?=[\\s\\]]|$)"
            + "|(?<![^\\s\\[])#([rgbyp])([^\\s\\[\\]]+|\\[[^\\s\\[\\]]+\\])"
            + "|\\[([^\\[\\]\\s#]*|#[0-9a-fA-F]{6}(?:[0-9a-fA-F]{2})?)\\]");
    private static final Pattern HEX = Pattern.compile("#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})");
    private static final String ICON_CODES = "ERGBWCPTS";
    private static final char FIRST_ICON = '\uE000';
    private static final BitmapFont.Glyph[] placeholders = new BitmapFont.Glyph[ICON_CODES.length()];
    private static final BitmapFont.Glyph[] previousGlyphs = new BitmapFont.Glyph[ICON_CODES.length()];
    private static final GlyphLayout layout = new GlyphLayout();
    private static final String COLOR_TAGS = "(?:\\[(?:#[0-9a-fA-F]{6,8}|[A-Za-z_]+)?\\])*";
    private static final Pattern BEFORE_CN_NUMBER = Pattern.compile(
            "([\\p{IsHan}：])(" + COLOR_TAGS + ")[ \\t\\u3000]+(" + COLOR_TAGS + ")(?=[+−-]?[0-9])");
    private static final Pattern AFTER_CN_NUMBER = Pattern.compile(
            "([0-9%％])(" + COLOR_TAGS + ")[ \\t\\u3000]+(" + COLOR_TAGS
                    + ")(?=[\\p{IsHan}，。！？、；：）])");
    private final boolean eventSpacing;

    public PortraitRichText() { this(false); }
    PortraitRichText(boolean eventSpacing) { this.eventSpacing = eventSpacing; }

    private static final Pattern BETWEEN_CN_COLOR = Pattern.compile(
            "([\\p{IsHan}，。！？、；：）])(" + COLOR_TAGS + ")[ \\t\\u3000]+(" + COLOR_TAGS
                    + ")(?=[\\p{IsHan}，。！？、；：）])");

    static String tightenEventNumbers(String text) {
        String s = AFTER_CN_NUMBER.matcher(BEFORE_CN_NUMBER.matcher(text).replaceAll("$1$2$3")).replaceAll("$1$2$3");
        return BETWEEN_CN_COLOR.matcher(s).replaceAll("$1$2$3");
    }

    private String source;
    private String formatted;
    private int icons;

    static {
        for (int i = 0; i < placeholders.length; i++) {
            BitmapFont.Glyph glyph = new BitmapFont.Glyph();
            glyph.id = FIRST_ICON + i;
            placeholders[i] = glyph;
        }
    }

    void draw(SpriteBatch batch, BitmapFont font, String text, Color color, float x, float y,
            float width, float height, float lineHeight, int alignment) {
        draw(batch, font, text, color, x, y, width, height, lineHeight, alignment, AbstractDungeon.player);
    }

    void draw(SpriteBatch batch, BitmapFont font, String text, Color color, float x, float y,
            float width, float height, float lineHeight, int alignment, AbstractPlayer character) {
        draw(batch, font, text, color, x, y, width, height, lineHeight, alignment, character, false);
    }

    void drawTop(SpriteBatch batch, BitmapFont font, String text, Color color, float x, float topY,
            float width, float height, float lineHeight, int alignment, AbstractPlayer character) {
        draw(batch, font, text, color, x, topY, width, height, lineHeight, alignment, character, true);
    }

    private void draw(SpriteBatch batch, BitmapFont font, String text, Color color, float x, float y,
            float width, float height, float lineHeight, int alignment, AbstractPlayer character, boolean topAligned) {
        if (text == null || text.isEmpty()) return;
        if (!text.equals(source)) {
            source = text;
            formatted = format(text);
            if (eventSpacing) formatted = tightenEventNumbers(formatted);
            icons = 0;
            for (int i = 0; i < formatted.length(); i++) {
                int icon = formatted.charAt(i) - FIRST_ICON;
                if (icon >= 0 && icon < placeholders.length) icons |= 1 << icon;
            }
        }
        BitmapFont.BitmapFontData data = font.getData();
        float oldX = data.scaleX;
        float oldY = data.scaleY;
        boolean markup = data.markupEnabled;
        float fit = lineHeight / Math.max(1f, font.getCapHeight());
        installIcons(data, Math.max(1, Math.round(font.getCapHeight() / oldY)));
        data.markupEnabled = true;
        data.setScale(oldX * fit, oldY * fit);
        try {
            layout.setText(font, formatted, color, width, Align.left, true);
            if (layout.height > height || layout.width > width) {
                fit *= Math.min(height / Math.max(layout.height, height), width / Math.max(layout.width, width));
                data.setScale(oldX * fit, oldY * fit);
                layout.setText(font, formatted, color, width, Align.left, true);
            }
            if (alignment == Align.center) centerLines(width, data.scaleX);
            float top = topAligned ? y : y + layout.height * .5f;
            font.draw(batch, layout, x, top);
            if (icons != 0) drawIcons(batch, font, x, top, character);
        } finally {
            data.markupEnabled = markup;
            data.setScale(oldX, oldY);
            for (int i = 0; i < placeholders.length; i++) {
                if ((icons & (1 << i)) == 0) continue;
                data.setGlyph(FIRST_ICON + i, previousGlyphs[i]);
                previousGlyphs[i] = null;
            }
        }
    }

    private void installIcons(BitmapFont.BitmapFontData data, int cap) {
        for (int i = 0; i < placeholders.length; i++) {
            if ((icons & (1 << i)) == 0) continue;
            BitmapFont.Glyph glyph = placeholders[i];
            glyph.width = cap;
            glyph.xadvance = cap + Math.max(2, cap / 5);
            previousGlyphs[i] = data.getGlyph((char) glyph.id);
            data.setGlyph(glyph.id, glyph);
        }
    }

    private static void drawIcons(SpriteBatch batch, BitmapFont font, float x, float top, AbstractPlayer character) {
        BitmapFont.BitmapFontData data = font.getData();
        BitmapFont.Glyph cap = data.getGlyph('H');
        float capBottom = cap == null ? -font.getCapHeight() : cap.yoffset * data.scaleY;
        float size = font.getCapHeight();
        float packedColor = batch.getPackedColor();
        try {
            for (GlyphLayout.GlyphRun run : layout.runs) {
                float pen = x + run.x;
                for (int i = 0; i < run.glyphs.size; i++) {
                    BitmapFont.Glyph glyph = run.glyphs.get(i);
                    pen += run.xAdvances.get(i);
                    int icon = glyph.id - FIRST_ICON;
                    if (icon < 0 || icon >= placeholders.length || glyph != placeholders[icon]) continue;
                    TextureRegion region = iconRegion(ICON_CODES.charAt(icon), character);
                    if (region == null) continue;
                    batch.setColor(1f, 1f, 1f, run.color.a);
                    batch.draw(region, pen, top + data.ascent + run.y + capBottom, size, size);
                }
            }
        } finally {
            batch.setColor(packedColor);
        }
    }

    private static TextureRegion iconRegion(char code, AbstractPlayer character) {
        switch (code) {
            case 'E': return character == null ? AbstractCard.orb_red : character.getOrb();
            case 'R': return AbstractCard.orb_red;
            case 'G': return AbstractCard.orb_green;
            case 'B': return AbstractCard.orb_blue;
            case 'W': return AbstractCard.orb_purple;
            case 'C': return AbstractCard.orb_card;
            case 'P': return AbstractCard.orb_potion;
            case 'T': return AbstractCard.orb_relic;
            case 'S': return AbstractCard.orb_special;
            default: return null;
        }
    }

    private static void centerLines(float width, float scale) {
        int first = 0;
        while (first < layout.runs.size) {
            float baseline = layout.runs.get(first).y;
            float left = Float.POSITIVE_INFINITY;
            float right = Float.NEGATIVE_INFINITY;
            int next = first;
            while (next < layout.runs.size && layout.runs.get(next).y == baseline) {
                GlyphLayout.GlyphRun run = layout.runs.get(next++);
                float pen = run.x;
                for (int i = 0; i < run.glyphs.size; i++) {
                    BitmapFont.Glyph glyph = run.glyphs.get(i);
                    pen += run.xAdvances.get(i);
                    if (glyph.width == 0) continue;
                    if (glyph.height == 0 && (glyph.id < FIRST_ICON || glyph.id >= FIRST_ICON + placeholders.length)) continue;
                    left = Math.min(left, pen + glyph.xoffset * scale);
                    right = Math.max(right, pen + (glyph.xoffset + glyph.width) * scale);
                }
            }
            if (left <= right) {
                float offset = (width - left - right) * .5f;
                for (int i = first; i < next; i++) layout.runs.get(i).x += offset;
            }
            first = next;
        }
    }

    static String format(String text) {
        Matcher tokens = MARKUP.matcher(text);
        StringBuilder output = new StringBuilder(text.length() + 24);
        int end = 0;
        while (tokens.find()) {
            literal(output, text, end, tokens.start());
            String control = tokens.group(1);
            String color = tokens.group(2);
            String tag = tokens.group(4);
            if (control != null) {
                if ("NL".equals(control)) {
                    output.append('\n');
                    end = tokens.end();
                    while (end < text.length() && text.charAt(end) == ' ') end++;
                    continue;
                }
                output.append("    ");
            } else if (color != null) {
                output.append("[#").append(colorCode(color.charAt(0))).append(']');
                if (!appendIcon(output, tokens.group(3))) literal(output, text, tokens.start(3), tokens.end(3));
                output.append("[]");
            } else if (appendIcon(output, tokens.group())) {
                // Inline icons use a measured placeholder and are drawn over it after the text pass.
            } else if (tag.isEmpty() || HEX.matcher(tag).matches() || Colors.get(tag) != null) {
                output.append(tokens.group());
            } else {
                literal(output, text, tokens.start(), tokens.end());
            }
            end = tokens.end();
        }
        literal(output, text, end, text.length());
        return output.toString();
    }

    private static boolean appendIcon(StringBuilder output, String token) {
        if (token.length() != 3 || token.charAt(0) != '[' || token.charAt(2) != ']') return false;
        int icon = ICON_CODES.indexOf(token.charAt(1));
        if (icon < 0) return false;
        output.append((char) (FIRST_ICON + icon));
        return true;
    }

    public static String separateNativeIcons(String text) {
        if (!PortraitViewport.active()) return text;
        StringBuilder result = null;
        int copied = 0;
        for (int start = text.indexOf('['); start >= 0 && start + 2 < text.length();
                start = text.indexOf('[', start + 1)) {
            if (text.charAt(start + 2) != ']' || ICON_CODES.indexOf(text.charAt(start + 1)) < 0) continue;
            int end = start + 3;
            boolean hasColor = start >= 2 && text.charAt(start - 2) == '#'
                    && "rgbyp".indexOf(text.charAt(start - 1)) >= 0
                    && (start == 2 || Character.isWhitespace(text.charAt(start - 3)));
            boolean before = start > 0 && !Character.isWhitespace(text.charAt(start - 1));
            boolean after = end < text.length() && !Character.isWhitespace(text.charAt(end));
            if (!before && !after) continue;
            if (result == null) result = new StringBuilder(text.length() + 8);
            result.append(text, copied, hasColor ? start - 2 : start);
            if (result.length() > 0 && !Character.isWhitespace(result.charAt(result.length() - 1))) result.append(' ');
            result.append(text, start, end);
            if (after) result.append(' ');
            copied = end;
        }
        return result == null ? text : result.append(text, copied, text.length()).toString();
    }

    // The native tooltip layout recognizes icons only when each one is a complete space-delimited token.
    @SpirePatch(clz = FontHelper.class, method = "renderSmartText",
            paramtypez = {SpriteBatch.class, BitmapFont.class, String.class, float.class, float.class,
                    float.class, float.class, Color.class})
    @SpirePatch(clz = FontHelper.class, method = "exampleNonWordWrappedText")
    @SpirePatch(clz = FontHelper.class, method = "getSmartHeight")
    @SpirePatch(clz = FontHelper.class, method = "getHeightForCharLineBreak")
    @SpirePatch(clz = FontHelper.class, method = "getSmartWidth",
            paramtypez = {BitmapFont.class, String.class, float.class, float.class})
    public static class NativeIconTokens {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (String.class.getName().equals(call.getClassName()) && "split".equals(call.getMethodName())) {
                        call.replace("{ $_ = sts1portrait.PortraitRichText.separateNativeIcons($0).split($$); }");
                    }
                }
            };
        }
    }

    private static void literal(StringBuilder output, String text, int start, int end) {
        for (int i = start; i < end; i++) {
            char character = text.charAt(i);
            if (character == '[') output.append('[');
            output.append(character);
        }
    }

    private static String colorCode(char code) {
        switch (code) {
            case 'g': return "7fff48";
            case 'r': return "ff6961";
            case 'b': return "74c9ff";
            case 'p': return "c881ff";
            default: return "f5d06b";
        }
    }
}
