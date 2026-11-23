package tn.nightbeam.betterwitchhuts.core;

import java.util.List;
import java.util.Locale;

/**
 * The eight hexes. A hex stays through death and milk until it is broken: by doing its task, by a Witch Bottle, or by
 * a resident witch for a price. Each hex rolls one of its tasks when it lands.
 */
public enum Hex {
    LEADEN_LIMBS(0x4A4E5A, Objectives.SWIM, Objectives.SPRINT),
    GNAWING_HUNGER(0x8A6A2E, Objectives.EAT, Objectives.FISH),
    BLACK_CAT(0x1B1B22, Objectives.RING_BELLS, Objectives.TRADE),
    BRITTLE_BONES(0xD8D3C0, Objectives.SLAY_SKELETONS, Objectives.SLAY_ANY),
    WITHERING_GRIP(0x4C3A57, Objectives.SLAY_ANY, Objectives.SLAY_ZOMBIES),
    INSOMNIA(0x2A2F5C, Objectives.PLACE_CANDLES, Objectives.SLAY_PHANTOMS),
    FRAIL_HEART(0x9C2E3F, Objectives.BREED, Objectives.GOLDEN_APPLE),
    CLUMSY_FINGERS(0x6B5B3E, Objectives.MINE_STONE, Objectives.MINE_LOGS);

    private final int color;
    private final List<Objective> tasks;

    Hex(int color, Objective... tasks) {
        this.color = color;
        this.tasks = List.of(tasks);
    }

    public int color() {
        return color;
    }

    public List<Objective> tasks() {
        return tasks;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static int index(Hex hex) {
        return hex == null ? -1 : hex.ordinal();
    }

    public static Hex byIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : null;
    }

    public static Hex byId(String id) {
        for (Hex hex : values()) {
            if (hex.id().equals(id)) {
                return hex;
            }
        }
        return null;
    }
}
