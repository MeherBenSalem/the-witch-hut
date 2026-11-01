package tn.nightbeam.betterwitchhuts.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DefinitionsTest {
    @Test
    void objectiveIdsAreUniqueAndIndexed() {
        Set<String> ids = new HashSet<>();
        for (Objective objective : Objectives.all()) {
            assertTrue(ids.add(objective.id()), "duplicate objective " + objective.id());
            assertEquals(objective, Objectives.byIndex(Objectives.index(objective)));
            assertTrue(objective.target() > 0);
            assertTrue(objective.displayTarget() > 0);
        }
        // Menu data travels as shorts: every index must fit.
        assertTrue(Objectives.all().size() < Short.MAX_VALUE);
    }

    @Test
    void everyHexHasTasksAndEveryErrandAnObjective() {
        for (Hex hex : Hex.values()) {
            assertTrue(hex.tasks().size() >= 2, hex + " should roll between tasks");
            for (Objective task : hex.tasks()) {
                assertNotNull(Objectives.byId(task.id()));
            }
        }
        for (Errand errand : Errand.values()) {
            assertNotNull(Objectives.byId(errand.objective().id()));
            assertTrue(errand.standing() > 0 && errand.emeralds() > 0);
        }
    }

    @Test
    void idsRoundTrip() {
        for (Pact pact : Pact.values()) {
            assertEquals(pact, Pact.byId(pact.id()));
            assertEquals(pact, Pact.byIndex(Pact.index(pact)));
        }
        for (Hex hex : Hex.values()) {
            assertEquals(hex, Hex.byId(hex.id()));
        }
        assertNull(Pact.byIndex(-1));
        assertNull(Hex.byId("nope"));
    }

    @Test
    void distancesDisplayInBlocks() {
        assertEquals(150, Objectives.SWIM.displayTarget());
        assertEquals(75, Objectives.SWIM.display(7500));
        assertEquals(150, Objectives.SWIM.display(999999));
    }

    @Test
    void sanitizeDropsUnknownEntries() {
        PlayerRecord record = new PlayerRecord();
        record.standing = 999;
        record.pacts.add("leech");
        record.pacts.add("leech");
        record.pacts.add("removed_pact");
        record.hexes.add(new PlayerRecord.HexState("insomnia", "place_candles", 0, ""));
        record.hexes.add(new PlayerRecord.HexState("gone", "place_candles", 0, ""));
        record.hexes.add(new PlayerRecord.HexState("insomnia", "gone_task", 0, ""));
        record.errand = new PlayerRecord.ErrandState("gone", 0, "");
        record.sanitize();
        assertEquals(100, record.standing);
        assertEquals(1, record.pacts.size());
        assertEquals(1, record.hexes.size());
        assertNull(record.errand);
        assertTrue(record.hasPact(Pact.LEECH));
        assertTrue(record.hasHex(Hex.INSOMNIA));
    }
}
