#!/usr/bin/env python3
"""La máquina de la final (veintiuno), chica y pegada al brazo: una plancha de latón con un cojín de cuero donde se apoya el antebrazo
izquierdo (el brazo queda ENCIMA, a la vista), dos correas de latón que lo sujetan (una arriba del codo y otra en la muñeca), una guillotina
chiquita entre dos postes justo a la mitad del brazo, tres focos rojos al frente (uno por ronda perdida), un engrane a un lado y un
cuadrante con su aguja al otro. Cuando corta, aparece la sangre (en el cojín, escurriendo por los lados, en la mesa y en la hoja).

Escribe geo/maquina.geo.json, animations/maquina.animation.json y textures/entity/maquina.png.
La entidad está sobre la mesa (y = 0 del modelo), debajo del antebrazo; mira a -z (hacia el rival): el hombro queda atrás (+z) y la mano
adelante (-z). El brazo se apoya en el cojín (arriba en y 4.3). La hoja cae en z = +1.75 (la mitad del brazo).
Mide 8 px de ancho, 13 de largo y 16 de alto.

Huesos: raiz, base, correa_a (arriba del codo), correa_b (la muñeca), cuchilla (con sangre_hoja), engrane, aguja, luz0..2 (los focos
encendidos), sangre. Todo lo mueve el código (Maquina.axPose).

Uso: python3 herramientas/maquina.py
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


def hueso(nombre, padre, pivote, giro=(0, 0, 0)):
    HUESOS.append((nombre, padre, list(pivote), list(giro)))


def cubo(h, o, t, mat, inflar=0.0):
    CUBOS.append({'hueso': h, 'o': list(o), 't': list(t), 'mat': mat, 'inflar': inflar})


def modelo():
    hueso('raiz', None, (0, 0, 0))
    hueso('base', 'raiz', (0, 0, 0))
    # la plancha, el pedestal y el cojín de cuero (donde se apoya el antebrazo)
    cubo('base', (-3.5, 0, -5), (7, 1, 10), 'laton_oscuro')
    cubo('base', (-3, 1, -4.5), (6, 2.8, 9), 'laton')
    cubo('base', (-2.3, 3.8, -4.5), (4.6, 0.5, 9), 'cuero')
    # el tope del frente (ahí quedan los dedos) con los tres focos arriba
    cubo('base', (-3.5, 1, -6.5), (7, 4.2, 1.6), 'laton_oscuro')
    for k, x in enumerate((-2.6, -0.8, 1.0)):
        cubo('base', (x, 5.2, -6.2), (1.6, 1.2, 1.2), 'foco_apagado')
    # remaches
    for x in (-2.9, 2.1):
        for z in (-4, 3.2):
            cubo('base', (x, 3.8, z), (0.8, 0.4, 0.8), 'remache')
    # los postes y la viga de la guillotina (a los lados del brazo, a la mitad)
    cubo('base', (-3.7, 3.8, 1.0), (0.9, 11.4, 1.5), 'hierro')
    cubo('base', (2.8, 3.8, 1.0), (0.9, 11.4, 1.5), 'hierro')
    cubo('base', (-4.0, 15.2, 0.8), (8.0, 1.2, 1.9), 'hierro')
    # el cuadrante a un lado y el engrane al otro
    cubo('base', (-3.6, 1.2, -2.2), (0.6, 2.6, 2.6), 'cuadrante')
    # los focos encendidos (se ven solo cuando hay rondas perdidas)
    for k, x in enumerate((-2.6, -0.8, 1.0)):
        hueso('luz%d' % k, 'base', (x + 0.8, 5.8, -5.6))
        cubo('luz%d' % k, (x, 5.2, -6.2), (1.6, 1.2, 1.2), 'foco_encendido', 0.15)
    # las correas (arcos de latón sobre el brazo; la bisagra en la orilla de abajo de un lado: se abren girando)
    for nombre, z in (('correa_a', 3.4), ('correa_b', -2.9)):
        hueso(nombre, 'base', (0, 4.3, z + 0.6))
        cubo(nombre, (-3.0, 4.3, z), (0.8, 4.2, 1.2), 'laton')
        cubo(nombre, (2.2, 4.3, z), (0.8, 4.2, 1.2), 'laton')
        cubo(nombre, (-3.0, 8.3, z), (6.0, 0.8, 1.2), 'laton')
        cubo(nombre, (-0.5, 8.7, z - 0.1), (1.0, 0.6, 1.4), 'remache')
    # la guillotina (arriba, lista para caer a la mitad del brazo)
    hueso('cuchilla', 'base', (0, 11, 1.75))
    cubo('cuchilla', (-2.7, 9.8, 1.5), (5.4, 3.2, 0.5), 'cuchilla')
    cubo('cuchilla', (-2.2, 13.0, 1.25), (4.4, 1.6, 1.0), 'hierro')
    hueso('sangre_hoja', 'cuchilla', (0, 10, 1.75))
    cubo('sangre_hoja', (-2.7, 9.8, 1.42), (5.4, 1.3, 0.66), 'sangre')
    # el engrane del otro lado y la aguja del cuadrante
    hueso('engrane', 'base', (3.4, 2.5, -0.9))
    cubo('engrane', (3.0, 1.0, -2.4), (0.8, 3, 3), 'engrane')
    hueso('aguja', 'base', (-3.7, 2.5, -0.9))
    cubo('aguja', (-3.75, 2.4, -1.0), (0.2, 1.3, 0.2), 'aguja')
    # la sangre (solo después del corte): en el cojín, escurriendo por los lados y en la mesa
    hueso('sangre', 'base', (0, 4.3, 1.75))
    cubo('sangre', (-2.4, 4.32, -1.2), (4.8, 0.1, 5.4), 'sangre')
    cubo('sangre', (-3.06, 1.2, -0.5), (0.1, 3.0, 3.6), 'sangre_gotas')
    cubo('sangre', (2.96, 1.2, -0.5), (0.1, 3.0, 3.6), 'sangre_gotas')
    cubo('sangre', (-5.5, 0.03, -3.0), (11.0, 0.1, 8.0), 'sangre')


# ---------------------------------------------------------------- textura

def hexc(c):
    c = c.lstrip('#')
    return [int(c[i:i + 2], 16) for i in (0, 2, 4)]


def mat(nombre, w, h, cara, s):
    r = np.random.RandomState(s)
    yy, xx = np.mgrid[0:h, 0:w]
    ruido = r.rand(h, w)[..., None]
    a = np.full((h, w), 255, np.uint8)
    if nombre in ('laton', 'laton_oscuro', 'cobre'):
        base = {'laton': '#b08a3a', 'laton_oscuro': '#6c5222', 'cobre': '#a35a2c'}[nombre]
        rgb = np.array(hexc(base), float) * (0.78 + 0.3 * ruido)
        # pátina verde en manchas y rayones
        pat = r.rand(h, w) < 0.07
        rgb[pat] = rgb[pat] * 0.5 + np.array(hexc('#4a7a5c')) * 0.5
        if w > 3 and h > 3:
            rgb[0, :] *= 1.25
            rgb[-1, :] *= 0.7
            rgb[(xx == w // 2) & (yy % 3 == 0)] *= 0.85
    elif nombre == 'cuero':
        rgb = np.array(hexc('#4a1418'), float) * (0.8 + 0.35 * ruido)
        if w >= 3:
            rgb[:, (xx[0] % 3 == 0)] *= 0.8
    elif nombre == 'remache':
        rgb = np.array(hexc('#d8b860'), float) * (0.85 + 0.2 * ruido)
    elif nombre == 'hierro':
        rgb = np.array(hexc('#3a3a40'), float) * (0.75 + 0.4 * ruido)
        oxido = r.rand(h, w) < 0.12
        rgb[oxido] = np.array(hexc('#6a3a1c')) * (0.8 + 0.3 * r.rand())
    elif nombre == 'acero_oscuro':
        rgb = np.array(hexc('#55585e'), float) * (0.8 + 0.3 * ruido)
        rgb[(xx + yy) % 2 == 0] *= 0.9
    elif nombre == 'cuchilla':
        rgb = np.array(hexc('#c4c9d0'), float) * (0.88 + 0.16 * ruido)
        if cara in ('north', 'south') and h >= 3:
            rgb[-1, :] = np.array(hexc('#f4f8ff'))      # el filo
            rgb[-2, :] *= 1.08
            rgb[0, :] *= 0.75
    elif nombre == 'foco_apagado':
        rgb = np.array(hexc('#3a0c0e'), float) * (0.8 + 0.4 * ruido)
    elif nombre == 'foco_encendido':
        rgb = np.array(hexc('#ff3a24'), float) * (0.9 + 0.15 * ruido)
        if w >= 3 and h >= 3:
            rgb[h // 2, w // 2] = np.array(hexc('#ffd8b0'))
    elif nombre == 'cuadrante':
        rgb = np.array(hexc('#6c5222'), float) * (0.8 + 0.3 * ruido)
        if cara in ('east', 'west') and w >= 4 and h >= 4:
            d = np.hypot(xx - (w - 1) / 2, yy - (h - 1) / 2)
            rgb[d < min(w, h) / 2 - 0.4] = np.array(hexc('#e8dcc0'))
            rgb[(d < min(w, h) / 2 - 0.4) & (yy < h / 2) & (xx > w / 2)] = np.array(hexc('#c83a2a'))   # la zona roja
    elif nombre == 'aguja':
        rgb = np.full((h, w, 3), 20.0)
    elif nombre == 'engrane':
        rgb = np.array(hexc('#8a6a2a'), float) * (0.8 + 0.3 * ruido)
        if cara in ('east', 'west') and w >= 4 and h >= 4:
            d = np.hypot(xx - (w - 1) / 2, yy - (h - 1) / 2)
            ang = np.arctan2(yy - (h - 1) / 2, xx - (w - 1) / 2)
            dientes = (np.cos(ang * 8) > 0.2) & (d > min(w, h) / 2 - 1.2)
            rgb[dientes] *= 0.55
            rgb[d < 0.9] = np.array(hexc('#2a2018'))
    elif nombre == 'sangre':
        rgb = np.array(hexc('#6e0a0e'), float) * (0.7 + 0.5 * ruido)
        a = np.where(r.rand(h, w) < 0.82, 255, 0).astype(np.uint8)
    elif nombre == 'sangre_gotas':
        rgb = np.array(hexc('#7a0c10'), float) * (0.7 + 0.5 * ruido)
        gota = np.zeros((h, w), bool)
        for x in range(w):
            largo = r.randint(0, h + 1)
            gota[:largo, x] = r.rand() < 0.6
        a = np.where(gota, 255, 0).astype(np.uint8)
    else:
        rgb = np.full((h, w, 3), 200.0)
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
            img.paste(Image.fromarray(mat(c['mat'], cw, ch, cara, 900 + i * 7 + len(cara)), 'RGBA'), (x, y))
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
        'description': {'identifier': 'geometry.coleccionista.maquina', 'texture_width': TW, 'texture_height': TH,
                        'visible_bounds_width': 3, 'visible_bounds_height': 3, 'visible_bounds_offset': [0, 1, 0]},
        'bones': bones}]}


def anims():
    return {'format_version': '1.8.0', 'animations': {
        'animation.maquina.idle': {'loop': True, 'animation_length': 4.0, 'bones': {}},
    }}


def main():
    modelo()
    empacar()
    tex = textura()
    for sub in ('geo', 'animations', os.path.join('textures', 'entity')):
        os.makedirs(os.path.join(A, sub), exist_ok=True)
    json.dump(geo(), open(os.path.join(A, 'geo', 'maquina.geo.json'), 'w'), indent=1)
    json.dump(anims(), open(os.path.join(A, 'animations', 'maquina.animation.json'), 'w'), indent=1)
    tex.save(os.path.join(A, 'textures', 'entity', 'maquina.png'))
    print(len(HUESOS), 'huesos', len(CUBOS), 'cubos')


if __name__ == '__main__':
    main()
