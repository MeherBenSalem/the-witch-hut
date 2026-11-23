package tn.nightbeam.betterwitchhuts.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import tn.nightbeam.betterwitchhuts.content.ModContent;
import tn.nightbeam.betterwitchhuts.game.LegacyAttributes;

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

    /** End of every client tick: the local player's step height and Raven's gravity (1.20.1, see LegacyAttributes). */
    public static void tick(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.level != null && !minecraft.isPaused()) {
            LegacyAttributes.clientTick(minecraft.player);
        }
    }
}
