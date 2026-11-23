#!/usr/bin/env python3
"""El uniforme de las piezas (Los Juegos del Coleccionista): camisa de manga larga color hueso, gastada y sucia, pantalón gris oscuro,
tenis de lona y una tela cosida en el pecho y en la espalda (el número lo escribe el juego). Unas cuantas manchas rojas, distintas en
cada variante.

Se pinta con el acomodo de una skin de Minecraft (64x64) pero al doble de resolución (128x128: cada píxel del modelo son 2x2). Lo
transparente deja ver la skin: la cabeza, las manos y el cuello.

Uso: python3 uniforme.py   (escribe en src/main/resources/assets/coleccionista/textures/prenda/)
"""
import os

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(AQUI, '..', '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'textures', 'prenda')
K = 2  # texeles por píxel del modelo
VARIANTES = 6
NIVELES = 4  # desgaste: 0 al despertar, 3 al final

HUESO = np.array([192, 182, 160], np.float32)
PANTALON = np.array([64, 64, 66], np.float32)
TENIS = np.array([46, 42, 38], np.float32)
SUELA = np.array([150, 142, 128], np.float32)
TELA_NUM = np.array([226, 220, 204], np.float32)
SANGRE = [np.array(c, np.float32) for c in ([112, 18, 18], [138, 26, 24], [86, 22, 16], [70, 20, 14])]


def ruido(h, w, seed, escala=1.0):
    r = np.random.RandomState(seed)
    a = r.rand(h, w).astype(np.float32)
    if escala > 1:
        hh, ww = max(2, int(h / escala)), max(2, int(w / escala))
        b = np.array(Image.fromarray((r.rand(hh, ww) * 255).astype(np.uint8)).resize((w, h), Image.BILINEAR), np.float32) / 255
        a = a * 0.4 + b * 0.6
    return a


class Lienzo:
    def __init__(self):
        self.img = np.zeros((64 * K, 64 * K, 4), np.float32)

    def cara(self, x, y, w, h):
        """Una cara (en píxeles de la skin) para pintar: devuelve la vista (h*K, w*K, 4)."""
        return self.img[y * K:(y + h) * K, x * K:(x + w) * K]


def tela(v, base, seed, sucio=0.25, trama=True):
    """Tela con trama fina, variación y mugre."""
    h, w = v.shape[:2]
    n = ruido(h, w, seed, 3.0)
    rgb = base[None, None, :] * (0.86 + 0.18 * n[..., None])
    if trama:
        yy, xx = np.mgrid[0:h, 0:w]
        rgb = rgb * (1 - 0.04 * (((xx + yy) % 2) == 0))[..., None]
    m = ruido(h, w, seed + 7, 6.0)
    rgb = np.where((m > 1 - sucio)[..., None], rgb * 0.82 + np.array([18, 14, 6], np.float32), rgb)
    v[..., :3] = rgb
    v[..., 3] = 255


def linea_h(v, fila, color, alfa=1.0):
    v[fila, :, :3] = v[fila, :, :3] * (1 - alfa) + color * alfa


def linea_v(v, col, color, alfa=1.0):
    v[:, col, :3] = v[:, col, :3] * (1 - alfa) + color * alfa


def degradado_abajo(v, desde, fuerza):
    """Más sucio hacia abajo (desde la fila 'desde', en texeles)."""
    h = v.shape[0]
    for y in range(desde, h):
        f = (y - desde + 1) / max(1, h - desde) * fuerza
        v[y, :, :3] = v[y, :, :3] * (1 - f) + np.array([92, 78, 58], np.float32) * f


def mancha(v, cx, cy, r, seed, color=None, gotas=3):
    """Una mancha de sangre seca: centro irregular y gotitas alrededor (en texeles de la cara)."""
    rs = np.random.RandomState(seed)
    h, w = v.shape[:2]
    c = color if color is not None else SANGRE[rs.randint(len(SANGRE))]
    yy, xx = np.mgrid[0:h, 0:w]
    ang = np.arctan2(yy - cy, xx - cx)
    borde = r * (0.75 + 0.35 * np.sin(ang * 3 + rs.rand() * 6) + 0.15 * np.sin(ang * 7 + rs.rand() * 6))
    d = np.hypot(xx - cx, yy - cy)
    m = d <= borde
    tono = (0.85 + 0.3 * rs.rand(h, w))[..., None]
    v[..., :3] = np.where(m[..., None], v[..., :3] * 0.25 + c * tono * 0.75, v[..., :3])
    # orilla más oscura (seca)
    o = m & (d > borde - 0.9)
    v[..., :3] = np.where(o[..., None], v[..., :3] * 0.8, v[..., :3])
    for _ in range(gotas):
        a = rs.rand() * 6.28
        dd = r + 1 + rs.rand() * r * 1.6
        gx, gy = int(cx + np.cos(a) * dd), int(cy + np.sin(a) * dd)
        if 0 <= gx < w and 0 <= gy < h:
            v[gy, gx, :3] = v[gy, gx, :3] * 0.3 + c * 0.7


def caja(L, u, v_, w, h, d):
    """Las 6 caras de una caja de skin: arriba, abajo, derecha, frente, izquierda, atrás (x, y, ancho, alto)."""
    return {
        'arriba': (u + d, v_, w, d), 'abajo': (u + d + w, v_, w, d),
        'der': (u, v_ + d, d, h), 'frente': (u + d, v_ + d, w, h), 'izq': (u + d + w, v_ + d, d, h), 'atras': (u + d + w + d, v_ + d, w, h)}


def camisa(L, u, v_, w, h, d, seed, cuerpo, delgado=False):
    caras = caja(L, u, v_, w, h, d)
    for i, (k, (x, y, cw, ch)) in enumerate(caras.items()):
        if k == 'abajo' and not cuerpo:
            continue  # el puño queda abierto: se ve la mano
        vv = L.cara(x, y, cw, ch)
        tela(vv, HUESO, seed + i * 13, sucio=0.3)
        if k in ('der', 'frente', 'izq', 'atras') and not cuerpo:
            # manga larga: el puño (doblez) y la mano libre abajo
            alto = ch * K
            linea_h(vv, alto - 3 * K - 1, np.array([150, 140, 120], np.float32), 0.6)
            vv[alto - 3 * K:, :, 3] = 0
            degradado_abajo(vv, alto // 2, 0.22)
            # costura del hombro arriba
            linea_h(vv, 0, np.array([160, 150, 130], np.float32), 0.5)
        if cuerpo and k in ('frente', 'atras', 'der', 'izq'):
            alto = ch * K
            # la camisa llega a la cintura; abajo el pantalón (con su cordón)
            cintura = 9 * K
            pv = vv[cintura:]
            tela(pv, PANTALON, seed + 50 + i, sucio=0.25)
            linea_h(vv, cintura, np.array([40, 40, 42], np.float32))
            linea_h(vv, cintura + 1, np.array([40, 40, 42], np.float32), 0.6)
            degradado_abajo(vv[:cintura], cintura // 2, 0.15)
            if k == 'frente':
                # cuello en V (se ve la piel) y la tapeta con dos botones
                mid = cw * K // 2
                for yy in range(3 * K):
                    ancho = max(0, 3 * K - 1 - yy)
                    vv[yy, mid - ancho // 2:mid + ancho // 2 + (1 if ancho else 0), 3] = 0
                linea_v(vv, mid - 1, np.array([170, 160, 140], np.float32), 0.7)
                for by in (4 * K, 7 * K):
                    vv[by:by + 2, mid - 1:mid + 1, :3] = np.array([120, 112, 96], np.float32)
                # cordón del pantalón
                vv[cintura + 2:cintura + 5, mid - 2, :3] = np.array([200, 192, 170], np.float32)
                vv[cintura + 2:cintura + 4, mid + 1, :3] = np.array([200, 192, 170], np.float32)
            if k in ('der', 'izq'):
                # costura del costado
                linea_v(vv[:cintura], cw * K // 2, np.array([170, 160, 140], np.float32), 0.5)
    return caras


def tela_numero(vv, x0, y0, w, h):
    """La tela cosida donde va el número (en texeles de la cara): blanca, con puntadas alrededor."""
    vv[y0:y0 + h, x0:x0 + w, :3] = TELA_NUM * (0.94 + 0.08 * ruido(h, w, x0 * 7 + y0)[..., None])
    for x in range(x0, x0 + w, 2):
        vv[y0, x, :3] = np.array([120, 110, 92], np.float32)
        vv[y0 + h - 1, x, :3] = np.array([120, 110, 92], np.float32)
    for y in range(y0, y0 + h, 2):
        vv[y, x0, :3] = np.array([120, 110, 92], np.float32)
        vv[y, x0 + w - 1, :3] = np.array([120, 110, 92], np.float32)


def pantalon(L, u, v_, seed):
    caras = caja(L, u, v_, 4, 12, 4)
    for i, (k, (x, y, cw, ch)) in enumerate(caras.items()):
        vv = L.cara(x, y, cw, ch)
        if k == 'abajo':
            vv[..., :3] = SUELA * 0.6
            vv[..., 3] = 255
            continue
        tela(vv, PANTALON, seed + i * 11, sucio=0.3)
        if k == 'arriba':
            continue
        alto = ch * K
        # tenis de lona abajo (2 px) con suela clara
        vv[alto - 2 * K:, :, :3] = TENIS * (0.9 + 0.15 * ruido(2 * K, cw * K, seed + i)[..., None])
        linea_h(vv, alto - 1, SUELA)
        linea_h(vv, alto - 2 * K, np.array([30, 28, 26], np.float32))
        # rodilla gastada y raya del planchado
        if k in ('frente',):
            r0 = 5 * K
            vv[r0:r0 + 2 * K, 1:-1, :3] = vv[r0:r0 + 2 * K, 1:-1, :3] * 1.12
            linea_v(vv[:alto - 2 * K], cw * K // 2, np.array([52, 52, 54], np.float32), 0.6)
        degradado_abajo(vv[:alto - 2 * K], (alto - 2 * K) // 2, 0.25)
    return caras


def rasgadura(vv, x0, y0, largo, seed):
    """Una rasgadura: una línea quebrada oscura con la orilla deshilachada (en texeles de la cara)."""
    rs = np.random.RandomState(seed)
    h, w = vv.shape[:2]
    x, y = x0, y0
    for _ in range(largo):
        if 0 <= x < w and 0 <= y < h and vv[y, x, 3] > 0:
            vv[y, x, :3] = np.array([38, 32, 26], np.float32)
            if 0 <= y - 1 < h and vv[y - 1, x, 3] > 0:
                vv[y - 1, x, :3] = vv[y - 1, x, :3] * 1.12
        x += 1
        y += rs.choice([-1, 0, 0, 1])


def mugre(vv, fuerza, seed):
    """Mugre general (manchas de tierra y grasa) según el desgaste."""
    h, w = vv.shape[:2]
    m = ruido(h, w, seed, 5.0)
    sucio = (m > 1 - 0.18 * fuerza) & (vv[..., 3] > 0)
    vv[..., :3] = np.where(sucio[..., None], vv[..., :3] * 0.72 + np.array([30, 22, 10], np.float32), vv[..., :3])


def pintar(variante, delgado, nivel=0):
    L = Lienzo()
    s = 100 * variante
    ab = 3 if delgado else 4
    cuerpo = camisa(L, 16, 16, 8, 12, 4, s + 1, True)
    bd = camisa(L, 40, 16, ab, 12, 4, s + 2, False, delgado)
    bi = camisa(L, 32, 48, ab, 12, 4, s + 3, False, delgado)
    pd = pantalon(L, 0, 16, s + 4)
    pi = pantalon(L, 16, 48, s + 5)
    # tela del número: pecho (lado izquierdo de la pieza = derecha de quien la ve) y espalda
    x, y, w, h = cuerpo['frente']
    tela_numero(L.cara(x, y, w, h), 4 * K + 1, 2 * K, 4 * K - 2, 3 * K)
    x, y, w, h = cuerpo['atras']
    tela_numero(L.cara(x, y, w, h), 1 * K, 1 * K, 6 * K, 5 * K)
    # manchas de sangre (pocas): cada variante en otros lugares
    rs = np.random.RandomState(9000 + variante)
    lugares = [
        ('cuerpo', 'frente', (1.5, 6.5)), ('cuerpo', 'frente', (6.0, 7.5)), ('cuerpo', 'atras', (5.5, 8.0)),
        ('cuerpo', 'der', (2.0, 5.0)), ('bd', 'frente', (2.0, 7.5)), ('bi', 'frente', (1.5, 6.0)), ('bd', 'der', (2.0, 4.0)),
        ('pd', 'frente', (2.0, 3.0)), ('pi', 'frente', (2.0, 6.5)), ('pi', 'izq', (2.0, 2.5)), ('bi', 'atras', (2.0, 3.0))]
    cajas = {'cuerpo': cuerpo, 'bd': bd, 'bi': bi, 'pd': pd, 'pi': pi}
    elegidos = rs.choice(len(lugares), size=3 + variante % 3, replace=False)
    for j, n in enumerate(elegidos):
        parte, cara, (cx, cy) = lugares[n]
        x, y, w, h = cajas[parte][cara]
        vv = L.cara(x, y, w, h)
        cx = min(w - 0.8, cx * (w / 4 if parte != 'cuerpo' or cara in ('der', 'izq') else 1))
        mancha(vv, cx * K + rs.uniform(-1, 1), cy * K + rs.uniform(-1, 1), K * rs.uniform(0.6, 1.3), 777 * variante + j,
               gotas=rs.randint(1, 5))
    # una salpicadura de gotitas en el pecho en algunas variantes
    if variante % 2 == 0:
        x, y, w, h = cuerpo['frente']
        vv = L.cara(x, y, w, h)
        for g in range(6):
            gx, gy = rs.randint(1, w * K - 1), rs.randint(K, 8 * K)
            if vv[gy, gx, 3] > 0:
                vv[gy, gx, :3] = vv[gy, gx, :3] * 0.3 + SANGRE[g % 4] * 0.7
    # desgaste: con cada prueba que sobreviven el uniforme se ve peor (más mugre, más manchas, rasgaduras)
    if nivel > 0:
        for j, (k, caras) in enumerate(cajas.items()):
            for cara, (x, y, w, h) in caras.items():
                if cara in ('arriba', 'abajo'):
                    continue
                mugre(L.cara(x, y, w, h), nivel, 5000 + variante * 31 + j * 7 + len(cara))
        # sudor amarillento bajo los brazos
        for cara in ('der', 'izq'):
            x, y, w, h = cuerpo[cara]
            vv = L.cara(x, y, w, h)
            vv[K:4 * K, 1:-1, :3] = vv[K:4 * K, 1:-1, :3] * (1 - 0.12 * nivel) + np.array([150, 130, 60], np.float32) * 0.12 * nivel
        extra = rs.choice(len(lugares), size=min(len(lugares), nivel + 1), replace=False)
        for j, n in enumerate(extra):
            parte, cara, (cx, cy) = lugares[n]
            x, y, w, h = cajas[parte][cara]
            vv = L.cara(x, y, w, h)
            mancha(vv, min(w * K - 2, cx * K * 1.1 + rs.uniform(-2, 2)), cy * K + rs.uniform(-2, 2), K * rs.uniform(0.7, 1.0 + 0.15 * nivel),
                   4000 + 97 * variante + 13 * nivel + j, gotas=rs.randint(2, 6))
    if nivel >= 2:
        for j, (parte, cara, fila) in enumerate((('cuerpo', 'frente', 7), ('bd', 'frente', 5), ('pi', 'frente', 6), ('cuerpo', 'atras', 9))[:nivel + 1]):
            x, y, w, h = cajas[parte][cara]
            rasgadura(L.cara(x, y, w, h), rs.randint(0, 3), fila * K + rs.randint(-2, 3), w * K // 2 + rs.randint(0, 4), 6000 + j + variante)
    img = np.clip(L.img, 0, 255).astype(np.uint8)
    return Image.fromarray(img, 'RGBA')


def main():
    os.makedirs(SALIDA, exist_ok=True)
    n = 0
    for v in range(1, VARIANTES + 1):
        for nivel in range(NIVELES):
            fin = '' if nivel == 0 else f'_{nivel}'
            pintar(v, False, nivel).save(os.path.join(SALIDA, f'uniforme_{v}{fin}.png'))
            pintar(v, True, nivel).save(os.path.join(SALIDA, f'uniforme_delgado_{v}{fin}.png'))
            n += 2
    print(f'{n} texturas -> {os.path.normpath(SALIDA)}')


if __name__ == '__main__':
    main()
