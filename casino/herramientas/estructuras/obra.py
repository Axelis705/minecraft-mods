#!/usr/bin/env python3
"""Herramienta para diseñar estructuras del casino bloque por bloque en Python y guardarlas como plantilla .nbt de Minecraft
(formato de bloque de estructura, DataVersion de 1.20.1: las versiones nuevas la actualizan solas al cargarla).

Coordenadas de la plantilla: x = este, y = arriba, z = sur. El frente (entrada) de los casinos va en el lado sur (z máxima).
Los bloques con dirección se escriben como en el juego: "minecraft:quartz_stairs[facing=north,half=bottom]".
Las máquinas y bloques especiales van como marcadores (bloque de estructura en modo datos): ver TemplateCasinoPiece.Markers.
"""
import gzip, io, struct

DATA_VERSION = 3465  # 1.20.1
AIR = "minecraft:air"
VOID = "minecraft:structure_void"

# ------------------------------------------------------------------ NBT
class Tag:
    def __init__(self, t, v): self.t, self.v = t, v
def Byte(v): return Tag(1, v)
def Short(v): return Tag(2, v)
def Int(v): return Tag(3, v)
def Long(v): return Tag(4, v)
def Float(v): return Tag(5, v)
def Double(v): return Tag(6, v)
def Str(v): return Tag(8, v)
def List(t, items): return Tag(9, (t, items))
def Comp(d): return Tag(10, d)
def IntArr(v): return Tag(11, v)

def _w_payload(o, tag):
    t, v = tag.t, tag.v
    if t == 1: o.write(struct.pack('>b', v))
    elif t == 2: o.write(struct.pack('>h', v))
    elif t == 3: o.write(struct.pack('>i', v))
    elif t == 4: o.write(struct.pack('>q', v))
    elif t == 5: o.write(struct.pack('>f', v))
    elif t == 6: o.write(struct.pack('>d', v))
    elif t == 8:
        b = v.encode('utf-8'); o.write(struct.pack('>H', len(b))); o.write(b)
    elif t == 9:
        et, items = v
        o.write(struct.pack('>bi', et if items else 0, len(items)))
        for it in items: _w_payload(o, it)
    elif t == 10:
        for k, it in v.items():
            o.write(struct.pack('>b', it.t)); _w_payload(o, Str(k)); _w_payload(o, it)
        o.write(b'\x00')
    elif t == 11:
        o.write(struct.pack('>i', len(v)))
        for x in v: o.write(struct.pack('>i', x))
    else: raise ValueError(t)

def write_nbt(path, root):
    o = io.BytesIO()
    o.write(struct.pack('>b', 10)); _w_payload(o, Str('')); _w_payload(o, root)
    with gzip.open(path, 'wb') as f: f.write(o.getvalue())

# ------------------------------------------------------------------ estados
def norm(st):
    if ':' not in st.split('[')[0]: st = 'minecraft:' + st
    return st

def split_state(st):
    st = norm(st)
    if '[' in st:
        n, pr = st[:-1].split('[', 1)
        props = dict(kv.split('=', 1) for kv in pr.split(',') if kv)
        return n, props
    return st, {}

DIRS = ['north', 'east', 'south', 'west']
OPP = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}
DIR_I = {d: i for i, d in enumerate(DIRS)}

class Obra:
    def __init__(self, W, H, D, fill=AIR):
        self.W, self.H, self.D = W, H, D
        self.b = {}
        self.markers = {}
        self.base = fill

    def inside(self, x, y, z):
        return 0 <= x < self.W and 0 <= y < self.H and 0 <= z < self.D

    def set(self, x, y, z, st):
        if not self.inside(x, y, z): return
        if st is None: self.b.pop((x, y, z), None); return
        self.b[(x, y, z)] = norm(st)
        self.markers.pop((x, y, z), None)

    def get(self, x, y, z):
        return self.b.get((x, y, z), self.base)

    def fill(self, x0, y0, z0, x1, y1, z1, st):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, st)

    def box(self, x0, y0, z0, x1, y1, z1, st):
        """Solo el borde (paredes) de una caja, sin piso ni techo."""
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, z0, st); self.set(x, y, z1, st)
            for z in range(z0, z1 + 1):
                self.set(x0, y, z, st); self.set(x1, y, z, st)

    def marker(self, x, y, z, meta, preview=None):
        """Marcador de datos (máquinas, elevador...). preview = bloque que se muestra en el visor."""
        self.b[(x, y, z)] = 'minecraft:structure_block[mode=data]'
        self.markers[(x, y, z)] = (meta, preview)

    # -------------------------------------------------------------- salida
    def save(self, path):
        palette, pidx, blocks = [], {}, []
        def pal(st):
            if st not in pidx:
                n, props = split_state(st)
                d = {'Name': Str(n)}
                if props: d['Properties'] = Comp({k: Str(v) for k, v in props.items()})
                pidx[st] = len(palette); palette.append(Comp(d))
            return pidx[st]
        for x in range(self.W):
            for y in range(self.H):
                for z in range(self.D):
                    st = self.b.get((x, y, z), self.base)
                    e = {'pos': List(3, [Int(x), Int(y), Int(z)]), 'state': Int(pal(st))}
                    if (x, y, z) in self.markers:
                        meta = self.markers[(x, y, z)][0]
                        e['nbt'] = Comp({'id': Str('minecraft:structure_block'), 'mode': Str('DATA'), 'metadata': Str(meta),
                                         'name': Str(''), 'author': Str('casino'), 'posX': Int(0), 'posY': Int(1), 'posZ': Int(0),
                                         'sizeX': Int(0), 'sizeY': Int(0), 'sizeZ': Int(0), 'rotation': Str('NONE'), 'mirror': Str('NONE'),
                                         'ignoreEntities': Byte(1), 'powered': Byte(0), 'showair': Byte(0), 'showboundingbox': Byte(1),
                                         'integrity': Float(1.0), 'seed': Long(0)})
                    blocks.append(Comp(e))
        root = Comp({'DataVersion': Int(DATA_VERSION), 'size': List(3, [Int(self.W), Int(self.H), Int(self.D)]),
                     'palette': List(10, palette), 'blocks': List(10, blocks), 'entities': List(10, [])})
        write_nbt(path, root)
        return len(palette), len(blocks)

    def dump(self, path, previews=True):
        """Volcado 'x y z estado' para el visor 3D (estructuras/herramientas/visor/bake.py)."""
        with open(path, 'w') as f:
            for (x, y, z), st in self.b.items():
                if (x, y, z) in self.markers:
                    pv = self.markers[(x, y, z)][1]
                    if not previews or not pv: continue
                    for (dx, dy, dz, s) in pv:
                        f.write(f'{x+dx} {y+dy} {z+dz} {norm(s)}\n')
                    continue
                if st in (AIR, VOID, 'minecraft:cave_air'): continue
                f.write(f'{x} {y} {z} {st}\n')
