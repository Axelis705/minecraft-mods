#!/usr/bin/env python3
"""Modelo aproximado de Block.canSurvive de Minecraft 1.20.1 para los bloques que usan las estructuras.

Uso: apoyo.py <volcado.txt> [--lista TIPO]
Dice cuantos bloques del plano no pueden sostenerse (antorcha sin muro, farola en el aire, alfombra sin suelo, flor sin tierra...).
El servidor de CI quita esos mismos bloques al construir (Estructuras.dropUnsupported) y escribe el conteo por tipo; este script
sirve para encontrarlos y arreglarlos en el diseno SIN esperar al servidor.
"""
import sys
from collections import Counter, defaultdict

DIRS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}
OPP = {'north': 'south', 'south': 'north', 'west': 'east', 'east': 'west', 'up': 'down', 'down': 'up'}

cells = {}


def parse(s):
    s = s.replace('minecraft:', '')
    if '[' in s:
        n, pr = s[:-1].split('[', 1)
        d = {}
        for kv in pr.split(','):
            if '=' in kv:
                k, v = kv.split('=', 1)
                d[k.strip()] = v.strip()
        return n, d
    return s, {}


def load(path):
    cells.clear()
    for ln in open(path):
        p = ln.split(None, 3)
        if len(p) < 4:
            continue
        n, d = parse(p[3].strip())
        cells[(int(p[0]), int(p[1]), int(p[2]))] = (n, d)


def get(x, y, z):
    c = cells.get((x, y, z))
    if c is not None:
        return c
    if y == -1:
        return ('grass_block', {})
    if y < -1:
        return ('dirt', {}) if y >= -4 else ('stone', {})
    return ('air', {})


NOT_FULL_EXACT = {
    'chest', 'trapped_chest', 'ender_chest', 'enchanting_table', 'lectern', 'brewing_stand', 'cauldron', 'water_cauldron', 'lava_cauldron',
    'powder_snow_cauldron', 'campfire', 'soul_campfire', 'anvil', 'chipped_anvil', 'damaged_anvil', 'bell', 'grindstone', 'composter', 'hopper',
    'lightning_rod', 'end_rod', 'chain', 'lantern', 'soul_lantern', 'iron_bars', 'cobweb', 'lily_pad', 'cake', 'stonecutter', 'sculk_sensor',
    'dragon_egg', 'daylight_detector', 'conduit', 'end_portal_frame', 'farmland', 'dirt_path', 'soul_sand', 'barrier', 'light', 'water', 'lava',
    'air', 'cave_air', 'void_air', 'scaffolding', 'big_dripleaf', 'small_dripleaf', 'ladder', 'vine', 'glow_lichen', 'snow', 'sea_pickle',
    'flower_pot', 'turtle_egg', 'pointed_dripstone', 'amethyst_cluster', 'large_amethyst_bud', 'medium_amethyst_bud', 'small_amethyst_bud',
    'bamboo', 'cocoa', 'tripwire', 'tripwire_hook', 'lever', 'rail', 'powered_rail', 'detector_rail', 'activator_rail', 'redstone_wire',
    'azalea', 'flowering_azalea', 'fern', 'grass', 'tall_grass', 'large_fern', 'dead_bush', 'sugar_cane', 'kelp', 'seagrass', 'torch', 'wall_torch',
    'soul_torch', 'soul_wall_torch', 'redstone_torch', 'redstone_wall_torch', 'moss_carpet', 'skeleton_skull', 'skeleton_wall_skull', 'wither_skeleton_skull',
    'zombie_head', 'player_head', 'creeper_head', 'dragon_head', 'hanging_roots', 'spore_blossom', 'candle', 'brown_mushroom', 'red_mushroom',
    'sculk_vein', 'sculk_shrieker', 'sculk_catalyst', 'bubble_column', 'fire', 'soul_fire', 'item_frame', 'decorated_pot', 'mud', 'honey_block',
    'slime_block', 'cave_vines', 'cave_vines_plant', 'weeping_vines', 'twisting_vines', 'piston_head', 'moving_piston',
}
NOT_FULL_SUFFIX = ('_stairs', '_slab', '_wall', '_fence', '_fence_gate', '_pane', '_carpet', '_candle', '_banner', '_wall_banner', '_door', '_trapdoor',
                   '_button', '_pressure_plate', '_sign', '_wall_sign', '_hanging_sign', '_wall_hanging_sign', '_bed', '_shulker_box', '_candle_cake',
                   '_sapling', '_tulip', '_head', '_wall_head', '_skull', '_wall_skull', '_leaves', '_fungus', '_roots', '_coral', '_coral_fan',
                   '_coral_wall_fan', '_pot', '_bush', '_stem', '_vine', '_vines')
FLOWERS = {'dandelion', 'poppy', 'blue_orchid', 'allium', 'azure_bluet', 'red_tulip', 'orange_tulip', 'white_tulip', 'pink_tulip', 'oxeye_daisy',
           'cornflower', 'lily_of_the_valley', 'wither_rose', 'torchflower', 'sunflower', 'lilac', 'rose_bush', 'peony', 'pitcher_plant'}
BUSH = FLOWERS | {'fern', 'grass', 'tall_grass', 'large_fern', 'azalea', 'flowering_azalea', 'sweet_berry_bush', 'brown_mushroom', 'red_mushroom'}
DIRT_TAG = {'dirt', 'grass_block', 'podzol', 'coarse_dirt', 'mycelium', 'rooted_dirt', 'moss_block', 'mud', 'muddy_mangrove_roots'}


def is_full(n):
    if n in NOT_FULL_EXACT:
        return False
    for s in NOT_FULL_SUFFIX:
        if n.endswith(s):
            return False
    if n.startswith('potted_'):
        return False
    return True


def sturdy(c, face):
    """isFaceSturdy (cara completa)."""
    n, d = c
    if n.endswith('_stairs'):
        half = d.get('half', 'bottom')
        if face == 'down':
            return half == 'bottom'
        if face == 'up':
            return half == 'top'
        return d.get('facing', 'north') == face and d.get('shape', 'straight') == 'straight'
    if n.endswith('_slab'):
        t = d.get('type', 'bottom')
        if t == 'double':
            return True
        return (t == 'bottom' and face == 'down') or (t == 'top' and face == 'up')
    return is_full(n)


def center(c, face):
    """canSupportCenter (el centro de la cara)."""
    n, d = c
    if sturdy(c, face):
        return True
    if n == 'chain':
        return d.get('axis', 'y') == 'y' and face in ('up', 'down')
    if n.endswith('_wall') or n.endswith('_fence') or n == 'iron_bars' or n.endswith('_pane') or n.endswith('_fence_gate'):
        return face in ('up', 'down')
    if n.endswith('_stairs'):
        return False
    if n.endswith('_slab'):
        return False
    if n in ('lantern', 'soul_lantern'):
        return face == 'up' and d.get('hanging', 'false') != 'true'
    if n in ('end_rod', 'lightning_rod'):
        return face in ('up', 'down')
    return False


def collision_full(c, face):
    """isFaceFull(collision) de un vecino para las enredaderas (las hojas si cuentan)."""
    n, d = c
    if n.endswith('_leaves'):
        return True
    return sturdy(c, face)


def survives(x, y, z, c):
    n, d = c
    below = get(x, y - 1, z)
    above = get(x, y + 1, z)
    if n in ('torch', 'soul_torch', 'redstone_torch'):
        return center(below, 'up')
    if n in ('wall_torch', 'soul_wall_torch', 'redstone_wall_torch'):
        f = d.get('facing', 'north')
        dx, dy, dz = DIRS[OPP[f]]
        return sturdy(get(x + dx, y, z + dz), f)
    if n in ('lantern', 'soul_lantern'):
        if d.get('hanging', 'false') == 'true':
            return center(above, 'down')
        return center(below, 'up')
    if n.endswith('_carpet') or n == 'moss_carpet':
        return below[0] != 'air'
    if n in BUSH:
        if n in ('tall_grass', 'large_fern', 'sunflower', 'lilac', 'rose_bush', 'peony'):
            if d.get('half') == 'upper':
                return below[0] == n
        if n in ('brown_mushroom', 'red_mushroom'):
            return False  # necesitan oscuridad o podzol/micelio; el juego no se puede predecir
        if n in ('azalea', 'flowering_azalea'):
            return below[0] in DIRT_TAG or below[0] == 'clay'
        return below[0] in DIRT_TAG
    if n.endswith('_candle') or n == 'candle':
        return center(below, 'up')
    if n == 'vine':
        for f in ('north', 'south', 'east', 'west', 'up'):
            if d.get(f, 'false') == 'true':
                dx, dy, dz = DIRS[f]
                nb = get(x + dx, y + dy, z + dz)
                if collision_full(nb, OPP[f]):
                    return True
                if f != 'up' and above[0] == 'vine' and above[1].get(f, 'false') == 'true':
                    return True
        return False
    if n.endswith('_wall_banner'):
        f = d.get('facing', 'north')
        dx, dy, dz = DIRS[OPP[f]]
        b = get(x + dx, y, z + dz)
        return b[0] not in ('air', 'water', 'lava') and is_solid_material(b)
    if n in ('amethyst_cluster', 'large_amethyst_bud', 'medium_amethyst_bud', 'small_amethyst_bud'):
        f = d.get('facing', 'up')
        dx, dy, dz = DIRS[OPP[f]]
        return sturdy(get(x + dx, y + dy, z + dz), f)
    if n == 'lily_pad':
        return below[0] == 'water' or below[0] in ('ice', 'frosted_ice')
    if n == 'snow':
        if below[0] in ('ice', 'packed_ice', 'barrier'):
            return False
        return collision_full(below, 'up')
    if n.endswith('_door'):
        if d.get('half') == 'upper':
            return below[0] == n
        return sturdy(below, 'up')
    if n.endswith('_pressure_plate'):
        return center(below, 'up')
    if n.endswith('_button') or n == 'lever':
        face = d.get('face', 'wall')
        if face == 'floor':
            return sturdy(below, 'up')
        if face == 'ceiling':
            return sturdy(above, 'down')
        f = d.get('facing', 'north')
        dx, dy, dz = DIRS[OPP[f]]
        return sturdy(get(x + dx, y + dy, z + dz), f)
    if n == 'ladder':
        f = d.get('facing', 'north')
        dx, dy, dz = DIRS[OPP[f]]
        return sturdy(get(x + dx, y + dy, z + dz), f)
    if n == 'cake':
        return below[0] != 'air' and below[0] != 'water'
    return True


def is_solid_material(c):
    n, d = c
    if n.endswith(('_carpet', '_candle', '_banner', '_sign')) or n in BUSH or n in ('torch', 'wall_torch', 'vine', 'lantern', 'chain', 'moss_carpet'):
        return False
    return True


def audit(path, lista=None):
    load(path)
    removed = {}
    total_by = Counter()
    for _ in range(6):
        changed = 0
        for (x, y, z), c in list(cells.items()):
            if c[0] in ('air', 'water'):
                continue
            if not survives(x, y, z, c):
                removed[(x, y, z)] = c
                cells[(x, y, z)] = ('air', {})
                total_by[c[0]] += 1
                changed += 1
        if not changed:
            break
    return removed, total_by


if __name__ == '__main__':
    path = sys.argv[1]
    lista = sys.argv[sys.argv.index('--lista') + 1] if '--lista' in sys.argv else None
    rem, by = audit(path)
    print(path, 'sin apoyo:', sum(by.values()), dict(by.most_common()))
    if lista:
        pos = sorted(p for p, c in rem.items() if c[0] == lista)
        for p in pos[:60]:
            print(p, rem[p])
