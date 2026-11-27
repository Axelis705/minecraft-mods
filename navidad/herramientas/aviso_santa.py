#!/usr/bin/env python3
"""La cinta del aviso "¡Santa Claus se acerca!" (roja con orilla dorada y puntas dobladas) y la flecha que señala de dónde viene.
A doble resolución. Escribe textures/gui/aviso_santa.png (512x128): la cinta en 0,0 (440x64) y la flecha en 448,0 (32x32).
Uso: python3 herramientas/aviso_santa.py"""
import math
import os
import random

from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad', 'textures', 'gui', 'aviso_santa.png')
R = random.Random(24)
img = Image.new('RGBA', (512, 128), (0, 0, 0, 0))
px = img.load()

ROJO, ROJO_OSC, ROJO_CLARO = (200, 30, 40), (130, 12, 22), (232, 70, 70)
ORO, ORO_OSC, ORO_CLARO = (226, 176, 56), (160, 110, 28), (255, 232, 140)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def put(x, y, c, a=255):
    if 0 <= x < 512 and 0 <= y < 128:
        px[x, y] = c + (a,)


# las puntas de atrás (más oscuras, con la muesca en V), una a cada lado
for lado in (0, 1):
    for y in range(10, 58):
        for k in range(44):
            x = k if lado == 0 else 439 - k
            muesca = abs(y - 34) * 0.9
            if k < muesca * 0.9:
                continue
            borde = y < 13 or y > 54
            put(x, y, ORO_OSC if borde else mix(ROJO_OSC, ROJO, 0.3))
# la cinta del frente
for y in range(2, 62):
    for x in range(30, 410):
        t = (y - 2) / 59
        c = mix(ROJO_CLARO, ROJO_OSC, t)
        if (x + y * 2) % 13 == 0:
            c = mix(c, ROJO_OSC, 0.25)
        if y < 6 or y > 57:
            c = ORO_CLARO if (x // 3) % 3 == 0 else ORO
        elif y < 8 or y > 55:
            c = ORO_OSC
        put(x, y, c)
    # el doblez de la cinta en cada orilla
    for x in range(30, 40):
        put(x, y, mix(ROJO_OSC, (60, 6, 12), (40 - x) / 10))
    for x in range(400, 410):
        put(x, y, mix(ROJO_OSC, (60, 6, 12), (x - 400) / 10))
# copitos y estrellitas en la cinta
for _ in range(26):
    x, y = R.randrange(46, 394), R.randrange(12, 52)
    for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
        put(x + dx, y + dy, (255, 236, 236), 120)

# la flecha (apunta hacia arriba): dorada con orilla oscura
for y in range(32):
    for x in range(32):
        cx = x - 15.5
        dentro = False
        if 2 <= y <= 18 and abs(cx) <= (y - 2) * 0.85:
            dentro = True
        if 17 <= y <= 30 and abs(cx) <= 5:
            dentro = True
        if not dentro:
            continue
        orilla = (2 <= y <= 18 and abs(cx) > (y - 2) * 0.85 - 2) or (y >= 17 and abs(cx) > 3.2) or y >= 29
        put(x + 448, y, ORO_OSC if orilla else ORO_CLARO if cx < 0 else ORO)
img.save(OUT)
print('aviso_santa.png listo')
