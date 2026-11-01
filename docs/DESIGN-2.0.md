# Witchbound 2.0: audit, gaps and plan

This document records:
- what The Witch Hut 1.0 actually did (read from the jar and the template, not the listing)
- where the RPG market has room (summarised from [market-analysis.md](market-analysis.md))
- what 2.0 changes, and why

Every 2.0 claim below is implemented in all six builds:
- Fabric and NeoForge on Minecraft 26.1.2, 26.2 and 26.3
- checked in game as recorded in [QA-2.0.0.md](QA-2.0.0.md)

## 1. Audit of 1.0

| Area | What the code and data did |
|---|---|
| Content | One jigsaw structure, `better_witch_huts:witch_hut` (a 24×14×23 template of a round orange-terracotta tower under a deepslate "witch hat" roof), and a log line. No blocks, items, entities or loot tables. |
| Placement | Plain jigsaw at `WORLD_SURFACE_WG`, no terrain check, `terrain_adaptation: none`, in swamp, dark forest and plains. Spacing 150, separation 100: rare. |
| Loot | One chest with 11 iron nuggets, 9 iron ingots and 2 emeralds saved in the template. Five barrels were empty. |
| "Witches" | A `minecraft:witch` spawner in the bedroom. The listing promised hostile, neutral and trader witches, random magical loot and secret compartments. None of it was in the jar (Modpack audit §2.17). |
| Dependencies | Published 1.21.1 jars required `jauml` without saying so on Modrinth. |
| Repository | A `shared/` copy of the data beside each workspace. No tests, QA, marketing or patch notes. |

## 2. Where the market has room

The full numbers are in [market-analysis.md](market-analysis.md). In short:

- **Saturated:** bosses, dungeons, spells and classes, combat feel, mob scaling and item affixes. NightBeam already
  covers these with RAS, Runeborn, RPG Mobs, the Eclipsed Realm, Skills and Powers and Fellowship.
- **Better witch huts are a solved problem.** YUNG's Better Witch Huts has over 100M downloads across both sites.
- **Open:**
  - pacts (a permanent boon with a permanent price)
  - mob-cast hexes that survive death and need a cure
  - witches as a faction
  - a witch mob overhaul: no witch-variant mod passes 7k downloads, while creeper, enderman and illager overhauls each
    have 8 to 11M
  - faction reputation: Reputation's 5.9M downloads are stranded on 1.18.2
- **Witchcraft on Fabric 26.x does not exist.** Bewitchment's 1.65M Fabric players have had nothing since 1.20.1.

The remake moves out of the crowded "better hut" lane and into these open systems. Huts stay, as the places where
the systems live.

## 3. Gap table

| Expectation | 1.0 | 2.0 |
|---|---|---|
| Structures | 1 hut | **7 coven huts**, one per biome family. The 1.0 hut is kept and repaired as **The Old Hut** |
| Fit to the land | fixed jigsaw, cliffs and floating corners | **Terrain-aware placement**: the land is sampled at each hut's footprint before it is placed. Land huts need level ground, swamp huts need shallow water or mud, and a spot that does not fit is skipped. Kept 4 chunks from villages |
| Witches | a spawner | **Resident Coven Witches**: named, persistent, never join raids, and fight only if struck or if the Coven hunts you |
| Loot | fixed iron in one chest | a loot table per hut plus shared storage barrels: potions, reagents, Witch Bottles, Poppets, Grimoires, enchanted books |
| RPG systems | none | **Pacts**, **Hexes**, **Coven Standing**, **Errands**, **Offerings**, **Remedies** |
| UI | none | the **Coven screen** at every resident, the **Grimoire** ledger, and pacts and hexes as status-effect sigils on the HUD |
| Wild witches | vanilla | they **channel hexes** at players they fight, which you can interrupt by hitting them, and leave Kin of the Coven alone |
| Progression | none | a 25-advancement tab |
| Config | none | `config/better_witch_huts.json`, commented: pact slots per tier, hexing, standing rules, prices, per-hut toggles |
| Admin | none | `/witchbound` for players; `info`, `standing`, `pact`, `hex` and `errand` subcommands for operators |
| Dependencies | jauml (undeclared) | **none** (Fabric API on Fabric) |
| Versions | 26.1.2, 26.2 | 26.1.2, 26.2, **26.3** (Fabric and NeoForge) |
| Tests / QA | none | 15 JUnit tests for the rules, a dedicated server on every build, and a real client under Xvfb for the screens |

## 4. Name and identity

**Witchbound** (it was *The Witch Hut*). The mod is no longer a hut: it is a bond with the Coven, made of the
bargains you strike and the curses you carry. The name says "witch", so search still finds it, and says "bound" for the
pacts and hexes. Modrinth has no other project with this name (checked 2026-11-01).

On the listing it follows the studio's RPG line: **Remnants | Witchbound**. The tagline is **Every gift has a price.**

**The mod id stays `better_witch_huts`**, and the 1.0 structure keeps both its id (`better_witch_huts:witch_hut`) and
its 1.0 template (`witch_hut.nbt`, unchanged). So:
- 1.0 worlds load without remapping
- huts 1.0 left half-generated finish from the same template

New huts of that kind use a repaired copy, `old_hut.nbt`. The CurseForge and Modrinth projects, their downloads and
their followers all carry over.

The visual identity is a coven by candlelight:
- deep aubergine and night-blue backgrounds
- candle gold for titles and rules
- a sickly witch-green for gifts and blood red for prices and hexes
- a serif display face and pixel-art sigils

## 5. The feature set as built

### The seven huts

| Id | Name | Biomes | Resident | Why visit |
|---|---|---|---|---|
| `witch_hut` (unchanged) | The Old Hut | plains, sunflower plains, forests, birch forests | Granny Hollowell | the 1.0 tower, repaired: real loot, a brewing stand where the spawner was |
| `bog_hut` | Bog Hut | swamp | Old Mother Mirewort | a crooked hut on stilts with a jetty, a mossy chimney and a toadstool island |
| `mangrove_roost` | Mangrove Roost | mangrove swamp | Auntie Silt | a two-storey round tower on mangrove roots with a wraparound balcony |
| `hedge_cottage` | Hedge Cottage | meadow, flower forest, cherry grove | Hedgewife Tansy | a thatched cottage with a hedged herb garden, berry rows and beehives |
| `nightshade_circle` | Nightshade Circle | dark forest, pale garden | Mother Nightshade | standing stones and a trilithon around a great cauldron on a blackstone dais, soul fires, and her crooked hut |
| `rime_hut` | Rime Hut | snowy taiga, snowy plains, grove | Frostwife Isolde | a half-buried log cabin with a powder-snow cauldron and an ice altar |
| `bone_hut` | Bone Hut | badlands, desert | Crone Ossia | terracotta and bone, with a ribcage door, skulls on poles, a lava cauldron and cactus |

**Placement:**
- The structure type is `better_witch_huts:hut`, adapted from Fisherman's Coast's shore-aware type.
- It samples the hut's footprint before placing it, in every rotation and at a few spots around the chunk's centre.
- Land huts need dry ground within 2 to 3 blocks of level. Swamp huts need shallow water or mud.
- The terrain around a hut must not rise above its roof line.
- `beard_thin` blends the ground in.
- Every hut can be found with `/locate structure #better_witch_huts:coven_huts`.

### Coven Standing

Each player has a standing from −100 to 100 with the witches of the world. It is saved per player in the world folder.

| Tier | Standing | What it means |
|---|---|---|
| Hunted | ≤ −50 | residents refuse everything and fight you; wild witches hex you more often (+25 %) |
| Distrusted | −49 to −10 | no pacts; remedies cost 16 emeralds |
| Stranger | −9 to 19 | 1 pact slot; remedies cost 8 |
| Known | 20 to 49 | 1 slot; Poppets for sale; remedies cost 6 |
| Trusted | 50 to 79 | 2 slots; remedies cost 4 |
| Kin | ≥ 80 | 3 slots; free remedies; wild witches never target or harm you |

**How standing moves:**

| Action | Change |
|---|---|
| Hand in an errand | +8 to +15 |
| Offerings | +1 per 2 witch reagents, at most +6 per in-game day |
| Break a hex by doing its task | +3 |
| Kill a wild witch (raiders exempt) | −4 |
| Strike a resident | −5, once per 3 seconds |
| Abandon an errand | −2 |
| Undo a pact | −5 |
| Kill a resident | −40, plus a guaranteed hex |

Every number is in the config.

### Pacts: every gift has a price

- Each resident offers **three pacts a day**. The draft is seeded by witch, player and day, so reopening never
  rerolls it.
- Click **Strike** and the pact is yours until you pay a resident to undo it (24 emeralds and −5 standing).
- Pacts show as sigils among your status effects.
- **Death, milk and `/effect clear` cannot remove them**: the record is the truth, and the effect comes back within a
  second.

| Pact | Gift | Price |
|---|---|---|
| Leech | melee hits heal 20 % of the damage dealt | −4 max health |
| Night Hunter | +35 % melee damage at night and underground | −20 % under the daytime sky |
| Toad | +40 % jump height, +6 safe-fall distance | burning lasts twice as long |
| Stoneskin | +4 armour, +2 toughness | −12 % speed |
| Hare | +18 % speed, step up full blocks | +50 % fall damage |
| Magpie | +3 Luck, remedies a quarter cheaper | −2 max health |
| Witchfire | melee hits ignite targets for 4 s | −25 % melee damage while wet |
| Moth | permanent night vision | −10 % speed and −20 % mining speed under the daytime sky |
| Wolf | every kill restores 2 hunger and saturation | hunger drains 50 % faster |
| Briar | melee attackers take 30 % of their damage back | −10 % attack speed |
| Raven | −20 % gravity, +3 safe fall, +1 reach | −25 % armour |
| Undying | once per in-game day, a killing blow leaves you at 2 hearts with Regeneration II | −6 max health |

**How the gifts and prices work:**
- Fixed attribute changes are carried by the status effect itself.
- Changes that depend on the moment (night and day, wetness, Moth's night vision, hunger drain) are transient modifiers,
  updated once a second.
- Combat gifts hook the loaders' damage and death events: Fabric `ServerLivingEntityEvents`, NeoForge
  `LivingIncomingDamageEvent`, `LivingDamageEvent.Post` and `LivingDeathEvent`.

### Hexes

A hex is a curse that stays **through death and milk** until it is broken. A player carries at most three.

**Where hexes come from:**
- **Wild witches** (and angry residents) channel a hex at a player they fight. Purple motes circle the witch for 1.5
  seconds, then the hex flies if she can still see you.
- **Hitting her while she channels breaks the spell.**
- The chance is 35 %, +15 % at night, +25 % more under a full moon, and +25 % if you are Hunted. The cooldown is 14 to
  26 s.
- **Killing a resident** always lays one (*the Coven will remember*).

**How to break one:**
- **Do its task.** Each hex rolls one of two tasks when it lands. Progress is read from vanilla statistics, so it works
  on both loaders and counts what other mods do too.
- **Smash a Witch Bottle** to break the oldest hex.
- **Pay a resident** (the price depends on your tier).
- **A Poppet** carried in the inventory catches the next hex cast at you, then burns.

| Hex | Effect | Tasks (one is rolled) |
|---|---|---|
| Leaden Limbs | −15 % speed, −25 % jump | swim 150 blocks · sprint 600 blocks |
| Gnawing Hunger | hunger drains twice as fast | eat 10 meals · catch 5 fish |
| Black Cat | −4 Luck | ring a bell 3 times · trade with villagers 3 times |
| Brittle Bones | double fall damage, one block sooner | slay 3 skeletons · slay 12 creatures |
| Withering Grip | −20 % melee damage, −10 % attack speed | slay 12 creatures · slay 6 zombies |
| Insomnia | you cannot sleep | place 6 candles · slay 2 phantoms |
| Frail Heart | −6 max health | breed 3 animals · eat a golden apple |
| Clumsy Fingers | −25 % mining speed, −1 reach | mine 48 stone or deepslate · chop 24 logs |

### Errands, offerings and remedies

- **Errands:**
  - One errand at a time per player, offered by each resident (a new one each day).
  - 12 deliveries (spider eyes, toadstools, phantom membranes, amethyst…) and 8 kill or fishing jobs (drowned,
    creepers, endermen, illagers — "the Witch-Burners" —…).
  - Hand it in at **any** resident for 4 to 12 emeralds, 8 to 15 standing and a 25 % chance of a Witch Bottle.
- **Offerings:** the reagents witches carry (spider eyes, sugar, glowstone, redstone, glass bottles, fermented spider
  eyes, nether wart). Two items give one standing, up to six a day.
- **Remedies:**
  - Lift a hex: price by tier.
  - Buy a Witch Bottle: 6 emeralds.
  - Buy a Poppet: 10 emeralds, from Known.

### Items

| Item | How to get it | What it does |
|---|---|---|
| Grimoire | book + spider eye + feather + ink sac; hut chests | opens your ledger anywhere: standing, pacts, hexes and tasks, errand, record |
| Witch Bottle | glass bottle + 2 iron nuggets + sweet berries + redstone; residents; chests; errands | right-click to smash it and break your oldest hex |
| Poppet | string, wheat and an amethyst shard; residents (Known+); rare in chests | carried, it catches the next hex cast at you |

### The Coven screen

A hand-drawn screen. There is no custom networking: the server sends the state as menu data values and the screen
sends button clicks, both through vanilla container packets, so a server and client on the same version always agree.

**At a resident (four tabs):**
- **Pacts:** tonight's three offers with icon, gift and price, a tooltip with the lore, Strike; your pacts with Undo.
- **Hexes:** each hex's effect, its task with a progress bar, Lift.
- **Errand:** accept, track, hand in, abandon.
- **Remedies:** Witch Bottle, Poppet, offerings.

**From the Grimoire:** the same tabs, read-only, plus **Record** (pacts struck, hexes broken, errands done, huts
visited, what your tier means and where the next one starts).

## 6. Compatibility

- **No dependencies.**
- **YUNG's Better Witch Huts** and other swamp-hut mods replace the vanilla hut. Witchbound adds separate huts, so they
  coexist.
- **Remnants mods:** the systems do not overlap with RAS, Runeborn, RPG Mobs, Skills and Powers or Fellowship.
  - Pact attributes stack with RAS points like any other modifier.
  - RPG Mobs levels wild witches as usual. Residents are a separate entity type and can be added to its tags.
- **Raids:** witches in a raid are exempt from the standing penalty and from Kin peace.
- **Data:** structures, loot, biomes and spacing are datapack data under `data/better_witch_huts/`. Pacts, hexes and
  errands are code with ids, and can be disabled in the config.

## 7. Versions and build

| Minecraft | Loaders | Java |
|---|---|---|
| 26.1.2 | Fabric, NeoForge | 25 |
| 26.2 | Fabric, NeoForge | 25 |
| 26.3 | Fabric, NeoForge | 25 (Gradle itself runs on 25 for Loom 1.18) |

1.20.1 and 1.21.1 keep 1.0 on the listing. A port would mean rewriting the screen (the old `GuiGraphics` API), effect
holders, saved values and the 26.x structure piece; that is roadmap item 6.

## 8. Roadmap

1. **Witch variants:** biome witches (bog, frost, bone, ash) with their own potions and hexes. This is the empty
   witch-mob niche.
2. **The Sabbath:** on full moons the Coven gathers at the Nightshade Circle. Kin are welcome; the Hunted face the Coven
   Mother.
3. **Herbalism:** wild mandrake, wolfsbane and nightshade, a mortar, and brewed remedies that cure hexes.
4. **Familiars:** bond a cat, frog, raven or fox for small passives that grow with your standing.
5. **Allegiance:** generalise Coven Standing into faction reputation for villagers, illagers and piglins (market
   analysis idea 5).
6. **1.21.1 NeoForge and Fabric**, for the Remnants modpack baseline.
