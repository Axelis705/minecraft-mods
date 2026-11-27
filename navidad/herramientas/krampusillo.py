#!/usr/bin/env python3
"""El krampusillo: diablito chiquito de cabra que trabaja para el Krampus. Cabezón, con cuernitos, orejas largas y puntiagudas,
sonrisa de dientes chuecos y ojos amarillos que brillan; pelo café rojizo desgreñado, patitas de cabra, cola de diablo con punta de
flecha y garritas. Lleva los regalos robados en la mano (hueso right_item).
Animaciones: idle, walk, run, attack, robar, risa, hurt, death.  Uso: python3 herramientas/krampusillo.py"""
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
from geo import Anim, Creature, save_anims  # noqa: E402
import krampus as K  # noqa: E402

A = K.A
S, C = K.S, K.C
P = K.P
P.update(fur=(88, 46, 34), fur_d=(54, 26, 20), fur_l=(132, 76, 52), gris=(70, 52, 46), piel=(44, 26, 24), piel_d=(26, 14, 14),
         ojo=(255, 222, 40), ojo2=(255, 250, 190))
pelo, piel, solido, cuerno, pezuna, garra = K.pelo, K.piel, K.solido, K.cuerno, K.pezuna, K.garra


def cara(face, x, y, w, hh):
    """Cara de cuero con una sonrisa enorme de dientes chuecos."""
    if face == 'front':
        if y == hh - 2 and 1 <= x <= w - 2:
            return P['diente'] if x % 2 == 1 else P['boca']        # dientes chuecos
        if y == hh - 3 and x in (1, w - 2):
            return P['boca']                                       # las comisuras suben
        if y == 1 and 1 <= x <= w - 2:
            return P['piel_d']                                     # ceño
        return P['piel']
    if face == 'top':
        return P['fur_d']
    return pelo(0)(face, x, y, w, hh) if y < 3 else P['piel']


def ojos(face, x, y, w, hh):
    if x in (0, 1) or x in (w - 2, w - 1):
        return (P['ojo2'] if x in (1, w - 2) else P['ojo']) + (255,)
    return None


def oreja(face, x, y, w, hh):
    if face == 'front' and 1 <= x <= w - 2:
        return K.mix(P['piel'], (150, 60, 60), 0.5)
    return P['piel']


c = Creature('geometry.navidad.krampusillo', 64, 64, seed=55)
c.bone('root', [0, 0, 0])
c.bone('body', [0, 7, 0], 'root', rotation=[12, 0, 0])
c.part('body', [-3, 6, -2], [6, 6, 4], pelo(2, 0.2))
c.bone('head', [0, 12, -0.5], 'body', rotation=[-12, 0, 0])
c.part('head', [-3.5, 12, -4], [7, 6, 6], cara)
c.part('head', [-1.5, 12.5, -5], [3, 2, 1], solido('piel_d'))                # hocico chato
c.part('head', [-3.5, 17, -3.5], [7, 2, 5], pelo(1))                         # greña
c.bone('ojos', [0, 15, -4.05], 'head')
c.plane('ojos', [-3, 15, -4.08], 6, 1, ojos)
for lado, s in (('r', -1), ('l', 1)):
    c.bone('oreja_' + lado, [3.5 * s, 15.5, -1.5], 'head', rotation=[0, 0, -40 * s])
    c.part('oreja_' + lado, [3.5 * s - (0 if s > 0 else 4), 15, -2], [4, 1, 1], oreja)
    c.bone('cuerno_' + lado, [1.8 * s, 18, -2], 'head', rotation=[-20, 0, 20 * s])
    c.part('cuerno_' + lado, [1.8 * s - 0.5, 18, -2.5], [1, 3, 1], cuerno(0, 0.7))
    c.bone('cuerno_' + lado + '_2', [1.8 * s, 21, -2], 'cuerno_' + lado, rotation=[-30, 0, -10 * s])
    c.part('cuerno_' + lado + '_2', [1.8 * s - 0.5, 21, -2.5], [1, 2, 1], cuerno(0.7, 1))
    x = 3.6 * s
    c.bone('arm_' + lado, [x, 11, 0], 'body', rotation=[-14, 0, -8 * s])
    c.part('arm_' + lado, [x - 1, 5, -1], [2, 6, 2], pelo(1))
    c.part('arm_' + lado, [x - 1, 3, -1.5], [1, 2, 1], garra)
    c.part('arm_' + lado, [x, 3, -0.5], [1, 2, 1], garra)
    x = 1.6 * s
    leg = 'leg_' + lado
    c.bone(leg, [x, 7, 0], 'body', rotation=[-24, 0, 0])
    c.part(leg, [x - 1.5, 3, -1.5], [3, 4, 3], pelo(1))
    c.bone(leg + '_low', [x, 3.5, 0], leg, rotation=[40, 0, 0])
    c.part(leg + '_low', [x - 1, 1, 0], [2, 3, 2], pelo(0))
    c.part(leg + '_low', [x - 1, 0, -1], [2, 1, 2], pezuna)
c.bone('right_item', [-3.6, 4, -1], 'arm_r')
c.bone('tail', [0, 8, 2], 'body', rotation=[-50, 0, 0])
c.part('tail', [-0.5, 8, 2], [1, 7, 1], solido('piel'))
c.bone('tail_tip', [0, 15, 2.5], 'tail', rotation=[30, 0, 0])
c.part('tail_tip', [-1, 15, 1.5], [2, 2, 1], solido('lengua_d'))
c.glow('ojos')
c.build(os.path.join(A, 'geo', 'krampusillo.geo.json'), os.path.join(A, 'textures', 'entity', 'krampusillo.png'))

an = []
L = 2.0
a = Anim('idle', L, True)
a.rot('body', K.cyc([[0, 0, 0], [4, 0, 0]], L)).rot('head', K.cyc([[0, 0, 0], [-6, 14, 6], [0, 0, 0], [-6, -14, -6]], L))
a.rot('tail', K.cyc([[0, 0, 0], [10, 0, 20], [0, 0, 0], [10, 0, -20]], L)).rot('tail_tip', K.cyc([[0, 0, 0], [20, 0, 0]], L / 2))
a.rot('oreja_r', K.cyc([[0, 0, 0], [0, 0, 14]], L)).rot('oreja_l', K.cyc([[0, 0, 0], [0, 0, -14]], L, 0.4))
a.rot('arm_r', K.cyc([[0, 0, 0], [-10, 0, 6]], L)).rot('arm_l', K.cyc([[0, 0, 0], [-10, 0, -6]], L, 0.5))
an.append(a)
for nombre, L, amp in (('walk', 0.6, 1.0), ('run', 0.36, 1.5)):
    a = Anim(nombre, L, True)
    for lado, ph in (('r', 0.0), ('l', 0.5)):
        a.rot('leg_' + lado, K.cyc([[-30 * amp, 0, 0], [24 * amp, 0, 0]], L, ph))
        a.rot('leg_' + lado + '_low', K.cyc([[10, 0, 0], [36 * amp, 0, 0]], L, ph))
        a.rot('arm_' + lado, K.cyc([[34 * amp, 0, 0], [-34 * amp, 0, 0]], L, ph))
    a.pos('body', K.cyc([[0, 0, 0], [0, 1 * amp, 0]], L / 2)).rot('body', K.cyc([[4 * amp, 0, 0], [10 * amp, 0, 0]], L / 2))
    a.rot('tail', K.cyc([[0, 0, 20], [0, 0, -20]], L)).rot('head', K.cyc([[0, 8, 0], [0, -8, 0]], L))
    an.append(a)
a = Anim('attack', 0.6, False)
a.rot('arm_r', {0: [0, 0, 0], 0.2: ([-150, 0, 20], 'easeOutQuad'), 0.32: ([-40, 0, -10], 'easeInQuart'), 0.6: [0, 0, 0]})
a.rot('arm_l', {0: [0, 0, 0], 0.28: ([-150, 0, -20], 'easeOutQuad'), 0.4: ([-40, 0, 10], 'easeInQuart'), 0.6: [0, 0, 0]})
a.rot('body', {0: [0, 0, 0], 0.2: ([-10, 0, 0], S), 0.36: ([20, 0, 0], S), 0.6: [0, 0, 0]})
an.append(a)
a = Anim('robar', 0.9, False)
a.rot('body', {0: [0, 0, 0], 0.3: ([40, 0, 0], S), 0.6: ([-10, 0, 0], S), 0.9: [0, 0, 0]})
a.rot('arm_r', {0: [0, 0, 0], 0.3: ([-60, 0, 0], S), 0.6: ([-90, 0, 0], S), 0.9: [0, 0, 0]})
a.rot('arm_l', {0: [0, 0, 0], 0.3: ([-60, 0, 0], S), 0.6: ([-90, 0, 0], S), 0.9: [0, 0, 0]})
a.pos('root', {0: [0, 0, 0], 0.6: ([0, 3, 0], 'easeOutQuad'), 0.9: [0, 0, 0]})
an.append(a)
a = Anim('risa', 1.2, False)
a.pos('root', {0: [0, 0, 0], 0.15: ([0, 2, 0], S), 0.3: ([0, 0, 0], S), 0.45: ([0, 2, 0], S), 0.6: ([0, 0, 0], S), 0.75: ([0, 2, 0], S),
               0.9: [0, 0, 0]})
a.rot('head', {0: [0, 0, 0], 0.15: ([-20, 0, 10], S), 0.45: ([-20, 0, -10], S), 0.75: ([-20, 0, 10], S), 1.2: [0, 0, 0]})
a.rot('arm_r', {0: [0, 0, 0], 0.15: ([0, 0, -60], S), 0.9: ([0, 0, -60], S), 1.2: [0, 0, 0]})
a.rot('arm_l', {0: [0, 0, 0], 0.15: ([0, 0, 60], S), 0.9: ([0, 0, 60], S), 1.2: [0, 0, 0]})
a.sound(0.05, 'navidad:krampusillo_risa')
an.append(a)
a = Anim('hurt', 0.35, False)
a.rot('body', {0: [0, 0, 0], 0.1: ([-16, 0, 6], 'easeOutQuad'), 0.35: [0, 0, 0]})
an.append(a)
a = Anim('death', 1.0, 'hold')
a.rot('root', {0: [0, 0, 0], 0.5: ([0, 0, 90], 'easeInQuad'), 1.0: ([0, 0, 90], S)})
a.pos('root', {0: [0, 0, 0], 0.5: ([0, 1, 0], S), 1.0: ([0, 1, 0], S)})
an.append(a)
save_anims('krampusillo', an, os.path.join(A, 'animations', 'krampusillo.animation.json'))
print('krampusillo listo')
