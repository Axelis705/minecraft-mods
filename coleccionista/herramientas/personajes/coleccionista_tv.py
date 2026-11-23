#!/usr/bin/env python3
"""El Coleccionista en las pantallas, en vivo: medio cuerpo en 3D sentado en su sillón de terciopelo (máscara de porcelana con grieta,
ojos huecos con un punto rojo que brilla, sombrero de copa con cinta roja, frac negro con solapas de satín, moño rojo, guantes blancos).
El juego lo mueve por código (respira, ladea la cabeza como muñeco, junta las manos, hace ademanes al hablar, se ríe) y lo aplana sobre
el vidrio de la tele.

Escala: el triple de un jugador (la cabeza mide 24, una de jugador 8), un texel por unidad (la cara es de 24x24).

Escribe:
  assets/coleccionista/geo/coleccionista_tv.geo.json
  assets/coleccionista/textures/entity/coleccionista_tv.png   (256x256)
  y el disfraz de Axelis con los mismos dibujos: textures/entity/coleccionista.png (piel al triple, 192x192),
  coleccionista_ojos.png (los puntos rojos que brillan) y sombrero.png (96x48)
Con --vista [pose] dibuja también una vista previa de frente (scratch: vista_coleccionista_tv_<pose>.png).

Uso: python3 coleccionista_tv.py [--vista quieto|habla|risa|todas] [--salida carpeta]
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(AQUI, '..', '..', 'src', 'main', 'resources', 'assets', 'coleccionista')
TW, TH = 256, 256

# ---------------------------------------------------------------- colores
PORC = np.array([238, 232, 222], np.float32)
PORC_S = np.array([206, 198, 188], np.float32)
NEGRO = np.array([14, 12, 16], np.float32)
PELO = np.array([22, 20, 26], np.float32)
FRAC = np.array([30, 26, 34], np.float32)
SATIN = np.array([56, 50, 64], np.float32)
CAMISA = np.array([228, 222, 214], np.float32)
ROJO = np.array([170, 16, 24], np.float32)
ROJO_CL = np.array([228, 54, 60], np.float32)
ORO = np.array([196, 156, 72], np.float32)
ORO_OSC = np.array([120, 88, 36], np.float32)
GUANTE = np.array([238, 236, 230], np.float32)
VELLUDO = np.array([104, 14, 26], np.float32)


def ruido(h, w, seed, escala=1.0):
    r = np.random.RandomState(seed)
    a = r.rand(h, w).astype(np.float32)
    if escala > 1:
        hh, ww = max(2, int(h / escala)), max(2, int(w / escala))
        b = np.array(Image.fromarray((r.rand(hh, ww) * 255).astype(np.uint8)).resize((w, h), Image.BILINEAR), np.float32) / 255
        a = a * 0.4 + b * 0.6
    return a


def llenar(v, color, seed, var=0.10, escala=3.0):
    h, w = v.shape[:2]
    n = ruido(h, w, seed, escala)
    v[..., :3] = color[None, None, :] * (1 - var / 2 + var * n[..., None])
    v[..., 3] = 255


def mezclar(v, mascara, color, alfa=1.0):
    v[..., :3] = np.where(mascara[..., None], v[..., :3] * (1 - alfa) + color * alfa, v[..., :3])


def malla(v):
    h, w = v.shape[:2]
    return np.mgrid[0:h, 0:w]


# ---------------------------------------------------------------- pintores de caras (cada uno recibe la vista (h, w, 4))

def p_cara(v, seed):
    """La máscara: 24x24, de frente."""
    llenar(v, PORC, seed, 0.05, 4)
    yy, xx = malla(v)
    h, w = v.shape[:2]
    # sombra suave hacia las orillas y la barbilla (porcelana redonda)
    dx = np.abs(xx - (w - 1) / 2) / (w / 2)
    sombra = np.clip((dx - 0.62) * 1.3, 0, 0.35) + np.clip((yy - 18) / 6, 0, 1) * 0.18
    v[..., :3] = v[..., :3] * (1 - sombra[..., None]) + PORC_S * sombra[..., None] * 0.2
    # ojos huecos (almendra negra) con el punto rojo
    for ox in (6.5, 16.5):
        e = ((xx - ox) / 3.6) ** 2 + ((yy - 10.6) / 2.3) ** 2 <= 1
        mezclar(v, e, NEGRO)
        borde = (((xx - ox) / 4.4) ** 2 + ((yy - 10.6) / 3.0) ** 2 <= 1) & ~e
        mezclar(v, borde, np.array([150, 140, 136], np.float32), 0.45)
        v[10, int(ox), :3] = ROJO_CL
        v[10, int(ox) + 1, :3] = ROJO_CL
        v[11, int(ox), :3] = ROJO
        v[11, int(ox) + 1, :3] = ROJO
    # cejas pintadas: la izquierda (derecha de quien mira) más alta, como sorprendida
    ceja = np.array([64, 54, 56], np.float32)
    for x in range(3, 11):
        v[6 if 5 <= x <= 8 else 7, x, :3] = ceja
    for x in range(13, 21):
        v[5 if 14 <= x <= 17 else 6, x, :3] = ceja
    # lágrima pintada bajo el ojo derecho (izquierda de quien mira)
    for y in range(13, 17):
        v[y, 6, :3] = NEGRO * 0.6 + PORC * 0.4
    v[17, 6, :3] = NEGRO
    v[17, 5, :3] = NEGRO * 0.7 + PORC * 0.3
    v[17, 7, :3] = NEGRO * 0.7 + PORC * 0.3
    v[18, 6, :3] = NEGRO * 0.8 + PORC * 0.2
    # mejillas
    for cx in (4.0, 19.0):
        m = ((xx - cx) / 2.6) ** 2 + ((yy - 15.2) / 1.4) ** 2 <= 1
        mezclar(v, m, np.array([232, 164, 170], np.float32), 0.55)
    # sonrisa pintada, fina, que sube en las puntas, con puntadas
    boca = np.array([110, 30, 38], np.float32)
    for x in range(6, 18):
        t = (x - 11.5) / 5.5
        y = int(round(19.2 - 1.6 * t * t))
        v[y, x, :3] = boca
        if x % 2 == 0 and 7 <= x <= 16:
            v[y - 1, x, :3] = v[y - 1, x, :3] * 0.55 + boca * 0.45
            v[y + 1, x, :3] = v[y + 1, x, :3] * 0.55 + boca * 0.45
    # la grieta: baja desde arriba a la derecha hasta cerca del ojo
    grieta = [(15, 0), (15, 1), (16, 2), (16, 3), (15, 4), (16, 5), (17, 6), (17, 7), (16, 8), (19, 3), (20, 4), (18, 2)]
    for x, y in grieta:
        v[y, x, :3] = np.array([112, 104, 100], np.float32)
        if x + 1 < w:
            v[y, x + 1, :3] = np.minimum(v[y, x + 1, :3] + 12, 255)


def p_cabeza_lado(v, seed, derecha):
    """Lado de la cabeza (24 de ancho = profundidad): mitad de la cara porcelana, mitad de atrás pelo negro peinado."""
    llenar(v, PORC, seed, 0.05, 4)
    yy, xx = malla(v)
    h, w = v.shape[:2]
    # en la tira de la caja, el lado derecho del personaje tiene la cara a la derecha de la vista; el izquierdo, a la izquierda
    atras = xx < w * 0.55 if derecha else xx >= w * 0.45
    arriba = yy < 7 + (np.abs(xx - (w * 0.55 if derecha else w * 0.45)) * 0.25)
    pelo = atras | arriba
    llenar_p = PELO * (0.9 + 0.2 * ruido(h, w, seed + 3, 2)[..., None])
    v[..., :3] = np.where(pelo[..., None], llenar_p, v[..., :3] * 0.94)
    # orilla de la máscara (una línea fina de sombra) y una cinta negra que la sostiene
    borde = (np.abs(xx - (w * 0.55 if derecha else w * 0.45)) < 0.8) & ~arriba
    mezclar(v, borde, NEGRO, 0.5)
    cinta = (yy >= 10) & (yy <= 11)
    mezclar(v, cinta & ~pelo, np.array([30, 26, 30], np.float32), 0.9)
    # oreja apenas marcada
    oreja = ((xx - (w * 0.62 if derecha else w * 0.38)) ** 2 / 4 + (yy - 13) ** 2 / 9 <= 1) & ~pelo
    mezclar(v, oreja, PORC_S, 0.6)


def p_pelo(v, seed):
    h, w = v.shape[:2]
    v[..., :3] = PELO * (0.85 + 0.3 * ruido(h, w, seed, 2)[..., None])
    v[..., 3] = 255
    yy, xx = malla(v)
    brillo = (np.abs(xx - w * 0.35 - yy * 0.3) < 1.2) & (yy < h * 0.7)
    mezclar(v, brillo, np.array([60, 56, 70], np.float32), 0.5)


def p_porc_sombra(v, seed):
    llenar(v, PORC_S, seed, 0.06, 4)


def p_frac(v, seed, sombra_abajo=True):
    llenar(v, FRAC, seed, 0.12, 3)
    yy, xx = malla(v)
    h = v.shape[0]
    if sombra_abajo:
        f = np.clip((yy - h * 0.55) / h, 0, 1) * 0.35
        v[..., :3] *= (1 - f)[..., None]
    # pliegues muy suaves
    plieg = (np.sin(xx * 0.9 + yy * 0.35 + seed) > 0.93)
    mezclar(v, plieg, FRAC * 1.5, 0.35)


def p_pecho(v, seed):
    """Frente del torso (24x30): frac con solapas de satín, camisa blanca con pechera, botones, pañuelo rojo y el prendedor del ojo."""
    p_frac(v, seed)
    yy, xx = malla(v)
    h, w = v.shape[:2]
    cx = (w - 1) / 2
    # la V abierta: camisa
    ancho_v = np.clip(6.8 - yy * 0.30, 0, 99)
    camisa = (np.abs(xx - cx) <= ancho_v) & (yy < 22)
    v[..., :3] = np.where(camisa[..., None], CAMISA * (0.93 + 0.1 * ruido(h, w, seed + 5)[..., None]), v[..., :3])
    # pechera: dos líneas de alforza
    for dx in (-2, 2):
        col = int(round(cx + dx))
        for y in range(4, 19):
            if abs(col - cx) <= ancho_v[y, col]:
                v[y, col, :3] = CAMISA * 0.82
    # botones de la camisa (negros, pequeños)
    for y in (7, 11, 15):
        v[y, int(cx), :3] = NEGRO
        v[y, int(cx) + 1, :3] = NEGRO
    # solapas de satín (peak lapel): franja junto a la V, más ancha arriba, con la punta hacia los hombros
    for y in range(0, 24):
        a = max(0.0, 6.8 - y * 0.30)
        for lado in (-1, 1):
            x0 = cx + lado * a
            anch = 3.6 if y < 14 else 3.6 - (y - 14) * 0.35
            for x in range(w):
                d = (x - x0) * lado
                if 0 < d <= anch:
                    v[y, x, :3] = SATIN * (1.06 if d < 1.2 else 0.94)
    # la muesca de la solapa
    for lado in (-1, 1):
        x = int(round(cx + lado * 9.2))
        for y in (4, 5):
            v[y, x, :3] = FRAC * 0.7
    # botones del frac
    for y in (23, 27):
        for x in (int(cx) - 1, int(cx) + 2):
            v[y, x, :3] = NEGRO * 1.3
    # pañuelo rojo en la bolsa (a la izquierda del personaje = derecha de quien mira)
    bx = int(cx + 7)
    for y in range(12, 15):
        for x in range(bx, bx + 4 - (14 - y)):
            v[y, x, :3] = ROJO_CL if (x + y) % 3 else ROJO
    for x in range(bx - 1, bx + 5):
        v[15, x, :3] = FRAC * 0.6
    # prendedor del ojo (oro con un punto rojo) en la solapa derecha del personaje
    px, py = int(cx - 8), 9
    for (x, y) in [(px, py - 1), (px - 1, py), (px + 1, py), (px, py + 1)]:
        v[y, x, :3] = ORO
    v[py, px, :3] = ROJO_CL


def p_moño(v, seed):
    llenar(v, ROJO, seed, 0.08, 2)
    yy, xx = malla(v)
    h, w = v.shape[:2]
    cx = (w - 1) / 2
    nudo = np.abs(xx - cx) <= 1.1
    mezclar(v, nudo, ROJO_CL)
    # alas del moño: un poco más oscuras en las puntas, con un pliegue
    mezclar(v, (np.abs(xx - cx) > 3.5) & ((yy == 0) | (yy == h - 1)), ROJO * 0.7)
    mezclar(v, (np.abs(xx - cx) == 3) & (yy == h // 2), ROJO * 0.75)


def p_brazo(v, seed):
    p_frac(v, seed, False)
    yy, xx = malla(v)
    h = v.shape[0]
    pliegue = (yy == int(h * 0.75)) | (yy == int(h * 0.75) + 1)
    mezclar(v, pliegue, FRAC * 0.7, 0.6)


def p_antebrazo(v, seed, cara):
    """Antebrazo (15 de largo): manga 7, puño de camisa 2 (con mancuerna dorada en el lado de afuera) y guante blanco 6."""
    h, w = v.shape[:2]
    p_frac(v, seed, False)
    v[7:9, :, :3] = CAMISA
    v[7:9, :, 3] = 255
    v[9:, :, :3] = GUANTE * (0.95 + 0.08 * ruido(h - 9, w, seed + 1)[..., None])
    # orilla del guante un poco más oscura (para que no se pierda contra la camisa)
    v[9:, 0, :3] *= 0.8
    v[9:, w - 1, :3] *= 0.8
    v[9, :, :3] *= 0.85
    # pespuntes del guante (tres líneas) en el frente
    if cara == 'frente':
        for x in (3, 6, 9):
            for y in range(10, 14):
                v[y, x, :3] = GUANTE * 0.78
        # dedos al final
        for x in range(0, w, 3):
            v[h - 1, x, :3] = GUANTE * 0.8
    if cara == 'afuera':
        v[7, w // 2, :3] = ORO
        v[8, w // 2, :3] = ORO_OSC


def p_guante_punta(v, seed):
    h, w = v.shape[:2]
    v[..., :3] = GUANTE * (0.92 + 0.06 * ruido(h, w, seed)[..., None])
    v[..., 3] = 255
    for x in range(0, w, 3):
        v[:, x, :3] = GUANTE * 0.8
    v[0, :, :3] *= 0.85
    v[h - 1, :, :3] *= 0.85
    v[:, 0, :3] *= 0.85
    v[:, w - 1, :3] *= 0.85


def p_copa(v, seed, cara):
    """Copa del sombrero (20x20 por lado): negra con un brillo y la cinta roja abajo."""
    h, w = v.shape[:2]
    llenar(v, NEGRO * 1.25, seed, 0.10, 3)
    if cara != 'arriba':
        yy, xx = malla(v)
        brillo = (np.abs(xx - w * 0.28) < 1.5)
        mezclar(v, brillo, np.array([62, 58, 72], np.float32), 0.55)
        v[h - 5:h - 1, :, :3] = ROJO * (0.92 + 0.12 * ruido(4, w, seed + 2)[..., None])
        v[h - 5, :, :3] = ROJO_CL
        v[0, :, :3] = np.array([40, 36, 46], np.float32)


def p_ala(v, seed, cara):
    h, w = v.shape[:2]
    llenar(v, NEGRO * 1.2, seed, 0.08, 3)
    if cara in ('arriba',):
        yy, xx = malla(v)
        borde = (xx <= 0) | (yy <= 0) | (xx >= w - 1) | (yy >= h - 1)
        mezclar(v, borde, np.array([44, 40, 52], np.float32), 0.8)


def p_terciopelo(v, seed, capitone=True, borde=2):
    """Terciopelo rojo del sillón, con capitonado (botones dorados en rombo y pliegues) y ribete dorado."""
    h, w = v.shape[:2]
    llenar(v, VELLUDO, seed, 0.16, 2)
    yy, xx = malla(v)
    if capitone:
        paso = 10
        a = ((xx + yy) % paso)
        b = ((xx - yy) % paso)
        pliegue = (a == 0) | (b == 0)
        mezclar(v, pliegue, VELLUDO * 0.55, 0.7)
        # el relieve entre pliegues: más claro hacia el centro de cada rombo
        da = np.minimum(a, paso - a) / (paso / 2)
        db = np.minimum(b, paso - b) / (paso / 2)
        luz = np.minimum(da, db)
        v[..., :3] *= (0.85 + 0.3 * luz)[..., None]
        boton = (a == 0) & (b == 0)
        mezclar(v, boton, ORO)
    if borde > 0:
        orilla = (xx < borde) | (xx >= w - borde) | (yy < borde) | (yy >= h - borde)
        mezclar(v, orilla, ORO)
        mezclar(v, orilla & (((xx + yy) % 4) == 0), ORO_OSC, 0.6)


def p_madera_oro(v, seed):
    h, w = v.shape[:2]
    llenar(v, ORO, seed, 0.25, 2)
    yy, xx = malla(v)
    mezclar(v, (xx % 5) == 0, ORO_OSC, 0.5)


def p_ojo_brillo(v, seed):
    v[..., :3] = np.array([255, 70, 60], np.float32)
    v[..., 3] = 255


def p_sombra(color):
    def f(v, seed, *a):
        llenar(v, color, seed, 0.06, 2)
    return f


# ---------------------------------------------------------------- el modelo
# huesos: nombre, padre, pivote (Bedrock: y hacia arriba; la base del medio cuerpo en y = 0)
HUESOS = [
    ('raiz', None, (0, 0, 0)),
    ('sillon', 'raiz', (0, 0, 10)),
    ('cuerpo', 'raiz', (0, 0, 0)),
    ('cabeza', 'cuerpo', (0, 30, 0)),
    ('sombrero', 'cabeza', (0, 54, 0)),
    ('ojos', 'cabeza', (0, 43, -12)),
    ('brazo_der', 'cuerpo', (-15, 26, 0)),
    ('antebrazo_der', 'brazo_der', (-18, 15, 0)),
    ('brazo_izq', 'cuerpo', (15, 26, 0)),
    ('antebrazo_izq', 'brazo_izq', (18, 15, 0)),
]

# cubos: hueso, origen, tamaño, pintores por cara (arriba, abajo, der, frente, izq, atras) o uno para todas
CARAS = ('arriba', 'abajo', 'der', 'frente', 'izq', 'atras')


def caras_cabeza(k, v, seed):
    if k == 'frente':
        p_cara(v, seed)
    elif k in ('arriba', 'atras'):
        p_pelo(v, seed)
    elif k == 'abajo':
        p_porc_sombra(v, seed)
    else:
        p_cabeza_lado(v, seed, k == 'der')


def caras_torso(k, v, seed):
    if k == 'frente':
        p_pecho(v, seed)
    else:
        p_frac(v, seed)


def caras_antebrazo(lado):
    def f(k, v, seed):
        if k == 'abajo':
            p_guante_punta(v, seed)
        else:
            afuera = (k == 'der' and lado == 'der') or (k == 'izq' and lado == 'izq')
            p_antebrazo(v, seed, 'afuera' if afuera else k)
    return f


CUBOS = [
    # el sillón: respaldo, orejas y copete
    ('sillon', (-30, 0, 9), (60, 66, 6), lambda k, v, s: p_terciopelo(v, s, k == 'frente', 2 if k == 'frente' else 0)),
    ('sillon', (-31, 0, -2), (7, 46, 11), lambda k, v, s: p_terciopelo(v, s, False, 1)),
    ('sillon', (24, 0, -2), (7, 46, 11), lambda k, v, s: p_terciopelo(v, s, False, 1)),
    ('sillon', (-28, 66, 10), (56, 4, 4), lambda k, v, s: p_madera_oro(v, s)),
    # el cuerpo
    ('cuerpo', (-12, 0, -6), (24, 30, 12), caras_torso),
    ('cuerpo', (-5, 25, -7.5), (10, 4, 1.5), lambda k, v, s: p_moño(v, s)),
    # la cabeza y el sombrero
    ('cabeza', (-12, 30, -12), (24, 24, 24), caras_cabeza),
    ('sombrero', (-15, 54, -15), (30, 2, 30), lambda k, v, s: p_ala(v, s, k)),
    ('sombrero', (-9, 56, -9), (18, 20, 18), lambda k, v, s: p_copa(v, s, k)),
    # los puntos rojos que brillan (encima de los ojos pintados)
    # (un poco delante de la cara: en la tele el modelo va aplanado y así no parpadean contra ella)
    ('ojos', (-6, 42.4, -14), (2, 1, 0.1), lambda k, v, s: p_ojo_brillo(v, s)),
    ('ojos', (4, 42.4, -14), (2, 1, 0.1), lambda k, v, s: p_ojo_brillo(v, s)),
    # brazos
    ('brazo_der', (-24, 14, -6), (12, 16, 12), lambda k, v, s: p_brazo(v, s)),
    ('antebrazo_der', (-24, 0, -6), (12, 15, 12), caras_antebrazo('der')),
    ('brazo_izq', (12, 14, -6), (12, 16, 12), lambda k, v, s: p_brazo(v, s)),
    ('antebrazo_izq', (12, 0, -6), (12, 15, 12), caras_antebrazo('izq')),
]


def uv_size(size):
    sx, sy, sz = [int(math.ceil(c)) for c in size]
    return 2 * (sx + sz), sz + sy


def empacar():
    """Acomoda las cajas en la textura por estantes (las más altas primero)."""
    orden = sorted(range(len(CUBOS)), key=lambda i: -uv_size(CUBOS[i][2])[1])
    pos = {}
    x = y = alto = 0
    for i in orden:
        w, h = uv_size(CUBOS[i][2])
        if x + w > TW:
            x = 0
            y += alto
            alto = 0
        if y + h > TH:
            raise SystemExit('no cabe la textura: agrandar TW/TH')
        pos[i] = (x, y)
        x += w
        alto = max(alto, h)
    return pos


def regiones(u, v, size):
    sx, sy, sz = [int(math.ceil(c)) for c in size]
    return {'arriba': (u + sz, v, sx, sz), 'abajo': (u + sz + sx, v, sx, sz), 'der': (u, v + sz, sz, sy),
            'frente': (u + sz, v + sz, sx, sy), 'izq': (u + sz + sx, v + sz, sz, sy), 'atras': (u + 2 * sz + sx, v + sz, sx, sy)}


def construir():
    pos = empacar()
    img = np.zeros((TH, TW, 4), np.float32)
    huesos = {n: {'name': n, 'pivot': list(p), 'cubes': []} | ({'parent': padre} if padre else {}) for n, padre, p in HUESOS}
    for i, (hueso, origen, tam, pintor) in enumerate(CUBOS):
        u, v = pos[i]
        huesos[hueso]['cubes'].append({'origin': list(origen), 'size': list(tam), 'uv': [u, v]})
        for j, (k, (x, y, w, h)) in enumerate(regiones(u, v, tam).items()):
            if w <= 0 or h <= 0:
                continue
            vista = img[y:y + h, x:x + w]
            pintor(k, vista, 1000 + i * 17 + j)
    geo = {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {'identifier': 'geometry.coleccionista.coleccionista_tv', 'texture_width': TW, 'texture_height': TH},
            'axelis_glow': ['ojos'],
            'bones': [huesos[n] for n, _, _ in HUESOS],
        }]}
    tex = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')
    return geo, tex


# ---------------------------------------------------------------- el disfraz (Axelis como el Coleccionista en persona)

def p_brazo_jugador(v, seed, cara, afuera):
    """Brazo entero de jugador al triple (12x36): manga del frac, puño de camisa con mancuerna y guante blanco."""
    h, w = v.shape[:2]
    if cara == 'arriba':
        p_frac(v, seed, False)
        return
    if cara == 'abajo':
        p_guante_punta(v, seed)
        return
    p_frac(v, seed, False)
    v[26:29, :, :3] = CAMISA
    v[29:, :, :3] = GUANTE * (0.95 + 0.08 * ruido(h - 29, w, seed + 1)[..., None])
    v[29:, 0, :3] *= 0.8
    v[29:, w - 1, :3] *= 0.8
    v[29, :, :3] *= 0.85
    # pliegue del codo
    v[14:16, :, :3] *= 0.75
    if afuera:
        v[27, w // 2, :3] = ORO
        v[28, w // 2, :3] = ORO_OSC
    if cara == 'frente':
        for x in (3, 6, 9):
            for y in range(30, 34):
                v[y, x, :3] = GUANTE * 0.78


def p_pierna(v, seed, cara, afuera):
    """Pierna al triple (12x36): pantalón negro con franja de satín al lado y zapato de charol."""
    h, w = v.shape[:2]
    if cara == 'abajo':
        llenar(v, NEGRO * 0.8, seed, 0.05, 2)
        return
    llenar(v, FRAC * 0.85, seed, 0.10, 3)
    if cara == 'arriba':
        return
    if afuera:
        v[:h - 6, w // 2 - 1:w // 2 + 1, :3] = SATIN * 1.1
    # el zapato: negro de charol con un brillo
    v[h - 6:, :, :3] = NEGRO * (1.1 + 0.2 * ruido(6, w, seed + 4)[..., None])
    v[h - 6, :, :3] = NEGRO * 0.6
    if cara == 'frente':
        v[h - 4, 3:6, :3] = np.array([70, 66, 80], np.float32)


def disfraz():
    """La piel del disfraz (al triple de una skin: 192x192, mismo acomodo), los ojos que brillan y el sombrero (96x48). Mismos dibujos
    que el Coleccionista de la tele, así se ve igual en persona."""
    S = 64 * 3
    img = np.zeros((S, S, 4), np.float32)

    def caja3(u, v, sx, sy, sz):
        u, v, sx, sy, sz = [a * 3 for a in (u, v, sx, sy, sz)]
        r = {'arriba': (u + sz, v, sx, sz), 'abajo': (u + sz + sx, v, sx, sz), 'der': (u, v + sz, sz, sy),
             'frente': (u + sz, v + sz, sx, sy), 'izq': (u + sz + sx, v + sz, sz, sy), 'atras': (u + 2 * sz + sx, v + sz, sx, sy)}
        return {k: img[y:y + hh, x:x + ww] for k, (x, y, ww, hh) in r.items()}
    for i, (k, v) in enumerate(caja3(0, 0, 8, 8, 8).items()):
        caras_cabeza(k, v, 3000 + i)
    for i, (k, v) in enumerate(caja3(16, 16, 8, 12, 4).items()):
        caras_torso(k, v, 3100 + i)
    for i, (k, v) in enumerate(caja3(40, 16, 4, 12, 4).items()):
        p_brazo_jugador(v, 3200 + i, k, k == 'der')
    for i, (k, v) in enumerate(caja3(32, 48, 4, 12, 4).items()):
        p_brazo_jugador(v, 3300 + i, k, k == 'izq')
    for i, (k, v) in enumerate(caja3(0, 16, 4, 12, 4).items()):
        p_pierna(v, 3400 + i, k, k == 'der')
    for i, (k, v) in enumerate(caja3(16, 48, 4, 12, 4).items()):
        p_pierna(v, 3500 + i, k, k == 'izq')
    # el moño (en la piel no hay cubo aparte): pintado arriba del pecho
    pecho = caja3(16, 16, 8, 12, 4)['frente']
    sub = pecho[0:4, 7:17]
    p_moño(sub, 3600)
    piel = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')
    # los puntos rojos de los ojos (capa que brilla en la oscuridad): en la cara, que empieza en (24, 24)
    ojos = np.zeros((S, S, 4), np.uint8)
    for x in (6, 7, 16, 17):
        for y in (10, 11):
            ojos[24 + y, 24 + x] = (255, 60, 50, 255)
    # el sombrero: la copa (33 de alto, con la cinta roja abajo y un brillo) y el ala (15)
    som = np.zeros((48, 96, 4), np.float32)
    copa = som[0:33]
    llenar(copa, NEGRO * 1.25, 3700, 0.10, 3)
    yy, xx = malla(copa)
    mezclar(copa, np.abs(xx - 27) < 2, np.array([62, 58, 72], np.float32), 0.55)
    copa[0:2, :, :3] = np.array([40, 36, 46], np.float32)
    copa[27:33, :, :3] = ROJO * (0.92 + 0.12 * ruido(6, 96, 3701)[..., None])
    copa[27, :, :3] = ROJO_CL
    ala = som[33:48]
    llenar(ala, NEGRO * 1.2, 3702, 0.08, 3)
    ala[0, :, :3] = np.array([44, 40, 52], np.float32)
    return piel, Image.fromarray(ojos, 'RGBA'), Image.fromarray(np.clip(som, 0, 255).astype(np.uint8), 'RGBA')


# ---------------------------------------------------------------- vista previa (como GeoRenderer: ModelPart, y hacia abajo)

def rot_zyx(rz, ry, rx):
    cz, sz_ = math.cos(rz), math.sin(rz)
    cy, sy_ = math.cos(ry), math.sin(ry)
    cx, sx_ = math.cos(rx), math.sin(rx)
    Rz = np.array([[cz, -sz_, 0], [sz_, cz, 0], [0, 0, 1]])
    Ry = np.array([[cy, 0, sy_], [0, 1, 0], [-sy_, 0, cy]])
    Rx = np.array([[1, 0, 0], [0, cx, -sx_], [0, sx_, cx]])
    return Rz @ Ry @ Rx


def pose(nombre, t):
    """Lo mismo que hace PantallaRenderer (en radianes, convención de ModelPart). Devuelve {hueso: (rot(x,y,z), pos(x,y,z))}."""
    P = {}

    def put(b, rx=0, ry=0, rz=0, px=0, py=0, pz=0):
        P[b] = ((rx, ry, rz), (px, py, pz))
    resp = math.sin(t * 1.7)
    put('cuerpo', rx=-0.04 + resp * 0.012, py=-resp * 0.35)
    if nombre == 'quieto':
        put('cabeza', rx=0.08 + math.sin(t * 0.53) * 0.04, ry=math.sin(t * 0.37) * 0.14, rz=0.18)
        put('brazo_der', rx=-0.55, rz=0.12)
        put('antebrazo_der', rx=-0.35, rz=-2.0)
        put('brazo_izq', rx=-0.55, rz=-0.12)
        put('antebrazo_izq', rx=-0.35, rz=2.0)
    elif nombre == 'habla':
        put('cabeza', rx=0.02 + math.sin(t * 7) * 0.05, ry=0.22, rz=-0.08)
        put('brazo_der', rx=-0.75, rz=0.35)
        put('antebrazo_der', rx=-0.2, rz=-2.75)
        put('brazo_izq', rx=-0.55, rz=-0.12)
        put('antebrazo_izq', rx=-0.35, rz=2.0)
    elif nombre == 'abre':
        put('cabeza', rx=0.0 + math.sin(t * 7) * 0.05, ry=-0.1, rz=0.05)
        put('brazo_der', rx=-0.3, rz=0.5)
        put('antebrazo_der', rx=-0.6, rz=1.0)
        put('brazo_izq', rx=-0.3, rz=-0.5)
        put('antebrazo_izq', rx=-0.6, rz=-1.0)
    elif nombre == 'risa':
        put('cabeza', rx=-0.38, ry=0.0, rz=0.1)
        put('cuerpo', rx=0.06, py=math.sin(t * 40) * 0.6)
        put('brazo_der', rx=-0.45, rz=0.25)
        put('antebrazo_der', rx=-0.3, rz=-1.75)
        put('brazo_izq', rx=-0.45, rz=-0.25)
        put('antebrazo_izq', rx=-0.3, rz=1.75)
    return P


def vista(geo, tex, P, escala=8, salida='vista.png'):
    g = geo['minecraft:geometry'][0]
    bones = g['bones']
    idx = {b['name']: i for i, b in enumerate(bones)}
    texa = np.array(tex.convert('RGBA'), np.float32)
    # matrices de cada hueso (y hacia abajo como en el juego: py = 24 - y)
    M = {}

    def piv(b):
        p = b['pivot']
        return np.array([p[0], 24 - p[1], p[2]], np.float64)

    def calc(b, padreM, padreP):
        rot, des = P.get(b['name'], ((0, 0, 0), (0, 0, 0)))
        p = piv(b)
        T = np.eye(4)
        T[:3, 3] = p - padreP + np.array(des)
        R = np.eye(4)
        R[:3, :3] = rot_zyx(rot[2], rot[1], rot[0])
        W = padreM @ T @ R
        M[b['name']] = (W, p)
        for c in bones:
            if c.get('parent') == b['name']:
                calc(c, W, p)
    for b in bones:
        if 'parent' not in b:
            calc(b, np.eye(4), np.zeros(3))
    quads = []
    for b in bones:
        W, p = M[b['name']]
        for c in b['cubes']:
            ox, oy, oz = c['origin']
            sx, sy, sz = c['size']
            x0, y0, z0 = ox, 24 - (oy + sy), oz
            x1, y1, z1 = x0 + sx, y0 + sy, z0 + sz
            u, w_ = c['uv']
            dx, dy, dz = [math.ceil(s) for s in (sx, sy, sz)]
            # como ModelPart.Cube: 8 esquinas y 6 caras con su UV de caja
            V = [(x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1), (x0, y0, z0)]
            Q = [(4, 3, 7, 0), (1, 2, 6, 5), (7, 3, 6, 2), (0, 7, 2, 1), (4, 0, 1, 5), (3, 4, 5, 6)]
            f4, f5, f6, f7, f8, f9 = u, u + dz, u + dz + dx, u + dz + 2 * dx, u + dz + dx + dz, u + 2 * dz + 2 * dx
            f10, f11, f12 = w_, w_ + dz, w_ + dz + dy
            UV = [(f5, f10, f6, f11), (f6, f11, f7, f10), (f4, f11, f5, f12), (f5, f11, f6, f12), (f6, f11, f8, f12), (f8, f11, f9, f12)]
            N = [(0, -1, 0), (0, 1, 0), (-1, 0, 0), (0, 0, -1), (1, 0, 0), (0, 0, 1)]
            for k in range(6):
                fu = UV[k]
                t = [(fu[2], fu[1]), (fu[0], fu[1]), (fu[0], fu[3]), (fu[2], fu[3])]
                pts = []
                for m in range(4):
                    q = np.array(V[Q[k][m]], np.float64) - p
                    w4 = W @ np.array([q[0], q[1], q[2], 1.0])
                    pts.append(w4[:3])
                n = W[:3, :3] @ np.array(N[k], np.float64)
                if n[2] >= 0:  # mira hacia atrás (quien mira está en -z)
                    continue
                luz = 0.55 + 0.45 * max(0.0, -n[2] * 0.8 - n[1] * 0.3 + n[0] * 0.1)
                quads.append((sum(pp[2] for pp in pts) / 4, pts, t, luz, b['name'] == 'ojos'))
    # de frente: x de pantalla = x (quien mira ve la derecha del personaje a su izquierda), y de pantalla = y del modelo (abajo)
    xs = [pp[0] for q in quads for pp in q[1]]
    ys = [pp[1] for q in quads for pp in q[1]]
    mx, Mx, my, My = min(xs) - 4, max(xs) + 4, min(ys) - 4, max(ys) + 4
    W_, H_ = int((Mx - mx) * escala), int((My - my) * escala)
    out = np.zeros((H_, W_, 3), np.float32)
    out[...] = np.array([40, 10, 16], np.float32)
    zbuf = np.full((H_, W_), 1e9)
    for zc, pts, t, luz, brilla in quads:
        P0 = np.array([(pts[0][0] - mx) * escala, (pts[0][1] - my) * escala])
        P1 = np.array([(pts[1][0] - mx) * escala, (pts[1][1] - my) * escala])
        P3 = np.array([(pts[3][0] - mx) * escala, (pts[3][1] - my) * escala])
        e1, e2 = P1 - P0, P3 - P0
        det = e1[0] * e2[1] - e1[1] * e2[0]
        if abs(det) < 1e-6:
            continue
        cs = np.array([P0, P1, P3, P1 + e2])
        x0, x1 = int(max(0, cs[:, 0].min())), int(min(W_ - 1, cs[:, 0].max()) + 1)
        y0, y1 = int(max(0, cs[:, 1].min())), int(min(H_ - 1, cs[:, 1].max()) + 1)
        if x1 <= x0 or y1 <= y0:
            continue
        yy, xx = np.mgrid[y0:y1, x0:x1] + 0.5
        rx, ry = xx - P0[0], yy - P0[1]
        a = (rx * e2[1] - ry * e2[0]) / det
        bb = (e1[0] * ry - e1[1] * rx) / det
        ok = (a >= 0) & (a <= 1) & (bb >= 0) & (bb <= 1)
        z = np.array([pp[2] for pp in pts])
        zz = z[0] + a * (z[1] - z[0]) + bb * (z[3] - z[0])
        uu = t[0][0] + a * (t[1][0] - t[0][0]) + bb * (t[3][0] - t[0][0])
        vv = t[0][1] + a * (t[1][1] - t[0][1]) + bb * (t[3][1] - t[0][1])
        ui = np.clip(uu.astype(int), 0, TW - 1)
        vi = np.clip(vv.astype(int), 0, TH - 1)
        col = texa[vi, ui]
        ok &= col[..., 3] > 0
        ok &= zz < zbuf[y0:y1, x0:x1]
        sub = out[y0:y1, x0:x1]
        c = col[..., :3] * (1.0 if brilla else luz)
        sub[ok] = c[ok]
        zbuf[y0:y1, x0:x1][ok] = zz[ok]
    Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)).save(salida)
    return salida


def main():
    args = sys.argv[1:]
    geo, tex = construir()
    os.makedirs(os.path.join(ASSETS, 'geo'), exist_ok=True)
    with open(os.path.join(ASSETS, 'geo', 'coleccionista_tv.geo.json'), 'w', encoding='utf-8') as f:
        json.dump(geo, f, ensure_ascii=False, separators=(',', ':'))
    tex.save(os.path.join(ASSETS, 'textures', 'entity', 'coleccionista_tv.png'), optimize=True)
    piel, ojos, som = disfraz()
    piel.save(os.path.join(ASSETS, 'textures', 'entity', 'coleccionista.png'), optimize=True)
    ojos.save(os.path.join(ASSETS, 'textures', 'entity', 'coleccionista_ojos.png'), optimize=True)
    som.save(os.path.join(ASSETS, 'textures', 'entity', 'sombrero.png'), optimize=True)
    print('modelo y texturas escritos (tele y disfraz)')
    if '--vista' in args:
        cual = args[args.index('--vista') + 1] if args.index('--vista') + 1 < len(args) else 'quieto'
        carpeta = args[args.index('--salida') + 1] if '--salida' in args else '.'
        for n in (['quieto', 'habla', 'abre', 'risa'] if cual == 'todas' else [cual]):
            print(vista(geo, tex, pose(n, 1.0), 8, os.path.join(carpeta, f'vista_coleccionista_tv_{n}.png')))


if __name__ == '__main__':
    main()
