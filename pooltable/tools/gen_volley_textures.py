#!/usr/bin/env python3
"""Genera las texturas del voleibol: piso azul y naranja, zona de afuera, red, poste, pelota, base del bloque y las dos pieles de los bots.
Uso: python3 pooltable/tools/gen_volley_textures.py"""
import os, random, math
from PIL import Image

base = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/pooltable/textures")
random.seed(5)

def save(img, folder, name):
    d = os.path.join(base, folder); os.makedirs(d, exist_ok=True)
    img.save(os.path.join(d, name)); print("ok", folder, name)

def clamp(c): return max(0, min(255, int(c)))
def lerp(a, b, t): return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))

def court(name, c0, c1):
    # degradado suave con un brillo diagonal (se estira sobre media cancha, por eso sin ruido fino)
    n = 32
    img = Image.new("RGB", (n, n)); px = img.load()
    for y in range(n):
        for x in range(n):
            t = (x * 0.4 + y * 0.6) / n
            c = lerp(c0, c1, min(1, t))
            gloss = max(0.0, 1 - abs((x - y) - 6) / 7.0) * 0.12
            c = lerp(c, (255, 255, 255), gloss)
            px[x, y] = tuple(clamp(v) for v in c)
    save(img, "block", name)

court("volley_court_a.png", (70, 170, 255), (30, 110, 220))
court("volley_court_b.png", (255, 170, 70), (240, 110, 30))

# zona de afuera: arena clara
img = Image.new("RGB", (16, 16)); px = img.load()
for y in range(16):
    for x in range(16):
        c = lerp((240, 224, 176), (226, 204, 152), (x + y) / 30)
        px[x, y] = tuple(clamp(v) for v in c)
save(img, "block", "volley_out.png")

def solid(name, rgb, folder="block", size=16):
    save(Image.new("RGB", (size, size), rgb), folder, name)

solid("volley_line.png", (255, 255, 255))
solid("volley_white.png", (246, 250, 255))

# red: malla blanca sobre transparente (cada cuadro de 4 px)
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = img.load()
for y in range(16):
    for x in range(16):
        if x % 4 == 0 or y % 4 == 0:
            px[x, y] = (245, 248, 255, 235)
save(img, "block", "volley_net.png")

# poste: gris claro con franjas azul y naranja
img = Image.new("RGB", (16, 16)); px = img.load()
for y in range(16):
    for x in range(16):
        c = (226, 232, 240)
        if y % 8 in (3, 4):
            c = (45, 140, 255) if (y // 8) % 2 == 0 else (255, 138, 30)
        if x in (0, 15):
            c = lerp(c, (120, 130, 150), 0.5)
        px[x, y] = tuple(clamp(v) for v in c)
save(img, "block", "volley_post.png")

# pelota de playa: franjas verticales (u = giro) y tapas blancas en los polos (v)
W, H = 64, 32
img = Image.new("RGB", (W, H)); px = img.load()
cols = [(255, 214, 40), (255, 255, 255), (40, 140, 255), (255, 255, 255), (255, 80, 60), (255, 255, 255)]
for y in range(H):
    for x in range(W):
        c = cols[(x * len(cols)) // W]
        if y < 3 or y >= H - 3:
            c = (250, 250, 250)
        # un poco de brillo arriba
        c = lerp(c, (255, 255, 255), max(0.0, 0.25 - abs(y - 8) / 30.0))
        px[x, y] = tuple(clamp(v) for v in c)
save(img, "block", "volley_ball.png")

# base del bloque (cara de arriba): mitad azul, mitad naranja, linea blanca central
img = Image.new("RGB", (16, 16)); px = img.load()
for y in range(16):
    for x in range(16):
        c = (40, 120, 230) if x < 8 else (245, 120, 30)
        if x in (7, 8):
            c = (255, 255, 255)
        if x in (0, 15) or y in (0, 15):
            c = (240, 246, 255)
        px[x, y] = tuple(clamp(v) for v in c)
save(img, "block", "volley_base.png")

# ---------------------------------------------------------------- pieles 64x64
def skin(name, jersey, jersey_dark, hair, skin_tone, short):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); px = img.load()
    def rect(x0, y0, w, h, c):
        for yy in range(y0, y0 + h):
            for xx in range(x0, x0 + w):
                px[xx, yy] = c + (255,) if len(c) == 3 else c
    shade = lambda c, f: tuple(clamp(v * f) for v in c)
    # cabeza (8x8x8) en (0,0): arriba (8,0) abajo (16,0) der (0,8) frente (8,8) izq (16,8) atras (24,8)
    rect(8, 0, 8, 8, hair); rect(16, 0, 8, 8, shade(skin_tone, 0.8))
    for fx in (0, 16, 24):
        rect(fx, 8, 8, 8, skin_tone)
        rect(fx, 8, 8, 3, hair)
    rect(24, 8, 8, 8, hair)
    rect(8, 8, 8, 8, skin_tone); rect(8, 8, 8, 3, hair)
    # cara: ojos y boca
    rect(9, 12, 2, 1, (255, 255, 255)); rect(13, 12, 2, 1, (255, 255, 255))
    rect(10, 12, 1, 1, (40, 60, 120)); rect(13, 12, 1, 1, (40, 60, 120))
    rect(11, 14, 2, 1, (170, 90, 80))
    # cintillo del color del equipo
    rect(8, 10, 8, 1, jersey); rect(0, 10, 8, 1, jersey); rect(16, 10, 8, 1, jersey); rect(24, 10, 8, 1, jersey)
    # torso (8x12x4) en (16,16): arriba (20,16) abajo (28,16) der (16,20) frente (20,20) izq (28,20) atras (32,20)
    rect(20, 16, 8, 4, jersey); rect(28, 16, 8, 4, shade(jersey, 0.8))
    for (tx, tw) in ((16, 4), (20, 8), (28, 4), (32, 8)):
        rect(tx, 20, tw, 12, jersey)
        rect(tx, 20, tw, 1, shade(jersey, 1.15))
        rect(tx, 28, tw, 4, short)           # short
    rect(23, 22, 2, 4, (255, 255, 255))      # numerito en el frente
    rect(35, 22, 2, 4, (255, 255, 255))      # y en la espalda
    # brazo derecho en (40,16): mangas cortas del color del equipo, antebrazo piel
    for (ax, aw) in ((40, 4), (44, 4), (48, 4), (52, 4)):
        rect(ax, 20, aw, 12, skin_tone)
        rect(ax, 20, aw, 4, jersey)
    rect(44, 16, 4, 4, jersey); rect(48, 16, 4, 4, skin_tone)
    # brazo izquierdo en (32,48)
    for (ax, aw) in ((32, 4), (36, 4), (40, 4), (44, 4)):
        rect(ax, 52, aw, 12, skin_tone)
        rect(ax, 52, aw, 4, jersey)
    rect(36, 48, 4, 4, jersey); rect(40, 48, 4, 4, skin_tone)
    # pierna derecha en (0,16) y pierna izquierda en (16,48): short arriba, piel, calcetin blanco, tenis del color del equipo
    for (lx, ly, ltop) in ((0, 20, (4, 16)), (16, 52, (20, 48))):
        for (px0, pw) in ((lx, 4), (lx + 4, 4), (lx + 8, 4), (lx + 12, 4)):
            rect(px0, ly, pw, 12, skin_tone)
            rect(px0, ly, pw, 4, short)
            rect(px0, ly + 7, pw, 3, (255, 255, 255))
            rect(px0, ly + 10, pw, 2, jersey_dark)
        rect(ltop[0], ltop[1], 4, 4, short)
        rect(ltop[0] + 4, ltop[1], 4, 4, jersey_dark)
    save(img, "entity", name)

skin("volley_a.png", (45, 140, 255), (20, 80, 190), (60, 40, 30), (240, 190, 150), (30, 60, 130))
skin("volley_b.png", (255, 138, 30), (190, 90, 10), (230, 190, 90), (225, 170, 130), (150, 70, 10))
