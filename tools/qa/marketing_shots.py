#!/usr/bin/env python3
"""Captures the listing screenshots from a running QA client (see devrun.sh) on the showcase world.

  python3 tools/qa/marketing_shots.py                 # every hut, two views each
  python3 tools/qa/marketing_shots.py bog_hut rime_hut

Each hut is found with /locate where the world generated it, and is not edited; the camera is a spectator placed
around the resident witch (the hut's middle). Every screenshot is a single unedited frame.
"""
from __future__ import annotations

import math
import re
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from client import Session  # noqa: E402

OUT = Path(__file__).resolve().parents[2] / 'marketing' / 'images' / 'screenshots'
HUTS = ['witch_hut', 'bog_hut', 'mangrove_roost', 'hedge_cottage', 'nightshade_circle', 'rime_hut', 'bone_hut']
# hut: (time of day, [(angle degrees, distance, height)])
VIEWS = {
    'witch_hut': (6000, [(215, 17, 9), (125, 16, 8), (305, 16, 8), (35, 16, 9)]),
    'bog_hut': (11000, [(30, 16, 7), (120, 16, 7), (210, 16, 7), (300, 16, 7)]),
    'mangrove_roost': (6000, [(220, 18, 8), (250, 13, 9), (190, 14, 11), (310, 18, 8)]),
    'hedge_cottage': (6000, [(320, 17, 9), (140, 17, 9), (50, 17, 9), (230, 17, 9)]),
    'nightshade_circle': (6000, [(20, 9, 15), (110, 9, 15), (200, 9, 15), (290, 9, 15)]),
    'rime_hut': (6000, [(45, 15, 7), (135, 15, 7), (225, 15, 7), (315, 15, 7)]),
    'bone_hut': (12000, [(45, 16, 8), (135, 16, 8), (225, 16, 8), (315, 16, 8)]),
}


def main(huts):
    OUT.mkdir(parents=True, exist_ok=True)
    s = Session(display=':1')
    s.cmd('gamemode spectator Builder', 'gamerule doDaylightCycle false', 'weather clear 100000', 'gamerule doMobSpawning false')
    s.hud(False) if hasattr(s, 'hud') else None
    for hut in huts:
        found = s.cmd(f'locate structure better_witch_huts:{hut}')
        m = re.search(r'\[(-?\d+), ~, (-?\d+)\]', found)
        if not m:
            print(hut, 'not found', found)
            continue
        x, z = int(m.group(1)), int(m.group(2))
        s.cmd(f'forceload add {x - 24} {z - 24} {x + 24} {z + 24}')
        s.tp(x, 140, z, pitch=90)
        time.sleep(12)
        pos = s.cmd(f'execute as @e[type=better_witch_huts:coven_witch,limit=1,sort=nearest,x={x},y=80,z={z},distance=..64] run data get entity @s Pos')
        nums = re.findall(r'(-?[\d.]+)d', pos)
        if len(nums) < 3:
            print(hut, 'no resident found near', x, z, pos)
            continue
        wx, wy, wz = (float(v) for v in nums[:3])
        s.cmd(f'data merge entity @e[type=better_witch_huts:coven_witch,limit=1,sort=nearest,x={wx},y={wy},z={wz}] {{NoAI:1b}}')
        clock, views = VIEWS[hut]
        s.cmd(f'time set {clock}')
        for i, (angle, dist, height) in enumerate(views):
            a = math.radians(angle)
            cx, cz = wx + math.cos(a) * dist, wz + math.sin(a) * dist
            s.tp(round(cx, 2), round(wy + height, 2), round(cz, 2), facing=(wx, wy + 1.5, wz))
            time.sleep(14 if i == 0 else 8)
            target = OUT / f'{hut}_{i}.png'
            subprocess.run(['import', '-window', 'root', str(target)], env=s.env, check=False)
            print(hut, i, '->', target.name, f'resident at {wx:.0f} {wy:.0f} {wz:.0f}')
        s.cmd(f'forceload remove {x - 24} {z - 24} {x + 24} {z + 24}')


if __name__ == '__main__':
    main(sys.argv[1:] or HUTS)
