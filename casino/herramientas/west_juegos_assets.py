#!/usr/bin/env python3
"""Recursos de los juegos del saloon: blancos de la galería de tiro (patito, botella, bandido, señorita, estrella), el revólver de
feria (modelo 3D), la caja del mostrador con campana, el icono del toro mecánico, estados, botines.

    python3 casino/herramientas/west_juegos_assets.py [--preview carpeta]
"""
import math
import os
import sys

from PIL import Image, ImageDraw

H = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, H)
import decor_models as dm  # noqa: E402
from decor_models import box, A  # noqa: E402

ENT = os.path.join(A, "textures", "entity", "west")
ITEMT = os.path.join(A, "textures", "item")
T = lambda n: "casino:block/west/" + n  # noqa: E731
DIRS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def rgba(c, a=255):
    return tuple(c) + (a,)


def save(im, name, folder=ENT):
    os.makedirs(folder, exist_ok=True)
    im.save(os.path.join(folder, name + ".png"))
    return im


# ================================================================== blancos (texturas de entidad, se estiran al tamaño del blanco)

def bullseye(d, cx, cy, r):
    for k, col in enumerate(((236, 236, 228), (196, 38, 30), (236, 236, 228), (196, 38, 30))):
        rr = r * (1 - k / 4)
        d.ellipse([cx - rr, cy - rr, cx + rr - 1, cy + rr - 1], fill=rgba(col))


def duck():
    im = Image.new("RGBA", (32, 28), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    out, body, light = (130, 92, 16), (242, 200, 44), (255, 232, 120)
    d.ellipse([3, 11, 25, 27], fill=rgba(out))
    d.ellipse([4, 12, 24, 26], fill=rgba(body))
    d.ellipse([1, 14, 8, 22], fill=rgba(body))            # cola
    d.ellipse([16, 1, 28, 13], fill=rgba(out))           # cabeza
    d.ellipse([17, 2, 27, 12], fill=rgba(body))
    d.polygon([(27, 6), (31, 8), (27, 10)], fill=rgba((236, 120, 30)))   # pico
    d.rectangle([22, 5, 23, 6], fill=rgba((20, 16, 10)))                  # ojo
    d.point((22, 5), fill=rgba((255, 255, 255)))
    d.ellipse([7, 15, 17, 21], fill=rgba(light))         # ala
    bullseye(d, 13, 21, 4)
    d.rectangle([0, 26, 31, 27], fill=rgba((40, 110, 160)))              # agüita
    return save(im, "target_duck")


def bottle():
    im = Image.new("RGBA", (14, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    glass, dark, hi = (52, 130, 64), (26, 74, 34), (150, 220, 150)
    d.rectangle([2, 12, 11, 31], fill=rgba(dark))
    d.rectangle([3, 12, 10, 30], fill=rgba(glass, 235))
    d.polygon([(3, 12), (5, 8), (8, 8), (10, 12)], fill=rgba(glass, 235))
    d.rectangle([5, 3, 8, 8], fill=rgba(glass, 235))
    d.rectangle([5, 0, 8, 3], fill=rgba((140, 96, 54)))                   # corcho
    d.rectangle([3, 17, 10, 24], fill=rgba((232, 220, 186)))              # etiqueta
    d.text((4, 17), "X", fill=rgba((170, 30, 24)))
    d.line([(4, 13), (4, 29)], fill=rgba(hi, 200))
    return save(im, "target_bottle")


def figure(lady):
    im = Image.new("RGBA", (22, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # orilla de madera pintada (es un cartón recortado)
    skin = (226, 186, 150)
    if lady:
        dress, trim = (226, 120, 160), (255, 236, 244)
        d.polygon([(3, 31), (6, 14), (16, 14), (19, 31)], fill=rgba(dress))            # vestido
        d.rectangle([7, 14, 15, 18], fill=rgba(dress))
        d.line([(3, 31), (19, 31)], fill=rgba(trim), width=2)
        d.ellipse([7, 4, 15, 13], fill=rgba(skin))                                        # cara
        d.ellipse([4, 1, 18, 7], fill=rgba((250, 220, 120)))                              # sombrero
        d.rectangle([7, 0, 15, 4], fill=rgba((250, 220, 120)))
        d.rectangle([6, 3, 16, 4], fill=rgba((220, 60, 90)))                              # listón
        d.point((9, 8), fill=rgba((40, 20, 20)))
        d.point((13, 8), fill=rgba((40, 20, 20)))
        d.line([(10, 11), (12, 11)], fill=rgba((200, 40, 60)))
        # corazón en el pecho: ¡no disparar!
        d.polygon([(9, 18), (11, 21), (13, 18)], fill=rgba((230, 30, 60)))
        d.ellipse([8, 16, 11, 19], fill=rgba((230, 30, 60)))
        d.ellipse([11, 16, 14, 19], fill=rgba((230, 30, 60)))
        d.line([(19, 6), (19, 20)], fill=rgba((90, 60, 40)))                              # sombrilla
        d.pieslice([13, 1, 25, 13], 180, 360, fill=rgba((255, 255, 255)))
    else:
        coat, hat = (96, 64, 40), (24, 20, 18)
        d.rectangle([4, 14, 18, 31], fill=rgba(coat))                                     # saco
        d.rectangle([9, 14, 13, 31], fill=rgba((70, 46, 30)))
        d.rectangle([2, 15, 5, 26], fill=rgba(coat))                                      # brazos
        d.rectangle([17, 15, 20, 24], fill=rgba(coat))
        d.rectangle([17, 22, 21, 24], fill=rgba((60, 60, 66)))                            # pistola
        d.ellipse([7, 4, 15, 13], fill=rgba(skin))                                        # cara
        d.rectangle([6, 9, 16, 13], fill=rgba((176, 32, 28)))                             # pañuelo
        d.point((8, 10), fill=rgba((230, 230, 230)))
        d.point((14, 11), fill=rgba((230, 230, 230)))
        d.rectangle([7, 6, 15, 7], fill=rgba((20, 16, 14)))                               # antifaz
        d.point((9, 6), fill=rgba((250, 250, 250)))
        d.point((13, 6), fill=rgba((250, 250, 250)))
        d.rectangle([2, 3, 20, 4], fill=rgba(hat))                                        # sombrero
        d.rectangle([6, 0, 16, 3], fill=rgba(hat))
        bullseye(d, 11, 21, 4)
    # contorno blanco de cartón
    px = im.load()
    out = im.copy()
    po = out.load()
    for y in range(32):
        for x in range(22):
            if px[x, y][3] == 0 and any(0 <= x + dx < 22 and 0 <= y + dy < 32 and px[x + dx, y + dy][3] > 0
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                po[x, y] = (240, 232, 214, 255)
    return save(out, "target_lady" if lady else "target_bandit")


def star():
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    pts = []
    for i in range(12):
        r = 15 if i % 2 == 0 else 8
        a = math.pi / 2 + i * math.pi / 6
        pts.append((16 + r * math.cos(a), 16 - r * math.sin(a)))
    d.polygon(pts, fill=rgba((150, 104, 20)))
    pts2 = [(16 + (x - 16) * 0.82, 16 + (y - 16) * 0.82) for x, y in pts]
    d.polygon(pts2, fill=rgba((242, 196, 52)))
    for x, y in pts[::2]:
        d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=rgba((250, 220, 90)))
    d.ellipse([11, 11, 21, 21], fill=rgba((206, 156, 30)))
    d.ellipse([13, 13, 19, 19], fill=rgba((255, 236, 140)))
    d.line([(9, 8), (12, 11)], fill=rgba((255, 255, 230)))
    return save(im, "target_star")


def back():
    im = Image.new("RGBA", (16, 16), rgba((150, 112, 72)))
    d = ImageDraw.Draw(im)
    for y in range(0, 16, 3):
        d.line([(0, y), (15, y)], fill=rgba((128, 94, 58)))
    return save(im, "target_back")


# ================================================================== objetos

def bull_icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    hide, dark, horn = (92, 52, 28), (54, 30, 16), (236, 228, 204)
    d.rectangle([4, 4, 11, 12], fill=rgba(hide))
    d.rectangle([5, 12, 10, 14], fill=rgba((180, 140, 106)))     # hocico
    d.point((6, 13), fill=rgba((20, 10, 6)))
    d.point((9, 13), fill=rgba((20, 10, 6)))
    d.rectangle([6, 14, 9, 14], fill=rgba((212, 175, 55)))       # argolla
    d.rectangle([5, 7, 6, 8], fill=rgba((250, 250, 240)))
    d.rectangle([9, 7, 10, 8], fill=rgba((250, 250, 240)))
    d.point((5, 8), fill=rgba((10, 6, 4)))
    d.point((10, 8), fill=rgba((10, 6, 4)))
    d.rectangle([5, 4, 10, 5], fill=rgba(dark))
    for sx in (-1, 1):
        x0 = 3 if sx < 0 else 12
        d.rectangle([min(x0, x0 - sx), 4, max(x0, x0 - sx), 5], fill=rgba(horn))
        d.rectangle([x0 + sx, 1, x0 + sx, 4], fill=rgba(horn))
    d.rectangle([2, 6, 3, 7], fill=rgba(hide))
    d.rectangle([12, 6, 13, 7], fill=rgba(hide))
    return save(im, "mechanical_bull", ITEMT)


def gun_textures():
    # acero pavonado con brillo
    im = Image.new("RGBA", (16, 16), rgba((58, 60, 70)))
    d = ImageDraw.Draw(im)
    for y in range(16):
        c = 58 + int(18 * math.sin(y / 15 * math.pi))
        d.line([(0, y), (15, y)], fill=rgba((c, c + 2, c + 10)))
    d.line([(0, 3), (15, 3)], fill=rgba((120, 124, 136)))
    os.makedirs(os.path.join(A, "textures", "block", "west"), exist_ok=True)
    im.save(os.path.join(A, "textures", "block", "west", "gun_steel.png"))
    im = Image.new("RGBA", (16, 16), rgba((118, 70, 38)))
    d = ImageDraw.Draw(im)
    for x in range(0, 16, 2):
        d.line([(x, 0), (x + 1, 15)], fill=rgba((96, 56, 30)))
    d.ellipse([5, 6, 10, 10], fill=rgba((212, 175, 55)))          # medallón
    im.save(os.path.join(A, "textures", "block", "west", "gun_grip.png"))


# ================================================================== modelos

def revolver():
    dm.TEX.update({"g_steel": T("gun_steel"), "g_grip": T("gun_grip"), "w_brass": T("brass")})
    els = [box([7.5, 9, 0.5], [8.5, 10, 8], "g_steel"),                 # cañón
           box([7.6, 10, 0.5], [8.4, 10.4, 8], "g_steel"),              # costilla
           box([7.8, 10.4, 0.8], [8.2, 11, 1.6], "w_brass"),            # mira
           box([7.6, 8.4, 2], [8.4, 9, 7.8], "g_steel"),                # varilla
           box([6.9, 8.3, 8], [9.1, 10.9, 10.6], "g_steel"),            # tambor
           box([7.3, 8, 10.6], [8.7, 10.7, 12.2], "g_steel"),           # armazón
           box([7.75, 10.7, 11.4], [8.25, 11.5, 12.4], "w_brass"),      # martillo
           box([7.25, 6.2, 11.6], [8.75, 8.2, 13.4], "g_grip"),         # cacha
           box([7.25, 3.8, 12.3], [8.75, 6.4, 14.3], "g_grip"),
           box([7.2, 3.4, 12.6], [8.8, 3.9, 14.4], "w_brass"),          # remate
           box([7.75, 6.8, 9.2], [8.25, 7.2, 11.6], "g_steel"),         # guardamonte
           box([7.75, 7.2, 9.2], [8.25, 8.0, 9.6], "g_steel"),
           box([7.85, 7.3, 10.2], [8.15, 8.1, 10.5], "w_brass")]        # gatillo
    m = dm.emit(els, {}, T("gun_steel"))
    m["display"] = {
        "gui": {"rotation": [0, 90, 0], "translation": [0, 1, 0], "scale": [1.3, 1.3, 1.3]},
        "ground": {"translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
        "fixed": {"rotation": [0, 90, 0], "scale": [1.2, 1.2, 1.2]},
        "thirdperson_righthand": {"rotation": [-5, 0, 0], "translation": [0, 2.5, -1], "scale": [0.9, 0.9, 0.9]},
        "thirdperson_lefthand": {"rotation": [-5, 0, 0], "translation": [0, 2.5, -1], "scale": [0.9, 0.9, 0.9]},
        "firstperson_righthand": {"rotation": [0, 0, 0], "translation": [-1, 3.5, -3], "scale": [1.1, 1.1, 1.1]},
        "firstperson_lefthand": {"rotation": [0, 0, 0], "translation": [-1, 3.5, -3], "scale": [1.1, 1.1, 1.1]}}
    dm.write(os.path.join(A, "models", "item", "gallery_revolver.json"), m)


def gallery_block():
    dm.TEX.update({"w_walnut": T("walnut"), "w_dark": T("walnut_dark"), "w_panel": T("piano_panel"), "w_brass": T("brass")})
    els = [box([2, 0, 3], [14, 6, 13], "w_walnut"),
           box([1.5, 6, 2.5], [14.5, 7, 13.5], "w_dark"),
           box([3, 1, 2.8], [13, 5, 3], "w_panel", faces=["north"], uv={"north": [2, 2, 14, 10]}),
           box([7, 3.8, 2.6], [9, 4.3, 2.8], "w_brass", faces=["north", "up", "down", "west", "east"]),
           box([6.4, 7, 6.9], [9.6, 7.5, 10.1], "w_dark"),
           box([6.8, 7.5, 7.3], [9.2, 8.7, 9.7], "w_brass"),
           box([7.2, 8.7, 7.7], [8.8, 9.1, 9.3], "w_brass"),
           box([7.7, 9.1, 8.2], [8.3, 9.6, 8.8], "w_brass"),
           # cajita de balas
           box([10.5, 7, 4], [13, 8.2, 6], "w_panel"),
           box([3, 7, 4.5], [5, 7.6, 11], "w_dark")]
    m = dm.emit(els, {}, T("walnut"))
    dm.write(os.path.join(A, "models", "block", "west", "shooting_gallery.json"), m)
    v = {f"facing={f}": {"model": "casino:block/west/shooting_gallery", "y": y} for f, y in DIRS}
    dm.write(os.path.join(A, "blockstates", "shooting_gallery.json"), {"variants": v})
    dm.write(os.path.join(A, "models", "item", "shooting_gallery.json"), {"parent": "casino:block/west/shooting_gallery", "display": dm.DISPLAY_SMALL})


def duel_bell():
    """Poste con campana y letrero (frente al norte: la calle queda al oeste, x pequeña). El texto del letrero lo pone el renderizador."""
    dm.TEX.update({"w_walnut": T("walnut"), "w_dark": T("walnut_dark"), "w_brass": T("brass"), "w_board": "minecraft:block/birch_planks",
                   "g_steel": T("gun_steel")})
    els = [box([4, 0, 4], [12, 2, 12], "w_walnut"),
           box([6, 2, 6], [10, 30, 10], "w_dark"),
           box([1, 27, 7], [6, 29, 9], "w_dark"),                         # brazo hacia la calle
           box([2.75, 24, 7.75], [3.25, 27, 8.25], "g_steel"),            # cadenita
           box([1.6, 20.5, 6.6], [4.4, 24, 9.4], "w_brass"),              # campana
           box([1.1, 19.6, 6.1], [4.9, 20.6, 9.9], "w_brass"),            # boca
           box([2.6, 18.8, 7.6], [3.4, 19.7, 8.4], "g_steel"),            # badajo
           box([5, 13, 3], [6, 19, 13], "w_board"),                       # letrero
           box([5.2, 12.5, 2.5], [6, 13, 13.5], "w_dark"), box([5.2, 19, 2.5], [6, 19.5, 13.5], "w_dark"),
           box([5.2, 13, 2.5], [6, 19, 3], "w_dark"), box([5.2, 13, 13], [6, 19, 13.5], "w_dark")]
    m = dm.emit(els, {}, T("walnut_dark"))
    dm.write(os.path.join(A, "models", "block", "west", "duel_bell.json"), m)
    v = {f"facing={f}": {"model": "casino:block/west/duel_bell", "y": y} for f, y in DIRS}
    dm.write(os.path.join(A, "blockstates", "duel_bell.json"), {"variants": v})
    dm.write(os.path.join(A, "models", "item", "duel_bell.json"), {"parent": "casino:block/west/duel_bell", "display": dm.DISPLAY_TALL})


def dice_table():
    """Cubierta de mesa con fieltro verde (va sobre una pata de cerca); los cubiletes y los dados los dibuja el renderizador."""
    dm.TEX.update({"v_spruce": "minecraft:block/spruce_planks", "v_felt": "minecraft:block/green_wool", "w_dark": T("walnut_dark")})
    els = [box([0, 0, 0], [16, 2, 16], "v_spruce"),
           box([1.5, 2, 1.5], [14.5, 2.4, 14.5], "v_felt"),
           box([0, 2, 0], [16, 2.8, 1], "w_dark"), box([0, 2, 15], [16, 2.8, 16], "w_dark"),
           box([0, 2, 1], [1, 2.8, 15], "w_dark"), box([15, 2, 1], [16, 2.8, 15], "w_dark")]
    m = dm.emit(els, {}, "minecraft:block/spruce_planks")
    dm.write(os.path.join(A, "models", "block", "west", "liars_dice.json"), m)
    v = {f"facing={f}": {"model": "casino:block/west/liars_dice", "y": y} for f, y in DIRS}
    dm.write(os.path.join(A, "blockstates", "liars_dice.json"), {"variants": v})
    dm.write(os.path.join(A, "models", "item", "liars_dice.json"), {"parent": "casino:block/west/liars_dice", "display": dm.DISPLAY})


def bull_block():
    # el toro entero lo dibuja el renderizador; el bloque no tiene modelo propio
    v = {f"facing={f}": {"model": "casino:block/heist/empty", "y": y} for f, y in DIRS}
    dm.write(os.path.join(A, "blockstates", "mechanical_bull.json"), {"variants": v})
    dm.write(os.path.join(A, "models", "item", "mechanical_bull.json"), {"parent": "item/generated", "textures": {"layer0": "casino:item/mechanical_bull"}})


if __name__ == "__main__":
    ims = [duck(), bottle(), figure(False), figure(True), star(), back()]
    bull_icon()
    gun_textures()
    revolver()
    gallery_block()
    bull_block()
    duel_bell()
    dice_table()
    for n in ("mechanical_bull", "shooting_gallery", "duel_bell", "liars_dice"):
        dm.loot(n)
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGBA", (6 * 80, 90), (60, 40, 24, 255))
        for i, im in enumerate(ims):
            big = im.resize((im.width * 2, im.height * 2), Image.NEAREST)
            sheet.paste(big, (i * 80 + 4, 4), big)
        sheet.save(out)
    print("recursos de los juegos del saloon listos")
