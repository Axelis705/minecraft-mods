#!/usr/bin/env python3
"""Cascabel, la mascota del Coleccionista: el modelo (.geo.json), sus animaciones (.animation.json) y su textura, hechos por código.

Un oso de peluche gigante y roto: parado en dos patas, encorvado, con brazos largos que le llegan a las rodillas. Le falta un botón
(cuelga de un hilo) y por el hueco se asoma un ojo rojo; la oreja derecha está descosida y por la panza se le sale el relleno. Por las
costuras de las patas le salen garras de hueso. La boca cosida se abre como una mordida llena de dientes. Lleva un moño rojo con un
cascabel de latón (lo oye quien se esconde), un parche de cuadros cosido en el hombro y una llave de cuerda en la espalda: es un juguete
de la colección al que le dieron cuerda.

Uso: python3 herramientas/mascota.py [--vista carpeta]   (con --vista también dibuja una vista previa en PNG)
Escribe en src/main/resources/assets/coleccionista: geo/cascabel.geo.json, animations/cascabel.animation.json y
textures/entity/cascabel.png.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

AQUI = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista')
TW, TH = 256, 256
DZ = 0  # (en dos patas el jugador queda en medio)

# ---------------------------------------------------------------- el modelo
# cada cubo: (hueso, origen x y z, tamaño x y z, material, extra)  (unidades de Blockbench: 16 = 1 bloque, y hacia arriba, frente = -z)
HUESOS = []   # (nombre, padre, pivote, giro)
CUBOS = []    # dict


def hueso(nombre, padre, pivote, giro=(0, 0, 0)):
    HUESOS.append((nombre, padre, [pivote[0], pivote[1], pivote[2] + DZ], list(giro)))


def cubo(h, o, t, mat, inflar=0.0, giro=None, pivote=None, espejo=False):
    c = {'hueso': h, 'o': [o[0], o[1], o[2] + DZ], 't': list(t), 'mat': mat, 'inflar': inflar, 'espejo': espejo}
    if giro:
        c['giro'] = list(giro)
        c['pivote'] = [pivote[0], pivote[1], pivote[2] + DZ]
    CUBOS.append(c)


def modelo():
    hueso('root', None, (0, 0, 0))
    hueso('body', 'root', (0, 13, 0), (8, 0, 0))                          # encorvado hacia adelante
    cubo('body', (-9, 12, -6), (18, 17, 13), 'pelo')                      # la panza redonda
    cubo('body', (-10, 26, -5.5), (20, 9, 12), 'pelo')                    # el pecho y los hombros
    cubo('body', (-6.5, 14, -6.5), (13, 13, 0.5), 'pelo_claro')           # la panza clarita
    cubo('body', (-0.4, 13, -6.9), (0.8, 15, 0.4), 'costura')             # la costura de en medio, abierta abajo
    cubo('body', (-3, 15, -8.5), (6, 5, 2.5), 'relleno')                  # el relleno que se sale
    cubo('body', (-1.5, 13.5, -9.5), (3, 2.5, 1.5), 'relleno')
    cubo('body', (2, 19.5, -7.6), (2, 2, 1.5), 'relleno')
    cubo('body', (5.5, 27.5, -6), (5, 6, 0.6), 'parche')                  # un parche de cuadros en el pecho
    cubo('body', (-2, 13, 6.5), (4, 4, 3), 'pelo')                        # la colita
    hueso('key', 'body', (0, 24, 6.5))
    cubo('key', (-1, 23, 6.5), (2, 2, 4), 'laton')                        # la llave de cuerda (en la espalda)
    cubo('key', (-5, 20.5, 10.5), (10, 7, 1), 'llave')
    hueso('head', 'body', (0, 34, -1))
    cubo('head', (-9, 33, -9), (18, 15, 15), 'pelo')                      # la cabezota
    cubo('head', (-4.5, 36, -12.5), (9, 5, 4), 'hocico')                  # el hocico (arriba de la boca)
    cubo('head', (-1.5, 39.5, -13), (3, 2, 1), 'nariz')
    cubo('head', (-0.25, 37.5, -12.8), (0.5, 2, 0.3), 'costura')          # el hilo de la nariz a la boca
    for k, x in enumerate((-4, -2.4, -0.8, 0.8, 2.4)):
        cubo('head', (x, 34.6 - (k % 2) * 0.6, -12.3), (0.9, 1.6 + (k % 2) * 0.6, 0.9), 'hueso')   # colmillos de arriba
    cubo('head', (2.5, 41, -9.6), (3.5, 3.5, 0.8), 'boton')               # su ojo: un botón
    cubo('head', (-6.5, 41, -9.3), (3.5, 3.5, 0.4), 'cuenca')             # el otro se cayó: un hueco
    cubo('head', (-8.4, 34.2, -9.7), (2.6, 2.6, 0.6), 'boton')            # ... y cuelga de un hilo
    cubo('head', (-7.3, 36.8, -9.55), (0.4, 4.4, 0.3), 'hilo')
    cubo('head', (-9.5, 40, -3), (0.6, 4, 6), 'costura_x')                # costuras en la cabeza
    hueso('eyes', 'head', (0, 42.5, -9.6))
    cubo('eyes', (-5.9, 41.6, -9.6), (2.2, 2.2, 0.3), 'ojo')              # el ojo rojo que se asoma por el hueco
    cubo('eyes', (3.8, 42.4, -10.05), (0.8, 0.8, 0.1), 'ojo_chico')       # un brillo rojo en el botón
    hueso('jaw', 'head', (0, 35.5, -8))
    cubo('jaw', (-4.5, 32.5, -12.5), (9, 3.5, 4), 'quijada')
    for k, x in enumerate((-3.6, -2, -0.4, 1.2, 2.8)):
        cubo('jaw', (x, 35.6, -12.1), (0.9, 1.4 + (k % 2) * 0.5, 0.9), 'hueso')   # colmillos de abajo
    hueso('mouth', 'jaw', (0, 33, -12))                                   # donde queda la presa (localizador)
    hueso('ear_l', 'head', (6.5, 47, -2))
    cubo('ear_l', (4, 45.5, -3), (5, 5, 2), 'oreja')
    hueso('ear_r', 'head', (-6.5, 47, -2), (0, 0, 25))                    # la descosida: chueca y con relleno
    cubo('ear_r', (-9, 45.5, -3), (5, 4, 2), 'oreja')
    cubo('ear_r', (-8, 44.6, -2.6), (3, 1.4, 1.2), 'relleno')
    # el moño rojo con el cascabel
    hueso('collar', 'body', (0, 33, -6))
    cubo('collar', (-1.5, 31.5, -8), (3, 3, 2), 'cinta')
    cubo('collar', (-6, 31.8, -7.5), (4.5, 3.4, 1), 'cinta')
    cubo('collar', (1.5, 31.8, -7.5), (4.5, 3.4, 1), 'cinta')
    cubo('collar', (-2.5, 28.5, -7.4), (1.5, 3.2, 0.6), 'cinta', giro=(0, 0, -15), pivote=(-1.75, 31.5, -7.1))
    cubo('collar', (1, 28.5, -7.4), (1.5, 3.2, 0.6), 'cinta', giro=(0, 0, 15), pivote=(1.75, 31.5, -7.1))
    hueso('bell', 'collar', (0, 31.5, -8.5))
    cubo('bell', (-1.5, 28.6, -10), (3, 3, 3), 'cascabel')
    # los brazos: largos, con garras de hueso que salen por las costuras de las manos
    for lado, x in (('l', 1), ('r', -1)):
        xa = 9.5 if x > 0 else -14.5
        hueso('arm_' + lado, 'body', (12 * x, 32, -0.5))
        cubo('arm_' + lado, (xa, 20, -3.5), (5, 13, 6), 'pelo')
        if lado == 'r':
            cubo('arm_' + lado, (xa - 0.3, 27, -3.8), (5.6, 1, 6.6), 'costura_x')   # el hombro cosido
            cubo('arm_' + lado, (xa - 0.8, 28, -2), (1.2, 2.5, 3), 'relleno')
        hueso('arm_' + lado + '_low', 'arm_' + lado, (12 * x, 21, -0.5))
        cubo('arm_' + lado + '_low', (xa + 0.25, 9, -3.25), (4.5, 12, 5.5), 'pelo')
        cubo('arm_' + lado + '_low', (xa - 0.5, 4.5, -4), (6, 5, 7), 'pelo')     # la mano (zarpa)
        cubo('arm_' + lado + '_low', (xa - 0.6, 4.5, -4.3), (6.2, 3, 0.6), 'pelo_claro')
        for k in range(3):
            cubo('arm_' + lado + '_low', (xa + 0.3 + 1.9 * k, 1, -4.4), (1, 4, 1.3), 'hueso',
                 giro=(-20, 0, 0), pivote=(xa + 0.8 + 1.9 * k, 5, -3.75))
        # las piernas: cortas y gordas, con la planta clarita y garras
        xl = 1.5 if x > 0 else -8.5
        hueso('leg_' + lado, 'root', (5 * x, 13, 0))
        cubo('leg_' + lado, (xl, 3, -3.5), (7, 11, 7), 'pelo' if lado == 'r' else 'parche_pierna')
        cubo('leg_' + lado, (xl - 0.5, 0, -6), (8, 4, 10), 'pelo')
        cubo('leg_' + lado, (xl + 0.5, 0.5, -6.3), (6, 3, 0.4), 'pelo_claro')
        for k in range(3):
            cubo('leg_' + lado, (xl + 0.5 + 2.2 * k, 0, -8), (1, 1.4, 2.2), 'hueso')


# ---------------------------------------------------------------- texturas
R = np.random.RandomState(31)


def ruido(w, h, celda, semilla):
    rs = np.random.RandomState(semilla)
    g = rs.rand(h // celda + 3, w // celda + 3)
    y, x = np.mgrid[0:h, 0:w] / celda
    ix, iy = x.astype(int), y.astype(int)
    fx, fy = x - ix, y - iy
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a = g[iy, ix] * (1 - fx) + g[iy, ix + 1] * fx
    b = g[iy + 1, ix] * (1 - fx) + g[iy + 1, ix + 1] * fx
    return a * (1 - fy) + b * fy


def hexc(c):
    c = c.lstrip('#')
    return np.array([int(c[i:i + 2], 16) for i in (0, 2, 4)], np.float32)


def pelaje(w, h, s, base='#9a6a32', raya='#2a1a10', rayas=True, claro=None):
    n = ruido(w, h, 3, s) * 0.6 + R.rand(h, w) * 0.4
    rgb = hexc(base) * (0.78 + 0.35 * n)[..., None]
    if claro is not None:
        k = np.linspace(0, 1, h)[:, None] ** 2
        rgb = rgb * (1 - k[..., None] * 0.6) + hexc(claro) * (k[..., None] * 0.6)
    if rayas:
        y, x = np.mgrid[0:h, 0:w]
        f = np.sin((x + ruido(w, h, 4, s + 1) * 9) * 0.55 + y * 0.12)
        m = f > 0.72
        rgb = np.where(m[..., None], hexc(raya) * (0.8 + 0.3 * n[..., None]), rgb)
    # polvo y calvas de animal disecado
    calva = ruido(w, h, 5, s + 7) > 0.78
    rgb = np.where(calva[..., None], rgb * 0.72 + hexc('#5a4a38') * 0.28, rgb)
    return rgb


def porcelana(w, h, s):
    n = ruido(w, h, 4, s) * 0.5 + R.rand(h, w) * 0.15
    rgb = hexc('#e4ddcc') * (0.9 + 0.12 * n)[..., None]
    return rgb


def peluche(w, h, s, base='#7c5232', oscuro=0.0):
    """Pelo de peluche viejo: pelusa fina, mugre y partes gastadas (calvas) más claras."""
    n = ruido(w, h, 2, s) * 0.45 + R.rand(h, w) * 0.4 + ruido(w, h, 5, s + 3) * 0.15
    rgb = hexc(base) * (0.74 + 0.36 * n)[..., None]
    mugre = ruido(w, h, 4, s + 9) > 0.7
    rgb = np.where(mugre[..., None], rgb * 0.7 + hexc('#3a2a1c') * 0.3, rgb)
    gastado = ruido(w, h, 3, s + 17) > 0.8
    rgb = np.where(gastado[..., None], rgb * 0.8 + hexc('#b89a76') * 0.2, rgb)
    return rgb * (1 - oscuro)


def cuadros(w, h, s):
    """Tela de cuadros (roja y azul marino), para los parches."""
    y, x = np.mgrid[0:h, 0:w]
    rgb = np.broadcast_to(hexc('#7a1820'), (h, w, 3)).copy()
    rgb[(x % 4) < 1] = hexc('#1c2240')
    rgb[(y % 4) < 1] = hexc('#1c2240')
    rgb[((x % 4) < 1) & ((y % 4) < 1)] = hexc('#0e1020')
    return rgb * (0.82 + 0.25 * R.rand(h, w))[..., None]


def puntadas(rgb, horizontal=True, color='#2a1a12'):
    """Puntadas en cruz por el borde (de arriba si horizontal, de la izquierda si no)."""
    h, w = rgb.shape[:2]
    c = hexc(color)
    if horizontal:
        for xx in range(0, w, 2):
            rgb[0 if xx % 4 == 0 else min(1, h - 1), xx] = c
    else:
        for yy in range(0, h, 2):
            rgb[yy, 0 if yy % 4 == 0 else min(1, w - 1)] = c


def pintar_cara(img, x, y, w, h, mat, cara, s):
    """Pinta una cara del cubo (x, y = esquina en la textura; w, h = tamaño en texeles)."""
    if w <= 0 or h <= 0:
        return
    a = np.full((h, w), 255, np.uint8)
    if mat == 'pelo':
        rgb = peluche(w, h, s, oscuro=0.12 if cara == 'down' else 0.0)
    elif mat == 'parche_pierna':
        rgb = peluche(w, h, s)
        if cara == 'north' and w >= 5 and h >= 6:
            px, py, pw, ph = 1, 2, w - 2, h - 5
            rgb[py:py + ph, px:px + pw] = cuadros(pw, ph, s)
            for xx in range(px, px + pw, 2):
                rgb[py, xx] = hexc('#e2d6b8')
                rgb[py + ph - 1, xx] = hexc('#e2d6b8')
    elif mat == 'pelo_claro':
        rgb = peluche(w, h, s, base='#cfab7e')
    elif mat in ('hocico', 'quijada'):
        rgb = peluche(w, h, s, base='#c9a174')
        if (mat == 'hocico' and cara == 'down') or (mat == 'quijada' and cara == 'up'):
            rgb = hexc('#4a0a12') * (0.7 + 0.5 * ruido(w, h, 1, s))[..., None]   # la boca por dentro
        elif cara in ('north', 'east', 'west'):
            # la boca cosida: una raya de puntadas donde se abre
            fila = h - 1 if mat == 'hocico' else 0
            rgb[fila, :] = hexc('#2a1410')
            for xx in range(0, w, 2):
                rgb[max(0, min(h - 1, fila + (-1 if mat == 'hocico' else 1))), xx] = hexc('#2a1410')
    elif mat == 'oreja':
        rgb = peluche(w, h, s)
        if cara == 'north' and w > 2 and h > 2:
            rgb[1:h - 1, 1:w - 1] = peluche(w - 2, h - 2, s + 1, base='#c9a174')
    elif mat == 'relleno':
        n = ruido(w, h, 1, s) * 0.6 + R.rand(h, w) * 0.4
        rgb = hexc('#ece5d2') * (0.82 + 0.2 * n)[..., None]
        a[(R.rand(h, w) < 0.12)] = 0          # borde de pelusa
    elif mat == 'parche':
        rgb = cuadros(w, h, s)
        puntadas(rgb, True, '#e2d6b8')
    elif mat in ('costura', 'costura_x'):
        rgb = peluche(w, h, s, base='#6a4428')
        c = hexc('#2a1a12')
        if mat == 'costura':
            rgb[:, :] = rgb * 0.6
            for yy in range(0, h, 2):
                rgb[yy, :] = c
        else:
            for xx in range(0, w, 2):
                rgb[:, xx] = np.where((np.arange(h) % 2 == xx % 4 // 2)[:, None], c, rgb[:, xx])
    elif mat == 'boton':
        rgb = np.broadcast_to(hexc('#1c1616'), (h, w, 3)).copy()
        if cara == 'north' and w >= 3 and h >= 3:
            rgb[0, :] = rgb[-1, :] = rgb[:, 0] = rgb[:, -1] = hexc('#3a3232')     # el borde
            rgb[h // 2 - 1:h // 2 + 1, w // 2 - 1:w // 2 + 1] = hexc('#4a4040') # los agujeros
            rgb[h // 2 - 1, w // 2 - 1] = rgb[h // 2, w // 2] = hexc('#0a0808')
    elif mat == 'cuenca':
        rgb = np.broadcast_to(hexc('#0c0606'), (h, w, 3)).copy()
        if cara == 'north':
            rgb[0, :] = rgb[:, 0] = hexc('#ece5d2')   # el relleno a la orilla del hueco
    elif mat == 'hilo':
        rgb = np.broadcast_to(hexc('#d8ccb0'), (h, w, 3)).copy()
    elif mat == 'cinta':
        n = ruido(w, h, 2, s)
        rgb = hexc('#a8141e') * (0.7 + 0.45 * n)[..., None]
        if h > 2:
            rgb[h // 3, :] = rgb[h // 3, :] * 1.25       # brillo del listón
    elif mat == 'cascabel':
        rgb = hexc('#c9a240') * (0.75 + 0.4 * ruido(w, h, 2, s))[..., None]
        if cara in ('north', 'south', 'east', 'west'):
            rgb[int(h * 0.5):int(h * 0.62), :] = hexc('#2a1e0a')
            rgb[int(h * 0.62):, int(w * 0.4):int(w * 0.6) + 1] = hexc('#2a1e0a')
    elif mat in ('laton', 'llave'):
        rgb = hexc('#b08a3a') * (0.75 + 0.35 * ruido(w, h, 2, s))[..., None]
    elif mat == 'hueso':
        n = ruido(w, h, 2, s) * 0.6 + R.rand(h, w) * 0.4
        rgb = hexc('#ddd2b6') * (0.8 + 0.25 * n)[..., None]
        if cara in ('east', 'west', 'north', 'south') and h > 2:
            k = np.linspace(0, 1, h)[:, None]
            rgb = rgb * (1 - 0.2 * k[..., None])
    elif mat == 'nariz':
        rgb = hexc('#1e1410') * (0.8 + 0.3 * R.rand(h, w))[..., None]
        if cara == 'north':
            for xx in range(0, w, 2):
                rgb[:, xx] = hexc('#3a2a22')
    elif mat == 'ojo':
        rgb = np.broadcast_to(hexc('#ff2a18'), (h, w, 3)).copy()
        if w >= 2 and h >= 2:
            rgb[:, w // 2] = hexc('#ffd24a')
    elif mat == 'ojo_chico':
        rgb = np.broadcast_to(hexc('#ff3a20'), (h, w, 3)).copy()
    else:
        rgb = np.broadcast_to(hexc('#ff00ff'), (h, w, 3)).copy()
    # el mango de la llave: dos aros (con hueco)
    if mat == 'llave' and cara in ('north', 'south'):
        yy, xx = np.mgrid[0:h, 0:w]
        for cx in (w * 0.27, w * 0.73):
            d = np.hypot((xx + 0.5 - cx) / (w * 0.22), (yy + 0.5 - h * 0.5) / (h * 0.42))
            a[d < 0.55] = 0
        a[:, int(w * 0.46):int(w * 0.54)] = 255
    # una costura en un costado del pelo (donde se cosieron los pedazos): raya oscura con puntadas
    if mat in ('pelo', 'parche_pierna') and cara in ('east', 'west') and w >= 5 and h >= 8 and s % 3 == 0:
        xx = w // 2
        rgb[1:h - 1, xx] = rgb[1:h - 1, xx] * 0.55
        for yy in range(2, h - 2, 3):
            rgb[yy, max(0, xx - 1)] = hexc('#2a1a12')
            rgb[yy, min(w - 1, xx + 1)] = hexc('#2a1a12')
    out = np.dstack([np.clip(rgb, 0, 255).astype(np.uint8), a])
    img.paste(Image.fromarray(out, 'RGBA'), (x, y))


def caras_caja(u, v, w, h, d):
    """Las caras de un cubo con UV de caja (como Minecraft): cara -> (x, y, ancho, alto)."""
    return {
        'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
        'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
        'west': (u + d + w, v + d, d, h), 'south': (u + d + w + d, v + d, w, h),
    }


def empacar():
    """Acomoda las UV de caja de todos los cubos en la textura (por estantes)."""
    tam = []
    for i, c in enumerate(CUBOS):
        w, h, d = (max(1, int(math.ceil(x))) for x in c['t'])
        tam.append((i, 2 * (w + d), d + h, w, h, d))
    tam.sort(key=lambda r: -r[2])
    x = y = fila = 0
    for i, ww, hh, w, h, d in tam:
        if x + ww > TW:
            x, y, fila = 0, y + fila, 0
        if y + hh > TH:
            raise SystemExit('no cabe en la textura')
        CUBOS[i]['uv'] = [x, y]
        CUBOS[i]['uvt'] = (w, h, d)
        x += ww
        fila = max(fila, hh)


def textura():
    img = Image.new('RGBA', (TW, TH), (0, 0, 0, 0))
    for i, c in enumerate(CUBOS):
        u, v = c['uv']
        w, h, d = c['uvt']
        for cara, (x, y, cw, ch) in caras_caja(u, v, w, h, d).items():
            pintar_cara(img, x, y, cw, ch, c['mat'], cara, 1000 + i * 7 + len(cara))
    return img


def geo():
    bones = []
    for nombre, padre, piv, giro in HUESOS:
        b = {'name': nombre, 'pivot': piv}
        if padre:
            b['parent'] = padre
        if any(giro):
            b['rotation'] = giro
        cs = []
        for c in CUBOS:
            if c['hueso'] != nombre:
                continue
            x = {'origin': c['o'], 'size': c['t'], 'uv': c['uv']}
            if c['inflar']:
                x['inflate'] = c['inflar']
            if c.get('giro'):
                x['rotation'] = c['giro']
                x['pivot'] = c['pivote']
            cs.append(x)
        if cs:
            b['cubes'] = cs
        bones.append(b)
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.coleccionista.cascabel', 'texture_width': TW, 'texture_height': TH,
                        'visible_bounds_width': 6, 'visible_bounds_height': 4, 'visible_bounds_offset': [0, 1.5, 0]},
        'axelis_glow': ['eyes'],
        'bones': bones}]}


# ---------------------------------------------------------------- animaciones
def anims():
    T = 'q.anim_time'
    a = {}
    AF = 6  # los brazos cuelgan un poco hacia afuera
    # quieto: respira, la cabeza se ladea despacio (y de pronto da un tirón), los brazos cuelgan y se mecen, la llave gira sola
    a['animation.cascabel.idle'] = {'loop': True, 'animation_length': 4.0, 'bones': {
        'body': {'position': [0, f'math.sin({T}*90)*0.3', 0], 'rotation': [f'math.sin({T}*90)*1.5', 0, 0]},
        'head': {'rotation': [f'math.sin({T}*45)*3', f'math.sin({T}*30)*10',
                              f'({T} > 2.6 && {T} < 3.5) ? -13 : math.sin({T}*45)*7']},   # se ladea y de pronto da un tirón
        'jaw': {'rotation': [f'math.max(0, math.sin({T}*90 - 60))*4', 0, 0]},
        'arm_l': {'rotation': [f'math.sin({T}*90)*3', 0, f'{AF} + math.sin({T}*90)*1.5']},
        'arm_r': {'rotation': [f'math.sin({T}*90 + 40)*3', 0, f'-{AF} - math.sin({T}*90)*1.5']},
        'arm_l_low': {'rotation': [-6, 0, 0]}, 'arm_r_low': {'rotation': [-6, 0, 0]},
        'ear_r': {'rotation': [0, 0, f'({T} > 2.9 && {T} < 3.3) ? 16 : 0']},
        'bell': {'rotation': [f'math.sin({T}*90)*5', 0, 0]},
        'key': {'rotation': [0, 0, f'{T}*90']},
    }}
    # camina: se bambolea de lado a lado como un peluche pesado, los brazos se mecen al revés de las piernas
    w = 300
    a['animation.cascabel.walk'] = {'loop': True, 'animation_length': 1.2, 'bones': {
        'body': {'position': [0, f'math.abs(math.sin({T}*{w}))*0.8', 0],
                 'rotation': [0, f'math.sin({T}*{w})*5', f'math.sin({T}*{w})*5']},
        'head': {'rotation': [f'math.sin({T}*{w*2})*3', 0, f'-math.sin({T}*{w})*5']},
        'leg_l': {'rotation': [f'math.sin({T}*{w})*28', 0, 0]},
        'leg_r': {'rotation': [f'-math.sin({T}*{w})*28', 0, 0]},
        'arm_l': {'rotation': [f'-math.sin({T}*{w})*22', 0, AF]},
        'arm_r': {'rotation': [f'math.sin({T}*{w})*22', 0, -AF]},
        'arm_l_low': {'rotation': [f'-10 - math.max(0, math.sin({T}*{w}))*14', 0, 0]},
        'arm_r_low': {'rotation': [f'-10 - math.max(0, -math.sin({T}*{w}))*14', 0, 0]},
        'bell': {'rotation': [f'math.sin({T}*{w*2})*26', 0, f'math.sin({T}*{w})*12']},
        'ear_r': {'rotation': [0, 0, f'math.sin({T}*{w*2})*8']},
        'key': {'rotation': [0, 0, f'{T}*180']},
    }}
    # corre: se va de frente, con los brazos estirados hacia adelante y la boca abierta
    r = 600
    a['animation.cascabel.run'] = {'loop': True, 'animation_length': 0.6, 'bones': {
        'body': {'position': [0, f'math.abs(math.sin({T}*{r}))*1.4', 0], 'rotation': [14, 0, f'math.sin({T}*{r})*4']},
        'head': {'rotation': [-14, 0, f'-math.sin({T}*{r})*4']},
        'jaw': {'rotation': [f'16 + math.sin({T}*{r*2})*6', 0, 0]},
        'leg_l': {'rotation': [f'math.sin({T}*{r})*46', 0, 0]},
        'leg_r': {'rotation': [f'-math.sin({T}*{r})*46', 0, 0]},
        'arm_l': {'rotation': [f'-70 + math.sin({T}*{r})*22', 0, 10]},
        'arm_r': {'rotation': [f'-70 - math.sin({T}*{r})*22', 0, -10]},
        'arm_l_low': {'rotation': [-20, 0, 0]}, 'arm_r_low': {'rotation': [-20, 0, 0]},
        'bell': {'rotation': [f'math.sin({T}*{r*2})*38', 0, f'math.sin({T}*{r})*16']},
        'ear_l': {'rotation': [-20, 0, 0]}, 'ear_r': {'rotation': [-20, 0, 0]},
        'key': {'rotation': [0, 0, f'{T}*360']},
    }}
    # olfatea: se agacha, la cabeza baja y huele a golpecitos de un lado a otro
    a['animation.cascabel.olfatear'] = {'loop': False, 'animation_length': 2.4, 'bones': {
        'body': {'rotation': {'0.0': [0, 0, 0], '0.35': {'vector': [24, 0, 0], 'easing': 'easeOutQuad'}, '2.0': [24, 0, 0],
                              '2.4': {'vector': [0, 0, 0], 'easing': 'easeInOutSine'}},
                 'position': {'0.0': [0, 0, 0], '0.35': [0, -1.5, 0], '2.0': [0, -1.5, 0], '2.4': [0, 0, 0]}},
        'head': {'rotation': {'0.0': [0, 0, 0], '0.35': [16, 0, 0], '0.6': [20, -18, 0], '0.75': [16, -16, 6], '0.9': [20, -14, 0],
                              '1.2': [18, 16, 0], '1.35': [22, 18, -6], '1.5': [18, 14, 0], '1.8': [20, 0, 0], '2.0': [16, 0, 0],
                              '2.4': [0, 0, 0]}},
        'jaw': {'rotation': [f'math.max(0, math.sin({T}*1400))*5', 0, 0]},
        'arm_l': {'rotation': {'0.0': [0, 0, AF], '0.35': [-22, 0, 4], '2.0': [-22, 0, 4], '2.4': [0, 0, AF]}},
        'arm_r': {'rotation': {'0.0': [0, 0, -AF], '0.35': [-22, 0, -4], '2.0': [-22, 0, -4], '2.4': [0, 0, -AF]}},
        'ear_l': {'rotation': [-12, 0, 0]},
    }}
    # ruge: se echa para atrás, levanta los brazos con las garras abiertas, abre la boca de golpe y sacude la cabeza
    a['animation.cascabel.rugir'] = {'loop': False, 'animation_length': 2.2, 'bones': {
        'body': {'rotation': {'0.0': [0, 0, 0], '0.4': {'vector': [-14, 0, 0], 'easing': 'easeOutBack'}, '1.6': [-12, 0, 0],
                              '2.2': {'vector': [0, 0, 0], 'easing': 'easeInOutSine'}}},
        'head': {'rotation': {'0.0': [0, 0, 0], '0.4': [-22, 0, 0], '0.7': [6, 0, 0], '1.6': [4, 0, 0], '2.2': [0, 0, 0]},
                 'position': [f'math.sin({T}*2400)*(({T} > 0.6 && {T} < 1.6) ? 0.35 : 0)', 0, 0]},
        'jaw': {'rotation': {'0.0': [0, 0, 0], '0.45': {'vector': [55, 0, 0], 'easing': 'easeOutQuad'}, '1.55': [50, 0, 0],
                             '1.9': {'vector': [0, 0, 0], 'easing': 'easeInQuad'}}},
        'arm_l': {'rotation': {'0.0': [0, 0, AF], '0.4': {'vector': [-135, 0, 38], 'easing': 'easeOutBack'}, '1.6': [-128, 0, 34],
                               '2.2': {'vector': [0, 0, AF], 'easing': 'easeInOutSine'}}},
        'arm_r': {'rotation': {'0.0': [0, 0, -AF], '0.4': {'vector': [-135, 0, -38], 'easing': 'easeOutBack'}, '1.6': [-128, 0, -34],
                               '2.2': {'vector': [0, 0, -AF], 'easing': 'easeInOutSine'}}},
        'arm_l_low': {'rotation': {'0.0': [0, 0, 0], '0.4': [-35, 0, 0], '1.6': [-35, 0, 0], '2.2': [0, 0, 0]}},
        'arm_r_low': {'rotation': {'0.0': [0, 0, 0], '0.4': [-35, 0, 0], '1.6': [-35, 0, 0], '2.2': [0, 0, 0]}},
        'ear_l': {'rotation': [-30, 0, 0]}, 'ear_r': {'rotation': [-30, 0, 30]},
        'bell': {'rotation': [f'math.sin({T}*1800)*30', 0, 0]},
        'eyes': {'scale': {'0.0': [1, 1, 1], '0.4': [1.5, 1.5, 1], '1.6': [1.5, 1.5, 1], '2.2': [1, 1, 1]}},
    }}
    # come: se lanza, agarra con las dos garras, lo sube a la boca, muerde varias veces sacudiendo la cabeza y se lo traga
    a['animation.cascabel.comer'] = {'loop': False, 'animation_length': 2.6, 'bones': {
        'body': {'rotation': {'0.0': [0, 0, 0], '0.25': {'vector': [24, 0, 0], 'easing': 'easeOutQuad'}, '0.55': [6, 0, 0],
                              '1.5': [6, 0, 0], '1.8': [-10, 0, 0], '2.6': {'vector': [0, 0, 0], 'easing': 'easeInOutSine'}},
                 'scale': {'0.0': [1, 1, 1], '1.85': [1, 1, 1], '2.05': [1.08, 1.04, 1.12], '2.4': [1, 1, 1]}},
        'arm_l': {'rotation': {'0.0': [0, 0, AF], '0.25': {'vector': [-88, 0, -6], 'easing': 'easeOutQuad'}, '0.55': [-98, 0, -16],
                               '1.5': [-96, 0, -14], '2.0': [-30, 0, 0], '2.6': {'vector': [0, 0, AF], 'easing': 'easeInOutSine'}}},
        'arm_r': {'rotation': {'0.0': [0, 0, -AF], '0.25': {'vector': [-88, 0, 6], 'easing': 'easeOutQuad'}, '0.55': [-98, 0, 16],
                               '1.5': [-96, 0, 14], '2.0': [-30, 0, 0], '2.6': {'vector': [0, 0, -AF], 'easing': 'easeInOutSine'}}},
        'arm_l_low': {'rotation': {'0.0': [0, 0, 0], '0.25': [-10, 0, 0], '0.55': [-55, 0, 0], '1.5': [-50, 0, 0], '2.0': [-10, 0, 0],
                                   '2.6': [0, 0, 0]}},
        'arm_r_low': {'rotation': {'0.0': [0, 0, 0], '0.25': [-10, 0, 0], '0.55': [-55, 0, 0], '1.5': [-50, 0, 0], '2.0': [-10, 0, 0],
                                   '2.6': [0, 0, 0]}},
        'head': {'rotation': {'0.0': [0, 0, 0], '0.3': [8, 0, 0], '0.55': [10, 0, -12], '0.75': [10, 0, 12], '0.95': [10, 0, -10],
                              '1.15': [10, 0, 10], '1.4': [6, 0, 0], '1.8': [-26, 0, 0], '2.2': [-22, 0, 0], '2.6': [0, 0, 0]}},
        'jaw': {'rotation': {'0.0': [0, 0, 0], '0.35': {'vector': [60, 0, 0], 'easing': 'easeOutQuad'}, '0.55': [4, 0, 0],
                             '0.7': [42, 0, 0], '0.85': [4, 0, 0], '1.0': [40, 0, 0], '1.15': [4, 0, 0], '1.3': [34, 0, 0],
                             '1.45': [0, 0, 0], '1.85': [22, 0, 0], '2.0': [0, 0, 0]}},
        'collar': {'scale': {'0.0': [1, 1, 1], '1.85': [1, 1, 1], '2.0': [1.15, 1.25, 1.1], '2.25': [1, 1, 1]}},
        'bell': {'rotation': [f'math.sin({T}*1600)*30', 0, 0]},
        'eyes': {'scale': {'0.0': [1, 1, 1], '0.3': [1.4, 1.4, 1], '1.5': [1.4, 1.4, 1], '1.8': [1, 1, 1]}},
    }}
    return {'format_version': '1.8.0', 'animations': a}


# ---------------------------------------------------------------- vista previa (sin Minecraft): cajas pintadas con su color promedio
def rotm(rx, ry, rz):
    rx, ry, rz = (math.radians(v) for v in (rx, ry, rz))
    X = np.array([[1, 0, 0], [0, math.cos(rx), -math.sin(rx)], [0, math.sin(rx), math.cos(rx)]])
    Y = np.array([[math.cos(ry), 0, math.sin(ry)], [0, 1, 0], [-math.sin(ry), 0, math.cos(ry)]])
    Z = np.array([[math.cos(rz), -math.sin(rz), 0], [math.sin(rz), math.cos(rz), 0], [0, 0, 1]])
    return Z @ Y @ X


def vista(tex, salida, yaw=35, pitch=18, extra=None):
    extra = extra or {}
    mats = {}
    porn = {h[0]: h for h in HUESOS}

    def mundo(nombre):
        if nombre in mats:
            return mats[nombre]
        n, padre, piv, giro = porn[nombre]
        g = np.array(giro, float) + np.array(extra.get(nombre, (0, 0, 0)), float)
        # (como en Java con y hacia abajo: los giros en x y en z cambian de signo al ver con y hacia arriba)
        R_ = rotm(-g[0], g[1], -g[2])
        M = np.eye(4)
        M[:3, :3] = R_
        T1, T2 = np.eye(4), np.eye(4)
        T1[:3, 3] = piv
        T2[:3, 3] = [-v for v in piv]
        L = T1 @ M @ T2
        P = mundo(padre) if padre else np.eye(4)
        mats[nombre] = P @ L
        return mats[nombre]
    caras = []
    t = np.array(tex)
    for c in CUBOS:
        M = mundo(c['hueso'])
        o, s = np.array(c['o'], float), np.array(c['t'], float)
        o -= c['inflar']
        s += 2 * c['inflar']
        u, v = c['uv']
        w, h, d = c['uvt']
        reg = caras_caja(u, v, w, h, d)
        defs = {
            'north': ([0, 0, 0], [1, 0, 0], [1, 1, 0], [0, 1, 0]), 'south': ([0, 0, 1], [1, 0, 1], [1, 1, 1], [0, 1, 1]),
            'west': ([0, 0, 0], [0, 0, 1], [0, 1, 1], [0, 1, 0]), 'east': ([1, 0, 0], [1, 0, 1], [1, 1, 1], [1, 1, 0]),
            'up': ([0, 1, 0], [1, 1, 0], [1, 1, 1], [0, 1, 1]), 'down': ([0, 0, 0], [1, 0, 0], [1, 0, 1], [0, 0, 1])}
        for cara, pts in defs.items():
            q = [M @ np.append(o + np.array(p) * s, 1) for p in pts]
            x, y, cw, ch = reg[cara]
            parche = t[y:y + max(1, ch), x:x + max(1, cw)]
            al = parche[..., 3] > 0
            col = parche[..., :3][al].mean(0) if al.any() else np.array([0, 0, 0])
            caras.append(([p[:3] for p in q], col, al.mean() if al.size else 0))
    ya, pi = math.radians(yaw), math.radians(pitch)
    Rv = rotm(0, 0, 0)
    Ry = np.array([[math.cos(ya), 0, math.sin(ya)], [0, 1, 0], [-math.sin(ya), 0, math.cos(ya)]])
    Rx = np.array([[1, 0, 0], [0, math.cos(pi), -math.sin(pi)], [0, math.sin(pi), math.cos(pi)]])
    Rv = Rx @ Ry
    luz = np.array([0.4, 0.8, -0.5])
    luz /= np.linalg.norm(luz)
    S = 7
    img = Image.new('RGB', (900, 560), (40, 38, 44))
    dr = ImageDraw.Draw(img)
    proy = []
    for pts, col, al in caras:
        P = [Rv @ p for p in pts]
        n = np.cross(P[1] - P[0], P[3] - P[0])
        if np.linalg.norm(n) < 1e-9 or al < 0.2:
            continue
        n /= np.linalg.norm(n)
        if n[2] > 0:
            n = -n
        k = 0.55 + 0.45 * max(0, float(np.dot(n, Rv @ luz)))
        z = sum(p[2] for p in P) / 4
        proy.append((z, [(450 + p[0] * S, 470 - p[1] * S) for p in P], tuple(int(v) for v in np.clip(col * k, 0, 255))))
    for z, poly, col in sorted(proy, key=lambda r: r[0], reverse=True):
        dr.polygon(poly, fill=col, outline=(20, 18, 16))
    dr.line((450 - 8 * S, 470, 450 + 8 * S, 470), fill=(200, 60, 60))
    img.save(salida)


def main():
    modelo()
    empacar()
    tex = textura()
    os.makedirs(os.path.join(A, 'geo'), exist_ok=True)
    os.makedirs(os.path.join(A, 'animations'), exist_ok=True)
    os.makedirs(os.path.join(A, 'textures', 'entity'), exist_ok=True)
    json.dump(geo(), open(os.path.join(A, 'geo', 'cascabel.geo.json'), 'w'), indent=1)
    json.dump(anims(), open(os.path.join(A, 'animations', 'cascabel.animation.json'), 'w'), indent=1)
    tex.save(os.path.join(A, 'textures', 'entity', 'cascabel.png'))
    print(len(HUESOS), 'huesos', len(CUBOS), 'cubos')
    if '--vista' in sys.argv:
        out = sys.argv[sys.argv.index('--vista') + 1]
        os.makedirs(out, exist_ok=True)
        vista(tex, os.path.join(out, 'cascabel_34.png'), 35, 15)
        vista(tex, os.path.join(out, 'cascabel_lado.png'), 90, 0)
        vista(tex, os.path.join(out, 'cascabel_frente.png'), 180 + 20, 10)
        vista(tex, os.path.join(out, 'cascabel_ruge.png'), 60, 10, {'body': (-14, 0, 0), 'head': (-20, 0, 0), 'jaw': (55, 0, 0),
                                                                    'arm_l': (-135, 0, -38), 'arm_r': (-135, 0, 38)})
        tex.resize((TW * 2, TH * 2), Image.NEAREST).save(os.path.join(out, 'cascabel_textura.png'))


if __name__ == '__main__':
    main()
