#!/usr/bin/env python3
"""Dibuja un modelo JSON de bloque/item de Minecraft 1.20.1 en una imagen PNG.
Uso: python3 tools/preview_model.py <carpeta-del-mod> <modelo> [salida.png] [rotX rotY]
  <modelo> = por ejemplo casino:block/cashier  o  casino:item/plinko
Por defecto dibuja la vista de inventario (rotacion 30,225) y una vista trasera (30,45) lado a lado.
Soporta: elements, from/to, rotation del elemento, uv, rotation de cara, parent (del mod y cube/cube_all/cube_column basicos),
variables de textura (#nombre), texturas no cuadradas. No dibuja: entidades (BlockEntityRenderer), luces, animaciones."""
import json, math, os, sys
import numpy as np
from PIL import Image

mod = sys.argv[1]; start = sys.argv[2]
out = sys.argv[3] if len(sys.argv) > 3 else "preview.png"
views = [(30, 225), (30, 45)]
if len(sys.argv) > 5: views = [(float(sys.argv[4]), float(sys.argv[5]))]
assets = os.path.join(mod, "src/main/resources/assets")

def split(r, d="minecraft"): return r.split(":", 1) if ":" in r else (d, r)
def mpath(ref):
    ns, p = split(ref); return os.path.join(assets, ns, "models", p + ".json")

BUILTIN = {
 "minecraft:block/cube_all": {"elements": [{"from":[0,0,0],"to":[16,16,16],"faces":{f:{"texture":"#all"} for f in ["down","up","north","south","west","east"]}}]},
 "minecraft:block/cube": {"elements": [{"from":[0,0,0],"to":[16,16,16],"faces":{f:{"texture":"#"+f} for f in ["down","up","north","south","west","east"]}}]},
 "minecraft:block/cube_column": {"elements": [{"from":[0,0,0],"to":[16,16,16],"faces":{"down":{"texture":"#end"},"up":{"texture":"#end"},"north":{"texture":"#side"},"south":{"texture":"#side"},"west":{"texture":"#side"},"east":{"texture":"#side"}}}]},
 "minecraft:block/block": {}, "minecraft:block/thin_block": {},
}
def resolve(ref):
    ns, p = split(ref); full = f"{ns}:{p}"
    if full in BUILTIN: return dict(BUILTIN[full]), {}
    f = mpath(full)
    if not os.path.isfile(f): raise SystemExit(f"modelo no encontrado: {full}")
    m = json.load(open(f, encoding="utf-8"))
    textures = dict(m.get("textures", {})); elements = m.get("elements")
    if "parent" in m and m["parent"] not in ("builtin/generated",):
        pm, pt = resolve(m["parent"])
        textures = {**pt, **textures}
        if elements is None: elements = pm.get("elements")
    return {"elements": elements}, textures

model, textures = resolve(start)
elements = model.get("elements") or []
if not elements:
    raise SystemExit("el modelo no tiene 'elements' (es un item plano generated o vacio)")

def tex(ref, depth=0):
    while ref.startswith("#") and depth < 10:
        ref = textures.get(ref[1:], ""); depth += 1
    if not ref or ref.startswith("#"): return None
    ns, p = split(ref); f = os.path.join(assets, ns, "textures", p + ".png")
    if not os.path.isfile(f): return None
    im = Image.open(f).convert("RGBA")
    if im.height > im.width and im.height % im.width == 0:  # animada: primer cuadro
        im = im.crop((0, 0, im.width, im.width))
    return np.array(im)
cache = {}

FACE = {  # vertices (x,y,z) como indices en from/to: 0=min,1=max
 "down": [(0,0,1),(0,0,0),(1,0,0),(1,0,1)], "up": [(0,1,0),(0,1,1),(1,1,1),(1,1,0)],
 "north": [(1,1,0),(1,0,0),(0,0,0),(0,1,0)], "south": [(0,1,1),(0,0,1),(1,0,1),(1,1,1)],
 "west": [(0,1,0),(0,0,0),(0,0,1),(0,1,1)], "east": [(1,1,1),(1,0,1),(1,0,0),(1,1,0)]}
SHADE = {"up":1.0,"down":0.5,"north":0.8,"south":0.8,"west":0.6,"east":0.6}
def default_uv(f, a, b):
    x1,y1,z1 = a; x2,y2,z2 = b
    return {"down":[x1,16-z2,x2,16-z1],"up":[x1,z1,x2,z2],"north":[16-x2,16-y2,16-x1,16-y1],
            "south":[x1,16-y2,x2,16-y1],"west":[z1,16-y2,z2,16-y1],"east":[16-z2,16-y2,16-z1,16-y1]}[f]

quads = []  # (4 puntos 3D, 4 uv (0..1), textura, sombra)
for el in elements:
    a, b = el["from"], el["to"]
    for fname, fd in el.get("faces", {}).items():
        t = fd.get("texture", "")
        if t not in cache: cache[t] = tex(t)
        im = cache[t]
        if im is None: continue
        pts = np.array([[ (b if c[0] else a)[0], (b if c[1] else a)[1], (b if c[2] else a)[2]] for c in FACE[fname]], float)
        u1,v1,u2,v2 = fd.get("uv", default_uv(fname, a, b))
        uv = np.array([[u1,v1],[u1,v2],[u2,v2],[u2,v1]], float) / 16.0
        r = fd.get("rotation", 0) // 90 % 4
        uv = np.roll(uv, -r, axis=0)
        rot = el.get("rotation")
        if rot:
            o = np.array(rot["origin"], float); ang = math.radians(rot["angle"]); ax = rot["axis"]
            c, s = math.cos(ang), math.sin(ang); q = pts - o
            if ax == "x": q = np.stack([q[:,0], q[:,1]*c - q[:,2]*s, q[:,1]*s + q[:,2]*c], 1)
            if ax == "y": q = np.stack([q[:,0]*c + q[:,2]*s, q[:,1], -q[:,0]*s + q[:,2]*c], 1)
            if ax == "z": q = np.stack([q[:,0]*c - q[:,1]*s, q[:,0]*s + q[:,1]*c, q[:,2]], 1)
            pts = q + o
        quads.append((pts, uv, im, SHADE[fname]))

SIZE = 384
def render(rx, ry):
    img = np.zeros((SIZE, SIZE, 4), np.float32); zb = np.full((SIZE, SIZE), -1e9, np.float32)
    ax, ay = math.radians(rx), math.radians(ry)
    Rx = np.array([[1,0,0],[0,math.cos(ax),-math.sin(ax)],[0,math.sin(ax),math.cos(ax)]])
    Ry = np.array([[math.cos(ay),0,math.sin(ay)],[0,1,0],[-math.sin(ay),0,math.cos(ay)]])
    M = Rx @ Ry
    allp = np.concatenate([((q[0] - 8) @ M.T)[:, :2] for q in quads]) if quads else np.zeros((1, 2))
    lo, hi = allp.min(0), allp.max(0)
    sc = min(SIZE * 0.9 / max(hi[0] - lo[0], 1e-6), SIZE * 0.9 / max(hi[1] - lo[1], 1e-6), SIZE / 24.0 * 2.2)
    mid = (lo + hi) / 2
    for pts, uv, im, sh in quads:
        p = ((pts - 8) @ M.T)
        scr = np.stack([SIZE/2 + (p[:,0]-mid[0])*sc, SIZE/2 - (p[:,1]-mid[1])*sc], 1); z = p[:,2]
        # descarta caras traseras
        n = np.cross(p[1]-p[0], p[2]-p[1])
        if n[2] <= 0: continue
        for tri in ((0,1,2),(0,2,3)):
            A,B,C = scr[list(tri)]
            x0,x1 = int(max(0, min(A[0],B[0],C[0]))), int(min(SIZE-1, max(A[0],B[0],C[0])))
            y0,y1 = int(max(0, min(A[1],B[1],C[1]))), int(min(SIZE-1, max(A[1],B[1],C[1])))
            if x1 < x0 or y1 < y0: continue
            d = (B[1]-C[1])*(A[0]-C[0]) + (C[0]-B[0])*(A[1]-C[1])
            if abs(d) < 1e-9: continue
            X, Y = np.meshgrid(np.arange(x0, x1+1)+0.5, np.arange(y0, y1+1)+0.5)
            l1 = ((B[1]-C[1])*(X-C[0]) + (C[0]-B[0])*(Y-C[1])) / d
            l2 = ((C[1]-A[1])*(X-C[0]) + (A[0]-C[0])*(Y-C[1])) / d
            l3 = 1 - l1 - l2
            inside = (l1 >= -1e-6) & (l2 >= -1e-6) & (l3 >= -1e-6)
            if not inside.any(): continue
            ia,ib,ic = tri
            zz = l1*z[ia] + l2*z[ib] + l3*z[ic]
            u = l1*uv[ia,0] + l2*uv[ib,0] + l3*uv[ic,0]; v = l1*uv[ia,1] + l2*uv[ib,1] + l3*uv[ic,1]
            h, w = im.shape[:2]
            tx = np.clip((u*w).astype(int), 0, w-1); ty = np.clip((v*h).astype(int), 0, h-1)
            col = im[ty, tx].astype(np.float32)
            sub = zb[y0:y1+1, x0:x1+1]
            ok = inside & (zz > sub) & (col[...,3] > 8)
            sub[ok] = zz[ok]
            tgt = img[y0:y1+1, x0:x1+1]
            c = col.copy(); c[...,:3] *= sh
            tgt[ok] = c[ok]
    return img

tiles = []
for rx, ry in views:
    r = render(rx, ry)
    bg = np.zeros((SIZE, SIZE, 3), np.float32)
    yy, xx = np.mgrid[0:SIZE, 0:SIZE]; chk = (((yy//16)+(xx//16)) % 2) * 20 + 70
    bg[...] = chk[..., None]
    a = (r[...,3:4] / 255.0); comp = bg*(1-a) + r[...,:3]*a
    tiles.append(comp.astype(np.uint8))
Image.fromarray(np.concatenate(tiles, 1)).save(out)
print(f"{start}: {len(quads)} caras dibujadas -> {out}")
