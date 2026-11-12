#!/usr/bin/env python3
"""Escenario de las carreras de caballos para la pantalla gigante (atlas 256x256, estilo pixel art que se repite de lado).

Regiones (x, y, ancho, alto) — las mismas constantes están en client/RaceScreenRenderer.java:
  STANDS  (0, 0, 256, 64)    cielo, gradas con público y techo, banderines
  HILLS   (0, 64, 128, 24)   lomas y árboles lejanos (capa de fondo, va más lento)
  GRASS   (128, 64, 64, 16)  pasto con la valla interior
  DIRT    (192, 64, 64, 16)  pista de arena con huellas
  RAIL    (128, 80, 64, 8)   valla blanca del frente
  GATE    (0, 96, 32, 48)    un cajón de salida
  FINISH  (32, 96, 16, 64)   poste de meta a cuadros
  POLE    (48, 96, 8, 40)    poste de distancia
  PANEL   (64, 96, 64, 64)   fondo de los paneles de información (degradado con marco)
  WHITE   (248, 248, 8, 8)   blanco liso (rectángulos teñidos)

    python3 casino/herramientas/race_scene.py
"""
import math, os, random
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'casino', 'textures', 'entity')
R = random.Random(42)
im = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
px = im.load()


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def put(x, y, c, a=255):
    px[x, y] = tuple(c) + (a,)


# ---------------------------------------------------------------- gradas (se repiten cada 256)
for y in range(64):
    for x in range(256):
        put(x, y, mix((88, 150, 230), (180, 214, 250), y / 26))          # cielo
for _ in range(10):                                                     # nubes
    cx, cy, w = R.randrange(256), R.randrange(3, 14), R.randrange(10, 24)
    for x in range(-w, w):
        for y in range(-3, 4):
            if (x / w) ** 2 + (y / 3.5) ** 2 < 1:
                put((cx + x) % 256, cy + y, (244, 248, 255))
# edificio de gradas: techo verde con postes, público de colores, frente blanco
for x in range(256):
    for y in range(22, 27):
        put(x, y, mix((40, 96, 64), (24, 64, 44), (y - 22) / 5))       # techo
    if x % 32 in (0, 1):
        for y in range(27, 50):
            put(x, y, (226, 224, 214))                                   # columnas
CROWD = [(220, 60, 60), (60, 100, 220), (240, 220, 80), (240, 240, 240), (60, 180, 90), (230, 140, 50), (180, 80, 200), (40, 40, 44)]
for y in range(27, 50):
    for x in range(256):
        if x % 32 in (0, 1):
            continue
        row = (y - 27) // 3
        shade = 0.55 + 0.45 * (y - 27) / 23
        if (y - 27) % 3 == 0:
            put(x, y, mix((60, 50, 44), (110, 96, 80), shade))             # escalón
        else:
            c = CROWD[(x * 7 + row * 13 + R.randrange(3)) % len(CROWD)] if R.random() < 0.85 else (226, 190, 150)
            put(x, y, mix((30, 26, 24), c, shade))
for y in range(50, 64):
    for x in range(256):
        put(x, y, mix((236, 234, 226), (196, 194, 186), (y - 50) / 14))   # frente de las gradas
for x in range(256):                                                    # banderines
    if x % 16 < 8:
        for y in range(19, 22):
            if abs((x % 16) - 4) <= (21 - y) * 1.5:
                put(x, y, CROWD[(x // 16) % 7])
for x in range(0, 256, 64):                                             # cartel del casino en las gradas
    for y in range(53, 61):
        for xx in range(x + 20, x + 44):
            put(xx, y, (24, 22, 30) if y in (53, 60) or xx in (x + 20, x + 43) else (40, 30, 70))
    for xx in range(x + 24, x + 40):
        if (xx - x) % 3 != 0:
            put(xx, 57, (240, 200, 80))

# ---------------------------------------------------------------- lomas y árboles (0,64,128,24)
for y in range(24):
    for x in range(128):
        h = 10 + 5 * math.sin(x / 128 * 2 * math.pi * 2) + 3 * math.sin(x / 128 * 2 * math.pi * 5 + 1)
        if 24 - y < h:
            put(x, 64 + y, mix((96, 150, 80), (64, 120, 60), y / 24))
for k in range(18):
    tx = R.randrange(128)
    ty = R.randrange(8, 16)
    for dy in range(-6, 4):
        for dx in range(-4, 5):
            if dx * dx + (dy + 1) ** 2 * 0.7 < 16:
                put((tx + dx) % 128, 64 + min(23, max(0, ty + dy)), mix((36, 90, 46), (60, 130, 60), (dy + 6) / 10))

# ---------------------------------------------------------------- pasto con valla interior (128,64,64,16)
for y in range(16):
    for x in range(64):
        g = mix((70, 150, 60), (54, 128, 50), ((x // 4 + y // 2) % 2) * 0.5 + R.random() * 0.3)
        put(128 + x, 64 + y, g)
for x in range(64):
    put(128 + x, 64 + 3, (240, 240, 236)); put(128 + x, 64 + 4, (200, 200, 196))
    if x % 16 in (0, 1):
        for y in range(3, 12):
            put(128 + x, 64 + y, (230, 230, 226) if x % 16 == 0 else (190, 190, 186))

# ---------------------------------------------------------------- arena (192,64,64,16)
for y in range(16):
    for x in range(64):
        c = mix((176, 128, 82), (150, 104, 66), R.random() * 0.6)
        if (x + y * 3) % 23 == 0:
            c = (120, 84, 54)
        put(192 + x, 64 + y, c)

# ---------------------------------------------------------------- valla del frente (128,80,64,8)
for x in range(64):
    for y in range(8):
        a = 0
        if y in (1, 2):
            put(128 + x, 80 + y, (246, 246, 242) if y == 1 else (206, 206, 200)); a = 1
        if x % 16 in (0, 1) and y >= 1:
            put(128 + x, 80 + y, (236, 236, 230) if x % 16 == 0 else (196, 196, 190)); a = 1

# ---------------------------------------------------------------- cajón de salida (0,96,32,48)
for y in range(48):
    for x in range(32):
        frame = x < 3 or x > 28 or y < 4
        bars = 6 <= y <= 44 and x in (10, 11, 20, 21) and y % 2 == 0
        if frame:
            put(x, 96 + y, (46, 120, 66) if x not in (0, 31) and y != 0 else (28, 80, 44))
        elif bars:
            put(x, 96 + y, (200, 200, 190))
        elif y < 10:
            put(x, 96 + y, (36, 96, 54))
for x in range(5, 27):
    for y in range(4, 8):
        put(x, 96 + y, (244, 240, 220))

# ---------------------------------------------------------------- poste de meta (32,96,16,64)
for y in range(64):
    for x in range(16):
        if y < 16:
            put(32 + x, 96 + y, (20, 20, 20) if ((x // 4) + (y // 4)) % 2 else (246, 246, 246))
        elif 6 <= x <= 9:
            put(32 + x, 96 + y, (236, 60, 60) if (y // 6) % 2 else (246, 246, 246))

# ---------------------------------------------------------------- poste de distancia (48,96,8,40)
for y in range(40):
    for x in range(8):
        if y < 8:
            put(48 + x, 96 + y, (240, 200, 40) if 1 <= x <= 6 else (40, 40, 40))
        elif 3 <= x <= 4:
            put(48 + x, 96 + y, (246, 246, 246))

# ---------------------------------------------------------------- panel (64,96,64,64)
for y in range(64):
    for x in range(64):
        edge = x in (0, 63) or y in (0, 63)
        edge2 = x in (1, 62) or y in (1, 62)
        if edge:
            put(64 + x, 96 + y, (200, 160, 60))
        elif edge2:
            put(64 + x, 96 + y, (120, 90, 30))
        else:
            put(64 + x, 96 + y, mix((26, 22, 44), (10, 10, 18), y / 64))

# ---------------------------------------------------------------- blanco (248,248,8,8) para rectángulos de color
for y in range(248, 256):
    for x in range(248, 256):
        put(x, y, (255, 255, 255))

os.makedirs(OUT, exist_ok=True)
im.save(os.path.join(OUT, 'race_scene.png'))
im.resize((512, 512), Image.NEAREST).save('/tmp/race_scene_big.png')
print('ok')
