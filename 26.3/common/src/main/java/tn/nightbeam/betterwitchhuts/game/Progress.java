package tn.nightbeam.betterwitchhuts.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import tn.nightbeam.betterwitchhuts.core.Objective;

/** Reads an {@link Objective}'s counter from a player's vanilla statistics (or inventory, for deliveries). */
public final class Progress {
    private static final Map<Objective, List<?>> SUBJECTS = new ConcurrentHashMap<>();

    private Progress() {
    }

    /** Called when datapacks (tags) reload. */
    public static void clearCache() {
        SUBJECTS.clear();
    }

    public static int read(ServerPlayer player, Objective objective) {
        ServerStatsCounter stats = player.getStats();
        long total = 0;
        switch (objective.kind()) {
            case CUSTOM -> {
                for (Object id : subjects(objective)) {
                    total += stats.getValue(Stats.CUSTOM.get((Identifier) id));
                }
            }
            case USED -> {
                for (Object item : subjects(objective)) {
                    total += stats.getValue(Stats.ITEM_USED, (Item) item);
                }
            }
            case KILLED -> {
                for (Object type : subjects(objective)) {
                    total += stats.getValue(Stats.ENTITY_KILLED, (EntityType<?>) type);
                }
            }
            case MINED -> {
                for (Object block : subjects(objective)) {
                    total += stats.getValue(Stats.BLOCK_MINED, (Block) block);
                }
            }
            case DELIVER -> total = count(player.getInventory(), objective);
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static int count(Inventory inventory, Objective objective) {
        int n = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && subjects(objective).contains(stack.getItem())) {
                n += stack.getCount();
            }
        }
        return n;
    }

    /** Takes {@code amount} of the objective's items; the caller has checked there are enough. */
    public static void take(Inventory inventory, Objective objective, int amount) {
        int left = amount;
        for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && subjects(objective).contains(stack.getItem())) {
                int used = Math.min(left, stack.getCount());
                stack.shrink(used);
                left -= used;
            }
        }
        inventory.setChanged();
    }

    /** The first item of a delivery, for icons. */
    public static Item firstItem(Objective objective) {
        List<?> list = subjects(objective);
        return !list.isEmpty() && list.get(0) instanceof Item item ? item : null;
    }

    private static List<?> subjects(Objective objective) {
        return SUBJECTS.computeIfAbsent(objective, Progress::resolve);
    }

    private static List<?> resolve(Objective objective) {
        List<Object> out = new ArrayList<>();
        for (String key : objective.keys()) {
            switch (objective.kind()) {
                case CUSTOM -> {
                    Identifier id = parse(key);
                    if (BuiltInRegistries.CUSTOM_STAT.containsKey(id)) {
                        out.add(BuiltInRegistries.CUSTOM_STAT.getValue(id));
                    }
                }
                case USED, DELIVER -> {
                    if (key.equals("#food")) {
                        for (Item item : BuiltInRegistries.ITEM) {
                            if (new ItemStack(item).has(DataComponents.FOOD)) {
                                out.add(item);
                            }
                        }
                    } else {
                        add(out, BuiltInRegistries.ITEM, Registries.ITEM, key);
                    }
                }
                case KILLED -> add(out, BuiltInRegistries.ENTITY_TYPE, Registries.ENTITY_TYPE, key);
                case MINED -> add(out, BuiltInRegistries.BLOCK, Registries.BLOCK, key);
            }
        }
        return List.copyOf(new java.util.LinkedHashSet<>(out));
    }

    private static <T> void add(List<Object> out, Registry<T> registry, ResourceKey<Registry<T>> key, String entry) {
        if (entry.startsWith("#")) {
            for (Holder<T> holder : registry.getTagOrEmpty(TagKey.create(key, parse(entry.substring(1))))) {
                out.add(holder.value());
            }
        } else {
            registry.getOptional(parse(entry)).ifPresent(out::add);
        }
    }

    private static Identifier parse(String key) {
        return key.contains(":") ? Identifier.parse(key) : Identifier.withDefaultNamespace(key);
    }
}
