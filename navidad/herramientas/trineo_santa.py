#!/usr/bin/env python3
"""El trineo de Santa con Santa adentro (modelo hecho a mano, textura y animaciones). Los renos van aparte (son los renos animados del
mod, los dibuja TrineoRenderer adelante).

Trineo rojo con orillas y patines dorados que se enroscan adelante, farol encendido, asiento de terciopelo verde y atrás el costal de
juguetes con regalos asomándose. Santa: barriga, cinturón con hebilla, barba y bigote, cachetes rojos, gorro con pompón y guantes
negros; con las riendas en las manos.
Animaciones: idle (va meciéndose, el pompón y el farol se balancean), saluda (alza el brazo y mueve la mano), rie (la panza le
brinca y echa la cabeza para atrás) y lanza (saca un regalo del costal y lo avienta por un lado).
Uso: python3 herramientas/trineo_santa.py"""
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
from geo import Anim, Creature, save_anims  # noqa: E402

A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')
S, C = 'easeInOutSine', 'catmullrom'


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


ROJO, ROJO_OSC, ROJO_CLARO = (196, 28, 36), (138, 14, 24), (226, 64, 64)
ORO, ORO_OSC, ORO_CLARO = (224, 172, 52), (158, 108, 24), (255, 228, 132)
MADERA, MADERA_OSC = (110, 70, 40), (76, 46, 26)
VERDE, VERDE_OSC = (30, 104, 60), (18, 70, 40)
BLANCO, BLANCO_OSC = (246, 246, 250), (214, 218, 228)
PIEL, PIEL_OSC, CACHETE = (244, 196, 160), (220, 164, 128), (236, 120, 120)
NEGRO, NEGRO2 = (26, 24, 28), (52, 48, 56)
CAFE, CAFE_OSC = (150, 92, 50), (112, 66, 34)
LUZ = (255, 214, 96)


def lados(cara=None, arriba=None, abajo=None, frente=None, atras=None):
    def f(face, x, y, w, h):
        if face == 'top':
            return arriba or cara
        if face == 'bottom':
            return abajo or cara
        if face == 'front':
            return frente or cara
        if face == 'back':
            return atras or cara
        return cara
    return f


def pintura_roja(oro_arriba=False, adorno=False):
    """Madera pintada de rojo con brillo; con orilla dorada arriba y, si se pide, una greca dorada en los lados."""
    def f(face, x, y, w, h):
        if face == 'top':
            return ORO if oro_arriba else ROJO_CLARO
        if face == 'bottom':
            return ROJO_OSC
        if oro_arriba and y == 0:
            return ORO_CLARO if x % 4 == 0 else ORO
        if y == h - 1:
            return ROJO_OSC
        if adorno and face in ('right', 'left') and 1 < y < h - 2:
            # greca: rombos dorados y unas volutas
            cx, cy = w / 2, h / 2
            d = abs(x - cx) / max(1, w / 2) + abs(y - cy) / max(1, h / 2)
            if abs(d - 0.62) < 0.09 or (abs(y - cy) < 0.6 and abs(x - cx) < w * 0.35 and x % 2 == 0):
                return ORO
        return ROJO if (x + y * 3) % 11 else mix(ROJO, ROJO_CLARO, 0.5)
    return f


def oro(face, x, y, w, h):
    if face == 'top':
        return ORO_CLARO
    if face == 'bottom':
        return ORO_OSC
    return ORO if (x + y) % 5 else ORO_CLARO


def terciopelo(face, x, y, w, h):
    if face == 'top':
        return VERDE if (x // 2 + y // 2) % 2 else mix(VERDE, VERDE_OSC, 0.4)
    return VERDE_OSC


def madera(face, x, y, w, h):
    return MADERA_OSC if y % 3 == 0 else MADERA


def costal(face, x, y, w, h):
    if face == 'bottom':
        return CAFE_OSC
    if (x * 3 + y * 5) % 13 == 0:
        return CAFE_OSC                      # parches
    if face in ('front', 'back') and (x % 6 == 2 and y % 4 == 1):
        return mix(CAFE, CAFE_OSC, 0.5)      # costuras
    return CAFE if (x + y) % 4 else mix(CAFE, (180, 120, 70), 0.4)


def regalo(base, liston):
    def f(face, x, y, w, h):
        if face in ('top', 'bottom'):
            if abs(x - w // 2) < 1 or abs(y - h // 2) < 1:
                return liston
            return base
        if abs(x - w // 2) < 1:
            return liston
        return base
    return f


def saco_rojo(face, x, y, w, h):
    """El traje de Santa: rojo con el centro de piel blanca al frente, cinturón negro y hebilla."""
    if face == 'front':
        if abs(x - (w - 1) / 2) < 1.1:
            return BLANCO if (x + y) % 3 else BLANCO_OSC
    if face == 'top':
        return ROJO_CLARO
    if face == 'bottom':
        return ROJO_OSC
    return ROJO if (x + y) % 7 else ROJO_CLARO


def panza(face, x, y, w, h):
    # el cinturón (a la mitad de la panza), la piel blanca del centro y el traje
    if face != 'bottom' and face != 'top' and 3 <= y <= 4:
        return NEGRO if (x + y) % 5 else NEGRO2
    return saco_rojo(face, x, y, w, h)


def cara(face, x, y, w, h):
    if face == 'front':
        # ojos (con brillito), cachetes y cejas los ponen otras piezas
        if y == 2 and x in (1, w - 2):
            return NEGRO
        if y == 1 and x in (1, w - 2):
            return (255, 255, 255)
        if y == 4 and x in (0, w - 1):
            return CACHETE
        if y == 3 and x in (0, 1, w - 2, w - 1):
            return mix(PIEL, CACHETE, 0.5)
        return PIEL
    if face == 'back':
        return BLANCO
    if face in ('right', 'left'):
        return BLANCO if x >= w // 2 or y >= h - 2 else PIEL_OSC
    return PIEL


def barba(face, x, y, w, h):
    """Barba esponjada: blanca con mechones ondulados apenas grises."""
    if face == 'top':
        return BLANCO_OSC
    onda = (x * 2 + y + (y // 3) % 2) % 7
    return BLANCO_OSC if onda == 0 else (252, 252, 255) if onda == 3 else BLANCO


def pelo_blanco(face, x, y, w, h):
    return BLANCO_OSC if (x * 2 + y) % 7 == 0 else BLANCO


def guante(face, x, y, w, h):
    return NEGRO if (x + y) % 4 else NEGRO2


def botas(face, x, y, w, h):
    if face != 'top' and face != 'bottom' and y == 0:
        return ORO
    return NEGRO if (x + y) % 3 else NEGRO2


def rojo_liso(face, x, y, w, h):
    if face == 'bottom':
        return ROJO_OSC
    return ROJO if (x + y) % 6 else ROJO_CLARO


def luz(face, x, y, w, h):
    return LUZ if (x + y) % 2 else (255, 240, 170)


def hierro(face, x, y, w, h):
    return NEGRO2 if (x + y) % 2 else NEGRO


c = Creature('geometry.navidad.trineo_santa', 256, 256, seed=1225)
c.bone('root', [0, 0, 0])
c.bone('trineo', [0, 0, 0], 'root')

# ---------------------------------------------------------------- patines dorados (se enroscan adelante)
for s in (-1, 1):
    x0 = 6 if s > 0 else -7.5
    c.part('trineo', [x0, 0, -11], [1.5, 1.2, 23], oro)                       # riel
    c.part('trineo', [x0, 0, 11.5], [1.5, 2.6, 1.2], oro)                     # colita de atrás
    c.part('trineo', [x0, 0, -13], [1.5, 7.5, 1.5], oro)                      # sube adelante
    c.part('trineo', [x0, 7.5, -13], [1.5, 1.4, 4.2], oro)                    # se dobla hacia atrás
    c.part('trineo', [x0, 5.2, -10.2], [1.5, 2.4, 1.3], oro)                  # y baja (la voluta)
    for z in (-7, 5.5):
        c.part('trineo', [x0 + 0.25, 1.2, z], [1, 3.0, 1.2], oro)             # patitas

# ---------------------------------------------------------------- el carro
c.part('trineo', [-7, 4, -9], [14, 1, 22], madera)                            # piso
for s in (-1, 1):
    x0 = 6 if s > 0 else -7.5
    c.part('trineo', [x0, 5, -9], [1.5, 7, 22], pintura_roja(True, True))     # lados con greca
    c.part('trineo', [x0, 12, -10.5], [1.5, 3, 4], pintura_roja(True))        # el lado sube adelante
    c.part('trineo', [x0 - 0.1, 15, -11.6], [1.7, 1.5, 3.2], oro)              # voluta dorada arriba
c.part('trineo', [-6, 5, -10.2], [12, 8, 1.6], pintura_roja(True))            # el frente
c.part('trineo', [-6, 12.6, -11.6], [12, 2.6, 1.6], pintura_roja(True), rotation=[-22, 0, 0], pivot=[0, 12.6, -10.2])
c.part('trineo', [-6, 5, 12.4], [12, 6.5, 1.6], pintura_roja(True))           # atrás
c.part('trineo', [-6, 8, -2], [12, 2, 6], terciopelo)                          # asiento
c.part('trineo', [-6, 5, 4], [12, 11, 1.5], pintura_roja(False))              # respaldo
c.part('trineo', [-5.5, 16, 4], [11, 1.5, 1.5], oro)                           # su orilla dorada

# el farol de adelante (con su luz aparte, que brilla)
c.bone('farol', [6.6, 17, -9.3], 'trineo')
c.part('farol', [6.2, 12, -9.7], [0.8, 5, 0.8], hierro)
c.part('farol', [5.6, 16.6, -10.3], [2.0, 0.6, 2.0], hierro)
c.part('farol', [5.6, 19.6, -10.3], [2.0, 0.6, 2.0], hierro)
c.part('farol', [6.3, 20.2, -9.6], [0.6, 0.8, 0.6], hierro)
c.bone('farol_luz', [6.6, 17, -9.3], 'farol')
c.part('farol_luz', [5.8, 17.2, -10.1], [1.6, 2.4, 1.6], luz)
c.glow('farol_luz')

# ---------------------------------------------------------------- el costal de juguetes
c.bone('costal', [0, 5, 9], 'trineo')
c.part('costal', [-5, 5, 5.6], [10, 9.5, 6.8], costal)
c.part('costal', [-4.2, 14.5, 6.1], [8.4, 4, 5.8], costal)
c.part('costal', [-3.2, 18.5, 6.8], [6.4, 2.4, 4.6], costal)
c.part('costal', [-1.6, 20.9, 8.1], [3.2, 1.6, 2.2], lados(ORO_OSC, ORO, ORO_OSC))   # el amarre
c.part('costal', [-3, 22.3, 7.4], [6, 1.4, 3.6], costal)                             # la boca abierta
c.bone('juguetes', [0, 22, 9], 'costal')
c.part('juguetes', [-2.4, 22.4, 7.8], [3, 3, 3], regalo((214, 32, 40), ORO))
c.part('juguetes', [0.6, 22.8, 8.3], [2.4, 3.2, 2.4], regalo((40, 140, 70), (250, 250, 250)), rotation=[0, 0, -15], pivot=[1.8, 22.8, 9.5])
c.part('juguetes', [-0.6, 23.2, 9.8], [2.2, 2.4, 2.2], regalo((60, 110, 230), ORO), rotation=[12, 20, 0], pivot=[0.5, 23.2, 10.9])
c.part('juguetes', [2.2, 22.4, 7.2], [0.9, 6, 0.9], lambda f, x, y, w, h: (240, 240, 244) if (y + x) % 2 else (220, 30, 40),
       rotation=[0, 0, -20], pivot=[2.6, 22.4, 7.6])                                 # un bastón asomándose
c.part('juguetes', [2.2, 27.6, 7.2], [2.0, 0.9, 0.9], lambda f, x, y, w, h: (240, 240, 244) if x % 2 else (220, 30, 40),
       rotation=[0, 0, -20], pivot=[2.6, 22.4, 7.6])

# ---------------------------------------------------------------- Santa
c.bone('santa', [0, 9, 1], 'trineo')
# piernas (sentado): muslos hacia adelante, espinillas hacia abajo, botas
for s in (-1, 1):
    x0 = 0.4 if s > 0 else -4.2
    c.part('santa', [x0, 10, -5.5], [3.8, 3.2, 6.5], rojo_liso)
    c.part('santa', [x0 + 0.1, 5.8, -6.4], [3.6, 4.4, 3.4], rojo_liso)
    c.part('santa', [x0 - 0.1, 5, -7.6], [3.9, 2.4, 4.6], botas)
c.bone('cuerpo', [0, 10, 1], 'santa')
c.part('cuerpo', [-5.4, 9.6, -4.4], [10.8, 1.6, 9.0], lados(BLANCO, BLANCO, BLANCO_OSC))          # piel de abajo del abrigo
c.part('cuerpo', [-5, 10.6, -3.6], [10, 8.6, 7.8], panza)                                          # la panza
c.part('cuerpo', [-4.4, 11.8, -4.6], [8.8, 6.2, 1.0], panza)                                       # más panza al frente
c.part('cuerpo', [-1.4, 13.6, -4.9], [2.8, 2.2, 0.6], lados(ORO, ORO_CLARO, ORO_OSC, ORO_CLARO))  # hebilla
c.part('cuerpo', [-4.6, 19.2, -2.6], [9.2, 4.2, 6.6], saco_rojo)                                   # el pecho
# brazos (en reposo llevan las riendas al frente)
for lado, s in (('der', -1), ('izq', 1)):
    px = 5.6 * s
    c.bone(f'brazo_{lado}', [px, 22.4, 0.8], 'cuerpo')
    x0 = 4.6 if s > 0 else -7.6
    c.part(f'brazo_{lado}', [x0, 15.6, -0.8], [3, 7.2, 3.2], saco_rojo)                              # brazo
    c.part(f'brazo_{lado}', [x0 + 0.1, 14.4, -5.6], [2.8, 2.8, 5.2], saco_rojo)                      # antebrazo
    c.part(f'brazo_{lado}', [x0 - 0.1, 14.2, -6.2], [3.2, 3.2, 1.1], lados(BLANCO, BLANCO, BLANCO_OSC))  # puño de piel
    c.part(f'brazo_{lado}', [x0 + 0.3, 14.5, -8.0], [2.4, 2.6, 1.8], guante)                         # guante
# la cabeza
c.bone('cabeza', [0, 23.2, 0.8], 'cuerpo')
c.part('cabeza', [-3.5, 23.2, -2.6], [7, 7, 7], cara)
c.part('cabeza', [-0.8, 25.6, -3.4], [1.6, 1.4, 0.9], lambda f, x, y, w, h: (238, 140, 130) if f != 'top' else (246, 170, 150))  # nariz
c.part('cabeza', [-3.1, 28.3, -2.9], [2.4, 0.7, 0.4], lados(BLANCO))                     # cejas
c.part('cabeza', [0.7, 28.3, -2.9], [2.4, 0.7, 0.4], lados(BLANCO))
c.part('cabeza', [-3.6, 24.6, -3.3], [3.4, 1.2, 0.9], barba, rotation=[0, 0, 12], pivot=[-0.2, 25.2, -2.9])   # bigote
c.part('cabeza', [0.2, 24.6, -3.3], [3.4, 1.2, 0.9], barba, rotation=[0, 0, -12], pivot=[0.2, 25.2, -2.9])
c.part('cabeza', [-4.1, 19.2, -3.4], [8.2, 5.6, 3.6], barba)                              # barba
c.part('cabeza', [-2.8, 17.4, -3.4], [5.6, 1.8, 2.6], barba)                              # puntita de la barba
c.part('cabeza', [-4.0, 23.2, 1.0], [8.0, 5.4, 4.0], pelo_blanco)                         # pelo atrás y a los lados
# el gorro (con la punta caída hacia atrás y su pompón)
c.bone('gorro', [0, 30, 0.8], 'cabeza')
c.part('gorro', [-4.2, 29.4, -3.2], [8.4, 2.0, 8.2], lados(BLANCO, BLANCO, BLANCO_OSC))
c.part('gorro', [-3.6, 31.4, -2.4], [7.2, 2.6, 7.0], rojo_liso)
c.part('gorro', [-2.6, 33.6, -1.0], [5.2, 2.6, 5.4], rojo_liso, rotation=[22, 0, 0], pivot=[0, 33.6, 1.6])
c.bone('punta', [0, 35.4, 3.4], 'gorro')
c.part('punta', [-1.6, 35.4, 2.0], [3.2, 2.4, 3.4], rojo_liso, rotation=[50, 0, 0], pivot=[0, 35.4, 3.4])
c.part('punta', [-1.3, 34.6, 5.8], [2.6, 2.6, 2.6], pelo_blanco)

# ---------------------------------------------------------------- las riendas (de las manos hacia los renos)
c.bone('riendas', [0, 15.6, -8], 'trineo')
for s in (-1, 1):
    x = 5.4 * s - 0.25
    c.part('riendas', [x, 15.4, -26], [0.5, 0.5, 18.4], lados(MADERA_OSC, MADERA), rotation=[-7, 4 * s, 0], pivot=[x + 0.25, 15.6, -8])

c.build(os.path.join(A, 'geo', 'trineo_santa.geo.json'), os.path.join(A, 'textures', 'entity', 'trineo_santa.png'))

# ================================================================ animaciones
anims = []
L = 2.0
a = Anim('idle', L, True)
a.pos('santa', {0: ([0, 0, 0], S), 0.5: ([0, 0.35, 0], S), 1.0: ([0, 0, 0], S), 1.5: ([0, 0.35, 0], S), 2.0: ([0, 0, 0], S)})
a.rot('cabeza', {0: ([0, 0, 0], S), 1.0: ([3, 6, -2], S), 2.0: ([0, 0, 0], S)})
a.rot('gorro', {0: ([0, 0, 0], S), 0.5: ([4, 0, 3], S), 1.0: ([0, 0, 0], S), 1.5: ([4, 0, -3], S), 2.0: ([0, 0, 0], S)})
a.rot('punta', {0: ([0, 0, 0], S), 0.5: ([18, 0, 10], S), 1.0: ([0, 0, 0], S), 1.5: ([18, 0, -10], S), 2.0: ([0, 0, 0], S)})
a.rot('farol', {0: ([0, 0, 0], S), 0.5: ([8, 0, 5], S), 1.0: ([0, 0, 0], S), 1.5: ([-8, 0, -5], S), 2.0: ([0, 0, 0], S)})
a.scale('costal', {0: ([1, 1, 1], S), 0.6: ([1.03, 0.97, 1.03], S), 1.2: ([1, 1.02, 1], S), 2.0: ([1, 1, 1], S)})
for lado in ('der', 'izq'):
    a.rot(f'brazo_{lado}', {0: ([0, 0, 0], S), 0.5: ([-6, 0, 0], S), 1.0: ([0, 0, 0], S), 1.5: ([-6, 0, 0], S), 2.0: ([0, 0, 0], S)})
a.pos('riendas', {0: ([0, 0, 0], S), 0.5: ([0, -0.3, 0], S), 1.0: ([0, 0, 0], S), 1.5: ([0, -0.3, 0], S), 2.0: ([0, 0, 0], S)})
anims.append(a)

# saluda: alza el brazo derecho y mueve la mano
L = 2.2
a = Anim('saluda', L)
arriba = [-165, 0, -18]
claves = {0: ([0, 0, 0], S), 0.3: (arriba, S)}
for i, t in enumerate((0.5, 0.7, 0.9, 1.1, 1.3, 1.5)):
    claves[t] = ([arriba[0], 0, arriba[2] + (-26 if i % 2 == 0 else 14)], S)
claves[1.75] = (arriba, S)
claves[L] = ([0, 0, 0], S)
a.rot('brazo_der', claves)
a.rot('cabeza', {0: ([0, 0, 0], S), 0.3: ([-6, -14, -6], S), 1.75: ([-6, -14, -6], S), L: ([0, 0, 0], S)})
a.rot('cuerpo', {0: ([0, 0, 0], S), 0.3: ([0, -8, 0], S), 1.75: ([0, -8, 0], S), L: ([0, 0, 0], S)})
anims.append(a)

# rie: la panza le brinca y echa la cabeza para atrás
L = 1.6
a = Anim('rie', L)
brinco = {0: ([0, 0, 0], S)}
for i in range(1, 9):
    brinco[round(i * 0.15, 3)] = ([0, 0.6 if i % 2 else 0, 0], S)
brinco[L] = ([0, 0, 0], S)
a.pos('cuerpo', brinco)
a.scale('cuerpo', {0: ([1, 1, 1], S), 0.3: ([1.04, 0.97, 1.04], S), 0.6: ([1, 1.02, 1], S), 0.9: ([1.04, 0.97, 1.04], S), L: ([1, 1, 1], S)})
a.rot('cabeza', {0: ([0, 0, 0], S), 0.25: ([-16, 0, 0], S), 1.25: ([-16, 0, 0], S), L: ([0, 0, 0], S)})
a.rot('brazo_izq', {0: ([0, 0, 0], S), 0.3: ([-25, 0, 10], S), 1.2: ([-25, 0, 10], S), L: ([0, 0, 0], S)})
a.rot('punta', {0: ([0, 0, 0], S), 0.3: ([30, 0, 0], S), 0.6: ([-5, 0, 0], S), 0.9: ([30, 0, 0], S), L: ([0, 0, 0], S)})
anims.append(a)

# lanza: mete la mano al costal, saca un regalo y lo avienta por un lado
L = 1.1
a = Anim('lanza', L)
a.rot('brazo_der', {0: ([0, 0, 0], S), 0.28: ([55, 30, -20], S), 0.4: ([50, 30, -25], S), 0.62: ([-120, -10, -55], S),
                    0.8: ([-95, 0, -40], S), L: ([0, 0, 0], S)})
a.rot('cuerpo', {0: ([0, 0, 0], S), 0.28: ([0, 28, 0], S), 0.62: ([0, -22, 0], S), L: ([0, 0, 0], S)})
a.rot('cabeza', {0: ([0, 0, 0], S), 0.28: ([5, 25, 0], S), 0.62: ([-5, -30, 0], S), L: ([0, 0, 0], S)})
a.scale('costal', {0: ([1, 1, 1], S), 0.3: ([1.05, 0.94, 1.05], S), 0.45: ([0.98, 1.04, 0.98], S), L: ([1, 1, 1], S)})
anims.append(a)

save_anims('trineo_santa', anims, os.path.join(A, 'animations', 'trineo_santa.animation.json'))
print('trineo de Santa listo')
