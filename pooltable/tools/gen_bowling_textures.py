#!/usr/bin/env python3
"""Genera las texturas del boliche (madera de la pista, material oscuro, pino, bola e icono de la bola).
Uso: python3 pooltable/tools/gen_bowling_textures.py   (escribe en pooltable/src/main/resources/assets/pooltable/textures)"""
import math, os, random
from PIL import Image, ImageDraw

root = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/pooltable/textures")
random.seed(5)

def save(img, rel):
    p = os.path.join(root, rel); os.makedirs(os.path.dirname(p), exist_ok=True); img.save(p); print("ok", rel)

# --- pista: tablas de arce claro a lo largo del eje horizontal (u)
img = Image.new("RGB", (16, 16))
px = img.load()
for row in range(16):
    plank = row // 4
    base = [(222, 184, 120), (214, 175, 110), (228, 192, 128), (218, 180, 116)][plank]
    for x in range(16):
        n = random.randint(-7, 7)
        grain = int(5 * math.sin(x * 0.9 + plank * 2.1))
        px[x, row] = tuple(max(0, min(255, c + n + grain)) for c in base)
    if row % 4 == 3:
        for x in range(16):
            px[x, row] = tuple(int(c * 0.8) for c in px[x, row])
save(img, "block/bowling_lane.png")

# --- material oscuro (canaletas, foso, laterales, linea de falta)
img = Image.new("RGB", (16, 16))
px = img.load()
for y in range(16):
    for x in range(16):
        v = 38 + random.randint(-5, 5) + (6 if (x + y) % 8 == 0 else 0)
        px[x, y] = (v, v + 3, v + 8)
save(img, "block/bowling_dark.png")

# --- pino: blanco marfil con dos franjas rojas en el cuello; 32 x 64 (v = 0 punta, v = 1 base). Cuello a ~63% de altura desde la base.
img = Image.new("RGB", (32, 64), (244, 241, 232))
d = ImageDraw.Draw(img)
for y in range(64):
    h = 1 - y / 63.0
    if 0.62 <= h <= 0.66 or 0.69 <= h <= 0.73:
        d.line([(0, y), (31, y)], fill=(200, 28, 36))
    if h < 0.03:
        d.line([(0, y), (31, y)], fill=(200, 200, 196))
    if h > 0.94:
        d.line([(0, y), (31, y)], fill=(176, 170, 156))
for x in range(32):  # sombreado suave vertical para que no se vea plano
    s = int(10 * math.sin(x / 32 * 2 * math.pi))
    for y in range(64):
        r, g, b = img.getpixel((x, y))
        img.putpixel((x, y), (max(0, min(255, r + s)), max(0, min(255, g + s)), max(0, min(255, b + s))))
save(img, "entity/bowling_pin.png")

# --- bola: azul oscuro veteado con tres agujeros; 64 x 32 equirectangular
W, H = 64, 32
img = Image.new("RGB", (W, H))
px = img.load()
for y in range(H):
    for x in range(W):
        t = math.sin(x * 0.35 + math.sin(y * 0.5) * 1.8) * 0.5 + 0.5
        t2 = math.sin(y * 0.8 + x * 0.12) * 0.5 + 0.5
        k = 0.55 * t + 0.25 * t2
        px[x, y] = (int(14 + 50 * k), int(24 + 70 * k), int(70 + 120 * k))
holes = [(16, 12), (19, 11), (17, 15)]
for hx, hy in holes:
    for dy in range(-1, 2):
        for dx in range(-1, 2):
            if dx * dx + dy * dy <= 1:
                px[(hx + dx) % W, hy + dy] = (4, 4, 8)
save(img, "entity/bowling_ball.png")

# --- icono de la bola (16 x 16)
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
px = img.load()
cx = cy = 7.5
for y in range(16):
    for x in range(16):
        dd = math.hypot(x - cx, y - cy)
        if dd <= 6.6:
            lit = max(0.0, 1.0 - math.hypot(x - 5.2, y - 4.8) / 9.0)
            k = 0.35 + 0.65 * lit
            px[x, y] = (int(18 + 60 * k), int(30 + 90 * k), int(80 + 150 * k), 255)
        elif dd <= 7.4:
            px[x, y] = (8, 10, 24, 255)
for hx, hy in [(8, 5), (10, 6), (9, 8)]:
    px[hx, hy] = (4, 4, 10, 255)
px[4, 4] = (190, 215, 255, 255)
px[5, 3] = (150, 190, 250, 255)
save(img, "item/bowling_ball.png")

# --- textura del pino para el modelo del item (el atlas solo toma block/ e item/): blanco con franja roja
img = Image.new("RGB", (16, 16), (244, 241, 232))
d = ImageDraw.Draw(img)
d.rectangle([0, 1, 15, 1], fill=(200, 28, 36))
d.rectangle([0, 3, 15, 3], fill=(200, 28, 36))
save(img, "block/bowling_pin_icon.png")
