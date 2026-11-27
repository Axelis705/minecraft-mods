#!/usr/bin/env python3
"""Sonidos del lanzanieves (sintetizados): disparo (un "fump" de aire con crujido de nieve; 3 variantes), el giro de los cañones al
tomar vuelo, la carga de la bola gigante (un brillo que sube) y el disparo de la bola gigante (un golpe grave con soplido).
Uso: python3 herramientas/sonidos_lanzanieves.py"""
import os
import subprocess

import numpy as np
from scipy.signal import butter, lfilter

SR = 44100
AQUI = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad', 'sounds')


def banda(x, lo, hi):
    b, a = butter(2, [lo / (SR / 2), hi / (SR / 2)], btype='band')
    return lfilter(b, a, x)


def bajos(x, corte):
    b, a = butter(2, corte / (SR / 2))
    return lfilter(b, a, x)


def t_(dur):
    return np.arange(int(dur * SR)) / SR


def dispara(rng, tono=1.0):
    t = t_(0.28)
    aire = banda(rng.standard_normal(len(t)), 300 * tono, 1800 * tono) * np.exp(-t / 0.045) * 1.4
    golpe = np.sin(2 * np.pi * (95 * tono) * t * (1 - 0.4 * t / 0.28)) * np.exp(-t / 0.035) * 0.9
    crujido = banda(rng.standard_normal(len(t)), 3000, 7000) * np.exp(-np.maximum(0, t - 0.01) / 0.02) * 0.35 * (t > 0.008)
    x = aire + golpe + crujido
    return x / np.max(np.abs(x)) * 0.85


def gira(rng):
    t = t_(0.55)
    f = 40 + 120 * np.clip(t / 0.4, 0, 1) ** 0.7
    fase = 2 * np.pi * np.cumsum(f) / SR
    zumbido = np.sign(np.sin(fase)) * 0.3 + np.sin(2 * fase) * 0.3 + np.sin(4.0 * fase) * 0.15
    silbido = np.sin(2 * np.pi * np.cumsum(f * 9) / SR) * 0.08
    ruido = banda(rng.standard_normal(len(t)), 500, 2500) * 0.12
    env = np.clip(t / 0.05, 0, 1) * np.clip((0.55 - t) / 0.15, 0, 1)
    x = bajos(zumbido, 1800) + silbido + ruido
    x = x * env
    return x / np.max(np.abs(x)) * 0.7


def carga(rng):
    t = t_(1.1)
    x = np.zeros(len(t))
    for k, base in enumerate((660, 990, 1320)):
        f = base * (1 + 0.6 * t / 1.1)
        x += np.sin(2 * np.pi * np.cumsum(f) / SR) * (0.4 / (k + 1)) * (0.6 + 0.4 * np.sin(2 * np.pi * 9 * t))
    # chispitas de hielo
    for i in range(18):
        t0 = rng.uniform(0.05, 1.0)
        f = rng.uniform(2500, 5200)
        n0 = int(t0 * SR)
        d = t_(0.12)
        s = np.sin(2 * np.pi * f * d) * np.exp(-d / 0.03) * 0.2
        x[n0:n0 + len(s)] += s[:len(x) - n0]
    env = np.clip(t / 0.08, 0, 1) * np.clip((1.1 - t) / 0.2, 0, 1)
    x *= env
    return x / np.max(np.abs(x)) * 0.6


def bola(rng):
    t = t_(0.7)
    golpe = np.sin(2 * np.pi * 62 * t * (1 - 0.3 * t)) * np.exp(-t / 0.09)
    aire = banda(rng.standard_normal(len(t)), 150, 900) * np.exp(-t / 0.16) * 1.3
    soplo = banda(rng.standard_normal(len(t)), 900, 3000) * np.exp(-((t - 0.12) / 0.12) ** 2) * 0.4
    x = golpe + aire + soplo
    x = np.tanh(x * 1.5)
    return x / np.max(np.abs(x)) * 0.9


def guardar(x, nombre):
    k = int(0.003 * SR)
    x = x.copy()
    x[:k] *= np.linspace(0, 1, k)
    x[-k:] *= np.linspace(1, 0, k)
    ruta = os.path.join(OUT, nombre + '.ogg')
    raw = ruta + '.raw'
    (np.clip(x, -1, 1) * 32767).astype('<i2').tofile(raw)
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-f', 's16le', '-ar', str(SR), '-ac', '1', '-i', raw, '-c:a', 'libvorbis', '-q:a', '4',
                    ruta], check=True)
    os.remove(raw)


if __name__ == '__main__':
    rng = np.random.default_rng(1205)
    for i, tono in enumerate((1.0, 0.92, 1.08), 1):
        guardar(dispara(rng, tono), f'lanzanieves_dispara{i}')
    guardar(gira(rng), 'lanzanieves_gira')
    guardar(carga(rng), 'lanzanieves_carga')
    guardar(bola(rng), 'lanzanieves_bola')
    import json
    p = os.path.join(OUT, '..', 'sounds.json')
    d = json.load(open(p, encoding='utf-8'))
    d['lanzanieves_dispara'] = {'sounds': [f'navidad:lanzanieves_dispara{i}' for i in (1, 2, 3)], 'subtitle': 'navidad.sub.lanzanieves'}
    d['lanzanieves_gira'] = {'sounds': ['navidad:lanzanieves_gira'], 'subtitle': 'navidad.sub.lanzanieves_gira'}
    d['lanzanieves_carga'] = {'sounds': ['navidad:lanzanieves_carga'], 'subtitle': 'navidad.sub.lanzanieves_carga'}
    d['lanzanieves_bola'] = {'sounds': ['navidad:lanzanieves_bola'], 'subtitle': 'navidad.sub.lanzanieves_bola'}
    json.dump(d, open(p, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
    print('sonidos del lanzanieves listos')
