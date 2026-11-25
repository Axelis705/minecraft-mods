#!/usr/bin/env python3
"""La bailarina de la caja de música gigante (capítulo 6): de porcelana, en puntas sobre su caja, con tutú de encaje y los brazos arriba.
Mientras toca la melodía gira despacio; cuando alguien se equivoca, se detiene y voltea la cabeza (solo la cabeza) hacia él.

Escribe geo/bailarina.geo.json, animations/bailarina.animation.json y textures/entity/bailarina.png.
La entidad está en el piso del escenario (y = 0 del modelo); la caja mide 26 px de lado y 10 de alto; la bailarina mira a -z.

Huesos: base (la caja, fija), llave (la llave de cuerda de atrás, gira), giro (todo lo que gira), cuerpo, tutu, pierna_a (la de apoyo),
pierna_b (doblada), cabeza, brazo_der, brazo_izq. Todo lo mueve el código (Bailarina.axPose).

Uso: python3 herramientas/bailarina.py
"""
import json
import math
import os

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista')
TW, TH = 256, 128
HUESOS = []
CUBOS = []


def hueso(nombre, padre, pivote, giro=(0, 0, 0)):
    HUESOS.append((nombre, padre, list(pivote), list(giro)))


def cubo(h, o, t, mat, inflar=0.0):
    CUBOS.append({'hueso': h, 'o': list(o), 't': list(t), 'mat': mat, 'inflar': inflar})


def modelo():
    hueso('raiz', None, (0, 0, 0))
    # la caja de música: madera oscura con orillas doradas y un espejo al frente
    hueso('base', 'raiz', (0, 0, 0))
    cubo('base', (-13, 0, -13), (26, 8, 26), 'caja')
    cubo('base', (-13.5, 8, -13.5), (27, 2, 27), 'oro')
    cubo('base', (-6, 2, -13.6), (12, 5, 0.6), 'espejo')
    cubo('base', (-3, 10, -3), (6, 1, 6), 'oro')                     # el platito donde gira
    hueso('llave', 'base', (0, 5, 13.5))
    cubo('llave', (-0.6, 4.4, 13), (1.2, 1.2, 3), 'laton')
    cubo('llave', (-4, 3, 15.5), (8, 4, 1), 'laton')
    # lo que gira
    hueso('giro', 'raiz', (0, 11, 0))
    hueso('cuerpo', 'giro', (0, 11, 0))
    # la pierna de apoyo (en puntas) y la otra doblada
    hueso('pierna_a', 'cuerpo', (0, 30, 0))
    cubo('pierna_a', (-1.3, 11, -1.3), (2.6, 19, 2.6), 'malla')
    cubo('pierna_a', (-1.4, 11, -1.8), (2.8, 3, 3.2), 'zapatilla')
    hueso('pierna_b', 'cuerpo', (1.5, 30, 0), (60, 0, -20))
    cubo('pierna_b', (0.2, 21, -1.3), (2.6, 9, 2.6), 'malla')
    hueso('pierna_b2', 'pierna_b', (1.5, 21, 0), (-110, 0, 0))
    cubo('pierna_b2', (0.2, 12, -1.3), (2.6, 9, 2.6), 'malla')
    cubo('pierna_b2', (0.1, 11, -1.8), (2.8, 2, 3.2), 'zapatilla')
    # el tutú (dos capas de encaje) y el torso
    hueso('tutu', 'cuerpo', (0, 30, 0))
    cubo('tutu', (-12, 29.5, -12), (24, 1.5, 24), 'encaje')
    cubo('tutu', (-9, 31, -9), (18, 1.5, 18), 'encaje')
    cubo('cuerpo', (-3.5, 31, -2.2), (7, 11, 4.4), 'malla_torso')
    cubo('cuerpo', (-1.2, 42, -1.2), (2.4, 2, 2.4), 'porcelana')     # el cuello
    # la cabeza: porcelana agrietada, ojos negros, chongo
    hueso('cabeza', 'cuerpo', (0, 44, 0))
    cubo('cabeza', (-4.5, 44, -4.5), (9, 9, 9), 'cara')
    cubo('cabeza', (-2.5, 53, -1), (5, 3, 5), 'pelo')
    cubo('cabeza', (-4.7, 47, -1), (9.4, 6.4, 5.8), 'pelo')
    cubo('cabeza', (-3.5, 53.5, 0.5), (7, 1, 1), 'listón')
    # los brazos arriba, en arco (quinta posición)
    for lado, x, s in (('brazo_der', -4, -1), ('brazo_izq', 4, 1)):
        hueso(lado, 'cuerpo', (x, 41, 0), (0, 0, -150 * s))
        xa = x - 1 if s < 0 else x - 1
        cubo(lado, (xa, 28, -1), (2, 13, 2), 'porcelana')
        cubo(lado, (xa - 0.2, 26, -1.2), (2.4, 2.4, 2.4), 'porcelana')


# ---------------------------------------------------------------- textura

def hexc(c):
    c = c.lstrip('#')
    return [int(c[i:i + 2], 16) for i in (0, 2, 4)]


def mat(nombre, w, h, cara, s):
    r = np.random.RandomState(s)
    yy, xx = np.mgrid[0:h, 0:w]
    ruido = r.rand(h, w)[..., None]
    if nombre in ('porcelana', 'cara'):
        orilla = np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy))
        rgb = np.array(hexc('#efe8e2'), float) * (0.92 + 0.08 * ruido) * (0.86 + 0.14 * np.clip(orilla / 2, 0, 1))[..., None]
        if nombre == 'cara' and cara == 'north':
            # la cara: ojos negros enormes y vacíos, mejillas rosas, boquita roja, una grieta que baja desde el ojo
            for ex in (w * 0.3, w * 0.7):
                ey = h * 0.42
                d = np.hypot((xx - ex) / max(1.2, w * 0.1), (yy - ey) / max(1.2, h * 0.12))
                rgb[d < 1] = np.array(hexc('#0a0608'))
                rgb[(d >= 1) & (d < 1.35)] = rgb[(d >= 1) & (d < 1.35)] * 0.7
                mej = np.hypot(xx - ex, yy - (h * 0.64)) < 1.0
                rgb[mej] = rgb[mej] * 0.6 + np.array(hexc('#e8908c')) * 0.4
            boca = (np.abs(xx - w / 2) < 1.0) & (np.abs(yy - h * 0.78) < 0.6)
            rgb[boca] = np.array(hexc('#a0101c'))
            for t in range(int(h * 0.5)):
                xi, yi = int(w * 0.7 + (t % 3 == 0) * 1 - t * 0.15), int(h * 0.5 + t)
                if 0 <= xi < w and 0 <= yi < h:
                    rgb[yi, xi] = np.array(hexc('#4a3e38'))
        elif r.rand() < 0.5 and w > 2 and h > 2:
            x0, y0 = r.randint(0, w), r.randint(0, h)
            for t in range(3):
                if 0 <= x0 + t < w and 0 <= y0 + t < h:
                    rgb[y0 + t, x0 + t] = np.array(hexc('#7c746c'))
    elif nombre == 'malla':
        rgb = np.array(hexc('#f0c8c8'), float) * (0.9 + 0.1 * ruido)
    elif nombre == 'malla_torso':
        rgb = np.array(hexc('#e8a8b4'), float) * (0.88 + 0.12 * ruido)
        rgb[(xx + yy) % 4 == 0] *= 0.85
    elif nombre == 'zapatilla':
        rgb = np.array(hexc('#d07890'), float) * (0.85 + 0.15 * ruido)
    elif nombre == 'encaje':
        rgb = np.array(hexc('#f6e6ea'), float) * (0.85 + 0.15 * ruido)
        rgb[(xx + yy) % 3 == 0] *= 0.8
        rgb[(xx % 6 == 0) | (yy % 6 == 0)] *= 0.88
    elif nombre == 'pelo':
        rgb = np.array(hexc('#1a1012'), float) * (0.8 + 0.4 * ruido)
    elif nombre == 'listón':
        rgb = np.array(hexc('#b01c3a'), float) * (0.85 + 0.2 * ruido)
    elif nombre == 'caja':
        rgb = np.array(hexc('#3a1e14'), float) * (0.8 + 0.3 * ruido)
        if cara in ('north', 'south', 'east', 'west') and w >= 8 and h >= 6:
            borde = (xx < 1) | (xx >= w - 1) | (yy < 1) | (yy >= h - 1)
            rgb[borde] = np.array(hexc('#b8902e'))
            flor = (np.hypot(xx - w / 2, yy - h / 2) < 2.2) & (np.hypot(xx - w / 2, yy - h / 2) > 1.2)
            rgb[flor] = np.array(hexc('#d8b04a'))
    elif nombre == 'oro':
        rgb = np.array(hexc('#c79a32'), float) * (0.8 + 0.35 * ruido)
        rgb[0, :] = np.array(hexc('#f2d06a'))
    elif nombre == 'laton':
        rgb = np.array(hexc('#a77a2c'), float) * (0.75 + 0.35 * ruido)
    elif nombre == 'espejo':
        rgb = np.array(hexc('#8a9ca8'), float) * (0.8 + 0.3 * ruido)
        rgb[(xx + yy) % 5 == 0] *= 1.25
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
            img.paste(Image.fromarray(mat(c['mat'], cw, ch, cara, 500 + i * 7 + len(cara)), 'RGBA'), (x, y))
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
            cs.append(x)
        if cs:
            b['cubes'] = cs
        bones.append(b)
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.coleccionista.bailarina', 'texture_width': TW, 'texture_height': TH,
                        'visible_bounds_width': 4, 'visible_bounds_height': 8, 'visible_bounds_offset': [0, 3, 0]},
        'bones': bones}]}


def anims():
    return {'format_version': '1.8.0', 'animations': {
        'animation.bailarina.idle': {'loop': True, 'animation_length': 4.0, 'bones': {}},
    }}


def main():
    modelo()
    empacar()
    tex = textura()
    for sub in ('geo', 'animations', os.path.join('textures', 'entity')):
        os.makedirs(os.path.join(A, sub), exist_ok=True)
    json.dump(geo(), open(os.path.join(A, 'geo', 'bailarina.geo.json'), 'w'), indent=1)
    json.dump(anims(), open(os.path.join(A, 'animations', 'bailarina.animation.json'), 'w'), indent=1)
    tex.save(os.path.join(A, 'textures', 'entity', 'bailarina.png'))
    print(len(HUESOS), 'huesos', len(CUBOS), 'cubos')


if __name__ == '__main__':
    main()
