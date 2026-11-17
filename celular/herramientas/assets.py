#!/usr/bin/env python3
"""Genera las texturas del celular: el objeto (8 colores), los iconos de las apps, los iconos pequeños de la interfaz y los fondos
de pantalla. Uso: python3 celular/herramientas/assets.py"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "celular")
TEX = os.path.join(ROOT, "textures")


def save(img, rel, blur=False):
    p = os.path.join(TEX, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p, optimize=True)
    if blur:
        with open(p + ".mcmeta", "w") as fh:
            fh.write('{\n  "texture": {\n    "blur": true\n  }\n}\n')


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))


def grad(w, h, stops, angle=90):
    """Degradado lineal: stops = [(t, color)], angle 90 = de arriba a abajo, 45 = diagonal."""
    img = Image.new("RGBA", (w, h))
    px = img.load()
    a = math.radians(angle)
    dx, dy = math.cos(a), math.sin(a)
    corners = [x * dx + y * dy for x in (0, w) for y in (0, h)]
    lo, hi = min(corners), max(corners)
    for y in range(h):
        for x in range(w):
            t = ((x * dx + y * dy) - lo) / (hi - lo)
            for i in range(len(stops) - 1):
                t0, c0 = stops[i]
                t1, c1 = stops[i + 1]
                if t <= t1 or i == len(stops) - 2:
                    k = 0 if t1 == t0 else max(0, min(1, (t - t0) / (t1 - t0)))
                    px[x, y] = lerp(c0, c1, k)
                    break
    return img


def rounded_mask(size, r, ss=4):
    m = Image.new("L", (size * ss, size * ss), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, size * ss - 1, size * ss - 1), r * ss, fill=255)
    return m.resize((size, size), Image.LANCZOS)


# =====================================================================================================================
#  Objeto: el celular en 16 × 16
# =====================================================================================================================
CASES = {
    "phone": ("#1c1d21", "#3a3c44", "#0e0f12"),
    "phone_white": ("#e4e4e8", "#ffffff", "#a8a8b0"),
    "phone_pink": ("#f2a7c3", "#ffd3e4", "#c06f8f"),
    "phone_blue": ("#2f6fd6", "#6fa2ff", "#1c4590"),
    "phone_green": ("#2e9e62", "#6bd99c", "#1b6a40"),
    "phone_purple": ("#7b4fd1", "#b08cff", "#4e2f8f"),
    "phone_gold": ("#d4af37", "#ffe08a", "#8f7320"),
    "phone_red": ("#d03a3a", "#ff7a7a", "#8a2020"),
}


def phone_item(name, base, light, dark):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    b, l, d = hexc(base), hexc(light), hexc(dark)
    x0, x1, y0, y1 = 4, 11, 1, 14
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            corner = (x in (x0, x1)) and (y in (y0, y1))
            if corner:
                continue
            c = b
            if x == x0 or y == y0:
                c = l
            if x == x1 or y == y1:
                c = d
            p[x, y] = c
    # pantalla con degradado y "apps"
    scr_top, scr_bot = hexc("#2b5876"), hexc("#4e4376")
    for y in range(3, 13):
        for x in range(5, 11):
            p[x, y] = lerp(scr_top, scr_bot, (y - 3) / 9)
    for (x, y, c) in [(6, 5, "#feda75"), (8, 5, "#d62976"), (6, 7, "#34c759"), (8, 7, "#0a84ff"), (6, 9, "#ff424d"), (8, 9, "#8e8e93")]:
        p[x, y] = hexc(c)
        p[x + 1, y] = lerp(hexc(c), (255, 255, 255, 255), 0.25)
    p[7, 2] = (10, 10, 10, 255)
    p[8, 2] = (10, 10, 10, 255)
    p[7, 12] = (230, 230, 230, 200)
    p[8, 12] = (230, 230, 230, 200)
    # brillo del vidrio
    p[5, 3] = lerp(p[5, 3], (255, 255, 255, 255), 0.45)
    p[5, 4] = lerp(p[5, 4], (255, 255, 255, 255), 0.25)
    save(img, f"item/{name}.png")


# =====================================================================================================================
#  Iconos de las apps (64 × 64, dibujados en grande y achicados para que se vean suaves)
# =====================================================================================================================
S = 64
SS = 8  # se dibuja a 8x


def app_base(stops, angle=90):
    big = grad(S * 2, S * 2, stops, angle).resize((S * SS, S * SS), Image.BILINEAR)
    return big


def finish(big):
    small = big.resize((S, S), Image.LANCZOS)
    mask = rounded_mask(S, 14)
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    out.paste(small, (0, 0), mask)
    return out


def U(v):
    return int(v * SS)


def icon_insta():
    big = app_base([(0, hexc("#feda75")), (0.3, hexc("#fa7e1e")), (0.6, hexc("#d62976")), (0.85, hexc("#962fbf")), (1, hexc("#4f5bd5"))], 135)
    d = ImageDraw.Draw(big)
    w = (255, 255, 255, 255)
    # cámara cuadrada "de bloques" (marco con esquinas escalonadas) y lente cuadrada
    s = U(4.5)
    d.rounded_rectangle((U(14), U(14), U(50), U(50)), U(9), outline=w, width=s)
    d.rectangle((U(25), U(25), U(39), U(39)), outline=w, width=s)
    d.rectangle((U(41.5), U(19.5), U(45.5), U(23.5)), fill=w)
    return finish(big)


def icon_camera():
    big = app_base([(0, hexc("#e9e9ee")), (1, hexc("#a9a9b2"))])
    d = ImageDraw.Draw(big)
    k = (28, 28, 30, 255)
    d.rounded_rectangle((U(10), U(20), U(54), U(48)), U(6), fill=k)
    d.rounded_rectangle((U(22), U(15), U(36), U(22)), U(2), fill=k)
    d.ellipse((U(22), U(23), U(42), U(43)), fill=(70, 70, 76, 255))
    d.ellipse((U(25), U(26), U(39), U(40)), fill=(20, 30, 60, 255))
    d.ellipse((U(28), U(28), U(33), U(33)), fill=(160, 190, 255, 255))
    d.ellipse((U(45), U(24), U(50), U(29)), fill=(255, 200, 60, 255))
    return finish(big)


def icon_gallery():
    big = app_base([(0, hexc("#5ac8fa")), (1, hexc("#007aff"))])
    d = ImageDraw.Draw(big)
    w = (255, 255, 255, 255)
    d.rounded_rectangle((U(12), U(16), U(52), U(48)), U(4), fill=w)
    d.rectangle((U(15), U(19), U(49), U(45)), fill=(135, 206, 250, 255))
    d.ellipse((U(37), U(22), U(45), U(30)), fill=(255, 214, 10, 255))
    d.polygon([(U(15), U(45)), (U(27), U(29)), (U(36), U(41)), (U(40), U(35)), (U(49), U(45))], fill=(52, 199, 89, 255))
    d.polygon([(U(15), U(45)), (U(27), U(29)), (U(31), U(35)), (U(25), U(45))], fill=(40, 160, 70, 255))
    return finish(big)


def handset(d, cx, cy, size, mid, fill):
    """Auricular de teléfono: un arco grueso (el mango) con dos almohadillas que apuntan hacia adentro. mid = hacia dónde queda el
    mango visto desde el centro del círculo (grados; 135 = abajo a la izquierda, 270 = arriba)."""
    r = size * 0.48
    w = size * 0.2
    m = math.radians(mid)
    ox, oy = cx - math.cos(m) * r * 0.45, cy - math.sin(m) * r * 0.45
    span = 52
    pts = []
    for i in range(-span, span + 1, 2):
        a = math.radians(mid + i)
        pts.append((ox + math.cos(a) * r, oy + math.sin(a) * r))
    for i in range(len(pts) - 1):
        d.line([pts[i], pts[i + 1]], fill=fill, width=int(w))
    for p in pts:
        d.ellipse((p[0] - w / 2, p[1] - w / 2, p[0] + w / 2, p[1] + w / 2), fill=fill)
    pw = w * 1.55
    for end in (pts[0], pts[-1]):
        ex, ey = end
        vx, vy = ox - ex, oy - ey
        n = math.hypot(vx, vy)
        vx, vy = vx / n, vy / n
        L = r * 0.42
        tx, ty = ex + vx * L, ey + vy * L
        steps = 12
        for k in range(steps + 1):
            px, py = ex + (tx - ex) * k / steps, ey + (ty - ey) * k / steps
            d.ellipse((px - pw / 2, py - pw / 2, px + pw / 2, py + pw / 2), fill=fill)


def icon_phone():
    big = app_base([(0, hexc("#5ef08a")), (1, hexc("#1fae4b"))])
    d = ImageDraw.Draw(big)
    handset(d, U(32), U(32), U(40), 135, (255, 255, 255, 255))
    return finish(big)


def heart(d, cx, cy, s, fill):
    pts = []
    for i in range(0, 361, 3):
        t = math.radians(i)
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        pts.append((cx + x * s / 34, cy + y * s / 34))
    d.polygon(pts, fill=fill)


def star(d, cx, cy, r, fill, inner=0.45):
    pts = []
    for i in range(10):
        a = math.radians(-90 + i * 36)
        rr = r if i % 2 == 0 else r * inner
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(pts, fill=fill)


def icon_patreon():
    big = app_base([(0, hexc("#ff6a70")), (1, hexc("#e0283a"))])
    d = ImageDraw.Draw(big)
    heart(d, U(30), U(35), U(38), (255, 255, 255, 255))
    star(d, U(47), U(17), U(7), (255, 214, 10, 255))
    return finish(big)


def gear(d, cx, cy, r, fill, hole, teeth=8):
    pts = []
    for i in range(teeth * 4):
        a = math.radians(i * 360 / (teeth * 4))
        rr = r if (i % 4) in (0, 1) else r * 0.78
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(pts, fill=fill)
    d.ellipse((cx - r * 0.35, cy - r * 0.35, cx + r * 0.35, cy + r * 0.35), fill=hole)


def icon_settings():
    big = app_base([(0, hexc("#9a9aa2")), (1, hexc("#5c5c64"))])
    d = ImageDraw.Draw(big)
    gear(d, U(32), U(32), U(21), (240, 240, 244, 255), (110, 110, 118, 255), 10)
    d.ellipse((U(24), U(24), U(40), U(40)), outline=(150, 150, 158, 255), width=U(2))
    return finish(big)


def note(d, x, y, s, fill):
    """Nota musical doble (corchea unida) en (x, y) de tamaño s."""
    w = s * 0.11
    d.ellipse((x, y + s * 0.70, x + s * 0.34, y + s * 0.96), fill=fill)
    d.ellipse((x + s * 0.56, y + s * 0.58, x + s * 0.90, y + s * 0.84), fill=fill)
    d.rectangle((x + s * 0.34 - w, y + s * 0.18, x + s * 0.34, y + s * 0.84), fill=fill)
    d.rectangle((x + s * 0.90 - w, y + s * 0.06, x + s * 0.90, y + s * 0.72), fill=fill)
    d.polygon([(x + s * 0.34 - w, y + s * 0.18), (x + s * 0.90, y + s * 0.04), (x + s * 0.90, y + s * 0.20), (x + s * 0.34 - w, y + s * 0.34)],
              fill=fill)


def icon_music():
    big = app_base([(0, hexc("#ff5e7e")), (0.55, hexc("#fa233b")), (1, hexc("#b5179e"))], 135)
    d = ImageDraw.Draw(big)
    note(d, U(15), U(13), U(34), (255, 255, 255, 255))
    return finish(big)


def cloud(d, x, y, s, fill):
    d.ellipse((x, y + s * 0.30, x + s * 0.50, y + s * 0.80), fill=fill)
    d.ellipse((x + s * 0.22, y, x + s * 0.80, y + s * 0.62), fill=fill)
    d.ellipse((x + s * 0.55, y + s * 0.22, x + s, y + s * 0.80), fill=fill)
    d.rounded_rectangle((x + s * 0.20, y + s * 0.45, x + s * 0.85, y + s * 0.80), s * 0.15, fill=fill)


def icon_weather():
    big = app_base([(0, hexc("#56ccf2")), (1, hexc("#1d6fd6"))])
    d = ImageDraw.Draw(big)
    d.ellipse((U(26), U(10), U(52), U(36)), fill=hexc("#ffd23f"))
    for i in range(8):
        a = math.radians(i * 45)
        d.line([(U(39 + math.cos(a) * 16), U(23 + math.sin(a) * 16)), (U(39 + math.cos(a) * 20), U(23 + math.sin(a) * 20))],
               fill=hexc("#ffd23f"), width=U(2.4))
    cloud(d, U(9), U(26), U(40), (255, 255, 255, 255))
    return finish(big)


def icon_compass():
    big = app_base([(0, hexc("#3a3d46")), (1, hexc("#14161b"))])
    d = ImageDraw.Draw(big)
    d.ellipse((U(10), U(10), U(54), U(54)), outline=(235, 235, 240, 255), width=U(2.5))
    for i in range(12):
        a = math.radians(i * 30)
        r0 = 18 if i % 3 else 15
        d.line([(U(32 + math.cos(a) * r0), U(32 + math.sin(a) * r0)), (U(32 + math.cos(a) * 20), U(32 + math.sin(a) * 20))],
               fill=(200, 200, 208, 255), width=U(1.4))
    d.polygon([(U(32), U(13)), (U(37), U(32)), (U(27), U(32))], fill=hexc("#ff3b30"))
    d.polygon([(U(32), U(51)), (U(37), U(32)), (U(27), U(32))], fill=(240, 240, 244, 255))
    d.ellipse((U(29.5), U(29.5), U(34.5), U(34.5)), fill=hexc("#14161b"))
    return finish(big)


def icon_calc():
    big = app_base([(0, hexc("#4a4a50")), (1, hexc("#1e1e22"))])
    d = ImageDraw.Draw(big)
    for r in range(3):
        for c in range(3):
            col = hexc("#ff9f0a") if c == 2 else hexc("#a5a5ab")
            x, y = U(13 + c * 14), U(13 + r * 14)
            d.ellipse((x, y, x + U(11), y + U(11)), fill=col)
    W2 = (255, 255, 255, 255)
    d.line([(U(44.5), U(16)), (U(44.5), U(21))], fill=W2, width=U(1.6))
    d.line([(U(42), U(18.5)), (U(47), U(18.5))], fill=W2, width=U(1.6))
    d.line([(U(42), U(46)), (U(47), U(46))], fill=W2, width=U(1.6))
    d.line([(U(42), U(48.5)), (U(47), U(48.5))], fill=W2, width=U(1.6))
    return finish(big)


# =====================================================================================================================
#  Iconos pequeños de la interfaz (24 × 24, blancos; el juego los pinta del color que toque)
# =====================================================================================================================
G = 24
GS = 8


def glyph(fn):
    big = Image.new("RGBA", (G * GS, G * GS), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    fn(d, lambda v: v * GS)
    return big.resize((G, G), Image.LANCZOS)


W = (255, 255, 255, 255)


def g_heart(d, u):
    pts = []
    for i in range(0, 361, 3):
        t = math.radians(i)
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        pts.append((u(12) + x * u(20) / 34, u(12.5) + y * u(20) / 34))
    d.line(pts + [pts[0]], fill=W, width=int(u(2.2)), joint="curve")


def g_heart_full(d, u):
    heart(d, u(12), u(12.5), u(21), W)


def g_home(d, u):
    d.line([(u(3), u(11)), (u(12), u(3.5)), (u(21), u(11))], fill=W, width=int(u(2.2)), joint="curve")
    d.line([(u(6), u(10)), (u(6), u(20.5)), (u(18), u(20.5)), (u(18), u(10))], fill=W, width=int(u(2.2)), joint="curve")
    d.rectangle((u(10), u(14), u(14), u(20.5)), fill=W)


def g_plus(d, u):
    d.rounded_rectangle((u(3), u(3), u(21), u(21)), u(5), outline=W, width=int(u(2.2)))
    d.line([(u(12), u(7.5)), (u(12), u(16.5))], fill=W, width=int(u(2.2)))
    d.line([(u(7.5), u(12)), (u(16.5), u(12))], fill=W, width=int(u(2.2)))


def g_user(d, u):
    d.ellipse((u(7.5), u(3), u(16.5), u(12)), fill=W)
    d.pieslice((u(3.5), u(13), u(20.5), u(30)), 180, 360, fill=W)


def g_call(d, u):
    handset(d, u(12), u(12), u(20), 135, W)


def g_hangup(d, u):
    handset(d, u(12), u(13), u(22), 270, W)


def g_camera(d, u):
    d.rounded_rectangle((u(2.5), u(7), u(21.5), u(20)), u(3), fill=W)
    d.rounded_rectangle((u(8), u(4), u(16), u(8)), u(1.5), fill=W)
    d.ellipse((u(7.5), u(9), u(16.5), u(18)), fill=(0, 0, 0, 0))
    d.ellipse((u(9.5), u(11), u(14.5), u(16)), fill=W)


def g_switch(d, u):
    d.arc((u(4), u(4), u(20), u(20)), 200, 340, fill=W, width=int(u(2.2)))
    d.arc((u(4), u(4), u(20), u(20)), 20, 160, fill=W, width=int(u(2.2)))
    d.polygon([(u(19.5), u(5)), (u(21.5), u(11)), (u(15.5), u(10))], fill=W)
    d.polygon([(u(4.5), u(19)), (u(2.5), u(13)), (u(8.5), u(14))], fill=W)


def g_trash(d, u):
    d.line([(u(4), u(6)), (u(20), u(6))], fill=W, width=int(u(2.2)))
    d.rectangle((u(9), u(3), u(15), u(6)), outline=W, width=int(u(1.8)))
    d.polygon([(u(6), u(7)), (u(18), u(7)), (u(17), u(21)), (u(7), u(21))], outline=W, fill=None)
    d.line([(u(6), u(7)), (u(7), u(21)), (u(17), u(21)), (u(18), u(7))], fill=W, width=int(u(2.2)), joint="curve")
    d.line([(u(10), u(10)), (u(10), u(18))], fill=W, width=int(u(1.6)))
    d.line([(u(14), u(10)), (u(14), u(18))], fill=W, width=int(u(1.6)))


def g_back(d, u):
    d.line([(u(15), u(4)), (u(7), u(12)), (u(15), u(20))], fill=W, width=int(u(2.8)), joint="curve")


def g_next(d, u):
    d.line([(u(9), u(4)), (u(17), u(12)), (u(9), u(20))], fill=W, width=int(u(2.8)), joint="curve")


def g_send(d, u):
    d.polygon([(u(2.5), u(11)), (u(21.5), u(3)), (u(14), u(21.5)), (u(11), u(13))], outline=W, fill=None)
    d.line([(u(2.5), u(11)), (u(21.5), u(3)), (u(14), u(21.5)), (u(11), u(13)), (u(2.5), u(11))], fill=W, width=int(u(2)), joint="curve")


def g_star(d, u):
    star(d, u(12), u(12.5), u(10.5), W, 0.45)


def g_wifi(d, u):
    for r in (4, 8, 12):
        d.arc((u(12 - r), u(19 - r), u(12 + r), u(19 + r)), 225, 315, fill=W, width=int(u(2.4)))
    d.ellipse((u(10.5), u(17.5), u(13.5), u(20.5)), fill=W)


def g_battery(d, u):
    d.rounded_rectangle((u(1.5), u(7), u(19.5), u(17)), u(2.5), outline=W, width=int(u(1.6)))
    d.rounded_rectangle((u(3.5), u(9), u(15), u(15)), u(1), fill=W)
    d.rectangle((u(20.5), u(10), u(22), u(14)), fill=W)


def g_signal(d, u):
    for i, h in enumerate((5, 8, 12, 16)):
        x = u(3 + i * 5)
        d.rounded_rectangle((x, u(20 - h), x + u(3.2), u(20)), u(0.8), fill=W)


def g_gallery(d, u):
    d.rounded_rectangle((u(2.5), u(4), u(21.5), u(20)), u(3), outline=W, width=int(u(2)))
    d.ellipse((u(14), u(7), u(18), u(11)), fill=W)
    d.polygon([(u(4), u(18.5)), (u(10), u(11)), (u(14.5), u(16)), (u(16.5), u(14)), (u(20), u(18.5))], fill=W)


def g_gear(d, u):
    gear(d, u(12), u(12), u(10.5), W, (0, 0, 0, 0), 8)


def g_bell(d, u):
    d.pieslice((u(5), u(3), u(19), u(23)), 180, 360, fill=W)
    d.rectangle((u(5), u(12.9), u(19), u(17)), fill=W)
    d.polygon([(u(3), u(18.5)), (u(5), u(16)), (u(19), u(16)), (u(21), u(18.5))], fill=W)
    d.ellipse((u(10), u(18.5), u(14), u(22)), fill=W)
    d.ellipse((u(11), u(1.5), u(13), u(3.5)), fill=W)


def g_mic(d, u):
    d.rounded_rectangle((u(8.5), u(2.5), u(15.5), u(15)), u(3.5), fill=W)
    d.arc((u(5), u(6), u(19), u(18.5)), 0, 180, fill=W, width=int(u(2)))
    d.line([(u(12), u(18.5)), (u(12), u(21.5))], fill=W, width=int(u(2)))
    d.line([(u(8), u(21.5)), (u(16), u(21.5))], fill=W, width=int(u(2)))


def g_clock(d, u):
    d.ellipse((u(2.5), u(2.5), u(21.5), u(21.5)), outline=W, width=int(u(2.2)))
    d.line([(u(12), u(6.5)), (u(12), u(12)), (u(16), u(14.5))], fill=W, width=int(u(2.2)), joint="curve")


def g_people(d, u):
    d.ellipse((u(5), u(4), u(12), u(11)), fill=W)
    d.pieslice((u(1.5), u(12.5), u(15.5), u(26.5)), 180, 360, fill=W)
    d.ellipse((u(13.5), u(5.5), u(19), u(11)), fill=W)
    d.pieslice((u(12.5), u(13), u(23), u(25)), 180, 360, fill=W)


def g_close(d, u):
    d.line([(u(5), u(5)), (u(19), u(19))], fill=W, width=int(u(2.8)))
    d.line([(u(19), u(5)), (u(5), u(19))], fill=W, width=int(u(2.8)))


def g_wallpaper(d, u):
    d.rounded_rectangle((u(6), u(1.5), u(18), u(22.5)), u(2.5), outline=W, width=int(u(1.8)))
    d.polygon([(u(7.5), u(19)), (u(11), u(12)), (u(13.5), u(16)), (u(15), u(14)), (u(16.5), u(19))], fill=W)
    d.ellipse((u(12.5), u(5), u(15.5), u(8)), fill=W)


def g_play(d, u):
    d.polygon([(u(6), u(3.5)), (u(20.5), u(12)), (u(6), u(20.5))], fill=W)


def g_pause(d, u):
    d.rounded_rectangle((u(5), u(3.5), u(10), u(20.5)), u(1.2), fill=W)
    d.rounded_rectangle((u(14), u(3.5), u(19), u(20.5)), u(1.2), fill=W)


def g_prev(d, u):
    d.rectangle((u(3.5), u(5), u(6.5), u(19)), fill=W)
    d.polygon([(u(13), u(5)), (u(6.5), u(12)), (u(13), u(19))], fill=W)
    d.polygon([(u(20.5), u(5)), (u(14), u(12)), (u(20.5), u(19))], fill=W)


def g_skip(d, u):
    d.rectangle((u(17.5), u(5), u(20.5), u(19)), fill=W)
    d.polygon([(u(11), u(5)), (u(17.5), u(12)), (u(11), u(19))], fill=W)
    d.polygon([(u(3.5), u(5)), (u(10), u(12)), (u(3.5), u(19))], fill=W)


def g_shuffle(d, u):
    w = int(u(2.2))
    d.line([(u(2.5), u(7)), (u(8), u(7)), (u(15), u(17)), (u(19), u(17))], fill=W, width=w, joint="curve")
    d.line([(u(2.5), u(17)), (u(8), u(17)), (u(15), u(7)), (u(19), u(7))], fill=W, width=w, joint="curve")
    d.polygon([(u(18), u(3.5)), (u(22.5), u(7)), (u(18), u(10.5))], fill=W)
    d.polygon([(u(18), u(13.5)), (u(22.5), u(17)), (u(18), u(20.5))], fill=W)


def g_repeat(d, u):
    w = int(u(2.2))
    d.line([(u(4), u(13)), (u(4), u(8)), (u(18), u(8))], fill=W, width=w, joint="curve")
    d.line([(u(20), u(11)), (u(20), u(16)), (u(6), u(16))], fill=W, width=w, joint="curve")
    d.polygon([(u(17), u(4.5)), (u(21.5), u(8)), (u(17), u(11.5))], fill=W)
    d.polygon([(u(7), u(12.5)), (u(2.5), u(16)), (u(7), u(19.5))], fill=W)


def g_repeat1(d, u):
    g_repeat(d, u)
    d.rectangle((u(11), u(9.5), u(13.2), u(14.5)), fill=W)
    d.polygon([(u(9.5), u(11)), (u(11), u(9.5)), (u(11), u(11.5))], fill=W)


def g_folder(d, u):
    d.polygon([(u(2.5), u(5)), (u(9.5), u(5)), (u(11.5), u(7.5)), (u(21.5), u(7.5)), (u(21.5), u(19.5)), (u(2.5), u(19.5))], fill=W)


def g_note(d, u):
    note(d, u(3), u(3), u(18), W)


def speaker(d, u):
    d.polygon([(u(2.5), u(9)), (u(7), u(9)), (u(12), u(4.5)), (u(12), u(19.5)), (u(7), u(15)), (u(2.5), u(15))], fill=W)


def g_vol_low(d, u):
    speaker(d, u)
    d.arc((u(9), u(8), u(17), u(16)), -55, 55, fill=W, width=int(u(2)))


def g_vol_high(d, u):
    speaker(d, u)
    d.arc((u(9), u(8), u(17), u(16)), -55, 55, fill=W, width=int(u(2)))
    d.arc((u(9), u(4.5), u(21.5), u(19.5)), -55, 55, fill=W, width=int(u(2)))


def g_refresh(d, u):
    d.arc((u(4), u(4), u(20), u(20)), 40, 330, fill=W, width=int(u(2.4)))
    d.polygon([(u(14.5), u(2.5)), (u(21.5), u(5)), (u(16), u(10))], fill=W)


def g_down(d, u):
    d.line([(u(4), u(9)), (u(12), u(16)), (u(20), u(9))], fill=W, width=int(u(2.8)), joint="curve")


def g_sun(d, u):
    d.ellipse((u(7), u(7), u(17), u(17)), fill=W)
    for i in range(8):
        a = math.radians(i * 45)
        d.line([(u(12 + math.cos(a) * 7.5), u(12 + math.sin(a) * 7.5)), (u(12 + math.cos(a) * 10.5), u(12 + math.sin(a) * 10.5))],
               fill=W, width=int(u(2)))


def g_moon(d, u):
    big = Image.new("L", (int(u(24)), int(u(24))), 0)
    dd = ImageDraw.Draw(big)
    dd.ellipse((u(3), u(3), u(21), u(21)), fill=255)
    dd.ellipse((u(9), u(0), u(25), u(16)), fill=0)
    d.bitmap((0, 0), big, fill=W)


def g_cloud(d, u):
    cloud(d, u(2), u(5), u(20), W)


def g_rain(d, u):
    cloud(d, u(2), u(2), u(20), W)
    for x in (7, 12, 17):
        d.line([(u(x), u(17)), (u(x - 1.5), u(21.5))], fill=W, width=int(u(1.8)))


def g_storm(d, u):
    cloud(d, u(2), u(1.5), u(20), W)
    d.polygon([(u(12.5), u(14)), (u(8.5), u(19)), (u(11.5), u(19)), (u(10), u(23)), (u(15.5), u(17)), (u(12.5), u(17))], fill=W)


def g_pin(d, u):
    d.pieslice((u(5), u(2.5), u(19), u(16.5)), 150, 390, fill=W)
    d.polygon([(u(6.2), u(13)), (u(12), u(22)), (u(17.8), u(13))], fill=W)
    d.ellipse((u(9.5), u(7), u(14.5), u(12)), fill=(0, 0, 0, 0))


def g_nav(d, u):
    d.polygon([(u(12), u(2.5)), (u(20), u(21)), (u(12), u(16.5)), (u(4), u(21))], fill=W)


def g_skull(d, u):
    d.ellipse((u(4), u(2.5), u(20), u(17)), fill=W)
    d.rectangle((u(7.5), u(14), u(16.5), u(20.5)), fill=W)
    d.ellipse((u(7), u(8), u(11), u(12)), fill=(0, 0, 0, 0))
    d.ellipse((u(13), u(8), u(17), u(12)), fill=(0, 0, 0, 0))
    d.rectangle((u(10.5), u(17.5), u(11.3), u(20.5)), fill=(0, 0, 0, 0))
    d.rectangle((u(12.7), u(17.5), u(13.5), u(20.5)), fill=(0, 0, 0, 0))


def g_lock(d, u):
    d.rounded_rectangle((u(4.5), u(10.5), u(19.5), u(21.5)), u(2), fill=W)
    d.arc((u(7), u(3), u(17), u(15)), 180, 360, fill=W, width=int(u(2.4)))
    d.line([(u(7.6), u(8.5)), (u(7.6), u(11))], fill=W, width=int(u(2.4)))
    d.line([(u(16.4), u(8.5)), (u(16.4), u(11))], fill=W, width=int(u(2.4)))


def g_sliders(d, u):
    for y, k in ((6, 15), (12, 8), (18, 13)):
        d.line([(u(3), u(y)), (u(21), u(y))], fill=W, width=int(u(2)))
        d.ellipse((u(k - 2.8), u(y - 2.8), u(k + 2.8), u(y + 2.8)), fill=W)


def g_upload(d, u):
    cloud(d, u(1.5), u(3), u(21), W)
    d.polygon([(u(12), u(8)), (u(7), u(13.5)), (u(10), u(13.5)), (u(10), u(19)), (u(14), u(19)), (u(14), u(13.5)), (u(17), u(13.5))],
              fill=(0, 0, 0, 0))


def g_speaker(d, u):
    d.polygon([(u(3), u(9)), (u(7.5), u(9)), (u(13), u(4)), (u(13), u(20)), (u(7.5), u(15)), (u(3), u(15))], fill=W)
    d.arc((u(10), u(7.5), u(18), u(16.5)), 300, 60, fill=W, width=int(u(2)))
    d.arc((u(10), u(4), u(22), u(20)), 300, 60, fill=W, width=int(u(2)))


def g_headphones(d, u):
    d.arc((u(3.5), u(3.5), u(20.5), u(21)), 180, 360, fill=W, width=int(u(2.4)))
    d.rounded_rectangle((u(2.5), u(12), u(8), u(21)), u(2), fill=W)
    d.rounded_rectangle((u(16), u(12), u(21.5), u(21)), u(2), fill=W)


def g_globe(d, u):
    d.ellipse((u(3), u(3), u(21), u(21)), outline=W, width=int(u(2)))
    d.ellipse((u(8), u(3), u(16), u(21)), outline=W, width=int(u(1.6)))
    d.line([(u(3.5), u(12)), (u(20.5), u(12))], fill=W, width=int(u(1.6)))
    d.line([(u(5.5), u(7.5)), (u(18.5), u(7.5))], fill=W, width=int(u(1.3)))
    d.line([(u(5.5), u(16.5)), (u(18.5), u(16.5))], fill=W, width=int(u(1.3)))


def g_crown(d, u):
    d.polygon([(u(3), u(8)), (u(7.5), u(12.5)), (u(12), u(5)), (u(16.5), u(12.5)), (u(21), u(8)), (u(19), u(18)), (u(5), u(18))], fill=W)
    d.rectangle((u(5), u(19.2), u(19), u(21)), fill=W)


def g_thumb(d, u):
    d.rounded_rectangle((u(3), u(11), u(7), u(21)), u(1), fill=W)
    d.polygon([(u(8), u(11)), (u(12), u(3.5)), (u(14.5), u(4.5)), (u(13.5), u(10)), (u(20), u(10)), (u(21), u(12.5)),
               (u(18.5), u(21)), (u(8), u(21))], fill=W)


def g_download(d, u):
    cloud(d, u(1.5), u(3), u(21), W)
    d.polygon([(u(12), u(19.5)), (u(7), u(14)), (u(10), u(14)), (u(10), u(8.5)), (u(14), u(8.5)), (u(14), u(14)), (u(17), u(14))],
              fill=(0, 0, 0, 0))


def g_queue(d, u):
    for y in (5, 10.5):
        d.rounded_rectangle((u(3), u(y), u(16), u(y + 2.4)), u(1), fill=W)
    d.rounded_rectangle((u(3), u(16), u(11), u(18.4)), u(1), fill=W)
    d.polygon([(u(14), u(14)), (u(21), u(17.5)), (u(14), u(21))], fill=W)


def g_chat(d, u):
    d.rounded_rectangle((u(2.5), u(3.5), u(21.5), u(16.5)), u(4), fill=W)
    d.polygon([(u(6), u(15)), (u(5), u(21.5)), (u(12), u(15.5))], fill=W)
    for x in (7.5, 12, 16.5):
        d.ellipse((u(x - 1.3), u(8.7), u(x + 1.3), u(11.3)), fill=(0, 0, 0, 0))


def g_map(d, u):
    d.polygon([(u(2.5), u(5)), (u(8.5), u(3)), (u(15.5), u(5)), (u(21.5), u(3)), (u(21.5), u(19)), (u(15.5), u(21)), (u(8.5), u(19)),
               (u(2.5), u(21))], fill=W)
    d.line([(u(8.5), u(3.5)), (u(8.5), u(19))], fill=(0, 0, 0, 0), width=int(u(1.3)))
    d.line([(u(15.5), u(5)), (u(15.5), u(20.5))], fill=(0, 0, 0, 0), width=int(u(1.3)))


def g_notes(d, u):
    d.rounded_rectangle((u(4), u(2.5), u(20), u(21.5)), u(2.5), fill=W)
    for y in (8, 12, 16):
        d.line([(u(7.5), u(y)), (u(16.5), u(y))], fill=(0, 0, 0, 0), width=int(u(1.5)))


def g_flash(d, u):
    d.polygon([(u(5), u(3)), (u(19), u(3)), (u(16), u(10)), (u(8), u(10))], fill=W)
    d.rounded_rectangle((u(8.5), u(10), u(15.5), u(21.5)), u(1.5), fill=W)
    d.ellipse((u(10.6), u(13), u(13.4), u(15.8)), fill=(0, 0, 0, 0))


def g_wallet(d, u):
    d.rounded_rectangle((u(2.5), u(5.5), u(21.5), u(20.5)), u(3), fill=W)
    d.rounded_rectangle((u(13.5), u(10), u(22.5), u(16)), u(2), fill=(0, 0, 0, 0))
    d.ellipse((u(16), u(11.8), u(18.4), u(14.2)), fill=W)
    d.line([(u(4.5), u(5.5)), (u(17), u(2.5))], fill=W, width=int(u(2)))


def g_stopwatch(d, u):
    d.ellipse((u(4), u(5.5), u(20), u(21.5)), outline=W, width=int(u(2.2)))
    d.rectangle((u(10), u(2), u(14), u(4)), fill=W)
    d.line([(u(12), u(13.5)), (u(12), u(8.5))], fill=W, width=int(u(2)))
    d.line([(u(17.5), u(5.5)), (u(19.5), u(3.5))], fill=W, width=int(u(2)))


def g_alarm(d, u):
    d.ellipse((u(4), u(4.5), u(20), u(20.5)), outline=W, width=int(u(2.2)))
    d.line([(u(12), u(12.5)), (u(12), u(8))], fill=W, width=int(u(2)))
    d.line([(u(12), u(12.5)), (u(15.5), u(14.5))], fill=W, width=int(u(2)))
    d.line([(u(3), u(6)), (u(6.5), u(2.5))], fill=W, width=int(u(2.2)))
    d.line([(u(21), u(6)), (u(17.5), u(2.5))], fill=W, width=int(u(2.2)))
    d.line([(u(7), u(19.5)), (u(5), u(22))], fill=W, width=int(u(2)))
    d.line([(u(17), u(19.5)), (u(19), u(22))], fill=W, width=int(u(2)))


def g_link(d, u):
    d.rounded_rectangle((u(2.5), u(8), u(13.5), u(16)), u(4), outline=W, width=int(u(2.2)))
    d.rounded_rectangle((u(10.5), u(8), u(21.5), u(16)), u(4), outline=W, width=int(u(2.2)))


GLYPHS = [g_heart, g_heart_full, g_home, g_plus, g_user, g_call, g_hangup, g_camera,
          g_switch, g_trash, g_back, g_send, g_star, g_wifi, g_battery, g_signal,
          g_gallery, g_gear, g_mic, g_bell, g_clock, g_people, g_close, g_wallpaper,
          g_next, g_play, g_pause, g_prev, g_skip, g_shuffle, g_repeat, g_repeat1,
          g_folder, g_note, g_vol_low, g_vol_high, g_refresh, g_down, g_sun, g_moon,
          g_rain, g_storm, g_cloud, g_pin, g_nav, g_skull, g_lock, g_sliders,
          g_upload, g_speaker, g_headphones, g_globe, g_crown, g_thumb, g_download, g_queue,
          g_chat, g_map, g_notes, g_flash, g_wallet, g_stopwatch, g_alarm, g_link]


# =====================================================================================================================
#  Fondos de pantalla (180 × 390 cada uno)
# =====================================================================================================================
WW, WH = 180, 390


def blocky_hills(img, base_y, amp, color, seed, step=6):
    d = ImageDraw.Draw(img)
    rnd = random.Random(seed)
    y = base_y
    for x in range(0, WW, step):
        y += rnd.choice([-step, 0, 0, step]) * (amp / 20)
        y = max(base_y - amp, min(base_y + amp, y))
        d.rectangle((x, int(y), x + step - 1, WH), fill=color)


def wall_sunset():
    img = grad(WW, WH, [(0, hexc("#2b1055")), (0.45, hexc("#d53369")), (0.75, hexc("#ff9a44")), (1, hexc("#ffd36e"))])
    d = ImageDraw.Draw(img)
    d.rectangle((62, 205, 118, 261), fill=hexc("#ffe7a3"))
    d.rectangle((66, 209, 114, 257), fill=hexc("#fff4d0"))
    blocky_hills(img, 270, 30, hexc("#7a2a5c"), 3)
    blocky_hills(img, 300, 24, hexc("#4a1a48"), 5)
    blocky_hills(img, 330, 18, hexc("#2a0f30"), 9)
    return img


def wall_night():
    img = grad(WW, WH, [(0, hexc("#020111")), (0.6, hexc("#0b1f4a")), (1, hexc("#1b3a6b"))])
    d = ImageDraw.Draw(img)
    rnd = random.Random(7)
    for _ in range(120):
        x, y = rnd.randrange(WW), rnd.randrange(260)
        b = rnd.randrange(120, 255)
        s = 1 if rnd.random() < 0.85 else 2
        d.rectangle((x, y, x + s - 1, y + s - 1), fill=(b, b, min(255, b + 20), 255))
    d.rectangle((112, 46, 152, 86), fill=hexc("#e8eef8"))
    d.rectangle((120, 54, 128, 62), fill=hexc("#c9d2e0"))
    d.rectangle((136, 68, 144, 76), fill=hexc("#c9d2e0"))
    d.rectangle((124, 72, 128, 76), fill=hexc("#d6dde8"))
    blocky_hills(img, 300, 34, hexc("#0e1a33"), 11, 8)
    blocky_hills(img, 335, 20, hexc("#060c1a"), 13, 8)
    return img


def wall_ocean():
    img = grad(WW, WH, [(0, hexc("#00c6fb")), (0.5, hexc("#005bea")), (1, hexc("#001a52"))])
    d = ImageDraw.Draw(img)
    for i in range(14):
        y = 120 + i * 20
        col = (255, 255, 255, 30 + i * 3)
        pts = [(x, y + math.sin(x / 14 + i) * 5) for x in range(0, WW + 1, 4)]
        over = Image.new("RGBA", img.size, (0, 0, 0, 0))
        ImageDraw.Draw(over).line(pts, fill=col, width=2)
        img = Image.alpha_composite(img, over)
    return img


def wall_aurora():
    img = grad(WW, WH, [(0, hexc("#050816")), (1, hexc("#0b1a2e"))])
    over = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(over)
    for k, (col, off) in enumerate([("#2bff88", 0), ("#00d4ff", 40), ("#b84dff", 80)]):
        c = hexc(col, 90)
        for x in range(0, WW, 2):
            y = 120 + off + math.sin(x / 26 + k) * 30 + math.sin(x / 9 + k * 2) * 6
            d.line([(x, y - 60), (x, y + 10)], fill=c, width=2)
    over = over.filter(ImageFilter.GaussianBlur(10))
    img = Image.alpha_composite(img, over)
    d = ImageDraw.Draw(img)
    rnd = random.Random(21)
    for _ in range(70):
        x, y = rnd.randrange(WW), rnd.randrange(WH)
        d.point((x, y), fill=(255, 255, 255, rnd.randrange(80, 220)))
    blocky_hills(img, 330, 18, hexc("#03060d"), 17, 6)
    return img


def wall_pastel():
    img = grad(WW, WH, [(0, hexc("#fbc2eb")), (1, hexc("#a6c1ee"))])
    over = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(over)
    for (x, y, r, c) in [(40, 90, 60, "#ffffff"), (150, 200, 70, "#ffd1ff"), (60, 300, 80, "#c2e9fb"), (140, 360, 50, "#fff1a8")]:
        d.ellipse((x - r, y - r, x + r, y + r), fill=hexc(c, 140))
    over = over.filter(ImageFilter.GaussianBlur(22))
    return Image.alpha_composite(img, over)


def wall_forest():
    img = grad(WW, WH, [(0, hexc("#a8e063")), (0.5, hexc("#56ab2f")), (1, hexc("#134e0f"))])
    d = ImageDraw.Draw(img)
    rnd = random.Random(5)
    for row, (y0, col, trunk) in enumerate([(220, "#2f7d1f", "#5b3a1e"), (270, "#256418", "#4a2f18"), (320, "#1a4a10", "#3a2412")]):
        x = -10 + row * 8
        while x < WW:
            w = rnd.choice([18, 24, 30])
            h = rnd.choice([40, 52, 64])
            d.rectangle((x + w // 2 - 3, y0, x + w // 2 + 2, y0 + 30), fill=hexc(trunk))
            d.rectangle((x, y0 - h, x + w, y0 + 4), fill=hexc(col))
            d.rectangle((x + 4, y0 - h - 8, x + w - 4, y0 - h), fill=hexc(col))
            x += w + rnd.choice([2, 6, 10])
    d.rectangle((0, 350, WW, WH), fill=hexc("#0f330a"))
    return img


def wall_synthwave():
    img = grad(WW, WH, [(0, hexc("#0d0221")), (0.45, hexc("#541388")), (0.62, hexc("#ff3c8e")), (0.63, hexc("#1a0633")), (1, hexc("#0d0221"))])
    d = ImageDraw.Draw(img)
    # sol con rayas
    for y in range(150, 245):
        dy = (y - 200) / 48
        if abs(dy) > 1:
            continue
        if y > 205 and (y // 6) % 2 == 0:
            continue
        w = math.sqrt(1 - dy * dy) * 48
        t = (y - 150) / 95
        d.line([(90 - w, y), (90 + w, y)], fill=lerp(hexc("#ffe66d"), hexc("#ff3c8e"), t))
    # cuadrícula del piso
    for i in range(-12, 13):
        d.line([(90 + i * 8, 246), (90 + i * 40, WH)], fill=hexc("#ff3cf5", 170), width=1)
    y, k = 247, 2
    while y < WH:
        d.line([(0, y), (WW, y)], fill=hexc("#ff3cf5", 170), width=1)
        y += k
        k += 2
    rnd = random.Random(31)
    for _ in range(50):
        x, yy = rnd.randrange(WW), rnd.randrange(140)
        d.point((x, yy), fill=(255, 255, 255, rnd.randrange(90, 230)))
    return img


def wall_galaxy():
    img = grad(WW, WH, [(0, hexc("#05010f")), (1, hexc("#0a0322"))])
    over = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(over)
    for (x, y, r, c) in [(60, 140, 90, "#7b2ff7"), (130, 230, 80, "#f107a3"), (80, 300, 70, "#00d4ff")]:
        d.ellipse((x - r, y - r, x + r, y + r), fill=hexc(c, 110))
    over = over.filter(ImageFilter.GaussianBlur(30))
    img = Image.alpha_composite(img, over)
    d = ImageDraw.Draw(img)
    rnd = random.Random(44)
    for _ in range(220):
        x, y = rnd.randrange(WW), rnd.randrange(WH)
        b = rnd.randrange(110, 255)
        d.point((x, y), fill=(b, b, 255, 255))
    for _ in range(10):
        x, y = rnd.randrange(WW), rnd.randrange(WH)
        d.rectangle((x, y, x + 1, y + 1), fill=(255, 255, 255, 255))
    return img


def main():
    for name, (b, l, dk) in CASES.items():
        phone_item(name, b, l, dk)
    apps = [icon_insta(), icon_camera(), icon_gallery(), icon_phone(), icon_patreon(), icon_settings(), icon_music(), icon_weather(),
            icon_compass(), icon_calc()]
    atlas = Image.new("RGBA", (S * len(apps), S), (0, 0, 0, 0))
    for i, a in enumerate(apps):
        atlas.paste(a, (i * S, 0))
    save(atlas, "gui/apps.png", blur=True)
    ui = Image.new("RGBA", (G * 8, G * 8), (0, 0, 0, 0))
    for i, fn in enumerate(GLYPHS):
        ui.paste(glyph(fn), ((i % 8) * G, (i // 8) * G))
    save(ui, "gui/ui.png", blur=True)
    walls = [wall_sunset(), wall_night(), wall_ocean(), wall_aurora(), wall_pastel(), wall_forest(), wall_synthwave(), wall_galaxy()]
    wa = Image.new("RGBA", (WW * len(walls), WH), (0, 0, 0, 255))
    for i, w in enumerate(walls):
        wa.paste(w.convert("RGBA"), (i * WW, 0))
    save(wa.convert("RGB").convert("RGBA"), "gui/wallpapers.png", blur=True)
    print("texturas listas en", TEX)


if __name__ == "__main__":
    main()
