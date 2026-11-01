package tn.nightbeam.betterwitchhuts.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import tn.nightbeam.betterwitchhuts.content.ModContent;

/** Client registrations, called by each loader with its own registration hook. */
public final class WitchboundClient {
    private WitchboundClient() {
    }

    public interface Renderers {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    /** Residents look like the witches players know; their huts and names set them apart. */
    public static void renderers(Renderers renderers) {
        renderers.register(ModContent.COVEN_WITCH.get(), WitchRenderer::new);
    }
}
