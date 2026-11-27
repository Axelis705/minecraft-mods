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


# ---------------------------------------------------------------- adornos de las salas

def laton(seed):
    return kit.metal(Ramp('#6b4e1e', '#8a6a2a', '#a88438', '#c9a14a', '#e0bd62'), streak=0.5, seed=seed, sparkle=0.08)


def pintado(color, seed):
    """Madera pintada de juguete (un poco gastada)."""
    return kit.cloth(color, weave=0.04, seed=seed, fold=0.05)


def engrane():
    """Engrane de latón en la pared (el mecanismo de la caja de música): dientes, rayos y el eje."""
    p = Prop('engrane', 'Engrane de latón', 'Brass gear', kind='wall', sound='metal', seed=700, collide=False, shape=[[0, 0, 14, 16, 16, 16]])
    la, la2, n = laton(700), laton(701), solid('#2a2014')
    p.box(3, 3, 14.4, 13, 13, 15.6, la)                         # el disco
    for (x0, y0, x1, y1) in ((6.5, 0, 9.5, 3), (6.5, 13, 9.5, 16), (0, 6.5, 3, 9.5), (13, 6.5, 16, 9.5)):
        p.box(x0, y0, 14.5, x1, y1, 15.5, la2)                   # dientes rectos
    for (x0, y0, x1, y1) in ((1.6, 1.6, 4.4, 4.4), (11.6, 1.6, 14.4, 4.4), (1.6, 11.6, 4.4, 14.4), (11.6, 11.6, 14.4, 14.4)):
        p.box(x0, y0, 14.5, x1, y1, 15.5, la2)                   # dientes de las esquinas
    p.box(5, 5, 14.2, 11, 11, 14.4, n)                           # el hueco con rayos
    p.box(7.3, 5, 14.1, 8.7, 11, 14.4, la)
    p.box(5, 7.3, 14.1, 11, 8.7, 14.4, la)
    p.box(6.6, 6.6, 13.4, 9.4, 9.4, 14.2, laton(702))            # el eje
    return p


def nota_musical():
    """Una nota musical negra (corchea) colgando de un hilo: adorno del techo de la caja de música y el salón del piano."""
    p = Prop('nota_musical', 'Nota musical', 'Music note', kind='hang', sound='wood', seed=710, collide=False, shape=[[4, 0, 7, 12, 16, 9]])
    n, b, h = solid('#141214'), solid('#2a2628'), solid('#bfb8a8')
    p.box(7.8, 13, 7.8, 8.2, 16, 8.2, h)                         # el hilo
    p.box(4, 0.5, 6.8, 8.5, 4, 9.2, n)                           # la cabeza (ovalada)
    p.box(4.6, 0, 7, 7.9, 4.5, 9, n)
    p.box(7.4, 2.5, 7.2, 8.6, 13, 8.8, n)                        # la plica
    p.box(8.6, 9, 7.3, 10.6, 13, 8.7, b)                         # el gancho
    p.box(10.2, 7, 7.3, 11.6, 10, 8.7, b)
    return p


def soldadito():
    """Soldadito de plomo del tamaño de un niño: botas negras, pantalón blanco, casaca roja con botones dorados, gorro alto y su fusil."""
    p = Prop('soldadito', 'Soldadito de plomo', 'Tin soldier', kind='floor', sound='metal', seed=720, tall=True,
             shape=[[4, 0, 5, 12, 16, 11]], shape_upper=[[4, 0, 5, 12, 14, 11]])
    negro, blanco, rojo = pintado('#16140f', 720), pintado('#d8d2c2', 721), pintado('#8a1a18', 722)
    piel, oro_ = pintado('#e0b896', 723), laton(724)
    base = pintado('#2a4a2a', 725)
    p.at('lower')
    p.box(3.5, 0, 4.5, 12.5, 1, 11.5, base)                      # la base verde
    p.box(5, 1, 6, 7.6, 4, 10, negro)                            # botas
    p.box(8.4, 1, 6, 11, 4, 10, negro)
    p.box(5.2, 4, 6.3, 7.6, 10, 9.7, blanco)                     # pantalón
    p.box(8.4, 4, 6.3, 10.8, 10, 9.7, blanco)
    p.box(4.8, 10, 5.6, 11.2, 16, 10.4, rojo)                    # casaca
    p.box(4.7, 10, 5.5, 11.3, 11, 10.5, solid('#1a1410'))        # cinturón
    p.box(7.5, 10.2, 5.4, 8.5, 10.8, 5.5, oro_)                  # hebilla
    for y in (12, 13.5, 15):
        p.box(7.6, y, 5.45, 8.4, y + 0.6, 5.55, oro_)            # botones
    p.box(3.4, 10, 7, 4.8, 16, 9, rojo)                          # brazo izquierdo (al lado)
    p.box(11.2, 12, 6.5, 12.6, 16, 8.5, rojo)                    # brazo derecho (sostiene el fusil)
    p.box(12.0, 6, 7.2, 13.0, 16, 8.2, solid('#3a2a1a'))         # culata y cañón del fusil
    p.at('upper')
    p.box(4.8, 0, 5.6, 11.2, 2.5, 10.4, rojo)                    # hombros
    p.box(4.4, 1.6, 5.4, 11.6, 2.4, 10.6, oro_)                  # charreteras
    p.box(3.4, 0, 7, 4.8, 2.5, 9, rojo)
    p.box(11.2, 0, 6.5, 12.6, 2.5, 8.5, rojo)
    p.box(12.2, 0, 7.4, 12.8, 6, 8.0, solid('#55585e'))          # la bayoneta
    p.box(5.8, 2.5, 5.8, 10.2, 6.5, 10.2, piel)                  # cara
    p.box(6.6, 4.4, 5.7, 7.4, 5.2, 5.8, negro)                   # ojos
    p.box(8.6, 4.4, 5.7, 9.4, 5.2, 5.8, negro)
    p.box(6.4, 3.3, 5.7, 7.2, 3.8, 5.8, solid('#c46a6a'))        # chapitas
    p.box(8.8, 3.3, 5.7, 9.6, 3.8, 5.8, solid('#c46a6a'))
    p.box(5.4, 6.5, 5.4, 10.6, 13.5, 10.6, negro)                # el gorro alto
    p.box(5.3, 6.5, 5.3, 10.7, 7.2, 10.7, oro_)                  # su cinta
    return p


def bailarina_figura():
    """La bailarina de porcelana de una caja de música, en grande: sobre su pedestal de latón, en puntas, con tutú y los brazos arriba."""
    p = Prop('bailarina_figura', 'Bailarina de porcelana', 'Porcelain ballerina', kind='floor', sound='glass', seed=730, tall=True,
             shape=[[2, 0, 2, 14, 16, 14]], shape_upper=[[4, 0, 4, 12, 14, 12]])
    por, tutu, malla = porcelana(730, sucio=0.25), tela_vieja('#e7c9cf', seed=731, sucio=0.35), tela_vieja('#d9b3ba', seed=732, sucio=0.2)
    la, pelo = laton(733), solid('#2a1a12')
    p.at('lower')
    p.box(2, 0, 2, 14, 2, 14, la)                                # pedestal redondo (escalonado)
    p.box(3, 2, 3, 13, 3, 13, laton(734))
    p.box(5, 3, 5, 11, 3.6, 11, solid('#3a1a20'))                # terciopelo
    p.box(7.6, 3.6, 7.4, 8.4, 11, 8.4, malla)                    # la pierna en puntas
    p.box(7.6, 9, 7.6, 12, 9.8, 8.4, malla)                      # la otra pierna, en arabesco
    p.box(6, 11, 6, 10, 16, 10, malla)                           # cadera y torso
    p.box(2.5, 12.5, 2.5, 13.5, 14, 13.5, tutu)                  # el tutú (dos capas)
    p.box(3.5, 14, 3.5, 12.5, 15, 12.5, tutu)
    p.at('upper')
    p.box(6.4, 0, 6.6, 9.6, 3, 9.4, malla)                       # pecho
    p.box(6.6, 3, 6.6, 9.4, 6.2, 9.4, por)                       # cabeza
    p.box(6.4, 5, 6.4, 9.6, 6.6, 9.6, pelo)                      # chongo
    p.box(7.2, 6.6, 7.2, 8.8, 7.8, 8.8, pelo)
    p.box(4.4, 2, 7.5, 6.4, 3, 8.5, por)                         # brazos arriba, en arco
    p.box(9.6, 2, 7.5, 11.6, 3, 8.5, por)
    p.box(4.4, 3, 7.5, 5.4, 8, 8.5, por)
    p.box(10.6, 3, 7.5, 11.6, 8, 8.5, por)
    p.box(5.4, 8, 7.5, 10.6, 9, 8.5, por)
    return p


def cubos_letras():
    """Cubos de madera con letras (una torre de tres, chueca)."""
    p = Prop('cubos_letras', 'Cubos con letras', 'Alphabet blocks', kind='floor', sound='wood', seed=740, shape=[[1, 0, 1, 15, 15, 15]])
    cols = [('#8a2a20', '#e8d8b0'), ('#2a4a7a', '#e8d8b0'), ('#c8a030', '#3a2a18')]
    pos = [(1, 0, 1, 8), (8, 0, 6, 7), (4, 7, 3, 7)]
    for i, ((fondo, letra), (x, y, z, t)) in enumerate(zip(cols, pos)):
        p.box(x, y, z, x + t, y + t, z + t, pintado(fondo, 741 + i))
        # la letra: una cruz/marco en dos caras visibles
        p.box(x + 1.5, y + 1.5, z - 0.05, x + t - 1.5, y + 2.5, z, solid(letra))
        p.box(x + t / 2 - 0.5, y + 1.5, z - 0.05, x + t / 2 + 0.5, y + t - 1.5, z, solid(letra))
        p.box(x - 0.05, y + 1.5, z + 1.5, x, y + t - 1.5, z + 2.5, solid(letra))
        p.box(x - 0.05, y + t - 2.5, z + 1.5, x, y + t - 1.5, z + t - 1.5, solid(letra))
    return p


def tambor():
    """Tambor de juguete rojo y blanco con cuerdas doradas en zigzag y sus dos baquetas encima."""
    p = Prop('tambor', 'Tambor de juguete', 'Toy drum', kind='floor', sound='wood', seed=750, shape=[[2, 0, 2, 14, 10, 14]])
    rojo, blanco, la, cuero = pintado('#9a2220', 750), pintado('#e6dccb', 751), laton(752), solid('#d9c9a4')
    p.box(2.5, 0, 3.5, 13.5, 9, 12.5, rojo)
    p.box(3.5, 0, 2.5, 12.5, 9, 13.5, rojo)
    p.box(2.3, 0, 3.3, 13.7, 1.2, 12.7, blanco)                  # aros
    p.box(2.3, 7.8, 3.3, 13.7, 9, 12.7, blanco)
    p.box(3.3, 0, 2.3, 12.7, 1.2, 13.7, blanco)
    p.box(3.3, 7.8, 2.3, 12.7, 9, 13.7, blanco)
    p.box(3.5, 9, 3.5, 12.5, 9.3, 12.5, cuero)                   # el parche
    for x in (4.5, 7.5, 10.5):
        p.box(x, 1.2, 2.2, x + 0.6, 7.8, 2.4, la)                # cuerdas
    p.box(4, 9.3, 6, 12, 10, 6.7, solid('#5a3a22'))              # baquetas
    p.box(5, 9.3, 9, 13, 10, 9.7, solid('#5a3a22'))
    return p


def trompo():
    """Trompo de madera pintado a rayas, con la punta de metal y su cordón."""
    p = Prop('trompo', 'Trompo', 'Spinning top', kind='floor', sound='wood', seed=760, collide=False, shape=[[4, 0, 4, 12, 10, 12]])
    a, b, m = pintado('#1f5a8a', 760), pintado('#d8b030', 761), solid('#7a7e82')
    p.box(7.6, 0, 7.6, 8.4, 1.5, 8.4, m)
    p.box(6.5, 1.5, 6.5, 9.5, 3, 9.5, a)
    p.box(5, 3, 5, 11, 5, 11, b)
    p.box(4.2, 5, 4.2, 11.8, 7, 11.8, a)
    p.box(5.2, 7, 5.2, 10.8, 8, 10.8, b)
    p.box(7.4, 8, 7.4, 8.6, 10, 8.6, solid('#3a2a18'))
    p.box(2, 0, 9, 7, 0.4, 9.6, solid('#cfc3a0'))                # el cordón en el piso
    return p


def globos():
    """Tres globos desinflándose (rojo, amarillo y azul, deslavados) atados a un peso: altos, flotan."""
    p = Prop('globos', 'Globos', 'Balloons', kind='floor', sound='wool', seed=770, tall=True, collide=False,
             shape=[[6, 0, 6, 10, 4, 10]], shape_upper=[[2, 0, 2, 14, 15, 14]])
    hilo, peso = solid('#d8d0bc'), solid('#2a2a2c')
    p.at('lower')
    p.box(6, 0, 6, 10, 3, 10, peso)
    p.box(7.8, 3, 7.8, 8.2, 16, 8.2, hilo)
    p.box(7.0, 3, 7.8, 7.4, 16, 8.2, hilo)
    p.box(8.6, 3, 7.8, 9.0, 16, 8.2, hilo)
    p.at('upper')
    for (x, y, z, c) in ((2.5, 4, 5, '#9a3a34'), (8.5, 6, 4, '#b8a04a'), (5, 9, 8, '#3e5a86')):
        g = tela_vieja(c, seed=771, sucio=0.25)
        p.box(x, y, z, x + 5, y + 6, z + 5, g)
        p.box(x + 0.6, y - 0.6, z + 0.6, x + 4.4, y, z + 4.4, g)
        p.box(x + 2.2, y - 1.2, z + 2.2, x + 2.8, y - 0.6, z + 2.8, solid('#3a2a2a'))
    p.box(7.8, 0, 7.8, 8.2, 3, 8.2, hilo)
    return p


def banderines():
    """Una tira de banderines (triángulos de colores) que cuelga de un lado al otro del bloque (a lo largo de x)."""
    p = Prop('banderines', 'Banderines', 'Bunting', kind='hang', sound='wool', seed=780, collide=False, shape=[[0, 9, 7.5, 16, 16, 8.5]])
    cuerda = solid('#cfc3a0')
    p.box(0, 14.5, 7.8, 16, 15, 8.2, cuerda)
    for i, c in enumerate(('#9a2a24', '#c8a838', '#2e5a8a', '#3a7a3a')):
        x = i * 4 + 0.3
        mat = tela_vieja(c, seed=781 + i, sucio=0.3)
        p.box(x, 12, 7.9, x + 3.4, 14.5, 8.1, mat)
        p.box(x + 0.8, 10.5, 7.9, x + 2.6, 12, 8.1, mat)
        p.box(x + 1.4, 9.5, 7.9, x + 2.0, 10.5, 8.1, mat)
    return p


def marioneta():
    """Una marioneta de payaso colgando de su cruceta (hilos hasta arriba): cabeza blanca, nariz roja, traje de rombos."""
    p = Prop('marioneta', 'Marioneta de payaso', 'Clown marionette', kind='hang', sound='wool', seed=790, collide=False,
             shape=[[4, 0, 6, 12, 16, 10]])
    hilo, cruz = solid('#d8d0bc'), pintado('#4a3020', 790)
    cara, rojo, traje, traje2 = pintado('#e8e2d8', 791), solid('#b01818'), pintado('#6a2a6a', 792), pintado('#c8a838', 793)
    p.box(3, 15, 7.5, 13, 16, 8.5, cruz)                         # la cruceta
    p.box(7.5, 15, 3, 8.5, 16, 13, cruz)
    for x in (4, 12):
        p.box(x - 0.15, 5, 7.9, x + 0.15, 15, 8.1, hilo)         # hilos a las manos
    p.box(7.85, 11, 7.9, 8.15, 15, 8.1, hilo)                    # hilo a la cabeza
    p.box(6, 8, 6.5, 10, 11.5, 9.5, cara)                        # cabeza
    p.box(7.5, 9, 6.2, 8.5, 10, 6.5, rojo)                       # nariz
    p.box(5.6, 11, 6.2, 10.4, 12, 9.8, rojo)                     # pelo rojo
    p.box(6.5, 3, 7, 9.5, 8, 9, traje)                           # cuerpo
    p.box(6.5, 4.5, 6.9, 9.5, 5.5, 9.1, traje2)
    p.box(4, 4.5, 7.5, 6.5, 6, 8.5, traje)                       # brazos (colgando de los hilos)
    p.box(9.5, 4.5, 7.5, 12, 6, 8.5, traje)
    p.box(6.8, 0, 7.4, 7.8, 3, 8.6, traje2)                      # piernas flojas
    p.box(8.2, 0.5, 7.4, 9.2, 3, 8.6, traje2)
    return p


def caja_juguete():
    """Una cajita sorpresa de juguete, abierta: el payasito salió con el resorte (como Resortes, pero chiquito)."""
    p = Prop('caja_juguete', 'Cajita sorpresa', 'Toy jack-in-the-box', kind='floor', sound='wood', seed=800, shape=[[3, 0, 3, 13, 15, 13]])
    caja, filo, la = pintado('#2a3a7a', 800), pintado('#c8a838', 801), laton(802)
    cara, rojo = pintado('#e8e2d8', 803), solid('#b01818')
    p.box(3.5, 0, 3.5, 12.5, 7, 12.5, caja)
    p.box(3.3, 6.5, 3.3, 12.7, 7.2, 12.7, filo)
    p.box(3.5, 7, 12, 12.5, 15, 12.6, caja)                      # la tapa abierta (atrás)
    p.box(12.5, 3, 7.3, 14, 4, 8.7, la)                          # la manivela
    p.box(13.6, 3.5, 7.6, 14.4, 6, 8.4, la)
    for y in (7.5, 8.6, 9.7):
        p.box(6.8, y, 6.8, 9.2, y + 0.6, 9.2, solid('#9a9aa0'))  # el resorte
    p.box(5.8, 10.5, 5.8, 10.2, 14.5, 10.2, cara)                # cabeza del payasito
    p.box(7.5, 12, 5.5, 8.5, 13, 5.8, rojo)
    p.box(5.4, 14.2, 5.4, 10.6, 15.2, 10.6, pintado('#3a7a3a', 804))
    return p


def tetera():
    """Juego de té: la tetera de porcelana con flores, dos tazas con su plato y una charola de plata."""
    p = Prop('tetera', 'Juego de té', 'Tea set', kind='floor', sound='glass', seed=810, collide=False, shape=[[1, 0, 1, 15, 9, 15]])
    por, flor, plata = porcelana(810, sucio=0.15), solid('#9a3a5a'), cromo(811)
    p.box(1, 0, 2, 15, 0.6, 14, plata)                           # charola
    p.box(5, 0.6, 5, 11, 6, 11, por)                             # tetera
    p.box(4.6, 2.5, 4.6, 11.4, 4, 11.4, por)
    p.box(5, 3.2, 4.55, 11, 3.8, 4.65, flor)
    p.box(6.5, 6, 6.5, 9.5, 7, 9.5, por)                         # tapa
    p.box(7.5, 7, 7.5, 8.5, 8, 8.5, flor)
    p.box(11, 2, 7.4, 13.5, 3, 8.6, por)                         # pico
    p.box(12.8, 3, 7.4, 13.8, 4.5, 8.6, por)
    p.box(3.4, 2, 7.6, 5, 5, 8.4, por)                           # asa
    for (x, z) in ((1.8, 11), (11, 11.2)):
        p.box(x, 0.6, z, x + 3.4, 0.9, z + 3.4, por)             # plato
        p.box(x + 0.6, 0.9, z + 0.6, x + 2.8, 2.6, z + 2.8, por) # taza
        p.box(x + 0.9, 2.5, z + 0.9, x + 2.5, 2.6, z + 2.5, solid('#5a3018'))  # el té
    return p


def pastelero():
    """Platón de tres pisos con pastelitos (rosas, de chocolate y con cereza)."""
    p = Prop('pastelero', 'Platón de pastelitos', 'Cake stand', kind='floor', sound='glass', seed=820, collide=False,
             shape=[[2, 0, 2, 14, 14, 14]])
    por, la = porcelana(820, sucio=0.1), laton(821)
    p.box(7.4, 0, 7.4, 8.6, 13, 8.6, la)                         # el poste
    p.box(5, 0, 5, 11, 0.8, 11, la)
    for (y, r) in ((3, 6), (7, 4.5), (11, 3)):
        p.box(8 - r, y, 8 - r, 8 + r, y + 0.6, 8 + r, por)       # los platos
    p.box(7.2, 13, 7.2, 8.8, 14.2, 8.8, la)                      # el remate
    cs = ['#d8889a', '#5a3020', '#e8dcc0', '#d8889a', '#5a3020']
    for i, (x, z) in enumerate(((3, 3), (10, 4), (4, 10), (10.5, 10.5), (7, 2.6))):
        p.box(x, 3.6, z, x + 2.4, 5.4, z + 2.4, solid(cs[i % len(cs)]))
    for i, (x, z) in enumerate(((4.5, 4.5), (9, 5), (5, 9))):
        p.box(x, 7.6, z, x + 2.2, 9.2, z + 2.2, solid(cs[(i + 1) % len(cs)]))
        p.box(x + 0.8, 9.2, z + 0.8, x + 1.4, 9.8, z + 1.4, solid('#b0101a'))
    p.box(6.8, 11.6, 6.8, 9.2, 13, 9.2, solid('#d8889a'))
    return p


def arpa():
    """Arpa dorada de pie (alta), con sus cuerdas."""
    p = Prop('arpa', 'Arpa', 'Harp', kind='floor', sound='wood', seed=830, tall=True, shape=[[3, 0, 6, 13, 16, 10]],
             shape_upper=[[3, 0, 6, 13, 13, 10]])
    o, o2, cu = oro(830), laton(831), solid('#d8d0b8')
    p.at('lower')
    p.box(3, 0, 5, 13, 2, 11, o)                                 # base
    p.box(3.5, 2, 6.5, 6, 16, 9.5, o2)                           # la columna
    p.box(6, 2, 7, 12, 4, 9, o)                                  # la caja de resonancia (abajo)
    p.box(9, 4, 7, 12, 10, 9, o)
    p.box(11, 10, 7, 13, 16, 9, o)
    for i, x in enumerate((6.6, 7.6, 8.6, 9.6, 10.6)):
        p.box(x, 4, 7.9, x + 0.25, 16, 8.1, cu)                  # cuerdas
    p.at('upper')
    p.box(3.5, 0, 6.5, 6, 8, 9.5, o2)
    p.box(3, 8, 6, 6.5, 11, 10, o)                               # el remate de la columna
    p.box(6, 8, 7, 13, 10, 9, o)                                 # el cuello (arriba)
    p.box(11, 0, 7, 13, 8, 9, o)
    for x in (6.6, 7.6, 8.6, 9.6, 10.6):
        p.box(x, 0, 7.9, x + 0.25, 8, 8.1, cu)
    return p


def violin():
    """Un violín con su arco, colgado de un clavo en la pared."""
    p = Prop('violin', 'Violín', 'Violin', kind='wall', sound='wood', seed=840, collide=False, shape=[[3, 1, 14, 13, 15, 16]])
    m = kit.wood(Ramp('#5a2410', '#7a3416', '#93441c', '#a85424'), axis='v', grain=2.0, seed=840)
    n = solid('#15100c')
    p.box(4.5, 1, 14.2, 11.5, 5.5, 15.6, m)                      # cuerpo de abajo
    p.box(5.5, 5.5, 14.2, 10.5, 7.5, 15.6, m)                    # cintura
    p.box(5, 7.5, 14.2, 11, 11, 15.6, m)                         # cuerpo de arriba
    p.box(7.4, 11, 14.0, 8.6, 15, 15.2, n)                       # mástil
    p.box(7.2, 14.5, 13.8, 8.8, 16, 15.2, m)                     # clavijero
    p.box(7.0, 2, 14.0, 9.0, 3, 14.2, n)                         # cordal
    for x in (7.5, 7.9, 8.3, 8.7):
        p.box(x - 0.05, 3, 14.0, x + 0.05, 14.5, 14.1, solid('#c8c0a8'))
    p.box(5.6, 6.2, 14.1, 6.0, 7.2, 14.2, n)                     # efes
    p.box(10.0, 6.2, 14.1, 10.4, 7.2, 14.2, n)
    p.box(11.8, 1, 14.6, 12.4, 15, 15.2, solid('#3a2410'))       # el arco
    p.box(12.3, 1.5, 14.7, 12.5, 14.5, 15.1, solid('#e0d8c4'))
    return p


def fichas():
    """Torres de fichas de juego (rojas, negras y blancas) y unas cartas regadas."""
    p = Prop('fichas', 'Fichas y cartas', 'Chips and cards', kind='floor', sound='wood', seed=850, collide=False,
             shape=[[1, 0, 1, 15, 6, 15]])
    for (x, z, h, c) in ((3, 3, 5, '#9a1a1a'), (7, 3.5, 3.5, '#151515'), (4, 7.5, 2.5, '#e6e0d0'), (9, 8, 4.5, '#2a5a2a')):
        p.box(x, 0, z, x + 3, h, z + 3, solid(c))
        for y in range(1, int(h), 1):
            p.box(x - 0.02, y, z - 0.02, x + 3.02, y + 0.25, z + 3.02, solid('#d8d0b8'))
    for (x, z, rot) in ((10, 2, 22.5), (2, 11, -22.5), (8.5, 12, 0)):
        p.box(x, 0, z, x + 3.2, 0.2, z + 4.4, solid('#ece6d6'), rot=('y', rot, (x + 1.6, 0, z + 2.2)))
        p.box(x + 1.1, 0.2, z + 1.5, x + 2.1, 0.25, z + 2.9, solid('#a01818'))
    return p


def ruleta():
    """Una rueda de la fortuna de feria colgada en la pared: gajos de colores, clavitos dorados y la flecha arriba."""
    p = Prop('ruleta', 'Rueda de la fortuna', 'Wheel of fortune', kind='wall', sound='wood', seed=860, collide=False,
             shape=[[0, 0, 14, 16, 16, 16]])
    cols = ['#9a2222', '#e2d6b8', '#2a4a7a', '#c8a030']
    for i in range(8):
        y0 = 1 + i * (14 / 8)
        w = 14 - abs(i - 3.5) * 2.6
        x0 = 8 - w / 2
        for j in range(4):
            p.box(x0 + j * w / 4, y0, 14.8, x0 + (j + 1) * w / 4, y0 + 14 / 8, 15.6, solid(cols[(i + j) % 4]))
    p.box(7, 7, 14.4, 9, 9, 14.8, laton(861))                    # el centro
    p.box(7.4, 14.2, 14.2, 8.6, 16, 14.8, laton(862))            # la flecha
    p.box(0.5, 0, 15.6, 15.5, 16, 16, pintado('#3a2414', 863))   # el tablero
    return p


def botella():
    """Una botella de vino tinto a medias y una copa, sobre la mesa o el piso."""
    p = Prop('botella', 'Botella y copa', 'Bottle and glass', kind='floor', sound='glass', seed=870, collide=False,
             shape=[[3, 0, 5, 13, 13, 11]])
    v = kit.metal(Ramp('#0c1a10', '#122818', '#183420', '#20442a'), streak=0.2, seed=870, sparkle=0.1)
    et, cr = solid('#d8ccaa'), solid('#c8d0d4')
    p.box(4, 0, 6, 7.5, 7, 9.5, v)                               # la botella
    p.box(4.8, 7, 6.8, 6.7, 9.5, 8.7, v)
    p.box(5.3, 9.5, 7.3, 6.2, 12.5, 8.2, v)
    p.box(5.2, 12.5, 7.2, 6.3, 13, 8.3, solid('#6a1418'))
    p.box(3.95, 2.5, 5.95, 7.55, 5, 9.55, et)                    # etiqueta
    p.box(9.5, 0, 7, 12.5, 0.4, 10, cr)                          # la copa
    p.box(10.8, 0.4, 8.3, 11.2, 3, 8.7, cr)
    p.box(9.6, 3, 7.1, 12.4, 6, 9.9, cr)
    p.box(9.8, 3.2, 7.3, 12.2, 4.4, 9.7, solid('#5a0c12'))       # el vino
    return p


def todos():
    return [muneca_grande(), llanta(), asiento_bus(), volante(), maleta(),
            engrane(), nota_musical(), soldadito(), bailarina_figura(), cubos_letras(), tambor(), trompo(), globos(), banderines(),
            marioneta(), caja_juguete(), tetera(), pastelero(), arpa(), violin(), fichas(), ruleta(), botella()]
