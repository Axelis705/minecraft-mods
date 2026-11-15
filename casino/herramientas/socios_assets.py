#!/usr/bin/env python3
"""Premios de socio (ronda 8): trofeos de cada nivel (casino:trophy_<nivel>) y fichas de colección (casino:member_chip_<nivel>).

    python3 casino/herramientas/socios_assets.py

Hace las texturas de metal de cada nivel (a partir del oro, por brillo), el modelo del trofeo (base de mármol negro con placa dorada,
pie, copa con asas; el de Diamante lleva un cristal arriba), estados, objetos, botines y las texturas de las fichas (la de 500 teñida
del color de cada nivel, con un rombo al centro). Los nombres van en textos_ronda5.py.
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import decor_models as dm  # noqa: E402
from decor_models import box, octagon, A  # noqa: E402

LEVELS = ["bronze", "silver", "gold", "platinum", "diamond"]
# (oscuro, claro) de cada metal
RAMP = {"bronze": ((0x5E, 0x30, 0x12), (0xF6, 0xB0, 0x70)), "silver": ((0x62, 0x6A, 0x76), (0xF6, 0xF9, 0xFC)),
        "gold": None, "platinum": ((0x52, 0x7A, 0x84), (0xEC, 0xFC, 0xFF)), "diamond": ((0x12, 0x7C, 0x8A), (0xC4, 0xFF, 0xFF))}
CHIP = {"bronze": ((0x3C, 0x1C, 0x08), (0xE8, 0x9A, 0x58)), "silver": ((0x3A, 0x40, 0x4C), (0xE4, 0xEA, 0xF2)),
        "gold": ((0x5A, 0x3A, 0x06), (0xFF, 0xD8, 0x50)), "platinum": ((0x22, 0x48, 0x52), (0xC8, 0xF4, 0xFF)),
        "diamond": ((0x06, 0x48, 0x58), (0x8C, 0xFF, 0xFF))}
TEXD = os.path.join(A, "textures", "block", "decor")


def lum(c):
    return (0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]) / 255


def ramp(lo, hi, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(lo[i] + (hi[i] - lo[i]) * t) for i in range(3))


def metals():
    gold = Image.open(os.path.join(TEXD, "gold.png")).convert("RGBA")
    ls = [lum(gold.getpixel((x, y))) for y in range(gold.height) for x in range(gold.width)]
    lo_l, hi_l = min(ls), max(ls)
    for lv in LEVELS:
        if RAMP[lv] is None:
            gold.save(os.path.join(TEXD, "metal_gold.png"))
            continue
        lo, hi = RAMP[lv]
        im = Image.new("RGBA", gold.size)
        for y in range(gold.height):
            for x in range(gold.width):
                t = (lum(gold.getpixel((x, y))) - lo_l) / (hi_l - lo_l)
                im.putpixel((x, y), ramp(lo, hi, 0.15 + 0.85 * t) + (255,))
        im.save(os.path.join(TEXD, "metal_" + lv + ".png"))


def chips():
    src = Image.open(os.path.join(A, "textures", "item", "chip_500.png")).convert("RGBA")
    for lv in LEVELS:
        lo, hi = CHIP[lv]
        im = Image.new("RGBA", src.size)
        for y in range(16):
            for x in range(16):
                r, g, b, a = src.getpixel((x, y))
                if a == 0:
                    continue
                sat = max(r, g, b) - min(r, g, b)
                if (r, g, b) == (0xFA, 0xEB, 0x96):
                    c = tuple((v + 255) // 2 for v in hi)
                elif sat > 30:
                    c = ramp(lo, hi, (lum((r, g, b)) - 0.08) / 0.5)
                else:
                    c = (r, g, b)
                im.putpixel((x, y), c + (255,))
        # rombo al centro (la marca de la ficha de colección)
        for (x, y) in ((7, 5), (8, 5), (6, 6), (9, 6), (5, 7), (10, 7), (5, 8), (10, 8), (6, 9), (9, 9), (7, 10), (8, 10)):
            im.putpixel((x, y), ramp(lo, hi, 0.55) + (255,))
        for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
            im.putpixel((x, y), ramp(lo, hi, 1.0) + (255,))
        im.putpixel((7, 7), (255, 255, 255, 255))
        im.save(os.path.join(A, "textures", "item", "member_chip_" + lv + ".png"))
        dm.write(os.path.join(A, "models", "item", "member_chip_" + lv + ".json"),
                 {"parent": "item/generated", "textures": {"layer0": "casino:item/member_chip_" + lv}})


def trophy(crystal=False):
    """Trofeo mirando al norte (la placa al frente). Cabe en las formas de colisión de ModDecor.TROPHIES."""
    e = []
    e.append(box([4, 0, 4], [12, 2.5, 12], "marb"))
    e.append(box([4.5, 2.5, 4.5], [11.5, 3.5, 11.5], "#metal"))
    e.append(box([5.5, 0.6, 3.8], [10.5, 2.0, 4.0], "gold", faces=["north"], uv="full"))
    e.append(box([7.1, 3.5, 7.1], [8.9, 7, 8.9], "#metal"))
    e += octagon([6.2, 4.8, 6.2], [9.8, 5.8, 9.8], "#metal")
    e += octagon([5.6, 7, 5.6], [10.4, 8.2, 10.4], "#metal")
    e += octagon([4.2, 8.2, 4.2], [11.8, 12, 11.8], "#metal", faces=["north", "south", "west", "east", "down"])
    # borde de arriba y el interior oscuro
    e += [box([3.8, 11.6, 3.8], [12.2, 12.4, 4.6], "#metal"), box([3.8, 11.6, 11.4], [12.2, 12.4, 12.2], "#metal"),
          box([3.8, 11.6, 4.6], [4.6, 12.4, 11.4], "#metal"), box([11.4, 11.6, 4.6], [12.2, 12.4, 11.4], "#metal"),
          box([4.6, 10.6, 4.6], [11.4, 10.7, 11.4], "marb", faces=["up"])]
    # asas
    e += [box([2.6, 8.6, 7.4], [4.2, 9.4, 8.6], "#metal"), box([2.6, 9.4, 7.4], [3.4, 11.2, 8.6], "#metal"),
          box([2.6, 11.2, 7.4], [4.2, 12.0, 8.6], "#metal"),
          box([11.8, 8.6, 7.4], [13.4, 9.4, 8.6], "#metal"), box([12.6, 9.4, 7.4], [13.4, 11.2, 8.6], "#metal"),
          box([11.8, 11.2, 7.4], [13.4, 12.0, 8.6], "#metal")]
    if crystal:
        e.append(box([7.0, 11.0, 7.0], [9.0, 14.6, 9.0], "#crystal", rot=("y", 45, [8, 12, 8]), uv="full"))
    return e


def gen():
    dm.TEX.setdefault("marb", "casino:block/decor/marble_black")
    metals()
    chips()
    for lv in LEVELS:
        name = "trophy_" + lv
        tx = {"metal": "casino:block/decor/metal_" + lv}
        if lv == "diamond":
            tx["crystal"] = "casino:block/decor/crystal"
        dm.model(name, trophy(lv == "diamond"), textures=tx, particle="casino:block/decor/metal_" + lv)
        dm.facing_states(name, name)
        dm.item(name, name, dm.DISPLAY)
        dm.loot(name)


if __name__ == "__main__":
    gen()
    import zfix
    zfix.run(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/assets/casino/models"))
