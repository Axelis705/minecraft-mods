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
    gx, gy = x / cell, y / cell
    ix, iy = np.floor(gx), np.floor(gy)
    fx, fy = gx - ix, gy - iy
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    px = period_x or max(1, int(round(w / cell)))
    py = period_y or max(1, int(round(h / cell)))

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
    out = grime(out, seed + 20, amount=0.3, top=0.15)
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


def concreto(seed=0, color='#66645c', poros=0.012, cimbra=True, oxido=1):
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
