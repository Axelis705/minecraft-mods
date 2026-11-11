#!/usr/bin/env python3
"""Genera texturas, modelos, estados de bloque, idiomas, recetas, botines y etiquetas del mod Autos de Coleccion.
Uso: python3 gen_recursos.py   (necesita Pillow)"""
import json, os, colorsys
from PIL import Image, ImageDraw

MOD = "autos_coleccion"
ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A = os.path.join(ROOT, "assets", MOD)
DATA = os.path.join(ROOT, "data", MOD)

BOX_IDS = ["caja_ruta_urbana", "caja_pista_nocturna", "caja_leyendas"]
BOX_NAMES = ["Ruta Urbana", "Pista Nocturna", "Leyendas del Circuito"]
BOX_COL = [0xF2A93B, 0x8A3FFC, 0xE8C15A]
BOX_COL2 = [0x2D6CDF, 0x16F2E0, 0x1A1A1A]
CARS = ["brisa_compacto", "taxi_sol", "ranchera", "aurora_familiar", "mini_rayo", "bolita_68", "combi_viajera", "patrulla",
        "helados_paletin", "lowrider_dorado", "kaze_r", "neon_sprint", "drift_kitsune", "tormenta_gt", "mamba_negra", "shogun_rs",
        "rally_lince", "aurora_turbo", "fantasma_nocturno", "dragon_turbo_x", "falcone_rosso", "vortex_v12", "imperial_gt",
        "cometa_plata", "hyperion_e", "resistencia_24", "monoplaza_astra", "solaris_concept", "titan_hiper", "corona_oro"]
CAR_NAMES = ["Brisa Compacto", "Taxi Ciudad Sol", "Pickup Ranchera", "Aurora Familiar", "Mini Rayo", "Bolita '68", "Combi Viajera",
             "Patrulla Urbana", "Helados Paletín", "Lowrider Dorado", "Kaze R", "Neón Sprint", "Drift Kitsune", "Tormenta GT",
             "Mamba Negra", "Shogun RS", "Rally Lince", "Aurora Boreal Turbo", "Fantasma Nocturno", "Dragón Turbo X", "Falcone Rosso",
             "Vortex V12", "Imperial GT", "Cometa Plata", "Hyperion E", "Prototipo Resistencia 24", "Monoplaza Astra",
             "Solaris Concept", "Titán Hiper", "Corona de Oro"]
SOUNDS = ["coin", "machine_start", "count", "go", "perfect", "good", "miss", "rev", "shift", "nitro", "note", "key", "error",
          "tally", "result", "box_drop", "box_shake", "tape", "flaps", "reveal_common", "reveal_rare", "reveal_epic", "reveal_legend",
          "sparkle", "place", "garage", "whoosh", "new_car", "engine", "bump", "skid"]
SUB_ES = {"coin": "Moneda", "machine_start": "Máquina se enciende", "count": "Cuenta regresiva", "go": "¡Ya!", "perfect": "¡Perfecto!",
          "good": "Bien", "miss": "Fallo", "rev": "Motor acelera", "shift": "Cambio de marcha", "nitro": "Nitro", "note": "Nota",
          "key": "Tecla", "error": "Error", "tally": "Puntos", "result": "Resultado", "box_drop": "Cae una caja", "box_shake": "Caja se sacude",
          "tape": "Cinta se rompe", "flaps": "Caja se abre", "reveal_common": "Auto revelado", "reveal_rare": "Auto raro revelado",
          "reveal_epic": "Auto épico revelado", "reveal_legend": "¡Auto legendario!", "sparkle": "Destello", "place": "Auto en vitrina",
          "garage": "Garaje", "whoosh": "Zumbido", "new_car": "¡Auto nuevo!", "engine": "Motor de auto RC", "bump": "Choque", "skid": "Derrape"}
SUB_EN = {"coin": "Coin", "machine_start": "Machine powers up", "count": "Countdown", "go": "Go!", "perfect": "Perfect!", "good": "Good",
          "miss": "Miss", "rev": "Engine revs", "shift": "Gear shift", "nitro": "Nitro", "note": "Note", "key": "Key", "error": "Error",
          "tally": "Points", "result": "Result", "box_drop": "Box drops", "box_shake": "Box shakes", "tape": "Tape rips", "flaps": "Box opens",
          "reveal_common": "Car revealed", "reveal_rare": "Rare car revealed", "reveal_epic": "Epic car revealed",
          "reveal_legend": "Legendary car!", "sparkle": "Sparkle", "place": "Car displayed", "garage": "Garage", "whoosh": "Whoosh",
          "new_car": "New car!", "engine": "RC car engine", "bump": "Crash", "skid": "Skid"}


def wjson(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def shade(c, k):
    r, g, b = rgb(c) if isinstance(c, int) else c
    return (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)))


def tex(name, img):
    p = os.path.join(A, "textures", name + ".png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def noise_fill(img, base, amt=6, seed=1):
    import random
    r = random.Random(seed)
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            c = base(x, y) if callable(base) else base
            d = r.randint(-amt, amt)
            px[x, y] = tuple(max(0, min(255, v + d)) for v in c) + (255,)


# ------------------------------------------------------------------------------------------------------- texturas

def textures():
    tex("misc/white", Image.new("RGBA", (16, 16), (255, 255, 255, 255)))
    RED = (196, 32, 44)
    # cuerpo de la máquina: rojo brillante con reflejo vertical y canto oscuro
    im = Image.new("RGBA", (32, 32))
    noise_fill(im, lambda x, y: shade(RED, 1.25 if 5 <= x <= 7 else 1.1 if 4 <= x <= 9 else 0.95 - 0.004 * y), 4, 2)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 31, 0], fill=(120, 18, 26, 255))
    d.rectangle([0, 31, 31, 31], fill=(90, 14, 20, 255))
    d.line([(26, 2), (26, 29)], fill=(235, 235, 240, 255))
    d.line([(27, 2), (27, 29)], fill=(150, 150, 160, 255))
    tex("block/maquina_cuerpo", im)
    # frente de abajo: bandeja, botones y franja
    im = Image.new("RGBA", (32, 32))
    noise_fill(im, lambda x, y: shade(RED, 1.05 - 0.004 * y), 4, 3)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 31, 3], fill=(245, 245, 248, 255))
    d.rectangle([0, 4, 31, 4], fill=(40, 40, 48, 255))
    for i in range(5):
        d.rectangle([2 + i * 3, 7, 3 + i * 3, 8], fill=(255, 210, 74, 255) if i % 2 == 0 else (60, 224, 255, 255))
    d.rounded_rectangle([5, 19, 26, 28], 2, fill=(22, 22, 28, 255), outline=(200, 200, 210, 255))
    d.rectangle([7, 21, 24, 23], fill=(10, 10, 14, 255))
    d.text((9, 12), "PUSH", fill=(255, 255, 255, 255))
    tex("block/maquina_frente", im)
    # panel de la moneda (metal con ranura y botones)
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, lambda x, y: (150 + (16 - y) * 3, 155 + (16 - y) * 3, 165 + (16 - y) * 3), 5, 4)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline=(70, 72, 80, 255))
    d.rectangle([3, 3, 12, 5], fill=(15, 15, 18, 255))
    for i, c in enumerate([(230, 60, 60), (60, 200, 90), (60, 140, 255)]):
        d.ellipse([2 + i * 4, 9, 4 + i * 4, 11], fill=c + (255,))
    tex("block/maquina_panel", im)
    # interior de la vitrina de la máquina: oscuro con luces
    im = Image.new("RGBA", (32, 32))
    noise_fill(im, lambda x, y: (18 + y // 4, 16 + y // 5, 34 + y // 3), 3, 5)
    d = ImageDraw.Draw(im)
    for x in range(2, 32, 6):
        d.line([(x, 0), (x, 31)], fill=(40, 30, 70, 255))
    d.rectangle([0, 0, 31, 1], fill=(255, 240, 210, 255))
    tex("block/maquina_interior", im)
    # letrero (fondo)
    im = Image.new("RGBA", (32, 16))
    noise_fill(im, (20, 16, 40), 3, 6)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 31, 15], outline=(255, 210, 74, 255))
    d.rectangle([1, 1, 30, 14], outline=(255, 79, 160, 255))
    tex("block/maquina_letrero", im)
    # vitrina
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, lambda x, y: (28, 24, 26) if y not in (2, 13) else (200, 160, 60), 3, 7)
    tex("block/vitrina_base", im)
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, (60, 62, 70), 4, 8)
    tex("block/vitrina_metal", im)
    im = Image.new("RGBA", (16, 16), (210, 235, 255, 40))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline=(230, 245, 255, 110))
    d.line([(3, 12), (10, 5)], fill=(255, 255, 255, 120))
    d.line([(5, 13), (12, 6)], fill=(255, 255, 255, 70))
    tex("block/vitrina_vidrio", im)
    # cajas (frente, lado y tapa)
    for b in range(3):
        c1, c2 = rgb(BOX_COL[b]), rgb(BOX_COL2[b])
        im = Image.new("RGBA", (32, 24))
        noise_fill(im, c1, 5, 10 + b)
        d = ImageDraw.Draw(im)
        d.polygon([(0, 22), (10, 22), (31, 6), (31, 0), (21, 0)], fill=c2 + (255,))
        d.rectangle([0, 22, 31, 23], fill=shade(c2, 0.7) + (255,))
        d.rectangle([2, 7, 17, 17], fill=(27, 36, 51, 255), outline=(240, 240, 240, 255))
        d.polygon([(4, 14), (7, 11), (13, 11), (15, 14)], fill=(90, 112, 145, 255))
        d.rectangle([20, 2, 30, 5], fill=(246, 242, 232, 255))
        for s in range(b + 1):
            d.rectangle([21 + s * 3, 3, 22 + s * 3, 4], fill=c1 + (255,))
        tex(f"item/{BOX_IDS[b]}_frente", im)
        im = Image.new("RGBA", (16, 24))
        noise_fill(im, shade(c1, 0.92), 5, 20 + b)
        d = ImageDraw.Draw(im)
        d.rectangle([0, 22, 15, 23], fill=shade(c2, 0.7) + (255,))
        d.rectangle([0, 10, 15, 13], fill=c2 + (255,))
        tex(f"item/{BOX_IDS[b]}_lado", im)
        im = Image.new("RGBA", (32, 16))
        noise_fill(im, c1, 5, 30 + b)
        d = ImageDraw.Draw(im)
        d.line([(0, 8), (31, 8)], fill=shade(c1, 0.6) + (255,))
        d.rectangle([0, 6, 31, 10], fill=(242, 230, 198, 255))
        d.line([(0, 8), (31, 8)], fill=(200, 185, 150, 255))
        tex(f"item/{BOX_IDS[b]}_tapa", im)
    # garaje (libro con un auto)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([2, 1, 13, 14], fill=(30, 40, 90, 255), outline=(12, 16, 40, 255))
    d.rectangle([3, 2, 12, 13], fill=(40, 56, 120, 255))
    d.rectangle([13, 2, 14, 13], fill=(235, 230, 215, 255))
    d.rectangle([4, 3, 11, 4], fill=(255, 200, 60, 255))
    d.polygon([(4, 10), (5, 8), (7, 7), (10, 7), (11, 9), (11, 10)], fill=(225, 40, 50, 255))
    d.rectangle([5, 10, 6, 11], fill=(20, 20, 20, 255))
    d.rectangle([9, 10, 10, 11], fill=(20, 20, 20, 255))
    d.rectangle([7, 8, 9, 8], fill=(150, 210, 255, 255))
    tex("item/garaje", im)
    im = Image.new("RGBA", (16, 16))
    noise_fill(im, (180, 40, 50), 10, 40)
    tex("item/auto_particula", im)


# ------------------------------------------------------------------------------------------------------- modelos

def el(f, t, faces, rot=None):
    e = {"from": f, "to": t, "faces": faces}
    if rot:
        e["rotation"] = rot
    return e


def allf(texv, uv=None, skip=()):
    out = {}
    for d in ["north", "south", "east", "west", "up", "down"]:
        if d in skip:
            continue
        out[d] = {"texture": texv}
        if uv:
            out[d]["uv"] = uv
    return out


def machine_elements():
    lower = [
        el([0.5, 0, 1.5], [15.5, 1, 15.5], allf("#body")),
        el([1, 1, 2], [15, 16, 15], {**allf("#body", skip=("north",)), "north": {"texture": "#front", "uv": [0, 0, 16, 15]}}),
        el([8.5, 8, 1.5], [13.5, 13, 2], {"north": {"texture": "#panel", "uv": [0, 0, 16, 16]}, "east": {"texture": "#panel"}, "west": {"texture": "#panel"},
                                          "up": {"texture": "#panel"}, "down": {"texture": "#panel"}}),
        el([3, 1.5, 1.2], [13, 2.3, 2], allf("#panel")),
    ]
    upper = [
        el([1, 0, 14], [15, 16, 15], {**allf("#body", skip=("north",)), "north": {"texture": "#inside", "uv": [0, 0, 16, 16]}}),
        el([1, 0, 1.5], [2.8, 16, 14], allf("#body")),
        el([13.2, 0, 1.5], [15, 16, 14], allf("#body")),
        el([2.8, 0, 1.5], [13.2, 0.6, 14], {**allf("#inside", skip=("down",)), "down": {"texture": "#body"}}),
        el([1, 13.2, 0.8], [15, 16, 14], {**allf("#body", skip=("north",)), "north": {"texture": "#sign", "uv": [0, 0, 16, 16]}}),
    ]
    return lower, upper


def models():
    tx = {"body": f"{MOD}:block/maquina_cuerpo", "front": f"{MOD}:block/maquina_frente", "panel": f"{MOD}:block/maquina_panel",
          "inside": f"{MOD}:block/maquina_interior", "sign": f"{MOD}:block/maquina_letrero", "particle": f"{MOD}:block/maquina_cuerpo"}
    lower, upper = machine_elements()
    wjson(f"{A}/models/block/maquina_abajo.json", {"parent": "block/block", "textures": tx, "elements": lower})
    wjson(f"{A}/models/block/maquina_arriba.json", {"parent": "block/block", "textures": tx, "elements": upper})
    # objeto: las dos mitades juntas (la de arriba subida 16)
    both = json.loads(json.dumps(lower))
    for e in json.loads(json.dumps(upper)):
        e["from"][1] += 16
        e["to"][1] += 16
        both.append(e)
    for e in both:
        e["from"][1] = e["from"][1] / 2 + 0
        e["to"][1] = e["to"][1] / 2 + 0
        e["from"][0] = 4 + e["from"][0] / 2
        e["to"][0] = 4 + e["to"][0] / 2
        e["from"][2] = 4 + e["from"][2] / 2
        e["to"][2] = 4 + e["to"][2] / 2
    wjson(f"{A}/models/item/maquina_expendedora.json", {
        "parent": "block/block", "textures": tx, "elements": both,
        "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [1.05, 1.05, 1.05]},
                    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
                    "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]}}})
    vt = {"base": f"{MOD}:block/vitrina_base", "metal": f"{MOD}:block/vitrina_metal", "glass": f"{MOD}:block/vitrina_vidrio",
          "particle": f"{MOD}:block/vitrina_base"}
    els = [el([1, 0, 1], [15, 3, 15], allf("#base")), el([1, 15, 1], [15, 16, 15], allf("#base"))]
    for x in (1, 14.2):
        for z in (1, 14.2):
            els.append(el([x, 3, z], [x + 0.8, 15, z + 0.8], allf("#metal")))
    els += [el([1.8, 3, 1.2], [14.2, 15, 1.4], {"north": {"texture": "#glass"}, "south": {"texture": "#glass"}}),
            el([1.8, 3, 14.6], [14.2, 15, 14.8], {"north": {"texture": "#glass"}, "south": {"texture": "#glass"}}),
            el([1.2, 3, 1.8], [1.4, 15, 14.2], {"east": {"texture": "#glass"}, "west": {"texture": "#glass"}}),
            el([14.6, 3, 1.8], [14.8, 15, 14.2], {"east": {"texture": "#glass"}, "west": {"texture": "#glass"}})]
    wjson(f"{A}/models/block/vitrina.json", {"parent": "block/block", "render_type": "minecraft:translucent", "textures": vt, "elements": els})
    wjson(f"{A}/models/item/vitrina.json", {"parent": f"{MOD}:block/vitrina"})
    # cajas
    for b, bid in enumerate(BOX_IDS):
        t = {"front": f"{MOD}:item/{bid}_frente", "side": f"{MOD}:item/{bid}_lado", "top": f"{MOD}:item/{bid}_tapa",
             "particle": f"{MOD}:item/{bid}_frente"}
        wjson(f"{A}/models/item/{bid}.json", {
            "parent": "block/block", "textures": t,
            "elements": [el([2, 0, 4.5], [14, 9, 11.5], {
                "north": {"texture": "#front", "uv": [0, 0, 16, 16]}, "south": {"texture": "#front", "uv": [0, 0, 16, 16]}, "east": {"texture": "#side", "uv": [0, 0, 16, 16]},
                "west": {"texture": "#side", "uv": [0, 0, 16, 16]}, "up": {"texture": "#top", "uv": [0, 0, 16, 16]}, "down": {"texture": "#side", "uv": [0, 0, 16, 16]}})],
            "display": {"gui": {"rotation": [25, -35, 0], "translation": [0, 1, 0], "scale": [0.95, 0.95, 0.95]},
                        "ground": {"translation": [0, 3, 0], "scale": [0.45, 0.45, 0.45]},
                        "fixed": {"rotation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
                        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.4, 0.4, 0.4]},
                        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
                        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]}}})
    wjson(f"{A}/models/item/garaje.json", {"parent": "item/generated", "textures": {"layer0": f"{MOD}:item/garaje"}})
    car_display = {"gui": {"rotation": [28, -140, 0], "translation": [0, 3.5, 0], "scale": [1.1, 1.1, 1.1]},
                   "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
                   "fixed": {"rotation": [0, 90, 0], "translation": [0, 4, 0], "scale": [1, 1, 1]},
                   "head": {"translation": [0, 14, 0], "scale": [1, 1, 1]},
                   "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.45, 0.45, 0.45]},
                   "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.45, 0.45, 0.45]},
                   "firstperson_righthand": {"rotation": [0, -135, 0], "translation": [0, 3, 0], "scale": [0.55, 0.55, 0.55]},
                   "firstperson_lefthand": {"rotation": [0, -135, 0], "translation": [0, 3, 0], "scale": [0.55, 0.55, 0.55]}}
    for c in CARS:
        wjson(f"{A}/models/item/auto_{c}.json", {"parent": "builtin/entity", "gui_light": "front",
                                                   "textures": {"particle": f"{MOD}:item/auto_particula"}, "display": car_display})


def blockstates():
    v = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for half, m in (("lower", "maquina_abajo"), ("upper", "maquina_arriba")):
            e = {"model": f"{MOD}:block/{m}"}
            if y:
                e["y"] = y
            v[f"facing={f},half={half}"] = e
    wjson(f"{A}/blockstates/maquina_expendedora.json", {"variants": v})
    v = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        e = {"model": f"{MOD}:block/vitrina"}
        if y:
            e["y"] = y
        v[f"facing={f}"] = e
    wjson(f"{A}/blockstates/vitrina.json", {"variants": v})


def lang():
    for code, es in (("es_es", True), ("es_mx", True), ("en_us", False)):
        L = {
            "itemGroup.autos_coleccion": "Autos de Colección" if es else "Collector Cars",
            f"block.{MOD}.maquina_expendedora": "Máquina expendedora de autos" if es else "Car Vending Machine",
            f"block.{MOD}.vitrina": "Vitrina de colección" if es else "Display Case",
            f"item.{MOD}.garaje": "Garaje de colección" if es else "Collection Garage",
        }
        for b in range(3):
            L[f"item.{MOD}.{BOX_IDS[b]}"] = ("Caja " + BOX_NAMES[b]) if es else (BOX_NAMES[b] + " Box")
        for c, n in zip(CARS, CAR_NAMES):
            L[f"item.{MOD}.auto_{c}"] = n
        for s in SOUNDS:
            L[f"subtitles.{MOD}.{s}"] = SUB_ES[s] if es else SUB_EN[s]
        L[f"entity.{MOD}.auto_rc"] = "Auto a control remoto" if es else "RC car"
        wjson(f"{A}/lang/{code}.json", L)


def sounds():
    wjson(f"{A}/sounds.json", {s: {"sounds": [f"{MOD}:{s}"], "subtitle": f"subtitles.{MOD}.{s}"} for s in SOUNDS})


def data():
    wjson(f"{DATA}/recipes/maquina_expendedora.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["IGI", "CDC", "IRI"],
        "key": {"I": {"item": "minecraft:iron_ingot"}, "G": {"item": "minecraft:glass"}, "C": {"item": "minecraft:red_dye"},
                "D": {"item": "minecraft:dispenser"}, "R": {"item": "minecraft:redstone_block"}},
        "result": {"item": f"{MOD}:maquina_expendedora"}})
    wjson(f"{DATA}/recipes/vitrina.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["GLG", "G G", "SSS"],
        "key": {"G": {"item": "minecraft:glass"}, "L": {"item": "minecraft:glowstone_dust"}, "S": {"item": "minecraft:dark_oak_slab"}},
        "result": {"item": f"{MOD}:vitrina", "count": 2}})
    wjson(f"{DATA}/recipes/garaje.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:book"}, {"item": "minecraft:gold_nugget"}, {"item": "minecraft:red_dye"}],
        "result": {"item": f"{MOD}:garaje"}})
    wjson(f"{DATA}/loot_tables/blocks/maquina_expendedora.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:maquina_expendedora"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"},
                                  {"condition": "minecraft:block_state_property", "block": f"{MOD}:maquina_expendedora",
                                   "properties": {"half": "lower"}}]}]})
    wjson(f"{DATA}/loot_tables/blocks/vitrina.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:vitrina"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    wjson(os.path.join(ROOT, "data/minecraft/tags/blocks/mineable/pickaxe.json"),
          {"replace": False, "values": [f"{MOD}:maquina_expendedora", f"{MOD}:vitrina"]})


if __name__ == "__main__":
    textures()
    models()
    blockstates()
    lang()
    sounds()
    data()
    print("recursos listos")
