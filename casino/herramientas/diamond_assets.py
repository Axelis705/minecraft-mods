#!/usr/bin/env python3
"""Recursos de THE DIAMOND: panel del elevador, vitrina de premios y tarjetas VIP (texturas, modelos, estados, botín).

    python3 casino/herramientas/diamond_assets.py
"""
import json, os, random
from PIL import Image

R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = os.path.join(R, 'assets', 'casino')
D = os.path.join(R, 'data', 'casino')

def save_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f: json.dump(obj, f, indent=1)

def tex(path, im):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)

def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)

# ------------------------------------------------------------------ texturas
def elevator_panel():
    im = Image.new('RGBA', (16, 16), (16, 16, 20, 255)); p = im.load()
    # marco de latón
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)): p[x, y] = (200, 160, 70, 255)
        for (x, y) in ((i, 1), (1, i)): p[x, y] = (240, 210, 120, 255)
        for (x, y) in ((i, 14), (14, i)): p[x, y] = (130, 96, 30, 255)
    # pantallita con flechas arriba/abajo
    for x in range(3, 13):
        for y in range(2, 6): p[x, y] = (8, 30, 20, 255)
    for (x, y) in ((6, 3), (5, 4), (6, 4), (7, 4)): p[x, y] = (80, 255, 140, 255)
    for (x, y) in ((9, 4), (8, 3), (9, 3), (10, 3)): p[x, y] = (255, 120, 80, 255)
    # botones (2 columnas x 4 filas) con anillo dorado
    for r in range(4):
        for c in range(2):
            bx, by = 4 + c * 5, 7 + r * 2
            for dx in range(3):
                p[bx + dx, by] = (230, 196, 100, 255)
            p[bx + 1, by] = (255, 248, 220, 255) if (r + c) % 3 == 0 else (250, 230, 160, 255)
    return im

def card(base, hi, stripe):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
    for x in range(1, 15):
        for y in range(4, 13):
            corner = (x in (1, 14)) and (y in (4, 12))
            if corner: continue
            t = (x + (12 - y)) / 22
            p[x, y] = mix(base, hi, t)
    for x in range(1, 15):
        p[x, 6] = stripe; p[x, 7] = stripe
    # rombo (THE DIAMOND) y chip
    for (x, y) in ((11, 9), (10, 10), (11, 10), (12, 10), (11, 11)): p[x, y] = (90, 220, 255, 255)
    for (x, y) in ((3, 9), (4, 9), (3, 10), (4, 10)): p[x, y] = (230, 200, 110, 255)
    for x in range(2, 9): p[x, 12] = mix(base, (0, 0, 0), 0.25)
    return im

tex(os.path.join(A, 'textures/block/decor/elevator_panel.png'), elevator_panel())
tex(os.path.join(A, 'textures/item/vip_card_silver.png'), card((150, 156, 168), (240, 244, 252), (40, 40, 48, 255)))
tex(os.path.join(A, 'textures/item/vip_card_gold.png'), card((170, 120, 30), (255, 230, 130), (30, 24, 10, 255)))

# ------------------------------------------------------------------ modelos
def face(t, uv=None):
    f = {'texture': t}
    if uv: f['uv'] = uv
    return f

def box(fr, to, tex_all, faces=None, uv=None):
    fs = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        fs[d] = face((faces or {}).get(d, tex_all))
    return {'from': fr, 'to': to, 'faces': fs}

elevator = {
    'parent': 'block/block',
    'textures': {'brass': 'casino:block/decor/brass', 'panel': 'casino:block/decor/elevator_panel', 'particle': 'casino:block/decor/brass'},
    'elements': [
        box([3, 2, 15], [13, 14, 16], '#brass'),
        {'from': [3.5, 2.5, 14.4], 'to': [12.5, 13.5, 15], 'faces': {
            'north': {'texture': '#panel', 'uv': [0, 0, 16, 16]}, 'east': {'texture': '#brass'}, 'west': {'texture': '#brass'},
            'up': {'texture': '#brass'}, 'down': {'texture': '#brass'}}},
    ]}
save_json(os.path.join(A, 'models/block/decor/elevator.json'), elevator)

shop = {
    'parent': 'block/block',
    'textures': {'wood': 'casino:block/decor/mahogany_panel', 'gold': 'casino:block/decor/gold_trim', 'velvet': 'casino:block/decor/velvet_purple_tufted',
                 'brass': 'casino:block/decor/brass', 'particle': 'casino:block/decor/mahogany_panel'},
    'elements': [
        box([0, 0, 0], [16, 1, 16], '#gold'),
        box([0.5, 1, 0.5], [15.5, 10, 15.5], '#wood', {'up': '#wood', 'down': '#wood'}),
        box([0, 10, 0], [16, 11.5, 16], '#gold'),
        box([2, 11.5, 2], [14, 13, 14], '#velvet'),
        box([0, 11.5, 0], [1, 15, 1], '#brass'), box([15, 11.5, 0], [16, 15, 1], '#brass'),
        box([0, 11.5, 15], [1, 15, 16], '#brass'), box([15, 11.5, 15], [16, 15, 16], '#brass'),
    ]}
save_json(os.path.join(A, 'models/block/decor/prize_shop.json'), shop)

def facing_states(model):
    return {'variants': {f'facing={f}': ({'model': model} if r == 0 else {'model': model, 'y': r})
                         for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}}

save_json(os.path.join(A, 'blockstates/elevator.json'), facing_states('casino:block/decor/elevator'))
save_json(os.path.join(A, 'blockstates/prize_shop.json'), facing_states('casino:block/decor/prize_shop'))
BLOCK_DISPLAY = json.load(open(os.path.join(A, 'models/item/speaker.json')))['display']
save_json(os.path.join(A, 'models/item/elevator.json'), {'parent': 'casino:block/decor/elevator', 'display': BLOCK_DISPLAY})
save_json(os.path.join(A, 'models/item/prize_shop.json'), {'parent': 'casino:block/decor/prize_shop', 'display': BLOCK_DISPLAY})
for c in ('silver', 'gold'):
    save_json(os.path.join(A, f'models/item/vip_card_{c}.json'), {'parent': 'item/generated', 'textures': {'layer0': f'casino:item/vip_card_{c}'}})

for b in ('elevator', 'prize_shop'):
    save_json(os.path.join(D, f'loot_tables/blocks/{b}.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
        {'type': 'minecraft:item', 'name': f'casino:{b}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
print('ok')
