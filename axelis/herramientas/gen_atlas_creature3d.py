# Herramienta (no va en el mod): genera textures/vfx/creature3d.png (256x256) para las mascotas 3D.
# Misma distribución de celdas que la versión de 128 (las coordenadas UV del código no cambian), con más detalle:
# iris con degradado, fibras y sombra del párpado, perla brillante, ojo cerrado afinado, boca y rubor suaves.
# Los brillos blancos del ojo grande los pone el motor encima (así no se tiñen con el color del iris).
# Uso: python3 gen_atlas_creature3d.py <salida.png>
import sys
import numpy as np
from PIL import Image

SS = 8           # supermuestreo
N = 256          # tamaño final
W = N * SS
img = np.zeros((W, W, 4), dtype=np.float64)


def ss(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def cell(x0, y0, x1, y1):
    """Coordenadas normalizadas (-1..1, y hacia arriba) de una celda en píxeles finales."""
    X0, Y0, X1, Y1 = x0 * SS, y0 * SS, x1 * SS, y1 * SS
    ys, xs = np.mgrid[Y0:Y1, X0:X1].astype(np.float64)
    u = (xs + 0.5 - X0) / (X1 - X0) * 2 - 1
    v = -((ys + 0.5 - Y0) / (Y1 - Y0) * 2 - 1)
    return (slice(Y0, Y1), slice(X0, X1)), u, v


def put(sl, rgb, a):
    """Pinta encima (alfa normal) en la región."""
    reg = img[sl]
    a = np.clip(a, 0, 1)[..., None]
    col = np.zeros(reg.shape[:2] + (3,))
    col[...] = rgb if np.ndim(rgb) == 1 else rgb
    out_a = a + reg[..., 3:4] * (1 - a)
    out_rgb = np.where(out_a > 1e-6, (col * a + reg[..., :3] * reg[..., 3:4] * (1 - a)) / np.maximum(out_a, 1e-6), 0)
    reg[..., :3] = out_rgb
    reg[..., 3:4] = out_a


def disc(u, v, cx, cy, r, soft=0.02):
    d = np.sqrt((u - cx) ** 2 + (v - cy) ** 2)
    return 1 - ss(r - soft, r + soft, d)


def ell(u, v, cx, cy, rx, ry, rot=0.0, soft=0.03):
    c, s = np.cos(rot), np.sin(rot)
    x, y = (u - cx) * c + (v - cy) * s, -(u - cx) * s + (v - cy) * c
    d = np.sqrt((x / rx) ** 2 + (y / ry) ** 2)
    return 1 - ss(1 - soft / min(rx, ry), 1 + soft / min(rx, ry), d)


def stroke(u, v, pts, w0, w1=None, soft=0.02):
    """Trazo grueso por una polilínea (ancho de w0 a w1 a lo largo, extremos redondeados)."""
    if w1 is None:
        w1 = w0
    pts = np.array(pts, dtype=np.float64)
    seg_len = np.sqrt(((pts[1:] - pts[:-1]) ** 2).sum(1))
    total = seg_len.sum()
    best = np.full(u.shape, 1e9)
    acc = 0
    for i in range(len(pts) - 1):
        a, b = pts[i], pts[i + 1]
        ab = b - a
        L2 = max((ab ** 2).sum(), 1e-12)
        t = np.clip(((u - a[0]) * ab[0] + (v - a[1]) * ab[1]) / L2, 0, 1)
        px, py = a[0] + ab[0] * t, a[1] + ab[1] * t
        d = np.sqrt((u - px) ** 2 + (v - py) ** 2)
        along = (acc + t * seg_len[i]) / total
        w = w0 + (w1 - w0) * along if not callable(w0) else w0(along)
        best = np.minimum(best, d - w)
        acc += seg_len[i]
    return 1 - ss(-soft, soft, best)


def arc_pts(cx, cy, rx, ry, a0, a1, n=48):
    t = np.linspace(a0, a1, n)
    return list(zip(cx + rx * np.cos(t), cy + ry * np.sin(t)))


rng = np.random.default_rng(7)

# ---------------------------------------------------------------- ojo grande (iris teñible)
sl, u, v = cell(0, 0, 128, 128)
r = np.sqrt(u * u + v * v)
ang = np.arctan2(v, u)
R = 0.86
# iris: más claro abajo (luz que rebota), más oscuro arriba (sombra del párpado) y en el borde (anillo)
val = 0.62 + 0.36 * ss(0.35, -0.85, v)
val *= 1 - 0.30 * ss(0.64, 0.90, r)
val *= 1 - 0.20 * ss(0.05, 0.85, v) * ss(0.2, 0.75, r)
# fibras finas y suaves (sin contraste fuerte para que no parpadeen de lejos)
fib = 0.5 + 0.5 * np.sin(ang * 34 + 2.0 * np.sin(ang * 7)) * np.sin(ang * 13 + 1.3)
val *= 1 + 0.07 * (fib - 0.5) * ss(0.42, 0.62, r) * (1 - ss(0.80, 0.88, r))
# media luna brillante abajo
cres = ell(u, v, 0.0, -0.46, 0.50, 0.24, soft=0.20) * (1 - disc(u, v, 0.0, -0.30, 0.40, 0.18))
val += 0.22 * cres
iris = np.clip(val, 0, 1)
put(sl, np.stack([iris, iris, iris], -1), disc(u, v, 0, 0, R, 0.012))
# pupila con borde suave y un anillo más oscuro
pup = disc(u, v, 0.0, 0.03, 0.38, 0.03)
put(sl, np.array([0.03, 0.025, 0.035]), pup)
put(sl, np.array([0.10, 0.08, 0.10]), (disc(u, v, 0.0, 0.03, 0.47, 0.06) - pup).clip(0, 1) * 0.35)
# reflejo tenue dentro de la pupila (abajo a la derecha)
put(sl, np.array([0.35, 0.33, 0.38]), ell(u, v, 0.14, -0.16, 0.14, 0.08, -0.5, 0.06) * 0.55)
# contorno: más grueso arriba (línea de pestañas) y fino abajo
th = 0.05 + 0.06 * ss(-0.2, 0.9, v / np.maximum(r, 1e-6))
ring = ss(R - 0.03, R - 0.005, r) * (1 - ss(R + th - 0.014, R + th + 0.014, r))
put(sl, np.array([0.16, 0.09, 0.07]), ring)

# ---------------------------------------------------------------- ojo cerrado (arco feliz afinado)
sl, u, v = cell(128, 0, 256, 128)
pts = arc_pts(0, -0.42, 0.70, 0.52, np.pi * 0.92, np.pi * 0.08)
put(sl, np.array([0.18, 0.10, 0.07]), stroke(u, v, pts, lambda t: 0.035 + 0.075 * np.abs(np.sin(np.pi * t)) ** 0.7, soft=0.02))

# ---------------------------------------------------------------- rubor
sl, u, v = cell(0, 128, 128, 256)
r = np.sqrt((u / 1.0) ** 2 + (v / 0.82) ** 2)
a = (1 - ss(0.05, 0.92, r)) ** 1.4 * 0.92
put(sl, np.array([1.0, 0.56, 0.54]), a)
put(sl, np.array([1.0, 0.80, 0.78]), ell(u, v, -0.22, 0.20, 0.20, 0.11, 0.3, 0.12) * 0.35)

# ---------------------------------------------------------------- perla negra (ojo pequeño)
sl, u, v = cell(128, 128, 192, 192)
r = np.sqrt(u * u + v * v)
base = 0.05 + 0.13 * ss(0.0, -0.9, v) * ss(-0.6, 0.6, u)
put(sl, np.stack([base * 1.05 + 0.01, base * 0.85 + 0.01, base * 1.2 + 0.015], -1), disc(u, v, 0, 0, 0.90, 0.03))
put(sl, np.array([0.30, 0.26, 0.36]), ell(u, v, 0.18, -0.48, 0.42, 0.18, -0.25, 0.14) * 0.55 * (1 - disc(u, v, 0.10, -0.15, 0.48, 0.2)))
put(sl, np.array([1, 1, 1]), ell(u, v, -0.30, 0.34, 0.27, 0.22, 0.5, 0.05))
put(sl, np.array([1, 1, 1]), disc(u, v, 0.34, -0.30, 0.11, 0.04))
put(sl, np.array([1, 1, 1]), disc(u, v, 0.12, 0.44, 0.06, 0.03) * 0.85)
put(sl, np.array([0.14, 0.08, 0.07]), (ss(0.80, 0.86, r) * (1 - ss(0.90, 0.95, r))) * 0.8)

# ---------------------------------------------------------------- boca (sonrisa)
sl, u, v = cell(192, 128, 256, 192)
pts = arc_pts(0, 0.30, 0.56, 0.52, -np.pi * 0.86, -np.pi * 0.14)
put(sl, np.array([0.28, 0.12, 0.08]), stroke(u, v, pts, lambda t: 0.045 + 0.06 * np.abs(np.sin(np.pi * t)) ** 0.6, soft=0.03))

# ---------------------------------------------------------------- boca de gato (ω)
sl, u, v = cell(192, 192, 256, 256)
col = np.array([0.30, 0.13, 0.08])
for sd in (-1, 1):
    put(sl, col, stroke(u, v, arc_pts(sd * 0.30, -0.02, 0.30, 0.36, -np.pi * 0.98, -np.pi * 0.02), 0.07, soft=0.03))
put(sl, col, stroke(u, v, [(0, 0.40), (0, -0.02)], 0.065, soft=0.03))
put(sl, np.array([0.92, 0.42, 0.48]), ell(u, v, 0.0, -0.40, 0.17, 0.15, 0, 0.06) * (v < -0.2))

# ---------------------------------------------------------------- disco duro, disco suave, destello, hoja
sl, u, v = cell(128, 192, 160, 224)
put(sl, np.array([1, 1, 1]), disc(u, v, 0, 0, 0.86, 0.05))
sl, u, v = cell(160, 192, 192, 224)
r = np.sqrt(u * u + v * v)
put(sl, np.array([1, 1, 1]), (1 - ss(0.0, 0.95, r)) ** 1.6)
sl, u, v = cell(128, 224, 160, 256)
ax, ay = np.abs(u), np.abs(v)
star = np.maximum(1 - ss(0.0, 0.12, ax * 1.0 + ay * 0.13 - 0.02) , 1 - ss(0.0, 0.12, ay * 1.0 + ax * 0.13 - 0.02))
star = np.maximum(star * (1 - ss(0.80, 0.95, np.maximum(ax, ay))), disc(u, v, 0, 0, 0.18, 0.08))
put(sl, np.array([1, 1, 1]), star)
sl, u, v = cell(160, 224, 192, 256)
put(sl, np.array([1, 1, 1]), ell(u, v, 0, 0, 0.36, 0.86, 0, 0.04))
put(sl, np.array([0.82, 0.86, 0.82]), stroke(u, v, [(0, -0.8), (0, 0.7)], 0.035, soft=0.03) * ell(u, v, 0, 0, 0.36, 0.86, 0, 0.04))

# ---------------------------------------------------------------- blanco sólido (centro del disco): T_WHITE
img[205 * SS:212 * SS, 140 * SS:148 * SS] = [1, 1, 1, 1]

# reducir con promedio de caja (antialias)
small = img.reshape(N, SS, N, SS, 4)
a = small[..., 3:4].mean(axis=(1, 3))
rgb = (small[..., :3] * small[..., 3:4]).sum(axis=(1, 3)) / np.maximum(small[..., 3:4].sum(axis=(1, 3)), 1e-9)
out = np.concatenate([rgb, a], -1)
Image.fromarray((np.clip(out, 0, 1) * 255 + 0.5).astype(np.uint8), 'RGBA').save(sys.argv[1])
print('ok', sys.argv[1])
