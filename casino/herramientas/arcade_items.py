#!/usr/bin/env python3
"""Monedas arcade y boletos de la sala recreativa (casino Divertido): texturas de 16x16 y modelos de objeto.

    python3 casino/herramientas/arcade_items.py
"""
import json
import math
import os

from PIL import Image

A = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino")


def coin():
    """Moneda dorada con canto morado neón y una estrella en medio (estilo años 80)."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx = cy = 7.5
    pts = []
    for k in range(10):                          # estrella rellena de 5 picos
        a = -math.pi / 2 + k * math.pi / 5
        r = 4.6 if k % 2 == 0 else 2.0
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))

    def inside(px, py):
        n, j, ok = len(pts), len(pts) - 1, False
        for i in range(n):
            xi, yi = pts[i]
            xj, yj = pts[j]
            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
                ok = not ok
            j = i
        return ok
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d > 7.2:
                continue
            if d > 6.4:
                c = (120, 30, 150) if (x + y) % 2 else (150, 46, 186)            # canto morado
            elif d > 5.4:
                c = (250, 222, 110) if y < 8 else (212, 160, 40)                   # aro dorado con brillo arriba
            else:
                c = (232, 186, 58) if (x - y) % 5 else (246, 210, 90)
            if inside(x, y) and d <= 5.4:
                c = (255, 64, 190)                                                 # estrella rosa neón
            if (x, y) in ((5, 3), (4, 4)):
                c = (255, 248, 210)                                                # destello
            im.putpixel((x, y), c + (255,))
    im.save(os.path.join(A, "textures", "item", "arcade_coin.png"))


def ticket():
    """Boleto de feria: tira naranja en diagonal con orilla de perforaciones y una estrella."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            u = (x - y) / math.sqrt(2)                   # a lo largo de la tira
            v = (x + y - 15) / math.sqrt(2)              # a lo ancho
            if abs(v) > 3.6 or abs(u) > 7.2:
                continue
            c = (246, 128, 40)
            if abs(v) > 2.8:
                c = (200, 70, 30)                                                  # orilla
            if abs(abs(u) - 4.6) < 0.5 and abs(v) < 2.8:
                c = (255, 214, 150)                                                # línea punteada de corte
            if abs(u) < 1.2 and abs(v) < 1.2:
                c = (255, 240, 120)                                                # estrella / sello
            im.putpixel((x, y), c + (255,))
    # muescas de las puntas (las esquinas del boleto)
    for x, y in ((1, 1), (14, 14)):
        if im.getpixel((x, y))[3]:
            im.putpixel((x, y), (0, 0, 0, 0))
    im.save(os.path.join(A, "textures", "item", "arcade_ticket.png"))


def models():
    for n in ("arcade_coin", "arcade_ticket"):
        with open(os.path.join(A, "models", "item", n + ".json"), "w", encoding="utf-8") as fh:
            json.dump({"parent": "item/generated", "textures": {"layer0": "casino:item/" + n}}, fh, indent=1)


if __name__ == "__main__":
    coin()
    ticket()
    models()
    print("monedas arcade y boletos listos")
