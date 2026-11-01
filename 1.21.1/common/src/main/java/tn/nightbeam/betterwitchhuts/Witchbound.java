package tn.nightbeam.betterwitchhuts;

import net.minecraft.resources.ResourceLocation;
import tn.nightbeam.betterwitchhuts.content.ModContent;
import tn.nightbeam.betterwitchhuts.core.WitchConfig;
import tn.nightbeam.betterwitchhuts.platform.Platform;
import tn.nightbeam.betterwitchhuts.platform.Registrar;

/** Common entry point, called by each loader's mod class. */
public final class Witchbound {
    private Witchbound() {
    }

    public static void init(Platform platform, Registrar registrar) {
        WitchConfig.load(platform.configDir());
        ModContent.register(registrar, platform);
        Constants.LOG.info("{} ready: {} huts enabled", Constants.MOD_NAME,
                WitchConfig.STRUCTURES.stream().filter(WitchConfig.get()::structureEnabled).count());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
