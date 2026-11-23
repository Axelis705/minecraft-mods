#!/usr/bin/env python3
"""Texturas de bloque entero (32x32) para el complejo del Coleccionista: una vieja institucion abandonada (mitad prision, mitad
manicomio) con las salas del Coleccionista, elegantes pero podridas.

Cada funcion devuelve una imagen PIL RGBA de 32x32 (o 32xN). Todas reciben `seed` para sacar variantes del mismo material
(el bloque elige una al azar con el estado de bloque, asi las paredes no se ven repetidas).
"""
import math

import numpy as np
from PIL import Image

N = 32


# ---------------------------------------------------------------- ruido que se repite (para que las texturas encajen entre bloques)
def _h(ix, iy, seed):
    n = (ix.astype(np.int64) * 73856093) ^ (iy.astype(np.int64) * 19349663) ^ ((seed * 2654435761) & 0xFFFFFFFF)
    n &= 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n = (n ^ (n >> 16)) & 0xFFFFFFFF
    return (n & 0xFFFF) / 65535.0


def vnoise(w, h, cell, seed, period_x=None, period_y=None):
    """Ruido de valor suave que se repite cada period (en celdas) para que la textura encaje con el bloque vecino."""
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    px = period_x or max(1, int(round(w / cell)))
    py = period_y or max(1, int(round(h / cell)))
    # la celda se ajusta para que el ruido se repita justo cada bloque (asi la textura encaja con la del bloque de al lado)
    gx, gy = x * px / w, y * py / h
    ix, iy = np.floor(gx), np.floor(gy)
    fx, fy = gx - ix, gy - iy
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)

    def g(a, b):
        return _h(np.mod(a, px), np.mod(b, py), seed)
    return (g(ix, iy) * (1 - fx) * (1 - fy) + g(ix + 1, iy) * fx * (1 - fy) + g(ix, iy + 1) * (1 - fx) * fy + g(ix + 1, iy + 1) * fx * fy)


def fbm(w, h, cell, seed, oct=3):
    t, a, tot = 0, 1.0, 0
    for o in range(oct):
        c = max(1, cell / (2 ** o))
        t = t + a * vnoise(w, h, c, seed + o * 31)
        tot += a
        a *= 0.5
    return t / tot


def white(w, h, seed):
    y, x = np.mgrid[0:h, 0:w]
    return _h(x, y, seed + 7)


def hexc(c):
    c = c.lstrip('#')
    return np.array([int(c[i:i + 2], 16) for i in (0, 2, 4)], np.float32)


def ramp(t, *cols):
    """Colores con pasos duros (pixel art): t 0..1 elige uno de la lista."""
    c = np.array([hexc(x) for x in cols], np.float32)
    i = np.clip((np.clip(t, 0, 0.9999) * len(c)).astype(int), 0, len(c) - 1)
    return c[i]


def mix(a, b, t):
    t = np.asarray(t, np.float32)
    if t.ndim == 2:
        t = t[..., None]
    return a * (1 - t) + b * t


def img(rgb, alpha=None):
    h, w = rgb.shape[:2]
    out = np.zeros((h, w, 4), np.uint8)
    out[..., :3] = np.clip(rgb, 0, 255).astype(np.uint8)
    out[..., 3] = 255 if alpha is None else np.clip(alpha, 0, 255).astype(np.uint8)
    return Image.fromarray(out, 'RGBA')


def grime(rgb, seed, amount=0.25, top=0.0, bottom=0.0, w=N, h=N):
    """Mugre: manchas suaves oscuras, mas arriba (top) o abajo (bottom)."""
    n = fbm(w, h, 12, seed + 400)
    yy = np.linspace(0, 1, h)[:, None]
    k = amount * np.clip(n - 0.35, 0, 1) * 1.6 + top * np.clip(1 - yy * 2.2, 0, 1) + bottom * np.clip(yy * 2.2 - 1.2, 0, 1)
    k = k * (0.75 + 0.5 * white(w, h, seed + 401)[...] * 0.5)
    dirt = hexc('#2a2418')
    return mix(rgb, np.broadcast_to(dirt, rgb.shape), np.clip(k, 0, 0.8))


def envejecer(rgb, seed, k=0.5, w=N, h=N):
    """Le quita lo "nuevo" a un material: menos color, tono amarillento-cafe y manchas de suciedad grandes y chicas."""
    gris = rgb.mean(axis=-1, keepdims=True)
    rgb = rgb * (1 - 0.3 * k) + gris * 0.3 * k
    rgb = rgb * (np.array([1.0, 0.96, 0.86], np.float32) * (1 - 0.12 * k) + 0.12 * k * 0.8)
    big = fbm(w, h, 16, seed + 900)
    small = fbm(w, h, 4, seed + 901)
    d = np.clip((big - 0.45) * 1.4, 0, 1) * 0.45 + np.clip((small - 0.6) * 2, 0, 1) * 0.25
    rgb = mix(rgb, np.broadcast_to(hexc('#3a3020'), rgb.shape), np.clip(d * k * 1.4, 0, 0.7))
    return rgb


def streaks(rgb, seed, n=3, color='#3b2f1e', strength=0.35, w=N, h=N, start=0.0):
    """Escurrimientos verticales de agua sucia u oxido (bajan desde arriba)."""
    r = np.random.RandomState(seed)
    out = rgb.copy()
    col = hexc(color)
    for _ in range(n):
        x = r.randint(0, w)
        wid = r.randint(1, 3)
        top = int(start * h)
        length = r.randint(h // 3, h)
        for y in range(top, min(h, top + length)):
            f = strength * (1 - (y - top) / length) * (0.6 + 0.4 * r.rand())
            for dx in range(wid):
                xx = (x + dx + (1 if r.rand() < 0.15 else 0)) % w
                out[y, xx] = out[y, xx] * (1 - f) + col * f
    return out


def cracks(rgb, seed, n=1, w=N, h=N, dark=0.45):
    r = np.random.RandomState(seed + 99)
    out = rgb.copy()
    for _ in range(n):
        x, y = r.randint(0, w), r.randint(0, h)
        ang = r.rand() * math.pi * 2
        for _ in range(r.randint(8, 20)):
            out[y % h, x % w] *= dark
            ang += (r.rand() - 0.5) * 1.2
            x += int(round(math.cos(ang)))
            y += int(round(math.sin(ang)))
    return out


# ---------------------------------------------------------------- materiales de pared
def azulejo(seed=0, color='#8aa091', sucio=0.4, faltan=0.07, bajo=0.0):
    """Azulejo de hospital viejo: cuadritos de 8 px con junta, vidriado con brillo, mugre, grietas y algunos rotos."""
    r = np.random.RandomState(seed)
    base = hexc(color)
    y, x = np.mgrid[0:N, 0:N]
    tx, ty = x // 8, y // 8
    lx, ly = x % 8, y % 8
    rgb = np.zeros((N, N, 3), np.float32)
    tile_var = {}
    for a in range(4):
        for b in range(4):
            tile_var[(a, b)] = 0.9 + r.rand() * 0.16
    v = np.vectorize(lambda a, b: tile_var[(a, b)])(tx, ty).astype(np.float32)
    rgb[:] = base * v[..., None]
    n = fbm(N, N, 8, seed + 3)
    rgb *= (0.92 + 0.14 * n)[..., None]
    # vidriado: brillo arriba-izquierda y sombra abajo-derecha de cada cuadrito
    rgb = np.where(((ly == 1) | (lx == 1))[..., None] & (lx >= 1)[..., None] & (ly >= 1)[..., None], rgb * 1.12 + 8, rgb)
    rgb = np.where(((ly == 7) | (lx == 7))[..., None], rgb * 0.82, rgb)
    # junta
    junta = (lx == 0) | (ly == 0)
    rgb = np.where(junta[..., None], hexc('#4b4a40') * (0.85 + 0.3 * white(N, N, seed)[..., None]), rgb)
    # cuadritos rotos: se ve el cemento gris de atras
    rotos = np.zeros((N, N), bool)
    for a in range(4):
        for b in range(4):
            if r.rand() < faltan:
                m = (tx == a) & (ty == b)
                hole = m & (fbm(N, N, 4, seed + a * 7 + b) > 0.35 + r.rand() * 0.2)
                rotos |= hole
    cem = ramp(fbm(N, N, 3, seed + 50) * 0.8 + white(N, N, seed + 51) * 0.2, '#3f3d38', '#4f4c45', '#5c5851', '#6a655c')
    rgb = np.where(rotos[..., None], cem, rgb)
    rgb = cracks(rgb, seed, n=r.randint(0, 2), dark=0.62)
    rgb = envejecer(rgb, seed + 6, 0.7)
    rgb = grime(rgb, seed, amount=sucio, bottom=bajo)
    rgb = streaks(rgb, seed + 5, n=r.randint(1, 4), strength=0.35)
    return img(rgb)


def yeso(seed=0, color='#a39a82', pelado=0.5, manchas=4):
    """Yeso pintado y viejo: manchas de humedad, escurrimientos y pintura descarapelada que deja ver el cemento."""
    r = np.random.RandomState(seed + 1000)
    base = hexc(color)
    n = fbm(N, N, 10, seed + 10)
    rgb = base * (0.88 + 0.18 * n)[..., None]
    rgb *= (0.96 + 0.08 * white(N, N, seed + 11))[..., None]
    # humedad: aureolas cafes
    hum = fbm(N, N, 14, seed + 12)
    rgb = mix(rgb, np.broadcast_to(hexc('#7d6a48'), rgb.shape), np.clip((hum - 0.62) * 2.2, 0, 0.55))
    # pintura levantada
    p = fbm(N, N, 9, seed + 13) * 0.75 + fbm(N, N, 3, seed + 18) * 0.25
    mask = p > (1 - pelado * 0.45)
    cem = ramp(fbm(N, N, 3, seed + 14) * 0.7 + white(N, N, seed + 15) * 0.3, '#55524b', '#625e55', '#6f6a60')
    out = np.where(mask[..., None], cem, rgb)
    # orilla de la pintura: clara arriba, sombra abajo (relieve)
    up = np.roll(mask, 1, axis=0)
    down = np.roll(mask, -1, axis=0)
    edge_shadow = mask & ~up
    edge_light = ~mask & down
    out = np.where(edge_shadow[..., None], out * 0.6, out)
    out = np.where(edge_light[..., None], np.minimum(out * 1.12 + 10, 255), out)
    out = streaks(out, seed + 16, n=manchas, color='#5a4a32', strength=0.3)
    out = cracks(out, seed + 17, n=r.randint(0, 2), dark=0.7)
    out = envejecer(out, seed + 19, 0.8)
    out = grime(out, seed + 20, amount=0.3)
    return img(out)


def remate(seed=0, azul='#8fa597', yes='#b3a88f'):
    """El paso de azulejo (abajo) a yeso (arriba): una fila de azulejo de remate oscuro en medio."""
    a = np.array(azulejo(seed, azul, sucio=0.25)).astype(np.float32)[..., :3]
    b = np.array(yeso(seed, yes, pelado=0.2)).astype(np.float32)[..., :3]
    y = np.mgrid[0:N, 0:N][0]
    out = np.where((y < 13)[..., None], b, a)
    cap = (y >= 13) & (y <= 16)
    capc = hexc('#3f5747')
    n = white(N, N, seed + 3)
    capcol = capc * (0.9 + 0.2 * n)[..., None]
    capcol = np.where((y == 13)[..., None], capcol * 1.25 + 10, capcol)
    capcol = np.where((y == 16)[..., None], capcol * 0.6, capcol)
    out = np.where(cap[..., None], capcol, out)
    out = np.where((y == 17)[..., None], out * 0.7, out)  # sombra bajo el remate
    return img(grime(out, seed + 5, amount=0.15))


def concreto(seed=0, color='#66645c', poros=0.012, cimbra=False, oxido=1):
    """Concreto: moteado, poros, juntas de la cimbra y escurrimientos de oxido."""
    r = np.random.RandomState(seed + 2000)
    n = fbm(N, N, 6, seed + 20) * 0.7 + white(N, N, seed + 21) * 0.3
    rgb = hexc(color) * (0.8 + 0.35 * n)[..., None]
    po = white(N, N, seed + 22) > 1 - poros
    rgb = np.where(po[..., None], rgb * 0.55, rgb)
    if cimbra:
        y = np.mgrid[0:N, 0:N][0]
        rgb = np.where((y == 0)[..., None], rgb * 0.78, rgb)
        rgb = np.where((y == 1)[..., None], rgb * 1.06, rgb)
    rgb = streaks(rgb, seed + 23, n=oxido, color='#6a3f22', strength=0.35)
    rgb = envejecer(rgb, seed + 26, 0.6)
    rgb = grime(rgb, seed + 24, amount=0.25)
    rgb = cracks(rgb, seed + 25, n=r.randint(0, 2), dark=0.7)
    return img(rgb)


def concreto_piso(seed=0):
    r = np.random.RandomState(seed + 3000)
    n = fbm(N, N, 8, seed + 30) * 0.7 + white(N, N, seed + 31) * 0.3
    rgb = hexc('#5b5953') * (0.82 + 0.3 * n)[..., None]
    # rayones y marcas de arrastre
    for _ in range(r.randint(1, 4)):
        y0 = r.randint(0, N)
        x0 = r.randint(0, N)
        for i in range(r.randint(6, 18)):
            xx, yy = (x0 + i) % N, (y0 + i // 4) % N
            rgb[yy, xx] = rgb[yy, xx] * 1.15 + 6
    # manchas oscuras (aceite, agua vieja)
    m = fbm(N, N, 10, seed + 32)
    rgb = mix(rgb, np.broadcast_to(hexc('#26231d'), rgb.shape), np.clip((m - 0.6) * 2.0, 0, 0.6))
    rgb = cracks(rgb, seed + 33, n=r.randint(0, 2), dark=0.65)
    rgb = envejecer(rgb, seed + 34, 0.6)
    return img(rgb)


def linoleo(seed=0, a='#d8d2c0', b='#1d1c1a'):
    """Piso de cuadros blanco y negro (16 px por cuadro) gastado: rayones, mugre en las orillas, esquinas despostilladas."""
    r = np.random.RandomState(seed + 4000)
    y, x = np.mgrid[0:N, 0:N]
    chk = ((x // 16) + (y // 16)) % 2 == 0
    ca, cb = hexc(a), hexc(b)
    n = fbm(N, N, 6, seed + 40)
    rgb = np.where(chk[..., None], ca * (0.8 + 0.2 * n)[..., None], cb * (0.9 + 0.5 * n)[..., None] + 6)
    rgb = rgb * (0.95 + 0.08 * white(N, N, seed + 41))[..., None]
    # amarillento por los anos en los blancos
    rgb = np.where(chk[..., None], mix(rgb, np.broadcast_to(hexc('#b8a37a'), rgb.shape), 0.25 * fbm(N, N, 12, seed + 42)), rgb)
    # junta
    lx, ly = x % 16, y % 16
    rgb = np.where(((lx == 0) | (ly == 0))[..., None], rgb * 0.55, rgb)
    # despostillado: se ve el pegamento negro
    chip = (fbm(N, N, 5, seed + 43) > 0.78) & chk
    rgb = np.where(chip[..., None], hexc('#2a2620'), rgb)
    rgb = envejecer(rgb, seed + 45, 0.8)
    rgb = grime(rgb, seed + 44, amount=0.45)
    for _ in range(r.randint(2, 6)):  # rayones finos
        y0, x0 = r.randint(0, N), r.randint(0, N)
        d = r.choice([-1, 1])
        for i in range(r.randint(5, 14)):
            xx, yy = (x0 + i) % N, (y0 + d * (i // 3)) % N
            rgb[yy, xx] = rgb[yy, xx] * 0.75 + 30
    return img(rgb)


def metal_placa(seed=0, pintura='#3c4c40', pintado=0.7, remaches=True):
    """Lamina de acero pintada y oxidada, con orilla y remaches en las esquinas."""
    r = np.random.RandomState(seed + 5000)
    y, x = np.mgrid[0:N, 0:N]
    steel = hexc('#4a4c4d') * (0.85 + 0.3 * fbm(N, N, 5, seed + 50))[..., None]
    paint = hexc(pintura) * (0.88 + 0.2 * fbm(N, N, 9, seed + 51))[..., None]
    pm = fbm(N, N, 6, seed + 52) < pintado
    rgb = np.where(pm[..., None], paint, steel)
    rust = fbm(N, N, 5, seed + 53)
    rc = ramp(white(N, N, seed + 54), '#5b3218', '#7a4523', '#93582c', '#a86a36')
    rgb = np.where((rust > 0.66)[..., None], mix(rgb, rc * 0.8, np.clip((rust - 0.66) * 3, 0, 0.6)), rgb)
    # orilla de la placa
    edge = (x == 0) | (y == 0) | (x == N - 1) | (y == N - 1)
    rgb = np.where(edge[..., None], rgb * 0.55, rgb)
    rgb = np.where(((x == 1) | (y == 1))[..., None] & ~edge[..., None], rgb * 1.15 + 6, rgb)
    if remaches:
        for cx, cy in ((4, 4), (27, 4), (4, 27), (27, 27)):
            rgb[cy - 1:cy + 1, cx - 1:cx + 1] = hexc('#6b6c68')
            rgb[cy - 1, cx - 1] = hexc('#9a9a92')
            rgb[cy + 1, cx - 1:cx + 2] = hexc('#26231f')
            rgb[cy - 1:cy + 2, cx + 1] = hexc('#26231f')
    rgb = streaks(rgb, seed + 55, n=r.randint(1, 4), color='#6b3a1c', strength=0.45)
    return img(rgb)


def rejilla(seed=0):
    """Rejilla de acero para pasarelas: barras cada 4 px, agujeros transparentes."""
    y, x = np.mgrid[0:N, 0:N]
    bar = (x % 8 < 2) | (y % 4 == 0)
    frame = (x == 0) | (x == N - 1) | (y == 0) | (y == N - 1)
    solid = bar | frame
    n = fbm(N, N, 6, seed + 60)
    rgb = hexc('#4e4f4c') * (0.8 + 0.35 * n)[..., None]
    rgb = np.where(((y % 4) == 0)[..., None], rgb * 1.15 + 8, rgb)
    rust = fbm(N, N, 4, seed + 61) > 0.68
    rgb = np.where(rust[..., None], mix(rgb, np.broadcast_to(hexc('#5e3820'), rgb.shape), 0.45), rgb)
    rgb = rgb * 0.8
    return img(rgb, np.where(solid, 255, 0))


def barrotes(seed=0):
    """Barrotes oxidados (plantilla de barrotes de 16: el modelo usa la mitad central). Barras verticales y travesano."""
    y, x = np.mgrid[0:N, 0:N]
    bars = ((x >= 14) & (x <= 17)) | ((x >= 4) & (x <= 6)) | ((x >= 25) & (x <= 27))
    band = (y >= 2) & (y <= 4) | (y >= 27) & (y <= 29)
    solid = bars | band
    n = fbm(N, N, 4, seed + 70)
    rgb = hexc('#3d3b39') * (0.8 + 0.4 * n)[..., None]
    rgb = np.where(((x == 14) | (x == 4) | (x == 25))[..., None], rgb * 1.3 + 10, rgb)
    rgb = np.where(((x == 17) | (x == 6) | (x == 27))[..., None], rgb * 0.6, rgb)
    rust = fbm(N, N, 3, seed + 71)
    rc = ramp(white(N, N, seed + 72), '#4a2c1a', '#5e3820', '#704428')
    rgb = np.where((rust > 0.62)[..., None], mix(rgb, rc, 0.5), rgb)
    return img(rgb, np.where(solid, 255, 0))


def puerta(seed=0, mitad='abajo', pintura='#34463a'):
    """Puerta de celda de acero: arriba una ventanita con barrotes (agujeros), abajo la ranura para la comida."""
    r = np.random.RandomState(seed + 6000)
    y, x = np.mgrid[0:N, 0:N]
    base = np.array(metal_placa(seed, pintura, pintado=0.8, remaches=False)).astype(np.float32)[..., :3]
    alpha = np.full((N, N), 255.0)
    # marco de la hoja (6 px a cada lado en la plantilla de puerta de 16 -> 3 px = 6 a 32)
    rgb = base
    rgb = np.where(((x == 0) | (x == N - 1))[..., None], rgb * 0.5, rgb)
    if mitad == 'arriba':
        win = (x >= 9) & (x <= 22) & (y >= 6) & (y <= 19)
        rim = (x >= 7) & (x <= 24) & (y >= 4) & (y <= 21) & ~win
        rgb = np.where(rim[..., None], rgb * 0.7, rgb)
        hole = win & ~((x - 9) % 5 < 2)
        alpha = np.where(hole, 0, alpha)
        rgb = np.where((win & ~hole)[..., None], hexc('#2d2b29') * (0.9 + 0.3 * white(N, N, seed)[..., None]), rgb)
        rgb = np.where((y == 0)[..., None], rgb * 0.5, rgb)
    else:
        slot = (x >= 8) & (x <= 23) & (y >= 4) & (y <= 8)
        rgb = np.where(slot[..., None], hexc('#151412'), rgb)
        rgb = np.where(((y == 9) & (x >= 8) & (x <= 23))[..., None], hexc('#6f6e68'), rgb)
        rgb = grime(rgb, seed + 3, amount=0.2, bottom=0.6)
        rgb = np.where((y == N - 1)[..., None], rgb * 0.5, rgb)
    # remaches en el marco
    for cy in (3, 15, 28):
        rgb[cy, 2] = hexc('#8a8a82')
        rgb[cy, N - 3] = hexc('#8a8a82')
    return img(rgb, alpha)


def terciopelo(seed=0, color='#5a0f14'):
    """Terciopelo rojo viejo (cortinas, alfombra): pliegues verticales, polvo y desgaste."""
    y, x = np.mgrid[0:N, 0:N]
    fold = 0.75 + 0.25 * np.sin(x / N * math.pi * 4 + seed)
    n = fbm(N, N, 5, seed + 80)
    rgb = hexc(color) * (fold * (0.85 + 0.3 * n))[..., None]
    rgb *= (0.9 + 0.2 * white(N, N, seed + 81))[..., None]
    dust = fbm(N, N, 9, seed + 82) > 0.68
    rgb = np.where(dust[..., None], mix(rgb, np.broadcast_to(hexc('#4a2a28'), rgb.shape), 0.3), rgb)
    return img(rgb)


def alfombra(seed=0):
    """Alfombra roja con orilla oscura (para pasillos): se repite a lo largo, gastada en medio."""
    y, x = np.mgrid[0:N, 0:N]
    base = np.array(terciopelo(seed, '#6a1218')).astype(np.float32)[..., :3]
    worn = np.exp(-((x - 16) / 9.0) ** 2) * fbm(N, N, 6, seed + 83)
    rgb = mix(base, np.broadcast_to(hexc('#4a2a24'), base.shape), np.clip(worn * 0.8, 0, 0.6))
    return img(grime(rgb, seed + 84, amount=0.3))


def alfombra_orilla(seed=0):
    """Orilla de la alfombra: banda dorada vieja y negro."""
    y, x = np.mgrid[0:N, 0:N]
    base = np.array(alfombra(seed)).astype(np.float32)[..., :3]
    gold = hexc('#8a6a2a') * (0.8 + 0.4 * white(N, N, seed))[..., None]
    rgb = np.where(((x >= 2) & (x <= 4))[..., None], gold, base)
    rgb = np.where((x <= 1)[..., None], hexc('#1a0c0c'), rgb)
    return img(rgb)


def madera_panel(seed=0, color='#3b2618'):
    """Panel de madera oscura con moldura (salas elegantes del Coleccionista): marco y tablero hundido."""
    y, x = np.mgrid[0:N, 0:N]
    grain = fbm(N, N, 3, seed + 90) * 0.6 + np.sin(y * 0.9 + fbm(N, N, 6, seed + 91) * 6) * 0.2
    rgb = hexc(color) * (0.8 + 0.4 * grain)[..., None]
    inner = (x >= 5) & (x <= 26) & (y >= 5) & (y <= 26)
    rgb = np.where(inner[..., None], rgb * 0.88, rgb)
    rgb = np.where(((x == 4) & (y >= 4) & (y <= 27) | (y == 4) & (x >= 4) & (x <= 27))[..., None], rgb * 0.55, rgb)
    rgb = np.where(((x == 27) & (y >= 4) & (y <= 27) | (y == 27) & (x >= 4) & (x <= 27))[..., None], rgb * 1.3 + 8, rgb)
    rgb = grime(rgb, seed + 92, amount=0.2)
    return img(rgb)


def ladrillo(seed=0, color='#5e3a2e'):
    """Ladrillo viejo con mortero gris (muros del exterior de los pabellones)."""
    r = np.random.RandomState(seed + 7000)
    y, x = np.mgrid[0:N, 0:N]
    row = y // 8
    off = (row % 2) * 8
    lx = (x + off) % 16
    ly = y % 8
    mort = (lx == 0) | (ly == 0)
    bid = ((x + off) // 16) + row * 7
    v = np.vectorize(lambda b: 0.85 + ((b * 2654435761 + seed) % 97) / 97 * 0.3)(bid).astype(np.float32)
    rgb = hexc(color) * v[..., None] * (0.85 + 0.25 * fbm(N, N, 4, seed + 100))[..., None]
    rgb = np.where(mort[..., None], hexc('#59554c') * (0.85 + 0.3 * white(N, N, seed)[..., None]), rgb)
    rgb = np.where((ly == 7)[..., None] & ~mort[..., None], rgb * 0.8, rgb)
    rgb = envejecer(rgb, seed + 103, 0.7)
    rgb = grime(rgb, seed + 101, amount=0.35)
    rgb = streaks(rgb, seed + 102, n=r.randint(0, 3), color='#2a2a22', strength=0.3)
    return img(rgb)


def metal_negro(seed=0):
    """Metal negro mate (marcos de las pantallas, vigas del Coleccionista)."""
    n = fbm(N, N, 6, seed + 110) * 0.6 + white(N, N, seed + 111) * 0.4
    rgb = hexc('#1d1c1c') * (0.8 + 0.5 * n)[..., None]
    y, x = np.mgrid[0:N, 0:N]
    rgb = np.where(((x == 0) | (y == 0))[..., None], rgb * 1.6 + 6, rgb)
    rgb = np.where(((x == N - 1) | (y == N - 1))[..., None], rgb * 0.6, rgb)
    return img(rgb)


def vidrio_sucio(seed=0):
    """Vidrio con polvo y huellas: casi transparente, marco fino."""
    y, x = np.mgrid[0:N, 0:N]
    n = fbm(N, N, 8, seed + 120)
    a = 40 + 90 * np.clip(n - 0.4, 0, 1) + 20 * white(N, N, seed + 121)
    rgb = np.broadcast_to(hexc('#aab3a8'), (N, N, 3)).copy()
    frame = (x == 0) | (y == 0) | (x == N - 1) | (y == N - 1)
    rgb = np.where(frame[..., None], hexc('#2c2b29'), rgb)
    a = np.where(frame, 255, a)
    # reflejo diagonal
    d = ((x + y) % 24 < 2) & ~frame
    a = np.where(d, a + 60, a)
    rgb = np.where(d[..., None], rgb + 30, rgb)
    return img(rgb, a)


def viga_lado(seed=0, pintura='#2f3a33'):
    """Viga de acero (columna): costado con las alas de la viga y la pintura saltada."""
    y, x = np.mgrid[0:N, 0:N]
    base = np.array(metal_placa(seed, pintura, pintado=0.75, remaches=False)).astype(np.float32)[..., :3]
    rgb = np.where(((x <= 3) | (x >= 28))[..., None], base * 1.1 + 6, base * 0.85)
    rgb = np.where(((x == 4) | (x == 27))[..., None], rgb * 0.5, rgb)
    for cy in (6, 22):
        for cx in (1, 30):
            rgb[cy, cx] = hexc('#9a9a92')
            rgb[cy + 1, cx] = hexc('#2a2824')
    return img(rgb)


def viga_tapa(seed=0, pintura='#2f3a33'):
    """La punta de la viga: perfil en I."""
    y, x = np.mgrid[0:N, 0:N]
    i = ((y <= 5) | (y >= 26)) | ((x >= 13) & (x <= 18))
    base = np.array(metal_placa(seed + 1, pintura, pintado=0.75, remaches=False)).astype(np.float32)[..., :3]
    rgb = np.where(i[..., None], base, hexc('#161513'))
    return img(rgb)


# ---------------------------------------------------------------- las Cloacas (prueba 1)
def _ladrillos(seed, ancho=16, alto=8, color='#4f5047', mortero='#2b2a25', jitter=0.25):
    """Muro de ladrillos/sillares: devuelve (rgb, mortero, fila, borde_abajo) para que cada material le ponga lo suyo."""
    y, x = np.mgrid[0:N, 0:N]
    row = y // alto
    off = (row % 2) * (ancho // 2)
    lx = (x + off) % ancho
    ly = y % alto
    mort = (lx == 0) | (ly == 0)
    bid = ((x + off) // ancho) + row * 7
    # el tono de cada ladrillo no depende de la variante: asi las variantes encajan entre si
    v = np.vectorize(lambda b: 1 - jitter / 2 + ((b * 2654435761 + 7919) % 101) / 101 * jitter)(bid).astype(np.float32)
    rgb = hexc(color) * v[..., None] * (0.82 + 0.3 * fbm(N, N, 4, seed + 100))[..., None]
    rgb = np.where(mort[..., None], hexc(mortero) * (0.8 + 0.35 * white(N, N, seed)[..., None]), rgb)
    # canto de arriba claro y de abajo oscuro (relieve)
    rgb = np.where(((ly == 1) & ~mort)[..., None], rgb * 1.12 + 4, rgb)
    rgb = np.where(((ly == alto - 1) & ~mort)[..., None], rgb * 0.78, rgb)
    return rgb, mort, ly


def ladrillo_cloaca(seed=0):
    """Ladrillo de alcantarilla: piedra gris verdosa mojada, junta negra, salitre blanco, musgo y lama abajo (variantes)."""
    r = np.random.RandomState(seed + 8100)
    rgb, mort, ly = _ladrillos(seed, color='#4a4c42', mortero='#1b1b17', jitter=0.42)
    y = np.mgrid[0:N, 0:N][0]
    # manchas de humedad grandes (oscurecen)
    hum = fbm(N, N, 14, seed + 8108)
    rgb = rgb * (1 - 0.38 * np.clip((hum - 0.5) * 2.5, 0, 1))[..., None]
    tipo = seed % 4
    if tipo == 1:   # musgo en las juntas y abajo
        m = fbm(N, N, 6, seed + 8101)
        k = np.clip((m - 0.45) * 2.2, 0, 1) * 0.75 + mort * 0.35
        rgb = mix(rgb, ramp(white(N, N, seed + 8102), '#2f3f22', '#3d4f29', '#4a5c30')[...], np.clip(k, 0, 0.85))
    elif tipo == 2:  # agrietado
        rgb = cracks(rgb, seed + 8103, n=2, dark=0.6)
    elif tipo == 3:  # salitre (manchas blancas que bajan)
        rgb = streaks(rgb, seed + 8104, n=4, color='#b8b5a3', strength=0.35)
    rgb = streaks(rgb, seed + 8105, n=r.randint(2, 5), color='#181a14', strength=0.5)
    rgb = envejecer(rgb, seed + 8106, 0.55)
    rgb = grime(rgb, seed + 8107, amount=0.4)
    return img(rgb)


def ladrillo_oscuro(seed=0):
    """Sillares negros (marcos de las compuertas, el Refugio)."""
    rgb, mort, ly = _ladrillos(seed, ancho=16, alto=16, color='#2c2b2a', mortero='#121211', jitter=0.18)
    rgb = cracks(rgb, seed + 8201, n=1, dark=0.55)
    rgb = grime(rgb, seed + 8202, amount=0.2)
    return img(rgb)


def piso_cloaca(seed=0):
    """Piso de laja de la alcantarilla: lajas irregulares, lodo en las juntas y charquitos brillosos."""
    y, x = np.mgrid[0:N, 0:N]
    # lajas: celdas de Voronoi que se repiten (el mismo acomodo en todas las variantes, para que encajen)
    pts = np.random.RandomState(8300).rand(7, 2) * N
    d1 = np.full((N, N), 1e9)
    d2 = np.full((N, N), 1e9)
    idx = np.zeros((N, N), int)
    for i, (px, py) in enumerate(pts):
        for ox in (-N, 0, N):
            for oy in (-N, 0, N):
                d = np.hypot(x - px - ox, y - py - oy)
                closer = d < d1
                d2 = np.where(closer, d1, np.minimum(d2, d))
                idx = np.where(closer, i, idx)
                d1 = np.where(closer, d, d1)
    borde = (d2 - d1) < 1.6
    tono = (0.86 + 0.22 * ((idx * 37 + seed) % 11) / 11).astype(np.float32)
    rgb = hexc('#3d3c35') * tono[..., None] * (0.85 + 0.25 * fbm(N, N, 4, seed + 8301))[..., None]
    rgb = np.where(borde[..., None], hexc('#1f1a12') * (0.8 + 0.3 * white(N, N, seed + 8302))[..., None], rgb)
    # charquitos: zonas oscuras con brillo
    c = fbm(N, N, 10, seed + 8303)
    agua = c > 0.6
    rgb = np.where(agua[..., None], rgb * 0.55 + hexc('#1d2420') * 0.3, rgb)
    brillo = agua & (white(N, N, seed + 8304) > 0.93)
    rgb = np.where(brillo[..., None], rgb + 45, rgb)
    rgb = envejecer(rgb, seed + 8305, 0.4)
    return img(rgb)


def lodo(seed=0):
    n = fbm(N, N, 5, seed + 8400) * 0.6 + white(N, N, seed + 8401) * 0.4
    rgb = ramp(n, '#241c14', '#2c2219', '#33281d', '#3b2e21')
    w = fbm(N, N, 9, seed + 8402) > 0.7
    rgb = np.where(w[..., None], rgb * 0.7 + hexc('#1f2a26') * 0.3, rgb)
    return img(rgb)


def concreto_humedo(seed=0):
    """Boveda de concreto mojado: mas oscura, con goteras de lama verde y sarro."""
    base = np.array(concreto(seed, color='#4d4e48', oxido=0)).astype(np.float32)[..., :3]
    r = np.random.RandomState(seed + 8500)
    base = streaks(base, seed + 8501, n=r.randint(2, 5), color='#26301f', strength=0.5)
    base = streaks(base, seed + 8502, n=r.randint(0, 2), color='#a8a490', strength=0.25)
    base = grime(base, seed + 8503, amount=0.35)
    return img(base)


def cobre_viejo(seed=0):
    """Lamina de cobre de las tuberias grandes: verdin (patina azul verde), cobre que asoma y remaches en hilera."""
    r = np.random.RandomState(seed + 8600)
    y, x = np.mgrid[0:N, 0:N]
    cobre = ramp(fbm(N, N, 4, seed + 8601) * 0.7 + white(N, N, seed + 8602) * 0.3, '#57321f', '#683c25', '#7a482c', '#8a5433')
    verdin = ramp(fbm(N, N, 5, seed + 8603) * 0.6 + white(N, N, seed + 8604) * 0.4, '#25423a', '#2e4f44', '#385c4f', '#44695a')
    k = fbm(N, N, 7, seed + 8605)
    rgb = np.where((k > 0.42 - 0.1 * (seed % 3))[..., None], verdin, cobre)
    # costuras de las laminas y remaches
    cost = (y % 16 == 0) | (x == 0)
    rgb = np.where(cost[..., None], rgb * 0.5, rgb)
    rem = ((y % 16 == 3) & (x % 6 == 3))
    rgb = np.where(rem[..., None], hexc('#b07a4c'), rgb)
    rgb = np.where(np.roll(rem, 1, 0)[..., None], rgb * 0.55, rgb)
    rgb = streaks(rgb, seed + 8606, n=r.randint(1, 3), color='#1e3a30', strength=0.4)
    rgb = grime(rgb, seed + 8607, amount=0.25)
    return img(rgb)


def tablon(seed=0, color='#3e2f22'):
    """Tablones viejos y mojados (el puente, el Refugio): vetas, clavos y orillas podridas."""
    r = np.random.RandomState(seed + 8700)
    y, x = np.mgrid[0:N, 0:N]
    tabla = x // 8
    t = (0.85 + 0.25 * ((tabla * 53 + seed * 7) % 9) / 9).astype(np.float32)
    veta = np.sin(y * 0.55 + fbm(N, N, 5, seed + 8701) * 7 + tabla * 2.0) * 0.5 + 0.5
    rgb = hexc(color) * t[..., None] * (0.78 + 0.32 * veta)[..., None]
    rgb = np.where((x % 8 == 0)[..., None], rgb * 0.45, rgb)
    rgb = np.where((x % 8 == 1)[..., None], rgb * 1.12, rgb)
    for tx in range(4):
        for cy in (3, 28):
            rgb[cy, tx * 8 + 4] = hexc('#5a5a55')
    pod = fbm(N, N, 6, seed + 8702) > 0.68
    rgb = np.where(pod[..., None], rgb * 0.6 + hexc('#1c2018') * 0.25, rgb)
    rgb = envejecer(rgb, seed + 8703, 0.5)
    return img(rgb)


def cadena(seed=0):
    """Cadena oxidada con el acomodo de la textura de la cadena de Minecraft (x 0..5 una cara, x 6..11 la otra): eslabones que se alternan
    de frente y de canto."""
    a = np.zeros((N, N), np.float32)
    rgb = np.zeros((N, N, 3), np.float32)
    y, x = np.mgrid[0:N, 0:N]
    hierro = ramp(white(N, N, seed + 8800), '#463c34', '#574b40', '#685a4c', '#7a6753')
    for cara, x0 in ((0, 0), (1, 6)):
        for k in range(4):
            cy = k * 8 + 4
            cx = x0 + 2.5
            if (k + cara) % 2 == 0:   # de frente: anillo
                ring = (np.abs(x - cx) <= 2.5) & (np.abs(y - cy) <= 4) & ~((np.abs(x - cx) <= 0.5) & (np.abs(y - cy) <= 2))
                ring &= ~((np.abs(x - cx) >= 2) & (np.abs(y - cy) >= 4))
            else:                     # de canto: barrita
                ring = (np.abs(x - cx) <= 0.6) & (np.abs(y - cy) <= 4.5)
            ring &= (x >= x0) & (x < x0 + 6)
            a = np.where(ring, 255, a)
            rgb = np.where(ring[..., None], hierro, rgb)
    rgb = np.where(((np.roll(a, 1, 1) == 0) & (a > 0))[..., None], rgb * 1.25 + 8, rgb)
    rust = fbm(N, N, 4, seed + 8801) > 0.58
    rgb = np.where(rust[..., None], rgb * 0.7 + hexc('#7a3f1c') * 0.3, rgb)
    return img(rgb, a)


def trampilla(seed=0):
    """Trampilla de reja de acero (piso que se abre): marco grueso y rejilla."""
    y, x = np.mgrid[0:N, 0:N]
    base = np.array(metal_placa(seed, '#3a3d39', pintado=0.45, remaches=False)).astype(np.float32)[..., :3]
    marco = (x < 3) | (y < 3) | (x > 28) | (y > 28)
    reja = ((x % 5 == 1) | (y % 5 == 1))
    a = np.where(marco | reja, 255, 0)
    rgb = np.where(marco[..., None], base * 0.9, base * 1.05)
    rgb = np.where(((x == 3) | (y == 3))[..., None] & ~marco[..., None], rgb * 0.6, rgb)
    for cx, cy in ((1, 1), (30, 1), (1, 30), (30, 30)):
        rgb[cy, cx] = hexc('#8c8c84')
    return img(rgb, a)


def alfombra_negra(seed=0):
    n = fbm(N, N, 3, seed + 8900) * 0.5 + white(N, N, seed + 8901) * 0.5
    rgb = hexc('#1b1919') * (0.75 + 0.5 * n)[..., None]
    y, x = np.mgrid[0:N, 0:N]
    rgb = np.where(((x % 16 == 0) | (y % 16 == 0))[..., None], rgb * 0.8, rgb)
    rgb = grime(rgb, seed + 8902, amount=0.3)
    return img(rgb)


# ---------------------------------------------------------------- el Archivo (prueba 2)
def parquet(seed=0):
    """Parquet de espiga oscuro y gastado (las salas del Archivo)."""
    y, x = np.mgrid[0:N, 0:N]
    # espiga: tablillas de 4x16 que alternan direccion
    blk = ((x // 4) + (y // 4)) % 2
    u = np.where(blk == 0, (x + y // 4 * 4) % 16, (y + x // 4 * 4) % 16)
    tid = np.where(blk == 0, (x // 4) * 5 + (y // 16) * 3, (y // 4) * 5 + (x // 16) * 3 + 2)
    t = (0.82 + 0.3 * ((tid * 2654435761 + seed) % 17) / 17).astype(np.float32)
    veta = 0.85 + 0.15 * np.sin(u * 1.3 + fbm(N, N, 4, seed + 9001) * 4)
    rgb = hexc('#3b2516') * (t * veta)[..., None]
    borde = np.where(blk == 0, (x % 4 == 0), (y % 4 == 0)) | (u == 0)
    rgb = np.where(borde[..., None], rgb * 0.55, rgb)
    desg = fbm(N, N, 10, seed + 9002)
    rgb = mix(rgb, rgb * 1.25 + 10, np.clip((desg - 0.6) * 2, 0, 0.5))
    rgb = grime(rgb, seed + 9003, amount=0.25)
    return img(rgb)


def estante_archivo(seed=0):
    """Estante de madera con cajas de archivo, carpetas y legajos amarrados (frente). Dos repisas por bloque."""
    r = np.random.RandomState(seed + 9100)
    y, x = np.mgrid[0:N, 0:N]
    madera = hexc('#2e1d12') * (0.8 + 0.3 * fbm(N, N, 4, seed + 9101))[..., None]
    rgb = np.broadcast_to(hexc('#0e0a07'), (N, N, 3)).copy()      # fondo oscuro
    for y0 in (1, 17):
        xx = 1
        while xx < 31:
            kind = r.randint(0, 4)
            w = {0: 6, 1: 2, 2: 3, 3: 5}[kind]
            w = min(w, 31 - xx)
            h = {0: 12, 1: 13, 2: 11, 3: 9}[kind]
            top = y0 + 14 - h
            if kind == 0:    # caja de carton con etiqueta
                col = hexc(['#6b5a3e', '#5d4e36', '#7a6747'][r.randint(0, 3)])
                rgb[top:y0 + 14, xx:xx + w] = col * (0.85 + 0.2 * r.rand())
                rgb[top + 3:top + 6, xx + 1:xx + w - 1] = hexc('#c9c0a6')
                rgb[top + 4, xx + 1:xx + w - 2] = hexc('#3a3226')
            elif kind == 1:  # carpeta (lomo de color)
                col = hexc(['#3d4a5c', '#5c2a24', '#33473a', '#4a3d2c', '#2a2a2e'][r.randint(0, 5)])
                rgb[top:y0 + 14, xx:xx + w] = col
                rgb[top + 2, xx:xx + w] = col * 1.5
            elif kind == 2:  # legajo de papeles amarrado
                rgb[top:y0 + 14, xx:xx + w] = hexc('#b3a98c') * (0.8 + 0.2 * white(1, 1, xx)[0, 0])
                rgb[top + h // 2, xx:xx + w] = hexc('#5a3020')
            else:            # hueco con polvo
                pass
            rgb[top:y0 + 14, xx + w - 1:xx + w] *= 0.6
            xx += w
        rgb[y0 + 14:y0 + 16, :] = madera[y0 + 14:y0 + 16, :]
    rgb[:, :1] = madera[:, :1]
    rgb[:, 31:] = madera[:, 31:] * 0.7
    rgb[0:1, :] = madera[0:1, :]
    rgb = grime(rgb, seed + 9102, amount=0.25)
    return img(rgb)


def madera_tapa(seed=0, color='#2e1d12'):
    """Tapa de mueble de madera oscura (arriba y abajo de los estantes)."""
    y, x = np.mgrid[0:N, 0:N]
    veta = np.sin(x * 0.7 + fbm(N, N, 5, seed + 9201) * 6) * 0.5 + 0.5
    rgb = hexc(color) * (0.8 + 0.3 * veta)[..., None]
    rgb = np.where(((x == 0) | (y == 0) | (x == N - 1) | (y == N - 1))[..., None], rgb * 0.6, rgb)
    return img(grime(rgb, seed + 9202, amount=0.2))


def archivero(seed=0):
    """Archivero de metal de cuatro cajones (verde oficina viejo) con jaladeras y tarjeteros."""
    y, x = np.mgrid[0:N, 0:N]
    base = np.array(metal_placa(seed, '#4b5a4c', pintado=0.92, remaches=False)).astype(np.float32)[..., :3]
    rgb = base * 1.05
    cajon = y // 8
    ly = y % 8
    rgb = np.where((ly == 0)[..., None], rgb * 0.45, rgb)
    rgb = np.where((ly == 1)[..., None], rgb * 1.15, rgb)
    jal = (ly >= 4) & (ly <= 5) & (x >= 12) & (x <= 19)
    rgb = np.where(jal[..., None], hexc('#a8a89e') * (0.9 + 0.2 * white(N, N, seed)[..., None]), rgb)
    rgb = np.where(((ly == 6) & (x >= 12) & (x <= 19))[..., None], rgb * 0.5, rgb)
    tarj = (ly >= 2) & (ly <= 3) & (x >= 14) & (x <= 17)
    rgb = np.where(tarj[..., None], hexc('#d6ceb5'), rgb)
    rgb = np.where(((x == 0) | (x == N - 1))[..., None], rgb * 0.6, rgb)
    rgb = grime(rgb, seed + 9301, amount=0.3)
    return img(rgb)


def lingotes(seed=0):
    """Lingotes de oro apilados (el tesoro de la boveda): oro opaco con polvo."""
    y, x = np.mgrid[0:N, 0:N]
    row = y // 8
    off = (row % 2) * 4
    lx = (x + off) % 8
    ly = y % 8
    oro = ramp(fbm(N, N, 3, seed + 9401) * 0.6 + white(N, N, seed + 9402) * 0.4, '#6a4f1d', '#84652a', '#9c7a33', '#b48f42')
    rgb = oro.copy()
    rgb = np.where(((lx == 0) | (ly == 0))[..., None], rgb * 0.45, rgb)
    rgb = np.where(((ly == 1) & (lx > 0))[..., None], rgb * 1.2 + 10, rgb)
    rgb = np.where(((lx >= 2) & (lx <= 5) & (ly >= 3) & (ly <= 4))[..., None], rgb * 0.85, rgb)
    rgb = grime(rgb, seed + 9403, amount=0.35)
    return img(rgb)


def viga_madera_lado(seed=0):
    y, x = np.mgrid[0:N, 0:N]
    veta = np.sin(x * 0.9 + fbm(N, N, 6, seed + 9501) * 8) * 0.5 + 0.5
    rgb = hexc('#33200f') * (0.75 + 0.35 * veta)[..., None]
    rgb = np.where(((x <= 1) | (x >= 30))[..., None], rgb * 0.6, rgb)
    for yy in (4, 27):
        rgb[yy:yy + 2, 2:30] = rgb[yy:yy + 2, 2:30] * 0.55     # molduras
    return img(grime(rgb, seed + 9502, amount=0.25))


def viga_madera_tapa(seed=0):
    y, x = np.mgrid[0:N, 0:N]
    d = np.hypot(x - 15.5, y - 15.5)
    rgb = hexc('#4a3019') * (0.8 + 0.25 * np.sin(d * 1.4))[..., None]
    rgb = np.where(((x <= 1) | (x >= 30) | (y <= 1) | (y >= 30))[..., None], hexc('#2a1a0d'), rgb)
    return img(rgb)


def alfombra_cafe(seed=0):
    n = fbm(N, N, 3, seed + 9601) * 0.5 + white(N, N, seed + 9602) * 0.5
    rgb = hexc('#4a3322') * (0.75 + 0.45 * n)[..., None]
    y, x = np.mgrid[0:N, 0:N]
    orla = ((x % 32 < 2) | (y % 32 < 2))
    rgb = np.where(orla[..., None], hexc('#7a5a2a'), rgb)
    rgb = grime(rgb, seed + 9603, amount=0.35)
    return img(rgb)


# ---------------------------------------------------------------- el Salon (prueba 3)
def papel_tapiz(seed=0, fondo='#3a0e12', dibujo='#5c1a1e'):
    """Papel tapiz de damasco rojo oscuro, manchado y despegado en partes (se ve el yeso)."""
    r = np.random.RandomState(seed + 9700)
    y, x = np.mgrid[0:N, 0:N]
    # damasco: rombo con flor en el centro, se repite cada 16
    u, v = (x % 16) - 7.5, (y % 16) - 7.5
    rombo = (np.abs(u) + np.abs(v)) < 7
    flor = (np.hypot(u, v * 0.7) < 3.2) | ((np.abs(u) < 0.8) & (np.abs(v) < 6)) | ((np.abs(v) < 0.8) & (np.abs(u) < 5))
    linea = np.abs(np.abs(u) + np.abs(v) - 7) < 0.6
    rgb = np.broadcast_to(hexc(fondo), (N, N, 3)).copy() * (0.85 + 0.25 * fbm(N, N, 6, seed + 9701))[..., None]
    rgb = np.where((flor | linea)[..., None], hexc(dibujo) * (0.9 + 0.2 * white(N, N, seed)[..., None]), rgb)
    rgb = np.where((rombo & ~flor & ~linea & ((x + y) % 4 == 0))[..., None], rgb * 1.08, rgb)
    # despegado: se ve el yeso
    if seed % 3 == 1:
        d = fbm(N, N, 10, seed + 9702) > 0.68
        yeso_ = hexc('#7d7563') * (0.8 + 0.3 * white(N, N, seed + 9703))[..., None]
        rgb = np.where(d[..., None], yeso_, rgb)
        orilla = d & ~np.roll(d, 1, 0)
        rgb = np.where(orilla[..., None], hexc('#1f0a0b'), rgb)
    rgb = streaks(rgb, seed + 9704, n=r.randint(0, 3), color='#1a1410', strength=0.4)
    rgb = grime(rgb, seed + 9705, amount=0.32)
    return img(rgb)


def marmol(seed=0, base='#1c1b1d', veta='#6d6a62', brillo=0.0):
    """Marmol pulido con vetas (negro u oscuro), algo rayado."""
    y, x = np.mgrid[0:N, 0:N]
    t = fbm(N, N, 8, seed + 9800)
    w = np.abs(np.sin((x * 0.25 + y * 0.18 + t * 9.0)))
    vena = w < 0.08
    vena2 = np.abs(np.sin(x * 0.11 - y * 0.3 + fbm(N, N, 5, seed + 9801) * 7)) < 0.05
    rgb = hexc(base) * (0.85 + 0.3 * fbm(N, N, 5, seed + 9802))[..., None]
    rgb = np.where(vena[..., None], mix(rgb, np.broadcast_to(hexc(veta), rgb.shape), 0.7), rgb)
    rgb = np.where(vena2[..., None], mix(rgb, np.broadcast_to(hexc(veta), rgb.shape), 0.35), rgb)
    if brillo:
        rgb = np.where((((x + y) % 23) < 1)[..., None], rgb + 18 * brillo, rgb)
    rgb = np.where(((x == 0) | (y == 0))[..., None], rgb * 0.7, rgb)
    rgb = envejecer(rgb, seed + 9803, 0.3)
    return img(rgb)


def tapete(seed=0, orilla=False):
    """Alfombra roja gruesa como piso (bajo las mesas); con orilla dorada gastada."""
    n = fbm(N, N, 3, seed + 9900) * 0.5 + white(N, N, seed + 9901) * 0.5
    rgb = hexc('#4e0d12') * (0.75 + 0.45 * n)[..., None]
    y, x = np.mgrid[0:N, 0:N]
    if orilla:
        rgb = hexc('#2a0709') * (0.8 + 0.45 * n)[..., None]
        rgb = np.where((((x // 8) + (y // 8)) % 2 == 0)[..., None] & ((x % 8 == 4) & (y % 8 == 4))[..., None], hexc('#7a5a26'), rgb)
    rgb = grime(rgb, seed + 9902, amount=0.35)
    return img(rgb)


def marco_dorado(seed=0):
    """Moldura dorada vieja (marcos de la pantalla y de los cuadros)."""
    y, x = np.mgrid[0:N, 0:N]
    oro = ramp(fbm(N, N, 3, seed + 9950) * 0.5 + white(N, N, seed + 9951) * 0.5, '#4a3614', '#6a4f1e', '#8a6a2a', '#a8843a')
    perfil = np.minimum(np.minimum(x, N - 1 - x), np.minimum(y, N - 1 - y))
    rgb = oro * (0.55 + 0.6 * np.clip(np.sin(perfil * 0.9) * 0.5 + 0.5, 0, 1))[..., None]
    adorno = ((perfil == 6) & ((x + y) % 4 < 2))
    rgb = np.where(adorno[..., None], rgb * 1.25 + 10, rgb)
    rgb = grime(rgb, seed + 9952, amount=0.45)
    return img(rgb)


# ---------------------------------------------------------------- los Laberintos (prueba 4): cuartos acolchados del manicomio
def acolchado(seed=0):
    """Pared acolchada de cuarto de manicomio: cojines de lona rombo con botones, sucios, con alguna rotura y manchas."""
    r = np.random.RandomState(seed + 10100)
    y, x = np.mgrid[0:N, 0:N]
    u, v = (x + y) % 16, (x - y) % 16
    du, dv = np.minimum(u, 16 - u), np.minimum(v, 16 - v)
    k = np.minimum(du, dv)                                    # distancia a la costura
    rgb = hexc('#b8b09a') * (0.62 + 0.45 * np.clip(k / 6.0, 0, 1) ** 0.6)[..., None]
    rgb = rgb * (0.92 + 0.12 * fbm(N, N, 4, seed + 10101))[..., None]
    boton = ((x % 16 == 8) & (y % 16 == 0)) | ((x % 16 == 0) & (y % 16 == 8)) | ((x % 16 == 0) & (y % 16 == 0)) | ((x % 16 == 8) & (y % 16 == 8))
    boton = boton & ((x + y) % 16 == 0)
    rgb = np.where(boton[..., None], hexc('#4a4436'), rgb)
    # manchas amarillentas y cafes, y aranazos
    m = fbm(N, N, 9, seed + 10102)
    rgb = mix(rgb, np.broadcast_to(hexc('#8a7a4a'), rgb.shape), np.clip((m - 0.55) * 1.8, 0, 0.5))
    if seed % 4 == 2:
        for _ in range(3):
            x0, y0 = r.randint(4, 28), r.randint(2, 20)
            for i in range(r.randint(6, 12)):
                if 0 <= y0 + i < N:
                    rgb[y0 + i, x0 % N] = hexc('#3a2a20')
                    rgb[y0 + i, (x0 + 2) % N] = hexc('#3a2a20')
    if seed % 4 == 3:  # rotura: se ve el relleno
        cx, cy = r.randint(8, 24), r.randint(8, 24)
        hole = np.hypot(x - cx, (y - cy) * 1.6) < 3.2
        rgb = np.where(hole[..., None], hexc('#d9d1bc') * (0.7 + 0.4 * white(N, N, seed)[..., None]), rgb)
        rgb = np.where((np.abs(np.hypot(x - cx, (y - cy) * 1.6) - 3.4) < 0.6)[..., None], hexc('#3b3427'), rgb)
    rgb = grime(rgb, seed + 10103, amount=0.35)
    return img(rgb)


def linoleo_verde(seed=0):
    """Linoleo de hospital verde grisaceo, en hojas, gastado por donde se camina."""
    y, x = np.mgrid[0:N, 0:N]
    n = fbm(N, N, 6, seed + 10200) * 0.6 + white(N, N, seed + 10201) * 0.4
    rgb = hexc('#56604f') * (0.85 + 0.25 * n)[..., None]
    rgb = np.where((((x % 16) == 0) | ((y % 16) == 0))[..., None], rgb * 0.7, rgb)
    rgb = np.where(((white(N, N, seed + 10202) > 0.97))[..., None], rgb * 1.3, rgb)
    gastado = fbm(N, N, 12, seed + 10203)
    rgb = mix(rgb, rgb * 0.75 + hexc('#3a3424') * 0.2, np.clip((gastado - 0.55) * 2, 0, 0.6))
    rgb = cracks(rgb, seed + 10204, n=seed % 2, dark=0.7)
    return img(rgb)


def vidrio_armado(seed=0):
    """Vidrio armado (con malla de alambre): casi transparente para que el guia vea el laberinto, con la malla y el marco."""
    y, x = np.mgrid[0:N, 0:N]
    a = np.full((N, N), 26.0) + 22 * np.clip(fbm(N, N, 10, seed + 10300) - 0.55, 0, 1) * 2
    rgb = np.broadcast_to(hexc('#b9c6c1'), (N, N, 3)).copy()
    malla = ((x % 16 == 8) | (y % 16 == 8))
    rgb = np.where(malla[..., None], hexc('#6a6b66'), rgb)
    a = np.where(malla, 110, a)
    marco = (x == 0) | (y == 0)
    rgb = np.where(marco[..., None], hexc('#3a3a36'), rgb)
    a = np.where(marco, 170, a)
    return img(rgb, a)


def panel_luz(seed=0):
    """Panel de luz del techo: rejilla metalica sobre un tubo que alumbra."""
    y, x = np.mgrid[0:N, 0:N]
    rgb = np.broadcast_to(hexc('#f0eedf'), (N, N, 3)).copy() * (0.92 + 0.08 * white(N, N, seed))[..., None]
    rej = ((x % 6 == 0) | (y % 6 == 0))
    rgb = np.where(rej[..., None], hexc('#8c8a80'), rgb)
    marco = (x <= 1) | (y <= 1) | (x >= N - 2) | (y >= N - 2)
    rgb = np.where(marco[..., None], hexc('#55544e'), rgb)
    return img(rgb)


def pintura_piso(seed=0, color='#8a1c16'):
    """Concreto con una marca pintada (circulo con X): la salida o el inicio."""
    base = np.array(concreto_piso(seed)).astype(np.float32)[..., :3]
    y, x = np.mgrid[0:N, 0:N]
    d = np.hypot(x - 15.5, y - 15.5)
    m = (np.abs(d - 11) < 1.5) | ((np.abs(np.abs(x - 15.5) - np.abs(y - 15.5)) < 1.3) & (d < 10))
    raspado = white(N, N, seed + 10400) > 0.25
    rgb = np.where((m & raspado)[..., None], hexc(color) * (0.8 + 0.3 * white(N, N, seed + 10401)[..., None]), base)
    return img(rgb)


# ---------------------------------------------------------------- la Capilla (prueba 5)
def piedra_capilla(seed=0):
    """Sillares de piedra caliza vieja: grandes, gastados, con hollin de las velas, grietas o humedad (variantes)."""
    r = np.random.RandomState(seed + 10500)
    rgb, mort, ly = _ladrillos(seed, ancho=16, alto=16, color='#5c574b', mortero='#2a2720', jitter=0.34)
    y = np.mgrid[0:N, 0:N][0]
    tipo = seed % 4
    if tipo == 1:
        rgb = cracks(rgb, seed + 10501, n=2, dark=0.6)
    elif tipo == 2:   # hollin que sube
        rgb = mix(rgb, np.broadcast_to(hexc('#1d1a16'), rgb.shape), np.clip((fbm(N, N, 8, seed + 10502) - 0.45) * 1.4, 0, 0.55))
    elif tipo == 3:   # humedad y musgo abajo
        m = fbm(N, N, 6, seed + 10503)
        rgb = mix(rgb, np.broadcast_to(hexc('#3c4a2c'), rgb.shape), np.clip((m - 0.55) * 1.6, 0, 0.5))
    rgb = envejecer(rgb, seed + 10504, 0.5)
    rgb = grime(rgb, seed + 10505, amount=0.3)
    rgb = streaks(rgb, seed + 10506, n=r.randint(0, 3), color='#2a2620', strength=0.35)
    return img(rgb)


def losa_piedra(seed=0):
    """Losas de piedra del piso, grandes y gastadas al centro."""
    y, x = np.mgrid[0:N, 0:N]
    rgb, mort, ly = _ladrillos(seed, ancho=32, alto=16, color='#4d4840', mortero='#221f1a', jitter=0.25)
    gast = np.clip(1 - np.hypot((x % 32) - 16, (y % 16) - 8) / 14, 0, 1)
    rgb = rgb * (1 + 0.12 * gast)[..., None]
    rgb = envejecer(rgb, seed + 10601, 0.4)
    rgb = grime(rgb, seed + 10602, amount=0.3)
    return img(rgb)


def vitral(seed=0):
    """Vitral de plomo: rombos de vidrio rojo oscuro y ambar, con plomo negro (translucido)."""
    y, x = np.mgrid[0:N, 0:N]
    u, v = (x + y) % 12, (x - y) % 12
    plomo = (u == 0) | (v == 0) | (x == 0) | (y == 0)
    cid = ((x + y) // 12 + (x - y) // 12) % 3
    cols = np.array([hexc('#7a1012'), hexc('#a0541a'), hexc('#4a0c18')])
    rgb = cols[cid] * (0.8 + 0.35 * fbm(N, N, 5, seed + 10701))[..., None]
    a = np.full((N, N), 200.0)
    rgb = np.where(plomo[..., None], hexc('#141210'), rgb)
    a = np.where(plomo, 255, a)
    roto = (fbm(N, N, 8, seed + 10702) > 0.78) & ~plomo
    a = np.where(roto, 0, a)
    return img(rgb, a)


# ---------------------------------------------------------------- la Sala de las puertas (prueba 6)
def marco_luz(seed=0, color=None):
    """Marco de acero negro con una tira de luz de color al centro (las puertas cambian de color en cada ronda). Sin color: apagado."""
    y, x = np.mgrid[0:N, 0:N]
    base = np.array(metal_negro(seed)).astype(np.float32)[..., :3]
    d = np.minimum(np.minimum(x, N - 1 - x), np.minimum(y, N - 1 - y))
    tira = (d >= 9) & (d <= 12)
    if color is None:
        c = hexc('#3a3936') * (0.85 + 0.2 * white(N, N, seed)[..., None])
    else:
        c = np.minimum(hexc(color) * (0.95 + 0.1 * white(N, N, seed)[..., None]), 255)
    rgb = np.where(tira[..., None], c, base)
    rgb = np.where(((d == 8) | (d == 13))[..., None], base * 0.5, rgb)
    return img(rgb)


# ---------------------------------------------------------------- la Camara final
def madera_pulida(seed=0, color='#2b180d'):
    """Madera oscura pulida, sin juntas (la mesa redonda)."""
    y, x = np.mgrid[0:N, 0:N]
    veta = np.sin(x * 0.45 + fbm(N, N, 7, seed + 10801) * 9) * 0.5 + 0.5
    rgb = hexc(color) * (0.78 + 0.35 * veta)[..., None] * (0.95 + 0.1 * white(N, N, seed + 10802))[..., None]
    rgb = np.where((((x + y) % 29) < 1)[..., None], rgb * 1.2 + 8, rgb)
    return img(rgb)


def continuo(base, otra, margen=2.0, ancho=6.0):
    """Mezcla una variante con la base solo en el centro del bloque: las orillas quedan iguales a la base, asi un muro hecho de
    variantes al azar se ve como una sola pared (no como bloques sueltos)."""
    a = np.array(base).astype(np.float32)
    b = np.array(otra).astype(np.float32)
    h, w = a.shape[:2]
    y, x = np.mgrid[0:h, 0:w]
    d = np.minimum(np.minimum(x, w - 1 - x), np.minimum(y, h - 1 - y)).astype(np.float32)
    m = np.clip((d - margen) / ancho, 0, 1)
    m = m * m * (3 - 2 * m)
    out = a * (1 - m[..., None]) + b * m[..., None]
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), 'RGBA')
