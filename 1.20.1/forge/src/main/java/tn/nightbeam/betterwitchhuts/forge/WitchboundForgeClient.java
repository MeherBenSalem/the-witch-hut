package tn.nightbeam.betterwitchhuts.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import tn.nightbeam.betterwitchhuts.client.CovenScreen;
import tn.nightbeam.betterwitchhuts.client.WitchboundClient;
import tn.nightbeam.betterwitchhuts.content.ModContent;

final class WitchboundForgeClient {
    private WitchboundForgeClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> WitchboundClient.renderers(new WitchboundClient.Renderers() {
            @Override
            public <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                event.registerEntityRenderer(type, provider);
            }
        }));
        // Forge 1.20.1 has no menu screen event: screens are registered during client setup.
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> MenuScreens.register(ModContent.COVEN_MENU.get(), CovenScreen::new)));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                WitchboundClient.tick(Minecraft.getInstance());
            }
        });
    }
}
