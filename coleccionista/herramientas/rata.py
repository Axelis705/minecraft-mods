#!/usr/bin/env python3
"""La rata de cuerda del hotel: un ratón de juguete viejo, de hojalata pintada (gris, con el pelo litografiado, remaches en el lomo y
óxido), ojitos de cuenta roja que brillan, orejas rosas, bigotes de alambre, dientes, cola de hule en tres pedazos y la llave de
cuerda en la espalda (que gira más rápido cuando corre). Al morir se voltea, le salta el resorte de la espalda y la llave se detiene.

Uso: python3 herramientas/rata.py [--vista carpeta]
Escribe geo/rata.geo.json, animations/rata.animation.json y textures/entity/rata.png.
"""
import os
import sys

import numpy as np
from PIL import Image

from geokit import Modelo, hexc, ruido, liso, manchas

m = Modelo('rata', 64, 64)


def modelo():
    m.hueso('root', None, (0, 0, 0))
    m.hueso('cuerpo', 'root', (0, 2.5, 0))
    m.cubo('cuerpo', (-2, 1, -3.5), (4, 3.2, 7.5), 'hojalata')
    m.cubo('cuerpo', (-1.6, 4.2, -2.8), (3.2, 0.8, 5.8), 'lomo')
    m.hueso('cabeza', 'cuerpo', (0, 3, -3.5))
    m.cubo('cabeza', (-1.5, 1.3, -7), (3, 2.6, 3.5), 'hojalata')
    m.cubo('cabeza', (-1, 1.5, -8.2), (2, 1.8, 1.3), 'hojalata')
    m.cubo('cabeza', (-0.5, 2.3, -8.7), (1, 0.9, 0.6), 'nariz')
    m.cubo('cabeza', (0.9, 3.6, -5.2), (1.8, 1.8, 0.4), 'oreja')
    m.cubo('cabeza', (-2.7, 3.6, -5.2), (1.8, 1.8, 0.4), 'oreja')
    m.cubo('cabeza', (1, 2.4, -8), (2.6, 0.15, 0.15), 'alambre')     # bigotes
    m.cubo('cabeza', (-3.6, 2.4, -8), (2.6, 0.15, 0.15), 'alambre')
    m.cubo('cabeza', (1, 2.0, -7.8), (2.2, 0.15, 0.15), 'alambre')
    m.cubo('cabeza', (-3.2, 2.0, -7.8), (2.2, 0.15, 0.15), 'alambre')
    m.hueso('quijada', 'cabeza', (0, 1.5, -5))
    m.cubo('quijada', (-1.1, 0.8, -8.1), (2.2, 0.6, 3), 'hojalata')
    m.cubo('quijada', (-0.5, 1.35, -8.25), (1, 0.5, 0.3), 'diente')
    m.hueso('ojos', 'cabeza', (0, 3.2, -6.6))
    m.cubo('ojos', (1.05, 2.9, -6.6), (0.5, 0.7, 0.7), 'ojo')
    m.cubo('ojos', (-1.55, 2.9, -6.6), (0.5, 0.7, 0.7), 'ojo')
    m.hueso('llave', 'cuerpo', (0, 4.5, 0.5))
    m.cubo('llave', (-0.3, 4.8, 0.2), (0.6, 1.4, 0.6), 'laton')
    m.cubo('llave', (-1.6, 6.2, 0.35), (3.2, 1.8, 0.3), 'llave')
    m.hueso('resorte', 'cuerpo', (0, 4.6, 2))
    m.cubo('resorte', (-0.6, 4.6, 1.5), (1.2, 3, 1.2), 'resorte')
    m.hueso('cola_1', 'cuerpo', (0, 2.4, 4))
    m.cubo('cola_1', (-0.35, 2.05, 4), (0.7, 0.7, 3), 'cola')
    m.hueso('cola_2', 'cola_1', (0, 2.4, 7))
    m.cubo('cola_2', (-0.3, 2.1, 7), (0.6, 0.6, 3), 'cola')
    m.hueso('cola_3', 'cola_2', (0, 2.4, 10))
    m.cubo('cola_3', (-0.25, 2.15, 10), (0.5, 0.5, 2.5), 'cola')
    for nombre, x, z in (('pata_di', 0.8, -2.7), ('pata_dd', -1.8, -2.7), ('pata_ti', 0.8, 1.7), ('pata_td', -1.8, 1.7)):
        m.hueso(nombre, 'cuerpo', (x + 0.5, 1.3, z + 0.5))
        m.cubo(nombre, (x, 0, z), (1, 1.5, 1), 'pata')


def pintor(mat, cara, w, h, s, c):
    a = np.full((h, w), 255, np.uint8)
    if mat in ('hojalata', 'lomo'):
        rgb = liso(w, h, '#7a7876', s, 0.2, 2)
        y, x = np.mgrid[0:h, 0:w]
        rgb = np.where((((x + y * 2) % 3) == 0)[..., None], rgb * 0.82, rgb)     # pelo litografiado
        rgb = manchas(rgb, s + 1, '#8a4a22', 0.8, 0.45, 2)                         # óxido
        rgb = manchas(rgb, s + 2, '#c8c8c4', 0.86, 0.5, 1)                         # pintura saltada
        if cara == 'down':
            rgb = liso(w, h, '#d8d0c4', s, 0.15)                                   # la panza clara
        if mat == 'lomo' and cara == 'up':
            rgb[:, w // 2] = hexc('#a8a6a2')                                       # la costura con remaches
            rgb[::2, w // 2] = hexc('#e0dcd4')
    elif mat == 'nariz':
        rgb = liso(w, h, '#e88a9a', s, 0.15)
    elif mat == 'oreja':
        rgb = liso(w, h, '#6a6866', s, 0.2)
        if cara == 'north':
            rgb[:, :] = hexc('#e8a0aa')
    elif mat == 'alambre':
        rgb = liso(w, h, '#2a2a2a', s, 0.1)
    elif mat == 'diente':
        rgb = liso(w, h, '#e8dcb0', s, 0.1)
    elif mat == 'ojo':
        rgb = np.broadcast_to(hexc('#ff2418'), (h, w, 3)).copy()
    elif mat == 'laton':
        rgb = liso(w, h, '#b8913e', s, 0.3)
    elif mat == 'llave':
        rgb = liso(w, h, '#c9a040', s, 0.25)
        if cara in ('north', 'south') and w >= 3 and h >= 2:
            yy, xx = np.mgrid[0:h, 0:w]
            for cx in (w * 0.27, w * 0.73):
                d = np.hypot((xx + 0.5 - cx) / (w * 0.22), (yy + 0.5 - h * 0.5) / (h * 0.42))
                a[d < 0.5] = 0
    elif mat == 'resorte':
        rgb = liso(w, h, '#b0b4b8', s, 0.2)
        rgb[::2] = rgb[::2] * 0.55
    elif mat == 'cola':
        rgb = liso(w, h, '#b8888a', s, 0.2)
        rgb[:, ::2] = rgb[:, ::2] * 0.85
    elif mat == 'pata':
        rgb = liso(w, h, '#3a3836', s, 0.2)
    else:
        rgb = np.broadcast_to(hexc('#ff00ff'), (h, w, 3)).copy()
    return rgb, a


T = 'q.anim_time'
PATAS = ('pata_di', 'pata_dd', 'pata_ti', 'pata_td')


def anims():
    a = {}
    # quieta: olfatea a tironcitos, mueve la cola, la llave gira despacio
    a['animation.rata.idle'] = {'loop': True, 'animation_length': 2.0, 'bones': {
        'cabeza': {'rotation': [f'math.max(0, math.sin({T}*1440))*6 - 4', f'math.sin({T}*90)*14', 0]},
        'cola_1': {'rotation': [0, f'math.sin({T}*180)*14', 0]},
        'cola_2': {'rotation': [0, f'math.sin({T}*180 - 60)*18', 0]},
        'cola_3': {'rotation': [0, f'math.sin({T}*180 - 120)*22', 0]},
        'llave': {'rotation': [0, f'{T}*90', 0]},
        'resorte': {'scale': [0, 0, 0]},
    }}
    # corre: las patitas como motorcito, el cuerpo vibra, la llave gira rápido, la cola latiguea
    for nombre, vel, largo in (('walk', 1800, 0.4), ('run', 3600, 0.2)):
        b = {
            'cuerpo': {'position': [0, f'math.abs(math.sin({T}*{vel}))*0.25', 0], 'rotation': [0, 0, f'math.sin({T}*{vel})*3']},
            'cabeza': {'rotation': [f'math.sin({T}*{vel*2})*3', 0, 0]},
            'cola_1': {'rotation': [8, f'math.sin({T}*{vel/2})*24', 0]},
            'cola_2': {'rotation': [6, f'math.sin({T}*{vel/2} - 60)*30', 0]},
            'cola_3': {'rotation': [4, f'math.sin({T}*{vel/2} - 120)*34', 0]},
            'llave': {'rotation': [0, f'{T}*{vel/2}', 0]},
            'resorte': {'scale': [0, 0, 0]},
        }
        for k, p in enumerate(PATAS):
            fase = 0 if k in (0, 3) else 180
            b[p] = {'rotation': [f'math.sin({T}*{vel} + {fase})*55', 0, 0]}
        a['animation.rata.' + nombre] = {'loop': True, 'animation_length': largo, 'bones': b}
    # muerde: se lanza, abre la boca y la cierra de golpe
    a['animation.rata.atacar'] = {'loop': False, 'animation_length': 0.45, 'bones': {
        'cuerpo': {'position': {'0.0': [0, 0, 0], '0.12': [0, 0.8, -2.2], '0.3': [0, 0, -0.6], '0.45': [0, 0, 0]}},
        'cabeza': {'rotation': {'0.0': [0, 0, 0], '0.1': [-22, 0, 0], '0.2': [12, 0, 0], '0.45': [0, 0, 0]}},
        'quijada': {'rotation': {'0.0': [0, 0, 0], '0.1': [40, 0, 0], '0.2': [0, 0, 0], '0.45': [0, 0, 0]}},
        'resorte': {'scale': [0, 0, 0]},
    }}
    # muere: se voltea panza arriba, ¡boing! le sale el resorte, las patitas patalean y la llave da sus últimas vueltas
    b = {
        'cuerpo': {'rotation': {'0.0': [0, 0, 0], '0.3': {'vector': [0, 0, 185], 'easing': 'easeOutBack'}, '2.0': [0, 0, 180]},
                   'position': {'0.0': [0, 0, 0], '0.15': [0, 3, 0], '0.3': [0, 5, 0], '2.0': [0, 5, 0]}},
        'resorte': {'scale': {'0.0': [0, 0, 0], '0.3': [0, 0, 0], '0.42': [1, 1.5, 1], '0.55': [1, 0.8, 1], '0.7': [1, 1.1, 1],
                              '0.85': [1, 1, 1], '2.0': [1, 1, 1]},
                    'rotation': {'0.3': [0, 0, 0], '0.5': [0, 0, 20], '0.7': [0, 0, -14], '0.9': [0, 0, 8], '1.1': [0, 0, 0]}},
        'llave': {'rotation': {'0.0': [0, 0, 0], '1.0': [0, 540, 0], '2.0': [0, 640, 0]}},
        'quijada': {'rotation': [24, 0, 0]},
    }
    for k, p in enumerate(PATAS):
        b[p] = {'rotation': {'0.0': [0, 0, 0], '0.4': [40, 0, 0], '0.6': [-30, 0, 0], '0.8': [30, 0, 0], '1.0': [-20, 0, 0],
                             '1.3': [10, 0, 0], '1.6': [0, 0, 0]}}
    a['animation.rata.death'] = {'loop': False, 'animation_length': 2.0, 'bones': b}
    return a


def main():
    modelo()
    m.empacar()
    tex = m.textura(pintor)
    m.guardar(pintor, anims(), glow=['ojos'], tex=tex, bounds=(1.5, 1, 0.3))
    print(len(m.huesos), 'huesos', len(m.cubos), 'cubos')
    if '--vista' in sys.argv:
        out = sys.argv[sys.argv.index('--vista') + 1]
        os.makedirs(out, exist_ok=True)
        tex.resize((256, 256), Image.NEAREST).save(os.path.join(out, 'rata_textura.png'))


if __name__ == '__main__':
    main()
