package sts1portrait;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireRawPatch;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.screens.compendium.PotionViewScreen;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.Opcode;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class PortraitPotionLibrary {
    private PortraitPotionLibrary() {}

    public static int columns() {
        return PortraitScreens.enabled() ? 8 : 10;
    }

    public static void moveHitbox(Hitbox area, float x, float y) {
        if (PortraitScreens.enabled()) {
            area.width = PortraitViewport.contentWidth() * 0.09f;
            area.height = PortraitViewport.contentWidth() * 0.071f;
        }
        area.move(x, y);
    }

    @SpirePatch(clz = PotionViewScreen.class, method = "renderList",
            paramtypez = {SpriteBatch.class, String.class, String.class, ArrayList.class})
    public static class GridGeometry {
        @SpireRawPatch
        public static void setColumns(CtBehavior method) throws Exception {
            CodeAttribute code = method.getMethodInfo().getCodeAttribute();
            CodeIterator bytecode = code.iterator();
            while (bytecode.hasNext()) {
                int index = bytecode.next();
                if (bytecode.byteAt(index) != Opcode.BIPUSH || bytecode.byteAt(index + 1) != 10
                        || bytecode.byteAt(index + 2) != Opcode.IF_ICMPNE) continue;
                Bytecode call = new Bytecode(method.getMethodInfo().getConstPool());
                call.addInvokestatic(PortraitPotionLibrary.class.getName(), "columns", "()I");
                bytecode.writeByte(Opcode.NOP, index);
                bytecode.writeByte(Opcode.NOP, index + 1);
                bytecode.insertAt(index, call.get());
                method.getMethodInfo().rebuildStackMap(method.getDeclaringClass().getClassPool());
                return;
            }
        }

        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess field) throws CannotCompileException {
                    String name = field.getFieldName();
                    if (field.isWriter() && "posX".equals(name)) {
                        field.replace("{ $proceed(sts1portrait.PortraitScreens.enabled()"
                                + " ? sts1portrait.PortraitViewport.x(0.095f + col * 0.102f) : $1); }");
                    } else if (field.isReader() && "SPACE".equals(name)) {
                        field.replace("{ $_ = sts1portrait.PortraitScreens.enabled()"
                                + " ? sts1portrait.PortraitViewport.contentWidth() * 0.085f : $proceed(); }");
                    } else if (field.isReader() && "START_X".equals(name)) {
                        field.replace("{ $_ = sts1portrait.PortraitScreens.enabled()"
                                + " ? sts1portrait.PortraitViewport.x(0.082f) : $proceed(); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = PotionViewScreen.class, method = "updateList", paramtypez = {ArrayList.class})
    public static class TouchArea {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (Hitbox.class.getName().equals(call.getClassName()) && "move".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitPotionLibrary.moveHitbox($0, $$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = AbstractPotion.class, method = "labRender", paramtypez = {SpriteBatch.class})
    public static class Icons {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (SpriteBatch.class.getName().equals(call.getClassName()) && "draw".equals(call.getMethodName())
                            && "(Lcom/badlogic/gdx/graphics/Texture;FFFFFFFFFIIIIZZ)V".equals(call.getSignature())) {
                        call.replace("{ if (sts1portrait.PortraitScreens.enabled()) { $8 *= 1.65f; $9 *= 1.65f; } $proceed($$); }");
                    }
                }
            };
        }
    }
}
