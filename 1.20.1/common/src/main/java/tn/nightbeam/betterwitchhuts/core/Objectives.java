package tn.nightbeam.betterwitchhuts.core;

import java.util.List;
import tn.nightbeam.betterwitchhuts.core.Objective.Kind;

/** Every objective the mod uses, with a stable index so it can travel in a menu data slot. */
public final class Objectives {
    // Hex tasks
    public static final Objective SWIM = new Objective("swim", Kind.CUSTOM, List.of("swim_one_cm"), 15000, 100);
    public static final Objective SPRINT = new Objective("sprint", Kind.CUSTOM, List.of("sprint_one_cm"), 60000, 100);
    public static final Objective EAT = new Objective("eat", Kind.USED, List.of("#food"), 10);
    public static final Objective FISH = new Objective("fish", Kind.CUSTOM, List.of("fish_caught"), 5);
    public static final Objective RING_BELLS = new Objective("ring_bells", Kind.CUSTOM, List.of("bell_ring"), 3);
    public static final Objective TRADE = new Objective("trade", Kind.CUSTOM, List.of("traded_with_villager"), 3);
    public static final Objective SLAY_SKELETONS = new Objective("slay_skeletons", Kind.KILLED,
            List.of("skeleton", "stray", "bogged", "wither_skeleton", "parched"), 3);
    public static final Objective SLAY_ANY = new Objective("slay_any", Kind.CUSTOM, List.of("mob_kills"), 12);
    public static final Objective SLAY_ZOMBIES = new Objective("slay_zombies", Kind.KILLED,
            List.of("zombie", "husk", "drowned", "zombie_villager"), 6);
    public static final Objective PLACE_CANDLES = new Objective("place_candles", Kind.USED, List.of("#minecraft:candles"), 6);
    public static final Objective SLAY_PHANTOMS = new Objective("slay_phantoms", Kind.KILLED, List.of("phantom"), 2);
    public static final Objective BREED = new Objective("breed", Kind.CUSTOM, List.of("animals_bred"), 3);
    public static final Objective GOLDEN_APPLE = new Objective("golden_apple", Kind.USED,
            List.of("golden_apple", "enchanted_golden_apple"), 1);
    public static final Objective MINE_STONE = new Objective("mine_stone", Kind.MINED,
            List.of("stone", "deepslate", "andesite", "diorite", "granite", "tuff", "calcite"), 48);
    public static final Objective MINE_LOGS = new Objective("mine_logs", Kind.MINED, List.of("#minecraft:logs"), 24);

    /** Hex tasks first, then {@link Errand#objective()}s, in a fixed order. */
    private static final List<Objective> ALL;

    static {
        java.util.ArrayList<Objective> all = new java.util.ArrayList<>(List.of(SWIM, SPRINT, EAT, FISH, RING_BELLS, TRADE,
                SLAY_SKELETONS, SLAY_ANY, SLAY_ZOMBIES, PLACE_CANDLES, SLAY_PHANTOMS, BREED, GOLDEN_APPLE, MINE_STONE, MINE_LOGS));
        for (Errand errand : Errand.values()) {
            all.add(errand.objective());
        }
        ALL = List.copyOf(all);
    }

    private Objectives() {
    }

    public static List<Objective> all() {
        return ALL;
    }

    public static int index(Objective objective) {
        return objective == null ? -1 : ALL.indexOf(objective);
    }

    public static Objective byIndex(int index) {
        return index >= 0 && index < ALL.size() ? ALL.get(index) : null;
    }

    public static Objective byId(String id) {
        for (Objective objective : ALL) {
            if (objective.id().equals(id)) {
                return objective;
            }
        }
        return null;
    }
}
