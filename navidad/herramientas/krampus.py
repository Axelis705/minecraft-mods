#!/usr/bin/env python3
"""El Krampus, hecho a mano (ya no es el Coloso pintado): encorvado, con el pelo negro-café largo y desgreñado (las orillas de abajo de
cada pieza son mechones con huecos), patas de cabra que se doblan para atrás con pezuñas, brazos larguísimos con garras, cara de
cuero con colmillos, ojos que brillan, una lengua roja larguísima que le cuelga hasta el pecho, barba de chivo, orejas caídas y dos
cuernos enormes que suben, se van para atrás y se enroscan. Una cadena cruzada en el pecho con tres cascabeles viejos, el saco de
costal en la espalda (con parches y amarrado arriba), la vara de abedul (un manojo de varitas con listón rojo) en la mano derecha y
la cadena con su gancho colgando de la izquierda.

Texturas (mismo acomodo): krampus.png (ojos ámbar), krampus_f2.png (furia: ojos rojos, más oscuro), krampus_f3.png (la sombra del
apagón: casi negro con un brillo morado y ojos rojos). Escondido en lo oscuro, el código lo pinta todo negro menos los ojos.

Animaciones (los tiempos coinciden con los golpes del código, en ticks): idle, walk, run, varazos (golpes 0.5/0.9/1.4 s), cadena
(lanza a 0.7 s), pisoton (0.6), lanza (carbones, 0.75), rugido (0.4), agarre (agarra a 0.9 y lo mete al saco), giro (0.8), soplido
(0.9), emboscada (0.6), fase (furia), apagon (apaga todo a 1.6), aturdido, risa, hurt, death.
Uso: python3 herramientas/krampus.py"""
import hashlib
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
from geo import Anim, Creature, save_anims  # noqa: E402

A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')
S = "easeInOutSine"
C = "catmullrom"


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def h(*k):
    """Un número fijo (0..999) para cada combinación: así los mechones no cambian entre texturas."""
    return int(hashlib.md5(repr(k).encode()).hexdigest()[:6], 16) % 1000


# ------------------------------------------------------------------ paletas (una por textura)

BASE = dict(
    fur=(48, 34, 27), fur_d=(27, 18, 15), fur_l=(84, 60, 44), gris=(96, 88, 82),
    piel=(64, 40, 35), piel_d=(38, 23, 21), cuerno_d=(86, 74, 60), cuerno=(176, 160, 128), cuerno_l=(226, 214, 182),
    pezuna=(26, 22, 22), pezuna_l=(64, 58, 56), ojo=(255, 178, 46), ojo2=(255, 236, 150),
    lengua=(196, 42, 54), lengua_d=(140, 22, 34), diente=(232, 224, 198),
    costal=(152, 120, 78), costal_d=(116, 88, 54), costal_l=(180, 150, 102), parche=(110, 82, 62), cuerda=(196, 166, 112),
    cadena=(124, 124, 132), cadena_d=(66, 66, 74), laton=(178, 138, 58), laton_d=(112, 84, 34),
    abedul=(232, 230, 220), abedul_d=(40, 38, 36), liston=(176, 24, 32), boca=(40, 10, 12))
FURIA = dict(BASE, ojo=(255, 44, 30), ojo2=(255, 170, 140), fur=(42, 28, 23), fur_l=(80, 46, 36), lengua=(214, 30, 44))


def sombra(p):
    out = {}
    for k, v in p.items():
        if k in ('ojo', 'ojo2'):
            out[k] = v
        else:
            g = (v[0] + v[1] + v[2]) / 3
            out[k] = (int(g * 0.22 + 10), int(g * 0.18 + 6), int(g * 0.30 + 18))
    out['ojo'], out['ojo2'] = (255, 30, 24), (255, 140, 120)
    return out


P = dict(BASE)


# ------------------------------------------------------------------ pintores

def pelo(flecos=3, claro=0.0, cara_abajo=True):
    """Pelo desgreñado: vetas verticales con puntas claras y, abajo, mechones de distinto largo (los huecos son transparentes)."""
    def f(face, x, y, w, hh):
        if face == 'bottom':
            return None if flecos else P['fur_d']
        if face == 'top':
            return P['fur_d'] if (x * 3 + y * 5) % 7 == 0 else P['fur']
        r = hh - 1 - y  # filas desde abajo
        if flecos and r < flecos:
            corte = h(face, x, w, hh) % (flecos + 1)
            if r < corte:
                return None
        k = h('v', face, x) % 9
        col = P['fur_l'] if k == 0 else P['fur_d'] if k in (1, 2) else P['fur']
        if k == 3 and y % 4 == 1:
            col = P['gris']
        if claro:
            col = mix(col, P['fur_l'], claro * (1 - y / max(1, hh)))
        if flecos and r < flecos + 2 and k > 5:
            col = mix(col, P['fur_l'], 0.35)
        return col
    return f


def piel(face, x, y, w, hh):
    return P['piel_d'] if (x + y * 3) % 5 == 0 else P['piel']


def cara(face, x, y, w, hh):
    """La cabeza: cuero oscuro con el ceño marcado; en el frente, las cuencas hundidas (los ojos van en su propio plano que brilla)."""
    if face == 'front':
        if y in (3, 4) and (x in (1, 2) or x in (w - 3, w - 2)):
            return P['boca']                                  # cuencas
        if y == 2 and 1 <= x <= w - 2:
            return P['piel_d']                                # ceño
        if y >= hh - 2 and x in (2, w - 3):
            return P['piel_d']
        return P['piel'] if (x + y) % 4 else mix(P['piel'], P['fur'], 0.5)
    if face == 'top':
        return P['fur_d']
    return pelo(0)(face, x, y, w, hh) if y < 4 else P['piel']


def hocico(face, x, y, w, hh):
    if face == 'front':
        if y == 1 and x in (1, w - 2):
            return P['boca']                                  # fosas
        if y == hh - 1:
            return P['boca']                                  # el filo de la boca
        return P['piel_d'] if x in (0, w - 1) else P['piel']
    if face == 'bottom':
        return P['boca']
    return P['piel_d']


def quijada(face, x, y, w, hh):
    if face == 'top':
        return P['lengua_d'] if 1 <= x <= w - 2 and y >= 1 else P['diente']   # la boca por dentro con dientes en la orilla
    if face == 'front' and y == 0 and x in (0, 2, w - 3, w - 1):
        return P['diente']
    return P['piel_d'] if face == 'bottom' else P['piel']


def diente(face, x, y, w, hh):
    return P['diente'] if y < hh - 1 or face != 'front' else mix(P['diente'], P['piel'], 0.4)


def lengua(face, x, y, w, hh):
    if face in ('front', 'back') and w >= 2 and x == w // 2 - (0 if w % 2 else 1) and y % 2 == 0:
        return P['lengua_d']
    return P['lengua'] if (x + y) % 3 else mix(P['lengua'], P['lengua_d'], 0.4)


def cuerno(t0, t1):
    """Cuerno con anillos: del color de la base (t0) a la punta (t1), con rayitas oscuras."""
    def f(face, x, y, w, hh):
        t = t0 + (t1 - t0) * (1 - y / max(1, hh - 1))
        base = mix(P['cuerno_d'], P['cuerno'], t * 1.6) if t < 0.6 else mix(P['cuerno'], P['cuerno_l'], (t - 0.6) / 0.4)
        if y % 2 == 0 and face not in ('top', 'bottom'):
            base = mix(base, P['cuerno_d'], 0.35)
        return base
    return f


def pezuna(face, x, y, w, hh):
    if face == 'front' and x in (w // 2 - 1, w // 2) and y >= 0:
        return P['fur_d'] if x == w // 2 - 1 else P['pezuna']      # la pezuña partida
    if face == 'top':
        return P['fur_d']
    return P['pezuna_l'] if y == 0 and face != 'bottom' else P['pezuna']


def costal(face, x, y, w, hh):
    """Costal tejido con un parche cosido y la orilla de arriba más oscura."""
    if face == 'bottom':
        return P['costal_d']
    if face in ('back', 'right', 'left') and 2 <= x <= 5 and 3 <= y <= 6:
        borde = x in (2, 5) or y in (3, 6)
        return P['cuerda'] if borde and (x + y) % 2 == 0 else P['parche']
    col = P['costal'] if (x + y) % 2 == 0 else P['costal_d'] if (x * 3 + y) % 5 == 0 else P['costal_l']
    if y < 2 and face != 'top':
        col = mix(col, P['costal_d'], 0.5)
    return col


def cuerda(face, x, y, w, hh):
    return P['cuerda'] if (x + y) % 3 else mix(P['cuerda'], P['costal_d'], 0.5)


def cadena(face, x, y, w, hh):
    """Eslabones: uno de frente (claro) y uno de canto (oscuro), alternados."""
    if face in ('top', 'bottom'):
        return P['cadena_d']
    k = y % 4
    if k == 0:
        return P['cadena_d']
    return P['cadena'] if (k in (1, 3)) == (face in ('front', 'back')) else mix(P['cadena'], P['cadena_d'], 0.5)


def laton(face, x, y, w, hh):
    if face == 'bottom':
        return P['boca']
    return P['laton'] if (x + y) % 3 else P['laton_d']


def abedul(face, x, y, w, hh):
    if face in ('top', 'bottom'):
        return P['abedul_d']
    return P['abedul_d'] if h('a', face, x, y) % 6 == 0 else P['abedul']


def liston(face, x, y, w, hh):
    return P['liston'] if (x + y) % 4 else mix(P['liston'], P['boca'], 0.4)


def oreja(face, x, y, w, hh):
    if face == 'bottom':
        return mix(P['piel'], P['lengua_d'], 0.4) if 1 <= x <= w - 2 else P['piel_d']   # por dentro, rosado oscuro
    return pelo(0)(face, x, y, w, hh)


def garra(face, x, y, w, hh):
    return mix(P['cuerno_l'], P['cuerno_d'], y / max(1, hh - 1) * 0.3) if y < hh - 1 else P['cuerno_l']


def ojos(face, x, y, w, hh):
    """Dos ojos rasgados que brillan (el plano va pegado a las cuencas)."""
    izq = x in (0, 1, 2) and (y == 1 or (y == 0 and x == 2))
    der = x in (w - 3, w - 2, w - 1) and (y == 1 or (y == 0 and x == w - 3))
    if izq or der:
        return (P['ojo2'] if (x in (1, w - 2) and y == 1) else P['ojo']) + (255,)
    return None


def solido(k):
    return lambda face, x, y, w, hh: P[k]


# ------------------------------------------------------------------ el modelo

def modelo():
    c = Creature('geometry.navidad.krampus', 256, 256, seed=1205)
    c.bone('root', [0, 0, 0])
    c.bone('hips', [0, 22, 1], 'root')
    c.part('hips', [-6, 18, -3], [12, 8, 9], pelo(3))
    c.bone('tail', [0, 24, 6], 'hips', rotation=[-30, 0, 0])
    c.part('tail', [-1.5, 20, 5], [3, 5, 3], pelo(2, 0.4))

    for lado, s in (('r', -1), ('l', 1)):
        x = 3.5 * s
        leg = 'leg_' + lado
        c.bone(leg, [x, 22, 1], 'hips', rotation=[-22, 0, 0])
        c.part(leg, [x - 3, 13, -3], [6, 9, 7], pelo(2))                       # muslo peludo (el fleco cuelga sobre la espinilla)
        c.bone(leg + '_low', [x, 14, 1], leg, rotation=[46, 0, 0])
        c.part(leg + '_low', [x - 1.5, 6, 0], [3, 9, 4], pelo(0, 0.2))         # espinilla de chivo, para atrás
        c.bone(leg + '_foot', [x, 7, 1.5], leg + '_low', rotation=[-24, 0, 0])
        c.part(leg + '_foot', [x - 1, 2, -1], [2, 5, 3], pelo(0))              # el tobillo, para adelante
        c.part(leg + '_foot', [x - 2, 0, -2.5], [4, 2, 4], pezuna)

    c.bone('torso', [0, 25, 1], 'hips', rotation=[24, 0, 0])
    c.part('torso', [-8, 24, -5], [16, 13, 11], pelo(3, 0.15))
    c.part('torso', [-9, 34, -4], [18, 6, 10], pelo(2))                        # la joroba de los hombros
    c.part('torso', [-6, 26, -6], [12, 10, 1], pelo(4, 0.35))                  # pelo largo del pecho
    c.part('torso', [-7, 40, -4], [2, 1, 10], cuerda)                          # las cuerdas del saco sobre los hombros
    c.part('torso', [5, 40, -4], [2, 1, 10], cuerda)

    # la cadena cruzada en el pecho con tres cascabeles
    c.bone('cadena_pecho', [0, 31, -6.5], 'torso', rotation=[0, 0, -42])
    c.part('cadena_pecho', [-0.5, 21, -7.2], [1, 21, 1], cadena)
    for i, yy in enumerate((24.5, 29.5, 34.5)):
        b = f'cascabel_{i + 1}'
        c.bone(b, [0, yy, -7.2], 'cadena_pecho', rotation=[0, 0, 42])
        c.part(b, [-1, yy - 2.6, -8.2], [2, 2, 2], laton)
        c.part(b, [-0.5, yy - 3.2, -7.7], [1, 1, 1], solido('laton_d'))

    # el saco
    c.bone('saco', [0, 36, 6], 'torso')
    c.part('saco', [-6, 23, 6], [12, 13, 9], costal)
    c.part('saco', [-3, 36, 8], [6, 3, 5], costal)
    c.part('saco', [-3.5, 36.5, 7.5], [7, 1, 6], cuerda)
    c.part('saco', [-2, 39, 8.5], [4, 2, 4], costal)

    # cuello, cabeza, quijada, lengua, cuernos
    c.bone('neck', [0, 37, -3], 'torso', rotation=[-10, 0, 0])
    c.part('neck', [-3.5, 33, -8], [7, 7, 6], pelo(2, 0.25))
    c.bone('head', [0, 37, -7], 'neck', rotation=[-12, 0, 0])
    c.part('head', [-4.5, 33, -15], [9, 8, 8], cara)
    c.part('head', [-5, 40, -14.5], [10, 3, 9], pelo(2))                       # greña de arriba (el fleco cae sobre la frente)
    c.part('head', [-3, 33, -18], [6, 4, 3], hocico)
    c.part('head', [-1, 36, -16], [2, 3, 1], piel)                             # el puente de la nariz
    c.part('head', [-2.5, 30.5, -17.6], [1, 3, 1], diente)                     # colmillos
    c.part('head', [1.5, 30.5, -17.6], [1, 3, 1], diente)
    c.bone('ojos', [0, 37, -15.06], 'head')
    c.plane('ojos', [-3.5, 36.5, -15.08], 7, 2, ojos)
    for lado, s in (('r', -1), ('l', 1)):
        b = 'oreja_' + lado
        c.bone(b, [4.5 * s, 38, -10], 'head', rotation=[0, 0, -28 * s])
        c.part(b, [4.5 * s - (0 if s > 0 else 5), 37.5, -11.5], [5, 1, 3], oreja)
    c.bone('jaw', [0, 33, -10], 'head')
    c.part('jaw', [-3, 31, -17.5], [6, 2, 7], quijada)
    c.part('jaw', [-2, 26, -15], [4, 5, 2], pelo(2, 0.5))                      # barba de chivo
    # la lengua: sale de la boca y cuelga hasta el pecho (cada pedazo cuelga del anterior)
    c.bone('lengua_1', [0, 32, -16], 'jaw', rotation=[-6, 0, 0])
    c.part('lengua_1', [-1, 31.5, -20], [2, 1, 4], lengua)
    c.bone('lengua_2', [0, 32, -19.5], 'lengua_1', rotation=[12, 0, 0])
    c.part('lengua_2', [-1, 26, -20], [2, 6, 1], lengua)
    c.bone('lengua_3', [0, 26, -19.5], 'lengua_2', rotation=[-8, 0, 0])
    c.part('lengua_3', [-1, 21, -20], [2, 5, 1], lengua)
    c.bone('lengua_4', [0, 21, -19.5], 'lengua_3', rotation=[-10, 0, 0])
    c.part('lengua_4', [-0.5, 19, -20], [1, 2, 1], lengua)

    # cuernos: segmentos apilados derechos en reposo; cada hueso gira un poco más (así se enroscan solos)
    tam = [(3, 5), (2.6, 5), (2.2, 4), (1.8, 4), (1.4, 3), (1, 3)]
    giros = [(-6, 24), (-14, 8), (-18, 0), (-14, -8), (-2, -12), (18, -10)]
    for lado, s in (('r', -1), ('l', 1)):
        y = 40.5
        padre = 'head'
        for i, ((w, L), (gx, gz)) in enumerate(zip(tam, giros)):
            b = f'cuerno_{lado}_{i + 1}'
            c.bone(b, [3 * s, y, -11], padre, rotation=[gx, 0, gz * s])
            c.part(b, [3 * s - w / 2, y, -11 - w / 2], [w, L, w], cuerno(i / len(tam), (i + 1) / len(tam)))
            padre = b
            y += L

    # brazos larguísimos
    for lado, s in (('r', -1), ('l', 1)):
        x = 9.5 * s
        arm = 'arm_' + lado
        c.bone(arm, [x, 36, 0], 'torso', rotation=[-16, 0, -6 * s])
        c.part(arm, [x - 3, 23, -3], [6, 14, 6], pelo(3))
        c.bone(arm + '_low', [x, 25, 0], arm, rotation=[-12, 0, 0])
        c.part(arm + '_low', [x - 2, 12, -2], [4, 14, 4], pelo(2, 0.1))
        c.bone('mano_' + lado, [x, 12, 0], arm + '_low')
        c.part('mano_' + lado, [x - 2.5, 7, -2.5], [5, 5, 5], piel)
        for k, dz in enumerate((-2.5, -1, 0.5)):
            c.part('mano_' + lado, [x - 1.5 + k, 4, dz - 0.5], [1, 3, 1], garra)
        c.part('mano_' + lado, [x + 2 * s - 0.5, 5, -2.5], [1, 3, 1], garra)   # el pulgar

    # la vara de abedul en la derecha (apunta para adelante y abajo desde el puño)
    c.bone('vara', [-9.5, 9, -1], 'mano_r', rotation=[-70, 0, 0])
    for k, (dx, dz, L) in enumerate(((-1, 0, 20), (0, -1, 21), (1, 0, 19), (0, 1, 20), (0, 0, 22))):
        c.part('vara', [-10 + dx, 9 - L + 6, -1.5 + dz], [1, L, 1], abedul)
    c.part('vara', [-11.5, 6, -3], [3, 2, 3], liston)

    # la cadena con gancho colgando de la izquierda
    c.bone('cadena_mano', [9.5, 7, -0.5], 'mano_l')
    c.part('cadena_mano', [9, 1, -1], [1, 7, 1], cadena)
    c.bone('gancho', [9.5, 1, -0.5], 'cadena_mano')
    c.part('gancho', [9, -1, -1], [1, 2, 1], solido('cadena'))
    c.part('gancho', [9, -2, -2.5], [1, 1, 3], solido('cadena'))
    c.part('gancho', [9, -1, -3], [1, 2, 1], solido('cadena_d'))

    c.glow('ojos')
    return c


def construir():
    global P
    geo = os.path.join(A, 'geo', 'krampus.geo.json')
    tex = os.path.join(A, 'textures', 'entity')
    for nombre, pal in (('krampus', BASE), ('krampus_f2', FURIA), ('krampus_f3', sombra(FURIA))):
        P = dict(pal)
        modelo().build(geo, os.path.join(tex, nombre + '.png'))



# ------------------------------------------------------------------ animaciones

def cyc(vals, L, fase=0.0, ease=C):
    n = len(vals)
    out = {}
    for i, v in enumerate(vals):
        out[round(((i / n + fase) % 1.0) * L, 4)] = (v, ease)
    t0 = min(out)
    out[round(L if t0 == 0 else t0 + L, 4)] = out[t0]
    if t0 > 0:
        out[0.0] = out[t0]
    return out


def lados(a, base, fn):
    for lado, s in (('r', -1), ('l', 1)):
        fn(a, base + '_' + lado, s)


def animaciones():
    an = []
    # quieto: respira pesado, mueve la cabeza como olfateando, la lengua y los cascabeles se mecen
    L = 4.0
    a = Anim('idle', L, True)
    a.rot('torso', cyc([[0, 0, 0], [3, 0, 0]], L)).pos('torso', cyc([[0, 0, 0], [0, 0.6, 0]], L))
    a.rot('head', cyc([[0, 0, 0], [-4, 10, 4], [2, 0, 0], [-3, -12, -4]], L))
    a.rot('jaw', cyc([[0, 0, 0], [8, 0, 0], [2, 0, 0], [10, 0, 0]], L))
    a.rot('lengua_2', cyc([[0, 0, 4], [6, 0, -4]], L / 2)).rot('lengua_3', cyc([[0, 0, -6], [8, 0, 6]], L / 2, 0.25))
    for i in (1, 2, 3):
        a.rot(f'cascabel_{i}', cyc([[0, 0, 10], [0, 0, -10]], L / 2, i * 0.2))
    a.rot('saco', cyc([[0, 0, 2], [2, 0, -2]], L))
    a.rot('tail', cyc([[0, 0, 0], [10, 0, 20], [0, 0, 0], [10, 0, -20]], L / 2))
    a.rot('arm_r', cyc([[0, 0, 2], [-4, 0, 4]], L)).rot('arm_l', cyc([[0, 0, -2], [-4, 0, -4]], L, 0.3))
    a.rot('cadena_mano', cyc([[6, 0, 4], [-6, 0, -4]], L / 2)).rot('gancho', cyc([[8, 0, 0], [-8, 0, 0]], L / 2, 0.2))
    a.rot('oreja_r', cyc([[0, 0, 0], [0, 0, 8]], L)).rot('oreja_l', cyc([[0, 0, 0], [0, 0, -8]], L, 0.5))
    an.append(a)

    # caminar: acecha, encorvado, los brazos se arrastran; las pezuñas suenan
    L = 1.4
    a = Anim('walk', L, True)
    for lado, ph in (('r', 0.0), ('l', 0.5)):
        a.rot('leg_' + lado, cyc([[-26, 0, 0], [-6, 0, 0], [18, 0, 0], [0, 0, 0]], L, ph))
        a.rot('leg_' + lado + '_low', cyc([[16, 0, 0], [8, 0, 0], [-6, 0, 0], [34, 0, 0]], L, ph))
        a.rot('leg_' + lado + '_foot', cyc([[10, 0, 0], [-4, 0, 0], [-14, 0, 0], [-20, 0, 0]], L, ph))
        a.rot('arm_' + lado, cyc([[22, 0, 0], [-16, 0, 0]], L, ph + 0.5))
        a.rot('arm_' + lado + '_low', cyc([[-6, 0, 0], [-24, 0, 0]], L, ph + 0.5))
    a.rot('hips', cyc([[0, 6, 3], [0, -6, -3]], L)).pos('hips', cyc([[0, -1, 0], [0, 0.5, 0], [0, -1, 0], [0, 0.5, 0]], L))
    a.rot('torso', cyc([[4, -6, -2], [4, 6, 2]], L))
    a.rot('head', cyc([[0, 6, 0], [0, -6, 0]], L, 0.25))
    a.rot('saco', cyc([[0, 0, 4], [0, 0, -4]], L, 0.1))
    a.rot('lengua_2', cyc([[0, 0, 8], [0, 0, -8]], L, 0.3))
    for i in (1, 2, 3):
        a.rot(f'cascabel_{i}', cyc([[0, 0, 18], [0, 0, -18]], L, i * 0.15))
    a.rot('cadena_mano', cyc([[18, 0, 0], [-10, 0, 0]], L, 0.6))
    a.sound(0.05, 'navidad:krampus_pezuna').sound(L / 2 + 0.05, 'navidad:krampus_pezuna')
    an.append(a)

    # correr (embestidas del apagón): galope agachado
    L = 0.7
    a = Anim('run', L, True)
    for lado, ph in (('r', 0.0), ('l', 0.5)):
        a.rot('leg_' + lado, cyc([[-40, 0, 0], [-10, 0, 0], [30, 0, 0], [0, 0, 0]], L, ph))
        a.rot('leg_' + lado + '_low', cyc([[20, 0, 0], [10, 0, 0], [-10, 0, 0], [50, 0, 0]], L, ph))
        a.rot('arm_' + lado, cyc([[40, 0, 0], [-30, 0, 0]], L, ph + 0.5))
        a.rot('arm_' + lado + '_low', cyc([[-10, 0, 0], [-40, 0, 0]], L, ph + 0.5))
    a.rot('torso', cyc([[18, 0, 0], [12, 0, 0]], L / 2)).pos('hips', cyc([[0, -2, 0], [0, 0.5, 0]], L / 2))
    a.rot('head', cyc([[-14, 0, 0], [-8, 0, 0]], L / 2))
    a.sound(0.05, 'navidad:krampus_pezuna').sound(L / 2 + 0.05, 'navidad:krampus_pezuna')
    an.append(a)

    # varazos: tres golpes con la vara (0.5, 0.9 y 1.4 s)
    a = Anim('varazos', 1.6, False)
    a.rot('arm_r', {0: [0, 0, 0], 0.32: ([-150, 0, -40], 'easeOutQuad'), 0.5: ([-40, 0, 20], 'easeInQuart'), 0.62: ([-50, 0, 30], S),
                    0.78: ([-110, 0, 40], 'easeOutQuad'), 0.9: ([-50, 0, -40], 'easeInQuart'), 1.05: ([-60, 0, -40], S),
                    1.22: ([-175, 0, 0], 'easeOutQuad'), 1.4: ([-30, 0, 0], 'easeInQuart'), 1.6: [0, 0, 0]})
    a.rot('arm_r_low', {0: [0, 0, 0], 0.32: ([-50, 0, 0], S), 0.5: ([-5, 0, 0], S), 0.78: ([-40, 0, 0], S), 0.9: ([-5, 0, 0], S),
                        1.22: ([-60, 0, 0], S), 1.4: ([0, 0, 0], S), 1.6: [0, 0, 0]})
    a.rot('torso', {0: [0, 0, 0], 0.32: ([-6, 24, 0], S), 0.5: ([8, -26, 0], 'easeInQuart'), 0.78: ([0, -20, 0], S),
                    0.9: ([8, 24, 0], 'easeInQuart'), 1.22: ([-12, 0, 0], S), 1.4: ([16, 0, 0], 'easeInQuart'), 1.6: [0, 0, 0]})
    a.rot('head', {0: [0, 0, 0], 0.5: ([10, 0, 0], S), 0.9: ([10, 0, 0], S), 1.4: ([14, 0, 0], S), 1.6: [0, 0, 0]})
    a.rot('jaw', {0: [0, 0, 0], 0.4: ([24, 0, 0], S), 1.5: ([24, 0, 0], S), 1.6: [0, 0, 0]})
    a.rot('vara', {0: [0, 0, 0], 0.5: ([20, 0, 0], S), 0.9: ([20, 0, 0], S), 1.4: ([25, 0, 0], S), 1.6: [0, 0, 0]})
    for t in (0.42, 0.82, 1.32):
        a.sound(t, 'navidad:krampus_latigo')
    a.event(0.5, 'varazo').event(0.9, 'varazo').event(1.4, 'varazo')
    an.append(a)

    # cadena: la hace girar sobre la cabeza y la avienta (0.7 s)
    a = Anim('cadena', 1.4, False)
    a.rot('arm_l', {0: [0, 0, 0], 0.15: ([-160, 0, 20], S), 0.3: ([-160, 40, 30], S), 0.45: ([-160, -40, 30], S), 0.6: ([-170, 0, 20], S),
                    0.7: ([-70, 0, 0], 'easeInQuart'), 1.0: ([-60, 0, 0], S), 1.4: [0, 0, 0]})
    a.rot('arm_l_low', {0: [0, 0, 0], 0.3: ([-20, 0, 0], S), 0.7: ([0, 0, 0], S), 1.4: [0, 0, 0]})
    a.rot('cadena_mano', {0: [0, 0, 0], 0.15: ([0, 0, 60], S), 0.3: ([0, 0, -60], S), 0.45: ([0, 0, 60], S), 0.6: ([0, 0, -60], S),
                          0.7: ([-80, 0, 0], S), 1.0: ([-60, 0, 0], S), 1.4: [0, 0, 0]})
    a.rot('torso', {0: [0, 0, 0], 0.6: ([-10, -14, 0], S), 0.7: ([12, 10, 0], 'easeInQuart'), 1.4: [0, 0, 0]})
    a.rot('head', {0: [0, 0, 0], 0.6: ([-14, 0, 0], S), 0.75: ([6, 0, 0], S), 1.4: [0, 0, 0]})
    a.sound(0.15, 'navidad:krampus_cadenas').sound(0.68, 'navidad:krampus_cadenas')
    an.append(a)

    # pisotón de pezuña (0.6 s)
    a = Anim('pisoton', 1.1, False)
    a.rot('leg_r', {0: [0, 0, 0], 0.4: ([-70, 0, 0], 'easeOutQuad'), 0.6: ([-8, 0, 0], 'easeInQuart'), 1.1: [0, 0, 0]})
    a.rot('leg_r_low', {0: [0, 0, 0], 0.4: ([70, 0, 0], S), 0.6: ([6, 0, 0], S), 1.1: [0, 0, 0]})
    a.rot('torso', {0: [0, 0, 0], 0.4: ([-10, 0, 6], S), 0.6: ([14, 0, -3], S), 1.1: [0, 0, 0]})
    a.rot('arm_r', {0: [0, 0, 0], 0.4: ([-30, 0, -40], S), 0.6: ([0, 0, -10], S), 1.1: [0, 0, 0]})
    a.rot('arm_l', {0: [0, 0, 0], 0.4: ([-30, 0, 40], S), 0.6: ([0, 0, 10], S), 1.1: [0, 0, 0]})
    a.rot('jaw', {0: [0, 0, 0], 0.55: ([28, 0, 0], S), 0.9: ([20, 0, 0], S), 1.1: [0, 0, 0]})
    a.sound(0.6, 'navidad:krampus_pezuna')
    an.append(a)

    # lanza carbones: mete la mano al saco por encima del hombro y los avienta (0.75 s)
    a = Anim('lanza', 1.4, False)
    a.rot('arm_r', {0: [0, 0, 0], 0.35: ([-170, 0, 30], 'easeOutQuad'), 0.55: ([-190, 0, 30], S), 0.75: ([-50, 0, -10], 'easeInQuart'),
                    1.0: ([-40, 0, -10], S), 1.4: [0, 0, 0]})
    a.rot('arm_r_low', {0: [0, 0, 0], 0.35: ([-90, 0, 0], S), 0.55: ([-100, 0, 0], S), 0.75: ([-5, 0, 0], S), 1.4: [0, 0, 0]})
    a.rot('torso', {0: [0, 0, 0], 0.45: ([-10, 20, 0], S), 0.75: ([12, -20, 0], 'easeInQuart'), 1.4: [0, 0, 0]})
    a.rot('saco', {0: [0, 0, 0], 0.5: ([10, 0, -10], S), 0.7: ([-4, 0, 4], S), 1.4: [0, 0, 0]})
    a.sound(0.45, 'navidad:krampus_saco')
    an.append(a)

    # rugido (0.4 s): echa la cabeza para atrás, abre los brazos, saca la lengua
    a = Anim('rugido', 2.2, False)
    a.rot('torso', {0: [0, 0, 0], 0.4: ([-22, 0, 0], S), 1.8: ([-24, 0, 0], S), 2.2: [0, 0, 0]})
    a.rot('head', {0: [0, 0, 0], 0.4: ([-30, 0, 0], S), 0.6: ([-26, 0, 4], S), 0.8: ([-30, 0, -4], S), 1.0: ([-26, 0, 4], S),
                   1.2: ([-30, 0, -4], S), 1.8: ([-28, 0, 0], S), 2.2: [0, 0, 0]})
    a.rot('jaw', {0: [0, 0, 0], 0.4: ([40, 0, 0], 'easeOutBack'), 1.8: ([38, 0, 0], S), 2.2: [0, 0, 0]})
    a.rot('lengua_1', {0: [0, 0, 0], 0.4: ([-30, 0, 0], S), 1.8: ([-30, 0, 0], S), 2.2: [0, 0, 0]})
    a.rot('lengua_2', {0: [0, 0, 0], 0.5: ([-40, 0, 10], S), 0.8: ([-30, 0, -10], S), 1.1: ([-40, 0, 10], S), 1.8: ([-30, 0, 0], S),
                       2.2: [0, 0, 0]})
    a.rot('arm_r', {0: [0, 0, 0], 0.4: ([-30, 0, -80], 'easeOutBack'), 1.8: ([-28, 0, -78], S), 2.2: [0, 0, 0]})
    a.rot('arm_l', {0: [0, 0, 0], 0.4: ([-30, 0, 80], 'easeOutBack'), 1.8: ([-28, 0, 78], S), 2.2: [0, 0, 0]})
    a.rot('arm_r_low', {0: [0, 0, 0], 0.4: ([-50, 0, 0], S), 1.8: ([-50, 0, 0], S), 2.2: [0, 0, 0]})
    a.rot('arm_l_low', {0: [0, 0, 0], 0.4: ([-50, 0, 0], S), 1.8: ([-50, 0, 0], S), 2.2: [0, 0, 0]})
    for i in (1, 2, 3):
        a.rot(f'cascabel_{i}', {0: [0, 0, 0], 0.5: ([0, 0, 30], S), 0.7: ([0, 0, -30], S), 0.9: ([0, 0, 30], S), 1.1: ([0, 0, -30], S),
                                1.6: ([0, 0, 0], S)})
    a.sound(0.4, 'navidad:krampus_ruge').sound(0.5, 'navidad:krampus_cadenas')
    an.append(a)

    # agarre: se agacha, salta con los brazos por delante (agarra a 0.9 s) y lo mete al saco por encima del hombro
    a = Anim('agarre', 1.8, False)
    a.pos('hips', {0: [0, 0, 0], 0.35: ([0, -4, 2], S), 0.6: ([0, 3, -6], 'easeOutQuad'), 0.9: ([0, -2, -10], 'easeInQuad'),
                   1.1: ([0, -1, -6], S), 1.8: [0, 0, 0]})
    a.rot('torso', {0: [0, 0, 0], 0.35: ([18, 0, 0], S), 0.9: ([30, 0, 0], S), 1.2: ([-20, 0, 0], S), 1.5: ([-24, 0, 0], S), 1.8: [0, 0, 0]})
    for lado, s in (('r', -1), ('l', 1)):
        a.rot('arm_' + lado, {0: [0, 0, 0], 0.35: ([30, 0, 20 * s], S), 0.6: ([-100, 0, 30 * s], 'easeOutQuad'),
                              0.9: ([-80, 0, 6 * s], 'easeInQuart'), 1.1: ([-90, 0, 4 * s], S), 1.45: ([-200, 0, -10 * s], 'easeInOutQuad'),
                              1.8: [0, 0, 0]})
        a.rot('arm_' + lado + '_low', {0: [0, 0, 0], 0.6: ([-10, 0, 0], S), 0.9: ([-30, 0, 0], S), 1.45: ([-60, 0, 0], S), 1.8: [0, 0, 0]})
        a.rot('leg_' + lado, {0: [0, 0, 0], 0.35: ([-40, 0, 0], S), 0.6: ([20, 0, 0], S), 0.9: ([-30, 0, 0], S), 1.8: [0, 0, 0]})
        a.rot('leg_' + lado + '_low', {0: [0, 0, 0], 0.35: ([50, 0, 0], S), 0.6: ([0, 0, 0], S), 0.9: ([40, 0, 0], S), 1.8: [0, 0, 0]})
    a.rot('head', {0: [0, 0, 0], 0.6: ([-16, 0, 0], S), 0.9: ([10, 0, 0], S), 1.4: ([-10, 0, 0], S), 1.8: [0, 0, 0]})
    a.rot('jaw', {0: [0, 0, 0], 0.5: ([36, 0, 0], S), 1.0: ([10, 0, 0], S), 1.8: [0, 0, 0]})
    a.rot('saco', {0: [0, 0, 0], 1.4: ([-10, 0, 0], S), 1.55: ([14, 0, 6], S), 1.8: [0, 0, 0]})
    a.sound(0.6, 'navidad:krampus_cadenas').sound(1.45, 'navidad:krampus_saco')
    an.append(a)

    # giro: la cadena da vueltas a su alrededor (pega a 0.8 s)
    a = Anim('giro', 1.6, False)
    a.rot('root', {0: [0, 0, 0], 0.3: ([0, 40, 0], S), 0.8: ([0, -320, 0], 'easeInOutQuad'), 1.1: ([0, -360, 0], S), 1.6: ([0, -360, 0], S)})
    a.rot('arm_l', {0: [0, 0, 0], 0.3: ([-30, 0, 60], S), 0.5: ([-10, 0, 100], S), 1.1: ([-10, 0, 100], S), 1.6: [0, 0, 0]})
    a.rot('cadena_mano', {0: [0, 0, 0], 0.4: ([0, 0, 70], S), 1.1: ([0, 0, 80], S), 1.6: [0, 0, 0]})
    a.rot('torso', {0: [0, 0, 0], 0.3: ([-6, 0, 0], S), 0.8: ([10, 0, -8], S), 1.6: [0, 0, 0]})
    a.sound(0.35, 'navidad:krampus_cadenas').sound(0.75, 'navidad:krampus_cadenas')
    an.append(a)

    # soplido: se inclina y sopla (apaga el farol a 0.9 s)
    a = Anim('soplido', 1.6, False)
    a.rot('torso', {0: [0, 0, 0], 0.5: ([-14, 0, 0], S), 0.9: ([20, 0, 0], 'easeInQuart'), 1.3: ([18, 0, 0], S), 1.6: [0, 0, 0]})
    a.rot('head', {0: [0, 0, 0], 0.5: ([-24, 0, 0], S), 0.9: ([16, 0, 0], 'easeInQuart'), 1.3: ([14, 0, 0], S), 1.6: [0, 0, 0]})
    a.rot('jaw', {0: [0, 0, 0], 0.5: ([6, 0, 0], S), 0.9: ([30, 0, 0], S), 1.3: ([26, 0, 0], S), 1.6: [0, 0, 0]})
    a.scale('torso', {0: [1, 1, 1], 0.5: ([1.08, 1.04, 1.1], S), 0.9: ([0.98, 1, 0.96], S), 1.6: [1, 1, 1]})
    a.sound(0.85, 'navidad:krampus_soplido')
    an.append(a)

    # emboscada: brinca desde la oscuridad (pega a 0.6 s)
    a = Anim('emboscada', 1.1, False)
    a.pos('hips', {0: [0, -5, 0], 0.3: ([0, -6, 3], S), 0.45: ([0, 4, -6], 'easeOutQuad'), 0.6: ([0, -2, -12], 'easeInQuad'),
                   0.8: ([0, -1, -10], S), 1.1: [0, 0, 0]})
    for lado, s in (('r', -1), ('l', 1)):
        a.rot('arm_' + lado, {0: [0, 0, 0], 0.3: ([20, 0, 30 * s], S), 0.45: ([-150, 0, 20 * s], 'easeOutQuad'),
                              0.6: ([-60, 0, 0], 'easeInQuart'), 1.1: [0, 0, 0]})
        a.rot('leg_' + lado, {0: [-40, 0, 0], 0.3: ([-50, 0, 0], S), 0.45: ([30, 0, 0], S), 0.6: ([-20, 0, 0], S), 1.1: [0, 0, 0]})
        a.rot('leg_' + lado + '_low', {0: [60, 0, 0], 0.3: ([70, 0, 0], S), 0.45: ([0, 0, 0], S), 0.6: ([30, 0, 0], S), 1.1: [0, 0, 0]})
    a.rot('torso', {0: [20, 0, 0], 0.45: ([-10, 0, 0], S), 0.6: ([28, 0, 0], S), 1.1: [0, 0, 0]})
    a.rot('jaw', {0: [0, 0, 0], 0.4: ([40, 0, 0], S), 0.8: ([30, 0, 0], S), 1.1: [0, 0, 0]})
    a.sound(0.4, 'navidad:krampus_ruge')
    an.append(a)

    # furia (fase 2): se golpea el pecho y ruge
    a = Anim('fase', 2.6, False)
    for lado, s in (('r', -1), ('l', 1)):
        a.rot('arm_' + lado, {0: [0, 0, 0], 0.4: ([-90, 0, -30 * s], S), 0.55: ([-60, 0, -50 * s], 'easeInQuart'),
                              0.8: ([-90, 0, -30 * s], S), 0.95: ([-60, 0, -50 * s], 'easeInQuart'), 1.4: ([-40, 0, 80 * s], 'easeOutBack'),
                              2.2: ([-40, 0, 78 * s], S), 2.6: [0, 0, 0]})
        a.rot('arm_' + lado + '_low', {0: [0, 0, 0], 0.4: ([-80, 0, 0], S), 1.4: ([-40, 0, 0], S), 2.6: [0, 0, 0]})
    a.rot('torso', {0: [0, 0, 0], 0.55: ([-8, 0, 0], S), 0.95: ([-8, 0, 0], S), 1.4: ([-28, 0, 0], 'easeOutBack'), 2.2: ([-26, 0, 0], S),
                    2.6: [0, 0, 0]})
    a.rot('head', {0: [0, 0, 0], 1.4: ([-34, 0, 0], S), 1.6: ([-30, 0, 6], S), 1.8: ([-34, 0, -6], S), 2.2: ([-30, 0, 0], S), 2.6: [0, 0, 0]})
    a.rot('jaw', {0: [0, 0, 0], 1.4: ([44, 0, 0], 'easeOutBack'), 2.2: ([40, 0, 0], S), 2.6: [0, 0, 0]})
    a.sound(0.55, 'minecraft:entity.ravager.attack').sound(0.95, 'minecraft:entity.ravager.attack').sound(1.4, 'navidad:krampus_ruge')
    an.append(a)

    # apagón (fase 3): sube los brazos y aplaude: todo se apaga a 1.6 s
    a = Anim('apagon', 2.6, False)
    for lado, s in (('r', -1), ('l', 1)):
        a.rot('arm_' + lado, {0: [0, 0, 0], 0.8: ([-170, 0, 40 * s], 'easeOutQuad'), 1.3: ([-176, 0, 46 * s], S),
                              1.6: ([-150, 0, -6 * s], 'easeInQuart'), 2.2: ([-140, 0, -4 * s], S), 2.6: [0, 0, 0]})
    a.rot('torso', {0: [0, 0, 0], 0.8: ([-26, 0, 0], S), 1.6: ([6, 0, 0], 'easeInQuart'), 2.6: [0, 0, 0]})
    a.rot('head', {0: [0, 0, 0], 0.8: ([-30, 0, 0], S), 1.6: ([10, 0, 0], S), 2.6: [0, 0, 0]})
    a.rot('jaw', {0: [0, 0, 0], 0.8: ([30, 0, 0], S), 1.6: ([4, 0, 0], S), 2.0: ([36, 0, 0], S), 2.6: [0, 0, 0]})
    a.sound(1.55, 'navidad:krampus_apagon')
    an.append(a)

    # aturdido: se tambalea y se agarra la cabeza
    a = Anim('aturdido', 2.4, False)
    a.rot('torso', {0: [0, 0, 0], 0.3: ([-16, 0, 10], S), 0.8: ([-10, 0, -12], S), 1.3: ([-14, 0, 10], S), 1.8: ([-10, 0, -8], S), 2.4: [0, 0, 0]})
    a.rot('head', {0: [0, 0, 0], 0.3: ([-20, 20, 0], S), 0.8: ([-10, -20, 0], S), 1.3: ([-20, 20, 0], S), 2.4: [0, 0, 0]})
    for lado, s in (('r', -1), ('l', 1)):
        a.rot('arm_' + lado, {0: [0, 0, 0], 0.3: ([-150, 0, 10 * s], S), 2.0: ([-150, 0, 14 * s], S), 2.4: [0, 0, 0]})
        a.rot('arm_' + lado + '_low', {0: [0, 0, 0], 0.3: ([-80, 0, 0], S), 2.0: ([-80, 0, 0], S), 2.4: [0, 0, 0]})
    a.pos('hips', {0: [0, 0, 0], 0.3: ([1, -2, 1], S), 0.8: ([-1, -2, 0], S), 1.3: ([1, -2, 1], S), 2.4: [0, 0, 0]})
    an.append(a)

    # risa: se le mueven los hombros, la lengua afuera
    a = Anim('risa', 1.6, False)
    a.rot('torso', {0: [0, 0, 0], 0.2: ([-10, 0, 0], S), 0.4: ([-4, 0, 0], S), 0.6: ([-10, 0, 0], S), 0.8: ([-4, 0, 0], S),
                    1.0: ([-10, 0, 0], S), 1.6: [0, 0, 0]})
    a.rot('head', {0: [0, 0, 0], 0.2: ([-20, 0, 0], S), 1.2: ([-20, 0, 0], S), 1.6: [0, 0, 0]})
    a.rot('jaw', {0: [0, 0, 0], 0.2: ([30, 0, 0], S), 0.4: ([16, 0, 0], S), 0.6: ([30, 0, 0], S), 0.8: ([16, 0, 0], S),
                  1.0: ([30, 0, 0], S), 1.6: [0, 0, 0]})
    for lado, s in (('r', -1), ('l', 1)):
        a.rot('arm_' + lado, {0: [0, 0, 0], 0.2: ([-20, 0, 20 * s], S), 1.2: ([-20, 0, 20 * s], S), 1.6: [0, 0, 0]})
    a.sound(0.1, 'navidad:krampus_risa')
    an.append(a)

    a = Anim('hurt', 0.45, False)
    a.rot('torso', {0: [0, 0, 0], 0.1: ([-10, 0, 5], 'easeOutQuad'), 0.45: [0, 0, 0]})
    a.rot('head', {0: [0, 0, 0], 0.1: ([-14, 0, 0], 'easeOutQuad'), 0.45: [0, 0, 0]})
    an.append(a)

    # muere: cae de rodillas, se le cae la vara y se va de boca
    a = Anim('death', 3.5, 'hold')
    for lado in ('r', 'l'):
        a.rot('leg_' + lado, {0: [0, 0, 0], 0.8: ([-80, 0, 0], S), 3.5: ([-86, 0, 0], S)})
        a.rot('leg_' + lado + '_low', {0: [0, 0, 0], 0.8: ([100, 0, 0], S), 3.5: ([96, 0, 0], S)})
    a.pos('hips', {0: [0, 0, 0], 0.8: ([0, -9, -2], 'easeInQuad'), 2.4: ([0, -9, -2], S), 3.5: ([0, -12, -8], 'easeInQuad')})
    a.rot('torso', {0: [0, 0, 0], 0.8: ([-20, 0, 0], S), 1.6: ([-30, 0, 0], S), 2.4: ([-10, 0, 0], S), 3.5: ([70, 0, 0], 'easeInQuad')})
    a.rot('head', {0: [0, 0, 0], 0.8: ([-36, 0, 0], S), 1.6: ([-40, 0, 0], S), 3.5: ([20, 0, 14], S)})
    a.rot('jaw', {0: [0, 0, 0], 0.8: ([44, 0, 0], S), 3.5: ([30, 0, 0], S)})
    a.rot('arm_r', {0: [0, 0, 0], 0.8: ([-20, 0, -30], S), 3.5: ([-80, 0, -40], S)})
    a.rot('arm_l', {0: [0, 0, 0], 0.8: ([-20, 0, 30], S), 3.5: ([-80, 0, 40], S)})
    a.rot('vara', {0: [0, 0, 0], 0.6: ([60, 0, 40], S), 3.5: ([90, 0, 60], S)})
    a.sound(0.2, 'navidad:krampus_ruge').sound(3.2, 'navidad:krampus_cadenas')
    an.append(a)

    save_anims('krampus', an, os.path.join(A, 'animations', 'krampus.animation.json'))


if __name__ == '__main__':
    construir()
    animaciones()
    print('krampus listo')
