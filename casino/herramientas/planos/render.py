"""Vista previa de un plano .bp: dibujo isométrico con las texturas reales de Minecraft y cortes por piso (vista de arriba).

uso: python3 render.py plano.bp salida_prefijo [--cut y1,y2,...] [--angle 0-3]
Lee las texturas del jar del cliente (1.21.4) y las del mod. Es solo para revisar el diseño; no tiene que ser exacto.
"""
import gzip
import io
import os
import sys
import zipfile

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
MOD_TEX = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "casino", "textures", "block")
JAR = "/tmp/claude-0/-home-claude/480fd80e-aa2e-51e0-b666-f35748868f51/scratchpad/forge/1.21.4/libs/client-extra-1.21.4-20241203.143248.jar"

_zip = zipfile.ZipFile(JAR)
_names = set(_zip.namelist())
_cache = {}


def load(path):
    if path in _cache:
        return _cache[path]
    img = None
    if path.startswith("mc:"):
        n = "assets/minecraft/textures/block/" + path[3:] + ".png"
        if n in _names:
            img = Image.open(io.BytesIO(_zip.read(n))).convert("RGBA")
    else:
        p = os.path.join(MOD_TEX, path + ".png")
        if os.path.exists(p):
            img = Image.open(p).convert("RGBA")
    if img is not None and img.height > img.width:  # animadas: primer cuadro
        img = img.crop((0, 0, img.width, img.width))
    if img is not None:
        img = img.resize((16, 16), Image.NEAREST)
    _cache[path] = img
    return img


def parse(spec):
    spec = spec.split("|")[0]
    bid, props = spec, {}
    if "[" in spec:
        bid, rest = spec.split("[", 1)
        for kv in rest.rstrip("]").split(","):
            k, v = kv.split("=")
            props[k] = v
    ns, name = bid.split(":")
    return ns, name, props


COLORS = {  # muebles y bloques sin textura simple
    "slot": (200, 40, 40), "blackjack": (20, 110, 60), "poker": (20, 90, 70), "roulette": (20, 110, 60), "cashier": (120, 90, 30),
    "lucky_wheel": (220, 180, 40), "plinko": (60, 30, 120), "scratch_machine": (200, 120, 30),
}

SHAPES = {"carpet": 0.06, "slab": 0.5, "pressure_plate": 0.06, "rail": 0.06}


def textures(ns, name, props):
    """(arriba, lado, altura 0..1, opaco)."""
    h = 1.0
    for k, v in SHAPES.items():
        if name.endswith(k):
            h = v
    if props.get("type") == "double":
        h = 1.0
    if ns == "casino":
        if name in ("marble", "marble_stairs", "marble_slab"):
            t = load("marble") or load("decor/marble")
            return t, t, h, True
        for pre, tex in (("marble_black", "decor/marble_black"), ("casino_floor_", "decor/floor_"), ("casino_carpet_", "decor/floor_"),
                         ("velvet_wall_", "decor/velvet_"), ("mahogany_panel", "decor/mahogany_panel"), ("gold_trim", "decor/gold_trim")):
            if name.startswith(pre):
                suf = name[len(pre):]
                t = load(tex + suf) or load(tex)
                if t is not None:
                    hh = 0.06 if "carpet" in name else h
                    return t, t, hh, True
        return None, None, h, False
    base = name
    for suf in ("_stairs", "_slab", "_wall", "_fence_gate", "_fence", "_carpet", "_pane", "_button", "_pressure_plate", "_trapdoor"):
        if base.endswith(suf):
            base = base[: -len(suf)]
            break
    if name.endswith("_carpet"):
        base = base + "_wool"
    if name.endswith("_pane"):
        base = base if base != "glass" else "glass"
    alias = {"quartz_block": ("quartz_block_top", "quartz_block_side"), "quartz": ("quartz_block_top", "quartz_block_side"),
             "smooth_quartz": ("quartz_block_bottom", "quartz_block_bottom"), "quartz_pillar": ("quartz_pillar_top", "quartz_pillar"),
             "chiseled_quartz_block": ("chiseled_quartz_block_top", "chiseled_quartz_block"), "dark_oak": ("dark_oak_planks", "dark_oak_planks"),
             "oak": ("oak_planks", "oak_planks"), "spruce": ("spruce_planks", "spruce_planks"), "polished_blackstone_brick": ("polished_blackstone_bricks",) * 2,
             "stone_brick": ("stone_bricks",) * 2, "deepslate_tile": ("deepslate_tiles",) * 2, "polished_deepslate": ("polished_deepslate",) * 2,
             "smooth_stone": ("smooth_stone", "smooth_stone_slab_side"), "glowstone": ("glowstone",) * 2, "sea_lantern": ("sea_lantern",) * 2,
             "dark_prismarine": ("dark_prismarine",) * 2, "prismarine_brick": ("prismarine_bricks",) * 2, "nether_brick": ("nether_bricks",) * 2,
             "grass_block": ("grass_block_top", "grass_block_side"), "water": ("water_still", "water_still"), "snow_block": ("snow", "snow"),
             "spruce_leaves": ("spruce_leaves",) * 2, "end_stone_brick": ("end_stone_bricks",) * 2, "mud_brick": ("mud_bricks",) * 2,
             "cut_copper": ("cut_copper",) * 2, "waxed_cut_copper": ("cut_copper",) * 2, "waxed_oxidized_cut_copper": ("oxidized_cut_copper",) * 2,
             "smooth_sandstone": ("sandstone_top",) * 2, "black_stained_glass": ("black_stained_glass",) * 2, "brick": ("bricks",) * 2,
             "basalt": ("basalt_top", "basalt_side"), "polished_basalt": ("polished_basalt_top", "polished_basalt_side"),
             "bookshelf": ("oak_planks", "bookshelf"), "crimson": ("crimson_planks",) * 2, "warped": ("warped_planks",) * 2,
             "cherry": ("cherry_planks",) * 2, "mangrove": ("mangrove_planks",) * 2, "jungle": ("jungle_planks",) * 2, "birch": ("birch_planks",) * 2,
             "lantern": ("lantern",) * 2, "end_rod": ("end_rod",) * 2, "iron_bars": ("iron_bars",) * 2, "chain": ("chain",) * 2,
             "redstone_lamp": ("redstone_lamp_on",) * 2, "shroomlight": ("shroomlight",) * 2, "ochre_froglight": ("ochre_froglight_top", "ochre_froglight_side"),
             "pearlescent_froglight": ("pearlescent_froglight_top", "pearlescent_froglight_side"), "verdant_froglight": ("verdant_froglight_top", "verdant_froglight_side"),
             "calcite": ("calcite",) * 2, "dripstone_block": ("dripstone_block",) * 2, "packed_ice": ("packed_ice",) * 2, "blue_ice": ("blue_ice",) * 2,
             "potted_spruce_sapling": ("spruce_sapling",) * 2, "flower_pot": ("flower_pot",) * 2}
    cands = []
    if base in alias:
        cands.append(alias[base])
    if name in alias:
        cands.append(alias[name])
    cands += [(base + "_top", base + "_side"), (base, base), (base + "_top", base), (base + "s", base + "s"), (base + "_block", base + "_block")]
    for top, side in cands:
        t, s = load("mc:" + top), load("mc:" + side)
        if t is not None and s is not None:
            opaque = not any(k in name for k in ("glass", "pane", "lantern", "bars", "chain", "rod", "leaves", "sapling", "pot", "fence", "wall", "carpet", "slab", "stairs", "button", "trapdoor", "water"))
            return t, s, h, opaque
    return None, None, h, False


TILE = 10  # medio ancho del rombo en píxeles


def face_imgs(top, side, h, color):
    """Rombo de arriba y los dos lados (ya deformados y sombreados)."""
    a = TILE
    if top is None:
        top = Image.new("RGBA", (16, 16), color + (255,))
        side = top
    sz = (2 * a, 2 * a + int(2 * a * h) + 2)
    t = top.resize((32, 32), Image.NEAREST)
    # arriba: rombo (de un cuadrado girado 45° y aplastado)
    topimg = t.rotate(45, expand=True, resample=Image.NEAREST).resize((2 * a, a), Image.NEAREST)
    s = side.resize((32, 32), Image.NEAREST)
    hh = max(1, int(round(2 * a * h * 0.5)) * 2 // 2)
    hpx = int(round(a * 1.2 * h)) if h < 1 else int(a * 1.2)

    def skew(img, left, shade):
        w = a
        img = img.resize((w, max(1, hpx)), Image.NEAREST)
        out = Image.new("RGBA", (w, max(1, hpx) + a // 2 + 1), (0, 0, 0, 0))
        for x in range(w):
            col = img.crop((x, 0, x + 1, img.height))
            dy = (x // 2) if left else ((w - 1 - x) // 2)
            out.paste(col, (x, dy))
        px = out.load()
        for y in range(out.height):
            for x in range(out.width):
                r, g, b, al = px[x, y]
                if al:
                    px[x, y] = (int(r * shade), int(g * shade), int(b * shade), al)
        return out

    left = skew(s, True, 0.80)   # cara +z (sur)
    right = skew(s, False, 0.62)  # cara +x (este)
    return topimg, left, right, hpx


_faces = {}


def render_iso(bp, out, angle=0, ymax=None):
    W, H, D = bp["W"], bp["H"], bp["D"]
    cells = bp["cells"]

    def rot(x, z):
        # gira la vista: 0 mira desde el sureste
        if angle == 0:
            return x, z
        if angle == 1:
            return D - 1 - z, x
        if angle == 2:
            return W - 1 - x, D - 1 - z
        return z, W - 1 - x

    a = TILE
    hp = int(a * 1.2)
    RW, RD = (W, D) if angle % 2 == 0 else (D, W)
    img_w = (RW + RD) * a + 4 * a
    img_h = (RW + RD) * a // 2 + H * hp + 4 * a
    img = Image.new("RGBA", (img_w, img_h), (190, 215, 240, 255))
    ox = RD * a + 2 * a
    oy = H * hp + 2 * a
    items = []
    occ = {}
    for (x, y, z), spec in cells.items():
        if ymax is not None and y > ymax:
            continue
        ns, name, props = parse(spec)
        if name == "air":
            continue
        rx, rz = rot(x, z)
        items.append((rx, y, rz, ns, name, props))
        t = textures(ns, name, props)
        occ[(rx, y, rz)] = t[3]
    items.sort(key=lambda it: (it[0] + it[2], it[1]))
    for rx, y, rz, ns, name, props in items:
        key = (ns, name)
        if key not in _faces:
            top, side, h, opaque = textures(ns, name, props)
            color = COLORS.get(name, (255, 0, 255) if top is None and ns == "minecraft" else (150, 100, 160))
            _faces[key] = face_imgs(top, side, h, color) + (h,)
        topimg, left, right, hpx, h = _faces[key]
        sx = ox + (rx - rz) * a
        sy = oy + (rx + rz) * a // 2 - y * hp
        # caras laterales solo si el vecino no tapa
        ytop = sy - hpx
        if not occ.get((rx, y, rz + 1)):
            img.alpha_composite(left, (sx - a, ytop + a // 2))
        if not occ.get((rx + 1, y, rz)):
            img.alpha_composite(right, (sx, ytop + a // 2))
        if not occ.get((rx, y + 1, rz)) or h < 1:
            img.alpha_composite(topimg, (sx - a, ytop))
    for kind, x, y, z, d in bp["machines"]:
        rx, rz = rot(x, z)
        sx = ox + (rx - rz) * a
        sy = oy + (rx + rz) * a // 2 - y * hp
        dr = ImageDraw.Draw(img)
        c = COLORS.get(kind.lower(), (255, 255, 0))
        dr.rectangle((sx - 4, sy - 2 * hp, sx + 4, sy - hp), fill=c + (255,), outline=(255, 255, 0, 255))
    bbox = img.getbbox()
    img.convert("RGB").save(out, quality=90)


def avg(img):
    if img is None:
        return None
    px = [p for p in img.getdata() if p[3] > 0]
    if not px:
        return None
    return tuple(sum(c[i] for c in px) // len(px) for i in range(3))


_avg = {}


def color_of(spec):
    ns, name, props = parse(spec)
    if spec not in _avg:
        top, side, h, op = textures(ns, name, props)
        c = avg(top) or COLORS.get(name) or (255, 0, 255)
        _avg[spec] = c
    return _avg[spec]


def render_cut(bp, y, out, scale=12):
    """Planta de un piso: el bloque más alto entre y y y+2 (muebles encima del piso)."""
    W, D = bp["W"], bp["D"]
    img = Image.new("RGB", (W * scale, D * scale), (30, 30, 30))
    dr = ImageDraw.Draw(img)
    cells = bp["cells"]
    for x in range(W):
        for z in range(D):
            c = None
            for yy in (y + 2, y + 1, y):
                s = cells.get((x, yy, z))
                if s and not s.startswith("minecraft:air"):
                    c = color_of(s)
                    if yy > y:  # mueble/pared: más claro con borde
                        dr.rectangle((x * scale, z * scale, x * scale + scale - 1, z * scale + scale - 1), fill=c, outline=(0, 0, 0))
                    else:
                        dr.rectangle((x * scale, z * scale, x * scale + scale - 1, z * scale + scale - 1), fill=tuple(v // 2 + 20 for v in c))
                    break
    for kind, x, my, z, d in bp["machines"]:
        if abs(my - (y + 1)) <= 1:
            c = COLORS.get(kind.lower(), (255, 255, 0))
            dr.rectangle((x * scale + 2, z * scale + 2, x * scale + scale - 3, z * scale + scale - 3), fill=c, outline=(255, 255, 0))
            dx, dz = [(0, -1), (1, 0), (0, 1), (-1, 0)][d]
            cx, cz = x * scale + scale // 2, z * scale + scale // 2
            dr.line((cx, cz, cx + dx * scale, cz + dz * scale), fill=(255, 255, 0), width=2)
    for name, x, my, z, d in bp["marks"]:
        if abs(my - (y + 1)) <= 1:
            dr.ellipse((x * scale + 2, z * scale + 2, x * scale + scale - 3, z * scale + scale - 3), fill=(0, 200, 255))
    for i in range(0, W, 5):
        dr.text((i * scale + 1, 1), str(i), fill=(255, 255, 255))
    for i in range(0, D, 5):
        dr.text((1, i * scale + 1), str(i), fill=(255, 255, 255))
    img.save(out)


def read_bp(path):
    with gzip.open(path, "rt", encoding="utf-8") as f:
        lines = f.read().split("\n")
    _, W, H, D = lines[0].split()
    W, H, D = int(W), int(H), int(D)
    n = int(lines[1].split()[1])
    pal = [None] + lines[2:2 + n]
    i = 2 + n
    assert lines[i] == "C"
    toks = lines[i + 1].split()
    cells = {}
    k = 0
    for t in toks:
        if "*" in t:
            v, c = t.split("*")
            v, c = int(v), int(c)
        else:
            v, c = int(t), 1
        for _ in range(c):
            if v:
                y, rem = divmod(k, W * D)
                z, x = divmod(rem, W)
                cells[(x, y, z)] = pal[v]
            k += 1
    i += 2
    m = int(lines[i].split()[1])
    machines = []
    for j in range(m):
        kd, x, y, z, d = lines[i + 1 + j].split()
        machines.append((kd, int(x), int(y), int(z), int(d)))
    i += 1 + m
    q = int(lines[i].split()[1])
    marks = []
    for j in range(q):
        nm, x, y, z, d = lines[i + 1 + j].split()
        marks.append((nm, int(x), int(y), int(z), int(d)))
    return {"W": W, "H": H, "D": D, "cells": cells, "machines": machines, "marks": marks}


if __name__ == "__main__":
    bp = read_bp(sys.argv[1])
    pre = sys.argv[2]
    cuts = []
    angle = 0
    args = sys.argv[3:]
    for j, a in enumerate(args):
        if a == "--cut":
            cuts = [int(v) for v in args[j + 1].split(",")]
        if a == "--angle":
            angle = int(args[j + 1])
    render_iso(bp, pre + f"_iso{angle}.jpg", angle)
    for y in cuts:
        render_cut(bp, y, pre + f"_y{y}.png")
