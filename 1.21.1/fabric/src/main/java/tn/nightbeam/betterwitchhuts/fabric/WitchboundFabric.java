package tn.nightbeam.betterwitchhuts.fabric;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import tn.nightbeam.betterwitchhuts.Witchbound;
import tn.nightbeam.betterwitchhuts.command.WitchboundCommand;
import tn.nightbeam.betterwitchhuts.content.ModContent;
import tn.nightbeam.betterwitchhuts.game.Game;
import tn.nightbeam.betterwitchhuts.game.Progress;
import tn.nightbeam.betterwitchhuts.platform.Platform;
import tn.nightbeam.betterwitchhuts.platform.Registrar;

public final class WitchboundFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Witchbound.init(new FabricPlatform(), new FabricRegistrar());
        FabricDefaultAttributeRegistry.register(ModContent.COVEN_WITCH.get(), Witch.createAttributes());
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> ModContent.tabItems().forEach(entries::accept));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> WitchboundCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(Game::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(Game::serverStopping);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> Progress.clearCache());
        ServerTickEvents.END_SERVER_TICK.register(Game::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Game.join(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Game.leave(handler.player));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(Game::allowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> Game.afterDamage(entity, source, taken));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> Game.allowDeath(entity, source));
        ServerLivingEntityEvents.AFTER_DEATH.register(Game::afterDeath);
    }

    private static final class FabricPlatform implements Platform {
        @Override
        public Path configDir() {
            return FabricLoader.getInstance().getConfigDir();
        }

        @Override
        public <T extends AbstractContainerMenu> MenuType<T> menuType(MenuFactory<T> factory) {
            return new MenuType<>(factory::create, FeatureFlags.VANILLA_SET);
        }
    }

    private static final class FabricRegistrar implements Registrar {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String path, Function<ResourceLocation, T> factory) {
            ResourceLocation id = Witchbound.id(path);
            Registry<R> target = (Registry<R>) BuiltInRegistries.REGISTRY.getOrThrow((ResourceKey) registry);
            T value = factory.apply(id);
            Registry.register(target, id, value);
            return () -> value;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <R> Holder<R> holder(ResourceKey<? extends Registry<R>> registry, String path, Function<ResourceLocation, R> factory) {
            ResourceLocation id = Witchbound.id(path);
            Registry<R> target = (Registry<R>) BuiltInRegistries.REGISTRY.getOrThrow((ResourceKey) registry);
            return Registry.registerForHolder(target, id, factory.apply(id));
        }
    }
}
