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
    """Camastro mirando al norte: piernas hacia el frente (sale 6 px del bloque) y respaldo inclinado atrás. Quien se sienta queda en
    el centro del bloque, así que el respaldo arranca detrás de su espalda (z 10.6) y no lo atraviesa; sin brazos (chocaban con los suyos)."""
    e = []
    # patas cromadas
    for (x0, z0) in ((1.6, -5.4), (13.4, -5.4), (1.6, 13.6), (13.4, 13.6)):
        e.append(box([x0, 0, z0], [x0 + 1, 3.6, z0 + 1], "chrome"))
    # marco de teca y tablillas
    e += [box([1.2, 3.6, -6], [2.8, 5.2, 15.4], "#teak"), box([13.2, 3.6, -6], [14.8, 5.2, 15.4], "#teak"),
          box([2.8, 3.8, -6], [13.2, 4.8, 15.4], "#teak"), box([1.2, 3.6, -6.8], [14.8, 5.2, -6], "#teak")]
    # colchoneta del asiento y de las piernas (un poco más baja hacia los pies)
    e += [box([3, 4.8, -5.6], [13, 6.8, 11.2], "#canvas"),
          box([3.3, 6.8, -5.3], [12.7, 7.1, 10.9], "#canvas", faces=["up"])]
    # respaldo inclinado hacia atrás, detrás de la espalda, con colchoneta y almohada
    piv = [8, 5.2, 12.6]
    e += [box([2.8, 5.2, 12.4], [13.2, 18.0, 13.8], "#teak", rot=("x", 22.5, piv)),
          box([3, 5.6, 10.8], [13, 17.6, 12.4], "#canvas", rot=("x", 22.5, piv)),
          box([4.5, 14.8, 9.4], [11.5, 17.4, 10.8], "#canvas", rot=("x", 22.5, piv))]
    model("sun_lounger", e, textures={"canvas": "casino:block/decor/canvas_white", "teak": "casino:block/decor/teak"},
          particle="casino:block/decor/canvas_white")


def parasol_parts():
    """Mástil y lona (la parte de arriba) y base (abajo). Coordenadas desde el piso: 0..16 abajo, 16..48 arriba."""
    low, up = [], []
    low += octagon([3.5, 0, 3.5], [12.5, 1.4, 12.5], "chrome")
    low += octagon([5.5, 1.4, 5.5], [10.5, 2.4, 10.5], "lac")
    low += [box([7.3, 2.4, 7.3], [8.7, 16, 8.7], "#teak"), box([6.8, 12.0, 6.8], [9.2, 13.2, 9.2], "chrome")]
    up += [box([7.3, 0, 7.3], [8.7, 29.4, 8.7], "#teak"), box([6.9, 7.2, 6.9], [9.1, 8.4, 9.1], "chrome")]
    # varillas
    for (a, b) in (((-12, 7.6), (28, 8.4)), ((7.6, -12), (8.4, 28))):
        up.append(box([a[0], 24.6, a[1]], [b[0], 25.2, b[1]], "chrome"))
    # lona escalonada (de ancha a angosta) y remate dorado
    for (lo, y0, y1) in [(-14.5, 25.2, 26.4), (-11.0, 26.4, 27.6), (-7.0, 27.6, 28.8), (-2.5, 28.8, 29.9), (3.0, 29.9, 30.8)]:
        up.append(box([lo, y0, lo], [16 - lo, y1, 16 - lo], "#stripes"))
    lo, hi = -14.5, 30.5
    up += [box([lo, 23.8, lo], [hi, 25.2, lo + 0.4], "#stripes", faces=["north", "south"]),
           box([lo, 23.8, hi - 0.4], [hi, 25.2, hi], "#stripes", faces=["north", "south"]),
           box([lo, 23.8, lo], [lo + 0.4, 25.2, hi], "#stripes", faces=["west", "east"]),
           box([hi - 0.4, 23.8, lo], [hi, 25.2, hi], "#stripes", faces=["west", "east"])]
    up += [box([7.2, 30.8, 7.2], [8.8, 31.6, 8.8], "gold")]
    return low, up


def parasol():
    """Sombrilla de dos bloques (abajo base y mástil, arriba mástil y lona: la lona queda a 2.5 bloques del piso)."""
    low, up = parasol_parts()
    tx = {"stripes": "casino:block/decor/canvas_stripes", "teak": "casino:block/decor/teak"}
    model("parasol_lower", low, textures=tx, particle="casino:block/decor/canvas_stripes")
    model("parasol_upper", up, textures=tx, particle="casino:block/decor/canvas_stripes")
    # para el objeto: todo junto y más bajo (cabe en el cuadro del inventario)
    item = [dict(b, **{"from": [b["from"][0], b["from"][1] * 0.5, b["from"][2]], "to": [b["to"][0], b["to"][1] * 0.5, b["to"][2]]}) for b in low]
    item += [dict(b, **{"from": [b["from"][0], 8 + b["from"][1] * 0.75, b["from"][2]], "to": [b["to"][0], 8 + b["to"][1] * 0.75, b["to"][2]]}) for b in up]
    model("parasol", item, textures=tx, particle="casino:block/decor/canvas_stripes")


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
    if "--texturas" in sys.argv:
        textures()
    lounger()
    parasol()
    dm.facing_states("sun_lounger", "sun_lounger")
    v = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for half in ("lower", "upper"):
            m = {"model": "casino:block/decor/parasol_" + half}
            if y:
                m["y"] = y
            v[f"facing={f},half={half}"] = m
    dm.write(os.path.join(A, "blockstates", "parasol.json"), {"variants": v})
    dm.item("sun_lounger", "sun_lounger", dm.DISPLAY)
    dm.item("parasol", "parasol", DISPLAY_PARASOL)
    dm.loot("sun_lounger")
    # (la sombrilla suelta su objeto desde el código, una sola vez aunque tenga dos mitades)
    p = os.path.join(dm.D, "loot_tables", "blocks", "parasol.json")
    if os.path.exists(p):
        os.remove(p)
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
