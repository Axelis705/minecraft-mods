"""Catalogo de decoracion, parte 2 (muebles grandes, tapices, ciencia y magia). Misma convencion que props.py:
el FRENTE mira al NORTE (-z), las de pared tienen la espalda en z = 16, las colgantes cuelgan de y = 16."""
import math
import random

from PIL import Image, ImageDraw

from kit import *  # noqa: F401,F403
from props import (AMETHYST, BRASS, DWOOD, FIRE, GREEN_GLOW, IRON, OAK, PURPLE, SLATE, SOUL, WAX, book, chainmat, coins_mat, glassy)

BONE = Ramp('#6e6a5a', '#8d8776', '#b4ad98', '#d3ccb4', '#ebe5d0')


# ---------------------------------------------------------------- ayudas de dibujo
def octagon(p, plane, cx, cy, cz, R, th, mat):
    """Anillo octogonal fino en uno de los tres planos (xz horizontal, xy de frente, zy de lado), hecho con 8 cajas (4 rectas y 4 en diagonal)."""
    L = 2 * R * math.sin(math.radians(22.5))
    m = R * math.cos(math.radians(22.5))
    d = m * math.cos(math.radians(45))
    h = th / 2

    def seg(ux, uy, along, ang=None):
        # ux, uy: centro del segmento en el plano; along: 'u' o 'v'
        if plane == 'xz':
            c = (cx + ux, cy, cz + uy)
            dims = {'u': (L, th, th), 'v': (th, th, L)}[along]
            axis = 'y'
        elif plane == 'xy':
            c = (cx + ux, cy + uy, cz)
            dims = {'u': (L, th, th), 'v': (th, L, th)}[along]
            axis = 'z'
        else:
            c = (cx, cy + uy, cz + ux)
            dims = {'u': (th, th, L), 'v': (th, L, th)}[along]
            axis = 'x'
        a = (c[0] - dims[0] / 2, c[1] - dims[1] / 2, c[2] - dims[2] / 2)
        b = (c[0] + dims[0] / 2, c[1] + dims[1] / 2, c[2] + dims[2] / 2)
        kw = {'rot': (axis, ang, c)} if ang else {}
        p.box(*a, *b, mat, **kw)

    seg(0, m, 'u')
    seg(0, -m, 'u')
    seg(m, 0, 'v')
    seg(-m, 0, 'v')
    k = -1 if plane == 'xy' else 1   # el giro positivo alrededor de z va de x hacia y (en los otros planos va de u hacia -v)
    for sx, sy in ((1, 1), (-1, -1)):
        seg(sx * d, sy * d, 'u', 45 * k)
    for sx, sy in ((1, -1), (-1, 1)):
        seg(sx * d, sy * d, 'u', -45 * k)


def _noise_rows(w, h, rng):
    return [[rng.random() for _ in range(w)] for _ in range(h)]


def _canvas(w, h, top, bot, seed=1):
    rng = random.Random(seed)
    img = Image.new('RGBA', (w, h))
    px = img.load()
    for y in range(h):
        t = y / max(1, h - 1)
        for x in range(w):
            n = (rng.random() - 0.5) * 0.10
            c = tuple(max(0, min(255, int((top[i] * (1 - t) + bot[i] * t) * (1 + n)))) for i in range(3))
            px[x, y] = c + (255,)
    return img


def _border(img, gold, dark, thick=1):
    d = ImageDraw.Draw(img)
    w, h = img.size
    for k in range(thick):
        d.rectangle((k, k, w - 1 - k, h - 1 - k), outline=gold)
    d.rectangle((thick, thick, w - 1 - thick, h - 1 - thick), outline=dark)


def _hex(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def _star4(d, x, y, r, col):
    d.point((x, y), fill=col)
    for k in range(1, r + 1):
        for dx, dy in ((k, 0), (-k, 0), (0, k), (0, -k)):
            d.point((x + dx, y + dy), fill=col)


def _notch(img, depth=5):
    """Corte de golondrina en la parte baja del tapiz."""
    w, h = img.size
    px = img.load()
    for k in range(depth):
        for x in range(w):
            cx = (w - 1) / 2
            if abs(x - cx) <= (depth - k) * (cx / depth):
                px[x, h - 1 - k] = (0, 0, 0, 0)


def paint_estrellas():
    W, H = 14, 29
    img = _canvas(W, H, _hex('#0b1233'), _hex('#22336f'), 3)
    glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    gold, pale = _hex('#e8c85a'), _hex('#fff4c8')
    _border(img, gold, _hex('#6b5320'))
    d, g = ImageDraw.Draw(img), ImageDraw.Draw(glow)
    # luna creciente
    d.ellipse((3, 3, 10, 10), fill=pale + (255,))
    d.ellipse((5, 2, 12, 9), fill=_hex('#101a45') + (255,))
    g.ellipse((3, 3, 10, 10), fill=pale + (255,))
    g.ellipse((5, 2, 12, 9), fill=(0, 0, 0, 0))
    # estrellas
    for (x, y, r) in ((3, 14, 1), (10, 16, 1), (6, 19, 2), (11, 22, 1), (3, 24, 1)):
        _star4(d, x, y, r, pale + (255,))
        _star4(g, x, y, r, pale + (255,))
    for (x, y) in ((8, 4), (2, 8), (11, 12), (8, 26), (5, 13), (9, 21)):
        d.point((x, y), fill=gold + (255,))
        g.point((x, y), fill=gold + (255,))
    _notch(img, 4)
    _notch(glow, 4)
    return img, glow


def paint_arcano():
    W, H = 14, 29
    img = _canvas(W, H, _hex('#1a0932'), _hex('#35155f'), 5)
    glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ln, hi = _hex('#b78cff'), _hex('#f0dcff')
    _border(img, _hex('#c9a24a'), _hex('#5a4018'))
    d, g = ImageDraw.Draw(img), ImageDraw.Draw(glow)
    cx, cy, r = 6.5, 11, 5
    for dr in (d, g):
        dr.ellipse((cx - r, cy - r, cx + r, cy + r), outline=ln + (255,))
        # hexagrama
        pts1 = [(cx, cy - r + 1), (cx + 4, cy + 3), (cx - 4, cy + 3)]
        pts2 = [(cx, cy + r - 1), (cx + 4, cy - 3), (cx - 4, cy - 3)]
        dr.polygon(pts1, outline=hi + (255,))
        dr.polygon(pts2, outline=hi + (255,))
    # ojo
    ey = 22
    for dr in (d, g):
        dr.ellipse((2, ey - 2, 11, ey + 2), outline=ln + (255,))
        dr.rectangle((5, ey - 1, 8, ey + 1), fill=hi + (255,))
        dr.point((6, ey), fill=(30, 10, 60, 255))
        dr.point((7, ey), fill=(30, 10, 60, 255))
    # runas arriba y abajo
    for x in (2, 5, 8, 11):
        d.point((x, 2), fill=ln + (255,))
        g.point((x, 2), fill=ln + (255,))
    for x in (3, 6, 9):
        d.point((x, 26), fill=ln + (255,))
        g.point((x, 26), fill=ln + (255,))
    _notch(img, 4)
    _notch(glow, 4)
    return img, glow


def paint_real():
    W, H = 14, 29
    img = _canvas(W, H, _hex('#5c0f1b'), _hex('#7e1828'), 7)
    gold, lt = _hex('#e8c85a'), _hex('#fff0a0')
    _border(img, gold, _hex('#6b5320'), 2)
    d = ImageDraw.Draw(img)
    # corona
    d.polygon([(3, 12), (3, 7), (5, 9), (7, 5), (9, 9), (11, 7), (11, 12)], fill=gold + (255,))
    d.rectangle((3, 12, 11, 13), fill=_hex('#b8932f') + (255,))
    for x in (4, 7, 10):
        d.point((x, 11), fill=_hex('#e23a4a') + (255,))
    d.point((7, 7), fill=lt + (255,))
    # espada
    d.rectangle((6, 15, 7, 24), fill=_hex('#dfe6f0') + (255,))
    d.rectangle((4, 22, 9, 22), fill=gold + (255,))
    d.rectangle((6, 23, 7, 25), fill=_hex('#4a2a14') + (255,))
    d.point((6, 26), fill=gold + (255,))
    _notch(img, 4)
    return img, None


def paint_arbol():
    W, H = 14, 29
    img = _canvas(W, H, _hex('#0d2f2c'), _hex('#1b5a45'), 9)
    glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    gold, lf = _hex('#d9b248'), _hex('#7fd06a')
    _border(img, gold, _hex('#5b4a1c'))
    d, g = ImageDraw.Draw(img), ImageDraw.Draw(glow)
    rng = random.Random(4)
    d.ellipse((2, 3, 11, 14), fill=_hex('#2c7a3c') + (255,))
    for _ in range(26):
        x, y = rng.randint(2, 11), rng.randint(3, 14)
        if (x - 6.5) ** 2 / 20 + (y - 8.5) ** 2 / 30 <= 1:
            d.point((x, y), fill=lf + (255,) if rng.random() < 0.5 else _hex('#1e5a2c') + (255,))
    d.rectangle((6, 14, 7, 22), fill=_hex('#8a5a2a') + (255,))
    d.line((6, 18, 3, 15), fill=_hex('#8a5a2a') + (255,))
    d.line((7, 17, 10, 14), fill=_hex('#8a5a2a') + (255,))
    for (x, y) in ((5, 5), (9, 7), (4, 10), (8, 11), (10, 9)):
        d.point((x, y), fill=_hex('#ffd34a') + (255,))
        g.point((x, y), fill=_hex('#ffd34a') + (255,))
    # raices
    d.line((6, 22, 3, 25), fill=gold + (255,))
    d.line((7, 22, 10, 25), fill=gold + (255,))
    d.line((6, 22, 6, 26), fill=gold + (255,))
    _notch(img, 3)
    _notch(glow, 3)
    return img, glow


def paint_mapa():
    W, H = 14, 29
    rng = random.Random(12)
    img = _canvas(W, H, _hex('#d6bf8a'), _hex('#c4a86e'), 11)
    _border(img, _hex('#7a5a30'), _hex('#a88a52'))
    d = ImageDraw.Draw(img)
    px = img.load()
    # continentes: manchas con "ruido de valor" muy basto
    seeds = [(rng.uniform(2, 11), rng.uniform(3, 25), rng.uniform(2, 4.2)) for _ in range(7)]
    for y in range(2, H - 2):
        for x in range(2, W - 2):
            v = sum(max(0, 1 - ((x - sx) ** 2 + (y - sy) ** 2) / (r * r)) for sx, sy, r in seeds)
            if v > 0.35:
                px[x, y] = _hex('#7d8a4b') + (255,) if v > 0.6 else _hex('#9c9a5a') + (255,)
            elif v > 0.2:
                px[x, y] = _hex('#8a6a3a') + (255,)
    # ruta punteada y X roja
    for i, (x, y) in enumerate(((3, 6), (5, 9), (8, 11), (9, 14), (7, 17), (5, 20), (7, 23))):
        if i % 1 == 0:
            d.point((x, y), fill=_hex('#8a1c1c') + (255,))
    d.line((6, 22, 8, 24), fill=_hex('#b01c1c') + (255,))
    d.line((8, 22, 6, 24), fill=_hex('#b01c1c') + (255,))
    # rosa de los vientos
    d.point((11, 4), fill=(40, 30, 20, 255))
    d.line((11, 3, 11, 5), fill=(40, 30, 20, 255))
    d.line((10, 4, 12, 4), fill=(40, 30, 20, 255))
    _notch(img, 3)
    return img, None


def tapiz(pid, es, en, painter):
    """Tapiz de 2 bloques de alto, 1px de grosor pegado a la pared, se atraviesa. 'soft': no necesita muro detras (tapa pasadizos)."""
    img, glow = painter()
    p = Prop(pid, es, en, 'wall', light=0, collide=False, tall=True, soft=True, sound='wool',
             shape=[(1, 0, 14, 15, 16, 16)], shape_upper=[(1, 0, 14, 15, 16, 16)])
    rod = metal(BRASS, 0.5, 3, 0.05)
    dw = wood(DWOOD, 'h', 2.0, 0, 5)
    up_img = img.crop((0, 0, 14, 14))
    lo_img = img.crop((0, 14, 14, 29))
    p.at('upper')
    p.box(1, 0, 15, 15, 14, 16, image_mat(up_img))
    p.box(0, 14, 14.2, 16, 15.6, 15.8, dw, bevel=.1)
    p.box(-0.3, 14.2, 14.4, 1.0, 15.4, 15.6, rod)
    p.box(15.0, 14.2, 14.4, 16.3, 15.4, 15.6, rod)
    if glow is not None:
        p.box(1, 0, 14.8, 15, 14, 15, image_mat(glow.crop((0, 0, 14, 14))), emit=True, skip=['south', 'up', 'down', 'east', 'west'])
    p.at('lower')
    p.box(1, 1, 15, 15, 16, 16, image_mat(lo_img))
    if glow is not None:
        p.box(1, 1, 14.8, 15, 16, 15, image_mat(glow.crop((0, 14, 14, 29))), emit=True, skip=['south', 'up', 'down', 'east', 'west'])
    return p


# ---------------------------------------------------------------- tronos y muebles
def trono():
    p = Prop('trono', 'Trono real', 'Royal throne', 'floor', tall=True, sound='wood',
             shape=[(0, 0, 0, 16, 1.5, 16), (1, 1.5, 2, 15, 8, 15), (0.8, 8, 3, 3, 12, 15), (13, 8, 3, 15.2, 12, 15), (2, 8, 12, 14, 16, 15.5)],
             shape_upper=[(2, 0, 11, 14, 16, 15.5)])
    dw = wood(DWOOD, 'h', 2.0, 0, 5)
    br = metal(BRASS, 0.55, 3, 0.07)
    pur = cloth('#5a1f8a', 0.12, 5)
    sl = stone(SLATE)
    p.at('lower')
    p.box(0, 0, 0, 16, 1.5, 16, sl, bevel=.12, skip=['down'])
    p.box(1.5, 1.5, 2, 14.5, 7, 15, dw, bevel=.1)
    p.box(2.5, 7, 3.5, 13.5, 8.5, 13.5, pur)
    p.box(0.8, 7, 3, 3, 12, 14.5, dw, bevel=.1)
    p.box(13, 7, 3, 15.2, 12, 14.5, dw, bevel=.1)
    p.box(0.5, 11.8, 2.5, 3.3, 12.6, 15, br, bevel=.15)
    p.box(12.7, 11.8, 2.5, 15.5, 12.6, 15, br, bevel=.15)
    p.box(1.5, 6, 1.7, 14.5, 7, 2.1, br, bevel=.1)
    p.box(2, 8.5, 12, 14, 16, 15.5, dw, bevel=.1)
    p.box(3.5, 8.5, 11.6, 12.5, 16, 12, pur)
    p.at('upper')
    p.box(2, 0, 12, 14, 12, 15.5, dw, bevel=.1)
    p.box(3.5, 0, 11.6, 12.5, 10, 12, pur)
    p.box(1, 0, 11.8, 2.5, 14, 15.5, dw, bevel=.1)
    p.box(13.5, 0, 11.8, 15, 14, 15.5, dw, bevel=.1)
    p.box(0.8, 12, 11.4, 15.2, 13.2, 15.6, br, bevel=.15)
    p.box(5, 13.2, 12, 11, 16, 15, br, bevel=.15)
    p.box(7, 10.2, 11.4, 9, 12.2, 11.8, gem('#e0334a'))
    p.box(1, 14, 12.5, 2.8, 16, 14.5, br, bevel=.15)
    p.box(13.2, 14, 12.5, 15, 16, 14.5, br, bevel=.15)
    return p


def sillon():
    p = Prop('sillon', 'Sillón de lectura', 'Reading armchair', 'floor', shape=[(1, 0, 1, 15, 8, 15), (1, 8, 11, 15, 16, 15)])
    dw = wood(DWOOD, 'h', 2.0, 0, 5)
    red = cloth('#7a1c24', 0.15, 5)
    for x, z in ((1, 2), (13, 2), (1, 12), (13, 12)):
        p.box(x, 0, z, x + 2, 3, z + 2, dw, bevel=.1, skip=['down'])
    p.box(1, 3, 1.5, 15, 7, 15, red, bevel=.12)
    p.box(2.5, 7, 3, 13.5, 8.5, 12, cloth('#9a2a34', 0.15, 6))
    p.box(0.5, 4, 2, 2.8, 9.5, 14.5, red, bevel=.12)
    p.box(13.2, 4, 2, 15.5, 9.5, 14.5, red, bevel=.12)
    p.box(1, 7, 11, 15, 16, 15, red, bevel=.12)
    p.box(3, 8.5, 10.6, 13, 14.5, 11, cloth('#9a2a34', 0.15, 6))
    return p


def pila_libros():
    p = Prop('pila_libros', 'Pila de libros', 'Stack of books', 'floor', shape=[(2, 0, 2, 14, 10, 14)])

    def bk(x, y, z, w, d, h, color, ang=0):
        # Un libro = tapa de abajo + tapa de arriba + lomo + paginas, SIN cajas que se pisen (antes las caras coincidian y las texturas
        # se mezclaban): las paginas van entre las tapas, algo metidas hacia dentro, y asoman por tres lados.
        cov = cloth(color, 0.0, 5)
        pg = parchment(0.5)
        c = (x + w / 2, y + h / 2, z + d / 2)
        kw = {'rot': ('y', ang, c)} if ang else {}
        t = 0.8
        p.box(x, y, z, x + w, y + t, z + d, cov, **kw)
        p.box(x, y + h - t, z, x + w, y + h, z + d, cov, **kw)
        p.box(x, y + t, z, x + 1.0, y + h - t, z + d, cov, **kw)
        p.box(x + 1.0, y + t, z + 0.4, x + w - 0.3, y + h - t, z + d - 0.4, pg, **kw)

    # tres libros apilados de verdad (cada uno justo encima del anterior, sin solaparse) y girados poco
    bk(2, 0, 3, 12, 9, 3, '#7a1f1f')
    bk(2.5, 3, 3.5, 11, 8.5, 2.6, '#1f3f7a', 10)
    bk(3, 5.6, 4, 10, 8, 2.2, '#2d5a2a', -9)
    return p


def reloj():
    # Reloj de pie GRANDE: tres bloques de alto en total (los dos del bloque + la corona, que sobresale por arriba; el modelo puede
    # salirse del bloque hasta 16 px, y la colision sigue siendo la de dos bloques).
    p = Prop('reloj', 'Reloj de pie', 'Grandfather clock', 'floor', tall=True, sound='wood',
             shape=[(1, 0, 3, 15, 16, 16)], shape_upper=[(2, 0, 3.5, 14, 16, 15.5)])
    dw = wood(DWOOD, 'v', 2.0, 0, 5)
    br = metal(BRASS, 0.55, 3, 0.07)
    dial = Image.new('RGBA', (8, 8), _hex('#3f2a17') + (255,))
    d = ImageDraw.Draw(dial)
    d.ellipse((0, 0, 7, 7), fill=_hex('#efe6c8') + (255,))
    for (x, y) in ((3, 0), (4, 0), (7, 3), (7, 4), (3, 7), (4, 7), (0, 3), (0, 4)):
        dial.putpixel((x, y), _hex('#8a6a30') + (255,))
    for (x, y) in ((3, 3), (3, 2), (3, 1), (4, 4), (5, 4)):
        dial.putpixel((x, y), _hex('#2a1a0a') + (255,))
    iron = metal(IRON, 0.4, 5, 0.0)
    p.at('lower')
    p.box(1, 0, 3, 15, 3, 16, dw, bevel=.12, skip=['down'])
    p.box(2, 3, 3.5, 14, 5, 15.5, dw, bevel=.12)
    p.box(3, 5, 4.5, 13, 16, 15, dw, bevel=.1)
    p.box(4.5, 6, 4.1, 11.5, 16, 4.5, glassy('#cfe0e8'))
    p.box(4.5, 6, 4.5, 11.5, 16, 5, iron)
    p.box(7.6, 6, 5.1, 8.4, 16, 5.7, br)
    p.box(5.6, 6, 5.1, 10.4, 8.6, 5.7, br, bevel=.15)
    p.at('upper')
    p.box(3, 0, 4.5, 13, 4, 15, dw, bevel=.1)
    p.box(4.5, 0, 4.1, 11.5, 4, 4.5, glassy('#cfe0e8'))
    p.box(4.5, 0, 4.5, 11.5, 4, 5, iron)
    p.box(7.6, 0, 5.1, 8.4, 3, 5.7, br)
    p.box(2, 4, 3.5, 14, 15, 15.5, dw, bevel=.12)
    p.box(4, 5.5, 3.1, 12, 13.5, 3.5, image_mat(dial))
    p.box(3.4, 5, 3.0, 12.6, 5.5, 3.6, br)
    p.box(3.4, 13.5, 3.0, 12.6, 14, 3.6, br)
    p.box(3.4, 5, 3.0, 4, 14, 3.6, br)
    p.box(12, 5, 3.0, 12.6, 14, 3.6, br)
    # corona (sobresale por arriba del segundo bloque)
    p.box(1, 15, 3, 15, 17.5, 16, dw, bevel=.12)
    p.box(2.5, 17.5, 4, 13.5, 21, 15, dw, bevel=.12)
    p.box(4.5, 21, 5.5, 11.5, 23, 13.5, dw, bevel=.12)
    p.box(7, 23, 8, 9, 26, 10, br, bevel=.15)
    p.box(6.4, 25.5, 7.4, 9.6, 28.5, 10.6, br, bevel=.2)
    return p


def _mesa(pid, es, en, legs_n, legs_s):
    """Mesa de estudio de 14 px de alto (los sillones van al lado, no debajo). El eje largo es norte-sur; las patas van en el extremo
    norte (legs_n) y/o sur (legs_s); sin patas, la pieza es de enlace y se junta con la de al lado."""
    p = Prop(pid, es, en, 'floor', sound='wood', shape=[(0, 8, 0, 16, 14, 16)], render='solid')
    top = wood(OAK if False else DWOOD, 'h', 2.4, 0, 6)
    dark = wood(DWOOD, 'h', 1.6, 0, 8)
    p.box(0, 11.5, 0, 16, 14, 16, top, bevel=.1)
    p.box(1, 8.5, 0, 15, 11.5, 16, dark)
    p.box(5, 14, 0, 11, 14.25, 16, cloth('#7a1f2a', 0.12, 6), shade=False)
    legs = []
    if legs_n:
        legs += [(0.6, 0.6), (12.4, 0.6)]
    if legs_s:
        legs += [(0.6, 12.4), (12.4, 12.4)]
    for (x, z) in legs:
        p.box(x, 0, z, x + 3, 8.5, z + 3, dark, bevel=.15, skip=['down'])
        p.box(x - 0.4, 6.5, z - 0.4, x + 3.4, 8.5, z + 3.4, top, bevel=.1)
    return p


def mesa_extremo():
    return _mesa('mesa_extremo', 'Mesa de estudio (extremo)', 'Study table (end)', True, False)


def mesa_centro():
    return _mesa('mesa_centro', 'Mesa de estudio (centro)', 'Study table (middle)', False, False)


def mesa_sola():
    return _mesa('mesa_sola', 'Mesa de estudio (sola)', 'Study table (single)', True, True)


GOLDR = Ramp('#c08a14', '#e6b02a', '#f8d23e', '#ffe96a', '#fff8b8')
LAPISR = Ramp('#10215a', '#1a3590', '#2448b8', '#3a66d8', '#6a8cf0')


def oro_adorno():
    """Bloque entero que imita el oro pero NO suelta nada al romperlo (el jugador puede venir a picar)."""
    p = Prop('oro_adorno', 'Oro de adorno', 'Decorative gold', 'cube', sound='metal', shape=[(0, 0, 0, 16, 16, 16)], render='solid', drops=False)
    p.box(0, 0, 0, 16, 16, 16, metal(GOLDR, 0.22, 7, 0.22), bevel=.9)
    return p


def lapis_adorno():
    p = Prop('lapis_adorno', 'Lapislázuli de adorno', 'Decorative lapis', 'cube', sound='stone', shape=[(0, 0, 0, 16, 16, 16)], render='solid', drops=False)
    p.box(0, 0, 0, 16, 16, 16, stone(LAPISR, 1.6, 5, 0.0), bevel=.6)
    return p


def gargola():
    p = Prop('gargola', 'Gárgola', 'Gargoyle', 'floor', shape=[(3, 0, 3, 13, 14, 15)], sound='stone')
    st = stone(Ramp('#2c2c34', '#3a3a44', '#4a4a56', '#5e5e6c', '#767686'), 2.6, 7, 0.0)
    eye = glow(FIRE)
    p.box(3, 0, 3, 13, 2, 14, st, bevel=.15, skip=['down'])
    p.box(4, 2, 9, 7, 6, 14, st, bevel=.1)
    p.box(9, 2, 9, 12, 6, 14, st, bevel=.1)
    p.box(4, 5, 6, 12, 11, 14, st, bevel=.1)
    p.box(5, 4, 4, 11, 8, 7, st, bevel=.1)
    p.box(4.5, 2, 3.5, 6.5, 5, 6, st, bevel=.1)
    p.box(9.5, 2, 3.5, 11.5, 5, 6, st, bevel=.1)
    p.box(4.3, 2, 2.4, 6.7, 2.8, 3.5, st)
    p.box(9.3, 2, 2.4, 11.7, 2.8, 3.5, st)
    p.box(5.5, 8.5, 3.2, 10.5, 13.5, 8, st, bevel=.12)
    p.box(6.5, 9, 1.8, 9.5, 11.5, 3.2, st, bevel=.1)
    p.box(6.8, 8.6, 1.9, 9.2, 9.2, 3.0, stone(Ramp('#16161c', '#22222a')))
    p.box(5.9, 11.6, 2.95, 7.2, 12.6, 3.25, eye)
    p.box(8.8, 11.6, 2.95, 10.1, 12.6, 3.25, eye)
    p.box(4.8, 13, 4.5, 6.2, 16, 5.9, st, rot=('z', 22.5, (5.5, 13, 5)))
    p.box(9.8, 13, 4.5, 11.2, 16, 5.9, st, rot=('z', -22.5, (10.5, 13, 5)))
    p.box(4, 11.5, 5.5, 5.6, 14, 7, st)
    p.box(10.4, 11.5, 5.5, 12, 14, 7, st)
    p.box(1.2, 6, 9.5, 4.5, 14.5, 11.2, st, rot=('z', 22.5, (3, 8, 10)), bevel=.1)
    p.box(11.5, 6, 9.5, 14.8, 14.5, 11.2, st, rot=('z', -22.5, (13, 8, 10)), bevel=.1)
    p.box(0.8, 12, 9.8, 3, 15.5, 10.9, st, rot=('z', 22.5, (3, 8, 10)))
    p.box(13, 12, 9.8, 15.2, 15.5, 10.9, st, rot=('z', -22.5, (13, 8, 10)))
    p.box(7, 2, 14, 9, 4, 16, st)
    return p


def cristal_mana():
    p = Prop('cristal_mana', 'Cristal de maná', 'Mana crystal', 'floor', light=14, shape=[(3, 0, 3, 13, 15, 13)], sound='amethyst',
             particles=[{'type': 'end_rod', 'pos': [8, 11, 8], 'rate': 0.12}, {'type': 'witch', 'pos': [8, 10, 8], 'rate': 0.15}])
    sl = stone(SLATE)
    cr = glow(AMETHYST, 1.6, 17)
    p.box(3, 0, 3, 13, 2.5, 13, sl, bevel=.15, skip=['down'])
    p.box(4.5, 2.5, 4.5, 11.5, 4, 11.5, stone(Ramp('#202028', '#2c2c36')), bevel=.12)
    p.box(6.3, 4, 6.3, 9.7, 6, 9.7, cr)
    p.box(5.2, 6, 5.2, 10.8, 9, 10.8, cr)
    p.box(4.6, 9, 4.6, 11.4, 11, 11.4, cr)
    p.box(5.4, 11, 5.4, 10.6, 13, 10.6, cr)
    p.box(6.4, 13, 6.4, 9.6, 14.6, 9.6, cr)
    p.box(7.2, 14.6, 7.2, 8.8, 15.6, 8.8, cr)
    p.box(3.4, 4, 4.2, 5.4, 9.5, 6.2, cr, rot=('z', 22.5, (4.4, 4, 5.2)))
    p.box(10.6, 4, 9.8, 12.6, 8.5, 11.8, cr, rot=('z', -22.5, (11.6, 4, 10.8)))
    p.box(6.8, 4, 10.6, 8.8, 7.5, 12.4, cr, rot=('x', -22.5, (7.8, 4, 11.5)))
    return p


def candelero_pared():
    p = Prop('candelero_pared', 'Candelero de pared', 'Wall sconce', 'wall', light=13, shape=[(5.5, 4, 10, 10.5, 14, 16)], sound='metal',
             particles=[{'type': 'flame', 'pos': [8, 12.4, 11.5], 'rate': 0.3}, {'type': 'small_flame', 'pos': [8, 12.2, 11.5], 'rate': 0.2}])
    br = metal(BRASS, 0.55, 3, 0.07)
    p.box(5.5, 4, 15, 10.5, 14, 16, br, bevel=.15)
    p.box(6.5, 11.5, 14.4, 9.5, 14.5, 15, br, bevel=.15)
    p.box(7.25, 6.5, 10.5, 8.75, 7.8, 15, br, bevel=.12)
    p.box(6.3, 7.8, 9.8, 9.7, 8.8, 13.2, br, bevel=.15)
    p.box(7.2, 8.8, 10.7, 8.8, 11.8, 12.3, cloth('#efe7cb', 0.0, 5))
    fl = flame()
    p.box(6.6, 11.8, 11.5, 9.4, 14.2, 11.5, fl)
    p.box(8, 11.8, 10.3, 8, 14.2, 12.7, fl)
    return p


def monton_oro():
    p = Prop('monton_oro', 'Montón de oro', 'Gold hoard', 'floor', light=4, shape=[(1, 0, 1, 15, 2, 15), (2, 2, 2, 14, 4, 14), (4, 4, 4, 12, 6, 12)], sound='metal')
    co = coins_mat()
    p.box(1, 0, 1, 15, 2, 15, co, skip=['down'])
    p.box(2, 2, 2, 14, 4, 14, co)
    p.box(4, 4, 4, 12, 6, 12, co)
    p.box(6, 6, 5.5, 10, 7.5, 10.5, co)
    p.box(10.5, 2, 10.5, 13, 4.2, 13, gem('#2ecc71'), rot=('y', 22.5, (11.75, 3, 11.75)))
    p.box(3, 2, 10, 5.2, 4.2, 12.2, gem('#d12a3c'), rot=('y', -22.5, (4.1, 3, 11.1)))
    p.box(10, 4, 3.5, 12, 6, 5.5, gem('#46a0ff'), rot=('y', 22.5, (11, 5, 4.5)))
    br = metal(BRASS, 0.5, 3, 0.1)
    p.box(2.5, 2, 2.5, 5, 3, 5, br, bevel=.15)
    p.box(3.4, 3, 3.4, 4.1, 5, 4.1, br)
    p.box(2.6, 5, 2.6, 4.9, 7.2, 4.9, br, bevel=.15)
    return p


def jaula():
    p = Prop('jaula', 'Jaula colgante', 'Hanging cage', 'hang', collide=False, sound='chain', shape=[(3.5, 2, 3.5, 12.5, 16, 12.5)])
    ir = metal(IRON, 0.5, 5, 0.04)
    p.box(7.5, 13, 7.5, 8.5, 16, 8.5, chainmat())
    p.box(7, 12.6, 7, 9, 13.6, 9, ir, bevel=.15)
    p.box(4.5, 11.8, 4.5, 11.5, 12.6, 11.5, ir, bevel=.15)
    p.box(5.5, 12.6, 5.5, 10.5, 13, 10.5, ir)
    p.box(4, 2, 4, 12, 3, 12, ir, bevel=.15)
    for i in (4, 6.4, 8.8, 11.2):
        for (x, z) in ((i, 4), (i, 11.2)):
            p.box(x, 3, z, x + 0.8, 11.8, z + 0.8, ir)
    for i in (6.4, 8.8):
        for (x, z) in ((4, i), (11.2, i)):
            p.box(x, 3, z, x + 0.8, 11.8, z + 0.8, ir)
    p.box(4, 6, 4, 12, 6.7, 4.8, ir)
    p.box(4, 6, 11.2, 12, 6.7, 12, ir)
    p.box(4, 6, 4, 4.8, 6.7, 12, ir)
    p.box(11.2, 6, 4, 12, 6.7, 12, ir)
    bn = stone(BONE, 2.0, 5)
    p.box(6.4, 3, 6, 9.6, 5.8, 9.2, bn, bevel=.1)
    p.box(6.9, 3.8, 5.8, 7.7, 4.6, 6.1, stone(Ramp('#0e0e12', '#1a1a20')))
    p.box(8.3, 3.8, 5.8, 9.1, 4.6, 6.1, stone(Ramp('#0e0e12', '#1a1a20')))
    p.box(5, 3, 9.6, 11, 3.8, 11, bn)
    return p


def grilletes():
    p = Prop('grilletes', 'Grilletes de pared', 'Wall shackles', 'wall', collide=False, sound='chain', shape=[(2, 3, 13.5, 14, 15, 16)])
    ir = metal(Ramp('#3a3a48', '#52526a', '#6e6e88', '#9090aa', '#b4b4cc'), 0.5, 5, 0.08)
    for x in (4.5, 11.5):
        p.box(x - 1.2, 13, 14.6, x + 1.2, 15, 16, ir, bevel=.15)
        p.box(x - 0.5, 8.5, 14.6, x + 0.5, 13, 15.4, chainmat())
        p.box(x - 1.7, 5, 14, x + 1.7, 8.6, 15.7, ir, bevel=.15)
        p.box(x - 0.9, 5.4, 13.8, x + 0.9, 8.2, 14.0, stone(Ramp('#0a0a0e', '#14141a')))
        p.box(x - 1.5, 3.2, 14.2, x + 1.5, 5, 15.5, stone(BONE, 2.0, 5))
    return p


def caldero_magico():
    p = Prop('caldero_magico', 'Caldero mágico', 'Magic cauldron', 'floor', light=11, shape=[(2, 0, 2, 14, 11.5, 14)], sound='metal',
             particles=[{'type': 'happy_villager', 'pos': [8, 11, 8], 'rate': 0.15}, {'type': 'witch', 'pos': [8, 12, 8], 'rate': 0.1},
                        {'type': 'flame', 'pos': [8, 1.5, 8], 'rate': 0.25}])
    ir = metal(IRON, 0.5, 5, 0.06)
    for x, z in ((3, 3), (11, 3), (3, 11), (11, 11)):
        p.box(x, 0, z, x + 2, 3, z + 2, ir, bevel=.1, skip=['down'])
    p.box(5, 0.4, 5, 11, 1.4, 11, glow(FIRE))
    p.box(2.5, 3, 2.5, 13.5, 4.5, 13.5, ir, bevel=.1)
    p.box(2, 4.5, 2, 14, 10.5, 3.6, ir, bevel=.1)
    p.box(2, 4.5, 12.4, 14, 10.5, 14, ir, bevel=.1)
    p.box(2, 4.5, 3.6, 3.6, 10.5, 12.4, ir, bevel=.1)
    p.box(12.4, 4.5, 3.6, 14, 10.5, 12.4, ir, bevel=.1)
    p.box(1.4, 10.5, 1.4, 14.6, 11.5, 2.6, ir, bevel=.15)
    p.box(1.4, 10.5, 13.4, 14.6, 11.5, 14.6, ir, bevel=.15)
    p.box(1.4, 10.5, 2.6, 2.6, 11.5, 13.4, ir, bevel=.15)
    p.box(13.4, 10.5, 2.6, 14.6, 11.5, 13.4, ir, bevel=.15)
    p.box(3.6, 4.5, 3.6, 12.4, 9.8, 12.4, glow(GREEN_GLOW, 2.0, 21))
    fl = flame('#d9ffd0', '#7ae88c', '#32c25a', '#0e5a2a')
    p.box(6.8, 9.8, 8, 9.2, 12, 8, fl)
    p.box(8, 9.8, 6.8, 8, 12, 9.2, fl)
    return p


def esfera_armilar():
    p = Prop('esfera_armilar', 'Esfera armilar', 'Armillary sphere', 'floor', light=9, shape=[(3, 0, 3, 13, 15, 13)], sound='metal',
             particles=[{'type': 'end_rod', 'pos': [8, 9.5, 8], 'rate': 0.08}])
    br = metal(BRASS, 0.55, 3, 0.07)
    dw = wood(DWOOD, 'h', 2.0, 0, 5)
    p.box(3.5, 0, 3.5, 12.5, 1.5, 12.5, dw, bevel=.15, skip=['down'])
    p.box(7, 1.5, 7, 9, 4, 9, br, bevel=.12)
    p.box(5.5, 3.2, 5.5, 10.5, 4.2, 10.5, br, bevel=.15)
    cx, cy, cz = 8, 9.5, 8
    octagon(p, 'xz', cx, cy, cz, 5.6, 0.9, br)
    octagon(p, 'xy', cx, cy, cz, 4.8, 0.9, br)
    octagon(p, 'zy', cx, cy, cz, 4.0, 0.9, br)
    p.box(7.7, 4.2, 7.7, 8.3, 5, 8.3, br)
    p.box(7.6, 14.2, 7.6, 8.4, 15.6, 8.4, br)
    p.box(6.5, 8, 6.5, 9.5, 11, 9.5, glow(FIRE, 2.0, 25))
    p.box(12.7, 9, 7.4, 14, 10.3, 8.6, gem('#46a0ff'))
    p.box(2, 8.8, 7.5, 3.2, 10, 8.7, gem('#d9d4c8'))
    p.box(7.3, 13.4, 3.4, 8.7, 14.8, 4.6, gem('#d96a3a'))
    return p


def _books_row(w, h, seed):
    rng = random.Random(seed)
    img = Image.new('RGBA', (w, h), _hex('#150d08') + (255,))
    d = ImageDraw.Draw(img)
    pal = ['#7a1f1f', '#1f3f7a', '#2d5a2a', '#5a2a7a', '#8a5a1a', '#1f5a5a', '#6a1a3a', '#3a3a3a', '#a07a30', '#2a2a6a']
    x = 0
    while x < w:
        bw = rng.choice((1, 2, 2, 3))
        bh = rng.randint(h - 3, h - 1) if rng.random() < 0.9 else h - 5
        col = _hex(rng.choice(pal))
        for xx in range(x, min(w, x + bw)):
            for yy in range(h - bh, h):
                shade = 0.82 if xx == x else (1.12 if xx == x + bw - 1 and bw > 1 else 1.0)
                d.point((xx, yy), fill=tuple(min(255, int(c * shade)) for c in col) + (255,))
            d.point((xx, h - bh), fill=tuple(min(255, int(c * 1.3 + 20)) for c in col) + (255,))
        if rng.random() < 0.55 and bh >= 4:
            yy = h - bh + 1 + rng.randint(0, 1)
            for xx in range(x, min(w, x + bw)):
                d.point((xx, yy), fill=_hex('#d9b248') + (255,))
        x += bw
        if rng.random() < 0.12:
            x += 1
    return img


def estanteria():
    p = Prop('estanteria', 'Estantería alta', 'Tall bookshelf', 'floor', tall=True, sound='wood',
             shape=[(0, 0, 8, 16, 16, 16)], shape_upper=[(0, 0, 8, 16, 16, 16)])
    dw = wood(DWOOD, 'v', 2.0, 0, 5)
    ow = wood(OAK, 'h', 2.2, 0, 7)
    rows = {('lower', 0): (1, 7), ('lower', 1): (8, 16), ('upper', 0): (1, 7), ('upper', 1): (8, 15)}
    for part in ('lower', 'upper'):
        p.at(part)
        p.box(0, 0, 8, 1.5, 16, 16, dw, bevel=.1)
        p.box(14.5, 0, 8, 16, 16, 16, dw, bevel=.1)
        p.box(1.5, 0, 15, 14.5, 16, 16, dw)
        p.box(1.5, 0, 8, 14.5, 1, 15, ow, bevel=.1)
        p.box(1.5, 7, 8, 14.5, 8, 15, ow, bevel=.1)
        if part == 'upper':
            p.box(0, 15, 8, 16, 16, 16, ow, bevel=.12)
        for k in range(2):
            y0, y1 = rows[(part, k)]
            img = _books_row(13, y1 - y0, (10 if part == 'lower' else 20) + k)
            p.box(1.5, y0, 9.5, 14.5, y1, 15, image_mat(img))
    return p


def estatua_caballero():
    p = Prop('estatua_caballero', 'Armadura de caballero', 'Knight armor statue', 'floor', tall=True, sound='metal',
             shape=[(3, 0, 3, 13, 16, 13)], shape_upper=[(2, 0, 3, 14, 16, 14)])
    ir = metal(Ramp('#2a2c36', '#3d4050', '#565a6e', '#7a7f96', '#a4a9c0'), 0.5, 4, 0.1)
    sl = stone(SLATE)
    br = metal(BRASS, 0.55, 3, 0.07)
    lt = cloth('#5a3a1a', 0.1, 5)
    p.at('lower')
    p.box(3, 0, 3, 13, 2, 13, sl, bevel=.15, skip=['down'])
    for x in (5, 8.6):
        p.box(x, 2, 5, x + 2.4, 4, 10.5, ir, bevel=.15)
        p.box(x + 0.1, 4, 5.5, x + 2.3, 10, 9.5, ir, bevel=.12)
        p.box(x - 0.2, 7.5, 5.2, x + 2.6, 9, 9.8, ir, bevel=.15)
        p.box(x - 0.1, 10, 5.5, x + 2.5, 16, 10, ir, bevel=.12)
    p.box(7.4, 2, 3.4, 8.6, 16, 4.4, metal(Ramp('#8a90a4', '#b8bed0', '#dfe6f5', '#f5f8ff'), 0.4, 6, 0.2), bevel=.1)
    p.at('upper')
    p.box(7.4, 0, 3.4, 8.6, 8, 4.4, metal(Ramp('#8a90a4', '#b8bed0', '#dfe6f5', '#f5f8ff'), 0.4, 6, 0.2), bevel=.1)
    p.box(5.2, 8, 3.1, 10.8, 9.2, 4.7, br, bevel=.15)
    p.box(7.3, 9.2, 3.5, 8.7, 12.5, 4.3, lt)
    p.box(7, 12.5, 3.3, 9, 13.5, 4.5, br, bevel=.15)
    p.box(5, 0, 5, 11, 3, 10.5, ir, bevel=.12)
    p.box(4, 3, 4.6, 12, 10, 10.6, ir, bevel=.12)
    p.box(6.5, 4, 4.3, 9.5, 8, 4.7, br, bevel=.12)
    p.box(2, 8, 4.6, 4.6, 11, 10.4, ir, bevel=.15)
    p.box(11.4, 8, 4.6, 14, 11, 10.4, ir, bevel=.15)
    p.box(2.6, 3.5, 5.2, 4.4, 8.2, 8.6, ir, bevel=.12)
    p.box(11.6, 3.5, 5.2, 13.4, 8.2, 8.6, ir, bevel=.12)
    p.box(6.5, 10, 6, 9.5, 11.2, 9, ir)
    p.box(5.4, 11, 4.6, 10.6, 16, 10.6, ir, bevel=.15)
    p.box(5.8, 13, 4.3, 10.2, 14.1, 4.7, stone(Ramp('#050508', '#0c0c10')))
    p.box(7.6, 11.2, 4.3, 8.4, 13, 4.7, stone(Ramp('#050508', '#0c0c10')))
    p.box(7.3, 12.5, 10.6, 8.7, 16, 14.5, cloth('#9a1a28', 0.1, 6, 0.04))
    return p


def pedestal_runico():
    p = Prop('pedestal_runico', 'Pedestal rúnico', 'Runic pedestal', 'floor', light=12, shape=[(4, 0, 4, 12, 11, 12)], sound='stone',
             particles=[{'type': 'enchant', 'pos': [8, 12, 8], 'rate': 0.5}, {'type': 'end_rod', 'pos': [8, 14, 8], 'rate': 0.06}])
    sl = stone(SLATE)
    p.box(3.5, 0, 3.5, 12.5, 2, 12.5, sl, bevel=.15, skip=['down'])
    p.box(5, 2, 5, 11, 9, 11, stone(Ramp('#2a2a34', '#383844', '#474756'), 2.6, 9), bevel=.12)
    p.box(4.7, 4.5, 4.7, 11.3, 6, 11.3, glow(SOUL, 2.2, 31))
    p.box(3.5, 9, 3.5, 12.5, 10.5, 12.5, sl, bevel=.15)
    p.box(4.5, 10.5, 4.5, 11.5, 11, 11.5, glow(SOUL, 2.2, 33))
    p.box(6.6, 12, 6.6, 9.4, 14.8, 9.4, glow(SOUL, 1.8, 35))
    p.box(7.3, 11.5, 7.3, 8.7, 12, 8.7, glow(SOUL, 1.8, 37))
    p.box(7.3, 14.8, 7.3, 8.7, 15.6, 8.7, glow(SOUL, 1.8, 39))
    return p


def all_b():
    return [
        tapiz('tapiz_estrellas', 'Tapiz de estrellas', 'Starry tapestry', paint_estrellas),
        tapiz('tapiz_arcano', 'Tapiz arcano', 'Arcane tapestry', paint_arcano),
        tapiz('tapiz_real', 'Tapiz real', 'Royal tapestry', paint_real),
        tapiz('tapiz_arbol', 'Tapiz del árbol', 'Tree tapestry', paint_arbol),
        tapiz('tapiz_mapa', 'Tapiz de mapa', 'Map tapestry', paint_mapa),
        trono(), sillon(), pila_libros(), reloj(), gargola(), cristal_mana(), candelero_pared(), monton_oro(), jaula(), grilletes(),
        caldero_magico(), esfera_armilar(), estanteria(), estatua_caballero(), pedestal_runico(),
        mesa_extremo(), mesa_centro(), mesa_sola(), oro_adorno(), lapis_adorno(),
    ]
