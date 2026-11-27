#!/usr/bin/env python3
"""Adornos de las salas del evento (capítulos 1 y 3 a 7) y de los pasillos y transiciones entre ellas.

Mismas reglas del kit: el modelo mira al NORTE (z = 0 es el frente); los de pared tienen la espalda en z = 16; los altos ponen sus cajas
en dos partes ('lower' y 'upper', cada una de 0 a 16).
"""
import kit
from kit import Prop, Ramp, solid
from props_celdas import porcelana, tela as tela_vieja
from props_mansion import nogal, oro, hierro, tela


# ---------------------------------------------------------------- capítulo 1: el cuarto a oscuras

def muneca_grande():
    """Muñeca de porcelana del tamaño de una niña, de pie: vestido de encaje con moño, calcetas, ojos negros y una grieta. Sin choque
    (el evento sabe cuándo alguien la toca)."""
    p = Prop('muneca_grande', 'Muñeca grande', 'Life-size doll', kind='floor', sound='glass', seed=600, tall=True, collide=False,
             shape=[[3, 0, 3, 13, 16, 13]], shape_upper=[[4, 0, 4, 12, 12, 12]])
    por = porcelana(600, sucio=0.35)
    vestido = tela_vieja('#4a3a44', seed=601, sucio=0.45)
    encaje = tela_vieja('#d8d0bc', seed=602, sucio=0.45)
    calceta = tela_vieja('#c9c2b2', seed=603, sucio=0.3)
    zapato = solid('#1c1210')
    pelo = tela_vieja('#2e1a10', seed=604, sucio=0.2)
    mono = tela_vieja('#6a1a22', seed=605, sucio=0.3)
    negro = solid('#070505')
    p.at('lower')
    p.box(5.0, 0, 5.0, 7.4, 1.4, 9.0, zapato)                 # zapatos
    p.box(8.6, 0, 5.0, 11.0, 1.4, 9.0, zapato)
    p.box(5.4, 1.4, 6.2, 7.0, 6.0, 8.2, calceta)               # piernas
    p.box(9.0, 1.4, 6.2, 10.6, 6.0, 8.2, calceta)
    p.box(3.6, 6.0, 3.6, 12.4, 7.0, 12.4, encaje)              # orilla de encaje de la falda
    p.box(3.8, 7.0, 3.8, 12.2, 11.0, 12.2, vestido)            # falda (en campana)
    p.box(4.6, 11.0, 4.6, 11.4, 14.0, 11.4, vestido)
    p.box(5.2, 14.0, 5.2, 10.8, 16.0, 10.8, vestido)           # cintura
    p.box(5.0, 13.2, 4.9, 11.0, 14.0, 11.1, mono)              # listón de la cintura
    p.box(2.9, 10.5, 6.6, 4.4, 16.0, 8.6, por)                 # brazos colgando (manos abajo)
    p.box(11.6, 10.5, 6.6, 13.1, 16.0, 8.6, por)
    p.at('upper')
    p.box(5.2, 0, 5.4, 10.8, 4.0, 10.6, vestido)               # pecho
    p.box(2.9, 0, 6.4, 4.6, 4.0, 8.8, vestido)                 # mangas abombadas
    p.box(11.4, 0, 6.4, 13.1, 4.0, 8.8, vestido)
    p.box(4.8, 3.4, 5.0, 11.2, 4.4, 11.0, encaje)              # cuello de encaje
    p.box(5.0, 4.4, 4.6, 11.0, 10.6, 10.8, por)                # cabeza (grande)
    p.box(5.9, 7.0, 4.45, 7.5, 8.6, 4.6, negro)                # ojos negros
    p.box(8.5, 7.0, 4.45, 10.1, 8.6, 4.6, negro)
    p.box(6.5, 7.6, 4.4, 6.9, 8.0, 4.5, solid('#d8d4cc'))      # brillito
    p.box(9.1, 7.6, 4.4, 9.5, 8.0, 4.5, solid('#d8d4cc'))
    p.box(7.4, 5.4, 4.45, 8.6, 6.0, 4.6, solid('#7a1418'))     # boca
    p.box(6.1, 6.1, 4.45, 6.4, 7.0, 4.55, solid('#2a1a16'))    # lágrima sucia
    p.box(4.6, 9.6, 4.4, 11.4, 11.8, 11.2, pelo)               # pelo (flequillo y casco)
    p.box(4.4, 5.0, 8.4, 11.6, 11.0, 11.4, pelo)
    p.box(4.4, 2.0, 9.6, 11.6, 6.0, 11.2, pelo)                # caída del pelo por la espalda
    p.box(8.6, 11.4, 6.0, 12.4, 13.0, 8.0, mono)               # moño
    p.box(10.0, 10.8, 6.6, 11.0, 11.8, 7.4, mono)
    return p


# ---------------------------------------------------------------- los vehículos: el autobús del Coleccionista

def pintura(color, seed):
    """Pintura de carrocería con algo de brillo y rayones."""
    return kit.metal(Ramp(*[_tono(color, k) for k in (0.72, 0.84, 0.94, 1.04)]), streak=0.35, seed=seed, sparkle=0.03)


def _tono(color, k):
    c = color.lstrip('#')
    r, g, b = (int(c[i:i + 2], 16) for i in (0, 2, 4))
    return '#%02x%02x%02x' % tuple(max(0, min(255, int(v * k))) for v in (r, g, b))


def cromo(seed):
    return kit.metal(Ramp('#5b5f63', '#7b8085', '#9da2a6', '#c3c7ca'), streak=0.6, seed=seed, sparkle=0.12)


def hule(seed):
    return kit.stone(Ramp('#0f0f10', '#161617', '#1c1c1e', '#232325'), scale=1.5, seed=seed)


def llanta():
    """Rueda del autobús (pegada a la carrocería): llanta negra gastada, rin cromado con tuercas y tapón."""
    p = Prop('llanta', 'Rueda de autobús', 'Bus wheel', kind='wall', sound='metal', seed=610, shape=[[0, 0, 9, 16, 15, 16]])
    h, c, n = hule(610), cromo(611), solid('#2a2c2e')
    # la llanta: un "círculo" de cajas (de frente: x 0.5..15.5, y 0..15)
    for (x0, x1, y0, y1) in ((4.5, 11.5, 0, 15), (2.5, 13.5, 1, 14), (1.2, 14.8, 2.5, 12.5), (0.5, 15.5, 4.5, 10.5)):
        p.box(x0, y0, 10, x1, y1, 16, h)
    # el rin
    for (x0, x1, y0, y1) in ((5.5, 10.5, 3, 12), (4, 12, 4.5, 10.5)):
        p.box(x0, y0, 9.4, x1, y1, 10, c)
    p.box(6.5, 6, 8.8, 9.5, 9, 9.4, cromo(612))                # tapón
    for (x, y) in ((5.2, 7.5), (10.8, 7.5), (8, 4.6), (8, 10.4)):
        p.box(x - 0.5, y - 0.5, 9.1, x + 0.5, y + 0.5, 9.4, n)  # tuercas
    return p


def asiento_bus():
    """Asiento de autobús viejo: banca acolchada de vinil vino, respaldo alto con agarradera cromada y patas de tubo."""
    p = Prop('asiento_bus', 'Asiento de autobús', 'Bus seat', kind='floor', sound='wool', seed=620,
             shape=[[1, 0, 2, 15, 8, 13], [1, 8, 12, 15, 16, 15]])
    vin = kit.cloth('#5a1c1e', weave=0.03, seed=620, fold=0.15)
    vin2 = kit.cloth('#43141a', weave=0.03, seed=621, fold=0.1)
    c = cromo(622)
    p.box(2, 0, 4, 3, 6, 5, c)                                  # patas
    p.box(13, 0, 4, 14, 6, 5, c)
    p.box(2, 0, 11, 3, 6, 12, c)
    p.box(13, 0, 11, 14, 6, 12, c)
    p.box(1, 6, 2.5, 15, 8.5, 12.5, vin)                        # cojín
    p.box(1.4, 8.5, 3, 14.6, 8.8, 12, vin2)
    p.box(1, 8, 12, 15, 18, 14.5, vin)                          # respaldo
    p.box(1.5, 9, 11.7, 14.5, 17.4, 12, vin2)
    p.box(2, 18, 12.6, 14, 18.6, 13.9, c)                       # agarradera de arriba
    p.box(2, 17, 12.8, 2.8, 18, 13.7, c)
    p.box(13.2, 17, 12.8, 14, 18, 13.7, c)
    return p


def volante():
    """El volante del chofer sobre su columna (para el maniquí que maneja)."""
    p = Prop('volante', 'Volante', 'Steering wheel', kind='floor', sound='metal', seed=630, shape=[[3, 0, 2, 13, 13, 8]])
    n, c = solid('#111214'), cromo(631)
    p.box(7, 0, 3, 9, 1, 7, n)
    p.box(7.3, 1, 4, 8.7, 10, 5.4, n)                           # columna
    p.box(6.5, 9, 3.6, 9.5, 10.5, 5.8, c)
    for (x0, x1, y0, y1) in ((3, 13, 10.5, 11.5), (3, 4, 11.5, 13.5), (12, 13, 11.5, 13.5), (4, 12, 13.5, 14.5)):
        p.box(x0, y0, 4, x1, y1, 5.2, n)                         # el aro
    p.box(7.4, 11.5, 4.1, 8.6, 13.5, 5.0, c)                     # rayo
    return p


def maleta():
    """Maleta de cuero viejo con correas, esquinas de latón y etiqueta: equipaje olvidado."""
    p = Prop('maleta', 'Maleta vieja', 'Old suitcase', kind='floor', sound='wool', seed=640, shape=[[1.5, 0, 3, 14.5, 7.5, 13]])
    cu = kit.wood(Ramp('#3a2414', '#4a2e18', '#5a381e', '#6a4424'), axis='h', grain=1.2, seed=640)
    co = kit.cloth('#2a1a10', weave=0.05, seed=641)
    la = oro(642)
    p.box(1.5, 0, 3, 14.5, 7, 13, cu)
    p.box(1.4, 3.3, 2.9, 14.6, 3.7, 13.1, solid('#1e130b'))     # la orilla de la tapa
    for x in (4.5, 11.5):
        p.box(x - 0.6, -0.01, 2.8, x + 0.6, 7.2, 13.2, co)       # correas
    for (x, z) in ((1.5, 3), (13.6, 3), (1.5, 12.1), (13.6, 12.1)):
        p.box(x - 0.1, 0, z - 0.1, x + 1.0, 7.1, z + 1.0, la)    # esquinas de latón
    p.box(6.5, 7, 7, 9.5, 8.6, 9, co)                            # asa
    p.box(9, 4, 2.7, 12, 6.2, 2.9, solid('#d8cfb4'))             # etiqueta
    return p


def puerta_autobus():
    """Puerta plegable del autobús: abajo lámina pintada de vino; arriba vidrio oscuro (de afuera no se ve el interior)."""
    p = Prop('puerta_autobus', 'Puerta de autobús', 'Bus door', kind='floor', tall=True, sound='metal', seed=650)
    lam = pintura('#5a1c1e', 650)
    marco = solid('#1a1a1c')
    vid = kit.metal(Ramp('#0e1216', '#141a20', '#1a2128', '#222a32'), streak=0.2, seed=652, sparkle=0.02)
    c = cromo(653)
    z0, z1 = 7.2, 8.8
    abajo = ('down',)
    p.at('lower')
    p.box(0, 0, z0, 16, 16, z1, lam, skip=abajo)
    p.box(0, 0, 6.9, 1.2, 16, 9.1, marco, skip=abajo)
    p.box(14.8, 0, 6.9, 16, 16, 9.1, marco, skip=abajo)
    p.box(0, 0, 6.8, 16, 1.5, 9.2, c, skip=abajo)               # el escalón cromado
    p.box(2.5, 9, 7.0, 13.5, 15, 9.0, vid)                       # el vidrio de abajo
    p.box(13.6, 4, 6.6, 14.3, 12, 7.0, c)                        # la jaladera
    p.at('upper')
    p.box(0, 0, z0, 16, 14.5, z1, marco)
    p.box(1.2, 1, 7.0, 14.8, 13.5, 9.0, vid)                     # el vidrio grande
    p.box(0, 14.5, 6.6, 16, 16, 9.4, marco)                      # riel de arriba
    p.box(7.6, 1, 6.95, 8.4, 13.5, 9.05, marco)                  # el doblez de la hoja
    return p


def todos():
    return [muneca_grande(), llanta(), asiento_bus(), volante(), maleta()]
