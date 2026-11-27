#!/usr/bin/env python3
"""Ropa navideña para el Vestuario del Core:
- tipo piel (64x64, acomodo de skin): suéter de reno, suéter de pinos, suéter de copos y el traje de Santa (con barba).
- tipo modelo (geo.json con grupo "cabeza"): gorro de Santa, gorro de duende y diadema de cuernos de reno (con nariz roja).
Uso: python3 herramientas/ropa.py"""
import os, sys
import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
import geo as G  # noqa: E402

rs = np.random.RandomState(12)


def caja(u, v, w, h, d):
    return {'arriba': (u + d, v, w, d), 'abajo': (u + d + w, v, w, d), 'der': (u, v + d, d, h), 'frente': (u + d, v + d, w, h),
            'izq': (u + d + w, v + d, d, h), 'atras': (u + d + w + d, v + d, w, h)}


PARTES = {'cabeza': caja(0, 0, 8, 8, 8), 'cuerpo': caja(16, 16, 8, 12, 4), 'brazo_der': caja(40, 16, 4, 12, 4),
          'brazo_izq': caja(32, 48, 4, 12, 4), 'pierna_der': caja(0, 16, 4, 12, 4), 'pierna_izq': caja(16, 48, 4, 12, 4)}


def hex2(c):
    c = c.lstrip('#')
    return np.array([int(c[i:i + 2], 16) for i in (0, 2, 4)])


def piel(nombre, pintar, var=10):
    """pintar(parte, cara, x, y, w, h) -> '#rrggbb' o None (transparente)."""
    img = np.zeros((64, 64, 4), np.uint8)
    for parte, caras in PARTES.items():
        for cara, (x0, y0, w, h) in caras.items():
            for y in range(h):
                for x in range(w):
                    c = pintar(parte, cara, x, y, w, h)
                    if c is None:
                        continue
                    col = np.clip(hex2(c) + rs.randint(-var, var + 1), 0, 255)
                    img[y0 + y, x0 + x, :3] = col
                    img[y0 + y, x0 + x, 3] = 255
    p = os.path.join(RES, 'textures', 'prenda', nombre + '.png')
    os.makedirs(os.path.dirname(p), exist_ok=True)
    Image.fromarray(img, 'RGBA').save(p)


def mapa(filas, colores):
    """Un dibujito en texto: cada letra es un color (punto = nada)."""
    return [[colores.get(ch) for ch in fila] for fila in filas]


def sueter(nombre, base, banda1, banda2, frente):
    oscuro = '#%02x%02x%02x' % tuple(int(v * 0.72) for v in hex2(base))

    def f(parte, cara, x, y, w, h):
        if parte == 'cabeza' or parte.startswith('pierna'):
            return None
        if parte == 'cuerpo':
            if cara == 'arriba':
                return base if (x in (0, w - 1) or y in (0, h - 1)) and not (2 <= x <= 5 and y < 2) else None
            if cara == 'abajo':
                return oscuro
            if y == 0 and cara in ('frente', 'atras') and 2 <= x <= 5:
                return oscuro  # cuello
            if y == h - 1:
                return oscuro  # resorte de abajo
            if cara == 'frente' and frente is not None and 0 <= y < len(frente) and frente[y] is not None and frente[y][x] is not None:
                return frente[y][x]
            if y in (1, 10):
                return banda1 if (x + y) % 2 == 0 else banda2
            return base
        # brazos
        if cara == 'arriba':
            return base
        if cara == 'abajo':
            return None
        if y >= h - 2:
            return oscuro  # puño
        if y in (3, 7):
            return banda1 if x % 2 == 0 else banda2
        return base
    piel(nombre, f)


def traje_santa():
    rojo, blanco, negro, oro, piel_c = '#c41e2a', '#f4f4f0', '#1a1a1a', '#e8c040', None

    def f(parte, cara, x, y, w, h):
        if parte == 'cabeza':
            # barba y bigote (lo demás transparente para ver la cara)
            if cara == 'frente':
                if y == 4 and x in (1, 2, 5, 6):
                    return blanco
                if y >= 5:
                    return '#d8d8d0' if (y == 5 and 3 <= x <= 4) else blanco
                return None
            if cara in ('der', 'izq'):
                lado = x >= 4 if cara == 'der' else x <= 3
                if y >= 4 and lado:
                    return blanco
                return None
            if cara == 'abajo':
                return blanco
            return None
        if parte == 'cuerpo':
            if cara == 'arriba':
                return rojo
            if cara == 'abajo':
                return blanco
            if y >= h - 2:
                return blanco  # orilla de peluche
            if y in (7, 8):
                if cara == 'frente' and 3 <= x <= 4:
                    return oro if (y == 7 or x == 3) else '#a88020'
                return negro  # cinturón
            if cara == 'frente' and 3 <= x <= 4:
                return blanco  # peluche al centro
            return rojo
        if parte.startswith('brazo'):
            if cara == 'arriba':
                return rojo
            if cara == 'abajo':
                return None
            if y >= h - 2:
                return blanco
            return rojo
        # piernas: pantalón rojo y botas negras
        if cara == 'arriba':
            return None
        if cara == 'abajo':
            return negro
        if y >= 8:
            return negro if y != 8 else '#2a2a2a'
        return rojo
    piel('traje_santa', f)


def gorros():
    vacio = os.path.join(AQUI, 'vacio.animation.json')
    # gorro de Santa
    c = G.Creature('geometry.navidad.gorro_santa', 64, 64, seed=5)
    c.bone('cabeza', [0, 24, 0])
    c.part('cabeza', [-4.6, 30.6, -4.6], [9.2, 2.2, 9.2], G.solid((240, 240, 236)))
    c.part('cabeza', [-4.1, 32.6, -4.1], [8.2, 2, 8.2], G.solid((196, 30, 42)))
    c.bone('punta', [0, 34.4, 0], 'cabeza', [-22, 0, 8])
    c.part('punta', [-3.2, 34.4, -3.0], [6.4, 2, 6.4], G.solid((196, 30, 42)))
    c.part('punta', [-2.3, 36.2, -1.6], [4.6, 2, 4.6], G.solid((186, 26, 38)))
    c.bone('punta2', [0, 38, 0.6], 'punta', [-38, 0, 0])
    c.part('punta2', [-1.4, 38, -0.6], [2.8, 2.2, 2.8], G.solid((176, 24, 34)))
    c.part('punta2', [-1.3, 39.8, -0.3], [2.6, 2.6, 2.6], G.solid((246, 246, 244)))
    c.build(os.path.join(RES, 'geo', 'gorro_santa.geo.json'), os.path.join(RES, 'textures', 'prenda', 'gorro_santa.png'))
    # gorro de duende: verde, largo y con cascabel
    c = G.Creature('geometry.navidad.gorro_duende', 64, 64, seed=6)
    c.bone('cabeza', [0, 24, 0])
    c.part('cabeza', [-4.5, 30.8, -4.5], [9, 1.8, 9], G.solid((200, 40, 40)))
    c.part('cabeza', [-4.1, 32.6, -4.1], [8.2, 2.2, 8.2], G.solid((44, 150, 64)))
    c.bone('punta', [0, 34.8, 0], 'cabeza', [-12, 0, -6])
    c.part('punta', [-3.1, 34.8, -3.1], [6.2, 2.4, 6.2], G.solid((44, 150, 64)))
    c.part('punta', [-2.2, 37.2, -2.2], [4.4, 2.4, 4.4], G.solid((40, 140, 60)))
    c.bone('punta2', [0, 39.6, 0], 'punta', [-30, 0, 0])
    c.part('punta2', [-1.4, 39.6, -1.4], [2.8, 2.6, 2.8], G.solid((36, 130, 56)))
    c.part('punta2', [-0.8, 42.2, -0.8], [1.6, 2.2, 1.6], G.solid((32, 120, 50)))
    c.part('punta2', [-1.0, 44.2, -1.0], [2, 2, 2], G.solid((240, 200, 60)))
    c.build(os.path.join(RES, 'geo', 'gorro_duende.geo.json'), os.path.join(RES, 'textures', 'prenda', 'gorro_duende.png'))
    # diadema con cuernos de reno y nariz roja
    c = G.Creature('geometry.navidad.cuernos_reno', 64, 64, seed=7)
    cafe, cafe2 = (122, 82, 46), (150, 104, 60)
    c.bone('cabeza', [0, 24, 0])
    c.part('cabeza', [-4.6, 31.6, -0.6], [9.2, 0.9, 1.2], G.solid((60, 60, 64)))
    c.part('cabeza', [-4.7, 28.5, -0.6], [0.9, 3.2, 1.2], G.solid((60, 60, 64)))
    c.part('cabeza', [3.8, 28.5, -0.6], [0.9, 3.2, 1.2], G.solid((60, 60, 64)))
    for lado in (-1, 1):
        nb = 'cuerno_' + ('der' if lado < 0 else 'izq')
        c.bone(nb, [lado * 2.6, 32.4, 0], 'cabeza', [0, 0, lado * 18])
        x0 = lado * 2.6 - 0.6
        c.part(nb, [x0, 32.4, -0.6], [1.2, 5, 1.2], G.solid(cafe))
        c.part(nb, [x0 + (lado * 1.0 if lado > 0 else -1.6), 35.0, -0.5], [1.6, 1, 1], G.solid(cafe2))
        c.part(nb, [x0 + (lado * 1.4 if lado > 0 else -1.8), 35.0, -0.5], [1, 2.4, 1], G.solid(cafe2))
        c.part(nb, [x0 - (lado * 1.0 if lado > 0 else -1.0) - (0.6 if lado > 0 else 0), 36.4, -0.5], [1.2, 0.9, 1], G.solid(cafe2))
    c.part('cabeza', [-1.0, 26.2, -5.2], [2, 2, 1.2], G.solid((230, 30, 30)))
    c.glow('cabeza')
    c.build(os.path.join(RES, 'geo', 'cuernos_reno.geo.json'), os.path.join(RES, 'textures', 'prenda', 'cuernos_reno.png'))


def patines():
    """Patines de hielo: una bota blanca con puño rojo y agujetas, suela oscura y la cuchilla de metal. Cada uno sigue a su pierna."""
    c = G.Creature('geometry.navidad.patines', 64, 64, seed=8)
    blanco, rojo, agujeta, suela, metal = (238, 236, 230), (196, 30, 42), (90, 90, 98), (60, 44, 36), (196, 206, 218)
    for nb, x in (('pierna_der', -1.9), ('pierna_izq', 1.9)):
        c.bone(nb, [x, 12, 0])
        x0 = x - 2.35
        c.part(nb, [x0, 0, -2.35], [4.7, 5.2, 4.7], G.solid(blanco))
        c.part(nb, [x0 - 0.1, 4.6, -2.45], [4.9, 1.2, 4.9], G.solid(rojo))
        c.part(nb, [x0, 0, -3.3], [4.7, 2.2, 1.0], G.solid(blanco))
        for k in range(3):
            c.part(nb, [x - 1.2, 1.4 + k * 1.1, -2.5], [2.4, 0.4, 0.2], G.solid(agujeta))
        c.part(nb, [x0, -0.6, -3.4], [4.7, 0.6, 5.9], G.solid(suela))
        c.part(nb, [x - 0.3, -1.3, -2.6], [0.6, 0.7, 0.8], G.solid(metal))
        c.part(nb, [x - 0.3, -1.3, 1.4], [0.6, 0.7, 0.8], G.solid(metal))
        c.part(nb, [x - 0.3, -1.8, -3.6], [0.6, 0.5, 6.6], G.solid(metal))
        c.part(nb, [x - 0.3, -1.5, -4.1], [0.6, 0.7, 0.5], G.solid(metal))
    c.build(os.path.join(RES, 'geo', 'patines.geo.json'), os.path.join(RES, 'textures', 'prenda', 'patines.png'))


def invierno():
    """Bufanda de rayas (en el cuello, con las puntas colgando al frente) y orejeras peluditas con su diadema."""
    rojo, blanco, verde = (196, 30, 42), (242, 240, 234), (44, 140, 64)
    c = G.Creature('geometry.navidad.bufanda', 64, 64, seed=9)
    c.bone('cuerpo', [0, 24, 0])
    c.part('cuerpo', [-4.7, 22.2, -2.7], [9.4, 2.4, 5.4], G.solid(rojo))
    c.part('cuerpo', [-4.75, 23.0, -2.75], [9.5, 0.6, 5.5], G.solid(blanco))
    c.part('cuerpo', [0.8, 15.2, -3.1], [2.4, 7.2, 0.9], G.solid(rojo))
    c.part('cuerpo', [2.3, 16.6, -3.0], [2.2, 5.8, 0.8], G.solid(verde))
    for y in (17.2, 19.6):
        c.part('cuerpo', [0.75, y, -3.15], [2.5, 0.7, 1.0], G.solid(blanco))
    c.part('cuerpo', [0.8, 14.4, -3.1], [2.4, 0.8, 0.9], G.solid(blanco))
    c.part('cuerpo', [2.3, 15.8, -3.0], [2.2, 0.8, 0.8], G.solid(blanco))
    c.build(os.path.join(RES, 'geo', 'bufanda.geo.json'), os.path.join(RES, 'textures', 'prenda', 'bufanda.png'))
    c = G.Creature('geometry.navidad.orejeras', 64, 64, seed=10)
    rosa, diadema = (240, 176, 196), (230, 230, 236)
    c.bone('cabeza', [0, 24, 0])
    c.part('cabeza', [-4.5, 32.1, -0.6], [9.0, 0.8, 1.2], G.solid(diadema))
    c.part('cabeza', [-4.8, 29.2, -0.5], [0.8, 3.4, 1.0], G.solid(diadema))
    c.part('cabeza', [4.0, 29.2, -0.5], [0.8, 3.4, 1.0], G.solid(diadema))
    c.part('cabeza', [-5.9, 25.8, -1.9], [1.9, 3.8, 3.8], G.solid(rosa))
    c.part('cabeza', [4.0, 25.8, -1.9], [1.9, 3.8, 3.8], G.solid(rosa))
    c.part('cabeza', [-6.1, 26.4, -1.3], [0.4, 2.6, 2.6], G.solid(blanco))
    c.part('cabeza', [5.7, 26.4, -1.3], [0.4, 2.6, 2.6], G.solid(blanco))
    c.build(os.path.join(RES, 'geo', 'orejeras.geo.json'), os.path.join(RES, 'textures', 'prenda', 'orejeras.png'))


if __name__ == '__main__':
    R, W, V, D, N, Ca, Cn = '#b81e2a', '#f2f2ee', '#2a8a40', '#e8c040', '#1a2a5a', '#7a4a2a', '#d8b878'
    reno = mapa(['........', '.c....c.', '..c..c..', '..aaaa..', '.aaaaaa.', '..aEEa..', '..aaaa..', '...rr...', '........'],
                {'c': Cn, 'a': Ca, 'E': '#101010', 'r': '#ff2020'})
    pino = mapa(['........', '...ww...', '...gg...', '..gggg..', '...gg...', '..gggg..', '.gggggg.', '...tt...', '........'],
                {'w': D, 'g': '#1e7a34', 't': '#6a4020'})
    copo = mapa(['........', '...w....', '.w.w.w..', '..www...', 'wwwwwww.', '..www...', '.w.w.w..', '...w....', '........'],
                {'w': W})
    sueter('sueter_reno', R, W, V, [None, None] + reno[1:9])
    sueter('sueter_pinos', '#e8e0cc', R, V, [None, None] + pino[1:9])
    sueter('sueter_copos', '#2a4a8a', W, '#8ab8e8', [None, None] + copo[1:9])
    traje_santa()
    gorros()
    patines()
    invierno()
    print('ok')
