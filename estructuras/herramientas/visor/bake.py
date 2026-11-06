#!/usr/bin/env python3
"""Hornea un plano (volcado x y z estado) en geometria con texturas reales de Minecraft 1.20.1 e iluminacion tipo juego.

Uso: bake.py <volcado.txt> <carpeta_salida> [--assets DIR] [--night|--day] [--box x0 x1 y0 y1 z0 z1]
Las texturas y modelos salen del client-extra.jar de 1.20.1 (se descomprime en --assets, por defecto /tmp/mcassets).
"""
import json, math, os, re, sys, random, struct
from collections import deque
import numpy as np
from PIL import Image

ap = sys.argv[1:]
DUMP, OUT = ap[0], ap[1]
ASSETS = '/tmp/mcassets/assets/minecraft'
EXTRA = os.environ.get('EXTRA_ASSETS', '/tmp/decor_out')   # recursos propios (decoracion nueva): <EXTRA>/assets/<espacio>/...
SKY = 0.28  # noche; con --day sube
BOX = None
i = 2
while i < len(ap):
    if ap[i] == '--assets': ASSETS = ap[i + 1] + '/assets/minecraft'; i += 2
    elif ap[i] == '--day': SKY = 1.0; i += 1
    elif ap[i] == '--night': SKY = 0.28; i += 1
    elif ap[i] == '--box': BOX = [int(v) for v in ap[i + 1:i + 7]]; i += 7
    else: i += 1
os.makedirs(OUT, exist_ok=True)

def jl(p):
    with open(p) as f: return json.load(f)

# ---------------------------------------------------------------- volcado
cells = {}
for ln in open(DUMP):
    p = ln.split(None, 3)
    if len(p) < 4: continue
    cells[(int(p[0]), int(p[1]), int(p[2]))] = p[3].strip()

def split_state(s):
    if '[' in s:
        n, pr = s[:-1].split('[', 1)
        d = {}
        for kv in pr.split(','):
            if '=' in kv:
                k, v = kv.split('=', 1); d[k.strip()] = v.strip()
        return n.replace('minecraft:', ''), d
    return s.replace('minecraft:', ''), {}

# ---------------------------------------------------------------- bloques
AIRS = {'air', 'cave_air', 'void_air', 'barrier', 'light', 'structure_void', 'moving_piston'}
GLASSY = ('glass', 'ice', 'slime_block', 'honey_block', 'tinted_glass')
def is_water(n): return n in ('water',)
def is_lava(n): return n in ('lava',)

class Mdl:
    cache = {}
    @staticmethod
    def load(name):
        name = name.replace('minecraft:', '')
        if name in Mdl.cache: return Mdl.cache[name]
        p = f'{ASSETS}/models/{name}.json'
        if ':' in name:
            ns, path = name.split(':', 1)
            p = f'{EXTRA}/assets/{ns}/models/{path}.json'
        if not os.path.exists(p):
            Mdl.cache[name] = None; return None
        j = jl(p)
        parent = Mdl.load(j['parent']) if 'parent' in j else None
        tex = dict(parent['tex']) if parent else {}
        tex.update(j.get('textures', {}))
        el = j.get('elements', parent['el'] if parent else [])
        r = {'tex': tex, 'el': el}
        Mdl.cache[name] = r
        return r

def tex_of(m, ref):
    seen = 0
    while ref and ref.startswith('#') and seen < 12:
        ref = m['tex'].get(ref[1:]); seen += 1
    return ref.replace('minecraft:', '') if ref and not ref.startswith('#') else None

PRIO = ['false', 'straight', 'bottom', 'lower', 'none', 'north', 'left', '0', '1', 'x', 'floor', 'normal']
def rank(v): return PRIO.index(v) if v in PRIO else len(PRIO)

class BS:
    cache = {}
    @staticmethod
    def load(n):
        if n in BS.cache: return BS.cache[n]
        p = f'{ASSETS}/blockstates/{n}.json'
        if ':' in n:
            ns, path = n.split(':', 1)
            p = f'{EXTRA}/assets/{ns}/blockstates/{path}.json'
        BS.cache[n] = jl(p) if os.path.exists(p) else None
        return BS.cache[n]

def pick(val, rng):
    if isinstance(val, list):
        tot = sum(v.get('weight', 1) for v in val); r = rng.random() * tot
        for v in val:
            r -= v.get('weight', 1)
            if r <= 0: return v
        return val[0]
    return val

def cond_ok(when, props):
    if 'OR' in when: return any(cond_ok(w, props) for w in when['OR'])
    if 'AND' in when: return all(cond_ok(w, props) for w in when['AND'])
    for k, v in when.items():
        have = props.get(k, 'false')
        if have not in str(v).split('|'): return False
    return True

def models_for(n, props, rng):
    bs = BS.load(n)
    if not bs: return []
    out = []
    if 'variants' in bs:
        best, bp = None, 1e9
        for key, val in bs['variants'].items():
            pen = 0; ok = True
            if key:
                for kv in key.split(','):
                    k, v = kv.split('=')
                    if k in props:
                        if props[k] != v: ok = False; break
                    else: pen += rank(v)
            if ok and pen < bp: best, bp = val, pen
        if best is not None: out.append(pick(best, rng))
    else:
        for part in bs.get('multipart', []):
            if 'when' not in part or cond_ok(part['when'], props):
                out.append(pick(part['apply'], rng))
    return out

# ---------------------------------------------------------------- geometria de modelos
FACES = {
    'down':  [(0, 0, 1), (0, 0, 0), (1, 0, 0), (1, 0, 1)],
    'up':    [(0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)],
    'north': [(1, 1, 0), (1, 0, 0), (0, 0, 0), (0, 1, 0)],
    'south': [(0, 1, 1), (0, 0, 1), (1, 0, 1), (1, 1, 1)],
    'west':  [(0, 1, 0), (0, 0, 0), (0, 0, 1), (0, 1, 1)],
    'east':  [(1, 1, 1), (1, 0, 1), (1, 0, 0), (1, 1, 0)],
}
DIRV = {'down': (0, -1, 0), 'up': (0, 1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}

def rot_axis(p, axis, ang, o):
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    c, s = math.cos(math.radians(ang)), math.sin(math.radians(ang))
    if axis == 'x': y, z = y * c - z * s, y * s + z * c   # +x: la cara de arriba va al norte? se corrige abajo
    elif axis == 'y': x, z = x * c - z * s, x * s + z * c
    else: x, y = x * c - y * s, x * s + y * c
    return (x + o[0], y + o[1], z + o[2])

def mc_rot_x(p, ang):  # x: 90 -> arriba pasa al norte, norte pasa abajo
    y, z = p[1] - 8, p[2] - 8
    for _ in range(int(ang // 90) % 4):
        y, z = z, -y
    return (p[0], y + 8, z + 8)

def mc_rot_y(p, ang):  # y: 90 en sentido horario visto desde arriba: norte -> este
    x, z = p[0] - 8, p[2] - 8
    for _ in range(int(ang // 90) % 4):
        x, z = -z, x
    return (x + 8, p[1], z + 8)

def nearest_dir(n):
    ax = max(range(3), key=lambda i: abs(n[i]))
    v = [0, 0, 0]; v[ax] = 1 if n[ax] >= 0 else -1
    for k, d in DIRV.items():
        if d == tuple(v): return k

def model_quads(var):
    """Devuelve [(vertices[4](0..16), uvs[4], textura, cullface|None, tint, normal_dir)] ya rotados por el estado."""
    m = Mdl.load(var['model'])
    if not m: return []
    qs = []
    rx, ry = var.get('x', 0), var.get('y', 0)
    for el in m['el']:
        f, t = el['from'], el['to']
        for fname, fd in el.get('faces', {}).items():
            tx = tex_of(m, fd.get('texture'))
            if not tx: continue
            if 'uv' in fd: u1, v1, u2, v2 = fd['uv']
            else:
                x1, y1, z1 = f; x2, y2, z2 = t
                u1, v1, u2, v2 = {'down': (x1, 16 - z2, x2, 16 - z1), 'up': (x1, z1, x2, z2), 'north': (16 - x2, 16 - y2, 16 - x1, 16 - y1),
                                  'south': (x1, 16 - y2, x2, 16 - y1), 'west': (z1, 16 - y2, z2, 16 - y1), 'east': (16 - z2, 16 - y2, 16 - z1, 16 - y1)}[fname]
            rot = fd.get('rotation', 0) // 90
            uvs = []
            for k in range(4):
                s = (k + rot) % 4
                uu = u2 if s not in (0, 1) else u1
                vv = v2 if s not in (0, 3) else v1
                uvs.append((uu, vv))
            verts = [(f[0] + (t[0] - f[0]) * a, f[1] + (t[1] - f[1]) * b, f[2] + (t[2] - f[2]) * c) for (a, b, c) in FACES[fname]]
            if 'rotation' in el:
                r = el['rotation']
                verts = [rot_axis(v, r['axis'], r['angle'], r['origin']) for v in verts]
            if rx: verts = [mc_rot_x(v, rx) for v in verts]
            if ry: verts = [mc_rot_y(v, ry) for v in verts]
            cf = fd.get('cullface')
            if cf and (rx or ry):
                d = DIRV[cf]
                v = (d[0], d[1], d[2])
                p = mc_rot_x((8 + v[0] * 8, 8 + v[1] * 8, 8 + v[2] * 8), rx) if rx else (8 + v[0] * 8, 8 + v[1] * 8, 8 + v[2] * 8)
                p = mc_rot_y(p, ry) if ry else p
                cf = nearest_dir((p[0] - 8, p[1] - 8, p[2] - 8))
            a = [verts[1][i] - verts[0][i] for i in range(3)]; b = [verts[2][i] - verts[0][i] for i in range(3)]
            nrm = (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
            if abs(nrm[0]) + abs(nrm[1]) + abs(nrm[2]) < 1e-6: continue
            fdat = fd.get('forge_data', el.get('forge_data', {}))
            em = fdat.get('block_light', 0) > 0
            qs.append((verts, [(u / 16.0, v / 16.0) for u, v in uvs], tx, cf, fd.get('tintindex', -1), nearest_dir(nrm), el.get('shade', True), em))
    return qs

_full = {}
def is_full(var):
    key = (var['model'], var.get('x', 0), var.get('y', 0))
    if key in _full: return _full[key]
    m = Mdl.load(var['model']); ok = False
    if m and len(m['el']) >= 1:
        for el in m['el']:
            if el['from'] == [0, 0, 0] and el['to'] == [16, 16, 16] and len(el.get('faces', {})) == 6 and 'rotation' not in el:
                ok = True
    _full[key] = ok; return ok

# ---------------------------------------------------------------- mundo
def ground(x, y, z):
    if y == -1: return 'grass_block'
    if y < -1: return 'dirt' if y >= -4 else 'stone'
    return 'air'

def raw(x, y, z):
    s = cells.get((x, y, z))
    if s is None: return ground(x, y, z)
    return s

DIRS4 = {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)}
CCW = {'north': 'west', 'west': 'south', 'south': 'east', 'east': 'north'}
def nm(x, y, z): return split_state(raw(x, y, z))

def opaque_full(n, props, x, y, z):
    if n in AIRS or is_water(n) or is_lava(n): return False
    if n.endswith('_stairs') or n.endswith('_slab'): return n.endswith('_slab') and props.get('type') == 'double'
    for g in GLASSY:
        if g in n: return False
    if 'leaves' in n: return False
    bs = BS.load(n)
    if not bs: return False
    v = models_for(n, props, random.Random(0))
    return bool(v) and all(is_full(x_) for x_ in v[:1]) and len(v) == 1

def finish_props(n, props, x, y, z):
    """Propiedades que el juego calcula con los vecinos: escaleras, vallas, muros, barrotes, cristales."""
    p = dict(props)
    if n.endswith('_stairs'):
        facing = p.get('facing', 'north'); half = p.get('half', 'bottom')
        def st(dx, dz):
            a, b = nm(x + dx, y, z + dz)
            return (b if a.endswith('_stairs') and b.get('half', 'bottom') == half else None)
        fx, fz = DIRS4[facing]
        shape = 'straight'
        b = st(fx, fz)
        if b is not None:
            d = b.get('facing', 'north')
            if d in DIRS4 and (d in ('east', 'west')) != (facing in ('east', 'west')):
                ox, oz = DIRS4[d]
                nb = nm(x - ox, y, z - oz)  # canTakeShape: vecino en la direccion opuesta
                same = nb[0].endswith('_stairs') and nb[1].get('facing') == facing and nb[1].get('half', 'bottom') == half
                if not same: shape = 'outer_left' if d == CCW[facing] else 'outer_right'
        if shape == 'straight':
            b = st(-fx, -fz)
            if b is not None:
                d = b.get('facing', 'north')
                if d in DIRS4 and (d in ('east', 'west')) != (facing in ('east', 'west')):
                    ox, oz = DIRS4[d]
                    nb = nm(x + ox, y, z + oz)
                    same = nb[0].endswith('_stairs') and nb[1].get('facing') == facing and nb[1].get('half', 'bottom') == half
                    if not same: shape = 'inner_left' if d == CCW[facing] else 'inner_right'
        p['shape'] = shape
    elif n.endswith('_fence') or n == 'iron_bars' or n.endswith('_glass_pane') or n == 'glass_pane' or n.endswith('_wall') or n == 'chain_x':
        wall = n.endswith('_wall')
        for d, (dx, dz) in DIRS4.items():
            a, b = nm(x + dx, y, z + dz)
            conn = False
            if wall: conn = a.endswith('_wall') or opaque_full(a, b, x + dx, y, z + dz) or a.endswith('_fence_gate')
            elif n.endswith('_fence'): conn = (a.endswith('_fence') and (a.startswith('nether') == n.startswith('nether'))) or a.endswith('_fence_gate') or opaque_full(a, b, x + dx, y, z + dz)
            else: conn = a in ('iron_bars',) or a.endswith('glass_pane') or a.endswith('_wall') or opaque_full(a, b, x + dx, y, z + dz)
            p[d] = ('low' if wall else 'true') if conn else ('none' if wall else 'false')
        if wall:
            cs = [d for d in DIRS4 if p[d] != 'none']
            straight = len(cs) == 2 and ((set(cs) == {'north', 'south'}) or (set(cs) == {'east', 'west'}))
            up = nm(x, y + 1, z)[0] not in AIRS
            p['up'] = 'true' if (not straight or up or len(cs) != 2) else 'false'
    return p

# luces de la decoracion nueva (manifiesto est_decor.json)
_decor_light = None
def decor_light(n):
    global _decor_light
    if _decor_light is None:
        _decor_light = {}
        for mp in __import__('glob').glob(f'{EXTRA}/data/*/est_decor.json'):
            ns = mp.split('/data/')[1].split('/')[0]
            for b in jl(mp)['blocks']:
                _decor_light[f"{ns}:{b['id']}"] = b['light']
    return _decor_light.get(n, 0)

# luces emitidas
def emit(n, p):
    if ':' in n: return decor_light(n)
    if n in ('torch', 'wall_torch', 'end_rod'): return 14
    if n in ('soul_torch', 'soul_wall_torch'): return 10
    if n in ('lantern',): return 15
    if n in ('soul_lantern',): return 10
    if n in ('glowstone', 'sea_lantern', 'shroomlight', 'jack_o_lantern', 'beacon', 'conduit', 'lava', 'fire', 'ochre_froglight', 'verdant_froglight', 'pearlescent_froglight', 'respawn_anchor', 'end_gateway', 'end_portal'): return 15
    if n in ('campfire',) : return 15 if p.get('lit', 'true') == 'true' else 0
    if n in ('soul_campfire',): return 10 if p.get('lit', 'true') == 'true' else 0
    if n == 'soul_fire': return 10
    if n in ('furnace', 'smoker', 'blast_furnace'): return 13 if p.get('lit') == 'true' else 0
    if n == 'redstone_lamp': return 15 if p.get('lit') == 'true' else 0
    if n == 'crying_obsidian': return 10
    if n == 'enchanting_table': return 7
    if n == 'ender_chest': return 7
    if n == 'magma_block': return 3
    if n == 'glow_lichen': return 7
    if n == 'large_amethyst_bud': return 4
    if n == 'medium_amethyst_bud': return 2
    if n == 'small_amethyst_bud': return 1
    if n == 'amethyst_cluster': return 5
    if n.endswith('candle') and p.get('lit') == 'true': return 3 * int(p.get('candles', '1'))
    if n.endswith('candle_cake') and p.get('lit') == 'true': return 3
    if n == 'light': return int(p.get('level', '15'))
    if n == 'sea_pickle' and p.get('waterlogged', 'true') == 'true': return 6
    if n == 'brewing_stand': return 1
    if n == 'sculk_sensor': return 1
    if n == 'cave_vines' and p.get('berries') == 'true': return 14
    if n == 'cave_vines_plant' and p.get('berries') == 'true': return 14
    if n == 'lava_cauldron': return 15
    return 0

# ---------------------------------------------------------------- caja y rejilla de luz
xs = [k[0] for k in cells]; ys = [k[1] for k in cells]; zs = [k[2] for k in cells]
if BOX:
    x0, x1, y0, y1, z0, z1 = BOX
else:
    x0, x1, y0, y1, z0, z1 = min(xs) - 6, max(xs) + 6, min(ys) - 2, max(ys) + 6, min(zs) - 6, max(zs) + 6
# la luz se calcula en una region un poco mayor
M = 12
gx0, gx1, gy0, gy1, gz0, gz1 = x0 - M, x1 + M, y0 - 3, y1 + 6, z0 - M, z1 + M
W, H, D = gx1 - gx0 + 1, gy1 - gy0 + 1, gz1 - gz0 + 1
print('rejilla', W, H, D, W * H * D)
opq = np.zeros((W, H, D), bool)
emi = np.zeros((W, H, D), np.int8)
info = {}
for ix in range(W):
    x = gx0 + ix
    for iz in range(D):
        z = gz0 + iz
        for iy in range(H):
            y = gy0 + iy
            s = raw(x, y, z)
            n, p = split_state(s)
            if n in AIRS and n != 'light':
                continue
            if n in AIRS and n == 'light':
                emi[ix, iy, iz] = int(p.get('level', '15')); continue
            pp = p
            info[(x, y, z)] = (n, pp)
            if opaque_full(n, pp, x, y, z) or n.endswith('_stairs') or n.endswith('_slab'):
                opq[ix, iy, iz] = True
            e = emit(n, pp)
            if e: emi[ix, iy, iz] = e
print('bloques en rejilla', len(info))

def propagate(src, opaque):
    L = src.astype(np.int16).copy()
    for _ in range(15):
        m = L.copy()
        for ax in range(3):
            for sh in (1, -1):
                r = np.roll(L, sh, axis=ax) - 1
                sl = [slice(None)] * 3
                sl[ax] = slice(0, 1) if sh == 1 else slice(-1, None)
                r[tuple(sl)] = 0
                m = np.maximum(m, r)
        m[opaque] = 0
        if (m == L).all(): break
        L = m
    return L

blk = propagate(np.where(opq, 0, emi), opq)
# luz de cielo: columnas libres hasta arriba
sky = np.zeros((W, H, D), np.int16)
for ix in range(W):
    for iz in range(D):
        col = opq[ix, :, iz]
        top = np.nonzero(col)[0]
        t = top.max() + 1 if len(top) else 0
        sky[ix, t:, iz] = 15
sky = propagate(sky, opq)
print('luz lista')

def light_at(x, y, z):
    ix, iy, iz = x - gx0, y - gy0, z - gz0
    if 0 <= ix < W and 0 <= iy < H and 0 <= iz < D:
        return int(blk[ix, iy, iz]), int(sky[ix, iy, iz])
    return 0, 15

def is_opq(x, y, z):
    ix, iy, iz = x - gx0, y - gy0, z - gz0
    if 0 <= ix < W and 0 <= iy < H and 0 <= iz < D: return bool(opq[ix, iy, iz])
    return False

def curve(l):
    f = l / 15.0
    return f / (4 - 3 * f)

def color_of(bl, sk):
    f = curve(bl)
    br, bg, bb = f, f * ((f * 0.6 + 0.4) * 0.6 + 0.4), f * (f * f * 0.6 + 0.4)
    s = curve(sk) * SKY
    sr, sg, sb = s * 0.82, s * 0.88, s * 1.0
    c = [min(1.0, br + sr), min(1.0, bg + sg), min(1.0, bb + sb)]
    # brillo "Predeterminado" del juego (gamma 0.5)
    return [(1 - 0.5) * v + 0.5 * (1 - (1 - v) ** 4) for v in c]

# ---------------------------------------------------------------- texturas
tex_names = {}
def tex_id(name):
    if name not in tex_names: tex_names[name] = len(tex_names)
    return tex_names[name]

TINT = {
    'grass': (0.57, 0.74, 0.35), 'foliage': (0.47, 0.67, 0.18), 'birch': (0.50, 0.65, 0.33), 'spruce': (0.38, 0.60, 0.38),
    'water': (0.25, 0.46, 0.89), 'lily': (0.13, 0.50, 0.19),
}
def tint_for(n):
    if 'water' == n: return TINT['water']
    if n == 'lily_pad': return TINT['lily']
    if 'birch_leaves' in n: return TINT['birch']
    if 'spruce_leaves' in n: return TINT['spruce']
    if 'leaves' in n and 'azalea' not in n: return TINT['foliage']
    if n in ('vine', 'cave_vines', 'cave_vines_plant'): return TINT['foliage']
    if 'grass' in n or 'fern' in n or n in ('sugar_cane', 'bamboo'): return TINT['grass']
    return (1, 1, 1)

# ---------------------------------------------------------------- emitir caras
solid_q, trans_q = [], []   # (4 vertices, 4 uv, tex, 4 colors)
SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'west': 0.6, 'east': 0.6}
PLACE = {'chest': ('oak_planks', 14, 14, 14), 'trapped_chest': ('oak_planks', 14, 14, 14), 'ender_chest': ('obsidian', 14, 14, 14)}
COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']

def placeholder(n, p):
    for c in COLORS:
        if n == f'{c}_bed': return [{'box': (c + '_wool', (0, 0, 0), (16, 9, 16))}]
        if n in (f'{c}_banner', f'{c}_wall_banner'):
            if 'wall' in n: return [{'box': (c + '_wool', (1, 2, 14.5), (15, 16, 15.5)), 'face': p.get('facing', 'north')}]
            return [{'box': (c + '_wool', (7.5, 0, 7.5), (8.5, 16, 8.5))}, {'box': (c + '_wool', (1, 5, 7.5), (15, 16, 8.5))}]
        if n == f'{c}_shulker_box': return [{'box': (c + '_concrete', (1, 0, 1), (15, 15, 15))}]
        if n == f'{c}_carpet': pass
    if n in PLACE:
        t, a, b, c = PLACE[n]; return [{'box': (t, (1, 0, 1), (15, 14, 15))}]
    if n.endswith('_sign') or n.endswith('_wall_sign') or n.endswith('hanging_sign'):
        wood = n.replace('_wall', '').replace('_hanging', '').replace('_sign', '')
        return [{'box': (wood + '_planks', (2, 7, 14), (14, 15, 15)), 'face': p.get('facing', 'north')}] if 'wall' in n else [{'box': (wood + '_planks', (2, 8, 7), (14, 16, 9))}, {'box': (wood + '_planks', (7.5, 0, 7.5), (8.5, 8, 8.5))}]
    if n.endswith('_head') or n.endswith('_skull'): return [{'box': ('soul_sand', (4, 0, 4), (12, 8, 12))}]
    if n == 'decorated_pot': return [{'box': ('terracotta', (1, 0, 1), (15, 16, 15))}]
    if n == 'enchanting_table': return None
    return None

def box_var_quads(spec):
    t, f, e = spec['box']
    qs = []
    for fname in FACES:
        verts = [(f[0] + (e[0] - f[0]) * a, f[1] + (e[1] - f[1]) * b, f[2] + (e[2] - f[2]) * c) for (a, b, c) in FACES[fname]]
        qs.append((verts, [(0, 0), (0, 1), (1, 1), (1, 0)], 'block/' + t, None, -1, fname, True, False))
    return qs

def face_light(x, y, z, nrm, verts_w):
    """Luz por vertice (suave, con oclusion ambiental) para una cara alineada al cubo; si no, luz de la casilla."""
    return None

def emit_cell(x, y, z, n, p):
    rng = random.Random(x * 73856093 ^ y * 19349663 ^ z * 83492791)
    p = finish_props(n, p, x, y, z)
    if is_water(n) or is_lava(n):
        top_free = nm(x, y + 1, z)[0] != n
        faces = []
        for d, v in DIRV.items():
            a, b = nm(x + v[0], y + v[1], z + v[2])
            if a == n: continue
            if opaque_full(a, b, x + v[0], y + v[1], z + v[2]): continue
            if d == 'up': hgt = 14 if top_free else 16
            else: hgt = 16
            verts = [(vv[0] * 16, vv[1] * (hgt if True else 16), vv[2] * 16) for vv in FACES[d]]
            faces.append((verts, [(0, 0), (0, 1), (1, 1), (1, 0)], 'block/water_still' if is_water(n) else 'block/lava_still', None, 0 if is_water(n) else -1, d, True, False))
        return faces, True, is_lava(n)
    vs = models_for(n, p, rng)
    qs = []
    for var in vs:
        qs.extend(model_quads(var))
    if not qs:
        ph = placeholder(n, p)
        if ph:
            for sp in ph: qs.extend(box_var_quads(sp))
    return qs, False, False

EMISSIVE_QUAD = {'lava_still', 'glowstone', 'sea_lantern', 'shroomlight', 'magma'}
count = 0
cand = set()
for (x, y, z), (n, p) in info.items():
    if x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1: cand.add((x, y, z))

import time
t0 = time.time()
for (x, y, z) in cand:
    n, p = info[(x, y, z)]
    qs, fluid, hot = emit_cell(x, y, z, n, p)
    tint = tint_for(n)
    for (verts, uvs, tx, cf, ti, ndir, shade, qemit) in qs:
        if cf:
            v = DIRV[cf]
            a, b = nm(x + v[0], y + v[1], z + v[2])
            if opaque_full(a, b, x + v[0], y + v[1], z + v[2]) and not fluid: continue
            if a == n and ('glass' in n or 'ice' in n or fluid): continue
            if fluid and a == n: continue
        # luz: vertice a vertice sobre caras de cubo; si no, la casilla
        wv = [(x + vv[0] / 16.0, y + vv[1] / 16.0, z + vv[2] / 16.0) for vv in verts]
        nv = DIRV[ndir]
        cols = []
        aligned = all(abs((vv[0] - verts[0][0]) * nv[0] + (vv[1] - verts[0][1]) * nv[1] + (vv[2] - verts[0][2]) * nv[2]) < 1e-3 for vv in verts)
        onboundary = aligned and (abs(verts[0][0] * abs(nv[0]) + verts[0][1] * abs(nv[1]) + verts[0][2] * abs(nv[2]) - (16 if (nv[0] + nv[1] + nv[2]) > 0 else 0)) < 1e-3)
        sh = SHADE[ndir] if shade else 1.0
        if n in ('lava', 'torch', 'wall_torch', 'soul_torch', 'soul_wall_torch', 'lantern', 'soul_lantern', 'glowstone', 'sea_lantern', 'shroomlight', 'jack_o_lantern', 'campfire', 'soul_campfire', 'end_rod', 'fire', 'soul_fire', 'ochre_froglight', 'verdant_froglight', 'pearlescent_froglight', 'magma_block') or (n.endswith('candle') and p.get('lit') == 'true') or (n in ('furnace', 'smoker', 'blast_furnace') and p.get('lit') == 'true' and tx.endswith('_on')):
            sh = 1.0; emis = True
        else: emis = False
        if qemit: sh = 1.0; emis = True
        for k in range(4):
            vx, vy, vz = verts[k]
            if onboundary:
                # tangentes
                ax = max(range(3), key=lambda i: abs(nv[i]))
                tdirs = [i for i in range(3) if i != ax]
                sx = 1 if vx > 8 else -1; sy = 1 if vy > 8 else -1; sz = 1 if vz > 8 else -1
                sgn = [sx, sy, sz]
                base = [x + nv[0], y + nv[1], z + nv[2]]
                def off(da, db):
                    q = list(base)
                    q[tdirs[0]] += da; q[tdirs[1]] += db
                    return tuple(q)
                s1 = off(sgn[tdirs[0]], 0); s2 = off(0, sgn[tdirs[1]]); cr = off(sgn[tdirs[0]], sgn[tdirs[1]])
                o1, o2, oc = is_opq(*s1), is_opq(*s2), is_opq(*cr)
                cl = light_at(*base)
                l1 = cl if o1 else light_at(*s1)
                l2 = cl if o2 else light_at(*s2)
                lc = cl if (oc or (o1 and o2)) else light_at(*cr)
                bl = (cl[0] + l1[0] + l2[0] + lc[0]) / 4.0
                sk = (cl[1] + l1[1] + l2[1] + lc[1]) / 4.0
                if o1 and o2: aoc = (1.0 + 0.2 + 0.2 + 0.2) / 4.0
                else: aoc = (1.0 + (0.2 if o1 else 1.0) + (0.2 if o2 else 1.0) + (0.2 if oc else 1.0)) / 4.0
            else:
                bl, sk = light_at(x, y, z)
                # a media casilla si es opaca (cara interior): usa la vecina en la normal
                if is_opq(x, y, z): bl, sk = light_at(x + nv[0], y + nv[1], z + nv[2])
                aoc = 1.0
            if emis: bl = 15 if qemit else max(bl, 13)
            c = color_of(bl, sk)
            m = sh * aoc
            tt = tint if ti >= 0 else (1, 1, 1)
            cols.append((min(1, c[0] * m * tt[0]), min(1, c[1] * m * tt[1]), min(1, c[2] * m * tt[2])))
        quad = (wv, [(u, v) for u, v in uvs], tex_id(tx), cols)
        (trans_q if (fluid or 'glass' in n or 'ice' in n or 'stained' in n) else solid_q).append(quad)
        count += 1
print('caras', count, 'tiempo', round(time.time() - t0, 1))

# ---------------------------------------------------------------- atlas
names = sorted(tex_names, key=lambda k: tex_names[k])
side = 1
while side * side < len(names): side *= 2
TS = 16
for name in tex_names:
    pth = f'{ASSETS}/textures/{name}.png' if ':' not in name else f"{EXTRA}/assets/{name.split(':',1)[0]}/textures/{name.split(':',1)[1]}.png"
    if os.path.exists(pth):
        im0 = Image.open(pth)
        TS = max(TS, min(im0.width, 64))
TS = max(16, TS)
atlas = Image.new('RGBA', (side * TS, side * TS), (255, 0, 255, 255))
alpha_kind = {}
for name, idx in tex_names.items():
    p = f'{ASSETS}/textures/{name}.png' if ':' not in name else f"{EXTRA}/assets/{name.split(':',1)[0]}/textures/{name.split(':',1)[1]}.png"
    if not os.path.exists(p):
        print('FALTA textura', name)
        continue
    im = Image.open(p).convert('RGBA')
    if im.height > im.width: im = im.crop((0, 0, im.width, im.width))
    if im.width != TS: im = im.resize((TS, TS), Image.NEAREST)
    a = np.array(im)[:, :, 3]
    alpha_kind[idx] = 2 if ((a > 0) & (a < 255)).any() else (1 if (a == 0).any() else 0)
    atlas.paste(im, ((idx % side) * TS, (idx // side) * TS))
atlas.save(f'{OUT}/atlas.png')

def pack(qs, name):
    n = len(qs)
    pos = np.zeros((n, 4, 3), np.float32); uv = np.zeros((n, 4, 2), np.float32); col = np.zeros((n, 4, 3), np.float32)
    for i, (wv, uvs, tid, cols) in enumerate(qs):
        tx, ty = (tid % side), (tid // side)
        for k in range(4):
            pos[i, k] = wv[k]
            u, v = uvs[k]
            uv[i, k] = ((tx + min(max(u, 0), 1)) / side, 1.0 - (ty + min(max(v, 0), 1)) / side)
            col[i, k] = cols[k]
    pos.tofile(f'{OUT}/{name}_pos.bin'); uv.tofile(f'{OUT}/{name}_uv.bin'); col.tofile(f'{OUT}/{name}_col.bin')
    return n
meta = {'solid': pack(solid_q, 'solid'), 'trans': pack(trans_q, 'trans'), 'side': side,
        'bbox': [x0, x1, y0, y1, z0, z1]}
json.dump(meta, open(f'{OUT}/meta.json', 'w'))
# categoria de alfa por quad (para recorte): se guarda por quad en un byte
def kinds(qs):
    return np.array([alpha_kind.get(q[2], 0) for q in qs], np.uint8)
kinds(solid_q).tofile(f'{OUT}/solid_kind.bin'); kinds(trans_q).tofile(f'{OUT}/trans_kind.bin')
print('listo', meta)
