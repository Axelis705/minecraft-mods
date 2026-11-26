#!/usr/bin/env python3
"""Arma las imágenes y la música del launcher del Coleccionista (se corre una vez; el resultado va en launcher/arte):

- fondo.png       960x540: el escenario de la pantalla de inicio del mod (telones, reflector), el Coleccionista en su sillón a la
                  derecha, un panel oscuro a la izquierda y el título
- icono.ico/.png  la cara del Coleccionista (para el .exe y la ventana)

Necesita Pillow, ffmpeg, y que antes se hayan hecho las imágenes del mod (coleccionista/herramientas/menu_texturas.py y menu_sonidos.py).
El render del Coleccionista (coleccionista_render.png, fondo verde) se hizo con la vista previa de modelos (three.js).
"""
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

AQUI = os.path.dirname(os.path.abspath(__file__))
MOD = os.path.join(AQUI, '..', '..', 'coleccionista', 'src', 'main', 'resources', 'assets', 'coleccionista')
W, H = 960, 540
FUENTE = '/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf'
FUENTE2 = '/usr/share/fonts/truetype/dejavu/DejaVuSerifCondensed-BoldItalic.ttf'


def coleccionista():
    """El render con el verde quitado (y el brazo derecho del sillón copiado del izquierdo: la vista previa lo pinta mal)."""
    im = np.array(Image.open(os.path.join(AQUI, 'coleccionista_render.png')).convert('RGBA')).astype(int)
    h, w, _ = im.shape
    # el brazo del sillón de la derecha = espejo del de la izquierda (el modelo está centrado en la imagen)
    centro = w // 2
    for x in range(214, 272):
        xd = 2 * centro - x
        im[:, xd] = im[:, x]
    r, g, b = im[..., 0], im[..., 1], im[..., 2]
    verde = (g > 150) & (r < 120) & (b < 120)
    im[verde, 3] = 0
    # quita el borde verdoso que deja el suavizado
    borde = (g > r + 25) & (g > b + 25) & ~verde
    im[borde, 1] = np.maximum(im[borde, 0], im[borde, 2])
    out = Image.fromarray(im.clip(0, 255).astype(np.uint8), 'RGBA')
    return out.crop(out.getbbox())


def fondo():
    base = Image.open(os.path.join(MOD, 'textures', 'gui', 'inicio_fondo.png')).convert('RGBA').resize((W, H), Image.NEAREST)
    luz = Image.open(os.path.join(MOD, 'textures', 'gui', 'inicio_luz.png')).convert('RGBA').resize((W, H), Image.BILINEAR)
    img = Image.alpha_composite(base, luz)
    # él, en la luz (el sillón sobre el círculo del piso)
    c = coleccionista()
    alto = int(H * 0.6)
    c = c.resize((int(c.width * alto / c.height), alto), Image.NEAREST)
    cx = int(W * 0.685)
    sombra = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(sombra)
    d.ellipse([cx - c.width * 0.45, H * 0.865, cx + c.width * 0.45, H * 0.94], fill=(0, 0, 0, 120))
    img = Image.alpha_composite(img, sombra.filter(ImageFilter.GaussianBlur(8)))
    img.alpha_composite(c, (cx - c.width // 2, int(H * 0.905) - alto))
    # el panel de la izquierda: oscuro, que se desvanece hacia la derecha
    panel = np.zeros((H, W, 4), np.uint8)
    x = np.arange(W)
    a = np.clip((470 - x) / 140.0, 0, 1) * 215
    panel[..., 3] = a[None, :].astype(np.uint8)
    panel[..., 0], panel[..., 1], panel[..., 2] = 10, 4, 6
    img = Image.alpha_composite(img, Image.fromarray(panel, 'RGBA'))
    # el título
    d = ImageDraw.Draw(img)
    f1 = ImageFont.truetype(FUENTE, 22)
    f2 = ImageFont.truetype(FUENTE, 42)
    f3 = ImageFont.truetype(FUENTE2, 17)
    x0, y0 = 56, 52
    d.text((x0 + 2, y0), 'EL', font=f1, fill=(168, 152, 128, 255))
    for dx, dy, col in ((3, 3, (58, 0, 8, 255)), (0, 0, (233, 217, 184, 255))):
        d.text((x0 + dx, y0 + 24 + dy), 'COLECCIONISTA', font=f2, fill=col)
    ancho = d.textlength('COLECCIONISTA', font=f2)
    d.rectangle([x0, y0 + 80, x0 + ancho, y0 + 81], fill=(122, 90, 30, 255))
    d.text((x0, y0 + 88), 'Un evento de Axelis', font=f3, fill=(168, 152, 128, 255))
    # rayas de tele vieja, muy suaves
    rayas = np.zeros((H, W, 4), np.uint8)
    rayas[::3, :, 3] = 22
    img = Image.alpha_composite(img, Image.fromarray(rayas, 'RGBA'))
    img.convert('RGB').save(os.path.join(AQUI, 'fondo.png'))
    return c


def icono(c):
    # la cara: el cuadro de la cabeza (sin el sillón) con un poco de sombrero
    w, h = c.size
    cara = c.crop((int(w * 0.3), 0, int(w * 0.7), int(h * 0.55)))
    lado = max(cara.size)
    cuadro = Image.new('RGBA', (lado, lado), (20, 6, 9, 255))
    cuadro.alpha_composite(cara, ((lado - cara.width) // 2, lado - cara.height))
    cuadro = cuadro.resize((256, 256), Image.NEAREST)
    cuadro.save(os.path.join(AQUI, 'icono.png'))
    cuadro.save(os.path.join(AQUI, 'icono.ico'), sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    # el ícono del perfil en el launcher de Minecraft (128x128)
    cuadro.resize((128, 128), Image.NEAREST).save(os.path.join(AQUI, 'perfil.png'))


def main():
    c = fondo()
    icono(c)
    print('listo: fondo.png, icono.ico, icono.png, perfil.png')


if __name__ == '__main__':
    main()
