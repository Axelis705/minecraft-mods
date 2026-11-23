"""Recursos de la pesca hechos con código: el corcho (modelo, animaciones, textura), el hilo y las cañas (dibujos y modelos).

Uso (desde la carpeta del mod): python3 herramientas/recursos.py
"""
import json
import math
import os

from PIL import Image

MOD = "pesca_coleccion"
BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", MOD)


def hexc(s, a=255):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def shade(c, k):
    return mix(c, (255, 255, 255, c[3]), k) if k > 0 else mix(c, (0, 0, 0, c[3]), -k)


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(obj, fh, indent=2, ensure_ascii=False)


# ---------------------------------------------------------------- corcho

def box_uv(img, u, v, w, h, d, top, side, bottom, stripe=None):
    """Pinta una caja con UV de Blockbench (caja): arriba, abajo y los 4 lados (con un poco de sombra por cara)."""
    p = img.load()

    def rect(x0, y0, ww, hh, c, k=0.0):
        for yy in range(hh):
            for xx in range(ww):
                cc = shade(c, k)
                if stripe and stripe(yy, hh):
                    cc = shade(stripe(yy, hh), k)
                p[x0 + xx, y0 + yy] = cc
    rect(u + d, v, w, d, top, 0.15)
    rect(u + d + w, v, w, d, bottom, -0.2)
    rect(u, v + d, d, h, side, -0.1)
    rect(u + d, v + d, w, h, side, 0.05)
    rect(u + d + w, v + d, d, h, side, -0.15)
    rect(u + 2 * d + w, v + d, w, h, side, 0.0)


def corcho():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    red, white, gray = hexc("#D23A2E"), hexc("#F2F0EA"), hexc("#7C828C")
    # parte de abajo blanca (3x2x3), arriba roja (3x2x3), antena (1x3x1), anzuelo (1x2x1)
    box_uv(img, 0, 0, 3, 2, 3, white, white, white)
    box_uv(img, 0, 8, 3, 2, 3, red, red, red)
    box_uv(img, 16, 0, 1, 3, 1, white, white, white, stripe=lambda y, h: red if y < 1 else None)
    box_uv(img, 16, 8, 1, 2, 1, gray, gray, gray)
    os.makedirs(os.path.join(BASE, "textures", "entity"), exist_ok=True)
    img.save(os.path.join(BASE, "textures", "entity", "anzuelo.png"))
    hilo = Image.new("RGBA", (4, 4), (255, 255, 255, 255))
    hilo.save(os.path.join(BASE, "textures", "entity", "hilo.png"))
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.pesca_coleccion.anzuelo", "texture_width": 32, "texture_height": 32},
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "flotador", "parent": "root", "pivot": [0, 1, 0], "cubes": [
                {"origin": [-1.5, 0, -1.5], "size": [3, 2, 3], "uv": [0, 0]},
                {"origin": [-1.5, 2, -1.5], "size": [3, 2, 3], "uv": [0, 8]},
                {"origin": [-0.5, 4, -0.5], "size": [1, 3, 1], "uv": [16, 0]},
                {"origin": [-0.5, -2, -0.5], "size": [1, 2, 1], "uv": [16, 8]}]}]}]}
    write_json(os.path.join(BASE, "geo", "anzuelo.geo.json"), geo)

    def kf(pairs):
        return {f"{t:.2f}": {"vector": v, "easing": "easeInOutSine"} for t, v in pairs}
    anims = {"format_version": "1.8.0", "animations": {
        "animation.anzuelo.flota": {"loop": True, "animation_length": 2.4, "bones": {"flotador": {
            "position": {"0.0": [0, 0, 0], "1.2": {"vector": [0, -0.6, 0], "easing": "easeInOutSine"}, "2.4": {"vector": [0, 0, 0], "easing": "easeInOutSine"}},
            "rotation": ["math.sin(q.anim_time * 150) * 4", 0, "math.cos(q.anim_time * 110) * 3"]}}},
        "animation.anzuelo.vuela": {"loop": True, "animation_length": 1.0, "bones": {"flotador": {
            "rotation": [0, "q.anim_time * 360", 0]}}},
        "animation.anzuelo.piquete": {"animation_length": 0.3, "bones": {"flotador": {
            "position": kf([(0.0, [0, 0, 0]), (0.07, [0, -1.6, 0]), (0.3, [0, 0, 0])]),
            "rotation": kf([(0.0, [0, 0, 0]), (0.07, [12, 0, 0]), (0.3, [0, 0, 0])])}}},
        "animation.anzuelo.pica": {"animation_length": 0.8, "bones": {"flotador": {
            "position": kf([(0.0, [0, 0, 0]), (0.06, [0, -4.5, 0]), (0.5, [0, -4, 0]), (0.8, [0, -1, 0])]),
            "rotation": kf([(0.0, [0, 0, 0]), (0.06, [25, 0, 10]), (0.3, [-15, 0, -10]), (0.5, [20, 0, 8]), (0.8, [0, 0, 0])])}}},
        "animation.anzuelo.pelea": {"loop": True, "animation_length": 0.6, "bones": {"flotador": {
            "position": [0, "-1.5 + math.sin(q.anim_time * 900) * 0.8", 0],
            "rotation": ["math.sin(q.anim_time * 700) * 22", 0, "math.cos(q.anim_time * 560) * 18"]}}},
    }}
    write_json(os.path.join(BASE, "animations", "anzuelo.animation.json"), anims)


# ---------------------------------------------------------------- cañas

CANAS = {
    "cana_bambu": ("#7FB24A", "#4E7A28", "#C9A36A", "#9AA3AD"),
    "cana_roble": ("#A0723F", "#6B4A26", "#3A3A3A", "#B8BEC6"),
    "cana_hierro": ("#C8CDD4", "#7D848E", "#4A2E1A", "#E0B84A"),
    "cana_coral": ("#FF8A9A", "#C2506A", "#1E6E80", "#7FF0E0"),
    "cana_cristal": ("#B8F4FF", "#5FB7E6", "#2A3A6A", "#FFFFFF"),
    # la de Nilo: madera vieja y oscura con mango dorado y carrete rosa (mítica)
    "cana_nilo": ("#9A6434", "#55321A", "#E0B84A", "#FF7AC8"),
}


def cana(name, rod, rod_dark, handle, reel, lanzada=False):
    """Caña de 16x16 en diagonal (mango abajo a la izquierda, punta arriba a la derecha) con carrete e hilo que cuelga.

    lanzada: sin el hilo ni el corcho (se usa mientras el corcho está en el agua, como la caña de Minecraft)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    rod, rod_dark, handle, reel = hexc(rod), hexc(rod_dark), hexc(handle), hexc(reel)
    outline = mix(rod_dark, (20, 20, 28, 255), 0.6)
    # vara: de (2,13) a (13,2), dos pixeles de grueso cerca del mango y uno en la punta
    for i in range(12):
        x, y = 2 + i, 13 - i
        c = rod if i % 3 else rod_dark          # nudos (en el bambú se notan)
        if i < 4:
            c = handle if i < 3 else rod_dark
        p[x, y] = c
        if i < 7:
            p[x + 1, y] = shade(c, -0.25)
    # carrete
    for (x, y) in ((5, 12), (6, 12), (5, 13), (6, 13)):
        p[x, y] = reel
    p[5, 12] = shade(reel, 0.35)
    # hilo que cuelga de la punta
    if not lanzada:
        for y in range(3, 10):
            p[14, y] = (225, 228, 232, 255)
        p[14, 10] = hexc("#D23A2E")
        p[14, 11] = hexc("#F2F0EA")
    # contorno
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    q = out.load()
    for y in range(16):
        for x in range(16):
            if p[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < 16 and 0 <= yy < 16 and p[xx, yy][3] and not (xx == 14 and 3 <= yy <= 9):
                    q[x, y] = outline
                    break
    out.alpha_composite(img)
    os.makedirs(os.path.join(BASE, "textures", "item"), exist_ok=True)
    tex = name + ("_lanzada" if lanzada else "")
    out.save(os.path.join(BASE, "textures", "item", tex + ".png"))
    model = {"parent": "minecraft:item/handheld_rod", "textures": {"layer0": f"{MOD}:item/{tex}"}}
    if not lanzada:
        # 1.20.1 y 1.21.1: el modelo cambia con la propiedad del Core (ItemModelProps) mientras el corcho está afuera
        model["overrides"] = [{"predicate": {f"{MOD}:lanzada": 1}, "model": f"{MOD}:item/{name}_lanzada"}]
        # 1.21.4+: la misma condición en la definición del objeto
        write_json(os.path.join(BASE, "items", name + ".json"), {"model": {
            "type": "minecraft:condition", "property": f"{MOD}:lanzada",
            "on_true": {"type": "minecraft:model", "model": f"{MOD}:item/{name}_lanzada"},
            "on_false": {"type": "minecraft:model", "model": f"{MOD}:item/{name}"}}})
    write_json(os.path.join(BASE, "models", "item", tex + ".json"), model)


# ---------------------------------------------------------------- carnadas

def contorno(img, color):
    """Contorno de 1 pixel alrededor de lo dibujado (como los objetos de Minecraft)."""
    p = img.load()
    w, h = img.size
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    q = out.load()
    for y in range(h):
        for x in range(w):
            if p[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < w and 0 <= yy < h and p[xx, yy][3] > 200:
                    q[x, y] = color
                    break
    out.alpha_composite(img)
    return out


def disco(p, cx, cy, r, color, w=16, h=16):
    for y in range(h):
        for x in range(w):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                p[x, y] = color


def guardar_objeto(img, name):
    os.makedirs(os.path.join(BASE, "textures", "item"), exist_ok=True)
    img.save(os.path.join(BASE, "textures", "item", name + ".png"))
    write_json(os.path.join(BASE, "models", "item", name + ".json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})


def lombriz():
    """Lombriz en forma de S (en diagonal) con anillos, el cinturón más claro cerca de la cabeza y brillo arriba."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    body, dark, light, band = hexc("#DB7F80"), hexc("#B0525E"), hexc("#F8BDB4"), hexc("#F0A898")
    a, b, amp, r0, r1 = (3.0, 12.8), (12.8, 3.2), 2.2, 1.0, 1.55
    dx, dy = b[0] - a[0], b[1] - a[1]
    ln = math.hypot(dx, dy)
    nx, ny = -dy / ln, dx / ln
    pts = []
    for i in range(90):
        t = i / 89
        off = amp * math.sin(t * math.pi * 2)
        pts.append((a[0] + dx * t + nx * off, a[1] + dy * t + ny * off, t))
    for x, y, t in pts:
        r = r0 + (r1 - r0) * min(1, t * 4) if t < 0.25 else (r1 if t < 0.85 else r1 - (t - 0.85) * 3)
        disco(p, x, y, r, body)
    for x, y, t in pts:
        if 0.72 < t < 0.82:
            disco(p, x, y, r1 + 0.1, band)
    for i in range(4, 86, 6):
        x, y, t = pts[i]
        if 0.72 < t < 0.82:
            continue
        xi, yi = int(x + 0.5 * nx), int(y + 0.5 * ny)
        if p[xi, yi][3]:
            p[xi, yi] = dark
    for i in range(6, 84, 4):
        x, y, t = pts[i]
        xi, yi = int(x - 0.9 * nx), int(y - 0.9 * ny)
        if 0 <= xi < 16 and 0 <= yi < 16 and p[xi, yi][3] and p[xi, yi] != dark:
            p[xi, yi] = light
    guardar_objeto(contorno(img, hexc("#4A1E28")), "lombriz")


def cebo_dulce():
    """Bolita de masa dulce con miel que escurre y brillo."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    base, sombra, luz, miel = hexc("#E9A23B"), hexc("#B8661E"), hexc("#FFE08A"), hexc("#F7C243", 235)
    disco(p, 8, 7.5, 4.6, sombra)
    disco(p, 7.6, 7.0, 4.1, base)
    disco(p, 6.6, 5.8, 1.6, luz)
    p[6, 5] = (255, 250, 220, 255)
    # semillitas
    for x, y in ((9, 6), (10, 9), (7, 10), (5, 8)):
        p[x, y] = hexc("#8A4A16")
    # miel que escurre
    for y in range(11, 15):
        p[9, y] = miel
    p[9, 15] = hexc("#F7C243", 200)
    for y in range(11, 13):
        p[6, y] = miel
    p[10, 11] = miel
    guardar_objeto(contorno(img, hexc("#5A2E0C")), "cebo_dulce")


def cebo_brillante():
    """Larva que brilla (como las bayas luminosas) con chispas alrededor."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    body, dark, light = hexc("#7FF5E0"), hexc("#2FAFB0"), hexc("#E6FFFA")
    pts = []
    for i in range(40):
        a = math.pi * (0.15 + 1.25 * i / 39)
        pts.append((8 + math.cos(a) * 3.6, 8.6 + math.sin(a) * 3.2, i / 39))
    for x, y, t in pts:
        disco(p, x, y, 1.9 - 0.6 * t, dark)
    for x, y, t in pts:
        disco(p, x - 0.3, y - 0.3, 1.45 - 0.5 * t, body)
    for x, y, t in pts[4:30:5]:
        p[int(x), int(y - 1)] = light
    hx, hy, _ = pts[0]
    p[int(hx), int(hy)] = hexc("#1C5A66")
    out = contorno(img, hexc("#0E3A44"))
    q = out.load()
    for x, y, c in ((3, 2, "#FFFBD0"), (13, 3, "#FFFFFF"), (2, 12, "#BFFFF4"), (13, 13, "#FFFBD0"), (12, 1, "#BFFFF4")):
        q[x, y] = hexc(c)
    for x, y in ((3, 1), (3, 3), (2, 2), (4, 2)):
        q[x, y] = hexc("#FFF6A8", 170)
    guardar_objeto(out, "cebo_brillante")


def cebo_luna():
    """Señuelo de plata en forma de luna creciente con su argolla y una estrella."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    plata, sombra, luz = hexc("#D8E6FF"), hexc("#7E95C8"), hexc("#FFFFFF")
    for y in range(16):
        for x in range(16):
            fx, fy = x + 0.5, y + 0.5
            dentro = (fx - 7.5) ** 2 + (fy - 8.5) ** 2 <= 5.6 ** 2
            hueco = (fx - 10.2) ** 2 + (fy - 6.6) ** 2 <= 4.6 ** 2
            if dentro and not hueco:
                d = math.hypot(fx - 7.5, fy - 8.5)
                p[x, y] = sombra if d > 4.6 or fy > 12 else plata
    for x, y in ((4, 6), (3, 8), (4, 9), (5, 11)):
        if p[x, y][3]:
            p[x, y] = luz
    # argolla arriba
    for x, y in ((6, 1), (7, 1), (5, 2), (8, 2), (6, 3), (7, 3)):
        p[x, y] = hexc("#B8C4D8")
    out = contorno(img, hexc("#27335A"))
    q = out.load()
    # estrella
    for x, y in ((12, 11), (12, 10), (12, 12), (11, 11), (13, 11)):
        q[x, y] = hexc("#FFF3A0")
    q[12, 11] = hexc("#FFFFFF")
    guardar_objeto(out, "cebo_luna")


# ---------------------------------------------------------------- trofeo y acuario

def ruido(c, rnd, k=8):
    return tuple(max(0, min(255, c[i] + rnd.randint(-k, k))) for i in range(3)) + (c[3],)


def textura(name, w, h, fn, carpeta="block"):
    import random
    rnd = random.Random(name)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    p = img.load()
    for y in range(h):
        for x in range(w):
            c = fn(x, y, rnd)
            if c is not None:
                p[x, y] = c
    os.makedirs(os.path.join(BASE, "textures", carpeta), exist_ok=True)
    img.save(os.path.join(BASE, "textures", carpeta, name + ".png"))


def trofeo():
    oscura, clara = hexc("#4A2E18"), hexc("#8A5A32")

    def madera(x, y, rnd):
        # tabla con borde oscuro y vetas
        borde = x in (0, 15) or y in (0, 15)
        c = mix(oscura, clara, 0.35) if borde else (clara if (x * 3 + y // 2) % 7 else shade(clara, -0.12))
        return ruido(c, rnd, 6)

    def panel(x, y, rnd):
        c = hexc("#A87444") if (x + y * 5) % 11 else hexc("#94643A")
        return ruido(c, rnd, 5)

    def laton(x, y, rnd):
        c = hexc("#E8C25A") if y < 8 else hexc("#C99A32")
        if x in (0, 15) or y in (0, 15):
            c = hexc("#8C6418")
        if (x, y) in ((2, 7), (13, 7)):
            c = hexc("#6A4A10")
        return ruido(c, rnd, 4)
    textura("trofeo_madera", 16, 16, madera)
    textura("trofeo_panel", 16, 16, panel)
    textura("trofeo_laton", 16, 16, laton)
    caras = lambda t: {f: {"texture": "#" + t} for f in ("north", "south", "east", "west", "up", "down")}
    write_json(os.path.join(BASE, "models", "block", "trofeo.json"), {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{MOD}:block/trofeo_madera", "madera": f"{MOD}:block/trofeo_madera",
                     "panel": f"{MOD}:block/trofeo_panel", "laton": f"{MOD}:block/trofeo_laton"},
        "elements": [
            {"from": [1, 2, 15], "to": [15, 14, 16], "faces": caras("madera")},
            {"from": [2, 4.5, 14.5], "to": [14, 13, 15], "faces": caras("panel")},
            {"from": [2, 1.6, 14.4], "to": [14, 4.2, 15], "faces": caras("laton")}],
        "display": {
            "gui": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.8, 0.8, 0.8]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 225, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "firstperson_righthand": {"rotation": [0, 225, 0], "scale": [0.4, 0.4, 0.4]}}})
    write_json(os.path.join(BASE, "models", "item", "trofeo.json"), {"parent": f"{MOD}:block/trofeo"})
    write_json(os.path.join(BASE, "blockstates", "trofeo.json"), {"variants": {
                   "facing=north": {"model": f"{MOD}:block/trofeo"},
                   "facing=east": {"model": f"{MOD}:block/trofeo", "y": 90},
                   "facing=south": {"model": f"{MOD}:block/trofeo", "y": 180},
                   "facing=west": {"model": f"{MOD}:block/trofeo", "y": 270}}})


def acuario():
    def marco(x, y, rnd):
        c = hexc("#3A3F47") if (x + y) % 5 else hexc("#4B525C")
        if x in (0, 15) or y in (0, 15):
            c = hexc("#2A2E34")
        return ruido(c, rnd, 5)

    def arena(x, y, rnd):
        c = hexc("#E4D3A0")
        r = rnd.random()
        if r < 0.12:
            c = hexc("#C9B47E")
        elif r < 0.17:
            c = hexc("#9A9A9A")
        elif r < 0.2:
            c = hexc("#E88A6A")
        return ruido(c, rnd, 6)

    def roca(x, y, rnd):
        return ruido(hexc("#7A7F86") if (x * y) % 4 else hexc("#5E636A"), rnd, 8)

    def alga(x, y, rnd):
        if abs((x - 8) - round(2 * math.sin(y / 3.0))) > 2 - (y > 12):
            return None
        return ruido(hexc("#3FA05A") if x % 2 else hexc("#2E7F45"), rnd, 8)

    def agua(x, y, rnd):
        # agua azul con brillos; transparente para ver los peces
        base = (70, 150, 210, 95)
        if (x + y * 3) % 13 == 0 or (y == 1 and x % 4 == 0):
            base = (200, 235, 255, 140)
        return base

    def burbuja(x, y, rnd):
        d = math.hypot(x - 7.5, y - 7.5)
        if d > 7.6:
            return None
        if d > 6:
            return (230, 245, 255, 230)
        if 3 < x < 6 and 3 < y < 6:
            return (255, 255, 255, 240)
        return (200, 230, 255, 60)
    textura("acuario_marco", 16, 16, marco)
    textura("acuario_arena", 16, 16, arena)
    textura("acuario_roca", 16, 16, roca)
    textura("acuario_alga", 16, 16, alga, "entity")
    textura("acuario_agua", 16, 16, agua, "entity")
    textura("burbuja", 16, 16, burbuja, "entity")
    caras = lambda t: {f: {"texture": "#" + t} for f in ("north", "south", "east", "west", "up", "down")}
    els = [{"from": [0, 0, 0], "to": [16, 2, 16], "faces": caras("marco")},
           {"from": [1, 2, 1], "to": [15, 3, 15], "faces": caras("arena")}]
    for (x, z) in ((0, 0), (15, 0), (0, 15), (15, 15)):
        els.append({"from": [x, 2, z], "to": [x + 1, 15, z + 1], "faces": caras("marco")})
    els += [{"from": [0, 15, 0], "to": [16, 16, 1], "faces": caras("marco")},
            {"from": [0, 15, 15], "to": [16, 16, 16], "faces": caras("marco")},
            {"from": [0, 15, 1], "to": [1, 16, 15], "faces": caras("marco")},
            {"from": [15, 15, 1], "to": [16, 16, 15], "faces": caras("marco")},
            {"from": [3, 3, 9], "to": [6, 5, 12], "faces": caras("roca")},
            {"from": [4, 5, 10], "to": [5.5, 6, 11.5], "faces": caras("roca")}]
    # las algas las dibuja el renderizador (se mecen y necesitan transparencia)
    write_json(os.path.join(BASE, "models", "block", "acuario.json"), {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{MOD}:block/acuario_marco", "marco": f"{MOD}:block/acuario_marco", "arena": f"{MOD}:block/acuario_arena",
                     "roca": f"{MOD}:block/acuario_roca"},
        "elements": els})
    write_json(os.path.join(BASE, "models", "item", "acuario.json"), {"parent": f"{MOD}:block/acuario"})
    write_json(os.path.join(BASE, "blockstates", "acuario.json"), {"variants": {"": {"model": f"{MOD}:block/acuario"}}})


def copas():
    """Copas de torneo (oro, plata, bronce): base de madera oscura, pie, tallo con nudo, copa abierta y dos asas."""
    metales = {"oro": ("#FFE27A", "#E0A82E", "#8C5A10"), "plata": ("#F4F7FA", "#B8C2CC", "#5E6873"), "bronce": ("#F0B27A", "#C07838", "#6A3A14")}

    def base(x, y, rnd):
        c = hexc("#3A2618") if (x + y * 2) % 9 else hexc("#4A3220")
        if y in (0, 15):
            c = hexc("#2A1A10")
        return ruido(c, rnd, 4)
    textura("copa_base", 16, 16, base)
    for nombre, (claro, medio, oscuro) in metales.items():
        cl, me, os_ = hexc(claro), hexc(medio), hexc(oscuro)

        def metal(x, y, rnd, cl=cl, me=me, os_=os_):
            # degradado vertical con una franja de brillo en diagonal
            t = y / 15.0
            c = mix(cl, me, min(1, t * 1.4)) if t < 0.7 else mix(me, os_, (t - 0.7) / 0.3)
            if (x + y) % 11 in (0, 1):
                c = mix(c, (255, 255, 255, 255), 0.45)
            return ruido(c, rnd, 3)
        textura("copa_" + nombre, 16, 16, metal)
        caras = lambda t: {f: {"texture": "#" + t} for f in ("north", "south", "east", "west", "up", "down")}
        els = [
            {"from": [4, 0, 4], "to": [12, 1.5, 12], "faces": caras("base")},
            {"from": [5, 1.5, 5], "to": [11, 3, 11], "faces": caras("metal")},
            {"from": [7, 3, 7], "to": [9, 6.5, 9], "faces": caras("metal")},
            {"from": [6.5, 6.5, 6.5], "to": [9.5, 7.5, 9.5], "faces": caras("metal")},
            {"from": [5.5, 7.5, 5.5], "to": [10.5, 8.5, 10.5], "faces": caras("metal")},
            # paredes de la copa
            {"from": [4.5, 8.5, 4.5], "to": [11.5, 13.5, 5.5], "faces": caras("metal")},
            {"from": [4.5, 8.5, 10.5], "to": [11.5, 13.5, 11.5], "faces": caras("metal")},
            {"from": [4.5, 8.5, 5.5], "to": [5.5, 13.5, 10.5], "faces": caras("metal")},
            {"from": [10.5, 8.5, 5.5], "to": [11.5, 13.5, 10.5], "faces": caras("metal")},
            # borde
            {"from": [4, 13.5, 4], "to": [12, 14, 5], "faces": caras("metal")},
            {"from": [4, 13.5, 11], "to": [12, 14, 12], "faces": caras("metal")},
            {"from": [4, 13.5, 5], "to": [5, 14, 11], "faces": caras("metal")},
            {"from": [11, 13.5, 5], "to": [12, 14, 11], "faces": caras("metal")},
            # asas
            {"from": [2.5, 9.5, 7.5], "to": [4.5, 10.5, 8.5], "faces": caras("metal")},
            {"from": [2.5, 12, 7.5], "to": [4.5, 13, 8.5], "faces": caras("metal")},
            {"from": [2.5, 10.5, 7.5], "to": [3.5, 12, 8.5], "faces": caras("metal")},
            {"from": [11.5, 9.5, 7.5], "to": [13.5, 10.5, 8.5], "faces": caras("metal")},
            {"from": [11.5, 12, 7.5], "to": [13.5, 13, 8.5], "faces": caras("metal")},
            {"from": [12.5, 10.5, 7.5], "to": [13.5, 12, 8.5], "faces": caras("metal")}]
        bid = "copa_" + nombre
        write_json(os.path.join(BASE, "models", "block", bid + ".json"), {
            "parent": "minecraft:block/block",
            "textures": {"particle": f"{MOD}:block/{bid}", "metal": f"{MOD}:block/{bid}", "base": f"{MOD}:block/copa_base"},
            "elements": els})
        write_json(os.path.join(BASE, "models", "item", bid + ".json"), {"parent": f"{MOD}:block/{bid}"})
        write_json(os.path.join(BASE, "blockstates", bid + ".json"), {"variants": {"": {"model": f"{MOD}:block/{bid}"}}})


def main():
    corcho()
    trofeo()
    acuario()
    copas()
    for n, cols in CANAS.items():
        cana(n, *cols)
        cana(n, *cols, lanzada=True)
    lombriz()
    cebo_dulce()
    cebo_brillante()
    cebo_luna()
    print("recursos listos:", ", ".join(["anzuelo"] + list(CANAS) + ["lombriz", "cebo_dulce", "cebo_brillante", "cebo_luna"]))


if __name__ == "__main__":
    main()
