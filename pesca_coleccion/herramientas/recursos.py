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
}


def cana(name, rod, rod_dark, handle, reel):
    """Caña de 16x16 en diagonal (mango abajo a la izquierda, punta arriba a la derecha) con carrete e hilo que cuelga."""
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
    out.save(os.path.join(BASE, "textures", "item", name + ".png"))
    write_json(os.path.join(BASE, "models", "item", name + ".json"),
               {"parent": "minecraft:item/handheld_rod", "textures": {"layer0": f"{MOD}:item/{name}"}})


def main():
    corcho()
    for n, cols in CANAS.items():
        cana(n, *cols)
    print("recursos listos:", ", ".join(["anzuelo"] + list(CANAS)))


if __name__ == "__main__":
    main()
