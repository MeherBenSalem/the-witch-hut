package tn.nightbeam.betterwitchhuts.platform;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Registers content the loader's way: Fabric registers immediately, NeoForge through deferred registers. The returned
 * supplier or holder is only valid once the registry has been populated.
 */
public interface Registrar {
    <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String path, Function<Identifier, T> factory);

    /** For registries whose entries are passed around as holders (mob effects). */
    <R> Holder<R> holder(ResourceKey<? extends Registry<R>> registry, String path, Function<Identifier, R> factory);
}
