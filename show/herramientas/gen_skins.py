#!/usr/bin/env python3
"""Dibuja las 8 skins del publico (5 personas y 3 frutas, como el publico absurdo de los concursos de internet)."""
import os, random
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/show_de_axelis/textures/entity")
os.makedirs(OUT, exist_ok=True)
random.seed(3)

def rect(d, x1, y1, x2, y2, c):
    d.rectangle([x1, y1, x2 - 1, y2 - 1], fill=c)

def person(skin, hair, shirt, pants, hair_long=False, name="x"):
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rect(d, 0, 0, 32, 16, skin)                      # cabeza
    rect(d, 8, 0, 16, 8, hair)                       # pelo arriba
    rect(d, 0, 8, 32, 11, hair)                      # pelo alrededor
    rect(d, 24, 8, 32, 16, hair) if hair_long else None
    rect(d, 16, 8, 24, 11, hair)
    rect(d, 8, 8, 16, 11, hair)                      # flequillo al frente
    # cara
    rect(d, 9, 12, 11, 13, (255, 255, 255)); rect(d, 13, 12, 15, 13, (255, 255, 255))
    rect(d, 10, 12, 11, 13, (30, 30, 60)); rect(d, 14, 12, 15, 13, (30, 30, 60))
    rect(d, 11, 14, 13, 15, (170, 70, 70))
    rect(d, 16, 16, 40, 32, shirt)                   # torso
    rect(d, 0, 16, 16, 32, pants)                    # pierna der
    rect(d, 16, 48, 32, 64, pants)                   # pierna izq
    rect(d, 40, 16, 56, 32, shirt); rect(d, 40, 24, 56, 32, skin)   # brazo der
    rect(d, 32, 48, 48, 64, shirt); rect(d, 32, 56, 48, 64, skin)   # brazo izq
    rect(d, 20, 20, 28, 21, tuple(max(0, c - 40) for c in shirt))  # cuello
    return im

def fruit(body, spot, leaf, face=(30, 30, 30), stripes=None, name="x"):
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for box in [(0, 0, 32, 16), (16, 16, 40, 32), (0, 16, 16, 32), (16, 48, 32, 64), (40, 16, 56, 32), (32, 48, 48, 64)]:
        rect(d, *box, body)
    if stripes:
        for box in [(0, 0, 32, 16), (16, 16, 40, 32), (0, 16, 16, 32), (16, 48, 32, 64), (40, 16, 56, 32), (32, 48, 48, 64)]:
            for x in range(box[0], box[2], 4):
                rect(d, x, box[1], x + 2, box[3], stripes)
    for _ in range(40):
        x, y = random.randrange(0, 56), random.randrange(0, 64)
        if im.getpixel((x, y))[3]:
            rect(d, x, y, x + 1, y + 1, spot)
    rect(d, 8, 0, 16, 8, leaf)                       # hojita arriba
    rect(d, 9, 11, 11, 13, (255, 255, 255)); rect(d, 13, 11, 15, 13, (255, 255, 255))
    rect(d, 10, 12, 11, 13, face); rect(d, 14, 12, 15, 13, face)
    rect(d, 10, 14, 14, 15, face)
    return im

skins = [
    person((234, 190, 150), (80, 50, 30), (50, 100, 200), (40, 40, 70)),
    person((240, 200, 170), (170, 60, 30), (60, 160, 90), (60, 50, 90), True),
    person((190, 130, 90), (20, 20, 25), (240, 200, 40), (50, 50, 55)),
    person((245, 210, 180), (230, 200, 90), (140, 70, 190), (30, 60, 110)),
    person((235, 195, 165), (200, 200, 205), (230, 120, 160), (90, 70, 60), True),
    fruit((250, 220, 50), (120, 80, 20), (90, 140, 40)),                    # banana
    fruit((200, 30, 40), (240, 120, 120), (50, 150, 50)),                   # manzana
    fruit((60, 150, 70), (20, 80, 30), (40, 110, 40), stripes=(30, 100, 45))  # sandia
]
for i, s in enumerate(skins):
    s.save(os.path.join(OUT, f"spectator_{i}.png"))
print("ok", len(skins))
