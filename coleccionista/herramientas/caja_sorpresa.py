#!/usr/bin/env python3
"""La caja sorpresa gigante (capítulo 4) y Don Resorte, el payaso que vive adentro.

Escribe geo/caja_sorpresa.geo.json, animations/caja_sorpresa.animation.json y textures/entity/caja_sorpresa.png.
La entidad está en el piso del escenario (y = 0 del modelo), al centro de la caja. La caja mide 40 px de lado (2.5 bloques) y 36 de alto
sobre cuatro patitas; la tapa tiene la bisagra atrás (+z) y la manivela sale del frente (-z), donde se para quien le da cuerda. Adentro, Don Resorte: el resorte
(8 anillos que se estiran), el cuerpo con su gola, la cabeza grande (con la quijada que se abre), el sombrerito y los brazos largos con
guantes. Todo lo mueve el código (CajaSorpresa.axPose): guardado, el payaso queda agachado dentro de la caja.

Huesos: caja (fija), tapa, manivela, anillo0..7, payaso (sube con el resorte), giro (voltea hacia la manivela), cuerpo, cabeza,
mandibula, sombrero, brazo_der, brazo_izq.

Uso: python3 herramientas/caja_sorpresa.py
"""
import json
import math
import os

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista')
TW, TH = 512, 256
HUESOS = []
CUBOS = []
B = 20      # medio lado de la caja
H = 40      # altura de la parte de arriba de la caja (la tapa va encima)


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
    # la caja: cuatro paredes pintadas (cada una de un color), el fondo, la orilla dorada y las patitas
    hueso('caja', 'raiz', (0, 0, 0))
    cubo('caja', (-B, 4, -B), (2 * B, 2, 2 * B), 'madera_osc')                 # el fondo
    cubo('caja', (-B, 6, -B), (2 * B, H - 6, 2), 'panel_morado')               # frente (-z)
    cubo('caja', (-B, 6, B - 2), (2 * B, H - 6, 2), 'panel_verde')             # atrás
    cubo('caja', (-B, 6, -B + 2), (2, H - 6, 2 * B - 4), 'panel_rojo')         # izquierda (-x)
    cubo('caja', (B - 2, 6, -B + 2), (2, H - 6, 2 * B - 4), 'panel_azul')      # derecha (+x, la de la manivela)
    cubo('caja', (-B + 2, 6, -B + 2), (2 * B - 4, 1, 2 * B - 4), 'interior')   # el piso de adentro
    for (x, z) in ((-B, -B), (B - 2, -B), (-B, B - 2), (B - 2, B - 2)):
        cubo('caja', (x - 0.3, 4, z - 0.3), (2.6, H - 4, 2.6), 'oro')           # las esquinas doradas
        cubo('caja', (x - 0.5 + (1 if x > 0 else 0), 0, z - 0.5 + (1 if z > 0 else 0)), (3, 4, 3), 'oro')   # las patitas
    for y in (6, H - 2):
        cubo('caja', (-B - 0.3, y, -B - 0.3), (2 * B + 0.6, 1.4, 0.6), 'oro')
        cubo('caja', (-B - 0.3, y, B - 0.3), (2 * B + 0.6, 1.4, 0.6), 'oro')
        cubo('caja', (-B - 0.3, y, -B), (0.6, 1.4, 2 * B), 'oro')
        cubo('caja', (B - 0.3, y, -B), (0.6, 1.4, 2 * B), 'oro')
    # la tapa (bisagra atrás) con su perilla
    hueso('tapa', 'raiz', (0, H, B))
    cubo('tapa', (-B - 1, H, -B - 1), (2 * B + 2, 3, 2 * B + 2), 'tapa')
    cubo('tapa', (-3, H + 3, -3), (6, 2, 6), 'oro')
    cubo('tapa', (-1.5, H + 5, -1.5), (3, 2, 3), 'rojo')
    # la manivela (al frente, abajo del centro): el eje, el brazo y el mango rojo (gira en el plano del frente)
    hueso('manivela', 'raiz', (0, 15, -B - 2))
    cubo('manivela', (-1, 14, -B - 4), (2, 2, 4), 'oro')
    cubo('manivela', (-2, 13, -B - 2.5), (4, 4, 2), 'oro')
    cubo('manivela', (-0.8, 15, -B - 4.6), (1.6, 10, 1.6), 'oro')
    cubo('manivela', (-1.2, 23.5, -B - 8.5), (2.4, 2.4, 4.6), 'rojo')
    # el resorte: 8 anillos (cada uno sube su parte cuando el payaso sale)
    for k in range(8):
        n = f'anillo{k}'
        y = 7 + k * 0.9
        hueso(n, 'raiz', (0, y, 0))
        r = 5
        cubo(n, (-r, y, -r), (2 * r, 0.8, 1), 'resorte')
        cubo(n, (-r, y, r - 1), (2 * r, 0.8, 1), 'resorte')
        cubo(n, (-r, y, -r + 1), (1, 0.8, 2 * r - 2), 'resorte')
        cubo(n, (r - 1, y, -r + 1), (1, 0.8, 2 * r - 2), 'resorte')
    # Don Resorte: guardado, agachado dentro de la caja (de y 14 a 39)
    hueso('payaso', 'raiz', (0, 14, 0))
    hueso('giro', 'payaso', (0, 14, 0))
    hueso('cuerpo', 'giro', (0, 14, 0))
    cubo('cuerpo', (-5, 14, -4), (10, 9, 8), 'rayas')
    # la gola (cuello de holanes)
    cubo('cuerpo', (-8, 22, -8), (16, 2, 16), 'gola')
    cubo('cuerpo', (-8, 22, -8), (16, 2, 16), 'gola', giro=(0, 45, 0), pivote=(0, 23, 0))
    for y in (16, 19):
        cubo('cuerpo', (-1, y, -4.4), (2, 2, 0.6), 'rojo')
    # la cabeza: gigante, sin sentido para lo chiquito del cuerpo (guardada se aplasta dentro de la caja)
    hueso('cabeza', 'cuerpo', (0, 25, 0))
    cubo('cabeza', (-19, 36, -17), (38, 24, 34), 'cara')
    cubo('cabeza', (-3.5, 41, -21), (7, 7, 5), 'nariz')
    # el pelo: dos matorrales enormes a los lados y uno atrás
    cubo('cabeza', (-27, 38, -10), (8, 16, 20), 'pelo')
    cubo('cabeza', (19, 38, -10), (8, 16, 20), 'pelo')
    cubo('cabeza', (-30, 46, -6), (5, 10, 12), 'pelo')
    cubo('cabeza', (25, 46, -6), (5, 10, 12), 'pelo')
    cubo('cabeza', (-12, 50, 15), (24, 10, 4), 'pelo')
    # los dientes de arriba (afilados, en fila, por dentro de la orilla)
    for k in range(9):
        x = -16 + k * 4
        cubo('cabeza', (x, 33, -16), (2.4, 3, 2), 'diente')
        cubo('cabeza', (x + 0.6, 32, -16), (1.2, 1, 2), 'diente')
    for z in (-11, -6, -1, 4):
        cubo('cabeza', (-17, 33, z), (2, 3, 2.4), 'diente')
        cubo('cabeza', (15, 33, z), (2, 3, 2.4), 'diente')
    cubo('cabeza', (-17, 35.5, -16), (34, 0.6, 30), 'boca')
    # la quijada (enorme, con bisagra atrás): labio, lengua y dientes de abajo
    hueso('mandibula', 'cabeza', (0, 36, 13))
    cubo('mandibula', (-18, 26, -16), (36, 8, 31), 'quijada')
    cubo('mandibula', (-16, 34, -14), (32, 0.6, 27), 'boca')
    cubo('mandibula', (-5, 34.2, -12), (10, 1.4, 16), 'lengua')
    for k in range(9):
        x = -16 + k * 4 + 2
        cubo('mandibula', (x, 34, -15.5), (2.2, 3, 2), 'diente')
        cubo('mandibula', (x + 0.5, 37, -15.5), (1.2, 1, 2), 'diente')
    hueso('sombrero', 'cabeza', (0, 60, 0))
    cubo('sombrero', (-6, 60, -6), (12, 1.5, 12), 'negro', giro=(0, 0, -14), pivote=(0, 60, 0))
    cubo('sombrero', (-4.5, 61.5, -4.5), (9, 8, 9), 'negro', giro=(0, 0, -14), pivote=(0, 60, 0))
    cubo('sombrero', (-4.6, 61.5, -4.6), (9.2, 1.6, 9.2), 'rojo', giro=(0, 0, -14), pivote=(0, 60, 0))
    for lado, x in (('brazo_der', 5), ('brazo_izq', -5)):
        hueso(lado, 'cuerpo', (x, 21, 0))
        xa = x if x > 0 else x - 3
        cubo(lado, (xa, -3, -1.5), (3, 24, 3), 'rayas')
        cubo(lado, (xa - 2.5, -11, -4), (8, 8, 8), 'guante')
        cubo(lado, (xa - 1, -9, -6), (2, 5, 2), 'guante')
        cubo(lado, (xa + 2, -9, -6), (2, 5, 2), 'guante')


# ---------------------------------------------------------------- textura

def hexc(c):
    c = c.lstrip('#')
    return [int(c[i:i + 2], 16) for i in (0, 2, 4)]


def mat(nombre, w, h, cara, s):
    r = np.random.RandomState(s)
    yy, xx = np.mgrid[0:h, 0:w]
    ruido = r.rand(h, w)[..., None]
    if nombre.startswith('panel_'):
        color = {'panel_morado': '#4a1f5e', 'panel_verde': '#1e4d3a', 'panel_rojo': '#7a1620', 'panel_azul': '#1f3566'}[nombre]
        rgb = np.array(hexc(color), float) * (0.85 + 0.2 * ruido)
        if cara in ('north', 'south', 'east', 'west') and w >= 8 and h >= 8:
            # un marco dorado pintado y, al centro, un signo de interrogación o una estrella (pintura gastada)
            borde = (xx < 2) | (xx >= w - 2) | (yy < 2) | (yy >= h - 2)
            rgb[borde] = np.array(hexc('#b8902e')) * (0.8 + 0.3 * r.rand(int(borde.sum()), 1))
            cx, cy = w / 2, h / 2
            d = np.hypot(xx - cx, yy - cy)
            ang = np.arctan2(yy - cy, xx - cx)
            estrella = d < (6 + 3 * np.cos(5 * ang)) * min(w, h) / 34
            rgb[estrella] = np.array(hexc('#e8c86a'))
            # grietas y pintura caída
            rgb[r.rand(h, w) > 0.985] *= 0.4
    elif nombre == 'oro':
        rgb = np.array(hexc('#b8902e'), float) * (0.8 + 0.35 * ruido)
        rgb[0, :] = np.array(hexc('#f0cf72'))
    elif nombre == 'madera_osc':
        rgb = np.array(hexc('#2a1a10'), float) * (0.8 + 0.3 * ruido)
    elif nombre == 'interior':
        rgb = np.array(hexc('#120a0a'), float) * (0.8 + 0.4 * ruido)
    elif nombre == 'tapa':
        rgb = np.array(hexc('#5a1a2a'), float) * (0.85 + 0.2 * ruido)
        if cara == 'up':
            cuadros = ((xx // 6 + yy // 6) % 2 == 0)
            rgb[cuadros] = np.array(hexc('#2a0e18'))
            borde = (xx < 2) | (xx >= w - 2) | (yy < 2) | (yy >= h - 2)
            rgb[borde] = np.array(hexc('#b8902e'))
    elif nombre == 'rojo':
        rgb = np.array(hexc('#b01c24'), float) * (0.85 + 0.25 * ruido)
        rgb[0, :] = np.array(hexc('#e0555a'))
    elif nombre == 'negro':
        rgb = np.array(hexc('#141014'), float) * (0.8 + 0.4 * ruido)
    elif nombre == 'resorte':
        rgb = np.array(hexc('#8a8e96'), float) * (0.7 + 0.5 * ruido)
    elif nombre == 'rayas':
        rgb = np.where(((xx + yy) // 2 % 2 == 0)[..., None], np.array(hexc('#c42a30'), float), np.array(hexc('#e8e0d0'), float))
        rgb = rgb * (0.85 + 0.2 * ruido)
    elif nombre == 'gola':
        rgb = np.array(hexc('#ece6da'), float) * (0.8 + 0.25 * ruido)
        rgb[(xx % 3 == 0)] *= 0.75
    elif nombre == 'cara':
        # porcelana vieja, amarillenta en las orillas, con grietas
        orilla = np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy))
        rgb = np.array(hexc('#e6ddd0'), float) * (0.9 + 0.12 * ruido) * (0.86 + 0.14 * np.clip(orilla / 3, 0, 1))[..., None]
        if cara == 'north':
            for ex in (w * 0.29, w * 0.71):
                ey = h * 0.42
                aro = np.hypot((xx - ex) / max(2.1, w * 0.085), (yy - ey) / max(2.5, h * 0.16))
                rgb[aro < 1.35] = rgb[aro < 1.35] * 0.45 + np.array(hexc('#5a0a12')) * 0.55   # ojeras rojizas
                rgb[aro < 1] = np.array(hexc('#050304'))                                       # el ojo, hueco
                rgb[(np.abs(xx - (ex + 0.8)) < 0.55) & (np.abs(yy - (ey + 0.4)) < 0.55)] = np.array(hexc('#e8e8e0'))  # la pupila
                # lágrimas negras que escurren
                lag = (np.abs(xx - (ex - 1.2)) < 0.5) & (yy > ey + h * 0.14) & (yy < ey + h * 0.14 + h * 0.25 + (ex > w / 2) * 3)
                rgb[lag] = np.array(hexc('#14080a'))
                ceja = (np.abs(yy - (h * 0.14 + np.abs(xx - ex) * 0.4)) < 0.6) & (np.abs(xx - ex) < 3.2)
                rgb[ceja] = np.array(hexc('#1a0c0c'))
            # la sonrisa: muy ancha, de oreja a oreja, con puntadas
            curva = h * 0.9 - ((xx - w / 2) / (w / 2)) ** 2 * h * 0.36
            sonrisa = np.abs(yy - curva) < 1.0
            rgb[sonrisa] = np.array(hexc('#7a0a12'))
            punt = (np.abs(yy - curva) < 1.8) & (xx % 2 == 0) & (np.abs(xx - w / 2) > 1)
            rgb[punt] = np.array(hexc('#24080a'))
            # grietas
            for (x0, y0, dx, dy, n) in ((w * 0.6, 0, 0.6, 1, 5), (w * 0.15, h * 0.6, 0.8, -0.5, 4)):
                for t in range(n):
                    xi, yi = int(x0 + dx * t), int(y0 + dy * t)
                    if 0 <= xi < w and 0 <= yi < h:
                        rgb[yi, xi] = np.array(hexc('#3a302a'))
    elif nombre == 'quijada':
        rgb = np.array(hexc('#e8e0d6'), float) * (0.9 + 0.12 * ruido)
        if cara == 'north':
            rgb[: max(1, h // 2), :] = np.array(hexc('#9a1018'))
        if cara == 'up':
            rgb[:] = np.array(hexc('#3a060a'))
    elif nombre == 'boca':
        rgb = np.array(hexc('#3a0408'), float) * (0.7 + 0.5 * ruido)
    elif nombre == 'lengua':
        rgb = np.array(hexc('#8a1a2a'), float) * (0.8 + 0.3 * ruido)
        rgb[:, w // 2] *= 0.6
    elif nombre == 'diente':
        rgb = np.array(hexc('#e6dcc0'), float) * (0.85 + 0.15 * ruido)
        rgb[(yy == h - 1)] = np.array(hexc('#c8b890'))
        rgb[r.rand(h, w) > 0.8] = np.array(hexc('#9a6a50'))   # manchados
    elif nombre == 'dientes':
        rgb = np.array(hexc('#efe4c8'), float) * (0.85 + 0.15 * ruido)
        rgb[:, ::2] = np.array(hexc('#3a060a'))
    elif nombre == 'nariz':
        rgb = np.array(hexc('#d01a22'), float) * (0.85 + 0.2 * ruido)
        rgb[0, :] = np.array(hexc('#ff7070'))
    elif nombre == 'pelo':
        rgb = np.array(hexc('#d0561a'), float) * (0.7 + 0.5 * ruido)
    elif nombre == 'guante':
        rgb = np.array(hexc('#f0ece4'), float) * (0.85 + 0.15 * ruido)
        rgb[(yy == h - 1)] *= 0.7
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
        'description': {'identifier': 'geometry.coleccionista.caja_sorpresa', 'texture_width': TW, 'texture_height': TH,
                        'visible_bounds_width': 6, 'visible_bounds_height': 8, 'visible_bounds_offset': [0, 3, 0]},
        'bones': bones}]}


def anims():
    return {'format_version': '1.8.0', 'animations': {
        'animation.caja_sorpresa.idle': {'loop': True, 'animation_length': 4.0, 'bones': {}},
    }}


def main():
    modelo()
    empacar()
    tex = textura()
    for sub in ('geo', 'animations', os.path.join('textures', 'entity')):
        os.makedirs(os.path.join(A, sub), exist_ok=True)
    json.dump(geo(), open(os.path.join(A, 'geo', 'caja_sorpresa.geo.json'), 'w'), indent=1)
    json.dump(anims(), open(os.path.join(A, 'animations', 'caja_sorpresa.animation.json'), 'w'), indent=1)
    tex.save(os.path.join(A, 'textures', 'entity', 'caja_sorpresa.png'))
    print(len(HUESOS), 'huesos', len(CUBOS), 'cubos')


if __name__ == '__main__':
    main()
