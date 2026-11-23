package tn.nightbeam.betterwitchhuts.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StandingTest {
    @Test
    void tiersFollowTheThresholds() {
        assertEquals(Standing.Tier.HUNTED, Standing.tier(-100));
        assertEquals(Standing.Tier.HUNTED, Standing.tier(-50));
        assertEquals(Standing.Tier.DISTRUSTED, Standing.tier(-49));
        assertEquals(Standing.Tier.DISTRUSTED, Standing.tier(-10));
        assertEquals(Standing.Tier.STRANGER, Standing.tier(-9));
        assertEquals(Standing.Tier.STRANGER, Standing.tier(0));
        assertEquals(Standing.Tier.STRANGER, Standing.tier(19));
        assertEquals(Standing.Tier.KNOWN, Standing.tier(20));
        assertEquals(Standing.Tier.TRUSTED, Standing.tier(50));
        assertEquals(Standing.Tier.KIN, Standing.tier(80));
        assertEquals(Standing.Tier.KIN, Standing.tier(100));
    }

    @Test
    void outOfRangeValuesAreClamped() {
        assertEquals(Standing.Tier.KIN, Standing.tier(500));
        assertEquals(Standing.Tier.HUNTED, Standing.tier(-500));
        assertEquals(100, Standing.clamp(101));
        assertEquals(-100, Standing.clamp(-101));
    }

    @Test
    void nextTierClimbs() {
        assertEquals(Standing.Tier.KNOWN, Standing.Tier.STRANGER.next());
        assertEquals(Standing.Tier.KIN, Standing.Tier.KIN.next());
        assertTrue(Standing.Tier.KIN.atLeast(Standing.Tier.KNOWN));
    }
}
