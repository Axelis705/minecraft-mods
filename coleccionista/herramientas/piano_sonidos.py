#!/usr/bin/env python3
"""Los sonidos del piano gigante (capítulo 6), hechos a mano (sin descargar nada):

- piano_0.ogg .. piano_7.ogg  las 8 teclas: notas de caja de música (do, re, mi, fa, sol, la, si, do), con su campanita metálica y un
                               poquito desafinadas (como una caja vieja)
- piano_mal.ogg                tecla equivocada: un acorde chueco y grave, como si alguien aplastara todas las teclas
- piano_abre.ogg               el piso del piano se abre: crujido de madera, golpe de bisagras y el viento del foso

Uso: python3 herramientas/piano_sonidos.py  (necesita ffmpeg con libvorbis)
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
from caja_sonidos import SR, t_, lp, hp, bp, eco, normal  # noqa: E402

SND = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'sounds', 'piano')
R = np.random.RandomState(777)
NOTAS = [523.25, 587.33, 659.26, 698.46, 783.99, 880.00, 987.77, 1046.50]


def guardar(nombre, x):
    os.makedirs(SND, exist_ok=True)
    x = np.clip(x, -1, 1)
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, 'a.wav')
        with wave.open(w, 'wb') as f:
            f.setnchannels(1)
            f.setsampwidth(2)
            f.setframerate(SR)
            f.writeframes((x * 32767).astype(np.int16).tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', w, '-c:a', 'libvorbis', '-q:a', '6', os.path.join(SND, nombre + '.ogg')],
                       check=True)
    print(nombre, f'{len(x) / SR:.2f} s')


def lengueta(f, seg=1.6, desafina=0.0):
    """Una lengüeta de caja de música: ataque seco, parciales metálicos (no armónicos) que se apagan rápido y la fundamental que dura."""
    t = t_(seg)
    f = f * (1 + desafina)
    x = np.sin(2 * np.pi * f * t) * np.exp(-t * 2.6)
    x += 0.35 * np.sin(2 * np.pi * f * 2.0 * t) * np.exp(-t * 5)
    x += 0.22 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t * 14)
    x += 0.12 * np.sin(2 * np.pi * f * 8.9 * t) * np.exp(-t * 26)
    # un vibrato chiquito que se nota al final (el resorte de la caja ya está cansado)
    x *= 1 + 0.04 * np.sin(2 * np.pi * 5.5 * t) * (t / seg)
    golpe = R.randn(int(0.006 * SR)) * np.exp(-np.arange(int(0.006 * SR)) / (0.0015 * SR))
    x[: len(golpe)] += 0.5 * hp(golpe, 3000)
    ataque = np.minimum(1, t / 0.002)
    return x * ataque


def notas():
    for i, f in enumerate(NOTAS):
        x = lengueta(f, desafina=R.uniform(-0.006, 0.006))
        guardar(f'piano_{i}', normal(eco(x, seg=1.2, mezcla=0.25, oscuro=4000), 0.8))


def mal():
    t = t_(1.8)
    x = np.zeros_like(t)
    for f in (130.8, 138.6, 155.6, 185.0, 196.0, 233.1):
        x += np.sin(2 * np.pi * f * t + R.rand() * 6) * np.exp(-t * 1.6)
        x += 0.4 * np.sin(2 * np.pi * f * 3.01 * t) * np.exp(-t * 4)
    golpe = R.randn(len(t)) * np.exp(-t * 30)
    x += 1.5 * lp(golpe, 900)
    guardar('piano_mal', normal(eco(x, seg=1.6, mezcla=0.4, oscuro=1500), 0.9))


def abre():
    t = t_(2.4)
    # crujido de madera: chasquidos cortos que se juntan
    cr = np.zeros_like(t)
    for _ in range(40):
        i = int(R.uniform(0, 0.5) * SR)
        n = int(0.01 * SR)
        cr[i:i + n] += R.randn(n) * np.exp(-np.arange(n) / (0.002 * SR)) * R.uniform(0.3, 1)
    cr = bp(cr, 400, 4000)
    # las bisagras: dos golpes graves de metal
    gol = np.zeros_like(t)
    for ini in (0.45, 0.6):
        tt = t - ini
        m = tt >= 0
        gol[m] += np.sin(2 * np.pi * 90 * tt[m]) * np.exp(-tt[m] * 9) + 0.5 * np.sin(2 * np.pi * 233 * tt[m]) * np.exp(-tt[m] * 14)
    # el viento del foso
    vi = lp(R.randn(len(t)), 300 + 500 * np.exp(-((t - 1.2) ** 2) * 3)) * np.minimum(1, t / 0.6) * np.exp(-np.maximum(0, t - 1.4) * 2)
    x = cr + 1.3 * gol + 2.5 * vi
    guardar('piano_abre', normal(eco(x, seg=1.8, mezcla=0.45, oscuro=1800), 0.95))


def main():
    notas()
    mal()
    abre()


if __name__ == '__main__':
    main()
