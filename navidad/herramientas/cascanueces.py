#!/usr/bin/env python3
"""El Cascanueces guardián (modelo hecho a mano, textura y animaciones): un soldadito de madera pintada que cuida tu árbol.

Gorro alto de piel negra con banda dorada y penacho rojo, cara de madera con cachetes chapeados, ojos pintados, bigote y barba blancos
y la quijada cuadrada que se abre (hueso "quijada"); casaca roja con botones dorados, charreteras y banda blanca cruzada; pantalón
azul, botas negras; en la mano derecha un sable de bastón de caramelo.
Animaciones: idle (firme, respira y de vez en cuando truena la quijada), walk (marcha con las piernas tiesas y los brazos
balanceándose), attack (sablazo y mordida), salute (saludo militar), hurt y death (se cae tieso como juguete).
Uso: python3 herramientas/cascanueces.py"""
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
from geo import Anim, Creature, save_anims  # noqa: E402

A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')
S = 'easeInOutSine'


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


ROJO, ROJO_OSC, ROJO_CLARO = (196, 26, 34), (136, 12, 22), (226, 66, 66)
AZUL, AZUL_OSC = (40, 70, 160), (26, 46, 110)
NEGRO, NEGRO2 = (26, 24, 28), (54, 50, 58)
ORO, ORO_OSC, ORO_CLARO = (230, 180, 56), (160, 110, 26), (255, 232, 140)
BLANCO, BLANCO_OSC = (246, 246, 250), (214, 218, 228)
MADERA, MADERA_OSC = (234, 196, 150), (206, 160, 112)      # la cara de madera pintada
CACHETE = (228, 120, 110)


def lados(c, top=None, bottom=None, front=None):
    def f(face, x, y, w, h):
        if face == 'top':
            return top or c
        if face == 'bottom':
            return bottom or c
        if face == 'front' and front:
            return front
        return c
    return f


def gorro(face, x, y, w, h):
    # piel negra con vetas
    if face == 'top':
        return NEGRO2
    return NEGRO2 if (x * 3 + y) % 5 == 0 else NEGRO


def banda(face, x, y, w, h):
    return ORO_CLARO if (x + y) % 3 == 0 else ORO


def cara(face, x, y, w, h):
    if face == 'front':
        # ojos pintados grandes, cejas, cachetes chapeados
        if y == 1 and x in (0, 1, w - 2, w - 1):
            return BLANCO                       # cejas pobladas
        if y == 2 and x in (1, w - 2):
            return (40, 80, 170)                # ojos azules (con su pupila abajo)
        if y == 3 and x in (1, w - 2):
            return NEGRO
        if y == 4 and x in (0, 1, w - 2, w - 1):
            return CACHETE
        return MADERA
    if face == 'top':
        return BLANCO
    if face in ('right', 'left'):
        return BLANCO if x >= w // 2 else MADERA_OSC
    return BLANCO


def quijada(face, x, y, w, h):
    # la quijada de madera con la barba blanca encima y los dientes pintados arriba
    if face == 'front' and y == 0:
        return BLANCO if x % 2 == 0 else (230, 230, 230)
    if face == 'top':
        return (160, 30, 40)                    # boca por dentro
    return MADERA if face == 'front' and y == 1 else BLANCO if (x + y) % 3 else BLANCO_OSC


def bigote(face, x, y, w, h):
    return BLANCO if (x + y) % 3 else BLANCO_OSC


def nariz(face, x, y, w, h):
    return mix(MADERA, CACHETE, 0.45)


def casaca(face, x, y, w, h):
    if face == 'front':
        cx = (w - 1) / 2
        if abs(x - cx) < 0.6 and y % 2 == 1 and y < h - 1:
            return ORO_CLARO                    # botones
        if abs(x - cx) < 1.6 and y < h - 1:
            return mix(ROJO, ROJO_OSC, 0.25)    # la línea del cierre
        # banda blanca cruzada (del hombro izquierdo a la cadera derecha)
        if abs((x - w + 1) + y * (w / h)) < 1.1:
            return BLANCO
    if y == h - 1:
        return NEGRO                            # cinturón
    if face == 'top':
        return ROJO_CLARO
    return ROJO if (x + y) % 6 else ROJO_CLARO


def manga(face, x, y, w, h):
    if y >= h - 1:
        return ORO                              # puño dorado
    return ROJO if (x + y) % 5 else ROJO_CLARO


def charretera(face, x, y, w, h):
    return ORO_CLARO if (x + y) % 2 else ORO


def guante(face, x, y, w, h):
    return BLANCO if (x + y) % 4 else BLANCO_OSC


def pantalon(face, x, y, w, h):
    if face in ('right', 'left') and abs(x - (w - 1) / 2) < 0.6:
        return ORO                              # franja dorada al lado
    return AZUL if (x + y) % 5 else AZUL_OSC


def bota(face, x, y, w, h):
    if y == 0 and face != 'bottom':
        return NEGRO2
    return NEGRO if (x + y) % 4 else NEGRO2


def sable(face, x, y, w, h):
    return (244, 244, 248) if (y + x) % 4 < 2 else (214, 30, 40)


def mango_sable(face, x, y, w, h):
    return ORO if (x + y) % 2 else ORO_OSC


c = Creature('geometry.navidad.cascanueces', 128, 64, seed=1206)
c.bone('root', [0, 0, 0])
c.bone('cuerpo', [0, 12, 0], 'root')
c.part('cuerpo', [-4, 12, -2.5], [8, 9, 5], casaca)                                   # casaca
c.part('cuerpo', [-4.3, 11, -2.8], [8.6, 1.5, 5.6], lados(ROJO_OSC, ROJO))            # faldón
c.part('cuerpo', [-1, 15.5, -2.9], [2, 1.6, 0.4], lados(ORO, ORO_CLARO, ORO_OSC))      # hebilla de la banda
c.bone('cabeza', [0, 21, 0], 'cuerpo')
c.part('cabeza', [-3, 21, -3], [6, 6, 6], cara)
c.part('cabeza', [-0.6, 23.2, -3.7], [1.2, 1.4, 0.8], nariz)
c.part('cabeza', [-3.2, 22.6, -3.4], [3.0, 0.9, 0.6], bigote, rotation=[0, 0, 10], pivot=[-0.2, 23, -3.2])
c.part('cabeza', [0.2, 22.6, -3.4], [3.0, 0.9, 0.6], bigote, rotation=[0, 0, -10], pivot=[0.2, 23, -3.2])
c.part('cabeza', [-3.2, 22, -1], [6.4, 4.6, 4.2], lados(BLANCO, BLANCO, BLANCO_OSC))  # pelo blanco a los lados y atrás
c.bone('quijada', [0, 22, -1.5], 'cabeza')
c.part('quijada', [-2.6, 19.2, -3.3], [5.2, 2.8, 3.2], quijada)                       # quijada cuadrada con barba
c.part('quijada', [-2.2, 18.2, -3.1], [4.4, 1.2, 2.4], bigote)                        # puntita de la barba
c.bone('gorro', [0, 27, 0], 'cabeza')
c.part('gorro', [-3.3, 27, -3.3], [6.6, 7, 6.6], gorro)
c.part('gorro', [-3.5, 26.8, -3.5], [7.0, 1.4, 7.0], banda)
c.part('gorro', [-3.4, 32.4, -3.4], [6.8, 0.8, 6.8], banda)
c.part('gorro', [-1.6, 28.4, -3.6], [3.2, 2.6, 0.4], lados(ORO, ORO_CLARO, ORO_CLARO))   # escudo dorado
c.bone('penacho', [2.4, 34, 0], 'gorro')
c.part('penacho', [1.8, 33.4, -0.6], [1.2, 4.2, 1.2], lados(ROJO, ROJO_CLARO), rotation=[0, 0, -12], pivot=[2.4, 33.4, 0])
c.part('penacho', [1.9, 37.2, -0.7], [1.4, 1.4, 1.4], lados(BLANCO), rotation=[0, 0, -12], pivot=[2.4, 33.4, 0])
for lado, s in (('der', -1), ('izq', 1)):
    b = f'brazo_{lado}'
    c.bone(b, [5 * s, 20, 0], 'cuerpo')
    x0 = 4 if s > 0 else -6.6
    c.part(b, [x0, 13.2, -1.3], [2.6, 7.2, 2.6], manga)
    c.part(b, [x0 - 0.1, 12.0, -1.4], [2.8, 1.3, 2.8], guante)
    c.part(b, [x0 - 0.4, 19.6, -1.6], [3.4, 1.0, 3.2], charretera)                   # charretera
    p = f'pierna_{lado}'
    c.bone(p, [2 * s, 11, 0], 'root')
    x1 = 0.2 if s > 0 else -3.8
    c.part(p, [x1, 2.5, -1.7], [3.6, 9, 3.4], pantalon)
    c.part(p, [x1 - 0.2, 0, -2.4], [4.0, 3, 4.2], bota)
# el sable de bastón de caramelo en la mano derecha (sale hacia adelante)
c.bone('sable', [-5.3, 12.6, 0], 'brazo_der')
c.part('sable', [-5.8, 11.8, -1.6], [1.0, 1.6, 2.2], mango_sable)                      # empuñadura
c.part('sable', [-6.3, 12.3, -2.1], [2.0, 0.6, 0.6], mango_sable)                      # guarda
c.part('sable', [-5.6, 12.2, -11], [0.6, 0.8, 9.4], sable)                             # hoja
c.part('sable', [-5.6, 12.2, -11.4], [0.6, 2.0, 0.6], sable)                           # gancho del bastón
c.build(os.path.join(A, 'geo', 'cascanueces.geo.json'), os.path.join(A, 'textures', 'entity', 'cascanueces.png'))

# ================================================================ animaciones
an = []
L = 3.0
a = Anim('idle', L, True)
a.scale('cuerpo', {0: ([1, 1, 1], S), 1.5: ([1.015, 1.01, 1.015], S), 3.0: ([1, 1, 1], S)})
a.rot('quijada', {0: [0, 0, 0], 2.2: [0, 0, 0], 2.3: ([18, 0, 0], S), 2.4: ([0, 0, 0], S), 2.5: ([18, 0, 0], S), 2.6: ([0, 0, 0], S),
                  3.0: [0, 0, 0]})
a.rot('penacho', {0: ([0, 0, 0], S), 1.5: ([4, 0, 3], S), 3.0: ([0, 0, 0], S)})
a.rot('brazo_der', [-25, 0, 0])
an.append(a)
L = 0.8
a = Anim('walk', L, True)
for lado, s in (('der', 1), ('izq', -1)):
    a.rot(f'pierna_{lado}', {0: ([-38 * s, 0, 0], S), L / 2: ([38 * s, 0, 0], S), L: ([-38 * s, 0, 0], S)})
a.rot('brazo_izq', {0: ([32, 0, 0], S), L / 2: ([-32, 0, 0], S), L: ([32, 0, 0], S)})
a.rot('brazo_der', [-25, 0, 0])
a.pos('cuerpo', {0: ([0, 0, 0], S), L / 4: ([0, 0.6, 0], S), L / 2: ([0, 0, 0], S), 3 * L / 4: ([0, 0.6, 0], S), L: ([0, 0, 0], S)})
a.rot('cuerpo', {0: ([0, 0, 3], S), L / 2: ([0, 0, -3], S), L: ([0, 0, 3], S)})
a.rot('penacho', {0: ([0, 0, -8], S), L / 2: ([0, 0, 8], S), L: ([0, 0, -8], S)})
an.append(a)
L = 0.6
a = Anim('attack', L)
a.rot('brazo_der', {0: ([-25, 0, 0], S), 0.18: ([-150, 0, 20], S), 0.34: ([-20, 0, -15], 'easeInQuad'), L: ([-25, 0, 0], S)})
a.rot('cuerpo', {0: ([0, 0, 0], S), 0.18: ([0, -15, 0], S), 0.34: ([6, 18, 0], S), L: ([0, 0, 0], S)})
a.rot('quijada', {0: [0, 0, 0], 0.22: ([25, 0, 0], S), 0.32: ([0, 0, 0], S), 0.42: ([25, 0, 0], S), 0.5: ([0, 0, 0], S)})
an.append(a)
L = 1.6
a = Anim('salute', L)
a.rot('brazo_izq', {0: ([0, 0, 0], S), 0.3: ([-160, 0, -40], S), 1.2: ([-160, 0, -40], S), L: ([0, 0, 0], S)})
a.rot('cabeza', {0: ([0, 0, 0], S), 0.3: ([-6, 0, 0], S), 1.2: ([-6, 0, 0], S), L: ([0, 0, 0], S)})
a.pos('cuerpo', {0: ([0, 0, 0], S), 0.3: ([0, 0.6, 0], S), 1.2: ([0, 0.6, 0], S), L: ([0, 0, 0], S)})
an.append(a)
a = Anim('hurt', 0.35)
a.rot('cuerpo', {0: [0, 0, 0], 0.08: ([-12, 0, 6], 'easeOutQuad'), 0.35: [0, 0, 0]})
a.rot('quijada', {0: [0, 0, 0], 0.08: ([30, 0, 0], S), 0.35: [0, 0, 0]})
an.append(a)
a = Anim('death', 0.9, 'hold')
a.rot('root', {0: [0, 0, 0], 0.7: ([-90, 0, 0], 'easeInQuad'), 0.8: ([-84, 0, 0], S), 0.9: ([-90, 0, 0], S)})
a.rot('quijada', {0: [0, 0, 0], 0.7: ([30, 0, 0], S)})
an.append(a)
save_anims('cascanueces', an, os.path.join(A, 'animations', 'cascanueces.animation.json'))
print('cascanueces listo')
