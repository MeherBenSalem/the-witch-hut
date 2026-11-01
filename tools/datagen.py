#!/usr/bin/env python3
"""Writes Witchbound's assets and data into every workspace. Edit the tables here, not the generated JSON.

  python3 tools/gen_textures.py && python3 tools/structures.py && python3 tools/datagen.py

26.1.2 and 26.2 share one data format. 26.3 renamed loot "functions" to "modifier" (with "type" instead of "function")
and recipe_unlocked's "recipe" to "recipes" (as handled in Fisherman's Coast). 1.21.1 writes recipe ingredients as
{"item": id} objects, takes advancement backgrounds as texture paths, has no item model definitions (assets/<ns>/items)
and reads an entity's CustomName as JSON text, so its templates get JSON names.
"""
from __future__ import annotations

import json
import shutil
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
TOOLS = Path(__file__).resolve().parent
NS = 'better_witch_huts'
VERSIONS = ['1.21.1', '26.1.2', '26.2', '26.3']
PACK = {
    '1.21.1': {'pack_format': 34},
    '26.1.2': {'min_format': 84, 'max_format': 101},
    '26.2': {'min_format': 88, 'max_format': 121},
    '26.3': {'min_format': 88, 'max_format': 121},
}

# Huts ---------------------------------------------------------------------------------------------------------------
# id: (name, resident, biomes, spacing, separation, salt, icon)
STRUCTURES = {
    'witch_hut': ('The Old Hut', 'Granny Hollowell',
                  ['minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:forest', 'minecraft:birch_forest',
                   'minecraft:old_growth_birch_forest'], 30, 10, 198386209, 'carved_pumpkin'),
    'bog_hut': ('Bog Hut', 'Old Mother Mirewort', ['minecraft:swamp'], 16, 6, 198386210, 'lily_pad'),
    'mangrove_roost': ('Mangrove Roost', 'Auntie Silt', ['minecraft:mangrove_swamp'], 14, 5, 198386211, 'mangrove_propagule'),
    'hedge_cottage': ('Hedge Cottage', 'Hedgewife Tansy',
                      ['minecraft:meadow', 'minecraft:flower_forest', 'minecraft:cherry_grove'], 26, 9, 198386212, 'allium'),
    'nightshade_circle': ('Nightshade Circle', 'Mother Nightshade',
                          ['minecraft:dark_forest', {'id': 'minecraft:pale_garden', 'required': False}], 28, 10, 198386213,
                          'soul_campfire'),
    'rime_hut': ('Rime Hut', 'Frostwife Isolde', ['minecraft:snowy_taiga', 'minecraft:snowy_plains', 'minecraft:grove'],
                 26, 9, 198386214, 'blue_ice'),
    'bone_hut': ('Bone Hut', 'Crone Ossia',
                 ['minecraft:badlands', 'minecraft:eroded_badlands', 'minecraft:wooded_badlands', 'minecraft:desert'],
                 26, 9, 198386215, 'skeleton_skull'),
}
# The 1.0 hut keeps its structure id; new huts are placed from the repaired template, and the 1.0 template stays at its
# old id so huts 1.0 left half-generated finish the way they started.
TEMPLATES = {'witch_hut': 'old_hut'}
VILLAGE_CLEARANCE = 4


def e(item, weight, count=None, extra=None):
    return {'item': item, 'weight': weight, 'count': count, 'extra': extra}


INGREDIENTS = [e('spider_eye', 6, (1, 3)), e('fermented_spider_eye', 3, (1, 2)), e('sugar', 6, (2, 6)),
               e('glowstone_dust', 5, (2, 6)), e('redstone', 6, (2, 8)), e('glass_bottle', 6, (2, 5)),
               e('nether_wart', 4, (2, 5)), e('gunpowder', 3, (1, 4)), e('phantom_membrane', 2, (1, 2)),
               e('amethyst_shard', 3, (1, 4)), e('red_mushroom', 4, (1, 3)), e('brown_mushroom', 4, (1, 3)),
               e('bone', 4, (2, 5)), e('string', 4, (2, 5)), e('candle', 3, (1, 3))]
TREASURE = [e('emerald', 9, (2, 6)), e(f'{NS}:witch_bottle', 5), e(f'{NS}:poppet', 1), e(f'{NS}:grimoire', 3),
            e('potion', 4, None, 'healing'), e('potion', 3, None, 'night_vision'), e('potion', 3, None, 'swiftness'),
            e('potion', 2, None, 'fire_resistance'), e('splash_potion', 2, None, 'poison'),
            e('book', 3, None, ('levels', 10, 25)), e('experience_bottle', 3, (2, 5)), e('name_tag', 1),
            e('golden_apple', 1), e('rabbit_foot', 2)]
EXTRAS = {
    'witch_hut': [e('pumpkin_pie', 4, (1, 3)), e('carved_pumpkin', 3), e('pumpkin_seeds', 3, (2, 6)), e('cake', 1)],
    'bog_hut': [e('slime_ball', 6, (1, 4)), e('lily_pad', 4, (1, 3)), e('mud', 3, (2, 4)), e('potion', 3, None, 'water_breathing')],
    'mangrove_roost': [e('mangrove_propagule', 4, (1, 3)), e('slime_ball', 5, (1, 3)), e('ink_sac', 4, (1, 3)),
                       e('potion', 3, None, 'water_breathing')],
    'hedge_cottage': [e('honey_bottle', 4, (1, 2)), e('honeycomb', 3, (1, 3)), e('allium', 3, (1, 3)), e('sweet_berries', 4, (2, 6)),
                      e('bread', 4, (1, 3)), e('potion', 3, None, 'regeneration')],
    'nightshade_circle': [e('ender_pearl', 3, (1, 2)), e('soul_sand', 3, (1, 3)), e('black_candle', 4, (1, 3)),
                          e('crying_obsidian', 2, (1, 2)), e('potion', 3, None, 'invisibility'), e('splash_potion', 2, None, 'weakness'),
                          e('wither_rose', 1)],
    'rime_hut': [e('snowball', 5, (2, 8)), e('packed_ice', 4, (1, 3)), e('sweet_berries', 4, (2, 6)), e('leather', 3, (1, 3)),
                 e('blue_ice', 1), e('potion', 3, None, 'slow_falling')],
    'bone_hut': [e('bone_meal', 5, (3, 8)), e('gold_nugget', 5, (3, 9)), e('blaze_powder', 2, (1, 2)), e('magma_cream', 2, (1, 2)),
                 e('potion', 3, None, 'fire_resistance'), e('skeleton_skull', 1)],
}
LOOT = {f'chests/{k}': [((3, 5), INGREDIENTS + v), ((2, 3), TREASURE)] for k, v in EXTRAS.items()}
LOOT['chests/hut_storage'] = [((3, 6), INGREDIENTS + [e('bread', 4, (1, 3)), e('baked_potato', 3, (1, 4)), e('coal', 4, (2, 6)),
                                                     e('pumpkin_pie', 2, (1, 2)), e('book', 2), e('paper', 3, (2, 5))]),
                              ((0, 1), [e('emerald', 5, (1, 3)), e(f'{NS}:witch_bottle', 2), e('potion', 2, None, 'healing')])]

RECIPES = {
    # id: (pattern or None, key or ingredient list, result, count, unlocked by)
    'grimoire': (None, ['minecraft:book', 'minecraft:spider_eye', 'minecraft:feather', 'minecraft:ink_sac'], f'{NS}:grimoire', 1,
                 'minecraft:spider_eye'),
    'witch_bottle': (None, ['minecraft:glass_bottle', 'minecraft:iron_nugget', 'minecraft:iron_nugget', 'minecraft:sweet_berries',
                            'minecraft:redstone'], f'{NS}:witch_bottle', 1, 'minecraft:glass_bottle'),
    'poppet': (['SWS', 'WAW', 'W W'], {'S': 'minecraft:string', 'W': 'minecraft:wheat', 'A': 'minecraft:amethyst_shard'},
               f'{NS}:poppet', 1, 'minecraft:amethyst_shard'),
}
ITEMS = ['grimoire', 'witch_bottle', 'poppet']

PACTS = {
    # id: (name, gift, price, lore)
    'leech': ('Pact of the Leech', 'Melee hits heal you for 20% of the damage dealt', '2 fewer hearts of max health',
              'She drinks deep. So will you.'),
    'night_hunter': ('Pact of the Night Hunter', '+35% melee damage at night and underground',
                     '-20% melee damage under the daytime sky', 'The dark asks for nothing. Until dawn.'),
    'toad': ('Pact of the Toad', 'Jump 40% higher; fall damage starts 6 blocks later', 'You burn twice as long',
             'Cold-blooded things fear the fire.'),
    'stoneskin': ('Pact of Stoneskin', '+4 armor and +2 armor toughness', '-12% movement speed',
                  'Heavy as a gravestone, and as hard.'),
    'hare': ('Pact of the Hare', '+18% movement speed; step up full blocks', '+50% fall damage', 'Run, little hare. Mind the drop.'),
    'magpie': ('Pact of the Magpie', '+3 Luck (better loot and fishing); remedies cost a quarter less', '1 fewer heart of max health',
               'Shiny things find their way to you.'),
    'witchfire': ('Pact of Witchfire', 'Melee hits set targets on fire for 4 seconds', '-25% melee damage while wet (water or rain)',
                  'Fire in the blood hates the rain.'),
    'moth': ('Pact of the Moth', 'Permanent night vision', '-10% speed and -20% mining speed under the daytime sky',
             'Made for the lamplight, blinded by the sun.'),
    'wolf': ('Pact of the Wolf', 'Every kill restores 2 hunger and saturation', 'Hunger drains 50% faster',
             'Always hungry. Always hunting.'),
    'briar': ('Pact of the Briar', 'Melee attackers take 30% of the damage they deal back', '-10% attack speed',
              'Touch the rose, bleed for it.'),
    'raven': ('Pact of the Raven', '20% lower gravity, softer landings, +1 block reach', '-25% armor', 'Light bones break easily.'),
    'undying': ('Pact of the Undying', 'Once a day, a killing blow leaves you on 2 hearts with Regeneration II',
                '3 fewer hearts of max health', 'Death keeps a ledger. She will collect.'),
}
HEXES = {
    'leaden_limbs': ('Hex of Leaden Limbs', '-15% movement speed, -25% jump height'),
    'gnawing_hunger': ('Hex of Gnawing Hunger', 'Hunger drains twice as fast'),
    'black_cat': ('Hex of the Black Cat', '-4 Luck: worse loot and fishing'),
    'brittle_bones': ('Hex of Brittle Bones', 'Double fall damage, starting a block sooner'),
    'withering_grip': ('Hex of the Withering Grip', '-20% melee damage, -10% attack speed'),
    'insomnia': ('Hex of Insomnia', 'You cannot sleep'),
    'frail_heart': ('Hex of the Frail Heart', '3 fewer hearts of max health'),
    'clumsy_fingers': ('Hex of Clumsy Fingers', '-25% mining speed, -1 block reach'),
}
OBJECTIVES = {
    'swim': 'Swim %s blocks', 'sprint': 'Sprint %s blocks', 'eat': 'Eat %s meals', 'fish': 'Catch %s fish',
    'ring_bells': 'Ring a bell %s times', 'trade': 'Trade with villagers %s times', 'slay_skeletons': 'Slay %s skeletons',
    'slay_any': 'Slay %s creatures', 'slay_zombies': 'Slay %s zombies', 'place_candles': 'Place %s candles',
    'slay_phantoms': 'Slay %s phantoms', 'breed': 'Breed %s animals', 'golden_apple': 'Eat %s golden apple',
    'mine_stone': 'Mine %s stone or deepslate', 'mine_logs': 'Chop %s logs',
}
ERRANDS = {
    # id: (title, objective text)
    'spider_eyes': ('Eyes for the Cauldron', 'Bring %s spider eyes'), 'toadstools': ('Toadstools', 'Bring %s mushrooms'),
    'bones': ('Bones for the Broth', 'Bring %s bones'), 'feathers': ('Feathers for a Fetch', 'Bring %s feathers'),
    'glow_berries': ('Glow for the Dark', 'Bring %s glow berries'), 'ink': ('Ink for the Grimoire', 'Bring %s ink sacs'),
    'rabbit_foot': ('A Lucky Foot', "Bring %s rabbit's foot"), 'membranes': ('Wings of the Night', 'Bring %s phantom membranes'),
    'slime': ('Slime for Salves', 'Bring %s slimeballs'), 'pearls': ('Eyes of the End', 'Bring %s ender pearls'),
    'honey': ('Something Sweet', 'Bring %s honey bottles'), 'amethyst': ('Singing Stones', 'Bring %s amethyst shards'),
    'drowned': ('The Drowned Dead', 'Slay %s drowned'), 'restless_dead': ('The Restless Dead', 'Slay %s zombies or husks'),
    'creepers': ('Hissing Things', 'Slay %s creepers'), 'spiders': ('Eight Legs Too Many', 'Slay %s spiders'),
    'endermen': ('Tall Strangers', 'Slay %s endermen'), 'illagers': ('The Witch-Burners', 'Slay %s illagers'),
    'slimes': ('Bouncing Blobs', 'Slay %s slimes'), 'fishing': ('Supper', 'Catch %s fish'),
}
TIERS = {
    'hunted': ('Hunted', 'The Coven hunts you. Witches attack on sight and hex you more often, and no resident will deal with you.'),
    'distrusted': ('Distrusted', 'The witches watch you. Residents will not strike pacts with you and lifting a hex costs double.'),
    'stranger': ('Stranger', 'The Coven does not know you yet. You may hold one pact.'),
    'known': ('Known', 'The Coven knows your face. Residents sell Poppets, and remedies are cheaper.'),
    'trusted': ('Trusted', 'A friend of the Coven: two pacts, and remedies at a fair price.'),
    'kin': ('Kin', 'Kin of the Coven: three pacts, free remedies, and wild witches leave you be.'),
}

LANG = {
    'entity.better_witch_huts.coven_witch': 'Coven Witch',
    'item.better_witch_huts.grimoire': 'Grimoire',
    'item.better_witch_huts.grimoire.tooltip': 'Your standing, pacts, hexes and errand',
    'item.better_witch_huts.witch_bottle': 'Witch Bottle',
    'item.better_witch_huts.witch_bottle.tooltip': 'Smash it to break your oldest hex',
    'item.better_witch_huts.poppet': 'Poppet',
    'item.better_witch_huts.poppet.tooltip': 'Carried, it takes the next hex cast at you',
    'modmenu.nameTranslation.better_witch_huts': 'Witchbound',
    'modmenu.descriptionTranslation.better_witch_huts': 'Pacts, hexes and the Coven: a witchcraft RPG.',
    'message.better_witch_huts.standing.change': 'Coven Standing %s · %s',
    'message.better_witch_huts.standing.up': 'The Coven now counts you as %s.',
    'message.better_witch_huts.standing.down': 'The Coven now sees you as %s.',
    'message.better_witch_huts.hexed': '%s hexed you: %s.',
    'message.better_witch_huts.hexed.task': 'Break it: %s, or find a Witch Bottle or a resident witch.',
    'message.better_witch_huts.hex_broken': 'The %s breaks.',
    'message.better_witch_huts.hex_lifted': 'The witch lifts the %s.',
    'message.better_witch_huts.hex_bottle': 'The Witch Bottle shatters, and takes the %s with it.',
    'message.better_witch_huts.no_hex': 'You carry no hex to break.',
    'message.better_witch_huts.poppet': 'Your Poppet catches the %s and burns away.',
    'message.better_witch_huts.interrupted': 'You broke the witch\'s spell!',
    'message.better_witch_huts.pact_struck': 'You strike the %s. Every gift has a price.',
    'message.better_witch_huts.pact_renounced': 'The %s is undone.',
    'message.better_witch_huts.no_slot': 'You can hold no more pacts at your standing.',
    'message.better_witch_huts.undying': 'The Undying pact holds. Not today.',
    'message.better_witch_huts.refuses': '%s will not deal with you.',
    'message.better_witch_huts.grudge': 'You killed %s. The Coven will remember.',
    'message.better_witch_huts.errand_ready': 'Errand done: bring it to any resident witch.',
    'message.better_witch_huts.errand_done': '%s pays you %s emeralds.',
    'message.better_witch_huts.offering': '%s accepts your offering.',
    'message.better_witch_huts.offering_cap': '%s has had enough gifts today.',
    'message.better_witch_huts.insomnia': 'The hex will not let you rest.',
    'message.better_witch_huts.emeralds': 'You need %s emeralds.',
    'command.better_witch_huts.info.standing': '%s: Coven Standing %s (%s)',
    'command.better_witch_huts.info.pacts': 'Pacts %s/%s:',
    'command.better_witch_huts.info.hex': '%s: %s (%s/%s)',
    'command.better_witch_huts.info.errand': 'Errand: %s (%s/%s)',
    'gui.better_witch_huts.standing': '%s (%s)',
    'gui.better_witch_huts.tab.pacts': 'Pacts',
    'gui.better_witch_huts.tab.hexes': 'Hexes',
    'gui.better_witch_huts.tab.errand': 'Errand',
    'gui.better_witch_huts.tab.remedies': 'Remedies',
    'gui.better_witch_huts.tab.record': 'Record',
    'gui.better_witch_huts.offers': 'Tonight she offers',
    'gui.better_witch_huts.strike': 'Strike',
    'gui.better_witch_huts.renounce': 'Undo %s◆',
    'gui.better_witch_huts.your_pacts': 'Your pacts (%s/%s)',
    'gui.better_witch_huts.no_pacts': 'You hold no pacts.',
    'gui.better_witch_huts.no_pacts_tier': 'She will strike no pact at your standing.',
    'gui.better_witch_huts.slots_full': 'Your pacts are full. Raise your standing or undo one.',
    'gui.better_witch_huts.gift': 'Gift: ',
    'gui.better_witch_huts.price': 'Price: ',
    'gui.better_witch_huts.your_hexes': 'Your hexes (%s/%s)',
    'gui.better_witch_huts.no_hexes': 'No hex weighs on you.',
    'gui.better_witch_huts.break': 'Break: %s',
    'gui.better_witch_huts.progress': '%s / %s',
    'gui.better_witch_huts.hex_cures': 'Hexes stay through death and milk. Do the task, smash a Witch Bottle, or pay a resident.',
    'gui.better_witch_huts.lift': 'Lift %s◆',
    'gui.better_witch_huts.lift_free': 'Lift',
    'gui.better_witch_huts.refused': 'Refused',
    'gui.better_witch_huts.needs_known': 'Known+',
    'gui.better_witch_huts.errand_active': 'Your errand',
    'gui.better_witch_huts.errand_offer': 'She needs',
    'gui.better_witch_huts.errand_return': 'Bring it to any resident witch.',
    'gui.better_witch_huts.no_errand_ledger': 'No errand. Resident witches hand them out.',
    'gui.better_witch_huts.errand_hint': 'One errand at a time. Hand it in at any resident; a new one is offered each day.',
    'gui.better_witch_huts.accept': 'Accept',
    'gui.better_witch_huts.turn_in': 'Hand in',
    'gui.better_witch_huts.abandon': 'Abandon',
    'gui.better_witch_huts.reward': '+%s standing, %s emeralds',
    'gui.better_witch_huts.emeralds': 'Your emeralds: %s',
    'gui.better_witch_huts.buy': 'Buy %s◆',
    'gui.better_witch_huts.offering': 'Leave an offering',
    'gui.better_witch_huts.offering_desc': 'Spider eyes, sugar, glowstone, redstone, bottles, nether wart: 2 items, 1 standing.',
    'gui.better_witch_huts.offering_have': 'You carry %s · %s standing left today',
    'gui.better_witch_huts.offer': 'Offer',
    'gui.better_witch_huts.record.pacts_struck': 'Pacts struck',
    'gui.better_witch_huts.record.hexes_broken': 'Hexes broken by your own hand',
    'gui.better_witch_huts.record.errands_done': 'Errands done',
    'gui.better_witch_huts.record.huts': 'Coven huts visited',
    'gui.better_witch_huts.next_tier': 'Next: %s at %s standing.',
}


class D:
    modifier = False
    legacy = False

    @staticmethod
    def ingredient(value):
        if not D.legacy:
            return value
        return {'tag': value[1:]} if value.startswith('#') else {'item': value}

    @staticmethod
    def fn(kind, **fields):
        return {('type' if D.modifier else 'function'): f'minecraft:{kind}', **fields}

    @staticmethod
    def uniform(lo, hi):
        return {'type': 'minecraft:uniform', 'min': lo, 'max': hi}

    @staticmethod
    def item(x):
        name = x['item'] if ':' in x['item'] else 'minecraft:' + x['item']
        out = {'type': 'minecraft:item', 'name': name, 'weight': x['weight']}
        functions = []
        if isinstance(x['count'], tuple):
            functions.append(D.fn('set_count', count=D.uniform(*x['count'])))
        elif x['count']:
            functions.append(D.fn('set_count', count=x['count']))
        extra = x['extra']
        if isinstance(extra, tuple) and extra[0] == 'levels':
            functions.append(D.fn('enchant_with_levels', levels=D.uniform(extra[1], extra[2])))
        elif isinstance(extra, str):
            functions.append(D.fn('set_potion', id='minecraft:' + extra))
        if functions:
            if D.modifier:
                out['modifier'] = functions[0] if len(functions) == 1 else functions
            else:
                out['functions'] = functions
        return out

    @staticmethod
    def pool(rolls, entries):
        return {'rolls': D.uniform(*rolls) if isinstance(rolls, tuple) else rolls, 'entries': entries}

    @staticmethod
    def recipe(pattern, key, result, count):
        out = {'type': 'minecraft:crafting_shaped' if pattern else 'minecraft:crafting_shapeless', 'category': 'misc'}
        if pattern:
            out['pattern'] = pattern
            out['key'] = {k: D.ingredient(v) for k, v in key.items()}
        else:
            out['ingredients'] = [D.ingredient(v) for v in key]
        out['result'] = {'id': result, 'count': count}
        return out

    @staticmethod
    def unlock(recipe_id, item):
        return {'parent': 'minecraft:recipes/root',
                'criteria': {'has_item': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': item}]}},
                             'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked',
                                                'conditions': {('recipes' if D.modifier else 'recipe'): recipe_id}}},
                'requirements': [['has_item', 'has_the_recipe']], 'rewards': {'recipes': [recipe_id]}}


def write(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def advancements(lang):
    out = {}

    def display(name, item, title, desc, frame='task', root=False, hidden=False):
        lang[f'advancements.{NS}.{name}.title'] = title
        lang[f'advancements.{NS}.{name}.description'] = desc
        item = item if ':' in item else 'minecraft:' + item
        shown = {'icon': {'id': item}, 'title': {'translate': f'advancements.{NS}.{name}.title', 'fallback': title},
                 'description': {'translate': f'advancements.{NS}.{name}.description', 'fallback': desc}, 'frame': frame,
                 'show_toast': not root, 'announce_to_chat': not root and frame != 'task', 'hidden': hidden}
        if root:
            shown['background'] = 'minecraft:textures/block/mud_bricks.png' if D.legacy else 'minecraft:block/mud_bricks'
        return shown

    def granted(name, parent, item, title, desc, frame='task', hidden=False, xp=0):
        adv = {'parent': f'{NS}:{parent}', 'display': display(name, item, title, desc, frame, hidden=hidden),
               'criteria': {'granted': {'trigger': 'minecraft:impossible'}}, 'requirements': [['granted']]}
        if xp:
            adv['rewards'] = {'experience': xp}
        out[name] = adv

    out['root'] = {'display': display('root', f'{NS}:grimoire', 'Witchbound', 'Every gift has a price', root=True),
                   'criteria': {'tick': {'trigger': 'minecraft:tick'}}, 'requirements': [['tick']]}
    granted('found_hut', 'root', 'cauldron', 'Something Brewing', 'Find a coven hut')
    for sid, (name, resident, _b, _s, _p, _salt, icon) in STRUCTURES.items():
        granted(f'visit_{sid}', 'found_hut', icon, name, f'Find the {name}, home of {resident}')
    granted('all_huts', 'found_hut', 'brewing_stand', 'The Seven Covens', 'Visit all seven coven huts', 'challenge', xp=200)
    granted('first_pact', 'root', 'emerald', 'Every Gift Has a Price', 'Strike a pact with a resident witch', 'goal')
    granted('three_pacts', 'first_pact', 'nether_star', 'Thrice Bound', 'Hold three pacts at once', 'challenge', xp=150)
    granted('undying_saved', 'first_pact', 'totem_of_undying', 'Not Today', 'Survive a killing blow by the Pact of the Undying', 'goal')
    granted('hexed', 'root', 'fermented_spider_eye', 'Hexed!', 'Be hexed by a witch')
    granted('broke_hex', 'hexed', 'golden_carrot', 'Hex Breaker', 'Break a hex by doing its task')
    granted('witch_bottle', 'hexed', f'{NS}:witch_bottle', 'Bottled Up', 'Smash a Witch Bottle to break a hex')
    granted('poppet_ward', 'hexed', f'{NS}:poppet', 'Take It Instead', 'Let a Poppet catch a hex meant for you', 'goal')
    granted('three_hexes', 'hexed', 'wither_rose', 'Cursed Thrice Over', 'Carry three hexes at once', 'goal', hidden=True)
    granted('known', 'root', 'glass_bottle', 'Known to the Coven', 'Reach Known standing with the Coven')
    granted('trusted', 'known', 'amethyst_shard', 'Trusted', 'Reach Trusted standing with the Coven', 'goal')
    granted('kin', 'trusted', 'black_candle', 'Kin of the Coven', 'Reach Kin standing with the Coven', 'challenge', xp=250)
    granted('first_errand', 'root', 'paper', 'Errand Runner', 'Hand in a witch\'s errand')
    granted('ten_errands', 'first_errand', 'writable_book', 'Familiar Face', 'Hand in ten errands', 'goal', xp=100)
    granted('hunted', 'root', 'iron_sword', 'Witch Hunt', 'Fall so low the Coven hunts you', 'goal', hidden=True)
    granted('killed_resident', 'root', 'skeleton_skull', 'Burn the Witch', 'Kill a resident witch. The Coven will remember.',
            'goal', hidden=True)
    return out


def structure_json(sid, meta):
    s = meta[sid]
    site = {k: s[k] for k in ('mode', 'max_slope', 'min_depth', 'max_depth', 'max_above_sea', 'any_wet', 'y_offset', 'max_rise')
            if k in s}
    return {'type': f'{NS}:hut', 'biomes': f'#{NS}:has_structure/{sid}', 'step': 'surface_structures', 'spawn_overrides': {},
            'terrain_adaptation': 'beard_thin', 'template': f'{NS}:{TEMPLATES.get(sid, sid)}', 'config_name': sid, 'site': site,
            'anchor': s['anchor'], 'dry': s['dry'], 'wet': s['wet'], 'around': s['around'], 'search': s.get('search', 1)}


def assets(res: Path, lang):
    a = res / 'assets' / NS
    for item in ITEMS:
        write(a / f'models/item/{item}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{item}'}})
        if not D.legacy:
            write(a / f'items/{item}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{item}'}})
    shutil.copytree(TOOLS / 'assets' / 'textures', a / 'textures')
    write(a / 'lang/en_us.json', dict(sorted(lang.items())))


def copy_template(src: Path, dst: Path):
    """Copies a template; on 1.21.1 a resident's plain-text name is turned into the JSON text that version reads."""
    if not D.legacy:
        shutil.copy(src, dst)
        return
    import gzip
    from nbtlib import File, String, load
    root = load(src)
    for entity in root['entities']:
        nbt = entity['nbt']
        name = str(nbt.get('CustomName', ''))
        if name and not name.startswith('{'):
            nbt['CustomName'] = String(json.dumps({'text': name}, ensure_ascii=False))
    with gzip.GzipFile(dst, 'wb', mtime=0) as out:
        File(root).write(out, byteorder='big')


def generate(v):
    D.modifier = v == '26.3'
    D.legacy = v == '1.21.1'
    res = REPO / v / 'common/src/main/resources'
    for stale in (res / 'assets' / NS, res / 'data'):
        shutil.rmtree(stale, ignore_errors=True)
    write(res / 'pack.mcmeta', {'pack': {'description': 'Witchbound resources', **PACK[v]}})
    data = res / 'data' / NS
    lang = dict(LANG)
    for pid, (name, gift, price, lore) in PACTS.items():
        lang[f'effect.{NS}.pact_{pid}'] = name
        lang[f'pact.{NS}.{pid}.gift'] = gift
        lang[f'pact.{NS}.{pid}.price'] = price
        lang[f'pact.{NS}.{pid}.lore'] = lore
    for hid, (name, effect) in HEXES.items():
        lang[f'effect.{NS}.hex_{hid}'] = name
        lang[f'hex.{NS}.{hid}.effect'] = effect
    for oid, text in OBJECTIVES.items():
        lang[f'objective.{NS}.{oid}'] = text
    for eid, (title, text) in ERRANDS.items():
        lang[f'errand.{NS}.{eid}'] = title
        lang[f'objective.{NS}.errand_{eid}'] = text
    for tid, (name, desc) in TIERS.items():
        lang[f'tier.{NS}.{tid}'] = name
        lang[f'gui.{NS}.tier_desc.{tid}'] = desc
    for name, pools in LOOT.items():
        write(data / 'loot_table' / f'{name}.json',
              {'type': 'minecraft:chest', 'random_sequence': f'{NS}:{name}', 'pools': [D.pool(r, [D.item(x) for x in entries])
                                                                                   for r, entries in pools]})
    for name, adv in advancements(lang).items():
        write(data / 'advancement' / f'{name}.json', adv)
    for rid, (pattern, key, result, count, item) in RECIPES.items():
        write(data / 'recipe' / f'{rid}.json', D.recipe(pattern, key, result, count))
        write(data / 'advancement/recipes/misc' / f'{rid}.json', D.unlock(f'{NS}:{rid}', item))
    meta = json.loads((TOOLS / 'assets/structures/sites.json').read_text())
    for sid, (name, resident, biomes, spacing, separation, salt, _icon) in STRUCTURES.items():
        lang[f'structure.{NS}.{sid}'] = name
        write(data / 'worldgen/structure' / f'{sid}.json', structure_json(sid, meta))
        write(data / 'worldgen/structure_set' / f'{sid}.json', {
            'structures': [{'structure': f'{NS}:{sid}', 'weight': 1}],
            'placement': {'type': 'minecraft:random_spread', 'spacing': spacing, 'separation': separation, 'salt': salt,
                          'exclusion_zone': {'other_set': 'minecraft:villages', 'chunk_count': VILLAGE_CLEARANCE}}})
        write(data / 'tags/worldgen/biome/has_structure' / f'{sid}.json', {'values': biomes})
        template = TEMPLATES.get(sid, sid)
        (data / 'structure').mkdir(parents=True, exist_ok=True)
        copy_template(TOOLS / 'assets/structures' / f'{template}.nbt', data / 'structure' / f'{template}.nbt')
    shutil.copy(TOOLS / 'assets/structures/witch_hut.nbt', data / 'structure' / 'witch_hut.nbt')
    write(data / 'tags/worldgen/structure/coven_huts.json', {'values': [f'{NS}:{s}' for s in STRUCTURES]})
    # The 1.0 jigsaw pool, kept for huts 1.0 left half-generated.
    write(data / 'worldgen/template_pool/witch_hut.json', {
        'name': f'{NS}:witch_hut', 'fallback': 'minecraft:empty',
        'elements': [{'weight': 1, 'element': {'element_type': 'minecraft:single_pool_element', 'location': f'{NS}:witch_hut',
                                               'projection': 'rigid', 'processors': 'minecraft:empty'}}]})
    assets(res, lang)
    return lang


def main():
    for v in VERSIONS:
        if (REPO / v).is_dir():
            lang = generate(v)
            print('generated', v, len(lang), 'lang keys')


if __name__ == '__main__':
    main()
