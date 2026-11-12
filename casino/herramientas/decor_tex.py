#!/usr/bin/env python3
"""Texturas de la decoración del casino (estilo Vegas clásico, 32x32). Se generan con código para poder rehacerlas.

    python3 casino/herramientas/decor_tex.py
"""
import math
import os
import random

from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino", "textures", "block", "decor")
N = 32

VELVET = {  # (oscuro, medio, brillo)
    "red": ((74, 8, 14), (150, 20, 32), (214, 62, 70)),
    "black": ((14, 12, 16), (38, 34, 42), (88, 80, 96)),
    "purple": ((40, 12, 58), (96, 32, 128), (160, 86, 196)),
    "blue": ((10, 22, 70), (28, 56, 150), (86, 124, 214)),
}


def clamp(v):
    return max(0, min(255, int(v)))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(clamp(x * f) for x in c)


def img():
    return Image.new("RGBA", (N, N), (0, 0, 0, 255))


def put(im, x, y, c, a=255):
    if 0 <= x < N and 0 <= y < N:
        im.putpixel((x, y), (*c, a))


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"))


# ------------------------------------------------------------------ terciopelo
def velvet(name, cols, tufted):
    dark, mid, hi = cols
    rnd = random.Random(name)
    im = img()
    for y in range(N):
        for x in range(N):
            # brillo suave de tela (ondas diagonales) + grano
            s = 0.5 + 0.22 * math.sin((x + y * 0.6) / 5.0) + 0.12 * math.sin((x * 0.7 - y) / 3.1)
            s += rnd.uniform(-0.09, 0.09)
            c = mix(dark, mid, min(1, max(0, s))) if s < 1 else mix(mid, hi, s - 1)
            if s > 0.86:
                c = mix(c, hi, (s - 0.86) * 1.6)
            put(im, x, y, c)
    if tufted:
        # capitoné: rombos con pliegues oscuros y un botón dorado en cada cruce
        for y in range(N):
            for x in range(N):
                u = (x + y) % 16
                v = (x - y) % 16
                if u == 0 or v == 0:
                    put(im, x, y, shade(dark, 0.75))
                elif u == 1 or v == 1:
                    put(im, x, y, mix(dark, mid, 0.35))
                elif u == 15 or v == 15:
                    put(im, x, y, mix(mid, hi, 0.45))
        for (bx, by) in [(0, 0), (16, 0), (8, 8), (24, 8), (0, 16), (16, 16), (8, 24), (24, 24)]:
            for dx, dy, c in [(0, 0, (240, 205, 90)), (1, 0, (190, 140, 40)), (0, 1, (190, 140, 40)), (1, 1, (120, 84, 20))]:
                put(im, (bx + dx) % N, (by + dy) % N, c)
    save(im, f"velvet_{name}" + ("_tufted" if tufted else ""))


# ------------------------------------------------------------------ metales
def metal(name, base, hi, lo, bands=True):
    rnd = random.Random(name)
    im = img()
    for y in range(N):
        for x in range(N):
            s = 0.55 + 0.25 * math.sin(y / 32 * math.pi * 2 + 0.6) + rnd.uniform(-0.05, 0.05)
            if bands and (y % 11 in (2, 3)):
                s += 0.3
            s += 0.05 * math.sin(x * 1.7)
            c = mix(lo, base, min(1, s * 1.4)) if s < 0.7 else mix(base, hi, (s - 0.7) / 0.6)
            put(im, x, y, c)
    save(im, name)


# ------------------------------------------------------------------ madera
def wood(name, dark, light, panel=False):
    rnd = random.Random(name)
    im = img()
    rings = [rnd.uniform(0, 6.28) for _ in range(4)]
    for y in range(N):
        for x in range(N):
            g = math.sin(y * 1.1 + 0.9 * math.sin(x * 0.12 + rings[0]) + 0.35 * math.sin(x * 0.41 + rings[1]))
            s = 0.5 + 0.28 * g + rnd.uniform(-0.06, 0.06)
            if (y + int(3 * math.sin(x * 0.2 + rings[2]))) % 9 == 0:
                s -= 0.25
            put(im, x, y, mix(dark, light, max(0, min(1, s))))
    if panel:
        # panel tallado: marco con bisel
        for i in range(N):
            for t, f in [(2, 1.35), (3, 1.15), (28, 0.65), (29, 0.55)]:
                for (x, y) in [(i, t), (t, i)]:
                    if 2 <= i <= 29:
                        c = im.getpixel((x, y))[:3]
                        put(im, x, y, shade(c, f))
            for t, f in [(28, 0.65), (29, 0.55)]:
                if 2 <= i <= 29:
                    for (x, y) in [(i, t), (t, i)]:
                        c = im.getpixel((x, y))[:3]
                        put(im, x, y, shade(c, f))
        # línea dorada interior
        for i in range(5, 27):
            for (x, y) in [(i, 5), (i, 26), (5, i), (26, i)]:
                put(im, x, y, (214, 172, 70))
    save(im, name)


# ------------------------------------------------------------------ mármol
def marble(name, base, vein, vein2):
    rnd = random.Random(name)
    im = img()
    for y in range(N):
        for x in range(N):
            put(im, x, y, mix(base, shade(base, 0.93), rnd.random() * 0.6))
    for _ in range(3):
        x, y = rnd.uniform(0, N), rnd.uniform(0, N)
        ang = rnd.uniform(0, math.pi)
        for _ in range(70):
            ang += rnd.uniform(-0.5, 0.5)
            x = (x + math.cos(ang)) % N
            y = (y + math.sin(ang)) % N
            put(im, int(x), int(y), vein)
            if rnd.random() < 0.4:
                put(im, int(x) + 1, int(y), vein2)
    save(im, name)


# ------------------------------------------------------------------ laca negra
def lacquer(name, base=(18, 16, 20)):
    rnd = random.Random(name)
    im = img()
    for y in range(N):
        for x in range(N):
            s = 1.0 + 0.6 * max(0, 1 - abs((x - y) - 6) / 4.0) + rnd.uniform(-0.08, 0.08)
            put(im, x, y, shade(base, s) if s <= 1 else mix(base, (90, 86, 100), (s - 1) * 0.9))
    save(im, name)


# ------------------------------------------------------------------ vidrio y bebidas
def glass(name, tint, alpha_body=255):
    im = img()
    for y in range(N):
        for x in range(N):
            s = 0.75 + 0.25 * math.sin(x / N * math.pi)
            c = shade(tint, s)
            if x in (6, 7) or x in (22,):
                c = mix(c, (255, 255, 255), 0.65)
            put(im, x, y, c, alpha_body)
    save(im, name)


def bottle(name, tint, label):
    rnd = random.Random(name)
    im = img()
    for y in range(N):
        for x in range(N):
            s = 0.6 + 0.4 * math.sin(x / N * math.pi) + rnd.uniform(-0.04, 0.04)
            c = shade(tint, s)
            if x in (8, 9):
                c = mix(c, (255, 255, 255), 0.55)
            put(im, x, y, c)
    # etiqueta
    for y in range(12, 22):
        for x in range(N):
            c = label if y not in (12, 21) else (214, 172, 70)
            put(im, x, y, shade(c, 0.85 + 0.15 * math.sin(x / N * math.pi)))
    for x in range(10, 22, 3):
        put(im, x, 16, (60, 30, 20))
        put(im, x + 1, 17, (60, 30, 20))
    save(im, name)


def liquid(name, c, bubbles=False):
    rnd = random.Random(name)
    im = img()
    for y in range(N):
        for x in range(N):
            put(im, x, y, shade(c, 0.85 + 0.25 * (1 - y / N) + rnd.uniform(-0.05, 0.05)))
    if bubbles:
        for _ in range(14):
            put(im, rnd.randrange(N), rnd.randrange(N), mix(c, (255, 255, 230), 0.7))
    save(im, name)


def main():
    for k, cols in VELVET.items():
        velvet(k, cols, False)
        velvet(k, cols, True)
    metal("gold", (212, 168, 58), (255, 236, 150), (120, 82, 20))
    metal("chrome", (170, 174, 184), (245, 248, 255), (70, 74, 84))
    metal("brass", (180, 140, 60), (240, 210, 130), (90, 62, 24), bands=False)
    wood("mahogany", (46, 18, 12), (110, 46, 28))
    wood("mahogany_panel", (52, 20, 14), (118, 50, 30), panel=True)
    marble("marble", (236, 230, 218), (176, 160, 140), (204, 190, 170))
    marble("marble_black", (30, 28, 32), (196, 160, 80), (120, 100, 60))
    lacquer("lacquer")
    glass("glass", (210, 232, 240))
    bottle("bottle_green", (30, 92, 40), (238, 226, 196))
    bottle("bottle_amber", (130, 70, 18), (24, 24, 28))
    bottle("bottle_clear", (190, 214, 220), (150, 20, 32))
    bottle("bottle_red", (110, 14, 30), (238, 226, 196))
    bottle("bottle_blue", (24, 44, 120), (214, 172, 70))
    liquid("drink_whiskey", (176, 98, 26))
    liquid("drink_cocktail", (214, 40, 80))
    liquid("drink_champagne", (236, 206, 110), bubbles=True)
    liquid("drink_blue", (40, 140, 220))
    # cubos de hielo / aceituna / cereza
    im = img()
    for y in range(N):
        for x in range(N):
            put(im, x, y, (200, 30, 40) if (x // 8 + y // 8) % 2 == 0 else (90, 150, 50))
    save(im, "garnish")
    print("texturas listas en", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
