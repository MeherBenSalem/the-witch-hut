#!/usr/bin/env python3
"""Draws Witchbound's textures: three item icons (16x16) and twenty status-effect sigils (18x18), as pixel art.

  python3 tools/gen_textures.py      (needs Pillow; writes tools/assets/textures/)
"""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image

TOOLS = Path(__file__).resolve().parent
OUT = TOOLS / 'assets' / 'textures'


def hexc(value, alpha=255):
    value = value.lstrip('#')
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (alpha,)


def draw(rows, palette, size=16):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                img.putpixel((x, y), hexc(palette[ch]))
    return img


# Items --------------------------------------------------------------------------------------------------------------
GRIMOIRE = [
    '................',
    '..kkkkkkkkkkkk..',
    '.kddPPPPPPPPPPk.',
    '.kdPllPPPPPPPPwk',
    '.kdPlPPPPgPPPPwk',
    '.kdPPPPPgggPPPwk',
    '.kdPPPPggogPPPwk',
    '.kdPPPgggggPPPwk',
    '.kdPPPPggogPPPwk',
    '.kdPPPPPgggPPGgk',
    '.kdPPPPPPgPPPGgk',
    '.kdPPPPPPPPPPPwk',
    '.kdpPPPPPPPPPPWk',
    '.kddppppppppppWk',
    '..kkkkkkkkkkkkk.',
    '................',
]
GRIMOIRE_PAL = {'k': '1a0f22', 'd': '2e1a3d', 'p': '4b2a63', 'P': '64398a', 'l': '8f62b8', 'g': 'e0b04c', 'G': '9a6b22',
                'o': '5bd6c4', 'w': 'ece0c4', 'W': 'c3b490'}
WITCH_BOTTLE = [
    '................',
    '......kkkk......',
    '......kccK......',
    '......kccK......',
    '.....kkrrkk.....',
    '......kggk......',
    '.....kg..gk.....',
    '....kg.nr..gk...',
    '...kg.n.rn..gk..',
    '...kg..nr.n.gk..',
    '...kg.rn..n.gk..',
    '...kg.nrrnn.gk..',
    '...kgggggggggk..',
    '....kkkkkkkkk...',
    '................',
    '................',
]
WITCH_BOTTLE_PAL = {'k': '22303a', 'K': '3b2a1a', 'c': '9c6b3c', 'g': 'a8d0dc', '.': 'ffffff00', 'n': '8a8f96', 'r': 'b02a2a'}
POPPET = [
    '................',
    '......kkkk......',
    '.....kyyyyk.....',
    '.....kbyybk.....',
    '.....kyyyyk.....',
    '......kssk......',
    '...kkkyyyykkk...',
    '..kyyyyrryyyyk..',
    '...kkkyyrykkk...',
    '.....kyysyk.....',
    '.....kyyyyk.....',
    '.....kysyyk.....',
    '....kyk..kyk....',
    '....kyk..kyk....',
    '....kkk..kkk....',
    '................',
]
POPPET_PAL = {'k': '4a3418', 'y': 'd9b45a', 's': '8a6a2e', 'b': '111111', 'r': 'c0262c'}

# Sigils -------------------------------------------------------------------------------------------------------------
GLYPHS = {
    'leech': ['.....#.....', '....###....', '....###....', '...#####...', '..#######..', '..##x####..', '.##x######.',
              '.#########.', '.#########.', '..#######..', '...#####...'],
    'night_hunter': ['....####...', '..####.....', '.###.......', '.###.......', '###.....#..', '###........', '###...#....',
                     '.###.......', '.###.......', '..####.....', '....####...'],
    'toad': ['.##.....##.', '#x##...#x##', '####...####', '.#########.', '###########', '#xxxxxxxxx#', '###########',
             '.#########.', '..##...##..', '.###...###.', '...........'],
    'stoneskin': ['#.#.###.#.#', '###########', '###########', '###xxxxx###', '###x###x###', '###xxxxx###', '.#########.',
                  '.#########.', '..#######..', '...#####...', '....###....'],
    'hare': ['.##.....##.', '.##.....##.', '.##.....##.', '.##.....##.', '..##...##..', '..#######..', '.#########.',
             '.##x###x##.', '.#########.', '..###x###..', '...#####...'],
    'magpie': ['.....#.....', '....###....', '...##x##...', '..##x#x##..', '.##x###x##.', '##x#####x##', '.##x###x##.',
               '..##x#x##..', '...##x##...', '....###....', '.....#.....'],
    'witchfire': ['.....#.....', '....##.....', '....###....', '...####.#..', '..###x###..', '..##xx###..', '.###xxx###.',
                  '.##xxxxx##.', '.##xxxxx##.', '..##xxx##..', '...#####...'],
    'moth': ['##.......##', '###.....###', '####.#.####', '#####x#####', '.####x####.', '..###x###..', '.####x####.',
             '#####x#####', '####...####', '###.....###', '.#.......#.'],
    'wolf': ['#...#...#..', '##..##..##.', '.##..##..##', '.##..##..##', '..##..##..#', '..##..##..#', '...##..##..',
             '...##..##..', '....#...#..', '....#...#..', '...........'],
    'briar': ['........##.', '.......###.', '......##...', '.#...##....', '..#.##.#...', '...##...#..', '..##.......',
              '.##.#......', '##...#.....', '#..........', '...........'],
    'raven': ['.........##', '........###', '.......####', '......###x#', '.....###x#.', '....###x#..', '...###x#...',
              '..###x#....', '.###x#.....', '.#x#.......', '#.........'],
    'undying': ['..#######..', '.#########.', '###########', '##xx###xx##', '##xx###xx##', '#####x#####', '.#########.',
                '..#######..', '..#.#.#.#..', '..#######..', '...........'],
    # Hexes
    'leaden_limbs': ['...#####...', '..##...##..', '..##...##..', '.#########.', '###########', '###########',
                     '###########', '###########', '.#########.', '..#######..', '...........'],
    'gnawing_hunger': ['###########', '#.#.#.#.#.#', '#.........#', '#.........#', '#.........#', '#.#.#.#.#.#',
                       '###########', '...........', '...........', '...........', '...........'],
    'black_cat': ['#.........#', '##.......##', '###.....###', '###########', '###########', '##x#####x##', '###########',
                  '####x#x####', '.#########.', '..#######..', '...........'],
    'brittle_bones': ['.##........', '####.......', '.####......', '..####.....', '...###.....', '.....x.....', '.....###...',
                      '.....####..', '......####.', '.......####', '........##.'],
    'withering_grip': ['.#.#.#.#...', '.#.#.#.#...', '.#.#.#.#...', '.#######.#.', '.#######.#.', '.#########.', '.########..',
                       '..#######..', '...#####...', '...#####...', '...#####...'],
    'insomnia': ['...........', '...#####...', '.##.....##.', '#...###...#', '#..#x#x#..#', '#..##x##..#', '#...###...#',
                 '.##.....##.', '...#####...', '...........', '...........'],
    'frail_heart': ['.###...###.', '#####.#####', '####x######', '#####x#####', '####x######', '.####x####.', '..##x####..',
                    '...#x###...', '....###....', '.....#.....', '...........'],
    'clumsy_fingers': ['.#######...', '##.....##..', '#.......#..', '....##.....', '...##......', '..##..x....', '.##....x...',
                       '##.........', '#..........', '...........', '...........'],
}
PACTS = {'leech': '8e1b2b', 'night_hunter': '31336e', 'toad': '4f7f35', 'stoneskin': '6f6e73', 'hare': 'c29a62', 'magpie': '24304a',
         'witchfire': 'd8662a', 'moth': 'b7a3d9', 'wolf': '6a5444', 'briar': '3c6b37', 'raven': '22202c', 'undying': '86a39c'}
HEXES = {'leaden_limbs': '4a4e5a', 'gnawing_hunger': '8a6a2e', 'black_cat': '1b1b22', 'brittle_bones': 'd8d3c0',
         'withering_grip': '4c3a57', 'insomnia': '2a2f5c', 'frail_heart': '9c2e3f', 'clumsy_fingers': '6b5b3e'}


def shade(rgb, f):
    return tuple(max(0, min(255, int(c * f))) for c in rgb[:3]) + (255,)


def sigil(glyph, colour, hex_=False):
    img = Image.new('RGBA', (18, 18), (0, 0, 0, 0))
    base = hexc(colour)
    rim = hexc('6b1a1a') if hex_ else hexc('c9a45c')
    for y in range(18):
        for x in range(18):
            d = math.hypot(x - 8.5, y - 8.5)
            if d <= 8.6:
                if d > 7.4:
                    img.putpixel((x, y), rim if d <= 8.1 else hexc('120a16'))
                else:
                    f = 1.15 - (y / 18) * 0.45
                    img.putpixel((x, y), shade(base, f))
    light = hexc('f2e8d4') if not hex_ else hexc('e8c8c8')
    dark = shade(base, 0.45) if not hex_ else hexc('3a0c10')
    if colour in ('d8d3c0', 'c29a62', 'b7a3d9', '86a39c'):
        light, dark = hexc('2a1630'), shade(base, 1.35)
    for gy, row in enumerate(GLYPHS[glyph]):
        for gx, ch in enumerate(row):
            px, py = 4 + gx, 4 + gy
            if ch == '#':
                img.putpixel((px, py), light)
            elif ch == 'x':
                img.putpixel((px, py), dark)
    if hex_:
        # A crack across the coin.
        for i, (cx, cy) in enumerate(((3, 12), (4, 11), (5, 11), (6, 10))):
            img.putpixel((cx, cy), hexc('120a16'))
    return img


def main():
    (OUT / 'item').mkdir(parents=True, exist_ok=True)
    (OUT / 'mob_effect').mkdir(parents=True, exist_ok=True)
    draw(GRIMOIRE, GRIMOIRE_PAL).save(OUT / 'item/grimoire.png')
    draw(WITCH_BOTTLE, {k: v for k, v in WITCH_BOTTLE_PAL.items() if k != '.'}).save(OUT / 'item/witch_bottle.png')
    draw(POPPET, POPPET_PAL).save(OUT / 'item/poppet.png')
    for pid, colour in PACTS.items():
        sigil(pid, colour).save(OUT / f'mob_effect/pact_{pid}.png')
    for hid, colour in HEXES.items():
        sigil(hid, colour, hex_=True).save(OUT / f'mob_effect/hex_{hid}.png')
    print('textures written:', len(PACTS) + len(HEXES), 'sigils, 3 items')


if __name__ == '__main__':
    main()
