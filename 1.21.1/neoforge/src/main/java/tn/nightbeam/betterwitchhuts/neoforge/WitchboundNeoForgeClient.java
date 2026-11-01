package tn.nightbeam.betterwitchhuts.neoforge;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import tn.nightbeam.betterwitchhuts.client.CovenScreen;
import tn.nightbeam.betterwitchhuts.client.WitchboundClient;
import tn.nightbeam.betterwitchhuts.content.ModContent;

final class WitchboundNeoForgeClient {
    private WitchboundNeoForgeClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> WitchboundClient.renderers(new WitchboundClient.Renderers() {
            @Override
            public <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                event.registerEntityRenderer(type, provider);
            }
        }));
        modBus.addListener((RegisterMenuScreensEvent event) -> event.register(ModContent.COVEN_MENU.get(), CovenScreen::new));
    }
}
