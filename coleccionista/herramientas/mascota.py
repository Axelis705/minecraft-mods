#!/usr/bin/env python3
"""Cascabel, la mascota del Coleccionista: el modelo (.geo.json), sus animaciones (.animation.json) y su textura, hechos por código.

Una fiera disecada y vuelta a coser: el cuerpo de un tigre viejo (el pelo desteñido, con remiendos de otra piel y costuras de hilo
dorado), una máscara de porcelana agrietada como la de su dueño (con dos ojos de vidrio ámbar que brillan en la oscuridad), un collar de
cuero rojo con un cascabel de latón (lo oye quien se esconde) y una llave de cuerda en la espalda que gira sola: es un juguete al que le
dieron cuerda.

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
TW, TH = 256, 128
DZ = 10  # todo se corre hacia atrás: así el jugador (la cámara) queda a la altura del pecho y no del lomo

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
    hueso('body', 'root', (0, 20, 2))
    cubo('body', (-10, 14, -12), (20, 14, 32), 'piel')                 # el lomo, largo y bajo
    cubo('body', (-11, 13, -16), (22, 17, 12), 'pecho')                # el pecho y la joroba de los hombros
    cubo('body', (-8, 12, 4), (16, 4, 12), 'panza')                    # la panza que cuelga
    cubo('body', (-1, 28, -4), (2, 0.6, 24), 'costura')                # la costura del espinazo
    hueso('key', 'body', (0, 28, 6))
    cubo('key', (-1, 28, 5), (2, 4, 2), 'laton')                       # la llave de cuerda
    cubo('key', (-6, 32, 5.5), (12, 7, 1), 'llave')                    # su mango (dos orejas)
    hueso('neck', 'body', (0, 25, -14), (-10, 0, 0))
    cubo('neck', (-6, 21, -24), (12, 11, 12), 'piel')
    hueso('collar', 'neck', (0, 24, -19))
    cubo('collar', (-6.5, 21.5, -21), (13, 3, 6), 'cuero', inflar=0.6)
    hueso('bell', 'collar', (0, 21.5, -21))
    cubo('bell', (-2, 17.5, -23.5), (4, 4, 4), 'cascabel')
    hueso('head', 'neck', (0, 28, -24), (10, 0, 0))
    cubo('head', (-8.5, 22, -38), (17, 14, 14), 'cabeza')              # el cráneo
    cubo('head', (-8, 25, -39), (16, 11, 1), 'mascara')                # la máscara de porcelana
    cubo('head', (-5, 21.5, -44), (10, 5, 7), 'hocico')                # el hocico (arriba de la boca)
    cubo('head', (-3.8, 19.5, -43.5), (1, 2.2, 1), 'diente')           # colmillos de arriba
    cubo('head', (2.8, 19.5, -43.5), (1, 2.2, 1), 'diente')
    cubo('head', (-1, 25.2, -44.5), (2, 1.2, 0.6), 'nariz')
    hueso('eyes', 'head', (0, 30, -39))
    cubo('eyes', (-5.4, 29.2, -39.3), (3, 2, 0.4), 'ojo')
    cubo('eyes', (2.4, 29.2, -39.3), (3, 2, 0.4), 'ojo')
    hueso('jaw', 'head', (0, 23, -37))
    cubo('jaw', (-4.5, 19, -44), (9, 3, 7), 'quijada')
    cubo('jaw', (-3.5, 22, -43.5), (1, 1.4, 1), 'diente')
    cubo('jaw', (2.5, 22, -43.5), (1, 1.4, 1), 'diente')
    hueso('mouth', 'jaw', (0, 21, -43))                                # donde queda la presa (localizador)
    hueso('ear_l', 'head', (6.5, 36, -29), (0, 0, -20))
    cubo('ear_l', (5, 35.5, -30.5), (4, 5, 2), 'oreja')
    hueso('ear_r', 'head', (-6.5, 36, -29), (0, 0, 20))
    cubo('ear_r', (-9, 35.5, -30.5), (4, 5, 2), 'oreja_rota')
    # las patas: gruesas y cortas, de fiera
    for lado, x in (('l', 1), ('r', -1)):
        xi = 5 if x > 0 else -11
        hueso('front_' + lado, 'body', (8 * x, 18, -10))
        cubo('front_' + lado, (xi, 8, -13), (6, 11, 6), 'pata')
        hueso('front_' + lado + '_low', 'front_' + lado, (8 * x, 9, -10))
        cubo('front_' + lado + '_low', (xi + 0.5, 1.5, -12.5), (5, 8, 5), 'pata')
        cubo('front_' + lado + '_low', (xi, 0, -14.5), (6, 3, 7), 'garra')
        for k in range(3):
            cubo('front_' + lado + '_low', (xi + 0.6 + 1.9 * k, 0, -15.6), (1, 1.4, 1.3), 'una')
        hueso('back_' + lado, 'body', (8 * x, 19, 14))
        cubo('back_' + lado, (xi - 0.5, 8, 9), (7, 13, 9), 'pierna' if lado == 'l' else 'remiendo')
        hueso('back_' + lado + '_low', 'back_' + lado, (8 * x, 9, 15))
        cubo('back_' + lado + '_low', (xi + 0.5, 1.5, 13), (5, 8, 5), 'pata')
        cubo('back_' + lado + '_low', (xi, 0, 11.5), (6, 3, 7), 'garra')
        for k in range(3):
            cubo('back_' + lado + '_low', (xi + 0.6 + 1.9 * k, 0, 10.4), (1, 1.4, 1.3), 'una')
    # la cola en tres pedazos: cae y se curva hacia arriba al final
    hueso('tail1', 'body', (0, 25, 19), (-30, 0, 0))
    cubo('tail1', (-2, 23, 19), (4, 4, 10), 'cola')
    hueso('tail2', 'tail1', (0, 25, 29), (14, 0, 0))
    cubo('tail2', (-1.5, 23.5, 29), (3, 3, 10), 'cola')
    hueso('tail3', 'tail2', (0, 25, 39), (22, 0, 0))
    cubo('tail3', (-1.2, 23.8, 39), (2.4, 2.4, 8), 'cola')
    cubo('tail3', (-1.8, 23.2, 46), (3.6, 3.6, 4), 'mechon')


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


def pintar_cara(img, x, y, w, h, mat, cara, s):
    """Pinta una cara del cubo (x, y = esquina en la textura; w, h = tamaño en texeles)."""
    if w <= 0 or h <= 0:
        return
    if mat in ('piel', 'pecho', 'cabeza', 'pata', 'pierna', 'cola'):
        claro = '#c9ae86' if mat in ('pecho',) and cara in ('north', 'down') else None
        rgb = pelaje(w, h, s, claro=claro, rayas=mat != 'pata' or cara != 'down')
        if mat == 'pata':
            rgb = pelaje(w, h, s, base='#8c6230', rayas=True)
    elif mat == 'panza':
        rgb = pelaje(w, h, s, base='#c2a77e', rayas=False)
    elif mat == 'remiendo':
        # una pierna de otro animal: gris oscuro con manchas, cosida al cuerpo
        rgb = pelaje(w, h, s, base='#5b5650', raya='#2c2a28', rayas=False)
        manch = ruido(w, h, 3, s + 3) > 0.62
        rgb = np.where(manch[..., None], hexc('#2c2a28'), rgb)
    elif mat == 'mechon':
        rgb = pelaje(w, h, s, base='#2a1d14', rayas=False)
    elif mat == 'costura':
        rgb = np.broadcast_to(hexc('#6a4a2a'), (h, w, 3)).copy()
    elif mat == 'cuero':
        rgb = hexc('#6a1418') * (0.8 + 0.3 * ruido(w, h, 2, s))[..., None]
    elif mat == 'cascabel':
        rgb = hexc('#c9a240') * (0.75 + 0.4 * ruido(w, h, 2, s))[..., None]
    elif mat in ('laton', 'llave'):
        rgb = hexc('#b08a3a') * (0.75 + 0.35 * ruido(w, h, 2, s))[..., None]
    elif mat == 'mascara':
        rgb = porcelana(w, h, s)
    elif mat in ('hocico', 'quijada'):
        rgb = pelaje(w, h, s, base='#b28a5a', rayas=False)
        if (mat == 'hocico' and cara == 'down') or (mat == 'quijada' and cara == 'up'):
            rgb = np.broadcast_to(hexc('#3a0c0c'), (h, w, 3)).copy()
    elif mat in ('diente', 'una'):
        rgb = np.broadcast_to(hexc('#e6dcc2'), (h, w, 3)).copy()
    elif mat == 'nariz':
        rgb = np.broadcast_to(hexc('#1a1210'), (h, w, 3)).copy()
    elif mat == 'ojo':
        rgb = np.broadcast_to(hexc('#ffb020'), (h, w, 3)).copy()
    elif mat in ('oreja', 'oreja_rota'):
        rgb = pelaje(w, h, s, rayas=False)
        if cara == 'north':
            rgb = hexc('#7a4e40') * (0.85 + 0.2 * ruido(w, h, 2, s))[..., None]
    elif mat == 'garra':
        rgb = pelaje(w, h, s, base='#7a5428', rayas=False)
    else:
        rgb = np.broadcast_to(hexc('#ff00ff'), (h, w, 3)).copy()
    a = np.full((h, w), 255, np.uint8)
    # detalles por material y cara
    if mat == 'mascara' and cara == 'north':
        # dos huecos negros para los ojos, una grieta y una lágrima pintada
        for ex in (int(w * 0.14), int(w * 0.62)):
            rgb[int(h * 0.34):int(h * 0.62), ex:ex + max(2, int(w * 0.24))] = hexc('#120c0a')
        yy = int(h * 0.05)
        xx = int(w * 0.68)
        for i in range(int(h * 0.9)):
            xx += R.randint(-1, 2)
            if 0 <= xx < w and yy + i < h:
                rgb[yy + i, xx] = rgb[yy + i, xx] * 0.45
        rgb[int(h * 0.62):int(h * 0.9), int(w * 0.2)] = hexc('#3a2a5a')
        rgb[int(h * 0.75):int(h * 0.8), int(w * 0.4):int(w * 0.6)] = hexc('#7a2a2a')   # boca pintada
    if mat == 'llave' and cara in ('north', 'south'):
        # el mango de la llave: dos aros (con hueco)
        yy, xx = np.mgrid[0:h, 0:w]
        for cx in (w * 0.27, w * 0.73):
            d = np.hypot((xx + 0.5 - cx) / (w * 0.22), (yy + 0.5 - h * 0.5) / (h * 0.42))
            a[d < 0.55] = 0
        a[:, int(w * 0.46):int(w * 0.54)] = 255
    if mat == 'cascabel' and cara in ('north', 'south', 'east', 'west'):
        rgb[int(h * 0.55):int(h * 0.65), :] = hexc('#2a1e0a')
        rgb[int(h * 0.65):, int(w * 0.45):int(w * 0.55)] = hexc('#2a1e0a')
    if mat == 'cuero' and cara in ('north', 'south', 'east', 'west'):
        for x0 in range(1, w, 4):
            rgb[max(0, h // 2 - 1):h // 2 + 1, x0:x0 + 2] = hexc('#d8b04a')
    if mat in ('piel', 'pecho', 'cabeza', 'pierna', 'remiendo') and cara in ('east', 'west', 'up'):
        # costuras de hilo dorado (puntadas en cruz) donde se cosieron los pedazos
        rs = np.random.RandomState(s + 99)
        for _ in range(1 if w * h < 300 else 2):
            vertical = rs.rand() < 0.5
            pos = rs.randint(2, (w if vertical else h) - 2) if (w if vertical else h) > 4 else 1
            largo = w if not vertical else h
            for i in range(0, largo - 1, 3):
                for d in (0, 1):
                    if vertical:
                        yy, xx = i + d, pos + (d if i % 6 == 0 else -d)
                    else:
                        yy, xx = pos + (d if i % 6 == 0 else -d), i + d
                    if 0 <= yy < h and 0 <= xx < w:
                        rgb[yy, xx] = hexc('#e2cc8a')
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
    # quieta: respira, mira alrededor, mueve la cola, parpadea una oreja; la llave de cuerda gira sola
    a['animation.cascabel.idle'] = {'loop': True, 'animation_length': 4.0, 'bones': {
        'body': {'position': [0, f'math.sin({T}*90)*0.35', 0]},
        'neck': {'rotation': [f'math.sin({T}*90)*2', 0, 0]},
        'head': {'rotation': [f'math.sin({T}*45)*3', f'math.sin({T}*30)*12', f'math.sin({T}*22)*3']},
        'jaw': {'rotation': [f'math.max(0, math.sin({T}*90 - 60))*5', 0, 0]},
        'tail1': {'rotation': [0, f'math.sin({T}*120)*14', 0]},
        'tail2': {'rotation': [f'math.sin({T}*60)*6', f'math.sin({T}*120 - 40)*16', 0]},
        'tail3': {'rotation': [0, f'math.sin({T}*120 - 80)*20', 0]},
        'key': {'rotation': [0, f'{T}*90', 0]},
        'ear_l': {'rotation': [0, 0, {'0.0': [0], '2.6': [0], '2.75': [-18], '2.9': [0]}]},
        'bell': {'rotation': [f'math.sin({T}*90)*4', 0, 0]},
    }}
    # camina: las patas en cruz, el lomo sube y baja, la cabeza baja, el cascabel suena
    w = 300
    a['animation.cascabel.walk'] = {'loop': True, 'animation_length': 1.2, 'bones': {
        'body': {'position': [0, f'math.abs(math.sin({T}*{w}))*1.0', 0], 'rotation': [0, 0, f'math.sin({T}*{w})*2.5']},
        'neck': {'rotation': [f'6 + math.sin({T}*{w*2})*2.5', 0, 0]},
        'head': {'rotation': [f'-4 + math.sin({T}*{w*2} + 60)*3', f'math.sin({T}*{w})*4', 0]},
        'front_l': {'rotation': [f'math.sin({T}*{w})*26', 0, 0]},
        'front_r': {'rotation': [f'-math.sin({T}*{w})*26', 0, 0]},
        'front_l_low': {'rotation': [f'math.max(0, -math.sin({T}*{w} + 70))*-32', 0, 0]},
        'front_r_low': {'rotation': [f'math.max(0, math.sin({T}*{w} + 70))*-32', 0, 0]},
        'back_l': {'rotation': [f'-math.sin({T}*{w})*24', 0, 0]},
        'back_r': {'rotation': [f'math.sin({T}*{w})*24', 0, 0]},
        'back_l_low': {'rotation': [f'math.max(0, math.sin({T}*{w} + 70))*30', 0, 0]},
        'back_r_low': {'rotation': [f'math.max(0, -math.sin({T}*{w} + 70))*30', 0, 0]},
        'tail1': {'rotation': [0, f'math.sin({T}*{w})*16', 0]},
        'tail2': {'rotation': [0, f'math.sin({T}*{w} - 50)*20', 0]},
        'tail3': {'rotation': [0, f'math.sin({T}*{w} - 100)*24', 0]},
        'key': {'rotation': [0, f'{T}*180', 0]},
        'bell': {'rotation': [f'math.sin({T}*{w*2})*24', 0, f'math.sin({T}*{w})*10']},
    }, 'sound_effects': {'0.0': {'effect': 'coleccionista:cascabel'}, '0.6': {'effect': 'coleccionista:cascabel'}}}
    # corre: galope (las de adelante juntas y las de atrás juntas), el lomo se mece
    r = 600
    a['animation.cascabel.run'] = {'loop': True, 'animation_length': 0.6, 'bones': {
        'body': {'position': [0, f'math.abs(math.sin({T}*{r}))*2.2', 0], 'rotation': [f'math.sin({T}*{r})*6', 0, 0]},
        'neck': {'rotation': [f'10 + math.sin({T}*{r} + 90)*5', 0, 0]},
        'head': {'rotation': [f'-8 + math.sin({T}*{r} + 140)*5', 0, 0]},
        'front_l': {'rotation': [f'math.sin({T}*{r})*48', 0, 0]},
        'front_r': {'rotation': [f'math.sin({T}*{r} + 25)*48', 0, 0]},
        'front_l_low': {'rotation': [f'math.max(0, -math.sin({T}*{r} + 80))*-50', 0, 0]},
        'front_r_low': {'rotation': [f'math.max(0, -math.sin({T}*{r} + 105))*-50', 0, 0]},
        'back_l': {'rotation': [f'math.sin({T}*{r} + 180)*44', 0, 0]},
        'back_r': {'rotation': [f'math.sin({T}*{r} + 205)*44', 0, 0]},
        'back_l_low': {'rotation': [f'math.max(0, math.sin({T}*{r} + 260))*46', 0, 0]},
        'back_r_low': {'rotation': [f'math.max(0, math.sin({T}*{r} + 285))*46', 0, 0]},
        'tail1': {'rotation': [-20, f'math.sin({T}*{r})*8', 0]},
        'tail2': {'rotation': [10, f'math.sin({T}*{r} - 50)*10', 0]},
        'key': {'rotation': [0, f'{T}*360', 0]},
        'bell': {'rotation': [f'math.sin({T}*{r*2})*38', 0, f'math.sin({T}*{r})*16']},
        'ear_l': {'rotation': [-25, 0, 0]}, 'ear_r': {'rotation': [-25, 0, 0]},
    }, 'sound_effects': {'0.0': {'effect': 'coleccionista:cascabel'}, '0.3': {'effect': 'coleccionista:cascabel'}}}
    # olfatea: la cabeza baja al piso y huele en golpecitos, de un lado a otro
    a['animation.cascabel.olfatear'] = {'loop': False, 'animation_length': 2.4, 'bones': {
        'neck': {'rotation': {'0.0': [0, 0, 0], '0.35': {'vector': [32, 0, 0], 'easing': 'easeOutQuad'},
                              '2.0': {'vector': [32, 0, 0]}, '2.4': {'vector': [0, 0, 0], 'easing': 'easeInOutSine'}}},
        'head': {'rotation': {'0.0': [0, 0, 0], '0.35': [18, 0, 0],
                              '0.6': [22, -14, 0], '0.75': [18, -12, 0], '0.9': [22, -10, 0],
                              '1.2': [20, 12, 0], '1.35': [24, 14, 0], '1.5': [20, 10, 0],
                              '1.8': [22, 0, 0], '2.0': [18, 0, 0], '2.4': [0, 0, 0]}},
        'jaw': {'rotation': [f'math.max(0, math.sin({T}*1400))*4', 0, 0]},
        'ear_l': {'rotation': [-15, 0, 0]}, 'ear_r': {'rotation': [-15, 0, 0]},
        'tail1': {'rotation': [-10, f'math.sin({T}*400)*20', 0]},
        'body': {'position': [0, -1.2, 0]},
    }, 'sound_effects': {'0.5': {'effect': 'coleccionista:olfatea'}, '1.2': {'effect': 'coleccionista:olfatea'}}}
    # ruge: se para en las patas de atrás, abre el hocico, tiembla, y vuelve
    a['animation.cascabel.rugir'] = {'loop': False, 'animation_length': 2.2, 'bones': {
        'body': {'rotation': {'0.0': [0, 0, 0], '0.4': {'vector': [-14, 0, 0], 'easing': 'easeOutBack'}, '1.6': [-12, 0, 0],
                              '2.2': {'vector': [0, 0, 0], 'easing': 'easeInOutSine'}},
                 'position': {'0.0': [0, 0, 0], '0.4': [0, 2, 2], '1.6': [0, 2, 2], '2.2': [0, 0, 0]}},
        'neck': {'rotation': {'0.0': [0, 0, 0], '0.4': [-28, 0, 0], '0.7': [-10, 0, 0], '1.6': [-8, 0, 0], '2.2': [0, 0, 0]}},
        'head': {'rotation': {'0.0': [0, 0, 0], '0.4': [-20, 0, 0], '0.7': [6, 0, 0], '1.6': [4, 0, 0], '2.2': [0, 0, 0]},
                 'position': [f'math.sin({T}*2400)*(({T} > 0.6 && {T} < 1.6) ? 0.4 : 0)', 0, 0]},
        'jaw': {'rotation': {'0.0': [0, 0, 0], '0.45': {'vector': [48, 0, 0], 'easing': 'easeOutQuad'}, '1.55': [44, 0, 0],
                             '1.9': {'vector': [0, 0, 0], 'easing': 'easeInQuad'}}},
        'front_l': {'rotation': {'0.0': [0, 0, 0], '0.4': [-30, 0, 0], '1.6': [-26, 0, 0], '2.2': [0, 0, 0]}},
        'front_r': {'rotation': {'0.0': [0, 0, 0], '0.4': [-22, 0, 0], '1.6': [-20, 0, 0], '2.2': [0, 0, 0]}},
        'ear_l': {'rotation': [-35, 0, 0]}, 'ear_r': {'rotation': [-35, 0, 0]},
        'tail1': {'rotation': [-30, f'math.sin({T}*900)*12', 0]},
        'bell': {'rotation': [f'math.sin({T}*1800)*30', 0, 0]},
        'eyes': {'scale': {'0.0': [1, 1, 1], '0.4': [1.3, 1.3, 1], '1.6': [1.3, 1.3, 1], '2.2': [1, 1, 1]}},
    }, 'sound_effects': {'0.35': {'effect': 'coleccionista:rugido'}}}
    # come: se lanza, abre, muerde y sacude la cabeza, levanta la cabeza para tragar y vuelve
    a['animation.cascabel.comer'] = {'loop': False, 'animation_length': 2.6, 'bones': {
        'body': {'position': {'0.0': [0, 0, 0], '0.3': {'vector': [0, -1, -6], 'easing': 'easeOutQuad'}, '1.4': [0, -1, -4],
                              '2.6': {'vector': [0, 0, 0], 'easing': 'easeInOutSine'}},
                 'rotation': {'0.0': [0, 0, 0], '0.3': [6, 0, 0], '1.4': [4, 0, 0], '1.8': [-8, 0, 0], '2.6': [0, 0, 0]}},
        'neck': {'rotation': {'0.0': [0, 0, 0], '0.3': {'vector': [24, 0, 0], 'easing': 'easeOutQuad'},
                              '0.55': [18, -14, 0], '0.75': [18, 14, 0], '0.95': [18, -12, 0], '1.15': [18, 12, 0],
                              '1.4': [10, 0, 0], '1.8': {'vector': [-34, 0, 0], 'easing': 'easeOutQuad'}, '2.2': [-30, 0, 0],
                              '2.6': {'vector': [0, 0, 0], 'easing': 'easeInOutSine'}}},
        'head': {'rotation': {'0.0': [0, 0, 0], '0.3': [10, 0, 0], '0.55': [8, 0, -12], '0.75': [8, 0, 12], '0.95': [8, 0, -10],
                              '1.15': [8, 0, 10], '1.4': [0, 0, 0], '1.8': [-24, 0, 0], '2.2': [-22, 0, 0], '2.6': [0, 0, 0]}},
        'jaw': {'rotation': {'0.0': [0, 0, 0], '0.25': {'vector': [52, 0, 0], 'easing': 'easeOutQuad'},
                             '0.4': {'vector': [2, 0, 0], 'easing': 'easeInQuad'}, '1.6': [4, 0, 0], '1.85': [22, 0, 0],
                             '2.0': [0, 0, 0], '2.6': [0, 0, 0]}},
        'neck_gulp': {},
        'front_l': {'rotation': {'0.0': [0, 0, 0], '0.3': [-34, 0, 0], '0.5': [10, 0, 0], '2.6': [0, 0, 0]}},
        'front_r': {'rotation': {'0.0': [0, 0, 0], '0.3': [-30, 0, 0], '0.5': [8, 0, 0], '2.6': [0, 0, 0]}},
        'collar': {'scale': {'0.0': [1, 1, 1], '1.85': [1, 1, 1], '2.0': [1.15, 1.25, 1.1], '2.25': [1, 1, 1]}},
        'tail1': {'rotation': [-15, f'math.sin({T}*700)*18', 0]},
        'bell': {'rotation': [f'math.sin({T}*1600)*30', 0, 0]},
    }, 'sound_effects': {'0.2': {'effect': 'coleccionista:rugido_corto'}, '0.4': {'effect': 'coleccionista:mordida'},
                         '1.9': {'effect': 'coleccionista:trago'}}}
    del a['animation.cascabel.comer']['bones']['neck_gulp']
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
        vista(tex, os.path.join(out, 'cascabel_ruge.png'), 60, 10, {'body': (-14, 0, 0), 'neck': (-28, 0, 0), 'head': (-20, 0, 0),
                                                                    'jaw': (48, 0, 0), 'front_l': (-30, 0, 0), 'front_r': (-22, 0, 0)})
        tex.resize((TW * 2, TH * 2), Image.NEAREST).save(os.path.join(out, 'cascabel_textura.png'))


if __name__ == '__main__':
    main()
