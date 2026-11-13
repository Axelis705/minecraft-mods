#!/usr/bin/env python3
"""Modelos y estados de la decoración brillante (ronda 2): letras de neón, letreros animados, pantallas, candelabro y postes con
cordón de terciopelo.

    python3 casino/herramientas/neon_tex.py
    python3 casino/herramientas/neon_models.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import decor_models as dm  # noqa: E402
from decor_models import box, rot90, write, A, D  # noqa: E402
from neon_tex import CHARS, CHAR_NAMES, NEON, SIGNS, SCREENS  # noqa: E402

COLORS = list(NEON.keys())
COLOR_ES = {"red": "rojo", "pink": "rosa", "gold": "dorado", "green": "verde", "cyan": "celeste", "white": "blanco"}
DIRS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
GLOW = {"forge_data": {"block_light": 15, "sky_light": 15}, "light_emission": 15}


def glowing(model_path):
    """Las caras con #glow se ven con luz propia (Forge 1.20.1-1.21.1: forge_data; 1.21.2+: light_emission)."""
    with open(model_path, encoding="utf-8") as fh:
        m = json.load(fh)
    for el in m["elements"]:
        if any(f["texture"] in ("#glow", "#crystal", "#bulb") for f in el["faces"].values()):
            el.update(GLOW)
    with open(model_path, "w", encoding="utf-8") as fh:
        json.dump(m, fh, indent=1)


def base(name, els, textures, particle):
    dm.model(name, els, textures, particle)
    glowing(os.path.join(A, "models", "block", "decor", name + ".json"))


def gen():
    dm.TEX.update({"crystal": "casino:block/decor/crystal", "bulb": "casino:block/decor/bulb"})
    # --------------------------------------------------------------------------------------- panel de neón (letras y letreros)
    panel = [box([0, 0, 14], [16, 16, 16], "lac", faces=["south", "west", "east", "up", "down"]),
             box([0, 0, 14], [16, 16, 14.01], "#glow", faces=["north"], uv="full")]
    base("neon_panel", panel, {"glow": "casino:block/decor/neon/red_a"}, "casino:block/decor/lacquer")
    # pantalla: marco más grueso con la imagen un poco hundida
    screen = [box([0, 0, 13], [16, 16, 16], "lac", faces=["south", "west", "east", "up", "down"]),
              box([0, 0, 13], [16, 1, 13.01], "lac", faces=["north"]), box([0, 15, 13], [16, 16, 13.01], "lac", faces=["north"]),
              box([0, 1, 13], [1, 15, 13.01], "lac", faces=["north"]), box([15, 1, 13], [16, 15, 13.01], "lac", faces=["north"]),
              box([1, 1, 13.4], [15, 15, 13.41], "#glow", faces=["north"], uv="full"),
              box([1, 1, 13], [15, 1.2, 13.4], "gold", faces=["north", "up"]), box([1, 14.8, 13], [15, 15, 13.4], "gold", faces=["north", "down"]),
              box([1, 1.2, 13], [1.2, 14.8, 13.4], "gold", faces=["north", "east"]), box([14.8, 1.2, 13], [15, 14.8, 13.4], "gold", faces=["north", "west"])]
    base("wall_screen_base", screen, {"glow": "casino:block/decor/screen_jackpot"}, "casino:block/decor/lacquer")

    # letras: un modelo por letra y color
    variants = {}
    for ci, c in enumerate(COLORS):
        for i, ch in enumerate(CHARS):
            nm = f"neon_{c}_{CHAR_NAMES.get(ch, ch.lower())}"
            dm.child(nm, "neon_panel", {"glow": f"casino:block/decor/neon/{c}_{CHAR_NAMES.get(ch, ch.lower())}"})
            for f, y in DIRS:
                variants[f"color={ci},facing={f},glyph={i}"] = {"model": "casino:block/decor/" + nm, "y": y}
    write(os.path.join(A, "blockstates", "neon_letter.json"), {"variants": variants})
    dm.item("neon_letter", "neon_red_a", dm.DISPLAY)
    dm.names("neon_letter", "Letra de neón", "Neon Letter")
    dm.loot("neon_letter")
    dm.recipe("neon_letter", ["GGG", "GRG", "PPP"], {"G": "minecraft:glass_pane", "R": "minecraft:glowstone_dust", "P": "minecraft:blackstone_slab"}, 4)

    # letreros con dibujo
    variants = {}
    for i, s in enumerate(SIGNS):
        dm.child(f"neon_sign_{s}", "neon_panel", {"glow": f"casino:block/decor/sign_{s}"})
        for f, y in DIRS:
            variants[f"design={i},facing={f}"] = {"model": f"casino:block/decor/neon_sign_{s}", "y": y}
    write(os.path.join(A, "blockstates", "neon_sign.json"), {"variants": variants})
    dm.item("neon_sign", "neon_sign_cherry", dm.DISPLAY)
    dm.names("neon_sign", "Letrero de neón", "Neon Sign")
    dm.loot("neon_sign")
    dm.recipe("neon_sign", ["GRG", "RDR", "PPP"], {"G": "minecraft:glass_pane", "R": "minecraft:glowstone_dust", "D": "minecraft:redstone",
                                                    "P": "minecraft:blackstone_slab"}, 2)

    # pantallas
    variants = {}
    for i, s in enumerate(SCREENS):
        dm.child(f"wall_screen_{s}", "wall_screen_base", {"glow": f"casino:block/decor/screen_{s}"})
        for f, y in DIRS:
            variants[f"design={i},facing={f}"] = {"model": f"casino:block/decor/wall_screen_{s}", "y": y}
    write(os.path.join(A, "blockstates", "wall_screen.json"), {"variants": variants})
    dm.item("wall_screen", "wall_screen_jackpot", dm.DISPLAY)
    dm.names("wall_screen", "Pantalla de casino", "Casino Screen")
    dm.loot("wall_screen")
    dm.recipe("wall_screen", ["BBB", "BGB", "BRB"], {"B": "minecraft:blackstone", "G": "minecraft:glass_pane", "R": "minecraft:redstone"})

    # --------------------------------------------------------------------------------------- candelabro
    e = [box([6, 15.4, 6], [10, 16, 10], "gold"),
         box([7.6, 12, 7.6], [8.4, 15.4, 8.4], "gold"),
         box([5.5, 11.4, 5.5], [10.5, 12, 10.5], "gold")]
    e += dm.octagon([5, 8.6, 5], [11, 11.4, 11], "crystal")
    e += dm.octagon([4.4, 8.1, 4.4], [11.6, 8.6, 11.6], "gold")
    e += [box([6, 6, 6], [10, 8.1, 10], "crystal"), box([7, 4.4, 7], [9, 6, 9], "crystal"),
          box([7.6, 3.6, 7.6], [8.4, 4.4, 8.4], "gold")]
    # cuatro brazos con focos y tiras de cristal colgando
    arm = [box([1.4, 8.6, 7.6], [4.4, 9.1, 8.4], "gold"),
           box([0.8, 9.1, 7.1], [2.8, 9.6, 8.9], "gold"),
           box([1.2, 9.6, 7.5], [2.4, 11.6, 8.5], "bulb"),
           box([1.5, 5.6, 7.75], [2.1, 9.1, 8.25], "crystal"),
           box([3.3, 6.4, 7.75], [3.9, 8.6, 8.25], "crystal"),
           box([1.4, 5.0, 7.65], [2.2, 5.6, 8.35], "crystal")]
    for k in range(4):
        e += rot90(arm, k)
    drops = []
    for x, z in ((4.6, 4.6), (10.8, 4.6), (4.6, 10.8), (10.8, 10.8)):
        drops += [box([x, 6.0, z], [x + 0.6, 8.1, z + 0.6], "crystal"), box([x - 0.1, 5.4, z - 0.1], [x + 0.7, 6.0, z + 0.7], "crystal")]
    e += drops
    base("chandelier", e, {}, "casino:block/decor/gold")
    write(os.path.join(A, "blockstates", "chandelier.json"), {"variants": {"": {"model": "casino:block/decor/chandelier"}}})
    dm.item("chandelier", "chandelier", dm.DISPLAY)
    dm.names("chandelier", "Candelabro de cristal", "Crystal Chandelier")
    dm.loot("chandelier")
    dm.recipe("chandelier", [" C ", "GLG", "QQQ"], {"C": "minecraft:iron_bars", "G": "minecraft:gold_ingot", "L": "minecraft:glowstone",
                                                    "Q": "minecraft:glass_pane"})

    # --------------------------------------------------------------------------------------- poste con cordón
    post = []
    post += dm.octagon([3.5, 0, 3.5], [12.5, 0.8, 12.5], "brass")
    post += dm.octagon([5, 0.8, 5], [11, 1.8, 11], "brass")
    post += [box([6.6, 1.8, 6.6], [9.4, 2.6, 9.4], "gold"),
             box([7, 2.6, 7], [9, 12.6, 9], "brass"),                       # poste
             box([6.7, 5.4, 6.7], [9.3, 6.0, 9.3], "gold"),
             box([6.6, 9.2, 6.6], [9.4, 10.8, 9.4], "gold"),                 # anillo del cordón
             box([6.8, 12.6, 6.8], [9.2, 13.2, 9.2], "gold")]
    post += dm.octagon([6.2, 13.2, 6.2], [9.8, 15.4, 9.8], "gold")          # remate en bola
    post += [box([7.5, 15.4, 7.5], [8.5, 16.2, 8.5], "gold")]
    dm.model("rope_post", post, particle="casino:block/decor/brass")
    rope = [box([7.2, 8.4, 0], [8.8, 9.8, 2.2], "#v"),
            box([7.2, 8.7, 2.2], [8.8, 10.1, 4.0], "#v"),
            box([7.2, 9.1, 4.0], [8.8, 10.5, 5.6], "#v"),
            box([7.2, 9.5, 5.6], [8.8, 10.9, 6.6], "#v"),
            box([7.0, 9.3, 6.6], [9.0, 10.7, 7.0], "gold")]
    dm.model("rope_side", rope, particle="#v")
    # cordón en diagonal (hacia el noreste): el mismo cordón estirado √2 y girado 45° alrededor del poste
    k = 2 ** 0.5
    diag = []
    for e in rope:
        (x1, y1, z1), (x2, y2, z2) = e["from"], e["to"]
        diag.append(box([x1, y1, 8 - (8 - z1) * k], [x2, y2, 8 - (8 - z2) * k], e["tex"], rot=("y", -45, [8, 9, 8])))
    dm.model("rope_diag", diag, particle="#v")
    for c in dm.COLORS:
        tx = {"v": f"casino:block/decor/velvet_{c}"}
        dm.child(f"rope_side_{c}", "rope_side", tx)
        dm.child(f"rope_diag_{c}", "rope_diag", tx)
        mp = [{"apply": {"model": "casino:block/decor/rope_post"}}]
        for f, y in DIRS:
            mp.append({"when": {f: "true"}, "apply": {"model": f"casino:block/decor/rope_side_{c}", "y": y}})
        for d, y in (("ne", 0), ("se", 90), ("sw", 180), ("nw", 270)):
            mp.append({"when": {d: "true"}, "apply": {"model": f"casino:block/decor/rope_diag_{c}", "y": y}})
        name = f"rope_post_{c}"
        write(os.path.join(A, "blockstates", name + ".json"), {"multipart": mp})
        # objeto: poste con dos cordones (para que se vea el color)
        both = post + rope + rot90(rope, 2)
        dm.model(f"rope_post_item_{c}", both, textures=tx, particle="casino:block/decor/brass")
        dm.item(name, f"rope_post_item_{c}", dm.DISPLAY)
        dm.names(name, f"Poste con cordón {dm.COLOR_ES[c]}", f"{dm.COLOR_EN[c]} Rope Post")
        dm.loot(name)
        dm.recipe(name, [" G ", "WIW", " G "], {"G": "minecraft:gold_nugget", "W": dm.WOOL[c], "I": "minecraft:iron_ingot"}, 4)

    dm.LANG_ES["casino.neon.title"] = "Letra de neón"
    dm.LANG_EN["casino.neon.title"] = "Neon Letter"
    dm.LANG_ES["casino.neon.help"] = "Elige una letra y un color. Escribe con el teclado para llenar varias letras seguidas."
    dm.LANG_EN["casino.neon.help"] = "Pick a letter and a color. Type on your keyboard to fill several letters in a row."
    dm.LANG_ES["casino.neon.color"] = "Color"
    dm.LANG_EN["casino.neon.color"] = "Color"
    dm.LANG_ES["casino.neon.done"] = "Listo"
    dm.LANG_EN["casino.neon.done"] = "Done"
    for k, v in dm.LANG_ES.items():
        pass
    for lang, extra in (("es_es", dm.LANG_ES), ("en_us", dm.LANG_EN)):
        p = os.path.join(A, "lang", lang + ".json")
        with open(p, encoding="utf-8") as fh:
            data = json.load(fh)
        data.update(extra)
        with open(p, "w", encoding="utf-8") as fh:
            json.dump(data, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
    print("neón, pantallas, candelabro y postes listos")


if __name__ == "__main__":
    gen()

# sin parpadeo: separa caras encimadas de todos los modelos (ver zfix.py)
import zfix as _zf, os as _os
_zf.run(_os.path.join(_os.path.dirname(_os.path.abspath(__file__)), "..", "src/main/resources/assets/casino/models"))
