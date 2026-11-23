#!/usr/bin/env python3
"""Muebles y adornos 3D del Ala de celdas (y de uso general en el complejo).

Convencion del kit: el modelo mira al NORTE (z = 0 es el frente). Los de pared ('wall') tienen la espalda en z = 16.
Los que cuelgan ('hang') se pegan al techo (y = 16).
"""
import math

import numpy as np

import kit
from kit import Box, Prop, Ramp, fbm, rnd, with_alpha, solid, hexc

# ---------------------------------------------------------------- materiales propios


def pintado(color, oxido=0.35, seed=1, saltado=0.25):
    """Metal pintado viejo: la pintura se salta (se ve el acero) y sale oxido."""
    c = np.array(hexc(color), np.float32)
    steel = Ramp('#3b3c3c', '#4a4b4b', '#585958', '#666765')
    rust = Ramp('#4a2c1a', '#5a3520', '#6b4026', '#7a4a2c')

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        n = fbm(X, Y, Z, 3.0, P['seed'] + seed, 2)
        rgb = c * (0.82 + 0.3 * n)[..., None]
        rgb = rgb * (0.94 + 0.1 * rnd(X, Y, Z, seed))[..., None]
        chip = fbm(X, Y, Z, 1.6, P['seed'] + seed + 5, 2) > 1 - saltado
        rgb = np.where(chip[..., None], steel(rnd(X, Y, Z, seed + 2)), rgb)
        r = fbm(X, Y, Z, 2.2, P['seed'] + seed + 9, 2) > 1 - oxido
        rgb = np.where(r[..., None], rust(rnd(X, Y, Z, seed + 3)) * 0.7 + rgb * 0.3, rgb)
        return with_alpha(rgb)
    return f


def acero(seed=2, sucio=0.3):
    """Acero inoxidable sucio (retrete, lavabo)."""
    r = Ramp('#6d706f', '#7c7f7e', '#8b8e8c', '#9a9d9a', '#a9aba8')

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        t = fbm(X * 0.4 + Z * 0.4, Y * 1.4, 0 * X, 2.0, P['seed'] + seed, 2) * 0.7 + rnd(X, Y, Z, seed) * 0.3
        rgb = r(t)
        d = fbm(X, Y, Z, 3.0, P['seed'] + seed + 4, 2)
        rgb = np.where((d > 1 - sucio)[..., None], rgb * 0.62 + np.array([20, 16, 8], np.float32), rgb)
        return with_alpha(rgb)
    return f


def porcelana(seed=4, sucio=0.35):
    r = Ramp('#b9b6ab', '#c8c5ba', '#d6d3c8', '#e2dfd4')

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        t = fbm(X, Y, Z, 3.0, P['seed'] + seed, 2) * 0.6 + rnd(X, Y, Z, seed) * 0.4
        rgb = r(t)
        d = fbm(X, Y, Z, 2.5, P['seed'] + seed + 4, 2)
        rgb = np.where((d > 1 - sucio)[..., None], rgb * 0.7 + np.array([30, 24, 6], np.float32), rgb)
        crack = np.abs(fbm(X, Y, Z, 2.0, P['seed'] + seed + 7, 2) - 0.5) < 0.02
        rgb = np.where(crack[..., None], rgb * 0.45, rgb)
        return with_alpha(rgb)
    return f


def colchon(seed=6):
    """Colchon delgado de rayas (tela de cotin) con manchas viejas."""
    a, b = np.array(hexc('#8d8f88'), np.float32), np.array(hexc('#5d6670'), np.float32)

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        stripe = (np.floor(X * 1.0) % 3 == 0)
        rgb = np.where(stripe[..., None], b, a) * (0.9 + 0.15 * rnd(X, Y, Z, seed))[..., None]
        st = fbm(X, Y, Z, 3.5, P['seed'] + seed, 2)
        rgb = np.where((st > 0.62)[..., None], rgb * 0.68 + np.array([40, 30, 8], np.float32), rgb)
        return with_alpha(rgb)
    return f


def tela(color, seed=7, sucio=0.3):
    c = np.array(hexc(color), np.float32)

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        rgb = c * (0.85 + 0.2 * fbm(X, Y, Z, 2.0, P['seed'] + seed, 2) + 0.08 * rnd(X, Y, Z, seed))[..., None]
        st = fbm(X, Y, Z, 3.0, P['seed'] + seed + 3, 2)
        rgb = np.where((st > 1 - sucio)[..., None], rgb * 0.75, rgb)
        return with_alpha(rgb)
    return f


def luz(color='#e9f3ee', seed=8):
    """Superficie que da luz (tubos de lampara, focos): casi pareja, con un poco de brillo."""
    c = np.array(hexc(color), np.float32)

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        rgb = c * (0.93 + 0.07 * rnd(X, Y, Z, seed))[..., None]
        return with_alpha(np.minimum(rgb, 255))
    f.emit = True
    return f


def apagado(color='#5a605d', seed=8):
    c = np.array(hexc(color), np.float32)

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        return with_alpha(c * (0.85 + 0.15 * rnd(X, Y, Z, seed))[..., None])
    return f


def vidrio(seed=9, a=70):
    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        out = np.zeros(X.shape + (4,), np.float32)
        out[..., :3] = np.array(hexc('#b8c4be'), np.float32)
        d = fbm(X, Y, Z, 3.0, P['seed'] + seed, 2)
        out[..., 3] = a + 70 * np.clip(d - 0.55, 0, 1) * 2
        return out
    f.cutout = True
    return f


def madera_vieja(seed=10):
    return kit.wood(Ramp('#2b1a10', '#3a2416', '#4a2f1c', '#5a3a23'), axis='h', grain=2.0, seed=seed)


def oxido(seed=11):
    r = Ramp('#4a2814', '#5b3218', '#71401f', '#8a5228')

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        return with_alpha(r(fbm(X, Y, Z, 1.5, P['seed'] + seed, 2) * 0.6 + rnd(X, Y, Z, seed) * 0.4))
    return f


VERDE = '#34463a'
NEGRO = '#232321'

# ---------------------------------------------------------------- muebles


def catre():
    """Catre de metal con colchon de rayas y almohada. Dos bloques: cabecera (head) y pies (foot). En el modelo, la cabecera queda al norte."""
    p = Prop('catre', 'Catre de celda', 'Cell cot', kind='cama', sound='metal', seed=21)
    frame = pintado(VERDE, oxido=0.3, seed=21)
    # cabecera
    p.at('head')
    for x0 in (1, 13):
        p.box(x0, 0, 1, x0 + 2, 15, 3, frame)                      # postes de la cabecera
    p.box(3, 12, 1.5, 13, 14, 2.5, frame)                          # barra de arriba
    p.box(3, 8, 1.5, 13, 9, 2.5, frame)
    for x0 in (5.5, 9.5):
        p.box(x0, 9, 1.7, x0 + 1, 12, 2.3, frame)
    p.box(1, 5, 3, 15, 6.5, 16, frame)                             # bastidor
    p.box(1.5, 6.5, 3, 14.5, 9, 16, colchon(), skip=('south',))
    p.box(3, 9, 4, 13, 11, 9, tela('#9c998c', sucio=0.45))         # almohada
    # pies
    p.at('foot')
    p.box(1.5, 6.5, 0, 14.5, 9, 13, colchon(), skip=('north',))
    p.box(1, 5, 0, 15, 6.5, 13, frame)
    for x0 in (1, 13):
        p.box(x0, 0, 13, x0 + 2, 11, 15, frame)                    # postes de los pies
    p.box(3, 9, 13.5, 13, 10.5, 14.5, frame)
    p.box(3, 4, 13.5, 13, 5, 14.5, frame)
    return p


def retrete():
    p = Prop('retrete', 'Retrete de acero', 'Steel toilet', kind='floor', sound='metal', seed=22)
    s = acero(22, sucio=0.45)
    p.box(3, 0, 9, 13, 12, 16, s)          # tanque contra la pared
    p.box(4, 0, 4, 12, 3, 12, s)           # base
    p.box(3, 3, 2, 13, 7, 11, s)           # taza
    p.box(4, 7, 3, 12, 7.6, 10, solid('#2a2a24'))   # adentro (oscuro)
    p.box(2.5, 7, 1.5, 13.5, 8, 11.5, s, skip=('up',))  # orilla
    p.box(6, 12, 11, 10, 13, 14, s)        # boton
    return p


def lavabo():
    p = Prop('lavabo', 'Lavabo de celda', 'Cell sink', kind='wall', sound='metal', seed=23)
    s = acero(23, sucio=0.5)
    p.box(3, 6, 8, 13, 10, 16, s)
    p.box(4, 9.5, 9, 12, 10, 15, solid('#2b2b26'))
    p.box(7, 10, 14, 9, 13, 16, s)
    p.box(7.5, 12, 11, 8.5, 13, 14, s)     # llave
    p.box(5, 4, 14, 11, 6, 16, oxido(23))  # tubo de abajo
    return p


def espejo():
    p = Prop('espejo', 'Espejo roto', 'Broken mirror', kind='wall', sound='glass', seed=24)
    p.box(4, 3, 15, 12, 13, 16, pintado(NEGRO, seed=24))
    ref = kit.metal(Ramp('#56605d', '#6a7572', '#7f8986', '#939d99'), streak=0.2, seed=24, sparkle=0.1)
    p.box(5, 4, 14.8, 11, 12, 15, ref)
    return p


def cajon():
    """Baul de metal al pie del catre: aqui van las pistas (se abre como cofre)."""
    p = Prop('cajon', 'Baúl de la celda', 'Cell footlocker', kind='floor', sound='metal', seed=25)
    m = pintado('#3c3a2e', oxido=0.4, seed=25)
    p.box(1, 0, 3, 15, 7, 13, m, bevel=0.15)
    p.box(0.8, 7, 2.8, 15.2, 9, 13.2, m, bevel=0.2)      # tapa
    p.box(7, 5, 2.4, 9, 8, 2.8, solid('#7d7d74'))        # cerrojo
    for x0 in (2, 13):
        p.box(x0, 1, 2.6, x0 + 1, 8.5, 3, pintado(NEGRO, seed=26))
    return p


def silla():
    p = Prop('silla', 'Silla de metal', 'Metal chair', kind='floor', sound='metal', seed=27)
    m = pintado('#3e4a45', seed=27)
    for x0, z0 in ((3, 3), (12, 3), (3, 12), (12, 12)):
        p.box(x0, 0, z0, x0 + 1, 8, z0 + 1, m)
    p.box(2.5, 8, 2.5, 13.5, 9, 13.5, m)
    p.box(3, 9, 12, 4, 18, 13, m)
    p.box(12, 9, 12, 13, 18, 13, m)
    p.box(3, 13, 12.2, 13, 17, 12.8, m)
    return p


def lampara_tubo(on=True):
    """Lampara fluorescente que cuelga del techo (dos tubos)."""
    pid = 'lampara_tubo' if on else 'lampara_tubo_apagada'
    p = Prop(pid, 'Lámpara fluorescente', 'Fluorescent lamp', kind='hang', sound='metal', seed=28, collide=False, light=14)
    m = pintado('#5d625e', oxido=0.25, seed=28)
    p.box(7.5, 13, 7.5, 8.5, 16, 8.5, m)                 # varilla
    p.box(0, 11, 5, 16, 13, 11, m)                       # caja
    tubo = luz('#e6f0ec') if on else apagado()
    p.box(0.5, 10, 6, 15.5, 11, 7.5, tubo)
    p.box(0.5, 10, 8.5, 15.5, 11, 10, tubo)
    return p


def lampara_jaula(on=True):
    """Foco de pared con jaula de alambre."""
    pid = 'lampara_jaula' if on else 'lampara_jaula_apagada'
    p = Prop(pid, 'Lámpara de jaula', 'Cage lamp', kind='wall', sound='lantern', seed=29, collide=False, light=11)
    m = pintado(NEGRO, seed=29)
    p.box(5, 5, 15, 11, 11, 16, m)
    p.box(7, 7, 12, 9, 9, 15, m)
    p.box(6.5, 6.5, 8, 9.5, 9.5, 12, luz('#ffd9a0') if on else apagado('#6a5c46'))
    for (a, b) in ((5.5, 5.5), (10, 5.5), (5.5, 10), (10, 10)):
        p.box(a, b, 7, a + 0.5, b + 0.5, 13, m)
    p.box(5.5, 5.5, 7, 10.5, 10.5, 7.5, m, skip=('north', 'south'))
    return p


def foco(on):
    """El foco del techo de cada celda (lo prende el ¡pum!): jaula de alambre colgando."""
    pid = 'foco_encendido' if on else 'foco_apagado'
    p = Prop(pid, 'Foco', 'Bulb', kind='plain', sound='lantern', seed=30, collide=False)
    m = pintado(NEGRO, seed=30)
    p.box(7.5, 12, 7.5, 8.5, 16, 8.5, m)
    p.box(6, 10.5, 6, 10, 12, 10, m)
    p.box(6.5, 6.5, 6.5, 9.5, 10.5, 9.5, luz('#ffe2b0') if on else apagado('#59503f'))
    for (a, b) in ((5.5, 5.5), (10, 5.5), (5.5, 10), (10, 10)):
        p.box(a, 6, b, a + 0.5, 11, b + 0.5, m)
    p.box(5.5, 5.5, 5.5, 10.5, 6, 10.5, m)
    return p


def rejilla_ventilacion():
    p = Prop('ventila', 'Rejilla de ventilación', 'Air vent', kind='wall', sound='metal', seed=31, collide=False)
    m = pintado('#4c4f4a', oxido=0.45, seed=31)
    p.box(2, 3, 15, 14, 13, 16, solid('#121210'))
    p.box(2, 3, 14.5, 3, 13, 16, m)
    p.box(13, 3, 14.5, 14, 13, 16, m)
    p.box(2, 3, 14.5, 14, 4, 16, m)
    p.box(2, 12, 14.5, 14, 13, 16, m)
    for y in (5, 7, 9, 11):
        p.box(3, y, 14.6, 13, y + 0.8, 15.6, m, rot=('x', -22.5, (8, y, 15)))
    return p


def camara():
    """Camara de vigilancia en su brazo (con el foquito rojo)."""
    p = Prop('camara', 'Cámara de vigilancia', 'Security camera', kind='wall', sound='metal', seed=32, collide=False)
    m = pintado('#c8c3b4', oxido=0.1, seed=32, saltado=0.1)
    k = pintado(NEGRO, seed=33)
    p.box(6, 9, 14, 10, 13, 16, k)
    p.box(7.5, 10, 9, 8.5, 11, 14, k)
    p.box(5, 9, 2, 11, 14, 11, m, rot=('x', 22.5, (8, 11, 9)))
    p.box(6, 10, 1, 10, 13, 2, solid('#0e0e10'), rot=('x', 22.5, (8, 11, 9)))
    p.box(9.5, 13.4, 9, 10.5, 14.4, 10, luz('#ff2a1a'), rot=('x', 22.5, (8, 11, 9)))
    return p


def bocina():
    p = Prop('bocina', 'Bocina', 'Loudspeaker', kind='wall', sound='metal', seed=34, collide=False)
    m = pintado('#6b6d63', oxido=0.4, seed=34)
    p.box(7, 6, 13, 9, 9, 16, m)
    p.box(5, 5, 8, 11, 11, 13, m)
    p.box(3, 3, 5, 13, 13, 8, m)
    p.box(4, 4, 4.8, 12, 12, 5, solid('#151514'))
    return p


def reloj():
    p = Prop('reloj', 'Reloj de pared', 'Wall clock', kind='wall', sound='wood', seed=35, collide=False)
    p.box(3, 3, 15, 13, 13, 16, pintado(NEGRO, seed=35))
    p.box(4, 4, 14.8, 12, 12, 15, porcelana(35, sucio=0.5))
    p.box(7.6, 8, 14.6, 8.4, 11.5, 14.8, solid('#151412'))
    p.box(8, 7.6, 14.6, 10.5, 8.4, 14.8, solid('#151412'))
    return p


def telarana():
    p = Prop('telarana', 'Telaraña', 'Cobweb', kind='plain', sound='wool', seed=36, collide=False, render='cutout')

    def web(P):
        U, V, W, H = P['U'].astype(np.float32), P['V'].astype(np.float32), float(P['W']), float(P['H'])
        out = np.zeros(U.shape + (4,), np.float32)
        cx, cy = W * 0.3, H * 0.3
        dx, dy = U - cx, V - cy
        ang = np.arctan2(dy, dx)
        r = np.sqrt(dx * dx + dy * dy)
        spokes = np.abs(np.sin(ang * 4)) < 0.12
        rings = (np.abs((r % 5) - 0.0) < 0.6)
        m = (spokes | rings) & (r < W * 0.9)
        out[..., :3] = np.array(hexc('#d9d9d4'), np.float32)
        out[..., 3] = np.where(m, 170, 0)
        return out
    web.cutout = True
    p.box(0, 0, 8, 16, 16, 8, web, rot=('y', 45, (8, 8, 8)), shade=False)
    p.box(8, 0, 0, 8, 16, 16, web, rot=('y', 45, (8, 8, 8)), shade=False)
    return p


def calcomania(pid, es, en, mat, seed):
    """Adorno plano en el piso (mancha, charco, papeles): no estorba al caminar."""
    p = Prop(pid, es, en, kind='floor', sound='wool', seed=seed, collide=False, render='cutout')
    p.box(0, 0.05, 0, 16, 0.05, 16, mat, skip=('down',))
    return p


def mancha_mat(color='#2a1f14', a=200, seed=40):
    def f(P):
        X, Z = P['X'], P['Z']
        out = np.zeros(X.shape + (4,), np.float32)
        d = np.sqrt((X - 8) ** 2 + (Z - 8) ** 2) / 8
        n = fbm(X, X * 0, Z, 2.5, P['seed'] + seed, 3)
        m = (n * 1.1 - d * 0.75) > 0.12
        out[..., :3] = np.array(hexc(color), np.float32) * (0.8 + 0.3 * n[..., None])
        out[..., 3] = np.where(m, a, 0)
        return out
    f.cutout = True
    return f


def papeles_mat(seed=42):
    def f(P):
        X, Z = P['X'], P['Z']
        out = np.zeros(X.shape + (4,), np.float32)
        r = np.random.RandomState(seed)
        for _ in range(3):
            cx, cz, w, h, a = r.rand() * 10 + 3, r.rand() * 10 + 3, 3.5 + r.rand() * 1.5, 5 + r.rand() * 1.5, r.rand() * 3.14
            dx, dz = X - cx, Z - cz
            u = dx * math.cos(a) + dz * math.sin(a)
            v = -dx * math.sin(a) + dz * math.cos(a)
            m = (np.abs(u) < w / 2) & (np.abs(v) < h / 2)
            col = np.array(hexc('#c4bca4'), np.float32) * (0.8 + 0.2 * r.rand())
            lines = (np.floor(v * 2) % 2 == 0) & (np.abs(u) < w / 2 - 0.5)
            c2 = np.where(lines[..., None], col * 0.8, col)
            out[..., :3] = np.where(m[..., None], c2, out[..., :3])
            out[..., 3] = np.where(m, 255, out[..., 3])
        return out
    f.cutout = True
    return f


def vitrina():
    """Vitrina de la coleccion: pedestal de madera (abajo) y caja de vidrio con una cabeza de muneca de porcelana (arriba)."""
    p = Prop('vitrina', 'Vitrina de la colección', 'Collection showcase', kind='floor', tall=True, sound='glass', seed=43, render='translucent')
    w = madera_vieja(43)
    p.at('lower')
    p.box(2, 0, 2, 14, 2, 14, w, bevel=0.2)
    p.box(3, 2, 3, 13, 14, 13, kit.wood(Ramp('#24150c', '#2f1c10', '#3b2415'), axis='v', seed=44), bevel=0.15)
    p.box(2, 14, 2, 14, 16, 14, w, bevel=0.2)
    p.at('upper')
    # la muneca: cabeza de porcelana con ojos negros y una grieta
    por = porcelana(45, sucio=0.25)
    p.box(5, 0, 5, 11, 2, 11, pintado('#5a1418', seed=46))   # cojin
    p.box(5.5, 2, 5.5, 10.5, 7.5, 10.5, por)
    p.box(6.5, 5, 5.3, 7.5, 6, 5.5, solid('#0c0b0b'))
    p.box(8.5, 5, 5.3, 9.5, 6, 5.5, solid('#0c0b0b'))
    p.box(7, 3, 5.3, 9, 3.5, 5.5, solid('#6a1c1c'))
    p.box(5.2, 6.5, 5.2, 10.8, 8.5, 10.8, tela('#3b2616', sucio=0.2))  # pelo
    g = vidrio(47)
    p.box(2, 0, 2, 14, 12, 14, g, shade=False)
    p.box(2, 12, 2, 14, 13, 14, w)
    return p


def candelabro():
    p = Prop('candelabro', 'Candelabro de hierro', 'Iron candelabra', kind='floor', tall=True, light=12, sound='metal', seed=48,
             particles=[{'type': 'minecraft:small_flame', 'pos': [8, 30.5, 8], 'rate': 0.4}, {'type': 'minecraft:small_flame', 'pos': [3.5, 27.5, 8], 'rate': 0.3},
                        {'type': 'minecraft:small_flame', 'pos': [12.5, 27.5, 8], 'rate': 0.3}, {'type': 'minecraft:smoke', 'pos': [8, 31, 8], 'rate': 0.1}])
    m = pintado(NEGRO, oxido=0.2, seed=48)
    cera = kit.stone(Ramp('#b6ad94', '#c6bea5', '#d4ccb3'), scale=2, seed=49)
    p.at('lower')
    p.box(4, 0, 4, 12, 1, 12, m)
    p.box(7, 1, 7, 9, 16, 9, m)
    p.at('upper')
    p.box(7, 0, 7, 9, 10, 9, m)
    p.box(2.5, 8, 7, 13.5, 9, 9, m)
    for x0 in (2.5, 11.5):
        p.box(x0, 9, 7, x0 + 2, 10, 9, m)
        p.box(x0 + 0.3, 10, 7.3, x0 + 1.7, 11.5, 8.7, cera)
    p.box(6.8, 10, 6.8, 9.2, 11, 9.2, m)
    p.box(7.2, 11, 7.2, 8.8, 14.5, 8.8, cera)
    return p


def cortina():
    p = Prop('cortina', 'Cortina de terciopelo', 'Velvet curtain', kind='wall', tall=True, sound='wool', seed=50, collide=False)
    c = np.array(hexc('#5a0f14'), np.float32)

    def vel(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        fold = 0.7 + 0.3 * np.sin(X * 1.3)
        rgb = c * (fold * (0.85 + 0.25 * fbm(X, Y, Z, 2.0, P['seed'] + 3, 2)))[..., None]
        return with_alpha(rgb)
    p.at('lower')
    p.box(1, 0, 13, 15, 16, 15.5, vel)
    p.at('upper')
    p.box(1, 0, 13, 15, 14, 15.5, vel)
    p.box(0, 14, 12.5, 16, 16, 16, kit.metal(Ramp('#5a4a22', '#7a6430', '#957a3c'), seed=51))
    return p


def placa():
    """Plaquita de metal atornillada (para el numero de la celda: el texto lo dibuja el juego)."""
    p = Prop('placa', 'Placa', 'Plate', kind='wall', sound='metal', seed=52, collide=False)
    p.box(3, 5, 15.4, 13, 11, 16, pintado('#2b2b28', oxido=0.3, seed=52))
    for x0 in (3.6, 11.8):
        p.box(x0, 7.6, 15.2, x0 + 0.6, 8.4, 15.4, solid('#9a988e'))
    return p


def letrero():
    p = Prop('letrero', 'Letrero', 'Sign', kind='wall', sound='metal', seed=53, collide=False)
    p.box(0, 3, 15, 16, 13, 16, pintado('#1d1d1b', oxido=0.25, seed=53))
    return p


def rayon():
    """Rayones en la pared (las pistas de las celdas): solo una capa invisible donde el juego escribe."""
    p = Prop('rayon', 'Rayones', 'Scratches', kind='wall', sound='stone', seed=54, collide=False)
    def rasgunos(P):
        U, V = P['U'], P['V']
        out = np.zeros(U.shape + (4,), np.float32)
        r = np.random.RandomState(54)
        for _ in range(9):
            x0, y0 = r.randint(0, U.shape[1]), r.randint(0, U.shape[0])
            ln = r.randint(4, 12)
            dx = r.choice([-1, 1])
            for i in range(ln):
                xx, yy = x0 + (i * dx) // 3, y0 + i
                if 0 <= xx < U.shape[1] and 0 <= yy < U.shape[0]:
                    out[yy, xx, :3] = (190, 182, 166)
                    out[yy, xx, 3] = 110
        return out
    rasgunos.cutout = True
    p.box(1, 1, 15.95, 15, 15, 15.95, rasgunos, skip=('south',), shade=False)
    return p


def candado():
    """El candado de la celda (y el teclado de la boveda): caja de metal con pantallita y botones, atornillada a la pared."""
    p = Prop('candado', 'Candado', 'Lock', kind='wall', sound='metal', seed=55)
    m = pintado('#2c2d2a', oxido=0.35, seed=55)
    p.box(4, 2, 13, 12, 14, 16, m, bevel=0.2)
    p.box(5, 10, 12.8, 11, 12.5, 13, luz('#5a1a14'))
    for i in range(3):
        for j in range(3):
            p.box(5 + i * 2.2, 3.5 + j * 2.2, 12.6, 6.6 + i * 2.2, 5.1 + j * 2.2, 13, kit.metal(Ramp('#6a6a62', '#7d7d74', '#8e8e84'), seed=56 + i + j))
    p.box(7.5, 14, 14, 8.5, 16, 15, oxido(57))
    return p


def grilletes():
    """Grilletes con cadena colgando de una argolla en la pared."""
    p = Prop('grilletes', 'Grilletes de pared', 'Wall shackles', kind='wall', sound='chain', seed=58, collide=False)
    m = kit.metal(Ramp('#2d2c2a', '#3b3a37', '#4a4945', '#58564f'), seed=58)
    p.box(6, 12, 15, 10, 14, 16, m)
    p.box(7.5, 11, 14, 8.5, 13, 15, m)
    for i in range(5):
        y = 10 - i * 1.6
        p.box(7.4, y, 13.6, 8.6, y + 1.2, 14.4, m)
    p.box(5, 2, 12.5, 11, 3, 15, m)
    p.box(5, 2, 12.5, 6, 4.5, 15, m)
    p.box(10, 2, 12.5, 11, 4.5, 15, m)
    return p


def silla_ruedas():
    """Silla de ruedas vieja de hospital."""
    p = Prop('silla_ruedas', 'Silla de ruedas', 'Wheelchair', kind='floor', sound='metal', seed=59)
    m = acero(59, sucio=0.5)
    cuero = tela('#3b2d22', sucio=0.4)
    for x0 in (1, 14):
        p.box(x0, 1, 3, x0 + 1, 13, 15, m, skip=('up', 'down'))      # ruedas (aros)
        p.box(x0 - 0.2, 6, 7, x0 + 1.2, 8, 11, m)
    p.box(3, 6, 4, 13, 7, 13, m)
    p.box(3.5, 7, 4.5, 12.5, 8.5, 12.5, cuero)                       # asiento
    p.box(3.5, 8.5, 12, 12.5, 17, 13, cuero)                          # respaldo
    p.box(3, 8, 12.5, 4, 18, 13.5, m)
    p.box(12, 8, 12.5, 13, 18, 13.5, m)
    p.box(4, 1, 1, 6, 2, 4, m)                                        # pedales
    p.box(10, 1, 1, 12, 2, 4, m)
    p.box(4.5, 2, 3, 5.5, 6, 4, m)
    p.box(10.5, 2, 3, 11.5, 6, 4, m)
    return p


def camilla():
    """Camilla de hospital con sabana manchada (dos bloques de largo: se pone como cama sin dormir)."""
    p = Prop('camilla', 'Camilla', 'Gurney', kind='floor', sound='metal', seed=60)
    m = acero(60, sucio=0.45)
    sab = tela('#b5ae9a', sucio=0.5)
    for x0, z0 in ((1, 1), (14, 1), (1, 14), (14, 14)):
        p.box(x0, 1, z0, x0 + 1, 8, z0 + 1, m)
        p.box(x0 - 0.3, 0, z0 - 0.3, x0 + 1.3, 1.2, z0 + 1.3, solid('#151514'))
    p.box(0.5, 8, 0.5, 15.5, 9, 15.5, m)
    p.box(1, 9, 1, 15, 11, 15, sab)
    p.box(4, 11, 1.5, 12, 12.5, 5, tela('#c9c3b2', sucio=0.35))
    p.box(0.5, 9, 0.5, 1, 12, 15.5, m)
    p.box(15, 9, 0.5, 15.5, 12, 15.5, m)
    return p


def all_props():
    return [catre(), retrete(), lavabo(), espejo(), cajon(), silla(), lampara_tubo(True), lampara_tubo(False), lampara_jaula(True),
            lampara_jaula(False), rejilla_ventilacion(), camara(), bocina(), reloj(), telarana(),
            calcomania('mancha', 'Mancha', 'Stain', mancha_mat(), 40), calcomania('charco', 'Charco', 'Puddle', mancha_mat('#28343a', 150, 41), 41),
            calcomania('papeles', 'Papeles tirados', 'Scattered papers', papeles_mat(), 42), vitrina(), candelabro(), cortina(), placa(), letrero(), rayon()]


def focos():
    return [foco(True), foco(False)]
