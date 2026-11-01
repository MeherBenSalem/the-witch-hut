# QA record: Witchbound 2.0.0

Checked on 2026-11-01 against the jars built from this tree. "Client" means a real game dev client under Xvfb with Mesa
llvmpipe, on a dedicated dev server. Clicks were sent with xdotool, commands went over RCON, and
every result was read back from the server or from a screenshot.

## Builds and tests

| Workspace | `./gradlew build` | JUnit (`common`) |
|---|---|---|
| 1.21.1 | Fabric and NeoForge jars | 15 tests, 0 failures |
| 26.1.2 | Fabric and NeoForge jars | 15 tests, 0 failures |
| 26.2 | Fabric and NeoForge jars | 15 tests, 0 failures |
| 26.3 | Fabric and NeoForge jars | 15 tests, 0 failures |

The unit tests cover:
- the standing tiers and their clamping
- the pact draft: the same offers all day, a new set each day, never a held or disabled pact
- objective, hex and errand definitions, and their id round trips
- record sanitising
- config defaults, clamping, and writing then reading the commented file

## Dedicated servers

| Build | Boot | Huts | Resident in a generated hut | Loot tables |
|---|---|---|---|---|
| Fabric 26.2 | ✓ | all 7 found by `/locate`, and `#better_witch_huts:coven_huts` | Granny Hollowell, Frostwife Isolde, Mother Nightshade, Hedgewife Tansy, each with her `Hut` tag | rolled in game (chests in generated huts) |
| NeoForge 26.2 | ✓ | ✓ | Frostwife Isolde | – |
| Fabric 26.1.2 | ✓ | ✓ | Frostwife Isolde | – |
| Fabric 26.3 | ✓ | ✓ | Frostwife Isolde | all 8 tables rolled with `/loot insert` |
| Fabric 1.21.1 | ✓ | all 7, at the same places as 26.x on the same seed | Frostwife Isolde, Granny Hollowell, names read from JSON | all 8 tables rolled |
| NeoForge 1.21.1 | ✓ | ✓ | – | – |

Issues found and fixed during QA:
- **Residents showed their names as raw JSON.** The game's data fixer does not run on a modded entity, so templates now
  store plain names, and old names are repaired on load.
- **26.3 refused to load the legacy 1.0 template pool**, because the `block_ignore` processor format changed. The pool
  now uses `minecraft:empty`.
- **26.3 renamed loot `functions` to `modifier` and recipe-unlock `recipe` to `recipes`.** The datagen writes the 26.3
  format for that workspace.

## Scripted client checks (`tools/qa/witch_checks.py`)

12 checks, each read back from the server: summon a resident; Strike from the Coven screen (real click); the pact shows
as a status effect; lay two hexes; accept and abandon an errand (−2); an offering of 8 spider eyes (+4); a Witch Bottle
breaks the oldest hex; **control: a wild witch hexes or hurts a Stranger; Kin peace: the same witch neither hexes nor
hurts a Kin player**; **control: the player can sleep without a hex; Insomnia wakes the player**.

| Client | Result |
|---|---|
| Fabric 26.2 | 12/12 |
| NeoForge 26.2 | 12/12 |
| Fabric 1.21.1 | 12/12 |
| NeoForge 1.21.1 | 12/12 |

The script was fixed three times while closing these gaps. None of the failures was the mod:
- It cleared witch loot left on the test pad: the spider eyes counted as offerings.
- It moved each run to a fresh in-game day, so the daily offering cap starts empty.
- It counted "hurt" against the current max health, because pacts like Undying lower it.

The NeoForge and Fabric 1.21.1 passes came before the max-health change, which only removes false failures.

## Exploratory client checks (Fabric 26.2)

| Check | Result |
|---|---|
| Right-click a resident (in reach) opens the Coven screen with her name, standing bar, tabs and three offers | ✓ |
| Hover an offer: Gift, Price and lore tooltip | ✓ |
| Click **Strike** | ✓ Pact of the Magpie held: Luck 3.0, max health 18. Sigil on the HUD, "Every Gift Has a Price" toast, offers rerolled without Magpie, "pacts are full" shown, Undo button enabled |
| Hexes tab after two hexes | ✓ effect, task and progress bar per hex. Lift price 6 with Magpie (8 × 0.75) |
| Errand: accept, deliver 6 amethyst shards, **Hand in** | ✓ shards taken, +6 emeralds, standing 0 → 10 |
| Offering: 8 spider eyes | ✓ taken, standing 10 → 14 |
| Break a hex by its task (Brittle Bones: slay 12 creatures) | ✓ broken after 12 kills, +3 standing |
| A wild witch fights a survival player (QA config: 3 to 4 s cooldown, chance 1.0) | ✓ two hexes landed (Black Cat, Leaden Limbs) |
| Channelling on default settings | ✓ captured: purple motes around her head, then "Witch hexed you: Hex of Clumsy Fingers…" |
| Smash a Witch Bottle | ✓ the oldest hex (Insomnia) removed, bottle consumed |
| Undying: two killing blows in one day | ✓ first survived (health 4 plus regeneration), second killed |
| Respawn | ✓ every pact and hex back as an effect, max health 12 (Magpie −2, Undying −6) |
| `/effect clear` (the same as milk) | ✓ effects back within a second |
| Kill a resident | ✓ standing 17 → −23, grudge hex laid, "Burn the Witch" announced |
| Standing −80 (Hunted) | ✓ "Witch Hunt" announced; right-click a resident gives "Goody Thistle will not deal with you." and no menu |
| Grimoire right-click | ✓ read-only ledger: Pacts, Hexes, Errand, Record (visited huts counted) |
| Hut visits | ✓ "The Old Hut" advancement on entering it |

The 1.21.1 screen (drawn through `Canvas`) was compared with the 26.2 screen capture by capture and matches.
