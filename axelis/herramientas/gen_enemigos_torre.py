"""Texturas de los enemigos de la Torre (formato de piel de jugador 64x64, modelo humanoide):
 - esbirro.png: túnica añil con ribete dorado, capucha que tapa la cara en sombra y dos ojos morados.
 - armadura_encantada.png: transparente (solo se ve la armadura de hierro puesta).
Uso: python3 gen_enemigos_torre.py <carpeta textures/entity>"""
import os, sys, random
from PIL import Image

random.seed(5)
INDIGO = (46, 38, 104)
INDIGO_D = (30, 24, 72)
INDIGO_L = (70, 58, 140)
GOLD = (214, 172, 70)
SHADOW = (12, 10, 22)
EYE = (196, 120, 255)
SKIN = (150, 142, 170)
BOOT = (40, 30, 28)
BELT = (70, 46, 34)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def fill(im, x, y, w, h, col, noise=0.08):
    for i in range(x, x + w):
        for j in range(y, y + h):
            k = 1.0 + random.uniform(-noise, noise)
            im.putpixel((i, j), shade(col, k) + (255,))


def box(im, u, v, w, h, d, col, top=None, bottom=None, noise=0.08):
    """Despliegue de una caja de piel (u, v = esquina; w ancho, h alto, d fondo)."""
    fill(im, u + d, v, w, d, top or shade(col, 1.1), noise)            # arriba
    fill(im, u + d + w, v, w, d, bottom or shade(col, 0.8), noise)     # abajo
    fill(im, u, v + d, d, h, shade(col, 0.9), noise)                   # derecha
    fill(im, u + d, v + d, w, h, col, noise)                           # frente
    fill(im, u + d + w, v + d, d, h, shade(col, 0.9), noise)           # izquierda
    fill(im, u + 2 * d + w, v + d, w, h, shade(col, 0.85), noise)      # espalda


def esbirro(path):
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    # cabeza: capucha por todos lados, cara en sombra
    box(im, 0, 0, 8, 8, 8, INDIGO)
    fill(im, 9, 10, 6, 6, SHADOW, 0.05)                  # hueco de la capucha
    for x in (10, 13):
        im.putpixel((x, 12), EYE + (255,))
        im.putpixel((x + 1, 12), shade(EYE, 0.8) + (255,))
    fill(im, 8, 8, 8, 1, INDIGO_L, 0.04)                 # borde de la capucha
    # capa extra (sombrero = capucha un poco más grande): solo el borde y la punta de atrás
    box(im, 32, 0, 8, 8, 8, INDIGO_D)
    fill(im, 41, 9, 6, 7, (0, 0, 0), 0)                  # hueco de la cara transparente
    for i in range(41, 47):
        for j in range(9, 16):
            im.putpixel((i, j), (0, 0, 0, 0))
    # cuerpo: túnica con ribete y cinturón
    box(im, 16, 16, 8, 12, 4, INDIGO)
    fill(im, 23, 20, 2, 12, GOLD, 0.06)                  # ribete al frente
    fill(im, 20, 26, 8, 1, BELT, 0.05)                   # cinturón
    im.putpixel((24, 26), GOLD + (255,))
    # brazos: mangas anchas, manos grises
    for (u, v) in ((40, 16), (32, 48)):
        box(im, u, v, 4, 12, 4, INDIGO)
        fill(im, u + 4, v + 13, 4, 3, SKIN, 0.05)        # mano (frente, abajo)
        fill(im, u, v + 13, 4, 3, shade(SKIN, 0.9), 0.05)
        fill(im, u + 8, v + 13, 4, 3, shade(SKIN, 0.9), 0.05)
        fill(im, u + 12, v + 13, 4, 3, shade(SKIN, 0.85), 0.05)
        fill(im, u + 4, v + 11, 4, 2, GOLD, 0.05)        # puño dorado
    # piernas: la túnica sigue hasta las botas
    for (u, v) in ((0, 16), (16, 48)):
        box(im, u, v, 4, 12, 4, INDIGO_D)
        fill(im, u + 4, v + 13, 4, 3, BOOT, 0.05)
        fill(im, u, v + 13, 4, 3, BOOT, 0.05)
        fill(im, u + 8, v + 13, 4, 3, BOOT, 0.05)
        fill(im, u + 12, v + 13, 4, 3, BOOT, 0.05)
    # capa de la túnica (chaqueta): falda más ancha sobre las piernas
    box(im, 16, 32, 8, 12, 4, INDIGO_L, noise=0.06)
    for i in range(16, 40):
        for j in range(32, 41):
            im.putpixel((i, j), (0, 0, 0, 0))            # solo la parte de abajo (falda)
    fill(im, 23, 42, 2, 6, GOLD, 0.06)
    im.save(path)


def armadura(path):
    Image.new("RGBA", (64, 64), (0, 0, 0, 0)).save(path)


if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    esbirro(os.path.join(out, "esbirro.png"))
    armadura(os.path.join(out, "armadura_encantada.png"))
    big = Image.open(os.path.join(out, "esbirro.png")).resize((256, 256), Image.NEAREST)
    big.save(os.path.join(out, "..", "..", "..", "..", "..", "..", "..", "..", "gen", "esbirro_preview.png") if False else "/tmp/esbirro_preview.png")
    print("ok")
