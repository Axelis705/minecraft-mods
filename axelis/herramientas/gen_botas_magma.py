"""Icono animado de las Botas de Magma (32x32, 12 cuadros, como los demás iconos de reliquias): un par de botas de basalto
con grietas de lava que laten y un halo cálido. Uso: python3 gen_botas_magma.py <carpeta textures/item>"""
import math, os, sys
from PIL import Image, ImageDraw, ImageFilter

N = 12
S = 32
BASALT = (52, 46, 54)
BASALT_L = (84, 74, 82)
BASALT_D = (30, 26, 32)
LAVA = [(255, 214, 90), (255, 150, 40), (226, 72, 22)]
SOLE = (36, 30, 34)


def boot(d, ox, oy, flip):
    """Una bota vista de lado: caña, empeine y suela. ox, oy = esquina; flip = mira a la izquierda."""
    def X(x):
        return ox + (9 - x if flip else x)
    # caña
    for y in range(0, 11):
        for x in range(1, 7):
            c = BASALT_L if x in (1, 2) else BASALT if x < 6 else BASALT_D
            d.point((X(x), oy + y), fill=c)
    # empeine y puntera
    for y in range(9, 14):
        for x in range(1, 10 if y > 10 else 8):
            c = BASALT if y < 13 else BASALT_D
            d.point((X(x), oy + y), fill=c)
    # suela
    for x in range(0, 11):
        d.point((X(x), oy + 14), fill=SOLE)
    # borde de arriba
    for x in range(0, 8):
        d.point((X(x), oy), fill=BASALT_L)


def frame(k):
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    pulse = 0.5 + 0.5 * math.sin(2 * math.pi * k / N)
    # halo cálido
    halo = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    hd = ImageDraw.Draw(halo)
    a = int(70 + 60 * pulse)
    hd.ellipse((3, 5, 29, 31), fill=(255, 110, 30, a))
    halo = halo.filter(ImageFilter.GaussianBlur(4))
    im.alpha_composite(halo)
    d = ImageDraw.Draw(im)
    boot(d, 4, 9, False)
    boot(d, 17, 7, True)
    # grietas de lava (mismas en todos los cuadros; el color late)
    cracks = [(6, 12), (7, 13), (7, 14), (8, 15), (6, 17), (7, 18), (9, 20), (10, 21), (5, 20),
              (24, 10), (23, 11), (23, 12), (22, 13), (24, 15), (23, 16), (21, 18), (20, 19), (25, 18)]
    for i, (x, y) in enumerate(cracks):
        t = (i * 0.37 + k / N) % 1.0
        c = LAVA[0] if (pulse > 0.6 and t < 0.5) else LAVA[1] if t < 0.75 else LAVA[2]
        d.point((x, y), fill=c)
    # chispas que suben
    for j in range(3):
        y = 8 - ((k + j * 4) % N) * 0.6
        x = 8 + j * 7
        if y > 0:
            d.point((x, int(y)), fill=LAVA[0] + (220,))
    # gotas de lava en la suela
    d.point((9, 23), fill=LAVA[1])
    d.point((21, 21), fill=LAVA[1])
    return im


if __name__ == "__main__":
    out = sys.argv[1]
    strip = Image.new("RGBA", (S, S * N), (0, 0, 0, 0))
    for k in range(N):
        strip.alpha_composite(frame(k), (0, S * k))
    strip.save(os.path.join(out, "magma_boots.png"))
    with open(os.path.join(out, "magma_boots.png.mcmeta"), "w") as f:
        f.write('{"animation": {"frametime": 3}}')
    frame(0).resize((256, 256), Image.NEAREST).save("/tmp/magma_boots_preview.png")
    print("ok")
