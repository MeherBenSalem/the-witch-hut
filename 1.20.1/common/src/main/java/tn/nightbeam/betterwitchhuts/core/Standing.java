package tn.nightbeam.betterwitchhuts.core;

import java.util.Locale;

/** Coven Standing: how the witches of the world feel about one player, from -100 to 100. */
public final class Standing {
    public static final int MIN = -100;
    public static final int MAX = 100;

    private Standing() {
    }

    public enum Tier {
        /** Witches attack on sight and hex more often; residents refuse every service. */
        HUNTED(MIN),
        /** Residents refuse pacts and charge double to lift hexes. */
        DISTRUSTED(-49),
        /** Where everyone starts. */
        STRANGER(-9),
        /** Residents sell Poppets and charms. */
        KNOWN(20),
        /** A second pact slot, cheaper remedies. */
        TRUSTED(50),
        /** A third pact slot, free remedies, and wild witches leave you alone. */
        KIN(80);

        private final int from;

        Tier(int from) {
            this.from = from;
        }

        /** The lowest standing in this tier. */
        public int from() {
            return from;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The tier above, or this one at the top. */
        public Tier next() {
            return this == KIN ? KIN : values()[ordinal() + 1];
        }

        public boolean atLeast(Tier other) {
            return ordinal() >= other.ordinal();
        }
    }

    public static Tier tier(int standing) {
        int s = clamp(standing);
        Tier result = Tier.HUNTED;
        for (Tier tier : Tier.values()) {
            if (s >= tier.from()) {
                result = tier;
            }
        }
        return result;
    }

    public static int clamp(int standing) {
        return Math.max(MIN, Math.min(MAX, standing));
    }
}
