#!/usr/bin/env python3
"""THE DIAMOND: casino elegante (bosque oscuro, nieve y taiga). Torre art déco al estilo del Diamond Casino de GTA V.

Uso: the_diamond.py [salida.nbt] [volcado.txt]
Frente (entrada) al sur (z máxima). Pisos (y=0 es la planta baja; la plantilla lleva B bloques de sótano debajo):
  y=-12 sótano VIP (tarjeta Plata): carreras de caballos con pantalla gigante y salón privado con bar
  y=0   planta baja en salas: vestíbulo (caja y premios), ruleta de la suerte al centro, bar-isla (oeste), tragamonedas (este),
        escenario (fondo oeste), plinko y rasca (fondo este), elevador con guardia (fondo centro)
  y=12  salón de mesas (blackjack, ruleta, póker) + terraza con alberca sobre el techo del casino
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
B = 12   # sótano (TemplateCasinoStructure.sink("the_diamond") debe valer lo mismo)
o = Obra(W, H + B, D, oy=B)
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
            if o.get(x, y, z) == 'minecraft:air' or (x, y + o.oy, z) not in o.b:
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
            o.set(x, 0, z, 'minecraft:gray_concrete' if (abs(x - CX) + z) % 7 else 'minecraft:light_gray_concrete')
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
for x in range(IX0, IX1 + 1):
    for z in range(IZ0 + 3, IZ1, 6):
        if (x - CX) % 6 == 3:
            o.set(x, Y_ROOF - 1, z, 'minecraft:ochre_froglight')
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

# ---------------- vestíbulo: al entrar, un recibidor despejado (x 23..37) y a los lados dos salas con la puerta frente a frente:
#                  CAJA al oeste (x 6..21) y PREMIOS al este (x 39..54), una espejo de la otra (eje x=30). Sin muebles de por medio.
LZ0, LZ1 = 42, IZ1                       # z 42..49 (8 de fondo); puertas en z 44..47
floor_pattern(23, 37, LZ0, LZ1, 0, WM, BM, 'diamond', CX, 45)
for z in range(LZ0, LZ1 + 1):
    for x in range(CX - 2, CX + 3):
        o.set(x, 0, z, 'casino:casino_floor_royal')
def side_room(east, word, color, kind):
    X = (lambda x: 2 * CX - x) if east else (lambda x: x)
    face = 'west' if east else 'east'          # hacia la puerta (el lado del jugador)
    back = 'east' if east else 'west'
    d = 3 if east else 1
    for x in range(6, 22):
        for z in range(LZ0, LZ1 + 1):
            if x <= 8:
                st = 'minecraft:dark_oak_planks'                       # área del personal
            elif x == 9:
                st = GB
            else:
                st = GB if (z in (LZ0, LZ1) or x == 10) else 'casino:casino_floor_royal'
            o.set(X(x), 0, z, st)
    # mostrador de lado a lado, con las ventanillas al centro (frente a la puerta) y a los lados
    for z in range(LZ0, LZ1 + 1):
        o.set(X(9), 1, z, counter(face))
    for z in (43, 45, 46, 48):
        if kind == 'caja':
            mq.place(o, 'cashier', X(9), 1, z, d)
        else:
            o.marker(X(9), 1, z, f'shop {d}', [(0, 0, 0, f'casino:prize_shop[facing={face}]')])
    # marquesina dorada sobre el mostrador con faroles y letrero de neón
    for z in range(LZ0, LZ1 + 1):
        o.set(X(9), 5, z, GB); o.set(X(9), 6, z, 'minecraft:black_concrete'); o.set(X(9), 7, z, 'minecraft:black_concrete'); o.set(X(9), 8, z, GB)
        o.set(X(10), 5, z, stairs('minecraft:smooth_quartz_stairs', back, 'top'))
        if z in (42, 44, 47, 49):
            o.set(X(10), 4, z, 'minecraft:lantern[hanging=true]')
    for i, ch in enumerate(word):
        z = (LZ0 + i) if east else (48 - i)      # mirando al oeste se lee con z ascendente; mirando al este, descendente
        o.set(X(10), 7, z, neon(ch, face, color)) if ch != ' ' else None
    # fondo: vitrinas iluminadas contra la pared
    for z in range(LZ0, LZ1 + 1):
        dz = abs(2 * z - (LZ0 + LZ1))
        o.set(X(6), 1, z, GB)
        if kind == 'caja':
            o.set(X(6), 2, z, 'minecraft:iron_block' if dz % 4 == 1 else GB)
        else:
            o.set(X(6), 2, z, ('minecraft:diamond_block', 'minecraft:emerald_block', GB)[(dz // 2) % 3])
        o.set(X(6), 3, z, 'minecraft:glass')
        o.set(X(7), 1, z, 'minecraft:light[level=12]')
    # macetas en las esquinas junto a la puerta (fuera del paso)
    for z in (LZ0, LZ1):
        o.set(X(21), 1, z, 'minecraft:potted_azalea_bush')
side_room(False, '♦CAJA♦', 2, 'caja')
side_room(True, 'PREMIOS!', 1, 'premios')
# letreros sobre las puertas, mirando al recibidor (el de la caja lee z descendente; el de premios, ascendente)
for i, ch in enumerate('♦CAJA♦'):
    o.set(22 + 1, 7, 48 - i, neon(ch, 'east', 2))
for i, ch in enumerate('PREMIOS!'):
    o.set(38 - 1, 7, LZ0 + i, neon(ch, 'west', 1))
# recibidor: solo dos macetas junto a la entrada
for x in (23, 37):
    o.set(x, 1, LZ1, 'minecraft:potted_azalea_bush')

# ---------------- centro: ruleta de la suerte sobre una tarima redonda con candelabros
WX, WZ = CX, 27
for x in range(WX - 6, WX + 7):
    for z in range(WZ - 4, WZ + 9):
        d = ((x - WX) / 4.6) ** 2 + ((z - (WZ + 2)) / 4.6) ** 2
        if d <= 1.0:
            o.set(x, 0, z, BM if d > 0.75 else ('casino:casino_floor_deco'))
        if 0.85 < d <= 1.0 and z > WZ:
            o.set(x, 1, z, rope('purple'))
for x in range(WX - 2, WX + 3):
    for z in range(WZ - 1, WZ + 1):
        o.set(x, 1, z, BM)                       # tarima maciza: la rueda se apoya encima (antes era losa y quedaba flotando)
for x in range(WX - 3, WX + 4):                  # escalón de losas alrededor de la tarima para subir a la rueda
    o.set(x, 1, WZ + 1, slab(BMS))
for z in (WZ - 1, WZ):
    o.set(WX - 3, 1, z, slab(BMS)); o.set(WX + 3, 1, z, slab(BMS))
mq.place(o, 'wheel', WX, 2, WZ, 2)
# (sin columnas a los lados: tapaban la rueda; solo dos faroles bajos detrás de la tarima)
for dx in (-4, 4):
    o.set(WX + dx, 1, WZ - 2, GB); o.set(WX + dx, 2, WZ - 2, 'minecraft:end_rod[facing=up]')
for x, z in ((WX - 5, 31), (WX + 5, 31), (WX, 36), (WX - 5, 38), (WX + 5, 38)):
    o.set(x, Y_ROOF - 2, z, CHAND)

# ---------------- ala oeste (x 6..21, z 24..40): BAR con barra-isla al centro, taburetes alrededor y mesitas
for x in range(IX0, 22):
    for z in range(24, 41):
        o.set(x, 0, z, MAH if (x + z) % 2 else 'minecraft:dark_oak_planks')
for x in range(9, 19):
    for z in range(28, 37):
        o.set(x, 0, z, 'casino:casino_floor_royal')
BX0, BX1, BZ0, BZ1 = 10, 17, 29, 35      # barra-isla
for x in range(BX0, BX1 + 1):
    o.set(x, 1, BZ0, counter('north')); o.set(x, 1, BZ1, counter('south'))
for z in range(BZ0 + 1, BZ1):
    o.set(BX0, 1, z, counter('west')); o.set(BX1, 1, z, counter('east'))
# dentro: piso de servicio y estantes de botellas espalda con espalda, con columna dorada y letrero
for x in range(BX0 + 1, BX1):
    for z in range(BZ0 + 1, BZ1):
        o.set(x, 0, z, 'minecraft:dark_oak_planks')
for x in range(BX0 + 1, BX1):
    o.set(x, 1, BZ0 + 2, shelf('north')); o.set(x, 2, BZ0 + 2, shelf('north'))
    o.set(x, 1, BZ1 - 2, shelf('south')); o.set(x, 2, BZ1 - 2, shelf('south'))
    o.set(x, 1, 32, GB); o.set(x, 2, 32, GT); o.set(x, 3, 32, GB)
for x in range(BX0 + 1, BX1):
    for y in range(4, 10):
        o.set(x, y, 32, GT if y in (4, 9) else 'casino:velvet_wall_red')
for i, ch in enumerate('TRAGOS'):
    o.set(16 - i, 7, 31, neon(ch, 'north', 1)); o.set(11 + i, 7, 33, neon(ch, 'south', 1))
for x in (11, 16):
    o.set(x, 2, BZ0, drinks(2, 'north')); o.set(x, 2, BZ1, drinks(3, 'south'))
# taburetes alrededor de la barra
for x in (10, 12, 15, 17):
    o.set(x, 1, BZ0 - 1, stool('black', 'south')); o.set(x, 1, BZ1 + 1, stool('black', 'north'))
for z in range(BZ0 + 1, BZ1, 2):
    o.set(BX0 - 1, 1, z, stool('black', 'east')); o.set(BX1 + 1, 1, z, stool('black', 'west'))
# mesitas en las esquinas del bar
for (x, z) in ((8, 26), (19, 26), (8, 38), (19, 38)):
    o.set(x, 1, z, TABLE_B); o.set(x, 2, z, drinks(2))
    o.set(x - 1, 1, z, chair('red', 'east')); o.set(x + 1, 1, z, chair('red', 'west'))
for (x, z) in ((10, 26), (17, 26), (10, 38), (17, 38)):
    o.set(x, 1, z, 'minecraft:potted_azalea_bush')
o.set(IX0, 5, 28, speaker('east')); o.set(IX0, 5, 36, speaker('east'))

# ---------------- ala este: tragamonedas en hileras + plinko
for x in range(39, IX1 + 1):
    for z in range(24, 41):
        o.set(x, 0, z, 'casino:casino_floor_lounge')
SLOT_X = (41, 43, 45, 48, 50, 52)        # espejo: 41<->52, 43<->50, 45<->48; paso libre en x 46-47
for row_z in (26, 37):
    for x in SLOT_X:
        mq.place(o, 'slot', x, 1, row_z, 0)        # miran al norte
        mq.place(o, 'slot', x, 1, row_z + 1, 2)    # espalda con espalda, miran al sur
    for x in range(40, 54):
        lz = row_z if row_z < 32 else row_z + 1                  # del lado del pasillo central en las dos hileras
        o.set(x, 3, lz, 'minecraft:light[level=8]') if o.get(x, 3, lz) == AIR else None
for x in (42, 44, 49, 51):
    for z in (26, 27, 37, 38):
        o.set(x, 1, z, GT); o.set(x, 2, z, 'minecraft:end_rod[facing=up]')
# pasillo central de la sala (de la puerta al fondo): alfombra real
for x in range(39, IX1 + 1):
    for z in range(30, 35):
        o.set(x, 0, z, 'casino:casino_floor_royal')
o.set(IX1, 6, 29, sign(0, 'west')); o.set(IX1, 6, 35, sign(3, 'west'))
o.set(IX0, 6, 29, sign(5, 'east')); o.set(IX0, 6, 35, sign(7, 'east'))

# ---------------- fondo oeste: ESPECTÁCULO (x 6..21, z 10..21). Todo simétrico respecto a z=15.5, el eje de su puerta (z 14..17):
#                  al entrar se ve de frente el escenario (al fondo, oeste), la pista y dos bloques de sofás con pasillo al centro.
SH0, SH1 = IZ0, 21
for x in range(IX0, 23):
    for z in range(IZ0, 23):
        o.set(x, 0, z, MAH if (x + abs(2 * z - 31) // 2) % 2 else 'minecraft:dark_oak_planks')   # damero espejado en z=15.5
for z in range(SH0, SH1 + 1):
    for y in range(2, 9):
        o.set(6, y, z, 'casino:velvet_wall_red')                   # telón de fondo
    o.set(6, 9, z, GT)
for z in range(11, 21):
    for x in range(7, 11):
        o.set(x, 1, z, 'minecraft:dark_oak_planks')                 # escenario
    o.set(11, 1, z, stairs('minecraft:dark_oak_stairs', 'west'))   # escalones al frente
for z in (11, 20):                                                  # bambalinas
    for y in range(2, 9):
        o.set(7, y, z, 'minecraft:red_wool' if y < 8 else GT)
    o.set(8, 2, z, speaker('east')); o.set(8, 3, z, speaker('east'))
for i, ch in enumerate('SHOW'):
    o.set(7, 7, 17 - i, neon(ch, 'east', 2))                       # mirando al este se lee con z descendente
o.set(7, 5, 13, sign(1, 'east')); o.set(7, 5, 18, sign(1, 'east'))
o.set(8, 2, 15, 'minecraft:jukebox'); o.set(8, 2, 16, 'minecraft:jukebox')
for x in range(12, 15):                                             # pista frente al escenario
    for z in range(11, 21):
        o.set(x, 0, z, BM if (x + abs(2 * z - 31) // 2) % 2 else 'casino:marble')
for z in (12, 19):
    o.set(14, 1, z, TABLE_B); o.set(14, 2, z, drinks(2))
for z in list(range(11, 15)) + list(range(17, 21)):
    o.set(15, 1, z, sofa('red', 'west'))
for z in list(range(11, 14)) + list(range(18, 21)):
    o.set(18, 1, z, sofa('purple', 'west'))

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
o.marker(28, 1, 17, 'guard 2', [(0, 0, 0, 'minecraft:black_wool'), (0, 1, 0, 'minecraft:black_wool')])
o.set(26, 1, 16, rope('black')); o.set(34, 1, 16, rope('black'))
for x in range(27, 34):
    o.set(x, 0, 16, 'casino:casino_floor_royal'); o.set(x, 0, 17, 'casino:casino_floor_royal')
for i, ch in enumerate('VIP'):
    o.set(29 + i, 9, 16, neon(ch, 'south', 2))
o.set(24, 6, IZ0, sign(9, 'south')); o.set(36, 6, IZ0, sign(10, 'south'))

# ---------------- fondo este: PLINKO (x 39..54, z 10..21). Simétrico respecto a x=46.5 y a z=15.5 (el eje de su puerta, z 14..17):
#                  pasillo central de la puerta al fondo, rasca y gana a los dos lados del pasillo y 4 plinko contra las paredes.
for x in range(39, IX1 + 1):
    for z in range(IZ0, 23):
        o.set(x, 0, z, 'casino:casino_floor_galaxy')
for x in (43, 50):
    mq.place(o, 'plinko', x, 1, IZ0, 2)          # contra la pared norte, mirando al sur
    mq.place(o, 'plinko', x, 1, 21, 0)           # contra la pared sur, mirando al norte
for x in (41, 44, 49, 52):
    mq.place(o, 'scratch', x, 1, 13, 2)          # orilla norte del pasillo, mirando al pasillo
    mq.place(o, 'scratch', x, 1, 18, 0)          # orilla sur del pasillo
for i, ch in enumerate('PLINKO'):
    o.set(44 + i, 7, IZ0, neon(ch, 'south', 3)); o.set(49 - i, 7, 21, neon(ch, 'north', 3))
o.set(IX1, 6, 14, sign(0, 'west')); o.set(IX1, 6, 17, sign(0, 'west'))
for z in (11, 20):
    o.set(IX1, 1, z, 'minecraft:potted_azalea_bush'); o.set(39, 1, z, 'minecraft:potted_azalea_bush')

# ---------------- salas: muros con arcos dorados entre las zonas (oeste/este del salón central y frente a las alas)
def wall_x(x, z0, z1, gap0, gap1):
    for z in range(z0, z1 + 1):
        for y in range(1, Y_ROOF - 1):
            if gap0 <= z <= gap1 and y <= 5:
                o.set(x, y, z, AIR)
            elif gap0 - 1 <= z <= gap1 + 1 and y <= 6:
                o.set(x, y, z, GB if y in (1, 6) or z in (gap0 - 1, gap1 + 1) and y % 2 else GT)
            else:
                o.set(x, y, z, MAH if y <= 3 else ('casino:velvet_wall_purple' if y < Y_ROOF - 2 else GT))
def wall_z(z, x0, x1, gap0, gap1):
    for x in range(x0, x1 + 1):
        for y in range(1, Y_ROOF - 1):
            if gap0 <= x <= gap1 and y <= 5:
                o.set(x, y, z, AIR)
            elif gap0 - 1 <= x <= gap1 + 1 and y <= 6:
                o.set(x, y, z, GB if y in (1, 6) or x in (gap0 - 1, gap1 + 1) and y % 2 else GT)
            else:
                o.set(x, y, z, MAH if y <= 3 else ('casino:velvet_wall_purple' if y < Y_ROOF - 2 else GT))
wall_x(22, 23, 41, 29, 35)        # bar | salón central (puerta centrada en la sala: z 29..35, eje z=32)
wall_x(38, 23, 41, 29, 35)        # salón central | tragamonedas
wall_z(23, IX0, 21, -99, -99)     # escenario | bar (sin puerta)
wall_z(23, 39, IX1, -99, -99)     # plinko | tragamonedas (sin puerta)
wall_z(41, IX0, 21, -99, -99)     # bar | vestíbulo (sin puerta)
wall_z(41, 39, IX1, -99, -99)     # tragamonedas | vestíbulo (sin puerta)
wall_x(22, IZ0, 22, 14, 17)       # escenario | elevador (puerta centrada en la sala: z 14..17, eje z=15.5)
wall_x(38, IZ0, 22, 14, 17)       # elevador | plinko
wall_z(22, IX0, 21, -99, -99)     # escenario | bar (muro doble con el de z=23)
wall_z(22, 39, IX1, -99, -99)     # plinko | tragamonedas
wall_x(22, 42, IZ1, 44, 47)       # caja | recibidor
wall_x(38, 42, IZ1, 44, 47)       # recibidor | premios
# letreros de neón sobre los arcos (hacia el salón central y el vestíbulo)
for i, ch in enumerate('BAR'):
    o.set(22 + 1, 7, 33 - i, neon(ch, 'east', 1))
for i, ch in enumerate('SLOTS'):
    o.set(38 - 1, 7, 30 + i, neon(ch, 'west', 0))
for i, ch in enumerate('SHOW'):
    o.set(22 + 1, 7, 17 - i, neon(ch, 'east', 2))       # z 17..14, centrado en la puerta
for i, ch in enumerate('PLINKO'):
    o.set(38 - 1, 7, 13 + i, neon(ch, 'west', 3))       # z 13..18

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
elevator_stop(Y_VIP, 0, 'mesas')
# salón de mesas: 2 ruletas (oeste), 3 blackjack (este y sur) y el póker al centro
mq.place(o, 'poker', CX, Y_VIP + 1, 22, 2)
mq.place(o, 'roulette', 22, Y_VIP + 1, 19, 1)
mq.place(o, 'roulette', 22, Y_VIP + 1, 28, 1)
mq.place(o, 'blackjack', 38, Y_VIP + 1, 19, 3)
mq.place(o, 'blackjack', 38, Y_VIP + 1, 27, 3)
for x, z in ((CX, 21), (22, 19), (22, 28), (37, 19), (37, 27)):
    o.set(x, Y_VIP_TOP - 2, z, CHAND)
for (x, z) in ((TX0 + 2, TZ0 + 2), (TX1 - 2, TZ0 + 2), (TX0 + 2, TZ1 - 2), (TX1 - 2, TZ1 - 2)):
    o.set(x, Y_VIP + 1, z, 'minecraft:potted_azalea_bush')
for z in range(17, 31, 2):
    o.set(TX0 + 1, Y_VIP + 1, z, sofa('purple', 'east')) if z not in (19, 27) else None
for i, ch in enumerate('MESAS'):
    o.set(CX - 2 + i, Y_VIP + 6, 16, neon(ch, 'south', 2))
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
        if (x + z) % 4 == 0 and o.get(x, Y_ROOF - 3, z) == AIR:      # (nunca dentro de un muro: dejaría un hueco)
            o.set(x, Y_ROOF - 3, z, LIGHT)
for x in range(CX - 9, CX + 10, 3):
    o.set(x, Y_ROOF + 1, TER_Z0 + 15, sofa('blue', 'north'))
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
for x in (22, 24, 26, 34, 36, 38):
    o.set(x, Y_ROOF + 1, TER_Z0 + 3, sofa('blue', 'south'))        # camastros al norte de la alberca, mirándola
for x in (25, 35):                                                  # sombrillas
    o.set(x, Y_ROOF + 1, TER_Z0 + 2, 'minecraft:spruce_fence'); o.set(x, Y_ROOF + 2, TER_Z0 + 2, 'minecraft:spruce_fence')
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            o.set(x + dx, Y_ROOF + 3, TER_Z0 + 2 + dz, 'minecraft:white_wool' if (dx + dz) % 2 == 0 else 'minecraft:red_wool')
def cabana(x0, x1):
    z0, z1 = TER_Z0 + 4, TER_Z0 + 10
    for x in (x0, x1):
        for z in (z0, z1):
            for y in range(Y_ROOF + 1, Y_ROOF + 4):
                o.set(x, y, z, 'minecraft:dark_oak_fence')
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            o.set(x, Y_ROOF + 4, z, slab('minecraft:dark_oak_slab'))
            o.set(x, Y_ROOF, z, 'casino:casino_floor_royal')
    for z in range(z0 + 1, z1):
        o.set(x0, Y_ROOF + 3, z, 'minecraft:white_wool'); o.set(x1, Y_ROOF + 3, z, 'minecraft:white_wool')
    cx_ = (x0 + x1) // 2
    for z in (z0 + 2, z0 + 3, z0 + 4):
        o.set(x0 + 1, Y_ROOF + 1, z, sofa('red', 'east')); o.set(x1 - 1, Y_ROOF + 1, z, sofa('red', 'west'))
    o.set(cx_, Y_ROOF + 1, z0 + 3, TABLE_W); o.set(cx_, Y_ROOF + 2, z0 + 3, drinks(3))
    o.set(cx_, Y_ROOF + 3, z0 + 3, 'minecraft:lantern[hanging=true]')
cabana(7, 13)
cabana(47, 53)                                                       # espejo de la otra (7+53 = 60)
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
                    elif lvl == 2 and (abs(x - CX) + z) % 5 == 0:
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
                elif (abs(along - mid) % 3 == 0 if edge_z else min(along - z0, z1 - along) % 3 == 0):
                    o.set(x, y, z, QP)          # pilastras blancas continuas (simétricas desde el eje)
                elif lvl == 0:
                    o.set(x, y, z, SQ)          # antepecho blanco
                else:
                    o.set(x, y, z, GLASS)       # ventana oscura
    # ventanas encendidas al azar, pero iguales en los dos lados de la torre
    for y in range(y0 + 1, y1 + 1, 4):
        for x in range(x0 + 1, CX + 1):
            for z in range(z0 + 1, z1):
                if (x in (x0 + 1, x1 - 1) or z in (z0 + 1, z1 - 1)) and R.random() < 0.25:
                    st = 'minecraft:ochre_froglight' if R.random() < 0.5 else SEA
                    o.set(x, y + 1, z, st); o.set(2 * CX - x, y + 1, z, st)
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
# pista de baile iluminada: cuadros de 2x2 de luz de colores (froglights), simétrica respecto al eje
COLS_DANCE = ['minecraft:pearlescent_froglight', 'minecraft:verdant_froglight', 'minecraft:ochre_froglight', 'minecraft:sea_lantern']
for x in range(CX - 5, CX + 6):
    for z in range(21, 29):
        o.set(x, Y_PH_TOP, z, COLS_DANCE[((abs(x - CX) + 1) // 2 + (z - 21) // 2) % 4])
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
    o.set(CX + 2 - i, A + 2, SZ1 - 1, neon(ch, 'north', 1))      # mirando al norte: se escribe de este a oeste
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


# ================================================================== SÓTANO VIP (y -12..-1): carreras y salón privado
YB = -B                                   # piso del sótano
# todo lo que queda bajo tierra es macizo (tierra arriba, piedra abajo); luego se excava el sótano
for x in range(W):
    for z in range(D):
        for y in range(YB, 0):
            o.set(x, y, z, 'minecraft:dirt' if y >= -3 else 'minecraft:stone')
QX0, QX1, QZ0, QZ1 = PX0 + 1, PX1 - 1, PZ0 + 1, 34        # caja del sótano (muros incluidos): x 5..55, z 9..34
for x in range(QX0, QX1 + 1):
    for z in range(QZ0, QZ1 + 1):
        o.set(x, YB, z, BM)
        o.set(x, -1, z, SQ)
        for y in range(YB + 1, -1):
            edge = x in (QX0, QX1) or z in (QZ0, QZ1)
            o.set(x, y, z, BSB if edge else AIR)
# techo con retícula dorada y luces
for x in range(QX0 + 1, QX1):
    for z in range(QZ0 + 1, QZ1):
        if (x - CX) % 6 == 0 or (z - QZ0) % 6 == 0:
            o.set(x, -2, z, GT)
light_grid(QX0 + 2, QX1 - 2, QZ0 + 2, QZ1 - 2, -3, 4)
# forro de las paredes: caoba abajo, terciopelo negro y moldura
for y in range(YB + 1, -2):
    for x in range(QX0 + 1, QX1):
        for z in (QZ0, QZ1):
            o.set(x, y, z, MAH if y <= YB + 3 else ('casino:velvet_wall_black' if y < -3 else GT))
    for z in range(QZ0 + 1, QZ1):
        for x in (QX0, QX1):
            o.set(x, y, z, MAH if y <= YB + 3 else ('casino:velvet_wall_black' if y < -3 else GT))

# elevador: el hueco baja hasta el sótano
shaft(YB + 1, -1, YB)
elevator_stop(YB, 1, 'sotano')

# ---- salón privado VIP (x 6..36): alfombra real, bar privado al oeste, sofás y candelabros
for x in range(QX0 + 1, 37):
    for z in range(QZ0 + 1, QZ1):
        if not (28 <= x <= 32 and 11 <= z <= 15):
            o.set(x, YB, z, 'casino:casino_floor_royal')
for x in range(26, 35):
    for z in range(16, 19):
        o.set(x, YB, z, 'casino:casino_floor_deco')
for i, ch in enumerate('VIP'):
    o.set(29 + i, YB + 6, 16, neon(ch, 'south', 2)) if False else None
for z in range(12, 31):
    o.set(9, YB + 1, z, counter('east'))
    o.set(QX0 + 1, YB + 1, z, shelf('east')); o.set(QX0 + 1, YB + 2, z, shelf('east'))
    if z % 2 == 0:
        o.set(10, YB + 1, z, stool('purple', 'west'))
    if z % 4 == 1:
        o.set(9, YB + 2, z, drinks(1 + z % 3, 'east'))
o.set(9, YB + 1, 11, counter('east', 'outer_left')); o.set(9, YB + 1, 31, counter('east', 'outer_right'))
for i, ch in enumerate('VIP BAR'):
    o.set(QX0, YB + 5, 24 - i, neon(ch, 'east', 2)) if ch != ' ' else None
for (cx0, cz0) in ((15, 22), (33, 22), (15, 29)):          # (el de x=33, z=29 se quitó: ahí pasa la alfombra a carreras)     # espejo respecto a x=24; la salida del elevador (x 29..31) queda libre
    for dx in range(-1, 2):
        o.set(cx0 + dx, YB + 1, cz0 - 2, sofa('purple', 'south'))
        o.set(cx0 + dx, YB + 1, cz0 + 2, sofa('black', 'north'))
    o.set(cx0, YB + 1, cz0, TABLE_B); o.set(cx0, YB + 2, cz0, drinks(3))
    o.set(cx0, -3, cz0, CHAND)
for (x, z) in ((13, 31), (25, 12), (36, 33), (35, 10)):
    o.set(x, YB + 1, z, 'minecraft:potted_azalea_bush')
# chimenea al centro de la pared sur (x 22..26), con repisa dorada y el letrero VIP encima
for x in range(22, 27):
    for y in range(YB + 1, YB + 5):
        edge = x in (22, 26) or y == YB + 4
        o.set(x, y, QZ1 - 1, 'minecraft:polished_blackstone_bricks' if edge else AIR)
    o.set(x, YB + 5, QZ1 - 1, GT)
    o.set(x, YB, QZ1 - 1, 'minecraft:polished_blackstone')
for x in (23, 24, 25):
    o.set(x, YB + 1, QZ1 - 1, 'minecraft:campfire[lit=true,signal_fire=false]')
    o.set(x, YB, QZ1 - 2, GB)
for x in (21, 27):
    o.set(x, YB + 1, QZ1 - 1, 'minecraft:potted_azalea_bush')
for i, ch in enumerate('VIP'):
    o.set(25 - i, YB + 7, QZ1 - 1, neon(ch, 'north', 2))
# alfombra frente a la chimenea y sillones mirándola
for x in range(21, 28):
    for z in range(27, 32):
        o.set(x, YB, z, 'casino:casino_floor_deco') if o.get(x, YB, z).startswith('casino:casino_floor') else None
for x in (23, 24, 25):
    o.set(x, YB + 1, 28, sofa('purple', 'south'))
o.set(QX0 + 1, YB + 5, 27, speaker('east'))

# ---- sala de carreras (x 39..54, 16 de ancho: todo simétrico alrededor de x=46.5), como un cine. La pantalla está al SUR y la puerta
#      junto a ella: al bajar del elevador (que abre hacia el sur) la entrada queda enfrente; las gradas suben hacia el norte.
WR = 38                                   # muro entre el salón VIP y la sala
DZ0, DZ1 = 27, 31                         # puerta en el muro x=WR, al frente de la sala (junto a la pantalla)
for z in range(QZ0 + 1, QZ1):
    for y in range(YB + 1, -2):
        if DZ0 <= z <= DZ1 and y <= YB + 4:
            o.set(WR, y, z, AIR)
        elif DZ0 - 1 <= z <= DZ1 + 1 and y <= YB + 5:
            o.set(WR, y, z, GB if y in (YB + 1, YB + 5) or z in (DZ0 - 1, DZ1 + 1) else GT)
        else:
            o.set(WR, y, z, MAH if y <= YB + 3 else ('casino:velvet_wall_black' if y < -3 else GT))
for i, ch in enumerate('CARRERAS'):
    o.set(WR - 1, YB + 7, 25 + i, neon(ch, 'west', 2))          # mirando al oeste se lee con z ascendente; centrado en la puerta
for x in range(WR + 1, QX1):
    for z in range(QZ0 + 1, QZ1):
        o.set(x, YB, z, 'casino:casino_floor_galaxy')
# alfombra que guía de la salida del elevador a la puerta y hasta el frente de la sala
for x in range(29, 46):
    for z in range(DZ0 + 1, DZ1):
        o.set(x, YB, z, 'casino:casino_floor_royal')
# pantalla (12 x 6) con marco dorado en la pared sur
for x in range(40, 54):
    for y in range(YB + 2, YB + 10):
        o.set(x, y, QZ1, GB if x in (40, 53) or y in (YB + 2, YB + 9) else 'minecraft:black_concrete')
# mirando al norte la pantalla se extiende hacia el oeste desde el bloque (x 52..41)
o.marker(47, YB + 3, QZ1 - 1, 'tv 0 12 6', [(-dx, dy, 0, screen((dx + dy) % 4, 'north')) for dx in range(-5, 7) for dy in range(0, 6)])
for i, ch in enumerate('CARRERAS'):
    o.set(50 - i, YB + 9, QZ1 - 1, neon(ch, 'north', 2))       # mirando al norte se lee con x descendente
# gradas: 5 filas que suben 1 bloque cada una hacia el norte; pasillos: oeste (x 39-40), centro (x 46-47) y este (x 53-54)
AISLES = (39, 40, 46, 47, 53, 54)
for k in range(5):
    zr = 25 - 3 * k
    for z in range(QZ0 + 1, zr + 2):
        for x in range(WR + 1, QX1):
            o.set(x, YB + k, z, 'minecraft:dark_oak_planks' if z == zr + 1 else 'casino:casino_floor_galaxy')
    for x in range(WR + 1, QX1):
        if x in AISLES:
            if k:
                o.set(x, YB + k, zr + 1, stairs('minecraft:dark_oak_stairs', 'north'))
            continue
        o.set(x, YB + k + 1, zr, chair('blue' if k % 2 else 'black', 'south'))
    # luz suave en los escalones de los pasillos
    for x in (39, 54):
        o.set(x, YB + k + 3, zr, 'minecraft:light[level=12]')
# barandal de cordón delante de la primera fila y luces del frente
for x in range(41, 53):
    if x not in (46, 47):
        o.set(x, YB + 1, DZ0, rope('purple'))
for x in (39, 54):
    o.set(x, YB + 1, QZ1 - 1, 'minecraft:potted_azalea_bush')
light_grid(39, 54, 28, 32, YB + 5, 4, 'minecraft:light[level=13]')



# ================================================================== PASILLOS: alfombras con orilla dorada en las rutas principales
def runner(x0, x1, z0, z1, fy, edge=True, mat='casino:casino_floor_royal'):
    """Alfombra en el piso (fy) de x0..x1, z0..z1; con orilla de oro en los lados largos. No pisa bases de máquinas ni muros."""
    long_x = (x1 - x0) >= (z1 - z0)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            above = o.get(x, fy + 1, z)
            if not (above == AIR or above.startswith('minecraft:light')): continue
            side = (z in (z0, z1)) if long_x else (x in (x0, x1))
            o.set(x, fy, z, GB if (edge and side and (x1 - x0 >= 2 and z1 - z0 >= 2)) else mat)

# planta baja: entrada -> vestíbulo -> dos carriles alrededor de la rueda -> vestíbulo del elevador
runner(27, 33, 41, IZ1, 0)                 # de la puerta al salón (7 de ancho)
runner(23, 25, 18, 40, 0)                  # carril oeste (pasa por la puerta del bar y la del espectáculo)
runner(35, 37, 18, 40, 0)                  # carril este (tragamonedas y plinko)
runner(23, 37, 16, 21, 0, False)           # rellano frente al elevador
runner(23, 27, 14, 15, 0, False)           # la alfombra llega completa a la puerta del espectáculo (z 14..17)
runner(33, 37, 14, 15, 0, False)           # y a la del plinko
runner(18, 21, 30, 34, 0, False)           # tapete de entrada al bar
runner(19, 21, 14, 17, 0, False)           # tapete de entrada al espectáculo
runner(39, IX1, 14, 17, 0)                 # pasillo del plinko, de la puerta al fondo
# franja muerta detrás del hueco del elevador: se cierra
for x in range(27, 34):
    for y in range(1, Y_ROOF - 2):
        o.set(x, y, IZ0, MAH if y <= 3 else ('casino:velvet_wall_purple' if y < Y_ROOF - 3 else GT))
# primer piso: rellano del elevador, carriles a los lados del póker y salida a la terraza
runner(25, 35, 16, 19, Y_VIP)
runner(25, 27, 16, 32, Y_VIP)
runner(33, 35, 16, 32, Y_VIP)
runner(28, 32, 28, 32, Y_VIP, False)
# sótano: de la salida del elevador a la chimenea (y la alfombra a carreras ya existe)
runner(29, 31, 16, 27, YB)
runner(21, 31, 25, 26, YB, False)

# ================================================================== PULIDO: techos artesonados y paredes con pilastras, sala por sala
# Todo se reparte simétrico desde el centro de cada sala: vigas cada 4, casetones con panel de luz al centro y pilastras alineadas
# con las vigas. El techo de la planta baja baja a y=10 (macizo), así la alberca de arriba nunca gotea dentro.
WALLMAT = ('casino:mahogany_panel', 'casino:velvet_wall', 'casino:gold_trim', 'minecraft:quartz_pillar', 'minecraft:polished_blackstone_bricks', 'minecraft:gold_block', 'minecraft:chiseled_quartz_block',
           'minecraft:smooth_quartz', 'minecraft:red_wool')
SOFT = ('minecraft:air', 'minecraft:light', 'casino:gold_trim', 'minecraft:smooth_quartz', 'minecraft:ochre_froglight', 'casino:chandelier',
        'minecraft:sea_lantern', 'minecraft:end_rod')

def _soft(st):
    return any(st.startswith(k) for k in SOFT)

def lines(a, b, step=6):
    """Posiciones de vigas en [a, b], simétricas respecto al centro (a+b)/2."""
    out = []
    for p in range(a, b + 1):
        d2 = abs(2 * p - (a + b))
        if (a + b) % 2 == 0:
            if d2 % (2 * step) == step: out.append(p)
        else:
            if d2 % (2 * step) == step - 1: out.append(p)
    return out

def centers(a, b, beams):
    """Celdas del centro de cada casetón (entre vigas y paredes)."""
    edges = [a - 1] + beams + [b + 1]
    out = []
    for e0, e1 in zip(edges, edges[1:]):
        lo, hi = e0 + 1, e1 - 1
        if lo > hi: continue
        m2 = lo + hi
        out += [m2 // 2] if m2 % 2 == 0 else [m2 // 2, m2 // 2 + 1]
    return out

def glows(a, b, beams):
    """Panel de luz grande en cada casetón: todo menos el borde (un marco de 1 alrededor)."""
    edges = [a - 1] + beams + [b + 1]
    out = []
    for e0, e1 in zip(edges, edges[1:]):
        lo, hi = e0 + 1, e1 - 1
        if lo > hi: continue
        out += list(range(lo + 1, hi)) if hi - lo + 1 >= 4 else list(range(lo, hi + 1))
    return out

SHAFT = {(x, z) for x in range(28, 33) for z in range(11, 16)}

def coffer_mid(a, b, beams):
    """Centro de cada casetón: (celda, 1 si el ancho es par y el centro cae entre esa celda y la siguiente)."""
    edges = [a - 1] + beams + [b + 1]
    out = []
    for e0, e1 in zip(edges, edges[1:]):
        lo, hi = e0 + 1, e1 - 1
        if lo > hi: continue
        out.append(((lo + hi) // 2, (lo + hi) % 2))
    return out

GCH = 'casino:grand_chandelier'
def grand(x, y, z, ex=0, ez=0):
    """Candelabro grande colgado del techo: y es la celda pegada al techo; el bloque va una más abajo (cuelga de una vara de 1 bloque)."""
    if ex and ez: st, cells = f'{GCH}[facing=east,shift=2]', [(x + i, z + j) for i in (0, 1) for j in (0, 1)]
    elif ex: st, cells = f'{GCH}[facing=east,shift=1]', [(x + i, z + j) for i in (0, 1) for j in (-1, 0, 1)]
    elif ez: st, cells = f'{GCH}[facing=south,shift=1]', [(x + i, z + j) for i in (-1, 0, 1) for j in (0, 1)]
    else: st, cells = f'{GCH}[facing=north,shift=0]', [(x + i, z + j) for i in (-1, 0, 1) for j in (-1, 0, 1)]
    for (cx_, cz_) in cells:
        for yy in (y, y - 1, y - 2):
            g = o.get(cx_, yy, cz_)
            if not (g == AIR or g.startswith('minecraft:light')):
                return False
    o.set(x, y - 1, z, st)
    return True

def ceiling(x0, x1, z0, z1, cy, panel, beam, glow, chand='ring', extra=None):
    bx, bz = lines(x0, x1), lines(z0, z1)
    cx_, cz_ = centers(x0, x1, bx), centers(z0, z1, bz)
    gx_, gz_ = glows(x0, x1, bx), glows(z0, z1, bz)
    mx2, mz2 = x0 + x1, z0 + z1
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, z) in SHAFT: continue
            o.set(x, cy, z, panel)
            below = o.get(x, cy - 1, z)
            if x in bx or z in bz:
                if _soft(below): o.set(x, cy - 1, z, beam)
            else:
                if _soft(below): o.set(x, cy - 1, z, AIR)
                if x in gx_ and z in gz_:
                    o.set(x, cy, z, glow)
                elif (x in gx_) != (z in gz_) and (x not in bx and z not in bz):
                    pass
    # candelabros grandes en los casetones (simétricos); en casetones de ancho par el dibujo se corre medio bloque
    if chand:
        for (x, ex) in coffer_mid(x0, x1, bx):
            for (z, ez) in coffer_mid(z0, z1, bz):
                if (x, z) in SHAFT: continue
                ix = abs(2 * x + ex - mx2); iz = abs(2 * z + ez - mz2)
                ok = (chand == 'all') or (chand == 'ring' and (ix // 8 + iz // 8) % 2 == 0) or (chand == 'center' and ix <= 2 and iz <= 2)
                if ok:
                    grand(x, cy - 1, z, ex, ez)
    if extra: extra(bx, bz, cx_, cz_)

def dress_wall(cells, inward, y0, ytop, pil, panel='casino:velvet_wall_purple', base=MAH, sconce=True):
    """cells: [(x, z, t)] a lo largo del muro (t = coordenada a lo largo); pil: posiciones t de pilastras."""
    ts = [c[2] for c in cells]
    doorish = set()
    for (x, z, t) in cells:
        if o.get(x, y0 + 1, z) == AIR:
            doorish |= {t - 1, t, t + 1}
    for (x, z, t) in cells:
        if t in doorish: continue
        cur = [o.get(x, y, z) for y in range(y0 + 1, ytop + 1)]
        if not all(any(c.startswith(w) for w in WALLMAT) for c in cur): continue
        for y in range(y0 + 1, ytop + 1):
            h = y - y0
            if t in pil:
                st = GB if h == 1 else (CQ if y == ytop - 1 else (GT if y == ytop else QP))
            elif h <= 3:
                st = base
            elif h == 4 or y == ytop:
                st = GT
            else:
                st = panel
            o.set(x, y, z, st)
    if sconce:
        # apliques: a media altura en el centro de cada paño entre pilastras
        for c in centers(min(ts), max(ts), sorted(p for p in pil if min(ts) <= p <= max(ts))):
            for (x, z, t) in cells:
                if t != c or t in doorish: continue
                ix, iz = x + inward[0], z + inward[1]
                y = y0 + 6
                if o.get(ix, y, iz) == AIR and any(o.get(x, y, z).startswith(w) for w in WALLMAT):
                    f = {(1, 0): 'east', (-1, 0): 'west', (0, 1): 'south', (0, -1): 'north'}[inward]
                    o.set(ix, y, iz, f'minecraft:end_rod[facing={f}]')

def room_walls(x0, x1, z0, z1, y0, ytop, panel='casino:velvet_wall_purple', base=MAH):
    px, pz = lines(x0, x1), lines(z0, z1)
    dress_wall([(x, z0 - 1, x) for x in range(x0, x1 + 1)], (0, 1), y0, ytop, px, panel, base)
    dress_wall([(x, z1 + 1, x) for x in range(x0, x1 + 1)], (0, -1), y0, ytop, px, panel, base)
    dress_wall([(x0 - 1, z, z) for z in range(z0, z1 + 1)], (1, 0), y0, ytop, pz, panel, base)
    dress_wall([(x1 + 1, z, z) for z in range(z0, z1 + 1)], (-1, 0), y0, ytop, pz, panel, base)

PEARL = 'minecraft:pearlescent_froglight'
OCHRE = 'minecraft:ochre_froglight'
CY = Y_ROOF - 2          # techo de la planta baja (macizo)
# --- planta baja: paredes (mismo estilo en todas: caoba, moldura dorada, terciopelo morado y pilastras de cuarzo)
for (a, b, c, d) in ((6, 21, 42, 49), (39, 54, 42, 49), (23, 37, 10, 49), (6, 21, 24, 40), (39, 54, 24, 40), (6, 21, 10, 21), (39, 54, 10, 21)):
    room_walls(a, b, c, d, 0, CY - 1)
# --- planta baja: techos por sala
ceiling(6, 21, 42, 49, CY, SQ, GT, PEARL, 'all')                                   # caja
ceiling(39, 54, 42, 49, CY, SQ, GT, PEARL, 'all')                                  # premios
def _axis(bx, bz, cx_, cz_):
    # medallón dorado sobre la rueda: anillo de oro en el techo y candelabro grande al centro
    for x in range(23, 38):
        for z in range(22, 37):
            d = ((x - WX) ** 2 + (z - (WZ + 2)) ** 2) ** 0.5
            if 4.5 <= d < 5.5: o.set(x, CY, z, GB)
ceiling(23, 37, 10, 49, CY, SQ, GT, PEARL, 'ring', _axis)                          # eje central (rueda y elevador)
def _bar(bx, bz, cx_, cz_):
    for x in (10, 12, 15, 17):                                                    # lámparas colgantes sobre la barra
        for z in (BZ0, BZ1):
            if o.get(x, CY - 1, z) == AIR or o.get(x, CY - 1, z).startswith('minecraft:stripped'):
                o.set(x, CY - 1, z, 'minecraft:lantern[hanging=true]')
ceiling(6, 21, 24, 40, CY, 'minecraft:dark_oak_planks', 'minecraft:stripped_dark_oak_wood', OCHRE, None, _bar)   # bar
def _slots(bx, bz, cx_, cz_):
    for x in range(39, 55):                                                        # franja de luz sobre el pasillo
        o.set(x, CY, 32, PEARL)
ceiling(39, 54, 24, 40, CY, 'minecraft:black_concrete', GT, 'minecraft:magenta_stained_glass', 'center', _slots)  # tragamonedas
def _show(bx, bz, cx_, cz_):
    for z in (12, 14, 17, 19):                                                     # reflectores sobre el escenario
        o.set(9, CY - 1, z, 'minecraft:end_rod[facing=down]')
ceiling(6, 21, 10, 21, CY, 'casino:velvet_wall_red', 'minecraft:black_concrete', OCHRE, None, _show)               # espectáculo
def _plinko(bx, bz, cx_, cz_):
    rs = random.Random(11)
    for x in range(39, 47):                                                        # cielo estrellado espejado en x y en z
        for z in range(10, 16):
            if rs.random() < 0.12 and x not in bx and z not in bz:
                for (a, b) in ((x, z), (93 - x, z), (x, 31 - z), (93 - x, 31 - z)):
                    o.set(a, CY, b, SEA)
ceiling(39, 54, 10, 21, CY, 'minecraft:black_concrete', 'minecraft:purple_concrete', 'minecraft:amethyst_block', None, _plinko)  # plinko
# --- penthouse: casetones dorados con candelabros
ceiling(PI0, PI1, PJ0, PJ1, Y_PH_TOP - 1, SQ, GT, PEARL, 'ring')
# --- primer piso (salón de mesas): techo de cuarzo con casetones y candelabros
ceiling(TX0 + 1, TX1 - 1, TZ0 + 1, TZ1 - 1, Y_VIP_TOP - 1, SQ, GT, PEARL, 'ring')
# --- sótano: salón VIP (terciopelo negro) y carreras (cielo oscuro con luces de cine)
room_walls(QX0 + 1, 37, QZ0 + 1, QZ1 - 1, YB, -3, 'casino:velvet_wall_black')
ceiling(QX0 + 1, 37, QZ0 + 1, QZ1 - 1, -2, 'minecraft:black_concrete', GT, OCHRE, 'ring')
ceiling(39, 54, QZ0 + 1, QZ1 - 1, -2, 'minecraft:black_concrete', GT, SEA, None)

# --- ningún letrero de neón puede quedar tapado por una viga o un panel
_D = {'south': (0, 1), 'north': (0, -1), 'east': (1, 0), 'west': (-1, 0)}
for (x, y, z), st in list(o.b.items()):
    if 'neon_letter' in st:
        dx, dz = _D[st.split('facing=')[1].split(',')[0].split(']')[0]]
        ly = y - o.oy
        fr = o.get(x + dx, ly, z + dz)
        if not (fr.startswith('minecraft:air') or fr.startswith('minecraft:light')):
            o.set(x + dx, ly, z + dz, AIR)

# ================================================================== FACHADA TRASERA (antes era un muro liso)
def facade_back():
    z = PZ0
    for x in range(PX0, PX1 + 1):
        rel = (x - PX0) % 4
        for y in range(2, Y_ROOF):
            if rel == 0:
                o.set(x, y, z, QP if y < Y_ROOF - 2 else (GT if y == Y_ROOF - 2 else CQ))
            elif 4 <= y <= 8:
                o.set(x, y, z, TINT)
            elif y == 2:
                o.set(x, y, z, BM)
            else:
                o.set(x, y, z, SQ)
    for x in range(PX0 - 1, PX1 + 2):
        o.set(x, Y_ROOF, z - 1, stairs('minecraft:smooth_quartz_stairs', 'south', 'top'))
    # faroles en las pilastras
    for x in range(PX0 + 4, PX1, 8):
        o.set(x, 5, z - 1, 'minecraft:lantern[hanging=false]') if o.get(x, 4, z - 1) != AIR else None
facade_back()
# cornisa también en los lados
for z in range(PZ0, PZ1 + 1):
    o.set(PX0 - 1, Y_ROOF, z, stairs('minecraft:smooth_quartz_stairs', 'east', 'top'))
    o.set(PX1 + 1, Y_ROOF, z, stairs('minecraft:smooth_quartz_stairs', 'west', 'top'))
# farolas en el patio trasero, simétricas
for x in (PX0 + 6, PX1 - 6, CX - 8, CX + 8):
    o.set(x, 1, 3, 'minecraft:polished_blackstone_wall'); o.set(x, 2, 3, 'minecraft:polished_blackstone_wall')
    o.set(x, 3, 3, GB); o.set(x, 4, 3, 'minecraft:lantern')

# ================================================================== CORDONES: uniones ya calculadas en la plantilla (rectas y en diagonal)
def _ropes():
    posts = {k: v for k, v in o.b.items() if v.startswith('casino:rope_post')}
    isp = lambda x, y, z: (x, y, z) in posts
    for (x, y, z), st in posts.items():
        base = st.split('[')[0]
        n, e, s_, w = isp(x, y, z - 1), isp(x + 1, y, z), isp(x, y, z + 1), isp(x - 1, y, z)
        d = {}
        for name, dx, dz in (('ne', 1, -1), ('se', 1, 1), ('sw', -1, 1), ('nw', -1, -1)):
            d[name] = isp(x + dx, y, z + dz) and not isp(x + dx, y, z) and not isp(x, y, z + dz)
        props = dict(north=n, east=e, south=s_, west=w, **d)
        o.b[(x, y, z)] = base + '[' + ','.join(f'{k}={str(v).lower()}' for k, v in props.items()) + ']'
_ropes()

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'the_diamond.nbt'
    pal, n = o.save(out)
    print('paleta', pal, 'bloques', n, '->', out)
    if len(sys.argv) > 2:
        o.dump(sys.argv[2]); print('volcado', sys.argv[2])
