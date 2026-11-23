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


def all_props():
    return [farol(True), farol(False), vela(), musgo(), argolla(), barril(), placa_presion()]
