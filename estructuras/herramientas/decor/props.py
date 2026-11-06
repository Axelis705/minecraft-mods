"""Catalogo de decoracion de las estructuras. Cada funcion devuelve un Prop (ver kit.py). Orientacion de los modelos: el FRENTE mira al NORTE (-z);
los de pared tienen la espalda pegada a z = 16; los colgantes cuelgan de y = 16."""
from kit import *  # noqa: F401,F403

# ---------------------------------------------------------------- paletas
BRASS = Ramp('#4a3210', '#7a5719', '#a77a26', '#d1a43e', '#efd070')
IRON = Ramp('#1c1c24', '#2c2c38', '#3f3f4e', '#585869', '#7a7a8c')
DWOOD = Ramp('#1e130b', '#2d1d10', '#3f2a17', '#553822', '#6b4a2e')
OAK = Ramp('#4a3320', '#614329', '#7a5733', '#94703f', '#ad8a50')
WAX = Ramp('#cfc6a4', '#e0d8b8', '#eee7cc', '#f7f2de')
SLATE = Ramp('#25252d', '#30303a', '#3c3c48', '#4a4a58', '#5b5b6b')
PURPLE = Ramp('#2a1346', '#3d1c63', '#552a85', '#7440b0', '#9a63d6')
AMETHYST = Ramp('#3a1a66', '#5a2ea0', '#7b4fd0', '#a37af0', '#d3b8ff')
SOUL = Ramp('#0f5d6b', '#17909c', '#2fc4cc', '#7ae8ec', '#d6ffff')
FIRE = Ramp('#7a1d08', '#b8400f', '#e8741c', '#ffb02e', '#ffe58a')
GREEN_GLOW = Ramp('#0e5a2a', '#1b8c3c', '#32c25a', '#7ae88c', '#d2ffd9')


def candelabro():
    p = Prop('candelabro', 'Candelabro de pie', 'Standing candelabra', 'floor', light=14,
             shape=[(3, 0, 3, 13, 2, 13), (6, 2, 6, 10, 16, 10), (1, 8, 6.5, 15, 16, 9.5)],
             particles=[{'type': 'flame', 'pos': [8, 16.3, 8], 'rate': 0.35}, {'type': 'flame', 'pos': [2.5, 15.3, 8], 'rate': 0.25},
                        {'type': 'flame', 'pos': [13.5, 15.3, 8], 'rate': 0.25}, {'type': 'small_flame', 'pos': [8, 16.5, 8], 'rate': 0.2}])
    br = metal(BRASS, 0.55, 3, 0.07)
    p.box(3, 0, 3, 13, 1, 13, br, bevel=.15, skip=['down'])
    p.box(4, 1, 4, 12, 2, 12, br, bevel=.15)
    p.box(5.5, 2, 5.5, 10.5, 3, 10.5, br, bevel=.15)
    p.box(7, 3, 7, 9, 11, 9, br, bevel=.12)
    p.box(6, 4.5, 6, 10, 6, 10, br, bevel=.15)
    p.box(6.5, 7.5, 6.5, 9.5, 8.5, 9.5, br, bevel=.15)
    p.box(2, 8.5, 7.25, 14, 9.5, 8.75, br, bevel=.12)
    for cx in (2.5, 13.5):
        p.box(cx - 1.5, 9.5, 6.5, cx + 1.5, 10.5, 9.5, br, bevel=.15)
    p.box(6, 11, 6, 10, 12, 10, br, bevel=.15)
    wax = cloth('#efe7cb', 0.0, 5)
    p.box(7, 12, 7, 9, 14.5, 9, wax)
    for cx in (2.5, 13.5):
        p.box(cx - 0.75, 10.5, 7.25, cx + 0.75, 13, 8.75, wax)
    fl = flame()
    for (cx, y0, h) in ((8, 14.5, 2.2), (2.5, 13, 2.0), (13.5, 13, 2.0)):
        p.box(cx - 1.2, y0, 8, cx + 1.2, y0 + h, 8, fl)
        p.box(cx, y0, 8 - 1.2, cx, y0 + h, 8 + 1.2, fl)
    return p


def all_props():
    return [candelabro()]
