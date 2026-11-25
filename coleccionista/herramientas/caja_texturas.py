#!/usr/bin/env python3
"""Las texturas de la caja de música (capítulo 3):

- textures/gui/caja_<simbolo>.png      los cuatro símbolos grandes (pantallas y avisos), fondo transparente
- textures/prenda/caja_etiqueta_<s>.png la tarjeta que cada pieza lleva en la espalda (papel viejo, el símbolo y un seguro)
- textures/prenda/caja_mascara.png     la máscara de muñeco de caja de música (solo la cabeza, acomodo de skin 64x64)
- textures/prenda/vacia.png            una piel transparente (la etiqueta se dibuja aparte)

Uso: python3 herramientas/caja_texturas.py
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

AQUI = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'textures')
SIMBOLOS = ['pica', 'corazon', 'diamante', 'trebol']
ROJO, NEGRO = (178, 22, 30), (24, 20, 22)


def forma(d, s, x0, y0, tam, color):
    """Dibuja un símbolo en (x0, y0) de tamaño tam (cuadro), con el color dado."""
    cx, cy, r = x0 + tam / 2, y0 + tam / 2, tam / 2
    if s == 'corazon':
        a = r * 0.52
        d.ellipse([cx - 2 * a + a * 0.05, cy - r * 0.62, cx + a * 0.05, cy - r * 0.62 + 2 * a], fill=color)
        d.ellipse([cx - a * 0.05, cy - r * 0.62, cx + 2 * a - a * 0.05, cy - r * 0.62 + 2 * a], fill=color)
        d.polygon([(cx - r * 0.98, cy - r * 0.05), (cx + r * 0.98, cy - r * 0.05), (cx, cy + r * 0.92)], fill=color)
    elif s == 'diamante':
        d.polygon([(cx, cy - r * 0.95), (cx + r * 0.68, cy), (cx, cy + r * 0.95), (cx - r * 0.68, cy)], fill=color)
    elif s == 'pica':
        a = r * 0.5
        d.ellipse([cx - 2 * a + a * 0.1, cy - r * 0.2, cx + a * 0.1, cy - r * 0.2 + 2 * a * 0.95], fill=color)
        d.ellipse([cx - a * 0.1, cy - r * 0.2, cx + 2 * a - a * 0.1, cy - r * 0.2 + 2 * a * 0.95], fill=color)
        d.polygon([(cx - r * 0.96, cy + r * 0.2), (cx + r * 0.96, cy + r * 0.2), (cx, cy - r * 0.95)], fill=color)
        d.polygon([(cx - r * 0.12, cy + r * 0.35), (cx + r * 0.12, cy + r * 0.35), (cx + r * 0.38, cy + r * 0.95),
                   (cx - r * 0.38, cy + r * 0.95)], fill=color)
    elif s == 'trebol':
        b = r * 0.36
        for (dx, dy) in ((0, -0.48), (-0.5, 0.08), (0.5, 0.08)):
            d.ellipse([cx + dx * r - b, cy + dy * r - b, cx + dx * r + b, cy + dy * r + b], fill=color)
        d.ellipse([cx - b * 0.6, cy - b * 0.6, cx + b * 0.6, cy + b * 0.6], fill=color)
        d.polygon([(cx - r * 0.1, cy + r * 0.1), (cx + r * 0.1, cy + r * 0.1), (cx + r * 0.36, cy + r * 0.95),
                   (cx - r * 0.36, cy + r * 0.95)], fill=color)


def color(s):
    return ROJO if s in ('corazon', 'diamante') else NEGRO


def icono(s, tam=64):
    k = 6
    im = Image.new('RGBA', (tam * k, tam * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # un contorno claro (para que se lea sobre fondo oscuro)
    forma(d, s, 3 * k, 3 * k, (tam - 6) * k, (236, 226, 208, 255))
    im = im.filter(ImageFilter.MaxFilter(2 * k + 1))
    d = ImageDraw.Draw(im)
    forma(d, s, 3 * k, 3 * k, (tam - 6) * k, color(s) + (255,))
    return im.resize((tam, tam), Image.LANCZOS)


def tarjeta(s, tam=32):
    """Papel viejo con el símbolo, un borde rojo desvaído y un seguro arriba."""
    k = 8
    r = np.random.RandomState(SIMBOLOS.index(s) + 5)
    base = np.zeros((tam, tam, 4), np.float32)
    papel = np.array([226, 214, 186], np.float32)
    ruido = r.rand(tam, tam) * 0.12 - 0.06
    base[..., :3] = papel * (1 + ruido[..., None])
    # manchitas de humedad
    yy, xx = np.mgrid[0:tam, 0:tam]
    for _ in range(3):
        cx, cy, rad = r.rand() * tam, r.rand() * tam, 3 + r.rand() * 5
        m = np.exp(-((xx - cx) ** 2 + (yy - cy) ** 2) / (2 * rad ** 2))
        base[..., :3] *= (1 - 0.18 * m[..., None])
    base[..., 3] = 255
    # esquinas un poco comidas
    for (x, y) in ((0, 0), (tam - 1, 0), (0, tam - 1), (tam - 1, tam - 1)):
        base[y, x, 3] = 0
    im = Image.fromarray(np.clip(base, 0, 255).astype(np.uint8), 'RGBA')
    d = ImageDraw.Draw(im)
    d.rectangle([2, 2, tam - 3, tam - 3], outline=(150, 40, 40, 255))
    sim = Image.new('RGBA', (tam * k, tam * k), (0, 0, 0, 0))
    forma(ImageDraw.Draw(sim), s, 6 * k, 7 * k, (tam - 12) * k, color(s) + (255,))
    sim = sim.resize((tam, tam), Image.LANCZOS)
    im.alpha_composite(sim)
    # el seguro (arriba al centro)
    d.line([(tam // 2 - 4, 1), (tam // 2 + 4, 1)], fill=(150, 150, 160, 255))
    d.line([(tam // 2 - 4, 2), (tam // 2 + 4, 2)], fill=(90, 90, 100, 255))
    d.point([(tam // 2 + 5, 1), (tam // 2 + 5, 2)], fill=(200, 200, 210, 255))
    return im


def mascara():
    """La cara de porcelana de un muñeco de caja de música (cachetes rosas, ojos negros pintados, boquita roja y una grieta) y una
    capucha de terciopelo negro con un ribete dorado alrededor de la cara. Acomodo de la cabeza de una skin (64x64)."""
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    r = np.random.RandomState(9)

    def terciopelo(x0, y0, w, h):
        a = np.zeros((h, w, 4), np.uint8)
        v = 26 + r.rand(h, w) * 14
        a[..., 0] = v * 1.05
        a[..., 1] = v * 0.7
        a[..., 2] = v * 0.85
        a[..., 3] = 255
        im.paste(Image.fromarray(a, 'RGBA'), (x0, y0))

    # arriba (8,0), abajo (16,0), derecha (0,8), izquierda (16,8), atrás (24,8): capucha
    terciopelo(8, 0, 8, 8)
    terciopelo(16, 0, 8, 8)
    terciopelo(0, 8, 8, 8)
    terciopelo(16, 8, 8, 8)
    terciopelo(24, 8, 8, 8)
    d = ImageDraw.Draw(im)
    # ribete dorado en las orillas de los lados (junto a la cara)
    for x in (7, 16):
        d.line([(x, 8), (x, 15)], fill=(170, 130, 50, 255))
    # la cara: porcelana
    cara = np.zeros((8, 8, 4), np.float32)
    cara[..., :3] = np.array([236, 230, 222]) * (1 + (r.rand(8, 8)[..., None] * 0.05 - 0.025))
    cara[..., 3] = 255
    # cachetes rosas
    for (x, y) in ((1, 5), (6, 5)):
        cara[y, x, :3] = [226, 160, 160]
    cara[0, :, :3] = [170, 130, 50]  # el ribete de arriba
    im.paste(Image.fromarray(np.clip(cara, 0, 255).astype(np.uint8), 'RGBA'), (8, 8))
    # ojos negros pintados (con un brillito), cejas finas, la boquita y la grieta
    d.point([(10, 12), (13, 12)], fill=(16, 12, 14, 255))
    d.point([(10, 11), (13, 11)], fill=(36, 26, 30, 255))
    d.point([(9, 10), (10, 10), (13, 10), (14, 10)], fill=(90, 70, 60, 255))
    d.point([(11, 14), (12, 14)], fill=(170, 30, 40, 255))
    d.point([(14, 9), (14, 10), (15, 11)], fill=(120, 112, 104, 255))
    return im


def main():
    os.makedirs(os.path.join(A, 'gui'), exist_ok=True)
    os.makedirs(os.path.join(A, 'prenda'), exist_ok=True)
    for s in SIMBOLOS:
        icono(s).save(os.path.join(A, 'gui', f'caja_{s}.png'))
        tarjeta(s).save(os.path.join(A, 'prenda', f'caja_etiqueta_{s}.png'))
    mascara().save(os.path.join(A, 'prenda', 'caja_mascara.png'))
    vacia = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    vacia.putpixel((0, 0), (40, 30, 30, 255))   # (en una parte de la skin que no usa ninguna cara: no se ve)
    vacia.save(os.path.join(A, 'prenda', 'vacia.png'))
    print('listo')


if __name__ == '__main__':
    main()
