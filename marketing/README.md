# Marketing

Listing copy and artwork for CurseForge and Modrinth.

| Path | What it is |
|------|------------|
| `curseforge/description.md` | The project description (Markdown; also works on Modrinth). Images load from this repo's `main` branch. |
| `curseforge/summary.txt` | The one-line summary (under 250 characters). |
| `images/banner.png` | 1800×600 header banner. |
| `images/icon.png` | 512×512 project icon (also the mod's in-jar `logo.png`). |
| `images/card-*.png` | 1600×900 feature cards used in the description and as gallery images. |
| `images/header-*.png`, `images/divider.png` | Transparent section headers and a divider for the description. |
| `images/screenshots/*.jpg` | Real in-game captures (Minecraft 26.2, Fabric) used by the cards and the gallery. |
| `source/render.cjs` | Renders every PNG above from the screenshots, the mod's textures and the bundled fonts. |
| `source/config-excerpt.json` | The config file as the mod writes it on first start, shown on the config card. |
| `source/fonts/` | Cinzel, Cinzel Decorative, IM Fell English, Barlow and Barlow Condensed, all under the SIL Open Font License (licence files included). |

**The look:** a coven by candlelight.
- Night and aubergine gradients under a faint star field, with a candle-gold glow in the top corner.
- Cinzel Decorative for the wordmark, Cinzel for titles and IM Fell English italics for the witches' sayings, over
  clean Barlow text.
- Gold rules and ✦ marks.
- Gifts are always **witch green** and prices and hexes **blood red**. Each tier of Coven Standing keeps the colour it
  has on the in-game standing bar.
- The pixel-art sigils come straight from the mod's status-effect textures.
- The icon is the Old Hut's curled witch-hat roof in a gold frame, with the Grimoire and the Undying sigil.

## Regenerating the images

Needs Node.js and Playwright with Chromium:

```bash
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs               # everything
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs card-pacts    # one asset
```

The release name and the supported Minecraft versions and loaders are at the top of `render.cjs`, as are the pact,
hex, tier and hut tables the cards draw from. Supporting a new Minecraft version means adding one line there, plus one
row to the download table in `description.md`. After changing the icon, copy `images/icon.png` to every workspace's
`common/src/main/resources/logo.png`.

## Retaking the screenshots

`tools/qa/marketing_shots.py` captures the hut screenshots from a dev client:
- a 1920×1080 window under Xvfb (`Xvfb :1 -screen 0 1920x1080x24`)
- a 26.2 Fabric dev server with a world named `showcase`: normal world type, the QA seed `1928374655`,
  `view-distance=12` and the default config

```bash
tools/qa/devrun.sh prepare-server 26.2 fabric showcase
sed -i 's/view-distance=8/view-distance=12/' 26.2/fabric/runs/server/server.properties
rm 26.2/fabric/runs/server/config/better_witch_huts.json     # the default config instead of the fast QA one
tools/qa/devrun.sh start-server 26.2 fabric && DISPLAY=:1 tools/qa/devrun.sh start-client 26.2 fabric
python3 tools/qa/rcon.py "op Builder"
python3 tools/qa/marketing_shots.py                 # or a few: rime_hut bog_hut
```

Every hut stands where the world generated it, found with `/locate` and untouched. The camera is a spectator circling
the resident witch, with four views per hut; the chosen view is copied to `screenshots/<hut>.jpg`. Every screenshot is
a single unedited frame.

| Screenshot | Scene |
|------------|-------|
| `old_hut.jpg` | The Old Hut (the 1.0 hut, repaired) in a forest by the sea: the curled deepslate witch-hat roof. |
| `bog_hut.jpg` | The Bog Hut on its stilts over swamp water, with the jetty and lanterns. |
| `mangrove_roost.jpg` | The Mangrove Roost's round tower rising above the mangrove canopy. |
| `hedge_cottage.jpg` | The Hedge Cottage on a meadow slope with its hedged herb garden and beehives. |
| `nightshade_circle.jpg` | The Nightshade Circle from above: the standing stones, the dais and Mother Nightshade's roof in a dark forest. |
| `rime_hut.jpg` | The Rime Hut in a snowy taiga, with its ice altar and chimney smoke. |
| `bone_hut.jpg` | The Bone Hut on a terracotta band in the badlands, with skulls on poles and cactus. |
| `menu_pacts.jpg` | Hedgewife Tansy's Coven screen, opened with a real right-click inside her cottage, hovering the Pact of the Leech. |
| `menu_struck.jpg` | The same screen after a real click on Strike (Pact of the Undying held, offers rerolled, the slot is full). |
| `menu_hexes.jpg`, `menu_errand.jpg`, `menu_remedies.jpg` | Her other three tabs, with two hexes laid by `/witchbound hex`. |
| `ledger_hexes.jpg`, `ledger_record.jpg` | The Grimoire, opened with a real right-click. |
| `hex_channel.jpg`, `hex_landed.jpg` | A wild witch on a flat test pad at dusk, channelling a hex (purple motes around her head), and the next frame, when it lands. Night vision on the camera. |
