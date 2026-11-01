package tn.nightbeam.betterwitchhuts.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DraftTest {
    @Test
    void sameDaySameWitchSameOffers() {
        long seed = Draft.seed(11, 22, 5);
        assertEquals(Draft.pacts(seed, List.of(), Set.of()), Draft.pacts(seed, List.of(), Set.of()));
        assertEquals(Draft.errand(seed), Draft.errand(seed));
    }

    @Test
    void offersChangeFromDayToDay() {
        Set<List<Pact>> seen = new HashSet<>();
        for (int day = 0; day < 20; day++) {
            seen.add(Draft.pacts(Draft.seed(11, 22, day), List.of(), Set.of()));
        }
        assertTrue(seen.size() > 10, "offers should vary across days");
        assertNotEquals(Draft.seed(1, 2, 3), Draft.seed(1, 2, 4));
    }

    @Test
    void neverOffersHeldOrDisabledPacts() {
        for (int day = 0; day < 200; day++) {
            List<Pact> offers = Draft.pacts(Draft.seed(3, 4, day), List.of(Pact.LEECH, Pact.UNDYING), Set.of("toad"));
            assertEquals(3, offers.size());
            assertEquals(3, new HashSet<>(offers).size());
            assertFalse(offers.contains(Pact.LEECH));
            assertFalse(offers.contains(Pact.UNDYING));
            assertFalse(offers.contains(Pact.TOAD));
        }
    }

    @Test
    void fewerOffersWhenAlmostEverythingIsHeld() {
        List<Pact> held = List.of(Pact.values()).subList(0, Pact.values().length - 2);
        assertEquals(2, Draft.pacts(1, held, Set.of()).size());
    }
}
