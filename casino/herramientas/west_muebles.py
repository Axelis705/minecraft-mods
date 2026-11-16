#!/usr/bin/env python3
"""Muebles del Viejo Oeste para el saloon: silla, taburete, mesa redonda, mesa de póker, banca y candil de rueda de carreta.

    python3 casino/herramientas/west_muebles.py

Todo de madera con texturas de Minecraft (abeto, roble oscuro, troncos sin corteza) para que combine con el saloon. Los nombres
van en west_saloon_lang.py. Escribe modelos, estados, objetos, botines y recetas.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import decor_models as dm  # noqa: E402
from decor_models import box, octagon, rot90, A, D  # noqa: E402

dm.TEX.update({
    "w_spruce": "minecraft:block/spruce_planks", "w_dark": "minecraft:block/dark_oak_planks",
    "w_slog": "minecraft:block/stripped_spruce_log", "w_dlog": "minecraft:block/stripped_dark_oak_log",
    "felt": "minecraft:block/green_wool", "iron": "minecraft:block/anvil", "candle": "minecraft:block/candle",
    "flame": "minecraft:block/shroomlight", "chip_r": "minecraft:block/red_concrete", "chip_b": "minecraft:block/blue_concrete",
    "chip_w": "minecraft:block/white_concrete", "card": "minecraft:block/white_wool",
})

DISPLAY_CHANDELIER = {"gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.3, 0.3, 0.3]},
                      "ground": {"translation": [0, 3, 0], "scale": [0.15, 0.15, 0.15]},
                      "fixed": {"scale": [0.35, 0.35, 0.35]},
                      "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
                      "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.25, 0.25, 0.25]},
                      "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.25, 0.25, 0.25]}}


def model(name, els, particle):
    dm.write(os.path.join(A, "models", "block", "west", name + ".json"), dm.emit(els, {}, dm.TEX[particle]))


def item(name, display):
    dm.write(os.path.join(A, "models", "item", name + ".json"), {"parent": "casino:block/west/" + name, "display": display})


def facing_states(name):
    v = {f"facing={f}": {"model": "casino:block/west/" + name, "y": y} for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}
    dm.write(os.path.join(A, "blockstates", name + ".json"), {"variants": v})


def any_facing_states(name):
    """Bloques sin dirección que igual guardan FACING (DecorBlock): las cuatro variantes con el mismo modelo."""
    v = {f"facing={f}": {"model": "casino:block/west/" + name} for f in ("north", "east", "south", "west")}
    dm.write(os.path.join(A, "blockstates", name + ".json"), {"variants": v})


# ================================================================================================ SILLA
def chair():
    """Silla de madera con respaldo de travesaños (mira al norte: el respaldo queda al sur)."""
    e = []
    for x1 in (2.5, 12):
        e.append(box([x1, 0, 2.5], [x1 + 1.5, 7.5, 4], "w_dark"))            # patas de enfrente
        e.append(box([x1, 0, 12], [x1 + 1.5, 20, 13.5], "w_dark"))           # patas de atrás que suben al respaldo
    e += [box([4, 2.5, 2.9], [12, 3.3, 3.6], "w_dark"), box([4, 2.5, 12.4], [12, 3.3, 13.1], "w_dark"),
          box([2.9, 2.5, 4], [3.6, 3.3, 12], "w_dark"), box([12.4, 2.5, 4], [13.1, 3.3, 12], "w_dark")]
    e += [box([2, 7.5, 2], [14, 9, 14], "w_spruce"),                                         # asiento
          box([1.8, 7.3, 1.8], [14.2, 7.6, 14.2], "w_dark", faces=["north", "south", "west", "east", "down"])]
    e += [box([4, 10.5, 12.4], [12, 11.9, 13.2], "w_spruce"),                               # travesaños del respaldo
          box([4, 13.5, 12.4], [12, 14.9, 13.2], "w_spruce"),
          box([7, 9, 12.55], [9, 17.6, 13.05], "w_spruce"),
          box([2.2, 17.6, 11.8], [13.8, 20.4, 13.7], "w_dark"),                              # copete
          box([3.2, 20.4, 12.1], [12.8, 21, 13.4], "w_dark")]
    model("saloon_chair", e, "w_spruce")


# ================================================================================================ TABURETE
def stool():
    e = []
    for x1 in (3, 11.5):
        for z1 in (3, 11.5):
            e.append(box([x1, 0, z1], [x1 + 1.5, 12.2, z1 + 1.5], "w_dark"))
    e += [box([4.5, 4.5, 3.3], [11.5, 5.3, 4.2], "w_dark"), box([4.5, 4.5, 11.8], [11.5, 5.3, 12.7], "w_dark"),
          box([3.3, 4.5, 4.5], [4.2, 5.3, 11.5], "w_dark"), box([11.8, 4.5, 4.5], [12.7, 5.3, 11.5], "w_dark")]
    e += octagon([2.6, 12.2, 2.6], [13.4, 13.8, 13.4], "w_spruce")
    model("saloon_stool", e, "w_spruce")


# ================================================================================================ MESA REDONDA
def table():
    """Mesa redonda de un pie: cuatro patas en cruz, columna con anillo y cubierta hasta el borde del bloque (y=16)."""
    e = []
    for k in range(4):
        e += rot90([box([7, 0, 1.2], [9, 1.6, 6.5], "w_dark")], k)
    e += [box([6.5, 0, 6.5], [9.5, 13.6, 9.5], "w_dark"),
          box([6, 4.5, 6], [10, 5.5, 10], "w_slog"), box([6, 10.5, 6], [10, 11.5, 10], "w_slog")]
    e += octagon([3.5, 13.6, 3.5], [12.5, 14.3, 12.5], "w_dark")
    e += octagon([0.4, 14.3, 0.4], [15.6, 16, 15.6], "w_spruce")
    model("saloon_table", e, "w_spruce")


# ================================================================================================ MESA DE PÓKER
def poker_table():
    """Mesa cuadrada de póker: patas torneadas, canto de roble oscuro y paño verde; unas fichas y dos cartas encima."""
    e = []
    for x1 in (1.8, 12.2):
        for z1 in (1.8, 12.2):
            e += [box([x1, 0, z1], [x1 + 2, 13, z1 + 2], "w_dark"),
                  box([x1 - 0.4, 3, z1 - 0.4], [x1 + 2.4, 4, z1 + 2.4], "w_slog"),
                  box([x1 - 0.4, 9, z1 - 0.4], [x1 + 2.4, 10, z1 + 2.4], "w_slog")]
    e += [box([1.4, 12, 1.4], [14.6, 14, 14.6], "w_dark", faces=["north", "south", "west", "east", "down"])]
    e += [box([0, 14, 0], [16, 16, 2], "w_dark"), box([0, 14, 14], [16, 16, 16], "w_dark"),
          box([0, 14, 2], [2, 16, 14], "w_dark"), box([14, 14, 2], [16, 16, 14], "w_dark"),
          box([2, 14, 2], [14, 15.4, 14], "felt")]
    for x, z, t, h in ((4.5, 4.5, "chip_r", 1.2), (4.5, 6.6, "chip_b", 0.8), (11, 10.5, "chip_w", 1.0)):
        e.append(box([x, 15.4, z], [x + 1.6, 15.4 + h, z + 1.6], t))
    e += [box([8, 15.4, 7.2], [10, 15.55, 10], "card"), box([9.4, 15.4, 7.6], [11.4, 15.6, 10.4], "card")]
    model("poker_table", e, "w_dark")


# ================================================================================================ BANCA
def bench():
    """Banca con respaldo (mira al norte); en fila se ve como una sola banca larga."""
    e = [box([0, 7, 2.5], [16, 8.5, 12.5], "w_spruce")]
    for x1 in (1.5, 13):
        e += [box([x1, 0, 3], [x1 + 1.5, 7, 4.5], "w_dark"), box([x1, 0, 10.5], [x1 + 1.5, 7, 12], "w_dark"),
              box([x1 + 0.3, 2, 4.5], [x1 + 1.2, 3, 10.5], "w_dark"),
              box([x1, 8.5, 12], [x1 + 1.5, 18, 13.5], "w_dark")]
    e += [box([0, 10.5, 12.5], [16, 12.8, 13.3], "w_spruce"), box([0, 14.2, 12.5], [16, 16.5, 13.3], "w_spruce"),
          box([0, 17.4, 11.9], [16, 18.8, 13.6], "w_dark")]
    model("saloon_bench", e, "w_spruce")


# ================================================================================================ CANDIL DE RUEDA
def chandelier():
    """Rueda de carreta colgada (casi dos bloques de ancho) con ocho velas, para colgar de una cadena. Ilumina como farol."""
    e = []
    y0, y1 = 3, 5
    a = 14.0                                  # apotema del aro (píxeles desde el centro)
    half = a * math.tan(math.pi / 8)          # medio lado del octágono
    c = 8
    # aro: cuatro lados rectos y cuatro en diagonal (cajas giradas 45°)
    e += [box([c - half, y0, c - a - 1], [c + half, y1, c - a + 1], "w_dark"), box([c - half, y0, c + a - 1], [c + half, y1, c + a + 1], "w_dark"),
          box([c - a - 1, y0, c - half], [c - a + 1, y1, c + half], "w_dark"), box([c + a - 1, y0, c - half], [c + a + 1, y1, c + half], "w_dark")]
    cand = [(c, c - a), (c, c + a), (c - a, c), (c + a, c)]
    r = a / math.sqrt(2)
    for dx, dz in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        cx, cz = c + dx * r, c + dz * r
        ang = -45 if dx * dz > 0 else 45
        e.append(box([cx - half, y0, cz - 1], [cx + half, y1, cz + 1], "w_dark", rot=("y", ang, [cx, 4, cz])))
        cand.append((cx, cz))
    # rayos y maza
    e += [box([c - a + 1, 3.4, c - 0.7], [c + a - 1, 4.6, c + 0.7], "w_slog"), box([c - 0.7, 3.4, c - a + 1], [c + 0.7, 4.6, c + a - 1], "w_slog")]
    for ang in (45, -45):
        e.append(box([c - a + 1.5, 3.4, c - 0.7], [c + a - 1.5, 4.6, c + 0.7], "w_slog", rot=("y", ang, [c, 4, c])))
    e += [box([5.5, 2.5, 5.5], [10.5, 5.5, 10.5], "w_dark"), box([5.2, 3.6, 5.2], [10.8, 4.4, 10.8], "iron"),
          box([7.4, 5.5, 7.4], [8.6, 16, 8.6], "iron")]
    side = [0, 8, 2, 14]                      # la vela de la textura de Minecraft (como en su modelo)
    cuv = {"north": side, "south": side, "west": side, "east": side, "up": [0, 6, 2, 8], "down": [0, 6, 2, 8]}
    for cx, cz in cand:
        e += [box([cx - 1.2, y1, cz - 1.2], [cx + 1.2, y1 + 4.5, cz + 1.2], "candle", uv=cuv),
              box([cx - 0.6, y1 + 4.5, cz - 0.6], [cx + 0.6, y1 + 6.2, cz + 0.6], "flame")]
    model("wheel_chandelier", e, "w_dark")


# ================================================================================================ ESCRITORIO
def desk():
    """Escritorio del dueño (mira al norte = el lado de las visitas, cerrado; del otro lado, los cajones y el hueco para las piernas).
    Dos juntos se ven como un escritorio largo."""
    e = [box([0, 14, 0], [16, 16, 2], "w_dark"), box([0, 14, 14], [16, 16, 16], "w_dark"),
         box([0, 14, 2], [2, 16, 14], "w_dark"), box([14, 14, 2], [16, 16, 14], "w_dark"),
         box([2, 14, 2], [14, 15.6, 14], "felt")]                                         # cubierta con cuero verde
    e += [box([0.6, 0.5, 0.6], [15.4, 14, 1.6], "w_spruce"),                              # panel de enfrente
          box([0.4, 0, 0.4], [15.6, 0.6, 1.8], "w_dark"),
          box([0.6, 0, 1.6], [5.2, 14, 15.4], "w_spruce"), box([10.8, 0, 1.6], [15.4, 14, 15.4], "w_spruce")]   # cajoneras
    for x1, x2 in ((1.2, 4.6), (11.4, 14.8)):
        for y1 in (1.5, 5.5, 9.5):
            e += [box([x1, y1, 15.4], [x2, y1 + 3.2, 15.8], "w_dark"),
                  box([(x1 + x2) / 2 - 0.6, y1 + 1.2, 15.8], [(x1 + x2) / 2 + 0.6, y1 + 2, 16.1], "brass")]
    for x1, x2 in ((2, 7), (9, 14)):                                                        # molduras del panel
        e.append(box([x1, 3, 0.3], [x2, 11, 0.6], "w_dark", faces=["north", "west", "east", "up", "down"]))
    model("saloon_desk", e, "w_spruce")


def main():
    chair()
    stool()
    table()
    poker_table()
    bench()
    chandelier()
    desk()
    for name in ("saloon_chair", "saloon_stool", "saloon_bench", "saloon_desk"):
        facing_states(name)
    for name in ("saloon_table", "poker_table", "wheel_chandelier"):
        any_facing_states(name)
    item("saloon_chair", dm.DISPLAY_TALL)
    item("saloon_stool", dm.DISPLAY)
    item("saloon_table", dm.DISPLAY)
    item("poker_table", dm.DISPLAY)
    item("saloon_bench", dm.DISPLAY)
    item("wheel_chandelier", DISPLAY_CHANDELIER)
    item("saloon_desk", dm.DISPLAY)
    for name in ("saloon_chair", "saloon_stool", "saloon_table", "poker_table", "saloon_bench", "wheel_chandelier", "saloon_desk"):
        dm.loot(name)
    dm.recipe("saloon_chair", ["P  ", "PPP", "S S"], {"P": "minecraft:spruce_planks", "S": "minecraft:stick"}, 2)
    dm.recipe("saloon_stool", ["PPP", "S S"], {"P": "minecraft:spruce_slab", "S": "minecraft:stick"}, 2)
    dm.recipe("saloon_table", ["PPP", " L ", " L "], {"P": "minecraft:spruce_slab", "L": "minecraft:dark_oak_log"})
    dm.recipe("poker_table", ["WWW", "PPP", "S S"], {"W": "minecraft:green_wool", "P": "minecraft:dark_oak_planks", "S": "minecraft:stick"})
    dm.recipe("saloon_desk", ["WWW", "PCP", "P P"], {"W": "minecraft:green_wool", "P": "minecraft:spruce_planks", "C": "minecraft:chest"})
    dm.recipe("saloon_bench", ["P  ", "PPP", "S S"], {"P": "minecraft:spruce_slab", "S": "minecraft:stick"}, 3)
    dm.recipe("wheel_chandelier", [" C ", "TWT", " T "], {"C": "minecraft:iron_nugget", "T": "minecraft:candle", "W": "minecraft:dark_oak_planks"})
    print("muebles del Oeste listos")


if __name__ == "__main__":
    main()
