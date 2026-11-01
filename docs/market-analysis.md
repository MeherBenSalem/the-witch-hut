# Market analysis: RPG gaps and the Witchbound remake

Data collected on **2026-11-01** from the Modrinth API (`api.modrinth.com/v2/project/<slug>` and `/v2/search`) and
the CurseForge widget API (`api.cfwidget.com/minecraft/mc-mods/<slug>`). Download counts are all-time.

Numbers marked *unverified* could not be checked against an API.

The NightBeam portfolio comes from this account's repositories and the 2026-10-29 mod audit in
`Modpack/research/nightbeam_mods.md`.

## 1. What the studio already covers

NightBeam's RPG line, mostly sold as the **Remnants** family, already covers the classic RPG pillars:

| Pillar | NightBeam mod |
|---|---|
| Player levels, attributes, respecs | Remnants \| RPG Stats (RPG Attribute System 5.0) |
| Soulslike weapons, runes, shrines, spells | Runeborn 3.0 |
| Mob levels, ranks, elite traits, named champions | Remnants \| RPG Mobs 3.0 |
| Boss dimension and arenas | Remnants \| The Eclipsed Realm 3.1 |
| Roguelite dungeon runs | Remnants: Roguelite Dungeons 1.0 |
| Skill tree and elemental magic | Remnants \| Skills and Powers 2.0 |
| Mercenary companions | Remnants \| Fellowship 2.0 |
| Loot | Mob LootBags 2.0, Much More Dungeons 2.0 |
| Quests, MMO and wave gateways on servers | NightQuests, NightMMO, InfinityRifts (Paper plugins) |

**What is missing from the portfolio:**
- anything that tracks the player over time with lasting consequences: grudges, reputation, curses, bargains, lineage
- a faction to belong to
- a reason to visit the small structure mods (The Witch Hut, The Gingerbread House) twice

The 1.0 Witch Hut had one static hut with fixed chest contents. Its listing promised trader witches, random loot and
secret rooms, and none of that was in the jar (audit §2.17). It has 1,097 Modrinth and 14,556 CurseForge downloads.

## 2. What is saturated

These are the most downloaded RPG-adjacent mods with a 1.21.1 build (Modrinth). Crowded genres include bosses,
dungeons, weapons, spells and classes, combat feel, mob scaling and item affixes.

| Mod | Genre | Modrinth downloads | Newest MC |
|---|---|---|---|
| L_Ender's Cataclysm | Bosses and dungeons | 19,698,327 | 1.21.5 |
| Better Combat | Combat | 17,771,257 | 26.3 |
| Artifacts | Loot items | 14,601,280 | 26.1.2 |
| When Dungeons Arise | Dungeons | 11,597,204 | 1.21.1 |
| Aquamirae | Bosses | 11,402,506 | 1.21.1 |
| Simply Swords | Weapons | 10,595,834 | 1.21.1 |
| Mowzie's Mobs | Bosses | 9,051,886 | 1.21.1 |
| Illager Invasion | Mobs | 8,510,708 | 26.2 |
| Goety | Necromancy | 7,326,023 | 1.21.1 |
| Spell Engine / Iron's Spells / Wizards | Spells | 6.5M / 6.2M / 5.8M | 26.3 / 1.21.1 / 26.3 |
| Pufferfish's Skills | Skill trees | 4,833,022 | 26.3 |
| Origins | Races | 3,158,117 | 1.21.1 |

Another boss mod, skill tree or spell book would compete with these, and with NightBeam's own mods.

## 3. The witch niche

### Witchcraft mods

| Mod | Newest MC | Downloads (MR / CF) | Status |
|---|---|---|---|
| Witchery | 1.7.10 | – / 17.0M | abandoned since 2015 |
| Bewitchment (curses, poppets, familiars, demon pacts) | 1.20.1 | 1.65M / 10.6M (legacy) | abandoned since 2024-10 |
| Enchanted: Witchcraft (herbs, circle magic) | 1.21.1 | 562k / 4.4M | active |
| Hexerei (cauldron, brooms) | 1.21.1 | 462k / 27.4M | stale since 2025-04 |
| Hexalia (folk witchcraft, herbalism) | 1.21.1 | 695k / 3.9M | active; the closest competitor |
| Just Another Witchery Remake | 26.1.2 (NeoForge only) | 70k / 518k | active |
| Occultism (spirits and automation) | 26.3 (Forge/NeoForge) | 932k / 68.9M | active |

All of these are **tech or ritual magic**: altars, distilleries, circles and cauldron recipes. None treats witches as
a **faction**. Hexerei announced quest-giving coven witches and coven alignment and never shipped them (its Modrinth
description; last update 2025-04-02).

On Fabric 26.x, nothing carries on from Bewitchment's 1.65M-download audience.

### Witch-hut structures

| Mod | Newest MC | Downloads |
|---|---|---|
| YUNG's Better Witch Huts (replaces the vanilla hut) | 26.1.2 | 18.9M MR / 91M CF |
| Dungeons and Taverns: Swamp Hut Overhaul | 26.3 | 1.7M MR |
| Epic Structures: Witch Huts | 26.3 | 251k MR / 1.2M CF |
| The Witch Hut (1.0) | 26.2 | 1.1k MR / 14.6k CF |

Better huts are a solved problem: YUNG's alone has over 100M downloads. A second "prettier hut" mod has no room, so
Witchbound does not try to be one. Its huts are places where something happens, and they sit next to YUNG's without
conflict.

### Witch mobs

There is effectively nothing here: no witch-variant mod passes 7,000 downloads. Overhauls of other vanilla mobs are
huge: Creeper Overhaul 11.1M, Illager Invasion 8.5M, Enderman Overhaul 8.4M.

## 4. RPG mechanics: covered or open?

| Mechanic | Closest maintained mods (1.21.1+) | Verdict |
|---|---|---|
| **Pacts**: a permanent boon with a permanent cost | Bewitchment (stopped at 1.20.1), Wandering Cursebringer (2.2k), Binding Vows (185) | **Gap** |
| **Hexes**: mob-cast curses that survive death and need a cure task | Vampirism (a transformation, not curses), Contagion (an infection timer) | **Gap** |
| **Faction reputation** | Reputation: **5.9M** on Forge **1.18.2**, last file 2025-04. Its 26.2 rebuild has 1.3k | **Partial**: proven demand, left behind on old versions |
| Witch mob overhaul | none above 7k | **Gap** |
| Coven as a faction or quest giver | promised by Hexerei, never shipped | **Gap** |
| Nemesis system | Nemesis System [Neo/Fabric]: 925 downloads, 1.21.1; the 1.12.2 original has 26k | **Partial**: nothing proven, nothing on 26.x |
| Bloodline or heirs | MCA Descendants (6.5k, needs MCA, hardcore only) | **Gap** |
| Bestiary with combat mastery | Araxer's Bestiary (85k), Wildex (25k): information only | **Gap** for the combat bonus |
| Notoriety and bounty hunters | Thief (109k: golems react to crimes), Vampirism's hunters | **Gap** |
| Oaths and vows | The Vows (73), Binding Vows (185) | **Gap** |
| Monster Hunter-style hunts | Monster Expansion: **1.87M** CF on **1.20.1 only** | **Gap** on 1.21.1+ |
| Rumours and taverns | Dungeons and Taverns (22.6M) sells maps; no rumour system | **Partial** |
| Herbalism and remedies | Hexalia, Enchanted (both stop at 1.21.1) | Gap on 26.x |
| Bounty boards | Bountiful (8.0M MR / 80M CF), stops at 1.21.1 | Covered on 1.21.1, thin on 26.x |
| Familiars that buff the player | Familiar Friends (17k), Occultism familiars | Partial |

The pattern is clear. Systems that **track the player over time** and **make choices cost something** are the open
ground: grudges, standing, bargains, curses, oaths and lineage. The studio has not built any of them yet.

## 5. RPG mods that don't exist yet

Ranked by demand evidence, lack of competition, and fit with the Remnants line.

### 1. Witchbound: pacts, hexes and the Coven *(this remake)*

**The hook:** strike a bargain with a witch, keep the gift, pay the price forever. Witches become a faction you earn
standing with. Their hexes follow you past death until you break them.

**The gap:** rows 1 to 5 of the table above. The pact and hex gaps are open, Reputation's 5.9M audience is stranded on
1.18.2, and witch mobs are empty.

**Fit with the studio:**
- It slots between RPG Stats (numbers) and Runeborn (gear) as the **choices with consequences** layer.
- It needs no other mod.
- It reuses the Fisherman's Coast structure tooling and the new hut set gives the old listing a reason to exist.

**Status:** built in 2.0 (see `DESIGN-2.0.md`).

### 2. Remnants | Nemesis (Grudges)

**The hook:**
- A mob that kills you **rises**: it gets a name, a title taken from how it killed you ("Krug the Ankle-Biter", "the
  Drowned Who Waited") and a trait.
- It hunts you across later nights and gets stronger each time it wins.
- Captains gather around a warlord you can track down.

**Why now:** the only modern attempt has 925 downloads, and there is nothing on 26.x. It plugs straight into RPG Mobs:
ranks, traits and nameplates already exist there.

**Effort:** medium. Persistence, a name generator, and a "hunt" spawn director.

### 3. Remnants | Wanted (Notoriety and Bounty Hunters)

**The hook:**
- Crimes raise your notoriety in each region: killing villagers or golems, robbing village chests, burning crops.
- Past a threshold, named bounty hunters ride after you with tracking compasses.
- You can pay your bounty off at a notary, fight it out, or hire your own hunters to collect bounties on other
  players (servers).

**Why now:** nothing sends NPC hunters after the player. Thief (109k) shows people want crime to matter.

**Fit:** Fellowship's mercenary AI and RPG Mobs' ranks are reusable.

### 4. Remnants | The Hunt (Monster Hunter-style)

**The hook:**
- Track a great beast by its footprints, scent and broken trees.
- Break its parts mid-fight and trap it or slay it.
- Carve materials and forge gear sets with set skills.

**Why now:** Monster Expansion reached 1.87M CurseForge downloads in about a year, and only on 1.20.1. Nobody serves
this audience on 1.21.1 or 26.x.

**Effort:** high (GeckoLib beasts). The payoff is the largest audience on this list.

### 5. Remnants | Allegiance (faction reputation)

**The hook:** standing with every vanilla faction: villagers per village, illagers, piglins, the Coven (Witchbound),
the drowned. Ranks, faction quests, hostility, prices and safe passage all follow from it.

**Why now:** Reputation's 5.9M downloads are stranded on 1.18.2, and its 26.2 rebuild has 1.3k.

**Fit:** Witchbound's Coven Standing is the first faction. Witchbound's `Standing` and `Records` code generalise
directly.

### 6. Remnants | Bloodline (heirs)

**The hook:**
- On death (an optional "lineage" mode), you take over as your heir.
- The heir inherits one of the parent's traits, a random quirk (Rogue Legacy style) and the family heirloom, which
  grows with every generation.
- A family tree records every ancestor and how they died.

**Why now:** the only attempt needs MCA and hardcore. It is a strong hook for hardcore servers and content creators.

### 7. Hunter's Codex (bestiary mastery)

**The hook:** every mob type has a codex page that fills in as you fight it: weaknesses, behaviour, lore. Each mastery
rank gives a small, stacking bonus against that type, such as damage, resistance or loot.

**Why now:** existing bestiaries (85k, 25k) only give information.

**Fit:** a natural add-on to RPG Mobs, or a feature inside it.

### 8. Sworn (oaths and vows)

**The hook:** swear an oath at a shrine, such as no armour, no healing potions, or no deaths for three nights. Keep it
for loot multipliers, Valor or runes. Break it and you are Oathbroken for a while. It is Hades' Pact of Punishment for
Minecraft.

**Why now:** the existing attempts have 73 and 185 downloads.

**Fit:** Runeborn shrines or Witchbound residents could administer it, so it does not need to be its own mod.

### 9. Tall Tales (rumours and taverns)

**The hook:** tavern keepers and bards trade rumours that point to real, unexplored structures, including other mods'
structures through tags. A journal tracks them and the rumours escalate into short questlines.

**Why now:** Dungeons and Taverns' 22.6M downloads only sell maps.

### 10. Witchbound follow-ups *(roadmap, see `DESIGN-2.0.md` §8)*

- **Witch variants:** the empty witch-mob niche next to 8 to 11M-download mob overhauls.
- **Herbalism and remedies** on 26.x, where Hexalia does not reach.
- **Familiars.**
- **The Sabbath:** a full-moon coven gathering, with a boss for players the Coven hunts.

## 6. Why Witchbound for this repository

- It **keeps the asset**: the mod id, the CurseForge and Modrinth projects and the 1.0 hut all carry over, so
  downloads and followers carry over too.
- It **turns a structure into a system**: seven huts with residents give the RPG systems a place to live, the same move
  that made Fisherman's Coast work.
- It **fills the studio's gap**: lasting choices and consequences, with no overlap with RAS, Runeborn, RPG Mobs, Skills
  and Powers or Fellowship.
- It **fills the market's gap**: pacts, mob-cast hexes and a witch faction exist nowhere on modern versions. On Fabric
  26.x there is no witchcraft mod at all.
