#!/usr/bin/env python3
"""Texturas de bloque entero (32x32) de la mansión gótica del Coleccionista (capítulo 2): el techo de pizarra, las cornisas de cantera,
la balaustrada de madera, y el jardín de noche (pasto seco, tierra, grava, setos, troncos y ramas secas).

Mismas reglas que materiales.py: cada función recibe `seed` (variantes) y devuelve una imagen RGBA; el ruido se repite cada bloque para
que encaje con el de al lado.
"""
import numpy as np
from PIL import Image

import materiales as M
from materiales import N, fbm, white, hexc, ramp, mix, img, grime, envejecer, streaks, cracks


def pizarra(seed=0):
    """Tejas de pizarra en escama (cola de pez): gris azulado, cada teja de su tono, con orilla oscura abajo, liquen y escurrimientos."""
    r = np.random.RandomState(seed + 20100)
    y, x = np.mgrid[0:N, 0:N]
    fila = y // 8
    off = (fila % 2) * 4
    lx = (x + off) % 8
    ly = y % 8
    tid = ((x + off) // 8) + fila * 5
    # el tono de cada teja no depende de la variante (las variantes encajan)
    tono = np.vectorize(lambda t: 0.82 + ((t * 2654435761 + 104729) % 97) / 97 * 0.32)(tid).astype(np.float32)
    base = ramp(fbm(N, N, 5, seed + 20101), '#2a2e35', '#313640', '#383e48', '#3f4550')
    rgb = base * tono[..., None]
    # la orilla redonda de abajo de cada teja (la escama): fuera del arco se ve la teja de abajo, más oscura
    cx = lx - 3.5
    arco = (cx * cx) / 16.0 + ((ly - 2.5) ** 2) / 30.0 > 1.0
    abajo = arco & (ly >= 4)
    rgb = np.where(abajo[..., None], rgb * 0.62, rgb)
    borde = np.abs((cx * cx) / 16.0 + ((ly - 2.5) ** 2) / 30.0 - 1.0) < 0.22
    rgb = np.where((borde & (ly >= 3))[..., None], hexc('#16181c') * (0.9 + 0.2 * white(N, N, seed)[..., None]), rgb)
    # brillo arriba de cada teja (la luz de la luna le pega al canto)
    rgb = np.where(((ly == 0) & (lx > 0) & (lx < 7))[..., None], rgb * 1.18 + 5, rgb)
    rgb = np.where((lx == 0)[..., None] & (ly < 4)[..., None], rgb * 0.7, rgb)
    tipo = seed % 3
    if tipo == 1:   # liquen pálido en manchas
        m = fbm(N, N, 5, seed + 20102)
        rgb = mix(rgb, ramp(white(N, N, seed + 20103), '#5a6150', '#6e7660', '#7d8468')[...], np.clip((m - 0.62) * 2.6, 0, 0.75))
    elif tipo == 2:  # una teja rota
        tx, ty = r.randint(0, 4), r.randint(0, 4)
        rota = (tid == tx + ty * 5) & (ly < 6)
        rgb = np.where(rota[..., None], hexc('#121317'), rgb)
    rgb = streaks(rgb, seed + 20104, n=r.randint(1, 4), color='#1b1d21', strength=0.3)
    rgb = grime(rgb, seed + 20105, amount=0.2)
    return img(rgb)


def cornisa(seed=0):
    """Banda de cantera tallada (cornisas, repisas y dinteles): moldura arriba, una ranura, dentículos y la piedra gastada."""
    y, x = np.mgrid[0:N, 0:N]
    base = hexc('#6d6858') * (0.86 + 0.26 * fbm(N, N, 5, seed + 20201))[..., None]
    rgb = base.copy()
    # moldura de arriba (canto redondo): claro arriba, sombra abajo
    rgb = np.where((y <= 2)[..., None], rgb * 1.2 + 6, rgb)
    rgb = np.where(((y >= 3) & (y <= 4))[..., None], rgb * 0.7, rgb)
    # ranura
    rgb = np.where(((y >= 9) & (y <= 10))[..., None], rgb * 0.55, rgb)
    rgb = np.where((y == 11)[..., None], rgb * 1.12, rgb)
    # dentículos (cubitos): de 4 de ancho cada 6 (se repite: 32 no es múltiplo de 6, así que cada 8)
    den = (y >= 14) & (y <= 21) & ((x % 8) >= 1) & ((x % 8) <= 5)
    rgb = np.where(den[..., None], rgb * 1.1 + 4, rgb)
    rgb = np.where(((y >= 14) & (y <= 21) & ~den)[..., None], rgb * 0.5, rgb)
    rgb = np.where((den & (y == 21))[..., None], rgb * 0.75, rgb)
    # junta abajo
    rgb = np.where((y >= 30)[..., None], rgb * 0.72, rgb)
    rgb = np.where(((x % 32) == 0)[..., None], rgb * 0.6, rgb)
    rgb = envejecer(rgb, seed + 20202, 0.45)
    rgb = streaks(rgb, seed + 20203, n=2, color='#2a2620', strength=0.35, start=0.3)
    rgb = grime(rgb, seed + 20204, amount=0.25, bottom=0.15)
    return img(rgb)


def balaustrada(seed=0):
    """Balaustrada de madera: pasamanos arriba, zócalo abajo y balaustres torneados (dos por bloque). Lo demás es transparente."""
    y, x = np.mgrid[0:N, 0:N]
    pasa = y <= 3
    zoc = y >= 29
    lx = x % 16
    # el balaustre torneado: ancho que cambia con la altura (bulbo abajo, cuello arriba)
    t = (y - 4) / 24.0
    medio = 2.0 + 1.7 * np.exp(-((t - 0.68) ** 2) / 0.02) + 0.9 * np.exp(-((t - 0.25) ** 2) / 0.006)
    bal = (np.abs(lx - 7.5) <= medio) & (y > 3) & (y < 29)
    solid = pasa | zoc | bal
    veta = np.sin(y * 0.9 + x * 0.15 + fbm(N, N, 5, seed + 20301) * 6) * 0.5 + 0.5
    rgb = hexc('#3a2414') * (0.75 + 0.35 * veta)[..., None]
    rgb = np.where((y == 0)[..., None], rgb * 1.35 + 10, rgb)
    rgb = np.where((y == 3)[..., None], rgb * 0.6, rgb)
    rgb = np.where((y == 29)[..., None], rgb * 1.25 + 6, rgb)
    # brillo del lado izquierdo del balaustre (es redondo)
    rgb = np.where((bal & (lx < 7.5 - medio * 0.3))[..., None], rgb * 1.25 + 6, rgb)
    rgb = np.where((bal & (lx > 7.5 + medio * 0.5))[..., None], rgb * 0.7, rgb)
    out = np.zeros((N, N, 4), np.uint8)
    out[..., :3] = np.clip(rgb, 0, 255)
    out[..., 3] = np.where(solid, 255, 0)
    return Image.fromarray(out, 'RGBA')


def pasto(seed=0):
    """Pasto de noche, seco y ralo: verde muy oscuro con hebras amarillentas y tierra que se asoma."""
    r = np.random.RandomState(seed + 20400)
    t = fbm(N, N, 6, seed + 20401)
    rgb = ramp(t * 0.8 + white(N, N, seed + 20402) * 0.2, '#1f2a18', '#27331d', '#2f3c22', '#384528')
    # hebras secas
    for _ in range(26):
        x, y = r.randint(0, N), r.randint(0, N)
        c = hexc(r.choice(['#5b5a36', '#4d4a2e', '#646241']))
        for k in range(r.randint(2, 4)):
            rgb[(y + k) % N, (x + (k if r.rand() < 0.4 else 0)) % N] = c * (0.85 + 0.3 * r.rand())
    # tierra pelona en manchas
    d = fbm(N, N, 9, seed + 20403) > 0.66
    rgb = np.where(d[..., None], ramp(white(N, N, seed + 20404), '#2a2017', '#33281c', '#3b2e20')[...], rgb)
    rgb = grime(rgb, seed + 20405, amount=0.18)
    return img(rgb)


def pasto_lado(seed=0):
    """El lado del pasto: tierra oscura con el pasto que cuelga un poco arriba."""
    r = np.random.RandomState(seed + 20500)
    y, x = np.mgrid[0:N, 0:N]
    rgb = tierra_rgb(seed)
    fleco = 4 + (np.vectorize(lambda v: (v * 7919 + seed * 31) % 5)(x)).astype(np.float32)
    verde = ramp(white(N, N, seed + 20501), '#22301a', '#2b3920', '#344226')
    rgb = np.where((y < fleco)[..., None], verde, rgb)
    rgb = np.where((np.abs(y - fleco) < 1)[..., None], rgb * 0.6, rgb)
    return img(rgb)


def tierra_rgb(seed=0):
    r = np.random.RandomState(seed + 20600)
    t = fbm(N, N, 5, seed + 20601)
    rgb = ramp(t * 0.7 + white(N, N, seed + 20602) * 0.3, '#221a13', '#2a2017', '#32261b', '#3a2d20')
    # piedritas
    for _ in range(9):
        x, y = r.randint(0, N), r.randint(0, N)
        c = hexc(r.choice(['#4a4239', '#554c41', '#3f3830']))
        rgb[y, x] = c
        rgb[y, (x + 1) % N] = c * 0.8
    # raicitas
    for _ in range(2):
        x, y = r.randint(0, N), r.randint(0, N)
        for k in range(r.randint(4, 9)):
            rgb[y % N, x % N] = hexc('#4b3a28')
            x += r.choice([1, 1, 0])
            y += r.choice([0, 1, 1, -1])
    return rgb


def tierra(seed=0):
    """Tierra negra de las tumbas y los arriates, húmeda, con piedritas y raíces."""
    return img(grime(tierra_rgb(seed), seed + 20603, amount=0.2))


def grava(seed=0):
    """Grava del camino: piedritas redondas gris café, con sombra abajo y brillo arriba."""
    y, x = np.mgrid[0:N, 0:N]
    rgb = np.broadcast_to(hexc('#2a2622'), (N, N, 3)).copy()
    # piedras: puntos en una rejilla que se mueve (el mismo acomodo en todas las variantes para que encajen)
    rr = np.random.RandomState(20700)
    for gy in range(0, N, 4):
        for gx in range(0, N, 4):
            cx, cy = gx + rr.randint(0, 4), gy + rr.randint(0, 4)
            rad = 1.6 + rr.rand() * 1.4
            col = hexc(rr.choice(['#5c5750', '#6a645b', '#4e4943', '#777064', '#585046']))
            dx = ((x - cx + N / 2) % N) - N / 2
            dy = ((y - cy + N / 2) % N) - N / 2
            d = np.hypot(dx, dy * 1.15)
            k = d < rad
            luz = np.clip(1.15 - (dy + rad) / (2.4 * rad) * 0.5, 0.7, 1.2)
            rgb = np.where(k[..., None], col * luz[..., None], rgb)
    rgb = rgb * (0.9 + 0.2 * fbm(N, N, 8, seed + 20701))[..., None]
    rgb = grime(rgb, seed + 20702, amount=0.25)
    return img(rgb)


def seto(seed=0):
    """Seto podado de hojas chicas, verde casi negro, con huecos y ramitas secas (opaco)."""
    r = np.random.RandomState(seed + 20800)
    rgb = ramp(fbm(N, N, 4, seed + 20801), '#101a0e', '#162212', '#1d2b17', '#25351c')
    rr = np.random.RandomState(20802 + seed % 2)
    y, x = np.mgrid[0:N, 0:N]
    for _ in range(70):
        cx, cy = rr.randint(0, N), rr.randint(0, N)
        col = hexc(rr.choice(['#2b3e20', '#324726', '#3a502b', '#22331a']))
        dx = ((x - cx + N / 2) % N) - N / 2
        dy = ((y - cy + N / 2) % N) - N / 2
        hoja = (dx * dx) / 3.2 + (dy * dy) / 1.6 < 1
        rgb = np.where(hoja[..., None], col * (0.9 + 0.25 * (dy < 0))[..., None], rgb)
    hueco = fbm(N, N, 4, seed + 20803) < 0.28
    rgb = np.where(hueco[..., None], rgb * 0.45, rgb)
    if seed % 3 == 1:   # una mancha seca café
        m = fbm(N, N, 7, seed + 20804) > 0.68
        rgb = np.where(m[..., None], ramp(white(N, N, seed + 20805), '#3a2e1c', '#47381f', '#523f22')[...], rgb)
    for _ in range(3):
        x0, y0 = r.randint(0, N), r.randint(0, N)
        for k in range(r.randint(3, 6)):
            rgb[(y0 + k) % N, (x0 + k // 2) % N] = hexc('#3c2f22')
    return img(rgb)


def corteza(seed=0):
    """Corteza de árbol muerto: gris café, con surcos verticales hondos y liquen."""
    y, x = np.mgrid[0:N, 0:N]
    surco = np.sin(x * 0.75 + fbm(N, N, 6, seed + 20901) * 5 + np.sin(y * 0.2) * 0.6)
    rgb = ramp(surco * 0.5 + 0.5, '#1d1914', '#2b251e', '#373027', '#423a30')
    rgb = rgb * (0.85 + 0.3 * fbm(N, N, 3, seed + 20902))[..., None]
    rgb = np.where((surco < -0.75)[..., None], hexc('#120f0c'), rgb)
    m = fbm(N, N, 6, seed + 20903)
    rgb = mix(rgb, np.broadcast_to(hexc('#4c5544'), rgb.shape), np.clip((m - 0.66) * 2.4, 0, 0.6))
    return img(rgb)


def corte_tronco(seed=0):
    """El corte de arriba del tronco: anillos claros con la corteza alrededor."""
    y, x = np.mgrid[0:N, 0:N]
    d = np.hypot(x - 15.5, y - 15.5)
    anillos = np.sin(d * 1.3 + fbm(N, N, 6, seed + 21001) * 3) * 0.5 + 0.5
    rgb = ramp(anillos, '#4a3a28', '#55432f', '#614d36', '#6d573d')
    rgb = np.where((d > 13)[..., None], np.array(corteza(seed))[..., :3].astype(np.float32), rgb)
    rgb = np.where((np.abs(d - 13) < 0.8)[..., None], hexc('#1a1510'), rgb)
    rgb = cracks(rgb, seed + 21002, n=1, dark=0.5)
    return img(rgb)


def ramas(seed=0):
    """Ramitas secas (se unen como barrotes): una vara al centro con brotes chuecos; lo demás transparente."""
    r = np.random.RandomState(21100)
    a = np.zeros((N, N), bool)
    rgb = np.zeros((N, N, 3), np.float32)
    col = hexc('#2a2119')

    def pinta(x, y, c):
        if 0 <= x < N and 0 <= y < N:
            a[y, x] = True
            rgb[y, x] = c

    # la vara de en medio (de abajo a arriba, un poco chueca) y las de los lados (a la orilla, para que se unan)
    x = 15
    for y in range(N - 1, -1, -1):
        pinta(x, y, col * (0.9 + 0.2 * r.rand()))
        pinta(x + 1, y, col * 0.75)
        if r.rand() < 0.12:
            x = int(np.clip(x + r.choice([-1, 1]), 13, 17))
    for _ in range(7):
        y0 = r.randint(3, N - 3)
        x0 = 15 + r.choice([-1, 2])
        dx = r.choice([-1, 1])
        x, y = x0, y0
        for k in range(r.randint(5, 13)):
            pinta(x, y, col * (0.85 + 0.25 * r.rand()))
            x += dx
            if r.rand() < 0.55:
                y -= 1
        # una horquilla
        pinta(x, y - 1, col)
        pinta(x + dx, y - 2, col)
    out = np.zeros((N, N, 4), np.uint8)
    out[..., :3] = np.clip(rgb, 0, 255)
    out[..., 3] = np.where(a, 255, 0)
    return Image.fromarray(out, 'RGBA')


def papel(seed, fondo, dibujo):
    return M.papel_tapiz(seed, fondo=fondo, dibujo=dibujo)


def techo_yeso(seed=0):
    """Plafón de yeso con casetones (cuadros hundidos) y una moldura: el techo de los cuartos finos."""
    y, x = np.mgrid[0:N, 0:N]
    base = hexc('#7a7262') * (0.88 + 0.22 * fbm(N, N, 6, seed + 21201))[..., None]
    lx, ly = x % 32, y % 32
    marco = (lx <= 3) | (lx >= 28) | (ly <= 3) | (ly >= 28)
    rgb = np.where(marco[..., None], base * 1.08 + 4, base * 0.86)
    rgb = np.where(((lx == 4) | (ly == 4))[..., None], rgb * 0.62, rgb)
    rgb = np.where(((lx == 27) | (ly == 27))[..., None], rgb * 1.18 + 5, rgb)
    rosa = np.hypot(lx - 15.5, ly - 15.5)
    rgb = np.where(((rosa > 3) & (rosa < 4.4))[..., None], rgb * 1.15 + 4, rgb)
    rgb = np.where((rosa < 2)[..., None], rgb * 0.85, rgb)
    if seed % 3 == 1:   # mancha de humedad
        rgb = mix(rgb, np.broadcast_to(hexc('#4a3d28'), rgb.shape), np.clip((fbm(N, N, 10, seed + 21202) - 0.55) * 1.8, 0, 0.55))
    rgb = cracks(rgb, seed + 21203, n=1, dark=0.6) if seed % 3 == 2 else rgb
    rgb = grime(rgb, seed + 21204, amount=0.22)
    return img(rgb)
