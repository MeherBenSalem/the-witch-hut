#!/usr/bin/env python3
"""Client checks for Witchbound: drives a running QA client and its server (see devrun.sh) and prints PASS/FAIL lines.

  python3 tools/qa/witch_checks.py [--shots DIR]

Needs a 1920x1080 client at GUI scale 3 (the Coven screen's buttons are clicked at fixed places) joined as "Builder",
and a server with RCON. Uses a flat pad at (100, 100, -120) on whatever world is loaded. Every result is read back from
the server (/witchbound info, entity data), not assumed from the click.
"""
from __future__ import annotations

import argparse
import random
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from client import Session  # noqa: E402

PAD = (100, 100, -120)
# The Coven screen is 316x222 GUI pixels, centred; at GUI scale 3 on 1920x1080 its tabs and buttons sit here.
TAB = {'pacts': (613, 351), 'hexes': (841, 351), 'errand': (1069, 351), 'remedies': (1297, 351)}
STRIKE_FIRST = (1308, 470)
ACCEPT = (560, 600)
ABANDON = (1020, 636)
OFFER = (1302, 714)

results = []


def check(name, ok, detail=''):
    results.append(ok)
    print(('PASS ' if ok else 'FAIL ') + name + (f' ({detail})' if detail else ''))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--shots', default=None, help='save screenshots of the screens here')
    args = ap.parse_args()
    shots = Path(args.shots) if args.shots else None
    s = Session(display=':1')
    x0, y0, z0 = PAD

    def shot(name):
        if shots:
            s.xdo('mousemove', '1750', '1000')
            time.sleep(0.6)
            s.shot(shots / f'{name}.png')

    def click(pos):
        s.focus()
        s.xdo('mousemove', str(pos[0]), str(pos[1]))
        time.sleep(0.3)
        s.xdo('click', '1')
        time.sleep(1.2)

    def info():
        return s.cmd('witchbound info Builder')

    def standing():
        m = re.search(r'Coven Standing (-?\d+)', info())
        return int(m.group(1)) if m else None

    def empty_hand():
        slot = int(re.findall(r'(\d+)$', s.cmd('data get entity Builder SelectedItemSlot'))[0])
        s.cmd(f'item replace entity Builder hotbar.{slot} with minecraft:air')
        return slot

    def open_witch():
        s.tp(x0 + 0.5, y0 + 1, z0 + 0.5 - 2.2, facing=(x0 + 0.5, y0 + 2.4, z0 + 2.5))
        time.sleep(1.5)
        s.focus()
        s.xdo('mousemove', '960', '540')
        s.xdo('click', '3')
        time.sleep(1.5)

    # A fresh in-game day each run, so the daily offering cap starts empty.
    day = random.randint(1000, 80000)  # /time set takes an int
    s.cmd('op Builder', 'gamemode creative Builder', 'gamerule doDaylightCycle false', 'gamerule doMobSpawning false',
          f'time set {day * 24000 + 6000}', 'weather clear', 'difficulty normal', 'kill @e[type=minecraft:witch]',
          'witchbound pact Builder clear', 'witchbound hex Builder clear', 'witchbound errand Builder clear',
          'witchbound standing Builder set 0', 'clear Builder', 'kill @e[type=minecraft:item]')
    s.cmd(f'forceload add {x0 - 16} {z0 - 16} {x0 + 16} {z0 + 16}')
    s.tp(x0 + 0.5, y0 + 1, z0 + 0.5)
    time.sleep(4)
    s.cmd(f'fill {x0 - 12} {y0} {z0 - 12} {x0 + 12} {y0} {z0 + 12} minecraft:grass_block',
          f'fill {x0 - 12} {y0 + 1} {z0 - 12} {x0 + 12} {y0 + 8} {z0 + 12} minecraft:air',
          f'kill @e[type=better_witch_huts:coven_witch,x={x0},y={y0},z={z0},distance=..30]')
    s.cmd(f'summon better_witch_huts:coven_witch {x0 + 0.5} {y0 + 1} {z0 + 2.5} {{NoAI:1b,Hut:"bog_hut",Rotation:[180f,0f]}}')
    witches = s.cmd(f'execute if entity @e[type=better_witch_huts:coven_witch,x={x0},y={y0},z={z0},distance=..30]')
    check('summon a resident', 'passed' in witches, witches)
    empty_hand()

    # Pacts
    open_witch()
    shot('menu_pacts')
    click(STRIKE_FIRST)
    after = info()
    check('Strike from the Coven screen', 'Pacts 1/1' in after, after.split('Pacts')[1][:40] if 'Pacts' in after else after)
    effects = s.cmd('data get entity Builder active_effects')
    check('pact shows as a status effect', 'better_witch_huts:pact_' in effects)

    # Hexes (creative players are immune, so switch to survival with resistance)
    s.cmd('gamemode survival Builder', 'effect give Builder minecraft:resistance 600 4 true',
          'effect give Builder minecraft:saturation 600 1 true')
    s.cmd('witchbound hex Builder lay brittle_bones', 'witchbound hex Builder lay black_cat')
    time.sleep(1.5)
    check('two hexes laid', info().count('Hex of') == 2)
    click(TAB['hexes'])
    shot('menu_hexes')

    # Errand: accept, then abandon (-2 standing)
    click(TAB['errand'])
    click(ACCEPT)
    check('accept an errand', 'Errand:' in info())
    shot('menu_errand')
    before = standing()
    click(ABANDON)
    check('abandon the errand', 'Errand:' not in info() and standing() == before - 2, f'{before} -> {standing()}')

    # Offering: 8 spider eyes -> +4 standing
    s.cmd('give Builder minecraft:spider_eye 8')
    time.sleep(0.5)
    click(TAB['remedies'])
    before = standing()
    click(OFFER)
    check('leave an offering', standing() == before + 4, f'{before} -> {standing()}')
    shot('menu_remedies')
    s.key('Escape')
    time.sleep(0.5)

    # Witch Bottle breaks the oldest hex
    slot = empty_hand()
    s.cmd(f'item replace entity Builder hotbar.{slot} with better_witch_huts:witch_bottle 1')
    s.tp(x0 + 0.5, y0 + 1, z0 + 0.5 - 2.2, yaw=180, pitch=-30)
    time.sleep(1)
    s.focus()
    s.xdo('click', '3')
    time.sleep(1.5)
    check('Witch Bottle breaks the oldest hex', info().count('Hex of') == 1 and 'Brittle' not in info())

    # Grimoire
    s.cmd(f'item replace entity Builder hotbar.{slot} with better_witch_huts:grimoire 1')
    time.sleep(0.5)
    s.focus()
    s.xdo('click', '3')
    time.sleep(1.5)
    shot('ledger')
    s.key('Escape')
    time.sleep(0.5)

    def health():
        return float(re.findall(r'([\d.]+)f', s.cmd('data get entity Builder Health'))[0])

    def max_health():
        # Pacts and hexes change max health, so "hurt" means below the current maximum, not below 20.
        return float(re.findall(r'is ([\d.]+)', s.cmd('attribute Builder minecraft:max_health get'))[0])

    s.cmd('witchbound pact Builder clear')
    # Kin peace: a hostile witch hexes or hurts a Stranger (control) but neither targets nor hexes nor hurts a Kin player
    def witch_fight(seconds):
        # Let potions already in the air land before clearing their effects and healing.
        # Killed witches drop their loot (spider eyes, which count as offerings): clear it too.
        s.cmd('kill @e[type=minecraft:witch]')
        time.sleep(4)
        s.cmd('kill @e[type=minecraft:item]')
        # Clear the hexes first and let the effects sync (Frail Heart lowers max health), then heal to full.
        s.cmd('witchbound hex Builder clear', 'effect clear Builder')
        time.sleep(2)
        s.cmd('effect give Builder minecraft:instant_health 1 5 true', 'effect give Builder minecraft:saturation 600 1 true')
        time.sleep(1.5)
        s.cmd(f'summon minecraft:witch {x0 + 0.5} {y0 + 1} {z0 - 6.5}')
        hurt = False
        for _ in range(seconds):
            s.tp(x0 + 0.5, y0 + 1, z0 + 0.5 - 2.2, yaw=180, pitch=0)
            time.sleep(1)
            hurt = hurt or health() < max_health() - 0.01
            if info().count('Hex of') or hurt:
                break
        return info().count('Hex of'), hurt

    s.cmd('witchbound standing Builder set 0')
    hexes, hurt = witch_fight(40)
    check('control: a wild witch hexes or hurts a Stranger', hexes > 0 or hurt, f'hexes {hexes}, hurt {hurt}')
    s.cmd('witchbound standing Builder set 85')
    hexes, hurt = witch_fight(15)
    check('Kin peace: no hex and no harm from a wild witch', hexes == 0 and not hurt, f'hexes {hexes}, hurt {hurt}')
    s.cmd('kill @e[type=minecraft:witch]')

    # Insomnia: control (sleeps), then hexed (woken)
    s.cmd('kill @e[type=minecraft:witch]', 'kill @e[type=minecraft:item]')
    s.cmd('witchbound standing Builder set 0', f'time set {day * 24000 + 18000}', f'setblock {x0 - 9} {y0 + 1} {z0 - 8} minecraft:red_bed[facing=north,part=foot]',
          f'setblock {x0 - 9} {y0 + 1} {z0 - 9} minecraft:red_bed[facing=north,part=head]')

    def try_sleep():
        # Away from the resident: a witch is a monster, and monsters within 8 blocks forbid sleeping.
        s.tp(x0 - 8.5, y0 + 1, z0 - 6.5, facing=(x0 - 8.5, y0 + 1.3, z0 - 8.5))
        time.sleep(1.2)
        s.focus()
        s.xdo('mousemove', '960', '540')
        s.xdo('click', '3')
        time.sleep(0.4)
        return sleeping()

    def sleeping():
        # 1.21.1 saves SleepingX/Y/Z; 26.x saves sleeping_pos.
        return any('has the following' in s.cmd(f'data get entity Builder {key}') for key in ('SleepingX', 'sleeping_pos'))

    slept = try_sleep()
    time.sleep(1)
    s.cmd(f'time set {day * 24000 + 18000}')
    check('control: the player can sleep without the hex', slept)
    s.tp(x0 + 0.5, y0 + 1, z0 + 0.5)
    time.sleep(1)
    s.cmd(f'time set {day * 24000 + 18000}', 'witchbound hex Builder lay insomnia')
    time.sleep(1.2)
    try_sleep()
    time.sleep(1.5)
    still = sleeping()
    check('Insomnia wakes the player', not still)

    s.cmd(f'time set {day * 24000 + 6000}', 'witchbound hex Builder clear', 'witchbound pact Builder clear', 'witchbound standing Builder set 0',
          'gamemode creative Builder')
    print(f'{sum(results)}/{len(results)} checks passed')
    return 0 if all(results) else 1


if __name__ == '__main__':
    sys.exit(main())
