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
    """La piel del Coleccionista disfrazado (y sus ojos y el sombrero): ahora la pinta personajes/coleccionista_tv.py al triple de detalle,
    con los mismos dibujos que el Coleccionista de la tele."""
    import importlib.util
    ruta = os.path.join(os.path.dirname(os.path.abspath(__file__)), "personajes", "coleccionista_tv.py")
    spec = importlib.util.spec_from_file_location("coleccionista_tv", ruta)
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    piel, ojos, som = m.disfraz()
    guardar(piel, "entity/coleccionista.png")
    guardar(ojos, "entity/coleccionista_ojos.png")
    guardar(som, "entity/sombrero.png")


def encadenados():
    """La llave del grillete (16x16, hierro viejo con óxido) y la niebla del gas (128x128, se pinta verde con el color del vértice)."""
    import random, math
    r = random.Random(77)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    hierro = [(92, 88, 84, 255), (122, 116, 108, 255), (150, 144, 134, 255), (60, 56, 54, 255)]
    oxido = [(126, 62, 30, 255), (150, 82, 40, 255), (98, 48, 26, 255)]
    def px(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), c)
    # el ojo (anillo) arriba a la izquierda
    for y in range(1, 8):
        for x in range(1, 8):
            d = math.hypot(x - 4, y - 4)
            if 1.6 <= d <= 3.2:
                c = hierro[2] if (x + y) < 7 else hierro[1] if d < 2.6 else hierro[0]
                if r.random() < 0.18:
                    c = r.choice(oxido)
                px(x, y, c)
    # el vástago en diagonal hacia abajo a la derecha
    for i in range(0, 9):
        x, y = 6 + i, 6 + i
        px(x, y, hierro[1] if i % 3 else hierro[2])
        px(x - 1, y, hierro[0])
        if r.random() < 0.25:
            px(x, y, r.choice(oxido))
    # los dientes
    for (x, y) in [(12, 9), (13, 9), (13, 10), (10, 12), (9, 12), (9, 13)]:
        px(x, y, hierro[3] if y > 11 else hierro[1])
    for (x, y) in [(11, 10), (14, 10)]:
        px(x, y, oxido[0])
    guardar(img, "item/llave_grillete.png")
    # la niebla del gas: ruido suave que se repite, en grises (el color lo pone el dibujo)
    n = 128
    capas = []
    for esc in (8, 16, 32):
        rejilla = [[r.random() for _ in range(n // esc + 1)] for _ in range(n // esc + 1)]
        capas.append((esc, rejilla))
    gas = Image.new("RGBA", (n, n))
    for y in range(n):
        for x in range(n):
            v = 0
            pesos = 0
            for k, (esc, rej) in enumerate(capas):
                gx, gy = x / esc, y / esc
                x0, y0 = int(gx) % (n // esc), int(gy) % (n // esc)
                x1, y1 = (x0 + 1) % (n // esc), (y0 + 1) % (n // esc)
                fx, fy = gx - int(gx), gy - int(gy)
                fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
                a = rej[y0][x0] * (1 - fx) + rej[y0][x1] * fx
                b = rej[y1][x0] * (1 - fx) + rej[y1][x1] * fx
                w = (k + 1)
                v += (a * (1 - fy) + b * fy) * w
                pesos += w
            v /= pesos
            g = int(150 + 105 * v)
            al = int(max(0, min(255, (v - 0.22) * 330)))
            gas.putpixel((x, y), (g, g, g, al))
    guardar(gas, "misc/gas.png")


def cartas():
    """Las cartas de Compartir o robar (64x96): el dorso (rojo con la máscara), COMPARTIR (moneda partida en dos mitades iguales) y
    ROBAR (una mano negra que se lleva la moneda entera). El texto lo pone el juego (para que se traduzca)."""
    import math, random
    r = random.Random(5)
    W, H = 64, 96
    oro = (222, 172, 64, 255)
    oro2 = (176, 128, 40, 255)
    oro3 = (255, 222, 130, 255)
    def base(fondo, fondo2, borde):
        img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        for y in range(H):
            for x in range(W):
                # esquinas redondeadas
                if (x < 2 or x > W - 3) and (y < 2 or y > H - 3):
                    continue
                t = y / H
                c = mezcla(fondo, fondo2, t)
                if r.random() < 0.06:
                    c = mezcla(c, (0, 0, 0, 255), 0.12)
                img.putpixel((x, y), c)
        for x in range(2, W - 2):
            for y in (1, 2, H - 3, H - 2):
                img.putpixel((x, y), borde)
        for y in range(2, H - 2):
            for x in (1, 2, W - 3, W - 2):
                img.putpixel((x, y), borde)
        # filete interior
        for x in range(5, W - 5):
            img.putpixel((x, 5), mezcla(borde, (0, 0, 0, 255), 0.35))
            img.putpixel((x, H - 6), mezcla(borde, (0, 0, 0, 255), 0.35))
        for y in range(5, H - 5):
            img.putpixel((5, y), mezcla(borde, (0, 0, 0, 255), 0.35))
            img.putpixel((W - 6, y), mezcla(borde, (0, 0, 0, 255), 0.35))
        return img
    def moneda(img, cx, cy, rad, desde=0.0, hasta=2 * math.pi, dx=0):
        for y in range(cy - rad - 1, cy + rad + 2):
            for x in range(cx - rad - 1, cx + rad + 2):
                d = math.hypot(x - cx, y - cy)
                if d > rad + 0.3:
                    continue
                a = math.atan2(y - cy, x - cx) % (2 * math.pi)
                if not (desde <= a <= hasta or (hasta > 2 * math.pi and a <= hasta - 2 * math.pi)):
                    continue
                c = oro2 if d > rad - 1.4 else oro
                if d < rad * 0.55 and abs((x - cx) + (y - cy)) < 2:
                    c = oro3
                img.putpixel((x + dx, y), c)
    # el dorso
    dorso = base((120, 14, 22, 255), (60, 6, 12, 255), oro)
    for y in range(8, H - 8):
        for x in range(8, W - 8):
            if (x + y) % 6 == 0 or (x - y) % 6 == 0:
                dorso.putpixel((x, y), (150, 30, 36, 255))
    # la máscara al centro
    for y in range(34, 64):
        for x in range(18, 46):
            dx, dy = (x - 32) / 13.0, (y - 48) / 15.0
            if dx * dx + dy * dy <= 1:
                c = (232, 226, 216, 255)
                if (abs(x - 26) <= 2 or abs(x - 37) <= 2) and 43 <= y <= 46:
                    c = (20, 10, 12, 255)
                if 55 <= y <= 56 and 27 <= x <= 37:
                    c = (60, 20, 24, 255)
                dorso.putpixel((x, y), c)
    guardar(dorso, "gui/carta_dorso.png")
    # compartir: crema con borde dorado, la moneda partida en dos (las mitades separadas)
    comp = base((236, 226, 204, 255), (204, 188, 160, 255), oro)
    moneda(comp, 32, 46, 15, math.pi / 2, 3 * math.pi / 2, dx=-3)
    moneda(comp, 32, 46, 15, 3 * math.pi / 2, 5 * math.pi / 2, dx=3)
    guardar(comp, "gui/carta_compartir.png")
    # robar: negra con borde rojo, la moneda entera y una mano negra con dedos que la toma desde arriba
    rob = base((30, 22, 26, 255), (10, 6, 8, 255), (200, 30, 40, 255))
    moneda(rob, 32, 54, 15)
    mano = (6, 4, 6, 255)
    for dedo, (fx, largo) in enumerate([(22, 16), (28, 20), (34, 20), (40, 16)]):
        for y in range(22, 22 + largo):
            for x in range(fx, fx + 4):
                rob.putpixel((x, y), mano)
        # uñas rojas
        for x in range(fx, fx + 4):
            rob.putpixel((x, 22 + largo - 1), (150, 20, 28, 255))
    for y in range(10, 26):
        for x in range(20, 46):
            rob.putpixel((x, y), mano)
    guardar(rob, "gui/carta_robar.png")


if __name__ == "__main__":
    cartas()
    encadenados()
    fantasma()
    retrato()
    estatica()
    bloques()
    skin()
