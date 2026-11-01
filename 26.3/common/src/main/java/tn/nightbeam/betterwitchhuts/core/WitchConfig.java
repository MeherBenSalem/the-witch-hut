package tn.nightbeam.betterwitchhuts.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** {@code config/better_witch_huts.json}. Read once at startup; changes apply after a restart. */
public final class WitchConfig {
    public static final String FILE = "better_witch_huts.json";
    /** Every hut, in the order the docs list them. {@code witch_hut} is the 1.0 hut, id unchanged. */
    public static final List<String> STRUCTURES = List.of("witch_hut", "bog_hut", "mangrove_roost", "hedge_cottage",
            "nightshade_circle", "rime_hut", "bone_hut");

    private static volatile Values values = Values.defaults();

    private WitchConfig() {
    }

    public record StandingRules(int killWitch, int killResident, int hurtResident, int breakHexTask, int renounce,
                                int abandonErrand, int offeringDailyCap) {
    }

    public record Hexing(boolean wildWitches, int range, int cooldownMin, int cooldownMax, double chance, double nightBonus,
                         double fullMoonBonus, int maxHexes, boolean residentGrudge, Set<String> disabled) {
    }

    public record Services(Map<Standing.Tier, Integer> liftCost, int renounceCost, int witchBottleCost, int poppetCost) {
    }

    public record Values(Map<Standing.Tier, Integer> pactSlots, Set<String> disabledPacts, StandingRules standing,
                         Hexing hexing, Services services, boolean kinPeace, Map<String, Boolean> structures) {
        public static Values defaults() {
            return read(spec().defaults());
        }

        public boolean structureEnabled(String id) {
            return structures.getOrDefault(id, true);
        }

        public int pactSlots(Standing.Tier tier) {
            return pactSlots.getOrDefault(tier, 0);
        }
    }

    public static CommentedJson spec() {
        JsonObject structures = new JsonObject();
        STRUCTURES.forEach(id -> structures.addProperty(id, true));
        JsonObject root = Json.obj(
                "pacts", Json.obj(
                        "slots", Json.obj("hunted", 0, "distrusted", 0, "stranger", 1, "known", 1, "trusted", 2, "kin", 3),
                        "renounce_cost", 24,
                        "disabled", new JsonArray()),
                "hexes", Json.obj(
                        "wild_witches_cast", true,
                        "cast_range", 16,
                        "cooldown_seconds", Json.obj("min", 14, "max", 26),
                        "chance", 0.35,
                        "night_bonus", 0.15,
                        "full_moon_bonus", 0.25,
                        "max_hexes", 3,
                        "resident_grudge", true,
                        "disabled", new JsonArray()),
                "standing", Json.obj(
                        "kill_witch", -4,
                        "kill_resident", -40,
                        "hurt_resident", -5,
                        "break_hex_task", 3,
                        "renounce_pact", -5,
                        "abandon_errand", -2,
                        "offering_daily_cap", 6),
                "services", Json.obj(
                        "lift_hex_cost", Json.obj("distrusted", 16, "stranger", 8, "known", 6, "trusted", 4, "kin", 0),
                        "witch_bottle_cost", 6,
                        "poppet_cost", 10),
                "kin_peace", true,
                "structures", structures);
        return new CommentedJson(root)
                .header("Witchbound (mod id better_witch_huts). Changes apply after a restart.\n"
                        + "Biomes, spacing and loot are datapack data: see the README.")
                .comment("pacts", "slots: how many pacts a player may hold at each Coven Standing tier (a pact already\n"
                        + "struck is kept if the standing drops). renounce_cost: emeralds a resident asks to undo a pact.\n"
                        + "disabled: pact ids that are never offered, e.g. [\"undying\", \"leech\"]. Ids: leech, night_hunter,\n"
                        + "toad, stoneskin, hare, magpie, witchfire, moth, wolf, briar, raven, undying.")
                .comment("hexes", "wild_witches_cast: hostile witches (and angry residents) channel a hex at players they\n"
                        + "fight, every cooldown_seconds (random in between), from up to cast_range blocks, landing with\n"
                        + "chance (+night_bonus at night, +full_moon_bonus under a full moon). max_hexes: how many one player\n"
                        + "can carry. resident_grudge: killing a resident always lays a hex. disabled: hex ids never laid.\n"
                        + "Ids: leaden_limbs, gnawing_hunger, black_cat, brittle_bones, withering_grip, insomnia,\n"
                        + "frail_heart, clumsy_fingers.")
                .comment("standing", "How Coven Standing (-100 to 100) changes. Tiers: hunted <= -50, distrusted <= -10,\n"
                        + "stranger < 20, known < 50, trusted < 80, kin. Errands give their own amounts.\n"
                        + "offering_daily_cap: the most standing offerings can give in one in-game day.")
                .comment("services", "Emerald prices at a resident witch. lift_hex_cost per tier (hunted: refused).")
                .comment("kin_peace", "Wild witches never target or harm a player of Kin standing.")
                .comment("structures", "Set a hut to false to stop it generating in new chunks (ones already\n"
                        + "generated stay).");
    }

    public static Values read(JsonObject root) {
        Map<Standing.Tier, Integer> slots = new EnumMap<>(Standing.Tier.class);
        for (Standing.Tier tier : Standing.Tier.values()) {
            slots.put(tier, clamp(Json.integer(root, "pacts.slots." + tier.id(), defaultSlots(tier)), 0, Pact.values().length));
        }
        Map<Standing.Tier, Integer> lift = new EnumMap<>(Standing.Tier.class);
        for (Standing.Tier tier : Standing.Tier.values()) {
            if (tier != Standing.Tier.HUNTED) {
                lift.put(tier, clamp(Json.integer(root, "services.lift_hex_cost." + tier.id(), defaultLift(tier)), 0, 64));
            }
        }
        int cdMin = clamp(Json.integer(root, "hexes.cooldown_seconds.min", 14), 2, 3600);
        int cdMax = clamp(Json.integer(root, "hexes.cooldown_seconds.max", 26), cdMin, 7200);
        Hexing hexing = new Hexing(Json.bool(root, "hexes.wild_witches_cast", true),
                clamp(Json.integer(root, "hexes.cast_range", 16), 4, 48), cdMin, cdMax,
                clamp(Json.num(root, "hexes.chance", 0.35), 0, 1), clamp(Json.num(root, "hexes.night_bonus", 0.15), 0, 1),
                clamp(Json.num(root, "hexes.full_moon_bonus", 0.25), 0, 1),
                clamp(Json.integer(root, "hexes.max_hexes", 3), 1, Hex.values().length),
                Json.bool(root, "hexes.resident_grudge", true), Set.copyOf(Json.strings(root, "hexes.disabled")));
        StandingRules standing = new StandingRules(Json.integer(root, "standing.kill_witch", -4),
                Json.integer(root, "standing.kill_resident", -40), Json.integer(root, "standing.hurt_resident", -5),
                Json.integer(root, "standing.break_hex_task", 3), Json.integer(root, "standing.renounce_pact", -5),
                Json.integer(root, "standing.abandon_errand", -2),
                clamp(Json.integer(root, "standing.offering_daily_cap", 6), 0, 200));
        Services services = new Services(Map.copyOf(lift), clamp(Json.integer(root, "pacts.renounce_cost", 24), 0, 64),
                clamp(Json.integer(root, "services.witch_bottle_cost", 6), 1, 64),
                clamp(Json.integer(root, "services.poppet_cost", 10), 1, 64));
        Map<String, Boolean> structures = new LinkedHashMap<>();
        for (String id : STRUCTURES) {
            structures.put(id, Json.bool(root, "structures." + id, true));
        }
        return new Values(Map.copyOf(slots), Set.copyOf(Json.strings(root, "pacts.disabled")), standing, hexing, services,
                Json.bool(root, "kin_peace", true), Map.copyOf(structures));
    }

    private static int defaultSlots(Standing.Tier tier) {
        return switch (tier) {
            case HUNTED, DISTRUSTED -> 0;
            case STRANGER, KNOWN -> 1;
            case TRUSTED -> 2;
            case KIN -> 3;
        };
    }

    private static int defaultLift(Standing.Tier tier) {
        return switch (tier) {
            case HUNTED -> -1;
            case DISTRUSTED -> 16;
            case STRANGER -> 8;
            case KNOWN -> 6;
            case TRUSTED -> 4;
            case KIN -> 0;
        };
    }

    public static Values load(Path configDir) {
        values = read(spec().load(configDir.resolve(FILE)));
        return values;
    }

    public static Values get() {
        return values;
    }

    /** For tests. */
    public static void set(Values v) {
        values = v;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
