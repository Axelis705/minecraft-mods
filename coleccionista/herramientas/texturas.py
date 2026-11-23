#!/usr/bin/env python3
"""
Texturas de Los Juegos del Coleccionista (pixel art hecho a mano en código).

    python3 coleccionista/herramientas/texturas.py

Genera:
  - textures/entity/fantasma.png (32x32): el fantasmita de los eliminados (sábana, ojos, boca, falda con picos).
  - textures/pantalla/coleccionista.png (64x64): el Coleccionista en la pantalla; textures/pantalla/estatica.png: la estática.
  - textures/block/pantalla.png, candado.png, foco_apagado.png, foco_encendido.png.
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


def retrato():
    """El Coleccionista en la pantalla: máscara de porcelana con grieta, ojos huecos con un punto rojo, sombrero de copa y traje."""
    W = 64
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    porc = (238, 232, 222, 255)
    porc_s = (204, 196, 186, 255)
    porc_ss = (168, 158, 150, 255)
    negro = (14, 12, 16, 255)
    traje = (28, 24, 32, 255)
    traje_l = (48, 42, 56, 255)
    rojo = (170, 16, 24, 255)
    rojo_cl = (228, 54, 60, 255)
    cx = 31.5
    # traje y hombros
    for y in range(46, 64):
        ancho = 14 + (y - 46) * 1.4
        for x in range(64):
            if abs(x - cx) <= ancho:
                img.putpixel((x, y), traje_l if abs(x - cx) > ancho - 1.5 else traje)
    # camisa y corbata de moño roja
    for y in range(46, 64):
        for x in range(64):
            d = abs(x - cx)
            if d <= 4 - (y - 46) * 0.05 and y < 60:
                img.putpixel((x, y), (220, 214, 206, 255))
    for y in range(47, 52):
        for x in range(64):
            d = abs(x - cx)
            if d <= 1.2 or (2 <= d <= 5 and abs(y - 49.5) <= 2.6 - (d - 2) * 0.3):
                img.putpixel((x, y), rojo if d > 1.2 else rojo_cl)
    # cuello
    for y in range(40, 47):
        for x in range(int(cx - 5), int(cx + 6)):
            img.putpixel((x, y), (40, 36, 40, 255))
    # máscara (óvalo)
    for y in range(12, 44):
        for x in range(64):
            dx = (x - cx) / 13.5
            dy = (y - 27.5) / 16.5
            if dx * dx + dy * dy <= 1:
                sombra = 0.0
                if dx > 0.45:
                    sombra = (dx - 0.45) * 1.6
                if dy > 0.6:
                    sombra = max(sombra, (dy - 0.6) * 1.8)
                c = porc if sombra < 0.35 else porc_s if sombra < 0.75 else porc_ss
                img.putpixel((x, y), c)
    # ojos huecos con el punto rojo
    for ox in (24, 39):
        for y in range(23, 30):
            for x in range(ox - 3, ox + 4):
                dx = (x - ox) / 3.4
                dy = (y - 26.3) / 3.0
                if dx * dx + dy * dy <= 1:
                    img.putpixel((x, y), negro)
        img.putpixel((ox, 26), rojo_cl)
        img.putpixel((ox, 27), rojo)
        # ceja pintada
        for x in range(ox - 3, ox + 4):
            img.putpixel((x, 20 - (1 if abs(x - ox) < 2 else 0)), (60, 50, 50, 255))
    # sonrisa pintada fina y mejillas
    for x in range(26, 38):
        y = 36 + int(round(((x - 31.5) / 6) ** 2 * -1.6 + 1.0))
        img.putpixel((x, y), (90, 30, 36, 255))
    for ox in (21, 42):
        for x in range(ox - 1, ox + 2):
            img.putpixel((x, 32), (232, 170, 176, 255))
    # la grieta
    for (x, y) in [(36, 13), (36, 14), (37, 15), (37, 16), (36, 17), (37, 18), (38, 19), (38, 20), (37, 21)]:
        img.putpixel((x, y), (120, 112, 106, 255))
    # sombrero de copa
    for y in range(0, 14):
        for x in range(64):
            d = abs(x - cx)
            if (y >= 11 and d <= 18) or (y < 11 and d <= 11):
                c = negro if not (y == 9 or y == 10) else rojo
                img.putpixel((x, y), c)
    guardar(img, "pantalla/coleccionista.png")


def estatica():
    import random
    rnd = random.Random(7)
    img = Image.new("RGBA", (64, 64))
    for y in range(64):
        fila = rnd.random() * 40
        for x in range(64):
            v = int(rnd.random() * 200 + fila)
            v = min(255, v)
            img.putpixel((x, y), (v, v, v, 255))
    guardar(img, "pantalla/estatica.png")


def bloques():
    # marco de la pantalla (plástico negro con brillo arriba)
    img = Image.new("RGBA", (16, 16), (16, 16, 20, 255))
    for x in range(16):
        img.putpixel((x, 0), (52, 52, 60, 255))
        img.putpixel((x, 15), (8, 8, 10, 255))
    for y in range(16):
        img.putpixel((0, y), (40, 40, 46, 255))
        img.putpixel((15, y), (8, 8, 10, 255))
    guardar(img, "block/pantalla.png")
    # candado: placa de latón con 9 botones y un foquito rojo
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    laton = (176, 132, 58, 255)
    laton_o = (118, 84, 34, 255)
    laton_c = (226, 186, 104, 255)
    for y in range(16):
        for x in range(16):
            c = laton
            if x == 0 or y == 0:
                c = laton_c
            if x == 15 or y == 15:
                c = laton_o
            img.putpixel((x, y), c)
    for j in range(3):
        for i in range(3):
            bx, by = 3 + i * 4, 5 + j * 3
            img.putpixel((bx, by), (40, 34, 30, 255))
            img.putpixel((bx + 1, by), (40, 34, 30, 255))
            img.putpixel((bx, by + 1), (20, 16, 14, 255))
            img.putpixel((bx + 1, by + 1), (20, 16, 14, 255))
    for x in range(3, 13):
        img.putpixel((x, 2), (24, 40, 30, 255))
    img.putpixel((12, 2), (200, 30, 30, 255))
    guardar(img, "block/candado.png")
    # foco: cristal y metal (apagado y prendido)
    for nombre, cristal, brillo in (("foco_apagado", (70, 66, 58, 255), (96, 90, 80, 255)),
                                    ("foco_encendido", (255, 214, 120, 255), (255, 246, 200, 255))):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y), (54, 50, 48, 255) if y < 6 else cristal)
        for y in range(7, 13):
            img.putpixel((6, y), brillo)
            img.putpixel((7, y), brillo)
        for x in range(16):
            img.putpixel((x, 5), (30, 28, 28, 255))
        guardar(img, "block/" + nombre + ".png")


def skin():
    """La piel del Coleccionista (64x64, como la de un jugador): máscara de porcelana, traje negro, camisa, moño rojo, guantes blancos."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    porc, porc_s = (238, 232, 222, 255), (206, 198, 188, 255)
    negro, traje, traje_l = (14, 12, 16, 255), (30, 26, 34, 255), (46, 40, 52, 255)
    camisa, rojo, rojo_cl = (226, 220, 212, 255), (170, 16, 24, 255), (228, 54, 60, 255)
    guante = (236, 234, 228, 255)

    def rect(x0, y0, w, h, c):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                img.putpixel((x, y), c(x - x0, y - y0, w, h) if callable(c) else c)

    # cabeza (u 0, v 0): arriba 8..16 x 0..8, frente 8..16 x 8..16, lados y atrás
    rect(8, 0, 8, 8, porc)                       # arriba de la cabeza (lo tapa el sombrero)
    rect(16, 0, 8, 8, porc_s)                    # abajo
    rect(0, 8, 8, 8, lambda x, y, w, h: porc_s if x < 2 else porc)   # derecha
    rect(16, 8, 8, 8, lambda x, y, w, h: porc_s if x > 5 else porc)  # izquierda
    rect(24, 8, 8, 8, negro)                     # atrás: el pelo negro
    def cara(x, y, w, h):
        if y in (3, 4) and x in (1, 2, 5, 6):
            return negro                         # ojos huecos
        if y == 2 and x in (1, 2, 5, 6):
            return (70, 60, 60, 255)             # cejas pintadas
        if y == 6 and 2 <= x <= 5:
            return (120, 40, 46, 255)            # sonrisa fina
        if y == 5 and x in (0, 7):
            return (232, 170, 176, 255)          # mejillas
        return porc
    rect(8, 8, 8, 8, cara)
    img.putpixel((9, 12), rojo_cl)               # los puntos rojos en los ojos
    img.putpixel((14, 12), rojo_cl)
    for (x, y) in [(13, 8), (13, 9), (14, 10)]:   # la grieta
        img.putpixel((x, y), (130, 120, 114, 255))
    # torso (u 16, v 16): frente 20..28 x 20..32
    rect(20, 16, 8, 4, traje)                    # hombros arriba
    rect(28, 16, 8, 4, traje)
    rect(16, 20, 4, 12, traje)
    rect(28, 20, 4, 12, traje)
    rect(32, 20, 8, 12, lambda x, y, w, h: traje_l if y > 9 else traje)
    def frente(x, y, w, h):
        if 3 <= x <= 4 and y <= 8:
            return camisa
        if y in (0, 1) and 1 <= x <= 6:
            return rojo if x not in (3, 4) else rojo_cl  # moño
        if (x == 2 or x == 5) and y <= 6:
            return traje_l                       # solapas
        if x in (3, 4) and y in (5, 8):
            return negro                         # botones
        return traje
    rect(20, 20, 8, 12, frente)
    # brazo derecho (u 40, v 16) y piernas (u 0, v 16): traje, guantes y zapatos
    def brazo(x, y, w, h):
        return guante if y >= 9 else camisa if y == 8 else traje
    rect(44, 16, 4, 4, traje)
    rect(48, 16, 4, 4, guante)
    rect(40, 20, 16, 12, brazo)
    rect(36, 48, 4, 4, traje)                    # brazo izquierdo (u 32, v 48)
    rect(40, 48, 4, 4, guante)
    rect(32, 52, 16, 12, brazo)
    def pierna(x, y, w, h):
        return negro if y >= 10 else traje
    rect(4, 16, 4, 4, traje)
    rect(8, 16, 4, 4, negro)
    rect(0, 20, 16, 12, pierna)
    rect(20, 48, 4, 4, traje)                    # pierna izquierda (u 16, v 48)
    rect(24, 48, 4, 4, negro)
    rect(16, 52, 16, 12, pierna)
    guardar(img, "entity/coleccionista.png")
    # el sombrero de copa (textura de 32x16: copa negra, cinta roja, ala)
    som = Image.new("RGBA", (32, 16), negro)
    for x in range(32):
        som.putpixel((x, 9), rojo)
        som.putpixel((x, 10), rojo)
        som.putpixel((x, 0), (34, 30, 38, 255))
    guardar(som, "entity/sombrero.png")


if __name__ == "__main__":
    fantasma()
    retrato()
    estatica()
    bloques()
    skin()
