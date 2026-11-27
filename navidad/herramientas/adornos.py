#!/usr/bin/env python3
"""Los adornos para colgar en el Pino de Navidad (modelos 3D de objeto hechos a mano con cajas, y su textura pintada).

Todos se arman en el espacio de 0 a 16 con el gancho (o la base, en las puntas) donde dice ANCLA, para que el árbol los cuelgue
derechito de la punta de una rama. Se ven igual en el inventario, en la mano y colgados.
Escribe models/item/<id>.json y textures/item/<id>.png.

Uso: python3 herramientas/adornos.py"""
import math
import os
import sys

import numpy as np

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, 'deco'))
import kit  # noqa: E402
from kit import Prop, Ramp, hexc, metal, solid, with_alpha, rnd, fbm  # noqa: E402

kit.RES = 4  # adornos chiquitos: 4 texeles por pixel para que se vean finos
A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')

# ---------------------------------------------------------------- materiales propios


def brillante(color, oscuro=None, punto=(6.2, 9.6, 5.0), radio=1.8, emit=False, motas=None):
    """Esfera de vidrio pintado: más oscura abajo, un reflejo blanco arriba a la izquierda y (si se pide) motitas."""
    c = np.array(hexc(color), np.float32)
    o = np.array(hexc(oscuro), np.float32) if oscuro else c * 0.62

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        t = np.clip((Y - 4.0) / 7.0, 0, 1)[..., None]
        rgb = o * (1 - t) + c * t
        d = np.sqrt((X - punto[0]) ** 2 + (Y - punto[1]) ** 2 + (Z - punto[2]) ** 2)
        rgb = np.where((d < radio)[..., None], np.minimum(rgb * 0.35 + 190, 255), rgb)
        rgb = np.where(((d >= radio) & (d < radio + 0.6))[..., None], np.minimum(rgb * 0.75 + 60, 255), rgb)
        if motas is not None:
            m = np.array(hexc(motas), np.float32)
            mm = rnd(X, Y, Z, 41) > 0.86
            rgb = np.where(mm[..., None], m, rgb)
        return with_alpha(rgb)
    f.emit = emit
    return f


def oro(seed=3):
    return metal(Ramp('#a8701a', '#d39a2a', '#f0c040', '#ffe58a'), streak=0.4, seed=seed, sparkle=0.12)


def plata(seed=3):
    return metal(Ramp('#8a929c', '#b4bcc6', '#dce2ea', '#ffffff'), streak=0.4, seed=seed, sparkle=0.1)


def rayas(c1, c2, paso=1.4):
    """Bastón de caramelo: rayas en diagonal."""
    a, b = np.array(hexc(c1), np.float32), np.array(hexc(c2), np.float32)

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        k = np.floor((X + Y + Z) / paso) % 2 == 0
        return with_alpha(np.where(k[..., None], a, b))
    return f


def tela(color, pliegue=0.12):
    c = np.array(hexc(color), np.float32)

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        v = 1 + np.sin((X + Y * 0.6) * 2.2) * pliegue + (rnd(X, Y, Z, 7) - 0.5) * 0.08
        return with_alpha(np.clip(c * v[..., None], 0, 255))
    return f


def hielo(emit=True):
    r = Ramp('#9fd0f0', '#c4e6fa', '#e6f6ff', '#ffffff')

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        t = fbm(X, Y, Z, 1.2, P['seed'] + 5, 2) * 0.7 + (rnd(X, Y, Z, 3) - 0.5) * 0.4 + 0.25
        return with_alpha(r(t))
    f.emit = emit
    return f


def con_dibujo(base, dibujo):
    """Un material base y encima pixeles sueltos (cara de frente/atrás): dibujo(X, Y, face) -> color o None."""
    def f(P):
        out = base(P)
        X, Y, face = P['X'], P['Y'], P['face']
        if face in ('north', 'south'):
            hs, ws = X.shape
            for i in range(hs):
                for j in range(ws):
                    col = dibujo(float(X[i, j]), float(Y[i, j]), face)
                    if col is not None:
                        out[i, j, :3] = hexc(col)
        return out
    f.emit = getattr(base, 'emit', False)
    return f


def vidrio(color='#d8f0ff', alfa=96):
    c = np.array(hexc(color), np.float32)

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        out = np.zeros(X.shape + (4,), np.float32)
        out[..., :3] = c
        out[..., 3] = alfa
        d = np.sqrt((X - 6.0) ** 2 + (Y - 10.0) ** 2 + (Z - 5.0) ** 2)
        brillo = d < 1.6
        out[..., :3] = np.where(brillo[..., None], 255, out[..., :3])
        out[..., 3] = np.where(brillo, 220, out[..., 3])
        return out
    f.cutout = True
    return f


# ---------------------------------------------------------------- piezas comunes

def gancho(p, y=11.5, ancho=2.6):
    """La tapita dorada y el arito de donde se cuelga (termina en y = 14)."""
    a = ancho / 2
    p.box(8 - a, y, 8 - a, 8 + a, y + 1.25, 8 + a, oro())
    p.box(7.4, y + 1.25, 7.8, 8.6, 14, 8.2, oro(5))


def bola(p, cy=7.5, r=3.5, mat=None):
    """Una esfera hecha de cajas (un cubo con las caras abultadas)."""
    k = r * 0.78
    p.box(8 - k, cy - k, 8 - k, 8 + k, cy + k, 8 + k, mat)
    s = r * 0.5
    p.box(8 - r, cy - s, 8 - s, 8 + r, cy + s, 8 + s, mat)
    p.box(8 - s, cy - r, 8 - s, 8 + s, cy + r, 8 + s, mat)
    p.box(8 - s, cy - s, 8 - r, 8 + s, cy + s, 8 + r, mat)


# ---------------------------------------------------------------- los adornos (id, es, en, rareza, puntos, ancla)

ADORNOS = []


def adorno(id_, es, en, rareza, ancla=14, tipo='colgante'):
    def deco(fn):
        ADORNOS.append((id_, es, en, rareza, ancla, tipo, fn))
        return fn
    return deco


def esfera(color, oscuro, motas=None, banda=None):
    def fn():
        p = Prop('x', '', '')
        bola(p, mat=brillante(color, oscuro, motas=motas))
        if banda:
            p.box(4.35, 7.0, 4.35, 11.65, 8.0, 11.65, banda)
        gancho(p)
        return p
    return fn


adorno('adorno_esfera_roja', 'Esfera roja para el árbol', 'Red tree bauble', 'comun')(esfera('#e02434', '#8a0c18'))
adorno('adorno_esfera_azul', 'Esfera azul nevada', 'Snowy blue bauble', 'comun')(esfera('#3a7ae8', '#173a8a', motas='#f4f8ff'))
adorno('adorno_esfera_dorada', 'Esfera dorada', 'Gold bauble', 'comun')(esfera('#f2c040', '#a06a10', motas='#fff4c0'))


@adorno('adorno_baston', 'Bastoncito de caramelo', 'Little candy cane', 'comun')
def baston():
    p = Prop('x', '', '')
    m = rayas('#f4f4f6', '#e0202c')
    p.box(6.25, 2, 7.25, 7.75, 12.5, 8.75, m)            # el palo
    p.box(6.25, 12.5, 7.25, 10.25, 14, 8.75, m)           # la curva de arriba
    p.box(9.25, 10.5, 7.25, 10.75, 13.5, 8.75, m)         # el gancho que baja
    return p


@adorno('adorno_mono', 'Moño rojo', 'Red bow', 'comun', ancla=12)
def mono():
    p = Prop('x', '', '')
    rojo, osc = tela('#d81e2c'), tela('#a0101c')
    p.box(7, 8.5, 7.4, 9, 10.5, 8.6, osc)                                       # el nudo
    p.box(3.5, 8.75, 7.6, 7, 12, 8.4, rojo, rot=('z', 22.5, (7, 9.5, 8)))       # lazo izquierdo
    p.box(9, 8.75, 7.6, 12.5, 12, 8.4, rojo, rot=('z', -22.5, (9, 9.5, 8)))     # lazo derecho
    p.box(6, 4, 7.7, 7.25, 8.75, 8.3, rojo, rot=('z', -22.5, (7, 8.75, 8)))     # las tiras que cuelgan
    p.box(8.75, 4, 7.7, 10, 8.75, 8.3, rojo, rot=('z', 22.5, (9, 8.75, 8)))
    return p


@adorno('adorno_campanita', 'Campanita dorada', 'Little gold bell', 'bonito')
def campanita():
    p = Prop('x', '', '')
    o = oro(7)
    p.box(6.75, 9.5, 6.75, 9.25, 11, 9.25, o)
    p.box(5.75, 7, 5.75, 10.25, 9.5, 10.25, o)
    p.box(5, 5.75, 5, 11, 7, 11, o)
    p.box(4.5, 5, 4.5, 11.5, 5.75, 11.5, oro(9))
    p.box(7.4, 4, 7.4, 8.6, 5, 8.6, metal(Ramp('#5a4020', '#7a5a30', '#9a7a48')))   # el badajo
    rojo = tela('#d81e2c')
    p.box(6.5, 11, 7.5, 9.5, 12, 8.5, rojo)                                       # moñito
    p.box(4.75, 11, 7.6, 6.75, 12.75, 8.4, rojo, rot=('z', 22.5, (6.75, 11.5, 8)))
    p.box(9.25, 11, 7.6, 11.25, 12.75, 8.4, rojo, rot=('z', -22.5, (9.25, 11.5, 8)))
    p.box(7.5, 12, 7.8, 8.5, 14, 8.2, oro())
    return p


@adorno('adorno_copo', 'Copo de nieve brillante', 'Shining snowflake', 'bonito')
def copo():
    p = Prop('x', '', '')
    h = hielo()
    z0, z1 = 7.5, 8.5
    p.box(7.4, 2.5, z0, 8.6, 13, z1, h)                                       # brazo de arriba a abajo
    p.box(2.75, 7.15, z0, 13.25, 8.35, z1, h)                                 # brazo de lado a lado
    p.box(3.4, 7.2, z0 + 0.1, 12.6, 8.3, z1 - 0.1, h, rot=('z', 45, (8, 7.75, 8)))
    p.box(3.4, 7.2, z0 + 0.1, 12.6, 8.3, z1 - 0.1, h, rot=('z', -45, (8, 7.75, 8)))
    for y in (4.2, 11.0):                                                     # ramitas en V (en los brazos de arriba y abajo)
        p.box(6.4, y, z0 + 0.15, 9.6, y + 0.8, z1 - 0.15, h)
    for x in (4.6, 10.6):
        p.box(x, 6.15, z0 + 0.15, x + 0.8, 9.35, z1 - 0.15, h)
    p.box(6.9, 6.65, z0 - 0.2, 9.1, 8.85, z1 + 0.2, hielo())                  # el centro
    p.box(7.6, 13, 7.8, 8.4, 14, 8.2, plata())
    return p


def galleta_cara(x, y, face):
    if 9.6 < y < 10.4 and (6.9 < x < 7.6 or 8.4 < x < 9.1):
        return '#2a1a10'                                        # ojitos
    if 9.0 < y < 9.4 and 7.3 < x < 8.7:
        return '#f4f0f0'                                        # sonrisa de betún
    if 4.9 < y < 8.5 and 7.6 < x < 8.4 and (abs(y - 5.6) < 0.35 or abs(y - 6.8) < 0.35 or abs(y - 8.0) < 0.35):
        return '#e0202c' if abs(y - 6.8) < 0.35 else '#3bd16f'  # botones de colores
    if (abs(y - 7.6) < 0.25 and (4.3 < x < 5.6 or 10.4 < x < 11.7)) or (abs(y - 3.0) < 0.25 and (6.2 < x < 7.6 or 8.4 < x < 9.8)):
        return '#f4f0f0'                                        # rayitas de betún en manos y pies
    return None


@adorno('adorno_galleta', 'Galletita de jengibre', 'Gingerbread ornament', 'bonito')
def galleta():
    p = Prop('x', '', '')
    g = con_dibujo(solid('#b8743a'), galleta_cara)
    z0, z1 = 7.4, 8.6
    p.box(6.4, 9, z0, 9.6, 12, z1, g)          # cabeza
    p.box(6, 4.5, z0, 10, 9, z1, g)            # cuerpo
    p.box(4, 7, z0, 6, 8.5, z1, g)             # brazos
    p.box(10, 7, z0, 12, 8.5, z1, g)
    p.box(6, 2, z0, 7.75, 4.5, z1, g)          # piernas
    p.box(8.25, 2, z0, 10, 4.5, z1, g)
    p.box(7.6, 12, 7.8, 8.4, 14, 8.2, tela('#d81e2c'))   # el listón
    return p


@adorno('adorno_pina', 'Piña nevada', 'Snowy pinecone', 'bonito')
def pina():
    p = Prop('x', '', '')
    esc = Ramp('#4a2e16', '#6a4422', '#8a5c30', '#a87442')

    def escamas(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        k = (np.floor(Y * 1.6) + np.floor((X + Z) * 0.9)) % 2
        t = 0.25 + k * 0.45 + (Y - 3) * 0.02 + (rnd(X, Y, Z, 4) - 0.5) * 0.15
        return with_alpha(esc(t))
    p.box(5.25, 8, 5.25, 10.75, 11, 10.75, escamas)
    p.box(5.75, 6, 5.75, 10.25, 8, 10.25, escamas)
    p.box(6.4, 4.25, 6.4, 9.6, 6, 9.6, escamas)
    p.box(7.25, 3.2, 7.25, 8.75, 4.25, 8.75, escamas)
    p.box(5.5, 10.6, 5.5, 10.5, 11.5, 10.5, solid('#f2f6fa'))          # nieve arriba
    p.box(7.6, 11.5, 7.8, 8.4, 14, 8.2, tela('#d81e2c'))
    return p


@adorno('adorno_cristal', 'Esfera de cristal con nieve', 'Snow globe bauble', 'raro')
def cristal():
    p = Prop('x', '', '', render='translucent')
    # adentro (va primero para que se vea a través del vidrio): piso de nieve, un pinito y un muñequito
    p.box(5.25, 4.6, 5.25, 10.75, 5.4, 10.75, solid('#f4f8fc'))
    p.box(5.9, 5.4, 7.1, 7.9, 6.9, 9.1, solid('#2e7a3a'))
    p.box(6.3, 6.9, 7.5, 7.5, 8.2, 8.7, solid('#3b8a48'))
    p.box(6.7, 8.2, 7.9, 7.1, 8.6, 8.3, solid('#ffd84a'))
    p.box(8.6, 5.4, 7.6, 10.0, 6.8, 9.0, solid('#ffffff'))
    p.box(8.85, 6.8, 7.85, 9.75, 7.7, 8.75, solid('#ffffff'))
    p.box(8.9, 7.7, 7.9, 9.7, 7.95, 8.7, solid('#e0202c'))
    # el vidrio
    bola(p, cy=7.5, r=3.6, mat=vidrio())
    gancho(p, y=11.6)
    return p


@adorno('adorno_pajarito', 'Pajarito rojo', 'Little red bird', 'raro', ancla=4)
def pajarito():
    p = Prop('x', '', '')
    rojo = tela('#d0202c', 0.05)

    def cara(x, y, face):
        return None
    p.box(7.6, 4, 7.3, 8.4, 5.5, 8.7, oro())                                            # el clip
    p.box(6.4, 5.5, 6.0, 9.6, 8.5, 10.2, rojo)                                          # cuerpo
    p.box(6.9, 8.0, 4.6, 9.1, 10.6, 7.0, rojo)                                          # cabeza
    p.box(7.6, 10.4, 5.0, 8.4, 11.8, 6.6, rojo, rot=('x', -22.5, (8, 10.4, 5.8)))       # copete
    p.box(6.85, 8.4, 4.5, 9.15, 9.6, 5.0, solid('#1a1418'))                             # antifaz negro
    p.box(7.5, 8.6, 3.7, 8.5, 9.4, 4.5, solid('#f29a2a'))                               # pico
    p.box(7.0, 9.6, 4.55, 7.5, 10.0, 4.65, solid('#ffffff'))                            # brillito en el ojo
    p.box(8.5, 9.6, 4.55, 9.0, 10.0, 4.65, solid('#ffffff'))
    p.box(7.2, 6.0, 10.0, 8.8, 7.2, 13.4, tela('#a8141e', 0.04), rot=('x', 22.5, (8, 6.6, 10)))   # cola
    p.box(6.1, 5.9, 6.8, 6.5, 7.9, 9.6, tela('#a8141e', 0.04))                          # alas
    p.box(9.5, 5.9, 6.8, 9.9, 7.9, 9.6, tela('#a8141e', 0.04))
    return p


@adorno('adorno_pinguino', 'Pingüinito de adorno', 'Penguin ornament', 'raro')
def pinguino():
    p = Prop('x', '', '')
    negro = solid('#1c1e28')

    def panza(x, y, face):
        if face == 'north' and 6.5 < x < 9.5 and 3.5 < y < 8.2:
            return '#f4f5f8'
        if face == 'north' and 6.9 < x < 9.1 and 8.7 < y < 10.9:
            if 9.6 < y < 10.2 and (7.1 < x < 7.6 or 8.4 < x < 8.9):
                return '#101016'
            if 9.0 < y < 9.4 and (7.0 < x < 7.4 or 8.6 < x < 9.0):
                return '#f8a0ac'
            return '#f4f5f8'
        return None
    m = con_dibujo(negro, panza)
    p.box(6, 3, 6.25, 10, 8.5, 9.75, m)                         # cuerpo
    p.box(6.5, 8.5, 6.5, 9.5, 11.25, 9.5, m)                    # cabeza
    p.box(7.5, 9.1, 5.7, 8.5, 9.7, 6.5, solid('#f69624'))       # pico
    p.box(6.3, 2.4, 6.3, 7.7, 3, 7.9, solid('#f69624'))         # patitas
    p.box(8.3, 2.4, 6.3, 9.7, 3, 7.9, solid('#f69624'))
    p.box(5.4, 4.5, 7.2, 6, 7.6, 8.8, negro, rot=('z', -22.5, (6, 7.6, 8)))   # aletas
    p.box(10, 4.5, 7.2, 10.6, 7.6, 8.8, negro, rot=('z', 22.5, (10, 7.6, 8)))
    p.box(6.25, 8.1, 6.25, 9.75, 8.9, 9.75, rayas('#d81e2c', '#f4f4f6', 1.0))  # bufanda
    p.box(6.4, 11.25, 6.4, 9.6, 12, 9.6, solid('#f4f4f6'))      # gorrito
    p.box(6.9, 12, 6.9, 9.1, 13.2, 9.1, tela('#d81e2c'))
    p.box(7.6, 13.2, 7.6, 8.4, 14, 8.4, solid('#ffffff'))
    return p


@adorno('adorno_renito', 'Renito de adorno', 'Little reindeer ornament', 'raro')
def renito():
    p = Prop('x', '', '')
    cafe = solid('#a06a3e')
    claro = solid('#d8b890')
    p.box(5, 4, 7, 10.5, 7, 9, cafe)                                   # cuerpo
    p.box(5.2, 2, 7.2, 6.2, 4, 8.8, cafe)                              # patas
    p.box(9.3, 2, 7.2, 10.3, 4, 8.8, cafe)
    p.box(9.0, 6.5, 7.1, 11.4, 9.5, 8.9, cafe)                         # cabeza
    p.box(10.9, 7.0, 7.3, 12.2, 8.4, 8.7, claro)                       # hocico
    p.box(12.1, 7.3, 7.55, 12.8, 8.1, 8.45, brillante('#ff2a2a', '#c00010', punto=(12.3, 8.0, 7.6), radio=0.3, emit=True))  # nariz
    p.box(9.4, 9.5, 7.6, 9.9, 11.5, 8.0, solid('#6a4422'))             # cuernos
    p.box(9.4, 10.8, 7.6, 10.9, 11.3, 8.0, solid('#6a4422'))
    p.box(10.3, 9.5, 8.0, 10.8, 11.0, 8.4, solid('#6a4422'))
    p.box(10.3, 10.5, 8.0, 11.6, 11.0, 8.4, solid('#6a4422'))
    p.box(4.3, 6.2, 7.6, 5.1, 7.2, 8.4, claro)                         # colita
    p.box(8.9, 6.4, 6.9, 9.5, 7.2, 9.1, tela('#d81e2c'))               # collar
    p.box(7.6, 7, 7.8, 8.4, 14, 8.2, tela('#d81e2c'))                  # el listón para colgar
    return p


@adorno('adorno_luces', 'Tira de luces de colores', 'String of colored lights', 'bonito', tipo='luces')
def luces():
    p = Prop('x', '', '')
    cable = solid('#1e3a22')
    # un rollo: un aro de cable con focos de colores
    pts = 10
    cols = ['#ff4040', '#ffd640', '#4896ff', '#48dc78', '#ff78dc']
    for k in range(pts):
        a = 2 * math.pi * k / pts
        x, z = 8 + 4.2 * math.cos(a), 8 + 4.2 * math.sin(a)
        p.box(x - 0.7, 5.2, z - 0.7, x + 0.7, 6.0, z + 0.7, cable)
        c = cols[k % len(cols)]
        p.box(x - 0.55, 6.0, z - 0.55, x + 0.55, 7.8, z + 0.55, brillante(c, punto=(x - 0.3, 7.6, z - 0.3), radio=0.5, emit=True))
    p.box(3.2, 4.6, 3.2, 12.8, 5.2, 12.8, cable)
    return p


@adorno('adorno_guirnalda', 'Guirnalda dorada', 'Gold garland', 'comun', tipo='guirnalda')
def guirnalda():
    p = Prop('x', '', '')
    for k in range(14):
        a = 2 * math.pi * k / 14
        r = 3.6 + 0.6 * math.sin(a * 3)
        x, z = 8 + r * math.cos(a), 8 + r * math.sin(a)
        y = 5 + (k % 2) * 0.4
        p.box(x - 0.9, y, z - 0.9, x + 0.9, y + 1.6, z + 0.9, oro(k))
    return p


@adorno('punta_estrella', 'Estrella para la punta', 'Tree-top star', 'raro', ancla=0, tipo='punta')
def estrella():
    p = Prop('x', '', '')
    e = brillante('#ffd84a', '#e0a020', punto=(7.0, 9.5, 7.0), radio=1.2, emit=True)
    p.box(7.25, 0, 7.25, 8.75, 2.5, 8.75, oro())                           # el tubito que entra en la punta
    p.box(7.0, 2.5, 7.25, 9.0, 15.5, 8.75, e)                              # rayo de arriba a abajo
    p.box(2.5, 8.0, 7.25, 13.5, 10.0, 8.75, e)                             # rayo de lado a lado
    p.box(4.2, 8.3, 7.4, 11.8, 9.7, 8.6, e, rot=('z', 45, (8, 9, 8)))     # rayos cortos en diagonal
    p.box(4.2, 8.3, 7.4, 11.8, 9.7, 8.6, e, rot=('z', -45, (8, 9, 8)))
    p.box(6.25, 7.25, 6.75, 9.75, 10.75, 9.25, e)                          # el centro
    return p


@adorno('punta_angel', 'Ángel para la punta', 'Tree-top angel', 'raro', ancla=0, tipo='punta')
def angel():
    p = Prop('x', '', '')
    tunica = tela('#f6f4fa', 0.06)
    p.box(7.25, 0, 7.25, 8.75, 1.5, 8.75, oro())
    p.box(5.5, 1.5, 5.5, 10.5, 4.5, 10.5, tunica)                          # la túnica en tres pisos
    p.box(6.0, 4.5, 6.0, 10.0, 7.5, 10.0, tunica)
    p.box(6.6, 7.5, 6.6, 9.4, 9.5, 9.4, tunica)
    p.box(5.6, 4.0, 5.6, 10.4, 4.5, 10.4, oro(4))                          # orilla dorada
    p.box(6.9, 9.5, 6.9, 9.1, 11.7, 9.1, solid('#f2c8a0'))                 # cara

    def ojos(x, y, face):
        if face == 'north' and 10.4 < y < 10.9 and (7.4 < x < 7.8 or 8.2 < x < 8.6):
            return '#3a2a40'
        if face == 'north' and 9.9 < y < 10.2 and 7.8 < x < 8.2:
            return '#e07a7a'
        return None
    p.box(6.95, 9.55, 6.85, 9.05, 11.65, 6.9, con_dibujo(solid('#f2c8a0'), ojos))
    p.box(6.7, 11.2, 6.9, 9.3, 12.2, 9.4, solid('#f0c860'))               # pelo
    p.box(6.7, 9.6, 8.6, 9.3, 11.6, 9.4, solid('#f0c860'))
    aura = brillante('#ffe070', punto=(7, 13.4, 7), radio=0.6, emit=True)
    p.box(6.4, 13.0, 6.4, 9.6, 13.5, 7.0, aura)                            # la aureola
    p.box(6.4, 13.0, 9.0, 9.6, 13.5, 9.6, aura)
    p.box(6.4, 13.0, 7.0, 7.0, 13.5, 9.0, aura)
    p.box(9.0, 13.0, 7.0, 9.6, 13.5, 9.0, aura)
    ala = brillante('#ffffff', '#d8e0ec', punto=(4, 11, 9.5), radio=0.8)
    p.box(2.6, 5.0, 9.2, 7.2, 11.5, 9.9, ala, rot=('y', -22.5, (7.2, 8, 9.5)))   # alas
    p.box(8.8, 5.0, 9.2, 13.4, 11.5, 9.9, ala, rot=('y', 22.5, (8.8, 8, 9.5)))
    p.box(7.2, 7.0, 5.9, 8.8, 8.0, 6.6, solid('#f2c8a0'))                  # manitas juntas
    p.box(7.5, 8.0, 5.7, 8.5, 9.0, 6.3, brillante('#ffd84a', emit=True, punto=(7.7, 8.8, 5.8), radio=0.3))  # una estrellita
    return p


@adorno('punta_polar', 'Estrella polar de cristal', 'Crystal north star', 'epico', ancla=0, tipo='punta')
def polar():
    p = Prop('x', '', '')
    h = hielo()
    p.box(7.25, 0, 7.25, 8.75, 2.5, 8.75, plata())
    p.box(7.2, 2.5, 7.3, 8.8, 16, 8.7, h)                                   # rayos largos
    p.box(1.0, 8.5, 7.3, 15.0, 10.1, 8.7, h)
    p.box(7.3, 2.6, 7.2, 8.7, 15.9, 8.8, h, rot=('y', 45, (8, 9, 8)))       # y los mismos de lado (se ve de cualquier lado)
    p.box(4.0, 8.6, 7.4, 12.0, 10.0, 8.6, h, rot=('z', 45, (8, 9.3, 8)))   # rayos cortos
    p.box(4.0, 8.6, 7.4, 12.0, 10.0, 8.6, h, rot=('z', -45, (8, 9.3, 8)))
    p.box(6.0, 7.3, 6.0, 10.0, 11.3, 10.0, brillante('#f4fbff', '#a8dcff', punto=(6.6, 10.8, 6.0), radio=1.0, emit=True))
    return p


# ---------------------------------------------------------------- escribir

DISPLAY = {
    'gui': {'rotation': [20, -30, 0], 'translation': [0, -1.5, 0], 'scale': [1.25, 1.25, 1.25]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 1, 0], 'scale': [0.6, 0.6, 0.6]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    'head': {'rotation': [0, 0, 0], 'translation': [0, 8, 0], 'scale': [1, 1, 1]},
    'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 1.5, 1], 'scale': [0.7, 0.7, 0.7]},
    'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 1.5, 1], 'scale': [0.7, 0.7, 0.7]},
    'firstperson_righthand': {'rotation': [0, -30, 10], 'translation': [1.5, 2.5, 0], 'scale': [0.8, 0.8, 0.8]},
    'firstperson_lefthand': {'rotation': [0, -30, 10], 'translation': [1.5, 2.5, 0], 'scale': [0.8, 0.8, 0.8]},
}


def escribir():
    import json
    os.makedirs(os.path.join(A, 'models', 'item'), exist_ok=True)
    os.makedirs(os.path.join(A, 'textures', 'item'), exist_ok=True)
    filas = []
    for id_, es, en, rareza, ancla, tipo, fn in ADORNOS:
        p = fn()
        p.id = id_
        models, img, alfa = kit.build(p)
        img.save(os.path.join(A, 'textures', 'item', id_ + '.png'))
        j = {'credit': 'Navidad de Axelis - adornos del árbol', 'textures': {'0': f'navidad:item/{id_}', 'particle': f'navidad:item/{id_}'},
             'elements': models[None], 'display': DISPLAY}
        if p.render == 'translucent':
            j['render_type'] = 'minecraft:translucent'
        elif alfa:
            j['render_type'] = 'minecraft:cutout'
        if tipo == 'punta':
            d = dict(DISPLAY)
            d['gui'] = {'rotation': [20, -30, 0], 'translation': [0, 0, 0], 'scale': [0.95, 0.95, 0.95]}
            j['display'] = d
        with open(os.path.join(A, 'models', 'item', id_ + '.json'), 'w') as fh:
            json.dump(j, fh, indent=1)
        filas.append((id_, es, en, rareza, ancla, tipo))
    return filas


if __name__ == '__main__':
    for f in escribir():
        print(f)
