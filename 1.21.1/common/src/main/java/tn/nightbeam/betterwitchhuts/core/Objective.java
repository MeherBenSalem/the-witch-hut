package tn.nightbeam.betterwitchhuts.core;

import java.util.List;

/**
 * Something a player has to do: the task that breaks a hex, or a witch's errand. Progress is read from the player's
 * vanilla statistics (so it works on every loader and counts what other mods do too), except for deliveries, which are
 * taken from the inventory when the errand is handed in.
 *
 * @param id     language key suffix ({@code objective.better_witch_huts.<id>}); the text takes the target as %s
 * @param kind   which statistic is read
 * @param keys   the statistic's subjects: custom stat ids, items, blocks or entity types (namespace optional). A key
 *               starting with {@code #} is a tag, and {@code #food} means every item that can be eaten
 * @param target how much is needed, in the statistic's units
 * @param scale  divisor for display (distances are counted in centimetres)
 */
public record Objective(String id, Kind kind, List<String> keys, int target, int scale) {
    public enum Kind {
        /** {@code minecraft:custom} statistics: distance, fish caught, bells rung, animals bred, mob kills. */
        CUSTOM,
        /** Items used (eaten, drunk, placed). */
        USED,
        /** Entities killed. */
        KILLED,
        /** Blocks mined. */
        MINED,
        /** Items handed over to the witch. */
        DELIVER
    }

    public Objective(String id, Kind kind, List<String> keys, int target) {
        this(id, kind, keys, target, 1);
    }

    public int displayTarget() {
        return Math.max(1, target / Math.max(1, scale));
    }

    public int display(int progress) {
        return Math.min(displayTarget(), progress / Math.max(1, scale));
    }
}
