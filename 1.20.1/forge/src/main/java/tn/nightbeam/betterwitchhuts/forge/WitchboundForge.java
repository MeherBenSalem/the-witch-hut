package tn.nightbeam.betterwitchhuts.forge;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.server.ServerLifecycleHooks;
import tn.nightbeam.betterwitchhuts.Constants;
import tn.nightbeam.betterwitchhuts.Witchbound;
import tn.nightbeam.betterwitchhuts.command.WitchboundCommand;
import tn.nightbeam.betterwitchhuts.content.ModContent;
import tn.nightbeam.betterwitchhuts.game.Game;
import tn.nightbeam.betterwitchhuts.game.LegacyAttributes;
import tn.nightbeam.betterwitchhuts.game.Progress;
import tn.nightbeam.betterwitchhuts.platform.Platform;
import tn.nightbeam.betterwitchhuts.platform.Registrar;

@Mod(Constants.MOD_ID)
public final class WitchboundForge {
    private final Map<ResourceKey<?>, DeferredRegister<?>> registers = new HashMap<>();

    public WitchboundForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        Witchbound.init(new Platform() {
            @Override
            public Path configDir() {
                return FMLPaths.CONFIGDIR.get();
            }

            @Override
            public <T extends AbstractContainerMenu> MenuType<T> menuType(MenuFactory<T> factory) {
                return IForgeMenuType.create((containerId, inventory, data) -> factory.create(containerId, inventory));
            }
        }, new Registrar() {
            @Override
            public <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String path, Function<ResourceLocation, T> factory) {
                ResourceLocation id = Witchbound.id(path);
                return deferred(registry).register(path, () -> factory.apply(id));
            }
        });
        registers.values().forEach(deferred -> deferred.register(modBus));
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(ModContent.COVEN_WITCH.get(), Witch.createAttributes().build()));
        modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                ModContent.tabItems().forEach(event::accept);
            }
        });
        // Raven and Clumsy Fingers change reach through Forge's own attributes (1.20.1 has none of its own).
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(
                () -> ModContent.linkReach(ForgeMod.BLOCK_REACH.get(), ForgeMod.ENTITY_REACH.get())));
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> WitchboundCommand.register(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> Game.serverStarted(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> Game.serverStopping(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> Progress.clearCache());
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (event.phase == TickEvent.Phase.END && server != null) {
                Game.tick(server);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                Game.join(player);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                Game.leave(player);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((LivingAttackEvent event) -> {
            if (!event.getEntity().level().isClientSide() && !Game.allowDamage(event.getEntity(), event.getSource(), event.getAmount())) {
                event.setCanceled(true);
            }
        });
        // LivingDamageEvent carries the damage left after armor and absorption, just before it comes off the health.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (LivingDamageEvent event) -> {
            if (!event.getEntity().level().isClientSide() && event.getAmount() > 0) {
                Game.afterDamage(event.getEntity(), event.getSource(), event.getAmount());
            }
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, (LivingDeathEvent event) -> {
            if (!event.getEntity().level().isClientSide() && !Game.allowDeath(event.getEntity(), event.getSource())) {
                event.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (LivingDeathEvent event) -> {
            if (!event.getEntity().level().isClientSide()) {
                Game.afterDeath(event.getEntity(), event.getSource());
            }
        });
        // Mining speed of Clumsy Fingers and the Moth pact (fired on both sides).
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.BreakSpeed event) -> {
            float factor = LegacyAttributes.miningFactor(event.getEntity());
            if (factor != 1.0F) {
                event.setNewSpeed(event.getNewSpeed() * factor);
            }
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            WitchboundForgeClient.init(modBus);
        }
    }

    @SuppressWarnings("unchecked")
    private <R> DeferredRegister<R> deferred(ResourceKey<? extends Registry<R>> registry) {
        return (DeferredRegister<R>) registers.computeIfAbsent(registry,
                key -> DeferredRegister.create((ResourceKey<? extends Registry<R>>) key, Constants.MOD_ID));
    }
}
