#!/usr/bin/env python3
"""Decoración de la terraza: camastro (casino:sun_lounger, se puede sentar) y sombrilla (casino:parasol).

    python3 casino/herramientas/terraza_decor.py

Hace las texturas (lona blanca con ribete azul marino, lona a rayas y madera de teca), los modelos, estados, objetos, botines,
recetas y nombres en todos los idiomas.
"""
import json
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import decor_models as dm  # noqa: E402
from decor_models import box, octagon, model, A  # noqa: E402

TEXD = os.path.join(A, "textures", "block", "decor")


def textures():
    rnd = random.Random(7)
    # lona blanca con un tejido suave y ribete azul marino en la orilla
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = 236 + rnd.randint(-6, 6) - (4 if (x + y) % 2 else 0)
            im.putpixel((x, y), (v, v, min(255, v + 4), 255))
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            im.putpixel((x, y), (30, 52, 96, 255))
    im.save(os.path.join(TEXD, "canvas_white.png"))
    # lona a rayas: azul marino y blanco (rayas de 4 px)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            n = rnd.randint(-5, 5)
            c = (34 + n, 58 + n, 108 + n) if (x // 4) % 2 == 0 else (240 + n // 2, 240 + n // 2, 236 + n // 2)
            im.putpixel((x, y), c + (255,))
    im.save(os.path.join(TEXD, "canvas_stripes.png"))
    # teca: tablas claras con veta
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            plank = y // 4
            base = (176, 120, 70) if plank % 2 == 0 else (166, 112, 64)
            n = rnd.randint(-8, 8) + (10 if (x * 3 + plank * 5) % 11 == 0 else 0)
            c = tuple(max(0, min(255, b + n)) for b in base)
            if y % 4 == 3:
                c = (122, 80, 44)
            im.putpixel((x, y), c + (255,))
    im.save(os.path.join(TEXD, "teak.png"))


def lounger():
    """Camastro mirando al norte: el respaldo inclinado va atrás (sur) y las piernas hacia el frente (sale un poco del bloque)."""
    e = []
    # patas cromadas
    for (x0, z0) in ((1.6, -3.6), (13.4, -3.6), (1.6, 13.4), (13.4, 13.4)):
        e.append(box([x0, 0, z0], [x0 + 1, 4.2, z0 + 1], "chrome"))
    # marco de teca y tablillas
    e += [box([1.2, 4.2, -4], [2.8, 5.8, 15], "#teak"), box([13.2, 4.2, -4], [14.8, 5.8, 15], "#teak"),
          box([2.8, 4.4, -4], [13.2, 5.4, 15], "#teak"), box([1.2, 4.2, -4.8], [14.8, 5.8, -4], "#teak")]
    # colchoneta del asiento y de las piernas
    e += [box([3, 5.4, -3.6], [13, 7.6, 9.2], "#canvas")]
    e += [box([3.3, 7.6, -3.3], [12.7, 7.9, 8.9], "#canvas", faces=["up"])]
    # respaldo inclinado (gira hacia atrás) con su colchoneta y una almohadita
    piv = [8, 6, 10]
    e += [box([2.8, 5.4, 9.6], [13.2, 18.6, 11], "#teak", rot=("x", -22.5, piv)),
          box([3, 5.6, 8.0], [13, 18.2, 9.6], "#canvas", rot=("x", -22.5, piv)),
          box([4.5, 15.4, 6.6], [11.5, 18.0, 8.0], "#canvas", rot=("x", -22.5, piv))]
    # brazos
    e += [box([1.2, 5.8, 3], [2.8, 10.2, 10], "#teak"), box([13.2, 5.8, 3], [14.8, 10.2, 10], "#teak"),
          box([1.4, 10.2, 2.6], [2.6, 10.8, 10.2], "chrome"), box([13.4, 10.2, 2.6], [14.6, 10.8, 10.2], "chrome")]
    model("sun_lounger", e, textures={"canvas": "casino:block/decor/canvas_white", "teak": "casino:block/decor/teak"},
          particle="casino:block/decor/canvas_white")


def parasol():
    """Sombrilla de alberca: base pesada, mástil y lona a rayas de casi 3 bloques de ancho (el mástil está en el centro del bloque)."""
    e = []
    e += octagon([3.5, 0, 3.5], [12.5, 1.4, 12.5], "chrome")
    e += octagon([5.5, 1.4, 5.5], [10.5, 2.4, 10.5], "lac")
    e += [box([7.3, 2.4, 7.3], [8.7, 29.4, 8.7], "#teak"),
          box([6.8, 12.0, 6.8], [9.2, 13.2, 9.2], "chrome"),
          box([6.9, 23.2, 6.9], [9.1, 24.4, 9.1], "chrome")]
    # varillas
    for (a, b) in (((-12, 7.6), (28, 8.4)), ((7.6, -12), (8.4, 28))):
        e.append(box([a[0], 24.6, a[1]], [b[0], 25.2, b[1]], "chrome"))
    # lona escalonada (de ancha a angosta) y remate dorado
    tiers = [(-14.5, 25.2, 26.4), (-11.0, 26.4, 27.6), (-7.0, 27.6, 28.8), (-2.5, 28.8, 29.9), (3.0, 29.9, 30.8)]
    for (lo, y0, y1) in tiers:
        hi = 16 - lo
        e.append(box([lo, y0, lo], [hi, y1, hi], "#stripes"))
    # faldón colgando en la orilla
    lo, hi = -14.5, 30.5
    e += [box([lo, 23.8, lo], [hi, 25.2, lo + 0.4], "#stripes", faces=["north", "south"]),
          box([lo, 23.8, hi - 0.4], [hi, 25.2, hi], "#stripes", faces=["north", "south"]),
          box([lo, 23.8, lo], [lo + 0.4, 25.2, hi], "#stripes", faces=["west", "east"]),
          box([hi - 0.4, 23.8, lo], [hi, 25.2, hi], "#stripes", faces=["west", "east"])]
    e += [box([7.2, 30.8, 7.2], [8.8, 31.6, 8.8], "gold")]
    model("parasol", e, textures={"stripes": "casino:block/decor/canvas_stripes", "teak": "casino:block/decor/teak"},
          particle="casino:block/decor/canvas_stripes")


NAMES = {
    "sun_lounger": {"es": "Camastro", "en_us": "Sun Lounger", "de_de": "Sonnenliege", "fr_fr": "Transat", "it_it": "Lettino",
                    "pt_br": "Espreguiçadeira", "ru_ru": "Шезлонг", "ja_jp": "サンラウンジャー", "zh_cn": "躺椅"},
    "parasol": {"es": "Sombrilla", "en_us": "Parasol", "de_de": "Sonnenschirm", "fr_fr": "Parasol", "it_it": "Ombrellone",
                "pt_br": "Guarda-sol", "ru_ru": "Пляжный зонт", "ja_jp": "パラソル", "zh_cn": "遮阳伞"},
}

DISPLAY_PARASOL = {"gui": {"rotation": [30, 225, 0], "translation": [0, -2.5, 0], "scale": [0.3, 0.3, 0.3]},
                   "ground": {"translation": [0, 3, 0], "scale": [0.2, 0.2, 0.2]},
                   "fixed": {"scale": [0.3, 0.3, 0.3]},
                   "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.25, 0.25, 0.25]},
                   "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.25, 0.25, 0.25]},
                   "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.25, 0.25, 0.25]}}


def gen():
    dm.TEX.setdefault("lac", "casino:block/decor/lacquer")
    textures()
    lounger()
    parasol()
    dm.facing_states("sun_lounger", "sun_lounger")
    dm.facing_states("parasol", "parasol")
    dm.item("sun_lounger", "sun_lounger", dm.DISPLAY)
    dm.item("parasol", "parasol", DISPLAY_PARASOL)
    dm.loot("sun_lounger")
    dm.loot("parasol")
    dm.recipe("sun_lounger", ["  W", "SSS", "I I"], {"W": "minecraft:white_wool", "S": "minecraft:stripped_jungle_log", "I": "minecraft:iron_ingot"})
    dm.recipe("parasol", ["WBW", " S ", " I "], {"W": "minecraft:white_wool", "B": "minecraft:blue_wool", "S": "minecraft:stick", "I": "minecraft:iron_ingot"})
    ldir = os.path.join(A, "lang")
    for fn in sorted(os.listdir(ldir)):
        p = os.path.join(ldir, fn)
        with open(p, encoding="utf-8") as fh:
            lang = json.load(fh)
        code = fn[:-5]
        for k, names in NAMES.items():
            lang["block.casino." + k] = names["es"] if code.startswith("es") else names.get(code, names["en_us"])
        with open(p, "w", encoding="utf-8") as fh:
            json.dump(lang, fh, ensure_ascii=False, indent=2)
            fh.write("\n")


if __name__ == "__main__":
    gen()
    import zfix
    zfix.run(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/assets/casino/models"))
