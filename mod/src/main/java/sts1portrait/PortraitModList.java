package sts1portrait;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.helpers.TipHelper;
import java.util.StringJoiner;

@SpirePatch(clz = TipHelper.class, method = "renderGenericTip",
        paramtypez = {float.class, float.class, String.class, String.class})
public final class PortraitModList {
    private static ModInfo[] loadedMods;
    private static String loadedNames;

    private PortraitModList() {}

    @SpirePrefixPatch
    public static SpireReturn<Void> before(float x, float y, String title, String body) {
        if (("Mods:".equals(title) || "Mod:".equals(title)) && PortraitCombatHud.topBarActive()
                && Loader.MODINFOS != null && Loader.MODINFOS.length > 0 && names().equals(body)) {
            return SpireReturn.Return();
        }
        return SpireReturn.Continue();
    }

    private static String names() {
        ModInfo[] modules = Loader.MODINFOS;
        if (loadedMods != modules) {
            StringJoiner names = new StringJoiner(" NL ");
            for (ModInfo module : modules) names.add(module.Name);
            loadedNames = names.toString();
            loadedMods = modules;
        }
        return loadedNames;
    }
}
