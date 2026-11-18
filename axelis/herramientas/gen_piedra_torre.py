"""Piedra de Portal: Torre (etapa 4). Texturas 16x16 (lado y cima), modelos, estado, receta y botín. Uso: python3 gen_piedra_torre.py <resources>"""
import json, os, sys, random
from PIL import Image

res = sys.argv[1]
A = os.path.join(res, "assets/mundos_de_axelis")
D = os.path.join(res, "data/mundos_de_axelis")
rnd = random.Random(44)

DARK = [(34, 30, 46), (40, 36, 54), (29, 25, 40), (46, 41, 60)]
MORTAR = (18, 15, 26)
GLOW = (190, 120, 255)
GLOW2 = (235, 205, 255)
GOLD = (236, 196, 96)


def bricks():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 0 if row % 2 == 0 else 4
            if y % 4 == 3 or (x + off) % 8 == 7:
                c = MORTAR
            else:
                c = rnd.choice(DARK)
            img.putpixel((x, y), c + (255,))
    return img


def side():
    img = bricks()
    # marco dorado arriba y abajo
    for x in range(16):
        img.putpixel((x, 0), GOLD + (255,))
        img.putpixel((x, 15), (150, 118, 52, 255))
    # runa: un ojo con rayos
    pts = [(7, 4), (8, 4), (6, 5), (9, 5), (5, 6), (10, 6), (5, 7), (10, 7), (6, 8), (9, 8), (7, 9), (8, 9),
           (7, 2), (8, 2), (7, 11), (8, 11), (3, 6), (12, 6), (3, 7), (12, 7)]
    for p in pts:
        img.putpixel(p, GLOW + (255,))
    for p in [(7, 6), (8, 6), (7, 7), (8, 7)]:
        img.putpixel(p, GLOW2 + (255,))
    return img


def top():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), rnd.choice(DARK) + (255,))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 5.6 < d < 6.6:
                img.putpixel((x, y), GOLD + (255,))
            elif 3.0 < d < 3.9:
                img.putpixel((x, y), GLOW + (255,))
            elif d < 1.6:
                img.putpixel((x, y), GLOW2 + (255,))
    for p in [(7, 4), (8, 4), (7, 11), (8, 11), (4, 7), (4, 8), (11, 7), (11, 8)]:
        img.putpixel(p, GLOW + (255,))
    return img


os.makedirs(os.path.join(A, "textures/block"), exist_ok=True)
side().save(os.path.join(A, "textures/block/portal_stone_tower_side.png"))
top().save(os.path.join(A, "textures/block/portal_stone_tower_top.png"))
for sub in ("models/block", "models/item", "blockstates"):
    os.makedirs(os.path.join(A, sub), exist_ok=True)
json.dump({"parent": "minecraft:block/cube_column", "textures": {"end": "mundos_de_axelis:block/portal_stone_tower_top",
                                                                 "side": "mundos_de_axelis:block/portal_stone_tower_side"}},
          open(os.path.join(A, "models/block/portal_stone_tower.json"), "w"), indent=1)
json.dump({"parent": "mundos_de_axelis:block/portal_stone_tower"}, open(os.path.join(A, "models/item/portal_stone_tower.json"), "w"), indent=1)
json.dump({"variants": {"": {"model": "mundos_de_axelis:block/portal_stone_tower"}}},
          open(os.path.join(A, "blockstates/portal_stone_tower.json"), "w"), indent=1)
os.makedirs(os.path.join(D, "recipes"), exist_ok=True)
json.dump({"type": "minecraft:crafting_shaped", "pattern": ["ABA", "BEB", "ABA"],
           "key": {"A": {"item": "minecraft:amethyst_block"}, "B": {"item": "minecraft:bookshelf"}, "E": {"item": "minecraft:ender_eye"}},
           "result": {"item": "mundos_de_axelis:portal_stone_tower", "count": 1}},
          open(os.path.join(D, "recipes/portal_stone_tower.json"), "w"), indent=1)
os.makedirs(os.path.join(D, "loot_tables/blocks"), exist_ok=True)
json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "mundos_de_axelis:portal_stone_tower"}],
                                                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]},
          open(os.path.join(D, "loot_tables/blocks/portal_stone_tower.json"), "w"), indent=1)
print("ok")
