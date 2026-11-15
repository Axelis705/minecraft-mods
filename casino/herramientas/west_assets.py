#!/usr/bin/env python3
"""Recursos del casino del Viejo Oeste (saloon): texturas, modelos, estados, objetos, botines y recetas de los bloques nuevos
(puertas batientes, puertita VIP, pianola, letras pintadas, carteles de "Se busca") y las pieles del sheriff, cantinero, crupier,
anfitrión y los vaqueros.

    python3 casino/herramientas/west_assets.py [--preview carpeta]
"""
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFont

H = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, H)
import decor_models as dm  # noqa: E402
from decor_models import box, A  # noqa: E402
from neon_tex import CHARS, CHAR_NAMES  # noqa: E402
import patron_skins as ps  # noqa: E402

TEXD = os.path.join(A, "textures", "block", "west")
ITEMT = os.path.join(A, "textures", "item")
ENT = os.path.join(A, "textures", "entity")
T = lambda n: "casino:block/west/" + n  # noqa: E731
RYE = os.path.join(H, "fuentes", "Rye-Regular.ttf")
SANS = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
DIRS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
# colores de pintura de las letras (mismo orden que el neón: rojo, rosa, dorado, verde, celeste, blanco)
PAINT = [(150, 34, 26), (176, 74, 92), (196, 150, 48), (52, 92, 46), (52, 104, 120), (236, 228, 206)]
PAINT_NAMES = ["red", "pink", "gold", "green", "cyan", "white"]


def save(im, name):
    os.makedirs(TEXD, exist_ok=True)
    im.save(os.path.join(TEXD, name + ".png"))


def wood(base, rnd, size=16, grain=10, vertical=True):
    im = Image.new("RGBA", (size, size))
    offs = [rnd.randint(-grain, grain) for _ in range(size)]
    for y in range(size):
        for x in range(size):
            k = x if vertical else y
            line = offs[k] + int(4 * ((y if vertical else x) % 7 == 0))
            n = rnd.randint(-4, 4) + line
            im.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in base) + (255,))
    return im


# ================================================================== texturas

def textures():
    rnd = random.Random(31)
    save(wood((92, 54, 32), rnd), "walnut")
    save(wood((64, 36, 22), rnd), "walnut_dark")
    save(wood((92, 54, 32), rnd, vertical=False), "walnut_h")
    # frente del piano: tablero con molduras
    im = wood((92, 54, 32), rnd, vertical=False)
    d = ImageDraw.Draw(im)
    d.rectangle([2, 2, 13, 13], outline=(60, 34, 20, 255))
    d.rectangle([3, 3, 12, 12], outline=(118, 74, 44, 255))
    save(im, "piano_panel")
    im = Image.new("RGBA", (16, 16), (196, 160, 70, 255))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), (196 + rnd.randint(-14, 14), 160 + rnd.randint(-12, 12), 70, 255))
    save(im, "brass")
    letters()
    posters()
    item_icons()


def glyph_img(ch, color, size=32):
    big = Image.new("L", (256, 256), 0)
    d = ImageDraw.Draw(big)
    path = RYE if ch.isascii() else SANS
    f = ImageFont.truetype(path, 236)
    bb = d.textbbox((0, 0), ch, font=f)
    w, h = bb[2] - bb[0], bb[3] - bb[1]
    sc = min(212 / max(w, 1), 212 / max(h, 1), 1.0)
    f = ImageFont.truetype(path, int(236 * sc))
    bb = d.textbbox((0, 0), ch, font=f)
    w, h = bb[2] - bb[0], bb[3] - bb[1]
    d.text(((256 - w) / 2 - bb[0], (256 - h) / 2 - bb[1]), ch, font=f, fill=255)
    sm = big.resize((size, size), Image.BOX)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    rnd = random.Random(ord(ch) * 7 + sum(color))
    shadow = tuple(int(c * 0.45) for c in color)
    # sombra pintada abajo a la derecha (letrero de pintor del Oeste)
    for y in range(size):
        for x in range(size):
            if sm.getpixel((x, y)) > 110:
                for dx, dy in ((1, 1),):
                    if 0 <= x + dx < size and 0 <= y + dy < size:
                        out.putpixel((x + dx, y + dy), shadow + (255,))
    for y in range(size):
        for x in range(size):
            if sm.getpixel((x, y)) > 110:
                n = rnd.randint(-10, 10)
                worn = rnd.random() < 0.05            # pintura un poco gastada
                c = tuple(max(0, min(255, v + n + (40 if worn else 0))) for v in color)
                out.putpixel((x, y), c + (255,))
    return out


def letters():
    for ci, cn in enumerate(PAINT_NAMES):
        for ch in CHARS:
            nm = CHAR_NAMES.get(ch, ch.lower())
            if ch == " ":
                im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
                im.putpixel((0, 0), (0, 0, 0, 1))
            else:
                im = glyph_img(ch, PAINT[ci])
            sub = os.path.join(TEXD, "letter")
            os.makedirs(sub, exist_ok=True)
            im.save(os.path.join(sub, f"{cn}_{nm}.png"))


OUTLAWS = [  # (texto de arriba, recompensa, piel, pelo, sombrero, barba)
    ("WANTED", "$500", (214, 170, 128), (60, 40, 24), (40, 30, 22), True),
    ("SE BUSCA", "$1000", (190, 140, 100), (20, 18, 16), (90, 60, 34), False),
    ("WANTED", "$250", (230, 190, 150), (150, 70, 30), (120, 100, 70), True),
    ("SE BUSCA", "$2000", (160, 110, 76), (30, 24, 20), (24, 22, 20), True),
]


def posters():
    for i, (title, prize, skin, hair, hat, beard) in enumerate(OUTLAWS):
        rnd = random.Random(50 + i)
        S = 64
        im = Image.new("RGBA", (S, S))
        for y in range(S):
            for x in range(S):
                edge = min(x, y, S - 1 - x, S - 1 - y)
                k = rnd.randint(-8, 8) - (10 if edge < 3 else 0)
                im.putpixel((x, y), (226 + k, 204 + k, 158 + k, 255))
        d = ImageDraw.Draw(im)
        f = ImageFont.truetype(RYE, 13 if len(title) <= 6 else 10)
        bb = d.textbbox((0, 0), title, font=f)
        d.text(((S - (bb[2] - bb[0])) / 2 - bb[0], 3 - bb[1]), title, font=f, fill=(60, 30, 20, 255))
        # retrato con marco
        oy = -3
        d.rectangle([16, 18 + oy, 47, 45 + oy], fill=(206, 184, 140, 255), outline=(70, 44, 28, 255))
        d.rectangle([20, 38 + oy, 43, 44 + oy], fill=(80, 60, 44, 255))          # hombros
        d.ellipse([24, 25 + oy, 39, 42 + oy], fill=skin + (255,))
        d.rectangle([21, 22 + oy, 42, 25 + oy], fill=hat + (255,))               # ala del sombrero
        d.rectangle([25, 19 + oy, 38, 23 + oy], fill=hat + (255,))               # copa
        d.point([(28, 31 + oy), (35, 31 + oy)], fill=(20, 20, 20, 255))         # ojos
        if beard:
            d.rectangle([26, 36 + oy, 37, 41 + oy], fill=hair + (255,))
            d.line([(29, 35 + oy), (34, 35 + oy)], fill=hair + (255,))
        else:
            d.line([(29, 37 + oy), (34, 37 + oy)], fill=(110, 50, 40, 255))
            d.rectangle([24, 33 + oy, 39, 36 + oy], fill=(150, 30, 30, 255))      # pañuelo de bandido
        f2 = ImageFont.truetype(RYE, 10)
        bb = d.textbbox((0, 0), "REWARD", font=f2)
        d.text(((S - (bb[2] - bb[0])) / 2 - bb[0], 44 - bb[1]), "REWARD" if title == "WANTED" else "RECOMPENSA", font=f2 if title == "WANTED" else ImageFont.truetype(RYE, 7), fill=(70, 34, 22, 255))
        f3 = ImageFont.truetype(RYE, 10)
        bb = d.textbbox((0, 0), prize, font=f3)
        d.text(((S - (bb[2] - bb[0])) / 2 - bb[0], 52 - bb[1]), prize, font=f3, fill=(140, 30, 24, 255))
        d.ellipse([30, 1, 33, 4], fill=(110, 110, 110, 255))          # clavo
        save(im, f"wanted_{i}")


def item_icons():
    os.makedirs(ITEMT, exist_ok=True)
    for name, panel in (("saloon_door", None), ("vip_gate", (120, 16, 26))):
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        fr = (92, 56, 30, 255) if not panel else (74, 36, 20, 255)
        for x0, x1 in ((1, 7), (9, 15)):
            d.rectangle([x0, 4, x1, 13], outline=fr)
            if panel:
                d.rectangle([x0 + 1, 5, x1 - 1, 12], fill=panel + (255,))
            else:
                for x in range(x0 + 2, x1, 2):
                    d.line([(x, 6), (x, 12)], fill=(160, 106, 56, 255))
                d.line([(x0 + 1, 8), (x1 - 1, 8)], fill=fr)
            d.line([(x0, 3), (x1, 3 + (1 if x0 == 1 else -1))], fill=fr)
        if panel:
            d.rectangle([5, 8, 10, 9], fill=(212, 175, 55, 255))
        im.save(os.path.join(ITEMT, name + ".png"))


# ================================================================== modelos

def model(name, els, particle, textures=None, render=None):
    m = dm.emit(els, textures or {}, particle)
    if render:
        m["render_type"] = render
    dm.write(os.path.join(A, "models", "block", "west", name + ".json"), m)


def state(name, variants):
    dm.write(os.path.join(A, "blockstates", name + ".json"), {"variants": variants})


def models():
    dm.TEX.update({"w_walnut": T("walnut"), "w_dark": T("walnut_dark"), "w_h": T("walnut_h"), "w_panel": T("piano_panel"),
                   "w_brass": T("brass")})
    # puertas (las dibuja el renderizador): modelo vacío
    for name, extra in (("saloon_door", None), ("vip_gate", ("access", ("1", "2")))):
        v = {}
        for f, y in DIRS:
            for hinge in ("left", "right"):
                vals = extra[1] if extra else (None,)
                for val in vals:
                    key = f"facing={f},hinge={hinge}" + (f",{extra[0]}={val}" if extra else "")
                    v[key] = {"model": "casino:block/heist/empty", "y": y}
        state(name, v)
        dm.write(os.path.join(A, "models", "item", name + ".json"), {"parent": "item/generated", "textures": {"layer0": "casino:item/" + name}})
    # pianola: piano vertical (frente al norte), el teclado y el rollo los anima el renderizador
    piano = [box([0, 0, 6], [16, 2, 16], "w_dark"),                       # zócalo
             box([1, 2, 6.5], [15, 7, 16], "w_panel", uv={"north": [0, 0, 16, 16]}),
             box([0, 2, 6], [1, 7, 16], "w_walnut"), box([15, 2, 6], [16, 7, 16], "w_walnut"),
             box([1, 7, 2.5], [15, 8.5, 16], "w_h"),                       # repisa del teclado
             box([1, 8.5, 6.2], [15, 9, 7], "w_dark"),                     # tira de fieltro detrás de las teclas
             box([0, 7, 2], [1, 9.5, 16], "w_walnut"), box([15, 7, 2], [16, 9.5, 16], "w_walnut"),   # mejillas
             box([0, 8.5, 7], [16, 11, 16], "w_walnut"),
             box([0, 14.5, 7], [16, 16, 16], "w_walnut"),
             box([0, 11, 7], [4, 14.5, 16], "w_panel", uv={"north": [0, 0, 8, 16]}),
             box([12, 11, 7], [16, 14.5, 16], "w_panel", uv={"north": [8, 0, 16, 16]}),
             box([4, 11, 7.2], [12, 14.5, 16], "w_dark"),                  # hueco del rollo (el papel lo dibuja el renderizador)
             box([-0.5, 16, 6.5], [16.5, 17, 16.5], "w_dark"),             # tapa
             box([2, 16.5, 9], [3.2, 17.7, 10.2], "w_brass"), box([12.8, 16.5, 9], [14, 17.7, 10.2], "w_brass"),   # candeleros
             box([6, 0.5, 4.5], [7, 1, 6], "w_brass"), box([9, 0.5, 4.5], [10, 1, 6], "w_brass")]                 # pedales
    model("player_piano", piano, T("walnut"))
    v = {}
    for f, y in DIRS:
        for lit in ("false", "true"):
            v[f"facing={f},lit={lit}"] = {"model": "casino:block/west/player_piano", "y": y}
    state("player_piano", v)
    dm.write(os.path.join(A, "models", "item", "player_piano.json"), {"parent": "casino:block/west/player_piano", "display": dm.DISPLAY})
    # letras pintadas: una calcomanía delgada sobre la pared
    model("letter_base", [box([0, 0, 15.75], [16, 16, 15.76], "#paint", faces=["north"], uv="full")], T("letter/red_a"),
          {"paint": T("letter/red_a")}, render="minecraft:cutout")
    v = {}
    for ci, cn in enumerate(PAINT_NAMES):
        for gi, ch in enumerate(CHARS):
            nm = CHAR_NAMES.get(ch, ch.lower())
            mdl = f"letter_{cn}_{nm}"
            dm.write(os.path.join(A, "models", "block", "west", mdl + ".json"),
                     {"parent": "casino:block/west/letter_base", "textures": {"paint": T(f"letter/{cn}_{nm}"), "particle": T(f"letter/{cn}_{nm}")}})
            for f, y in DIRS:
                v[f"color={ci},facing={f},glyph={gi}"] = {"model": "casino:block/west/" + mdl, "y": y}
    state("painted_letter", v)
    dm.write(os.path.join(A, "models", "item", "painted_letter.json"), {"parent": "item/generated", "textures": {"layer0": T("letter/red_a")}})
    # carteles de "Se busca": papel pegado a la pared
    for i in range(4):
        model(f"wanted_{i}", [box([1, 1, 15.6], [15, 15, 16], "#p", faces=["north"], uv="full"),
                              box([1, 1, 15.6], [15, 15, 16], "#p", faces=["west", "east", "up", "down"], uv={"west": [0, 0, 1, 16], "east": [0, 0, 1, 16], "up": [0, 0, 16, 1], "down": [0, 0, 16, 1]})],
              T(f"wanted_{i}"), {"p": T(f"wanted_{i}")})
    v = {}
    for i in range(4):
        for f, y in DIRS:
            v[f"design={i},facing={f}"] = {"model": f"casino:block/west/wanted_{i}", "y": y}
    state("wanted_poster", v)
    dm.write(os.path.join(A, "models", "item", "wanted_poster.json"), {"parent": "item/generated", "textures": {"layer0": T("wanted_0")}})
    # botines y recetas
    for n in ("saloon_door", "player_piano", "painted_letter", "wanted_poster"):
        dm.loot(n)
    dm.recipe("saloon_door", ["S S", "PSP", "S S"], {"S": "minecraft:stick", "P": "minecraft:spruce_planks"}, 2)
    dm.recipe("player_piano", ["PPP", "WNW", "P P"], {"P": "minecraft:dark_oak_planks", "W": "minecraft:white_wool", "N": "minecraft:note_block"})
    dm.recipe("painted_letter", [" D ", "PPP"], {"D": "minecraft:red_dye", "P": "minecraft:birch_planks"}, 6)
    dm.recipe("wanted_poster", ["P", "I"], {"P": "minecraft:paper", "I": "minecraft:ink_sac"}, 2)


# ================================================================== pieles

def cowboy(i, tone, hair, shirt, vest, pants, scarf, kind="man", beard=False, plaid=None, star=False, apron=False, garter=False, visor=False):
    """Vaquero o vaquera del saloon: camisa (lisa o de cuadros), chaleco, pañuelo, pantalón con cinturón y botas."""
    s = ps.Skin(300 + i)
    sk = ps.SKIN[tone]
    hc = ps.HAIR[hair]
    woman = kind == "woman"

    def head(face, x, y, w, h):
        if face == "top":
            return hc
        if face == "bottom":
            return ps.shade(sk, 0.85)
        if face == "back":
            return hc if (woman or y < 6) else sk
        if face in ("right", "left"):
            if woman:
                return hc if y < 8 and not (y >= 3 and ((face == "right" and x >= 5) or (face == "left" and x <= 2))) else sk
            return hc if y < 2 else (ps.shade(hc, 0.9) if beard and y >= 5 else sk)
        if y == 0:
            return hc
        if y == 1:
            return hc if (woman and x in (0, 1, 6, 7)) or (not woman and x not in (2, 3, 4, 5)) else sk
        if woman and x in (0, 7) and y < 7:
            return hc
        if y == 3 and x in (1, 2, 5, 6):
            return ps.shade(hc, 0.7)
        if y == 4:
            if x in (1, 6):
                return (240, 240, 240)
            if x in (2, 5):
                return (60, 80, 120) if i % 2 else (70, 45, 25)
        if y == 5 and x in (3, 4):
            return ps.shade(sk, 0.86)
        if not woman and (beard or i % 3 == 0) and y == 5 and x in (2, 3, 4, 5):
            return ps.shade(hc, 0.85)                 # bigote
        if beard and y >= 6:
            return ps.shade(hc, 0.85)
        if y == 6 and x in (3, 4):
            return (176, 60, 70) if woman else ps.shade(sk, 0.78)
        return sk

    s.cube(0, 0, 8, 8, 8, head)

    def shirt_px(x, y):
        if plaid:
            return plaid[0] if (x // 2 + y // 2) % 2 == 0 else plaid[1]
        return shirt

    def torso(face, x, y, w, h):
        if face == "front":
            if y == 0 and scarf and 2 <= x <= 5:
                return scarf
            if y == 1 and scarf and 3 <= x <= 4:
                return scarf
            if woman:
                if y < 2 and 2 <= x <= 5:
                    return sk
                if y in (4, 5, 6):
                    return vest if vest else ps.shade(shirt, 0.8)      # corsé
                return shirt_px(x, y)
            if apron and y >= 6:
                return (232, 228, 214)
            if y == 9:
                return (54, 34, 20) if not (3 <= x <= 4) else (196, 160, 60)   # cinturón con hebilla
            if vest and (x <= 2 or x >= 5) and y < 9:
                if star and x == 2 and y in (3, 4):
                    return (232, 196, 70)
                return vest
            if garter and y == 2 and x in (0, 7):
                return (150, 30, 30)
            return shirt_px(x, y)
        if face == "top":
            return sk if woman and 2 <= x <= 5 else (vest or shirt)
        if face == "back":
            return vest if vest and y < 9 else (54, 34, 20) if y == 9 else shirt_px(x, y)
        return vest if vest and y < 9 and not woman else shirt_px(x, y)

    s.cube(16, 16, 8, 12, 4, torso)

    def arm(face, x, y, w, h):
        if face == "bottom":
            return sk
        if y >= 10:
            return sk
        if garter and y == 3:
            return (150, 30, 30)                       # liguilla del brazo (crupier, cantinero)
        if woman and y < 1:
            return shirt
        if y == 9:
            return ps.shade(shirt_px(x, y), 0.8)
        return shirt_px(x, y)

    s.cube(40, 16, 4, 12, 4, arm)
    s.cube(32, 48, 4, 12, 4, arm)

    def leg(face, x, y, w, h):
        if face == "bottom":
            return (40, 26, 18)
        if woman:
            if y < 9:
                return pants if face != "back" else ps.shade(pants, 0.9)
            return (70, 44, 26)
        if y >= 8:
            return (78, 48, 26) if y < 11 else (52, 32, 18)       # botas
        return pants

    s.cube(0, 16, 4, 12, 4, leg)
    s.cube(16, 48, 4, 12, 4, leg)
    if visor:
        pass
    return s.im


WEST_PEOPLE = [
    # tono, pelo, camisa, chaleco, pantalón, pañuelo, extra
    dict(tone=1, hair="brown", shirt=(160, 120, 80), vest=(70, 46, 30), pants=(58, 70, 96), scarf=(170, 40, 34)),
    dict(tone=0, hair="blonde", shirt=None, plaid=((170, 40, 40), (60, 30, 30)), vest=None, pants=(64, 56, 48), scarf=None),
    dict(tone=3, hair="black", shirt=(40, 36, 34), vest=(28, 26, 24), pants=(44, 42, 40), scarf=(30, 30, 30), beard=True),
    dict(tone=2, hair="grey", shirt=(196, 186, 160), vest=(110, 86, 60), pants=(90, 72, 52), scarf=(50, 70, 120), beard=True),
    dict(tone=0, hair="red", shirt=(214, 200, 176), vest=(150, 40, 40), pants=(70, 54, 38), scarf=None, kind="woman"),
    dict(tone=1, hair="black", shirt=(232, 230, 222), vest=(60, 20, 26), pants=(30, 30, 34), scarf=(20, 20, 22), garter=True),
    dict(tone=2, hair="brown", shirt=(150, 30, 60), vest=(30, 20, 20), pants=(150, 30, 60), scarf=None, kind="woman"),
    dict(tone=4, hair="grey", shirt=(120, 100, 72), vest=None, pants=(80, 64, 44), scarf=(140, 90, 40), beard=True, plaid=((120, 100, 72), (96, 78, 54))),
]


def skins(preview=None):
    out = []
    for i, p in enumerate(WEST_PEOPLE):
        p = dict(p)
        shirt = p.pop("shirt") or (0, 0, 0)
        im = cowboy(i, p.pop("tone"), p.pop("hair"), shirt, p.pop("vest"), p.pop("pants"), p.pop("scarf"), **p)
        im.save(os.path.join(ENT, f"dealer_west_patron_{i}.png"))
        out.append(im)
    staff = {
        "sheriff": cowboy(20, 2, "brown", (186, 160, 120), (92, 62, 40), (70, 62, 52), (120, 30, 30), star=True, beard=False),
        "bartender": cowboy(21, 0, "black", (236, 234, 226), (40, 24, 20), (34, 32, 30), (20, 20, 20), apron=True, garter=True),
        "dealer": cowboy(22, 1, "brown", (234, 230, 218), (30, 70, 44), (40, 36, 34), (20, 20, 22), garter=True),
        "host": cowboy(23, 3, "black", (230, 224, 210), (120, 20, 30), (30, 26, 26), (196, 160, 60), garter=True),
    }
    for k, im in staff.items():
        im.save(os.path.join(ENT, f"dealer_west_{k}.png"))
        out.append(im)
    if preview:
        ps.preview(out, os.path.join(preview, "west_skins.png"))


if __name__ == "__main__":
    textures()
    models()
    pv = sys.argv[sys.argv.index("--preview") + 1] if "--preview" in sys.argv else None
    skins(pv)
    print("listo")
