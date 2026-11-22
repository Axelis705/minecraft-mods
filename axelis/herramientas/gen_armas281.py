#!/usr/bin/env python3
"""Etapa 281: arte de las armas nuevas (5 tipos x 8 tiers = 40 armas).

Dibuja cada arma en pixel art de 32x32, en diagonal (mango abajo a la izquierda, punta arriba a la derecha), con
contorno oscuro, luz desde arriba a la izquierda, un brillo que recorre el filo y, en los tiers con energía (lava,
runas, rayos, estrellas), un pulso de luz. Cada textura tiene 16 fotogramas (32x512) con su .mcmeta.

Uso:
  python3 gen_armas281.py <carpeta resources>        escribe texturas, modelos y la hoja de muestra
  python3 gen_armas281.py --hoja <salida.png>        solo la hoja de muestra (fotograma 0, ampliada)
"""
import json
import math
import os
import sys

from PIL import Image

R2 = math.sqrt(2.0)
TIERS = ["bosque", "forja", "hielo", "magma", "magia", "dragon", "cielo", "abismo"]
TIPOS = ["espada", "hacha", "martillo", "lanza", "dagas"]
N_FRAMES = 16


def ramp(*hexes):
    return [tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) for h in hexes]


# Paletas por tier: cada material es una rampa de oscuro a claro.
PAL = {
    "bosque": {
        "hoja": ramp("#24361f", "#3f5c35", "#62874f", "#8fb57a", "#c6e3ad", "#eefae0"),
        "mango": ramp("#2c1a0e", "#4a2e17", "#6e4624", "#946233", "#b98444"),
        "asta": ramp("#2c1a0e", "#4a2e17", "#6e4624", "#946233", "#b98444"),
        "adorno": ramp("#173a14", "#245e1f", "#3a8a2e", "#5fb84a", "#9be27a"),
        "gema": ramp("#5a1020", "#a8243a", "#e0505e", "#ffa0a0"),
        "brillo": ramp("#2f6a1c", "#5fae34", "#9be05a", "#d8ff9a"),
        "tela": ramp("#173a14", "#2c6a24", "#4ea03c", "#86d064"),
    },
    "forja": {
        "hoja": ramp("#23272d", "#3d444e", "#5f6a77", "#8995a2", "#b9c3cd", "#eef3f7"),
        "mango": ramp("#1e130d", "#3a2619", "#573a26", "#775236", "#946a48"),
        "asta": ramp("#1e130d", "#3a2619", "#573a26", "#775236", "#946a48"),
        "adorno": ramp("#4a2a0c", "#7a4a18", "#b07428", "#dca448", "#ffd890"),
        "gema": ramp("#6a2000", "#c04a10", "#ff8a2a", "#ffd090"),
        "brillo": ramp("#6a2000", "#c04a10", "#ff8a2a", "#ffd890"),
        "tela": ramp("#4a1010", "#7a1c1c", "#a83030", "#d05050"),
    },
    "hielo": {
        "hoja": ramp("#16314e", "#2a5d8c", "#4f8fc4", "#86c4ec", "#c4ecff", "#ffffff"),
        "mango": ramp("#0f1a30", "#1d3156", "#2d4c80", "#4268a6", "#5c86c4"),
        "asta": ramp("#2c3e52", "#4a6078", "#6e88a2", "#9ab2c8", "#c8dcec"),
        "adorno": ramp("#3a5068", "#5c7690", "#86a2bc", "#b6cee2", "#e8f6ff"),
        "gema": ramp("#1a6a8a", "#30b0d8", "#7ef0ff", "#e8ffff"),
        "brillo": ramp("#2a8ab0", "#50d0f0", "#a0f6ff", "#ffffff"),
        "tela": ramp("#1d3156", "#2d4c80", "#4f8fc4", "#9fd4f4"),
    },
    "magma": {
        "hoja": ramp("#0e080c", "#1e131a", "#33232c", "#4d3843", "#6c5260", "#8e7482"),
        "mango": ramp("#120a08", "#2a1610", "#422418", "#5c3422", "#784630"),
        "asta": ramp("#120a08", "#241814", "#3a2a24", "#544038", "#6e5850"),
        "adorno": ramp("#1c1414", "#30262a", "#4a3c40", "#665658", "#887478"),
        "gema": ramp("#8a1a00", "#e04a00", "#ff9a1a", "#ffe46a"),
        "brillo": ramp("#7a1400", "#d03c00", "#ff7a10", "#ffbe3a", "#fff08a"),
        "tela": ramp("#4a0c00", "#8a1c00", "#c43a08", "#f06a1a"),
    },
    "magia": {
        "hoja": ramp("#1e0f38", "#36205e", "#553a8e", "#7c5cc0", "#aa8ce6", "#e2d4ff"),
        "mango": ramp("#160b26", "#2a1748", "#40246c", "#58348e", "#7048ac"),
        "asta": ramp("#160b26", "#2a1748", "#40246c", "#58348e", "#7048ac"),
        "adorno": ramp("#4e3408", "#86601a", "#c0902c", "#e8c050", "#fff0a8"),
        "gema": ramp("#6a1080", "#c030d0", "#ff78f0", "#ffd8ff"),
        "brillo": ramp("#3a3aa0", "#5a8af0", "#8adcff", "#e0ffff"),
        "tela": ramp("#3a0e5a", "#62188e", "#9030c0", "#c068f0"),
    },
    "dragon": {
        "hoja": ramp("#2a0410", "#520a1e", "#8a1430", "#c42a46", "#ee5c72", "#ffb4be"),
        "mango": ramp("#100808", "#22121a", "#341e26", "#4a2c36", "#603c48"),
        "asta": ramp("#100808", "#22121a", "#341e26", "#4a2c36", "#603c48"),
        "adorno": ramp("#4e3408", "#86601a", "#c0902c", "#e8c050", "#fff0a8"),
        "gema": ramp("#8a3a00", "#e07a10", "#ffc040", "#fff0a0"),
        "brillo": ramp("#a02a00", "#e05a10", "#ffa030", "#ffe080"),
        "tela": ramp("#3a0410", "#6a0a20", "#a01a36", "#d03a50"),
    },
    "cielo": {
        "hoja": ramp("#2c3a58", "#4a5e88", "#7690bc", "#a8c0e4", "#dae8ff", "#ffffff"),
        "mango": ramp("#121c40", "#1e2e66", "#2c4492", "#4060b6", "#5a7ed4"),
        "asta": ramp("#e8e0c8", "#f4ecd8", "#fff8ea", "#ffffff", "#ffffff"),
        "adorno": ramp("#4e3408", "#86601a", "#c0902c", "#e8c050", "#fff0a8"),
        "gema": ramp("#2050c0", "#3a8aff", "#8ad0ff", "#f0ffff"),
        "brillo": ramp("#a08a10", "#e0c820", "#fff26a", "#ffffff"),
        "tela": ramp("#8a90a8", "#c0c8dc", "#e6ecf8", "#ffffff"),
    },
    "abismo": {
        "hoja": ramp("#07040e", "#120a24", "#21143e", "#341f5e", "#4c3084", "#6e4ab0"),
        "mango": ramp("#050308", "#100a1a", "#1c122c", "#2a1c40", "#3a2856"),
        "asta": ramp("#050308", "#100a1a", "#1c122c", "#2a1c40", "#3a2856"),
        "adorno": ramp("#1c3a54", "#2e6084", "#4a90b4", "#7ac8e0", "#c0f6ff"),
        "gema": ramp("#20809a", "#40c8e0", "#8cf6ff", "#ffffff"),
        "brillo": ramp("#3a8aa8", "#5ad0ec", "#9cf6ff", "#ffffff"),
        "estrella": ramp("#ff7af0", "#8cf6ff", "#ffffff"),
        "tela": ramp("#2a0e48", "#4a1c78", "#7a34b0", "#b070f0"),
    },
}
# los tiers cuyo brillo late (lava, runas, rayos, estrellas)
PULSA = {"magma", "magia", "cielo", "abismo", "hielo", "dragon"}


def clamp(x, a, b):
    return a if x < a else b if x > b else x


def ss(e0, e1, x):
    t = clamp((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3 - 2 * t)


class Lienzo:
    """Rejilla de 32x32: cada píxel guarda (material, tono) o None."""

    def __init__(self, n=32):
        self.n = n
        self.px = [[None] * n for _ in range(n)]
        self.u = [[None] * n for _ in range(n)]

    def put(self, x, y, mat, idx, u=None, over=True):
        if 0 <= x < self.n and 0 <= y < self.n:
            if over or self.px[y][x] is None:
                self.px[y][x] = (mat, idx)
                self.u[y][x] = u


class Eje:
    """Ejes del arma: u a lo largo (del pomo a la punta), v de lado (negativo = lado iluminado, arriba a la izquierda)."""

    def __init__(self, x0, y0, dx, dy):
        self.x0, self.y0 = x0, y0
        ln = math.hypot(dx, dy)
        self.dx, self.dy = dx / ln, dy / ln
        # normal: girar la dirección 90° hacia abajo a la derecha
        self.nx, self.ny = -self.dy, self.dx
        if self.nx + self.ny < 0:
            self.nx, self.ny = -self.nx, -self.ny

    def uv(self, x, y):
        px, py = x + 0.5 - self.x0, y + 0.5 - self.y0
        return px * self.dx + py * self.dy, px * self.nx + py * self.ny


def tono_lado(t, base=2, top=4):
    """Tono de una superficie según el lado (t de -1 lado iluminado a +1 lado en sombra)."""
    if t < -0.55:
        return top
    if t < -0.05:
        return base + 1
    if t < 0.5:
        return base
    return base - 1


def tono_filo(t):
    """Hoja con arista: filo de la luz muy claro, arista central clara, lado en sombra oscuro."""
    if t < -0.58:
        return 5
    if t < -0.18:
        return 3
    if t < 0.18:
        return 4
    if t < 0.58:
        return 2
    return 1


# ------------------------------------------------------------------ piezas comunes

def pomo(u, v, uc, r, tier):
    d = math.hypot(u - uc, v)
    if d <= r:
        if d <= r * 0.45 and tier not in ("bosque",):
            return ("gema", 2 if v < 0 else 1)
        return ("adorno", 3 if v < -0.3 else 2 if v < 0.4 else 1)
    return None


def empuñadura(u, v, u0, u1, hw, mat="mango"):
    if u0 <= u <= u1 and abs(v) <= hw:
        banda = int((u - u0) / 1.45) % 2
        t = v / hw
        idx = 2 + banda
        if t > 0.4:
            idx -= 1
        elif t < -0.4:
            idx += 1
        return (mat, clamp(idx, 0, 4))
    return None


def asta(u, v, u0, u1, hw, mat="asta"):
    if u0 <= u <= u1 and abs(v) <= hw:
        t = v / max(hw, 0.01)
        return (mat, 3 if t < -0.3 else 2 if t < 0.35 else 1)
    return None


# ------------------------------------------------------------------ ESPADA

def espada(tier, u, v):
    L0, L1 = 12.6, 38.0
    # hoja: perfil de media anchura por tier
    s = (u - L0) / (L1 - L0)
    vc = 0.0
    if tier == "bosque":
        hw = 1.15 + 1.35 * math.sin(math.pi * clamp(s * 0.95 + 0.04, 0, 1)) ** 0.9
    elif tier == "forja":
        hw = 2.05 if s < 0.78 else 2.05 * (1 - (s - 0.78) / 0.22) ** 0.85
    elif tier == "hielo":
        dientes = 0.55 if (u * 0.9) % 2.0 < 1.0 else 0.0
        hw = (1.9 + dientes) * (1 if s < 0.7 else (1 - (s - 0.7) / 0.3) ** 0.9)
    elif tier == "magma":
        hw = (2.35 + 0.45 * math.sin(u * 1.05)) * (1 if s < 0.72 else (1 - (s - 0.72) / 0.28) ** 0.8)
    elif tier == "magia":
        hw = 1.6 * (1 if s < 0.8 else (1 - (s - 0.8) / 0.2) ** 0.9) + (0.6 if 0.35 < s < 0.45 else 0)
    elif tier == "dragon":
        vc = 1.1 * s * s  # hoja curva (cimitarra)
        hw = (1.7 + 0.9 * s) * (1 if s < 0.8 else (1 - (s - 0.8) / 0.2) ** 0.7)
    elif tier == "cielo":
        hw = 1.85 * (1 if s < 0.74 else (1 - (s - 0.74) / 0.26) ** 0.9)
    else:  # abismo
        hw = 2.45 * (1 if s < 0.7 else (1 - (s - 0.7) / 0.3) ** 0.75)
    if L0 <= u <= L1 and hw > 0.2:
        vv = v - vc
        if abs(vv) <= hw:
            t = vv / hw
            # dentado en el lomo del dragón
            if tier == "dragon" and t > 0.55 and (u * 1.1) % 2.2 < 0.9:
                return None
            if tier == "bosque" and abs(vv) < 0.36 and s < 0.85:
                return ("adorno", 1)  # nervio de la hoja
            if tier == "forja" and abs(vv) < 0.36 and s < 0.7:
                return ("hoja", 1)  # acanaladura
            if tier == "magma" and abs(vv) < 0.5 and s < 0.75 and int(u * 1.4) % 3 != 0:
                return ("brillo", 2)  # grieta de lava
            if tier == "magia" and abs(vv) < 0.36 and 0.08 < s < 0.75 and int(u * 1.3) % 3 == 0:
                return ("brillo", 3)  # runas
            if tier == "abismo":
                if t < -0.62 or t > 0.82:
                    return ("adorno", 3 if t < 0 else 2)  # filo celeste
                h = (int(u * 7.3) * 131 + int((vv + 3) * 5.1) * 71) % 23
                if h == 0 and abs(vv) < hw - 0.6:
                    return ("estrella", 2)
                if h == 7 and abs(vv) < hw - 0.6:
                    return ("estrella", 1)
            if tier == "cielo" and abs(vv) < 0.36 and s < 0.72:
                return ("brillo", 2)
            if tier == "hielo" and abs(vv) < 0.36 and s < 0.65:
                return ("hoja", 5)
            return ("hoja", tono_filo(t))
    # guarda
    g = None
    du = u - 11.4
    if tier == "bosque":
        # dos hojas a los lados de la guarda
        for sg in (-1, 1):
            hv = v * sg
            if 1.0 <= hv <= 5.2:
                k = (hv - 1.0) / 4.2
                w = 1.35 * math.sin(math.pi * k)
                if abs(du - 0.8 * k) <= w:
                    g = ("adorno", 3 if sg < 0 else 2)
        if abs(du) <= 0.9 and abs(v) <= 1.6:
            g = ("mango", 3)
    elif tier == "forja":
        if abs(du) <= 0.75 and abs(v) <= 4.6:
            g = ("adorno", 3 if v < 0 else 2)
            if abs(v) >= 3.9:
                g = ("adorno", 4)
    elif tier == "hielo":
        for sg in (-1, 1):
            hv = v * sg
            if 0.8 <= hv <= 5.4 and abs(du - 0.6 * (hv - 0.8)) <= 0.85 * (1 - (hv - 0.8) / 5.0) + 0.3:
                g = ("adorno", 3 if sg < 0 else 2)
        if abs(du) <= 0.8 and abs(v) <= 1.4:
            g = ("gema", 2)
    elif tier == "magma":
        for sg in (-1, 1):
            hv = v * sg
            if 0.8 <= hv <= 4.8 and abs(du + 0.5 * (hv - 0.8) ** 1.3 * 0.5) <= 0.95:
                g = ("adorno", 3 if sg < 0 else 2)
        if abs(du) <= 0.9 and abs(v) <= 1.2:
            g = ("brillo", 3)
    elif tier == "magia":
        d = math.hypot(du, v)
        if 2.1 <= d <= 3.1:
            g = ("adorno", 3 if v < 0 else 2)
        if abs(du) <= 0.7 and abs(v) <= 3.8:
            g = ("adorno", 3 if v < 0 else 2)
        if d <= 1.0:
            g = ("gema", 2)
    elif tier == "dragon":
        for sg in (-1, 1):
            hv = v * sg
            # cuernos curvados hacia la punta
            if 0.8 <= hv <= 5.6:
                cu = 0.28 * (hv - 0.8) ** 2
                if abs(du - cu) <= 1.0 * (1.2 - (hv - 0.8) / 5.2):
                    g = ("adorno", 3 if sg < 0 else 2)
        if math.hypot(du, v) <= 1.1:
            g = ("gema", 2)
    elif tier == "cielo":
        for sg in (-1, 1):
            hv = v * sg
            # alas: plumas que suben hacia la punta
            if 0.8 <= hv <= 6.0:
                k = (hv - 0.8) / 5.2
                top = 0.9 + 3.6 * k
                if -0.8 <= du <= top - 2.2 * k * k and du >= -0.8 + 1.6 * k * k:
                    pl = int((du + hv) * 1.2) % 2
                    g = ("asta", 3 - pl if sg < 0 else 2 - pl)
        if math.hypot(du, v) <= 1.0:
            g = ("gema", 2)
    else:  # abismo
        for sg in (-1, 1):
            hv = v * sg
            if 0.8 <= hv <= 4.4 and abs(du) <= 0.75 + 0.35 * (hv - 0.8):
                g = ("adorno", 3 if sg < 0 else 2) if hv < 3.6 else ("brillo", 3)
        if math.hypot(du, v) <= 1.2:
            g = ("gema", 2)
    if g:
        return g
    r = empuñadura(u, v, 2.2, 10.6, 0.75)
    if r:
        return r
    return pomo(u, v, 1.25, 1.55, tier)


# ------------------------------------------------------------------ HACHA DE GUERRA

def hacha(tier, u, v):
    uc, H = 30.4, 7.4
    doble = tier in ("magma", "magia", "cielo", "abismo")
    E = {"bosque": 7.0, "forja": 7.6, "hielo": 7.6, "magma": 7.0, "magia": 6.8, "dragon": 8.6, "cielo": 7.0, "abismo": 7.4}[tier]
    # punta de arriba
    if 36.6 <= u <= 39.6 and abs(v) <= 0.8 * (1 - (u - 36.6) / 3.2) + 0.2:
        return ("adorno", 3 if v < 0 else 2)
    for sg in ((-1, 1) if doble else (-1,)):
        e = E if sg < 0 else E * 0.8
        hv = -v if sg < 0 else v
        if hv < 0.7:
            continue
        k = (u - uc) / H
        if abs(k) > 1:
            continue
        # media luna: borde exterior en arco, borde interior cóncavo (la hoja solo toca el mango en el centro)
        fuera = e * math.sqrt(max(0.0, 1 - 0.86 * k * k))
        dentro = 0.75 + (e - 2.0) * k * k * (1.15 if tier != "dragon" else 0.9)
        if dentro <= hv <= fuera:
            if hv > fuera - 1.0:
                return ("hoja", 5 if sg < 0 else 4)  # filo
            if tier == "magma" and abs(u - uc) < 0.45 + 0.12 * hv and hv > 1.4:
                return ("brillo", 2)
            if tier == "magia" and 2.0 < hv < 3.4 and abs(u - uc) < 0.9:
                return ("gema", 2)
            if tier == "abismo":
                hsh = (int(u * 6.1) * 97 + int(hv * 4.3) * 53) % 17
                if hsh == 0:
                    return ("estrella", 2)
                if hv > fuera - 1.8:
                    return ("adorno", 3 if sg < 0 else 2)
            if tier == "bosque" and abs(u - uc) < 0.4 and hv < fuera - 1.2:
                return ("adorno", 1)
            if tier == "hielo" and (int(u * 1.3 + hv) % 3 == 0):
                return ("hoja", 4)
            if tier == "cielo" and abs(hv - fuera + 2.2) < 0.4:
                return ("adorno", 3)
            if tier == "dragon" and hv < 2.4 and int(u * 1.5) % 2 == 0:
                return ("adorno", 2)
            sh = 3 if (k < -0.25) else 2 if k < 0.4 else 1
            if sg > 0:
                sh -= 1
            return ("hoja", clamp(sh, 0, 5))
    # lomo del hacha simple: martillo (forja), púa (los demás)
    if not doble and v > 0.6:
        du = u - uc
        if tier == "forja" and abs(du) <= 1.6 and v <= 3.0:
            return ("adorno", 3 if du < 0 else 2)
        if abs(du) <= 1.4 * (1 - (v - 0.6) / 3.6) and v <= 4.2:
            return ("adorno" if tier != "bosque" else "hoja", 3 if du < 0 else 2)
    # collar donde se une la cabeza
    if abs(u - uc) <= 2.0 and abs(v) <= 1.15:
        return ("adorno", 3 if v < 0 else 2)
    r = empuñadura(u, v, 2.0, 9.8, 0.75)
    if r:
        return r
    if 0.0 <= u <= 1.7 and abs(v) <= 1.0:
        return ("adorno", 3 if v < 0 else 2)
    return asta(u, v, 0, 36.8, 0.75)


# ------------------------------------------------------------------ MARTILLO

def martillo(tier, u, v):
    uc = 31.6
    A = 3.9  # media longitud de la cabeza a lo largo del eje
    B = {"bosque": 6.6, "forja": 7.4, "hielo": 7.0, "magma": 7.4, "magia": 7.0, "dragon": 7.2, "cielo": 6.8, "abismo": 7.2}[tier]
    du = u - uc
    av = abs(v)
    dentro = abs(du) <= A and av <= B
    if tier in ("forja", "magma", "dragon"):
        # caras más anchas (como un yunque)
        dentro = dentro or (abs(du) <= A + 0.9 and B - 1.7 <= av <= B)
    if tier == "bosque":
        # tronco redondeado
        dentro = abs(du) <= A and av <= B - 0.6 * (du / A) ** 4
    if tier == "hielo":
        dentro = dentro or (av <= B + 1.8 and abs(du) <= 1.1 * (1 - (av - B) / 1.9) and av > B)
    # bisel en las esquinas
    if dentro and abs(du) + av > A + B - 1.2 and tier not in ("hielo",):
        dentro = False
    if dentro:
        tu = du / A
        if av > B - 1.0:
            # caras de golpe
            return ("adorno" if tier != "bosque" else "asta", 3 if du < 0 else 2)
        if tier == "bosque":
            if int((av) * 1.0) % 3 == 0 and abs(du) < A - 0.8:
                return ("asta", 1)
            if du < -A + 1.0 or (2.5 < av < 3.6 and du > 0.5):
                return ("adorno", 3 if du < 0 else 2)  # musgo
            return ("asta", 3 if tu < -0.3 else 2)
        if tier == "magma" and (abs(av - 2.6) < 0.45 or abs(du - 0.6 * (av - 3.0)) < 0.45) and abs(du) < A - 0.8:
            return ("brillo", 2)
        if tier == "magia" and av < 1.6 and abs(du) < A - 0.8:
            return ("gema", 2 if du < 0 else 1)
        if tier == "magia" and abs(av - 3.2) < 0.4:
            return ("brillo", 3)
        if tier == "abismo":
            if av < 1.3 and abs(du) < 1.3:
                return ("gema", 3)
            hsh = (int(du * 5.3 + 9) * 89 + int(av * 4.7) * 61) % 13
            if hsh == 0 and abs(du) < A - 0.8:
                return ("estrella", 2)
        if tier == "dragon" and av < 1.3 and abs(du) < 1.3:
            return ("gema", 2)
        if tier == "cielo" and av < 1.0 and abs(du) < A - 0.6:
            return ("brillo", 2)
        if tier == "forja" and abs(du) < A - 0.8 and (abs(av - 2.4) < 0.4) and int(du * 1.6) % 2 == 0:
            return ("adorno", 4)  # remaches
        if tier == "hielo" and int(du * 1.2 + av) % 4 == 0:
            return ("hoja", 5)
        sh = 4 if tu < -0.55 else 3 if tu < 0.0 else 2 if tu < 0.55 else 1
        return ("hoja", sh)
    # alas (cielo) a los lados del mango, bajo la cabeza
    if tier == "cielo":
        for sg in (-1, 1):
            hv = v * sg
            if 1.0 <= hv <= 5.0 and uc - A - 3.2 + 0.6 * hv <= u <= uc - A + 0.2:
                pl = int((u + hv) * 1.3) % 2
                return ("asta", 3 - pl if sg < 0 else 2 - pl)
    if tier == "dragon":
        for sg in (-1, 1):
            hv = v * sg
            # cuernos que salen de la cabeza hacia la punta
            if 1.6 <= hv <= 3.6 and uc + A - 0.2 <= u <= uc + A + 2.8 - 0.8 * (hv - 1.6) ** 1.2:
                return ("adorno", 3 if sg < 0 else 2)
    if abs(u - (uc - A - 0.7)) <= 0.8 and abs(v) <= 1.2:
        return ("adorno", 3 if v < 0 else 2)
    r = empuñadura(u, v, 2.0, 10.5, 0.75)
    if r:
        return r
    if 0.0 <= u <= 1.8 and abs(v) <= 1.05:
        return ("adorno", 3 if v < 0 else 2)
    return asta(u, v, 0, uc - A, 0.75)


# ------------------------------------------------------------------ LANZA

def lanza(tier, u, v):
    H0, H1 = 30.6, 39.6
    s = (u - H0) / (H1 - H0)
    if tier == "hielo":
        hw = 2.3 * (1 - s) ** 1.0 if s > 0.15 else 2.3 * (s / 0.15) ** 0.6
    elif tier == "dragon":
        hw = 2.2 * math.sin(math.pi * clamp(s, 0, 1)) ** 0.6 + (1.0 if s < 0.25 else 0)
    elif tier == "magia":
        hw = 1.8 * math.sin(math.pi * clamp(s, 0, 1)) ** 0.8
    else:
        hw = 2.1 * math.sin(math.pi * clamp(s * 0.92 + 0.02, 0, 1)) ** 0.7
    if 0 <= s <= 1 and abs(v) <= hw:
        t = v / max(hw, 0.1)
        if abs(v) < 0.36 and s < 0.7:
            return ("hoja" if tier not in ("magma", "magia", "cielo", "abismo") else "brillo", 1 if tier not in ("magma", "magia", "cielo", "abismo") else 2)
        if tier == "abismo" and abs(t) > 0.7:
            return ("adorno", 3 if t < 0 else 2)
        return ("hoja", tono_filo(t))
    # alas, garfios y adornos bajo la punta
    du = u - H0
    if tier == "cielo":
        for sg in (-1, 1):
            hv = v * sg
            if 0.6 <= hv <= 4.6 and -2.6 + 0.35 * hv <= du <= 0.4 + 0.5 * hv - 0.12 * hv * hv:
                pl = int((du + hv) * 1.3) % 2
                return ("asta", 3 - pl if sg < 0 else 2 - pl)
    if tier == "magia":
        d = math.hypot(du + 0.2, v)
        if 1.9 <= d <= 2.8 and du < 0.9:
            return ("adorno", 3 if v < 0 else 2)
    if tier in ("forja", "bosque", "magma", "abismo"):
        for sg in (-1, 1):
            hv = v * sg
            if 0.5 <= hv <= 2.8 and abs(du + 0.6) <= 0.7 - 0.12 * hv:
                return ("adorno", 3 if sg < 0 else 2)
    if -1.6 <= du <= 0.2 and abs(v) <= 1.1:
        return ("adorno", 3 if v < 0 else 2)
    # borla: cuelga del collar hacia abajo (en la pantalla), sin tocar el asta
    a = ((28.6 - u) + (v - 0.5)) / R2
    b = ((28.6 - u) - (v - 0.5)) / R2
    if v > 0.5 and 0 <= a <= 5.0 and abs(b) <= 0.55 + 0.22 * a:
        return ("tela", 3 if a < 1.2 else 2 if int(a * 1.4 + b) % 2 == 0 else 1)
    r = empuñadura(u, v, 11.0, 17.0, 0.75)
    if r:
        return r
    if 0 <= u <= 2.0 and abs(v) <= 0.75 * (u / 2.0) + 0.2:
        return ("adorno", 3 if v < 0 else 2)
    return asta(u, v, 0, H0, 0.42)


# ------------------------------------------------------------------ DAGAS

def daga(tier, u, v, L=24.0):
    B0 = 8.2
    s = (u - B0) / (L - B0)
    vc = 0.0
    if tier == "dragon":
        vc = 0.9 * s * s
    if tier == "magma":
        hw = 1.6 + 0.35 * math.sin(u * 1.3)
    elif tier == "bosque":
        hw = 0.9 + 1.0 * math.sin(math.pi * clamp(s * 0.9 + 0.05, 0, 1))
    elif tier == "hielo":
        hw = 1.6 + (0.45 if (u * 1.0) % 2.0 < 1.0 else 0)
    elif tier == "abismo":
        hw = 1.85
    else:
        hw = 1.5
    if s > 0.62:
        hw *= (1 - (s - 0.62) / 0.38) ** 0.85
    if 0 <= s <= 1 and abs(v - vc) <= hw and hw > 0.15:
        vv = v - vc
        t = vv / hw
        if tier == "abismo" and abs(t) > 0.7:
            return ("adorno", 3 if t < 0 else 2)
        if tier in ("magma", "magia") and abs(vv) < 0.36 and s < 0.6:
            return ("brillo", 2)
        if tier == "dragon" and t > 0.55 and (u * 1.2) % 2.4 < 1.0:
            return None
        return ("hoja", tono_filo(t))
    du = u - 7.4
    if tier == "cielo":
        for sg in (-1, 1):
            hv = v * sg
            if 0.6 <= hv <= 3.4 and -0.6 <= du <= 0.6 + 0.9 * (hv - 0.6):
                return ("asta", 3 if sg < 0 else 2)
    if tier == "dragon":
        for sg in (-1, 1):
            hv = v * sg
            if 0.6 <= hv <= 3.0 and abs(du - 0.22 * (hv - 0.6) ** 2) <= 0.7:
                return ("adorno", 3 if sg < 0 else 2)
    if abs(du) <= 0.7 and abs(v) <= {"bosque": 2.2, "magia": 2.6, "hielo": 2.8}.get(tier, 2.4):
        return ("adorno", 3 if v < 0 else 2) if tier != "bosque" else ("adorno", 3 if v < 0 else 2)
    r = empuñadura(u, v, 1.8, 6.8, 0.75)
    if r:
        return r
    return pomo(u, v, 0.9, 1.25, tier)


# ------------------------------------------------------------------ dibujo

def dibujar(tipo, tier, mano=False):
    lz = Lienzo()
    if tipo == "dagas" and not mano:
        ejes = [(Eje(27.5, 28.5, -1, -1), 23.0), (Eje(4.5, 28.5, 1, -1), 23.0)]
        for ej, L in ejes:
            capa = Lienzo()
            for y in range(32):
                for x in range(32):
                    u, v = ej.uv(x, y)
                    # en la daga de la izquierda (que va al revés) el lado iluminado cambia
                    r = daga(tier, u, v if ej.dx > 0 else -v, L)
                    if r:
                        capa.put(x, y, r[0], r[1], u)
            contorno(capa, tier)
            for y in range(32):
                for x in range(32):
                    if capa.px[y][x] is not None:
                        lz.px[y][x] = capa.px[y][x]
                        lz.u[y][x] = capa.u[y][x]
        return lz
    if tipo == "dagas":
        ej = Eje(3.5, 28.5, 1, -1)
        f = lambda u, v: daga(tier, u, v, 31.0)
    else:
        ej = Eje(2.0, 30.0, 1, -1)
        f = {"espada": espada, "hacha": hacha, "martillo": martillo, "lanza": lanza}[tipo]
        f = (lambda g: (lambda u, v: g(tier, u, v)))(f)
    for y in range(32):
        for x in range(32):
            u, v = ej.uv(x, y)
            r = f(u, v)
            if r:
                lz.put(x, y, r[0], r[1], u)
    contorno(lz, tier)
    return lz


def oscuro(c, k=0.45):
    return tuple(int(x * k) for x in c)


def contorno(lz, tier):
    pal = PAL[tier]
    n = lz.n
    nuevos = []
    for y in range(n):
        for x in range(n):
            if lz.px[y][x] is not None:
                continue
            vec = None
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < n and 0 <= yy < n and lz.px[yy][xx] is not None and lz.px[yy][xx][0] != "@":
                    vec = lz.px[yy][xx]
                    break
            if vec:
                nuevos.append((x, y, vec[0]))
    for x, y, mat in nuevos:
        lz.px[y][x] = ("@", mat)


def color(tier, mat, idx, frame):
    pal = PAL[tier]
    if mat == "@":
        base = pal.get(idx, pal["hoja"])[0]
        return oscuro(base, 0.55) + (255,)
    rp = pal.get(mat, pal["hoja"])
    if mat in ("brillo", "estrella", "gema") and tier in PULSA:
        p = 0.5 + 0.5 * math.sin(2 * math.pi * frame / N_FRAMES)
        idx = idx + (1 if p > 0.6 else 0) - (1 if p < 0.2 and mat == "brillo" else 0)
    return rp[clamp(idx, 0, len(rp) - 1)] + (255,)


def imagen(lz, tier, frame):
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # brillo que recorre el arma (fotogramas 10 a 15)
    barrido = None
    if frame >= 10:
        barrido = 4.0 + (frame - 10) * 7.0
    for y in range(32):
        for x in range(32):
            p = lz.px[y][x]
            if p is None:
                continue
            c = color(tier, p[0], p[1], frame)
            if barrido is not None and p[0] in ("hoja", "adorno") and lz.u[y][x] is not None:
                d = abs(lz.u[y][x] - barrido)
                if d < 1.6:
                    rp = PAL[tier][p[0]]
                    c = rp[clamp(p[1] + (2 if d < 0.8 else 1), 0, len(rp) - 1)] + (255,)
            im.putpixel((x, y), c)
    return im


def tira(tipo, tier, mano=False):
    lz = dibujar(tipo, tier, mano)
    out = Image.new("RGBA", (32, 32 * N_FRAMES))
    for f in range(N_FRAMES):
        out.paste(imagen(lz, tier, f), (0, 32 * f))
    return out


NOMBRE_TIER = ["Naturaleza", "Forja", "Hielo", "Fuego", "Magia", "Dragón", "Cielo", "Abismo"]
NOMBRE_TIPO = ["Espada", "Hacha de guerra", "Martillo", "Lanza", "Dagas"]


def hoja_muestra(path, escala=4):
    from PIL import ImageDraw, ImageFont
    try:
        fnt = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 20)
        fch = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 15)
    except OSError:
        fnt = fch = ImageFont.load_default()
    pad, izq, arriba = 10, 150, 40
    cel = 32 * escala + pad
    W = izq + len(TIPOS) * cel + pad
    H = arriba + len(TIERS) * cel + pad
    sh = Image.new("RGBA", (W, H), (30, 30, 38, 255))
    d = ImageDraw.Draw(sh)
    for i, n in enumerate(NOMBRE_TIPO):
        d.text((izq + i * cel + 64 - d.textlength(n, font=fch) / 2, 12), n, fill=(235, 225, 200, 255), font=fch)
    for j, tier in enumerate(TIERS):
        y = arriba + j * cel
        if j % 2 == 0:
            d.rectangle((0, y - pad // 2, W, y + cel - pad // 2), fill=(36, 36, 46, 255))
        d.text((10, y + 52), "%d. %s" % (j + 1, NOMBRE_TIER[j]), fill=(220, 220, 235, 255), font=fnt)
        for i, tipo in enumerate(TIPOS):
            im = imagen(dibujar(tipo, tier), tier, 0).resize((32 * escala, 32 * escala), Image.NEAREST)
            sh.alpha_composite(im, (izq + i * cel, y))
    sh.save(path)


MCMETA = {"animation": {"frametime": 4, "interpolate": False}}


def escribir(res):
    A = os.path.join(res, "assets", "mundos_de_axelis")
    tdir = os.path.join(A, "textures", "item", "arma")
    mdir = os.path.join(A, "models", "item")
    os.makedirs(tdir, exist_ok=True)
    os.makedirs(mdir, exist_ok=True)
    n = 0
    for tier in TIERS:
        for tipo in TIPOS:
            ident = "arma_%s_%s" % (tipo, tier)
            tira(tipo, tier).save(os.path.join(tdir, ident + ".png"))
            json.dump(MCMETA, open(os.path.join(tdir, ident + ".png.mcmeta"), "w"))
            if tipo == "dagas":
                tira(tipo, tier, mano=True).save(os.path.join(tdir, ident + "_mano.png"))
                json.dump(MCMETA, open(os.path.join(tdir, ident + "_mano.png.mcmeta"), "w"))
            json.dump(modelo(tipo, ident), open(os.path.join(mdir, ident + ".json"), "w"), indent=1)
            n += 1
    return n


# Cómo se sostiene cada tipo (el de Minecraft es "handheld"); las armas largas se ven más grandes en la mano.
MANO = {
    "espada": None,
    "dagas": None,
    "hacha": {"s": 1.15, "tr": [0, 5.0, 0.5], "fp": [1.13, 4.2, 1.13]},
    "martillo": {"s": 1.2, "tr": [0, 5.5, 0.5], "fp": [1.13, 4.6, 1.13]},
    "lanza": {"s": 1.45, "tr": [0, 3.2, 0.5], "fp": [1.13, 2.4, 1.13]},
}


def modelo(tipo, ident):
    tex = "mundos_de_axelis:item/arma/" + ident
    m = {"parent": "minecraft:item/handheld", "textures": {"layer0": tex}}
    t = MANO[tipo]
    if t:
        s = t["s"]
        m["display"] = {
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": t["tr"], "scale": [0.85 * s] * 3},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": t["tr"], "scale": [0.85 * s] * 3},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": t["fp"], "scale": [0.68 * s] * 3},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": t["fp"], "scale": [0.68 * s] * 3},
        }
    if tipo == "dagas":
        # en el inventario se ven las dos dagas cruzadas; en la mano, una sola
        return {
            "loader": "forge:separate_transforms",
            "gui_light": "front",
            "base": {"parent": "minecraft:item/handheld", "textures": {"layer0": tex + "_mano"}},
            "perspectives": {
                "gui": {"parent": "minecraft:item/generated", "textures": {"layer0": tex}},
                "ground": {"parent": "minecraft:item/generated", "textures": {"layer0": tex}},
                "fixed": {"parent": "minecraft:item/generated", "textures": {"layer0": tex}},
            },
        }
    return m


if __name__ == "__main__":
    if len(sys.argv) >= 3 and sys.argv[1] == "--hoja":
        hoja_muestra(sys.argv[2])
    else:
        print("armas:", escribir(sys.argv[1]))
        hoja_muestra(os.path.join(os.path.dirname(os.path.abspath(__file__)), "armas281_muestra.png"))
