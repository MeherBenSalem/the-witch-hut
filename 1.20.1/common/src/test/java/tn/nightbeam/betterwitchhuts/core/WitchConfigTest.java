package tn.nightbeam.betterwitchhuts.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WitchConfigTest {
    @Test
    void defaults() {
        WitchConfig.Values v = WitchConfig.Values.defaults();
        assertEquals(0, v.pactSlots(Standing.Tier.HUNTED));
        assertEquals(1, v.pactSlots(Standing.Tier.STRANGER));
        assertEquals(2, v.pactSlots(Standing.Tier.TRUSTED));
        assertEquals(3, v.pactSlots(Standing.Tier.KIN));
        assertEquals(3, v.hexing().maxHexes());
        assertTrue(v.hexing().wildWitches());
        assertNull(v.services().liftCost().get(Standing.Tier.HUNTED));
        assertEquals(0, v.services().liftCost().get(Standing.Tier.KIN));
        assertTrue(v.structureEnabled("bog_hut"));
        assertEquals(7, WitchConfig.STRUCTURES.size());
    }

    @Test
    void userValuesWinAndAreClamped() {
        JsonObject root = WitchConfig.spec().defaults();
        root.getAsJsonObject("hexes").addProperty("max_hexes", 99);
        root.getAsJsonObject("hexes").addProperty("chance", 7.5);
        root.getAsJsonObject("structures").addProperty("rime_hut", false);
        WitchConfig.Values v = WitchConfig.read(root);
        assertEquals(Hex.values().length, v.hexing().maxHexes());
        assertEquals(1.0, v.hexing().chance());
        assertFalse(v.structureEnabled("rime_hut"));
    }

    @Test
    void writesACommentedFileAndReadsItBack(@TempDir Path dir) throws Exception {
        WitchConfig.Values first = WitchConfig.load(dir);
        Path file = dir.resolve(WitchConfig.FILE);
        assertTrue(Files.exists(file));
        String text = Files.readString(file);
        assertTrue(text.contains("//"), "the file carries comments");
        assertEquals(first, WitchConfig.load(dir));
    }
}
