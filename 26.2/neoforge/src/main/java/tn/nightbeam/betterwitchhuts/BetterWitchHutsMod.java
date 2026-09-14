package tn.nightbeam.betterwitchhuts;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class BetterWitchHutsMod {

    public BetterWitchHutsMod(IEventBus eventBus) {
        BetterWitchHuts.init();
    }
}
