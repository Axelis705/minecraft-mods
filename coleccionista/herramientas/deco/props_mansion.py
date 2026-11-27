#!/usr/bin/env python3
"""Muebles y adornos de la mansión del Coleccionista (capítulo 2, gótica victoriana): ropero, reloj de pie, armadura, busto, retratos,
plantas, lápidas, ángel del jardín, banca, lámpara de mesa, globo terráqueo, caballito de madera, cuna, maniquí de costura, máquina de
coser, cabeza de trofeo, telescopio y estatua de cuervo.

Mismas reglas del kit: el modelo mira al NORTE (z = 0 es el frente); los de pared tienen la espalda en z = 16; los altos ponen sus cajas
en dos partes ('lower' y 'upper', cada una de 0 a 16).
"""
import kit
from kit import Prop, Ramp, solid
from props_celdas import luz, madera_vieja


def nogal(seed=1):
    """Madera oscura barnizada (los muebles finos de la casa)."""
    return kit.wood(Ramp('#2a1810', '#35200f', '#432914', '#51331b'), axis='v', grain=2.6, seed=seed)


def oro(seed=2):
    return kit.metal(Ramp('#5e4a22', '#7a6230', '#97793c', '#b59248'), seed=seed)


def hierro(seed=3):
    return kit.metal(Ramp('#26272a', '#33353a', '#42454b', '#55585e'), seed=seed, sparkle=0.02)


def marmol(seed=4):
    return kit.stone(Ramp('#bdb8ae', '#cbc6bc', '#d8d3ca', '#e4e0d8'), scale=3.5, seed=seed, cracks=0.05)


def piedra_vieja(seed=5):
    return kit.stone(Ramp('#4f5050', '#5d5e5c', '#6b6c69', '#787975'), scale=2.5, seed=seed, cracks=0.12)


def tela(color, seed=6, fold=0.3):
    return kit.cloth(color, weave=0.08, seed=seed, fold=fold)


# ---------------------------------------------------------------- muebles de adentro

def ropero():
    """Ropero alto de nogal con copete tallado, dos puertas con espejo ovalado y jaladeras de latón."""
    p = Prop('ropero', 'Ropero antiguo', 'Antique wardrobe', kind='floor', sound='wood', seed=300, tall=True,
             shape=[[0, 0, 3, 16, 16, 16]], shape_upper=[[0, 0, 3, 16, 14, 16]])
    m, o = nogal(300), oro(301)
    esp = kit.metal(Ramp('#5d6a70', '#77868c', '#95a3a8', '#b5c0c3'), seed=302, sparkle=0.1)
    p.at('lower')
    p.box(0.5, 0, 3.5, 15.5, 1.5, 16, m)                      # zócalo
    p.box(0, 1.5, 3, 16, 16, 16, m)                           # cuerpo
    p.box(1.2, 3, 2.6, 7.8, 16, 3, m)                         # puerta izquierda (relieve)
    p.box(8.2, 3, 2.6, 14.8, 16, 3, m)
    p.box(2, 2, 2.5, 14, 2.6, 3, o)                           # moldura de abajo
    p.at('upper')
    p.box(0, 0, 3, 16, 12, 16, m)
    p.box(1.2, 0, 2.6, 7.8, 10, 3, m)
    p.box(8.2, 0, 2.6, 14.8, 10, 3, m)
    p.box(2.5, 1.5, 2.4, 6.5, 8.5, 2.6, esp)                  # espejos
    p.box(9.5, 1.5, 2.4, 13.5, 8.5, 2.6, esp)
    p.box(7, -3, 2.2, 7.6, 1, 2.6, o)                         # jaladeras
    p.box(8.4, -3, 2.2, 9, 1, 2.6, o)
    p.box(-0.3, 11.5, 2.4, 16.3, 12.8, 16, m)                 # cornisa
    p.box(3, 12.8, 2.8, 13, 14, 15, m)                        # copete
    p.box(6.5, 12.8, 2.5, 9.5, 14, 2.9, o)
    return p


def reloj_pie():
    """Reloj de pie: caja alta de nogal, péndulo dorado detrás del vidrio y carátula de marfil con números romanos."""
    p = Prop('reloj_pie', 'Reloj de pie', 'Grandfather clock', kind='floor', sound='wood', seed=303, tall=True,
             shape=[[3, 0, 4, 13, 16, 14]], shape_upper=[[2.5, 0, 3.5, 13.5, 15, 14.5]])
    m, o = nogal(303), oro(304)
    vid = solid('#1a2228', 150)
    mar = kit.parchment(ink=0.25, seed=305)
    negro = solid('#141414')
    p.at('lower')
    p.box(2.5, 0, 3.5, 13.5, 2.5, 14.5, m)
    p.box(3.5, 2.5, 4.5, 12.5, 16, 13.5, m)
    p.box(5, 3.5, 4.2, 11, 15.5, 4.5, vid)                    # ventanita del péndulo
    p.box(7.6, 6, 4.6, 8.4, 16, 5, o)                         # varilla
    p.box(6.4, 5, 4.6, 9.6, 8, 5, o)                          # disco
    p.at('upper')
    p.box(3.5, 0, 4.5, 12.5, 2, 13.5, m)
    p.box(2.5, 2, 3.5, 13.5, 12.5, 14.5, m)                   # cabeza del reloj
    p.box(4, 3.5, 3.2, 12, 11.5, 3.5, mar)                    # carátula
    p.box(7.6, 7.2, 3, 8.4, 10.6, 3.2, negro)                 # manecillas
    p.box(7.8, 6.6, 3, 10.8, 7.4, 3.2, negro)
    p.box(4.5, 11.8, 3.1, 11.5, 12.2, 3.5, o)
    p.box(2, 12.5, 3, 14, 14, 15, m)                          # copete
    p.box(6.5, 14, 6, 9.5, 15, 12, o)
    return p


def armadura():
    """Armadura de caballero en su base, con penacho rojo y una alabarda."""
    p = Prop('armadura', 'Armadura', 'Suit of armor', kind='floor', sound='metal', seed=306, tall=True,
             shape=[[3, 0, 3, 13, 16, 13]], shape_upper=[[3, 0, 4, 13, 14, 12]])
    ac = kit.metal(Ramp('#4a4d52', '#626670', '#7d828c', '#9aa0aa', '#b9bec6'), seed=306, sparkle=0.15)
    base = piedra_vieja(307)
    m = madera_vieja(308)
    pluma = tela('#6a0e14', 309, 0.5)
    negro = solid('#0e0e10')
    p.at('lower')
    p.box(2.5, 0, 2.5, 13.5, 1.5, 13.5, base)
    p.box(5, 1.5, 6, 7.5, 8, 9.5, ac)                         # piernas
    p.box(8.5, 1.5, 6, 11, 8, 9.5, ac)
    p.box(4.8, 1.5, 5.2, 7.7, 3, 9.8, ac)                     # botas
    p.box(8.3, 1.5, 5.2, 11.2, 3, 9.8, ac)
    p.box(4.5, 8, 5.5, 11.5, 11, 10.5, ac)                    # faldón
    p.box(4.5, 11, 5.8, 11.5, 16, 10.2, ac)                   # pecho
    p.box(12.5, 0, 6.8, 13.4, 16, 7.6, m)                     # asta de la alabarda
    p.at('upper')
    p.box(4.2, 0, 5.5, 11.8, 4, 10.5, ac)                     # peto de arriba
    p.box(2.5, 1, 6.5, 4.5, 4, 9.5, ac)                       # hombreras
    p.box(11.5, 1, 6.5, 13.5, 4, 9.5, ac)
    p.box(2.8, -5, 7, 4.3, 1, 9, ac)                          # brazos
    p.box(11.7, -5, 7, 13.2, 1, 9, ac)
    p.box(5.5, 4, 6, 10.5, 9.5, 10.5, ac)                     # yelmo
    p.box(6, 6.5, 5.8, 10, 7.2, 6, negro)                     # la rendija de los ojos
    p.box(7.5, 9.5, 7, 8.5, 13.5, 11.5, pluma)                # penacho
    p.box(12.5, 0, 6.8, 13.4, 7, 7.6, m)
    p.box(11.5, 7, 6.6, 14.5, 10.5, 7.8, ac)                  # la hoja de la alabarda
    p.box(12.7, 10.5, 6.9, 13.2, 13, 7.5, ac)
    return p


def busto():
    """Busto de mármol sobre una columna (un caballero serio de la familia)."""
    p = Prop('busto', 'Busto de mármol', 'Marble bust', kind='floor', sound='stone', seed=310, tall=True,
             shape=[[3, 0, 3, 13, 16, 13]], shape_upper=[[3.5, 0, 4, 12.5, 11, 12]])
    mr = marmol(310)
    vena = kit.stone(Ramp('#8d8880', '#a39e95', '#b8b3a9'), scale=2, seed=311)
    p.at('lower')
    p.box(3, 0, 3, 13, 2, 13, vena)
    p.box(4.5, 2, 4.5, 11.5, 14, 11.5, mr)
    for x in (5, 7, 9, 11):                                   # estrías
        p.box(x - 0.3, 2.5, 4.3, x + 0.3, 13.5, 4.5, vena)
    p.box(3.5, 14, 3.5, 12.5, 16, 12.5, vena)
    p.at('upper')
    p.box(4, 0, 5, 12, 3.5, 11, mr)                           # hombros
    p.box(6.5, 3.5, 6.5, 9.5, 5, 9.5, mr)                     # cuello
    p.box(5.5, 5, 5.5, 10.5, 10.5, 10.5, mr)                  # cabeza
    p.box(7.4, 6.6, 5.1, 8.6, 8.2, 5.5, mr)                   # nariz
    p.box(5.3, 9.5, 5.8, 10.7, 11, 10.7, vena)                # cabello
    return p


def retrato(pid, es, en, fondo, ropa, piel, pelo, seed):
    """Retrato al óleo en marco dorado (de pared): una figura de la familia mirando al frente. Los ojos brillan un poquito."""
    p = Prop(pid, es, en, kind='wall', sound='wood', seed=seed, collide=False)
    o = oro(seed)
    f = tela(fondo, seed + 1, 0.05)
    r = tela(ropa, seed + 2, 0.1)
    s = solid(piel)
    pe = tela(pelo, seed + 3, 0.2)
    ojo = luz('#c9b48a', seed + 4)
    p.box(0.5, 1, 15, 15.5, 15, 16, o)                        # marco
    p.box(1.5, 2, 14.7, 14.5, 14, 15, f)                      # lienzo
    p.box(3.5, 2, 14.6, 12.5, 6.5, 14.7, r)                   # hombros
    p.box(6, 6.5, 14.6, 10, 11, 14.7, s)                      # cara
    p.box(5.5, 10.5, 14.55, 10.5, 12.5, 14.65, pe)            # cabello
    p.box(5.5, 7.5, 14.55, 6.1, 10.5, 14.65, pe)
    p.box(9.9, 7.5, 14.55, 10.5, 10.5, 14.65, pe)
    p.box(6.9, 9, 14.5, 7.6, 9.6, 14.6, ojo)                  # ojos
    p.box(8.4, 9, 14.5, 9.1, 9.6, 14.6, ojo)
    p.box(0, 0.5, 14.8, 16, 1.2, 16, o)                       # la placa de abajo
    return p


def planta():
    """Palma de salón en una urna de piedra con asas."""
    p = Prop('planta', 'Palma en urna', 'Parlour palm', kind='floor', sound='grass', seed=320, tall=True,
             shape=[[3, 0, 3, 13, 16, 13]], shape_upper=[[1, 0, 1, 15, 10, 15]], collide=True)
    u = piedra_vieja(320)
    tierra = solid('#2c2116')
    hoja = kit.cloth('#2f5a26', weave=0.25, seed=321, fold=0.6)
    hoja2 = kit.cloth('#3e7030', weave=0.25, seed=322, fold=0.6)
    tallo = solid('#4a3a22')
    p.at('lower')
    p.box(4, 0, 4, 12, 1.5, 12, u)
    p.box(5.5, 1.5, 5.5, 10.5, 3, 10.5, u)
    p.box(3.5, 3, 3.5, 12.5, 9, 12.5, u)
    p.box(2.5, 6, 7, 3.5, 8, 9, u)                            # asas
    p.box(12.5, 6, 7, 13.5, 8, 9, u)
    p.box(4, 8.8, 4, 12, 9, 12, tierra)
    p.box(7.4, 9, 7.4, 8.6, 16, 8.6, tallo)
    p.box(4, 13, 7.2, 12, 13.6, 8.8, hoja)
    p.box(7.2, 14, 4, 8.8, 14.6, 12, hoja2)
    p.at('upper')
    p.box(7.5, 0, 7.5, 8.5, 6, 8.5, tallo)
    for (x0, z0, x1, z1, y, h) in ((1, 7, 8, 9, 3, hoja), (8, 7, 15, 9, 4, hoja2), (7, 1, 9, 8, 5, hoja2), (7, 8, 9, 15, 2, hoja),
                                   (2, 2, 7, 7, 6, hoja), (9, 9, 14, 14, 6, hoja2), (9, 2, 14, 7, 7, hoja), (2, 9, 7, 14, 5, hoja2)):
        p.box(x0, y, z0, x1, y + 0.6, z1, h)
    p.box(5.5, 6, 5.5, 10.5, 8, 10.5, hoja)
    return p


def lampara_mesa():
    """Lámpara de mesa con pantalla de tela roja con flecos (va encima de una mesita o un buró)."""
    p = Prop('lampara_mesa', 'Lámpara de mesa', 'Table lamp', kind='floor', sound='lantern', seed=325, collide=False, light=11)
    o = oro(325)
    pant = kit.glow(Ramp('#6a1c14', '#8a2a1a', '#a83c20'), seed=326) if hasattr(kit, 'glow') else luz('#a83c20')
    fleco = solid('#c9a24a')
    p.box(5, 0, 5, 11, 1, 11, o)
    p.box(7.4, 1, 7.4, 8.6, 8, 8.6, o)
    p.box(6.5, 3, 6.5, 9.5, 5, 9.5, o)
    p.box(4, 8, 4, 12, 13, 12, pant)
    p.box(4.5, 13, 4.5, 11.5, 13.6, 11.5, pant)
    p.box(3.8, 7.4, 3.8, 12.2, 8, 12.2, fleco)
    p.box(6.8, 6.8, 6.8, 9.2, 8.6, 9.2, luz('#ffe2a0'))
    return p


def globo():
    """Globo terráqueo antiguo en su pie de madera, con meridiano de latón."""
    p = Prop('globo', 'Globo terráqueo', 'Old globe', kind='floor', sound='wood', seed=330)
    m = nogal(330)
    o = oro(331)
    mar = kit.stone(Ramp('#3a5a5e', '#46686a', '#557874'), scale=2, seed=332)
    tierra = kit.stone(Ramp('#8a7a4a', '#a08c56', '#b39c62'), scale=1.5, seed=333)
    p.box(4.5, 0, 4.5, 11.5, 1, 11.5, m)
    p.box(7.3, 1, 7.3, 8.7, 4, 8.7, m)
    p.box(4, 4, 4, 12, 12, 12, mar)
    p.box(4.6, 6, 3.8, 8, 10, 5, tierra)
    p.box(9, 5, 8, 12.2, 8.5, 11, tierra)
    p.box(5, 9, 9, 8, 11, 12.2, tierra)
    p.box(7.6, 3.5, 3.4, 8.4, 12.6, 4, o)
    p.box(7.6, 12, 4, 8.4, 12.6, 12, o)
    p.box(7.6, 3.5, 12, 8.4, 12.6, 12.6, o)
    return p


def caballito():
    """Caballito de madera mecedor, pintado de blanco con crin negra y silla roja (ya despintado)."""
    p = Prop('caballito', 'Caballito mecedor', 'Rocking horse', kind='floor', sound='wood', seed=335)
    blanco = kit.stone(Ramp('#bdb4a2', '#cfc6b3', '#ddd5c4'), scale=2, seed=335, cracks=0.1)
    negro = tela('#1a1614', 336, 0.4)
    rojo = tela('#7a1a1a', 337, 0.2)
    m = madera_vieja(338)
    ojo = solid('#0a0a0a')
    p.box(1, 0, 5.5, 15, 1, 6.5, m)                           # balancines
    p.box(1, 0, 9.5, 15, 1, 10.5, m)
    p.box(0.5, 1, 5.5, 1.5, 2, 10.5, m)
    p.box(14.5, 1, 5.5, 15.5, 2, 10.5, m)
    for (x, z) in ((3, 6.2), (12, 6.2), (3, 8.6), (12, 8.6)):
        p.box(x, 1, z, x + 1.2, 6, z + 1.2, blanco)
    p.box(2.5, 6, 6, 13.5, 9.5, 10, blanco)                   # cuerpo
    p.box(5.5, 9.5, 6.2, 9.5, 10.2, 9.8, rojo)                # silla
    p.box(1.5, 8.5, 6.8, 4, 13.5, 9.2, blanco)                # cuello
    p.box(0, 11.5, 6.6, 3.5, 14.5, 9.4, blanco)               # cabeza
    p.box(3, 10, 7.6, 4.2, 15, 8.4, negro)                    # crin
    p.box(13.5, 6.5, 7.4, 15, 9.5, 8.6, negro)                # cola
    p.box(0.6, 13, 6.4, 1.4, 13.8, 6.6, ojo)
    p.box(0.6, 13, 9.4, 1.4, 13.8, 9.6, ojo)
    return p


def cuna():
    """Cuna de madera con barrotes y una cobijita (vacía... o casi)."""
    p = Prop('cuna', 'Cuna antigua', 'Antique cradle', kind='floor', sound='wood', seed=340)
    m = nogal(340)
    cob = tela('#b9a6a0', 341, 0.5)
    p.box(1, 0, 3, 15, 1, 4, m)                               # balancín
    p.box(1, 0, 12, 15, 1, 13, m)
    p.box(1.5, 1, 3.5, 14.5, 3, 12.5, m)
    p.box(2, 3, 4, 14, 4, 12, cob)
    for x in (1.5, 4, 6.5, 9, 11.5, 14):
        p.box(x, 3, 3.5, x + 0.6, 9, 4.1, m)
        p.box(x, 3, 11.9, x + 0.6, 9, 12.5, m)
    p.box(1.5, 9, 3.5, 14.6, 10, 4.1, m)
    p.box(1.5, 9, 11.9, 14.6, 10, 12.5, m)
    p.box(1.5, 3, 4, 2.1, 12, 12, m)                          # cabecera alta
    p.box(14, 3, 4, 14.6, 9.5, 12, m)
    return p


def maniqui():
    """Maniquí de costura con un vestido a medio hacer y alfileres."""
    p = Prop('maniqui', 'Maniquí de costura', "Dressmaker's dummy", kind='floor', sound='wool', seed=345, tall=True,
             shape=[[4, 0, 4, 12, 16, 12]], shape_upper=[[4.5, 0, 5, 11.5, 8, 11]])
    m = nogal(345)
    tela_v = tela('#3b1c2e', 346, 0.6)
    beige = tela('#b9a58a', 347, 0.2)
    alf = solid('#d8d8d8')
    p.at('lower')
    p.box(4, 0, 4, 12, 0.8, 12, m)
    p.box(7.4, 0.8, 7.4, 8.6, 6, 8.6, m)
    p.box(4, 6, 4, 12, 16, 12, tela_v)                        # falda
    p.at('upper')
    p.box(5, 0, 5.5, 11, 6, 10.5, beige)                      # torso
    p.box(4.5, 3.5, 5.8, 11.5, 6.5, 10.2, beige)
    p.box(7.2, 6, 7.2, 8.8, 7.5, 8.8, m)                      # el pomo de arriba
    for (x, y) in ((6, 2), (9.5, 3.5), (7.5, 4.8)):
        p.box(x, y, 5.2, x + 0.4, y + 0.4, 5.6, alf)
    return p


def maquina_coser():
    """Máquina de coser negra con adornos dorados sobre su mesa de hierro con pedal."""
    p = Prop('maquina_coser', 'Máquina de coser', 'Sewing machine', kind='floor', sound='metal', seed=350)
    h = hierro(350)
    m = nogal(351)
    o = oro(352)
    n = solid('#121214')
    p.box(1, 0, 3, 2, 9, 13, h)                               # patas de hierro
    p.box(14, 0, 3, 15, 9, 13, h)
    p.box(2, 1, 6, 14, 2, 10, h)                              # pedal
    p.box(0.5, 9, 2.5, 15.5, 10, 13.5, m)                     # tablero
    p.box(3, 10, 6, 12, 11, 10, n)                            # base de la máquina
    p.box(9, 11, 6.5, 11.5, 15, 9.5, n)                       # columna
    p.box(3.5, 14, 7, 11.5, 15.5, 9, n)                       # brazo
    p.box(4, 11, 7.5, 5, 14, 8.5, n)                          # aguja
    p.box(4, 14.2, 6.9, 11, 14.6, 7.1, o)                     # filete dorado
    p.box(11.6, 12, 6, 12.4, 14, 10, o)                       # volante
    return p


def cabeza_trofeo():
    """Cabeza de ciervo disecada en su escudo de madera (de pared), con cornamenta grande."""
    p = Prop('cabeza_trofeo', 'Cabeza de ciervo', 'Mounted stag head', kind='wall', sound='wood', seed=355, collide=False)
    m = nogal(355)
    pelo = tela('#5a3e26', 356, 0.3)
    pelo2 = tela('#3e2a1a', 357, 0.3)
    hueso = kit.stone(Ramp('#b8a98a', '#c9bb9c', '#d8cbae'), scale=2, seed=358)
    ojo = luz('#3a2a10', 359)
    p.box(4, 3, 15, 12, 11, 16, m)                            # escudo
    p.box(6, 4.5, 11, 10, 9.5, 15, pelo)                      # cuello
    p.box(6.2, 6, 6.5, 9.8, 9.5, 11, pelo)                    # cabeza
    p.box(6.8, 5.5, 4, 9.2, 7.6, 6.5, pelo2)                  # hocico
    p.box(7.2, 6.4, 3.8, 8.8, 7.2, 4.1, solid('#141010'))     # nariz
    p.box(6, 8.2, 8, 6.3, 9, 9, ojo)
    p.box(9.7, 8.2, 8, 10, 9, 9, ojo)
    for s in (-1, 1):                                         # cornamenta: un tallo, una rama de lado y dos puntas
        x = 8 + s * 2.2
        xt = x + s * 3
        p.box(x - 0.4, 9.5, 9, x + 0.4, 12.5, 9.8, hueso)
        p.box(min(x, xt) - 0.4, 12, 9, max(x, xt) + 0.4, 12.8, 9.8, hueso)
        p.box(xt - 0.4, 12, 9, xt + 0.4, 15.5, 9.8, hueso)
        p.box(x + s * 1.4 - 0.4, 12.5, 9, x + s * 1.4 + 0.4, 15, 9.8, hueso)
    return p


def telescopio():
    """Telescopio de latón en su trípode, apuntando al cielo (el observatorio de la torre)."""
    p = Prop('telescopio', 'Telescopio', 'Telescope', kind='floor', sound='metal', seed=360, tall=True,
             shape=[[3, 0, 3, 13, 16, 13]], shape_upper=[[5, 0, 2, 11, 10, 12]])
    m = nogal(360)
    o = oro(361)
    vid = solid('#202830')
    p.at('lower')
    for (x0, z0, x1, z1) in ((7.4, 7.4, 8.6, 8.6), (3, 4, 4.2, 5.2), (11.8, 4, 13, 5.2), (7.4, 12, 8.6, 13.2)):
        p.box(x0, 0, z0, x1, 1, z1, m)
    p.box(3.5, 1, 4.5, 4.5, 10, 5.5, m, rot=('x', -22.5, (4, 10, 5)))
    p.box(11.5, 1, 4.5, 12.5, 10, 5.5, m, rot=('x', -22.5, (12, 10, 5)))
    p.box(7.5, 1, 11.5, 8.5, 10, 12.5, m, rot=('x', 22.5, (8, 10, 12)))
    p.box(7, 10, 7, 9, 16, 9, m)
    p.at('upper')
    p.box(6.5, 0, 6.5, 9.5, 2, 9.5, o)
    p.box(6.5, 2, 2, 9.5, 5, 14, o, rot=('x', -22.5, (8, 3.5, 8)))
    p.box(7, 2.5, 0.5, 9, 4.5, 2.5, o, rot=('x', -22.5, (8, 3.5, 8)))
    p.box(7.2, 2.7, 0.3, 8.8, 4.3, 0.6, vid, rot=('x', -22.5, (8, 3.5, 8)))
    return p


# ---------------------------------------------------------------- jardín

def lapida(cruz=False):
    """Lápida de piedra vieja, con musgo (redonda o con cruz)."""
    pid = 'lapida_cruz' if cruz else 'lapida'
    p = Prop(pid, 'Lápida con cruz' if cruz else 'Lápida', 'Cross gravestone' if cruz else 'Gravestone', kind='floor', sound='stone',
             seed=370 + cruz)
    s = piedra_vieja(370 + cruz)
    musgo = kit.cloth('#3a4a24', weave=0.4, seed=372)
    tierra = solid('#2a2016')
    p.box(2, 0, 7, 14, 1, 15, tierra)                         # la tumba (tierra removida)
    if cruz:
        p.box(7, 1, 3, 9, 15, 5, s)
        p.box(3.5, 9.5, 3, 12.5, 11.5, 5, s)
        p.box(5.5, 0, 2.5, 10.5, 2, 5.5, s)
    else:
        p.box(3, 0, 3, 13, 11, 5, s)
        p.box(4, 11, 3, 12, 12.5, 5, s)
        p.box(5.5, 12.5, 3, 10.5, 13.3, 5, s)
        p.box(4.5, 6, 2.8, 11.5, 9, 3, solid('#3c3d3b'))     # la inscripción gastada
    p.box(3, 0, 4.9, 6, 3, 5.2, musgo)
    return p


def angel():
    """Ángel de piedra llorando, con las alas recogidas (estatua del jardín)."""
    p = Prop('angel', 'Ángel de piedra', 'Weeping angel statue', kind='floor', sound='stone', seed=375, tall=True,
             shape=[[2, 0, 2, 14, 16, 14]], shape_upper=[[1, 0, 4, 15, 15, 13]])
    s = kit.stone(Ramp('#6e706c', '#80827d', '#91938d', '#a2a49e'), scale=2.5, seed=375, cracks=0.15)
    musgo = kit.cloth('#3a4a24', weave=0.4, seed=376)
    p.at('lower')
    p.box(2, 0, 2, 14, 3, 14, s)                              # pedestal
    p.box(3, 3, 3, 13, 4, 13, s)
    p.box(4.5, 4, 5, 11.5, 16, 11, s)                         # la túnica
    p.box(4, 4, 4.5, 12, 6, 11.5, s)
    p.box(2, 0, 2, 6, 2.6, 2.2, musgo)
    p.at('upper')
    p.box(5, 0, 5.5, 11, 6, 10.5, s)                          # torso
    p.box(4.5, 3, 4.5, 11.5, 5, 6.5, s)                       # brazos tapando la cara
    p.box(6, 6, 6, 10, 10, 10, s)                             # cabeza (agachada)
    p.box(5.5, 9, 6, 10.5, 10.5, 10.5, s)
    p.box(1, 2, 9, 5, 14, 12, s)                              # alas
    p.box(11, 2, 9, 15, 14, 12, s)
    p.box(2, 11, 9.5, 5.5, 15, 11.5, s)
    p.box(10.5, 11, 9.5, 14, 15, 11.5, s)
    return p


def banca():
    """Banca de jardín de hierro forjado y tablas."""
    p = Prop('banca', 'Banca de jardín', 'Garden bench', kind='floor', sound='metal', seed=380,
             shape=[[0, 0, 4, 16, 7, 13], [0, 7, 11.5, 16, 14, 13]])
    h = hierro(380)
    m = madera_vieja(381)
    for x in (0.5, 14.5):
        p.box(x, 0, 4, x + 1, 7, 5, h)
        p.box(x, 0, 11.5, x + 1, 14, 12.5, h)
        p.box(x, 6, 4, x + 1, 7, 12.5, h)
        p.box(x - 0.2, 7, 4.5, x + 1.2, 8, 9, h)              # brazos
    for z in (5, 7, 9):
        p.box(0, 6, z, 16, 6.8, z + 1.6, m)
    for y in (9, 11, 13):
        p.box(0, y, 12, 16, y + 1.2, 12.6, m)
    return p


def cuervo():
    """Estatua de cuervo negro sobre un pilar corto (los cuervos del Coleccionista)."""
    p = Prop('cuervo', 'Cuervo de hierro', 'Iron raven', kind='floor', sound='metal', seed=385)
    h = hierro(385)
    ojo = luz('#c81a1a', 386)
    p.box(4, 0, 4, 12, 6, 12, piedra_vieja(387))
    p.box(5.5, 6, 5.5, 10.5, 10, 11.5, h)                     # cuerpo
    p.box(6, 9, 3.5, 10, 12.5, 7, h)                          # cabeza
    p.box(7.2, 10, 1.5, 8.8, 11, 3.5, h)                      # pico
    p.box(6.5, 11, 3.3, 7.1, 11.6, 3.6, ojo)
    p.box(8.9, 11, 3.3, 9.5, 11.6, 3.6, ojo)
    p.box(6, 7, 11.5, 10, 8, 15, h)                           # cola
    p.box(4.5, 7, 6, 5.5, 10, 11, h)                          # alas
    p.box(10.5, 7, 6, 11.5, 10, 11, h)
    return p


def brasas():
    """Leños con brasas para las chimeneas: arden bajito, con chispas y humo (no queman)."""
    p = Prop('brasas', 'Leños con brasas', 'Smouldering logs', kind='floor', sound='wood', seed=410, collide=False, light=12,
             particles=[{'type': 'minecraft:small_flame', 'pos': [6, 5, 8], 'rate': 0.5}, {'type': 'minecraft:small_flame', 'pos': [10, 4.5, 7], 'rate': 0.45},
                        {'type': 'minecraft:flame', 'pos': [8, 4, 9], 'rate': 0.12}, {'type': 'minecraft:smoke', 'pos': [8, 8, 8], 'rate': 0.18}])
    lena = kit.wood(Ramp('#1e140c', '#2a1c10', '#3a2716', '#4a321c'), axis='h', grain=2.0, seed=410)
    brasa = kit.glow(Ramp('#7a1c06', '#b8360a', '#e86a14', '#ffb040'), seed=411)
    ceniza = kit.stone(Ramp('#2a2826', '#3a3734', '#4a4642'), scale=1.5, seed=412)
    hierro_ = hierro(413)
    p.box(2, 0, 3, 14, 0.6, 13, ceniza)
    p.box(3, 0.6, 4, 13, 1.6, 12, brasa)
    # el morillo de hierro
    p.box(2.5, 0, 4.5, 3.5, 4, 5.5, hierro_)
    p.box(12.5, 0, 4.5, 13.5, 4, 5.5, hierro_)
    p.box(2.5, 1.5, 4.5, 13.5, 2.3, 5.5, hierro_)
    # leños cruzados
    p.box(3, 1.2, 6, 13, 3.6, 8.4, lena)
    p.box(4, 1.2, 9, 12.5, 3.2, 11.2, lena)
    p.box(6.5, 3.2, 5, 9.5, 5.8, 11.5, lena)
    p.box(5, 3.4, 8.2, 11, 4.2, 8.8, brasa)
    p.box(7, 5.6, 7.5, 9, 6.2, 9, brasa)
    return p


def rosal():
    """Rosal seco del jardín: varas negras con espinas, hojas muertas y unas rosas rojo oscuro marchitas (no estorba al pasar)."""
    p = Prop('rosal', 'Rosal seco', 'Withered rosebush', kind='floor', sound='wool', seed=415, collide=False)
    vara = kit.wood(Ramp('#16110c', '#201810', '#2a2016'), axis='v', grain=1.5, seed=415)
    hoja = kit.cloth('#2c2a16', weave=0.1, seed=416, fold=0.4)
    rosa = kit.cloth('#4a0c12', weave=0.06, seed=417, fold=0.5)
    for (x, z, h) in ((5, 6, 11), (9, 5, 13), (11, 10, 10), (6, 11, 9), (8, 8, 14)):
        p.box(x - 0.5, 0, z - 0.5, x + 0.5, h, z + 0.5, vara)
        p.box(x - 2, h - 4, z - 0.4, x + 2, h - 3.2, z + 0.4, vara)
        p.box(x - 1.6, h - 2.5, z - 1.6, x + 1.6, h - 1, z + 1.6, hoja)
    for (x, y, z) in ((9, 13, 5), (5, 11, 6), (11, 10, 10), (8, 14, 8)):
        p.box(x - 1.3, y - 0.6, z - 1.3, x + 1.3, y + 1.6, z + 1.3, rosa)
    return p


def cama_antigua():
    """Cama antigua de nogal con cabecera alta tallada, colcha roja de terciopelo y almohadas (dos bloques, como el catre)."""
    p = Prop('cama_antigua', 'Cama antigua', 'Antique bed', kind='cama', sound='wood', seed=420)
    m, o = nogal(420), oro(421)
    colcha = tela('#5a1016', seed=422, fold=0.35)
    sab = tela('#b8ad94', seed=423, fold=0.2)
    p.at('head')
    p.box(0, 0, 0, 16, 15.5, 2.2, m)                          # cabecera
    p.box(1.5, 4, -0.2, 14.5, 13.5, 0.2, m)                   # tablero tallado (relieve)
    p.box(0, 15.5, 0.4, 16, 16, 1.8, o)                       # moldura dorada
    for x0 in (0, 14):
        p.box(x0, 0, 0, x0 + 2, 16, 2.4, m)                   # postes
    p.box(0.5, 3, 2.2, 15.5, 5, 16, m)                        # bastidor
    p.box(1, 5, 2.2, 15, 8, 16, sab)                          # colchón
    p.box(2, 8, 3, 7.4, 10, 7.5, sab)                         # almohadas
    p.box(8.6, 8, 3, 14, 10, 7.5, sab)
    p.box(0.6, 7.2, 8, 15.4, 8.6, 16, colcha)                 # colcha (empieza abajo de las almohadas)
    p.box(0.3, 4, 8, 0.6, 8.6, 16, colcha)
    p.box(15.4, 4, 8, 15.7, 8.6, 16, colcha)
    p.at('foot')
    p.box(0.5, 3, 0, 15.5, 5, 14, m)
    p.box(1, 5, 0, 15, 8, 14, sab)
    p.box(0.6, 7.2, 0, 15.4, 8.6, 14, colcha)
    p.box(0.3, 3.6, 0, 0.6, 8.6, 14, colcha)
    p.box(15.4, 3.6, 0, 15.7, 8.6, 14, colcha)
    p.box(0, 0, 14, 16, 9, 16, m)                             # piecera
    for x0 in (0, 14):
        p.box(x0, 0, 13.6, x0 + 2, 11, 16, m)
        p.box(x0 + 0.3, 11, 13.9, x0 + 1.7, 12, 15.7, o)      # perillas
    return p


def tina():
    """Media tina de patas (se ponen dos, una frente a la otra, para la tina completa): porcelana sucia con borde redondo."""
    p = Prop('tina', 'Tina de patas', 'Clawfoot bathtub', kind='floor', sound='metal', seed=425,
             shape=[[0.5, 0, 0.5, 15.5, 10, 16]])
    por = kit.stone(Ramp('#a8a292', '#b8b2a2', '#c6c0b0', '#d2ccbc'), scale=2.5, seed=425, cracks=0.04)
    adentro = kit.stone(Ramp('#7a7464', '#8a8474', '#9a9484'), scale=2, seed=426)
    o = oro(427)
    agua = kit.stone(Ramp('#2a3430', '#323e38', '#3a463f'), scale=2, seed=428)
    p.box(1, 2.5, 1.5, 15, 10, 16, por, skip=('up', 'south'))  # el cuerpo (abierto atrás: ahí sigue la otra mitad)
    p.box(2, 2.5, 0.5, 14, 10, 1.5, por)                      # el extremo redondo
    p.box(0.5, 9.4, 1, 15.5, 10.4, 16, por, skip=('south',))  # el borde
    p.box(2.4, 3, 2.4, 13.6, 9.4, 16, adentro, skip=('south',))
    p.box(2.6, 6, 2.6, 13.4, 6.4, 16, agua, skip=('south',))  # agua turbia
    for x0 in (1.5, 12.5):
        p.box(x0, 0, 3, x0 + 2, 2.6, 5.5, o)                  # patas de garra
    p.box(7.2, 10, 0.2, 8.8, 13, 1.8, o)                      # la llave
    return p


def todos():
    return [ropero(), reloj_pie(), armadura(), busto(),
            retrato('retrato_dama', 'Retrato de una dama', 'Portrait of a lady', '#1e2a22', '#3a1424', '#c8a890', '#2a1a10', 390),
            retrato('retrato_caballero', 'Retrato de un caballero', 'Portrait of a gentleman', '#2a1e14', '#16161a', '#c4a088', '#3a3a3a', 395),
            retrato('retrato_nina', 'Retrato de una niña', 'Portrait of a girl', '#28203a', '#5a6a8a', '#dcc0a8', '#a07a40', 400),
            planta(), lampara_mesa(), globo(), caballito(), cuna(), maniqui(), maquina_coser(), cabeza_trofeo(), telescopio(),
            lapida(False), lapida(True), angel(), banca(), cuervo(), brasas(), rosal(), tina()]
