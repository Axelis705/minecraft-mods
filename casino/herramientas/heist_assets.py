#!/usr/bin/env python3
"""Recursos del robo a la bóveda (ronda 9): texturas, modelos, estados y objetos de los bloques del robo, la tarjeta del gerente y
las pieles del guardia armado y del contacto.

    python3 casino/herramientas/heist_assets.py
"""
import os
import random
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import decor_models as dm  # noqa: E402
from decor_models import box, octagon, A  # noqa: E402

TEXD = os.path.join(A, "textures", "block", "heist")
ITEMT = os.path.join(A, "textures", "item")
ENT = os.path.join(A, "textures", "entity")
T = lambda n: "casino:block/heist/" + n  # noqa: E731


def save(im, name):
    os.makedirs(TEXD, exist_ok=True)
    im.save(os.path.join(TEXD, name + ".png"))


def noise(base, amp, rnd, size=16, stripes=None):
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            n = rnd.randint(-amp, amp)
            if stripes == "h":
                n += (2 if y % 2 else -2)
            im.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in base) + (255,))
    return im


def textures():
    rnd = random.Random(11)
    save(noise((58, 60, 66), 4, rnd, stripes="h"), "steel_dark")
    save(noise((150, 154, 162), 5, rnd, stripes="h"), "steel")
    save(noise((24, 24, 28), 3, rnd), "black")
    # pantalla verde / roja del lector
    for name, c in (("screen_green", (40, 220, 90)), ("screen_red", (230, 40, 40)), ("screen_idle", (40, 60, 110))):
        im = Image.new("RGBA", (16, 16), (8, 10, 14, 255))
        for y in range(3, 13):
            for x in range(3, 13):
                k = 0.75 + 0.25 * ((x + y) % 2)
                im.putpixel((x, y), tuple(int(v * k) for v in c) + (255,))
        save(im, name)
    # nota: papel con renglones y un número encerrado
    im = Image.new("RGBA", (16, 16), (244, 238, 216, 255))
    d = ImageDraw.Draw(im)
    for y in (3, 6, 9, 12):
        d.line([(2, y), (13, y)], fill=(120, 140, 200, 255))
    d.ellipse([7, 6, 13, 12], outline=(200, 30, 30, 255))
    d.point([(9, 8), (10, 9), (11, 8)], fill=(40, 40, 50, 255))
    save(im, "note")
    # cajas de seguridad (frente)
    im = noise((150, 154, 162), 4, rnd)
    d = ImageDraw.Draw(im)
    for gx in (0, 8):
        for gy in (0, 5, 10):
            d.rectangle([gx, gy, gx + 7, gy + 4], outline=(70, 74, 82, 255))
            d.point([(gx + 6, gy + 2)], fill=(210, 170, 60, 255))
            d.point([(gx + 2, gy + 2), (gx + 3, gy + 2)], fill=(40, 42, 46, 255))
    save(im, "deposit_front")
    # monitor de cámaras (4 cuadros, gris, con hora y REC)
    frames = []
    for f in range(4):
        fr = Image.new("RGBA", (16, 16), (12, 12, 14, 255))
        r2 = random.Random(f + 3)
        for y in range(1, 15):
            for x in range(1, 15):
                v = 40 + r2.randint(0, 18)
                # pasillo en perspectiva con una figura a veces
                if abs(x - 8) <= (y - 2) * 0.5 and y > 5:
                    v += 30
                if f % 2 == 0 and 7 <= x <= 8 and 8 <= y <= 12:
                    v = 20
                fr.putpixel((x, y), (v, v + 4, v, 255))
        fr.putpixel((13, 2), (230, 30, 30, 255) if f % 2 == 0 else (60, 20, 20, 255))
        for x in range(2, 7):
            fr.putpixel((x, 13), (200, 200, 200, 255))
        frames.append(fr)
    strip = Image.new("RGBA", (16, 64))
    for i, fr in enumerate(frames):
        strip.paste(fr, (0, i * 16))
    save(strip, "cctv")
    with open(os.path.join(TEXD, "cctv.png.mcmeta"), "w") as fh:
        fh.write('{"animation": {"frametime": 10}}\n')
    # consola: teclado y botones
    im = noise((34, 36, 40), 3, rnd)
    for y in (4, 7, 10):
        for x in range(2, 14, 2):
            im.putpixel((x, y), (120, 124, 132, 255))
    for x, c in ((3, (230, 50, 50)), (6, (60, 220, 90)), (9, (240, 200, 60)), (12, (60, 140, 240))):
        im.putpixel((x, 13), c + (255,))
    save(im, "console")
    # rejilla del ducto
    im = noise((92, 96, 104), 4, rnd)
    for y in range(1, 16, 3):
        for x in range(1, 15):
            im.putpixel((x, y), (24, 26, 30, 255))
    save(im, "grate")
    # botín
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            band = 6 <= x <= 9
            n = rnd.randint(-6, 6)
            im.putpixel((x, y), ((236, 226, 196) if band else (76 + n, 150 + n, 86 + n)) + (255,))
    save(im, "cash")
    im = noise((236, 186, 54), 10, rnd)
    for x in range(16):
        im.putpixel((x, 0), (255, 236, 140, 255))
    save(im, "gold_bar")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = (x * 7 + y * 13) % 5
            im.putpixel((x, y), (90 + v * 30, 220 + v * 7, 230, 255))
    save(im, "diamonds")
    im = Image.new("RGBA", (16, 16))
    cols = [(180, 36, 44), (32, 132, 68), (28, 28, 33), (40, 70, 160)]
    for y in range(16):
        for x in range(16):
            c = cols[(y // 4) % 4]
            if x % 4 == 0:
                c = (240, 240, 240)
            im.putpixel((x, y), c + (255,))
    save(im, "chips")
    save(noise((196, 200, 208), 6, rnd, stripes="h"), "chrome")
    save(noise((20, 20, 22), 2, rnd), "rubber")
    # caja fuerte (frente con dial)
    im = noise((92, 96, 104), 4, rnd)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline=(52, 54, 60, 255))
    d.ellipse([5, 4, 11, 10], fill=(40, 42, 46, 255), outline=(200, 170, 70, 255))
    d.point([(8, 5)], fill=(240, 60, 60, 255))
    d.rectangle([12, 6, 13, 9], fill=(200, 170, 70, 255))
    save(im, "safe_front")


# ================================================================== modelos

def wall_models():
    dm.TEX.update({"h_steel": T("steel"), "h_dark": T("steel_dark"), "h_black": T("black"), "h_note": T("note"),
                   "h_console": T("console"), "h_grate": T("grate"), "h_chrome": T("chrome"), "h_rubber": T("rubber"),
                   "h_safe": T("safe_front"), "h_deposit": T("deposit_front"), "h_cctv": T("cctv")})
    # cámara: el soporte lo dibuja el renderizador (el modelo queda vacío, solo partículas)
    dm.write(os.path.join(A, "models", "block", "heist", "empty.json"), {"textures": {"particle": T("steel")}, "elements": []})
    cam_item = [box([6, 6, 4], [10, 10, 12], "h_steel"), box([7, 7, 3], [9, 9, 4], "h_black"), box([7, 4, 10], [9, 6, 14], "h_dark"),
                box([5, 3, 13], [11, 7, 16], "h_dark")]
    model("security_camera_item", cam_item, T("steel"))
    single_facing("security_camera", "empty")
    item("security_camera", "security_camera_item", dm.DISPLAY_SMALL)
    # láser: caja chica con lente (el rayo lo dibuja el renderizador)
    model("laser_emitter", [box([5, 5, 14], [11, 11, 16], "h_dark"), box([6, 6, 13], [10, 10, 14], "h_black"),
                            box([7, 9, 12.5], [9, 10.6, 13], "h_steel")], T("steel_dark"))
    single_facing("laser_emitter", "laser_emitter")
    item("laser_emitter", "laser_emitter", dm.DISPLAY_SMALL)
    # lector de tarjeta: caja negra con pantalla y ranura
    for lit, scr in ((False, "screen_red"), (True, "screen_green")):
        name = "card_reader" + ("_lit" if lit else "")
        model(name, [box([5, 3, 14], [11, 13, 16], "h_black"), box([6, 8, 13.8], [10, 12, 14], "#screen", faces=["north"], uv="full"),
                     box([6, 4, 13.5], [10, 5, 14], "h_steel")], T("steel_dark"), {"screen": T(scr)})
    v = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit in ("false", "true"):
            v[f"facing={f},lit={lit}"] = {"model": "casino:block/heist/card_reader" + ("_lit" if lit == "true" else ""), "y": y}
    state("card_reader", v)
    item("card_reader", "card_reader", dm.DISPLAY_SMALL)
    # caja fuerte de pared (abierta: la puerta girada)
    body = [box([2, 0, 7], [14, 14, 16], "h_dark"), box([3, 1, 7.5], [13, 13, 8], "h_black", faces=["north"])]
    model("wall_safe", body + [box([2, 0, 6], [14, 14, 7], "h_safe", uv={"north": [0, 0, 16, 16]})], T("steel_dark"))
    model("wall_safe_open", body + [box([1, 0, -5], [2, 14, 7], "h_safe", uv={"west": [0, 0, 16, 16], "east": [0, 0, 16, 16]})], T("steel_dark"))
    v = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for o in ("false", "true"):
            v[f"facing={f},open={o}"] = {"model": "casino:block/heist/wall_safe" + ("_open" if o == "true" else ""), "y": y}
    state("wall_safe", v)
    item("wall_safe", "wall_safe", dm.DISPLAY)
    # nota con pista (papel acostado sobre la mesa)
    model("clue_note", [box([3, 0, 4], [13, 0.3, 12], "h_note", uv={"up": [0, 0, 16, 16], "down": [0, 0, 16, 16]}),
                        box([10, 0.3, 5], [11, 0.6, 11], "h_black", faces=["up", "north", "south", "east", "west"])], T("note"))
    single_facing("clue_note", "clue_note")
    item("clue_note", "clue_note", dm.DISPLAY_SMALL)
    # consola de cámaras: escritorio inclinado con botones
    model("camera_console", [box([1, 0, 6], [15, 4, 16], "h_dark"), box([1, 4, 9], [15, 6, 16], "h_console", uv={"up": [0, 0, 16, 16]}),
                             box([2, 4, 6], [14, 4.5, 9], "h_console", uv={"up": [0, 0, 16, 16]})], T("steel_dark"))
    single_facing("camera_console", "camera_console")
    item("camera_console", "camera_console", dm.DISPLAY)
    # monitor (cuadro animado)
    model("cctv_monitor", [box([0, 0, 10], [16, 16, 16], "h_black"), box([1, 1, 9.9], [15, 15, 10], "h_cctv", faces=["north"],
                                                                         uv={"north": [1, 1, 15, 15]})], T("black"))
    single_facing("cctv_monitor", "cctv_monitor")
    item("cctv_monitor", "cctv_monitor", dm.DISPLAY)
    # cajas de seguridad: bloque con frente
    dm.write(os.path.join(A, "models", "block", "heist", "deposit_boxes.json"),
             {"parent": "minecraft:block/orientable", "textures": {"front": T("deposit_front"), "side": T("steel"), "top": T("steel")}})
    single_facing("deposit_boxes", "deposit_boxes")
    dm.write(os.path.join(A, "models", "item", "deposit_boxes.json"), {"parent": "casino:block/heist/deposit_boxes"})
    # ducto de gas (bloque del techo con rejilla abajo)
    dm.write(os.path.join(A, "models", "block", "heist", "gas_vent.json"),
             {"parent": "minecraft:block/cube_bottom_top", "textures": {"bottom": T("grate"), "side": T("steel"), "top": T("steel")}})
    state("gas_vent", {"gas=false": {"model": "casino:block/heist/gas_vent"}, "gas=true": {"model": "casino:block/heist/gas_vent"}})
    dm.write(os.path.join(A, "models", "item", "gas_vent.json"), {"parent": "casino:block/heist/gas_vent"})
    # puerta de la bóveda y sellos (los dibuja el renderizador)
    single_facing("vault_door", "empty", extra=("open", ("false", "true")))
    state("vault_seal", {"": {"model": "casino:block/heist/empty"}})
    # objeto de la puerta: un disco plano
    dm.write(os.path.join(A, "models", "item", "vault_door.json"), {"parent": "item/generated", "textures": {"layer0": "casino:item/vault_door"}})
    # carritos de botín
    carts()


def model(name, els, particle, textures=None):
    dm.write(os.path.join(A, "models", "block", "heist", name + ".json"), dm.emit(els, textures or {}, particle))


def state(name, variants):
    dm.write(os.path.join(A, "blockstates", name + ".json"), {"variants": variants})


def single_facing(name, mdl, extra=None):
    v = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        if extra:
            for val in extra[1]:
                v[f"facing={f},{extra[0]}={val}"] = {"model": "casino:block/heist/" + mdl, "y": y}
        else:
            v[f"facing={f}"] = {"model": "casino:block/heist/" + mdl, "y": y}
    state(name, v)


def item(name, mdl, display):
    dm.write(os.path.join(A, "models", "item", name + ".json"), {"parent": "casino:block/heist/" + mdl, "display": display})


def carts():
    frame = [box([1, 2, 2], [15, 3, 14], "h_chrome"),                       # bandeja de abajo
             box([1, 9, 2], [15, 10, 14], "h_chrome")]                      # bandeja de arriba
    for x in (1, 14):
        for z in (2, 13):
            frame.append(box([x, 1, z], [x + 1, 10, z + 1], "h_chrome"))
    for x in (2, 13):
        for z in (3, 12):
            frame.append(box([x - 0.5, 0, z - 0.5], [x + 1.5, 1.2, z + 1.5], "h_rubber"))
    frame.append(box([2, 10, 13], [14, 13, 14], "h_chrome", faces=["north", "south", "up"]))      # agarradera
    texs = {"cash": T("cash"), "gold": T("gold_bar"), "diamond": T("diamonds"), "chips": T("chips")}
    for kind, tex in texs.items():
        for left in range(4):
            els = list(frame)
            if kind == "cash":
                # fajos en filas (cada nivel una capa)
                for layer in range(left):
                    for (x0, z0) in ((2.5, 3), (8.5, 3), (2.5, 8), (8.5, 8)):
                        els.append(box([x0, 10 + layer * 1.6, z0], [x0 + 5, 11.5 + layer * 1.6, z0 + 4.5], "#loot"))
            elif kind == "gold":
                for layer in range(left):
                    for i, x0 in enumerate((2.5, 6.0, 9.5)):
                        off = 0.8 if layer % 2 else 0
                        els.append(box([x0 + off, 10 + layer * 1.4, 3.5], [x0 + off + 3, 11.3 + layer * 1.4, 12.5], "#loot"))
            elif kind == "diamond":
                els.append(box([2, 10, 3], [14, 11, 13], "h_black"))
                for layer in range(left):
                    for (x0, z0) in ((3, 4), (6, 4), (9, 4), (3, 8), (6, 8), (9, 8), (4.5, 6), (7.5, 6), (10.5, 6)):
                        if (int(x0 + z0) + layer) % 3 != 2:
                            els.append(box([x0, 11 + layer * 1.2, z0], [x0 + 1.8, 12 + layer * 1.2, z0 + 1.8], "#loot"))
            else:
                for layer in range(left):
                    for (x0, z0) in ((3, 4), (7, 4), (11, 4), (3, 9), (7, 9), (11, 9)):
                        els.append(box([x0, 10 + layer * 1.8, z0], [x0 + 2.6, 11.7 + layer * 1.8, z0 + 2.6], "#loot"))
            model(f"loot_cart_{kind}_{left}", els, T("chrome"), {"loot": tex})
    v = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for kind in texs:
            for left in range(4):
                v[f"facing={f},kind={kind},left={left}"] = {"model": f"casino:block/heist/loot_cart_{kind}_{left}", "y": y}
    state("loot_cart", v)
    item("loot_cart", "loot_cart_gold_3", dm.DISPLAY)


# ================================================================== objetos y pieles

def items():
    rnd = random.Random(4)
    # tarjeta del gerente: negra con banda dorada, chip y "M"
    im = Image.new("RGBA", (16, 16))
    for y in range(4, 13):
        for x in range(1, 15):
            im.putpixel((x, y), (28 + rnd.randint(0, 6), 28, 34, 255))
    for x in range(1, 15):
        im.putpixel((x, 6), (214, 172, 56, 255))
    for (x, y) in ((3, 8), (4, 8), (3, 9), (4, 9)):
        im.putpixel((x, y), (230, 200, 90, 255))
    for (x, y) in ((9, 9), (9, 10), (10, 9), (11, 10), (12, 9), (12, 10), (11, 9)):
        im.putpixel((x, y), (230, 230, 240, 255))
    for x in range(1, 15):
        im.putpixel((x, 4), (60, 60, 70, 255)); im.putpixel((x, 12), (12, 12, 16, 255))
    im.save(os.path.join(ITEMT, "manager_keycard.png"))
    dm.write(os.path.join(A, "models", "item", "manager_keycard.json"), {"parent": "item/generated", "textures": {"layer0": "casino:item/manager_keycard"}})
    # icono de la puerta de la bóveda
    im = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(im)
    d.ellipse([0, 0, 15, 15], fill=(60, 62, 68, 255))
    d.ellipse([2, 2, 13, 13], fill=(150, 156, 166, 255))
    d.line([(4, 7), (11, 8)], fill=(214, 172, 56, 255))
    d.line([(7, 4), (8, 11)], fill=(214, 172, 56, 255))
    d.ellipse([6, 6, 9, 9], fill=(70, 72, 80, 255))
    im.save(os.path.join(ITEMT, "vault_door.png"))


def skins():
    # guardia armado: del guardia normal, chaleco táctico oscuro, casco y franja blanca
    im = Image.open(os.path.join(ENT, "dealer_guard.png")).convert("RGBA")
    px = im.load()
    rnd = random.Random(9)
    for y in range(16, 32):
        for x in range(16, 40):
            c = px[x, y]
            if c[3] and max(c[:3]) < 90:
                v = 34 + rnd.randint(-4, 4)
                px[x, y] = (v, v + 6, v - 2, 255)
    for x in range(20, 28):
        px[x, 24] = (220, 220, 220, 255)
    for (x, y) in ((22, 21), (23, 21), (22, 22), (23, 22)):
        px[x, y] = (214, 172, 56, 255)
    # casco en la capa del sombrero (32..64, 0..16): arriba y la parte alta de los lados
    for y in range(0, 8):
        for x in range(40, 56):
            px[x, y] = (42, 46, 40, 255) if y < 8 else px[x, y]
    for y in range(8, 11):
        for x in range(32, 64):
            px[x, y] = (40 + rnd.randint(-3, 3), 44, 38, 255)
    for x in range(40, 48):
        px[x, 11] = (20, 20, 22, 255)
    im.save(os.path.join(ENT, "dealer_swat.png"))
    # el contacto: del cliente de traje vino, saco carbón y sombrero de ala
    im = Image.open(os.path.join(ENT, "dealer_patron_6.png")).convert("RGBA")
    px = im.load()
    for y in range(16, 64):
        for x in range(0, 64):
            c = px[x, y]
            if c[3] and c[0] > 80 and c[1] < 50 and c[2] < 60:
                k = (c[0] - 80) / 100
                v = int(38 + 20 * k)
                px[x, y] = (v, v, v + 4, 255)
    for y in range(0, 8):
        for x in range(40, 56):
            px[x, y] = (54, 40, 30, 255)
    for y in range(8, 10):
        for x in range(32, 64):
            px[x, y] = (54, 40, 30, 255)
    for x in range(32, 64):
        px[x, 10] = (20, 18, 16, 255)
    im.save(os.path.join(ENT, "dealer_fixer.png"))


if __name__ == "__main__":
    textures()
    wall_models()
    items()
    skins()
    import zfix
    zfix.run(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/assets/casino/models"))
    print("listo")
