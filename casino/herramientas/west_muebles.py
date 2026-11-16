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
    "bone": "minecraft:block/bone_block_side", "bone_t": "minecraft:block/bone_block_top", "dark": "minecraft:block/black_terracotta",
    "ironb": "minecraft:block/iron_block", "barrel": "minecraft:block/barrel_side", "barrel_t": "minecraft:block/barrel_top",
    "hide_d": "minecraft:block/brown_terracotta",
    "hat": "minecraft:block/brown_concrete", "band": "minecraft:block/black_wool", "rope": "minecraft:block/stripped_birch_log",
    "water": "casino:block/west/water", "w_oak": "minecraft:block/stripped_oak_log",
    "obrass": "casino:block/west/brass_old", "hide": "casino:block/west/leather", "navajo": "casino:block/west/blanket",
    "chimney": "casino:block/west/lamp_glass",
})

DISPLAY_CHANDELIER = {"gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.3, 0.3, 0.3]},
                      "ground": {"translation": [0, 3, 0], "scale": [0.15, 0.15, 0.15]},
                      "fixed": {"scale": [0.35, 0.35, 0.35]},
                      "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
                      "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.25, 0.25, 0.25]},
                      "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.25, 0.25, 0.25]}}


def model(name, els, particle, render=None):
    m = dm.emit(els, {}, dm.TEX[particle])
    if render:
        m["render_type"] = render
    dm.write(os.path.join(A, "models", "block", "west", name + ".json"), m)


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


# ================================================================================================ SEGUNDA TANDA (16-nov-2026)
# Objetos sueltos del Oeste. Los de pared (cráneo, herradura, rueda, armero) miran al norte con la espalda en z=16: se ponen
# apuntando a la pared y quedan pegados a ella.

def octagon_xy(x1, y1, x2, y2, z1, z2, tex, faces=None, uv=None):
    """Como octagon() pero de pie (redondo visto de frente) y largo hacia atrás: barriles acostados, ruedas gruesas."""
    w, h = x2 - x1, y2 - y1
    out = []
    for i, (fx, fy) in enumerate(((1.0, 0.62), (0.62, 1.0), (0.86, 0.86))):
        ix, iy = w * (1 - fx) / 2, h * (1 - fy) / 2
        e = 0.02 * i
        out.append(box([x1 + ix, y1 + iy, z1 + e], [x2 - ix, y2 - iy, z2 - e], tex, faces, uv=uv))
    return out


def spittoon():
    """Escupidera de latón: base, panza, cuello y boca ancha (por dentro, oscura)."""
    b = "obrass"
    e = octagon([4.5, 0, 4.5], [11.5, 0.8, 11.5], b)
    e += octagon([3.4, 0.8, 3.4], [12.6, 4.6, 12.6], b)
    e += octagon([5, 4.6, 5], [11, 5.6, 11], b)
    e += octagon([3, 5.6, 3], [13, 6.8, 13], b, faces=["north", "south", "west", "east", "down"])
    e += [box([3, 6.6, 3], [13, 6.8, 4.2], b, faces=["up"]), box([3, 6.6, 11.8], [13, 6.8, 13], b, faces=["up"]),
          box([3, 6.6, 4.2], [4.2, 6.8, 11.8], b, faces=["up"]), box([11.8, 6.6, 4.2], [13, 6.8, 11.8], b, faces=["up"])]
    e += octagon([4.2, 5.9, 4.2], [11.8, 6.5, 11.8], "dark", faces=["up"])
    model("spittoon", e, b)


def longhorn_skull():
    """Cráneo de res con cuernos largos, para la pared (encima de las puertas)."""
    e = [box([4.6, 9, 12], [11.4, 13, 16], "bone"),                    # frente ancha
         box([5.4, 4, 12.6], [10.6, 9, 15.6], "bone"),                 # cara
         box([6, 1.2, 13], [10, 4, 15.4], "bone"),                     # hocico
         box([5.6, 1.6, 12.8], [10.4, 3.4, 13.2], "bone_t"),
         box([6.1, 8.4, 11.8], [7.5, 9.8, 12.1], "dark"), box([8.5, 8.4, 11.8], [9.9, 9.8, 12.1], "dark"),     # ojos
         box([6.7, 1.9, 12.6], [7.5, 2.7, 12.85], "dark"), box([8.5, 1.9, 12.6], [9.3, 2.7, 12.85], "dark"),   # nariz
         box([7.4, 5, 12.4], [8.6, 8.6, 12.7], "bone_t"),             # hueso de la nariz
         box([5.8, 12.6, 12.4], [10.2, 13.6, 15.6], "bone_t")]        # copete
    # cuernos: salen de los lados, van hacia afuera y suben en la punta
    for side in (-1, 1):
        cx = 8 + side * 3.4
        if side < 0:
            e += [box([-1, 10.6, 13.2], [cx, 12.4, 14.8], "bone_t"),
                  box([-7.4, 11.2, 13.4], [-0.6, 12.6, 14.6], "bone_t", rot=("z", -22.5, [-0.8, 11.5, 14])),
                  box([-10.2, 13.6, 13.6], [-6.4, 14.8, 14.4], "dark", rot=("z", -45, [-6.6, 13.9, 14]))]
        else:
            e += [box([cx, 10.6, 13.2], [17, 12.4, 14.8], "bone_t"),
                  box([16.6, 11.2, 13.4], [23.4, 12.6, 14.6], "bone_t", rot=("z", 22.5, [16.8, 11.5, 14])),
                  box([22.4, 13.6, 13.6], [26.2, 14.8, 14.4], "dark", rot=("z", 45, [22.6, 13.9, 14]))]
    e.append(box([7.4, 13.2, 15.6], [8.6, 14, 16], "iron"))         # clavo
    model("longhorn_skull", e, "bone")


def horseshoe():
    """Herradura de la suerte colgada de un clavo, con la abertura hacia arriba (así no se le sale la suerte)."""
    t = "iron"
    e = [box([6, 3.6, 15], [10, 5, 16], t),
         box([4.9, 4.4, 15], [6.4, 6.4, 16], t), box([9.6, 4.4, 15], [11.1, 6.4, 16], t),
         box([4.4, 6, 15], [5.9, 10.6, 16], t), box([10.1, 6, 15], [11.6, 10.6, 16], t),
         box([4.1, 10.4, 15], [5.7, 11.8, 16], t), box([10.3, 10.4, 15], [11.9, 11.8, 16], t)]
    for x, y in ((6.6, 4), (8.9, 4), (4.8, 7), (10.6, 7), (4.8, 9.2), (10.6, 9.2)):
        e.append(box([x, y, 14.8], [x + 0.6, y + 0.6, 15], "ironb", faces=["north", "west", "east", "up", "down"]))
    e += [box([7.6, 11.6, 14.2], [8.4, 12.4, 16], "ironb"), box([7.3, 11.3, 14], [8.7, 12.7, 14.2], "ironb")]   # clavo
    model("horseshoe", e, "iron")


def wagon_wheel():
    """Rueda de carreta de pie, recargada en la pared (casi bloque y medio de alto)."""
    e = []
    cx, cy, a = 8, 11, 10.0
    half = a * math.tan(math.pi / 8)
    z1, z2 = 13, 15.4
    e += [box([cx - half, cy - a - 1, z1], [cx + half, cy - a + 1, z2], "w_dark"), box([cx - half, cy + a - 1, z1], [cx + half, cy + a + 1, z2], "w_dark"),
          box([cx - a - 1, cy - half, z1], [cx - a + 1, cy + half, z2], "w_dark"), box([cx + a - 1, cy - half, z1], [cx + a + 1, cy + half, z2], "w_dark")]
    r = a / math.sqrt(2)
    for dx, dy in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        px, py = cx + dx * r, cy + dy * r
        ang = -45 if dx * dy > 0 else 45
        e.append(box([px - half, py - 1, z1], [px + half, py + 1, z2], "w_dark", rot=("z", ang, [px, py, 14])))
    e += [box([cx - a + 1, cy - 0.7, 13.6], [cx + a - 1, cy + 0.7, 14.8], "w_slog"), box([cx - 0.7, cy - a + 1, 13.6], [cx + 0.7, cy + a - 1, 14.8], "w_slog")]
    for ang in (45, -45):
        e.append(box([cx - a + 1.5, cy - 0.7, 13.6], [cx + a - 1.5, cy + 0.7, 14.8], "w_slog", rot=("z", ang, [cx, cy, 14])))
    e += [box([cx - 2.2, cy - 2.2, 12], [cx + 2.2, cy + 2.2, 16], "w_dark"), box([cx - 2.5, cy - 2.5, 13.2], [cx + 2.5, cy + 2.5, 14.2], "iron"),
          box([cx - 0.8, cy - 0.8, 11.6], [cx + 0.8, cy + 0.8, 12], "iron")]
    model("wagon_wheel", e, "w_dark")


def oil_lamp():
    """Lámpara de aceite de latón con depósito ámbar y chimenea de vidrio. Alumbra."""
    b = "obrass"
    e = octagon([5, 0, 5], [11, 0.8, 11], b)
    e += [box([7.2, 0.8, 7.2], [8.8, 2.6, 8.8], b)]
    e += octagon([5.2, 2.6, 5.2], [10.8, 5.6, 10.8], "b_amber")
    e += octagon([6.3, 5.6, 6.3], [9.7, 6.6, 9.7], b)
    e += [box([9.7, 5.8, 7.6], [10.8, 6.4, 8.4], b)]                             # perilla de la mecha
    e += [box([7.4, 6.6, 7.4], [8.6, 8.8, 8.6], "flame"), box([7.7, 8.8, 7.7], [8.3, 9.8, 8.3], "flame")]
    # chimenea: vidrio transparente (solo se ven los bordes y un brillo), panza abajo y cuello arriba
    e += [box([5.6, 6.6, 5.6], [10.4, 9.8, 10.4], "chimney", uv={f: [0, 0, 16, 16] for f in ("north", "south", "west", "east")},
              faces=["north", "south", "west", "east"]),
          box([6.6, 9.8, 6.6], [9.4, 13, 9.4], "chimney", uv={f: [0, 0, 16, 16] for f in ("north", "south", "west", "east")},
              faces=["north", "south", "west", "east"]),
          box([5.6, 9.6, 5.6], [10.4, 9.8, 10.4], "chimney", uv={"up": [0, 0, 16, 16], "down": [0, 0, 16, 16]}, faces=["up", "down"])]
    model("oil_lamp", e, b, render="minecraft:cutout")


def whiskey_barrel():
    """Barril de whisky acostado sobre su caballete, con la tapa y la llave de latón hacia enfrente."""
    side = {f: [0, 0, 16, 16, 90] for f in ("west", "east")}
    e = octagon_xy(1.5, 2.6, 14.5, 15.6, 1, 15.4, "barrel", faces=["west", "east", "up", "down"], uv=side)
    e += octagon_xy(1.5, 2.6, 14.5, 15.6, 1, 15.4, "barrel_t", faces=["north", "south"])
    for z in (3, 12.6):                                                           # aros de hierro
        e += octagon_xy(1.3, 2.4, 14.7, 15.8, z, z + 0.8, "iron", faces=["west", "east", "up", "down"])
    for z in (2.6, 11.8):                                                         # caballete
        e += [box([0.8, 0, z], [3.4, 5, z + 1.6], "w_dark"), box([12.6, 0, z], [15.2, 5, z + 1.6], "w_dark"),
              box([3.4, 0, z], [12.6, 2.4, z + 1.6], "w_dark")]
    e += [box([7.3, 5.4, -1.2], [8.7, 6.6, 1], "obrass"), box([7.4, 4.2, -1.2], [8.6, 5.4, -0.2], "obrass"),     # llave
          box([7.65, 6.6, -0.9], [8.35, 8.6, -0.3], "obrass"), box([6.8, 8.4, -0.9], [9.2, 9, -0.3], "obrass")]
    model("whiskey_barrel", e, "barrel")


def saddle_stand():
    """Silla de montar sobre un caballete de madera, con su cobija, la cabeza de la silla, los estribos y las faldas."""
    e = [box([1, 8.8, 7], [15, 10.8, 9], "w_slog")]
    for x in (1.6, 12.8):
        e += [box([x, 0, 4.2], [x + 1.6, 9, 5.8], "w_dark"), box([x, 0, 10.2], [x + 1.6, 9, 11.8], "w_dark"),
              box([x + 0.3, 2.5, 5.8], [x + 1.3, 3.5, 10.2], "w_dark")]
    e += [box([3.2, 10.8, 3.6], [12.8, 11.4, 12.4], "navajo"),                       # cobija
          box([3.2, 6.8, 3.4], [12.8, 10.8, 3.6], "navajo"), box([3.2, 6.8, 12.4], [12.8, 10.8, 12.6], "navajo")]
    e += [box([4, 11.4, 4.4], [12, 12.8, 11.6], "hide"),                           # asiento
          box([4.6, 6, 3.6], [11.4, 11.4, 4.4], "hide_d"), box([4.6, 6, 11.6], [11.4, 11.4, 12.4], "hide_d"),   # faldas
          box([3.4, 12, 6.2], [5.6, 14.2, 9.8], "hide"), box([3.8, 14.2, 7.2], [5, 16, 8.8], "hide_d"),        # cabeza
          box([3.4, 16, 6.8], [5.4, 16.6, 9.2], "hide_d"),
          box([11, 12, 5.2], [12.6, 15, 10.8], "hide"), box([12.2, 13.6, 5.2], [13, 15.4, 10.8], "hide_d")]     # teja
    for z1, z2 in ((3.2, 3.6), (12.4, 12.8)):                                         # estribos
        e += [box([7.5, 3, z1], [8.5, 6.4, z2], "hide_d"), box([6.6, 1.2, z1 - 0.3], [9.4, 3, z2 + 0.3], "w_dark")]
    model("saddle_stand", e, "hide")


def water_trough():
    """Abrevadero de tablas con agua (para la entrada, junto a los postes de los caballos)."""
    e = [box([0, 1.6, 2], [16, 10, 3.4], "w_spruce"), box([0, 1.6, 12.6], [16, 10, 14], "w_spruce"),
         box([0, 1.6, 3.4], [1.4, 10, 12.6], "w_spruce"), box([14.6, 1.6, 3.4], [16, 10, 12.6], "w_spruce"),
         box([1.4, 1.6, 3.4], [14.6, 3, 12.6], "w_spruce")]
    for x in (2.6, 12.4):
        e += [box([x, 1.8, 1.7], [x + 1, 10.2, 2], "iron"), box([x, 1.8, 14], [x + 1, 10.2, 14.3], "iron"),
              box([x, 10, 1.7], [x + 1, 10.2, 14.3], "iron", faces=["up"])]
    for x in (0.6, 13.4):
        for z in (2.4, 11.6):
            e.append(box([x, 0, z], [x + 2, 1.6, z + 2], "w_dark"))
    e.append(box([1.4, 3, 3.4], [14.6, 8.6, 12.6], "water", faces=["up"]))
    model("water_trough", e, "w_spruce")


def hitching_post():
    """Poste para amarrar caballos: dos postes y un travesaño (en fila se ven como un barandal largo), con argolla."""
    e = []
    for x in (1, 12.6):
        e += [box([x, 0, 6.6], [x + 2.4, 14, 9.4], "w_slog"), box([x - 0.2, 14, 6.4], [x + 2.6, 14.8, 9.6], "w_dark")]
    e += [box([0, 10.6, 7], [16, 12.6, 9], "w_oak")]
    e += [box([7.3, 7, 7.7], [8.7, 7.6, 8.3], "iron"), box([6.8, 7.6, 7.7], [7.4, 10.6, 8.3], "iron"),
          box([8.6, 7.6, 7.7], [9.2, 10.6, 8.3], "iron")]
    model("hitching_post", e, "w_slog")


def coat_rack():
    """Perchero de pie (casi dos bloques) con un sombrero vaquero arriba y una reata colgada."""
    e = [box([7, 0, 7], [9, 27, 9], "w_dark"), box([2.6, 0, 7.2], [13.4, 1.4, 8.8], "w_dark"), box([7.2, 0, 2.6], [8.8, 1.4, 13.4], "w_dark"),
         box([6.6, 1.4, 6.6], [9.4, 2.4, 9.4], "w_dark"), box([6.8, 26.4, 6.8], [9.2, 27.6, 9.2], "w_dark")]
    for k in range(4):                                                               # ganchos
        e += rot90([box([9, 22, 7.5], [11.6, 23, 8.5], "w_dark"), box([10.8, 23, 7.5], [11.8, 24.6, 8.5], "w_dark")], k)
    # sombrero en la punta
    e += [box([2.6, 27.6, 2.6], [13.4, 28.4, 13.4], "hat"), box([2, 28, 3.4], [2.8, 29.4, 12.6], "hat"), box([13.2, 28, 3.4], [14, 29.4, 12.6], "hat"),
          box([5, 28.4, 5.6], [11, 31.4, 10.4], "hat"), box([4.9, 28.4, 5.5], [11.1, 29.4, 10.5], "band"),
          box([5.6, 31.4, 6.4], [10.4, 32, 9.6], "hat")]
    # reata enrollada, colgada de frente
    z1, z2 = 4.6, 5.6
    cx, cy, ra = 8, 17, 3.6
    hs = ra * math.tan(math.pi / 8)
    e += [box([cx - hs, cy - ra - 0.5, z1], [cx + hs, cy - ra + 0.5, z2], "w_oak"), box([cx - hs, cy + ra - 0.5, z1], [cx + hs, cy + ra + 0.5, z2], "w_oak"),
          box([cx - ra - 0.5, cy - hs, z1], [cx - ra + 0.5, cy + hs, z2], "w_oak"), box([cx + ra - 0.5, cy - hs, z1], [cx + ra + 0.5, cy + hs, z2], "w_oak")]
    rr = ra / math.sqrt(2)
    for dx, dy in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        px, py = cx + dx * rr, cy + dy * rr
        e.append(box([px - hs, py - 0.5, z1 + 0.05], [px + hs, py + 0.5, z2 - 0.05], "w_oak", rot=("z", -45 if dx * dy > 0 else 45, [px, py, 5])))
    e += [box([7.5, cy + ra + 0.5, z1 + 0.1], [8.5, 22.6, 7], "w_oak"),                         # de donde cuelga
          box([10.2, 10.4, z1 + 0.1], [11, cy - ra + 1.2, z2 - 0.1], "w_oak")]                  # punta suelta
    model("coat_rack", e, "w_dark")


def cash_register():
    """Caja registradora antigua de latón: cajón de madera, teclado en escalones, ventanilla de la cuenta y manivela."""
    e = [box([1.5, 0, 2], [14.5, 3, 14], "w_dark"), box([7, 1, 1.6], [9, 2, 2], "obrass"),
         box([2.5, 3, 6], [13.5, 10, 14], "brass"),
         box([2.5, 3, 2.6], [13.5, 4.6, 4], "brass"), box([2.5, 3, 4], [13.5, 6.2, 5], "brass"), box([2.5, 3, 5], [13.5, 7.8, 6], "brass")]
    for x in (3.3, 5.4, 7.5, 9.6, 11.7):
        e += [box([x, 4.6, 2.9], [x + 1.1, 5.1, 3.8], "chip_w"), box([x, 6.2, 4.15], [x + 1.1, 6.7, 4.9], "chip_w"),
              box([x, 7.8, 5.15], [x + 1.1, 8.3, 5.9], "chip_w")]
    e += [box([4, 10, 8], [12, 13, 13], "brass"), box([5, 10.8, 7.8], [11, 12.4, 8], "dark", faces=["north"]),
          box([5.6, 11.1, 7.7], [6.8, 12.1, 7.8], "chip_w", faces=["north"]), box([7.4, 11.1, 7.7], [8.6, 12.1, 7.8], "chip_w", faces=["north"]),
          box([9.2, 11.1, 7.7], [10.4, 12.1, 7.8], "chip_w", faces=["north"]),
          box([5.5, 13, 9], [10.5, 14.2, 12], "brass"), box([7.2, 14.2, 10], [8.8, 15, 11], "brass")]
    e += [box([13.5, 5.4, 9], [14.4, 6.4, 10], "brass"), box([14.4, 4, 9.1], [15.2, 7.6, 9.9], "brass"), box([14.2, 7.6, 8.9], [15.4, 8.6, 10.1], "w_dark")]
    model("cash_register", e, "brass")


def rifle_rack():
    """Armero de pared: tabla clara con dos rifles largos acostados sobre sus soportes (los rifles salen un poco de los lados)."""
    e = [box([0.5, 2, 14.8], [15.5, 14, 16], "w_spruce"), box([0, 13.4, 14.2], [16, 14.6, 16], "w_dark"),
         box([0, 1.4, 14.2], [16, 2.6, 16], "w_dark")]
    for x in (2.2, 12.6):
        for y in (4, 9):
            e += [box([x, y, 12], [x + 1.2, y + 1, 14.8], "w_dark"), box([x, y + 1, 12], [x + 1.2, y + 2.6, 12.6], "w_dark")]
    for y in (5, 10):
        e += [box([-2.5, y - 0.4, 12.7], [3.5, y + 1.8, 13.9], "w_dark"),                 # culata
              box([-2.8, y - 0.6, 12.6], [-2.2, y + 2, 14], "iron"),                         # cantonera
              box([3.5, y + 0.2, 12.8], [5.8, y + 1.5, 13.8], "w_dark"),                     # garganta
              box([5.8, y, 12.6], [8.4, y + 1.8, 14], "iron"),                               # cajón de mecanismos
              box([6.2, y - 0.8, 13], [7.6, y, 13.6], "iron"), box([7.6, y + 1.8, 13.1], [8.2, y + 2.4, 13.5], "iron"),
              box([8.4, y + 0.3, 12.8], [13.4, y + 1.1, 13.8], "w_dark"),                    # guardamanos
              box([8.4, y + 1.1, 12.9], [18.4, y + 1.7, 13.7], "iron"),                      # cañón
              box([17.6, y + 1.7, 13.1], [18, y + 2.1, 13.5], "iron")]                       # mira
    model("rifle_rack", e, "w_dark")


def textures2():
    """Latón viejo, cuero de la silla, cobija de rayas y vidrio de la chimenea (transparente con bordes)."""
    import random
    from PIL import Image
    rnd = random.Random(7)
    folder = os.path.join(A, "textures", "block", "west")

    def noisy(base, amp, name, extra=None):
        im = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                k = 1 + rnd.uniform(-amp, amp)
                c = [max(0, min(255, int(v * k))) for v in base]
                if extra:
                    c = extra(x, y, c)
                im.putpixel((x, y), tuple(c) + (255,))
        im.save(os.path.join(folder, name + ".png"))

    def brass(x, y, c):
        k = 1.18 if x in (3, 4, 10) else 0.86 if x in (7, 13) else 1.0
        return [min(255, int(v * k)) for v in c]
    noisy((178, 132, 58), 0.07, "brass_old", brass)

    def leather(x, y, c):
        if y in (1, 14) and x % 2 == 0:
            return [214, 188, 140]                                     # costura
        return c
    noisy((122, 70, 38), 0.08, "leather", leather)
    stripes = {0: (40, 30, 26), 1: (40, 30, 26), 4: (236, 226, 204), 7: (40, 30, 26), 8: (40, 30, 26), 11: (236, 226, 204),
               14: (40, 30, 26), 15: (40, 30, 26)}

    def blanket(x, y, c):
        if y in stripes:
            return list(stripes[y])
        if y in (5, 6) and (x + y) % 4 < 2:
            return [230, 160, 40]                                      # zigzag naranja
        if y in (9, 10) and (x - y) % 4 < 2:
            return [40, 90, 140]                                       # zigzag azul
        return c
    noisy((168, 34, 30), 0.06, "blanket", blanket)
    g = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(16):
        for x, y in ((0, i), (15, i), (i, 0), (i, 15)):
            g.putpixel((x, y), (214, 228, 222, 255))
        if 2 <= i <= 12:
            g.putpixel((3, i), (240, 248, 245, 255))                   # brillo
    g.save(os.path.join(folder, "lamp_glass.png"))


def water_texture():
    """Agua azul animada (la de Minecraft es gris y se pinta por bioma; esta ya viene azul)."""
    from PIL import Image
    frames = 16
    im = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                t = f / frames * 2 * math.pi
                v = math.sin(x * 0.7 + t) + math.sin(y * 0.9 - t * 1.3) + 0.6 * math.sin((x + y) * 0.45 + t * 0.7)
                k = 0.86 + 0.07 * v
                im.putpixel((x, y + 16 * f), (int(44 * k), int(98 * k), int(196 * k), 255))
    path = os.path.join(A, "textures", "block", "west", "water.png")
    im.save(path)
    with open(path + ".mcmeta", "w", encoding="utf-8") as fh:
        fh.write('{"animation": {"frametime": 3}}\n')


DISPLAY_WALL = {"gui": {"rotation": [20, 200, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
                "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                "fixed": {"rotation": [0, 180, 0], "scale": [0.5, 0.5, 0.5]},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.35, 0.35, 0.35]},
                "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.35, 0.35, 0.35]}}
DISPLAY_RACK = {"gui": {"rotation": [30, 225, 0], "translation": [0, -3, 0], "scale": [0.36, 0.36, 0.36]},
                "ground": {"translation": [0, 3, 0], "scale": [0.2, 0.2, 0.2]},
                "fixed": {"scale": [0.4, 0.4, 0.4]},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.3, 0.3, 0.3]},
                "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.3, 0.3, 0.3]}}

TANDA2 = [  # nombre, ¿mira a algún lado?, display, receta (forma, llaves, cantidad)
    ("spittoon", True, dm.DISPLAY, (["C C", " C "], {"C": "minecraft:copper_ingot"}, 1)),
    ("longhorn_skull", True, DISPLAY_WALL, (["B B", " K "], {"B": "minecraft:bone", "K": "minecraft:bone_block"}, 1)),
    ("horseshoe", True, DISPLAY_WALL, (["N N", "N N", " N "], {"N": "minecraft:iron_nugget"}, 1)),
    ("wagon_wheel", True, DISPLAY_WALL, (["SPS", "PLP", "SPS"], {"S": "minecraft:stick", "P": "minecraft:spruce_planks", "L": "minecraft:dark_oak_log"}, 1)),
    ("oil_lamp", False, dm.DISPLAY, ([" G ", "GTG", " C "], {"G": "minecraft:glass_pane", "T": "minecraft:torch", "C": "minecraft:copper_ingot"}, 1)),
    ("whiskey_barrel", True, dm.DISPLAY, ([" N ", "SBS"], {"N": "minecraft:iron_nugget", "S": "minecraft:stick", "B": "minecraft:barrel"}, 1)),
    ("saddle_stand", True, dm.DISPLAY, (["LLL", "SSS", "S S"], {"L": "minecraft:leather", "S": "minecraft:stick"}, 1)),
    ("water_trough", True, dm.DISPLAY, (["PWP", "PPP"], {"P": "minecraft:spruce_planks", "W": "minecraft:water_bucket"}, 1)),
    ("hitching_post", True, dm.DISPLAY, (["SSS", "L L"], {"S": "minecraft:stick", "L": "minecraft:stripped_spruce_log"}, 2)),
    ("coat_rack", False, DISPLAY_RACK, ([" W ", " S ", "SSS"], {"W": "minecraft:brown_wool", "S": "minecraft:stick"}, 1)),
    ("cash_register", True, dm.DISPLAY, (["CCC", "CRC", "PPP"], {"C": "minecraft:copper_ingot", "R": "minecraft:redstone", "P": "minecraft:dark_oak_planks"}, 1)),
    ("rifle_rack", True, DISPLAY_WALL, (["NNN", "PPP"], {"N": "minecraft:iron_nugget", "P": "minecraft:dark_oak_planks"}, 1)),
]


def tanda2():
    textures2()
    water_texture()
    for fn in (spittoon, longhorn_skull, horseshoe, wagon_wheel, oil_lamp, whiskey_barrel, saddle_stand, water_trough, hitching_post,
               coat_rack, cash_register, rifle_rack):
        fn()
    for name, faces, display, (pattern, key, count) in TANDA2:
        (facing_states if faces else any_facing_states)(name)
        item(name, display)
        dm.loot(name)
        dm.recipe(name, pattern, key, count)


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
    tanda2()
    import zfix  # sin parpadeo: al importar decor_models se corrige lo viejo, pero lo de esta corrida se escribe después
    zfix.run(os.path.join(A, "models", "block", "west"))
    print("muebles del Oeste listos")


if __name__ == "__main__":
    main()
