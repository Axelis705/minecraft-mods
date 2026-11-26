#!/usr/bin/env python3
"""Las imágenes de la pantalla de inicio del Coleccionista (en píxeles grandes, como el juego):

- inicio_fondo.png  el escenario: pared oscura con papel tapiz, telones rojos a los lados y arriba (con fleco dorado), piso de tablas
                    que se van al fondo y las orillas oscuras
- inicio_luz.png    la luz del reflector (aparte, para que parpadee): el haz que baja, el círculo en la pared y en el piso

Uso: python3 herramientas/menu_texturas.py
"""
import os

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
GUI = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'textures', 'gui')
W, H = 384, 216
R = np.random.RandomState(26)
# dónde va el Coleccionista (el reflector lo alumbra a él)
CX = 0.68
PISO = 0.80


def fondo():
    y, x = np.mgrid[0:H, 0:W].astype(float)
    u, v = x / W, y / H
    img = np.zeros((H, W, 3))
    # la pared: papel tapiz de rayas (vino y casi negro) con un dibujo de rombos muy tenue
    raya = (np.floor(x / 6) % 2)
    base = np.array([26, 9, 12]) * (1 - raya[..., None]) + np.array([19, 6, 9]) * raya[..., None]
    rombo = ((np.abs((x % 24) - 12) + np.abs((y % 24) - 12)) < 4).astype(float)
    img[:] = base + rombo[..., None] * np.array([9, 4, 3])
    # una moldura dorada oscura a media pared y el lambrín de madera abajo
    mold = (np.abs(v - 0.62) < 0.006)
    img[mold] = [74, 52, 22]
    lamb = (v > 0.625) & (v < PISO)
    tabla = (np.floor(x / 16) % 2)
    img[lamb] = (np.array([34, 20, 12]) + tabla[..., None] * np.array([5, 3, 2]))[lamb]
    # el piso: tablas que se van hacia el fondo (al centro del reflector)
    m = v >= PISO
    prof = (v - PISO) / (1 - PISO)
    xs = (u - CX) / (0.25 + prof)
    lineas = (np.abs(((xs * 9) % 1) - 0.5) > 0.46)
    madera = np.array([46, 27, 15]) + R.randint(-3, 4, (H, W, 1))
    piso = madera * (0.6 + 0.6 * prof[..., None])
    piso[lineas] *= 0.55
    img[m] = piso[m]
    img[(np.abs(v - PISO) < 0.004)] = [20, 11, 7]
    # los telones de los lados: terciopelo rojo con pliegues
    for lado in (0, 1):
        # recogidos: más angostos arriba y más anchos abajo, con la orilla apenas ondulada
        ancho = 0.11 + 0.06 * v ** 1.5
        dist = u if lado == 0 else 1 - u
        orilla_x = ancho + 0.006 * np.sin(v * 23 + lado)
        tel = dist < orilla_x
        pl = np.sin(dist / ancho * np.pi * 5.5 + v * 0.8) * 0.5 + 0.5
        col = np.array([92, 10, 18]) * (0.35 + 0.75 * pl[..., None] ** 1.6)
        col *= (0.55 + 0.45 * (1 - dist / ancho))[..., None].clip(0.4, 1)
        img[tel] = col[tel]
        # el fleco dorado en la orilla de adentro
        orilla = tel & (np.abs(dist - orilla_x) < 0.005)
        img[orilla] = [150, 112, 40]
    # la cenefa de arriba: ondas de terciopelo con borde dorado y borlas
    onda = 0.11 + 0.025 * np.abs(np.sin(u * np.pi * 7))
    cen = v < onda
    pl = np.sin(u * np.pi * 28) * 0.5 + 0.5
    col = np.array([105, 12, 22]) * (0.45 + 0.6 * pl[..., None])
    img[cen] = col[cen]
    borde = (v >= onda - 0.012) & (v < onda)
    img[borde] = [168, 126, 48]
    for k in range(8):
        bx = (k + 0.5) / 8 * W
        by = (0.11 + 0.025) * H + 1
        for dy in range(7):
            ancho = 1 if dy < 4 else 2
            for dx in range(-ancho, ancho + 1):
                xx, yy = int(bx + dx), int(by + dy)
                if 0 <= xx < W and 0 <= yy < H:
                    img[yy, xx] = [176, 132, 50] if dy < 5 else [120, 86, 30]
    # las orillas oscuras (y un poco más abajo)
    d = np.sqrt(((u - 0.5) / 0.62) ** 2 + ((v - 0.45) / 0.7) ** 2)
    img *= np.clip(1.15 - d ** 2 * 0.75, 0.18, 1)[..., None]
    # un grano fino
    img += R.normal(0, 2.0, img.shape)
    return np.clip(img, 0, 255).astype(np.uint8)


def luz():
    y, x = np.mgrid[0:H, 0:W].astype(float)
    u, v = x / W, y / H
    a = np.zeros((H, W))
    # el círculo en la pared (detrás de él)
    d = np.sqrt(((u - CX) / 0.16) ** 2 + ((v - 0.47) / 0.29) ** 2)
    a += np.clip(1 - d, 0, 1) ** 1.2 * 0.45
    # el haz que baja desde arriba (se abre)
    ancho = 0.03 + 0.17 * v
    haz = np.clip(1 - np.abs(u - CX) / ancho, 0, 1) ** 1.3 * (0.25 + 0.2 * v)
    a += haz
    # el círculo en el piso
    d2 = np.sqrt(((u - CX) / 0.2) ** 2 + ((v - 0.9) / 0.07) ** 2)
    a += np.clip(1 - d2, 0, 1) * 0.6
    # se escalona un poco (píxeles grandes) para que no se vea liso de más
    a = np.round(a.clip(0, 1) * 18) / 18
    img = np.zeros((H, W, 4))
    img[..., 0], img[..., 1], img[..., 2] = 255, 228, 176
    img[..., 3] = a * 190
    return img.clip(0, 255).astype(np.uint8)


def main():
    os.makedirs(GUI, exist_ok=True)
    Image.fromarray(fondo(), 'RGB').save(os.path.join(GUI, 'inicio_fondo.png'))
    Image.fromarray(luz(), 'RGBA').save(os.path.join(GUI, 'inicio_luz.png'))
    print('listo: inicio_fondo.png, inicio_luz.png')


if __name__ == '__main__':
    main()
