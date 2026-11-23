#!/usr/bin/env python3
"""Prendas de muestra del Vestuario del Core (solo para las pruebas y como ejemplo para los mods):
- overol: tipo piel (64x64 con el acomodo de una skin), overol de mezclilla con tirantes y bolsa en el pecho (ahí el juego escribe el número).
- sombrero: tipo modelo (geo.json de Blockbench) con un grupo "cabeza": sombrero de ala ancha.
Uso: python3 herramientas/prendas.py
"""
import json
import os

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'axelis_core')


def caja(u, v, w, h, d):
    return {'arriba': (u + d, v, w, d), 'abajo': (u + d + w, v, w, d), 'der': (u, v + d, d, h), 'frente': (u + d, v + d, w, h),
            'izq': (u + d + w, v + d, d, h), 'atras': (u + d + w + d, v + d, w, h)}


def overol():
    img = np.zeros((64, 64, 4), np.uint8)
    r = np.random.RandomState(3)
    azul = np.array([58, 86, 132])

    def pinta(x, y, w, h, color, var=14):
        n = r.randint(-var, var + 1, (h, w, 1))
        img[y:y + h, x:x + w, :3] = np.clip(color + n, 0, 255)
        img[y:y + h, x:x + w, 3] = 255

    # piernas completas
    for (u, v) in ((0, 16), (16, 48)):
        for k, (x, y, w, h) in caja(u, v, 4, 12, 4).items():
            if k == 'arriba':
                continue
            pinta(x, y, w, h, azul)
            if k != 'abajo':
                img[y + h - 1, x:x + w, :3] = [40, 60, 96]  # dobladillo
    # cuerpo: peto con tirantes (arriba camiseta transparente para ver la skin)
    c = caja(16, 16, 8, 12, 4)
    x, y, w, h = c['frente']
    pinta(x + 1, y + 4, 6, 8, azul)  # peto
    pinta(x, y + 7, w, 5, azul)
    img[y:y + 4, x + 1, :3] = [50, 74, 116]
    img[y:y + 4, x + 1, 3] = 255
    img[y:y + 4, x + 6, :3] = [50, 74, 116]
    img[y:y + 4, x + 6, 3] = 255
    for bx in (x + 1, x + 6):
        img[y + 4, bx, :3] = [210, 190, 90]  # botones dorados
    pinta(x + 2, y + 5, 4, 3, np.array([70, 100, 148]), 6)  # bolsa del pecho (número)
    x, y, w, h = c['atras']
    pinta(x, y + 7, w, 5, azul)
    for t in range(7):
        img[y + t, x + 1 + t // 2, :3] = [50, 74, 116]
        img[y + t, x + 1 + t // 2, 3] = 255
        img[y + t, x + 6 - t // 2, :3] = [50, 74, 116]
        img[y + t, x + 6 - t // 2, 3] = 255
    for k in ('der', 'izq'):
        x, y, w, h = c[k]
        pinta(x, y + 7, w, 5, azul)
    os.makedirs(os.path.join(RES, 'textures', 'prenda'), exist_ok=True)
    Image.fromarray(img, 'RGBA').save(os.path.join(RES, 'textures', 'prenda', 'overol.png'))


def sombrero():
    geo = {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {'identifier': 'geometry.axelis_core.sombrero', 'texture_width': 64, 'texture_height': 32},
            'bones': [{
                'name': 'cabeza', 'pivot': [0, 24, 0],
                'cubes': [
                    {'origin': [-7, 31.5, -7], 'size': [14, 1, 14], 'uv': [0, 0]},     # ala
                    {'origin': [-4.5, 32.5, -4.5], 'size': [9, 4, 9], 'uv': [0, 15]},  # copa
                    {'origin': [-4.6, 32.5, -4.6], 'size': [9.2, 1, 9.2], 'uv': [36, 15]},  # cinta
                ]}]}]}
    os.makedirs(os.path.join(RES, 'geo'), exist_ok=True)
    with open(os.path.join(RES, 'geo', 'sombrero.geo.json'), 'w') as f:
        json.dump(geo, f, indent=1)
    img = np.zeros((32, 64, 4), np.uint8)
    r = np.random.RandomState(5)
    paja = np.array([196, 160, 92])
    img[..., :3] = np.clip(paja + r.randint(-18, 18, (32, 64, 1)), 0, 255)
    img[..., 3] = 255
    yy, xx = np.mgrid[0:32, 0:64]
    img[((xx + yy) % 3 == 0)] = np.concatenate([np.clip(paja * 0.85, 0, 255), [255]]).astype(np.uint8)
    img[15:25, 36:64, :3] = [120, 30, 30]  # cinta roja
    Image.fromarray(img, 'RGBA').save(os.path.join(RES, 'textures', 'prenda', 'sombrero.png'))


if __name__ == '__main__':
    overol()
    sombrero()
    print('overol.png, sombrero.geo.json y sombrero.png listos')
