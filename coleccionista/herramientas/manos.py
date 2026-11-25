#!/usr/bin/env python3
"""La mano gigante de porcelana del salón de té (capítulo 5): sale de la mesa con su mecanismo de latón y juega piedra, papel o tijera.

Escribe geo/mano_gigante.geo.json, animations/mano_gigante.animation.json y textures/entity/mano_gigante.png.
La entidad está sobre el mantel (y = 0 del modelo), en medio de la mesa, mirando hacia el rival (-z del modelo). Atrás (+z) está la base
de latón con engranes, de ahí sale el antebrazo (con un puño de encaje) y al frente la mano, de canto: la palma mira de lado (+x), el
pulgar arriba y los cuatro dedos apuntando al rival, uno encima de otro (índice arriba, meñique abajo). Así, vista de lado, se lee bien
la jugada: puño (piedra), mano abierta (papel) o dos dedos en V (tijera). Los dedos se doblan hacia la palma (giro en y).

Huesos: base (fija), brazo (sube y baja al sacudirse), mano, dedo0..3 (con su punta dedoN_p) y pulgar (con pulgar_p). Todo lo mueve el
código (ManoGigante.axPose).

Uso: python3 herramientas/manos.py
"""
import json
import math
import os

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista')
TW, TH = 128, 128
HUESOS = []
CUBOS = []
# el puño de la mano (la muñeca) y los nudillos
MY = 8.0        # altura del centro de la mano
WZ = -3.0       # la muñeca
KZ = -10.0      # los nudillos


def hueso(nombre, padre, pivote, giro=(0, 0, 0)):
    HUESOS.append((nombre, padre, list(pivote), list(giro)))


def cubo(h, o, t, mat, inflar=0.0):
    CUBOS.append({'hueso': h, 'o': list(o), 't': list(t), 'mat': mat, 'inflar': inflar})


def modelo():
    hueso('raiz', None, (0, 0, 0))
    # la base de latón: una cajita con engranes a los lados, atornillada al mantel
    hueso('base', 'raiz', (0, 0, 6))
    cubo('base', (-3.5, 0, 3), (7, 3, 7), 'laton')
    cubo('base', (-2.5, 3, 4), (5, 1, 5), 'laton_osc')
    for x in (-4.2, 3.4):
        cubo('base', (x, 0.5, 4.5), (0.8, 4, 4), 'engrane')
    # el brazo: de la base sale el antebrazo (porcelana) hacia el frente, con un puño de encaje en la muñeca
    hueso('brazo', 'raiz', (0, 4, 6.5))
    cubo('brazo', (-1.5, 3, 4.5), (3, 4, 3), 'laton')                     # el codo (articulación)
    cubo('brazo', (-2, 5.5, WZ), (4, 5, 9), 'porcelana')                  # el antebrazo
    cubo('brazo', (-2.6, 5, WZ - 0.6), (5.2, 6, 2.2), 'encaje')           # el puño de encaje
    cubo('brazo', (-2.3, 5.2, WZ + 1.4), (4.6, 5.6, 0.8), 'oro')          # el aro dorado
    # la mano: de canto (la palma mira a +x), el dorso a -x
    hueso('mano', 'brazo', (0, MY, WZ))
    cubo('mano', (-1.6, MY - 4, KZ), (3.2, 8, WZ - KZ), 'porcelana')
    # los cuatro dedos (índice arriba): base y punta; se doblan hacia la palma (giro en y)
    for k in range(4):
        y = MY + 2 - k * 2.0
        largo = (3.6, 4.0, 3.8, 3.0)[k]
        punta = (3.0, 3.4, 3.2, 2.6)[k]
        b, pnt = f'dedo{k}', f'dedo{k}_p'
        hueso(b, 'mano', (0, y + 0.9, KZ))
        cubo(b, (-1.2, y, KZ - largo), (2.4, 1.8, largo), 'porcelana')
        hueso(pnt, b, (0, y + 0.9, KZ - largo))
        cubo(pnt, (-1.1, y + 0.05, KZ - largo - punta), (2.2, 1.7, punta), 'porcelana')
        cubo(pnt, (0.6, y + 0.3, KZ - largo - punta - 0.05), (0.7, 1.1, 1.2), 'una')
    # el pulgar: arriba, cerca de la muñeca, apuntando al frente y un poco hacia arriba
    hueso('pulgar', 'mano', (0.4, MY + 3.5, WZ - 1.5))
    cubo('pulgar', (-0.9, MY + 3.5, WZ - 5.5), (2.2, 2.0, 4.0), 'porcelana')
    hueso('pulgar_p', 'pulgar', (0.4, MY + 4.5, WZ - 5.5))
    cubo('pulgar_p', (-0.8, MY + 3.6, WZ - 8.3), (2.0, 1.8, 2.8), 'porcelana')
    cubo('pulgar_p', (0.6, MY + 3.9, WZ - 8.35), (0.7, 1.1, 1.1), 'una')


# ---------------------------------------------------------------- textura

def hexc(c):
    c = c.lstrip('#')
    return [int(c[i:i + 2], 16) for i in (0, 2, 4)]


def mat(nombre, w, h, cara, s):
    r = np.random.RandomState(s)
    yy, xx = np.mgrid[0:h, 0:w]
    ruido = r.rand(h, w)[..., None]
    if nombre == 'porcelana':
        # porcelana vieja: blanca azulada, con un brillo arriba, sombra en las orillas y grietas finas
        orilla = np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy))
        rgb = np.array(hexc('#ece8e2'), float) * (0.92 + 0.08 * ruido) * (0.84 + 0.16 * np.clip(orilla / 2, 0, 1))[..., None]
        if cara == 'up':
            rgb = rgb * 1.04
        if w >= 3 and h >= 3:
            for _ in range(max(1, (w * h) // 30)):
                x0, y0 = r.randint(0, w), r.randint(0, h)
                for t in range(r.randint(2, 5)):
                    xi, yi = x0 + t * r.choice([-1, 0, 1]), y0 + t
                    if 0 <= xi < w and 0 <= yi < h:
                        rgb[yi, xi] = np.array(hexc('#7c746c'))
            # una mancha rosada (como rubor pintado) en algunos nudillos
            if cara in ('east', 'west') and r.rand() < 0.4:
                cx, cy = w / 2, h / 2
                d = np.hypot(xx - cx, yy - cy)
                rgb[d < 1.2] = rgb[d < 1.2] * 0.6 + np.array(hexc('#e0a0a0')) * 0.4
    elif nombre == 'una':
        rgb = np.array(hexc('#8a1622'), float) * (0.85 + 0.25 * ruido)
        rgb[0, :] = np.array(hexc('#d0485a'))
    elif nombre == 'encaje':
        rgb = np.array(hexc('#f4efe4'), float) * (0.85 + 0.15 * ruido)
        rgb[(xx + yy) % 3 == 0] *= 0.72
        rgb[(yy == h - 1)] *= 0.8
    elif nombre == 'oro':
        rgb = np.array(hexc('#c79a32'), float) * (0.8 + 0.35 * ruido)
        rgb[0, :] = np.array(hexc('#f2d06a'))
    elif nombre == 'laton':
        rgb = np.array(hexc('#a77a2c'), float) * (0.75 + 0.35 * ruido)
        rgb[(xx % 4 == 0) & (yy % 4 == 0)] = np.array(hexc('#5a3a12'))   # remaches
        rgb[0, :] = np.array(hexc('#d8aa52'))
    elif nombre == 'laton_osc':
        rgb = np.array(hexc('#6a4a1a'), float) * (0.8 + 0.3 * ruido)
    elif nombre == 'engrane':
        rgb = np.array(hexc('#8e6a26'), float) * (0.8 + 0.3 * ruido)
        cx, cy = (w - 1) / 2, (h - 1) / 2
        d = np.hypot(xx - cx, yy - cy)
        rgb[d < 0.8] = np.array(hexc('#2a1a08'))
        dientes = ((xx + yy) % 2 == 0) & ((xx == 0) | (yy == 0) | (xx == w - 1) | (yy == h - 1))
        rgb[dientes] = np.array(hexc('#4a3410'))
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
            img.paste(Image.fromarray(mat(c['mat'], cw, ch, cara, 300 + i * 7 + len(cara)), 'RGBA'), (x, y))
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
        'description': {'identifier': 'geometry.coleccionista.mano_gigante', 'texture_width': TW, 'texture_height': TH,
                        'visible_bounds_width': 3, 'visible_bounds_height': 3, 'visible_bounds_offset': [0, 1, 0]},
        'bones': bones}]}


def anims():
    return {'format_version': '1.8.0', 'animations': {
        'animation.mano_gigante.idle': {'loop': True, 'animation_length': 4.0, 'bones': {}},
    }}


def main():
    modelo()
    empacar()
    tex = textura()
    for sub in ('geo', 'animations', os.path.join('textures', 'entity')):
        os.makedirs(os.path.join(A, sub), exist_ok=True)
    json.dump(geo(), open(os.path.join(A, 'geo', 'mano_gigante.geo.json'), 'w'), indent=1)
    json.dump(anims(), open(os.path.join(A, 'animations', 'mano_gigante.animation.json'), 'w'), indent=1)
    tex.save(os.path.join(A, 'textures', 'entity', 'mano_gigante.png'))
    print(len(HUESOS), 'huesos', len(CUBOS), 'cubos')


if __name__ == '__main__':
    main()
