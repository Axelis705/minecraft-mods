#!/usr/bin/env python3
"""Pantalla de récords (casino:record_screen): texturas, modelo, estados, objeto, botín y receta.

    python3 casino/herramientas/record_screen.py

- textures/entity/record_screen.png: el atlas que usa el dibujante (blanco para rellenos y ruido de "estática").
- textures/block/decor/record_screen_front.png: el frente apagado (y el icono del objeto): una mini tabla de récords.
"""
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import decor_models as dm  # noqa: E402
from decor_models import box, model, A  # noqa: E402


def textures():
    rnd = random.Random(11)
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 255))
    for y in range(8):
        for x in range(8):
            im.putpixel((x, y), (255, 255, 255, 255))
    # estática: puntos grises con líneas horizontales más claras/oscuras
    for y in range(16, 64):
        band = rnd.randint(-30, 30) if rnd.random() < 0.3 else 0
        for x in range(64):
            v = max(0, min(255, rnd.randint(20, 235) + band))
            im.putpixel((x, y), (v, v, v, 255))
    im.save(os.path.join(A, "textures", "entity", "record_screen.png"))

    f = Image.new("RGBA", (16, 16), (8, 8, 12, 255))
    gold = (201, 162, 39, 255)
    for i in range(16):
        f.putpixel((i, 0), gold)
        f.putpixel((i, 15), gold)
        f.putpixel((0, i), gold)
        f.putpixel((15, i), gold)
    for y in range(1, 15):
        for x in range(1, 15):
            t = (y - 1) / 13
            f.putpixel((x, y), (int(20 - 12 * t), int(16 - 8 * t), int(56 - 30 * t), 255))
    # encabezado dorado
    for x in range(2, 14):
        for y in (2, 3):
            f.putpixel((x, y), (242, 194, 48, 255))
    # filas: medalla + barra
    medals = [(246, 203, 58), (201, 209, 219), (208, 138, 78), (60, 66, 110)]
    bars = [11, 9, 7, 5]
    for r in range(4):
        y = 5 + r * 2 + (r > 0) * 0
        y = 5 + r * 2
        f.putpixel((2, y), medals[r] + (255,))
        for x in range(4, 4 + bars[r]):
            f.putpixel((x, y), (63, 210, 122, 255) if r else (255, 216, 74, 255))
    # cinta roja abajo
    for x in range(1, 15):
        f.putpixel((x, 13), (90, 15, 18, 255))
        f.putpixel((x, 14), (90, 15, 18, 255))
    f.putpixel((2, 13), (216, 30, 42, 255))
    f.putpixel((3, 13), (216, 30, 42, 255))
    f.save(os.path.join(A, "textures", "block", "decor", "record_screen_front.png"))


def gen():
    if "--texturas" in sys.argv or not os.path.exists(os.path.join(A, "textures", "entity", "record_screen.png")):
        textures()
    tx = {"front": "casino:block/decor/record_screen_front", "side": "minecraft:block/black_concrete"}
    e = [box([0, 0, 14], [16, 16, 16], "#side", faces=["south", "west", "east", "up", "down"]),
         box([0, 0, 14], [16, 16, 16], "#front", faces=["north"], uv="full")]
    model("record_screen", e, textures=tx, particle="minecraft:block/black_concrete")
    dm.facing_states("record_screen", "record_screen")
    dm.item("record_screen", "record_screen", {
        "gui": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
        "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 180, 0], "scale": [0.6, 0.6, 0.6]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
        "firstperson_righthand": {"rotation": [0, 135, 0], "scale": [0.4, 0.4, 0.4]},
        "firstperson_lefthand": {"rotation": [0, 135, 0], "scale": [0.4, 0.4, 0.4]}})
    dm.loot("record_screen")
    dm.recipe("record_screen", ["BBB", "BGB", "BRB"], {"B": "minecraft:black_concrete", "G": "minecraft:glowstone_dust",
                                                        "R": "minecraft:redstone"}, count=2)


if __name__ == "__main__":
    gen()
