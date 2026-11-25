#!/usr/bin/env python3
"""El arma de la caja de música (capítulo 3): un revólver viejo en la punta de un mástil de madera que baja del techo.

Escribe geo/arma_techo.geo.json, animations/arma_techo.animation.json y textures/entity/arma_techo.png.
La entidad está en el techo (y = 0 del modelo); el mástil cuelga 5.5 bloques y el cañón apunta hacia el frente (-z).
Huesos: mastil (sube y baja), giro, codo (se inclina: el pitch), arma (el culatazo) y fogonazo (brilla; solo se ve al disparar).

Uso: python3 herramientas/arma_techo.py [--vista carpeta]
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista')
TW, TH = 128, 128
HUESOS = []
CUBOS = []
Y = -92  # la altura del cañón (píxeles bajo el techo)


def hueso(nombre, padre, pivote, giro=(0, 0, 0)):
    HUESOS.append((nombre, padre, list(pivote), list(giro)))


def cubo(h, o, t, mat, inflar=0.0, giro=None, pivote=None):
    c = {'hueso': h, 'o': list(o), 't': list(t), 'mat': mat, 'inflar': inflar}
    if giro:
        c['giro'] = list(giro)
        c['pivote'] = list(pivote)
    CUBOS.append(c)


def modelo():
    hueso('raiz', None, (0, 0, 0))
    hueso('mastil', 'raiz', (0, 0, 0))
    # el mástil: una viga vieja de madera con abrazaderas de latón, y la polea de donde cuelga
    cubo('mastil', (-1.5, Y + 4, -1.5), (3, 92, 3), 'madera')
    for y in range(Y + 14, 4, 22):
        cubo('mastil', (-1.5, y, -1.5), (3, 2, 3), 'laton', inflar=0.35)
    cubo('mastil', (-3, -2, -3), (6, 4, 6), 'hierro')
    cubo('mastil', (-0.5, -8, -2.4), (1, 6, 0.6), 'cuerda')
    # el nudo de abajo (donde gira el arma)
    cubo('mastil', (-2.5, Y + 1, -2.5), (5, 4, 5), 'laton')
    hueso('giro', 'mastil', (0, Y + 1, 0))
    cubo('giro', (-2, Y - 0.5, -2), (4, 1.5, 4), 'hierro')
    hueso('codo', 'giro', (0, Y, 0))
    # la horquilla que sostiene el revólver (dos placas a los lados)
    cubo('codo', (-2.6, Y - 2.5, -1.5), (0.6, 3.5, 3), 'hierro')
    cubo('codo', (2.0, Y - 2.5, -1.5), (0.6, 3.5, 3), 'hierro')
    hueso('arma', 'codo', (0, Y - 1, 0))
    # el revólver: cañón largo hacia el frente (-z), el tambor, el armazón, el martillo, el gatillo y la cacha de madera
    cubo('arma', (-0.8, Y - 1.8, -15), (1.6, 1.6, 11), 'metal')
    cubo('arma', (-0.55, Y - 0.4, -14.6), (1.1, 0.6, 1), 'metal')          # la mira
    cubo('arma', (-0.5, Y - 2.4, -9), (1, 0.6, 5), 'metal_osc')            # la varilla bajo el cañón
    cubo('arma', (-1.7, Y - 2.9, -4.2), (3.4, 3.4, 4.2), 'tambor')          # el tambor
    cubo('arma', (-1.1, Y - 3.1, -0.1), (2.2, 3.6, 3.4), 'metal')           # el armazón
    cubo('arma', (-0.45, Y - 0.2, 2.6), (0.9, 1.6, 1.2), 'metal_osc', giro=(-25, 0, 0), pivote=(0, Y, 3))   # el martillo
    cubo('arma', (-0.3, Y - 5.0, 0.6), (0.6, 1.8, 0.6), 'metal_osc')        # el gatillo
    cubo('arma', (-0.3, Y - 5.6, -0.4), (0.6, 0.5, 2.4), 'metal_osc')       # el guardamonte
    cubo('arma', (-1.0, Y - 9.0, 1.8), (2.0, 6.5, 2.8), 'cacha', giro=(22, 0, 0), pivote=(0, Y - 3, 2.5))  # la cacha
    cubo('arma', (-1.15, Y - 9.6, 2.0), (2.3, 1.0, 2.9), 'laton', giro=(22, 0, 0), pivote=(0, Y - 3, 2.5))
    hueso('fogonazo', 'arma', (0, Y - 1, -15))
    cubo('fogonazo', (-1.8, Y - 2.8, -21), (3.6, 3.6, 6), 'fuego')
    cubo('fogonazo', (-3.5, Y - 1.1, -18), (7, 0.2, 2.5), 'fuego_claro')
    cubo('fogonazo', (-0.1, Y - 4.6, -18), (0.2, 7, 2.5), 'fuego_claro')
    cubo('fogonazo', (-1, Y - 2, -24), (2, 2, 3), 'fuego_claro')


# ---------------------------------------------------------------- textura

R = np.random.RandomState(7)


def hexc(c):
    c = c.lstrip('#')
    return [int(c[i:i + 2], 16) for i in (0, 2, 4)]


def mat(nombre, w, h, cara, s):
    r = np.random.RandomState(s)
    yy, xx = np.mgrid[0:h, 0:w]
    if nombre == 'madera':
        # vetas a lo largo (la viga cuelga: vetas verticales en los lados), vieja y oscura
        base = np.array(hexc('#4a2e1a'), float)
        vet = np.sin(xx * 1.7 + r.rand() * 6 + np.sin(yy * 0.13) * 1.5) * 0.5 + 0.5
        n = r.rand(h, w) * 0.18
        f = 0.75 + vet[..., None] * 0.35 + n[..., None]
        rgb = base * f
        if cara in ('up', 'down'):
            rgb = np.array(hexc('#5a3a22'), float) * (0.8 + 0.3 * np.sin(np.hypot(xx - w / 2, yy - h / 2) * 2)[..., None])
    elif nombre == 'laton':
        base = np.array(hexc('#a07c34'), float)
        rgb = base * (0.8 + 0.35 * r.rand(h, w)[..., None])
        rgb[0, :] = np.array(hexc('#d8b45a'))
    elif nombre == 'hierro':
        rgb = np.array(hexc('#3a3a40'), float) * (0.8 + 0.4 * r.rand(h, w)[..., None])
        oxido = r.rand(h, w) > 0.8
        rgb[oxido] = np.array(hexc('#6a3a1e'))
    elif nombre == 'cuerda':
        rgb = np.array(hexc('#8a7a5a'), float) * (0.75 + 0.3 * ((xx + yy) % 2)[..., None])
    elif nombre in ('metal', 'metal_osc', 'tambor'):
        base = np.array(hexc('#2c3038' if nombre != 'metal_osc' else '#1c1e22'), float)
        brillo = (yy == 0) * 0.6
        rgb = base * (0.9 + 0.25 * r.rand(h, w)[..., None] + brillo[..., None])
        if nombre == 'tambor' and cara in ('north', 'south', 'east', 'west'):
            rgb[:, ::2] *= 0.6   # las ranuras del tambor
        if cara in ('north',) and nombre == 'metal' and w <= 2:
            rgb[:] = np.array(hexc('#050505'))   # la boca del cañón
    elif nombre == 'cacha':
        base = np.array(hexc('#6a2a1a'), float)
        rgb = base * (0.8 + 0.3 * np.sin(yy * 2.2 + xx)[..., None] * 0.5 + 0.2 * r.rand(h, w)[..., None])
        rgb[(xx + yy * 2) % 3 == 0] *= 0.75   # el rayado de la cacha
    elif nombre == 'fuego':
        d = np.hypot((xx - w / 2 + 0.5) / max(1, w / 2), (yy - h / 2 + 0.5) / max(1, h / 2))
        rgb = np.stack([np.full_like(d, 255), 230 - 120 * d, 120 - 110 * d], -1)
    elif nombre == 'fuego_claro':
        rgb = np.stack([np.full((h, w), 255.0), np.full((h, w), 245.0), np.full((h, w), 200.0)], -1)
    else:
        rgb = np.full((h, w, 3), 200.0)
    a = np.full((h, w), 255, np.uint8)
    return np.dstack([np.clip(rgb, 0, 255).astype(np.uint8), a])


def caras_caja(u, v, w, h, d):
    return {
        'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
        'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
        'west': (u + d + w, v + d, d, h), 'south': (u + d + w + d, v + d, w, h),
    }


def empacar():
    tam = []
    for i, c in enumerate(CUBOS):
        w, h, d = (max(1, int(math.ceil(x))) for x in c['t'])
        tam.append((i, 2 * (w + d), d + h, w, h, d))
    tam.sort(key=lambda r: -r[2])
    x = y = fila = 0
    for i, ww, hh, w, h, d in tam:
        if x + ww > TW:
            x, y, fila = 0, y + fila, 0
        if y + hh > TH:
            raise SystemExit('no cabe en la textura')
        CUBOS[i]['uv'] = [x, y]
        CUBOS[i]['uvt'] = (w, h, d)
        x += ww
        fila = max(fila, hh)


def textura():
    img = Image.new('RGBA', (TW, TH), (0, 0, 0, 0))
    for i, c in enumerate(CUBOS):
        u, v = c['uv']
        w, h, d = c['uvt']
        for cara, (x, y, cw, ch) in caras_caja(u, v, w, h, d).items():
            if cw <= 0 or ch <= 0:
                continue
            img.paste(Image.fromarray(mat(c['mat'], cw, ch, cara, 100 + i * 7 + len(cara)), 'RGBA'), (x, y))
    return img


def geo():
    bones = []
    for nombre, padre, piv, giro in HUESOS:
        b = {'name': nombre, 'pivot': piv}
        if padre:
            b['parent'] = padre
        if any(giro):
            b['rotation'] = giro
        cs = []
        for c in CUBOS:
            if c['hueso'] != nombre:
                continue
            x = {'origin': c['o'], 'size': c['t'], 'uv': c['uv']}
            if c['inflar']:
                x['inflate'] = c['inflar']
            if c.get('giro'):
                x['rotation'] = c['giro']
                x['pivot'] = c['pivote']
            cs.append(x)
        if cs:
            b['cubes'] = cs
        bones.append(b)
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.coleccionista.arma_techo', 'texture_width': TW, 'texture_height': TH,
                        'visible_bounds_width': 3, 'visible_bounds_height': 8, 'visible_bounds_offset': [0, -3, 0]},
        'axelis_glow': ['fogonazo'],
        'bones': bones}]}


def anims():
    T = 'q.anim_time'
    return {'format_version': '1.8.0', 'animations': {
        # quieta: se mece apenas (cuelga de una cuerda), y el martillo... no se mueve
        'animation.arma_techo.idle': {'loop': True, 'animation_length': 6.0, 'bones': {
            'mastil': {'rotation': [f'math.sin({T}*60)*0.6', 0, f'math.sin({T}*45)*0.5']},
        }},
    }}


def main():
    modelo()
    empacar()
    tex = textura()
    for sub in ('geo', 'animations', os.path.join('textures', 'entity')):
        os.makedirs(os.path.join(A, sub), exist_ok=True)
    json.dump(geo(), open(os.path.join(A, 'geo', 'arma_techo.geo.json'), 'w'), indent=1)
    json.dump(anims(), open(os.path.join(A, 'animations', 'arma_techo.animation.json'), 'w'), indent=1)
    tex.save(os.path.join(A, 'textures', 'entity', 'arma_techo.png'))
    print(len(HUESOS), 'huesos', len(CUBOS), 'cubos')


if __name__ == '__main__':
    main()
