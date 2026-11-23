#!/usr/bin/env python3
"""Pieles de las Personas de muestra del Core (64x64, formato de jugador) con sus casillas de expresiones:
(0,0) ojos cerrados · (24,0) boca abierta · (32,0) ojos felices · (56,0) sonrisa grande. Solo se pintan los píxeles que cambian.

    python3 axelis_core/herramientas/pieles.py
"""
import os
import random

from PIL import Image

E = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "axelis_core", "textures",
                 "entity", "persona")

SKIN = [(246, 212, 182), (230, 186, 148), (200, 150, 110), (156, 106, 74), (110, 72, 50)]


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def sh(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])


class Skin:
    def __init__(self, seed):
        self.im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.rnd = random.Random(seed)

    def px(self, x, y, c, noise=5, a=255):
        if c is None:
            return
        n = self.rnd.randint(-noise, noise) if noise else 0
        self.im.putpixel((x, y), tuple(max(0, min(255, v + n)) for v in c[:3]) + (a,))

    def get(self, x, y):
        return self.im.getpixel((x, y))

    def box(self, u, v, w, h, d, paint):
        """Pinta una caja (UV de Minecraft). paint(cara, x, y, w, h) -> color o None. Las caras: top, bottom, right, front, left, back
        (right = la primera franja, que es el lado derecho del personaje; front = la cara que mira al frente)."""
        reg = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
               "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}
        for face, (x0, y0, ww, hh) in reg.items():
            k = {"top": 1.12, "bottom": 0.72, "right": 0.88, "front": 1.0, "left": 0.88, "back": 0.8}[face]
            for y in range(hh):
                for x in range(ww):
                    c = paint(face, x, y, ww, hh)
                    if c is not None:
                        self.px(x0 + x, y0 + y, sh(c, k))

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.im.save(path)


def persona(seed, p):
    """p: piel, pelo, ojos, ropa principal, secundaria, pantalón, zapatos, estilo de pelo, extras."""
    s = Skin(seed)
    sk = SKIN[p["piel"]]
    hair = p["pelo"]
    eye = p["ojos"]
    top, sec, pants, shoes = p["ropa"], p["ropa2"], p["pantalon"], p["zapatos"]
    lip = sh(sk, 0.72)

    # ------------------------------------------------ cabeza
    long_hair = p.get("largo", False)

    def head(f, x, y, w, h):
        if f == "top":
            return sh(hair, 1.0 - 0.05 * ((x + y) % 2))
        if f == "bottom":
            return sk
        if f == "back":
            return hair if y < (8 if long_hair else 6) else sk
        if f in ("right", "left"):
            if y < 2 or (long_hair and y < 8 and (x < 3 if f == "right" else x > 4)):
                return hair
            if y < 4 and (x > 4 if f == "right" else x < 3):
                return hair
            if y == 4 and (x == 1 if f == "right" else x == 6):
                return sh(sk, 0.85)  # oreja
            return sk
        # cara
        if y < 2:
            return hair
        if y == 2 and (x < 1 or x > 6):
            return hair
        if p.get("flequillo") and y == 2 and 1 <= x <= 4:
            return hair
        if y == 3 and x in (1, 2, 5, 6):
            return sh(hair, 0.8)  # cejas
        if y == 4:
            if x in (1, 5):
                return (240, 240, 240)
            if x in (2, 6):
                return eye
        if y == 5 and x in (3, 4):
            return sh(sk, 0.9)  # nariz
        if y == 6 and 2 <= x <= 5:
            return lip if x in (3, 4) else sh(sk, 0.9)
        if p.get("barba") and y >= 6:
            return sh(hair, 0.9) if not (y == 6 and x in (3, 4)) else lip
        return sk

    s.box(0, 0, 8, 8, 8, head)

    # sombrero / capa de pelo
    hat = p.get("gorro")

    def hatl(f, x, y, w, h):
        if hat:
            col = hat
            if f == "front" and y <= 1:
                return col
            if f in ("right", "left", "back") and y <= 1:
                return col
            if f == "top":
                return sh(col, 1.0 - 0.04 * ((x * 3 + y) % 3))
            if f == "front" and y == 2 and p.get("visera"):
                return sh(col, 0.75)
            return None
        if long_hair and f in ("right", "left", "back") and y >= 6:
            return sh(hair, 0.92)
        return None

    s.box(32, 0, 8, 8, 8, hatl)

    # ------------------------------------------------ cuerpo
    def body(f, x, y, w, h):
        if f == "top" or f == "bottom":
            return top
        if y >= 10:
            return p.get("cinturon", sh(top, 0.7)) if y == 10 else pants
        if f == "front":
            if p.get("cuello") and y == 0 and 2 <= x <= 5:
                return sk
            if p.get("tirantes") and x in (1, 6):
                return sec
            if p.get("delantal") and 1 <= x <= 6 and y >= 2:
                return sec
            if p.get("botones") and x == 4 and y in (2, 4, 6, 8):
                return sec
            if p.get("franja") and y in (3, 4):
                return sec
        if f == "back" and p.get("mochila") and 1 <= x <= 6 and 1 <= y <= 8:
            return p["mochila"]
        return top

    s.box(16, 16, 8, 12, 4, body)

    def jacket(f, x, y, w, h):
        if p.get("capa") and f == "back":
            return p["capa"]
        if p.get("chaleco") and f in ("front",) and (x < 2 or x > 5) and y < 10:
            return p["chaleco"]
        return None

    s.box(16, 32, 8, 12, 4, jacket)

    # ------------------------------------------------ brazos
    def arm(f, x, y, w, h):
        if f == "bottom":
            return sk
        if f == "top":
            return top
        if y >= 11:
            return sk
        if y < p.get("manga", 6):
            return top if not (y == p.get("manga", 6) - 1 and p.get("puno")) else p["puno"]
        return sk

    s.box(40, 16, 4, 12, 4, arm)
    s.box(32, 48, 4, 12, 4, arm)

    # ------------------------------------------------ piernas
    def leg(f, x, y, w, h):
        if f == "top":
            return pants
        if f == "bottom":
            return sh(shoes, 0.8)
        if y >= 10:
            return shoes
        if p.get("falda") and y < 6:
            return p["falda"]
        return pants

    s.box(0, 16, 4, 12, 4, leg)
    s.box(16, 48, 4, 12, 4, leg)

    # ------------------------------------------------ expresiones (solo lo que cambia sobre la cara)
    face = lambda x, y: s.get(8 + x, 8 + y)  # noqa: E731
    lid = sh(sk, 0.96)
    lash = sh(hair, 0.6)
    # (0,0) ojos cerrados
    for x in (1, 2, 5, 6):
        s.px(0 + x, 4, lash, 0)
    # (24,0) boca abierta: oscura con lengua
    for x in range(2, 6):
        s.px(24 + x, 6, (60, 24, 28), 0)
    s.px(24 + 3, 7, (176, 74, 82), 0)
    s.px(24 + 4, 7, (176, 74, 82), 0)
    if not p.get("barba"):
        for x in (2, 5):
            s.px(24 + x, 7, sk, 0)
    # (32,0) ojos felices (^ ^)
    for (x0, x1) in ((1, 2), (5, 6)):
        s.px(32 + x0, 4, sk, 0)
        s.px(32 + x1, 4, sk, 0)
        s.px(32 + x0, 4, lash, 0)
        s.px(32 + x1, 3, lash, 0)
    # (56,0) sonrisa grande
    for x in range(1, 7):
        s.px(56 + x, 6, (70, 30, 32) if 2 <= x <= 5 else lip, 0)
    for x in range(2, 6):
        s.px(56 + x, 7, (240, 240, 236) if x in (3, 4) else sk, 0)
    return s


PERSONAS = {
    # exploradora: chaqueta verde, mochila, trenza larga castaña
    "mara": dict(piel=1, pelo=(110, 64, 34), ojos=(58, 110, 64), ropa=(72, 118, 66), ropa2=(196, 168, 112), pantalon=(92, 76, 58),
                 zapatos=(70, 46, 30), largo=True, mochila=(150, 104, 58), botones=True, cinturon=(60, 44, 28), puno=(196, 168, 112)),
    # herrero: piel morena, delantal de cuero, brazos descubiertos, barba
    "iker": dict(piel=3, pelo=(32, 26, 24), ojos=(70, 46, 30), ropa=(160, 74, 52), ropa2=(102, 70, 44), pantalon=(60, 60, 66),
                 zapatos=(40, 34, 30), delantal=True, barba=True, manga=2, cinturon=(44, 34, 26)),
    # mercader: túnica morada con franja dorada, gorro
    "lu": dict(piel=0, pelo=(40, 30, 50), ojos=(90, 60, 140), ropa=(110, 52, 140), ropa2=(222, 182, 70), pantalon=(80, 40, 104),
               zapatos=(60, 40, 30), franja=True, gorro=(140, 66, 170), cuello=True, puno=(222, 182, 70), manga=11, flequillo=True),
    # pescador: impermeable amarillo, gorra, barba canosa
    "nilo": dict(piel=2, pelo=(176, 176, 172), ojos=(60, 90, 140), ropa=(232, 196, 52), ropa2=(70, 60, 40), pantalon=(56, 74, 110),
                 zapatos=(34, 48, 70), barba=True, gorro=(48, 74, 120), visera=True, tirantes=True, manga=11, puno=(200, 166, 40)),
    # aldeana: vestido con delantal, flores
    "sol": dict(piel=4, pelo=(24, 18, 16), ojos=(80, 52, 34), ropa=(214, 92, 84), ropa2=(244, 236, 214), pantalon=(214, 92, 84),
                zapatos=(110, 60, 40), largo=True, delantal=True, falda=(214, 92, 84), cuello=True, manga=4, flequillo=True),
}

if __name__ == "__main__":
    for i, (name, p) in enumerate(PERSONAS.items()):
        persona(500 + i, p).save(os.path.join(E, name + ".png"))
    print("pieles:", ", ".join(PERSONAS))
