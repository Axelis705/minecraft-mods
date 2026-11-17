#!/usr/bin/env python3
"""Genera las texturas del celular: el objeto (8 colores), los iconos de las apps, los iconos pequeños de la interfaz y los fondos
de pantalla. Uso: python3 celular/herramientas/assets.py"""
import math
import json
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


def icon_flash():
    big = app_base([(0, hexc("#3a3a40")), (1, hexc("#0d0d10"))])
    # haz de luz (capa aparte para que se mezcle)
    beam = Image.new("RGBA", big.size, (0, 0, 0, 0))
    bd = ImageDraw.Draw(beam)
    bd.polygon([(U(32), U(30)), (U(10), U(4)), (U(54), U(4))], fill=(255, 236, 150, 110))
    bd.polygon([(U(32), U(30)), (U(20), U(6)), (U(44), U(6))], fill=(255, 246, 200, 120))
    big = Image.alpha_composite(big, beam)
    d = ImageDraw.Draw(big)
    d.rounded_rectangle((U(23), U(26), U(41), U(34)), radius=U(2), fill=hexc("#f2f2f7"))
    d.rectangle((U(24), U(25), U(40), U(27)), fill=hexc("#fff3b0"))
    d.rounded_rectangle((U(26.5), U(33), U(37.5), U(57)), radius=U(3), fill=hexc("#c7c7cc"))
    d.rounded_rectangle((U(26.5), U(33), U(29), U(57)), radius=U(1), fill=hexc("#e5e5ea"))
    d.ellipse((U(29.5), U(41), U(34.5), U(46)), fill=hexc("#ffcc00"))
    return finish(big)


def icon_messages():
    big = app_base([(0, hexc("#5fe07a")), (1, hexc("#1fb84a"))])
    d = ImageDraw.Draw(big)
    d.ellipse((U(12), U(15), U(52), U(45)), fill=(255, 255, 255, 255))
    d.polygon([(U(19), U(38)), (U(14), U(51)), (U(29), U(43))], fill=(255, 255, 255, 255))
    return finish(big)


def icon_notes():
    big = app_base([(0, hexc("#ffffff")), (1, hexc("#f2f2f2"))])
    d = ImageDraw.Draw(big)
    d.rectangle((0, 0, U(64), U(17)), fill=hexc("#ffd60a"))
    for i, y in enumerate((27, 36, 45, 54)):
        d.line([(U(12), U(y)), (U(52 if i < 3 else 36), U(y))], fill=hexc("#c7c7cc"), width=U(2))
    d.ellipse((U(44), U(40), U(58), U(54)), fill=hexc("#ff9f0a"))
    d.line([(U(51), U(43)), (U(51), U(47.5)), (U(54), U(49))], fill=(255, 255, 255, 255), width=U(1.6))
    return finish(big)


def icon_map():
    big = app_base([(0, hexc("#b8e08f")), (1, hexc("#8cc56a"))])
    d = ImageDraw.Draw(big)
    d.line([(U(0), U(20)), (U(20), U(28)), (U(34), U(26)), (U(64), U(40))], fill=hexc("#62b8f0"), width=U(7))
    d.line([(U(10), U(64)), (U(30), U(0))], fill=hexc("#f7f2e2"), width=U(5))
    d.line([(U(0), U(50)), (U(64), U(56))], fill=hexc("#f7f2e2"), width=U(4))
    # alfiler
    d.ellipse((U(34), U(10), U(52), U(28)), fill=hexc("#ff3b30"))
    d.polygon([(U(36), U(23)), (U(50), U(23)), (U(43), U(38))], fill=hexc("#ff3b30"))
    d.ellipse((U(40), U(16), U(46), U(22)), fill=(255, 255, 255, 255))
    return finish(big)


def icon_wallet():
    big = app_base([(0, hexc("#2b2b30")), (1, hexc("#0c0c0e"))])
    d = ImageDraw.Draw(big)
    d.rounded_rectangle((U(10), U(14), U(54), U(34)), radius=U(4), fill=hexc("#d4af37"))
    d.rounded_rectangle((U(10), U(22), U(54), U(42)), radius=U(4), fill=hexc("#0a84ff"))
    d.rounded_rectangle((U(10), U(30), U(54), U(52)), radius=U(4), fill=hexc("#f2f2f7"))
    # ficha
    d.ellipse((U(34), U(34), U(50), U(50)), fill=hexc("#e03b3b"))
    d.ellipse((U(38), U(38), U(46), U(46)), fill=hexc("#ffffff"))
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


# =====================================================================================================================
#  El celular en 3D: cuerpo con grosor, marco, cámara atrás, botones y la pantalla encendida (32 × 64) que ven los demás
# =====================================================================================================================
# El orden es el número que va en custom_model_data (1 = inicio ... ); tiene que ser igual al de PhoneUse.java.
# "lock" (sin número) es la pantalla normal (el celular guardado); "ear" es la pantalla apagada pegada a la oreja.
SCREENS = ["home", "insta", "camera", "gallery", "call", "patreon", "settings", "music", "weather", "compass", "calc", "messages",
           "notes", "map", "wallet", "flash", "jam", "ear"]
SW, SH = 32, 64       # píxeles de la pantalla
SS4 = 4               # se dibuja a 4x y se achica (bordes suaves)

# Letras de 3 × 5 para la hora y los números de las pantallas
FONT = {
    "0": ["111", "101", "101", "101", "111"], "1": ["010", "110", "010", "010", "111"], "2": ["111", "001", "111", "100", "111"],
    "3": ["111", "001", "111", "001", "111"], "4": ["101", "101", "111", "001", "001"], "5": ["111", "100", "111", "001", "111"],
    "6": ["111", "100", "111", "101", "111"], "7": ["111", "001", "010", "010", "010"], "8": ["111", "101", "111", "101", "111"],
    "9": ["111", "101", "111", "001", "111"], ":": ["0", "1", "0", "1", "0"], "°": ["11", "11", "00", "00", "00"],
    ".": ["0", "0", "0", "0", "1"], "+": ["000", "010", "111", "010", "000"], "-": ["000", "000", "111", "000", "000"],
}


def text_px(img, s, x, y, col, scale=1):
    """Texto de píxeles (sin suavizar) encima de la imagen ya achicada."""
    p = img.load()
    c = hexc(col) if isinstance(col, str) else col
    for ch in s:
        g = FONT.get(ch)
        if g is None:
            x += 2 * scale
            continue
        for gy, row in enumerate(g):
            for gx, v in enumerate(row):
                if v == "1":
                    for sy in range(scale):
                        for sx in range(scale):
                            px, py = x + gx * scale + sx, y + gy * scale + sy
                            if 0 <= px < img.width and 0 <= py < img.height:
                                p[px, py] = c
        x += (len(g[0]) + 1) * scale
    return x


def text_w(s, scale=1):
    return sum((len(FONT[ch][0]) + 1) * scale if ch in FONT else 2 * scale for ch in s) - scale


class Scr:
    """Lienzo grande (4x) para dibujar una pantalla; .done() la achica a 32 × 64 con las esquinas redondeadas."""

    def __init__(self, bg):
        self.big = Image.new("RGBA", (SW * SS4, SH * SS4), hexc(bg) if isinstance(bg, str) else bg)
        self.d = ImageDraw.Draw(self.big)

    def _over(self, fn):
        """Dibuja en una capa aparte y la mezcla encima (así el color semitransparente se ve como vidrio y no como un hueco)."""
        layer = Image.new("RGBA", self.big.size, (0, 0, 0, 0))
        fn(ImageDraw.Draw(layer))
        self.big = Image.alpha_composite(self.big, layer)
        self.d = ImageDraw.Draw(self.big)

    def grad(self, top, bot, y0=0, y1=SH):
        g = grad(SW * SS4, (y1 - y0) * SS4, [(0, hexc(top)), (1, hexc(bot))])
        self.big.paste(g, (0, int(y0 * SS4)))
        self.d = ImageDraw.Draw(self.big)

    def rect(self, x0, y0, x1, y1, c, r=0):
        box = (x0 * SS4, y0 * SS4, x1 * SS4 - 1, y1 * SS4 - 1)
        col = hexc(c) if isinstance(c, str) else c
        if r:
            self._over(lambda d: d.rounded_rectangle(box, radius=r * SS4, fill=col))
        else:
            self._over(lambda d: d.rectangle(box, fill=col))

    def circ(self, cx, cy, r, c, outline=None, w=0):
        col = None if c is None else hexc(c) if isinstance(c, str) else c
        self._over(lambda d: d.ellipse(((cx - r) * SS4, (cy - r) * SS4, (cx + r) * SS4, (cy + r) * SS4), fill=col,
                                       outline=(hexc(outline) if outline else None), width=int(w * SS4)))

    def line(self, pts, c, w=1.0):
        col = hexc(c) if isinstance(c, str) else c
        self._over(lambda d: d.line([(x * SS4, y * SS4) for x, y in pts], fill=col, width=max(1, int(w * SS4))))

    def poly(self, pts, c):
        col = hexc(c) if isinstance(c, str) else c
        self._over(lambda d: d.polygon([(x * SS4, y * SS4) for x, y in pts], fill=col))

    def status(self, dark=False):
        """Barra de arriba: hora, isla, señal y batería."""
        self.rect(14.5, 1, 20.5, 3.5, "#000000", 1.25)
        self.bar_dark = dark

    def home_bar(self, dark=False):
        self.rect(11, 61.2, 21, 62.2, "#1c1c1e" if dark else "#f2f2f2", 0.5)

    def done(self, clock=True, dark=False):
        small = self.big.resize((SW, SH), Image.LANCZOS)
        if hasattr(self, "bar_dark"):
            col = "#111111" if self.bar_dark else "#ffffff"
            if clock:
                text_px(small, "9:41", 1, 1, col)
            p = small.load()
            c = hexc(col)
            for x in range(25, 29):           # batería
                p[x, 2] = c
                p[x, 3] = c
            p[29, 2] = c
        # esquinas redondeadas (transparentes: se ve el marco negro)
        p = small.load()
        for (x, y) in ((0, 0), (1, 0), (0, 1), (SW - 1, 0), (SW - 2, 0), (SW - 1, 1), (0, SH - 1), (1, SH - 1), (0, SH - 2),
                       (SW - 1, SH - 1), (SW - 2, SH - 1), (SW - 1, SH - 2)):
            p[x, y] = (0, 0, 0, 0)
        out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))       # cuadrada: la pantalla usa la mitad izquierda
        out.paste(small, (0, 0))
        return out


def app_grid(s, y0, rows, cols=4, size=5, gapx=7, gapy=8, x0=3.5, seed=1):
    colors = ["#ff375f", "#30d158", "#0a84ff", "#ff9f0a", "#bf5af2", "#64d2ff", "#ffd60a", "#fa233b", "#5e5ce6", "#34c759", "#ff6482",
              "#8e8e93", "#ac8e68", "#00c7be", "#ff2d55", "#4cd964"]
    rnd = random.Random(seed)
    for r in range(rows):
        for c in range(cols):
            col = colors[rnd.randrange(len(colors))]
            x, y = x0 + c * gapx, y0 + r * gapy
            s.rect(x, y, x + size, y + size, col, 1.4)
            s.rect(x + 1.5, y + 1.5, x + size - 1.5, y + size - 1.5, lerp(hexc(col), (255, 255, 255, 255), 0.55), 0.6)


def scr_lock():
    s = Scr("#000000")
    s.grad("#5b2a86", "#1d4e89", 0, 34)
    s.grad("#1d4e89", "#f72585", 34, 64)
    s.circ(26, 46, 14, (255, 255, 255, 30))
    s.circ(5, 14, 9, (255, 214, 10, 40))
    s.status()
    # botones de linterna y cámara
    s.circ(6, 55, 3, (0, 0, 0, 90))
    s.circ(26, 55, 3, (0, 0, 0, 90))
    s.home_bar()
    img = s.done(clock=False)
    text_px(img, "9:41", 3, 12, "#ffffff", 2)
    p = img.load()
    for x in range(9, 23):                # fecha (rayita)
        p[x, 9] = (255, 255, 255, 170)
    for (x, y) in ((5, 54), (6, 54), (6, 55), (5, 56)):
        p[x, y] = hexc("#ffffff")
    for (x, y) in ((25, 54), (26, 54), (27, 54), (25, 55), (26, 55), (27, 55)):
        p[x, y] = hexc("#ffffff")
    return img


def scr_home():
    s = Scr("#000000")
    s.grad("#3a7bd5", "#6a4fd8")
    s.status()
    # widget de la canción
    s.rect(3, 6, 29, 16, (0, 0, 0, 70), 2.5)
    s.rect(4.5, 7.5, 11.5, 14.5, "#fa233b", 1.5)
    s.rect(13, 9, 26, 10.2, (255, 255, 255, 220), 0.5)
    s.rect(13, 12, 22, 13, (255, 255, 255, 140), 0.5)
    app_grid(s, 20, 3, seed=4)
    # dock de vidrio
    s.rect(2, 50, 30, 59, (255, 255, 255, 70), 3)
    for i, col in enumerate(("#30d158", "#34c759", "#ff375f", "#fa233b")):
        x = 4 + i * 6.7
        s.rect(x, 52, x + 5, 57, col, 1.4)
    s.home_bar()
    return s.done()


def scr_insta():
    s = Scr("#ffffff")
    s.status(dark=True)
    s.rect(0, 5, 32, 10, "#ffffff")
    s.rect(3, 6.5, 14, 8.5, "#262626", 0.6)          # el nombre de la app
    for i in range(4):                                # historias
        cx = 6 + i * 7
        s.circ(cx, 14, 3, "#d62976")
        s.circ(cx, 14, 2.3, "#ffffff")
        s.circ(cx, 14, 1.8, ["#feda75", "#8a3ab9", "#4c68d7", "#fa7e1e"][i])
    # una publicación: cielo, sol, montañas
    s.grad("#4facfe", "#a1f0ff", 19, 39)
    s.circ(24, 24, 3, "#ffd60a")
    s.poly([(0, 39), (9, 28), (17, 39)], "#3d7d4a")
    s.poly([(10, 39), (21, 30), (32, 39)], "#2e6b3b")
    s.rect(0, 35, 32, 39, "#5aa469")
    # me gusta, comentar, enviar
    s.circ(4, 42, 1.6, "#ff3040")
    s.circ(9, 42, 1.4, "#262626")
    s.circ(9, 42, 0.8, "#ffffff")
    s.poly([(12.5, 40.6), (15.5, 42), (12.5, 43.4)], "#262626")
    s.rect(3, 45, 20, 46, "#262626", 0.5)
    s.rect(3, 47.5, 26, 48.3, "#8e8e8e", 0.4)
    s.grad("#fd1d1d", "#fcb045", 51, 60)
    s.rect(0, 59, 32, 64, "#ffffff")
    for i in range(5):
        s.circ(4 + i * 6, 61, 1.2, "#262626")
    return s.done()


def scr_camera():
    s = Scr("#000000")
    s.status()
    s.grad("#87c5ff", "#d9f0ff", 8, 30)
    s.circ(22, 14, 3.5, "#fff3b0")
    s.poly([(0, 44), (10, 26), (20, 44)], "#6c7a89")
    s.poly([(7, 30.5), (10, 26), (13, 30.5)], "#ffffff")
    s.poly([(12, 44), (24, 30), (32, 40), (32, 44)], "#4d5b6b")
    s.rect(0, 40, 32, 48, "#4c9a4f")
    # cuadrícula de tercios
    for x in (10.6, 21.3):
        s.rect(x, 8, x + 0.3, 48, (255, 255, 255, 90))
    for y in (21.3, 34.6):
        s.rect(0, y, 32, y + 0.3, (255, 255, 255, 90))
    s.rect(11, 50, 21, 51.2, "#ffcc00", 0.5)         # "FOTO"
    s.circ(16, 56.5, 4.5, "#ffffff")
    s.circ(16, 56.5, 3.6, "#000000")
    s.circ(16, 56.5, 3.1, "#ffffff")
    s.rect(4, 54.5, 8, 58.5, "#3a3a3c", 1)            # última foto
    s.circ(27, 56.5, 2.4, "#2c2c2e")
    return s.done()


def scr_gallery():
    s = Scr("#ffffff")
    s.status(dark=True)
    s.rect(3, 6, 18, 8, "#111111", 0.6)
    cols = ["#ff9f0a", "#30d158", "#0a84ff", "#ff375f", "#bf5af2", "#64d2ff", "#ffd60a", "#ac8e68", "#5e5ce6", "#34c759", "#ff6482",
            "#00c7be", "#fa233b", "#8e8e93", "#4cd964"]
    k = 0
    for r in range(5):
        for c in range(3):
            x, y = c * 10.8, 10 + r * 10.2
            col = cols[k % len(cols)]
            s.rect(x + 0.4, y, x + 10.4, y + 9.8, col)
            s.rect(x + 0.4, y + 6, x + 10.4, y + 9.8, lerp(hexc(col), (0, 0, 0, 255), 0.25))
            s.circ(x + 7.5, y + 3, 1.5, lerp(hexc(col), (255, 255, 255, 255), 0.6))
            k += 1
    s.home_bar(dark=True)
    return s.done()


def scr_call():
    s = Scr("#000000")
    s.grad("#2c3e50", "#000000")
    s.status()
    # cara de quien llama (cuadrada, como en Minecraft)
    s.rect(10, 9, 22, 21, "#8d5a3b", 2)
    s.rect(10, 9, 22, 13, "#3b2a1a", 2)
    s.rect(12, 15, 14.5, 16.5, "#ffffff")
    s.rect(17.5, 15, 20, 16.5, "#ffffff")
    s.rect(13, 15.3, 14.5, 16.5, "#3b5ba8")
    s.rect(18.5, 15.3, 20, 16.5, "#3b5ba8")
    s.rect(13.5, 18.5, 18.5, 19.5, "#5a3825")
    s.rect(9, 24, 23, 25.6, "#ffffff", 0.8)
    s.rect(11, 27.5, 21, 28.6, "#30d158", 0.5)
    for i in range(3):
        for j in range(2):
            s.circ(7 + i * 9, 37 + j * 8, 3, (255, 255, 255, 50))
    s.circ(16, 55, 4, "#ff3b30")
    s.rect(13.5, 54.5, 18.5, 55.8, "#ffffff", 0.6)
    return s.done()


def scr_ear():
    """Pegado a la oreja: la pantalla se apaga (solo un reflejo)."""
    s = Scr("#050506")
    s.poly([(0, 0), (12, 0), (0, 20)], (255, 255, 255, 12))
    s.poly([(32, 40), (32, 64), (20, 64)], (255, 255, 255, 8))
    return s.done(clock=False)


def scr_patreon():
    s = Scr("#ffffff")
    s.rect(0, 0, 32, 20, "#ff424d")
    s.status()
    s.circ(13, 12, 4.5, "#ffffff")
    s.rect(18, 8, 20, 16.5, "#141414")
    for i in range(5):
        y = 23 + i * 7.5
        s.circ(5, y + 2.5, 2.5, ["#ffd700", "#c0c0c0", "#cd7f32", "#ff424d", "#ffd700"][i])
        s.rect(10, y + 1, 27, y + 2.3, "#141414", 0.6)
        s.rect(10, y + 3.5, 22, y + 4.4, "#9e9e9e", 0.4)
    s.home_bar(dark=True)
    return s.done()


def scr_settings():
    s = Scr("#f2f2f7")
    s.status(dark=True)
    s.rect(3, 6, 20, 8.4, "#111111", 0.6)
    s.rect(2, 11, 30, 18, "#ffffff", 2)
    s.circ(6.5, 14.5, 2.5, "#8e8e93")
    s.rect(11, 13, 24, 14.3, "#111111", 0.5)
    s.rect(11, 15.3, 19, 16.1, "#8e8e93", 0.4)
    cols = ["#ff9500", "#0a84ff", "#34c759", "#ff3b30", "#5856d6", "#ff2d55", "#636366", "#30d158"]
    s.rect(2, 20, 30, 20 + 8 * 4.6, "#ffffff", 2)
    for i, c in enumerate(cols):
        y = 21 + i * 4.6
        s.rect(3.5, y, 6.5, y + 3, c, 0.8)
        s.rect(8.5, y + 1, 20, y + 2, "#3a3a3c", 0.4)
        on = i % 3 != 1
        s.rect(23, y + 0.3, 28, y + 2.7, "#34c759" if on else "#d1d1d6", 1.2)
        s.circ(26.8 if on else 24.2, y + 1.5, 1.05, "#ffffff")
    s.home_bar(dark=True)
    return s.done()


def scr_music():
    s = Scr("#000000")
    s.grad("#7a1a2c", "#1a0508")
    s.status()
    s.rect(14, 5, 18, 5.6, (255, 255, 255, 120), 0.3)
    # portada
    s.rect(5, 9, 27, 31, (0, 0, 0, 90), 2.5)
    s.big.paste(grad(22 * SS4, 22 * SS4, [(0, hexc("#ff5f6d")), (1, hexc("#ffc371"))], 45), (5 * SS4, 8 * SS4),
                Image.new("L", (22 * SS4, 22 * SS4), 255))
    s.d = ImageDraw.Draw(s.big)
    s.circ(13, 23, 2.5, "#ffffff")
    s.rect(14.6, 13, 16, 23, "#ffffff")
    s.rect(14.6, 13, 20, 15, "#ffffff")
    s.rect(4, 34, 28, 36, (255, 255, 255, 230), 0.6)
    s.rect(4, 37.5, 18, 38.7, (255, 255, 255, 140), 0.4)
    # avance
    s.rect(4, 42, 28, 42.8, (255, 255, 255, 70), 0.4)
    s.rect(4, 42, 15, 42.8, "#ffffff", 0.4)
    s.circ(15, 42.4, 1.1, "#ffffff")
    # controles
    s.poly([(10, 49), (6, 51.5), (10, 54)], "#ffffff")
    s.poly([(7, 49), (3, 51.5), (7, 54)], "#ffffff")
    s.rect(14, 48.5, 15.6, 54.5, "#ffffff", 0.5)
    s.rect(17.4, 48.5, 19, 54.5, "#ffffff", 0.5)
    s.poly([(22, 49), (26, 51.5), (22, 54)], "#ffffff")
    s.poly([(25, 49), (29, 51.5), (25, 54)], "#ffffff")
    s.home_bar()
    return s.done()


def scr_weather():
    s = Scr("#000000")
    s.grad("#2f80ed", "#56ccf2")
    s.status()
    s.circ(23, 15, 5, "#ffd60a")
    s.circ(23, 15, 6.5, (255, 214, 10, 60))
    s.circ(10, 20, 4, "#ffffff")
    s.circ(15, 19, 5, "#ffffff")
    s.rect(6, 19, 20, 24, "#ffffff", 2)
    s.rect(3, 40, 29, 58, (255, 255, 255, 50), 3)
    for i in range(4):
        x = 6 + i * 6.5
        s.circ(x, 47, 1.8, ["#ffd60a", "#ffd60a", "#ffffff", "#8e8e93"][i])
        s.rect(x - 1.5, 52, x + 1.5, 53, (255, 255, 255, 200), 0.4)
    img = s.done()
    text_px(img, "21°", 6, 29, "#ffffff", 2)
    return img


def scr_compass():
    s = Scr("#0e0f13")
    s.status()
    s.circ(16, 26, 13, "#1c1c1e")
    for i in range(36):
        a = math.radians(i * 10)
        r0 = 11 if i % 9 == 0 else 12
        s.line([(16 + math.sin(a) * r0, 26 - math.cos(a) * r0), (16 + math.sin(a) * 13, 26 - math.cos(a) * 13)],
               "#ffffff" if i % 9 == 0 else "#8e8e93", 0.5)
    s.poly([(16, 15), (14.3, 26), (17.7, 26)], "#ff3b30")
    s.poly([(16, 37), (14.3, 26), (17.7, 26)], "#e5e5ea")
    s.circ(16, 26, 1.2, "#0e0f13")
    s.rect(9, 44, 23, 46, "#ffffff", 0.6)
    s.rect(7, 48.5, 25, 49.5, "#8e8e93", 0.4)
    s.rect(3, 53, 29, 59, "#1c1c1e", 2)
    s.circ(6.5, 56, 1.5, "#ff9f0a")
    s.rect(9.5, 55.5, 22, 56.5, "#e5e5ea", 0.4)
    return s.done()


def scr_calc():
    s = Scr("#000000")
    s.status()
    for r in range(5):
        for c in range(4):
            x, y = 2 + c * 7.3, 28 + r * 7
            if r == 4 and c == 0:
                s.rect(x, y, x + 13, y + 5.6, "#333333", 2.8)
                continue
            if r == 4 and c == 1:
                continue
            col = "#ff9f0a" if c == 3 else ("#a5a5a5" if r == 0 else "#333333")
            s.circ(x + 2.9, y + 2.8, 2.9, col)
    img = s.done()
    text_px(img, "128", 31 - text_w("128", 2), 16, "#ffffff", 2)
    return img


def scr_messages():
    s = Scr("#ffffff")
    s.status(dark=True)
    s.rect(0, 5, 32, 11, "#f7f7f7")
    s.circ(16, 8, 2.2, "#c7c7cc")
    for i, (mine, w, h) in enumerate(((False, 18, 4), (True, 15, 4), (True, 21, 7), (False, 12, 4), (False, 20, 7), (True, 14, 4))):
        y = 13 + sum(hh + 2 for _, _, hh in ((False, 18, 4), (True, 15, 4), (True, 21, 7), (False, 12, 4), (False, 20, 7),
                                              (True, 14, 4))[:i])
        x0 = 30 - w if mine else 2
        s.rect(x0, y, x0 + w, y + h, "#0a84ff" if mine else "#e5e5ea", 2)
    s.rect(2, 57, 26, 61, "#ffffff", 2)
    s.d.rounded_rectangle((2 * SS4, 57 * SS4, 26 * SS4, 61 * SS4), radius=2 * SS4, outline=hexc("#d1d1d6"), width=SS4 // 2)
    s.circ(29, 59, 2, "#0a84ff")
    return s.done()


def scr_notes():
    s = Scr("#fffdf2")
    s.status(dark=True)
    s.rect(0, 5, 32, 12, "#ffd60a")
    s.rect(3, 7.5, 16, 9.6, "#3a2e00", 0.6)
    for i in range(8):
        y = 15 + i * 5.5
        done = i in (1, 4)
        s.circ(4.5, y + 1, 1.6, "#ff9f0a" if done else "#c7c7cc")
        if not done:
            s.circ(4.5, y + 1, 1.0, "#fffdf2")
        s.rect(8, y + 0.5, 8 + [18, 13, 20, 9, 15, 19, 11, 16][i], y + 1.5, "#b0b0b5" if done else "#3a3a3c", 0.4)
    s.circ(27, 57, 3, "#ff9f0a")
    s.rect(26.4, 55, 27.6, 59, "#ffffff")
    s.rect(25, 56.4, 29, 57.6, "#ffffff")
    return s.done()


def scr_map():
    s = Scr("#a8d58c")
    s.rect(0, 0, 32, 64, "#b8de9c")
    s.rect(0, 14, 13, 30, "#9ccc7c", 2)
    s.rect(19, 38, 32, 54, "#9ccc7c", 2)
    s.line([(0, 22), (8, 26), (14, 25), (22, 33), (32, 31)], "#5ab4f0", 3.2)
    s.line([(9, 0), (11, 64)], "#fbf6e6", 2.2)
    s.line([(0, 46), (32, 41)], "#fbf6e6", 1.8)
    s.line([(18, 0), (25, 64)], "#fbf6e6", 1.2)
    s.status(dark=True)
    # tú (punto azul) y un amigo (alfiler)
    s.circ(13, 40, 3.2, (10, 132, 255, 60))
    s.circ(13, 40, 1.8, "#ffffff")
    s.circ(13, 40, 1.3, "#0a84ff")
    s.circ(24, 18, 3, "#ff3b30")
    s.poly([(21.6, 19.5), (26.4, 19.5), (24, 24)], "#ff3b30")
    s.circ(24, 18, 1.1, "#ffffff")
    s.rect(2, 55, 30, 62, (255, 255, 255, 235), 2.5)
    s.circ(6, 58.5, 2, "#8d5a3b")
    s.rect(10, 57.5, 22, 58.6, "#111111", 0.4)
    s.rect(10, 59.6, 18, 60.4, "#8e8e93", 0.4)
    return s.done()


def scr_wallet():
    s = Scr("#000000")
    s.status()
    s.rect(3, 7, 18, 9.4, "#ffffff", 0.6)
    s.rect(2, 13, 30, 28, "#0a84ff", 2.5)
    s.rect(2, 17, 30, 32, "#30d158", 2.5)
    s.big.paste(grad(28 * SS4, 16 * SS4, [(0, hexc("#f6d365")), (1, hexc("#b8860b"))], 30), (2 * SS4, 21 * SS4),
                Image.new("L", (28 * SS4, 16 * SS4), 255))
    s.d = ImageDraw.Draw(s.big)
    s.rect(5, 25, 9, 28, "#e8c46a", 0.8)
    s.rect(5, 25, 9, 26.2, "#c9a24a", 0.5)
    s.circ(25, 33, 2.4, "#e03b3b")
    s.circ(25, 33, 1.2, "#ffffff")
    for i in range(4):
        y = 42 + i * 5
        s.circ(5, y + 1.5, 1.8, "#2c2c2e")
        s.rect(9, y + 0.8, 20, y + 1.8, "#e5e5ea", 0.4)
        s.rect(23, y + 0.8, 29, y + 1.8, "#30d158" if i % 2 == 0 else "#ff453a", 0.4)
    return s.done()


def scr_flash():
    s = Scr("#fffbe8")
    s.big.paste(grad(SW * SS4, SH * SS4, [(0, hexc("#ffffff")), (0.55, hexc("#fff6cc")), (1, hexc("#ffe58a"))], 90), (0, 0))
    s.d = ImageDraw.Draw(s.big)
    for r, a in ((14, 40), (11, 70), (8, 110)):
        s.circ(16, 30, r, (255, 255, 255, a))
    s.circ(16, 30, 6, "#ffffff")
    s.rect(14.6, 25.5, 17.4, 34.5, "#ffcc00", 1)
    s.status(dark=True)
    return s.done()


def scr_jam():
    s = Scr("#000000")
    s.grad("#1c5c37", "#050a07")
    s.status()
    s.rect(5, 9, 27, 31, (0, 0, 0, 90), 2.5)
    s.big.paste(grad(22 * SS4, 22 * SS4, [(0, hexc("#1ed760")), (1, hexc("#0b6b33"))], 45), (5 * SS4, 8 * SS4),
                Image.new("L", (22 * SS4, 22 * SS4), 255))
    s.d = ImageDraw.Draw(s.big)
    for i, h in enumerate((5, 9, 13, 8, 11, 6)):          # ondas del Jam
        x = 8.5 + i * 2.8
        s.rect(x, 19 - h / 2, x + 1.6, 19 + h / 2, "#ffffff", 0.8)
    s.rect(4, 34, 28, 36, (255, 255, 255, 230), 0.6)
    s.rect(4, 37.5, 16, 38.7, "#1ed760", 0.4)
    for i in range(4):                                    # los del Jam
        s.circ(7 + i * 5.5, 45, 2.2, ["#8d5a3b", "#f2c094", "#5b3a29", "#e0ac69"][i])
        s.circ(7 + i * 5.5, 45, 2.2, None, outline="#1ed760", w=0.5)
    for i, col in enumerate(("#ff9f0a", "#bf5af2", "#30d158", "#ff375f")):   # botonera de DJ
        x = 3 + i * 6.8
        s.rect(x, 51, x + 5.6, 56, col, 1.2)
    s.home_bar()
    return s.done()


SCREEN_ART = {"lock": scr_lock, "home": scr_home, "insta": scr_insta, "camera": scr_camera, "gallery": scr_gallery, "call": scr_call,
              "patreon": scr_patreon, "settings": scr_settings, "music": scr_music, "weather": scr_weather, "compass": scr_compass,
              "calc": scr_calc, "messages": scr_messages, "notes": scr_notes, "map": scr_map, "wallet": scr_wallet, "flash": scr_flash,
              "jam": scr_jam, "ear": scr_ear}


def case_tex(name, base, light, dark):
    """Textura de la carcasa (64 × 64, 4 píxeles por unidad del modelo): frente (marco), espalda (con el módulo de cámara), costados,
    arriba/abajo y la cámara."""
    b, l, d = hexc(base), hexc(light), hexc(dark)
    K = 4                                              # píxeles por unidad del modelo
    img = Image.new("RGBA", (16 * K, 16 * K), (0, 0, 0, 0))
    p = img.load()
    W, H = 7 * K, 14 * K                               # frente y espalda: 7 × 14 unidades
    # frente: borde de color y cristal negro (la pantalla va encima)
    for y in range(H):
        for x in range(W):
            edge = x < 2 or x >= W - 2 or y < 2 or y >= H - 2
            p[x, y] = lerp(l, b, 0.3) if edge else (8, 8, 10, 255)
    for x in range(W // 2 - 3, W // 2 + 3):            # bocina de arriba
        p[x, 2] = (48, 48, 54, 255)
    # espalda: degradado, borde un poco más oscuro y un brillo suave en diagonal
    for y in range(H):
        for x in range(W):
            c = lerp(lerp(l, b, 0.35), lerp(b, d, 0.35), y / (H - 1))
            if x < 1 or x >= W - 1 or y < 1 or y >= H - 1:
                c = lerp(c, d, 0.4)
            band = abs((x - y * 0.35) - W * 0.25) / 3.0
            if band < 1:
                c = lerp(c, (255, 255, 255, 255), 0.13 * (1 - band))
            p[W + x, y] = c
    # el módulo de cámara (arriba a la izquierda visto de atrás): sombra donde va la cámara
    for y in range(2, 11):
        for x in range(2, 11):
            p[W + x, y] = lerp(p[W + x, y], (0, 0, 0, 255), 0.35)
    # logo: una estrellita de cuatro puntas (propia del celular)
    cx, cy = W + W // 2, H // 2
    glow = lerp(lerp(l, b, 0.2), (255, 255, 255, 255), 0.35)
    for k in range(-4, 5):
        w = max(0, 1 - abs(k) // 2)
        for j in range(-w, w + 1):
            p[cx + k, cy + j] = glow
            p[cx + j, cy + k] = glow
    p[cx, cy] = lerp(glow, (255, 255, 255, 255), 0.5)
    # costados (1.5 × 14 unidades) en x 14..15.5 y arriba/abajo (7 × 1.5) en y 14..15.5
    for y in range(H):
        for x in range(6):
            p[14 * K + x, y] = lerp(l, b, 0.15) if x in (2, 3) else lerp(b, d, 0.25)
    for y in range(6):
        for x in range(W):
            p[x, 14 * K + y] = lerp(l, b, 0.15) if y in (2, 3) else lerp(b, d, 0.25)
    # botones (un poco más claros) en 15..15.5 × 2..4
    for y in range(2 * K, 4 * K):
        for x in range(15 * K, 15 * K + 2):
            p[x, y] = lerp(l, b, 0.1)
    # la cámara (2 × 2 unidades = 8 × 8) en 14..16 × 14..16: vidrio oscuro, dos lentes y el flash
    ox, oy = 14 * K, 14 * K
    for y in range(8):
        for x in range(8):
            p[ox + x, oy + y] = (30, 30, 34, 255)
    for (lx, ly) in ((2, 2), (2, 5)):
        for y in range(-2, 2):
            for x in range(-2, 2):
                r2 = (x + 0.5) ** 2 + (y + 0.5) ** 2
                if r2 <= 3.2:
                    p[ox + lx + x, oy + ly + y] = (12, 12, 16, 255) if r2 > 1.2 else (60, 80, 140, 255)
    p[ox + 6, oy + 2] = (255, 240, 200, 255)
    p[ox + 6, oy + 5] = (90, 90, 96, 255)
    save(img, f"item/phone3d/{name}.png")


def phone3d():
    """Texturas, el modelo base (con su forma de sostenerse) y un modelo por carcasa × pantalla."""
    for n, fn in SCREEN_ART.items():
        save(fn(), f"item/phone3d/screen/{n}.png")
    for name, (b, l, dk) in CASES.items():
        case_tex(name, b, l, dk)
    models = os.path.join(ROOT, "models", "item")
    items = os.path.join(ROOT, "items")
    # quita lo del celular plano de antes
    import shutil
    shutil.rmtree(os.path.join(models, "screen"), ignore_errors=True)
    shutil.rmtree(os.path.join(TEX, "item", "screen"), ignore_errors=True)
    os.makedirs(os.path.join(models, "phone3d"), exist_ok=True)
    os.makedirs(items, exist_ok=True)

    def face(uv, tex="#case", rot=0):
        f = {"uv": uv, "texture": tex}
        if rot:
            f["rotation"] = rot
        return f

    side = [14, 0, 15.5, 14]
    cap = [0, 14, 7, 15.5]
    base = {
        "credit": "Celular de Axelis (generado por herramientas/assets.py)",
        "texture_size": [64, 64],
        "textures": {"particle": "#case", "case": "celular:item/phone3d/phone", "screen": "celular:item/phone3d/screen/lock"},
        "elements": [
            {"name": "cuerpo", "from": [4.5, 1, 7.25], "to": [11.5, 15, 8.75],
             "faces": {"south": face([0, 0, 7, 14]), "north": face([7, 0, 14, 14]), "east": face(side), "west": face(side),
                       "up": face(cap), "down": face(cap)}},
            {"name": "pantalla", "from": [5, 2, 8.76], "to": [11, 14, 8.76],
             "faces": {"south": {"uv": [0, 0, 8, 16], "texture": "#screen"}}},
            {"name": "camara", "from": [8.6, 11.6, 7.0], "to": [10.9, 13.9, 7.25],
             "faces": {"north": face([14, 14, 16, 16]), "east": face([14, 14, 14.5, 16]), "west": face([14, 14, 14.5, 16]),
                       "up": face([14, 14, 16, 14.5]), "down": face([14, 14, 16, 14.5])}},
            {"name": "boton", "from": [11.5, 9.6, 7.75], "to": [11.75, 11.6, 8.25],
             "faces": {"east": face([15, 2, 15.5, 4]), "north": face([15, 2, 15.5, 4]), "south": face([15, 2, 15.5, 4]),
                       "up": face([15, 2, 15.5, 2.5]), "down": face([15, 2, 15.5, 2.5])}},
            {"name": "volumen1", "from": [4.25, 10.6, 7.75], "to": [4.5, 11.8, 8.25],
             "faces": {"west": face([15, 2, 15.5, 3.2]), "north": face([15, 2, 15.5, 3.2]), "south": face([15, 2, 15.5, 3.2]),
                       "up": face([15, 2, 15.5, 2.5]), "down": face([15, 2, 15.5, 2.5])}},
            {"name": "volumen2", "from": [4.25, 8.8, 7.75], "to": [4.5, 10.0, 8.25],
             "faces": {"west": face([15, 2, 15.5, 3.2]), "north": face([15, 2, 15.5, 3.2]), "south": face([15, 2, 15.5, 3.2]),
                       "up": face([15, 2, 15.5, 2.5]), "down": face([15, 2, 15.5, 2.5])}},
        ],
        # cómo se sostiene (calculado con una simulación de la mano: la pantalla mira a los ojos al escribir, tomar foto y alumbrar)
        "display": {
            "thirdperson_righthand": {"rotation": [0, -50, 90], "translation": [-0.5, -1, -1.2], "scale": [0.45, 0.45, 0.45]},
            "firstperson_righthand": {"rotation": [-12, -15, 0], "translation": [-3.4, 5.2, 0.3], "scale": [0.42, 0.42, 0.42]},
            "gui": {"rotation": [8, -22, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
        },
    }
    with open(os.path.join(models, "phone3d", "base.json"), "w") as fh:
        json.dump(base, fh, indent=2)
    # en una llamada el celular va pegado a la oreja: otra forma de sostenerlo (también calculada con la simulación)
    ear = json.loads(json.dumps(base))
    ear["display"]["thirdperson_righthand"] = {"rotation": [-180, -110, -100], "translation": [-3.33, -0.94, 4.12], "scale": [0.45, 0.45, 0.45]}
    with open(os.path.join(models, "phone3d", "base_ear.json"), "w") as fh:
        json.dump(ear, fh, indent=2)
    for case in CASES:
        overrides, entries = [], []
        for i, n in enumerate(SCREENS, 1):
            m = f"celular:item/phone3d/{case}_{n}"
            with open(os.path.join(models, "phone3d", f"{case}_{n}.json"), "w") as fh:
                json.dump({"parent": "celular:item/phone3d/" + ("base_ear" if n == "ear" else "base"),
                           "textures": {"case": f"celular:item/phone3d/{case}", "screen": f"celular:item/phone3d/screen/{n}"}}, fh,
                          indent=2)
            overrides.append({"predicate": {"custom_model_data": i}, "model": m})
            entries.append({"threshold": i, "model": {"type": "minecraft:model", "model": m}})
        with open(os.path.join(models, f"{case}.json"), "w") as fh:
            json.dump({"parent": "celular:item/phone3d/base",
                       "textures": {"case": f"celular:item/phone3d/{case}", "screen": "celular:item/phone3d/screen/lock"},
                       "overrides": overrides}, fh, indent=2)
        with open(os.path.join(items, f"{case}.json"), "w") as fh:
            json.dump({"model": {"type": "minecraft:range_dispatch", "property": "minecraft:custom_model_data", "index": 0,
                                 "fallback": {"type": "minecraft:model", "model": f"celular:item/{case}"}, "entries": entries}}, fh,
                      indent=2)


def main():
    for name, (b, l, dk) in CASES.items():
        phone_item(name, b, l, dk)
    phone3d()
    apps = [icon_insta(), icon_camera(), icon_gallery(), icon_phone(), icon_patreon(), icon_settings(), icon_music(), icon_weather(),
            icon_compass(), icon_calc(), icon_flash(), icon_messages(), icon_notes(), icon_map(), icon_wallet()]
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
