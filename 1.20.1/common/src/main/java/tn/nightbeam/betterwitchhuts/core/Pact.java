package tn.nightbeam.betterwitchhuts.core;

import java.util.Locale;

/**
 * The twelve pacts. Each has a Gift and a Price; the numbers live in {@code PactRules} (Minecraft side) and in the
 * language file, and are listed in the README.
 */
public enum Pact {
    LEECH(0x8E1B2B),
    NIGHT_HUNTER(0x31336E),
    TOAD(0x4F7F35),
    STONESKIN(0x6F6E73),
    HARE(0xC29A62),
    MAGPIE(0x24304A),
    WITCHFIRE(0xD8662A),
    MOTH(0xB7A3D9),
    WOLF(0x6A5444),
    BRIAR(0x3C6B37),
    RAVEN(0x22202C),
    UNDYING(0x86A39C);

    private final int color;

    Pact(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** -1 for none, so it can travel in a menu data slot. */
    public static int index(Pact pact) {
        return pact == null ? -1 : pact.ordinal();
    }

    public static Pact byIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : null;
    }

    public static Pact byId(String id) {
        for (Pact pact : values()) {
            if (pact.id().equals(id)) {
                return pact;
            }
        }
        return null;
    }
}
