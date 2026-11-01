package tn.nightbeam.betterwitchhuts.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the Coven remembers about one player. Saved as JSON in the world folder
 * ({@code data/better_witch_huts/players/<uuid>.json}), so it survives death, logging out and loader changes.
 */
public final class PlayerRecord {
    public int version = 1;
    public int standing;
    /** Pact ids in the order they were struck. */
    public List<String> pacts = new ArrayList<>();
    public List<HexState> hexes = new ArrayList<>();
    /** The errand being worked on, or null. */
    public ErrandState errand;
    /** The game day the Undying pact last saved this player (-1: never). */
    public long undyingDay = -1;
    /** The game day of the last offering, and how much standing offerings gave that day. */
    public long offeringDay = -1;
    public int offeringStanding;
    /** Lifetime counts, shown in the Grimoire. */
    public int pactsStruck;
    public int hexesBroken;
    public int errandsDone;
    /** Huts visited (structure ids), for the Grimoire and the "all seven" advancement. */
    public List<String> visited = new ArrayList<>();

    public transient boolean dirty;

    public static final class HexState {
        public String hex = "";
        /** The objective that breaks it. */
        public String task = "";
        /** The statistic's value when the hex landed; progress is the difference. */
        public int baseline;
        /** Who laid it, for the Grimoire ("Old Mother Mirewort", "a witch"). */
        public String caster = "";

        public HexState() {
        }

        public HexState(String hex, String task, int baseline, String caster) {
            this.hex = hex;
            this.task = task;
            this.baseline = baseline;
            this.caster = caster;
        }
    }

    public static final class ErrandState {
        public String errand = "";
        public int baseline;
        /** The resident who gave it. */
        public String giver = "";

        public ErrandState() {
        }

        public ErrandState(String errand, int baseline, String giver) {
            this.errand = errand;
            this.baseline = baseline;
            this.giver = giver;
        }
    }

    public boolean hasPact(Pact pact) {
        return pacts.contains(pact.id());
    }

    public boolean hasHex(Hex hex) {
        for (HexState state : hexes) {
            if (state.hex.equals(hex.id())) {
                return true;
            }
        }
        return false;
    }

    public List<Pact> pactList() {
        List<Pact> list = new ArrayList<>();
        for (String id : pacts) {
            Pact pact = Pact.byId(id);
            if (pact != null) {
                list.add(pact);
            }
        }
        return list;
    }

    /** Drops unknown pacts and hexes (from a newer version, or removed ones) and clamps the standing. */
    public PlayerRecord sanitize() {
        if (pacts == null) {
            pacts = new ArrayList<>();
        }
        if (hexes == null) {
            hexes = new ArrayList<>();
        }
        if (visited == null) {
            visited = new ArrayList<>();
        }
        pacts.removeIf(id -> Pact.byId(id) == null);
        pacts = new ArrayList<>(new java.util.LinkedHashSet<>(pacts));
        hexes.removeIf(h -> h == null || Hex.byId(h.hex) == null || Objectives.byId(h.task) == null);
        if (errand != null && Errand.byId(errand.errand) == null) {
            errand = null;
        }
        standing = Standing.clamp(standing);
        return this;
    }
}
