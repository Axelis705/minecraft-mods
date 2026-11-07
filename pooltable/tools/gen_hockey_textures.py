#!/usr/bin/env python3
"""Genera las texturas del hockey de mesa en estilo 'aero' (cristal azul, cian brillante, lima y magenta).
Uso: python3 pooltable/tools/gen_hockey_textures.py"""
import os, random, math
from PIL import Image

root = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/pooltable/textures/block")
random.seed(11)

def save(img, name):
    os.makedirs(root, exist_ok=True); img.save(os.path.join(root, name)); print("ok", name)

def clamp(c): return max(0, min(255, int(c)))

def lerp(a, b, t): return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))

def solid(name, rgb, noise=3):
    img = Image.new("RGB", (16, 16)); px = img.load()
    for y in range(16):
        for x in range(16):
            n = random.randint(-noise, noise)
            px[x, y] = tuple(clamp(c + n) for c in rgb)
    save(img, name)

# gabinete: degradado turquesa con una franja de brillo y una linea blanca abajo
img = Image.new("RGB", (16, 16)); px = img.load()
for y in range(16):
    base = lerp((120, 214, 244), (34, 150, 206), y / 15)
    if y in (2, 3):
        base = lerp(base, (255, 255, 255), 0.55 if y == 2 else 0.25)
    if y == 14:
        base = (224, 246, 255)
    if y == 15:
        base = (20, 110, 170)
    for x in range(16):
        n = random.randint(-2, 2)
        px[x, y] = tuple(clamp(c + n) for c in base)
save(img, "hockey_body.png")

# superficie: azul cristal claro con un degradado suave y puntitos de aire
img = Image.new("RGB", (16, 16)); px = img.load()
for y in range(16):
    for x in range(16):
        base = lerp((205, 228, 252), (178, 210, 246), (x + y) / 30)
        if x % 4 == 1 and y % 4 == 1:
            base = (140, 186, 232)
        n = random.randint(-2, 2)
        px[x, y] = tuple(clamp(c + n) for c in base)
save(img, "hockey_surface.png")

# riel: cian brillante con una linea blanca al centro (se dibuja a plena luz, como si brillara)
img = Image.new("RGB", (16, 16)); px = img.load()
for y in range(16):
    for x in range(16):
        base = (40, 208, 255)
        if y in (7, 8):
            base = (220, 250, 255)
        if y in (0, 15):
            base = (10, 140, 210)
        px[x, y] = base
save(img, "hockey_rail.png")

solid("hockey_red.png", (214, 40, 150))     # magenta (base del mazo rival)
solid("hockey_blue.png", (36, 170, 236))    # cian (lineas de la cancha)
solid("hockey_lime.png", (170, 230, 40))    # lima (base del mazo propio)
solid("hockey_black.png", (18, 24, 34), 2)
solid("hockey_white.png", (240, 248, 255), 2)

def glass(name, rgb, alpha):
    img = Image.new("RGBA", (16, 16)); px = img.load()
    for y in range(16):
        for x in range(16):
            k = 1 - y / 15
            c = lerp(rgb, (255, 255, 255), 0.35 * k)
            px[x, y] = (clamp(c[0]), clamp(c[1]), clamp(c[2]), alpha)
    save(img, name)

glass("hockey_glass_lime.png", (190, 245, 90), 120)
glass("hockey_glass_magenta.png", (200, 40, 140), 170)

# disco: lima con aro oscuro y centro blanco-verde (se ve desde arriba)
S = 32
img = Image.new("RGB", (S, S)); px = img.load()
for y in range(S):
    for x in range(S):
        d = math.hypot(x - S / 2 + 0.5, y - S / 2 + 0.5) / (S / 2)
        if d < 0.42: c = (232, 255, 150)
        elif d < 0.58: c = (30, 36, 30)
        elif d < 0.9: c = (150, 220, 40)
        else: c = (24, 30, 26)
        px[x, y] = c
save(img, "hockey_puck.png")
