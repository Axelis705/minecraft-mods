#!/usr/bin/env python3
"""El Botones del hotel del Coleccionista: el modelo (.geo.json), sus animaciones y su textura, hechos a mano por código.

Un botones gigante de porcelana (casi tres bloques y medio): cabeza de muñeca antigua demasiado grande, con el pelo pintado, una
mejilla rota (por el hueco se asoma un ojo rojo que brilla), el otro ojo de vidrio azul, chapitas rosas y una sonrisa pintada; la
quijada es de muñeco de ventrílocuo (con las rayas de marioneta) y se abre de golpe cuando grita. Gorrito rojo de botones ladeado,
con barbiquejo. Saco rojo con doble fila de botones dorados, charreteras y su placa con el nombre; pantalón azul marino con raya
dorada, zapatos de charol. Brazos larguísimos (le llegan abajo de las rodillas) con guantes blancos y dedos largos.

Empuja un carrito de maletas de latón (cubierta con alfombra roja, maletas viejas, una sombrerera, la cabeza de una muñeca asomándose
de una maleta y un timbre de recepción en la barra). Cuando persigue a alguien suelta el carrito: el carrito sale aparte
(carrito.geo.json, una entidad que se queda donde lo dejó).

Uso: python3 herramientas/botones.py [--vista carpeta]
Escribe geo/botones.geo.json, geo/carrito.geo.json, animations/botones.animation.json, animations/carrito.animation.json y las
texturas textures/entity/botones.png y textures/entity/carrito.png.
"""
import os
import sys

import numpy as np
from PIL import Image

from geokit import Modelo, hexc, ruido, liso, manchas

R = np.random.RandomState(77)
m = Modelo('botones', 256, 256)

# ---------------------------------------------------------------- el modelo
CARRO_Z = -16     # la barra del carrito (donde agarra)
CARRO_Y = 16


def piernas():
    for lado, x0 in (('i', 1.5), ('d', -4.5)):
        cx = x0 + 1.5
        m.hueso('pierna_' + lado, 'root', (cx, 21, 0))
        m.cubo('pierna_' + lado, (x0, 11, -1.5), (3, 10, 3), 'pantalon')
        m.hueso('pierna_' + lado + '_baja', 'pierna_' + lado, (cx, 11, 0))
        m.cubo('pierna_' + lado + '_baja', (x0 + 0.1, 2, -1.4), (2.8, 9, 2.8), 'pantalon')
        m.cubo('pierna_' + lado + '_baja', (x0 - 0.4, 0, -4), (3.8, 2.2, 5.6), 'zapato')
        m.cubo('pierna_' + lado + '_baja', (x0 - 0.2, 1.6, -1.6), (3.4, 0.8, 3.2), 'calcetin')


def cuerpo():
    m.hueso('cuerpo', 'root', (0, 21, 0), (8, 0, 0))                    # encorvado hacia el carrito
    m.cubo('cuerpo', (-4.5, 20, -2.5), (9, 3, 5), 'pantalon')            # la cadera
    m.cubo('cuerpo', (-5.5, 22.5, -3.5), (11, 13, 7), 'saco')            # el saco
    m.cubo('cuerpo', (-5, 17.5, 2.6), (10, 6, 1.2), 'saco_faldon')       # los faldones de atrás
    for y in (24.6, 27.6, 30.6, 33.4):                                    # doble fila de botones dorados
        for x in (-2.7, 1.5):
            m.cubo('cuerpo', (x, y, -3.9), (1.2, 1.2, 0.5), 'boton')
    m.cubo('cuerpo', (-4, 34.5, -3.2), (8, 1.8, 6.4), 'saco_oscuro')     # el cuello del saco
    m.cubo('cuerpo', (5, 34, -2.5), (2.6, 1.4, 5), 'oro')                 # charreteras
    m.cubo('cuerpo', (-7.6, 34, -2.5), (2.6, 1.4, 5), 'oro')
    m.cubo('cuerpo', (-4.6, 30.8, -3.85), (2.6, 1, 0.4), 'placa')         # la placa con su nombre
    m.cubo('cuerpo', (-1.4, 35.5, -1.4), (2.8, 3.5, 2.8), 'porcelana')   # el cuello de porcelana


def cabeza():
    m.hueso('cabeza', 'cuerpo', (0, 38.5, 0), (-8, 0, 0))               # (endereza la cabeza: el cuerpo va encorvado)
    m.cubo('cabeza', (-6.5, 40.5, -6.5), (13, 10.5, 12), 'cara')
    m.cubo('cabeza', (-0.6, 43, -7.1), (1.2, 1.6, 0.7), 'nariz')
    m.cubo('cabeza', (6.4, 42.5, -1.5), (0.9, 3, 2.2), 'oreja')
    m.cubo('cabeza', (-7.3, 42.5, -1.5), (0.9, 3, 2.2), 'oreja')
    m.cubo('cabeza', (-6.75, 39, -2), (0.3, 12, 0.7), 'correa')          # el barbiquejo
    m.cubo('cabeza', (6.45, 39, -2), (0.3, 12, 0.7), 'correa')
    m.cubo('cabeza', (2.4, 42.7, -6.85), (3.2, 1.4, 0.45), 'astilla')    # un pedazo de porcelana a medio caer (bajo el hueco)
    m.hueso('mandibula', 'cabeza', (0, 40.5, 3))
    m.cubo('mandibula', (-4.5, 37.8, -6.3), (9, 2.8, 9), 'mandibula')
    m.hueso('ojos', 'cabeza', (0, 45, -6.6))
    m.cubo('ojos', (3.3, 45.0, -6.55), (1.5, 1.5, 0.2), 'ojo_rojo')      # el ojo que se asoma por el hueco (la cara va al revés en x)
    m.hueso('gorro', 'cabeza', (0, 51, 0), (0, 0, 11))
    m.cubo('gorro', (-3.5, 50.6, -3.5), (7, 3.8, 7), 'gorro')
    m.cubo('gorro', (-0.6, 54.4, -0.6), (1.2, 0.6, 1.2), 'oro')


def brazos():
    for lado, s in (('i', 1), ('d', -1)):
        px = 6.8 * s

        def X(x0, w):
            return x0 if s > 0 else -(x0 + w)
        b = 'brazo_' + lado
        m.hueso(b, 'cuerpo', (px, 34.5, 0), (-45, 0, 0))
        m.cubo(b, (X(5.6, 2.6), 24, -1.3), (2.6, 11, 2.6), 'saco')
        m.hueso(b + '_bajo', b, (6.9 * s, 24.5, 0), (-4, 0, 0))
        m.cubo(b + '_bajo', (X(5.75, 2.3), 14, -1.15), (2.3, 10.5, 2.3), 'saco')
        m.cubo(b + '_bajo', (X(5.55, 2.7), 13.5, -1.35), (2.7, 1.6, 2.7), 'puno')
        m.hueso('mano_' + lado, b + '_bajo', (6.9 * s, 13.6, 0))
        mano = 'mano_' + lado
        m.cubo(mano, (X(5.4, 3), 9.5, -1.7), (3, 4.2, 3.4), 'guante')
        for k, z in enumerate((-1.45, -0.45, 0.55)):                      # los dedos, largos
            largo = (4.4, 4.8, 4.2)[k]
            m.cubo(mano, (X(6.5, 0.8), 9.6 - largo, z), (0.8, largo, 0.8), 'guante')
        m.cubo(mano, (X(5.5, 0.8), 7.4, -2.2), (0.8, 2.6, 0.8), 'guante')   # el pulgar


def carrito():
    z0, z1 = CARRO_Z - 14.5, CARRO_Z + 0.6
    m.hueso('carrito', 'root', (0, 0, CARRO_Z))
    m.cubo('carrito', (-7, 2.2, z0), (14, 1.4, z1 - z0), 'cubierta')
    for k, (x, z) in enumerate(((-7.3, z0 + 0.6), (5.9, z0 + 0.6), (-7.3, z1 - 3.0), (5.9, z1 - 3.0))):
        r = 'rueda_%d' % (k + 1)
        m.hueso(r, 'carrito', (x + 0.7, 1.2, z + 1.2))
        m.cubo(r, (x, 0, z), (1.4, 2.4, 2.4), 'rueda')
    zb, zf = CARRO_Z - 0.55, z0 + 0.1
    for x in (-6.8, 5.7):
        m.cubo('carrito', (x, 3.6, zb - 0.55), (1.1, CARRO_Y - 3.6, 1.1), 'laton')     # postes de atrás (la barra)
        m.cubo('carrito', (x, 3.6, zf), (1.1, CARRO_Y - 3.6, 1.1), 'laton')           # postes de adelante
        m.cubo('carrito', (x, CARRO_Y, zf), (1.1, 1.1, zb - zf + 0.55), 'laton')       # rieles de arriba
    m.cubo('carrito', (-6.8, CARRO_Y, zb - 0.55), (13.6, 1.1, 1.1), 'laton')           # la barra de empujar
    m.cubo('carrito', (-6.8, CARRO_Y, zf), (13.6, 1.1, 1.1), 'laton')
    m.cubo('carrito', (-5.8, 3.6, zf + 1.2), (11.5, 5, 8), 'maleta_cafe')
    m.cubo('carrito', (-5, 8.6, zf + 2), (9.5, 4, 6.5), 'maleta_roja')
    m.cubo('carrito', (-3.4, 12.6, zf + 3.4), (5.6, 3.2, 5), 'sombrerera')
    m.cubo('carrito', (-5.4, 3.6, zb - 4.2), (4.4, 3.4, 3.2), 'maleta_verde')
    m.cubo('carrito', (2.3, 12.6, zf + 1.4), (2.8, 2.8, 2.8), 'muneca')                # una muñeca se asoma de la maleta
    m.cubo('carrito', (4.6, 6.4, zf - 0.2), (1.5, 2.1, 0.2), 'etiqueta')
    m.hueso('timbre', 'carrito', (0, CARRO_Y + 1.1, zb))
    m.cubo('timbre', (-1.6, CARRO_Y + 1.1, zb - 1.6), (3.2, 0.6, 3.2), 'laton_oscuro')
    m.cubo('timbre', (-1.2, CARRO_Y + 1.7, zb - 1.2), (2.4, 1.4, 2.4), 'campana')
    m.cubo('timbre', (-0.3, CARRO_Y + 3.1, zb - 0.3), (0.6, 0.6, 0.6), 'laton')


def modelo():
    m.hueso('root', None, (0, 0, 0))
    piernas()
    cuerpo()
    cabeza()
    brazos()
    carrito()


# ---------------------------------------------------------------- texturas
def lana(w, h, color, s, var=0.22):
    rgb = liso(w, h, color, s, var, 2)
    y, x = np.mgrid[0:h, 0:w]
    rgb = rgb * (1 - 0.06 * ((x + y) % 2))[..., None]        # tejido
    return manchas(rgb, s + 5, '#2a0a0a', 0.8, 0.25)


def latón(w, h, s, base='#b8913e'):
    rgb = liso(w, h, base, s, 0.35, 2)
    return manchas(rgb, s + 3, '#4a5a3a', 0.78, 0.35)       # verdín


def cara_porcelana(w, h, s):
    rgb = liso(w, h, '#ece4d4', s, 0.08, 4)
    # sombra suave hacia las orillas
    y, x = np.mgrid[0:h, 0:w]
    bx = np.minimum(x, w - 1 - x) / max(1, w / 2)
    rgb = rgb * (0.9 + 0.1 * np.clip(bx * 2, 0, 1))[..., None]
    return rgb


def grieta(rgb, x, y, pasos, semilla, color='#3a3028'):
    rs = np.random.RandomState(semilla)
    h, w = rgb.shape[:2]
    c = hexc(color)
    for _ in range(pasos):
        if 0 <= x < w and 0 <= y < h:
            rgb[y, x] = c
        x += rs.choice([-1, 0, 1, 1])
        y += rs.choice([0, 1, 1, -1])
    return rgb


def pelo_pintado(w, h, s):
    rgb = liso(w, h, '#2e2018', s, 0.25, 2)
    for xx in range(0, w, 2):
        rgb[:, xx] = rgb[:, xx] * 0.8                          # las rayas del peine
    return rgb


def pintor(mat, cara, w, h, s, c):
    a = np.full((h, w), 255, np.uint8)
    if mat == 'pantalon':
        rgb = lana(w, h, '#1e2338', s, 0.25)
        if cara in ('east', 'west') and w >= 3:
            rgb[:, w // 2] = hexc('#c9a040')                   # la raya dorada
    elif mat == 'zapato':
        rgb = liso(w, h, '#141012', s, 0.15)
        if cara == 'up':
            rgb[1:3, 1:w - 1] = hexc('#4a4448')                # el brillo del charol
        if cara in ('north', 'east', 'west', 'south'):
            rgb[-1, :] = hexc('#3a2418')                       # la suela
    elif mat == 'calcetin':
        rgb = liso(w, h, '#d8d0c0', s, 0.1)
    elif mat in ('saco', 'saco_faldon'):
        rgb = lana(w, h, '#8a1a1e', s)
        if cara == 'north' and mat == 'saco' and h > 8:
            rgb[:, w // 2] = rgb[:, w // 2] * 0.5               # la abertura del saco
            rgb[:, w // 2 - 1] = hexc('#c9a040') * 0.85         # vivo dorado
            rgb[-2:, :] = hexc('#c9a040') * 0.85                # la orilla de abajo
            # una mancha vieja (vino… o algo peor)
            rgb[h * 2 // 3:h * 2 // 3 + 2, 1:3] = hexc('#3a0808')
        if cara in ('east', 'west') and mat == 'saco' and h > 8:
            rgb[-2:, :] = hexc('#c9a040') * 0.85
    elif mat == 'saco_oscuro':
        rgb = lana(w, h, '#5c1014', s)
    elif mat == 'boton':
        rgb = liso(w, h, '#e0b44a', s, 0.2)
        if cara == 'north' and w >= 1:
            rgb[:, :] = hexc('#f0cc66')
    elif mat in ('oro', 'puno'):
        rgb = liso(w, h, '#c9a040', s, 0.3)
        y, x = np.mgrid[0:h, 0:w]
        rgb = rgb * (0.85 + 0.2 * ((x + y) % 2))[..., None]   # trenzado
    elif mat == 'placa':
        rgb = liso(w, h, '#d8b45a', s, 0.15)
        if cara == 'north' and w >= 3:
            rgb[0, 1:w - 1:2] = hexc('#3a2a10')
    elif mat == 'porcelana':
        rgb = cara_porcelana(w, h, s)
    elif mat == 'nariz':
        rgb = cara_porcelana(w, h, s)
        if cara == 'down':
            rgb[:, :] = hexc('#c8a898')
    elif mat == 'oreja':
        rgb = cara_porcelana(w, h, s)
        if cara in ('east', 'west') and h > 1:
            rgb[1:-1, 1:-1] = hexc('#e0c4b4')
    elif mat == 'correa':
        rgb = liso(w, h, '#141012', s, 0.2)
    elif mat == 'astilla':
        rgb = cara_porcelana(w, h, s)
        if cara == 'north':
            rgb[-1, :] = hexc('#8a7a68')
    elif mat == 'cara':
        rgb = cara_porcelana(w, h, s)
        if cara == 'up':
            rgb = pelo_pintado(w, h, s)
            rgb[:, w * 2 // 3] = hexc('#ece4d4') * 0.9          # la raya del peinado
        elif cara == 'down':
            rgb = liso(w, h, '#2a0606', s, 0.3)                 # por dentro de la boca
        elif cara == 'south':
            rgb[:h * 3 // 5] = pelo_pintado(w, h * 3 // 5, s)
            for k in range(2):
                rgb = grieta(rgb, w // 2 + k, 1, 10, s + k)
        elif cara in ('east', 'west'):
            rgb[:h // 3] = pelo_pintado(w, h // 3, s)
            # la patilla
            rgb[h // 3:h // 2, w // 3:w // 3 + 2] = hexc('#2e2018')
        elif cara == 'north':
            # (13 x 11) la cara pintada: x 0 a la izquierda viéndola de frente
            rgb[0:2] = pelo_pintado(w, 2, s)                    # el fleco
            rgb[0:3, w - 4:] = pelo_pintado(4, 3, s + 1)
            ce = hexc('#4a3426')
            rgb[3, 1:5] = ce                                     # cejas finitas, una más alta (cara de pregunta)
            rgb[2, 8:12] = ce
            # el ojo bueno (derecho al verla): de vidrio azul, muy abierto
            rgb[4:7, 8:12] = hexc('#f4f0e8')
            rgb[4:7, 9:11] = hexc('#3a6ab0')
            rgb[5, 9:11] = hexc('#0c0c14')
            rgb[4, 9] = hexc('#ffffff')
            rgb[4, 8:12] = rgb[4, 8:12] * 0.75                   # párpado
            # el otro: la mejilla rota, un hueco negro (adentro brilla el ojo rojo, que es otro cubo)
            hueco = [(3, 1), (3, 2), (3, 3), (4, 0), (4, 1), (4, 2), (4, 3), (4, 4), (5, 0), (5, 1), (5, 2), (5, 3), (5, 4),
                     (6, 1), (6, 2), (6, 3), (6, 4), (7, 2), (7, 3)]
            for yy, xx in hueco:
                rgb[yy, xx] = hexc('#0a0604')
                a[yy, xx] = 255
            rgb[3, 0] = rgb[7, 1] = rgb[2, 2] = hexc('#8a7a68')  # el borde roto
            rgb = grieta(rgb, 4, 7, 9, s + 11)
            rgb = grieta(rgb, 1, 7, 7, s + 13)
            rgb = grieta(rgb, 3, 2, 5, s + 17)
            # chapitas rosas
            rgb[7:9, 9:12] = rgb[7:9, 9:12] * 0.7 + hexc('#e28a8a') * 0.3
            rgb[8, 1:3] = rgb[8, 1:3] * 0.7 + hexc('#e28a8a') * 0.3
            # la sonrisa pintada, muy fina y demasiado larga
            rgb[9, 3:10] = hexc('#a01818')
            rgb[8, 2] = rgb[8, 10] = hexc('#a01818')
            # las rayas de marioneta (de las comisuras para abajo)
            rgb[10, 3] = rgb[10, 9] = hexc('#4a3a30')
    elif mat == 'mandibula':
        rgb = cara_porcelana(w, h, s)
        if cara == 'north':
            rgb[:, 0] = rgb[:, -1] = hexc('#4a3a30')             # las rayas de marioneta
            rgb[0, 1:-1] = hexc('#a01818') * 0.9                  # el labio de abajo
        elif cara == 'up':
            rgb = liso(w, h, '#3a0808', s, 0.3)                  # dentro de la boca
            rgb[:2, :] = hexc('#e8e0cc')                          # dientes pintados
            rgb[:2, ::2] = hexc('#b8ae98')
    elif mat == 'gorro':
        rgb = lana(w, h, '#9a1c22', s, 0.18)
        if cara in ('east', 'west', 'north', 'south') and h >= 3:
            rgb[h // 2] = hexc('#c9a040')                        # la banda dorada
            rgb[-1] = hexc('#5c1014')
        if cara == 'up' and w >= 5:
            rgb[1, 1:-1] = rgb[-2, 1:-1] = hexc('#c9a040') * 0.9
    elif mat == 'guante':
        rgb = liso(w, h, '#e8e4da', s, 0.12)
        rgb = manchas(rgb, s + 1, '#8a8478', 0.72, 0.3)          # mugre en los pliegues
        if s % 5 == 0 and cara == 'north' and w > 1 and h > 2:
            rgb[h // 2, w // 2] = hexc('#5a0a0a')                 # una gotita seca
    elif mat == 'ojo_rojo':
        rgb = np.broadcast_to(hexc('#ff2a18'), (h, w, 3)).copy()
        if w >= 2 and h >= 2:
            rgb[h // 2, w // 2] = hexc('#ffd24a')
    # --- el carrito
    elif mat == 'cubierta':
        rgb = latón(w, h, s)
        if cara == 'up':
            rgb = lana(w, h, '#7a1418', s)
            rgb[[0, -1], :] = hexc('#c9a040')
            rgb[:, [0, -1]] = hexc('#c9a040')
    elif mat == 'laton':
        rgb = latón(w, h, s)
    elif mat == 'laton_oscuro':
        rgb = latón(w, h, s, '#7a5e28')
    elif mat == 'rueda':
        rgb = liso(w, h, '#1a1614', s, 0.2)
        if cara in ('east', 'west') and w >= 2 and h >= 2:
            rgb[h // 2 - 1:h // 2 + 1, w // 2 - 1:w // 2 + 1] = hexc('#b8913e')
    elif mat in ('maleta_cafe', 'maleta_roja', 'maleta_verde'):
        base = {'maleta_cafe': '#6a4428', 'maleta_roja': '#7a2a24', 'maleta_verde': '#2e4a36'}[mat]
        rgb = liso(w, h, base, s, 0.3, 3)
        rgb = manchas(rgb, s + 2, '#2a1a10', 0.7, 0.3)
        if cara in ('north', 'south', 'east', 'west') and w >= 4:
            rgb[:, w // 3] = hexc('#2a1a10')                     # las correas
            rgb[:, 2 * w // 3] = hexc('#2a1a10')
            rgb[0, 0] = rgb[0, -1] = rgb[-1, 0] = rgb[-1, -1] = hexc('#c9a040')   # esquinas de latón
            if mat == 'maleta_roja' and h >= 3:
                rgb[1:3, 1:3] = hexc('#d8c890')                  # una calcomanía vieja de viaje
    elif mat == 'sombrerera':
        rgb = liso(w, h, '#d8ccb0', s, 0.15)
        y, x = np.mgrid[0:h, 0:w]
        rgb = np.where(((x // 2) % 2 == 0)[..., None] & (cara != 'up'), rgb * 0.6 + hexc('#3a7a7a') * 0.4, rgb)
    elif mat == 'muneca':
        rgb = cara_porcelana(w, h, s)
        if cara == 'up':
            rgb = liso(w, h, '#c8a050', s, 0.3)                   # rizos rubios
        if cara == 'north' and w >= 3 and h >= 3:
            rgb[0, :] = hexc('#c8a050')
            rgb[1, 0] = rgb[1, 2] = hexc('#0a0604')               # ojitos negros
            rgb[2, 1] = hexc('#a01818')
    elif mat == 'etiqueta':
        rgb = liso(w, h, '#d8ccaa', s, 0.1)
    elif mat == 'campana':
        rgb = liso(w, h, '#d0c8b8', s, 0.25)
        if cara == 'up':
            rgb = rgb * 1.1
    else:
        rgb = np.broadcast_to(hexc('#ff00ff'), (h, w, 3)).copy()
    return rgb, a


# ---------------------------------------------------------------- animaciones
T = 'q.anim_time'
BRAZO = -45   # los brazos estirados hasta la barra del carrito


def anims():
    a = {}
    # con el carrito, quieto: las manos en la barra, la cabeza se ladea despacio y de pronto da un tirón; los dedos tamborilean
    a['animation.botones.idle'] = {'loop': True, 'animation_length': 5.0, 'bones': {
        'cuerpo': {'position': [0, f'math.sin({T}*72)*0.2', 0]},
        'cabeza': {'rotation': [f'math.sin({T}*36)*3', f'math.sin({T}*24)*14',
                                f'({T} > 3.2 && {T} < 4.3) ? 24 : math.sin({T}*36)*5']},
        'mandibula': {'rotation': [f'({T} > 3.3 && {T} < 3.6) ? 6 : 0', 0, 0]},
        'mano_i': {'rotation': [f'math.max(0, math.sin({T}*900))*8', 0, 0]},
        'mano_d': {'rotation': [f'math.max(0, math.sin({T}*900 + 90))*8', 0, 0]},
    }}
    # empuja el carrito: pasos tiesos y largos, la cabeza mira a los lados a tirones, las ruedas giran
    w = 220
    ruedas = {f'rueda_{k}': {'rotation': [f'{T}*-540', 0, 0]} for k in range(1, 5)}
    a['animation.botones.walk'] = {'loop': True, 'animation_length': 1.6363, 'bones': dict({
        'cuerpo': {'position': [0, f'math.abs(math.sin({T}*{w}))*0.9', 0], 'rotation': [0, f'math.sin({T}*{w})*3', 0]},
        'pierna_i': {'rotation': [f'math.sin({T}*{w})*24', 0, 0]},
        'pierna_d': {'rotation': [f'-math.sin({T}*{w})*24', 0, 0]},
        'pierna_i_baja': {'rotation': [f'math.max(0, -math.sin({T}*{w}))*16', 0, 0]},
        'pierna_d_baja': {'rotation': [f'math.max(0, math.sin({T}*{w}))*16', 0, 0]},
        'cabeza': {'rotation': [0, f'(math.mod({T}, 1.6363) > 0.8) ? 32 : -6', f'math.sin({T}*{w})*4']},
        'carrito': {'position': [0, f'math.abs(math.sin({T}*{w*2}))*0.15', 0]},
        'timbre': {'rotation': [0, 0, f'math.sin({T}*{w*2})*2']},
    }, **ruedas)}
    # sin carrito: camina tieso con los brazos colgando hasta abajo de las rodillas
    a['animation.botones.caminar'] = {'loop': True, 'animation_length': 1.6363, 'bones': {
        'cuerpo': {'rotation': [2, f'math.sin({T}*{w})*3', 0], 'position': [0, f'math.abs(math.sin({T}*{w}))*0.9', 0]},
        'pierna_i': {'rotation': [f'math.sin({T}*{w})*24', 0, 0]},
        'pierna_d': {'rotation': [f'-math.sin({T}*{w})*24', 0, 0]},
        'pierna_i_baja': {'rotation': [f'math.max(0, -math.sin({T}*{w}))*16', 0, 0]},
        'pierna_d_baja': {'rotation': [f'math.max(0, math.sin({T}*{w}))*16', 0, 0]},
        'brazo_i': {'rotation': [f'{-BRAZO} - 4 - math.sin({T}*{w})*14', 0, 4]},
        'brazo_d': {'rotation': [f'{-BRAZO} - 4 + math.sin({T}*{w})*14', 0, -4]},
        'brazo_i_bajo': {'rotation': [-4, 0, 0]}, 'brazo_d_bajo': {'rotation': [-4, 0, 0]},
        'cabeza': {'rotation': [8, f'math.sin({T}*{w/2})*20', 0]},
    }}
    # quieto sin carrito: los brazos cuelgan y se mecen, la cabeza gira demasiado (casi para atrás) y regresa de golpe
    a['animation.botones.idle_solo'] = {'loop': True, 'animation_length': 4.0, 'bones': {
        'cuerpo': {'rotation': [0, 0, f'math.sin({T}*90)*1.5']},
        'brazo_i': {'rotation': [f'{-BRAZO} - 4 + math.sin({T}*90)*4', 0, 5]},
        'brazo_d': {'rotation': [f'{-BRAZO} - 4 - math.sin({T}*90)*4', 0, -5]},
        'cabeza': {'rotation': [8, f'({T} > 1.5 && {T} < 2.6) ? 150 : math.sin({T}*45)*10', 0]},
    }}
    # corre (persigue): se va de frente, largos trancos, los brazos larguísimos al frente, la boca abierta
    r = 520
    a['animation.botones.run'] = {'loop': True, 'animation_length': 0.6923, 'bones': {
        'cuerpo': {'rotation': [22, 0, f'math.sin({T}*{r})*5'], 'position': [0, f'math.abs(math.sin({T}*{r}))*1.6', 0]},
        'cabeza': {'rotation': [-18, 0, f'-math.sin({T}*{r})*8 + 10']},
        'mandibula': {'rotation': [f'22 + math.sin({T}*{r*2})*6', 0, 0]},
        'pierna_i': {'rotation': [f'math.sin({T}*{r})*52', 0, 0]},
        'pierna_d': {'rotation': [f'-math.sin({T}*{r})*52', 0, 0]},
        'pierna_i_baja': {'rotation': [f'math.max(0, -math.sin({T}*{r}))*40', 0, 0]},
        'pierna_d_baja': {'rotation': [f'math.max(0, math.sin({T}*{r}))*40', 0, 0]},
        'brazo_i': {'rotation': [f'{-BRAZO} - 110 + math.sin({T}*{r})*18', 0, 8]},
        'brazo_d': {'rotation': [f'{-BRAZO} - 110 - math.sin({T}*{r})*18', 0, -8]},
        'brazo_i_bajo': {'rotation': [-12, 0, 0]}, 'brazo_d_bajo': {'rotation': [-12, 0, 0]},
        'mano_i': {'rotation': [f'math.sin({T}*{r*2})*20', 0, 0]},
        'mano_d': {'rotation': [f'-math.sin({T}*{r*2})*20', 0, 0]},
        'gorro': {'rotation': [f'math.sin({T}*{r*2})*6', 0, 0]},
    }}
    # te vio: voltea de golpe, azota el timbre con la mano (¡ding!) y abre la quijada como si gritara
    a['animation.botones.campana'] = {'loop': False, 'animation_length': 1.6, 'bones': {
        'cabeza': {'rotation': {'0.0': [0, 0, 0], '0.08': [0, 0, -22], '0.5': [0, 0, -22], '0.6': [-10, 0, 8], '1.6': [0, 0, 0]}},
        'brazo_d': {'rotation': {'0.0': [0, 0, 0], '0.25': {'vector': [-40, 0, -24], 'easing': 'easeOutQuad'},
                                 '0.42': {'vector': [6, 0, 16], 'easing': 'easeInQuad'}, '0.7': [0, 0, 14], '1.6': [0, 0, 0]}},
        'brazo_d_bajo': {'rotation': {'0.0': [0, 0, 0], '0.25': [-40, 0, 0], '0.42': [0, 0, 0], '1.6': [0, 0, 0]}},
        'timbre': {'scale': {'0.0': [1, 1, 1], '0.42': [1, 1, 1], '0.46': [1.15, 0.7, 1.15], '0.55': [1, 1.1, 1], '0.65': [1, 1, 1]}},
        'mandibula': {'rotation': {'0.0': [0, 0, 0], '0.5': [0, 0, 0], '0.62': {'vector': [38, 0, 0], 'easing': 'easeOutBack'},
                                   '1.3': [34, 0, 0], '1.6': [0, 0, 0]}},
        'ojos': {'scale': {'0.0': [1, 1, 1], '0.6': [1.6, 1.6, 1], '1.3': [1.6, 1.6, 1], '1.6': [1, 1, 1]}},
        'cuerpo': {'rotation': {'0.0': [0, 0, 0], '0.6': [-6, 0, 0], '1.6': [0, 0, 0]}},
    }}
    # agarra: se lanza con los dos brazos, junta las manos de golpe y se echa para atrás
    a['animation.botones.atacar'] = {'loop': False, 'animation_length': 0.9, 'bones': {
        'cuerpo': {'rotation': {'0.0': [0, 0, 0], '0.2': [-8, 0, 0], '0.4': {'vector': [26, 0, 0], 'easing': 'easeInQuad'},
                                '0.9': [0, 0, 0]}},
        'brazo_i': {'rotation': {'0.0': [0, 0, 0], '0.2': [-80, 0, 34], '0.4': {'vector': [-55, 0, -6], 'easing': 'easeInQuad'},
                                 '0.9': [0, 0, 0]}},
        'brazo_d': {'rotation': {'0.0': [0, 0, 0], '0.2': [-80, 0, -34], '0.4': {'vector': [-55, 0, 6], 'easing': 'easeInQuad'},
                                 '0.9': [0, 0, 0]}},
        'mandibula': {'rotation': {'0.0': [0, 0, 0], '0.2': [30, 0, 0], '0.45': [4, 0, 0], '0.9': [0, 0, 0]}},
    }}
    # busca: se queda quieto y la cabeza da vueltas de muñeco (de más), olfateando
    a['animation.botones.buscar'] = {'loop': False, 'animation_length': 3.0, 'bones': {
        'cabeza': {'rotation': {'0.0': [0, 0, 0], '0.4': [6, 70, 0], '0.9': [6, 70, 12], '1.3': [6, -90, 0], '1.9': [6, -90, -14],
                                '2.4': [10, 175, 0], '2.7': [10, 175, 0], '3.0': [0, 0, 0]}},
        'mandibula': {'rotation': {'0.0': [0, 0, 0], '1.0': [8, 0, 0], '1.2': [0, 0, 0], '2.4': [10, 0, 0], '2.6': [0, 0, 0]}},
    }}
    return a


def anims_carrito():
    return {'animation.carrito.idle': {'loop': True, 'animation_length': 3.0, 'bones': {
        'timbre': {'rotation': [0, 0, f'math.sin({T}*120)*1']}}}}


# ---------------------------------------------------------------- todo
CARRO = ['carrito', 'rueda_1', 'rueda_2', 'rueda_3', 'rueda_4', 'timbre']


def main():
    modelo()
    m.empacar()
    tex = m.textura(pintor)
    m.guardar(pintor, anims(), glow=['ojos'], tex=tex, bounds=(5, 4.5, 1.8))
    # el carrito solo (cuando lo suelta): mismos cubos, su propia textura (la misma imagen)
    m.guardar(pintor, anims_carrito(), nombre='carrito', solo=CARRO, tex=tex, bounds=(3, 2, 0.5))
    print(len(m.huesos), 'huesos', len(m.cubos), 'cubos')
    if '--vista' in sys.argv:
        out = sys.argv[sys.argv.index('--vista') + 1]
        os.makedirs(out, exist_ok=True)
        m.vista(tex, os.path.join(out, 'botones_34.png'), 215, 12, escala=8, tam=(900, 600), centro=(450, 540))
        m.vista(tex, os.path.join(out, 'botones_lado.png'), 270, 4, escala=8, tam=(900, 600), centro=(520, 540))
        m.vista(tex, os.path.join(out, 'botones_cara.png'), 190, 4, escala=22, tam=(700, 600), centro=(350, 1240), ocultar=('carrito',))
        m.vista(tex, os.path.join(out, 'botones_corre.png'), 230, 8, escala=8, tam=(900, 600), centro=(450, 540), ocultar=('carrito',),
                extra={'cuerpo': (14, 0, 0), 'brazo_i': (-65, 0, 8), 'brazo_d': (-65, 0, -8), 'mandibula': (22, 0, 0),
                       'pierna_i': (40, 0, 0), 'pierna_d': (-40, 0, 0)})
        tex.resize((512, 512), Image.NEAREST).save(os.path.join(out, 'botones_textura.png'))


if __name__ == '__main__':
    main()
