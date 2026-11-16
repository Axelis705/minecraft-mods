#!/usr/bin/env python3
"""THE DIAMOND: casino elegante (bosque oscuro, nieve y taiga). Torre art déco al estilo del Diamond Casino de GTA V.

Uso: the_diamond.py [salida.nbt] [volcado.txt]
Frente (entrada) al sur (z máxima). Pisos (y=0 es la planta baja; la plantilla lleva B bloques de sótano debajo):
  y=-24 bóveda (robo, nivel Diamante): antesala con láseres y guardias armados, puerta redonda y bóveda con carritos de botín
  y=-12 sótano VIP (tarjeta Plata): carreras de caballos con pantalla gigante y salón privado con bar; detrás (z 37..49) la zona de
        empleados, a la que solo se llega por la caseta de servicio del patio trasero
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
B = 38   # bloques bajo la planta baja: sótano VIP (y -12), sala de guardias (y -24) y bóveda (y -36) (TemplateCasinoStructure.sink("the_diamond") debe valer lo mismo)
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
# anfitrión de la ruleta: parado en la tarima, a la derecha de la rueda (vista de frente), mirando al público
o.marker(WX + 2, 2, WZ, 'wheelhost 2', [(0, 0, 0, 'minecraft:red_wool')])
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
    o.set(x - 1, 1, z, stool('black', 'east')); o.set(x + 1, 1, z, stool('black', 'west'))
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
for (x, z) in ((14, 10), (17, 10), (14, 21), (17, 21)):
    o.set(x, 1, z, TABLE_B); o.set(x, 2, z, drinks(3 if x == 14 else 2, 'south'))
o.set(6, 1, 10, 'casino:velvet_wall_red'); o.set(6, 1, 21, 'casino:velvet_wall_red')   # el telón llega al piso en las esquinas
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

# bartenders: dos en la barra-isla del bar, uno por cada lado y al centro de la isla (el del bar VIP está con el sótano)
o.marker(13, 1, 30, 'bartender 0 x', [(0, 0, 0, 'minecraft:purple_wool'), (0, 1, 0, 'minecraft:purple_wool')])
o.marker(13, 1, 34, 'bartender 2 x', [(0, 0, 0, 'minecraft:purple_wool'), (0, 1, 0, 'minecraft:purple_wool')])
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
# (los rasca y gana se mudaron junto al elevador, a los lados de su puerta)
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
# piso del salón de mesas: mármol negro con incrustaciones doradas justo debajo de las vigas del techo (mismo reparto que el techo),
# una alfombra azul con orilla dorada debajo de cada mesa y los pasillos rojos (más abajo, con runner)
o.fill(TX0 + 1, Y_VIP, TZ0 + 1, TX1 - 1, Y_VIP, TZ1 - 1, BM)
def table_rug(x0, x1, z0, z1):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            o.set(x, Y_VIP, z, GT if x in (x0, x1) or z in (z0, z1) else 'casino:casino_floor_lounge')
for (x0, x1) in ((19, 24), (36, 41)):                 # ruleta (arriba) y blackjack (abajo) a cada lado, en espejo
    table_rug(x0, x1, 15, 23)
    table_rug(x0, x1, 25, 31)
table_rug(28, 32, 20, 26)                             # póker al centro
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
# marco dorado de la puerta, toldo a rayas afuera con faroles y letrero TERRAZA encima
for y in range(Y_VIP + 1, Y_VIP + 5):
    o.set(CX - 3, y, TZ1, GB); o.set(CX + 3, y, TZ1, GB)
for x in range(CX - 2, CX + 3):
    o.set(x, Y_VIP + 4, TZ1, GT)
for x in range(CX - 3, CX + 4):
    for z in (TZ1 + 1, TZ1 + 2):
        o.set(x, Y_VIP + 5, z, 'minecraft:white_wool' if x % 2 else 'minecraft:blue_wool')
    o.set(x, Y_VIP + 5, TZ1 + 3, slab('minecraft:smooth_quartz_slab', 'top'))
for x in (CX - 3, CX + 3):
    o.set(x, Y_VIP + 4, TZ1 + 1, 'minecraft:lantern[hanging=true]')
for i, ch in enumerate('TERRAZA'):
    o.set(CX - 3 + i, Y_VIP + 7, TZ1 + 1, neon(ch, 'south', 4))
shaft(Y_VIP + 1, Y_VIP_TOP - 1, Y_VIP)
elevator_stop(Y_VIP, 0, 'mesas')
# salón de mesas, simétrico respecto a x=30: a cada lado una ruleta (al norte) y un blackjack (al sur), mirando al centro, y el
# póker en medio. Los candelabros grandes los pone el techo (más abajo).
mq.place(o, 'poker', CX, Y_VIP + 1, 24, 0)       # mira al elevador: el crupier queda del lado de la terraza
mq.place(o, 'roulette', 22, Y_VIP + 1, 19, 1)
mq.place(o, 'roulette', 38, Y_VIP + 1, 19, 3)
mq.place(o, 'blackjack', 22, Y_VIP + 1, 28, 1)
mq.place(o, 'blackjack', 38, Y_VIP + 1, 28, 3)
for i, ch in enumerate('MESAS'):
    o.set(CX - 2 + i, Y_VIP + 6, 16, neon(ch, 'south', 2))
o.set(TX0 + 1, Y_VIP + 4, 20, sign(11, 'east')); o.set(TX1 - 1, Y_VIP + 4, 20, sign(11, 'west'))
# salitas en las 4 esquinas (sofás en L, mesita con bebidas y planta) y sofás para mirar las mesas junto a las ventanas
def corner_lounge(xw, zn, sx, sz):
    """xw, zn: la esquina; sx, sz: hacia dónde queda el salón (+1/-1)."""
    side = 'east' if sx > 0 else 'west'
    front = 'south' if sz > 0 else 'north'
    o.set(xw, Y_VIP + 1, zn, 'minecraft:potted_azalea_bush')
    for k in (1, 2):
        o.set(xw, Y_VIP + 1, zn + sz * k, sofa('black', side))
        o.set(xw + sx * k, Y_VIP + 1, zn, sofa('black', front))
    o.set(xw + sx, Y_VIP + 1, zn + sz, TABLE_B); o.set(xw + sx, Y_VIP + 2, zn + sz, drinks(2, front))
corner_lounge(TX0 + 1, TZ0 + 1, 1, 1); corner_lounge(TX1 - 1, TZ0 + 1, -1, 1)
corner_lounge(TX0 + 1, TZ1 - 1, 1, -1); corner_lounge(TX1 - 1, TZ1 - 1, -1, -1)
for z in (16, 17, 25, 26):
    o.set(TX0 + 1, Y_VIP + 1, z, sofa('purple', 'east')); o.set(TX1 - 1, Y_VIP + 1, z, sofa('purple', 'west'))

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
LOUNGER, PARASOL = 'casino:sun_lounger', 'casino:parasol'
def lounger(x, z, f): o.set(x, Y_ROOF + 1, z, f'{LOUNGER}[facing={f}]')
def parasol(x, z, y=None):
    y = Y_ROOF + 1 if y is None else y
    o.set(x, y, z, f'{PARASOL}[facing=north,half=lower]'); o.set(x, y + 1, z, f'{PARASOL}[facing=north,half=upper]')
# camastros con sombrilla en medio, en grupos de 3, todo en espejo respecto a x=30: al norte de la alberca mirándola (dejando libre la
# salida de la puerta, x 27..33) y al sur, a los lados del bar
for (xa, xb) in ((20, 22), (24, 26), (34, 36), (38, 40)):
    lounger(xa, TER_Z0 + 3, 'south'); parasol(xa + 1, TER_Z0 + 3); lounger(xb, TER_Z0 + 3, 'south')
for (xa, xb) in ((20, 22), (38, 40)):
    lounger(xa, TER_Z0 + 14, 'north'); parasol(xa + 1, TER_Z0 + 14); lounger(xb, TER_Z0 + 14, 'north')
# bar de la alberca al sur, al centro: barra mirando a la alberca con taburetes, toldo a rayas y bartender
for x in range(CX - 3, CX + 4):
    o.set(x, Y_ROOF + 1, TER_Z0 + 14, counter('north'))
    o.set(x, Y_ROOF + 1, TER_Z0 + 13, stool('blue', 'south')) if x % 2 == 0 else None
    o.set(x, Y_ROOF + 5, TER_Z0 + 14, 'minecraft:white_wool' if x % 2 else 'minecraft:blue_wool')     # (alto: que no tape el
    o.set(x, Y_ROOF + 5, TER_Z0 + 15, 'minecraft:white_wool' if x % 2 else 'minecraft:blue_wool')     #  globo del bartender)
o.set(CX - 3, Y_ROOF + 2, TER_Z0 + 14, drinks(3)); o.set(CX + 3, Y_ROOF + 2, TER_Z0 + 14, drinks(3))
for x in (CX - 4, CX + 4):
    for y in range(Y_ROOF + 1, Y_ROOF + 5):
        o.set(x, y, TER_Z0 + 15, 'minecraft:stripped_jungle_log[axis=y]')
    o.set(x, Y_ROOF + 5, TER_Z0 + 15, GB)
for x in (CX - 3, CX + 3):
    o.set(x, Y_ROOF + 4, TER_Z0 + 14, 'minecraft:lantern[hanging=true]')     # colgados del toldo
for x in (CX - 2, CX + 2):
    o.set(x, Y_ROOF + 1, TER_Z0 + 15, shelf('north'))
o.marker(CX, Y_ROOF + 1, TER_Z0 + 15, 'bartender 0', [(0, 0, 0, 'minecraft:purple_wool'), (0, 1, 0, 'minecraft:purple_wool')])
# a los lados de la alberca (donde iban los jacuzzis): camastros mirando a la alberca, sombrilla en medio y macetas
for (x, f) in ((17, 'east'), (43, 'west')):
    lounger(x, TER_Z0 + 7, f); parasol(x, TER_Z0 + 8); lounger(x, TER_Z0 + 9, f)
    for z in (TER_Z0 + 6, TER_Z0 + 10):
        o.set(x, Y_ROOF + 1, z, 'minecraft:potted_azalea_bush')
# palmeras en las 4 esquinas de la zona de la alberca (tronco de jungla y copa en estrella)
def palm(x, z, lean):
    o.set(x, Y_ROOF + 1, z, GB)
    top = Y_ROOF + 6
    for y in range(Y_ROOF + 2, top + 1):
        o.set(x + (lean if y >= top - 1 else 0), y, z, 'minecraft:jungle_log[axis=y]')
    px = x + lean
    leaf = 'minecraft:jungle_leaves[persistent=true]'
    o.set(px, top + 1, z, leaf)
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        o.set(px + dx, top + 1, z + dz, leaf)
        o.set(px + dx * 2, top, z + dz * 2, leaf)
        o.set(px + dx * 3, top - 1, z + dz * 3, leaf)
    for (dx, dz) in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        o.set(px + dx, top, z + dz, leaf)
        o.set(px + dx * 2, top - 1, z + dz * 2, leaf)
    o.set(px, top, z + 1, 'minecraft:cocoa[facing=north,age=2]')
    o.set(px, top, z - 1, 'minecraft:cocoa[facing=south,age=2]')
for (x, z) in ((18, TER_Z0 + 3), (42, TER_Z0 + 3), (18, TER_Z0 + 12), (42, TER_Z0 + 12)):    # (lejos del muro del salón y del barandal)
    palm(x, z, 1 if x < CX else -1)
# postes con farol en las esquinas de la alberca y en las esquinas de la terraza
for (x, z) in ((PX0 + 3, TER_Z0 + 2), (PX1 - 3, TER_Z0 + 2), (PX0 + 3, PZ1 - 3), (PX1 - 3, PZ1 - 3)):
    o.set(x, Y_ROOF + 1, z, 'minecraft:polished_blackstone_wall'); o.set(x, Y_ROOF + 2, z, 'minecraft:polished_blackstone_wall')
    o.set(x, Y_ROOF + 3, z, 'minecraft:lantern')
for x in (CX - 10, CX + 10):
    for z in (TER_Z0 + 5, TER_Z0 + 12):
        o.set(x, Y_ROOF + 1, z, 'minecraft:polished_blackstone_wall')
        o.set(x, Y_ROOF + 2, z, 'minecraft:polished_blackstone_wall')
        o.set(x, Y_ROOF + 3, z, 'minecraft:lantern')
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
# alas laterales del techo (fuera de la torre): jardines en espejo (x -> 60 - x) con un camino de grava clara desde la terraza, una
# fuente redonda al centro, bancas alrededor, faroles, setos recortados contra los muros y arriates de flores
GZ0, GZ1 = PZ0 + 2, TER_Z0 - 1          # z 10..33
GCZ = (GZ0 + GZ1) // 2                   # centro de la fuente
def garden(sx):
    """sx = +1 jardín oeste (x 6..15), -1 el del este (se dibuja en espejo)."""
    X = (lambda x: x) if sx > 0 else (lambda x: 60 - x)
    side = lambda f: f if sx > 0 else {'east': 'west', 'west': 'east'}.get(f, f)
    for x in range(PX0 + 2, TX0 - 1):
        for z in range(GZ0, GZ1 + 1):
            o.set(X(x), Y_ROOF, z, 'minecraft:grass_block' if (x + z) % 3 else 'minecraft:moss_block')
            o.set(X(x), Y_ROOF + 1, z, AIR)
    gx = (PX0 + 2 + TX0 - 2) // 2         # x 10 / 50: eje del jardín
    # camino: de la terraza a la fuente y alrededor de ella
    for z in range(GZ0 + 2, GZ1 + 1):
        for x in (gx, gx + 1):
            o.set(X(x), Y_ROOF, z, 'minecraft:mud_bricks' if z % 4 == 0 else 'minecraft:packed_mud')
    for dx in range(-3, 5):
        for dz in range(-3, 4):
            if abs(dx - 0.5) + abs(dz) <= 5.5:
                o.set(X(gx + dx), Y_ROOF, GCZ + dz, 'minecraft:packed_mud' if (dx + dz) % 3 else 'minecraft:mud_bricks')
    # (Axelis quitó la fuente: la plaza queda libre, de lodo compactado)
    # bancas alrededor de la plaza (negras, como en el salón)
    for dz in (0, 1):
        o.set(X(gx - 3), Y_ROOF + 1, GCZ + dz, sofa('black', side('east')))
        o.set(X(gx + 4), Y_ROOF + 1, GCZ + dz, sofa('black', side('west')))
    for dx in (0, 1):
        o.set(X(gx + dx), Y_ROOF + 1, GCZ - 4, sofa('black', 'south'))
    # faroles a los lados del camino y en la entrada desde la terraza
    for z in (GZ0 + 3, GCZ + 5, GZ1 - 1):
        for x in (gx - 1, gx + 2):
            o.set(X(x), Y_ROOF + 1, z, 'minecraft:polished_blackstone_wall')
            o.set(X(x), Y_ROOF + 2, z, 'minecraft:lantern')
    # setos recortados contra el barandal y contra el muro del salón, con arriates de flores entre ellos
    for z in range(GZ0, GZ1 + 1):
        for x in (PX0 + 2, TX0 - 2):
            o.set(X(x), Y_ROOF + 1, z, 'minecraft:azalea_leaves[persistent=true]' if z % 4 else 'minecraft:flowering_azalea_leaves[persistent=true]')
    flowers = ('minecraft:red_tulip', 'minecraft:white_tulip', 'minecraft:allium', 'minecraft:oxeye_daisy', 'minecraft:blue_orchid')
    for z in range(GZ0 + 1, GZ1):
        if abs(z - GCZ) <= 5: continue
        for x in (PX0 + 3, TX0 - 3):
            if o.get(X(x), Y_ROOF + 1, z) == AIR:
                o.set(X(x), Y_ROOF + 1, z, flowers[(z + (0 if x == PX0 + 3 else 2)) % len(flowers)])
    # árboles: azaleas en flor en las 4 esquinas del jardín
    for (x, z) in ((PX0 + 4, GZ0 + 2), (TX0 - 4, GZ0 + 2), (PX0 + 4, GZ1 - 3), (TX0 - 4, GZ1 - 3)):
        o.set(X(x), Y_ROOF + 1, z, 'minecraft:oak_log[axis=y]')
        o.set(X(x), Y_ROOF + 2, z, 'minecraft:oak_log[axis=y]')
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                o.set(X(x + dx), Y_ROOF + 3, z + dz, 'minecraft:flowering_azalea_leaves[persistent=true]')
        o.set(X(x), Y_ROOF + 4, z, 'minecraft:flowering_azalea_leaves[persistent=true]')
garden(1); garden(-1)

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
    o.set(x, P, 24, stool('red', 'south')); o.set(x, P, 26, stool('red', 'north'))
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
    o.set(x, P, PJ1, sofa('purple', 'south'))
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
YB = -12                                  # piso del sótano
# todo lo que queda bajo tierra es macizo (tierra arriba, piedra abajo); luego se excava el sótano
for x in range(W):
    for z in range(D):
        for y in range(-B, 0):
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
o.marker(8, YB + 1, 21, 'bartender 1', [(0, 0, 0, 'minecraft:purple_wool'), (0, 1, 0, 'minecraft:purple_wool')])   # bartender del bar VIP
for i, ch in enumerate('VIP BAR'):
    o.set(QX0, YB + 5, 24 - i, neon(ch, 'east', 2)) if ch != ' ' else None
for (cx0, cz0) in ((15, 22), (35, 22), (15, 29)):          # (el de x=33, z=29 se quitó: ahí pasa la alfombra a carreras)     # espejo respecto a x=24; la salida del elevador (x 29..31) queda libre
    col = 'purple' if cz0 == 29 else 'black'
    for dx in range(-1, 2):
        o.set(cx0 + dx, YB + 1, cz0 - 2, sofa(col, 'south'))
        o.set(cx0 + dx, YB + 1, cz0 + 2, sofa(col, 'north'))
    o.set(cx0, YB + 1, cz0, TABLE_B); o.set(cx0, YB + 2, cz0, drinks(3))
    o.set(cx0, -3, cz0, CHAND)
for (x, z) in ((25, 12), (36, 33)):
    o.set(x, YB + 1, z, 'minecraft:potted_azalea_bush')
# chimenea al centro de la pared sur (x 21..27): columnas de cuarzo, marco de mármol negro, repisa dorada, un cuadro encima y el
# letrero VIP arriba; enfrente una alfombra con dos sillones frente a frente y una mesita (así se ve el fuego desde el salón)
FZ = QZ1 - 1
for y in range(YB + 1, YB + 6):
    for x in (21, 27):
        o.set(x, y, FZ, GB if y == YB + 1 else (CQ if y == YB + 5 else QP))
for y in range(YB + 1, YB + 4):
    o.set(22, y, FZ, BM); o.set(26, y, FZ, BM)
for x in (23, 24, 25):
    o.set(x, YB + 1, FZ, 'minecraft:campfire[lit=true,signal_fire=false]')
    o.set(x, YB + 2, FZ, AIR)
    o.set(x, YB + 3, FZ, BM)
    o.set(x, YB, FZ, 'minecraft:polished_blackstone')
for x in range(22, 27):
    o.set(x, YB + 4, FZ, GT)
    o.set(x, YB, FZ - 1, GB)
for x in range(21, 28):
    for y in range(YB + 5, YB + 9):
        if 22 <= x <= 26:
            o.set(x, y, FZ, AIR)
for i, ch in enumerate('VIP'):
    o.set(25 - i, YB + 8, FZ, neon(ch, 'north', 2))
for x in range(21, 28):
    for z in range(27, 32):
        o.set(x, YB, z, 'casino:casino_floor_deco') if o.get(x, YB, z).startswith('casino:casino_floor') else None
for z in (28, 29, 30):
    o.set(21, YB + 1, z, sofa('purple', 'east')); o.set(27, YB + 1, z, sofa('purple', 'west'))
o.set(24, YB + 1, 29, TABLE_B); o.set(24, YB + 2, 29, drinks(3, 'south'))
o.set(QX0 + 1, YB + 5, 27, speaker('east'))

# ---- sala de carreras (x 39..54, 16 de ancho: todo simétrico alrededor de x=46.5), como un cine. La pantalla está al SUR y la puerta
#      junto a ella: al bajar del elevador (que abre hacia el sur) la entrada queda enfrente; las gradas suben hacia el norte.
WR = 38                                   # muro entre el salón VIP y la sala
DZ0, DZ1 = 27, 32                         # puerta en el muro x=WR, al frente de la sala (junto a la pantalla); 6 de ancho, centro z=29.5
for z in range(QZ0 + 1, QZ1):
    for y in range(YB + 1, -2):
        if DZ0 <= z <= DZ1 and y <= YB + 4:
            o.set(WR, y, z, AIR)
        elif DZ0 - 1 <= z <= DZ1 + 1 and y <= YB + 5:
            o.set(WR, y, z, GB if y in (YB + 1, YB + 5) or z in (DZ0 - 1, DZ1 + 1) else GT)
        else:
            o.set(WR, y, z, MAH if y <= YB + 3 else ('casino:velvet_wall_black' if y < -3 else GT))
for i, ch in enumerate('CARRERAS'):
    o.set(WR - 1, YB + 7, 26 + i, neon(ch, 'west', 2))          # z 26..33: mirando al oeste se lee con z ascendente; centrado en la puerta
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
# (los carriles con alfombra a los lados de la rueda se quitaron: ahí sigue el mármol del salón)

runner(23, 37, 16, 21, 0, False)           # rellano frente al elevador
runner(23, 27, 14, 15, 0, False)           # la alfombra llega completa a la puerta del espectáculo (z 14..17)
runner(33, 37, 14, 15, 0, False)           # y a la del plinko
runner(18, 21, 30, 34, 0, False)           # tapete de entrada al bar
runner(19, 21, 14, 17, 0, False)           # tapete de entrada al espectáculo
runner(39, 46, 14, 17, 0)                  # pasillo del plinko (la mitad de atrás queda con el piso de galaxia, frente a la pantalla)
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
# --- primer piso (salón de mesas): pared norte vestida (caoba, terciopelo negro y pilastras alineadas con las vigas), incrustaciones
# doradas en el piso justo debajo de las vigas y techo de cuarzo con casetones y candelabros grandes
dress_wall([(x, TZ0, x) for x in range(TX0 + 1, TX1)], (0, 1), Y_VIP, Y_VIP_TOP - 2, lines(TX0 + 1, TX1 - 1), 'casino:velvet_wall_black')
for x in range(TX0 + 1, TX1):
    for z in range(TZ0 + 1, TZ1):
        if (x in lines(TX0 + 1, TX1 - 1) or z in lines(TZ0 + 1, TZ1 - 1)) and o.get(x, Y_VIP, z) == BM:
            o.set(x, Y_VIP, z, GT)
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

# ================================================================== AJUSTES (13 nov, del video de Axelis): umbrales, sala junto al elevador, tarima
# umbral dorado en cada puerta de los arcos (alternando bloque de oro y moldura), igual de los dos lados
def threshold(x, z0, z1, pattern):
    for i, z in enumerate(range(z0, z1 + 1)):
        o.set(x, 0, z, GB if pattern[i] == 'B' else GT)
for x in (22, 38):
    threshold(x, 14, 17, 'TBBT')            # espectáculo / plinko
    threshold(x, 29, 35, 'BTBTBTB')         # bar / tragamonedas
    threshold(x, 44, 47, 'TBBT')            # caja / premios
for x in range(27, 34):                     # entrada principal
    o.set(x, 0, 50, GB if x in (27, 33) else 'casino:casino_floor_royal')
for x in list(range(23, 26)) + list(range(35, 38)):
    o.set(x, 0, 22, GB)                     # cierre dorado del rellano hacia el salón
o.set(27, 0, 23, BM); o.set(33, 0, 23, BM)
# salita a los lados del elevador: alfombra, sofás contra la pared y mesitas
for (sx, side) in ((23, 1), (37, -1)):
    for k in range(5):
        x = sx + side * k
        for z in range(10, 14):
            o.set(x, 0, z, 'casino:casino_floor_royal')
    for k in range(4):
        o.set(sx + side * k, 1, 10, sofa('red', 'south'))
    for z in (11, 12, 13):
        o.set(sx + side * 4, 1, z, sofa('red', 'west' if side == 1 else 'east'))
    for k in (1, 2):
        for z in (12, 13):
            o.set(sx + side * k, 1, z, TABLE_B)
# tarima de la rueda: también la fila de atrás (z 25) en mármol negro, con oro en las puntas
for x in range(27, 34):
    o.set(x, 1, 25, BM)
o.set(26, 1, 25, GB); o.set(34, 1, 25, GB)
# moldura dorada arriba de los muros (y 9) también sobre las puertas, en el telón del show y sobre las marquesinas de caja/premios
for x in (22, 38):
    for z in range(IZ0, IZ1 + 1):
        if o.get(x, CY - 1, z).startswith('casino:velvet_wall'):
            o.set(x, CY - 1, z, GT)
for z in range(10, 22):
    o.set(6, CY - 1, z, GT)
for z in range(LZ0, LZ1 + 1):
    o.set(9, CY - 1, z, GT); o.set(51, CY - 1, z, GT)
# el candelabro frente al elevador chocaba con el letrero VIP: un bloque más abajo, colgado de una moldura
for (x, y, z), st in list(o.b.items()):
    if 'grand_chandelier' in st and y - o.oy == CY - 2 and (x, z) == (30, 18):
        o.set(30, CY - 2, 18, AIR); o.set(30, CY - 3, 18, st); o.set(30, CY - 1, 18, GT)
# rasca y gana junto a la puerta del elevador (planta baja), con postes de cordón a los lados de la puerta
mq.place(o, 'scratch', 27, 1, 14, 3)
mq.place(o, 'scratch', 33, 1, 14, 1)
o.set(27, 1, 15, rope('black')); o.set(33, 1, 15, rope('black'))
o.set(26, 0, 16, 'casino:casino_floor_royal'); o.set(34, 0, 16, 'casino:casino_floor_royal'); o.set(28, 0, 17, WM)
for x in (24, 25, 35, 36):
    for z in (12, 13):
        o.set(x, 2, z, drinks(1, 'south'))
o.set(27, 4, 10, GT); o.set(33, 4, 10, GT)
# sótano: el hueco detrás del elevador se cierra con columnas, las esquinas del hueco son pilastras de cuarzo y una moldura corre arriba
for x in (28, 32):
    for y in range(YB + 1, -2):
        h = y - YB
        o.set(x, y, 10, MAH if h <= 3 else (GT if h in (4, 9) else 'casino:velvet_wall_black'))
        o.set(x, y, 11, GB if h == 1 else (GT if h == 9 else QP))
    o.set(x, -3, 12, GT)
for x in list(range(25, 28)) + list(range(33, 36)):
    o.set(x, -3, 11, GT)
for x in list(range(24, 29)) + list(range(32, 37)):
    o.set(x, YB, 10, BM)
o.set(29, YB, 15, WM); o.set(31, YB, 15, WM)

# sillas de tragamonedas frente a cada máquina (la silla de casino queda para las carreras)
for x in SLOT_X:
    # la silla mira hacia la máquina (quien se sienta mira hacia "facing")
    o.set(x, 1, 25, 'casino:slot_chair[facing=south]'); o.set(x, 1, 28, 'casino:slot_chair[facing=north]')
    o.set(x, 1, 36, 'casino:slot_chair[facing=south]'); o.set(x, 1, 39, 'casino:slot_chair[facing=north]')

# ================================================================== CUADROS (casino:casino_painting, 2x2 bloques centrados en su bloque)
PAINT = {'diamond': 0, 'derby': 1, 'roulette': 2, 'royal_flush': 3, 'dice': 4, 'chips': 5, 'vegas': 6, 'cocktail': 7}
_FD = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}
PAINTINGS_OK = []
# hacia dónde corre el cuadro la variante "shift" (el +x del modelo, girado según hacia dónde mira)
_SHIFT_DIR = {'north': (1, 0), 'south': (-1, 0), 'east': (0, 1), 'west': (0, -1)}
def painting(x, y, z, facing, art, shift=False):
    """Cuelga un cuadro mirando a 'facing' (hacia la sala), pegado a la pared de atrás. Quita apliques que estorben (también el de
    justo encima). Con shift el cuadro queda corrido medio bloque (ver painting_at)."""
    dx, dz = _FD[facing]
    lx, lz = (1, 0) if dx == 0 else (0, 1)
    if shift:
        sx_, sz_ = _SHIFT_DIR[facing]
        ks = (0, 1) if (sx_ + sz_) > 0 else (-1, 0)
    else:
        ks = (-1, 0, 1)
    cells = [(x + lx * k, yy, z + lz * k) for k in ks for yy in (y, y + 1)]
    for (cx_, cy_, cz_) in cells:
        st = o.get(cx_, cy_, cz_)
        if not (st == AIR or st.startswith('minecraft:light') or st.startswith('minecraft:end_rod')):
            raise SystemExit(f'cuadro en {x},{y},{z}: estorba {st} en {cx_},{cy_},{cz_}')
        wall = o.get(cx_ - dx, cy_, cz_ - dz)
        if wall == AIR:
            raise SystemExit(f'cuadro en {x},{y},{z}: no hay pared detrás en {cx_ - dx},{cy_},{cz_ - dz}')
    for (cx_, cy_, cz_) in cells + [(a, b + 2, c) for (a, b, c) in cells]:
        if o.get(cx_, cy_, cz_).startswith('minecraft:end_rod'):
            o.set(cx_, cy_, cz_, AIR)
    o.set(x, y, z, f'casino:casino_painting[design={PAINT[art]},facing={facing},shift={"true" if shift else "false"}]')
    PAINTINGS_OK.append((x, y, z))

def painting_at(c, other, y, facing, art):
    """Cuadro centrado en la coordenada c a lo largo de la pared (c = n + 0.5 es el centro del bloque n; un número entero es la
    junta entre dos bloques, y entonces se usa la variante corrida medio bloque). other = la otra coordenada (z o x)."""
    along_x = facing in ('north', 'south')
    if abs(c - round(c)) == 0.5:
        p, sh = int(c - 0.5), False
    else:
        sx_, sz_ = _SHIFT_DIR[facing]
        p, sh = (int(c) - 1 if (sx_ + sz_) > 0 else int(c)), True
    if along_x:
        painting(p, y, other, facing, art, sh)
    else:
        painting(other, y, p, facing, art, sh)
# ---- ajustes que Axelis hizo a mano (pilastras del sótano, letrero VIP BAR con luz, apliques, techo junto al elevador...)
from ajustes_axelis import AXELIS_PATCH, AXELIS_PATCH_PH
for (x, y, z, st) in AXELIS_PATCH:
    o.set(x, y, z, st)
# ---- penthouse y azotea con los cambios del 15 nov (ver ajustes_axelis.py)
for (x, y, z, st) in AXELIS_PATCH_PH:
    o.set(x, y, z, st)
for z in (21, 23, 25, 27):
    o.set(SX0 + 1, Y_PH_TOP + 1, z, stool('purple', 'east'))           # taburetes del lado de la pared, mirando a la barra
for x in (CX - 6, CX + 6):
    o.set(x, Y_PH_TOP, 29, 'casino:marble_black')                       # la orilla dorada de la pista, igual de corta en los dos lados
# donde estaba la recámara (noroeste): un rincón chico para platicar, en espejo con el jacuzzi del otro lado: tapete de 5 x 5 con
# orilla dorada, dos sofás frente a frente, mesita con bebidas, lámparas de pie y plantas
RX, RZ = 23, 16
for x in range(RX - 2, RX + 3):
    for z in range(RZ - 2, RZ + 3):
        o.set(x, Y_PH, z, GT if abs(x - RX) == 2 or abs(z - RZ) == 2 else 'casino:casino_floor_lounge')
for x in range(RX - 1, RX + 2):
    o.set(x, P, RZ - 2, sofa('purple', 'south')); o.set(x, P, RZ + 2, sofa('purple', 'north'))
o.set(RX, P, RZ, TABLE_B); o.set(RX, P + 1, RZ, drinks(2))
for x in (RX - 2, RX + 2):
    o.set(x, P, RZ - 2, 'minecraft:potted_azalea_bush'); o.set(x, P, RZ + 2, 'minecraft:potted_azalea_bush')
    o.set(x, P, RZ, 'minecraft:dark_oak_fence'); o.set(x, P + 1, RZ, 'minecraft:end_rod[facing=up]')
# los apliques que quitó en la planta baja se quitan en todo el casino por igual (lado espejo incluido)
for (x, y, z, st) in AXELIS_PATCH:
    if y > 0 and st == 'minecraft:air' and o.get(60 - x, y, z).startswith('minecraft:end_rod'):
        o.set(60 - x, y, z, AIR)
for x in (21, 39):          # y en el bar y los slots, también del otro lado del espejo (z 26 = espejo de z 38)
    if o.get(x, 6, 38) == AIR and o.get(x, 6, 26).startswith('minecraft:end_rod'):
        o.set(x, 6, 26, AIR)

# ---- planta baja: cuadros un bloque más arriba (y 6), en pares simétricos
painting(6, 6, 32, 'east', 'cocktail')          # bar, pared del fondo
painting(54, 6, 32, 'west', 'chips')            # tragamonedas, pared del fondo
painting_at(14.0, 42, 6, 'south', 'royal_flush')   # caja (centrado en su paño, x 12..15)
painting_at(14.0, 49, 6, 'north', 'diamond')
painting_at(47.0, 42, 6, 'south', 'roulette')      # premios (espejo: x 45..48)
painting_at(47.0, 49, 6, 'north', 'dice')
painting(23, 6, 23, 'east', 'vegas')            # salón de la rueda: entre el show y el bar / el plinko y los slots
painting(37, 6, 23, 'west', 'vegas')
painting_at(40.0, 23, 6, 'east', 'diamond')       # entre el bar y la caja / los slots y premios (paño z 37..42)
painting_at(40.0, 37, 6, 'west', 'diamond')
# ---- detrás de la rueda, a los lados: dos marcos dorados con pantalla
for (x0, scr) in ((26, 0), (32, 0)):
    for x in range(x0, x0 + 3):
        for y in (2, 3, 4):
            edge_x = x in (x0, x0 + 2); edge_y = y in (2, 4)
            st = GB if (edge_x and edge_y) else (GT if (edge_x or edge_y) else f'casino:wall_screen[design={scr},facing=south]')
            o.set(x, y, 25, st)
    o.set(x0 + 1, 5, 25, 'minecraft:end_rod[facing=up]')
# ---- plinko: al fondo del pasillo, la pantalla del premio (4 x 3) con marco dorado; macetones en las esquinas y bancas al centro
for z in range(14, 18):
    for x in range(47, IX1 + 1):
        o.set(x, 0, z, 'casino:casino_floor_galaxy')
    o.set(IX1, 1, z, GB); o.set(IX1, 5, z, GT)
    for y in (2, 3, 4):
        o.set(IX1, y, z, f'casino:wall_screen[design={0 if y == 3 else 1},facing=west]')
for z in (13, 18):
    for y in range(1, 6):
        o.set(IX1, y, z, GB if y in (1, 5) else QP)
for (x, z) in ((39, 10), (IX1, 10), (39, 21), (IX1, 21)):
    o.set(x, 1, z, GB); o.set(x, 2, z, 'minecraft:flowering_azalea_leaves[persistent=true,distance=1]')
for (x, z) in ((40, 10), (39, 11), (53, 10), (IX1, 11), (40, 21), (39, 20), (53, 21), (IX1, 20)):
    o.set(x, 1, z, 'minecraft:potted_fern')
for x in (46, 47):
    o.set(x, 1, 10, sofa('purple', 'south')); o.set(x, 1, 21, sofa('purple', 'north'))
# ---- sótano: mesa privada de póker para VIP entre el bar y el elevador, y vitrina del diamante con cordones en rombo
mq.place(o, 'poker', 23, YB + 1, 13, 2)
for (x, z) in ((34, 12), (36, 12), (35, 11), (35, 13)):
    o.set(x, YB + 1, z, rope('purple'))
o.set(35, YB + 1, 12, GB); o.set(35, YB + 2, 12, 'minecraft:diamond_block')
for x in (33, 37):
    o.set(x, YB + 1, 10, 'minecraft:potted_azalea_bush')
for x in range(15, 19):
    o.set(x, YB + 1, 10, sofa('purple', 'south'))     # sofá contra la pared norte, centrado bajo el cuadro (x 15..18)
# ---- primer piso: cuadros en la pared norte, a los lados del elevador
painting(24, Y_VIP + 5, TZ0 + 1, 'south', 'royal_flush')
painting(36, Y_VIP + 5, TZ0 + 1, 'south', 'roulette')
# ---- sótano: cuadros
painting_at(17.0, 10, YB + 4, 'south', 'derby')   # sobre el sofá de la pared norte (paño x 14..19)
painting(30, YB + 6, 16, 'south', 'diamond')   # sobre la puerta del elevador
painting_at(22.0, 37, YB + 6, 'west', 'derby')   # pared de la sala de carreras, del lado del salón (paño z 20..23)
for (x, art) in ((40, 'derby'), (44, 'vegas'), (49, 'vegas'), (53, 'derby')):   # sala de carreras, pared del fondo: espejo en 46.5 (x <-> 93 - x)
    painting(x, YB + 6, QZ0 + 1, 'south', art)
painting(24, YB + 5, FZ, 'north', 'roulette')   # sobre la chimenea

# ================================================================== PANTALLAS DE RÉCORDS (casino:record_screen)
# Varias juntas forman una pantalla grande que va cambiando de canal (los que más ganan, apuestan, pierden...).
def record_screen(x0, x1, y0, y1, z, facing):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            o.set(x, y, z, f'casino:record_screen[facing={facing}]')
# planta baja: sobre la puerta del elevador, de frente a la entrada (se ve al entrar, por encima de la ruleta). El candelabro grande
# que colgaba enfrente la tapaba: en su lugar va uno chico pegado al techo, más arriba que la vista hacia la pantalla.
for (x, y, z), st in list(o.b.items()):
    if 'grand_chandelier' in st and (x, z) == (30, 18) and y - o.oy < Y_ROOF:
        o.set(30, y - o.oy, 18, AIR)
o.set(30, CY - 1, 18, CHAND)
record_screen(28, 32, 6, 8, 16, 'south')
# piso de las mesas y penthouse: sobre la puerta del elevador, arriba del letrero
record_screen(28, 32, Y_VIP + 7, Y_VIP + 8, 16, 'south')
record_screen(28, 32, Y_PH + 5, Y_PH + 6, 16, 'south')

# ================================================================== ROBO A LA BÓVEDA (nivel Diamante)
# Caseta de servicio en el patio trasero (puerta de hierro: se abre con ganzúa durante el robo) -> escalera -> pasillo de servicio
# (y -12, por el lado este) -> zona de empleados (oficina del gerente, vestidores, escalera, comedor, seguridad) -> escalera con
# lector de tarjeta -> pasillo de láseres (y -24) -> antesala con guardias armados y dos lectores -> puerta redonda -> bóveda.
YS, YV = YB, -24
SOLID = ('minecraft:dirt', 'minecraft:stone')
CONC, CONC_L = 'minecraft:gray_concrete', 'minecraft:light_gray_concrete'
STEEL = 'minecraft:iron_block'
TILES = 'minecraft:polished_deepslate'

def carve(cells, wall, floor=None):
    """Vacía las celdas (x, y, z) y forra con `wall` las caras que tocan tierra o piedra; `floor` va abajo de cada celda."""
    cells = set(cells)
    for c in cells:
        o.set(*c, AIR)
    for (x, y, z) in cells:
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0), (0, -1, 0)):
            n = (x + dx, y + dy, z + dz)
            if n not in cells and o.get(*n) in SOLID:
                o.set(*n, floor if (dy == -1 and floor) else wall)

def room(x0, x1, z0, z1, y0, y1):
    return [(x, y, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1) for y in range(y0, y1 + 1)]

def door(x, y, z, f, mat='minecraft:iron_door', hinge='left'):
    o.set(x, y, z, f'{mat}[facing={f},half=lower,hinge={hinge},open=false,powered=false]')
    o.set(x, y + 1, z, f'{mat}[facing={f},half=upper,hinge={hinge},open=false,powered=false]')

def cam(x, y, z, f): o.set(x, y, z, f'casino:security_camera[facing={f}]')

# ---- caseta de servicio (patio trasero, esquina noreste, pegada al podio)
for x in range(56, 61):
    for z in range(0, 11):
        o.set(x, 0, z, CONC)
        for y in range(1, 5):
            o.set(x, y, z, BSB if x in (56, 60) or z in (0, 10) else AIR)
        o.set(x, 5, z, slab('minecraft:polished_blackstone_brick_slab'))
door(56, 1, 3, 'east')
o.set(55, 4, 3, 'minecraft:lantern[hanging=false]') if False else None
o.set(56, 4, 5, 'minecraft:lantern[hanging=true]') if False else None
cam(55, 4, 5, 'west')                            # mira la puerta desde fuera
o.set(57, 4, 2, 'minecraft:sea_lantern'); o.set(59, 4, 8, 'minecraft:sea_lantern')
# escalera hacia el pasillo de servicio (baja hacia el sur, x 57..59)
stair_cells = []
for k in range(12):
    z = 5 + k
    for x in range(57, 60):
        for y in range(-k + 1, -k + 4):
            stair_cells.append((x, y, z))
for x in range(57, 60):
    for y in range(-11, -8):
        for z in range(17, 40):
            stair_cells.append((x, y, z))
carve([c for c in stair_cells if c[1] < 1], CONC_L, 'minecraft:polished_andesite')
for k in range(12):
    for x in range(57, 60):
        o.set(x, -k, 5 + k, stairs('minecraft:polished_andesite_stairs', 'north'))
for x in range(57, 60):
    for z in range(17, 40):
        o.set(x, YS, z, 'minecraft:polished_andesite')
for z in range(19, 40, 5):
    o.set(58, -8, z, 'minecraft:sea_lantern')
for k in range(0, 12, 4):
    o.set(58, -k + 4, 5 + k, 'minecraft:sea_lantern') if -k + 4 < 0 else None
cam(57, -9, 18, 'south'); cam(59, -9, 38, 'north')

# ---- zona de empleados (y -12): pasillo z 37..39 y cuartos z 41..49, simétrica respecto a x=30
carve(room(6, 54, 37, 49, -11, -8) + room(55, 56, 37, 39, -11, -9), CONC_L, TILES)
for x in range(6, 55):
    for z in range(37, 50):
        o.set(x, YS, z, TILES if 37 <= z <= 39 else CONC)
for x in range(55, 57):
    for z in range(37, 40):
        o.set(x, YS, z, TILES)
for x in range(6, 55):                                         # muro entre el pasillo y los cuartos
    for y in range(-11, -7):
        o.set(x, y, 40, CONC_L)
for wx in (16, 25, 35, 44):                                     # muros entre cuartos
    for y in range(-11, -7):
        for z in range(41, 50):
            o.set(wx, y, z, CONC_L)
for dx, mat in ((11, 'minecraft:dark_oak_door'), (21, 'minecraft:spruce_door'), (39, 'minecraft:spruce_door'), (49, 'minecraft:dark_oak_door')):
    door(dx, -11, 40, 'south', mat)
door(30, -11, 40, 'south')                                     # escalera a la bóveda: puerta de hierro con lector
o.set(32, -10, 39, 'casino:card_reader[facing=north,lit=false]')
for x in range(8, 54, 6):                                      # luces del pasillo
    o.set(x, -7, 38, 'minecraft:sea_lantern')
for x in range(6, 55):                                         # zócalo oscuro en el pasillo
    o.set(x, -11, 36, 'minecraft:polished_blackstone_bricks'); o.set(x, -11, 40, 'minecraft:polished_blackstone_bricks') if o.get(x, -11, 40) == CONC_L else None
cam(7, -8, 37, 'east'); cam(53, -8, 37, 'west')
# oficina del gerente (x 6..15): piso de caoba, escritorio, caja fuerte en el muro oeste, librero, cuadro
for x in range(6, 16):
    for z in range(41, 50):
        o.set(x, YS, z, 'minecraft:dark_oak_planks')
        for y in range(-11, -7):
            if x == 6 or z == 49:
                o.set(x, y, z, MAH if y < -9 else 'casino:velvet_wall_black')
for x in range(9, 13):
    o.set(x, -11, 46, 'minecraft:dark_oak_slab[type=top]')
o.set(10, -10, 46, 'casino:clue_note[facing=south]')
o.set(11, -11, 47, stairs('minecraft:dark_oak_stairs', 'north'))
o.set(6, -10, 44, 'casino:wall_safe[facing=east]')
for z in (42, 48):
    o.set(7, -11, z, 'minecraft:potted_azalea_bush')
for x in (13, 14, 15):
    for y in (-11, -10):
        o.set(x, y, 49, 'minecraft:bookshelf')
o.set(10, -7, 45, 'minecraft:sea_lantern')
cam(15, -8, 41, 'west')
# vestidores (x 17..24): casilleros, bancas y una nota
for z in range(42, 49):
    o.set(17, -11, z, 'minecraft:iron_block'); o.set(17, -10, z, 'minecraft:iron_trapdoor[facing=east,half=top,open=true]')
    o.set(24, -11, z, 'minecraft:iron_block'); o.set(24, -10, z, 'minecraft:iron_trapdoor[facing=west,half=top,open=true]')
for z in (44, 45, 46):
    o.set(20, -11, z, 'minecraft:spruce_slab[type=bottom]'); o.set(21, -11, z, 'minecraft:spruce_slab[type=bottom]')
o.set(23, -11, 48, 'minecraft:spruce_slab[type=top]'); o.set(23, -10, 48, 'casino:clue_note[facing=north]')
o.set(20, -7, 45, 'minecraft:sea_lantern')
# comedor (x 36..43), espejo de los vestidores: mesa, sillas, cafetera y otra nota
for z in range(42, 49):
    o.set(43, -11, z, 'minecraft:smooth_quartz'); o.set(36, -11, z, 'minecraft:smooth_quartz') if z in (42, 48) else None
o.set(39, -11, 45, TABLE_W); o.set(40, -11, 45, TABLE_W)
for x in (39, 40):
    o.set(x, -11, 44, stool('black', 'south')); o.set(x, -11, 46, stool('black', 'north'))
o.set(43, -10, 43, 'minecraft:brewing_stand'); o.set(43, -10, 47, 'minecraft:cake')
o.set(43, -10, 45, 'casino:clue_note[facing=west]')
o.set(40, -7, 45, 'minecraft:sea_lantern')
# seguridad (x 45..54): pared de monitores, consola de cámaras, silla
for x in range(45, 55):
    for z in range(41, 50):
        o.set(x, YS, z, 'minecraft:gray_concrete')
for x in range(47, 53):
    for y in (-10, -9):
        o.set(x, y, 49, 'casino:cctv_monitor[facing=north]')
for x in range(47, 53):
    o.set(x, -11, 48, 'minecraft:polished_blackstone_slab[type=top]')
o.set(50, -10, 48, 'casino:camera_console[facing=north]')
o.set(50, -11, 46, chair('black', 'south'))
o.set(50, -7, 45, 'minecraft:sea_lantern')
cam(45, -8, 41, 'east')

# ---- escalera de la bóveda (x 26..34, z 41..49): tramo A baja al sur (x 26..28), descanso, tramo B sube al norte (x 32..34)
cells = []
for k in range(6):
    for x in range(26, 29):
        for y in range(-12 - k, -8):
            cells.append((x, y, 42 + k))
for x in range(26, 35):
    for z in (48, 49):
        for y in range(-18, -15):
            cells.append((x, y, z))
for j in range(5):
    for x in range(32, 35):
        for y in range(-19 - j, -15):
            cells.append((x, y, 47 - j))
for x in range(26, 35):
    for z in (41, 42):
        for y in range(-23, -19):
            cells.append((x, y, z))
for x in range(29, 32):
    for z in range(33, 41):
        for y in range(-23, -19):
            cells.append((x, y, z))
carve(cells, CONC_L, TILES)
for k in range(6):
    for x in range(26, 29):
        o.set(x, -13 - k, 42 + k, stairs('minecraft:polished_deepslate_stairs', 'north'))
for x in range(26, 35):
    for z in (48, 49):
        o.set(x, -19, z, TILES)
for j in range(5):
    for x in range(32, 35):
        o.set(x, -20 - j, 47 - j, stairs('minecraft:polished_deepslate_stairs', 'south'))
for x in range(26, 35):
    for z in (41, 42):
        o.set(x, YV, z, TILES)
for x in range(26, 29):                                        # barandal del tramo A
    o.set(x, -11, 48, 'minecraft:polished_blackstone_wall')
for z in range(42, 48):
    o.set(29, -11, z, 'minecraft:polished_blackstone_wall')
o.set(30, -16, 49, 'minecraft:sea_lantern') if False else None
o.set(30, -15, 48, 'minecraft:sea_lantern'); o.set(30, -19, 41, 'minecraft:sea_lantern')

# ---- pasillo de láseres (x 29..31, z 33..40): emisores en el muro oeste, cada uno con su ritmo
for z in range(33, 41):
    for x in range(29, 32):
        o.set(x, YV, z, 'minecraft:polished_blackstone_bricks' if x == 30 else TILES)
    o.set(28, -22, z, STEEL); o.set(32, -22, z, STEEL)
for i, z in enumerate((34, 36, 38)):
    o.set(28, -23, z, f'casino:laser_emitter[facing=east,pattern={i % 4}]')
    o.set(28, -21, z, f'casino:laser_emitter[facing=east,pattern={(i + 2) % 4}]')
for z in (33, 40):
    o.set(30, -19, z, 'minecraft:sea_lantern')

# ---- dos salas iguales una encima de la otra (x 18..42, z 22..32, 6 de alto): mármol, pilastras doradas y cámaras.
#   y -24: SALA DE GUARDIAS (guardias armados; la compuerta del oeste se abre hackeando su consola)
#   y -36: ANTESALA de la bóveda (dos lectores y la puerta redonda en el muro norte)
YV2 = -36
def hall(fy, south_gap):
    carve(room(18, 42, 22, 32, fy + 1, fy + 6), BSB, WM)
    floor_pattern(18, 42, 22, 32, fy, WM, BM, 'diamond', 30, 27)
    for x in range(18, 43):
        o.set(x, fy + 7, 22, GT); o.set(x, fy + 7, 32, GT)
    for z in range(22, 33):
        o.set(17, fy + 1, z, MAH); o.set(43, fy + 1, z, MAH)
    for x in range(17, 44):
        if not (south_gap and 29 <= x <= 31):
            o.set(x, fy + 1, 33, MAH)
    for px in (18, 24, 36, 42):                                 # pilastras de cuarzo
        for y in range(fy + 1, fy + 7):
            o.set(px, y, 32, QP)
    for x in range(20, 42, 4):
        for z in (24, 30):
            o.set(x, fy + 7, z, 'minecraft:sea_lantern')
    cam(19, fy + 5, 32, 'north') if False else None
    cam(18, fy + 5, 31, 'east'); cam(42, fy + 5, 31, 'west'); cam(18, fy + 5, 23, 'east'); cam(42, fy + 5, 23, 'west')

hall(YV, True)
# compuerta del oeste (puerta de hierro) con su consola: hay que hackearla para bajar
door(17, YV + 1, 27, 'west')
o.set(18, YV + 1, 25, 'casino:camera_console[facing=east]')
o.set(18, YV + 4, 27, neon('!', 'east', 1)) if False else None
# escalera al oeste (x 16..6, baja 11) hasta un descanso (x 3..5, z 26..32) en y -36
cells = []
for k in range(11):
    x = 16 - k
    for z in range(26, 29):
        for y in range(YV - k, YV - k + 4):
            cells.append((x, y, z))
for x in range(3, 6):
    for z in range(26, 33):
        for y in range(YV2 + 1, YV2 + 5):
            cells.append((x, y, z))
# pasillo de láseres de abajo (x 6..17, z 30..32) hasta la antesala
for x in range(6, 18):
    for z in range(30, 33):
        for y in range(YV2 + 1, YV2 + 5):
            cells.append((x, y, z))
carve(cells, CONC_L, TILES)
for k in range(11):
    for z in range(26, 29):
        o.set(16 - k, YV - 1 - k, z, stairs('minecraft:polished_deepslate_stairs', 'east'))
for x in range(3, 6):
    for z in range(26, 33):
        o.set(x, YV2, z, TILES)
for x in range(6, 18):
    for z in range(30, 33):
        o.set(x, YV2, z, 'minecraft:polished_blackstone_bricks' if z == 31 else TILES)
    o.set(x, YV2 + 3, 29, STEEL); o.set(x, YV2 + 3, 33, STEEL)
o.set(4, YV2 + 5, 29, 'minecraft:sea_lantern'); o.set(11, YV2 + 5, 26, 'minecraft:sea_lantern')
for i, x in enumerate((7, 9, 11, 13, 15)):
    o.set(x, YV2 + 1, 29, f'casino:laser_emitter[facing=south,pattern={i % 4}]')
    o.set(x, YV2 + 2, 29, f'casino:laser_emitter[facing=south,pattern={(i + 2) % 4}]')
cam(6, YV2 + 4, 32, 'east')

hall(YV2, False)
for z in range(30, 33):                                         # entrada desde el pasillo de láseres
    for y in range(YV2 + 1, YV2 + 5):
        o.set(17, y, z, AIR)
# muro de la bóveda (z 20..21, acero) y la puerta redonda al centro
for x in range(17, 44):
    for y in range(YV2, YV2 + 8):
        for z in (20, 21):
            o.set(x, y, z, STEEL if (x + y) % 2 == 0 else 'minecraft:smooth_stone')
DY = YV2 + 3                                                     # centro de la puerta
for x in range(28, 33):
    for y in range(DY - 2, DY + 3):
        o.set(x, y, 21, 'casino:vault_seal'); o.set(x, y, 20, 'casino:vault_seal')
o.set(30, DY, 21, 'casino:vault_door[facing=south,open=false]')
o.set(24, YV2 + 2, 22, 'casino:card_reader[facing=south,lit=false]')
o.set(36, YV2 + 2, 22, 'casino:card_reader[facing=south,lit=false]')
for x in (24, 36):
    o.set(x, YV2 + 7, 23, 'minecraft:sea_lantern')

# ---- bóveda (x 20..40, z 8..19, y -36): carritos de botín, lingotes, cajas de seguridad y ductos de gas
carve(room(20, 40, 8, 19, YV2 + 1, YV2 + 5), STEEL, TILES)
for x in range(20, 41):
    for z in range(8, 20):
        o.set(x, YV2, z, TILES if (x + z) % 2 else 'minecraft:polished_deepslate')
        o.set(x, YV2 + 6, z, STEEL)
for x in range(20, 41):                                         # lingotes al fondo
    o.set(x, YV2 + 1, 8, GB); o.set(x, YV2 + 2, 8, GB) if x % 2 else None
for z in range(9, 19):
    for y in range(YV2 + 2, YV2 + 5):
        o.set(20, y, z, 'casino:deposit_boxes[facing=east]'); o.set(40, y, z, 'casino:deposit_boxes[facing=west]')
KINDS = ('cash', 'gold', 'diamond', 'chips')
i = 0
for z in (11, 14, 17):
    for x in (23, 26, 34, 37):
        o.set(x, YV2 + 1, z, f'casino:loot_cart[facing=south,kind={KINDS[i % 4]},left=3]')
        i += 1
for x in (24, 30, 36):
    for z in (10, 16):
        o.set(x, YV2 + 6, z, 'casino:gas_vent')
for x in (22, 38, 30):
    o.set(x, YV2 + 6, 13, 'minecraft:sea_lantern')

# ---- callejón detrás del casino (x 50..55, z 1..6): el contacto espera escondido entre botes de basura y cajas
for x in range(50, 56):
    for z in range(0, 8):
        o.set(x, 0, z, 'minecraft:gray_concrete' if (x + z) % 3 else 'minecraft:cobblestone')
for x in range(50, 56):                                          # muro del fondo y techito
    for y in range(1, 5):
        o.set(x, y, 0, BSB)
    o.set(x, 5, 0, slab('minecraft:polished_blackstone_brick_slab'))
for z in range(0, 5):                                            # muro oeste (tapa la vista desde el patio)
    for y in range(1, 5):
        o.set(50, y, z, BSB)
for x in range(51, 56):
    for z in range(1, 4):
        o.set(x, 4, z, 'minecraft:dark_oak_slab[type=top]')
o.set(51, 1, 1, 'minecraft:barrel[facing=up]'); o.set(51, 2, 1, 'minecraft:barrel[facing=up]'); o.set(52, 1, 1, 'minecraft:barrel[facing=up]')
o.set(51, 1, 3, 'minecraft:green_shulker_box') if False else o.set(51, 1, 3, 'minecraft:composter')
o.set(55, 1, 1, 'minecraft:spruce_trapdoor[facing=south,half=bottom,open=false]') if False else None
o.set(53, 3, 1, 'minecraft:lantern[hanging=false]') if False else o.set(53, 3, 1, 'minecraft:soul_lantern[hanging=false]')
o.set(55, 1, 5, 'minecraft:cobweb') if False else None

# ---- gente del robo: guardias (con táser arriba, armados abajo) y el contacto en el callejón
#   "heistguard dir tipo dx dz": patrulla entre su lugar y su lugar + (dx, dz); tipo 0 táser y porra, 1 pistola
for (x, y, z, kind, d, dx, dz) in ((10, -11, 38, 0, 3, 40, 0), (58, -11, 20, 0, 0, 0, 16), (50, -11, 45, 0, 2, 0, 0),
                                   (21, YV + 1, 27, 1, 3, 0, 4), (39, YV + 1, 27, 1, 1, 0, 4), (30, YV + 1, 24, 1, 2, 0, 0),
                                   (21, YV2 + 1, 27, 1, 3, 0, 4), (39, YV2 + 1, 27, 1, 1, 0, 4), (30, YV2 + 1, 30, 1, 0, 0, 0)):
    o.marker(x, y, z, f'heistguard {d} {kind} {dx} {dz}', [(0, 0, 0, 'minecraft:black_wool'), (0, 1, 0, 'minecraft:black_wool')])
o.marker(54, 1, 2, 'heistboss 2', [(0, 0, 0, 'minecraft:gray_wool'), (0, 1, 0, 'minecraft:gray_wool')])

# ---- luces escondidas (invisibles) en todo lo subterráneo para que no aparezcan monstruos en lo oscuro
for x in range(0, W, 6):
    for z in range(0, D, 6):
        for y in range(-B + 1, 0):
            if o.get(x, y, z) == AIR and o.get(x, y + 1, z) not in (AIR, None) and not o.get(x, y + 1, z).startswith('minecraft:light'):
                o.set(x, y, z, 'minecraft:light[level=11]')


# ================================================================== CLIENTES (gente que pasea, se sienta, ve las mesas y platica)
# Cada uno se queda a menos de 11 bloques de donde aparece (no cambian de piso). "patron dir piel".
PATRONS = [(26, 1, 36), (34, 1, 36), (26, 1, 21), (12, 1, 26), (18, 1, 37), (46, 1, 30), (46, 1, 19), (14, 1, 19), (30, 1, 45),
           (21, 1, 47), (39, 1, 47),                                   # planta baja (salón, bar, slots, plinko, show, caja y premios)
           (26, 13, 22), (34, 13, 26), (30, 13, 31),                   # piso de mesas
           (21, 13, 47), (39, 13, 47), (22, 13, 36),                   # terraza
           (25, 47, 24), (35, 47, 28),                                 # penthouse
           (20, -11, 24), (14, -11, 16), (44, -11, 28)]                # sótano (salón VIP y carreras)
# Nombre de cada uno (último número): las pieles pares son señores y las nones señoras; la 4 y la 11 son don y doña. Se reparten en
# orden dentro de cada lista para que en un mismo casino no se repita ningún nombre (ver DealerEntity.patronNameKey).
_names = {}
for i, (x, y, z) in enumerate(PATRONS):
    skin = (i * 5) % 12
    pool = ('o' if skin in (4, 11) else '') + ('m' if skin % 2 == 0 else 'f')
    n = _names.get(pool, 0)
    _names[pool] = n + 1
    o.marker(x, y, z, f'patron {i % 4} {skin} {n}', [(0, 0, 0, 'minecraft:light_blue_wool')])
assert _names.get('m', 0) <= 15 and _names.get('f', 0) <= 15 and _names.get('om', 0) <= 6 and _names.get('of', 0) <= 6, _names

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
