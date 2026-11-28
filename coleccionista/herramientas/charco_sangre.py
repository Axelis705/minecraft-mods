#!/usr/bin/env python3
"""El charco de sangre debajo del cuerpo sin cabeza (hotel): una mancha oscura y brillosa de orilla irregular, con gotas alrededor.
Escribe textures/entity/charco_sangre.png (64 x 64) y charco_brillo.png (los reflejos de líquido, que se dibujan encima con más luz
para que el charco se vea mojado aunque el cuarto esté oscuro). Uso: python3 herramientas/charco_sangre.py"""
import os

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
CARPETA = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'textures', 'entity')
SALIDA = os.path.join(CARPETA, 'charco_sangre.png')
BRILLO = os.path.join(CARPETA, 'charco_brillo.png')
N = 64
R = np.random.RandomState(316)


def ruido(n, escala):
    g = R.rand(n // escala + 2, n // escala + 2)
    y, x = np.mgrid[0:n, 0:n] / escala
    x0, y0 = x.astype(int), y.astype(int)
    fx, fy = x - x0, y - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a = g[y0, x0] * (1 - fx) + g[y0, x0 + 1] * fx
    b = g[y0 + 1, x0] * (1 - fx) + g[y0 + 1, x0 + 1] * fx
    return a * (1 - fy) + b * fy


def main():
    y, x = (np.mgrid[0:N, 0:N] + 0.5) / N * 2 - 1
    r = np.sqrt(x * x + y * y)
    ang = np.arctan2(y, x)
    # la orilla: un círculo deformado (lóbulos) con ruido
    orilla = 0.72 + 0.1 * np.sin(ang * 3 + 0.7) + 0.06 * np.sin(ang * 7 + 2.1) + 0.12 * (ruido(N, 8) - 0.5)
    dentro = r < orilla
    # gotas sueltas alrededor
    gotas = np.zeros((N, N), bool)
    for _ in range(9):
        a, d, s = R.rand() * 6.28, 0.8 + R.rand() * 0.15, 0.03 + R.rand() * 0.04
        gotas |= (x - np.cos(a) * d) ** 2 + (y - np.sin(a) * d) ** 2 < s * s
    m = dentro | gotas
    # color: casi negro en medio, rojo oscuro en la orilla (más delgada), con brillos de líquido
    t = np.clip(r / np.maximum(orilla, 0.01), 0, 1)
    base = np.stack([35 + 55 * t, 3 + 6 * t, 5 + 6 * t], -1)
    n = ruido(N, 4)
    base *= (0.85 + 0.3 * n)[..., None]
    brillo = (ruido(N, 6) > 0.84) & (t < 0.85)
    base[brillo] = [125, 30, 32]
    alfa = np.where(m, np.where(t > 0.92, 200, 240), 0)
    img = np.dstack([np.clip(base, 0, 255), alfa]).astype(np.uint8)
    os.makedirs(os.path.dirname(SALIDA), exist_ok=True)
    Image.fromarray(img, 'RGBA').save(SALIDA)
    print('charco_sangre.png', int(m.sum()), 'pixeles')
    # los reflejos: una franja larga y suave (la lámpara reflejada) y manchitas de brillo, solo dentro del charco y lejos de la orilla
    franja = np.exp(-((x * 0.8 + y * 0.6 + 0.12) / 0.13) ** 2) * np.exp(-((x * 0.6 - y * 0.8) / 0.3) ** 2)
    puntos = np.clip((ruido(N, 3) - 0.82) / 0.12, 0, 1)
    luz = np.clip(franja * 1.1 + puntos * 0.45, 0, 1) * np.clip((0.9 - t) / 0.25, 0, 1) * dentro
    # una orilla fina más clara (la sangre fresca del borde) que separa el charco de la alfombra
    borde = dentro & (t > 0.8)
    rgb = np.zeros((N, N, 3))
    rgb[...] = [255, 205, 205]
    rgb[borde] = [205, 40, 44]
    a2 = np.where(borde, 150, luz * 190)
    Image.fromarray(np.dstack([rgb, np.clip(a2, 0, 255)]).astype(np.uint8), 'RGBA').save(BRILLO)
    print('charco_brillo.png', int((a2 > 20).sum()), 'pixeles')


if __name__ == '__main__':
    main()
