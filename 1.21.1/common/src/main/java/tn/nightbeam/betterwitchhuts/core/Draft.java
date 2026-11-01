package tn.nightbeam.betterwitchhuts.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * What a resident offers: three pacts and one errand, the same all day for one player at one witch, and new ones the
 * next day. Seeded, so reopening the menu never rerolls.
 */
public final class Draft {
    public static final int OFFERS = 3;

    private Draft() {
    }

    public static long seed(long witch, long player, long day) {
        long h = 0x9E3779B97F4A7C15L;
        h = (h ^ witch) * 0xBF58476D1CE4E5B9L;
        h = (h ^ player) * 0x94D049BB133111EBL;
        h = (h ^ day) * 0xBF58476D1CE4E5B9L;
        return h ^ (h >>> 31);
    }

    /** Up to {@link #OFFERS} different pacts, none of them already held or disabled. */
    public static List<Pact> pacts(long seed, Collection<Pact> held, Collection<String> disabled) {
        List<Pact> pool = new ArrayList<>();
        for (Pact pact : Pact.values()) {
            if (!held.contains(pact) && !disabled.contains(pact.id())) {
                pool.add(pact);
            }
        }
        Random random = new Random(seed);
        List<Pact> offers = new ArrayList<>();
        while (!pool.isEmpty() && offers.size() < OFFERS) {
            offers.add(pool.remove(random.nextInt(pool.size())));
        }
        return offers;
    }

    public static Errand errand(long seed) {
        Errand[] all = Errand.values();
        return all[new Random(seed ^ 0x5DEECE66DL).nextInt(all.length)];
    }

    /** Which of a hex's tasks it rolls. */
    public static Objective task(Hex hex, Random random) {
        return hex.tasks().get(random.nextInt(hex.tasks().size()));
    }
}
