// Renders the listing artwork in marketing/images/ from real in-game captures (marketing/images/screenshots/), the
// mod's own textures (tools/assets/textures) and the bundled OFL fonts.
// Usage (from the repository root): NODE_PATH="$(npm root -g)" node marketing/source/render.cjs [name...]
// Requires the `playwright` package with a Chromium build available.
const path = require('path');
const fs = require('fs');
const { chromium } = require('playwright');

// The release and the supported Minecraft versions. Adding a version is one line here
// (plus one row in the download table of curseforge/description.md).
const RELEASE = { version: '2.0', name: 'Every Gift Has a Price' };
const VERSIONS = [
  { minecraft: '26.1.2', loaders: ['Fabric', 'NeoForge'] },
  { minecraft: '26.2', loaders: ['Fabric', 'NeoForge'] },
  { minecraft: '26.3', loaders: ['Fabric', 'NeoForge'] },
];
const LOADERS = ['Fabric', 'NeoForge'].filter((loader) => VERSIONS.some((v) => v.loaders.includes(loader)));

// A coven by candlelight: night and aubergine, candle gold, witch-green gifts and blood-red prices.
const C = {
  night: '#0d0814', aubergine: '#1e1028', plum: '#3a1d4a', gold: '#e8b75a', parchment: '#efe3c8',
  gift: '#8fdc6a', price: '#e0566a', moon: '#b9c3ff', frost: '#9fd8f0', bone: '#e8dfc8', ember: '#e8763a', moss: '#7fae5a',
};
const TIERS = [
  ['Hunted', '≤ −50', '#9b1c22', 'residents refuse you; witches hex more'],
  ['Distrusted', '−49…−10', '#c4553f', 'no pacts; remedies 16'],
  ['Stranger', '−9…19', '#8c8592', '1 pact; remedies 8'],
  ['Known', '20…49', '#62b553', 'Poppets for sale; remedies 6'],
  ['Trusted', '50…79', '#4fb8bd', '2 pacts; remedies 4'],
  ['Kin', '≥ 80', '#cf72e3', '3 pacts; free remedies; wild witches leave you be'],
];
const PACTS = [
  ['leech', 'Leech', 'Melee hits heal 20%', '−2 hearts'],
  ['night_hunter', 'Night Hunter', '+35% damage at night', '−20% in daylight'],
  ['toad', 'Toad', '+40% jump, softer falls', 'burn twice as long'],
  ['stoneskin', 'Stoneskin', '+4 armor, +2 toughness', '−12% speed'],
  ['hare', 'Hare', '+18% speed, step up', '+50% fall damage'],
  ['magpie', 'Magpie', '+3 Luck, cheaper remedies', '−1 heart'],
  ['witchfire', 'Witchfire', 'hits set foes alight', '−25% damage when wet'],
  ['moth', 'Moth', 'permanent night vision', 'slow in daylight'],
  ['wolf', 'Wolf', 'kills feed you', 'hunger 50% faster'],
  ['briar', 'Briar', 'reflect 30% of melee', '−10% attack speed'],
  ['raven', 'Raven', 'light as a feather, +1 reach', '−25% armor'],
  ['undying', 'Undying', 'cheat death once a day', '−3 hearts'],
];
const HEXES = [
  ['leaden_limbs', 'Leaden Limbs', '−15% speed, −25% jump', 'swim 150 · sprint 600'],
  ['gnawing_hunger', 'Gnawing Hunger', 'hunger drains ×2', 'eat 10 meals · catch 5 fish'],
  ['black_cat', 'Black Cat', '−4 Luck', 'ring bells · trade'],
  ['brittle_bones', 'Brittle Bones', 'double fall damage', 'slay skeletons · slay 12'],
  ['withering_grip', 'Withering Grip', '−20% melee damage', 'slay 12 · slay zombies'],
  ['insomnia', 'Insomnia', 'you cannot sleep', 'place candles · slay phantoms'],
  ['frail_heart', 'Frail Heart', '−3 hearts', 'breed 3 · golden apple'],
  ['clumsy_fingers', 'Clumsy Fingers', '−25% mining, −1 reach', 'mine 48 stone · chop 24 logs'],
];
const HUTS = [
  ['old_hut.jpg', 'The Old Hut', 'Plains & forests', 'Granny Hollowell', C.ember],
  ['bog_hut.jpg', 'Bog Hut', 'Swamps', 'Old Mother Mirewort', C.moss],
  ['mangrove_roost.jpg', 'Mangrove Roost', 'Mangrove swamps', 'Auntie Silt', '#c46b5a'],
  ['hedge_cottage.jpg', 'Hedge Cottage', 'Meadows & flower forests', 'Hedgewife Tansy', C.gift],
  ['nightshade_circle.jpg', 'Nightshade Circle', 'Dark forests', 'Mother Nightshade', '#a77bd6'],
  ['rime_hut.jpg', 'Rime Hut', 'Snowy taigas', 'Frostwife Isolde', C.frost],
  ['bone_hut.jpg', 'Bone Hut', 'Badlands & deserts', 'Crone Ossia', C.bone],
];

const root = path.resolve(__dirname, '..', '..');
const out = path.join(root, 'marketing', 'images');
const shots = path.join(out, 'screenshots');
const fonts = path.join(__dirname, 'fonts');
const textures = path.join(root, 'tools', 'assets', 'textures');
const url = (file) => 'file://' + file;
const shot = (name) => url(path.join(shots, name));
const tex = (name) => url(path.join(textures, name + '.png'));
const font = (name) => url(path.join(fonts, name));

const css = `
@font-face { font-family: Deco; font-weight: 900; src: url('${font('cinzel-decorative-900.woff2')}'); }
@font-face { font-family: Display; font-weight: 700; src: url('${font('cinzel-700.woff2')}'); }
@font-face { font-family: Display; font-weight: 900; src: url('${font('cinzel-900.woff2')}'); }
@font-face { font-family: Fell; font-style: italic; src: url('${font('im-fell-italic.woff2')}'); }
@font-face { font-family: Fell; font-style: normal; src: url('${font('im-fell-400.woff2')}'); }
@font-face { font-family: Body; font-weight: 500; src: url('${font('barlow-500.woff2')}'); }
@font-face { font-family: Body; font-weight: 700; src: url('${font('barlow-700.woff2')}'); }
@font-face { font-family: Body; font-weight: 800; src: url('${font('barlow-800.woff2')}'); }
@font-face { font-family: Cond; font-weight: 700; src: url('${font('barlow-condensed-700.woff2')}'); }
* { box-sizing: border-box; }
html, body { margin: 0; background: ${C.night}; color: ${C.parchment}; font-family: Body, sans-serif; font-weight: 700; }
.stage { position: relative; overflow: hidden;
         background: radial-gradient(ellipse at 85% -15%, rgba(232,183,90,.20) 0%, rgba(232,183,90,0) 45%),
                     radial-gradient(ellipse at 0% 110%, rgba(143,220,106,.10) 0%, rgba(143,220,106,0) 50%),
                     linear-gradient(165deg, ${C.plum} 0%, ${C.aubergine} 45%, ${C.night} 100%); }
.deco { font-family: Deco, serif; font-weight: 900; letter-spacing: .01em; }
.display { font-family: Display, serif; font-weight: 900; letter-spacing: .02em; }
.title { background: linear-gradient(180deg, #fff6e2 0%, ${C.parchment} 45%, ${C.gold} 100%);
         -webkit-background-clip: text; background-clip: text; color: transparent; }
.shadow { filter: drop-shadow(0 4px 0 rgba(8,4,12,.92)) drop-shadow(0 0 30px rgba(232,183,90,.25)); }
.kicker { font-family: Cond, sans-serif; color: ${C.gold}; letter-spacing: .26em; font-size: 24px; text-transform: uppercase; }
.fell { font-family: Fell, serif; font-style: italic; font-weight: 400; }
.stars { position: absolute; inset: 0; opacity: .5;
         background-image: radial-gradient(1.5px 1.5px at 20px 30px, #fff 50%, transparent 51%),
                           radial-gradient(1px 1px at 120px 80px, #e8dcff 50%, transparent 51%),
                           radial-gradient(1.5px 1.5px at 210px 160px, #fff 50%, transparent 51%),
                           radial-gradient(1px 1px at 300px 40px, #fff 50%, transparent 51%);
         background-size: 340px 220px; }
.moon { position: absolute; width: 520px; height: 520px; right: -170px; top: -250px; border-radius: 50%;
        background: radial-gradient(circle, rgba(232,183,90,.45) 0%, rgba(232,183,90,.07) 55%, rgba(232,183,90,0) 70%); }
.backdrop { position: absolute; inset: -40px; background-size: cover; background-position: center;
            filter: blur(14px) brightness(.26) saturate(1.2) hue-rotate(-10deg); }
.frame { position: relative; overflow: hidden; background: #000; border-radius: 14px; border: 5px solid ${C.gold};
         box-shadow: 0 0 0 3px rgba(8,4,12,.92), 0 26px 60px rgba(0,0,0,.65); }
.frame img.fill { display: block; width: 100%; height: 100%; object-fit: cover; }
.rule { height: 4px; border-radius: 2px; background: linear-gradient(90deg, transparent, ${C.gold} 18%, ${C.gold} 82%, transparent); }
.label { position: absolute; display: inline-flex; align-items: center; gap: 10px; padding: 7px 16px 6px; border-radius: 999px;
         font-family: Cond, sans-serif; font-size: 19px; letter-spacing: .1em; text-transform: uppercase; color: ${C.night};
         background: ${C.parchment}; box-shadow: 0 6px 18px rgba(0,0,0,.5); white-space: nowrap; }
.dot { display: inline-block; width: 12px; height: 12px; border-radius: 50%; }
ul.points { list-style: none; padding: 0; margin: 0; }
ul.points li { position: relative; padding-left: 38px; margin: 0 0 15px; font-size: 26px; line-height: 1.3; color: #ece2d2; font-weight: 500; }
ul.points li::before { content: '✦'; position: absolute; left: 0; top: -1px; font-size: 24px; color: var(--accent, ${C.gold});
                       text-shadow: 0 0 12px var(--accent, ${C.gold}); }
b.hi { color: #fff; font-weight: 800; }
b.g { color: ${C.gift}; font-weight: 800; }
b.r { color: ${C.price}; font-weight: 800; }
.stats { display: flex; margin-top: 22px; border-radius: 14px; overflow: hidden; background: rgba(13,8,20,.9);
         border: 3px solid rgba(232,183,90,.3); box-shadow: 0 18px 50px rgba(0,0,0,.5); }
.stats > div { flex: 1; padding: 12px 20px 10px; border-left: 3px solid rgba(232,183,90,.15); }
.stats > div:first-child { border-left: 0; }
.stats .t { font-family: Cond, sans-serif; font-size: 16px; letter-spacing: .16em; color: #b8a9c6; text-transform: uppercase; }
.stats .v { font-family: Display, serif; font-weight: 900; font-size: 30px; color: ${C.parchment}; margin-top: 2px; white-space: nowrap; }
.pill { display: inline-block; border-radius: 999px; padding: 6px 20px 5px; margin-right: 10px; font-family: Cond, sans-serif;
        font-size: 23px; letter-spacing: .08em; color: ${C.night}; background: ${C.gold}; }
.px { image-rendering: pixelated; display: block; }
.tile { display: flex; align-items: center; gap: 14px; padding: 10px 14px; border-radius: 12px;
        background: rgba(13,8,20,.86); border: 2px solid var(--c); box-shadow: 0 10px 26px rgba(0,0,0,.45); }
.tile .n { font-family: Display, serif; font-weight: 900; font-size: var(--n, 21px); color: ${C.parchment}; line-height: 1.05; white-space: nowrap; }
.tile .g { font-size: var(--t, 16px); color: ${C.gift}; line-height: 1.2; font-weight: 700; }
.tile .r { font-size: var(--t, 16px); color: ${C.price}; line-height: 1.2; font-weight: 700; }
.tile .s { font-size: var(--t, 16px); color: #c8bdd4; line-height: 1.2; font-weight: 500; }
`;

const page = (width, height, body, transparent = false) => `<!doctype html><html><head><meta charset="utf-8"><style>${css}
body { width: ${width}px; height: ${height}px; } .stage { width: ${width}px; height: ${height}px; }
${transparent ? 'html, body, .stage { background: transparent; }' : ''}</style></head>
<body><div class="stage">${body}</div></body></html>`;

const framed = (file, w, h, pos = 'center', style = '') =>
  `<div class="frame" style="width:${w}px;height:${h}px;${style}"><img class="fill" src="${shot(file)}" style="object-position:${pos}"></div>`;
// A screen capture zoomed onto the Coven screen in the middle of a 1920x1080 frame.
const screen = (file, w, h, style = '', crop = [470, 190, 980, 700]) =>
  `<div class="frame" style="width:${w}px;height:${h}px;${style}"><img src="${shot(file)}" style="position:absolute;width:${Math.round(w * 1920 / crop[2])}px;left:${-Math.round(w * crop[0] / crop[2])}px;top:${-Math.round(h * crop[1] / crop[3])}px"></div>`;
const label = (text, color, pos) => `<span class="label" style="${pos}"><span class="dot" style="background:${color}"></span>${text}</span>`;
const stats = (items) => `<div class="stats">${items.map(([t, v, flex]) =>
  `<div${flex ? ` style="flex:${flex}"` : ''}><div class="t">${t}</div><div class="v">${v}</div></div>`).join('')}</div>`;
const sigil = (name, size, style = '') => `<img class="px" src="${tex('mob_effect/' + name)}" style="width:${size}px;height:${size}px;${style}">`;
const item = (name, size, style = '') => `<img class="px" src="${tex('item/' + name)}" style="width:${size}px;height:${size}px;${style}">`;

const card = ({ kicker, title, points, art, width = 1600, height = 900, artWidth = 940, backdrop, color = C.gold, quote }) => page(width, height, `
  <div class="backdrop" style="background-image:url('${shot(backdrop)}')"></div><div class="stars"></div><div class="moon"></div>
  <div style="position:absolute;left:74px;top:0;bottom:0;width:${width - artWidth - 146}px;display:flex;flex-direction:column;justify-content:center;--accent:${color}">
    <div class="kicker" style="color:${color}">${kicker}</div>
    <div class="display title shadow" style="font-size:60px;line-height:1.06;margin:12px 0 20px">${title}</div>
    <div class="rule" style="width:240px;margin-bottom:28px"></div>
    <ul class="points">${points.map((point) => `<li>${point.replace(/ (\S+)$/, '&nbsp;$1')}</li>`).join('')}</ul>
    ${quote ? `<div class="fell" style="font-size:28px;color:#cbb88f;margin-top:10px">“${quote}”</div>` : ''}
  </div>
  <div style="position:absolute;right:62px;top:50%;transform:translateY(-50%);width:${artWidth}px">${art}</div>`);

const versionText = VERSIONS.map((v) => v.minecraft).join(' · ');

const configPanel = () => {
  const esc = (t) => t.replace(/&/g, '&amp;').replace(/</g, '&lt;');
  const text = fs.readFileSync(path.join(__dirname, 'config-excerpt.json'), 'utf8').split('\n').slice(0, 27).map((line) => {
    if (/^\s*\/\//.test(line)) return `<span style="color:#9b8fae">${esc(line)}</span>`;
    return esc(line).replace(/("[^"]+")(:)/g, `<span style="color:${C.gold}">$1</span>$2`)
      .replace(/: (true|false|-?[\d.]+|"[^"]*")/g, `: <span style="color:${C.gift}">$1</span>`);
  }).join('\n');
  return `<pre style="margin:0;padding:18px 22px;font-size:13.5px;line-height:1.5;color:#e9e0d2;white-space:pre;font-family:'DejaVu Sans Mono',monospace">${text}</pre>`;
};

const header = (title, color = C.gold) => page(1400, 120, `
  <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:22px">
    <div class="rule" style="width:230px"></div>
    <span style="font-size:30px;color:${color};text-shadow:0 0 14px ${color}">✦</span>
    <div class="display title shadow" style="font-size:54px;white-space:nowrap">${title}</div>
    <span style="font-size:30px;color:${color};text-shadow:0 0 14px ${color}">✦</span>
    <div class="rule" style="width:230px"></div>
  </div>`, true);

const divider = () => page(1400, 50, `
  <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:14px">
    <div class="rule" style="width:430px;opacity:.8"></div>
    ${['pact_leech', 'pact_moth', 'hex_insomnia', 'pact_witchfire', 'pact_undying'].map((s) => sigil(s, 36)).join('')}
    <div class="rule" style="width:430px;opacity:.8"></div>
  </div>`, true);

const assets = {
  banner: () => page(1800, 600, `
    <img src="${shot('old_hut.jpg')}" style="position:absolute;right:-80px;top:-40px;width:1500px;filter:saturate(1.1) brightness(.92) hue-rotate(-8deg);-webkit-mask-image:linear-gradient(90deg,transparent 0%,#000 34%);mask-image:linear-gradient(90deg,transparent 0%,#000 34%)">
    <div style="position:absolute;inset:0;background:linear-gradient(90deg,rgba(13,8,20,.98) 0%,rgba(20,10,30,.94) 34%,rgba(30,16,40,.35) 62%,rgba(13,8,20,0) 82%)"></div>
    <div style="position:absolute;inset:0;background:linear-gradient(0deg,rgba(13,8,20,.85) 0%,rgba(13,8,20,0) 32%)"></div>
    <div class="stars" style="-webkit-mask-image:linear-gradient(90deg,#000 0%,#000 40%,transparent 60%);mask-image:linear-gradient(90deg,#000 0%,#000 40%,transparent 60%)"></div>
    <div style="position:absolute;left:84px;top:52px;width:1120px">
      <div class="kicker">Remnants · ${RELEASE.version} · ${RELEASE.name}</div>
      <div class="deco title shadow" style="display:inline-block;font-size:124px;line-height:1.02;margin:10px 0 14px;padding-right:14px;white-space:nowrap">Witchbound</div>
      <div class="rule" style="width:600px;margin-bottom:22px"></div>
      <div style="font-size:33px;color:${C.parchment};line-height:1.3;font-weight:800">Strike pacts with witches. Carry their hexes past death.<br>Earn your standing with the Coven, across seven coven huts.</div>
      <div style="margin-top:26px;display:flex;align-items:center">${LOADERS.map((l) => `<span class="pill">${l}</span>`).join('')}
        <span style="font-family:Cond,sans-serif;font-size:24px;color:#cdbfdc;margin-left:6px;letter-spacing:.05em">${versionText}</span>
        <span style="display:flex;gap:6px;margin-left:28px">${['pact_leech', 'pact_night_hunter', 'pact_moth', 'pact_witchfire', 'pact_undying', 'hex_black_cat', 'hex_insomnia'].map((s) => sigil(s, 40)).join('')}</span></div>
    </div>`),

  icon: () => page(512, 512, `
    <div style="position:absolute;inset:0;border-radius:96px;overflow:hidden;background:${C.night}">
      <img src="${shot('old_hut.jpg')}" style="position:absolute;width:1300px;left:-500px;top:-30px;filter:saturate(1.15) contrast(1.06) brightness(.95)">
      <div style="position:absolute;inset:0;background:radial-gradient(ellipse at 50% 40%,rgba(30,16,40,0) 0%,rgba(30,16,40,.3) 55%,rgba(13,8,20,.92) 100%)"></div>
      ${item('grimoire', 168, 'position:absolute;right:26px;bottom:22px;filter:drop-shadow(0 8px 0 rgba(8,4,12,.9))')}
      ${sigil('pact_undying', 96, 'position:absolute;left:30px;bottom:34px;filter:drop-shadow(0 6px 0 rgba(8,4,12,.9))')}
    </div>
    <div style="position:absolute;inset:0;border-radius:96px;border:12px solid ${C.gold};box-shadow:inset 0 0 0 4px rgba(8,4,12,.8)"></div>`, true),

  'card-huts': () => card({
    kicker: 'The world', title: 'Seven coven huts,<br>seven witches', color: C.gold, backdrop: 'old_hut.jpg',
    points: ['One hut for every kind of land: swamp, mangrove, meadow, dark forest, snow and badlands',
      'A <b class="hi">named resident witch</b> in each, who never despawns and never joins raids',
      'Loot tables in every chest: potions, reagents, Witch Bottles, Grimoires', 'Placed only on ground that suits them'],
    artWidth: 960,
    art: `<div style="display:grid;grid-template-columns:1fr 1fr 1fr 1fr;gap:12px 12px">
      ${HUTS.map(([file, name, where, who, c]) => `<div>${framed(file, 231, 150, 'center', `border-color:${c};border-width:4px`)}
        <div style="margin-top:5px;font-family:Display,serif;font-weight:900;font-size:18px;color:${C.parchment};white-space:nowrap">${name}</div>
        <div style="font-family:Cond,sans-serif;font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:${c}">${where}</div>
        <div class="fell" style="font-size:16px;color:#cbb88f">${who}</div></div>`).join('')}
      <div class="tile" style="--c:${C.gold};flex-direction:column;justify-content:center;text-align:center;height:150px">
        ${sigil('pact_magpie', 56)}<div class="n" style="font-size:18px;white-space:normal">The Seven Covens</div>
        <div class="s" style="font-size:14px">visit all seven</div></div>
      </div>` }),

  'card-pacts': () => card({
    kicker: 'The headline', title: 'Every gift<br>has a price', color: C.gift, backdrop: 'menu_pacts.jpg',
    points: ['Each resident offers <b class="hi">three pacts a night</b>', 'Strike one: keep the <b class="g">Gift</b>, pay the <b class="r">Price</b>, for good',
      'Pacts survive <b class="hi">death and milk</b>; only a witch can undo one', 'One pact as a Stranger, <b class="hi">three as Kin</b>'],
    quote: 'Death keeps a ledger. She will collect.',
    art: `<div style="position:relative">${screen('menu_pacts.jpg', 940, 556, `border-color:${C.gift}`, [470, 190, 1290, 763])}${label('Hedgewife Tansy’s offers, in her cottage', C.gift, 'left:18px;bottom:18px')}</div>` }),

  'card-pact-list': () => page(1600, 900, `
    <div class="backdrop" style="background-image:url('${shot('hedge_cottage.jpg')}')"></div><div class="stars"></div><div class="moon"></div>
    <div style="position:absolute;left:0;right:0;top:46px;text-align:center">
      <div class="kicker" style="color:${C.gift}">Twelve pacts</div>
      <div class="display title shadow" style="font-size:56px;margin-top:8px">Choose your bargain</div>
    </div>
    <div style="position:absolute;left:60px;right:60px;top:180px;display:grid;grid-template-columns:1fr 1fr 1fr;gap:20px;--n:26px;--t:20px">
      ${PACTS.map(([id, name, gift, price]) => `<div class="tile" style="--c:rgba(232,183,90,.45);padding:16px 18px;gap:18px">${sigil('pact_' + id, 90)}
        <div><div class="n">${name}</div><div class="g">✦ ${gift}</div><div class="r">✦ ${price}</div></div></div>`).join('')}
    </div>
    <div class="fell" style="position:absolute;left:0;right:0;bottom:40px;text-align:center;font-size:30px;color:#cbb88f">Gifts in green, prices in red. Both are forever, until a witch says otherwise.</div>`),

  'card-hexes': () => card({
    kicker: 'The threat', title: 'Hexes that<br>follow you', color: C.price, backdrop: 'hex_channel.jpg',
    points: ['Witches you fight <b class="hi">channel a hex</b>: purple motes, then the curse flies',
      '<b class="hi">Hit her mid-spell</b> to break it. Nights and full moons are worse',
      'A hex stays through <b class="r">death and milk</b>', 'Break it by its <b class="hi">task</b>, a Witch Bottle, or a witch’s price',
      'A Poppet takes the next hex for you'],
    art: `<div style="position:relative">${framed('hex_channel.jpg', 940, 529, 'center 46%', `border-color:${C.price}`)}${label('Channelling…', C.price, 'left:18px;top:18px')}
      <div style="position:absolute;right:-26px;bottom:-72px">${framed('hex_landed.jpg', 440, 248, 'left bottom', `border-color:${C.price}`)}</div>
      ${label('…and it lands', C.price, 'right:8px;bottom:190px;font-size:15px')}</div>
      <div style="margin-top:86px;width:470px">${stats([['Hexes', '8'], ['Carry up to', '3']])}</div>` }),

  'card-hex-list': () => page(1600, 900, `
    <div class="backdrop" style="background-image:url('${shot('nightshade_circle.jpg')}')"></div><div class="stars"></div><div class="moon"></div>
    <div style="position:absolute;left:0;right:0;top:46px;text-align:center">
      <div class="kicker" style="color:${C.price}">Eight hexes</div>
      <div class="display title shadow" style="font-size:56px;margin-top:8px">Break them by your own hand</div>
    </div>
    <div style="position:absolute;left:80px;right:80px;top:180px;display:grid;grid-template-columns:1fr 1fr;gap:20px;--n:27px;--t:20px">
      ${HEXES.map(([id, name, effect, tasks]) => `<div class="tile" style="--c:rgba(224,86,106,.5);padding:14px 20px;gap:20px">${sigil('hex_' + id, 90)}
        <div><div class="n">Hex of ${name}</div><div class="r">✦ ${effect}</div><div class="s">Break it: ${tasks}</div></div></div>`).join('')}
    </div>
    <div class="fell" style="position:absolute;left:0;right:0;bottom:40px;text-align:center;font-size:30px;color:#cbb88f">Each hex rolls one task. Progress counts from your own statistics.</div>`),

  'card-coven': () => card({
    kicker: 'Coven Standing', title: 'Earn the trust<br>of witches', color: '#cf72e3', backdrop: 'menu_struck.jpg',
    points: ['Your standing with every witch, from <b class="hi">−100 to 100</b>', 'Rise with <b class="hi">errands</b>, offerings and broken hexes',
      'Fall by killing witches. <b class="r">Kill a resident and the Coven remembers</b>', 'Kin of the Coven: three pacts, free remedies, safe passage'],
    art: `<div style="display:flex;flex-direction:column;gap:12px">
      ${TIERS.map(([name, range, c, perk], i) => `<div class="tile" style="--c:${c};padding:12px 18px">
        <div style="width:${110 + i * 26}px;height:22px;border-radius:6px;background:${c};box-shadow:0 0 14px ${c}"></div>
        <div style="flex:1"><div class="n" style="color:${c}">${name} <span style="font-family:Cond,sans-serif;font-size:17px;color:#b8a9c6;letter-spacing:.08em">${range}</span></div>
        <div class="s" style="font-size:18px">${perk}</div></div></div>`).join('')}
      </div>` }),

  'card-errands': () => card({
    kicker: 'Errands, offerings, remedies', title: 'Work for<br>the Coven', color: C.gold, backdrop: 'menu_errand.jpg',
    points: ['A new <b class="hi">errand</b> from every resident each day: 20 kinds, deliveries and hunts',
      'Hand it in at <b class="hi">any</b> resident for emeralds and standing', '<b class="hi">Offerings</b> of witch reagents raise your standing',
      'Remedies: lift a hex, buy a Witch Bottle or a Poppet'],
    art: `<div style="position:relative">${screen('menu_errand.jpg', 620, 443, `border-color:${C.gold}`)}
      <div style="position:absolute;right:-20px;top:250px">${screen('menu_remedies.jpg', 520, 371, `border-color:${C.gold}`)}</div></div>
      <div style="height:190px"></div>` }),

  'card-grimoire': () => card({
    kicker: 'Your ledger', title: 'The Grimoire', color: C.gold, backdrop: 'ledger_record.jpg',
    points: ['Your standing, pacts, hexes and errand, <b class="hi">anywhere</b>', 'Every hex’s task with a progress bar',
      'Pacts and hexes also show as <b class="hi">sigils</b> on your HUD', 'Witch Bottles break hexes; Poppets catch them'],
    art: `<div style="position:relative">${screen('ledger_hexes.jpg', 700, 500, `border-color:${C.gold}`)}</div>
      <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:14px;margin-top:22px">
      ${[['grimoire', 'Grimoire', 'book + spider eye + feather + ink'], ['witch_bottle', 'Witch Bottle', 'smash: break your oldest hex'], ['poppet', 'Poppet', 'catches the next hex']].map(([i, n, s]) =>
        `<div class="tile" style="--c:rgba(232,183,90,.5);flex-direction:column;text-align:center">${item(i, 84)}<div class="n">${n}</div><div class="s">${s}</div></div>`).join('')}
      </div>` }),

  'card-config': () => card({
    kicker: 'For servers and modpacks', title: 'Your coven,<br>your rules', color: C.gold, backdrop: 'rime_hut.jpg',
    points: ['<b class="hi">config/better_witch_huts.json</b> with comments: pact slots, hexing, standing, prices, each hut',
      'Biomes, spacing and loot are datapack data', '<b class="hi">/witchbound</b> for players; operator tools to read and set any record',
      'No dependencies. Worlds from 1.0 keep loading'],
    art: `<div style="display:flex;flex-direction:column;gap:18px">
      <div class="frame" style="background:#0f0918;border-color:${C.gold}">${configPanel()}</div>
      ${stats([['Loaders', LOADERS.join(' · '), 2], ['Versions', String(VERSIONS.length)]])}</div>` }),
};

const HEADERS = {
  'header-whats-new': ['What’s New in 2.0', C.gold], 'header-huts': ['The Seven Huts', C.ember],
  'header-pacts': ['Pacts', C.gift], 'header-hexes': ['Hexes', C.price], 'header-coven': ['Coven Standing', '#cf72e3'],
  'header-errands': ['Errands and Remedies', C.gold], 'header-grimoire': ['The Grimoire', C.gold],
  'header-config': ['For Servers and Modpacks', C.gold], 'header-downloads': ['Downloads', C.gold],
  'header-faq': ['FAQ', C.gold], 'header-roadmap': ['What Comes Next', C.moon],
};
for (const [name, [title, color]] of Object.entries(HEADERS)) {
  assets[name] = () => header(title, color);
}
assets.divider = divider;

(async () => {
  const wanted = process.argv.slice(2);
  const names = wanted.length ? wanted : Object.keys(assets);
  const browser = await chromium.launch();
  const context = await browser.newContext({ deviceScaleFactor: 1 });
  for (const name of names) {
    if (!assets[name]) {
      console.error('unknown asset', name);
      continue;
    }
    const file = path.join(__dirname, `.render-${name}.html`);
    fs.writeFileSync(file, assets[name]());
    const p = await context.newPage();
    await p.goto(url(file));
    await p.evaluate(() => document.fonts.ready);
    await p.waitForTimeout(250);
    const { width, height } = await p.evaluate(() => ({ width: document.body.scrollWidth, height: document.body.scrollHeight }));
    await p.setViewportSize({ width, height });
    const transparent = name.startsWith('header') || name === 'divider' || name === 'icon';
    await p.screenshot({ path: path.join(out, `${name}.png`), omitBackground: transparent });
    await p.close();
    fs.unlinkSync(file);
    console.log('rendered', name);
  }
  await browser.close();
})();
