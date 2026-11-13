#!/usr/bin/env python3
"""Cuadros del casino (casino:casino_painting): 8 pinturas de 2x2 bloques con marco dorado, dibujadas aquí con código.

Cada dibujo se pinta a 256x256 con suavizado y se reduce a 64x64 (32 píxeles por bloque) para que se vea como pixel art fino.
Clic derecho en el cuadro cambia el dibujo (agachado, hacia atrás), como los letreros de neón.

    python3 casino/herramientas/paintings.py
"""
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import decor_models as dm  # noqa: E402
from decor_models import box, write, A, D  # noqa: E402

N = 256
OUT = os.path.join(A, "textures", "block", "decor")
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
FONT_SANS = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
NAMES = ["diamond", "derby", "roulette", "royal_flush", "dice", "chips", "vegas", "cocktail"]
GOLD = (222, 178, 74)
GOLD_D = (150, 104, 30)
GOLD_L = (255, 226, 140)


def grad(c0, c1, vertical=True):
    im = Image.new("RGB", (N, N))
    px = im.load()
    for i in range(N):
        t = i / (N - 1)
        c = tuple(int(c0[k] + (c1[k] - c0[k]) * t) for k in range(3))
        for j in range(N):
            px[(j, i) if vertical else (i, j)] = c
    return im


def radial(c_in, c_out, cx=N / 2, cy=N / 2, r=N * 0.75):
    im = Image.new("RGB", (N, N))
    px = im.load()
    for y in range(N):
        for x in range(N):
            t = min(1.0, math.hypot(x - cx, y - cy) / r)
            px[x, y] = tuple(int(c_in[k] + (c_out[k] - c_in[k]) * t) for k in range(3))
    return im


def deco_border(d, color=GOLD, inset=10, w=4):
    """Marco interior art déco: doble línea con escalones en las esquinas."""
    a, b = inset, N - 1 - inset
    d.rectangle([a, a, b, b], outline=color, width=w)
    a2, b2 = a + 10, b - 10
    d.rectangle([a2, a2, b2, b2], outline=color, width=2)
    for (x, y, sx, sy) in ((a, a, 1, 1), (b, a, -1, 1), (a, b, 1, -1), (b, b, -1, -1)):
        for k in range(3):
            s = 6 + k * 8
            d.line([(x, y + sy * s), (x + sx * s, y + sy * s), (x + sx * s, y)], fill=color, width=2)


def sunburst(d, cx, cy, n, color, r=N):
    for i in range(n):
        a0 = 2 * math.pi * i / n
        a1 = a0 + math.pi / n
        d.polygon([(cx, cy), (cx + r * math.cos(a0), cy + r * math.sin(a0)), (cx + r * math.cos(a1), cy + r * math.sin(a1))], fill=color)


def sparkle(d, x, y, s, color=(255, 255, 255)):
    d.polygon([(x, y - s), (x + s * 0.25, y - s * 0.25), (x + s, y), (x + s * 0.25, y + s * 0.25), (x, y + s),
               (x - s * 0.25, y + s * 0.25), (x - s, y), (x - s * 0.25, y - s * 0.25)], fill=color)


def text(d, xy, s, size, fill, font=FONT, anchor="mm"):
    d.text(xy, s, font=ImageFont.truetype(font, size), fill=fill, anchor=anchor)


# ------------------------------------------------------------------------------------------------ dibujos
def p_diamond():
    im = radial((40, 30, 90), (8, 6, 24))
    d = ImageDraw.Draw(im)
    sunburst(d, 128, 120, 24, (30, 24, 66))
    cx, top, mid, bot = 128, 70, 108, 205
    w_top, w_mid = 52, 86
    pts_crown = [(cx - w_top, top), (cx + w_top, top), (cx + w_mid, mid), (cx - w_mid, mid)]
    d.polygon(pts_crown, fill=(150, 220, 250))
    # facetas de la corona
    xs = [cx - w_mid + i * (2 * w_mid) / 6 for i in range(7)]
    xt = [cx - w_top + i * (2 * w_top) / 4 for i in range(5)]
    for i in range(6):
        col = [(120, 200, 240), (190, 240, 255), (100, 180, 230)][i % 3]
        j = min(4, round(i * 4 / 6))
        d.polygon([(xs[i], mid), (xs[i + 1], mid), (xt[min(4, j + 1)], top) if i % 2 else (xt[j], top)], fill=col)
    d.polygon([(xt[1], top), (xt[3], top), (cx, mid - 10)], fill=(220, 250, 255))
    # pabellón
    for i in range(6):
        col = [(70, 150, 220), (130, 210, 250), (50, 120, 200), (160, 225, 255), (80, 160, 225), (40, 100, 180)][i]
        d.polygon([(xs[i], mid), (xs[i + 1], mid), (cx, bot)], fill=col)
    d.line(pts_crown + [pts_crown[0]], fill=(235, 250, 255), width=2)
    d.line([(cx - w_mid, mid), (cx, bot), (cx + w_mid, mid)], fill=(235, 250, 255), width=2)
    for (x, y, s) in ((70, 64, 12), (190, 80, 9), (176, 150, 7), (84, 160, 6), (128, 54, 8)):
        sparkle(d, x, y, s)
    text(d, (128, 228), "THE DIAMOND", 20, GOLD_L)
    deco_border(d)
    return im


def p_derby():
    cx, cy = 128, 108
    hs = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    hd = ImageDraw.Draw(hs)
    # herradura en U (abertura hacia arriba, la de la buena suerte): media vuelta abajo y dos patas
    cx, cy, R, W = 128, 104, 52, 26
    hd.arc([cx - R, cy - R, cx + R, cy + R], 0, 180, fill=GOLD, width=W)
    for sx in (-1, 1):
        x0 = cx + sx * R - (W if sx > 0 else 0)
        hd.rectangle([x0, cy - 46, x0 + W, cy], fill=GOLD)
        hd.rectangle([x0 - 3, cy - 52, x0 + W + 3, cy - 44], fill=GOLD_D)
    hd.arc([cx - R + 8, cy - R + 8, cx + R - 8, cy + R - 8], 10, 170, fill=GOLD_L, width=5)
    for k in range(5):
        a = math.radians(15 + k * 37.5)
        px, py = cx + (R - 13) * math.cos(a), cy + (R - 13) * math.sin(a)
        hd.ellipse([px - 4, py - 4, px + 4, py + 4], fill=GOLD_D)
    for sx in (-1, 1):
        for yy in (cy - 30, cy - 12):
            px = cx + sx * (R - 13)
            hd.ellipse([px - 4, yy - 4, px + 4, yy + 4], fill=GOLD_D)
    base = grad((255, 150, 80), (120, 40, 100))
    bd = ImageDraw.Draw(base)
    bd.ellipse([24, 26, 70, 72], fill=(255, 220, 120))
    bd.rectangle([0, 150, N, N], fill=(46, 120, 50))
    for y in (150, 176, 208):
        bd.rectangle([0, y, N, y + 4], fill=(240, 240, 240))
        for x in range(0, N, 20):
            bd.rectangle([x, y - 10, x + 3, y + 4], fill=(240, 240, 240))
    base.paste(hs, (0, 0), hs)
    d = ImageDraw.Draw(base)
    for (x, y) in ((60, 70), (196, 70), (128, 36)):
        sparkle(d, x, y, 9, GOLD_L)
    text(d, (128, 232), "DERBY", 24, GOLD_L)
    deco_border(d)
    return base


def p_roulette():
    im = radial((30, 90, 50), (6, 30, 14))
    d = ImageDraw.Draw(im)
    cx = cy = 128
    d.ellipse([cx - 104, cy - 104, cx + 104, cy + 104], fill=(90, 50, 20))
    d.ellipse([cx - 96, cy - 96, cx + 96, cy + 96], fill=GOLD_D)
    n = 37
    for i in range(n):
        a0 = 2 * math.pi * i / n - math.pi / 2
        a1 = a0 + 2 * math.pi / n
        col = (30, 150, 60) if i == 0 else ((190, 20, 30) if i % 2 else (20, 20, 24))
        pts = [(cx + 92 * math.cos(a0), cy + 92 * math.sin(a0)), (cx + 92 * math.cos(a1), cy + 92 * math.sin(a1)),
               (cx + 64 * math.cos(a1), cy + 64 * math.sin(a1)), (cx + 64 * math.cos(a0), cy + 64 * math.sin(a0))]
        d.polygon(pts, fill=col)
        d.line([pts[0], pts[3]], fill=GOLD, width=1)
    d.ellipse([cx - 64, cy - 64, cx + 64, cy + 64], fill=(110, 60, 25))
    d.ellipse([cx - 44, cy - 44, cx + 44, cy + 44], fill=GOLD)
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        d.line([(cx, cy), (cx + 40 * math.cos(a), cy + 40 * math.sin(a))], fill=GOLD_L, width=5)
    d.ellipse([cx - 12, cy - 12, cx + 12, cy + 12], fill=GOLD_L)
    bx, by = cx + 78 * math.cos(-1.1), cy + 78 * math.sin(-1.1)
    d.ellipse([bx - 6, by - 6, bx + 6, by + 6], fill=(250, 250, 250))
    deco_border(d)
    return im


def p_royal_flush():
    im = radial((150, 20, 40), (40, 4, 12))
    d = ImageDraw.Draw(im)
    sunburst(d, 128, 250, 18, (120, 14, 32))
    ranks = ["10", "J", "Q", "K", "A"]
    for i, rk in enumerate(ranks):
        ang = (i - 2) * 14
        card = Image.new("RGBA", (78, 110), (0, 0, 0, 0))
        cd = ImageDraw.Draw(card)
        cd.rounded_rectangle([0, 0, 77, 109], 8, fill=(250, 248, 240), outline=(60, 60, 60), width=2)
        text(cd, (14, 16), rk, 18, (20, 20, 20), FONT_SANS)
        text(cd, (39, 60), "♠", 46, (20, 20, 20), "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")
        card = card.rotate(-ang, expand=True, resample=Image.BICUBIC)
        x = int(128 + (i - 2) * 30 - card.width / 2)
        y = int(128 - card.height / 2 + abs(i - 2) * 6)
        im.paste(card, (x, y), card)
    d = ImageDraw.Draw(im)
    text(d, (128, 226), "ROYAL FLUSH", 20, GOLD_L)
    deco_border(d)
    return im


def p_dice():
    im = grad((20, 60, 110), (6, 14, 40))
    d = ImageDraw.Draw(im)
    sunburst(d, 128, 128, 16, (26, 70, 124))

    def die(x, y, s, pips, rot):
        t = Image.new("RGBA", (s + 20, s + 20), (0, 0, 0, 0))
        td = ImageDraw.Draw(t)
        td.rounded_rectangle([10, 10, s + 9, s + 9], 16, fill=(205, 25, 35), outline=(120, 10, 20), width=3)
        td.rounded_rectangle([14, 14, s + 5, s / 2], 12, fill=(230, 60, 70))
        P = {1: [(.5, .5)], 2: [(.25, .25), (.75, .75)], 3: [(.25, .25), (.5, .5), (.75, .75)],
             4: [(.25, .25), (.75, .25), (.25, .75), (.75, .75)], 5: [(.25, .25), (.75, .25), (.5, .5), (.25, .75), (.75, .75)],
             6: [(.25, .25), (.75, .25), (.25, .5), (.75, .5), (.25, .75), (.75, .75)]}
        for (u, v) in P[pips]:
            px, py = 10 + u * s, 10 + v * s
            td.ellipse([px - s * .08, py - s * .08, px + s * .08, py + s * .08], fill=(250, 250, 250))
        t = t.rotate(rot, expand=True, resample=Image.BICUBIC)
        im.paste(t, (int(x - t.width / 2), int(y - t.height / 2)), t)

    die(92, 128, 96, 5, 12)
    die(168, 140, 88, 2, -16)
    d = ImageDraw.Draw(im)
    for (x, y, s) in ((50, 60, 8), (210, 66, 10), (200, 200, 7)):
        sparkle(d, x, y, s, GOLD_L)
    text(d, (128, 226), "LUCKY 7", 20, GOLD_L)
    deco_border(d)
    return im


def p_chips():
    im = radial((20, 110, 60), (6, 40, 20))
    d = ImageDraw.Draw(im)
    cols = [((200, 30, 40), (250, 250, 250)), ((30, 60, 170), (250, 250, 250)), ((20, 20, 20), GOLD), ((60, 150, 60), (250, 250, 250))]
    stacks = [(66, 200, 6, 0), (112, 196, 9, 1), (160, 202, 7, 2), (200, 198, 4, 3)]
    for (x, y, h, c) in stacks:
        main, stripe = cols[c]
        for k in range(h):
            yy = y - k * 12
            d.ellipse([x - 22, yy - 9, x + 22, yy + 9], fill=tuple(int(v * 0.7) for v in main))
            d.ellipse([x - 22, yy - 13, x + 22, yy + 5], fill=main)
            for a in range(0, 360, 60):
                r = math.radians(a)
                d.line([(x + 15 * math.cos(r), yy - 4 + 6 * math.sin(r)), (x + 21 * math.cos(r), yy - 4 + 8 * math.sin(r))], fill=stripe, width=3)
        d.ellipse([x - 10, y - h * 12 - 8, x + 10, y - h * 12 + 0], outline=stripe, width=2)
    text(d, (128, 50), "ALL IN", 30, GOLD_L)
    deco_border(d)
    return im


def p_vegas():
    im = grad((40, 10, 70), (230, 90, 120))
    d = ImageDraw.Draw(im)
    rnd = random.Random(7)
    for _ in range(60):
        x, y = rnd.randrange(14, 242), rnd.randrange(14, 120)
        d.point((x, y), fill=(255, 255, 230))
    d.ellipse([170, 30, 210, 70], fill=(255, 245, 200))
    d.ellipse([180, 26, 220, 66], fill=(60, 16, 80))
    towers = [(14, 150), (34, 120), (58, 160), (78, 96), (104, 140), (124, 70), (150, 130), (172, 100), (196, 150), (220, 118), (242, 160)]
    for i, (x, h) in enumerate(towers):
        w = 22 if i % 2 else 18
        d.rectangle([x - w / 2, h, x + w / 2, N], fill=(20, 8, 34))
        for yy in range(h + 6, N - 20, 9):
            for xx in range(int(x - w / 2) + 4, int(x + w / 2) - 3, 6):
                if rnd.random() < 0.55:
                    d.rectangle([xx, yy, xx + 2, yy + 3], fill=rnd.choice([(255, 220, 120), (255, 160, 220), (140, 230, 255)]))
    # torre del diamante al centro con antena
    d.polygon([(124, 70), (110, 82), (124, 50), (138, 82)], fill=(150, 230, 255))
    d.line([(124, 50), (124, 30)], fill=(200, 200, 200), width=2)
    d.rectangle([0, 226, N, N], fill=(10, 4, 20))
    text(d, (128, 236), "VEGAS", 20, (255, 120, 200), FONT_SANS)
    deco_border(d, (255, 140, 210))
    return im


def p_cocktail():
    im = radial((120, 20, 60), (30, 4, 18))
    d = ImageDraw.Draw(im)
    sunburst(d, 128, 120, 20, (100, 14, 50))
    # copa de martini
    d.polygon([(60, 70), (196, 70), (128, 148)], fill=(220, 240, 250))
    d.polygon([(72, 80), (184, 80), (128, 140)], fill=(120, 220, 230))
    d.line([(60, 70), (196, 70)], fill=(255, 255, 255), width=3)
    d.rectangle([125, 148, 131, 206], fill=(220, 240, 250))
    d.ellipse([92, 200, 164, 214], fill=(220, 240, 250))
    # aceituna en palillo
    d.line([(100, 40), (150, 112)], fill=GOLD, width=3)
    d.ellipse([132, 92, 152, 112], fill=(110, 150, 40))
    d.ellipse([139, 98, 145, 104], fill=(200, 60, 50))
    for (x, y, s) in ((70, 120, 6), (190, 140, 8), (180, 50, 6)):
        sparkle(d, x, y, s)
    text(d, (128, 232), "BAR", 22, GOLD_L)
    deco_border(d)
    return im


DRAW = [p_diamond, p_derby, p_roulette, p_royal_flush, p_dice, p_chips, p_vegas, p_cocktail]


def textures():
    os.makedirs(OUT, exist_ok=True)
    for name, f in zip(NAMES, DRAW):
        im = f().convert("RGB")
        small = im.resize((64, 64), Image.LANCZOS)
        small = small.filter(ImageFilter.UnsharpMask(radius=1, percent=60, threshold=2))
        small.save(os.path.join(OUT, f"painting_{name}.png"))


def models():
    # marco de 2x2 bloques centrado en el bloque (de -8 a 24 de ancho, de 0 a 32 de alto); el cuadro mira al norte, pegado a la pared sur
    e = [box([-8, 0, 15.2], [24, 32, 16], "lac", faces=["north", "south", "up", "down", "west", "east"]),
         box([-6, 2, 14.9], [22, 30, 15.2], "#art", faces=["north"], uv={"north": [0, 0, 16, 16]}),
         box([-8, 30, 13.9], [24, 32, 15.2], "gold"), box([-8, 0, 13.9], [24, 2, 15.2], "gold"),
         box([-8, 2, 13.9], [-6, 30, 15.2], "gold"), box([22, 2, 13.9], [24, 30, 15.2], "gold"),
         box([-6, 29.4, 14.4], [22, 30, 14.9], "gold"), box([-6, 2, 14.4], [22, 2.6, 14.9], "gold"),
         box([-6, 2.6, 14.4], [-5.4, 29.4, 14.9], "gold"), box([21.4, 2.6, 14.4], [22, 29.4, 14.9], "gold")]
    for (x, y) in ((-8, 0), (22, 0), (-8, 30), (22, 30)):
        e.append(box([x - 0.4, y - 0.4 if y == 0 else y, 13.5], [x + 2.4, y + 2 if y == 0 else y + 2.4, 14.0], "gold"))
    dm.model("painting_frame", e, particle="casino:block/decor/gold")
    variants = {}
    for i, n in enumerate(NAMES):
        dm.child(f"painting_{n}", "painting_frame", {"art": f"casino:block/decor/painting_{n}"})
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            v = {"model": f"casino:block/decor/painting_{n}"}
            if y:
                v["y"] = y
            variants[f"design={i},facing={f}"] = v
    write(os.path.join(A, "blockstates", "casino_painting.json"), {"variants": variants})
    disp = {"gui": {"rotation": [0, 180, 0], "translation": [-2, -2, 0], "scale": [0.42, 0.42, 0.42]},
            "ground": {"translation": [0, 3, 0], "scale": [0.2, 0.2, 0.2]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.45, 0.45, 0.45]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.25, 0.25, 0.25]},
            "firstperson_righthand": {"rotation": [0, 225, 0], "scale": [0.3, 0.3, 0.3]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.3, 0.3, 0.3]}}
    dm.item("casino_painting", "painting_diamond", disp)
    dm.loot("casino_painting")
    dm.recipe("casino_painting", ["GGG", "GPG", "GGG"], {"G": "minecraft:gold_nugget", "P": "minecraft:painting"}, 1)
    names = {"en_us": "Casino Painting", "de_de": "Casino-Gemälde", "fr_fr": "Tableau de casino", "it_it": "Quadro del casinò",
             "pt_br": "Quadro de cassino", "ru_ru": "Картина казино", "ja_jp": "カジノの絵画", "zh_cn": "赌场画作"}
    ldir = os.path.join(A, "lang")
    for fn in sorted(os.listdir(ldir)):
        p = os.path.join(ldir, fn)
        with open(p, encoding="utf-8") as fh:
            lang = json.load(fh)
        code = fn[:-5]
        lang["block.casino.casino_painting"] = "Cuadro del casino" if code.startswith("es") else names.get(code, names["en_us"])
        with open(p, "w", encoding="utf-8") as fh:
            json.dump(lang, fh, ensure_ascii=False, indent=2)
            fh.write("\n")


if __name__ == "__main__":
    textures()
    models()
    import zfix
    zfix.run(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/assets/casino/models"))
