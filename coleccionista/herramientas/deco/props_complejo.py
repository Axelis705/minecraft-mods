#!/usr/bin/env python3
"""Muebles y adornos 3D del resto del complejo (Cloacas, Archivo, Salon, Capilla, Puertas, Final). Mismas reglas del kit que
props_celdas.py: el modelo mira al NORTE (z = 0 es el frente); los de pared tienen la espalda en z = 16; los que cuelgan, en y = 16.
"""
import numpy as np

import kit
from kit import Prop, Ramp, fbm, rnd, with_alpha, solid, hexc
from props_celdas import pintado, acero, luz, apagado, madera_vieja, oxido, NEGRO


def farol(on=True):
    """Farol de aceite que cuelga de una cadena (tuneles, salas viejas)."""
    pid = 'farol' if on else 'farol_apagado'
    p = Prop(pid, 'Farol viejo', 'Old lantern', kind='hang', sound='lantern', seed=70, collide=False, light=12,
             particles=[{'type': 'minecraft:smoke', 'pos': [8, 12.5, 8], 'rate': 0.04}] if on else None)
    m = pintado(NEGRO, oxido=0.35, seed=70)
    cad = pintado('#3a3632', oxido=0.5, seed=71)
    for y in (14.5, 12.8):
        p.box(7.6, y, 7.6, 8.4, y + 1.6, 8.4, cad)
    p.box(6, 11, 6, 10, 12, 10, m)                          # tapa
    p.box(6.5, 12, 6.5, 9.5, 12.8, 9.5, m)
    p.box(5.5, 3, 5.5, 10.5, 4, 10.5, m)                    # base
    for a, b in ((5.5, 5.5), (9.8, 5.5), (5.5, 9.8), (9.8, 9.8)):
        p.box(a, 4, b, a + 0.7, 11, b + 0.7, m)
    vid = luz('#ffc874') if on else apagado('#5a4a33')
    p.box(6.4, 4, 6.4, 9.6, 10.8, 9.6, kit.solid('#2a2418', 120) if not on else vid)
    p.box(7.4, 4.5, 7.4, 8.6, 7, 8.6, luz('#fff0c0') if on else apagado('#3c3428'))   # la mecha
    return p


def vela():
    """Velas de cera vieja escurrida, en grupo (de pie)."""
    p = Prop('vela', 'Velas', 'Candles', kind='floor', sound='wool', seed=72, collide=False, light=6,
             particles=[{'type': 'minecraft:small_flame', 'pos': [7, 7.6, 7], 'rate': 0.35}, {'type': 'minecraft:small_flame', 'pos': [10.5, 5.6, 9.5], 'rate': 0.3},
                        {'type': 'minecraft:small_flame', 'pos': [5, 4.6, 10], 'rate': 0.3}])
    cera = kit.stone(Ramp('#b9ae90', '#c8bea2', '#d6cdb2'), scale=2, seed=72)
    mecha = solid('#1d1a16')
    for (x, z, h) in ((7, 7, 7), (10.5, 9.5, 5), (5, 10, 4)):
        p.box(x - 1, 0, z - 1, x + 1, h, z + 1, cera)
        p.box(x - 1.3, 0, z - 1.3, x + 1.3, 0.6, z + 1.3, cera)          # cera escurrida
        p.box(x - 0.2, h, z - 0.2, x + 0.2, h + 0.6, z + 0.2, mecha)
    return p


def musgo():
    """Liquen que brilla tenue en la pared humeda (calcomania de pared con poquita luz)."""
    p = Prop('musgo', 'Liquen', 'Glowing lichen', kind='wall', sound='wool', seed=73, collide=False, light=4, render='cutout')

    def tex(P):
        U, V, W, H = P['U'].astype(np.float32), P['V'].astype(np.float32), float(P['W']), float(P['H'])
        out = np.zeros(U.shape + (4,), np.float32)
        n = fbm(U * 0.9, V * 0.9, 0 * U, 1.0, 73, 3)
        d = np.hypot(U / W - 0.5, (V / H - 0.62) * 1.3)
        m = (n - d * 1.1) > 0.12
        dots = (rnd(U, V, 0 * U, 74) > 0.93) & m
        verde = np.array(hexc('#33452a'), np.float32) * (0.65 + 0.6 * n[..., None])
        out[..., :3] = np.where(dots[..., None], np.array(hexc('#a9c46e'), np.float32), verde)
        out[..., 3] = np.where(m, 230, 0)
        return out
    tex.cutout = True
    p.box(0, 0, 15.9, 16, 16, 15.9, tex, shade=False)
    return p


def argolla():
    """Argolla de hierro clavada en la pared (de aqui sale la cadena de cada pieza en Encadenados)."""
    p = Prop('argolla', 'Argolla de pared', 'Wall ring', kind='wall', sound='chain', seed=74, collide=False)
    m = kit.metal(Ramp('#2d2b28', '#3a3834', '#494641', '#57534c'), seed=74)
    p.box(5.5, 5.5, 15, 10.5, 10.5, 16, m)                  # placa
    p.box(7.5, 7.5, 13.5, 8.5, 8.5, 15, m)                  # perno
    # el aro (cuatro barritas)
    p.box(6, 3, 13, 10, 4, 14, m)
    p.box(6, 3, 13, 7, 8, 14, m)
    p.box(9, 3, 13, 10, 8, 14, m)
    p.box(6, 7, 13, 10, 8, 14, m)
    return p


def barril():
    """Barril de metal oxidado con tapa (se abre como cofre: en Encadenados lleva lo de cada pieza)."""
    p = Prop('barril', 'Barril oxidado', 'Rusty barrel', kind='floor', sound='metal', seed=75)
    m = pintado('#3f4a3c', oxido=0.55, seed=75, saltado=0.3)
    aro = pintado('#2a2b28', oxido=0.5, seed=76)
    p.box(2.5, 0, 2.5, 13.5, 15, 13.5, m)
    p.box(3, 15, 3, 13, 15.6, 13, aro)
    p.box(9.5, 15.6, 9.5, 11.5, 16, 11.5, aro)              # tapon
    for y in (1, 7, 13):
        p.box(2.2, y, 2.2, 13.8, y + 1, 13.8, aro)
    p.box(5, 9, 2.3, 11, 12, 2.5, solid('#b8aa7a'))         # etiqueta vieja
    p.box(5.5, 10.4, 2.2, 10.5, 10.9, 2.3, solid('#4a2a1c'))
    return p


def placa_presion():
    """Placa de piso de acero que se hunde (las compuertas de dos placas)."""
    p = Prop('placa_presion', 'Placa de piso', 'Floor plate', kind='plain', sound='metal', seed=77, collide=False)
    m = pintado('#4b4d47', oxido=0.4, seed=77)
    p.box(1, 0, 1, 15, 1, 15, m)
    raya = solid('#a8862a')
    for i in range(4):
        x0 = 2 + i * 3.4
        p.box(x0, 1, 2, x0 + 1.4, 1.05, 14, raya)
    return p


def atril():
    """Atril de madera con un libro de registro abierto (la mesa de lectura del Archivo)."""
    p = Prop('atril', 'Atril con registro', 'Ledger stand', kind='floor', sound='wood', seed=78)
    m = madera_vieja(78)
    p.box(4, 0, 4, 12, 1, 12, m)
    p.box(6.5, 1, 6.5, 9.5, 10, 9.5, m)
    p.box(2, 10, 3, 14, 11.5, 13, m, rot=('x', -22.5, (8, 10.5, 8)))
    papel = kit.parchment(ink=0.35, seed=79)
    p.box(2.6, 11.5, 3.6, 7.8, 12, 12.4, papel, rot=('x', -22.5, (8, 10.5, 8)))
    p.box(8.2, 11.5, 3.6, 13.4, 12, 12.4, papel, rot=('x', -22.5, (8, 10.5, 8)))
    p.box(7.8, 11.4, 3.4, 8.2, 12.1, 12.6, solid('#3a1c12'), rot=('x', -22.5, (8, 10.5, 8)))
    return p


def cofre_boveda():
    """Caja fuerte vieja de la boveda: acero negro, perilla y bisagras de laton."""
    p = Prop('cofre_boveda', 'Caja fuerte', 'Strongbox', kind='floor', sound='metal', seed=80)
    m = pintado('#262624', oxido=0.25, seed=80, saltado=0.12)
    laton = kit.metal(Ramp('#5e4a22', '#7a6230', '#97793c', '#b59248'), seed=81)
    p.box(1.5, 0, 2.5, 14.5, 12, 14, m, bevel=0.2)
    p.box(2.5, 1, 2, 13.5, 11, 2.5, m)
    p.box(6.5, 4.5, 1.4, 9.5, 7.5, 2, laton)             # perilla
    p.box(7.6, 5.6, 1, 8.4, 6.4, 1.4, solid('#1a1712'))
    for y0 in (2, 8.5):
        p.box(12.6, y0, 1.6, 13.6, y0 + 1.5, 2.2, laton)   # bisagras
    p.box(3.5, 9, 1.8, 6, 10, 2.1, laton)                # placa
    return p




def mesa_tapete():
    """Mesa de juego: cubierta de pano verde con borde de madera y pata central (varias juntas hacen una mesa larga)."""
    p = Prop('mesa_tapete', 'Mesa de juego', 'Card table', kind='floor', sound='wood', seed=82,
             shape=[[0, 13, 0, 16, 16, 16], [6, 0, 6, 10, 13, 10]])
    madera = madera_vieja(82)
    pano = kit.cloth('#1f3a2a', weave=0.12, seed=83)
    p.box(0, 14, 0, 16, 15.2, 16, pano)
    p.box(0, 13, 0, 16, 14, 16, madera)
    p.box(-0.01, 14, -0.01, 16.01, 15.6, 1, madera)
    p.box(-0.01, 14, 15, 16.01, 15.6, 16.01, madera)
    p.box(6.5, 1.5, 6.5, 9.5, 13, 9.5, madera)
    p.box(4, 0, 4, 12, 1.5, 12, madera)
    return p


def sillon():
    """Sillon de terciopelo rojo con respaldo alto (el lugar del perdonado)."""
    p = Prop('sillon', 'Sillón de terciopelo', 'Velvet armchair', kind='floor', sound='wool', seed=84,
             shape=[[1, 0, 1, 15, 8, 15], [1, 8, 12, 15, 22, 15]], tall=True, shape_upper=[[1, 0, 12, 15, 6, 15]])
    tela = kit.cloth('#5a0f14', weave=0.08, seed=84, fold=0.3)
    madera = madera_vieja(85)
    oro = kit.metal(Ramp('#5e4a22', '#7a6230', '#97793c'), seed=86)
    p.at('lower')
    for x0, z0 in ((1.5, 1.5), (13, 1.5), (1.5, 13), (13, 13)):
        p.box(x0, 0, z0, x0 + 1.5, 3, z0 + 1.5, madera)
    p.box(1, 3, 1, 15, 7.5, 15, tela)                     # asiento
    p.box(0.5, 7.5, 1, 3, 12, 14, tela)                   # brazos
    p.box(13, 7.5, 1, 15.5, 12, 14, tela)
    p.box(1, 7.5, 12, 15, 16, 15, tela)                   # respaldo
    p.at('upper')
    p.box(1, 0, 12, 15, 5, 15, tela)
    p.box(2, 5, 12.3, 14, 6.5, 14.7, tela)
    p.box(7, 6.5, 12.6, 9, 7.5, 14.4, oro)
    return p


def estandarte(color='#5a0f14', pid='estandarte_rojo', es='Estandarte rojo', en='Red banner'):
    """Estandarte de tela que cuelga de una barra (dos bloques de alto) con flecos dorados y el emblema de la coleccion (un ojo)."""
    p = Prop(pid, es, en, kind='wall', tall=True, sound='wool', seed=87, collide=False)
    tela = kit.cloth(color, weave=0.1, seed=87, fold=0.5)
    oro = kit.metal(Ramp('#5e4a22', '#7a6230', '#97793c', '#b59248'), seed=88)
    p.at('upper')
    p.box(1, 14, 14, 15, 15, 15, oro)                     # la barra
    p.box(0.5, 13.5, 13.8, 1.5, 15.5, 15.2, oro)
    p.box(14.5, 13.5, 13.8, 15.5, 15.5, 15.2, oro)
    p.box(2, 0, 14.6, 14, 14, 15, tela)
    p.box(6, 5, 14.5, 10, 9, 14.6, oro)                   # el ojo
    p.box(7.3, 6.3, 14.4, 8.7, 7.7, 14.5, solid('#120c0a'))
    p.at('lower')
    p.box(2, 4, 14.6, 14, 16, 15, tela)
    for i in range(6):
        x0 = 2.3 + i * 2
        p.box(x0, 1.5 + (i % 2), 14.65, x0 + 1.2, 4, 14.95, oro)   # flecos
    return p


def arana():
    """Arana de velas (candelabro de techo) de hierro negro con velas: cuelga de su cadena."""
    p = Prop('arana', 'Araña de velas', 'Candle chandelier', kind='hang', sound='metal', seed=89, collide=False, light=13,
             particles=[{'type': 'minecraft:small_flame', 'pos': [2.5, 9.6, 8], 'rate': 0.25}, {'type': 'minecraft:small_flame', 'pos': [13.5, 9.6, 8], 'rate': 0.25},
                        {'type': 'minecraft:small_flame', 'pos': [8, 9.6, 2.5], 'rate': 0.25}, {'type': 'minecraft:small_flame', 'pos': [8, 9.6, 13.5], 'rate': 0.25}])
    m = pintado(NEGRO, oxido=0.2, seed=89)
    cera = kit.stone(Ramp('#b6ad94', '#c6bea5', '#d4ccb3'), scale=2, seed=90)
    p.box(7.5, 9, 7.5, 8.5, 16, 8.5, m)
    p.box(6, 5, 6, 10, 9, 10, m)
    p.box(1.5, 6, 7.5, 14.5, 7, 8.5, m)
    p.box(7.5, 6, 1.5, 8.5, 7, 14.5, m)
    for (x, z) in ((2.5, 8), (13.5, 8), (8, 2.5), (8, 13.5)):
        p.box(x - 1.2, 7, z - 1.2, x + 1.2, 7.6, z + 1.2, m)
        p.box(x - 0.6, 7.6, z - 0.6, x + 0.6, 9.4, z + 0.6, cera)
    p.box(7, 3.5, 7, 9, 5, 9, m)
    return p


def lampara_billar():
    """Lampara de mesa de juego: pantalla de metal verde que cuelga y alumbra hacia abajo."""
    p = Prop('lampara_billar', 'Lámpara de mesa', 'Table lamp', kind='hang', sound='metal', seed=91, collide=False, light=12)
    pant = pintado('#1f3b2c', oxido=0.15, seed=91, saltado=0.1)
    laton = kit.metal(Ramp('#5e4a22', '#7a6230', '#97793c'), seed=92)
    p.box(7.6, 12, 7.6, 8.4, 16, 8.4, laton)
    p.box(5.5, 10, 5.5, 10.5, 12, 10.5, pant)
    p.box(3, 7, 3, 13, 10, 13, pant)
    p.box(4, 6.6, 4, 12, 7, 12, luz('#ffd9a0'))
    return p


def placa_altar():
    """La placa del altar: lamina de laton gastado con el ojo de la coleccion grabado."""
    p = Prop('placa_altar', 'Placa del altar', 'Altar plate', kind='plain', sound='metal', seed=93, collide=False)
    laton = kit.metal(Ramp('#4e3c1a', '#6a5226', '#86692f', '#a3823c'), seed=93)
    p.box(1, 0, 1, 15, 0.8, 15, laton)
    oscuro = solid('#2a1e0e')
    p.box(3, 0.8, 7.3, 13, 0.9, 8.7, oscuro)               # el ojo: almendra
    p.box(4.5, 0.8, 6.3, 11.5, 0.9, 9.7, oscuro)
    p.box(6, 0.8, 5.6, 10, 0.9, 10.4, oscuro)
    p.box(6.8, 0.9, 6.8, 9.2, 0.95, 9.2, solid('#d8cfa8'))  # el blanco
    p.box(7.4, 0.95, 7.4, 8.6, 1.0, 8.6, solid('#8a1c16'))  # la pupila
    return p


def trono():
    """El trono del Coleccionista: respaldo muy alto de terciopelo negro con marco dorado y el ojo arriba."""
    p = Prop('trono', 'Trono del Coleccionista', "The Collector's throne", kind='floor', tall=True, sound='wood', seed=94, collide=False)
    tela = kit.cloth('#1a0c10', weave=0.08, seed=94, fold=0.3)
    roja = kit.cloth('#4a0a10', weave=0.08, seed=95, fold=0.2)
    oro = kit.metal(Ramp('#5e4a22', '#7a6230', '#97793c', '#b59248'), seed=96)
    madera = madera_vieja(97)
    p.at('lower')
    for x0, z0 in ((1, 1), (13.5, 1), (1, 13), (13.5, 13)):
        p.box(x0, 0, z0, x0 + 1.5, 4, z0 + 1.5, oro)
    p.box(0.5, 4, 0.5, 15.5, 8, 15, madera)
    p.box(1, 8, 1, 15, 9.5, 14, roja)                     # cojin
    p.box(0, 8, 1, 2, 13, 14, madera)                     # brazos
    p.box(14, 8, 1, 16, 13, 14, madera)
    p.box(-0.2, 12.5, 0.5, 2.2, 13.5, 14.5, oro)
    p.box(13.8, 12.5, 0.5, 16.2, 13.5, 14.5, oro)
    p.box(1, 8, 13, 15, 16, 16, tela)                     # respaldo
    p.at('upper')
    p.box(1, 0, 13, 15, 13, 16, tela)
    p.box(0.5, 0, 12.6, 1.5, 14, 16, oro)
    p.box(14.5, 0, 12.6, 15.5, 14, 16, oro)
    p.box(0.5, 13, 12.6, 15.5, 14.5, 16, oro)
    p.box(5, 14.5, 13, 11, 16, 15.6, oro)                 # remate
    p.box(5.5, 7, 12.6, 10.5, 10, 12.9, oro)              # el ojo
    p.box(7.2, 7.8, 12.4, 8.8, 9.2, 12.6, solid('#8a1c16'))
    return p


def craneo():
    """Craneo viejo (recuerdo en las vitrinas)."""
    p = Prop('craneo', 'Cráneo', 'Skull', kind='floor', sound='stone', seed=98, collide=False)
    hueso = kit.stone(Ramp('#8f8670', '#a39a82', '#b5ad96', '#c4bca6'), scale=2, seed=98)
    hoyo = solid('#161310')
    p.box(4.5, 0, 4, 11.5, 6, 11, hueso)
    p.box(5, 6, 4.5, 11, 7.5, 11, hueso)
    p.box(5.5, 0, 3.2, 10.5, 2.4, 4, hueso)               # quijada
    p.box(5.4, 3, 3.8, 7.4, 5, 4.05, hoyo)                # ojos
    p.box(8.6, 3, 3.8, 10.6, 5, 4.05, hoyo)
    p.box(7.6, 2, 3.8, 8.4, 3, 4.05, hoyo)
    for i in range(4):
        p.box(6 + i * 1.1, 1.1, 3.1, 6.7 + i * 1.1, 1.9, 3.25, solid('#d8d0b8'))
    return p


def zapatos():
    """Un par de zapatos viejos (lo único que deja Don Resorte), con las agujetas sueltas y manchados de sangre."""
    p = Prop('zapatos', 'Zapatos', 'Shoes', kind='floor', sound='wool', seed=99, collide=False)
    cuero = kit.stone(Ramp('#2a1810', '#3a2216', '#4a2c1c', '#5a3824'), scale=1.5, seed=99)
    suela = solid('#141010')
    sangre = kit.stone(Ramp('#3a0408', '#5a0a10', '#7a1018'), scale=1.2, seed=100)
    agujeta = solid('#c8c0b0')
    for (x0, dz) in ((3.2, 0.0), (9.0, 1.2)):
        p.box(x0, 0, 3 + dz, x0 + 3.6, 0.8, 11.5 + dz, suela)            # la suela
        p.box(x0 + 0.2, 0.8, 3.4 + dz, x0 + 3.4, 2.6, 11.2 + dz, cuero)  # el empeine
        p.box(x0 + 0.3, 2.6, 7.6 + dz, x0 + 3.3, 4.6, 11.2 + dz, cuero)  # el talón (la caña)
        p.box(x0 + 0.8, 2.6, 5.0 + dz, x0 + 2.8, 2.9, 7.4 + dz, agujeta)
        p.box(x0 + 0.4, 4.4, 7.8 + dz, x0 + 3.2, 4.65, 11.0 + dz, sangre)   # sangre en el borde
    p.box(5.5, 0, 1.5, 11.5, 0.15, 6.5, sangre)                                # el charquito
    return p
