"""Gran Casino Aurora (el casino elegante: nieve y bosque oscuro).

Estilo art déco de lujo (Monte Carlo, el Diamond de GTA 5): podio de 2 pisos de cuarzo, mármol y oro con una torre encima.
  Planta baja (y 0):  vestíbulo con cajeros, atrio central de doble altura (ruleta, blackjack, póker) con escalera imperial,
                      ala oeste de tragamonedas, ala este con bar y escenario, rueda de la suerte al fondo.
  Entrepiso (y 10):   oeste: Hipódromo (pantalla gigante de carreras + asientos); este: salón VIP (guardia en la puerta);
                      balcones sobre el atrio.
  Azotea del podio (y 20): terraza con piscina y jardín alrededor de la torre.
  Torre: restaurante (y 20), suites (y 27), penthouse (y 34) y en el techo (y 41) la discoteca "Sky Club" al aire libre.
Entrada al sur (+z). Escalera de caracol en la torre que une el entrepiso con todos los pisos.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from bp import BP, st, DIRS, DX, DZ, opp, cw, ccw, AIR  # noqa: E402

W, H, D = 45, 52, 56
bp = BP(W, H, D)

# ---------------- paleta ----------------
QZ = "quartz_block"
SQZ = "smooth_quartz"
QP = st("quartz_pillar", axis="y")
CQZ = "chiseled_quartz_block"
GOLD = "gold_block"
TRIM = "casino:gold_trim"
MARB = "casino:marble"
MARBK = "casino:marble_black"
PANEL = "casino:mahogany_panel"
DOAK = "dark_oak_planks"
DSTONE = "polished_deepslate"
DTILE = "deepslate_tiles"
GLASS = "black_stained_glass"
GLASSL = "light_blue_stained_glass"
SEA = "sea_lantern"
GLOW = "glowstone"
ROYAL = "casino:casino_floor_royal"
ROYALC = "casino:casino_carpet_royal"
DECO = "casino:casino_floor_deco"
GALAXY = "casino:casino_floor_galaxy"
LOUNGE = "casino:casino_floor_lounge"
VEL_P = "casino:velvet_wall_purple"
VEL_R = "casino:velvet_wall_red"
VEL_K = "casino:velvet_wall_black"


def stairs(block, facing, half="bottom", shape="straight"):
    return st(block, facing=DIRS[facing], half=half, shape=shape)


def slab(block, t="bottom"):
    return st(block, type=t)


def pane(block, **conn):
    return st(block, **{k: "true" for k in conn})


def lantern(hanging=False):
    return st("lantern", hanging="true" if hanging else "false")


def chain():
    return st("chain|iron_chain", axis="y")


def chair(color, d):
    return st(f"casino:casino_chair_{color}", facing=DIRS[d])


def stool(color, d):
    return st(f"casino:bar_stool_{color}", facing=DIRS[d])


def sofa(color, d, left=False, right=False):
    return st(f"casino:sofa_{color}", facing=DIRS[d], left=left, right=right)


def counter(d, shape="straight"):
    return st("casino:bar_counter", facing=DIRS[d], shape=shape)


def neon(ch, d, color=2):
    chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?$&♥♠♦♣★- "
    return st("casino:neon_letter", facing=DIRS[d], glyph=chars.index(ch), color=color)


def sign(design, d):
    return st("casino:neon_sign", facing=DIRS[d], design=design)


def screen(design, d):
    return st("casino:wall_screen", facing=DIRS[d], design=design)


def rope(color, n=False, e=False, s=False, w=False):
    return st(f"casino:rope_post_{color}", north=n, east=e, south=s, west=w)


def drinks(n, d=0):
    return st("casino:drinks", facing=DIRS[d], drinks=n)


def speaker(d):
    return st("casino:speaker", facing=DIRS[d])


def sofa_row(color, x0, z0, x1, z1, d, y):
    """Sofá recto que se une (brazos en las puntas). d = hacia donde mira el frente."""
    pts = [(x, z) for x in range(min(x0, x1), max(x0, x1) + 1) for z in range(min(z0, z1), max(z0, z1) + 1)]
    # la izquierda de quien está sentado mirando a d: dirección ccw(d)... el brazo izquierdo va en la punta hacia ccw(d) desde el que mira
    # quien se sienta mira hacia d; su izquierda es ccw(d)
    lx, lz = DX[ccw(d)], DZ[ccw(d)]
    for (x, z) in pts:
        left = (x + lx, z + lz) not in pts
        right = (x - lx, z - lz) not in pts
        bp.set(x, y, z, sofa(color, d, left, right))


# ---------------- máquinas (huella, igual que CasinoPlan.cells) ----------------
BJ_CELLS = [(-2, 0), (-1, 0), (0, 0), (1, 0), (2, 0), (-2, 1), (-1, 1), (0, 1), (1, 1), (2, 1), (-1, 2), (0, 2), (1, 2)]
RL_CELLS = [(c, r) for r in (-1, 0, 1) for c in range(-3, 4)]


def right_of(d):
    return (d + 3) & 3


def cells(kind, x, y, z, d):
    r = right_of(d)
    if kind in ("BLACKJACK", "POKER"):
        return [(x + DX[r] * c + DX[d] * w, y, z + DZ[r] * c + DZ[d] * w) for c, w in BJ_CELLS]
    if kind == "ROULETTE":
        return [(x + DX[r] * c + DX[d] * w, y, z + DZ[r] * c + DZ[d] * w) for c, w in RL_CELLS]
    if kind in ("SLOT", "SCRATCH"):
        return [(x, y, z), (x, y + 1, z)]
    if kind == "CASHIER":
        return [(x, y, z)]
    return [(x + DX[r] * (px - 1), y + py, z + DZ[r] * (px - 1)) for px in range(3) for py in range(4)]


def machine(kind, x, y, z, d):
    for c in cells(kind, x, y, z, d):
        bp.set(*c, AIR)  # sitio libre (la máquina la pone el mod)
    bp.machine(kind, x, y, z, d)


# =====================================================================
# MEDIDAS
# =====================================================================
X0, X1 = 2, 42          # muros del podio
Z0, Z1 = 2, 42          # Z1 = fachada (sur)
F1, F2, F3 = 0, 10, 20  # pisos: planta baja, entrepiso, azotea del podio
AX0, AX1, AZ0, AZ1 = 15, 29, 14, 35   # atrio de doble altura (aire de y1 a y19)
TX0, TX1, TZ0, TZ1 = 11, 33, 9, 31    # torre
TF = [20, 27, 34, 41]                 # pisos de la torre (el último es el techo con la discoteca)
CX = 22                               # eje de la entrada


# =====================================================================
# TERRENO Y PLAZA
# =====================================================================
def plaza():
    # base de piedra en toda la huella
    bp.fill(0, 0, 0, W - 1, 0, D - 1, "polished_andesite")
    # bordes de pasto/abeto alrededor
    for x in range(W):
        for z in range(D):
            edge = x <= 0 or x >= W - 1 or z <= 0 or z >= D - 1
            if edge:
                bp.set(x, 0, z, "grass_block")
    # camino de mármol con borde negro hasta la puerta
    for z in range(Z1 + 1, D):
        for x in range(CX - 3, CX + 4):
            b = MARBK if x in (CX - 3, CX + 3) else MARB
            bp.set(x, 0, z, b)
    # alfombra roja con postes de cordón
    for z in range(Z1 + 1, D - 1):
        for x in range(CX - 1, CX + 2):
            bp.set(x, 1, z, "red_carpet")
        for x in (CX - 2, CX + 2):
            if z % 2 == 1:
                bp.set(x, 1, z, rope("red", n=z > Z1 + 2, s=z < D - 3))
    # fuentes gemelas a los lados del camino
    for fx in (9, 35):
        fountain(fx, 49)
    # faroles en la plaza
    for x in (CX - 5, CX + 5):
        for z in (45, 50, 54):
            lamp_post(x, z)
    # abetos en macetas en las esquinas de la plaza
    for (x, z) in ((2, 45), (2, 53), (42, 45), (42, 53)):
        planter_tree(x, z)
    # marquesina (porte-cochère) sobre la entrada
    canopy()


def fountain(cx, cz):
    r = 4
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            dx, dz = x - cx, z - cz
            dd = dx * dx + dz * dz
            if dd <= r * r:
                if dd >= (r - 1) * (r - 1) + 1:
                    bp.set(x, 1, z, slab("smooth_quartz_slab"))
                    bp.set(x, 0, z, SQZ)
                else:
                    bp.set(x, 0, z, "water")
                    bp.set(x, -0 + 0, z, "water")
    # pilar central con tazón y chorro
    bp.set(cx, 0, cz, QP)
    bp.set(cx, 1, cz, QP)
    bp.set(cx, 2, cz, QP)
    for d in range(4):
        bp.set(cx + DX[d], 3, cz + DZ[d], stairs("quartz_stairs", opp(d), "top"))
    bp.set(cx, 3, cz, GOLD)
    bp.set(cx, 4, cz, "water")
    for d in range(4):
        bp.set(cx + DX[d] * 2, 1, cz + DZ[d] * 2, AIR)


def lamp_post(x, z):
    bp.set(x, 1, z, "polished_blackstone_wall|polished_blackstone_wall")
    bp.set(x, 2, z, "polished_blackstone_wall")
    bp.set(x, 3, z, "polished_blackstone_wall")
    bp.set(x, 4, z, GOLD)
    bp.set(x, 5, z, lantern())


def planter_tree(x, z):
    bp.set(x, 1, z, "spruce_trapdoor[facing=north,half=bottom,open=false]")
    bp.set(x, 1, z, st("dark_oak_log", axis="y"))
    for y in range(2, 6):
        bp.set(x, y, z, st("spruce_log", axis="y"))
    for (r, y) in ((2, 3), (2, 4), (1, 5), (1, 6), (0, 7)):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if abs(dx) + abs(dz) <= r + (1 if r == 2 else 0) and (dx, dz) != (0, 0) or (r == 0):
                    if 0 <= x + dx < W and 0 <= z + dz < D:
                        if bp.get(x + dx, y, z + dz) is None:
                            bp.set(x + dx, y, z + dz, st("spruce_leaves", persistent="true"))


def canopy():
    """Marquesina dorada sobre la puerta: 9 de ancho, sale 6 bloques, con columnas y luces colgando."""
    z0, z1 = Z1 + 1, Z1 + 6
    x0, x1 = CX - 5, CX + 5
    y = 7
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z == z1
            bp.set(x, y, z, TRIM if edge else SQZ)
            bp.set(x, y + 1, z, slab("smooth_quartz_slab") if not edge else stairs("quartz_stairs", 2 if z == z1 else (3 if x == x0 else 1)))
    for x in (x0, x1):
        for yy in range(1, y):
            bp.set(x, yy, z1, QP)
        bp.set(x, 1, z1, CQZ)
    # luces empotradas
    for x in range(x0 + 2, x1 - 1, 3):
        for z in (z0 + 1, z0 + 4):
            bp.set(x, y, z, SEA)
    # letrero de neón "GRAN CASINO" mirando al sur sobre la marquesina
    word = "AURORA"
    xs = CX - len(word) // 2
    for i, ch in enumerate(word):
        bp.set(xs + i, y + 2, z1 - 1, neon(ch, 2, 2))
        bp.set(xs + i, y + 1, z1 - 1, GOLD)


# =====================================================================
# PODIO: CASCARÓN
# =====================================================================
def shell():
    # piso de la planta baja y del entrepiso, techo del podio
    bp.fill(X0, F1, Z0, X1, F1, Z1, MARB)
    bp.fill(X0, F2, Z0, X1, F2, Z1, SQZ)
    bp.fill(X0, F3, Z0, X1, F3, Z1, SQZ)
    # aire interior
    bp.fill(X0 + 1, 1, Z0 + 1, X1 - 1, F2 - 1, Z1 - 1, AIR)
    bp.fill(X0 + 1, F2 + 1, Z0 + 1, X1 - 1, F3 - 1, Z1 - 1, AIR)
    # atrio de doble altura
    bp.fill(AX0, F2, AZ0, AX1, F2, AZ1, AIR)
    # muros: zócalo de deepslate, cuerpo de cuarzo, pilastras cada 4 con oro
    for y in range(0, F3 + 1):
        for x in range(X0, X1 + 1):
            for z in (Z0, Z1):
                bp.set(x, y, z, facade(x, y, z, along_x=True))
        for z in range(Z0, Z1 + 1):
            for x in (X0, X1):
                bp.set(x, y, z, facade(x, y, z, along_x=False))
    # cornisa del podio y antepecho de la azotea
    for x in range(X0 - 1, X1 + 2):
        for z in (Z0 - 1, Z1 + 1):
            bp.set(x, F3, z, stairs("quartz_stairs", 0 if z == Z0 - 1 else 2, "top"))
    for z in range(Z0 - 1, Z1 + 2):
        for x in (X0 - 1, X1 + 1):
            bp.set(x, F3, z, stairs("quartz_stairs", 3 if x == X0 - 1 else 1, "top"))
    for x in range(X0, X1 + 1):
        for z in (Z0, Z1):
            bp.set(x, F3 + 1, z, parapet(x))
    for z in range(Z0, Z1 + 1):
        for x in (X0, X1):
            bp.set(x, F3 + 1, z, parapet(z))
    # puerta principal: 5 de ancho, 6 de alto, arco de oro
    for x in range(CX - 2, CX + 3):
        for y in range(1, 7):
            bp.set(x, y, Z1, AIR)
    for x in range(CX - 3, CX + 4):
        bp.set(x, 7, Z1, TRIM)
    for y in range(1, 7):
        bp.set(CX - 3, y, Z1, GOLD)
        bp.set(CX + 3, y, Z1, GOLD)
    for x in range(CX - 2, CX + 3):
        bp.set(x, 6, Z1, stairs("quartz_stairs", 2, "top"))
    # puertas de vidrio decorativas abiertas a los lados (paneles)
    # reloj art déco sobre la puerta (rosetón)
    for x in range(CX - 2, CX + 3):
        bp.set(x, 8, Z1, CQZ)
    bp.set(CX, 9, Z1, GOLD)


def parapet(i):
    return QP if i % 4 == 0 else "quartz_bricks"


def facade(x, y, z, along_x):
    i = x if along_x else z
    corner = (x in (X0, X1)) and (z in (Z0, Z1))
    if y == 0:
        return DTILE
    if corner:
        return QP if y % 10 else CQZ
    if y in (1, F2, F3):
        return DSTONE if y == 1 else TRIM
    if i % 4 == 2:
        return QP if y not in (9, 19) else CQZ
    # ventanales altos (y 3..8 y 12..18) con travesaño de oro
    if 3 <= y <= 8 or 12 <= y <= 18:
        if y in (5, 15):
            return TRIM
        return GLASSL if along_x else GLASSL
    return QZ


# =====================================================================
# PLANTA BAJA
# =====================================================================
def ground_floor():
    # pisos: vestíbulo de mármol en damero, atrio con alfombra real, alas con piso deco
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            if z >= 36:
                b = MARB if (x + z) % 2 == 0 else MARBK
            elif AX0 <= x <= AX1 and AZ0 <= z <= AZ1:
                b = ROYAL
            elif x < AX0:
                b = DECO
            elif x > AX1:
                b = DOAK
            else:
                b = MARB
            bp.set(x, F1, z, b)
    # camino de mármol con borde de oro desde la puerta hasta la rueda
    for z in range(4, Z1):
        for x in (CX - 1, CX, CX + 1):
            if z >= 36 or z < AZ0:
                bp.set(x, F1, z, MARB)
    # columnas del atrio (cuarzo con capitel de oro) en las esquinas y lados
    for (x, z) in ((AX0 - 1, AZ0 - 1), (AX1 + 1, AZ0 - 1), (AX0 - 1, AZ1 + 1), (AX1 + 1, AZ1 + 1),
                   (AX0 - 1, 21), (AX1 + 1, 21), (AX0 - 1, 28), (AX1 + 1, 28)):
        for y in range(1, F2):
            bp.set(x, y, z, QP)
        bp.set(x, F2 - 1, z, GOLD)
        bp.set(x, 1, z, CQZ)
    # techo de las alas (debajo del entrepiso): artesonado con luces
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            if bp.get(x, F2, z) == AIR:
                continue
            if (x % 4 == 0) and (z % 4 == 0):
                bp.set(x, F2, z, SEA)
            elif x % 4 == 0 or z % 4 == 0:
                bp.set(x, F2, z, TRIM)
    lobby()
    slots_wing()
    bar_wing()
    atrium()
    back_zone()


def lobby():
    # mostradores de cajero a los lados de la entrada (mirando al centro)
    for x, d in ((15, 1), (29, 3)):
        machine("CASHIER", x, 1, 39, d)
        for z in (37, 38, 40):
            bp.set(x, 1, z, counter(d))
        bp.set(x - DX[d], 1, 37, PANEL)
        bp.set(x - DX[d], 2, 37, sign(1, d))
    # rasca y gana junto a los cajeros
    machine("SCRATCH", 12, 1, 40, 0)
    machine("SCRATCH", 32, 1, 40, 0)
    # macetas con abetos chicos y bancas
    for x in (4, 40):
        for z in (37, 40):
            bp.set(x, 1, z, "potted_spruce_sapling")
    for x in range(5, 9):
        bp.set(x, 1, 39, sofa("red", 0, x == 5, x == 8)) if False else None
    sofa_row("red", 5, 40, 8, 40, 0, 1)
    sofa_row("red", 36, 40, 39, 40, 0, 1)
    for x in (6, 38):
        bp.set(x, 1, 38, "casino:cocktail_table")
        bp.set(x, 2, 38, drinks(2))
    # letrero de neón "CASINO" sobre la puerta por dentro
    for i, ch in enumerate("CASINO"):
        bp.set(CX + 2 - i, 9, Z1 - 1, neon(ch, 0, 2))
    # postes con cordón en el vestíbulo
    for z in (37, 39):
        for x in (CX - 2, CX + 2):
            bp.set(x, 1, z, rope("red", n=z == 39, s=z == 37))
    bp.set(CX - 2, 1, 38, rope("red", n=True, s=True)) if False else None


def slots_wing():
    # filas de tragamonedas: contra la pared (mirando al este), isla doble y fila del borde (mirando al oeste)
    for z in range(5, 34):
        if z % 6 == 4:  # pasillos cada 6
            continue
        machine("SLOT", 3, 1, z, 1)
        machine("SLOT", 8, 1, z, 3)
        machine("SLOT", 9, 1, z, 1)
    # plinkos al fondo del ala
    machine("PLINKO", 6, 1, 3 + 0, 2) if False else None
    machine("PLINKO", 12, 1, 4, 3)
    # taburetes delante de cada tragamonedas de la pared
    # lámparas de piso y letreros sobre las filas
    for z in (7, 13, 19, 25, 31):
        bp.set(3, 3, z, sign((z // 6) % 12, 1))
    # bancas contra el atrio
    for z in (16, 24):
        bp.set(13, 1, z, chair("red", 3))
        bp.set(13, 1, z + 1, chair("red", 3))


def bar_wing():
    # barra en L contra la pared este con estantes de botellas detrás
    bx = 37
    for z in range(13, 30):
        bp.set(bx, 1, z, counter(3))
        bp.set(41, 1, z, st("casino:bottle_shelf", facing="west"))
        bp.set(41, 2, z, st("casino:bottle_shelf", facing="west"))
        bp.set(41, 3, z, PANEL)
        if z % 2 == 0:
            bp.set(bx - 1, 1, z, stool("red", 1))
            bp.set(bx, 2, z, drinks(1 + z % 3, 3))
    bp.set(bx, 1, 12, counter(0, "outer_right"))
    bp.set(bx, 1, 30, counter(2, "outer_left"))
    for x in range(bx + 1, 41):
        bp.set(x, 1, 12, counter(0))
        bp.set(x, 1, 30, counter(2))
    # mesas de cóctel con sillas entre la barra y el atrio
    for (x, z) in ((32, 15), (32, 21), (32, 27), (35, 33)):
        bp.set(x, 1, z, "casino:cocktail_table_black")
        bp.set(x, 2, z, drinks(2, z & 3))
        for d in (1, 3):
            bp.set(x - DX[d], 1, z - DZ[d], chair("black", d))
    # escenario al fondo del ala este: tarima de madera oscura con borde de oro, telón de terciopelo, bocinas y neón
    for x in range(31, 42):
        for z in range(3, 10):
            bp.set(x, 1, z, slab("dark_oak_slab", "top") if z < 9 else stairs("dark_oak_stairs", 0))
    for x in range(31, 42):
        bp.set(x, 2, 3, VEL_R)
        bp.set(x, 3, 3, VEL_R)
        for y in range(4, 9):
            bp.set(x, y, 3, VEL_R if x not in (31, 41) else GOLD)
    for x in (31, 41):
        bp.set(x, 2, 8, speaker(2))
        bp.set(x, 3, 8, speaker(2))
    for i, ch in enumerate("JAZZ"):
        bp.set(34 + i, 7, 4, neon(ch, 2, 1))
    bp.set(36, 2, 6, st("casino:casino_chair_red", facing="south"))
    # sofás del lounge frente al escenario
    sofa_row("purple", 31, 12, 35, 12, 0, 1)


def atrium():
    # alfombra real, ruleta al fondo, dos blackjack y un póker
    machine("ROULETTE", CX, 1, 19, 2)
    machine("BLACKJACK", 19, 1, 26, 2)
    machine("BLACKJACK", 25, 1, 26, 2)
    machine("POKER", CX, 1, 31, 0)
    # candelabros colgando del techo del atrio (y 19) con cadenas
    for (x, z) in ((CX, 18), (CX, 25), (CX, 31), (18, 21), (26, 21), (18, 30), (26, 30)):
        for y in range(15, F3):
            bp.set(x, y, z, chain())
        bp.set(x, 14, z, "casino:chandelier")
    # escalera imperial: dos tramos pegados a los lados del atrio que suben hacia el fondo hasta el entrepiso
    for side, xs in (("w", (AX0, AX0 + 1)), ("e", (AX1 - 1, AX1))):
        for i in range(10):          # y 1..10
            z = 34 - i
            y = 1 + i
            for x in xs:
                bp.set(x, y, z, stairs("quartz_stairs", 0))
                for yy in range(1, y):
                    bp.set(x, yy, z, SQZ if yy < y - 1 else SQZ)
            # pasamanos de oro del lado del atrio
            rx = xs[1] + 1 if side == "w" else xs[0] - 1
            bp.set(rx, y, z, SQZ if y > 1 else SQZ)
            bp.set(rx, y + 1, z, "polished_blackstone_wall") if False else bp.set(rx, y + 1, z, st("gold_block") if i % 3 == 0 else "quartz_slab[type=bottom]")
        # balcón (y 10) del tramo al fondo
        for z in range(AZ0, 25):
            for x in xs:
                bp.set(x, F2, z, SQZ)
            rx = xs[1] + 1 if side == "w" else xs[0] - 1
            bp.set(rx, F2, z, SQZ)
            bp.set(rx, F2 + 1, z, glass_rail())
    # barandal del balcón del fondo (y 11) sobre el borde norte del atrio
    for x in range(AX0, AX1 + 1):
        bp.set(x, F2 + 1, AZ0 - 1, glass_rail())
    # macetas bajo las escaleras
    for x in (AX0, AX1):
        bp.set(x, 1, AZ1, "potted_spruce_sapling")


def glass_rail():
    return st("glass_pane", north="true", south="true", east="true", west="true")


def back_zone():
    # rueda de la suerte en un podio al final del eje de la entrada
    for x in range(CX - 3, CX + 4):
        for z in range(3, 7):
            bp.set(x, 1, z, slab("smooth_quartz_slab", "bottom") if z == 6 else SQZ)
    machine("WHEEL", CX, 2, 4, 2)
    for x in (CX - 3, CX + 3):
        for y in range(2, 8):
            bp.set(x, y, 3, GOLD if y % 2 else QP)
        bp.set(x, 8, 3, "casino:chandelier") if False else None
    for i, ch in enumerate("SUERTE"):
        bp.set(CX + 2 - i, 8, 3, neon(ch, 2, 1)) if False else None
    # plinko a los lados de la rueda
    machine("PLINKO", 16, 1, 4, 2)
    machine("PLINKO", 28, 1, 4, 2)


# =====================================================================
# ENTREPISO
# =====================================================================
def mezzanine():
    # piso: alfombra lounge en el hipódromo, galaxia en el VIP, mármol en el pasillo del fondo
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            if bp.get(x, F2, z) == AIR:
                continue
            if x < AX0 and z >= AZ0 - 1:
                b = LOUNGE
            elif x > AX1 and z >= AZ0 - 1:
                b = GALAXY
            else:
                b = MARB if (x + z) % 2 else MARBK
            if bp.get(x, F2, z) in (SQZ, None) or True:
                bp.set(x, F2, z, b)
    # vuelven las escaleras (se taparon con el piso)
    # barandales de vidrio a lo largo del atrio en el entrepiso
    for z in range(AZ0, AZ1 + 1):
        for x in (AX0 - 1, AX1 + 1):
            if bp.get(x, F2, z) != AIR:
                bp.set(x, F2 + 1, z, glass_rail())
    # techo del entrepiso: artesonado con luces
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            if x % 4 == 0 and z % 4 == 0:
                bp.set(x, F3, z, SEA)
    hippodrome()
    vip()


def hippodrome():
    # pantalla gigante en la pared oeste (marco de oro; el mod la convertirá en la carrera)
    sx = X0 + 1
    for z in range(15, 34):
        for y in range(F2 + 2, F2 + 9):
            edge = z in (15, 33) or y in (F2 + 2, F2 + 8)
            bp.set(sx, y, z, GOLD if edge else "black_concrete")
    bp.mark("horse_screen", sx, F2 + 2, 15, 1)
    # letrero "HIPODROMO"
    for i, ch in enumerate("CARRERAS"):
        bp.set(sx, F2 + 9, 20 + i, neon(ch, 1, 0))
    # gradas: filas de sillas con mesitas mirando a la pantalla
    for x in (7, 10, 13):
        for z in range(16, 33):
            if z % 4 == 0:
                bp.set(x, F2 + 1, z, "casino:cocktail_table")
            else:
                bp.set(x, F2 + 1, z, chair("blue", 3))
    # ventanilla de apuestas
    machine("CASHIER", 8, F2 + 1, 37, 0)
    for x in (6, 7, 9, 10):
        bp.set(x, F2 + 1, 37, counter(0))
    bp.set(8, F2 + 3, 38, sign(5, 0)) if False else None


def vip():
    # muro de terciopelo morado que cierra el VIP del lado del pasillo del fondo, con puerta y guardia
    vz = AZ0 - 1
    for x in range(AX1 + 1, X1):
        for y in range(F2 + 1, F3):
            bp.set(x, y, vz, VEL_P if x % 4 else GOLD)
    for y in range(F2 + 1, F2 + 4):
        bp.set(35, y, vz, AIR)
        bp.set(36, y, vz, AIR)
    bp.set(34, F2 + 1, vz - 1, rope("purple", e=True))
    bp.set(37, F2 + 1, vz - 1, rope("purple", w=True))
    bp.mark("vip_guard", 35, F2 + 1, vz - 1, 0)
    for i, ch in enumerate("VIP"):
        bp.set(37 - i, F2 + 5, vz - 1, neon(ch, 0, 1))
    # mesas de alto riesgo
    machine("BLACKJACK", 36, F2 + 1, 22, 2)
    machine("POKER", 36, F2 + 1, 33, 0)
    # bar privado contra la pared este
    for z in range(16, 30):
        bp.set(40, F2 + 1, z, counter(3)) if False else None
    sofa_row("purple", 41, 26, 41, 29, 3, F2 + 1)
    sofa_row("purple", 41, 36, 41, 40, 3, F2 + 1)
    for z in (27, 38):
        bp.set(39, F2 + 1, z, "casino:cocktail_table_black")
        bp.set(39, F2 + 2, z, drinks(3, 1))
    bp.set(41, F2 + 1, 31, speaker(3))
    bp.mark("vip_auction", 33, F2 + 1, 40, 0)
    for (x, z) in ((36, 18), (36, 28), (36, 38)):
        bp.set(x, F3 - 1, z, "casino:chandelier")


# =====================================================================
# AZOTEA DEL PODIO: TERRAZA CON PISCINA
# =====================================================================
def podium_roof():
    y = F3
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            if not (TX0 <= x <= TX1 and TZ0 <= z <= TZ1):
                bp.set(x, y, z, "smooth_stone" if (x + z) % 2 else SQZ)
    # piscina al frente (entre la torre y la fachada)
    for x in range(14, 31):
        for z in range(34, 40):
            edge = x in (14, 30) or z in (34, 39)
            bp.set(x, y + 1, z, slab("smooth_quartz_slab") if edge else "water")
            if not edge:
                bp.set(x, y, z, "prismarine_bricks")
    for x in range(15, 30, 3):
        bp.set(x, y, 36, SEA)
    # camastros (sillas blancas) y sombrillas
    for x in (5, 8, 36, 39):
        for z in (34, 38):
            bp.set(x, y + 1, z, chair("blue", 2))
    # jardineras con abetos en las esquinas
    for (x, z) in ((5, 5), (39, 5), (5, 26), (39, 26)):
        planter_tree(x, z) if False else small_tree(x, y + 1, z)


def small_tree(x, y, z):
    bp.set(x, y, z, st("spruce_log", axis="y"))
    bp.set(x, y + 1, z, st("spruce_log", axis="y"))
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if (dx, dz) != (0, 0):
                bp.set(x + dx, y + 1, z + dz, st("spruce_leaves", persistent="true"))
            bp.set(x + dx, y + 2, z + dz, st("spruce_leaves", persistent="true"))
    bp.set(x, y + 3, z, st("spruce_leaves", persistent="true"))


# =====================================================================
# TORRE
# =====================================================================
def tower():
    for i in range(len(TF) - 1):
        y0, y1 = TF[i], TF[i + 1]
        bp.fill(TX0 + 1, y0 + 1, TZ0 + 1, TX1 - 1, y1 - 1, TZ1 - 1, AIR)
        for y in range(y0, y1 + 1):
            for x in range(TX0, TX1 + 1):
                for z in (TZ0, TZ1):
                    bp.set(x, y, z, tower_wall(x, y, along_x=True, y0=y0))
            for z in range(TZ0, TZ1 + 1):
                for x in (TX0, TX1):
                    bp.set(x, y, z, tower_wall(z, y, along_x=False, y0=y0))
        if i > 0:
            bp.fill(TX0 + 1, y0, TZ0 + 1, TX1 - 1, y0, TZ1 - 1, DOAK)
    # techo de la torre = piso de la discoteca
    bp.fill(TX0, TF[-1], TZ0, TX1, TF[-1], TZ1, SQZ)
    restaurant(TF[0])
    suites(TF[1])
    penthouse(TF[2])
    sky_club(TF[3])
    spiral()


def tower_wall(i, y, along_x, y0):
    rel = y - y0
    if (along_x and i in (TX0, TX1)) or (not along_x and i in (TZ0, TZ1)):
        return QP
    if rel == 0:
        return TRIM
    if i % 3 == 0:
        return QP
    if 2 <= rel <= 5:
        return GLASSL
    return QZ


def restaurant(y):
    for x in range(TX0 + 1, TX1):
        for z in range(TZ0 + 1, TZ1):
            if bp.get(x, y, z) == AIR:
                continue
    # mesas redondas con sillas
    for (x, z) in ((16, 18), (16, 24), (22, 18), (22, 24), (28, 18), (28, 24), (19, 28), (25, 28)):
        bp.set(x, y + 1, z, "casino:cocktail_table")
        bp.set(x, y + 2, z, drinks(2))
        for d in range(4):
            bp.set(x + DX[d], y + 1, z + DZ[d], chair("red", opp(d)))
    for x in range(TX0 + 2, TX1 - 1, 5):
        bp.set(x, y + 6, TZ0 + 6, "casino:chandelier") if False else None
    for (x, z) in ((16, 21), (22, 21), (28, 21), (22, 27)):
        bp.set(x, y + 6, z, "casino:chandelier")


def suites(y):
    # pasillo central (z 19..21) y 4 habitaciones a cada lado
    for x in range(TX0 + 1, TX1):
        for z in range(TZ0 + 1, TZ1):
            bp.set(x, y, z, ROYALC.replace("carpet", "floor") if 19 <= z <= 21 else LOUNGE)
    for x in range(TX0 + 1, TX1):
        for yy in range(y + 1, y + 6):
            if not (x - TX0) % 5 == 3 or yy > y + 3:
                bp.set(x, yy, 18, PANEL)
                bp.set(x, yy, 22, PANEL)
    for xw in range(TX0 + 5, TX1, 5):
        for yy in range(y + 1, y + 6):
            for z in list(range(TZ0 + 1, 18)) + list(range(23, TZ1)):
                bp.set(xw, yy, z, PANEL)
    # camas y muebles en cada cuarto
    for xr in range(TX0 + 1, TX1 - 3, 5):
        for (zb, d) in ((TZ0 + 2, 2), (TZ1 - 2, 0)):
            cx = xr + 1
            bed(cx, y + 1, zb, d)
            bp.set(cx + 2, y + 1, zb, "casino:cocktail_table")
            bp.set(cx + 2, y + 2, zb, lantern())
    for x in range(TX0 + 3, TX1, 5):
        bp.set(x, y + 5, 20, SEA) if False else bp.set(x, y + 5, 20, "casino:chandelier")


def bed(x, y, z, d):
    """Cama roja de 2 bloques; d = hacia donde apunta la cabecera (pegada a la pared)."""
    head = (x, y, z)
    foot = (x - DX[d], y, z - DZ[d])
    bp.set(*head, st("red_bed", facing=DIRS[d], part="head"))
    bp.set(*foot, st("red_bed", facing=DIRS[d], part="foot"))


def penthouse(y):
    for x in range(TX0 + 1, TX1):
        for z in range(TZ0 + 1, TZ1):
            bp.set(x, y, z, MARB if (x + z) % 2 else MARBK)
    # sala con sofás en U frente a una pantalla, comedor, recámara con cama doble
    sofa_row("black", 18, 24, 26, 24, 0, y + 1)
    sofa_row("black", 17, 19, 17, 23, 1, y + 1)
    bp.set(22, y + 1, 21, "casino:cocktail_table_black")
    bp.set(22, y + 2, 21, drinks(4))
    for x in range(19, 26):
        for yy in (y + 2, y + 3, y + 4):
            bp.set(x, yy, TZ0 + 1, screen((x + yy) % 4, 2))
    bed(30, y + 1, 26, 1)
    bed(30, y + 1, 27, 1)
    bp.set(30, y + 1, 25, "casino:cocktail_table")
    bp.set(30, y + 2, 25, lantern())
    bp.set(30, y + 1, 28, "casino:cocktail_table")
    bp.set(30, y + 2, 28, lantern())
    bp.set(22, y + 6, 21, "casino:chandelier")
    bp.set(29, y + 6, 26, "casino:chandelier")
    # bar privado
    for z in range(13, 18):
        bp.set(29, y + 1, z, counter(3))
        bp.set(31, y + 1, z, st("casino:bottle_shelf", facing="west"))
        bp.set(31, y + 2, z, st("casino:bottle_shelf", facing="west"))
    for z in (14, 16):
        bp.set(28, y + 1, z, stool("black", 1))


def sky_club(y):
    """Discoteca al aire libre en el techo: pista de baile iluminada, cabina del DJ, barra, sofás y la corona art déco."""
    # pista de baile 9x9 de vidrio de colores sobre luces
    colors = ["magenta", "cyan", "purple", "lime", "pink", "light_blue", "yellow", "orange"]
    for x in range(18, 27):
        for z in range(18, 27):
            c = colors[((x // 2) + (z // 2)) % len(colors)]
            bp.set(x, y, z, f"{c}_stained_glass")
            bp.set(x, y - 1, z, SEA)
    # resto del piso negro brillante
    for x in range(TX0 + 1, TX1):
        for z in range(TZ0 + 1, TZ1):
            if not (18 <= x <= 26 and 18 <= z <= 26):
                bp.set(x, y, z, "black_concrete" if (x + z) % 2 else "black_glazed_terracotta|black_concrete")
    # barandal de vidrio alrededor (seguridad) con postes de oro
    for x in range(TX0, TX1 + 1):
        for z in (TZ0, TZ1):
            bp.set(x, y + 1, z, GOLD if x % 4 == 0 else st("glass_pane", east="true", west="true"))
    for z in range(TZ0, TZ1 + 1):
        for x in (TX0, TX1):
            bp.set(x, y + 1, z, GOLD if z % 4 == 0 else st("glass_pane", north="true", south="true"))
    # cabina del DJ al fondo (norte), mirando a la pista
    for x in range(19, 26):
        bp.set(x, y + 1, 14, counter(2))
    for x in (18, 26):
        for yy in range(y + 1, y + 4):
            bp.set(x, yy, 13, speaker(2))
    bp.mark("dj", CX, y + 1, 13, 2)
    for i, ch in enumerate("SKY CLUB"):
        if ch != " ":
            bp.set(26 - i, y + 6, 12, neon(ch, 2, 1 if i < 3 else 4))
    # pórtico art déco detrás del DJ (marco escalonado de oro con luces)
    for x in range(16, 29):
        for yy in range(y + 1, y + 9):
            edge = x in (16, 28) or yy == y + 8
            if edge:
                bp.set(x, yy, 12, GOLD if (x + yy) % 2 else SEA)
    # barra a la derecha y sofás a la izquierda
    for z in range(15, 28):
        bp.set(31, y + 1, z, counter(3))
        if z % 2:
            bp.set(30, y + 1, z, stool("purple", 1))
    sofa_row("purple", 13, 16, 13, 22, 1, y + 1)
    sofa_row("purple", 13, 24, 13, 29, 1, y + 1)
    for z in (19, 27):
        bp.set(15, y + 1, z, "casino:cocktail_table_black")
        bp.set(15, y + 2, z, drinks(3, 1))
    # luces de colores en postes (reflectores)
    for (x, z) in ((TX0 + 1, TZ0 + 1), (TX1 - 1, TZ0 + 1), (TX0 + 1, TZ1 - 1), (TX1 - 1, TZ1 - 1)):
        for yy in range(y + 1, y + 5):
            bp.set(x, yy, z, "polished_blackstone_wall")
        bp.set(x, y + 5, z, "end_rod")
    # corona escalonada en las esquinas de la torre (se ve desde lejos)
    for (x, z) in ((TX0, TZ0), (TX1, TZ0), (TX0, TZ1), (TX1, TZ1)):
        for yy in range(y + 1, y + 8):
            bp.set(x, yy, z, QP if yy < y + 7 else GOLD)
        bp.set(x, y + 8, z, "end_rod")


def spiral():
    """Escalera de caracol en un pozo de 4x4 (x 13..16, z 10..13) desde el entrepiso hasta el techo: medio bloque por paso."""
    sx, sz = 13, 10
    ring = [(0, 0), (1, 0), (2, 0), (3, 0), (3, 1), (3, 2), (3, 3), (2, 3), (1, 3), (0, 3), (0, 2), (0, 1)]
    # muros del pozo con ventanitas y la columna central
    ystart, yend = F2, TF[-1]
    for y in range(ystart + 1, yend + 1):
        for dx in range(-1, 5):
            for dz in range(-1, 5):
                if dx in (-1, 4) or dz in (-1, 4):
                    bp.set(sx + dx, y, sz + dz, "quartz_bricks" if y % 7 else TRIM)
                else:
                    bp.set(sx + dx, y, sz + dz, AIR)
        for (dx, dz) in ((1, 1), (2, 1), (1, 2), (2, 2)):
            bp.set(sx + dx, y, sz + dz, QP if y % 7 else GOLD)
    # pasos: cada celda del anillo sube medio bloque
    half = 0
    k = 0
    surface = {}
    while True:
        level = ystart * 2 + 2 + half  # superficie en medios bloques (empieza en y F2+1)
        by = level // 2  # bloque donde va el escalón
        dx, dz = ring[k % 12]
        if level % 2 == 1:   # superficie a .5: losa abajo en by
            spec = slab("smooth_quartz_slab", "bottom")
            blk_y = by
        else:                 # superficie entera: losa arriba en by-1
            spec = slab("smooth_quartz_slab", "top")
            blk_y = by - 1
        if blk_y > yend:
            break
        bp.set(sx + dx, blk_y, sz + dz, spec)
        surface[(dx, dz, level)] = blk_y
        # luz cada 6 pasos en la columna
        if k % 6 == 0:
            pass
        half += 1
        k += 1
        if level >= (yend + 1) * 2:
            break
    # puertas en cada piso: busca el paso cuya superficie es el piso + 1 y abre el muro hacia afuera
    floors = [F2, F3] + TF[1:]
    for fy in floors:
        target = (fy + 1) * 2
        for (dx, dz, level), by in surface.items():
            if level == target:
                ox, oz = (dx - 1 if dx == 0 else dx + 1 if dx == 3 else dx), (dz - 1 if dz == 0 else dz + 1 if dz == 3 else dz)
                for yy in (fy + 1, fy + 2):
                    bp.set(sx + ox, yy, sz + oz, AIR)
                bp.mark("spiral_exit", sx + ox, fy + 1, sz + oz, 0)
                break
    # luces en la columna
    for y in range(ystart + 3, yend, 4):
        bp.set(sx + 1, y, sz + 1, SEA)


# =====================================================================
def build():
    plaza()
    shell()
    ground_floor()
    mezzanine()
    podium_roof()
    tower()
    # el atrio vuelve a quedar abierto (el piso del entrepiso no debe taparlo)
    for x in range(AX0, AX1 + 1):
        for z in range(AZ0, AZ1 + 1):
            if bp.get(x, F2, z) not in (None, AIR) and not (x in (AX0, AX0 + 1, AX1 - 1, AX1) and z <= 25):
                bp.set(x, F2, z, AIR)
    return bp


if __name__ == "__main__":
    b = build()
    out = sys.argv[1] if len(sys.argv) > 1 else "elegante.bp"
    n = b.save(out)
    print("paleta", n, "celdas", len(b.cells), "máquinas", len(b.machines), "marcas", len(b.marks))
