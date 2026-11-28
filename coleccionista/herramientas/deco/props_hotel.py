#!/usr/bin/env python3
"""Adornos y piezas del hotel del Coleccionista (el minijuego del principio del capítulo 1) y de su sala de espera.

Mismas reglas del kit: el modelo mira al NORTE (z = 0 es el frente); los de pared tienen la espalda en z = 16; los altos ponen sus cajas
en dos partes ('lower' y 'upper', cada una de 0 a 16).

  piano_pared     piano vertical viejo (el salón de baile)
  tubo_suelo      un tubo oxidado tirado en el piso, con cinta amarilla y negra: se puede agarrar (para las ratas)
  puas            una tabla con clavos para arriba (lastima al pisarla)
  botiquin        botiquín de pared (cura una vez) y botiquin_vacio (ya lo usaron)
  boca_tobogan    una esquina de la boca del tobogán (el clóset de la salida): cuatro forman el hoyo de colores
  colchon_brinco  colchón de rayas de colores donde se cae al salir del tobogán (rebota)
  monitor         tele vieja de la sala de espera (la imagen la dibuja el juego)
"""
import numpy as np

import kit
from kit import Prop, Ramp, solid
from props_celdas import pintado, tela as tela_vieja
from props_mansion import nogal, oro, hierro


def _colores(P, eje, paso, cols, mezcla=0.0, seed=0):
    """Rayas de colores a lo largo de un eje del modelo ('X', 'Y' o 'Z'), con un poco de ruido para que no se vea de plástico."""
    v = P[eje]
    i = (np.floor(v / paso).astype(int)) % len(cols)
    out = np.zeros(v.shape + (4,), np.float32)
    for k, c in enumerate(cols):
        out[i == k, :3] = kit.hexc(c)
    n = kit.fbm(P['X'], P['Y'], P['Z'], 3.0, seed)
    out[..., :3] *= (0.86 + 0.2 * n)[..., None]
    out[..., 3] = 255
    return out


ARCOIRIS = ['#c8242a', '#f2b81c', '#2f9a3c', '#2457b8']


def piano_pared():
    """Piano vertical: caja de nogal con molduras, teclado amarillento (con teclas que faltan), candeleros de latón y partituras."""
    p = Prop('piano_pared', 'Piano viejo', 'Old upright piano', kind='floor', sound='wood', seed=700, tall=True,
             shape=[[0, 0, 4, 16, 16, 16]], shape_upper=[[0, 0, 7, 16, 9, 16]])
    m = nogal(700)
    o = oro(701)
    marfil = kit.stone(Ramp('#b8ad8a', '#c9bf9c', '#d6ccab', '#e2d9ba'), scale=1.2, seed=702)
    negro = solid('#141110')
    papel = kit.parchment(ink=0.35, seed=703)
    p.at('lower')
    p.box(0, 0, 6, 16, 1.5, 16, m)                             # zócalo
    p.box(0.5, 1.5, 7, 15.5, 10, 16, m)                         # la caja de abajo
    p.box(1.5, 2.5, 6.6, 7.5, 9, 7, m, bevel=0.15)             # tableros
    p.box(8.5, 2.5, 6.6, 14.5, 9, 7, m, bevel=0.15)
    p.box(0, 1.5, 4, 1.2, 12, 7, m)                             # los costados que sostienen el teclado
    p.box(14.8, 1.5, 4, 16, 12, 7, m)
    p.box(1.2, 10, 4, 14.8, 11, 7, m)                           # el apoyo del teclado
    p.box(1.2, 11, 4.2, 14.8, 11.8, 7, marfil)                  # teclas blancas
    for i in range(12):
        x = 1.9 + i * 1.08
        if i % 7 in (2, 6):
            continue
        p.box(x, 11.8, 5.6, x + 0.55, 12.4, 7, negro)           # teclas negras
    p.box(5.2, 11.2, 4.1, 6.2, 11.85, 5.5, negro)               # un hueco (falta una tecla)
    p.box(0.5, 11.8, 7, 15.5, 16, 16, m)                        # el cuerpo de arriba
    p.box(1, 12.6, 6.2, 15, 15.6, 7, m, bevel=0.2)              # la tapa del atril
    p.box(3.2, 13.2, 5.6, 12.8, 16, 6.2, papel)                 # partitura
    p.box(4.6, 3.4, 6.2, 5.6, 4.4, 6.6, o)                      # pedales
    p.box(10.4, 3.4, 6.2, 11.4, 4.4, 6.6, o)
    p.at('upper')
    p.box(0.5, 0, 7, 15.5, 7, 16, m)
    p.box(0, 7, 6.6, 16, 8.4, 16, m)                            # cornisa
    p.box(3.2, 0, 5.6, 12.8, 2, 6.2, papel)
    for x0 in (1.0, 13.0):
        p.box(x0, 2.5, 5.8, x0 + 2, 3.2, 7, o)                  # candeleros
        p.box(x0 + 0.6, 3.2, 6.2, x0 + 1.4, 5.8, 6.8, solid('#d8d0b8'))
    return p


def tubo_suelo():
    """Un tubo de fierro tirado, oxidado, con dos vueltas de cinta amarilla y negra que brilla un poco (para que se note que se agarra)."""
    p = Prop('tubo_suelo', 'Tubo suelto', 'Loose pipe', kind='floor', sound='metal', seed=710, collide=False,
             shape=[[1, 0, 6, 15, 3, 10]])
    fierro = kit.metal(Ramp('#3b2a20', '#5a3a26', '#6e5040', '#7c6a5c', '#8a8880'), streak=0.7, seed=710, sparkle=0.02)

    def cinta(P):
        out = _colores(P, 'X', 0.75, ['#f2c414', '#1a1a1a'], seed=711)
        return out
    cinta.emit = True
    p.box(1, 0, 7, 15, 2.4, 9.4, fierro)
    p.box(0.6, -0.1, 6.6, 1.8, 2.8, 9.8, fierro)                # las roscas de las puntas
    p.box(14.2, -0.1, 6.6, 15.4, 2.8, 9.8, fierro)
    p.box(4, -0.15, 6.8, 6, 2.6, 9.6, cinta, emit=True)
    p.box(10, -0.15, 6.8, 12, 2.6, 9.6, cinta, emit=True)
    return p


def puas():
    """Tabla podrida con clavos largos para arriba (algunos chuecos) y manchas de sangre."""
    p = Prop('puas', 'Tabla con clavos', 'Nail board', kind='floor', sound='wood', seed=720, collide=False,
             shape=[[0, 0, 0, 16, 3, 16]])
    tabla = kit.wood(Ramp('#2e2016', '#3e2c1e', '#4c3828', '#5a4634'), grain=2.6, seams=3, seed=720)
    clavo = kit.metal(Ramp('#3a2a22', '#5a4a40', '#7a7068', '#9a948c'), seed=721)
    sangre = solid('#5a0a0c')
    p.box(0.5, 0, 0.5, 15.5, 1.2, 15.5, tabla)
    p.box(3, 1.2, 4, 7, 1.25, 7, sangre)
    p.box(9, 1.2, 10, 12, 1.25, 12.5, sangre)
    r = np.random.RandomState(720)
    for i in range(4):
        for j in range(4):
            x, z = 2 + i * 3.6 + r.uniform(-0.6, 0.6), 2 + j * 3.6 + r.uniform(-0.6, 0.6)
            h = r.uniform(2.4, 3.6)
            p.box(x, 1.2, z, x + 0.5, 1.2 + h, z + 0.5, clavo)
    return p


def _botiquin(vacio):
    pid = 'botiquin_vacio' if vacio else 'botiquin'
    es, en = ('Botiquín vacío', 'Empty first aid kit') if vacio else ('Botiquín', 'First aid kit')
    p = Prop(pid, es, en, kind='wall', sound='metal', seed=730, collide=False, light=0 if vacio else 4)
    caja = pintado('#d8d4c8', oxido=0.25, seed=730)
    rojo = solid('#b81c22')
    adentro = solid('#2a2622')
    p.box(3, 3, 12, 13, 13, 16, caja)                           # la caja pegada a la pared
    if vacio:
        p.box(3.2, 3.2, 12.2, 12.8, 12.8, 12.6, adentro)       # se ve el fondo
        p.box(2.2, 3, 4.2, 3, 13, 12, caja)                    # la tapa abierta de lado
        p.box(2.1, 6.5, 7, 2.2, 9.5, 9.5, rojo)
        p.box(4, 4, 12.6, 8, 4.6, 15, solid('#8a8478'))        # una venda que quedó tirada
    else:
        p.box(2.8, 2.8, 11.6, 13.2, 13.2, 12.2, caja)          # la tapa
        def cruz(P):
            out = np.zeros(P['X'].shape + (4,), np.float32)
            x, y = P['X'], P['Y']
            en = ((np.abs(x - 8) < 1.3) & (np.abs(y - 8) < 3.6)) | ((np.abs(y - 8) < 1.3) & (np.abs(x - 8) < 3.6))
            out[..., :3] = np.where(en[..., None], kit.hexc('#d4141c'), kit.hexc('#e8e4da'))
            out[..., 3] = 255
            return out
        cruz.emit = True
        p.box(3.4, 3.4, 11.5, 12.6, 12.6, 11.6, cruz, emit=True)
        p.box(6.5, 12.6, 11, 9.5, 13.6, 13, kit.metal(Ramp('#5a5a5e', '#7a7a80', '#9a9aa0'), seed=731))   # el asa
    return p


def botiquin():
    return _botiquin(False)


def botiquin_vacio():
    return _botiquin(True)


def boca_tobogan():
    """Una esquina de la boca del tobogán: el hoyo del clóset de la salida es de 2 x 2; esta esquina tiene muro al norte y al oeste (las
    otras tres se ponen giradas). Por dentro, anillos de colores que se van a lo negro, como un tubo muy largo visto desde arriba."""
    p = Prop('boca_tobogan', 'Boca del tobogán', 'Slide opening', kind='floor', sound='metal', seed=740,
             shape=[[0, 0, 0, 16, 1, 16]])

    def fondo(P):
        # el centro del hoyo es la esquina sureste de esta pieza (x 16, z 16)
        x, z = P['X'] - 16, P['Z'] - 16
        r = np.sqrt(x * x + z * z)
        out = np.zeros(x.shape + (4,), np.float32)
        anillo = (np.floor(r / 3.2).astype(int)) % 4
        for k, c in enumerate(ARCOIRIS):
            out[anillo == k, :3] = kit.hexc(c)
        # más oscuro hacia el centro (es muy hondo)
        osc = np.clip((r - 2) / 22, 0, 1) ** 1.6
        out[..., :3] *= osc[..., None]
        out[..., 3] = 255
        return out

    def pared(P):
        return _colores(P, 'Y', 2, ARCOIRIS, seed=741)
    borde = kit.metal(Ramp('#5a5a5e', '#7a7a80', '#a0a0a6', '#c0c0c6'), seed=742)
    p.box(0, 0, 0, 16, 0.5, 16, fondo, shade=False)
    p.box(0, 0.5, 0, 16, 15, 2, pared)
    p.box(0, 0.5, 2, 2, 15, 16, pared)
    p.box(0, 15, 0, 16, 16, 2.5, borde)                         # el borde de lámina
    p.box(0, 15, 2.5, 2.5, 16, 16, borde)
    return p


def colchon_brinco():
    """Colchón gordo de rayas de colores (como de brincolín), con costuras y botones."""
    p = Prop('colchon_brinco', 'Colchón de brincolín', 'Bouncy mattress', kind='floor', sound='wool', seed=750,
             shape=[[0, 0, 0, 16, 9, 16]])

    def rayas(P):
        out = _colores(P, 'X', 4, ARCOIRIS, seed=750)
        if P['face'] == 'up':
            x, z = P['X'] % 8, P['Z'] % 8
            boton = (np.abs(x - 4) < 0.7) & (np.abs(z - 4) < 0.7)
            out[boton, :3] *= 0.6
        return out
    p.box(0, 0, 0, 16, 8, 16, rayas, bevel=0.25)
    p.box(0.5, 8, 0.5, 15.5, 9, 15.5, rayas)
    return p


def monitor():
    """Tele vieja de bulbo (de las de vigilancia), gris, con su perilla y la luz roja de "en vivo"; la imagen la dibuja el juego."""
    p = Prop('monitor', 'Monitor', 'Monitor', kind='floor', sound='metal', seed=760, light=3,
             shape=[[1, 0, 2, 15, 13, 16]])
    caja = pintado('#4a4a46', oxido=0.15, seed=760)
    marco = pintado('#2a2a28', oxido=0.1, seed=761)
    vidrio = solid('#0a1410')
    p.box(1, 0, 2, 15, 13, 13, caja, bevel=0.2)
    p.box(3, 2, 13, 13, 11, 16, caja)                           # la parte de atrás (el bulbo)
    p.box(1.6, 2.4, 1.6, 12.4, 11.6, 2.1, marco)
    p.box(2.2, 3, 1.4, 11.8, 11, 1.7, vidrio)
    p.box(13, 8.5, 1.7, 14.2, 9.7, 2.1, solid('#9a9a90'))      # perillas
    p.box(13, 6, 1.7, 14.2, 7.2, 2.1, solid('#9a9a90'))
    luz = solid('#ff2a1a')
    luz.emit = True
    p.box(13.2, 3.2, 1.7, 14, 4, 2, luz, emit=True)
    p.box(1, 13, 7, 15, 13.4, 9, solid('#1a1a1a'))              # la antena de conejo (base)
    return p


def todos():
    return [piano_pared(), tubo_suelo(), puas(), botiquin(), botiquin_vacio(), boca_tobogan(), colchon_brinco(), monitor()]
