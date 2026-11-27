#!/usr/bin/env python3
"""El lanzanieves y su tanque (modelos hechos a mano y sus texturas).

Lanzanieves: un juguete navideño de verdad: cuerpo rojo con rayas blancas de bastón de caramelo y detalles dorados, culata, mango y
agarradera negros, cuatro cañones de hielo que giran alrededor de un eje (hueso "canones"), una ventanita de vidrio arriba con nieve
adentro, un manómetro al lado con su aguja (hueso "aguja": la mueve el juego según cuánta nieve queda) y la conexión de la manguera.
Tanque: cilindro rojo con anillos dorados y rayas blancas que va en la espalda (sobre la plantilla de jugador de Blockbench, hueso
"cuerpo"), con correas y hebilla al frente, ventana de vidrio atrás con la nieve que tiene (hueso "nieve", se encoge al gastarla),
válvula de rueda arriba y la boquilla de la manguera abajo.
Uso: python3 herramientas/lanzanieves.py"""
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
from geo import Creature  # noqa: E402

A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


ROJO, ROJO_OSC, ROJO_CLARO = (204, 30, 40), (140, 14, 24), (236, 76, 76)
BLANCO, BLANCO_OSC = (246, 246, 250), (210, 216, 226)
ORO, ORO_OSC, ORO_CLARO = (226, 176, 56), (158, 108, 24), (255, 230, 136)
NEGRO, NEGRO2 = (30, 28, 34), (56, 52, 62)
HIELO, HIELO_OSC, HIELO_CLARO = (150, 206, 240), (96, 156, 206), (226, 246, 255)
PLATA, PLATA_OSC = (196, 202, 212), (130, 138, 150)
VIDRIO = (190, 226, 246)
CUERO, CUERO_OSC = (110, 66, 36), (78, 44, 22)


def rayas(face, x, y, w, h):
    """Rojo con rayas blancas en diagonal (bastón de caramelo)."""
    if face == 'bottom':
        return ROJO_OSC
    if (x + y) % 5 in (0, 1):
        return BLANCO if face != 'top' else BLANCO_OSC
    return ROJO if (x * 3 + y) % 7 else ROJO_CLARO


def rojo(face, x, y, w, h):
    if face == 'bottom':
        return ROJO_OSC
    if face == 'top':
        return ROJO_CLARO
    return ROJO if (x + y) % 6 else ROJO_CLARO


def oro(face, x, y, w, h):
    if face == 'top':
        return ORO_CLARO
    if face == 'bottom':
        return ORO_OSC
    return ORO if (x + y) % 4 else ORO_CLARO


def negro(face, x, y, w, h):
    return NEGRO2 if (x * 2 + y) % 5 == 0 else NEGRO


def mango(face, x, y, w, h):
    # rayitas para agarrar bien
    if face in ('front', 'back', 'right', 'left') and y % 2 == 0:
        return NEGRO2
    return NEGRO


def hielo(face, x, y, w, h):
    if face in ('front', 'back') and (x + y) % 3 == 0:
        return HIELO_CLARO
    if face == 'top':
        return HIELO_CLARO
    return HIELO if (x + y) % 4 else HIELO_OSC


def anillo_escarcha(face, x, y, w, h):
    return HIELO_CLARO if (x + y) % 2 else BLANCO


def plata(face, x, y, w, h):
    return PLATA if (x + y) % 3 else PLATA_OSC


def vidrio(face, x, y, w, h):
    if (x == 0 and y == 0) or (x == 1 and y == 0):
        return (255, 255, 255)
    return VIDRIO


def nieve(face, x, y, w, h):
    return BLANCO if (x + y * 2) % 5 else (226, 238, 250)


def cara_manometro(face, x, y, w, h):
    if face in ('right', 'left'):
        if (x - (w - 1) / 2) ** 2 + (y - (h - 1) / 2) ** 2 > (w / 2) ** 2:
            return ORO
        if y == 0 or y == h - 1:
            return ORO
        return BLANCO if (x + y) % 5 else (230, 230, 236)
    return ORO


def correa(face, x, y, w, h):
    if y == 0 or y == h - 1:
        return CUERO_OSC
    return CUERO if (x + y) % 5 else CUERO_OSC


def copito(face, x, y, w, h):
    # el tanque: rojo con un copo blanco atrás (cara 'back' en Blockbench = la que mira hacia atrás)
    if face == 'back':
        cx, cy = (w - 1) / 2, (h - 1) / 2
        dx, dy = abs(x - cx), abs(y - cy)
        if (dx < 0.6 and dy < h * 0.32) or (dy < 0.6 and dx < w * 0.32) or (abs(dx - dy) < 0.6 and dx < w * 0.25):
            return BLANCO
    return rayas(face, x, y, w, h)


# ================================================================ el lanzanieves (de frente a -z, el mango en y = 0)
g = Creature('geometry.navidad.lanzanieves', 128, 64, seed=7)
g.bone('arma', [0, 0, 0])
g.bone('cuerpo', [0, 6, 0], 'arma')
g.part('cuerpo', [-2, 4, -4], [4, 4.5, 10], rayas)                           # cuerpo
g.part('cuerpo', [-2.2, 3.8, -4.4], [4.4, 0.6, 10.8], oro)                   # orilla dorada de abajo
g.part('cuerpo', [-2.2, 8.4, -4.4], [4.4, 0.6, 10.8], oro)                   # y de arriba
g.part('cuerpo', [-1.5, 4.5, 6], [3, 3.5, 4.2], rojo)                        # culata
g.part('cuerpo', [-1.7, 4.2, 10.2], [3.4, 4.1, 1], lambda f, x, y, w, h: BLANCO if f != 'bottom' else BLANCO_OSC)   # tope
g.part('cuerpo', [-1.25, 0, 2], [2.5, 4.2, 2.4], mango, rotation=[-18, 0, 0], pivot=[0, 4, 3.2])                     # mango
g.part('cuerpo', [-0.4, 2.3, 0.2], [0.8, 0.5, 2.2], oro)                      # guardamonte
g.part('cuerpo', [-0.3, 2.8, 0.7], [0.6, 1.4, 0.6], oro)                      # gatillo
g.part('cuerpo', [-1, 1.4, -3], [2, 2.8, 1.8], mango)                        # agarradera de adelante
g.part('cuerpo', [-1.6, 8.9, -2.2], [3.2, 0.4, 5.8], plata)                  # marco de la ventanita
g.part('cuerpo', [-0.8, 3.0, 7.2], [1.6, 1.2, 1.6], oro)                     # conexión de la manguera
g.part('cuerpo', [-0.6, 1.6, 7.4], [1.2, 1.4, 1.2], lambda f, x, y, w, h: (220, 30, 40) if y % 2 else BLANCO)  # tramo de manguera
g.part('cuerpo', [2, 5.4, 0.2], [0.6, 2.6, 2.6], cara_manometro)             # manómetro (lado izquierdo del arma)
g.bone('aguja', [2.75, 6.7, 1.5], 'cuerpo')
g.part('aguja', [2.6, 6.6, 0.6], [0.2, 0.25, 1.0], lambda f, x, y, w, h: (210, 20, 30))
# la ventanita de arriba con nieve adentro (brilla un poco)
g.bone('ventana', [0, 9.3, 0.7], 'cuerpo')
g.part('ventana', [-1.3, 9.3, -1.9], [2.6, 1.5, 5.2], vidrio)
g.bone('nieve_arma', [0, 9.3, 0.7], 'ventana')
g.part('nieve_arma', [-1.1, 9.3, -1.6], [2.2, 0.9, 4.6], nieve)
# los cañones (giran en el eje z) y el plato de enfrente
g.bone('canones', [0, 6.25, -4], 'cuerpo')
for i, (cx, cy) in enumerate(((-1.15, 1.15), (1.15, 1.15), (1.15, -1.15), (-1.15, -1.15))):
    g.part('canones', [cx - 0.7, 6.25 + cy - 0.7, -11], [1.4, 1.4, 7], hielo)
    g.part('canones', [cx - 0.8, 6.25 + cy - 0.8, -8.4], [1.6, 1.6, 0.6], anillo_escarcha)
g.part('canones', [-0.5, 5.75, -10.6], [1, 1, 6.6], plata)                   # eje
g.part('canones', [-2.1, 4.15, -10.2], [4.2, 4.2, 0.6], plata)               # plato que sostiene los cañones
g.part('canones', [-2.1, 4.15, -5], [4.2, 4.2, 0.8], oro)
g.bone('boca', [0, 6.25, -11], 'canones')
for i, (cx, cy) in enumerate(((-1.15, 1.15), (1.15, 1.15), (1.15, -1.15), (-1.15, -1.15))):
    g.part('boca', [cx - 0.55, 6.25 + cy - 0.55, -11.15], [1.1, 1.1, 0.3], lambda f, x, y, w, h: HIELO_CLARO)
g.glow('boca', 'nieve_arma')
g.build(os.path.join(A, 'geo', 'lanzanieves.geo.json'), os.path.join(A, 'textures', 'item', 'lanzanieves_3d.png'))

# ================================================================ el tanque (plantilla de jugador: el cuerpo va de y = 12 a 24, la espalda en z = 2)
t = Creature('geometry.navidad.tanque_nieve', 128, 64, seed=8)
t.bone('cuerpo', [0, 24, 0])
# correas: por los hombros y el cinturón del pecho
for s in (-1, 1):
    x0 = 1.6 if s > 0 else -3.0
    t.part('cuerpo', [x0, 23.8, -2.3], [1.4, 0.5, 4.6], correa)               # sobre el hombro
    t.part('cuerpo', [x0, 15.5, -2.3], [1.4, 8.5, 0.4], correa)               # por el pecho
t.part('cuerpo', [-4.1, 15.0, -2.4], [8.2, 1.0, 0.5], correa)                # cinturón
t.part('cuerpo', [-0.9, 14.8, -2.6], [1.8, 1.4, 0.4], oro)                   # hebilla
# el cilindro (redondeado con dos cajas) y sus anillos
t.part('cuerpo', [-2.6, 11.5, 2.2], [5.2, 10.5, 5.0], copito)
t.part('cuerpo', [-3.1, 11.5, 2.7], [6.2, 10.5, 4.0], rayas)
t.part('cuerpo', [-3.3, 11.2, 2.5], [6.6, 1.0, 4.4], oro)
t.part('cuerpo', [-2.8, 11.2, 2.0], [5.6, 1.0, 5.4], oro)
t.part('cuerpo', [-3.3, 21.4, 2.5], [6.6, 1.0, 4.4], oro)
t.part('cuerpo', [-2.8, 21.4, 2.0], [5.6, 1.0, 5.4], oro)
t.part('cuerpo', [-2.2, 22.4, 2.8], [4.4, 1.2, 3.8], rojo)                   # tapa redonda de arriba
# la válvula de rueda
t.part('cuerpo', [-0.4, 23.6, 4.3], [0.8, 0.8, 0.8], oro)
t.part('cuerpo', [-1.4, 24.4, 4.3], [2.8, 0.4, 0.8], oro)
t.part('cuerpo', [-0.4, 24.4, 3.3], [0.8, 0.4, 2.8], oro)
# ventana de vidrio atrás con la nieve (que se encoge cuando se gasta)
t.part('cuerpo', [-1.7, 12.8, 7.15], [3.4, 7.6, 0.3], plata)
t.bone('nieve', [0, 13.2, 7.3], 'cuerpo')
t.part('nieve', [-1.2, 13.2, 7.35], [2.4, 6.8, 0.25], nieve)
t.glow('nieve')
t.part('cuerpo', [-1.3, 13.1, 7.55], [2.6, 7.0, 0.05], lambda f, x, y, w, h: (220, 240, 255) if (x + y) % 6 else (255, 255, 255))
# la boquilla de la manguera (abajo, del lado derecho)
t.part('cuerpo', [-3.4, 10.4, 4.0], [1.4, 1.4, 1.4], oro)
t.part('cuerpo', [-3.2, 9.6, 4.2], [1.0, 0.8, 1.0], plata)
t.build(os.path.join(A, 'geo', 'tanque_nieve.geo.json'), os.path.join(A, 'textures', 'entity', 'tanque_nieve.png'))
print('lanzanieves y tanque listos')
