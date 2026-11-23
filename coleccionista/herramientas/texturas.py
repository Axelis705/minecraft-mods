#!/usr/bin/env python3
"""
Texturas de Los Juegos del Coleccionista (pixel art hecho a mano en código).

    python3 coleccionista/herramientas/texturas.py

Genera:
  - textures/entity/fantasma.png (32x32): el fantasmita de los eliminados (sábana, ojos, boca, falda con picos).
  - textures/item/ficha_vida.png (16x16): la ficha de vida (porcelana con corazón rojo y filo dorado).
"""
import os

from PIL import Image

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "coleccionista", "textures")


def guardar(img, ruta):
    ruta = os.path.join(RAIZ, ruta)
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    img.save(ruta)
    print("ok", ruta)


def mezcla(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def caja(img, u, v, w, h, d, color_fn):
    """Pinta las 6 caras de una caja con UV de Minecraft (u, v) y tamaño (w, h, d). color_fn(cara, x, y, ancho, alto)."""
    caras = {
        "arriba": (u + d, v, w, d),
        "abajo": (u + d + w, v, w, d),
        "derecha": (u, v + d, d, h),
        "frente": (u + d, v + d, w, h),
        "izquierda": (u + d + w, v + d, d, h),
        "atras": (u + d + w + d, v + d, w, h),
    }
    for cara, (x0, y0, cw, ch) in caras.items():
        for y in range(ch):
            for x in range(cw):
                c = color_fn(cara, x, y, cw, ch)
                if c is not None:
                    img.putpixel((x0 + x, y0 + y), c)


def fantasma():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    blanco = (236, 242, 250, 225)
    sombra = (176, 190, 214, 225)
    luz = (255, 255, 255, 235)

    def tono(x, y, cw, ch):
        # un poco más oscuro abajo y en los bordes: se ve redondo
        t = y / max(1, ch - 1) * 0.55 + (0.25 if x in (0, cw - 1) else 0)
        return mezcla(luz if y == 0 else blanco, sombra, min(1, t))

    # cabeza (6x5x6) en uv 0,0: ojos y boca en el frente
    def cabeza(cara, x, y, cw, ch):
        if cara in ("arriba",):
            return mezcla(luz, blanco, 0.3 if 1 <= x <= cw - 2 and 1 <= y <= ch - 2 else 0.8)
        if cara == "abajo":
            return sombra
        c = tono(x, y, cw, ch)
        if cara == "frente":
            if y in (1, 2) and x in (1, 4):
                # ojos de 1x2: abajo negro, arriba con un brillito azulado
                return (24, 18, 34, 255) if y == 2 else (62, 66, 104, 255)
            if y == 3 and x in (2, 3):
                return (60, 40, 70, 255)  # boquita
            if y == 2 and x in (0, 5):
                return (255, 170, 190, 160)  # cachetes
        return c

    caja(img, 0, 0, 6, 5, 6, cabeza)

    # falda (6x4x6) en uv 0,11: picos abajo (transparente en forma de sierra)
    def falda(cara, x, y, cw, ch):
        if cara in ("arriba", "abajo"):
            return None if cara == "abajo" else sombra
        if y == ch - 1 and x % 2 == 1:
            return None
        if y == ch - 2 and x % 4 == 3:
            return None
        return mezcla(blanco, sombra, 0.3 + y / ch * 0.6)

    caja(img, 0, 11, 6, 4, 6, falda)

    # gorrito de la sábana (4x1x4) en uv 0,22
    caja(img, 0, 22, 4, 1, 4, lambda c, x, y, cw, ch: luz)

    # bracitos (1x2x2) en uv 24,0 y 24,4
    for v in (0, 4):
        caja(img, 24, v, 1, 2, 2, lambda c, x, y, cw, ch: mezcla(blanco, sombra, y / 2))
    guardar(img, "entity/fantasma.png")


def ficha():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    oro_os = (150, 104, 30, 255)
    oro = (222, 172, 64, 255)
    oro_cl = (255, 226, 130, 255)
    porc = (236, 230, 222, 255)
    porc_os = (196, 186, 176, 255)
    rojo = (178, 18, 26, 255)
    rojo_cl = (232, 64, 70, 255)
    rojo_os = (110, 8, 16, 255)
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d > 7.4:
                continue
            if d > 6.3:
                img.putpixel((x, y), oro_cl if (x + y) < 12 else oro_os if (x + y) > 18 else oro)
            elif d > 5.6:
                img.putpixel((x, y), oro_os)
            else:
                img.putpixel((x, y), porc if (x + y) < 17 else porc_os)
    corazon = [
        "..##.##..",
        ".#######.",
        ".#######.",
        "..#####..",
        "...###...",
        "....#....",
    ]
    for j, fila in enumerate(corazon):
        for i, ch in enumerate(fila):
            if ch == "#":
                x, y = 4 + i, 5 + j
                c = rojo
                if j <= 1 and i in (2, 5):
                    c = rojo_cl
                elif j >= 3 or i >= 6:
                    c = rojo_os if (i + j) > 8 else rojo
                img.putpixel((x, y), c)
    # grieta fina de porcelana (el sello del Coleccionista)
    for (x, y) in [(11, 3), (11, 4), (12, 5)]:
        img.putpixel((x, y), porc_os)
    guardar(img, "item/ficha_vida.png")


if __name__ == "__main__":
    fantasma()
    ficha()
