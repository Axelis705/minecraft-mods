"""Huella de las máquinas del casino (igual que CasinoPlan.cells en Java) para colocar marcadores y vistas previas."""
DX = [0, 1, 0, -1]
DZ = [-1, 0, 1, 0]
BJ = [(-2, 0), (-1, 0), (0, 0), (1, 0), (2, 0), (-2, 1), (-1, 1), (0, 1), (1, 1), (2, 1), (-1, 2), (0, 2), (1, 2)]
RO = [(c, r) for r in (-1, 0, 1) for c in range(-3, 4)]
FACE = ['north', 'east', 'south', 'west']

def right(d): return (d + 3) & 3

def cells(kind, d):
    r = right(d)
    out = []
    if kind in ('blackjack', 'poker'):
        for c0, c1 in BJ: out.append((DX[r] * c0 + DX[d] * c1, 0, DZ[r] * c0 + DZ[d] * c1))
    elif kind == 'roulette':
        for c0, c1 in RO: out.append((DX[r] * c0 + DX[d] * c1, 0, DZ[r] * c0 + DZ[d] * c1))
    elif kind in ('slot', 'scratch'):
        out = [(0, 0, 0), (0, 1, 0)]
    elif kind == 'cashier':
        out = [(0, 0, 0)]
    elif kind in ('wheel', 'plinko'):
        for px in range(3):
            for py in range(4): out.append((DX[r] * (px - 1), py, DZ[r] * (px - 1)))
    return out

def dealer(kind, d):
    r = right(d)
    if kind in ('blackjack', 'poker'): return (-DX[d], 0, -DZ[d])
    if kind == 'roulette': return (DX[r] * -2 + DX[d] * -2, 0, DZ[r] * -2 + DZ[d] * -2)
    return None

PREVIEW = {'blackjack': 'minecraft:green_concrete', 'poker': 'minecraft:lime_concrete', 'roulette': 'minecraft:green_terracotta',
           'wheel': 'minecraft:gold_block', 'plinko': 'minecraft:purple_concrete', 'cashier': 'casino:cashier'}

def place(o, kind, x, y, z, d):
    """Pone el marcador de una máquina (d: 0 norte, 1 este, 2 sur, 3 oeste = hacia donde mira su frente) y reserva su huella."""
    pv = []
    for (cx, cy, cz) in cells(kind, d):
        if kind in ('slot', 'scratch'):
            st = f'casino:{kind if kind == "slot" else "scratch_machine"}[facing={FACE[d]},half={"lower" if cy == 0 else "upper"}]'
        elif kind == 'cashier':
            st = f'casino:cashier[facing={FACE[d]}]'
        else:
            st = PREVIEW[kind]
        pv.append((cx, cy, cz, st))
        if (cx, cy, cz) != (0, 0, 0):
            o.set(x + cx, y + cy, z + cz, 'minecraft:air')
    o.marker(x, y, z, f'm {kind} {d}', pv)
    dl = dealer(kind, d)
    if dl:
        o.set(x + dl[0], y, z + dl[2], 'minecraft:air'); o.set(x + dl[0], y + 1, z + dl[2], 'minecraft:air')
    return dl
