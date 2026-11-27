#!/usr/bin/env python3
"""La ventana del tanque de nieve (estilo de los cofres de Minecraft): a la izquierda el tanque dibujado con su ventanita (la nieve la
dibuja el juego según lo que tiene), a la derecha sus 6 ranuras y abajo tu inventario. Escribe textures/gui/tanque.png (256x256).
Uso: python3 herramientas/tanque_gui.py"""
import os

from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad', 'textures', 'gui', 'tanque.png')
img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
px = img.load()
GRIS, CLARO, OSCURO, NEGRO = (198, 198, 198), (255, 255, 255), (85, 85, 85), (0, 0, 0)
RANURA, R_OSC = (139, 139, 139), (55, 55, 55)


def rect(x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = c + (255,) if len(c) == 3 else c


W, H = 176, 166
rect(0, 0, W, H, GRIS)
# orillas: negra afuera, blanca arriba-izquierda, gris oscuro abajo-derecha (con esquinas redondas)
for x in range(W):
    for y in range(H):
        if x == 0 or y == 0 or x == W - 1 or y == H - 1:
            px[x, y] = NEGRO + (255,)
        elif x in (1, 2) or y in (1, 2):
            px[x, y] = CLARO + (255,)
        elif x in (W - 2, W - 3) or y in (H - 2, H - 3):
            px[x, y] = OSCURO + (255,)
for (x, y) in ((0, 0), (1, 0), (0, 1), (W - 1, 0), (W - 2, 0), (W - 1, 1), (0, H - 1), (1, H - 1), (0, H - 2), (W - 1, H - 1),
               (W - 2, H - 1), (W - 1, H - 2)):
    px[x, y] = (0, 0, 0, 0)


def ranura(x, y):
    for yy in range(y, y + 18):
        for xx in range(x, x + 18):
            if xx == x or yy == y:
                px[xx, yy] = R_OSC + (255,)
            elif xx == x + 17 or yy == y + 17:
                px[xx, yy] = CLARO + (255,)
            else:
                px[xx, yy] = RANURA + (255,)


for i in range(6):
    ranura(97 + (i % 3) * 18, 23 + (i // 3) * 18)
for f in range(3):
    for c in range(9):
        ranura(7 + c * 18, 83 + f * 18)
for c in range(9):
    ranura(7 + c * 18, 141)

# el tanque dibujado (cilindro rojo con rayas blancas, anillos dorados y la ventanita)
ROJO, ROJO_OSC, BLANCO, ORO, ORO_OSC = (204, 30, 40), (140, 14, 24), (246, 246, 250), (226, 176, 56), (158, 108, 24)
x0, x1, y0, y1 = 22, 60, 18, 76
for y in range(y0, y1):
    for x in range(x0, x1):
        borde = x in (x0, x1 - 1)
        c = ROJO if (x + y) % 8 > 2 else BLANCO
        if x in (x0 + 1, x0 + 2):
            c = tuple(int(v * 1.15) if v < 220 else v for v in c)
        if x > x1 - 5:
            c = tuple(int(v * 0.75) for v in c)
        if borde:
            c = ROJO_OSC
        px[x, y] = c + (255,)
for y in (y0, y0 + 1, y0 + 2, y1 - 3, y1 - 2, y1 - 1):
    for x in range(x0 - 1, x1 + 1):
        px[x, y] = (ORO if y in (y0 + 1, y1 - 2) else ORO_OSC) + (255,)
# tapa y válvula
for y in range(y0 - 5, y0):
    for x in range(x0 + 6, x1 - 6):
        px[x, y] = (ROJO if y > y0 - 4 else ROJO_OSC) + (255,)
for x in range(x0 + 14, x1 - 14):
    px[x, y0 - 7] = ORO + (255,)
    px[x, y0 - 6] = ORO_OSC + (255,)
# la ventanita (marco plateado; adentro la nieve la dibuja el juego en 32,26 de 18x42)
for y in range(24, 70):
    for x in range(30, 52):
        if x in (30, 31, 50, 51) or y in (24, 25, 68, 69):
            px[x, y] = (150, 158, 170, 255) if (x + y) % 3 else (200, 206, 216, 255)
        else:
            px[x, y] = (36, 52, 72, 255)
# manómetro al lado
for y in range(30, 42):
    for x in range(62, 74):
        d = (x - 67.5) ** 2 + (y - 35.5) ** 2
        if d <= 30:
            px[x, y] = (ORO if d > 20 else BLANCO) + (255,)
os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print('tanque.png listo')
