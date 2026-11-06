#!/usr/bin/env python3
"""Kit para fabricar decoracion 3D de Minecraft 1.20.1 (modelo JSON + textura PNG) a partir de cajas pintadas por codigo.

Cada decoracion (Prop) es una lista de cajas en coordenadas de bloque 0..16. Las texturas se pintan solas (madera, metal, tela,
piedra, cristal, llamas...) y se empaquetan en UNA imagen por decoracion; los UV se calculan automaticamente. Las partes que brillan
(llamas, cristales, pociones) llevan forge_data con luz 15 para verse encendidas incluso en la oscuridad.
"""
import json
import math
import os

import numpy as np
from PIL import Image

NS = 'mundos_de_axelis'
PREFIX = 'est_'


# ---------------------------------------------------------------- ruido
def _hash(ix, iy, iz, seed):
    n = (ix.astype(np.int64) * 73856093) ^ (iy.astype(np.int64) * 19349663) ^ (iz.astype(np.int64) * 83492791) ^ (seed * 2654435761 & 0xFFFFFFFF)
    n = n & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n = (n ^ (n >> 16)) & 0xFFFFFFFF
    return (n & 0xFFFF) / 65535.0


def noise3(X, Y, Z, scale=1.0, seed=0):
    x, y, z = X / scale, Y / scale, Z / scale
    ix, iy, iz = np.floor(x), np.floor(y), np.floor(z)
    fx, fy, fz = x - ix, y - iy, z - iz
    fx, fy, fz = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy), fz * fz * (3 - 2 * fz)
    out = 0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (fx if dx else 1 - fx) * (fy if dy else 1 - fy) * (fz if dz else 1 - fz)
                out = out + w * _hash(ix + dx, iy + dy, iz + dz, seed)
    return out


def fbm(X, Y, Z, scale=4.0, seed=0, octaves=3):
    t, a, tot = 0, 1.0, 0
    for o in range(octaves):
        t = t + a * noise3(X, Y, Z, scale / (2 ** o), seed + o * 17)
        tot += a
        a *= 0.5
    return t / tot


def rnd(X, Y, Z, seed=0):
    """Ruido blanco por texel (cada pixel distinto)."""
    return _hash(np.floor(X * 16 + 0.5), np.floor(Y * 16 + 0.5), np.floor(Z * 16 + 0.5), seed + 991)


def hexc(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


class Ramp:
    """Paleta de tonos (de oscuro a claro); da colores con pasos duros como en el pixel art de Minecraft."""

    def __init__(self, *cols):
        self.c = np.array([hexc(c) if isinstance(c, str) else c for c in cols], np.float32)

    def __call__(self, t):
        t = np.clip(t, 0, 0.9999)
        idx = (t * len(self.c)).astype(int)
        return self.c[idx]

    def smooth(self, t):
        t = np.clip(t, 0, 1) * (len(self.c) - 1)
        i = np.minimum(t.astype(int), len(self.c) - 2)
        f = (t - i)[..., None]
        return self.c[i] * (1 - f) + self.c[i + 1] * f


# ---------------------------------------------------------------- materiales
# Un material es una funcion f(P) -> RGBA (alto, ancho, 4) en 0..255. P trae X,Y,Z (pixeles 0..16 del bloque, local a la caja sin rotar),
# U,V (texel dentro de la cara), W,H (tamano de la cara), face, seed.
def solid(color, alpha=255):
    c = np.array(hexc(color) if isinstance(color, str) else color, np.float32)

    def f(P):
        out = np.zeros(P['X'].shape + (4,), np.float32)
        out[..., :3] = c
        out[..., 3] = alpha
        return out
    f.flat = True
    return f


def with_alpha(rgb, a=255):
    out = np.zeros(rgb.shape[:-1] + (4,), np.float32)
    out[..., :3] = rgb
    out[..., 3] = a
    return out


def wood(ramp, axis='h', grain=2.2, seams=0, seed=1):
    """Madera: vetas largas (axis 'h' = horizontales, 'v' = verticales) y juntas opcionales cada `seams` pixeles."""
    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        if axis == 'h':
            t = fbm(X * 0.35 + Z * 0.35, Y * grain, X * 0.0 + Z * 0.0, 2.5, P['seed'] + seed, 3)
        else:
            t = fbm(X * grain, Y * 0.3, Z * grain, 2.5, P['seed'] + seed, 3)
        t = t + (rnd(X, Y, Z, seed) - 0.5) * 0.12
        rgb = ramp(0.15 + t * 0.7)
        if seams:
            coord = np.floor(Y if axis == 'h' else (X + Z))
            line = (coord % seams) == 0
            rgb = np.where(line[..., None], rgb * 0.7, rgb)
        return with_alpha(rgb)
    return f


def metal(ramp, streak=0.5, seed=3, sparkle=0.05):
    """Metal cepillado con rayas horizontales y algun brillo suelto."""
    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        t = fbm(X * 0.2 + Z * 0.2, Y * 1.6, 0 * X, 2.0, P['seed'] + seed, 2) * streak + (1 - streak) * 0.5
        t = t + (rnd(X, Y, Z, seed) - 0.5) * 0.18
        rgb = ramp(t)
        sp = rnd(X, Y, Z, seed + 5) > (1 - sparkle)
        rgb = np.where(sp[..., None], np.minimum(rgb * 1.25 + 18, 255), rgb)
        return with_alpha(rgb)
    return f


def stone(ramp, scale=3.0, seed=7, cracks=0.0):
    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        t = fbm(X, Y, Z, scale, P['seed'] + seed, 3) + (rnd(X, Y, Z, seed) - 0.5) * 0.18
        rgb = ramp(t)
        if cracks:
            c = np.abs(fbm(X, Y, Z, 2.2, P['seed'] + seed + 40, 2) - 0.5) < 0.025 * cracks
            rgb = np.where(c[..., None], rgb * 0.55, rgb)
        return with_alpha(rgb)
    return f


def cloth(base, weave=0.1, seed=5, fold=0.0):
    c = np.array(hexc(base) if isinstance(base, str) else base, np.float32)

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        chk = ((np.floor(X * 1.0 + Z * 1.0) + np.floor(Y * 1.0)) % 2) * 2 - 1
        n = (rnd(X, Y, Z, seed) - 0.5) * 0.12
        v = 1 + chk * weave * 0.5 + n
        if fold:
            v = v + np.sin((X + Z) * 1.1) * fold
        rgb = np.clip(c * v[..., None], 0, 255)
        return with_alpha(rgb)
    return f


def parchment(ink=0.0, seed=9):
    r = Ramp('#b9a272', '#cdb888', '#dcc89c', '#e8d8ae', '#f2e6c4')

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        t = fbm(X, Y, Z, 2.5, P['seed'] + seed, 2) * 0.8 + (rnd(X, Y, Z, seed) - 0.5) * 0.22
        rgb = r(t)
        if ink:
            line = (np.floor(Y) % 2 == 0) & (rnd(X, Y, Z, seed + 3) < ink)
            rgb = np.where(line[..., None], np.array([70, 48, 30], np.float32), rgb)
        return with_alpha(rgb)
    return f


def gem(color, seed=11, bright=1.0):
    """Gema o cristal: facetas claras y oscuras alrededor del color base."""
    c = np.array(hexc(color) if isinstance(color, str) else color, np.float32)

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        t = noise3(X, Y, Z, 2.0, P['seed'] + seed)
        facet = np.floor(t * 4) / 4.0
        v = (0.62 + facet * 0.9) * bright
        rgb = np.clip(c * v[..., None], 0, 255)
        hl = rnd(X, Y, Z, seed) > 0.93
        rgb = np.where(hl[..., None], np.minimum(rgb * 1.3 + 45, 255), rgb)
        return with_alpha(rgb)
    f.emit = True
    return f


def glow(ramp, scale=2.5, seed=13):
    """Superficie luminosa con remolinos suaves (pociones, energia, brasas)."""
    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        t = fbm(X, Y, Z, scale, P['seed'] + seed, 3) * 0.85 + (rnd(X, Y, Z, seed) - 0.5) * 0.15
        return with_alpha(ramp(t))
    f.emit = True
    return f


def flame(core='#fff6c0', mid='#ffcf4a', edge='#ff8a1c', tip='#e8471a'):
    """Llama plana (para un plano de ancho W x alto H pixeles): gota con alfa, brillante al centro."""
    cc, cm, ce, ct = (np.array(hexc(v), np.float32) for v in (core, mid, edge, tip))

    def f(P):
        U, V, W, H = P['U'].astype(np.float32), P['V'].astype(np.float32), float(P['W']), float(P['H'])
        out = np.zeros(U.shape + (4,), np.float32)
        yy = (V + 0.5) / H            # 0 arriba, 1 abajo
        xx = ((U + 0.5) / W - 0.5) * 2  # -1..1
        half = np.sin(np.clip(yy, 0, 1) * math.pi * 0.5) ** 0.8 * 0.95 * (1 - 0.18 * (1 - yy))
        half = np.where(yy < 0.12, 0.0, half)
        inside = np.abs(xx) <= half + 0.0
        d = np.abs(xx) / np.maximum(half, 1e-3)
        col = np.where((d < 0.35)[..., None] & (yy > 0.45)[..., None], cc, np.where((d < 0.7)[..., None], cm, ce))
        col = np.where((yy < 0.28)[..., None], ct * 0.5 + ce * 0.5, col)
        out[..., :3] = col
        out[..., 3] = np.where(inside, 255, 0)
        return out
    f.emit = True
    f.cutout = True
    return f


def image_mat(img, flip=False):
    """Material que toma los pixeles de una imagen RGBA (PIL) segun U,V de la cara (para tapices, mapas, etc.)."""
    arr = np.array(img.convert('RGBA'), np.float32)

    def f(P):
        U = np.clip(P['U'] + P.get('u0', 0), 0, arr.shape[1] - 1)
        V = np.clip(P['V'] + P.get('v0', 0), 0, arr.shape[0] - 1)
        return arr[V, U].copy()
    f.cutout = bool((arr[..., 3] < 255).any())
    return f


# ---------------------------------------------------------------- caja
FACES = ['north', 'south', 'west', 'east', 'up', 'down']


class Box:
    def __init__(self, f, t, mat, emit=False, rot=None, skip=(), bevel=0.0, shade=True, name=None, uvmat=None):
        self.f = [float(v) for v in f]
        self.t = [float(v) for v in t]
        self.mat = mat
        self.emit = emit or getattr(mat, 'emit', False)
        self.rot = rot
        self.skip = set(skip)
        self.bevel = bevel
        self.shade = shade
        self.rects = {}
        self.name = name

    def dims(self):
        return [self.t[i] - self.f[i] for i in range(3)]

    def face_size(self, face):
        dx, dy, dz = self.dims()
        if face in ('north', 'south'):
            return dx, dy
        if face in ('west', 'east'):
            return dz, dy
        return dx, dz


class Prop:
    def __init__(self, pid, es, en, kind='floor', light=0, shape=None, collide=True, particles=None, tall=False, desc_es='', desc_en='',
                 render='cutout', seed=1, sound='wood', shape_upper=None):
        """kind: floor (de pie), wall (en la pared), hang (cuelga del techo), plain (sin orientacion). tall = dos bloques de alto."""
        self.id = pid
        self.es, self.en = es, en
        self.kind = kind
        self.light = light
        self.shape = shape          # lista de cajas [x0,y0,z0,x1,y1,z1] (16 = bloque); si falta se calcula de las cajas
        self.collide = collide
        self.particles = particles or []
        self.tall = tall
        self.render = render
        self.seed = seed
        self.sound = sound
        self.shape_upper = shape_upper
        self.boxes = []             # (parte, Box); parte = 'lower' | 'upper' | None
        self.part = None

    def box(self, x0, y0, z0, x1, y1, z1, mat, **kw):
        b = Box((x0, y0, z0), (x1, y1, z1), mat, **kw)
        self.boxes.append((self.part, b))
        return b

    def at(self, part):
        self.part = part
        return self


def _pack(rects, size_hint=None):
    """Empaquetado por estantes de rectangulos (id, w, h); devuelve (tamano, {id: (x, y)})."""
    for size in (16, 32, 48, 64, 96, 128, 192, 256):
        pos, x, y, rowh, ok = {}, 0, 0, 0, True
        for rid, w, h in sorted(rects, key=lambda r: (-r[2], -r[1])):
            if x + w > size:
                x, y, rowh = 0, y + rowh, 0
            if y + h > size:
                ok = False
                break
            pos[rid] = (x, y)
            x += w
            rowh = max(rowh, h)
        if ok:
            return size, pos
    raise RuntimeError('no cabe en 256x256')


def _tex_xyz(b, face, u, v):
    """Coordenadas locales (pixeles de bloque) del centro del texel (u,v) de una cara, mirando la cara desde fuera."""
    x0, y0, z0 = b.f
    x1, y1, z1 = b.t
    uu = np.minimum(u + 0.5, 1e9)
    vv = v + 0.5
    if face == 'north':
        return np.minimum(x1 - uu, x1).clip(x0, x1), (y1 - vv).clip(y0, y1), np.full(u.shape, z0)
    if face == 'south':
        return (x0 + uu).clip(x0, x1), (y1 - vv).clip(y0, y1), np.full(u.shape, z1)
    if face == 'west':
        return np.full(u.shape, x0), (y1 - vv).clip(y0, y1), (z0 + uu).clip(z0, z1)
    if face == 'east':
        return np.full(u.shape, x1), (y1 - vv).clip(y0, y1), (z1 - uu).clip(z0, z1)
    if face == 'up':
        return (x0 + uu).clip(x0, x1), np.full(u.shape, y1), (z0 + vv).clip(z0, z1)
    return (x0 + uu).clip(x0, x1), np.full(u.shape, y0), (z1 - vv).clip(z0, z1)


def paint_face(b, face, w, h, seed):
    U, V = np.meshgrid(np.arange(w), np.arange(h))
    X, Y, Z = _tex_xyz(b, face, U.astype(np.float32), V.astype(np.float32))
    P = {'X': X, 'Y': Y, 'Z': Z, 'U': U, 'V': V, 'W': w, 'H': h, 'face': face, 'seed': seed}
    img = b.mat(P).astype(np.float32)
    if b.bevel and w >= 3 and h >= 3:
        k = b.bevel
        edge = np.zeros((h, w), np.float32)
        edge[0, :] += 1
        edge[:, 0] += 1
        edge[-1, :] -= 1
        edge[:, -1] -= 1
        img[..., :3] = np.clip(img[..., :3] * (1 + edge[..., None] * k), 0, 255)
    return img


def build(prop):
    """Devuelve (modelos {parte: json}, imagen PIL, hay_alfa)."""
    models = {}
    rects = []
    parts = {}
    for part, b in prop.boxes:
        parts.setdefault(part, []).append(b)
    all_boxes = [b for _, b in prop.boxes]
    for i, b in enumerate(all_boxes):
        for face in FACES:
            if face in b.skip:
                continue
            fw, fh = b.face_size(face)
            # una cara plana (grosor 0) solo se dibuja por delante y por detras
            dx, dy, dz = b.dims()
            if (dx == 0 and face in ('up', 'down', 'north', 'south')) or (dy == 0 and face not in ('up', 'down')) or (dz == 0 and face in ('west', 'east', 'up', 'down')):
                continue
            if fw <= 0 or fh <= 0:
                continue
            w, h = max(1, math.ceil(fw - 1e-6)), max(1, math.ceil(fh - 1e-6))
            rects.append(((i, face), w, h))
    size, pos = _pack(rects)
    img = np.zeros((size, size, 4), np.float32)
    has_alpha = False
    for i, b in enumerate(all_boxes):
        for face in FACES:
            key = (i, face)
            if key not in pos:
                continue
            fw, fh = b.face_size(face)
            w, h = max(1, math.ceil(fw - 1e-6)), max(1, math.ceil(fh - 1e-6))
            tile = paint_face(b, face, w, h, prop.seed * 31 + i)
            x, y = pos[key]
            img[y:y + h, x:x + w] = tile
            if (tile[..., 3] < 255).any():
                has_alpha = True
            b.rects[face] = (x, y, fw, fh)
    S = float(size)
    for part, bl in parts.items():
        els = []
        for b in bl:
            el = {'from': [round(v, 4) for v in b.f], 'to': [round(v, 4) for v in b.t], 'faces': {}}
            if b.name:
                el['name'] = b.name
            if b.rot:
                el['rotation'] = {'angle': b.rot[1], 'axis': b.rot[0], 'origin': list(b.rot[2])}
            if not b.shade or b.emit:
                el['shade'] = False
            if b.emit:
                el['forge_data'] = {'block_light': 15, 'sky_light': 15, 'ambient_occlusion': False}
            for face, (x, y, fw, fh) in b.rects.items():
                el['faces'][face] = {'uv': [round(x / S * 16, 4), round(y / S * 16, 4), round((x + fw) / S * 16, 4), round((y + fh) / S * 16, 4)],
                                     'texture': '#0'}
            els.append(el)
        models[part] = els
    return models, Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA'), has_alpha


DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.62, 0.62, 0.62]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.4, 0.4, 0.4]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.6, 0.6, 0.6]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
}


def write_prop(prop, root):
    """Escribe modelo(s), estado de bloque, modelo de objeto, textura, tabla de botin. Devuelve la ficha para el manifiesto."""
    models, img, has_alpha = build(prop)
    name = PREFIX + prop.id
    assets = os.path.join(root, 'assets', NS)
    for sub in ('blockstates', 'models/block', 'models/item', 'textures/block'):
        os.makedirs(os.path.join(assets, sub), exist_ok=True)
    img.save(os.path.join(assets, 'textures/block', name + '.png'))
    render = prop.render
    if render == 'cutout' and not has_alpha:
        render = 'solid'

    def model_json(els, ptex=True):
        j = {'credit': 'Mundos de Axelis - decoracion de estructuras', 'textures': {'0': f'{NS}:block/{name}', 'particle': f'{NS}:block/{name}'},
             'elements': els, 'display': DISPLAY}
        if render != 'solid':
            j['render_type'] = 'minecraft:' + render
        return j

    mnames = {}
    if prop.tall:
        for part in ('lower', 'upper'):
            mn = f'{name}_{part}'
            json.dump(model_json(models[part]), open(os.path.join(assets, 'models/block', mn + '.json'), 'w'), indent=1)
            mnames[part] = f'{NS}:block/{mn}'
    else:
        els = models.get(None, [])
        json.dump(model_json(els), open(os.path.join(assets, 'models/block', name + '.json'), 'w'), indent=1)
        mnames[None] = f'{NS}:block/{name}'
    # estado de bloque
    variants = {}
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    if prop.kind == 'plain' and not prop.tall:
        variants[''] = {'model': mnames[None]}
    elif prop.tall:
        for part in ('lower', 'upper'):
            for f, y in rot.items():
                v = {'model': mnames[part]}
                if y:
                    v['y'] = y
                variants[f'facing={f},half={part}'] = v
    else:
        for f, y in rot.items():
            v = {'model': mnames[None]}
            if y:
                v['y'] = y
            variants[f'facing={f}'] = v
    json.dump({'variants': variants}, open(os.path.join(assets, 'blockstates', name + '.json'), 'w'), indent=1)
    json.dump({'parent': mnames['lower'] if prop.tall else mnames[None]}, open(os.path.join(assets, 'models/item', name + '.json'), 'w'), indent=1)
    # botin: se suelta a si mismo (la mitad de arriba de un bloque alto no suelta nada)
    ld = os.path.join(root, 'data', NS, 'loot_tables/blocks')
    os.makedirs(ld, exist_ok=True)
    pools = [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{name}'}],
              'conditions': [{'condition': 'minecraft:survives_explosion'}]}]
    if prop.tall:
        pools[0]['conditions'] = [{'condition': 'minecraft:block_state_property', 'block': f'{NS}:{name}', 'properties': {'half': 'lower'}},
                                  {'condition': 'minecraft:survives_explosion'}]
    json.dump({'type': 'minecraft:block', 'pools': pools}, open(os.path.join(ld, name + '.json'), 'w'), indent=1)
    # forma de colision / contorno
    if prop.shape is not None:
        shape = [list(s) for s in prop.shape]
    else:
        shape = []
        for part, b in prop.boxes:
            if part in (None, 'lower') and not b.emit and not b.rot:
                shape.append(list(b.f) + list(b.t))
    out = {'id': name, 'kind': prop.kind, 'tall': prop.tall, 'light': prop.light, 'collide': prop.collide, 'sound': prop.sound, 'shape': shape,
           'particles': prop.particles, 'es': prop.es, 'en': prop.en}
    if prop.tall:
        if prop.shape_upper is not None:
            out['shape_upper'] = [list(s) for s in prop.shape_upper]
        else:
            out['shape_upper'] = [list(b.f) + list(b.t) for part, b in prop.boxes if part == 'upper' and not b.emit and not b.rot]
    return out
