#!/usr/bin/env python3
"""Piel del anfitrión de la ruleta de la suerte (dealer_wheel.png), hecha a partir de la del crupier de ruleta:
saco rojo de lentejuelas con destellos dorados (chaleco y mangas), puños blancos y moño dorado.

    python3 casino/herramientas/dealer_skins.py
"""
import os
import random

from PIL import Image

E = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino", "textures", "entity")


def sequin(rnd, x, y):
    base = (176, 22, 58) if (x + y) % 2 == 0 else (150, 16, 48)
    r = rnd.random()
    if r < 0.07:
        return (255, 214, 90, 255)      # destello dorado
    if r < 0.12:
        return (214, 60, 96, 255)       # brillo
    return base + (255,)


def main():
    rnd = random.Random(5)
    im = Image.open(os.path.join(E, "dealer_roulette.png")).convert("RGBA")
    px = im.load()

    def whiteish(c):
        return c[3] > 0 and c[0] > 200 and c[1] > 200 and c[2] > 195

    def crimson(c):
        return c[3] > 0 and c[0] > 90 and c[1] < 70 and c[2] < 90

    # mangas: brazo derecho (40..56, 16..32) y brazo izquierdo (32..48, 48..64): lo blanco pasa a saco rojo
    for (x0, y0, x1, y1) in ((40, 16, 56, 32), (32, 48, 48, 64)):
        rows = [y for y in range(y0, y1) if any(whiteish(px[x, y]) for x in range(x0, x1))]
        last = max(rows) if rows else None
        for y in range(y0, y1):
            for x in range(x0, x1):
                if whiteish(px[x, y]):
                    px[x, y] = (244, 244, 240, 255) if y == last else sequin(rnd, x, y)   # puño blanco en la última fila
    # chaleco (capa de encima del cuerpo, 16..40, 32..48): de rojo vino a lentejuelas
    for y in range(32, 48):
        for x in range(16, 40):
            if crimson(px[x, y]):
                px[x, y] = sequin(rnd, x, y)
    # solapas negras a los lados del chaleco al frente (20..28, 36..48): una columna a cada lado de la camisa
    im.save(os.path.join(E, "dealer_wheel.png"))
    im.resize((512, 512), Image.NEAREST).save(os.path.join(os.environ.get("TMPDIR", "/tmp"), "dealer_wheel_big.png"))


if __name__ == "__main__":
    main()
