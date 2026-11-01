package tn.nightbeam.betterwitchhuts.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import tn.nightbeam.betterwitchhuts.client.CovenScreen;
import tn.nightbeam.betterwitchhuts.client.WitchboundClient;
import tn.nightbeam.betterwitchhuts.content.ModContent;

public final class WitchboundFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WitchboundClient.renderers(new WitchboundClient.Renderers() {
            @Override
            public <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                EntityRendererRegistry.register(type, provider);
            }
        });
        MenuScreens.register(ModContent.COVEN_MENU.get(), CovenScreen::new);
    }
}
