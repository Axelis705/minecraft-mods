"""Genera los trofeos de los jefes 3 (Lobo Ártico) y 4 (Archimago): modelo JSON con cajas y una textura-paleta de 16x16
(16 colores en celdas de 4x4, como los otros trofeos). Uso: python3 gen_trofeos.py <carpeta resources>"""
import json, os, sys
from PIL import Image

PAL = [0x2B2F3A, 0xC8D0DC, 0xBFEFFF, 0x7FD6FF, 0x3A8FD6, 0xFFD86A, 0xB46EFF, 0x5A2E9A,
       0xF2E6C8, 0x6B3A24, 0xF4FBFF, 0x1A1026, 0x78F0FF, 0xC0303A, 0x5A3A22, 0x6A6F7A]
STONE, SILVER, ICE_L, ICE_M, ICE_D, GOLD, VIOLET, VIOLET_D, PARCH, LEATHER, WHITE, OBSID, ARCANE, RED, WOOD, GRAY = range(16)


def uv(c):
    x, y = (c % 4) * 4, (c // 4) * 4
    return [x + 0.1, y + 0.1, x + 3.9, y + 3.9]


def box(f, t, c, rot=None, faces=None):
    e = {"from": list(f), "to": list(t), "faces": {d: {"uv": uv(faces.get(d, c) if faces else c), "texture": "#0"}
                                                 for d in ("north", "east", "south", "west", "up", "down")}}
    if rot:
        e["rotation"] = rot
    return e


def r(angle, axis, origin):
    return {"angle": angle, "axis": axis, "origin": list(origin)}


def ice():
    els = [box((3, 0, 3), (13, 2, 13), STONE),
           box((4, 2, 4), (12, 2.8, 12), SILVER),
           # placa azul al frente
           box((6, 0.5, 2.6), (10, 1.6, 3), ICE_D),
           # cristal central y cristales inclinados alrededor
           box((7, 2.8, 7), (9, 15, 9), ICE_L, faces={"up": WHITE, "north": ICE_L, "south": ICE_M, "east": ICE_M, "west": ICE_L}),
           box((5, 2.8, 7.2), (6.8, 11, 8.8), ICE_M, r(22.5, "z", (6, 3, 8))),
           box((9.2, 2.8, 7.2), (11, 10, 8.8), ICE_M, r(-22.5, "z", (10, 3, 8))),
           box((7.2, 2.8, 5), (8.8, 9, 6.8), ICE_D, r(-22.5, "x", (8, 3, 6))),
           box((7.2, 2.8, 9.2), (8.8, 12, 11), ICE_L, r(22.5, "x", (8, 3, 10))),
           box((5.5, 2.8, 5.5), (6.8, 6.5, 6.8), ICE_L, r(22.5, "y", (6, 3, 6))),
           box((9.2, 2.8, 9.2), (10.5, 7, 10.5), ICE_D, r(-22.5, "y", (10, 3, 10))),
           # escarcha en la base
           box((4.5, 2.8, 4.5), (11.5, 3.2, 11.5), WHITE)]
    return els


def tower():
    els = [box((3, 0, 3), (13, 2, 13), OBSID),
           box((4, 2, 4), (12, 2.6, 12), GOLD),
           # libro cerrado tumbado
           box((4.6, 2.6, 5), (11.4, 4.2, 11), VIOLET_D, faces={"up": VIOLET, "east": PARCH, "west": VIOLET_D}),
           box((10.6, 2.9, 5.4), (11.5, 3.9, 10.6), PARCH),
           box((7.4, 2.6, 4.7), (8.6, 4.4, 5.2), GOLD),
           # pie y aro dorado que sostienen el orbe
           box((7.4, 4.2, 7.4), (8.6, 9, 8.6), WOOD),
           box((6.5, 8.6, 6.5), (9.5, 9.2, 9.5), GOLD),
           box((6, 9.2, 6), (10, 13.2, 10), ARCANE, faces={"up": WHITE}),
           box((6.6, 9.8, 5.7), (9.4, 12.6, 10.3), ARCANE, r(45, "y", (8, 11, 8))),
           # tres cristales que flotan alrededor
           box((3.6, 11, 7.2), (4.8, 13, 8.4), VIOLET, r(22.5, "y", (4, 12, 8))),
           box((10.8, 12.4, 6), (12, 14.4, 7.2), VIOLET, r(-22.5, "y", (11, 13, 7))),
           box((7.4, 13.6, 10.8), (8.6, 15.6, 12), ARCANE, r(22.5, "y", (8, 14, 11)))]
    return els


def write(res, name, els):
    tex = "mundos_de_axelis:block/decor3d/" + name
    model = {"textures": {"0": tex, "particle": tex}, "elements": els}
    d = os.path.join(res, "assets/mundos_de_axelis")
    for sub in ("models/block", "models/item", "blockstates", "textures/block/decor3d"):
        os.makedirs(os.path.join(d, sub), exist_ok=True)
    json.dump(model, open(os.path.join(d, "models/block", name + ".json"), "w"), indent=1)
    json.dump({"parent": "mundos_de_axelis:block/" + name}, open(os.path.join(d, "models/item", name + ".json"), "w"), indent=1)
    bs = {"variants": {}}
    for face, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        v = {"model": "mundos_de_axelis:block/" + name}
        if y:
            v["y"] = y
        bs["variants"]["facing=" + face] = v
    json.dump(bs, open(os.path.join(d, "blockstates", name + ".json"), "w"), indent=1)
    img = Image.new("RGBA", (16, 16))
    for i, c in enumerate(PAL):
        for yy in range(4):
            for xx in range(4):
                # un poco de brillo arriba a la izquierda de cada celda para que no se vea plano
                k = 1.08 if (xx + yy) < 2 else 0.94 if (xx + yy) > 4 else 1.0
                rgb = tuple(min(255, int(((c >> s) & 255) * k)) for s in (16, 8, 0))
                img.putpixel(((i % 4) * 4 + xx, (i // 4) * 4 + yy), rgb + (255,))
    img.save(os.path.join(d, "textures/block/decor3d", name + ".png"))
    loot = {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "mundos_de_axelis:" + name}],
                                                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
    ld = os.path.join(res, "data/mundos_de_axelis/loot_tables/blocks")
    os.makedirs(ld, exist_ok=True)
    json.dump(loot, open(os.path.join(ld, name + ".json"), "w"), indent=1)


if __name__ == "__main__":
    res = sys.argv[1]
    write(res, "decor_trophy_ice", ice())
    write(res, "decor_trophy_tower", tower())
    print("ok")
