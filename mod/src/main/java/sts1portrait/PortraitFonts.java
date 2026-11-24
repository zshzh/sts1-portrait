package sts1portrait;

import basemod.ReflectionHacks;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import java.util.HashMap;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

public final class PortraitFonts {
    private static FileHandle regularFile;
    private static FileHandle boldFile;
    private static BitmapFont bodyFont;
    private static BitmapFont headerFont;
    private static float builtScale = -1f;

    private PortraitFonts() {}

    public static void captureRegular() {
        FileHandle current = ReflectionHacks.getPrivateStatic(FontHelper.class, "fontFile");
        if (current != null) regularFile = current;
    }

    public static void captureBold() {
        FileHandle current = ReflectionHacks.getPrivateStatic(FontHelper.class, "fontFile");
        if (current != null) boldFile = current;
        disposeCached();
    }

    private static void disposeCached() {
        if (bodyFont != null) {
            bodyFont.dispose();
            bodyFont = null;
        }
        if (headerFont != null) {
            headerFont.dispose();
            headerFont = null;
        }
        builtScale = -1f;
    }

    @SpirePatch(clz = FontHelper.class, method = "initialize", paramtypez = {})
    public static class CaptureFontFiles {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    if (field.isWriter() && FontHelper.class.getName().equals(field.getClassName())
                            && "charDescFont".equals(field.getFieldName())) {
                        field.replace("{ sts1portrait.PortraitFonts.captureRegular(); $proceed($$); }");
                    }
                }
            };
        }

        @SpirePostfixPatch
        public static void after() {
            captureBold();
        }
    }

    private static void ensureFresh() {
        if (Math.abs(builtScale - Settings.scale) > 0.001f) {
            disposeCached();
            builtScale = Settings.scale;
        }
    }

    public static BitmapFont body() {
        if (!PortraitViewport.active()) return FontHelper.tipBodyFont;
        ensureFresh();
        if (bodyFont == null) {
            bodyFont = generate(regularFile != null ? regularFile : boldFile, 76f, 0f,
                    new Color(0f, 0f, 0f, 0.42f), 2);
            if (bodyFont == null) return FontHelper.tipBodyFont;
        }
        return bodyFont;
    }

    public static BitmapFont header() {
        if (!PortraitViewport.active()) return FontHelper.tipHeaderFont;
        ensureFresh();
        if (headerFont == null) {
            headerFont = generate(boldFile != null ? boldFile : regularFile, 78f,
                    Math.max(1.1f, 1.6f * Settings.scale),
                    new Color(0f, 0f, 0f, 0.56f), 2);
            if (headerFont == null) return FontHelper.tipHeaderFont;
        }
        return headerFont;
    }

    private static BitmapFont generate(FileHandle file, float baseSize, float borderWidth,
            Color shadowColor, int shadowOffset) {
        try {
            if (file == null) file = ReflectionHacks.getPrivateStatic(FontHelper.class, "fontFile");
            if (file == null) return null;
            HashMap<String, FreeTypeFontGenerator> generators =
                    ReflectionHacks.getPrivateStatic(FontHelper.class, "generators");
            FreeTypeFontGenerator gen = generators != null ? generators.get(file.path()) : null;
            if (gen == null) {
                gen = new FreeTypeFontGenerator(file);
                if (generators != null) generators.put(file.path(), gen);
            }
            float fontScale = 1f;
            Float fs = ReflectionHacks.getPrivateStatic(FontHelper.class, "fontScale");
            if (fs != null && fs > 0f) fontScale = fs;
            float size = baseSize * fontScale * Settings.scale;
            if (Settings.BIG_TEXT_MODE) size *= 1.15f;

            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.characters = "";
            p.incremental = true;
            p.size = Math.max(18, Math.round(size));
            p.gamma = 1.15f;
            p.spaceX = 0;
            p.kerning = true;
            p.borderWidth = borderWidth;
            p.borderColor = new Color(0.03f, 0.045f, 0.065f, 0.72f);
            p.borderStraight = false;
            p.shadowOffsetX = Math.max(1, Math.round(shadowOffset * Settings.scale));
            p.shadowOffsetY = Math.max(1, Math.round(shadowOffset * Settings.scale));
            p.shadowColor = shadowColor;
            p.minFilter = Texture.TextureFilter.Linear;
            p.magFilter = Texture.TextureFilter.Linear;

            FreeTypeFontGenerator.FreeTypeBitmapFontData data = new FreeTypeFontGenerator.FreeTypeBitmapFontData();
            data.xChars = new char[] {'x'};
            data.capChars = new char[] {'A', 'H', 'M', 'W'};
            BitmapFont font = gen.generateFont(p, data);
            font.setUseIntegerPositions(false);
            font.getData().markupEnabled = true;
            if (FontHelper.tipBodyFont != null) {
                font.getData().breakChars = FontHelper.tipBodyFont.getData().breakChars;
            }
            return font;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
