#!/usr/bin/env python3
"""El reno adulto (el que se monta, el que jala el trineo de Santa y Cometa, el de Ramiro), hecho a mano: cuerpo café oscuro con la
panza clara, la melena color crema que le cuelga del cuello (como los renos de verdad), cabeza con hocico oscuro y nariz negra, orejas
de lado, cuernos grandes con varias puntas (y una que cae al frente), patas largas con una franja blanca arriba de la pezuña y colita
blanca. Mismos huesos que antes (para sus animaciones, que salen de herramientas/criaturas.py) y el lomo a la misma altura (el jinete).
Uso: python3 herramientas/reno.py"""
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
from geo import Creature  # noqa: E402

A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


CAFE, CAFE_OSC, CAFE_CLARO = (122, 82, 50), (86, 56, 34), (150, 106, 68)
GRIS = (168, 150, 128)
CREMA, CREMA_OSC = (236, 226, 204), (210, 196, 170)
HOCICO, NARIZ = (112, 84, 64), (24, 18, 16)
PEZUNA, OJO = (40, 30, 24), (20, 14, 12)
ASTA, ASTA_OSC, ASTA_CLARO = (190, 160, 118), (150, 122, 86), (222, 200, 160)


def pelo(face, x, y, w, h):
    if face == 'bottom':
        return CREMA_OSC
    if face == 'top':
        return CAFE_OSC if abs(x - (w - 1) / 2) < 0.6 else CAFE if (x + y) % 5 else CAFE_CLARO
    if face in ('right', 'left') and y >= h - 2:
        return mix(CAFE, GRIS, 0.6) if y == h - 2 else GRIS
    return CAFE if (x * 2 + y) % 7 else CAFE_CLARO


def melena(face, x, y, w, h):
    # pelo largo color crema, con mechones
    if y == h - 1 and x % 2 == 0:
        return CREMA_OSC
    return CREMA if (x + y * 2) % 5 else CREMA_OSC


def cabeza(face, x, y, w, h):
    """Cabeza (6x6x6) con los ojos a los lados, como los renos de verdad: ojo de 2x2 café muy oscuro con brillo, párpado oscuro arriba
    y el pelo clarito alrededor; la frente con su mechón más oscuro."""
    if face == 'front':
        return CAFE_OSC if abs(x - (w - 1) / 2) < 1 else CAFE
    if face == 'bottom':
        return CREMA_OSC
    if face in ('right', 'left'):
        # columnas desde el frente: 0 = la orilla de enfrente
        k = (w - 1 - x) if face == 'right' else x
        if y in (1, 2) and k in (1, 2):
            if y == 1 and k == 1:
                return (235, 235, 228)          # brillito
            return OJO if k == 1 or y == 2 else (58, 36, 24)
        if y == 0 and k in (1, 2, 3):
            return (64, 42, 26)                 # párpado / ceja oscura
        if (y == 3 and k in (1, 2, 3)) or (y in (1, 2) and k == 3):
            return CAFE_CLARO                   # pelo clarito alrededor del ojo
    return CAFE if (x + y) % 6 else CAFE_CLARO


def hocico(face, x, y, w, h):
    if face == 'front' and y <= 1 and 0 < x < w - 1:
        return NARIZ
    if face == 'bottom':
        return CREMA_OSC
    return HOCICO if (x + y) % 3 else mix(HOCICO, CAFE, 0.4)


def pata(face, x, y, w, h):
    return CAFE_OSC if (x + y) % 4 == 0 else CAFE


def pata_baja(face, x, y, w, h):
    if y >= h - 1 or face == 'bottom':
        return PEZUNA
    if y == h - 2:
        return CREMA        # la franja blanca arriba de la pezuña
    return CAFE_OSC if (x + y) % 3 == 0 else CAFE


def oreja(face, x, y, w, h):
    if face == 'bottom':
        return (210, 170, 150)
    return CAFE if (x + y) % 3 else CAFE_CLARO


def asta(face, x, y, w, h):
    if face == 'top':
        return ASTA_CLARO
    return ASTA if (x + y) % 3 else ASTA_OSC


def ojo(face, x, y, w, h):
    return (255, 255, 255) if x == 0 and y == 0 else OJO


def colita(face, x, y, w, h):
    return CREMA if (x + y) % 2 else (248, 244, 236)


c = Creature('geometry.navidad.reno', 128, 128, seed=1223)
c.bone('root', [0, 0, 0])
c.bone('body', [0, 13, 2], 'root')
c.part('body', [-4.5, 9, -6.5], [9, 8, 15], pelo)
c.part('body', [-5, 10.5, -7], [10, 6, 4], pelo)                       # hombros
c.part('body', [-3.5, 9, -7.5], [7, 3, 6], melena)                     # melena del pecho
c.bone('neck', [0, 15, -6], 'body')
c.part('neck', [-2.5, 13, -9.5], [5, 8, 4], pelo, rotation=[24, 0, 0], pivot=[0, 15, -6])
c.part('neck', [-2.5, 11, -10.5], [5, 6, 3], melena, rotation=[24, 0, 0], pivot=[0, 15, -6])    # melena que cuelga
c.bone('head', [0, 16, -8.5], 'neck')
c.part('head', [-3, 17, -14.5], [6, 6, 6], cabeza)
c.part('head', [-2, 17, -18.5], [4, 3, 4], hocico)
c.bone('jaw', [0, 17, -14], 'head')
c.part('jaw', [-2, 16.2, -18.2], [4, 1, 4], lambda f, x, y, w, h: CREMA_OSC if f == 'bottom' else HOCICO)
c.bone('eyes', [0, 21, -14.5], 'head')      # (los ojos van pintados en la cabeza; el hueso se queda para las animaciones)
for lado, s_ in (('r', -1), ('l', 1)):
    s = s_
    e = 'ear_' + lado
    c.bone(e, [3 * s, 22.5, -9.5], 'head')      # orejas atrás de los ojos
    c.part(e, [3 if s > 0 else -6, 22, -10.5], [3, 1, 2], oreja, rotation=[0, 0, 15 * s], pivot=[3 * s, 22.5, -9.5])
    a = 'antler_' + lado
    c.bone(a, [1.8 * s, 22.6, -11.2], 'head')
    bx = 1.8 * s
    # tronco del cuerno: sube, se abre hacia el lado y hacia atrás, con puntas al frente
    c.part(a, [bx - 0.5, 22.6, -11.7], [1, 5, 1], asta, rotation=[-20, 0, 18 * s], pivot=[bx, 22.6, -11.2])
    c.part(a, [bx + (1.0 if s > 0 else -2.0), 26.2, -10.6], [1, 4, 1], asta, rotation=[-30, 0, 28 * s], pivot=[bx, 26.2, -10.2])
    c.part(a, [bx + (2.4 if s > 0 else -3.4), 29.0, -8.6], [1, 3, 1], asta, rotation=[-10, 0, 10 * s], pivot=[bx, 29, -8.2])
    c.part(a, [bx + (0.8 if s > 0 else -1.8), 27.0, -13.4], [1, 1, 3], asta, rotation=[-25, 0, 0], pivot=[bx, 27, -11])     # punta al frente
    c.part(a, [bx + (2.0 if s > 0 else -3.0), 29.4, -11.6], [1, 2, 1], asta, rotation=[10, 0, 20 * s], pivot=[bx, 29.4, -11])
    c.part(a, [bx + (3.0 if s > 0 else -4.0), 31.4, -8.6], [1, 2, 1], asta, rotation=[-15, 0, 25 * s], pivot=[bx, 31.4, -8.2])
    c.part(a, [bx - 0.5, 23.4, -14.2], [1, 1, 3], asta, rotation=[20, 0, 0], pivot=[bx, 23.4, -11.6])                       # la que cae al frente
    for pierna, z in (('front', -4), ('back', 6)):
        b = f'{pierna}_{lado}'
        c.bone(b, [2.6 * s, 10, z], 'body')
        c.part(b, [2.6 * s - 1.5, 5, z - 1.5], [3, 5, 3], pata)
        c.bone(b + '_low', [2.6 * s, 5, z], b)
        c.part(b + '_low', [2.6 * s - 1, 0, z - 1], [2, 5, 2], pata_baja)
c.bone('tail1', [0, 15, 8.5], 'body')
c.part('tail1', [-1, 13.5, 8.4], [2, 3, 2], colita, rotation=[25, 0, 0], pivot=[0, 15, 8.5])
c.bone('tail2', [0, 15, 10], 'tail1')
c.bone('tail3', [0, 15, 11], 'tail2')
c.build(os.path.join(A, 'geo', 'reno.geo.json'), os.path.join(A, 'textures', 'entity', 'reno.png'))
print('reno listo')
