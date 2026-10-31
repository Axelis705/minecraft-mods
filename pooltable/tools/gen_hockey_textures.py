#!/usr/bin/env python3
"""Genera las texturas del hockey de mesa (gabinete, superficie de juego y colores lisos para disco, mazos y lineas).
Uso: python3 pooltable/tools/gen_hockey_textures.py"""
import os, random
from PIL import Image

root = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/pooltable/textures/block")
random.seed(11)

def save(img, name):
    os.makedirs(root, exist_ok=True); img.save(os.path.join(root, name)); print("ok", name)

def solid(name, rgb, noise=4):
    img = Image.new("RGB", (16, 16)); px = img.load()
    for y in range(16):
        for x in range(16):
            n = random.randint(-noise, noise)
            px[x, y] = tuple(max(0, min(255, c + n)) for c in rgb)
    save(img, name)

# gabinete: rojo oscuro con franjas negras
img = Image.new("RGB", (16, 16)); px = img.load()
for y in range(16):
    for x in range(16):
        n = random.randint(-5, 5)
        base = (30, 30, 34) if y in (0, 1, 14, 15) else (150, 28, 34)
        px[x, y] = tuple(max(0, min(255, c + n)) for c in base)
save(img, "hockey_body.png")

# superficie: azul muy claro con puntitos (los agujeros del aire)
img = Image.new("RGB", (16, 16)); px = img.load()
for y in range(16):
    for x in range(16):
        base = (214, 238, 248)
        if x % 4 == 1 and y % 4 == 1:
            base = (178, 214, 232)
        n = random.randint(-3, 3)
        px[x, y] = tuple(max(0, min(255, c + n)) for c in base)
save(img, "hockey_surface.png")

solid("hockey_red.png", (216, 48, 48))
solid("hockey_blue.png", (48, 96, 224))
solid("hockey_black.png", (20, 20, 24), 3)
solid("hockey_white.png", (235, 235, 240), 3)
