#!/usr/bin/env python3
"""Las voces del pingüino, hechas a mano con numpy (sin descargar nada):

- pinguino_hola1..3  graznidos cortitos y nasales (los que hace solo, de vez en cuando)
- pinguino_lastima   un graznido que baja (cuando lo lastiman)
- pinguino_feliz     un trino rápido que sube (al darle pescado, al hacerse tuyo, al traerte algo)

Uso: python3 herramientas/sonidos_pinguino.py  (necesita ffmpeg con libvorbis)
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from sonidos_invierno import SR, bp, guardar, normal  # noqa: E402

R = np.random.RandomState(1225)


def graznido(dur, f0, f1, nasal=0.6, vib=0.0, ruido=0.04):
    """Un graznido: onda con armónicos que va de f0 a f1, con un filtro nasal y un poquito de aire."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    k = np.linspace(0, 1, n)
    f = f0 + (f1 - f0) * (k ** 0.8) + vib * np.sin(2 * np.pi * 28 * t) * f0
    fase = 2 * np.pi * np.cumsum(f) / SR
    x = sum(np.sin(h * fase) / h ** 0.9 for h in range(1, 10))
    x += ruido * R.randn(n)
    x = (1 - nasal) * x + nasal * bp(x, 900, 3200)
    env = np.minimum(1, t / 0.012) * np.exp(-((k - 0.35) ** 2) / 0.18) * np.minimum(1, (dur - t) / 0.03)
    return x * env


def silencio(s):
    return np.zeros(int(s * SR))


def main():
    guardar('pinguino_hola1', normal(np.concatenate([graznido(0.11, 620, 820), silencio(0.06), graznido(0.14, 640, 760)]), 0.8))
    guardar('pinguino_hola2', normal(graznido(0.22, 560, 900, vib=0.03), 0.8))
    guardar('pinguino_hola3', normal(np.concatenate([graznido(0.08, 760, 900), silencio(0.04), graznido(0.08, 760, 920),
                                                     silencio(0.04), graznido(0.12, 700, 820)]), 0.8))
    guardar('pinguino_lastima', normal(graznido(0.28, 980, 470, nasal=0.4, vib=0.05, ruido=0.12), 0.9))
    trino = [graznido(0.06, 700 + 70 * i, 820 + 80 * i) for i in range(6)]
    partes = []
    for tr in trino:
        partes += [tr, silencio(0.025)]
    guardar('pinguino_feliz', normal(np.concatenate(partes), 0.85))


if __name__ == '__main__':
    main()
