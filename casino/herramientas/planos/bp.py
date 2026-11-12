"""Planos de estructuras del casino: un constructor en Python que escribe un archivo .bp que el mod lee al generar el mundo.

Marco del plano: x de 0 a W-1 (este = +x), y de 0 a H-1 (y = 0 es el piso a nivel del terreno), z de 0 a D-1 (sur = +z, lado de la
entrada). Cada celda guarda un bloque como texto ("minecraft:quartz_stairs[facing=north,half=top]"); con "a|b" se dan alternativas
por si un bloque no existe en alguna versión. Las máquinas del casino van aparte (el mod las arma con todas sus partes y crupieres).

Formato .bp (texto comprimido con gzip):
    CBP1 W H D
    P n            + n líneas con el texto de cada bloque (índice 1..n; 0 = no tocar)
    C              + una línea con las celdas en orden y, z, x (RLE: "i" o "i*veces", separado por espacios)
    M n            + n líneas "TIPO x y z dir"   (dir 0 norte, 1 este, 2 sur, 3 oeste: hacia donde mira su frente)
    K n            + n líneas "nombre x y z dir" (marcas: guardia, aparición de NPC, etc.)
"""
import gzip

DIRS = ["north", "east", "south", "west"]
DX = [0, 1, 0, -1]
DZ = [-1, 0, 1, 0]
AIR = "minecraft:air"


def opp(d):
    return (d + 2) & 3


def cw(d):
    return (d + 1) & 3


def ccw(d):
    return (d + 3) & 3


def st(block, **props):
    """Texto de un estado: st("quartz_stairs", facing="north", half="top")."""
    if ":" not in block.split("|")[0]:
        block = "minecraft:" + block
    if not props:
        return block
    inner = ",".join(f"{k}={str(v).lower()}" for k, v in props.items())
    # las alternativas comparten propiedades
    return "|".join(f"{b}[{inner}]" for b in block.split("|"))


class BP:
    def __init__(self, W, H, D):
        self.W, self.H, self.D = W, H, D
        self.cells = {}
        self.machines = []
        self.marks = []

    # --- bloques ---
    def inside(self, x, y, z):
        return 0 <= x < self.W and 0 <= y < self.H and 0 <= z < self.D

    def set(self, x, y, z, spec):
        if not self.inside(x, y, z):
            raise ValueError(f"fuera del plano: {x},{y},{z} ({spec})")
        if spec is None:
            self.cells.pop((x, y, z), None)
        else:
            self.cells[(x, y, z)] = spec if ":" in spec.split("|")[0].split("[")[0] else "minecraft:" + spec

    def get(self, x, y, z):
        return self.cells.get((x, y, z))

    def fill(self, x0, y0, z0, x1, y1, z1, spec):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, spec)

    def replace(self, x0, y0, z0, x1, y1, z1, old, new):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if self.get(x, y, z) == old:
                        self.set(x, y, z, new)

    def walls(self, x0, y0, z0, x1, y1, z1, spec):
        """Solo el contorno (las 4 paredes) de una caja."""
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, z0, spec)
                self.set(x, y, z1, spec)
            for z in range(z0, z1 + 1):
                self.set(x0, y, z, spec)
                self.set(x1, y, z, spec)

    # --- máquinas y marcas ---
    def machine(self, kind, x, y, z, d):
        self.machines.append((kind, x, y, z, d))

    def mark(self, name, x, y, z, d=0):
        self.marks.append((name, x, y, z, d))

    # --- guardar ---
    def save(self, path):
        pal = {}
        order = []
        for spec in self.cells.values():
            if spec not in pal:
                pal[spec] = len(order) + 1
                order.append(spec)
        toks = []
        prev, run = None, 0
        for y in range(self.H):
            for z in range(self.D):
                for x in range(self.W):
                    s = self.cells.get((x, y, z))
                    i = pal[s] if s else 0
                    if i == prev:
                        run += 1
                    else:
                        if prev is not None:
                            toks.append(str(prev) if run == 1 else f"{prev}*{run}")
                        prev, run = i, 1
        toks.append(str(prev) if run == 1 else f"{prev}*{run}")
        out = [f"CBP1 {self.W} {self.H} {self.D}", f"P {len(order)}"] + order + ["C", " ".join(toks), f"M {len(self.machines)}"]
        out += [f"{k} {x} {y} {z} {d}" for k, x, y, z, d in self.machines]
        out += [f"K {len(self.marks)}"] + [f"{n} {x} {y} {z} {d}" for n, x, y, z, d in self.marks]
        with gzip.open(path, "wt", encoding="utf-8") as f:
            f.write("\n".join(out) + "\n")
        return len(order)

    def block_ids(self):
        ids = set()
        for spec in self.cells.values():
            for alt in spec.split("|"):
                ids.add(alt.split("[")[0])
        return ids
