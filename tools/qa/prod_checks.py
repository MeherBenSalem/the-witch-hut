#!/usr/bin/env python3
"""Server-side checks against a production server running a release jar (see prodserver.sh).

  python3 tools/qa/prod_checks.py --port 25576 --log <server.log> --config <config file>

Checks that the mod loaded and wrote its config, every hut is locatable, a generated hut has its named resident, every
loot table rolls, the commands are registered and the log has no data-loading errors.
"""
from __future__ import annotations

import argparse
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from rcon import Rcon  # noqa: E402

HUTS = ['witch_hut', 'bog_hut', 'mangrove_roost', 'hedge_cottage', 'nightshade_circle', 'rime_hut', 'bone_hut']
TABLES = ['chests/' + h for h in HUTS] + ['chests/hut_storage']
results = []


def check(name, ok, detail=''):
    results.append(ok)
    print(('PASS ' if ok else 'FAIL ') + name + (f' ({detail})' if detail else ''))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--port', type=int, default=25576)
    ap.add_argument('--log', required=True)
    ap.add_argument('--config', required=True)
    args = ap.parse_args()
    log = Path(args.log).read_text(errors='replace')
    check('mod loaded', 'Witchbound ready: 7 huts enabled' in log)
    check('config written with comments', Path(args.config).exists() and '//' in Path(args.config).read_text())
    bad = [line for line in log.splitlines() if re.search(r"Registry loading errors|Failed to (load|parse)|Couldn't (load|parse)|Parsing error", line)]
    check('no data loading errors', not bad, bad[0][:160] if bad else '')

    r = Rcon(port=args.port, timeout=300)
    spots = {}
    for hut in HUTS:
        out = r.command(f'locate structure better_witch_huts:{hut}')
        m = re.search(r'\[(-?\d+), ~, (-?\d+)\]', out)
        check(f'locate {hut}', bool(m), out.strip()[:90])
        if m:
            spots[hut] = (int(m.group(1)), int(m.group(2)))
    x, z = spots.get('rime_hut', (0, 0))
    r.command(f'forceload add {x - 32} {z - 32} {x + 32} {z + 32}')
    name = ''
    for _ in range(30):
        time.sleep(2)
        name = r.command(f'execute as @e[type=better_witch_huts:coven_witch,x={x},y=64,z={z},distance=..96] run data get entity @s CustomName')
        if 'Frostwife Isolde' in name:
            break
    check('generated Rime Hut has Frostwife Isolde', 'Frostwife Isolde' in name and '{' not in name.split('data:')[-1], name.strip()[:100])
    for table in TABLES:
        r.command(f'setblock {x} 250 {z} minecraft:air')
        r.command(f'setblock {x} 250 {z} minecraft:chest')
        out = r.command(f'loot insert {x} 250 {z} loot better_witch_huts:{table}')
        check(f'loot {table}', bool(re.search(r'(Dropped|Inserted|Put) \d+', out)) and not re.search(r' 0 item', out), out.strip()[:60])
    out = r.command('witchbound info Nobody')
    check('/witchbound registered', 'Unknown or incomplete command' not in out and 'Unknown command' not in out, out.strip()[:60])
    r.command(f'forceload remove {x - 32} {z - 32} {x + 32} {z + 32}')
    print(f'{sum(results)}/{len(results)} checks passed')
    return 0 if all(results) else 1


if __name__ == '__main__':
    sys.exit(main())
