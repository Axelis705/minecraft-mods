#!/usr/bin/env python3
"""Las cartas del veintiuno (la final): del 1 al 11 y el reverso, en papel viejo.

Escribe textures/entity/cartas21.png (512 x 256): 8 cartas por renglón de 64 x 96; la 0 es el reverso (vino con orillas doradas y el ojo
del Coleccionista), la 1 a la 11 son las caras (número grande en rojo oscuro, números chiquitos en las esquinas, marco fino).
Usa la letra DejaVu Serif Bold del sistema (licencia libre).

Uso: python3 herramientas/cartas21.py
"""
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

AQUI = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'textures', 'entity', 'cartas21.png')
W, H = 64, 96
LETRA = '/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf'
R = np.random.RandomState(21)


def papel():
    base = np.array([233, 223, 200], float)
    ruido = R.rand(H, W, 1)
    yy, xx = np.mgrid[0:H, 0:W]
    # amarillento hacia las orillas, con manchitas
    orilla = np.minimum(np.minimum(xx, W - 1 - xx), np.minimum(yy, H - 1 - yy))[..., None]
    rgb = base * (0.9 + 0.1 * ruido) * (0.9 + 0.1 * np.clip(orilla / 8, 0, 1))
    rgb[..., 2] *= 0.94
    for _ in range(3):
        cx, cy, r = R.randint(0, W), R.randint(0, H), R.randint(3, 8)
        m = np.hypot(xx - cx, yy - cy) < r
        rgb[m] *= 0.93
    img = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), 'RGB').convert('RGBA')
    # las esquinas redondeadas
    a = Image.new('L', (W, H), 0)
    ImageDraw.Draw(a).rounded_rectangle((0, 0, W - 1, H - 1), radius=5, fill=255)
    img.putalpha(a)
    return img


def cara(n):
    img = papel()
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((3, 3, W - 4, H - 4), radius=4, outline=(110, 22, 26, 255), width=1)
    d.rounded_rectangle((5, 5, W - 6, H - 6), radius=3, outline=(160, 120, 60, 200), width=1)
    grande = ImageFont.truetype(LETRA, 40 if n < 10 else 32)
    chica = ImageFont.truetype(LETRA, 11)
    s = str(n)
    # el número grande al centro, con una sombrita
    bb = d.textbbox((0, 0), s, font=grande)
    x, y = (W - (bb[2] - bb[0])) / 2 - bb[0], (H - (bb[3] - bb[1])) / 2 - bb[1]
    d.text((x + 1, y + 1), s, font=grande, fill=(60, 30, 20, 120))
    d.text((x, y), s, font=grande, fill=(128, 14, 22, 255))
    # los chiquitos en las esquinas (el de abajo, de cabeza)
    d.text((8, 7), s, font=chica, fill=(90, 14, 18, 255))
    esq = Image.new('RGBA', (20, 16), (0, 0, 0, 0))
    ImageDraw.Draw(esq).text((1, 1), s, font=chica, fill=(90, 14, 18, 255))
    esq = esq.rotate(180)
    img.alpha_composite(esq, (W - 8 - 20 + 1 + (2 if n < 10 else -1), H - 7 - 16 + 1))
    # un rombo chiquito arriba y abajo del número
    for cy in (19, H - 20):
        d.polygon([(W / 2, cy - 3), (W / 2 + 3, cy), (W / 2, cy + 3), (W / 2 - 3, cy)], fill=(160, 120, 60, 255))
    return img


def reverso():
    yy, xx = np.mgrid[0:H, 0:W]
    rgb = np.zeros((H, W, 3))
    rgb[:] = np.array([70, 10, 20], float) * (0.85 + 0.2 * R.rand(H, W, 1))
    # celosía de rombos
    celo = ((xx + yy) % 8 == 0) | ((xx - yy) % 8 == 0)
    rgb[celo] = np.array([110, 30, 32])
    img = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), 'RGB').convert('RGBA')
    a = Image.new('L', (W, H), 0)
    ImageDraw.Draw(a).rounded_rectangle((0, 0, W - 1, H - 1), radius=5, fill=255)
    img.putalpha(a)
    d = ImageDraw.Draw(img)
    oro = (206, 160, 64, 255)
    d.rounded_rectangle((2, 2, W - 3, H - 3), radius=4, outline=oro, width=2)
    d.rounded_rectangle((6, 6, W - 7, H - 7), radius=3, outline=(150, 110, 46, 255), width=1)
    # el ojo del Coleccionista al centro, en un medallón
    cx, cy = W / 2, H / 2
    d.ellipse((cx - 15, cy - 15, cx + 15, cy + 15), fill=(40, 6, 12, 255), outline=oro, width=2)
    d.ellipse((cx - 11, cy - 6, cx + 11, cy + 6), fill=(232, 220, 196, 255))
    d.ellipse((cx - 5, cy - 5, cx + 5, cy + 5), fill=(128, 14, 22, 255))
    d.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=(10, 4, 6, 255))
    for k in range(-2, 3):
        d.line((cx + k * 4, cy - 7, cx + k * 5, cy - 11), fill=oro, width=1)
    return img


def main():
    atlas = Image.new('RGBA', (512, 256), (0, 0, 0, 0))
    for k in range(12):
        c = reverso() if k == 0 else cara(k)
        atlas.alpha_composite(c, ((k % 8) * W, (k // 8) * H))
    os.makedirs(os.path.dirname(SALIDA), exist_ok=True)
    atlas.save(SALIDA)
    print('cartas21.png', atlas.size)


if __name__ == '__main__':
    main()
