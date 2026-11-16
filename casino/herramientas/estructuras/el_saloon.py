#!/usr/bin/env python3
"""EL SALOON: casino del Viejo Oeste (desierto, badlands y sabana), como un saloon de película.

Uso: el_saloon.py [salida.nbt] [volcado.txt]
Frente al sur (z máxima): calle de tierra, banqueta de tablones con techito y postes, y tres fachadas falsas:
  - al centro el SALOON (2 pisos, frontón con "SALOON", balcón con barandal arriba del porche, puertas batientes)
  - a la izquierda RODEO (sala del toro mecánico) y a la derecha TIRO (galería de tiro al blanco)
  - detrás, la torre de agua, la letrina, barriles y cactus.
Planta baja del saloon: barra larga con espejo y botellas (cantinero), pianola, 2 mesas de 21, ruleta de la suerte entre
tragamonedas, caja con rejas y tienda de premios; escalera al centro del fondo con puertitas VIP y dos sheriffs.
Mesa de vencidas en el salón (contra un forzudo o contra otro jugador).
Primer piso (tarjeta Plata): sala de cartas con una mesa de 21 de alto riesgo y la mesa de dados del mentiroso, galería con barandal alrededor del salón, cuartos al frente
y el palco del dueño (tarjeta Oro) con salida al balcón.
"""
import sys, os, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from obra import Obra
import maquinas as mq

R = random.Random(18)
FLOORS = {'planta baja': 1, 'primer piso': 9}   # para recorrido.py
W, H, D = 60, 26, 50
o = Obra(W, H, D)
CX = 29.5          # eje de simetría del saloon

# ------------------------------------------------------------------ paleta
CLAP = 'minecraft:birch_planks'             # tablas de la fachada (crema, como la foto)
TRIM = 'minecraft:spruce_planks'            # molduras
POST = 'minecraft:stripped_spruce_log[axis=y]'
RED = 'minecraft:mangrove_planks'           # marcos rojos de las ventanas
FLOOR = 'minecraft:dark_oak_planks'
WAIN = 'minecraft:spruce_planks'            # lambrín
PAPER = 'casino:velvet_wall_red'            # tapiz
BEAM = 'minecraft:stripped_dark_oak_log[axis=x]'
BEAMZ = 'minecraft:stripped_dark_oak_log[axis=z]'
GLASS = 'minecraft:glass_pane'
AIR = 'minecraft:air'
LIGHT = 'minecraft:light[level=13]'


def stairs(b, f, half='bottom'): return f'{b}[facing={f},half={half}]'
def slab(b, t='bottom'): return f'{b}[type={t}]'
def letter(ch, f, color=0):
    chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?$&♥♠♦♣★- "
    return f'casino:painted_letter[facing={f},glyph={chars.index(ch)},color={color}]'
def word(text, x0, y, z, f, color=0, step=1):
    for i, ch in enumerate(text):
        if ch != ' ':
            o.set(x0 + i * step, y, z, letter(ch, f, color))
def mx(x): return int(2 * CX - x)       # espejo en x
def sym(x, y, z, st, st2=None):
    o.set(x, y, z, st); o.set(mx(x), y, z, st2 or st)


def lantern(x, y_top, z, drop=1):
    """Farol colgando de una cadena (y_top = bloque del techo)."""
    for k in range(1, drop + 1):
        o.set(x, y_top - k, z, 'minecraft:chain[axis=y]')
    o.set(x, y_top - drop - 1, z, 'minecraft:lantern[hanging=true]')


def chandelier(x, y, z, chain=2):
    """Candil de rueda de carreta con velas (casino:wheel_chandelier, casi dos bloques de ancho), colgado de una cadena."""
    o.set(x, y, z, 'casino:wheel_chandelier')
    for k in range(1, chain + 1):
        o.set(x, y + k, z, 'minecraft:chain[axis=y]')


SIDES = (('n', 0, -1, 'south'), ('s', 0, 1, 'north'), ('w', -1, 0, 'east'), ('e', 1, 0, 'west'))


def table(x, z, y=1, chairs='nswe', drinks=None, kind='saloon_table'):
    """Mesa del saloon (redonda o de póker) con sillas de madera alrededor, mirando a la mesa. chairs = lados con silla."""
    o.set(x, y, z, f'casino:{kind}')
    if drinks:
        o.set(x, y + 1, z, f'casino:drinks[facing=north,drinks={drinks}]')
    for k, dx, dz, f in SIDES:
        if k in chairs:
            o.set(x + dx, y, z + dz, f'casino:saloon_chair[facing={f}]')


# forro de los anexos (rodeo y tiro): lambrín de roble oscuro, riel de tronco, tablas de abeto arriba y postes cada 4 bloques
def annex_lining(y, k, axis):
    if k % 4 == 0:
        return 'minecraft:stripped_spruce_log[axis=y]'
    if y <= 2:
        return 'minecraft:dark_oak_planks'
    if y == 3:
        return f'minecraft:stripped_dark_oak_log[axis={axis}]'
    return 'minecraft:spruce_planks'


def line_wall_x(x, z0, z1, y1=5):
    """Forra una pared que corre en z (x fija) desde z0 hasta z1."""
    for z in range(z0, z1 + 1):
        for y in range(1, y1 + 1):
            o.set(x, y, z, annex_lining(y, z, 'z'))


def line_wall_z(z, x0, x1, y1=5):
    for x in range(x0, x1 + 1):
        for y in range(1, y1 + 1):
            o.set(x, y, z, annex_lining(y, x, 'x'))


def frame_opening(x0, x1, y0, y1, z=None, x=None, log='minecraft:stripped_spruce_log'):
    """Abre un hueco (ventana o puerta) en un forro y le pone marco de tronco: en el plano z fijo (x0..x1) o x fijo (z = x0..x1)."""
    for a in range(x0 - 1, x1 + 2):
        for b in range(y0 - 1, y1 + 2):
            inside = x0 <= a <= x1 and y0 <= b <= y1
            if b < 1:
                continue
            if z is not None:
                pos = (a, b, z)
                ax = 'y' if a in (x0 - 1, x1 + 1) else 'x'
            else:
                pos = (x, b, a)
                ax = 'y' if a in (x0 - 1, x1 + 1) else 'z'
            o.set(*pos, AIR if inside else f'{log}[axis={ax}]')


def window(x0, x1, y0, y1, z, out='south'):
    """Ventana con marco rojo (mangle) y vidrio."""
    for x in range(x0 - 1, x1 + 2):
        o.set(x, y0 - 1, z, RED); o.set(x, y1 + 1, z, RED)
    for y in range(y0, y1 + 1):
        o.set(x0 - 1, y, z, RED); o.set(x1 + 1, y, z, RED)
        for x in range(x0, x1 + 1):
            o.set(x, y, z, GLASS)
    # repisa y cornisita
    dz = 1 if out == 'south' else -1
    for x in range(x0 - 1, x1 + 2):
        o.set(x, y0 - 1, z + dz, stairs('minecraft:spruce_stairs', 'north' if out == 'south' else 'south', 'top'))
        o.set(x, y1 + 2, z + dz, stairs('minecraft:spruce_stairs', 'north' if out == 'south' else 'south'))


# ================================================================== TERRENO, CALLE Y PATIO
for x in range(W):
    for z in range(D):
        r = R.random()
        o.set(x, 0, z, 'minecraft:coarse_dirt' if r < 0.55 else 'minecraft:sand' if r < 0.8 else 'minecraft:dirt')
# calle (z 43..49) con surcos de carreta
for x in range(W):
    for z in range(43, D):
        o.set(x, 0, z, 'minecraft:dirt_path' if z in (45, 47) and R.random() < 0.85 else
              'minecraft:coarse_dirt' if R.random() < 0.7 else 'minecraft:packed_mud')
# matas secas y piedritas en la calle
for _ in range(14):
    x, z = R.randrange(W), R.randrange(44, D)
    if z not in (45, 47):
        o.set(x, 1, z, 'minecraft:dead_bush')
        o.set(x, 0, z, 'minecraft:sand')

# ================================================================== BANQUETA (z 39..42) A LO LARGO DE LAS TRES FACHADAS
for x in range(3, 58):
    for z in range(39, 43):
        o.set(x, 0, z, 'minecraft:spruce_planks' if (x + z) % 5 else 'minecraft:stripped_spruce_wood[axis=x]')
    o.set(x, 0, 43, 'minecraft:spruce_slab[type=top]')
# escalones a la calle frente a las puertas
for x in (28, 29, 30, 31, 8, 9, 10, 49, 50, 51, 52):
    o.set(x, 0, 43, stairs('minecraft:spruce_stairs', 'north', 'top'))

# ================================================================== SALOON (x 15..44, z 8..38), 2 pisos
SX0, SX1, SZ0, SZ1 = 15, 44, 8, 38
Y2 = 8           # piso de arriba
YR = 15          # techo / azotea
# muros
for y in range(1, YR):
    for x in range(SX0, SX1 + 1):
        o.set(x, y, SZ0, CLAP); o.set(x, y, SZ1, CLAP)
    for z in range(SZ0, SZ1 + 1):
        o.set(SX0, y, z, CLAP); o.set(SX1, y, z, CLAP)
# esquinas y pilastras de tronco, franja de moldura en el entrepiso
for y in range(1, YR):
    for x, z in ((SX0, SZ0), (SX1, SZ0), (SX0, SZ1), (SX1, SZ1)):
        o.set(x, y, z, POST)
for x in range(SX0, SX1 + 1):
    o.set(x, Y2, SZ1, TRIM); o.set(x, Y2, SZ0, TRIM)
for z in range(SZ0, SZ1 + 1):
    o.set(SX0, Y2, z, TRIM); o.set(SX1, Y2, z, TRIM)
# pisos
for x in range(SX0 + 1, SX1):
    for z in range(SZ0 + 1, SZ1):
        o.set(x, 0, z, FLOOR)

# ---- fachada falsa: el frente sube hasta y 17 y el frontón (en escalones) hasta y 22, con "SALOON"
for x in range(SX0, SX1 + 1):
    for y in range(YR, 18):
        o.set(x, y, SZ1, CLAP)
    o.set(x, 18, SZ1, stairs('minecraft:dark_oak_stairs', 'south'))
    o.set(x, 18, SZ1 + 1, slab('minecraft:dark_oak_slab', 'bottom'))
    o.set(x, 17, SZ1 + 1, stairs('minecraft:dark_oak_stairs', 'north', 'top'))     # cornisa
for x in range(SX0, SX1 + 1):
    o.set(x, YR - 1, SZ1 + 1, stairs('minecraft:spruce_stairs', 'north', 'top'))
GX0, GX1 = 19, 40
for x in range(GX0, GX1 + 1):
    top = 17 + min(x - GX0, GX1 - x) // 2
    for y in range(18, top + 1):
        o.set(x, y, SZ1, CLAP)
    nxt = 17 + min(x + 1 - GX0, GX1 - x - 1) // 2
    prv = 17 + min(x - 1 - GX0, GX1 - x + 1) // 2
    f = 'east' if x < CX else 'west'
    o.set(x, top + 1, SZ1, stairs('minecraft:dark_oak_stairs', f) if (x < CX and nxt > top) or (x > CX and prv > top) else slab('minecraft:dark_oak_slab'))
# marco oscuro del letrero y las letras
for x in range(24, 36):
    o.set(x, 18, SZ1, TRIM); o.set(x, 21, SZ1, TRIM)
for y in range(18, 22):
    o.set(24, y, SZ1, TRIM); o.set(35, y, SZ1, TRIM)
word("SALOON", 27, 19, SZ1 + 1, 'south', 0)
for x in (25, 34):
    o.set(x, 19, SZ1 + 1, letter('★', 'south', 2))
# ---- ventanas del frente (simétricas) y puertas batientes
window(19, 22, 2, 4, SZ1)
window(37, 40, 2, 4, SZ1)
for y in range(1, 5):
    for x in (27, 28, 31, 32):
        o.set(x, y, SZ1, RED if x in (28, 31) or y == 4 else CLAP)
for y in range(1, 4):
    o.set(29, y, SZ1, AIR); o.set(30, y, SZ1, AIR)
for x in (28, 29, 30, 31):
    o.set(x, 4, SZ1, RED)
o.set(29, 1, SZ1, 'casino:saloon_door[facing=north,hinge=left]')
o.set(30, 1, SZ1, 'casino:saloon_door[facing=north,hinge=right]')
o.set(29, 5, SZ1 + 1, letter('$', 'south', 2)); o.set(30, 5, SZ1 + 1, letter('$', 'south', 2))
# ventanas de arriba y la puerta al balcón con su placa "1880"
window(19, 21, 10, 12, SZ1)
window(38, 40, 10, 12, SZ1)
for y in range(9, 13):
    for x in (28, 31):
        o.set(x, y, SZ1, RED)
for x in (28, 29, 30, 31):
    o.set(x, 12, SZ1, RED)
o.set(29, 9, SZ1, 'minecraft:spruce_door[facing=south,half=lower,hinge=left,open=false]')
o.set(29, 10, SZ1, 'minecraft:spruce_door[facing=south,half=upper,hinge=left,open=false]')
o.set(30, 9, SZ1, 'minecraft:spruce_door[facing=south,half=lower,hinge=right,open=false]')
o.set(30, 10, SZ1, 'minecraft:spruce_door[facing=south,half=upper,hinge=right,open=false]')
o.set(29, 11, SZ1, GLASS); o.set(30, 11, SZ1, GLASS)
for x in range(27, 33):
    o.set(x, 13, SZ1, TRIM)
word("1880", 28, 13, SZ1 + 1, 'south', 5)

# ---- porche: postes, techo = piso del balcón, barandal del balcón
for x in (15, 19, 23, 27, 32, 36, 40, 44):
    for y in range(1, Y2):
        o.set(x, y, 42, POST)
    o.set(x, Y2 - 1, 41, stairs('minecraft:spruce_stairs', 'south', 'top'))
for x in range(SX0, SX1 + 1):
    for z in range(39, 43):
        o.set(x, Y2, z, 'minecraft:spruce_planks')
    o.set(x, Y2, 43, stairs('minecraft:spruce_stairs', 'south'))
    o.set(x, Y2 + 1, 42, 'minecraft:spruce_fence')
for z in range(39, 43):
    o.set(SX0, Y2 + 1, z, 'minecraft:spruce_fence'); o.set(SX1, Y2 + 1, z, 'minecraft:spruce_fence')
for x in (15, 19, 23, 27, 32, 36, 40, 44):
    o.set(x, Y2 + 1, 42, 'minecraft:spruce_fence')
# faroles del porche y del balcón
for x in (21, 25, 34, 38):
    o.set(x, Y2 - 1, 40, 'minecraft:lantern[hanging=true]')
# barandas para amarrar caballos y bebederos frente al saloon
for x in list(range(17, 22)) + list(range(38, 43)):
    o.set(x, 1, 44, 'minecraft:spruce_fence')
for x in (17, 21, 38, 42):
    o.set(x, 1, 44, 'minecraft:stripped_spruce_wood[axis=y]')
for x in (23, 24, 35, 36):
    o.set(x, 1, 44, 'minecraft:water_cauldron[level=3]')

# ---- techo: dos aguas bajas detrás del frontón (cumbrera al centro)
for x in range(SX0, SX1 + 1):
    h = YR + min(x - SX0, SX1 - x) // 3
    for z in range(SZ0, SZ1):
        o.set(x, h, z, 'minecraft:spruce_planks' if x in (29, 30) else 'minecraft:dark_oak_planks')
        for y in range(YR, h):
            if x in (SX0, SX1) or z == SZ0:
                o.set(x, y, z, CLAP)
for z in range(SZ0, SZ1):
    for x in range(SX0, SX1 + 1):
        h = YR + min(x - SX0, SX1 - x) // 3
        for y in range(YR, h):
            if z == SZ0:
                o.set(x, y, z, CLAP)

# ================================================================== INTERIOR, PLANTA BAJA
# muros dobles: por fuera tablas, por dentro forro (lambrín abajo, tapiz arriba, molduras)
LX0, LX1, LZ0 = SX0 + 1, SX1 - 1, SZ0 + 1
IX0, IX1, IZ0, IZ1 = SX0 + 2, SX1 - 2, SZ0 + 2, SZ1 - 1


def lining(y):
    if y == Y2:
        return TRIM
    yy = y if y < Y2 else y - Y2
    if y < Y2:
        return WAIN if yy <= 2 else TRIM if yy in (3, Y2 - 1) else PAPER
    return WAIN if yy == 1 else TRIM if y == YR - 1 else PAPER


for y in range(1, YR):
    for z in range(LZ0, SZ1):
        o.set(LX0, y, z, lining(y)); o.set(LX1, y, z, lining(y))
    for x in range(LX0, LX1 + 1):
        o.set(x, y, LZ0, lining(y))
for y in range(1, YR):
    for x in range(LX0, LX1 + 1):
        o.set(x, y, SZ1 - 1, lining(y))
# huecos con marco en el forro del frente: puerta de la calle, ventanas y puerta del balcón
frame_opening(29, 30, 1, 3, z=SZ1 - 1)
frame_opening(19, 22, 2, 4, z=SZ1 - 1); frame_opening(37, 40, 2, 4, z=SZ1 - 1)
frame_opening(19, 21, 10, 12, z=SZ1 - 1); frame_opening(38, 40, 10, 12, z=SZ1 - 1)
frame_opening(29, 30, Y2 + 1, Y2 + 3, z=SZ1 - 1)
# puertas a los anexos (rodeo al oeste, tiro al este) atraviesan los dos muros
for y in (1, 2, 3):
    for z in (30, 31):
        for x in (SX0, LX0, SX1, LX1):
            o.set(x, y, z, AIR)
for x in (SX0, LX0, SX1, LX1):
    o.set(x, 4, 30, TRIM); o.set(x, 4, 31, TRIM)
# marcos de tronco en el paso (se veía la tabla blanca de afuera por dentro del hueco)
for x in (SX0, SX1):
    for z in (29, 32):
        for y in range(1, 5):
            o.set(x, y, z, 'minecraft:stripped_spruce_log[axis=y]')
    o.set(x, 4, 30, 'minecraft:stripped_spruce_log[axis=z]'); o.set(x, 4, 31, 'minecraft:stripped_spruce_log[axis=z]')
frame_opening(30, 31, 1, 3, x=LX0); frame_opening(30, 31, 1, 3, x=LX1)

# ---- techo de la planta baja = piso de arriba (alrededor del salón alto del centro)
ATX0, ATX1, ATZ0, ATZ1 = 22, 37, 18, 29        # hueco de doble altura sobre el centro del salón
for x in range(IX0, IX1 + 1):
    for z in range(IZ0, IZ1 + 1):
        hole = ATX0 <= x <= ATX1 and ATZ0 <= z <= ATZ1
        stair_hole = x in (29, 30) and 12 <= z <= 14
        if not hole and not stair_hole:
            o.set(x, Y2, z, 'minecraft:spruce_planks')
            if z < SZ1 - 1:                     # la fila del frente es el forro (no se borra)
                o.set(x, Y2 - 1, z, AIR)
# vigas bajo el piso de arriba
for z in range(IZ0, IZ1 + 1, 4):
    for x in range(IX0, IX1 + 1):
        if o.get(x, Y2, z) == 'minecraft:spruce_planks':
            o.set(x, Y2, z, BEAM)
# barandal de la galería alrededor del hueco
for x in range(ATX0, ATX1 + 1):
    o.set(x, Y2 + 1, ATZ0 - 1, 'minecraft:spruce_fence'); o.set(x, Y2 + 1, ATZ1 + 1, 'minecraft:spruce_fence')
for z in range(ATZ0 - 1, ATZ1 + 2):
    o.set(ATX0 - 1, Y2 + 1, z, 'minecraft:spruce_fence'); o.set(ATX1 + 1, Y2 + 1, z, 'minecraft:spruce_fence')
for x, z in ((ATX0 - 1, ATZ0 - 1), (ATX1 + 1, ATZ0 - 1), (ATX0 - 1, ATZ1 + 1), (ATX1 + 1, ATZ1 + 1)):
    o.set(x, Y2 + 1, z, 'minecraft:stripped_spruce_wood[axis=y]')
    for y in range(1, Y2):
        o.set(x, y, z, POST)                    # columnas que sostienen la galería
for x in range(ATX0, ATX1 + 1):
    o.set(x, Y2, ATZ0 - 1, stairs('minecraft:spruce_stairs', 'north', 'top') if False else 'minecraft:stripped_spruce_wood[axis=x]')
    o.set(x, Y2, ATZ1 + 1, 'minecraft:stripped_spruce_wood[axis=x]')
for z in range(ATZ0, ATZ1 + 1):
    o.set(ATX0 - 1, Y2, z, 'minecraft:stripped_spruce_wood[axis=z]'); o.set(ATX1 + 1, Y2, z, 'minecraft:stripped_spruce_wood[axis=z]')

# ---- escalera al centro del fondo (sube hacia el norte): puertitas VIP abajo y dos sheriffs
for k in range(8):
    z, y = 18 - k, 1 + k
    for x in (29, 30):
        o.set(x, y, z, stairs('minecraft:spruce_stairs', 'north'))
        for yy in range(1, y):
            o.set(x, yy, z, 'minecraft:spruce_planks')
    for x in (28, 31):
        for yy in range(1, y + 1):
            o.set(x, yy, z, 'minecraft:spruce_planks')
        o.set(x, y + 1, z, 'minecraft:spruce_fence')
o.set(29, 1, 19, 'casino:vip_gate[facing=north,hinge=left,access=1]')
o.set(30, 1, 19, 'casino:vip_gate[facing=north,hinge=right,access=1]')
o.set(28, 1, 19, 'minecraft:stripped_spruce_wood[axis=y]'); o.set(31, 1, 19, 'minecraft:stripped_spruce_wood[axis=y]')
o.set(28, 2, 19, 'minecraft:lantern'); o.set(31, 2, 19, 'minecraft:lantern')
o.marker(27, 1, 19, 'guard 2 w', [(0, 0, 0, 'minecraft:black_wool'), (0, 1, 0, 'minecraft:black_wool')])
o.marker(32, 1, 19, 'guard 2 w', [(0, 0, 0, 'minecraft:black_wool'), (0, 1, 0, 'minecraft:black_wool')])
# hueco de la escalera arriba con barandal
for z in range(12, 15):
    o.set(28, Y2 + 1, z, 'minecraft:spruce_fence'); o.set(31, Y2 + 1, z, 'minecraft:spruce_fence')
for x in (28, 29, 30, 31):
    o.set(x, Y2 + 1, 15, 'minecraft:spruce_fence')

# ---- barra larga (oeste): repisas con botellas y espejo en la pared, cantinero y taburetes
BAR_X = 19
for z in range(11, 28):
    o.set(BAR_X, 1, z, f'casino:bar_counter[facing=east,shape=straight]')
    o.set(BAR_X + 1, 1, z, 'casino:saloon_stool[facing=west]' if z % 2 == 1 else AIR)
for z in range(11, 28, 2):
    o.set(BAR_X, 2, z, f'casino:drinks[facing=east,drinks={1 + (z // 2) % 3}]')
for z in range(10, 29):
    o.set(SX0 + 2, 2, z, 'casino:bottle_shelf[facing=east]')
    o.set(SX0 + 2, 3, z, 'casino:bottle_shelf[facing=east]')
    o.set(SX0 + 2, 1, z, 'minecraft:barrel[facing=east]' if z % 3 else 'minecraft:spruce_planks')
for z in range(12, 27):
    o.set(LX0, 4, z, 'minecraft:light_gray_stained_glass')   # espejo detrás de la barra
    o.set(LX0, 5, z, 'minecraft:light_gray_stained_glass')
for i, ch in enumerate("WHISKEY"):              # mirando al este se lee hacia el norte
    o.set(SX0 + 2, 6, 22 - i, letter(ch, 'east', 2))
o.marker(18, 1, 15, 'bartender 1 w', [(0, 0, 0, 'minecraft:purple_wool'), (0, 1, 0, 'minecraft:purple_wool')])
o.marker(18, 1, 23, 'bartender 1 w', [(0, 0, 0, 'minecraft:purple_wool'), (0, 1, 0, 'minecraft:purple_wool')])
for z in (12, 19, 26):
    lantern(18, Y2, z, 1)

# ---- pianola en la esquina del frente-oeste y mesas de cartas
o.set(17, 1, 33, 'casino:player_piano[facing=east,lit=true]')
o.set(17, 1, 32, stairs('minecraft:dark_oak_stairs', 'east')); o.set(17, 2, 33, AIR)
o.set(18, 1, 33, 'casino:saloon_stool[facing=west]')                # banquito del pianista
# mesas redondas con sillas de madera (las del frente-oeste junto a la pianola) y una de póker del lado este
table(21, 31, drinks=2); table(21, 35, drinks=3); table(25, 34, chairs='nswe')
table(34, 34, kind='poker_table'); table(35, 30, drinks=1, chairs='sw')

# ---- caja con rejas (frente-este) y tienda de premios; entre las dos queda el paso a la galería de tiro (z 30-31)
for z in range(32, 37):
    o.set(39, 1, z, 'minecraft:spruce_planks'); o.set(39, 2, z, 'minecraft:iron_bars'); o.set(39, 3, z, 'minecraft:iron_bars')
    o.set(39, 4, z, TRIM)
for x in range(39, 44):
    o.set(x, 1, 32, 'minecraft:spruce_planks'); o.set(x, 2, 32, 'minecraft:iron_bars'); o.set(x, 3, 32, 'minecraft:iron_bars')
    o.set(x, 4, 32, TRIM)
for x in range(40, 44):
    for z in range(33, 37):
        o.set(x, 0, z, 'minecraft:spruce_planks')
mq.place(o, 'cashier', 39, 1, 33, 3)
mq.place(o, 'cashier', 39, 1, 35, 3)
o.set(39, 2, 33, AIR); o.set(39, 2, 35, AIR)
for i, ch in enumerate("CAJA"):
    o.set(38, 4, 33 + i, letter(ch, 'west', 2))
o.set(42, 1, 34, 'minecraft:chest[facing=west]'); o.set(43, 1, 33, 'minecraft:barrel[facing=up]')
o.marker(42, 1, 28, 'shop 0', [(0, 0, 0, 'casino:prize_shop[facing=north]')])
o.marker(40, 1, 28, 'shop 0', [(0, 0, 0, 'casino:prize_shop[facing=north]')])

# ---- mesa de vencidas en el salón (de pie; el retador del lado este, el forzudo o el otro jugador del lado oeste)
o.set(26, 1, 28, 'casino:arm_table[facing=east]')

# ---- pared este: ruleta de la suerte entre tragamonedas
mq.place(o, 'wheel', IX1, 1, 19, 3)
o.marker(41, 1, 21, 'wheelhost 3 w', [(0, 0, 0, 'minecraft:red_wool'), (0, 1, 0, 'minecraft:red_wool')])
for z in (11, 13, 15, 23, 25, 27):
    mq.place(o, 'slot', IX1, 1, z, 3)
    o.set(IX1 - 1, 1, z, 'casino:saloon_stool[facing=east]')
for z in (12, 14, 16, 22, 24, 26):
    o.set(IX1, 3, z, 'minecraft:lantern')

# ---- dos mesas de 21 (crupier del Oeste) en el centro
for x in (26, 33):
    mq.place(o, 'blackjack', x, 1, 22, 2)
    meta, pv = o.markers[(x, 1, 22)]
    o.markers[(x, 1, 22)] = (meta + ' w', pv)
mq.place(o, 'scratch', 22, 1, 26, 1)
mq.place(o, 'scratch', 37, 1, 26, 3)

for i, ch in enumerate("RODEO"):
    o.set(SX0 + 2, 5, 33 - i, letter(ch, 'east', 0))
for i, ch in enumerate("TIRO"):
    o.set(SX1 - 2, 5, 29 + i, letter(ch, 'west', 0))

# ---- candiles de rueda sobre el salón alto y faroles bajo la galería
for x in (26, 33):
    for z in (21, 26):
        chandelier(x, YR - 3, z)
for x in (18, 41):
    for z in (31, 35):
        lantern(x, Y2, z, 1)
for x in (24, 35):
    lantern(x, Y2, 32, 1)
# cuadros: carteles de "Se busca" en las paredes
o.set(SX1 - 2, 4, 33, 'casino:wanted_poster[design=0,facing=west]')
o.set(SX0 + 2, 4, 33, 'casino:wanted_poster[design=1,facing=east]')
o.set(24, 4, IZ0, 'casino:wanted_poster[design=2,facing=south]')
o.set(35, 4, IZ0, 'casino:wanted_poster[design=3,facing=south]')
# cornamenta (cráneo de toro) sobre la escalera

# ================================================================== PRIMER PISO (tarjeta Plata)
# muros interiores de los cuartos del frente (z 32..37) y su pasillo (z 30..31)
for x in range(IX0, IX1 + 1):
    for y in range(Y2 + 1, YR):
        o.set(x, y, 32, PAPER if Y2 + 2 <= y <= YR - 2 else WAIN)
for z in range(32, IZ1 + 1):
    for y in range(Y2 + 1, YR):
        o.set(23, y, z, WAIN if y == Y2 + 1 else PAPER); o.set(36, y, z, WAIN if y == Y2 + 1 else PAPER)
# puertas de los cuartos y la puertita Oro del palco
for x, h in ((19, 'left'), (40, 'right')):
    o.set(x, Y2 + 1, 32, f'minecraft:spruce_door[facing=south,half=lower,hinge={h},open=false]')
    o.set(x, Y2 + 2, 32, f'minecraft:spruce_door[facing=south,half=upper,hinge={h},open=false]')
for x, h in ((29, 'right'), (30, 'left')):      # mirando al sur, la izquierda es el este
    o.set(x, Y2 + 1, 32, f'casino:vip_gate[facing=south,hinge={h},access=2]')
    o.set(x, Y2 + 2, 32, AIR); o.set(x, Y2 + 3, 32, AIR)
for i, ch in enumerate("JEFE"):                 # mirando al norte se lee hacia el oeste
    o.set(31 - i, Y2 + 4, 31, letter(ch, 'north', 2))
# techo del primer piso: tablas con vigas (cerrado: antes se veía el hueco del tejado entre las vigas)
for x in range(IX0 - 1, IX1 + 2):
    for z in range(IZ0 - 1, IZ1 + 1):
        o.set(x, YR, z, BEAM if (z - IZ0) % 3 == 0 else 'minecraft:spruce_planks')
# sala de cartas del fondo (z 9..17): 2 mesas de alto riesgo, aparador y retratos
for x in (22,):
    mq.place(o, 'blackjack', x, Y2 + 1, 13, 2)
    meta, pv = o.markers[(x, Y2 + 1, 13)]
    o.markers[(x, Y2 + 1, 13)] = (meta + ' w', pv)
# mesa de dados del mentiroso: el viejo Hank del lado norte, la silla del jugador al sur
o.set(37, Y2 + 1, 13, 'minecraft:spruce_fence')
o.set(37, Y2 + 2, 13, 'casino:liars_dice[facing=south]')
o.set(37, Y2 + 1, 14, 'casino:saloon_chair[facing=north]')
for x, f in ((36, 'east'), (38, 'west')):
    o.set(x, Y2 + 1, 13, f'casino:saloon_chair[facing={f}]')
table(33, 12, y=Y2 + 1, kind='poker_table')
o.marker(37, Y2 + 1, 12, 'dicer 2', [(0, 0, 0, 'minecraft:orange_wool'), (0, 1, 0, 'minecraft:orange_wool')])
for x in range(IX0, IX1 + 1):
    if x not in (28, 29, 30, 31):
        o.set(x, Y2 + 1, IZ0, 'casino:bar_counter[facing=south,shape=straight]')
for x in (18, 26, 33, 41):
    o.set(x, Y2 + 2, IZ0, 'casino:drinks[facing=south,drinks=3]')
for x in (20, 25, 34, 39):
    o.set(x, Y2 + 3, IZ0, f'casino:wanted_poster[design={(x // 5) % 4},facing=south]')
for x in (19, 26, 33, 40):
    lantern(x, YR, 14, 1)
o.marker(17, Y2 + 1, 11, 'wpatron 2 5', [(0, 0, 0, 'minecraft:light_blue_wool')])
o.marker(42, Y2 + 1, 15, 'wpatron 3 2', [(0, 0, 0, 'minecraft:light_blue_wool')])
# galería: mesitas junto al barandal
for z in (20, 24, 28):
    table(18, z, y=Y2 + 1, chairs='ns', drinks=1 + z % 3)
    table(41, z, y=Y2 + 1, chairs='ns', drinks=1 + (z + 1) % 3)
for z in (22, 26):
    lantern(17, YR, z, 1); lantern(42, YR, z, 1)
# cuartos del frente: el del oeste con cama, el del este con mesa de juego
o.set(17, Y2 + 1, 36, 'minecraft:white_bed[facing=south,part=head]'); o.set(17, Y2 + 1, 35, 'minecraft:white_bed[facing=south,part=foot]')
table(21, 35, y=Y2 + 1, chairs='n', drinks=1)
o.set(17, Y2 + 1, 33, 'minecraft:chest[facing=east]')
lantern(20, YR, 34, 1)
table(40, 34, y=Y2 + 1, kind='poker_table', chairs='nswe')
lantern(39, YR, 34, 1)
# palco del dueño (tarjeta Oro): sillones de piel, mesa de cartas, aparador de whisky; sale al balcón
for x in (25, 34):
    o.set(x, Y2 + 1, 36, f'casino:sofa_black[facing={"east" if x < CX else "west"}]')
    o.set(x, Y2 + 1, 35, f'casino:sofa_black[facing={"east" if x < CX else "west"}]')
o.set(29, Y2 + 1, 34, 'casino:poker_table'); o.set(30, Y2 + 1, 34, 'casino:poker_table')
o.set(29, Y2 + 2, 34, 'casino:drinks[facing=north,drinks=3]')
for x in (29, 30):
    o.set(x, Y2 + 1, 35, 'casino:saloon_chair[facing=north]')
for x in (28, 31):
    o.set(x, Y2 + 1, 34, f'casino:saloon_chair[facing={"east" if x < CX else "west"}]')
for x in (24, 35):
    o.set(x, Y2 + 1, 33, 'casino:bottle_shelf[facing=south]' if False else 'minecraft:bookshelf')
o.set(26, Y2 + 3, 36, 'casino:wanted_poster[design=0,facing=north]')
o.set(33, Y2 + 3, 36, 'casino:wanted_poster[design=3,facing=north]')
lantern(29, YR, 35, 1); lantern(30, YR, 35, 1)

# ================================================================== ANEXO OESTE: RODEO (toro mecánico)
RX0, RX1, RZ0 = 3, SX0, 20
for y in range(1, 6):
    for x in range(RX0, RX1 + 1):
        o.set(x, y, RZ0, CLAP)
        if o.get(x, y, SZ1) in (AIR, None) or x < SX0:
            o.set(x, y, SZ1, CLAP)
    for z in range(RZ0, SZ1 + 1):
        o.set(RX0, y, z, CLAP)
for y in range(1, 6):
    o.set(RX0, y, RZ0, POST); o.set(RX0, y, SZ1, POST)
for x in range(RX0 + 1, RX1):
    for z in range(RZ0 + 1, SZ1):
        o.set(x, 0, z, 'minecraft:spruce_planks')
        o.set(x, 6, z, 'minecraft:spruce_planks')
    o.set(x, 6, RZ0, TRIM); o.set(x, 6, SZ1, TRIM)
for z in range(RZ0, SZ1 + 1):
    o.set(RX0, 6, z, TRIM)
# fachada falsa con "RODEO"
for x in range(RX0, RX1):
    for y in range(6, 10):
        o.set(x, y, SZ1, CLAP)
    o.set(x, 10, SZ1, stairs('minecraft:dark_oak_stairs', 'south'))
word("RODEO", 7, 8, SZ1 + 1, 'south', 0)
for y in range(1, 4):
    for x in (8, 9, 10):
        o.set(x, y, SZ1, AIR)
for x in (7, 11):
    for y in range(1, 4):
        o.set(x, y, SZ1, RED)
for x in range(7, 12):
    o.set(x, 4, SZ1, RED)
window(4, 5, 2, 3, SZ1); window(13, 14, 2, 3, SZ1) if False else window(13, 13, 2, 3, SZ1)
# porche del anexo
for x in range(RX0, RX1):
    for z in range(39, 43):
        o.set(x, 5, z, 'minecraft:spruce_slab[type=bottom]')
for x in (3, 9, 14):
    for y in range(1, 5):
        o.set(x, y, 42, POST)
# forro por dentro (antes se veía la tabla blanca de afuera): oeste, fondo, frente y el muro del saloon
line_wall_x(RX0 + 1, RZ0 + 1, SZ1 - 1)
line_wall_x(SX0 - 1, RZ0 + 1, SZ1 - 1)
line_wall_z(RZ0 + 1, RX0 + 1, SX0 - 1)
line_wall_z(SZ1 - 1, RX0 + 1, SX0 - 1)
frame_opening(30, 31, 1, 3, x=SX0 - 1)                       # puerta al saloon
frame_opening(8, 10, 1, 3, z=SZ1 - 1)                        # entrada de la calle
frame_opening(13, 13, 2, 3, z=SZ1 - 1)                       # ventanas del frente
frame_opening(5, 5, 2, 3, z=SZ1 - 1)
# techo con vigas
for z in range(RZ0 + 2, SZ1, 4):
    for x in range(RX0 + 1, SX0):
        o.set(x, 6, z, 'minecraft:stripped_dark_oak_log[axis=x]')
# ruedo: arena (con tierra pisada) y cerca redonda; queda un pasillo entre la cerca y las paredes
RCX, RCZ = 9, 29
for x in range(RX0 + 2, SX0 - 1):
    for z in range(RZ0 + 2, SZ1 - 1):
        d = ((x - RCX) ** 2 + (z - RCZ) ** 2) ** 0.5
        if d <= 2.8:
            o.set(x, 0, z, 'minecraft:sand' if R.random() < 0.8 else 'minecraft:coarse_dirt')
        elif d <= 3.6:
            o.set(x, 1, z, 'minecraft:spruce_fence')
o.set(RCX, 1, RCZ + 3, 'minecraft:spruce_fence_gate[facing=south,open=false,in_wall=false]')
o.set(RCX, 1, RCZ, 'casino:mechanical_bull[facing=south]')    # el toro mira a la puerta del ruedo (el letrero del récord lo dibuja él)
# gradas al fondo: banca alta contra la pared y banca baja enfrente
for x in range(RX0 + 2, SX0 - 1):
    o.set(x, 1, RZ0 + 1 + 1, 'minecraft:spruce_planks')
    o.set(x, 2, RZ0 + 2, 'casino:saloon_bench[facing=south]')
    o.set(x, 1, RZ0 + 3, 'casino:saloon_bench[facing=south]')
# pacas, barriles y una mesita a la entrada
for x, z in ((5, 36), (13, 36), (5, 35)):
    o.set(x, 1, z, 'minecraft:hay_block[axis=y]')
o.set(13, 1, 34, 'minecraft:barrel[facing=up]'); o.set(13, 2, 34, 'casino:drinks[facing=north,drinks=2]')
o.set(6, 1, 26, 'minecraft:barrel[facing=up]'); o.set(12, 1, 26, 'minecraft:barrel[facing=up]')
o.set(RX0 + 2, 4, 30, 'casino:wanted_poster[design=1,facing=east]'); o.set(SX0 - 2, 4, 28, 'casino:wanted_poster[design=2,facing=west]')
chandelier(RCX, 5, RCZ, chain=0)
for z in (24, 34):
    lantern(RCX, 6, z, 1)
o.marker(5, 1, 33, 'wpatron 1 0', [(0, 0, 0, 'minecraft:light_blue_wool')])
o.marker(13, 1, 25, 'wpatron 3 3', [(0, 0, 0, 'minecraft:light_blue_wool')])

# ================================================================== ANEXO ESTE: TIRO (galería de tiro al blanco)
TX0, TX1, TZ0 = SX1, 57, 18
for y in range(1, 6):
    for x in range(TX0, TX1 + 1):
        o.set(x, y, TZ0, CLAP)
        if x > SX1:
            o.set(x, y, SZ1, CLAP)
    for z in range(TZ0, SZ1 + 1):
        o.set(TX1, y, z, CLAP)
for y in range(1, 6):
    o.set(TX1, y, TZ0, POST); o.set(TX1, y, SZ1, POST)
for x in range(TX0 + 1, TX1):
    for z in range(TZ0 + 1, SZ1):
        o.set(x, 0, z, 'minecraft:spruce_planks')
        o.set(x, 6, z, 'minecraft:spruce_planks')
    o.set(x, 6, TZ0, TRIM); o.set(x, 6, SZ1, TRIM)
for z in range(TZ0, SZ1 + 1):
    o.set(TX1, 6, z, TRIM)
for x in range(TX0 + 1, TX1 + 1):
    for y in range(6, 10):
        o.set(x, y, SZ1, CLAP)
    o.set(x, 10, SZ1, stairs('minecraft:dark_oak_stairs', 'south'))
word("TIRO", 49, 8, SZ1 + 1, 'south', 0)
o.set(48, 8, SZ1 + 1, letter('★', 'south', 2)); o.set(53, 8, SZ1 + 1, letter('★', 'south', 2))
for y in range(1, 4):
    for x in (50, 51):
        o.set(x, y, SZ1, AIR)
for y in range(1, 5):
    o.set(49, y, SZ1, RED); o.set(52, y, SZ1, RED)
o.set(50, 4, SZ1, RED); o.set(51, 4, SZ1, RED)
o.set(50, 1, SZ1, 'casino:saloon_door[facing=north,hinge=left]')
o.set(51, 1, SZ1, 'casino:saloon_door[facing=north,hinge=right]')
window(46, 47, 2, 3, SZ1); window(54, 55, 2, 3, SZ1)
for x in range(SX1 + 1, TX1 + 1):
    for z in range(39, 43):
        o.set(x, 5, z, 'minecraft:spruce_slab[type=bottom]')
for x in (45, 50, 51, 56):
    for y in range(1, 5):
        o.set(x, y, 42, POST)
# mostrador con la caja de la galería (campana): los blancos, el agua de los patitos, la repisa de las botellas, el riel de los
# bandidos y el marcador los dibuja el renderizador de la galería (casino:shooting_gallery, mira al tirador)
for x in range(TX0 + 1, TX1):
    o.set(x, 1, 30, 'minecraft:spruce_planks'); o.set(x, 2, 30, 'minecraft:spruce_slab[type=bottom]')
o.set(TX0 + 1, 1, 30, AIR); o.set(TX0 + 1, 2, 30, AIR)            # paso detrás del mostrador
o.set(50, 2, 30, 'casino:shooting_gallery[facing=south]')
for x in range(TX0 + 1, TX1):
    for z in range(TZ0 + 1, 30):
        o.set(x, 0, z, 'minecraft:sand' if (x + z) % 3 else 'minecraft:smooth_sandstone')
# telón pintado del fondo: desierto con mesas, cactus y sol
for x in range(TX0 + 1, TX1):
    for y in range(1, 6):
        st = ('minecraft:cut_sandstone' if y == 1 else 'minecraft:sandstone' if y == 2 else 'minecraft:orange_terracotta' if y == 3
              else 'minecraft:light_blue_terracotta')
        if y == 3 and x % 4 == 0:
            st = 'minecraft:red_terracotta'
        o.set(x, y, TZ0 + 1, st)
for x, y in ((47, 2), (47, 3), (55, 2), (55, 3), (54, 3)):
    o.set(x, y, TZ0 + 1, 'minecraft:green_terracotta')
o.set(53, 5, TZ0 + 1, 'minecraft:yellow_terracotta'); o.set(52, 5, TZ0 + 1, 'minecraft:yellow_terracotta')
o.set(53, 4, TZ0 + 1, 'minecraft:yellow_terracotta')
# canal de los patitos (fila de enfrente) y postes de la repisa de las botellas
for x in range(TX0 + 1, TX1):
    o.set(x, 1, 25, 'minecraft:spruce_planks')
    o.set(x, 1, 26, 'minecraft:spruce_slab[type=bottom]')
for x in (TX0 + 1, TX1 - 1):
    o.set(x, 1, 23, 'minecraft:spruce_fence'); o.set(x, 2, 23, 'minecraft:spruce_fence')
    o.set(x, 1, 20, 'minecraft:spruce_fence'); o.set(x, 2, 20, 'minecraft:spruce_fence')
# faroles a los lados (para no tapar los blancos)
for z in (22, 27):
    lantern(TX0 + 1, 6, z, 1); lantern(TX1 - 1, 6, z, 1)
lantern(48, 6, 34, 1); lantern(54, 6, 34, 1)
o.marker(53, 1, 33, 'wpatron 2 1', [(0, 0, 0, 'minecraft:light_blue_wool')])
# forro por dentro en el lado del público (los costados del escenario quedan como caseta)
line_wall_z(SZ1 - 1, TX0 + 1, TX1 - 1)
line_wall_x(TX1 - 1, TZ0 + 2, SZ1 - 1)
line_wall_x(TX0 + 1, TZ0 + 2, 29); line_wall_x(TX0 + 1, 32, SZ1 - 1)       # (z 30-31: la puerta que viene del saloon)
for z in (30, 31):
    o.set(TX0 + 1, 5, z, annex_lining(5, z, 'z'))
frame_opening(30, 31, 1, 3, x=TX0 + 1)
frame_opening(50, 51, 1, 3, z=SZ1 - 1)                       # entrada de la calle
frame_opening(46, 47, 2, 3, z=SZ1 - 1); frame_opening(54, 55, 2, 3, z=SZ1 - 1)
for z in range(TZ0 + 2, SZ1, 4):
    for x in range(TX0 + 1, TX1):
        o.set(x, 6, z, 'minecraft:stripped_dark_oak_log[axis=x]')
# banca para los mirones al fondo y mesita
for x in range(46, 50):
    o.set(x, 1, 36, 'casino:saloon_bench[facing=north]')
for x in range(52, 56):
    o.set(x, 1, 36, 'casino:saloon_bench[facing=north]')
for z in (33, 35):
    o.set(TX1 - 2, 3, z, f'casino:wanted_poster[design={z % 4},facing=west]')

# ================================================================== PATIO TRASERO: torre de agua, letrina, barriles
WTX, WTZ = 51, 6
for x, z in ((WTX, WTZ), (WTX + 4, WTZ), (WTX, WTZ + 4), (WTX + 4, WTZ + 4)):
    for y in range(1, 11):
        o.set(x, y, z, POST)
for y in (4, 8):
    for k in range(WTX, WTX + 5):
        o.set(k, y, WTZ, 'minecraft:spruce_fence'); o.set(k, y, WTZ + 4, 'minecraft:spruce_fence')
    for k in range(WTZ, WTZ + 5):
        o.set(WTX, y, k, 'minecraft:spruce_fence'); o.set(WTX + 4, y, k, 'minecraft:spruce_fence')
for x in range(WTX - 1, WTX + 6):
    for z in range(WTZ - 1, WTZ + 6):
        corner = x in (WTX - 1, WTX + 5) and z in (WTZ - 1, WTZ + 5)
        if corner:
            continue
        o.set(x, 11, z, 'minecraft:spruce_planks')
        edge = x in (WTX - 1, WTX + 5) or z in (WTZ - 1, WTZ + 5) or (x in (WTX, WTX + 4) and z in (WTZ, WTZ + 4) and False)
        for y in range(12, 16):
            if edge:
                o.set(x, y, z, 'minecraft:stripped_spruce_wood[axis=y]' if (x + z) % 2 else 'minecraft:spruce_planks')
            else:
                o.set(x, y, z, 'minecraft:water' if y < 15 else 'minecraft:spruce_planks')
for x in range(WTX, WTX + 5):
    for z in range(WTZ, WTZ + 5):
        o.set(x, 16, z, 'minecraft:dark_oak_slab[type=bottom]')
o.set(WTX + 2, 16, WTZ + 2, 'minecraft:dark_oak_planks'); o.set(WTX + 2, 17, WTZ + 2, 'minecraft:dark_oak_slab[type=bottom]')
# letrina
for y in range(1, 4):
    for x, z in ((6, 4), (8, 4), (6, 6), (8, 6)):
        o.set(x, y, z, 'minecraft:spruce_planks')
    o.set(7, y, 4, 'minecraft:spruce_planks'); o.set(6, y, 5, 'minecraft:spruce_planks'); o.set(8, y, 5, 'minecraft:spruce_planks')
o.set(7, 1, 6, 'minecraft:spruce_door[facing=south,half=lower,hinge=left,open=false]')
o.set(7, 2, 6, 'minecraft:spruce_door[facing=south,half=upper,hinge=left,open=false]')
o.set(7, 3, 6, 'minecraft:spruce_planks')
for x in range(5, 10):
    for z in range(3, 8):
        o.set(x, 4, z, 'minecraft:spruce_slab[type=bottom]')
# barriles, cajas, carreta de heno y cactus
for x, z in ((14, 5), (15, 5), (14, 6), (40, 4), (41, 4), (20, 3), (24, 5)):
    o.set(x, 1, z, 'minecraft:barrel[facing=up]')
o.set(15, 2, 5, 'minecraft:barrel[facing=up]')
for x, z in ((27, 4), (28, 4), (27, 5)):
    o.set(x, 1, z, 'minecraft:hay_block[axis=x]')
for x, z in ((2, 14), (12, 2), (33, 2), (46, 12), (58, 15), (1, 44), (58, 46)):
    o.set(x, 0, z, 'minecraft:sand')
    for y in range(1, 2 + (x + z) % 3):
        o.set(x, y, z, 'minecraft:cactus')
o.marker(26, 1, 6, 'wpatron 0 7', [(0, 0, 0, 'minecraft:light_blue_wool')])

# ================================================================== DUELO AL MEDIODÍA
# campana a la orilla de la calle (mira hacia donde aparece el pistolero); las X de gis (la tuya en (22,47) y la del pistolero en
# (38,47), justo frente a las puertas del saloon) las dibuja la propia campana. Sin matas en la línea del duelo.
for x in range(18, 43):
    for z in (46, 47, 48):
        if o.get(x, 1, z) == 'minecraft:dead_bush':
            o.set(x, 1, z, AIR)
            o.set(x, 0, z, 'minecraft:coarse_dirt')
o.set(22, 1, 49, 'casino:duel_bell[facing=east]')

# ================================================================== CLIENTES DEL SALOON
for (x, y, z, d, s) in [(24, 1, 29, 2, 0), (35, 1, 29, 2, 1), (21, 1, 18, 1, 2), (38, 1, 16, 3, 4), (28, 1, 34, 2, 6),
                        (31, 1, 26, 0, 3), (22, 1, 34, 3, 5)]:
    o.marker(x, y, z, f'wpatron {d} {s}', [(0, 0, 0, 'minecraft:light_blue_wool')])

# ================================================================== UMBRALES: bajo los muros (y sus puertas) va madera, no tierra
TERRAIN = ('minecraft:coarse_dirt', 'minecraft:sand', 'minecraft:dirt', 'minecraft:packed_mud', 'minecraft:dirt_path')


def floor_under(x0, x1, z0, z1, st):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x in (x0, x1) or z in (z0, z1)) and o.get(x, 0, z) in TERRAIN:
                o.set(x, 0, z, st)


floor_under(SX0, SX1, SZ0, SZ1, FLOOR)
floor_under(RX0, RX1, RZ0, SZ1, 'minecraft:spruce_planks')
floor_under(TX0, TX1, TZ0, SZ1, 'minecraft:spruce_planks')

# ================================================================== LUZ ESCONDIDA (sin monstruos) y uniones de cercas
# (probado en el juego: con rejilla de 5 y nivel 11 había rincones con luz 6-9; ahora rejilla de 4 y nivel 13)
for x in range(SX0 + 2, SX1 - 1, 4):
    for z in range(SZ0 + 2, SZ1 - 1, 4):
        for y in (1, Y2 + 1):
            if o.get(x, y, z) == AIR and o.get(x, y + 1, z) == AIR:
                o.set(x, y + 1, z, 'minecraft:light[level=13]')
# rincón de la pianola: luz extra para que se vea el teclado
for x, z in ((19, 32), (19, 34)):
    if o.get(x, 3, z) == AIR:
        o.set(x, 3, z, 'minecraft:light[level=14]')
for x in range(RX0 + 2, RX1, 5):
    for z in range(RZ0 + 2, SZ1, 5):
        if o.get(x, 2, z) == AIR:
            o.set(x, 3, z, 'minecraft:light[level=11]')
for x in range(TX0 + 2, TX1, 5):
    for z in range(TZ0 + 2, SZ1, 5):
        if o.get(x, 3, z) == AIR:
            o.set(x, 3, z, 'minecraft:light[level=11]')


def _connect():
    """Las cercas, rejas y vidrios de ventana se guardan ya unidos (las plantillas no recalculan las uniones)."""
    solid_kw = ('planks', 'log', 'wood', 'terracotta', 'stone', 'glass', 'wall', 'fence', 'pane', 'bars', 'casino:velvet', 'barrel')
    for (x, y, z), st in list(o.b.items()):
        base = st.split('[')[0]
        if not (base.endswith('_fence') or base.endswith('_pane') or base == 'minecraft:iron_bars'):
            continue
        props = {}
        for name, dx, dz in (('north', 0, -1), ('east', 1, 0), ('south', 0, 1), ('west', -1, 0)):
            n = o.b.get((x + dx, y, z + dz), '')
            nb = n.split('[')[0]
            ok = False
            if base.endswith('_fence'):
                ok = nb.endswith('_fence') or nb.endswith('_fence_gate') or (any(k in nb for k in ('planks', 'log', 'wood', 'casino:velvet'))
                                                                            and 'stairs' not in nb and 'slab' not in nb)
            else:
                ok = nb.endswith('_pane') or nb == 'minecraft:iron_bars' or (any(k in nb for k in solid_kw) and 'stairs' not in nb and 'slab' not in nb
                                                                            and not nb.endswith('_fence'))
            props[name] = 'true' if ok else 'false'
        if base.endswith('_pane') or base == 'minecraft:iron_bars' or base.endswith('_fence'):
            props['waterlogged'] = 'false'
        o.b[(x, y, z)] = base + '[' + ','.join(f'{k}={v}' for k, v in props.items()) + ']'


_connect()

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'el_saloon.nbt'
    pal, n = o.save(out)
    print('paleta', pal, 'bloques', n, '->', out)
    if len(sys.argv) > 2:
        o.dump(sys.argv[2]); print('volcado', sys.argv[2])
