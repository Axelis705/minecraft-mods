#!/usr/bin/env python3
"""La textura del Libro de Navidad (pantalla): pasta de cuero rojo con orilla dorada, dos páginas de papel con sombra en el lomo,
ramitas de acebo, un copo de agua en el fondo, las cintas de los capítulos, el sello de cera, las casillas, el botón de reclamar y
estrellitas. Todo a doble resolución (2 texeles por pixel de pantalla) para que se vea fino.

Escribe textures/gui/libro.png (1024x512). Las posiciones están en LibroPantalla.java.
Uso: python3 herramientas/libro.py"""
import math
import os
import random

from PIL import Image, ImageDraw

AQUI = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad', 'textures', 'gui', 'libro.png')
R = random.Random(1225)

W, H = 1024, 512
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
px = img.load()


def put(x, y, c):
    if 0 <= x < W and 0 <= y < H:
        px[x, y] = c if len(c) == 4 else c + (255,)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def rect(x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            put(x, y, c)


def redondo(x0, y0, x1, y1, r, fn):
    """Rectángulo con esquinas redondas; fn(x, y, orilla) da el color (orilla = distancia al borde)."""
    for y in range(y0, y1):
        for x in range(x0, x1):
            dx = max(x0 + r - x, 0, x - (x1 - 1 - r))
            dy = max(y0 + r - y, 0, y - (y1 - 1 - r))
            if dx * dx + dy * dy > r * r:
                continue
            borde = min(x - x0, x1 - 1 - x, y - y0, y1 - 1 - y)
            if dx and dy:
                borde = min(borde, int(r - math.hypot(dx, dy)))
            put(x, y, fn(x, y, borde))


# ================================================================ el libro abierto (600 x 380 en 0,0)
CUERO, CUERO_OSC, CUERO_CLARO = (122, 26, 32), (78, 14, 20), (150, 40, 44)
ORO, ORO_OSC, ORO_CLARO = (214, 170, 64), (150, 104, 30), (250, 224, 140)
PAPEL, PAPEL_OSC, PAPEL_LINEA = (246, 236, 210), (226, 210, 174), (214, 196, 156)


def cuero(x, y, b):
    n = R.random()
    if b < 2:
        return CUERO_OSC
    if b in (9, 10):
        return ORO_CLARO if (x + y) % 7 == 0 else ORO      # la orilla dorada
    if b == 11:
        return ORO_OSC
    c = mix(CUERO, CUERO_OSC, 0.25) if n < 0.3 else CUERO if n < 0.85 else CUERO_CLARO
    if b < 5:
        c = mix(c, CUERO_OSC, 0.4)
    return c


redondo(0, 0, 600, 380, 18, cuero)

# esquinas doradas
for cx, cy, sx, sy in ((14, 14, 1, 1), (585, 14, -1, 1), (14, 365, 1, -1), (585, 365, -1, -1)):
    for k in range(22):
        for j in range(22 - k):
            if k + j > 14 and (k + j) % 3:
                continue
            put(cx + sx * k, cy + sy * j, ORO if (k + j) % 4 else ORO_CLARO)


def pagina(x0, x1, izquierda):
    y0, y1 = 16, 364
    for y in range(y0, y1):
        for x in range(x0, x1):
            n = R.random()
            c = PAPEL if n < 0.9 else mix(PAPEL, PAPEL_OSC, 0.35)
            # sombra hacia el lomo
            d = (x1 - x) if izquierda else (x - x0)
            if d < 36:
                c = mix(c, (170, 140, 96), (1 - d / 36) ** 2 * 0.55)
            # orilla de afuera: hojas apiladas
            e = (x - x0) if izquierda else (x1 - 1 - x)
            if e < 6 and (e % 2 == 0):
                c = PAPEL_LINEA
            if y1 - 1 - y < 5 and (y1 - 1 - y) % 2 == 0:
                c = mix(c, PAPEL_LINEA, 0.8)
            if y - y0 < 3:
                c = mix(c, PAPEL_LINEA, 0.4)
            put(x, y, c)


pagina(18, 298, True)
pagina(302, 582, False)
# el lomo
for y in range(14, 366):
    for x in range(296, 304):
        put(x, y, mix((60, 24, 16), (36, 12, 8), abs(x - 299.5) / 4))


def copo(cx, cy, r, color, alfa):
    """Un copo de nieve grande y tenue (marca de agua)."""
    for k in range(6):
        a = k * math.pi / 3
        for t in range(int(r)):
            x, y = cx + math.cos(a) * t, cy + math.sin(a) * t
            for dx in (0, 1):
                for dy in (0, 1):
                    xx, yy = int(x + dx), int(y + dy)
                    if 0 <= xx < W and 0 <= yy < H:
                        base = px[xx, yy]
                        px[xx, yy] = mix(base[:3], color, alfa) + (255,)
            if t in (int(r * 0.45), int(r * 0.7)):
                for s in (-1, 1):
                    b = a + s * math.pi / 4
                    for u in range(int(r * 0.22)):
                        xx, yy = int(x + math.cos(b) * u), int(y + math.sin(b) * u)
                        if 0 <= xx < W and 0 <= yy < H:
                            base = px[xx, yy]
                            px[xx, yy] = mix(base[:3], color, alfa) + (255,)


copo(158, 200, 92, (190, 170, 130), 0.18)
copo(442, 200, 92, (190, 170, 130), 0.18)

# acebo en las esquinas de arriba de las páginas
HOJA, HOJA_OSC, BAYA = (46, 120, 56), (26, 80, 36), (210, 30, 40)


def hoja(cx, cy, ang, largo=16):
    for t in range(largo):
        ancho = int(5 * math.sin(math.pi * t / largo)) + 1
        for w in range(-ancho, ancho + 1):
            x = cx + math.cos(ang) * t - math.sin(ang) * w
            y = cy + math.sin(ang) * t + math.cos(ang) * w
            borde = abs(w) == ancho or (abs(w) == ancho - 1 and t % 4 == 0)
            put(int(x), int(y), HOJA_OSC if borde or w == 0 else HOJA)


def bayas(cx, cy):
    for bx, by in ((0, 0), (6, 2), (2, 6)):
        for y in range(-3, 4):
            for x in range(-3, 4):
                if x * x + y * y <= 9:
                    c = (255, 200, 200) if x == -1 and y == -1 else BAYA if x * x + y * y < 6 else (150, 16, 24)
                    put(cx + bx + x, cy + by + y, c)


# (solo abajo a la izquierda: arriba van los títulos y a la derecha las tareas)
for cx, cy, sx in ((34, 352, 1),):
    hoja(cx, cy, math.radians(-20), 22)
    hoja(cx, cy, math.radians(-75), 18)
    bayas(cx - 2, cy - 2)

# ================================================================ las cintas de los capítulos (40 x 56) en x=608 (normal) y x=652 (elegida)
COLORES = [(200, 30, 42), (58, 122, 232), (46, 138, 72), (184, 134, 11), (130, 70, 170)]
for i, c in enumerate(COLORES):
    for sel in (0, 1):
        x0, y0 = 608 + sel * 44, i * 60
        claro = mix(c, (255, 255, 255), 0.35)
        osc = mix(c, (0, 0, 0), 0.4)

        def cinta(x, y, b, c=c, claro=claro, osc=osc, sel=sel):
            if b < 2:
                return ORO_OSC if sel else osc
            if b < 3:
                return ORO if sel else mix(c, osc, 0.5)
            if (y - y0) < 10:
                return claro
            return c if (x + y) % 9 else mix(c, claro, 0.4)
        redondo(x0, y0, x0 + 40, y0 + 56, 10, cinta)

# ================================================================ sello de cera (48 x 48) en 720,0
CERA, CERA_OSC, CERA_CLARA = (190, 26, 36), (130, 12, 22), (236, 90, 90)
for y in range(48):
    for x in range(48):
        d = math.hypot(x - 23.5, y - 23.5)
        borde = 21 + 1.6 * math.sin(math.atan2(y - 23.5, x - 23.5) * 9)
        if d > borde:
            continue
        c = CERA
        if d > borde - 3:
            c = CERA_OSC
        elif d < 15 and d > 13:
            c = CERA_OSC
        # estrella en relieve
        a = math.atan2(y - 23.5, x - 23.5) + math.pi / 2
        r_est = 9 * (0.55 + 0.45 * abs(math.cos(a * 2.5)))
        if d < r_est:
            c = CERA_CLARA if (x - 23.5) + (y - 23.5) < 0 else mix(CERA, CERA_CLARA, 0.4)
        if x < 22 and y < 14 and d < borde - 4 and d > 16:
            c = mix(c, (255, 255, 255), 0.25)
        put(720 + x, y, c)

# ================================================================ casillas (20 x 20): vacía en 720,52 y marcada en 744,52
TINTA = (70, 44, 26)
for k, marcada in ((0, False), (1, True)):
    x0, y0 = 720 + k * 24, 52
    for y in range(20):
        for x in range(20):
            if x < 2 or y < 2 or x > 17 or y > 17:
                put(x0 + x, y0 + y, TINTA)
            else:
                put(x0 + x, y0 + y, (250, 244, 226))
    if marcada:
        for t in range(14):
            # palomita verde
            if t < 5:
                x, y = 4 + t, 10 + t
            else:
                x, y = 9 + (t - 5), 15 - (t - 5) * 1.2
            for dx in range(3):
                for dy in range(3):
                    put(x0 + int(x) + dx - 1, y0 + int(y) + dy - 1, (40, 150, 60))

# ================================================================ botón (100 x 28): normal 720,80 / encima 720,112 / apagado 720,144
for k, (base, borde) in enumerate((((46, 138, 72), ORO), ((70, 178, 96), ORO_CLARO), ((150, 150, 150), (110, 110, 110)))):
    x0, y0 = 720, 80 + k * 32

    def boton(x, y, b, base=base, borde=borde, y0=y0):
        if b < 2:
            return borde
        if b < 3:
            return mix(borde, (0, 0, 0), 0.3)
        if y - y0 < 10:
            return mix(base, (255, 255, 255), 0.18)
        return base
    redondo(x0, y0, x0 + 100, y0 + 28, 8, boton)

# ================================================================ estrellitas (16 x 16): llena 720,176 / vacía 740,176
for k, llena in ((0, True), (1, False)):
    x0, y0 = 720 + k * 20, 176
    for y in range(16):
        for x in range(16):
            a = math.atan2(y - 8, x - 7.5) + math.pi / 2
            d = math.hypot(x - 7.5, y - 8)
            r_est = 7.5 * (0.45 + 0.55 * abs(math.cos(a * 2.5)) ** 1.5)
            if d <= r_est:
                if llena:
                    c = ORO_CLARO if x + y < 13 else ORO
                else:
                    c = (200, 184, 150) if d > r_est - 1.5 else (238, 226, 196)
                put(x0 + x, y0 + y, c)
            elif d <= r_est + 1:
                put(x0 + x, y0 + y, ORO_OSC if llena else (170, 150, 116))

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print('libro.png listo')
