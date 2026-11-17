"""Dibuja un jugador (cajas) con una pose y el celular 3D en la mano, usando la misma cadena de transformaciones que el juego."""
import json, math, os, sys
import numpy as np
from PIL import Image
import hand
from java_check import aim_java

MOD = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "assets", "celular")
base = json.load(open(f"{MOD}/models/item/phone3d/base.json"))
disp = base["display"]["thirdperson_righthand"]

def tex(path):
    im = np.array(Image.open(f"{MOD}/textures/{path}.png").convert("RGBA")); return im

FACE = {"down": [(0,0,1),(0,0,0),(1,0,0),(1,0,1)], "up": [(0,1,0),(0,1,1),(1,1,1),(1,1,0)],
        "north": [(1,1,0),(1,0,0),(0,0,0),(0,1,0)], "south": [(0,1,1),(0,0,1),(1,0,1),(1,1,1)],
        "west": [(0,1,0),(0,0,0),(0,0,1),(0,1,1)], "east": [(1,1,1),(1,0,1),(1,0,0),(1,1,0)]}

def phone_quads(case, screen, M):
    texs = {"#case": tex(f"item/phone3d/{case}"), "#screen": tex(f"item/phone3d/screen/{screen}")}
    out = []
    for el in base["elements"]:
        a, b = el["from"], el["to"]
        for fn, fd in el["faces"].items():
            pts = np.array([[(b if c[0] else a)[0], (b if c[1] else a)[1], (b if c[2] else a)[2]] for c in FACE[fn]], float) / 16.0
            u1, v1, u2, v2 = fd["uv"]
            uv = np.array([[u1,v1],[u1,v2],[u2,v2],[u2,v1]], float) / 16.0
            P = (M @ np.c_[pts - 0.5, np.ones(4)].T).T[:, :3] * 16     # píxeles del modelo del jugador
            out.append((P, uv, texs[fd["texture"]]))
    return out

def box(x0, y0, z0, x1, y1, z1, M, col):
    out = []
    for fn, f in FACE.items():
        pts = np.array([[(x1 if c[0] else x0), (y1 if c[1] else y0), (z1 if c[2] else z0)] for c in f], float)
        P = (M @ np.c_[pts / 16, np.ones(4)].T).T[:, :3] * 16
        im = np.zeros((1, 1, 4), np.uint8); im[0, 0] = col
        out.append((P, np.zeros((4, 2)), im))
    return out

def part(px, py, pz, rot):
    xr, yr, zr = rot
    return hand.T(px/16, py/16, pz/16) @ hand.Rz(zr) @ hand.Ry(yr) @ hand.Rx(xr)

ear_disp = json.load(open(f"{MOD}/models/item/phone3d/base_ear.json"))["display"]["thirdperson_righthand"]

def player(arm_r, arm_l, head=(0, 0, 0), screen="messages", case="phone_red", phone_left=False):
    q = []
    skin, shirt, pants, hair = (204, 150, 110, 255), (40, 170, 190, 255), (60, 60, 160, 255), (70, 45, 30, 255)
    Mh = part(0, 0, 0, head)
    q += box(-4, -8, -4, 4, 0, 4, Mh, skin)
    q += box(-4.05, -8.05, -1, 4.05, -5, 4.05, Mh, hair)          # pelo
    q += box(-3, -4.6, -4.06, -1, -3.6, -4.0, Mh, (255, 255, 255, 255))   # ojos
    q += box(1, -4.6, -4.06, 3, -3.6, -4.0, Mh, (255, 255, 255, 255))
    q += box(-4, 0, -2, 4, 12, 2, np.eye(4), shirt)
    Mr = part(-5, 2, 0, arm_r); Ml = part(5, 2, 0, arm_l)
    q += box(-3, -2, -2, 1, 10, 2, Mr, skin); q += box(-3, -2, -2, 1, 4, 2, Mr, shirt)
    q += box(-1, -2, -2, 3, 10, 2, Ml, skin); q += box(-1, -2, -2, 3, 4, 2, Ml, shirt)
    q += box(-4, 12, -2, 0, 24, 2, np.eye(4), pants); q += box(0, 12, -2, 4, 24, 2, np.eye(4), pants)
    dd = ear_disp if screen == "ear" else disp
    Mp = hand.chain(arm_l if phone_left else arm_r, tuple(dd["rotation"]), tuple(dd["translation"]), dd["scale"][0], not phone_left)
    q += phone_quads(case, screen, Mp)
    return q

def render(quads, yaw, pitch, size=420):
    img = np.zeros((size, size, 4), np.float32); zb = np.full((size, size), -1e9, np.float32)
    a, b = math.radians(yaw), math.radians(pitch)
    Ry = np.array([[math.cos(a), 0, math.sin(a)], [0, 1, 0], [-math.sin(a), 0, math.cos(a)]])
    Rx = np.array([[1, 0, 0], [0, math.cos(b), -math.sin(b)], [0, math.sin(b), math.cos(b)]])
    V = Rx @ Ry
    sc = size / 40.0
    light = np.array([0.3, 0.8, 0.5]); light /= np.linalg.norm(light)
    for P, uv, im in quads:
        W = np.stack([P[:, 0], -P[:, 1], -P[:, 2]], 1)           # modelo (y abajo, frente -z) -> mundo
        p = W @ V.T
        n = np.cross(p[1] - p[0], p[2] - p[1])
        if np.linalg.norm(n) < 1e-9 or n[2] <= 0:
            continue
        shade = 0.55 + 0.45 * abs(np.dot((np.cross(W[1] - W[0], W[2] - W[1])) / (np.linalg.norm(np.cross(W[1] - W[0], W[2] - W[1])) + 1e-9), light))
        scr = np.stack([size / 2 + p[:, 0] * sc, size * 0.42 - p[:, 1] * sc], 1); z = p[:, 2]
        for tri in ((0, 1, 2), (0, 2, 3)):
            A, B, C = scr[list(tri)]
            x0, x1 = int(max(0, min(A[0], B[0], C[0]))), int(min(size - 1, max(A[0], B[0], C[0])))
            y0, y1 = int(max(0, min(A[1], B[1], C[1]))), int(min(size - 1, max(A[1], B[1], C[1])))
            if x1 < x0 or y1 < y0:
                continue
            d = (B[1] - C[1]) * (A[0] - C[0]) + (C[0] - B[0]) * (A[1] - C[1])
            if abs(d) < 1e-9:
                continue
            X, Y = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            l1 = ((B[1] - C[1]) * (X - C[0]) + (C[0] - B[0]) * (Y - C[1])) / d
            l2 = ((C[1] - A[1]) * (X - C[0]) + (A[0] - C[0]) * (Y - C[1])) / d
            l3 = 1 - l1 - l2
            inside = (l1 >= -1e-6) & (l2 >= -1e-6) & (l3 >= -1e-6)
            if not inside.any():
                continue
            ia, ib, ic = tri
            zz = l1 * z[ia] + l2 * z[ib] + l3 * z[ic]
            u = l1 * uv[ia, 0] + l2 * uv[ib, 0] + l3 * uv[ic, 0]; v = l1 * uv[ia, 1] + l2 * uv[ib, 1] + l3 * uv[ic, 1]
            h, w = im.shape[:2]
            tx = np.clip((u * w).astype(int), 0, w - 1); ty = np.clip((v * h).astype(int), 0, h - 1)
            col = im[ty, tx].astype(np.float32)
            sub = zb[y0:y1 + 1, x0:x1 + 1]
            ok = inside & (zz > sub) & (col[..., 3] > 8)
            sub[ok] = zz[ok]
            c = col.copy(); c[..., :3] *= shade
            img[y0:y1 + 1, x0:x1 + 1][ok] = c[ok]
    bg = np.zeros((size, size, 3), np.float32); bg[...] = (120, 170, 220)
    a = img[..., 3:4] / 255.0
    return (bg * (1 - a) + img[..., :3] * a).astype(np.uint8)

def poses():
    out = []
    # escribir (las dos manos)
    def text_arm(right):
        ax = -5 if right else 5; bx = -2.2 if right else 2.2; by, bz = 5.5, -7.5
        return aim_java(right, bx - ax, by - 2, bz - 0, -bx, -4.5 - by, -4 - bz)
    out.append(("escribir", text_arm(True), text_arm(False), "messages"))
    out.append(("llamada", aim_java(True, -0.35, -1, -0.2, 1, 0, 0), (0, 0, 0), "ear"))
    l = (0, 0, -1.)
    out.append(("linterna", aim_java(True, *l, 0.4, -1, 0), (0, 0, 0), "flash"))
    def photo_arm(right):
        i = 1 if right else -1; ax = -5 if right else 5
        dx, dy, dz = l[0] + 0.25 * i, l[1] - 0.35, l[2]; dl = math.sqrt(dx * dx + dy * dy + dz * dz)
        hx, hy, hz = ax + dx / dl * 10, 2 + dy / dl * 10, dz / dl * 10
        return aim_java(right, dx, dy, dz, -hx, -4.5 - hy, -4 - hz)
    out.append(("foto", photo_arm(True), photo_arm(False), "camera"))
    out.append(("en la mano", (-0.314, 0, 0), (0, 0, 0), "lock"))
    return out

if __name__ == "__main__":
    tiles = []
    for name, ar, al, scr in poses():
        q = player(ar, al, screen=scr)
        tiles.append(np.concatenate([render(q, 35, 12), render(q, -40, 15)], 0))
    Image.fromarray(np.concatenate(tiles, 1)).save(sys.argv[1] if len(sys.argv) > 1 else "poses.png")
