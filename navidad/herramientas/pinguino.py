#!/usr/bin/env python3
"""El pingüino de Navidad de Axelis (modelo, textura y animaciones), la mascota del mod.

Chiquito y redondo (cabezón, cuerpo de pera y panza abultada): espalda negra con brillo azul, panza blanca, carita blanca en forma de\ncorazón con ojos brillantes y cachetes rosas, pico y patas naranjas. Todo pintado directo en los cubos, con medidas enteras.
Cuando es tuyo se le ven una bufanda roja de rayas y un gorrito de Santa (el código esconde esos huesos si no tiene dueño).
Animaciones: idle, walk, run, slide (de panza), swim, sit, sleep, happy, flap, hurt y death.
Uso: python3 herramientas/pinguino.py"""
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
import criaturas as CC  # noqa: E402
from geo import Anim, Creature, save_anims  # noqa: E402

A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')
S, mix, cyc = CC.S, CC.mix, CC.cyc

NEGRO, NEGRO2 = (26, 28, 38), (46, 50, 66)
BLANCO, CREMA = (244, 245, 248), (252, 232, 186)
NARANJA, NARANJA2 = (246, 150, 36), (214, 110, 24)
ROSA, OJO = (248, 160, 172), (16, 16, 22)
ROJO, ROJO2 = (200, 30, 44), (150, 18, 30)


def pinguino():
    c = Creature('geometry.navidad.pinguino', 128, 64, seed=33)
    AZUL = (40, 46, 66)          # brillo azulado del plumaje

    def lomo(face, x, y, w, h):
        """Espalda, lados y coronilla: negro con vetas de brillo azulado (más claro arriba)."""
        if face == 'bottom':
            return NEGRO
        if face == 'top':
            return NEGRO2 if (x + y) % 4 else AZUL
        if (x * 2 + y) % 7 == 0:
            return AZUL
        return mix(NEGRO2, NEGRO, min(1, y / max(1, h - 1) * 1.4))

    # ------------------------------------------------------------ todo pintado directo en los cubos (sin placas encimadas) y con
    # medidas enteras: así cada pixel cae donde debe y no hay rayas ni colores corridos
    def cuerpo(face, x, y, w, h):
        """Cuerpo (9 de ancho): al frente la panza blanca con orilla negra y esquinas redondeadas abajo."""
        if face != 'front':
            return lomo(face, x, y, w, h)
        if x in (0, w - 1):
            return NEGRO
        if y == h - 1 and x in (1, w - 2):
            return NEGRO
        return CREMA if y == 0 else BLANCO

    def panza(face, x, y, w, h):
        """La panza abultada (7x6x1): blanca, con un toque crema arriba y las esquinas sombreadas para que se vea redonda."""
        GRIS = (226, 228, 236)
        if face == 'top':
            return CREMA
        if face == 'front':
            if y == 0:
                return mix(CREMA, BLANCO, 0.35) if 0 < x < w - 1 else GRIS
            if (x in (0, w - 1)) and (y == h - 1):
                return GRIS
            return BLANCO if (x * 3 + y * 5) % 11 else (238, 240, 246)
        return GRIS if face in ('right', 'left') else BLANCO

    CARA = ["BBBBBBBBBB",
            "BBWWBBWWBB",
            "BWWWWWWWWB",
            "BWEEWWEEWB",
            "BWEEWWEEWB",
            "WPWWWWWWPW",
            "WWWWWWWWWW",
            "WWWWWWWWWW"]

    def cabeza(face, x, y, w, h):
        """Cabeza (10x8x8): carita blanca en forma de corazón con ojos de 2x2, cachetes rosas; lo demás negro."""
        if face == 'front':
            ch = CARA[y][x]
            return {'B': NEGRO, 'W': BLANCO, 'E': OJO, 'G': (255, 255, 255), 'P': ROSA}[ch]
        if face == 'bottom':
            return BLANCO if y < 2 else NEGRO
        if face in ('right', 'left') and y >= h - 3:
            # la carita blanca da vuelta un poquito por los lados, abajo y al frente
            frente = x >= w - 2 if face == 'right' else x <= 1
            if frente:
                return BLANCO
        return lomo(face, x, y, w, h)

    def pico(face, x, y, w, h):
        if face == 'top' and x == 0:
            return (255, 190, 90)
        return NARANJA2 if face == 'bottom' else NARANJA

    def pico_bajo(face, x, y, w, h):
        return NARANJA2

    def pata(face, x, y, w, h):
        if face == 'top' and y == 0 and x % 2 == 0:
            return NARANJA2        # deditos
        return NARANJA2 if face == 'bottom' else NARANJA

    def aleta(face, x, y, w, h):
        if face in ('right', 'left'):
            if y >= h - 1:
                return NEGRO2
            return AZUL if (x + y) % 5 == 0 else NEGRO
        if face == 'front' and y >= h - 2:
            return (210, 212, 222)   # la orilla de abajo de la aleta es clarita
        return NEGRO

    def bufanda(face, x, y, w, h):
        if face in ('top', 'bottom'):
            return ROJO2
        return BLANCO if x % 3 == 2 else ROJO

    def punta(face, x, y, w, h):
        if y == h - 1:
            return BLANCO          # fleco
        return BLANCO if y % 2 == 1 else ROJO2 if face == 'bottom' else ROJO

    def gorro(face, x, y, w, h):
        return ROJO2 if face == 'bottom' else ROJO if (x + y) % 5 else mix(ROJO, (240, 80, 90), 0.5)

    def pelo(face, x, y, w, h):
        return (232, 232, 238) if (x + y) % 2 else BLANCO

    c.bone('root', [0, 0, 0])
    c.bone('body', [0, 1, 0], 'root')
    c.part('body', [-4.5, 1, -4], [9, 8, 8], cuerpo)                          # cuerpo
    c.part('body', [-3.5, 2, -5], [7, 6, 1], panza)                           # panza abultada
    c.part('body', [-1, 1.5, 3.5], [2, 1, 2], aleta, rotation=[-25, 0, 0], pivot=[0, 2, 4])     # colita
    c.bone('head', [0, 9, 0], 'body')
    c.part('head', [-5, 9, -4], [10, 8, 8], cabeza)
    c.bone('beak', [0, 11, -4], 'head')
    c.part('beak', [-1, 11, -6], [2, 1, 2], pico)
    c.part('beak', [-1, 10.3, -5], [2, 1, 1], pico_bajo)
    for lado, s in (('r', -1), ('l', 1)):
        b = 'flipper_' + lado
        c.bone(b, [4.6 * s, 8.5, 0], 'body')
        c.part(b, [4.6 if s > 0 else -5.6, 2.5, -1.5], [1, 6, 3], aleta, rotation=[0, 0, 14 * s], pivot=[4.6 * s, 8.5, 0])
        f = 'foot_' + lado
        c.bone(f, [2 * s, 1, 0], 'root')
        c.part(f, [2 * s - 1.5, 0, -5], [3, 1, 4], pata, rotation=[0, -12 * s, 0], pivot=[2 * s, 0, -1])
    # la bufanda (cuello y una punta que cuelga) y el gorrito de Santa: solo cuando tiene dueño
    c.bone('bufanda', [0, 9, 0], 'body')
    c.part('bufanda', [-5.5, 8, -5], [11, 2, 10], bufanda)
    c.part('bufanda', [1.5, 4.2, -5.6], [2, 4, 1], punta, rotation=[0, 0, -8], pivot=[2.5, 8, -5])
    c.bone('gorro', [0, 17, 0], 'head', [0, 0, -12])
    c.part('gorro', [-5.5, 16, -4.5], [11, 2, 9], pelo)
    c.part('gorro', [-4, 18, -3.5], [8, 2, 7], gorro)
    c.part('gorro', [-2, 20, -2.5], [5, 2, 4], gorro)
    c.part('gorro', [0, 22, -1.5], [2, 2, 2], pelo)
    c.build(os.path.join(A, 'geo', 'pinguino.geo.json'), os.path.join(A, 'textures', 'entity', 'pinguino.png'))

    an = []
    # quieto: se mece, voltea a ver y de vez en cuando aletea
    L = 4.0
    a = Anim('idle', L, True)
    a.rot('body', cyc([[0, 0, 2], [0, 0, -2]], L))
    a.rot('head', cyc([[0, 0, 0], [4, 18, 3], [0, 0, 0], [0, 0, 0], [4, -20, -3], [0, 0, 0]], L))
    for lado, s in (('r', -1), ('l', 1)):
        a.rot('flipper_' + lado, {0: [0, 0, 0], 2.4: [0, 0, 0], 2.55: ([0, 0, 35 * s], S), 2.7: ([0, 0, 5 * s], S), 2.85: ([0, 0, 35 * s], S),
                                  3.05: ([0, 0, 0], S), L: [0, 0, 0]})
    an.append(a)
    # caminar: el bamboleo de pingüino (de lado a lado, pasitos cortos, aletas abiertas)
    L = 0.5
    a = Anim('walk', L, True)
    a.rot('root', cyc([[0, 0, 9], [0, 0, 0], [0, 0, -9], [0, 0, 0]], L))
    a.pos('body', cyc([[0, 0, 0], [0, 0.5, 0]], L / 2))
    a.pos('foot_r', cyc([[0, 0.8, -0.5], [0, 0, 0], [0, 0, 0.3], [0, 0, 0]], L))
    a.pos('foot_l', cyc([[0, 0, 0.3], [0, 0, 0], [0, 0.8, -0.5], [0, 0, 0]], L))
    a.rot('flipper_r', cyc([[0, 0, -18], [0, 0, -8]], L)).rot('flipper_l', cyc([[0, 0, 18], [0, 0, 8]], L, 0.5))
    a.rot('head', cyc([[0, 0, -4], [0, 0, 4]], L))
    an.append(a)
    # correr: igual pero más rápido, inclinado y aleteando
    L = 0.32
    a = Anim('run', L, True)
    a.rot('root', cyc([[0, 0, 12], [0, 0, 0], [0, 0, -12], [0, 0, 0]], L))
    a.rot('body', [12, 0, 0])
    a.pos('body', cyc([[0, 0, 0], [0, 0.9, 0]], L / 2))
    a.pos('foot_r', cyc([[0, 1.0, -0.8], [0, 0, 0], [0, 0, 0.5], [0, 0, 0]], L))
    a.pos('foot_l', cyc([[0, 0, 0.5], [0, 0, 0], [0, 1.0, -0.8], [0, 0, 0]], L))
    a.rot('flipper_r', cyc([[0, 0, -55], [0, 0, -10]], L / 2)).rot('flipper_l', cyc([[0, 0, 55], [0, 0, 10]], L / 2))
    a.rot('head', [-10, 0, 0])
    an.append(a)
    # de panza en el hielo: acostado, cabeza arriba, aletas pegadas y patas atrás, con un temblorcito
    L = 0.8
    a = Anim('slide', L, True)
    a.rot('body', cyc([[84, 0, -3], [84, 0, 3]], L))
    a.pos('body', [0, 2.6, 0])
    a.rot('head', [-62, 0, 0])
    a.rot('flipper_r', cyc([[-8, 0, -14], [-8, 0, -6]], L)).rot('flipper_l', cyc([[-8, 0, 14], [-8, 0, 6]], L))
    a.rot('foot_r', [85, 0, 0]).rot('foot_l', [85, 0, 0])
    a.pos('foot_r', [0, 2.4, 2.2]).pos('foot_l', [0, 2.4, 2.2])
    an.append(a)
    # nadar: horizontal, aletas como remos y patas pataleando
    L = 0.6
    a = Anim('swim', L, True)
    a.rot('body', cyc([[80, 0, -4], [80, 0, 4]], L))
    a.pos('body', [0, 2.4, 0])
    a.rot('head', [-55, 0, 0])
    a.rot('flipper_r', cyc([[-30, 0, -70], [30, 0, -20]], L)).rot('flipper_l', cyc([[-30, 0, 70], [30, 0, 20]], L))
    a.rot('foot_r', cyc([[70, 0, 0], [100, 0, 0]], L)).rot('foot_l', cyc([[100, 0, 0], [70, 0, 0]], L))
    a.pos('foot_r', [0, 2.4, 2.2]).pos('foot_l', [0, 2.4, 2.2])
    an.append(a)
    # sentado (cuando se lo pides): echado para atrás con las patitas al frente
    a = Anim('sit', 0.4, 'hold')
    a.rot('body', {0: [0, 0, 0], 0.4: ([-18, 0, 0], S)})
    a.pos('body', {0: [0, 0, 0], 0.4: ([0, -0.8, 0.6], S)})
    a.rot('head', {0: [0, 0, 0], 0.4: ([14, 0, 0], S)})
    a.rot('foot_r', {0: [0, 0, 0], 0.4: ([-40, -12, 0], S)}).rot('foot_l', {0: [0, 0, 0], 0.4: ([-40, 12, 0], S)})
    a.pos('foot_r', {0: [0, 0, 0], 0.4: ([0, 0.4, -0.8], S)}).pos('foot_l', {0: [0, 0, 0], 0.4: ([0, 0.4, -0.8], S)})
    a.rot('flipper_r', {0: [0, 0, 0], 0.4: ([0, 0, -12], S)}).rot('flipper_l', {0: [0, 0, 0], 0.4: ([0, 0, 12], S)})
    an.append(a)
    # dormido (de noche, sentado): cabeza abajo
    a = Anim('sleep', 1.2, 'hold')
    a.rot('body', {0: [0, 0, 0], 1.2: ([-10, 0, 0], S)})
    a.pos('body', {0: [0, 0, 0], 1.2: ([0, -0.8, 0.4], S)})
    a.rot('head', {0: [0, 0, 0], 1.2: ([38, 0, 8], S)})
    a.rot('flipper_r', {0: [0, 0, 0], 1.2: ([0, 0, -4], S)}).rot('flipper_l', {0: [0, 0, 0], 1.2: ([0, 0, 4], S)})
    an.append(a)
    # feliz (al darle pescado o al hacerse tuyo): brinquito con aleteo y vuelta
    a = Anim('happy', 1.1, False)
    a.pos('root', {0: [0, 0, 0], 0.12: ([0, -0.6, 0], S), 0.35: ([0, 3.2, 0], 'easeOutQuad'), 0.6: ([0, 0, 0], 'easeInQuad'),
                   0.72: ([0, 1.4, 0], 'easeOutQuad'), 0.85: ([0, 0, 0], 'easeInQuad'), 1.1: [0, 0, 0]})
    a.rot('root', {0: [0, 0, 0], 0.12: [0, 0, 0], 0.6: ([0, 360, 0], S), 1.1: ([0, 360, 0], S)})
    for lado, s in (('r', -1), ('l', 1)):
        a.rot('flipper_' + lado, {0: [0, 0, 0], 0.15: ([0, 0, 70 * s], S), 0.25: ([0, 0, 20 * s], S), 0.35: ([0, 0, 70 * s], S),
                                  0.45: ([0, 0, 20 * s], S), 0.55: ([0, 0, 70 * s], S), 0.8: ([0, 0, 10 * s], S), 1.1: [0, 0, 0]})
    a.rot('head', {0: [0, 0, 0], 0.35: ([-20, 0, 0], S), 0.8: ([0, 0, 12], S), 1.1: [0, 0, 0]})
    an.append(a)
    # aleteo (un gesto suelto)
    a = Anim('flap', 0.8, False)
    for lado, s in (('r', -1), ('l', 1)):
        a.rot('flipper_' + lado, {0: [0, 0, 0], 0.1: ([0, 0, 60 * s], S), 0.2: ([0, 0, 15 * s], S), 0.3: ([0, 0, 60 * s], S),
                                  0.4: ([0, 0, 15 * s], S), 0.5: ([0, 0, 60 * s], S), 0.8: [0, 0, 0]})
    a.rot('head', {0: [0, 0, 0], 0.2: ([-25, 0, 0], S), 0.6: ([-25, 0, 0], S), 0.8: [0, 0, 0]})
    a.rot('beak', {0: [0, 0, 0], 0.2: ([-20, 0, 0], S), 0.3: [0, 0, 0], 0.4: ([-20, 0, 0], S), 0.5: [0, 0, 0]})
    an.append(a)
    a = Anim('hurt', 0.35, False)
    a.rot('body', {0: [0, 0, 0], 0.08: ([-10, 0, 10], 'easeOutQuad'), 0.35: [0, 0, 0]})
    a.rot('flipper_r', {0: [0, 0, 0], 0.08: ([0, 0, -45], S), 0.35: [0, 0, 0]}).rot('flipper_l', {0: [0, 0, 0], 0.08: ([0, 0, 45], S), 0.35: [0, 0, 0]})
    an.append(a)
    a = Anim('death', 0.8, 'hold')
    a.rot('root', {0: [0, 0, 0], 0.6: ([0, 0, 90], 'easeInQuad')})
    a.pos('root', {0: [0, 0, 0], 0.6: ([1.5, 1.5, 0], S)})
    an.append(a)
    save_anims('pinguino', an, os.path.join(A, 'animations', 'pinguino.animation.json'))


if __name__ == '__main__':
    pinguino()
    print('ok')
