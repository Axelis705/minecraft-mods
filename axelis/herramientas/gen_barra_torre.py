"""Barra de jefe de la Torre (textures/gui/boss_tower.png, 256x256): marco de obsidiana con borde dorado, un libro a la izquierda
y un cristal a la derecha (marco en (0,0) de 202x26) y tres rellenos de 182x6 en v=32 (fase 1, arcano), 40 (fase 2, alquimia)
y 48 (fase 3, estrellas). Uso: python3 gen_barra_torre.py <resources>"""
import os, sys
from PIL import Image, ImageDraw

res = sys.argv[1]
img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
OBS = (26, 16, 38, 255)
OBS2 = (44, 28, 64, 255)
VIO = (180, 110, 255, 255)
VIO_D = (90, 46, 154, 255)
GOLD = (236, 196, 96, 255)
GOLD_D = (150, 110, 46, 255)

# placa del nombre
d.rectangle([44, 0, 157, 9], fill=(20, 10, 34, 200))
d.line([44, 9, 157, 9], fill=GOLD_D)
d.line([46, 0, 155, 0], fill=GOLD_D)
# ranura de la barra: borde de obsidiana y línea morada interior
d.rectangle([8, 8, 193, 17], fill=OBS)
d.rectangle([9, 9, 192, 16], fill=OBS2)
d.rectangle([10, 10, 191, 15], fill=(10, 6, 16, 255))
d.line([8, 8, 193, 8], fill=GOLD)
d.line([8, 17, 193, 17], fill=GOLD_D)
# runas bajo la barra
for x in range(20, 186, 12):
    d.point((x, 19), fill=VIO)
    d.point((x + 1, 20), fill=VIO_D)
    d.point((x - 1, 20), fill=VIO_D)
# libro a la izquierda
d.rectangle([0, 6, 12, 20], fill=VIO_D)
d.rectangle([1, 7, 11, 19], fill=(120, 60, 190, 255))
d.rectangle([10, 7, 12, 19], fill=(240, 230, 200, 255))
d.line([0, 6, 12, 6], fill=GOLD)
d.line([0, 20, 12, 20], fill=GOLD)
d.rectangle([4, 11, 7, 14], fill=GOLD)
d.point((5, 12), fill=(255, 245, 190, 255))
# cristal a la derecha (rombo)
cx, cy = 195, 13
for yy in range(-8, 9):
    w = 5 - abs(yy) * 5 // 8
    for xx in range(-w, w + 1):
        edge = abs(xx) == w or abs(yy) == 8
        c = VIO_D if edge else ((220, 180, 255, 255) if xx < 0 and yy < 0 else VIO)
        img.putpixel((cx + xx, cy + yy), c)
img.putpixel((cx - 1, cy - 3), (255, 255, 255, 255))
img.putpixel((cx - 2, cy - 2), (255, 255, 255, 255))
# esquinas doradas
for (x, y) in ((8, 8), (193, 8), (8, 17), (193, 17)):
    d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=GOLD)


def fill(v, top, bottom, spark):
    for y in range(6):
        k = y / 5.0
        c = tuple(int(top[i] * (1 - k) + bottom[i] * k) for i in range(3))
        for x in range(182):
            img.putpixel((x, v + y), c + (255,))
    for x in range(0, 182, 9):
        for y in range(6):
            xx = x + y
            if xx < 182 and (y in (1, 2)):
                img.putpixel((xx, v + y), spark)


fill(32, (140, 240, 255), (40, 110, 200), (230, 255, 255, 255))    # fase 1: arcano
fill(40, (170, 255, 120), (50, 140, 60), (230, 255, 210, 255))     # fase 2: alquimia
fill(48, (255, 230, 140), (200, 110, 40), (255, 255, 230, 255))    # fase 3: estrellas
out = os.path.join(res, "assets/mundos_de_axelis/textures/gui")
os.makedirs(out, exist_ok=True)
img.save(os.path.join(out, "boss_tower.png"))
img.crop((0, 0, 202, 56)).resize((808, 224), Image.NEAREST).save("/tmp/claude-0/barra_prev.png")
print("ok")
