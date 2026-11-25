#!/usr/bin/env python3
"""La escopeta de la caja de música (capítulo 3): una escopeta vieja de dos cañones, de latón grabado y madera oscura, en la punta de un
brazo mecánico que sale de un foso bajo el escenario (como un juguete de cuerda gigante: lleva una llave de cuerda que gira).

Escribe geo/escopeta.geo.json, animations/escopeta.animation.json y textures/entity/escopeta.png.
La entidad está en el fondo del foso (y = 0 del modelo). Arriba del todo, el eje del arma queda a 78 px (4.9 bloques): un poco arriba de
las cabezas de los que están sentados en el círculo. Guardada, el mástil baja 61 px y el arma queda parada (apuntando al techo) dentro
del foso, debajo de la compuerta. El cañón apunta hacia el frente (-z).
Huesos: base (fija, con su engrane), mastil (sube y baja; con la cremallera), giro (la vuelta extra al salir), codo (se inclina: el pitch),
arma (el culatazo), canones (se quiebran para cargar), martillos, llave (la llave de cuerda) y fogonazo (brilla; solo al disparar).

Uso: python3 herramientas/escopeta.py
"""
import json
import math
import os

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista')
TW, TH = 128, 256
HUESOS = []
CUBOS = []
P = 78          # la altura del eje del arma (arriba del todo)
T = P - 18      # la parte de arriba de la tornamesa (la horquilla es alta: el arma cabe parada)


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
    # la base, en el fondo del foso: una caja de hierro remachada con un engrane grande a cada lado
    hueso('base', 'raiz', (0, 0, 0))
    cubo('base', (-8, 0, -8), (16, 6, 16), 'hierro')
    cubo('base', (-9, 0, -9), (18, 1, 18), 'hierro_osc')
    cubo('base', (-4, 6, -4), (8, 1, 8), 'laton')
    hueso('engrane', 'base', (8.5, 3, 0))
    cubo('engrane', (8.2, -1, -4), (0.8, 8, 8), 'cobre')
    cubo('engrane', (8.2, -1, -4), (0.8, 8, 8), 'cobre', giro=(45, 0, 0), pivote=(8.6, 3, 0))
    cubo('engrane', (8.0, 1.5, -1.5), (1.2, 3, 3), 'laton')
    hueso('engrane2', 'base', (-8.5, 3, 0))
    cubo('engrane2', (-9.0, -1, -4), (0.8, 8, 8), 'cobre')
    cubo('engrane2', (-9.0, -1, -4), (0.8, 8, 8), 'cobre', giro=(45, 0, 0), pivote=(-8.6, 3, 0))
    cubo('engrane2', (-9.2, 1.5, -1.5), (1.2, 3, 3), 'laton')

    # el mástil: una columna de hierro con anillos de latón y una cremallera (los dientes) por delante
    hueso('mastil', 'raiz', (0, 0, 0))
    cubo('mastil', (-3, -70, -3), (6, 62, 6), 'hierro')
    cubo('mastil', (-3, -8, -3), (6, T - 4 + 8, 6), 'hierro')
    for y in range(-62, T - 6, 12):
        cubo('mastil', (-3, y, -3), (6, 1.5, 6), 'laton', inflar=0.3)
    for y in range(-68, T - 6, 3):
        cubo('mastil', (-1.5, y, -4), (3, 1.2, 1), 'hierro_osc')
    # la tornamesa: un plato de latón con terciopelo rojo, como la de una caja de música
    cubo('mastil', (-5, T - 4, -5), (10, 3, 10), 'laton')
    cubo('mastil', (-4.5, T - 1, -4.5), (9, 1, 9), 'terciopelo')

    # lo que gira (la horquilla alta que sostiene el arma)
    hueso('giro', 'mastil', (0, T, 0))
    cubo('giro', (-3.5, T, -3.5), (7, 2, 7), 'hierro_osc')
    for x in (-4.2, 3.0):
        cubo('giro', (x, T + 1, -1), (1.2, P - T + 1, 2), 'hierro')
        cubo('giro', (x - 0.2, P - 1.5, -1.5), (1.6, 3, 3), 'laton')
    cubo('giro', (-4.2, T + 1, -1), (8.4, 1.4, 2), 'hierro')

    hueso('codo', 'giro', (0, P, 0))
    hueso('arma', 'codo', (0, P, 0))
    # el cajón de los mecanismos (latón grabado), el gatillo, el guardamonte y la culata de madera oscura
    cubo('arma', (-2.4, P - 2.6, -1), (4.8, 5, 6), 'grabado')
    cubo('arma', (-0.3, P - 4.6, 1.6), (0.6, 2, 0.6), 'metal_osc')
    cubo('arma', (-0.3, P - 5.1, 0.4), (0.6, 0.5, 3.4), 'metal_osc')
    cubo('arma', (-1.8, P - 4.2, 4.6), (3.6, 5.6, 11), 'madera', giro=(-10, 0, 0), pivote=(0, P, 5))
    cubo('arma', (-1.9, P - 4.5, 15.3), (3.8, 6.0, 0.9), 'metal_osc', giro=(-10, 0, 0), pivote=(0, P, 5))
    cubo('arma', (-1.85, P - 1.0, 6.5), (3.7, 0.6, 5), 'laton', giro=(-10, 0, 0), pivote=(0, P, 5))
    # una calaverita de latón a cada lado del cajón
    for x in (-2.6, 2.4):
        cubo('arma', (x, P - 1.2, 1), (0.2, 2, 2), 'calavera')
    # la llave de cuerda (sale del lado derecho de la culata y gira sola)
    hueso('llave', 'arma', (2.2, P - 1, 8.5))
    cubo('llave', (2.0, P - 1.5, 8.0), (1.6, 1, 1), 'laton')
    cubo('llave', (3.6, P - 3.5, 7.7), (0.6, 5, 1.6), 'laton')
    cubo('llave', (3.6, P - 3.8, 7.3), (0.6, 1.4, 2.4), 'laton')
    cubo('llave', (3.6, P + 0.4, 7.3), (0.6, 1.4, 2.4), 'laton')
    # los martillos (atrás del cajón, echados para atrás: listos)
    hueso('martillos', 'arma', (0, P + 2.4, 4.4))
    for x in (-1.6, 0.8):
        cubo('martillos', (x, P + 2.2, 3.8), (0.8, 2, 1.2), 'metal_osc')
        cubo('martillos', (x, P + 3.8, 3.2), (0.8, 0.6, 1.2), 'metal_osc')
    # los dos cañones, con su costilla arriba, el guardamanos de madera abajo y las bocas negras
    hueso('canones', 'arma', (0, P - 1.6, -1))
    for x in (-2.5, 0.1):
        cubo('canones', (x, P - 1.2, -28), (2.4, 2.4, 27), 'metal')
        cubo('canones', (x + 0.4, P - 0.8, -28.05), (1.6, 1.6, 0.1), 'boca')
    cubo('canones', (-0.4, P + 1.2, -27.5), (0.8, 0.5, 26), 'metal_osc')
    cubo('canones', (-0.3, P + 1.7, -27.6), (0.6, 0.6, 0.8), 'laton')
    cubo('canones', (-2.0, P - 3.2, -15), (4, 2.2, 13), 'madera')
    cubo('canones', (-2.7, P - 1.6, -9), (5.4, 3.2, 1.2), 'laton')
    cubo('canones', (-2.7, P - 1.6, -21), (5.4, 3.2, 1.0), 'laton')
    # el fogonazo, en las bocas
    hueso('fogonazo', 'canones', (0, P, -28))
    cubo('fogonazo', (-3.5, P - 2.8, -36), (7, 5.6, 8), 'fuego')
    cubo('fogonazo', (-7, P - 0.2, -33), (14, 0.3, 3), 'fuego_claro')
    cubo('fogonazo', (-0.15, P - 6, -33), (0.3, 12, 3), 'fuego_claro')
    cubo('fogonazo', (-1.5, P - 1.4, -40), (3, 3, 4), 'fuego_claro')


# ---------------------------------------------------------------- textura

def hexc(c):
    c = c.lstrip('#')
    return [int(c[i:i + 2], 16) for i in (0, 2, 4)]


def mat(nombre, w, h, cara, s):
    r = np.random.RandomState(s)
    yy, xx = np.mgrid[0:h, 0:w]
    if nombre == 'madera':
        base = np.array(hexc('#3a2214'), float)
        vet = np.sin(yy * 1.3 + r.rand() * 6 + np.sin(xx * 0.4) * 1.2) * 0.5 + 0.5
        f = 0.75 + vet[..., None] * 0.35 + r.rand(h, w)[..., None] * 0.12
        rgb = base * f
    elif nombre == 'laton':
        rgb = np.array(hexc('#a37a30'), float) * (0.8 + 0.35 * r.rand(h, w)[..., None])
        rgb[0, :] = np.array(hexc('#e0bc62'))
    elif nombre == 'grabado':
        # latón con un grabado de volutas (líneas oscuras en espiral)
        rgb = np.array(hexc('#b08a3a'), float) * (0.85 + 0.2 * r.rand(h, w)[..., None])
        voluta = (np.sin(xx * 1.6 + np.cos(yy * 1.1) * 2.2) > 0.75)
        rgb[voluta] *= 0.55
        rgb[0, :] = np.array(hexc('#e6c46a'))
    elif nombre == 'calavera':
        rgb = np.array(hexc('#d8b45a'), float) * np.ones((h, w, 1))
        if w >= 2 and h >= 2:
            rgb[min(h - 1, h // 2 - 1 if h > 1 else 0), :] *= 0.4
    elif nombre in ('hierro', 'hierro_osc'):
        base = '#34343a' if nombre == 'hierro' else '#1e1e22'
        rgb = np.array(hexc(base), float) * (0.8 + 0.4 * r.rand(h, w)[..., None])
        oxido = r.rand(h, w) > 0.86
        rgb[oxido] = np.array(hexc('#5e3418'))
    elif nombre == 'cobre':
        rgb = np.array(hexc('#8a4e2a'), float) * (0.8 + 0.4 * r.rand(h, w)[..., None])
        verde = r.rand(h, w) > 0.85
        rgb[verde] = np.array(hexc('#3e7a66'))
    elif nombre == 'terciopelo':
        rgb = np.array(hexc('#6a0e18'), float) * (0.8 + 0.3 * r.rand(h, w)[..., None])
    elif nombre in ('metal', 'metal_osc'):
        base = np.array(hexc('#2a2e36' if nombre == 'metal' else '#18191d'), float)
        brillo = (yy == 0) * 0.7
        rgb = base * (0.9 + 0.25 * r.rand(h, w)[..., None] + brillo[..., None])
    elif nombre == 'boca':
        rgb = np.full((h, w, 3), 4.0)
    elif nombre == 'fuego':
        d = np.hypot((xx - w / 2 + 0.5) / max(1, w / 2), (yy - h / 2 + 0.5) / max(1, h / 2))
        rgb = np.stack([np.full_like(d, 255), 235 - 120 * d, 130 - 120 * d], -1)
    elif nombre == 'fuego_claro':
        rgb = np.stack([np.full((h, w), 255.0), np.full((h, w), 248.0), np.full((h, w), 210.0)], -1)
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
        'description': {'identifier': 'geometry.coleccionista.escopeta', 'texture_width': TW, 'texture_height': TH,
                        'visible_bounds_width': 5, 'visible_bounds_height': 10, 'visible_bounds_offset': [0, 3, 0]},
        'axelis_glow': ['fogonazo'],
        'bones': bones}]}


def anims():
    return {'format_version': '1.8.0', 'animations': {
        # quieta: nada se mueve solo (lo demás lo mueve el código: subir, girar, apuntar, cargar y disparar)
        'animation.escopeta.idle': {'loop': True, 'animation_length': 4.0, 'bones': {}},
    }}


def main():
    modelo()
    empacar()
    tex = textura()
    for sub in ('geo', 'animations', os.path.join('textures', 'entity')):
        os.makedirs(os.path.join(A, sub), exist_ok=True)
    json.dump(geo(), open(os.path.join(A, 'geo', 'escopeta.geo.json'), 'w'), indent=1)
    json.dump(anims(), open(os.path.join(A, 'animations', 'escopeta.animation.json'), 'w'), indent=1)
    tex.save(os.path.join(A, 'textures', 'entity', 'escopeta.png'))
    print(len(HUESOS), 'huesos', len(CUBOS), 'cubos')


if __name__ == '__main__':
    main()
