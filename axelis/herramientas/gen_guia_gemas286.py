"""Etapa 286: icono de la Guía del Gemista (16x16): un libro de cuero morado con esquinas doradas y una gema en la tapa.
Uso: python3 gen_guia_gemas286.py <carpeta assets/mundos_de_axelis>"""
import json, os, sys
from PIL import Image

root = sys.argv[1]
O = None
K = (40, 16, 52, 255)        # contorno
C1 = (118, 52, 150, 255)     # cuero
C2 = (150, 78, 186, 255)     # cuero claro
C3 = (86, 34, 112, 255)      # cuero oscuro
P1 = (236, 226, 200, 255)    # hojas
P2 = (206, 192, 160, 255)
G1 = (255, 214, 92, 255)     # oro
G2 = (196, 142, 40, 255)
E1 = (120, 236, 255, 255)    # gema
E2 = (40, 168, 220, 255)
E3 = (220, 255, 255, 255)
art = [
    "................",
    "..KKKKKKKKKKK...",
    ".KGCCCCCCCCCGK..",
    ".KCBBBBBBBBBCKP.",
    ".KCBCCCCCCCBCKP.",
    ".KCBCCCWCCCBCKP.",
    ".KCBCCWEEC CBCKP".replace(' ', 'C')[:16],
    ".KCBCWEEEECCBCKP",
    ".KCBCCDEEDCCBCKP",
    ".KCBCCCDDCCCBCKP",
    ".KCBCCCCCCCCBCKP",
    ".KCBBBBBBBBBBCKP",
    ".KGCCCCCCCCCCGKP",
    ".KKKKKKKKKKKKKKQ",
    "..QQQQQQQQQQQQQ.",
    "................",
]
pal = {'.': O, 'K': K, 'C': C1, 'B': C2, 'G': G1, 'P': P1, 'Q': P2, 'W': E3, 'E': E1, 'D': E2}
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for y, row in enumerate(art):
    for x, ch in enumerate(row[:16].ljust(16, '.')):
        c = pal.get(ch)
        if c:
            img.putpixel((x, y), c)
# un brillo en el oro de la esquina
img.putpixel((2, 2), G1)
img.putpixel((13, 12), G2)
os.makedirs(os.path.join(root, 'textures/item'), exist_ok=True)
os.makedirs(os.path.join(root, 'models/item'), exist_ok=True)
img.save(os.path.join(root, 'textures/item/guia_gemas.png'))
json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "mundos_de_axelis:item/guia_gemas"}},
          open(os.path.join(root, 'models/item/guia_gemas.json'), 'w'), indent=1)
img.resize((128, 128), Image.NEAREST).save('/tmp/guia_gemas_x8.png')
print('ok')
