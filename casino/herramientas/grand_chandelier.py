#!/usr/bin/env python3
"""Candelabro grande de techo (casino:grand_chandelier): mide casi 3 bloques de ancho y cuelga casi 2 bloques.

El bloque va pegado al techo, al centro del casetón. Si el casetón tiene ancho par, el centro cae entre dos bloques: el estado
"shift" corre el dibujo medio bloque (1 = hacia "facing", 2 = hacia "facing" y su derecha) y en ese caso mide 2 bloques de ancho
(Minecraft no deja que un modelo salga más de 1 bloque de su cubo).

    python3 casino/herramientas/grand_chandelier.py
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import decor_models as dm  # noqa: E402
from decor_models import box, write, A, D  # noqa: E402
from neon_models import base  # noqa: E402

DIRS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def oct_(cx, cz, r, y0, y1, tex):
    return dm.octagon([cx - r, y0, cz - r], [cx + r, y1, cz + r], tex)


def ring(cx, cz, r, y0, y1, th, tex):
    """Aro octagonal (r = distancia del centro al medio de cada lado)."""
    s = 2 * r * math.tan(math.pi / 8) + th * 0.4
    out = []
    for k in range(8):
        a = k * math.pi / 4
        mx, mz = cx + r * math.sin(a), cz - r * math.cos(a)
        if k % 2 == 0:      # lados rectos: norte/este/sur/oeste
            if k in (0, 4):
                out.append(box([mx - s / 2, y0, mz - th / 2], [mx + s / 2, y1, mz + th / 2], tex))
            else:
                out.append(box([mx - th / 2, y0, mz - s / 2], [mx + th / 2, y1, mz + s / 2], tex))
        else:               # diagonales: caja girada 45°
            ang = -45 if k in (1, 5) else 45
            out.append(box([mx - s / 2, y0, mz - th / 2], [mx + s / 2, y1, mz + th / 2], tex, rot=("y", ang, [mx, (y0 + y1) / 2, mz])))
    return out


def chandelier(cx, cz, R):
    e = []
    # florón del techo y vástago con dos nudos
    e += oct_(cx, cz, 0.3 * R, 15.2, 16, "gold")
    e += oct_(cx, cz, 0.17 * R, 14.2, 15.2, "gold")
    e += [box([cx - 0.45, 3.6, cz - 0.45], [cx + 0.45, 14.2, cz + 0.45], "gold")]
    e += oct_(cx, cz, 1.3, 10.2, 11.2, "gold")
    e += oct_(cx, cz, 1.0, 7.0, 7.8, "gold")
    # corona de arriba con 8 velas (entre las de abajo)
    r1 = 0.52 * R
    e += ring(cx, cz, r1, 3.6, 4.3, 0.7, "gold")
    for ang in (0, 45):                       # radios de la corona
        e.append(box([cx - r1, 3.75, cz - 0.3], [cx + r1, 4.15, cz + 0.3], "gold", rot=("y", ang, [cx, 3.95, cz])))
        e.append(box([cx - 0.3, 3.75, cz - r1], [cx + 0.3, 4.15, cz + r1], "gold", rot=("y", ang, [cx, 3.95, cz])))
    for k in range(8):
        a = (k + 0.5) * math.pi / 4
        x, z = cx + r1 * math.sin(a), cz - r1 * math.cos(a)
        e += oct_(x, z, 0.6, 4.3, 4.9, "gold")
        e.append(box([x - 0.35, 4.9, z - 0.35], [x + 0.35, 6.3, z + 0.35], "bulb"))
        e.append(box([x - 0.2, 0.2, z - 0.2], [x + 0.2, 3.6, z + 0.2], "crystal"))
        e.append(box([x - 0.4, -0.5, z - 0.4], [x + 0.4, 0.2, z + 0.4], "crystal"))
    # brazos y aro grande de abajo con 8 velas
    r2 = 0.9 * R
    for ang in (0, 45):
        e.append(box([cx - r2, -0.9, cz - 0.35], [cx + r2, -0.3, cz + 0.35], "gold", rot=("y", ang, [cx, -0.6, cz])))
        e.append(box([cx - 0.35, -0.9, cz - r2], [cx + 0.35, -0.3, cz + r2], "gold", rot=("y", ang, [cx, -0.6, cz])))
    e += ring(cx, cz, r2, -1.1, -0.2, 0.8, "gold")
    for k in range(8):
        a = k * math.pi / 4
        x, z = cx + r2 * math.sin(a), cz - r2 * math.cos(a)
        e += oct_(x, z, 0.75, -0.2, 0.6, "gold")
        e.append(box([x - 0.42, 0.6, z - 0.42], [x + 0.42, 2.4, z + 0.42], "bulb"))
    # cortina de cristales colgando del aro grande (16, largos alternados) con gota al final
    for k in range(16):
        a = (k + 0.5) * math.pi / 8
        x, z = cx + (r2 - 0.2) * math.sin(a), cz - (r2 - 0.2) * math.cos(a)
        low = -6.5 if k % 2 else -8.5
        e.append(box([x - 0.35, low, z - 0.35], [x + 0.35, -1.1, z + 0.35], "crystal"))
        e.append(box([x - 0.6, low - 1.1, z - 0.6], [x + 0.6, low, z + 0.6], "crystal"))
    # segunda cortina por dentro, más corta (y velas pequeñas entre las grandes)
    r3 = 0.68 * R
    e += ring(cx, cz, r3, -1.0, -0.4, 0.5, "gold")
    for k in range(16):
        a = k * math.pi / 8
        x, z = cx + r3 * math.sin(a), cz - r3 * math.cos(a)
        low = -4.5 if k % 2 else -5.5
        e.append(box([x - 0.3, low, z - 0.3], [x + 0.3, -1.0, z + 0.3], "crystal"))
        e.append(box([x - 0.5, low - 0.9, z - 0.5], [x + 0.5, low, z + 0.5], "crystal"))
    for k in range(8):
        a = (k + 0.5) * math.pi / 4
        x, z = cx + r2 * math.sin(a), cz - r2 * math.cos(a)
        e += oct_(x, z, 0.55, -0.2, 0.4, "gold")
        e.append(box([x - 0.3, 0.4, z - 0.3], [x + 0.3, 1.6, z + 0.3], "bulb"))
    # cuerpo central de cristal y remate
    e += oct_(cx, cz, 0.3 * R, -1.6, 3.6, "crystal")
    e += oct_(cx, cz, 0.2 * R, -4.6, -1.6, "crystal")
    e += oct_(cx, cz, 0.07 * R + 0.3, -6.2, -4.6, "gold")
    e += oct_(cx, cz, 0.5, -8.6, -6.2, "crystal")
    e += oct_(cx, cz, 0.3 * R + 0.25, 3.6, 4.0, "gold")
    e += oct_(cx, cz, 0.3 * R + 0.25, -2.0, -1.6, "gold")
    return e


def gen():
    dm.TEX.update({"crystal": "casino:block/decor/crystal", "bulb": "casino:block/decor/bulb"})
    base("grand_chandelier", chandelier(8, 8, 22), {}, "casino:block/decor/gold")
    base("grand_chandelier_edge", chandelier(8, 0, 15.5), {}, "casino:block/decor/gold")
    base("grand_chandelier_corner", chandelier(16, 0, 15.5), {}, "casino:block/decor/gold")
    v = {}
    for f, y in DIRS:
        for sh, m in ((0, "grand_chandelier"), (1, "grand_chandelier_edge"), (2, "grand_chandelier_corner")):
            v[f"facing={f},shift={sh}"] = {"model": "casino:block/decor/" + m, "y": y} if y else {"model": "casino:block/decor/" + m}
    write(os.path.join(A, "blockstates", "grand_chandelier.json"), {"variants": v})
    disp = {"gui": {"rotation": [30, 225, 0], "translation": [0, 1.5, 0], "scale": [0.3, 0.3, 0.3]},
            "ground": {"translation": [0, 3, 0], "scale": [0.15, 0.15, 0.15]},
            "fixed": {"scale": [0.3, 0.3, 0.3]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.22, 0.22, 0.22]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.22, 0.22, 0.22]}}
    dm.item("grand_chandelier", "grand_chandelier", disp)
    dm.loot("grand_chandelier")
    dm.recipe("grand_chandelier", ["CCC", "CGC", "CCC"], {"C": "casino:chandelier", "G": "minecraft:gold_block"})
    names = {"en_us": "Grand Chandelier", "de_de": "Großer Kronleuchter", "fr_fr": "Grand lustre", "it_it": "Grande lampadario",
             "pt_br": "Grande lustre", "ru_ru": "Большая люстра", "ja_jp": "大シャンデリア", "zh_cn": "大型水晶吊灯"}
    ldir = os.path.join(A, "lang")
    for fn in sorted(os.listdir(ldir)):
        p = os.path.join(ldir, fn)
        with open(p, encoding="utf-8") as fh:
            lang = json.load(fh)
        code = fn[:-5]
        lang["block.casino.grand_chandelier"] = names.get(code, "Candelabro grande" if code.startswith("es") else "Grand Chandelier")
        with open(p, "w", encoding="utf-8") as fh:
            json.dump(lang, fh, ensure_ascii=False, indent=2)
            fh.write("\n")


if __name__ == "__main__":
    gen()
    import zfix
    zfix.run(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/assets/casino/models"))
