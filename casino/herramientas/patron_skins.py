#!/usr/bin/env python3
"""Pieles de los clientes del casino (dealer_patron_0..11.png): señores de traje y señoras de vestido de noche, con distintos
tonos de piel, peinados y colores. Formato de piel de jugador 64x64.

    python3 casino/herramientas/patron_skins.py [--preview carpeta]
"""
import os
import random
import sys

from PIL import Image

E = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino", "textures", "entity")

SKIN = [(244, 208, 177), (226, 182, 143), (196, 146, 106), (152, 102, 70), (112, 74, 52)]
HAIR = {"black": (28, 24, 26), "brown": (86, 52, 30), "blonde": (214, 182, 106), "red": (150, 58, 30), "grey": (168, 168, 172)}

# (tipo, piel, pelo, color principal, color secundario, detalle)
PEOPLE = [
    ("man", 1, "black", (30, 40, 82), (240, 240, 236), (150, 20, 40)),      # traje azul, corbata roja
    ("woman", 0, "blonde", (186, 26, 44), None, (214, 170, 60)),           # vestido rojo
    ("man", 3, "black", (230, 228, 222), (240, 240, 236), (20, 20, 22)),    # esmoquin blanco, moño negro
    ("woman", 2, "black", (20, 118, 72), None, (230, 230, 230)),           # vestido esmeralda
    ("man", 0, "grey", (52, 52, 58), (240, 240, 236), (40, 70, 140)),       # traje gris, corbata azul (señor mayor)
    ("woman", 4, "black", (210, 168, 58), None, (250, 240, 200)),          # vestido dorado
    ("man", 2, "brown", (110, 26, 42), (240, 240, 236), (20, 20, 22)),      # saco vino
    ("woman", 1, "red", (36, 58, 168), None, (230, 230, 240)),             # vestido azul rey
    ("man", 4, "black", (24, 24, 28), (240, 240, 236), (210, 168, 58)),     # esmoquin negro, moño dorado
    ("woman", 0, "brown", (110, 40, 140), None, (220, 200, 240)),          # vestido morado
    ("man", 1, "blonde", (168, 138, 98), (240, 240, 236), (60, 90, 50)),    # traje camel
    ("woman", 3, "grey", (24, 24, 30), None, (200, 200, 210)),             # vestido negro (señora elegante)
]


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


class Skin:
    def __init__(self, seed):
        self.im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.rnd = random.Random(seed)

    def px(self, x, y, c, noise=6):
        n = self.rnd.randint(-noise, noise) if noise else 0
        self.im.putpixel((x, y), tuple(max(0, min(255, v + n)) for v in c[:3]) + (255,))

    def rect(self, x0, y0, w, h, c, noise=6):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                self.px(x, y, c, noise)

    def clear(self, x0, y0, w, h):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                self.im.putpixel((x, y), (0, 0, 0, 0))

    # partes: (u, v, ancho, alto, fondo) como en la piel de Minecraft
    def cube(self, u, v, w, h, d, faces):
        """faces: función (cara, x, y) -> color. Caras: top, bottom, right, front, left, back."""
        reg = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
               "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}
        for name, (x0, y0, ww, hh) in reg.items():
            for y in range(hh):
                for x in range(ww):
                    c = faces(name, x, y, ww, hh)
                    if c is not None:
                        self.px(x0 + x, y0 + y, c)


def make(i, kind, tone, hair, main, second, detail):
    s = Skin(100 + i)
    sk = SKIN[tone]
    hc = HAIR[hair]
    woman = kind == "woman"
    lip = (176, 60, 70) if woman else shade(sk, 0.78)

    def head(face, x, y, w, h):
        if face == "top":
            return hc
        if face == "bottom":
            return shade(sk, 0.85)
        if face == "back":
            return hc if (woman or y < 6) else sk
        if face in ("right", "left"):
            if woman:
                return hc if (y < 8 and not (y >= 3 and ((face == "right" and x >= 5) or (face == "left" and x <= 2)))) else sk
            return hc if y < 2 or (y < 4 and ((face == "right" and x < 3) or (face == "left" and x > 4))) else sk
        # cara
        if y == 0:
            return hc
        if y == 1:
            if woman:
                return hc if x in (0, 1, 6, 7) or i % 2 == 0 and x < 5 else sk
            return hc if x not in (3, 4) or i % 3 == 0 else sk
        if woman and x in (0, 7) and y < 6:
            return hc
        if y == 3 and x in (1, 2, 5, 6):
            return shade(hc, 0.75)
        if y == 4:
            if x in (1, 6):
                return (240, 240, 240)
            if x in (2, 5):
                return (40, 60, 110) if i % 3 else (70, 45, 25)
        if y == 5 and x in (3, 4):
            return shade(sk, 0.88)
        if y == 6 and x in (3, 4):
            return lip
        if not woman and i in (4, 6) and y >= 6 and x in (1, 2, 5, 6):
            return shade(hc, 0.9)          # barba / bigote
        return sk

    s.cube(0, 0, 8, 8, 8, head)

    # torso: traje (saco abierto, camisa y corbata) o vestido con escote
    def torso(face, x, y, w, h):
        if face == "front":
            if woman:
                if y < 2 and 2 <= x <= 5:
                    return sk                     # escote
                if y == 2 and 3 <= x <= 4:
                    return detail                 # collar
                if y == 6:
                    return shade(main, 0.75)      # cintura
                return main
            if 3 <= x <= 4:
                if y == 0:
                    return detail                 # moño / nudo
                return detail if (second and y < 8 and x == 3 + (y % 2) * 0) else second
            if x in (2, 5) and y < 6:
                return shade(main, 0.8)           # solapa
            if y == 7 and x in (1, 6):
                return shade(main, 0.7)           # bolsillo
            return main
        if face == "top":
            return sk if woman and 2 <= x <= 5 else main
        return shade(main, 0.92)

    s.cube(16, 16, 8, 12, 4, torso)

    # brazos: mangas del saco con puño blanco; ellas sin mangas (y una pulsera)
    def arm(face, x, y, w, h):
        if face == "bottom":
            return sk
        if woman:
            if y == 9:
                return detail
            return sk if not (y < 2 and face in ("top", "right", "left", "front", "back")) else main
        if y >= 10:
            return sk
        if y == 9:
            return second
        return main if face != "top" else main

    s.cube(40, 16, 4, 12, 4, arm)
    s.cube(32, 48, 4, 12, 4, arm)

    # piernas: pantalón y zapatos; ellas: vestido hasta la rodilla, piernas y tacones
    def leg(face, x, y, w, h):
        if face == "bottom":
            return (20, 20, 22)
        if woman:
            if y < 7:
                return main if face != "back" else shade(main, 0.9)
            if y >= 11:
                return shade(detail, 0.6) if face != "top" else main
            return shade(sk, 0.95)
        if y >= 11:
            return (22, 20, 20)
        return shade(main, 0.82)

    s.cube(0, 16, 4, 12, 4, leg)
    s.cube(16, 48, 4, 12, 4, leg)

    # cabello largo por encima (capa del sombrero) en las señoras
    if woman:
        def hat(face, x, y, w, h):
            if face in ("back",):
                return hc
            if face in ("right", "left") and y < 7:
                return hc if (face == "right" and x < 5) or (face == "left" and x > 2) else None
            if face == "front" and y == 0:
                return hc
            if face == "top":
                return hc
            return None
        s.cube(32, 0, 8, 8, 8, hat)
    return s.im


def preview(skins, out):
    """Muñequitos de frente (cabeza, torso, brazos y piernas) para revisar los colores."""
    sc = 8
    im = Image.new("RGBA", (len(skins) * 18 * sc, 34 * sc), (60, 60, 70, 255))
    for k, s in enumerate(skins):
        ox = k * 18 * sc + 1 * sc
        parts = [((8, 8, 8, 8), (4, 0)), ((40, 8, 8, 8), (4, 0)), ((20, 20, 8, 12), (4, 8)), ((44, 20, 4, 12), (0, 8)), ((36, 52, 4, 12), (12, 8)),
                 ((4, 20, 4, 12), (4, 20)), ((20, 52, 4, 12), (8, 20))]
        for (u, v, w, h), (dx, dy) in parts:
            piece = s.crop((u, v, u + w, v + h)).resize((w * sc, h * sc), Image.NEAREST)
            im.alpha_composite(piece, (ox + dx * sc, sc + dy * sc))
    im.save(out)


def main():
    skins = []
    for i, p in enumerate(PEOPLE):
        im = make(i, *p)
        im.save(os.path.join(E, f"dealer_patron_{i}.png"))
        skins.append(im)
    if "--preview" in sys.argv:
        preview(skins, os.path.join(sys.argv[sys.argv.index("--preview") + 1], "patrons.png"))
    print(len(skins), "pieles")


if __name__ == "__main__":
    main()
