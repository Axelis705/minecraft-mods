#!/usr/bin/env python3
"""Fabrica los bloques de decoración navideña (texturas, modelos, estados de bloque, objetos, botín, nombres y el manifiesto
data/navidad/deco.json que lee el mod para registrarlos).

Uso: python3 herramientas/deco/build.py
Los adornos 3D se arman con cajas pintadas por código (kit.py, el mismo del Coleccionista); los bloques enteros, con texturas de 32x32.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
import kit  # noqa: E402
from kit import Prop, Ramp, solid, cloth, wood, metal, stone, gem, glow, flame, with_alpha, fbm, rnd, hexc  # noqa: E402

NS = 'navidad'
ROOT = os.path.abspath(os.path.join(AQUI, '..', '..', 'src', 'main', 'resources'))
A = os.path.join(ROOT, 'assets', NS)
for sub in ('blockstates', 'models/block', 'models/item', 'textures/block'):
    os.makedirs(os.path.join(A, sub), exist_ok=True)

ficha = []
nombres = {}


def dump(rel, data):
    path = os.path.join(A, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=1, ensure_ascii=False)


def tex(name, image):
    image.save(os.path.join(A, 'textures/block', name + '.png'))
    return f'{NS}:block/{name}'


def nombre(bid, es, en):
    nombres[bid] = (es, en)


def botin(bid):
    d = os.path.join(ROOT, 'data', NS, 'loot_tables', 'blocks')
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, bid + '.json'), 'w') as f:
        json.dump({'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{bid}'}],
                                                          'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}, f, indent=1)


# ---------------------------------------------------------------- texturas de 32x32

def grid(n=32):
    y, x = np.mgrid[0:n, 0:n].astype(np.float32)
    return x, y


def img(rgb, a=None):
    out = np.zeros(rgb.shape[:2] + (4,), np.uint8)
    out[..., :3] = np.clip(rgb, 0, 255)
    out[..., 3] = 255 if a is None else np.clip(a, 0, 255)
    return Image.fromarray(out, 'RGBA')


def col(h):
    return np.array(hexc(h), np.float32)


def ruido(x, y, s, seed, oct=2):
    return fbm(x, y, x * 0 + seed * 1.7, s, seed, oct)


def t_caramelo(seed, a='#d8202c', b='#f4efe8'):
    x, y = grid()
    banda = ((x + y) // 6) % 2 == 0
    rgb = np.where(banda[..., None], col(a), col(b))
    brillo = 0.9 + 0.12 * np.sin((x - y) / 5.0) + (rnd(x, y, x * 0, seed) - 0.5) * 0.08
    rgb = rgb * brillo[..., None]
    rgb = np.where(((x + 3) % 16 < 2)[..., None], rgb * 1.12 + 10, rgb)
    return img(rgb)


def t_jengibre(seed):
    x, y = grid()
    t = ruido(x, y, 3.0, seed, 3)
    rgb = Ramp('#7a3f17', '#8f4c1d', '#a35a24', '#b4692c')(t * 0.9 + 0.05)
    hoyo = rnd(x, y, x * 0, seed + 7) > 0.965
    rgb = np.where(hoyo[..., None], rgb * 0.6, rgb)
    return img(rgb)


def t_jengibre_ladrillo(seed):
    x, y = grid()
    t = ruido(x, y, 3.0, seed, 3)
    rgb = Ramp('#7a3f17', '#8f4c1d', '#a35a24', '#b4692c')(t * 0.9 + 0.05)
    fila = (y // 8).astype(int)
    junta_h = (y % 8) < 1.5
    junta_v = ((x + (fila % 2) * 8) % 16) < 1.5
    glas = junta_h | junta_v
    rgb = np.where(glas[..., None], col('#fbf6ee') * (0.94 + 0.06 * rnd(x, y, x * 0, seed)[..., None]), rgb)
    return img(rgb)


def t_glaseado(seed, chispas=False):
    x, y = grid()
    t = ruido(x, y, 4.0, seed, 2)
    rgb = Ramp('#e9e4dc', '#f2eee7', '#faf7f2', '#ffffff')(t)
    if chispas:
        r = rnd(x, y, x * 0, seed + 3)
        cs = [col('#e03a3a'), col('#2e9e4e'), col('#f2c230'), col('#3a7ae0'), col('#e85ab8')]
        for i, c in enumerate(cs):
            m = (r > 0.955 - i * 0.0) & ((np.floor(x * 7 + y * 13 + seed) % 5) == i) & (r > 0.94)
            rgb = np.where(m[..., None], c, rgb)
    return img(rgb)


def t_nieve(seed):
    x, y = grid()
    t = ruido(x, y, 4.0, seed, 3)
    rgb = Ramp('#d4e2ee', '#e2ecf4', '#eef4fa', '#f8fbff')(t)
    brillo = rnd(x, y, x * 0, seed + 9) > 0.975
    rgb = np.where(brillo[..., None], col('#ffffff') * 1.0 + 0, rgb)
    tinte = rnd(x, y, x * 0, seed + 11) > 0.988
    rgb = np.where(tinte[..., None], col('#bfe8ff'), rgb)
    return img(rgb)


def t_luces_anim(seed):
    """El bloque de luces con sus focos cambiando de color (CUADROS_LUZ cuadros, uno debajo del otro)."""
    return np.concatenate([np.array(t_luces(seed, f)) for f in range(CUADROS_LUZ)], axis=0)


def t_luces(seed, cuadro=0):
    x, y = grid()
    t = ruido(x, y, 3.0, seed, 2)
    rgb = Ramp('#0f2a16', '#173a20', '#1f4a28')(t)
    cs = [col('#ff4040'), col('#ffd23a'), col('#40e070'), col('#4aa0ff'), col('#ff70d0')]
    k = 0
    for cy in (4, 12, 20, 28):
        for cx in (4, 12, 20, 28):
            c = cs[(k + seed + cuadro) % len(cs)]
            if (k * 7 + cuadro + seed) % 5 == 0 and cuadro > 0:
                c = c * 0.35 + 20
            k += 1
            dx, dy = x - (cx + (cy // 8) % 2 * 0), y - cy
            d = np.sqrt(dx * dx + dy * dy)
            rgb = np.where((d < 2.6)[..., None], c, rgb)
            rgb = np.where(((d >= 2.6) & (d < 3.6))[..., None], rgb * 0.4 + c * 0.35, rgb)
            rgb = np.where((d < 1.0)[..., None], np.minimum(c * 0.4 + 170, 255), rgb)
    return img(rgb)


def t_papel_tapiz(seed, fondo='#7c1220', motivo='#d6a23a'):
    x, y = grid()
    rgb = col(fondo) * (0.92 + 0.08 * ruido(x, y, 4.0, seed, 2))[..., None]
    # copos y estrellas doradas repetidos
    cx, cy = (x % 16) - 8, (y % 16) - 8
    estrella = ((np.abs(cx) < 0.8) & (np.abs(cy) < 5)) | ((np.abs(cy) < 0.8) & (np.abs(cx) < 5)) | \
               ((np.abs(cx - cy) < 0.9) & (np.abs(cx) < 3.5)) | ((np.abs(cx + cy) < 0.9) & (np.abs(cx) < 3.5))
    rgb = np.where(estrella[..., None], col(motivo), rgb)
    raya = (x % 16 == 0) | (x % 16 == 15)
    rgb = np.where(raya[..., None], rgb * 0.8, rgb)
    return img(rgb)


def t_cuadros(seed, a='#1d5a2c', b='#a51c26'):
    x, y = grid()
    ca, cb = col(a), col(b)
    rgb = np.where((((x // 8) + (y // 8)) % 2 == 0)[..., None], ca, ca * 0.55 + cb * 0.45)
    linea = ((x % 8) == 3) | ((y % 8) == 3)
    rgb = np.where(linea[..., None], cb, rgb)
    fina = ((x % 8) == 6) | ((y % 8) == 6)
    rgb = np.where(fina[..., None], col('#e6c44a') * 0.8, rgb)
    rgb = rgb * (0.94 + 0.06 * rnd(x, y, x * 0, seed))[..., None]
    return img(rgb)


def t_cabana(seed):
    x, y = grid()
    t = ruido(x * 0.3, y * 2.5, 2.5, seed, 3)
    rgb = Ramp('#5a3418', '#6e4220', '#82512a', '#966236')(t * 0.8 + 0.1)
    tronco = (y // 8)
    borde = (y % 8 < 1) | (y % 8 > 6.5)
    rgb = np.where(borde[..., None], rgb * 0.62, rgb)
    nudo = ((x - 9 - tronco * 5 % 13) ** 2 + ((y % 8) - 4) ** 2) < 2.5
    rgb = np.where(nudo[..., None], rgb * 0.75, rgb)
    return img(rgb)


def t_terciopelo(seed, c='#a3141f'):
    x, y = grid()
    t = ruido(x, y, 3.0, seed, 3)
    rgb = col(c) * (0.78 + 0.34 * t)[..., None]
    return img(rgb)


def t_regalo(seed, papel='#b81d2a', lazo='#f0c43a', motivo=None):
    x, y = grid()
    rgb = col(papel) * (0.92 + 0.08 * ruido(x, y, 4.0, seed, 2))[..., None]
    if motivo:
        lunar = (((x + 4) % 10) - 5) ** 2 + (((y + 4) % 10) - 5) ** 2 < 3.2
        rgb = np.where(lunar[..., None], col(motivo), rgb)
    cinta = (np.abs(x - 15.5) < 3) | (np.abs(y - 15.5) < 3)
    rgb = np.where(cinta[..., None], col(lazo) * (0.9 + 0.1 * np.sin(x + y))[..., None], rgb)
    orilla = (np.abs(x - 15.5) > 2) & (np.abs(x - 15.5) < 3) | (np.abs(y - 15.5) > 2) & (np.abs(y - 15.5) < 3)
    rgb = np.where(orilla[..., None], rgb * 0.8, rgb)
    return img(rgb)


def t_hielo(seed):
    x, y = grid()
    t = ruido(x, y, 4.0, seed, 2)
    rgb = Ramp('#a8d4f0', '#bfe2f6', '#d6eefb', '#eef8ff')(t)
    a = 150 + 40 * t
    # copo de nieve al centro
    cx, cy = x - 15.5, y - 15.5
    r = np.sqrt(cx * cx + cy * cy)
    ang = np.arctan2(cy, cx)
    rama = (np.abs(np.sin(ang * 3)) < 0.12) & (r < 11)
    punta = (np.abs(np.sin(ang * 3)) < 0.35) & (np.abs(r - 8) < 0.8)
    copo = rama | punta
    rgb = np.where(copo[..., None], col('#ffffff'), rgb)
    a = np.where(copo, 235, a)
    marco = (x < 1) | (x > 30) | (y < 1) | (y > 30)
    rgb = np.where(marco[..., None], col('#e8f6ff'), rgb)
    a = np.where(marco, 230, a)
    return img(rgb, a)


def t_alfombra_navidad(seed):
    x, y = grid()
    rgb = col('#8e1622') * (0.9 + 0.1 * ruido(x, y, 3.0, seed, 2))[..., None]
    borde = (x < 3) | (x > 28) | (y < 3) | (y > 28)
    rgb = np.where(borde[..., None], col('#1c5a2c'), rgb)
    filo = ((x == 3) | (x == 28) | (y == 3) | (y == 28)) & ~((x < 3) | (x > 28) | (y < 3) | (y > 28))
    rgb = np.where(filo[..., None], col('#e0b23a'), rgb)
    cx, cy = x - 15.5, y - 15.5
    rombo = (np.abs(cx) + np.abs(cy)) < 6
    rgb = np.where(rombo[..., None], col('#e0b23a') * 0.9, rgb)
    rombo2 = (np.abs(cx) + np.abs(cy)) < 3.5
    rgb = np.where(rombo2[..., None], col('#1c5a2c'), rgb)
    return img(rgb)


def t_alfombra_nieve(seed):
    x, y = grid()
    t = ruido(x, y, 2.0, seed, 3)
    rgb = Ramp('#dfe6ee', '#ebf0f5', '#f6f9fc', '#ffffff')(t)
    return img(rgb)


# ---------------------------------------------------------------- tipos de bloque

def cubo(bid, es, en, fn, variantes=1, sonido='stone', dureza=1.5, luz=0, render=None):
    models = []
    for i in range(variantes):
        t = tex(f'{bid}_{i}' if variantes > 1 else bid, fn(i * 13 + 1))
        m = {'parent': 'minecraft:block/cube_all', 'textures': {'all': t}}
        if render:
            m['render_type'] = 'minecraft:' + render
        mn = f'{bid}_{i}' if variantes > 1 else bid
        dump(f'models/block/{mn}.json', m)
        models.append({'model': f'{NS}:block/{mn}'})
    dump(f'blockstates/{bid}.json', {'variants': {'': models if len(models) > 1 else models[0]}})
    dump(f'models/item/{bid}.json', {'parent': models[0]['model']})
    ficha.append({'id': bid, 'kind': 'cubo', 'sound': sonido, 'strength': dureza, 'light': luz})
    botin(bid)
    nombre(bid, es, en)


def vidrio(bid, es, en, fn, sonido='glass'):
    t = tex(bid, fn(1))
    dump(f'models/block/{bid}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': t}, 'render_type': 'minecraft:translucent'})
    dump(f'blockstates/{bid}.json', {'variants': {'': {'model': f'{NS}:block/{bid}'}}})
    dump(f'models/item/{bid}.json', {'parent': f'{NS}:block/{bid}'})
    ficha.append({'id': bid, 'kind': 'vidrio', 'sound': sonido, 'strength': 0.6})
    botin(bid)
    nombre(bid, es, en)


def alfombra(bid, es, en, fn):
    t = tex(bid, fn(1))
    dump(f'models/block/{bid}.json', {'parent': 'minecraft:block/carpet', 'textures': {'wool': t}})
    dump(f'blockstates/{bid}.json', {'variants': {'': {'model': f'{NS}:block/{bid}'}}})
    dump(f'models/item/{bid}.json', {'parent': f'{NS}:block/{bid}'})
    ficha.append({'id': bid, 'kind': 'alfombra', 'sound': 'wool', 'strength': 0.2})
    botin(bid)
    nombre(bid, es, en)


def prop(p, extra=None):
    f = kit.write_prop(p, ROOT)
    parpadear(p)
    f['kind'] = 'prop'
    f['sub'] = p.kind
    f.update(extra or {})
    ficha.append(f)
    nombre(p.id, p.es, p.en)
    if p.tall:
        objeto_alto(p.id)
    return f


def objeto_alto(pid):
    """El objeto de un adorno de dos bloques de alto: las dos mitades juntas a la mitad de tamaño (en el inventario se ve entero)."""
    base = os.path.join(A, 'models', 'block')
    lo = json.load(open(os.path.join(base, pid + '_lower.json')))
    up = json.load(open(os.path.join(base, pid + '_upper.json')))

    def esc(v, dy=0.0):
        return [round(8 + (v[0] - 8) * 0.5, 4), round((v[1] + dy) * 0.5, 4), round(8 + (v[2] - 8) * 0.5, 4)]
    els = []
    for src, dy in ((lo, 0.0), (up, 16.0)):
        for e in src['elements']:
            e = json.loads(json.dumps(e))
            e['from'], e['to'] = esc(e['from'], dy), esc(e['to'], dy)
            if 'rotation' in e:
                e['rotation']['origin'] = esc(e['rotation']['origin'], dy)
            els.append(e)
    j = {'textures': lo['textures'], 'elements': els,
         'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [1.0, 1.0, 1.0]},
                     'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.6, 0.6, 0.6]},
                     'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1.0, 1.0, 1.0]},
                     'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.6, 0.6, 0.6]},
                     'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.7, 0.7, 0.7]}}}
    if 'render_type' in lo:
        j['render_type'] = lo['render_type']
    dump(f'models/item/{pid}.json', j)


# ---------------------------------------------------------------- materiales de los adornos

def agujas(base='#1e5a2a', seed=1, nieve=0.0):
    r = Ramp('#0f3317', '#174524', base, '#2e7a3a')

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        t = fbm(X * 1.6, Y * 1.6, Z * 1.6, 1.5, P['seed'] + seed, 3) * 0.8 + (rnd(X, Y, Z, seed) - 0.5) * 0.35
        rgb = r(t)
        if nieve:
            n = rnd(X, Y, Z, seed + 4) > (1 - nieve)
            rgb = np.where(n[..., None], np.array([236, 243, 250], np.float32), rgb)
        return with_alpha(rgb)
    return f


def rayas(a='#d8202c', b='#f4efe8', ancho=2.0):
    ca, cb = np.array(hexc(a), np.float32), np.array(hexc(b), np.float32)

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        banda = (np.floor((X + Y + Z) / ancho) % 2) == 0
        rgb = np.where(banda[..., None], ca, cb)
        return with_alpha(rgb * (0.92 + 0.08 * rnd(X, Y, Z, 3))[..., None])
    return f


def brillo(color):
    """Algo que brilla en la oscuridad (focos, esferas, la estrella)."""
    c = np.array(hexc(color), np.float32)

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        v = 0.85 + 0.25 * fbm(X, Y, Z, 1.5, P['seed'] + 7, 2)
        rgb = np.clip(c * v[..., None], 0, 255)
        hl = rnd(X, Y, Z, 5) > 0.9
        rgb = np.where(hl[..., None], np.minimum(rgb * 0.5 + 150, 255), rgb)
        return with_alpha(rgb)
    f.emit = True
    return f


ORO = metal(Ramp('#7a5414', '#a8761e', '#d4a23a', '#f0cc66', '#fff0a8'), streak=0.4, seed=21, sparkle=0.12)
PLATA = metal(Ramp('#5c646e', '#8a939e', '#b8c0ca', '#e0e6ee'), streak=0.4, seed=22, sparkle=0.12)
ROJO = cloth('#b81d2a', weave=0.12, seed=31)
VERDE = cloth('#1e6a32', weave=0.12, seed=32)
BLANCO = cloth('#f2f0ec', weave=0.08, seed=33)
NEGRO = cloth('#1c1c22', weave=0.1, seed=34)
MADERA = wood(Ramp('#4a2c14', '#5e3a1c', '#734a26', '#885a30'), seed=35)
MADERA_CLARA = wood(Ramp('#8a6238', '#a37848', '#ba8e58', '#cfa46a'), seed=36)
NIEVE = stone(Ramp('#cfdbe6', '#dde7f0', '#eaf1f7', '#f6f9fc'), scale=3, seed=37)
BARRO = stone(Ramp('#7a3a1c', '#8e4824', '#a3582e', '#b4683a'), scale=2.5, seed=38)
LADRILLO = stone(Ramp('#5e2418', '#74301e', '#8a3c26', '#9c4a30'), scale=1.6, seed=39, cracks=0.4)
CARBON = solid('#222228')
ZANAHORIA = cloth('#ec7a1c', weave=0.15, seed=40)
CREMA = cloth('#f6e7c8', weave=0.1, seed=41)
JENGIBRE = stone(Ramp('#7a3f17', '#8f4c1d', '#a35a24', '#b4692c'), scale=2, seed=42)
FOCOS = ['#ff3b3b', '#ffd23a', '#3be070', '#4aa0ff', '#ff70d0']


def esferas(p, puntos, seed=0, colores=None):
    colores = colores or ['#d42030', '#e8b830', '#c8ccd4']
    for i, (x, y, z) in enumerate(puntos):
        c = colores[(i + seed) % len(colores)]
        p.box(x - 0.75, y - 0.75, z - 0.75, x + 0.75, y + 0.75, z + 0.75, gem(c, seed=i + seed, bright=1.15))


def focos(p, puntos, seed=0):
    for i, (x, y, z) in enumerate(puntos):
        b = p.box(x - 0.45, y - 0.45, z - 0.45, x + 0.45, y + 0.45, z + 0.45, brillo(FOCOS[(i + seed) % len(FOCOS)]))
        b.foco = i + seed


CUADROS_LUZ = 4


def parpadear(p):
    """Si el adorno tiene focos, su textura se vuelve animada: cada foco cambia de color por turnos y a veces se apaga un momento
    (CUADROS_LUZ cuadros de 0.6 s). Se hace después de escribir la textura, con los lugares que el kit le dio a cada cara."""
    focos_ = [b for _, b in p.boxes if hasattr(b, 'foco') and b.rects]
    if not focos_:
        return
    ruta = os.path.join(A, 'textures/block', kit.PREFIX + p.id + '.png')
    base = np.array(Image.open(ruta).convert('RGBA'), np.float32)
    S = base.shape[0]
    esc = S / 16.0 * 16 / S  # las coordenadas de b.rects ya vienen en pixeles de la textura
    cuadros = []
    for f in range(CUADROS_LUZ):
        im = base.copy()
        for b in focos_:
            c = np.array(hexc(FOCOS[(b.foco + f) % len(FOCOS)]), np.float32)
            apagado = (b.foco * 7 + f) % 5 == 0
            for face, (x, y, w, h) in b.rects.items():
                x0, y0 = int(round(x * esc)), int(round(y * esc))
                x1, y1 = max(x0 + 1, int(round((x + w) * esc))), max(y0 + 1, int(round((y + h) * esc)))
                reg = base[y0:y1, x0:x1, :3]
                lum = reg.mean(axis=2, keepdims=True) / max(1.0, reg.mean())
                nuevo = np.clip(c * lum, 0, 255)
                if apagado:
                    nuevo = nuevo * 0.35 + 20
                im[y0:y1, x0:x1, :3] = nuevo
        cuadros.append(im)
    tira = np.concatenate(cuadros, axis=0)
    Image.fromarray(np.clip(tira, 0, 255).astype(np.uint8), 'RGBA').save(ruta)
    with open(ruta + '.mcmeta', 'w') as fh:
        json.dump({'animation': {'frametime': 12}}, fh)


def anillo(cx, cy, cz, r, n, fase=0.0):
    return [(cx + r * math.cos(2 * math.pi * (k + fase) / n), cy, cz + r * math.sin(2 * math.pi * (k + fase) / n)) for k in range(n)]


# ---------------------------------------------------------------- los adornos

def arbol(pid, es, en, nevado=False, colores=None, seed=1):
    p = Prop(pid, es, en, kind='floor', tall=True, light=11, sound='wool', seed=seed, collide=True,
             particles=[{'type': 'minecraft:end_rod', 'pos': [8, 29, 8], 'rate': 0.08}],
             shape=[[2, 0, 2, 14, 16, 14]], shape_upper=[[4, 0, 4, 12, 14, 12]])
    ag = agujas('#e4ecf2' if nevado else '#1e5a2a', seed=seed, nieve=0.0 if nevado else 0.06)
    if nevado:
        ag = stone(Ramp('#b8c8d6', '#cdd9e4', '#dfe8f0', '#f2f6fa'), scale=1.4, seed=seed)
    p.at('lower')
    p.box(4, 0, 4, 12, 2, 12, ROJO)                       # la tela de abajo
    p.box(7, 0, 7, 9, 3, 9, MADERA)                        # tronco
    capas = [(0.5, 3, 8), (2, 8, 12), (3.5, 12, 16)]
    for m, y0, y1 in capas:
        p.box(m, y0, m, 16 - m, y1, 16 - m, ag)
    esferas(p, [(0.5, 5, 4), (15.5, 6, 11), (5, 4, 15.5), (11, 7, 0.5), (2, 10, 13), (14, 9.5, 4), (8, 13, 3.5), (12.5, 14, 12.5),
                (3.5, 14.5, 7)], seed=seed, colores=colores)
    focos(p, [(0.5, 7, 9), (6, 7.5, 0.5), (15.5, 4.5, 6), (10, 4, 15.5), (2, 11.5, 3), (14, 11, 14), (7, 15, 12.5), (12.5, 14.5, 3.5)],
          seed=seed)
    p.at('upper')
    for m, y0, y1 in [(4.5, 0, 4), (5.5, 4, 8), (6.5, 8, 11)]:
        p.box(m, y0, m, 16 - m, y1, 16 - m, ag)
    esferas(p, [(4.5, 2, 9), (11.5, 3, 6), (8, 5, 5.5), (10.5, 6.5, 10.5)], seed=seed + 2, colores=colores)
    focos(p, [(5.5, 6, 6), (10.5, 2.5, 11.5), (6.5, 9.5, 9.5)], seed=seed + 1)
    est = brillo('#ffd84a')
    p.box(7.25, 11, 7.25, 8.75, 15.5, 8.75, est)
    p.box(5.75, 12.5, 7.5, 10.25, 14, 8.5, est)
    p.box(6.5, 11.5, 7.6, 9.5, 14.8, 8.4, est)
    return p


def pino_maceta():
    p = Prop('pino_maceta', 'Pino en maceta', 'Potted pine', kind='floor', sound='wool', seed=5, light=5)
    p.box(5, 0, 5, 11, 4, 11, BARRO)
    p.box(5.5, 3.5, 5.5, 10.5, 4, 10.5, solid('#3a2614'))
    p.box(7.5, 4, 7.5, 8.5, 5, 8.5, MADERA)
    ag = agujas(seed=5, nieve=0.05)
    for m, y0, y1 in [(4, 5, 8), (5, 8, 11), (6, 11, 13.5), (7, 13.5, 15.5)]:
        p.box(m, y0, m, 16 - m, y1, 16 - m, ag)
    focos(p, [(4, 6.5, 8), (12, 7, 6), (8, 9, 4.8), (11, 10, 11), (6, 12, 6)], seed=2)
    p.box(7.5, 15.5, 7.5, 8.5, 16, 8.5, brillo('#ffd84a'))
    return p


def esfera(pid, es, en, color, seed):
    p = Prop(pid, es, en, kind='hang', sound='glass', seed=seed, collide=False, light=4)
    p.box(7.75, 12, 7.75, 8.25, 16, 8.25, solid('#c8a040'))
    p.box(7, 10.5, 7, 9, 12, 9, ORO)
    p.box(4.5, 3, 4.5, 11.5, 10.5, 11.5, gem(color, seed=seed, bright=1.1))
    p.box(5.5, 2.2, 5.5, 10.5, 3, 10.5, gem(color, seed=seed + 1, bright=1.0))
    p.box(5.5, 10.5, 5.5, 10.5, 11, 10.5, gem(color, seed=seed + 2, bright=1.0))
    return p


def guirnalda():
    p = Prop('guirnalda_luces', 'Guirnalda con luces', 'Garland with lights', kind='wall', sound='wool', seed=11, collide=False, light=9)
    ag = agujas(seed=11, nieve=0.04)
    for i in range(8):
        x0 = i * 2
        y = 11 + 2.5 * math.sin(math.pi * (i + 0.5) / 8)
        p.box(x0, 16 - y + 2, 13, x0 + 2, 16 - y + 5, 16, ag)
    focos(p, [(1 + 2 * i, 16 - (11 + 2.5 * math.sin(math.pi * (i + 0.5) / 8)) + 1.4, 12.6) for i in range(8)], seed=1)
    p.box(6.5, 9.5, 12.5, 9.5, 12, 13.5, ROJO)
    return p


def luces_colgantes():
    p = Prop('luces_colgantes', 'Luces colgantes', 'Hanging lights', kind='hang', sound='lantern', seed=12, collide=False, light=10)
    cable = solid('#1d3a22')
    for i in range(8):
        x0 = i * 2
        y = 13 - 3 * math.sin(math.pi * (i + 0.5) / 8)
        p.box(x0, y, 7.6, x0 + 2, y + 0.6, 8.4, cable)
    focos(p, [(1 + 2 * i, 12.2 - 3 * math.sin(math.pi * (i + 0.5) / 8), 8) for i in range(8)], seed=3)
    for x0 in (0, 15):
        p.box(x0, 13, 7.6, x0 + 1, 16, 8.4, cable)
    return p


def corona():
    p = Prop('corona_navidad', 'Corona navideña', 'Christmas wreath', kind='wall', sound='wool', seed=13, collide=False)
    ag = agujas(seed=13, nieve=0.03)
    for k in range(16):
        a = 2 * math.pi * k / 16
        cx, cy = 8 + 5.2 * math.cos(a), 8.5 + 5.2 * math.sin(a)
        p.box(cx - 1.6, cy - 1.6, 13.5, cx + 1.6, cy + 1.6, 16, ag)
    esferas(p, [(8 + 5.2 * math.cos(a), 8.5 + 5.2 * math.sin(a), 13.2) for a in (0.3, 1.4, 2.6, 3.9, 5.1)], colores=['#d42030'])
    p.box(6, 2, 13, 10, 4.5, 14, ROJO)
    p.box(4, 2.5, 13, 6.5, 4.5, 14, ROJO)
    p.box(9.5, 2.5, 13, 12, 4.5, 14, ROJO)
    p.box(6.5, 0, 13.2, 7.5, 2.5, 13.8, ROJO)
    p.box(8.5, 0, 13.2, 9.5, 2.5, 13.8, ROJO)
    return p


def calcetin(pid, es, en, tela, seed):
    p = Prop(pid, es, en, kind='wall', sound='wool', seed=seed, collide=False)
    p.box(5, 9, 13.5, 11, 14, 16, BLANCO)
    p.box(5.5, 3, 14, 10.5, 9, 16, tela)
    p.box(3, 1, 14, 10.5, 4.5, 16, tela)
    p.box(2.5, 1, 14, 4.5, 4, 16, BLANCO)
    p.box(7.6, 14, 14.6, 8.4, 15.5, 15.4, solid('#c8a040'))
    p.box(6, 11, 13.2, 7, 12.5, 13.6, brillo('#ffd84a'))
    return p


def muerdago():
    p = Prop('muerdago', 'Muérdago', 'Mistletoe', kind='hang', sound='wool', seed=14, collide=False)
    p.box(7.6, 11, 7.6, 8.4, 16, 8.4, ROJO)
    p.box(6, 10, 6, 10, 11.5, 10, ROJO)
    hoja = cloth('#3c8a3a', weave=0.12, seed=14)
    for (x, z) in ((5, 8), (11, 8), (8, 5), (8, 11), (6, 6), (10, 10)):
        p.box(x - 1.5, 5.5, z - 1, x + 1.5, 9.5, z + 1, hoja)
    for (x, z) in ((7, 8), (9, 8.5), (8, 7)):
        p.box(x - 0.6, 7, z - 0.6, x + 0.6, 8.2, z + 0.6, solid('#f4f4ee'))
    return p


def nochebuena():
    p = Prop('nochebuena', 'Nochebuena', 'Poinsettia', kind='floor', sound='wool', seed=15, collide=False)
    p.box(5, 0, 5, 11, 5, 11, ORO)
    p.box(5.5, 4.5, 5.5, 10.5, 5, 10.5, solid('#3a2614'))
    hoja = cloth('#2e6e30', weave=0.12, seed=15)
    roja = cloth('#c81828', weave=0.1, seed=16)
    for a in range(5):
        an = 2 * math.pi * a / 5
        x, z = 8 + 3.2 * math.cos(an), 8 + 3.2 * math.sin(an)
        p.box(x - 1.8, 6, z - 1.8, x + 1.8, 7, z + 1.8, hoja)
    for a in range(6):
        an = 2 * math.pi * (a + 0.5) / 6
        x, z = 8 + 2.6 * math.cos(an), 8 + 2.6 * math.sin(an)
        p.box(x - 1.6, 7.5, z - 1.6, x + 1.6, 8.5, z + 1.6, roja)
    p.box(6.5, 8, 6.5, 9.5, 9.5, 9.5, roja)
    p.box(7.3, 9.5, 7.3, 8.7, 10, 8.7, solid('#f2d23a'))
    return p


def corona_adviento():
    llamas = [{'type': 'minecraft:small_flame', 'pos': [x, 11.6, z], 'rate': 0.3} for x, z in ((4, 8), (12, 8), (8, 4), (8, 12))]
    p = Prop('corona_adviento', 'Corona de Adviento', 'Advent wreath', kind='floor', sound='wool', seed=16, collide=False, light=10,
             particles=llamas)
    ag = agujas(seed=16, nieve=0.03)
    for k in range(12):
        a = 2 * math.pi * k / 12
        cx, cz = 8 + 4.5 * math.cos(a), 8 + 4.5 * math.sin(a)
        p.box(cx - 1.6, 0, cz - 1.6, cx + 1.6, 2.4, cz + 1.6, ag)
    morada = cloth('#6a2a8a', weave=0.08, seed=17)
    rosa = cloth('#d670a8', weave=0.08, seed=18)
    for i, (x, z) in enumerate(((4, 8), (12, 8), (8, 4), (8, 12))):
        p.box(x - 1, 2, z - 1, x + 1, 9.5, z + 1, rosa if i == 2 else morada)
        p.box(x - 0.4, 9.5, z - 0.4, x + 0.4, 11, z + 0.4, flame())
    esferas(p, [(8 + 4.5 * math.cos(a), 2.6, 8 + 4.5 * math.sin(a)) for a in (0.8, 2.4, 3.9, 5.5)], colores=['#d42030'])
    return p


def velas():
    llamas = [{'type': 'minecraft:small_flame', 'pos': [x, y + 1.6, z], 'rate': 0.3} for x, y, z in ((5, 10, 8), (9, 13, 7), (12, 8, 10))]
    p = Prop('velas_navidad', 'Velas navideñas', 'Christmas candles', kind='floor', sound='wool', seed=19, collide=False, light=9,
             particles=llamas)
    roja = cloth('#c01c26', weave=0.06, seed=19)
    for x, h, z in ((5, 10, 8), (9, 13, 7), (12, 8, 10)):
        p.box(x - 1.3, 0, z - 1.3, x + 1.3, h, z + 1.3, roja)
        p.box(x - 0.4, h, z - 0.4, x + 0.4, h + 1.6, z + 0.4, flame())
    hoja = cloth('#2e6e30', weave=0.12, seed=19)
    for x, z in ((3, 11), (7, 11.5), (11, 5), (14, 7)):
        p.box(x - 1.5, 0, z - 0.8, x + 1.5, 0.8, z + 0.8, hoja)
    for x, z in ((4.5, 11), (12.5, 6)):
        p.box(x - 0.6, 0.6, z - 0.6, x + 0.6, 1.6, z + 0.6, solid('#e02030'))
    return p


def regalo(pid, es, en, papel, lazo, seed, alto=9, ancho=10):
    p = Prop(pid, es, en, kind='floor', sound='wool', seed=seed)
    a0, a1 = 8 - ancho / 2, 8 + ancho / 2
    pap = cloth(papel, weave=0.08, seed=seed)
    cin = cloth(lazo, weave=0.06, seed=seed + 1)
    p.box(a0, 0, a0, a1, alto, a1, pap)
    p.box(7, 0, a0 - 0.2, 9, alto + 0.2, a1 + 0.2, cin)
    p.box(a0 - 0.2, 0, 7, a1 + 0.2, alto + 0.2, 9, cin)
    p.box(5, alto, 7, 7.5, alto + 2.5, 9, cin)
    p.box(8.5, alto, 7, 11, alto + 2.5, 9, cin)
    p.box(7.2, alto, 7.2, 8.8, alto + 1.4, 8.8, cin)
    return p


def pila_regalos():
    p = Prop('pila_regalos', 'Pila de regalos', 'Pile of presents', kind='floor', sound='wool', seed=24)
    for (x0, z0, x1, z1, y0, y1, pa, la, s) in ((1, 1, 9, 9, 0, 7, '#b81d2a', '#f0c43a', 1), (8, 6, 15, 14, 0, 6, '#1e6a32', '#d8202c', 2),
                                                 (3, 2, 9, 8, 7, 12, '#2a4fb0', '#e8e8f0', 3), (2, 10, 7, 15, 0, 4, '#f0c43a', '#b81d2a', 4)):
        pap, cin = cloth(pa, weave=0.08, seed=s), cloth(la, weave=0.06, seed=s + 9)
        p.box(x0, y0, z0, x1, y1, z1, pap)
        cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
        p.box(cx - 0.8, y0, z0 - 0.1, cx + 0.8, y1 + 0.1, z1 + 0.1, cin)
        p.box(x0 - 0.1, y0, cz - 0.8, x1 + 0.1, y1 + 0.1, cz + 0.8, cin)
    return p


def muneco():
    p = Prop('muneco_nieve', 'Muñeco de nieve', 'Snowman', kind='floor', tall=True, sound='wool', seed=25,
             shape=[[3, 0, 3, 13, 16, 13]], shape_upper=[[4, 0, 4, 12, 14, 12]])
    p.at('lower')
    p.box(2, 0, 2, 14, 11, 14, NIEVE)
    p.box(3.5, 11, 3.5, 12.5, 16, 12.5, NIEVE)
    for y in (13, 15):
        p.box(7.4, y, 3.1, 8.6, y + 1, 3.6, CARBON)
    p.box(7.4, 8, 1.6, 8.6, 9, 2.1, CARBON)
    p.at('upper')
    p.box(3.5, 0, 3.5, 12.5, 2.5, 12.5, NIEVE)
    p.box(3.2, 0.8, 3.2, 12.8, 2.6, 12.8, ROJO)
    p.box(9.5, -3, 2.8, 11.5, 1, 4, ROJO)
    p.box(4.5, 2.5, 4.5, 11.5, 9, 11.5, NIEVE)
    p.box(5.2, 6, 4.1, 6.4, 7.2, 4.5, CARBON)
    p.box(9.6, 6, 4.1, 10.8, 7.2, 4.5, CARBON)
    p.box(7.3, 4.3, 1.5, 8.7, 5.7, 4.5, ZANAHORIA)
    for x in (5.8, 7, 8.2, 9.4):
        p.box(x, 3.4, 4.1, x + 0.8, 4.0, 4.5, CARBON)
    p.box(4, 9, 4, 12, 9.8, 12, NEGRO)
    p.box(5.5, 9.8, 5.5, 10.5, 14.5, 10.5, NEGRO)
    p.box(5.4, 10.2, 5.4, 10.6, 11.2, 10.6, ROJO)
    for (x0, x1) in ((0, 3.5), (12.5, 16)):
        p.at('lower')
        p.box(x0, 12.5, 7.6, x1, 13.3, 8.4, MADERA)
    return p


def cascanueces():
    p = Prop('cascanueces', 'Cascanueces', 'Nutcracker', kind='floor', tall=True, sound='wood', seed=26,
             shape=[[4, 0, 4, 12, 16, 12]], shape_upper=[[4.5, 0, 4.5, 11.5, 14, 11.5]])
    rojo = cloth('#b8202a', weave=0.06, seed=26)
    azul = cloth('#203a8a', weave=0.06, seed=27)
    p.at('lower')
    p.box(4, 0, 4, 12, 1.5, 12, MADERA)
    p.box(5, 1.5, 6, 7.5, 9, 10, azul)
    p.box(8.5, 1.5, 6, 11, 9, 10, azul)
    p.box(5, 1.5, 5.5, 7.5, 3, 10, NEGRO)
    p.box(8.5, 1.5, 5.5, 11, 3, 10, NEGRO)
    p.box(4.5, 9, 5.5, 11.5, 16, 10.5, rojo)
    p.box(4.4, 9, 5.4, 11.6, 10.2, 10.6, NEGRO)
    p.box(7.3, 10.2, 5.2, 8.7, 16, 5.5, ORO)
    for y in (11.5, 13.5):
        p.box(6, y, 5.2, 7, y + 0.8, 5.5, ORO)
        p.box(9, y, 5.2, 10, y + 0.8, 5.5, ORO)
    p.box(3, 9.5, 6.5, 4.5, 15.5, 9.5, rojo)
    p.box(11.5, 9.5, 6.5, 13, 15.5, 9.5, rojo)
    p.box(3, 8.5, 6.5, 4.5, 9.5, 9.5, BLANCO)
    p.box(11.5, 8.5, 6.5, 13, 9.5, 9.5, BLANCO)
    p.at('upper')
    p.box(5, 0, 5.5, 11, 6, 10.5, CREMA)
    p.box(6, 1, 5.2, 10, 2.2, 5.5, BLANCO)
    p.box(5.6, 2.2, 5.3, 10.4, 2.8, 5.5, NEGRO)
    p.box(6.3, 3.6, 5.2, 7.3, 4.6, 5.5, CARBON)
    p.box(8.7, 3.6, 5.2, 9.7, 4.6, 5.5, CARBON)
    p.box(7.5, 2.6, 4.6, 8.5, 4, 5.5, CREMA)
    p.box(4.8, 6, 5.3, 11.2, 13, 10.7, NEGRO)
    p.box(4.7, 6, 5.2, 11.3, 7, 10.8, ORO)
    p.box(7.4, 9, 5, 8.6, 12, 5.4, ORO)
    p.box(7.2, 13, 7.2, 8.8, 14.5, 8.8, rojo)
    return p


def baston():
    p = Prop('baston_gigante', 'Bastón de caramelo gigante', 'Giant candy cane', kind='floor', tall=True, sound='stone', seed=27,
             collide=True, shape=[[6, 0, 6, 10, 16, 10]], shape_upper=[[3, 0, 6, 11, 16, 10]])
    r = rayas()
    p.at('lower')
    p.box(6.5, 0, 6.5, 9.5, 16, 9.5, r)
    p.at('upper')
    p.box(6.5, 0, 6.5, 9.5, 11, 9.5, r)
    p.box(6, 11, 6.5, 9, 14, 9.5, r)
    p.box(4, 13, 6.5, 7, 16, 9.5, r)
    p.box(2.5, 11, 6.5, 5.5, 14, 9.5, r)
    p.box(2.5, 8, 6.5, 5.5, 11, 9.5, r)
    p.box(6.4, 12, 6.3, 9.6, 13, 9.7, cloth('#1e6a32', 0.08, 27))
    return p


def trineo():
    p = Prop('trineo', 'Trineo de Santa', "Santa's sleigh", kind='floor', sound='wood', seed=28)
    roj = cloth('#a81622', weave=0.06, seed=28)
    p.box(1, 0, 2, 15, 1, 3.5, ORO)
    p.box(1, 0, 12.5, 15, 1, 14, ORO)
    p.box(0, 1, 2, 2, 4, 3.5, ORO)
    p.box(0, 1, 12.5, 2, 4, 14, ORO)
    for x in (4, 11):
        p.box(x, 1, 2.5, x + 1, 3, 3, ORO)
        p.box(x, 1, 13, x + 1, 3, 13.5, ORO)
    p.box(2, 3, 2, 15, 7, 14, roj)
    p.box(11, 7, 2, 15, 12, 14, roj)
    p.box(2, 7, 2, 11, 8, 3, roj)
    p.box(2, 7, 13, 11, 8, 14, roj)
    p.box(1.8, 6.8, 1.8, 15.2, 7.4, 14.2, ORO)
    p.box(10.8, 11.6, 1.8, 15.2, 12.2, 14.2, ORO)
    p.box(3, 7, 3, 10.5, 8.5, 13, cloth('#6a1018', 0.08, 29))
    p.box(4, 8.5, 4, 9, 12, 9, cloth('#5a3a20', 0.12, 30))
    esferas(p, [(5, 12.6, 5), (8, 12.6, 7.5)], colores=['#d42030', '#1e6a32'])
    return p


def campanas():
    p = Prop('campanas', 'Campanas navideñas', 'Christmas bells', kind='hang', sound='metal', seed=31, collide=False)
    for x in (5, 11):
        p.box(x - 2.5, 3, 6, x + 2.5, 4, 10, ORO)
        p.box(x - 2, 4, 6.5, x + 2, 7.5, 9.5, ORO)
        p.box(x - 1.4, 7.5, 7, x + 1.4, 9.5, 9, ORO)
        p.box(x - 0.6, 2, 7.4, x + 0.6, 3, 8.6, solid('#7a5414'))
    p.box(6.5, 9.5, 7, 9.5, 12, 9, ROJO)
    p.box(4.5, 10, 7, 6.5, 12, 9, ROJO)
    p.box(9.5, 10, 7, 11.5, 12, 9, ROJO)
    p.box(7.6, 12, 7.6, 8.4, 16, 8.4, ROJO)
    p.box(6, 9.4, 6.5, 8, 10, 9.5, cloth('#2e6e30', 0.1, 31))
    return p


def chimenea():
    fuego = [{'type': 'minecraft:flame', 'pos': [8, 4, 7], 'rate': 0.5}, {'type': 'minecraft:smoke', 'pos': [8, 8, 7], 'rate': 0.2}]
    p = Prop('chimenea', 'Chimenea navideña', 'Christmas fireplace', kind='floor', sound='stone', seed=32, light=14, particles=fuego)
    p.box(0, 0, 9, 16, 16, 16, LADRILLO)
    p.box(0, 0, 4, 3, 13, 9, LADRILLO)
    p.box(13, 0, 4, 16, 13, 9, LADRILLO)
    p.box(0, 13, 3, 16, 15, 9, MADERA)
    p.box(3, 0, 4, 13, 1, 9, solid('#2a1c18'))
    p.box(4, 1, 6, 12, 2, 8, MADERA)
    p.box(5, 1, 5.5, 7, 6, 5.5, flame())
    p.box(7, 1, 6, 10, 8, 6, flame())
    p.box(10, 1, 6.5, 12, 5, 6.5, flame())
    p.box(3, 10, 3.5, 13, 13, 4.5, agujas(seed=32, nieve=0.03))
    focos(p, [(4, 11.5, 3.3), (6.5, 12, 3.3), (9.5, 11.5, 3.3), (12, 12, 3.3)], seed=4)
    for x in (2, 13):
        p.box(x - 1, 15, 4, x + 1, 16, 6, CREMA)
    return p


def esfera_nieve():
    p = Prop('esfera_nieve', 'Esfera de nieve', 'Snow globe', kind='floor', sound='glass', seed=33, collide=False, render='translucent')
    p.box(4, 0, 4, 12, 2.5, 12, MADERA)
    p.box(4.5, 2.5, 4.5, 11.5, 3.2, 11.5, NIEVE)
    vidrio_m = stone(Ramp('#bcd8ec', '#cfe4f4', '#e2f0fa'), scale=3, seed=33)
    vidrio_m.cutout = False
    p.box(7.2, 3.2, 7.2, 8.8, 4.5, 8.8, MADERA)
    ag = agujas(seed=33, nieve=0.1)
    for m, y0, y1 in [(5.8, 4.5, 6.5), (6.4, 6.5, 8.2), (7, 8.2, 9.5)]:
        p.box(m, y0, m, 16 - m, y1, 16 - m, ag)
    p.box(4.3, 2.5, 4.3, 11.7, 10.5, 4.5, solid('#d8ecfa', 90))
    p.box(4.3, 2.5, 11.5, 11.7, 10.5, 11.7, solid('#d8ecfa', 90))
    p.box(4.3, 2.5, 4.5, 4.5, 10.5, 11.5, solid('#d8ecfa', 90))
    p.box(11.5, 2.5, 4.5, 11.7, 10.5, 11.5, solid('#d8ecfa', 90))
    p.box(4.3, 10.5, 4.3, 11.7, 10.7, 11.7, solid('#d8ecfa', 90))
    return p


def galletas_leche():
    p = Prop('galletas_leche', 'Galletas y leche para Santa', 'Cookies and milk for Santa', kind='floor', sound='glass', seed=34,
             collide=False)
    p.box(1.5, 0, 3, 10.5, 0.8, 12, solid('#f4f4f6'))
    p.box(2, 0.8, 3.5, 10, 1.1, 11.5, solid('#d8dce4'))
    for x, z in ((3.5, 5), (6.5, 6), (4.5, 9), (7.5, 9.5)):
        p.box(x - 1.4, 0.9, z - 1.4, x + 1.4, 1.7, z + 1.4, JENGIBRE)
        p.box(x - 0.4, 1.7, z - 0.4, x + 0.4, 1.8, z + 0.4, solid('#ffffff'))
    p.box(11, 0, 8, 14.5, 6, 11.5, solid('#e8f2fa', 170))
    p.box(11.3, 0.2, 8.3, 14.2, 5, 11.2, solid('#fbfbf6'))
    return p


def plato_migajas():
    """Lo que queda después de que Santa pasa: el plato con migajas, el vaso casi vacío y una notita de gracias."""
    p = Prop('plato_migajas', 'Plato con migajas', 'Plate with crumbs', kind='floor', sound='glass', seed=44, collide=False)
    p.box(1.5, 0, 3, 10.5, 0.8, 12, solid('#f4f4f6'))
    p.box(2, 0.8, 3.5, 10, 1.1, 11.5, solid('#d8dce4'))
    for x, z in ((3.2, 5.2), (5.6, 8.4), (7.8, 6.1), (4.4, 10.2), (8.4, 9.6), (6.6, 4.6)):
        p.box(x - 0.4, 1.1, z - 0.4, x + 0.3, 1.4, z + 0.3, JENGIBRE)
    p.box(5.6, 1.1, 6.6, 7.4, 1.6, 8.4, JENGIBRE)
    p.box(11, 0, 8, 14.5, 6, 11.5, solid('#e8f2fa', 170))
    p.box(11.3, 0.2, 8.3, 14.2, 0.9, 11.2, solid('#fbfbf6'))
    p.box(10.8, 0, 2.5, 15, 0.3, 6.5, CREMA)
    p.box(11.4, 0.3, 3.2, 14.2, 0.35, 3.5, solid('#b81d2a'))
    p.box(11.4, 0.3, 4.2, 13.6, 0.35, 4.5, solid('#3a3a44'))
    p.box(11.4, 0.3, 5.2, 14, 0.35, 5.5, solid('#3a3a44'))
    return p


def chocolate():
    p = Prop('taza_chocolate', 'Taza de chocolate caliente', 'Mug of hot chocolate', kind='floor', sound='glass', seed=35,
             collide=False, particles=[{'type': 'minecraft:cloud', 'pos': [8, 7, 8], 'rate': 0.05}])
    p.box(5, 0, 5, 11, 6, 11, cloth('#c81c28', 0.05, 35))
    p.box(5.5, 5.5, 5.5, 10.5, 5.8, 10.5, solid('#5a2e16'))
    p.box(11, 1.5, 7.3, 13, 2.5, 8.7, cloth('#c81c28', 0.05, 35))
    p.box(12, 2.5, 7.3, 13, 4.5, 8.7, cloth('#c81c28', 0.05, 35))
    p.box(11, 4.5, 7.3, 13, 5.5, 8.7, cloth('#c81c28', 0.05, 35))
    for x, z in ((7, 7), (9, 8.5), (7.5, 9.3)):
        p.box(x - 0.7, 5.8, z - 0.7, x + 0.7, 6.8, z + 0.7, solid('#fbf4ee'))
    p.box(4.9, 2.5, 4.9, 11.1, 3.5, 11.1, solid('#f2f0ec'))
    return p


def ponche():
    p = Prop('olla_ponche', 'Olla de ponche', 'Pot of ponche', kind='floor', sound='stone', seed=36,
             particles=[{'type': 'minecraft:cloud', 'pos': [8, 12, 8], 'rate': 0.08}])
    p.box(2, 0, 2, 14, 10, 14, BARRO)
    p.box(1.5, 9, 1.5, 14.5, 11, 14.5, BARRO)
    p.box(2.5, 10.5, 2.5, 13.5, 10.8, 13.5, cloth('#b8541c', 0.15, 36))
    for x, z, c in ((5, 6, '#e8b020'), (9, 5, '#c84018'), (7, 10, '#e8b020'), (11, 9, '#8a5a30'), (4.5, 10.5, '#d8c060')):
        p.box(x - 0.9, 10.6, z - 0.9, x + 0.9, 11.6, z + 0.9, solid(c))
    p.box(9.5, 10.5, 9.5, 10.5, 16, 10.5, MADERA)
    p.box(0.5, 6, 7, 1.5, 9, 9, BARRO)
    p.box(14.5, 6, 7, 15.5, 9, 9, BARRO)
    for x in range(3, 14, 3):
        p.box(x, 5, 1.8, x + 1.5, 6, 2, solid('#f0e6c8'))
    return p


def farolito():
    p = Prop('farolito', 'Farolito de papel', 'Paper lantern', kind='floor', sound='wool', seed=37, collide=False, light=11,
             particles=[{'type': 'minecraft:small_flame', 'pos': [8, 3.6, 8], 'rate': 0.05}])
    papel = stone(Ramp('#d89a4a', '#e8b060', '#f2c878', '#fadc98'), scale=2.5, seed=37)
    papel.emit = True
    p.box(4.5, 0, 4.5, 11.5, 9, 11.5, papel)
    p.box(4.3, 8.5, 4.3, 11.7, 9.3, 11.7, stone(Ramp('#b8803a', '#c8904a'), scale=2, seed=38))
    for x in (6, 8, 10):
        p.box(x - 0.5, 4, 4.4, x + 0.5, 5, 4.5, solid('#8a4a1a'))
    return p


def papel_picado():
    p = Prop('papel_picado', 'Papel picado navideño', 'Christmas papel picado', kind='hang', sound='wool', seed=39, collide=False)
    p.box(0, 15, 7.8, 16, 15.4, 8.2, solid('#3a3a3a'))
    for i, c in enumerate(('#e8303a', '#2ea84a', '#f2c230', '#3a7ae0')):
        x0 = i * 4 + 0.3
        p.box(x0, 9, 7.9, x0 + 3.4, 15, 8.1, cloth(c, 0.05, 39 + i))
    return p


def pinata_adorno():
    p = Prop('pinata_adorno', 'Piñata de estrella (adorno)', 'Star piñata (decoration)', kind='hang', sound='wool', seed=40, collide=False)
    centro = cloth('#f2c230', 0.12, 40)
    p.box(5, 5, 5, 11, 11, 11, centro)
    picos = [('#e8303a', (8, 11, 8), (2, 5, 2)), ('#2ea84a', (8, 5, 8), (2, -4, 2)), ('#3a7ae0', (11, 8, 8), (4, 2, 2)),
             ('#d64ab0', (5, 8, 8), (-4, 2, 2)), ('#f07a1c', (8, 8, 11), (2, 2, 4)), ('#8a3ad6', (8, 8, 5), (2, 2, -4))]
    for c, (x, y, z), (dx, dy, dz) in picos:
        x0, x1 = sorted((x - abs(dx) / 2 if dx in (2, -2) else x, x + dx if dx not in (2, -2) else x + abs(dx) / 2))
        y0, y1 = sorted((y - abs(dy) / 2 if dy in (2, -2) else y, y + dy if dy not in (2, -2) else y + abs(dy) / 2))
        z0, z1 = sorted((z - abs(dz) / 2 if dz in (2, -2) else z, z + dz if dz not in (2, -2) else z + abs(dz) / 2))
        p.box(max(0, x0), max(0, y0), max(0, z0), min(16, x1), min(16, y1), min(16, z1), cloth(c, 0.1, 41))
    p.box(7.6, 15, 7.6, 8.4, 16, 8.4, solid('#8a6a3a'))
    for x, z in ((6, 6), (10, 10)):
        p.box(x - 0.3, 1, z - 0.3, x + 0.3, 5, z + 0.3, cloth('#e8303a', 0.05, 42))
    return p


def reno_adorno():
    p = Prop('reno_adorno', 'Reno de adorno', 'Reindeer decoration', kind='floor', sound='wood', seed=43, collide=True,
             shape=[[2, 0, 5, 14, 14, 11]])
    piel = cloth('#8a5a30', 0.1, 43)
    p.box(3, 5, 6, 13, 10, 10, piel)
    for x in (3.5, 11):
        for z in (6.2, 8.4):
            p.box(x, 0, z, x + 1.4, 5, z + 1.4, piel)
    p.box(11, 8, 6.5, 14, 13, 9.5, piel)
    p.box(13.6, 10, 7.4, 14.6, 11.2, 8.6, brillo('#ff2a2a'))
    p.box(12.6, 11.5, 6.4, 13.2, 12.3, 6.6, CARBON)
    p.box(12.6, 11.5, 9.4, 13.2, 12.3, 9.6, CARBON)
    cuerno = MADERA_CLARA
    for z in (6.8, 8.8):
        p.box(11.5, 13, z, 12.2, 15.5, z + 0.6, cuerno)
        p.box(10.5, 14.5, z, 12.2, 15.2, z + 0.6, cuerno)
    p.box(10.5, 9.8, 6.3, 11.6, 10.8, 9.7, ROJO)
    p.box(2.5, 9, 7.4, 3.5, 10.5, 8.6, BLANCO)
    return p


# ---------------------------------------------------------------- el nacimiento (como los de México: con heno, musgo y los Reyes)

PIEL = cloth('#d8a070', weave=0.04, seed=70)
PIEL_MORENA = cloth('#8a5634', weave=0.04, seed=71)
PAJA = cloth('#c8a44a', weave=0.18, seed=72, fold=0.1)
BARBA_BLANCA = cloth('#ece8e0', weave=0.2, seed=73)
BARBA_CAFE = cloth('#5a3a22', weave=0.2, seed=74)
LANA = stone(Ramp('#d8d2c4', '#e6e0d2', '#f2eee4', '#fbf8f2'), scale=4, seed=75)


def pesebre():
    p = Prop('pesebre', 'Pesebre con el Niño', 'Manger with the Child', kind='floor', sound='wood', seed=70, collide=True,
             shape=[[2, 0, 4, 14, 9, 12]])
    # patas en cruz y el cajón
    for x in (2.5, 12):
        p.box(x, 0, 5, x + 1.5, 6, 6.2, MADERA)
        p.box(x, 0, 9.8, x + 1.5, 6, 11, MADERA)
    p.box(2.5, 5, 4.5, 13.5, 6, 11.5, MADERA)
    p.box(2.5, 6, 4, 13.5, 8, 4.8, MADERA)
    p.box(2.5, 6, 11.2, 13.5, 8, 12, MADERA)
    p.box(2, 6, 4, 2.8, 8.5, 12, MADERA)
    p.box(13.2, 6, 4, 14, 8.5, 12, MADERA)
    p.box(2.8, 6, 4.8, 13.2, 7.4, 11.2, PAJA)
    for x, z in ((3.2, 4.2), (6.5, 11.4), (11.5, 4.1), (9, 11.5)):
        p.box(x, 7.4, z, x + 1.6, 8.6, z + 0.5, PAJA)
    # el Niño envuelto en pañales
    p.box(5, 7.4, 6.4, 11, 9.2, 9.6, BLANCO)
    p.box(10.6, 7.6, 6.6, 12.6, 9.8, 9.4, PIEL)
    p.box(10.4, 9.4, 6.4, 12.8, 10, 9.6, cloth('#6a4426', 0.1, 76))
    p.box(4.6, 7.6, 6.2, 5.4, 9.4, 9.8, cloth('#e8dcc4', 0.1, 77))
    p.box(12.65, 8.4, 7.4, 12.75, 8.8, 7.8, CARBON)
    p.box(12.65, 8.4, 8.2, 12.75, 8.8, 8.6, CARBON)
    return p


def figura(pid, es, en, seed, cuerpo, manto, piel=PIEL, barba=None, cabeza=None, baston=False, arrodillada=False, regalo=None,
           corona=None):
    """Una figura de nacimiento de pie (o hincada) que mira al frente (z bajo): túnica, manto, cara, manos y lo que cargue."""
    p = Prop(pid, es, en, kind='floor', sound='wood', seed=seed, collide=True, shape=[[4, 0, 4, 12, 15, 12]])
    alto = 9 if arrodillada else 0
    base = 1
    p.box(4, 0, 4, 12, base, 12, MADERA_CLARA)
    if arrodillada:
        p.box(4.5, base, 4.5, 11.5, 4.5, 11.5, cuerpo)        # las rodillas bajo la túnica
        p.box(5, 4.5, 6, 11, 9, 11, cuerpo)
        p.box(4.6, base, 8, 11.4, 9.5, 11.6, manto)
        y = 9
    else:
        p.box(5, base, 5.5, 11, 10, 10.5, cuerpo)
        p.box(4.8, base, 8.5, 11.2, 10.5, 11, manto)
        p.box(4.6, 6, 5.4, 5.4, 10.5, 10.6, manto)
        p.box(10.6, 6, 5.4, 11.4, 10.5, 10.6, manto)
        y = 10
    # brazos y manos juntas al frente (o cargando algo)
    p.box(4.4, y - 3.5, 6, 5.6, y, 9, manto)
    p.box(10.4, y - 3.5, 6, 11.6, y, 9, manto)
    if regalo is not None:
        p.box(6, y - 3.2, 3.6, 10, y - 0.6, 6, regalo)
        p.box(7.6, y - 0.6, 4.4, 8.4, y + 0.2, 5.2, ORO)
        p.box(5.4, y - 3, 5.4, 6.4, y - 1.6, 6.2, piel)
        p.box(9.6, y - 3, 5.4, 10.6, y - 1.6, 6.2, piel)
    else:
        p.box(7.2, y - 2.6, 4.6, 8.8, y - 0.8, 6, piel)
    # cuello, cara y cabeza
    p.box(6.2, y, 6.2, 9.8, y + 4, 9.8, piel)
    p.box(6.6, y + 2.4, 6.1, 7.4, y + 3, 6.2, CARBON)
    p.box(8.6, y + 2.4, 6.1, 9.4, y + 3, 6.2, CARBON)
    if barba is not None:
        p.box(6.2, y - 0.6, 5.8, 9.8, y + 1.8, 7, barba)
        p.box(6.2, y + 3.4, 6.6, 9.8, y + 4.4, 9.8, barba)
    if cabeza is not None:
        p.box(5.8, y + 2, 6.8, 10.2, y + 4.8, 10.2, cabeza)
        p.box(6, y + 3.8, 6, 10, y + 4.8, 6.8, cabeza)
    if corona is not None:
        p.box(6, y + 4, 6, 10, y + 5, 10, corona)
        for x, z in ((6, 6), (9.2, 6), (6, 9.2), (9.2, 9.2), (7.6, 6)):
            p.box(x, y + 5, z, x + 0.8, y + 6, z + 0.8, corona)
        p.box(7.6, y + 4.2, 5.8, 8.4, y + 5, 6, brillo('#e02a3a'))
    if baston:
        p.box(12, base, 7, 12.8, 15, 7.8, MADERA)
        p.box(11.6, 14, 7, 12.8, 14.8, 7.8, MADERA)
    return p


def borrego():
    p = Prop('borreguito', 'Borreguito de nacimiento', 'Nativity lamb', kind='floor', sound='wool', seed=78, collide=True,
             shape=[[3, 0, 5, 13, 9, 11]])
    p.box(4, 3, 5.5, 11.5, 8, 10.5, LANA)
    for x in (4.6, 10):
        for z in (6, 9):
            p.box(x, 0, z, x + 1, 3, z + 1, NEGRO)
    p.box(11, 5, 6.5, 14, 8.5, 9.5, NEGRO)
    p.box(11.2, 7.6, 6.3, 13.4, 9, 9.7, LANA)
    p.box(11.4, 7, 5.6, 12.4, 7.8, 6.4, NEGRO)
    p.box(11.4, 7, 9.6, 12.4, 7.8, 10.4, NEGRO)
    p.box(3.2, 6, 7.4, 4, 7.5, 8.6, LANA)
    return p


def animal(pid, es, en, seed, pelo, cabeza_pelo, cuernos=None, orejas=None, nariz=None, mancha=None):
    p = Prop(pid, es, en, kind='floor', sound='wood', seed=seed, collide=True, shape=[[1, 0, 4, 15, 13, 12]])
    p.box(2.5, 4.5, 4.8, 12, 10, 11.2, pelo)
    if mancha is not None:
        p.box(5, 6, 4.7, 8.5, 9, 11.3, mancha)
    for x in (3, 10.4):
        for z in (5.4, 9.2):
            p.box(x, 0, z, x + 1.6, 4.5, z + 1.4, pelo)
            p.box(x, 0, z, x + 1.6, 0.8, z + 1.4, CARBON)
    p.box(11, 8, 5.8, 14.5, 12, 10.2, cabeza_pelo)
    p.box(13.6, 8, 6.4, 15.4, 10, 9.6, nariz or cabeza_pelo)
    p.box(13.2, 10.6, 5.7, 13.8, 11.2, 6.0, CARBON)
    p.box(13.2, 10.6, 10.0, 13.8, 11.2, 10.3, CARBON)
    if cuernos is not None:
        p.box(11.6, 11.4, 4, 12.6, 12.4, 12, cuernos)
        p.box(11.6, 12.4, 3.6, 12.6, 14, 4.6, cuernos)
        p.box(11.6, 12.4, 11.4, 12.6, 14, 12.4, cuernos)
    if orejas is not None:
        p.box(11.4, 11.5, 6.2, 12.4, 15.5, 7.4, orejas)
        p.box(11.4, 11.5, 8.6, 12.4, 15.5, 9.8, orejas)
    p.box(1.4, 7, 7.5, 2.6, 9.5, 8.5, pelo)
    return p


def estrella_belen():
    p = Prop('estrella_belen', 'Estrella de Belén', 'Star of Bethlehem', kind='hang', sound='metal', seed=79, collide=False, light=13)
    luz = brillo('#ffe27a')
    p.box(7.6, 9, 7.6, 8.4, 16, 8.4, solid('#9a8a5a'))
    p.box(7, 3, 7.4, 9, 9, 8.6, luz)
    p.box(5, 5, 7.4, 11, 7, 8.6, luz)
    p.box(5.6, 5.5, 7.5, 10.4, 6.5, 8.5, luz, rot=('z', 45, (8, 6, 8)))
    p.box(5.6, 5.5, 7.5, 10.4, 6.5, 8.5, luz, rot=('z', -45, (8, 6, 8)))
    p.box(6.6, 4.6, 7.3, 9.4, 7.4, 8.7, brillo('#fff2b8'))
    p.box(7.4, 0.5, 7.6, 8.6, 3, 8.4, brillo('#fff2b8'))
    p.box(3.5, 5.6, 7.6, 5, 6.4, 8.4, brillo('#fff2b8'))
    p.box(11, 5.6, 7.6, 12.5, 6.4, 8.4, brillo('#fff2b8'))
    return p


def portal_belen():
    p = Prop('portal_belen', 'Portal de Belén', 'Nativity stable', kind='floor', sound='wood', seed=80, collide=True,
             shape=[[0, 0, 12, 16, 16, 16]])
    p.box(0.5, 0, 0.5, 2, 13, 2, MADERA)
    p.box(14, 0, 0.5, 15.5, 13, 2, MADERA)
    p.box(0.5, 0, 13, 15.5, 11, 15.5, MADERA_CLARA)
    for y in (2.5, 5.5, 8.5):
        p.box(0.4, y, 12.8, 15.6, y + 0.4, 13.1, MADERA)
    p.box(0.5, 0, 2, 1.5, 11, 13, MADERA_CLARA)
    p.box(14.5, 0, 2, 15.5, 11, 13, MADERA_CLARA)
    p.box(0, 13, 0, 16, 14, 16, MADERA)
    p.box(-0.5, 14, -0.5, 16.5, 15.5, 16.5, PAJA)
    p.box(2, 15.5, 2, 14, 16, 14, PAJA)
    for x in (1, 5, 9, 13):
        p.box(x, 12.2, -0.6, x + 1.4, 14, 0, PAJA)
    p.box(7.2, 11.4, 0.2, 8.8, 13, 0.6, brillo('#ffe27a'))
    p.box(0.5, 0, 2, 15.5, 0.5, 15, PAJA)
    return p


def t_musgo(seed):
    x, y = grid()
    t = ruido(x, y, 2.0, seed, 3)
    rgb = Ramp('#2c4a1a', '#3c6224', '#4e7a2c', '#6a9238')(t)
    heno = (ruido(x, y, 1.0, seed + 5, 2) > 0.72) & (((x + y * 3) % 7) < 2)
    rgb = np.where(heno[..., None], col('#b8b4a0'), rgb)
    return img(rgb)


def t_angel_nieve(seed):
    """Un ángel de nieve: la huella de alguien que se acostó en la nieve y movió brazos y piernas."""
    x, y = grid()
    t = ruido(x, y, 2.0, seed, 3)
    rgb = Ramp('#dfe6ee', '#ebf0f5', '#f6f9fc', '#ffffff')(t)
    cx = x - 15.5
    cabeza = (cx ** 2 + (y - 5.5) ** 2) < 3.3 ** 2
    cuerpo = (np.abs(cx) < 2.6 + (y - 10) * 0.12) & (y >= 10) & (y <= 20)
    ang = np.arctan2(y - 12, np.abs(cx))
    rr = np.sqrt(cx ** 2 + (y - 12) ** 2)
    alas = (rr < 11.5) & (rr > 4.2) & (ang > -0.8) & (ang < 0.38) & (np.abs(cx) > 2.6)
    piernas_r = np.sqrt(cx ** 2 + (y - 18) ** 2)
    piernas = (piernas_r < 12.5) & (y > 18) & (np.abs(np.arctan2(cx, y - 18)) < 0.62)
    huella = cabeza | cuerpo | alas | piernas
    sombra = Ramp('#b8c6d8', '#c4d2e2', '#ccd8e6')(t)
    rgb = np.where(huella[..., None], sombra, rgb)
    # la orilla de la huella, un poco más oscura
    borde = huella & ~(np.roll(huella, 1, 0) & np.roll(huella, -1, 0) & np.roll(huella, 1, 1) & np.roll(huella, -1, 1))
    rgb = np.where(borde[..., None], col('#a8b8cc'), rgb)
    return img(rgb)


def nacimiento():
    prop(pesebre())
    prop(figura('figura_maria', 'Virgen María (nacimiento)', 'Mary (nativity)', 81, cloth('#d88a98', 0.06, 81), cloth('#2a5aa8', 0.06, 82),
                cabeza=cloth('#2a5aa8', 0.06, 82), arrodillada=True))
    prop(figura('figura_jose', 'San José (nacimiento)', 'Joseph (nativity)', 83, cloth('#7a4e2a', 0.08, 83), cloth('#a8862e', 0.08, 84),
                barba=BARBA_CAFE, cabeza=cloth('#5a3a22', 0.1, 85), baston=True))
    prop(figura('figura_pastor', 'Pastor (nacimiento)', 'Shepherd (nativity)', 86, cloth('#8a7a5a', 0.15, 86), cloth('#5a6a3a', 0.12, 87),
                barba=BARBA_CAFE, cabeza=cloth('#c8b48a', 0.1, 88), baston=True))
    prop(figura('rey_melchor', 'Rey Melchor', 'King Melchior', 89, cloth('#e8e0d0', 0.06, 89), cloth('#b01c2a', 0.06, 90),
                barba=BARBA_BLANCA, corona=ORO, regalo=ORO))
    prop(figura('rey_gaspar', 'Rey Gaspar', 'King Caspar', 91, cloth('#2a6a3a', 0.06, 91), cloth('#d0a030', 0.06, 92),
                barba=BARBA_CAFE, corona=ORO, regalo=solid('#7a4a2a')))
    prop(figura('rey_baltasar', 'Rey Baltasar', 'King Balthazar', 93, cloth('#e8c040', 0.06, 93), cloth('#5a2a8a', 0.06, 94),
                piel=PIEL_MORENA, cabeza=cloth('#f2f0ec', 0.08, 95), corona=ORO, regalo=PLATA))
    prop(borrego())
    prop(animal('figura_burro', 'Burrito de nacimiento', 'Nativity donkey', 96, cloth('#8a8a8e', 0.15, 96), cloth('#7a7a80', 0.15, 97),
                orejas=cloth('#6a6a70', 0.15, 98), nariz=cloth('#d8d4cc', 0.1, 99)))
    prop(animal('figura_buey', 'Buey de nacimiento', 'Nativity ox', 100, cloth('#6a4428', 0.15, 100), cloth('#5a3a22', 0.15, 101),
                cuernos=CREMA, nariz=cloth('#c8a890', 0.1, 102), mancha=cloth('#efe8dc', 0.15, 103)))
    prop(estrella_belen())
    prop(portal_belen())
    alfombra('musgo_nacimiento', 'Musgo con heno', 'Moss with Spanish moss', lambda s: t_musgo(s))
    alfombra('angel_nieve', 'Ángel de nieve', 'Snow angel', lambda s: t_angel_nieve(s))


# ---------------------------------------------------------------- el catálogo

def catalogo():
    # bloques enteros
    cubo('bloque_caramelo', 'Bloque de caramelo', 'Candy cane block', lambda s: t_caramelo(s), sonido='stone')
    cubo('bloque_caramelo_verde', 'Bloque de caramelo de menta', 'Mint candy block', lambda s: t_caramelo(s, '#1e8a3c'))
    cubo('bloque_jengibre', 'Bloque de pan de jengibre', 'Gingerbread block', lambda s: t_jengibre(s), variantes=3, sonido='wood')
    cubo('ladrillo_jengibre', 'Ladrillos de jengibre', 'Gingerbread bricks', lambda s: t_jengibre_ladrillo(s), sonido='wood')
    cubo('glaseado', 'Glaseado', 'Icing', lambda s: t_glaseado(s), sonido='wool', dureza=0.8)
    cubo('glaseado_chispas', 'Glaseado con chispas', 'Sprinkled icing', lambda s: t_glaseado(s, True), sonido='wool', dureza=0.8)
    cubo('nieve_brillante', 'Nieve brillante', 'Sparkling snow', lambda s: t_nieve(s), variantes=3, sonido='wool', dureza=0.4, luz=6)
    cubo('bloque_luces', 'Bloque de luces navideñas', 'Christmas lights block', lambda s: Image.fromarray(t_luces_anim(s), 'RGBA'),
         variantes=3, sonido='glass', dureza=0.8, luz=14)
    for i in range(3):
        with open(os.path.join(A, 'textures/block', f'bloque_luces_{i}.png.mcmeta'), 'w') as fh:
            json.dump({'animation': {'frametime': 12}}, fh)
    cubo('papel_tapiz_navidad', 'Papel tapiz navideño', 'Christmas wallpaper', lambda s: t_papel_tapiz(s), sonido='wool')
    cubo('papel_tapiz_dorado', 'Papel tapiz verde y dorado', 'Green and gold wallpaper', lambda s: t_papel_tapiz(s, '#174a26', '#e0b23a'),
         sonido='wool')
    cubo('tela_cuadros', 'Tela de cuadros navideña', 'Christmas plaid', lambda s: t_cuadros(s), sonido='wool', dureza=0.8)
    cubo('madera_cabana', 'Troncos de cabaña', 'Cabin logs', lambda s: t_cabana(s), variantes=2, sonido='wood')
    cubo('terciopelo_rojo', 'Terciopelo de Santa', "Santa's velvet", lambda s: t_terciopelo(s), sonido='wool', dureza=0.8)
    cubo('bloque_regalo_rojo', 'Bloque de regalo rojo', 'Red gift block', lambda s: t_regalo(s), sonido='wool', dureza=0.8)
    cubo('bloque_regalo_verde', 'Bloque de regalo verde', 'Green gift block', lambda s: t_regalo(s, '#1e6a32', '#d8202c', '#f2f2ea'),
         sonido='wool', dureza=0.8)
    cubo('bloque_regalo_azul', 'Bloque de regalo azul', 'Blue gift block', lambda s: t_regalo(s, '#2a4fb0', '#e8e8f0', '#f0c43a'),
         sonido='wool', dureza=0.8)
    vidrio('hielo_copo', 'Hielo con copo de nieve', 'Snowflake ice', lambda s: t_hielo(s))
    alfombra('alfombra_navidad', 'Alfombra navideña', 'Christmas rug', lambda s: t_alfombra_navidad(s))
    alfombra('alfombra_nieve', 'Alfombra de nieve', 'Snow rug', lambda s: t_alfombra_nieve(s))
    # adornos
    prop(arbol('arbol_navidad', 'Árbol de Navidad', 'Christmas tree'))
    prop(arbol('arbol_nevado', 'Árbol de Navidad nevado', 'Snowy Christmas tree', nevado=True, colores=['#3a7ae0', '#c8ccd4', '#e8f4ff'],
               seed=7))
    prop(arbol('arbol_dorado', 'Árbol de Navidad dorado', 'Golden Christmas tree', colores=['#e8b830', '#f4e2a0', '#d42030'], seed=9))
    prop(pino_maceta())
    for pid, es, en, c, s in (('esfera_roja', 'Esfera roja', 'Red bauble', '#d42030', 51), ('esfera_dorada', 'Esfera dorada', 'Gold bauble',
                              '#e8b830', 52), ('esfera_azul', 'Esfera azul', 'Blue bauble', '#2a6ae0', 53),
                              ('esfera_plata', 'Esfera plateada', 'Silver bauble', '#c8ccd4', 54), ('esfera_verde', 'Esfera verde',
                              'Green bauble', '#2ea84a', 55)):
        prop(esfera(pid, es, en, c, s))
    prop(guirnalda())
    prop(luces_colgantes())
    prop(corona())
    prop(calcetin('calcetin_rojo', 'Calcetín navideño rojo', 'Red Christmas stocking', ROJO, 61))
    prop(calcetin('calcetin_verde', 'Calcetín navideño verde', 'Green Christmas stocking', VERDE, 62))
    prop(calcetin('calcetin_rayas', 'Calcetín navideño de rayas', 'Striped Christmas stocking', rayas('#d8202c', '#f4efe8', 1.5), 63))
    prop(muerdago())
    prop(nochebuena())
    prop(corona_adviento())
    prop(velas())
    for pid, es, en, pa, la, s, al, an in (('regalo_rojo', 'Regalo rojo', 'Red present', '#b81d2a', '#f0c43a', 71, 9, 10),
                                           ('regalo_verde', 'Regalo verde', 'Green present', '#1e6a32', '#d8202c', 72, 8, 11),
                                           ('regalo_azul', 'Regalo azul', 'Blue present', '#2a4fb0', '#e8e8f0', 73, 11, 9),
                                           ('regalo_dorado', 'Regalo dorado', 'Golden present', '#e0b23a', '#b81d2a', 74, 7, 12)):
        prop(regalo(pid, es, en, pa, la, s, al, an))
    prop(pila_regalos())
    prop(muneco())
    prop(cascanueces())
    prop(baston())
    prop(trineo())
    prop(campanas())
    prop(chimenea())
    prop(esfera_nieve())
    prop(galletas_leche())
    prop(plato_migajas())
    prop(chocolate())
    prop(ponche())
    prop(farolito())
    prop(papel_picado())
    prop(pinata_adorno())
    prop(reno_adorno())
    nacimiento()


# ---------------------------------------------------------------- bloques con código propio (no son de decoración)

def especiales():
    """La caja de regalo (envuelta y vacía) y el calendario de Adviento: los modelos salen del mismo kit, pero los bloques son clases
    propias (CajaRegalo, Calendario), así que no van al manifiesto; aquí se arreglan su estado de bloque y su botín."""
    llena = regalo('caja_regalo', 'Caja de regalo', 'Gift box', '#c8202e', '#f0c43a', 70)
    kit.write_prop(llena, ROOT)
    v = Prop('caja_regalo_vacia', 'Caja de regalo', 'Gift box', kind='floor', sound='wool', seed=71)
    pap = cloth('#c8202e', weave=0.08, seed=71)
    dentro = cloth('#f4ecd8', weave=0.05, seed=72)
    v.box(3, 0, 3, 13, 1, 13, pap)
    v.box(3, 1, 3, 13, 9, 4, pap)
    v.box(3, 1, 12, 13, 9, 13, pap)
    v.box(3, 1, 4, 4, 9, 12, pap)
    v.box(12, 1, 4, 13, 9, 12, pap)
    v.box(4, 1, 4, 12, 6, 12, dentro)
    v.box(5, 6, 6, 9, 8.5, 8, dentro, rot=('y', 22.5, (7, 7, 7)))
    kit.write_prop(v, ROOT)
    for sub in ('blockstates/caja_regalo_vacia.json', 'models/item/caja_regalo_vacia.json'):
        os.remove(os.path.join(A, sub))
    os.remove(os.path.join(ROOT, 'data', NS, 'loot_tables', 'blocks', 'caja_regalo_vacia.json'))
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    var = {}
    for f, y in rot.items():
        for ll, m in (('true', 'caja_regalo'), ('false', 'caja_regalo_vacia')):
            e = {'model': f'{NS}:block/{m}'}
            if y:
                e['y'] = y
            var[f'facing={f},llena={ll}'] = e
    dump('blockstates/caja_regalo.json', {'variants': var})
    botin = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:caja_regalo', 'functions': [
        {'function': 'minecraft:copy_nbt', 'source': 'block_entity', 'ops': [
            {'source': 'Contenido', 'target': 'BlockEntityTag.Contenido', 'op': 'replace'},
            {'source': 'De', 'target': 'BlockEntityTag.De', 'op': 'replace'},
            {'source': 'Para', 'target': 'BlockEntityTag.Para', 'op': 'replace'}]}]}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}
    json.dump(botin, open(os.path.join(ROOT, 'data', NS, 'loot_tables', 'blocks', 'caja_regalo.json'), 'w'), indent=1)
    # el calendario: una casita de madera con 24 puertitas de colores, techo nevado y estrella
    c = Prop('calendario_adviento', 'Calendario de Adviento', 'Advent calendar', kind='floor', sound='wood', seed=73, light=6)
    c.box(1, 0, 5, 15, 11, 11, MADERA_CLARA)
    colores = ['#c8202e', '#2a8a40', '#f0c43a', '#2a5ab0', '#e8e8f0', '#d6408a']
    for i in range(24):
        fila, col = divmod(i, 6)
        x0 = 2 + col * 2.1
        y0 = 8.4 - fila * 2.4
        c.box(x0, y0, 4.6, x0 + 1.6, y0 + 1.8, 5, cloth(colores[(i * 5 + fila) % 6], weave=0.04, seed=80 + i))
    c.box(0, 11, 4, 16, 12, 12, MADERA)
    c.box(1, 12, 4.5, 15, 13, 11.5, MADERA)
    c.box(3, 13, 5, 13, 14, 11, MADERA)
    c.box(0, 12, 4, 16, 12.6, 12, cloth('#f4f6fa', weave=0.04, seed=74))
    c.box(2, 13, 4.5, 14, 13.5, 11.5, cloth('#f4f6fa', weave=0.04, seed=75))
    c.box(7, 14, 7.5, 9, 16, 8.5, brillo('#ffd84a'))
    kit.write_prop(c, ROOT)
    # el buzón de Santa: rojo, sobre un poste de bastón de caramelo, con nieve encima y cartas asomándose
    b = Prop('buzon_santa', 'Buzón de Santa', "Santa's mailbox", kind='floor', sound='metal', seed=101)
    rojo = metal(Ramp('#5a0c12', '#8a141c', '#b81d2a', '#d83a44', '#f07a80'), streak=0.3, seed=102, sparkle=0.03)
    blanco_p = cloth('#f4f0e8', weave=0.04, seed=103)
    b.box(5, 0, 5, 11, 1, 11, MADERA)
    for k in range(9):
        b.box(6.5, k, 6.5, 9.5, k + 1, 9.5, rojo if k % 2 == 0 else blanco_p)
    b.box(3, 9, 3.5, 13, 14, 12.5, rojo)
    b.box(3.5, 14, 4, 12.5, 15, 12, rojo)
    b.box(4.5, 15, 4.5, 11.5, 15.5, 11.5, rojo)
    b.box(3.2, 15.2, 3.7, 12.8, 16, 12.3, NIEVE)
    b.box(3.0, 14.4, 3.5, 4.4, 15.4, 5.0, NIEVE)
    b.box(11.4, 14.6, 10.8, 13.0, 15.4, 12.5, NIEVE)
    b.box(5, 12, 3.3, 11, 12.8, 3.5, solid('#1a1012'))
    b.box(5.5, 9.8, 3.25, 10.5, 11.6, 3.5, ORO)
    b.box(6.8, 10.1, 3.1, 9.2, 11.3, 3.3, blanco_p)
    b.box(6.9, 10.5, 3.05, 8.0, 10.65, 3.12, solid('#c8202e'), rot=('z', -22.5, (8, 10.6, 3.1)))
    b.box(8.0, 10.5, 3.05, 9.1, 10.65, 3.12, solid('#c8202e'), rot=('z', 22.5, (8, 10.6, 3.1)))
    b.box(6.0, 12.3, 2.9, 9.6, 14.2, 3.4, cloth('#efe4c8', weave=0.05, seed=104), rot=('x', -22.5, (8, 12.3, 3.2)))
    b.box(7.6, 12.3, 3.1, 10.6, 13.6, 3.5, cloth('#f8f4ea', weave=0.05, seed=105), rot=('x', -22.5, (9, 12.3, 3.3)))
    b.box(13, 10, 9, 13.6, 15, 9.6, PLATA)
    b.box(13, 12.8, 5.6, 13.4, 15, 9, cloth('#f0c43a', weave=0.08, seed=106))
    b.box(5, 15.9, 8.5, 8, 16.4, 10.5, cloth('#1e6a32', weave=0.1, seed=107), rot=('y', 22.5, (6.5, 16, 9.5)))
    b.box(8, 15.9, 8.2, 11, 16.4, 10.2, cloth('#2a7a3a', weave=0.1, seed=108), rot=('y', -22.5, (9.5, 16, 9.2)))
    esferas(b, [(7.8, 16.6, 9.4)], colores=['#d42030'])
    kit.write_prop(b, ROOT)
    nombre('buzon_santa', 'Buzón de Santa', "Santa's mailbox")
    nombre('caja_regalo', 'Caja de regalo', 'Gift box')
    nombre('calendario_adviento', 'Calendario de Adviento', 'Advent calendar')


def modelos_sueltos():
    """Modelos que no son bloques: el trineo de madera para pasear (lo dibuja TrineoMaderaRenderer y sirve de icono de su objeto).
    Mide casi dos bloques de largo: va de z=-6 (la punta curva, adelante) a z=22; el centro queda en (8, 8)."""
    t = Prop('trineo_madera', 'Trineo de madera', 'Wooden sled', kind='floor', sound='wood', seed=91)
    hierro = metal(Ramp('#3a1414', '#6a1c1c', '#962626', '#c23a3a', '#e86a6a'), streak=0.5, seed=92, sparkle=0.05)
    tabla = [MADERA_CLARA, wood(Ramp('#7a5430', '#94683c', '#ad7e4a', '#c4945a'), seed=93)]
    for x in (1.5, 13.5):
        # el patín (rojo de metal) y su punta que se enrosca hacia arriba
        t.box(x, 0, -3, x + 1, 1, 21, hierro)
        t.box(x, 1, -5, x + 1, 2, -2.5, hierro, rot=('x', 22.5, (x + 0.5, 1, -3)))
        t.box(x, 2.2, -6.4, x + 1, 5.2, -5.4, hierro)
        t.box(x, 5.0, -5.8, x + 1, 6.0, -3.6, hierro)
        t.box(x, 4.2, -3.8, x + 1, 5.0, -2.8, hierro)
        t.box(x, 0.5, 20.6, x + 1, 2.2, 21.6, hierro)
        # los postes que sostienen la tabla
        for z in (0, 8.5, 17):
            t.box(x, 1, z, x + 1, 4, z + 1.2, MADERA)
    # la tabla de listones (dos tonos) y los travesaños
    z = -2.5
    k = 0
    while z < 19.5:
        t.box(0.5, 4, z, 15.5, 5, min(z + 2.6, 20), tabla[k % 2])
        z += 3.0
        k += 1
    for z in (0.2, 8.7, 17.2):
        t.box(1, 3.2, z, 15, 4, z + 0.8, MADERA)
    # el barandal de los lados, la barra de adelante (pintada) y el cordón para jalarlo
    for x in (0.5, 14.5):
        t.box(x, 5, -1, x + 1, 6.4, 19, MADERA)
        t.box(x, 5, 18.5, x + 1, 8, 19.5, MADERA)
    t.box(0.5, 5, -3, 15.5, 7, -1.6, cloth('#b81d2a', weave=0.06, seed=94))
    t.box(0.5, 7, -3.2, 15.5, 7.6, -1.4, ORO)
    t.box(1, 7.4, 18.5, 15, 8.4, 19.5, MADERA)
    t.box(3, 6.2, -3.6, 4, 7, -3.0, solid('#e8dcc0'))
    t.box(12, 6.2, -3.6, 13, 7, -3.0, solid('#e8dcc0'))
    t.box(3.2, 6.4, -5.5, 12.8, 6.9, -5.0, cloth('#efe2c4', weave=0.2, seed=95))
    t.box(3.2, 6.4, -5.2, 3.8, 6.9, -3.4, cloth('#efe2c4', weave=0.2, seed=95))
    t.box(12.2, 6.4, -5.2, 12.8, 6.9, -3.4, cloth('#efe2c4', weave=0.2, seed=95))
    # un cojín a cuadros, una bufanda verde echada y un cascabel
    cuadros = cloth('#8e1820', weave=0.25, seed=96)
    t.box(3, 5, 7.5, 13, 6, 17.5, cuadros)
    t.box(3.4, 6, 8, 12.6, 6.4, 17, cloth('#a8222c', weave=0.2, seed=97))
    t.box(10, 6.4, 14.5, 13.6, 6.9, 17.6, cloth('#1e6a32', weave=0.15, seed=98), rot=('y', 22.5, (12, 6.4, 16)))
    t.box(7.4, 6.0, -4.0, 8.6, 7.2, -2.8, ORO)
    kit.write_prop(t, ROOT)
    for sub in ('blockstates/trineo_madera.json',):
        os.remove(os.path.join(A, sub))
    os.remove(os.path.join(ROOT, 'data', NS, 'loot_tables', 'blocks', 'trineo_madera.json'))
    # el objeto usa el mismo modelo, más chico para que quepa en el inventario
    dump('models/item/trineo_madera.json', {'parent': f'{NS}:block/trineo_madera', 'display': {
        'gui': {'rotation': [30, 135, 0], 'translation': [0, 1, 0], 'scale': [0.42, 0.42, 0.42]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.3, 0.3, 0.3]},
        'fixed': {'rotation': [0, 90, 0], 'translation': [0, 0, 0], 'scale': [0.42, 0.42, 0.42]},
        'thirdperson_righthand': {'rotation': [75, 135, 0], 'translation': [0, 2.5, 0], 'scale': [0.3, 0.3, 0.3]},
        'firstperson_righthand': {'rotation': [0, 135, 0], 'translation': [0, 1, 0], 'scale': [0.3, 0.3, 0.3]},
        'firstperson_lefthand': {'rotation': [0, 135, 0], 'translation': [0, 1, 0], 'scale': [0.3, 0.3, 0.3]}}})


def paracaidas():
    """El paracaídas de los regalos que suelta Santa (lo dibuja RegaloCayendoRenderer arriba del regalo). Domo de gajos rojos y blancos
    de 40 px de ancho (de -12 a 28) entre y=22 y y=32, con cuatro cordones que bajan al regalo (cuyo moño queda en y≈11)."""
    t = Prop('paracaidas', 'Paracaídas de regalo', 'Gift parachute', kind='floor', sound='wool', seed=120)
    rojo = cloth('#c41e2a', weave=0.08, seed=121, fold=0.25)
    blanco = cloth('#f4f0ea', weave=0.08, seed=122, fold=0.25)
    verde = cloth('#2a8a40', weave=0.08, seed=123, fold=0.25)
    cordon = solid('#e8dcc0')
    # el domo: cuatro pisos que se van cerrando, en gajos alternados
    for (a, b, y0, y1) in ((-12, 28, 22, 24.5), (-9, 25, 24.5, 27.5), (-5, 21, 27.5, 30), (0, 16, 30, 31.6)):
        w = (b - a) / 5.0
        for k in range(5):
            x0, x1 = a + k * w, a + (k + 1) * w
            t.box(x0, y0, a, x1, y1, b, (rojo, blanco)[k % 2])
    t.box(5, 31.6, 5, 11, 32, 11, verde)
    # la orilla de abajo, un poquito más ancha (como tela que cuelga)
    for (x0, z0, x1, z1) in ((-12.4, -12.4, 28.4, -11.6), (-12.4, 27.6, 28.4, 28.4), (-12.4, -11.6, -11.6, 27.6), (27.6, -11.6, 28.4, 27.6)):
        t.box(x0, 21.2, z0, x1, 22.4, z1, rojo)
    # cuatro cordones a 45 grados, de la orilla del domo al moño del regalo
    largo = 17.0
    for (cx, cz, eje, ang) in ((-2.5, 8, 'z', -45), (18.5, 8, 'z', 45), (8, -2.5, 'x', 45), (8, 18.5, 'x', -45)):
        cy = 16.5
        t.box(cx - 0.25, cy - largo / 2, cz - 0.25, cx + 0.25, cy + largo / 2, cz + 0.25, cordon, rot=(eje, ang, (cx, cy, cz)))
    kit.write_prop(t, ROOT)
    os.remove(os.path.join(A, 'blockstates/paracaidas.json'))
    os.remove(os.path.join(ROOT, 'data', NS, 'loot_tables', 'blocks', 'paracaidas.json'))
    for d in ('models/item/paracaidas.json',):
        if os.path.exists(os.path.join(A, d)):
            os.remove(os.path.join(A, d))


def main():
    catalogo()
    especiales()
    modelos_sueltos()
    paracaidas()
    os.makedirs(os.path.join(ROOT, 'data', NS), exist_ok=True)
    with open(os.path.join(ROOT, 'data', NS, 'deco.json'), 'w', encoding='utf-8') as f:
        json.dump({'blocks': ficha}, f, indent=1, ensure_ascii=False)
    ld = os.path.join(A, 'lang')
    for code in ('es_mx', 'es_es', 'en_us'):
        p = os.path.join(ld, code + '.json')
        d = json.load(open(p, encoding='utf-8'))
        for bid, (es, en) in nombres.items():
            d[f'block.{NS}.{bid}'] = en if code == 'en_us' else es
        d[f'itemGroup.{NS}.deco'] = 'Christmas: decoration' if code == 'en_us' else 'Navidad: decoración'
        json.dump(d, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    print(len(ficha), 'bloques ->', ROOT)


if __name__ == '__main__':
    main()
