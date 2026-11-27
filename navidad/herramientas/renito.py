#!/usr/bin/env python3
"""El renito de nariz roja, hecho a mano como reno BEBÉ (antes era el reno grande en chiquito): cabeza grande, ojos grandes con brillo,
orejas grandes, cuernitos de terciopelo, hocico clarito con la nariz roja que brilla, cuerpo corto con manchitas blancas de cervatillo,
patitas delgadas con pezuñas oscuras, pecho y colita esponjados, y su collar rojo con cascabel (solo cuando es tuyo).
Usa los mismos huesos que el reno (body, neck, head, jaw, ear_*, front_*/back_* con su parte baja, tail1-3, antler_*, nariz, collar,
eyes) para que sirvan sus animaciones (se escriben aparte con herramientas/criaturas.py, derivadas del reno).
Uso: python3 herramientas/renito.py  (después de criaturas.py, que escribe las animaciones)"""
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
from geo import Creature  # noqa: E402

A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


CAFE, CAFE_OSC, CAFE_CLARO = (164, 110, 66), (118, 76, 42), (196, 146, 96)
CREMA, BLANCO = (238, 220, 190), (248, 244, 236)
PEZUNA, NEGRO, OJO = (60, 42, 30), (26, 20, 18), (24, 16, 14)
ROJO, ROJO_OSC = (230, 30, 36), (170, 14, 22)
TERCIOPELO, TERCIOPELO_OSC = (150, 118, 86), (116, 88, 62)
ORO = (236, 186, 58)


def pelo(face, x, y, w, h):
    """Café con la panza clarita y manchitas blancas de cervatillo en el lomo y los lados."""
    if face == 'bottom':
        return CREMA
    if face == 'top':
        if (x * 3 + y * 5) % 11 == 0 and 1 <= x <= w - 2:
            return BLANCO
        return CAFE_OSC if abs(x - (w - 1) / 2) < 0.6 else CAFE
    if face in ('right', 'left'):
        if y >= h - 2:
            return CREMA if y == h - 1 else mix(CAFE, CREMA, 0.5)
        if y <= 2 and (x * 5 + y * 3) % 9 == 0:
            return BLANCO
    return CAFE if (x + y) % 5 else CAFE_CLARO


def pecho(face, x, y, w, h):
    return BLANCO if (x + y) % 3 else CREMA


def cabeza(face, x, y, w, h):
    if face == 'top':
        return CAFE_OSC if abs(x - (w - 1) / 2) < 1 else CAFE
    if face == 'bottom':
        return CREMA
    if face == 'front':
        # la frente: un mechón más oscuro al centro
        return CAFE_OSC if abs(x - (w - 1) / 2) < 1 and y < 3 else CAFE
    return CAFE if (x + y) % 6 else CAFE_CLARO


def hocico(face, x, y, w, h):
    if face == 'bottom':
        return BLANCO
    return CREMA if (x + y) % 4 else BLANCO


def nariz(face, x, y, w, h):
    if face == 'top' or (y == 0 and x == 0):
        return (255, 140, 140)
    return ROJO if (x + y) % 3 else ROJO_OSC


def pata(face, x, y, w, h):
    return CAFE if (x + y) % 4 else CAFE_OSC


def pata_baja(face, x, y, w, h):
    if y >= h - 1 or face == 'bottom':
        return PEZUNA
    return CAFE_OSC if y == h - 2 else CAFE


def oreja(face, x, y, w, h):
    if face == 'bottom':
        return (236, 170, 160)    # por dentro rosita
    return CAFE if (x + y) % 3 else CAFE_CLARO


def cuerno(face, x, y, w, h):
    return TERCIOPELO if (x + y) % 2 else TERCIOPELO_OSC


def ojo(face, x, y, w, h):
    # ojo grande de 2x2: negro con un brillito blanco arriba y pestañita
    if x == 0 and y == 0:
        return (255, 255, 255)
    return OJO


def collar(face, x, y, w, h):
    return ROJO if (x + y) % 4 else ROJO_OSC


def colita(face, x, y, w, h):
    return BLANCO if (x + y) % 2 else CREMA


c = Creature('geometry.navidad.renito', 128, 64, seed=1224)
c.bone('root', [0, 0, 0])
c.bone('body', [0, 13, 2], 'root')
c.part('body', [-3.5, 9.5, -4], [7, 5.5, 10], pelo)                     # cuerpo corto
c.part('body', [-3, 9.2, -4.4], [6, 4.8, 1.0], pecho)                   # pecho esponjado
c.part('body', [-3, 14.8, -3], [6, 0.6, 8], pelo)                       # lomo redondito
c.bone('neck', [0, 15, -6], 'body')
c.part('neck', [-1.8, 12.4, -7.6], [3.6, 7.0, 3.4], pelo, rotation=[22, 0, 0], pivot=[0, 15, -5.5])
c.part('neck', [-1.4, 12.6, -8.2], [2.8, 4.6, 1.0], pecho, rotation=[22, 0, 0], pivot=[0, 15, -5.5])
c.bone('head', [0, 16, -8.5], 'neck')
c.part('head', [-3.4, 16.4, -12.0], [6.8, 6.2, 6.2], cabeza)            # cabezota
c.part('head', [-2.0, 15.4, -15.2], [4.0, 3.4, 3.4], hocico)            # hocico
c.bone('jaw', [0, 15.4, -12.6], 'head')
c.part('jaw', [-1.6, 14.8, -14.8], [3.2, 0.8, 3.0], hocico)
c.bone('nariz', [0, 17.4, -15.5], 'head')
c.part('nariz', [-1.1, 16.8, -16.2], [2.2, 1.7, 1.2], nariz)
c.glow('nariz')
c.bone('eyes', [0, 19.6, -12.1], 'head')
for s in (-1, 1):
    c.part('eyes', [2.1 * s - 1, 18.6, -12.1], [2, 2, 0.1], ojo)        # ojos grandes al frente
c.part('eyes', [-3.0, 20.7, -12.15], [1.6, 0.35, 0.1], lambda f, x, y, w, h: CAFE_OSC)   # cejitas
c.part('eyes', [1.4, 20.7, -12.15], [1.6, 0.35, 0.1], lambda f, x, y, w, h: CAFE_OSC)
for lado, s in (('r', -1), ('l', 1)):
    e = 'ear_' + lado
    c.bone(e, [2.6 * s, 21.4, -9.6], 'head')
    x0 = 3.0 if s > 0 else -6.6
    c.part(e, [x0, 21.0, -10.4], [3.6, 1.0, 2.2], oreja, rotation=[0, 0, 20 * s], pivot=[2.6 * s, 21.4, -9.6])
    a = 'antler_' + lado
    c.bone(a, [1.6 * s, 22.4, -9.2], 'head')
    c.part(a, [1.6 * s - 0.5, 22.4, -9.7], [1.0, 2.6, 1.0], cuerno, rotation=[-10, 0, 14 * s], pivot=[1.6 * s, 22.4, -9.2])
    c.part(a, [1.6 * s + (0.3 if s > 0 else -1.3), 23.6, -9.6], [1.0, 1.4, 0.8], cuerno, rotation=[0, 0, 40 * s],
           pivot=[1.6 * s, 23.6, -9.2])
    for pierna, z in (('front', -2.6), ('back', 4.4)):
        b = f'{pierna}_{lado}'
        c.bone(b, [2.2 * s, 10, z], 'body')
        c.part(b, [2.2 * s - 0.9, 5, z - 0.9], [1.8, 5.5, 1.8], pata)
        c.bone(b + '_low', [2.2 * s, 5, z], b)
        c.part(b + '_low', [2.2 * s - 0.8, 0, z - 0.8], [1.6, 5.2, 1.6], pata_baja)
c.bone('tail1', [0, 14, 6], 'body')
c.part('tail1', [-1.1, 12.6, 5.8], [2.2, 2.4, 1.8], colita, rotation=[30, 0, 0], pivot=[0, 14, 6])
c.bone('tail2', [0, 14, 7.5], 'tail1')
c.bone('tail3', [0, 14, 8.5], 'tail2')
c.bone('collar', [0, 13, -7.5], 'neck')
c.part('collar', [-2.2, 14.4, -8.2], [4.4, 1.1, 4.0], collar, rotation=[22, 0, 0], pivot=[0, 15, -5.5])
c.part('collar', [-0.7, 13.0, -8.9], [1.4, 1.4, 1.4], lambda f, x, y, w, h: ORO if (x + y) % 2 else (255, 230, 140),
       rotation=[22, 0, 0], pivot=[0, 15, -5.5])
c.build(os.path.join(A, 'geo', 'renito.geo.json'), os.path.join(A, 'textures', 'entity', 'renito.png'))
print('renito listo')
