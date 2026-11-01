#!/usr/bin/env python3
"""Builds the seven coven hut templates and their placement metadata.

  python3 tools/structures.py        (needs nbtlib; writes tools/assets/structures/*.nbt and sites.json)

tools/datagen.py copies the templates into every workspace and writes each structure's JSON from the metadata here
(anchor, sample points, site rules), so a template and its placement never drift apart.

Every template's ground layer is G = 0: it replaces the top block of the terrain (y_offset puts it there). Templates
are written in the 1.20.1 format (DataVersion 3465) and upgraded by the game, as in Fisherman's Coast.
"""
from __future__ import annotations

import gzip
import json
import math
import shutil
from pathlib import Path

from nbtlib import File, Int, String, load, Compound, Byte, Float, List

from nbtbuild import AIR, NS, Template, door, fence, lantern, log, slab, stairs, trapdoor, json_str

TOOLS = Path(__file__).resolve().parent
OUT = TOOLS / 'assets' / 'structures'
LEGACY_HUT = TOOLS / 'assets' / 'legacy' / 'witch_hut_1.0.nbt'
SITES: dict[str, dict] = {}


def ring(cx, cz, radius, n=8):
    return [(cx + round(radius * math.cos(a)), cz + round(radius * math.sin(a))) for a in [i * 2 * math.pi / n for i in range(n)]]


def site(t: Template, key, mode, anchor, dry=(), wet=(), around=(), ground=0, **rules):
    """Records where a template samples the land, in template coordinates."""
    def loc(p):
        x, _, z = t.local(p[0], ground, p[1])
        return [x, z]
    g = t.local(0, ground, 0)[1]
    SITES[key] = {'mode': mode, 'anchor': loc(anchor), 'dry': [loc(p) for p in dry], 'wet': [loc(p) for p in wet],
                  'around': [loc(p) for p in around], 'y_offset': -(g + 1), **rules}


# Shared pieces ------------------------------------------------------------------------------------------------------

def pad(t: Template, cx, cz, radius, y, block, wobble=1.5, seed=0):
    """A rounded patch of ground with a ragged edge, so a hut does not sit on a visible square."""
    for x in range(int(cx - radius - 2), int(cx + radius + 3)):
        for z in range(int(cz - radius - 2), int(cz + radius + 3)):
            a = math.atan2(z - cz, x - cx)
            r = radius + wobble * math.sin(3 * a + seed) + 0.7 * math.cos(5 * a + seed * 2)
            if math.hypot(x - cx, z - cz) <= r:
                t.set(x, y, z, block)


def candle(colour='black', n=3, lit=True):
    c = '' if colour is None else colour + '_'
    return f'minecraft:{c}candle[candles={n},lit={str(lit).lower()},waterlogged=false]'


def gable_roof(t: Template, x1, x2, z1, z2, y, material, gable=None, overhang=1):
    """A roof over x1..x2 whose ridge runs along x, one block up per row from z1 and z2 to the middle."""
    half = (z2 - z1) // 2
    for i in range(half + 1):
        yy = y + i
        for x in range(x1 - overhang, x2 + overhang + 1):
            if z1 + i < z2 - i:
                t.set(x, yy, z1 + i, stairs(material, 'south'))
                t.set(x, yy, z2 - i, stairs(material, 'north'))
            elif z1 + i == z2 - i:
                t.set(x, yy, z1 + i, slab(material))
        if gable:
            for x in (x1, x2):
                for z in range(z1 + i + 1, z2 - i):
                    t.set(x, yy, z, gable)
    return y + half


def cone(t: Template, cx, cz, y, radius, material, cap=None):
    """A round, pointed roof of stairs rising one ring per layer."""
    for r in range(radius, 0, -1):
        yy = y + (radius - r)
        for x in range(cx - r - 1, cx + r + 2):
            for z in range(cz - r - 1, cz + r + 2):
                d = math.hypot(x - cx, z - cz)
                if r - 0.5 < d <= r + 0.5:
                    dx, dz = x - cx, z - cz
                    facing = ('west' if dx > 0 else 'east') if abs(dx) >= abs(dz) else ('north' if dz > 0 else 'south')
                    t.set(x, yy, z, stairs(material, facing))
                elif d <= r - 0.5:
                    t.set(x, yy, z, f'minecraft:{material}_planks' if 'stone' not in material and 'brick' not in material else f'minecraft:{material}s')
    t.set(cx, y + radius, cz, cap or f'minecraft:{material}_fence' if 'stone' not in material else cap or 'minecraft:lightning_rod')
    return y + radius


def frame_walls(t: Template, x1, z1, x2, z2, y1, y2, post, infill, every=3):
    """Timber walls: posts at the corners and every few blocks, infill between, a beam on top."""
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if x not in (x1, x2) and z not in (z1, z2):
                continue
            corner = x in (x1, x2) and z in (z1, z2)
            along_x = z in (z1, z2)
            idx = (x - x1) if along_x else (z - z1)
            length = (x2 - x1) if along_x else (z2 - z1)
            is_post = corner or idx % every == 0 or idx == length
            for y in range(y1, y2 + 1):
                if y == y2 and not corner:
                    t.set(x, y, z, log(post, 'x' if along_x else 'z'))
                else:
                    t.set(x, y, z, log(post) if is_post else f'minecraft:{infill}')


def brewing():
    return 'minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=false,has_bottle_2=true]'


def vine(face):
    sides = {'north': 'false', 'south': 'false', 'east': 'false', 'west': 'false'}
    sides[face] = 'true'
    return 'minecraft:vine[' + ','.join(f'{k}={v}' for k, v in sides.items()) + ',up=false]'


def skull_on_post(t: Template, x, y, z, wood, height=2, rotation=0, kind='skeleton_skull'):
    t.column(x, z, y, y + height - 1, fence(wood))
    t.set(x, y + height, z, f'minecraft:{kind}[rotation={rotation}]')


# 1. The Old Hut (the 1.0 hut, repaired) -----------------------------------------------------------------------------

def old_hut():
    """The 1.0 tower hut, kept as it was built but finally lived in: its chest and barrels draw from loot tables
    (1.0 had a fixed handful of iron), the witch spawner is gone, and Granny Hollowell lives by the bed."""
    src = load(LEGACY_HUT)
    pal = src['palette']
    brewing_state = None
    for i, entry in enumerate(pal):
        if str(entry['Name']) == 'minecraft:spawner':
            pal[i] = Compound({'Name': String('minecraft:brewing_stand'),
                               'Properties': Compound({'has_bottle_0': String('true'), 'has_bottle_1': String('false'),
                                                       'has_bottle_2': String('true')})})
            brewing_state = i
    for b in src['blocks']:
        name = str(pal[int(b['state'])]['Name'])
        if int(b['state']) == brewing_state:
            b['nbt'] = Compound({'id': String('minecraft:brewing_stand')})
        elif name == 'minecraft:chest':
            b['nbt'] = Compound({'id': String('minecraft:chest'), 'LootTable': String(f'{NS}:chests/witch_hut')})
        elif name == 'minecraft:barrel':
            b['nbt'] = Compound({'id': String('minecraft:barrel'), 'LootTable': String(f'{NS}:chests/hut_storage')})
    for e in src['entities']:
        if 'TileX' in e['nbt']:
            for axis, v in zip('XYZ', e['blockPos']):
                e['nbt'][f'Tile{axis}'] = Int(int(v))
    granny = Compound({'id': String(f'{NS}:coven_witch'), 'CustomName': String('Granny Hollowell'),
                       'PersistenceRequired': Byte(1), 'CanJoinRaid': Byte(0), 'Hut': String('witch_hut'),
                       'Rotation': List[Float]([Float(90.0), Float(0.0)]), 'Health': Float(26.0)})
    from nbtlib import Double
    src['entities'].append(Compound({'pos': List[Double]([Double(11.5), Double(1.0), Double(12.5)]),
                                     'blockPos': List[Int]([Int(11), Int(1), Int(12)]), 'nbt': granny}))
    OUT.mkdir(parents=True, exist_ok=True)
    with gzip.GzipFile(OUT / 'old_hut.nbt', 'wb', mtime=0) as out:
        File(src).write(out, byteorder='big')
    # Placement: the yard (24x23) is the ground layer.
    t = Template('old_hut')
    t.set(0, 0, 0, AIR)
    t.set(23, 0, 22, AIR)
    site(t, 'witch_hut', 'land', anchor=(12, 12), dry=[(3, 3), (20, 3), (3, 19), (20, 19), (12, 12), (12, 4), (12, 20)],
         around=ring(12, 12, 15), max_slope=3, max_rise=7)
    # Keep the 1.0 template at its id so huts 1.0 left half-generated finish as they started.
    shutil.copyfile(LEGACY_HUT, OUT / 'witch_hut.nbt')
    print('old_hut: 24x14x23 (repaired 1.0 template)')


# 2. Bog Hut -----------------------------------------------------------------------------------------------------------

def bog_hut():
    """A crooked spruce hut on stilts over the swamp, with a sagging roof, a mossy chimney, a toadstool patch on a mud
    island and a jetty."""
    t = Template('bog_hut')
    G = 0
    D = G + 4
    for x in (0, 4, 8):
        for z in (0, 4, 8):
            if (x, z) != (4, 4):
                t.column(x, z, G - 4, D - 1, log('dark_oak_log'))
    t.fill(0, D, 0, 8, D, 8, 'minecraft:spruce_planks')
    for x, z in ((0, 0), (8, 8), (0, 8), (8, 0)):
        t.set(x, D, z, log('dark_oak_log'))
    t.air(-2, D + 1, -2, 10, D + 10, 14)
    frame_walls(t, 1, 1, 7, 7, D + 1, D + 3, 'dark_oak_log', 'spruce_planks', every=3)
    for x, z in ((3, 1), (5, 1), (1, 4), (7, 4)):
        t.set(x, D + 2, z, 'minecraft:brown_stained_glass_pane')
    door(t, 4, D + 1, 7, 'spruce', 'south')
    roof = gable_roof(t, 1, 7, 0, 8, D + 4, 'spruce', gable='minecraft:dark_oak_planks')
    # A crooked crown: the ridge sags at one end.
    t.set(7, roof, 4, 'minecraft:spruce_stairs[facing=west,half=bottom,shape=straight]')
    t.set(8, roof, 4, AIR)
    # Chimney of mossy cobblestone through the roof.
    t.fill(2, D + 1, 2, 2, roof + 2, 2, 'minecraft:mossy_cobblestone')
    t.set(2, roof + 3, 2, 'minecraft:campfire[lit=true,signal_fire=false,facing=north,waterlogged=false]')
    t.set(2, D + 1, 3, 'minecraft:furnace[facing=south,lit=true]')
    # Inside.
    t.set(3, D + 1, 2, 'minecraft:water_cauldron[level=3]')
    t.set(4, D + 1, 2, brewing())
    t.set(5, D + 1, 2, 'minecraft:crafting_table')
    t.set(6, D + 1, 2, 'minecraft:barrel[facing=up,open=false]')
    t.container(6, D + 1, 2, 'minecraft:barrel[facing=up,open=false]', 'chests/hut_storage')
    t.container(6, D + 1, 3, 'minecraft:chest[facing=west,type=single,waterlogged=false]', 'chests/bog_hut')
    t.set(2, D + 1, 6, 'minecraft:green_bed[facing=north,part=foot,occupied=false]')
    t.set(2, D + 1, 5, 'minecraft:green_bed[facing=north,part=head,occupied=false]')
    t.set(6, D + 1, 6, 'minecraft:potted_red_mushroom')
    t.set(6, D + 1, 5, 'minecraft:spruce_trapdoor[facing=west,half=top,open=false,waterlogged=false]')
    t.set(6, D + 2, 5, candle('black', 2))
    t.set(5, D + 2, 2, candle('purple', 1))
    t.set(4, D + 3, 4, 'minecraft:lantern[hanging=true,waterlogged=false]')
    t.set(3, D + 3, 2, 'minecraft:cobweb')
    t.set(6, D + 3, 6, 'minecraft:cobweb')
    t.set(2, D + 3, 4, 'minecraft:hanging_roots[waterlogged=false]')
    t.witch(4, D + 1, 4, 'Old Mother Mirewort', 'bog_hut', yaw=180.0)
    t.cat(3, D + 1, 5)
    # Porch with a rail and a ladder down to the jetty.
    for x in range(0, 9):
        if x not in (3, 4, 5):
            t.set(x, D + 1, 8, fence('spruce'))
    t.set(0, D + 2, 8, 'minecraft:soul_lantern[hanging=false,waterlogged=false]')
    t.set(8, D + 2, 8, skullish())
    t.set(4, D, 9, 'minecraft:spruce_planks')
    t.fill(3, G + 1, 10, 5, G + 1, 14, 'minecraft:spruce_planks')
    t.column(4, 11, G - 3, D, log('dark_oak_log'))
    t.set(4, D + 1, 11, 'minecraft:lantern[hanging=false,waterlogged=false]')
    t.column(4, 10, G + 2, D, 'minecraft:ladder[facing=north,waterlogged=false]')
    t.column(3, 14, G - 3, G, log('dark_oak_log'))
    t.column(5, 14, G - 3, G, log('dark_oak_log'))
    t.set(3, G + 2, 14, fence('spruce'))
    t.set(3, G + 3, 14, 'minecraft:lantern[hanging=false,waterlogged=false]')
    # A mud island with a toadstool garden.
    t.fill(9, G - 2, 1, 12, G, 4, 'minecraft:mud')
    t.fill(10, G, 2, 11, G, 3, 'minecraft:podzol')
    for x, z, m in ((9, 1, 'red_mushroom'), (12, 2, 'brown_mushroom'), (10, 4, 'red_mushroom'), (12, 4, 'brown_mushroom'),
                    (9, 3, 'brown_mushroom')):
        t.set(x, G + 1, z, f'minecraft:{m}')
    t.set(10, G + 1, 2, 'minecraft:red_mushroom_block[down=false,east=true,north=true,south=true,up=true,west=true]')
    t.set(11, G + 1, 3, 'minecraft:mushroom_stem[down=false,east=true,north=true,south=true,up=false,west=true]')
    t.set(11, G + 2, 3, 'minecraft:red_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]')
    for x in range(0, 9, 2):
        t.set(x, D + 3, -1, vine('south'))
        t.set(x + 1, D + 2, 9, vine('north'))
    site(t, 'bog_hut', 'swamp', anchor=(4, 4), dry=[(0, 0), (8, 0), (0, 8), (8, 8), (4, 4), (4, 13), (11, 3)],
         around=ring(5, 6, 12), max_slope=3, max_depth=5, max_rise=5)
    return t


def skullish():
    return 'minecraft:skeleton_skull[rotation=8]'


# 3. Mangrove Roost --------------------------------------------------------------------------------------------------

def mangrove_roost():
    """A two-storey round hut raised on mangrove roots, with a wraparound balcony, a pointed mud-brick and mangrove roof,
    and hanging lanterns."""
    t = Template('mangrove_roost')
    G = 0
    D = G + 3
    cx = cz = 5
    for x in range(cx - 6, cx + 7):
        for z in range(cz - 6, cz + 7):
            d = math.hypot(x - cx, z - cz)
            if d <= 5.5:
                t.set(x, D, z, 'minecraft:mangrove_planks')
            if 4.5 < d <= 5.5 and (x + z) % 3 == 0:
                t.column(x, z, G - 4, D - 1, 'minecraft:mangrove_roots[waterlogged=false]')
    for x, z in ((cx - 3, cz - 3), (cx + 3, cz - 3), (cx - 3, cz + 3), (cx + 3, cz + 3)):
        t.column(x, z, G - 4, D - 1, log('mangrove_log'))
    t.column(cx, cz, G - 4, D - 1, 'minecraft:muddy_mangrove_roots[axis=y]')
    t.air(cx - 7, D + 1, cz - 7, cx + 7, D + 14, cz + 7)
    # Round walls, two storeys.
    for y in range(D + 1, D + 8):
        for x in range(cx - 4, cx + 5):
            for z in range(cz - 4, cz + 5):
                d = math.hypot(x - cx, z - cz)
                if 2.9 < d <= 3.7:
                    block = 'minecraft:mud_bricks' if y in (D + 1, D + 4) else 'minecraft:mangrove_planks'
                    if (x == cx or z == cz) and y not in (D + 1, D + 4, D + 7):
                        block = log('mangrove_log')
                    t.set(x, y, z, block)
    for x, z in ((cx + 3, cz), (cx - 3, cz), (cx, cz - 3)):
        t.set(x, D + 2, z, 'minecraft:light_gray_stained_glass_pane')
        t.set(x, D + 6, z, 'minecraft:light_gray_stained_glass_pane')
    door(t, cx, D + 1, cz + 3, 'mangrove', 'south')
    t.fill(cx - 2, D + 4, cz - 2, cx + 2, D + 4, cz + 2, 'minecraft:mangrove_planks')
    t.set(cx + 2, D + 4, cz - 1, AIR)
    t.column(cx + 2, cz - 1, D + 1, D + 4, 'minecraft:ladder[facing=west,waterlogged=false]')
    # Balcony rail.
    for x in range(cx - 6, cx + 7):
        for z in range(cz - 6, cz + 7):
            d = math.hypot(x - cx, z - cz)
            if 4.9 < d <= 5.5 and not (x == cx and z > cz):
                t.set(x, D + 1, z, fence('mangrove'))
    cone(t, cx, cz, D + 8, 4, 'mangrove', cap='minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]')
    # Downstairs: the work room.
    t.set(cx - 2, D + 1, cz, 'minecraft:water_cauldron[level=3]')
    t.set(cx - 2, D + 1, cz - 1, brewing())
    t.set(cx - 1, D + 1, cz - 2, 'minecraft:smoker[facing=south,lit=true]')
    t.container(cx + 1, D + 1, cz - 2, 'minecraft:barrel[facing=up,open=false]', 'chests/hut_storage')
    t.set(cx + 2, D + 1, cz + 1, 'minecraft:potted_mangrove_propagule')
    t.set(cx - 2, D + 1, cz + 1, candle('green', 3))
    t.set(cx, D + 3, cz, 'minecraft:lantern[hanging=true,waterlogged=false]')
    t.witch(cx, D + 1, cz, 'Auntie Silt', 'mangrove_roost', yaw=0.0)
    # Upstairs: the bedroom and her chest.
    t.set(cx - 2, D + 5, cz, 'minecraft:lime_bed[facing=east,part=head,occupied=false]')
    t.set(cx - 1, D + 5, cz, 'minecraft:lime_bed[facing=east,part=foot,occupied=false]')
    t.container(cx, D + 5, cz - 2, 'minecraft:chest[facing=south,type=single,waterlogged=false]', 'chests/mangrove_roost')
    t.set(cx + 1, D + 5, cz + 2, 'minecraft:flower_pot')
    t.set(cx - 1, D + 5, cz + 2, candle('lime', 2))
    t.set(cx + 1, D + 5, cz - 2, 'minecraft:decorated_pot[facing=south,cracked=false,waterlogged=false]')
    t.set(cx, D + 7, cz, 'minecraft:soul_lantern[hanging=true,waterlogged=false]')
    t.cat(cx + 1, D + 5, cz + 1, 'black')
    # Lanterns hanging off the balcony and moss on the roots.
    for x, z in ((cx - 5, cz), (cx + 5, cz), (cx, cz - 5)):
        t.set(x, D - 1, z, 'minecraft:chain[axis=y,waterlogged=false]')
        t.set(x, D - 2, z, 'minecraft:lantern[hanging=true,waterlogged=false]')
    t.column(cx, cz + 5, G - 4, D - 1, log('mangrove_log'))
    t.column(cx, cz + 6, G + 1, D, 'minecraft:ladder[facing=south,waterlogged=false]')
    t.set(cx, D, cz + 5, 'minecraft:mangrove_planks')
    site(t, 'mangrove_roost', 'swamp', anchor=(cx, cz), dry=[(cx - 5, cz), (cx + 5, cz), (cx, cz - 5), (cx, cz + 5), (cx, cz)],
         around=ring(cx, cz, 11), max_slope=3, max_depth=6, max_rise=7)
    return t


# 4. Hedge Cottage ---------------------------------------------------------------------------------------------------

def hedge_cottage():
    """A hedge witch's cottage: cobble footing, timber and white walls under a steep thatch of hay, a fenced herb garden
    with beehives, and a bench by the door."""
    t = Template('hedge_cottage')
    G = 0
    pad(t, 7, 6, 9.0, G, 'minecraft:grass_block[snowy=false]', seed=2)
    t.air(-1, G + 1, -1, 15, G + 12, 13)
    t.fill(0, G, 0, 8, G, 6, 'minecraft:cobblestone')
    t.fill(1, G, 1, 7, G, 5, 'minecraft:spruce_planks')
    frame_walls(t, 0, 0, 8, 6, G + 1, G + 3, 'oak_log', 'white_terracotta', every=4)
    for x in (2, 6):
        t.set(x, G + 2, 0, 'minecraft:glass_pane')
        t.set(x, G + 2, 6, 'minecraft:glass_pane')
    t.set(0, G + 2, 3, 'minecraft:glass_pane')
    door(t, 4, G + 1, 6, 'oak', 'south')
    roof = gable_roof(t, 0, 8, -1, 7, G + 4, 'spruce', gable='minecraft:oak_planks')
    for x in range(-1, 10):
        t.set(x, roof, 3, 'minecraft:hay_block[axis=x]')
    t.fill(7, G + 1, 1, 7, roof + 1, 1, 'minecraft:bricks')
    t.set(7, roof + 2, 1, 'minecraft:campfire[lit=true,signal_fire=false,facing=north,waterlogged=false]')
    # Inside: herbs drying, a cauldron, a bed, a lectern-like desk.
    t.set(6, G + 1, 1, 'minecraft:water_cauldron[level=2]')
    t.set(5, G + 1, 1, brewing())
    t.set(1, G + 1, 1, 'minecraft:red_bed[facing=east,part=head,occupied=false]')
    t.set(2, G + 1, 1, 'minecraft:red_bed[facing=east,part=foot,occupied=false]')
    t.container(1, G + 1, 5, 'minecraft:chest[facing=east,type=single,waterlogged=false]', 'chests/hedge_cottage')
    t.container(1, G + 1, 4, 'minecraft:barrel[facing=east,open=false]', 'chests/hut_storage')
    t.set(7, G + 1, 5, 'minecraft:composter[level=4]')
    t.set(7, G + 1, 4, 'minecraft:oak_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]')
    t.set(3, G + 1, 1, 'minecraft:bookshelf')
    t.set(3, G + 2, 1, 'minecraft:potted_allium')
    t.set(3, G + 1, 5, 'minecraft:potted_cornflower')
    t.set(6, G + 1, 5, 'minecraft:potted_lily_of_the_valley')
    for x in (2, 4, 6):
        t.set(x, G + 3, 3, 'minecraft:spore_blossom' if x == 4 else 'minecraft:hanging_roots[waterlogged=false]')
    t.set(4, G + 3, 1, 'minecraft:lantern[hanging=true,waterlogged=false]')
    t.set(5, G + 1, 5, candle('white', 3))
    t.witch(4, G + 1, 3, 'Hedgewife Tansy', 'hedge_cottage', yaw=0.0)
    # Bench and lantern by the door.
    t.set(2, G + 1, 7, 'minecraft:oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
    t.set(1, G + 1, 7, 'minecraft:oak_trapdoor[facing=west,half=bottom,open=true,waterlogged=false]')
    t.set(6, G + 1, 7, fence('oak'))
    t.set(6, G + 2, 7, 'minecraft:lantern[hanging=false,waterlogged=false]')
    # The herb garden: a hedge of leaves and fences around rows of flowers and berries, two beehives.
    for x in range(9, 16):
        for z in range(-1, 14):
            if x in (9, 15) or z in (-1, 13):
                t.set(x, G + 1, z, 'minecraft:oak_leaves[distance=1,persistent=true,waterlogged=false]')
    t.set(9, G + 1, 6, 'minecraft:oak_fence_gate[facing=east,in_wall=false,open=false,powered=false]')
    t.set(9, G + 1, 5, 'minecraft:oak_leaves[distance=1,persistent=true,waterlogged=false]')
    herbs = ['allium', 'cornflower', 'oxeye_daisy', 'lily_of_the_valley', 'azure_bluet', 'poppy', 'blue_orchid']
    for i, z in enumerate(range(0, 13, 2)):
        t.fill(10, G, z, 14, G, z, 'minecraft:farmland[moisture=7]')
        for x in range(10, 15):
            if z in (4, 8):
                t.set(x, G + 1, z, 'minecraft:sweet_berry_bush[age=2]')
            else:
                t.set(x, G + 1, z, f'minecraft:{herbs[(i + x) % len(herbs)]}')
    t.set(12, G, 7, 'minecraft:water')
    t.set(14, G + 1, 1, 'minecraft:beehive[facing=west,honey_level=3]')
    t.set(14, G + 1, 11, 'minecraft:beehive[facing=west,honey_level=5]')
    t.set(10, G + 1, 11, 'minecraft:rose_bush[half=lower]')
    t.set(10, G + 2, 11, 'minecraft:rose_bush[half=upper]')
    site(t, 'hedge_cottage', 'land', anchor=(7, 6), dry=[(0, 0), (8, 0), (0, 6), (8, 6), (15, -1), (15, 13), (9, 13), (4, 3)],
         around=ring(7, 6, 12), max_slope=2, max_rise=6)
    return t


# 5. Nightshade Circle -----------------------------------------------------------------------------------------------

def nightshade_circle():
    """A ring of standing stones around a great cauldron on a blackstone dais, soul fires and candles, and Mother
    Nightshade's crooked dark oak hut at its edge. Where the Coven meets."""
    t = Template('nightshade_circle')
    G = 0
    cx = cz = 8
    t.air(cx - 11, G + 1, cz - 11, cx + 11, G + 10, cz + 11)
    for x in range(cx - 10, cx + 11):
        for z in range(cz - 10, cz + 11):
            d = math.hypot(x - cx, z - cz)
            if d <= 9.5:
                t.set(x, G, z, 'minecraft:podzol[snowy=false]' if (x * 7 + z * 3) % 5 else 'minecraft:coarse_dirt')
            if d <= 3.5:
                t.set(x, G, z, 'minecraft:polished_blackstone')
            if d <= 2.5:
                t.set(x, G + 1, z, 'minecraft:polished_blackstone_slab[type=bottom,waterlogged=false]')
    # The standing stones.
    for i, (x, z) in enumerate(ring(cx, cz, 8, 8)):
        h = 3 + (i % 3)
        t.column(x, z, G + 1, G + h, 'minecraft:mossy_stone_bricks' if i % 2 else 'minecraft:cobbled_deepslate')
        t.set(x, G + h + 1, z, 'minecraft:mossy_stone_brick_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]'
              if i % 2 else candle('purple', 4))
    # Lintels joining two pairs of stones, like a trilithon.
    t.fill(cx - 1, G + 5, cz - 8, cx + 1, G + 5, cz - 8, 'minecraft:deepslate_bricks')
    t.column(cx - 1, cz - 8, G + 1, G + 4, 'minecraft:deepslate_brick_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]')
    t.column(cx + 1, cz - 8, G + 1, G + 4, 'minecraft:deepslate_brick_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]')
    t.set(cx, G + 1, cz - 8, AIR)
    # The great cauldron and its fires.
    t.set(cx, G + 2, cz, 'minecraft:water_cauldron[level=3]')
    t.set(cx, G + 1, cz, 'minecraft:soul_campfire[lit=true,signal_fire=false,facing=north,waterlogged=false]')
    for x, z in ((cx - 2, cz - 2), (cx + 2, cz - 2), (cx - 2, cz + 2), (cx + 2, cz + 2)):
        t.set(x, G + 2, z, candle('black', 4))
    for x, z in ((cx - 3, cz), (cx + 3, cz), (cx, cz - 3)):
        t.set(x, G + 1, z, 'minecraft:soul_campfire[lit=true,signal_fire=false,facing=north,waterlogged=false]')
    for x, z in ((cx - 5, cz - 5), (cx + 5, cz + 4), (cx - 4, cz + 6), (cx + 6, cz - 3)):
        t.set(x, G + 1, z, 'minecraft:red_mushroom' if (x + z) % 2 else 'minecraft:brown_mushroom')
    t.set(cx + 4, G + 1, cz - 6, 'minecraft:skeleton_skull[rotation=6]')
    t.set(cx - 6, G + 1, cz + 2, 'minecraft:wither_rose')
    t.set(cx + 6, G + 1, cz + 2, 'minecraft:wither_rose')
    # Mother Nightshade's hut on the circle's south edge.
    hx, hz = cx - 3, cz + 10
    t.fill(hx, G, hz, hx + 6, G, hz + 5, 'minecraft:dark_oak_planks')
    frame_walls(t, hx, hz, hx + 6, hz + 5, G + 1, G + 3, 'dark_oak_log', 'stripped_dark_oak_wood', every=3)
    t.set(hx + 6, G + 2, hz + 2, 'minecraft:purple_stained_glass_pane')
    t.set(hx, G + 2, hz + 2, 'minecraft:purple_stained_glass_pane')
    door(t, hx + 3, G + 1, hz, 'dark_oak', 'north')
    roof = gable_roof(t, hx, hx + 6, hz - 1, hz + 6, G + 4, 'dark_oak', gable='minecraft:dark_oak_planks')
    t.set(hx + 1, G + 1, hz + 4, 'minecraft:purple_bed[facing=east,part=foot,occupied=false]')
    t.set(hx + 2, G + 1, hz + 4, 'minecraft:purple_bed[facing=east,part=head,occupied=false]')
    t.container(hx + 5, G + 1, hz + 4, 'minecraft:chest[facing=west,type=single,waterlogged=false]', 'chests/nightshade_circle')
    t.container(hx + 5, G + 1, hz + 3, 'minecraft:barrel[facing=west,open=false]', 'chests/hut_storage')
    t.set(hx + 5, G + 1, hz + 1, brewing())
    t.set(hx + 1, G + 1, hz + 1, 'minecraft:chiseled_bookshelf[facing=east,slot_0_occupied=true,slot_1_occupied=false,slot_2_occupied=true,slot_3_occupied=true,slot_4_occupied=false,slot_5_occupied=true]')
    t.set(hx + 1, G + 2, hz + 1, candle('purple', 2))
    t.set(hx + 3, G + 3, hz + 3, 'minecraft:soul_lantern[hanging=true,waterlogged=false]')
    t.set(hx + 4, G + 3, hz + 4, 'minecraft:cobweb')
    t.witch(cx, G + 2, cz + 2, 'Mother Nightshade', 'nightshade_circle', yaw=180.0)
    t.cat(hx + 3, G + 1, hz + 3)
    site(t, 'nightshade_circle', 'land', anchor=(cx, cz + 3),
         dry=[(cx - 8, cz), (cx + 8, cz), (cx, cz - 8), (cx, cz), (hx, hz + 5), (hx + 6, hz + 5)],
         around=ring(cx, cz + 3, 15), max_slope=3, max_rise=7)
    return t


# 6. Rime Hut --------------------------------------------------------------------------------------------------------

def rime_hut():
    """A spruce log cabin half-buried in snow with a steep snow-laden roof, blue ice windows, a powder-snow cauldron
    and a frozen altar."""
    t = Template('rime_hut')
    G = 0
    pad(t, 5, 4, 7.5, G, 'minecraft:snow_block', seed=1)
    t.air(-2, G + 1, -2, 12, G + 12, 11)
    t.fill(0, G, 0, 8, G, 6, 'minecraft:spruce_planks')
    for y in range(G + 1, G + 4):
        for x in range(0, 9):
            for z in range(0, 7):
                if x in (0, 8) or z in (0, 6):
                    t.set(x, y, z, log('spruce_log', 'x' if z in (0, 6) else 'z') if x not in (0, 8) or z not in (0, 6) else log('spruce_log'))
    for x in (2, 6):
        t.set(x, G + 2, 0, 'minecraft:blue_ice')
        t.set(x, G + 2, 6, 'minecraft:light_blue_stained_glass')
    door(t, 4, G + 1, 6, 'spruce', 'south')
    roof = gable_roof(t, 0, 8, -1, 7, G + 4, 'spruce', gable='minecraft:spruce_planks')
    for x in range(-1, 10):
        for z in range(-1, 8):
            top = None
            for y in range(roof + 1, G + 3, -1):
                if t.has(x, y, z) and t.get(x, y, z) != AIR:
                    top = y
                    break
            if top is not None and t.get(x, top + 1, z) in (None, AIR) and 'stairs' not in t.get(x, top, z):
                t.set(x, top + 1, z, 'minecraft:snow[layers=1]')
    t.fill(1, G + 1, 1, 1, roof + 1, 1, 'minecraft:stone_bricks')
    t.set(1, roof + 2, 1, 'minecraft:campfire[lit=true,signal_fire=false,facing=north,waterlogged=false]')
    # Inside: warm and cold sides.
    t.set(2, G + 1, 1, 'minecraft:smoker[facing=south,lit=true]')
    t.set(6, G + 1, 1, 'minecraft:powder_snow_cauldron[level=3]')
    t.set(7, G + 1, 1, brewing())
    t.set(7, G + 1, 3, 'minecraft:blue_ice')
    t.set(7, G + 2, 3, 'minecraft:amethyst_cluster[facing=up,waterlogged=false]')
    t.set(1, G + 1, 4, 'minecraft:white_bed[facing=north,part=head,occupied=false]')
    t.set(1, G + 1, 5, 'minecraft:white_bed[facing=north,part=foot,occupied=false]')
    t.container(7, G + 1, 5, 'minecraft:chest[facing=west,type=single,waterlogged=false]', 'chests/rime_hut')
    t.container(7, G + 1, 4, 'minecraft:barrel[facing=west,open=false]', 'chests/hut_storage')
    t.set(4, G + 1, 1, 'minecraft:white_carpet')
    t.set(3, G + 1, 3, 'minecraft:light_blue_carpet')
    t.set(4, G + 1, 3, 'minecraft:light_blue_carpet')
    t.set(5, G + 1, 3, 'minecraft:light_blue_carpet')
    t.set(4, G + 3, 3, 'minecraft:soul_lantern[hanging=true,waterlogged=false]')
    t.set(2, G + 2, 1, candle('light_blue', 2))
    t.witch(4, G + 1, 4, 'Frostwife Isolde', 'rime_hut', yaw=180.0)
    # Outside: a frozen altar of packed ice, a woodpile and berry bushes.
    t.fill(10, G + 1, 1, 11, G + 1, 2, 'minecraft:packed_ice')
    t.set(10, G + 2, 1, 'minecraft:blue_ice')
    t.set(10, G + 3, 1, candle('white', 4))
    t.set(11, G + 2, 2, 'minecraft:skeleton_skull[rotation=12]')
    for z in range(1, 5):
        t.set(-1, G + 1, z, log('spruce_log', 'z'))
    t.set(-1, G + 2, 2, log('spruce_log', 'z'))
    t.set(10, G + 1, 7, 'minecraft:sweet_berry_bush[age=3]')
    t.set(11, G + 1, 8, 'minecraft:sweet_berry_bush[age=2]')
    t.set(3, G + 1, 7, fence('spruce'))
    t.set(3, G + 2, 7, 'minecraft:lantern[hanging=false,waterlogged=false]')
    site(t, 'rime_hut', 'land', anchor=(4, 3), dry=[(0, 0), (8, 0), (0, 6), (8, 6), (11, 1), (11, 8), (4, 3)],
         around=ring(5, 4, 11), max_slope=2, max_rise=6)
    return t


# 7. Bone Hut --------------------------------------------------------------------------------------------------------

def bone_hut():
    """A crone's hut of terracotta and bone in the badlands: a ribcage arch for a door, skulls on poles, a lava cauldron
    and a cactus garden."""
    t = Template('bone_hut')
    G = 0
    t.air(-3, G + 1, -3, 13, G + 11, 13)
    pad(t, 4, 4, 8.0, G, 'minecraft:red_sand', seed=3)
    t.fill(0, G, 0, 8, G, 8, 'minecraft:smooth_red_sandstone')
    for y in range(G + 1, G + 5):
        for x in range(0, 9):
            for z in range(0, 9):
                d = max(abs(x - 4), abs(z - 4))
                corner = abs(x - 4) == 4 and abs(z - 4) == 4
                if d == 4 and not corner:
                    block = 'minecraft:terracotta' if y % 2 else 'minecraft:orange_terracotta'
                    if (x - 4) % 2 == 0 and (z - 4) % 2 == 0:
                        block = 'minecraft:bone_block[axis=y]'
                    t.set(x, y, z, block)
                elif corner:
                    t.set(x, y, z, 'minecraft:bone_block[axis=y]')
    for x, z in ((2, 0), (6, 0), (0, 4), (8, 4)):
        t.set(x, G + 2, z, 'minecraft:orange_stained_glass_pane')
    # A ribcage arch around the door.
    door(t, 4, G + 1, 8, 'acacia', 'south')
    for y in range(G + 1, G + 5):
        t.set(3, y, 9, 'minecraft:bone_block[axis=y]')
        t.set(5, y, 9, 'minecraft:bone_block[axis=y]')
    t.set(4, G + 5, 9, 'minecraft:bone_block[axis=x]')
    t.set(3, G + 5, 9, 'minecraft:bone_block[axis=x]')
    t.set(5, G + 5, 9, 'minecraft:bone_block[axis=x]')
    t.set(4, G + 6, 9, 'minecraft:skeleton_skull[rotation=0]')
    # A flat roof of terracotta slabs with a bone crown.
    t.fill(-1, G + 5, -1, 9, G + 5, 9, 'minecraft:red_sandstone_slab[type=bottom,waterlogged=false]')
    t.fill(0, G + 5, 0, 8, G + 5, 8, 'minecraft:terracotta')
    for x, z in ((0, 0), (8, 0), (0, 8), (8, 8)):
        t.set(x, G + 6, z, 'minecraft:bone_block[axis=y]')
        t.set(x, G + 7, z, 'minecraft:skeleton_skull[rotation=%d]' % (2 if x == 8 else 14))
    t.set(4, G + 6, 4, 'minecraft:campfire[lit=true,signal_fire=false,facing=north,waterlogged=false]')
    t.set(4, G + 5, 4, 'minecraft:terracotta')
    # Inside.
    t.set(2, G + 1, 1, 'minecraft:lava_cauldron')
    t.set(3, G + 1, 1, brewing())
    t.set(5, G + 1, 1, 'minecraft:blast_furnace[facing=south,lit=false]')
    t.set(6, G + 1, 1, 'minecraft:bone_block[axis=y]')
    t.set(6, G + 2, 1, 'minecraft:wither_skeleton_skull[rotation=8]')
    t.set(1, G + 1, 6, 'minecraft:orange_bed[facing=north,part=head,occupied=false]')
    t.set(1, G + 1, 7, 'minecraft:orange_bed[facing=north,part=foot,occupied=false]')
    t.container(7, G + 1, 6, 'minecraft:chest[facing=west,type=single,waterlogged=false]', 'chests/bone_hut')
    t.container(7, G + 1, 5, 'minecraft:barrel[facing=west,open=false]', 'chests/hut_storage')
    t.set(7, G + 1, 7, 'minecraft:decorated_pot[facing=west,cracked=false,waterlogged=false]')
    t.set(1, G + 1, 2, 'minecraft:potted_dead_bush')
    t.set(4, G + 4, 4, 'minecraft:lantern[hanging=true,waterlogged=false]')
    t.set(2, G + 1, 3, candle('orange', 3))
    t.set(4, G + 4, 2, 'minecraft:cobweb')
    t.witch(4, G + 1, 4, 'Crone Ossia', 'bone_hut', yaw=180.0)
    # Skulls on poles around the hut, a cactus garden and dead bushes.
    for x, z, r in ((-2, -2, 6), (10, -2, 10), (-2, 10, 2), (10, 10, 14), (4, 12, 0)):
        skull_on_post(t, x, G + 1, z, 'acacia', 2, r)
    for x, z in ((10, 3), (11, 5), (10, 7)):
        t.column(x, z, G + 1, G + 2, 'minecraft:cactus[age=0]')
    for x, z in ((-1, 4), (11, 1), (1, 11), (8, 11)):
        t.set(x, G + 1, z, 'minecraft:dead_bush')
    site(t, 'bone_hut', 'land', anchor=(4, 4), dry=[(0, 0), (8, 0), (0, 8), (8, 8), (4, 4), (-2, -2), (10, 10)],
         around=ring(4, 5, 12), max_slope=2, max_rise=7)
    return t


BUILDERS = [bog_hut, mangrove_roost, hedge_cottage, nightshade_circle, rime_hut, bone_hut]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    old_hut()
    for build in BUILDERS:
        t = build()
        size = t.save(OUT / f'{t.name}.nbt')
        print(f'{t.name}: {size[0]}x{size[1]}x{size[2]}, {len(t.blocks)} blocks, {len(t.entities)} entities')
    (OUT / 'sites.json').write_text(json.dumps(SITES, indent=2) + '\n')


if __name__ == '__main__':
    main()
