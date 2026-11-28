#!/usr/bin/env python3
"""Los modelos de las trampas del hotel, hechos a mano por código:

- trampa_escopeta: una escopeta recortada de dos cañones, sujeta con abrazaderas a un tripié de madera, a la altura de la cabeza,
  apuntando a la puerta; un hilo va del gatillo a la manija. Animación "disparo": culatazo, fogonazo y el hilo que se afloja.
- puerta_hotel: la puerta de servicio (verde gris, pintura que se cae, placa de latón, manija redonda, mirilla) que se abre despacio
  rechinando (bisagra a la izquierda, se abre hacia adentro). Animaciones "abrir" (lenta), "abrir_rapido" y "cerrada".

Uso: python3 herramientas/trampas_hotel.py [--vista carpeta]
"""
import os
import sys

import numpy as np

from geokit import Modelo, hexc, liso, manchas

T = 'q.anim_time'


# ---------------------------------------------------------------- la escopeta del tripié
def escopeta():
    m = Modelo('trampa_escopeta', 64, 64)
    m.hueso('raiz', None, (0, 0, 0))
    # el tripié: tres patas abiertas y la cabeza
    m.hueso('tripie', 'raiz', (0, 0, 0))
    for k, (giro, z) in enumerate((((0, 0, 18), 0), ((0, 0, -18), 0), ((-18, 0, 0), 0))):
        piv = (0, 22, 0)
        m.cubo('tripie', (-0.6, 0, -0.6), (1.2, 22, 1.2), 'madera', giro=giro, pivote=piv)
    m.cubo('tripie', (-1.5, 21.5, -1.5), (3, 2, 3), 'metal')
    m.hueso('arma', 'tripie', (0, 25, 0))
    m.cubo('arma', (-1.6, 23.6, -2), (3.2, 2.6, 5), 'metal')                # el cajón de los mecanismos
    m.cubo('arma', (-1.5, 24.3, -12), (1.4, 1.4, 10), 'canon')             # los dos cañones (recortados)
    m.cubo('arma', (0.1, 24.3, -12), (1.4, 1.4, 10), 'canon')
    m.cubo('arma', (-1.4, 23.3, -8), (2.8, 1, 6), 'madera_arma')            # el guardamanos
    m.cubo('arma', (-1.3, 22.2, 3), (2.6, 3.4, 5), 'madera_arma')           # la culata recortada
    m.cubo('arma', (-2.2, 23.2, -5.5), (4.4, 0.6, 1), 'metal')              # las abrazaderas
    m.cubo('arma', (-2.2, 23.2, 0.5), (4.4, 0.6, 1), 'metal')
    m.cubo('arma', (-0.3, 22.6, 0), (0.6, 1.2, 0.6), 'metal')               # el gatillo
    m.hueso('cuerda', 'arma', (0, 23, 0.3))
    m.cubo('cuerda', (-0.1, 23, -16), (0.2, 0.2, 16.3), 'cuerda', giro=(26, 0, 0), pivote=(0, 23, 0.3))
    m.hueso('fogonazo', 'arma', (0, 25, -12.5))
    m.cubo('fogonazo', (-1.6, 23.4, -14.5), (3.2, 3.2, 2.2), 'fuego')

    def pintor(mat, cara, w, h, s, c):
        a = np.full((h, w), 255, np.uint8)
        if mat == 'madera':
            rgb = liso(w, h, '#5a3a22', s, 0.3)
            rgb[:, ::3] = rgb[:, ::3] * 0.8
        elif mat == 'madera_arma':
            rgb = liso(w, h, '#4a2a18', s, 0.25)
            rgb = manchas(rgb, s, '#2a1408', 0.75, 0.3)
        elif mat == 'metal':
            rgb = liso(w, h, '#3a3a3e', s, 0.3)
            rgb = manchas(rgb, s, '#6a3a1a', 0.8, 0.4)
        elif mat == 'canon':
            rgb = liso(w, h, '#2a2a2e', s, 0.2)
            if cara == 'north':
                rgb[1:-1, 1:-1] = hexc('#050505')     # la boca del cañón
        elif mat == 'cuerda':
            rgb = liso(w, h, '#c8b48a', s, 0.2)
        elif mat == 'fuego':
            rgb = liso(w, h, '#ffd060', s, 0.3)
            a[:] = 230
        else:
            rgb = np.broadcast_to(hexc('#ff00ff'), (h, w, 3)).copy()
        return rgb, a

    anims = {
        'animation.trampa_escopeta.idle': {'loop': True, 'animation_length': 2.0, 'bones': {'fogonazo': {'scale': [0, 0, 0]}}},
        # ¡pum!: el culatazo, el fogonazo un instante y el hilo que se afloja
        'animation.trampa_escopeta.disparo': {'loop': False, 'animation_length': 1.2, 'bones': {
            'arma': {'position': {'0.0': [0, 0, 0], '0.04': [0, 0.6, 3], '0.4': [0, 0, 0.6], '1.2': [0, 0, 0]},
                     'rotation': {'0.0': [0, 0, 0], '0.04': {'vector': [-16, 0, 0]}, '0.5': {'vector': [-2, 0, 0], 'easing': 'easeOutQuad'},
                                  '1.2': [0, 0, 0]}},
            'fogonazo': {'scale': {'0.0': [0, 0, 0], '0.01': [1.4, 1.4, 1.4], '0.07': [1, 1, 1], '0.12': [0, 0, 0]}},
            'cuerda': {'rotation': {'0.0': [0, 0, 0], '0.1': [20, 0, 0], '0.6': [44, 0, 6], '1.2': [42, 0, 4]}},
            'tripie': {'rotation': {'0.0': [0, 0, 0], '0.05': [-3, 0, 0], '0.3': [1, 0, 0], '0.6': [0, 0, 0]}},
        }},
    }
    m.empacar()
    tex = m.textura(pintor)
    m.guardar(pintor, anims, glow=['fogonazo'], tex=tex, bounds=(2, 2.5, 0.8))
    return m, tex


# ---------------------------------------------------------------- la puerta de servicio
def puerta():
    m = Modelo('puerta_hotel', 64, 64)
    m.hueso('raiz', None, (0, 0, 0))
    # la bisagra está en la orilla izquierda (vista desde afuera: x = 8); la hoja ocupa todo el hueco de la puerta (16 x 32)
    m.hueso('hoja', 'raiz', (7.5, 0, 0.5))
    m.cubo('hoja', (-8, 0, -0.5), (16, 32, 1.6), 'puerta')
    m.cubo('hoja', (-6.5, 16, -1.1), (2, 1.2, 0.7), 'laton')                 # la manija redonda (del lado contrario a la bisagra)
    m.cubo('hoja', (-6.3, 16.2, -1.5), (1.6, 0.8, 0.5), 'laton')
    m.cubo('hoja', (-6.5, 16, 1.0), (2, 1.2, 0.7), 'laton')
    m.hueso('placa', 'hoja', (0, 24.5, -0.6))
    m.cubo('placa', (-3, 22, -0.8), (6, 2.5, 0.4), 'placa')                   # la placa de latón ("SOLO PERSONAL")
    m.cubo('hoja', (-0.6, 26, -0.7), (1.2, 1.2, 0.3), 'mirilla')
    # el detalle raro de las puertas con trampa: un hilo de nylon que sale de abajo de la puerta y sube a la manija, y una mancha
    # oscura en el piso (las dibuja el juego solo en esas puertas)
    m.hueso('hilo', 'hoja', (-5.5, 0, -1.3))
    m.cubo('hilo', (-5.6, 0.1, -1.4), (0.18, 16.2, 0.18), 'hilo')
    m.cubo('hilo', (-6.2, 16.0, -1.45), (0.9, 0.18, 0.25), 'hilo')
    m.hueso('mancha', 'raiz', (0, 0, -3))
    m.cubo('mancha', (-5, 0.03, -4.2), (9, 0.05, 3.8), 'mancha')
    # el detalle de la salida: luz de colores que se cuela por abajo de la puerta
    m.hueso('luz', 'raiz', (0, 0, 0))
    m.cubo('luz', (-7.6, 0.02, -2.4), (15.2, 0.06, 2.2), 'luz')

    def pintor(mat, cara, w, h, s, c):
        a = np.full((h, w), 255, np.uint8)
        if mat == 'puerta':
            rgb = liso(w, h, '#46564e', s, 0.15, 3)
            rgb = manchas(rgb, s + 1, '#8a8a7a', 0.8, 0.6, 2)            # la pintura que se cae
            rgb = manchas(rgb, s + 2, '#3a2412', 0.84, 0.4, 2)           # óxido
            if cara in ('north', 'south') and w >= 10 and h >= 20:
                # dos tableros hundidos
                for y0, y1 in ((3, 13), (16, 29)):
                    rgb[h - y1:h - y0, 2:w - 2] = rgb[h - y1:h - y0, 2:w - 2] * 0.86
                    rgb[h - y1, 2:w - 2] = rgb[h - y1, 2:w - 2] * 0.7
                # rayones (alguien quiso salir)
                for k in range(4):
                    rgb[h - 12 - k * 2:h - 4 - k * 2, 3 + k * 2] = rgb[h - 12 - k * 2:h - 4 - k * 2, 3 + k * 2] * 0.6 + hexc('#8a8a7a') * 0.4
        elif mat == 'laton':
            rgb = liso(w, h, '#b8913e', s, 0.3)
        elif mat == 'placa':
            rgb = liso(w, h, '#c9a040', s, 0.2)
            if cara == 'north' and w >= 6:
                rgb[1, 1:-1:2] = hexc('#3a2a10')                          # letras chiquitas (no se leen de lejos)
        elif mat == 'hilo':
            rgb = liso(w, h, '#dcdcd4', s, 0.05)
        elif mat == 'mancha':
            rgb = liso(w, h, '#3a0c0a', s, 0.3)
            yy, xx = np.mgrid[0:h, 0:w]
            d = np.hypot((xx - w * 0.45) / (w * 0.5), (yy - h * 0.6) / (h * 0.55))
            a[:] = np.clip((1.1 - d) * 400, 0, 220).astype(np.uint8)
        elif mat == 'luz':
            cols = [hexc('#ff3a30'), hexc('#ffd030'), hexc('#40e050'), hexc('#3a8aff')]
            rgb = np.zeros((h, w, 3), np.float32)
            for x in range(w):
                rgb[:, x] = cols[(x * 4 // max(1, w)) % 4]
            yy = np.mgrid[0:h, 0:w][0]
            a[:] = np.clip(255 - yy * (255 // max(1, h)), 40, 255).astype(np.uint8)
        elif mat == 'mirilla':
            rgb = liso(w, h, '#b8913e', s, 0.2)
            if cara == 'north':
                rgb[:, :] = hexc('#101418')
        else:
            rgb = np.broadcast_to(hexc('#ff00ff'), (h, w, 3)).copy()
        return rgb, a

    anims = {
        'animation.puerta_hotel.cerrada': {'loop': True, 'animation_length': 1.0, 'bones': {}},
        # se abre despacio, rechinando (a tirones), y se queda abierta
        'animation.puerta_hotel.abrir': {'loop': False, 'animation_length': 1.6, 'bones': {
            'hoja': {'rotation': {'0.0': [0, 0, 0], '0.15': [0, 4, 0], '0.35': [0, 6, 0], '0.7': {'vector': [0, 38, 0], 'easing': 'easeInSine'},
                                  '1.3': {'vector': [0, 96, 0], 'easing': 'easeOutQuad'}, '1.45': [0, 92, 0], '1.6': [0, 94, 0]}}}},
        'animation.puerta_hotel.abrir_rapido': {'loop': False, 'animation_length': 0.5, 'bones': {
            'hoja': {'rotation': {'0.0': [0, 0, 0], '0.35': {'vector': [0, 100, 0], 'easing': 'easeOutQuad'}, '0.5': [0, 94, 0]}}}},
        'animation.puerta_hotel.abierta': {'loop': True, 'animation_length': 1.0, 'bones': {'hoja': {'rotation': [0, 94, 0]}}},
        # se cierra sola de un portazo (la de la salida, cuando alguien ya pasó)
        'animation.puerta_hotel.cerrar': {'loop': False, 'animation_length': 0.9, 'bones': {
            'hoja': {'rotation': {'0.0': [0, 94, 0], '0.55': {'vector': [0, 6, 0], 'easing': 'easeInQuad'}, '0.62': [0, -2, 0],
                                  '0.72': [0, 2, 0], '0.9': [0, 0, 0]}}}},
    }
    m.empacar()
    tex = m.textura(pintor)
    m.guardar(pintor, anims, glow=['luz'], tex=tex, bounds=(2, 2.5, 1))
    return m, tex


def main():
    me, te = escopeta()
    mp, tp = puerta()
    print('escopeta', len(me.cubos), 'cubos · puerta', len(mp.cubos), 'cubos')


if __name__ == '__main__':
    main()
