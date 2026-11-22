package sts1portrait;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

@SpireInitializer
public final class PortraitMod implements PostInitializeSubscriber {
    private PortraitMod() {
    }

    public static void initialize() {
        BaseMod.subscribe(new PortraitMod());
        System.out.println("[STS1 Portrait] initializer registered");
    }

    @Override
    public void receivePostInitialize() {
        System.out.println("[STS1 Portrait] BaseMod post-initialize reached");
        PortraitViewport.logDimensions();
        PortraitDesktopMode.registerSettings();
    }
}
