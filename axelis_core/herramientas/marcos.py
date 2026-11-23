"""Marcos de las pantallas del Core (texturas de 32x32 que se estiran por partes: esquinas de 10x10 y bordes que se repiten).

El centro queda transparente (el fondo lo pone el tema con su degradado). Uso: python3 marcos.py <carpeta textures/gui>
"""
import math
import os
import random
import sys

from PIL import Image

S = 32       # tamaño de la textura
C = 10       # esquina
T = 6        # grosor del borde (sin contar el contorno)


def hexc(s, a=255):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def shade(c, k):
    return mix(c, (255, 255, 255, c[3]), k) if k > 0 else mix(c, (0, 0, 0, c[3]), -k)


def ring_depth(x, y):
    """Qué tan adentro del borde está el pixel (0 = contorno de afuera); None si está en el centro."""
    d = min(x, y, S - 1 - x, S - 1 - y)
    return d if d <= T else None


def side(x, y):
    d = {"arriba": y, "abajo": S - 1 - y, "izq": x, "der": S - 1 - x}
    return min(d, key=d.get)


def base_frame(material, outline="#06080D", inner_hi=0.28, inner_lo=-0.35):
    """Borde con relieve: contorno oscuro, material, filo claro adentro arriba/izquierda y oscuro abajo/derecha."""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    p = img.load()
    for y in range(S):
        for x in range(S):
            d = ring_depth(x, y)
            if d is None:
                continue
            if d == 0:
                p[x, y] = hexc(outline)
                continue
            c = material(x, y, d, side(x, y))
            sd = side(x, y)
            if d == 1:
                c = shade(c, 0.22 if sd in ("arriba", "izq") else -0.25)
            elif d == T:
                c = shade(c, inner_lo if sd in ("arriba", "izq") else inner_hi * 0.6)
            p[x, y] = c
    return img


def put(img, x, y, c):
    if 0 <= x < S and 0 <= y < S:
        img.putpixel((x, y), c)


def mirror_corners(img, draw_corner):
    """Dibuja algo en la esquina de arriba a la izquierda y lo copia a las otras tres (espejo)."""
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    draw_corner(layer)
    tl = layer.crop((0, 0, C, C))
    for fx, fy in ((False, False), (True, False), (False, True), (True, True)):
        t = tl
        if fx:
            t = t.transpose(Image.FLIP_LEFT_RIGHT)
        if fy:
            t = t.transpose(Image.FLIP_TOP_BOTTOM)
        img.alpha_composite(t, (S - C if fx else 0, S - C if fy else 0))


def madera():
    rnd = random.Random(7)
    grain = [[rnd.random() for _ in range(S)] for _ in range(S)]
    light, mid, dark = hexc("#A8743F"), hexc("#86592C"), hexc("#5E3D1C")

    def mat(x, y, d, sd):
        along = x if sd in ("arriba", "abajo") else y
        across = d
        # vetas a lo largo del tablón
        v = math.sin(along * 0.55 + across * 1.7 + grain[y][x] * 1.3)
        c = mix(mid, light, max(0, v) * 0.55) if v > 0 else mix(mid, dark, -v * 0.45)
        if grain[y][x] > 0.93:
            c = shade(c, -0.25)
        return c

    img = base_frame(mat, outline="#1E1208")

    def corner(l):
        # escuadra de hierro y clavo de latón
        iron, iron_hi = hexc("#3B3F4A"), hexc("#6B7280")
        for i in range(1, 8):
            put(l, i, 1, iron)
            put(l, 1, i, iron)
            put(l, i, 2, iron_hi if i < 7 else iron)
            put(l, 2, i, iron_hi if i < 7 else iron)
        for (x, y, c) in ((4, 4, "#F6D77A"), (5, 4, "#C9962E"), (4, 5, "#C9962E"), (5, 5, "#8A6114")):
            put(l, x, y, hexc(c))

    mirror_corners(img, corner)
    return img


def pergamino():
    rnd = random.Random(3)
    paper, edge, burnt = hexc("#EFDDB4"), hexc("#D2B27A"), hexc("#8C6235")

    def mat(x, y, d, sd):
        n = rnd.random()
        c = mix(paper, edge, (T - d) / T * 0.8)
        if d <= 2 and n > 0.55:
            c = mix(c, burnt, 0.5)
        if n > 0.97:
            c = shade(c, -0.12)
        return c

    img = base_frame(mat, outline="#4A2E12", inner_hi=0.1, inner_lo=-0.18)

    def corner(l):
        # rizo del papel enrollado
        curl = ["..XXX.", ".XoooX", "XooXoX", "XoX.oX", "XooooX", ".XXXX."]
        for r, row in enumerate(curl):
            for q, ch in enumerate(row):
                if ch == "X":
                    put(l, q + 1, r + 1, hexc("#6E4A22"))
                elif ch == "o":
                    put(l, q + 1, r + 1, hexc("#E8CF98"))

    mirror_corners(img, corner)
    return img


def noche():
    steel, steel_hi = hexc("#2B3247"), hexc("#3E4866")
    gold, gold_hi, gold_lo = hexc("#E8B84A"), hexc("#FFE6A0"), hexc("#A8781E")

    def mat(x, y, d, sd):
        along = x if sd in ("arriba", "abajo") else y
        c = mix(steel, steel_hi, 0.5 + 0.5 * math.sin(along * 0.9) * 0.3)
        if d == 3:
            c = gold_lo if along % 4 else gold   # hilo dorado con puntadas
        return c

    img = base_frame(mat, outline="#05070C")

    def corner(l):
        # filigrana dorada: rombo con remate
        pts = [(4, 1), (3, 2), (5, 2), (2, 3), (6, 3), (1, 4), (7, 4), (2, 5), (6, 5), (3, 6), (5, 6), (4, 7)]
        for (x, y) in pts:
            put(l, x, y, gold)
        put(l, 4, 4, gold_hi)
        put(l, 4, 3, gold_lo)
        put(l, 4, 5, gold_lo)
        put(l, 3, 4, gold_lo)
        put(l, 5, 4, gold_lo)

    mirror_corners(img, corner)
    return img


def mar():
    deep, foam = hexc("#14495A"), hexc("#2C7C92")
    rope, rope_d = hexc("#D9C38E"), hexc("#9C8455")

    def mat(x, y, d, sd):
        along = x if sd in ("arriba", "abajo") else y
        if 2 <= d <= 4:
            # cuerda torcida: rayas en diagonal
            k = (along + (d if sd in ("arriba", "der") else -d)) % 4
            return rope if k < 2 else rope_d
        return mix(deep, foam, 0.4 if d == 5 else 0.0)

    img = base_frame(mat, outline="#04121A")

    def corner(l):
        # nudo redondo de la cuerda
        knot = [".XXXX.", "XooooX", "XoXXoX", "XoXXoX", "XooooX", ".XXXX."]
        for r, row in enumerate(knot):
            for q, ch in enumerate(row):
                if ch == "X":
                    put(l, q + 1, r + 1, hexc("#7C6639"))
                elif ch == "o":
                    put(l, q + 1, r + 1, hexc("#E8D3A0"))

    mirror_corners(img, corner)
    return img


def piedra():
    rnd = random.Random(11)
    s1, s2, mortar, moss = hexc("#8A8A8E"), hexc("#6E6E73"), hexc("#4A4A4F"), hexc("#5E8A3A")

    def mat(x, y, d, sd):
        along = x if sd in ("arriba", "abajo") else y
        brick = (along // 5 + (1 if d > 3 else 0)) % 2
        if along % 5 == 0 or d == 3:
            return mortar
        c = s1 if brick else s2
        if rnd.random() > 0.9:
            c = shade(c, -0.15)
        if d <= 2 and rnd.random() > 0.86:
            c = moss
        return c

    img = base_frame(mat, outline="#1C1C20")

    def corner(l):
        for (x, y) in ((2, 2), (3, 2), (2, 3), (3, 3)):
            put(l, x, y, hexc("#A8A8AE"))
        put(l, 2, 2, hexc("#C8C8CE"))

    mirror_corners(img, corner)
    return img


MARCOS = {"madera": madera, "pergamino": pergamino, "noche": noche, "mar": mar, "piedra": piedra}


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    for name, fn in MARCOS.items():
        fn().save(os.path.join(out, "marco_" + name + ".png"))
    print("marcos:", ", ".join(MARCOS))


if __name__ == "__main__":
    main()
