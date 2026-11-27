#!/usr/bin/env python3
"""El Pino de Navidad para decorar (modelo de Blockbench hecho a mano, su textura y los lugares para colgar adornos).

Es un abeto de dos bloques y medio: tronco, falda roja con orilla blanca, cinco pisos de ramas que caen (cada rama con su punta y dos
ramitas a los lados, con un poco de escarcha arriba) y una puntita. Además trae, escondidas, las cosas que se le ponen:
  * luces_1..3 (cada una con grupos a, b y c que parpadean por turnos): focos de colores sobre las ramas; cada tira llena un tercio.
  * guirnalda_1..3: guirnaldas doradas que cuelgan de punta a punta de las ramas (una por piso, de abajo hacia arriba).
Los lugares para colgar adornos (abajo de la punta de las ramas) y el de la punta se escriben en PuntosArbol.java, en el espacio del
bloque, calculados con las mismas cuentas que usa el Core para dibujar (así el servidor sabe cuál está más cerca de donde hiciste clic).

Uso: python3 herramientas/arbol.py"""
import math
import os
import random
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
from geo import Creature  # noqa: E402

A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')
JAVA = os.path.join(AQUI, '..', 'src', 'main', 'java', 'com', 'navidad', 'arbol', 'PuntosArbol.java')

R = random.Random(2412)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# ---------------------------------------------------------------- colores
AG = (30, 92, 46)        # agujas
AG_OSC = (18, 62, 32)
AG_CLARO = (52, 128, 62)
AG_PUNTA = (78, 156, 80)
ESCARCHA = (232, 242, 246)
TRONCO, TRONCO2 = (92, 60, 32), (70, 44, 24)
ROJO, ROJO_OSC = (196, 28, 40), (150, 16, 28)
BLANCO = (246, 246, 250)
ORO, ORO_CLARO, ORO_OSC = (236, 186, 58), (255, 236, 150), (184, 128, 28)
FOCOS = [(255, 64, 64), (255, 214, 64), (72, 150, 255), (72, 220, 120), (255, 120, 220), (255, 160, 60)]


# ---------------------------------------------------------------- las cuentas del Core (para saber dónde queda cada punto)

def rot_zyx(v, rx, ry, rz):
    """Como Quaternionf.rotationZYX(rz, ry, rx) de JOML: primero gira en x, luego en y, luego en z (grados)."""
    x, y, z = v
    a = math.radians(rx)
    y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    a = math.radians(ry)
    x, z = x * math.cos(a) + z * math.sin(a), -x * math.sin(a) + z * math.cos(a)
    a = math.radians(rz)
    x, y = x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a)
    return (x, y, z)


def a_bloque(p_b, rot=None, piv_b=None):
    """Un punto de Blockbench (y arriba), quizá girado con su cubo, al espacio del bloque (x, y, z en bloques; 0,0,0 = centro del piso)."""
    m = (p_b[0], 24 - p_b[1], p_b[2])
    if rot:
        pm = (piv_b[0], 24 - piv_b[1], piv_b[2])
        d = rot_zyx((m[0] - pm[0], m[1] - pm[1], m[2] - pm[2]), *rot)
        m = (d[0] + pm[0], d[1] + pm[1], d[2] + pm[2])
    return (-m[0] / 16, 1.501 - m[1] / 16, m[2] / 16)


def a_blockbench(q):
    """Del espacio del bloque a coordenadas de Blockbench (para poner cubos sin girar justo ahí)."""
    return (-q[0] * 16, (q[1] - 0.001) * 16, q[2] * 16)


# ---------------------------------------------------------------- pintura

def agujas(punta=False, escarcha=0.10):
    """Agujas en espiga: arriba, rayitas que salen del centro de la rama hacia los lados (como una pluma); a los lados, rayas
    horizontales; abajo, más oscuro. Un poco de escarcha sobre la vena de en medio."""
    claro = AG_PUNTA if punta else AG_CLARO

    def f(face, x, y, w, h):
        r = R.random()
        if face == 'top':
            cx = (w - 1) / 2
            d = abs(x - cx)
            if d < 0.75:
                return mix(ESCARCHA, claro, 0.25) if r < escarcha * 2.5 else AG_OSC
            espiga = (y + int(d)) % 3
            if espiga == 0:
                return mix(claro, ESCARCHA, 0.55) if r < escarcha else claro
            return AG if espiga == 1 else mix(AG, AG_OSC, 0.4)
        if face == 'bottom':
            return AG_OSC if (x + y) % 2 == 0 else mix(AG_OSC, AG, 0.35)
        # los lados: las puntas de las agujas en rayitas
        if y == 0:
            return mix(claro, ESCARCHA, 0.5) if r < escarcha * 1.5 else claro
        if y == h - 1:
            return AG_OSC
        return claro if (x + y) % 3 == 0 else AG if (x + y) % 3 == 1 else mix(AG, AG_OSC, 0.5)
    return f


def centro(face, x, y, w, h):
    if face == 'top':
        return AG if (x + y) % 3 else AG_CLARO
    return AG_OSC if (x + 2 * y) % 4 else mix(AG_OSC, AG, 0.5)


def tronco(face, x, y, w, h):
    return TRONCO2 if (x + y) % 3 == 0 else TRONCO


def falda(face, x, y, w, h):
    if face == 'top':
        # pliegues: rayas un poco más oscuras que salen del centro
        cx, cy = w / 2, h / 2
        ang = math.atan2(y - cy, x - cx)
        return ROJO_OSC if math.sin(ang * 8) > 0.75 else ROJO
    return ROJO_OSC


def orilla(face, x, y, w, h):
    r = R.random()
    return BLANCO if r < 0.75 else (226, 228, 236)


def foco(col):
    def f(face, x, y, w, h):
        return mix(col, (255, 255, 255), 0.35) if face == 'top' else col
    return f


def oropel(face, x, y, w, h):
    r = R.random()
    return ORO_CLARO if r < 0.25 else ORO_OSC if r < 0.45 else ORO


# ---------------------------------------------------------------- el árbol

c = Creature('geometry.navidad.pino_navidad', 256, 256, seed=24)
c.bone('arbol', [0, 0, 0])
c.bone('falda', [0, 0, 0], 'arbol')
c.bone('ramas', [0, 0, 0], 'arbol')

# la falda: dos octágonos (un cuadro y otro girado 45°): la orilla blanca abajo y la tela roja encima
for k, giro in enumerate((0, 45)):
    c.part('falda', [-10.5, 0, -10.5], [21, 0.75, 21], orilla, rotation=[0, giro, 0], pivot=[0, 0, 0])
    c.part('falda', [-9.5, 0.5, -9.5], [19, 0.75, 19], falda, rotation=[0, giro + 0.01, 0], pivot=[0, 0, 0])
# el tronco
c.part('arbol', [-1.5, 0, -1.5], [3, 8, 3], tronco)

# los pisos: (altura donde salen las ramas, largo, cuántas, caída en grados, grueso, ancho, giro inicial)
PISOS = [
    (11.0, 12.0, 9, 27, 4.0, 6.0, 0),
    (17.0, 10.5, 8, 25, 4.0, 5.5, 20),
    (22.5, 8.6, 7, 23, 3.5, 5.0, 8),
    (27.5, 6.6, 6, 21, 3.2, 4.2, 30),
    (31.8, 4.6, 5, 18, 3.0, 3.6, 12),
]
# lo de en medio de cada piso (para que no se vea a través): (abajo, alto, ancho)
CENTROS = [(5.0, 8.5, 13), (11.5, 8, 11), (17.5, 7, 9), (23.0, 6.5, 7), (28.0, 5.5, 5)]

lugares = []      # (x, y, z, piso) en el espacio del bloque: donde cuelga un adorno
puntas = []       # por piso: [(x, y, z), ...] la punta de cada rama (para las guirnaldas)
encima = []       # (x, y, z, piso): puntos sobre las ramas (para los focos)

for t, (y0, alto, ancho) in enumerate(CENTROS):
    c.part('ramas', [-ancho / 2, y0, -ancho / 2], [ancho, alto, ancho], centro, rotation=[0, 45 * (t % 2) + 0.01, 0], pivot=[0, 0, 0])

for t, (ya, largo, n, caida, grueso, ancho, giro0) in enumerate(PISOS):
    ps = []
    for k in range(n):
        giro = giro0 + 360 * k / n + R.uniform(-6, 6)
        ca = caida + R.uniform(-3, 3)
        la = largo * R.uniform(0.92, 1.05)
        z0 = 1.0
        piv = (0, ya, z0)
        rot = [-ca, giro, 0]
        # la rama principal (sale hacia +z desde cerca del centro)
        c.part('ramas', [-ancho / 2, ya - grueso / 2, z0], [ancho, grueso, la], agujas(), rotation=rot, pivot=list(piv))
        # la punta: más angosta y un poco más caída
        piv2 = (0, ya, z0 + la - 0.5)
        pp = a_bloque((0, ya, z0 + la - 0.5), rot, piv)
        pb = a_blockbench(pp)
        rot2 = [-(ca + 12), giro, 0]
        c.part('ramas', [pb[0] - ancho * 0.3, pb[1] - grueso * 0.35, pb[2]], [ancho * 0.6, grueso * 0.7, 2.5], agujas(True), rotation=rot2,
               pivot=list(pb))
        # dos ramitas a los lados, a media rama
        for lado in (-1, 1):
            m = a_bloque((0, ya, z0 + la * 0.55), rot, piv)
            mb = a_blockbench(m)
            c.part('ramas', [mb[0] - ancho * 0.3, mb[1] - grueso * 0.35, mb[2]], [ancho * 0.6, grueso * 0.7, la * 0.45], agujas(True),
                   rotation=[-(ca + 8), giro + lado * 38, 0], pivot=list(mb))
        # el lugar para colgar: abajo de la rama, cerca de la punta
        # (al final de la puntita de la rama, por abajo: así el adorno queda afuera y se ve)
        cuelga = a_bloque((pb[0], pb[1] - grueso * 0.35, pb[2] + 2.2), rot2, pb)
        if t > 0:  # (las del piso de abajo casi tocan el suelo: ahí no se cuelga nada)
            lugares.append((cuelga[0], cuelga[1] - 0.015, cuelga[2], t, giro))
        tip = a_bloque((0, ya - grueso * 0.2, z0 + la + 1.5), rot2, pb if False else piv)
        ps.append((a_bloque((0, ya, z0 + la + 1.0), rot, piv), giro))
        # puntos encima de la rama para los focos
        for d in (0.35, 0.62, 0.88):
            q = a_bloque((R.uniform(-ancho * 0.3, ancho * 0.3), ya + grueso / 2 + 0.3, z0 + la * d), rot, piv)
            encima.append((q, t))
    puntas.append(sorted(ps, key=lambda e: e[1] % 360))

# la puntita de arriba
c.part('ramas', [-2, 33.5, -2], [4, 3, 4], agujas(True), rotation=[0, 45, 0], pivot=[0, 0, 0])
c.part('ramas', [-1.25, 36.5, -1.25], [2.5, 2.5, 2.5], agujas(True, 0.1))
PUNTA = a_bloque((0, 39.0, 0))

# ---------------------------------------------------------------- luces: tres tiras, cada una con tres grupos que parpadean
R2 = random.Random(77)
R2.shuffle(encima)
for s in range(1, 4):
    for g in 'abc':
        c.bone(f'luces_{s}_{g}', [0, 0, 0], 'arbol')
        c.glow(f'luces_{s}_{g}')
for i, (q, t) in enumerate(encima):
    s = 1 + i % 3
    g = 'abc'[R2.randrange(3)]
    b = a_blockbench(q)
    col = FOCOS[R2.randrange(len(FOCOS))]
    c.part(f'luces_{s}_{g}', [b[0] - 0.5, b[1] - 0.3, b[2] - 0.5], [1, 1.3, 1], foco(col))

# ---------------------------------------------------------------- guirnaldas: de punta a punta, colgando un poquito
for t in range(3):
    nombre = f'guirnalda_{t + 1}'
    c.bone(nombre, [0, 0, 0], 'arbol')
    ps = [p for p, _ in puntas[t + 1 if t > 0 else 0]]
    for k in range(len(ps)):
        a, b = ps[k], ps[(k + 1) % len(ps)]
        pasos = 7
        for j in range(pasos):
            u = (j + 0.5) / pasos
            x = a[0] + (b[0] - a[0]) * u
            z = a[2] + (b[2] - a[2]) * u
            y = a[1] + (b[1] - a[1]) * u - 0.16 * math.sin(math.pi * u)
            # un poco hacia afuera para que no se pierda entre las ramas
            d = math.hypot(x, z)
            if d > 0:
                x, z = x * (d + 0.05) / d, z * (d + 0.05) / d
            bb = a_blockbench((x, y, z))
            ang = math.degrees(math.atan2(b[0] - a[0], b[2] - a[2]))
            c.part(nombre, [bb[0] - 0.75, bb[1] - 0.6, bb[2] - 0.75], [1.5, 1.2, 1.5], oropel, rotation=[R.uniform(-20, 20), -ang, 0],
                   pivot=list(bb))

c.build(os.path.join(A, 'geo', 'pino_navidad.geo.json'), os.path.join(A, 'textures', 'entity', 'pino_navidad.png'))

# ---------------------------------------------------------------- los lugares para Java
os.makedirs(os.path.dirname(JAVA), exist_ok=True)
filas = ',\n'.join(f'            {{{x:.4f}F, {y:.4f}F, {z:.4f}F, {t}}}' for x, y, z, t, _ in lugares)
with open(JAVA, 'w', encoding='utf-8') as fh:
    fh.write(f'''package com.navidad.arbol;

/**
 * Dónde se cuelgan los adornos del Pino de Navidad (lo escribe {{@code herramientas/arbol.py}} con las mismas cuentas del dibujo; no
 * se edita a mano). Cada lugar: x, y, z en bloques desde el centro del piso del árbol, y el piso de ramas (0 = el de abajo).
 */
public final class PuntosArbol {{
    public static final float[][] LUGARES = {{
{filas}
    }};
    /** La punta del árbol (donde va la estrella o el ángel). */
    public static final float[] PUNTA = {{{PUNTA[0]:.4f}F, {PUNTA[1]:.4f}F, {PUNTA[2]:.4f}F}};

    private PuntosArbol() {{
    }}
}}
''')
print(f'pino_navidad: {len(lugares)} lugares para adornos, {len(encima)} focos, punta en {PUNTA}')
