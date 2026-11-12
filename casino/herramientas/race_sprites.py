#!/usr/bin/env python3
"""Sprites de las carreras de caballos (THE DIAMOND): caballo con jinete galopando, 8 cuadros, de lado con un poco de perspectiva.

Se dibuja con cajas 3D (estilo Minecraft) y se separa en capas para teñirlas en el juego:
  fila 0      base (fijo: arreos, número, botas, cara del jinete, cascos, contorno)
  fila 1      pelaje (gris: se tiñe con el color del caballo; crin y cola más oscuras)
  filas 2-5   seda principal del jinete para los 4 dibujos (liso, mangas, aros, banda)
  filas 6-9   seda secundaria para los 4 dibujos
Cada cuadro mide 64x44. Sale casino/textures/entity/race_horse.png (512x440) y una vista previa.

    python3 casino/herramientas/race_sprites.py [vista_previa.png]
"""
import math, os, random, sys
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'casino', 'textures', 'entity')
FW, FH, FRAMES = 64, 44, 8
S = 1.0          # 1 unidad = 1 píxel
OX, OY = 24, 4   # origen en el cuadro (pies del caballo)

# materiales
BASE_COL = {'pants': (238, 236, 230), 'tack': (236, 232, 222), 'tack2': (60, 40, 26), 'boot': (34, 30, 30), 'skin': (226, 178, 142), 'hoof': (40, 34, 30),
            'eye': (16, 12, 12), 'num': (20, 20, 24), 'visor': (24, 24, 28)}
COAT_MATS = {'coat': 0.86, 'mane': 0.42, 'muzzle': 0.62}
SILK = ('silkA', 'silkB')


def rot2(x, y, ang, px, py):
    c, s = math.cos(ang), math.sin(ang)
    x, y = x - px, y - py
    return px + x * c - y * s, py + x * s + y * c


class Box:
    def __init__(self, x0, y0, z0, x1, y1, z1, mat, rots=()):
        self.c = (x0, y0, z0, x1, y1, z1)
        self.mat = mat
        self.rots = rots   # lista de (ángulo, px, py): giros en el plano x-y (alrededor del eje z)

    def corners(self):
        x0, y0, z0, x1, y1, z1 = self.c
        out = []
        for (x, y, z) in ((x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)):
            for ang, px, py in self.rots:
                x, y = rot2(x, y, ang, px, py)
            out.append((x, y, z))
        return out


FACES = [((4, 5, 6, 7), 1.00, 'side'),    # z+ (hacia la cámara)
         ((3, 2, 6, 7), 1.18, 'top'),     # y+
         ((1, 2, 6, 5), 0.86, 'front'),   # x+
         ((0, 3, 7, 4), 0.74, 'back'),    # x-
         ((0, 1, 2, 3), 0.62, 'far')]     # z- (casi no se ve)


def project(p):
    x, y, z = p
    return OX + x + z * 0.32, FH - (OY + y + z * 0.22)


def fill_poly(pts, cb):
    ys = [p[1] for p in pts]
    for py in range(max(0, int(math.floor(min(ys)))), min(FH, int(math.ceil(max(ys))) + 1)):
        yc = py + 0.5
        xs = []
        n = len(pts)
        for i in range(n):
            (x1, y1), (x2, y2) = pts[i], pts[(i + 1) % n]
            if (y1 <= yc < y2) or (y2 <= yc < y1):
                xs.append(x1 + (yc - y1) * (x2 - x1) / (y2 - y1))
        xs.sort()
        for k in range(0, len(xs) - 1, 2):
            for px in range(max(0, int(math.ceil(xs[k] - 0.5))), min(FW, int(math.floor(xs[k + 1] - 0.5)) + 1)):
                cb(px, py)


def horse(frame, pattern):
    t = frame / FRAMES
    bob = 1.2 * math.sin(4 * math.pi * t)
    boxes = []
    by = 13 + bob
    # piernas: galope (pares desfasados)
    legs = [(-9.5, -2.6, 0.00, 'far'), (-8.0, 2.6, 0.12, 'near'), (7.0, -2.6, 0.50, 'far'), (8.5, 2.6, 0.62, 'near')]
    for lx, lz, off, _ in legs:
        ph = 2 * math.pi * (t + off)
        hip = 0.75 * math.sin(ph)
        knee = max(0.0, 0.9 * math.sin(ph + 1.2)) * (1 if lx < 0 else -1)
        top = (hip, lx + 1.5, by + 1)
        boxes.append(Box(lx, by - 6, lz - 1.5, lx + 3, by + 1, lz + 1.5, 'coat', [top]))
        boxes.append(Box(lx + 0.3, by - 11.5, lz - 1.2, lx + 2.7, by - 6, lz + 1.2, 'coat', [(knee, lx + 1.5, by - 6), top]))
        boxes.append(Box(lx + 0.1, by - 12.6, lz - 1.3, lx + 2.9, by - 11.5, lz + 1.3, 'hoof', [(knee, lx + 1.5, by - 6), top]))
    # cuerpo
    boxes.append(Box(-11, by, -5, 11, by + 10, 5, 'coat'))
    # mantilla con número y montura
    boxes.append(Box(-4, by + 4, -5.3, 3, by + 10.2, 5.3, 'tack'))
    boxes.append(Box(-3.5, by + 10, -3.2, 2.5, by + 10.8, 3.2, 'tack2'))
    # cuello (sube hacia adelante), cabeza (apunta adelante y un poco abajo), crin y orejas
    nk = 0.78 + 0.07 * math.sin(4 * math.pi * t + 1)
    piv = (nk, 8, by + 7)
    boxes.append(Box(7, by + 3.5, -2.4, 16, by + 9.5, 2.4, 'coat', [piv]))
    boxes.append(Box(7, by + 9.5, -0.8, 15.5, by + 11, 0.8, 'mane', [piv]))
    hp = (-1.2, 15, by + 6.5)
    rots = [hp, piv]
    boxes.append(Box(14, by + 3.5, -2.7, 21, by + 9.5, 2.7, 'coat', rots))
    boxes.append(Box(20.5, by + 3.8, -2.0, 24.5, by + 8.3, 2.0, 'muzzle', rots))
    boxes.append(Box(14.2, by + 9.5, 1.0, 15.6, by + 11.5, 2.2, 'coat', rots))
    boxes.append(Box(14.2, by + 9.5, -2.2, 15.6, by + 11.5, -1.0, 'coat', rots))
    boxes.append(Box(16.8, by + 7.2, 2.7, 18.0, by + 8.4, 2.8, 'eye', rots))
    boxes.append(Box(18.5, by + 4.0, -2.8, 19.3, by + 9.6, 2.8, 'tack2', rots))   # brida
    # cola
    tl = 0.45 + 0.25 * math.sin(2 * math.pi * t * 2)
    boxes.append(Box(-17, by + 4.5, -1.2, -11, by + 9, 1.2, 'mane', [(tl, -11, by + 8)]))
    # jinete agachado hacia adelante (torso inclinado), gorra, brazos estirados a las riendas
    jy = by + 10.5
    jl = -1.05 + 0.06 * math.sin(4 * math.pi * t)
    jp = (jl, 0, jy)
    boxes.append(Box(-1.8, jy - 0.6, 4.6, 3.6, jy + 1.6, 6.4, 'pants'))                   # muslo
    boxes.append(Box(2.0, jy - 4.6, 4.7, 3.8, jy - 0.4, 6.3, 'boot'))                     # bota
    boxes.append(Box(-2.2, jy, -2.6, 2.4, jy + 7.5, 2.6, 'torso', [jp]))                    # torso
    boxes.append(Box(-1.6, jy + 7.5, -2.3, 2.6, jy + 11.2, 2.3, 'skin', [jp]))              # cabeza
    boxes.append(Box(-2.0, jy + 9.6, -2.6, 2.9, jy + 12.0, 2.6, 'cap', [jp]))               # gorra
    boxes.append(Box(2.9, jy + 9.6, -2.0, 4.6, jy + 10.3, 2.0, 'visor', [jp]))
    ap = (0.75, 1.5, jy + 6.5)
    boxes.append(Box(0.5, jy + 1.5, 2.6, 2.6, jy + 6.8, 4.4, 'sleeve', [ap, jp]))           # brazo cercano
    boxes.append(Box(0.5, jy + 0.6, 2.8, 2.5, jy + 1.6, 4.2, 'skin', [ap, jp]))             # mano
    return boxes


def silk_role(mat, pattern, lx, ly):
    """Qué seda lleva cada parte según el dibujo: 0 liso, 1 mangas, 2 aros, 3 banda diagonal."""
    if mat == 'cap':
        return 'silkB'
    if mat == 'sleeve':
        return 'silkB' if pattern == 1 else ('silkB' if pattern == 2 and int(lx) % 3 == 0 else 'silkA')
    if mat == 'torso':
        if pattern == 2 and int(ly) % 3 == 0:
            return 'silkB'
        if pattern == 3 and abs((lx - ly * 1.0) % 7 - 3.5) < 1.2:
            return 'silkB'
        return 'silkA'
    return None


def render(frame, pattern):
    rnd = random.Random(frame * 31 + pattern)
    layers = {k: Image.new('RGBA', (FW, FH), (0, 0, 0, 0)) for k in ('base', 'coat', 'silkA', 'silkB')}
    depth = [[-1e9] * FW for _ in range(FH)]
    owner = [[None] * FW for _ in range(FH)]
    faces = []
    for b in horse(frame, pattern):
        cs = b.corners()
        for idx, shade, name in FACES:
            pts3 = [cs[i] for i in idx]
            # profundidad: hacia la cámara (z) y hacia arriba (para que los techos ganen a los lados de atrás)
            d = sum(p[2] for p in pts3) / 4 + (0.3 if name == 'top' else 0) + (0.2 if name == 'side' else 0)
            faces.append((d, [project(p) for p in pts3], b, shade, name, pts3))
    faces.sort(key=lambda f: f[0])
    for d, pts, b, shade, name, pts3 in faces:
        cx = sum(p[0] for p in pts3) / 4
        cy = sum(p[1] for p in pts3) / 4

        def put(px, py, b=b, shade=shade, cx=cx, cy=cy):
            owner[py][px] = (b, shade, cx + (px - OX), (FH - py))
        fill_poly(pts, put)
    for py in range(FH):
        for px in range(FW):
            o = owner[py][px]
            if not o:
                continue
            b, shade, lx, ly = o
            noise = 0.94 + rnd.random() * 0.1
            m = b.mat
            role = silk_role(m, pattern, lx, ly)
            if role:
                g = int(255 * min(1.0, 0.86 * shade * noise))
                layers[role].putpixel((px, py), (g, g, g, 255))
            elif m in COAT_MATS:
                g = int(255 * min(1.0, COAT_MATS[m] * shade * noise))
                layers['coat'].putpixel((px, py), (g, g, g, 255))
            else:
                c = BASE_COL[m]
                k = min(1.25, shade * noise)
                layers['base'].putpixel((px, py), tuple(max(0, min(255, int(v * k))) for v in c) + (255,))
    # contorno oscuro alrededor de la silueta (se lee mejor en la pantalla)
    full = [[any(layers[k].getpixel((x, y))[3] for k in layers) for x in range(FW)] for y in range(FH)]
    for y in range(FH):
        for x in range(FW):
            if full[y][x]:
                continue
            if any(0 <= x + dx < FW and 0 <= y + dy < FH and full[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                layers['base'].putpixel((x, y), (14, 12, 14, 200))
    # número en la mantilla (se dibuja como "marca" clara; el número real lo pone el juego encima)
    return layers


def main():
    sheet = Image.new('RGBA', (FW * FRAMES, FH * 10), (0, 0, 0, 0))
    for f in range(FRAMES):
        l0 = render(f, 0)
        sheet.paste(l0['base'], (f * FW, 0))
        sheet.paste(l0['coat'], (f * FW, FH))
        for pat in range(4):
            lp = render(f, pat)
            sheet.paste(lp['silkA'], (f * FW, FH * (2 + pat)))
            sheet.paste(lp['silkB'], (f * FW, FH * (6 + pat)))
    os.makedirs(OUT, exist_ok=True)
    sheet.save(os.path.join(OUT, 'race_horse.png'))
    if len(sys.argv) > 1:
        coats = [(130, 70, 35), (172, 92, 44), (46, 40, 40), (222, 222, 216), (222, 176, 96), (86, 54, 32)]
        silks = [(220, 40, 40), (40, 90, 220), (40, 170, 70), (240, 210, 40), (240, 240, 240), (30, 30, 34), (240, 130, 30),
                 (140, 60, 200), (240, 110, 170), (40, 200, 210), (150, 220, 40), (20, 30, 90)]
        prev = Image.new('RGBA', (FW * FRAMES * 4, FH * 6 * 4), (60, 120, 50, 255))
        for h in range(6):
            for f in range(FRAMES):
                def tint(im, c):
                    im = im.copy(); px = im.load()
                    for yy in range(FH):
                        for xx in range(FW):
                            r, g, b, a = px[xx, yy]
                            if a: px[xx, yy] = (r * c[0] // 255, g * c[1] // 255, b * c[2] // 255, a)
                    return im
                cell = Image.new('RGBA', (FW, FH), (0, 0, 0, 0))
                pat = h % 4
                for img in (tint(sheet.crop((f * FW, FH, f * FW + FW, 2 * FH)), coats[h]),
                            tint(sheet.crop((f * FW, FH * (2 + pat), f * FW + FW, FH * (3 + pat))), silks[h * 2 % 12]),
                            tint(sheet.crop((f * FW, FH * (6 + pat), f * FW + FW, FH * (7 + pat))), silks[(h * 2 + 5) % 12]),
                            sheet.crop((f * FW, 0, f * FW + FW, FH))):
                    cell = Image.alpha_composite(cell, img)
                big = cell.resize((FW * 4, FH * 4), Image.NEAREST)
                prev.alpha_composite(big, (f * FW * 4, h * FH * 4))
        prev.save(sys.argv[1])


if __name__ == '__main__':
    main()
