package tn.nightbeam.betterwitchhuts.core;

import java.util.List;
import java.util.Locale;
import tn.nightbeam.betterwitchhuts.core.Objective.Kind;

/** Errands a resident witch hands out: one at a time per player, handed in at any resident. */
public enum Errand {
    SPIDER_EYES(Kind.DELIVER, List.of("spider_eye"), 6, 8, 5),
    TOADSTOOLS(Kind.DELIVER, List.of("red_mushroom", "brown_mushroom"), 8, 8, 4),
    BONES(Kind.DELIVER, List.of("bone"), 12, 8, 5),
    FEATHERS(Kind.DELIVER, List.of("feather"), 8, 8, 4),
    GLOW_BERRIES(Kind.DELIVER, List.of("glow_berries"), 6, 10, 6),
    INK(Kind.DELIVER, List.of("ink_sac", "glow_ink_sac"), 4, 8, 5),
    RABBIT_FOOT(Kind.DELIVER, List.of("rabbit_foot"), 1, 12, 8),
    MEMBRANES(Kind.DELIVER, List.of("phantom_membrane"), 2, 12, 8),
    SLIME(Kind.DELIVER, List.of("slime_ball"), 6, 10, 6),
    PEARLS(Kind.DELIVER, List.of("ender_pearl"), 2, 12, 8),
    HONEY(Kind.DELIVER, List.of("honey_bottle"), 2, 10, 6),
    AMETHYST(Kind.DELIVER, List.of("amethyst_shard"), 6, 10, 6),
    DROWNED(Kind.KILLED, List.of("drowned"), 5, 12, 8),
    RESTLESS_DEAD(Kind.KILLED, List.of("zombie", "husk", "zombie_villager"), 8, 10, 6),
    CREEPERS(Kind.KILLED, List.of("creeper"), 4, 10, 7),
    SPIDERS(Kind.KILLED, List.of("spider", "cave_spider"), 6, 10, 6),
    ENDERMEN(Kind.KILLED, List.of("enderman"), 2, 14, 10),
    ILLAGERS(Kind.KILLED, List.of("pillager", "vindicator", "evoker", "illusioner"), 4, 15, 12),
    SLIMES(Kind.KILLED, List.of("slime"), 6, 10, 6),
    FISHING(Kind.CUSTOM, List.of("fish_caught"), 6, 8, 5);

    private final Objective objective;
    private final int standing;
    private final int emeralds;

    Errand(Kind kind, List<String> keys, int target, int standing, int emeralds) {
        this.objective = new Objective("errand_" + name().toLowerCase(Locale.ROOT), kind, keys, target);
        this.standing = standing;
        this.emeralds = emeralds;
    }

    public Objective objective() {
        return objective;
    }

    /** Standing gained when handed in. */
    public int standing() {
        return standing;
    }

    public int emeralds() {
        return emeralds;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static int index(Errand errand) {
        return errand == null ? -1 : errand.ordinal();
    }

    public static Errand byIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : null;
    }

    public static Errand byId(String id) {
        for (Errand errand : values()) {
            if (errand.id().equals(id)) {
                return errand;
            }
        }
        return null;
    }
}
