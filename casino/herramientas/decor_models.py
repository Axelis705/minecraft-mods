#!/usr/bin/env python3
"""Modelos, estados, objetos, botines, recetas y textos de la decoración del casino (ronda 1: asientos y bar).

    python3 casino/herramientas/decor_tex.py      (texturas)
    python3 casino/herramientas/decor_models.py   (todo lo demás)

Los modelos se arman con cajas en código: así se pueden girar, recortar y unir (esquinas de la barra) sin dibujarlos a mano.
"""
import copy
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
A = os.path.join(ROOT, "assets", "casino")
D = os.path.join(ROOT, "data", "casino")

COLORS = ["red", "black", "purple", "blue"]
COLOR_ES = {"red": "rojo", "black": "negro", "purple": "morado", "blue": "azul"}
COLOR_EN = {"red": "Red", "black": "Black", "purple": "Purple", "blue": "Blue"}
WOOL = {"red": "minecraft:red_wool", "black": "minecraft:black_wool", "purple": "minecraft:purple_wool", "blue": "minecraft:blue_wool"}

TEX = {
    "gold": "casino:block/decor/gold", "chrome": "casino:block/decor/chrome", "brass": "casino:block/decor/brass",
    "lac": "casino:block/decor/lacquer", "mah": "casino:block/decor/mahogany", "mahp": "casino:block/decor/mahogany_panel",
    "mar": "casino:block/decor/marble", "marb": "casino:block/decor/marble_black", "glass": "casino:block/decor/glass",
    "b_green": "casino:block/decor/bottle_green", "b_amber": "casino:block/decor/bottle_amber", "b_clear": "casino:block/decor/bottle_clear",
    "b_red": "casino:block/decor/bottle_red", "b_blue": "casino:block/decor/bottle_blue",
    "whiskey": "casino:block/decor/drink_whiskey", "cocktail": "casino:block/decor/drink_cocktail",
    "champagne": "casino:block/decor/drink_champagne", "bluedrink": "casino:block/decor/drink_blue", "garnish": "casino:block/decor/garnish",
    "vred_t": "casino:block/decor/velvet_red_tufted", "vblack": "casino:block/decor/velvet_black", "bulb": "casino:block/decor/bulb",
}
FACES = ["north", "south", "west", "east", "up", "down"]


# ------------------------------------------------------------------------------------------------ cajas
def box(f, t, tex, faces=None, rot=None, uv=None):
    """Una caja. tex = clave de textura (o '#var'). faces = caras a dibujar (todas por defecto). rot = (eje, ángulo, origen).
    uv = None (automático por posición) o {cara: [u1,v1,u2,v2]} o 'full'."""
    return {"from": list(f), "to": list(t), "tex": tex, "faces": list(faces or FACES), "rot": rot, "uv": uv}


def octagon(f, t, tex, faces=None):
    """Forma casi redonda sin girar cajas: una cruz ancha + un cuadrado con las esquinas recortadas.
    Cada pieza queda un poquito más baja que la anterior para que las caras de arriba no parpadeen."""
    (x1, y1, z1), (x2, y2, z2) = f, t
    w, d = x2 - x1, z2 - z1
    out = []
    for i, (fx, fz) in enumerate(((1.0, 0.62), (0.62, 1.0), (0.86, 0.86))):
        ix, iz = w * (1 - fx) / 2, d * (1 - fz) / 2
        e = 0.02 * i
        out.append(box([x1 + ix, y1 + e, z1 + iz], [x2 - ix, y2 - e, z2 - iz], tex, faces))
    return out


def auto_uv(face, a, b):
    x1, y1, z1 = a
    x2, y2, z2 = b
    uv = {"down": [x1, 16 - z2, x2, 16 - z1], "up": [x1, z1, x2, z2], "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1],
          "south": [x1, 16 - y2, x2, 16 - y1], "west": [z1, 16 - y2, z2, 16 - y1], "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1]}[face]
    out = []
    for lo, hi in ((uv[0], uv[2]), (uv[1], uv[3])):
        size = hi - lo
        if size > 16:
            lo, hi = 0, 16
        else:
            while lo < 0:
                lo, hi = lo + 16, hi + 16
            while hi > 16:
                lo, hi = lo - 16, hi - 16
            if lo < 0:
                lo, hi = 0, size
        out += [round(lo, 4), round(hi, 4)]
    return [out[0], out[2], out[1], out[3]]


def rot90(els, k):
    """Gira las cajas k*90° en el sentido del reloj visto desde arriba (como "y": 90 en un blockstate)."""
    out = []
    for e in els:
        e = copy.deepcopy(e)
        for _ in range(k % 4):
            (x1, y1, z1), (x2, y2, z2) = e["from"], e["to"]
            e["from"], e["to"] = [16 - z2, y1, x1], [16 - z1, y2, x2]
            m = {"north": "east", "east": "south", "south": "west", "west": "north", "up": "up", "down": "down"}
            e["faces"] = [m[f] for f in e["faces"]]
            if isinstance(e["uv"], dict):
                e["uv"] = {m[f]: v for f, v in e["uv"].items()}
            if e["rot"]:
                ax, ang, o = e["rot"]
                o = [16 - o[2], o[1], o[0]]
                if ax == "x":
                    ax = "z"
                elif ax == "z":
                    ax, ang = "x", -ang
                e["rot"] = (ax, ang, o)
        out.append(e)
    return out


def mirror_x(els):
    out = []
    for e in els:
        e = copy.deepcopy(e)
        (x1, y1, z1), (x2, y2, z2) = e["from"], e["to"]
        e["from"], e["to"] = [16 - x2, y1, z1], [16 - x1, y2, z2]
        m = {"west": "east", "east": "west"}
        e["faces"] = [m.get(f, f) for f in e["faces"]]
        if isinstance(e["uv"], dict):
            e["uv"] = {m.get(f, f): v for f, v in e["uv"].items()}
        if e["rot"]:
            ax, ang, o = e["rot"]
            e["rot"] = (ax, ang if ax == "x" else -ang, [16 - o[0], o[1], o[2]])
        out.append(e)
    return out


def clip(els, x=(-99, 99), z=(-99, 99)):
    """Se queda con la parte de cada caja dentro del rectángulo (en x y z). Las cajas giradas solo entran si su centro está dentro."""
    out = []
    for e in els:
        (x1, y1, z1), (x2, y2, z2) = e["from"], e["to"]
        if e["rot"]:
            cx, cz = (x1 + x2) / 2, (z1 + z2) / 2
            if x[0] <= cx <= x[1] and z[0] <= cz <= z[1]:
                out.append(copy.deepcopy(e))
            continue
        nx1, nx2 = max(x1, x[0]), min(x2, x[1])
        nz1, nz2 = max(z1, z[0]), min(z2, z[1])
        if nx2 - nx1 <= 1e-6 or nz2 - nz1 <= 1e-6:
            continue
        e = copy.deepcopy(e)
        e["from"], e["to"] = [nx1, y1, nz1], [nx2, y2, nz2]
        out.append(e)
    return out


def emit(els, textures, particle, ao=True):
    used = {}
    elements = []
    for e in els:
        el = {"from": [round(v, 4) for v in e["from"]], "to": [round(v, 4) for v in e["to"]]}
        if e["rot"]:
            ax, ang, o = e["rot"]
            el["rotation"] = {"angle": ang, "axis": ax, "origin": [round(v, 4) for v in o]}
        faces = {}
        for f in e["faces"]:
            t = e["tex"]
            var = t[1:] if t.startswith("#") else t
            if not t.startswith("#"):
                used[var] = TEX[var]
            fd = {"texture": "#" + var}
            if e["uv"] == "full":
                fd["uv"] = [0, 0, 16, 16]
            elif isinstance(e["uv"], dict) and f in e["uv"]:
                fd["uv"] = e["uv"][f]
            else:
                fd["uv"] = auto_uv(f, el["from"], el["to"])
            faces[f] = fd
        el["faces"] = faces
        elements.append(el)
    tex = dict(used)
    # las plantillas de terciopelo traen rojo por defecto (cada color las reemplaza con su modelo hijo)
    for e in els:
        if e["tex"] in ("#v", "#vt"):
            tex.setdefault("v", "casino:block/decor/velvet_red")
            tex.setdefault("vt", "casino:block/decor/velvet_red_tufted")
    tex.update(textures)
    tex["particle"] = particle
    m = {"textures": tex, "elements": elements}
    if not ao:
        m["ambientocclusion"] = False
    return m


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(obj, fh, indent=1)


def model(name, els, textures=None, particle="casino:block/decor/gold"):
    write(os.path.join(A, "models", "block", "decor", name + ".json"), emit(els, textures or {}, particle))


def child(name, parent, textures):
    write(os.path.join(A, "models", "block", "decor", name + ".json"),
          {"parent": "casino:block/decor/" + parent, "textures": textures})


DISPLAY_TALL = {"gui": {"rotation": [30, 225, 0], "translation": [0, -1.5, 0], "scale": [0.5, 0.5, 0.5]},
                "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                "fixed": {"scale": [0.5, 0.5, 0.5]},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]},
                "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.4, 0.4, 0.4]}}
DISPLAY = {"gui": {"rotation": [30, 225, 0], "scale": [0.625, 0.625, 0.625]},
           "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
           "fixed": {"scale": [0.5, 0.5, 0.5]},
           "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
           "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]},
           "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.4, 0.4, 0.4]}}
DISPLAY_SMALL = {"gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [1.1, 1.1, 1.1]},
                 "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                 "fixed": {"scale": [1, 1, 1]},
                 "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.6, 0.6, 0.6]},
                 "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.7, 0.7, 0.7]},
                 "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 3, 0], "scale": [0.7, 0.7, 0.7]}}


def item(name, parent, display):
    write(os.path.join(A, "models", "item", name + ".json"), {"parent": "casino:block/decor/" + parent, "display": display})


def facing_states(name, mdl, extra=None):
    v = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        if extra:
            for key, m in extra(mdl):
                v[f"facing={f},{key}"] = {"model": "casino:block/decor/" + m, "y": y}
        else:
            v[f"facing={f}"] = {"model": "casino:block/decor/" + mdl, "y": y}
    write(os.path.join(A, "blockstates", name + ".json"), {"variants": v})


def single_state(name, mdl):
    write(os.path.join(A, "blockstates", name + ".json"), {"variants": {"": {"model": "casino:block/decor/" + mdl}}})


def loot(name):
    write(os.path.join(D, "loot_tables", "blocks", name + ".json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "casino:" + name}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def recipe(name, pattern, key, count=1):
    write(os.path.join(D, "recipes", name + ".json"), {
        "type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern,
        "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in key.items()},
        "result": {"item": "casino:" + name, "count": count}})


LANG_ES = {}
LANG_EN = {}


def names(block, es, en):
    LANG_ES["block.casino." + block] = es
    LANG_EN["block.casino." + block] = en


# ================================================================================================ SILLA DE CASINO
def chair():
    e = []
    e += octagon([4, 0, 4], [12, 1, 12], "gold")
    e += [box([5.5, 1, 5.5], [10.5, 1.6, 10.5], "gold")]
    e += [box([7, 1.6, 7], [9, 6, 9], "chrome")]
    e += [box([6.5, 5.5, 6.5], [9.5, 6.2, 9.5], "gold")]
    # aro para los pies
    e += [box([4, 3, 2.5], [12, 3.6, 3.3], "gold"),
          box([7.6, 3.05, 3.3], [8.4, 3.55, 7], "chrome")]
    # asiento
    e += [box([2, 6.2, 2], [14, 7.2, 14], "lac"),
          box([1.8, 6.6, 1.8], [14.2, 7, 14.2], "gold", faces=["north", "south", "west", "east"]),
          box([2.4, 7.2, 2.2], [13.6, 9.2, 13.2], "#v"),
          box([3, 9.2, 2.8], [13, 9.6, 12.6], "#v", faces=["up", "north", "west", "east"])]
    # respaldo alto
    e += [box([2, 7.2, 13.2], [14, 21, 15], "lac"),
          box([2.8, 9.2, 12.4], [13.2, 20.2, 13.2], "#vt", faces=["north", "west", "east", "up"]),
          box([2, 21, 13.1], [14, 21.8, 15.1], "gold"),
          box([3.5, 21.8, 13.4], [12.5, 22.5, 14.8], "gold"),
          box([6.5, 22.5, 13.7], [9.5, 23.6, 14.5], "gold"),
          box([1.8, 7.2, 12.9], [2.2, 21, 13.3], "gold"),
          box([13.8, 7.2, 12.9], [14.2, 21, 13.3], "gold")]
    # descansabrazos
    for x1, x2 in ((1.2, 2.8), (13.2, 14.8)):
        e += [box([x1 + 0.3, 7.2, 4], [x2 - 0.3, 10.2, 5], "lac"),
              box([x1, 10.2, 3.5], [x2, 11.2, 13.2], "lac"),
              box([x1 - 0.1, 11.2, 3.8], [x2 + 0.1, 11.9, 13], "#v"),
              box([x1 - 0.05, 10.2, 3.4], [x2 + 0.05, 11.3, 3.6], "gold", faces=["north", "west", "east", "up"])]
    model("casino_chair", e, particle="#v")


def stool():
    e = []
    e += octagon([4, 0, 4], [12, 0.8, 12], "gold")
    e += [box([5.5, 0.8, 5.5], [10.5, 1.3, 10.5], "gold"),
          box([7.25, 1.3, 7.25], [8.75, 10, 8.75], "chrome")]
    # aro de pies
    e += [box([4, 4, 4], [12, 4.6, 4.7], "gold"), box([4, 4, 11.3], [12, 4.6, 12], "gold"),
          box([4, 4, 4.7], [4.7, 4.6, 11.3], "gold"), box([11.3, 4, 4.7], [12, 4.6, 11.3], "gold"),
          box([4.7, 4.1, 7.65], [11.3, 4.5, 8.35], "chrome"), box([7.65, 4.1, 4.7], [8.35, 4.5, 11.3], "chrome")]
    e += [box([6.8, 9.6, 6.8], [9.2, 10.2, 9.2], "gold")]
    e += octagon([3.6, 10.2, 3.6], [12.4, 10.8, 12.4], "gold")
    e += octagon([3.8, 10.8, 3.8], [12.2, 13, 12.2], "#v")
    e += octagon([4.6, 13, 4.6], [11.4, 13.6, 11.4], "#vt", faces=["up", "north", "south", "west", "east"])
    model("bar_stool", e, particle="#v")


def table(name, top):
    """Mesa de cóctel art déco: base escalonada con 4 patas en cruz, columna con anillos, cubierta con canto dorado y lamparita."""
    e = []
    e += octagon([3, 0, 3], [13, 0.8, 13], "gold")
    e += [box([5, 0.8, 5], [11, 1.6, 11], "lac")]
    for k in range(4):
        e += rot90([box([7.2, 0, 1.2], [8.8, 1.2, 4.2], "gold")], k)            # patas en cruz
    # la cubierta llega justo al borde del bloque (y=16): lo que se ponga encima (bebidas, macetas, velas) queda apoyado, no flotando
    e += [box([7, 1.6, 7], [9, 13.6, 9], "gold"),
          box([6.4, 3.4, 6.4], [9.6, 4.2, 9.6], "chrome"),
          box([6.2, 6.6, 6.2], [9.8, 7.6, 9.8], "lac"),
          box([6.4, 7.6, 6.4], [9.6, 8.2, 9.6], "chrome"),
          box([6.6, 12.6, 6.6], [9.4, 13.6, 9.4], "chrome")]
    e += octagon([5, 13.6, 5], [11, 14.4, 11], "gold")
    e += octagon([0.6, 14.4, 0.6], [15.4, 15.1, 15.4], "gold")
    e += octagon([1, 15.1, 1], [15, 16, 15], top)
    model(name, e, particle=TEX[top])


def sofa():
    base = [box([0, 2, 2], [16, 5, 16], "mah"),
            box([0, 2, 1.8], [16, 2.5, 2], "gold", faces=["north", "up", "down"]),
            box([0.05, 5, 1.5], [7.95, 8.5, 12.5], "#v"),
            box([8.05, 5, 1.5], [15.95, 8.5, 12.5], "#v"),
            box([0, 4.6, 1.3], [16, 5.1, 1.6], "gold", faces=["north", "up", "down"]),
            box([0, 5, 12.5], [16, 15.5, 15.5], "#vt"),
            box([0, 5, 15.5], [16, 15, 16], "mah"),
            box([0, 15, 12], [16, 17, 16], "#v")]
    for x in (0.6, 13.4):
        for z in (2.6, 13.4):
            base.append(box([x, 0, z], [x + 2, 2, z + 2], "gold"))
    model("sofa_base", base, particle="#v")
    arm = [box([0, 5, 1], [3, 11.5, 16], "#v"),
           box([-0.2, 11.5, 0.5], [3.2, 13, 15.8], "#v"),
           box([0.4, 6, 0.8], [2.6, 11, 1], "gold", faces=["north"]),
           box([-0.25, 11.6, 0.4], [3.25, 12.9, 0.5], "gold", faces=["north"])]
    model("sofa_arm_left", arm, particle="#v")
    model("sofa_arm_right", mirror_x(arm), particle="#v")


def counter():
    """Barra de bar: cuerpo de caoba, frente acolchado capitoné con molduras doradas, cubierta de mármol que sobresale hacia
    los clientes con un borde acolchado negro (el apoyabrazos de las barras), y reposapiés de latón sobre soportes."""
    s = [box([0, 0, 4], [16, 14, 16], "mah"),
         box([0, 0, 3.4], [16, 1.8, 4], "lac"),                                   # zócalo
         box([0, 1.8, 3.4], [16, 13, 4], "vred_t", uv="full"),                     # frente acolchado
         box([0, 1.8, 3.2], [0.8, 13, 3.4], "gold"), box([15.2, 1.8, 3.2], [16, 13, 3.4], "gold"),
         box([0, 12.6, 3.0], [16, 13.4, 4], "gold"),                               # moldura bajo la cubierta
         box([0, 13.4, 0.8], [16, 14.6, 16], "mar"),                               # cubierta de mármol (sobresale)
         box([0, 13.0, 0.2], [16, 15.4, 1.8], "vblack"),                           # borde acolchado (apoyabrazos)
         box([0, 14.6, 13.5], [16, 15.0, 16], "gold"),                             # canal de bebidas del lado del barman
         box([0, 2.6, 0.6], [16, 3.4, 1.4], "brass"),                              # reposapiés
         box([3, 2.4, 1.4], [4, 3.6, 3.4], "brass"),
         box([12, 2.4, 1.4], [13, 3.6, 3.4], "brass")]
    model("bar_counter_straight", s, particle=TEX["mah"])
    sw = rot90(s, 3)  # frente al oeste
    # esquina hacia afuera: frentes norte y oeste, cuerpo en x>=3, z>=3
    outer = clip(s, x=(3, 16)) + clip(sw, x=(0, 3), z=(3, 16)) + [
        box([0.2, 13, 0.2], [1.8, 15.4, 1.8], "vblack"),
        box([0, 0, 2.4], [3, 1.5, 3], "lac"), box([2.4, 0, 0], [3, 1.5, 2.4], "lac"),
        box([0.6, 2.6, 0.6], [3, 3.4, 1.4], "brass")]
    model("bar_counter_outer_left", outer, particle=TEX["mah"])
    model("bar_counter_outer_right", mirror_x(outer), particle=TEX["mah"])
    # esquina hacia adentro: frentes norte y oeste, cuerpo en todo menos x<3, z<3
    inner = clip(s, z=(3, 16)) + clip(sw, x=(3, 16), z=(0, 3)) + clip(s, x=(0, 3), z=(1.5, 3)) + clip(sw, x=(1.5, 3), z=(0, 1.5)) \
        + clip(s, x=(0, 1.5), z=(0, 1.5))
    model("bar_counter_inner_left", inner, particle=TEX["mah"])
    model("bar_counter_inner_right", mirror_x(inner), particle=TEX["mah"])


BOTTLES = ["b_green", "b_amber", "b_clear", "b_red", "b_blue"]


def bottle(x, y, z, kind, tall):
    h = 4.5 if tall else 3.6
    w = 1.8
    return [box([x, y, z], [x + w, y + h, z + w], kind, faces=["north", "south", "west", "east", "down"], uv="full"),
            box([x, y + h, z], [x + w, y + h + 0.01, z + w], kind, faces=["up"], uv={"up": [2, 2, 6, 6]}),
            box([x + 0.5, y + h, z + 0.5], [x + w - 0.5, y + h + 1.4, z + w - 0.5], kind, uv={f: [0, 0, 4, 4] for f in FACES}),
            box([x + 0.45, y + h + 1.4, z + 0.45], [x + w - 0.45, y + h + 1.8, z + w - 0.45], "gold")]


def shelf():
    e = [box([0, 0, 15], [16, 16, 16], "mahp"),
         box([0, 0, 9], [1, 16, 15], "mah"), box([15, 0, 9], [16, 16, 15], "mah"),
         box([1, 0, 9], [15, 1, 15], "marb"), box([1, 7.5, 9], [15, 8.5, 15], "marb"),
         box([0, 15, 8.5], [16, 16, 15], "mah"),
         box([0, 15.2, 8.3], [16, 15.7, 8.5], "gold", faces=["north"]),
         box([1, 1, 8.8], [15, 1.6, 9], "gold", faces=["north", "up"]),
         box([1, 8.5, 8.8], [15, 9.1, 9], "gold", faces=["north", "up"])]
    xs = [1.6, 4.3, 7.0, 9.7, 12.4]
    for i, x in enumerate(xs):
        e += bottle(x, 1, 10.6 + (i % 2) * 1.4, BOTTLES[i], i % 2 == 0)
    for i, x in enumerate(xs):
        e += bottle(x, 8.5, 10.6 + ((i + 1) % 2) * 1.4, BOTTLES[(i + 2) % 5], i % 2 == 1)
    model("bottle_shelf", e, particle=TEX["mah"])


def rim(x1, z1, x2, z2, y, t=0.15):
    return [box([x1, y, z1], [x2, y + 0.3, z1 + t], "glass"), box([x1, y, z2 - t], [x2, y + 0.3, z2], "glass"),
            box([x1, y, z1 + t], [x1 + t, y + 0.3, z2 - t], "glass"), box([x2 - t, y, z1 + t], [x2, y + 0.3, z2 - t], "glass")]


def drinks():
    martini = [box([3, 0, 3], [5.4, 0.3, 5.4], "glass"), box([4, 0.3, 4], [4.4, 2.6, 4.4], "glass"),
               box([3.4, 2.6, 3.4], [5, 3.2, 5], "glass"),
               box([2.8, 3.2, 2.8], [5.6, 3.9, 5.6], "cocktail")] + rim(2.65, 2.65, 5.75, 5.75, 3.85) + \
              [box([3.95, 3.9, 3.95], [4.45, 4.4, 4.45], "garnish", uv={f: [1, 1, 3, 3] for f in FACES})]
    whiskey = [box([9, 0, 3], [11.6, 0.4, 5.6], "glass"),
               box([9.1, 0.4, 3.1], [11.5, 1.8, 5.5], "whiskey"),
               box([9.1, 1.8, 3.1], [11.5, 2.4, 5.5], "glass", faces=["north", "south", "west", "east"]),
               box([9.7, 1.5, 3.8], [10.6, 2.3, 4.7], "glass")] + rim(9, 3, 11.6, 5.6, 2.4)
    flute = [box([3.2, 0, 10.2], [4.8, 0.25, 11.8], "glass"), box([3.85, 0.25, 10.85], [4.15, 2, 11.15], "glass"),
             box([3.4, 2, 10.4], [4.6, 4.6, 11.6], "champagne")] + rim(3.3, 10.3, 4.7, 11.7, 4.6)
    highball = [box([9.5, 0, 10], [11.5, 0.4, 12], "glass"),
                box([9.6, 0.4, 10.1], [11.4, 3.6, 11.9], "bluedrink")] + rim(9.45, 9.95, 11.55, 12.05, 3.6) + \
               [box([10.6, 3.0, 10.6], [10.9, 5.4, 10.9], "garnish", uv={f: [0, 0, 1, 4] for f in FACES})]
    sets = [martini, martini + whiskey, martini + whiskey + flute, martini + whiskey + flute + highball]
    for i, s in enumerate(sets, 1):
        model(f"drinks_{i}", s, particle=TEX["glass"])


# ================================================================================================ registro de bloques
def gen():
    chair()
    stool()
    table("cocktail_table_model", "mar")
    table("cocktail_table_black_model", "marb")
    sofa()
    counter()
    shelf()
    drinks()

    for c in COLORS:
        tx = {"v": f"casino:block/decor/velvet_{c}", "vt": f"casino:block/decor/velvet_{c}_tufted"}
        for kind, parent, es, en in (("casino_chair", "casino_chair", "Silla de casino", "Casino Chair"),
                                     ("bar_stool", "bar_stool", "Taburete de bar", "Bar Stool"),
                                     ("sofa", None, "Sofá de terciopelo", "Velvet Sofa")):
            name = f"{kind}_{c}"
            if parent:
                child(name, parent, tx)
                facing_states(name, name)
                item(name, name, DISPLAY_TALL if kind == "casino_chair" else DISPLAY)
            else:
                for part in ("sofa_base", "sofa_arm_left", "sofa_arm_right"):
                    child(f"{part}_{c}", part, tx)
                mp = []
                for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                    mp.append({"when": {"facing": f}, "apply": {"model": f"casino:block/decor/sofa_base_{c}", "y": y}})
                    mp.append({"when": {"facing": f, "left": "true"}, "apply": {"model": f"casino:block/decor/sofa_arm_left_{c}", "y": y}})
                    mp.append({"when": {"facing": f, "right": "true"}, "apply": {"model": f"casino:block/decor/sofa_arm_right_{c}", "y": y}})
                write(os.path.join(A, "blockstates", name + ".json"), {"multipart": mp})
                item_els = emit(sofa_full(), tx, tx["v"])
                write(os.path.join(A, "models", "block", "decor", f"sofa_single_{c}.json"), item_els)
                item(name, f"sofa_single_{c}", DISPLAY)
            names(name, f"{es} {COLOR_ES[c]}", f"{COLOR_EN[c]} {en}")
            loot(name)
        wool = WOOL[c]
        recipe(f"casino_chair_{c}", ["  W", "WWW", "GIG"], {"W": wool, "G": "minecraft:gold_ingot", "I": "minecraft:iron_ingot"}, 2)
        recipe(f"bar_stool_{c}", ["WWW", " I ", "GGG"], {"W": wool, "G": "minecraft:gold_nugget", "I": "minecraft:iron_ingot"}, 2)
        recipe(f"sofa_{c}", ["W  ", "WWW", "PGP"], {"W": wool, "G": "minecraft:gold_ingot", "P": "minecraft:dark_oak_planks"}, 2)

    for name, mdl, es, en, r, disp in (
            ("cocktail_table", "cocktail_table_model", "Mesa de cóctel de mármol", "Marble Cocktail Table",
             (["QQQ", " G ", " G "], {"Q": "minecraft:quartz_block", "G": "minecraft:gold_ingot"}), DISPLAY),
            ("cocktail_table_black", "cocktail_table_black_model", "Mesa de cóctel de mármol negro", "Black Marble Cocktail Table",
             (["QQQ", " G ", " G "], {"Q": "minecraft:polished_blackstone", "G": "minecraft:gold_ingot"}), DISPLAY)):
        single_state(name, mdl)
        item(name, mdl, disp)
        names(name, es, en)
        loot(name)
        recipe(name, *r)

    # barra
    shapes = {"straight": "bar_counter_straight", "outer_left": "bar_counter_outer_left", "outer_right": "bar_counter_outer_right",
              "inner_left": "bar_counter_inner_left", "inner_right": "bar_counter_inner_right"}
    facing_states("bar_counter", None, extra=lambda _: [(f"shape={k}", v) for k, v in shapes.items()])
    item("bar_counter", "bar_counter_straight", DISPLAY)
    names("bar_counter", "Barra de bar", "Bar Counter")
    loot("bar_counter")
    recipe("bar_counter", ["QQQ", "PGP", "PPP"], {"Q": "minecraft:quartz_slab", "G": "minecraft:gold_ingot", "P": "minecraft:dark_oak_planks"}, 3)

    facing_states("bottle_shelf", "bottle_shelf")
    item("bottle_shelf", "bottle_shelf", DISPLAY)
    names("bottle_shelf", "Estante con botellas", "Bottle Shelf")
    loot("bottle_shelf")
    recipe("bottle_shelf", ["PPP", "BBB", "PPP"], {"P": "minecraft:dark_oak_planks", "B": "minecraft:glass_bottle"})

    facing_states("drinks", None, extra=lambda _: [(f"drinks={i}", f"drinks_{i}") for i in range(1, 5)])
    item("drinks", "drinks_1", DISPLAY_SMALL)
    names("drinks", "Bebidas", "Drinks")
    # el botín de las bebidas devuelve una por cada vaso
    write(os.path.join(D, "loot_tables", "blocks", "drinks.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "casino:drinks", "functions": [
            {"function": "minecraft:set_count", "count": float(i), "add": False,
             "conditions": [{"condition": "minecraft:block_state_property", "block": "casino:drinks", "properties": {"drinks": str(i)}}]}
            for i in range(2, 5)] + [{"function": "minecraft:explosion_decay"}]}]}]})
    write(os.path.join(D, "recipes", "drinks.json"), {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:glass_bottle"}, {"item": "minecraft:sweet_berries"}, {"item": "minecraft:sugar"}],
        "result": {"item": "casino:drinks", "count": 2}})

    LANG_ES["casino.decor.seat_taken"] = "Ese asiento está ocupado."
    LANG_EN["casino.decor.seat_taken"] = "That seat is taken."
    LANG_ES["itemGroup.casino.decor"] = "Casino: decoración"
    LANG_EN["itemGroup.casino.decor"] = "Casino: Decoration"
    for lang, extra in (("es_es", LANG_ES), ("en_us", LANG_EN)):
        p = os.path.join(A, "lang", lang + ".json")
        with open(p, encoding="utf-8") as fh:
            data = json.load(fh)
        data.update(extra)
        with open(p, "w", encoding="utf-8") as fh:
            json.dump(data, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
    print("decoración lista")


def sofa_full():
    # se reconstruye leyendo las piezas ya hechas (base + dos brazos)
    out = []
    for part in ("sofa_base", "sofa_arm_left", "sofa_arm_right"):
        with open(os.path.join(A, "models", "block", "decor", part + ".json"), encoding="utf-8") as fh:
            m = json.load(fh)
        for el in m["elements"]:
            faces = list(el["faces"].keys())
            t = list(el["faces"].values())[0]["texture"]
            tex = t if t[1:] in ("v", "vt") else t[1:]
            rot = None
            if "rotation" in el:
                r = el["rotation"]
                rot = (r["axis"], r["angle"], r["origin"])
            out.append(box(el["from"], el["to"], ("#" + tex[1:]) if tex.startswith("#") else tex, faces, rot,
                           uv={f: fd["uv"] for f, fd in el["faces"].items()}))
    return out


if __name__ == "__main__":
    gen()

# sin parpadeo: separa caras encimadas de todos los modelos (ver zfix.py)
import zfix as _zf, os as _os
_zf.run(_os.path.join(_os.path.dirname(_os.path.abspath(__file__)), "..", "src/main/resources/assets/casino/models"))
