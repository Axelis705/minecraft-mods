#!/usr/bin/env python3
"""THE DIAMOND: casino elegante (bosque oscuro, nieve y taiga). Torre art déco al estilo del Diamond Casino de GTA V.

Uso: the_diamond.py [salida.nbt] [volcado.txt]
Frente (entrada) al sur (z máxima). Pisos:
  y=0   planta del casino (vestíbulo, cajero, tienda, ruleta de la suerte, mesas, tragamonedas, bar y escenario, carreras)
  y=12  VIP (tarjeta Plata) + terraza con alberca sobre el techo del casino
  y=46  penthouse (tarjeta Oro)
  y=54  azotea con discoteca (tarjeta Oro)
Se sube con el elevador (marcadores "elev"); el guardia (marcador "guard") está junto al elevador de abajo.
"""
import sys, os, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from obra import Obra, OPP
import maquinas as mq

R = random.Random(7)
W, H, D = 61, 84, 72
o = Obra(W, H, D)
CX = 30  # eje central

# ------------------------------------------------------------------ paleta
WM = 'casino:marble'                 # mármol blanco
BM = 'casino:marble_black'           # mármol negro
WMS = 'casino:marble_slab'
BMS = 'casino:marble_black_slab'
GT = 'casino:gold_trim'              # moldura dorada
GB = 'minecraft:gold_block'
QP = 'minecraft:quartz_pillar'
SQ = 'minecraft:smooth_quartz'
QB = 'minecraft:quartz_bricks'
CQ = 'minecraft:chiseled_quartz_block'
BS = 'minecraft:polished_blackstone'
BSB = 'minecraft:polished_blackstone_bricks'
DT = 'minecraft:deepslate_tiles'
GLASS = 'minecraft:black_stained_glass'
GLASS_P = 'minecraft:black_stained_glass_pane'
TGLASS = 'minecraft:gray_stained_glass'
TINT = 'minecraft:tinted_glass'
LIGHT = 'minecraft:light[level=15]'
LIGHT_S = 'minecraft:light[level=12]'
SEA = 'minecraft:sea_lantern'
MAH = 'casino:mahogany_panel'
AIR = 'minecraft:air'

def stairs(b, f, half='bottom'): return f'{b}[facing={f},half={half}]'
def slab(b, t='bottom'): return f'{b}[type={t}]'
def neon(ch, f, color=4):
    chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?$&♥♠♦♣★- "
    return f'casino:neon_letter[facing={f},glyph={chars.index(ch)},color={color}]'
def chair(c, f): return f'casino:casino_chair_{c}[facing={f}]'
def stool(c, f): return f'casino:bar_stool_{c}[facing={f}]'
def sofa(c, f): return f'casino:sofa_{c}[facing={f}]'
def rope(c): return f'casino:rope_post_{c}'
def drinks(n, f='north'): return f'casino:drinks[facing={f},drinks={n}]'
def sign(d, f): return f'casino:neon_sign[design={d},facing={f}]'
def screen(d, f): return f'casino:wall_screen[design={d},facing={f}]'
def counter(f, shape='straight'): return f'casino:bar_counter[facing={f},shape={shape}]'
def shelf(f): return f'casino:bottle_shelf[facing={f}]'
def speaker(f): return f'casino:speaker[facing={f},lit=true]'
CHAND = 'casino:chandelier'
TABLE_W = 'casino:cocktail_table'
TABLE_B = 'casino:cocktail_table_black'

def light_grid(x0, x1, z0, z1, y, step=4, st=LIGHT):
    for x in range(x0, x1 + 1, step):
        for z in range(z0, z1 + 1, step):
            if o.get(x, y, z) == 'minecraft:air' or (x, y, z) not in o.b:
                o.set(x, y, z, st)

def floor_pattern(x0, x1, z0, z1, y, a, b, kind='diamond', cx=None, cz=None):
    cx = (x0 + x1) / 2 if cx is None else cx
    cz = (z0 + z1) / 2 if cz is None else cz
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if kind == 'diamond':
                d = abs(x - cx) + abs(z - cz)
                o.set(x, y, z, b if int(d) % 6 == 0 else a)
            elif kind == 'check':
                o.set(x, y, z, a if (x + z) % 2 == 0 else b)
            elif kind == 'border':
                edge = x in (x0, x1) or z in (z0, z1)
                o.set(x, y, z, b if edge else a)

# ================================================================== TERRENO / PLAZA
# todo el piso de la plantilla es sólido (césped alrededor); se rellena por debajo en el juego
o.fill(0, 0, 0, W - 1, 0, D - 1, 'minecraft:grass_block')
PX0, PX1, PZ0, PZ1 = 4, 56, 8, 51       # podio (casino)
TX0, TX1, TZ0, TZ1 = 17, 43, 10, 33     # torre

# calzada en U frente a la entrada con fuente en el centro
for x in range(2, W - 2):
    for z in range(PZ1 + 1, D - 1):
        o.set(x, 0, z, 'minecraft:smooth_stone')
# carriles de la calzada (asfalto oscuro) con borde
for x in range(4, W - 4):
    for z in range(PZ1 + 6, D - 3):
        inner = 12 <= x <= W - 13 and PZ1 + 9 <= z <= D - 7
        if not inner:
            o.set(x, 0, z, 'minecraft:gray_concrete' if (x + z) % 7 else 'minecraft:light_gray_concrete')
for x in range(4, W - 4):
    o.set(x, 0, PZ1 + 5, 'minecraft:polished_andesite'); o.set(x, 0, D - 3, 'minecraft:polished_andesite')
# isla central con la fuente
for x in range(12, W - 12):
    for z in range(PZ1 + 9, D - 6):
        o.set(x, 0, z, 'minecraft:grass_block')
FX, FZ = CX, PZ1 + 13
for x in range(FX - 7, FX + 8):
    for z in range(FZ - 4, FZ + 5):
        d = ((x - FX) / 7.5) ** 2 + ((z - FZ) / 4.5) ** 2
        if d <= 1.0:
            o.set(x, 0, z, WM)
        if 0.62 < d <= 1.0:
            o.set(x, 1, z, slab(WMS))
        elif d <= 0.62:
            o.set(x, 0, z, 'minecraft:water')
            o.set(x, -1 if False else 0, z, 'minecraft:water')
# fuente: taza central en dos niveles con chorros
for y in range(1, 4):
    o.set(FX, y, FZ, QP if y < 3 else CQ)
for dx in (-1, 0, 1):
    for dz in (-1, 0, 1):
        if dx or dz:
            o.set(FX + dx, 3, FZ + dz, slab(WMS, 'top'))
            o.set(FX + dx, 4, FZ + dz, 'minecraft:water')
o.set(FX, 4, FZ, GB); o.set(FX, 5, FZ, 'minecraft:water')
for dx, dz in ((-4, 0), (4, 0), (0, -2), (0, 2)):
    o.set(FX + dx, 1, FZ + dz, 'minecraft:water')
# setos y faroles en la isla
for x in range(13, W - 13):
    for z in (PZ1 + 10, D - 7):
        if abs(x - FX) > 8 and o.get(x, 0, z) == 'minecraft:grass_block':
            o.set(x, 1, z, 'minecraft:azalea_leaves[persistent=true]')
for x in (14, W - 15):
    for z in (PZ1 + 11, D - 8):
        o.set(x, 1, z, 'minecraft:polished_blackstone_wall'); o.set(x, 2, z, 'minecraft:polished_blackstone_wall')
        o.set(x, 3, z, GB); o.set(x, 4, z, 'minecraft:lantern')

# alfombra roja de la entrada (de la puerta a la calzada) con postes de cordón
for x in range(CX - 2, CX + 3):
    for z in range(PZ1, PZ1 + 6):
        o.set(x, 0, z, 'casino:casino_floor_royal')
for z in range(PZ1 + 1, PZ1 + 5):
    for x in (CX - 3, CX + 3):
        o.set(x, 1, z, rope('red'))
# acera de mármol frente al edificio
for x in range(PX0 - 1, PX1 + 2):
    for z in range(PZ1 + 1, PZ1 + 5):
        if not (CX - 2 <= x <= CX + 2):
            o.set(x, 0, z, BM if z in (PZ1 + 1, PZ1 + 4) else WM)
# jardineras y farolas a lo largo de la fachada
for x in list(range(PX0, CX - 6, 6)) + list(range(PX1, CX + 6, -6)):
    o.set(x, 1, PZ1 + 2, 'minecraft:polished_blackstone_wall'); o.set(x, 2, PZ1 + 2, 'minecraft:polished_blackstone_wall')
    o.set(x, 3, PZ1 + 2, 'minecraft:polished_blackstone_wall'); o.set(x, 4, PZ1 + 2, GB); o.set(x, 5, PZ1 + 2, 'minecraft:lantern')
    for dx in (-2, 2):
        o.set(x + dx, 1, PZ1 + 2, 'minecraft:spruce_leaves[persistent=true]')

# ================================================================== PODIO (casino)
Y_ROOF = 12
# paredes exteriores
for y in range(1, Y_ROOF):
    for x in range(PX0, PX1 + 1):
        o.set(x, y, PZ0, BSB); o.set(x, y, PZ1, BSB)
    for z in range(PZ0, PZ1 + 1):
        o.set(PX0, y, z, BSB); o.set(PX1, y, z, BSB)
# zócalo de mármol negro y cornisa
for x in range(PX0, PX1 + 1):
    o.set(x, 1, PZ1, BM); o.set(x, 1, PZ0, BM)
for z in range(PZ0, PZ1 + 1):
    o.set(PX0, 1, z, BM); o.set(PX1, 1, z, BM)
# techo
o.fill(PX0, Y_ROOF, PZ0, PX1, Y_ROOF, PZ1, SQ)

def facade_south():
    z = PZ1
    # pilastras de cuarzo cada 4 con capitel dorado; entre ellas ventanales oscuros sobre un zócalo de mármol negro
    for x in range(PX0, PX1 + 1):
        rel = (x - PX0) % 4
        for y in range(2, Y_ROOF):
            if rel == 0:
                o.set(x, y, z, QP if y < Y_ROOF - 2 else (GT if y == Y_ROOF - 2 else CQ))
            elif 3 <= y <= 8:
                o.set(x, y, z, TINT)
            elif y == 2:
                o.set(x, y, z, BM)
            else:
                o.set(x, y, z, SQ)
    # cornisa escalonada (art déco)
    for x in range(PX0 - 1, PX1 + 2):
        o.set(x, Y_ROOF, z + 1, stairs('minecraft:smooth_quartz_stairs', 'north', 'top'))
        o.set(x, Y_ROOF + 1, z, SQ); o.set(x, Y_ROOF + 2, z, slab('minecraft:smooth_quartz_slab'))
facade_south()

def facade_side(x, f):
    for z in range(PZ0, PZ1 + 1):
        rel = (z - PZ0) % 4
        for y in range(2, Y_ROOF):
            if rel == 0:
                o.set(x, y, z, QP if y < Y_ROOF - 2 else GT)
            elif 3 <= y <= 8 and PZ0 + 2 <= z <= PZ1 - 2:
                o.set(x, y, z, TINT)
            elif y == 2:
                o.set(x, y, z, BM)
            else:
                o.set(x, y, z, SQ)
    for z in range(PZ0, PZ1 + 1):
        o.set(x, Y_ROOF + 1, z, SQ)
facade_side(PX0, 'west'); facade_side(PX1, 'east')
for x in range(PX0, PX1 + 1):
    o.set(x, Y_ROOF + 1, PZ0, SQ)

# ---- entrada: portal de 7 de ancho y 7 de alto con marco dorado, marquesina y rombo de vidrio
for x in range(CX - 3, CX + 4):
    for y in range(1, 8):
        o.set(x, y, PZ1, AIR)
for y in range(1, 9):
    o.set(CX - 4, y, PZ1, GB if y % 2 else GT); o.set(CX + 4, y, PZ1, GB if y % 2 else GT)
for x in range(CX - 4, CX + 5):
    o.set(x, 8, PZ1, GB)
# puertas de vidrio a los lados (paneles) y arco
for x in (CX - 3, CX + 3):
    for y in range(1, 7):
        o.set(x, y, PZ1, TGLASS)
# marquesina en voladizo
for x in range(CX - 6, CX + 7):
    for z in range(PZ1 + 1, PZ1 + 6):
        edge = x in (CX - 6, CX + 6) or z == PZ1 + 5
        o.set(x, 8, z, GB if edge else BM)
        if not edge and (x + z) % 3 == 0:
            o.set(x, 7, z, LIGHT)
for x in (CX - 6, CX + 6):
    for y in range(1, 8):
        o.set(x, y, PZ1 + 5, GB if y in (1, 7) else 'minecraft:polished_blackstone_wall')
# rombo de THE DIAMOND sobre la marquesina (vidrio celeste con luz dentro)
DY = 14
for dy in range(-5, 6):
    w = 5 - abs(dy)
    for dx in range(-w, w + 1):
        x, y = CX + dx, DY + dy
        if y < 9: continue
        edge = abs(dx) == w
        o.set(x, y, PZ1, GB if edge else ('minecraft:light_blue_stained_glass' if (dx + dy) % 2 else 'minecraft:cyan_stained_glass'))
        if not edge: o.set(x, y, PZ1 - 1, SEA)
# letrero de neón THE DIAMOND sobre la cornisa, a los dos lados del rombo
word_l, word_r = 'THE', 'DIAMOND'
for i, ch in enumerate(word_l):
    o.set(CX - 6 - len(word_l) + 1 + i - 1, Y_ROOF + 1, PZ1 + 1, neon(ch, 'south', 4))
for i, ch in enumerate(word_r):
    o.set(CX + 6 + i, Y_ROOF + 1, PZ1 + 1, neon(ch, 'south', 4))
# fondo negro tras las letras (en la cornisa)
for x in list(range(CX - 10, CX - 5)) + list(range(CX + 6, CX + 14)):
    o.set(x, Y_ROOF + 1, PZ1, BM)

# ================================================================== INTERIOR DEL CASINO
IX0, IX1, IZ0, IZ1 = PX0 + 1, PX1 - 1, PZ0 + 1, PZ1 - 1   # 5..55, 9..50
o.fill(IX0, 1, IZ0, IX1, Y_ROOF - 1, IZ1, AIR)
# techo artesonado: retícula dorada y luces escondidas
for x in range(IX0, IX1 + 1):
    for z in range(IZ0, IZ1 + 1):
        grid = (x - CX) % 6 == 0 or (z - IZ0) % 6 == 0
        o.set(x, Y_ROOF - 1, z, GT if grid else 'minecraft:smooth_quartz')
        if not grid and (x + z) % 2 == 0:
            o.set(x, Y_ROOF - 1, z, 'minecraft:smooth_quartz')
for x in range(IX0 + 2, IX1, 6):
    for z in range(IZ0 + 3, IZ1, 6):
        o.set(x + 1, Y_ROOF - 1, z, 'minecraft:ochre_froglight')
light_grid(IX0 + 1, IX1 - 1, IZ0 + 1, IZ1 - 1, Y_ROOF - 2, 4)
light_grid(IX0 + 2, IX1 - 1, IZ0 + 2, IZ1 - 1, 3, 6, LIGHT_S)
# piso base: mármol blanco con rombos negros
floor_pattern(IX0, IX1, IZ0, IZ1, 0, WM, BM, 'diamond', CX, 30)
# paredes interiores: forro de caoba abajo, terciopelo morado y moldura dorada arriba (detrás de los ventanales)
def lining(y):
    return MAH if y <= 3 else ('casino:velvet_wall_purple' if y <= 8 else GT)
for y in range(1, Y_ROOF - 1):
    for x in range(IX0, IX1 + 1):
        o.set(x, y, IZ0, lining(y)); o.set(x, y, IZ1, lining(y))
    for z in range(IZ0, IZ1 + 1):
        o.set(IX0, y, z, lining(y)); o.set(IX1, y, z, lining(y))
# el forro no puede tapar la entrada (portal de 7 de ancho y 7 de alto)
for x in range(CX - 3, CX + 4):
    for y in range(1, 8):
        o.set(x, y, IZ1, AIR)
for y in range(1, 9):
    o.set(CX - 4, y, IZ1, GB if y % 2 else GT); o.set(CX + 4, y, IZ1, GB if y % 2 else GT)
for x in range(CX - 4, CX + 5):
    o.set(x, 8, IZ1, GB)
IX0, IX1, IZ0, IZ1 = IX0 + 1, IX1 - 1, IZ0 + 1, IZ1 - 1   # 6..54, 10..49 (espacio útil)
# columnas que dividen las zonas (cada 6 a lo largo de x=22 y x=38)
COLS = [(22, 24), (22, 42), (38, 24), (38, 42), (22, 33), (38, 33)]
for (x, z) in COLS:
    for y in range(1, Y_ROOF - 1):
        o.set(x, y, z, QP if 2 <= y <= 8 else (GB if y in (1, 9) else GT))

# ---------------- vestíbulo (z 44..50): mármol con rombo grande, cajero a la izquierda, tienda a la derecha
floor_pattern(IX0, IX1, 44, IZ1, 0, WM, BM, 'diamond', CX, 47)
for z in range(44, IZ1 + 1):
    for x in range(CX - 2, CX + 3):
        o.set(x, 0, z, 'casino:casino_floor_royal')
# cajero: mostrador de mármol negro con 3 cajeros y vitrina
for x in range(7, 19):
    o.set(x, 1, 45, counter('north'))   # (los cajeros se ponen encima de esta línea)
for x in (9, 12, 15):
    mq.place(o, 'cashier', x, 1, 45, 0)        # en la línea del mostrador, mirando al salón
for x in range(7, 19):
    o.set(x, 0, 43, MAH); o.set(x, 0, 44, MAH)
o.set(6, 1, 45, counter('north', 'outer_left')); o.set(19, 1, 45, counter('north', 'outer_right'))
for x in range(6, 20):
    o.set(x, 5, 42, 'casino:velvet_wall_red')
for i, ch in enumerate('CAJA'):
    o.set(11 + i, 6, 43, neon(ch, 'south', 2))
o.set(8, 2, 45, 'minecraft:light[level=10]')
# tienda de premios: mostrador + vitrinas de vidrio con oro y diamantes de adorno
for x in range(42, 54):
    o.set(x, 1, 45, counter('north'))
for x in (45, 48, 51):
    o.marker(x, 1, 45, 'shop 0', [(0, 0, 0, 'casino:prize_shop[facing=north]')])
for x in range(42, 54):
    o.set(x, 0, 43, MAH); o.set(x, 0, 44, MAH)
for i, ch in enumerate('PREMIOS'):
    o.set(44 + i, 6, 43, neon(ch, 'south', 1))
for x in range(41, 55):
    o.set(x, 5, 42, 'casino:velvet_wall_red')
for x in (42, 53):
    for z in (47, 49):
        o.set(x, 1, z, 'minecraft:glass'); o.set(x, 2, z, 'minecraft:glass')
o.set(42, 1, 47, GB); o.set(53, 1, 47, 'minecraft:diamond_block')
# sofás de espera en el vestíbulo
for z in (47, 48, 49):
    o.set(22, 1, z, sofa('red', 'east')); o.set(38, 1, z, sofa('red', 'west'))
o.set(22, 1, 46, TABLE_W); o.set(38, 1, 46, TABLE_W)
o.set(22, 2, 46, drinks(2)); o.set(38, 2, 46, drinks(3))
for (x, z) in ((CX - 4, 44), (CX + 4, 44)):
    o.set(x, 1, z, 'minecraft:potted_azalea_bush')

# ---------------- centro: ruleta de la suerte sobre una tarima redonda con candelabros
WX, WZ = CX, 27
for x in range(WX - 6, WX + 7):
    for z in range(WZ - 4, WZ + 9):
        d = ((x - WX) / 6.5) ** 2 + ((z - (WZ + 2)) / 6.5) ** 2
        if d <= 1.0:
            o.set(x, 0, z, BM if d > 0.75 else ('casino:casino_floor_deco'))
        if 0.85 < d <= 1.0 and z > WZ:
            o.set(x, 1, z, rope('purple'))
for x in range(WX - 2, WX + 3):
    for z in range(WZ - 1, WZ + 1):
        o.set(x, 1, z, slab(BMS))
mq.place(o, 'wheel', WX, 2, WZ, 2)
# (sin columnas a los lados: tapaban la rueda; solo dos faroles bajos detrás de la tarima)
for dx in (-4, 4):
    o.set(WX + dx, 1, WZ - 2, GB); o.set(WX + dx, 2, WZ - 2, 'minecraft:end_rod[facing=up]')
for x, z in ((WX - 5, 31), (WX + 5, 31), (WX, 36), (WX - 5, 38), (WX + 5, 38)):
    o.set(x, Y_ROOF - 2, z, CHAND)
for x, z in ((WX - 2, 34), (WX + 2, 34)):
    o.set(x, 1, z, TABLE_B); o.set(x, 2, z, drinks(1))

# ---------------- ala oeste: mesas (ruleta, 2 blackjack, póker) sobre alfombra real con cordones
for x in range(IX0, 22):
    for z in range(24, 43):
        o.set(x, 0, z, 'casino:casino_floor_royal')
for x in range(IX0, 22):
    o.set(x, 0, 23, GT); o.set(x, 0, 43, GT)
mq.place(o, 'roulette', 13, 1, 28, 2)
mq.place(o, 'blackjack', 9, 1, 36, 2)
mq.place(o, 'blackjack', 17, 1, 36, 2)
for x in (8, 18):
    o.set(x, Y_ROOF - 2, 30, CHAND)
o.set(13, Y_ROOF - 2, 38, CHAND)
for z in (25, 31, 41):
    o.set(21, 1, z, rope('red'))
for i, ch in enumerate('MESAS'):
    o.set(11 + i, 7, IZ0 + 13, neon(ch, 'south', 0)) if False else None

# ---------------- ala este: tragamonedas en hileras + plinko
for x in range(39, IX1 + 1):
    for z in range(24, 43):
        o.set(x, 0, z, 'casino:casino_floor_lounge')
for x in range(39, IX1 + 1):
    o.set(x, 0, 23, GT); o.set(x, 0, 43, GT)
for row_z in (27, 33):
    for x in range(41, 54, 2):
        mq.place(o, 'slot', x, 1, row_z, 0)        # miran al norte
        mq.place(o, 'slot', x, 1, row_z + 1, 2)    # espalda con espalda, miran al sur
    for x in range(41, 54):
        o.set(x, 3, row_z, 'minecraft:light[level=8]') if o.get(x, 3, row_z) == AIR else None
for x in range(42, 54, 2):
    o.set(x, 1, 27, GT); o.set(x, 1, 28, GT); o.set(x, 2, 27, 'minecraft:end_rod[facing=up]'); o.set(x, 2, 28, 'minecraft:end_rod[facing=up]')
    o.set(x, 1, 33, GT); o.set(x, 1, 34, GT); o.set(x, 2, 33, 'minecraft:end_rod[facing=up]'); o.set(x, 2, 34, 'minecraft:end_rod[facing=up]')
mq.place(o, 'plinko', 46, 1, 40, 2)
mq.place(o, 'plinko', 51, 1, 40, 2)
mq.place(o, 'scratch', 41, 1, 40, 2)
mq.place(o, 'scratch', 54, 1, 38, 3)
for x in (44, 50):
    o.set(x, Y_ROOF - 2, 30, CHAND)
o.set(IX1, 6, 30, sign(0, 'west')); o.set(IX1, 6, 36, sign(3, 'west'))
o.set(IX0, 6, 30, sign(5, 'east')); o.set(IX0, 6, 36, sign(7, 'east'))

# ---------------- fondo oeste: bar y escenario
for x in range(IX0, 23):
    for z in range(IZ0, 23):
        o.set(x, 0, z, MAH if (x + z) % 2 else 'minecraft:dark_oak_planks')
# escenario elevado contra la pared norte
for x in range(6, 17):
    for z in range(IZ0, IZ0 + 5):
        o.set(x, 1, z, 'minecraft:dark_oak_planks' if z < IZ0 + 4 else stairs('minecraft:dark_oak_stairs', 'north'))
for x in range(6, 17):
    for y in range(2, 9):
        o.set(x, y, IZ0 - 1, 'casino:velvet_wall_red')
for x in (6, 16):
    for y in range(2, 9):
        o.set(x, y, IZ0, 'minecraft:red_wool' if y < 8 else GT)
o.set(7, 2, IZ0, speaker('south')); o.set(15, 2, IZ0, speaker('south'))
o.set(7, 3, IZ0, speaker('south')); o.set(15, 3, IZ0, speaker('south'))
o.set(11, 6, IZ0, sign(1, 'south')); o.set(9, 6, IZ0, sign(2, 'south')); o.set(13, 6, IZ0, sign(4, 'south'))
for x in range(7, 16):
    o.set(x, Y_ROOF - 2, IZ0 + 4, 'minecraft:end_rod[facing=down]') if x % 2 else None
o.set(10, 2, IZ0 + 1, 'minecraft:note_block'); o.set(12, 2, IZ0 + 1, 'minecraft:jukebox')
# barra en U con taburetes
for x in range(9, 20):
    o.set(x, 1, 18, counter('south'))
for z in range(14, 18):
    o.set(20, 1, z, counter('east'))
o.set(20, 1, 18, counter('south', 'outer_left'))
for x in range(10, 20, 2):
    o.set(x, 1, 19, stool('black', 'north'))
for z in range(14, 18, 2):
    o.set(21, 1, z, stool('black', 'west'))
for x in range(9, 20):
    o.set(x, 2, 18, drinks(1 + (x % 3), 'south')) if x % 3 == 0 else None
# estante de botellas detrás de la barra
for x in range(9, 20):
    o.set(x, 1, 15, shelf('south')); o.set(x, 2, 15, shelf('south'))
for i, ch in enumerate('BAR'):
    o.set(13 + i, 4, 15, neon(ch, 'south', 1))
# mesas del bar
for (x, z) in ((7, 20), (13, 21), (18, 21)):
    o.set(x, 1, z, TABLE_B); o.set(x - 1, 1, z, chair('red', 'east')); o.set(x + 1, 1, z, chair('red', 'west'))
    o.set(x, 2, z, drinks(2))
o.set(12, Y_ROOF - 2, 19, CHAND)

# ---------------- centro fondo: vestíbulo del elevador
EX, EZ = CX, 12   # panel del elevador (en todos los pisos en la misma columna)
for x in range(23, 38):
    for z in range(IZ0, 23):
        o.set(x, 0, z, BM if (x + z) % 2 else WM)
for x in range(24, 37):
    o.set(x, 0, 22, GT)

def shaft(y0, y1, door_y):
    """Hueco del elevador: paredes de x 28..32, z 11..15; cabina 29..31 / 12..14; puerta al sur (z 15) en door_y..door_y+2."""
    for y in range(y0, y1 + 1):
        for x in range(28, 33):
            for z in range(11, 16):
                if x in (28, 32) or z in (11, 15):
                    o.set(x, y, z, GB if (x in (28, 32) and z == 15) else BM)
                else:
                    o.set(x, y, z, AIR)

def elevator_stop(fy, access, name, roof=False):
    """Parada del elevador con piso en fy (el jugador pisa fy y queda en fy+1)."""
    for x in range(29, 32):
        for z in range(12, 15):
            o.set(x, fy, z, 'casino:casino_floor_deco')
            o.set(x, fy + 4, z, GT)
        o.set(x, fy + 3, 13, LIGHT)
    for x in range(29, 32):
        for y in range(fy + 1, fy + 4):
            o.set(x, y, 15, AIR)
    o.set(28, fy + 1, 15, GB); o.set(32, fy + 1, 15, GB)
    for x in range(28, 33):
        o.set(x, fy + 4, 15, GB)
    o.marker(EX, fy + 2, EZ, f'elev 2 {access} {name}', [(0, 0, 0, 'minecraft:gold_block')])

# ================================================================== TORRE
Y_VIP, Y_VIP_TOP = 12, 22
Y_PH, Y_PH_TOP = 46, 54
SX0, SX1, SZ0, SZ1 = 19, 41, 12, 31    # penthouse/azotea (retranqueo)

shaft(1, Y_PH_TOP + 3, 1)
elevator_stop(0, 0, 'casino')

# guardia junto al elevador
o.marker(26, 1, 17, 'guard 2', [(0, 0, 0, 'minecraft:black_wool'), (0, 1, 0, 'minecraft:black_wool')])
o.set(26, 1, 16, rope('black')); o.set(34, 1, 16, rope('black'))
for x in range(27, 34):
    o.set(x, 0, 16, 'casino:casino_floor_royal'); o.set(x, 0, 17, 'casino:casino_floor_royal')
for i, ch in enumerate('VIP'):
    o.set(29 + i, 9, 16, neon(ch, 'south', 2))
o.set(24, 6, IZ0, sign(9, 'south')); o.set(36, 6, IZ0, sign(10, 'south'))
for x in (24, 36):
    for y in range(1, 9):
        o.set(x, y, 21, QP if y < 8 else GB)

# ---------------- fondo este: Inside Track (carreras) con pantalla gigante
for x in range(38, IX1 + 1):
    for z in range(IZ0, 23):
        o.set(x, 0, z, 'casino:casino_floor_galaxy')
# pantalla gigante en la pared este (9 x 5) con marco dorado
SCX0, SCZ0 = 41, IZ0 + 1
for x in range(40, 54):
    for y in range(2, 10):
        o.set(x, y, IZ0 - 1, GB if x in (40, 53) or y in (2, 9) else 'minecraft:black_concrete')
o.marker(46, 3, IZ0, 'tv 2 12 6', [(dx, dy, 0, screen((dx + dy) % 4, 'south')) for dx in range(-5, 7) for dy in range(0, 6)])
for z in (14, 16, 18, 20):
    for x in range(41, 53):
        if x in (46, 47): continue
        o.set(x, 1 if z < 18 else 1, z, chair('blue' if z % 4 else 'black', 'north'))
for x in range(40, 54):
    o.set(x, 0, 18, 'minecraft:dark_oak_planks'); o.set(x, 0, 20, 'minecraft:dark_oak_planks')
for i, ch in enumerate('CARRERAS'):
    o.set(42 + i, 10, IZ0, neon(ch, 'south', 2))
o.set(IX1, 5, 15, sign(6, 'west')); o.set(IX1, 5, 20, sign(8, 'west'))

# ================================================================== PISO VIP (y 12..22) y terraza con alberca
for y in range(Y_VIP + 1, Y_VIP_TOP):
    for x in range(TX0, TX1 + 1):
        o.set(x, y, TZ0, BSB); o.set(x, y, TZ1, BSB)
    for z in range(TZ0, TZ1 + 1):
        o.set(TX0, y, z, BSB); o.set(TX1, y, z, BSB)
o.fill(TX0 + 1, Y_VIP, TZ0 + 1, TX1 - 1, Y_VIP, TZ1 - 1, BM)
floor_pattern(TX0 + 1, TX1 - 1, TZ0 + 1, TZ1 - 1, Y_VIP, BM, GT, 'border')
for x in range(TX0 + 2, TX1 - 1):
    for z in range(TZ0 + 2, TZ1 - 1):
        o.set(x, Y_VIP, z, BM if (abs(x - CX) + abs(z - 22)) % 5 else 'casino:marble')
o.fill(TX0, Y_VIP_TOP, TZ0, TX1, Y_VIP_TOP, TZ1, SQ)
for x in range(TX0 + 1, TX1):
    for z in range(TZ0 + 1, TZ1):
        if (x - CX) % 4 == 0 or (z - TZ0) % 4 == 0:
            o.set(x, Y_VIP_TOP - 1, z, GT)
light_grid(TX0 + 2, TX1 - 2, TZ0 + 2, TZ1 - 2, Y_VIP_TOP - 2, 4)
# paredes VIP: terciopelo negro con pilastras doradas; ventanales al sur hacia la terraza
for y in range(Y_VIP + 1, Y_VIP_TOP):
    for x in range(TX0, TX1 + 1):
        if (x - CX) % 6 == 0 or x in (TX0, TX1):
            o.set(x, y, TZ0, QP); o.set(x, y, TZ1, QP if y < Y_VIP_TOP - 1 else GT)
        else:
            o.set(x, y, TZ0, 'casino:velvet_wall_black' if y < Y_VIP_TOP - 2 else GT)
            o.set(x, y, TZ1, GLASS if 2 <= y - Y_VIP <= 7 else SQ)
    for z in range(TZ0, TZ1 + 1):
        for x in (TX0, TX1):
            o.set(x, y, z, QP if (z - TZ0) % 6 == 0 else (GLASS if 2 <= y - Y_VIP <= 7 and TZ0 + 2 < z < TZ1 - 2 else SQ))
# puertas a la terraza (aberturas en el ventanal sur)
for x in range(CX - 2, CX + 3):
    for y in range(Y_VIP + 1, Y_VIP + 4):
        o.set(x, y, TZ1, AIR)
shaft(Y_VIP + 1, Y_VIP_TOP - 1, Y_VIP)
elevator_stop(Y_VIP, 1, 'vip')
# mesas de alto riesgo
mq.place(o, 'poker', CX, Y_VIP + 1, 22, 2)
mq.place(o, 'roulette', 23, Y_VIP + 1, 25, 1)
mq.place(o, 'blackjack', 38, Y_VIP + 1, 24, 3)
for x, z in ((CX, 21), (23, 25), (37, 24), (CX, 28)):
    o.set(x, Y_VIP_TOP - 2, z, CHAND)
# lounge VIP con sofás morados y bar privado
for x in range(TX0 + 2, TX0 + 7):
    o.set(x, Y_VIP + 1, TZ0 + 2, sofa('purple', 'south'))
o.set(TX0 + 4, Y_VIP + 1, TZ0 + 4, TABLE_B); o.set(TX0 + 4, Y_VIP + 2, TZ0 + 4, drinks(3))
for z in range(TZ0 + 2, TZ0 + 7):
    o.set(TX1 - 2, Y_VIP + 1, z, counter('west'))
    o.set(TX1 - 1, Y_VIP + 1, z, shelf('west')); o.set(TX1 - 1, Y_VIP + 2, z, shelf('west'))
    o.set(TX1 - 3, Y_VIP + 1, z, stool('purple', 'east')) if z % 2 == 0 else None
for i, ch in enumerate('VIP'):
    o.set(CX - 1 + i, Y_VIP + 7, TZ0 + 1, neon(ch, 'south', 2))
o.set(TX0 + 1, Y_VIP + 4, 20, sign(11, 'east')); o.set(TX1 - 1, Y_VIP + 4, 20, sign(9, 'west'))
o.set(TX0 + 2, Y_VIP + 1, TZ1 - 2, speaker('north'))

# terraza sobre el techo del casino: alberca, camastros, setos y barandal de vidrio
TER_Z0 = TZ1 + 1
for x in range(PX0 + 1, PX1):
    for z in range(TER_Z0, PZ1):
        o.set(x, Y_ROOF, z, WM if (x + z) % 2 else 'minecraft:smooth_quartz')
for x in range(PX0, PX1 + 1):
    o.set(x, Y_ROOF + 1, PZ1 - 1, 'minecraft:glass_pane') if o.get(x, Y_ROOF + 1, PZ1) != AIR else None
for z in range(PZ0, PZ1):
    o.set(PX0 + 1, Y_ROOF + 1, z, 'minecraft:glass_pane'); o.set(PX1 - 1, Y_ROOF + 1, z, 'minecraft:glass_pane')
for x in range(PX0 + 1, PX1):
    o.set(x, Y_ROOF + 1, PZ0 + 1, 'minecraft:glass_pane')
# alberca
for x in range(CX - 9, CX + 10):
    for z in range(TER_Z0 + 5, TER_Z0 + 13):
        edge = x in (CX - 9, CX + 9) or z in (TER_Z0 + 5, TER_Z0 + 12)
        o.set(x, Y_ROOF, z, WM if edge else 'minecraft:water')
        if not edge:
            o.set(x, Y_ROOF - 1, z, 'minecraft:light_blue_concrete')
            if (x + z) % 5 == 0: o.set(x, Y_ROOF - 1, z, SEA)
# debajo de la alberca: techo doble (sin luces ni candelabros) para que no se vean gotas dentro del casino
for x in range(CX - 10, CX + 11):
    for z in range(TER_Z0 + 4, TER_Z0 + 14):
        o.set(x, Y_ROOF - 2, z, 'minecraft:smooth_quartz')
        if (x + z) % 4 == 0:
            o.set(x, Y_ROOF - 3, z, LIGHT)
for x in range(CX - 9, CX + 10, 3):
    o.set(x, Y_ROOF + 1, TER_Z0 + 14, sofa('blue', 'north'))
for x in (CX - 13, CX + 13):
    for z in range(TER_Z0 + 2, TER_Z0 + 15, 3):
        o.set(x, Y_ROOF + 1, z, 'minecraft:spruce_leaves[persistent=true]')
        o.set(x, Y_ROOF + 2, z, 'minecraft:spruce_leaves[persistent=true]')
for x in (CX - 11, CX + 11):
    o.set(x, Y_ROOF + 1, TER_Z0 + 3, TABLE_W); o.set(x, Y_ROOF + 2, TER_Z0 + 3, drinks(2))
    for y in range(Y_ROOF + 2, Y_ROOF + 4): pass
for x, z in ((PX0 + 3, TER_Z0 + 2), (PX1 - 3, TER_Z0 + 2), (PX0 + 3, PZ1 - 3), (PX1 - 3, PZ1 - 3)):
    o.set(x, Y_ROOF + 1, z, 'minecraft:polished_blackstone_wall'); o.set(x, Y_ROOF + 2, z, 'minecraft:polished_blackstone_wall')
    o.set(x, Y_ROOF + 3, z, 'minecraft:lantern')
# alas laterales del techo (fuera de la torre): jardín
for x in list(range(PX0 + 2, TX0 - 1)) + list(range(TX1 + 2, PX1 - 1)):
    for z in range(PZ0 + 2, TER_Z0):
        o.set(x, Y_ROOF, z, 'minecraft:moss_block' if (x + z) % 4 else 'minecraft:grass_block')
        if (x * 7 + z * 3) % 11 == 0: o.set(x, Y_ROOF + 1, z, 'minecraft:azalea_leaves[persistent=true]')

# ================================================================== CUERPO DE LA TORRE (habitaciones, y 22..46)
def tower_body(y0, y1, x0, x1, z0, z1):
    for y in range(y0, y1 + 1):
        lvl = (y - y0) % 4
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge_x = x in (x0, x1)
                edge_z = z in (z0, z1)
                if not (edge_x or edge_z):
                    if lvl == 0:
                        o.set(x, y, z, SQ)
                    elif lvl == 2 and (x + z) % 5 == 0 and R.random() < 0.6:
                        o.set(x, y, z, LIGHT)
                    else:
                        o.set(x, y, z, AIR)
                    continue
                along = x if edge_z else z
                mid = (x0 + x1) // 2 if edge_z else (z0 + z1) // 2
                corner = edge_x and edge_z
                if corner:
                    o.set(x, y, z, QB)
                elif abs(along - mid) == 1 and edge_z:
                    o.set(x, y, z, GT)          # aletas doradas del eje central
                elif (along - (x0 if edge_z else z0)) % 3 == 0:
                    o.set(x, y, z, QP)          # pilastras blancas continuas
                elif lvl == 0:
                    o.set(x, y, z, SQ)          # antepecho blanco
                else:
                    o.set(x, y, z, GLASS)       # ventana oscura
    for y in range(y0 + 1, y1 + 1, 4):
        for x in range(x0 + 1, x1):
            for z in range(z0 + 1, z1):
                if (x in (x0 + 1, x1 - 1) or z in (z0 + 1, z1 - 1)) and R.random() < 0.25:
                    o.set(x, y + 1, z, 'minecraft:ochre_froglight' if R.random() < 0.5 else SEA)
tower_body(Y_VIP_TOP, Y_PH - 1, TX0, TX1, TZ0, TZ1)
shaft(Y_VIP_TOP + 1, Y_PH - 1, 0)
# retranqueos art déco en las esquinas a partir de y 38
for y in range(38, Y_PH):
    for (x, z) in ((TX0, TZ0), (TX1, TZ0), (TX0, TZ1), (TX1, TZ1)):
        o.set(x, y, z, AIR)
    for (x, z) in ((TX0 + 1, TZ0 + 1), (TX1 - 1, TZ0 + 1), (TX0 + 1, TZ1 - 1), (TX1 - 1, TZ1 - 1)):
        o.set(x, y, z, GB)
# franjas doradas horizontales cada 8 pisos y aletas doradas en el centro de la fachada
for y in (Y_VIP_TOP, 38):
    for x in range(TX0, TX1 + 1):
        o.set(x, y, TZ1, CQ); o.set(x, y, TZ0, CQ)
    for z in range(TZ0, TZ1 + 1):
        o.set(TX0, y, z, CQ); o.set(TX1, y, z, CQ)
# letrero vertical de neón en la fachada sur: THE DIAMOND
for i, ch in enumerate('THEDIAMOND'):
    y = 43 - i - (1 if i >= 3 else 0)
    o.set(CX, y, TZ1 + 1, neon(ch, 'south', 4))
for y in range(Y_VIP_TOP + 2, 45):
    o.set(CX - 1, y, TZ1 + 1, GT); o.set(CX + 1, y, TZ1 + 1, GT)
    o.set(CX, y, TZ1, BM)

# ================================================================== PENTHOUSE (y 46..54)
for x in range(TX0, TX1 + 1):
    for z in range(TZ0, TZ1 + 1):
        o.set(x, Y_PH, z, SQ)   # cornisa del retranqueo
for x in range(TX0, TX1 + 1):
    o.set(x, Y_PH, TZ1 + 1, stairs('casino:marble_black_stairs', 'north', 'top')) if False else None
for y in range(Y_PH + 1, Y_PH_TOP):
    for x in range(SX0, SX1 + 1):
        for z in range(SZ0, SZ1 + 1):
            edge = x in (SX0, SX1) or z in (SZ0, SZ1)
            if edge:
                corner = x in (SX0, SX1) and z in (SZ0, SZ1)
                along = x if z in (SZ0, SZ1) else z
                o.set(x, y, z, QB if corner else (QP if along % 4 == 0 else (GLASS if y < Y_PH_TOP - 1 else CQ)))
            else:
                o.set(x, y, z, AIR)
o.fill(SX0 + 1, Y_PH, SZ0 + 1, SX1 - 1, Y_PH, SZ1 - 1, WM)
o.fill(SX0, Y_PH_TOP, SZ0, SX1, Y_PH_TOP, SZ1, SQ)
light_grid(SX0 + 2, SX1 - 2, SZ0 + 2, SZ1 - 2, Y_PH_TOP - 1, 4)
shaft(Y_PH + 1, Y_PH_TOP - 1, Y_PH)
elevator_stop(Y_PH, 2, 'penthouse')
P = Y_PH + 1
PI0, PI1, PJ0, PJ1 = SX0 + 1, SX1 - 1, SZ0 + 1, SZ1 - 1     # interior 20..40 x 13..30
# vestíbulo frente al elevador: alfombra y dos macetas
for x in range(27, 34):
    for z in range(16, 19):
        o.set(x, Y_PH, z, 'casino:casino_floor_deco')
o.set(27, P, 16, 'minecraft:potted_azalea_bush'); o.set(33, P, 16, 'minecraft:potted_azalea_bush')
# --- recámara (noroeste) con pared de caoba y puerta
for z in range(PJ0, 20):
    for y in range(P, Y_PH_TOP):
        o.set(27, y, z, MAH if y < P + 3 else 'casino:velvet_wall_red')
for y in range(P, P + 3):
    o.set(27, y, 18, AIR); o.set(27, y, 17, AIR)
for x in range(PI0, 27):
    for z in range(PJ0, 20):
        o.set(x, Y_PH, z, 'casino:casino_carpet_royal' if False else 'casino:casino_floor_royal')
# cama doble con cabecera de terciopelo y burós
for x in (22, 23):
    o.set(x, P, PJ0 + 1, f'minecraft:red_bed[facing=south,part=head]')
    o.set(x, P, PJ0 + 2, f'minecraft:red_bed[facing=south,part=foot]')
for x in range(21, 25):
    o.set(x, P, PJ0, 'casino:velvet_wall_red'); o.set(x, P + 1, PJ0, 'casino:velvet_wall_red'); o.set(x, P + 2, PJ0, GT)
for x in (21, 24):
    o.set(x, P, PJ0 + 1, 'minecraft:dark_oak_trapdoor[facing=south,half=top,open=false]')
    o.set(x, P + 1, PJ0 + 1, 'minecraft:lantern')
# vestidor y espejo
for z in range(PJ0 + 3, 19):
    o.set(PI0, P, z, 'minecraft:dark_oak_planks'); o.set(PI0, P + 1, z, 'minecraft:dark_oak_planks')
    o.set(PI0, P + 2, z, 'minecraft:dark_oak_slab[type=bottom]')
o.set(25, P, 18, 'minecraft:armor_stand') if False else None
o.set(23, Y_PH_TOP - 1, 16, CHAND)
# --- sala (suroeste): sofás en U frente a una pantalla grande en la pared oeste
for x in range(PI0, 28):
    for z in range(20, PJ1 + 1):
        o.set(x, Y_PH, z, WM)
for x in range(21, 27):
    for z in range(22, 29):
        o.set(x, Y_PH, z, 'casino:casino_floor_lounge')
for z in range(22, 28):
    for y in range(P + 1, P + 4):
        o.set(PI0 - 1, y, z, BM)
for z in range(23, 27):
    for y in range(P + 1, P + 3):
        o.set(PI0, y, z, screen((z + y) % 4, 'east'))
for x in range(23, 27):
    o.set(x, P, 28, sofa('black', 'north'))
for z in range(23, 28):
    o.set(27, P, z, sofa('black', 'west'))
o.set(24, P, 25, TABLE_B); o.set(24, P + 1, 25, drinks(4))
o.set(26, P, 22, 'minecraft:potted_bamboo')
o.set(24, Y_PH_TOP - 1, 25, CHAND)
o.set(PI0, P, 29, speaker('east')); o.set(PI0, P, 21, speaker('east'))
# --- comedor (centro sur) y piano
for x in range(30, 35):
    o.set(x, P, 24, 'minecraft:dark_oak_slab[type=top]') if False else None
o.set(31, P, 25, TABLE_W); o.set(33, P, 25, TABLE_W)
for x in (31, 33):
    o.set(x, P, 24, chair('red', 'south')); o.set(x, P, 26, chair('red', 'north'))
    o.set(x, P + 1, 25, drinks(2))
o.set(32, Y_PH_TOP - 1, 25, CHAND)
# piano de cola (bloques negros con teclas)
for x in range(29, 32):
    o.set(x, P, 29, 'minecraft:black_concrete')
o.set(29, P + 1, 29, 'minecraft:black_carpet'); o.set(30, P + 1, 29, 'minecraft:white_carpet'); o.set(31, P + 1, 29, 'minecraft:black_carpet')
o.set(30, P, 30, 'minecraft:note_block')
o.set(30, P, 28, 'minecraft:dark_oak_stairs[facing=north]')
# --- bar privado (este)
for z in range(22, 29):
    o.set(37, P, z, counter('west'))
    o.set(PI1, P, z, shelf('west')); o.set(PI1, P + 1, z, shelf('west'))
for z in range(22, 29, 2):
    o.set(36, P, z, stool('red', 'east'))
o.set(37, P + 1, 23, drinks(3, 'west')); o.set(37, P + 1, 26, drinks(1, 'west'))
for i, ch in enumerate('BAR'):
    o.set(PI1, P + 3, 24 + i, neon(ch, 'west', 1))
# --- jacuzzi (noreste) con mampara de vidrio
for x in range(34, PI1 + 1):
    for z in range(PJ0, 20):
        o.set(x, Y_PH, z, 'minecraft:smooth_quartz')
for x in range(35, 40):
    for z in range(PJ0 + 1, 19):
        edge = x in (35, 39) or z in (PJ0 + 1, 18)
        o.set(x, P, z, slab(WMS, 'top') if edge else 'minecraft:water')
        if not edge:
            o.set(x, Y_PH, z, SEA)
for z in range(PJ0, 20):
    o.set(34, P + 1, z, 'minecraft:glass_pane') if z != 19 else None
o.set(36, P + 1, 19, 'minecraft:potted_fern'); o.set(38, P + 1, 19, 'minecraft:potted_fern')
o.set(37, Y_PH_TOP - 1, 16, 'minecraft:sea_lantern')
# camastros frente al ventanal sur
for x in (21, 39):
    o.set(x, P, PJ1, chair('purple', 'south'))
o.set(PI0 + 1, P + 3, 21, sign(3, 'east')) if False else None

# ================================================================== AZOTEA: discoteca (y 54..)
A = Y_PH_TOP + 1
for x in range(SX0, SX1 + 1):
    for z in range(SZ0, SZ1 + 1):
        if x in (SX0, SX1) or z in (SZ0, SZ1):
            o.set(x, A, z, 'minecraft:glass_pane')
# piso de la azotea: mármol negro con rombos
for x in range(SX0 + 1, SX1):
    for z in range(SZ0 + 1, SZ1):
        o.set(x, Y_PH_TOP, z, BM if (x + z) % 2 else 'casino:marble_black')
# pista de baile iluminada (cuadros de colores con luz debajo)
COLS_DANCE = ['minecraft:magenta_stained_glass', 'minecraft:cyan_stained_glass', 'minecraft:yellow_stained_glass', 'minecraft:lime_stained_glass']
for x in range(CX - 5, CX + 6):
    for z in range(21, 29):
        o.set(x, Y_PH_TOP, z, COLS_DANCE[(x // 2 + z // 2) % 4])
        o.set(x, Y_PH_TOP - 1, z, SEA)
for x in range(CX - 6, CX + 7):
    for z in (20, 29):
        o.set(x, Y_PH_TOP, z, GT)
# cabina del DJ al fondo (sur), mirando a la pista; la salida del elevador queda libre
for x in range(CX - 4, CX + 5):
    for y in range(A, A + 4):
        o.set(x, y, SZ1, BM if y < A + 3 else GT)
for x in range(CX - 3, CX + 4):
    o.set(x, A, SZ1 - 2, counter('north'))
for x in (CX - 5, CX + 5):
    o.set(x, A, SZ1 - 1, speaker('north')); o.set(x, A + 1, SZ1 - 1, speaker('north'))
o.set(CX - 1, A + 1, SZ1 - 2, 'minecraft:jukebox'); o.set(CX + 1, A + 1, SZ1 - 2, 'minecraft:note_block')
for i, ch in enumerate('DISCO'):
    o.set(CX - 2 + i, A + 2, SZ1 - 1, neon(ch, 'north', 1))
# bar y sillones de la azotea
for z in range(21, 29):
    o.set(SX0 + 2, A, z, counter('east'))
    o.set(SX0 + 1, A, z, shelf('east'))
for z in range(21, 29, 2):
    o.set(SX0 + 3, A, z, stool('purple', 'west'))
for z in range(21, 29):
    o.set(SX1 - 1, A, z, sofa('purple', 'west'))
o.set(SX1 - 3, A, 23, TABLE_B); o.set(SX1 - 3, A + 1, 23, drinks(3))
o.set(SX1 - 3, A, 27, TABLE_B); o.set(SX1 - 3, A + 1, 27, drinks(2))
# luces de fiesta: postes con barras de luz y faroles
for x in [v for v in range(SX0 + 2, SX1 - 1, 4) if abs(v - CX) > 6]:
    o.set(x, A, SZ1 - 1, 'minecraft:end_rod[facing=up]'); o.set(x, A + 1, SZ1 - 1, 'minecraft:end_rod[facing=up]')
for (x, z) in ((SX0 + 1, SZ1 - 1), (SX1 - 1, SZ1 - 1)):
    o.set(x, A, z, 'minecraft:polished_blackstone_wall'); o.set(x, A + 1, z, 'minecraft:polished_blackstone_wall'); o.set(x, A + 2, z, 'minecraft:sea_lantern')

# corona art déco: tres pisos escalonados (el primero encierra el elevador) y un diamante gigante arriba
TIERS = ((24, 36, SZ0, 18, 6), (26, 34, SZ0, 17, 5), (28, 32, SZ0 + 1, 16, 5))
yb = A
for i, (x0, x1, z0, z1, h) in enumerate(TIERS):
    for y in range(yb, yb + h):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    corner = x in (x0, x1) and z in (z0, z1)
                    o.set(x, y, z, GB if corner else (GT if y == yb + h - 1 else (QP if (x + z) % 2 == 0 else GLASS)))
                elif i > 0 or not (28 <= x <= 32 and 11 <= z <= 15):
                    o.set(x, y, z, LIGHT if y < yb + h - 1 else SQ)
    # techo de cada piso
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            o.set(x, yb + h - 1, z, o.get(x, yb + h - 1, z) if x in (x0, x1) or z in (z0, z1) else SQ)
    yb += h
# puerta de la corona hacia la pista (frente del elevador)
for x in range(29, 32):
    for y in range(A, A + 3):
        o.set(x, y, 18, AIR)
        o.set(x, y, 16, AIR); o.set(x, y, 17, AIR)
o.set(28, A + 3, 18, GB); o.set(32, A + 3, 18, GB)
# cabina del elevador dentro de la corona
for x in range(28, 33):
    for z in range(11, 16):
        for y in range(A, A + 4):
            if x in (28, 32) or z == 11:
                o.set(x, y, z, BM)
            elif z == 15 and not (29 <= x <= 31 and y < A + 3):
                o.set(x, y, z, BM)
elevator_stop(Y_PH_TOP, 2, 'azotea')
GY = yb
for dy in range(0, 10):
    w = 4 - abs(dy - 4) if dy < 9 else 0
    for dx in range(-w, w + 1):
        for dz in range(-w, w + 1):
            if abs(dx) + abs(dz) <= w:
                edge = abs(dx) + abs(dz) == w
                o.set(CX + dx, GY + dy, 14 + dz, 'minecraft:light_blue_stained_glass' if edge else SEA)
o.set(CX, GY + 9, 14, 'minecraft:end_rod[facing=up]')

# ================================================================== patio trasero y accesos de servicio
for x in range(PX0, PX1 + 1):
    for z in range(0, PZ0):
        o.set(x, 0, z, 'minecraft:smooth_stone' if 2 <= z <= 5 and PX0 + 4 <= x <= PX1 - 4 else 'minecraft:grass_block')

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'the_diamond.nbt'
    pal, n = o.save(out)
    print('paleta', pal, 'bloques', n, '->', out)
    if len(sys.argv) > 2:
        o.dump(sys.argv[2]); print('volcado', sys.argv[2])
