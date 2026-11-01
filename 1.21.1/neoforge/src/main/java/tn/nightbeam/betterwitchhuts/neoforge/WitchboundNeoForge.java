package tn.nightbeam.betterwitchhuts.neoforge;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import tn.nightbeam.betterwitchhuts.Constants;
import tn.nightbeam.betterwitchhuts.Witchbound;
import tn.nightbeam.betterwitchhuts.command.WitchboundCommand;
import tn.nightbeam.betterwitchhuts.content.ModContent;
import tn.nightbeam.betterwitchhuts.game.Game;
import tn.nightbeam.betterwitchhuts.game.Progress;
import tn.nightbeam.betterwitchhuts.platform.Platform;
import tn.nightbeam.betterwitchhuts.platform.Registrar;

@Mod(Constants.MOD_ID)
public final class WitchboundNeoForge {
    private static final ResourceKey<CreativeModeTab> TOOLS = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            ResourceLocation.withDefaultNamespace("tools_and_utilities"));

    private final Map<ResourceKey<?>, DeferredRegister<?>> registers = new HashMap<>();

    public WitchboundNeoForge(IEventBus modBus, Dist dist) {
        Witchbound.init(new Platform() {
            @Override
            public Path configDir() {
                return FMLPaths.CONFIGDIR.get();
            }

            @Override
            public <T extends AbstractContainerMenu> MenuType<T> menuType(MenuFactory<T> factory) {
                return new MenuType<>(factory::create, FeatureFlags.VANILLA_SET);
            }
        }, new Registrar() {
            @Override
            public <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String path, Function<ResourceLocation, T> factory) {
                return deferred(registry).register(path, factory);
            }

            @Override
            public <R> Holder<R> holder(ResourceKey<? extends Registry<R>> registry, String path, Function<ResourceLocation, R> factory) {
                return deferred(registry).register(path, factory);
            }
        });
        registers.values().forEach(deferred -> deferred.register(modBus));
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(ModContent.COVEN_WITCH.get(), Witch.createAttributes().build()));
        modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == TOOLS) {
                ModContent.tabItems().forEach(event::accept);
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> WitchboundCommand.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> Game.serverStarted(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> Game.serverStopping(event.getServer()));
        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> Progress.clearCache());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> Game.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                Game.join(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                Game.leave(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent event) -> {
            if (!event.getEntity().level().isClientSide() && !Game.allowDamage(event.getEntity(), event.getSource(), event.getAmount())) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingDamageEvent.Post event) -> {
            if (!event.getEntity().level().isClientSide()) {
                Game.afterDamage(event.getEntity(), event.getSource(), event.getNewDamage());
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (LivingDeathEvent event) -> {
            if (!event.getEntity().level().isClientSide() && !Game.allowDeath(event.getEntity(), event.getSource())) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (LivingDeathEvent event) -> {
            if (!event.getEntity().level().isClientSide()) {
                Game.afterDeath(event.getEntity(), event.getSource());
            }
        });
        if (dist.isClient()) {
            WitchboundNeoForgeClient.init(modBus);
        }
    }

    @SuppressWarnings("unchecked")
    private <R> DeferredRegister<R> deferred(ResourceKey<? extends Registry<R>> registry) {
        return (DeferredRegister<R>) registers.computeIfAbsent(registry,
                key -> DeferredRegister.create((ResourceKey<? extends Registry<R>>) key, Constants.MOD_ID));
    }
}
