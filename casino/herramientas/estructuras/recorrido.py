#!/usr/bin/env python3
"""Revisa que en una plantilla se pueda caminar: desde la calle hasta la entrada, el elevador y las máquinas de cada piso.
El jugador ocupa 1x2 bloques, sube escalones de 1 (escaleras, losas) y baja lo que sea. Los elevadores conectan sus paradas.

    python3 recorrido.py the_diamond
"""
import importlib.util, os, sys
from collections import deque

name = sys.argv[1] if len(sys.argv) > 1 else 'the_diamond'
here = os.path.dirname(os.path.abspath(__file__))
sys.argv = [sys.argv[0]]
spec = importlib.util.spec_from_file_location(name, os.path.join(here, name + '.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
o = m.o

PASS = ('minecraft:air', 'minecraft:light', 'minecraft:cave_air', 'minecraft:structure_void')
SOFT = ('carpet', 'minecraft:light[', 'casino:drinks', 'end_rod', 'pressure_plate')


def passable(st):
    if st is None:
        return True
    n = st.split('[')[0]
    if st.startswith('minecraft:light') or n in PASS:
        return True
    return any(k in st for k in SOFT)


def low(st):
    """Bloques que se pueden pisar subiendo (escalón)."""
    return 'stairs' in st or 'slab' in st


def solid(st):
    return not passable(st) and 'water' not in st


def get(x, y, z):
    if (x, y, z) in o.markers:
        return 'marker'
    return o.get(x, y, z)


def standable(x, y, z):
    """El jugador puede estar parado con los pies en (x,y,z)."""
    if not (0 <= x < o.W and 0 <= z < o.D and 1 <= y < o.H - 1):
        return False
    if (x, y, z) in o.markers or (x, y + 1, z) in o.markers:
        return False
    return passable(o.get(x, y, z)) and passable(o.get(x, y + 1, z)) and solid(o.get(x, y - 1, z))


def bfs(starts):
    seen = set(starts)
    q = deque(starts)
    while q:
        x, y, z = q.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            for ny in (y, y + 1, y - 1, y - 2, y - 3):
                if ny == y + 1 and not passable(o.get(x, y + 2, z)):
                    continue
                if standable(nx, ny, nz):
                    if (nx, ny, nz) not in seen:
                        seen.add((nx, ny, nz)); q.append((nx, ny, nz))
                    break
    return seen


# paradas del elevador: el jugador aparece delante del panel (panel mira al sur: z+1, y-1)
stops = sorted([(k, v[0]) for k, v in o.markers.items() if v[0].startswith('elev')], key=lambda t: t[0][1])
start = (m.CX, 1, o.D - 3)
reach = bfs([start])
for (p, meta) in stops:
    stand = (p[0], p[1] - 1, p[2] + 1)
    if stand in reach:
        # el elevador lleva a todas las demás paradas
        reach |= bfs([(q[0], q[1] - 1, q[2] + 1) for q, _ in stops if standable(q[0], q[1] - 1, q[2] + 1)])
        break

problems = []
for (p, meta) in stops:
    stand = (p[0], p[1] - 1, p[2] + 1)
    ok = stand in reach
    print(('OK   ' if ok else 'MAL  ') + 'elevador ' + meta, stand)
    if not ok:
        problems.append(meta)
# cada máquina y vitrina: algún bloque vecino del frente alcanzable
for p, (meta, _) in o.markers.items():
    if meta.startswith('elev'):
        continue
    near = [(p[0] + dx, yy, p[2] + dz) for dx in range(-4, 5) for dz in range(-4, 5) for yy in (p[1] - 1, p[1], p[1] + 1)]
    ok = any(n in reach for n in near)
    if not ok:
        problems.append(meta + ' ' + str(p))
        print('MAL  ' + meta, p)
# zonas importantes por piso
floors = {'casino': 1, 'vip': m.Y_VIP + 1, 'penthouse': m.Y_PH + 1, 'azotea': m.Y_PH_TOP + 1}
for fname, fy in floors.items():
    n = sum(1 for r in reach if r[1] == fy)
    print(f'{fname}: {n} bloques caminables')
print('PROBLEMAS:', len(problems))
sys.exit(1 if problems else 0)
