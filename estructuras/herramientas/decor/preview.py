#!/usr/bin/env python3
"""Vista previa de decoracion nueva con el visor (texturas, luz y brillo de verdad).

Uso: preview.py [id ...] [--day]   -> /tmp/decor_prev/<id>.png (cerca) y all.png (sala de exposicion)
Cada decoracion se pone sobre un pedestal de deepslate; las de pared, contra un muro; las colgantes, bajo un techo.
"""
import json, os, subprocess, sys

here = os.path.dirname(os.path.abspath(__file__))
visor = os.path.join(here, '..', 'visor')
OUT = '/tmp/decor_out'
PREV = '/tmp/decor_prev'
os.makedirs(PREV, exist_ok=True)
args = [a for a in sys.argv[1:] if not a.startswith('--')]
day = '--day' in sys.argv
subprocess.run([sys.executable, os.path.join(here, 'build.py'), OUT], check=True, stdout=subprocess.DEVNULL)
man = json.load(open(f'{OUT}/data/mundos_de_axelis/est_decor.json'))['blocks']
ids = [b['id'] for b in man if not args or b['id'].replace('est_', '') in args or b['id'] in args]
kinds = {b['id']: b for b in man}
cells = {}
n = len(ids)
W = 5 * n + 3
for x in range(-3, W):
    for z in range(-3, 9):
        cells[(x, -1, z)] = 'polished_deepslate'
    for y in range(0, 7):
        cells[(x, y, 6)] = 'deepslate_bricks'
    for z in range(-3, 7):
        cells[(x, 7, z)] = 'deepslate_tiles'
for x in range(-3, W):
    for y in range(0, 7):
        cells[(x, y, -3)] = 'air'
pos = {}
for i, bid in enumerate(ids):
    x = 3 + i * 5
    b = kinds[bid]
    nm = 'mundos_de_axelis:' + bid
    cells[(x, -1, 2)] = 'polished_blackstone'
    if b['kind'] == 'wall' and b['tall']:
        cells[(x, 1, 5)] = f'{nm}[facing=north,half=lower]'
        cells[(x, 2, 5)] = f'{nm}[facing=north,half=upper]'
        pos[bid] = (x, 2.0, 5)
    elif b['kind'] == 'wall':
        cells[(x, 2, 5)] = f'{nm}[facing=north]'
        pos[bid] = (x, 2.5, 5)
    elif b['kind'] == 'hang':
        cells[(x, 6, 2)] = f'{nm}[facing=north]'
        pos[bid] = (x, 6.5, 2)
    elif b['tall']:
        cells[(x, 0, 2)] = f'{nm}[facing=north,half=lower]'
        cells[(x, 1, 2)] = f'{nm}[facing=north,half=upper]'
        pos[bid] = (x, 1.5, 2)
    elif b['kind'] == 'plain':
        cells[(x, 0, 2)] = nm
        pos[bid] = (x, 0.6, 2)
    else:
        cells[(x, 0, 2)] = f'{nm}[facing=north]'
        pos[bid] = (x, 0.6, 2)
with open('/tmp/decor_prev/dump.txt', 'w') as f:
    for (x, y, z), s in cells.items():
        f.write(f'{x} {y} {z} {s}\n')
env = dict(os.environ, EXTRA_ASSETS=OUT)
sky = '--day' if day else '--night'
subprocess.run([sys.executable, os.path.join(visor, 'bake.py'), '/tmp/decor_prev/dump.txt', '/tmp/decor_prev/bk', sky, '--box', '-3', str(W), '-2', '9', '-3', '8'], check=True, env=env)
shots = []
for bid in ids:
    x, y, z = pos[bid]
    shots.append(f'{x},{y + 0.6},{z + 4.2},0,6,48')   # camara delante, mirando al sur->norte? yaw 180 mira al norte
shots2 = []
for bid in ids:
    x, y, z = pos[bid]
    shots2.append(f'{x},{y + 0.7},{z - 4.2},0,6,48')
subprocess.run([sys.executable, os.path.join(visor, 'shoot.py'), '/tmp/decor_prev/bk', '/tmp/decor_prev/s'] + shots2, check=True)
for i, bid in enumerate(ids):
    os.replace(f'/tmp/decor_prev/s_{i}.png', f'{PREV}/{bid}.png')
if '--close' in sys.argv:
    # tomas de cerca desde 3 angulos (yaw 0 mira al sur; el juego usa direccion = (-sin yaw, 0, cos yaw))
    import math
    for bid in ids:
        d = 3.1 if kinds[bid]['tall'] else 2.1
        x, y, z = pos[bid]
        tx, tz = x + 0.5, z + 0.5
        ty = y + 0.2 if kinds[bid]['kind'] != 'hang' else y - 0.35
        cs = []
        for yaw in (0, 40, -40):
            a = math.radians(yaw)
            cs.append(f'{tx + d * math.sin(a):.2f},{ty + 0.25:.2f},{tz - d * math.cos(a):.2f},{yaw},8,50')
        subprocess.run([sys.executable, os.path.join(visor, 'shoot.py'), '/tmp/decor_prev/bk', f'/tmp/decor_prev/c_{bid}'] + cs, check=True, stdout=subprocess.DEVNULL)
        from PIL import Image
        ims = [Image.open(f'/tmp/decor_prev/c_{bid}_{i}.png').convert('RGB') for i in range(3)]
        w, h = ims[0].size
        m = Image.new('RGB', (w * 3 // 2, h // 2))
        for i, im in enumerate(ims):
            m.paste(im.resize((w // 2, h // 2)), (i * w // 2, 0))
        m.save(f'/tmp/decor_prev/m_{bid}.png')
print('listo', ids)
